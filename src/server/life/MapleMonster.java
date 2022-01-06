/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.life;

import client.*;
import client.MapleTrait.MapleTraitType;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import scripting.EventInstanceManager;
import server.*;
import server.Timer;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.field.boss.lotus.MapleEnergySphere;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.polofritto.MapleRandomPortal;
import tools.Pair;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.MobPacket;
import tools.packet.MobPacket.BossLucid;
import tools.packet.MobPacket.BossWill;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import server.Timer.MapTimer;
import tools.packet.SLFCGPacket;

public class MapleMonster extends AbstractLoadedMapleLife {

    private MapleMonsterStats stats;
    private ChangeableStats ostats = null;
    private long hp, nextKill = 0, lastDropTime = 0, barrier = 0;
    private int mp;
    private byte carnivalTeam = -1, phase = 0, bigbangCount = 0;
    private MapleMap map;
    private WeakReference<MapleMonster> sponge = new WeakReference<MapleMonster>(null);
    private int linkoid = 0, lastNode = -1, highestDamageChar = 0, linkCID = 0; // Just a reference for monster EXP distribution after dead
    private WeakReference<MapleCharacter> controller = new WeakReference<MapleCharacter>(null);
    private boolean fake = false;
    private boolean dropsDisabled = false;
    private boolean controllerHasAggro = false;
    private boolean demianChangePhase = false;
    private boolean extreme = false;
    private final List<AttackerEntry> attackers = new CopyOnWriteArrayList<>();
    private EventInstanceManager eventInstance;
    private MonsterListener listener = null;
    private byte[] reflectpack = null, nodepack = null;
    private Map<MonsterStatus, MonsterStatusEffect> stati = new ConcurrentHashMap<MonsterStatus, MonsterStatusEffect>();
    private final LinkedList<MonsterStatusEffect> poisons = new LinkedList<MonsterStatusEffect>();
    private Map<MobSkill, Long> usedSkills = new HashMap<MobSkill, Long>();
    private int stolen = -1, seperateSoul = 0, airFrame = 0; //monster can only be stolen ONCE
    private boolean shouldDropItem = false, killed = false, isseperated = false, isMobGroup = false, isSkillForbid = false, useSpecialSkill = false;
    private long lastReceivedMovePacket = System.currentTimeMillis();
    private long spawnTime = 0;
    private long lastBindTime = 0;
    private long lastCriticalBindTime = 0;

    private long lastSpecialAttackTime = System.currentTimeMillis(), lastSeedCountedTime = System.currentTimeMillis(), lastStoneTime = System.currentTimeMillis(), lastSpawnBlindMobtime = System.currentTimeMillis();
    public long lastDistotionTime = System.currentTimeMillis(), lastCapTime = 0, astObstacleTime = System.currentTimeMillis(), lastChainTime = System.currentTimeMillis(), lastThunderTime = System.currentTimeMillis(), lastEyeTime = System.currentTimeMillis();
    private int nextSkill = 0, nextSkillLvl = 0, freezingOverlap = 0, curseBound = 0;
    private int owner = -1, scale = 100, eliteGrade = -1, eliteType = 0, spiritGate = 0, anotherByte = 0;
    private List<Integer> spawnList = new ArrayList<>(), willHplist = new ArrayList<>();
    private List<Ignition> ignitions = new CopyOnWriteArrayList<>();
    private List<MapleEnergySphere> spheres = new ArrayList<>();
    private List<Pair<Integer, Integer>> eliteGradeInfo = new ArrayList<>();
    private ScheduledFuture<?> schedule = null;
    public long lastObstacleTime = System.currentTimeMillis(), lastBWBliThunder = System.currentTimeMillis(), lastBWThunder = System.currentTimeMillis(), lastLaserTime = System.currentTimeMillis(), lastRedObstacleTime = System.currentTimeMillis(), lastSpearTime = System.currentTimeMillis();

    private int SerenTimetype;
    private int SerenNoonTotalTime, SerenSunSetTotalTime, SerenMidNightSetTotalTime, SerenDawnSetTotalTime, SerenNoonNowTime, SerenSunSetNowTime, SerenMidNightSetNowTime, SerenDawnSetNowTime;
    private long shield = 0L, shieldmax = 0L;
    private Map<Integer, SkillCustomInfo> customInfo = new LinkedHashMap<>();

    public MapleMonster(final int id, final MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(final int id, final MapleMonsterStats stats, boolean extreme) {
        super(id);
        initWithStats(stats, extreme);
    }

    public MapleMonster(final MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    public double bonusHp() {
        int level = stats.getLevel();

        double bonus = 1;

        return bonus;
    }

    private final void initWithStats(final MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        //if (stats.getLevel())
        hp = (long) (stats.getHp() * bonusHp());
        mp = stats.getMp();
    }

    private final void initWithStats(final MapleMonsterStats stats, boolean extreme) {
        setStance(5);
        this.stats = stats;
        this.extreme = extreme;
        hp = (long) (stats.getHp() * bonusHp());
        mp = stats.getMp();
    }

    public final List<AttackerEntry> getAttackers() {
        if (attackers == null || attackers.size() <= 0) {
            return new ArrayList<AttackerEntry>();
        }
        List<AttackerEntry> ret = new ArrayList<AttackerEntry>();
        for (AttackerEntry e : attackers) {
            if (e != null) {
                ret.add(e);
            }
        }
        return ret;
    }

    public long getLastReceivedMovePacket() {
        return lastReceivedMovePacket;
    }

    public void receiveMovePacket() {
        lastReceivedMovePacket = System.currentTimeMillis();
    }

    public final MapleMonsterStats getStats() {
        return stats;
    }

    public final void disableDrops() {
        this.dropsDisabled = true;
    }

    public final boolean dropsDisabled() {
        return dropsDisabled;
    }

    public final void setSponge(final MapleMonster mob) {
        sponge = new WeakReference<MapleMonster>(mob);
        if (linkoid <= 0) {
            linkoid = mob.getObjectId();
        }
    }

    public final void setMap(final MapleMap map) {
        this.map = map;
        startDropItemSchedule();
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int id) {
        owner = id;
    }

    public final long getHp() {
        return hp;
    }

    public final void setHp(long hp) {
        this.hp = hp;
    }

    public final void addHp(long hp, boolean brodcast) {
        this.hp = getHp() + hp;
        if (this.hp > getStats().getHp()) {
            this.hp = getStats().getHp();
        }
        if (brodcast) {
            getMap().broadcastMessage(MobPacket.showBossHP(this));
        }
        if (this.hp <= 0L) {
            this.map.killMonster(this, this.controller.get(), true, false, (byte) 1, 0);
        }
    }

    public final ChangeableStats getChangedStats() {
        return ostats;
    }

    public final long getMobMaxHp() {
//        if (ostats != null) {
        //          return ostats.hp;
        //    }
        return stats.getHp();
    }

    public final int getMp() {
        return mp;
    }

    public final void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public final int getMobMaxMp() {
        if (ostats != null) {
            return ostats.mp;
        }
        return stats.getMp();
    }

    public final long getMobExp() {
        if (ostats != null) {
            return ostats.exp;
        }
        return stats.getExp();
    }

    public final void setOverrideStats(final OverrideMonsterStats ostats) {
        this.ostats = new ChangeableStats(stats, ostats);
        this.hp = ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final void changeLevel(final int newLevel) {
        changeLevel(newLevel, true);
    }

    public final void changeLevel(final int newLevel, boolean pqMob) {
        if (!stats.isChangeable()) {
            return;
        }
        this.ostats = new ChangeableStats(stats, newLevel, pqMob);
        this.hp = ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final MapleMonster getSponge() {
        return sponge.get();
    }

    public final void damage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
        damage(from, damage, updateAttackTime, 0);
    }

    public final void damage(final MapleCharacter from, long damage, final boolean updateAttackTime, final int lastSkill) {
        if (from == null || damage <= 0 || !isAlive()) {
            return;
        }

        int 방어구[] = {1003244, 1082753, 1073535, 1042295, 1062283, 1103428};
        int 장신구[] = {1190303, 1122810, 1152209, 1114325};
        int acount = 0;
        int bcount = 0;
        int per = 0;

        for (int i = 0; i < 방어구.length; i++) {
            if (from.hasEquipped(방어구[i])) {
                bcount++;
            }
        }
        for (int i = 0; i < 장신구.length; i++) {
            if (from.hasEquipped(장신구[i])) {
                acount++;
            }
        }
        if (acount == 장신구.length) {
            per += 5;
        }
        if (bcount == 방어구.length) {
            per += 5;
        }

        if (from.getKeyValue(12345, "damage") > 0) {
            per += from.getKeyValue(12345, "damage");
        }

        damage += (long) (damage * (per / 100));
        if (per > 0) {
            from.dropMessage(-1, "자신의 데미지 " + per + "% 적용 중, 실제 데미지 : " + damage);
        }

        AttackerEntry attacker = null;

        MapleMonster linkMob = map.getMonsterById(stats.getHpLinkMob());
        if (linkMob != null) {
            linkMob.damage(from, damage, updateAttackTime, lastSkill);
            return;
        }

        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId());
        } else {
            attacker = new SingleAttackerEntry(from);
        }
        boolean replaced = false;
        for (final AttackerEntry aentry : getAttackers()) {
            if (aentry != null && aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }

        if (lastSkill == 400021069 && getStats().isBoss() && !from.getBuffedValue(32121056)) {
            MapleStatEffect reaper = from.getBuffedEffect(400021069);
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            int localDuration = (int) (from.getBuffLimit(400021069) + reaper.getZ() * 1000);

            MapleBuffStatValueHolder mbsvh = from.checkBuffStatValueHolder(MapleBuffStat.IndieSummon, 400021069);
            mbsvh.localDuration = localDuration;

            final CancelEffectAction cancelAction = new CancelEffectAction(from, reaper, System.currentTimeMillis(), MapleBuffStat.IndieSummon);
            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                cancelAction.run();
            }, localDuration);

            mbsvh.schedule.cancel(false);

            mbsvh.schedule = schedule;
            statups.put(MapleBuffStat.IndieSummon, new Pair<>(400021069, localDuration));
            from.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, reaper, from));
        }

        if (this.getId() == 9390612 || this.getId() == 9390610 || this.getId() == 9390911 || this.getId() == 8645066) {
            if (from.getKeyValue(200106, "golrux_in") == 1) {
                from.setKeyValue(200106, "golrux_dmg", (from.getKeyValue(200106, "golrux_dmg") + damage) + "");
            }
        }
        if (getId() == 8880305) {
            List<Obstacle> obs = new ArrayList<>();
            if (Randomizer.isSuccess(20)) {
                for (int i = 0; i < Randomizer.rand(1, 3); i++) {
                    int key = Randomizer.rand(63, 64);
                    int x = Randomizer.nextInt(1200) - 600;
                    int y = getTruePosition().y > 0 ? -2020 : 159;
                    Obstacle ob = new Obstacle(key, new Point(x, y - 601), new Point(x, y), 0x28, key == 0x40 ? 0 : 0x3C, 0x4B8, 0x6F, 3, 0x257);
                    obs.add(ob);
                }
                map.broadcastMessage(MobPacket.createObstacle(this, obs, (byte) 0));
            }
        }

        if (lastSkill == 4201004 && getStats().isBoss()) {
            Item item = new Item(2431850, (short) 1, (short) 1, 0);
            map.spawnItemDrop(this, from, item, getTruePosition(), true, false);
        }

        if ((from.getBuffedEffect(MapleBuffStat.Reincarnation) != null) && !stats.isBoss()) {
            if (from.getReinCarnation() > 0) {
                from.setReinCarnation(from.getReinCarnation() - 1);
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.Reincarnation, new Pair<>(1, (int) from.getBuffLimit(from.getBuffSource(MapleBuffStat.Reincarnation))));
                from.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, from.getBuffedEffect(MapleBuffStat.Reincarnation), from));
            }
        }

        if (getSeperateSoul() > 0) {
            if (from.getMap() != null) {
                if (from.getMap().getMonsterByOid(getSeperateSoul()) != null) {
                    from.getMap().getMonsterByOid(getSeperateSoul()).damage(from, damage, updateAttackTime);
                }
            }
            return;
        }

        attacker.addDamage(from, damage, updateAttackTime);

        NumberFormat Number = NumberFormat.getInstance();
        if (getMobMaxHp() > Integer.MAX_VALUE) {
            if (getId() == 9300800) {
                from.DamageMeter += damage;
                from.dropMessage(-1, "누적 데미지 : " + from.DamageMeter);
                from.getClient().getSession().writeAndFlush(CField.getGameMessage(9, "누적 데미지 : " + from.DamageMeter));
            }
        }
        if (from.getMapId() == 120000102) {
            from.setDamageMeter(from.getDamageMeter() + damage);
        }
        Pair<MobSkill, Integer> cap = from.getDisease(MapleBuffStat.CapDebuff);
        if (cap != null) {
            if ((cap.right == 100 && (getId() == 8900001 || getId() == 8900101)) || (cap.right == 200 && (getId() == 8900002 || getId() == 8900102))) {
                if (hp < stats.getHp() * bonusHp()) {
                    hp = (long) Math.min(hp + damage, stats.getHp() * bonusHp());
                    if (sponge.get() == null && hp > 0) {
                        switch (stats.getHPDisplayType()) {
                            case 0:
                                map.broadcastMessage(MobPacket.showBossHP(this), this.getTruePosition());
                                break;
                            case 1:
                                map.broadcastMessage(from, MobPacket.damageFriendlyMob(this, 1, true), false);
                                break;
                            case 2:
                                map.broadcastMessage(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                break;
                            case 3:
                                for (final AttackerEntry mattacker : getAttackers()) {
                                    if (mattacker != null) {
                                        for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                                            if (cattacker != null && cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map of the monster
                                                if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                                    cattacker.getAttacker().getClient().getSession().writeAndFlush(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                        }
                    }
                }
                return;
            }
        }

        //데미안 강제 2페이즈 전환
        if (getHPPercent() <= 30 && (getId() == 8880100 || getId() == 8880110)) {

            if (!demianChangePhase) {
                map.removeAllFlyingSword();
                map.broadcastMessage(CField.enforceMSG("데미안이 완전한 어둠을 손에 넣었습니다.", 216, 30000000));
                map.broadcastMessage(MobPacket.ChangePhaseDemian(this, 0x4F));
                demianChangePhase = true;

                Timer.MapTimer.getInstance().schedule(() -> {
                    map.killMonster(this, from, false, false, (byte) 1);
                }, 5000);
            }
            return;
        }
        if (stats.getSelfD() != -1) {
            hp -= damage;
            if (hp > 0) {
                if (hp < stats.getSelfDHp()) { // HP is below the selfd level
                    map.killMonster(this, from, false, false, stats.getSelfD(), lastSkill);
                } else { // Show HP
                    for (final AttackerEntry mattacker : getAttackers()) {
                        for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                            if (cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map of the monster
                                if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                    cattacker.getAttacker().getClient().getSession().writeAndFlush(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                }
                            }
                        }
                    }
                }
            } else { // Character killed it without explosing :(
                map.killMonster(this, from, true, false, (byte) 1, lastSkill);
            }
        } else {
            if (sponge.get() != null) {
                if (sponge.get().hp > 0) { // If it's still alive, dont want double/triple rewards
                    // Sponge are always in the same map, so we can use this.map
                    // The only mob that uses sponge are PB/HT
                    sponge.get().hp -= damage;

                    if (sponge.get().hp <= 0) {
                        map.broadcastMessage(MobPacket.showBossHP(sponge.get().getId(), -1, (long) (sponge.get().getMobMaxHp() * bonusHp())));
                        map.killMonster(sponge.get(), from, true, false, (byte) 1, lastSkill);
                    } else {
                        map.broadcastMessage(MobPacket.showBossHP(sponge.get()));
                    }
                }
            }
            if (hp > 0) {
                if (barrier > 0) {
                    if (barrier >= damage) {
                        barrier -= damage;
                    } else {
                        barrier = 0;
                        hp -= (damage - barrier);
                    }
                } else {
                    hp -= damage;
                }
                if (eventInstance != null) {
                    eventInstance.monsterDamaged(from, this, damage);
                } else {
                    final EventInstanceManager em = from.getEventInstance();
                    if (em != null) {
                        em.monsterDamaged(from, this, damage);
                    }
                }

                if (getId() == 8880303 || getId() == 8880304) {
                    MapleMonster will = map.getMonsterById(8880300);

                    if (getHPPercent() <= 67 && will.getWillHplist().contains(666)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 67 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));
                        return;
                    } else if (getHPPercent() <= 34 && will.getWillHplist().contains(333)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 34 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));
                        return;
                    } else if (getHPPercent() <= 1 && will.getWillHplist().contains(3)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 1 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));
                        return;
                    }
                } else if (getId() == 8880301 || getId() == 8880341) {
                    if (getHPPercent() <= 51 && getWillHplist().contains(503)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 51 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));

                        if (!isSkillForbid) {
                            if (schedule != null) {
                                schedule.cancel(true);
                            }
                            MobSkill msi = MobSkillFactory.getMobSkill(242, 7);
                            msi.applyEffect(null, this, true, isFacingLeft());
                        }
                        return;
                    } else if (getHPPercent() <= 1 && getWillHplist().contains(3)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 1 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));

                        if (!isSkillForbid) {
                            if (schedule != null) {
                                schedule.cancel(true);
                            }
                            MobSkill msi = MobSkillFactory.getMobSkill(242, 7);
                            msi.applyEffect(null, this, true, isFacingLeft());
                        }
                        return;
                    }
                } else if (getId() == 8880343 || getId() == 8880344) {
                    MapleMonster will = map.getMonsterById(8880340);
                    if (getHPPercent() <= 67 && will.getWillHplist().contains(666)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 67 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));
                        return;
                    } else if (getHPPercent() <= 34 && will.getWillHplist().contains(333)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 34 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));
                        return;
                    } else if (getHPPercent() <= 1 && will.getWillHplist().contains(3)) {
                        setHp((long) (getMobMaxHp() * bonusHp() * 1 / 100));
                        map.broadcastMessage(MobPacket.showBossHP(this));
                        return;
                    }
                }
                if (getStats().isMobZone()) {
                    byte phase;
                    if (getHPPercent() > 75) {
                        phase = 1;
                    } else if (getHPPercent() > 50) {
                        phase = 2;
                    } else if (getHPPercent() > 25) {
                        phase = 3;
                    } else {
                        phase = 4;
                    }

                    if (getId() != 8644650 && getId() != 8644655 && getId() != 8644658 && getId() != 8644659) {  //더스크는 이거안해!
                        if (this.phase != phase) {
                            setPhase(phase);
                        }
                        map.broadcastMessage(MobPacket.changePhase(this));
                        map.broadcastMessage(MobPacket.changeMobZone(this));
                    }

                }

                if (hp > 0) {
                    int count = (100 - getHPPercent()) / 10;
                    if (map.getLucidCount() + map.getLucidUseCount() < count && (this.getId() == 8880140 || this.getId() == 8880141 || this.getId() == 8880150 || this.getId() == 8880151)) {
                        if (map.getLucidCount() < 3) {
                            map.setLucidCount(map.getLucidCount() + 1);
                            map.broadcastMessage(BossLucid.changeStatueState(false, map.getLucidCount(), false));
                            map.broadcastMessage(CField.enforceMSG("나팔동상 근처에서 '채집'키를 눌러 사용하면 루시드의 힘을 억제할 수 있습니다!", 222, 2000));
                        }
                    }
                    switch (getId()) {
                        case 8880300:
                        case 8880303:
                        case 8880304: {
                            MapleMonster will = map.getMonsterById(8880300);
                            if (will != null) {
                                List<Integer> hps = will.getWillHplist();
                                map.broadcastMessage(BossWill.setWillHp(hps, map, 8880300, 8880303, 8880304));
                            }
                            break;
                        }
                        case 8880340:
                        case 8880343:
                        case 8880344: {
                            MapleMonster will = map.getMonsterById(8880340);
                            if (will != null) {
                                List<Integer> hps = will.getWillHplist();
                                map.broadcastMessage(BossWill.setWillHp(hps, map, 8880340, 8880343, 8880344));
                            }
                            break;
                        }
                        case 8880301:
                        case 8880341:
                            map.broadcastMessage(BossWill.setWillHp(getWillHplist()));
                            break;
                    }
                }
                if (sponge.get() == null && hp > 0) {
                    switch (stats.getHPDisplayType()) {
                        case 0:
                            map.broadcastMessage(MobPacket.showBossHP(this), this.getTruePosition());
                            break;
                        case 1:
                            map.broadcastMessage(from, MobPacket.damageFriendlyMob(this, 1, true), false);
                            break;
                        case 2:
                            map.broadcastMessage(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                            break;
                        case 3:
                            for (final AttackerEntry mattacker : getAttackers()) {
                                if (mattacker != null) {
                                    for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                                        if (cattacker != null && cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map of the monster
                                            if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                                cattacker.getAttacker().getClient().getSession().writeAndFlush(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }

                if (barrier > 0) {
                    if (getId() % 10 == 5 || getId() % 10 == 2) {
                        map.broadcastMessage(MobPacket.mobBarrier(this.getObjectId(), (int) (barrier / 10000000000L)));
                    } else {
                        map.broadcastMessage(MobPacket.mobBarrier(this.getObjectId(), (int) (barrier / 30000000000L)));
                    }
                }

                if (hp <= 0) {
                    if (stats.getHPDisplayType() == 0) {
                        map.broadcastMessage(MobPacket.showBossHP(getId(), -1, (long) (getMobMaxHp() * bonusHp())), this.getTruePosition());
                    }
                    map.killMonster(this, from, true, false, (byte) 1, lastSkill);
                    if (from.getMonsterCombo() == 0) {
                        from.setMonsterComboTime(System.currentTimeMillis());
                    }
                    if (from.getKeyValue(16700, "count") < 300) {
                        from.setKeyValue(16700, "count", String.valueOf(from.getKeyValue(16700, "count") + 1));
                    }
                    if ((System.currentTimeMillis() - from.getMonsterComboTime()) > 8000) {
                        from.setMonsterCombo((short) 0);
                    } else {
                        if (from.getMonsterCombo() < 50000) {
                            from.addMonsterCombo((short) 1);
                        }
                        if (from.getV("d_combo") == null) {
                            from.addKV("d_combo", "" + from.getMonsterCombo());
                        } else if (from.getMonsterCombo() > Long.parseLong(from.getV("d_combo"))) {
                            from.addKV("d_combo", "" + from.getMonsterCombo());
                        }
                        from.getClient().getSession().writeAndFlush(InfoPacket.comboKill(from.getMonsterCombo(), getObjectId()));
                        from.setMonsterComboTime(System.currentTimeMillis());

                        if (from.getMonsterCombo() % 50 == 0) {
                            int itemId;
                            if (from.getMonsterCombo() < 350) {
                                itemId = 2023484;
                            } else if (from.getMonsterCombo() < 750) {
                                itemId = 2023494;
                            } else if (from.getMonsterCombo() < 2000) {
                                itemId = 2023495;
                            } else {
                                itemId = 2023669;
                            }
                            map.spawnMobPublicDrop(new Item(itemId, (byte) 0, (short) 1, (byte) 0), getTruePosition(), this, from, (byte) 0, 0);
                        }
                    }
                }
            }
        }
        startDropItemSchedule();
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / (getMobMaxHp() * bonusHp()));
    }

    public double getHPPercentDouble() {
        return Math.ceil((hp * 100.0) / (getMobMaxHp() * bonusHp()));
    }

    public final void heal(int hp, int mp, final boolean broadcast) {
        final long TotalHP = getHp() + hp;
        final int TotalMP = getMp() + mp;

        if (TotalHP >= getMobMaxHp()) {
            setHp(getMobMaxHp());
        } else {
            setHp(TotalHP);
        }
        if (TotalMP >= getMp()) {
            setMp(getMp());
        } else {
            setMp(TotalMP);
        }
        if (broadcast) {
            map.broadcastMessage(MobPacket.healMonster(getObjectId(), hp));
        } else if (sponge.get() != null) { // else if, since only sponge doesn't broadcast
            sponge.get().hp += hp;
        }
    }

    public final void killed() {
        for (Entry<MonsterStatus, MonsterStatusEffect> skill : stati.entrySet()) {
            if (skill.getValue().getSchedule() != null && !skill.getValue().getSchedule().isDone()) {
                skill.getValue().getSchedule().cancel(true);
            }
        }
        if (listener != null) {
            listener.monsterKilled();
        }
        if (getSchedule() != null) {
            getSchedule().cancel(true);
        }

        listener = null;
    }

    private final void giveExpToCharacter(final MapleCharacter attacker, long exp, final boolean highestDamage, final int numExpSharers, final byte pty, final byte Class_Bonus_EXP_PERCENT, final byte Premium_Bonus_EXP_PERCENT, final int lastskillID) {

        if ((getId() >= 9830000 && getId() <= 9830018) || (getId() >= 9831000 && getId() <= 9831014)) {
            //마리당 0.01%씩 주기
            exp = (int) (GameConstants.getExpNeededForLevel(attacker.getLevel()) * 0.0001);
        }

        int linkMobs[] = {9010152, 9010153, 9010154, 9010155, 9010156, 9010157, 9010158, 9010159, 9010160, 9010161, 9010162, 9010163, 9010164, 9010165, 9010166, 9010167,
            9010168, 9010169, 9010170, 9010171, 9010172, 9010173, 9010174, 9010175, 9010176, 9010177, 9010178, 9010179, 9010180, 9010181};

        for (int linkMob : linkMobs) {
            if (getId() == linkMob) {
                double plus = 0.0001;
                plus *= (275 - attacker.getLevel()) * 0.025;
                exp = (int) (GameConstants.getExpNeededForLevel(attacker.getLevel()) * plus);
            }
        }

        if (exp > 0) {

            if (attacker.hasDisease(MapleBuffStat.Curse)) {
                exp /= 2;
            }
            byte cleric = 0;
            if (attacker.getParty() != null) {
                for (MaplePartyCharacter member : attacker.getParty().getMembers()) {
                    if (member.getJobId() == 230 || member.getJobId() == 231 || member.getJobId() == 232) {
                        if (member.getId() != attacker.getId() && (member.getLevel() - attacker.getLevel()) < 15 && (member.getLevel() - attacker.getLevel()) > -15) {
                            cleric++;
                        }
                    }
                }
            }
            if (cleric > 3) {
                cleric = 3;
            }
            exp += exp * (cleric * 0.2);

            exp *= attacker.getClient().getChannelServer().getExpRate();

            if (exp > 0) {
                // 레벨별 경험치 기본 50배 가정 (배율)
                if (attacker.getLevel() >= 0 && attacker.getLevel() < 200) { 
                    exp *= 450;
                } else if (attacker.getLevel() >= 200 && attacker.getLevel() < 210) { 
                    exp *= 12;
                } else if (attacker.getLevel() >= 210 && attacker.getLevel() < 220) { 
                    exp *= 8;
                } else if (attacker.getLevel() >= 220 && attacker.getLevel() < 230) { 
                    exp *= 6;
                } else if (attacker.getLevel() >= 230 && attacker.getLevel() < 240) { 
                    exp *= 20;
                } else if (attacker.getLevel() >= 240 && attacker.getLevel() < 250) { 
                    exp *= 24;
                } else if (attacker.getLevel() >= 250 && attacker.getLevel() < 275) { 
                    exp *= 20;
                } else if (attacker.getLevel() >= 275 && attacker.getLevel() < 300) { 
                    exp *= 20;
                } else {
                    exp *= 0;
                }
            }

            if (!attacker.getBuffedValue(80002282)) {
                exp -= exp * attacker.getMap().getRuneCurseDecrease() / 100;
            }

            if (isBuffed(MonsterStatus.MS_SeperateSoulP) || isBuffed(MonsterStatus.MS_SeperateSoulC)) {
                exp *= 2;
            }

            attacker.getTrait(MapleTraitType.charisma).addExp(stats.getCharismaEXP(), attacker);
            attacker.gainExpMonster(exp, true, highestDamage, stati);
        }
    }

    public final int killBy(final MapleCharacter killer, final int lastSkill) {
        if (killed) {
            return 1;
        }
        killed = true;
        long totalBaseExp = getMobExp();
        AttackerEntry highest = null;
        long highdamage = 0;
        final List<AttackerEntry> list = getAttackers();
        for (final AttackerEntry attackEntry : list) {
            if (attackEntry != null && attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        for (final AttackerEntry attackEntry : list) {
            if (attackEntry != null) {
//                baseExp = (int) Math.ceil(totalBaseExp * ((double) attackEntry.getDamage() / getMobMaxHp()));
                attackEntry.killedMob(getMap(), totalBaseExp, attackEntry == highest, lastSkill);
            }
        }

        final MapleCharacter controll = controller.get();
        if (controll != null) { // this can/should only happen when a hidden gm attacks the monster
            controll.getClient().getSession().writeAndFlush(MobPacket.stopControllingMonster(getObjectId()));
            controll.stopControllingMonster(this);
        }

        if (killer != null && killer.getPosition() != null && !(getId() >= 8644101 && getId() <= 8644112)) {
            // killer.AddStarDustPoint(5, getTruePosition());
        }

        if (!FieldLimitType.Event.check(map.getFieldLimit())) {
            if (getStats().getLevel() >= killer.getLevel() - 30 && getStats().getLevel() <= killer.getLevel() + 30) {
                if (Randomizer.nextInt(10000) < 4 && map.getPoloFrittoPortal() == null && map.getFireWolfPortal() == null) {
                    MapleMap target = killer.getClient().getChannelServer().getMapFactory().getMap(993000000);
                    MapleMap target2 = killer.getClient().getChannelServer().getMapFactory().getMap(993000100);
                    MapleRandomPortal portal;
                    if (target.characterSize() == 0 || target2.characterSize() == 0) {
                        portal = new MapleRandomPortal(2, getTruePosition(), map.getId(), killer.getId(), Randomizer.nextBoolean());
                         map.spawnRandomPortal(portal);
                    } else {
                        portal = new MapleRandomPortal(2, getTruePosition(), map.getId(), killer.getId(), false);
                        map.spawnRandomPortal(portal);
                    }
                }

                if (Randomizer.nextInt(10000) < 4 && map.getFireWolfPortal() == null && map.getPoloFrittoPortal() == null ) {
                    MapleRandomPortal portal = new MapleRandomPortal(3, getTruePosition(), map.getId(), killer.getId(), false);
                    map.spawnRandomPortal(portal);
                }
            }
        }

        if (killer.getMapId() == 993000500) { //불늑 격파
            MapleMap target = ChannelServer.getInstance(killer.getClient().getChannel()).getMapFactory().getMap(993000600);
            for (MapleCharacter chr : killer.getMap().getAllChracater()) {
                chr.changeMap(target, target.getPortal(0));
                chr.setFWolfKiller(true);
                if (chr.getQuestStatus(16407) == 1) {
                    chr.forceCompleteQuest(16407);
                }
            }
        }

        spawnRevives(getMap());
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
            eventInstance = null;
        }
//        if (killer != null && killer.getPyramidSubway() != null) {
        //          killer.getPyramidSubway().onKill(killer);
        //    }
        hp = 0;
        MapleMonster oldSponge = getSponge();
        sponge = new WeakReference<MapleMonster>(null);
        if (oldSponge != null && oldSponge.isAlive()) {
            boolean set = true;
            for (MapleMapObject mon : map.getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mon;
                if (mons.isAlive() && mons.getObjectId() != oldSponge.getObjectId() && mons.getStats().getLevel() > 1 && mons.getObjectId() != this.getObjectId() && (mons.getSponge() == oldSponge || mons.getLinkOid() == oldSponge.getObjectId())) { //sponge was this, please update
                    set = false;
                    break;
                }
            }
            if (set) { //all sponge monsters are dead, please kill off the sponge
                map.killMonster(oldSponge, killer, true, false, (byte) 1);
            }
        }

        reflectpack = null;
        nodepack = null;
        if (stati.size() > 0) {
            List<MonsterStatus> statuses = new LinkedList<MonsterStatus>(stati.keySet());
            for (MonsterStatus ms : statuses) {
//                cancelStatus(ms);
            }
            statuses.clear();
        }
        //attackers.clear();
        cancelDropItem();
        int v1 = highestDamageChar;
        this.highestDamageChar = 0; //reset so we dont kill twice
        return v1;
    }

    public final void spawnRevives(final MapleMap map) {
        final List<Integer> toSpawn = stats.getRevives();

        if (toSpawn == null || this.getLinkCID() > 0) {
            return;
        }
        MapleMonster spongy = null;
        switch (getId()) {
            case 8820002:
            case 8820003:
            case 8820004:
            case 8820005:
            case 8820006:
            case 8840000:
            case 6160003:
            case 8850011:
            case 8820102:
            case 8820103:
            case 8820104:
            case 8820105:
            case 8820106:
                break;
            case 8810026:
            case 8810130:
            case 8820008:
            case 8820009:
            case 8820010:
            case 8820011:
            case 8820012:
            case 8820013:
            case 8820108:
            case 8820109:
            case 8820110:
            case 8820111:
            case 8820112:
            case 8820113: {
                final List<MapleMonster> mobs = new ArrayList<MapleMonster>();

                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    mob.setPosition(getTruePosition());
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    switch (mob.getId()) {
                        case 8810018: // Horntail Sponge
                        case 8810122:
                        case 8820009: // PinkBeanSponge0
                        case 8820010: // PinkBeanSponge1
                        case 8820011: // PinkBeanSponge2
                        case 8820012: // PinkBeanSponge3
                        case 8820013: // PinkBeanSponge4
                        case 8820014: // PinkBeanSponge5
                        case 8820109: // ChaosPinkBeanSponge0
                        case 8820110: // ChaosPinkBeanSponge1
                        case 8820111: // ChaosPinkBeanSponge2
                        case 8820112: // ChaosPinkBeanSponge3
                        case 8820113: // ChaosPinkBeanSponge4
                        case 8820114: // ChaosPinkBeanSponge5
                            spongy = mob;
                            break;
                        default:
                            mobs.add(mob);
                            break;
                    }
                }
                if (spongy != null && map.getMonsterById(spongy.getId()) == null) {
                    map.spawnMonster(spongy, -2);

                    for (final MapleMonster i : mobs) {
                        map.spawnMonster(i, -2);
                        i.setSponge(spongy);
                    }
                }
                break;
            }
            case 8820114:
            case 8820014: {
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getTruePosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnMonster(mob, -2);
                }
                break;
            }
            default: {
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getTruePosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnRevives(mob, this.getObjectId());
                }
                break;
            }
        }
    }

    public final boolean isAlive() {
        return hp > 0;
    }

    public final void setCarnivalTeam(final byte team) {
        carnivalTeam = team;
    }

    public final byte getCarnivalTeam() {
        return carnivalTeam;
    }

    public final MapleCharacter getController() {
        return controller.get();
    }

    public final void setController(final MapleCharacter controller) {
        this.controller = new WeakReference<MapleCharacter>(controller);
    }

    public final void switchController(final MapleCharacter newController, final boolean immediateAggro) {
        final MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        } else if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().getSession().writeAndFlush(MobPacket.stopControllingMonster(getObjectId()));
            sendStatus(controllers.getClient());
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
    }

    public final void addListener(final MonsterListener listener) {
        this.listener = listener;
    }

    public final boolean isControllerHasAggro() {
        return controllerHasAggro;
    }

    public final void setControllerHasAggro(final boolean controllerHasAggro) {
        this.controllerHasAggro = controllerHasAggro;
    }

    public final void sendStatus(final MapleClient client) {
        if (reflectpack != null) {
            client.getSession().writeAndFlush(reflectpack);
        }
        if (stati.size() > 0) {
            Map<MonsterStatus, MonsterStatusEffect> statiz = stati;
            if (stati.containsKey(MonsterStatus.MS_Burned)) {
                statiz.remove(MonsterStatus.MS_Burned);
            }
            client.getSession().writeAndFlush(MobPacket.applyMonsterStatus(this, statiz, true, null));
        }
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
        if (!isAlive() || owner >= 0 && owner != client.getPlayer().getId()) {
            return;
        }
        client.getSession().writeAndFlush(MobPacket.spawnMonster(this, fake && linkCID <= 0 ? -4 : -1, 0));
        sendStatus(client);
        if (map != null && !stats.isEscort() && client.getPlayer() != null && client.getPlayer().getTruePosition().distanceSq(getTruePosition()) <= GameConstants.maxViewRangeSq_Half()) {
            map.updateMonsterController(this);
        }
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        if (stats.isEscort() && getEventInstance() != null && lastNode >= 0) { //shammos
            map.resetShammos(client);
        } else {
            client.getSession().writeAndFlush(MobPacket.killMonster(getObjectId(), 0));
            if (getController() != null && client.getPlayer() != null && client.getPlayer().getId() == getController().getId()) {
                client.getPlayer().stopControllingMonster(this);
            }
        }
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(stats.getName());
        sb.append("(");
        sb.append(getId());
        sb.append(") (Level ");
        sb.append(stats.getLevel());
        sb.append(") at (X");
        sb.append(getTruePosition().x);
        sb.append("/ Y");
        sb.append(getTruePosition().y);
        sb.append(") with ");
        sb.append(getHp());
        sb.append("/ ");
        sb.append(getMobMaxHp());
        sb.append("hp, ");
        sb.append(getMp());
        sb.append("/ ");
        sb.append(getMobMaxMp());
        sb.append(" mp, oid: ");
        sb.append(getObjectId());
        sb.append(" || Controller : ");
        final MapleCharacter chr = controller.get();
        sb.append(chr != null ? chr.getName() : "none");

        return sb.toString();
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public final EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public final void setEventInstance(final EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public final ElementalEffectiveness getEffectiveness(final Element e) {
//        if (stati.size() > 0 && stati.containsKey(MonsterStatus.DOOM)) {
        //          return ElementalEffectiveness.NORMAL; // like blue snails
        //    }
        return stats.getEffectiveness(e);
    }

    public final void setTempEffectiveness(final Element e, final long milli) {
        stats.setEffectiveness(e, ElementalEffectiveness.WEAK);
        EtcTimer.getInstance().schedule(new Runnable() {

            public void run() {
                stats.removeEffectiveness(e);
            }
        }, milli);
    }

    public final boolean isBuffed(final MonsterStatus status) {
        return stati.containsKey(status);
    }

    public final boolean isBuffed(int skillid) {
        for (Entry<MonsterStatus, MonsterStatusEffect> skill : stati.entrySet()) {
            if (skill.getValue().getSkill() == skillid) {
                return true;
            }
        }
        return false;
    }

    public final MonsterStatusEffect getBuff(final MonsterStatus status) {
        return stati.get(status);
    }

    public final MonsterStatusEffect getBuff(int skillid) {
        for (Entry<MonsterStatus, MonsterStatusEffect> skill : stati.entrySet()) {
            if (skill.getValue().getSkill() == skillid) {
                return skill.getValue();
            }
        }
        return null;
    }

    public final int getBurnedBuffSize(int skillid) {
        int size = 0;
        for (Entry<MonsterStatus, MonsterStatusEffect> skill : stati.entrySet()) {
            if (skill.getValue().getSkill() == skillid && skill.getKey() == MonsterStatus.MS_Burned) {
                size++;
            }
        }
        return size;
    }

    public final int getBurnedBuffSize() {
        int size = 0;
        for (Entry<MonsterStatus, MonsterStatusEffect> skill : stati.entrySet()) {
            if (skill.getKey() == MonsterStatus.MS_Burned) {
                size++;
            }
        }
        return size;
    }

    public final int getStatiSize() {
        return stati.size() + (poisons.size() > 0 ? 1 : 0);
    }

    public final Map<MonsterStatus, MonsterStatusEffect> getAllDebuffs() {
        Map<MonsterStatus, MonsterStatusEffect> ret = new HashMap<MonsterStatus, MonsterStatusEffect>();
        for (Entry<MonsterStatus, MonsterStatusEffect> e : stati.entrySet()) {
            ret.put(e.getKey(), e.getValue());
        }
        return ret;
    }

    public final ArrayList<MonsterStatusEffect> getAllBuffs() {
        ArrayList<MonsterStatusEffect> ret = new ArrayList<MonsterStatusEffect>();
        for (MonsterStatusEffect e : stati.values()) {
            ret.add(e);
        }
        synchronized (poisons) {
            for (MonsterStatusEffect e : poisons) {
                ret.add(e);
            }
        }
        return ret;
    }

    public final void setFake(final boolean fake) {
        this.fake = fake;
    }

    public final boolean isFake() {
        return fake;
    }

    public final MapleMap getMap() {
        return map;
    }

    public final List<MobSkill> getSkills() {
        return stats.getSkills();
    }

    public final boolean hasSkill(final int skillId, final int level) {
        return stats.hasSkill(skillId, level);
    }

    public final long getLastSkillUsed(final int skillId, final int skillLevel) {
        for (Entry<MobSkill, Long> kvp : usedSkills.entrySet()) {
            if (kvp.getKey().getSkillId() == skillId && kvp.getKey().getSkillLevel() == skillLevel) {
                return kvp.getValue();
            }
        }
        return 0;
    }

    public final void setLastSkillUsed(MobSkill msi, final long now, long cooltime) {
        usedSkills.put(msi, now + (cooltime * (msi.getSkillId() == 203 && msi.getSkillLevel() == 1 ? 2 : 1)));
    }

    public final byte getNoSkills() {
        return stats.getNoSkills();
    }

    public final boolean isFirstAttack() {
        return stats.isFirstAttack();
    }

    public final int getBuffToGive() {
        return stats.getBuffToGive();
    }

    public void applyStatus(final MapleClient c, Map<MonsterStatus, MonsterStatusEffect> datas, MapleStatEffect effect) {
        if (c.getPlayer().getSkillLevel(261) > 0
                || c.getPlayer().getSkillLevel(262) > 0
                || c.getPlayer().getSkillLevel(263) > 0
                || c.getPlayer().getSkillLevel(80002770) > 0
                || c.getPlayer().getSkillLevel(80002771) > 0
                || c.getPlayer().getSkillLevel(80002772) > 0
                || c.getPlayer().getSkillLevel(80002773) > 0) {
            int skilllevel = 0;

            skilllevel = c.getPlayer().getSkillLevel(261) > 0 ? c.getPlayer().getSkillLevel(261)
                    : c.getPlayer().getSkillLevel(262) > 0 ? c.getPlayer().getSkillLevel(262)
                    : c.getPlayer().getSkillLevel(263) > 0 ? c.getPlayer().getSkillLevel(263)
                    : c.getPlayer().getSkillLevel(80002770) > 0 ? c.getPlayer().getSkillLevel(80002770)
                    : c.getPlayer().getSkillLevel(80002771) > 0 ? c.getPlayer().getSkillLevel(80002771)
                    : c.getPlayer().getSkillLevel(80002772) > 0 ? c.getPlayer().getSkillLevel(80002772)
                    : c.getPlayer().getSkillLevel(80002773);

            if (c.getPlayer().lastSkill() + 20000 <= System.currentTimeMillis()) {
                c.getSession().writeAndFlush(CField.skillCooldown(80002770, 20000));
                SkillFactory.getSkill(80002770).getEffect(c.getPlayer().getSkillLevel(skilllevel)).applyTo(c.getPlayer(), false, 10000);
                MapTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        c.getSession().writeAndFlush(CField.skillCooldown(80002770, 0));
                    }
                }, 19000);
                c.getPlayer().lastskill = System.currentTimeMillis();
            }
        }
        for (Entry<MonsterStatus, MonsterStatusEffect> data : datas.entrySet()) {
            if (data.getKey() == MonsterStatus.MS_Burned) {
                int dotSuperpos = 0;
                ArrayList<Ignition> list = new ArrayList<>();
                for (Ignition ig : getIgnitions()) {
                    if (ig != null && (ig.getStartTime() + ig.getDuration()) < System.currentTimeMillis()) {
                        list.add(ig);
                    } else {
                        if (ig.getSkill() == data.getValue().getSkill()) {
                            dotSuperpos++;
                        }
                    }
                }
                list.stream().forEach((igs) -> {
                    getIgnitions().remove(igs);
                });
                // c.getPlayer().dropMessageGM(6, "[MS_Burned] ignitions = " + getIgnitions().size() + ", dotsuperpos = " + effect.getDotSuperpos() + " , dotSuperpos = " + dotSuperpos);
                if ((effect.getDotSuperpos() == 0 && dotSuperpos <= 0) || (dotSuperpos < effect.getDotSuperpos())) {
                    if (c.getPlayer().getSkillLevel(2110000) > 0) {
                        MapleStatEffect extremeMagic = SkillFactory.getSkill(2110000).getEffect(c.getPlayer().getSkillLevel(2110000));
                        data.getValue().setDuration(data.getValue().getDuration() * (100 + extremeMagic.getX()) / 100);
                    }
                    ScheduledFuture<?> timer = null;
                    if (effect.getDotInterval() > 0 && data.getValue().getValue() > 0) {
                        data.getValue().setLastPoisonTime(System.currentTimeMillis());
                        data.getValue().setInterval(effect.getDotInterval());
                    }
                    //   c.getPlayer().dropMessageGM(6, "[MS_Burned] [skillid=" + data.getValue().getSkill() + ", value=" + data.getValue().getValue() + ", Interval=" + effect.getDotInterval() + ", Duration=" + data.getValue().getDuration() + "]");
                    getIgnitions().add(new Ignition(c.getPlayer().getId(), data.getValue().getSkill(), data.getValue().getValue(), effect.getDotInterval(), data.getValue().getDuration())
                    );
                    if (data.getValue().getDuration() < 0) {
                        cancelStatus(MonsterStatus.MS_Burned, data.getValue().getSkill());
                    }
                }
            }

            if (data.getKey() == MonsterStatus.MS_SeperateSoulP) {
                final MapleMonster mob = MapleLifeFactory.getMonster(getId()); //
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, getPosition());
                mob.setSeperateSoul(getObjectId()); //
                mob.applyStatus(c, MonsterStatus.MS_SeperateSoulC, data.getValue(), (int) data.getValue().getValue(), effect);
                mob.applyStatus(c, MonsterStatus.MS_Stun, data.getValue(), (int) data.getValue().getValue(), effect);
                if (effect.getDuration() > 0) {
                    Timer.MobTimer.getInstance().schedule(() -> {
                        try {
                            c.getPlayer().getMap().killMonster(mob);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, effect.getDuration()); // delay
                } else {
                    c.getPlayer().getMap().killMonster(mob);
                }
            }

            CancelStatusAction action = new CancelStatusAction(this, data.getKey());

            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                action.run();
            }, data.getValue().getDuration());

            data.getValue().setSchedule(schedule);
            data.getValue().setChr(c.getPlayer());

            if (stati.containsKey(data.getKey())) {
                cancelStatus(data.getKey(), effect.getSourceId());
            }

            if (getStats().isBoss() && (/*datas.containsKey(MonsterStatus.MS_Stun) || */(isResist() && data.getKey() == MonsterStatus.MS_Freeze && effect.getSourceId() != 100001283))) {
                continue;
            } else if (!isResist() && data.getKey() == MonsterStatus.MS_Freeze && effect.getSourceId() != 101120110 && effect.getSourceId() != 100001283) {
                c.getPlayer().dropMessageGM(6, data.getValue().getSkill() + " 스킬로 " + getObjectId() + " 몬스터에게 " + (data.getValue().getDuration() / 1000) + "초 동안 " + data.getKey() + " 상태이상 적용 . 값 : " + data.getValue().getValue());
                stati.put(data.getKey(), data.getValue());
            } else {
                c.getPlayer().dropMessageGM(6, data.getValue().getSkill() + " 스킬로 " + getObjectId() + " 몬스터에게 " + (data.getValue().getDuration() / 1000) + "초 동안 " + data.getKey() + " 상태이상 적용 . 값 : " + data.getValue().getValue());
                stati.put(data.getKey(), data.getValue());
            }
        }

        if (effect.getSourceId() != 101120110 && effect.getSourceId() != 100001283) {
            if (datas.containsKey(MonsterStatus.MS_Freeze)) {
                if (isResist()) {
                    datas.remove(MonsterStatus.MS_Freeze);
                    c.getSession().writeAndFlush(MobPacket.monsterResist(this, c.getPlayer(), (int) (90 - ((System.currentTimeMillis() - lastBindTime) / 1000)), effect.getSourceId()));
                } else {
                    setResist(System.currentTimeMillis());
                }
            }
        }

//        if (getStats().isBoss() && datas.containsKey(MonsterStatus.MS_Stun)) {
//            datas.remove(MonsterStatus.MS_Stun);
//        }

        map.broadcastMessage(MobPacket.applyMonsterStatus(this, datas, false, effect));
    }

    public void applyStatus(final MapleClient c, MonsterStatus status, MonsterStatusEffect effect, int value, MapleStatEffect eff) {
        if (getStati().containsKey(MonsterStatus.MS_PCounter) || getStati().containsKey(MonsterStatus.MS_MCounter) || getStati().containsKey(MonsterStatus.MS_PImmune) || getStati().containsKey(MonsterStatus.MS_MImmune)) {
            return;
        }
        if ((status == MonsterStatus.MS_Stun || status == MonsterStatus.MS_Seal) && getStats().isBoss() && !getStati().containsKey(MonsterStatus.MS_SeperateSoulC)) {
            return;
        }

        if (c == null || effect == null || eff == null) {
            return;
        }
        if (c.getPlayer().getSkillLevel(261) > 0
                || c.getPlayer().getSkillLevel(262) > 0
                || c.getPlayer().getSkillLevel(263) > 0
                || c.getPlayer().getSkillLevel(80002770) > 0
                || c.getPlayer().getSkillLevel(80002771) > 0
                || c.getPlayer().getSkillLevel(80002772) > 0
                || c.getPlayer().getSkillLevel(80002773) > 0) {
            int skilllevel = 0;

            skilllevel = c.getPlayer().getSkillLevel(261) > 0 ? c.getPlayer().getSkillLevel(261)
                    : c.getPlayer().getSkillLevel(262) > 0 ? c.getPlayer().getSkillLevel(262)
                    : c.getPlayer().getSkillLevel(263) > 0 ? c.getPlayer().getSkillLevel(263)
                    : c.getPlayer().getSkillLevel(80002770) > 0 ? c.getPlayer().getSkillLevel(80002770)
                    : c.getPlayer().getSkillLevel(80002771) > 0 ? c.getPlayer().getSkillLevel(80002771)
                    : c.getPlayer().getSkillLevel(80002772) > 0 ? c.getPlayer().getSkillLevel(80002772)
                    : c.getPlayer().getSkillLevel(80002773);

            if (c.getPlayer().lastSkill() + 20000 <= System.currentTimeMillis()) {
                c.getSession().writeAndFlush(CField.skillCooldown(80002770, 20000));
                SkillFactory.getSkill(80002770).getEffect(c.getPlayer().getSkillLevel(skilllevel)).applyTo(c.getPlayer(), false, 10000);
                MapTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        c.getSession().writeAndFlush(CField.skillCooldown(80002770, 0));
                    }
                }, 19000);
                c.getPlayer().lastskill = System.currentTimeMillis();
            }
        }
        CancelStatusAction action = new CancelStatusAction(this, status);

        ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
            action.run();
        }, effect.getDuration());

        if (stati.containsKey(status)) {
            cancelStatus(status, effect.getSkill());
        }

        effect.setSchedule(schedule);

        stati.put(status, effect);
        effect.setValue(value);
        effect.setStati(status);
        effect.setChr(c.getPlayer());
        effect.setCid(c.getPlayer().getId());
        if (status == MonsterStatus.MS_Burned) {
            int dotSuperpos = 0;
            ArrayList<Ignition> list = new ArrayList<>();
            for (Ignition ig : getIgnitions()) {
                if (ig != null && (ig.getStartTime() + ig.getDuration()) < (System.currentTimeMillis() % 1000000000)) {
                    list.add(ig);
                } else if (ig.getSkill() == effect.getSkill()) {
                    dotSuperpos++;
                }
            }
            for (Ignition igs : list) {
                getIgnitions().remove(igs);
            }
            if (!(dotSuperpos >= eff.getDotSuperpos())) {
                ScheduledFuture<?> timer = null;
                if (eff.getDotInterval() > 0 && effect.getValue() > 0) {
                    effect.setLastPoisonTime(System.currentTimeMillis());
                    effect.setInterval(eff.getDotInterval());
                    /*                	timer = MobTimer.getInstance().register(() -> {
                     if (getHp() > 1) {
                     damage(c.getPlayer(), Math.min(getHp() - 1, effect.getValue()), true);
                     if (eff.getSourceId() == 25121006) {
                     c.getPlayer().addHP(effect.getValue() * eff.getX() / 100);
                     }
                     }
                     }, eff.getDotInterval());*/
                }
                getIgnitions().add(new Ignition(c.getPlayer().getId(), effect.getSkill(), effect.getValue(), eff.getDotInterval(), effect.getDuration())
                );
                if (effect.getDuration() < 0) {
                    cancelStatus(MonsterStatus.MS_Burned, effect.getSkill());
                }
            }
        }
        if (status == MonsterStatus.MS_SeperateSoulP) {
            final MapleMonster mob = MapleLifeFactory.getMonster(getId()); //
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, getPosition());
            mob.setSeperateSoul(getObjectId()); //
            mob.applyStatus(c, MonsterStatus.MS_SeperateSoulC, effect, value, eff);
            mob.applyStatus(c, MonsterStatus.MS_Stun, effect, value, eff);
            if (effect.getDuration() > 0) {
                try {
                    Timer.MobTimer.getInstance().schedule(() -> {
                        try {
                            c.getPlayer().getMap().killMonster(mob);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, effect.getDuration()); // delay
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                c.getPlayer().getMap().killMonster(mob);
            }
        }

        Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
        applys.put(status, effect);

        map.broadcastMessage(MobPacket.applyMonsterStatus(this, applys, false, eff));

        if (effect.getDuration() < 0) {
            cancelStatus(status);
        }
    }

    public int getSeperateSoul() {
        return seperateSoul;
    }

    private void setSeperateSoul(int id) {
        this.seperateSoul = id;
    }

    public final void cancelStatus(Map<MonsterStatus, MonsterStatusEffect> cancels) {
        final MapleCharacter con = getController();
        //   map.broadcastMessage(MobPacket.cancelMonsterStatus(this, cancels), getTruePosition());

        Map<MonsterStatus, MonsterStatusEffect> cancelsf = new HashMap<MonsterStatus, MonsterStatusEffect>();

        List<Ignition> removes = new ArrayList<>();

        for (Entry<MonsterStatus, MonsterStatusEffect> cancel : cancels.entrySet()) {
            if (cancel.getKey() == MonsterStatus.MS_Burned) {

                for (Ignition ignition : getIgnitions()) {
                    long time = System.currentTimeMillis();
                    if (time >= ignition.getStartTime() + ignition.getDuration()) {
                        removes.add(ignition);
                    }
                }
                cancelsf = new HashMap<MonsterStatus, MonsterStatusEffect>();
                cancelsf.put(cancel.getKey(), cancel.getValue());
                stati.remove(cancel.getKey());
                for (Ignition remove : removes) {
                    getIgnitions().remove(remove);
                }
                continue;
            } else {
                stati.remove(cancel.getKey());
                cancelsf.put(cancel.getKey(), cancel.getValue());
            }

            ScheduledFuture<?> schedule = cancel.getValue().getSchedule();
            if (schedule != null) {
                schedule.cancel(true);
            }

            if (con != null) {
                con.dropMessageGM(6, "[CancelStatus] [skillid=" + cancel.getValue().getSkill() + "]");
            }

            if (cancel.getValue().getSkill() == 12101024 || cancel.getValue().getSkill() == 12121002) {
                map.broadcastMessage(CField.ignitionBomb(12100029, getObjectId(), getTruePosition()));
            } else if (cancel.getValue().getSkill() == 11121004) {
                map.broadcastMessage(CField.ignitionBomb(11121013, getObjectId(), getTruePosition()));
            }
        }
        map.broadcastMessage(MobPacket.cancelMonsterStatus(this, cancelsf, removes), getTruePosition());
    }

    public final void cancelStatus(final MonsterStatus stat) {
        cancelStatus(stat, 0);
    }

    public final void cancelStatus(final MonsterStatus stat, int skillid) {
        if (stat == null || stati == null || !stati.containsKey(stat)) {
            return;
        }

        final MonsterStatusEffect mse = stati.get(stat);
        if (mse == null || !isAlive()) {
            return;
        }

        final MapleCharacter con = getController();
        Map<MonsterStatus, MonsterStatusEffect> cancels = new HashMap<>();

        List<Ignition> removes = new ArrayList<>();

        if (stat == MonsterStatus.MS_Burned) {
            for (Ignition ignition : getIgnitions()) {
                if (skillid > 0) {
                    if (ignition.getSkill() == skillid) {
                        removes.add(ignition);
                    }
                }
                long time = System.currentTimeMillis();
                if (time >= ignition.getStartTime() + ignition.getDuration()) {
                    removes.add(ignition);
                }
            }
            cancels.put(stat, mse);
            stati.remove(stat);
            for (Ignition remove : removes) {
                getIgnitions().remove(remove);
            }
        } else {
            stati.remove(stat);
            cancels.put(stat, mse);
        }

        if (con != null) {
            map.broadcastMessage(con, MobPacket.cancelMonsterStatus(this, cancels, removes), getTruePosition());
            con.getClient().getSession().writeAndFlush(MobPacket.cancelMonsterStatus(this, cancels, removes));
        } else {
            map.broadcastMessage(MobPacket.cancelMonsterStatus(this, cancels), getTruePosition());
        }
        ScheduledFuture<?> schedule = mse.getSchedule();
        if (schedule != null) {
            schedule.cancel(true);
        }
        if (con != null) {
            con.dropMessageGM(6, "[CancelStatus] [skillid=" + mse.getSkill() + "]");
        }

        if (con != null) {

            if (mse.getSkill() == 12101024 || mse.getSkill() == 12121002) {
                map.broadcastMessage(CField.ignitionBomb(12100029, getObjectId(), getTruePosition()));
                map.broadcastMessage(con, EffectPacket.showEffect(con, 0, 12100029, 10, 0, 0, (byte) 0, true, getTruePosition(), null, null), false);
                con.getClient().getSession().writeAndFlush(EffectPacket.showEffect(con, 0, 12100029, 10, 0, 0, (byte) 0, false, getTruePosition(), null, null));
            } else if (mse.getSkill() == 11121004) {
                map.broadcastMessage(CField.ignitionBomb(11121013, getObjectId(), getTruePosition()));
                map.broadcastMessage(con, EffectPacket.showEffect(con, 0, 11121013, 10, 0, 0, (byte) 0, true, getTruePosition(), null, null), false);
                con.getClient().getSession().writeAndFlush(EffectPacket.showEffect(con, 0, 11121013, 10, 0, 0, (byte) 0, false, getTruePosition(), null, null));
            }
        }
    }

    public final void cancelSingleStatus(final MonsterStatusEffect stat) {
        if (stat == null || !isAlive()) {
            return;
        }
//        if (stat.getStati() != MonsterStatus.MS_Poison && stat.getStati() != MonsterStatus.MS_Burned) {
        cancelStatus(stat.getStati());
//        }
    }

    public final void dispels() {
        for (Entry<MonsterStatus, MonsterStatusEffect> stat : stati.entrySet()) {
            switch (stat.getKey()) {
                case MS_Pad:
                case MS_Pdr:
                case MS_Mad:
                case MS_Mdr:
                case MS_Acc:
                case MS_Eva:
                case MS_Speed:
                case MS_Powerup:
                case MS_Magicup:
                case MS_PGuardup:
                case MS_MGuardup:
                case MS_PImmune:
                case MS_MImmune:
                case MS_Hardskin:
                case MS_PowerImmune: {
                    if (stat.getValue().getValue() > 0) {
                        cancelStatus(stat.getKey());
                    }
                    break;
                }

            }
        }
    }

    private static class AttackingMapleCharacter {

        private MapleCharacter attacker;
        private long lastAttackTime;

        public AttackingMapleCharacter(final MapleCharacter attacker, final long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }

        public final long getLastAttackTime() {
            return lastAttackTime;
        }

        public final void setLastAttackTime(final long lastAttackTime) {
            this.lastAttackTime = lastAttackTime;
        }

        public final MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        public void addDamage(MapleCharacter from, long damage, boolean updateAttackTime);

        public long getDamage();

        public boolean contains(MapleCharacter chr);

        public void killedMob(MapleMap map, long baseExp, boolean mostDamage, int lastSkill);
    }

    private final class SingleAttackerEntry implements AttackerEntry {

        private long damage = 0;
        private int chrid;
        private long lastAttackTime;

        public SingleAttackerEntry(final MapleCharacter from) {
            this.chrid = from.getId();
        }

        @Override
        public void addDamage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
                if (updateAttackTime) {
                    lastAttackTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public final List<AttackingMapleCharacter> getAttackers() {
            final MapleCharacter chr = map.getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr, lastAttackTime));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(final MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public long getDamage() {
            return damage;
        }

        @Override
        public void killedMob(final MapleMap map, final long baseExp, final boolean mostDamage, final int lastSkill) {
            final MapleCharacter chr = map.getCharacterById(chrid);
            if (chr != null && chr.isAlive()) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1, (byte) 0, (byte) 0, (byte) 0, lastSkill);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private static final class ExpMap {

        public final long exp;
        public final byte ptysize;
        public final byte Class_Bonus_EXP;
        public final byte Premium_Bonus_EXP;

        public ExpMap(final long exp, final byte ptysize, final byte Class_Bonus_EXP, final byte Premium_Bonus_EXP) {
            super();
            this.exp = exp;
            this.ptysize = ptysize;
            this.Class_Bonus_EXP = Class_Bonus_EXP;
            this.Premium_Bonus_EXP = Premium_Bonus_EXP;
        }
    }

    private static final class OnePartyAttacker {

        public MapleParty lastKnownParty;
        public long damage;
        public long lastAttackTime;

        public OnePartyAttacker(final MapleParty lastKnownParty, final long damage) {
            super();
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {

        private long totDamage = 0;
        private final Map<Integer, OnePartyAttacker> attackers = new HashMap<Integer, OnePartyAttacker>(6);
        private int partyid;

        public PartyAttackerEntry(final int partyid) {
            this.partyid = partyid;
        }

        public List<AttackingMapleCharacter> getAttackers() {
            final List<AttackingMapleCharacter> ret = new ArrayList<AttackingMapleCharacter>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                final MapleCharacter chr = map.getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }

        private final Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            final Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<MapleCharacter, OnePartyAttacker>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                final MapleCharacter chr = map.getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }

        @Override
        public final boolean contains(final MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public final long getDamage() {
            return totDamage;
        }

        public void addDamage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
            final OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                // TODO actually this causes wrong behaviour when the party changes between attacks
                // only the last setup will get exp - but otherwise we'd have to store the full party
                // constellation for every attack/everytime it changes, might be wanted/needed in the
                // future but not now
                final OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0;
                }
            }
            totDamage += damage;
        }

        @Override
        public final void killedMob(final MapleMap map, final long baseExp, final boolean mostDamage, final int lastSkill) {
            MapleCharacter pchr, highest = null;
            long iDamage, highestDamage = 0;
            long iexp = 0;
            MapleParty party;
            double addedPartyLevel, levelMod, innerBaseExp;
            List<MapleCharacter> expApplicable;
            final Map<MapleCharacter, ExpMap> expMap = new HashMap<MapleCharacter, ExpMap>(6);
            byte Class_Bonus_EXP;
            byte Premium_Bonus_EXP, added_partyinc;

            for (final Entry<MapleCharacter, OnePartyAttacker> attacker : resolveAttackers().entrySet()) {
                party = attacker.getValue().lastKnownParty;
                addedPartyLevel = 0;
                added_partyinc = 0;
                Class_Bonus_EXP = 0;
                Premium_Bonus_EXP = 0;
                expApplicable = new ArrayList<MapleCharacter>();
                for (final MaplePartyCharacter partychar : party.getMembers()) {
                    if (attacker.getKey().getLevel() - partychar.getLevel() <= 5 || stats.getLevel() - partychar.getLevel() <= 5) {
                        pchr = map.getCharacterById(partychar.getId());
                        if (pchr != null && pchr.isAlive()) {

                            boolean enable = true;
                            int linkMobs[] = {9010152, 9010153, 9010154, 9010155, 9010156, 9010157, 9010158, 9010159, 9010160, 9010161, 9010162, 9010163, 9010164, 9010165, 9010166, 9010167,
                                9010168, 9010169, 9010170, 9010171, 9010172, 9010173, 9010174, 9010175, 9010176, 9010177, 9010178, 9010179, 9010180, 9010181};

                            for (int linkMob : linkMobs) {
                                if (getId() == linkMob && pchr.getId() != attacker.getKey().getId()) {
                                    enable = false;
                                }
                            }

                            if (enable) {
                                expApplicable.add(pchr);
                                addedPartyLevel += pchr.getLevel();

                                //                            Class_Bonus_EXP += ServerConstants.Class_Bonus_EXP(pchr.getJob());
                                if (pchr.getStat().equippedWelcomeBackRing && Premium_Bonus_EXP == 0) {
                                    Premium_Bonus_EXP = 80;
                                }
                                if (pchr.getStat().hasPartyBonus && added_partyinc < 4 && map.getPartyBonusRate() <= 0) {
                                    added_partyinc++;
                                }
                            }
                        }
                    }
                }
                iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                innerBaseExp = baseExp * ((double) iDamage / totDamage);
                if (expApplicable.size() <= 1) {
                    Class_Bonus_EXP = 0; //no class bonus if not in a party.
                }

                for (final MapleCharacter expReceiver : expApplicable) {
                    iexp = expMap.get(expReceiver) == null ? 0 : expMap.get(expReceiver).exp;
                    levelMod = expReceiver.getLevel() / addedPartyLevel * 0.4;
                    iexp += (int) Math.round(((attacker.getKey().getId() == expReceiver.getId() ? 0.6 : 0.0) + levelMod) * innerBaseExp);
                    expMap.put(expReceiver, new ExpMap(iexp, (byte) (expApplicable.size() + added_partyinc), Class_Bonus_EXP, Premium_Bonus_EXP));
                }
            }
            ExpMap expmap;
            for (final Entry<MapleCharacter, ExpMap> expReceiver : expMap.entrySet()) {
                expmap = expReceiver.getValue();
                giveExpToCharacter(expReceiver.getKey(), expmap.exp, mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expmap.ptysize, expmap.Class_Bonus_EXP, expmap.Premium_Bonus_EXP, lastSkill);
            }
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            if (partyid != other.partyid) {
                return false;
            }
            return true;
        }
    }

    public int getLinkOid() {
        return linkoid;
    }

    public void setLinkOid(int lo) {
        this.linkoid = lo;
    }

    public final Map<MonsterStatus, MonsterStatusEffect> getStati() {
        return stati;
    }

    public final int getStolen() {
        return stolen;
    }

    public final void setStolen(final int s) {
        this.stolen = s;
    }

    public final void handleSteal(MapleCharacter chr) {
        double showdown = 100.0;
        final MonsterStatusEffect mse = getBuff(MonsterStatus.MS_Showdown);
        if (mse != null) {
            showdown += mse.getValue();
        }

        Skill steal = SkillFactory.getSkill(4201004);
        final int level = chr.getTotalSkillLevel(steal), chServerrate = ChannelServer.getInstance(chr.getClient().getChannel()).getDropRate();
        if (level > 0 && !getStats().isBoss() && stolen == -1 && steal.getEffect(level).makeChanceResult()) {
            final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
            final List<MonsterDropEntry> de = mi.retrieveDrop(getId());
            if (de == null) {
                stolen = 0;
                return;
            }
            final List<MonsterDropEntry> dropEntry = new ArrayList<MonsterDropEntry>(de);
            Collections.shuffle(dropEntry);
            Item idrop;
            for (MonsterDropEntry d : dropEntry) { //set to 4x rate atm, 40% chance + 10x                                                                                                                        
                if (d.itemId > 0 && d.questid == 0 && d.itemId / 10000 != 238 && Randomizer.nextInt(999999) < (int) (10 * d.chance * chServerrate * chr.getDropMod() * (chr.getStat().dropBuff / 100.0) * (showdown / 100.0))) { //kinda op
                    if (GameConstants.getInventoryType(d.itemId) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(d.itemId) == MapleInventoryType.DECORATION) {
                        idrop = (Equip) MapleItemInformationProvider.getInstance().getEquipById(d.itemId);
                    } else {
                        idrop = new Item(d.itemId, (byte) 0, (short) (d.Maximum != 1 ? Randomizer.nextInt(d.Maximum - d.Minimum) + d.Minimum : 1), (byte) 0);
                    }
                    stolen = d.itemId;
                    map.spawnMobDrop(idrop, map.calcDropPos(getPosition(), getTruePosition()), this, chr, (byte) 0, (short) 0);
                    break;
                }
            }
        } else {
            stolen = 0; //failed once, may not go again
        }
    }

    public final void setLastNode(final int lastNode) {
        this.lastNode = lastNode;
    }

    public final int getLastNode() {
        return lastNode;
    }

    public final void cancelDropItem() {
        lastDropTime = 0;
    }

    public final void startDropItemSchedule() {
        cancelDropItem();
        if (stats.getDropItemPeriod() <= 0 || !isAlive()) {
            return;
        }
        shouldDropItem = false;
        lastDropTime = System.currentTimeMillis();
    }

    public boolean shouldDrop(long now) {
        return lastDropTime > 0 && lastDropTime + (stats.getDropItemPeriod() * 1000) < now;
    }

    public void doDropItem(long now) {
        final int itemId;
        switch (getId()) {
            case 9300061:
                itemId = 4001101;
                break;
            default: //until we find out ... what other mobs use this and how to get the ITEMID
                cancelDropItem();
                return;
        }
        if (isAlive() && map != null) {
            if (shouldDropItem) {
                map.spawnAutoDrop(itemId, getTruePosition());
            } else {
                shouldDropItem = true;
            }
        }
        lastDropTime = now;
    }

    public byte[] getNodePacket() {
        return nodepack;
    }

    public void setNodePacket(final byte[] np) {
        this.nodepack = np;
    }

    public void registerKill(final long next) {
        this.nextKill = System.currentTimeMillis() + next;
    }

    public boolean shouldKill(long now) {
        return nextKill > 0 && now > nextKill;
    }

    public int getLinkCID() {
        return linkCID;
    }

    public void setLinkCID(int lc) {
        this.linkCID = lc;
        if (lc > 0) {
//            stati.put(MonsterStatus.HYPNOTIZE, new MonsterStatusEffect(MonsterStatus.HYPNOTIZE, 60000, 30001062, null, false));
        }
    }

    public void applyMonsterBuff(MapleMap map, Map<MonsterStatus, MonsterStatusEffect> stats, MobSkill mobSkill) {
        if (getStati().containsKey(MonsterStatus.MS_PCounter) || getStati().containsKey(MonsterStatus.MS_MCounter) || getStati().containsKey(MonsterStatus.MS_PImmune) || getStati().containsKey(MonsterStatus.MS_MImmune)) {
            return;
        }
        for (Entry<MonsterStatus, MonsterStatusEffect> e : stats.entrySet()) {
            e.getValue().setLevel(mobSkill.getSkillLevel());

            CancelStatusAction action = new CancelStatusAction(this, e.getKey());

            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                action.run();
            }, e.getValue().getDuration());

            if (stati.containsKey(e.getKey())) {
                cancelStatus(e.getKey());
            }

            e.getValue().setSchedule(schedule);

            stati.put(e.getKey(), e.getValue());
        }
        map.broadcastMessage(MobPacket.applyMonsterStatus(this, stats, true, null));
        if (mobSkill.getDuration() < 0) {
            cancelStatus(stats);
        }
    }

    public int getNextSkill() {
        return nextSkill;
    }

    public void setNextSkill(int nextSkill) {
        this.nextSkill = nextSkill;
    }

    public int getNextSkillLvl() {
        return nextSkillLvl;
    }

    public void setNextSkillLvl(int nextSkillLvl) {
        this.nextSkillLvl = nextSkillLvl;
    }

    public boolean isResist() {
        return System.currentTimeMillis() - lastBindTime < 90000;
    }

    public long getResist() {
        return lastBindTime;
    }

    public void setResist(long time) {
        this.lastBindTime = time;
    }

    public int getAirFrame() {
        return airFrame;
    }

    public void setAirFrame(int airFrame) {
        this.airFrame = airFrame;
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    public void setSpawnTime(long spawnTime) {
        this.spawnTime = spawnTime;
    }

    public byte getPhase() {
        return phase;
    }

    public void setPhase(byte phase) {
        this.phase = phase;
    }

    public int getFreezingOverlap() {
        return freezingOverlap;
    }

    public void setFreezingOverlap(int freezingOverlap) {
        this.freezingOverlap = freezingOverlap;
    }

    public boolean isMobGroup() {
        return isMobGroup;
    }

    public void setMobGroup(boolean isMobGroup) {
        this.isMobGroup = isMobGroup;
    }

    public List<Integer> getSpawnList() {
        return spawnList;
    }

    public void setSpawnList(List<Integer> spawnList) {
        this.spawnList = spawnList;
    }

    public boolean isSkillForbid() {
        return isSkillForbid;
    }

    public void setSkillForbid(boolean isSkillForbid) {
        this.isSkillForbid = isSkillForbid;
    }

    public List<Integer> getWillHplist() {
        return willHplist;
    }

    public void setWillHplist(List<Integer> willHplist) {
        this.willHplist = willHplist;
    }

    public boolean isUseSpecialSkill() {
        return useSpecialSkill;
    }

    public void setUseSpecialSkill(boolean useSpecialSkill) {
        this.useSpecialSkill = useSpecialSkill;
    }

    public List<MapleEnergySphere> getSpheres() {
        return spheres;
    }

    public void setSpheres(List<MapleEnergySphere> spheres) {
        this.spheres = spheres;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getEliteGrade() {
        return eliteGrade;
    }

    public void setEliteGrade(int eliteGrade) {
        this.eliteGrade = eliteGrade;
    }

    public List<Pair<Integer, Integer>> getEliteGradeInfo() {
        return eliteGradeInfo;
    }

    public void setEliteGradeInfo(List<Pair<Integer, Integer>> eliteGradeInfo) {
        this.eliteGradeInfo = eliteGradeInfo;
    }

    public int getEliteType() {
        return eliteType;
    }

    public void setEliteType(int eliteType) {
        this.eliteType = eliteType;
    }

    public int getCurseBound() {
        return curseBound;
    }

    public void setCurseBound(int curseBound) {
        this.curseBound = curseBound;
    }

    public byte getBigbangCount() {
        return bigbangCount;
    }

    public void setBigbangCount(byte bigbangCount) {
        this.bigbangCount = bigbangCount;
    }

    public int getSpiritGate() {
        return spiritGate;
    }

    public void setSpiritGate(int spiritGate) {
        this.spiritGate = spiritGate;
    }

    public List<Ignition> getIgnitions() {
        return ignitions;
    }

    public void setIgnitions(List<Ignition> ignitions) {
        this.ignitions = ignitions;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }

    public long getBarrier() {
        return barrier;
    }

    public void setBarrier(long barrier) {
        this.barrier = barrier;
    }

    public int getAnotherByte() {
        return anotherByte;
    }

    public void setAnotherByte(int anotherByte) {
        this.anotherByte = anotherByte;
    }

    public boolean isDemianChangePhase() {
        return demianChangePhase;
    }

    public void setDemianChangePhase(boolean demianChangePhase) {
        this.demianChangePhase = demianChangePhase;
    }

    public long getLastCriticalBindTime() {
        return lastCriticalBindTime;
    }

    public void setLastCriticalBindTime(long lastCriticalBindTime) {
        this.lastCriticalBindTime = lastCriticalBindTime;
    }

    public long getLastSpecialAttackTime() {
        return lastSpecialAttackTime;
    }

    public void setLastSpecialAttackTime(long lastSpecialAttackTime) {
        this.lastSpecialAttackTime = lastSpecialAttackTime;
    }

    public boolean isExtreme() {
        return extreme;
    }

    public void setExtreme(boolean extreme) {
        this.extreme = extreme;
    }

    public long getLastStoneTime() {
        return lastStoneTime;
    }

    public void setLastStoneTime(long lastStoneTime) {
        this.lastStoneTime = lastStoneTime;
    }

    public long getLastSeedCountedTime() {
        return lastSeedCountedTime;
    }

    public void setLastSeedCountedTime(long lastSeedCountedTime) {
        this.lastSeedCountedTime = lastSeedCountedTime;
    }

    public int getSerenTimetype() {
        return SerenTimetype;
    }

    public void setSerenTimetype(int serenTimetype) {
        SerenTimetype = serenTimetype;
    }

    public int getSerenNoonTotalTime() {
        return SerenNoonTotalTime;
    }

    public void setSerenNoonTotalTime(int serenNoonTotalTime) {
        SerenNoonTotalTime = serenNoonTotalTime;
    }

    public int getSerenSunSetTotalTime() {
        return SerenSunSetTotalTime;
    }

    public void setSerenSunSetTotalTime(int serenSunSetTotalTime) {
        SerenSunSetTotalTime = serenSunSetTotalTime;
    }

    public int getSerenMidNightSetTotalTime() {
        return SerenMidNightSetTotalTime;
    }

    public void setSerenMidNightSetTotalTime(int serenMidNightSetTotalTime) {
        SerenMidNightSetTotalTime = serenMidNightSetTotalTime;
    }

    public int getSerenDawnSetTotalTime() {
        return SerenDawnSetTotalTime;
    }

    public void setSerenDawnSetTotalTime(int serenDawnSetTotalTime) {
        SerenDawnSetTotalTime = serenDawnSetTotalTime;
    }

    public int getSerenNoonNowTime() {
        return SerenNoonNowTime;
    }

    public void setSerenNoonNowTime(int serenNoonNowTime) {
        SerenNoonNowTime = serenNoonNowTime;
    }

    public int getSerenSunSetNowTime() {
        return SerenSunSetNowTime;
    }

    public void setSerenSunSetNowTime(int serenSunSetNowTime) {
        SerenSunSetNowTime = serenSunSetNowTime;
    }

    public int getSerenMidNightSetNowTime() {
        return SerenMidNightSetNowTime;
    }

    public void setSerenMidNightSetNowTime(int serenMidNightSetNowTime) {
        SerenMidNightSetNowTime = serenMidNightSetNowTime;
    }

    public int getSerenDawnSetNowTime() {
        return SerenDawnSetNowTime;
    }

    public void setSerenDawnSetNowTime(int serenDawnSetNowTime) {
        SerenDawnSetNowTime = serenDawnSetNowTime;
    }


    public void ResetSerenTime(boolean show) {
        this.SerenTimetype = 1;
        this.SerenNoonNowTime = 110;
        this.SerenNoonTotalTime = 110;
        this.SerenSunSetNowTime = 110;
        this.SerenSunSetTotalTime = 110;
        this.SerenMidNightSetNowTime = 30;
        this.SerenMidNightSetTotalTime = 30;
        this.SerenDawnSetNowTime = 110;
        this.SerenDawnSetTotalTime = 110;
        if (show) {
            getMap().broadcastMessage(MobPacket.BossSeren.SerenTimer(0, new int[]{360000, this.SerenNoonTotalTime, this.SerenSunSetTotalTime, this.SerenMidNightSetTotalTime, this.SerenDawnSetTotalTime}));
        }
    }
    public void AddSerenTotalTimeHandler(int type, int add, int turn) {
        getMap().broadcastMessage(MobPacket.BossSeren.SerenTimer(1, new int[] { this.SerenNoonTotalTime, this.SerenSunSetTotalTime, this.SerenMidNightSetTotalTime, this.SerenDawnSetTotalTime, turn }));
    }

    public void AddSerenTimeHandler(int type, int add) {
        int nowtime = 0;
        switch (type) {
            case 1:
                this.SerenNoonNowTime += add;
                break;
            case 2:
                this.SerenSunSetNowTime += add;
                for (MapleCharacter chr : getMap().getAllChracater()) {
                    if (chr.isAlive() && chr.getBuffedValue(MapleBuffStat.NotDamaged) == null && chr.getBuffedValue(MapleBuffStat.IndieNotDamaged) == null) {
                        int minushp = (int) (-chr.getStat().getCurrentMaxHp() / 100L);
                        chr.addHP(minushp);
                        chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showEffect(chr, 0, minushp, 36, 0, 0, (byte) 0, true, null, null, null));
                    }
                }
                break;
            case 3:
                this.SerenMidNightSetNowTime += add;
                break;
            case 4:
                this.SerenDawnSetNowTime += add;
                break;
        }
        nowtime = (type == 4) ? this.SerenDawnSetNowTime : ((type == 3) ? this.SerenMidNightSetNowTime : ((type == 2) ? this.SerenSunSetNowTime : this.SerenNoonNowTime));
        MapleMonster seren = null;
        int[] serens = {8880603, 8880607, 8880609, 8880612};
        for (int ids : serens) {
            seren = getMap().getMonsterById(ids);
            if (seren != null) {
                break;
            }
        }
        if (nowtime == 3) {
            for (MapleMonster mob : getMap().getAllMonster()) {
                if (mob.getId() == seren.getId() + 1) {
                    getMap().broadcastMessage(MobPacket.ChangePhaseDemian(mob, 79));
                    getMap().killMonsterType(mob, 2);
                }
            }
        }

        if (nowtime <= 0) {
            if (seren != null) {
                Point pos = seren.getPosition();
                getMap().broadcastMessage(MobPacket.BossSeren.SerenTimer(2, new int[]{1}));
                setCustomInfo(8880603, 1, 0);
                getMap().broadcastMessage(MobPacket.BossSeren.SerenChangePhase("Mob/" + seren.getId() + ".img/skill3", 0, seren));
                for (MapleMonster mob : getMap().getAllMonster()) {
                    if (mob.getId() == seren.getId() || mob.getId() == 8880605 || mob.getId() == 8880606 || mob.getId() == 8880611) {
                        getMap().broadcastMessage(MobPacket.ChangePhaseDemian(mob, 79));
                        getMap().killMonsterType(mob, 2);
                    }
                }
                this.SerenTimetype++;
                if (this.SerenTimetype > 4) {
                    this.SerenTimetype = 1;
                }
                getMap().broadcastMessage(CWvsContext.serverNotice(5, "", "시간이 흐르고 태양 또한 정해진 순환에 따라 변화합니다."));
                switch (this.SerenTimetype) {
                    case 1:
                        addHp(this.shield, false);
                        this.shield = -1L;
                        this.shieldmax = -1L;
                        getMap().broadcastMessage(MobPacket.showBossHP(this));
                        getMap().broadcastMessage(MobPacket.mobBarrier(this));
                        getMap().broadcastMessage(CWvsContext.serverNotice(5, "", "정오가 시작됨과 동시에 남아있는 여명의 기운이 세렌을 회복시킵니다."));
                        this.SerenNoonNowTime = this.SerenNoonTotalTime;
                        break;
                    case 2:
                        getMap().broadcastMessage(CWvsContext.serverNotice(5, "", "황혼의 불타는 듯한 석양이 회복 효율을 낮추고 지속적으로 피해를 입힙니다."));
                        this.SerenSunSetNowTime = this.SerenSunSetTotalTime;
                        break;
                    case 3:
                        getMap().broadcastMessage(CWvsContext.serverNotice(5, "", "태양이 저물어 빛을 잃고 자정이 시작됩니다."));
                        this.SerenMidNightSetNowTime = this.SerenMidNightSetTotalTime;
                        break;
                    case 4:
                        getMap().broadcastMessage(CWvsContext.serverNotice(5, "", "태양이 서서히 떠올라 빛과 희망이 시작되는 여명이 다가옵니다."));
                        this.SerenDawnSetNowTime = this.SerenDawnSetTotalTime;
                        break;
                }
                Timer.MapTimer.getInstance().schedule(() -> {
                    getMap().broadcastMessage(SLFCGPacket.ClearObstacles());
                    getMap().broadcastMessage(MobPacket.useFieldSkill(100024, 1));
                }, 500L);

                Timer.MapTimer.getInstance().schedule(() -> {
                    int nextid = (type == 4) ? 8880607 : ((type == 3) ? 8880603 : ((type == 2) ? 8880612 : 8880609));
                    getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(nextid), pos);
                    getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(nextid + 1), new Point(-49, 305));
                    if (nextid == 8880603) {
                        getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8880605), pos);
                        MapleMonster totalseren = getMap().getMonsterById(8880602);
                        if (totalseren != null)
                            totalseren.gainShield(totalseren.getStats().getHp() * 15L / 100L, !(totalseren.getShield() > 0L), 0);
                    }
                    getMap().broadcastMessage(MobPacket.BossSeren.SerenTimer(2, new int[]{0}));
                    setCustomInfo(8880603, 0, 0);
                    getMap().broadcastMessage(MobPacket.BossSeren.SerenChangeBackground(this.SerenTimetype));
                }, 3560L);
            }
        }
    }

    public void gainShield(long energy, boolean first, int delayremove) {
        this.shield += energy;
        if (first) {
            this.shield = energy;
            this.shieldmax = energy;
            if (delayremove > 0) {
                Timer.EtcTimer.getInstance().schedule(() -> {
                    this.shield = 0L;
                    this.shieldmax = 0L;
                    getMap().broadcastMessage(MobPacket.mobBarrier(this));
                }, (delayremove * 1000));
            }
        }
        getMap().broadcastMessage(MobPacket.mobBarrier(this));
    }

    public long getShield() {
        return this.shield;
    }

    public void setShield(long shield) {
        this.shield = shield;
    }

    public long getShieldmax() {
        return this.shieldmax;
    }

    public void setShieldmax(long shieldmax) {
        this.shieldmax = shieldmax;
    }

    public int getShieldPercent() {
        return (int)Math.ceil(this.shield * 100.0D / this.shieldmax);
    }

    public Map<Integer, SkillCustomInfo> getSkillCustomValues() {
        return customInfo;
    }

    public Long getSkillCustomValue(final int skillid) {
        if (customInfo.containsKey(skillid)) {
            return customInfo.get(skillid).getValue();
        }
        return null;
    }

    public long getCustomValue0(final int skillid) {
        if (customInfo.containsKey(skillid)) {
            return customInfo.get(skillid).getValue();
        }
        return 0;
    }

    public void removeCustomInfo(final int skillid) {
        customInfo.remove(skillid);
    }

    public void setCustomInfo(final int skillid, final int value, final int time) {
        customInfo.put(skillid, new SkillCustomInfo(value, time));
    }

    public void addSkillCustomInfo(int skillid, long value) {
        customInfo.put(Integer.valueOf(skillid), new SkillCustomInfo(getCustomValue0(skillid) + value, 0L));
    }

    public Map<Integer, SkillCustomInfo> getCustomValues() {
        return this.customInfo;
    }

    public static class CancelStatusAction implements Runnable {

        private final WeakReference<MapleMonster> target;
        private final MonsterStatus status;

        public CancelStatusAction(final MapleMonster target,
                final MonsterStatus status) {
            this.target = new WeakReference<MapleMonster>(target);
            this.status = status;
        }

        @Override
        public void run() {
            final MapleMonster realTarget = target.get();
            if (realTarget != null && realTarget.isAlive()) {
                realTarget.cancelStatus(status);
            }
        }
    }

}
