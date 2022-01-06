package constants;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MatrixSkill;
import client.PlayerStats;
import client.Skill;
import client.SkillFactory;
import client.custom.inventory.CustomItem;
import client.inventory.Equip;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatus;
import handling.channel.handler.AttackInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.DailyGiftItemInfo;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField;

public class GameConstants {

    public static int bitcoin = 0;

    public static final List<Triple<Integer, Integer, Integer>> fishingItem = new ArrayList<>();

    static {
        fishingItem.add(new Triple<>(3801268, 1, 80));
        fishingItem.add(new Triple<>(3801269, 1, 60));
        fishingItem.add(new Triple<>(3801270, 1, 30));
        fishingItem.add(new Triple<>(3801271, 1, 10));
        fishingItem.add(new Triple<>(3801272, 1, 5));
        fishingItem.add(new Triple<>(3801273, 1, 1));
    }

    public static final List<MapleMapObjectType> rangedMapobjectTypes = Collections.unmodifiableList(Arrays.asList(
            MapleMapObjectType.RUNE,
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.REACTOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.NPC,
            MapleMapObjectType.MIST,
            MapleMapObjectType.EXTRACTOR));

    public static final long[] exp = new long[1000];
    public static final long[] exp2 = new long[20];
    private static final int[] closeness = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161, 2793,
        3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074,
        30000};
    private static final int[] setScore = {0, 10, 100, 300, 600, 1000, 2000, 4000, 7000, 10000};
    private static final int[] cumulativeTraitExp = {0, 20, 46, 80, 124, 181, 255, 351, 476, 639, 851, 1084,
        1340, 1622, 1932, 2273, 2648, 3061, 3515, 4014, 4563, 5128,
        5710, 6309, 6926, 7562, 8217, 8892, 9587, 10303, 11040, 11788,
        12547, 13307, 14089, 14883, 15689, 16507, 17337, 18179, 19034, 19902,
        20783, 21677, 22584, 23505, 24440, 25399, 26362, 27339, 28331, 29338,
        30360, 31397, 32450, 33519, 34604, 35705, 36823, 37958, 39110, 40279,
        41466, 32671, 43894, 45135, 46395, 47674, 48972, 50289, 51626, 52967,
        54312, 55661, 57014, 58371, 59732, 61097, 62466, 63839, 65216, 66597,
        67982, 69371, 70764, 72161, 73562, 74967, 76376, 77789, 79206, 80627,
        82052, 83481, 84914, 86351, 87792, 89237, 90686, 92139, 93596, 96000};
    private static final int[] mobHpVal = {0, 15, 20, 25, 35, 50, 65, 80, 95, 110, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350,
        375, 405, 435, 465, 495, 525, 580, 650, 720, 790, 900, 990, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800,
        1900, 2000, 2100, 2200, 2300, 2400, 2520, 2640, 2760, 2880, 3000, 3200, 3400, 3600, 3800, 4000, 4300, 4600, 4900, 5200,
        5500, 5900, 6300, 6700, 7100, 7500, 8000, 8500, 9000, 9500, 10000, 11000, 12000, 13000, 14000, 15000, 17000, 19000, 21000, 23000,
        25000, 27000, 29000, 31000, 33000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000, 53000, 55000, 57000, 59000, 61000, 63000,
        65000, 67000, 69000, 71000, 73000, 75000, 77000, 79000, 81000, 83000, 85000, 89000, 91000, 93000, 95000, 97000, 99000, 101000, 103000,
        105000, 107000, 109000, 111000, 113000, 115000, 118000, 120000, 125000, 130000, 135000, 140000, 145000, 150000, 155000, 160000, 165000, 170000, 175000, 180000,
        185000, 190000, 195000, 200000, 205000, 210000, 215000, 220000, 225000, 230000, 235000, 240000, 250000, 260000, 270000, 280000, 290000, 300000, 310000, 320000,
        330000, 340000, 350000, 360000, 370000, 380000, 390000, 400000, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 500000, 510000, 520000,
        530000, 550000, 570000, 590000, 610000, 630000, 650000, 670000, 690000, 710000, 730000, 750000, 770000, 790000, 810000, 830000, 850000, 870000, 890000, 910000};
    private static final int[] pvpExp = {0, 3000, 6000, 12000, 24000, 48000, 960000, 192000, 384000, 768000};
    private static final int[] guildexp = {0, 15000, 60000, 135000, 240000, 375000, 540000, 735000, 960000, 1215000, 1500000, 1815000, 2160000, 2535000, 2940000, 3375000,
        3840000, 4335000, 4860000, 5415000, 6000000, 6615000, 7260000, 7935000, 8640000, 12528000, 18165600, 26340120, 38193170, 68747700};
    private static final int[] mountexp = {0, 6, 25, 50, 105, 134, 196, 254, 263, 315, 367, 430, 543, 587, 679, 725, 897, 1146, 1394, 1701, 2247,
        2543, 2898, 3156, 3313, 3584, 3923, 4150, 4305, 4550};
    public static final int[] itemBlock = {};
    public static final int[] cashBlock = {}; //miracle cube and stuff
    public static final int JAIL = 180000002, MAX_BUFFSTAT = 31, MAX_MOB_BUFFSTAT = 4;
    public static final int[] blockedSkills = {4341003};
    public static final String[] RESERVED = {"Rental", "Donor", "MapleNews"};
    public static final List<Pair<String, String>> dList = new ArrayList<>();
    public static final String[] stats = {"tuc", "reqLevel", "reqJob", "reqSTR", "reqDEX", "reqINT", "reqLUK", "reqPOP", "cash", "cursed", "success", "setItemID", "equipTradeBlock", "durability", "randOption", "randStat", "masterLevel", "reqSkillLevel", "elemDefault", "incRMAS", "incRMAF", "incRMAI", "incRMAL", "canLevel", "skill", "charmEXP", "bdR", "imdR", "onlyEquip", "jokerToSetItem"};
    public static final int[] hyperTele = {310000000, 220000000, 100000000, 250000000, 240000000, 104000000, 103000000, 102000000, 101000000, 120000000, 260000000, 200000000, 230000000};
    public static final int[] rankC = {70000000, 70000001, 70000002, 70000003, 70000004, 70000005, 70000006, 70000007, 70000008, 70000009, 70000010, 70000011, 70000012, 70000013};
    public static final int[] rankB = {70000014, 70000015, 70000016, 70000017, 70000018, 70000021, 70000022, 70000023, 70000024, 70000025, 70000026};
    public static final int[] rankA = {70000027, 70000028, 70000029, 70000030, 70000031, 70000032, 70000033, 70000034, 70000035, 70000036, 70000039, 70000040, 70000041, 70000042};
    public static final int[] rankS = {70000043, 70000044, 70000045, 70000047, 70000048, 70000049, 70000050, 70000051, 70000052, 70000053, 70000054, 70000055, 70000056, 70000057, 70000058, 70000059, 70000060, 70000061, 70000062};
    public static final int[] showEffectDropItems = {1022231, 1012478, 1132296, 1113149, 1032241, 1122000, 1122076, 1022232, 1132272,
        1162025, 1122254, 1122150, 1162009, 1032136, 1152170, 1182087, 1113282, 1022277,
        1672076, 1012632, 1022278, 1132308, 1162080, 1162082, 1162081, 1162083, 1122430, 1182285};
    public static final int[][] StarInfo = {
        {20, 30, 50},
        {20, 30, 50},
        {20, 30, 50},
        {10, 30, 50},
        {20, 60, 100},
        {15, 45, 75},
        {30, 90, 180},
        {15, 45, 75},
        {20, 60, 100},
        {35, 105, 175},
        {30, 90, 150},
        {20, 60, 100},
        {25, 75, 125},
        {15, 45, 75},
        {15, 45, 75},
        {20, 60, 100},
        {20, 60, 100},
        {120, 200, 300},
        {40, 120, 200},
        {50, 150, 250},
        {30, 90, 150},
        {30, 90, 150},
        {30, 90, 150},
        {30, 90, 150},
        {40, 120, 200},
        {40, 120, 200},
        {60, 180, 300},
        {60, 180, 300},
        {35, 105, 175},
        {30, 90, 150},
        {30, 90, 150},
        {15, 45, 75},
        {60, 180, 300},
        {15, 45, 75},
        {60, 180, 300},
        {40, 120, 200},
        {40, 120, 200},
        {45, 135, 225},
        {35, 105, 175},
        {540, 570, 600}
    };

    public static List<Integer> questReader = new ArrayList<>();
    public static final Map<Integer, Integer> price = new HashMap<>();
    public static final List<DailyGiftItemInfo> dailyItems = new ArrayList<>();
    public static final List<Triple<Integer, Integer, Integer>> chariotItems = new ArrayList<>();
    public static final List<Integer> partyExpMapList = new ArrayList<>();
    public static List<CustomItem> customItems = new ArrayList<>();
    static {
        partyExpMapList.add(993072000);//초급
        partyExpMapList.add(993072100);//중급
        partyExpMapList.add(993072200);//상급
        partyExpMapList.add(993072300);//최상급
        partyExpMapList.add(993072400);//초월1
        partyExpMapList.add(993072500);//초월2
    }
    public static final int MaxLevel = 999;

    static { //황금마차 보상
        chariotItems.add(new Triple<>(9, 2630512, 1));
        chariotItems.add(new Triple<>(18, 2630512, 1));
        chariotItems.add(new Triple<>(27, 2630512, 1));
        chariotItems.add(new Triple<>(36, 2630512, 1));
        chariotItems.add(new Triple<>(45, 2630512, 1));
        chariotItems.add(new Triple<>(54, 2630512, 1));
        chariotItems.add(new Triple<>(63, 2630512, 1));
        chariotItems.add(new Triple<>(72, 2630512, 1));
        chariotItems.add(new Triple<>(81, 2630512, 1));
        chariotItems.add(new Triple<>(90, 2630512, 1));
        chariotItems.add(new Triple<>(99, 2630512, 1));
        chariotItems.add(new Triple<>(108, 2630512, 1));
        chariotItems.add(new Triple<>(117, 2630512, 1));
        chariotItems.add(new Triple<>(126, 2630512, 1));
        chariotItems.add(new Triple<>(135, 2630512, 1));

    }

    static {

        //미러던전 엔피시 설정 갯수가 최대 몇개였는지는 기억안나는데 암튼 여기임
        dList.add(new Pair("mirrorD_322_0_", "이동 시스템 :: 어디로든지 이동할 수 있는 시스템"));
        dList.add(new Pair("mirrorD_322_1_", "편의 시스템 :: 서버의 기능을 편하게 이용할 수 있는 시스템"));
        dList.add(new Pair("mirrorD_322_2_", "상점 시스템 :: 유용한 장비 및 아이템을 얻을 수 있는 시스템"));
        dList.add(new Pair("mirrorD_322_3_", "성장 컨텐츠 :: 캐릭터의 육성과 관련된 컨텐츠들을 간편하게 이용할 수 있는 시스템"));
        //  dList.add(new Pair("mirrorD_322_4_", "기능2 :: 기능이라고 별건 없네요"));

        /* 데일리 기프트 */
        dailyItems.add(new DailyGiftItemInfo(1, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(2, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(3, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(4, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(5, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(6, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(7, 4310282, 10, 0));

        dailyItems.add(new DailyGiftItemInfo(8, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(9, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(10, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(11, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(12, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(13, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(14, 4310282, 10, 0));

        dailyItems.add(new DailyGiftItemInfo(15, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(16, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(17, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(18, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(19, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(20, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(21, 4310282, 10, 0));

        dailyItems.add(new DailyGiftItemInfo(22, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(23, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(24, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(25, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(26, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(27, 4310282, 5, 0));
        dailyItems.add(new DailyGiftItemInfo(28, 4310282, 10, 0));

        //일간 보스
        price.put(8800002, 175);
        price.put(8880010, 190);
        price.put(8870000, 200);
        price.put(8810214, 210);
        price.put(8900103, 220);
        price.put(8910100, 220);
        price.put(8920106, 220);
        price.put(8930100, 220);
        price.put(8810018, 225);
        price.put(8840007, 230);
        price.put(8860005, 240);
        price.put(8880200, 250);
        price.put(8810122, 260);
        price.put(8820001, 265);
        price.put(8840000, 270);
        price.put(8840014, 350);
        price.put(8860000, 355);
        price.put(8880002, 360);
        price.put(8500012, 365);

        //주간 보스
        price.put(8850111, 675);
        price.put(8870100, 750);
        price.put(8820212, 800);
        price.put(8850011, 850);
        price.put(8800102, 900);
        price.put(8900003, 900);

        price.put(8910000, 900);
        price.put(8920006, 900);
        price.put(8880000, 975);
        price.put(8930000, 1025);
        price.put(8500022, 1100);
        price.put(8880167, 1100);
        price.put(8950102, 1225);
        price.put(8880111, 1250);
        price.put(8880342, 1400);
        price.put(8880101, 1500);
        price.put(8880177, 1500);
        price.put(8950002, 1525);
        price.put(8880153, 1625);
        price.put(8880302, 1700);
        price.put(8880405, 1875);
        price.put(8880504, 3500);
        price.put(8644650, 4500);
        price.put(8644655, 8500);
    }

    static {

        exp2[0] = 15;
        exp2[1] = 34;
        exp2[2] = 57;
        exp2[3] = 92;
        exp2[4] = 135;
        exp2[5] = 372;
        exp2[6] = 560;
        exp2[7] = 840;
        exp2[8] = 1242;

        for (int a = 1; a < 15; a++) {
            exp[a] = exp2[a - 1];
            exp2[a - 1] = exp[a];
            if (a >= 9) {
                exp2[a] = exp[a];
            }

        }

        for (int a = 15; a < 30; a++) {
            if (a == 15) {
                exp[15] = (long) Math.floor(exp[14] * 1.2);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.2);
            }
        }

        for (int a = 30; a < 35; a++) {
            if (a == 30) {
                exp[30] = exp[29];
            } else {
                exp[a] = exp[a - 1];
            }
        }

        for (int a = 35; a < 40; a++) {
            if (a == 35) {
                exp[35] = (long) Math.floor(exp[34] * 1.2);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.2);
            }
        }

        for (int a = 40; a < 60; a++) {
            if (a == 40) {
                exp[40] = (long) Math.floor(exp[39] * 1.08);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.08);
            }
        }

        for (int a = 60; a < 65; a++) {
            if (a == 60) {
                exp[60] = exp[59];
            } else {
                exp[a] = exp[a - 1];
            }
        }

        for (int a = 65; a < 75; a++) {
            if (a == 65) {
                exp[65] = (long) Math.floor(exp[64] * 1.075);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.075);
            }
        }

        for (int a = 75; a < 90; a++) {
            if (a == 75) {
                exp[75] = (long) Math.floor(exp[74] * 1.07);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.07);
            }
        }
        for (int a = 90; a < 100; a++) {
            if (a == 90) {
                exp[90] = (long) Math.floor(exp[89] * 1.065);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.065);
            }
        }

        for (int a = 100; a < 105; a++) {
            if (a == 100) {
                exp[100] = exp[99];
            } else {
                exp[a] = exp[a - 1];
            }
        }

        for (int a = 105; a < 140; a++) {
            if (a == 105) {
                exp[105] = (long) Math.floor(exp[104] * 1.065);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.065);
            }
        }

        for (int a = 140; a < 170; a++) {
            if (a == 140) {
                exp[140] = (long) Math.floor(exp[139] * 1.0625);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.0625);
            }
        }

        for (int a = 170; a < 200; a++) {
            if (a == 170) {
                exp[170] = 138750435L;
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.05);
            }
        }

        for (int a = 200; a < 210; a++) {
            if (a == 200 || a == 201) {
                exp[200] = 2207026470L;
                exp[201] = (long) Math.floor(exp[200] * 1.12);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.12);
            }
        }

        for (int a = 210; a < 215; a++) {
            if (a == 210 || a == 211) {
                exp[210] = (long) Math.floor(exp[209] * 1.6);
                exp[211] = (long) Math.floor(exp[210] * 1.11);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.11);
            }
        }

        for (int a = 215; a < 220; a++) {
            if (a == 215 || a == 211) {
                exp[215] = (long) Math.floor(exp[214] * 1.3);
                exp[211] = (long) Math.floor(exp[210] * 1.09);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.09);
            }
        }

        for (int a = 220; a < 225; a++) {
            if (a == 220 || a == 221) {
                exp[220] = (long) Math.floor(exp[219] * 1.6);
                exp[221] = (long) Math.floor(exp[220] * 1.07);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.07);
            }
        }
        for (int a = 225; a < 230; a++) {
            if (a == 225 || a == 226) {
                exp[225] = (long) Math.floor(exp[224] * 1.3);
                exp[226] = (long) Math.floor(exp[225] * 1.05);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.05);
            }
        }
        for (int a = 230; a < 235; a++) {
            if (a == 230) {
                exp[230] = (long) Math.floor(exp[229] * 1.6);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.03);
            }
        }
        for (int a = 235; a < 240; a++) {
            if (a == 235) {
                exp[235] = (long) Math.floor(exp[234] * 1.3);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.03);
            }
        }
        for (int a = 240; a < 245; a++) {
            if (a == 240) {
                exp[240] = (long) Math.floor(exp[239] * 1.6);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.03);
            }
        }
        for (int a = 245; a < 250; a++) {
            if (a == 245) {
                exp[245] = (long) Math.floor(exp[244] * 1.3);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.03);
            }
        }
        for (int a = 250; a < 260; a++) {
            if (a == 250) {
                exp[250] = 1313764762354L;
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.01);
            }
        }

        for (int a = 260; a < 275; a++) {
            if (a == 260 || a == 270) {
                exp[a] = (long) Math.floor((exp[a - 1] * 1.01) * 2);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.01);
            }
        }

        for (int a = 275; a < 280; a++) {
            if (a == 275) {
                exp[a] = (long) Math.floor(exp[a - 1] * 2.02);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.1);
            }
        }
        for (int a = 280; a < 285; a++) {
            if (a == 280) {
                exp[a] = (long) Math.floor(exp[a - 1] * 2.02);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.1);
            }
        }
        for (int a = 285; a < 290; a++) {
            if (a == 285) {
                exp[a] = (long) Math.floor(exp[a - 1] * 2.02);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.1);
            }
        }
        for (int a = 290; a < 295; a++) {
            if (a == 290) {
                exp[a] = (long) Math.floor(exp[a - 1] * 2.02);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.1);
            }
        }
        for (int a = 295; a < 300; a++) {
            if (a == 295) {
                exp[a] = (long) Math.floor(exp[a - 1] * 2.02);
            } else if (a == 299) {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.5);
            } else {
                exp[a] = (long) Math.floor(exp[a - 1] * 1.1);
            }
        }
        for (int a = 300; a < 999; a++) {
            exp[a] = (long) Math.floor(exp[a - 1]);
        }
        exp[999] = 0;
    }

    static {
        // 추가 장비
        CustomItem ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "호그의 이빨"); //0
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 5);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 3);
        customItems.add(ci);

        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "루칸의 등껍질"); //1
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 10);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 6);
        customItems.add(ci);

        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "골드리치의 이어링"); //2
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 15);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 9);
        customItems.add(ci);

        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "어둠의 보석"); //3
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 20);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 12);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "이계의 보석"); //4
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 25);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 15);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "전설의 증표"); //5
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 30);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 18);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "정복자의 영광"); //6
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 35);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 21);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "초월자의 이어링"); //7
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 40);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 24);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.보조장비, "정복자의 펜던트"); //8
        ci.addEffects(CustomItem.CustomItemEffect.CrD, 10);
        ci.addEffects(CustomItem.CustomItemEffect.BdR, 30);
        ci.addEffects(CustomItem.CustomItemEffect.AllStatR, 30);
        ci.addEffects(CustomItem.CustomItemEffect.MesoR, 20);
        ci.addEffects(CustomItem.CustomItemEffect.DropR, 20);
        customItems.add(ci);

        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.각인석, "흔한 각인석"); //9
        ci.addEffects(CustomItem.CustomItemEffect.CrD, 8);
        ci.addEffects(CustomItem.CustomItemEffect.DropR, 10);
        customItems.add(ci);

        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.각인석, "희귀한 각인석"); //10
        ci.addEffects(CustomItem.CustomItemEffect.CrD, 16);
        ci.addEffects(CustomItem.CustomItemEffect.DropR, 20);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.각인석, "레전더리 각인석"); //11
        ci.addEffects(CustomItem.CustomItemEffect.CrD, 24);
        ci.addEffects(CustomItem.CustomItemEffect.DropR, 30);
        customItems.add(ci);
        
        ci = new CustomItem(customItems.size(), CustomItem.CustomItemType.각인석, "에픽 각인석"); //12
        ci.addEffects(CustomItem.CustomItemEffect.CrD, 40);
        ci.addEffects(CustomItem.CustomItemEffect.DropR, 50);
        customItems.add(ci);
    }

    public static long getExpNeededForLevel(final int level) {
        return exp[level];
    }

    public static int getGuildExpNeededForLevel(final int level) {
        if (level < 0 || level >= guildexp.length) {
            return Integer.MAX_VALUE;
        }
        return guildexp[level];
    }

    public static int getPVPExpNeededForLevel(final int level) {
        if (level < 0 || level >= pvpExp.length) {
            return Integer.MAX_VALUE;
        }
        return pvpExp[level];
    }

    public static int getClosenessNeededForLevel(final int level) {
        return closeness[level - 1];
    }

    public static int getMountExpNeededForLevel(final int level) {
        return mountexp[level - 1];
    }

    public static int getTraitExpNeededForLevel(final int level) {
        if (level < 0 || level >= cumulativeTraitExp.length) {
            return Integer.MAX_VALUE;
        }
        return cumulativeTraitExp[level];
    }

    public static int getSetExpNeededForLevel(final int level) {
        if (level < 0 || level >= setScore.length) {
            return Integer.MAX_VALUE;
        }
        return setScore[level];
    }

    public static int getMonsterHP(final int level) {
        if (level < 0 || level >= mobHpVal.length) {
            return Integer.MAX_VALUE;
        }
        return mobHpVal[level];
    }

    public static int getBookLevel(final int level) {
        return (int) ((5 * level) * (level + 1));
    }

    public static int getTimelessRequiredEXP(final int level) {
        return 70 + (level * 10);
    }

    public static int getReverseRequiredEXP(final int level) {
        return 60 + (level * 5);
    }

    public static int getProfessionEXP(final int level) {
        return ((100 * level * level) + (level * 400)) / 2;
    }

    public static boolean isHarvesting(final int itemId) {
        return itemId >= 1500000 && itemId < 1520000;
    }

    public static final double maxViewRangeSq() {
        return /*1000 * 1000;*/ Double.POSITIVE_INFINITY; // 800 * 800
    }

    public static int maxViewRangeSq_Half() {
        return 500000; // 800 * 800
    }

    public static boolean isJobFamily(final int baseJob, final int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isGM(final int job) {
        return job == 800 || job == 900;
    }

    public static boolean isKOC(final int job) {
        return job >= 1000 && job < 2000;
    }

    public static final boolean isNightWalker(final int job) {
        return job == 1400 || (job >= 1400 && job <= 1412);
    }

    public static boolean isEvan(final int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

    public static boolean isMichael(final int job) {
        return job == 5000 || (job >= 5000 && job <= 5112);
    }

    public static boolean isMercedes(final int job) {
        return job == 2002 || (job >= 2300 && job <= 2312);
    }

    public static boolean isEunWol(int job) {
        return job == 2005 || (job >= 2500 && job <= 2512);
    }

    public static boolean isKinesis(int job) {
        return job == 14000 || (job >= 14200 && job <= 14212);
    }

    public static boolean isDemonSlayer(final int job) {
        return job == 3001 || (job >= 3100 && job <= 3112);
    }

    public static boolean isDemonAvenger(int job) {
        return job == 3101 || (job >= 3120 && job <= 3122);
    }

    public static final boolean isArk(final int job) {
        return job == 15001 || (job >= 15500 && job <= 15512);
    }

    public static final boolean isHoyeong(final int job) {
        return job == 16000 || (job >= 16400 && job <= 16412);
    }

    public static final boolean isKain(final int job) {
        return job == 6003 || (job >= 6300 && job <= 6312);
    }

    public static final boolean isAdel(final int job) {
        return job == 15002 || (job >= 15100 && job <= 15112);
    }

    public static boolean isXenon(final int job) {
        return job == 3002 || (job >= 3600 && job <= 3612);
    }

    public static boolean isAngelicBuster(final int job) {
        return job == 6001 || (job >= 6500 && job <= 6512);
    }

    public static boolean isAran(final int job) {
        return job == 2000 || (job >= 2100 && job <= 2112);
    }

    public static boolean isKadena(final int job) {
        return job == 6002 || (job >= 6400 && job <= 6412);
    }

    public static boolean isArc(final int job) {
        return job == 15001 || (job >= 15500 && job <= 15512);
    }

    public static boolean isAdele(final int job) {
        return job == 15002 || (job >= 15100 && job <= 15112);
    }

    public static boolean isIllium(final int job) {
        return job == 15000 || (job >= 15200 && job <= 15212);
    }

    public static boolean isResist(final int job) {
        return job >= 3000 && job <= 3712;
    }

    public static boolean isAdventurer(final int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isPhantom(final int job) {
        return job == 2003 || (job / 100 == 24);
    }

    public static boolean isBattleMage(final int job) {
        return job >= 3200 && job <= 3212;
    }

    public static boolean isWildHunter(final int job) {
        return job >= 3300 && job <= 3312;
    }

    public static boolean isMechanic(final int job) {
        return job >= 3500 && job <= 3512;
    }

    public static boolean isLuminous(int job) {
        return job == 2004 || (job >= 2700 && job <= 2712);
    }

    public static boolean isKaiser(final int job) {
        return job == 6000 || (job >= 6100 && job <= 6112);
    }

    public static boolean isSoulMaster(final int job) {
        return job >= 1100 && job <= 1112;
    }

    public static boolean isFlameWizard(final int job) {
        return job >= 1200 && job <= 1212;
    }

    public static boolean isWindBreaker(final int job) {
        return job >= 1300 && job <= 1312;
    }

    public static boolean isStriker(final int job) {
        return job >= 1500 && job <= 1512;
    }

    public static boolean isZero(int job) {
        return job == 10000 || (job >= 10100 && job <= 10112);
    }

    public static boolean isPathFinder(int job) {
        return job == 301 || (job >= 330 && job <= 332);
    }

    public static boolean JobCodeCheck(final int firstjob, final int secondjob) {
        // 모험가 0
        if (GameConstants.isHero(firstjob) && GameConstants.isHero(secondjob)) {
            return true;
        }
        if (GameConstants.isPaladin(firstjob) && GameConstants.isPaladin(secondjob)) {
            return true;
        }
        if (GameConstants.isDarkKnight(firstjob) && GameConstants.isDarkKnight(secondjob)) {
            return true;
        }
        if (GameConstants.isFPMage(firstjob) && GameConstants.isFPMage(secondjob)) {
            return true;
        }
        if (GameConstants.isILMage(firstjob) && GameConstants.isILMage(secondjob)) {
            return true;
        }
        if (GameConstants.isBishop(firstjob) && GameConstants.isBishop(secondjob)) {
            return true;
        }
        if (GameConstants.isPathFinder(firstjob) && GameConstants.isPathFinder(secondjob)) {
            return true;
        }
        if (GameConstants.isBowMaster(firstjob) && GameConstants.isBowMaster(secondjob)) {
            return true;
        }
        if (GameConstants.isMarksMan(firstjob) && GameConstants.isMarksMan(secondjob)) {
            return true;
        }
        if (GameConstants.isNightLord(firstjob) && GameConstants.isNightLord(secondjob)) {
            return true;
        }
        if (GameConstants.isShadower(firstjob) && GameConstants.isShadower(secondjob)) {
            return true;
        }
        if (GameConstants.isDualBlade(firstjob) && GameConstants.isDualBlade(secondjob)) {
            return true;
        }
        if (GameConstants.isViper(firstjob) && GameConstants.isViper(secondjob)) {
            return true;
        }
        if (GameConstants.isCaptain(firstjob) && GameConstants.isCaptain(secondjob)) {
            return true;
        }
        if (GameConstants.isCannon(firstjob) && GameConstants.isCannon(secondjob)) {
            return true;
        }

        // 시그너스 1000
        if (GameConstants.isSoulMaster(firstjob) && GameConstants.isSoulMaster(secondjob)) {
            return true;
        }
        if (GameConstants.isFlameWizard(firstjob) && GameConstants.isFlameWizard(secondjob)) {
            return true;
        }
        if (GameConstants.isWindBreaker(firstjob) && GameConstants.isWindBreaker(secondjob)) {
            return true;
        }
        if (GameConstants.isNightWalker(firstjob) && GameConstants.isNightWalker(secondjob)) {
            return true;
        }
        if (GameConstants.isStriker(firstjob) && GameConstants.isStriker(secondjob)) {
            return true;
        }

        // 영웅 2000
        if (GameConstants.isAran(firstjob) && GameConstants.isAran(secondjob)) {
            return true;
        }
        if (GameConstants.isMercedes(firstjob) && GameConstants.isMercedes(secondjob)) {
            return true;
        }
        if (GameConstants.isEvan(firstjob) && GameConstants.isEvan(secondjob)) {
            return true;
        }
        if (GameConstants.isPhantom(firstjob) && GameConstants.isPhantom(secondjob)) {
            return true;
        }
        if (GameConstants.isEunWol(firstjob) && GameConstants.isEunWol(secondjob)) {
            return true;
        }
        if (GameConstants.isLuminous(firstjob) && GameConstants.isLuminous(secondjob)) {
            return true;
        }

        // 레지스탕스 3000
        if (GameConstants.isBlaster(firstjob) && GameConstants.isBlaster(secondjob)) {
            return true;
        }
        if (GameConstants.isMechanic(firstjob) && GameConstants.isMechanic(secondjob)) {
            return true;
        }
        if (GameConstants.isBattleMage(firstjob) && GameConstants.isBattleMage(secondjob)) {
            return true;
        }
        if (GameConstants.isWildHunter(firstjob) && GameConstants.isWildHunter(secondjob)) {
            return true;
        }
        if (GameConstants.isDemonSlayer(firstjob) && GameConstants.isDemonSlayer(secondjob)) {
            return true;
        }
        if (GameConstants.isDemonAvenger(firstjob) && GameConstants.isDemonAvenger(secondjob)) {
            return true;
        }
        if (GameConstants.isXenon(firstjob) && GameConstants.isXenon(secondjob)) {
            return true;
        }

        // 미하일 5000
        if (GameConstants.isMichael(firstjob) && GameConstants.isMichael(secondjob)) {
            return true;
        }

        // 노바 6000
        if (GameConstants.isKaiser(firstjob) && GameConstants.isKaiser(secondjob)) {
            return true;
        }
        if (GameConstants.isKadena(firstjob) && GameConstants.isKadena(secondjob)) {
            return true;
        }
        if (GameConstants.isAngelicBuster(firstjob) && GameConstants.isAngelicBuster(secondjob)) {
            return true;
        }

        if (GameConstants.isKain(firstjob) && GameConstants.isKain(secondjob)) {
            return true;
        }

        // 제로 10000
        if (GameConstants.isZero(firstjob) && GameConstants.isZero(secondjob)) {
            return true;
        }

        // 키네시스 10000
        if (GameConstants.isKinesis(firstjob) && GameConstants.isKinesis(secondjob)) {
            return true;
        }

        // 레프 15000
        if (GameConstants.isIllium(firstjob) && GameConstants.isIllium(secondjob)) {
            return true;
        }
        if (GameConstants.isArc(firstjob) && GameConstants.isArc(secondjob)) {
            return true;
        }
        if (GameConstants.isHoyeong(firstjob) && GameConstants.isHoyeong(secondjob)) {
            return true;
        }
        if (GameConstants.isAdel(firstjob) && GameConstants.isAdel(secondjob)) {
            return true;
        }
        if (GameConstants.isLala(firstjob) && GameConstants.isLala(secondjob)) {
            return true;
        }
        return false;
    }

    public static boolean isFourthJob(int job) {
        switch (job) {
            case 112:
            case 122:
            case 132:
            case 212:
            case 222:
            case 232:
            case 312:
            case 322:
            case 412:
            case 422:
            case 512:
            case 522:
                return true;
        }
        return false;
    }

    public static boolean isRecoveryIncSkill(final int id) {
        switch (id) {
            case 1110000:
            case 2000000:
            case 1210000:
            case 11110000:
            case 4100002:
            case 4200001:
                return true;
        }
        return false;
    }

    public static boolean isFusionSkill(int skill) {

        switch (skill) {
            case 22110014:
            case 22110024:
            case 22110025:
            case 22140014:
            case 22140015:
            case 22140023:
            case 22140024:
            case 22170065:
            case 22170066:
            case 22170067:
            case 400020046:
            case 400041058:
                return true;
        }
        return false;
    }

    public static boolean isAngelicBlessSkill(Skill skill) {
        if (!skill.isVMatrix()) {
            switch (skill.getId() % 10000) {
                case 1085: //엔젤릭 블레스
                case 1087: //다크 엔젤릭 블레스
                case 1090: //눈꽃 엔젤릭 블레스
                case 1179: //화이트 엔젤릭 블레스
                    return true;
            }
        }
        return false;
    }

    public static boolean isSaintSaverSkill(int skill) {
        switch (skill) {
            case 80001034:
            case 80001035:
            case 80001036:
                return true;
        }
        return false;
    }

    public static boolean isLinkedSkill(final int id) {
        return getLinkedSkill(id) != id;
    }

    public static int getLinkedSkill(final int id) {
        switch (id) {
            case 5310011:
                return 5311010;
            case 400021131:
                return 400021130;
            case 162111010:
                return 162110002;
            case 162101009:
            case 162101010:
            case 162101011:
            case 162121017:
            case 162121018:
            case 162121019:
                return 162100008;
            case 162101003:
            case 162101004:
            case 162121012:
            case 162121013:
            case 162121014:
                return 162100002;
            case 162101006:
            case 162101007:
            case 162121015:
            case 162121016:
                return 162100005;
            case 162121003:
                return 162120002;
            case 162121006:
                return 162120005;
            case 162121009:
            case 162121010:
                return 162120008;
            case 400041060:
                return 400041059;
            case 400031047:
            case 400031049:
            case 400031051:
                return 400031057;
            case 13120003:
            case 13110022:
                return 13101022;
            case 23111009:
            case 23111010:
                return 23111008;
            case 5120021:
                return 5121013;
            case 25111211:
                return 25111209;
            case 400031031:
                return 400031030;
            case 30001078:
            case 30001079:
            case 30001080:
                return 30001068;
            case 61121026:
                return 61121102;
            case 400001040:
            case 400001041:
                return 400001039;
            case 400001060:
                return 400001059;
            case 400041051:
                return 400041050;
            case 400001044:
                return 400001043;
            case 151101004:
            case 151101010:
                return 151101003;
            case 131001001:
            case 131001002:
            case 131001003:
                return 131001000;
            case 131001106:
            case 131001206:
            case 131001306:
            case 131001406:
            case 131001506:
                return 131001006;
            case 131001107:
            case 131001207:
            case 131001307:
                return 131001007;
            case 24121010:
                return 24121003;
            case 24111008:
                return 24111006;
            case 151101007:
            case 151101008:
                return 151101006;
            case 142120001:
                return 142120000;
            case 142110003:
                return 142111002;
            case 400041049:
                return 400041048;
            case 400041053:
                return 400041052;
            case 37000009:
                return 37001001;
            case 37100008:
                return 37100007;
            case 151001003:
                return 151001002;
            case 400001051:
            case 400001053:
            case 400001054:
            case 400001055:
                return 400001050;
            case 95001000:
                return 3111013;
            case 400031018:
            case 400031019:
                return 400031017;
            case 164111016:
                return 164111003;
            case 164111001:
            case 164111002:
            case 164111009:
            case 164111010:
            case 164111011:
                return 164110000;
            case 400051048:
                return 400051047;
            case 400001047:
            case 400001048:
            case 400001049:
                return 400001046;
            case 164001002:
                return 164001001;
            case 151121011:
                return 151121004;
            case 164101001:
            case 164101002:
                return 164100000;
            case 164101004:
                return 164101003;
            case 164121001:
            case 164121002:
            case 164121014:
                return 164120000;
            case 164121004:
                return 164121003;
            case 164121015:
                return 164121008;
            case 164120007: // 이게 나비
                return 164121007;
            case 164121044:
                return 164121043;
            case 164121011:
            case 164121012:
                return 164121006;
            case 164111004:
            case 164111005:
            case 164111006:
                return 164111003;
            case 400031035:
                return 400031034;
            case 400031038:
            case 400031039:
            case 400031040:
            case 400031041:
            case 400031042:
            case 400031043:
                return 400031037;
            case 31011004:
            case 31011005:
            case 31011006:
            case 31011007:
                return 31011000;
            case 31201007:
            case 31201008:
            case 31201009:
            case 31201010:
                return 31201000;
            case 31211007:
            case 31211008:
            case 31211009:
            case 31211010:
                return 31211000;
            case 31221009:
            case 31221010:
            case 31221011:
            case 31221012:
                return 31221000;
            case 3311011:
                return 3311010;
            case 3011006:
            case 3011007:
            case 3011008:
                return 3011005;
            case 3301009:
                return 3301008;
            case 3301004:
                return 3301003;
            case 3321003:
            case 3321004:
            case 3321005:
            case 3321006:
            case 3321007:
                return 3320002;
            case 3321036:
            case 3321037:
            case 3321038:
            case 3321039:
            case 3321040:
                return 3321035;
            case 3321016:
            case 3321017:
            case 3321018:
            case 3321019:
            case 3321020:
            case 3321021:
                return 3321014;
            case 21000004:
                return 21001009;
            case 142100010:
                return 142101009;
            case 142100008:
                return 142101002;
            case 27120211:
                return 27121201;
            case 33121255:
                return 33121155;
            case 400041024:
                return 400041022;
            case 33100016:
            case 33101215:
                return 33101115;
            case 37000005: // 익스플로젼 무브
                return 37001004;
            case 400011074:
            case 400011075:
            case 400011076:
                return 400011073;
            case 33001202:
                return 33001102;
            case 152000009:
                return 152000007;
            case 152001005:
                return 152001004;
            case 152120002:
                return 152120001;
            case 152101000:
            case 152101004:
                return 152101003;
            case 152121006:
                return 152121005;
            case 400051019:
            case 400051020:
                return 400051018;
            case 152110004:
            case 152120016:
            case 152120017:
                return 152001001;
//            case 65121007:
            //          case 65121008:
            //        	return 65121101;
            case 37120055:
            case 37120056:
            case 37120057:
            case 37120058:
            case 37120059:
                return 37121052;
            case 400021064:
            case 400021065:
                return 400021063;
            case 1100012:
                return 1101012;
            case 1111014:
                return 1111008;
            case 2100010:
                return 2101010;
            case 61111221:
            case 61111114:
                return 61111008;
            case 14121055:
            case 14121056:
                return 14121054;
            case 61121220:
                return 61121015;
            case 400031008:
            case 400031009:
                return 400031007;
            case 142120030:
                return 142121030;
            case 400051039:
            case 400051052:
            case 400051053:
                return 400051038;
            case 400021043:
            case 400021044:
            case 400021045:
                return 400021042;
            case 400051049:
            case 400051050:
                return 400051040;
            case 400040006:
                return 400041006;
            case 155001204:
                return 155001104;
            case 61121222:
                return 61121105;
            case 400021013:
            case 400021014:
            case 400021015:
            case 400021016:
            case 400020046:
            case 400020051:
                return 400021012;
            case 61121124:
            case 61121221:
            case 61121223:
            case 61121225:
            case 61121116:
                return 61121104;
            case 400011002:
                return 400011001;
            case 400010030:
                return 400011031;
            // case 80002634:
            //      return 80002633;
            case 400051051:
                return 400051041;
            case 400021077:
                return 400021070;
            case 2120013:
                return 2121007;
            case 2220014:
                return 2221007;
            case 32121011:
                return 32121004;
            case 400011059:
            case 400011060:
            case 400011061:
                return 400011058;
            case 400021075:
            case 400021076:
                return 400021074;
            case 400011033:
            case 400011034:
            case 400011035:
            case 400011036:
            case 400011037:
            case 400011067:
                return 400011032;
            case 400011080:
            case 400011081:
            case 400011082:
                return 400011079;
            case 400011084:
                return 400011083;
            case 21120026:
                return 21120019;
            case 400020009:
            case 400020010:
            case 400020011:
            case 400021010:
            case 400021011:
                return 400021008;
            case 400041026:
            case 400041027:
                return 400041025;
            case 400040008:
            case 400041019:
                return 400041008;
            case 400041003:
            case 400041004:
            case 400041005:
                return 400041002;
            case 400051045:
                return 400051044;
            case 400011078:
                return 400011077;
            case 400031016:
                return 400031015;
            case 400031013:
            case 400031014:
                return 400031012;
            case 400011102:
                return 400011090;
            case 400020002:
                return 400021002;
            case 22140023:
                return 22140014;
            case 22140024:
                return 22140015;
            case 22141012:
                return 22140022;
            case 22110014:
            case 22110025:
                return 22110014;
            case 22170061:
                return 22170060;
            case 22170093:
                return 22170064;
            case 22171083:
                return 22171080;
            case 22170094:
                return 22170065;
            case 400011069:
                return 400011068;
            case 400031033:
                return 400031032;
            case 25121133:
                return 25121131;
            case 23121015:
                return 23121014;
            case 24120055:
                return 24121052;
            case 31221014:
                return 31221001;
            case 400021031:
            case 400021040:
                return 400021030;
            case 4120019:
                return 4120018;
            case 37000010:
                return 37001001;
            case 155001000:
                return 155001001;
            case 155001009:
                return 155001104;
            case 155100009:
                return 155101008;
            case 155101002:
                return 155101003;
            case 155101013:
            case 155101015:
            case 155101101:
            case 155101112:
                return 155101100;
            case 155101114:
                return 155101104;
            case 155101214:
                return 155101204;
            case 155101201:
            case 155101212:
                return 155101200;
            case 155111002:
            case 155111111:
                return 155111102;
            case 155111103:
            case 155111104:
                return 155111105;
            case 155111106:
                return 155111102;
            case 155111211:
            case 155111212:
                return 155111202;
            case 155121002:
                return 155121102;
            case 155121003:
            case 155121004:
                return 155121005;
            case 155121006:
            case 155121007:
                return 155121008;
            case 155121215:
                return 155121202;
            case 400041010:
            case 400041011:
            case 400041012:
            case 400041013:
            case 400041014:
            case 400041015:
                return 400041009;
            case 400011099:
                return 400011098;
            case 400011101:
                return 400011100;
            case 400011053:
                return 400011052;
            case 400001016:
                return 400001013;
            case 400021029:
                return 400021028;
            case 400030002:
                return 400031002;
            case 400021049:
            case 400021050:
                return 400021041;
            case 14000027:
            case 14000028:
            case 14000029:
                return 14001027;
            case 4100011:
            case 4100012:
                return 4101011;
            case 5211015:
            case 5211016:
                return 5211011;
            case 5220023:
            case 5220024:
            case 5220025:
                return 5221022;
            case 51001006:
            case 51001007:
            case 51001008:
            case 51001009:
            case 51001010:
            case 51001011:
            case 51001012:
            case 51001013:
                return 51001005;
            case 25120115:
                return 25120110;
            case 5201005:
                return 5201011;
            case 5320011:
                return 5321004;
            case 33001008:
            case 33001009:
            case 33001010:
            case 33001011:
            case 33001012:
            case 33001013:
            case 33001014:
            case 33001015:
                return 33001007;
            case 65120011:
                return 65121011;
            case 400041034:
                return 400041033;
            case 400041036:
                return 400041035;
            case 21110027:
            case 21111021:
            case 21110028:
                return 21110020;
            case 100000276:
            case 100000277:
                return 100000267;
            case 400001025:
            case 400001026:
            case 400001027:
            case 400001028:
            case 400001029:
            case 400001030:
                return 400001024;
            case 400001015:
                return 400001014;
            case 400011013:
            case 400011014:
                return 400011012;
            case 400001022:
                return 400001019;
            case 400021033:
            case 400021052:
                return 400021032;
            case 400041016:
                return 4001344;
            case 400041017:
                return 4111010;
            case 400041018:
                return 4121013;
            case 400051003:
            case 400051004:
            case 400051005:
                return 400051002;
            case 400051025:
            case 400051026:
                return 400051024;
            case 400051023:
                return 400051022;
            case 2321055:
                return 2321052;
            case 5121055:
                return 5121052;
            case 61111220:
                return 61111002;
            case 12001027:
            case 12001028:
                return 12000023;
            case 36121013:
            case 36121014:
                return 36121002;
            case 36121011:
            case 36121012:
                return 36121001;
            case 400010010:
                return 400011010;
            case 10001253:
            case 10001254:
            case 14001026:
                return 10000252;
            case 142000006:
                return 142001004;
            case 4321001:
                return 4321000;
            case 33101006:
            case 33101007:
                return 33101005;
            case 33101008:
                return 33101004;
            case 35101009:
            case 35101010:
                return 35100008;
            case 35111009:
            case 35111010:
                return 35111001;
            case 35121013:
                return 35111005;
            case 35121011:
                return 35121009;
            case 3000008:
            case 3000009:
            case 3000010:
                return 3001007;
            case 32001007:
            case 32001008:
            case 32001009:
            case 32001010:
            case 32001011:
                return 32001001;
            case 64001007:
            case 64001008:
            case 64001009:
            case 64001010:
            case 64001011:
            case 64001012:
                return 64001000;
            case 64001013:
                return 64001002;
            case 64100001:
                return 64100000;
            case 64001006:
                return 64001001;
            case 64101008:
                return 64101002;
            case 64111012:
                return 64111004;
            case 64121012:
            case 64121013:
            case 64121014:
            case 64121015:
            case 64121017:
            case 64121018:
            case 64121019:
                return 64121001;
            case 64121022:
            case 64121023:
            case 64121024:
                return 64121021;
            case 64121016:
                return 64121003;
            case 64121055:
                return 64121053;
            case 5300007:
                return 5301001;
            case 23101007:
                return 23101001;
            case 31001006:
            case 31001007:
            case 31001008:
                return 31000004;
            case 30010183:
            case 30010184:
            case 30010186:
                return 30010110;
            case 25000001:
                return 25001000;
            case 25000003:
                return 25001002;
            case 25100001:
            case 25100002:
                return 25101000; //파력권
            case 25100010:
                return 25100009; //여우령
            case 25110001:
            case 25110002:
            case 25110003:
                return 25111000; //통백권
            case 25120001:
            case 25120002:
            case 25120003:
                return 25121000; //폭류권
            case 101000102:
                return 101000101;
            case 101000202:
                return 101000201;
            case 101100202:
                return 101100201;
            case 101110201:
                return 101110200;
            case 101110204:
                return 101110203;
            case 101120101:
                return 101120100;
            case 101120103:
                return 101120102;
            case 101120105:
            case 101120106:
                return 101120104;
            case 101120203:
                return 101120202;
            case 400031021:
                return 400031020;
            case 101120205:
            case 101120206:
                return 101120204;
            case 101120200:
                return 101121200;
            case 100001269:
            case 100001266:
                return 100001265;
            case 1111002:
                return 1101013; //콤보 어택
            case 3120019:
                return 3111009; //폭풍의 시
            case 5201013:
            case 5201014:
                return 5201012; //서먼 크루
            case 5210016:
            case 5210017:
            case 5210018:
                return 5210015; //어셈블 크루
            case 11121055:
                return 11121052; //크로스 더 스틱스
            case 12120011:
                return 12121001; //블레이징 익스팅션
            case 12121055:
                return 12121054; //드래곤 슬레이브
            case 12120013:
            case 12120014:
                return 12121004; //스피릿 오브 플레임
            case 14111023:
                return 14111022; // 스타더스트
//            case 21121013:
            //              return 21120005; //파이널 블로우
            case 61110211:
            case 61120007:
            case 61121217:
                return 61101002; //윌오브소드
            case 61120008:
                return 61111008; //파이널 피규레이션
            case 61121201:
                return 61121100; //기가슬래셔
            case 65111007:
                return 65111100; //소울시커
            case 36111009:
            case 36111010:
                return 36111000;
            case 63001003:
            case 63001005:
                return 63001002;
            case 63121102:
            case 63121103:
                return 63120102;
            case 400001011:
                return 400001010;
            case 63101104:
                return 63101004;
        }

        if (id == 155101204) {
            return 155101104;
        }
        return id;
    }

    public final static boolean isDemonSlash(int skillid) {
        switch (skillid) {
            case 31000004:
            case 31001006:
            case 31001007:
            case 31001008:
            case 31100007:
            case 31110010:
            case 31120011:
                return true;
        }
        return false;
    }

    public final static boolean isAfterCooltimeSkill(int skillid) {
        switch (skillid) {
            case 2221052:
            case 2301002:
            case 4001003:
            case 12111023:
            case 12121054:
            case 14001023:
            case 20031205:
            case 24111002:
            case 24121005:
            case 25111005:
            case 142121005:
            case 400011038:
            case 400021006:
            case 400021072:
            case 400041007:
            case 151101006:
            case 151101007:
            case 151101008:
            case 151101009:
            case 400001011:
                return true;
        }
        return false;
    }

    public final static boolean isForceIncrease(int skillid) {
        if (isDemonSlash(skillid)) {
            return true;
        }
        switch (skillid) {
            case 400011007:
            case 400011008:
            case 400011009:
            case 400011018:
                return true;
        }
        return false;
    }

    public static int getBOF_ForJob(final int job) {
        return PlayerStats.getSkillByJob(12, job);
    }

    public static int getEmpress_ForJob(final int job) {
        return PlayerStats.getSkillByJob(73, job);
    }

    public static boolean isElementAmp_Skill(final int skill) {
        switch (skill) {
            case 2110001:
            case 2210001:
            case 12110001:
            case 22150000:
                return true;
        }
        return false;
    }

    public static int getJobShortValue(int job) {
        if (job >= 1000) {
            job -= (job / 1000) * 1000;
        }
        job /= 100;
        if (job == 4) { // For some reason dagger/ claw is 8.. IDK
            job *= 2;
        } else if (job == 3) {
            job += 1;
        } else if (job == 5) {
            job += 11; // 16
        }
        return job;
    }

    public static boolean isPyramidSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && skill % 10000 == 1020;
    }

    public static boolean isInflationSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1092 || skill % 10000 == 1094 || skill % 10000 == 1095);
    }

    public static boolean isMulungSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1009 || skill % 10000 == 1010 || skill % 10000 == 1011);
    }

    public static boolean isIceKnightSkill(final int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1098 || skill % 10000 == 99 || skill % 10000 == 100 || skill % 10000 == 103 || skill % 10000 == 104 || skill % 10000 == 1105);
    }

    public static boolean isThrowingStar(final int itemId) {
        return itemId / 10000 == 207;
    }

    public static boolean isArcaneSymbol(final int itemid) {
        return itemid >= 1712000 && itemid <= 1712006;
    }

    public static boolean isAscenticSymbol(final int itemid) {
        return itemid >= 1713000 && itemid <= 1713002;
    }

    public static boolean isBullet(final int itemId) {
        return itemId / 10000 == 233;
    }

    public static boolean isRechargable(final int itemId) {
        return isThrowingStar(itemId) || isBullet(itemId);
    }

    public static boolean isOverall(final int itemId) {
        return itemId / 10000 == 105;
    }

    public static boolean isPet(final int itemId) {
        return itemId / 10000 == 500;
    }

    public static boolean isArrowForCROSSBOW(final int itemId) {
        return itemId >= 2061000 && itemId < 2062000;
    }

    public static boolean isArrowForBow(final int itemId) {
        return itemId >= 2060000 && itemId < 2061000;
    }

    public static boolean isMagicWeapon(final int itemId) {
        final int s = itemId / 10000;
        return s == 137 || s == 138 || s == 121 || s == 126;
    }

    public static boolean isWeapon(final int itemId) {
        return itemId >= 1200000 && itemId < 1600000;
    }

    public static boolean isSecondaryWeapon(final int itemId) {
        return itemId / 10000 == 135 || itemId / 1000 == 1098 || itemId / 1000 == 1099;
    }

    public static boolean isCape(final int itemId) {
        return itemId / 10000 == 109 || itemId / 10000 == 110 || itemId / 10000 == 113;
    }

    public static MapleInventoryType getInventoryTypeOld(final int itemId) {
        final byte type = (byte) (itemId / 1000000);
        if (type < 1 || type > 5) {
            return MapleInventoryType.UNDEFINED;
        }
        return MapleInventoryType.getByType(type);
    }

    public static MapleInventoryType getInventoryType(final int itemId) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte type = (byte) (itemId / 1000000);
        if (type == 1) {
            if (ii.getEquipStats(itemId) != null && ii.getEquipStats(itemId).get("cash") != null) {
                type = 6;
            }
        }
        if (type < 1 || type > 6) {
            return MapleInventoryType.UNDEFINED;
        }
        return MapleInventoryType.getByType(type);
    }

    public static boolean isInBag(final int slot, final byte type) {
        return ((slot >= 256 && slot <= 512) && type == MapleInventoryType.ETC.getType());
    }

    public static MapleWeaponType getWeaponType(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) { // 39, 50, 51 ??
            case 21:
                if (itemId < 1213000) {
                    return MapleWeaponType.PLANE;
                } else {
                    return MapleWeaponType.TUNER;
                }
            case 22:
                return MapleWeaponType.SOULSHOOTER;
            case 23:
                return MapleWeaponType.DESPERADO;
            case 24:
                return MapleWeaponType.ENERGYSWORD;
            case 26:
                return MapleWeaponType.ESPLIMITER;
            case 27:
                return MapleWeaponType.CHAIN;
            case 28:
                return MapleWeaponType.MAGICGUNTLET;
            case 29:
                return MapleWeaponType.FAN;
            case 30:
                return MapleWeaponType.SWORD1H;
            case 31:
                return MapleWeaponType.AXE1H;
            case 32:
                return MapleWeaponType.BLUNT1H;
            case 33:
                return MapleWeaponType.DAGGER;
            case 34:
                return MapleWeaponType.KATARA;
            case 35:
                return MapleWeaponType.MAGIC_ARROW; // can be magic arrow or cards
            case 36:
                return MapleWeaponType.CANE;
            case 37:
                return MapleWeaponType.WAND;
            case 38:
                return MapleWeaponType.STAFF;
            case 40:
                return MapleWeaponType.SWORD2H;
            case 41:
                return MapleWeaponType.AXE2H;
            case 42:
                return MapleWeaponType.BLUNT2H;
            case 43:
                return MapleWeaponType.SPEAR;
            case 44:
                return MapleWeaponType.POLE_ARM;
            case 45:
                return MapleWeaponType.BOW;
            case 46:
                return MapleWeaponType.CROSSBOW;
            case 47:
                return MapleWeaponType.CLAW;
            case 48:
                return MapleWeaponType.KNUCKLE;
            case 49:
                return MapleWeaponType.GUN;
            case 52:
                return MapleWeaponType.DUAL_BOW;
            case 53:
                return MapleWeaponType.HANDCANNON;
            case 56:
                return MapleWeaponType.BIG_SWORD;
            case 57:
                return MapleWeaponType.LONG_SWORD;
            case 58:
                return MapleWeaponType.GUNTLETREVOLVER;
            case 59:
                return MapleWeaponType.ACIENTBOW;
        }
        return MapleWeaponType.NOT_A_WEAPON;
    }

    public static boolean isShield(final int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        return cat == 9;
    }

    public static boolean isEquip(final int itemId) {
        return itemId / 1000000 == 1;
    }

    public static boolean isCleanSlate(int itemId) {
        return itemId / 100 == 20490;
    }

    public static boolean isAccessoryScroll(int itemId) {
        return itemId / 100 == 20492;
    }

    public static boolean isChaosScroll(int itemId) {
        if (itemId >= 2049105 && itemId <= 2049110) {
            return false;
        }
        return itemId / 100 == 20491 || itemId == 2040126;
    }

    public static int getChaosNumber(int itemId) {
        return itemId == 2049153 ? 6 : 5; //놀긍혼 긍혼 스탯 조정 칸주문서
    }

    public static boolean isEquipScroll(int scrollId) {
        return scrollId / 100 == 20493;
    }

    public static boolean isAlphaWeapon(int itemId) {
        return itemId / 10000 == 157;
    }

    public static boolean isBetaWeapon(int itemId) {
        return itemId / 10000 == 156;
    }

    public static boolean isZeroWeapon(int itemId) {
        return (isAlphaWeapon(itemId) || isBetaWeapon(itemId));
    }

    public static boolean isLuckyScroll(int scrollId) {
        return scrollId / 100 == 20489;
    }

    public static boolean isPotentialScroll(int scrollId) {
        return scrollId / 100 == 20494 || scrollId / 100 == 20497 || scrollId == 5534000;
    }

    public static boolean isProstyScroll(int scrollId) {
        switch (scrollId) {
            case 2046964:
            case 2046965:
            case 2047801:
            case 2047914:
            case 2047915:
            case 2046841:
            case 2046842:
            case 2046967:
            case 2046971:
            case 2047803:
            case 2047917:
                return true;
        }
        return false;
    }

    public static boolean isEightRockScroll(int scrollId) { //8樂 주문서
        return scrollId / 1000 == 2046;
    }

    public static boolean isRebirthFireScroll(int scrollId) {
        return scrollId / 100 == 20487;
    }

    public static boolean isSpecialScroll(final int scrollId) {
        switch (scrollId) {
            case 2040727: // Spikes on show
            case 2041058: // Cape for Cold protection
            case 2530000:
            case 2530001:
            case 2531000:
            case 2532000:
            case 2533000:
            case 2720000:
            case 2720001:
            case 5063000:
            case 5064000:
            case 5064100:
            case 5064200:
            case 5064300:
            case 5064400:
                return true;
        }
        return false;
    }

    public static boolean isTwoHanded(final int itemId) {
        switch (getWeaponType(itemId)) {
            case SWORD2H:
            case AXE2H:
            case BLUNT2H:
            case SPEAR:
            case POLE_ARM:
            case BIG_SWORD:
            case LONG_SWORD:
            case GUNTLETREVOLVER:
            case BOW:
            case CROSSBOW:
            case DUAL_BOW:
            case CLAW:
            case GUN:
            case KNUCKLE:
            case HANDCANNON:
            case ACIENTBOW:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSpecialShield(final int itemid) {
        return itemid / 1000 == 1098 || itemid / 1000 == 1099 || itemid / 10000 == 135;
    }

    public static boolean isTownScroll(final int id) {
        return id >= 2030000 && id < 2040000;
    }

    public static boolean isUpgradeScroll(final int id) {
        return id >= 2040000 && id < 2050000;
    }

    public static boolean isGun(final int id) {
        return id >= 1492000 && id < 1500000;
    }

    public static boolean isUse(final int id) {
        return id >= 2000000 && id < 3000000;
    }

    public static boolean isSummonSack(final int id) {
        return id / 10000 == 210;
    }

    public static boolean isMonsterCard(final int id) {
        return id / 10000 == 238;
    }

    public static boolean isSpecialCard(final int id) {
        return id / 1000 >= 2388;
    }

    public static int getCardShortId(final int id) {
        return id % 10000;
    }

    public static boolean isGem(final int id) {
        return id >= 4250000 && id <= 4251402;
    }

    public static boolean isOtherGem(final int id) {
        switch (id) {
            case 4001174:
            case 4001175:
            case 4001176:
            case 4001177:
            case 4001178:
            case 4001179:
            case 4001180:
            case 4001181:
            case 4001182:
            case 4001183:
            case 4001184:
            case 4001185:
            case 4001186:
            case 4031980:
            case 2041058:
            case 2040727:
            case 1032062:
            case 4032334:
            case 4032312:
            case 1142156:
            case 1142157:
                return true; //mostly quest items
        }
        return false;
    }

    public static boolean isCustomQuest(final int id) {
        return id > 99999 && id != 100825 && id != 100879;
    }

    public static long getTaxAmount(final long meso) {
        if (meso >= 100000000) {
            return Math.round(0.06 * meso);
        } else if (meso >= 25000000) {
            return Math.round(0.05 * meso);
        } else if (meso >= 10000000) {
            return Math.round(0.04 * meso);
        } else if (meso >= 5000000) {
            return Math.round(0.03 * meso);
        } else if (meso >= 1000000) {
            return Math.round(0.018 * meso);
        } else if (meso >= 100000) {
            return Math.round(0.008 * meso);
        }
        return 0;
    }

    public static int EntrustedStoreTax(final long theQuantity) {
        if (theQuantity >= 100000000) {
            return (int) Math.round(0.03 * theQuantity);
        } else if (theQuantity >= 25000000) {
            return (int) Math.round(0.025 * theQuantity);
        } else if (theQuantity >= 10000000) {
            return (int) Math.round(0.02 * theQuantity);
        } else if (theQuantity >= 5000000) {
            return (int) Math.round(0.015 * theQuantity);
        } else if (theQuantity >= 1000000) {
            return (int) Math.round(0.009 * theQuantity);
        } else if (theQuantity >= 100000) {
            return (int) Math.round(0.004 * theQuantity);
        }
        return 0;
    }

    public static int getAttackDelay(final int id, final Skill skill) {
        switch (id) { // Assume it's faster(2)
            case 3121004: // Storm of Arrow
            case 23121000:
            case 33121009:
            case 13111002: // Storm of Arrow
            case 5221004: // Rapidfire
            case 5201006: // Recoil shot/ Back stab shot
            case 35121005:
            case 35111004:
            case 35121013:
                return 40; //reason being you can spam with final assaulter
            case 14111005:
            case 4121007:
            case 5221007:
                return 99; //skip duh chek
            case 0: // Normal Attack, TODO delay for each weapon type
                return 570;
        }
        if (skill != null && skill.getSkillType() == 3) {
            return 0; //final attack
        }
        if (skill != null && skill.getDelay() > 0 && !isNoDelaySkill(id)) {
            return skill.getDelay();
        }
        // TODO delay for final attack, weapon type, swing,stab etc
        return 330; // Default usually
    }

    public static byte gachaponRareItem(final int id) {
        switch (id) {
            case 2340000: // White Scroll
            case 2049100: // Chaos Scroll
            case 2049000: // Reverse Scroll
            case 2049001: // Reverse Scroll
            case 2049002: // Reverse Scroll
            case 2040006: // Miracle
            case 2040007: // Miracle
            case 2040303: // Miracle
            case 2040403: // Miracle
            case 2040506: // Miracle
            case 2040507: // Miracle
            case 2040603: // Miracle
            case 2040709: // Miracle
            case 2040710: // Miracle
            case 2040711: // Miracle
            case 2040806: // Miracle
            case 2040903: // Miracle
            case 2041024: // Miracle
            case 2041025: // Miracle
            case 2043003: // Miracle
            case 2043103: // Miracle
            case 2043203: // Miracle
            case 2043303: // Miracle
            case 2043703: // Miracle
            case 2043803: // Miracle
            case 2044003: // Miracle
            case 2044103: // Miracle
            case 2044203: // Miracle
            case 2044303: // Miracle
            case 2044403: // Miracle
            case 2044503: // Miracle
            case 2044603: // Miracle
            case 2044908: // Miracle
            case 2044815: // Miracle
            case 2044019: // Miracle
            case 2044703: // Miracle
                return 2;
            //1 = wedding msg o.o
        }
        return 0;
    }
    public final static int[] goldrewards = {
        2049400, 1,
        2049401, 2,
        2049301, 2,
        2340000, 1, // white scroll
        2070007, 2,
        2070016, 1,
        2330007, 1,
        2070018, 1, // balance fury
        1402037, 1, // Rigbol Sword
        2290096, 1, // Maple Warrior 20
        2290049, 1, // Genesis 30
        2290041, 1, // Meteo 30
        2290047, 1, // Blizzard 30
        2290095, 1, // Smoke 30
        2290017, 1, // Enrage 30
        2290075, 1, // Snipe 30
        2290085, 1, // Triple Throw 30
        2290116, 1, // Areal Strike
        1302059, 3, // Dragon Carabella
        2049100, 1, // Chaos Scroll
        1092049, 1, // Dragon Kanjar
        1102041, 1, // Pink Cape
        1432018, 3, // Sky Ski
        1022047, 3, // Owl Mask
        3010051, 1, // Chair
        3010020, 1, // Portable meal table
        2040914, 1, // Shield for Weapon Atk

        1432011, 3, // Fair Frozen
        1442020, 3, // HellSlayer
        1382035, 3, // Blue Marine
        1372010, 3, // Dimon Wand
        1332027, 3, // Varkit
        1302056, 3, // Sparta
        1402005, 3, // Bezerker
        1472053, 3, // Red Craven
        1462018, 3, // Casa Crow
        1452017, 3, // Metus
        1422013, 3, // Lemonite
        1322029, 3, // Ruin Hammer
        1412010, 3, // Colonian Axe

        1472051, 1, // Green Dragon Sleeve
        1482013, 1, // Emperor's Claw
        1492013, 1, // Dragon fire Revlover

        1382049, 1,
        1382050, 1, // Blue Dragon Staff
        1382051, 1,
        1382052, 1,
        1382045, 1, // Fire Staff, Level 105
        1382047, 1, // Ice Staff, Level 105
        1382048, 1, // Thunder Staff
        1382046, 1, // Poison Staff

        1372035, 1,
        1372036, 1,
        1372037, 1,
        1372038, 1,
        1372039, 1,
        1372040, 1,
        1372041, 1,
        1372042, 1,
        1332032, 8, // Christmas Tree
        1482025, 7, // Flowery Tube

        4001011, 8, // Lupin Eraser
        4001010, 8, // Mushmom Eraser
        4001009, 8, // Stump Eraser

        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        2046004, 1,
        2046005, 1,
        2046104, 1,
        2046105, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        //list
        1132014, 3,
        1132015, 2,
        1132016, 1,
        1002801, 2,
        1102205, 2,
        1332079, 2,
        1332080, 2,
        1402048, 2,
        1402049, 2,
        1402050, 2,
        1402051, 2,
        1462052, 2,
        1462054, 2,
        1462055, 2,
        1472074, 2,
        1472075, 2,
        //pro raven
        1332077, 1,
        1382082, 1,
        1432063, 1,
        1452087, 1,
        1462053, 1,
        1472072, 1,
        1482048, 1,
        1492047, 1,
        2030008, 5, // Bottle, return scroll
        1442018, 3, // Frozen Tuna
        2040900, 4, // Shield for DEF
        2049100, 10,
        2000005, 10, // Power Elixir
        2000004, 10, // Elixir
        4280000, 8,
        2430144, 10,
        2290285, 10,
        2028061, 10,
        2028062, 10,
        2530000, 5,
        2531000, 5}; // Gold Box
    public final static int[] silverrewards = {
        2049401, 2,
        2049301, 2,
        3010041, 1, // skull throne
        1002452, 6, // Starry Bandana
        1002455, 6, // Starry Bandana
        2290084, 1, // Triple Throw 20
        2290048, 1, // Genesis 20
        2290040, 1, // Meteo 20
        2290046, 1, // Blizzard 20
        2290074, 1, // Sniping 20
        2290064, 1, // Concentration 20
        2290094, 1, // Smoke 20
        2290022, 1, // Berserk 20
        2290056, 1, // Bow Expert 30
        2290066, 1, // xBow Expert 30
        2290020, 1, // Sanc 20
        1102082, 1, // Black Raggdey Cape
        1302049, 1, // Glowing Whip
        2340000, 1, // White Scroll
        1102041, 1, // Pink Cape
        1452019, 2, // White Nisrock
        4001116, 3, // Hexagon Pend
        4001012, 3, // Wraith Eraser
        1022060, 2, // Foxy Racoon Eye
        2430144, 5,
        2290285, 5,
        2028062, 5,
        2028061, 5,
        2530000, 1,
        2531000, 1,
        2041100, 1,
        2041101, 1,
        2041102, 1,
        2041103, 1,
        2041104, 1,
        2041105, 1,
        2041106, 1,
        2041107, 1,
        2041108, 1,
        2041109, 1,
        2041110, 1,
        2041111, 1,
        2041112, 1,
        2041113, 1,
        2041114, 1,
        2041115, 1,
        2041116, 1,
        2041117, 1,
        2041118, 1,
        2041119, 1,
        2041300, 1,
        2041301, 1,
        2041302, 1,
        2041303, 1,
        2041304, 1,
        2041305, 1,
        2041306, 1,
        2041307, 1,
        2041308, 1,
        2041309, 1,
        2041310, 1,
        2041311, 1,
        2041312, 1,
        2041313, 1,
        2041314, 1,
        2041315, 1,
        2041316, 1,
        2041317, 1,
        2041318, 1,
        2041319, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1,
        1432011, 3, // Fair Frozen
        1442020, 3, // HellSlayer
        1382035, 3, // Blue Marine
        1372010, 3, // Dimon Wand
        1332027, 3, // Varkit
        1302056, 3, // Sparta
        1402005, 3, // Bezerker
        1472053, 3, // Red Craven
        1462018, 3, // Casa Crow
        1452017, 3, // Metus
        1422013, 3, // Lemonite
        1322029, 3, // Ruin Hammer
        1412010, 3, // Colonian Axe

        1002587, 3, // Black Wisconsin
        1402044, 1, // Pumpkin lantern
        2101013, 4, // Summoning Showa boss
        1442046, 1, // Super Snowboard
        1422031, 1, // Blue Seal Cushion
        1332054, 3, // Lonzege Dagger
        1012056, 3, // Dog Nose
        1022047, 3, // Owl Mask
        3012002, 1, // Bathtub
        1442012, 3, // Sky snowboard
        1442018, 3, // Frozen Tuna
        1432010, 3, // Omega Spear
        1432036, 1, // Fishing Pole
        2000005, 10, // Power Elixir
        2049100, 10,
        2000004, 10, // Elixir
        4280001, 8}; // Silver Box
    public final static int[] peanuts = {2430091, 200, 2430092, 200, 2430093, 200, 2430101, 200, 2430102, 200, 2430136, 200, 2430149, 200,//mounts
        2340000, 1, //rares
        1152000, 5, 1152001, 5, 1152004, 5, 1152005, 5, 1152006, 5, 1152007, 5, 1152008, 5, //toenail only comes when db is out.
        1152064, 5, 1152065, 5, 1152066, 5, 1152067, 5, 1152070, 5, 1152071, 5, 1152072, 5, 1152073, 5,
        3010019, 2, //chairs
        1001060, 10, 1002391, 10, 1102004, 10, 1050039, 10, 1102040, 10, 1102041, 10, 1102042, 10, 1102043, 10, //equips
        1082145, 5, 1082146, 5, 1082147, 5, 1082148, 5, 1082149, 5, 1082150, 5, //wg
        2043704, 10, 2040904, 10, 2040409, 10, 2040307, 10, 2041030, 10, 2040015, 10, 2040109, 10, 2041035, 10, 2041036, 10, 2040009, 10, 2040511, 10, 2040408, 10, 2043804, 10, 2044105, 10, 2044903, 10, 2044804, 10, 2043009, 10, 2043305, 10, 2040610, 10, 2040716, 10, 2041037, 10, 2043005, 10, 2041032, 10, 2040305, 10, //scrolls
        2040211, 5, 2040212, 5, 1022097, 10, //dragon glasses
        2049000, 10, 2049001, 10, 2049002, 10, 2049003, 10, //clean slate
        1012058, 5, 1012059, 5, 1012060, 5, 1012061, 5,//pinocchio nose msea only.
        1332100, 10, 1382058, 10, 1402073, 10, 1432066, 10, 1442090, 10, 1452058, 10, 1462076, 10, 1472069, 10, 1482051, 10, 1492024, 10, 1342009, 10, //durability weapons level 105
        2049400, 1, 2049401, 2, 2049301, 2,
        2049100, 10,
        2430144, 10,
        2290285, 10,
        2028062, 10,
        2028061, 10,
        2530000, 5,
        2531000, 5,
        1032080, 5,
        1032081, 4,
        1032082, 3,
        1032083, 2,
        1032084, 1,
        1112435, 5,
        1112436, 4,
        1112437, 3,
        1112438, 2,
        1112439, 1,
        1122081, 5,
        1122082, 4,
        1122083, 3,
        1122084, 2,
        1122085, 1,
        1132036, 5,
        1132037, 4,
        1132038, 3,
        1132039, 2,
        1132040, 1,
        //source
        1092070, 5,
        1092071, 4,
        1092072, 3,
        1092073, 2,
        1092074, 1,
        1092075, 5,
        1092076, 4,
        1092077, 3,
        1092078, 2,
        1092079, 1,
        1092080, 5,
        1092081, 4,
        1092082, 3,
        1092083, 2,
        1092084, 1,
        1092087, 1,
        1092088, 1,
        1092089, 1,
        1302143, 5,
        1302144, 4,
        1302145, 3,
        1302146, 2,
        1302147, 1,
        1312058, 5,
        1312059, 4,
        1312060, 3,
        1312061, 2,
        1312062, 1,
        1322086, 5,
        1322087, 4,
        1322088, 3,
        1322089, 2,
        1322090, 1,
        1332116, 5,
        1332117, 4,
        1332118, 3,
        1332119, 2,
        1332120, 1,
        1332121, 5,
        1332122, 4,
        1332123, 3,
        1332124, 2,
        1332125, 1,
        1342029, 5,
        1342030, 4,
        1342031, 3,
        1342032, 2,
        1342033, 1,
        1372074, 5,
        1372075, 4,
        1372076, 3,
        1372077, 2,
        1372078, 1,
        1382095, 5,
        1382096, 4,
        1382097, 3,
        1382098, 2,
        1392099, 1,
        1402086, 5,
        1402087, 4,
        1402088, 3,
        1402089, 2,
        1402090, 1,
        1412058, 5,
        1412059, 4,
        1412060, 3,
        1412061, 2,
        1412062, 1,
        1422059, 5,
        1422060, 4,
        1422061, 3,
        1422062, 2,
        1422063, 1,
        1432077, 5,
        1432078, 4,
        1432079, 3,
        1432080, 2,
        1432081, 1,
        1442107, 5,
        1442108, 4,
        1442109, 3,
        1442110, 2,
        1442111, 1,
        1452102, 5,
        1452103, 4,
        1452104, 3,
        1452105, 2,
        1452106, 1,
        1462087, 5,
        1462088, 4,
        1462089, 3,
        1462090, 2,
        1462091, 1,
        1472113, 5,
        1472114, 4,
        1472115, 3,
        1472116, 2,
        1472117, 1,
        1482075, 5,
        1482076, 4,
        1482077, 3,
        1482078, 2,
        1482079, 1,
        1492075, 5,
        1492076, 4,
        1492077, 3,
        1492078, 2,
        1492079, 1,
        1132012, 2,
        1132013, 1,
        1942002, 2,
        1952002, 2,
        1962002, 2,
        1972002, 2,
        1612004, 2,
        1622004, 2,
        1632004, 2,
        1642004, 2,
        1652004, 2,
        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        2046004, 1,
        2046005, 1,
        2046104, 1,
        2046105, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1,
        //ele wand
        1372035, 1,
        1372036, 1,
        1372037, 1,
        1372038, 1,
        //ele staff
        1382045, 1,
        1382046, 1,
        1382047, 1,
        1382048, 1,
        1382049, 1,
        1382050, 1, // Blue Dragon Staff
        1382051, 1,
        1382052, 1,
        1372039, 1,
        1372040, 1,
        1372041, 1,
        1372042, 1,
        2070016, 1,
        2070007, 2,
        2330007, 1,
        2070018, 1,
        2330008, 1,
        2070023, 1,
        2070024, 1,
        2028062, 5,
        2028061, 5};

    public static int[] eventCommonReward = {
        0, 10,
        1, 10,
        4, 5,
        5060004, 25,
        4170024, 25,
        4280000, 5,
        4280001, 6,
        5490000, 5,
        5490001, 6
    };

    public static int[] theSeedBoxReward = {
        0, 1,
        1, 1,
        4310034, 1,
        4310014, 1,
        4310016, 1,
        4001208, 1,
        4001547, 1,
        4001548, 1,
        4001549, 1,
        4001550, 1,
        4001551, 1,};

    public static int[] eventUncommonReward = {
        1, 4,
        2, 8,
        3, 8,
        2022179, 5,
        5062000, 20,
        2430082, 20,
        2430092, 20,
        2022459, 2,
        2022460, 1,
        2022462, 1,
        2430103, 2,
        2430117, 2,
        2430118, 2,
        2430201, 4,
        2430228, 4,
        2430229, 4,
        2430283, 4,
        2430136, 4,
        2430476, 4,
        2430511, 4,
        2430206, 4,
        2430199, 1,
        1032062, 5,
        5220000, 28,
        2022459, 5,
        2022460, 5,
        2022461, 5,
        2022462, 5,
        2022463, 5,
        5050000, 2,
        4080100, 10,
        4080000, 10,
        2049100, 10,
        2430144, 10,
        2290285, 10,
        2028062, 10,
        2028061, 10,
        2530000, 5,
        2531000, 5,
        2041100, 1,
        2041101, 1,
        2041102, 1,
        2041103, 1,
        2041104, 1,
        2041105, 1,
        2041106, 1,
        2041107, 1,
        2041108, 1,
        2041109, 1,
        2041110, 1,
        2041111, 1,
        2041112, 1,
        2041113, 1,
        2041114, 1,
        2041115, 1,
        2041116, 1,
        2041117, 1,
        2041118, 1,
        2041119, 1,
        2041300, 1,
        2041301, 1,
        2041302, 1,
        2041303, 1,
        2041304, 1,
        2041305, 1,
        2041306, 1,
        2041307, 1,
        2041308, 1,
        2041309, 1,
        2041310, 1,
        2041311, 1,
        2041312, 1,
        2041313, 1,
        2041314, 1,
        2041315, 1,
        2041316, 1,
        2041317, 1,
        2041318, 1,
        2041319, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1
    };
    public static int[] eventRareReward = {
        2049100, 5,
        2430144, 5,
        2290285, 5,
        2028062, 5,
        2028061, 5,
        2530000, 2,
        2531000, 2,
        2049116, 1,
        2049401, 10,
        2049301, 20,
        2049400, 3,
        2340000, 1,
        3010130, 5,
        3010131, 5,
        3010132, 5,
        3010133, 5,
        3010136, 5,
        3010116, 5,
        3010117, 5,
        3010118, 5,
        1112405, 1,
        1112445, 1,
        1022097, 1,
        2040211, 1,
        2040212, 1,
        2049000, 2,
        2049001, 2,
        2049002, 2,
        2049003, 2,
        1012058, 2,
        1012059, 2,
        1012060, 2,
        1012061, 2,
        2022460, 4,
        2022461, 3,
        2022462, 4,
        2022463, 3,
        2040041, 1,
        2040042, 1,
        2040334, 1,
        2040430, 1,
        2040538, 1,
        2040539, 1,
        2040630, 1,
        2040740, 1,
        2040741, 1,
        2040742, 1,
        2040829, 1,
        2040830, 1,
        2040936, 1,
        2041066, 1,
        2041067, 1,
        2043023, 1,
        2043117, 1,
        2043217, 1,
        2043312, 1,
        2043712, 1,
        2043812, 1,
        2044025, 1,
        2044117, 1,
        2044217, 1,
        2044317, 1,
        2044417, 1,
        2044512, 1,
        2044612, 1,
        2044712, 1,
        2046000, 1,
        2046001, 1,
        2046004, 1,
        2046005, 1,
        2046100, 1,
        2046101, 1,
        2046104, 1,
        2046105, 1,
        2046200, 1,
        2046201, 1,
        2046202, 1,
        2046203, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        2046300, 1,
        2046301, 1,
        2046302, 1,
        2046303, 1,
        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        1112427, 5,
        1112428, 5,
        1112429, 5,
        1012240, 10,
        1022117, 10,
        1032095, 10,
        1112659, 10,
        2070007, 10,
        2330007, 5,
        2070016, 5,
        2070018, 5,
        1152038, 1,
        1152039, 1,
        1152040, 1,
        1152041, 1,
        1122090, 1,
        1122094, 1,
        1122098, 1,
        1122102, 1,
        1012213, 1,
        1012219, 1,
        1012225, 1,
        1012231, 1,
        1012237, 1,
        2070023, 5,
        2070024, 5,
        2330008, 5,
        2003516, 5,
        2003517, 1,
        1132052, 1,
        1132062, 1,
        1132072, 1,
        1132082, 1,
        1112585, 1,
        //walker
        1072502, 1,
        1072503, 1,
        1072504, 1,
        1072505, 1,
        1072506, 1,
        1052333, 1,
        1052334, 1,
        1052335, 1,
        1052336, 1,
        1052337, 1,
        1082305, 1,
        1082306, 1,
        1082307, 1,
        1082308, 1,
        1082309, 1,
        1003197, 1,
        1003198, 1,
        1003199, 1,
        1003200, 1,
        1003201, 1,
        1662000, 1,
        1662001, 1,
        1672000, 1,
        1672001, 1,
        1672002, 1,
        //crescent moon
        1112583, 1,
        1032092, 1,
        1132084, 1,
        //mounts, 90 day
        2430290, 1,
        2430292, 1,
        2430294, 1,
        2430296, 1,
        2430298, 1,
        2430300, 1,
        2430302, 1,
        2430304, 1,
        2430306, 1,
        2430308, 1,
        2430310, 1,
        2430312, 1,
        2430314, 1,
        2430316, 1,
        2430318, 1,
        2430320, 1,
        2430322, 1,
        2430324, 1,
        2430326, 1,
        2430328, 1,
        2430330, 1,
        2430332, 1,
        2430334, 1,
        2430336, 1,
        2430338, 1,
        2430340, 1,
        2430342, 1,
        2430344, 1,
        2430347, 1,
        2430349, 1,
        2430351, 1,
        2430353, 1,
        2430355, 1,
        2430357, 1,
        2430359, 1,
        2430361, 1,
        2430392, 1,
        2430512, 1,
        2430536, 1,
        2430477, 1,
        2430146, 1,
        2430148, 1,
        2430137, 1,};
    public static int[] eventSuperReward = {
        2022121, 10,
        4031307, 50,
        3010127, 10,
        3010128, 10,
        3010137, 10,
        3010157, 10,
        2049300, 10,
        2040758, 10,
        1442057, 10,
        2049402, 10,
        2049304, 1,
        2049305, 1,
        2040759, 7,
        2040760, 5,
        2040125, 10,
        2040126, 10,
        1012191, 5,
        1112514, 1, //untradable/tradable
        1112531, 1,
        1112629, 1,
        1112646, 1,
        1112515, 1, //untradable/tradable
        1112532, 1,
        1112630, 1,
        1112647, 1,
        1112516, 1, //untradable/tradable
        1112533, 1,
        1112631, 1,
        1112648, 1,
        2040045, 10,
        2040046, 10,
        2040333, 10,
        2040429, 10,
        2040542, 10,
        2040543, 10,
        2040629, 10,
        2040755, 10,
        2040756, 10,
        2040757, 10,
        2040833, 10,
        2040834, 10,
        2041068, 10,
        2041069, 10,
        2043022, 12,
        2043120, 12,
        2043220, 12,
        2043313, 12,
        2043713, 12,
        2043813, 12,
        2044028, 12,
        2044120, 12,
        2044220, 12,
        2044320, 12,
        2044520, 12,
        2044513, 12,
        2044613, 12,
        2044713, 12,
        2044817, 12,
        2044910, 12,
        2046002, 5,
        2046003, 5,
        2046102, 5,
        2046103, 5,
        2046204, 10,
        2046205, 10,
        2046206, 10,
        2046207, 10,
        2046304, 10,
        2046305, 10,
        2046306, 10,
        2046307, 10,
        2040006, 2,
        2040007, 2,
        2040303, 2,
        2040403, 2,
        2040506, 2,
        2040507, 2,
        2040603, 2,
        2040709, 2,
        2040710, 2,
        2040711, 2,
        2040806, 2,
        2040903, 2,
        2040913, 2,
        2041024, 2,
        2041025, 2,
        2044815, 2,
        2044908, 2,
        1152046, 1,
        1152047, 1,
        1152048, 1,
        1152049, 1,
        1122091, 1,
        1122095, 1,
        1122099, 1,
        1122103, 1,
        1012214, 1,
        1012220, 1,
        1012226, 1,
        1012232, 1,
        1012238, 1,
        1032088, 1,
        1032089, 1,
        1032090, 1,
        1032091, 1,
        1132053, 1,
        1132063, 1,
        1132073, 1,
        1132083, 1,
        1112586, 1,
        1112593, 1,
        1112597, 1,
        1662002, 1,
        1662003, 1,
        1672003, 1,
        1672004, 1,
        1672005, 1,
        //130, 140 weapons
        1092088, 1,
        1092089, 1,
        1092087, 1,
        1102275, 1,
        1102276, 1,
        1102277, 1,
        1102278, 1,
        1102279, 1,
        1102280, 1,
        1102281, 1,
        1102282, 1,
        1102283, 1,
        1102284, 1,
        1082295, 1,
        1082296, 1,
        1082297, 1,
        1082298, 1,
        1082299, 1,
        1082300, 1,
        1082301, 1,
        1082302, 1,
        1082303, 1,
        1082304, 1,
        1072485, 1,
        1072486, 1,
        1072487, 1,
        1072488, 1,
        1072489, 1,
        1072490, 1,
        1072491, 1,
        1072492, 1,
        1072493, 1,
        1072494, 1,
        1052314, 1,
        1052315, 1,
        1052316, 1,
        1052317, 1,
        1052318, 1,
        1052319, 1,
        1052329, 1,
        1052321, 1,
        1052322, 1,
        1052323, 1,
        1003172, 1,
        1003173, 1,
        1003174, 1,
        1003175, 1,
        1003176, 1,
        1003177, 1,
        1003178, 1,
        1003179, 1,
        1003180, 1,
        1003181, 1,
        1302152, 1,
        1302153, 1,
        1312065, 1,
        1312066, 1,
        1322096, 1,
        1322097, 1,
        1332130, 1,
        1332131, 1,
        1342035, 1,
        1342036, 1,
        1372084, 1,
        1372085, 1,
        1382104, 1,
        1382105, 1,
        1402095, 1,
        1402096, 1,
        1412065, 1,
        1412066, 1,
        1422066, 1,
        1422067, 1,
        1432086, 1,
        1432087, 1,
        1442116, 1,
        1442117, 1,
        1452111, 1,
        1452112, 1,
        1462099, 1,
        1462100, 1,
        1472122, 1,
        1472123, 1,
        1482084, 1,
        1482085, 1,
        1492085, 1,
        1492086, 1,
        1532017, 1,
        1532018, 1,
        //mounts
        2430291, 1,
        2430293, 1,
        2430295, 1,
        2430297, 1,
        2430299, 1,
        2430301, 1,
        2430303, 1,
        2430305, 1,
        2430307, 1,
        2430309, 1,
        2430311, 1,
        2430313, 1,
        2430315, 1,
        2430317, 1,
        2430319, 1,
        2430321, 1,
        2430323, 1,
        2430325, 1,
        2430327, 1,
        2430329, 1,
        2430331, 1,
        2430333, 1,
        2430335, 1,
        2430337, 1,
        2430339, 1,
        2430341, 1,
        2430343, 1,
        2430345, 1,
        2430348, 1,
        2430350, 1,
        2430352, 1,
        2430354, 1,
        2430356, 1,
        2430358, 1,
        2430360, 1,
        2430362, 1,
        //rising sun
        1012239, 1,
        1122104, 1,
        1112584, 1,
        1032093, 1,
        1132085, 1
    };
    public static int[] tenPercent = {
        //10% scrolls
        2040002,
        2040005,
        2040026,
        2040031,
        2040100,
        2040105,
        2040200,
        2040205,
        2040302,
        2040310,
        2040318,
        2040323,
        2040328,
        2040329,
        2040330,
        2040331,
        2040402,
        2040412,
        2040419,
        2040422,
        2040427,
        2040502,
        2040505,
        2040514,
        2040517,
        2040534,
        2040602,
        2040612,
        2040619,
        2040622,
        2040627,
        2040702,
        2040705,
        2040708,
        2040727,
        2040802,
        2040805,
        2040816,
        2040825,
        2040902,
        2040915,
        2040920,
        2040925,
        2040928,
        2040933,
        2041002,
        2041005,
        2041008,
        2041011,
        2041014,
        2041017,
        2041020,
        2041023,
        2041058,
        2041102,
        2041105,
        2041108,
        2041111,
        2041302,
        2041305,
        2041308,
        2041311,
        2043002,
        2043008,
        2043019,
        2043102,
        2043114,
        2043202,
        2043214,
        2043302,
        2043402,
        2043702,
        2043802,
        2044002,
        2044014,
        2044015,
        2044102,
        2044114,
        2044202,
        2044214,
        2044302,
        2044314,
        2044402,
        2044414,
        2044502,
        2044602,
        2044702,
        2044802,
        2044809,
        2044902,
        2045302,
        2048002,
        2048005
    };
    public static int[] fishingReward = {
        0, 100, // Meso
        1, 100, // EXP
        2022179, 1, // Onyx Apple
        1302021, 5, // Pico Pico Hammer
        1072238, 1, // Voilet Snowshoe
        1072239, 1, // Yellow Snowshoe
        2049100, 2, // Chaos Scroll
        2430144, 1,
        2290285, 1,
        2028062, 1,
        2028061, 1,
        2049301, 1, // Equip Enhancer Scroll
        2049401, 1, // Potential Scroll
        1302000, 3, // Sword
        1442011, 1, // Surfboard
        4000517, 8, // Golden Fish
        4000518, 10, // Golden Fish Egg
        4031627, 2, // White Bait (3cm)
        4031628, 1, // Sailfish (120cm)
        4031630, 1, // Carp (30cm)
        4031631, 1, // Salmon(150cm)
        4031632, 1, // Shovel
        4031633, 2, // Whitebait (3.6cm)
        4031634, 1, // Whitebait (5cm)
        4031635, 1, // Whitebait (6.5cm)
        4031636, 1, // Whitebait (10cm)
        4031637, 2, // Carp (53cm)
        4031638, 2, // Carp (60cm)
        4031639, 1, // Carp (100cm)
        4031640, 1, // Carp (113cm)
        4031641, 2, // Sailfish (128cm)
        4031642, 2, // Sailfish (131cm)
        4031643, 1, // Sailfish (140cm)
        4031644, 1, // Sailfish (148cm)
        4031645, 2, // Salmon (166cm)
        4031646, 2, // Salmon (183cm)
        4031647, 1, // Salmon (227cm)
        4031648, 1, // Salmon (288cm)
        4001187, 20,
        4001188, 20,
        4001189, 20,
        4031629, 1 // Pot
    };
    public static int[] randomReward = {
        2000005, 5, // 파워 엘릭서
        2000005, 10, // 파워 엘릭서
        2000004, 5, // 엘릭서
        2000004, 10, // 엘릭서
        2001554, 5, // 황혼의 이슬
        2001554, 10, // 황혼의 이슬
        2001555, 3, // 시원한 주스
        2001555, 5, // 시원한 주스
        2001556, 5, // 만병 통치약
        2001556, 10, // 만병 통치약
        2002000, 3, // 민첩함의 물약
        2002000, 5, // 민첩함의 물약
        2002001, 3, // 속도향상의 물약
        2002001, 5, // 속도향상의 물약
        2002002, 3, // 마법사의 물약
        2002002, 5, // 마법사의 물약
        2002003, 3, // 현자의 물약
        2002003, 5, // 현자의 물약
        2002004, 3, // 전사의 물약
        2002004, 5, // 전사의 물약
        2002005, 3, // 명사수의 물약
        2002005, 5, // 명사수의 물약
    };

    public static boolean isReverseItem(int itemId) {
        switch (itemId) {
            case 1002790:
            case 1002791:
            case 1002792:
            case 1002793:
            case 1002794:
            case 1082239:
            case 1082240:
            case 1082241:
            case 1082242:
            case 1082243:
            case 1052160:
            case 1052161:
            case 1052162:
            case 1052163:
            case 1052164:
            case 1072361:
            case 1072362:
            case 1072363:
            case 1072364:
            case 1072365:

            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            case 1342012:
            case 1942002:
            case 1952002:
            case 1962002:
            case 1972002:
            case 1532016:
            case 1522017:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTimelessItem(int itemId) {
        switch (itemId) {
            case 1032031: //shield earring, but technically
            case 1102172:
            case 1002776:
            case 1002777:
            case 1002778:
            case 1002779:
            case 1002780:
            case 1082234:
            case 1082235:
            case 1082236:
            case 1082237:
            case 1082238:
            case 1052155:
            case 1052156:
            case 1052157:
            case 1052158:
            case 1052159:
            case 1072355:
            case 1072356:
            case 1072357:
            case 1072358:
            case 1072359:
            case 1092057:
            case 1092058:
            case 1092059:

            case 1122011:
            case 1122012:

            case 1302081:
            case 1312037:
            case 1322060:
            case 1332073:
            case 1332074:
            case 1372044:
            case 1382057:
            case 1402046:
            case 1412033:
            case 1422037:
            case 1432047:
            case 1442063:
            case 1452057:
            case 1462050:
            case 1472068:
            case 1482023:
            case 1492023:
            case 1342011:
            case 1532015:
            case 1522016:
                //raven.
                return true;
            default:
                return false;
        }
    }

    public static boolean isRing(int itemId) {
        return itemId >= 1112000 && itemId < 1120000;
    }// 112xxxx - pendants, 113xxxx - belts

    //if only there was a way to find in wz files -.-
    public static boolean isEffectRing(int itemid) {
        return isFriendshipRing(itemid) || isCrushRing(itemid) || isMarriageRing(itemid);
    }

    public static boolean isMarriageRing(int itemId) {
        switch (itemId) {
            case 1112300:
            case 1112301:
            case 1112302:
            case 1112303:
            case 1112304:
            case 1112305:
            case 1112306:
            case 1112307:
            case 1112308:
            case 1112309:
            case 1112310:
            case 1112311:
            case 1112744:
            case 4210000:
            case 4210001:
            case 4210002:
            case 4210003:
            case 4210004:
            case 4210005:
            case 4210006:
            case 4210007:
            case 4210008:
            case 4210009:
            case 4210010:
            case 4210011:
                return true;
        }
        return false;
    }

    public static boolean isFriendshipRing(int itemId) {
        switch (itemId) {
            case 1112800:
            case 1112801:
            case 1112802:
            case 1112810: //new
            case 1112811: //new, doesnt work in friendship?
            case 1112812: //new, im ASSUMING it's friendship cuz of itemID, not sure.
            case 1112816: //new, i'm also assuming
            case 1112817:

            case 1049000:
                return true;
        }
        return false;
    }

    public static boolean isCrushRing(int itemId) {
        switch (itemId) {
            case 1112001:
            case 1112002:
            case 1112003:
            case 1112005: //new
            case 1112006: //new
            case 1112007:
            case 1112012:
            case 1112015: //new

            case 1048000:
            case 1048001:
            case 1048002:
                return true;
        }
        return false;
    }
    public static int[] Equipments_Bonus = {1122017};

    public static int Equipment_Bonus_EXP(final int itemid) { // TODO : Add Time for more exp increase
        switch (itemid) {
            case 1122017:
                return 10;
        }
        return 0;
    }
    public static int[] blockedMaps = {180000001, 180000002, 109050000, 280030000, 240060200, 280090000, 280030001, 240060201, 950101100, 950101010};
    //If you can think of more maps that could be exploitable via npc,block nao pliz!

    public static int getExpForLevel(int i, int itemId) {
        if (isReverseItem(itemId)) {
            return getReverseRequiredEXP(i);
        } else if (getMaxLevel(itemId) > 0) {
            return getTimelessRequiredEXP(i);
        }
        return 0;
    }

    public static int getMaxLevel(final int itemId) {
        if (itemId >= 1113098 && itemId <= 1113128) {
            return 4;
        }
        Map<Integer, Map<String, Integer>> inc = MapleItemInformationProvider.getInstance().getEquipIncrements(itemId);
        return inc != null ? (inc.size()) : 0;
    }

    public static int getStatChance() {
        return 25;
    }

    public static int getXForStat(MonsterStatus stat) {
        switch (stat) {
            case MS_Darkness:
                return -70;
            case MS_Speed:
                return -50;
        }
        return 0;
    }

    public static int getSkillForStat(MonsterStatus stat) {
        switch (stat) {
            case MS_Darkness:
                return 1111003;
            case MS_Speed:
                return 3121007;
        }
        return 0;
    }
    public final static int[] normalDrops = {
        4001009, //real
        4001010,
        4001011,
        4001012,
        4001013,
        4001014, //real
        4001021,
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007}; //end
    public final static int[] rareDrops = {
        2022179,
        2049100,
        2049100,
        2430144,
        2028062,
        2028061,
        2290285,
        2049301,
        2049401,
        2022326,
        2022193,
        2049000,
        2049001,
        2049002};
    public final static int[] superDrops = {
        2040804,
        2049400,
        2028062,
        2028061,
        2430144,
        2430144,
        2430144,
        2430144,
        2290285,
        2049100,
        2049100,
        2049100,
        2049100};

    public static int getSkillBook(final int job) {
        if (job >= 2210 && job <= 2218) {
            switch (job) {
                case 2211:
                    return 1;
                case 2214:
                    return 2;
                case 2217:
                    return 3;
                default:
                    return 0;
            }
        } else if (job == 434) {
            return 5;
        }
        switch ((job % 100)) {
            case 10:
            case 20:
            case 30:
            case 40:
            case 50:
                return 1;
            case 11:
            case 21:
            case 31:
            case 41:
            case 51:
                return 2;
            case 12:
            case 22:
            case 32:
            case 42:
            case 52:
                return 3;
            case 33:
                return 4;
        }
        return 0;
    }

    public static int getSkillBook(final int job, final int skill) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        if (isZero(job)) {
            if (skill > 0) {
                int type = (skill % 1000) / 100; //1 beta 2 alpha
                return type == 1 ? 1 : 0;
            } else {
                return 0;
            }
        }
        switch (job) {

            case 110:
            case 120:
            case 130:
            case 210:
            case 220:
            case 230:
            case 310:
            case 320:
            case 410:
            case 420:
            case 510:
            case 520:

            case 570:
            case 1110:
            case 1310:
            case 1510:
            case 2310:
            case 2410:
            case 2510:
            case 2710:
            case 3110:
            case 3120:
            case 3210:
            case 3310:
            case 3510:
            case 3610:
            case 4110:
            case 4210:
            case 5110:
            case 6110:
            case 6510:
                return 1;
            case 111:
            case 121:
            case 131:
            case 211:
            case 221:
            case 231:
            case 311:
            case 321:
            case 411:
            case 421:
            case 511:
            case 521:

            case 571:
            case 1111:
            case 1311:
            case 1511:
            case 2311:
            case 2411:
            case 2511:
            case 2711:
            case 3111:
            case 3121:
            case 3211:
            case 3311:
            case 3511:
            case 3611:
            case 4111:
            case 4211:
            case 5111:
            case 6111:
            case 6511:
                return 2;
            case 112:
            case 122:
            case 132:
            case 212:
            case 222:
            case 232:
            case 312:
            case 322:
            case 412:
            case 422:
            case 512:
            case 522:

            case 572:
            case 1112:
            case 1312:
            case 1512:
            case 2312:
            case 2412:
            case 2512:
            case 2712:
            case 3112:
            case 3122:
            case 3212:
            case 3312:
            case 3512:
            case 3612:
            case 4112:
            case 4212:
            case 5112:
            case 6112:
            case 6512:
                return 3;
            case 508:
                return 0;
        }
        if (isSeparatedSp((short) job)) {
            if (job % 10 > 4) {
                return 0;
            }
            return (job % 10);
        }
        return 0;
    }

    public static int getSkillBook(final int job, final int level, final int skill) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        if (isSeparatedSp((short) job)) {
            return (level <= 30 ? 0 : (level >= 31 && level <= 60 ? 1 : (level >= 61 && level <= 100 ? 2 : (level >= 100 ? 3 : 0))));
        }
        return 0;
    }

    public static int getSkillBookForSkill(final int skillid) {
        return getSkillBook(skillid / 10000, skillid);
    }

    public static int getMountItemEx(int buffid) {
        final int riding = 1932000;
        switch (buffid) {
            case 1204: // 배틀쉽
                return riding + 0;
            case 80001163: // 스페이스쉽
                return riding + 2;
            case 80001449: // 스페이스쉽2
                return riding + 225;
            case 80001450: // 오토바이2
                return riding + 226;
            case 80001451: // 슈트2
                return riding + 227;
            case 80001026: // 빗자루 ok
                return riding + 5;
            case 80001003: // 목마 ok
                return riding + 6;
            case 80001004: // 악어 ok
                return riding + 7;
            case 80001005: // 오토바이 (갈색) ok
                return riding + 8;
            case 80001006: // 오토바이 (분홍색) ok
                return riding + 9;
            case 80001007: // 구름 ok
                return riding + 11;
            case 80001008: // 진짜 발록 ok
                return riding + 10;
            case 80001009: // 레이싱카 ok
                return riding + 13;
            case 80001010: // 피시방 호랑이 ok
                return riding + 14;
            case 80001011: // 미스트 발록 (전체모습) ok
                return riding + 12;
            case 80001013: // 주황버섯 ok
                return riding + 23;
            case 80001014: // 불타는 말 ok
                return riding + 25;
            case 80001015: // 타조 ok
                return riding + 26;
            case 80001016: // 핑크곰 열기구 ok
                return riding + 27;
            case 80001017: // 파랑 로봇 ok
                return riding + 28;
            case 80001018: // 오토바이 (빨강색) ok
                return riding + 34;
            case 80001019: // 파워드 슈트 ok
                return riding + 35;
            case 80001020: // 라이언킹 ok
                return riding + 41;
            case 80001021: // 블루 스쿠더 ok
                return riding + 43;
            case 80001022: // 루돌푸 개삐대 ok
                return riding + 44;
            case 80001023: // 복주머니 ok
                return riding + 48;
            case 80001027: // 나무 비행기 ok
                return riding + 49;
            case 80001028: // 빨간 비행기 ok
                return riding + 50;
            case 80001038: // 황금 장식 배 ok
                return riding + 53;
            case 80001030: // 닭 ok
                return riding + 54;
            case 80001031: // 부엉이 ok
                return riding + 55;
            case 80001032: // 파랑 자동차 ok
                return riding + 56;
            case 80001033: // 카니발 라이딩 ok
                return riding + 57;
            case 80001044: // 꼬마토기 ok
                return riding + 90;
            case 80001082: // 황소 ok
                return riding + 93;
            case 80001083: // 수레꾼토끼 ok
                return riding + 94;
            case 80001084: // 시발무서운토끼 ok
                return riding + 95;
            case 80001090: // 추장멧돼지 ok
                return riding + 96;
            case 80001137: // 검은부엉이 ok
                return riding + 110;
            case 80001144: // 류호수레꾼 ok
                return riding + 113;
            case 80001148: // 빨간붕붕차 ok
                return riding + 114;
            case 80001149: // 멋진 로봇 ok
                return riding + 115;
            case 80001198: // 드래고니카 ok
                return riding + 140;
            case 80001220: // 팬텀 ok
                return riding + 143;
            case 80001221: // 아리아 ok
                return riding + 144;
            case 80001228: // 재규어 ok
                return riding + 148;
            case 80001237: // 블랙와이번 ok
                return riding + 153;
            case 80001243: // 외발자전거 ok
                return riding + 156;
            case 80001244: // 겨울왕국 ok
                return riding + 157;
            case 80001246: // 달 ok
                return riding + 159;
            case 80001257: // 핑크빈 둥둥 ok
                return riding + 161;
            case 80001258: // 블랙빈 둥둥 ok
                return riding + 162;
            case 80001261: // 어떤 이상한년 ok
                return riding + 164;
            case 80001285: // 풍선 둥둥 ok
                return riding + 167;
            case 80001289: // 데비존 ok
                return riding + 170;
            case 80001290: // 신비목마 ok
                return riding + 171;
            case 80001292: // 어린왕자 ok
                return riding + 173;
            case 80001302: // 검은 드래곤 ok
                return riding + 178;
            case 80001304: // 멧돼지 ok
                return riding + 179;
            case 80001305: // 은빛갈기 ok
                return riding + 180;
            case 80001306: // 레드 드라코 ok
                return riding + 181;
            case 80001307: // 티티아나 ok
                return riding + 182;
            case 80001308: // 티티오 ok
                return riding + 183;
            case 80001309: // 신조 ok
                return riding + 184;
            case 80001312: // 류호 1 ok
                return riding + 187;
            case 80001313: // 류호 2 ok
                return riding + 188;
            case 80001314: // 류호 3 ok
                return riding + 189;
            case 80001315: // 류호 4 ok
                return riding + 190;
            case 80001316: // 에반 1 ok
                return riding + 191;
            case 80001317: // 에반 2 ok
                return riding + 192;
            case 80001318: // 에반 3 ok
                return riding + 193;
            case 80001319: // 하이에나 ok
                return riding + 194;
            case 80001327: // 덕덕 ok
                return riding + 198;
            case 80001331: // 보석 섹스 ok
                return riding + 199;
            case 80001336: // 하얀병아리 ok
                return riding + 200;
            case 80001338: // 장난감 ok
                return riding + 201;
            case 80001333: // 빨간근두운 ok
                return riding + 205;
            case 80001347: // 악마년 ok
                return riding + 207;
            case 80001348: // 힙합 ok
                return riding + 208;
            case 80001353: // 악마년2 ok
                return riding + 211;
            case 80001413: // 쟁반 ok
                return riding + 219;
            case 80001421: // 마차 ok
                return riding + 221;
            case 80001423: // 벨룸 ok
                return riding + 222;
            case 80001445: // 빛날개 ok
                return riding + 242;
            case 80001447: // 어둠날개 ok
                return riding + 243;
            case 80001484: // 부츠 ok
                return riding + 235;
            case 80001508: // 얼음말 ok
                return riding + 244;
            case 80001345:// 헤카톤주먹
                return riding + 204;
            case 80001199:// 독수으리 대처
                return riding + 256;
            case 80001490: // 나으리 대처
                return riding + 259;
            case 80001491: // 헬리콥터 대처
                return riding + 258;
            case 80001505: // 지각했당
                return riding + 251;
            case 80001492: // 꿀꿀나비
                return riding + 249;
            case 80001503: // 투명발록
                return riding + 12;
            case 80001531: //이상한말
                return riding + 253;
            case 80001549: //메이플차?
                return riding + 254;
            case 80001550: //팬더
                return riding + 255;
            case 80001355://돌고래
                return riding + 212;
            case 80001411://좀비트럭
                return riding + 218;
            case 80001552: //독수으리
            case 80001553:
                return +256;
            case 80001554: //헬리콥터?
            case 80001555:
                return +258;
            case 80001557://나으리
            case 80001558:
                return +259;
            case 80002305://총총사막여우
                return 1932454;

            case 80001786://오로라밤비니
                return 1932335;

            case 80002450://화염마
                return 1932522;

            case 80002446://15번가 시티투어
                return 1932517;
            case 80001533:// 꽃잎 프로펠러 라이딩
                return 1930001;

            case 2431495:// 예티 라이더 라이딩 30일
                return 1932003;

            case 80001174:// 더블 주황버섯 라이딩
                return 1932023;

            case 80001051:// 타조 라이딩
                return 1932026;

            case 80001187:// 더블 곰 열기구 라이딩
                return 1932027;

            case 80001510:// 새해 복많이
                return 1932048;

            case 80001186:// 더블 부엉이 라이딩
                return 1932055;

            case 80001324:// 바운싱 카 라이딩
                return 1932056;

            case 80001184:// 더블 나이트메어 라이딩
                return 1932127;

            case 80001644:// 블랙 와이번 라이딩
                return 1932153;

            case 80001245:// 비구름 라이딩 30일
                return 1932158;

            case 80001262:// 천사와 함께 라이딩 90일
                return 1932164;

            case 80001278:// 카푸의 라이딩
                return 1932165;

            case 80001039:// 페가수스 라이딩
                return 1932177;

            case 80001480:// 드래곤나이트 라이딩
                return 1932178;

            case 80001344:// 전투 비행정 라이딩
                return 1932203;

            case 80001398:// 낭만가을 라이딩
                return 1932216;

            case 80001400:// 비행침대 라이딩
                return 1932217;

            case 80001412:// 좀비트럭
                return 1932218;

            case 80001420:// 스케이트 보드
                return 1932220;

            case 80001404:// 지니 라이딩
                return 1932223;

            case 80001435:// 네이버 모자 라이딩 30일
                return 1932224;

            case 80001440:// 나인하트와 설원을 라이딩
                return 1932237;

            case 80001441:// 시그너스와 설원을 라이딩
                return 1932238;

            case 80001442:// 오르카와 설원을 라이딩
                return 1932239;

            case 80001443:// 하얀마법사와 설원을 라이딩 1일
                return 1932240;

            case 80001444:// 힐라와 설원을 라이딩
                return 1932241;

            case 80001561:// 비기닝 오픈카 라이딩
                return 1932263;

            case 80001562:// 라이징 스포츠카 라이딩
                return 1932264;

            case 80001563:// 플라잉 스포츠카 라이딩
                return 1932265;

            case 80001564:// 뉴 보드 라이딩
                return 1932266;

            case 80001565:// 프레시 보드 라이딩
                return 1932267;

            case 80001566:// 주니어 보드 라이딩
                return 1932268;

            case 80001567:// 시니어 보드 라이딩
                return 1932269;

            case 80001568:// 마스터 보드 라이딩
                return 1932270;

            case 80001569:// 프라임 보드 라이딩
                return 1932271;

            case 80001570:// 샤이닝 벌룬 라이딩
                return 1932272;

            case 80001582:// 고양이 손수레 라이딩
                return 1932275;

            case 80001584:// 제트스키 라이딩
                return 1932276;

            case 80001639:// 오르카의 부축 라이딩
                return 1932300;

            case 80001640:// 헬레나의 부축 라이딩
                return 1932301;

            case 80001703:// 별에서 온 요원 라이딩
                return 1932305;

            case 80001707:// 날아라 푸른양 라이딩
                return 1932306;

            case 80001708:// 날아라 분홍양 라이딩
                return 1932307;

            case 80001713:// 딸기케이크 라이딩
                return 1932311;

            case 80001763:// The MAY.Full 라이딩 30일
                return 1932319;

            case 80001764:// 펭귄즈 라이딩
                return 1932320;

            case 80001766:// 동물 해적단 라이딩
                return 1932321;

            case 80001775:// 함께해 핑크빈! 라이딩
                return 1932323;

            case 80001776:// 잠수정 라이딩
                return 1932324;

            case 80001790:// 꽃가마 라이딩
                return 1932236;

            case 80001792:// 아기가 되었다 라이딩
                return 1932337;

            case 80001811:// 꿈틀꿈틀 라이딩
                return 1932341;

            case 80001813:// 밤을 걷는 기차와 나 라이딩
                return 1932342;

            case 80001867:// 성난황소 라이딩 30일
                return 1932344;

            case 80001870:// 꺄아 송편 라이딩 30일
                return 1932347;

            case 80001918:// 향기방구 라이딩
                return 1932253;

            case 80001920:// 폭주 미메트 라이딩 30일
                return 1932354;

            case 80001933:// 게오르크 라이딩
                return 1932357;

            case 80001934:// 분쇄기 라이딩
                return 1932358;

            case 80001921:// 익룡 라이딩
                return 1932360;

            case 80001923:// 종이배 라이딩
                return 1932361;

            case 80001942:// 루돌프 썰매 라이딩
                return 1932365;

            case 80001954:// 타조 수레 라이딩
                return 1932369;

            case 80001955:// 낙타 수레 라이딩
                return 1932370;

            case 80001956:// 스팀 실린더 윙 라이딩
                return 1932374;

            case 80001958:// 눈꽃 마법진 라이딩
                return 1932375;

            case 80001975:// 슈퍼 히어로 라이딩
                return 1932377;

            case 80001977:// 귤팽이 라이딩
                return 1932378;

            case 80001980:// 스킨 스쿠버 라이딩
                return 1932379;

            case 80001982:// 꽃바람 라이딩
                return 1932380;

            case 80001988:// 핑크빈과 함께 클래식카 라이딩
                return 1932383;

            case 80001989:// 고급 클래식카 라이딩
                return 1932384;

            case 80001990:// 구조되다 라이딩
                return 1932385;

            case 80001991:// 요상한 몬스터 풍선 라이딩
                return 1932386;

            case 80001993:// 별 빛 구름 라이딩
                return 1932388;

            case 80001997:// 블링블링 하트 라이딩
                return 1932391;

            case 80001995:// 붕붕붕 핑크빈 라이딩
                return 1932392;

            case 80002202:// 화염새 라이딩
                return 1932395;

            case 80002219:// 구름동자 라이딩
                return 1932398;

            case 80002220:// 난다 고래 라이딩
                return 1932401;

            case 80002221:// 애드벌룬 라이딩
                return 1932402;

            case 80002222:// 자이로콥터 라이딩
                return 1932403;

            case 80002225:// 블루 츄츄고래 라이딩
                return 1932406;

            case 80002204:// 날치 라이딩
                return 1932408;

            case 80002229:// 아기 도요새 라이딩
                return 1932409;

            case 80002233:// 도령 월묘 라이딩
                return 1932410;

            case 80002234:// 방패연 라이딩
                return 1932411;

            case 80002235:// 흩날려라 한글 라이딩
                return 1932412;

            case 80002236:// 파스텔 자전거 라이딩
                return 1932414;

            case 80002238:// 콩콩사슴 라이딩
                return 1932415;

            case 80002240:// 스윗 도넛 라이딩
                return 1932418;

            case 80002242:// 쇼핑카트 라이딩
                return 1932419;

            case 80002248:// 아이스 하트 라이딩
                return 1932421;

            case 80002250:// 총총 너구리 라이딩
                return 1932422;

            case 80002270:// 절친 하늘 자전거 라이딩
                return 1932426;

            case 80002259:// 까치 라이딩
                return 1932427;

            case 80002258:// 럭비 토끼 라이딩
                return 1932428;

            case 80002261:// 냥냥 쿠션 라이딩
                return 1932430;

            case 80002262:// 푸른불꽃 허스키 라이딩
                return 1932431;

            case 80002265:// 까치 바구니 라이딩
                return 1932432;

            case 80002272:// 아르카나 바람의 정령 라이딩
                return 1932438;

            case 80002271:// 북극곰 라이딩
                return 1932439;

            case 80002277:// 붕붕 킥보드 라이딩
                return 1932441;

            case 80002278:// 바나달 라이딩
                return 1932442;

            case 80002287:// 꿀냥 꿀냥 라이딩
                return 1932445;

            case 80002289:// 키다리 풍선 라이딩
                return 1932446;

            case 80002315:// 더키와 목욕 타임 라이딩
                return 1932447;

            case 80002295:// 하트 별자리 라이딩
                return 1932448;

            case 80002297:// 우주 별고래 라이딩
                return 1932449;

            case 2436957:// 우주선 라이딩
                return 1932452;

            case 80002304:// 야광별 라이딩
                return 1932453;

            case 80002307:// 둥실 무지개 너머 라이딩
                return 1932455;

            case 80002314:// 샤이닝 요트 세일링 라이딩
                return 1932459;

            case 80002318:// 바나나보트 라이딩
                return 1932463;

            case 80002319:// 아이스 드래곤 라이딩
                return 1932464;

            case 80002321:// 고고 캠핑카 라이딩
                return 1932465;

            case 80002335:// 슬라임 빙수기 라이딩
                return 1932468;

            case 80002345:// 크리스탈 마법진 라이딩
                return 1932470;

            case 80002356:// 트윙클 솜사탕 라이딩
                return 1932474;

            case 80002358:// 바람은 풍선을 타고 라이딩
                return 1932475;

            case 80002361:// 날아라 보물지도 라이딩
                return 1932479;

            case 80002367:// 굿 밤 라이딩
                return 1932483;

            case 80002369:// 신나는 발걸음 라이딩
                return 1932486;

            case 80002400:// 눈사람 콩콩 라이딩
                return 1932498;

            case 80002402:// 귤끼리 라이딩
                return 1932499;

            case 80002417:// 예핑크스
                return 1932500;

            case 80002418:// 껨디와 썰매 라이딩
                return 1932501;

            case 80002425:// 이건 약과지 라이딩
                return 1932504;

            case 80002427:// 복조리 라이딩
                return 1932506;

            case 80002429:// 날아! 토끼랑 붕붕 라이딩
                return 1932507;

            case 80002432:// 돌돌 극세사 이불말이 라이딩
                return 1932508;

            case 80002545:// 워터 썸머 페스티벌 라이딩
                return 1932509;

            case 80002433:// 봄날의 오리 라이딩
                return 1932510;

            case 80002437:// 봄바람 튤립 라이딩
                return 1932513;

            case 80002439:// 보석새 라이딩
                return 1932514;

            case 80002443:// 셀럽의 슈퍼카 라이딩
                return 1932515;

            case 80002441:// 러브 메신저 라이딩
                return 1932516;

            case 80002447:// 핑크빈 스쿠터 라이딩
                return 1932518;

            case 80002448:// 오로라 돌고래 라이딩
                return 1932521;

            case 80002454:// 뽀송 하늘다람쥐 라이딩
                return 1932524;

            case 80002424:// 파카파카 라이딩
                return 1932525;

            case 80002546:// 거대 괴수 라이딩
                return 1932527;

            case 80002547:// 리버스 다크소울 라이딩
                return 1932528;

            case 80002572:// 디어 판타지아 라이딩
                return 1932537;

            case 80002573:// 스완 판타지아 라이딩
                return 1932538;

            case 80002622:// 어둠의 근원 라이딩
                return 1932540;

            case 80002585:// 하프물범 라이딩
                return 1932542;

            case 80002628://푸드트럭이담 라이딩
                return 1932543;

            case 80002630://루시드의 날개 라이딩
                return 1932544;

            case 80002648://이클립스 버드
                return 1932550;

            case 80002594://트랙터 라이딩
                return 1932552;

            case 80002595://병아리 라이딩
                return 1932553;

            case 80001621:// 헬레나와 함께 비행정
                return 1939000;

            case 80001620:// 지크문트와 함께 비행정
                return 1939001;

            case 80001623:// 겔리메르와 함께 비행정
                return 1939003;

            case 80001625:// 베릴과 함께 비행정
                return 1939004;

            case 80001628:// 스우와 함께 비행정
                return 1939005;

            case 80002699:// 돼지런 라이딩
                return 1932572;

            case 80002659:// 눈굴리기
                return 1932560;

            case 80002660:// 눈굴리기
                return 1932560;

            case 80002663:// 글래셔라이딩
                return 1932562;

            case 80002698://  글래셔라이딩
                return 1932562;

            case 80002664:// 호라이즌 페가수스
                return 1932571;

            case 80002668:// 달콤 찐득 초코 슬라임
                return 1932567;

            case 80002669:// 달콤 찐득 초코 슬라임
                return 1932567;

            case 80002667:// 붕어빵 라이딩
                return 1932564;

            case 80002702: // 허수아비 라이딩
                return 1932573;

            case 80002712: // 집중! 라이딩
                return 1932580;

            case 80002713: // 모험 가득 블랙빈 라이딩
                return 1932581;

            case 80002714: // 전부 담으시개 라이딩
                return 1932558;

            case 80002715: // 클라우드 라이딩
                return 1932584;

            case 80002716: // 클라우드 라이딩
                return 1932584;

            case 80002717: // 그림자 군마 라이딩
                return 1932585;

            case 80002718: // 그림자 군마 라이딩
                return 1932586;

            case 80002735: // 코튼 캔디빈 라이딩
                return 1932589;

            case 80002736: // 공사용 열기구
                return 1932591;

            case 80002738: // 뉴트로 화염새 라이딩
                return 1932590;

            case 80002740: // 뉴트로 냉동참치 라이딩
                return 1932594;

            case 80002742: // 글로리온 라이딩
                return 1932601;

            case 80002743: // 도철 라이딩
                return 1932602;

            case 80002744: // 천마 라이딩
                return 1932603;

            case 80002748: // 달려라! 은빛갈기!
                return 1932595;

            case 80002752: // 해적 거북이 라이딩
                return 1932596;

            case 80002754: // 뽀송 토끼 라이딩
                return 1932597;

            case 80002756: // 쁘띠 샤크 라이딩
                return 1932598;

            case 80002757: // 쁘띠 샤크 라이딩
                return 1932598;

            case 80002795: // (스트링 없음)
                return 1932606;

            case 80002796: // (스트링 없음)
                return 1932607;

            case 80002797: // (스트링 없음)
                return 1932607;

            case 80002798: // (스트링 없음)
                return 1932609;

            case 80002812: // (스트링 없음)
                return 1939012;

            case 80002824: // 근두운
                return 1932612;

            case 80002826: // 씨트론 탑승
                return 1932613;

            case 80002827: // 씨트론 탑승
                return 1939014;

            case 80002828: // 씨트론 탑승
                return 1939015;

            case 80002831: // 레드 씨트론 라이딩
                return 1932615;

            case 80002843: // 얼음 마차 라이딩
                return 1932614;

            case 80002844: // 얼음 마차 라이딩
                return 1932614;

            case 80002845: // 블루 씨트론 라이딩
                return 1932616;

            case 80002846: // 옐로우 씨트론 라이딩
                return 1932617;

            case 80002853: // 해치와 함께 라이딩
                return 1932618;

            case 80002854: // 메소팡팡 구름도깨비 라이딩
                return 1932619;

            case 80002855: // 천둥새 라이딩
                return 1939016;

            case 80002858: // 할로윈의 유령 라이딩
                return 1932623;

            case 80002859: // 달려라 보드 라이딩
                return 1932625;

            case 80002860: // 달려라 보드 라이딩
                return 1932625;

            case 80002862: // 핑아일체 라이딩
                return 1932624;

            case 80002869: // 메이플 월드의 산타 라이딩
                return 1932632;

            case 80002870: // 새우튀김 라이딩
                return 1932633;

            case 80002872: // 스노우메이커 라이딩
                return 1932635;

            case 80002875: // 서리 부엉이 라이딩 (▲ 여기까지 191223)
                return 1932639;
            default:
                return 0;
        }
    }

    public static int getMountItem(final int sourceid, MapleCharacter player) {
        if (sourceid == 33001001) {
            switch (Integer.parseInt(player.getInfoQuest(123456))) { // 임시 아직 재규어 전임
                case 10:
                    return 1932015;
                case 20:
                    return 1932030;
                case 30:
                    return 1932031;
                case 40:
                    return 1932032;
                case 50:
                    return 1932033;
                case 60:
                    return 1932036;
                case 70:
                    return 1932100;
                case 80:
                    return 1932149;
                case 90:
                    return 1932215;
                default:
                    return 0;
            }
        }
        return getMountItemEx(sourceid);
    }

    public static int getJaguarSummonId(int sourceid) {
        switch (sourceid - 33001007) {
            case 0:
                return 1932015;
            case 1:
                return 1932030;
            case 2:
                return 1932031;
            case 3:
                return 1932032;
            case 4:
                return 1932033;
            case 5:
                return 1932036;
            case 6:
                return 1932100;
            case 7:
                return 1932149;
            case 8:
                return 1932215;
            default:
                return 0;
        }
    }

    public static boolean isKatara(int itemId) {
        return itemId / 10000 == 134;
    }

    public static boolean isDagger(int itemId) {
        return itemId / 10000 == 133;
    }

    public static boolean isApplicableSkill(int skil) {
        return ((skil < 80000000 || skil >= 100000000) && (skil % 10000 < 8000 || skil % 10000 > 8006) && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); //no additional/decent skills
    }

    public static boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
        for (int i : PlayerStats.pvpSkills) {
            if (skil == i) {
                return true;
            }
        }
        return (skil >= 90000000 && skil < 92000000) || (skil % 10000 >= 8000 && skil % 10000 <= 8003) || isAngel(skil);
    }

    public static boolean isTablet(int itemId) {
        return itemId >= 2047000 && itemId <= 2047309;
    }

    public static boolean isGeneralScroll(int itemId) {
        return itemId / 1000 == 2046;
    }

    public static int getSuccessTablet(final int scrollId, final int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 55;
                case 2:
                    return 43;
                case 3:
                    return 33;
                case 4:
                    return 26;
                case 5:
                    return 20;
                case 6:
                    return 16;
                case 7:
                    return 12;
                case 8:
                    return 10;
                default:
                    return 7;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 35;
                case 2:
                    return 18;
                case 3:
                    return 12;
                default:
                    return 7;
            }
        } else {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 50; //-20
                case 2:
                    return 36; //-14
                case 3:
                    return 26; //-10
                case 4:
                    return 19; //-7
                case 5:
                    return 14; //-5
                case 6:
                    return 10; //-4
                default:
                    return 7;  //-3
            }
        }
    }

    public static int getCurseTablet(final int scrollId, final int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 12;
                case 2:
                    return 16;
                case 3:
                    return 20;
                case 4:
                    return 26;
                case 5:
                    return 33;
                case 6:
                    return 43;
                case 7:
                    return 55;
                case 8:
                    return 70;
                default:
                    return 100;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 12;
                case 1:
                    return 18;
                case 2:
                    return 35;
                case 3:
                    return 70;
                default:
                    return 100;
            }
        } else {
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 14; //+4
                case 2:
                    return 19; //+5
                case 3:
                    return 26; //+7
                case 4:
                    return 36; //+10
                case 5:
                    return 50; //+14
                case 6:
                    return 70; //+20
                default:
                    return 100;  //+30
            }
        }
    }

    public static boolean isAccessory(final int itemId) {
        return (itemId / 10000 == 101 || itemId / 10000 == 102 || itemId / 10000 == 103 || itemId / 10000 == 111 || itemId / 10000 == 112 || itemId / 10000 == 113 || itemId / 10000 == 114 || itemId / 10000 == 115);
    }

    public static boolean isMedal(final int itemId) {
        return itemId / 10000 == 114;
    }

    public static boolean potentialIDFits(final int potentialID, final int newstate, final int i) {
        //first line is always the best
        //but, sometimes it is possible to get second/third line as well
        //may seem like big chance, but it's not as it grabs random potential ID anyway
        if (newstate == 20) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 40000 : potentialID >= 30000 && potentialID < 60004); // xml say so
        } else if (newstate == 19) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 30000 : potentialID >= 20000 && potentialID < 30000);
        } else if (newstate == 18) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 20000 && potentialID < 30000 : potentialID >= 10000 && potentialID < 20000);
        } else if (newstate == 17) {
            return (i == 0 || Randomizer.nextInt(10) == 0 ? potentialID >= 10000 && potentialID < 20000 : potentialID < 10000);
        } else {
            return false;
        }
    }

    public static boolean optionTypeFits(final int optionType, final int itemId) {
        switch (optionType) {
            case 10: // weapons
                return isWeapon(itemId);
            case 11: // all equipment except weapons
                return !isWeapon(itemId);
            case 20: // all armors
                return !isAccessory(itemId) && !isWeapon(itemId);
            case 40: // accessories
                return isAccessory(itemId);
            case 51: // hat
                return itemId / 10000 == 100;
            case 52: // top and overall
                return itemId / 10000 == 104 || itemId / 10000 == 105;
            case 53: // bottom and overall
                return itemId / 10000 == 106 || itemId / 10000 == 105;
            case 54: // glove
                return itemId / 10000 == 108;
            case 55: // shoe
                return itemId / 10000 == 107;
            default:
                return true;
        }
    }

    public static final boolean isMountItemAvailable(final int mountid, final int jobid) {
        if (jobid != 900 && mountid / 10000 == 190) {
            switch (mountid) {
                case 1902000:
                case 1902001:
                case 1902002:
                    return isAdventurer(jobid);
                case 1902005:
                case 1902006:
                case 1902007:
                    return isKOC(jobid);
                case 1902015:
                case 1902016:
                case 1902017:
                case 1902018:
                    return isAran(jobid);
                case 1902040:
                case 1902041:
                case 1902042:
                    return isEvan(jobid);
                case 1932016:
                    return isMechanic(jobid);
                case 1932417:
                    return isMercedes(jobid);
            }

            if (isResist(jobid)) {
                return false; //none lolol
            }
        }
        if (mountid / 10000 != 190) {
            return false;
        }
        return true;
    }

    public static boolean isMechanicItem(final int itemId) {
        return itemId >= 1610000 && itemId < 1660000;
    }

    public static boolean isEvanDragonItem(final int itemId) {
        return itemId >= 1940000 && itemId < 1980000; //194 = mask, 195 = pendant, 196 = wings, 197 = tail
    }

    public static boolean canScroll(final int itemId) {
        return itemId / 100000 != 19 && itemId / 100000 != 16; //no mech/taming/dragon
    }

    public static boolean canHammer(final int itemId) {
        switch (itemId) {
            case 1122000:
            case 1122076: //ht, chaos ht
                return false;
        }
        if (!canScroll(itemId)) {
            return false;
        }
        return true;
    }
    public static int[] owlItems = new int[]{
        1002357, // 자쿰의 투구
        1112585, // 엔젤릭 블레스
        1032022, // 하프 이어링
        1082002, // 노가다 목장갑
        5062000, // 미라클 큐브
        2049100, // 혼돈의 주문서 60%
        1050018, // 파란색 가운
        1112400, // 연금술사의 반지
        5062009, // 레드큐브
        1112748, // 템페스트 링
    };

    public static int[] royalstyle = new int[]{
        1042290, // 화이트 체리 니트 (남자)
        1042290, // 화이트 체리 니트 (여자)
        1003910, // 쁘띠 디아블
        1003910, // 쁘띠 디아블
        1003859, // 아이리스 프쉬케
        1003859, // 아이리스 프쉬케
        1012371, // 손바닥 얼굴장식
        1012371, // 손바닥 얼굴장식
        1022183, // 이글이글 눈빛
        1022183, // 이글이글 눈빛
        1112258, // 청사과 말풍선 반지
        1112258, // 청사과 말풍선 반지
        1112146, // 청사과 명찰 반지
        1112146, // 청사과 명찰 반지
        5065100, // 스페셜 연발 폭죽
        5065100, // 스페셜 연발 폭죽
        5281003, // 방귀 스멜
        5281003, // 방귀 스멜
        5030028, // 팬더 고용상점 7일권
        5030028, // 팬더 고용상점 7일권
    };

    public static int[] masterpiece = new int[]{
        5069000, // 마스터피스
        5069001, // 프리미엄 마스터피스
    };

    public static int getMasterySkill(final int job) {
        if (job >= 1410 && job <= 1412) {
            return 14100000;
        } else if (job >= 410 && job <= 412) {
            return 4100000;
        } else if (job >= 520 && job <= 522) {
            return 5200000;
        }
        return 0;
    }

    public static int getExpRate_Below10(final int job) {
        if (GameConstants.isEvan(job)) {
            return 1;
        } else if (GameConstants.isAran(job) || GameConstants.isKOC(job) || GameConstants.isResist(job)) {
            return 1; //5
        }
        return 1; //10
    }

    public static int getExpRate_Quest(final int level) {
        return (level >= 30 ? (level >= 70 ? (level >= 120 ? 10 : 5) : 2) : 1);
    }

    public static String getCashBlockedMsg(final int id) {
        switch (id) {
            case 5062000:
            case 5050000:
            case 5062001:
                //cube
                return "This item may only be purchased at the PlayerNPC in FM.";
        }
        return "This item is blocked from the Cash Shop.";
    }

    public static int getCustomReactItem(final int rid, final int original) {
        if (rid == 2008006) { //orbis pq LOL
            return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 4001055);
            //4001056 = sunday. 4001062 = saturday
        } else {
            return original;
        }
    }

    public static int getJobNumber(int skill) {
        int jobz;
        int job = ((jobz = skill / 10000) % 1000);
        if (SkillFactory.getSkill(skill) != null && SkillFactory.getSkill(skill).isHyper()) {
            return 5;
        }
        if ((job / 100 == 0) || isBeginnerJob(jobz)) {
            return 0; //beginner
        }
        if (((job / 10) % 10 == 0) || (job == 501) || (job == 430)) {
            return 1;
        }
        if ((job == 431) || (job == 432)) {
            return 2;
        }
        if (job == 433) {
            return 3;
        }
        if (job == 434 ) {
            return 4;
        }
        return 2 + job % 10;
    }

    public static int getOrdinaryJobNumber(int job) {
        if (isAdventurer(job)) {
            return 0;
        } else if (isKOC(job)) {
            return 1000;
        } else if (isAran(job)) {
            return 2000;
        } else if (isEvan(job)) {
            return 2001;
        } else if (isMercedes(job)) {
            return 2002;
        } else if (isPhantom(job)) {
            return 2003;
        } else if (isLuminous(job)) {
            return 2004;
        } else if (isEunWol(job)) {
            return 2005;
        } else if (isDemonSlayer(job) || isDemonAvenger(job)) {
            return 3001;
        } else if (isXenon(job)) {
            return 3002;
        } else if (isResist(job)) {
            return 3000;
        } else if (isMichael(job)) {
            return 5000;
        } else if (isKaiser(job)) {
            return 6000;
        } else if (isAngelicBuster(job)) {
            return 6001;
        } else if (isKadena(job)) {
            return 6002;
        } else if (isZero(job)) {
            return 10000;
        } else if (isPinkBean(job)) {
            return 13000;
        } else if (isKinesis(job)) {
            return 14000;
        } else if (isIllium(job)) {
            return 15000;
        } else if (isArk(job)) {
            return 15001;
        } else if (isHoyeong(job)) {
            return 16000;
        } else if (isLala(job)) {
            return 16001;
        }
        return -1;
    }

    public static boolean isBeginnerJob(final int job) {
        return job == 0 || job == 1 || job == 301 || job == 1000 || job == 2000 || job == 2001 || job == 3000 || job == 3001 || job == 2002 || job == 2003 || job == 5000 || job == 2004 || job == 2005 || job == 6000 || job == 6001 || job == 6002 || job == 6003 || job == 3002 || job == 10000 || job == 13000 || job == 14000 || job == 6002 || job == 15000 || job == 15001 || job == 15002 || job == 16000 || job == 16001;
    }

    public static boolean isForceRespawn(int mapid) {
        switch (mapid) {
            case 103000800: //kerning PQ crocs
            case 925100100: //crocs and stuff
                return true;
        }
        return false;
    }

    public static int getCustomSpawnID(int summoner, int def) {
        switch (summoner) {
            case 9400589:
            case 9400748: //MV
                return 9400706; //jr
            default:
                return def;
        }
    }

    public static boolean canForfeit(int questid) {
        switch (questid) {
            case 20000:
            case 20010:
            case 20015: //cygnus quests
            case 20020:
                return false;
            default:
                return true;
        }
    }

    public static double getAttackRange(MapleStatEffect def, int rangeInc) {
        double defRange = ((400.0 + rangeInc) * (400.0 + rangeInc));
        if (def != null) {
            defRange += def.getMaxDistanceSq() + (def.getRange() * def.getRange());
        }
        //rangeInc adds to X
        //400 is approximate, screen is 600.. may be too much
        //200 for y is also too much
        //default 200000
        return defRange + 200000.0;
    }

    public static double getAttackRange(Point lt, Point rb) {
        double defRange = (400.0 * 400.0);
        final int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        final int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        defRange += (maxX * maxX) + (maxY * maxY);
        //rangeInc adds to X
        //400 is approximate, screen is 600.. may be too much
        //200 for y is also too much
        //default 200000
        return defRange + 120000.0;
    }

    public static int getLowestPrice(int itemId) {
        switch (itemId) {
            case 2340000: //ws
            case 2531000:
            case 2530000:
                return 50000000;
        }
        return -1;
    }

    public static boolean isNoDelaySkill(int skillId) {
        return skillId == 3301008 || skillId == 3311010 || skillId == 5100015 || skillId == 5110014 || skillId == 5120018 || skillId == 21101003 || skillId == 15100004 || skillId == 33101004 || skillId == 32111010 || skillId == 2111007 || skillId == 2211007 || skillId == 2311007 || skillId == 32121003 || skillId == 35121005 || skillId == 35111004 || skillId == 35121013 || skillId == 35121003 || skillId == 22150004 || skillId == 22181004 || skillId == 11101002 || skillId == 13101002 || skillId == 25100001 || skillId == 25100002 || skillId == 3121013 || skillId == 95001000 || skillId == 152120003;
    }

    public static boolean isNoSpawn(int mapID) {
        return mapID == 809040100 || mapID == 925020010 || mapID == 925020011 || mapID == 925020012 || mapID == 925020013 || mapID == 925020014 || mapID == 980010000 || mapID == 980010100 || mapID == 980010200 || mapID == 980010300 || mapID == 980010020;
    }

    public static int getExpRate(int job, int def) {
        return def;
    }

    public static int getModifier(int itemId, int up) {
        if (up <= 0) {
            return 0;
        }
        switch (itemId) {
            case 2022459:
            case 2860179:
            case 2860193:
            case 2860207:
                return 130;
            case 2022460:
            case 2022462:
            case 2022730:
                return 150;
            case 2860181:
            case 2860195:
            case 2860209:
                return 200;
        }
        return 200;
    }

    public static short getSlotMax(int itemId) {
        switch (itemId) {
            case 4001168:
            case 4031306:
            case 4031307:
            case 3993000:
            case 3993002:
            case 3993003:
            case 2048716:
            case 2049370:
            case 2450042:
            case 2049153:
            case 2048226:
            case 4310086:
            case 5220010:
            case 5220013:
            case 2434851:
            case 2048724:
            case 2048745:
            case 5220020:
            case 2432423:
            case 2711003:
            case 2711004:
            case 2630281:
            case 2711005:
            case 2711012:
            case 2435748:
            case 2437158:
            case 4036444:
            case 4310113:
            case 4310029:
            case 4310065:
            case 4310229:
            case 4021031:
            case 4000094:
            case 4001832:
            case 4031213:
            case 5220000:
            case 4310261:
            case 4033114:
            case 4000178:
            case 4000620:
            case 4000006:
            case 5062009:
            case 5062010:
            case 4031831:
            case 4001715:
            case 4033884:
            case 4033885:
            case 4033891:
            case 4033892:
            case 4003002:
            case 2437121:
            case 4034181:
            case 2430026:
            case 2430027:
            case 2450064:
            case 2450134:
            case 2431940:
            case 2437760:
            case 4310291:
            case 4036068:
            case 4310027:
            case 4310034:
            case 4310036:
            case 4310038:
            case 4310048:
            case 4310057:
            case 4310059:
            case 4310063:
            case 4310085:
            case 4310153:
            case 5062005:
            case 2048753:
            case 3996007:
            case 4031788:
            case 2430016:
            case 5062500:
            case 2431341:
            case 2438145:
            case 4033338:
            case 4009155:
            case 4000190:
            case 4000965:
            case 4010000:
            case 4010001:
            case 2435719:
            case 5068300:
            case 5068301:
            case 5068302:
            case 4260003:
            case 4260004:
            case 4260005:
            case 4021016:
            case 4031457:
            case 5068303:
            case 5068304:
            case 4031466:
            case 4162009:
            case 4009239:
            case 4310061:
            case 4036573:
            case 4034271:
            case 4310001:
            case 4031569:
            case 4031838:
            case 2433979:
            case 2437157:
            case 2450054:
            case 2049372:
            case 4034803:
            case 4033151:
            case 4036531:
            case 4021037:
            case 2000019:
            case 2023072:
            case 5060048:
            case 4001878:
            case 2003550:
            case 2003551:
            case 2003575:
            case 2630127:
            case 4310129:
            case 2049704:
            case 4001842:
            case 4001843:
            case 4001868:
            case 4001869:
            case 2048717:
            case 2630755:
            case 2439653:
            case 4000101:
            case 4034809:
            case 4000220:
            case 4000439:
            case 4000896:
            case 4000979:
            case 4031311:
            case 4310198:
            case 4001786:
            case 40:
                return 30000;
        }
        return 0;
    }

    public static boolean isDropRestricted(int itemId) {
        return itemId == 3012000 || itemId == 4030004 || itemId == 1052098 || itemId == 1052202;
    }

    public static boolean isPickupRestricted(int itemId) {
        return itemId == 4030003 || itemId == 4030004;
    }

    public static short getStat(int itemId, int def) {
        switch (itemId) {
            case 1002419:
                return 5;
            case 1002959:
                return 25;
            case 1142002:
                return 10;
            case 1122121:
                return 7;
        }
        return (short) def;
    }

    public static short getHpMp(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 500;
            case 1142002:
            case 1002959:
                return 1000;
        }
        return (short) def;
    }

    public static short getATK(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 3;
            case 1002959:
                return 4;
            case 1142002:
                return 9;
        }
        return (short) def;
    }

    public static short getDEF(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 250;
            case 1002959:
                return 500;
        }
        return (short) def;
    }

    public static boolean isDojo(int mapId) {
        return mapId >= 925020100 && mapId <= 925023814;
    }

    public static int getPartyPlayHP(int mobID) {
        switch (mobID) {
            case 4250000:
                return 836000;
            case 4250001:
                return 924000;
            case 5250000:
                return 1100000;
            case 5250001:
                return 1276000;
            case 5250002:
                return 1452000;

            case 9400661:
                return 15000000;
            case 9400660:
                return 30000000;
            case 9400659:
                return 45000000;
            case 9400658:
                return 20000000;
        }
        return 0;
    }

    public static int getPartyPlayEXP(int mobID) {
        switch (mobID) {
            case 4250000:
                return 5770;
            case 4250001:
                return 6160;
            case 5250000:
                return 7100;
            case 5250001:
                return 7975;
            case 5250002:
                return 8800;

            case 9400661:
                return 40000;
            case 9400660:
                return 70000;
            case 9400659:
                return 90000;
            case 9400658:
                return 50000;
        }
        return 0;
    }

    public static int getPartyPlay(int mapId) {
        switch (mapId) {
            case 300010000:
            case 300010100:
            case 300010200:
            case 300010300:
            case 300010400:
            case 300020000:
            case 300020100:
            case 300020200:
            case 300030000:

            case 683070400:
            case 683070401:
            case 683070402:
                return 25;
        }
        return 0;
    }

    public static int getPartyPlay(int mapId, int def) {
        int dd = getPartyPlay(mapId);
        if (dd > 0) {
            return dd;
        }
        return def / 2;
    }

    public static boolean isHyperTeleMap(int mapId) {
        for (int i : hyperTele) {
            if (i == mapId) {
                return true;
            }
        }
        return false;
    }

    public static String getCurrentFullDate() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        return new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).append(time.substring(11, 13)).append(time.substring(14, 16)).append(time.substring(17, 19)).toString();
    }

    public static int getCurrentDate() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).append(time.substring(11, 13)).toString());
    }

    public static int getCurrentDateYesterday() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        String date = String.valueOf(Integer.parseInt(time.substring(8, 10)) - 1);
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(date).append(time.substring(11, 13)).toString());
    }

    public static int getCurrentDate_NoTime() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).toString());
    }

    public static String getCurrentDate_NoTime2() {
        final String time = FileoutputUtil.CurrentReadable_Time();
        return new StringBuilder(time.substring(2, 4)).append("/").append(time.substring(5, 7)).append("/").append(time.substring(8, 10)).toString();
    }

    public static void achievementRatio(MapleClient c, int mapId) {
        //PQs not affected: Amoria, MV, CWK, English, Zakum, Horntail(?), Carnival, Ghost, Guild, LudiMaze, Elnath(?)
        switch (mapId) {
            case 240080600:
            case 920010000:
            case 930000000:
            case 930000100:
            case 910010000:
            case 922010100:
            case 910340100:
            case 925100000:
            case 926100000:
            case 926110000:
            case 921120005:
            case 932000100:
            case 923040100:
            case 921160100:
                c.getSession().writeAndFlush(CField.achievementRatio(0));
                break;
            case 930000200:
            case 922010200:
            case 922010300:
            case 922010400:
            case 922010401:
            case 922010402:
            case 922010403:
            case 922010404:
            case 922010405:
            case 925100100:
            case 926100001:
            case 926110001:
            case 921160200:
                c.getSession().writeAndFlush(CField.achievementRatio(10));
                break;
            case 930000300:
            case 910340200:
            case 922010500:
            case 922010600:
            case 925100200:
            case 925100201:
            case 925100202:
            case 926100100:
            case 926110100:
            case 921120100:
            case 932000200:
            case 923040200:
            case 921160300:
            case 921160310:
            case 921160320:
            case 921160330:
            case 921160340:
            case 921160350:
                c.getSession().writeAndFlush(CField.achievementRatio(25));
                break;
            case 930000400:
            case 926100200:
            case 926110200:
            case 926100201:
            case 926110201:
            case 926100202:
            case 926110202:
            case 921160400:
                c.getSession().writeAndFlush(CField.achievementRatio(35));
                break;
            case 910340300:
            case 922010700:
            case 930000500:
            case 925100300:
            case 925100301:
            case 925100302:
            case 926100203:
            case 926110203:
            case 921120200:
            case 932000300:
            case 240080700:
            case 240080800:
            case 923040300:
            case 921160500:
                c.getSession().writeAndFlush(CField.achievementRatio(50));
                break;
            case 910340400:
            case 922010800:
            case 930000600:
            case 925100400:
            case 926100300:
            case 926110300:
            case 926100301:
            case 926110301:
            case 926100302:
            case 926110302:
            case 926100303:
            case 926110303:
            case 926100304:
            case 926110304:
            case 921120300:
            case 932000400:
            case 923040400:
            case 921160600:
                c.getSession().writeAndFlush(CField.achievementRatio(70));
                break;
            case 910340500:
            case 922010900:
            case 930000700:
            case 920010800:
            case 925100500:
            case 926100400:
            case 926110400:
            case 926100401:
            case 926110401:
            case 921120400:
            case 921160700:
                c.getSession().writeAndFlush(CField.achievementRatio(85));
                break;
            case 922011000:
            case 922011100:
            case 930000800:
            case 920011000:
            case 920011100:
            case 920011200:
            case 920011300:
            case 925100600:
            case 926100500:
            case 926110500:
            case 926100600:
            case 926110600:
            case 921120500:
            case 921120600:
                c.getSession().writeAndFlush(CField.achievementRatio(100));
                break;
        }
    }

    public static boolean isAngel(int sourceid) {
        return isBeginnerJob(sourceid / 10000) && (sourceid % 10000 == 1085 || sourceid % 10000 == 1087 || sourceid % 10000 == 1090 || sourceid % 10000 == 1179);
    }

    public static int getRewardPot(int itemid, int closeness) {
        switch (itemid) {
            case 2440000:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028041 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028046 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028049 + (closeness / 10);
                }
                return 2028057;
            case 2440001:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028044 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028049 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028052 + (closeness / 10);
                }
                return 2028060;
            case 2440002:
                return 2028069;
            case 2440003:
                return 2430278;
            case 2440004:
                return 2430381;
            case 2440005:
                return 2430393;
        }
        return 0;
    }

    public static boolean isMagicChargeSkill(final int skillid) {
        switch (skillid) {
            case 2121001: // Big Bang
            case 2221001:
            case 2321001:
                return true;
        }
        return false;
    }

    public static boolean isTeamMap(final int mapid) {
        return mapid == 109080000 || mapid == 109080001 || mapid == 109080002 || mapid == 109080003 || mapid == 109080010 || mapid == 109080011 || mapid == 109080012 || mapid == 109090301 || mapid == 109090302 || mapid == 109090303 || mapid == 109090304 || mapid == 910040100 || mapid == 960020100 || mapid == 960020101 || mapid == 960020102 || mapid == 960020103 || mapid == 960030100 || mapid == 689000000 || mapid == 689000010;
    }

    public static int getStatDice(int stat) {
        switch (stat) {
            case 2:
                return 30;
            case 3:
                return 20;
            case 4:
                return 15;
            case 5:
                return 20;
            case 6:
                return 30;
        }
        return 0;
    }

    public static int getDiceStat(int buffid, int stat) {
        if (buffid == stat || buffid % 10 == stat || buffid / 10 == stat) {
            return getStatDice(stat);
        } else if (buffid == (stat * 100)) {
            return getStatDice(stat) + 10;
        }
        return 0;
    }

    public static int getMPByJob(MapleCharacter chr) {
        int force = 30;
        switch (chr.getJob()) {
            case 3110:
                force = 50;
            case 3111:
                force = 100;
            case 3112:
                force = 120;
        }
        Equip shield = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        if (chr.getSkillLevel(31120038) > 0) {
            force += 50;
        }
        if (chr.getSkillLevel(80000406) > 0) {
            force += SkillFactory.getSkill(80000406).getEffect(chr.getSkillLevel(80000406)).getMaxDemonForce();
        }
        if (shield != null) {
            force += shield.getMp();
        }
        return force; // beginner or 3100
    }

    public static int getSkillLevel(final int level) {
        if (level >= 70 && level < 120) {
            return 2;
        } else if (level >= 120 && level < 200) {
            return 3;
        } else if (level >= 200) {
            return 4;
        }
        return 1;
    }

    public static int[] getInnerSkillbyRank(int rank) {
        if (rank == 0) {
            return rankC;
        } else if (rank == 1) {
            return rankB;
        } else if (rank == 2) {
            return rankA;
        } else if (rank == 3) {
            return rankS;
        } else {
            return null;
        }
    }

    public static int ArcaneNextUpgrade(int level) {
        int lev = 12;
        for (int i = 1; i < level; i++) {
            lev += (i) + (i + 1);
        }
        return lev;
    }

    public static int AscenticNextUpgrade(int level) {
        int lev = 0;
        lev = 9 * (int) Math.pow(level, 2);
        lev += 20 * level;

        return lev;
    }

    public static int getCubeMeso(int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!ii.getEquipStats(itemId).containsKey("reqLevel")) {
            return -1;
        }
        int level = ii.getEquipStats(itemId).get("reqLevel").intValue();
        int meso = 0;
        switch (level) {
            case 35:
                meso = 612;
                break;
            case 38:
                meso = 722;
                break;
            case 40:
                meso = 800;
                break;
            case 43:
                meso = 924;
                break;
            case 45:
                meso = 1012;
                break;
            case 48:
                meso = 1152;
                break;
            case 50:
                meso = 1250;
                break;
            case 55:
                meso = 1512;
                break;
            case 58:
                meso = 1682;
                break;
            case 60:
                meso = 1800;
                break;
            case 64:
                meso = 2048;
                break;
            case 65:
                meso = 2112;
                break;
            case 68:
                meso = 2312;
                break;
            case 70:
                meso = 2450;
                break;
            case 75:
                meso = 14062;
                break;
            case 77:
                meso = 14822;
                break;
            case 78:
                meso = 15210;
                break;
            case 80:
                meso = 16000;
                break;
            case 85:
                meso = 18062;
                break;
            case 90:
                meso = 20250;
                break;
            case 95:
                meso = 22562;
                break;
            case 100:
                meso = 25000;
                break;
            case 105:
                meso = 27562;
                break;
            case 110:
                meso = 30250;
                break;
            case 115:
                meso = 33062;
                break;
            case 120:
                meso = 36000;
                break;
            case 125:
                meso = 312500;
                break;
            case 130:
                meso = 338000;
                break;
            case 135:
                meso = 364500;
                break;
            case 140:
                meso = 392000;
                break;
            case 145:
                meso = 420500;
                break;
            case 150:
                meso = 450000;
                break;
            case 160:
                meso = 512000;
                break;
            case 200:
                meso = 800000;
                break;
        }
        return meso;
    }

    public static long getMagnifyPrice(Equip eq) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!ii.getEquipStats(eq.getItemId()).containsKey("reqLevel")) {
            return -1;
        }
        int level = ii.getEquipStats(eq.getItemId()).get("reqLevel").intValue();
        long price;
        int v1; // esi@1
        double v2; // st7@7
        int v3; // eax@7
        double v4; // st6@7
        int v5; // eax@12

        v1 = 0;
        if (level > 120) {
            v1 = 20;
        } else if (level > 70) {
            v1 = 5;
        } else if (level > 30) {
            v1 = 1;
        }
        v2 = (double) level;
        v3 = 2;
        v4 = 0.5;
        while (1 != 0) {
            if ((v3 & 1) != 0) {
                v4 = v4 * v2;
            }
            v3 >>= 1;
            if (!(v3 != 0)) {
                break;
            }
            v2 = v2 * v2;
        }
        v5 = (int) Math.ceil(v4);
        price = ((v1 * v5 <= 0 ? 1 : 0) - 1) & v1 * v5;

        return price;
    }

    public static final boolean isSuperior(int itemId) {
        return ((itemId >= 1102471) && (itemId <= 1102485)) || ((itemId >= 1072732) && (itemId <= 1072747)) || ((itemId >= 1132164) && (itemId <= 1132178)) || ((itemId >= 1122241) && (itemId <= 1122245)) || ((itemId >= 1082543) && (itemId <= 1082547));
    }

    public static int getOptionType(final int itemId) {
        int id = itemId / 10000;
        if (isWeapon(itemId) || ((int) (itemId / 1000)) == 1099) {
            return 10; //무기
        } else if (id == 109 || id == 110 || id == 113) {
            return 20; //방패 & 망토 & 벨트
        } else if (isAccessory(itemId)) {
            return 40; //악세사리
        } else if (id == 100) {
            return 51; //투구
        } else if (id == 104 || id == 106) {
            return 52; //상의, 한벌옷
        } else if (id == 105) {
            return 53; //하의
        } else if (id == 108) {
            return 54; //장갑
        } else if (id == 107) {
            return 55;
        }
        return 0;
    }

    public static int getLuckyInfofromItemId(int itemid) {
        switch (itemid) {
            case 2048900:
                return 6;
            case 2048901:
                return 45;
            case 2048902:
                return 25;
            case 2048903:
                return 69;
            case 2048904:
                return 214;
            case 2048905:
                return 247;
            case 2048906:
                return 104;
            case 2048907:
                return 303;
            case 2048912:
                return 452;
            case 2048913:
                return 457;
            case 2048915:
                return 504;
            case 2048918:
                return 617;
        }
        return 0;
    }

    public static boolean isAngelicBlessBuffEffectItem(int skill) {
        switch (skill) {
            case 2022746: //엔젤릭 블레스
            case 2022747: //다크 엔젤릭 블레스
            case 2022764: //눈꽃 엔젤릭 블레스
            case 2022823: //화이트 엔젤릭 블레스
                return true;
        }
        return false;
    }

    public static int getRandomProfessionReactorByRank(int rank) {
        int base1 = 100000;
        int base2 = 200000;
        if (Randomizer.nextBoolean()) {
            if (rank == 1) {
                base1 += Randomizer.rand(0, 7);
            } else if (rank == 2) {
                base1 += Randomizer.rand(4, 9);
            } else if (rank == 3) {
                if (Randomizer.rand(0, 4) == 1) {
                    base1 += 11;
                } else {
                    base1 += Randomizer.rand(0, 9);
                }
            }
            return base1;
        } else {
            if (rank == 1) {
                base2 += Randomizer.rand(0, 7);
            } else if (rank == 2) {
                base2 += Randomizer.rand(4, 9);
            } else if (rank == 3) {
                if (Randomizer.rand(0, 6) == 1) {
                    base2 += 11;
                } else {
                    base2 += Randomizer.rand(0, 9);
                }
            }
            return base2;
        }
    }

    public static int getTriFling(final int job) {
        switch (job) {
            case 1310:
                return 13100022;
            case 1311:
                return 13110022;
            case 1312:
                return 13120003;
        }
        return 0;
    }

    public static int getStealSkill(int job) {
        switch (job) {
            case 1:
                return 24001001;
            case 2:
                return 24101001;
            case 3:
                return 24111001;
            case 4:
                return 24121001;
            case 5:
                return 24121054;
        }
        return 0;
    }

    public static int getNumSteal(int jobNum) {
        switch (jobNum) {
            case 1:
            case 2:
                return 4;
            case 3:
                return 3;
            case 4:
            case 5:
                return 2;
            default:
                return 0;
        }
    }

    public static int getAndroidType(int itemid) {
        switch (itemid) {
            case 1662000: //보급
            case 1662001:
            case 1662002: //고급
            case 1662003:
                return itemid - 1661999;
            case 1662004: //눈꽃
            case 1662005:
                return itemid - 1662003;
            case 1662006: //오마이프린세스
                return itemid - 1662001;
            case 1662007: //보급
            case 1662008:
                return itemid - 1662006;
            case 1662009: //발렌타인
            case 1662010:
                return itemid - 1662008;
            case 1662011: //판타스틱
            case 1662012:
                return itemid - 1662005;
            case 1662013: //9주년
            case 1662014:
                return itemid - 1662010;
            case 1662015: //메소레인저
            case 1662016:
            case 1662017: //썸머
            case 1662018:
                return itemid - 1662007;
            case 1662019: //썸머
            case 1662020:
            case 1662021: //TOP 스쿨
            case 1662022:
                return itemid - 1662009;
            case 1662024: //베릴
            case 1662025: //행복한
            case 1662026:
                return itemid - 1662010;
            case 1662027: //메이드
                return itemid - 1662022;
            case 1662035: //제로
            case 1662036:
                return itemid - 1662018;
            case 1662115:
                return 38;
            case 1662116:
                return 39;
            case 1662111:
                return 36;
            case 1662092:
                return 22;
            case 1662141:
                return 46;
            case 1662130:
                return 42;
            case 1662131:
                return 43;
            case 1662140:
                return 45;
            case 1662125:
                return 32;
            case 1662126:
                return 33;
            case 1662039: //오르카
            case 1662093:
                return 107;
            case 1662041: //할로윈
                return 110;
            case 1662032: //서큐버스
                return 101;
            case 1662043: //은월
            case 1662044:
                return itemid - 1662024;
            case 1662046: //무르무르
                return itemid - 1662025;
            case 1662114:
                return 37;
        }
        return 1;
    }

    public static boolean isExceedAttack(int id) {
        switch (id) {
            case 31011000:
            case 31011004:
            case 31011005:
            case 31011006:
            case 31011007:
            case 31201000:
            case 31201007:
            case 31201008:
            case 31201009:
            case 31201010:
            case 31211000:
            case 31211007:
            case 31211008:
            case 31211009:
            case 31211010:
            case 31221000:
            case 31221009:
            case 31221010:
            case 31221011:
            case 31221012:
                return true;
        }
        return false;
    }

    public static int isLightSkillsGaugeCheck(int skillid) { // 이부분 수정해주시면됨 밑에 * 3 옆에 숫자가 클수록 빨리참
        switch (skillid) {
            case 20041226: // 스펙트럴 라이트 (기본 직업)
                return 230 * 3;
            case 27001100: // 트윙클 플래쉬
                return 253 * 3;
            case 27101100: // 실피드 랜서
                return 325 * 3;
            case 27101101: // 인바이러빌러티
                return 230 * 3;
            case 27111100: // 스펙트럴 라이트
                return 255 * 3;
            case 27111101: // 샤인 리뎀션
                return 250 * 3;
            case 27121100: // 라이트 리플렉션
                return 354 * 3;
            default:
                return 0;
        }
    }

    public static int isDarkSkillsGaugeCheck(MapleCharacter chr, int skillid) {
        switch (skillid) {
            case 27001201: // 다크 폴링
                return 140 * 3;
            case 27101202: // 보이드 프레셔
                return 73 * 3;
            case 27111202: // 녹스피어
                return 210 * 3;
            case 27121201: // 모닝 스타폴
                return 10 * 3;
            case 27121202: { // 아포칼립스
                if (chr.getSkillLevel(27120047) > 0) {
                    return (int) (397 * 3 * 2);
                }
                return 397 * 3;
            }
            default:
                return 0;
        }
    }

    public static boolean isLightSkills(int skillid) {
        switch (skillid) {
            case 20041226: // 스펙트럴 라이트 (기본 직업)
            case 27001100: // 트윙클 플래쉬
            case 27101100: // 실피드 랜서
            case 27111100: // 스펙트럴 라이트
            case 27121100: // 라이트 리플렉션
                return true;
        }
        return false;
    }

    public static boolean isDarkSkills(int skillid) {
        switch (skillid) {
            case 27001201: // 다크 폴링
            case 27101202: // 보이드 프레셔
            case 27111202: // 녹스피어
            case 27121201: // 모닝 스타폴
            case 27121202: // 아포칼립스
                return true;
        }
        return false;
    }

    public static final String getJobNameById(final int job) {
        switch (job) {
            case 0:
                return "초보자";
            case 100:
                return "검사";
            case 110:
                return "파이터";
            case 111:
                return "크루세이더";
            case 112:
                return "히어로";
            case 120:
                return "페이지";
            case 121:
                return "나이트";
            case 122:
                return "팔라딘";
            case 130:
                return "스피어맨";
            case 131:
                return "버서커";
            case 132:
                return "다크나이트";
            case 200:
                return "마법사";
            case 210:
                return "위자드(불,독)";
            case 211:
                return "메이지(불,독)";
            case 212:
                return "아크메이지(불,독)";
            case 220:
                return "위자드(썬,콜)";
            case 221:
                return "메이지(썬,콜)";
            case 222:
                return "아크메이지(썬,콜)";
            case 230:
                return "클레릭";
            case 231:
                return "프리스트";
            case 232:
                return "비숍";
            case 300:
                return "아처";
            case 310:
                return "헌터";
            case 311:
                return "레인저";
            case 312:
                return "보우마스터";
            case 320:
                return "사수";
            case 321:
                return "저격수";
            case 322:
                return "신궁";
            case 400:
                return "로그";
            case 410:
                return "어쌔신";
            case 411:
                return "허밋";
            case 412:
                return "나이트로드";
            case 420:
                return "시프";
            case 421:
                return "시프마스터";
            case 422:
                return "섀도어";
            case 430:
                return "세미듀어러";
            case 431:
                return "듀어러";
            case 432:
                return "듀얼마스터";
            case 433:
                return "슬래셔";
            case 434:
                return "듀얼블레이더";
            case 500:
                return "해적";
            case 510:
                return "인파이터";
            case 511:
                return "버커니어";
            case 512:
                return "바이퍼";
            case 520:
                return "건슬링거";
            case 521:
                return "발키리";
            case 522:
                return "캡틴";
            case 800:
                return "매니저";
            case 900:
                return "운영자";
            case 1000:
                return "노블레스";
            case 1100:
            case 1110:
            case 1111:
            case 1112:
                return "소울마스터";
            case 1200:
            case 1210:
            case 1211:
            case 1212:
                return "플레임위자드";
            case 1300:
            case 1310:
            case 1311:
            case 1312:
                return "윈드브레이커";
            case 1400:
            case 1410:
            case 1411:
            case 1412:
                return "나이트워커";
            case 1500:
            case 1510:
            case 1511:
            case 1512:
                return "스트라이커";
            case 2000:
                return "레전드";
            case 2100:
            case 2110:
            case 2111:
            case 2112:
                return "아란";
            case 2001:
            case 2200:
            case 2210:
            case 2211:
            case 2212:
            case 2213:
            case 2214:
            case 2215:
            case 2216:
            case 2217:
            case 2218:
                return "에반";
            case 3000:
                return "시티즌";
            case 3200:
            case 3210:
            case 3211:
            case 3212:
                return "배틀메이지";
            case 3300:
            case 3310:
            case 3311:
            case 3312:
                return "와일드헌터";
            case 3500:
            case 3510:
            case 3511:
            case 3512:
                return "메카닉";
            case 501:
                return "해적(캐논슈터)";
            case 530:
                return "캐논슈터";
            case 531:
                return "캐논블래스터";
            case 532:
                return "캐논마스터";
            case 2002:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
                return "메르세데스";
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
                return "데몬슬레이어";
            case 2003:
            case 2400:
            case 2410:
            case 2411:
            case 2412:
                return "팬텀";
            case 2004:
            case 2700:
            case 2710:
            case 2711:
            case 2712:
                return "루미너스";
            case 5000:
            case 5100:
            case 5110:
            case 5111:
            case 5112:
                return "미하일";
            case 6000:
            case 6100:
            case 6110:
            case 6111:
            case 6112:
                return "카이저";
            case 6001:
            case 6500:
            case 6510:
            case 6511:
            case 6512:
                return "엔젤릭버스터";
            case 3101:
            case 3120:
            case 3121:
            case 3122:
                return "데몬어벤져";
            case 3002:
            case 3600:
            case 3610:
            case 3611:
            case 3612:
                return "제논";
            case 10000:
                return "제로JR";
            case 10100:
                return "제로10100";
            case 10110:
                return "제로10110";
            case 10111:
                return "제로10111";
            case 10112:
                return "제로";
            case 2005:
                return "???";
            case 2500:
            case 2510:
            case 2511:
            case 2512:
                return "은월";
            case 14000:
            case 14200:
            case 14210:
            case 14211:
            case 14212:
                return "키네시스";
            default:
                return "알수없음";
        }
    }

    public static short getSoulName(int soulid) {
        return (short) (soulid - 2591000 + 1);
    }

    public static int[] soulItemid
            = {
                2591010, 2591011, 2591012, 2591013, 2591014, 2591015, 2591016, 2591017, 2591018, 2591019, 2591020, 2591021, 2591022, 2591023, 2591024, 2591025, 2591026,
                2591027, 2591028, 2591029, 2591030, 2591031, 2591032, 2591033, 2591034, 2591035, 2591036, 2591037, 2591038, 2591039, 2591040, 2591041, 2591042, 2591043, 2591044, 2591045, 2591046,
                2591047, 2591048, 2591049, 2591050, 2591051, 2591052, 2591053, 2591054, 2591055, 2591056, 2591057, 2591058, 2591059, 2591060, 2591061, 2591062, 2591063, 2591064, 2591065, 2591066,
                2591067, 2591068, 2591069, 2591070, 2591071, 2591072, 2591073, 2591074, 2591075, 2591076, 2591077, 2591078, 2591079, 2591080, 2591081, 2591082, 2591085, 2591086, 2591087, 2591088,
                2591089, 2591090, 2591091, 2591092, 2591093, 2591094, 2591095, 2591096, 2591097, 2591098, 2591099, 2591100, 2591101, 2591102, 2591103, 2591104, 2591105, 2591106, 2591107, 2591108,
                2591109, 2591110, 2591111, 2591112, 2591113, 2591114, 2591115, 2591116, 2591117, 2591118, 2591119, 2591120, 2591121, 2591122, 2591123, 2591124, 2591125, 2591126, 2591127, 2591128,
                2591129, 2591130, 2591131, 2591132, 2591133, 2591134, 2591135, 2591136, 2591137, 2591138, 2591139, 2591140, 2591141, 2591142, 2591143, 2591144, 2591145, 2591146, 2591147, 2591148,
                2591149, 2591150, 2591151, 2591152, 2591153, 2591154, 2591155, 2591156, 2591157, 2591158, 2591159, 2591160, 2591161, 2591162, 2591163, 2591164, 2591165, 2591166, 2591167, 2591168,
                2591169, 2591170, 2591171, 2591172, 2591173, 2591174, 2591175, 2591176, 2591177, 2591178, 2591179, 2591180, 2591181, 2591182, 2591183, 2591184, 2591185, 2591186, 2591187, 2591188,
                2591189, 2591190, 2591191, 2591192, 2591193, 2591194, 2591195, 2591196, 2591197, 2591198, 2591199, 2591200, 2591201, 2591202, 2591203, 2591204, 2591205, 2591206, 2591207, 2591208,
                2591209, 2591210, 2591211, 2591212, 2591213, 2591214, 2591215, 2591216, 2591217, 2591218, 2591219, 2591220, 2591221, 2591222, 2591223, 2591224, 2591225, 2591226, 2591227, 2591228,
                2591229, 2591230, 2591231, 2591232, 2591233, 2591234, 2591235, 2591236, 2591237, 2591238, 2591239, 2591240, 2591241, 2591242, 2591243, 2591244, 2591245, 2591246, 2591247, 2591248,
                2591249, 2591250, 2591251, 2591252, 2591253, 2591254, 2591255, 2591256, 2591257, 2591258, 2591259, 2591260, 2591261, 2591262, 2591263, 2591264, 2591265, 2591266, 2591267, 2591268,
                2591269, 2591270, 2591271, 2591272, 2591273, 2591274, 2591275, 2591276, 2591277, 2591278, 2591279, 2591288, 2591289, 2591290, 2591291, 2591292, 2591293, 2591294, 2591295, 2591296,
                2591297, 2591298, 2591299, 2591300, 2591301, 2591302, 2591303, 2591304, 2591305, 2591306, 2591307, 2591308, 2591309, 2591310, 2591311, 2591312, 2591313, 2591314, 2591315, 2591316,
                2591317, 2591318, 2591319, 2591320, 2591321, 2591322, 2591323, 2591324, 2591325, 2591326, 2591327, 2591328, 2591329, 2591330, 2591331, 2591332, 2591333, 2591334, 2591335, 2591336,
                2591337, 2591338, 2591339, 2591340, 2591341, 2591342, 2591343, 2591344, 2591345, 2591346, 2591347, 2591348, 2591349, 2591350, 2591351, 2591352, 2591353, 2591354, 2591355, 2591356,
                2591357, 2591358, 2591359, 2591360, 2591361, 2591362, 2591363, 2591364, 2591365, 2591366, 2591367, 2591368, 2591369, 2591370, 2591371, 2591372, 2591373, 2591374, 2591375, 2591376,
                2591377, 2591378, 2591379, 2591380, 2591381
            };
    public static short[] soulPotentials
            = {
                177, 102, 103, 104, 131, 132, 201, 101, 102, 103, 104, 131, 132, 201, 105, 106, 107, 108, 133, 134, 202, 105, 106, 107, 108, 133, 134, 202, 109, 110,
                111, 112, 135, 136, 203, 113, 114, 115, 116, 204, 151, 152, 137, 403, 603, 121, 122, 123, 124, 206, 155, 156, 139, 403, 603, 117, 118, 119, 120, 207, 153, 154, 138, 403, 603, 167,
                168, 169, 170, 208, 171, 172, 177, 0, 0, 0, 0, 101, 102, 103, 104, 131, 132, 201, 101, 102, 103, 104, 131, 132, 201, 105, 106, 107, 108, 133, 134, 202, 105, 106, 107, 108, 133,
                134, 202, 109, 110, 111, 112, 135, 136, 203, 113, 114, 115, 116, 204, 151, 152, 137, 117, 118, 119, 120, 207, 153, 154, 138, 121, 122, 123, 124, 206, 155, 156, 139, 101, 102,
                103, 104, 131, 132, 201, 163, 164, 165, 166, 210, 151, 152, 175, 0, 101, 102, 103, 104, 131, 132, 201, 163, 164, 165, 166, 210, 151, 152, 175, 167, 168, 169, 170, 208, 171,
                172, 177, 179, 180, 181, 182, 183, 184, 201, 185, 186, 187, 188, 205, 153, 154, 189, 0, 179, 180, 181, 182, 183, 184, 201, 185, 186, 187, 188, 205, 153, 154, 189, 109, 110,
                111, 112, 135, 136, 203, 117, 118, 119, 120, 207, 153, 154, 138, 0, 109, 110, 111, 112, 135, 136, 203, 117, 118, 119, 120, 205, 153, 154, 138, 101, 102, 103, 104, 131, 132,
                201, 167, 168, 169, 170, 208, 173, 172, 177, 0, 101, 102, 103, 104, 131, 132, 201, 167, 168, 169, 170, 208, 173, 172, 177, 167, 168, 169, 170, 208, 171, 172, 177, 0, 121,
                186, 187, 188, 205, 153, 154, 189, 0, 185, 186, 187, 188, 207, 153, 154, 189, 0, 185, 186, 187, 188, 205, 153, 154, 189, 0, 185, 186, 187, 188, 207, 153, 154, 189, 0, 185,
                186, 187, 188, 206, 153, 154, 189, 0, 121, 186, 187, 188, 205, 153, 154, 189, 185, 186, 187, 188, 205, 153, 154, 189, 185, 186, 187, 188, 205, 153, 154, 189, 185, 186, 187,
                188, 207, 153, 154, 189, 185, 186, 187, 188, 206, 153, 154, 189
            };

    public static short getSoulPotential(int soulid) {
        short potential = 0;
        for (int i = 0; i < soulItemid.length; i++) {
            if (soulItemid[i] == soulid) {
                potential = soulPotentials[i];
                break;
            }
        }
        return potential;
    }

    public static boolean isSoulSummonSkill(int skillid) {
        switch (skillid) {
            case 80001266:
            case 80001269:
            case 80001270:
            case 80001322:
            case 80001323:
            case 80001341:
            case 80001395:
            case 80001396:
            case 80001493:
            case 80001494:
            case 80001495:
            case 80001496:
            case 80001497:
            case 80001498:
            case 80001499:
            case 80001500:
            case 80001501:
            case 80001502:
            case 80001681:
            case 80001682:
            case 80001683:
            case 80001685:
            case 80001690:
            case 80001691:
            case 80001692:
            case 80001693:
            case 80001695:
            case 80001696:
            case 80001697:
            case 80001698:
            case 80001700:
            case 80001804:
            case 80001806:
            case 80001807:
            case 80001808:
            case 80001984:
            case 80001985:
            case 80002639:
            case 80002405:
            case 80002406:
            case 80002230:
            case 80002231:
            case 80002641:
                return true;
        }
        return false;
    }

    private static int[] dmgskinitem = {2431965, 2431966, 2431967, 2432084, 2432131, 2432153, 2432154, 2432207, 2432354, 2432355, 2432465, 2432479, 2432526, 2432532, 2432592, 2432637, 2432638, 2432639, 2432640, 2432658, 2432659, 2432660, 2432661, 2432710, 2432836, 2432972, 2432973, 2433063, 2433178, 2433456, 2433631, 2433655, 2433715, 2433804, 2433913, 2433919, 2433980, 2433981, 2433990, 2434248, 2434273, 2434274, 2434289, 2434390, 2434391, 2434528, 2434529, 2434530, 2434542, 2434546, 2434574, 2434575, 2434654, 2434655, 2434661, 2434710, 2434734, 2434824, 2434950, 2434951, 2435023, 2435024, 2435025, 2435026, 2435027, 2435028, 2435029, 2435030, 2435043, 2435044, 2435045, 2435046, 2435047, 2435140, 2435141, 2435157, 2435158, 2435159, 2435160, 2435161, 2435162, 2435166, 2435168, 2435169, 2435170, 2435171, 2435172, 2435173, 2435174, 2435175, 2435176, 2435177, 2435179, 2435182, 2435184, 2435222, 2435293, 2435313, 2435316, 2435325, 2435326, 2435331, 2435332, 2435333, 2435334, 2435408, 2435424, 2435425, 2435427, 2435428, 2435429, 2435430, 2435431, 2435432, 2435433, 2435456, 2435461, 2435473, 2435474, 2435477, 2435478, 2435490, 2435491, 2435493, 2435516, 2435521, 2435522, 2435523, 2435524, 2435538, 2435972, 2436023, 2436024, 2436026, 2436027, 2436028, 2436029, 2435832, 2435833, 2435839, 2435840, 2435841, 2436045, 2436083, 2436084, 2436085, 2436098, 2436103, 2436140, 2436182, 2436206, 2436212, 2436215, 2435905, 2435906, 2435948, 2435949, 2435955, 2435956, 2436132, 2436258, 2436259, 2436268, 2436400, 2436437, 2436521, 2436522, 2436528, 2436529, 2436553, 2436560, 2436578, 2436596, 2436611, 2436612, 2436679, 2436680, 2436681, 2436682, 2436683, 2436684, 2436785, 2436810, 2436951, 2436952, 2436953, 2437009, 2437022, 2437023, 2437024, 2437164, 2437238, 2437239, 2437243, 2437482, 2437495, 2437496, 2437498, 2437515, 2437691, 2437877, 2438143, 2438144, 2438352, 2438378, 2438379, 2438413, 2438415, 2438417, 2438419, 2438460};
    private static int[] dmgskinnum = {0, 1, 2, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 5, 4, 11, 14, 5, 4, 11, 14, 15, 16, 8, 17, 18, 20, 19, 22, 22, 23, 24, 26, 15, 27, 28, 29, 34, 35, 36, 37, 38, 39, 41, 42, 43, 47, 44, 45, 46, 48, 49, 50, 51, 52, 53, 74, 75, 7, 23, 26, 27, 34, 35, 36, 76, 77, 78, 5, 79, 80, 81, 82, 85, 86, 87, 88, 89, 84, 91, 24, 20, 15, 46, 1, 8, 11, 14, 16, 17, 83, 90, 92, 93, 94, 95, 100, 48, 49, 96, 97, 98, 99, 101, 109, 110, 102, 103, 104, 112, 111, 113, 114, 105, 106, 96, 99, 34, 12, 36, 35, 106, 115, 116, 117, 118, 119, 120, 127, 128, 129, 130, 131, 132, 133, 121, 122, 123, 124, 125, 134, 136, 137, 135, 1, 138, 140, 142, 141, 143, 144, 1114, 1115, 1118, 1119, 1125, 1126, 1133, 146, 147, 145, 148, 149, 150, 154, 152, 153, 155, 156, 157, 160, 158, 159, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 175, 172, 173, 174, 176, 177, 179, 178, 184, 180, 181, 182, 183, 185, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200};
    public static boolean isOpen = false;

    public static int getDamageSkinNumberByItem(int itemid) {
        for (int i = 0; i < dmgskinitem.length; i++) {
            if (dmgskinitem[i] == itemid) {
                return dmgskinnum[i];
            }
        }
        return -1;
    }

    public static int getMonsterId(int mobid) {
        int ret = mobid;
        switch (mobid) {
            case 8641004:
            case 9800191:
                ret = 8641004;
                break;
            case 8641005:
            case 9010173:
            case 9800192:
                ret = 8641005;
                break;
            case 8641006:
            case 8641014:
            case 9101086:
            case 9800193:
                ret = 8641006;
                break;
            case 8641007:
            case 9800194:
                ret = 8641007;
                break;
            case 8642000:
            case 9010174:
            case 9800198:
            case 9833030:
            case 9833050:
            case 9833245:
                ret = 8642000;
                break;
            case 9800201:
            case 9833039:
            case 9833059:
            case 9833254:
                ret = 9800201;
                break;
            case 8642006:
            case 9800204:
            case 9833033:
            case 9833053:
            case 9833248:
                ret = 8642006;
                break;
            case 8642017:
            case 9800205:
            case 9833041:
            case 9833061:
            case 9833256:
                ret = 8642017;
                break;
            case 8642004:
            case 9800202:
            case 9833032:
            case 9833052:
                ret = 8642004;
                break;
            case 8642005:
            case 8642018:
            case 9800203:
            case 9833040:
                ret = 8642005;
                break;
            case 8642008:
            case 9800206:
            case 9833034:
            case 9833054:
            case 9833249:
                ret = 8642008;
                break;
            case 8642009:
            case 9800207:
            case 9833042:
            case 9833062:
            case 9833257:
                ret = 8642009;
                break;
            case 8642012:
            case 9800210:
            case 9833036:
                ret = 8642012;
                break;
            case 8642014:
            case 8642023:
            case 8642024:
            case 9800212:
            case 9833037:
            case 9833057:
            case 9833252:
                ret = 8642014;
                break;
            case 8642015:
            case 8642022:
            case 9800213:
            case 9833045:
            case 9833065:
                ret = 8642015;
                break;
            case 8642013:
            case 8642021:
            case 9800211:
            case 9833044:
                ret = 8642013;
                break;
            case 8643001:
            case 9010176:
                ret = 8643001;
                break;
            case 8643007:
            case 8643015:
            case 9010177:
                ret = 8643007;
                break;
            case 8644405:
            case 8644428:
            case 8644439:
                ret = 8644405;
                break;
            case 8644406:
            case 8644429:
            case 8644440:
            case 9010180:
                ret = 8644406;
                break;
            case 8644408:
            case 8644438:
            case 8644442:
                ret = 8644408;
                break;
            case 8644409:
            case 8644421:
            case 8644431:
            case 8644443:
                ret = 8644409;
                break;
            case 8644410:
            case 8644422:
            case 8644432:
            case 8644444:
                ret = 8644410;
                break;
            case 8644411:
            case 8644423:
            case 8644433:
            case 8644445:
                ret = 8644411;
                break;
            case 8644412:
            case 8644424:
            case 8644434:
            case 8644446:
            case 9010181:
                ret = 8644412;
                break;
            case 8644508:
            case 8644510:
                ret = 8644508;
                break;
            case 8644509:
            case 8644511:
            case 8880315:
            case 8880316:
            case 8880317:
            case 8880318:
            case 8880319:
            case 8880346:
            case 8880347:
            case 8880348:
            case 8880349:
            case 9010186:
                ret = 8644509;
                break;
            case 8644614:
            case 8644622:
            case 8644632:
            case 8644640:
                ret = 8644614;
                break;
            case 8644615:
            case 8644623:
            case 8644633:
            case 8644641:
                ret = 8644615;
                break;
            case 8644616:
            case 8644624:
            case 8644634:
            case 8644642:
                ret = 8644616;
                break;
            case 8644617:
            case 8644625:
            case 8644635:
            case 8644643:
                ret = 8644617;
                break;
            case 8644618:
            case 8644626:
            case 8644636:
            case 8644644:
                ret = 8644618;
                break;
            case 8644619:
            case 8644627:
            case 8644637:
            case 8644645:
                ret = 8644619;
                break;
            case 8644704:
            case 8644727:
            case 8644757:
            case 8644807:
            case 8644823:
            case 8644825:
            case 8644835:
            case 9833299:
            case 9833318:
                ret = 8644704;
                break;
            case 8644705:
            case 8644728:
            case 8644758:
            case 8644808:
            case 8644824:
            case 8644826:
            case 8644836:
            case 9833300:
            case 9833319:
                ret = 8644705;
                break;
            case 8644700:
            case 8644720:
            case 8644750:
            case 8644800:
            case 8644828:
            case 9833293:
            case 9833312:
                ret = 8644700;
                break;
            case 8644701:
            case 8644721:
            case 8644751:
            case 8644801:
            case 8644829:
            case 9833291:
            case 9833292:
            case 9833311:
                ret = 8644701;
                break;
            case 8645010:
            case 8645030:
                ret = 8645010;
                break;
            case 8645011:
            case 8645014:
            case 8645032:
            case 8645040:
                ret = 8645011;
                break;
        }
        return ret;
    }

    public static int getDamageSkinIdByNumber(int number) {
        for (int i = 0; i < dmgskinitem.length; i++) {
            if (dmgskinnum[i] == number) {
                return dmgskinitem[i];
            }
        }
        return 0;
    }

    public static int getItemIdbyNum(int num) {
        int ret = -1;
        switch (num) {
            case 230:
                ret = 2630214;
                break;
            case 229:
                ret = 2630178;
                break;
            case 228:
                ret = 2630222;
                break;
            case 227:
                ret = 2630137;
                break;
            case 225:
                ret = 2439927;
                break;
            case 224:
                ret = 2439925;
                break;
            case 223:
                ret = 2439769;
                break;
            case 222:
                ret = 2439686;
                break;
            case 221:
                ret = 2439684;
                break;
            case 219:
                ret = 2439665;
                break;
            case 218:
                ret = 2439617;
                break;
            case 217:
                ret = 2439572;
                break;
            case 216:
                ret = 2439408;
                break;
            case 215:
                ret = 2439395;
                break;
            case 214:
                ret = 2439393;
                break;
            case 213:
                ret = 2439381;
                break;
            case 212:
                ret = 2439338;
                break;
            case 211:
                ret = 2439337;
                break;
            case 210:
                ret = 2439336;
                break;
            case 209:
                ret = 2439298;
                break;
            case 208:
                ret = 2438871;
                break;
            case 207:
                ret = 2438885;
                break;
            case 206:
                ret = 2438881;
                break;
            case 205:
                ret = 2438713;
                break;
            case 204:
                ret = 2438672;
                break;
            case 203:
                ret = 2438637;
                break;
            case 202:
                ret = 2438530;
                break;
            case 201:
                ret = 2438492;
                break;
            case 200:
                ret = 2438485;
                break;
            case 199:
                ret = 2438419;
                break;
            case 198:
                ret = 2438417;
                break;
            case 197:
                ret = 2438415;
                break;
            case 196:
                ret = 2438413;
                break;
            case 195:
                ret = 2438379;
                break;
            case 194:
                ret = 2438378;
                break;
            case 193:
                ret = 2438353;
                break;
            case 192:
                ret = 2438147;
                break;
            case 191:
                ret = 2438146;
                break;
            case 190:
                ret = 2437877;
                break;
            case 189:
                ret = 2438315;
                break;
            case 188:
                ret = 2438314;
                break;
            case 187:
                ret = 2438313;
                break;
            case 186:
                ret = 2438312;
                break;
            case 185:
                ret = 2438311;
                break;
            case 184:
                ret = 2438310;
                break;
            case 183:
                ret = 2438309;
                break;
            case 182:
                ret = 2438308;
                break;
            case 181:
                ret = 2438307;
                break;
            case 180:
                ret = 2438306;
                break;
            case 179:
                ret = 2438305;
                break;
            case 178:
                ret = 2438304;
                break;
            case 177:
                ret = 2438303;
                break;
            case 176:
                ret = 2438302;
                break;
            case 175:
                ret = 2438301;
                break;
            case 174:
                ret = 2438300;
                break;
            case 173:
                ret = 2438299;
                break;
            case 172:
                ret = 2438298;
                break;
            case 171:
                ret = 2438297;
                break;
            case 170:
                ret = 2438296;
                break;
            case 169:
                ret = 2438295;
                break;
            case 168:
                ret = 2438294;
                break;
            case 167:
                ret = 2438293;
                break;
            case 166:
                ret = 2438292;
                break;
            case 165:
                ret = 2438291;
                break;
            case 164:
                ret = 2438290;
                break;
            case 163:
                ret = 2438289;
                break;
            case 162:
                ret = 2438288;
                break;
            case 161:
                ret = 2438287;
                break;
            case 160:
                ret = 2436596;
                break;
            case 159:
                ret = 2438286;
                break;
            case 158:
                ret = 2438285;
                break;
            case 157:
                ret = 2438284;
                break;
            case 156:
                ret = 2438283;
                break;
            case 155:
                ret = 2438282;
                break;
            case 154:
                ret = 2438281;
                break;
            case 153:
                ret = 2438280;
                break;
            case 152:
                ret = 2438279;
                break;
            case 150:
                ret = 2438278;
                break;
            case 149:
                ret = 2438277;
                break;
            case 148:
                ret = 2438276;
                break;
            case 147:
                ret = 2438275;
                break;
            case 146:
                ret = 2438274;
                break;
            case 145:
                ret = 2438273;
                break;
            case 144:
                ret = 2438272;
                break;
            case 143:
                ret = 2438271;
                break;
            case 142:
                ret = 2436182;
                break;
            case 141:
                ret = 2438270;
                break;
            case 140:
                ret = 2438269;
                break;
            case 139:
                ret = 2438268;
                break;
            case 138:
                ret = 2438267;
                break;
            case 137:
                ret = 2438266;
                break;
            case 136:
                ret = 2438265;
                break;
            case 135:
                ret = 2438264;
                break;
            case 134:
                ret = 2438263;
                break;
            case 133:
                ret = 2438262;
                break;
            case 132:
                ret = 2438261;
                break;
            case 131:
                ret = 2438260;
                break;
            case 130:
                ret = 2438259;
                break;
            case 129:
                ret = 2438258;
                break;
            case 128:
                ret = 2438257;
                break;
            case 127:
                ret = 2438256;
                break;
            case 126:
                ret = 2438255;
                break;
            case 125:
                ret = 2438254;
                break;
            case 124:
                ret = 2438253;
                break;
            case 123:
                ret = 2438252;
                break;
            case 122:
                ret = 2438251;
                break;
            case 121:
                ret = 2438250;
                break;
            case 120:
                ret = 2438249;
                break;
            case 119:
                ret = 2435524;
                break;
            case 118:
                ret = 2435523;
                break;
            case 117:
                ret = 2435521;
                break;
            case 115:
                ret = 2435516;
                break;
            case 114:
                ret = 2438248;
                break;
            case 113:
                ret = 2438247;
                break;
            case 112:
                ret = 2438246;
                break;
            case 111:
                ret = 2438245;
                break;
            case 110:
                ret = 2438244;
                break;
            case 109:
                ret = 2438243;
                break;
            case 108:
                ret = 2438242;
                break;
            case 107:
                ret = 2438241;
                break;
            case 106:
                ret = 2438240;
                break;
            case 105:
                ret = 2438239;
                break;
            case 104:
                ret = 2438238;
                break;
            case 103:
                ret = 2438237;
                break;
            case 102:
                ret = 2438236;
                break;
            case 101:
                ret = 2438235;
                break;
            case 100:
                ret = 2438234;
                break;
            case 99:
                ret = 2438233;
                break;
            case 98:
                ret = 2438232;
                break;
            case 97:
                ret = 2438231;
                break;
            case 96:
                ret = 2438230;
                break;
            case 95:
                ret = 2438229;
                break;
            case 94:
                ret = 2438228;
                break;
            case 93:
                ret = 2438227;
                break;
            case 92:
                ret = 2438226;
                break;
            case 91:
                ret = 2438225;
                break;
            case 90:
                ret = 2438224;
                break;
            case 89:
                ret = 2438223;
                break;
            case 88:
                ret = 2438222;
                break;
            case 87:
                ret = 2438221;
                break;
            case 86:
                ret = 2438220;
                break;
            case 85:
                ret = 2438219;
                break;
            case 84:
                ret = 2438218;
                break;
            case 83:
                ret = 2438217;
                break;
            case 82:
                ret = 2438216;
                break;
            case 81:
                ret = 2438215;
                break;
            case 80:
                ret = 2438214;
                break;
            case 79:
                ret = 2438213;
                break;
            case 78:
                ret = 2438212;
                break;
            case 77:
                ret = 2438211;
                break;
            case 76:
                ret = 2438210;
                break;
            case 75:
                ret = 2438209;
                break;
            case 74:
                ret = 2438208;
                break;
            case 73:
                ret = 2438200;
                break;
            case 72:
                ret = 2438199;
                break;
            case 71:
                ret = 2438198;
                break;
            case 70:
                ret = 2438197;
                break;
            case 69:
                ret = 2438196;
                break;
            case 68:
                ret = 2438195;
                break;
            case 67:
                ret = 2438194;
                break;
            case 66:
                ret = 2438192;
                break;
            case 65:
                ret = 2438190;
                break;
            case 64:
                ret = 2438189;
                break;
            case 63:
                ret = 2438188;
                break;
            case 62:
                ret = 2438185;
                break;
            case 61:
                ret = 2438184;
                break;
            case 60:
                ret = 2438183;
                break;
            case 59:
                ret = 2438181;
                break;
            case 58:
                ret = 2438171;
                break;
            case 57:
                ret = 2438166;
                break;
            case 56:
                ret = 2438164;
                break;
            case 55:
                ret = 2438161;
                break;
            case 54:
                ret = 2435172;
                break;
            case 53:
                ret = 2438207;
                break;
            case 52:
                ret = 2438206;
                break;
            case 51:
                ret = 2438205;
                break;
            case 50:
                ret = 2438204;
                break;
            case 49:
                ret = 2438203;
                break;
            case 48:
                ret = 2438202;
                break;
            case 47:
                ret = 2438201;
                break;
            case 39:
                ret = 2438193;
                break;
            case 37:
                ret = 2438191;
                break;
            case 33:
                ret = 2438167;
                break;
            case 32:
                ret = 2438174;
                break;
            case 31:
                ret = 2438170;
                break;
            case 30:
                ret = 2438163;
                break;
            case 29:
                ret = 2438187;
                break;
            case 28:
                ret = 2438186;
                break;
            case 24:
                ret = 2438182;
                break;
            case 22:
                ret = 2438180;
                break;
            case 21:
                ret = 2438179;
                break;
            case 20:
                ret = 2438178;
                break;
            case 18:
                ret = 2438177;
                break;
            case 17:
                ret = 2438176;
                break;
            case 16:
                ret = 2438175;
                break;
            case 14:
                ret = 2438173;
                break;
            case 13:
                ret = 2438172;
                break;
            case 10:
                ret = 2438169;
                break;
            case 9:
                ret = 2438168;
                break;
            case 6:
                ret = 2438165;
                break;
            case 3:
                ret = 2438162;
                break;
            case 1:
                ret = 2438160;
                break;
            case 0:
                ret = 2438159;
                break;
        }
        return ret;
    }

    public static int getRidingNum(int itemid) {
        int ret = -1;
        switch (itemid) {
            case 2432149: //낭만가을 라이딩
                ret = 80001398;
                break;
            case 2432151: //비행침대 라이딩
                ret = 80001400;
                break;
            case 2432170: //천사와 함께 라이딩 90일
                ret = 80001262;
                break;
            case 2432218: //니나의 마법진 라이딩
                ret = 80001413;
                break;
            case 2432295: //벨룸 라이딩
                ret = 80001423;
                break;
            case 2432309: //지니 라이딩
                ret = 80001404;
                break;
            case 2432328: //네이버 모자 라이딩 30일
                ret = 80001435;
                break;
            case 2432347: //나인하트와 설원을 라이딩
                ret = 80001440;
                break;
            case 2432348: //시그너스와 설원을 라이딩
                ret = 80001441;
                break;
            case 2432349: //오르카와 설원을 라이딩
                ret = 80001442;
                break;
            case 2432350: //하얀마법사와 설원을 라이딩 1일
                ret = 80001443;
                break;
            case 2432351: //힐라와 설원을 라이딩
                ret = 80001444;
                break;
            case 2432359: //천사 미카엘 라이딩
                ret = 80001445;
                break;
            case 2432361: //악마 루시퍼 라이딩
                ret = 80001447;
                break;
            case 2432418: //BBQ 거대수탉 라이딩 30일
                ret = 80001454;
                break;
            case 2432431: //드래곤나이트 라이딩
                ret = 80001480;
                break;
            case 2432433: //매직 브룸스틱 라이딩
                ret = 80001482;
                break;
            case 2432449: //윙부츠 라이딩
                ret = 80001484;
                break;
            case 2432527: //열기구 O 라이딩 90일
                ret = 80001490;
                break;
            case 2432528: //열기구 X 라이딩 90일
                ret = 80001491;
                break;
            case 2432552: //꿀꿀나비 라이딩
                ret = 80001492;
                break;
            case 2432580: //투명발록 라이딩
                ret = 80001503;
                break;
            case 2432582: //지각했다! 라이딩
                ret = 80001505;
                break;
            case 2432635: //스밴 라이딩 90일
                ret = 80001517;
                break;
            case 2432645: //당나귀 라이딩
                ret = 80001531;
                break;
            case 2432653: //꽃잎 프로펠러 라이딩
                ret = 80001533;
                break;
            case 2432724: //붕붕붕 주황버섯 라이딩
                ret = 80001549;
                break;
            case 2432733: //독수으리! 라이딩
                ret = 80001552;
                break;
            case 2432735: //팬더 라이딩
                ret = 80001550;
                break;
            case 2432751: //헬리콥터 라이딩
                ret = 80001554;
                break;
            case 2432806: //나으리 라이딩
                ret = 80001557;
                break;
            case 2432994: //비기닝 오픈카 라이딩
                ret = 80001561;
                break;
            case 2432995: //라이징 스포츠카 라이딩
                ret = 80001562;
                break;
            case 2432996: //플라잉 스포츠카 라이딩
                ret = 80001563;
                break;
            case 2432997: //뉴 보드 라이딩
                ret = 80001564;
                break;
            case 2432998: //프레시 보드 라이딩
                ret = 80001565;
                break;
            case 2432999: //주니어 보드 라이딩
                ret = 80001566;
                break;
            case 2433000: //시니어 보드 라이딩
                ret = 80001567;
                break;
            case 2433001: //마스터 보드 라이딩
                ret = 80001568;
                break;
            case 2433002: //프라임 보드 라이딩
                ret = 80001569;
                break;
            case 2433003: //샤이닝 벌룬 라이딩
                ret = 80001570;
                break;
            case 2431949: //거대수탉 라이딩
                ret = 80001336;
                break;
            case 2433051: //고양이 손수레 라이딩
                ret = 80001582;
                break;
            case 2433053: //제트스키 라이딩
                ret = 80001584;
                break;
            case 2433499: //오르카의 부축 라이딩
                ret = 80001639;
                break;
            case 2433501: //헬레나의 부축 라이딩
                ret = 80001640;
                break;
            case 2433458: //기사단 전차 라이딩
                ret = 80001642;
                break;
            case 2433459: //발록 라이딩
                ret = 80001643;
                break;
            case 2433460: //블랙 와이번 라이딩
                ret = 80001644;
                break;
            case 2430633: //예티 라이딩 30일
                ret = 80001001;
                break;
            case 2431424: //눈꽃송이 라이딩
                ret = 80001244;
                break;
            case 2433658: //별에서 온 요원 라이딩 30일
                ret = 80001703;
                break;
            case 2433718: //파워드 수트 라이딩
                ret = 80001019;
                break;
            case 2433735: //날아라 푸른양 라이딩
                ret = 80001707;
                break;
            case 2433736: //날아라 분홍양 라이딩
                ret = 80001708;
                break;
            case 2433809: //끌어준다냥 라이딩
                ret = 80001711;
                break;
            case 2433811: //딸기케이크 라이딩
                ret = 80001713;
                break;
            case 2433932: //The MAY.Full 라이딩 30일
                ret = 80001763;
                break;
            case 2433946: //펭귄즈 라이딩
                ret = 80001764;
                break;
            case 2433948: //동물해적단 라이딩
                ret = 80001766;
                break;
            case 2433992: //돼지바 라이딩
                ret = 80001769;
                break;
            case 2434013: //함께해 핑크빈! 라이딩 30일
                ret = 80001775;
                break;
            case 2431473: //핑크빈과 둥둥 여행 라이딩
                ret = 80001257;
                break;
            case 2434077: //잠수정 라이딩
                ret = 80001776;
                break;
            case 2434275: //서핑보드 라이딩
                ret = 80001785;
                break;
            case 2434277: //오로라 밤비니 라이딩
                ret = 80001786;
                break;
            case 2434377: //아기가 되었다 라이딩
                ret = 80001792;
                break;
            case 2434379: //꽃가마 라이딩
                ret = 80001790;
                break;
            case 2434515: //꿈틀꿈틀 라이딩
                ret = 80001811;
                break;
            case 2434517: //밤을 걷는 기차와 나 라이딩
                ret = 80001813;
                break;
            case 2434525: //성조기 모자 라이딩 30일
                ret = 80001814;
                break;
            case 2434526: //성난황소 라이딩 30일
                ret = 80001867;
                break;
            case 2434527: //멀라이언 라이딩 30일
                ret = 80001868;
                break;
            case 2434580: //꺄아 송편 라이딩 30일
                ret = 80001870;
                break;
            case 2431415: //버터플라이 스윙 라이딩
                ret = 80001872;
                break;
            case 2434649: //향기방구 라이딩
                ret = 80001918;
                break;
            case 2434674: //폭주 미메트 라이딩 30일
                ret = 80001920;
                break;
            case 2434728: //게오르크 라이딩
                ret = 80001933;
                break;
            case 2434735: //익룡 라이딩
                ret = 80001921;
                break;
            case 2434737: //종이배 라이딩
                ret = 80001923;
                break;
            case 2434761: //분쇄기 라이딩
                ret = 80001934;
                break;
            case 2434762: //미니 블랙헤븐 라이딩
                ret = 80001935;
                break;
            case 2434967: //루돌프 썰매 라이딩
                ret = 80001942;
                break;
            case 2435089: //스팀 실린더 윙 라이딩
                ret = 80001956;
                break;
            case 2435091: //눈꽃 마법진 라이딩
                ret = 80001958;
                break;
            case 2435112: //개구리 수레 라이딩
                ret = 80001953;
                break;
            case 2435113: //타조 수레 라이딩
                ret = 80001954;
                break;
            case 2435114: //낙타 수레 라이딩
                ret = 80001955;
                break;
            case 2435203: //슈퍼 히어로 라이딩
                ret = 80001975;
                break;
            case 2435205: //귤팽이 라이딩
                ret = 80001977;
                break;
            case 2435296: //스킨 스쿠버 라이딩
                ret = 80001980;
                break;
            case 2435298: //꽃바람 라이딩
                ret = 80001982;
                break;
            case 2435440: //핑크빈과 함께 클래식카 라이딩
                ret = 80001988;
                break;
            case 2435441: //고급 클래식카 라이딩
                ret = 80001989;
                break;
            case 2435442: //구조되다 라이딩
                ret = 80001990;
                break;
            case 2435476: //요상한 몬스터 풍선 라이딩
                ret = 80001991;
                break;
            case 2435517: //별 빛 구름 라이딩
                ret = 80001993;
                break;
            case 2435720: //붕붕붕 핑크빈 라이딩
                ret = 80001995;
                break;
            case 2435722: //블링블링 하트 라이딩
                ret = 80001997;
                break;
            case 2435842: //구름동자 라이딩
                ret = 80002219;
                break;
            case 2435843: //난다 고래 라이딩
                ret = 80002220;
                break;
            case 2435844: //애드벌룬 라이딩
                ret = 80002221;
                break;
            case 2435845: //자이로콥터 라이딩
                ret = 80002222;
                break;
            case 2435965: //핑크 츄츄고래 라이딩
                ret = 80002223;
                break;
            case 2435967: //블루 츄츄고래 라이딩
                ret = 80002225;
                break;
            case 2435986: //화염새 라이딩
                ret = 80002202;
                break;
            case 2436030: //날치 라이딩
                ret = 80002204;
                break;
            case 2436031: //아기 도요새 라이딩
                ret = 80002229;
                break;
            case 2436079: //달 라이딩
                ret = 80001246;
                break;
            case 2436080: //도령 월묘 라이딩
                ret = 80002233;
                break;
            case 2436081: //방패연 라이딩
                ret = 80002234;
                break;
            case 2436126: //흩날려라 한글 라이딩
                ret = 80002235;
                break;
            case 2436183: //파스텔 자전거 라이딩
                ret = 80002236;
                break;
            case 2436185: //콩콩사슴 라이딩
                ret = 80002238;
                break;
            case 2436292: //스윗 도넛 라이딩
                ret = 80002240;
                break;
            case 2436294: //쇼핑카트 라이딩
                ret = 80002242;
                break;
            case 2436405: //아이스 하트 라이딩
                ret = 80002248;
                break;
            case 2436407: //총총 너구리 라이딩
                ret = 80002250;
                break;
            case 2436523: //절친 하늘자전거 라이딩
                ret = 80002270;
                break;
            case 2436524: //까치 라이딩
                ret = 80002259;
                break;
            case 2436525: //럭비 토끼 라이딩
                ret = 80002258;
                break;
            case 2436597: //냥냥 쿠션 라이딩
                ret = 80002261;
                break;
            case 2436599: //푸른불꽃 허스키 라이딩
                ret = 80002262;
                break;
            case 2436610: //까치 바구니 라이딩
                ret = 80002265;
                break;
            case 2436648: //유니온 랭킹 라이딩
                ret = 80002266;
                break;
            case 2436714: //절친 하늘 라이딩
                ret = 80002270;
                break;
            case 2436715: //북극곰 라이딩
                ret = 80002271;
                break;
            case 2436716: //아르카나 바람의 정령 라이딩
                ret = 80002272;
                break;
            case 2436728: //바나달 라이딩
                ret = 80002278;
                break;
            case 2436730: //붕붕 킥보드 라이딩
                ret = 80002277;
                break;
            case 2436778: //꿀냥 꿀냥 라이딩
                ret = 80002287;
                break;
            case 2436780: //키다리 풍선 라이딩
                ret = 80002289;
                break;
            case 2436837: //하트 병자리 라이딩
                ret = 80002295;
                break;
            case 2436839: //우주 별고래 라이딩
                ret = 80002297;
                break;
            case 2436957: //우주선 라이딩
                ret = 80002302;
                break;
            case 2437026: //야광별 라이딩
                ret = 80002304;
                break;
            case 2437040: //총총 사막여우 라이딩
                ret = 80002305;
                break;
            case 2437042: //둥실 무지개 너머 라이딩
                ret = 80002307;
                break;
            case 2437123: //샤이닝 요트 세일링 라이딩
                ret = 80002314;
                break;
            case 2437125: //더키와 목욕 타임 라이딩
                ret = 80002315;
                break;
            case 2437240: //바나나보트 라이딩
                ret = 80002318;
                break;
            case 2437259: //아이스 드래곤 라이딩
                ret = 80002319;
                break;
            case 2437261: //고고 캠핑카 라이딩
                ret = 80002321;
                break;
            case 2437497: //슬라임 빙수기 라이딩
                ret = 80002335;
                break;
            case 2437623: //크리스탈 마법진 라이딩
                ret = 80002345;
                break;
            case 2437625: //돌격! 정령군단 라이딩
                ret = 80002347;
                break;
            case 2437719: //바람은 풍선을 타고 라이딩
                ret = 80002358;
                break;
            case 2437721: //트윙클 솜사탕 라이딩
                ret = 80002356;
                break;
            case 2437737: //돈워리! 라이딩
                ret = 80002354;
                break;
            case 2437738: //날아라 보물지도 라이딩
                ret = 80002361;
                break;
            case 2437794: //베스트 드라이버! 라이딩
                ret = 80002355;
                break;
            case 2437809: //굿 밤 라이딩
                ret = 80002367;
                break;
            case 2437852: //마녀의 빗자루 라이딩
                ret = 80001002;
                break;
            case 2437923: //신나는 발걸음 라이딩
                ret = 80002369;
                break;
            case 2438136: //통통배 라이딩
                ret = 80002382;
                break;
            case 2438137: //증기기관선 라이딩
                ret = 80002383;
                break;
            case 2438138: //크루즈 라이딩
                ret = 80002384;
                break;
            case 2438139: //네이키드 바이크 라이딩
                ret = 80001005;
                break;
            case 2438340: //황금상어 라이딩
                ret = 80002375;
                break;
            case 2438380: //눈사람 콩콩 라이딩
                ret = 80002400;
                break;
            case 2438382: //귤끼리 라이딩 90일 라이딩
                ret = 80002402;
                break;
            case 2438373: //디스커버리 낙하산 라이딩
                ret = 80002392;
                break;
            case 2430935: //더블 부엉이 라이딩
                ret = 80001186;
                break;
            case 2430906: //더블 토끼수레 라이딩 30일
                ret = 80001173;
                break;
            case 2430908: //더블 슈퍼래빗 라이딩 30일
                ret = 80001175;
                break;
            case 2431541: //하늘 자전거 라이딩
                ret = 80001243;
                break;
            case 2431528: //눈꽃송이 라이딩 30일
                ret = 80001244;
                break;
            case 2431529: //비구름 라이딩 30일
                ret = 80001245;
                break;
            case 2431474: //블랙빈과 둥둥 라이딩
                ret = 80001258;
                break;
            case 2431073: //더블 개구리 라이딩
                ret = 80001452;
                break;
            case 2430190: //블루 스쿠터 라이딩
                ret = 80001021;
                break;
            case 2430634: //핑크 스쿠터 라이딩
                ret = 80001006;
                break;
            case 2431494: //타조 라이딩
                ret = 80001051;
                break;
            case 2431495: //예티 라이더 라이딩
                ret = 80001001;
                break;
            case 2430053: //크로코 라이딩 30일
                ret = 80001004;
                break;
            case 2431498: //근두운 라이딩 30일
                ret = 80001007;
                break;
            case 2430057: //경주용 카트 라이딩
                ret = 80001009;
                break;
            case 2431500: //오토바이 라이딩
                ret = 80001450;
                break;
            case 2431501: //돌격! 목마 라이딩
                ret = 80001003;
                break;
            case 2431503: //닭 라이딩
                ret = 80001030;
                break;
            case 2431504: //부엉이 라이딩
                ret = 80001031;
                break;
            case 2431505: //로우 라이더 라이딩
                ret = 80001032;
                break;
            case 2430149: //레오나르도 라이딩
                ret = 80001020;
                break;
            case 2431745: //카푸의 라이딩
                ret = 80001278;
                break;
            case 2431757: //둥실둥실 풍선 라이딩
                ret = 80001285;
                break;
            case 2431760: //플라잉 부엉이 라이딩
                ret = 80001291;
                break;
            case 2431764: //데비존의 포로 라이딩
                ret = 80001289;
                break;
            case 2431765: //흔들린 목마 라이딩
                ret = 80001290;
                break;
            case 2431797: //페가수스 라이딩
                ret = 80001039;
                break;
            case 2431799: //드래곤 라이딩
                ret = 80001302;
                break;
            case 2431898: //바운싱 카 라이딩
                ret = 80001324;
                break;
            case 2430521: //꼬마토끼 라이딩
                ret = 80001044;
                break;
            case 2431915: //펠리컨 라이딩
                ret = 80001327;
                break;
            case 2432003: //전투 비행정 라이딩 10일
                ret = 80001344;
                break;
            case 2432007: //헤카톤의 주먹 라이딩 10일
                ret = 80001345;
                break;
            case 2432015: //붉은 모래구름 라이딩
                ret = 80001333;
                break;
            case 2432029: //구형 배틀쉽 라이딩
                ret = 80001346;
                break;
            case 2432030: //가고일 라이딩
                ret = 80001347;
                break;
            case 2432031: //친구 라이딩
                ret = 80001348;
                break;
            case 2432078: //지옥개 라이딩
                ret = 80001353;
                break;
            case 2432085: //돌고래 라이딩
                ret = 80001355;
                break;
            case 2430091: //나이트메어 라이딩
                ret = 80001014;
                break;
            case 2430506: //버팔로 라이딩
                ret = 80001082;
                break;
            case 2430610: //산타 썰매 라이딩
                ret = 80001022;
                break;
            case 2430937: //더블 막시무스 라이딩
                ret = 80001144;
                break;
            case 2430938: //더블 빨간트럭 라이딩
                ret = 80001148;
                break;
            case 2430939: //더블 파워슈트 라이딩
                ret = 80001149;
                break;
            case 2430794: //스페이스쉽 라이딩
                ret = 80001163;
                break;
            case 2430907: //더블 주황버섯 라이딩
                ret = 80001174;
                break;
            case 2430932: //더블 트랜스폼 라이딩
                ret = 80001183;
                break;
            case 2430933: //더블 나이트메어 라이딩
                ret = 80001184;
                break;
            case 2430934: //더블 신수 라이딩
                ret = 80001185;
                break;
            case 2430936: //더블 곰 열기구 라이딩
                ret = 80001187;
                break;
            case 2431137: //드래고니카 라이딩
                ret = 80001198;
                break;
            case 2431135: //너와 함께 팬텀 라이딩
                ret = 80001220;
                break;
            case 2431136: //너와 함께 아리아 라이딩
                ret = 80001221;
                break;
            case 2431267: //오닉스 재규어 라이딩
                ret = 80001228;
                break;
            case 2434079: //언프리티 헬기스타
                ret = 80001778;
                break;
            case 2433454: //복만이
                ret = 80001023;
                break;
            case 2433349: //스우와 함께 비행정
                ret = 80001628;
                break;
            case 2433347: //베릴과 함께 비행정
                ret = 80001625;
                break;
            case 2630261:
                ret = 80002699;
                break;
            case 2439909:
                ret = 80002660;
                break;
            case 2439913:
                ret = 80002664;
                break;
            case 2630240:
                ret = 80002698;
                break;
            case 2630116:
                ret = 80002668;
                break;
            case 2439933:
                ret = 80002667;
                break;
            case 2433345: //겔리메르와 함께 비행정
                ret = 80001623;
                break;
            case 2433276: //헬레나와 함께 비행정
                ret = 80001621;
                break;
            case 2433274: //지그문트와 함께 비행정
                ret = 80001620;
                break;
            case 2433272: //시그너스와 함께 비행정
                ret = 80001617;
                break;
            case 2432500: //새해 복많이
                ret = 80001510;
                break;
            case 2432498: //푸른불꽃 나이트메어
                ret = 80001508;
                break;
            case 2432293: //호박마차
                ret = 80001421;
                break;
            case 2432291: //스케이트 보드
                ret = 80001420;
                break;
            case 2432216: //좀비트럭
                ret = 80001412;
                break;
            case 2438408: //예핑크스
                ret = 80002417;
                break;
            case 2438409: //껨디와 썰매 라이딩
                ret = 80002418;
                break;
            case 2438486: //날아! 토끼랑 붕붕 라이딩
                ret = 80002429;
                break;
            case 2438488: //돌돌 극세사 이불말이 라이딩
                ret = 80002432;
                break;
            case 2438493: //복조리 라이딩
                ret = 80002427;
                break;
            case 2438494: //이건 약과지 라이딩
                ret = 80002425;
                break;
            case 2438638: //봄날의 오리 라이딩
                ret = 80002433;
                break;
            case 2438640: //무지개 그네 라이딩
                ret = 80002436;
                break;
            case 2438657: //봄바람 튤립 라이딩
                ret = 80002437;
                break;
            case 2438715: //보석새 라이딩
                ret = 80002439;
                break;
            case 2438743: //러브 메신저 라이딩
                ret = 80002441;
                break;
            case 2438745: //셀럽의 슈퍼카 라이딩
                ret = 80002443;
                break;
            case 2438882: //15번가 시티투어 버스 라이딩
                ret = 80002446;
                break;
            case 2438886: //핑크빈 스쿠터 라이딩
                ret = 80002447;
                break;
            case 2439034: //오로라 돌고래 라이딩
                ret = 80002448;
                break;
            case 2439036: //화염마 라이딩
                ret = 80002450;
                break;
            case 2439127: //뽀송 하늘다람쥐 라이딩
                ret = 80002454;
                break;
            case 2439144: //파카파카 라이딩
                ret = 80002424;
                break;
            case 2439266: //워터 썸머 페스티벌 라이딩
                ret = 80002545;
                break;
            case 2439278: //거대 괴수 라이딩
                ret = 80002546;
                break;
            case 2439295: //리버스 다크소울 라이딩
                ret = 80002547;
                break;
            case 2439329: //디어 판타지아 라이딩
                ret = 80002572;
                break;
            case 2439331: //스완 판타지아 라이딩
                ret = 80002573;
                break;
            case 2439406: //어둠의 근원 라이딩
                ret = 80002622;
                break;
            case 2439443: //하프물범 라이딩
                ret = 80002585;
                break;
            case 2439484: //푸드트럭이담 라이딩
                ret = 80002628;
                break;
            case 2439486: //루시드의 날개 라이딩
                ret = 80002630;
                break;
            case 2439666: //트랙터 라이딩
                ret = 80002594;
                break;
            case 2439667: //병아리 라이딩
                ret = 80002595;
                break;
            case 2439675: //이클립스 버드 라이딩
                ret = 80002648;
                break;
            case 2439677: //케이카 라이딩
                ret = 80002650;
                break;
            case 2630279: // 허수아비 라이딩
                ret = 80002702;
                break;

            case 2630386: // 집중! 라이딩
                ret = 80002712;
                break;

            case 2630387: // 모험 가득 블랙빈 라이딩
                ret = 80002713;
                break;

            case 2630448: // 전부 담으시개 라이딩
                ret = 80002714;
                break;

            case 2630451: // 클라우드 라이딩
                ret = 80002715;
                break;

            case 2630452: // 클라우드 라이딩
                ret = 80002716;
                break;

            case 2630476: // 코튼 캔디빈 라이딩
                ret = 80002735;
                break;

            case 2630488: // 뉴트로 화염새 라이딩
                ret = 80002738;
                break;

            case 2630563: // 뉴트로 냉동참치 라이딩
                ret = 80002740;
                break;

            case 2630763: // 글로리온 라이딩
                ret = 80002742;
                break;

            case 2630764: // 도철 라이딩
                ret = 80002743;
                break;

            case 2630765: // 천마 라이딩
                ret = 80002744;
                break;

            case 263076: // 달려라! 은빛갈기!
                ret = 80002748;
                break;

            case 2630570: // 해적 거북이 라이딩
                ret = 80002752;
                break;

            case 2630573: // 뽀송 토끼 라이딩
                ret = 80002754;
                break;

            case 2630575: // 쁘띠 샤크 라이딩
                ret = 80002756;
                break;

            case 2630576: // 쁘띠 샤크 라이딩
                ret = 80002757;
                break;

            case 2430039: // 근두운
                ret = 80002824;
                break;

            case 2630917: // 레드 씨트론 라이딩
                ret = 80002831;
                break;

            case 2630913: // 얼음 마차 라이딩
                ret = 80002843;
                break;

            case 2630914: // 얼음 마차 라이딩
                ret = 80002844;
                break;

            case 2630918: // 블루 씨트론 라이딩
                ret = 80002845;
                break;

            case 2630919: // 옐로우 씨트론 라이딩
                ret = 80002846;
                break;

            case 2630971: // 해치와 함께 라이딩
                ret = 80002853;
                break;

            case 5120198: // 메소팡팡 구름도깨비 라이딩 템없
                ret = 80002854;
                break;

            case 2631140: // 천둥새 라이딩
                ret = 80002855;
                break;

            case 2631136: // 할로윈의 유령 라이딩
                ret = 80002858;
                break;

            case 2631190: // 달려라 보드 라이딩
                ret = 80002859;
                break;

            case 2631191: // 달려라 보드 라이딩
                ret = 80002860;
                break;
            default:
                ret = -1;
                break;
        }
        return ret;
    }

    public static int getRidingItemIdbyNum(int num) {
        int ret = -1;
        switch (num) {
            case 80001398:
                ret = 2432149;
                break;
            case 80001400:
                ret = 2432151;
                break;
            case 80001262:
                ret = 2432170;
                break;
            case 80001413:
                ret = 2432218;
                break;
            case 80001423:
                ret = 2432295;
                break;
            case 80001404:
                ret = 2432309;
                break;
            case 80001435:
                ret = 2432328;
                break;
            case 80001440:
                ret = 2432347;
                break;
            case 80001441:
                ret = 2432348;
                break;
            case 80001442:
                ret = 2432349;
                break;
            case 80001443:
                ret = 2432350;
                break;
            case 80001444:
                ret = 2432351;
                break;
            case 80001445:
                ret = 2432359;
                break;
            case 80001447:
                ret = 2432361;
                break;
            case 80001454:
                ret = 2432418;
                break;
            case 80001480:
                ret = 2432431;
                break;
            case 80001482:
                ret = 2432433;
                break;
            case 80001484:
                ret = 2432449;
                break;
            case 80001490:
                ret = 2432527;
                break;
            case 80001491:
                ret = 2432528;
                break;
            case 80001492:
                ret = 2432552;
                break;
            case 80001503:
                ret = 2432580;
                break;
            case 80001505:
                ret = 2432582;
                break;
            case 80001517:
                ret = 2432635;
                break;
            case 80001531:
                ret = 2432645;
                break;

            case 80002699:
                ret = 2630261;
                break;
            case 80002660:
                ret = 2439909;
                break;
            case 80002664:
                ret = 2439913;
                break;
            case 80002698:
                ret = 2630240;
                break;
            case 80002668:
                ret = 2630116;
                break;
            case 80002667:
                ret = 2439933;
                break;
            case 80001533:
                ret = 2432653;
                break;
            case 80001549:
                ret = 2432724;
                break;
            case 80001552:
                ret = 2432733;
                break;
            case 80001550:
                ret = 2432735;
                break;
            case 80001554:
                ret = 2432751;
                break;
            case 80001557:
                ret = 2432806;
                break;
            case 80001561:
                ret = 2432994;
                break;
            case 80001562:
                ret = 2432995;
                break;
            case 80001563:
                ret = 2432996;
                break;
            case 80001564:
                ret = 2432997;
                break;
            case 80001565:
                ret = 2432998;
                break;
            case 80001566:
                ret = 2432999;
                break;
            case 80001567:
                ret = 2433000;
                break;
            case 80001568:
                ret = 2433001;
                break;
            case 80001569:
                ret = 2433002;
                break;
            case 80001570:
                ret = 2433003;
                break;
            case 80001336:
                ret = 2431949;
                break;
            case 80001582:
                ret = 2433051;
                break;
            case 80001584:
                ret = 2433053;
                break;
            case 80001639:
                ret = 2433499;
                break;
            case 80001640:
                ret = 2433501;
                break;
            case 80001642:
                ret = 2433458;
                break;
            case 80001643:
                ret = 2433459;
                break;
            case 80001644:
                ret = 2433460;
                break;
            case 80001001:
                ret = 2430633;
                break;
            case 80001244:
                ret = 2431424;
                break;
            case 80001703:
                ret = 2433658;
                break;
            case 80001019:
                ret = 2433718;
                break;
            case 80001707:
                ret = 2433735;
                break;
            case 80001708:
                ret = 2433736;
                break;
            case 80001711:
                ret = 2433809;
                break;
            case 80001713:
                ret = 2433811;
                break;
            case 80001763:
                ret = 2433932;
                break;
            case 80001764:
                ret = 2433946;
                break;
            case 80001766:
                ret = 2433948;
                break;
            case 80001769:
                ret = 2433992;
                break;
            case 80001775:
                ret = 2434013;
                break;
            case 80001257:
                ret = 2431473;
                break;
            case 80001776:
                ret = 2434077;
                break;
            case 80001785:
                ret = 2434275;
                break;
            case 80001786:
                ret = 2434277;
                break;
            case 80001792:
                ret = 2434377;
                break;
            case 80001790:
                ret = 2434379;
                break;
            case 80001811:
                ret = 2434515;
                break;
            case 80001813:
                ret = 2434517;
                break;
            case 80001814:
                ret = 2434525;
                break;
            case 80001867:
                ret = 2434526;
                break;
            case 80001868:
                ret = 2434527;
                break;
            case 80001870:
                ret = 2434580;
                break;
            case 80001872:
                ret = 2431415;
                break;
            case 80001918:
                ret = 2434649;
                break;
            case 80001920:
                ret = 2434674;
                break;
            case 80001933:
                ret = 2434728;
                break;
            case 80001921:
                ret = 2434735;
                break;
            case 80001923:
                ret = 2434737;
                break;
            case 80001934:
                ret = 2434761;
                break;
            case 80001935:
                ret = 2434762;
                break;
            case 80001942:
                ret = 2434967;
                break;
            case 80001956:
                ret = 2435089;
                break;
            case 80001958:
                ret = 2435091;
                break;
            case 80001953:
                ret = 2435112;
                break;
            case 80001954:
                ret = 2435113;
                break;
            case 80001955:
                ret = 2435114;
                break;
            case 80001975:
                ret = 2435203;
                break;
            case 80001977:
                ret = 2435205;
                break;
            case 80001980:
                ret = 2435296;
                break;
            case 80001982:
                ret = 2435298;
                break;
            case 80001988:
                ret = 2435440;
                break;
            case 80001989:
                ret = 2435441;
                break;
            case 80001990:
                ret = 2435442;
                break;
            case 80001991:
                ret = 2435476;
                break;
            case 80001993:
                ret = 2435517;
                break;
            case 80001995:
                ret = 2435720;
                break;
            case 80001997:
                ret = 2435722;
                break;
            case 80002219:
                ret = 2435842;
                break;
            case 80002220:
                ret = 2435843;
                break;
            case 80002221:
                ret = 2435844;
                break;
            case 80002222:
                ret = 2435845;
                break;
            case 80002223:
                ret = 2435965;
                break;
            case 80002225:
                ret = 2435967;
                break;
            case 80002202:
                ret = 2435986;
                break;
            case 80002204:
                ret = 2436030;
                break;
            case 80002229:
                ret = 2436031;
                break;
            case 80001246:
                ret = 2436079;
                break;
            case 80002233:
                ret = 2436080;
                break;
            case 80002234:
                ret = 2436081;
                break;
            case 80002235:
                ret = 2436126;
                break;
            case 80002236:
                ret = 2436183;
                break;
            case 80002238:
                ret = 2436185;
                break;
            case 80002240:
                ret = 2436292;
                break;
            case 80002242:
                ret = 2436294;
                break;
            case 80002248:
                ret = 2436405;
                break;
            case 80002250:
                ret = 2436407;
                break;
            case 80002270:
                ret = 2436523;
                break;
            case 80002259:
                ret = 2436524;
                break;
            case 80002258:
                ret = 2436525;
                break;
            case 80002261:
                ret = 2436597;
                break;
            case 80002262:
                ret = 2436599;
                break;
            case 80002265:
                ret = 2436610;
                break;
            case 80002266:
                ret = 2436648;
                break;
            case 80002271:
                ret = 2436715;
                break;
            case 80002272:
                ret = 2436716;
                break;
            case 80002278:
                ret = 2436728;
                break;
            case 80002277:
                ret = 2436730;
                break;
            case 80002287:
                ret = 2436778;
                break;
            case 80002289:
                ret = 2436780;
                break;
            case 80002295:
                ret = 2436837;
                break;
            case 80002297:
                ret = 2436839;
                break;
            case 80002302:
                ret = 2436957;
                break;
            case 80002304:
                ret = 2437026;
                break;
            case 80002305:
                ret = 2437040;
                break;
            case 80002307:
                ret = 2437042;
                break;
            case 80002314:
                ret = 2437123;
                break;
            case 80002315:
                ret = 2437125;
                break;
            case 80002318:
                ret = 2437240;
                break;
            case 80002319:
                ret = 2437259;
                break;
            case 80002321:
                ret = 2437261;
                break;
            case 80002335:
                ret = 2437497;
                break;
            case 80002345:
                ret = 2437623;
                break;
            case 80002347:
                ret = 2437625;
                break;
            case 80002358:
                ret = 2437719;
                break;
            case 80002356:
                ret = 2437721;
                break;
            case 80002354:
                ret = 2437737;
                break;
            case 80002361:
                ret = 2437738;
                break;
            case 80002355:
                ret = 2437794;
                break;
            case 80002367:
                ret = 2437809;
                break;
            case 80001002:
                ret = 2437852;
                break;
            case 80002369:
                ret = 2437923;
                break;
            case 80002382:
                ret = 2438136;
                break;
            case 80002383:
                ret = 2438137;
                break;
            case 80002384:
                ret = 2438138;
                break;
            case 80001005:
                ret = 2438139;
                break;
            case 80002375:
                ret = 2438340;
                break;
            case 80002400:
                ret = 2438380;
                break;
            case 80002402:
                ret = 2438382;
                break;
            case 80002392:
                ret = 2438373;
                break;
            case 80001186:
                ret = 2430935;
                break;
            case 80001173:
                ret = 2430906;
                break;
            case 80001175:
                ret = 2430908;
                break;
            case 80001243:
                ret = 2431541;
                break;
            case 80001245:
                ret = 2431529;
                break;
            case 80001258:
                ret = 2431474;
                break;
            case 80001452:
                ret = 2431073;
                break;
            case 80001021:
                ret = 2430190;
                break;
            case 80001006:
                ret = 2430634;
                break;
            case 80001051:
                ret = 2431494;
                break;
            case 80001004:
                ret = 2430053;
                break;
            case 80001007:
                ret = 2431498;
                break;
            case 80001009:
                ret = 2430057;
                break;
            case 80001450:
                ret = 2431500;
                break;
            case 80001003:
                ret = 2431501;
                break;
            case 80001030:
                ret = 2431503;
                break;
            case 80001031:
                ret = 2431504;
                break;
            case 80001032:
                ret = 2431505;
                break;
            case 80001020:
                ret = 2430149;
                break;
            case 80001278:
                ret = 2431745;
                break;
            case 80001285:
                ret = 2431757;
                break;
            case 80001291:
                ret = 2431760;
                break;
            case 80001289:
                ret = 2431764;
                break;
            case 80001290:
                ret = 2431765;
                break;
            case 80001039:
                ret = 2431797;
                break;
            case 80001302:
                ret = 2431799;
                break;
            case 80001324:
                ret = 2431898;
                break;
            case 80001044:
                ret = 2430521;
                break;
            case 80001327:
                ret = 2431915;
                break;
            case 80001344:
                ret = 2432003;
                break;
            case 80001345:
                ret = 2432007;
                break;
            case 80001333:
                ret = 2432015;
                break;
            case 80001346:
                ret = 2432029;
                break;
            case 80001347:
                ret = 2432030;
                break;
            case 80001348:
                ret = 2432031;
                break;
            case 80001353:
                ret = 2432078;
                break;
            case 80001355:
                ret = 2432085;
                break;
            case 80001014:
                ret = 2430091;
                break;
            case 80001082:
                ret = 2430506;
                break;
            case 80001022:
                ret = 2430610;
                break;
            case 80001144:
                ret = 2430937;
                break;
            case 80001148:
                ret = 2430938;
                break;
            case 80001149:
                ret = 2430939;
                break;
            case 80001163:
                ret = 2430794;
                break;
            case 80001174:
                ret = 2430907;
                break;
            case 80001183:
                ret = 2430932;
                break;
            case 80001184:
                ret = 2430933;
                break;
            case 80001185:
                ret = 2430934;
                break;
            case 80001187:
                ret = 2430936;
                break;
            case 80001198:
                ret = 2431137;
                break;
            case 80001220:
                ret = 2431135;
                break;
            case 80001221:
                ret = 2431136;
                break;
            case 80001228:
                ret = 2431267;
                break;
            case 80001778:
                ret = 2434079;
                break;
            case 80001023:
                ret = 2433454;
                break;
            case 80001628:
                ret = 2433349;
                break;
            case 80001625:
                ret = 2433347;
                break;
            case 80001623:
                ret = 2433345;
                break;
            case 80001621:
                ret = 2433276;
                break;
            case 80001620:
                ret = 2433274;
                break;
            case 80001617:
                ret = 2433272;
                break;
            case 80001510:
                ret = 2432500;
                break;
            case 80001508:
                ret = 2432498;
                break;
            case 80001421:
                ret = 2432293;
                break;
            case 80001420:
                ret = 2432291;
                break;
            case 80001412:
                ret = 2432216;
                break;
            case 80002417:
                ret = 2438408;
                break;
            case 80002418:
                ret = 2438409;
                break;
            case 80002429:
                ret = 2438486;
                break;
            case 80002432:
                ret = 2438488;
                break;
            case 80002427:
                ret = 2438493;
                break;
            case 80002425:
                ret = 2438494;
                break;
            case 80002433:
                ret = 2438638;
                break;
            case 80002436:
                ret = 2438640;
                break;
            case 80002437:
                ret = 2438657;
                break;
            case 80002439:
                ret = 2438715;
                break;
            case 80002441:
                ret = 2438743;
                break;
            case 80002443:
                ret = 2438745;
                break;
            case 80002446:
                ret = 2438882;
                break;
            case 80002447:
                ret = 2438886;
                break;
            case 80002448:
                ret = 2439034;
                break;
            case 80002450:
                ret = 2439036;
                break;
            case 80002454:
                ret = 2439127;
                break;
            case 80002424:
                ret = 2439144;
                break;
            case 80002545:
                ret = 2439266;
                break;
            case 80002546:
                ret = 2439278;
                break;
            case 80002547:
                ret = 2439295;
                break;
            case 80002572:
                ret = 2439329;
                break;
            case 80002573:
                ret = 2439331;
                break;
            case 80002622:
                ret = 2439406;
                break;
            case 80002585:
                ret = 2439443;
                break;
            case 80002628:
                ret = 2439484;
                break;
            case 80002630:
                ret = 2439486;
                break;
            case 80002594:
                ret = 2439666;
                break;
            case 80002595:
                ret = 2439667;
                break;
            case 80002648:
                ret = 2439675;
                break;
            case 80002650:
                ret = 2439677;
                break;
            case 80002702: // 허수아비 라이딩
                ret = 2630279;
                break;

            case 80002712: // 집중! 라이딩
                ret = 2630386;
                break;

            case 80002713: // 모험 가득 블랙빈 라이딩
                ret = 2630387;
                break;

            case 80002714: // 전부 담으시개 라이딩
                ret = 2630448;
                break;

            case 80002715: // 클라우드 라이딩
                ret = 2630451;
                break;

            case 80002716: // 클라우드 라이딩
                ret = 2630452;
                break;

            case 80002735: // 코튼 캔디빈 라이딩
                ret = 2630476;
                break;

            case 80002738: // 뉴트로 화염새 라이딩
                ret = 2630488;
                break;

            case 80002740: // 뉴트로 냉동참치 라이딩
                ret = 2630563;
                break;

            case 80002742: // 글로리온 라이딩
                ret = 2630763;
                break;

            case 80002743: // 도철 라이딩
                ret = 2630764;
                break;

            case 80002744: // 천마 라이딩
                ret = 2630765;
                break;

            case 80002748: // 달려라! 은빛갈기!
                ret = 2630766;
                break;

            case 80002752: // 해적 거북이 라이딩
                ret = 2630570;
                break;

            case 80002754: // 뽀송 토끼 라이딩
                ret = 2630573;
                break;

            case 80002756: // 쁘띠 샤크 라이딩
                ret = 2630575;
                break;

            case 80002757: // 쁘띠 샤크 라이딩
                ret = 2630576;
                break;

            case 80002824: // 근두운
                ret = 2430039;
                break;

            case 80002831: // 레드 씨트론 라이딩
                ret = 2630917;
                break;

            case 80002843: // 얼음 마차 라이딩
                ret = 2630913;
                break;

            case 80002844: // 얼음 마차 라이딩
                ret = 2630914;
                break;

            case 80002845: // 블루 씨트론 라이딩
                ret = 2630918;
                break;

            case 80002846: // 옐로우 씨트론 라이딩
                ret = 2630919;
                break;

            case 80002853: // 해치와 함께 라이딩
                ret = 2630971;
                break;

            case 80002854: // 메소팡팡 구름도깨비 라이딩 템없
                ret = 5120198;
                break;

            case 80002855: // 천둥새 라이딩
                ret = 2631140;
                break;

            case 80002858: // 할로윈의 유령 라이딩
                ret = 2631136;
                break;

            case 80002859: // 달려라 보드 라이딩
                ret = 2631190;
                break;

            case 80002860: // 달려라 보드 라이딩
                ret = 2631191;
                break;
        }
        return ret;
    }

    public static int getDSkinNum(int itemid) {
        int ret = -1;
        switch (itemid) {
            case 2631493: // 스테인드글라스
                ret = 269;
                break;
            case 2631492: // 스테인드글라스
                ret = 269;
                break;
            case 2631472: // 로얄 클래식
                ret = 268;
                break;
            case 2631471: // 로얄 클래식
                ret = 268;
                break;
            case 2631452: // 종이리본 유닛
                ret = 267;
                break;
            case 2631451: // 종이리본 유닛
                ret = 267;
                break;
            case 2631402: // 럭키세븐
                ret = 266;
                break;
            case 2631401: // 럭키세븐
                ret = 266;
                break;
            case 2631189: // 마스터 쉐도우
                ret = 264;
                break;
            case 2631184: // 무릉도장 격파
                ret = 265;
                break;
            case 2631183: // 무릉도장 격파
                ret = 265;
                break;
            case 2631138: // 할로윈
                ret = 20;
                break;
            case 2631137: // 좀비
                ret = 50;
                break;
            case 2631135: // 할로윈의 밤
                ret = 263;
                break;
            case 2631134: // 할로윈의 밤
                ret = 263;
                break;
            case 2631098: // 불꽃
                ret = 262;
                break;
            case 2631097: // 불꽃
                ret = 262;
                break;
            case 2631095: // 6000일 축하해 유닛
                ret = 261;
                break;
            case 2631091: // 6000일 축하해 유닛
                ret = 261;
                break;
            case 2631094: // 6000일 축하해
                ret = 260;
                break;
            case 2631090: // 6000일 축하해
                ret = 260;
                break;
            case 2630970: // 구미호 유닛
                ret = 259;
                break;
            case 2630969: // 구미호 유닛
                ret = 259;
                break;
            case 2630804: // 글로리온
                ret = 258;
                break;
            case 2630780: // 글로리온
                ret = 258;
                break;
            case 2630766: // 패스파인더
                ret = 233;
                break;
            case 2630754: // 글로리온 유닛
                ret = 257;
                break;
            case 2630753: // 글로리온 유닛
                ret = 257;
                break;
            case 2630752: // 홍염
                ret = 256;
                break;
            case 2630751: // 홍염
                ret = 256;
                break;
            case 2630750: // 새벽
                ret = 255;
                break;
            case 2630749: // 새벽
                ret = 255;
                break;
            case 2630748: // 애쉬
                ret = 254;
                break;
            case 2630747: // 애쉬
                ret = 254;
                break;
            case 2630746: // 천족
                ret = 253;
                break;
            case 2630745: // 천족
                ret = 253;
                break;
            case 2630744: // 호영
                ret = 252;
                break;
            case 2630743: // 호영
                ret = 252;
                break;
            case 2630653: // 극한돌파
                ret = 251;
                break;
            case 2630652: // 극한돌파
                ret = 251;
                break;
            case 2630561: // 뉴트로 13주년 단풍잎
                ret = 250;
                break;
            case 2630560: // 뉴트로 13주년 단풍잎
                ret = 250;
                break;
            case 2630559: // 뉴트로 크로우
                ret = 249;
                break;
            case 2630558: // 뉴트로 크로우
                ret = 249;
                break;
            case 2630557: // 뉴트로 하트뿅뿅
                ret = 248;
                break;
            case 2630556: // 뉴트로 하트뿅뿅
                ret = 248;
                break;
            case 2630555: // 뉴트로 폭염
                ret = 247;
                break;
            case 2630554: // 뉴트로 폭염
                ret = 247;
                break;
            case 2630553: // 뉴트로 스타플래닛
                ret = 246;
                break;
            case 2630552: // 뉴트로 스타플래닛
                ret = 246;
                break;
            case 2630517: // 달콤 복숭아
                ret = 230;
                break;
            case 2630516: // 달콤 복숭아
                ret = 230;
                break;
            case 2630486: // 뉴트로 V 네온 데미지 스킨
                ret = 245;
                break;
            case 2630485: // 뉴트로 V 네온 데미지 스킨
                ret = 245;
                break;
            case 2630484: // 뉴트로 눈송송 데미지 스킨
                ret = 244;
                break;
            case 2630483: // 뉴트로 눈송송 데미지 스킨
                ret = 244;
                break;
            case 2630482: // 뉴트로 기본 데미지 스킨
                ret = 243;
                break;
            case 2630481: // 뉴트로 기본 데미지 스킨
                ret = 243;
                break;
            case 2630480: // 뉴트로 별별 데미지 스킨
                ret = 242;
                break;
            case 2630479: // 뉴트로 별별 데미지 스킨
                ret = 242;
                break;
            case 2630478: // 뉴트로 별별 데미지 스킨
                ret = 241;
                break;
            case 2630477: // 뉴트로 별별 데미지 스킨
                ret = 241;
                break;
            /*    case 2630437: // 대세는 토벤머리 데미지 스킨
             ret = 240;
             break;*/
            case 2630436: // 대세는 토벤머리 데미지 스킨
                ret = 240;
                break;
            case 2630434: // 봄날의 강아지 데미지 스킨
                ret = 239;
                break;
            case 2630421: // 봄날의 강아지 데미지 스킨
                ret = 239;
                break;
            case 2630400: // 드림래빗 데미지 스킨
                ret = 238;
                break;
            case 2630385: // 스네이크 데미지 스킨
                ret = 237;
                break;
            case 2630384: // 스네이크 데미지 스킨
                ret = 237;
                break;
            case 2630381: // 어드벤처 데미지 스킨 (유닛)
                ret = 231;
                break;
            case 2630380: // 어드벤처 데미지 스킨 (유닛)
                ret = 231;
                break;
            case 2630269: // 월묘의 떡 데미지 스킨
                ret = 1030;
                break;
            case 2630268: // 월묘의 떡 데미지 스킨
                ret = 1030;
                break;
            case 2630267: // 꿀꿀 데미지 스킨
                ret = 236;
                break;
            case 2630266: // 꿀꿀 데미지 스킨
                ret = 236;
                break;
            case 2630265: // 쉐도우 데미지 스킨
                ret = 235;
                break;
            case 2630264: // 쉐도우 데미지 스킨
                ret = 235;
                break;
            case 2630263: // 서펜트 데미지 스킨
                ret = 234;
                break;
            case 2630262: // 서펜트 데미지 스킨
                ret = 234;
                break;
            case 2630235: // 배틀 호라이즌 데미지 스킨
                ret = 232;
                break;
            case 2630225: // 패스파인더 데미지 스킨
                ret = 233;
                break;
            case 2630224: // 패스파인더 데미지 스킨
                ret = 233;
                break;
            case 2630222: // 온 더 레드 데미지 스킨
                ret = 228;
                break;
            case 2630214: // 종이접기
                ret = 230;
                break;
            case 2630178: // 전설의 데미지 ver2
                ret = 229;
                break;
            case 2630137: // 어드벤처 데미지
                ret = 227;
                break;
            case 2439927: // 샤방
                ret = 225;
                break;
            case 2439925: // 탐정사무소페페
                ret = 224;
                break;
            case 2439769: // 할로캣2
                ret = 223;
                break;
            case 2439686: // 한글날 궁서체 데미지 스킨(한글)
                ret = 222;
                break;
            case 2439684: // 한글날 데미지 스킨(한글)
                ret = 221;
                break;
            case 2439665: // 추수
                ret = 219;
                break;
            case 2439617: // 챌린지
                ret = 218;
                break;
            case 2439572: // 스타플래닛 데미지 스킨 (유닛)
                ret = 217;
                break;
            case 2439408: // 검은 사슬
                ret = 216;
                break;
            case 2439395: // 미궁의 화염 데미지 스킨 (유닛)
                ret = 215;
                break;
            case 2439393: // 미궁
                ret = 214;
                break;
            case 2439381: // 마스터 스텔라
                ret = 213;
                break;
            case 2439338: // 연합
                ret = 212;
                break;
            case 2439337: // 안개 데미지 스킨 (유닛)
                ret = 211;
                break;
            case 2439336: // 안개
                ret = 210;
                break;
            case 2439298: // 신수
                ret = 209;
                break;
            case 2438871: // 기본 데미지 스킨 (유닛)
                ret = 208;
                break;
            case 2438885: // 15번가
                ret = 207;
                break;
            case 2438881: // 몬스터
                ret = 206;
                break;
            case 2438713: // 축구 유니폼
                ret = 205;
                break;
            case 2438672: // 엉망진창
                ret = 204;
                break;
            case 2438637: // 그림일기
                ret = 203;
                break;
            case 2438530: // 십이지
                ret = 202;
                break;
            case 2438492: // 동글 초코
                ret = 201;
                break;
            case 2438485: // 레드 써킷
                ret = 200;
                break;
            case 2438419: // 하이브리드
                ret = 199;
                break;
            case 2438417: // 천공
                ret = 198;
                break;
            case 2438415: // 에스페라
                ret = 197;
                break;
            case 2438413: // 디스커버리
                ret = 196;
                break;
            case 2438379: // 설렘하트
                ret = 195;
                break;
            case 2438378: // 왈왈
                ret = 194;
                break;
            case 2438353: // 아크
                ret = 193;
                break;
            case 2438147: // 슈퍼스타:
                ret = 192;
                break;
            case 2438146: // 시간의 초월자:
                ret = 191;
                break;
            case 2437877: // 마스터 블러드
                ret = 190;
                break;
            case 2438315: // 샤방샤방2:
                ret = 189;
                break;
            case 2438314: // 팝콘:
                ret = 188;
                break;
            case 2438313: // 송편:
                ret = 187;
                break;
            case 2438312: // 메이플스토리체:
                ret = 186;
                break;
            case 2438311: // 라이트닝:
                ret = 185;
                break;
            case 2438310: // 일리움:
                ret = 184;
                break;
            case 2438309: // 야자수:
                ret = 183;
                break;
            case 2438308: // 돈많이:
                ret = 182;
                break;
            case 2438307: // 소나기:
                ret = 181;
                break;
            case 2438306: // 먹구름:
                ret = 180;
                break;
            case 2438305: // 체스:
                ret = 179;
                break;
            case 2438304: // 퍼즐:
                ret = 178;
                break;
            case 2438303: // 블랙로즈:
                ret = 177;
                break;
            case 2438302: // 카데나:
                ret = 176;
                break;
            case 2438301: // 파티 퀘스트
                ret = 175;
                break;
            case 2438300: // 매지컬스타:
                ret = 174;
                break;
            case 2438299: // 일루미네이션
                ret = 173;
                break;
            case 2438298: // 솔루나:
                ret = 172;
                break;
            case 2438297: // 아이스크림:
                ret = 171;
                break;
            case 2438296: // 외계인:
                ret = 170;
                break;
            case 2438295: // 별자리:
                ret = 169;
                break;
            case 2438294: // 이볼빙:
                ret = 168;
                break;
            case 2438293: // 꿀꿀비:
                ret = 167;
                break;
            case 2438292: // XOXO:
                ret = 166;
                break;
            case 2438291: // 병아리:
                ret = 165;
                break;
            case 2438290: // 앱솔랩스:
                ret = 164;
                break;
            case 2438289: // 파프니르:
                ret = 163;
                break;
            case 2438288: // 엠퍼러스:
                ret = 162;
                break;
            case 2438287: // 아르카나:
                ret = 161;
                break;
            case 2436596: // 마스터 아이스
                ret = 160;
                break;
            case 2438286: // 감나무:
                ret = 159;
                break;
            case 2438285: // 까치 깃털:
                ret = 158;
                break;
            case 2438284: // 라떼:
                ret = 157;
                break;
            case 2438283: // 메카
                ret = 156;
                break;
            case 2438282: // 전설의 데미지 스킨:
                ret = 155;
                break;
            case 2438281: // 여신의 흔적
                ret = 154;
                break;
            case 2438280: // 양
                ret = 153;
                break;
            case 2438279: // 하트뿅뿅:
                ret = 152;
                break;
            case 2438278: // 눈송송:
                ret = 150;
                break;
            case 2438277: // 보안관:
                ret = 149;
                break;
            case 2438276: // 한계돌파:
                ret = 148;
                break;
            case 2438275: // 상형문자:
                ret = 147;
                break;
            case 2438274: // 유물:
                ret = 146;
                break;
            case 2438273: // 호빵:
                ret = 145;
                break;
            case 2438272: // 샤방샤방:
                ret = 144;
                break;
            case 2438271: // 할로캣:
                ret = 143;
                break;
            case 2436182: // 고스트
                ret = 142;
                break;
            case 2438270: // 명탐정:
                ret = 141;
                break;
            case 2438269: // 은행나무:
                ret = 140;
                break;
            case 2438268: // 한글날 궁서체:
                ret = 139;
                break;
            case 2438267: // 문라이트:
                ret = 138;
                break;
            case 2438266: // 퓨리:
                ret = 137;
                break;
            case 2438265: // 빛과 어둠:
                ret = 136;
                break;
            case 2438264: // 알밤:
                ret = 135;
                break;
            case 2438263: // 별빛 오로라:
                ret = 134;
                break;
            case 2438262: // 콜라쥬:
                ret = 133;
                break;
            case 2438261: // 뮤직파워:
                ret = 132;
                break;
            case 2438260: // 블루 스트라이크:
                ret = 131;
                break;
            case 2438259: // 포이즌 플레임:
                ret = 130;
                break;
            case 2438258: // 레헬른:
                ret = 129;
                break;
            case 2438257: // 츄츄
                ret = 128;
                break;
            case 2438256: // 소멸의 여로:
                ret = 127;
                break;
            case 2438255: // 만우절 (흑백 궁서체):
                ret = 126;
                break;
            case 2438254: // 데모닉:
                ret = 125;
                break;
            case 2438253: // 홀리:
                ret = 124;
                break;
            case 2438252: // 스페이드:
                ret = 123;
                break;
            case 2438251: // V 네온:
                ret = 122;
                break;
            case 2438250: // 라운딩:
                ret = 121;
                break;
            case 2438249: // 크라운:
                ret = 120;
                break;
            case 2435524: // 스파크
                ret = 119;
                break;
            case 2435523: // 초콜릿
                ret = 118;
                break;
            case 2435521: // 크로우
                ret = 117;
                break;
            case 2435516: // 투명
                ret = 115;
                break;
            case 2438248: // 나노픽셀:
                ret = 114;
                break;
            case 2438247: // 퍼플:
                ret = 113;
                break;
            case 2438246: // 푸른화염:
                ret = 112;
                break;
            case 2438245: // 시크릿 수학:
                ret = 111;
                break;
            case 2438244: // 리프레:
                ret = 110;
                break;
            case 2438243: // 헤네시스:
                ret = 109;
                break;
            case 2438242: // 만우절 컬러 궁서체
                ret = 108;
                break;
            case 2438241: // 만우절 둥근체
                ret = 107;
                break;
            case 2438240: // 몬스터풍선:
                ret = 106;
                break;
            case 2438239: // 러블리:
                ret = 105;
                break;
            case 2438238: // 초코도넛
                ret = 104;
                break;
            case 2438237: // 스페이스:
                ret = 103;
                break;
            case 2438236: // 전자 방식:
                ret = 102;
                break;
            case 2438235: // 13주년 단풍잎:
                ret = 101;
                break;
            case 2438234: // 헤이스트:
                ret = 100;
                break;
            case 2438233: // 만우절 컬러 궁서체:
                ret = 99;
                break;
            case 2438232: // 만우절 흑백 궁서체:
                ret = 98;
                break;
            case 2438231: // 만우절 이펙트체:
                ret = 97;
                break;
            case 2438230: // 만우절 둥근체:
                ret = 96;
                break;
            case 2438229: // 블랙데이:
                ret = 95;
                break;
            case 2438228: // 만우절 오리지널:
                ret = 94;
                break;
            case 2438227: // 페스티벌 별주부
                ret = 93;
                break;
            case 2438226: // 근성의 숲:
                ret = 92;
                break;
            case 2438225: // 월묘 데미지 스킨:
                ret = 91;
                break;
            case 2438224: // 악보 데미지 스킨:
                ret = 90;
                break;
            case 2438223: // 도넛 데미지 스킨:
                ret = 89;
                break;
            case 2438222: // 미호 데미지 스킨:
                ret = 88;
                break;
            case 2438221: // 스노윙 데미지 스킨:
                ret = 87;
                break;
            case 2438220: // 익스플로전 데미지 스킨:
                ret = 86;
                break;
            case 2438219: // 붓글씨 데미지 스킨:
                ret = 85;
                break;
            case 2438218: // 앤티크 골드 데미지 스킨:
                ret = 84;
                break;
            case 2438217: // 캔디 데미지 스킨:
                ret = 83;
                break;
            case 2438216: // 얼음땡 데미지 스킨:
                ret = 82;
                break;
            case 2438215: // 네온사인 데미지 스킨:
                ret = 81;
                break;
            case 2438214: // 하트풍선 데미지 스킨:
                ret = 80;
                break;
            case 2438213: // 폭죽 데미지 스킨:
                ret = 79;
                break;
            case 2438212: // 히어로즈 메르세데스 데미지 스킨:
                ret = 78;
                break;
            case 2438211: // 히어로즈 팬텀 데미지 스킨:
                ret = 77;
                break;
            case 2438210: // 크리스마스 전구 데미지 스킨:
                ret = 76;
                break;
            case 2438209: // 소프트 콘 데미지 스킨:
                ret = 75;
                break;
            case 2438208: // 젤리빈 데미지 스킨:
                ret = 74;
                break;
            case 2438200: // 햇님이 반짝:
                ret = 73;
                break;
            case 2438199: // 만월
                ret = 72;
                break;
            case 2438198: // 낙서쟁이 데니스
                ret = 71;
                break;
            case 2438197: // 싱가폴 야경
                ret = 70;
                break;
            case 2438196: // 츄러스
                ret = 69;
                break;
            case 2438195: // USA
                ret = 68;
                break;
            case 2438194: // 폭염
                ret = 67;
                break;
            case 2438192: // 곰돌이
                ret = 66;
                break;
            case 2438190: // 마시멜로
                ret = 65;
                break;
            case 2438189: // 밤하늘
                ret = 64;
                break;
            case 2438188: // 무지개 봉봉
                ret = 63;
                break;
            case 2438185: // 슬라임X주황버섯
                ret = 62;
                break;
            case 2438184: // 예티X페페
                ret = 61;
                break;
            case 2438183: // 별별
                ret = 60;
                break;
            case 2438181: // 색동
                ret = 59;
                break;
            case 2438171: // 살랑살랑 봄바람
                ret = 58;
                break;
            case 2438166: // 메리 크리스마스
                ret = 57;
                break;
            case 2438164: // 달콤한 전통 한과
                ret = 56;
                break;
            case 2438161: // 크리티아스
                ret = 55;
                break;
            case 2435172: // 디지털 라이즈
                ret = 54;
                break;
            case 2438207: // 몬스터 파크 데미지 스킨
                ret = 53;
                break;
            case 2438206: // 블랙헤븐 데미지 스킨
                ret = 52;
                break;
            case 2438205: // MVP 스페셜 전용 데미지 스킨
                ret = 51;
                break;
            case 2438204: // 좀비 데미지 스킨:
                ret = 50;
                break;
            case 2438203: // 구미호 데미지 스킨:
                ret = 49;
                break;
            case 2438202: // 무르무르 데미지 스킨:
                ret = 48;
                break;
            case 2438201: // 노히메 데미지 스킨
                ret = 47;
                break;
            case 2438193: // 파왕 데미지 스킨:
                ret = 39;
                break;
            case 2438191: // 무릉도장 데미지 스킨:
                ret = 37;
                break;
            case 2438167: // 눈 꽃송이
                ret = 33;
                break;
            case 2438174: // 주황버섯 데미지 스킨:
                ret = 32;
                break;
            case 2438170: // 키보드워리어
                ret = 31;
                break;
            case 2438163: // 임팩티브 [중복]
                ret = 30;
                break;
            case 2438187: // 돼지바 데미지 스킨:
                ret = 29;
                break;
            case 2438186: // 핑크빈 데미지 스킨:
                ret = 28;
                break;
            case 2438182: // 커플부대 데미지 스킨:
                ret = 24;
                break;
            case 2438180: // 네네치킨 데미지 스킨:
                ret = 22;
                break;
            case 2438179: // 한글날:
                ret = 21;
                break;
            case 2438178: // 할로윈 데미지 스킨:
                ret = 20;
                break;
            case 2438177: // 스타플래닛 데미지 스킨:
                ret = 18;
                break;
            case 2438176: // 모노톤 데미지 스킨:
                ret = 17;
                break;
            case 2438175: // 왕관 데미지 스킨:
                ret = 16;
                break;
            case 2438173: // 레미너선스 데미지 스킨:
                ret = 14;
                break;
            case 2438172: // 솔로부대 데미지 스킨:
                ret = 13;
                break;
            case 2438169: // 도로시의 데미지 스킨:
                ret = 10;
                break;
            case 2438168: // 알리샤의 데미지 스킨:
                ret = 9;
                break;
            case 2438165: // 클럽 헤네시스 데미지 스킨:
                ret = 6;
                break;
            case 2438162: // 파티 퀘스트 데미지 스킨:
                ret = 3;
                break;
            case 2438160: // 디지털라이즈 데미지 스킨:
                ret = 1;
                break;
            case 2438159: // 기본 데미지 스킨:
                ret = 0;
                break;
        }
        return ret;
    }
    public static final int[] publicNpcIds = {9270035, 9070004, 9010022, 9071003, 9000087, 9000088, 9010000, 9000085, 9000018, 9000000};
    public static final String[] publicNpcs = {"#cUniversal NPC#", "Move to the #cBattle Square# to fight other players", "Move to a variety of #cparty quests#.", "Move to #cMonster Park# to team up to defeat monsters.", "Move to #cFree Market# to trade items with players.", "Move to #cArdentmill#, the crafting town.",
        "Check #cdrops# of any monster in the map.", "Review #cPokedex#.", "Review #cPokemon#.", "Join an #cevent# in progress."};
    //questID; FAMILY USES 19000x, MARRIAGE USES 16000x, EXPED USES 16010x
    //dojo = 150000, bpq = 150001, master monster portals: 122600
    //compensate evan = 170000, compensate sp = 170001
    public static final int OMOK_SCORE = 122200;
    public static final int MATCH_SCORE = 122210;
    public static final int HP_ITEM = 122221;
    public static final int MP_ITEM = 122223;
    public static final int JAIL_TIME = 123455;
    public static final int JAIL_QUEST = 123456;
    public static final int REPORT_QUEST = 123457;
    public static final int ULT_EXPLORER = 111111;
    //codex = -55 slot
    //crafting/gathering are designated as skills(short exp then byte 0 then byte level), same with recipes(integer.max_value skill level)
    public static final int ENERGY_DRINK = 122500;
    public static final int HARVEST_TIME = 122501;
    public static final int PENDANT_SLOT = 122700;
    public static final int BOSS_PQ = 150001;
    public static final int DOJO = 150100;
    public static final int DOJO_RECORD = 150101;
    public static final int PARTY_REQUEST = 122900;
    public static final int PARTY_INVITE = 122901;
    public static final int RUNE = 222222;
    public static final boolean GMS = false;

    public static boolean isBlaster(final int job) {
        return job >= 3700 && job <= 3712;
    }

    public static boolean isPinkBean(final int job) {
        return job == 13000 || job == 13100;
    }

    public static boolean isDefaultWarrior(final int job) {
        return job == 100;
    }

    public static boolean isHero(final int job) {
        return job >= 110 && job <= 112;
    }

    public static boolean isPaladin(final int job) {
        return job >= 120 && job <= 122;
    }

    public static boolean isDarkKnight(final int job) {
        return job >= 130 && job <= 132;
    }

    public static boolean isDefaultMagician(final int job) {
        return job == 200;
    }

    public static boolean isFPMage(final int job) {
        return job >= 210 && job <= 212;
    }

    public static boolean isILMage(final int job) {
        return job >= 220 && job <= 222;
    }

    public static boolean isBishop(final int job) {
        return job >= 230 && job <= 232;
    }

    public static boolean isDefaultArcher(final int job) {
        return job == 300;
    }

    public static boolean isBowMaster(final int job) {
        return job >= 310 && job <= 312;
    }

    public static boolean isMarksMan(final int job) {
        return job >= 320 && job <= 322;
    }

    public static boolean isDefaultThief(final int job) {
        return job == 400;
    }

    public static boolean isNightLord(final int job) {
        return job >= 410 && job <= 412;
    }

    public static boolean isShadower(final int job) {
        return job >= 420 && job <= 422;
    }

    public static boolean isDualBlade(final int job) {
        return (job >= 430 && job <= 434);
    }

    public static boolean isDefaultPirate(final int job) {
        return job == 500 || job == 501;
    }

    public static boolean isViper(final int job) {
        return (job >= 510 && job <= 512);
    }

    public static boolean isCaptain(final int job) {
        return (job >= 520 && job <= 522);
    }

    public static boolean isCannon(final int job) {
        return job == 1 || job == 501 || (job >= 530 && job <= 532);
    }

    public static boolean isWarrior(final int job) {
        if (GameConstants.isDefaultWarrior(job) || GameConstants.isHero(job) || GameConstants.isPaladin(job) || GameConstants.isDarkKnight(job) || GameConstants.isSoulMaster(job) || GameConstants.isAran(job) || GameConstants.isBlaster((short) job) || GameConstants.isDemonSlayer(job) || GameConstants.isDemonAvenger(job) || GameConstants.isMichael(job) || GameConstants.isKaiser(job) || GameConstants.isZero(job) || GameConstants.isAdel(job)) {
            return true;
        }
        return false;
    }

    public static boolean isMagician(final int job) {
        if (GameConstants.isDefaultMagician(job) || GameConstants.isFPMage(job) || GameConstants.isILMage(job) || GameConstants.isBishop(job) || GameConstants.isFlameWizard(job) || GameConstants.isEvan(job) || GameConstants.isLuminous(job) || GameConstants.isBattleMage(job) || GameConstants.isKinesis(job) || GameConstants.isIllium(job) || GameConstants.isLala(job)) {
            return true;
        }
        return false;
    }

    public static boolean isArcher(final int job) {
        if (GameConstants.isDefaultArcher(job) || GameConstants.isKain(job) || GameConstants.isPathFinder(job) || GameConstants.isBowMaster(job) || GameConstants.isMarksMan(job) || GameConstants.isWindBreaker(job) || GameConstants.isMercedes(job) || GameConstants.isWildHunter(job)) {
            return true;
        }
        return false;
    }

    public static boolean isThief(final int job) {
        if (GameConstants.isDefaultThief(job) || GameConstants.isHoyeong(job) || GameConstants.isNightLord(job) || GameConstants.isShadower(job) || GameConstants.isDualBlade(job) || GameConstants.isNightWalker(job) || GameConstants.isPhantom(job) || GameConstants.isKadena(job) || GameConstants.isXenon(job)) {
            return true;
        }
        return false;
    }

    public static boolean isPirate(final int job) {
        if (GameConstants.isDefaultPirate(job) || GameConstants.isViper(job) || GameConstants.isCaptain(job) || GameConstants.isCannon(job) || GameConstants.isStriker(job) || GameConstants.isEunWol(job) || GameConstants.isMechanic(job) || GameConstants.isAngelicBuster(job) || GameConstants.isArc(job) || GameConstants.isXenon(job)) {
            return true;
        }
        return false;
    }

    public static int MatrixExp(int level) {
        switch (level) {
            case 1:
                return 50;
            case 2:
                return 89;
            case 3:
                return 149;
            case 4:
                return 221;
            case 5:
                return 306;
            case 6:
                return 404;
            case 7:
                return 514;
            case 8:
                return 638;
            case 9:
                return 774;
            case 10:
                return 922;
            case 11:
                return 1084;
            case 12:
                return 1258;
            case 13:
                return 1445;
            case 14:
                return 1645;
            case 15:
                return 1857;
            case 16:
                return 2083;
            case 17:
                return 2321;
            case 18:
                return 2571;
            case 19:
                return 2835;
            case 20:
                return 3111;
            case 21:
                return 3400;
            case 22:
                return 3702;
            case 23:
                return 4016;
            case 24:
                return 4344;
        }
        return 4684;
    }

    public static int HyperStatSp(int curLevel) {
        switch (curLevel) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 10;
            case 5:
                return 15;
            case 6:
                return 20;
            case 7:
                return 25;
            case 8:
                return 30;
            case 9:
                return 35;
        }
        return 0;
    }

    public static int getMyLinkSkill(short job) {
        if (isMercedes(job)) {
            return 20021110;
        } else if (isCannon(job)) {
            return 110;
        } else if (isDemonSlayer(job)) {
            return 30010112;
        } else if (isPhantom(job)) {
            return 20030204;
        } else if (isLuminous(job)) {
            return 20040218;
        } else if (isKaiser(job)) {
            return 60000222;
        } else if (isXenon(job)) {
            return 30020233;
        } else if (isDemonAvenger(job)) {
            return 30010241;
        } else if (isSoulMaster(job)) {
            return 10000255;
        } else if (isFlameWizard(job)) {
            return 10000256;
        } else if (isWindBreaker(job)) {
            return 10000257;
        } else if (isNightWalker(job)) {
            return 10000258;
        } else if (isStriker(job)) {
            return 10000259;
        } else if (isZero(job)) {
            return 100000271;
        } else if (isEunWol(job)) {
            return 20050286;
        } else if (isKinesis(job)) {
            return 140000292;
        } else if (isAngelicBuster(job)) {
            return 60011219;
        } else if (isEvan(job)) {
            return 20010294;
        } else if (isAran(job)) {
            return 20000297;
        } else if (isMichael(job)) {
            return 50001214;
        } else if (isBattleMage(job)) {
            return 30000074;
        } else if (isWildHunter(job)) {
            return 30000075;
        } else if (isMechanic(job)) {
            return 30000076;
        } else if (isBlaster(job)) {
            return 30000077;
        } else if (isKadena(job)) {
            return 60020218;
        } else if (isIllium(job)) {
            return 150000017;
        } else if (isArk(job)) {
            return 150010241;
        } else if (isHero(job)) {
            return 252;
        } else if (isPaladin(job)) {
            return 253;
        } else if (isDarkKnight(job)) {
            return 254;
        } else if (isFPMage(job)) {
            return 255;
        } else if (isILMage(job)) {
            return 256;
        } else if (isBishop(job)) {
            return 257;
        } else if (isBowMaster(job)) {
            return 258;
        } else if (isMarksMan(job)) {
            return 259;
        } else if (isPathFinder(job)) {
            return 260;
        } else if (isNightLord(job)) {
            return 261;
        } else if (isShadower(job)) {
            return 262;
        } else if (isDualBlade(job)) {
            return 263;
        } else if (isViper(job)) {
            return 265;
        } else if (isCaptain(job)) {
            return 264;
        } else if (isHoyeong(job)) {
            return 160000001;
        } else if (isAdel(job)) {
            return 150020241;
        } else if (isKain(job)){
            return 60030241;
        } else if (isLala(job)) {
            return 160010001;
        }
        return 0;
    }

    public static int getLinkedSkillByJob(short job) {
        if (isMercedes(job)) {
            return 80001040;
        } else if (isCannon(job)) {
            return 80000000;
        } else if (isDemonSlayer(job)) {
            return 80000001;
        } else if (isPhantom(job)) {
            return 80000002;
        } else if (isLuminous(job)) {
            return 80000005;
        } else if (isKaiser(job)) {
            return 80000006;
        } else if (isXenon(job)) {
            return 80000047;
        } else if (isDemonAvenger(job)) {
            return 80000050;
        } else if (isSoulMaster(job)) {
            return 80000066;
        } else if (isFlameWizard(job)) {
            return 80000067;
        } else if (isWindBreaker(job)) {
            return 80000068;
        } else if (isNightWalker(job)) {
            return 80000069;
        } else if (isStriker(job)) {
            return 80000070;
        } else if (isZero(job)) {
            return 80000110;
        } else if (isEunWol(job)) {
            return 80000169;
        } else if (isKinesis(job)) {
            return 80000188;
        } else if (isAngelicBuster(job)) {
            return 80001155;
        } else if (isEvan(job)) {
            return 80000369;
        } else if (isAran(job)) {
            return 80000370;
        } else if (isMichael(job)) {
            return 80001140;
        } else if (isBattleMage(job)) {
            return 80000333;
        } else if (isWildHunter(job)) {
            return 80000334;
        } else if (isMechanic(job)) {
            return 80000335;
        } else if (isBlaster(job)) {
            return 80000378;
        } else if (isKadena(job)) {
            return 80000261;
        } else if (isIllium(job)) {
            return 80000268;
        } else if (isArk(job)) {
            return 80000514;
        } else if (isHero(job)) {
            return 80002759;
        } else if (isPaladin(job)) {
            return 80002760;
        } else if (isDarkKnight(job)) {
            return 80002761;
        } else if (isFPMage(job)) {
            return 80002763;
        } else if (isILMage(job)) {
            return 80002764;
        } else if (isBishop(job)) {
            return 80002765;
        } else if (isBowMaster(job)) {
            return 80002767;
        } else if (isMarksMan(job)) {
            return 80002768;
        } else if (isPathFinder(job)) {
            return 80002769;
        } else if (isNightLord(job)) {
            return 80002771;
        } else if (isShadower(job)) {
            return 80002772;
        } else if (isDualBlade(job)) {
            return 80002773;
        } else if (isViper(job)) {
            return 80002775;
        } else if (isCaptain(job)) {
            return 80002776;
        } else if (isHoyeong(job)) {
            return 80000609;
        } else if (isAdel(job)) {
            return 80002857;
        } else if(isKain(job)){
            return 80003015;
        } else if (isLala(job)) {
            return 80003058;
        }
        return 0;
    }

    public static boolean MovementAffectingStat(MapleBuffStat stat) {
        switch (stat) {
            case IndieJump:
            case IndieSpeed:
            case Stun:
            case Weakness:
            case Slow:
            case Morph:
            case Ghost:
            case BasicStatUp:
            case Attract:
            case DashSpeed:
            case DashJump:
            case Flying:
            case KeyDownMoving:
            case Frozen:
            case Frozen2:
            case Speed:
            case Jump:
            case Mechanic:
            case Magnet:
            case KnightsAura:
            case MagnetArea:
            case DarkTornado:
            case NewFlying:
            case NaviFlying:
            case Dance:
            case RWCylinder:
            case RideVehicle: // maybe?
            case RideVehicleExpire:
            case DemonFrenzy:
            case ShadowSpear:
            case VampDeath: // maybe
            case Lapidification:
            case SelfWeakness:
            case PoseType:
                return true;
        }
        return false;
    }

    public static boolean isStrangeCube(int itemid) {
        switch (itemid) {
            case 2436499:
            case 2710000:
            case 2711000:
            case 2711001:
            case 2711009:
            case 2711011:
                return true;
        }
        return false;
    }

    public static boolean isTryFling(int skillid) {
        switch (skillid) {
            case 13120003:
            case 13120010:
            case 13110022:
            case 13110027:
            case 13100027:
            case 13100022:
            case 13101022:
                return true;
        }
        return false;
    }

    public static boolean sub_816650(int a1) {
        return a1 == 80001587 || a1 - 80001587 == 42 || a1 - 80001587 == 871;
    }

    public static boolean sub_8166C0(int a1) {  //++342 아몰랑
        boolean v1;
        if (a1 > 0x1F962EF + 2) {
            if (a1 > 0x4C4BEDC) {
                if (a1 > 400011028) {
                    if (a1 > 400031046) {
                        if (a1 == 400041006 || a1 == 400041009) {
                            return true;
                        }
                        v1 = a1 - 400041009 == 10015;
                    } else {
                        if (a1 == 400031046 || a1 == 400011072 || a1 == 400011091) {
                            return true;
                        }
                        v1 = a1 - 400011091 == 9970;
                    }
                } else {
                    if (a1 == 400011028) {
                        return true;
                    }
                    if (a1 > 131001004) {
                        if (a1 > 131001021) {
                            v1 = a1 == 142111010;
                        } else {
                            if (a1 >= 131001020) {
                                return true;
                            }
                            v1 = a1 == 131001008;
                        }
                    } else {
                        if (a1 == 131001004) {
                            return true;
                        }
                        if (a1 > 0x5A999A9) {
                            if (a1 >= 101110101 && a1 <= 101110102) {
                                return true;
                            }
                            if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
                                return true;
                            }
                            return false;
                        }
                        if (a1 == 0x5A999A9) {
                            return true;
                        }
                        v1 = a1 == 0x4C4BEE1;
                    }
                }
            } else {
                if (a1 == 0x4C4BEDC) {
                    return true;
                }
                if (a1 > 0x393B2D0) {
                    if (a1 > 0x3E1AAEB) {
                        if (a1 == 0x4C4BB2C || a1 - 0x4C4BB2C == 51) {
                            return true;
                        }
                        v1 = a1 - 0x4C4BB2C == 849;
                    } else {
                        if (a1 == 0x3E1AAEB) {
                            return true;
                        }
                        if (a1 > 0x3D093F0) {
                            v1 = a1 == 0x3D268A9 + 1;
                        } else {
                            if (a1 >= 0x3D093EE + 1) {
                                return true;
                            }
                            v1 = a1 == 0x3D093E4 + 4;
                        }
                    }
                } else {
                    if (a1 == 0x393B2D0) {
                        return true;
                    }
                    if (a1 > 0x226DB89) {
                        if (a1 == 0x22729A7 + 1 || a1 - (0x22729A7 + 1) == 1000003) {
                            return true;
                        }
                        v1 = a1 - (0x22729A7 + 1) == 1000052;
                    } else {
                        if (a1 == 0x226DB89
                                || a1 == 0x1F96359 + 1
                                || a1 - (0x1F96359 + 1) == 100) {
                            return true;
                        }
                        v1 = a1 - (0x1F96359 + 1) == 1999901;
                    }
                }
            }
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        if (a1 == 0x1F962EF + 2) {
            return true;
        }
        if (a1 > 0xD7782B + 1) {
            if (a1 > 0x17F5106) {
                if (a1 > 0x1D909A6 + 2) {
                    if (a1 == 0x1DA9042 + 6 || a1 == 0x1DAB75D) {
                        return true;
                    }
                    v1 = a1 == 0x1DC3DF9;
                } else {
                    if (a1 == 0x1D909A6 + 2 || a1 == 0x19D8811 + 1 || a1 == 0x19DAEBC) {
                        return true;
                    }
                    v1 = a1 == 0x1CA1672 + 4;
                }
            } else {
                if (a1 == 0x17F5106) {
                    return true;
                }
                if (a1 > 0x160CC67 + 1) {
                    if (a1 == 0x1700EA4 + 4 || a1 - (0x1700EA4 + 4) == 5) {
                        return true;
                    }
                    v1 = a1 - (0x1700EA4 + 4) == 990005;
                } else {
                    if (a1 == 0x160CC67 + 1) {
                        return true;
                    }
                    if (a1 > 0x142440F + 4) {
                        v1 = a1 == 0x1524DCA + 1;
                    } else {
                        if (a1 >= 0x142440F + 3) {
                            return true;
                        }
                        v1 = a1 == 0x131CE08 + 2;
                    }
                }
            }
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        if (a1 == 0xD7782B + 1) {
            return true;
        }
        if (a1 <= 0x4FAA8C) {
            if (a1 == 0x4FAA8C) {
                return true;
            }
            if (a1 > 3101008) {
                if (a1 == 3111013 || a1 == 3121020) {
                    return true;
                }
                v1 = a1 == 0x423D09 + 1;
            } else {
                if (a1 == 3101008 || a1 == 1311011 || a1 == 2221011) {
                    return true;
                }
                v1 = a1 - 2221011 == 41;
            }
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        if (a1 > 0xB8F3D9 + 5) {
            if (a1 == 0xC80EEB + 1 || a1 == 0xC835E9) {
                return true;
            }
            v1 = a1 == 0xD7511E;
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        if (a1 == 0xB8F3D9 + 5) {
            return true;
        }
        if (a1 <= 0xA9B198 + 4) {
            if (a1 == 0xA9B198 + 4) {
                return true;
            }
            v1 = a1 == 0x510A1A;
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        if (a1 >= 0xA9B19F && a1 <= 0xA9B19F + 1) {
            return true;
        }
        if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_816650(a1)) {
            return true;
        }
        return false;
    }

    public static boolean sub_7818A0(int a1) {
        return a1 == 80001587 || a1 == 80001629 || a1 == 80002458;
    }

    public static boolean isZeroSkill(int a1) {
        int v1; // eax

        v1 = a1 / 10000;
        if (a1 / 10000 == 8000) {
            v1 = a1 / 100;
        }
        return v1 == 10000 || v1 == 10100 || v1 == 10110 || v1 == 10111 || v1 == 10112;
    }

    public static int sub_82A590(int a1) {
        boolean v1 = false; // zf
        boolean v3; // zf
        int v4; // ecx

        if (a1 > 14001023) {
            if (a1 > 0x217E38D + 1) {
                if (a1 > 0x3E1AAEC) {
                    if (a1 <= 400001020) {
                        if (a1 == 400001020) {
                            return 1;
                        }
                        if (a1 > 80001816) {
                            if (a1 > 131001018) {
                                v1 = a1 == 142121016;
                            } else {
                                if (a1 == 131001018 || a1 == 100001268) {
                                    return 1;
                                }
                                v1 = a1 == 100001271;
                            }
                        } else {
                            if (a1 == 80001816) {
                                return 1;
                            }
                            if (a1 > 0x3E1AB1E) {
                                v1 = a1 == 80000365;
                            } else {
                                if (a1 >= 0x3E1AB1D || a1 == 0x3E1AAF1) {
                                    return 1;
                                }
                                v1 = a1 - 0x3E1AAF1 == 2;
                            }
                        }
                        if (!v1) {
                            return 0;
                        }
                        return 1;
                    }
                    if (a1 <= 400041008) {
                        if (a1 == 400041008) {
                            return 1;
                        }
                        if (a1 > 400011102) {
                            v1 = a1 == 400031002;
                        } else {
                            if (a1 == 400011102 || a1 == 400011010) {
                                return 1;
                            }
                            v1 = a1 == 400011066;
                        }
                        if (!v1) {
                            return 0;
                        }
                        return 1;
                    }
                    if (a1 == 400051001) {
                        return 1;
                    }
                    v4 = a1 - 400051015;
                    v3 = a1 == 400051015;
                    if (!v1) {
                        return 0;
                    }
                    return 1;
                }
                if (a1 == 0x3E1AAEC) {
                    return 1;
                }
                if (a1 > 0x3A453CA) {
                    if (a1 > 0x3A4A2BD + 4) {
                        if (a1 > 0x3D268DE) {
                            v1 = a1 == 0x3E1843C;
                        } else {
                            if (a1 == 0x3D268DE || a1 == 0x3D268AB + 1) {
                                return 1;
                            }
                            v1 = a1 - (0x3D268AB + 1) == 7;
                        }
                    } else {
                        if (a1 == 0x3A4A2BD + 4) {
                            return 1;
                        }
                        if (a1 > 0x3A49E07) {
                            v1 = a1 == 0x3A4A1F6;
                        } else {
                            if (a1 == 0x3A49E07) {
                                return 1;
                            }
                            if (a1 < 0x3A477C3) {
                                return 0;
                            }
                            if (a1 <= 0x3A477C4) {
                                return 1;
                            }
                            v1 = a1 == 0x3A47AE0;
                        }
                    }
                } else {
                    if (a1 == 0x3A453CA) {
                        return 1;
                    }
                    if (a1 > 37121053) {
                        if (a1 > 0x30C0B69 + 4) {
                            if (a1 >= 0x30C0B9C + 1) {
                                if (a1 > 0x30C0B9E) {
                                    return 0;
                                }
                                return 1;
                            }
                        } else {
                            if (a1 == 0x30C0B69 + 4)// || a1 == nullsub_206 )
                            {
                                return 1;
                            }
                            if (a1 > 0x30BE458 + 3) {
                                if (a1 > 0x30BE45D) {
                                    return 0;
                                }
                                return 1;
                            }
                        }
                        return 0;
                    }
                    if (a1 == 0x2366C1C + 1) {
                        return 1;
                    }
                    if (a1 > 0x22729B0) {
                        if (a1 >= 0x22729DC + 1) {
                            if (a1 > 0x22729DC + 2) {
                                return 0;
                            }
                            return 1;
                        }
                        return 0;
                    }
                    if (a1 == 0x22729B0 || a1 == 0x217E79D) {
                        return 1;
                    }
                    v1 = a1 == 0x227029B + 1;
                }
            } else {
                if (a1 == 0x217E38D + 1) {
                    return 1;
                }
                if (a1 > 0x1700EDC + 1) {
                    if (a1 > 0x1DADE9D) {
                        if (a1 > 0x1EA20AF) {
                            if (a1 > 0x1F9631B + 2) {
                                v1 = a1 == 0x217C061 + 4;
                            } else {
                                if (a1 == 0x1F9631B + 2 || a1 == 0x1EA20DD) {
                                    return 1;
                                }
                                v1 = a1 == 0x1F962EE + 1;
                            }
                        } else {
                            if (a1 == 0x1EA20AF) {
                                return 1;
                            }
                            if (a1 > 0x1DC650F + 1) {
                                if (a1 >= 0x1DC653C + 1) {
                                    if (a1 > 0x1DC653E) {
                                        return 0;
                                    }
                                    return 1;
                                }
                                return 0;
                            }
                            if (a1 == 0x1DC650F + 1) {
                                return 1;
                            }
                            if (a1 < 0x1DC3DF9 + 2) {
                                return 0;
                            }
                            if (a1 <= 0x1DC3DFC) {
                                return 1;
                            }
                            v1 = a1 == 0x1DC6509;
                        }
                    } else {
                        if (a1 == 0x1DADE9D) {
                            return 1;
                        }
                        if (a1 > 0x19DD56D + 1) {
                            if (a1 > 0x1D930B9) {
                                if (a1 >= 0x1DADE6B) {
                                    if (a1 > 31121005) {
                                        return 0;
                                    }
                                    return 1;
                                }
                            } else {
                                if (a1 == 0x1D930B9 || a1 == 0x19DD570 + 1) {
                                    return 1;
                                }
                                if (a1 > 0x19DD59C) {
                                    if (a1 > 0x19DD59E) {
                                        return 0;
                                    }
                                    return 1;
                                }
                            }
                            return 0;
                        }
                        if (a1 == 0x19DD56D + 1) {
                            return 1;
                        }
                        if (a1 > 0x17F516B + 1) {
                            if (a1 >= 0x19DAE5D) {
                                if (a1 > 0x19DAE5D + 1) {
                                    return 0;
                                }
                                return 1;
                            }
                            return 0;
                        }
                        if (a1 == 0x17F516B + 1 || a1 == 0x17F02D0 + 1) {
                            return 1;
                        }
                        v1 = a1 == 0x17F5153 + 1;
                    }
                } else {
                    if (a1 == 0x1700EDC + 1) {
                        return 1;
                    }
                    if (a1 > 0x14247E6 + 2) {
                        if (a1 > 0x160CC6C + 1) {
                            if (a1 > 0x1700EAB + 1) {
                                if (a1 >= 0x1700EAD + 2) {
                                    if (a1 > 0x1700EAD + 3) {
                                        return 0;
                                    }
                                    return 1;
                                }
                                return 0;
                            }
                            if (a1 == 0x1700EAB + 1) {
                                return 1;
                            }
                            if (a1 < 0x160CC9A + 3) {
                                return 0;
                            }
                            if (a1 <= 0x160CC9A + 4) {
                                return 1;
                            }
                            v1 = a1 == 0x16FE79B;
                        } else {
                            if (a1 >= 0x160CC6C) {
                                return 1;
                            }
                            if (a1 > 0x1524DC1) {
                                v1 = a1 == 0x1524DC7 + 3;
                            } else {
                                if (a1 == 0x1524DC1) {
                                    return 1;
                                }
                                if (a1 < 0x142481B + 2) {
                                    return 0;
                                }
                                if (a1 <= 0x142481B + 3) {
                                    return 1;
                                }
                                v1 = a1 == 0x1524DBC;
                            }
                        }
                    } else {
                        if (a1 == 0x14247E6 + 2) {
                            return 1;
                        }
                        if (a1 > 0xE6BA63 + 5) {
                            if (a1 > 0x131A6EA) {
                                v1 = a1 == 0x14220E3 + 1;
                            } else {
                                if (a1 >= 0x131A6E8 + 1 || a1 == 0xE6BA6B + 2) {
                                    return 1;
                                }
                                v1 = a1 - (0xE6BA6B + 2) == 48;
                            }
                        } else {
                            if (a1 == 0xE6BA63 + 5) {
                                return 1;
                            }
                            if (a1 > 0xD7785D) {
                                v1 = a1 == 0xE4E5BE;
                            } else {
                                if (a1 == 0xD7785D || a1 == 0xD5A382 + 1) {
                                    return 1;
                                }
                                v1 = a1 == 0xD77827 + 1;
                            }
                        }
                    }
                }
            }
            if (!v1) {
                return 0;
            }
            return 1;
        }
        if (a1 >= 14001022) {
            return 1;
        }
        if (a1 > 4101011) {
            if (a1 <= 0x510A1D) {
                if (a1 >= 5311004) {
                    return 1;
                }
                if (a1 <= 5121000) {
                    if (a1 == 0x4E23E6 + 2) {
                        return 1;
                    }
                    if (a1 > 0x41A0CB) {
                        if (a1 > 5111007) {
                            v1 = a1 == 0x4E200B + 1;
                        } else {
                            if (a1 == 0x4DFCDD + 2 || a1 == 0x423D04 + 4) {
                                return 1;
                            }
                            v1 = a1 - (0x423D04 + 4) == 53;
                        }
                    } else {
                        if (a1 == 0x41A0CB) {
                            return 1;
                        }
                        if (a1 > 4221000) {
                            v1 = a1 == 0x40687D;
                        } else {
                            if (a1 == 0x406846 + 2 || a1 == 4121000) {
                                return 1;
                            }
                            v1 = a1 == 4121053;
                        }
                    }
                    if (!v1) {
                        return 0;
                    }
                    return 1;
                }
                if (a1 <= 5220014) {
                    if (a1 == 0x4FA6AD + 1) {
                        return 1;
                    }
                    if (a1 > 5121054) {
                        v1 = a1 == 0x4F837F;
                    } else {
                        if (a1 >= 5121053 || a1 == 0x4E23F0 + 1) {
                            return 1;
                        }
                        v1 = a1 == 0x4E23F6 + 1;
                    }
                    if (!v1) {
                        return 0;
                    }
                    return 1;
                }
                if (a1 > 5221054) {
                    v1 = a1 == 0x50E30A + 1;
                    if (!v1) {
                        return 0;
                    }
                    return 1;
                }
                if (a1 >= 0x4FAABD) {
                    return 1;
                }
                v4 = a1 - 5221000;
                v3 = a1 == 0x4FAA84 + 4;
                if (v3) {
                    return 1;
                }
                v1 = v4 == 18;
                if (!v1) {
                    return 0;
                }
                return 1;
            }
            if (a1 > 0xB8A585 + 4) {
                if (a1 > 0xC835E8) {
                    if (a1 > 0xD5A36B) {
                        v1 = a1 == 0xD5A36F;
                    } else {
                        if (a1 == 0xD5A36B || a1 == 0xC835ED) {
                            return 1;
                        }
                        v1 = a1 - 0xC835ED == 48;
                    }
                } else {
                    if (a1 == 0xC835E8) {
                        return 1;
                    }
                    if (a1 > 0xC6613E) {
                        v1 = a1 == 0xC7E7DB + 5;
                    } else {
                        if (a1 == 0xC6613E || a1 == 0xB8F3A8) {
                            return 1;
                        }
                        v1 = a1 - 0xB8F3A8 == 53;
                    }
                }
            } else {
                if (a1 >= 0xB8A585 + 3) {
                    return 1;
                }
                if (a1 > 0xA9635D + 1) {
                    if (a1 > 0xA9B16D) {
                        if (a1 >= 0xA9B199 + 4) {
                            if (a1 > 0xA9B19E) {
                                return 0;
                            }
                            return 1;
                        }
                        return 0;
                    }
                    if (a1 == 0xA9B16D) {
                        return 1;
                    }
                    if (a1 < 0xA98A6E) {
                        return 0;
                    }
                    if (a1 <= 0xA98A6E + 1) {
                        return 1;
                    }
                    v1 = a1 == 0xA9B168;
                } else {
                    if (a1 == 0xA9635D + 1) {
                        return 1;
                    }
                    if (a1 > 5321053) {
                        v1 = a1 == 0xA7DCBE;
                    } else {
                        if (a1 == 0x51315C + 1) {
                            return 1;
                        }
                        if (a1 < 5320007) {
                            return 0;
                        }
                        if (a1 <= 5320008) {
                            return 1;
                        }
                        v1 = a1 == 0x51312D;
                    }
                }
            }
            if (!v1) {
                return 0;
            }
            return 1;
        }
        if (a1 == 4101011) {
            return 1;
        }
        if (a1 > 2201001) {
            if (a1 > 3121000) {
                if (a1 > 3221053) {
                    if (a1 > 4001003) {
                        v1 = a1 == 4001005;
                    } else {
                        if (a1 == 4001003) {
                            return 1;
                        }
                        if (a1 < 3321022) {
                            return 0;
                        }
                        if (a1 <= 3321023) {
                            return 1;
                        }
                        v1 = a1 == 3321041;
                    }
                } else {
                    if (a1 == 3221053) {
                        return 1;
                    }
                    if (a1 > 3211012) {
                        v1 = a1 == 3221000;
                    } else {
                        if (a1 == 3211012 || a1 == 3121002) {
                            return 1;
                        }
                        v1 = a1 == 3121053;
                    }
                }
            } else {
                if (a1 == 3121000) {
                    return 1;
                }
                if (a1 > 2311003) {
                    if (a1 > 2321053) {
                        v1 = a1 == 3111011;
                    } else {
                        if (a1 == 2321053 || a1 == 2321000) {
                            return 1;
                        }
                        v1 = a1 == 2321005;
                    }
                } else {
                    if (a1 == 2311003) {
                        return 1;
                    }
                    if (a1 > 2301004) {
                        v1 = a1 == 2311001;
                    } else {
                        if (a1 == 2301004 || a1 == 2221000) {
                            return 1;
                        }
                        v1 = a1 == 2221053;
                    }
                }
            }
            if (!v1) {
                return 0;
            }
            return 1;
        }
        if (a1 == 2201001) {
            return 1;
        }
        if (a1 <= 1221053) {
            if (a1 >= 1221052) {
                return 1;
            }
            if (a1 > 1121054) {
                if (a1 > 1221000) {
                    v1 = a1 == 1221014;
                } else {
                    if (a1 == 1221000 || a1 == 1211010) {
                        return 1;
                    }
                    v1 = a1 == 1211013;
                }
            } else {
                if (a1 >= 1121053) {
                    return 1;
                }
                if (a1 > 1121000) {
                    v1 = a1 == 1121016;
                } else {
                    if (a1 == 1121000 || a1 == 1000003) {
                        return 1;
                    }
                    v1 = a1 == 1101006;
                }
            }
            if (!v1) {
                return 0;
            }
            return 1;
        }
        if (a1 > 1321053) {
            if (a1 > 2121000) {
                if (a1 >= 2121053) {
                    if (a1 > 2121054) {
                        return 0;
                    }
                    return 1;
                }
                return 0;
            }
            if (a1 == 2121000 || a1 == 2101001) {
                return 1;
            }
            v1 = a1 == 2101010;
            if (!v1) {
                return 0;
            }
            return 1;
        }
        if (a1 == 1321053) {
            return 1;
        }
        if (a1 > 1321000) {
            if (a1 < 1321014 || a1 > 1321015) {
                return 0;
            }
            return 1;
        }
        if (a1 == 1321000) {
            return 1;
        }
        if (a1 >= 1301006) {
            if (a1 <= 1301007) {
                return 1;
            }
            v1 = a1 == 1311015;
            if (!v1) {
                return 0;
            }
            return 1;
        }
        return 0;
    }

    public static int sub_82AE80(int a1) {
        boolean v2; // zf

        if (a1 > 3321004) {
            if (a1 == 131001023 || a1 == 152001001) {
                return 1;
            }
            v2 = a1 == 152120001;
        } else {
            if (a1 >= 3321003) {
                return 1;
            }
            if (a1 > 3301003) {
                v2 = a1 == 3310001;
            } else {
                if (a1 == 3301003 || a1 == 3011004) {
                    return 1;
                }
                v2 = a1 == 3300002;
            }
        }
        if (!v2) {
            return 0;
        }
        return 1;
    }

    public static boolean sub_896310(int a1) {
        if (a1 <= 64111012) {
            if (a1 == 0x3D241A4) {
                return true;
            }
            if (a1 > 3311013) {
                if (a1 == 3321005 || a1 == 3321039) {
                    return true;

                }
            } else {
                if (a1 == 3311013 || a1 == 3301004 || a1 == 3311011) {
                    return true;
                }
            }
        }
        if (a1 > 400021053) {
            if (a1 == 400021035) {
                return true;
            }
        }
        if (a1 != 400021053) {
            if (a1 < 400020009) {
                return false;
            }
            if (a1 > 400020011) {
                if (a1 == 400021029) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int sub_893240(int a1) {
        int result; // eax

        if (a1 > 0x1421D09 + 1) {
            if (a1 == 0x1421D0C || a1 == 0x1424418 + 1) {
                return 1;
            }
        } else {
            if (a1 == 0x1421D09 + 1) {
                return 1;
            }
            if (a1 > 0x1407332) {
                if (a1 >= 0x1421D04 + 2 && a1 <= 0x1421D04 + 3) {
                    return 1;
                }
            } else if (a1 == 0x1407332
                    || a1 >= 0x1406F44 + 2 && a1 <= 0x1406F47) {
                return 1;
            }
        }
        switch (a1) {
            case 0x4C4BB85:
            case 0x4C4BB86:
            case 0x4C4BB87:
            case 0x4C4BB90:
            case 0x4C4BB91:
            case 0x4C4BB92:
                return 1;
            default:
                result = 0;
                break;
        }
        return result;
    }

    public static boolean sub_781900(int a1) {
        boolean v1; // zf

        if (a1 > 31111005) {
            if (a1 > 80001587) {
                if (a1 > 400011028) {
                    if (a1 > 400041006) {
                        if (a1 == 400041009) {
                            return true;
                        }
                        v1 = a1 == 400051024;
                    } else {
                        if (a1 == 400041006 || a1 == 400011072 || a1 == 400011091) {
                            return true;
                        }
                        v1 = a1 - 400011091 == 9970;
                    }
                } else {
                    if (a1 == 400011028) {
                        return true;
                    }
                    if (a1 > 131001004) {
                        if (a1 > 131001021) {
                            v1 = a1 == 142111010;
                        } else {
                            if (a1 >= 131001020) {
                                return true;
                            }
                            v1 = a1 == 131001008;
                        }
                    } else {
                        if (a1 == 131001004) {
                            return true;
                        }
                        if (a1 > 95001001) {
                            if (a1 >= 101110101 && a1 <= 101110102) {
                                return true;
                            }
                            if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
                                return true;
                            }
                            return false;
                        }
                        if (a1 == 95001001) {
                            return true;
                        }
                        v1 = a1 == 80001887;
                    }
                }
            } else {
                if (a1 == 80001587) {
                    return true;
                }
                if (a1 > 37121003) {
                    if (a1 > 64001008) {
                        if (a1 == 64121002) {
                            return true;
                        }
                        v1 = a1 == 65121003;
                    } else {
                        if (a1 >= 64001007 || a1 == 37121052 || a1 == 60011216) {
                            return true;
                        }
                        v1 = a1 == 64001000;
                    }
                } else {
                    if (a1 == 37121003) {
                        return true;
                    }
                    if (a1 > 33121214) {
                        if (a1 == 35121015 || a1 == 36101001) {
                            return true;
                        }
                        v1 = a1 == 36121000;
                    } else {
                        if (a1 == 33121214
                                || a1 == 31211000 + 1
                                || a1 - (31211000 + 1) == 1910008) {
                            return true;
                        }
                        v1 = a1 - (31211000 + 1) == 1910113;
                    }
                }
            }
            if (!v1) {
                if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        if (a1 == 31111001 + 4) {
            return true;
        }
        if (a1 <= 14111002 + 4) {
            if (a1 == 14111002 + 4) {
                return true;
            }
            if (a1 > 4340999 + 3) {
                if (a1 > 11121055) {
                    if (a1 == 12121052 + 2 || a1 == 13111020) {
                        return true;
                    }
                    v1 = a1 == 13120998 + 3;
                } else {
                    if (a1 == 11121055 || a1 == 5221004 || a1 == 5311002) {
                        return true;
                    }
                    v1 = a1 == 11121052;
                }
            } else {
                if (a1 == 4341002) {
                    return true;
                }
                if (a1 > 2321001) {
                    if (a1 == 3101008 || a1 == 3111013) {
                        return true;
                    }
                    v1 = a1 == 3121020;
                } else {
                    if (a1 == 2321001 || a1 == 1311011 || a1 == 2221011) {
                        return true;
                    }
                    v1 = a1 - 2221011 == 41;
                }
            }
            if (!v1) {
                if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        if (a1 > 25111005) {
            if (a1 > 30021238) {
                if (a1 == 31101000) {
                    return true;
                }
                v1 = a1 == 31101000;
            } else {
                if (a1 == 30021238 || a1 == 25121030 || a1 == 27101202) {
                    return true;
                }
                v1 = a1 == 27111100;
            }
            if (!v1) {
                if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        if (a1 == 25111005) {
            return true;
        }
        if (a1 > 22171083) {
            if (a1 == 23121000 || a1 == 24121000) {
                return true;
            }
            v1 = a1 == 24121005;
            if (!v1) {
                if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        if (a1 == 22171083) {
            return true;
        }
        if (a1 <= 20041226) {
            if (a1 == 20041226) {
                return true;
            }
            v1 = a1 == 14121004;
            if (!v1) {
                if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
        if (a1 >= 21120018 && a1 <= 21120019) {
            return true;
        }
        if (((a1 - 80001389) >= -3 && (a1 - 80001389) <= 3) || sub_7818A0(a1)) {
            return true;
        }
        return false;
    }

    public static int sub_6F3100(int a1) {
        int result; // eax

        if (a1 > 0x1421D09 + 1) {
            if (a1 == 0x1421D0C || a1 == 0x1424418 + 1) {
                return 1;
            }
        } else {
            if (a1 == 0x1421D09 + 1) {
                return 1;
            }
            if (a1 > 0x1407332) {
                if (a1 >= 0x1421D04 + 2 && a1 <= 0x1421D04 + 3) {
                    return 1;
                }
            } else if (a1 == 0x1407332
                    || a1 >= 0x1406F44 + 2 && a1 <= 0x1406F47) {
                return 1;
            }
        }
        switch (a1) {
            case 0x4C4BB85:
            case 0x4C4BB86:
            case 0x4C4BB87:
            case 0x4C4BB90:
            case 0x4C4BB91:
            case 0x4C4BB92:
                return 1;
            default:
                result = 0;
                break;
        }
        return result;
    }

    public static boolean sub_53F780(int a1) {
        boolean v2; // zf

        if (a1 > 0x160A55A + 1) {
            if (a1 > 131001108) {
                if (a1 <= 131001213) {
                    if (a1 != 131001213) {
                        switch (a1) {
                            case 0x7CEEB19:
                            case 0x7CEEB71:
                            case 0x7CEEB72:
                            case 0x7CEEB73:
                            case 0x7CEEB78:
                                return true;
                            default:
                                return false;
                        }
                    }
                    return true;
                }
                if (a1 == 131001313 || a1 == 131002010) {
                    return true;
                }
                v2 = a1 == 400031024;
            } else {
                if (a1 == 131001108) {
                    return true;
                }
                if (a1 > 131001005) {
                    switch (a1) {
                        case 0x7CEEAB0:
                        case 0x7CEEAB2:
                        case 0x7CEEAB3:
                        case 0x7CEEAB4:
                        case 0x7CEEAB5:
                        case 0x7CEEB0D:
                        case 0x7CEEB0E:
                        case 0x7CEEB0F:
                        case 0x7CEEB10:
                            return true;
                        default:
                            return false;
                    }
                }
                if (a1 >= 131001000) {
                    return true;
                }
                if (a1 > 0x160CC69 + 2) {
                    v2 = a1 == (0x160CC9B + 1);
                } else {
                    if (a1 >= 0x160CC69 + 1 || a1 == (0x160C88A + 3)) {
                        return true;
                    }
                    v2 = a1 == (0x160CC67 + 1);
                }
            }
            if (v2) {
                return true;
            }
            return false;
        }
        if (a1 >= 0x160A558) {
            return true;
        }
        if (a1 > 0xD7512E + 1) {
            if (a1 > 0x1607A5E + 6) {
                if (a1 <= 0x1607E4F) {
                    if (a1 == 0x1607E4F) {
                        return true;
                    }
                    if (a1 >= 0x1607E48) {
                        if (a1 > 0x1607E48 + 1) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }
                v2 = a1 == (0x160A174 + 2);
            } else {
                if (a1 == (0x1607A5E + 6)) {
                    return true;
                }
                if (a1 > 0xD7782A) {
                    v2 = a1 == (0x15EF7A7 + 1);
                } else {
                    if (a1 >= 0xD77827 + 2) {
                        return true;
                    }
                    v2 = a1 == (0xD77467 + 6);
                }
            }
            if (v2) {
                return true;
            }
            return false;
        }
        if (a1 >= 0xD75128 + 4) {
            return true;
        }
        if (a1 > 0xA9B1CD + 2) {
            if (a1 > 0xD5A37C) {
                if (a1 >= 0xD72A19 + 3) {
                    if (a1 > 0xD72A19 + 4) {
                        return false;
                    }
                    return true;
                }
            } else {
                if (a1 == 0xD5A37C) {
                    return true;
                }
                if (a1 >= 0xA9B230 + 1) {
                    if (a1 > 0xA9B233) {
                        return false;
                    }
                    return true;
                }
            }
        } else {
            if (a1 >= 0xA9B1CD) {
                return true;
            }
            if (a1 <= 0xA98AD0 + 1) {
                if (a1 < 0xA98AD0) {
                    switch (a1) {
                        case 0xA963C0:
                        case 0xA963C1:
                        case 0xA96424:
                        case 0xA96425:
                            return true;
                        default:
                            return false;
                    }
                }
                return true;
            }
            if (a1 >= 0xA98B34 && a1 <= 0xA98B35) {
                return true;
            }
        }
        return false;
    }

    public static boolean sub_6E84F0(int skill) {
        return skill == 13111020;
    }

    public static boolean sub_893F80(int a1) {
        return a1 == 0x23619E1 + 1 || a1 - 0x23619E1 + 1 == 9999 || a1 - 0x23619E1 + 1 == 10002;
    }

    public static boolean sub_57DCA0(int a1) {
        boolean v1, v2, v3; // zf

        if (a1 > 0x160C889 + 4) {
            if (a1 <= 131001108) {
                if (a1 != 131001108) {
                    if (a1 > 131001005) {
                        switch (a1) {
                            default:
                                return false;
                            case 131001008:
                            case 131001010:
                            case 131001011:
                            case 131001012:
                            case 131001013:
                            case 131001101:
                            case 131001102:
                            case 131001103:
                            case 131001104:
                                return true;
                        }
                    } else if (a1 < 131001000) {
                        switch (a1) {
                            default:
                                return false;
                            case 23121000:
                            case 23121002:
                            case 23121003:
                            case 23121011:
                            case 23121052:
                                return true;
                        }
                    }
                }
                return true;
            }
            if (a1 <= 131001313) {
                if (a1 != 131001313) {
                    switch (a1) {
                        default:
                            return false;
                        case 131001113:
                        case 131001201:
                        case 131001202:
                        case 131001203:
                        case 131001208:
                        case 131001213:
                            return true;
                    }
                }
                return true;
            }
            if (a1 > 400031024) {
                if ((a1 - 400041059) > 1) //unsigned
                {
                    return false;
                }
            } else if (a1 != 400031024 && a1 != 131002010) {
                return false;
            }
            return true;
        }
        if (a1 == 0x160C889 + 4) {
            return true;
        }
        if (a1 <= 0xD7512F) {
            if (a1 >= 0xD7512B + 1) {
                return true;
            }
            if (a1 > 0xA9B1CD + 2) {
                if (a1 > 0xD5A37C) {
                    if (a1 < 0xD72A1C) {
                        return false;
                    }
                    v3 = a1 > 0xD72A1C + 1;
                    v1 = a1 == 0xD72A1C + 1;
                    v2 = a1 - (0xD72A1C + 1) < 0;
                } else {
                    if (a1 == 0xD5A37C) {
                        return true;
                    }
                    if (a1 < 0xA9B231) {
                        return false;
                    }
                    v3 = a1 > 0xA9B233;
                    v1 = a1 == 0xA9B233;
                    v2 = a1 - 0xA9B233 < 0;
                }
                if (!((v2 ^ v3) | v1)) {
                    return false;
                }
                return true;
            }
            if (a1 >= 0xA9B1CD) {
                return true;
            }
            if (a1 <= 0xA98ACB + 6) {
                if (a1 < 0xA98ACB + 5) {
                    switch (a1) {
                        default:
                            return false;
                        case 11101120:
                        case 11101121:
                        case 11101220:
                        case 11101221:
                            return true;
                    }
                }
                return true;
            }
            if (a1 >= 0xA98B34) {
                v3 = a1 > 0xA98B34 + 1;
                v1 = a1 == 0xA98B34 + 1;
                v2 = a1 - (0xA98B34 + 1) < 0;
                if (!((v2 ^ v3) | v1)) {
                    return false;
                }
                return true;
            }
            return false;
        }
        if (a1 <= 0x1607E49) {
            if (a1 >= 0x1607E48) {
                return true;
            }
            if (a1 > 0x15EF7A6 + 2) {
                if (a1 != 0x1607A60 + 4) {
                    return false;
                }
                return true;
            }
            if (a1 == 0x15EF7A6 + 2 || a1 == 0xD7746D) {
                return true;
            }
            if (a1 <= 0xD77826 + 2) {
                return false;
            }
            v3 = a1 > 0xD77829 + 1;
            v1 = a1 == 0xD77829 + 1;
            v2 = a1 - (0xD77829 + 1) < 0;
            if (!((v2 ^ v3) | v1)) {
                return false;
            }
            return true;
        }
        if (a1 <= 0x160A175 + 1) {
            if (a1 != 0x160A175 + 1 && a1 != 0x1607E4F) {
                return false;
            }
            return true;
        }
        if (a1 < 0x160A554 + 4) {
            return false;
        }
        v3 = a1 > 0x160A55A + 1;
        v1 = a1 == 0x160A55A + 1;
        v2 = a1 - (0x160A55A + 1) < 0;

        if (!((v2 ^ v3) | v1)) {
            return false;
        }
        return true;
    }

    public static boolean sub_8845D0(int a1) {
        boolean v1; // zf@8

        if (a1 > 0x3A47B49) {
            if (a1 > 101120203) {
                if (a1 > 400031004) {
                    v1 = a1 == 400031036;
                } else {
                    if (a1 >= 400031003 || a1 == 101120205) {
                        return true;
                    }
                    v1 = a1 == 400001018;
                }
            } else {
                if (a1 == 101120203) {
                    return true;
                }
                if (a1 > 0x4C4BCC7) {
                    if (a1 == 0x4C4BCFC) {
                        return true;
                    }
                    v1 = a1 == 101120200;
                } else {
                    if (a1 == 0x4C4BCC7 || a1 == 0x3A47BB2) {
                        return true;
                    }
                    v1 = a1 == 0x3D21A8A;
                }
            }
        } else {
            if (a1 == 0x3A47B49) {
                return true;
            }
            if (a1 > 0xD7512E) {
                if (a1 > 0x19DD631) {
                    if (a1 == 0x1DC16E6 + 3) {
                        return true;
                    }
                    v1 = a1 == 0x3A47B3C;
                } else {
                    if (a1 == 0x19DD631 || a1 == 0x151D46E + 1) {
                        return true;
                    }
                    v1 = a1 - (0x151D46E + 1) == 9;
                }
            } else {
                if (a1 == 0xD7512E) {
                    return true;
                }
                if (a1 > 5101014) {
                    if (a1 == 0x50E306 + 3) {
                        return true;
                    }
                    v1 = a1 == 0xB8F3A9;
                } else {
                    if (a1 == 0x4DD5D5 + 1 || a1 == 2221012) {
                        return true;
                    }
                    v1 = a1 == 0x4DD5D2 + 2;
                }
            }
        }
        if (!v1) {
            return false;
        }
        return true;
    }

    public static boolean is_screen_attack_skill(int a1) {
        return a1 == 4221052 || a1 == 65121052;
    }

    public static boolean sub_84ABA0(int a1) {
        return a1 == 152110004 || a1 == 152120016 || a1 == 155121003;
    }

    public static boolean sub_849720(int a1) {
        boolean v1; // zf

        if (a1 <= 0x3D241A2 + 2) {
            if (a1 == 0x3D241A2 + 2) {
                return true;
            }
            if (a1 > 3311013) {
                if (a1 == 3321005) {
                    return true;
                }
                v1 = a1 == 3321039;
            } else {
                if (a1 == 3311013 || a1 == 3301004) {
                    return true;
                }
                v1 = a1 == 3311011;
            }
            if (!v1) {
                return false;
            }
            return true;
        }
        if (a1 > 400021053) {
            v1 = a1 == 400031035;
            if (!v1) {
                return false;
            }
            return true;
        }
        if (a1 != 400021053) {
            if (a1 < 400020009) {
                return false;
            }
            if (a1 > 400020011) {
                v1 = a1 == 400021029;
                if (!v1) {
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    public static boolean sub_833C40(int a1) {
        boolean v1 = false; // zf

        if (a1 > 0x3A47B49) {
            if (a1 > 101120203) {
                if (a1 > 400031004) {
                    v1 = a1 == 400031036;
                } else {
                    if (a1 >= 400031003 || a1 == 101120205) {
                        return true;
                    }
                    v1 = a1 == 400001018;
                }
            } else {
                if (a1 == 101120203) {
                    return true;
                }
                if (a1 > 80002247) {
                    if (a1 == 80002300) {
                        return true;
                    }
                    v1 = a1 == 101120200;
                } else {
                    if (a1 == 80002247 || a1 == 0x3A47BB2) {
                        return true;
                    }
                    v1 = a1 == 0x3D21A8A;
                }
            }
        } else {
            if (a1 == 0x3A47B49) {
                return true;
            }
            if (a1 > 0xD7512C + 2) {
                if (a1 > 0x19DD631) {
                    if (a1 == 0x1DC16E8 + 1) {
                        return true;
                    }
//	        v1 = a1 == aY_0;
                } else {
                    if (a1 == 0x19DD631 || a1 == 0x151D46F) {
                        return true;
                    }
                    v1 = a1 - 0x151D46F == 9;
                }
            } else {
                if (a1 == 0xD7512C + 2) {
                    return true;
                }
                if (a1 > 5101014) {
                    if (a1 == 0x50E309) {
                        return true;
                    }
                    v1 = a1 == 0xB8F3A8 + 1;
                } else {
                    if (a1 == 0x4DD5D2 + 4 || a1 == 2221012) {
                        return true;
                    }
                    v1 = a1 == 0x4DD5D2 + 2;
                }
            }
        }
        return v1;
    }

    public static int sub_7F9870(int a1) {
        boolean v1; // zf

        if (a1 > 0x23640F1) {
            if (a1 == 0x23640F3 + 1 || a1 - (0x23640F3 + 1) == 996) {
                return 1;
            }
            v1 = a1 - (0x23640F3 + 1) == 999;
        } else {
            if (a1 == 0x23640F1) {
                return 1;
            }
            if (a1 > 0x23619E1 + 1) {
                v1 = a1 == 0x2361DC8 + 1;
            } else {
                if (a1 == 0x23619E1 + 1 || a1 == 0x234934A) {
                    return 1;
                }
                v1 = a1 == 0x2349727 + 2;
            }
        }
        if (!v1) {
            return 0;
        }
        return 1;
    }

    public static boolean sub_8242D0(int a1) {
        int v1; // eax
        boolean result; // eax

        if (a1 <= 0) {
            return result = a1 - 90000000 >= 0 && a1 - 90000000 < 12;
        }
        v1 = a1 / 10000;
        if (a1 / 10000 == 8000) {
            v1 = a1 / 100;
        }
        if (v1 == 9500) {
            result = false;
        } else {
            result = a1 - 90000000 >= 0 && a1 - 90000000 < 12;
        }
        return result;
    }

    public static int sub_846930(int a1) {
        int result; // eax

        if (a1 > 0x1421D0A) {
            if (a1 == 0x1421D0B + 1 || a1 == 0x1424419) {
                return 1;
            }
        } else {
            if (a1 == 0x1421D0A) {
                return 1;
            }
            if (a1 > 0x1407331 + 1) {
                if (a1 >= 0x1421D05 + 1 && a1 <= 0x1421D07) {
                    return 1;
                }
            } else if (a1 == 0x1407331 + 1
                    || a1 >= 0x1406F45 + 1 && a1 <= 0x1406F47) {
                return 1;
            }
        }
        switch (a1) {
            case 80001925:
            case 80001926:
            case 80001927:
            case 80001936:
            case 80001937:
            case 80001938:
                return 1;
            default:
                result = 0;
                break;
        }
        return result;
    }

    public static boolean sub_847580(int a1) {
        return a1 == 0x23619E0 + 2
                || a1 - (0x23619E0 + 2) == 9999
                || a1 - (0x23619E0 + 2) == 10002;
    }

    public static boolean is_shadow_assult(int a1) {
        return (a1 - 400041002) <= 3 && (a1 - 400041002) >= -3;
    }

    public static boolean sub_8327B0(int skill) {
        return skill == 13111020;
    }

    public static boolean is_pathfinder_blast_skill(int a1) {
        boolean v1; // zf

        if (a1 > 3321005) {
            if (a1 == 3321039) {
                return true;
            }
            v1 = a1 == 400031035;
        } else {
            if (a1 == 3321005 || a1 == 3301004 || a1 == 3311011) {
                return true;
            }
            v1 = a1 == 3311013;
        }
        if (!v1) {
            return false;
        }
        return true;
    }

    public static boolean sub_7FB860(int a1) {
        boolean v1; // zf

        if (a1 <= 64111012) {
            if (a1 == 64111012) {
                return true;
            }
            if (a1 > 3311013) {
                if (a1 == 3321005) {
                    return true;
                }
                v1 = a1 == 3321039;
            } else {
                if (a1 == 3311013 || a1 == 3301004) {
                    return true;
                }
                v1 = a1 == 3311011;
            }
            return v1;
        }
        if (a1 > 400021053) {
            v1 = a1 == 400031035;
            return v1;
        }
        if (a1 != 400021053) {
            if (a1 < 400020009) {
                return false;
            }
            if (a1 > 400020011) {
                v1 = a1 == 400021029;
                return v1;
            }
        }
        return true;
    }

    public static boolean sub_896160(int a1) {
        boolean v1; // zf

        if (a1 > 400021028) {
            if (a1 > 400041020) {
                if (a1 > 400051008) {
                    v1 = a1 == 400051016;
                } else {
                    if (a1 == 400051008 || a1 == 400041034) {
                        return true;
                    }
                    v1 = a1 == 400051003;
                }
                if (v1) {
                    return v1 == true;
                }
            }
            if (a1 != 400041020) {
                if (a1 > 400031048) {
                    if (a1 < 400041016 || a1 > 400041018) {
                        return false;
                    }
                } else if (a1 != 400031048) {
                    switch (a1) {
                        case 400021047:
                        case 400021048:
                        case 400021064:
                        case 400021065:
                            return true;
                        default:
                            return false;
                    }
                }
            }
            return true;
        }
        if (a1 == 400021028) {
            return true;
        }
        if (a1 > 152120003) {
            if (a1 <= 400021004) {
                if (a1 == 400021004 || a1 == 152121004) {
                    return true;
                }
                v1 = a1 == 400011004;
                if (v1) {
                    return v1 == true;
                }
            }
            if (a1 < 400021009 || a1 > 400021011) {
                return false;
            }
            return true;
        }
        if (a1 == 152120003) {
            return true;
        }
        if (a1 > 0x4C4BF12) {
            v1 = a1 == 152001002;
        } else {
            if (a1 == 0x4C4BF12 || a1 == 0x4C4BE83) {
                return true;
            }
            v1 = a1 == 0x4C4BF10;
        }
        return false;
    }

    public static boolean is_screen_attack(int a1) {
        boolean v1; // zf

        if (a1 > 0x1424820 + 1) {
            if (a1 == (80001431)) {
                return true;
            }
            v1 = a1 == 100001283;
        } else {
            if ((a1 == 0x1424820 + 1)
                    || a1 == 0xC8361C
                    || a1 == 0xD77856 + 6) {
                return true;
            }
            v1 = a1 == 0xE6BA9A + 2;
        }
        if (!v1) {
            return false;
        }
        return true;
    }

    public static boolean is_thunder_rune(int a1) {
        return a1 == 80001762 || a1 == 80002212 || a1 == 80002463;
    }

    public static int get_evan_job_level(final int job) {
        int result; // eax@2

        switch (job) {
            case 2200:
            case 2210:
                result = 1;
                break;
            case 2211:
            case 2212:
            case 2213:
                result = 2;
                break;
            case 2214:
            case 2215:
            case 2216:
                result = 3;
                break;
            case 2217:
            case 2218:
                result = 4;
                break;
            default:
                result = 0;
                break;
        }
        return result;
    }

    public static int getJaguarIdByMob(int mob) {
        switch (mob) {
            case 9304004:
                return 1932033;
            case 9304003:
                return 1932032;
            case 9304002:
                return 1932031;
            case 9304001:
                return 1932030;
            case 9304000:
                return 1932015;
            case 9304005:
                return 1932036;
            case 9304006:
                return 1932100;
        }
        return 0;
    }

    public static int getJaguarType(int mobid) {
        return mobid - 9303999;
    }

    public static boolean is_evan_force_skill(int a1) {
        boolean v2; // zf@8

        if (a1 > 22141012) {
            if (a1 > 400021012) {
                v2 = a1 == 400021046;
            } else {
                if (a1 == 400021012) {
                    return true;
                }
                if (a1 < 22171062) {
                    return false;
                }
                if (a1 <= 22171063) {
                    return true;
                }
                v2 = a1 == 80001894;
            }
            if (!v2) {
                return false;
            }
            return true;
        }
        if (a1 >= 22141011) {
            return true;
        }
        if (a1 > 22111017) {
            v2 = a1 == 22140022;
            if (!v2) {
                return false;
            }
            return true;
        }
        if (a1 == 22111017
                || a1 <= 22111009 + 3
                && (a1 >= 22111009 + 2
                || a1 >= 22110017 + 5 && a1 <= 22110017 + 6)) {
            return true;
        }
        return false;
    }

    public static int bullet_count_bonus(int a1) {
        if (a1 == 4121013) {
            return 4120051;
        }
        if (a1 == 5321012) {
            return 5320051;
        }
        return 0;
    }

    public static int attack_count_bonus(int a1) {
        int v1; // esi

        v1 = 0;
        if (a1 <= 15120003) {
            if (a1 != 15120003) {
                if (a1 <= 5121020) {
                    if (a1 != 5121020) {
                        if (a1 <= 3121020) {
                            if (a1 == 3121020) {
                                return 3120051;
                            }
                            if (a1 > 1221011) {
                                if (a1 == 2121006) {
                                    return 2120048;
                                }
                                if (a1 == 2221006) {
                                    return 2220048;
                                }
                                if (a1 == 3121015) {
                                    return 3120048;
                                }
                            } else {
                                if (a1 == 1221011) {
                                    return 1220050;
                                }
                                if (a1 == 1120017 || a1 == 1121008) {
                                    return 1120051;
                                }
                                if (a1 == 1221009) {
                                    return 1220048;
                                }
                            }
                            if (sub_833A80(a1)) {
                                v1 = 3320030;
                            }
                            return v1;
                        }
                        if (a1 <= 4341009) {
                            if (a1 == 4341009) {
                                return (4340046 + 2);
                            }
                            if (a1 == 3221017) {
                                return 3220048;
                            }
                            if (a1 == (4221005 + 2)) {
                                return (4220047 + 1);
                            }
                            if (a1 == (4330997 + 3)) {
                                return (4340041 + 4);
                            }
                            if (sub_833A80(a1)) {
                                v1 = 3320030;
                            }
                            return v1;
                        }
                        if (a1 != (5121005 + 2)) {
                            if ((a1 - 5121016) >= -1 && (a1 - 5121016) <= 1) {
                                return 5120051;
                            }
                            if (sub_833A80(a1)) {
                                v1 = 3320030;
                            }
                            return v1;
                        }
                    }
                    return (0x4E202B + 5);
                }
                if (a1 <= 0xB8A1BB + 1) {
                    if (a1 != (0xB8A1BB + 1)) {
                        if (a1 <= 5321004) {
                            if (a1 == 0x513128 + 1) {
                                return (0x512D68 + 3);
                            }
                            if (a1 == (0x4FAA97 + 1)) {
                                return 0x4FA6CF;
                            }
                            if (a1 == (0x512D49 + 2)) {
                                return (0x512D68 + 3);
                            }
                            if (a1 == 0x513128) {
                                return (0x512D6E + 2);
                            }
                            if (sub_833A80(a1)) {
                                v1 = 3320030;
                            }
                            return v1;
                        }
                        if (a1 == (0xA9B1CD + 2) || a1 - (0xA9B1CD + 2) == 100) {
                            return 0xA9ADB0;
                        }
                        if (a1 - (0xA9B1CD + 2) != 878923) {
                            if (sub_833A80(a1)) {
                                v1 = 3320030;
                            }
                            return v1;
                        }
                    }
                    return 0xB8EFED;
                }
                if (a1 <= 0xC835EA) {
                    if (a1 == 0xC835EA) {
                        return (0xC8322F + 1);
                    }
                    if (a1 != 0xB8C8CC && a1 - 0xB8C8CC != 9982) {
                        if (a1 - 0xB8C8CC == 9983) {
                            return (0xB8EFED + 1);
                        }
                        if (sub_833A80(a1)) {
                            v1 = 3320030;
                        }
                        return v1;
                    }
                    return 0xB8EFED;
                }
                if (a1 == 0xD7782A) {
                    return (0xD7746B + 2);
                }
                if (sub_833A80(a1)) {
                    v1 = 3320030;
                }
                return v1;
            }
            return 0xE6B6AD;
        }
        if (a1 <= 0x30C0B70) {
            if (a1 == 0x30C0B70) {
                return 0x30C07B0;
            }
            if (a1 <= 0x17F50EB + 2) {
                if (a1 != (0x17F50EB + 2)) {
                    if (a1 <= 0x1424406) {
                        if (a1 == 0x1424406) {
                            return 0x142442F + 2;
                        }
                        if (a1 == 0xE6BA6A) {
                            return 0xE6B6AD + 3;
                        }
                        if (a1 == (0x1421D03 + 1) || a1 == (0x14220EC + 1)) {
                            return 0x142442F;
                        }
                        if (sub_833A80(a1)) {
                            v1 = 3320030;
                        }
                        return v1;
                    }
                    if (a1 > 0x14247F9) {
                        if (a1 == 0x151D477) {
                            return 0x15249E6;
                        }
                        if (sub_833A80(a1)) {
                            v1 = 3320030;
                        }
                        return v1;
                    }
                    if (a1 >= 0x14247F6 + 2 || a1 == (0x1424415 + 1)) {
                        return 0x1424442;
                    }
                    if (sub_833A80(a1)) {
                        v1 = 3320030;
                    }
                    return v1;
                }
                return 0x17F4D94;
            }
            if (a1 <= 0x2366800 + 1) {
                if (a1 == (0x2366800 + 1)) {
                    return 0x236682D;
                }
                if (a1 == 0x1DADE68 + 1) {
                    return (0x1DADAB0 + 2);
                }
                if (a1 == 0x217E778) {
                    return 0x217E3B0 + 3;
                }
                if (a1 == (0x23640EF + 3)) {
                    return 0x236682D;
                }
                if (sub_833A80(a1)) {
                    v1 = 3320030;
                }
                return v1;
            }
            if (a1 != 0x30C07B9) {
                if (a1 == 0x30C0B6F) {
                    return 0x30C07B3;
                }
                if (sub_833A80(a1)) {
                    v1 = 3320030;
                }
                return v1;
            }
            return 0x30C07BA;
        }
        if (a1 <= 152110004) {
            if (a1 != 152110004) {
                if (a1 <= 0x3E1AAEC + 4) {
                    if (a1 < 0x3E1AAEC + 3) {
                        if (a1 == 0x30C0B71) {
                            return 0x30C07BA;
                        }
                        if ((a1 - 51121009) != 0x9896DB && a1 - 51121009 - 0x9896DB != 101) {
                            if (sub_833A80(a1)) {
                                v1 = 3320030;
                            }
                            return v1;
                        }
                        return 0x3A49E2D;
                    }
                    return 0x3E1A733;
                }
                if (a1 == (0x3E1AB49 + 4)) {
                    return 0x3E1A733;
                }
                if (a1 != 152001001) {
                    if (sub_833A80(a1)) {
                        v1 = 3320030;
                    }
                    return v1;
                }
            }
            return 152120032;
        }
        if (a1 > 400010070) {
            if (a1 < 400011079) {
                if (sub_833A80(a1)) {
                    v1 = 3320030;
                }
                return v1;
            }
            if (a1 > 400011082) {
                if (a1 != 400051043) {
                    if (sub_833A80(a1)) {
                        v1 = 3320030;
                    }
                    return v1;
                }
                return 0x17F4D94;
            }
            return 0x3A49E2D;
        }
        if (a1 == 400010070) {
            return 0x1424442;
        }
        if (a1 > 152121004) {
            if (a1 <= 152121006) {
                return 152120038;
            }
            if (sub_833A80(a1)) {
                v1 = 3320030;
            }
            return v1;
        }
        if (a1 == 152121004) {
            return 152120035;
        }
        if (a1 == 152120001) {
            return 152120032;
        }
        if (sub_833A80(a1)) {
            v1 = 3320030;
        }
        return v1;
    }

    public static boolean sub_833A80(int a1) {
        return a1 == 3011004
                || a1 == 3300002
                || a1 == 3321003
                || a1 == 3301003
                || a1 == 3310001
                || a1 == 3321004
                || a1 == 3301004
                || a1 == 3311013
                || a1 == 3321005
                || a1 == 3311002
                || a1 == 3321006
                || a1 == 3311003
                || a1 == 3321007;
    }

    public static int checkMountItem(final int sourceid) {
        return getMountItemEx(sourceid);
    }

    public static boolean isSeparatedSp(int a1) {

        if ((a1 == 0
                || a1 == 100 || GameConstants.isHero(a1) || GameConstants.isPaladin(a1) || GameConstants.isDarkKnight(a1))
                || (a1 == 200 || GameConstants.isFPMage(a1) || GameConstants.isILMage(a1) || GameConstants.isBishop(a1))
                || (a1 == 300 || GameConstants.isBowMaster(a1) || GameConstants.isMarksMan(a1) || GameConstants.isPathFinder(a1))
                || (a1 == 400 || GameConstants.isNightLord(a1) || GameConstants.isShadower(a1) || GameConstants.isDualBlade(a1))
                || (a1 == 500 || GameConstants.isViper(a1) || GameConstants.isCaptain(a1) || GameConstants.isCannon(a1))
                || GameConstants.isIllium(a1)
                || GameConstants.isArk(a1)
                || GameConstants.isKOC(a1)
                || GameConstants.isResist(a1)
                || GameConstants.isEvan(a1)
                || GameConstants.isMercedes(a1)
                || GameConstants.isPhantom(a1)
                || GameConstants.isMichael(a1)
                || GameConstants.isLuminous(a1)
                || (GameConstants.isKaiser(a1) || GameConstants.isKadena(a1) || GameConstants.isAngelicBuster(a1)) // Nova
                || GameConstants.isAdel(a1)
                || GameConstants.isZero(a1)
                || GameConstants.isEunWol(a1)
                || GameConstants.isAran(a1)
                || GameConstants.isKinesis(a1)
                || GameConstants.isHoyeong(a1)
                || GameConstants.isKain(a1)
                || GameConstants.isLala(a1)) {
            /*if (job == 0
             || job == 100
             || job == 110
             || job == 111
             || job == 112
             || job == 120
             || job == 121
             || job == 122
             || job == 130
             || job == 131
             || job == 132
             || (job >= 200 && job < 300)
             || (job >= 300 && job < 400)
             || (job >= 400 && job < 500)
             || (job >= 500 && job < 600)
             || isKOC(job)
             || isResist(job)
             || isEvan(job)
             || isMercedes(job)
             || isPhantom(job)
             || isMichael(job)
             || isLuminous(job)
             || isKaiser(job) || isAngelicBuster(job)
             || isZero(job)
             || isEunWol(job)
             || isAran(job)
             || isKinesis(job)) {*/
            return true;
        }
        return false;
    }

    public static byte[] getServerIp(String ip) {
        return new byte[]{(byte) 175, (byte) 207, (byte) 0, (byte) 33};
    }

    public static boolean sub_1F04F40(int a1) {
        boolean v1; // zf

        if (a1 > 13121009) {
            if (a1 == 36110005) {
                return true;
            }
            v1 = a1 == 65101006;
        } else {
            if (a1 == 13121009 || a1 == (11121010 + 3)) {
                return true;
            }
            v1 = a1 == 12100029;
        }
        return v1;
    }

    public static boolean isLinkMap(int mapid) {
        switch (mapid) {
            case 993014200:
            case 993018200:
            case 993021200:
            case 993029200:
            case 940711300:
                return true;
        }
        return false;
    }

    public static boolean isTowerChair(final int chairid) {
        return chairid / 1000 == 3017;
    }

    public static boolean isTextChair(final int chairid) {
        return chairid / 1000 == 3014;
    }

    public static boolean isCooltimeKeyDownSkill(int skillid) {
        if (sub_7C6810(skillid) || sub_781900(skillid) || is_screen_attack_skill(skillid)) {
            return true;
        }
        switch (skillid) {
            case 400011089:
            case 400011030:
            case 400011031:
            case 400041021:
            case 400021046:
            case 400021012:
            case 400011108:
            case 400011109:
            case 151111002:
            case 151121003:
            case 2221011:
            case 4221052:
            case 5220023:
            case 25121030:
            case 22110025:
            case 22110014:
            case 22140022:
            case 5220024:
            case 5220025:
            case 12121054:
            case 12121055:
            case 3301008:
            case 400051017:
            case 400031001:
            case 22171063:
            case 11121014:
            case 3321036:
            case 3321037:
            case 3321038:
            case 3321039:
            case 3321040:
            case 3321035:
            case 14121003:
            case 14121004:
            case 21120018:
            case 21120019:
            case 21120023:
            case 21120026:
            case 21120027:
            case 22171080:
            case 24121005:
            case 24120055:
            case 31211001:
            case 64111004:
            case 64111012:
            case 64121002:
            case 64121003:
            case 64121011:
            case 65121003:
            case 65121052:
            case 131001004:
            case 152121004:
            case 155101104:
            case 155101114:
            case 155101204:
            case 155101214:
            case 155111202:
            case 155111211:
            case 155111212:
            case 155111306:
            case 155121341:
            case 400011028:
            case 400011038:
            case 400011039:
            case 400011068:
            case 400011069:
            case 400011091:
            case 400011092:
            case 400011093:
            case 400011094:
            case 400011095:
            case 400011096:
            case 400011097:
            case 400021004:
            case 400021031:
            case 400021040:
            case 400021061:
            case 400031024:
            case 400040006:
            case 400041006:
            case 400041007:
            case 400051006:
            case 400051016:
            case 400051040:
            case 400051041:
            case 400001011:
            case 400021086:
                return true;
        }
        return false;
    }

    public static boolean isAutoAttackSkill(int skillid) {
        switch (skillid) {
            case 3111013:
            case 31121005:
            case 31221001:
            case 142100010:
            case 151111003:
            case 400011052:
            case 400051015:
            case 151101006:
            case 151101007:
            case 151101008:
            case 151101009:
            case 400031020:
                return true;
        }
        return false;
    }

    public static boolean isNoApplySkill(int skillid) {
        if (isTryFling(skillid)) {
            return true;
        }
        switch (skillid) {
            case 22111012:
            case 80001762:
            case 5311010:
            case 162111006:
            case 400011132:
            case 400031031:
            case 400051003:
            case 400051004:
            case 400051005:
            case 164101004:
            case 400011137:
            case 152120001:
            case 400011047:
            case 400011001:
            case 400011110:
            case 400011111:
            case 400011002:
            case 400021061:
            case 400021063:
            case 24121010:
            case 24111008:
            case 12121055:
            case 12100029:
            case 2321055:
            case 3100010:
            case 5221015:
            case 400031046:
            case 21001008:
            case 151001001:
            case 151111003:
            case 155101002:
            case 155111003:
            case 155121003:
            case 151121001:
            case 155001000:
            case 400011108:
            case 400011109:
            case 400011106:
            case 400011107:
            case 400041053:
            case 400021004:
            case 142101009:
            case 142120002:
            case 142120001:
            case 142110003:
            case 142111007:
            case 164001002:
            case 400051048:
            case 400051047:
            case 164120007:
            case 164121004:
            case 400020051:
            case 142100010:
            case 400031035:
            case 400021048:
            case 400021053:
            case 400041007:
            case 400021041:
            case 400021049:
            case 400021050:
            case 400031017:
            case 400031018:
            case 400031019:
            case 31101002:
            case 21120024:
            case 3300005:
            case 3301004:
            case 3301009:
            case 3311003:
            case 3310004:
            case 3311011:
            case 3311013:
            case 3321005:
            case 3321007:
            case 3321015:
            case 3321017:
            case 3321019:
            case 3321021:
            case 3321035:
            case 3321036:
            case 3321037:
            case 3321038:
            case 3321039:
            case 3321040:
            case 5211014:
            case 400011099:
            case 400011101:
            case 400021045:
            case 400021070:
            case 400021077:
            case 400010030:
            case 400011031:
            case 400031026:
            case 400011074:
            case 400011075:
            case 400011076:
            case 400041036:
            case 400031036:
            case 101120205:
            case 27120211:
            case 27121201:
            case 2321007:
            case 3011004:
            case 3300002:
            case 3321003:
            case 14111022:
            case 14111023:
            case 4321002:
            case 5101012:
            case 5300007:
            case 5301001:
            case 13120003:
            case 13110022:
            case 13100022:
            case 13120010:
            case 13110027:
            case 13100027:
            case 0:
            case 80001770:
            case 155100009:
            case 142121030:
            case 400031012:
            case 400031013:
            case 400031014:
            case 24120055:
            case 95001000:
            case 2121052:
            case 2121006:
            case 5220023:
            case 5220024:
            case 5220025:
            case 64121012:
            case 64121013:
            case 64121014:
            case 64121015:
            case 64121017:
            case 64121018:
            case 64121019:
            case 64121022:
            case 64121023:
            case 64121024:
            case 400011058:
            case 400011072:
            case 400051006:
            case 400011004:
            case 400051007:
            case 400051013:
            case 400011102:
            case 400021001:
            case 400021008:
            case 400021028:
            case 400021029:
            case 400051025:
            case 400051026:
            case 3111013:
            case 27101202:
            case 2121054:
            case 14121004:
            case 4221013:
            case 400041039:
            case 24100003:
            case 24120002:
            case 31121005:
            case 4221052:
            case 65121052:
            case 13121054:
            case 400031022:
            case 400051018:
            case 400051019:
            case 400051020:
            case 400051027:
            case 4341054:
            case 400031000:
            case 400031001:
            case 400051334:
            case 400041037:
            case 2221012:
            case 12120011:
            case 4341052:
            case 35111003:
            case 35001002:
            case 4100012:
            case 4120019:
            case 400031015:
            case 400031016:
            case 36110004:
            case 2120013:
            case 2220014:
            case 32121011:
            case 155111207:
            case 400001038:
            case 400041038:
            case 400010010:
            case 36001005:
            case 27111100:
            case 400011052:
            case 400011053:
            case 400041010:
            case 400041043:
            case 400031032:
            case 400031033:
            case 400021066:
            case 2121011:
            case 12111003:
            case 2121007:
            case 35121003:
            case 61110211:
            case 61101002:
            case 64101002:
            case 36101001:
            case 400001018:
            case 35101002:
            case 35110017:
            case 35120017:
            case 61111100:
            case 31201001:
            case 32120052:
            case 32120055:
            case 400020009:
            case 400020010:
            case 400020011:
            case 400021009:
            case 400021010:
            case 400021011:
            case 400021075:
            case 400021076:
            case 14000028:
            case 14000029:
            case 4210014:
            case 31221014:
            case 65111007:
            case 65120011:
            case 400041040:
            case 400041045:
            case 400041046:
            case 400041016:
            case 400041017:
            case 400041018:
            case 400041020:
            case 400041022:
            case 400041023:
            case 400041024:
            case 400011019:
            case 400051041:
            case 400051008:
            case 400031021:
            case 400051015:
            case 400031020:
            case 400031003:
            case 400031004:
            case 400051044:
            case 400051045:
            case 32111016:
            case 400021047:
            case 64101008:
            case 400051049:
            case 400051050:
            case 142110011:
            case 151101001:
            case 400031066:
            case 400031061:
            case 400031065:
            case 64121055:
                return true;
        }
        return false;
    }

    public static long getDreamBreakerHP(final int stage) {
        if (stage < 10) {
            return 220000000L;
        } else if (stage >= 10 && stage < 20) {
            return 500000000L;
        } else if (stage >= 20 && stage < 30) {
            return 1200000000L;
        } else if (stage >= 30 && stage < 40) {
            return 100000000L;
        } else if (stage >= 40 && stage < 50) {
            return 5400000000L;
        } else if (stage >= 50 && stage < 60) {
            return 9750000000L;
        } else if (stage >= 60 && stage < 70) {
            return 15250000000L;
        } else if (stage >= 70 && stage < 80) {
            return 24700000000L;
        } else if (stage >= 80 && stage < 90) {
            return 36000000000L;
        } else if (stage == 90) {
            return 50000000000L;
        } else if (stage >= 91 && stage < 100) {
            return 87000000000L;
        } else if (stage == 100) {
            return 135000000000L;
        } else if (stage >= 101 && stage < 110) {
            return 335000000000L;
        } else if (stage >= 110 && stage < 120) {
            return 373000000000L;
        } else if (stage >= 120 && stage < 130) {
            return 403000000000L;
        } else if (stage >= 130 && stage < 140) {
            return 435000000000L;
        } else if (stage >= 140 && stage < 150) {
            return 469000000000L;
        } else if (stage >= 150 && stage < 160) {
            return 503000000000L;
        } else if (stage >= 160 && stage < 170) {
            return 533000000000L;
        } else if (stage >= 170 && stage < 180) {
            return 569000000000L;
        } else if (stage >= 180 && stage < 190) {
            return 603000000000L;
        } else if (stage >= 190 && stage < 200) {
            return 635000000000L;
        } else if (stage == 200) {
            return 669000000000L;
        } else if (stage == 201) {
            return 4700000000000L;
        } else {
            return (long) Math.max(1, 4700000000000L * (stage - 201));
        }
    }

    public static boolean isNoReflectDamageSkill(Skill skill) {
        if (skill.isVMatrix() || is_forceAtom_attack_skill(skill.getId()) || skill.isIgnoreCounter()) {
            return true;
        }
        switch (skill.getId()) {
            case 2100010:
            case 2101010:
            case 2121052:
            case 2121054:
            case 2121055:
            case 2201009:
            case 2221012:
            case 2221052:
            case 4111003:
            case 4341052:
            case 5121013:
            case 5221013:
            case 5221022:
            case 5321001:
            case 11121052:
            case 11121055:
            case 12120011:
            case 14121003:
            case 14121004:
            case 22110014:
            case 22110022:
            case 22110023:
            case 22110024:
            case 22110025:
            case 22111012:
            case 22140014:
            case 22140015:
            case 22140022:
            case 22140023:
            case 22140024:
            case 22141012:
            case 22170065:
            case 22170066:
            case 22170067:
            case 22171063:
            case 22171081:
            case 23111008:
            case 23111009:
            case 23111010:
            case 24120055:
            case 25100010:
            case 25120115:
            case 25121006:
            case 31220013:
            case 31221001:
            case 31221014:
            case 32001014:
            case 32121004:
            case 32121011:
            case 33001016:
            case 33101115:
            case 33111013:
            case 33111015:
            case 33120056:
            case 33121012:
            case 33121017:
            case 33121255:
            case 35101002:
            case 35101012:
            case 35111002:
            case 35121009:
            case 35121052:
            case 36001005:
            case 36110004:
            case 36110005:
            case 36111004:
            case 36121002:
            case 36121013:
            case 37001002:
            case 37001004:
            case 37111000:
            case 51001005:
            case 61111002:
            case 61111100:
            case 61111113:
            case 61111218:
            case 61111220:
            case 64101009:
            case 64111012:
            case 64111013:
            case 64121020:
            case 65111007:
            case 65111100:
            case 65121052:
            case 101100100:
            case 101100101:
            case 101100201:
            case 101100202:
            case 101110200:
            case 101110201:
            case 101110204:
            case 101120101:
            case 101120103:
            case 101120105:
            case 101120106:
            case 101120203:
            case 101120205:
            case 101120206:
            case 131001004:
            case 131001007:
            case 131001008:
            case 131001011:
            case 142101009:
            case 151111003:
            case 152001003:
            case 152101008:
            case 152121005:
            case 152121006:
            case 155001000:
            case 155101002:
            case 155101008:
            case 155111003:
            case 155111207:
            case 155111306:
            case 155121003:
            case 155121306:
                return true;
        }
        return false;
    }

    public static boolean isMagneticPet(int id) {
        switch (id) {
            case 5000930:
            case 5000931:
            case 5000932:
                return true;
        }
        return false;
    }

    public static void AttackBonusRecv(LittleEndianAccessor lea, AttackInfo ret) {
        lea.skip(1);
        lea.skip(1); // 350 new
        ret.slot = lea.readShort();
        ret.item = lea.readInt();
        lea.skip(1);
        lea.skip(1);
        lea.skip(1); // 307++
        lea.skip(1); // 342++
        lea.skip(4); // 333++
        lea.skip(8); // 333++
        lea.skip(4); // 336++
        lea.skip(4); // 342++
        int c = lea.readInt();
        for (int i = 0; i < c; i++) {
            lea.readInt();
        }
    }

    public static void calcAttackPosition(LittleEndianAccessor lea, AttackInfo ret) {
        lea.skip(4); // 307++
        byte aa = lea.readByte(); // 307++

        if (aa != 0) {
            lea.skip(4); //342++
            int k = -1;
            do {
                k = lea.readInt();
                //  System.out.println("calcAttackPos"+k);
                switch (k) {
                    case 1: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.readInt();
                            lea.readInt();
                            lea.readInt();
                            lea.readByte();
                            int a = lea.readByte();
                            for(int i =0; i<a;i++){
                                lea.readInt();
                            }
                        }
                        break;
                    }
                    case 2: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.skip(19); // 1 + 1 + 4 + 4 + 1 + 4 + 4
                        }
                        break;
                    }
                    case 3: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.readByte();
                            lea.readInt();
                        }
                        break;
                    }
                    case 4: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.skip(24);
                        }
                        break;
                    }
                    case 5: {
                        lea.readByte();
                        break;
                    }
                    case 6: {
                        lea.readByte();
                        break;
                    }
                    case 7: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            ret.plusPosition2 = new Point(lea.readInt(), lea.readInt());
                            ret.rlType = lea.readByte();
                        }
                        break;
                    }
                    case 8: {
                        ret.across = lea.readByte() != 0;
                        if (ret.across) {
                            ret.acrossPosition = new Rectangle(lea.readInt(), lea.readInt(), lea.readInt(), lea.readInt());
                        }
                        break;
                    }
                    case 9: {
                        ret.across = lea.readByte() != 0;
                        if (ret.across) {
                            ret.acrossPosition = new Rectangle(lea.readInt(), lea.readInt(), lea.readInt(), lea.readInt());
                        } else {
                            byte x = lea.readByte();
                            if (x > 0) {
                                lea.skip(16);
                            }
                        }
                        break;
                    }
                    case 10: {
                        lea.readByte();
                        break;
                    }
                    case 11: {
                        lea.readByte();
                        break;
                    }
                    case 12: {
                        lea.readByte();
                        break;
                    }
                    case 13: {
                        lea.readByte();
                        break;
                    }
                    case 14: {
                        lea.readByte();
                        break;
                    }
                    case 15: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            int count = lea.readInt();
                            for (int i = 0; i < count; i++) {
                                lea.readLong();
                            }
                            lea.readLong();
                            lea.readLong();
                        }
                        break;
                    }
                    case 16: {
                        lea.readByte();
                        break;
                    }
                    case 19:
                        if (lea.readByte() == 1) {
                            ret.unk = lea.readInt();
                            ret.usedCount = lea.readInt();
                            lea.readInt(); // 1124 new
                        }
                        break;
                    case 20: {
                        lea.readByte();
                        break;
                    }
                    case 21: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.skip(32); // 4 * 8
                        }
                        break;
                    }
                    case 22: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            int size = lea.readInt();
                            for (int i = 0; i < size; ++i) {
                                lea.readInt(); // 0
                                ret.mistPoints.add(new Point(lea.readInt(), lea.readInt()));
                            }
                        }
                        break;
                    }
                    case 31: {
                        int size = lea.readInt();
                        break;
                    }
                    case 34: {
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.skip(32);
                        }
                        break;
                    }
                    case 36:
                        byte x = lea.readByte();
                        if (x > 0) {
                            lea.readInt();
                            lea.readInt(); //342+
                        }
                        break;
                    case 37:
                        if (lea.readByte() > 0) {
                            lea.readInt();
                            lea.readInt(); //342+
                        }
                        break;
                }
            } while (k != -1);
        }
    }

    public static void sub_2224400(LittleEndianAccessor lea, AttackInfo ret) {
        byte unk = lea.readByte();

        if (unk == 1) {
            lea.readMapleAsciiString();
            lea.readInt();
            int a = lea.readInt();
            if (a > 0) {
                for (int k = 0; k < a; ++k) {
                    lea.readMapleAsciiString();
                }
            }

        } else if (unk == 2) {
            lea.readMapleAsciiString();
            lea.readInt();
        }

        lea.skip(1);

        if (ret != null) {
            ret.attPos = lea.readPos(); // monster
            ret.attPos2 = lea.readPos(); // player
            ret.attPos3 = lea.readPos(); // skleton -> 사용 X
        } else {
            lea.readPos();
            lea.readPos();
            lea.readPos();
        }

        lea.skip(1);
        lea.skip(4);
    }

    public static void sendFireOption(MaplePacketLittleEndianWriter mplew, long newRebirth, Equip equip) {
        int[] rebirth = new int[4];
        String fire = String.valueOf(newRebirth);

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        Equip ordinary = (Equip) MapleItemInformationProvider.getInstance().getEquipById(equip.getItemId(), false);

        int ordinaryPad = ordinary.getWatk() > 0 ? ordinary.getWatk() : ordinary.getMatk();
        int ordinaryMad = ordinary.getMatk() > 0 ? ordinary.getMatk() : ordinary.getWatk();

        if (fire.length() == 12) {
            rebirth[0] = Integer.parseInt(fire.substring(0, 3));
            rebirth[1] = Integer.parseInt(fire.substring(3, 6));
            rebirth[2] = Integer.parseInt(fire.substring(6, 9));
            rebirth[3] = Integer.parseInt(fire.substring(9));
        } else if (fire.length() == 11) {
            rebirth[0] = Integer.parseInt(fire.substring(0, 2));
            rebirth[1] = Integer.parseInt(fire.substring(2, 5));
            rebirth[2] = Integer.parseInt(fire.substring(5, 8));
            rebirth[3] = Integer.parseInt(fire.substring(8));
        } else if (fire.length() == 10) {
            rebirth[0] = Integer.parseInt(fire.substring(0, 1));
            rebirth[1] = Integer.parseInt(fire.substring(1, 4));
            rebirth[2] = Integer.parseInt(fire.substring(4, 7));
            rebirth[3] = Integer.parseInt(fire.substring(7));
        }

        Map<Integer, Integer> zz = new HashMap<>();

        for (int i = 0; i < rebirth.length; ++i) {
            int value = rebirth[i] - (rebirth[i] / 10 * 10);

            fireValue(rebirth[i] / 10, ii.getReqLevel(equip.getItemId()), value, ordinaryPad, ordinaryMad, equip.getItemId(), zz); // value
        }

        mplew.writeInt(zz.size());

        for (Entry<Integer, Integer> z : zz.entrySet()) {
            mplew.writeInt(z.getKey());
            mplew.writeInt(z.getValue());
        }
    }

    public static void fireValue(int randomOption, int reqLevel, int randomValue, int ordinaryPad, int ordinaryMad, int itemId, Map<Integer, Integer> zz) {
        switch (randomOption) {
            case 0:
            case 1:
            case 2:
            case 3:
                if (zz.containsKey(randomOption)) {
                    zz.put(randomOption, zz.get(randomOption) + ((reqLevel / 20 + 1) * randomValue));
                } else {
                    zz.put(randomOption, ((reqLevel / 20 + 1) * randomValue));
                }
                break;
            case 4:
                if (zz.containsKey(0)) {
                    zz.put(0, zz.get(0) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(0, ((reqLevel / 40 + 1) * randomValue));
                }

                if (zz.containsKey(1)) {
                    zz.put(1, zz.get(1) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(1, ((reqLevel / 40 + 1) * randomValue));
                }
                break;
            case 5:
                if (zz.containsKey(0)) {
                    zz.put(0, zz.get(0) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(0, ((reqLevel / 40 + 1) * randomValue));
                }

                if (zz.containsKey(2)) {
                    zz.put(2, zz.get(2) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(2, ((reqLevel / 40 + 1) * randomValue));
                }
                break;
            case 6:
                if (zz.containsKey(0)) {
                    zz.put(0, zz.get(0) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(0, ((reqLevel / 40 + 1) * randomValue));
                }

                if (zz.containsKey(3)) {
                    zz.put(3, zz.get(3) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(3, ((reqLevel / 40 + 1) * randomValue));
                }
                break;
            case 7:
                if (zz.containsKey(1)) {
                    zz.put(1, zz.get(1) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(1, ((reqLevel / 40 + 1) * randomValue));
                }

                if (zz.containsKey(2)) {
                    zz.put(2, zz.get(2) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(2, ((reqLevel / 40 + 1) * randomValue));
                }
                break;
            case 8:
                if (zz.containsKey(1)) {
                    zz.put(1, zz.get(1) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(1, ((reqLevel / 40 + 1) * randomValue));
                }

                if (zz.containsKey(3)) {
                    zz.put(3, zz.get(3) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(3, ((reqLevel / 40 + 1) * randomValue));
                }
                break;
            case 9:
                if (zz.containsKey(2)) {
                    zz.put(2, zz.get(2) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(2, ((reqLevel / 40 + 1) * randomValue));
                }

                if (zz.containsKey(3)) {
                    zz.put(3, zz.get(3) + ((reqLevel / 40 + 1) * randomValue));
                } else {
                    zz.put(3, ((reqLevel / 40 + 1) * randomValue));
                }
                break;
            case 10:
                zz.put(randomOption, (reqLevel * 3 * randomValue));
                break;
            case 11:
                zz.put(randomOption, (reqLevel * 3 * randomValue));
                break;
            case 13:
                zz.put(randomOption, (reqLevel / 20 + 1) * randomValue);
                break;
            case 17: {
                if (GameConstants.isWeapon(itemId)) {
                    switch (randomValue) {
                        case 3:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryPad * 1200) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryPad * 1500) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryPad * 1800) / 10000) + 1);
                            }
                            break;
                        case 4:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryPad * 1760) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryPad * 2200) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryPad * 2640) / 10000) + 1);
                            }
                            break;
                        case 5:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryPad * 2420) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryPad * 3025) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryPad * 3630) / 10000) + 1);
                            }
                            break;
                        case 6:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryPad * 3200) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryPad * 4000) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryPad * 4800) / 10000) + 1);
                            }
                            break;
                        case 7:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryPad * 4100) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryPad * 5125) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryPad * 6150) / 10000) + 1);
                            }
                            break;
                    }
                } else {
                    zz.put(randomOption, randomValue);
                }
                break;
            }
            case 18: {
                if (GameConstants.isWeapon(itemId)) {
                    switch (randomValue) {
                        case 3:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryMad * 1200) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryMad * 1500) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryMad * 1800) / 10000) + 1);
                            }
                            break;
                        case 4:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryMad * 1760) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryMad * 2200) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryMad * 2640) / 10000) + 1);
                            }
                            break;
                        case 5:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryMad * 2420) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryMad * 3025) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryMad * 3630) / 10000) + 1);
                            }
                            break;
                        case 6:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryMad * 3200) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryMad * 4000) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryMad * 4800) / 10000) + 1);
                            }
                            break;
                        case 7:
                            if (reqLevel <= 150) {
                                zz.put(randomOption, ((ordinaryMad * 4100) / 10000) + 1);
                            } else if (reqLevel <= 160) {
                                zz.put(randomOption, ((ordinaryMad * 5125) / 10000) + 1);
                            } else {
                                zz.put(randomOption, ((ordinaryMad * 6150) / 10000) + 1);
                            }
                            break;
                    }
                } else {
                    zz.put(randomOption, randomValue);
                }
                break;
            }
            case 19:
                zz.put(randomOption, randomValue);
                break;
            case 20:
                zz.put(randomOption, randomValue);
                break;
            case 21:
                zz.put(randomOption, (randomValue * 2));
                break;
            case 22:
                zz.put(randomOption, (-5 * randomValue));
                break;
            case 23:
                zz.put(randomOption, randomValue);
                break;
            case 24:
                zz.put(randomOption, randomValue);
                break;
        }
    }

    public static List<MatrixSkill> matrixSkills(LittleEndianAccessor slea) {
        int count = slea.readInt();
        List<MatrixSkill> skills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MatrixSkill skill = new MatrixSkill(slea.readInt(), slea.readInt(), slea.readInt(), slea.readShort(), slea.readPos(), slea.readInt(), slea.readByte());
            byte unk5 = slea.readByte();
            int x = 0, y = 0;
            if (unk5 > 0) {
                x = slea.readInt();
                y = slea.readInt();
            }
            skill.setUnk5(unk5, x, y);
            byte unk6 = slea.readByte();
            int x2 = 0, y2 = 0;
            if (unk6 > 0) {
                x2 = slea.readInt();
                y2 = slea.readInt();
            }
            skill.setUnk6(unk6, x2, y2);
            skills.add(skill);
        }

        return skills;
    }

    public static boolean sub_884580(int a1) {
        boolean v1; // zf

        if (a1 > 155111003) {
            v1 = a1 == 155121003;
        } else {
            if (a1 == 155111003 || a1 == 155001000) {
                return true;
            }
            v1 = a1 == 155101002;
        }
        return v1;
    }

    public static boolean sub_5B9DA0(int a1) {
        return a1 == 0x151D855 + 4 || a1 == 0x15249D5 + 1 || a1 == 155111207;
    }

    public static boolean sub_86B470(int a1) {
        return a1 == 3011004 || a1 == 3300002 || a1 == 3321003;
    }

    public static boolean is_forceAtom_attack_skill(int a1) {
        boolean v2; // zf
        boolean v3; // zf

        if (a1 > 0x3A49E07) {
            if (a1 == 0x3A4A2C1 || (a1 > 400011057 && a1 <= 400011059)) {
                return true;
            }
        } else if (a1 == 0x3A49E07 || a1 == 0x3A453CA || a1 == 0x3A477C3) {
            return true;
        }
        if (a1 > 0x226FEBC) {
            v2 = a1 == 0x22725CF;
        } else {
            if (a1 == 0x226FEBC || a1 == 0x22554ED) {
                return true;
            }
            v2 = a1 == 0x226D7AA;
        }
        if (v2
                || a1 == 4100012
                || a1 == 4120019
                || a1 == 0x217994A
                || a1 == 0x217BC7F + 2
                || sub_84ABA0(a1)
                || sub_884580(a1)
                || sub_5B9DA0(a1)
                || (a1 - 80002602) <= 0x13
                || sub_86B470(a1)) {
            return true;
        }
        if (a1 <= 0x226FEB4) {
            if (a1 == 0x226FEB4) {
                return true;
            }
            if (a1 > 0xC7E7DD + 1) {
                if (a1 > 0xD59F9D) {
                    if (a1 > 0x17EFEE9 + 1) {
                        if (a1 == 0x17F4D73) {
                            return true;
                        }
                        v3 = a1 == 0x1DC6516;
                    } else {
                        if (a1 == 0x17EFEE9 + 1 || a1 == 0x16FBC9D + 6) {
                            return true;
                        }
                        v3 = a1 == 0x1700AC0 + 2;
                    }
                } else {
                    if (a1 >= 0xD59F98 + 4) {
                        return true;
                    }
                    if (a1 > 0xC83203) {
                        if (a1 == 0xC83205 + 5) {
                            return true;
                        }
                        v3 = a1 == 0xC8361D + 1;
                    } else {
                        if (a1 == 0xC83203 || a1 == 0xC80B02 + 4) {
                            return true;
                        }
                        v3 = a1 - (0xC80B02 + 4) == 5;
                    }
                }
            } else {
                if (a1 == 0xC7E7DD + 1) {
                    return true;
                }
                if (a1 > 4210014) {
                    if (a1 > 0xB8C8CC) {
                        if (a1 == 0xB8EFCA) {
                            return true;
                        }
                        v3 = a1 == 0xC7E3FA + 1;
                    } else {
                        if (a1 == 0xB8C8CC || a1 == 0xB71B1A) {
                            return true;
                        }
                        v3 = a1 == 0xB8A1BB + 1;
                    }
                } else {
                    if (a1 == 0x403D5D + 1) {
                        return true;
                    }
                    if (a1 > 3300005) {
                        if (a1 == 3301009) {
                            return true;
                        }
                        v3 = a1 == 3321037;
                    } else {
                        if (a1 == 3300005 || a1 == 2121055 || a1 == 3100010) {
                            return true;
                        }
                        v3 = a1 == 3120017;
                    }
                }
            }
            return v3;
        }
        if (a1 <= 164120007) {
            if (a1 == 164120007) {
                return true;
            }
            if (a1 > 131003016) {
                if (a1 > 152120002) {
                    if (a1 == 155100009) {
                        return true;
                    }
                    v3 = a1 == 164101004;
                } else {
                    if (a1 >= 152120001 || a1 == 142110011) {
                        return true;
                    }
                    v3 = a1 == 152001001;
                }
            } else {
                if (a1 == 131003016) {
                    return true;
                }
                if (a1 > 80001588) {
                    if (a1 == 80001890) {
                        return true;
                    }
                    v3 = a1 == 80002811;
                } else {
                    if (a1 == 80001588)// || a1 == (_BYTE *)&aI_1[1] + 1 )
                    {
                        return true;
                    }
                    v3 = a1 == 0x3E1A70B;
                }
            }
            return v3;
        }
        if (a1 > 400031031) {
            if (a1 > 400041038) {
                if (a1 == 400041049) {
                    return true;
                }
                v3 = a1 == 400051017;
            } else {
                if (a1 == 400041038 || a1 == 400041010) {
                    return true;
                }
                v3 = a1 - 400041010 == 13;
            }
            return v3;
        }
        if (a1 == 400031031) {
            return true;
        }
        if (a1 <= 400031000) {
            if (a1 == 400031000 || a1 == 400021001) {
                return true;
            }
            v3 = a1 - 400021001 == 44;
            return v3;
        }
        if (a1 >= 400031020) {
            if (a1 <= 400031022) {
                return true;
            }
            v3 = a1 == 400031029;
            return v3;
        }
        return false;
    }

    public static boolean isHair(final int itemId) {
        return itemId / 10000 == 3 || itemId / 10000 == 4 || itemId / 10000 == 6;
    }

    public static boolean isFace(final int itemId) {
        return itemId / 10000 == 2 || itemId / 10000 == 5;
    }

    public static byte EqitemPostionById(final int itemId) {
        switch (itemId / 10000) {
            case 104://코트
            case 105://롱코트
                return -5;
            case 106://바지
                return -6;
            case 107://신발
                return -7;
            case 110:
                return -9;
            case 109:
            case 156:
                return -10;
            case 121:
            case 122:
            case 123:
            case 124:
            case 130:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 183:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 153:
            case 157:
                return -11;
        }
        return 1;
    }

    public static String 랭크(int a) {
        String name = "";
        if (a >= 10000) {
            name = "<챌린저>";
        } else if (a >= 5000) {
            name = "<마스터>";
        } else if (a >= 2500) {
            name = "<다이아몬드>";
        } else if (a >= 1000) {
            name = "<플래티넘>";
        } else if (a >= 500) {
            name = "<골드>";
        } else if (a >= 250) {
            name = "<실버>";
        } else if (a >= 50) {
            name = "<브론즈>";
        } else if (a >= 5) {
            name = "<아이언>";
        } else {
            name = "<언랭크>";
        }
        return name;
    }

    public static boolean isExpCoupon(int itemid) {
        switch (itemid) {
            case 2450064:
            case 2450124:
            case 2450163:
            case 2450042:
            case 2450130:
            case 2450164:
            case 2450155:
                return true;
        }
        return false;
    }

    public static boolean isNotParseSKill(int skillid) {
        switch (skillid) {
            case 2100010:
            case 2221012:
            case 2450042:
            case 11121014:
            case 11121004:
            case 11121052:
            case 11121055:
            case 13110022:
            case 13120003:
            case 13120010:
            case 13121054:
            case 14000028:
            case 14000029:
            case 14121004:
            case 23121011:
            case 24120055:
            case 25111206:
            case 27121201:
            case 33111013:
            case 33121016:
            case 64101002:
            case 101120200:
            case 101120205:
            case 151121041:
            case 400010010:
            case 400011124:
            case 400011132:
            case 400011048:
            case 400011049:
            case 400020002:
            case 400020051:
            case 400021041:
            case 400021049:
            case 400021050:
            case 400030002:
            case 400031025:
            case 400031026:
            case 400031027:
            case 400031031:
            case 400031035:
            case 400031036:
            case 400031045:
            case 400031046:
            case 400031048:
            case 400031049:
            case 400031050:
            case 400031056:
            case 400031058:
            case 400031059:
            case 400040006:
            case 400040008:
            case 400041002:
            case 400041003:
            case 400041004:
            case 400041005:
            case 400041006:
            case 400041007:
            case 400041010:
            case 400041018:
            case 400041020:
            case 400041021:
            case 400041027:
            case 400041031:
            case 400041037:
            case 400041038:
            case 400041041:
            case 400041042:
            case 400041043:
            case 400041053:
            case 400041055:
            case 400041056:
            case 400041058:
            case 400041059:
            case 400041061:
            case 400041062:
            case 400041079:
            case 400051018:
            case 400051019:
            case 400051020:
            case 400051025:
            case 400051026:
            case 400051073:
            case 400051081:
            case 400031064:
            case 63001000:
            case 63001001:
                return false;
        }
        return true;
    }

    public static boolean isNotCooldownSkill(int skillid) {
        switch (skillid) {
            case 400011081:
            case 400031011:
            case 400041036:
            case 400031045:
            case 400031058:
            case 400041055:
            case 400041056:
            case 400041058:
            case 400041059:
            case 400041077:
            case 400051072:
            case 400051070:
            case 400051073:
            case 400051078:
            case 400051080:
                return true;
        }
        return false;
    }

    public static boolean sub_7C6810(int a1) {
        boolean v1;
        if (a1 > 0x1DC3DF9) {
            if (a1 > 80002685) {
                if (a1 > 142111010) {
                    if (a1 > 400021061) {
                        if (a1 == 400041006 || a1 == 400041009) {
                            return true;
                        }
                        v1 = a1 - 400041009 == 10015;
                    } else {
                        if (a1 == 400021061 || a1 == 400011028 || a1 == 400011072) {
                            return true;
                        }
                        v1 = a1 - 400011072 == 19;
                    }
                } else {
                    if (a1 == 142111010) {
                        return true;
                    }
                    if (a1 > 101110102) {
                        switch (a1) {
                            case 0x7CEEAAC:
                            case 0x7CEEAB0:
                            case 0x7CEEABC:
                            case 0x7CEEABD:
                                return true;
                            default:
                                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                                    return true;
                                }
                                return false;
                        }
                    }
                    if (a1 >= 101110101 || a1 == 80002780 || a1 == 80002785) {
                        return true;
                    }
                    v1 = a1 - 80002785 == 0xE4DAC8;
                }
            } else {
                if (a1 == 80002685) {
                    return true;
                }
                if (a1 > 0x2366C1C) {
                    if (a1 > 0x3D268AA) {
                        if (a1 == 0x3E1AAEB || (a1 - 0x3E1AAEB) == 0xE31041) {
                            return true;
                        }
                        v1 = a1 - 0x3E1AAEB - 0xE31041 == 51;
                    } else {
                        if (a1 == 0x3D268AA) {
                            return true;
                        }
                        if (a1 > 0x3D093E8) {
                            if (a1 >= 0x3D093EF && a1 <= 0x3D093F0) {
                                return true;
                            }
                            if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                                return true;
                            }
                            return false;
                        }
                        if (a1 == 0x3D093E8) {
                            return true;
                        }
                        v1 = a1 == 0x393B2D0;
                    }
                } else {
                    if (a1 == 0x2366C1C) {
                        return true;
                    }
                    if (a1 > 0x217E776 + 1) {
                        if (a1 == 0x226DB88 + 1 || a1 == 0x22729A8) {
                            return true;
                        }
                        v1 = a1 == 0x2366BE8 + 3;
                    } else {
                        if (a1 == 0x217E776 + 1
                                || a1 == 0x1F962EE + 3
                                || a1 - (0x1F962EE + 3) == 105) {
                            return true;
                        }
                        v1 = a1 - (0x1F962EE + 3) == 205;
                    }
                }
            }
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        if (a1 == 0x1DC3DF9) {
            return true;
        }

        if (a1 <= 0xD7511E) {
            if (a1 == 0xD7511E) {
                return true;
            }
            if (a1 > 0x423D09 + 1) {
                if (a1 > 0xA9B19E + 1) {
                    if (a1 == 0xB8F3DD + 1 || a1 == 0xC80EEA + 2) {
                        return true;
                    }
                    v1 = a1 == 0xC835E8 + 1;
                } else {
                    if (a1 == 0xA9B19E + 1 || a1 == 0x4FAA8B + 1 || a1 == 0x510A19 + 1) {
                        return true;
                    }
                    v1 = a1 == 0xA9B199 + 3;
                }
            } else {
                if (a1 == 0x423D09 + 1) {
                    return true;
                }
                if (a1 > 2321001) {
                    if (a1 == 3101008 || a1 == 3111013) {
                        return true;
                    }
                    v1 = a1 == 3121020;
                } else {
                    if (a1 == 2321001 || a1 == 1311011 || a1 == 2221011) {
                        return true;
                    }
                    v1 = a1 - 2221011 == 41;
                }
            }
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        if (a1 > 0x17F29D5 + 8) {
            if (a1 > 0x1CA1676) {
                if (a1 == 0x1D909A6 + 2 || a1 == 0x1DA9048) {
                    return true;
                }
                v1 = a1 == 0x1DAB75A + 3;
            } else {
                if (a1 == 0x1CA1676 || a1 == 0x17F5105 + 1 || a1 == 0x19D880C + 6) {
                    return true;
                }
                v1 = a1 == 0x19DAEBC;
            }
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        if (a1 == 0x17F29D5 + 8) {
            return true;
        }

        if (a1 > 0x1524DC7 + 4) {
            if (a1 == 0x160CC66 + 2 || a1 - (0x160CC66 + 2) == 1000000) {
                return true;
            }
            v1 = a1 - (0x160CC66 + 2) == 1000005;
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        if (a1 == 0x1524DC7 + 4) {
            return true;
        }

        if (a1 <= 0x131CE0A) {
            if (a1 == 0x131CE0A) {
                return true;
            }
            v1 = a1 == 0xD77827 + 5;
            if (!v1) {
                if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        if (a1 >= 0x1424412 && a1 <= 0x1424413) {
            return true;
        }

        if (((a1 - 80001389) <= 3 && (a1 - 80001389) >= 0) || sub_7C67B0(a1)) {
            return true;
        }

        return false;
    }

    public static boolean sub_7C67B0(int a1) {
        return a1 == 80001587 || a1 == 80001629 || a1 == 80002458;
    }

    public static boolean isKainRevelationSkill(int sid) {
        switch (sid) {
            case 63001000:
            case 63101004:
            case 63001100:
            case 63101100:
            case 63101104:
            case 63111105:
            case 63121102:
            case 63121141:
            case 400031061:
            case 400031064:
                return true;
        }
        return false;
    }

    public static boolean isKainDeathSkill(int sid) {

        return false;
    }

    public static boolean isKainExpressionSkill(int sid) {
        switch (sid) {
            case 63101100:
            case 63101104:
            case 63121102:
            case 63121103:
            case 63111103:
            case 63111104:
            case 63121141:
                return true;
        }
        return false;

    }

    public static boolean isKainCanDeaathBlessingSkill(int sid) { //축복을 내리는건 몹 디버프일듯
        switch (sid) {
            case 63110103: //샤프트 브레이크 폭발인가?
            case 63120102: //폴링 더스트 충격파인가?

                return true;
        }
        return false;
    }

    public static boolean isKainDoDeaathBlessingSkill(int sid) { //축복을 실현 공로버프
        switch (sid) {
            case 63121006: //샤프트 브레이크 폭발인가?
            case 63121004: //폴링 더스트 충격파인가?

                return true;
        }
        return false;
    }

    public static final boolean isLala(final int job) {
        return job == 16001 || (job >= 16200 && job <= 16212);
    }
}
