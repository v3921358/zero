package server.games;

import client.MapleCharacter;
import handling.world.World;
import server.Randomizer;
import server.Timer;
import tools.packet.CWvsContext;
import tools.packet.SLFCGPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author SLFCG
 */
public class MultiYutGame {

    private int objectId = 1;

    public class Yut {

        private int objectId;

        public Yut(int... args) {
            this.setObjectId(args[0]);
        }

        public int getObjectId() {
            return objectId;
        }

        public void setObjectId(int objectId) {
            this.objectId = objectId;
        }
    }

    public class MultiYutPlayer {

        private byte position;
        private List<Yut> yuts;
        private MapleCharacter chr;

        public MultiYutPlayer(MapleCharacter player, byte position) {
            chr = player;
            this.yuts = new ArrayList<>();
            this.position = position;

            this.yuts.add(new Yut(++objectId));
            this.yuts.add(new Yut(++objectId));
            this.yuts.add(new Yut(++objectId));
            this.yuts.add(new Yut(++objectId));
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

        public List<Yut> getYuts() {
            return yuts;
        }

        public void setYuts(List<Yut> yuts) {
            this.yuts = yuts;
        }
    }

    public static class multiYutMagchingInfo {

        public List<MapleCharacter> players = new ArrayList<>();

        public multiYutMagchingInfo(List<MapleCharacter> chrs) {
            for (MapleCharacter chr : chrs) {
                players.add(chr);
            }
        }
    }

    public static List<MapleCharacter> multiYutMagchingQueue = new ArrayList<>();
    private ScheduledFuture<?> multiYutTimer = null;
    private List<MultiYutPlayer> players = new ArrayList<>();

    public MultiYutGame(List<MapleCharacter> chrs) {
        for (int i = 0; i < chrs.size(); i++) {
            getPlayers().add(new MultiYutPlayer(chrs.get(i), (byte) i));
        }
    }

    public MultiYutPlayer getPlayer(MapleCharacter chr) {
        for (MultiYutPlayer ocp : getPlayers()) {
            if (ocp.chr.getId() == chr.getId()) {
                return ocp;
            }
        }
        return null;
    }

    public MultiYutPlayer getOpponent(MapleCharacter chr) {
        for (MultiYutPlayer ocp : getPlayers()) {
            if (ocp.chr.getId() != chr.getId()) {
                return ocp;
            }
        }
        return null;
    }

    private List<MultiYutPlayer> getPlayers() {
        return players;
    }

    private void StartGame(MapleCharacter chr) {

        MultiYutPlayer first = Randomizer.nextBoolean() ? getPlayers().get(1) : getPlayers().get(0);

//    	sendPacketToPlayers(SLFCGPacket.MultiYutGamePacket);
        if (getMultiYutTimer() != null) {
            getMultiYutTimer().cancel(false);
        }

        setMultiYutTimer(Timer.EventTimer.getInstance().schedule(() -> {
            //15초 잠수시 강제로 카드 맥이고 넘어가야함
//        	skipPlayer();
        }, 15 * 1000));
        //TODO: Run Game Timer
    }

    public static void addQueue(MapleCharacter chr) {
        if (!multiYutMagchingQueue.contains(chr)) {
            multiYutMagchingQueue.add(chr);
            if (multiYutMagchingQueue.size() == 2) {
                multiYutMagchingInfo info = new multiYutMagchingInfo(multiYutMagchingQueue);

                List<MapleCharacter> chrs = new ArrayList<>();

                for (MapleCharacter player : info.players) {
                    multiYutMagchingQueue.remove(player);
                    player.warp(910044100);
                    chrs.add(player);
                }

                Timer.EventTimer.getInstance().schedule(() -> {

                    MultiYutGame myg = new MultiYutGame(chrs);

                    for (MapleCharacter p : chrs) {
                        p.setMultiYutInstance(myg);
                    }

                    for (MultiYutPlayer myp : myg.getPlayers()) {
                        MultiYutPlayer me = myg.getPlayer(myp.getPlayer());
                        MultiYutPlayer opponent = myg.getOpponent(myp.getPlayer());
                        myp.chr.getClient().getSession().writeAndFlush(SLFCGPacket.MultiYutGamePacket.createUI(me.chr, opponent.chr));
                    }

                    Timer.EventTimer.getInstance().schedule(() -> {
                        myg.StartGame(chr);
                    }, 3 * 1000);
                }, 5 * 1000);
            } else {
                World.Broadcast.broadcastSmega(CWvsContext.serverNotice(19, "", chr.getName() + "님이 윷놀이 대기열에 캐릭터를 등록했습니다. 남은 인원 : 1"));
            }
        }
    }

    public void sendPacketToPlayers(byte[] packet) {
        for (MultiYutPlayer player : this.players) {
            player.getPlayer().getClient().getSession().writeAndFlush(packet);
        }
    }

    public ScheduledFuture<?> getMultiYutTimer() {
        return multiYutTimer;
    }

    public void setMultiYutTimer(ScheduledFuture<?> multiYutTimer) {
        this.multiYutTimer = multiYutTimer;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }
}
