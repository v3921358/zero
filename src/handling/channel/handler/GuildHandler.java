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
import client.Skill;
import client.SkillFactory;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.world.World;
import handling.world.World.Find;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.world.guild.MapleGuildResponse;
import server.MapleStatEffect;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.GuildPacket;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuildHandler {

    public static final void DenyGuildRequest(String from, MapleClient c) {
        MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
        if (cfrom != null) {
            cfrom.getClient().getSession().writeAndFlush(GuildPacket.denyGuildInvitation(c.getPlayer().getName()));
        }
    }

    private static boolean isGuildNameAcceptable(final String name) throws UnsupportedEncodingException {
        if (name.getBytes("EUC-KR").length < 2 || name.getBytes("EUC-KR").length > 12) {
            return false;
        }
        return true;
    }

    private static void respawnPlayer(final MapleCharacter mc) {
        if (mc.getMap() == null) {
            return;
        }
//        mc.getMap().broadcastMessage(CField.loadGuildName(mc));
        mc.getMap().broadcastMessage(CField.loadGuildIcon(mc));
    }

    public static final void GuildCancelRequest(final MapleClient c, final MapleCharacter chr) {
        if (c == null || chr == null) {
            return;
        }
        chr.setKeyValue(26015, "name", "");
        for (MapleGuild mg : World.Guild.getGuilds(1, 999, 1, 999, 1, 999)) {
            if (mg.getRequest(chr.getId()) != null) {
                mg.removeRequest(chr.getId());
                for (int i = 1; i <= 5; i++) {
                    mg.RankBroadCast(CWvsContext.GuildPacket.RequestDeny(chr), i);
                }
            }
        }
        c.getSession().writeAndFlush(CWvsContext.GuildPacket.RequestDeny(chr));
        request.put(chr.getId(), System.currentTimeMillis());
    }

    public static final void GuildJoinRequest(final int gid, final MapleCharacter chr) {
        if (chr == null || gid <= 0) {
            return;
        }
        MapleGuild g = World.Guild.getGuild(gid);
        MapleGuildCharacter mgc2 = new MapleGuildCharacter(chr);
        mgc2.setGuildId(gid);
        if (request.get(chr.getId()) == null) {
            if (g.addRequest(mgc2)) {
                g.broadcast(CWvsContext.GuildPacket.addRegisterRequest(mgc2));
            }
            chr.setKeyValue(26015, "name", g.getName());
        } else {
            if (System.currentTimeMillis() >= request.get(chr.getId()).longValue() + 60000) {
                request.remove(chr.getId());
                if (g.addRequest(mgc2)) {
                    g.broadcast(CWvsContext.GuildPacket.addRegisterRequest(mgc2));
                }
                chr.setKeyValue(26015, "name", "");
            } else {
                chr.getClient().getSession().writeAndFlush(CWvsContext.GuildPacket.DelayRequest());
            }
        }
    }

    public static final void GuildJoinDeny(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        byte action = slea.readByte();
        for (int i = 0; i < action; i++) {
            int cid = slea.readInt();
            if (chr.getGuildId() > 0 && chr.getGuildRank() <= 2) {
                MapleGuild g = chr.getGuild();
                if (chr.getGuildRank() <= 2) {
                    g.removeRequest(cid);
                    int ch = Find.findChannel(cid);
                    if (ch < 0) {
                        return;
                    }
                    final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(cid);
                    c.getClient().getSession().writeAndFlush(CWvsContext.GuildPacket.RequestDeny(c));
                    chr.setKeyValue(26015, "name", "");
                    request.put(cid, System.currentTimeMillis());
                } else {
                    chr.dropMessage(6, "길드 권한이 부족합니다.");
                }
            }
        }
    }

    public static final void GuildRegisterAccept(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        byte action = slea.readByte();
        for (int i = 0; i < action; i++) {
            int cid = slea.readInt();
            if (chr.getGuildId() > 0 && chr.getGuildRank() <= 2) {
                MapleGuild g = chr.getGuild();
                if (chr.getGuildRank() <= 2 && g != null) {
                    MapleCharacter c = null;
                    for (ChannelServer cs : ChannelServer.getAllInstances()) {
                        c = cs.getPlayerStorage().getCharacterById(cid);
                        if (c != null) {
                            MapleGuildCharacter temp = g.getRequest(cid);
                            g.addGuildMember(temp);
                            c.getClient().getSession().writeAndFlush(GuildPacket.showGuildInfo(chr));
                            g.removeRequest(cid);
                            c.setGuildId(g.getId());
                            c.setGuildRank((byte) 5);
                            c.saveGuildStatus();
                            c.setKeyValue(26015, "name", "");
                            respawnPlayer(c);
                            break;
                        }
                    }
                    if (c == null) {
                        MapleGuildCharacter temp = OfflineMapleGuildCharacter(cid, chr.getGuildId());
                        if (temp != null) {
                            temp.setOnline(false);
                            g.addGuildMember(temp);
                            MapleGuild.setOfflineGuildStatus(g.getId(), (byte) 5, 0, (byte) 5, cid);
                            g.removeRequest(cid);
                        } else {
                            chr.dropMessage(5, "존재하지 않는 캐릭터입니다.");
                        }
                    }

                } else {
                    chr.dropMessage(6, "길드 권한이 부족합니다.");
                }
            }
        }
    }

    public static final MapleGuildCharacter OfflineMapleGuildCharacter(int cid, int gid) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();

            ps = con.prepareStatement("SELECT * FROM characters where id = ?");
            ps.setInt(1, cid);
            rs = ps.executeQuery();

            if (rs.next()) {
                byte gRank = rs.getByte("guildrank"), aRank = rs.getByte("alliancerank");
                return new MapleGuildCharacter(cid, rs.getShort("level"), rs.getString("name"), (byte) -1, rs.getInt("job"), gRank, rs.getInt("guildContribution"), aRank, gid, false);
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException se) {
            System.err.println("Error Laod Offline MapleGuildCharacter");
            se.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(GuildHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(GuildHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static final void GuildRequest(final int guildid, final MapleCharacter player) {
        player.dropMessage(1, "현재 이 기능은 사용하실 수 없습니다.");
        /*final MapleGuild guild = World.Guild.getGuild(guildid);
         player.updateInfoQuest(26015, "name=" + guild.getName());
         guild.addRequest(new MapleGuildCharacter(player));
         //    requests.put(player.getName(), new Pair<>(guildid, System.currentTimeMillis() + (20 * 60000)));
         for (MapleGuildCharacter gc : guild.getMembers()) {
         if (gc.getGuildRank() <= 2 && gc.isOnline()) {
         ChannelServer.getInstance(gc.getChannel()).getPlayerStorage().getCharacterById(gc.getId()).getClient().getSession().writeAndFlush(GuildPacket.requestGuild(guild.getInvitedId(), player));
         }
         }*/
    }

    public static void cancelGuildRequest(MapleClient c, MapleCharacter player) {
        player.dropMessage(1, "현재 이 기능은 사용하실 수 없습니다.");
        /*
         c.getSession().writeAndFlush(GuildPacket.cancelGuildRequest(player.getId()));
         player.updateInfoQuest(26015, "");
         */
    }

    public static void SendGuild(LittleEndianAccessor slea, MapleClient c) {
        c.getPlayer().dropMessage(1, "현재 이 기능은 사용하실 수 없습니다.");
        /*byte size = slea.readByte();
         for (int i = 0; i < size; i++) {
         int cid = slea.readInt();
         MapleCharacter chra = c.getChannelServer().getPlayerStorage().getCharacterById(cid);
         c.getSession().writeAndFlush(GuildPacket.GuildRequest(c.getPlayer().getGuildId(), cid));
         c.getSession().writeAndFlush(GuildPacket.RequestGuildadd(chra));
         c.getSession().writeAndFlush(GuildPacket.RequestGuildadd2(chra, cid));
         }*/
    }

    private static class Invited {

        public String name;
        public int gid;
        public long expiration;

        public Invited(String n, int id) {
            name = n.toLowerCase();
            gid = id;
            expiration = System.currentTimeMillis() + 60 * 60 * 1000; // 1 hr expiration
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Invited)) {
                return false;
            }
            Invited oth = (Invited) other;
            return (gid == oth.gid && name.equals(oth));
        }
    }

    private static java.util.List<Invited> invited = new java.util.LinkedList<Invited>();
    //private static java.util.List<Pair<Integer, Long>> request = new java.util.LinkedList<Pair<Integer, Long>>();
    private static Map<Integer, Long> request = new LinkedHashMap<>();
    private static long nextPruneTime = System.currentTimeMillis() + 5 * 60 * 1000;

    public static final void Guild(final LittleEndianAccessor slea, final MapleClient c) {
        if (System.currentTimeMillis() >= nextPruneTime) {
            Iterator<Invited> itr = invited.iterator();
            Invited inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (System.currentTimeMillis() >= inv.expiration) {
                    itr.remove();
                }
            }
            nextPruneTime += 5 * 60 * 1000;
        }
        try {
            final int action = slea.readByte();
            switch (action) {
                case 0x04: {
                    int cid = slea.readInt();

                    int ch = Find.findChannel(cid);
                    if (ch < 0) {

                        MapleGuild g = World.Guild.getGuild(c.getPlayer().getLastCharGuildId());
                        if (g != null) {
                            c.getSession().writeAndFlush(GuildPacket.getGuildInfo(g));
                        } else {
                            c.getPlayer().dropMessage(1, "가입된 길드가 없습니다.");
                        }
                        return;
                    }
                    final MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(cid);

                    if (player != null) {
                        MapleGuild g = World.Guild.getGuild(player.getGuildId());
                        if (g != null) {
                            c.getSession().writeAndFlush(GuildPacket.getGuildInfo(g));
                        } else {
                            c.getPlayer().dropMessage(1, "가입된 길드가 없습니다.");
                        }
                    }
                    break;
                }
                case 0x01: //길드 생성 343+
                    if (c.getPlayer().getGuildId() > 0) {
                        c.getPlayer().dropMessage(1, "이미 길드에 가입되어 있어 길드를 만들 수 없습니다.");
                        return;
                    } else if (c.getPlayer().getMeso() < 5000000) {
                        c.getPlayer().dropMessage(1, "길드 제작에 필요한 메소 [500만 메소] 가 충분하지 않습니다.");
                        return;
                    }
                    final String guildName = slea.readMapleAsciiString();
                    if (!isGuildNameAcceptable(guildName)) {
                        c.getPlayer().dropMessage(1, "해당 길드 이름은 만들 수 없습니다.");
                        return;
                    }
                    int guildId = World.Guild.createGuild(c.getPlayer().getId(), guildName);
                    if (guildId == 0) {
                        c.getPlayer().dropMessage(1, "잠시후에 다시 시도 해주세요.");
                        return;
                    }
                    c.getPlayer().gainMeso(-5000000, true, true); //본메 길드창설 비용 500만메소
                    c.getPlayer().setGuildId(guildId);
                    c.getPlayer().setGuildRank((byte) 1);
                    c.getPlayer().saveGuildStatus();
                    World.Guild.setGuildMemberOnline(c.getPlayer().getMGC(), true, c.getChannel());
                    c.getSession().writeAndFlush(GuildPacket.showGuildInfo(c.getPlayer()));
                    c.getSession().writeAndFlush(GuildPacket.newGuildInfo(c.getPlayer()));
                    World.Guild.gainContribution(c.getPlayer().getGuildId(), 500, c.getPlayer().getId());
                    respawnPlayer(c.getPlayer());
                    break;
                case 0x24: // 초대  343 +
                    if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) { // 1 == guild master, 2 == jr
                        return;
                    }
                    String name = slea.readMapleAsciiString();
                    MapleGuildResponse mgr = MapleGuild.sendInvite(c, name);

                    if (mgr != null) {
                        c.getSession().writeAndFlush(mgr.getPacket());
                    } else {
                        Invited inv = new Invited(name, c.getPlayer().getGuildId());
                        if (!invited.contains(inv)) {
                            invited.add(inv);
                        }
                    }
                    break;
                case 0x02:
                    MapleGuild g = World.Guild.getGuild(slea.readInt());
                    if (g != null) {
                        c.getSession().writeAndFlush(GuildPacket.getGuildInfo(g));
                    } else {
                        c.getPlayer().dropMessage(1, "잠시후에 다시 시도 해주세요.");
                        return;
                    }
                    /*guildId = slea.readInt();
                     name = c.getPlayer().getName().toLowerCase();
                     Iterator<Invited> itr = invited.iterator();
                     while (itr.hasNext()) {
                     Invited inv = (Invited) itr.next();
                     if ((guildId == inv.gid) && (name.equals(inv.name))) {
                     c.getPlayer().setGuildId(guildId);
                     c.getPlayer().setGuildRank((byte) 5);
                     itr.remove();
                     int guildmax;
                     guildmax = World.Guild.addGuildMember(c.getPlayer().getMGC());
                     c.getPlayer().setGuildInvited(false);
                     if (guildmax == 0) {
                     c.getPlayer().dropMessage(1, "가입하려는 길드는 이미 정원이 꽉 찼습니다.");
                     c.getPlayer().setGuildId(0);
                     return;
                     }
                     c.getSession().writeAndFlush(GuildPacket.showGuildInfo(c.getPlayer()));
                     c.getPlayer().saveGuildStatus();
                     respawnPlayer(c.getPlayer());
                     }
                     }*/
                    break;
                case 0x03: // 길드 상세정보
                    int gid = slea.readInt();

                    break;
                case 0x06: //탈퇴
                    int cid = slea.readInt();
                    name = slea.readMapleAsciiString();

                    if (cid != c.getPlayer().getId() || !name.equals(c.getPlayer().getName()) || c.getPlayer().getGuildId() <= 0) {
                        return;
                    }
                    World.Guild.leaveGuild(c.getPlayer().getMGC());
                    break;
                case 0x07: //추방
                    cid = slea.readInt();
                    name = slea.readMapleAsciiString();

                    if (c.getPlayer().getGuildRank() > 2 || c.getPlayer().getGuildId() <= 0) {
                        return;
                    }
                    World.Guild.expelMember(c.getPlayer().getMGC(), name, cid);
                    respawnPlayer(c.getPlayer());
                    break;
                case 0x0F: { // Rank change
                    byte newRank = slea.readByte();
                    int size = slea.readInt();

                    if ((newRank <= 1 || newRank > 5) || c.getPlayer().getGuildRank() > 2 || (newRank <= 2 && c.getPlayer().getGuildRank() != 1) || c.getPlayer().getGuildId() <= 0) {
                        c.getSession().writeAndFlush(GuildPacket.genericGuildMessage(121));
                        return;
                    }

                    for (int i = 0; i < size; i++) {
                        cid = slea.readInt();

                        World.Guild.changeRank(c.getPlayer().getGuildId(), cid, newRank);
                    }

                    c.getSession().writeAndFlush(GuildPacket.genericGuildMessage(118));
                    break;
                }
                case 0x0C: {
                    /*
                    01
                    07 00 B8 B6 BD BA C5 CD 31
                    7F 05 00 00
                    */
                    int index = slea.readByte();
                    String rankName = slea.readMapleAsciiString();
                    int rankRole = slea.readInt();

                    String ranks[] = new String[10];
                    int roles[] = new int[10];
                    for (int i = 1; i <= 10; i++) {
                        if (i > 5) {
                            ranks[i - 1] = "";
                            roles[i - 1] = 0;
                        } else {
                            if (i == index) {
                                ranks[i - 1] = rankName;
                                roles[i - 1] = c.getPlayer().getGuild().getRankRole(i) | rankRole;
                            } else {
                                ranks[i - 1] = c.getPlayer().getGuild().getRankTitle(i);
                                roles[i - 1] = c.getPlayer().getGuild().getRankRole(i);
                            }
                        }
                    }

                    World.Guild.changeRankTitleRole(c.getPlayer(), ranks, roles);
                    break;
                }
                case 0x0D: { // 새로운 직위 추가
                    /*
                    06
                    06 00 B1 E6 B5 E5 BF F8
                    00 00 00 00
                    */
                    int index = slea.readByte();
                    String rankName = slea.readMapleAsciiString();
                    int rankRole = slea.readInt();

                    String ranks[] = new String[10];
                    int roles[] = new int[10];
                    for (int i = 1; i <= 10; i++) {
                        if (i > 5) {
                            ranks[i - 1] = "";
                            roles[i - 1] = 0;
                        } else {
                            if (i == index) {
                                ranks[i - 1] = rankName;
                                roles[i - 1] = c.getPlayer().getGuild().getRankRole(i) | rankRole;
                            } else {
                                ranks[i - 1] = c.getPlayer().getGuild().getRankTitle(i);
                                roles[i - 1] = c.getPlayer().getGuild().getRankRole(i);
                            }
                        }
                    }

                    World.Guild.changeRankTitleRole(c.getPlayer(), ranks, roles);
                    break;
                }
//                case 0x0C: {
//                    if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() != 1) {
//                        return;
//                    }
//                    String ranks[] = new String[5];
//                    for (int i = 0; i < 5; i++) {
//                        ranks[i] = slea.readMapleAsciiString();
//                    }
//                    World.Guild.changeRankTitle(c.getPlayer(), ranks);
//                    break;
//                }
//                case 0x0D: {
//                    if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() != 1) {
//                        return;
//                    }
//                    int roles[] = new int[5];
//                    for (int i = 0; i < 5; i++) {
//                        roles[i] = slea.readInt();
//                    }
//                    World.Guild.changeRankRole(c.getPlayer(), roles);
//                    break;
//                }
                case 0x0E:
                    if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() != 1) {
                        return;
                    }
                    String ranks[] = new String[5];
                    int roles[] = new int[5];
                    for (int i = 0; i < 5; i++) {
                        roles[i] = slea.readInt();
                        ranks[i] = slea.readMapleAsciiString();
                    }
                    World.Guild.changeRankTitleRole(c.getPlayer(), ranks, roles);
                    break;
                case 0x10: // 길드 마크 변경
                    if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() != 1) {
                        c.getPlayer().dropMessage(1, "길드가 없거나 마스터가 아닙니다.");
                        return;
                    }

                    if (c.getPlayer().getMeso() < 1500000) {
                        c.getPlayer().dropMessage(1, "길드마크를 만들기 위해 필요한 돈이 부족합니다");
                        return;
                    }
                    final byte isCustomImage = slea.readByte();
                    if (isCustomImage == 0) {
                        final short bg = slea.readShort();
                        final byte bgcolor = slea.readByte();
                        final short logo = slea.readShort();
                        final byte logocolor = slea.readByte();

                        World.Guild.setGuildEmblem(c.getPlayer(), bg, bgcolor, logo, logocolor);
                    } else {
                        int size = slea.readInt();
                        byte[] imgdata = new byte[size];
                        for (int i = 0; i < size; i++) {
                            imgdata[i] = slea.readByte();
                        }

                        World.Guild.setGuildCustomEmblem(c.getPlayer(), imgdata);
                    }

                    c.getPlayer().gainMeso(-1500000, true, true);
                    respawnPlayer(c.getPlayer());
                    break;
                case 0x11: // 길드 공지사항 변경
                    final String notice = slea.readMapleAsciiString();
                    if (notice.length() > 100 || c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) {
                        return;
                    }
                    World.Guild.setGuildNotice(c.getPlayer(), notice);
                    break;
                case 0x18: // 가입 설정
                    //01 00 00 00 00 00 00 00 00 00 00 00 00
                    break;
                case 0x12: // 길드 가입 홍보
                    break;
                case 0x2D: //길드스킬 레벨업 343ok
                    Skill skilli = SkillFactory.getSkill(slea.readInt());
                    if (c.getPlayer().getGuildId() <= 0 || skilli == null) {
                        return;
                    }
                    int eff = World.Guild.getSkillLevel(c.getPlayer().getGuildId(), skilli.getId());
                    if (eff <= 0) {
                        return;
                    }
                    final MapleStatEffect skillii = skilli.getEffect(eff);
                    if (skillii.getReqGuildLevel() < 0 || c.getPlayer().getMeso() < skillii.getExtendPrice()) {
                        return;
                    }
                    if (World.Guild.activateSkill(c.getPlayer().getGuildId(), skillii.getSourceId(), c.getPlayer().getName())) {
                        c.getPlayer().gainMeso(-skillii.getExtendPrice(), true);
                    }
                    break;
                case 0x2E: //guild skill purchase
                    //길드스킬 다시한번 확인해야함. 센드값 확인 x
                    skilli = SkillFactory.getSkill(slea.readInt());
                    if (c.getPlayer().getGuildId() <= 0 || skilli == null || skilli.getId() < 91000000) {
                        return;
                    }
                    eff = World.Guild.getSkillLevel(c.getPlayer().getGuildId(), skilli.getId()) + 1;
                    if (eff > skilli.getMaxLevel()) {
                        return;
                    }
                    final MapleStatEffect skillid = skilli.getEffect(eff);
                    if (skillid.getReqGuildLevel() < 0 || c.getPlayer().getMeso() < skillid.getPrice()) {
                        return;
                    }
                    if (World.Guild.purchaseSkill(c.getPlayer().getGuildId(), skillid.getSourceId(), c.getPlayer().getName(), c.getPlayer().getId())) {
                        //c.getPlayer().gainMeso(-skillid.getPrice(), true);
                    }
                    break;

                case 0x36: //노블레스 스킬 사용
                    if (c.getPlayer().getGuildId() <= 0) {
                        return;
                    }
                    int sid = slea.readInt();
                    eff = World.Guild.getSkillLevel(c.getPlayer().getGuildId(), sid);
                    SkillFactory.getSkill(sid).getEffect(eff).applyTo(c.getPlayer());
                    c.getSession().writeAndFlush(CField.skillCooldown(sid, 3600000));
                    c.getPlayer().addCooldown(sid, System.currentTimeMillis(), 3600000);
                    c.getSession().writeAndFlush(GuildPacket.useNoblessSkill(sid));
                    break;
                case 0x1F: //길드장 위임
                    cid = slea.readInt();
                    if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 1) {
                        return;
                    }
                    World.Guild.setGuildLeader(c.getPlayer().getGuildId(), cid);
                    break;
                /*case 0x2E: //노블레스 스킬 오픈
                 int nobless = 0x88;
                 c.getSession().writeAndFlush(GuildPacket.genericGuildMessage(nobless));
                 break;*/
                case 0x29: //길드 검색
                    /*
                     검색 기준선택
                     3 = 전체 / 1 = 길드명 / 2 = 길드 마스터명 
                     4 = 길드 가입 칸
                     */
                    byte mode = slea.readByte();
                    slea.readByte();
                    String text = slea.readMapleAsciiString();
                    boolean Check = slea.readByte() == 1; // 정확히 일치
                    switch (mode) {
                        case 1:
                            c.getSession().writeAndFlush(GuildPacket.showSearchGuildInfo(mode, World.Guild.getGuildsByName(text, Check), text));
                            break;
                        case 2:
                            c.getSession().writeAndFlush(GuildPacket.showSearchGuildInfo(mode, World.Guild.getGuildsByLeader(text, Check), text));
                            break;
                        case 3:
                            c.getSession().writeAndFlush(GuildPacket.showSearchGuildInfo(mode, World.Guild.getGuildsByAll(text, Check), text));
                            break;
                        case 4:
                            c.getSession().writeAndFlush(GuildPacket.showSearchGuildInfo(mode, World.Guild.getGuilds(), text));
                            break;
                    }
                    break;
                case 0x25: //출석체크
                    break;
                case 0x32: //길드 홍보 로딩
                    break;
                case 0x22: //길드 상세 보기
                    MapleGuild g1 = World.Guild.getGuild(slea.readInt());
                    if (g1 != null) {
                        c.getSession().writeAndFlush(GuildPacket.getGuildInfo(g1));
                    } else {
                        c.getPlayer().dropMessage(1, "잠시후에 다시 시도 해주세요.");
                        return;
                    }
                    break;
                case 0x28: //길드 정보 불러오기 ??
                    c.getSession().writeAndFlush(GuildPacket.getGuildRanksInfo((short) 1, (short) 1, (short) 1));
                    break;
                case 0x39: //길드 도움말
                    break;
                default: {
                    System.out.println("New Action: " + action + " Remaining: " + slea.toString());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void guildRankingRequest(MapleClient c) {
//        c.getSession().writeAndFlush(GuildPacket.showGuildRanks((byte) 1, MapleGuildRanking.getInstance()));
    }
}
