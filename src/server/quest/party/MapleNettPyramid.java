/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.quest.party;

import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author 윤정환
 */
public class MapleNettPyramid {

    private int wave, life;
    private MapleMap map;
    private List<MapleNettPyramidMember> members = new ArrayList<>();
    private Point point1, point2, point3, point4;

    private ScheduledFuture<?> monstertask;
    private ScheduledFuture<?> wavetask;
    private boolean next;
    private boolean hard;
    private long[] easyhp = {100000000L, 200000000L, 300000000L, 400000000L, 500000000L, 600000000L, 700000000L, 800000000L, 900000000L, 1000000000L, 1200000000L, 1300000000L, 1400000000L, 1500000000L, 1600000000L, 1700000000L, 1800000000L, 1900000000L, 2000000000L, 2300000000L, 2500000000L, 3000000000L, 30000000000L};
    private long[] hardhp = {1000000000L, 2000000000L, 3000000000L, 4000000000L, 5000000000L, 6000000000L, 7000000000L, 8000000000L, 9000000000L, 10000000000L, 12000000000L, 13000000000L, 14000000000L, 15000000000L, 16000000000L, 17000000000L, 18000000000L, 19000000000L, 20000000000L, 23000000000L, 25000000000L, 30000000000L, 300000000000L};
    //피라미드 체력

    private MapleNettPyramid() {
        wave = -1;
        life = 20;
        map = null;
        setMembers(new ArrayList<>());
        point1 = new Point(910, 155);
        point2 = new Point(910, -25);
        point3 = new Point(910, -205);
        point4 = new Point(910, -385);
        monstertask = null;
        wavetask = null;
        next = false;
    }

    public static MapleNettPyramid getInfo(final MapleCharacter chr, final boolean hard) {
        final MapleNettPyramid ret = new MapleNettPyramid();
        final MapleClient c = chr.getClient();
        ret.hard = hard;
        ret.map = chr.getMap();
        if (chr.getParty() != null) {
            for (MaplePartyCharacter member : chr.getParty().getMembers()) {
                MapleCharacter m = c.getChannelServer().getPlayerStorage().getCharacterById(member.getId());
                if (m != null) {
                    ret.getMembers().add(new MapleNettPyramidMember(m));
                }
            }
        } else {
            return null;
        }
        return ret;
    }

    public static boolean warpNettPyramid(final MapleCharacter chr, final boolean hard) {
        if (chr.isLeader()) {
            MapleCharacter m = null;
            MapleClient c = chr.getClient();
            final ChannelServer ch = chr.getClient().getChannelServer();
            MapleMap map = null;
            for (int i = 0; i < 20; i++) {
                map = ch.getMapFactory().getMap(926010300 + i);
                if (map.getCharactersSize() == 0) {
                    map.resetFully(false);
                    break;
                }
                map = null;
            }

            if (map != null) {
                for (MaplePartyCharacter member : chr.getParty().getMembers()) {
                    m = c.getChannelServer().getPlayerStorage().getCharacterById(member.getId());
                    m.nettDifficult = hard ? 2 : 1;
                    if (m != null) {
                        m.changeMap(map);
                    }
                    m = null;
                }
            } else {
                chr.Message("이용 가능한 맵이 없습니다. 채널 이동 후 시도해주세요.");
                chr.message("이용 가능한 맵이 없습니다. 채널 이동 후 시도해주세요.");
                return false;
            }
        } else {
            chr.Message("파티장이 아니면 입장 신청을 하실 수 없습니다.");
            chr.message("파티장이 아니면 입장 신청을 하실 수 없습니다.");
            return false;
        }
        return true;
    }

    public void firstNettPyramid(MapleCharacter chr) {
        try {
            setting();
            startNettPyramid();
        } catch (Exception e) {
            e.printStackTrace();
            chr.dropMessage(-8, "오류가 발생했습니다. 이 메세지를 찍어 1대1 문의에 제보하세요.");
        }
    }

    public void setting() {
        if (map != null) {
            map.broadcastMessage(CField.NettPyramidPoint(0));
            map.broadcastMessage(CField.UIPacket.openUI(62));
        }
        nextWave();
        changeLife();
    }

    public MapleNettPyramidMember getMember(final int cid) {
        for (MapleNettPyramidMember mnpm : this.getMembers()) {
            if (mnpm.getCid() == cid) {
                return mnpm;
            }
        }
        return null;
    }

    public void startNettPyramid() {
        if (wavetask != null) {
            wavetask.cancel(true);
            wavetask = null;
        }
        wavetask = EventTimer.getInstance().register(new Runnable() {
            int time = -1;

            @Override
            public void run() {
                time++;
                if (time == 5) {
                    showCount();
                } else if (time == 8) {
                    startWaveNettPyramid();
                    if (wavetask != null) {
                        wavetask.cancel(true);
                        wavetask = null;
                    }
                    next = false;
                }
            }
        }, 1000);

    }

    public void startWaveNettPyramid() {
        nextWave();
        if (monstertask != null) {
            monstertask.cancel(true);
            monstertask = null;
        }
        monstertask = EventTimer.getInstance().register(new Runnable() {
            int time = -1;

            @Override
            public void run() {
                time++;
                if (time >= 0) {
                    int mobid = getMonsters().get(time);
                    spawnMonsters(mobid);
                }
                if (time == getMonsters().size() - 1) {
                    if (monstertask != null) {
                        monstertask.cancel(true);
                        monstertask = null;
                    }
                }
            }
        }, 1000);
    }

    public void nextWave() {
        wave += 1;
        changeWave();
    }

    public void changeWave() {
        if (map != null) {
            map.broadcastMessage(CField.NettPyramidWave(wave));
            if (wave >= 1) {
                map.broadcastMessage(CField.environmentChange("defense/wave/" + wave, 16));
                map.broadcastMessage(CField.environmentChange("killing/first/start", 16));
            }
        }
    }

    public void showCount() {
        if (map != null) {
            map.broadcastMessage(CField.environmentChange("defense/count", 16));
        }
    }

    public void minusLife(int objectId) {
        MapleMonster mob = map.getMonsterByOid(objectId);
        if (mob != null) {
            map.killMonster(mob);
        }
        life -= 1;
        if (life < 0) {
            life = 0;
        }
        changeLife();
        if (life == 0) {
            waveFail();
        }
    }

    public void waveFail() {
        map.killAllMonsters(true);
        next = true;
        for (MapleNettPyramidMember mnpm : this.getMembers()) {
            mnpm.getChr().getClient().getSession().writeAndFlush(CField.environmentChange("killing/fail", 16));
            EventTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    mnpm.getChr().getClient().getSession().writeAndFlush(CField.NettPyramidClear(false, wave, life, mnpm.getPoint(), mnpm.getExp()));
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            endWave();
                            mnpm.getChr().changeMap(mnpm.getChr().getWarpMap(926010001));
                        }
                    }, 7000);
                }
            }, 2000);
        }
    }

    public void changeLife() {
        if (map != null) {
            map.broadcastMessage(CField.NettPyramidLife(life));
        }
    }

    public void spawnMonsters(int mid) {
        int level = getLevel(map);
        long hp = getHp(mid, level);
        int exp = getExp(level);
        map.spawnMonsterWithEffect(map.makePyramidMonster(MapleLifeFactory.getMonster(mid), hp, level, exp), -1, point1);
        map.spawnMonsterWithEffect(map.makePyramidMonster(MapleLifeFactory.getMonster(mid), hp, level, exp), -1, point3);
        if (wave != 20) {
            map.spawnMonsterWithEffect(map.makePyramidMonster(MapleLifeFactory.getMonster(mid), hp, level, exp), -1, point2);
            map.spawnMonsterWithEffect(map.makePyramidMonster(MapleLifeFactory.getMonster(mid), hp, level, exp), -1, point4);
        }
    }

    public long getHp(int mid, int level) {
        int id = mid - 9305400;
        int plus = Math.max(1, level - 240);
        if (hard) {
            return hardhp[id] * plus * ((4 + wave) / 4);
        } else {
            return easyhp[id] * plus * ((4 + wave) / 4);
        }
    }

    public int getLevel(MapleMap map) {
        int total = 0;
        if (map.getAllCharactersThreadsafe().size() == 0) {
            return 0;
        }
        for (MapleNettPyramidMember mnpm : this.getMembers()) {
            total += mnpm.getChr().getLevel();
        }
        return total / map.getAllCharactersThreadsafe().size();
    }

    public int getExp(int level) {
        double exp = (GameConstants.getExpNeededForLevel(level) * 0.0001 * wave / 10000);
        if (hard) {
            return (int) exp;
        } else {
            return (int) (exp * 0.01);
        }
    }

    public void check() {
        if (map.getAllMonster().size() == 0 && monstertask == null && wave > 0 && wave < 21 && !next) {
            waveClear();
            next = true;
            if (wave < 20) {
                startNettPyramid();
            }
        }
    }

    public List<Integer> getMonsters() {
        Map<Integer, List<Integer>> info = map.getmonsterDefense();
        if (info == null || info.get(wave) == null) {
            System.out.println("NULL");
            return null;
        }
        return info.get(wave);
    }

    public void waveClear() {
        for (MapleNettPyramidMember mnpm : this.getMembers()) {
            if (wave == 20) {
                mnpm.getChr().getClient().getSession().writeAndFlush(CField.environmentChange("killing/clear", 16));
                EventTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        mnpm.getChr().getClient().getSession().writeAndFlush(CField.NettPyramidClear(true, wave, life, mnpm.getPoint(), mnpm.getExp()));
                        EventTimer.getInstance().schedule(new Runnable() {
                            @Override
                            public void run() {
                                endWave();
                                mnpm.getChr().changeMap(mnpm.getChr().getWarpMap(hard ? 926010002 : 926010003));
                            }
                        }, 7000);
                    }
                }, 2000);
            } else {
                mnpm.getChr().getClient().getSession().writeAndFlush(CWvsContext.getTopMsg("WAVE를 막아냈습니다. 다음 WAVE를 준비해주세요."));
            }
        }
    }

    public void plusPoint(MapleCharacter chr, int point) {
        for (MapleNettPyramidMember mnpm : getMembers()) {
            if (mnpm.getChr().equals(chr)) {
                mnpm.plusPoint(point);
                break;
            }
        }
    }

    public void minusPoint(MapleCharacter chr, int point) {
        for (MapleNettPyramidMember mnpm : getMembers()) {
            if (mnpm.getChr().equals(chr)) {
                mnpm.minusPoint(point);
                break;
            }
        }
    }

    public int getPoint(MapleCharacter chr) {
        for (MapleNettPyramidMember mnpm : getMembers()) {
            if (mnpm.getChr().equals(chr)) {
                return mnpm.getPoint();
            }
        }
        return 0;
    }

    public void plusExp(MapleCharacter chr, int exp) {
        for (MapleNettPyramidMember mnpm : getMembers()) {
            if (mnpm.getChr().equals(chr)) {
                mnpm.plusExp(exp);
                break;
            }
        }
    }

    public void endWave() {
        for (MapleNettPyramidMember mnpm : getMembers()) {
            mnpm.getChr().setNettPyramid(null);
            mnpm.getChr().getClient().getSession().writeAndFlush(CField.UIPacket.closeUI(62));
        }
    }

    public void useSkill(MapleCharacter chr, int sid) {
        int usePoint = 0;
        if (sid == 2800014) { // 아누비스
            usePoint = 500;
            if (getPoint(chr) >= usePoint) {
                SkillFactory.getSkill(80001104).getEffect(1).applyTo(chr);
            } else {
                usePoint = -1;
            }
        } else if (sid == 2800017) { // 오시리스
            usePoint = 500;
            if (getPoint(chr) >= usePoint) {
                for (MapleMonster mob : map.getAllMonster()) {
                    //스턴
                }
            } else {
                usePoint = -1;
            }
        } else if (sid == 2800016) { // 이시스
            usePoint = 500;
            for (MapleMonster mob : map.getAllMonster()) {
                //독
            }
        } else if (sid == 2800015) { // 호루스
            usePoint = 700;
            if (getPoint(chr) >= usePoint) {
                for (MapleMonster mob : map.getAllMonster()) {
                    //속도
                }
            } else {
                usePoint = -1;
            }
        } else if (sid == 2800019) { // 라
            usePoint = 2000;
        }
        if (usePoint == -1) {
            chr.dropMessage(5, "포인트가 부족합니다.");
        } else if (usePoint == 0) {
            chr.dropMessage(5, "사용 할 수 없는 스킬입니다.");
        } else {
            minusPoint(chr, usePoint);
        }
    }

    public boolean isHard() {
        return hard;
    }

    public List<MapleNettPyramidMember> getMembers() {
        return members;
    }

    public void setMembers(List<MapleNettPyramidMember> members) {
        this.members = members;
    }

    public static class MapleNettPyramidMember {

        private MapleCharacter chr;
        private int point = 0, exp = 0;

        public MapleNettPyramidMember(final MapleCharacter chr) {
            this.chr = chr;
        }

        public MapleCharacter getChr() {
            return this.chr;
        }

        public int getCid() {
            return this.chr.getId();
        }

        public void plusPoint(final int point) {
            this.point += point;
            changePoint();
        }

        public void minusPoint(final int point) {
            if (getPoint() > 0) {
                this.point -= point;
                changePoint();
            }
        }

        public int getPoint() {
            return this.point;
        }

        public void changePoint() {
            getChr().getClient().getSession().writeAndFlush(CField.NettPyramidPoint(point));
        }

        public void plusExp(final int exp) {
            this.exp += exp;
        }

        public int getExp() {
            return this.exp;
        }

    }
}
