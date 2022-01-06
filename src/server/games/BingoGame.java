package server.games;

import client.MapleCharacter;
import constants.GameConstants;
import handling.world.World;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer;
import server.maps.MapleMap;
import tools.Pair;
import tools.packet.SLFCGPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class BingoGame {

    private Map<MapleCharacter, int[][]> players = new HashMap<>();
    private List<MapleCharacter> rank = new ArrayList<>();
    private List<Integer> hostednumbers = new ArrayList<>();
    private ScheduledFuture<?> BingoTimer = null;
    private MapleCharacter Owner = null;
    private int round = 1, MessageTime = 3;
    public static int point = 0;
    public static List<Pair<Integer, Integer>> items = new ArrayList<>();

    public static boolean isRunning = false;

    public BingoGame(MapleCharacter owner, boolean isByAdmin) {
        isRunning = true;
        Owner = owner;
        String channel = owner.getClient().getChannel() == 1 ? "1" : owner.getClient().getChannel() == 2 ? "20세 이상" : String.valueOf(owner.getClient().getChannel() - 1);
        /*        for (MapleCharacter chr : World.getAllChars()) {
         if (isByAdmin) {
         chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003301, 7000, "#face3#운영자가 빙고 게임 참여자를 모집 중이야! " + channel + "채널에서 @빙고입장을 통해 입장해봐~", ""));
         } else {
         chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003301, 7000, "#face2##b#e" + Owner.getName() + "#k#n가 빙고 게임 참여자를 모집 중이야! " + channel + "채널에서 @빙고입장을 통해 입장해봐~", ""));
         }
         }*/
    }

    public int getMessageTime() {
        return MessageTime;
    }

    public void addRank(MapleCharacter a1) {
        if (!rank.contains(a1)) {
            rank.add(a1);
            a1.getMap().broadcastMessage(SLFCGPacket.BingoAddRank(a1));
        }
    }

    public MapleCharacter getOwner() {
        return Owner;
    }

    public int[][] getTable(MapleCharacter a1) {
        return players.get(a1);
    }

    public void setTable(MapleCharacter a1, int[][] a2) {
        players.replace(a1, players.get(a1), a2);
    }

    public List<MapleCharacter> getRanking() {
        return rank;
    }

    public void StartGame() {
        for (MapleCharacter chr : players.keySet()) {
            chr.cancelAllBuffs();
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.BingoUI(3, round));
        }
        if (getBingoTimer() != null) {
            getBingoTimer().cancel(false);
        }
        setBingoTimer(Timer.EventTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                int temp = players.size();
                for (MapleCharacter chr : players.keySet()) {
                    if (chr == null) {
                        temp--;
                    }
                }
                if (temp <= 0) {
                    StopBingo();
                    return;
                }
                if (hostednumbers.size() == 75 || rank.size() == 30 || rank.size() >= players.size()) {
                    StopBingo();
                    return;
                }
                while (true) {
                    int number = Randomizer.rand(1, 76);
                    if (!hostednumbers.contains(number) && number <= 75) {
                        hostednumbers.add(number);
                        for (MapleCharacter chr : players.keySet()) {
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.BingoHostNumberReady());
                            chr.dropMessageGM(1, "test");
                            chr.getClient().getSession()
                                    .writeAndFlush(SLFCGPacket.BingoHostNumber(1, number, 75 - hostednumbers.size()));
                            if ((75 - hostednumbers.size()) % 5 == 0) {
                                chr.getClient().getSession()
                                        .writeAndFlush(SLFCGPacket.BingoHostNumber(0, number, 75 - hostednumbers.size()));
                            }
                        }
                        break;
                    }
                }
            }
        }, 5000));
    }

    public void InitGame(List<MapleCharacter> a1) {
        for (MapleCharacter chr : a1) {
            int[][] table = new int[5][5];
            List<Integer> temp = new ArrayList<Integer>();

            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    while (true) {
                        int number = Randomizer.rand((x * 15) + 1, (x + 1) * 15);
                        if (!temp.contains(number) && number <= (x + 1) * 15) {
                            temp.add(number);
                            table[x][y] = number;
                            break;
                        }
                    }
                }
            }
            table[2][2] = 0;// joker
            players.put(chr, table);
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.BingoInit(table));
        }
    }

    public void StopBingo() {
        getBingoTimer().cancel(true);
        for (MapleCharacter chr : players.keySet()) {
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.playSE("multiBingo/gameover"));
            if (chr != null) {
                MapleMap target = chr.getClient().getChannelServer().getMapFactory().getMap(922290200);
                chr.changeMap(target, target.getPortal(0));
                chr.setBingoGame(null);
                if (!rank.contains(chr)) {
                    for (Pair<Integer, Integer> item : items) {
                        if (chr.getInventory(GameConstants.getInventoryType(item.left)).getNumFreeSlot() > 0) {
                            chr.gainItem(item.left, item.right);
                        } else {
                            chr.dropMessage(-7, "인벤토리 공간이 부족하여 " + MapleItemInformationProvider.getInstance().getName(item.left) + "아이템 " + item.right + "개를 지급받지 못했습니다. 이 메세지를 캡쳐하여 GM에게 문의해주세요.");
                        }
                    }
                }
            }
        }

        for (MapleCharacter chr : rank) {
            if (chr != null) {
                final int ranknumber = rank.indexOf(chr) + 1;
                chr.dropMessage(5, "빙고 " + ranknumber + "등 보상 및 전체 보상이 지급되었습니다. 인벤토리 및 포인트를 확인해주세요.");
                if (ranknumber == 1) {
                    chr.AddStarDustCoin(point * 10);
                } else if (ranknumber >= 2 && ranknumber <= 10) {
                    chr.AddStarDustCoin(point * 5);
                } else if (ranknumber >= 11 && ranknumber <= 20) {
                    chr.AddStarDustCoin(point * 3);
                } else {
                    chr.AddStarDustCoin(point);
                }
            }
        }

        isRunning = false;
    }

    public ScheduledFuture<?> getBingoTimer() {
        return BingoTimer;
    }

    public void setBingoTimer(ScheduledFuture<?> bingoTimer) {
        BingoTimer = bingoTimer;
    }
}
