/*






 */
package server.shops;

import client.MapleClient;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.StringUtil;
import tools.packet.CField.NPCPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapleShop {

    private static final Set<Integer> rechargeableItems = new LinkedHashSet<Integer>();
    private int id;
    private int npcId;
    private int coinKey;
    private int questEx;
    private String saleString, shopString;
    private List<MapleShopItem> items;

    static {
        rechargeableItems.add(2070000);
        rechargeableItems.add(2070001);
        rechargeableItems.add(2070002);
        rechargeableItems.add(2070003);
        rechargeableItems.add(2070004);
        rechargeableItems.add(2070005);
        rechargeableItems.add(2070006);
        rechargeableItems.add(2070007);
        rechargeableItems.add(2070008);
        rechargeableItems.add(2070009);
        rechargeableItems.add(2070010);
        rechargeableItems.add(2070011);
        rechargeableItems.add(2070012);
        rechargeableItems.add(2070013);
        rechargeableItems.add(2070023); //플레임 표창
        rechargeableItems.add(2070024);    // 무한의 수리검
        rechargeableItems.add(2070026);

        rechargeableItems.add(2330000);
        rechargeableItems.add(2330001);
        rechargeableItems.add(2330002);
        rechargeableItems.add(2330003);
        rechargeableItems.add(2330004);
        rechargeableItems.add(2330005);
        rechargeableItems.add(2330008);
        rechargeableItems.add(2330016);

        rechargeableItems.add(2331000); // Capsules
        rechargeableItems.add(2332000); // Capsules
    }

    /**
     * Creates a new instance of MapleShop
     */
    public MapleShop(int id, int npcId, int coinKey, int questEx, String shopString, String saleString) {
        this.id = id;
        this.npcId = npcId;
        this.coinKey = coinKey;
        this.questEx = questEx;
        this.shopString = shopString;
        this.saleString = saleString;
        items = new LinkedList<MapleShopItem>();
    }

    public void addItem(MapleShopItem item) {
        items.add(item);
    }

    public void sendShop(MapleClient c) {
        sendShop(c, 0);
    }

    public void sendShop(MapleClient c, int id2) {
        if (items == null) {
            System.out.println("상점에 아무정보가 없습니다.");
            return;
        }
        c.getPlayer().setShop(this);
        c.getSession().writeAndFlush(NPCPacket.getNPCShop(id2 == 0 ? getNpcId() : id2, this, c));

    }

    public List<MapleShopItem> getItems() {
        return items;
    }

    public void buy(MapleClient c, int itemId, short quantity, short position) {

        MapleShopItem item = findById(itemId, position);
        if (item == null || item.getItemId() != itemId) {
            c.getPlayer().dropMessage(1, "아이템 정보를 불러오는 도중 오류가 발생했습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (item.getPrice() < 0 || item.getPrice() > 2147483647) {
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(2, "아이템 수량 오류(핵의심) 가 발생했습니다. 발생유저 : " + c.getPlayer().getName()));
            //c.getPlayer().dropMessage(2, "아이템 수량 오류(핵의심) 가 발생했습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (item != null && item.getPrice() > 0 && item.getPriceQuantity() == 0 && coinKey == 0) {
            if (c.getPlayer().getMeso() >= item.getPrice() * quantity) {
                if (MapleInventoryManipulator.checkSpace(c, itemId, (short) (quantity * item.getQuantity()), "")) {
                    if (GameConstants.isPet(itemId)) {
                        MapleInventoryManipulator.addById(c, itemId, (short) (quantity * item.getQuantity()), null, MaplePet.createPet(itemId, item.getPeriod()), 0, new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구매한 아이템.").toString());
                    } else {
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        if (GameConstants.isRechargable(itemId)) {
                            quantity = ii.getSlotMax(item.getItemId());
                            c.getPlayer().gainMeso(-(item.getPrice()), false);
                            MapleInventoryManipulator.addById(c, itemId, (short) (quantity * item.getQuantity()), null, null, item.getPeriod(), "");
                        } else {
                            c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
                            c.getPlayer().gainItem(itemId, (short) (item.getQuantity() * quantity), false, item.getPeriod() <= 0 ? 0 : ((item.getPeriod() * 60L * 1000L) + System.currentTimeMillis()), "상점에서 구입한 아이템");
                        }
                    }
                    c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 0, this, c, -1, itemId));
                } else {
                    c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 10, this, c, -1, itemId));
                }
            } else {
                c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 2, this, c, -1, itemId));
            }
        } else if (item != null && item.getPrice() > 0 && item.getPriceQuantity() > 0) {
            if (c.getPlayer().haveItem((int) item.getPrice(), item.getPriceQuantity() * quantity, false, true)) {
                if (MapleInventoryManipulator.checkSpace(c, itemId, (short) (quantity * item.getQuantity()), "")) {
                    if (GameConstants.isPet(itemId)) {
                        MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType((int) item.getPrice()), (int) item.getPrice(), item.getPriceQuantity(), false, false);
                        MapleInventoryManipulator.addById(c, itemId, (short) (quantity * item.getQuantity()), null, MaplePet.createPet(itemId, item.getPeriod()), 30, new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구매한 아이템.").toString());
                    } else {
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        if (GameConstants.isRechargable(itemId)) {
                            quantity = ii.getSlotMax(item.getItemId());
                            MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType((int) item.getPrice()), (int) item.getPrice(), item.getPriceQuantity(), false, false);
                            MapleInventoryManipulator.addById(c, itemId, (short) (quantity * item.getQuantity()), null, null, item.getPeriod(), new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구매한 아이템.").toString());
                        } else {
                            MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType((int) item.getPrice()), (int) item.getPrice(), item.getPriceQuantity() * quantity, false, false);
                            if (GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP) {
                                for (int i = 0; i < quantity; ++i) {
                                    c.getPlayer().gainItem(itemId, (short) 1, false, item.getPeriod() <= 0 ? 0 : ((item.getPeriod() * 60L * 1000L) + System.currentTimeMillis()), new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구입한 아이템").toString());
                                }
                            } else {
                                c.getPlayer().gainItem(itemId, (short) (item.getQuantity() * quantity), false, item.getPeriod() <= 0 ? 0 : ((item.getPeriod() * 60L * 1000L) + System.currentTimeMillis()), new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구입한 아이템").toString());
                            }
                        }
                    }
                    c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 0, this, c, -1, itemId));
                } else {
                    c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 10, this, c, -1, itemId));
                }
            } else {
                c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 1, this, c, -1, itemId));
            }
        } else if (coinKey != 0 && item != null && item.getPrice() > 0) {
            if (item.getPrice() >= 0 && c.getPlayer().getKeyValue(coinKey, "point") >= item.getPrice()) {
                if (MapleInventoryManipulator.checkSpace(c, itemId, (short) (quantity * item.getQuantity()), "")) {
                    c.getPlayer().setKeyValue(coinKey, "point", String.valueOf(c.getPlayer().getKeyValue(coinKey, "point") - item.getPrice()));
                    if (coinKey == 16180) {
                        c.getPlayer().gainDonationPoint((int) -item.getPrice());
                    }
                    if (GameConstants.isPet(itemId)) {
                        MapleInventoryManipulator.addById(c, itemId, (short) (quantity * item.getQuantity()), null, MaplePet.createPet(itemId, item.getPeriod()), 0, "");
                    } else {
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        if (GameConstants.isRechargable(itemId)) {
                            quantity = ii.getSlotMax(item.getItemId());
                            c.getPlayer().gainMeso(-(item.getPrice()), false);
                            MapleInventoryManipulator.addById(c, itemId, (short) (quantity * item.getQuantity()), null, null, item.getPeriod(), new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구매한 아이템.").toString());
                        } else {
                            if (GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP) {
                                for (int i = 0; i < quantity; ++i) {
                                    c.getPlayer().gainItem(itemId, (short) 1, false, item.getPeriod() <= 0 ? 0 : ((item.getPeriod() * 60L * 1000L) + System.currentTimeMillis()), new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구입한 아이템").toString());
                                }
                            } else {
                                c.getPlayer().gainItem(itemId, (short) (item.getQuantity() * quantity), false, item.getPeriod() <= 0 ? 0 : ((item.getPeriod() * 60L * 1000L) + System.currentTimeMillis()), new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(this.id + " 상점에서 구입한 아이템").toString());
                            }
                        }
                    }
                    c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 0, this, c, -1, itemId));
                } else {
                    c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 10, this, c, -1, itemId));
                }
            } else {
                c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 3, this, c, -1, itemId));
            }
            return;
        }
    }

    public void sell(MapleClient c, MapleInventoryType type, short slot, short quantity) {
        if (quantity <= 0) {
            quantity = 1;
        }
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null) {
            return;
        }
        if (item.getType() == 1) {
            Equip eq = (Equip) item;
            if (eq.getEnchantBuff() > 0) {
                c.getPlayer().dropMessage(1, "장비의 흔적은 이동이 불가합니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
        }
        if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
            quantity = item.getQuantity();
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (quantity <= iQuant && iQuant > 0) {
            Item itemm = item.copy();
            itemm.setQuantity((short) quantity);
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            if (itemm.getReward() != null) {
                if (c.getPlayer().getKeyValue(1068, "count") <= 0) {
                    System.out.println(c.getPlayer() + "결정석 핵 쓰려고 시도함.");
                    c.disconnect(true, false, false);
                    c.getSession().close();
                    return;
                } else {
                    c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
                }
            }
            //c.getPlayer().getInventory(type).removeItem(slot, quantity, false);
            double price;
            if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(item.getItemId());
            } else if (itemm.getReward() != null) {
                price = itemm.getReward().getPrice();
                quantity = 1;
            } else {
                price = ii.getPrice(item.getItemId());
            }
            final long recvMesos = (long) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 11, this, c, -1, item.getItemId()));
        }
    }

    public void recharge(final MapleClient c, final short slot) {
        final Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (item == null || (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId()))) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        short slotMax = ii.getSlotMax(item.getItemId());
        final int skill = GameConstants.getMasterySkill(c.getPlayer().getJob());

        if (skill != 0) {
            slotMax += c.getPlayer().getSkillLevel(SkillFactory.getSkill(skill)) * 10;
        }
        if (item.getQuantity() < slotMax) {
            final int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(MapleInventoryType.USE, (Item) item, false));
                c.getPlayer().gainMeso(-price, false, true);
                c.getSession().writeAndFlush(NPCPacket.confirmShopTransactionItem((byte) 0, this, c, -1, item.getItemId()));
            }
        }
    }

    protected MapleShopItem findById(int itemId, int position) {
        for (MapleShopItem item : items) {
            if (item.getItemId() == itemId && item.getPosition() == position) {
                return item;
            }
        }
        return null;
    }

    public static MapleShop createFromDB(int id, boolean isShopId) {
        MapleShop ret = null;
        int shopId;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(isShopId ? "SELECT * FROM shops WHERE shopid = ?" : "SELECT * FROM shops WHERE npcid = ?");

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new MapleShop(shopId, rs.getInt("npcid"), rs.getInt("coinKey"), rs.getInt("questEx"), rs.getString("shopString"), rs.getString("saleString"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<Integer>(rechargeableItems);
            int i = 1;
            while (rs.next()) {
                if (GameConstants.isThrowingStar(rs.getInt("itemid")) || GameConstants.isBullet(rs.getInt("itemid"))) {
                    MapleShopItem starItem = new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("pricequantity"), (byte) rs.getInt("Tab"), rs.getInt("quantity"), rs.getInt("period"), i, rs.getInt("itemrate"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("pricequantity"), (byte) rs.getInt("Tab"), rs.getInt("quantity"), rs.getInt("period"), i, rs.getInt("itemrate")));
                }
                i++;
            }
            i = 1;
            for (Integer recharge : recharges) {
                ret.addItem(new MapleShopItem((short) 1000, recharge.intValue(), 0, 0, (byte) 0, 0, 0, i, 0));
                i++;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Could not load shop" + e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    Logger.getLogger(MapleShop.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }

    public int getCoinKey() {
        return coinKey;
    }

    public void setCoinKey(int coinKey) {
        this.coinKey = coinKey;
    }

    public String getSaleString() {
        return saleString;
    }

    public void setSaleString(String saleString) {
        this.saleString = saleString;
    }

    public String getShopString() {
        return shopString;
    }

    public void setShopString(String shopString) {
        this.shopString = shopString;
    }

    public int getQuestEx() {
        return questEx;
    }

    public void setQuestEx(int questEx) {
        this.questEx = questEx;
    }
}
