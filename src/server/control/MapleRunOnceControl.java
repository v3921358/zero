package server.control;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.AuctionHistory;
import client.inventory.AuctionItem;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.auction.AuctionServer;
import handling.auction.handler.AuctionHistoryIdentifier;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.guild.MapleGuild;
import tools.CurrentTime;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class MapleRunOnceControl implements Runnable {

    private long lastSaveAuctionTime = 0;
    public static int date, count, month;

    public static Map<String, Integer> ids = new HashMap<>();
    //일간 초기화 할 키벨류 추가
    public static Map<String, String> todayKeyValues = new HashMap<>();

    //주간 초기화 할 키벨류 추가
    public static String[][] weekKeyValues = {{"db_lastweek", "0"}, {"dojo", "0"}, {"dojo_time", "0"}};

    //일간 초기화 계정 키밸류
    public static String[] clientDateKeyValues = {"dailyGiftComplete", "hotelMapleToday", "ht", "mPark", "jump_1", "jump_2", "jump_3", "goldT", "goldComplete", "day_reborn_1", "day_reborn_2", "day_qitem", "day_summer_a", "day_summer_e"};

    //경매장 id
    public static List<Integer> updateAuctionClients = new ArrayList<>();

    static {
        todayKeyValues.put("muto", "0");
        todayKeyValues.put("arcane_quest_2", "-1");
        todayKeyValues.put("arcane_quest_3", "-1");
        todayKeyValues.put("arcane_quest_4", "-1");
        todayKeyValues.put("arcane_quest_5", "-1");
        todayKeyValues.put("arcane_quest_6", "-1");
        todayKeyValues.put("arcane_quest_7", "-1");
        todayKeyValues.put("NettPyramid", "0");
        todayKeyValues.put("linkMobCount", "0");
    }

    public MapleRunOnceControl() {
        date = CurrentTime.요일();
        month = CurrentTime.월();
        count = 0;
        this.lastSaveAuctionTime = System.currentTimeMillis();
        System.out.println("[Loading Completed] Maple RunOnceControl Start");

        //addBoss Clear
        for (String event : ChannelServer.getInstance(1).getEventSM().getEvents().keySet()) {
            todayKeyValues.put(event, "0");
        }
    }

    public static void runningAuctionItems(long time) {
        updateAuctionClients.clear();
        Iterator<AuctionItem> items = AuctionServer.getItems().values().iterator();
        while (items.hasNext()) {
            AuctionItem aItem = items.next();
            if (aItem.getEndDate() < time && aItem.getState() == 0) {
                aItem.setState(4);
                aItem.setPrice(0);
                aItem.setSecondPrice(-1);

                AuctionHistory history = new AuctionHistory();

                history.setAuctionId(aItem.getAuctionId());
                history.setAccountId(aItem.getAccountId());
                history.setCharacterId(aItem.getCharacterId());
                history.setItemId(aItem.getItem().getItemId());
                history.setState(aItem.getState());
                history.setPrice(aItem.getPrice());
                history.setBuyTime(System.currentTimeMillis());
                history.setDeposit(aItem.getDeposit());
                history.setQuantity(aItem.getItem().getQuantity());
                history.setWorldId(aItem.getWorldId());
                history.setId(AuctionHistoryIdentifier.getInstance());
                aItem.setHistory(history);

                updateAuctionClients.add(aItem.getAccountId());
            }
        }

    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        runningAuctionItems(time);
        if (time - lastSaveAuctionTime >= 15 * 60 * 1000) {
            count++;
            AuctionServer.saveItems();
            System.out.println("[알림] 서버 오픈 이후 " + (count * 15 / 60) + "시간 " + (count * 15 % 60) + "분 경과하였습니다.");
            lastSaveAuctionTime = time;
        }

        Iterator<ChannelServer> channels = ChannelServer.getAllInstances().iterator();

        int dd = CurrentTime.요일();
        int mon = CurrentTime.월();
        if (date != dd) {
            date = dd;
            ids.clear();

            reset(channels);

            month = mon;
        }

        while (channels.hasNext()) {
            ChannelServer cs = channels.next();
            Iterator<MapleCharacter> chrs = cs.getPlayerStorage().getAllCharacters().values().iterator();
            while (chrs.hasNext()) {
                MapleCharacter chr = chrs.next();

                //첫 접속 시 reset 키가 없음, 체크 이후 초기화 대상에 추가
                if (chr.getV("reset") == null && !ids.containsKey(chr.getName())) {
                    ids.put(chr.getName(), chr.getId());
                }

                if (ids.containsKey(chr.getName())) {
                    chr.dropMessage(6, "일일 퀘스트 횟수가 초기화 되었습니다.");
                    chr.addKV("reset", "1");
                    chr.setKeyValue(16700, "count", "0");
                    chr.setKeyValue(16700, "date", String.valueOf(GameConstants.getCurrentDate_NoTime()));

                    //chr.dropMessage(6, "일간 네오코어 획득량이 초기화 되었습니다.");
                    chr.setKeyValue(501215, "today", 0 + "");

                    // chr.dropMessage(6, "도전 미션!이 초기화 되었습니다.");
                    for (int i = 1; i <= 10; i++) {
                        chr.setKeyValue(20220311, "ove" + i, 0 + "");
                        chr.setKeyValue(20220311, "ove" + i + "_clear", 0 + "");
                    }

                    for (Entry<String, String> keyValue : todayKeyValues.entrySet()) {
                        chr.addKV(keyValue.getKey(), keyValue.getValue());
                    }

                    if (chr.getV("resetWeek") == null) {
                        //chr.dropMessage(6, "주간 미션이 초기화 되었습니다.");
                        chr.addKV("resetWeek", "1");
                        for (String[] keyValue : weekKeyValues) {
                            if (keyValue[0].equals("dojo") && chr.getV("dojo") != null) {
                                chr.addDojoCoin(Integer.parseInt(chr.getV("dojo")) * 100);
                                // chr.dropMessage(-8, "무릉도장 포인트가 정산되었습니다.");
                            }
                            chr.addKV(keyValue[0], keyValue[1]);
                        }
                    }

                    ids.remove(chr.getName());
                }

//                if (updateAuctionClients.contains(chr.getClient().getAccID())) {
//                    chr.getClient().getSession().writeAndFlush(CWvsContext.AlarmAuction(chr, item));
//                    updateAuctionClients.remove(chr.getClient().getAccID());
//                }
            }
        }
    }

    public static void reset(Iterator<ChannelServer> channels) {

        System.out.println("요일이 바뀌어 데이터를 초기화합니다.");
        try {

            //모든 캐릭터 ID 담음
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.put(rs.getString("name"), rs.getInt("id"));
            }

            ps.close();
            rs.close();

            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //12시 지나면 미접속중인 모든 캐릭터의 reset값 삭제
        //리셋용 키벨류 초기화
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("DELETE FROM keyvalue WHERE `key` = ?");
            ps.setString(1, "reset");
            ps.executeUpdate();
            ps.close();

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //리셋용 계정 키벨류 초기화
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("DELETE FROM acckeyvalue WHERE `key` = ?");
            for (String key : clientDateKeyValues) {
                ps.setString(1, key);
                ps.execute();
            }

            if (CurrentTime.일() == 1) {
                ps.setString(1, "dailyGiftDay");
                ps.execute();
            }
            ps.close();

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //리셋옹 주간 키벨류 초기화
        if (date == 1) {
            try {
                Connection con = DatabaseConnection.getConnection();

                PreparedStatement ps = con.prepareStatement("DELETE FROM keyvalue WHERE `key` = ?");
                ps.setString(1, "resetWeek");
                ps.executeUpdate();
                ps.close();

                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (MapleGuild guild : World.Guild.getGuilds()) {
                guild.setNoblessSkillPoint(Math.max((int) (guild.getGuildScore() / 100), 60)); // 100점당 1포인트
                guild.setGuildScore(0);
                if (guild.removeSkill(91001022, "") && guild.removeSkill(91001023, "") && guild.removeSkill(91001024, "") && guild.removeSkill(91001025, "")) {
                    guild.broadcast(CField.getGameMessage(5, "[" + LoginServer.getServerName() + "] : 지난 주 지하수로 점수를 통해 노블레스 " + guild.getNoblessSkillPoint() + "포인트가 지급되었습니다."));
                }
            }
        }

        List<MapleClient> clients = new ArrayList<>();
        List<MapleClient> clients2 = new ArrayList<>();
        List<MapleClient> clients3 = new ArrayList<>();

        //12시 지나면 접속중인 모든 캐릭터의 reset값 삭제
        while (channels.hasNext()) {
            ChannelServer cs = channels.next();
            Iterator<MapleCharacter> chrs = cs.getPlayerStorage().getAllCharacters().values().iterator();
            while (chrs.hasNext()) {
                MapleCharacter chr = chrs.next();
                if (chr.getV("reset") != null) {
                    chr.removeV("reset");
                    if (date == 1 && chr.getV("resetWeek") != null) {
                        chr.removeV("resetWeek");
                    }
                }
                if (!clients.contains(chr.getClient())) {
                    clients.add(chr.getClient());
                }
            }
        }

        Iterator<MapleCharacter> chrs = AuctionServer.getPlayerStorage().getAllCharacters().values().iterator();

        while (chrs.hasNext()) {
            MapleCharacter chr = chrs.next();
            if (chr.getV("reset") != null) {
                chr.removeV("reset");
                if (date == 1 && chr.getV("resetWeek") != null) {
                    chr.removeV("resetWeek");
                }
            }
            if (!clients.contains(chr.getClient())) {
                clients.add(chr.getClient());
            }
        }

        for (MapleClient client : clients) {

            if (CurrentTime.일() == 1) {
                client.setKeyValue("dailyGiftDay", "0");
                client.getSession().writeAndFlush(CField.getGameMessage(8, "1일이 되어 데일리 기프트가 초기화됩니다."));
            }

            for (String keyValue : clientDateKeyValues) {
                client.setKeyValue(keyValue, "0");

                if (keyValue.equals("dailyGiftComplete")) {
                    clients2.add(client);
                }

                if (keyValue.equals("goldComplete")) {
                    clients3.add(client);
                }
            }
        }

        for (MapleClient client : clients2) {
            if (client != null && client.getPlayer() != null) {
                try {
                    client.getSession().writeAndFlush(CWvsContext.updateDailyGift("count=0;day=" + client.getKeyValue("dailyGiftDay") + ";date=" + GameConstants.getCurrentDate_NoTime()));
                    client.getSession().writeAndFlush(CField.dailyGift(client.getPlayer(), 1, 0));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    client.getSession().writeAndFlush(CField.getGameMessage(7, "데일리 기프트가 초기화 되었습니다."));
                    client.getSession().writeAndFlush(CField.getGameMessage(7, "날짜 초기화 시 채널 변경 후에 이용해주세요."));
                }
            }
        }

        for (MapleClient client : clients3) {
            if (client != null) {
                try {
                    client.setKeyValue("goldCount", "0");
                    client.setKeyValue("goldT", GameConstants.getCurrentFullDate());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    //  client.getSession().writeAndFlush(CField.getGameMessage(7, "황금마차 시간 기록이 시작되었습니다."));
                    client.getSession().writeAndFlush(CField.getGameMessage(7, "시간 초기화 시 채널 변경 후에 이용해주세요."));
                }
            }
        }
    }
}
