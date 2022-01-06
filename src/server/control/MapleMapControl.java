package server.control;

import handling.channel.ChannelServer;
import server.Randomizer;
import server.SkillCustomInfo;
import server.Timer;
import server.field.boss.demian.MapleIncinerateObject;
import server.field.boss.lucid.Butterfly;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleRune;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.MobPacket;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MapleMapControl implements Runnable {

    private boolean isfirst;
    private int numTimes = 0;

    public MapleMapControl() {
        this.isfirst = false;
        System.out.println("[Loading Completed] Start MapControl");
    }

    @Override
    public void run() {
        numTimes++;
        long time = System.currentTimeMillis();

        Iterator<ChannelServer> css = ChannelServer.getAllInstances().iterator();
        try {
            while (css.hasNext()) {
                ChannelServer cs = css.next();
                if (!cs.hasFinishedShutdown()) {

                    Iterator<MapleMap> maps = cs.getMapFactory().getAllLoadedMaps().iterator();

                    while (maps.hasNext()) {

                        MapleMap map = maps.next();

                        if (!map.isTown() && (map.getCharactersThreadsafe().size() > 0 || map.getId() == 931000500)) { //jaira hack
                            boolean hurt = map.canHurt(time);
//                                  if (map.canSpawn(time)) {
                            map.respawn(false, time);
//                                   }
                        }

                        if (map.getAllItemsThreadsafe().size() > 0) {
                            Iterator<MapleMapItem> items = map.getAllItemsThreadsafe().iterator();
                            while (items.hasNext()) {
                                MapleMapItem item = items.next();
                                if (item.shouldExpire(time)) {
                                    item.expire(map);
                                } else if (item.shouldFFA(time)) {
                                    item.setDropType((byte) 2);
                                }
                            }
                        }

                        Iterator<MapleMonster> lifes = map.getAllMonstersThreadsafe().iterator();
                        while (lifes.hasNext()) {
                            MapleMonster life = lifes.next();
                            if (life.getCustomValues().size() > 0) {
                                Map<Integer, SkillCustomInfo> customInfo = new LinkedHashMap<>();
                                customInfo.putAll(life.getCustomValues());
                                for (Map.Entry<Integer, SkillCustomInfo> sci : customInfo.entrySet()) {
                                    if (((SkillCustomInfo) sci.getValue()).canCancel(System.currentTimeMillis())) {
                                        life.removeCustomInfo(((Integer) sci.getKey()).intValue());
                                    }
                                }
                            }
                        }

                        if (map.getAllNormalMonstersThreadsafe().size() > 0 && !map.isTown()) {
                            if (map.getBurningIncreasetime() == 0) {
                                map.setBurningIncreasetime(System.currentTimeMillis());
                            }

                            if (map.getAllCharactersThreadsafe().size() == 0) {
                                if (map.getBurning() <= 10) {
                                    if (time - map.getBurningIncreasetime() > 600 * 1000) {
                                        map.setBurningIncreasetime(time);
                                        if (map.getBurning() < 10) {
                                            map.setBurning(map.getBurning() + 1);
                                        }
                                    }
                                }
                            } else {
                                if (map.getBurning() > 0) {
                                    if (time - map.getBurningIncreasetime() > 600 * 1000) {
                                        map.setBurningIncreasetime(time);
                                        map.setBurning(map.getBurning() - 1);
                                        if (map.getBurning() > 0) {
                                            map.broadcastMessage(EffectPacket.showBurningFieldEffect("#fn나눔고딕 ExtraBold##fs26#    버닝 " + map.getBurning() + "단계 : 경험치 " + (map.getBurning() * 10) + "% 추가지급!"));
                                        } else {
                                            map.broadcastMessage(EffectPacket.showBurningFieldEffect("#fn나눔고딕 ExtraBold##fs26#  버닝필드 소멸!"));
                                        }
                                    }
                                }
                            }
                        }

                        if (map.getRune() != null && map.getRuneCurse() < 4) {
                            MapleRune rune = map.getRune(); // 맵에 룬은 단 1개
                            if (time - rune.getCreateTimeMills() >= 1000 * 60 * 30) {
                                map.setRuneCurse(map.getRuneCurse() + 1);
                                rune.setCreateTimeMills(time);
                                map.broadcastMessage(CField.runeCurse("룬을 해방하여 엘리트 보스의 저주를 풀어야 합니다!!\\n저주 " + map.getRuneCurse()+ "단계 :  경험치 획득, 드롭률 " + map.getRuneCurseDecrease() + "% 감소 효과 적용 중", false));
                            }
                        }

                        int[] demians = {8880100, 8880110, 8880101, 8880111};

                        MapleMonster demian = null;

                        for (int id : demians) {
                            demian = map.getMonsterById(id);
                            if (demian != null) {
                                if (time - map.lastIncinerateTime >= 30 * 1000) { // 값이 정확히 나와있지 않음
                                    if (map.lastIncinerateTime != 0) {
                                        map.spawnIncinerateObject(new MapleIncinerateObject(Randomizer.nextInt(map.getRight() - map.getLeft()) + map.getLeft(), 16));
                                    }
                                    map.lastIncinerateTime = time;
                                }
                                break;
                            }
                        }

                        if (map.getId() == 450008350 || map.getId() == 450008950) {

                            MapleMonster will = map.getMonsterById(8880302);
                            if (will == null) {
                                will = map.getMonsterById(8880342);
                            }

                            if (will != null) {
                                if (will.getLastSkillUsed(242, 13) == 0) {
                                    MobSkill web = MobSkillFactory.getMobSkill(242, 13);
                                    will.setLastSkillUsed(web, time, web.getInterval());
                                    web.applyEffect(null, will, true, will.isFacingLeft());
                                }
                            }
                        }

                        if (map.getId() >= 105200310 && map.getId() <= 105200319 || map.getId() >= 105200710 && map.getId() <= 105200719) {
                            for (MapleMonster m : map.getAllMonstersThreadsafe()) {
                                if (m == null) {
                                    continue;
                                }
                                if (m.isAlive()) {
                                    if (m.getId() == 8920004 || m.getId() == 8920104) {
                                        if (System.currentTimeMillis() - m.getSpawnTime() >= 9500) {
                                            map.broadcastMessage(MobPacket.BossBlodyQueen.killMonster(m.getObjectId()));
                                            map.removeMapObject(m);
                                            m.killed();
                                        }
                                    }
                                }
                            }
                        }

                        if (map.getId() >= 105200210 && map.getId() <= 105200219 || map.getId() >= 105200610 && map.getId() <= 105200619) {
                            for (MapleMonster m : map.getAllMonstersThreadsafe()) {
                                if (m == null) {
                                    continue;
                                }
                                if (m.isAlive()) {
                                    if (m.getId() >= 8900000 && m.getId() <= 8900002 || m.getId() >= 8900100 && m.getId() <= 8900102) {
                                        if (time - map.lastCapDropTime >= 15000) {
                                            List<Point> pos = new ArrayList<>();
                                            int cout = Randomizer.rand(6, 10);

                                            for (int i = 0; i < cout; i++) {
                                                pos.add(new Point(Randomizer.rand(0, 1600), 551));
                                            }
                                            map.lastCapDropTime = time;
                                            map.broadcastMessage(MobPacket.dropStone("CapEffect", pos));
                                            break;
                                        }
                                    }
                                }
                            }

                        }

                        if (map.getId() == 450004450 || map.getId() == 450004150) {
                            if (map.getAllCharactersThreadsafe().size() <= 0) {
                                map.lucidButterflyReSpawnGage = 5;
                                map.lucidButterflyCount = 0;
                                // map.broadcastMessage(MobPacket.BossLucid.setButterflyAction(Butterfly.Mode.ERASE,0));
                            } else {
                                for (MapleMonster m : map.getAllMonstersThreadsafe()) {
                                    if (m == null) {
                                        continue;
                                    }
                                    if (m.isAlive()) {
                                        if (m.getId() == 8880140 || m.getId() == 8880141) {
                                            if (m.getHPPercent() <= 25 && map.lucidButterflyReSpawnGage > 4) {
                                                map.lucidButterflyReSpawnGage = 4;
                                                map.broadcastMessage(CField.enforceMSG("루시드가 분노한 것 같습니다!", 222, 2000));
                                            } else if (m.getHPPercent() <= 50 && map.lucidButterflyReSpawnGage > 3) {
                                                map.lucidButterflyReSpawnGage = 3;
                                                map.broadcastMessage(CField.enforceMSG("루시드가 더 강한 힘을 발휘할 겁니다!", 222, 2000));
                                            } else if (m.getHPPercent() <= 75 && map.lucidButterflyReSpawnGage > 2) {
                                                map.lucidButterflyReSpawnGage = 2;
                                                map.broadcastMessage(CField.enforceMSG("루시드가 힘을 이끌어내고 있습니다!", 222, 2000));
                                            }
                                        }
                                        if (m.getId() == 8880165 || m.getId() == 8880167 || m.getId() == 8880168 || m.getId() == 8880169 || m.getId() == 8880164 || m.getId() == 8880184 || m.getId() == 8880185 || m.getId() == 8880157) { //나비, 독버섯
                                            if (time - m.getSpawnTime() >= 20000) {
                                                map.broadcastMessage(MobPacket.BossBlodyQueen.killMonster(m.getObjectId()));
                                                map.removeMapObject(m);
                                                m.killed();
                                            }
                                        }

                                    }
                                }
                                if (time - map.getLastButterFlyTime() >= map.lucidButterflyReSpawnGage * 1000) {
                                    if (map.lucidButterflyCount % 30 == 0) {
                                        map.broadcastMessage(CField.enforceMSG("꿈이 강해지고 있습니다. 조심하세요!", 222, 2000));
                                    }
                                    if (map.lucidButterflyCount >= 40) {
                                        map.broadcastMessage(MobPacket.BossLucid.setButterflyAction(Butterfly.Mode.ATTACK, 3, 1));
                                        map.lucidButterflyCount = 0;
                                    } else {
                                        map.setLastButterFlyTime(time);
                                        List<Butterfly> b = new ArrayList<>();
                                        b.add(new Butterfly(Randomizer.rand(1, 8), true, Randomizer.rand(0, 39)));
                                        map.lucidButterflyCount += 1;
                                        map.broadcastMessage(MobPacket.BossLucid.createButterfly(1, b));
                                    }
                                }
                            }

                        } else if (map.getId() == 450004250 || map.getId() == 450004550) {
                            if (map.getAllCharactersThreadsafe().size() <= 0) {
                                map.lucidButterflyReSpawnGage = 5;
                                map.lucidButterflyCount = 0;
                                //    map.broadcastMessage(MobPacket.BossLucid.setButterflyAction(Butterfly.Mode.ERASE,0));
                            } else {
                                for (MapleMonster m : map.getAllMonstersThreadsafe()) {
                                    if (m == null) {
                                        continue;
                                    }
                                    if (m.isAlive()) {
                                        if (m.getId() == 8880140 || m.getId() == 8880141) {
                                            if (m.getHPPercent() <= 25 && map.lucidButterflyReSpawnGage > 4) {
                                                map.lucidButterflyReSpawnGage = 4;
                                                map.broadcastMessage(CField.enforceMSG("루시드가 분노한 것 같습니다!", 222, 2000));
                                            } else if (m.getHPPercent() <= 50 && map.lucidButterflyReSpawnGage > 3) {
                                                map.lucidButterflyReSpawnGage = 3;
                                                map.broadcastMessage(CField.enforceMSG("루시드가 더 강한 힘을 발휘할 겁니다!", 222, 2000));
                                            } else if (m.getHPPercent() <= 75 && map.lucidButterflyReSpawnGage > 2) {
                                                map.lucidButterflyReSpawnGage = 2;
                                                map.broadcastMessage(CField.enforceMSG("루시드가 힘을 이끌어내고 있습니다!", 222, 2000));
                                            }
                                        }
                                        if (m.getId() == 8880175 || m.getId() == 8880178 || m.getId() == 8880179) { //나비, 독버섯
                                            if (time - m.getSpawnTime() >= 20000) {
                                                map.broadcastMessage(MobPacket.BossBlodyQueen.killMonster(m.getObjectId()));
                                                map.removeMapObject(m);
                                                m.killed();
                                            }
                                        }

                                    }
                                }
                            }
                            if (time - map.getLastButterFlyTime() >= map.lucidButterflyReSpawnGage * 1000) {
                                if (map.lucidButterflyCount % 30 == 0) {
                                    map.broadcastMessage(CField.enforceMSG("꿈이 강해지고 있습니다. 조심하세요!", 222, 2000));
                                }
                                map.setLastButterFlyTime(time);
                                List<Butterfly> b = new ArrayList<>();
                                b.add(new Butterfly(Randomizer.rand(1, 8), false, Randomizer.rand(0, 39)));
                                map.lucidButterflyCount += 1;
                                map.broadcastMessage(MobPacket.BossLucid.createButterfly(1, b));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
