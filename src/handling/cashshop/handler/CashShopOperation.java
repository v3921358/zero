package handling.cashshop.handler;

import client.*;
import client.inventory.*;
import constants.GameConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.World;
import server.CashItemFactory;
import server.CashItemInfo;
import server.CashShop;
import server.MapleInventoryManipulator;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CSPacket;
import tools.packet.CWvsContext;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CashShopOperation {

    public static void LeaveCS(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());

        try {

            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());
            c.getSession().writeAndFlush(CField.getChannelChange(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1])));
        } finally {
            final String s = c.getSessionIPAddress();
            LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
            new MapleCharacterSave(chr).saveToDB(chr, false, true);
            c.setPlayer(null);
            c.setReceiving(false);
            //c.getSession().close(true);
        }
    }

    public static void EnterCS(final int playerid, final MapleClient c) {
        CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            return;
        }

        World.isCharacterListConnected(c.getPlayer().getName(), c.loadCharacterNames(c.getWorld()));
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        CashShopServer.getPlayerStorage().registerPlayer(chr);
        c.getSession().writeAndFlush(CSPacket.warpCS(c));
        doCSPackets(c);
    }

    public static void CSUpdate(final MapleClient c) {
        c.getSession().writeAndFlush(CSPacket.getCSGifts(c));
        doCSPackets(c);
        c.getSession().writeAndFlush(CSPacket.sendWishList(c.getPlayer(), false));
    }

    public static void CouponCode(final String code, final MapleClient c) {
        if (code.length() <= 0) {
            return;
        }
        Triple<Boolean, Integer, Integer> info = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (info != null && info.left) {
            int type = info.mid, item = info.right;
            MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
            /*
             * Explanation of type!
             * Basically, this makes coupon codes do
             * different things!
             *
             * Type 1: A-Cash,
             * Type 2: Maple Points
             * Type 3: Item.. use SN
             * Type 4: Mesos
             */
            Map<Integer, Item> itemz = new HashMap<Integer, Item>();
            int maplePoints = 0, mesos = 0;
            switch (type) {
                case 1:
                case 2:
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    break;
                case 3:
                    CashItemInfo itez = CashItemFactory.getInstance().getItem(item);
                    if (itez == null) {
                        c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
                        return;
                    }
                    byte slot = MapleInventoryManipulator.addId(c, itez.getId(), (short) 1, "", "Cash shop: coupon code" + " on " + FileoutputUtil.CurrentReadable_Date());
                    if (slot <= -1) {
                        c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
                        return;
                    } else {
                        itemz.put(item, c.getPlayer().getInventory(GameConstants.getInventoryType(item)).getItem(slot));
                    }
                    break;
                case 4:
                    c.getPlayer().gainMeso(item, false);
                    mesos = item;
                    break;
            }
            c.getSession().writeAndFlush(CSPacket.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
        } else {
            c.getSession().writeAndFlush(CSPacket.sendCSFail(info == null ? 0xA7 : 0xA5)); //A1, 9F
        }
    }

    public static final void BuyCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int action = slea.readByte();
        if (action == 0) {
            slea.skip(2);
            CouponCode(slea.readMapleAsciiString(), c);
        } else if (action == 3) { //?????? ????????? ??????
            slea.readShort();
            slea.skip(1); // ?
            final int toCharge = 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            //TODO :: int 1 - ?????? ???????????? / int 0 - ????????? ????????????
            if (item != null && chr.getCSPoints(toCharge) >= item.getPrice()) {
                if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.getSession().writeAndFlush(CSPacket.sendCSFail(0xA6));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.getSession().writeAndFlush(CSPacket.sendCSFail(0xB1));
                    doCSPackets(c);
                    return;
                }
                for (int i : GameConstants.cashBlock) {
                    if (item.getId() == i) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                Item itemz = chr.getCashInventory().toItem(item);
                if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == item.getId() && itemz.getQuantity() == item.getCount()) {
                    if (toCharge == 1 && itemz.getType() == 1) {
                        itemz.setFlag((ItemFlag.KARMA_EQUIP.getValue()));
                    } else if (toCharge == 1 && itemz.getType() != 1) {
                        itemz.setFlag((ItemFlag.KARMA_USE.getValue()));
                    }
                    chr.getCashInventory().addToInventory(itemz);
                    String amount = "amount=900;";
                    String given = "given=-1;";
                    String per = "per=9";
                    c.getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(18155, "start=20150309;" + amount + given + per)); //???????????? ????????? ??????
                    c.getSession().writeAndFlush(CSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                } else {
                    c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
                }
            } else {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
            }
        } else if (action == 5) { // ??? ??????
            chr.clearWishlist();
            if (slea.available() < 40) {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            int[] wishlist = new int[12];
            for (int i = 0; i < 12; i++) {
                wishlist[i] = slea.readInt();
            }
            chr.setWishlist(wishlist);
            c.getSession().writeAndFlush(CSPacket.sendWishList(chr, true));
        } else if (action == 6) { // ???????????? ?????? ?????????
            slea.skip(1);
            final int toCharge = 1;
            final boolean coupon = slea.readByte() > 0;
            if (coupon) {
                final MapleInventoryType type = getInventoryType(slea.readInt());
                if (chr.getCSPoints(toCharge) >= 12000 && chr.getInventory(type).getSlotLimit() < 89) {
                    chr.modifyCSPoints(toCharge, -12000, false);
                    chr.getInventory(type).addSlot((byte) 8);
                    chr.dropMessage(1, "???????????? ????????? ???????????????. ?????? " + chr.getInventory(type).getSlotLimit() + " ????????? ???????????????.\r\n\r\n??????????????? ????????? ????????? ?????? ????????? ????????? ???????????? ???????????????, ??????????????? ???????????? ??????????????? ????????? ???????????? ??? ??? ????????????.");
                } else {
                    chr.dropMessage(1, "????????? ??? ?????? ?????? ??? ????????????.");
                }
            } else {
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                if (chr.getCSPoints(toCharge) >= 8000 && chr.getInventory(type).getSlotLimit() < 93) {
                    chr.modifyCSPoints(toCharge, -8000, false);
                    chr.getInventory(type).addSlot((byte) 4);
                    chr.dropMessage(1, "???????????? ????????? ???????????????. ?????? " + chr.getInventory(type).getSlotLimit() + " ????????? ???????????????.\r\n\r\n??????????????? ????????? ????????? ?????? ????????? ????????? ???????????? ???????????????, ??????????????? ???????????? ??????????????? ????????? ???????????? ??? ??? ????????????.");
                } else {
                    chr.dropMessage(1, "????????? ??? ?????? ?????? ??? ????????????.");
                }
            }
        } else if (action == 7) { // ?????? ?????? ?????????
            if (chr.getCSPoints(1) >= 8000 && chr.getStorage().getSlots() < 48) {
                chr.modifyCSPoints(1, -8000, false);
                chr.getStorage().increaseSlots((byte) 4);
                chr.dropMessage(1, "??????????????? ???????????????. ?????? ?????? ????????? " + chr.getStorage().getSlots() + "??? ?????????.");
            } else {
                chr.dropMessage(1, "????????? ??? ?????? ?????? ??? ????????????.");
            }
        } else if (action == 8) { // ????????? ?????? ?????????
            slea.skip(1);
            final int toCharge = 1;
            CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            int slots = c.getCharacterSlots();
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || slots > 15 || item.getId() != 5430000) {
                doCSPackets(c);
                return;
            }
            if (c.gainCharacterSlot()) {
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().writeAndFlush(CSPacket.buyCharacterSlot());
            } else {
                chr.dropMessage(1, "????????? ??? ?????? ?????? ??? ????????????.");
            }
        } else if (action == 10) { // ????????? ?????? ?????????
            final int toCharge = slea.readByte() + 1;
            final int sn = slea.readInt();
            CashItemInfo item = CashItemFactory.getInstance().getItem(sn);
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || item.getId() / 10000 != 555) {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            MapleQuestStatus marr = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            if (marr != null && marr.getCustomData() != null && Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis()) {
                chr.dropMessage(1, "?????? ????????? ???????????? ??????????????????.");
                doCSPackets(c);
            } else {
                long days = 0;
                if (item.getId() == 5550000) { // ????????? ??????????????? : 30???
                    days = 30;
                } else if (item.getId() == 5550001) { // ????????? ??????????????? : 7???
                    days = 7;
                }
                c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(System.currentTimeMillis() + days * 24 * 60 * 60000));
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().writeAndFlush(CSPacket.buyPendantSlot((short) 30));
            }
        } else if (action == 14) { // ?????? ?????????????????? ????????? ?????????
            //uniqueid, 00 01 01 00, type->position(short)
            Item item = c.getPlayer().getCashInventory().findByCashId(slea.readInt());
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                Item item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, false, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        //c.getPlayer().addPet(item_.getPet());
                    }
                    c.getPlayer().getCashInventory().removeFromInventory(item);
                    c.getSession().writeAndFlush(CSPacket.confirmFromCSInventory(item_, pos));
                } else {
                    c.getSession().writeAndFlush(CSPacket.sendCSFail(0xB1));
                }
            } else {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(0xB1));
            }
        } else if (action == 15) { // ?????? ??????????????? ??????
            Item item = null;
            MapleInventory inv = null;
            short slot = -1;
            int uniqueid = slea.readInt();
            for (MapleInventory iv : c.getPlayer().getInventorys()) {
                item = iv.findByUniqueId(uniqueid);
                if (item != null) {
                    slot = item.getPosition();
                    inv = iv;
                    break;
                }
            }
            if (item != null) {
                c.getPlayer().getCashInventory().addToInventory(item);
                c.getSession().writeAndFlush(CSPacket.confirmToCSInventory(item, c.getAccID(), -1));
                if (item.getPet() != null) {
                    c.getPlayer().removePet(item.getPet(), false);
                }
                inv.removeSlot(slot);
            } else {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(0xB1));
            }
        } else if (action == 35) { // ????????? ????????? ??????
            final int toCharge = slea.readByte() + 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            List<Integer> ccc = null;
            if (item != null) {
                ccc = CashItemFactory.getInstance().getPackageItems(item.getId());
            }
            if (item == null || ccc == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()) {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(3));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(11));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                c.getSession().writeAndFlush(CSPacket.sendCSFail(24));
                doCSPackets(c);
                return;
            }
            for (int iz : GameConstants.cashBlock) {
                if (item.getId() == iz) {
                    c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            Map<Integer, Item> ccz = new HashMap<Integer, Item>();
            for (int i : ccc) {
                final CashItemInfo cii = CashItemFactory.getInstance().getSimpleItem(i);
                if (cii == null) {
                    continue;
                }
                Item itemz = c.getPlayer().getCashInventory().toItem(cii);
                if (itemz == null || itemz.getUniqueId() <= 0) {
                    continue;
                }
                for (int iz : GameConstants.cashBlock) {
                    if (itemz.getItemId() == iz) {
                        c.getPlayer().dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                        doCSPackets(c);
                        return;
                    }
                }
                ccz.put(i, itemz);
            }
            for (Item itemsa : ccz.values()) {
                c.getPlayer().getCashInventory().addToInventory(itemsa);
            }
            chr.modifyCSPoints(toCharge, -item.getPrice(), false);
            c.getSession().writeAndFlush(CSPacket.showBoughtCSPackage(ccz, c.getAccID()));
        } else if (action == 49) { // ?????????????
            c.getSession().writeAndFlush(CSPacket.updatePurchaseRecord());
        } else if (action == 58) { // ?????? ??????
            long uniqueid = slea.readLong();
            CashShop csinv = chr.getCashInventory();
            Item item = csinv.findByCashId((int) uniqueid);
            if (item == null) {
                c.getPlayer().dropMessage(1, "????????? ??????????????????! ?????? ?????????????????? ???????????? ???????????????. GM?????? ????????? ?????????.");
                c.getSession().writeAndFlush(CSPacket.showNXMapleTokens(chr));
                return;
            }
            int reward = 0;
            switch (item.getItemId()) {
                case 5533008: { //???????????? ?????? ??????
                    int[] items = {1102376, 1102377, 1102378};
                    reward = items[(int) Math.floor(Math.random() * items.length)];
                    break;
                }
                case 5533011: { //?????? ???????????? ??????
                    int[] items = {1003398, 1003399, 1003400, 1003401, 1003402};
                    reward = items[(int) Math.floor(Math.random() * items.length)];
                    break;
                }
                case 5533012: { //??????????????? ????????????
                    int[] items = {1102385, 1102386, 1102389, 1102390};
                    reward = items[(int) Math.floor(Math.random() * items.length)];
                    break;
                }
                case 5533002: { //??????????????? ?????? ??????
                    int[] items = {1702269, 1702270, 1702271, 1702272, 1702273};
                    reward = items[(int) Math.floor(Math.random() * items.length)];
                    break;
                }
                case 5533017: { //?????? ?????? ??????
                    int[] items = {1702361, 1702362, 1702364, 1702363};
                    reward = items[(int) Math.floor(Math.random() * items.length)];
                    break;
                }
            }
            if (reward != 0) {
                Item tem = null;
                if (GameConstants.isEquip(reward)) {
                    Equip equip = new Equip(reward, (short) 1, (byte) 0);
//                    if (item.getPeriod() > 0)
//                        equip.setExpiration(System.currentTimeMillis() + ((long) (item.getPeriod()) * ((long) 86400000)));
                    tem = equip.copy();
                }
                if (chr.getCashInventory() == null || item == null) {
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "?????????????????? ???????????? ????????? ????????? ?????????????????????!"));
                    return;
                }
                if (tem != null) {
                    short pos = MapleInventoryManipulator.addbyItem(c, tem, false, true);
                    if (pos == -1) {
                        c.getPlayer().dropMessage(1, "???????????? ?????? ????????? ???????????????.");
                        c.getSession().writeAndFlush(CSPacket.showNXMapleTokens(chr));
                        return;
                    }
                    c.getSession().writeAndFlush(CSPacket.sendRandomBox(uniqueid, tem, pos));
                    c.getPlayer().getCashInventory().removeFromInventory(item);
                } else {
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "?????????????????? ???????????? ????????? ????????? ?????????????????????!"));
                    return;
                }
            } else {
                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "???????????? ?????? ???????????????."));
            }
        } else {
            System.out.println("New Action: " + action + " Remaining: " + slea.toString());
            //c.getPlayer().dropMessage(1, "New Action: " + action);
            c.getSession().writeAndFlush(CSPacket.sendCSFail(0));
        }
        doCSPackets(c);
    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200093:
                return MapleInventoryType.EQUIP;
            case 50200094:
                return MapleInventoryType.USE;
            case 50200197:
                return MapleInventoryType.SETUP;
            case 50200095:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }

    public static final void doCSPackets(MapleClient c) {
        c.getSession().writeAndFlush(CSPacket.getCSInventory(c));
        c.getSession().writeAndFlush(CSPacket.showNXMapleTokens(c.getPlayer()));
        c.getSession().writeAndFlush(CSPacket.enableCSUse());
        c.getPlayer().getCashInventory().checkExpire(c);
    }
}
