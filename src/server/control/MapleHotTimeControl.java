/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.control;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import java.io.FileReader;
import java.util.Date;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
 *
 * @author 멜론K
 */
public class MapleHotTimeControl implements Runnable {

    private static final Properties props = new Properties();
    private static String itemid, itemqty;
    private static int hour, minute, r;
    FileReader fr = null;
    String test;

    public MapleHotTimeControl() {
        try {
            fr = new FileReader("setting/hottime.properties");
            props.load(fr);
            fr.close();
            hour = Integer.parseInt(props.getProperty("hottime_hour"));
            minute = Integer.parseInt(props.getProperty("hottime_minute"));
            itemid = props.getProperty("itemid");
            itemqty = props.getProperty("itemqty");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        System.out.println("[Loading Completed] Maple HotTime Control Start");
    }

    @Override
    public void run() {
        if (new Date().getHours() == hour && new Date().getMinutes() == minute) {
            String players = "";
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters1()) {
                    for (int j = 0; j < itemid.split(",").length; j++) {
                        chr.gainItem(Integer.parseInt(itemid.split(",")[j]), Integer.parseInt(itemqty.split(",")[j]));
                    }
                    chr.dropMessage(1, "핫타임이 지급되었습니다. 인벤토리를 확인해 주세요!");
                    if (chr != null) {
                        players += ", ";
                    }
                    players += chr.getName();
                }
            }
            JOptionPane.showMessageDialog(null, "핫타임이 지급되었습니다. 아래는 핫타임을 지급받은 유저의 닉네임입니다.\n\n" + players);
        }
    }

    public static int getHotTimeHour() {
        return hour;
    }

    public static int getHotTimeMinute() {
        return minute;
    }

    public static String getHotTimeItemIds() {
        return itemid;
    }

    public static String getHotTimeItemQtys() {
        return itemqty;
    }

    public static void reloadProperty() {
        try {
            MapleHotTimeControl control = new MapleHotTimeControl();
            control.fr = new FileReader("hottime.properties");
            props.load(control.fr);
            control.fr.close();
            hour = Integer.parseInt(props.getProperty("hottime_hour"));
            minute = Integer.parseInt(props.getProperty("hottime_minute"));
            itemid = props.getProperty("itemid");
            itemqty = props.getProperty("itemqty");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
