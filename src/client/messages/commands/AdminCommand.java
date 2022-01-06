package client.messages.commands;

import client.*;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.auction.AuctionServer;
import handling.channel.ChannelServer;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.ShutdownServer;
import server.Timer.EventTimer;
import server.field.boss.lucid.Butterfly;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.CPUSampler;
import tools.Pair;
import tools.packet.*;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import tools.packet.CWvsContext.BuffPacket;

/**
 *
 * @author Emilyx3
 */
public class AdminCommand {

    private static ScheduledFuture<?> ts = null;
    protected static Thread t = null;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    public static ScheduledFuture<?> getts() {
        return ts;
    }

    public static void setts(ScheduledFuture<?> ts1) {
        ts = ts1;
    }

    public static Thread gett() {
        return t;
    }

    public static void sett(Thread t1) {
        t = t1;
    }

    public static class StripEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ChannelServer cs = c.getChannelServer();
            for (MapleCharacter mchr : cs.getPlayerStorage().getAllCharacters().values()) {
                if (mchr.isGM()) {
                    continue;
                }
                MapleInventory equipped = mchr.getInventory(MapleInventoryType.EQUIPPED);
                MapleInventory equip = mchr.getInventory(MapleInventoryType.EQUIP);
                List<Short> ids = new ArrayList<Short>();
                for (Item item : equipped.newList()) {
                    ids.add(item.getPosition());
                }
                for (short id : ids) {
                    MapleInventoryManipulator.unequip(mchr.getClient(), id, equip.getNextFreeSlot(), MapleInventoryType.EQUIP);
                }
            }
            return 1;
        }
    }

    public static class 버프테스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            //3700514
            // c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(80002928), (byte) 1, (byte) 3);
            //     c.getPlayer().changeSkillLevel(80002930, (byte) 1, (byte) 1);
            //    c.getPlayer().changeSkillLevel(80002929, (byte) 1, (byte) 1);
            // SkillFactory.getSkill(80002928).getEffect(1).applyTo(c.getPlayer(), false);
            //     SkillFactory.getSkill(80002930).getEffect(1).applyTo(c.getPlayer());
            //      SkillFactory.getSkill(80002929).getEffect(1).applyTo(c.getPlayer());
            //     c.getSession().writeAndFlush(CField.UIPacket.openUI(1271));
            /*
             final int rate = Integer.parseInt(splitted[1]);
             c.getPlayer().setSkillBuffTest(rate);
             c.getPlayer().dropMessage(6, "[설정완료] 현재 버프 코드 : " + c.getPlayer().getSkillBuffTest());
             */
            return 1;
        }
    }

    public static class 테스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            /*for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters().values()) {
                    mch.gainMeso(Integer.parseInt(splitted[1]), true);
                }
            }*/
            //  c.getPlayer().getMap().broadcastMessage(MobPacket.BossLucid.Lucid3rdPhase(Integer.parseInt(splitted[1])));

            //  c.getSession().writeAndFlush(CField.UIPacket.openUI(Integer.parseInt(splitted[1])));
            // c.getPlayer().getClient().getSession().writeAndFlush(MobPacket.BossDunKel.eliteBossAttack(c.getPlayer()));
            return 1;
        }
    }

    public static class ExpRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(rate);
                    }
                } else {
                    c.getChannelServer().setExpRate(rate);
                }
                c.getPlayer().dropMessage(6, "Exprate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !exprate <number> [all]");
            }
            return 1;
        }
    }

    public static class 링크소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setLinkMobCount(100);
            for (int i = 0; i < 10; i++) {
                MapleMonster m = MapleLifeFactory.getMonster(100100);
                m.setHp((long) (m.getStats().getHp()));
                m.getStats().setHp((long) (m.getStats().getHp()));
                m.setOwner(c.getPlayer().getId());
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(m, c.getPlayer().getTruePosition());
            }
            return 1;
        }
    }

    public static class 경매장저장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] Splitted) {
            AuctionServer.saveItems();
            c.getPlayer().dropMessage(6, "경매장 데이터를 저장하였습니다.");
            c.getPlayer().getStat().setHp(0, c.getPlayer());
            return 1;
        }
    }

    public static class MesoRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(rate);
                    }
                } else {
                    c.getChannelServer().setMesoRate(rate);
                }
                c.getPlayer().dropMessage(6, "Meso Rate has been changed to " + rate + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !mesorate <number> [all]");
            }
            return 1;
        }
    }

    public static class DCAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
            }
            return 1;
        }
    }

    public static class Shutdown extends CommandExecute {

        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Shutting down...");
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
            }
            return 1;
        }
    }

    public static class ShutdownTime extends Shutdown {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, minutesLeft + "분 뒤 서버가 종료됩니다.");

            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage("서버 리붓이 예정되어 있습니다. 게임 이용에 참고하시길 바랍니다.");
            }

            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = EventTimer.getInstance().register(new Runnable() {

                    public void run() {
                        if (minutesLeft == 0) {
                            ShutdownServer.getInstance().shutdown();
                            t.start();
                            ts.cancel(false);
                            return;
                        }
                        if (minutesLeft < 10 || minutesLeft % 5 == 0) {
                            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "", "서버가 " + minutesLeft + "분 뒤 종료될 예정입니다."));
                        }
                        minutesLeft--;
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, "이미 저장된 타이머가 있습니다.");
            }
            return 1;
        }
    }

    public static class StartProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("connector");
            sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("log");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            c.getPlayer().dropMessageGM(-5, "프로파일링을 시작합니다.");
            return 1;
        }
    }

    public static class StopProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "CPU프로파일링.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "이미 존재하는 파일입니다. 삭제나 이름 변경을 해주세요.");
                    return 0;
                }
                sampler.stop();
                FileWriter fw = new FileWriter(file);
                sampler.save(fw, 1, 10);
                fw.close();
                sampler.reset();
                c.getPlayer().dropMessage(6, "파일을 저장했습니다.");
            } catch (IOException e) {
                System.err.println("Error saving profile" + e);
            }
            return 1;
        }
    }

    public static class 맥스스탯 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setDex((short) 32767, c.getPlayer());
            c.getPlayer().getStat().setInt((short) 32767, c.getPlayer());
            c.getPlayer().getStat().setLuk((short) 32767, c.getPlayer());
            c.getPlayer().getStat().setMaxHp(500000, c.getPlayer());
            if (!GameConstants.isZero(c.getPlayer().getJob())) {
                c.getPlayer().getStat().setMaxMp(500000, c.getPlayer());
                c.getPlayer().getStat().setMp(500000, c.getPlayer());
            }
            c.getPlayer().getStat().setHp(500000, c.getPlayer());
            c.getPlayer().getStat().setStr((short) 32767, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.STR, 32767);
            c.getPlayer().updateSingleStat(MapleStat.DEX, 32767);
            c.getPlayer().updateSingleStat(MapleStat.INT, 32767);
            c.getPlayer().updateSingleStat(MapleStat.LUK, 32767);
            c.getPlayer().updateSingleStat(MapleStat.MAXHP, 500000);
            if (!GameConstants.isZero(c.getPlayer().getJob())) {
                c.getPlayer().updateSingleStat(MapleStat.MAXMP, 500000);
                c.getPlayer().updateSingleStat(MapleStat.MP, 500000);
            }
            c.getPlayer().updateSingleStat(MapleStat.HP, 500000);
            return 1;
        }
    }

    public static class 스탯초기화 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().setStr((short) 100, c.getPlayer());
            c.getPlayer().getStat().setDex((short) 100, c.getPlayer());
            c.getPlayer().getStat().setInt((short) 100, c.getPlayer());
            c.getPlayer().getStat().setLuk((short) 100, c.getPlayer());
            c.getPlayer().getStat().setMaxHp(10000, c.getPlayer());
            if (!GameConstants.isZero(c.getPlayer().getJob())) {
                c.getPlayer().getStat().setMaxMp(10000, c.getPlayer());
                c.getPlayer().getStat().setMp(10000, c.getPlayer());
            }
            c.getPlayer().getStat().setHp(10000, c.getPlayer());
            c.getPlayer().updateSingleStat(MapleStat.STR, 100);
            c.getPlayer().updateSingleStat(MapleStat.DEX, 100);
            c.getPlayer().updateSingleStat(MapleStat.INT, 100);
            c.getPlayer().updateSingleStat(MapleStat.LUK, 100);
            c.getPlayer().updateSingleStat(MapleStat.MAXHP, 10000);
            if (!GameConstants.isZero(c.getPlayer().getJob())) {
                c.getPlayer().updateSingleStat(MapleStat.MAXMP, 10000);
                c.getPlayer().updateSingleStat(MapleStat.MP, 10000);
            }
            c.getPlayer().updateSingleStat(MapleStat.HP, 10000);
            return 1;
        }
    }

    public static class 시간 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(CField.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return 1;
        }
    }

    public static class 피시방시간 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Need amount.");
                return 0;
            }
            c.getPlayer().setInternetCafeTime(c.getPlayer().getInternetCafeTime() + Integer.parseInt(splitted[1]));
            c.getPlayer().dropMessage(6, "PC방 정량제를 " + Integer.parseInt(splitted[1]) + "분 늘렸습니다.");
            return 1;
        }
    }

    public static class 쿨타임리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().clearAllCooldowns();
            return 1;
        }
    }

    public static class 버프리스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "현재 버프 리스트입니다.");
            for (Pair<MapleBuffStat, MapleBuffStatValueHolder> effect : c.getPlayer().getEffects()) {
                c.getPlayer().dropMessage(-8, effect.left.name() + " : " + effect.right.effect.getSourceId() + " / " + effect.right.localDuration);
            }
            return 1;
        }
    }

    public static class 서버종료 extends CommandExecute {

        int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (AdminCommand.getts() == null && (AdminCommand.gett() == null || !AdminCommand.gett().isAlive())) {

                minutesLeft = Integer.parseInt(splitted[1]);
                AdminCommand.sett(new Thread(ShutdownServer.getInstance()));
                AdminCommand.setts(EventTimer.getInstance().register(new Runnable() {
                    public void run() {
                        if (minutesLeft == 0) {
                            ShutdownServer.getInstance().shutdown();
                            AdminCommand.gett().start();
                            AdminCommand.getts().cancel(false);
                        }
                        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "", "서버가 " + minutesLeft + "분 뒤 종료될 예정입니다. 안전한 저장을 위해 로그아웃 해주세요."));
                        minutesLeft--;
                    }
                }, 60000));
            } else {
                c.getPlayer().dropMessage(1, "리붓이 이미 진행중입니다.\n\n" + minutesLeft + " ~ " + (minutesLeft + 1) + "분 뒤에 서버가 종료됩니다.");
            }
            return 1;
        }
    }

    public static class 버프스탯테스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "버프를 선택해주세요.");
                return 0;
            }

            int type = Integer.parseInt(splitted[1]);

            if (type > MapleBuffStat.getUnkBuffStats().size()) {
                c.getPlayer().dropMessage(5, "최대 사이즈 : " + MapleBuffStat.getUnkBuffStats().size());
                return 0;
            } else {
                MapleBuffStat stat = MapleBuffStat.getUnkBuffStats().get(type);

                Map<MapleBuffStat, Pair<Integer, Integer>> dds = new HashMap<>();
                dds.put(stat, new Pair<>(1, 0));
                c.getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(dds, SkillFactory.getSkill(2121004).getEffect(20), c.getPlayer()));

                c.getPlayer().dropMessage(5, "적용된 버프 : " + stat.name());
            }
            return 1;
        }
    }

    public static class 전체소환 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chrs : cserv.getPlayerStorage().getAllCharacters().values()) {
                    if (chrs != null) {
                        if (chrs.getClient().getChannel() != c.getChannel()) {
                            chrs.changeChannel(c.getChannel());
                        }
                        if (chrs.getMapId() != c.getPlayer().getMapId()) {
                            chrs.warp(c.getPlayer().getMapId());
                        }
                    }
                }
            }
            return 1;
        }
    }

}
