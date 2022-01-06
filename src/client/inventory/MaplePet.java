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

import database.DatabaseConnection;
import server.MapleItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

import java.awt.*;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaplePet implements Serializable {

    public static enum PetFlag {

        ITEM_PICKUP(0x01, 5190000, 5191000),
        EXPAND_PICKUP(0x02, 5190002, 5191002),
        AUTO_PICKUP(0x04, 5190003, 5191003),
        UNPICKABLE(0x08, 5190005, -1),
        LEFTOVER_PICKUP(0x10, 5190004, 5191004),
        HP_CHARGE(0x20, 5190001, 5191001),
        MP_CHARGE(0x40, 5190006, -1),
        PET_BUFF(0x80, 5190010, -1),
        PET_TRAINING(0x100, 5190011, -1),
        PET_GIANT(0x200, 5190012, -1),
        PET_SHOP(0x400, 5190013, -1);
        private final int i, item, remove;

        private PetFlag(int i, int item, int remove) {
            this.i = i;
            this.item = item;
            this.remove = remove;
        }

        public final int getValue() {
            return i;
        }

        public final boolean check(int flag) {
            return (flag & i) == i;
        }

        public static final PetFlag getByAddId(final int itemId) {
            for (PetFlag flag : PetFlag.values()) {
                if (flag.item == itemId) {
                    return flag;
                }
            }
            return null;
        }

        public static final PetFlag getByDelId(final int itemId) {
            for (PetFlag flag : PetFlag.values()) {
                if (flag.remove == itemId) {
                    return flag;
                }
            }
            return null;
        }
    }

    private static final long serialVersionUID = 9179541993413738569L;
    private String name;
    private int Fh = 0, stance = 0, color = -1, uniqueid, petitemid, secondsLeft = 0, buffSkillId = 0, wonderGrade = -1;
    private Point pos;
    private byte fullness = 100, level = 1, summoned = 0;
    private short inventorypos = 0, closeness = 0, flags = 0, size = 100;
    private boolean changed = false;

    private MaplePet(final int petitemid, final int uniqueid) {
        this.petitemid = petitemid;
        this.uniqueid = uniqueid;
    }

    private MaplePet(final int petitemid, final int uniqueid, final short inventorypos) {
        this.petitemid = petitemid;
        this.uniqueid = uniqueid;
        this.inventorypos = inventorypos;
    }

    public static final MaplePet loadFromDb(final int itemid, final int petid, final short inventorypos) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final MaplePet ret = new MaplePet(itemid, petid, inventorypos);

            con = DatabaseConnection.getConnection(); // Get a connection to the database
            ps = con.prepareStatement("SELECT * FROM pets WHERE petid = ?"); // Get pet details..
            ps.setInt(1, petid);

            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }

            ret.setName(rs.getString("name"));
            ret.setCloseness(rs.getShort("closeness"));
            ret.setLevel(rs.getByte("level"));
            ret.setFullness(rs.getByte("fullness"));
            ret.setSecondsLeft(rs.getInt("seconds"));
            ret.setFlags(rs.getShort("flags"));
            ret.setBuffSkillId(rs.getInt("petbuff"));
            ret.setPetSize(rs.getShort("size"));
            ret.setWonderGrade(rs.getInt("wonderGrade"));
            ret.setChanged(false);
            rs.close();
            ps.close();
            con.close();

            return ret;
        } catch (SQLException ex) {
            Logger.getLogger(MaplePet.class.getName()).log(Level.SEVERE, null, ex);
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

    public final void saveToDb(Connection con) {
        if (!isChanged()) {
            return;
        }
        try {
            final PreparedStatement ps = con.prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, seconds = ?, flags = ?, petbuff = ?, size = ?, wonderGrade = ? WHERE petid = ?");
            ps.setString(1, name); // Set name
            ps.setByte(2, level); // Set Level
            ps.setShort(3, closeness); // Set Closeness
            ps.setByte(4, fullness); // Set Fullness
            ps.setInt(5, secondsLeft);
            ps.setShort(6, flags);
            ps.setInt(7, buffSkillId);
            ps.setShort(8, size);
            ps.setInt(9, wonderGrade);
            ps.setInt(10, uniqueid); // Set ID
            ps.executeUpdate(); // Execute statement
            ps.close();
            setChanged(false);
        } catch (final SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static final MaplePet createPet(final int itemid, final int uniqueid) {
        return createPet(itemid, MapleItemInformationProvider.getInstance().getName(itemid), 1, 0, 100, uniqueid, 18000, (short) 0);
    }

    public static final MaplePet createPet(int itemid, String name, int level, int closeness, int fullness, int uniqueid, int secondsLeft, short flag) {
        if (uniqueid <= -1) { //wah
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        Connection con = null;
        PreparedStatement pse = null;
        try { // Commit to db first
            con = DatabaseConnection.getConnection();
            pse = con.prepareStatement("INSERT INTO pets (petid, name, level, closeness, fullness, seconds, flags, size, wonderGrade) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            pse.setInt(1, uniqueid);
            pse.setString(2, name);
            pse.setByte(3, (byte) level);
            pse.setShort(4, (short) closeness);
            pse.setByte(5, (byte) fullness);
            pse.setInt(6, secondsLeft);
            pse.setShort(7, flag);
            pse.setShort(8, (short) 100);
            pse.setInt(9, PetDataFactory.getWonderGrade(itemid));
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
        final MaplePet pet = new MaplePet(itemid, uniqueid);
        pet.setName(name);
        pet.setLevel(level);
        pet.setFullness(fullness);
        pet.setCloseness(closeness);
        pet.setFlags(flag);
        pet.setSecondsLeft(secondsLeft);
        pet.setWonderGrade(PetDataFactory.getWonderGrade(itemid));
        pet.setPetSize((short) 100);
        return pet;
    }

    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
        this.setChanged(true);
    }

    public final boolean getSummoned() {
        return summoned > 0;
    }

    public final byte getSummonedValue() {
        return summoned;
    }

    public final void setSummoned(final byte summoned) {
        this.summoned = summoned;
    }

    public final short getInventoryPosition() {
        return inventorypos;
    }

    public final void setInventoryPosition(final short inventorypos) {
        this.inventorypos = inventorypos;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public final short getCloseness() {
        return closeness;
    }

    public final void setCloseness(final int closeness) {
        this.closeness = (short) closeness;
        this.setChanged(true);
    }

    public final byte getLevel() {
        return level;
    }

    public final void setLevel(final int level) {
        this.level = (byte) level;
        this.setChanged(true);
    }

    public final byte getFullness() {
        return 100;//fullness;
    }

    public final void setFullness(final int fullness) {
        this.fullness = (byte) fullness;
        this.setChanged(true);
    }

    public final short getFlags() {
        return 0x01 + 0x02 + 0x04 + 0x08 + 0x10 + 0x20 + 0x40 + 0x80 + 0x100;
    }

    /*
     ITEM_PICKUP(0x01, 5190000, 5191000),
     EXPAND_PICKUP(0x02, 5190002, 5191002),
     AUTO_PICKUP(0x04, 5190003, 5191003),
     UNPICKABLE(0x08, 5190005, -1),
     LEFTOVER_PICKUP(0x10, 5190004, 5191004),
     HP_CHARGE(0x20, 5190001, 5191001),
     MP_CHARGE(0x40, 5190006, -1),
     PET_BUFF(0x80, 5190010, -1),
     PET_TRAINING(0x100, 5190011, -1),
     PET_GIANT(0x200, 5190012, -1),
     PET_SHOP(0x400, 5190013, -1);
     */
    public final void setFlags(final int fffh) {
        this.flags = (short) fffh;
        this.setChanged(true);
    }

    public final int getBuffSkillId() {
        return buffSkillId;
    }

    public final void setBuffSkillId(int skillId) {
        this.buffSkillId = skillId;
        this.setChanged(true);
    }

    public final int getWonderGrade() {
        return wonderGrade;
    }

    public final void setWonderGrade(int grade) {
        this.wonderGrade = grade;
        this.setChanged(true);
    }

    public final short getPetSize() {
        return size;
    }

    public final void setPetSize(short size) {
        this.size = size;
        this.setChanged(true);
    }

    public void addPetSize(short size) {
        this.size += size;
        this.setChanged(true);
    }

    public final int getFh() {
        return Fh;
    }

    public final void setFh(final int Fh) {
        this.Fh = Fh;
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

    public final int getColor() {
        return color;
    }

    public final void setColor(final int color) {
        this.color = color;
    }

    public final int getPetItemId() {
        return petitemid;
    }

    public final boolean canConsume(final int itemId) {
        final MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (final int petId : mii.getItemEffect(itemId).getPetsCanConsume()) {
            if (petId == petitemid) {
                return true;
            }
        }
        return false;
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

    public final int getSecondsLeft() {
        return secondsLeft;
    }

    public final void setSecondsLeft(int sl) {
        this.secondsLeft = sl;
        this.setChanged(true);
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
