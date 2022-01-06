/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

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
package client.inventory;

import constants.GameConstants;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.enchant.EquipmentEnchant;
import server.maps.BossReward;

public enum ItemLoader {

    INVENTORY("inventoryitems", "inventoryequipment", "inventoryequipenchant", 0, "characterid"),
    STORAGE("inventoryitems", "inventoryequipment", "inventoryequipenchant", 1, "accountid"),
    CASHSHOP("csitems", "csequipment", null, 2, "accountid"),
    HIRED_MERCHANT("hiredmerchitems", "hiredmerchequipment", null, 5, "packageid"),
    DUEY("dueyitems", "dueyequipment", "dueyequipenchant", 6, "packageid");

    private int value;
    private String table, table_equip, table_enchant, arg;

    private ItemLoader(String table, String table_equip, String table_enchant, int value, String arg) {
        this.table = table;
        this.table_equip = table_equip;
        this.table_enchant = table_enchant;
        this.value = value;
        this.arg = arg;
    }

    public int getValue() {
        return value;
    }

    //does not need connection con to be auto commit
    public Map<Long, Item> loadItems(boolean login, int id, MapleInventoryType type) throws SQLException {
        Map<Long, Item> items = new LinkedHashMap<Long, Item>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `");
        if (value <= 1) {
            switch (type.getType()) {
                case 2:
                    query.append("inventoryitemsuse");
                    break;
                case 3:
                    query.append("inventoryitemssetup");
                    break;
                case 4:
                    query.append("inventoryitemsetc");
                    break;
                case 5:
                    query.append("inventoryitemscash");
                    break;
                default:
                    query.append("inventoryitems");
                    break;
            }
        } else {
            query.append(table);
        }
        query.append("` LEFT JOIN `");
        query.append(table_equip);
        if (type.getType() == 1 || type.getType() == 6) {

            query.append("` USING (`inventoryitemid`) WHERE `inventorytype` = ?");
        } else {
            query.append("` USING (`inventoryitemid`) WHERE `type` = ?");
        }
        query.append(" AND `");
        query.append(arg);
        query.append("` = ?");

        if (login) {
            query.append(" AND `inventorytype` = ");
            query.append(MapleInventoryType.EQUIPPED.getType());
        }

        Connection con = DatabaseConnection.getConnection();

        try (PreparedStatement ps = con.prepareStatement(query.toString())) {

            if (type.getType() == 1 || type.getType() == 6) {
                ps.setInt(1, type.getType());
            } else {
                ps.setInt(1, value);
            }
            ps.setInt(2, id);

            try (ResultSet rs = ps.executeQuery()) {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                Item item_;
                while (rs.next()) {
                    if (!ii.itemExists(rs.getInt("itemid"))) { //EXPENSIVE
                        continue;
                    }
                    if (rs.getInt("itemid") / 1000000 == 1) {
                        Equip equip = new Equip(rs.getInt("itemid"), rs.getShort("position"), rs.getInt("uniqueid"), rs.getInt("flag"));
                        if (!login) {
                            equip.setQuantity((short) 1);
                            equip.setInventoryId(rs.getLong("inventoryitemid"));
                            equip.setOwner(rs.getString("owner"));
                            equip.setExpiration(rs.getLong("expiredate"));
                            equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                            equip.setLevel(rs.getByte("level"));
                            equip.setStr(rs.getShort("str"));
                            equip.setDex(rs.getShort("dex"));
                            equip.setInt(rs.getShort("int"));
                            equip.setLuk(rs.getShort("luk"));
                            equip.setHp(rs.getShort("hp"));
                            equip.setMp(rs.getShort("mp"));
                            equip.setWatk(rs.getShort("watk"));
                            equip.setMatk(rs.getShort("matk"));
                            equip.setWdef(rs.getShort("wdef"));
                            equip.setMdef(rs.getShort("mdef"));
                            equip.setAcc(rs.getShort("acc"));
                            equip.setAvoid(rs.getShort("avoid"));
                            equip.setHands(rs.getShort("hands"));
                            equip.setSpeed(rs.getShort("speed"));
                            equip.setJump(rs.getShort("jump"));
                            equip.setViciousHammer(rs.getByte("ViciousHammer"));
                            equip.setItemEXP(rs.getInt("itemEXP"));
                            equip.setGMLog(rs.getString("GM_Log"));
                            equip.setDurability(rs.getInt("durability"));
                            equip.setEnhance(rs.getByte("enhance"));
                            equip.setState(rs.getByte("state"));
                            equip.setLines(rs.getByte("line"));
                            equip.setPotential1(rs.getInt("potential1"));
                            equip.setPotential2(rs.getInt("potential2"));
                            equip.setPotential3(rs.getInt("potential3"));
                            equip.setPotential4(rs.getInt("potential4"));
                            equip.setPotential5(rs.getInt("potential5"));
                            equip.setPotential6(rs.getInt("potential6"));
                            equip.setGiftFrom(rs.getString("sender"));
                            equip.setIncSkill(rs.getInt("incSkill"));
                            equip.setPVPDamage(rs.getShort("pvpDamage"));
                            equip.setCharmEXP(rs.getShort("charmEXP"));
                            if (equip.getCharmEXP() < 0) { //has not been initialized yet
                                equip.setCharmEXP(((Equip) ii.getEquipById(equip.getItemId())).getCharmEXP());
                            }
                            if (equip.getUniqueId() > -1) {
                                if (GameConstants.isEffectRing(rs.getInt("itemid"))) {
                                    MapleRing ring = MapleRing.loadFromDb(equip.getUniqueId(), type.equals(MapleInventoryType.EQUIPPED));
                                    if (ring != null) {
                                        equip.setRing(ring);
                                    }
                                } else if (equip.getItemId() / 10000 == 166) {
                                    MapleAndroid ring = MapleAndroid.loadFromDb(equip.getItemId(), equip.getUniqueId());
                                    if (ring != null) {
                                        equip.setAndroid(ring);
                                    }
                                }
                            }
                            equip.setEnchantBuff(rs.getShort("enchantbuff"));
                            equip.setReqLevel(rs.getByte("reqLevel"));
                            equip.setYggdrasilWisdom(rs.getByte("yggdrasilWisdom"));
                            equip.setFinalStrike(rs.getByte("finalStrike") > 0);
                            equip.setBossDamage(rs.getByte("bossDamage"));
                            equip.setIgnorePDR(rs.getByte("ignorePDR"));
                            equip.setTotalDamage(rs.getByte("totalDamage"));
                            equip.setAllStat(rs.getByte("allStat"));
                            equip.setKarmaCount(rs.getByte("karmaCount"));
                            equip.setSoulEnchanter(rs.getShort("soulenchanter"));
                            equip.setSoulName(rs.getShort("soulname"));
                            equip.setSoulPotential(rs.getShort("soulpotential"));
                            equip.setSoulSkill(rs.getInt("soulskill"));
                            equip.setFire(rs.getLong("fire") < 0 ? 0 : rs.getLong("fire"));
                            equip.setArc(rs.getShort("arc"));
                            equip.setArcEXP(rs.getInt("arcexp"));
                            equip.setArcLevel(rs.getInt("arclevel"));
                            equip.setEquipmentType(rs.getInt("equipmenttype"));
                            equip.setMoru(rs.getInt("moru"));

                            if (table_enchant != null) {
                                PreparedStatement ps1 = con.prepareStatement("SELECT * FROM `" + table_enchant + "` WHERE inventoryitemid = ?");
                                ps1.setLong(1, equip.getInventoryId());
                                ResultSet rs1 = ps1.executeQuery();
                                if (rs1.next()) {
                                    equip.setEnchantStr(rs1.getShort("str"));
                                    equip.setEnchantDex(rs1.getShort("dex"));
                                    equip.setEnchantInt(rs1.getShort("int"));
                                    equip.setEnchantLuk(rs1.getShort("luk"));
                                    equip.setEnchantHp(rs1.getShort("hp"));
                                    equip.setEnchantMp(rs1.getShort("mp"));
                                    equip.setEnchantWatk(rs1.getShort("watk"));
                                    equip.setEnchantMatk(rs1.getShort("matk"));
                                    equip.setEnchantWdef(rs1.getShort("wdef"));
                                    equip.setEnchantMdef(rs1.getShort("mdef"));
                                    equip.setEnchantAcc(rs1.getShort("acc"));
                                    equip.setEnchantAvoid(rs1.getShort("avoid"));
                                }
                                rs1.close();
                                ps1.close();
                            }
                        }
                        item_ = equip.copy();

                        if (isCanMadeItem((Equip) item_)) {
                            items.put(rs.getLong("inventoryitemid"), item_);
                        }
                    } else {
                        Item item = new Item(rs.getInt("itemid"), rs.getShort("position"), rs.getShort("quantity"), rs.getInt("flag"), rs.getInt("uniqueid"));
                        item.setOwner(rs.getString("owner"));
                        item.setInventoryId(rs.getLong("inventoryitemid"));
                        item.setExpiration(rs.getLong("expiredate"));
                        item.setGMLog(rs.getString("GM_Log"));
                        item.setGiftFrom(rs.getString("sender"));
                        item.setMarriageId(rs.getInt("marriageId"));
                        if (GameConstants.isPet(item.getItemId())) {
                            if (item.getUniqueId() > -1) {
                                MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getUniqueId(), item.getPosition());
                                if (pet != null) {
                                    item.setPet(pet);
                                }
                            } else {
                                //O_O hackish fix
                                item.setPet(MaplePet.createPet(item.getItemId(), MapleInventoryIdentifier.getInstance()));
                            }
                        }
                        if (item.getItemId() == 4001886) {
                            item.setReward(new BossReward(rs.getInt("objectid"), rs.getInt("mobid"), rs.getInt("partyid"), rs.getInt("price")));
                        }
                        item_ = item.copy();
                        items.put(rs.getLong("inventoryitemid"), item_);
                    }
                }
                ps.close();
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.close();
            }
        }
        return items;
    }

    public void saveItems(List<Item> items, int id, MapleInventoryType type, boolean dc) {
        try {
            Connection con = DatabaseConnection.getConnection();
            saveItems(items, con, id, type, dc);
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void saveItems(List<Item> items, final Connection con, int id, MapleInventoryType type, boolean dc) {
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM `");
        if (value <= 1) {
            switch (type.getType()) {
                case 2:
                    query.append("inventoryitemsuse");
                    break;
                case 3:
                    query.append("inventoryitemssetup");
                    break;
                case 4:
                    query.append("inventoryitemsetc");
                    break;
                case 5:
                    query.append("inventoryitemscash");
                    break;
                default:
                    query.append("inventoryitems");
                    break;
            }
        } else {
            query.append(table);
        }
        if (type.getType() == 1 || type.getType() == 6) {
            query.append("`WHERE `inventorytype` = ?");
        } else {
            query.append("`WHERE `type` = ?");
        }
        query.append(" AND `").append(arg);
        query.append("` = ?");
        try {
            PreparedStatement ps = con.prepareStatement(query.toString());
            if (type.getType() == 1 || type.getType() == 6) {
                ps.setInt(1, type.getType());
            } else {
                ps.setInt(1, value);
            }
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
            if (items == null) {
                return;
            }
            StringBuilder query_2 = new StringBuilder("INSERT INTO `");
            if (value <= 1) {
                switch (type.getType()) {
                    case 2:
                        query_2.append("inventoryitemsuse");
                        break;
                    case 3:
                        query_2.append("inventoryitemssetup");
                        break;
                    case 4:
                        query_2.append("inventoryitemsetc");
                        break;
                    case 5:
                        query_2.append("inventoryitemscash");
                        break;
                    default:
                        query_2.append("inventoryitems");
                        break;
                }
            } else {
                query_2.append(table);
            }
            query_2.append("` (");
            query_2.append(arg);
            query_2.append(", itemid, inventorytype, position, quantity, owner, GM_Log, uniqueid, expiredate, flag, `type`, sender, marriageId");
            query_2.append(", price, partyid, mobid, objectid");
            query_2.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
            query_2.append(", ?, ?, ?, ?, ?");
            query_2.append(")");
            ps = con.prepareStatement(query_2.toString(), Statement.RETURN_GENERATED_KEYS);
            PreparedStatement pse = con.prepareStatement("INSERT INTO " + table_equip + " VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            final Iterator<Item> iter = items.iterator();
            Item item;
            while (iter.hasNext()) {
                item = iter.next();
                ps.setInt(1, id);
                ps.setInt(2, item.getItemId());
                ps.setInt(3, GameConstants.getInventoryType(item.getItemId()).getType());
                ps.setInt(4, item.getPosition());
                ps.setInt(5, item.getQuantity());
                ps.setString(6, item.getOwner());
                ps.setString(7, item.getGMLog());
                if (item.getPet() != null) { //expensif?
                    item.getPet().saveToDb(con);
                    ps.setInt(8, Math.max(item.getUniqueId(), item.getPet().getUniqueId()));
                } else {
                    ps.setInt(8, item.getUniqueId());
                }
                ps.setLong(9, item.getExpiration());
                if (item.getFlag() < 0) {
                    ps.setInt(10, MapleItemInformationProvider.getInstance().getItemInformation(item.getItemId()).flag);
                } else {
                    ps.setInt(10, item.getFlag());
                }
                ps.setByte(11, (byte) value);
                ps.setString(12, item.getGiftFrom());
                ps.setInt(13, item.getMarriageId());
                if (item.getReward() != null) {
                    ps.setInt(14, item.getReward().getPrice());
                    ps.setInt(15, item.getReward().getPartyId());
                    ps.setInt(16, item.getReward().getMobId());
                    ps.setInt(17, item.getReward().getObjectId());
                } else {
                    ps.setInt(14, 0);
                    ps.setInt(15, 0);
                    ps.setInt(16, 0);
                    ps.setInt(17, 0);
                }
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();

                if (!rs.next()) {
                    rs.close();
                    continue;
                }

                final long iid = rs.getLong(1);
                rs.close();

                if (dc) {
                    item.setInventoryId(iid);
                }

                if (item.getItemId() / 1000000 == 1) { // equip
                    Equip equip = (Equip) item;

                    if (!isCanMadeItem(equip)) {
                        continue;
                    }

                    pse.setLong(1, iid);
                    pse.setInt(2, equip.getUpgradeSlots());
                    if (equip.getItemId() >= 1113098 && equip.getItemId() <= 1113128) {
                        pse.setInt(3, equip.getBaseLevel());
                    } else {
                        pse.setInt(3, equip.getLevel());
                    }
                    pse.setInt(4, equip.getStr());
                    pse.setInt(5, equip.getDex());
                    pse.setInt(6, equip.getInt());
                    pse.setInt(7, equip.getLuk());
                    pse.setShort(8, equip.getArc());
                    pse.setInt(9, equip.getArcEXP());
                    pse.setInt(10, equip.getArcLevel());
                    pse.setInt(11, equip.getHp());
                    pse.setInt(12, equip.getMp());
                    pse.setInt(13, equip.getWatk());
                    pse.setInt(14, equip.getMatk());
                    pse.setInt(15, equip.getWdef());
                    pse.setInt(16, equip.getMdef());
                    pse.setInt(17, equip.getAcc());
                    pse.setInt(18, equip.getAvoid());
                    pse.setInt(19, equip.getHands());
                    pse.setInt(20, equip.getSpeed());
                    pse.setInt(21, equip.getJump());
                    pse.setInt(22, equip.getViciousHammer());
                    pse.setInt(23, equip.getItemEXP());
                    pse.setInt(24, equip.getDurability());
                    pse.setByte(25, equip.getEnhance());
                    pse.setByte(26, equip.getState());
                    pse.setByte(27, equip.getLines());
                    pse.setInt(28, equip.getPotential1());
                    pse.setInt(29, equip.getPotential2());
                    pse.setInt(30, equip.getPotential3());
                    pse.setInt(31, equip.getPotential4());
                    pse.setInt(32, equip.getPotential5());
                    pse.setInt(33, equip.getPotential6());
                    pse.setInt(34, equip.getIncSkill());
                    pse.setShort(35, equip.getCharmEXP());
                    pse.setShort(36, equip.getPVPDamage());
                    pse.setShort(37, equip.getEnchantBuff());
                    pse.setByte(38, equip.getReqLevel());
                    pse.setByte(39, equip.getYggdrasilWisdom());
                    pse.setByte(40, (byte) (equip.getFinalStrike() ? 1 : 0));
                    pse.setShort(41, equip.getBossDamage());
                    pse.setShort(42, equip.getIgnorePDR());
                    pse.setByte(43, equip.getTotalDamage());
                    pse.setByte(44, equip.getAllStat());
                    pse.setByte(45, equip.getKarmaCount());
                    pse.setShort(46, equip.getSoulName());
                    pse.setShort(47, equip.getSoulEnchanter());
                    pse.setShort(48, equip.getSoulPotential());
                    pse.setInt(49, equip.getSoulSkill());
                    pse.setLong(50, equip.getFire());
                    pse.setInt(51, equip.getEquipmentType());
                    pse.setInt(52, equip.getMoru());
                    pse.executeUpdate();
                    if (equip.getItemId() / 10000 == 166) {
                        if (equip.getAndroid() != null) {
                            equip.getAndroid().saveToDb();
                        }
                    }

                    if (table_enchant != null) {
                        PreparedStatement ps2 = con.prepareStatement("INSERT INTO `" + table_enchant + "` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        ps2.setLong(1, iid);
                        ps2.setShort(2, equip.getEnchantStr());
                        ps2.setShort(3, equip.getEnchantDex());
                        ps2.setShort(4, equip.getEnchantInt());
                        ps2.setShort(5, equip.getEnchantLuk());
                        ps2.setShort(6, equip.getEnchantHp());
                        ps2.setShort(7, equip.getEnchantMp());
                        ps2.setShort(8, equip.getEnchantWatk());
                        ps2.setShort(9, equip.getEnchantMatk());
                        ps2.setShort(10, equip.getEnchantWdef());
                        ps2.setShort(11, equip.getEnchantMdef());
                        ps2.setShort(12, equip.getEnchantAcc());
                        ps2.setShort(13, equip.getEnchantAvoid());
                        ps2.executeUpdate();
                        ps2.close();
                    }
                }
            }
            ps.close();
            pse.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isCanMadeItem(Equip equip) {

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        
        if (ii.getName(equip.getItemId()) == null) {
            System.err.println("정보가 없어 " + equip.getItemId());
            return false;
        } 

        if (ii.getName(equip.getItemId()).startsWith("제네시스")) {
            EquipmentEnchant.checkEquipmentStats(null, equip);
            return true;
        }

        if (equip.getFire() > 0) {
            if (GameConstants.isRing(equip.getItemId()) || (equip.getItemId() / 1000) == 1092 || (equip.getItemId() / 1000) == 1342 || (equip.getItemId() / 1000) == 1712 || (equip.getItemId() / 1000) == 1713 || (equip.getItemId() / 1000) == 1152 || (equip.getItemId() / 1000) == 1143 || (equip.getItemId() / 1000) == 1672 || GameConstants.isSecondaryWeapon(equip.getItemId()) || (equip.getItemId() / 1000) == 1190 || (equip.getItemId() / 1000) == 1182 || (equip.getItemId() / 1000) == 1662 || (equip.getItemId() / 1000) == 1802) {
                equip.setFire(0);
            }
        }

        int reqLevel = ii.getReqLevel(equip.getItemId()), maxEnhance;

        boolean isSuperiol = ii.isSuperial(equip.getItemId()).left != null;

        if (reqLevel < 95) {
            maxEnhance = isSuperiol ? 3 : 5;
        } else if (reqLevel <= 107) {
            maxEnhance = isSuperiol ? 5 : 8;
        } else if (reqLevel <= 119) {
            maxEnhance = isSuperiol ? 8 : 10;
        } else if (reqLevel <= 129) {
            maxEnhance = isSuperiol ? 10 : 15;
        } else if (reqLevel <= 139) {
            maxEnhance = isSuperiol ? 12 : 20;
        } else {
            maxEnhance = isSuperiol ? 15 : 25;
        }

        if (equip.getArcLevel() > 20) {
            equip.setArcLevel(20);
        }

        if (equip.getArc() > 220) {
            equip.setArc((short) 0);
        }

        if (equip.getItemId() == 1182285 || equip.getItemId() == 1122430 || ii.getEquipStats(equip.getItemId()).get("undecomposable") != null || ii.getEquipStats(equip.getItemId()).get("unsyntesizable") != null) {
            return true;
        }

        EquipmentEnchant.checkEquipmentStats(null, equip);

        if (equip.getItemId() == 1672082) {
            equip.setPotential1(60011);
            equip.setPotential2(60010);
        }

        /*        if ((equip.getEquipmentType() & 0x600) != 0 && equip.getEnhance() > 15) {
         System.out.println(equip.getItemId() + " 2번 때문에 못불러옴.");
         return false;
         }
        
         if (equip.getEnhance() > maxEnhance) {
         System.out.println(equip.getItemId() + " 3번 때문에 못불러옴.");
         return false;
         }
        
         if (equip.getViciousHammer() == 0 && ii.getSlots(equip.getItemId()) < equip.getLevel()) {
         System.out.println(equip.getItemId() + " 5번 때문에 못불러옴.");
         return false;
         }
        
         if (equip.getViciousHammer() == 1 && ii.getSlots(equip.getItemId()) + 1 < equip.getLevel()) {
         System.out.println(equip.getItemId() + " 6번 때문에 못불러옴.");
         return false;
         }*/
        return true;
    }
}
