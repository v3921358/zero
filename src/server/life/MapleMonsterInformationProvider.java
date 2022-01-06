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
package server.life;

import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapleMonsterInformationProvider {

    private static final MapleMonsterInformationProvider instance = new MapleMonsterInformationProvider();
    private final Map<Integer, ArrayList<MonsterDropEntry>> drops = new HashMap<Integer, ArrayList<MonsterDropEntry>>();

    public static MapleMonsterInformationProvider getInstance() {
        return instance;
    }

    public ArrayList<MonsterDropEntry> retrieveDrop(final int monsterId) {
        if (drops.containsKey(monsterId)) {
            return drops.get(monsterId);
        } else {
            final ArrayList<MonsterDropEntry> ret = new ArrayList<MonsterDropEntry>();

            PreparedStatement ps = null;
            ResultSet rs = null;
            Connection con = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM drop_data WHERE dropperid = ?");
                ps.setInt(1, monsterId);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(new MonsterDropEntry(
                            rs.getInt("itemid"),
                            rs.getInt("chance"),
                            rs.getInt("minimum_quantity"),
                            rs.getInt("maximum_quantity"),
                            rs.getInt("questid")));
                }

                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0");
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.add(
                            new MonsterDropEntry(
                                    rs.getInt("itemid"),
                                    rs.getInt("chance"),
                                    //                                    rs.getInt("continent"),
                                    //                                  rs.getByte("dropType"),
                                    rs.getInt("minimum_quantity"),
                                    rs.getInt("maximum_quantity"),
                                    rs.getInt("questid")));
                }
                ps.close();
                rs.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException ignore) {
                    ignore.printStackTrace();
                    return ret;
                }
            }
            drops.put(Integer.valueOf(monsterId), ret);
            return ret;
        }
    }

    public void clearDrops() {
        drops.clear();
    }

    public boolean contains(ArrayList<MonsterDropEntry> e, int toAdd) {
        for (MonsterDropEntry f : e) {
            if (f.itemId == toAdd) {
                return true;
            }
        }
        return false;
    }

    public int chanceLogic(int itemId) { //not much logic in here. most of the drops should already be there anyway.
        if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
            return 50000; //with *10
        } else if (GameConstants.getInventoryType(itemId) == MapleInventoryType.SETUP || GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH) {
            return 500;
        } else {
            switch (itemId / 10000) {
                case 204:
                case 207:
                case 233:
                case 229:
                    return 500;
                case 401:
                case 402:
                    return 5000;
                case 403:
                    return 5000; //lol
            }
            return 20000;
        }
    }
    //MESO DROP: level * (level / 10) = max, min = 0.66 * max
    //explosive Reward = 7 meso drops
    //boss, ffaloot = 2 meso drops
    //boss = level * level = max
    //no mesos if: mobid / 100000 == 97 or 95 or 93 or 91 or 90 or removeAfter > 0 or invincible or onlyNormalAttack or friendly or dropitemperiod > 0 or cp > 0 or point > 0 or fixeddamage > 0 or selfd > 0 or mobType != null and mobType.charat(0) == 7 or PDRate <= 0
}
