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
package client.inventory;

import constants.GameConstants;
import database.DatabaseConnection;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.Pair;
import tools.Triple;

import java.awt.*;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MapleAndroid implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private int stance = 0, uniqueid, itemid, hair, face, skin, gender;
    private boolean ear;
    private String name;
    private Point pos = new Point(0, 0);
    private boolean changed = false;

    private MapleAndroid(final int itemid, final int uniqueid) {
        this.itemid = itemid;
        this.uniqueid = uniqueid;
    }

    public static final MapleAndroid loadFromDb(final int itemid, final int uid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final MapleAndroid ret = new MapleAndroid(itemid, uid);

            con = DatabaseConnection.getConnection(); // Get a connection to the database
            ps = con.prepareStatement("SELECT * FROM androids WHERE uniqueid = ?"); // Get pet details..
            ps.setInt(1, uid);

            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }

            ret.setHair(rs.getInt("hair"));
            ret.setFace(rs.getInt("face"));
            ret.setSkin(rs.getInt("skin"));
            ret.setGender(rs.getInt("gender"));
            ret.setName(rs.getString("name"));
            ret.setEar(rs.getBoolean("ear"));
            ret.changed = false;

            rs.close();
            ps.close();
            con.close();

            return ret;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public final void saveToDb() {
        if (!changed) {
            return;
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE androids SET hair = ?, face = ?, name = ?, skin = ?, gender = ?, ear = ? WHERE uniqueid = ?");
            ps.setInt(1, hair);
            ps.setInt(2, face);
            ps.setString(3, name);
            ps.setInt(4, skin);
            ps.setInt(5, gender);
            ps.setBoolean(6, ear); // Set ID
            ps.setInt(7, uniqueid); // Set ID
            ps.executeUpdate(); // Execute statement
            ps.close();
            con.close();
            changed = false;
        } catch (final SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static final MapleAndroid create(final int itemid, final int uniqueid) {
        Triple<Pair<List<Integer>, List<Integer>>, List<Integer>, Integer> aInfo = MapleItemInformationProvider.getInstance().getAndroidInfo(GameConstants.getAndroidType(itemid));
        if (aInfo == null) {
            return null;
        }
        return create(itemid, uniqueid, aInfo.left.left.get(Randomizer.nextInt(aInfo.left.left.size())), aInfo.left.right.get(Randomizer.nextInt(aInfo.left.right.size())), aInfo.mid.get(Randomizer.nextInt(aInfo.mid.size())), aInfo.right);
    }

    public static final MapleAndroid create(int itemid, int uniqueid, int hair, int face, int skin, int gender) {
        if (uniqueid <= -1) { //wah
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        skin -= 2000;
        Connection con = null;
        PreparedStatement pse = null;
        try { // Commit to db first
            con = DatabaseConnection.getConnection();
            pse = con.prepareStatement("INSERT INTO androids (uniqueid, hair, face, skin, gender, name, ear) VALUES (?, ?, ?, ?, ?, ?, ?)");
            pse.setInt(1, uniqueid);
            pse.setInt(2, hair);
            pse.setInt(3, face);
            pse.setInt(4, skin);
            pse.setInt(5, gender);
            pse.setString(6, "안드로이드");
            pse.setBoolean(7, true);
            pse.executeUpdate();
            pse.close();
            con.close();
        } catch (final SQLException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        final MapleAndroid pet = new MapleAndroid(itemid, uniqueid);
        pet.setHair(hair);
        pet.setFace(face);
        pet.setSkin(skin);
        pet.setGender(gender);
        pet.setName("안드로이드");
        pet.setEar(true);
        return pet;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public final void setEar(final boolean ear) {
        this.ear = ear;
        this.changed = true;
    }

    public final boolean getEar() {
        return ear;
    }

    public final void setHair(final int closeness) {
        this.hair = closeness;
        this.changed = true;
    }

    public final int getHair() {
        return hair;
    }

    public final int getSkin() {
        return skin;
    }

    public final int getGender() {
        return gender;
    }

    public final void setFace(final int closeness) {
        this.face = closeness;
        this.changed = true;
    }

    public final void setSkin(final int closeness) {
        this.skin = closeness;
        this.changed = true;
    }

    public final void setGender(final int ged) {
        this.gender = ged;
        this.changed = true;
    }

    public final int getFace() {
        return face;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
        this.changed = true;
    }

    public final Point getPos() {
        return pos;
    }

    public final void setPos(final Point pos) {
        this.pos = pos;
    }

    public final int getStance() {
        return stance;
    }

    public final void setStance(final int stance) {
        this.stance = stance;
    }

    public final int getItemId() {
        return itemid;
    }

    public final void updatePosition(final List<LifeMovementFragment> movement) {
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    setPos(((LifeMovement) move).getPosition());
                }
                setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
