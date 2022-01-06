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
package server.life;

import constants.GameConstants;
import provider.*;
import server.Randomizer;
import tools.Pair;
import tools.StringUtil;
import client.*;

import java.io.File;
import java.util.*;

public class MapleLifeFactory {

    private static final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Mob.wz"));
    private static final MapleDataProvider npcData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Npc.wz"));
    private static final MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/String.wz"));
    private static final MapleDataProvider etcDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Etc.wz"));
    private static final MapleData mobStringData = stringDataWZ.getData("Mob.img");
    private static final MapleData npcStringData = stringDataWZ.getData("Npc.img");
    private static final MapleData npclocData = etcDataWZ.getData("NpcLocation.img");
    private static Map<Integer, String> npcNames = new HashMap<Integer, String>();
    private static Map<Integer, String> npcScripts = new HashMap<Integer, String>();
    private static Map<Integer, MapleMonsterStats> monsterStats = new HashMap<Integer, MapleMonsterStats>();
    private static Map<Integer, Integer> NPCLoc = new HashMap<Integer, Integer>();
    private static Map<Integer, List<Integer>> questCount = new HashMap<Integer, List<Integer>>();
    public MapleCharacter chr;

    public static AbstractLoadedMapleLife getLife(int id, String type) {
        if (type.equalsIgnoreCase("n")) {
            return getNPC(id);
        } else if (type.equalsIgnoreCase("m")) {
            return getMonster(id);
        } else {
            System.err.println("Unknown Life type: " + type + "");
            return null;
        }
    }

    public static int getNPCLocation(int npcid) {
        if (NPCLoc.containsKey(npcid)) {
            return NPCLoc.get(npcid);
        }
        final int map = MapleDataTool.getIntConvert(Integer.toString(npcid) + "/0", npclocData, -1);
        NPCLoc.put(npcid, map);
        return map;
    }

    public static final void loadQuestCounts() {
        if (questCount.size() > 0) {
            return;
        }
        for (MapleDataDirectoryEntry mapz : data.getRoot().getSubdirectories()) {
            if (mapz.getName().equals("QuestCountGroup")) {
                for (MapleDataFileEntry entry : mapz.getFiles()) {
                    final int id = Integer.parseInt(entry.getName().substring(0, entry.getName().length() - 4));
                    MapleData dat = data.getData("QuestCountGroup/" + entry.getName());
                    if (dat != null && dat.getChildByPath("info") != null) {
                        List<Integer> z = new ArrayList<Integer>();
                        for (MapleData da : dat.getChildByPath("info")) {
                            z.add(MapleDataTool.getInt(da, 0));
                        }
                        questCount.put(id, z);
                    } else {
                        System.out.println("null questcountgroup");
                    }
                }
            }
        }
        for (MapleData c : npcStringData) {
//        	System.out.println(c.getName());
            int nid;
            try {
                nid = Integer.parseInt(c.getName());
            } catch (Exception e) {
                continue;
            }
            String n = StringUtil.getLeftPaddedStr(nid + ".img", '0', 11);
            try {
                if (npcData.getData(n) != null) {//only thing we really have to do is check if it exists. if we wanted to, we could get the script as well :3
                    String name = MapleDataTool.getString("name", c, "MISSINGNO");
                    if (name.contains("Maple TV") || name.contains("Baby Moon Bunny")) {
                        continue;
                    }
                    npcNames.put(nid, name);
                }
            } catch (NullPointerException e) {
            } catch (RuntimeException e) { //swallow, don't add if 
            }
        }
    }

    public static final void loadNpcScripts() {
        for (MapleData c : npcStringData) {
            int nid;
            try {
                nid = Integer.parseInt(c.getName());
            } catch (Exception e) {
                continue;
            }
            String n = StringUtil.getLeftPaddedStr(nid + ".img", '0', 11);
            try {
                if (npcData.getData(n) != null) {//only thing we really have to do is check if it exists. if we wanted to, we could get the script as well :3
                    for (MapleData d : npcData.getData(n)) {
                        if (d.getName().equals("info")) {
                            for (MapleData e : d) {
                                if (e.getName().equals("script")) {
                                    for (MapleData f : e) {
                                        if (e.getName().equals("script")) {
                                            for (MapleData scripts : f) {
                                                if (scripts.getType() != MapleDataType.STRING) { //                                                   continue;
                                                }
                                                npcScripts.put(nid, (String) scripts.getData());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException e) {
                System.out.println(c.getName());
                e.printStackTrace();
            } catch (RuntimeException e) { //swallow, don't add if 
            }
        }
        // Custom Scripts
        npcScripts.put(9000216, "mannequin_manage");
    }

    public static final List<Integer> getQuestCount(final int id) {
        return questCount.get(id);
    }

    public static MapleMonster getMonster(int mid) {
        MapleMonsterStats stats = getMonsterStats(mid);
        if (stats == null) {
            return null;
        }
        return new MapleMonster(mid, stats);
    }

    public static MapleMonster getMonster(int mid, boolean extreme) {
        MapleMonsterStats stats = getMonsterStats(mid);
        if (stats == null) {
            return null;
        }
        return new MapleMonster(mid, stats, extreme);
    }

    public static MapleMonsterStats getMonsterStats(int mid) {
        MapleMonsterStats stats = monsterStats.get(Integer.valueOf(mid));

        if (stats == null) {
            MapleData monsterData = null;
            try {
                monsterData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(mid) + ".img", '0', 11));
            } catch (RuntimeException e) {
                return null;
            }
            if (monsterData == null) {
                return null;
            }
            MapleData monsterInfoData = monsterData.getChildByPath("info");
            stats = new MapleMonsterStats(mid);

            short mobLevel = (short) MapleDataTool.getIntConvert("level", monsterInfoData, 1);

            switch (mid) {
                case 9833971:
                    stats.setHp(1000);
                    break;
                case 9020107:
                    stats.setHp(9999999999999999L);
                    break;
                case 8880181:
                case 8880183:
                case 8880184:
                case 8880185:
                    stats.setHp(600000000000L);
                    break;
                case 9305600:
                    stats.setHp(3000000000L);
                    break;
                case 9500149:
                    stats.setHp(80000000000L);
                    break;
                case 9500150:
                    stats.setHp(380000000000L);
                    break;
                case 9500147:
                    stats.setHp(1580000000000L);
                    break;
                case 8210002:
                    stats.setHp(350000000000L);
                    break;
                case 9010180:
                    stats.setHp(3005952744540L);
                    break;
                case 9440025:
                    stats.setHp(1005500000000000000L);
                    break;
                case 8120105: //중급피로도
                    stats.setHp(3216291020L);
                    break;
                case 9300075: //1티어
                    stats.setHp(2952744540L);
                    break;
                case 9300076:
                    stats.setHp(8952744540L);
                    break;
                case 9500380:
                    stats.setHp(109527445400L);
                    break;
                case 9833556:
                    stats.setHp(305952744540L);
                    break;
                case 9300851:
                    stats.setHp(665952744540L);
                    break;
                case 9832021:
                    stats.setHp(805952744540L);
                    break;
                case 9833495:
                    stats.setHp(905952744540L);
                    break;
                case 9500136:
                    stats.setHp(1005952744540L);
                    break;
                case 8144008:
                    stats.setHp(50000000000000L);
                    break;
                case 8144005:
                    stats.setHp(99999999999900000L);
                    break;
                case 8210004:
                    stats.setHp(400000000000L);
                    break;
                case 8210003:
                    stats.setHp(1500000000000L);
                    break;
                case 7130102:
                    stats.setHp(520000000000L);
                    break;
                case 9309083:
                    stats.setHp(10000000000000L);
                    break;
                case 9833493:
                    stats.setHp(10000000000000L);
                    break;
                case 9300136:
                    stats.setHp(30000000L);
                    break;
                case 9410004:
                    stats.setHp(500000000L);
                    break;
                case 8210000:
                    stats.setHp(50000000000L);
                    break;
                case 8644018:
                    stats.setHp(7257531880L);
                    break;
                case 8644019:
                    stats.setHp(15257531880L);
                    break;
                case 8644017:
                    stats.setHp(36257531880L);
                    break;
                case 9306007:
                    stats.setHp(66257531880L);
                    break;
                case 9306000:
                    stats.setHp(8257531880L);
                    break;
                case 8880405:
                    stats.setHp(1500000000000000000L);
                    break;
                case 8880406:
                    stats.setHp(1000000000000L);
                    break;
                case 8880407:
                    stats.setHp(1000000000000L);
                    break;
                case 8880425:
                    stats.setHp(1000000000000L);
                    break;
                case 9010129:
                    stats.setHp(120000000000L);
                    break;
                case 9010135:
                    stats.setHp(120000000000L);
                    break;
                case 9010128:
                    stats.setHp(120000000000L);
                    break;
                case 9300019:
                    stats.setHp(10000000000L);
                    break;
                case 9500675:
                    stats.setHp(30000000000L);
                    break;
                case 9420002:
                    stats.setHp(50000000000L);
                    break;
                case 8230076:
                    stats.setHp(80000000000L);
                    break;
                case 9300009:
                    stats.setHp(29000000000000L);
                    break;
                case 9460026:
                    stats.setHp(7999999999999999999L);
                    break;
                case 9400050:
                    stats.setHp(999999999999999999L);
                    break;
                case 9833530:
                    stats.setHp(10000000000000L);
                    break;
                case 9833568:
                    stats.setHp(300000000000000L);
                    break;
                case 9500475:
                    stats.setHp(10000000000000000L);
                    break;
                case 8800002:
                    stats.setHp(14000000L);
                    break;
                case 8644619:
                    stats.setHp(10000000000L);
                    break;
                case 8645040:
                    stats.setHp(15000000000L);
                    break;
                case 4110301:
                    stats.setHp(90000000L);
                    break;
                case 9305653:
                    stats.setHp(9000000000000000000L);
                    break;
                case 8800102:
                    stats.setHp(252000000000L);
                    break;
                case 8850011:
                    stats.setHp(252000000000L);
                    break;
                case 8820001:
                    stats.setHp(22950000000L);
                    break;
                case 8860000:
                    stats.setHp(37800000000L);
                    break;
                case 8840000:
                    stats.setHp(12600000000L);
                    break;
                case 8840014:
                    stats.setHp(21000000000L);
                    break;
                case 8870000:
                    stats.setHp(1000000000L);
                    break;
                case 8870100:
                    stats.setHp(50400000000L);
                    break;
                case 8920100:
                    stats.setHp(630000000L);
                    break;
                case 8920002:
                    stats.setHp(420000000000L);
                    break;
                case 8930100:
                    stats.setHp(1100000000L);
                    break;
                case 8930000: //카오스 벨룸
                    stats.setHp(880000000000L);
                    break;
                case 8641010:
                    stats.setHp(400000000000L);
                    break;
                case 2600105:
                    stats.setHp(20000000000L);
                    break;
                case 9309207:
                    stats.setHp(1000000000000000000L);
                case 8910100:
                    stats.setHp(3000000000L);
                    break;
                case 8910000: //카오스 반반
                    stats.setHp(610000000000L);
                    break;
                case 8500011:
                    stats.setHp(24900000000L);
                    break;
                case 8500012:
                    stats.setHp(8300000000L);
                    break;
                case 8500021:
                    stats.setHp(756000000000L);
                    break;
                case 8500022:
                    stats.setHp(1504000000000L);
                    break;
                case 9300035:
                    stats.setHp(5000000000000000L);
                    break;
                case 8880002:
                    stats.setHp(12000000000L);
                    break;
                case 8810018: // 노말 혼테일
                    stats.setHp(3250000000L);
                    break;
                case 8810122: // 카오스 혼테일
                    stats.setHp(23850000000L);
                    break;
                case 8880000: // 하드 매그너스
                    stats.setHp(720000000000L);
                    break;
                case 8900100: // 노말 피에르
                    stats.setHp(3150000000L);
                    break;
                case 8900000: // 카오스 피에르
                    stats.setHp(590000000000L);
                    break;
                case 8920000: // 카오스 블러드 퀸
                    stats.setHp(650000000000L);
                    break;
                case 8950100: // 노말 스우 1 페이지
                    stats.setHp(1590000000000L);
                    break;
                case 8950101: // 노말 스우 2 페이지
                    stats.setHp(910000000000L);
                    break;
                case 8950102: // 노말 스우 3 페이지
                    stats.setHp(2500000000000L);
                    break;
                case 8250031: // 플라잉레드부터~
                    stats.setHp(100000000L);
                    break;
                case 8250032: //
                    stats.setHp(100000000L);
                    break;
                case 8250033: //
                    stats.setHp(200000000L);
                    break;
                case 8250034: // 그린까지
                    stats.setHp(300000000L);
                    break;
                case 8644821: // 데미안 구슬
                    stats.setHp(100000000000L);
                    break;
                case 9803055: // 데미안 구슬2
                    stats.setHp(1000000000000000L);
                    break;

                case 8240124: // 노말스우 레이져안드
                    stats.setHp(2500000000000L);
                    break;

                case 8240059: // 하드스우 레이져안드
                    stats.setHp(20000000000000L);
                    break;

                case 8950000: // 하드 스우 1 페이지
                    stats.setHp(4700000000000L);
                    break;
                case 8950001: // 하드 스우 2 페이지
                    stats.setHp(19000000000000L);
                    break;
                case 8950002: // 하드 스우 3 페이지
                    stats.setHp(35000000000000L);
                    break;
                case 8880110: // 노말 데미안 1 페이지
                    stats.setHp(2040000000000L);
                    break;
                case 8880111: // 노말 데미안 2 페이지
                    stats.setHp(2360000000000L);
                    break;
                case 8880100: // 하드 데미안 1 페이지
                    stats.setHp(45200000000000L);
                    break;
                case 8880101: // 하드 데미안 2 페이지
                    stats.setHp(25200000000000L);
                    break;
                case 8880140: // 노말 루시드 1 페이지
                    stats.setHp(8800000000000L);
                    break;
                case 8880150: // 노말 루시드 2 페이지
                    stats.setHp(12800000000000L);
                    break;
                case 8880141: // 하드 루시드 1 페이지
                    stats.setHp(120800000000000L);
                    break;
                case 8880151: // 하드 루시드 2 페이지
                    stats.setHp(120800000000000L);
                    break;
                case 8880153: // 하드 루시드 3 페이지

                    stats.setHp(21970000000000L);
                    break;
                case 8880343:
                case 8880344:
                case 8880340: // 노말 윌 1 페이지
                    stats.setHp(65700000000000L);
                    break;
                case 8880341: // 노말 윌 2 페이지
                    stats.setHp(67500000000000L);
                    break;
                case 8880342: // 노말 윌 3 페이지
                    stats.setHp(136900000000000L);
                    break;
                case 8800200: // 라바나
                    stats.setHp(230000000000000L);
                    break;
                case 8880303:
                case 8880304:
                case 8880300: // 하드 윌 1 페이지
                    stats.setHp(452000000000000L);
                    break;
                case 8880301: // 하드 윌 2 페이지
                    stats.setHp(350500000000000L);
                    break;
                case 8880302: // 하드 윌 3 페이지
                    stats.setHp(502500000000000L);
                    break;
                case 9101078: // 불꽃 늑대
                    stats.setHp(1161200000000000L);
                    break;
                case 8881000: // 우르스
                    stats.setHp(1085000000000000L);
                    break;
                case 8645009: // 듄켈
                    stats.setHp(1055000000000000L);
                    break;
                case 8644630: // 엘보 1
                    stats.setHp(250000L);
                    break;
                case 9303131: // 엘보 2
                    stats.setHp(10000000L);
                    break;
                case 8820200: // 카핑
                    stats.setHp(8000000000000L);
                    break;

                case 8220022: // 듄켈 수하 1
                case 8220023: // 듄켈 수하 2
                case 8220024: // 듄켈 수하 3
                case 8220026: // 듄켈 수하 5
                    stats.setHp(70000000000000L);

                case 8220025: // 엘보
                    stats.setHp(100000000000L);
                    break;
                case 9833400: // 뉴트로몹
                    stats.setHp(100000000000000L);
                    break;

                case 9300454: // 아니
                    stats.setHp(2900000000000L);
                    break;

                case 9300012: // 알리샤르
                    stats.setHp(120000000000L);
                    break;
                case 8644650: // 더스크
                    stats.setHp(600000000000000L);
                    break;
                case 8644655: // 카오스 더스크
                    stats.setHp(2255000000000000L);
                    break;
                case 8644612: // 더스크 수하
                    stats.setHp(1000000000L);
                    break;
                case 9500360: // 진 힐라 손
                    stats.setHp(1000000000L);
                    break;
                case 6500001: // 진 힐라
                    stats.setHp(3555000000000000L);
                    break;
                case 8880413: // 진 힐라 수하 1
                    stats.setHp(350000000000L);
                    break;
                case 8880414: // 진 힐라 수하 2
                    stats.setHp(350000000000L);
                    break;
                case 8880415: // 진 힐라 수하 3
                    stats.setHp(350000000000L);
                    break;
                case 8880500: // 창조의 아이온
                case 8880501: // 파괴의 얄다바오트
                    stats.setHp(8000000000000000L);
                    break;

                case 8880200: // 카웅
                    stats.setHp(400000000L);
                    break;
                case 8880505: // 검은마법사 1 페이즈
                    stats.setHp(1200000000000000L);
                    break;
                case 8880502: // 검은마법사 2 페이즈
                    stats.setHp(2900000000000000L);
                    break;
                case 8880503: // 검은마법사 3 페이즈
                    stats.setHp(3222000000000000L);
                    break;
                case 8880504: // 검은마법사 4 페이즈
                    stats.setHp(2813000000000000L);
                    break;
                case 8880600: // 세렌 1페이즈
                    stats.setHp(6320000000000000L);
                    break;
                case 8880602: // 세렌 2페이즈
                    stats.setHp(6320000000000000L);
                    break;
                    
                case 8144000: // 황금사원 275~
                    stats.setHp(8554702000L);
                    stats.setLevel((short) 275);
                    break;

                case 8144001: // 황금사원 275~
                    stats.setHp(10554702000L);
                    stats.setLevel((short) 275);
                    break;

                case 8144002: // 황금사원 275~
                    stats.setHp(11554702000L);

                    break;
                case 8144003: // 황금사원 275~
                    stats.setHp(12554702000L);

                    break;
                case 8144004: // 황금사원 275~
                    stats.setHp(13554702000L);

                    break;

                case 8144006: // 황금사원 275~
                    stats.setHp(1554702000L);

                    break;
                case 8144007: // 황금사원 275~
                    stats.setHp(16554702000L);

                    break;

                case 9833802: // 테마파크 300~
                    stats.setHp(7554702000L);

                    break;

                case 8145001: // 테마파크 300~
                    stats.setHp(17554702000L);

                    break;

                case 8145002: // 테마파크 300~
                    stats.setHp(20554702000L);

                    break;

                case 8145003: // 테마파크 300~
                    stats.setHp(22554702000L);

                    break;

                case 8145004: // 테마파크 300~ㄷ
                    stats.setHp(24554702000L);

                    break;

                case 8145005: // 테마파크 300~
                    stats.setHp(25154702000L);

                    break;

                case 9833803: // 티어사냥터 300~
                    stats.setHp(12154702000L);

                    break;

                case 9833804: // 티어사냥터 300~
                    stats.setHp(18254702000L);

                    break;

                case 9833805: // 티어사냥터 300~
                    stats.setHp(20954702000L);

                    break;

                case 9300800: // 허수아비 (데미지미터기)
                    stats.setHp(9000000000000000000L);

                    break;

                default:
                    long hp = 0;
                    if (monsterInfoData.getChildByPath("finalmaxHP") != null) {
                        hp += MapleDataTool.getIntConvert("maxHP", monsterInfoData);
                        hp += MapleDataTool.getLongConvert("finalmaxHP", monsterInfoData, 0);
                    } else {
                        hp = MapleDataTool.getLongConvert("maxHP", monsterInfoData);
                    }

                    // 몹HP배율
                    double rate = 100.0;

                    if (mobLevel < 241) { // 1~200
                        rate += 0.0;
                    } else if (mobLevel < 251) { // 240~250
                        rate += 50.0;
                    } else if (mobLevel < 261) { // 250~260
                        rate += 150.0;
                    } else if (mobLevel < 271) { // 260~270
                        rate += 200.0;
                    } else if (mobLevel < 301) { // 260~270
                        rate += 300.0;
                    }

                    hp = (long) (rate * hp / 100.0);

                    stats.setHp(GameConstants.getPartyPlayHP(mid) > 0 ? GameConstants.getPartyPlayHP(mid) : hp);
                    break;
            }


            stats.setMp(MapleDataTool.getIntConvert("maxMP", monsterInfoData, 0));
            stats.setExp(mid == 9300027 ? 0 : (GameConstants.getPartyPlayEXP(mid) > 0 ? GameConstants.getPartyPlayEXP(mid) : MapleDataTool.getIntConvert("exp", monsterInfoData, 0)));
            stats.setLevel(mobLevel);
            stats.setCharismaEXP((short) MapleDataTool.getIntConvert("charismaEXP", monsterInfoData, 0));
            stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", monsterInfoData, 0));
            stats.setrareItemDropLevel((byte) MapleDataTool.getIntConvert("rareItemDropLevel", monsterInfoData, 0));
            stats.setFixedDamage(MapleDataTool.getIntConvert("fixedDamage", monsterInfoData, -1));
            stats.setOnlyNormalAttack(MapleDataTool.getIntConvert("onlyNormalAttack", monsterInfoData, 0) > 0);
            stats.setBoss(GameConstants.getPartyPlayHP(mid) > 0 || MapleDataTool.getIntConvert("boss", monsterInfoData, 0) > 0 || mid == 8810018 || mid == 9410066 || (mid >= 8810118 && mid <= 8810122));
            stats.setExplosiveReward(MapleDataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0);
            stats.setUndead(MapleDataTool.getIntConvert("undead", monsterInfoData, 0) > 0);
            stats.setEscort(MapleDataTool.getIntConvert("escort", monsterInfoData, 0) > 0);
            stats.setPartyBonus(GameConstants.getPartyPlayHP(mid) > 0 || MapleDataTool.getIntConvert("partyBonusMob", monsterInfoData, 0) > 0);
            stats.setPartyBonusRate(MapleDataTool.getIntConvert("partyBonusR", monsterInfoData, 0));
            if (mobStringData.getChildByPath(String.valueOf(mid)) != null) {
                stats.setName(MapleDataTool.getString("name", mobStringData.getChildByPath(String.valueOf(mid)), "MISSINGNO"));
            }
            stats.setBuffToGive(MapleDataTool.getIntConvert("buff", monsterInfoData, -1));
            stats.setChange(MapleDataTool.getIntConvert("changeableMob", monsterInfoData, 0) > 0);
            stats.setFriendly(MapleDataTool.getIntConvert("damagedByMob", monsterInfoData, 0) > 0);
            stats.setNoDoom(MapleDataTool.getIntConvert("noDoom", monsterInfoData, 0) > 0);
            stats.setCP((byte) MapleDataTool.getIntConvert("getCP", monsterInfoData, 0));
            stats.setPoint(MapleDataTool.getIntConvert("point", monsterInfoData, 0));
            stats.setDropItemPeriod(MapleDataTool.getIntConvert("dropItemPeriod", monsterInfoData, 0));
            stats.setPhysicalAttack(MapleDataTool.getIntConvert("PADamage", monsterInfoData, 0));
            stats.setMagicAttack(MapleDataTool.getIntConvert("MADamage", monsterInfoData, 0));
            stats.setPDRate((byte) MapleDataTool.getIntConvert("PDRate", monsterInfoData, 0));
            stats.setMDRate((byte) MapleDataTool.getIntConvert("MDRate", monsterInfoData, 0));
            stats.setAcc(MapleDataTool.getIntConvert("acc", monsterInfoData, 0));
            stats.setEva(MapleDataTool.getIntConvert("eva", monsterInfoData, 0));
            stats.setSummonType((byte) MapleDataTool.getIntConvert("summonType", monsterInfoData, 0));
            stats.setHpLinkMob(MapleDataTool.getIntConvert("HpLinkMob", monsterInfoData, 0));
            if (mid == 8880512) { // fuck
                stats.setSummonType((byte) 1);
            }
            stats.setCategory((byte) MapleDataTool.getIntConvert("category", monsterInfoData, 0));
            stats.setSpeed(MapleDataTool.getIntConvert("speed", monsterInfoData, 0));
            stats.setPushed(MapleDataTool.getIntConvert("pushed", monsterInfoData, 0));
            stats.setPublicReward(MapleDataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0 || MapleDataTool.getIntConvert("individualReward", monsterInfoData, 0) > 0);
            final boolean hideHP = MapleDataTool.getIntConvert("HPgaugeHide", monsterInfoData, 0) > 0 || MapleDataTool.getIntConvert("hideHP", monsterInfoData, 0) > 0;
            final MapleData selfd = monsterInfoData.getChildByPath("selfDestruction");
            if (selfd != null) {
                stats.setSelfDHP(MapleDataTool.getIntConvert("hp", selfd, 0));
                stats.setRemoveAfter(MapleDataTool.getIntConvert("removeAfter", selfd, stats.getRemoveAfter()));
                stats.setSelfD((byte) MapleDataTool.getIntConvert("action", selfd, -1));
            } else {
                stats.setSelfD((byte) -1);
            }
            final MapleData firstAttackData = monsterInfoData.getChildByPath("firstAttack");
            if (firstAttackData != null) {
                if (firstAttackData.getType() == MapleDataType.FLOAT) {
                    stats.setFirstAttack(Math.round(MapleDataTool.getFloat(firstAttackData)) > 0);
                } else {
                    stats.setFirstAttack(MapleDataTool.getInt(firstAttackData) > 0);
                }
            }
            if (stats.isBoss() || isDmgSponge(mid)) {
                if (/*hideHP || */monsterInfoData.getChildByPath("hpTagColor") == null || monsterInfoData.getChildByPath("hpTagBgcolor") == null) {
                    stats.setTagColor(0);
                    stats.setTagBgColor(0);
                } else {
                    stats.setTagColor(MapleDataTool.getIntConvert("hpTagColor", monsterInfoData));
                    stats.setTagBgColor(MapleDataTool.getIntConvert("hpTagBgcolor", monsterInfoData));
                }
            }

            final MapleData banishData = monsterInfoData.getChildByPath("ban");
            if (banishData != null) {
                stats.setBanishInfo(new BanishInfo(
                        MapleDataTool.getString("banMsg", banishData),
                        MapleDataTool.getInt("banMap/0/field", banishData, -1),
                        MapleDataTool.getString("banMap/0/portal", banishData, "sp")));
            }

            if (mid == 8860000 || mid == 8860001 || mid == 8860005 || mid == 8860007) {
                stats.setBanishInfo(new BanishInfo("자신 속의 추악한 내면을 마주한 기분이 어떠신지요?", 272020300, "0"));
            }

            final MapleData reviveInfo = monsterInfoData.getChildByPath("revive");
            if (reviveInfo != null) {
                List<Integer> revives = new LinkedList<Integer>();
                for (MapleData bdata : reviveInfo) {
                    revives.add(MapleDataTool.getInt(bdata));
                }
                stats.setRevives(revives);
            }

            final MapleData skeletonData = monsterData.getChildByPath("HitParts");
            if (skeletonData != null) {
                for (MapleData skeleton : skeletonData) {
                    int durability = Integer.valueOf(MapleDataTool.getInt("0/stat/durability", skeleton, 0));
                    stats.addSkeleton(skeleton.getName(), 0, durability);
                }
            }

            final MapleData trans = monsterInfoData.getChildByPath("trans");

            if (trans != null) {
                Transform transform = new Transform(
                        MapleDataTool.getInt("0", trans, 0),
                        MapleDataTool.getInt("1", trans, 0),
                        MapleDataTool.getInt("cooltime", trans, 0),
                        MapleDataTool.getInt("hpTriggerOff", trans, 0),
                        MapleDataTool.getInt("hpTriggerOn", trans, 0),
                        MapleDataTool.getInt("time", trans, 0),
                        MapleDataTool.getInt("withMob", trans, 0));
                List<Pair<Integer, Integer>> skills = new ArrayList<>();

                MapleData transSkills = trans.getChildByPath("skill");
                if (transSkills != null) {
                    for (MapleData transSkill : transSkills.getChildren()) {
                        skills.add(new Pair<Integer, Integer>(MapleDataTool.getInt("skill", transSkill, 0), MapleDataTool.getInt("level", transSkill, 0)));
                    }
                }

                transform.setSkills(skills);
                stats.setTrans(transform);
            }

            final MapleData monsterSkillData = monsterInfoData.getChildByPath("skill");
            if (monsterSkillData != null) {
                int i = 0;
                List<MobSkill> skills = new ArrayList<>();
                while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
                    int onlyFsm = Integer.valueOf(MapleDataTool.getInt(i + "/onlyFsm", monsterSkillData, 0));
                    int onlyOtherSkill = Integer.valueOf(MapleDataTool.getInt(i + "/onlyOtherSkill", monsterSkillData, 0));
                    MobSkill ms = MobSkillFactory.getMobSkill(Integer.valueOf(MapleDataTool.getInt(i + "/skill", monsterSkillData, 0)), Integer.valueOf(MapleDataTool.getInt(i + "/level", monsterSkillData, 0)));
                    if (ms != null) {
                        ms.setOnlyFsm(onlyFsm > 0);
                        ms.setAction(Integer.valueOf(MapleDataTool.getInt(i + "/action", monsterSkillData, 0)));
                        int skillAfter = Integer.valueOf(MapleDataTool.getInt(i + "/skillAfter", monsterSkillData, 0));
                        if (skillAfter > ms.getSkillAfter()) {
                            ms.setSkillAfter(skillAfter);
                        }
                        ms.setOnlyOtherSkill(onlyOtherSkill > 0);
                        ms.setSkillForbid(Integer.valueOf(MapleDataTool.getInt(i + "/skillForbid", monsterSkillData, 0)));
                        ms.setAfterAttack(Integer.valueOf(MapleDataTool.getInt(i + "/afterAttack", monsterSkillData, -1)));
                        ms.setAfterAttackCount(Integer.valueOf(MapleDataTool.getInt(i + "/afterAttackCount", monsterSkillData, 0)));
                        ms.setAfterDead(Integer.valueOf(MapleDataTool.getInt(i + "/afterDead", monsterSkillData, 0)));
                        skills.add(ms);
                    }
                    i++;
                }
                stats.setSkills(skills);
            }

            decodeElementalString(stats, MapleDataTool.getString("elemAttr", monsterInfoData, ""));

            // Other data which isn't in the mob, but might in the linked data
            final int link = MapleDataTool.getIntConvert("link", monsterInfoData, 0);
            if (link != 0) { // Store another copy, for faster processing.
                monsterData = data.getData(StringUtil.getLeftPaddedStr(link + ".img", '0', 11));
            }

            for (MapleData idata : monsterData) {
                if (idata.getName().equals("fly")) {
                    stats.setFly(true);
                    stats.setMobile(true);
                    break;
                } else if (idata.getName().equals("move")) {
                    stats.setMobile(true);
                }
            }

            final boolean mobZone = monsterInfoData.getChildByPath("mobZone") != null;
            stats.setMobZone(mobZone);

            final MapleData monsterAtt = monsterInfoData.getChildByPath("attack");
            if (monsterAtt != null) {
                int i = 0;
                List<MobAttack> attacks = new ArrayList<MobAttack>();
                while (monsterAtt.getChildByPath(Integer.toString(i)) != null) {
                    MobAttack attack = new MobAttack(MapleDataTool.getInt(i + "/action", monsterAtt, -1), MapleDataTool.getInt(i + "/afterAttack", monsterAtt, -1), MapleDataTool.getInt(i + "/fixAttack", monsterAtt, -1), MapleDataTool.getInt(i + "/onlyAfterAttack", monsterAtt, -1), MapleDataTool.getInt(i + "/cooltime", monsterAtt, -1), MapleDataTool.getInt(i + "/afterAttackCount", monsterAtt, -1));
                    if (monsterAtt.getChildByPath(Integer.toString(i) + "/callSkill") != null) {
                        MapleData callSkillData = monsterAtt.getChildByPath(Integer.toString(i) + "/callSkill");
                        int j = 0;
                        while (callSkillData.getChildByPath(String.valueOf(j)) != null) {
                            MapleData callSkillIdxData = callSkillData.getChildByPath(String.valueOf(j));
                            attack.addSkill(MapleDataTool.getInt("skill", callSkillIdxData, 0), MapleDataTool.getInt("level", callSkillIdxData, 0), MapleDataTool.getInt("delay", callSkillIdxData, 0));
                            j++;
                        }
                    }
                    if (monsterAtt.getChildByPath(Integer.toString(i) + "/callSkillWithData") != null) {
                        MapleData callSkillData = monsterAtt.getChildByPath(Integer.toString(i) + "/callSkillWithData");
                        attack.addSkill(MapleDataTool.getInt("skill", callSkillData, 0), MapleDataTool.getInt("level", callSkillData, 0), MapleDataTool.getInt("delay", callSkillData, 0));
                    }
                    attacks.add(attack);
                    i++;
                }
                stats.setAttacks(attacks);
            }

            byte hpdisplaytype = -1;
            if (stats.getTagColor() > 0) {
                hpdisplaytype = 0;
            } else if (stats.isFriendly()) {
                hpdisplaytype = 1;
            } else if (mid >= 9300184 && mid <= 9300215) { // Mulung TC mobs
                hpdisplaytype = 2;
            } else if (!stats.isBoss() || mid == 9410066 || stats.isPartyBonus()) { // Not boss and dong dong chiang
                hpdisplaytype = 3;
            }
            stats.setHPDisplayType(hpdisplaytype);

            monsterStats.put(Integer.valueOf(mid), stats);
        }
        return stats;
    }

    public static final void decodeElementalString(MapleMonsterStats stats, String elemAttr) {
        for (int i = 0; i < elemAttr.length(); i += 2) {
            stats.setEffectiveness(
                    Element.getFromChar(elemAttr.charAt(i)),
                    ElementalEffectiveness.getByNumber(Integer.valueOf(String.valueOf(elemAttr.charAt(i + 1)))));
        }
    }

    private static final boolean isDmgSponge(final int mid) {
        switch (mid) {
            case 8810018:
            case 8810122:
            case 8810119:
            case 8810120:
            case 8810121:
            case 8820009:
            case 8820010:
            case 8820011:
            case 8820012:
            case 8820013:
            case 8820014:
            case 8820110:
            case 8820111:
            case 8820112:
            case 8820113:
            case 8820114:
                return true;
        }
        return false;
    }

    public static MapleNPC getNPC(final int nid) {
        String name = npcNames.get(nid);
        if (name == null) {
            return null;
        }
        return new MapleNPC(nid, name);
    }

    public static int getRandomNPC() {
        List<Integer> vals = new ArrayList<Integer>(npcNames.keySet());
        int ret = 0;
        while (ret <= 0) {
            ret = vals.get(Randomizer.nextInt(vals.size()));
            if (npcNames.get(ret).contains("MISSINGNO")) {
                ret = 0;
            }
        }
        return ret;
    }

    public static Map<Integer, String> getNpcScripts() {
        return npcScripts;
    }

    public static void setNpcScripts(Map<Integer, String> npcScripts) {
        MapleLifeFactory.npcScripts = npcScripts;
    }
}
