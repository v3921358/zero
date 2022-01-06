package handling.channel.handler;

import client.MapleClient;
import client.MapleUnion;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnionHandler {

    public static List<Integer> groupIndex = new ArrayList<>();
    public static List<Point> boardPos = new ArrayList<>();
    public static List<Integer> openLevels = new ArrayList<>();
    public static Map<Integer, Integer> cardSkills = new HashMap<>();
    public static Map<Integer, Map<Integer, List<Point>>> characterSizes = new HashMap<>();
    public static List<Integer> skills = new ArrayList<>();

    public static void loadUnion() {
        final String WZpath = System.getProperty("wz");
        final MapleDataProvider prov = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/Etc.wz"));
        MapleData nameData = prov.getData("mapleUnion.img");
        try {
            for (MapleData dat : nameData) {
                switch (dat.getName()) {
                    case "BoardInfo":
                        for (MapleData d : dat) {
                            groupIndex.add(MapleDataTool.getInt(d.getChildByPath("groupIndex")));
                            boardPos.add(new Point(MapleDataTool.getInt(d.getChildByPath("xPos")), MapleDataTool.getInt(d.getChildByPath("yPos"))));
                            openLevels.add(MapleDataTool.getInt(d.getChildByPath("openLevel")));
                        }
                        break;
                    case "Card":
                        for (MapleData d : dat) {
                            cardSkills.put(Integer.parseInt(d.getName()), MapleDataTool.getInt(d.getChildByPath("skillID")));
                        }
                        break;
                    case "CharacterSize":
                        for (MapleData d : dat) {
                            int num = Integer.parseInt(d.getName());
                            Map<Integer, List<Point>> array = new HashMap<>();
                            for (MapleData z : d) {
                                int idx = Integer.parseInt(z.getName());
                                List<Point> arr = new ArrayList<>();
                                for (MapleData zz : z) {
//                					System.out.println(num + " / " + idx);
                                    Point data = MapleDataTool.getPoint(zz);
                                    arr.add(data);
//                					System.out.println(num + " /" + idx + " / " + zz.getName() + " / " + data);
                                }
                                array.put(idx, arr);
                            }
                            characterSizes.put(num, array);
                        }
                        break;
                    case "SkillInfo":
                        for (MapleData d : dat) {
                            skills.add(MapleDataTool.getInt(d.getChildByPath("skillID")));
                        }
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openUnion(MapleClient c) {
        if (Integer.parseInt(c.getKeyValue("rank")) > c.getPlayer().getKeyValue(18771, "rank")) {
            c.getPlayer().setKeyValue(18771, "rank", c.getKeyValue("rank"));
        }
        c.getSession().writeAndFlush(CField.openUnionUI(c)); // 패킷 출력
    }

    public static void unionFreeset(LittleEndianAccessor slea, MapleClient c) {
        c.getSession().writeAndFlush(CWvsContext.unionFreeset(c, slea.readInt())); //
    }

    public static void setUnion(LittleEndianAccessor slea, MapleClient c) {
        try {

            slea.skip(4);
            int size1 = slea.readInt(); // 유니온 프리셋
            for (int i = 0; i < size1; i++) {
                slea.skip(4);
            }

            short size2 = slea.readShort();
            slea.skip(4);

            List<String> names = new ArrayList<>();

            for (int i = 0; i < size2; i++) { // 착용한 유니온 캐릭터만 출력됨.
                slea.skip(4);//int key = slea.readInt(); // 1
                int id = slea.readInt();
                int lv = slea.readInt();
                int job = slea.readInt();
                int unk1 = slea.readInt(); // 0
                int unk2 = slea.readInt(); // 0
                int pos = slea.readInt();
                int unk3 = slea.readInt(); // 0
                String name = slea.readMapleAsciiString();
                names.add(name);
                for (MapleUnion union : c.getPlayer().getUnions().getUnions()) {
                    if (union.getCharid() == id) {
//                        union.setKey(key);
                        union.setLevel(lv);
                        union.setJob(job);
                        union.setUnk1(unk1);
                        union.setUnk2(unk2);
                        union.setPosition(pos);
                        union.setUnk3(unk3);
                        union.setName(name);
                    }
                }
            }

            for (MapleUnion union : c.getPlayer().getUnions().getUnions()) {
                if (union.getPosition() != -1 && !names.contains(union.getName())) {
                    union.setPosition(-1);
                }
            }

            c.getSession().writeAndFlush(CWvsContext.setUnion(c));

            c.getPlayer().getStat().recalcLocalStats(c.getPlayer());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
