package server;

import client.*;
import client.MapleTrait.MapleTraitType;
import client.inventory.*;
import client.inventory.EquipAdditions.RingSet;
import constants.GameConstants;
import java.awt.Point;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import database.DatabaseConnection;
import scripting.NPCConversationManager;
import server.maps.AramiaFireWorks;
import server.quest.MapleQuest;
import tools.StringUtil;
import tools.packet.CSPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;

public class MapleInventoryManipulator {

    public static void addRing(MapleCharacter chr, int itemId, int ringId, int sn, String partner) {
        CashItemInfo csi = CashItemFactory.getInstance().getItem(sn);
        if (csi == null) {
            return;
        }
        Item ring = chr.getCashInventory().toItem(csi, ringId);
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        chr.getCashInventory().addToInventory(ring);
        chr.getClient().getSession().writeAndFlush(CSPacket.sendBoughtRings(GameConstants.isCrushRing(itemId), ring, sn, chr.getClient().getAccID(), partner));
    }

    public static boolean addbyItem(final MapleClient c, final Item item) {
        return addbyItem(c, item, false, false) >= 0;
    }

    public static boolean addbyItem(final MapleClient c, final Item item, boolean fromcs) {
        return addbyItem(c, item, false, false) >= 0;
    }

    public static short addbyItem(final MapleClient c, final Item item, boolean sort, final boolean fromcs) {
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());
        final short newSlot = c.getPlayer().getInventory(type).addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
            }
            return newSlot;
        }
        if (GameConstants.isHarvesting(item.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }

        if (GameConstants.isPet(item.getItemId())) {
            //item.setExpiration(System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000)); // 30일
        }

        if (GameConstants.isArcaneSymbol(item.getItemId()) && !sort) {
            final Equip equip = (Equip) item;
            if (equip.getArcLevel() == 0) {
                equip.setArc((short) 30);
                equip.setArcLevel((byte) 1);
                if (equip.getArcEXP() == 0)
                    equip.setArcEXP(1);
                if (GameConstants.isXenon(c.getPlayer().getJob())) {
                    equip.setStr((short) 117);
                    equip.setDex((short) 117);
                    equip.setLuk((short) 117);
                } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                    equip.setHp((short) 525);
                } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                    equip.setStr((short) 300);
                } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                    equip.setInt((short) 300);
                } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                    equip.setDex((short) 300);
                } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                    equip.setLuk((short) 300);
                } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                    equip.setStr((short) 300);
                }
            }
        }
        if (GameConstants.isAscenticSymbol(item.getItemId()) && !sort) {
            final Equip equip = (Equip) item;
            if (equip.getArcLevel() == 0) {
                equip.setArc((short) 10);
                equip.setArcLevel((byte) 1);
                equip.setArcEXP(1);
                if (GameConstants.isXenon(c.getPlayer().getJob())) {
                    equip.setStr((short) 195);
                    equip.setDex((short) 195);
                    equip.setLuk((short) 195);
                } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                    equip.setHp((short) 8750);
                } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                    equip.setStr((short) 500);
                } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                    equip.setInt((short) 500);
                } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                    equip.setDex((short) 500);
                } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                    equip.setLuk((short) 500);
                } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                    equip.setStr((short) 500);
                }
            }
        }

        if (item.getItemId() >= 1113098 && item.getItemId() <= 1113128 && !sort) {
            final Equip equip = (Equip) item;
            if (equip.getBaseLevel() == 0) {
                byte lvl = (byte) Randomizer.rand(1, 4);
                equip.setLevel(lvl);
            }
        }
        /*Connection con = null; //아이템 인벤토리아디때매/./
        List<Item> items = c.getPlayer().getInventory(type.getType()).newList();
        try {
            con = DatabaseConnection.getConnection();
            if (con != null) {
                new MapleCharacterSave(c.getPlayer()).saveInventory(con,true);
                con.close();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }*/

        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, item));
        c.getPlayer().havePartyQuest(item.getItemId());
        if (item.getItemId() == 4001886) {
            c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
        }
        if (item.getQuantity() >= 300 && !sort) {
            String data;
            data = c.getPlayer().getName() + " | " + item.getItemId() + " (x" + item.getQuantity() + ")를 addbyItem을 통해 얻음.\r\n";
            NPCConversationManager.writeLog("Log/Item.log", data, true);
        }

        return newSlot;
    }

    public static int getUniqueId(int itemId, MaplePet pet) {
        int uniqueid = -1;
        if (GameConstants.isPet(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = MapleInventoryIdentifier.getInstance();
            }
        } else if (GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || MapleItemInformationProvider.getInstance().isCash(itemId)) { //less work to do
            uniqueid = MapleInventoryIdentifier.getInstance(); //shouldnt be generated yet, so put it here
        }
        return uniqueid;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, gmLog, false);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addById(c, itemId, quantity, owner, null, 0, gmLog, false);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog, boolean special) {
        return addById(c, itemId, quantity, null, null, 0, gmLog, special);
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addId(c, itemId, quantity, owner, null, 0, gmLog, false);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, 0, gmLog, false);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog) {
        return addId(c, itemId, quantity, owner, pet, period, gmLog, false) >= 0;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog, boolean special) {
        return addId(c, itemId, quantity, owner, pet, period, gmLog, special) >= 0;
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog, boolean special) {
        if (quantity >= 300) {
            String data;
            data = c.getPlayer().getName() + " | " + itemId + " (x" + quantity + ")를 addbyId를 통해 얻음.\r\n";
            NPCConversationManager.writeLog("Log/Item.log", data, true);
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return -1;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP) && !type.equals(MapleInventoryType.DECORATION)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!GameConstants.isRechargable(itemId)) {
                if (!ii.isCash(itemId)) {
                    if (existing.size() > 0) { // first update all existing slots to slotMax except cash
                        Iterator<Item> i = existing.iterator();
                        while (quantity > 0) {
                            if (i.hasNext()) {
                                Item eItem = (Item) i.next();
                                short oldQ = eItem.getQuantity();
                                if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null) && eItem.getExpiration() == -1) {
                                    short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                    quantity -= (newQ - oldQ);
                                    eItem.setQuantity(newQ);
                                    int flag = eItem.getFlag();
                                    if (ii.isCash(itemId)) {
                                        if (GameConstants.isEquip(itemId)) {
                                            if (!ItemFlag.KARMA_EQUIP.check(flag)) {
                                                flag |= ItemFlag.KARMA_EQUIP.getValue();
                                            }
                                        } else {
                                            if (!ItemFlag.KARMA_USE.check(flag)) {
                                                flag |= ItemFlag.KARMA_USE.getValue();
                                            }
                                        }
                                    }
                                    eItem.setFlag(flag);
                                    c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(type, eItem, false));
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0, uniqueid);
                        newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                            c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                            return -1;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                        }
                        if (pet != null) {
                            nItem.setFlag(ItemFlag.KARMA_USE.getValue());
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                            // c.getPlayer().addPet(pet);
                        }

                        int flag = nItem.getFlag();
                        if (ii.isCash(itemId)) {
                            if (GameConstants.isEquip(itemId)) {
                                if (!ItemFlag.KARMA_EQUIP.check(flag)) {
                                    flag |= ItemFlag.KARMA_EQUIP.getValue();
                                }
                            } else {
                                if (!ItemFlag.KARMA_USE.check(flag)) {
                                    flag |= ItemFlag.KARMA_USE.getValue();
                                }
                            }
                        }
                        nItem.setFlag(flag);
                        /*   List<Item> items = c.getPlayer().getInventory(type.getType()).newList();
                        Connection con = null; //아이템 인벤토리아디때매/./
                        try {
                            con = DatabaseConnection.getConnection();
                            if (con != null) {
                                new MapleCharacterSave(c.getPlayer()).saveInventory(con,true);
                                con.close();
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }*/
                        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getPlayer().havePartyQuest(itemId);
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return (byte) newSlot;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0, uniqueid);
                newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }

                int flag = nItem.getFlag();
                if (ii.isCash(itemId)) {
                    if (ii.isCash(itemId)) {
                        if (GameConstants.isEquip(itemId)) {
                            if (!ItemFlag.KARMA_EQUIP.check(flag)) {
                                flag |= ItemFlag.KARMA_EQUIP.getValue();
                            }
                        } else {
                            if (!ItemFlag.KARMA_USE.check(flag)) {
                                flag |= ItemFlag.KARMA_USE.getValue();
                            }
                        }
                    }
                    nItem.setUniqueId(MapleInventoryIdentifier.getInstance());
                }
                nItem.setFlag(flag);

                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem));
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } else {
            if (quantity == 1) {
                final Item nEquip = ii.getEquipById(itemId, uniqueid);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) {
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }

                int flag = nEquip.getFlag();
                if (ii.isCash(itemId)) {
                    nEquip.setUniqueId(MapleInventoryIdentifier.getInstance());
                    if (GameConstants.isEquip(itemId)) {
                        if (!ItemFlag.KARMA_EQUIP.check(flag)) {
                            flag |= ItemFlag.KARMA_EQUIP.getValue();
                        }
                    } else {
                        if (!ItemFlag.KARMA_USE.check(flag)) {
                            flag |= ItemFlag.KARMA_USE.getValue();
                        }
                    }
                }
                nEquip.setFlag(flag);

                newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                /*  List<Item> items = c.getPlayer().getInventory(type.getType()).newList();
                Connection con = null; //아이템 인벤토리아디때매/./
                try {
                    con = DatabaseConnection.getConnection();
                    if (con != null) {
                        new MapleCharacterSave(c.getPlayer()).saveInventory(con,true);
                        con.close();
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }*/
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nEquip));
                if (GameConstants.isHarvesting(itemId)) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getPlayer().havePartyQuest(itemId);

        if (itemId == 4001886) {
            c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
        }

        if (itemId == 4310308) {
            int lock = c.getPlayer().isLockNeoCore() ? 1 : 0;
            c.getPlayer().updateInfoQuest(501215, "point=" + c.getPlayer().getItemQuantity(4310308, false) + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";;week=0;total=0;today=0;lock=" + lock + ""); //네오 코어
        }

        return (byte) newSlot;
    }

    public static Item addId_Item(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog, boolean special) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return null;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP) || !type.equals(MapleInventoryType.DECORATION)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!GameConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null) && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(type, eItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0, uniqueid);
                        newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                            c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                            return null;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                        }
                        if (pet != null) {
                            nItem.setFlag(ItemFlag.KARMA_USE.getValue());
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                            //  c.getPlayer().addPet(pet);
                        }

                        int flag = nItem.getFlag();
                        if (ii.isCash(itemId)) {
                            if (GameConstants.isEquip(itemId)) {
                                if (!ItemFlag.KARMA_EQUIP.check(flag)) {
                                    flag |= ItemFlag.KARMA_EQUIP.getValue();
                                }
                            } else {
                                if (!ItemFlag.KARMA_USE.check(flag)) {
                                    flag |= ItemFlag.KARMA_USE.getValue();
                                }
                            }
                        }

                        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getPlayer().havePartyQuest(itemId);
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        nItem = null;
                    }
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0, uniqueid);
                newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return null;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem));
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final Item nEquip = ii.getEquipById(itemId, uniqueid);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) {
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (special) {
                    Equip eq = (Equip) nEquip;
                    short rand = (short) Randomizer.rand(1, 5);
                }
                newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return null;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nEquip));
                if (GameConstants.isHarvesting(itemId)) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
                return nEquip;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getPlayer().havePartyQuest(itemId);
        if (itemId == 4001886) {
            c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
        }
        return null;
    }

    public static Item addbyId_Gachapon(final MapleClient c, final int itemId, short quantity) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.DECORATION).getNextFreeSlot() == -1) {
            return null;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return null;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);

        if (!type.equals(MapleInventoryType.EQUIP) || !type.equals(MapleInventoryType.DECORATION)) {
            short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);

            if (!GameConstants.isRechargable(itemId)) {
                Item nItem = null;
                boolean recieved = false;

                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = (Item) i.next();
                            short oldQ = nItem.getQuantity();

                            if (oldQ < slotMax) {
                                recieved = true;

                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(type, nItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);
                        final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem));
                        if (GameConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved) {
                    c.getPlayer().havePartyQuest(nItem.getItemId());
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem));
                c.getPlayer().havePartyQuest(nItem.getItemId());
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final Item item = ii.getEquipById(itemId);
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, item, true));
                c.getPlayer().havePartyQuest(item.getItemId());
                return item;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    public static boolean addFromDrop(final MapleClient c, final Item item, final boolean show) {
        return addFromDrop(c, item, show, false, false);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean enhance, boolean pet) {
        return addFromDrop(c, item, show, enhance, pet, false);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean enhance, boolean pet, boolean sort) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (c.getPlayer() == null || (ii.isPickupRestricted(item.getItemId()) && !sort && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) || (!ii.itemExists(item.getItemId()))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return false;
        }
        final int before = c.getPlayer().itemQuantity(item.getItemId());
        short quantity = item.getQuantity();
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());

        if (!type.equals(MapleInventoryType.EQUIP) && !type.equals(MapleInventoryType.DECORATION)) {
            final short slotMax = ii.getSlotMax(item.getItemId());
            final List<Item> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!GameConstants.isRechargable(item.getItemId())) {
                if (quantity <= 0) { //wth
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
                    return false;
                }
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner()) && item.getExpiration() == eItem.getExpiration()) {
                                //      if (item.getFlag() == eItem.getFlag() && (!type.equals(MapleInventoryType.CASH) || (type.equals(MapleInventoryType.CASH) && item.getUniqueId() == eItem.getUniqueId()))) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(type, eItem, !pet));
                                //     }
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ, item.getFlag());
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setPet(item.getPet());
                    nItem.setGMLog(item.getGMLog());
                    nItem.setReward(item.getReward());
                    short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (ii.isCash(nItem.getItemId())) {
                        nItem.setUniqueId(item.getUniqueId());
                    }
                    if (newSlot == -1) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem, !pet));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity, item.getFlag());
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());
                nItem.setPet(item.getPet());
                nItem.setGMLog(item.getGMLog());
                nItem.setReward(item.getReward());
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, nItem, !pet));
//                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } else {
            if (quantity == 1) {
                if (enhance) {
                    item = checkEnhanced(item, c.getPlayer());
                }
                Equip equip = (Equip) item;
                if (equip.getItemId() == 1672082) {
                    equip.setPotential1(60011);
                    equip.setPotential2(60010);
                }
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                if (GameConstants.isArcaneSymbol(item.getItemId())) {
                    if (equip.getArcLevel() == 0) {
                        equip.setArc((short) 30);
                        equip.setArcLevel((byte) 1);
                        if (equip.getArcEXP() == 0)
                            equip.setArcEXP(1);
                        if (GameConstants.isXenon(c.getPlayer().getJob())) {
                            equip.setStr((short) 117);
                            equip.setDex((short) 117);
                            equip.setLuk((short) 117);
                        } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                            equip.setHp((short) 525);
                        } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                            equip.setStr((short) 300);
                        } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                            equip.setInt((short) 300);
                        } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                            equip.setDex((short) 300);
                        } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                            equip.setLuk((short) 300);
                        } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                            equip.setStr((short) 300);
                        }
                    }
                }
                if (GameConstants.isAscenticSymbol(item.getItemId())) {
                    if (equip.getArcLevel() == 0) {
                        equip.setArc((short) 10);
                        equip.setArcLevel((byte) 1);
                        equip.setArcEXP(1);
                        if (GameConstants.isXenon(c.getPlayer().getJob())) {
                            equip.setStr((short) 195);
                            equip.setDex((short) 195);
                            equip.setLuk((short) 195);
                        } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                            equip.setHp((short) 8750);
                        } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                            equip.setStr((short) 500);
                        } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                            equip.setInt((short) 500);
                        } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                            equip.setDex((short) 500);
                        } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                            equip.setLuk((short) 500);
                        } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                            equip.setStr((short) 500);
                        }
                    }
                }

                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(type, item, !pet));
                if (GameConstants.isHarvesting(item.getItemId())) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (item.getQuantity() >= 50 && item.getItemId() == 2340000) {
//            c.setMonitored(true);
        }
        if (before == 0) {
            switch (item.getItemId()) {
                case AramiaFireWorks.KEG_ID:
                    c.getPlayer().dropMessage(5, "You have gained a Powder Keg, you can give this in to Aramia of Henesys.");
                    break;
                case AramiaFireWorks.SUN_ID:
                    c.getPlayer().dropMessage(5, "You have gained a Warm Sun, you can give this in to Maple Tree Hill through @joyce.");
                    break;
                case AramiaFireWorks.DEC_ID:
                    c.getPlayer().dropMessage(5, "You have gained a Tree Decoration, you can give this in to White Christmas Hill through @joyce.");
                    break;
            }
        }
        c.getPlayer().havePartyQuest(item.getItemId());
        if (show) {
            c.getSession().writeAndFlush(InfoPacket.getShowItemGain(item.getItemId(), item.getQuantity(), pet));
        }
        if (item.getItemId() == 4001886) {
            c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
        }

        if (item.getItemId() == 4310308) {
            int lock = c.getPlayer().isLockNeoCore() ? 1 : 0;
            c.getPlayer().updateInfoQuest(501215, "point=" + c.getPlayer().getItemQuantity(4310308, false) + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";;week=0;total=0;today=0;lock=" + lock + ""); //네오 코어
        }
        return true;
    }

    private static Item checkEnhanced(final Item before, final MapleCharacter chr) {
        if (before instanceof Equip) {
            final Equip eq = (Equip) before;
            if (eq.getState() == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1) && GameConstants.canScroll(eq.getItemId()) && Randomizer.nextInt(100) >= 80) { //20% chance of pot?
                eq.resetPotential();
            }
        }
        return before;
    }

    public static boolean checkSpace(final MapleClient c, final int itemid, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (c.getPlayer() == null || (ii.isPickupRestricted(itemid) && c.getPlayer().haveItem(itemid, 1, true, false)) || (!ii.itemExists(itemid))) {
//            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return false;
        }
        if (quantity <= 0 && !GameConstants.isRechargable(itemid)) {
            return false;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        if (c == null || c.getPlayer() == null || c.getPlayer().getInventory(type) == null) { //wtf is causing this?
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP) || !type.equals(MapleInventoryType.DECORATION)) {
            final short slotMax = ii.getSlotMax(itemid);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!GameConstants.isRechargable(itemid)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (Item eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            final int numSlotsNeeded;
            if (slotMax > 0 && !GameConstants.isRechargable(itemid)) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, final short quantity, final boolean fromDrop) {
        return removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            final boolean allowZero = consume && GameConstants.isRechargable(item.getItemId());
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);

            if (item.getItemId() == 4310308) {
                int lock = c.getPlayer().isLockNeoCore() ? 1 : 0;
                c.getPlayer().updateInfoQuest(501215, "point=" + c.getPlayer().getItemQuantity(4310308, false) + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";;week=0;total=0;today=0;lock=" + lock + ""); //네오 코어
            }
            if (GameConstants.isHarvesting(item.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }

            if (item.getQuantity() == 0 && !allowZero) {
                c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(type, item.getPosition(), fromDrop));
            } else {
                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(type, (Item) item, fromDrop));
            }
            if (item.getItemId() == 4001886) {
                c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
            }
            return true;
        }

        return false;
    }

    public static boolean removeById(final MapleClient c, final MapleInventoryType type, final int itemId, final int quantity, final boolean fromDrop, final boolean consume) {
        int remremove = quantity;
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
            if (item.getItemId() == 4001886) {
                c.getSession().writeAndFlush(CWvsContext.setBossReward(c.getPlayer()));
            }
        }

        if (itemId == 4310308) {
            int lock = c.getPlayer().isLockNeoCore() ? 1 : 0;
            c.getPlayer().updateInfoQuest(501215, "point=" + c.getPlayer().getItemQuantity(4310308, false) + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";;week=0;total=0;today=0;lock=" + lock + ""); //네오 코어
        }
        return remremove <= 0;
    }

    public static boolean removeFromSlot_Lock(final MapleClient c, final MapleInventoryType type, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            if (ItemFlag.LOCK.check(item.getFlag()) || ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                return false;
            }
            return removeFromSlot(c, type, slot, quantity, fromDrop, consume);
        }
        return false;
    }

    public static boolean removeById_Lock(final MapleClient c, final MapleInventoryType type, final int itemId) {
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (removeFromSlot_Lock(c, type, item.getPosition(), (short) 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final short src, final short dst) {
        if (src < 0 || dst < 0 || src == dst || type == MapleInventoryType.EQUIPPED) {
            return;
        }

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        final Item source = c.getPlayer().getInventory(type).getItem(src);
        if (source == null) {
            return;
        }

        short olddstQ = -1;
        final Item initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }

        final short slotMax = ii.getSlotMax(source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);

        final short oldsrcQ = source.getQuantity();
        if (type != MapleInventoryType.EQUIP && type != MapleInventoryType.CASH && initialTarget != null && source.getItemId() == initialTarget.getItemId()) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.getSession().writeAndFlush(InventoryPacket.moveAndCombineWithRestItem(source, initialTarget));
            } else {
                c.getSession().writeAndFlush(InventoryPacket.moveAndCombineItem(source, initialTarget));
            }
        } else {
            c.getSession().writeAndFlush(InventoryPacket.moveInventoryItem(type, src, dst, false, false));
        }
    }

    public static void equip(final MapleClient c, final short src, short dst, MapleInventoryType type) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        Equip source = null;
        if (type == MapleInventoryType.DECORATION) {
            source = (Equip) c.getPlayer().getInventory(MapleInventoryType.DECORATION).getItem(src);
        } else {
            source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src);
        }
        Equip target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);

        if (source == null || source.getDurability() == 0 || GameConstants.isHarvesting(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        final Map<String, Integer> stats = ii.getEquipStats(source.getItemId());

        if (stats == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        if (dst > -1200 && dst < -999 && !GameConstants.isEvanDragonItem(source.getItemId()) && !GameConstants.isMechanicItem(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        } else if (((dst <= -1200 && dst > -1300) || (dst >= -999 && dst < -99)) && !stats.containsKey("cash") && c.getPlayer().getAndroid() == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        } else if ((dst <= -1300 && dst > -1400) && !GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (source.getItemId() != 1342069 && GameConstants.isWeapon(source.getItemId()) && dst != -10 && dst != -11) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (dst == -18 && !GameConstants.isMountItemAvailable(source.getItemId(), c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if ((dst <= (short) -100 && dst > (short) -200) && !MapleItemInformationProvider.getInstance().isCash(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (dst == -118 && source.getItemId() / 10000 != 190) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (dst == -55) { //pendant
            MapleQuestStatus stat = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < System.currentTimeMillis()) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
        }
        if (GameConstants.isKatara(source.getItemId()) || source.getItemId() / 10000 == 135) {
            if (source.getItemId() == 1342069) {
                dst = (byte) -110;
            } else {
                dst = (byte) -10; //shield slot
            }
        }
        if (GameConstants.isEvanDragonItem(source.getItemId()) && (chr.getJob() < 2200 || chr.getJob() > 2218)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        if (GameConstants.isMechanicItem(source.getItemId()) && (chr.getJob() < 3500 || chr.getJob() > 3512)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (GameConstants.isArcaneSymbol(source.getItemId())) {
            // 이미껴져잇는지 확인
            boolean isequiped = false;
            for (short i = -1600; i >= -1605; i--) {
                if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem(i) != null) {
                    if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem(i).getItemId() == source.getItemId()) {
                        isequiped = true;
                    }
                }
            }
            if (isequiped) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
        }
        if (GameConstants.isAscenticSymbol(source.getItemId())) {
            // 이미껴져잇는지 확인
            boolean isequiped = false;
            for (short i = -1700; i >= -1702; i--) {
                if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem(i) != null) {
                    if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem(i).getItemId() == source.getItemId()) {
                        isequiped = true;
                    }
                }
            }
            if (isequiped) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
        }
        if (source.getItemId() / 1000 == 1112) { //ring
            for (RingSet s : RingSet.values()) {
                if (s.id.contains(Integer.valueOf(source.getItemId()))) {
                    List<Integer> theList = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).listIds();
                    for (Integer i : s.id) {
                        if (theList.contains(i)) {
                            c.getPlayer().dropMessage(1, "You may not equip this item because you already have a " + (StringUtil.makeEnumHumanReadable(s.name())) + " equipped.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            return;
                        }
                    }
                }
            }
        }
        switch (dst) {
            case -6: { // Top
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -5);
                if (top != null && GameConstants.isOverall(top.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), type);
                }
                break;
            }
            case -5: {
                final Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -5);
                final Item bottom = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -6);
                if (top != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull(bottom != null && GameConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), type);
                }
                if (bottom != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), type);
                }
                break;
            }
        }
        if (type == MapleInventoryType.DECORATION) {
            source = (Equip) c.getPlayer().getInventory(MapleInventoryType.DECORATION).getItem(src);
        } else {
            source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src);
        }
        target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst); // Currently equipping

        if (source == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        List<Integer> potentials = new ArrayList<>();
        potentials.add(source.getPotential1());
        potentials.add(source.getPotential2());
        potentials.add(source.getPotential3());
        potentials.add(source.getPotential4());
        potentials.add(source.getPotential5());
        potentials.add(source.getPotential6());
        for (Integer potential : potentials) {
            int lv = (ii.getReqLevel(source.getItemId()) / 10) - 1;
            if (lv < 0) {
                lv = 0;
            }
            if (potential == 0 || ii.getPotentialInfo(potential) == null || ii.getPotentialInfo(potential).get(lv) == null) {
                continue;
            }
            int usefulSkill = ii.getPotentialInfo(potential).get(lv).skillID;
            if (usefulSkill > 0) {
                c.getPlayer().changeSkillLevel(GameConstants.getOrdinaryJobNumber(c.getPlayer().getJob()) * 10000 + usefulSkill, (byte) 1, (byte) 1);
            }
        }

        int flag = source.getFlag();
        if (stats.get("equipTradeBlock") != null || source.getItemId() / 10000 == 167 || source.getItemId() / 10000 == 166) { // Block trade when equipped.
            if (!ItemFlag.UNTRADEABLE.check(flag)) {
                flag += ItemFlag.UNTRADEABLE.getValue();
                source.setFlag(flag);
            }
        }

        if (source.getItemId() / 10000 == 166) { //안드로이드 착용
            final Item heart = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -28);
            chr.removeAndroid();
            if (heart != null) {
                if (ItemFlag.ANDROID_ACTIVATED.check(flag)) {
                    if (source.getAndroid() != null) {
                        chr.setAndroid(source.getAndroid());
                    } else {
                        chr.dropMessage(1, "안드로이드 오류가 발생하였습니다.");
                        return;
                    }
                } else {
                    final int uid = MapleInventoryIdentifier.getInstance();
                    source.setUniqueId(uid);
                    MapleAndroid androids = MapleAndroid.create(source.getItemId(), uid);
                    source.setAndroid(androids);
                    flag += ItemFlag.ANDROID_ACTIVATED.getValue();
                    source.setFlag(flag);
                    chr.setAndroid(androids); //spawn it
                }
            }
        } else if (source.getItemId() / 10000 == 167) { //안드로이드 심장 착용
            final Item android = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -27);
            chr.removeAndroid();
            if (android != null) {
                int androidflag = android.getFlag();
                if (ItemFlag.ANDROID_ACTIVATED.check(androidflag)) {
                    if (android.getAndroid() != null) {
                        chr.setAndroid(android.getAndroid());
                    } else {
                        chr.dropMessage(1, "안드로이드 오류가 발생하였습니다.");
                        return;
                    }
                } else {
                    final int uid = MapleInventoryIdentifier.getInstance();
                    android.setUniqueId(uid);
                    MapleAndroid androids = MapleAndroid.create(android.getItemId(), uid);
                    android.setAndroid(androids);
                    flag += ItemFlag.ANDROID_ACTIVATED.getValue();
                    android.setFlag(flag);
                    c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, android));
                    chr.setAndroid(androids); //spawn it
                }
            }
        }
        if (source.getCharmEXP() > 0 && !ItemFlag.CHARM_EQUIPED.check(source.getFlag())) {
            chr.getTrait(MapleTraitType.charm).addExp(source.getCharmEXP(), chr);
            source.setCharmEXP((short) 0);
            source.setFlag(source.getFlag() + ItemFlag.CHARM_EQUIPED.getValue());
        }

        chr.getInventory(type).removeSlot(src);
        if (target != null) {
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        source.setPosition(dst);
        chr.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            chr.getInventory(type).addFromDB(target);
        }

        if (ItemFlag.KARMA_EQUIP.check(source.getFlag()) && !ItemFlag.UNTRADEABLE.check(source.getFlag())) {
            source.setFlag(source.getFlag() - ItemFlag.KARMA_EQUIP.getValue() + ItemFlag.UNTRADEABLE.getValue());
        }

        if (GameConstants.isWeapon(source.getItemId()) && source.getItemId() != 1342069) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Booster);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.NoBulletConsume);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SoulArrow);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WeaponCharge);
        }
        if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
        } else if (source.getItemId() == 1122017) {
            chr.startFairySchedule(true, true);
        }

        if (source.getItemId() >= 1113098 && source.getItemId() <= 1113128) {
            chr.changeSkillLevel(SkillFactory.getSkill(80001455 + (source.getItemId() - 1113098)), (byte) source.getBaseLevel(), (byte) 4);
        }

        if (target != null) {
            if (target.getItemId() >= 1113098 && target.getItemId() <= 1113128) {
                chr.changeSkillLevel(SkillFactory.getSkill(80001455 + (target.getItemId() - 1113098)), (byte) -1, (byte) 0);
            }
        }

        if (source.getItemId() == 1112586) {
            MapleItemInformationProvider.getInstance().getItemEffect(2022747).applyTo(chr, false, 0);
        } else if (source.getItemId() == 1112663) {
            MapleItemInformationProvider.getInstance().getItemEffect(2022823).applyTo(chr, false, 0);
        }

        chr.setSoulMP(null);

        if (source != null) {
            chr.setSoulMP(source);
        }

        c.getSession().writeAndFlush(InventoryPacket.moveInventoryItem(type, src, dst, false, false));
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, type, source));

        if (target != null) {
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, type, target));
        }

        if ((dst <= -1200 && dst > -1300) && chr.getAndroid() != null) {
            chr.setAndroid(chr.getAndroid()); //respawn it
        }
        chr.equipChanged();
    }

    public static void unequip(final MapleClient c, short src, final short dst, MapleInventoryType type) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = null;
        if (type == MapleInventoryType.EQUIP || type == null) {
            target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        } else {
            target = (Equip) c.getPlayer().getInventory(MapleInventoryType.DECORATION).getItem(dst);
        }

        if (target != null) {
            if (src > -1200 && src < -999 && !GameConstants.isEvanDragonItem(target.getItemId()) && !GameConstants.isMechanicItem(target.getItemId())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            } else if ((src <= -1300 && src > -1400) && !GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (target.getItemId() != 1342069 && GameConstants.isWeapon(target.getItemId()) && src != -10 && src != -11) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (src == -18 && !GameConstants.isMountItemAvailable(target.getItemId(), c.getPlayer().getJob())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (src <= -100 && !MapleItemInformationProvider.getInstance().isCash(target.getItemId())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if ((src > -100 || src <= -200) && MapleItemInformationProvider.getInstance().isCash(target.getItemId())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (src == -118 && target.getItemId() / 10000 != 190) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (src == -55) { //pendant
                MapleQuestStatus stat = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
                if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < System.currentTimeMillis()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
            }
            if (GameConstants.isKatara(target.getItemId()) || target.getItemId() / 10000 == 135) {
                if (target.getItemId() == 1342069) {
                    src = (byte) -110;
                } else {
                    src = (byte) -10; //shield slot
                }
            }
            if (GameConstants.isEvanDragonItem(target.getItemId()) && (c.getPlayer().getJob() < 2200 || c.getPlayer().getJob() > 2218)) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }

            if (GameConstants.isMechanicItem(target.getItemId()) && (c.getPlayer().getJob() < 3500 || c.getPlayer().getJob() > 3512)) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }

            if (ii.isOnlyEquip(target.getItemId()) && c.getPlayer().hasEquipped(target.getItemId())) {
                c.getPlayer().dropMessage(1, "고유장착 아이템은 1개만 착용할 수 있습니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }

            int flag = target.getFlag();

            final Map<String, Integer> stats = ii.getEquipStats(target.getItemId());

            if (stats.get("equipTradeBlock") != null || source.getItemId() / 10000 == 167 || source.getItemId() / 10000 == 166) { // Block trade when equipped.
                if (!ItemFlag.UNTRADEABLE.check(flag)) {
                    flag += ItemFlag.UNTRADEABLE.getValue();
                    target.setFlag(flag);
                }
            }
        }

        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(type).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(type).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }
        if (GameConstants.isWeapon(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Booster);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.NoBulletConsume);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SoulArrow);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WeaponCharge);
        } else if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
        } else if (source.getItemId() / 10000 == 166) {
            c.getPlayer().removeAndroid();
        } else if (source.getItemId() / 10000 == 167) {
            c.getPlayer().removeAndroid();
        } else if ((src <= -1200 && src > -1300) && c.getPlayer().getAndroid() != null) {
            c.getPlayer().setAndroid(c.getPlayer().getAndroid());
        } else if (source.getItemId() == 1122017) {
            c.getPlayer().cancelFairySchedule(true);
        } else if (source.getItemId() == 1112585) {
            c.getPlayer().dispelSkill(2022746);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RepeatEffect);
        } else if (source.getItemId() == 1112586) {
            c.getPlayer().dispelSkill(2022747);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RepeatEffect);
        } else if (source.getItemId() == 1112594) {
            c.getPlayer().dispelSkill(2022764);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RepeatEffect);
        } else if (source.getItemId() == 1112663) {
            c.getPlayer().dispelSkill(2022823);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RepeatEffect);
        } else if (source.getItemId() == 1112735) {
            c.getPlayer().dispelSkill(2022823);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RepeatEffect);
        }

        c.getPlayer().setSoulMP(null);

        List<Integer> potentials = new ArrayList<>();
        potentials.add(source.getPotential1());
        potentials.add(source.getPotential2());
        potentials.add(source.getPotential3());
        potentials.add(source.getPotential4());
        potentials.add(source.getPotential5());
        potentials.add(source.getPotential6());
        for (Integer potential : potentials) {
            int lv = (ii.getReqLevel(source.getItemId()) / 10) - 1;
            if (lv < 0) {
                lv = 0;
            }
            if (potential == 0 || ii.getPotentialInfo(potential) == null || ii.getPotentialInfo(potential).get(lv) == null) {
                continue;
            }
            int usefulSkill = ii.getPotentialInfo(potential).get(lv).skillID;
            if (usefulSkill > 0) {
                c.getPlayer().changeSkillLevel(GameConstants.getOrdinaryJobNumber(c.getPlayer().getJob()) * 10000 + usefulSkill, (byte) -1, (byte) 0);
            }
        }

        if (target != null) {
            if (ItemFlag.KARMA_EQUIP.check(target.getFlag()) && !ItemFlag.UNTRADEABLE.check(target.getFlag())) {
                target.setFlag(target.getFlag() - ItemFlag.KARMA_EQUIP.getValue() + ItemFlag.UNTRADEABLE.getValue());
            }
        }

        if (source.getItemId() >= 1113098 && source.getItemId() <= 1113128) {
            c.getPlayer().changeSkillLevel(SkillFactory.getSkill(80001455 + (source.getItemId() - 1113098)), (byte) -1, (byte) 0);
        }

        c.getSession().writeAndFlush(InventoryPacket.moveInventoryItem(type, src, dst, false, false));
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, type, source));
        if (target != null) {
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, type, target));
        }

        c.getPlayer().equipChanged();
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, final short quantity) {
        return drop(c, type, src, quantity, false);
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, short quantity, final boolean npcInduced) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return false;
        }
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        if (quantity < 0 || source == null || (!npcInduced && GameConstants.isPet(source.getItemId())) || (quantity == 0 && !GameConstants.isRechargable(source.getItemId())) || c.getPlayer().inPVP()) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return false;
        }

        final int flag = source.getFlag();
        if (quantity > source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return false;
        }
        if (ItemFlag.LOCK.check(flag) || (quantity != 1 && type == MapleInventoryType.EQUIP)) { // hack
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return false;
        }
        if (type == MapleInventoryType.EQUIP) {
            Equip equip = (Equip) source;
            if ((equip.getEnchantBuff() & 0x88) != 0) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                c.getPlayer().dropMessage(1, "장비의 흔적은 버릴 수 없습니다.");
                return false;
            }
        }
        final Point dropPos = new Point(c.getPlayer().getPosition());
        if (quantity < source.getQuantity() && !GameConstants.isRechargable(source.getItemId())) {
            final Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(type, source, true));

            if (ii.isDropRestricted(target.getItemId()) || ii.isAccountShared(target.getItemId())) {
                if (ItemFlag.KARMA_EQUIP.check(target.getFlag())) {
                    target.setFlag((target.getFlag() - ItemFlag.KARMA_EQUIP.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(target.getFlag())) {
                    target.setFlag((target.getFlag() - ItemFlag.KARMA_USE.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                }
            } else {
                if (GameConstants.isPet(target.getItemId()) || ItemFlag.UNTRADEABLE.check(target.getFlag())) {

                    if (ItemFlag.KARMA_EQUIP.check(target.getFlag())) {
                        target.setFlag((target.getFlag() - ItemFlag.KARMA_EQUIP.getValue()));
                        c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                    } else if (ItemFlag.KARMA_USE.check(target.getFlag())) {
                        target.setFlag((target.getFlag() - ItemFlag.KARMA_USE.getValue()));
                        c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                    } else {
                        c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                    }
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                }
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            if (GameConstants.isHarvesting(source.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(type, src, true));
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            if (ii.isDropRestricted(source.getItemId()) || ii.isAccountShared(source.getItemId())) {
                if (ItemFlag.KARMA_EQUIP.check(flag)) {
                    source.setFlag((flag - ItemFlag.KARMA_EQUIP.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    source.setFlag((flag - ItemFlag.KARMA_USE.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                }
            } else {
                if (GameConstants.isPet(source.getItemId()) || ItemFlag.UNTRADEABLE.check(flag)) {
                    if (ItemFlag.KARMA_EQUIP.check(flag)) {
                        source.setFlag((flag - ItemFlag.KARMA_EQUIP.getValue()));
                        c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                    } else if (ItemFlag.KARMA_USE.check(flag)) {
                        source.setFlag((flag - ItemFlag.KARMA_USE.getValue()));
                        c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                    } else {
                        c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                    }
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                }
            }
        }
        return true;
    }
}
