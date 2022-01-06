package server;

import database.DatabaseConnection;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author Emilyx3
 */
public class ServerProperties {

    private static final Properties props = new Properties();

    private ServerProperties() {
    }

    static {
        String toLoad = "channel.properties";
        loadProperties(toLoad);
        toLoad = "ports.properties";
        loadProperties(toLoad);
        toLoad = "database.properties";
        loadProperties(toLoad);
        /*        try {
         Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT * FROM auth_server_channel_ip");
         ResultSet rs = ps.executeQuery();
         while (rs.next()) {
         //if (rs.getString("name").equalsIgnoreCase("gms")) {
         //    GameConstants.GMS = Boolean.parseBoolean(rs.getString("value"));
         //} else {
         props.put(rs.getString("name") + rs.getInt("channelid"), rs.getString("value"));
         //}
         }
         rs.close();
         ps.close();
         con.close();
         } catch (SQLException ex) {
         //            ex.printStackTrace();
         System.out.println("SQL 연동을 해주세요.");
         System.exit(0); //Big ass error.
         }*/
        toLoad = "world.properties";
        loadProperties(toLoad);
        /*
        toLoad = "discord.properties";
        loadProperties(toLoad);
         */
    }

    public static void loadProperties(String s) {
        FileReader fr;
        try {
            fr = new FileReader(s);
            props.load(fr);
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String s) {
        return props.getProperty(s);
    }

    public static void setProperty(String prop, String newInf) {
        props.setProperty(prop, newInf);
    }

    public static String getProperty(String s, String def) {
        return props.getProperty(s, def);
    }
}
