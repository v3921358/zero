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
package handling.channel;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class MapleGuildRanking {

    private static MapleGuildRanking instance = new MapleGuildRanking();
    private List<GuildRankingInfo> honorRank = new LinkedList<GuildRankingInfo>();
    private List<GuildRankingInfo> flagRaceRank = new LinkedList<GuildRankingInfo>();
    private List<GuildRankingInfo> culvertRank = new LinkedList<GuildRankingInfo>();

    public static MapleGuildRanking getInstance() {
        return instance;
    }

    public void load() {
        reload();
    }

    public List<GuildRankingInfo> getHonorRank() {
        return honorRank;
    }

    private void reload() {
        honorRank.clear();
        flagRaceRank.clear();
        culvertRank.clear();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM guilds ORDER BY `GP` DESC LIMIT 50");
            rs = ps.executeQuery();

            while (rs.next()) {

                final GuildRankingInfo rank = new GuildRankingInfo(
                        rs.getString("name"),
                        rs.getInt("GP"),
                        rs.getInt("guildid"));

                honorRank.add(rank);
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("SELECT * FROM guilds ORDER BY `score` DESC LIMIT 50");
            rs = ps.executeQuery();

            while (rs.next()) {

                final GuildRankingInfo rank = new GuildRankingInfo(
                        rs.getString("name"),
                        rs.getInt("score"),
                        rs.getInt("guildid"));

                culvertRank.add(rank);
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Error handling guildRanking");
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<GuildRankingInfo> getCulvertRank() {
        return culvertRank;
    }

    public void setCulvertRank(List<GuildRankingInfo> culvertRank) {
        this.culvertRank = culvertRank;
    }

    public List<GuildRankingInfo> getFlagRaceRank() {
        return flagRaceRank;
    }

    public void setFlagRaceRank(List<GuildRankingInfo> flagRaceRank) {
        this.flagRaceRank = flagRaceRank;
    }

    public static class GuildRankingInfo {

        private String name;
        private int score, id;

        public GuildRankingInfo(String name, int score, int id) {
            this.name = name;
            this.score = score;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }

        public int getId() {
            return id;
        }

        public void setId(int gid) {
            this.id = gid;
        }
    }
}
