package handling.login.handler;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AutoRegister {

    private static final int ACCOUNTS_PER_IP = 999; //change the value to the amount of accounts you want allowed for each ip
    public static final boolean autoRegister = true; //enable = true or disable = false

    public static boolean getAccountExists(String login) {
        boolean accountExists = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?");
            ps.setString(1, login);
            rs = ps.executeQuery();
            if (rs.first()) {
                accountExists = true;
            }
            ps.close();
            rs.close();
        } catch (Exception ex) {
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
        return accountExists;
    }

    public static boolean createAccount(String login, String pwd, String eip) {
        String sockAddr = eip;
        Connection con;
        boolean success = false;
        //connect to database or halt
        try {
            con = DatabaseConnection.getConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
            return success;
        }

        try {
            ResultSet rs;
            try (PreparedStatement ipc = con.prepareStatement("SELECT SessionIP FROM accounts WHERE SessionIP = ?")) {
                ipc.setString(1, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                rs = ipc.executeQuery();

                if (rs.first() == false || rs.last() == true && rs.getRow() < ACCOUNTS_PER_IP) {
                    try {
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, SessionIP) VALUES (?, ?, ?, ?, ?, ?)")) {
                            ps.setString(1, login);
                            ps.setString(2, pwd);
                            ps.setString(3, "no@email.provided");
                            ps.setString(4, "2008-04-07");
                            ps.setString(5, "00-00-00-00-00-00");
                            ///  ps.setInt(6, 123456);
                            ps.setString(6, sockAddr.substring(1, sockAddr.lastIndexOf(':')));
                            ps.executeUpdate();
                            ps.close();
                        }
                        success = true;
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return success;
                    }
                }
                rs.close();
                ipc.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    public static boolean getDiscordExists(String discord) {
        boolean discordExists = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT discord FROM accounts WHERE discord = ?");
            ps.setString(1, discord);
            rs = ps.executeQuery();
            if (rs.first()) {
                discordExists = true;
            }
            ps.close();
            rs.close();
            con.close();
        } catch (Exception ex) {
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
        return discordExists;
    }
}
