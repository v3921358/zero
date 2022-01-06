/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import log.DBLogger;
import log.LogType;
import server.*;
import server.Timer.ShowTimer;
import server.games.OneCardGame;
import server.games.OneCardGame.OneCard;
import server.games.OneCardGame.OneCardPlayer;
import server.maps.FieldLimitType;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.marriage.MarriageMiniBox;
import server.shops.*;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.PlayerShopPacket;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerInteractionHandler {

    public enum Interaction {

        SET_ITEMS1(0),
        SET_ITEMS2(1),
        SET_ITEMS3(2),
        SET_ITEMS4(3),
        SET_MESO1(4),
        SET_MESO2(5),
        SET_MESO3(6),
        SET_MESO4(7),
        CONFIRM_TRADE1(8),
        CONFIRM_TRADE2(9),
        CONFIRM_TRADE_MESO1(10),
        CONFIRM_TRADE_MESO2(11),
        CREATE(16),
        VISIT(19),
        INVITE_TRADE(21),
        DENY_TRADE(22),
        CHAT(24),
        OPEN(26),
        EXIT(28),
        HIRED_MERCHANT_MAINTENANCE(29),
        RESET_HIRED(30),
        ADD_ITEM1(31),
        ADD_ITEM2(32),
        ADD_ITEM3(33),
        ADD_ITEM4(34),
        BUY_ITEM_HIREDMERCHANT(35),
        PLAYER_SHOP_ADD_ITEM(36),
        BUY_ITEM_PLAYER_SHOP(37),
        BUY_ITEM_STORE(38),
        REMOVE_ITEM(47),
        MAINTANCE_OFF(48), //This is misspelled..
        MAINTANCE_ORGANISE(49),
        CLOSE_MERCHANT(50), // + 2.
        TAKE_MESOS(52),
        VIEW_MERCHANT_VISITOR(55),
        VIEW_MERCHANT_BLACKLIST(56),
        MERCHANT_BLACKLIST_ADD(57),
        MERCHANT_BLACKLIST_REMOVE(58),
        ADMIN_STORE_NAMECHANGE(59),
        //아직 안맞춤
        REQUEST_TIE(85),
        ANSWER_TIE(86),
        GIVE_UP(87),
        REQUEST_REDO(89),
        ANSWER_REDO(90),
        EXIT_AFTER_GAME(91),
        CANCEL_EXIT(92),
        READY(93),
        UN_READY(94),
        EXPEL(95),
        START(96),
        GAME_RESULT(97),
        SKIP(98),
        MOVE_OMOK(99),
        SELECT_CARD(103),
        WEDDING_START(105),
        WEDDING_END(108),
        INVITE_ROCK_PAPER_SCISSORS(112),
        ONECARD(155),
        ONECARD_EMOTION(156);
        public int action;

        private Interaction(int action) {
            this.action = action;
        }

        public static Interaction getByAction(int i) {
            for (Interaction s : Interaction.values()) {
                if (s.action == i) {
                    return s;
                }
            }
            return null;
        }
    }

    public static final void PlayerInteraction(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final Interaction action = Interaction.getByAction(slea.readByte() & 0xFF); // 음수 ㅆ1발련

        if (chr == null || action == null) {
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);

        switch (action) { // Mode
            case CREATE: {
                if (chr.getPlayerShop() != null || c.getChannelServer().isShutdown()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                final byte createType = slea.readByte();
                if (createType == 3 || createType == 4) { // 가위바위보, 교환
                    MapleTrade.startTrade(chr, createType == 4 ? true : false);
                } else if (createType == 1 || createType == 2 || createType == 5 || createType == 6) { // shop
                    //if (createType == 4 && !chr.isIntern()) { //not hired merch... blocked playershop
                    //    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    //    return;
                    //}
                    if (chr.getMap().getMapObjectsInRange(chr.getTruePosition(), 20000, Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.HIRED_MERCHANT)).size() != 0 || chr.getMap().getPortalsInRange(chr.getTruePosition(), 20000).size() != 0) {
                        chr.dropMessage(1, "이곳에 상점을 세울 수 없습니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    } else if (createType == 1 || createType == 2) {
                        if (FieldLimitType.Minigames.check(chr.getMap().getFieldLimit()) || chr.getMap().allowPersonalShop()) {
                            chr.dropMessage(1, "이곳에 미니게임을 개설할 수 없습니다.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            return;
                        }
                    }
                    final String desc = slea.readMapleAsciiString();
                    String pass = "";
                    if (slea.readByte() > 0) {
                        pass = slea.readMapleAsciiString();
                    }
                    if (createType == 1 || createType == 2) {
                        final int piece = slea.readByte();
                        final int itemId = createType == 1 ? (4080000 + piece) : 4080100;
                        if (!chr.haveItem(itemId) || (c.getPlayer().getMapId() >= 910000001 && c.getPlayer().getMapId() <= 910000022)) {
                            return;
                        }
                        MapleMiniGame game = new MapleMiniGame(chr, itemId, desc, pass, createType); //itemid
                        game.setPieceType(piece);
                        chr.setPlayerShop(game);
                        game.setAvailable(true);
                        game.setOpen(true);
                        game.send(c);
                        chr.getMap().addMapObject(game);
                        game.update();
                    } else if (chr.getMap().allowPersonalShop()) {
                        Item shop = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) slea.readShort());
                        if (shop == null || shop.getQuantity() <= 0 || shop.getItemId() != slea.readInt() || c.getPlayer().getMapId() < 910000001 || c.getPlayer().getMapId() > 910000022) {
                            return;
                        }
                        if (createType == 4) {
                            //MaplePlayerShop mps = new MaplePlayerShop(chr, shop.getItemId(), desc);
                            //chr.setPlayerShop(mps);
                            //chr.getMap().addMapObject(mps);
                            //c.getSession().writeAndFlush(PlayerShopPacket.getPlayerStore(chr, true));

//                        final byte state = HiredMerchantHandler.checkExistance(c.getPlayer().getAccountID(), c.getPlayer().getId());
//                        switch (state) {
//                            case 1:
//                                c.getPlayer().dropMessage(1, "프레드릭에게 먼저 물건을 찾아가세요.");
//                                break;
//                        }
                        } else if (HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) {
                            final HiredMerchant merch = new HiredMerchant(chr, shop.getItemId(), desc);
                            chr.setPlayerShop(merch);
                            chr.getMap().addMapObject(merch);
                            c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merch, true));
                        }
                    }
                }
                break;
            }
            case INVITE_TRADE: {
                if (chr.getMap() == null) {
                    return;
                }
                int dd = slea.readInt();
                MapleCharacter chrr = chr.getMap().getCharacterById(dd);
                if (chrr == null || c.getChannelServer().isShutdown()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                MapleTrade.inviteTrade(chr, chrr, true);
                break;
            }
            case INVITE_ROCK_PAPER_SCISSORS: {
                if (chr.getMap() == null) {
                    return;
                }
                MapleCharacter chrr = chr.getMap().getCharacterById(slea.readInt());
                if (chrr == null || c.getChannelServer().isShutdown()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                MapleTrade.inviteTrade(chr, chrr, false);
                break;
            }
            case DENY_TRADE: {
                if (chr.getMarriage() != null) {
                    chr.getMarriage().closeMarriageBox(true, 0x18);
                    chr.setMarriage(null);
                } else {
                    MapleTrade.declineTrade(chr);
                }
                break;
            }
            case WEDDING_START: {
                c.getPlayer().getMarriage().StartMarriage();
                break;
            }
            case WEDDING_END: {
                c.getPlayer().getMarriage().EndMarriage();
                break;
            }
            case VISIT: {
                if (c.getChannelServer().isShutdown()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                } else if (chr.getTrade() == null && chr.getPlayerShop() != null) {
                    chr.dropMessage(1, "이미 닫힌 방입니다.");
                    return;
                }
                if (chr.getTrade() != null && chr.getTrade().getPartner() != null && !chr.getTrade().inTrade()) {
                    MapleTrade.visitTrade(chr, chr.getTrade().getPartner().getChr(), chr.getTrade().getPartner().getChr().isTrade);
                } else if (chr.getMap() != null && chr.getTrade() == null) {
                    final int obid = slea.readInt();
                    if (obid == 0) {
                        if (chr.getMarriage() == null || chr.getMarriage().getPlayer1().getMarriage() == null) {
                            chr.dropMessage(1, "이미 닫힌 방입니다.");
                            return;
                        }
                        if (chr.getMarriage() != null && chr.getMarriage().getPartnerId() == chr.getId()) {
                            chr.setPlayerShop(chr.getMarriage());
                            chr.getMarriage().setPlayer2(chr);
                            chr.getMarriage().setAvailable(true);
                            chr.getMarriage().addVisitor(chr);
                            chr.getMarriage().send(c);
                            chr.getMarriage().update();
                            return;
                        }
                    }
                    MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                    if (ob == null) {
                        ob = chr.getMap().getMapObject(obid, MapleMapObjectType.SHOP);
                    }

                    if (ob instanceof IMaplePlayerShop && chr.getPlayerShop() == null) {
                        final IMaplePlayerShop ips = (IMaplePlayerShop) ob;

                        if (ob instanceof HiredMerchant) {
                            final HiredMerchant merchant = (HiredMerchant) ips;
                            /*if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
                             merchant.setOpen(false);
                             merchant.removeAllVisitors((byte) 16, (byte) 0);
                             chr.setPlayerShop(ips);
                             c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                             } else {*/
                            if (!merchant.isOpen() || !merchant.isAvailable()) {
                                chr.dropMessage(1, "현재 고용상점이 준비중에 있습니다. 잠시 후에 다시 와주세요.");
                            } else {
                                if (ips.getFreeSlot() == -1) {
                                    chr.dropMessage(1, "해당 상점을 이미 수용가능한 최대 인원이 보고 있습니다. 잠시 후 다시 시도해 주세요.");
                                } else if (merchant.isInBlackList(chr.getName())) {
                                    chr.dropMessage(1, "블랙리스트에 등록되어 해당 상점을 이용하실 수 없습니다.");
                                } else {
                                    chr.setPlayerShop(ips);
                                    merchant.addVisitor(chr);
                                    c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                                }
                            }
                            //}
                        } else {
                            if (ips instanceof MaplePlayerShop && ((MaplePlayerShop) ips).isBanned(chr.getName())) {
                                chr.dropMessage(1, "상점에서 강퇴당했습니다.");
                                return;
                            } else {
                                if (ips.getFreeSlot() < 0 || ips.getVisitorSlot(chr) > -1 || !ips.isOpen() || !ips.isAvailable()) {
                                    c.getSession().writeAndFlush(PlayerShopPacket.getMiniGameFull());
                                } else {
                                    if (slea.available() > 0 && slea.readByte() > 0) { //a password has been entered
                                        String pass = slea.readMapleAsciiString();
                                        if (!pass.equals(ips.getPassword())) {
                                            c.getPlayer().dropMessage(1, "The password you entered is incorrect.");
                                            return;
                                        }
                                    } else if (ips.getPassword().length() > 0) {
                                        c.getPlayer().dropMessage(1, "The password you entered is incorrect.");
                                        return;
                                    }
                                    chr.setPlayerShop(ips);
                                    ips.addVisitor(chr);
                                    if (ips instanceof MarriageMiniBox) {
                                        ((MarriageMiniBox) ips).send(c);
                                    } else if (ips instanceof MapleMiniGame) {
                                        ((MapleMiniGame) ips).send(c);
                                    } else {
                                        c.getSession().writeAndFlush(PlayerShopPacket.getPlayerStore(chr, false));
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
            case HIRED_MERCHANT_MAINTENANCE: {
                if (c.getChannelServer().isShutdown() || chr.getMap() == null || chr.getTrade() != null) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                slea.skip(1); // 9?
                byte type = slea.readByte(); // 5?
                if (type != 5) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                final String password = slea.readMapleAsciiString();
                //if (!c.CheckSecondPassword(password) || password.length() < 6 || password.length() > 16) {
                //	chr.dropMessage(5, "Please enter a valid PIC.");
                //	c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                //	return;
                //}				
                final int obid = slea.readInt();
                MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                if (ob == null || chr.getPlayerShop() != null) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                if (ob instanceof IMaplePlayerShop && ob instanceof HiredMerchant) {
                    final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                    final HiredMerchant merchant = (HiredMerchant) ips;
                    if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors((byte) 16, (byte) 0);
                        chr.setPlayerShop(ips);
                        c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    }
                }
                break;
            }
            case CHAT: {
                slea.readInt();
                final String message = slea.readMapleAsciiString();
                if (chr.getTrade() != null) {
                    chr.getTrade().chat(message);
                } else if (chr.getPlayerShop() != null) {
                    final IMaplePlayerShop ips = chr.getPlayerShop();
                    ips.broadcastToVisitors(PlayerShopPacket.shopChat(chr.getName(), chr.getId(), message, ips.getVisitorSlot(chr)));
                    LogType.Chat chatType = LogType.Chat.PlayerShop;
                    String etc = "";
                    if (ips instanceof MaplePlayerShop) {
                        chatType = LogType.Chat.PlayerShop;
                        etc = "주인 : " + ips.getOwnerName() + " / 상점명 : " + ips.getDescription() + " / 수신 : " + ips.getMemberNames();
                    } else if (ips instanceof MapleMiniGame) {
                        chatType = LogType.Chat.MiniGame;
                        etc = "주인 : " + ips.getOwnerName() + " / 게임명 : " + ips.getDescription() + " / 암호 : " + (ips.getPassword() == null ? "없음" : ("있음 - " + ips.getPassword())) + " / 수신 : " + ips.getMemberNames();
                    } else if (ips instanceof HiredMerchant) {
                        chatType = LogType.Chat.HiredMerchant;
                        etc = "주인 : " + ips.getOwnerName() + " / 상점명 : " + ips.getDescription() + " / 수신 : " + ips.getMemberNames();
                    }
                    DBLogger.getInstance().logChat(chatType, c.getPlayer().getId(), c.getPlayer().getName(), message, etc);
                    if (chr.getClient().isMonitored()) { //Broadcast info even if it was a command.
//                        World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, chr.getName() + " said in " + ips.getOwnerName() + " shop : " + message));
                    }
                }
                break;
            }
            case EXIT: {
                if (chr.getTrade() != null) {
                    MapleTrade.cancelTrade(chr.getTrade(), chr.getClient(), chr);
                } else if (chr.getOneCardInstance() != null) {
                    chr.getOneCardInstance().sendPacketToPlayers(SLFCGPacket.leaveResult(chr.getOneCardInstance().getPlayer(chr).getPosition()));
                    chr.getOneCardInstance().playerDead(chr.getOneCardInstance().getPlayer(chr), false);
                } else if (chr.getBattleReverseInstance() != null) {
                    chr.getBattleReverseInstance().endGame(chr, true);
                } else {
                    final IMaplePlayerShop ips = chr.getPlayerShop();
                    if (ips == null) { //should be null anyway for owners of hired merchants (maintenance_off)
                        return;
                    }
                    if (ips.isOwner(chr) && ips.getShopType() != 1) {
                        ips.closeShop(false, ips.isAvailable()); //how to return the items?
                    } else {
                        ips.removeVisitor(chr);
                    }
                    chr.setPlayerShop(null);
                }
                break;
            }
            case OPEN: {
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop.isOwner(chr) && shop.getShopType() < 3 && !shop.isAvailable()) {
                    if (chr.getMap().allowPersonalShop()) {
                        if (c.getChannelServer().isShutdown()) {
                            chr.dropMessage(1, "서버가 곧 종료되기때문에, 상점을 세울수 없습니다.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            shop.closeShop(shop.getShopType() == 1, false);
                            return;
                        }

                        if (shop.getShopType() == 1 && HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) {
                            final HiredMerchant merchant = (HiredMerchant) shop;
                            merchant.setStoreid(c.getChannelServer().addMerchant(merchant));
                            merchant.setOpen(true);
                            merchant.setAvailable(true);
                            chr.getMap().broadcastMessage(PlayerShopPacket.spawnHiredMerchant(merchant));
                            chr.setPlayerShop(null);

                        } else if (shop.getShopType() == 2) {
                            shop.setOpen(true);
                            shop.setAvailable(true);
                            shop.update();
                        }
                    } else {
                        c.disconnect(true, false, false);
                        c.getSession().close();
                    }
                }

                break;
            }
            case SET_ITEMS4:
            case SET_ITEMS3:
            case SET_ITEMS2:
            case SET_ITEMS1: {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
                final Item item = chr.getInventory(ivType).getItem((short) slea.readShort());
                final short quantity = slea.readShort();
                final byte targetSlot = slea.readByte();

                if (chr.getTrade() != null && item != null) {
                    if ((quantity <= item.getQuantity() && quantity >= 0) || GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                        chr.getTrade().setItems(c, item, targetSlot, quantity);
                    }
                }
                break;
            }
            case SET_MESO4:
            case SET_MESO3:
            case SET_MESO2:
            case SET_MESO1: {
                final MapleTrade trade = chr.getTrade();
                if (trade != null) {
                    long meso = slea.readLong();
                    if (meso < 0) {
                        meso = meso & 0xFFFFFFFFL;
                    }
                    trade.setMeso(meso);
                }
                break;
            }
            case ADD_ITEM4:
            case ADD_ITEM3:
            case ADD_ITEM2:
            case ADD_ITEM1: {
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                final short slot = slea.readShort();
                final short bundles = slea.readShort(); // How many in a bundle
                final short perBundle = slea.readShort(); // Price per bundle
                final long price = slea.readLong();
                if (price <= 0 || bundles <= 0 || perBundle <= 0) {
                    return;
                }
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame) {
                    return;
                }
                final Item ivItem = chr.getInventory(type).getItem(slot);
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (ivItem != null) {
                    long check = bundles * perBundle;
                    if (check > 32767 || check <= 0) { //This is the better way to check.
                        return;
                    }
                    final short bundles_perbundle = (short) (bundles * perBundle);
//                    if (bundles_perbundle < 0) { // int_16 overflow
//                        return;
//                    }
                    if (ivItem.getQuantity() >= bundles_perbundle) {
                        final int flag = ivItem.getFlag();
                        if (ItemFlag.LOCK.check(flag)) {
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            return;
                        }
                        if (ii.isDropRestricted(ivItem.getItemId()) || ii.isAccountShared(ivItem.getItemId()) || ItemFlag.UNTRADEABLE.check(flag)) {
                            if (!(ItemFlag.KARMA_EQUIP.check(flag) || ItemFlag.KARMA_USE.check(flag))) {
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                                return;
                            }
                        }
                        if (bundles_perbundle >= 50 && ivItem.getItemId() == 2340000) {
//                            c.setMonitored(true); //hack check
                        }
                        if (GameConstants.getLowestPrice(ivItem.getItemId()) > price) {
                            c.getPlayer().dropMessage(1, "The lowest you can sell this for is " + GameConstants.getLowestPrice(ivItem.getItemId()));
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            return;
                        }
                        if (GameConstants.isThrowingStar(ivItem.getItemId()) || GameConstants.isBullet(ivItem.getItemId())) {
                            // Ignore the bundles
                            MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);
                            final Item sellItem = ivItem.copy();
                            shop.addItem(new MaplePlayerShopItem(sellItem, (short) 1, price));
                        } else {
                            MapleInventoryManipulator.removeFromSlot(c, type, slot, bundles_perbundle, true);

                            final Item sellItem = ivItem.copy();
                            sellItem.setQuantity(perBundle);
                            shop.addItem(new MaplePlayerShopItem(sellItem, bundles, price));
                        }
                        c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(shop));
                    } else {
                        chr.dropMessage(1, "물품을 판매하려면 적어도 1개이상 있어야 합니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                }
                break;
            }
            case CONFIRM_TRADE_MESO1:
            case CONFIRM_TRADE_MESO2:
            case CONFIRM_TRADE2:
            case CONFIRM_TRADE1:
            case BUY_ITEM_PLAYER_SHOP:
            case BUY_ITEM_STORE:
            case BUY_ITEM_HIREDMERCHANT: { // Buy and Merchant buy
                if (chr.getTrade() != null) {
                    MapleTrade.completeTrade(chr);
                    break;
                }
                final int item = slea.readByte();
                final short quantity = slea.readShort();
                //slea.skip(4);
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || shop.isOwner(chr) || shop instanceof MapleMiniGame || item >= shop.getItems().size()) {
                    return;
                }
                final MaplePlayerShopItem tobuy = shop.getItems().get(item);
                if (tobuy == null) {
                    return;
                }
                long check = tobuy.bundles * quantity;
                long check2 = tobuy.price * quantity;
                long check3 = tobuy.item.getQuantity() * quantity;
                if (check <= 0 || check2 > 2147483647 || check2 <= 0 || check3 > 32767 || check3 < 0) { //This is the better way to check.
                    return;
                }
                if (tobuy.bundles < quantity || (tobuy.bundles % quantity != 0 && GameConstants.isEquip(tobuy.item.getItemId())) // Buying
                        || chr.getMeso() - (check2) < 0 || chr.getMeso() - (check2) > 2147483647 || shop.getMeso() + (check2) < 0 || shop.getMeso() + (check2) > 2147483647) {
                    return;
                }
                if (quantity >= 50 && tobuy.item.getItemId() == 2340000) {
                    c.setMonitored(true); //hack check
                }
                shop.buy(c, item, quantity);
                shop.broadcastToVisitors(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case RESET_HIRED: {
                byte subpacket = slea.readByte();
                byte type = slea.readByte(); //5 = 플레이어, 6 = 아이템 고용상인
                if (subpacket == 0x13 && (type == 5 || type == 6)) {
                    final String secondPassword = slea.readMapleAsciiString();
                    if (c.CheckSecondPassword(secondPassword)) {
                        final int obid = slea.readInt();
                        MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob == null) {
                            return;
                        }
                        final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                        if (ob instanceof HiredMerchant) {
                            final HiredMerchant merchant = (HiredMerchant) ips;
                            if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
                                merchant.setOpen(false);
                                merchant.broadcastToVisitors(CWvsContext.serverNotice(1, "", "판매자가 물품을 정리하고 있습니다."));
                                merchant.removeAllVisitors((byte) 0, (byte) 1);
                                chr.setPlayerShop(ips);
                                c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                            }
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "2차비밀번호가 일치하지 않습니다. \r\n확인 후 다시 시도해 주세요.");
                    }
                } else if (subpacket == 0x0B && type == 5) {
                    slea.skip(4);
                    final IMaplePlayerShop shop = chr.getPlayerShop();
                    if (shop.getShopType() == 1 && HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) {
                        final HiredMerchant merchant = (HiredMerchant) shop;
                        merchant.setStoreid(c.getChannelServer().addMerchant(merchant));
                        merchant.setOpen(true);
                        merchant.setAvailable(true);
                        chr.getMap().broadcastMessage(PlayerShopPacket.spawnHiredMerchant(merchant));
                        chr.setPlayerShop(null);
                        shop.removeVisitor(chr);
                    }
                } else if (subpacket == 0x0B && type == 4) {
                    final IMaplePlayerShop shop = chr.getPlayerShop();
                    shop.setOpen(true);
                    shop.setAvailable(true);
                    shop.update();
                } else if (subpacket == 16 && type == 7) {
                    final String secondPassword = slea.readMapleAsciiString();
                    if (c.CheckSecondPassword(secondPassword)) {
                        MapleCharacter chrr = chr.getMap().getCharacterById(slea.readInt());
                        if (chrr == null || c.getChannelServer().isShutdown()) {
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            return;
                        }
                        MapleTrade.startCashTrade(chr);
                        MapleTrade.inviteCashTrade(chr, chrr);
                    } else {
                        c.getPlayer().dropMessage(1, "2차비밀번호가 일치하지 않습니다. \r\n확인 후 다시 시도해 주세요.");
                    }
                } else if (subpacket == 19 && type == 7) {
                    final String secondPassword = slea.readMapleAsciiString();
                    if (c.CheckSecondPassword(secondPassword)) {
                        if (chr != null && chr.getTrade() != null && chr.getTrade().getPartner() != null && chr.getTrade().getPartner().getChr() != null) {
                            MapleTrade.visitCashTrade(chr, chr.getTrade().getPartner().getChr());
                        } else {
                            c.getPlayer().dropMessage(1, "오류가 발생했습니다. \r\n잠시 후 다시 시도해 주세요.");
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "2차비밀번호가 일치하지 않습니다. \r\n확인 후 다시 시도해 주세요.");
                    }
                }
                break;
            }
            case REMOVE_ITEM: {
                slea.skip(1); // ?
                int slot = slea.readShort(); //0
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame || shop.getItems().size() <= 0 || shop.getItems().size() <= slot || slot < 0) {
                    return;
                }
                final MaplePlayerShopItem item = shop.getItems().get(slot);

                if (item != null) {
                    if (item.bundles > 0) {
                        Item item_get = item.item.copy();
                        long check = item.bundles * item.item.getQuantity();
                        if (check < 0 || check > 32767) {
                            return;
                        }
                        item_get.setQuantity((short) check);
                        if (item_get.getQuantity() >= 50 && item.item.getItemId() == 2340000) {
                            c.setMonitored(true); //hack check
                        }
                        if (MapleInventoryManipulator.checkSpace(c, item_get.getItemId(), item_get.getQuantity(), item_get.getOwner())) {
                            MapleInventoryManipulator.addFromDrop(c, item_get, false);
                            item.bundles = 0;
                            shop.removeFromSlot(slot);
                        }
                    }
                }
                c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case MAINTANCE_OFF: {
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop instanceof HiredMerchant && shop.isOwner(chr) && shop.isAvailable()) {
                    shop.setOpen(true);
                    shop.removeAllVisitors(-1, -1);
                }
                break;
            }
            case MAINTANCE_ORGANISE: {
                final IMaplePlayerShop imps = chr.getPlayerShop();
                if (imps != null && imps.isOwner(chr) && !(imps instanceof MapleMiniGame)) {
                    for (int i = 0; i < imps.getItems().size(); i++) {
                        if (imps.getItems().get(i).bundles == 0) {
                            imps.getItems().remove(i);
                        }
                    }
                    if (chr.getMeso() + imps.getMeso() > 0) {
                        chr.gainMeso(imps.getMeso(), false);
                        imps.setMeso(0);
                    }
                    c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(imps));
                }
                break;
            }
            case CLOSE_MERCHANT: {
                //1 = 상점에 있는 돈이 너무 많아 메소와 아이템을 찾지 못했습니다. 자유 시장 입구의 프레드릭을 찾아가 보세요.
                //2 = 메소를 찾았습니다. 하나 밖에 가질 수 없는 아이템이 있어 아이템은 찾지 못했습니다. 자유 시장 입구의 프레드릭을 찾아가 보세요.
                //3 = 메소를 찾았습니다. 인벤토리에 자리가 없어서 아이템은 찾지 못했습니다. 자유 시장 입구의 프레드릭을 찾아가 보세요.
                //4 = 메소를 찾았습니다. 알 수 없는 이유로 아이템은 찾지 못했습니다. 자유 시장 입구의 프레드릭을 찾아가 보세요.
                //5 = 아무오류없음.
                //6이상부터는 모두 메시지없는 팝업창.
                //6 = 메시지없는 팝업창.
                //7 = 메시지없는 팝업창                 
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    boolean save = false;
                    if (chr.getMeso() + merchant.getMeso() < 0) { //오버플로우
                        save = false;
                    } else {
                        if (merchant.getMeso() > 0) {
                            chr.gainMeso(merchant.getMeso(), false);
                        }
                        merchant.setMeso(0);
                        if (merchant.getItems().size() > 0) {
                            for (MaplePlayerShopItem items : merchant.getItems()) {
                                if (items.bundles > 0) {
                                    Item item_get = items.item.copy();
                                    item_get.setQuantity((short) (items.bundles * items.item.getQuantity()));
                                    if (MapleInventoryManipulator.addFromDrop(c, item_get, false) != false) {
                                        items.bundles = 0;
                                        save = false;
                                    } else {
                                        save = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (save) {
                        c.getPlayer().dropMessage(1, "프레드릭 에게서 아이템을 찾아가 주십시오.");
                        c.getSession().writeAndFlush(PlayerShopPacket.shopErrorMessage(0x14, 0));
                    } else {
                        c.getSession().writeAndFlush(PlayerShopPacket.MerchantClose(0, 0));
                        HiredMerchantHandler.removeItemFromDB(chr.getAccountID(), chr.getId());
                    }
                    merchant.closeShop(save, true);
                    chr.setPlayerShop(null);
                }
                break;
            }
            case TAKE_MESOS: {
                final IMaplePlayerShop imps = chr.getPlayerShop();
                if (imps != null && imps.isOwner(chr)) {
                    if (chr.getMeso() + imps.getMeso() < 0) {
                        c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(imps));
                    } else {
                        chr.gainMeso(imps.getMeso(), false);
                        imps.setMeso(0);
                        c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(imps));
                    }
                }
                break;
            }
            case ADMIN_STORE_NAMECHANGE: { // Changing store name, only Admin
                final String storename = slea.readMapleAsciiString();
                c.getSession().writeAndFlush(PlayerShopPacket.merchantNameChange(chr.getId(), storename));
                break;
            }
            case VIEW_MERCHANT_VISITOR: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).sendVisitor(c);
                }
                break;
            }
            case VIEW_MERCHANT_BLACKLIST: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).sendBlackList(c);
                }
                break;
            }
            case MERCHANT_BLACKLIST_ADD: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).addBlackList(slea.readMapleAsciiString());
                }
                break;
            }
            case MERCHANT_BLACKLIST_REMOVE: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).removeBlackList(slea.readMapleAsciiString());
                }
                break;
            }
            case GIVE_UP: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 0, game.getVisitorSlot(chr)));
                    game.nextLoser();
                    game.setOpen(true);
                    game.update();
                    game.checkExitAfterGame();
                }
                break;
            }
            case EXPEL: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    if (!((MapleMiniGame) ips).isOpen()) {
                        break;
                    }
                    ips.removeAllVisitors(5, 1); //no msg
                }
                break;
            }
            case READY:
            case UN_READY: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (!game.isOwner(chr) && game.isOpen()) {
                        game.setReady(game.getVisitorSlot(chr));
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameReady(game.isReady(game.getVisitorSlot(chr))));
                    }
                }
                break;
            }
            case START: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOwner(chr) && game.isOpen()) {
                        for (int i = 1; i < ips.getSize(); i++) {
                            if (!game.isReady(i)) {
                                return;
                            }
                        }
                        game.setGameType();
                        game.shuffleList();
                        if (game.getGameType() == 1) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameStart(game.getLoser()));
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardStart(game, game.getLoser()));
                        }
                        game.setOpen(false);
                        game.update();
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameInfoMsg((byte) 102, chr.getName())); //게임이 시작 되었습니다.
                    }
                } else { //가위바위보 게임 스타트!
                    if (chr.getTrade() != null && chr.getTrade().getPartner() != null) {
                        c.getSession().writeAndFlush(PlayerShopPacket.StartRPS());
                        chr.getTrade().getPartner().getChr().getClient().getSession().writeAndFlush(PlayerShopPacket.StartRPS());
                    }
                }
                break;
            }
            case REQUEST_TIE: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameRequestTie(), false);
                    } else {
                        game.getMCOwner().getClient().getSession().writeAndFlush(PlayerShopPacket.getMiniGameRequestTie());
                    }
                    game.setRequestedTie(game.getVisitorSlot(chr));
                }
                break;
            }
            case ANSWER_TIE: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getRequestedTie() > -1 && game.getRequestedTie() != game.getVisitorSlot(chr)) {
                        if (slea.readByte() > 0) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 1, game.getRequestedTie()));
                            game.nextLoser();
                            game.setOpen(true);
                            game.update();
                            game.checkExitAfterGame();
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameDenyTie());
                        }
                        game.setRequestedTie(-1);
                    }
                }
                break;
            }
            case REQUEST_REDO: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameRequestRedo(), false);
                    } else {
                        game.getMCOwner().getClient().getSession().writeAndFlush(PlayerShopPacket.getMiniGameRequestRedo());
                    }
                }
                break;
            }
            case ANSWER_REDO: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (slea.readByte() > 0) {
                        ips.broadcastToVisitors(PlayerShopPacket.getMiniGameSkip(ips.getVisitorSlot(chr) == 0 ? 1 : 0));
                        game.nextLoser();
                    } else {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameDenyRedo());
                    }
                }
                break;
            }
            case SKIP: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getLoser() != ips.getVisitorSlot(chr)) {
                        //ips.broadcastToVisitors(PlayerShopPacket.shopChat("Turn could not be skipped by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + ips.getVisitorSlot(chr), ips.getVisitorSlot(chr)));
                        return;
                    }
                    ips.broadcastToVisitors(PlayerShopPacket.getMiniGameSkip(ips.getVisitorSlot(chr) == 0 ? 1 : 0));
                    game.nextLoser();
                } else { //가위바위보 게임 종료!
                    if (chr.getTrade() != null && chr.getTrade().getPartner() != null) {
                        chr.getTrade().setRPS(slea.readByte());
                        ShowTimer.getInstance().schedule(new Runnable() {

                            @Override
                            public void run() {
                                byte result = getResult(chr.getTrade().getPRS(), chr.getTrade().getPartner().getPRS());
                                if (result == 2) {
                                    chr.dropMessage(1, "아쉽지만, 가위바위보에서 지셨습니다!");
                                    chr.addFame(-1);
                                } else if (result == 0) {
                                    if (chr.getKeyValue(20220311, "ove7") < 0) {
                                        chr.setKeyValue(20220311, "ove7", 0 + "");
                                    }
                                    if (chr.getKeyValue(20220311, "ove7") < 3) {
                                        chr.setKeyValue(20220311, "ove7", (int) chr.getKeyValue(20220311, "ove7") + 1 + "");
                                        //     chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 가위바위보 "+chr.getKeyValue(20220311,"ove7")+" 회 승리 달성 !!",0,4));
                                    }
                                    chr.dropMessage(1, "축하합니다! 가위바위보에서 이기셨습니다!");
                                    chr.addFame(1);
                                }
                                c.getSession().writeAndFlush(PlayerShopPacket.FinishRPS(result, chr.getTrade().getPartner().getPRS()));
                            }
                        }, 800);
                    }
                }
                break;
            }
            case MOVE_OMOK: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getLoser() != game.getVisitorSlot(chr)) {
                        //game.broadcastToVisitors(PlayerShopPacket.shopChat("Omok could not be placed by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
                        return;
                    }
                    game.setPiece(slea.readInt(), slea.readInt(), slea.readByte(), chr);
                }
                break;
            }
            case SELECT_CARD: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getLoser() != game.getVisitorSlot(chr)) {
                        //game.broadcastToVisitors(PlayerShopPacket.shopChat("Card could not be placed by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
                        return;
                    }
                    if (slea.readByte() != game.getTurn()) {
                        //game.broadcastToVisitors(PlayerShopPacket.shopChat("Omok could not be placed by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + game.getVisitorSlot(chr) + " Turn: " + game.getTurn(), game.getVisitorSlot(chr)));
                        return;
                    }
                    final int slot = slea.readByte();
                    final int turn = game.getTurn();
                    final int fs = game.getFirstSlot();
                    if (turn == 1) {
                        game.setFirstSlot(slot);
                        if (game.isOwner(chr)) {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, turn), false);
                        } else {
                            game.getMCOwner().getClient().getSession().writeAndFlush(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, turn));
                        }
                        game.setTurn(0); //2nd turn nao
                        return;
                    } else if (fs > 0 && game.getCardId(fs + 1) == game.getCardId(slot + 1)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, game.isOwner(chr) ? 2 : 3));
                        game.setPoints(game.getVisitorSlot(chr)); //correct.. so still same loser. diff turn tho
                    } else {
                        game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, game.isOwner(chr) ? 0 : 1));
                        game.nextLoser();//wrong haha

                    }
                    game.setTurn(1);
                    game.setFirstSlot(0);

                }
                break;
            }
            case EXIT_AFTER_GAME:
            case CANCEL_EXIT: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.setExitAfter(chr);
                    if (game.isExitAfter(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameInfoMsg((byte) 5, chr.getName())); //나가기 예약
                    } else {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameInfoMsg((byte) 6, chr.getName())); //나가기 예약 취소
                    }
                }
                break;
            }
            case ONECARD: {

                byte type = slea.readByte();

                switch (type) {
                    case 0: {
                        int objectId = slea.readInt();
                        OneCardGame oc = chr.getOneCardInstance();

                        if (oc == null) {
                            return;
                        }

                        OneCardPlayer ocp = oc.getPlayer(chr);

                        if (ocp == null) {
                            return;
                        }

                        OneCard selCard = null;

                        for (OneCard card : ocp.getCards()) {
                            if (card.getObjectId() == objectId) {
                                selCard = card;
                                break;
                            }
                        }

                        if (selCard != null) {
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onPutCardResult(ocp, selCard));
                            ocp.getCards().remove(selCard);
                        } else {
                            System.out.println("selCard에 문제 발생.");
                            oc.sendPacketToPlayers(CWvsContext.serverNotice(1, "", "원카드에 문제가 발생하여 게임이 종료됩니다."));
                            oc.endGame(ocp, true);
                            return;
                        }

                        if (ocp.getCards().size() == 0 || ocp.getCards().isEmpty()) {
                            oc.endGame(ocp, false);
                            return;
                            //우승
                        } else if (ocp.getCards().size() == 1) {
                            chr.getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/lastcard"));
                        }

                        oc.setLastCard(selCard);

                        if (oc.getLastCard().getType() == 11) {
                            oc.setbClockWiseTurn(!oc.isbClockWiseTurn());
                        }

                        OneCardPlayer nextPlayer = null;
                        if (oc.getLastCard().getType() == 9 || oc.getLastCard().getType() == 8 || (oc.getLastCard().getType() == 12 && oc.getLastCard().getColor() == 3)) {
                            nextPlayer = oc.getLastPlayer();
                        } else {
                            if (oc.getLastCard().getType() == 10) {
                                nextPlayer = oc.setNextPlayer(oc.setNextPlayer(oc.getLastPlayer(), oc.isbClockWiseTurn()), oc.isbClockWiseTurn());
                            } else {
                                nextPlayer = oc.setNextPlayer(oc.getLastPlayer(), oc.isbClockWiseTurn());
                            }
                        }

                        if (oc.getLastCard().getType() == 6) {
                            oc.setFire(oc.getFire() + 2);
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(2, 2, chr.getId(), false));
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : " + chr.getName() + "님의 공격!"));
                        } else if (oc.getLastCard().getType() == 7) {
                            oc.setFire(oc.getFire() + 3);
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(2, 3, chr.getId(), false));
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : " + chr.getName() + "님의 공격!"));
                        } else if (oc.getLastCard().getType() == 12 & oc.getLastCard().getColor() == 0) {
                            oc.setFire(oc.getFire() + 5);
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/oz"));
                            oc.sendPacketToPlayers(CField.playSound("Sound/MiniGame.img/oneCard/flame_burst"));
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(2, 5, chr.getId(), false));
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : " + chr.getName() + "님의 공격!"));
                        }

                        if (oc.getLastCard().getType() == 8) {
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : 색 바꾸기!"));
                        }

                        if (oc.getLastCard().getType() == 9) {
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : 한 번 더!"));
                        }

                        if (oc.getLastCard().getType() == 10) {
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : 점프!"));
                        }

                        if (oc.getLastCard().getType() == 11) {
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText("마법 : 거꾸로!"));
                        }

                        if (oc.getLastCard().getType() == 12) {
                            if (oc.getLastCard().getColor() == 1) {
                                oc.setFire(0);
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/michael"));
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(3, 0, chr.getId(), false));
                                oc.sendPacketToPlayers(CField.playSound("Sound/MiniGame.img/oneCard/shield_appear"));
                            } else if (oc.getLastCard().getColor() == 2) {
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/hawkeye"));
                                for (OneCardPlayer bp : oc.getPlayers()) {
                                    List<OneCard> newcards = new ArrayList<>();

                                    if (bp.getPlayer().getId() != chr.getId()) {
                                        for (int i = 0; i < 2; ++i) {
                                            if (oc.getOneCardDeckInfo().size() == 0) {
                                                oc.resetDeck();
                                                if (oc.getOneCardDeckInfo().size() == 0) {
                                                    break;
                                                }
                                            }
                                            int num = Randomizer.nextInt(oc.getOneCardDeckInfo().size());
                                            OneCard card = oc.getOneCardDeckInfo().get(num);
                                            bp.getCards().add(card);
                                            newcards.add(card);
                                            oc.getOneCardDeckInfo().remove(num);
                                        }
                                        oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onGetCardResult(bp, newcards));
                                    }
                                }
                            } else if (oc.getLastCard().getColor() == 3) {
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/irina"));
                                for (OneCardPlayer bp : oc.getPlayers()) {
                                    List<OneCard> removes = new ArrayList<>();
                                    for (OneCard card : bp.getCards()) {
                                        if (card.getColor() == 3) {
                                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onPutCardResult(bp, card));
                                            removes.add(card);
                                        }
                                    }

                                    for (OneCard card : removes) {
                                        bp.getCards().remove(card);
                                    }

                                    if (bp.getCards().size() == 0 || bp.getCards().isEmpty()) {
                                        oc.endGame(bp, false);
                                        return;
                                        //우승
                                    } else if (ocp.getCards().size() == 1) {
                                        oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/lastcard"));
                                    }
                                }

                                List<Integer> ableColors = new ArrayList<>();
                                for (int i = 0; i <= 3; ++i) {
                                    ableColors.add(i);
                                }

                                c.getSession().writeAndFlush(SLFCGPacket.OneCardGamePacket.onChangeColorRequest(ableColors));
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));

                                oc.setLastPlayer(nextPlayer);

                                if (oc.getOneCardTimer() != null) {
                                    oc.getOneCardTimer().cancel(false);
                                }

                                oc.setOneCardTimer(Timer.ShowTimer.getInstance().schedule(() -> {
                                    //15초 잠수시 강제로 카드 맥이고 넘어가야함
                                    oc.skipPlayer();
                                }, 15 * 1000));

                                return;
                            } else if (oc.getLastCard().getColor() == 4) {
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/icart")); // 이카르트
                            }
                        }

                        oc.setLastPlayer(nextPlayer);

                        if (nextPlayer != null) {

                            if (oc.getLastCard().getType() == 8) {// || (oc.getLastCard().getType() == 12 && oc.getLastCard().getType() == 3)) {

                                List<Integer> ableColors = new ArrayList<>();

                                for (int i = 0; i <= 3; ++i) {
                                    if (i != oc.getLastCard().getColor()) {
                                        ableColors.add(i);
                                    }
                                }

                                c.getSession().writeAndFlush(SLFCGPacket.OneCardGamePacket.onChangeColorRequest(ableColors));
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            } else {
                                List<OneCard> possibleCards = new ArrayList<>();
                                if (oc.getLastCard().getColor() == 4) {
                                    for (OneCard card : nextPlayer.getCards()) {
                                        if (oc.getFire() == 0) {
                                            possibleCards.add(card);
                                        } else {
                                            if (card.getType() == 6 || card.getType() == 7 || (card.getType() == 12 && card.getColor() == 0)) {
                                                possibleCards.add(card);
                                            }
                                        }
                                    }
                                } else {
                                    for (OneCard card : nextPlayer.getCards()) {
                                        if (card.getType() <= 5) {
                                            if ((card.getColor() == oc.getLastCard().getColor() || card.getType() == oc.getLastCard().getType()) && oc.getFire() == 0) {
                                                possibleCards.add(card);
                                            }
                                        } else if (card.getType() <= 7) {
                                            if (oc.getFire() == 0) {
                                                if (card.getColor() == oc.getLastCard().getColor() || card.getType() == oc.getLastCard().getType()) {
                                                    possibleCards.add(card);
                                                }
                                            } else {
                                                if (oc.getLastCard().getType() == 6) {
                                                    if (card.getType() == 6) {
                                                        possibleCards.add(card);
                                                    } else if (card.getType() == 7 && card.getColor() == oc.getLastCard().getColor()) {
                                                        possibleCards.add(card);
                                                    }
                                                } else if (oc.getLastCard().getType() == 7) {
                                                    if (card.getType() == 7) {
                                                        possibleCards.add(card);
                                                    }
                                                }
                                            }
                                        } else if (card.getType() <= 11) {
                                            if ((card.getColor() == oc.getLastCard().getColor() || card.getType() == oc.getLastCard().getType()) && oc.getFire() == 0) {
                                                possibleCards.add(card);
                                            }
                                        } else {
                                            switch (card.getColor()) {
                                                case 0: // 오즈
                                                    if (oc.getFire() > 0) {
                                                        possibleCards.add(card);
                                                    }
                                                    break;
                                                case 1: // 미하일
                                                    if (oc.getFire() > 0 || oc.getLastCard().getColor() == 1) {
                                                        possibleCards.add(card);
                                                    }
                                                    break;
                                                case 2: // 호크아이
                                                    if (oc.getLastCard().getColor() == card.getColor() && oc.getFire() == 0) {
                                                        possibleCards.add(card);
                                                    }
                                                    break;
                                                case 3: // 이리나
                                                    if (oc.getLastCard().getColor() == card.getColor() && oc.getFire() == 0) {
                                                        possibleCards.add(card);
                                                    }
                                                    break;
                                                case 4: // 이카르트
                                                    possibleCards.add(card);
                                                    break;
                                            }
                                        }
                                    }
                                }

                                if (oc.getOneCardDeckInfo().size() == 0 || oc.getOneCardDeckInfo().isEmpty()) {
                                    oc.resetDeck();
                                    oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(0, 0, 0, false));
                                    oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onUserPossibleAction(nextPlayer, possibleCards, (possibleCards.size() == 0 && nextPlayer.getCards().size() == 16) || nextPlayer.getCards().size() < 16, oc.isbClockWiseTurn()));
                                } else {
                                    oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onUserPossibleAction(nextPlayer, possibleCards, (possibleCards.size() == 0 && nextPlayer.getCards().size() == 16) || nextPlayer.getCards().size() < 16, oc.isbClockWiseTurn()));
                                }

                                nextPlayer.getPlayer().getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/myturn"));
                                nextPlayer.getPlayer().getClient().getSession().writeAndFlush(CWvsContext.enableActions(nextPlayer.getPlayer()));
                                nextPlayer.getPlayer().getClient().getSession().writeAndFlush(SLFCGPacket.onShowText("당신의 턴입니다."));
                            }

                            if (oc.getOneCardTimer() != null) {
                                oc.getOneCardTimer().cancel(false);
                            }

                            oc.setOneCardTimer(Timer.ShowTimer.getInstance().schedule(() -> {
                                //15초 잠수시 강제로 카드 맥이고 넘어가야함
                                oc.skipPlayer();
                            }, 15 * 1000));
                        }
                        break;
                    }
                    case 1: {
                        OneCardGame oc = chr.getOneCardInstance();
                        OneCardPlayer ocp = oc.getPlayer(chr);
                        List<OneCard> newcards = new ArrayList<>();

                        if (oc.getFire() > 0) {
                            oc.sendPacketToPlayers(SLFCGPacket.onShowText(chr.getName() + "님이 " + oc.getFire() + "의 피해를 입었습니다."));
                        }
                        for (int i = 0; i < (oc.getFire() > 0 ? oc.getFire() : 1); ++i) {
                            if (oc.getOneCardDeckInfo().size() == 0) {
                                oc.resetDeck();
                                if (oc.getOneCardDeckInfo().size() == 0) {
                                    break;
                                }
                            }
                            int num = Randomizer.nextInt(oc.getOneCardDeckInfo().size());
                            OneCard card = oc.getOneCardDeckInfo().get(num);
                            ocp.getCards().add(card);
                            newcards.add(card);
                            oc.getOneCardDeckInfo().remove(num);
                        }
                        oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onGetCardResult(ocp, newcards));

                        if (ocp.getCards().size() >= 17) { // 파산
                            oc.setFire(0);
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/gameover"));
                            chr.getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/gameover"));
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(5, 0, chr.getId(), true));
                            oc.playerDead(ocp, false);
                        } else if (oc.getFire() > 0) {
                            oc.setFire(0);
                            oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(4, 0, chr.getId(), false));
                        }

                        OneCardPlayer nextPlayer = oc.setNextPlayer(oc.getLastPlayer(), oc.isbClockWiseTurn());

                        oc.setLastPlayer(nextPlayer);

                        if (nextPlayer != null) {
                            List<OneCard> possibleCards = new ArrayList<>();
                            if (oc.getLastCard().getColor() == 4) {
                                for (OneCard card : nextPlayer.getCards()) {
                                    possibleCards.add(card);
                                }
                            } else {
                                for (OneCard card : nextPlayer.getCards()) {
                                    if (card.getType() <= 11) {
                                        if ((card.getColor() == oc.getLastCard().getColor() || card.getType() == oc.getLastCard().getType())) {
                                            possibleCards.add(card);
                                        }
                                    } else {
                                        switch (card.getColor()) {
                                            case 0: // 오즈
                                                if (oc.getLastCard().getColor() == card.getColor()) {
                                                    possibleCards.add(card);
                                                }
                                                break;
                                            case 1: // 미하일
                                                possibleCards.add(card);
                                                break;
                                            case 2: // 호크아이
                                                if (oc.getLastCard().getColor() == card.getColor()) {
                                                    possibleCards.add(card);
                                                }
                                                break;
                                            case 3: // 이리나
                                                if (oc.getLastCard().getColor() == card.getColor()) {
                                                    possibleCards.add(card);
                                                }
                                                break;
                                            case 4: // 이카르트
                                                possibleCards.add(card);
                                                break;
                                        }
                                    }
                                }
                            }

                            if (oc.getOneCardDeckInfo().size() == 0 || oc.getOneCardDeckInfo().isEmpty()) {
                                oc.resetDeck();
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(0, 0, 0, false));
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onUserPossibleAction(nextPlayer, possibleCards, nextPlayer.getCards().size() < 17, oc.isbClockWiseTurn()));
                            } else {
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onUserPossibleAction(nextPlayer, possibleCards, nextPlayer.getCards().size() < 17, oc.isbClockWiseTurn()));
                            }

                            nextPlayer.getPlayer().getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/myturn"));
                            nextPlayer.getPlayer().getClient().getSession().writeAndFlush(CWvsContext.enableActions(nextPlayer.getPlayer()));

                            if (oc.getOneCardTimer() != null) {
                                oc.getOneCardTimer().cancel(false);
                            }

                            oc.setOneCardTimer(Timer.ShowTimer.getInstance().schedule(() -> {
                                //15초 잠수시 강제로 카드 맥이고 넘어가야함
                                oc.skipPlayer();
                            }, 15 * 1000));
                        }
                        break;
                    }
                    case 2: {
                        byte color = slea.readByte();
                        OneCardGame oc = chr.getOneCardInstance();

                        oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onChangeColorResult(oc.getLastCard().getType() == 12, color));

                        OneCardPlayer nextPlayer = oc.setNextPlayer(oc.getLastPlayer(), oc.isbClockWiseTurn());

                        oc.getLastCard().setColor(color);

                        oc.setLastPlayer(nextPlayer);

                        if (nextPlayer != null) {

                            List<OneCard> possibleCards = new ArrayList<>();
                            if (oc.getLastCard().getColor() == 4) {
                                for (OneCard card : nextPlayer.getCards()) {
                                    possibleCards.add(card);
                                }
                            } else {
                                for (OneCard card : nextPlayer.getCards()) {
                                    if (card.getType() <= 11) {
                                        if (card.getColor() == oc.getLastCard().getColor() || card.getType() == oc.getLastCard().getType()) {
                                            possibleCards.add(card);
                                        }
                                    } else {
                                        switch (card.getColor()) {
                                            case 0: // 오즈
                                                if (oc.getLastCard().getColor() == card.getColor()) {
                                                    possibleCards.add(card);
                                                }
                                                break;
                                            case 1: // 미하일
                                                possibleCards.add(card);
                                                break;
                                            case 2: // 호크아이
                                                if (oc.getLastCard().getColor() == card.getColor()) {
                                                    possibleCards.add(card);
                                                }
                                                break;
                                            case 3: // 이리나
                                                if (oc.getLastCard().getColor() == card.getColor()) {
                                                    possibleCards.add(card);
                                                }
                                                break;
                                            case 4: // 이카르트
                                                possibleCards.add(card);
                                                break;
                                        }
                                    }
                                }
                            }

                            if (oc.getOneCardDeckInfo().size() == 0 || oc.getOneCardDeckInfo().isEmpty()) {
                                oc.resetDeck();
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEffectResult(0, 0, 0, false));
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onUserPossibleAction(nextPlayer, possibleCards, nextPlayer.getCards().size() < 17, oc.isbClockWiseTurn()));
                            } else {
                                oc.sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onUserPossibleAction(nextPlayer, possibleCards, nextPlayer.getCards().size() < 17, oc.isbClockWiseTurn()));
                            }

                            nextPlayer.getPlayer().getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/myturn"));
                            nextPlayer.getPlayer().getClient().getSession().writeAndFlush(CWvsContext.enableActions(nextPlayer.getPlayer()));

                            if (oc.getOneCardTimer() != null) {
                                oc.getOneCardTimer().cancel(false);
                            }

                            oc.setOneCardTimer(Timer.ShowTimer.getInstance().schedule(() -> {
                                //15초 잠수시 강제로 카드 맥이고 넘어가야함
                                oc.skipPlayer();
                            }, 15 * 1000));
                        }
                        break;
                    }
                }
                break;
            }
            case ONECARD_EMOTION: {
                slea.skip(4);
                int emotionId = slea.readInt();
                chr.getOneCardInstance().sendPacketToPlayers(SLFCGPacket.OneCardGamePacket.onEmotion(chr.getId(), emotionId));
                chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
                break;
            }
            default: {
                //some idiots try to send huge amounts of data to this (:
                System.out.println("Unhandled interaction action by " + chr.getName() + " : " + action + ", " + slea.toString());
                //19 (0x13) - 00 OR 01 -> itemid(maple leaf) ? who knows what this is
                break;
            }
        }
    }

    public static void minigameOperation(LittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        switch (type) {
            case 185: {
                Point pos = new Point(slea.readInt(), slea.readInt());
                if (c.getPlayer().getBattleReverseInstance() != null) {
                    c.getPlayer().getBattleReverseInstance().sendPlaceStone(c.getPlayer(), pos);
                } else {
                    c.getPlayer().warp(ServerConstants.WarpMap);
                    c.getPlayer().dropMessage(5, "오류가 발생하여 게임이 취소됩니다.");
                }
                break;
            }
        }
    }

    public static final byte getResult(byte rps1, byte rps2) {
        switch (rps1) {
            case 0: {
                if (rps2 == 1) {
                    return 2;
                }
                if (rps2 == 2) {
                    return 0;
                }
                break;
            }
            case 1: {
                if (rps2 == 2) {
                    return 2;
                }
                if (rps2 == 0) {
                    return 0;
                }
                break;
            }
            case 2: {
                if (rps2 == 0) {
                    return 2;
                }
                if (rps2 == 1) {
                    return 0;
                }
                break;
            }
        }
        return 1;
    }
}
