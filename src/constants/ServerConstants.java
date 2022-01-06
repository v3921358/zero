package constants;

import client.custom.inventory.CustomItem;
import server.DimentionMirrorEntry;
import server.QuickMoveEntry;
import server.ServerProperties;
import server.games.BingoGame;
import tools.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConstants {

    // IP
    public static String Gateway_IP = "175.207.0.33";

    // MAPLE VERSION
    public static final short MAPLE_VERSION = 351;
    public static final byte MAPLE_PATCH = 3;

    // DEBUG MODE
    public static boolean Use_Fixed_IV = false; // 고정하세요
    public static boolean Use_Localhost = false; // 고정하세요
    public static boolean DEBUG_RECEIVE = false;
    public static boolean DEBUG_SEND = false;
    public static int starForceSalePercent = 0;

    public static String crank1 = null;

    public static int BuddyChatPort = Integer.parseInt(ServerProperties.getProperty("ports.buddy"));

    //EXP RATE
    public static int EventBonusExp = Integer.parseInt(ServerProperties.getProperty("world.eventBonus"));
    public static int WeddingExp = Integer.parseInt(ServerProperties.getProperty("world.weddingBonus"));
    public static int PartyExp = Integer.parseInt(ServerProperties.getProperty("world.partyBonus"));
    public static int PcRoomExp = Integer.parseInt(ServerProperties.getProperty("world.pcBonus"));
    public static int RainbowWeekExp = Integer.parseInt(ServerProperties.getProperty("world.rainbowBonus"));
    public static int BoomupExp = Integer.parseInt(ServerProperties.getProperty("world.boomBonus"));
    public static int PortionExp = Integer.parseInt(ServerProperties.getProperty("world.portionBonus"));
    public static int RestExp = Integer.parseInt(ServerProperties.getProperty("world.restBonus"));
    public static int ItemExp = Integer.parseInt(ServerProperties.getProperty("world.itemBonus"));
    public static int ValueExp = Integer.parseInt(ServerProperties.getProperty("world.valueBonus"));
    public static int IceExp = Integer.parseInt(ServerProperties.getProperty("world.iceBonus"));
    public static int HpLiskExp = Integer.parseInt(ServerProperties.getProperty("world.hpLiskBonus"));
    public static int FieldBonusExp = Integer.parseInt(ServerProperties.getProperty("world.fieldBonus"));
    public static int EventBonusExp2 = Integer.parseInt(ServerProperties.getProperty("world.eventBonus2"));
    public static int FieldBonusExp2 = Integer.parseInt(ServerProperties.getProperty("world.fieldBonus2"));

    public static final byte check = 1;

    public static int WarpMap = 100000000;
    public static int StartMap = 924050001; //시작맵
    public static int MainTown = 100000000;
    public static int csNpc = 9001174; // disable = 0;
    public static int JuhunFever = 0;
    public static String WORLD_UI = "UI/UIWindowEvent.img/sundayMaple";
    public static String SUNDAY_TEXT = "#sunday# #fn나눔고딕 ExtraBold##fs20##fc0xFFFFFFFF#경험치 3배 쿠폰(15분) #fc0xFFFFD800#5개 #fc0xFFFFFFFF#지급!\\n#sunday# #fs20##fc0xFFFFFFFF#RISE 포인트 획득 가능량 #fc0xFFFFD800#2배!#fc0xFFFFFFFF#";
    public static String SUNDAY_DATE = "#fn나눔고딕 ExtraBold##fs15##fc0xFFB7EC00#2019년 12월 22일 일요일";
    public static String serverMessage = "";
    public static boolean ChangeMapUI = false;
    public static boolean feverTime = false;
    public static int ReqDailyLevel = 33;
    public static List<BingoGame> BingoGameHolder = new ArrayList<>();
    public static List<QuickMoveEntry> quicks = new ArrayList<>();
    public static List<DimentionMirrorEntry> mirrors = new ArrayList<>();
    public static String SundayMapleUI = "UI/UIWindowEvent.img/sundayMaple";
    public static String SundayMapleTEXTLINE_1 = "#sunday# #fn나눔고딕 ExtraBold#"
            + "#fs20##fc0xFFFFFFFF#"
            + "#fc0xFFFFD800#"
            + "#fc0xFFFFFFFF#\\r\\n#sunday# #fn나눔고딕 ExtraBold#"
            + "#fs20##fc0xFFFFFFFF#"
            + "#fc0xFFFFD800#"
            + "#fc0xFFFFFFFF#";
    public static String SundayMapleTEXTLINE_2 = "#fn나눔고딕 ExtraBold#"
            + "#fs15##fc0xFFB7EC00#";

    /*커넥터 설정*/
    public static final Map<String, Triple<String, String, String>> authlist = new ConcurrentHashMap<>();//모두포함
    public static final Map<String, Triple<String, String, String>> authlist2 = new ConcurrentHashMap<>();//모두포함
    public static boolean ConnectorSetting = Boolean.parseBoolean(ServerProperties.getProperty("world.useConnecter")); //커넥터 사용여부
    public static boolean isTestServer = false; // 테스터 지원 상자 지급 여부
    public static boolean ConnecterLog = false;

    public static int MaxNeoCore = 5000;

    public static enum PlayerGMRank {

        NORMAL('@', 0),
        DONATOR('#', 1),
        SUPERDONATOR('$', 2),
        INTERN('%', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        ADMIN('!', 6);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
}
