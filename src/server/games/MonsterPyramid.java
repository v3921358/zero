package server.games;

import client.MapleCharacter;
import handling.world.World;
import server.Timer;
import tools.packet.CWvsContext;
import tools.packet.SLFCGPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author SLFCG
 */
public class MonsterPyramid {

    public static class MonsterPyramidMatchingInfo {

        public List<MapleCharacter> players = new ArrayList<>();

        public MonsterPyramidMatchingInfo(List<MapleCharacter> chrs) {
            for (MapleCharacter chr : chrs) {
                players.add(chr);
            }
        }
    }

    public static List<MapleCharacter> monsterPyramidMatchingQueue = new ArrayList<>();
    private List<PyramidPlayer> players = new ArrayList<>();

    public MonsterPyramid(List<MapleCharacter> chrs) {
        for (int i = 0; i < chrs.size(); i++) {
            players.add(new PyramidPlayer(chrs.get(i), (byte) i));
        }
    }

    public class PyramidPlayer {

        private byte position;
        private MapleCharacter chr;

        public PyramidPlayer(MapleCharacter player, byte position) {
            this.chr = player;
            this.position = position;
        }

        public MapleCharacter getPlayer() {
            return chr;
        }

        public byte getPosition() {
            return position;
        }

        public void setPosition(byte position) {
            this.position = position;
        }
    }

    public PyramidPlayer getPlayer(MapleCharacter chr) {
        for (PyramidPlayer ocp : getPlayers()) {
            if (ocp.getPlayer().getId() == chr.getId()) {
                return ocp;
            }
        }
        return null;
    }

    private void StartGame(MapleCharacter chr) {
    }

    public void skipPlayer() {

    }

    public void endGame() {

    }

    public void playerDead() {

    }

    public static void addQueue(MapleCharacter chr, int req) {
        if (!monsterPyramidMatchingQueue.contains(chr)) {
            monsterPyramidMatchingQueue.add(chr);
            if (monsterPyramidMatchingQueue.size() == req) {
                MonsterPyramidMatchingInfo info = new MonsterPyramidMatchingInfo(monsterPyramidMatchingQueue);

                List<MapleCharacter> chrs = new ArrayList<>();

                for (MapleCharacter player : info.players) {
                    monsterPyramidMatchingQueue.remove(player);
                    player.warp(910044100);
                    chrs.add(player);
                }

                Timer.EventTimer.getInstance().schedule(() -> {

                    MonsterPyramid ocg = new MonsterPyramid(chrs);

                    for (MapleCharacter p : chrs) {
                        p.setMonsterPyramidInstance(ocg);
                    }

                    for (PyramidPlayer ocp : ocg.getPlayers()) {
                        ocp.getPlayer().getClient().getSession().writeAndFlush(SLFCGPacket.MonsterPyramidPacket.createUI(chrs));
                        ocp.getPlayer().getClient().getSession().writeAndFlush(SLFCGPacket.MonsterPyramidPacket.onInit(chrs));
                    }

                    Timer.EventTimer.getInstance().schedule(() -> {
                        ocg.StartGame(chr);
                    }, 3 * 1000);
                }, 5 * 1000);
            } else {
                World.Broadcast.broadcastSmega(CWvsContext.serverNotice(19, "", chr.getName() + "님이 피라미드 대기열에 캐릭터를 등록했습니다. 남은 인원 : " + (req - monsterPyramidMatchingQueue.size())));
            }
        }
    }

    public void sendPacketToPlayers(byte[] packet) {
        for (PyramidPlayer player : this.players) {
            player.getPlayer().getClient().getSession().writeAndFlush(packet);
        }
    }

    public List<PyramidPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<PyramidPlayer> players) {
        this.players = players;
    }
}
