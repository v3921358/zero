package server.field.boss;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.SkillFactory;
import client.status.MonsterStatus;
import server.Obstacle;
import server.Randomizer;
import server.Timer.MobTimer;
import server.field.FieldSkill;
import server.field.FieldSkillFactory;
import server.field.boss.demian.MapleFlyingSword;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleNodes;
import tools.Pair;
import tools.packet.CField;
import tools.packet.MobPacket;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class MapleBossManager {

    public static void changePhase(MapleMonster monster) {
        if (monster.getId() >= 8800102 && monster.getId() <= 8800110) {
            int[] arms = {8800103, 8800104, 8800105, 8800106, 8800107, 8800108, 8800109, 8800110};
            if (monster.getPhase() == 0) {
                monster.setPhase((byte) 1);
            }
            if (monster.getId() == 8800102) {
                boolean nextPhase = true;
                for (int arm : arms) {
                    if (monster.getMap().getMonsterById(arm) != null) {
                        nextPhase = false;
                        break;
                    }
                }
                if (nextPhase) {
                    monster.setPhase((byte) 3);
                }
                if (monster.getHPPercent() <= 20) {
                    monster.setPhase((byte) 4);
                    for (int arm : arms) {
                        monster.getMap().killMonster(arm);
                    }
                }
            }
            monster.getMap().broadcastMessage(MobPacket.changePhase(monster));
        } else if (monster.getId() == 8880000 || monster.getId() == 8880002 || monster.getId() == 8880010) {
            byte phase;
            if (monster.getHPPercent() <= 25) {
                phase = 4;
            } else if (monster.getHPPercent() <= 50) {
                phase = 3;
            } else if (monster.getHPPercent() <= 75) {
                phase = 2;
            } else {
                phase = 1;
            }
            monster.setPhase(phase);
            monster.getMap().broadcastMessage(MobPacket.changePhase(monster));
        }
    }

    public static void setBlockAttack(MapleMonster monster) {
        List<Integer> blocks = new ArrayList<>();
        switch (monster.getId()) {
            case 8800102: {
                if (monster.getPhase() != 2) {
                    monster.getMap().killMonster(8800117);
                    List<String> updateLists = new ArrayList<>();
                    monster.getMap().updateEnvironment(updateLists);
                    blocks.add(1);
                }
                if (monster.getPhase() != 3) {
                    blocks.add(2);
                    blocks.add(3);
                    blocks.add(4);
                    blocks.add(5);
                }
                if (monster.getPhase() != 4) {
                    blocks.add(6);
                    blocks.add(7);
                    blocks.add(8);
                }
                break;
            }
            case 8850011:
            case 8850111: {
                blocks.add(4);
                break;
            }
            case 8910000:
            case 8910100: {
                if (monster.getHPPercent() > 10) {
                    blocks.add(3);
                    blocks.add(4);
                    blocks.add(5);
                    blocks.add(6);
                    blocks.add(7);
                }
                if (monster.getHPPercent() > 70) {
                    blocks.add(2);
                }
                break;
            }
            case 8930000:
            case 8930100: {
                if (monster.getId() == 8930100 && monster.getHPPercent() > 70) {
                    blocks.add(2);
                    blocks.add(3);
                    blocks.add(4);
                }
                if (monster.getHPPercent() > 40) {
                    blocks.add(8);
                    blocks.add(9);
                    blocks.add(10);
                    blocks.add(12);
                    blocks.add(13);
                    blocks.add(14);
                    blocks.add(15);
                }
                break;
            }
            case 8880300:
            case 8880301:
            case 8880303:
            case 8880304:
            case 8880321:
            case 8880322:
            case 8880325:
            case 8880326:
            case 8880340:
            case 8880341:
            case 8880343:
            case 8880344:
            case 8880351:
            case 8880352:
            case 8880355:
            case 8880356:
                return;
        }
        monster.getMap().broadcastMessage(MobPacket.BlockAttack(monster, blocks));
    }

    public static void ZakumBodyHandler(MapleMonster monster, MapleCharacter chr, boolean facingLeft) {
        long time = System.currentTimeMillis();
        List<MobSkill> useableSkills = new ArrayList<>();
        for (MobSkill msi : monster.getSkills()) {
            if ((time - monster.getLastSkillUsed(msi.getSkillId(), msi.getSkillLevel())) >= 0) {
                if (monster.getHPPercent() <= msi.getHP() && !msi.isOnlyOtherSkill()) {
                    if (msi.isOnlyFsm()) {
                        if (monster.getPhase() == 2) {
                            if (msi.getSkillId() == 201 && msi.getSkillLevel() == 162) {
                                useableSkills.add(msi);
                            }
                        } else if (monster.getPhase() == 3) {
                            if (msi.getSkillId() == 176 && msi.getSkillLevel() == 27) {
                                if (msi.getSkillId() != 201 && msi.getSkillLevel() != 162) {
                                    useableSkills.add(msi);
                                }
                            }
                        }
                    } else {
                        useableSkills.add(msi);
                    }
                }
            }
        }

        if (!useableSkills.isEmpty()) {
            MobSkill msi = useableSkills.get(Randomizer.nextInt(useableSkills.size()));
            monster.setLastSkillUsed(msi, time, msi.getInterval());
            monster.setNextSkill(msi.getSkillId());
            monster.setNextSkillLvl(msi.getSkillLevel());
        }
    }

    public static void ZakumArmHandler(MapleMonster monster, MapleCharacter chr, short moveid, boolean movingAttack, boolean facingLeft) {
        long time = System.currentTimeMillis();
        List<MobSkill> useableSkills = new ArrayList<>();
        for (MobSkill msi : monster.getSkills()) {
            if ((time - monster.getLastSkillUsed(msi.getSkillId(), msi.getSkillLevel())) >= 0) {
                if (monster.getHPPercent() <= msi.getHP() && !msi.isOnlyOtherSkill()) {
                    if (msi.isOnlyFsm()) {
                        if (monster.getPhase() == 1) {
                            if (msi.getSkillId() == 176 && (msi.getSkillLevel() == 25 || msi.getSkillLevel() == 26)) {
                                useableSkills.add(msi);
                            }
                        } else if (monster.getPhase() == 2) {
                            if (msi.getSkillId() == 176 && msi.getSkillLevel() == 27) {
                                if (monster.getId() <= 8800106) {
                                    if (monster.getMap().getMonsterById(monster.getId() + 4) != null) { // 반대편 팔도 존재해야 합창하지 ㅡ.ㅡ;
                                        useableSkills.add(msi);
                                    }
                                } else {
                                    if (monster.getMap().getMonsterById(monster.getId() - 4) != null) { // 반대편 팔도 존재해야 합창하지 ㅡ.ㅡ;
                                        useableSkills.add(msi);
                                    }
                                }
                            }
                        }
                    } else {
                        useableSkills.add(msi);
                    }
                }
            }
        }

        if (!useableSkills.isEmpty()) {
            MobSkill msi = useableSkills.get(Randomizer.nextInt(useableSkills.size()));

            List<MapleMonster> nextattackMobs = new ArrayList<>();

            if (msi.getSkillId() == 176) {
                if (msi.getSkillLevel() == 25) {
                    for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
                        if (mob.getId() >= 8800103 && mob.getId() <= 8800110) { // 찍는거 한꺼번에 하고, 한꺼번에 쿨타임 주기
                            if (((mob.getId() != monster.getId() && Randomizer.isSuccess(25)) || mob.getId() == monster.getId()) && nextattackMobs.size() < 3) {
                                nextattackMobs.add(mob);
                                mob.setNextSkill(msi.getSkillId());
                                mob.setNextSkillLvl(msi.getSkillLevel());
                            }
                            mob.setLastSkillUsed(msi, time, 5000);
                            chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(mob.getObjectId(), moveid, mob.getMp(), movingAttack, mob.getNextSkill(), mob.getNextSkillLvl(), 0));
                        }
                    }
                } else if (msi.getSkillLevel() == 26) {
                    for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
                        if (mob.getId() >= 8800103 && mob.getId() <= 8800110) { // 찍는거 한꺼번에 하고, 한꺼번에 쿨타임 주기
                            nextattackMobs.add(mob);
                            mob.setNextSkill(msi.getSkillId());
                            mob.setNextSkillLvl(msi.getSkillLevel());
                            if (((mob.getId() != monster.getId() && Randomizer.isSuccess(75)) || mob.getId() == monster.getId()) && nextattackMobs.size() > 4) {
                                nextattackMobs.remove(mob);
                                mob.setNextSkill(0);
                                mob.setNextSkillLvl(0);
                            }
                            mob.setLastSkillUsed(msi, time, 10000);
                            chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(mob.getObjectId(), moveid, mob.getMp(), movingAttack, mob.getNextSkill(), mob.getNextSkillLvl(), 0));
                        }
                    }
                } else if (msi.getSkillLevel() == 27) {
                    for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
                        if (mob.getId() >= 8800103 && mob.getId() <= 8800110) { // 찍는거 한꺼번에 하고, 한꺼번에 쿨타임 주기
                            if ((mob.getId() - 4 == monster.getId()) || (mob.getId() + 4 == monster.getId()) || monster.getId() == mob.getId()) {
                                mob.setNextSkill(msi.getSkillId());
                                mob.setNextSkillLvl(msi.getSkillLevel());
                            } else {
                                mob.setNextSkill(0);
                                mob.setNextSkillLvl(0);
                            }
                            mob.setLastSkillUsed(msi, time, 7500);

                            chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(mob.getObjectId(), moveid, mob.getMp(), movingAttack, mob.getNextSkill(), mob.getNextSkillLvl(), 0));
                        }
                    }
                }
            } else {
                monster.setLastSkillUsed(msi, time, msi.getInterval());
                monster.setNextSkill(msi.getSkillId());
                monster.setNextSkillLvl(msi.getSkillLevel());
                chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, monster.getMp(), movingAttack, monster.getNextSkill(), monster.getNextSkillLvl(), 0));
            }
        } else {
            chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, monster.getMp(), movingAttack, monster.getNextSkill(), monster.getNextSkillLvl(), 0));
        }
    }

//    public static void ZakumArmHandler(MapleMonster monster, MapleCharacter chr, short moveid, boolean movingAttack, boolean facingLeft) {
//        long time = System.currentTimeMillis();
//        List<MobSkill> useableSkills = new ArrayList<>();
//        for (MobSkill msi : monster.getSkills()) {
//            if ((time - monster.getLastSkillUsed(msi.getSkillId(), msi.getSkillLevel())) >= 0) {
//                if (monster.getHPPercent() <= msi.getHP() && !msi.isOnlyOtherSkill()) {
//                    if (msi.isOnlyFsm()) {
//                        if (monster.getPhase() == 1) {
//                            if (msi.getSkillId() == 176 && (msi.getSkillLevel() == 25 || msi.getSkillLevel() == 26)) {
//                                useableSkills.add(msi);
//                            }
//                        } else if (monster.getPhase() == 2) {
//                            if (msi.getSkillId() == 176 && msi.getSkillLevel() == 27) {
//                                if (monster.getId() <= 8800106) {
//                                    if (monster.getMap().getMonsterById(monster.getId() + 4) != null) { // 반대편 팔도 존재해야 합창하지 ㅡ.ㅡ;
//                                        useableSkills.add(msi);
//                                    }
//                                } else {
//                                    if (monster.getMap().getMonsterById(monster.getId() - 4) != null) { // 반대편 팔도 존재해야 합창하지 ㅡ.ㅡ;
//                                        useableSkills.add(msi);
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        useableSkills.add(msi);
//                    }
//                }
//            }
//        }
//
//        if (!useableSkills.isEmpty()) {
//            MobSkill msi = useableSkills.get(Randomizer.nextInt(useableSkills.size()));
//
//            List<MapleMonster> nextattackMobs = new ArrayList<>();
//
//            if (msi.getSkillId() == 176) {
//                switch (msi.getSkillLevel()) {
//                    case 25: { // 짧게 찍기
//                        for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
//                            if (mob.getId() >= 8800103 && mob.getId() <= 8800110) { // 쿨타임은 8마리 전부에게, 찍는건 그 중 일부만
//                                if ((mob.getId() == monster.getId() || Randomizer.isSuccess(25)) && nextattackMobs.size() < 3) {
//                                    nextattackMobs.add(mob);
//                                    mob.setNextSkill(msi.getSkillId());
//                                    mob.setNextSkillLvl(msi.getSkillLevel());
//                                }
//                                mob.setLastSkillUsed(MobSkillFactory.getMobSkill(176, 25), time, 3500);
//                                mob.setLastSkillUsed(MobSkillFactory.getMobSkill(176, 26), time, 3500);
//                                chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(mob.getObjectId(), moveid, mob.getMp(), movingAttack, mob.getNextSkill(), mob.getNextSkillLvl(), 0));
//                            }
//                        }
//                        break;
//                    }
//                    case 26: { // 길게 찍기
//                        for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
//                            if (mob.getId() >= 8800103 && mob.getId() <= 8800110) { // 쿨타임은 8마리 전부에게, 찍는건 그 중 일부만
//                                if ((mob.getId() == monster.getId() || Randomizer.isSuccess(50)) && nextattackMobs.size() < 6) {
//                                    nextattackMobs.add(mob);
//                                    mob.setNextSkill(msi.getSkillId());
//                                    mob.setNextSkillLvl(msi.getSkillLevel());
//                                }
//                                mob.setLastSkillUsed(MobSkillFactory.getMobSkill(176, 25), time, 3500);
//                                mob.setLastSkillUsed(MobSkillFactory.getMobSkill(176, 26), time, 3500);
//                                chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(mob.getObjectId(), moveid, mob.getMp(), movingAttack, mob.getNextSkill(), mob.getNextSkillLvl(), 0));
//                            }
//                        }
//                        break;
//                    }
//                    case 27: { // 합창
//                        monster.setNextSkill(msi.getSkillId());
//                        monster.setNextSkillLvl(msi.getSkillLevel());
//                        for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
//                            if (mob.getId() >= 8800103 && mob.getId() <= 8800110) { // 쿨타임은 8마리 전부에게, 찍는건 그 중 일부만
//                                if ((mob.getId() - 4 == monster.getId()) || (mob.getId() + 4 == monster.getId())) {
//                                    mob.setNextSkill(msi.getSkillId());
//                                    mob.setNextSkillLvl(msi.getSkillLevel());
//                                }
//                                mob.setLastSkillUsed(msi, time, 7500);
//
//                                chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(mob.getObjectId(), moveid, mob.getMp(), movingAttack, mob.getNextSkill(), mob.getNextSkillLvl(), 0));
//                            }
//                        }
//                    }
//                    break;
//                }
//            } else {
//                monster.setLastSkillUsed(msi, time, msi.getInterval());
//                monster.setNextSkill(msi.getSkillId());
//                monster.setNextSkillLvl(msi.getSkillLevel());
//                chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, monster.getMp(), movingAttack, monster.getNextSkill(), monster.getNextSkillLvl(), 0));
//            }
//        } else {
//
//            chr.getMap().broadcastMessage(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, monster.getMp(), movingAttack, monster.getNextSkill(), monster.getNextSkillLvl(), 0));
//        }
//    }

    public static void magnusHandler(MapleMonster monster) {
        if (monster != null) {
            // 1 ~ 5 : 초록, 6 ~ 7 : 파랑, 8 ~ 9 : 보라
            int[] types = {4, 6, 8};
            List<Obstacle> obs = new ArrayList<>();
            int size = Randomizer.rand(4 * monster.getPhase(), 7 * monster.getPhase());

            for (int i = 0; i < size; ++i) {
                int type = types[Randomizer.nextInt(types.length)];
                Obstacle ob;
                int x = Randomizer.rand(550, 3050);
                if (type == 4) {
                    ob = new Obstacle(4, new Point(x, -2000), new Point(x, -1347), 25, 50, 1459, Randomizer.rand(50, 150), 1, 653, 0);
                } else if (type == 8) {
                    ob = new Obstacle(8, new Point(x, -2000), new Point(x, -1347), 65, 100, 542, Randomizer.rand(50, 150), 2, 653, 0);
                } else { // 6
                    ob = new Obstacle(6, new Point(x, -2000), new Point(x, -1347), 45, 100, 1481, Randomizer.rand(50, 150), 1, 653, 0);
                }

                obs.add(ob);
            }

            monster.getMap().broadcastMessage(MobPacket.createObstacle(monster, obs, (byte) 0));
        }
    }

    public static void lotusHandler(MapleMonster monster) {
        if (monster != null) {
            // 48 ~ 52
            int[] types = {48, 49, 50, 51, 52};
            List<Obstacle> obs = new ArrayList<>();
            int aa = monster.getId() % 10;
            int size = Randomizer.rand(1, 3);

            for (int i = 0; i < size; ++i) {
                int type = types[Randomizer.nextInt(aa == 2 ? 5 : aa == 1 ? 4 : 3)];
                Obstacle ob;
                int x = Randomizer.rand(-600, 650);
                if (type == 48) {
                    ob = new Obstacle(48, new Point(x, -520), new Point(x, -16), 36, 10, 157, Randomizer.rand(650, 850), 1, 504, 0);
                } else if (type == 49) {
                    ob = new Obstacle(49, new Point(x, -520), new Point(x, -16), 51, 10, 185, Randomizer.rand(500, 700), 1, 504, 0);
                } else if (type == 50) {
                    ob = new Obstacle(50, new Point(x, -520), new Point(x, -16), 51, 10, 718, Randomizer.rand(500, 700), 2, 504, 0);
                } else if (type == 51) {
                    ob = new Obstacle(51, new Point(x, -520), new Point(x, -16), 65, 20, 797, Randomizer.rand(400, 500), 2, 504, 0);
                } else { // 52
                    ob = new Obstacle(52, new Point(x, -520), new Point(x, -16), 190, 100, 136, Randomizer.rand(300, 350), 1, 504, 0);
                }

                obs.add(ob);
            }

            monster.getMap().broadcastMessage(MobPacket.createObstacle(monster, obs, (byte) 0));
        }
    }

    public static void duskHandler(MapleMonster dusk, MapleMap map) {
        map.broadcastMessage(SLFCGPacket.OnYellowDlg(0, 3000, "촉수가 눈을 방어하고 있어 제대로 된 피해를 주기 힘들겠군.", ""));
        map.broadcastMessage(CField.enforceMSG("점차 공포가 차올라 있을 수 없는 것이 보이게 됩니다! 견디지 못하면 공포가 전이되니 주의하세요!", 250, 3000));
        dusk.setPhase((byte) 1);
        dusk.setLastSpecialAttackTime(System.currentTimeMillis());
        for (MapleCharacter cchr : map.getCharacters()) {
            cchr.setDuskGauge(0);
        }

        dusk.setSchedule(MobTimer.getInstance().register(() -> {
            if (map.getId() == 450009400 || map.getId() == 450009450) {
                long time = System.currentTimeMillis();
                MapleMonster att = null;
                if (map.getId() == 450009400) {
                    att = map.getMonsterById(8644658);
                } else if (map.getId() == 450009450) {
                    att = map.getMonsterById(8644659);
                }
                if (dusk != null) {
                    if (map.getCharactersSize() <= 0) {
                        map.removeMapObject(dusk);
                        dusk.killed();
                    }
                    for (MapleCharacter cchr : map.getCharacters()) {
                        if (cchr.isDuskBlind()) {
                            if (time - cchr.getLastSpawnBlindMobTime() >= 3000) {
                                for (int i = 0; i < 3; i++) {
                                    MapleMonster mob = MapleLifeFactory.getMonster(8644653);
                                    MapleMonster mob2 = MapleLifeFactory.getMonster(8644653);
                                    mob.setOwner(cchr.getId());
                                    mob2.setOwner(cchr.getId());
                                    map.spawnMonsterOnGroundBelow(mob, new Point(Randomizer.rand(-650, 650), Randomizer.rand(-500, -200)));
                                    map.spawnMonsterOnGroundBelow(mob2, new Point(Randomizer.rand(-650, 650), -157));
                                    cchr.setLastSpawnBlindMobTime(time);
                                }
                            }
                            cchr.setDuskGauge(Math.max(0, cchr.getDuskGauge() - 40));
                            if (cchr.getDuskGauge() <= 0) {
                                for (MapleMonster m : map.getAllMonstersThreadsafe()) {
                                    if (m.getOwner() == cchr.getId()) {
                                        m.setHp(0);
                                        map.broadcastMessage(MobPacket.killMonster(m.getObjectId(), 1));
                                        map.removeMapObject(m);
                                        m.killed();;
                                    }
                                }
                                cchr.cancelEffectFromBuffStat(MapleBuffStat.DuskDarkness, 80002902);
                                cchr.setDuskBlind(false);
                            }
                        } else {
                            cchr.setDuskGauge(Math.min(1000, cchr.getDuskGauge() + 5));
                            if (cchr.getDuskGauge() >= 1000) {
                                SkillFactory.getSkill(80002902).getEffect(1).applyTo(cchr);
                                cchr.setDuskBlind(true);
                            }
                        }
                        cchr.getClient().getSession().writeAndFlush(MobPacket.BossDusk.handleDuskGauge(cchr.isDuskBlind(), cchr.getDuskGauge(), 1000));
                    }

                    if (dusk.getPhase() == 1) {
                        if (time - dusk.getLastSpecialAttackTime() >= 60000) {
                            dusk.setPhase((byte) 0);
                            dusk.setUseSpecialSkill(true);
                            dusk.setLastSpecialAttackTime(time);
                            map.broadcastMessage(MobPacket.changeMobZone(dusk));
                            map.broadcastMessage(MobPacket.BossDusk.spawnTempFoothold(map.getId() == 450009450 ? 3 : 1));
                            map.broadcastMessage(CField.enforceMSG("방어하던 촉수로 강력한 공격을 할거예요! 버텨낸다면 드러난 공허의 눈을 공격할 수 있어요!", 250, 3000));
                        }
                    }
                    if (dusk.getPhase() == 0) {
                        if (time - dusk.getLastSpecialAttackTime() >= 25000) {
                            if (dusk.isUseSpecialSkill()) {
                                MobSkill msi = MobSkillFactory.getMobSkill(186, 11);
                                msi.applyEffect(null, dusk, true, true);
                                if (map.getId() == 450009450) {
                                    MobSkill msi2 = MobSkillFactory.getMobSkill(213, 10);
                                    msi2.applyEffect(null, dusk, true, true);
                                }
                                dusk.setNextSkill(213);
                                dusk.setNextSkillLvl(10);
                                dusk.setUseSpecialSkill(false);
                                att.setLastSeedCountedTime(time);
                                map.broadcastMessage(MobPacket.moveMonsterResponse(dusk.getObjectId(), (short) 0, dusk.getMp(), true, dusk.getNextSkill(), dusk.getNextSkillLvl(), 0));

                            }
                        }
                        if (time - dusk.getLastSpecialAttackTime() >= 35000) {
                            dusk.setPhase((byte) 1);
                            map.broadcastMessage(MobPacket.changeMobZone(dusk));
                            dusk.setLastSpecialAttackTime(time);
                        }
                    }

                    if (att != null) {
                        if (dusk.getPhase() == 1 && !dusk.isUseSpecialSkill()) {
                            if (time - att.getLastSeedCountedTime() >= 10000) {
                                map.broadcastMessage(MobPacket.BossDusk.spawnDrillAttack(Randomizer.rand(-650, 650), Randomizer.nextInt(100) < 50, map.getId() == 450009450 ? 4 : 2));
                                att.setLastSeedCountedTime(time);
                            }
                            if (time - att.getLastSpecialAttackTime() >= 8000) {
                                map.broadcastMessage(MobPacket.enableOnlyFsmAttack(att, 2, 0));
                                att.setLastSpecialAttackTime(time);
                            }
                        }
                    }
                    if (time - dusk.getLastStoneTime() >= 3000) {
                        int[] types = {65, 66, 67};
                        List<Obstacle> obs = new ArrayList<>();
                        int size = Randomizer.rand(3, 10);

                        for (int i = 0; i < size; ++i) {
                            int type = types[Randomizer.nextInt(types.length)];
                            Obstacle ob;
                            int x = Randomizer.rand(-654, 652);
                            if (type == 65) {
                                ob = new Obstacle(type, new Point(x, -1055), new Point(x, -157), 24, map.getId() == 450009450 ? 30 : 15, 157, Randomizer.rand(300, 850), 1, 898, 0);
                            } else if (type == 66) {
                                ob = new Obstacle(type, new Point(x, -1055), new Point(x, -157), 24, map.getId() == 450009450 ? 30 : 15, 185, Randomizer.rand(300, 850), 1, 898, 0);
                            } else if (type == 67) {
                                ob = new Obstacle(type, new Point(x, -1055), new Point(x, -157), 24, map.getId() == 450009450 ? 30 : 15, 718, Randomizer.rand(300, 850), 1, 898, 0);
                            } else { //67 나올리는없을듯.
                                ob = new Obstacle(type, new Point(x, -1055), new Point(x, -157), 24, map.getId() == 450009450 ? 30 : 15, 718, Randomizer.rand(300, 850), 1, 898, 0);
                            }
                            obs.add(ob);
                        }
                        dusk.getMap().broadcastMessage(MobPacket.createObstacle(dusk, obs, (byte) 0));
                        dusk.setLastStoneTime(time);
                    }
                }
            }
        }, 500));
    }

    public static void demianHandler(MapleMonster monster) {
        monster.getMap().removeAllFlyingSword();

        if (monster.getId() == 8880100 || monster.getId() == 8880110) {
            MapleFlyingSword mfs = new MapleFlyingSword(0, monster);
            monster.getMap().spawnFlyingSword(mfs);
            monster.getMap().setNewFlyingSwordNode(mfs, monster.getTruePosition());

            monster.getMap().broadcastMessage(CField.StigmaTime(30000));
            monster.getMap().broadcastMessage(CField.enforceMSG("데미안이 누구에게 낙인을 새길지 알 수 없습니다.", 216, 30000000));

            monster.setSchedule(MobTimer.getInstance().register(() -> {
                int size = monster.getMap().getAllCharactersThreadsafe().size();
                if (size > 0) {
                    MapleCharacter chr = monster.getMap().getAllCharactersThreadsafe().get(Randomizer.nextInt(size));
                    MobSkill ms = MobSkillFactory.getMobSkill(237, 1);
                    ms.applyEffect(chr, monster, true, monster.isFacingLeft());
                    monster.getMap().broadcastMessage(CField.StigmaTime(30000));
                    monster.getMap().broadcastMessage(CField.enforceMSG("데미안이 누구에게 낙인을 새길지 알 수 없습니다.", 216, 30000000));
                } else if (monster.getSchedule() != null) {
                    monster.getSchedule().cancel(false);
                }
            }, 30000));
        } else if (monster.getId() == 8880101 || monster.getId() == 8880111) {
            monster.setHp((long) (monster.getHp() * 0.3));

            monster.getMap().broadcastMessage(CField.StigmaTime(25000));
            monster.getMap().broadcastMessage(CField.enforceMSG("데미안이 누구에게 낙인을 새길지 알 수 없습니다.", 216, 30000000));

            monster.setSchedule(MobTimer.getInstance().register(() -> {
                int size = monster.getMap().getAllCharactersThreadsafe().size();
                if (size > 0) {
                    MapleCharacter chr = monster.getMap().getAllCharactersThreadsafe().get(Randomizer.nextInt(size));
                    MobSkill ms = MobSkillFactory.getMobSkill(237, 1);
                    ms.applyEffect(chr, monster, true, monster.isFacingLeft());
                    monster.getMap().broadcastMessage(CField.StigmaTime(25000));
                    monster.getMap().broadcastMessage(CField.enforceMSG("데미안이 누구에게 낙인을 새길지 알 수 없습니다.", 216, 30000000));
                } else {
                    monster.getSchedule().cancel(false);
                }
            }, 25000));
        }
    }

    public static void pierreHandler(MapleMonster monster) {
        List<Point> pos = new ArrayList<>();
        pos.add(new Point(200, 551));
        pos.add(new Point(1000, 551));
        pos.add(new Point(1400, 551));
        pos.add(new Point(0, 551));
        pos.add(new Point(600, 551));
        pos.add(new Point(1200, 551));
        pos.add(new Point(400, 551));
        pos.add(new Point(800, 551));
        pos.add(new Point(1600, 551));
        monster.getMap().broadcastMessage(MobPacket.dropStone("CapEffect", pos));
        monster.setSchedule(MobTimer.getInstance().register(() -> {

            Transform trans = monster.getStats().getTrans();

            if (monster.getId() % 10 == 0) { // 보라색 피에르
                //     System.out.println( monster.lastCapTime+"  d"+monster.getHPPercent());
                if (monster.getHPPercent() < trans.getOn()) { //몬스터의 hp퍼센트가 70%이하 31% 이상일떄 && monster.getHPPercent() > trans.getOff()

                    if (System.currentTimeMillis() - monster.lastCapTime >= trans.getDuration() * 1000) {
                        MapleMonster copy = MapleLifeFactory.getMonster(Randomizer.nextBoolean() ? monster.getId() + 1 : monster.getId() + 2);

                        copy.setHp(monster.getHp());
                        copy.lastCapTime = System.currentTimeMillis();

                        monster.getMap().spawnMonsterOnGroundBelow(copy, monster.getTruePosition());

                        monster.getMap().killMonster(monster, monster.getController(), false, false, (byte) 0);

                        for (MapleCharacter chr : copy.getMap().getAllCharactersThreadsafe()) {
                            Pair<Integer, Integer> skill = trans.getSkills().get(Randomizer.nextInt(trans.getSkills().size()));

                            if (chr.isAlive()) {
                                chr.cancelDisease(MapleBuffStat.CapDebuff);
                                MobSkillFactory.getMobSkill(skill.left, skill.right).applyEffect(chr, copy, true, copy.isFacingLeft());
                            }
                        }
                    }
                }
                /*else if (monster.getHPPercent() <= trans.getOff()) { // 분열 시작
                    	if (System.currentTimeMillis() - monster.lastCapTime >= trans.getDuration() * 1000) {
                     MapleMonster copy1 = MapleLifeFactory.getMonster(monster.getId() + 1);

                     copy1.setHp(monster.getHp());
                     copy1.lastCapTime = System.currentTimeMillis();

                     monster.getMap().spawnMonsterOnGroundBelow(copy1, monster.getTruePosition());

                     MapleMonster copy2 = MapleLifeFactory.getMonster(monster.getId() + 2);

                     copy2.setHp(monster.getHp());
                     copy2.lastCapTime = System.currentTimeMillis();

                     monster.getMap().spawnMonsterOnGroundBelow(copy2, monster.getTruePosition());

                     for (MapleCharacter chr : monster.getMap().getAllCharactersThreadsafe()) {
                     Pair<Integer, Integer> skill = trans.getSkills().get(Randomizer.nextInt(trans.getSkills().size()));

                     if (chr.isAlive()) {
                     chr.cancelDisease(MapleBuffStat.CapDebuff);
                     MobSkillFactory.getMobSkill(skill.left, skill.right).applyEffect(chr, monster, true, monster.isFacingLeft());
                     }
                     }

                     monster.getMap().killMonster(monster, monster.getController(), false, false, (byte) 0);
                     }
                }*/
            } else if (monster.getId() % 10 == 1 || monster.getId() % 10 == 2) { // 빨간색, 파란색 피에르
                /*if (monster.getHPPercent() <= trans.getOn() && monster.getHPPercent() >= trans.getOff()) { // 분열 상태
        			if (System.currentTimeMillis() - monster.lastCapTime >= trans.getDuration() * 1000) {
                     MapleMonster copy1 = MapleLifeFactory.getMonster(monster.getId());
                     MapleMonster copy2 = MapleLifeFactory.getMonster(monster.getId() % 10 == 1 ? monster.getId() + 1 : monster.getId() - 1);

                     copy1.setHp((long) (copy1.getHp() * 0.3));
                     copy2.setHp((long) (copy2.getHp() * 0.3));

                     copy1.lastCapTime = System.currentTimeMillis();
                     copy2.lastCapTime = System.currentTimeMillis();

                     monster.getMap().spawnMonsterOnGroundBelow(copy1, monster.getTruePosition());
                     monster.getMap().spawnMonsterOnGroundBelow(copy2, monster.getTruePosition());


                     monster.getMap().killAllMonster(monster.getController());

                     for (MapleCharacter chr : copy2.getMap().getAllCharactersThreadsafe()) {
                     Pair<Integer, Integer> skill = trans.getSkills().get(Randomizer.nextInt(trans.getSkills().size()));

                     if (chr.isAlive()) {
                     chr.cancelDisease(MapleBuffStat.CapDebuff);
                     MobSkillFactory.getMobSkill(skill.left, skill.right).applyEffect(chr, copy2, true, copy2.isFacingLeft());
                     }
                     }
                     }
                } else { // 미분열 상태*/
                if (System.currentTimeMillis() - monster.lastCapTime >= trans.getDuration() * 1000) {
                    MapleMonster copy = MapleLifeFactory.getMonster(monster.getId() % 10 == 1 ? monster.getId() - 1 : monster.getId() - 2);
                    copy.setHp(monster.getHp());
                    copy.lastCapTime = System.currentTimeMillis();

                    monster.getMap().spawnMonsterOnGroundBelow(copy, monster.getTruePosition());

                    monster.getMap().killMonster(monster, monster.getController(), false, false, (byte) 0);

                    for (MapleCharacter chr : copy.getMap().getAllCharactersThreadsafe()) {
                        Pair<Integer, Integer> skill = trans.getSkills().get(Randomizer.nextInt(trans.getSkills().size()));

                        if (chr.isAlive()) {
                            chr.cancelDisease(MapleBuffStat.CapDebuff);
                            MobSkillFactory.getMobSkill(skill.left, skill.right).applyEffect(chr, copy, true, copy.isFacingLeft());
                        }
                    }
                }
                //  }
            }
        }, 1000));
    }

    public static void blackMageHandler(MapleMonster monster) {

        monster.setSchedule(MobTimer.getInstance().register(() -> {
            long time = System.currentTimeMillis();
            if (time - monster.lastObstacleTime >= 60000) {
                monster.lastObstacleTime = time;
                int x = Randomizer.rand(-1700, 1700);
                int[] angle = {973, 808, 690};
                int[] length = {5, 6, 7};

                //FieldSkill 100010
                if (monster.getMap().getMonsterById(8880502) != null || monster.getMap().getMonsterById(8880503) != null || monster.getMap().getMonsterById(8880505) != null) {

                    List<Obstacle> obs = new ArrayList<>();

                    for (int i = 0; i < 13; i++) {
                        Obstacle ob = new Obstacle(75, new Point(-1800 + (i * 200), -600), new Point(-1800 + (i * 200), 88), 25, 50, 1459, 125, 1, 653, 0);
                        ob.setVperSec(1000);
                        obs.add(ob);
                    }

                    for (int i = 0; i < obs.size(); ++i) {
                        Obstacle ob = obs.get(i);
                        MobTimer.getInstance().schedule(() -> {
                            monster.getMap().broadcastMessage(MobPacket.createObstacle2(monster, ob, (byte) 4));
                        }, 500 * i);
                    }
                }

                if (monster.getMap().getMonsterById(8880502) != null || monster.getMap().getMonsterById(8880503) != null) {
                    for (int i = 0; i < 3; i++) {
                        MobTimer.getInstance().schedule(() -> {
                            Obstacle ob = new Obstacle(77, new Point(x, -600), new Point(x - 700, 88), 95, 35, 0x1F1, 125, length[Randomizer.nextInt(length.length)], angle[Randomizer.nextInt(angle.length)], 45);
                            ob.setVperSec(1000);
                            monster.getMap().broadcastMessage(MobPacket.createObstacle2(monster, ob, (byte) 5));
                        }, 1000 * i);
                    }
                }
            }

            if (monster.getMap().getMonsterById(8880505) != null || monster.getMap().getMonsterById(8880502) != null) {
                if (time - monster.lastChainTime >= 10000) {
                    monster.lastChainTime = time;
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(100007, 1));
                }
            }

            if (monster.getMap().getMonsterById(8880505) != null) {
                if (time - monster.lastThunderTime >= 10000) {
                    monster.lastThunderTime = time;
                    int[] pointz = {-400, -1000, -1600, 400, 1000, 1600};
                    int count = 0;
                    for (MapleMonster mob : monster.getMap().getAllMonstersThreadsafe()) {
                        if (mob.getId() == 8880506) {
                            count++;
                        }
                    }

                    if (count < 6) {
                        MapleMonster thunder = MapleLifeFactory.getMonster(8880506);
                        monster.getMap().spawnMonsterOnGroundBelow(thunder, new Point(pointz[Randomizer.nextInt(pointz.length)], 84));

                        //20초 후 삭제
                        MobTimer.getInstance().schedule(() -> {
                            monster.getMap().killMonster(thunder, thunder.getController(), false, false, (byte) 0);
                        }, 20000);
                    }
//					 monster.getMap().broadcastMessage(MobPacket.useFieldSkill(100005, 1));
                }
            }

            if (monster.getMap().getMonsterById(8880502) != null) {
                if (time - monster.lastThunderTime >= 72000) {
                    monster.lastThunderTime = time;
                    monster.getMap().broadcastMessage(CField.enforceMSG("검은 마법사의 붉은 번개가 모든 곳을 뒤덮는다. 피할 곳을 찾아야 한다.", 265, 3000));
                    monster.getMap().broadcastMessage(CField.EffectPacket.showFieldSkillEffect(100017, 1));
                }

                if (time - monster.lastEyeTime >= 50000) {
                    monster.lastEyeTime = time;
                    monster.getMap().broadcastMessage(CField.enforceMSG("파멸의 눈이 적을 쫓는다.", 265, 3000));
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(100012, 1));
                }
                if (time - monster.lastSpearTime >= 19820 && monster.getMap().getMonsterById(8880516) != null) {
                    monster.lastSpearTime = time;
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(FieldSkillFactory.getInstance().getFieldSkill(100009, 1)));

                    monster.getMap().broadcastMessage(MobPacket.enableOnlyFsmAttack(monster.getMap().getMonsterById(8880516), 1, 0));
                }
            }


            if (monster.getMap().getMonsterById(8880503) != null) {

                byte phase;
                if (monster.getMap().getMonsterById(8880503).getHPPercent() > 75) {
                    phase = 1;
                } else if (monster.getMap().getMonsterById(8880503).getHPPercent() > 50) {
                    phase = 2;
                } else if (monster.getMap().getMonsterById(8880503).getHPPercent() > 25) {
                    phase = 3;
                } else {
                    phase = 4;
                }
                if (monster.getMap().getMonsterById(8880503).getPhase() != phase) {
                    monster.getMap().getMonsterById(8880503).setPhase(phase);
                    monster.getMap().getMonsterById(8880503).getMap().broadcastMessage(MobPacket.changePhase(monster));
                    monster.getMap().getMonsterById(8880503).getMap().broadcastMessage(MobPacket.changeMobZone(monster));
                }

                if (time - monster.lastRedObstacleTime >= 18740) {
                    monster.lastRedObstacleTime = time;

                    //FieldSkill 100010
                    int randx = Randomizer.rand(-800, 800);
                    Obstacle ob = new Obstacle(76, new Point(randx, 400), new Point(randx, -600), 95, 10, 0x1F1, 30, 5, 1000, 45);
                    ob.setVperSec(500);
                    monster.getMap().broadcastMessage(MobPacket.createObstacle2(monster, ob, (byte) 5));

                    randx = Randomizer.rand(-800, 800);
                    ob = new Obstacle(79, new Point(randx, 400), new Point(randx, -600), 95, 10, 0x1F1, 30, 5, 1000, 45);
                    ob.setVperSec(500);
                    monster.getMap().broadcastMessage(MobPacket.createObstacle2(monster, ob, (byte) 5));
                }

                if (time - monster.lastLaserTime >= 5250) {
                    monster.lastLaserTime = time;

                    int randX = Randomizer.rand(-850, -750);

                    FieldSkill skill = new FieldSkill(100011, 1);

                    List<FieldSkill.LaserInfo> infos = new ArrayList<>();
                    for (int i = 0; i < 3; ++i) {
                        infos.add(new FieldSkill.LaserInfo(new Point(randX + i * 500, Randomizer.rand(-400, 300)), Randomizer.rand(5, 50), Randomizer.rand(500, 900)));
                    }
                    skill.setLaserInfoList(infos);

                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(skill));
                    monster.getMap().broadcastMessage(MobPacket.forcedSkillAction(monster.getMap().getMonsterById(8880503).getObjectId(), 3, false));
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(FieldSkillFactory.getInstance().getFieldSkill(100015, 1)));

                }
                if (time - monster.lastThunderTime >= 66710) {
                    monster.lastThunderTime = time;
                    int type = Randomizer.rand(1, 2);
                    List<MapleNodes.Environment> envs = new ArrayList<>();
                    for (MapleNodes.Environment env : monster.getMap().getNodez().getEnvironments()) {
                        if (env.getName().contains("foo") && !env.getName().contains("foot")) {
                            env.setShow(true);
                            envs.add(env);
                        }
                    }

                    FieldSkill skill = new FieldSkill(100013, 1);
                    skill.setEnvInfo(envs);
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(skill));
                    monster.getMap().broadcastMessage(CField.getUpdateEnvironment(envs));
                    //발판 미리생성
                    MobTimer.getInstance().schedule(() -> {
                        monster.getMap().broadcastMessage(MobPacket.forcedSkillAction(monster.getMap().getMonsterById(8880503).getObjectId(), type - 1, true));
                        monster.getMap().broadcastMessage(CField.enforceMSG("검은 마법사가 창조와 파괴의 권능을 사용한다. 위와 아래, 어느 쪽으로 피할지 선택해야 한다.", 265, 3000));
                    }, 5000);
                    MobTimer.getInstance().schedule(() -> {
                        if (type == 1) {

                            monster.getMap().broadcastMessage(MobPacket.useFieldSkill(FieldSkillFactory.getInstance().getFieldSkill(100015, 1)));
//                            monster.getMap().setBlackMage3rdSkill(true);
                        } else if (type == 2) {
                            List<Obstacle> obs = new ArrayList<>();

                            for (int i = 0; i < 29; i++) {
                                int x = -930 + (i * 65);
                                int y = 85;
                                if (x <= -500 && x >= -630) {
                                    y = -298;
                                }
                                if (x <= -195 && x >= -380) {
                                    y = -211;
                                }
                                if (x <= 205 && x >= 75) {
                                    y = -90;
                                }
                                if (x <= 575 && x >= 395) {
                                    y = -108;
                                }
                                if (x <= 915 && x >= 660) {
                                    y = -309;
                                }
                                Point maxheight = new Point(x, -600); //x좌표와 높이
                                Point minheight = new Point(x, y); // x좌표와 높이
                                Obstacle ob = new Obstacle(78, maxheight, minheight, 100, 999, 450, 500 - i * 15, 2, y + 600, 0);
                                obs.add(ob);
                            }

                            monster.getMap().broadcastMessage(MobPacket.createObstacle(monster, obs, (byte) 0));
//
                        }
                    }, 7500);
                    MobTimer.getInstance().schedule(() -> {
                        monster.setBarrier(1000000000000L);
                        monster.getMap().broadcastMessage(MobPacket.mobBarrier(monster.getObjectId(), 100)); // %값
                    }, 15500);

                    MobTimer.getInstance().schedule(() -> {
                        for (MapleNodes.Environment env : envs) {
                            env.setShow(false);
                        }

                        FieldSkill skills = new FieldSkill(100013, 1);
                        skills.setEnvInfo(envs);

//                        monster.getMap().setBlackMage3rdSkill(false);
                        monster.getMap().broadcastMessage(MobPacket.useFieldSkill(skills));

                        monster.getMap().broadcastMessage(MobPacket.useFieldSkill(skills));
                        monster.getMap().broadcastMessage(CField.getUpdateEnvironment(envs));
                    }, 15500);
                }

                if (time - monster.lastEyeTime >= 47720) {
                    monster.lastEyeTime = time;
                    monster.getMap().broadcastMessage(CField.enforceMSG("파괴의 천사가 무에서 창조된다.", 265, 3000));

                    monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880509), new Point(Randomizer.rand(0, 1960) - 970, 85)); // rand point
                    monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880510), new Point(Randomizer.rand(0, 1960) - 970, 85));
                    monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880511), new Point(Randomizer.rand(0, 1960) - 970, 85));

                    MobTimer.MobTimer.getInstance().schedule(() -> {
                        monster.getMap().killMonster(8880509);
                        monster.getMap().killMonster(8880510);
                        monster.getMap().killMonster(8880511);
                    }, 20000);
                }
            }

            if (monster.getMap().getMonsterById(8880504) != null) {
                if (time - monster.lastBWBliThunder >= 8000) {
                    monster.lastBWBliThunder = time;

                    FieldSkill skill = new FieldSkill(100016, 1);

                    List<FieldSkill.ThunderInfo> infos = new ArrayList<>();
                    int randdam = Randomizer.rand(3, 4);
                    for (int i = 1; i <= randdam; ++i) {
                        int ranx = Randomizer.rand(monster.getMap().getLeft(), monster.getMap().getRight());
                        int rany = Randomizer.rand(-251, 159);
                        infos.add(new FieldSkill.ThunderInfo(new Point(ranx - 150, rany - 150), new Point(ranx + 150, rany + 150), Randomizer.rand(0, 1), 240 * i));
                    }
                    skill.setThunderInfo(infos);
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(skill));
                }


                if (time - monster.lastBWThunder >= 30000) {
                    monster.lastBWThunder = time;
                    monster.getMap().broadcastMessage(CField.enforceMSG("신에 가까운 자의 권능이 발현된다. 창조와 파괴, 어떤 힘을 품을지 선택해야 한다.", 265, 3000));

                    MobTimer.getInstance().schedule(() -> {
                        FieldSkill skill = new FieldSkill(100014, 1);
                        List<FieldSkill.ThunderInfo> infos = new ArrayList<>();
                        for (int i = 0; i < 14; ++i) {
                            infos.add(new FieldSkill.ThunderInfo(new Point(-801 + (i * 120), -404), new Point(-681 + (i * 120), 238), Randomizer.rand(0, 3), 0));
                        }
                        skill.setThunderInfo(infos);
                        monster.getMap().broadcastMessage(MobPacket.useFieldSkill(skill));
                    }, 1200);

                    MobTimer.getInstance().schedule(() -> {
                        monster.setBarrier(1000000000000L);
                        monster.getMap().broadcastMessage(MobPacket.mobBarrier(monster.getObjectId(), 100)); // %값
                    }, 3000);
                }
            }

            for (MapleCharacter chr : monster.getMap().getAllCharactersThreadsafe()) {
                if (chr.hasDisease(MapleBuffStat.CurseOfCreation)) {
                    if (Randomizer.isSuccess(25)) {
                        monster.getMap().broadcastMessage(MobPacket.useFieldSkill(100015, Randomizer.rand(1, 2)));
                    }
                }
            }
        }, 1000));
    }

    public static void SerenHandler(MapleMonster monster) {
        switch (monster.getId()) {
            case 8880600:
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    List<Obstacle> obs = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        int x = Randomizer.rand(-1030, 1030);
                        Obstacle ob = new Obstacle(84, new Point(x, -440), new Point(x, 275), 30, 15, i * 1000, Randomizer.rand(16, 32), 3, 715, 0);
                        obs.add(ob);
                    }
                    monster.getMap().CreateObstacle(monster, obs);
                    for (MapleCharacter chr : monster.getMap().getAllChracater()) {
                        if (chr.isAlive())
                            chr.addSerenGauge(-10);
                    }
                }, 5000L));
                break;
            case 8880601:
            case 8880604:
            case 8880608:
            case 8880613: {
                int time = monster.getId() == 8880601 ? 7000 : Randomizer.rand(7000, 13000);
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    monster.getMap().broadcastMessage(MobPacket.enableOnlyFsmAttack(monster, 1, 0));
                }, time));
                break;
            }
            case 8880602:
                monster.ResetSerenTime(true);
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    if (monster.getMap().getAllChracater().size() > 0 && monster.getCustomValue0(8880603) == 0L) {
                        monster.addSkillCustomInfo(8880602, 1L);
                        if (monster.getCustomValue0(8880602) >= (long)(monster.getSerenTimetype() == 3 ? 1 : 5)) {
                            monster.removeCustomInfo(8880602);
                            if (monster.getSerenTimetype() == 4) {
                                monster.gainShield(monster.getStats().getHp() / 100L, monster.getShield() <= 0L, 0);
                            }

                            Iterator var1 = monster.getMap().getAllChracater().iterator();

                            while(var1.hasNext()) {
                                MapleCharacter chr = (MapleCharacter)var1.next();
                                if (chr.isAlive()) {
                                    chr.addSerenGauge(monster.getSerenTimetype() == 3 ? -20 : 20);
                                }
                            }
                        }

                        monster.AddSerenTimeHandler(monster.getSerenTimetype(), -1);
                    }

                }, 1000L));
                break;
            case 8880603:
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    if (monster == null || monster.getMap().getMonsterById(8880603) == null) {
                        monster.getSchedule().cancel(true);
                        monster.setSchedule((ScheduledFuture)null);
                    }

                    if (monster != null) {
                        for (int i = 0; i < 2; ++i) {
                            int time = i == 0 ? 10 : 1000;
                            MobTimer.getInstance().schedule(() -> {
                                monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880606), new Point(470, 305));
                                monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880606), new Point(-10, 305));
                                monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880606), new Point(-450, 305));
                            }, time);
                        }
                    }

                }, (long)Randomizer.rand(7000, 11000)));
                break;

            case 8880605:
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    for (MapleCharacter chr : monster.getMap().getAllChracater()) {
                        if (chr.isAlive() && (chr.getPosition()).x - 300 <= (monster.getPosition()).x && (chr.getPosition()).x + 300 >= (monster.getPosition()).x) {
                            monster.addSkillCustomInfo(8880605, 1L);
                            if (monster.getCustomValue0(8880605) >= 10L) {
                                monster.switchController(chr.getClient().getRandomCharacter(), true);
                                monster.removeCustomInfo(8880605);
                            }
                            monster.getMap().broadcastMessage(MobPacket.enableOnlyFsmAttack(monster, 1, 0));
                            break;
                        }
                    }
                }, 2000L));
                break;
            case 8880606:
            case 8880609:
            case 8880611:
            case 8880612:
                break;
            case 8880607:
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    monster.getMap().broadcastMessage(MobPacket.useFieldSkill(100023, 1));
                }, Randomizer.rand(5000, 10000)));
                break;

            case 8880610:
                monster.setSchedule(MobTimer.getInstance().register(() -> {
                    if (monster != null) {
                        int type = Randomizer.rand(0, 2);
                        if (type == 0) {
                            monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880611), new Point(-320, 305));
                            monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880611), new Point(470, 305));
                        } else {
                            monster.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880611), (type == 1) ? new Point(-320, 305) : new Point(470, 305));
                        }
                    }
                }, Randomizer.rand(30000, 50000)));
                break;

        }
    }
}
