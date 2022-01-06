package server.games;

import client.MapleCharacter;
import constants.GameConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import server.Randomizer;
import server.Timer;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author SLFCG
 */
public class BattleReverse {

    public class BattleReverseStone {

        private int StoneId;
        private Point Position;

        public BattleReverseStone(int... args) {
            Position = new Point(args[0], args[1]);
            StoneId = args[2];
        }

        public Point getStonePosition() {
            return Position;
        }

        public int getStoneId() {
            return StoneId;
        }

        public void setStoneId(final int a) {
            StoneId = a;
        }
    }

    public class BattleReversePlayer {

        private int HP, StoneId;
        private MapleCharacter chr;

        public BattleReversePlayer(MapleCharacter player, int... args) {
            chr = player;
            HP = args[0];
            StoneId = args[1]; // 0: 콩 1: 닭 3: 돌
        }

        public MapleCharacter getPlayer() {
            return chr;
        }

        public void setHP(int a1) {
            HP = a1;
        }

        public int getHP() {
            return HP;
        }

        public int getStoneId() {
            return StoneId;
        }
    }

    public static class BattleReverseMatchingInfo {

        public MapleCharacter p1, p2;

        public BattleReverseMatchingInfo(MapleCharacter... chrs) {
            p1 = chrs[0];
            p2 = chrs[1];
        }
    }

    public static List<MapleCharacter> BattleReverseMatchingQueue = new ArrayList<>();
    public static List<BattleReverseMatchingInfo> BattleReverseGameList = new ArrayList<>();
    private List<BattleReversePlayer> Players = new ArrayList<>();
    private BattleReverseStone[][] Board = new BattleReverseStone[8][8];
    private Point lastPoint = new Point(0, 0);
    private ScheduledFuture<?> othelloTimer = null;

    public BattleReverse(List<MapleCharacter> chrs) {
        int stone = 0;
        for (MapleCharacter chr : chrs) {
            Players.add(new BattleReversePlayer(chr, 1000, stone++));
        }
        initBoard();
    }

    private void initBoard() {
        for (int a = 0; a < 8; a++) {
            for (int b = 0; b < 8; b++) {
                Board[a][b] = new BattleReverseStone(a, b, -1);
            }
        }
        Board[3][3].setStoneId(0);
        Board[4][4].setStoneId(0);
        Board[4][3].setStoneId(1);
        Board[3][4].setStoneId(1);

        for (int a = 0; a < Randomizer.nextInt(5) + 1; a++) {
            boolean temp = false;
            while (!temp) {
                temp = makeHole(new Point(Randomizer.nextInt(8), Randomizer.nextInt(8)));
            }
        }
    }

    private boolean makeHole(Point p) {
        if (Board[p.x][p.y].getStoneId() == -1) {
            Board[p.x][p.y].setStoneId(3);
            return true;
        }
        return false;
    }

    public List<BattleReverseStone> getStones() {
        List<BattleReverseStone> list = new ArrayList<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (Board[x][y].getStoneId() != -1) {
                    list.add(Board[x][y]);
                }
            }
        }
        return list;
    }

    public List<BattleReversePlayer> getPlayers() {
        return this.Players;
    }

    public BattleReversePlayer getPlayer(int id) {
        for (BattleReversePlayer player : this.Players) {
            if (player.chr.getId() == id) {
                return player;
            }
        }
        return null;
    }

    public BattleReversePlayer getOpponent(int id) {
        for (BattleReversePlayer player : this.Players) {
            if (player.chr.getId() != id) {
                return player;
            }
        }
        return null;
    }

    public boolean isValidMove(MapleCharacter chr, Point pt) {
        boolean ret = false;
        BattleReversePlayer Player = this.getPlayer(chr.getId());
        BattleReversePlayer Opponent = this.getOpponent(chr.getId());
        if (Player != null && Opponent != null & pt != null) {
            if ((Player.getStoneId() == 0 || Player.getStoneId() == 1) && (Opponent.getStoneId() == 0 || Opponent.getStoneId() == 1)) {
                if (pt.x >= 0 && pt.x < 8 && pt.y >= 0 && pt.y < 8) {
                    ret = getPlaceablePoints(Player).contains(pt);
                }
            }
        }
        return ret;
    }

    public void sendPlaceStone(MapleCharacter chr, Point pos) {
        if (chr.getBattleReverseInstance().isValidMove(chr, pos)) {
            chr.getBattleReverseInstance().placeStone(pos, chr);
        }
    }

    public void skipPlayer(MapleCharacter chr) {
        List<Point> pts = chr.getBattleReverseInstance().getPlaceablePoints(chr.getBattleReverseInstance().getPlayer(chr.getId()));
        if (!pts.isEmpty()) {
            chr.getBattleReverseInstance().sendPlaceStone(chr, pts.get(Randomizer.nextInt(pts.size())));
        } else {
            chr.getBattleReverseInstance().endGame(chr, true);
            chr.dropMessage(1, "오류가 발생하여 게임이 취소됩니다.");
        }
    }

    public void placeStone(Point pt, MapleCharacter chr) {
        BattleReversePlayer player = this.getPlayer(chr.getId());
        BattleReversePlayer opponent = this.getOpponent(chr.getId());
        int x = pt.x, y = pt.y;
        int b_x = x, b_y = y;
        this.setLastPoint(pt);

        int c = 0;

        //왼위
        while (x >= 0 && y >= 0) {
            if (x != b_x && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (b_x - x); ++i) {
                    this.Board[x + i][y + i].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            x--;
            y--;
        }

        x = b_x;
        y = b_y;

        //왼
        while (x >= 0) {
            if (x != b_x && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (b_x - x); ++i) {
                    this.Board[x + i][y].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            x--;
        }

        x = b_x;
        y = b_y;

        //왼아
        while (x >= 0 && y <= 7) {
            if (x != b_x && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (b_x - x); ++i) {
                    this.Board[x + i][y - i].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            x--;
            y++;
        }

        x = b_x;
        y = b_y;

        //아
        while (y <= 7) {
            if (y != b_y && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (y - b_y); ++i) {
                    this.Board[x][y - i].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            y++;
        }

        x = b_x;
        y = b_y;

        //오아
        while (x <= 7 && y <= 7) {
            if (x != b_x && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (x - b_x); ++i) {
                    this.Board[x - i][y - i].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            x++;
            y++;
        }

        x = b_x;
        y = b_y;

        //오
        while (x <= 7) {
            if (x != b_x && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (x - b_x); ++i) {
                    this.Board[x - i][y].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            x++;
        }

        x = b_x;
        y = b_y;

        //오위
        while (x <= 7 && y >= 0) {
            if (x != b_x && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (x - b_x); ++i) {
                    this.Board[x - i][y + i].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            x++;
            y--;
        }

        x = b_x;
        y = b_y;

        //위
        while (y >= 0) {
            if (y != b_y && (this.Board[x][y].getStoneId() == -1 || this.Board[x][y].getStoneId() == 3)) {
                break;
            }
            if (this.Board[x][y].getStoneId() == player.getStoneId()) {
                for (int i = 0; i < (b_y - y); ++i) {
                    this.Board[x][y + i].setStoneId(player.getStoneId());
                    c++;
                }
                break;
            }
            y--;
        }

        x = b_x;
        y = b_y;

        opponent.setHP(opponent.getHP() - c);
        if (c >= 4) { // good
            if (c >= 6) { // great
                opponent.setHP(opponent.getHP() - 10);
            } else {
                opponent.setHP(opponent.getHP() - 5);
            }
        }

        this.Board[x][y].setStoneId(player.getStoneId());

        if (opponent.getHP() <= 0) {
            for (BattleReversePlayer bp : this.Players) {
                bp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onBoardUpdate(opponent.getPlayer().getId() == bp.getPlayer().getId(), pt, player.getStoneId(), getOpponent(player.getPlayer().getId()).getHP(), this.getStones()));
            }
            endGame(chr, false);
        } else {
            setTurn(chr, player, opponent, pt);
        }
    }

    public void setTurn(MapleCharacter chr, BattleReversePlayer player, BattleReversePlayer opponent, Point pt) {
        if (getPlaceablePoints(getOpponent(chr.getId())).size() == 0) {
            if (getPlaceablePoints(getPlayer(chr.getId())).size() == 0) {
                for (BattleReversePlayer bp : this.Players) {
                    bp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onBoardUpdate(opponent.getPlayer().getId() == bp.getPlayer().getId(), pt, player.getStoneId(), getOpponent(player.getPlayer().getId()).getHP(), this.getStones()));
                }
                endGame(chr, false);
            } else {
                for (BattleReversePlayer bp : this.Players) {
                    bp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.onShowText("둘 수 있는 곳이 없어 턴이 넘어갑니다."));
                    bp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onBoardUpdate(player.getPlayer().getId() == bp.getPlayer().getId(), pt, getOpponent(chr.getId()).getStoneId(), getPlayer(chr.getId()).getHP(), this.getStones()));
                }
            }
        } else {
            for (BattleReversePlayer bp : this.Players) {
                bp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onBoardUpdate(opponent.getPlayer().getId() == bp.getPlayer().getId(), pt, player.getStoneId(), getOpponent(player.getPlayer().getId()).getHP(), this.getStones()));
            }

            if (othelloTimer != null) {
                othelloTimer.cancel(false);
            }

            othelloTimer = Timer.EventTimer.getInstance().schedule(() -> {
                //15초 잠수시 강제로 카드 맥이고 넘어가야함
                skipPlayer(opponent.chr);
            }, 16 * 1000);
        }
    }

    public void endGame(MapleCharacter chr, boolean exit) {

        BattleReversePlayer player = getPlayer(chr.getId());
        BattleReversePlayer opponent = getOpponent(chr.getId());

        BattleReversePlayer winner = null, loser = null;

        if (!exit) {
            if (player.getHP() <= 0) {
                winner = opponent;
                loser = player;
            } else if (opponent.HP <= 0) {
                winner = player;
                loser = opponent;
            } else {
                if (player.getHP() > opponent.getHP()) {
                    winner = player;
                    loser = opponent;
                } else if (player.getHP() < opponent.getHP()) {
                    winner = opponent;
                    loser = player;
                } else {
                    player.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onResult(2));
                    opponent.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onResult(2));

                    //비김
                }
            }
        } else {
            opponent.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onResult(4));
            opponent.chr.getClient().getSession().writeAndFlush(SLFCGPacket.onShowText("상대방이 미니게임을 종료하여 게임이 종료됩니다."));
            opponent.chr.getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/victory"));
            opponent.chr.getClient().getSession().writeAndFlush(SLFCGPacket.OneCardGamePacket.onShowScreenEffect("/Effect/screeneff/gameover"));
        }

        if (winner != null && loser != null) {
            loser.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onResult(1));
            loser.chr.getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/gameover"));

            winner.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onResult(4));
            winner.chr.getClient().getSession().writeAndFlush(CField.playSound("Sound/MiniGame.img/oneCard/victory"));
            /*
            final int winp  = 500;
            final int losep  = 200;
            if(opponent.getPlayer().getKeyValue(501215, "today") < 0){
                opponent.getPlayer().setKeyValue(501215, "today", 0 + "");
            }
            if(player.getPlayer().getKeyValue(501215, "today") < 0){
                player.getPlayer().setKeyValue(501215, "today", 0 + "");
            }

            if(opponent.getPlayer().getKeyValue(501215, "today") >= 2000){
                opponent.getPlayer().dropMessage(-8, "하루동안 [미니게임 플레이]로 획득 가능한 네오 코어량을 초과하였습니다.");
            }else{
                int todaycore = (int) opponent.getPlayer().getKeyValue(501215, "point") + winp;
                int coreg = winp + (int) opponent.getPlayer().getKeyValue(501215, "point");
                opponent.getPlayer().setKeyValue(501215, "today", todaycore + "");
                opponent.getPlayer().setKeyValue(501215, "point", coreg + "");
                opponent.getPlayer().dropMessage(-8, "게임에서 승리하여 "+winp+"포인트를 획득하였습니다.");
                opponent.getPlayer().updateInfoQuest(501215, "point=" + opponent.getPlayer().getKeyValue(501215, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";week=0;total=0;lock=0"); //네오 코어
            }
            if(player.getPlayer().getKeyValue(501215, "today") >= 2000){
                player.getPlayer().dropMessage(-8, "하루동안 [미니게임 플레이]로 획득 가능한 네오 코어량을 초과하였습니다.");
            }else{
                int todaycore = (int) player.getPlayer().getKeyValue(501215, "point") + winp;
                int coreg = winp + (int) player.getPlayer().getKeyValue(501215, "point");
                player.getPlayer().setKeyValue(501215, "today", todaycore + "");
                player.getPlayer().setKeyValue(501215, "point", coreg + "");
                opponent.getPlayer().dropMessage(-8, "게임에서 패배하여 "+losep+"포인트를 획득하였습니다.");
                player.getPlayer().updateInfoQuest(501215, "point=" + player.getPlayer().getKeyValue(501215, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";week=0;total=0;lock=0"); //네오 코어
            }*/

            winner.chr.getClient().getSession().writeAndFlush(SLFCGPacket.onShowText(winner.chr.getName() + "님의 승리!"));
            loser.chr.getClient().getSession().writeAndFlush(SLFCGPacket.onShowText(winner.chr.getName() + "님의 승리!"));
        }

        if (othelloTimer != null) {
            othelloTimer.cancel(false);
        }

        Timer.EventTimer.getInstance().schedule(() -> {
            for (BattleReversePlayer bp : this.getPlayers()) {
                bp.chr.warp(ServerConstants.WarpMap);
                bp.chr.setBattleReverseInstance(null);
            }

        }, 7 * 1000);
    }

    public List<Point> getPlaceablePoints(BattleReversePlayer Player) {
        List<Point> list = new ArrayList<>();
        BattleReversePlayer Opponent = this.getOpponent(Player.chr.getId());

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (Board[x][y].getStoneId() == Opponent.getStoneId()) {
                    int b_x = x, b_y = y;

                    /*                    if (x - 1 >= 0 & y - 1 >= 0) {
                     if (Board[x - 1][y - 1].getStoneId() == -1) {
                     if (x < 7 & y < 7) {
                     x++;
                     y++;
                     while (x < 7 & y < 7 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     x++;
                     y++;
                     }

                     if (x <= 7 & y <= 7 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x - 1, b_y - 1));
                     }
                     }
                     }
                     }

                     x = b_x;
                     y = b_y;
                     if (x - 1 >= 0) {
                     if (Board[x - 1][y].getStoneId() == -1) {
                     if (x < 7) {
                     x++;
                     while (x < 7 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     x++;
                     }
                     if (x <= 7 && Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x - 1, b_y));
                     }
                     }
                     }
                     }

                     x = b_x;

                     if (x - 1 >= 0 & y + 1 <= 7) {
                     if (Board[x - 1][y + 1].getStoneId() == -1) {
                     if (x < 7 & y > 1) {
                     x++;
                     y--;
                     while (x < 7 & y > 0 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     x++;
                     y--;
                     }

                     if (x <= 7 & y >= 0 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x - 1, b_y + 1));
                     }
                     }
                     }
                     }

                     x = b_x;
                     y = b_y;

                     if (y - 1 >= 0) {
                     if (Board[x][y - 1].getStoneId() == -1) {
                     if (y < 7) {
                     y++;
                     while (y < 7 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     y++;
                     }
                     if (y <= 7 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x, b_y - 1));
                     }
                     }
                     }
                     }

                     y = b_y;
                     if (y + 1 <= 7) {
                     if (Board[x][y + 1].getStoneId() == -1) {
                     if (y > 1) {
                     y--;
                     while (y > 0 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     y--;
                     }
                     if (y >= 0 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x, b_y + 1));
                     }
                     }
                     }
                     }

                     y = b_y;

                     if (x + 1 <= 7 & y - 1 >= 0) {
                     if (Board[x + 1][y - 1].getStoneId() == -1) {
                     if (x > 1 && y < 7) {
                     x--;
                     y++;
                     while (x > 0 & y < 7 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     x--;
                     y++;
                     }

                     if (x >= 0 & y <= 7 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x + 1, b_y - 1));
                     }
                     }
                     }
                     }

                     x = b_x;
                     y = b_y;
                     if (x + 1 <= 7) {
                     if (Board[x + 1][y].getStoneId() == -1 & x > 0) {
                     if (x > 1) {
                     x--;
                     while (x > 0 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     x--;
                     }
                     if (x >= 0 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x + 1, b_y));
                     }
                     }
                     }
                     }

                     x = b_x;
                     if (x + 1 <= 7 & y + 1 <= 7) {
                     if (Board[x + 1][y + 1].getStoneId() == -1) {
                     if (x > 1 & y > 1) {
                     x--;
                     y--;
                     while (x > 0 & y > 0 & Board[x][y].getStoneId() == Opponent.getStoneId()) {
                     x--;
                     y--;
                     }

                     if (x >= 0 & y >= 0 & Board[x][y].getStoneId() == Player.getStoneId()) {
                     list.add(new Point(b_x + 1, b_y + 1));
                     }
                     }
                     }
                     }*/
                    //leftTop
                    if (x - 1 >= 0 && y - 1 >= 0 && x + 1 <= 7 && y + 1 <= 7) {
                        if (Board[x + 1][y + 1].getStoneId() == Player.getStoneId()) {
                            while (x - 1 >= 0 && y - 1 >= 0) {
                                if (Board[x - 1][y - 1].getStoneId() == Opponent.getStoneId()) {
                                    x--;
                                    y--;
                                } else if (Board[x - 1][y - 1].getStoneId() == -1) {
                                    list.add(new Point(x - 1, y - 1));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //rightTop
                    if (x + 1 <= 7 && y - 1 >= 0 && x - 1 >= 0 && y + 1 <= 7) {
                        if (Board[x - 1][y + 1].getStoneId() == Player.getStoneId()) {
                            while (x + 1 <= 7 && y - 1 >= 0) {
                                if (Board[x + 1][y - 1].getStoneId() == Opponent.getStoneId()) {
                                    x++;
                                    y--;
                                } else if (Board[x + 1][y - 1].getStoneId() == -1) {
                                    list.add(new Point(x + 1, y - 1));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //rightBottom
                    if (x + 1 <= 7 && y + 1 <= 7 && x - 1 >= 0 && y - 1 >= 0) {
                        if (Board[x - 1][y - 1].getStoneId() == Player.getStoneId()) {
                            while (x + 1 <= 7 && y + 1 <= 7) {
                                if (Board[x + 1][y + 1].getStoneId() == Opponent.getStoneId()) {
                                    x++;
                                    y++;
                                } else if (Board[x + 1][y + 1].getStoneId() == -1) {
                                    list.add(new Point(x + 1, y + 1));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //leftBottom
                    if (x - 1 >= 0 && y + 1 <= 7 && x + 1 <= 7 && y - 1 >= 0) {
                        if (Board[x + 1][y - 1].getStoneId() == Player.getStoneId()) {
                            while (x - 1 >= 0 && y + 1 <= 7) {
                                if (Board[x - 1][y + 1].getStoneId() == Opponent.getStoneId()) {
                                    x--;
                                    y++;
                                } else if (Board[x - 1][y + 1].getStoneId() == -1) {
                                    list.add(new Point(x - 1, y + 1));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //Top
                    if (y - 1 >= 0 && y + 1 <= 7) {
                        if (Board[x][y + 1].getStoneId() == Player.getStoneId()) {
                            while (y - 1 >= 0) {
                                if (Board[x][y - 1].getStoneId() == Opponent.getStoneId()) {
                                    y--;
                                } else if (Board[x][y - 1].getStoneId() == -1) {
                                    list.add(new Point(x, y - 1));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //Bottom
                    if (y + 1 <= 7 && y - 1 >= 0) {
                        if (Board[x][y - 1].getStoneId() == Player.getStoneId()) {
                            while (y + 1 <= 7) {
                                if (Board[x][y + 1].getStoneId() == Opponent.getStoneId()) {
                                    y++;
                                } else if (Board[x][y + 1].getStoneId() == -1) {
                                    list.add(new Point(x, y + 1));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //right
                    if (x + 1 <= 7 && x - 1 >= 0) {
                        if (Board[x - 1][y].getStoneId() == Player.getStoneId()) {
                            while (x + 1 <= 7) {
                                if (Board[x + 1][y].getStoneId() == Opponent.getStoneId()) {
                                    x++;
                                } else if (Board[x + 1][y].getStoneId() == -1) {
                                    list.add(new Point(x + 1, y));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;

                    //left
                    if (x - 1 >= 0 && x + 1 <= 7) {
                        if (Board[x + 1][y].getStoneId() == Player.getStoneId()) {
                            while (x - 1 >= 0) {
                                if (Board[x - 1][y].getStoneId() == Opponent.getStoneId()) {
                                    x--;
                                } else if (Board[x - 1][y].getStoneId() == -1) {
                                    list.add(new Point(x - 1, y));
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    x = b_x;
                    y = b_y;
                }
            }
        }
        return list;
    }

    private void startGame() {
        BattleReversePlayer First = this.Players.get(0);
        for (BattleReversePlayer bp : this.Players) {
            bp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.onInit(this.getStones(), First.StoneId));
        }

        if (othelloTimer != null) {
            othelloTimer.cancel(false);
        }

        othelloTimer = Timer.EventTimer.getInstance().schedule(() -> {
            //15초 잠수시 강제로 카드 맥이고 넘어가야함
            skipPlayer(First.chr);
        }, 16 * 1000);
    }

    public static void addQueue(MapleCharacter chr, boolean partyQueue) {
        if (!partyQueue) {
            if (!BattleReverseMatchingQueue.contains(chr)) {
                BattleReverseMatchingQueue.add(chr);
                if (BattleReverseMatchingQueue.size() == 2) {
                    BattleReverseMatchingInfo info = new BattleReverseMatchingInfo(BattleReverseMatchingQueue.get(0), BattleReverseMatchingQueue.get(1));
                    BattleReverseGameList.add(info);
                    BattleReverseMatchingQueue.remove(info.p1);
                    BattleReverseMatchingQueue.remove(info.p2);
                    info.p1.warp(993101000);
                    info.p2.warp(993101000);
                    Timer.EventTimer.getInstance().schedule(() -> {
                        //p1 = 오른쪽 p2 = 왼쪽
                        List<MapleCharacter> chrs = new ArrayList<>();
                        chrs.add(info.p1);
                        chrs.add(info.p2);

                        BattleReverse br = new BattleReverse(chrs);

                        for (MapleCharacter p : chrs) {
                            p.setBattleReverseInstance(br);
                        }

                        br.initBoard();

                        for (MapleCharacter p : chrs) {
                            p.getClient().getSession().writeAndFlush(SLFCGPacket.MultiOthelloGamePacket.createUI(chrs, p, br.getPlayer(p.getId()).getStoneId()));
                        }

                        Timer.EventTimer.getInstance().schedule(() -> {
                            br.startGame();
                        }, 3 * 1000);
                    }, 5 * 1000);
                } else if (!partyQueue) {
                    World.Broadcast.broadcastSmega(CWvsContext.serverNotice(19, "", chr.getName() + "님이 배틀 리버스 대기열에 캐릭터를 등록했습니다. 남은 인원 : " + 1));
                }
            }
        } else {
            for (MaplePartyCharacter pc : chr.getParty().getMembers()) {
                int ch = World.Find.findChannel(pc.getId());
                if (ch > 0) {
                    MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(pc.getId());
                    addQueue(player, false);
                }
            }
        }
    }

    public Point getLastPoint() {
        return lastPoint;
    }

    public void setLastPoint(Point lastPoint) {
        this.lastPoint = lastPoint;
    }

    public ScheduledFuture<?> getOthelloTimer() {
        return othelloTimer;
    }

    public void setOthelloTimer(ScheduledFuture<?> othelloTimer) {
        this.othelloTimer = othelloTimer;
    }
}
