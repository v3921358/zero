/**
 * @package : server.quest
 * @author : Yein
 * @fileName : QuestCompleteStatus.java
 * @date : 2019. 11. 17.
 */
package server.quest;

import provider.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QuestCompleteStatus {

    public static List<Integer> completeQuests = new ArrayList<>();

    public static int getInt(String a) {
        try {
            return Integer.parseInt(a);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void run() {
        MapleDataProvider npc = MapleDataProviderFactory.getDataProvider(new File("wz" + "/Npc.wz"));
        final MapleDataDirectoryEntry root = npc.getRoot();

        for (MapleDataFileEntry topDir : root.getFiles()) {
            if (topDir.getName().length() <= 20) {
                for (MapleData data : npc.getData(topDir.getName())) {
                    if (data.getName().contains("condition")) {
                        for (MapleData questData : data) {
                            int id = getInt(questData.getName());
                            if (id != 0 && !completeQuests.contains(id)) {
//	               				completeQuests.add(id);
                            }
                        }
                    }
                }
            }
        }

        MapleData questInfo = MapleDataProviderFactory.getDataProvider(new File("wz" + "/Quest.wz")).getData("QuestInfo.img");

        for (MapleData data : questInfo) {
            int autoStart = MapleDataTool.getInt("autoStart", data, 0);
            int selfStart = MapleDataTool.getInt("selfStart", data, 0);
            int dailyAlarm = MapleDataTool.getInt("dailyAlarm", data, 0);
            int blocked = MapleDataTool.getInt("blocked", data, 0);

            int id = getInt(data.getName());

            if (blocked > 0 || id == 0) {
                continue;
            }

            if (autoStart > 0 || selfStart > 0 || dailyAlarm > 0) {
                if (!completeQuests.contains(id)) {
//          			completeQuests.add(id);
                }
            }
        }
    }
}
