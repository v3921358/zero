package server.control;

import client.*;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.channel.ChannelServer;
import scripting.EventInstanceManager;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMist;
import tools.CurrentTime;
import tools.packet.CField;

import java.awt.*;
import java.util.List;
import java.util.*;

public class MapleMistControl implements Runnable {

    private Skill _bigHugeGiganticCanonBall;
    private Skill _howlingGale;
    private Skill _demonicFrenzy;
    private Skill _blackMagicAlter;
    private Skill _overloadMode;
    private Skill _furiousCharge;
    private Skill _infinityFlameCircle;
    private Skill _wildGrenadier;
    private int date, dos;
    private boolean isfirst;
    private long lastSaveAuctionTime = 0;

    public MapleMistControl() {
        this.date = CurrentTime.요일();
        this.dos = 0;
        this.isfirst = false;
        this.lastSaveAuctionTime = System.currentTimeMillis();
        System.out.println("[Loading Completed] Start MistControl");
    }

    public static void handleCooldowns(final MapleCharacter chr, final int numTimes, final boolean hurt, final long now) {
        if (chr.getCooldownSize() > 0) {
            for (MapleCoolDownValueHolder m : chr.getCooldowns()) {
                if (m.startTime + m.length < now) {
                    chr.removeCooldown(m.skillId);
                    chr.getClient().getSession().writeAndFlush(CField.skillCooldown(m.skillId, 0));
                }
            }
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
                List<MapleMist> toRemove = new ArrayList<>();
                Iterator<MapleMist> mists = map.getAllMistsThreadsafe().iterator();
                while (mists.hasNext()) { // 모든 미스트 관리
                    MapleMist mist = mists.next();
                    int endTime = mist.getEndTime();
                    int duration = mist.getDuration();

                    boolean isEnd = false;

                    if (endTime > 0) {
                        isEnd = time - mist.getStartTime() >= endTime;
                    } else if (duration > 0) {
                        isEnd = time - mist.getStartTime() >= duration;
                    }

                    if (isEnd) {
                        if (mist.getSourceSkill() != null) {
                            /* if (mist.getSourceSkill().getId() == 400041008 && mist.getDuration() != 1800) { //
                                int spearSize = 0;
                                for (MapleMist mistz : map.getAllMistsThreadsafe()) {
                                    if (mistz.getSourceSkill().getId() == 400040008 || mistz.getSourceSkill().getId() == 400041008) {
                                        spearSize++;
                                    }
                                }
                                MapleMist spear = new MapleMist(new Rectangle(mist.getBox().x, mist.getBox().y, mist.getBox().width, mist.getBox().height), mist.getOwner(), SkillFactory.getSkill(spearSize >= 5 ? 400041008 : 400040008).getEffect(mist.getSkillLevel()), 1800, (byte) 0);
                                System.out.println(spear.getSource().getSourceId());
                                spear.setPosition(mist.getTruePosition());
                                map.spawnMist(spear, false);
                            } else */
                            if (mist.getSourceSkill().getId() == 400051025) {
                                MapleMist icbm = new MapleMist(new Rectangle(mist.getBox().x, mist.getBox().y, mist.getBox().width, mist.getBox().height), mist.getOwner(), SkillFactory.getSkill(400051026).getEffect(mist.getSkillLevel()), 15000, (byte) 0);
                                icbm.setPosition(mist.getTruePosition());
                                map.spawnMist(icbm, false);
                            }
                        }

                        toRemove.add(mist);

                    } else if (mist.getOwner() != null) {
                        if (map.getCharacter(mist.getOwner().getId()) == null) {
                            toRemove.add(mist);
                        }

                    } else if (mist.getMob() != null) {
                        if (map.getMonsterByOid(mist.getMob().getObjectId()) == null) {
                            toRemove.add(mist);
                        }
                    }

                    if (mist.getSource() != null) {
                        switch (mist.getSource().getSourceId()) {
                            case 2111003: {
                                Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();
                                while (mobs.hasNext()) {
                                    MapleMonster mob = mobs.next();
                                    if (mist.getBox().contains(mob.getTruePosition()) && !mob.isBuffed(2111003)) {
                                        if (mist.getOwner().getId() == chr.getId()) {
                                            Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
                                            MapleStatEffect effect = SkillFactory.getSkill(2111003).getEffect(chr.getSkillLevel(2111003));
                                            MapleStatEffect bonusTime = null, bonusDam = null;
                                            if (chr.getSkillLevel(2120044) > 0) {
                                                bonusTime = SkillFactory.getSkill(2120044).getEffect(chr.getSkillLevel(2120044));
                                            }
                                            if (chr.getSkillLevel(2120045) > 0) {
                                                bonusDam = SkillFactory.getSkill(2120045).getEffect(chr.getSkillLevel(2120045));
                                            }
                                            applys.put(MonsterStatus.MS_Burned, new MonsterStatusEffect(2111003, effect.getDOTTime() + (bonusTime != null ? bonusTime.getDOTTime() : 0), chr.getDotDamage()));
                                            mob.applyStatus(chr.getClient(), applys, mist.getSource());
                                        }
                                    } else {
                                        mob.cancelSingleStatus(mob.getBuff(2111003));
                                    }
                                }
                                break;
                            }
                            case 4121015: {
                                Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();

                                Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();

                                if (chr.getSkillLevel(4120046) > 0) {
                                    applys.put(MonsterStatus.MS_Pad, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW() - SkillFactory.getSkill(4120046).getEffect(1).getV()));
                                    applys.put(MonsterStatus.MS_Pdr, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW() - SkillFactory.getSkill(4120046).getEffect(1).getV()));
                                    applys.put(MonsterStatus.MS_Mad, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW() - SkillFactory.getSkill(4120046).getEffect(1).getV()));
                                    applys.put(MonsterStatus.MS_Mdr, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW() - SkillFactory.getSkill(4120046).getEffect(1).getV()));
                                } else {
                                    applys.put(MonsterStatus.MS_Pad, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW()));
                                    applys.put(MonsterStatus.MS_Pdr, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW()));
                                    applys.put(MonsterStatus.MS_Mad, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW()));
                                    applys.put(MonsterStatus.MS_Mdr, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getW()));
                                }

                                if (chr.getSkillLevel(4120047) > 0) {
                                    applys.put(MonsterStatus.MS_Speed, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getY() - SkillFactory.getSkill(4120047).getEffect(1).getS()));
                                } else {
                                    applys.put(MonsterStatus.MS_Speed, new MonsterStatusEffect(4121015, mist.getSource().getDuration(), -mist.getSource().getY()));
                                }

                                while (mobs.hasNext()) {
                                    MapleMonster mob = mobs.next();
                                    if ((mob.getStats().isBoss() && chr.getSkillLevel(4120048) > 0) || !mob.getStats().isBoss()) {
                                        if (mist.getBox().contains(mob.getTruePosition())) {
                                            if (mist.getOwner().getId() == chr.getId() && !mob.isBuffed(4121015)) {
                                                mob.applyStatus(chr.getClient(), applys, mist.getSource());
                                            }
                                        } else if (mob.isBuffed(4121015)) {
                                            mob.cancelStatus(applys);
                                        }
                                    }
                                }
                                break;
                            }
                            case 4221006: {
                                Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();
                                while (mobs.hasNext()) {
                                    MapleMonster mob = mobs.next();
                                    if (mist.getBox().contains(mob.getTruePosition())) {
                                        if (mist.getOwner().getId() == chr.getId()) {
                                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_HitCritDamR, new MonsterStatusEffect(4221006, mist.getSource().getDuration()), mist.getSource().getX(), mist.getSource());
                                        }
                                    } else {
                                        mob.cancelSingleStatus(mob.getBuff(4221006));
                                    }
                                }
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (mist.getOwner().getId() == chr.getId() && !chr.getBuffedValue(4221006)) {
                                        SkillFactory.getSkill(4221006).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                    } else if (mist.getOwner().getParty() != null) {
                                        if (mist.getOwner().getParty().getMemberById(chr.getId()) != null && !chr.getBuffedValue(4221006)) {
                                            SkillFactory.getSkill(4221006).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        }
                                    }
                                }
                                break;
                            }
                            case 12121005:
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (mist.getOwner().getId() == chr.getId() && !chr.getBuffedValue(12121005)) {
                                        SkillFactory.getSkill(12121005).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                    } else if (mist.getOwner().getParty() != null) {
                                        if (mist.getOwner().getParty().getMemberById(chr.getId()) != null && !chr.getBuffedValue(12121005)) {
                                            SkillFactory.getSkill(12121005).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        }
                                    }
                                } else {
                                    if (chr.getBuffedValue(12121005)) {
                                        chr.cancelEffectFromBuffStat(MapleBuffStat.IndieDamR, 12121005);
                                        chr.cancelEffectFromBuffStat(MapleBuffStat.IndieBooster, 12121005);
                                    }
                                }
                                break;
                            case 400001017: {
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (mist.getOwner().getId() == chr.getId() && !chr.getBuffedValue(400001017)) {
                                        SkillFactory.getSkill(400001017).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                    } else if (mist.getOwner().getParty() != null) {
                                        if (mist.getOwner().getParty().getMemberById(chr.getId()) != null) {
                                            SkillFactory.getSkill(400001017).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        }
                                    }
                                }

                                Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();
                                while (mobs.hasNext()) {
                                    MapleMonster mob = mobs.next();
                                    if (mist.getBox().contains(mob.getTruePosition())) {
                                        if (mist.getOwner().getId() == chr.getId()) {
                                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_Pdr, new MonsterStatusEffect(400001017, mist.getSource().getDuration()), -mist.getSource().getZ(), mist.getSource());
                                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_Mdr, new MonsterStatusEffect(400001017, mist.getSource().getDuration()), -mist.getSource().getZ(), mist.getSource());
                                        }
                                    } else {
                                        mob.cancelSingleStatus(mob.getBuff(400001017));
                                    }
                                }
                                break;
                            }
                            case 100001261: {
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (time - chr.lastDistotionTime >= 4000) {
                                        chr.lastDistotionTime = time;
                                        chr.dispelDebuffs();
                                        SkillFactory.getSkill(100001261).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                    }
                                }

                                Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();
                                while (mobs.hasNext()) {
                                    MapleMonster mob = mobs.next();
                                    if (mist.getBox().contains(mob.getTruePosition())) {
                                        if (time - mob.lastDistotionTime >= mist.getSource().getSubTime()) {
                                            mob.lastDistotionTime = time;
                                            mob.dispels();
                                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_AdddamSkill, new MonsterStatusEffect(100001261, mist.getSource().getSubTime()), mist.getSource().getX(), mist.getSource());
                                            if (!mob.getStats().isBoss()) {
                                                mob.applyStatus(chr.getClient(), MonsterStatus.MS_Freeze, new MonsterStatusEffect(100001261, mist.getSource().getSubTime()), mist.getSource().getSubTime(), mist.getSource());
                                            }
                                        }
                                    }
                                }
                                /*                   		 } else if (mist.getSource().getSourceId() == 35111008 || mist.getSource().getSourceId() == 35120002) {
                                 if (mist.getSource().getSourceId() == 35120002) {
                                 if (mist.getBox().contains(chr.getTruePosition())) {
                                 SkillFactory.getSkill(35120002).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                 } else {
                                 chr.cancelEffectFromBuffStat(MapleBuffStat.IndiePmdR, 35120002);
                                 }
                                 }
                                 for (MapleMonster mob : map.getAllMonstersThreadsafe()) {
                                 if (mist.getOwner().getId() == chr.getId()) {
                                 if (!mob.isBuffed(mist.getSource().getSourceId())) {
                                 mob.applyStatus(chr.getClient(), MonsterStatus.MS_Pdr, new MonsterStatusEffect(mist.getSource().getSourceId(), mist.getSource().getDuration()), -mist.getSource().getW(), mist.getSource());
                                 mob.applyStatus(chr.getClient(), MonsterStatus.MS_Mdr, new MonsterStatusEffect(mist.getSource().getSourceId(), mist.getSource().getDuration()), -mist.getSource().getW(), mist.getSource());
                                 }
                                 }
                                 }*/
                                break;
                            }
                            case 36121007:
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    for (MapleCoolDownValueHolder cooldown : chr.getCooldowns()) {
                                        if (cooldown.skillId != 36121007 && !SkillFactory.getSkill(cooldown.skillId).isHyper() && GameConstants.isXenon(cooldown.skillId / 10000)) {
                                            chr.changeCooldown(cooldown.skillId, -1000);
                                        }
                                    }
                                }
                                break;
                            case 162121043:
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (mist.getOwner().getId() == chr.getId() && !chr.getBuffedValue(162121043)) {
                                        SkillFactory.getSkill(162121043).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        long lastDispellTime = chr.getSkillCustomTime(162121043);
                                        if (lastDispellTime == 0)
                                            chr.setSkillCustomTime(162121043, System.currentTimeMillis());
                                        else if (System.currentTimeMillis() - lastDispellTime >= 2000) {
                                            chr.dispel();
                                            chr.setSkillCustomTime(162121043, System.currentTimeMillis());
                                        }
                                    } else if (mist.getOwner().getParty() != null) {
                                        if (mist.getOwner().getParty().getMemberById(chr.getId()) != null && !chr.getBuffedValue(162121043)) {
                                            SkillFactory.getSkill(162121043).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        }
                                    }
                                } else {
                                    if (chr.getBuffedEffect(162121043) != null)
                                        chr.cancelEffect(chr.getBuffedEffect(162121043), true, -1);
                                }

                                Iterator<MapleMonster> mobs = map.getAllMonstersThreadsafe().iterator();
                                while (mobs.hasNext()) {
                                    MapleMonster mob = mobs.next();
                                    if (mist.getBox().contains(mob.getTruePosition())) {
                                        if (mist.getOwner().getId() == chr.getId() && !mob.isBuffed(162121043)) {
                                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_MinusDef, new MonsterStatusEffect(162121043, (int) chr.getBuffLimit(162121044)), -mist.getSource().getS(), mist.getSource());
                                        }
                                    }
                                }
                                break;
                            case 162111003:
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (mist.getOwner().getId() == chr.getId()) {
                                        SkillFactory.getSkill(162111004).getEffect(mist.getSkillLevel()).applyTo(chr, true);
                                        long lastHealTime = chr.getSkillCustomTime(162111003);
                                        if (lastHealTime == 0)
                                            chr.setSkillCustomTime(162111003, System.currentTimeMillis());
                                        else if (System.currentTimeMillis() - lastHealTime >= 2000) {
                                            chr.addHP((int) (chr.getStat().getCurrentMaxHp() * (mist.getSource().getHp() / 100.0)));
                                            chr.setSkillCustomTime(162111003, System.currentTimeMillis());
                                        }
                                    } else if (mist.getOwner().getParty() != null) {
                                        if (mist.getOwner().getParty().getMemberById(chr.getId()) != null) {
                                            SkillFactory.getSkill(162111004).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        }
                                    }
                                }
                                break;
                            case 162111000: {
                                if (mist.getBox().contains(chr.getTruePosition())) {
                                    if (mist.getOwner().getId() == chr.getId()) {
                                        SkillFactory.getSkill(162111001).getEffect(mist.getSkillLevel()).applyTo(chr, true);
                                    } else if (mist.getOwner().getParty() != null) {
                                        if (mist.getOwner().getParty().getMemberById(chr.getId()) != null) {
                                            SkillFactory.getSkill(162111001).getEffect(mist.getSkillLevel()).applyTo(chr, false);
                                        }
                                    }
                                }
                                break;
                            }
                            default:
                                break;
                        }
                    } else if (mist.getMobSkill() != null) {
                        if (mist.getMobSkill().getSkillId() == 191) {
                            MapleMonster mob = chr.getMap().getMonsterById(8910000);
                            if (mob != null) {
                                if (mist.getBox().contains(mob.getTruePosition())) {
                                    EventInstanceManager eim = chr.getEventInstance();
                                    if (eim != null) {
                                        if (mist.getMobSkill().getSkillLevel() == 2) {
                                            if (eim.getTimeLeft() < 595000) {
                                                eim.restartEventTimer(eim.getTimeLeft() + 5000);
                                            }
                                        } else if (mist.getMobSkill().getSkillLevel() == 1) {
                                            if (eim.getTimeLeft() > 5000) {
                                                eim.restartEventTimer(eim.getTimeLeft() - 5000);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (MapleMist mist : toRemove) {
                    map.broadcastMessage(CField.removeMist(mist));
                    map.removeMapObject(mist);
                }
            }
        }
    }
}
