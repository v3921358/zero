package client;

import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import database.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import server.MaplePortal;
import server.life.PlayerNPC;
import server.maps.SavedLocationType;
import tools.FileoutputUtil;
import tools.Pair;

public class MapleCharacterSave {

    private MapleCharacter chr;

    public MapleCharacterSave(MapleCharacter chr) {
        this.chr = chr;
    }

    public void saveInventory(final Connection con, boolean disconnect) throws SQLException {

        MapleInventoryType[] types = {MapleInventoryType.USE, MapleInventoryType.SETUP, MapleInventoryType.ETC, MapleInventoryType.CASH, MapleInventoryType.DECORATION};
        for (MapleInventoryType type : types) {
            List<Item> items = chr.getInventory(type.getType()).newList();

            if (chr.memorialcube != null && type.getType() == 5) {
                items.add(chr.memorialcube);
            }

            if (chr.returnscroll != null && type.getType() == 2) {
                items.add(chr.returnscroll);
            }
            if (con != null) {
                ItemLoader.INVENTORY.saveItems(items, con, chr.getId(), type, disconnect);
            } else {
                ItemLoader.INVENTORY.saveItems(items, chr.getId(), type, disconnect);
            }
        }
        List<Item> equips = new ArrayList<>();
        for (Item item : chr.getInventory(MapleInventoryType.EQUIP).newList()) {
            equips.add(item);

        }

        for (Item item : chr.getInventory(MapleInventoryType.EQUIPPED).newList()) {
            equips.add(item);
        }

        if (chr.choicepotential != null) {
            equips.add(chr.choicepotential);
        }

        if (con != null) {
            ItemLoader.INVENTORY.saveItems(equips, con, chr.getId(), MapleInventoryType.EQUIP, disconnect);
        } else {
            ItemLoader.INVENTORY.saveItems(equips, chr.getId(), MapleInventoryType.EQUIP, disconnect);
        }
    }

    public void saveMannequinToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM mannequins WHERE characterid = ?");

            for (MapleMannequin hair : chr.getHairRoom()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO mannequins (value, baseprob, basecolor, addcolor, characterid, type) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, hair.getValue());
                ps.setInt(2, hair.getBaseProb());
                ps.setInt(3, hair.getBaseColor());
                ps.setInt(4, hair.getAddColor());
                ps.setInt(5, chr.getId());
                ps.setInt(6, 0);
                ps.execute();
                ps.close();
            }

            for (MapleMannequin face : chr.getFaceRoom()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO mannequins (value, baseprob, basecolor, addcolor, characterid, type) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, face.getValue());
                ps.setInt(2, face.getBaseProb());
                ps.setInt(3, face.getBaseColor());
                ps.setInt(4, face.getAddColor());
                ps.setInt(5, chr.getId());
                ps.setInt(6, 1);
                ps.execute();
                ps.close();
            }

            for (MapleMannequin skin : chr.getSkinRoom()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO mannequins (value, baseprob, basecolor, addcolor, characterid, type) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, skin.getValue());
                ps.setInt(2, skin.getBaseProb());
                ps.setInt(3, skin.getBaseColor());
                ps.setInt(4, skin.getAddColor());
                ps.setInt(5, chr.getId());
                ps.setInt(6, 2);
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveDummyToDB(Connection con) {
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET choicepotential = ?, memorialcube = ?, returnscroll = ?, returnsc = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
            if (chr.choicepotential != null) {
                ps.setLong(1, chr.choicepotential.getInventoryId());
            } else {
                ps.setLong(1, 0);
            }
            if (chr.memorialcube != null) {
                ps.setLong(2, chr.memorialcube.getInventoryId());
            } else {
                ps.setLong(2, 0);
            }
            if (chr.returnscroll != null) {
                ps.setLong(3, chr.returnscroll.getInventoryId());
            } else {
                ps.setLong(3, 0);
            }
            ps.setInt(4, chr.returnSc);
            ps.setInt(5, chr.getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveCharToDB(Connection con) {
        synchronized (chr) {
            try {
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, secondSkincolor = ?, gender = ?, secondgender = ?, job = ?, hair = ?, basecolor = ?, addcolor = ?, baseprob = ?, secondhair = ?, face = ?, secondface = ?, demonMarking = ?, map = ?, meso = ?, hpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, subcategory = ?, marriageId = ?, currentrep = ?, totalrep = ?, fatigue = ?, charm = ?, charisma = ?, craft = ?, insight = ?, sense = ?, will = ?, totalwins = ?, totallosses = ?, pvpExp = ?, pvpPoints = ?, reborns = ?, apstorage = ?, name = ?, honourExp = ?, honourLevel = ?, soulcount = ?, itcafetime = ?, pets = ?, LinkMobCount = ?, secondbasecolor = ?, secondaddcolor = ?, secondbaseprob = ?, mesochair = ?, betaclothes = ?, exceptionlist = ?,  bpoint = ?, basebpoint = ?, bosstier = ?, tier = ?, levelpoint = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);

                ps.setInt(1, chr.getLevel());
                ps.setInt(2, chr.getFame());
                ps.setShort(3, chr.getStat().getStr());
                ps.setShort(4, chr.getStat().getDex());
                ps.setShort(5, chr.getStat().getLuk());
                ps.setShort(6, chr.getStat().getInt());
                ps.setLong(7, chr.getExp());
                ps.setLong(8, chr.getStat().getHp() < 1 ? 50 : chr.getStat().getHp());
                ps.setLong(9, chr.getStat().getMp());
                ps.setLong(10, chr.getStat().getMaxHp());
                ps.setLong(11, chr.getStat().getMaxMp());
                final StringBuilder sps = new StringBuilder();
                for (int i = 0; i < chr.getRemainingSps().length; i++) {
                    sps.append(chr.getRemainingSp(i));
                    sps.append(",");
                }
                String sp = sps.toString();
                ps.setString(12, sp.substring(0, sp.length() - 1));
                ps.setShort(13, chr.getRemainingAp());
                ps.setByte(14, (byte) chr.getGMLevel());
                ps.setByte(15, chr.getSkinColor());
                ps.setByte(16, chr.getSecondSkinColor());
                ps.setByte(17, chr.getGender());
                ps.setByte(18, chr.getSecondGender());
                ps.setShort(19, chr.getJob());
                ps.setInt(20, chr.getHair());
                ps.setInt(21, chr.getBaseColor());
                ps.setInt(22, chr.getAddColor());
                ps.setInt(23, chr.getBaseProb());
                ps.setInt(24, chr.getSecondHair());
                ps.setInt(25, chr.getFace());
                ps.setInt(26, chr.getSecondFace());
                ps.setInt(27, chr.getDemonMarking());
                /*if (!fromcs && map != null) {
                 if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                 ps.setInt(28, map.getForcedReturnId());
                 } else {
                 ps.setInt(28, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
                 }
                 } else {
                 ps.setInt(28, mapid);
                 }*/
                ps.setInt(28, ServerConstants.WarpMap);
                ps.setLong(29, chr.getMeso());
                ps.setShort(30, chr.getHpApUsed());
                if (chr.getMap() == null) {
                    ps.setByte(31, (byte) 0);
                } else {
                    final MaplePortal closest = chr.getMap().findClosestSpawnpoint(chr.getTruePosition());
                    ps.setByte(31, (byte) (closest != null ? closest.getId() : 0));
                }
                ps.setInt(32, chr.getParty() == null ? -1 : chr.getParty().getId());
                ps.setShort(33, chr.getBuddylist() == null ? 20 : chr.getBuddylist().getCapacity());
                ps.setByte(34, chr.getSubcategory());
                ps.setInt(35, chr.getMarriageId());
                ps.setInt(36, chr.getCurrentRep());
                ps.setInt(37, chr.getTotalRep());
                ps.setShort(38, chr.getFatigue());
                ps.setInt(39, chr.getTrait(MapleTrait.MapleTraitType.charm).getTotalExp());
                ps.setInt(40, chr.getTrait(MapleTrait.MapleTraitType.charisma).getTotalExp());
                ps.setInt(41, chr.getTrait(MapleTrait.MapleTraitType.craft).getTotalExp());
                ps.setInt(42, chr.getTrait(MapleTrait.MapleTraitType.insight).getTotalExp());
                ps.setInt(43, chr.getTrait(MapleTrait.MapleTraitType.sense).getTotalExp());
                ps.setInt(44, chr.getTrait(MapleTrait.MapleTraitType.will).getTotalExp());
                ps.setInt(45, chr.getTotalWins());
                ps.setInt(46, chr.getTotalLosses());
                ps.setInt(47, chr.getTotalBattleExp());
                ps.setInt(48, chr.getBattlePoints());
                /*Start of Custom Features*/
                ps.setInt(49, chr.getReborns());
                ps.setInt(50, chr.getAPS());
                /*End of Custom Features*/
                ps.setString(51, chr.getName());
                ps.setInt(52, chr.getHonourExp());
                ps.setInt(53, chr.getHonorLevel());
                ps.setInt(54, 0);
                ps.setInt(55, chr.getInternetCafeTime());
                sps.delete(0, sps.toString().length());
                for (int i = 0; i < 3; i++) {
                    if (chr.pets[i] != null) {
                        sps.append(chr.pets[i].getUniqueId());
                    } else {
                        sps.append("-1");
                    }
                    sps.append(",");
                }
                sp = sps.toString();

                ps.setString(56, sp.substring(0, sp.length() - 1));
                ps.setInt(57, chr.getLinkMobCount());
                ps.setInt(58, chr.getSecondBaseColor());
                ps.setInt(59, chr.getSecondAddColor());
                ps.setInt(60, chr.getSecondBaseProb());
                ps.setInt(61, chr.getMesoChairCount());
                ps.setInt(62, chr.getBetaClothes());

                final StringBuilder str = new StringBuilder();
                for (Integer excep : chr.getExceptionList()) {
                    sps.append(excep);
                    sps.append(",");
                }
                String exp = str.toString();
                if (exp.length() > 0) {
                    ps.setString(63, exp.substring(0, exp.length() - 1));
                } else {
                    ps.setString(63, exp);
                }
                ps.setLong(64, chr.bpoint);
                ps.setLong(65, chr.basebpoint);
                ps.setInt(66, chr.bosstier);
                ps.setInt(67, chr.tier);
                ps.setLong(68, chr.getLevelPoint());
                ps.setInt(69, chr.getId());

                if (ps.executeUpdate() < 1) {
                    ps.close();
                    throw new DatabaseException("Character not in database (" + chr.getId() + ")");
                }
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void savePetToDB(Connection con) {
        for (int i = 0; i < chr.pets.length; ++i) {
            if (chr.pets[i] != null) {
                chr.pets[i].saveToDb(con);
            }
        }
    }

    public void saveMatrixToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM matrix WHERE charid = ?");

            for (VMatrix matrix : chr.getMatrixs()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO matrix (`level`, `position`, `id`, `unlock`, `charid`) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, matrix.getLevel());
                ps.setInt(2, matrix.getPosition());
                ps.setInt(3, matrix.getId());
                ps.setByte(4, (byte) (matrix.isUnLock() ? 1 : 0));
                ps.setInt(5, chr.getId());
                ps.execute();
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCoreToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM core WHERE charid = ?");

            for (Core core : chr.getCore()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO core (crcid, coreid, level, exp, state, maxlevel, skill1, skill2, skill3, position, charid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, core.getCrcId());
                ps.setInt(2, core.getCoreId());
                ps.setInt(3, core.getLevel());
                ps.setInt(4, core.getExp());
                ps.setInt(5, core.getState());
                ps.setInt(6, core.getMaxlevel());
                ps.setInt(7, core.getSkill1());
                ps.setInt(8, core.getSkill2());
                ps.setInt(9, core.getSkill3());
                ps.setInt(10, core.getPosition());
                ps.setInt(11, chr.getId());
                ps.execute();
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSteelToDB(Connection con) {
//    	synchronized (copyCores) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM stolen WHERE characterid = ?");

            for (Pair<Integer, Boolean> st : chr.getStolenSkills()) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO stolen (characterid, skillid, chosen) VALUES (?, ?, ?)");
                ps.setInt(1, chr.getId());
                ps.setInt(2, st.left);
                ps.setInt(3, st.right ? 1 : 0);
                ps.execute();
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//    	}
    }

    public void saveMacroToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
            for (int i = 0; i < 5; i++) {
                final SkillMacro macro = chr.getMacros()[i];
                if (macro != null) {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, chr.getId());
                    ps.setInt(2, macro.getSkill1());
                    ps.setInt(3, macro.getSkill2());
                    ps.setInt(4, macro.getSkill3());
                    ps.setString(5, macro.getName());
                    ps.setInt(6, macro.getShout());
                    ps.setInt(7, i);
                    ps.execute();
                    ps.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSlotToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
            PreparedStatement ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`, `decoration`) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.getId());
            ps.setShort(2, chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit());
            ps.setShort(3, chr.getInventory(MapleInventoryType.USE).getSlotLimit());
            ps.setShort(4, chr.getInventory(MapleInventoryType.SETUP).getSlotLimit());
            ps.setShort(5, chr.getInventory(MapleInventoryType.ETC).getSlotLimit());
            ps.setShort(6, chr.getInventory(MapleInventoryType.CASH).getSlotLimit());
            ps.setShort(7, chr.getInventory(MapleInventoryType.DECORATION).getSlotLimit());
            ps.execute();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveQuestInfoToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
            PreparedStatement ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?)");
            ps.setInt(1, chr.getId());

            for (Map.Entry<Integer, String> q : chr.getInfoQuest_Map().entrySet()) {
                ps.setInt(2, q.getKey());
                ps.setString(3, q.getValue());
                ps.execute();
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveQuestStatusToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            PreparedStatement ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, chr.getId());

            for (MapleQuestStatus q : chr.getQuest_Map().values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                ResultSet rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        PreparedStatement pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                        pse.close();
                    }
                }
                rs.close();
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSkillToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
            PreparedStatement ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, chr.getId());

            for (Map.Entry<Skill, SkillEntry> skill : chr.getSkills().entrySet()) {
                if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                    ps.setInt(2, skill.getKey().getId());
                    ps.setInt(3, skill.getValue().skillevel);
                    ps.setByte(4, skill.getValue().masterlevel);
                    ps.setLong(5, skill.getValue().expiration);
                    ps.execute();
                }
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveInnerToDB(Connection con) {
        try {
            if (chr.getInnerSkills() != null) {
                chr.deleteWhereCharacterId(con, "DELETE FROM inner_ability_skills WHERE player_id = ?");
                PreparedStatement ps = con.prepareStatement("INSERT INTO inner_ability_skills (player_id, skill_id, skill_level, max_level, rank) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, chr.getId());

                for (int i = 0; i < chr.getInnerSkills().size(); ++i) {
                    ps.setInt(2, chr.getInnerSkills().get(i).getSkillId());
                    ps.setInt(3, chr.getInnerSkills().get(i).getSkillLevel());
                    ps.setInt(4, chr.getInnerSkills().get(i).getMaxLevel());
                    ps.setInt(5, chr.getInnerSkills().get(i).getRank());
                    ps.execute();
                }
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveKeyValueToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM keyvalue WHERE `id` = ?");
            PreparedStatement ps = con.prepareStatement("INSERT INTO keyvalue (`id`, `key`, `value`) VALUES (?, ?, ?)");
            ps.setInt(1, chr.getId());

            for (Map.Entry<String, String> keyValue : chr.getKeyValues().entrySet()) {
                ps.setString(2, keyValue.getKey());
                ps.setString(3, keyValue.getValue());
                ps.execute();
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCooldownToDB(Connection con, boolean dc) {
        try {
            List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
            chr.deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");
            if (dc && cd.size() > 0) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, chr.getId());
                for (final MapleCoolDownValueHolder cooling : cd) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveRockToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
            PreparedStatement ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
            ps.setInt(1, chr.getId());
            for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (chr.getSavedLocations()[savedLocationType.getValue()] != -1) {
                    ps.setInt(2, savedLocationType.getValue());
                    ps.setInt(3, chr.getSavedLocations()[savedLocationType.getValue()]);
                    ps.execute();
                }
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*    public void saveExtendedSlotsToDB(Connection con) {
     try {
     if (chr.changed_extendedSlots) {
     chr.deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?");
     for (int i : chr.extendedSlots) {
     if (chr.getInventory(MapleInventoryType.ETC).findById(i) != null) { //just in case
     PreparedStatement ps = con.prepareStatement("INSERT INTO extendedSlots(characterid, itemId) VALUES(?, ?) ");
     ps.setInt(1, chr.getId());
     ps.setInt(2, i);
     ps.execute();
     }
     }
     }
     } catch (Exception e) {
     e.printStackTrace();
     }
     }*/
    public void saveBuddyToDB(Connection con) {
        try {
            chr.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE accid = ? AND pending = 0", chr.getClient().getAccID());
            PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (accid, `buddyaccid`, `repname`, `pending`, `groupname`, `memo`) VALUES (?, ?, ?, 0, ?, ?)");
            ps.setInt(1, chr.getClient().getAccID());
            for (BuddylistEntry entry : chr.getBuddylist().getBuddies()) {
                if (entry.isVisible()) {
                    ps.setInt(2, entry.getAccountId());
                    ps.setString(3, entry.getRepName());
                    ps.setString(4, entry.getGroupName());
                    ps.setString(5, entry.getMemo() == null ? "" : entry.getMemo());
                    ps.execute();
                }
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveToDB(MapleCharacter player, boolean dc, boolean fromcs) {
        chr = player;
        Connection con = null;
        ReentrantLock LockObj = new ReentrantLock();
        LockObj.lock();
        try {
            con = DatabaseConnection.getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            saveCharToDB(con);

            saveCoreToDB(con);

            saveMatrixToDB(con);

            saveQuestInfoToDB(con);

            saveQuestStatusToDB(con);

            saveSkillToDB(con);

            saveInnerToDB(con);

            savePetToDB(con);

            saveSteelToDB(con);

            saveMacroToDB(con);

            saveSlotToDB(con);

            saveInventory(con, dc);

            saveCooldownToDB(con, dc);

            saveRockToDB(con);

            saveBuddyToDB(con);

            saveKeyValueToDB(con);

            if (chr.getClient() != null) {
                chr.getClient().saveKeyValueToDB(con);
            }

            if (chr.getUnions() != null) {
                chr.getUnions().savetoDB(con, chr.getAccountID());
            }

            saveDummyToDB(con);

            saveMannequinToDB(con);

            if (chr.getStorage() != null) {
                chr.getStorage().saveToDB(con);
            }

            if (chr.getCashInventory() != null) {
                chr.getCashInventory().save(con);
            }

            PlayerNPC.updateByCharId(chr);

            chr.getKeyLayout().saveKeys(con, chr.getId());
            chr.getMount().saveMount(con, chr.getId());
            con.commit();
            chr.lastSaveTime = System.currentTimeMillis();

        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            try {
                con.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (con != null) {
                    con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                e.printStackTrace();
            }
            LockObj.unlock();
        }
    }
}
