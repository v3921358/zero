package server.control;

import client.MapleBuffStat;
import client.MapleBuffStatValueHolder;
import client.MapleCharacter;
import client.MapleCoolDownValueHolder;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.field.boss.MapleBossManager;
import server.life.Ignition;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleNodes.Environment;
import server.maps.MapleSummon;
import tools.CurrentTime;
import tools.Pair;
import tools.packet.CField;
import tools.packet.CField.SummonPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.MobPacket;

public class MapleSkillControl implements Runnable {

    private Skill _overloadMode;
    private int date, dos, numTimes;
    private boolean isfirst;
    private long lastSaveAuctionTime = 0;

    public MapleSkillControl() {
        this.date = CurrentTime.요일();
        this.dos = 0;
        this.numTimes = 0;
        this.isfirst = false;
        this.lastSaveAuctionTime = System.currentTimeMillis();
        System.out.println("[Loading Completed] Start SkillControl");
    }

    public static void managementStackBuff(MapleCharacter chr, long time) {
        if (chr.getSkillLevel(400041074) > 0) {
            MapleStatEffect effect = SkillFactory.getSkill(400041074).getEffect(chr.getSkillLevel(400041074));
            if (chr.lastVarietyFinaleTime == 0) {
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.VarietyFinaleCount, 0));
                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
            }
            if (time - chr.lastVarietyFinaleTime >= effect.getX() * 1000) {
                if (chr.VarietyFinaleCount < effect.getY()) {
                    chr.VarietyFinaleCount++;
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.VarietyFinaleCount, 0));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                }
                chr.lastVarietyFinaleTime = time;
            }
        }
        // 하울링 게일
        if (chr.getSkillLevel(400031003) > 0) {
            MapleStatEffect effect = SkillFactory.getSkill(400031003).getEffect(chr.getSkillLevel(400031003));
            if (chr.lastHowlingGaleTime == 0) {
                chr.lastHowlingGaleTime = System.currentTimeMillis();
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.HowlingGale, new Pair<>(chr.getHowlingGaleCount(), 0));
                effect.setDuration(0);
                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
            }

            if (time - chr.lastHowlingGaleTime >= effect.getX() * 1000) {
                if (chr.getHowlingGaleCount() < effect.getY()) {
                    chr.setHowlingGaleCount(chr.getHowlingGaleCount() + 1);
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.HowlingGale, new Pair<>(chr.getHowlingGaleCount(), 0));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                }
                chr.lastHowlingGaleTime = time;
            }
        }

        // 와일드 그레네이드
        if (chr.getSkillLevel(400031032) > 0) {
            MapleStatEffect effect = SkillFactory.getSkill(400031032).getEffect(chr.getSkillLevel(400031032));

            if (chr.lastWildGrenadierTime == 0) {
                chr.lastWildGrenadierTime = System.currentTimeMillis();

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.WildGrenadier, new Pair<>(chr.getWildGrenadierCount(), 0));

                effect.setDuration(0);

                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
            }

            if (time - chr.lastWildGrenadierTime >= effect.getT() * 1000) {
                if (chr.getWildGrenadierCount() < effect.getZ()) {
                    chr.setWildGrenadierCount(chr.getWildGrenadierCount() + 1);

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.WildGrenadier, new Pair<>(chr.getWildGrenadierCount(), 0));

                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                }

                chr.lastWildGrenadierTime = time;
            }
        }

        if (chr.getSkillLevel(400011131) > 0) {
            MapleStatEffect effect = SkillFactory.getSkill(400011131).getEffect(chr.getSkillLevel(400011131));
            if (chr.lastMjollnirTime == 0) {
                chr.lastMjollnirTime = System.currentTimeMillis();
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.getMjollnir(), 0));
                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
            }
            if (time - chr.lastMjollnirTime >= effect.getX() * 1000) {
                if (chr.getMjollnir() < effect.getY()) {
                    chr.setMjollnir(chr.getMjollnir() + 1);

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.getMjollnir(), 0));

                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                }
                chr.lastMjollnirTime = time;
            }
        }

        if (chr.getSkillLevel(63101001) > 0) {//카인 포제션I
            MapleStatEffect effect = SkillFactory.getSkill(63101001).getEffect(chr.getSkillLevel(63101001));
            if (chr.lastMaliceChargeTime == 0) {
                chr.lastMaliceChargeTime = System.currentTimeMillis();
            }
            if (time - chr.lastMaliceChargeTime >= effect.getU() * 1000) {
                if (chr.getBuffedValue(MapleBuffStat.Possession) == null) {
                    if (!(chr.MalicePoint >= 300 && chr.getSkillLevel(63120000) < 1) && !(chr.MalicePoint >= 500 && chr.getSkillLevel(63120000) > 0)) {
                        chr.MalicePoint += 10;
                        if (chr.getSkillLevel(63120000) > 0) {//카인 포제션II
                            chr.MalicePoint += 10;
                            if (chr.MalicePoint > 500) {
                                chr.MalicePoint = 500;
                            }
                        } else {
                            if (chr.MalicePoint > 300) {
                                chr.MalicePoint = 300;
                            }
                        }
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        statups.put(MapleBuffStat.Malice, new Pair<>(chr.MalicePoint, 0));
                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
                        chr.lastMaliceChargeTime = time;
                    }
                }
            }
        }
    }

    public static void handleCooldowns(final MapleCharacter chr, final long now) {
        List<MapleCoolDownValueHolder> cooldowns = new ArrayList<>();
        if (chr.getCooldownSize() > 0) {
            for (MapleCoolDownValueHolder m : chr.getCooldowns()) {
                if (m.startTime + m.length < now) {
                    cooldowns.add(m);
                }
            }
        }

        if (!cooldowns.isEmpty()) {
            chr.clearCooldowns(cooldowns);
        }
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();

        Iterator<ChannelServer> channels = ChannelServer.getAllInstances().iterator();
        while (channels.hasNext()) {
            ChannelServer cs = channels.next();
            Iterator<MapleCharacter> chrs = cs.getPlayerStorage().getAllCharacters().values().iterator();
            while (chrs.hasNext()) {
                MapleCharacter chr = chrs.next();
                MapleMap map = chr.getMap();

                if (chr.isAlive()) {
                    if (chr.canFairy(time)) {
                        chr.doFairy();
                    }
                    if (chr.canDOT(time)) {
                        chr.doDOT();
                    }

                    handleCooldowns(chr, time);

                    if (chr.getMapId() > 450008000 && chr.getMapId() < 450009000 && map.getAllMonstersThreadsafe().size() > 0 && chr.getMoonGauge() < 90) {
                        chr.setMoonGauge(Math.min(chr.getMoonGauge() + 1, 100));
                        chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(chr.getMoonGauge()));
                    }

                    Item weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon_item != null) {
                        // 무기가 null이 아닌지 체크
                        String weapon_name = MapleItemInformationProvider.getInstance().getName(weapon_item.getItemId());
                        if (weapon_name != null) {
                            if (weapon_name.startsWith("제네시스 ")) { // 무기명이 제네시스로 시작하는지 체크
                                if (chr.getSkillLevel(80002632) <= 0) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(80002632), (byte) 1, (byte) 1);
                                }
                                if (chr.getSkillLevel(80002633) <= 0) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(80002633), (byte) 1, (byte) 1);
                                }
                                if (chr.getSkillLevel(80002634) <= 0) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(80002634), (byte) 1, (byte) 1);
                                }
                            }
                        }
                    }

                    if (chr.getDisease(MapleBuffStat.VampDeath) != null) {
                        Pair<MobSkill, Integer> skill = chr.getDisease(MapleBuffStat.VampDeath);
                        MapleMonster mob = chr.getMap().getMonsterById(8870100);
                        if (skill != null && mob != null) {
                            mob.heal(63000000, 0, true);
                        }
                    }

                    if (map.getId() >= 105200520 && map.getId() < 105200530) { // 매 초, 최대 체력의 20%씩 감소
                        chr.addHP(-chr.getStat().getCurrentMaxHp() / 5);
                    }

                    Iterator<MapleMonster> lifes = map.getAllMonstersThreadsafe().iterator();

                    if (lifes.hasNext()) {
                        if (chr.getBuffedValue(80001752)) {
                            final List<MapleMonster> objs = chr.getMap().getAllMonstersThreadsafe();
                            if (objs != null && objs.size() > 0) {
                                MapleMonster mob = chr.getMap().getAllMonster().get(Randomizer.rand(0, objs.size() - 1));
                                if (mob.isAlive() && System.currentTimeMillis() - chr.getSkillCustomTime(80001752) >= 3000) {
                                    chr.getClient().getSession().writeAndFlush(CField.thunderAttack(mob.getPosition().x, mob.getPosition().y, mob.getId()));
                                    chr.setSkillCustomTime(80001752, System.currentTimeMillis());
                                }
                            }
                        }
                    }

                    while (lifes.hasNext()) {
                        MapleMonster life = lifes.next();
                        if (life.getId() >= 8800102 && life.getId() <= 8800110) {
                            if ((time - life.getSpawnTime() >= 60000) && life.getPhase() == 1) { // 대충 2분으로 설정
                                life.setPhase((byte) 2);
                                map.broadcastMessage(MobPacket.changePhase(life));
                                life.setSpawnTime(time);

                                List<Environment> envs = chr.getMap().getNodez().getEnvironments();
                                for (Environment ev : envs) {
                                    ev.setShow(true);
                                }
                                chr.getMap().broadcastMessage(CField.getUpdateEnvironment(envs));
                            }

                            if ((time - life.getSpawnTime() >= 60000) && life.getPhase() == 2) { // 대충 2분으로 설정
                                life.setPhase((byte) 1);
                                map.broadcastMessage(MobPacket.changePhase(life));
                                life.setSpawnTime(time);

                                List<Environment> envs = chr.getMap().getNodez().getEnvironments();
                                for (Environment ev : envs) {
                                    ev.setShow(false);
                                }
                                chr.getMap().broadcastMessage(CField.getUpdateEnvironment(envs));
                            }
                        }

                        if (life.isBuffed(MonsterStatus.MS_Burned)) {
                            MonsterStatusEffect buff = life.getBuff(MonsterStatus.MS_Burned);
                            if (buff != null && life.getHp() > 1 && buff.getInterval() > 0) {
                                if (time - buff.getLastPoisonTime() > buff.getInterval()) {
                                    buff.setLastPoisonTime(time);
                                    life.damage(chr, Math.min(life.getHp() - 1, buff.getValue()), true);
                                    if (buff.getSkill() == 25121006 && chr.getSkillLevel(25121006) > 0) {
                                        chr.addHP(buff.getValue() * SkillFactory.getSkill(buff.getSkill()).getEffect(chr.getSkillLevel(25121006)).getX() / 100);
                                    }
                                }
                            }
                        }

                        if (life.isExtreme()) {
                            /* custom */
                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                            statups.put(MapleBuffStat.PmdReduce, new Pair<>(70, 0));
                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
                        }

                        if (life.getId() == 8880000 || life.getId() == 8880002 || life.getId() == 8880010) {
                            /* 몹존 */
                            int pix;
                            if (life.getHPPercent() <= 25) {
                                pix = 195;
                            } else if (life.getHPPercent() <= 50) {
                                pix = 295;
                            } else if (life.getHPPercent() <= 75) {
                                pix = 345;
                            } else {
                                pix = 395;
                            }
                            boolean damaged = false;
                            if (chr.getTruePosition().getX() < (life.getTruePosition().getX() - pix)) {
                                damaged = true;
                            } else if (chr.getTruePosition().getX() > (life.getTruePosition().getX() + pix)) {
                                damaged = true;
                            }
                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                            statups.put(MapleBuffStat.MobZoneState, new Pair<>(damaged ? 0 : 1, life.getObjectId()));
                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
                            MapleBossManager.changePhase(life);
                            if (damaged) {
                                chr.addHP(-chr.getStat().getCurrentMaxHp() / 10);
                            }
                        }
                    }

                    if (chr.getSkillLevel(27110007) > 0) {
                        if (chr.getBuffedValue(MapleBuffStat.LifeTidal, 27110007) == null) {
                            MapleStatEffect lifetidal = SkillFactory.getSkill(27110007).getEffect(chr.getSkillLevel(27110007));
                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

                            if (chr.getStat().getHPPercent() > chr.getStat().getMPPercent()) {
                                statups.put(MapleBuffStat.LifeTidal, new Pair<>(1, 0));
                            } else {
                                statups.put(MapleBuffStat.LifeTidal, new Pair<>(2, 0));
                            }

                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, lifetidal, chr));
                            chr.getEffects().add(new Pair<>(MapleBuffStat.LifeTidal, new MapleBuffStatValueHolder(lifetidal, -1, null, statups.get(MapleBuffStat.LifeTidal).left, 0, chr.getId())));
                        } else {
                            if (chr.getStat().getHPPercent() > chr.getStat().getMPPercent()) {
                                if (chr.getBuffedValue(MapleBuffStat.LifeTidal, 27110007) == 2) {
                                    chr.cancelEffectFromBuffStat(MapleBuffStat.LifeTidal, 27110007);
                                }
                            } else {
                                if (chr.getBuffedValue(MapleBuffStat.LifeTidal, 27110007) == 1) {
                                    chr.cancelEffectFromBuffStat(MapleBuffStat.LifeTidal, 27110007);
                                }
                            }
                        }
                    }

                    if (chr.getSkillLevel(22170074) > 0) {
                        MapleStatEffect dragonFury = SkillFactory.getSkill(22170074).getEffect(chr.getSkillLevel(22170074));
                        if (chr.getStat().getMPPercent() >= dragonFury.getX() && chr.getStat().getMPPercent() <= dragonFury.getY()) {
                            if (!chr.getBuffedValue(22170074)) {
                                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                statups.put(MapleBuffStat.IndieMadR, new Pair<>((int) dragonFury.getDamage(), 0));
                                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, dragonFury, chr));
                                chr.getEffects().add(new Pair<>(MapleBuffStat.IndieMadR, new MapleBuffStatValueHolder(dragonFury, -1, null, dragonFury.getDamage(), 0, chr.getId())));
                            }
                        } else if (chr.getBuffedValue(22170074)) {
                            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieMadR, 22170074);
                        }
                    }

                    if (chr.getCooldownLimit(31121054) > 0) {
                        if (time - chr.cooldownforceBlood >= 3000) {
                            chr.changeCooldown(31121054, -2000);
                        }
                    }

                    if (chr.getBuffedValue(32121018)) {
                        chr.addMP(-chr.getBuffedEffect(32121018).getMPCon());
                        if (time - chr.checkBuffStatValueHolder(MapleBuffStat.DebuffAura, 32121018).startTime >= 2000) { // 첫 2초는 안됨
                            final List<MapleMapObject> objs = chr.getMap().getMapObjectsInRange(chr.getTruePosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER));
                            MapleStatEffect debuffAura = SkillFactory.getSkill(32121018).getEffect(chr.getSkillLevel(32121018));
                            Iterator<MapleMapObject> objz = objs.iterator();
                            while (objz.hasNext()) {
                                MapleMonster mo = (MapleMonster) objz.next();
                                mo.applyStatus(chr.getClient(), MonsterStatus.MS_MultiPMDR, new MonsterStatusEffect(debuffAura.getSourceId(), debuffAura.getDuration()), debuffAura.getX(), debuffAura);
                            }
                        }
                    }

                    if (!chr.getBuffedValue(33110014) && chr.getSkillLevel(33110014) > 0) {
                        SkillFactory.getSkill(33110014).getEffect(chr.getSkillLevel(33110014)).applyTo(chr, true);
                    }

                    if (chr.getBuffedValue(400021006)) {
                        chr.addMP(-chr.getBuffedEffect(400021006).getMPCon());
                    }

                    if (chr.getBuffedEffect(MapleBuffStat.Sungi) != null && time - chr.lastRecoverScrollGauge >= 3000) {
                        chr.lastRecoverScrollGauge = time;
                        chr.energy = Math.min(chr.energy + chr.getBuffedEffect(MapleBuffStat.Sungi).getX(), 100);
                        chr.scrollGauge = Math.min(900, chr.scrollGauge + chr.getBuffedEffect(MapleBuffStat.Sungi).getY());

                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        statups.put(MapleBuffStat.TidalForce, new Pair<>(chr.energy, 0));

                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
                    }

                    if (chr.getBuffedValue(400051015)) {
                        chr.energy = Math.max(0, chr.energy - chr.getBuffedEffect(400051015).getY());
                        if (chr.energy == 0) {
                            chr.energyCharge = false;
                            chr.cancelEffectFromBuffStat(MapleBuffStat.DevilishPower, 400051015);
                        }
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

                        statups.put(MapleBuffStat.EnergyCharged, new Pair<>(chr.energy, 0));

                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.EnergyCharged), chr));
                        chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, chr.getBuffedEffect(MapleBuffStat.EnergyCharged)), false);
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "HolySymbol") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(2311003) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(2311003), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(2311003)) {
                                    SkillFactory.getSkill(2311003).getEffect(chr.getSkillLevel(2311003)).applyToBuff(chr, Integer.MAX_VALUE);
                                    chr.dropMessage(5, "거침?");
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "WindBooster") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(5121009) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(5121009), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(5121009)) {
                                    SkillFactory.getSkill(5121009).getEffect(chr.getSkillLevel(5121009)).applyToBuff(chr, Integer.MAX_VALUE);
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "SharpEyes") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(3121002) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(3121002), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(3121002)) {
                                    SkillFactory.getSkill(3121002).getEffect(chr.getSkillLevel(3121002)).applyToBuff(chr, Integer.MAX_VALUE);
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "MagicGuard") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(2001002) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(2001002), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(2001002)) {
                                    SkillFactory.getSkill(2001002).getEffect(chr.getSkillLevel(2001002)).applyToBuff(chr, Integer.MAX_VALUE);
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "CrossOverChain") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(1311015) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(1311015), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(1311015)) {
                                    SkillFactory.getSkill(1311015).getEffect(chr.getSkillLevel(1311015)).applyToBuff(chr, Integer.MAX_VALUE);
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "PrayOfAria") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(24121004) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(24121004), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(24121004)) {
                                    SkillFactory.getSkill(24121004).getEffect(chr.getSkillLevel(24121004)).applyToBuff(chr, Integer.MAX_VALUE);
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(207720, "Screw") == 1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(400051015) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(400051015), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(400051015)) {
                                    SkillFactory.getSkill(400051015).getEffect(chr.getSkillLevel(400051015)).applyToBuff(chr, Integer.MAX_VALUE);
                                }
                            }
                        }
                    }

                    if (chr.getMapId() != ServerConstants.WarpMap) {
                        if (chr.getKeyValue(51384, "ww_buck") != -1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(5321054) >= 1)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(5321054), (byte) 1, (byte) 1);
                                }
                                if (!chr.getBuffedValue(5321054)) {
                                    SkillFactory.getSkill(5321054).getEffect(chr.getSkillLevel(5321054)).applyTo(chr, false);
                                }
                            }
                        }
                        if (chr.getKeyValue(51384, "ww_magu") != -1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(2001002) >= 10)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(2001002), (byte) 10, (byte) 10);
                                }
                                if (!chr.getBuffedValue(2001002)) {
                                    SkillFactory.getSkill(2001002).getEffect(chr.getSkillLevel(2001002)).applyTo(chr, false);
                                }
                            }
                        }
                        if (chr.getKeyValue(51384, "ww_holy") != -1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(2311003) >= 20)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(2311003), (byte) 20, (byte) 20);
                                }
                                if (!chr.getBuffedValue(2311003)) {
                                    SkillFactory.getSkill(2311003).getEffect(chr.getSkillLevel(2311003)).applyTo(chr, false);
                                }
                            }
                        }
                        if (chr.getKeyValue(51384, "ww_sharp") != -1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(3121002) >= 20)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(3121002), (byte) 20, (byte) 20);
                                }
                                if (!chr.getBuffedValue(3121002)) {
                                    SkillFactory.getSkill(3121002).getEffect(chr.getSkillLevel(3121002)).applyTo(chr, false);
                                }
                            }
                        }
                        if (chr.getKeyValue(51384, "ww_winb") != -1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(5121009) >= 20)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(5121009), (byte) 20, (byte) 20);
                                }
                                if (!chr.getBuffedValue(5121009)) {
                                    SkillFactory.getSkill(5121009).getEffect(chr.getSkillLevel(5121009)).applyTo(chr, false);
                                }
                            }
                        }
                        if (chr.getKeyValue(51384, "ww_sakro") != -1) {
                            if (!(chr.getStat().getHp() <= 0)) {
                                if (!(chr.getSkillLevel(1221054) >= 20)) {
                                    chr.changeSingleSkillLevel(SkillFactory.getSkill(1221054), (byte) 20, (byte) 20);
                                }
                                if (!chr.getBuffedValue(1221054)) {
                                    SkillFactory.getSkill(1221054).getEffect(chr.getSkillLevel(1221054)).applyTo(chr, false);
                                }
                            }
                        }
                    }

                    if (chr.getBuffedEffect(MapleBuffStat.IceAura) != null) { // ICE_AURA
                        MapleMonster mob = null;
                        List<MapleMapObject> objs = chr.getMap().getMapObjectsInRange(chr.getPosition(), 10000, Arrays.asList(MapleMapObjectType.MONSTER));
                        Iterator<MapleMapObject> objz = objs.iterator();
                        while (objz.hasNext()) {
                            mob = (MapleMonster) objz.next();
                            if (mob.isAlive()) {
                                if (mob.getFreezingOverlap() < 5 && (time - chr.lastFreezeTime) >= 3000) { // 3초마다로 잡고..
                                    chr.lastFreezeTime = time;
                                    mob.setFreezingOverlap((byte) (mob.getFreezingOverlap() + 1));
                                    MapleStatEffect eff = SkillFactory.getSkill(2221054).getEffect(1);
                                    MonsterStatusEffect effect = new MonsterStatusEffect(2221054, eff.getDuration());
                                    mob.applyStatus(chr.getClient(), MonsterStatus.MS_Speed, effect, eff.getV(), eff);
                                }
                            }
                        }
                    }
                    if (chr.getBuffedValue(MapleBuffStat.Infinity) != null) {
                        if ((time - chr.lastInfinityTime) >= 4000 && chr.getInfinity() < 25) { // 4초마다, 25스택 이하일 시
                            chr.setInfinity((byte) (chr.getInfinity() + 1)); // 인피니티 1스택 추가
                            chr.setBuffedValue(MapleBuffStat.Infinity, chr.getInfinity());
                            MapleStatEffect effect = chr.getBuffedEffect(MapleBuffStat.Infinity);

                            chr.addHP((long) (chr.getStat().getMaxHp() * 0.1)); // HP 10% 회복
                            chr.addMP((long) (chr.getStat().getMaxMp() * 0.1)); // MP 10% 회복
                            chr.lastInfinityTime = System.currentTimeMillis();

                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                            statups.put(MapleBuffStat.Infinity, new Pair<>((int) chr.getInfinity(), (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.Infinity))));

                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                        }
                    }

                    if (chr.getBuffedValue(400001043)) {
                        if ((time - chr.lastCygnusTime) >= 4000) { // 4초마다, 25스택 이하일 시
                            MapleStatEffect eff = chr.getBuffedEffect(400001043);
                            int value = chr.getBuffedValue(MapleBuffStat.IndieDamR, 400001043);
                            if (value < eff.getW()) {
                                chr.setBuffedValue(MapleBuffStat.Infinity, 400001043, value + eff.getDamage());

                                chr.addHP((long) (chr.getStat().getMaxHp() * eff.getY() / 100)); // HP 회복
                                chr.lastCygnusTime = System.currentTimeMillis();

                                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                statups.put(MapleBuffStat.IndieDamR, new Pair<>(value + eff.getDamage(), (int) chr.getBuffLimit(400001043)));

                                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, chr));
                            }
                        }
                    }

                    if (chr.getBuffedValue(400001044)) {
                        if ((time - chr.lastCygnusTime) >= 4000) { // 4초마다, 25스택 이하일 시
                            MapleStatEffect eff = chr.getBuffedEffect(400001044);
                            int value = chr.getBuffedValue(MapleBuffStat.IndieDamR, 400001044);
                            if (value < eff.getW()) {
                                chr.setBuffedValue(MapleBuffStat.IndieDamR, 400001044, value + eff.getDamage());

                                chr.addHP((long) (chr.getStat().getMaxHp() * eff.getY() / 100)); // HP 회복
                                chr.lastCygnusTime = System.currentTimeMillis();

                                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                statups.put(MapleBuffStat.IndieDamR, new Pair<>(value + eff.getDamage(), (int) chr.getBuffLimit(400001044)));

                                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, chr));
                            }
                        }
                    }

                    if (chr.getSkillLevel(31110009) > 0) {
                        if (time - chr.lastDrainAuraTime >= 4000) {
                            chr.lastDrainAuraTime = time;
                            chr.addMP(SkillFactory.getSkill(31110009).getEffect(chr.getSkillLevel(31110009)).getY(), true);
                        }
                    }

                    if (chr.getSkillLevel(32101009) > 0) {
                        if (time - chr.lastDrainAuraTime >= 4000) {
                            chr.lastDrainAuraTime = time;
                            chr.addHP(SkillFactory.getSkill(32101009).getEffect(chr.getSkillLevel(32101009)).getHp());
                        }
                    }

                    if (chr.getBuffedValue(5121054) && (time - chr.lastChargeEnergyTime) >= 5000) {
                        if (chr.getBuffedEffect(MapleBuffStat.EnergyCharged) == null) {
                            if (chr.getSkillLevel(5120018) > 0) {
                                SkillFactory.getSkill(5120018).getEffect(chr.getSkillLevel(5120018)).applyTo(chr, false);
                            } else if (chr.getSkillLevel(5110014) > 0) {
                                SkillFactory.getSkill(5110014).getEffect(chr.getSkillLevel(5110014)).applyTo(chr, false);
                            } else {
                                SkillFactory.getSkill(5100015).getEffect(chr.getSkillLevel(5100015)).applyTo(chr, false);
                            }
                        }

                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

                        MapleStatEffect energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);

                        int max = energyCharge.getZ(); // 에너지 차지에 도달하기 까지 최대 값

                        if (!chr.energyCharge) { // 에너지 충전 상태이다.
                            chr.lastChargeEnergyTime = time;
                            chr.energy = Math.min(max, chr.energy + chr.getBuffedEffect(5121054).getX());
                            if (chr.energy == max) { // 에너지를 다 채웠다면
                                chr.energyCharge = true; // 에너지 차지상태로 넘어간다.
                                chr.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeActived(true);
                            } else {
                                chr.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeCooling(false);
                                chr.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeActived(false);
                            }

                            statups.put(MapleBuffStat.EnergyCharged, new Pair<>(chr.energy, 0));

                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.EnergyCharged), chr));
                            chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, chr.getBuffedEffect(MapleBuffStat.EnergyCharged)), false);
                        }
                    }

                    if (chr.getBuffedValue(400021032)) {
                        MapleStatEffect angel = SkillFactory.getSkill(400021032).getEffect(chr.getSkillLevel(400021032));

                        if (chr.lastAngelTime == 0) {
                            chr.lastAngelTime = System.currentTimeMillis();
                        }

                        if (time - chr.lastAngelTime >= angel.getX() * 1000) {
                            if (chr.getStat().getHp() > 0) {
                                chr.lastAngelTime = System.currentTimeMillis();
                                chr.addHP(chr.getStat().getCurrentMaxHp() * angel.getY() / 100);
                                SkillFactory.getSkill(400021052).getEffect(chr.getSkillLevel(400021032)).applyTo(chr, false);
                            }
                        }
                    }

                    if (_overloadMode == null) {
                        _overloadMode = (Skill) SkillFactory.getSkill(400041029);
                    }

                    // HealPassive
                    if (chr.getSkillLevel(5100013) > 0) {
                        MapleStatEffect endurance = SkillFactory.getSkill(5100013).getEffect(chr.getSkillLevel(5100013));

                        if (chr.lastHealTime == 0) {
                            chr.lastHealTime = System.currentTimeMillis();
                        }

                        if (time - chr.lastHealTime >= endurance.getW() * 1000) {
                            chr.lastHealTime = System.currentTimeMillis();
                            chr.addMPHP(chr.getStat().getCurrentMaxHp() * endurance.getX() / 100, chr.getStat().getCurrentMaxMp(chr) * endurance.getX() / 100);
                        }
                    } else if (chr.getSkillLevel(11110025) > 0) {
                        MapleStatEffect willofSteal = SkillFactory.getSkill(11110025).getEffect(chr.getSkillLevel(11110025));

                        if (chr.lastHealTime == 0) {
                            chr.lastHealTime = System.currentTimeMillis();
                        }

                        if (time - chr.lastHealTime >= willofSteal.getW() * 1000) {
                            chr.lastHealTime = System.currentTimeMillis();
                            chr.addHP(chr.getStat().getCurrentMaxHp() * willofSteal.getY() / 100);
                        }
                    } else if (chr.getSkillLevel(51110000) > 0) {
                        MapleStatEffect selfRecovery = SkillFactory.getSkill(51110000).getEffect(chr.getSkillLevel(51110000));

                        if (chr.lastHealTime == 0) {
                            chr.lastHealTime = System.currentTimeMillis();
                        }

                        if (time - chr.lastHealTime >= 4000) {
                            chr.lastHealTime = System.currentTimeMillis();
                            chr.addMPHP(selfRecovery.getHp(), selfRecovery.getMp());
                        }
                    } else if (chr.getSkillLevel(61110006) > 0) {
                        MapleStatEffect selfRecovery = SkillFactory.getSkill(61110006).getEffect(chr.getSkillLevel(61110006));

                        if (chr.lastHealTime == 0) {
                            chr.lastHealTime = System.currentTimeMillis();
                        }

                        if (time - chr.lastHealTime >= selfRecovery.getW() * 1000) {
                            chr.lastHealTime = System.currentTimeMillis();
                            chr.addMPHP(chr.getStat().getCurrentMaxHp() * selfRecovery.getX() / 100, chr.getStat().getCurrentMaxMp(chr) * selfRecovery.getX() / 100);
                        }
                    }

                    //SummonAttack
                    if (chr.getBuffedValue(400011077)) {
                        MapleStatEffect effect = SkillFactory.getSkill(400011077).getEffect(chr.getSkillLevel(400011077));

                        if (chr.lastNemeaAttackTime == 0) {
                            chr.lastNemeaAttackTime = System.currentTimeMillis();
                        }

                        for (MapleSummon summon : chr.getSummons()) {
                            if (summon.getSkill() == 400011077 && (time - chr.lastNemeaAttackTime >= effect.getX() * 1000)) {
                                chr.lastNemeaAttackTime = System.currentTimeMillis();
                                chr.getMap().broadcastMessage(SummonPacket.DeathAttack(summon));
                            } else if (summon.getSkill() == 400011078 && (time - chr.lastGerionAttackTime >= effect.getZ() * 1000)) {
                                chr.lastGerionAttackTime = System.currentTimeMillis();
                                chr.getMap().broadcastMessage(SummonPacket.DeathAttack(summon));
                            }
                        }
                    }

                    // StackBuff
                    managementStackBuff(chr, time);

                    if (chr.getBuffedValue(400041029)) {
                        MapleStatEffect effect = _overloadMode.getEffect(chr.getSkillLevel(400041029));

                        int consumeMP = (int) (chr.getStat().getMaxMp() * (effect.getQ() / 100.0D)) + effect.getY();

                        if (chr.getStat().getMp() < consumeMP) {
                            chr.cancelEffectFromBuffStat(MapleBuffStat.Overload);
                        } else {
                            chr.addMP(-consumeMP);
                        }
                    }

                    if (chr.getBuffedValue(11111023)) {
                        MapleStatEffect effect = SkillFactory.getSkill(11111023).getEffect(chr.getSkillLevel(11111023));
                        if (effect.makeChanceResult()) {
                            List<MapleMapObject> objs = chr.getMap().getMapObjectsInRange(chr.getPosition(), 10000, Arrays.asList(MapleMapObjectType.MONSTER));
                            Iterator<MapleMapObject> objz = objs.iterator();
                            while (objz.hasNext()) {
                                MapleMonster mob = (MapleMonster) objz.next();
                                if (mob.isAlive()) {
                                    MonsterStatusEffect eff = new MonsterStatusEffect(11101023, effect.getDuration());
                                    mob.applyStatus(chr.getClient(), MonsterStatus.MS_TrueSight, eff, effect.getV(), effect);
                                }
                            }
                        }
                    }

                    if (chr.getBuffedEffect(MapleBuffStat.KinesisPsychicOver) != null) {
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        if (chr.PPoint < 30) {
                            chr.PPoint++;
                            statups.put(MapleBuffStat.KinesisPsychicPoint, new Pair<>(chr.PPoint, 0));

                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.KinesisPsychicOver), chr));
                        }

                    }

                    MapleBuffStat[] mpCons = {MapleBuffStat.YellowAura, MapleBuffStat.DrainAura, MapleBuffStat.BlueAura, MapleBuffStat.DarkAura, MapleBuffStat.DebuffAura, MapleBuffStat.IceAura, MapleBuffStat.FireAura};

                    for (MapleBuffStat mpCon : mpCons) {
                        if (chr.getBuffedValue(mpCon) != null) {
                            chr.addMP(-chr.getBuffedEffect(mpCon).getMPCon());
                        }
                    }

                    //핑크빈 요요
                    if (chr.getSkillLevel(131001010) > 0) {
                        MapleStatEffect effect = SkillFactory.getSkill(131001011).getEffect(chr.getSkillLevel(130001010));

                        if (chr.lastYoyoTime == 0) {
                            chr.lastYoyoTime = System.currentTimeMillis();

                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                            statups.put(MapleBuffStat.PinkbeanYoYoStack, new Pair<>(chr.getYoyoCount(), 0));

                            effect.setDuration(0);

                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                        }

                        if (time - chr.lastYoyoTime >= 7000) {
                            if (chr.getYoyoCount() < 8) {
                                chr.setYoyoCount(chr.getYoyoCount() + 1);

                                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                statups.put(MapleBuffStat.PinkbeanYoYoStack, new Pair<>(chr.getYoyoCount(), 0));

                                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                            }

                            chr.lastYoyoTime = time;
                        }
                    }

                    if (chr.getSkillLevel(2100009) > 0) {
                        byte stack = 0;
                        MapleStatEffect drain;
                        //   if (chr.getSkillLevel(2120014) > 0) {
                        //       drain = SkillFactory.getSkill(2120014).getEffect(1);
                        //    } else {
                        drain = SkillFactory.getSkill(2100009).getEffect(1);
                        // }
                        Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();
                        while (mobs.hasNext()) {
                            MapleMonster mob = mobs.next();
                            Iterator<Ignition> lp = mob.getIgnitions().iterator();//.getStati().entrySet().iterator();
                            while (lp.hasNext()) {
                                Ignition zz = lp.next();
                                if (zz.getOwnerId() == chr.getId()) {
                                    if (!(stack >= 5)) {
                                        stack++;
                                    }
                                }
                                if (stack == 5) {
                                    break;
                                }
                            }
                            if (stack == 5) {
                                break;
                            }
                        }

                        chr.setPoisonStack(stack);

                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        statups.put(MapleBuffStat.DotBasedBuff, new Pair<>((int) stack, 0));
                        drain.setDuration(0);

                        if (stack > 0) {
                            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, drain, chr));
                        } else {
                            chr.getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(statups, chr));
                        }
                    }

                    if (chr.getSkillLevel(2300009) > 0) {
                        MapleStatEffect blessingAnsanble;
                        if (chr.getSkillLevel(2320013) > 0) {
                            blessingAnsanble = SkillFactory.getSkill(2320013).getEffect(1);
                        } else {
                            blessingAnsanble = SkillFactory.getSkill(2300009).getEffect(1);
                        }
                        if (chr.getBuffedEffect(MapleBuffStat.BlessingAnsanble) == null || (chr.getBuffedValue(MapleBuffStat.BlessingAnsanble) != (chr.getBlessingAnsanble() * blessingAnsanble.getX()))) {
                            blessingAnsanble.applyTo(chr, false);
                        }
                    }

                    if (chr.getSkillLevel(400031057) > 0 && chr.getStartRelicUnboundTime() != 0) {
                        long relict = System.currentTimeMillis() - chr.getStartRelicUnboundTime();
                        int secs = 3500;
                        if (relict >= secs) {
                            for (MapleSummon s : chr.getMap().getAllSummonsThreadsafe()) {
                                if (s.getOwner().getId() == chr.getId()) {
                                    if (s.getSkill() == 400031051) {
                                        chr.setStartRelicUnboundTime(System.currentTimeMillis());
                                        chr.getClient().getSession().writeAndFlush(CField.TigerSpecialAttack(chr.getId(), s.getObjectId(), s.getSkill()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
