package server;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessor;
import constants.GameConstants;
import constants.ServerConstants.CommandType;
import handling.world.World;
import log.DBLogger;
import log.LogType;
import tools.StringUtil;
import tools.packet.CField.InteractionPacket;
import tools.packet.CWvsContext;
import tools.packet.PlayerShopPacket;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class MapleTrade {

    private MapleTrade partner = null;
    private final List<Item> items = new LinkedList<Item>();
    private List<Item> exchangeItems;
    private long meso = 0, exchangeMeso = 0;
    private boolean locked = false, inTrade = false;
    private final WeakReference<MapleCharacter> chr;
    private final byte tradingslot;
    private byte rps = 0;

    public MapleTrade(final byte tradingslot, final MapleCharacter chr) {
        this.tradingslot = tradingslot;
        this.chr = new WeakReference<MapleCharacter>(chr);
    }

    public final void CompleteTrade() {
        if (exchangeItems != null) { // just to be on the safe side...
            List<Item> itemz = new LinkedList<Item>(exchangeItems);
            for (final Item item : itemz) {
                item.setGMLog(new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(partner.getChr().getName() + "과의 교환으로 얻은 아이템.").toString());
//            	if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                if (ItemFlag.KARMA_EQUIP.check(item.getFlag())) {
                    item.setFlag(item.getFlag() - ItemFlag.KARMA_EQUIP.getValue());
                }
                if (ItemFlag.KARMA_USE.check(item.getFlag())) {
                    item.setFlag(item.getFlag() - ItemFlag.KARMA_USE.getValue());
                }
//            	}
                MapleInventoryManipulator.addbyItem(chr.get().getClient(), item, false, false);
            }

            exchangeItems.clear();
        }
        if (exchangeMeso > 0) {
            chr.get().gainMeso(exchangeMeso - GameConstants.getTaxAmount(exchangeMeso), false, false);
        }
        exchangeMeso = 0;

        chr.get().getClient().getSession().writeAndFlush(InteractionPacket.TradeMessage(tradingslot, (byte) 0x07));
    }

    public final void cancel(final MapleClient c, final MapleCharacter chr) {
        if (items != null) { // just to be on the safe side...
            List<Item> itemz = new LinkedList<Item>(items);
            for (final Item item : itemz) {
                MapleInventoryManipulator.addbyItem(c, item, true, false);
            }
            items.clear();
        }
        if (meso > 0) {
            chr.gainMeso(meso, false, false);
        }
        meso = 0;

        c.getSession().writeAndFlush(InteractionPacket.getTradeCancel(tradingslot));
    }

    public final boolean isLocked() {
        return locked;
    }

    public final void setMeso(final long meso) {
        if (locked || partner == null || meso <= 0 || this.meso + meso <= 0) {
            return;
        }
        if (chr.get().getMeso() >= meso) {
            chr.get().gainMeso(-meso, false, false);
            this.meso += meso;
            chr.get().getClient().getSession().writeAndFlush(InteractionPacket.getTradeMesoSet((byte) 0, this.meso));
            if (partner != null) {
                partner.getChr().getClient().getSession().writeAndFlush(InteractionPacket.getTradeMesoSet((byte) 1, this.meso));
            }
        }
    }

    public final void addItem(final Item item) {
        if (locked || partner == null) {
            return;
        }
        items.add(item);
        chr.get().getClient().getSession().writeAndFlush(InteractionPacket.getTradeItemAdd((byte) 0, item));
        if (partner != null) {
            partner.getChr().getClient().getSession().writeAndFlush(InteractionPacket.getTradeItemAdd((byte) 1, item));
        }
    }

    public final void chat(final String message) {
        if (!CommandProcessor.processCommand(chr.get().getClient(), message, CommandType.TRADE)) {
            chr.get().getClient().getSession().writeAndFlush(PlayerShopPacket.shopChat(chr.get().getName(), chr.get().getId(), message, chr.get().getTrade().tradingslot));
            if (partner != null) {
                partner.getChr().getClient().getSession().writeAndFlush(PlayerShopPacket.shopChat(chr.get().getName(), chr.get().getId(), message, chr.get().getTrade().tradingslot));
            }
        }

        DBLogger.getInstance().logChat(LogType.Chat.Trade, chr.get().getId(), chr.get().getName(), message, "수신 : " + partner.getChr().getName());

        if (chr.get().getClient().isMonitored()) { //Broadcast info even if it was a command.
            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, chr.get().getName(), chr.get().getName() + " said in trade with " + partner.getChr().getName() + ": " + message));
        } else if (partner != null && partner.getChr() != null && partner.getChr().getClient().isMonitored()) {
            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, chr.get().getName(), chr.get().getName() + " said in trade with " + partner.getChr().getName() + ": " + message));
        }
    }

    public final void chatAuto(final String message) {
        chr.get().dropMessage(-2, message);
        if (partner != null) {
            partner.getChr().getClient().getSession().writeAndFlush(PlayerShopPacket.shopChat(partner.getChr().getName(), partner.getChr().getId(), message, 1));
        }
        if (chr.get().getClient().isMonitored()) { //Broadcast info even if it was a command.
            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, chr.get().getName(), chr.get().getName() + " said in trade [Automated] with " + partner.getChr().getName() + ": " + message));
        } else if (partner != null && partner.getChr() != null && partner.getChr().getClient().isMonitored()) {
            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, chr.get().getName(), chr.get().getName() + " said in trade [Automated] with " + partner.getChr().getName() + ": " + message));
        }
    }

    public final MapleTrade getPartner() {
        return partner;
    }

    public final void setPartner(final MapleTrade partner) {
        if (locked) {
            return;
        }
        this.partner = partner;
    }

    public final MapleCharacter getChr() {
        return chr.get();
    }

    public final int getNextTargetSlot() {
        if (items.size() >= 9) {
            return -1;
        }
        int ret = 1; //first slot
        for (Item item : items) {
            if (item.getPosition() == ret) {
                ret++;
            }
        }
        return ret;
    }

    public boolean inTrade() {
        return inTrade;
    }

    public final boolean setItems(final MapleClient c, final Item item, byte targetSlot, final int quantity) {
        int target = getNextTargetSlot();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (partner == null || target == -1 || isLocked() || (GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP && quantity != 1)) {
            return false;
        }
        final int flag = item.getFlag();
        if (ItemFlag.LOCK.check(flag)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return false;
        }
        if (ItemFlag.UNTRADEABLE.check(flag) || GameConstants.isPet(item.getItemId()) || ii.isDropRestricted(item.getItemId()) || ii.isAccountShared(item.getItemId())) {
            if (!(ItemFlag.KARMA_EQUIP.check(flag) || ItemFlag.KARMA_USE.check(flag))) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return false;
            }
        }
        if (item.getType() == 1) {
            Equip equip = (Equip) item;
            if ((equip.getEnchantBuff() & 0x88) != 0) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                c.getPlayer().dropMessage(1, "장비의 흔적은 교환하실 수 없습니다.");
                return false;
            }
        }
        Item tradeItem = item.copy();
        if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
            tradeItem.setQuantity(item.getQuantity());
            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(item.getItemId()), item.getPosition(), item.getQuantity(), true);
        } else {
            tradeItem.setQuantity((short) quantity);
            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(item.getItemId()), item.getPosition(), (short) quantity, true);
        }
        if (targetSlot < 0) {
            targetSlot = (byte) target;
        } else {
            for (Item itemz : items) {
                if (itemz.getPosition() == targetSlot) {
                    targetSlot = (byte) target;
                    break;
                }
            }
        }
        tradeItem.setPosition(targetSlot);
        addItem(tradeItem);
        return true;
    }

    private final int check() { //0 = fine, 1 = invent space not, 2 = pickupRestricted
        if (chr.get().getMeso() + exchangeMeso < 0) {
            return 1;
        }

        if (exchangeItems != null) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0, decoration = 0;
            for (final Item item : exchangeItems) {
                switch (GameConstants.getInventoryType(item.getItemId())) {
                    case EQUIP:
                        eq++;
                        break;
                    case USE:
                        use++;
                        break;
                    case SETUP:
                        setup++;
                        break;
                    case ETC:
                        etc++;
                        break;
                    case CASH: // Not allowed, probably hacking
                        cash++;
                    case DECORATION:
                        decoration++;
                        break;
                }
                if (ii.isPickupRestricted(item.getItemId()) && chr.get().haveItem(item.getItemId(), 1, true, true)) {
                    return 2;
                }
            }

            if (chr.get().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.get().getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.get().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.get().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.get().getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash || chr.get().getInventory(MapleInventoryType.DECORATION).getNumFreeSlot() < decoration) {
                return 1;
            }
        }

        return 0;
    }

    public final static void completeTrade(final MapleCharacter c) {
        final MapleTrade local = c.getTrade();
        final MapleTrade partner = local.getPartner();

        if (partner == null || local.locked) {
            return;
        }
        local.locked = true; // Locking the trade
        partner.getChr().getClient().getSession().writeAndFlush(InteractionPacket.getTradeConfirmation());

        partner.exchangeItems = new LinkedList<Item>(local.items); // Copy this to partner's trade since it's alreadt accepted
        partner.exchangeMeso = local.meso; // Copy this to partner's trade since it's alreadt accepted

        if (partner.isLocked()) { // Both locked
            int lz = local.check(), lz2 = partner.check();
            if (lz == 0 && lz2 == 0) {
                local.CompleteTrade();
                partner.CompleteTrade();
            } else {
                // NOTE : IF accepted = other party but inventory is full, the item is lost.
                partner.cancel(partner.getChr().getClient(), partner.getChr());
                local.cancel(c.getClient(), c);
            }
            partner.getChr().setTrade(null);
            c.setTrade(null);
        }
    }

    public static final void cancelTrade(final MapleTrade Localtrade, final MapleClient c, final MapleCharacter chr) {
        Localtrade.cancel(c, chr);

        final MapleTrade partner = Localtrade.getPartner();
        if (partner != null && partner.getChr() != null) {
            partner.cancel(partner.getChr().getClient(), partner.getChr());
            partner.getChr().setTrade(null);
        }
        chr.setTrade(null);
    }

    public static final void startTrade(MapleCharacter c, boolean isTrade) {
        if (c.getTrade() == null) {
            c.setTrade(new MapleTrade((byte) 0, c));
            c.getClient().getSession().writeAndFlush(InteractionPacket.getTradeStart(c.getClient(), c.getTrade(), (byte) 0, isTrade));
            c.isTrade = isTrade;
        } else {
            c.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(5, "", "다른 유저와 교환중인 유저입니다."));
        }
    }

    public static final void startCashTrade(final MapleCharacter c) {
        if (c.getTrade() == null) {
            c.setTrade(new MapleTrade((byte) 0, c));
            c.getClient().getSession().writeAndFlush(InteractionPacket.getCashTradeStart(c.getClient(), c.getTrade(), (byte) 0));
        } else {
            c.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(5, "", "다른 유저와 교환중인 유저입니다."));
        }
    }

    public static final void inviteTrade(MapleCharacter c1, MapleCharacter c2, boolean isTrade) {
        if ((c1 == null) || (c1.getTrade() == null)) {
            return;
        }
        if ((c2 != null) && (c2.getTrade() == null)) {
            c2.setTrade(new MapleTrade((byte) 1, c2));
            c2.getTrade().setPartner(c1.getTrade());
            c1.getTrade().setPartner(c2.getTrade());
            c2.getClient().getSession().writeAndFlush(InteractionPacket.getTradeInvite(c1, isTrade));
        } else {
            c1.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(5, "", "다른 유저와 교환중인 유저입니다."));
            cancelTrade(c1.getTrade(), c1.getClient(), c1);
        }
    }

    public static final void inviteCashTrade(final MapleCharacter c1, final MapleCharacter c2) {
        if (c1 == null || c1.getTrade() == null) {
            return;
        }
        if (c2 != null && c2.getTrade() == null) {
            c2.setTrade(new MapleTrade((byte) 1, c2));
            c2.getTrade().setPartner(c1.getTrade());
            c1.getTrade().setPartner(c2.getTrade());
            c2.getClient().getSession().writeAndFlush(InteractionPacket.getCashTradeInvite(c1));
        } else {
            c1.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(5, "", "다른 유저와 교환중인 유저입니다."));
            cancelTrade(c1.getTrade(), c1.getClient(), c1);
        }
    }

    public static final void visitTrade(final MapleCharacter c1, final MapleCharacter c2, boolean isTrade) {
        if (c2 != null && c1.getTrade() != null && c1.getTrade().getPartner() == c2.getTrade() && c2.getTrade() != null && c2.getTrade().getPartner() == c1.getTrade()) {
            // We don't need to check for map here as the user is found via MapleMap.getCharacterById()
            c1.getTrade().inTrade = true;
            c2.getClient().getSession().writeAndFlush(PlayerShopPacket.shopVisitorAdd(c1, 1));
            c1.getClient().getSession().writeAndFlush(InteractionPacket.getTradeStart(c1.getClient(), c1.getTrade(), (byte) 1, isTrade));
        } else {
            c1.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(5, "", "다른 유저와 교환중인 유저입니다."));
        }
    }

    public static final void visitCashTrade(final MapleCharacter c1, final MapleCharacter c2) {
        if (c2 != null && c1.getTrade() != null && c1.getTrade().getPartner() == c2.getTrade() && c2.getTrade() != null && c2.getTrade().getPartner() == c1.getTrade()) {
            // We don't need to check for map here as the user is found via MapleMap.getCharacterById()
            c1.getTrade().inTrade = true;
            c2.getClient().getSession().writeAndFlush(PlayerShopPacket.shopVisitorAdd(c1, 1));
            c1.getClient().getSession().writeAndFlush(InteractionPacket.getCashTradeStart(c1.getClient(), c1.getTrade(), (byte) 1));
        } else {
            c1.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(5, "", "다른 유저와 교환중인 유저입니다."));
        }
    }

    public static final void declineTrade(final MapleCharacter c) {
        final MapleTrade trade = c.getTrade();
        if (trade != null) {
            if (trade.getPartner() != null) {
                MapleCharacter other = trade.getPartner().getChr();
                if (other != null && other.getTrade() != null) {
                    other.getTrade().cancel(other.getClient(), other);
                    other.setTrade(null);
                    other.dropMessage(5, c.getName() + "님이 교환을 취소했습니다.");
                }
            }
            trade.cancel(c.getClient(), c);
            c.setTrade(null);
        }
    }

    public byte getPRS() {
        return rps;
    }

    public void setRPS(byte rps) {
        this.rps = rps;
    }
}
