/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.control;

import handling.channel.ChannelServer;
import handling.world.World;
import java.io.FileReader;
import java.util.Date;
import java.util.Properties;
import javax.swing.JOptionPane;
import server.ServerProperties;
import tools.packet.CField;

/**
 *
 * @author 멜론K
 */
public class MapleRateEventControl implements Runnable {

    private static int expmin = 0, mesomin = 0, dropmin = 0;

    public MapleRateEventControl() {
        System.out.println("[Loading Completed] Maple Rate Event Control Loaded.");
    }

    @Override
    public void run() {
        if (expmin > 0) {
            expmin--;
            if (expmin == 0) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.setExpRate(Integer.parseInt(ServerProperties.getProperty("net.sf.odinms.world.exp")));
                }
                World.Broadcast.broadcastMessage(CField.getGameMessage(7, "[경험치 이벤트] 경험치 이벤트가 종료되었습니다."));
            }
        }

        if (mesomin > 0) {
            mesomin--;
            if (mesomin == 0) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.setMesoRate(Integer.parseInt(ServerProperties.getProperty("net.sf.odinms.world.meso")));
                }
                World.Broadcast.broadcastMessage(CField.getGameMessage(7, "[메소 이벤트] 메소 이벤트가 종료되었습니다."));
            }
        }

        if (dropmin > 0) {
            dropmin--;
            if (dropmin == 0) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.setDropRate(Integer.parseInt(ServerProperties.getProperty("net.sf.odinms.world.drop")));
                }
                World.Broadcast.broadcastMessage(CField.getGameMessage(7, "[드롭 이벤트] 드롭 이벤트가 종료되었습니다."));
            }
        }
    }

    public static int getExpMin() {
        return expmin;
    }

    public static int getMesoMin() {
        return mesomin;
    }

    public static int getDropMin() {
        return dropmin;
    }

    public static void setExpMin(int min) {
        expmin = min;
    }

    public static void setMesoMin(int min) {
        mesomin = min;
    }

    public static void setDropMin(int min) {
        dropmin = min;
    }
}
