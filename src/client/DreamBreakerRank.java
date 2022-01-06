package client;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author SLFCG
 */
public class DreamBreakerRank {

    public static Map<String, Integer> Rank = new LinkedHashMap<>();

    public static void LoadRank() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM dreambreaker");
            rs = ps.executeQuery();
            rs.last();
            rs.beforeFirst();
            while (rs.next()) {
                final String Name = rs.getString("name");
                final int Point = rs.getInt("floor") * 1000 + (180 - rs.getInt("time"));
                Rank.put(Name, Point);
            }
            Rank = sortByValue(Rank);
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void SaveRank() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            for (Map.Entry<String, Integer> info : Rank.entrySet()) {
                ps = con.prepareStatement("SELECT * FROM dreambreaker WHERE name = ?");
                int floor = info.getValue() / 1000;
                int time = (180 - info.getValue() % 1000);
                ps.setString(1, info.getKey());
                rs = ps.executeQuery();
                if (rs.next()) {
                    ps = con.prepareStatement("UPDATE dreambreaker SET floor = ?, time = ? WHERE name = ?");
                    ps.setInt(1, floor);
                    ps.setInt(2, time);
                    ps.setString(3, info.getKey());
                    ps.executeUpdate();
                    ps.close();
                } else {
                    SaveNewRecord(info.getKey(), floor, time);
                }
                ps.close();
                rs.close();
            }
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void WipeRecord() {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM dreambreaker");
            rs = ps.executeQuery();
            rs.last();
            rs.beforeFirst();
            rs = ps.executeQuery();
            while (rs.next()) {
                final int cid = getCid(rs.getString("name"));
                ps1 = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ? AND quest = ?");
                ps1.setInt(1, cid);
                ps1.setInt(2, 20190131);

                rs2 = ps1.executeQuery();

                ps2 = con.prepareStatement("INSERT INTO keyvalue (`id`, `key`, `value`) VALUES (?, ?, ?)");
                String[] temp = rs2.getString("custumData").split(";");
                String temp2 = temp[0] + "lastweek=" + getRank(rs.getString("name")) + temp[2];

                ps2.setInt(1, cid);
                ps2.setString(2, "db_lastweek");
                ps2.setString(3, temp2);
                ps2.execute();

                ps1.close();
                ps2.close();
                rs2.close();
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("TRUNCATE dreambreaker");
            ps.executeQuery();
            ps.close();

            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (ps1 != null) {
                    ps1.close();
                }
                if (ps2 != null) {
                    ps2.close();
                }
                if (rs2 != null) {
                    rs2.close();
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
        Rank.clear();
    }

    public static int getCid(String name) {
        int ret = 0;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret = rs.getInt("id");
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
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
        return ret;
    }

    public static void SaveNewRecord(String name, int floor, int time) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO dreambreaker (name, floor, time) VALUES (?, ?, ?)");
            ps.setString(1, name);
            ps.setInt(2, floor);
            ps.setInt(3, time);
            ps.executeUpdate();
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

    public static void EditRecord(String name, long floor, long time) {
        Rank.put(name, (int) floor * 1000 + (180 - (int) time));
        Rank = sortByValue(Rank);
        SaveRank();
    }

    public static int getRank(String name) {
        int index = 1;
        if (!Rank.containsKey(name)) {
            return 0;
        }
        for (Map.Entry<String, Integer> info : Rank.entrySet()) {
            if (info.getKey().equals(name)) {
                break;
            }
            index++;
        }
        return index;
    }

    public static Map<String, Integer> sortByValue(final Map<String, Integer> wordCounts) {
        return wordCounts.entrySet()
                .stream()
                .sorted((Map.Entry.<String, Integer>comparingByValue().reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
