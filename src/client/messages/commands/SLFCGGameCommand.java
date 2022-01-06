package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import handling.world.World;
import server.MapleItemInformationProvider;
import server.Timer.EventTimer;
import server.games.BingoGame;
import server.games.DetectiveGame;
import server.games.OXQuizGame;
import tools.Pair;
import tools.packet.CField;
import tools.packet.SLFCGPacket;

import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author SLFCG
 */
public class SLFCGGameCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.SUPERGM;
    }

    public static class SetBingoStep extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                switch (splitted[1]) {
                    case "0": {
                        BingoGame bingo = new BingoGame(c.getPlayer(), true);
                        c.getPlayer().setBingoGame(bingo);
                        c.getPlayer().getMap().setBingoGame(true);

                        if (BingoGame.point == 0) {
                            c.getPlayer().dropMessage(5, "[Warning] 지급하는 포인트의 값이 0입니다.");
                        }

                        if (BingoGame.items.isEmpty()) {
                            c.getPlayer().dropMessage(5, "[Warning] 지급하는 아이템의 데이터가 없습니다.");
                        }
                    }
                    break;
                    case "1": {
                        if (c.getPlayer().getMapId() != 922290000) { //빙고 대기맵
                            c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(1052230, 3000, "#face1#빙고 대기맵에서 실행해 주세요", ""));
                            return 0;
                        }

                        if (c.getPlayer().getMap().getCharacters().size() < 1) {
                            c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(1052230, 3000, "#face1#빙고 최소 등록 인원은 30명 입니다", ""));
                            return 0;
                        }

                        BingoGame bingo = c.getPlayer().getBingoGame();

                        if (bingo == null) {
                            c.getPlayer().dropMessage(6, "빙고 데이터가 없습니다.");
                            return 0;
                        }

                        c.getPlayer().getMap().setBingoGame(false);

                        for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                            if (chr != null) {
                                chr.warp(922290100);
                            }
                        }

                        EventTimer.getInstance().schedule(() -> {
                            if (c.getPlayer().getBingoGame() == null) {
                                return;
                            }
                            if (c.getPlayer().getMapId() == 922290100) {
                                bingo.InitGame(c.getPlayer().getMap().getAllChracater());
                                for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                    if (chr != null) {
                                        chr.setBingoGame(bingo);
                                        chr.getClient().getSession()
                                                .writeAndFlush(CField.musicChange("BgmEvent/dolphin_night"));
                                        chr.getClient().getSession()
                                                .writeAndFlush(SLFCGPacket.playSE("multiBingo/start"));
                                        chr.getClient().getSession()
                                                .writeAndFlush(CField.MapEff("Gstar/start"));
                                    }
                                }
                            }
                        }, 10 * 1000);

                        EventTimer.getInstance().schedule(() -> {
                            if (c.getPlayer().getBingoGame() == null) {
                                return;
                            }
                            c.getPlayer().getBingoGame().StartGame();
                        }, 12 * 1000);

                    }
                    break;
                    case "2": { //Force Stop
                        c.getPlayer().getBingoGame().StopBingo();
                    }
                    break;
                    case "3": { //빙고 보상 설정
                        if (splitted.length == 2) {
                            c.getPlayer().dropMessage(-7, "현재 등록된 아이템 목록입니다. 모든 유저에게 일괄적으로 지급됩니다.");
                            for (Pair<Integer, Integer> item : BingoGame.items) {
                                c.getPlayer().dropMessage(6, MapleItemInformationProvider.getInstance().getName(item.left) + "아이템 " + item.right + "개");
                            }
                            return 0;
                        }
                        BingoGame.items.add(new Pair<>(Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3])));
                        c.getPlayer().dropMessage(-7, MapleItemInformationProvider.getInstance().getName(Integer.parseInt(splitted[2])) + "아이템 " + splitted[3] + "개가 아이템 리스트에 등록되었습니다.");
                    }
                    break;
                    case "4": { // 포인트 보상 설정
                        if (splitted.length == 2) {
                            c.getPlayer().dropMessage(6, "현재 설정된 지급 포인트 기본 값 : " + BingoGame.point);
                            return 0;
                        }
                        BingoGame.point = Integer.parseInt(splitted[2]);
                        c.getPlayer().dropMessage(-7, "지급보상 포인트가 " + BingoGame.point + "로 설정되었습니다.");
                        c.getPlayer().dropMessage(6, "1등 보상 : " + BingoGame.point * 10 + " (10배)");
                        c.getPlayer().dropMessage(6, "2등 ~ 10등 보상 : " + BingoGame.point * 5 + " (5배)");
                        c.getPlayer().dropMessage(6, "11등 ~ 20등 보상 : " + BingoGame.point * 3 + " (3배)");
                        c.getPlayer().dropMessage(6, "21등 ~ 30등 보상 : " + BingoGame.point + " (1배)");
                    }
                    break;
                    default: {
                        c.getPlayer().dropMessage(5, "0 : 게임 참여 인원 받기");
                        c.getPlayer().dropMessage(5, "1 : 게임 참여 인원 받기 종료 및 게임 시작");
                        c.getPlayer().dropMessage(5, "2 : 게임 강제 종료");
                        c.getPlayer().dropMessage(5, "3 : 아이템 보상 설정");
                        c.getPlayer().dropMessage(5, "4 : 포인트 보상 설정");
                    }
                    break;
                }
            }
            return 0;
        }
    }

    public static class SetOXStep extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                switch (splitted[1]) {
                    case "0":// Initialize Game
                    {
                        if (c.getPlayer().getMapId() != 910048000) {
                            return 0;
                        }
                        if (c.getPlayer().getMap().getCharacters().size() < 1) {
                            c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(1052230, 3000, "#face1#OX 최소 등록 인원 30명", ""));
                            return 0;
                        }
                        OXQuizGame ox = new OXQuizGame(c.getPlayer(), true);
                        c.getPlayer().setOXGame(ox);
                        c.getPlayer().getMap().broadcastMessage(CField.getClock(30));
                        EventTimer.getInstance().schedule(new Runnable() {
                            @Override
                            public void run() {
                                if (c.getPlayer().getOXGame() == null) {
                                    return;
                                }
                                ox.RegisterPlayers(c.getPlayer().getMap().getAllCharactersThreadsafe());
                                for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                    if (chr != null) {
                                        chr.warp(910048100);
                                        chr.applySkill(80001013, 1);
                                        chr.getClient().getSession().writeAndFlush(SLFCGPacket.OXQuizTelePort((byte) 1));
                                    }
                                }
                                if (c.getPlayer().getMapId() == 910048100) {
                                    for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                        if (chr != null) {
                                            chr.setOXGame(ox);
                                        }
                                    }
                                }
                            }
                        }, 30 * 1000);
                        EventTimer.getInstance().schedule(new Runnable() {
                            @Override
                            public void run() {
                                if (c.getPlayer().getOXGame() == null) {
                                    return;
                                }
                                for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                    if (chr != null) {
                                        chr.getClient().getSession()
                                                .writeAndFlush(CField.musicChange("BgmEvent2/rhythmBgm1"));
                                    }
                                }
                                if (c.getPlayer().getMapId() == 910048100) {
                                    c.getPlayer().getOXGame().InitGame();
                                }
                            }
                        }, 35 * 1000);
                        break;
                    }
                    case "1":// Forcely Stop Game
                    {
                        c.getPlayer().getOXGame().StopQuiz();
                        break;
                    }
                    case "2": {
                        String channel = c.getChannel() == 1 ? "1" : c.getChannel() == 2 ? "20세 이상" : String.valueOf(c.getChannel() - 1);
                        for (MapleCharacter chr : World.getAllChars()) {
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(3003301, 7000, "#face3#운영자가 OX퀴즈 게임 참여자를 모집 중이야! " + channel + "채널에서 깸디를 클릭해봐~", ""));
                        }
                        break;
                    }
                }
            }
            return 0;
        }
    }

    public static class SetDetectiveStep extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                switch (splitted[1]) {
                    case "0": {
                        if (c.getPlayer().getMapId() != 993022000) { //암호 추리 대기맵
                            c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(1052230, 3000, "#face1#암호 추리 대기맵에서 실행해 주세요", ""));
                            return 0;
                        }
                        if (c.getPlayer().getMap().getCharacters().size() < 30) {

                            c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(1052230, 3000, "#face1#암호 추리 최소 등록 인원은 30명 입니다", ""));
                            return 0;
                        }
                        DetectiveGame game = new DetectiveGame(c.getPlayer(), true);
                        c.getPlayer().setDetectiveGame(game);
                        c.getPlayer().getMap().broadcastMessage(CField.getClock(30)); //30 대기
                        ScheduledFuture<?> a = EventTimer.getInstance().schedule(() -> {
                            if (c.getPlayer().getDetectiveGame() == null) {
                                return;
                            }
                            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                if (chr != null) {
                                    chr.warp(993022100);
                                }
                            }
                        }, 30 * 1000);

                        EventTimer.getInstance().schedule(() -> {
                            if (c.getPlayer().getDetectiveGame() == null) {
                                return;
                            }
                            if (c.getPlayer().getMapId() == 993022100) {
                                game.RegisterPlayers(c.getPlayer().getMap().getAllCharactersThreadsafe());
                                for (MapleCharacter chr : c.getPlayer().getDetectiveGame().getPlayers()) {
                                    if (chr != null) {
                                        chr.setDetectiveGame(game);
                                    }
                                }
                                c.getPlayer().getDetectiveGame().StartGame();
                            }
                        }, 35 * 1000);
                    }
                    break;
                    case "1": { //Force Stop
                        c.getPlayer().getDetectiveGame().StopGame();
                    }
                    break;
                }
            }
            return 0;
        }
    }
}
