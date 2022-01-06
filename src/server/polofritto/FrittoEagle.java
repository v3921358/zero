/**
 * @package : server.poloFritto
 * @author : Yein
 * @fileName : FrittoEagle.java
 * @date : 2019. 12. 1.
 */
package server.polofritto;

import client.MapleClient;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.packet.CField;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;

public class FrittoEagle {

    private ScheduledFuture<?> sc, sch;
    private int score, bullet;

    public FrittoEagle(int score, int bullet) {
        this.score = score;
        this.bullet = bullet;
    }

    public void createGun(MapleClient c) {
        c.getSession().writeAndFlush(SLFCGPacket.createGun());
        c.getSession().writeAndFlush(SLFCGPacket.setGun());
        c.getSession().writeAndFlush(SLFCGPacket.setAmmo(bullet));
    }

    public void checkFinish(MapleClient c) {
        sch = EventTimer.getInstance().register(new Runnable() {

            public void run() {
                if (c != null && c.getPlayer() != null && c.getPlayer().getMap() != null) {
                    if (score >= 1000 || c.getPlayer().getMap().getAllMonstersThreadsafe().size() == 0) {
                        if (sc != null) {
                            sc.cancel(false);
                        }
                        if (sch != null) {
                            sch.cancel(false);
                        }

                        c.getSession().writeAndFlush(CField.environmentChange("killing/clear", 16));

                        EventTimer.getInstance().schedule(new Runnable() {
                            public void run() {
                                if (c != null && c.getPlayer() != null) {
                                    c.getSession().writeAndFlush(SLFCGPacket.SetIngameDirectionMode(false, true, false, false));
                                    c.getSession().writeAndFlush(SLFCGPacket.InGameDirectionEvent("", 0xA, 0x0));
                                    c.getPlayer().warp(993000601);
                                }
                            }
                        }, 2000);
                    }
                }
            }
        }, 1000);
    }

    public void addScore(MapleMonster monster, MapleClient c) {
        switch (monster.getId()) {
            case 9833000:
                score += 50;
                break;
            case 9833001:
                score += 100;
                break;
            case 9833002:
                score += 200;
                break;
            case 9833003:
                score -= 50;
                break;
        }
        c.getPlayer().setKeyValue(15141, "point", String.valueOf(score));
        c.getSession().writeAndFlush(SLFCGPacket.deadOnFPSMode(monster.getObjectId(), score));
    }

    public void updateNewWave(MapleClient c) {
        c.getSession().writeAndFlush(CField.environmentChange("killing/first/start", 16));

        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833000), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833000), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833000), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833000), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833000), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833000), new Point(0, 0));

        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833001), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833001), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833001), new Point(0, 0));

        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833002), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833002), new Point(0, 0));

        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833003), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833003), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833003), new Point(0, 0));
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9833003), new Point(0, 0));

        EventTimer.getInstance().schedule(new Runnable() {

            public void run() {
                checkFinish(c);
            }
        }, 2000);

    }

    public void shootResult(MapleClient c) {
        if (bullet > 1) {
            c.getSession().writeAndFlush(SLFCGPacket.attackRes());
        } else {
            if (sc != null) {
                sc.cancel(false);
            }
            if (sch != null) {
                sch.cancel(false);
            }
            c.getSession().writeAndFlush(SLFCGPacket.SetIngameDirectionMode(false, true, false, false));
            c.getSession().writeAndFlush(SLFCGPacket.InGameDirectionEvent("", 0xA, 0x0));
            c.getPlayer().warp(993000601);
        }
    }

    public void start(MapleClient c) {

        createGun(c);

        c.getPlayer().setKeyValue(15141, "point", "0");
        c.getSession().writeAndFlush(SLFCGPacket.SetIngameDirectionMode(true, false, false, false));
        c.getSession().writeAndFlush(SLFCGPacket.InGameDirectionEvent("", 0xA, 0x1));
        c.getSession().writeAndFlush(CField.environmentChange("PoloFritto/msg1", 20));
        c.getSession().writeAndFlush(CField.startMapEffect("독수리를 침착하게 한 마리씩 잡도록 해! 아참, 대머리 독수리는 아무 쓸모 없으니 잡지마!", 5120160, true));
        c.getSession().writeAndFlush(CField.getClock(30));
        updateNewWave(c);

        sc = EventTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (c.getPlayer().getMapId() == 993000200) {
                    c.getSession().writeAndFlush(SLFCGPacket.SetIngameDirectionMode(false, true, false, false));
                    c.getSession().writeAndFlush(SLFCGPacket.InGameDirectionEvent("", 0xA, 0x0));
                    c.getPlayer().warp(993000601);
                }
            }
        }, 30 * 1000);
    }

    public ScheduledFuture<?> getSch() {
        return sch;
    }

    public void setSch(ScheduledFuture<?> sch) {
        this.sch = sch;
    }

    public ScheduledFuture<?> getSc() {
        return sc;
    }

    public void setSc(ScheduledFuture<?> sc) {
        this.sc = sc;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBullet() {
        return bullet;
    }

    public void setBullet(int bullet) {
        this.bullet = bullet;
    }
}
