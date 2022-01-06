package constants;

import client.inventory.MapleInventoryIdentifier;
import database.DatabaseConnection;
import server.ServerProperties;
import server.ShutdownServer;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class test {

    public static void main(String[] args) {
        //System.out.println(ShutdownServer.class.getSimpleName());
        int buffstat = 0x4000;
        int pos = 2;

        for (int flag = 0; flag < 999; flag++)
        {
            if ((1 << (31 - (flag % 32))) == buffstat && pos == (byte) Math.floor(flag / 32))
                System.out.println(flag);
            if ((1 << (31 - (flag % 32))) == buffstat && pos == (byte) (4 - Math.floor(flag / 32)))
                System.out.println("mob " + flag);

        }

        System.out.println(GameConstants.getCurrentDate_NoTime2());

        System.out.println("뷁뷁뷁뷁뷁".getBytes().length);
    }
}
