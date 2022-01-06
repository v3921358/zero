/**
 * @package : constants
 * @author : Yein
 * @fileName : addMesoDropData.java
 * @date : 2019. 7. 29.
 */
package constants;

import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;
import tools.StringUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AddMesoDropData {

    public static void main(String[] args) {
        final List<String> strings = new ArrayList<>();
        final String WZpath = System.getProperty("net.sf.odinms.wzpath");
        final MapleDataProvider stringData = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/String.wz"));
        final MapleData mobStringData = stringData.getData("Mob.img");

        final List<Pair<Integer, Integer>> mobMesoDatas = new ArrayList<>();

        for (MapleData ms : mobStringData) {
            try {
                strings.add(ms.getName());
            } catch (Exception e) {
                continue;
            }
        }

        final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/Mob.wz"));
        for (String string : strings) {
            try {
                if (mobStringData.getChildByPath(string) != null) {
                    MapleData mobData = data.getData(StringUtil.getLeftPaddedStr(string + ".img", '0', 11));
                    if (mobData != null) {
                        MapleData leveldata = mobData.getChildByPath("info/level");
                        if (leveldata != null) {
                            int level = MapleDataTool.getInt("info/level", mobData, 0);
                            //  System.out.println(string + " 몬스터의 레벨 : " + level);

                            mobMesoDatas.add(new Pair<>(Integer.parseInt(string), level));
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        DatabaseConnection.init();

        int i = 1000;

        try {
            Connection con = DatabaseConnection.getConnection();
            for (Pair<Integer, Integer> db : mobMesoDatas) {
                PreparedStatement ps = con.prepareStatement("INSERT INTO `drop_data` VALUES (?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
                ps.setInt(1, i);
                ps.setInt(2, db.left);
                ps.setInt(3, 0);
                ps.setInt(4, db.right * 25);
                ps.setInt(5, db.right * 100);
                ps.setInt(6, 0);
                ps.setInt(7, 1000000);
                ps.setInt(8, 0);
                ps.executeUpdate();
                ps.close();
                i++;
            }
            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

    }

}
