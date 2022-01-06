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
package handling.world.guild;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.World;
import log.DBLogger;
import log.LogType;
import server.MapleStatEffect;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.AlliancePacket;
import tools.packet.CWvsContext.GuildPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.PacketHelper;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MapleGuild implements java.io.Serializable {

    private static enum BCOp {

        NONE, DISBAND, EMBELMCHANGE
    }
    public static final long serialVersionUID = 6322150443228168192L;
    private final List<MapleGuildCharacter> members = new CopyOnWriteArrayList<MapleGuildCharacter>();
    private final List<MapleGuildCharacter> requests = new ArrayList<>();
    private final Map<Integer, MapleGuildSkill> guildSkills = new HashMap<Integer, MapleGuildSkill>();
    private final String rankTitles[] = new String[10]; // 1 = master, 2 = jr, 5 = lowest member
    private final int rankRoles[] = new int[10];
    private String name, notice;
    private double guildScore = 0.0;
    private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature, level, noblessskillpoint;
    private boolean bDirty = true, proper = true;
    private int allianceid = 0, invitedid = 0;
    private byte[] customEmblem;
    private boolean init = false, changed_skills = false, changed_requests = false;

    public MapleGuild(final int guildid) {
        super();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();

            if (!rs.first()) {
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
            logo = rs.getInt("logo");
            logoColor = rs.getInt("logoColor");
            logoBG = rs.getInt("logoBG");
            logoBGColor = rs.getInt("logoBGColor");
            capacity = rs.getInt("capacity");
            rankTitles[0] = rs.getString("rank1title");
            rankTitles[1] = rs.getString("rank2title");
            rankTitles[2] = rs.getString("rank3title");
            rankTitles[3] = rs.getString("rank4title");
            rankTitles[4] = rs.getString("rank5title");
            rankRoles[0] = rs.getInt("rank1role");
            rankRoles[1] = rs.getInt("rank2role");
            rankRoles[2] = rs.getInt("rank3role");
            rankRoles[3] = rs.getInt("rank4role");
            rankRoles[4] = rs.getInt("rank5role");
            leader = rs.getInt("leader");
            notice = rs.getString("notice");
            signature = rs.getInt("signature");
            allianceid = rs.getInt("alliance");
            guildScore = (double) rs.getInt("score");
            Blob custom = rs.getBlob("customEmblem");
            if (custom != null) {
                customEmblem = custom.getBytes(1L, (int) custom.length());
            }
            rs.close();
            ps.close();

            MapleGuildAlliance alliance = World.Alliance.getAlliance(allianceid);
            if (alliance == null) {
                allianceid = 0;
            }

            ps = con.prepareStatement("SELECT id, name, level, job, guildrank, guildContribution, alliancerank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC", ResultSet.CONCUR_UPDATABLE);
            ps.setInt(1, guildid);
            rs = ps.executeQuery();

            if (!rs.first()) {
                System.err.println("No members in guild " + id + ".  Impossible... guild is disbanding");
                rs.close();
                ps.close();
                writeToDB(true);
                proper = false;
                return;
            }
            boolean leaderCheck = false;
            byte gFix = 0, aFix = 0;
            do {
                int cid = rs.getInt("id");
                byte gRank = rs.getByte("guildrank"), aRank = rs.getByte("alliancerank");

                if (cid == leader) {
                    leaderCheck = true;
                    if (gRank != 1) { //needs updating to 1
                        gRank = 1;
                        gFix = 1;
                    }
                    if (alliance != null) {
                        if (alliance.getLeaderId() == cid && aRank != 1) {
                            aRank = 1;
                            aFix = 1;
                        } else if (alliance.getLeaderId() != cid && aRank != 2) {
                            aRank = 2;
                            aFix = 2;
                        }
                    }
                } else {
                    if (gRank == 1) {
                        gRank = 2;
                        gFix = 2;
                    }
                    if (aRank < 3) {
                        aRank = 3;
                        aFix = 3;
                    }
                }
                members.add(new MapleGuildCharacter(cid, rs.getShort("level"), rs.getString("name"), (byte) -1, rs.getInt("job"), gRank, rs.getInt("guildContribution"), aRank, guildid, false));
            } while (rs.next());
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM guildsrequest WHERE gid = ?", ResultSet.CONCUR_UPDATABLE);
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            List<Pair<Integer, Integer>> request = new ArrayList<Pair<Integer, Integer>>();
            while (rs.next()) {
                request.add(new Pair<>(rs.getInt("cid"), rs.getInt("gid")));
            }
            rs.close();
            ps.close();

            for (Pair<Integer, Integer> chrid : request) {
                ps = con.prepareStatement("SELECT id, name, level, job, guildrank, guildContribution, alliancerank FROM characters WHERE id = ?", ResultSet.CONCUR_UPDATABLE);
                ps.setInt(1, (int) chrid.left);
                rs = ps.executeQuery();
                while (rs.next()) {
                    byte gRank = rs.getByte("guildrank"), aRank = rs.getByte("alliancerank");

                    if ((int) chrid.left == leader) {
                        leaderCheck = true;
                        if (gRank != 1) { //needs updating to 1
                            gRank = 1;
                            gFix = 1;
                        }
                        if (alliance != null) {
                            if (alliance.getLeaderId() == (int) chrid.left && aRank != 1) {
                                aRank = 1;
                                aFix = 1;
                            } else if (alliance.getLeaderId() != (int) chrid.left && aRank != 2) {
                                aRank = 2;
                                aFix = 2;
                            }
                        }
                    } else {
                        if (gRank == 1) {
                            gRank = 2;
                            gFix = 2;
                        }
                        if (aRank < 3) {
                            aRank = 3;
                            aFix = 3;
                        }
                    }
                    requests.add(new MapleGuildCharacter((int) chrid.left, rs.getShort("level"), rs.getString("name"), (byte) -1, rs.getInt("job"), gRank, rs.getInt("guildContribution"), aRank, (int) chrid.right, false));
                }
                rs.close();
                ps.close();
            }

            if (!leaderCheck) {
                System.err.println("Leader " + leader + " isn't in guild " + id + ".  Impossible... guild is disbanding.");
                writeToDB(true);
                proper = false;
                return;
            }

            if (gFix > 0) {
                ps = con.prepareStatement("UPDATE characters SET guildrank = ? WHERE id = ?");
                ps.setByte(1, gFix);
                ps.setInt(2, leader);
                ps.executeUpdate();
                ps.close();
            }
            if (aFix > 0) {
                ps = con.prepareStatement("UPDATE characters SET alliancerank = ? WHERE id = ?");
                ps.setByte(1, aFix);
                ps.setInt(2, leader);
                ps.executeUpdate();
                ps.close();
            }

            ps = con.prepareStatement("SELECT * FROM guildskills WHERE guildid = ?");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            while (rs.next()) {
                int sid = rs.getInt("skillid");
                if (sid < 91000000) { //hack
                    rs.close();
                    ps.close();
                    System.err.println("Skill " + sid + " is in guild " + id + ".  Impossible... guild is disbanding.");
                    writeToDB(true);
                    proper = false;
                    return;
                }
                guildSkills.put(sid, new MapleGuildSkill(sid, rs.getInt("level"), rs.getLong("timestamp"), rs.getString("purchaser"), "")); //activators not saved
            }
            rs.close();
            ps.close();
            con.close();
            level = calculateLevel();
        } catch (SQLException se) {
            System.err.println("unable to read guild information from sql");
            se.printStackTrace();
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

    public boolean isProper() {
        return proper;
    }

    public final void writeToDB(final boolean bDisband) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (!bDisband) {
                StringBuilder buf = new StringBuilder("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                for (int i = 1; i < 6; i++) {
                    buf.append("rank").append(i).append("title = ?, ");
                    buf.append("rank").append(i).append("role = ?, ");
                }
                buf.append("capacity = ?, notice = ?, alliance = ?, leader = ?, customEmblem = ?, noblesspoint = ?, score = ? WHERE guildid = ?");
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement(buf.toString());
                ps.setInt(1, gp);
                ps.setInt(2, logo);
                ps.setInt(3, logoColor);
                ps.setInt(4, logoBG);
                ps.setInt(5, logoBGColor);
                ps.setString(6, rankTitles[0]);
                ps.setInt(7, rankRoles[0]);
                ps.setString(8, rankTitles[1]);
                ps.setInt(9, rankRoles[1]);
                ps.setString(10, rankTitles[2]);
                ps.setInt(11, rankRoles[2]);
                ps.setString(12, rankTitles[3]);
                ps.setInt(13, rankRoles[3]);
                ps.setString(14, rankTitles[4]);
                ps.setInt(15, rankRoles[4]);
                ps.setInt(16, capacity);
                ps.setString(17, notice);
                ps.setInt(18, allianceid);
                ps.setInt(19, leader);
                Blob blob = null;
                if (customEmblem != null) {
                    blob = new javax.sql.rowset.serial.SerialBlob(customEmblem);
                }
                ps.setBlob(20, blob);
                ps.setInt(21, noblessskillpoint);
                ps.setInt(22, (int) guildScore);
                ps.setInt(23, id);
                ps.executeUpdate();
                ps.close();

                if (changed_skills) {
                    ps = con.prepareStatement("DELETE FROM guildskills WHERE guildid = ?");
                    ps.setInt(1, id);
                    ps.execute();
                    ps.close();

                    ps = con.prepareStatement("INSERT INTO guildskills(`guildid`, `skillid`, `level`, `timestamp`, `purchaser`) VALUES(?, ?, ?, ?, ?)");
                    ps.setInt(1, id);
                    for (MapleGuildSkill i : guildSkills.values()) {
                        ps.setInt(2, i.skillID);
                        ps.setByte(3, (byte) i.level);
                        ps.setLong(4, i.timestamp);
                        ps.setString(5, i.purchaser);
                        ps.execute();
                    }
                    ps.close();
                }
                changed_skills = false;

                if (changed_requests) {
                    ps = con.prepareStatement("DELETE FROM guildsrequest WHERE gid = ?");
                    ps.setInt(1, id);
                    ps.execute();
                    ps.close();

                    ps = con.prepareStatement("INSERT INTO guildsrequest(`gid`, `cid`) VALUES(?, ?)");
                    for (MapleGuildCharacter mgc : requests) {
                        ps.setInt(1, mgc.getGuildId());
                        ps.setInt(2, mgc.getId());
                        ps.execute();
                    }
                    ps.close();
                }
                changed_requests = false;
            } else {
                con = DatabaseConnection.getConnection();
                try {
                    ps = con.prepareStatement("DELETE FROM guildskills WHERE guildid = ?");
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    ps.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                ps.close();
                if (allianceid > 0) {
                    final MapleGuildAlliance alliance = World.Alliance.getAlliance(allianceid);
                    if (alliance != null) {
                        alliance.removeGuild(id, false);
                    }
                }

                broadcast(GuildPacket.guildDisband(id));
                broadcast(GuildPacket.guildDisband2());
            }
        } catch (SQLException se) {
            System.err.println("Error saving guild to SQL");
            se.printStackTrace();
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

    public final int getId() {
        return id;
    }

    public final int getLeaderId() {
        return leader;
    }

    public final MapleCharacter getLeader(final MapleClient c) {
        return c.getChannelServer().getPlayerStorage().getCharacterById(leader);
    }

    public final int getGP() {
        return gp;
    }

    public final int getLogo() {
        return logo;
    }

    public final void setLogo(final int l) {
        logo = l;
    }

    public final int getLogoColor() {
        return logoColor;
    }

    public final void setLogoColor(final int c) {
        logoColor = c;
    }

    public final int getLogoBG() {
        return logoBG;
    }

    public final void setLogoBG(final int bg) {
        logoBG = bg;
    }

    public final int getLogoBGColor() {
        return logoBGColor;
    }

    public final void setLogoBGColor(final int c) {
        logoBGColor = c;
    }

    public final String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public final String getName() {
        return name;
    }

    public final int getCapacity() {
        return capacity;
    }

    public final int getSignature() {
        return signature;
    }

    public final void RankBroadCast(final byte[] packet, final int rank) {

        for (MapleGuildCharacter mgc : members) {
            if (mgc.isOnline() && mgc.getGuildRank() == rank) {
                int ch = World.Find.findChannel(mgc.getId());
                if (ch < 0) {
                    return;
                }
                final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(mgc.getId());
                if (c != null && c.getGuildId() == mgc.getGuildId()) {
                    c.getClient().getSession().writeAndFlush(packet);
                }
            }
        }
    }

    public final void broadcast(final byte[] packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public final void broadcast(final byte[] packet, final int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    // multi-purpose function that reaches every member of guild (except the character with exceptionId) in all channels with as little access to rmi as possible
    public final void broadcast(final byte[] packet, final int exceptionId, final BCOp bcop) {
        broadcast(packet, exceptionId, bcop, null);
    }

    public final void broadcast(final byte[] packet, final int exceptionId, final BCOp bcop, MapleCharacter chr) {
        buildNotifications();

        for (MapleGuildCharacter mgc : members) {
            if (bcop == BCOp.DISBAND) {
                if (mgc.isOnline()) {
                    World.Guild.setGuildAndRank(mgc.getId(), 0, 5, 0, 5);
                } else {
                    setOfflineGuildStatus(0, (byte) 5, 0, (byte) 5, mgc.getId());
                }
            } else if (mgc.isOnline() && mgc.getId() != exceptionId) {
                if (bcop == BCOp.EMBELMCHANGE) {
                    World.Guild.changeEmblem(chr, mgc.getId(), this);
                } else {
                    World.Broadcast.sendGuildPacket(mgc.getId(), packet, exceptionId, id);
                }
            }
        }
    }

    private final void buildNotifications() {
        if (!bDirty) {
            return;
        }
        final List<Integer> mem = new LinkedList<Integer>();
        final Iterator<MapleGuildCharacter> toRemove = members.iterator();
        while (toRemove.hasNext()) {
            MapleGuildCharacter mgc = toRemove.next();
            if (!mgc.isOnline()) {
                continue;
            }
            if (mem.contains(mgc.getId()) || mgc.getGuildId() != id) {
                members.remove(mgc);
                continue;
            }
            mem.add(mgc.getId());
        }
        bDirty = false;
    }

    public final void setOnline(final int cid, final boolean online, final int channel) {
        boolean bBroadcast = true;
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getGuildId() == id && mgc.getId() == cid) {
                if (mgc.isOnline() == online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel((byte) channel);
                break;
            }
        }
        if (bBroadcast) {
            broadcast(GuildPacket.guildMemberOnline(id, cid, online), cid);
            if (allianceid > 0) {
                World.Alliance.sendGuild(AlliancePacket.allianceMemberOnline(allianceid, id, cid, online), id, allianceid);
            }
        }
        bDirty = true; // member formation has changed, update notifications
        init = true;
    }

    public final void guildChat(MapleCharacter player, final String msg, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
        Item item = null;
        if (recv == RecvPacketOpcode.PARTYCHATITEM) {
            byte invType = (byte) slea.readInt();
            byte pos = (byte) slea.readInt();
            item = player.getInventory(MapleInventoryType.getByType((pos > 0 ? invType : -1))).getItem(pos);
        }
        DBLogger.getInstance().logChat(LogType.Chat.Guild, player.getId(), name, msg, "[" + getName() + "]");

        broadcast(CField.multiChat(player, msg, 2, item), player.getId());
    }

    public final void allianceChat(MapleCharacter player, final String msg, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
        Item item = null;
        if (recv == RecvPacketOpcode.PARTYCHATITEM) {
            byte invType = (byte) slea.readInt();
            byte pos = (byte) slea.readInt();
            item = player.getInventory(MapleInventoryType.getByType((pos > 0 ? invType : -1))).getItem(pos);
        }
        broadcast(CField.multiChat(player, msg, 3, item), player.getId());
    }

    public final String getRankTitle(final int rank) {
        return rankTitles[rank - 1];
    }

    public final int getRankRole(final int role) {
        return rankRoles[role - 1];
    }

    public final byte[] getCustomEmblem() {
        return customEmblem;
    }

    public final void setCustomEmblem(byte[] emblem) {
        this.customEmblem = emblem;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            Blob blob = null;
            if (emblem != null) {
                blob = new javax.sql.rowset.serial.SerialBlob(emblem);
            }
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET customEmblem = ? WHERE guildid = ?");
            ps.setBlob(1, blob);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
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

    public int getAllianceId() {
        //return alliance.getId();
        return this.allianceid;
    }

    public int getInvitedId() {
        return this.invitedid;
    }

    public void setInvitedId(int iid) {
        this.invitedid = iid;
    }

    public void setAllianceId(int a) {
        this.allianceid = a;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET alliance = ? WHERE guildid = ?");
            ps.setInt(1, a);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Saving allianceid ERROR" + e);
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

    // function to create guild, returns the guild id if successful, 0 if not
    public static final int createGuild(final int leaderId, final String name) {
        if (name.length() > 12) {
            return 0;
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();

            if (rs.first()) {// name taken
                rs.close();
                ps.close();
                con.close();
                return 0;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`, `alliance`) VALUES (?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, leaderId);
            ps.setString(2, name);
            ps.setInt(3, (int) (System.currentTimeMillis() / 1000));
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            int ret = 0;
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            rs.close();
            ps.close();
            con.close();
            return ret;
        } catch (SQLException se) {
            System.err.println("SQL THROW");
            se.printStackTrace();
            return 0;
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

    public final int addGuildMember(final MapleGuildCharacter mgc) {
        // first of all, insert it into the members keeping alphabetical order of lowest ranks ;)
        if (members.size() >= capacity) {
            return 0;
        }
        for (int i = members.size() - 1; i >= 0; i--) {
            if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                members.add(i + 1, mgc);
                bDirty = true;
                break;
            }
        }
        gainGP(500, true, mgc.getId());
        broadcast(GuildPacket.newGuildMember(mgc));
        if (allianceid > 0) {
            World.Alliance.sendGuild(allianceid);
        }
        return 1;
    }

    public final void leaveGuild(final MapleGuildCharacter mgc) {
        final Iterator<MapleGuildCharacter> itr = members.iterator();
        while (itr.hasNext()) {
            final MapleGuildCharacter mgcc = itr.next();

            if (mgcc.getId() == mgc.getId()) {
                for (int i = 1; i <= 5; i++) {
                    RankBroadCast(GuildPacket.memberLeft(mgcc, (i != 5)), i);
                }
                //broadcast(GuildPacket.memberLeft(mgcc, true));
                bDirty = true;
                gainGP(mgcc.getGuildContribution() > 0 ? -mgcc.getGuildContribution() : -50);
                members.remove(mgcc);
                if (mgc.isOnline()) {
                    World.Guild.setGuildAndRank(mgcc.getId(), 0, 5, 0, 5);
                } else {
                    setOfflineGuildStatus((short) 0, (byte) 5, 0, (byte) 5, mgcc.getId());
                }
                break;
            }
        }
        if (bDirty && allianceid > 0) {
            World.Alliance.sendGuild(allianceid);
        }
    }

    public final void expelMember(final MapleGuildCharacter initiator, final String name, final int cid) {
        final Iterator<MapleGuildCharacter> itr = members.iterator();
        while (itr.hasNext()) {
            final MapleGuildCharacter mgc = itr.next();

            if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                broadcast(GuildPacket.memberLeft(mgc, true));
                bDirty = true;

                gainGP(mgc.getGuildContribution() > 0 ? -mgc.getGuildContribution() : -50);
                if (mgc.isOnline()) {
                    World.Guild.setGuildAndRank(cid, 0, 5, 0, 5);
                } else {
                    MapleCharacterUtil.sendNote(mgc.getName(), initiator.getName(), "길드에서 강퇴당했습니다.", 0);
                    setOfflineGuildStatus((short) 0, (byte) 5, 0, (byte) 5, cid);
                }
                members.remove(mgc);
                break;
            }
        }
        if (bDirty && allianceid > 0) {
            World.Alliance.sendGuild(allianceid);
        }
    }

    public final void changeARank() {
        changeARank(false);
    }

    public final void changeARank(final boolean leader) {
        if (allianceid <= 0) {
            return;
        }
        for (final MapleGuildCharacter mgc : members) {
            byte newRank = 3;
            if (this.leader == mgc.getId()) {
                newRank = (byte) (leader ? 1 : 2);
            }
            if (mgc.isOnline()) {
                World.Guild.setGuildAndRank(mgc.getId(), this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank);
            } else {
                setOfflineGuildStatus(this.id, (byte) mgc.getGuildRank(), mgc.getGuildContribution(), (byte) newRank, mgc.getId());
            }
            mgc.setAllianceRank((byte) newRank);
        }
        World.Alliance.sendGuild(allianceid);
    }

    public final void changeARank(final int newRank) {
        if (allianceid <= 0) {
            return;
        }
        for (final MapleGuildCharacter mgc : members) {
            if (mgc.isOnline()) {
                World.Guild.setGuildAndRank(mgc.getId(), this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank);
            } else {
                setOfflineGuildStatus(this.id, (byte) mgc.getGuildRank(), mgc.getGuildContribution(), (byte) newRank, mgc.getId());
            }
            mgc.setAllianceRank((byte) newRank);
        }
        World.Alliance.sendGuild(allianceid);
    }

    public final boolean changeARank(final int cid, final int newRank) {
        if (allianceid <= 0) {
            return false;
        }
        for (final MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                if (mgc.isOnline()) {
                    World.Guild.setGuildAndRank(cid, this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank);
                } else {
                    setOfflineGuildStatus(this.id, (byte) mgc.getGuildRank(), mgc.getGuildContribution(), (byte) newRank, cid);
                }
                mgc.setAllianceRank((byte) newRank);
                World.Alliance.sendGuild(allianceid);
                return true;
            }
        }
        return false;
    }

    public final void changeGuildLeader(final int cid) {
        if (changeRank(cid, 1) && changeRank(leader, 2)) {
            if (allianceid > 0) {
                int aRank = getMGC(leader).getAllianceRank();
                if (aRank == 1) {
                    World.Alliance.changeAllianceLeader(allianceid, cid, true);
                } else {
                    changeARank(cid, aRank);
                }
                changeARank(leader, 3);
            }
            broadcast(GuildPacket.guildLeaderChanged(id, leader, cid, allianceid));
            this.leader = cid;
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE guilds SET leader = ? WHERE guildid = ?");
                ps.setInt(1, cid);
                ps.setInt(2, id);
                ps.execute();
                ps.close();
                con.close();
            } catch (SQLException e) {
                System.err.println("Saving leaderid ERROR" + e);
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
    }

    public final boolean changeRank(final int cid, final int newRank) {
        for (final MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                if (mgc.isOnline()) {
                    World.Guild.setGuildAndRank(cid, this.id, newRank, mgc.getGuildContribution(), mgc.getAllianceRank());
                } else {
                    setOfflineGuildStatus(this.id, (byte) newRank, mgc.getGuildContribution(), (byte) mgc.getAllianceRank(), cid);
                }
                mgc.setGuildRank((byte) newRank);
                broadcast(GuildPacket.changeRank(mgc));
                return true;
            }
        }
        // it should never get to this point unless cid was incorrect o_O
        return false;
    }

    public final void setGuildNotice(final MapleCharacter chr, final String notice) {
        this.notice = notice;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET notice = ? WHERE guildid = ?");
            ps.setString(1, notice);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Saving guild notice ERROR");
            e.printStackTrace();
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
        broadcast(GuildPacket.guildNotice(chr, notice));
    }

    public final void memberLevelJobUpdate(final MapleGuildCharacter mgc) {
        for (final MapleGuildCharacter member : members) {
            if (member.getId() == mgc.getId()) {
                int old_level = member.getLevel();
                int old_job = member.getJobId();
                member.setJobId(mgc.getJobId());
                member.setLevel((short) mgc.getLevel());
                if (mgc.getLevel() > old_level) {
                    gainGP((mgc.getLevel() - old_level) * mgc.getLevel() * 5, false, mgc.getId());
                    //aftershock: formula changes (below 100 = 40, above 100 = 80) (12000 max) but i prefer level (21100 max), add guildContribution, do setGuildAndRank or just get the MapleCharacter object
                }
                if (old_level != mgc.getLevel()) {
                    this.broadcast(CWvsContext.sendLevelup(false, mgc.getLevel(), mgc.getName()), mgc.getId());
                }
                if (old_job != mgc.getJobId()) {
                    this.broadcast(CWvsContext.sendJobup(false, mgc.getJobId(), mgc.getName()), mgc.getId());
                }
                broadcast(GuildPacket.guildMemberLevelJobUpdate(mgc));
                if (allianceid > 0) {
                    World.Alliance.sendGuild(AlliancePacket.updateAlliance(mgc, allianceid), id, allianceid);
                }
                break;
            }
        }
    }

    public final void changeRankTitle(MapleCharacter chr, final String[] ranks) {
        int[] roles = rankRoles;
        for (int i = 0; i < 5; i++) {
            rankTitles[i] = ranks[i];
        }
        updateRankRole();
        broadcast(GuildPacket.rankTitleChange(chr, ranks, roles));
    }

    public final void changeRankRole(MapleCharacter chr, final int[] roles) {
        String[] ranks = rankTitles;
        for (int i = 0; i < 5; i++) {
            rankRoles[i] = roles[i];
        }
        updateRankRole();
        broadcast(GuildPacket.rankTitleChange(chr, ranks, roles));
    }

    public final void changeRankTitleRole(MapleCharacter chr, final String[] ranks, final int[] roles) {
        for (int i = 0; i < 5; i++) {
            rankRoles[i] = roles[i];
            rankTitles[i] = ranks[i];
        }
        updateRankRole();
        broadcast(GuildPacket.rankTitleChange(chr, ranks, roles));
    }

    public final void updateRankRole() {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            StringBuilder buf = new StringBuilder("UPDATE guilds SET ");
            for (int i = 1; i < 6; i++) {
                buf.append("rank").append(i).append("title = ?, ");
                buf.append("rank").append(i).append("role = ?");
                if (i != 5) {
                    buf.append(", ");
                }
            }
            buf.append(" WHERE guildid = ?");
            ps = con.prepareStatement(buf.toString());
            ps.setString(1, rankTitles[0]);
            ps.setInt(2, rankRoles[0]);
            ps.setString(3, rankTitles[1]);
            ps.setInt(4, rankRoles[1]);
            ps.setString(5, rankTitles[2]);
            ps.setInt(6, rankRoles[2]);
            ps.setString(7, rankTitles[3]);
            ps.setInt(8, rankRoles[3]);
            ps.setString(9, rankTitles[4]);
            ps.setInt(10, rankRoles[4]);
            ps.setInt(11, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Saving guild rank / roles ERROR");
            e.printStackTrace();
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

    public final void disbandGuild() {
        writeToDB(true);
        broadcast(null, -1, BCOp.DISBAND);
    }

    public final void setGuildEmblem(final MapleCharacter chr, final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        setCustomEmblem(null);
        broadcast(null, -1, BCOp.EMBELMCHANGE, chr);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?");
            ps.setInt(1, logo);
            ps.setInt(2, logoColor);
            ps.setInt(3, logoBG);
            ps.setInt(4, logoBGColor);
            ps.setInt(5, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Saving guild logo / BG colo ERROR");
            e.printStackTrace();
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

    public final void setGuildCustomEmblem(final MapleCharacter chr, byte[] imgdata) {
        this.logoBG = 0;
        this.logoBGColor = 0;
        this.logo = 0;
        this.logoColor = 0;
        setCustomEmblem(imgdata);
        broadcast(GuildPacket.changeCustomGuildEmblem(chr, imgdata));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?");
            ps.setInt(1, logo);
            ps.setInt(2, logoColor);
            ps.setInt(3, logoBG);
            ps.setInt(4, logoBGColor);
            ps.setInt(5, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Saving guild logo / BG colo ERROR");
            e.printStackTrace();
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

    public final MapleGuildCharacter getMGC(final int cid) {
        for (final MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public final boolean increaseCapacity(boolean trueMax) {
        if (capacity >= (trueMax ? 200 : 100) || ((capacity + 5) > (trueMax ? 200 : 100))) {
            return false;
        }
        capacity += 5;
        broadcast(GuildPacket.guildCapacityChange(this.id, this.capacity));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?");
            ps.setInt(1, this.capacity);
            ps.setInt(2, this.id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Saving guild capacity ERROR");
            e.printStackTrace();
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
        return true;
    }

    public final void gainGP(final int amount) {
        gainGP(amount, true, -1);
    }

    public final void gainGP(int amount, final boolean broadcast) {
        gainGP(amount, broadcast, -1);
    }

    public final void gainGP(int amount, final boolean broadcast, final int cid) {
        if (amount == 0 || calculateLevel() >= 30) { //no change, no broadcast and no sql.
            return;
        }
        if (amount + gp < 0) {
            amount = -gp;
        } //0 lowest
        if (cid > 0 && amount > 0) {
            final MapleGuildCharacter mg = getMGC(cid);
            if (mg != null) {
                mg.setGuildContribution(mg.getGuildContribution() + amount);
                if (mg.isOnline()) {
                    World.Guild.setGuildAndRank(cid, this.id, mg.getGuildRank(), mg.getGuildContribution(), mg.getAllianceRank());
                } else {
                    setOfflineGuildStatus(this.id, (byte) mg.getGuildRank(), mg.getGuildContribution(), (byte) mg.getAllianceRank(), cid);
                }
                broadcast(GuildPacket.guildContribution(id, cid, mg.getGuildContribution()));
            }
        }
        gp += amount;
        level = calculateLevel();
        broadcast(GuildPacket.updateGP(id, gp, level));
        if (broadcast) {
            broadcast(InfoPacket.getGPMsg(amount));
        }
    }

    public Collection<MapleGuildSkill> getSkills() {
        return guildSkills.values();
    }

    public Map<Integer, MapleGuildSkill> getGuildSkills() {
        return guildSkills;
    }

    public int getSkillLevel(int sid) {
        if (!guildSkills.containsKey(sid)) {
            return 0;
        }
        return guildSkills.get(sid).level;
    }

    public boolean activateSkill(int skill, String name) {
        if (!guildSkills.containsKey(skill)) {
            return false;
        }
        final MapleGuildSkill ourSkill = guildSkills.get(skill);
        final MapleStatEffect skillid = SkillFactory.getSkill(skill).getEffect(ourSkill.level);
        if (ourSkill.timestamp > System.currentTimeMillis() || skillid.getPeriod() <= 0) {
            return false;
        }
        ourSkill.timestamp = System.currentTimeMillis() + (skillid.getPeriod() * 60000L);
        ourSkill.activator = name;
        writeToDB(false);
        broadcast(GuildPacket.guildSkillPurchased(id, skill, ourSkill.level, ourSkill.timestamp, ourSkill.purchaser, name));
        return true;
    }

    public boolean purchaseSkill(int skill, String name, int cid) {
        final MapleStatEffect skillid = SkillFactory.getSkill(skill).getEffect(getSkillLevel(skill) + 1);
        if (skillid.getReqGuildLevel() > getLevel() || skillid.getLevel() <= getSkillLevel(skill)) {
            return false;
        }
        MapleGuildSkill ourSkill = guildSkills.get(skill);
        if (ourSkill == null) {
            ourSkill = new MapleGuildSkill(skill, skillid.getLevel(), 0, name, name);
            guildSkills.put(skill, ourSkill);
        } else {
            ourSkill.level = skillid.getLevel();
            ourSkill.purchaser = name;
            ourSkill.activator = name;
        }
        if (skillid.getPeriod() <= 0) {
            ourSkill.timestamp = -1L;
        } else {
            ourSkill.timestamp = System.currentTimeMillis() + (skillid.getPeriod() * 60000L);
        }
        changed_skills = true;
        writeToDB(false);
        broadcast(GuildPacket.guildSkillPurchased(id, skill, ourSkill.level, ourSkill.timestamp, name, name));
        return true;
    }

    public boolean removeSkill(int skill, String name) {
        if (guildSkills.containsKey(skill)) {
            guildSkills.remove(skill);
        }
        changed_skills = true;
        writeToDB(false);
        broadcast(GuildPacket.guildSkillPurchased(id, skill, 0, -1, name, name));
        return true;
    }

    public int getLevel() {
        return level;
    }

    public final int calculateLevel() {
        for (int i = 1; i < 30; i++) {
            if (gp < GameConstants.getGuildExpNeededForLevel(i)) {
                return i;
            }
        }
        return 30;
    }

    public final int calculateGuildPoints() {
        int rgp = gp;
        for (int i = 1; i < 30; i++) {
            if (rgp < GameConstants.getGuildExpNeededForLevel(i)) {
                return rgp;
            }
            rgp -= GameConstants.getGuildExpNeededForLevel(i);
        }
        return rgp;
    }

    public final void addMemberData(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(members.size());

        for (final MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
        }
        for (final MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId()); // 351 new
            mplew.writeAsciiString(mgc.getName(), 13);
            mplew.writeInt(mgc.getJobId()); //-1 = ??
            mplew.writeInt(mgc.getLevel()); //-1 = ??
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeLong(PacketHelper.getTime(-2)); // 1.2.329++
            mplew.writeInt(mgc.getAllianceRank());
            mplew.writeInt(mgc.getGuildContribution());//기여도
            mplew.writeInt(0);//소지중기여도
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeInt(0);// 
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeLong(System.currentTimeMillis()); // 343+

        }
    }

    public final void addRequestMemberData(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(requests.size());

        for (final MapleGuildCharacter mgc : requests) {
            mplew.writeInt(mgc.getId());
        }
        for (final MapleGuildCharacter mgc : requests) {
            mplew.writeInt(mgc.getId()); // 351 new
            mplew.writeAsciiString(mgc.getName(), 13);
            mplew.writeInt(mgc.getJobId()); //-1 = ??
            mplew.writeInt(mgc.getLevel()); //-1 = ??
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeLong(PacketHelper.getTime(-2)); // 1.2.329++
            mplew.writeInt(mgc.getAllianceRank());
            //mplew.writeInt(mgc.getGuildContribution());
            mplew.writeInt(mgc.getGuildContribution());//기여도
            mplew.writeInt(0);//소지중기여도
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeInt(0);// 
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeLong(System.currentTimeMillis()); // 343+
        }

        mplew.writeInt(requests.size()); // 351 new
        for (final MapleGuildCharacter mgc : requests) {
            mplew.writeMapleAsciiString("Zero"); // 자기소개
        }

    }

    public static final MapleGuildResponse sendInvite(final MapleClient c, final String targetName) {
        final MapleCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (mc == null) {
            return MapleGuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return MapleGuildResponse.ALREADY_IN_GUILD;
        }
        mc.getClient().getSession().writeAndFlush(GuildPacket.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getGuild().getName(), c.getPlayer()));
        return null;
    }

    public java.util.Collection<MapleGuildCharacter> getMembers() {
        return java.util.Collections.unmodifiableCollection(members);
    }

    public final boolean isInit() {
        return init;
    }

    public boolean hasSkill(int id) {
        return guildSkills.containsKey(id);
    }

    public static void setOfflineGuildStatus(int guildid, byte guildrank, int contribution, byte alliancerank, int cid) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, guildContribution = ?, alliancerank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, contribution);
            ps.setInt(4, alliancerank);
            ps.setInt(5, cid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
            se.printStackTrace();
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

    public int avergeMemberLevel() {
        if (members.size() < 10) {
            return 0;
        }
        int totallevel = 0;
        for (MapleGuildCharacter gc : members) {
            totallevel += gc.getLevel();
        }
        return (int) (totallevel / members.size());
    }

    public String getLeaderName() {
        for (MapleGuildCharacter gc : members) {
            if (gc.getId() == leader) {
                return gc.getName();
            }
        }
        return "없음";
    }

    public boolean addRequest(MapleGuildCharacter mgc) {
        changed_requests = true;
        final Iterator<MapleGuildCharacter> toRemove = requests.iterator();
        while (toRemove.hasNext()) {
            MapleGuildCharacter mgc2 = toRemove.next();

            if (mgc2.getId() == mgc.getId()) {
                return false;
            }
        }
        requests.add(mgc);
        return true;
    }

    public void removeRequest(int cid) {
        final Iterator<MapleGuildCharacter> toRemove = requests.iterator();
        while (toRemove.hasNext()) {
            MapleGuildCharacter mgc = toRemove.next();
            if (mgc.getId() == cid) {
                requests.remove(mgc);
                changed_requests = true;
                break;
            }
        }
    }

    public MapleGuildCharacter getRequest(int cid) {
        final Iterator<MapleGuildCharacter> toRemove = requests.iterator();
        while (toRemove.hasNext()) {
            MapleGuildCharacter mgc = toRemove.next();
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public void setNoblessSkillPoint(int point) {
        this.noblessskillpoint = point;
        writeToDB(false);
    }

    public int getNoblessSkillPoint() {
        return this.noblessskillpoint;
    }

    public void updateGuildScore(long totDamageToOneMonster) {
        double guildScore = this.guildScore;
        double add = totDamageToOneMonster / 1000000000000.0;
        this.guildScore += add; // 100억당 1점
        if (guildScore != this.guildScore) {
            broadcast(CField.updateGuildScore((int) this.guildScore));
        }
    }

    public double getGuildScore() {
        return guildScore;
    }

    public void setGuildScore(double guildScore) {
        this.guildScore = guildScore;
    }

}
