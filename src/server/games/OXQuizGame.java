package server.games;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import handling.world.World;
import server.Timer.EventTimer;
import tools.packet.CField;
import tools.packet.SLFCGPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author SLFCG
 */
public class OXQuizGame {

    private List<MapleCharacter> chars = new ArrayList<MapleCharacter>();
    private List<MapleCharacter> deadchars = new ArrayList<MapleCharacter>();
    private List<OXQuiz> quizes = new ArrayList<OXQuiz>();
    private ScheduledFuture<?> QuizTimer = null;
    private MapleCharacter Owner = null;
    private int MessageTime = 3;

    public static boolean isRunning = false;

    public OXQuizGame(MapleCharacter owner, boolean isByAdmin) {
        isRunning = true;
        Owner = owner;
        /*        for (MapleCharacter chr : World.getAllChars()) {
         if (isByAdmin) {
         chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003302, 7000, "#face1#운영자가 OX퀴즈 참여자를 모집 중이야!", ""));
         } else {
         chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003302, 7000, "#face2##b#e" + Owner.getName() + "#k#n가 OX퀴즈 참여자를 모집 중이야!", ""));
         }
         }*/
    }

    public void sendMessage() {
        /*        for (MapleCharacter chr : World.getAllChars()) {
         if (Owner.isGM()) {
         chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003302, 5000, "#face1#운영자가 OX퀴즈 참여자를 모집 중이야!\r\n지금 #r" + Owner.getMap().getAllChracater().size() + "명#k이 대기실에 있어!", ""));
         } else {
         chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003302, 5000, "#face2##e#b" + Owner.getName() + "#k#n가 OX퀴즈 참여자를 모집 중이야!\r\n지금 #r" + Owner.getMap().getAllChracater().size() + "명#k이 대기실에 있어!", ""));
         }
         }*/
        MessageTime--;
    }

    public int getMessageTime() {
        return MessageTime;
    }

    public MapleCharacter getOwner() {
        return Owner;
    }

    public void RegisterPlayers(List<MapleCharacter> players) {
        chars = players;
    }

    public void InitGame() {
        quizes = OXQuizProvider.getQuizList2(200);
        for (MapleCharacter chr : chars) {
            chr.getClient().getSession()
                    .writeAndFlush(SLFCGPacket.OXQuizExplain("문제를 많이 맞힐수록 더 많은 보상이 기다리고 있지. 너의 상식은 어디까지?"));
        }
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (MapleCharacter chr : chars) {
                    chr.getClient().getSession()
                            .writeAndFlush(SLFCGPacket.OXQuizExplain("확실히 선택해야 해. 가운데 애매하게 걸치면 광속으로 탈락할 수 있다구!"));
                }
            }
        }, 7 * 1000);
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (MapleCharacter chr : chars) {
                    chr.getClient().getSession().writeAndFlush(
                            SLFCGPacket.OXQuizExplain("모두들 밑으로 내려가서 문제를 잘 들어봐. 맞다고 생각하면 O! 틀린 것 같다면 X!"));
                }
            }
        }, 14 * 1000);
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (MapleCharacter chr : chars) {
                    chr.getClient().getSession()
                            .writeAndFlush(SLFCGPacket.OXQuizExplain("왼쪽? 오른쪽? 숫자가 모두 카운트되기 전에 확실히 이동하라구!"));
                }
            }
        }, 21 * 1000);
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (MapleCharacter chr : chars) {
                    chr.getClient().getSession()
                            .writeAndFlush(SLFCGPacket.OXQuizExplain("그럼 지금부터 시작한다, 스릴 넘치는 OX 퀴즈!"));
                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.playSE("multiBingo/start"));
                    chr.getClient().getSession().writeAndFlush(CField.MapEff("Gstar/start"));
                }
            }
        }, 28 * 1000);

        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                StartQuiz();
            }
        }, 30 * 1000);
    }

    public void StartQuiz() {
        QuizTimer = EventTimer.getInstance().register(new Runnable() {
            int count = 0;
            int index = -1;
            OXQuiz tempq = null;

            @Override
            public void run() {
                if (chars.size() == deadchars.size()) {
                    StopQuiz();
                }
                if (count == 0) {
                    index++;
                    tempq = quizes.get(index);
                    for (MapleCharacter chr : chars) {
                        chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizQuestion(tempq.getQuestion(),
                                quizes.indexOf(tempq), quizes.size() - (quizes.indexOf(tempq) + 1)));
                        chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizCountdown(10));
                    }
                    count = 1;
                } else {
                    count = 0;
                    for (MapleCharacter chr : chars) {
                        chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizResult(tempq.isX()));
                        if (chr.getPosition().getY() >= 89 && chr.getPosition().getY() <= 576) {
                            if (chr.getPosition().getX() <= -752 && chr.getPosition().getX() >= -1450)// O
                            {
                                if (tempq.isX()) {
                                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizTelePort((byte) 10));
                                    deadchars.add(chr);
                                } else {
                                    int amount = (index + 1) * 2;
                                    chr.AddStarDustCoin(100);
                                }
                            } else if (chr.getPosition().getX() <= -500 && chr.getPosition().getX() >= -753) {
                                deadchars.add(chr);
                                chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizTelePort((byte) 10));
                            } else {
                                if (tempq.isX()) {
                                    int amount = (index + 1) * 2;
                                    chr.AddStarDustCoin(100);
                                } else {
                                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizTelePort((byte) 10));
                                    deadchars.add(chr);
                                }
                            }
                        } else {
                            if (!deadchars.contains(chr)) {
                                deadchars.add(chr);
                            }
                        }
                    }
                }
            }
        }, 11000);
    }

    public void StopQuiz() {
        QuizTimer.cancel(true);
        for (MapleCharacter chr : chars) {
            if (chr != null) {
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.playSE("multiBingo/gameover"));
                chr.setOXGame(null);
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizExplain("잠시후 이동됩니다. 맵을 이탈하지 마세요."));
            }
        }
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (MapleCharacter chr : chars) {
                    if (chr != null && chr.getMapId() == 910048100) {
                        chr.warp(910048200);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.RideVehicle, 80001013);
                    }
                }
            }
        }, 10 * 1000);
    }
}
