package server.games;

import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import handling.world.World;
import server.Randomizer;
import server.Timer;
import tools.packet.CField;
import tools.packet.SLFCGPacket;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * @author SLFCG
 */
public class DetectiveGame {

    private int Stage = 1, MessageTime = 3;
    private List<MapleCharacter> Rank = new ArrayList<>();
    private List<MapleCharacter> Fail = new ArrayList<>();
    private Map<MapleCharacter, Integer> Players = new HashMap<>();
    private Map<MapleCharacter, Integer> Answers = new HashMap<>();
    private ScheduledFuture<?> DetectiveTimer = null;
    private MapleCharacter Owner = null;

    public static boolean isRunning = false;

    public DetectiveGame(MapleCharacter owner, boolean isByAdmin) {
        isRunning = true;
        Owner = owner;
        World.getAllChars().stream().forEach((chr) -> {
            if (isByAdmin) {
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003501, 7000, "#face0#운영자가 암호 추리 게임 참여자를 모집 중이야", ""));
            } else {
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003156, 7000, "#face0##b#e" + Owner.getName() + "#k#n가 암호 추리 게임 참여자를 모집 중이야!", ""));
            }
        });
    }

    public void sendMessage() {
        World.getAllChars().stream().forEach((chr) -> {
            if (Owner.isGM()) {
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003501, 5000, "#face0#운영자가 암호 추리 게임 참여자를 모집 중이야!\r\n지금 #r" + Owner.getMap().getAllChracater().size() + "명#k이 대기실에 있어!", ""));
            } else {
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003156, 5000, "#face0##e#b" + Owner.getName() + "#k#n가 암호 추리 게임 참여자를 모집 중이야!\r\n지금 #r" + Owner.getMap().getAllChracater().size() + "명#k이 대기실에 있어!", ""));
            }
        });
        MessageTime--;
    }

    public void RegisterPlayers(List<MapleCharacter> chars) {
        for (MapleCharacter chr : chars) {
            Players.put(chr, 1);
        }
    }

    public void addAttempt(MapleCharacter chr) {
        Players.put(chr, Players.get(chr) + 1);
        int temp = Players.get(chr);
        if (Players.get(chr) == 15) {
            Fail.add(chr);
        }
    }

    public int getAnswer(MapleCharacter chr) {
        return Answers.get(chr);
    }

    public List<MapleCharacter> getRanking() {
        return Rank;
    }

    public MapleCharacter getOwner() {
        return Owner;
    }

    public Set<MapleCharacter> getPlayers() {
        return Players.keySet();
    }

    public void addRank(MapleCharacter a1) {
        if (!Rank.contains(a1)) {
            Rank.add(a1);
            a1.getMap().broadcastMessage(SLFCGPacket.HundredDetectiveGameAddRank(a1.getId(), a1.getName()));
            if (Rank.size() == 30 || Rank.size() + Fail.size() == Players.size()) {
                DetectiveTimer.cancel(true);
                for (MapleCharacter chr : Players.keySet()) {
                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(4, Stage));
                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveReEnable(16));
                    Players.put(chr, 1);
                }
                if (Stage == 3) {
                    StopGame();
                } else {
                    Stage++;
                    Answers.clear();
                    Rank.clear();
                    Fail.clear();
                    for (MapleCharacter chr : Players.keySet()) {
                        int Answer = GetRandomNumber();
                        Answers.put(chr, Answer);
                    }
                    Timer.EventTimer.getInstance().schedule(() -> {
                        for (MapleCharacter chr : Players.keySet()) {
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameReady(Stage));
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(2, Stage));
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(3, Stage));
                        }
                        Timer.EventTimer.getInstance().schedule(() -> {
                            for (MapleCharacter chr : Players.keySet()) {
                                chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveReEnable(1));
                                chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(6, Stage));
                            }
                            DetectiveTimer = Timer.EventTimer.getInstance().register(() -> {
                                for (MapleCharacter chr : Players.keySet()) {
                                    if (Players.get(chr) > 1 && Players.get(chr) <= 15) {
                                        chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveReEnable(Players.get(chr)));
                                    }
                                }
                            }, 10 * 1000);
                        }, 5 * 1000);
                    }, 5 * 1000);
                    for (MapleCharacter chr : Rank) {
                        if (chr != null) {
                            final int ranknumber = Rank.indexOf(chr) + 1;
                            if (ranknumber == 1) {
                                if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 10) {
                                    chr.AddStarDustCoin(100);
                                }
                            } else if (ranknumber >= 2 && ranknumber <= 10) {
                                if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 5) {
                                    chr.AddStarDustCoin(20);
                                }
                            } else if (ranknumber >= 11 && ranknumber <= 20) {
                                if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 4) {
                                    chr.AddStarDustCoin(10);
                                }
                            } else {
                                if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 3) {
                                    chr.AddStarDustCoin(5);
                                }
                            }
                        }
                    }
                    for (MapleCharacter chr : Players.keySet()) {
                        if (chr != null) {
                            if (!Rank.contains(chr)) {
                                if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() > 0) {
                                    chr.AddStarDustCoin(2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int GetRandomNumber() {
        List<Integer> temp = new ArrayList<>();
        while (temp.size() != 3) {
            int a = 0;
            while (a == 0) {
                a = Randomizer.nextInt(10);
            }
            if (!temp.contains(a)) {
                temp.add(a);
            }
        }
        int num = temp.get(0) * 100 + temp.get(1) * 10 + temp.get(2);
        while (Answers.containsValue(num)) {
            num = GetRandomNumber();
        }
        return num;
    }

    public void StartGame() {

        for (MapleCharacter chr : Players.keySet()) {
            chr.getClient().getSession().writeAndFlush(CField.musicChange("BgmEvent2/adventureIsland"));
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameExplain());
            int Answer = GetRandomNumber();
            Answers.put(chr, Answer);
        }
        Timer.EventTimer.getInstance().schedule(() -> {
            for (MapleCharacter chr : Players.keySet()) {
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameReady(Stage));
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(2, Stage));
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(3, Stage));
            }
            Timer.EventTimer.getInstance().schedule(() -> {
                for (MapleCharacter chr : Players.keySet()) {
                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameControl(6, Stage));
                }
                DetectiveTimer = Timer.EventTimer.getInstance().register(() -> {
                    for (MapleCharacter chr : Players.keySet()) {
                        if (Players.get(chr) > 1 && Players.get(chr) <= 15) {
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.HundredDetectiveReEnable(Players.get(chr)));
                        }
                    }
                }, 10 * 1000);
            }, 5 * 1000);
        }, 40 * 1000);
    }

    public void StopGame() {
        Timer.EventTimer.getInstance().schedule(() -> {
            for (MapleCharacter chr : Players.keySet()) {
                if (chr != null) {
                    chr.warp(993022200);
                    chr.setDetectiveGame(null);
                }
            }
            isRunning = false;
        }, 5 * 1000);
    }
}
