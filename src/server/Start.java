package server;

import client.DreamBreakerRank;
import client.SkillFactory;
import client.custom.inventory.CustomItem;
import client.inventory.MapleInventoryIdentifier;
import connector.ConnectorPanel;
import connector.ConnectorServer;
import constants.AddMesoDropData;
import constants.GameConstants;
import constants.ServerConstants;
import constants.SpecialItemConstants;
import database.DatabaseConnection;
import handling.auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.channel.handler.ChatHandler;
import handling.channel.handler.MatrixHandler;
import handling.channel.handler.UnionHandler;
import handling.farm.FarmServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.World;
import server.Timer.*;
import server.control.*;
import server.events.MapleOxQuizFactory;
import server.field.FieldSkill;
import server.field.FieldSkillFactory;
import server.field.boss.lucid.Butterfly;
import server.life.MapleLifeFactory;
import server.life.MobAttackInfoFactory;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.marriage.MarriageManager;
import server.quest.MapleQuest;
import server.quest.QuestCompleteStatus;
import tools.CMDCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import tools.ControlPannel;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);

    public void run() {
    	System.out.println("It has been improved by Dongjun....");
    	System.out.println("");
    	System.out.println("제로팩 기반으로 아래의 사항을 개선 후 반영하였습니다.");
    	System.out.println("");
    	System.out.println(" - @메소도박 명령어 기능 추가");
    	System.out.println(" - (GM 계정일 경우,)@레벨업 기능 추가 (world.properties 파일 상단 닉네임 수정하시면 됩니다.)" );
    	System.out.println(" - (GM 계정일 경우,)@공지 기능 추가 (world.properties 파일 상단 닉네임 수정하시면 됩니다.)");
    	System.out.println(" - 메소 럭키백 UI창 제거");
    	System.out.println(" - 서버UI 최소화");
    	System.out.println(" - Lv.275 이상 경험치 배율 재조정");
    	System.out.println(" - 카루타 이하 보스가 소환되지 않는 현상");
    	System.out.println(" - 위습의 원더베리 오류 수정");
    	System.out.println(" - 호영 바인드스킬 오류 개선 및 호영 여러 스킬 수정");
    	System.out.println(" - 반 레온 공중감옥 갇힘 현상 개선");
    	System.out.println(" - 잠재능력 주문서 성공/실패 여부 에러 수정");
    	System.out.println("");
    	System.out.println("Discord : 동준#0566");
    	System.out.println("");
        DatabaseConnection.init();
        if (Boolean.parseBoolean(ServerProperties.getProperty("world.admin"))) {
            ServerConstants.Use_Fixed_IV = false;
            System.out.println("[!!! Admin Only Mode Active !!!]");
        }
        System.setProperty("wz", "wz");
        try {
            Connection con = DatabaseConnection.getConnection();
            final PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0, allowed = 0, connecterClient = null");
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        }
        //  GlobalSetting();
        World.init();
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        MobTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        ShowTimer.getInstance().start();
        // String[] agrs = {};
        //  AddMesoDropData.main(agrs); << 메소 드랍 .bat
        ServerConstants.WORLD_UI = ServerProperties.getProperty("login.serverUI");
        ServerConstants.ChangeMapUI = Boolean.parseBoolean(ServerProperties.getProperty("login.ChangeMapUI"));

        //ServerConstants.quicks.add(new QuickMoveEntry(1, 2150007, 6, 10, "이동"));
        //ServerConstants.quicks.add(new QuickMoveEntry(2, 3001243, 2, 10, "편의기능"));
        //ServerConstants.quicks.add(new QuickMoveEntry(3, 3003361, 3, 10, "상점"));
        //ServerConstants.quicks.add(new QuickMoveEntry(4, 9062115, 15, 10, "컨텐츠"));
        //ServerConstants.quicks.add(new QuickMoveEntry(5, 2040014, 13, 10, "코디"));
        //ServerConstants.quicks.add(new QuickMoveEntry(6, 2007, 20, 10, "경매장"));
        //ServerConstants.quicks.add(new QuickMoveEntry(7, 9900006, 19, 10, "후원"));
        //ServerConstants.quicks.add(new QuickMoveEntry(8, 1540104, 17, 10, "홍보"));
        //ServerConstants.quicks.add(new QuickMoveEntry(9, 3003273, 8, 10, "호텔메이플"));
        //ServerConstants.quicks.add(new QuickMoveEntry(10, 1052206, 7, 10, "퀘스트"));
        //ServerConstants.quicks.add(new QuickMoveEntry(11, 9010106, 21, 10, "유니온"));
        DreamBreakerRank.LoadRank();

        AllLoding allLoding = new AllLoding();
        allLoding.start();

        System.out.println("[Loading LOGIN]");
        LoginServer.run_startup_configurations();

        System.out.println("[Loading CHANNEL]");
        ChannelServer.startChannel_Main();

        System.out.println("[Loading CASH SHOP]");
        CashShopServer.run_startup_configurations();

        System.out.println("[Loading Farm]");
        FarmServer.run_startup_configurations();

        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        PlayerNPC.loadAll();// touch - so we see database problems early...
        LoginServer.setOn(); //now or later
        InnerAbillity.getInstance().load();
        Butterfly.load();
        SpecialItemConstants.LoadGoldAppleItems();

        WorldTimer.getInstance().register(new MapleEtcControl(), 1000);
        WorldTimer.getInstance().register(new MapleMistControl(), 1000);
        WorldTimer.getInstance().register(new MapleSkillControl(), 1000);
        WorldTimer.getInstance().register(new MapleSummonControl(), 1000);
        WorldTimer.getInstance().register(new MapleMapControl(), 1000);
        WorldTimer.getInstance().register(new MapleRunOnceControl(), 10000);
        WorldTimer.getInstance().register(new MapleHotTimeControl(), 60000);

        ConnectorPanel cp = new ConnectorPanel();
        //필요없는 컴포넌트 false 처리
        cp.setVisible(false);

        ControlPannel cnp = new ControlPannel();
        cnp.setVisible(true);
        AutoGame();

        try {
            new ConnectorServer().run_startup_configurations();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

        } catch (Exception E) {

        }
    }

    private class AllLoding extends Thread {

        @Override
        public void run() {
            LoadingThread SkillLoader = new LoadingThread(new Runnable() {
                public void run() {
                    // Skill
                    SkillFactory.load();
                }
            }, "SkillLoader", this);

            LoadingThread QuestLoader = new LoadingThread(new Runnable() {
                public void run() {
                    MapleQuest.initQuests();
                    MapleLifeFactory.loadQuestCounts();
                }
            }, "QuestLoader", this);

            LoadingThread QuestCustomLoader = new LoadingThread(new Runnable() {
                public void run() {
                    MapleLifeFactory.loadNpcScripts();
                    QuestCompleteStatus.run();
                }
            }, "QuestCustomLoader", this);

            // Item
            LoadingThread ItemLoader = new LoadingThread(new Runnable() {
                public void run() {
                    MapleInventoryIdentifier.getInstance();
                    CashItemFactory.getInstance().initialize();
                    MapleItemInformationProvider.getInstance().runEtc();
                    MapleItemInformationProvider.getInstance().runItems();
                    AuctionServer.run_startup_configurations();
                }
            }, "ItemLoader", this);

            LoadingThread GuildRankingLoader = new LoadingThread(new Runnable() {
                public void run() {
                    MapleGuildRanking.getInstance().load();
                }
            }, "GuildRankingLoader", this);

            // Etc
            LoadingThread EtcLoader = new LoadingThread(new Runnable() {
                public void run() {
                    LoginInformationProvider.getInstance();
                    RandomRewards.load();
                    MapleOxQuizFactory.getInstance();
                    MapleCarnivalFactory.getInstance();
                    SpeedRunner.loadSpeedRuns();
                    UnionHandler.loadUnion();
                }
            }, "EtcLoader", this);

            LoadingThread MonsterLoader = new LoadingThread(new Runnable() {
                public void run() {
                    MobSkillFactory.getInstance();
                    MobAttackInfoFactory.getInstance();
                    FieldSkillFactory.getInstance();
                }
            }, "MonsterLoader", this);

            LoadingThread MatrixLoader = new LoadingThread(new Runnable() {
                public void run() {
                    MatrixHandler.loadCore();
                }
            }, "MatrixLoader", this);

            LoadingThread MarriageLoader = new Start.LoadingThread(new Runnable() {
                public void run() {
                    MarriageManager.getInstance();
                }
            }, "MarriageLoader", this);

            LoadingThread[] LoadingThreads = {SkillLoader, QuestLoader, QuestCustomLoader, ItemLoader, GuildRankingLoader, EtcLoader, MonsterLoader, MatrixLoader, MarriageLoader};

            for (Thread t : LoadingThreads) {
                t.start();
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            while (CompletedLoadingThreads.get() != LoadingThreads.length) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            World.Guild.load();
            GameConstants.isOpen = true;
            System.out.println("[Fully Initialized in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds]");

            if (!ServerConstants.ConnectorSetting) {
                CMDCommand.main();
            }

        }
    }

    private static class LoadingThread extends Thread {

        protected String LoadingThreadName;

        private LoadingThread(Runnable r, String t, Object o) {
            super(new NotifyingRunnable(r, o, t));
            LoadingThreadName = t;
        }

        @Override
        public synchronized void start() {
            System.out.println("[Loading...] Started " + LoadingThreadName + " Thread");
            super.start();
        }
    }

    private static class NotifyingRunnable implements Runnable {

        private String LoadingThreadName;
        private long StartTime;
        private Runnable WrappedRunnable;
        private final Object ToNotify;

        private NotifyingRunnable(Runnable r, Object o, String name) {
            WrappedRunnable = r;
            ToNotify = o;
            LoadingThreadName = name;
        }

        public void run() {
            StartTime = System.currentTimeMillis();
            WrappedRunnable.run();
            System.out.println("[Loading Completed] " + LoadingThreadName + " | Completed in " + (System.currentTimeMillis() - StartTime) + " Milliseconds. (" + (CompletedLoadingThreads.get() + 1) + "/9)");
            synchronized (ToNotify) {
                CompletedLoadingThreads.incrementAndGet();
                ToNotify.notify();
            }
        }
    }

    public static class Shutdown implements Runnable {

        @Override
        public void run() {
            ShutdownServer.getInstance().run();
        }
    }

    public static void main(final String args[]) throws InterruptedException {
        instance.run();
    }

    public static final void GlobalSetting() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SET GLOBAL max_allowed_packet = 1073741824;");
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("show variables where Variable_name = 'max_allowed_packet';");
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("* max_allowed_packet = " + rs.getInt("Value"));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SET GLOBAL max_connections = 210000000;");
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("show variables where Variable_name = 'max_connections';");
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("* max_connections = " + rs.getInt("Value"));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void AutoGame() { //Maked By 키네시스, 라피스 (네이트온: Kinesis8@nate.com , 디스코드 : 라피스#2519)
        WorldTimer tMan = WorldTimer.getInstance();
        Runnable r = new Runnable() {
            public void run() {
                if (new Date().getMinutes() % 2 == 1) {
                    //날짜함수
                    Date time = new Date();
                    String Year = (time.getYear() + 1900) + "";
                    String Month = ((time.getMonth() + 1) < 10) ? ("0" + (time.getMonth() + 1)) : ((time.getMonth() + 1) + "");
                    String Day = (time.getDate() < 10) ? ("0" + time.getDate()) : (time.getDate() + "");
                    int Today = Integer.parseInt(Year + "" + Month + "" + Day);
                    String Hour = (time.getHours() < 10) ? ("0" + time.getHours()) : (time.getHours() + "");
                    String Minute = (time.getMinutes() < 10) ? ("0" + time.getMinutes()) : (time.getMinutes() + "");
                    String HM = Hour + "" + Minute;
                    String holjjak = (Randomizer.rand(1, 2) == 1) ? "홀" : "짝";
                    String leftright = (Randomizer.rand(1, 2) == 1) ? "좌" : "우";
                    String threefour = (Randomizer.rand(1, 2) == 1) ? "3" : "4";
                    int count = 0;
                    //SQL 쿼리문 처리
                    Connection con = null;
                    PreparedStatement ps = null;
                    ResultSet rs = null;
                    try {
                        //회차 확인하기
                        con = DatabaseConnection.getConnection();
                        ps = con.prepareStatement("SELECT * FROM `bettingresult` WHERE `date` = ?");
                        ps.setInt(1, Today);
                        rs = ps.executeQuery();
                        while (rs.next()) {
                            count++;
                            if (rs.getInt("count") != count) {
                                System.out.println("오늘의 회차값 정보가 누락되거나 일치하지 않습니다. 쿼리를 확인해주세요.");
                                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(2, "[시스템] : 자동게임 시스템에 문제가 있을 수 있으니 상태를 점검해 주세요."));
                            }
                        }
                        rs.close();
                        ps.close();
                        //새로운 회차 정보 등록하기
                        count++;
                        ps = con.prepareStatement("INSERT INTO `bettingresult` (`date`, `time`, `count`, `holjjack`, `leftright`, `threefour`) VALUES (?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, Today);
                        ps.setString(2, HM);
                        ps.setInt(3, count);
                        ps.setString(4, holjjak);
                        ps.setString(5, leftright);
                        ps.setString(6, threefour);
                        ps.executeUpdate();
                        ps.close();
                        con.close();
                        //zWorld.Broadcast.broadcastMessage(CWvsContext.serverNotice(2, "[시스템] : " + count + "회 홀짝 추첨 결과는 [" + holjjak + "] 입니다."));
                        //World.Broadcast.broadcastMessage(CWvsContext.serverNotice(2, "[시스템] : " + count + "회 사다리 추첨 결과는 [" + leftright + "" + threefour + "" + holjjak + "] 입니다."));
                    } catch (Exception 오류) {
                        System.out.println("자동게임 시스템에 오류가 발생했습니다.");
                        System.out.println(오류);
                    } finally {
                        if (rs != null) {
                            try {
                                rs.close();
                            } catch (Exception e) {
                                System.out.println("자동게임 시스템 rs 커넥션 오류가 발생했습니다.");
                                System.out.println(e);
                            }
                        }
                        if (ps != null) {
                            try {
                                ps.close();
                            } catch (Exception e) {
                                System.out.println("자동게임 시스템 ps 커넥션 오류가 발생했습니다.");
                                System.out.println(e);
                            }
                        }
                        if (con != null) {
                            try {
                                con.close();
                            } catch (Exception e) {
                                System.out.println("자동게임 시스템 con 커넥션 오류가 발생했습니다.");
                                System.out.println(e);
                            }
                        }
                    }
                }
            }
        };
        tMan.register(r, 60000); //건드리지 말것
        System.out.println("[알림] 자동게임 시스템이 활성화 되었습니다.");
    }

}
