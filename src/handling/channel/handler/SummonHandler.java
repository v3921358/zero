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
package handling.channel.handler;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MatrixSkill;
import client.Skill;
import client.SkillFactory;
import client.SummonSkillEntry;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.Timer;
import server.life.MapleMonster;
import server.maps.ForceAtom;
import server.maps.MapleAtom;
import server.maps.MapleDragon;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import server.movement.LifeMovementFragment;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.MobPacket;

public class SummonHandler {

    public static final void MoveDragon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(12); //POS
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 5);
        if (chr != null && chr.getDragon() != null && res.size() > 0) {
            final Point pos = chr.getDragon().getPosition();
            MovementParse.updatePosition(res, chr.getDragon(), 0);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, CField.moveDragon(chr.getDragon(), pos, res), chr.getTruePosition());
            }
        }
    }

    public static final void MoveSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMapObject obj = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null) {
            return;
        }
        if (obj instanceof MapleDragon) {
            MoveDragon(slea, chr);
            return;
        }
        final MapleSummon sum = (MapleSummon) obj;
        if (sum.getOwner().getId() != chr.getId() || sum.getSkillLevel() <= 0 || sum.getMovementType() == SummonMovementType.STATIONARY) {
            return;
        }
        slea.skip(12); //startPOS
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 4);

        final Point pos = sum.getPosition();
        MovementParse.updatePosition(res, sum, 0);
        if (res.size() > 0) {
            chr.getMap().broadcastMessage(chr, SummonPacket.moveSummon(chr.getId(), sum.getObjectId(), pos, res), sum.getTruePosition());
        }
    }

    public static final void DamageSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int objectId = slea.readInt();
        final int unkByte = slea.readByte();
        final int damage = slea.readInt();
        final int monsterIdFrom = slea.readInt();

        MapleSummon summon = chr.getMap().getSummonByOid(objectId);
        MapleMonster monster = chr.getMap().getMonsterById(monsterIdFrom);

        if (summon == null) {
            return;
        }

        boolean remove = false;

        if (monster != null) {
            switch (summon.getSkill()) {
                case 13111024:
                    if (!monster.isBuffed(13111024)) {
                        monster.applyStatus(chr.getClient(), MonsterStatus.MS_Speed, new MonsterStatusEffect(13111024, (int) chr.getBuffLimit(13111024)), -100, chr.getBuffedEffect(MapleBuffStat.IndieSummon, 13111024));
                    } else {
                        chr.dropMessageGM(6, "13111024 스킬 은 이미 적용중이라 넘어감.");
                    }
                    break;
            }
        }

        if ((summon.isPuppet() || summon.getSkill() == 3221014) && summon.getOwner().getId() == chr.getId() && damage > 0) { //We can only have one puppet(AFAIK O.O) so this check is safe.
            summon.addHP(-damage);
            if (summon.getHP() <= 0) {
                remove = true;
            }
            chr.dropMessageGM(-8, "DamageSummon DMG : " + damage);
            chr.getMap().broadcastMessage(chr, SummonPacket.damageSummon(chr.getId(), summon.getSkill(), damage, unkByte, monsterIdFrom), summon.getTruePosition());
            if (summon.getSkill() == 14000027) {
                summon.removeSummon(chr.getMap(), false);
            }
        }

        if (remove) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon);
        }
    }

    public static void SummonAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null) {
            return;
        }
        final MapleMap map = chr.getMap();
        final MapleMapObject obj = map.getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        final MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwner().getId() != chr.getId() || summon.getSkillLevel() <= 0) {
            chr.dropMessageGM(5, "Error.");
            return;
        }
        final SummonSkillEntry sse = SkillFactory.getSummonData(summon.getSkill());
        if (summon.getSkill() / 1000000 != 35 && summon.getSkill() != 12120013 && summon.getSkill() != 33101008 && summon.getSkill() != 400041038 && sse == null) {
            chr.dropMessage(5, "Error in processing attack.");
            return;
        }
        slea.skip(4); // 0, 324++
        slea.skip(4); //tick
        int skillid = slea.readInt();
        if (summon.getSkill() != skillid) {
            chr.dropMessage(5, "skill data unmatched.");
        }
        int skillid2 = slea.readInt();
        if (skillid == 12120013) {
            skillid2 = 400021043;
        }

        slea.skip(1);

        switch (skillid2) {
            case 152110001:
                slea.skip(4);
                break;
        }

        slea.skip(1);

        final byte animation = slea.readByte();
        byte tbyte = (byte) (slea.readByte());
        byte numAttacked = (byte) ((tbyte >>> 4) & 0xF);
        byte hits = (byte) (tbyte & 0xF);

        slea.readByte();
        if (summon.getSkill() == 35111002 && chr.getBuffedValue(35111002)) {
            slea.skip(4 * 3);
        }

        slea.skip(4);
        Point pos = slea.readPos();

        byte a = slea.readByte();
        if (a != 0) {
            slea.skip(2);
            slea.skip(2);
        }

        slea.skip(4);
        slea.skip(2);
        slea.skip(4);
        slea.skip(4);

        long damage = 0, totDamageToOneMonster = 0;
        final List<Pair<Integer, List<Long>>> allDamage = new ArrayList<Pair<Integer, List<Long>>>();
        for (int i = 0; i < numAttacked; i++) {
            final int objectId = slea.readInt();
            slea.readInt();
            slea.readByte();
            slea.readByte();
            slea.readByte();
            slea.readByte();
            slea.readByte();
            slea.readInt();
            slea.readByte();
            slea.skip(4);
            slea.skip(4);
            slea.readInt();
            slea.skip(2);
            slea.readInt();
            slea.readInt();
            slea.readByte();

            List<Long> damages = new ArrayList<>();
            for (int j = 0; j < hits; j++) {
                totDamageToOneMonster += damage;
                damage = slea.readLong();
                if (damage < 0) {
                    damage = damage & 0xFFFFFFFFL;
                }
                damages.add(damage);
            }

            slea.skip(4);

            GameConstants.sub_2224400(slea, null);
            allDamage.add(new Pair<Integer, List<Long>>(objectId, damages));
        }

        map.broadcastMessage(chr, SummonPacket.summonAttack(summon, skillid2 != 0 ? skillid2 : skillid, animation, tbyte, allDamage, chr.getLevel(), pos, false), summon.getTruePosition());

        final Skill summonSkill = SkillFactory.getSkill(summon.getSkill());
        final MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        if (summonEffect == null) {
            chr.dropMessage(5, "Error in attack.");
            return;
        }

        /*        boolean useBulletCount = summonLinkEffect.getAttackCount() == 0 && summonLinkEffect.getBulletCount() > 0;
    	
         int target = summonLinkEffect.getMobCount();
         int attackCount = useBulletCount ? summonLinkEffect.getBulletCount() : summonLinkEffect.getAttackCount();
         for (int skill : SkillFactory.getSkill(skillid2 != 0 ? skillid2 : skillid).getPsdSkills()) {
         int bonusSkillLevel = chr.getSkillLevel(skill);
         if (bonusSkillLevel > 0 && skill != (skillid2 != 0 ? skillid2 : skillid)) {
         MapleStatEffect bonusEffect = SkillFactory.getSkill(skill).getEffect(bonusSkillLevel);

         target += bonusEffect.getTargetPlus();
         target += bonusEffect.getTargetPlus_5th();
         attackCount += useBulletCount ? bonusEffect.getBulletCount() : bonusEffect.getAttackCount();
         }
         }
    	
         if (summonLinkEffect.getMobCount() > 0 && chr.getSkillLevel(70000047) > 0) {
         target += SkillFactory.getSkill(70000047).getEffect(chr.getSkillLevel(70000047)).getTargetPlus();
         }
    	
         Integer plusCount = chr.getBuffedValue(MapleBuffStat.Buckshot);
         if (plusCount != null) {
         attackCount *= plusCount;
         }
    	
         if (chr.getBuffedEffect(MapleBuffStat.ShadowPartner) != null) {
         attackCount *= 2;
         }
    	
         Integer attackCountX = chr.getBuffedValue(MapleBuffStat.AttackCountX);
         int[] blowSkills = {32001000, 32101000, 32111002, 32121002};
         if (attackCountX != null) {
         for (int blowSkill : blowSkills) {
         if ((skillid2 != 0 ? skillid2 : skillid) == blowSkill) {
         attackCount += attackCountX;
         }
         }
         }
    	
         chr.dropMessageGM(-5, "몹 개체수 > 클라이언트 계산 : " + numAttacked + " / 서버 계산 : " + target);
         chr.dropMessageGM(-5, "공격 횟수 > 클라이언트 계산 : " + hits + " / 서버 계산 : " + attackCount);
    	
         if (numAttacked > target) {
         chr.dropMessageGM(-6, "개체수가 계산값보다 많습니다.");
         return;
         }
    	
         if (hits > attackCount) {
         chr.dropMessageGM(-6, "공격 횟수가 계산값보다 많습니다.");
         return;
         }*/
        for (Pair<Integer, List<Long>> attackEntry : allDamage) {
            for (Long toDamage : attackEntry.right) {
                final MapleMonster mob = map.getMonsterByOid(attackEntry.left);
                if (mob == null) {
                    continue;
                }

                //////////////////////////////////////
                //재코딩 - 예인
                List<Triple<MonsterStatus, MonsterStatusEffect, Long>> statusz = new ArrayList<>();
                switch (skillid) {
                    case 1311014: { // 鍮꾪????쇳겕
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) 1));
                        break;
                    }
                    case 2111010: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(skillid, summonEffect.getDOTTime()), (long) (summonEffect.getDOT() * totDamageToOneMonster / allDamage.size() / 10000)));
                        break;
                    }
                    case 2121005: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(skillid, summonEffect.getDOTTime()), (long) (summonEffect.getDOT() * totDamageToOneMonster / allDamage.size() / 10000)));
                        break;
                    }
                    case 2221005: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) summonEffect.getV()));

                        if (mob.getFreezingOverlap() < 5) {
                            mob.setFreezingOverlap((byte) (mob.getFreezingOverlap() + 1));
                        }
                        break;
                    }
                    case 2321003: { // 바하뮤트
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_ElementResetBySummon, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) summonEffect.getX()));
                        break;
                    }
                    case 3111005: { // 피닉스
                        if (Randomizer.rand(0, 100) < 41) {
                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) 1));
                        }
                        break;
                    }
                    case 3211005: { // 프리져
                        if (!mob.getStats().isBoss()) {
                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) summonEffect.getX()));
                        }
                        break;
                    }
                    case 3311009: { // 레이븐
                        if (chr.getSkillLevel(3320000) > 0) {
                            MapleStatEffect energyCharge = SkillFactory.getSkill(3320000).getEffect(1);
                            if (energyCharge != null) {

                                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

                                int max = energyCharge.getU(); // 렐릭 차지 최대 값

                                int add = energyCharge.getS(); // 렐릭 차지량

                                if (chr.energy < max) {
                                    chr.energy = Math.min(max, chr.energy + add);
                                }

                                statups.put(MapleBuffStat.RelikGauge, new Pair<>(chr.energy, 0));

                                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
                            }
                        }
                        break;
                    }
                    case 3221014: { // 애로우 일루젼
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) 1));
                        break;
                    }
                    case 23111008: {
                        if (!mob.getStats().isBoss()) {
                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(skillid, summonEffect.getSubTime()), (long) -10));
                        }
                        break;
                    }
                    case 23111009: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(skillid, summonEffect.getDOTTime()), (long) (summonEffect.getDOT() * totDamageToOneMonster / allDamage.size() / 10000)));
                        break;
                    }
                    case 61111002:
                    case 61111220: { // 패트리파이드
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) summonEffect.getX()));
                        break;
                    }

                    case 400021067: { // 스피릿 오브 스노우
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(skillid, summonEffect.getSubTime() > 0 ? summonEffect.getSubTime() : summonEffect.getDuration()), (long) summonEffect.getV()));

                        if (mob.getFreezingOverlap() < 5) {
                            mob.setFreezingOverlap((byte) (mob.getFreezingOverlap() + (numAttacked == 1 ? summonEffect.getZ() : 1)));
                        }
                        break;
                    }

                    case 400021033: { // 엔젤 오브 리브라
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_ElementResetBySummon, new MonsterStatusEffect(skillid, summonEffect.getQ() * 1000), (long) summonEffect.getQ2()));
                        break;
                    }
                    case 400051023: { // 귀문진
                        final Skill summonSkill2 = SkillFactory.getSkill(400051022);
                        final MapleStatEffect summonEffect2 = summonSkill2.getEffect(summon.getSkillLevel());
                        if (mob.getSpiritGate() < summonEffect2.getY()) {
                            mob.setSpiritGate(mob.getSpiritGate() + 1);
                        }
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_SpiritGate, new MonsterStatusEffect(skillid, summonEffect2.getS2() * 1000), (long) mob.getSpiritGate()));
                        break;
                    }
                }

                Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();

                for (Triple<MonsterStatus, MonsterStatusEffect, Long> status : statusz) {
                    if (status.left != null && status.mid != null) {
//                		if (!status.mid.shouldCancel(System.currentTimeMillis())) {
                        if (summonEffect.makeChanceResult()) {
                            if (status.left == MonsterStatus.MS_Burned && status.right < 0) {
                                status.right = status.right & 0xFFFFFFFFL;
                            }
                            status.mid.setValue(status.right);
                            applys.put(status.left, status.mid);
                        }
//                		}
                    }
                }

                mob.applyStatus(c, applys, summonEffect);

                statusz.clear();

                switch (skillid2) {
                    case 33001016: {
                        chr.addHP(chr.getStat().getCurrentMaxHp() * SkillFactory.getSkill(33001016).getEffect(chr.getSkillLevel(33001016)).getQ() / 100);
                        if (mob.getAnotherByte() == 0) {
                            mob.setAnotherByte(1);
                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                        } else {
                            if (Randomizer.isSuccess(30)) {
                                if (mob.getAnotherByte() < 3) {
                                    mob.setAnotherByte(mob.getAnotherByte() + 1);
                                }
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                            }
                        }
                        break;
                    }
                    case 33101115: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(skillid, 3000), (long) 1));

                        if (mob.getAnotherByte() == 0) {
                            mob.setAnotherByte(1);
                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                        } else {
                            if (Randomizer.isSuccess(80)) {
                                if (mob.getAnotherByte() < 3) {
                                    mob.setAnotherByte(mob.getAnotherByte() + 1);
                                }
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                            }
                        }
                        break;
                    }
                    case 33111015: {
                        if (mob.getAnotherByte() == 0) {
                            mob.setAnotherByte(1);
                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                        } else {
                            if (Randomizer.isSuccess(40)) {
                                if (mob.getAnotherByte() < 3) {
                                    mob.setAnotherByte(mob.getAnotherByte() + 1);
                                }
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                            }
                        }
                        break;
                    }
                    case 33121017: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(skillid, 10000), (long) summonEffect.getDuration()));

                        if (mob.getAnotherByte() < 3) {
                            mob.setAnotherByte(mob.getAnotherByte() + 1);
                        }
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));

                        break;
                    }
                    case 33121255: {
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                        break;
                    }
                    default: {
                        if (skillid >= 33001007 && skillid <= 33001015) {
                            if (Randomizer.isSuccess(15)) {
                                if (mob.getAnotherByte() < 3) {
                                    mob.setAnotherByte(mob.getAnotherByte() + 1);
                                }
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_JaguarBleeding, new MonsterStatusEffect(33000036, 8000), (long) mob.getAnotherByte()));
                            }
                        }
                        break;
                    }
                }

                applys.clear();

                for (Triple<MonsterStatus, MonsterStatusEffect, Long> status : statusz) {
                    if (status.left != null && status.mid != null) {
//                		if (!status.mid.shouldCancel(System.currentTimeMillis())) {
//                        if (summonEffect.makeChanceResult()) {
                        if (status.left == MonsterStatus.MS_Burned && status.right < 0) {
                            status.right = status.right & 0xFFFFFFFFL;
                        }
                        status.mid.setValue(status.right);
                        applys.put(status.left, status.mid);
                        //                      }
//                		}
                    }
                }

                mob.applyStatus(c, applys, summonEffect);

                if (sse != null && sse.delay > 0 && summon.getMovementType() != SummonMovementType.STATIONARY && summon.getMovementType() != SummonMovementType.CIRCLE_STATIONARY && summon.getMovementType() != SummonMovementType.WALK_STATIONARY && chr.getTruePosition().distanceSq(mob.getTruePosition()) > 400000.0) {
                    //                chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_SUMMON);
                }
                //            if (chr.isGM() || toDamage < (chr.getStat().getCurrentMaxBaseDamage() * 5.0 * (summonEffect.getSelfDestruction() + summonEffect.getDamage() + chr.getStat().getDamageIncrease(summonEffect.getSourceId())) / 100.0)) { //10 x dmg.. eh
                mob.damage(chr, toDamage, true);
                chr.checkMonsterAggro(mob);
                if (!mob.isAlive()) {
                    chr.getClient().getSession().writeAndFlush(MobPacket.killMonster(mob.getObjectId(), 1));
                    if (mob.getStati().containsValue(new MonsterStatusEffect(summonSkill.getId(), summonEffect.getDuration()))) {
                        byte size = 0;
                        for (MapleSummon sum : map.getAllSummonsThreadsafe()) {
                            if (sum.getSkill() == skillid) {
                                size++;
                            }
                        }
                        if (size < 10 && skillid != 32001014 && skillid != 32100010 && skillid != 32110017 && skillid != 32120019 && !summon.isNoapply()) {
                            summonEffect.applyTo(chr, false);
                        }
                    }
                }
            }
        }

        if (summon.getSkill() == 400051046) {
            if (summon.getEnergy() < 8) {
                summon.setEnergy(summon.getEnergy() + 1);
            }
            chr.getClient().getSession().writeAndFlush(CField.SummonPacket.ElementalRadiance(summon, 2));
            chr.getClient().getSession().writeAndFlush(CField.SummonPacket.specialSummon(summon, 2));
        }
        if (summon.getSkill() == 1301013) {
            if (chr.getCooldownLimit(1321015) > 0) {
                chr.changeCooldown(1321015, -300);
            }
        } else if (summon.getSkill() == 12120013) {
            c.getPlayer().setIgnition(0);
            MapleStatEffect sub = SkillFactory.getSkill(400021042).getEffect(chr.getSkillLevel(400021042));
            if (sub.getCooldown(chr) > 0) {
                chr.addCooldown(skillid2, System.currentTimeMillis(), sub.getCooldown(chr));
                c.getSession().writeAndFlush(CField.skillCooldown(skillid2, sub.getCooldown(chr)));
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        } else if (skillid2 == 152101006) {
            if (summon.getCrystalSkills().size() > 0 && summon.getCrystalSkills().get(0).booleanValue() == true) {
                summon.getCrystalSkills().set(0, false);
                c.getSession().writeAndFlush(SummonPacket.transformSummon(summon, 2));
                c.getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 3));
            }
        }

        if (skillid != 5320011 && skillid != 5321004 && skillid != 5211014 && skillid != 400011001 && skillid != 400011002) {
            if (summonEffect.getCooldown(c.getPlayer()) > 0 && c.getPlayer().getCooldownLimit(summon.getSkill()) == 0) {
                c.getPlayer().addCooldown(summon.getSkill(), System.currentTimeMillis(), summonEffect.getCooldown(c.getPlayer()));
                c.getSession().writeAndFlush(CField.skillCooldown(summon.getSkill(), summonEffect.getCooldown(c.getPlayer())));
            }
        }

        if (SkillFactory.getSkill(skillid2) != null) {
            MapleStatEffect subSummonEffect = SkillFactory.getSkill(skillid2).getEffect(chr.getSkillLevel(skillid2));

            if (subSummonEffect.getCooldown(c.getPlayer()) > 0 && c.getPlayer().getCooldownLimit(skillid2) == 0) {
                c.getPlayer().addCooldown(skillid2, System.currentTimeMillis(), subSummonEffect.getCooldown(c.getPlayer()));
                c.getSession().writeAndFlush(CField.skillCooldown(skillid2, subSummonEffect.getCooldown(c.getPlayer())));
            }
        }
        if (summon.getSkill() == 162101012) {
            summon.removeSummon(c.getPlayer().getMap(), false);
        }
        if (summon.getSkill() == 400041038) {
            summon.removeSummon(c.getPlayer().getMap(), false);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, summon.getSkill());
        } // 끝어택이후 제거
    }

    public static final void RemoveSummon(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleMapObject obj = c.getPlayer().getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwner().getId() != c.getPlayer().getId() || summon.getSkillLevel() <= 0) {
            c.getPlayer().dropMessageGM(5, "Error.");
            return;
        }
        if (summon.getSkill() == 35111002) {
            return;
        }
        if (summon.getSkill() >= 400031047 && summon.getSkill() <= 400031051) {
            slea.skip(1);
            int skillid = slea.readInt();
            int level = slea.readInt();
            int unk1 = slea.readInt();
            int unk2 = slea.readInt();
            int bullet = slea.readInt();
            Point p = slea.readPos();
            slea.skip(25);
            int count = slea.readInt();
            List<MatrixSkill> skills = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                MatrixSkill skill = new MatrixSkill(slea.readInt(), slea.readInt(), slea.readInt(), slea.readShort(), slea.readPos(), slea.readInt(), slea.readByte());
                byte unk5 = slea.readByte();
                int x = 0, y = 0;
                if (unk5 > 0) {
                    x = slea.readInt();
                    y = slea.readInt();
                }
                skill.setUnk5(unk5, x, y);
                skills.add(skill);
            }
            c.getSession().writeAndFlush(CWvsContext.MatrixSkill(skillid, (byte) level, skills));
            return;

        }
        if (summon.getSkill() == 400021047 || summon.getSkill() == 400021063 || summon.getSkill() == 400041033 || summon.getSkill() == 400041034) {
            if (summon.getSkill() == 400021047) {
                c.getPlayer().setBlackMagicAlter(c.getPlayer().getBlackMagicAlter() + 1);
            }
            slea.skip(1);
            int skillid = slea.readInt();
            int level = slea.readInt();
            int unk1 = slea.readInt();
            int unk2 = slea.readInt();
            int bullet = slea.readInt();
            slea.readPos(); // chr Point
            slea.skip(4 * 5); // 5 ints
            slea.readByte();
            List<MatrixSkill> skills = GameConstants.matrixSkills(slea);

            /*if (summon.getSkill() == 400021047 || summon.getSkill() == 400021063) {
                return;
            }*/
            c.getSession().writeAndFlush(CWvsContext.MatrixSkill(skillid, level, skills));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CWvsContext.MatrixSkillMulti(c.getPlayer(), skillid, level, unk1, unk2, bullet, skills), false);
            return;
        }
        if (summon.getSkill() == 400041038) {
            return;
        }
        if (summon.getSkill() == 400031047 || summon.getSkill() == 400041048) {
            return;
        }
        summon.removeSummon(c.getPlayer().getMap(), false);
        if (summon.getSkill() != 35121011 && summon.getSkill() != 400051011) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, summon.getSkill());
            //TODO: Multi Summoning, must do something about hack buffstat
        }
    }

    public static final void SubSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMapObject obj = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        final MapleSummon sum = (MapleSummon) obj;
        if (sum == null || sum.getOwner().getId() != chr.getId() || sum.getSkillLevel() <= 0 || !chr.isAlive()) {
            return;
        }

        MapleStatEffect eff = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel());
        slea.skip(4); // 324 ++
        switch (sum.getSkill()) {
            case 35121009:
                if (!chr.canSummon(eff.getX() * 1000)) {
                    return;
                }
                final int skillId = slea.readInt(); // 35121009?
                if (sum.getSkill() != skillId) {
                    return;
                }
                for (MapleSummon summon : chr.getMap().getAllSummonsThreadsafe()) {
                    if (summon.getSkill() == sum.getSkill()) {
                        slea.skip(1); // level
                        slea.readInt();
                        for (int i = 0; i < 3; i++) {
                            final MapleSummon tosummon = new MapleSummon(chr, SkillFactory.getSkill(35121011).getEffect(sum.getSkillLevel()), new Point(sum.getTruePosition().x, sum.getTruePosition().y - 5), SummonMovementType.WALK_STATIONARY);
                            chr.getMap().spawnSummon(tosummon, 5000);
//                            chr.addSummon(tosummon);
                        }
                        return;
                    }
                }
                break;
            case 35111008: //healing
            case 35120002:
                if (!chr.canSummon(eff.getX() * 1000)) {
                    return;
                }

                if (chr.getParty() != null) {
                    for (MaplePartyCharacter pc : chr.getParty().getMembers()) {
                        int ch = World.Find.findChannel(pc.getId());
                        if (ch > 0) {
                            MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(pc.getId());
                            if (player != null) {
                                player.addHP((int) (player.getStat().getCurrentMaxHp() * eff.getHp() / 100.0));
                                player.getClient().getSession().writeAndFlush(EffectPacket.showSummonEffect(player, sum.getSkill(), true));
                                player.getMap().broadcastMessage(player, EffectPacket.showSummonEffect(player, sum.getSkill(), false), false);
                            }

                            if (sum.getSkill() == 35120002) {
                                if (player.getBuffedEffect(MapleBuffStat.IndiePmdR, 35120002) == null) {
                                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                    int value = eff.getZ();
                                    if (chr.getSkillLevel(35120047) > 0) {
                                        value += 5;
                                    }
                                    statups.put(MapleBuffStat.IndiePmdR, new Pair<>(value, (int) chr.getBuffLimit(sum.getSkill())));

                                    for (Map.Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
                                        final MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(player, eff, System.currentTimeMillis(), statup.getKey());
                                        ScheduledFuture<?> schedule = Timer.BuffTimer.getInstance().schedule(() -> {
                                            cancelAction.run();
                                        }, statup.getValue().right);

                                        player.registerEffect(eff, System.currentTimeMillis(),
                                                schedule, statup, false, player.getId());
                                    }

                                    player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, player));
                                }
                            }
                        }
                    }
                } else {
                    chr.addHP((int) (chr.getStat().getCurrentMaxHp() * eff.getHp() / 100.0));
                    chr.getClient().getSession().writeAndFlush(EffectPacket.showSummonEffect(chr, sum.getSkill(), true));
                    chr.getMap().broadcastMessage(chr, EffectPacket.showSummonEffect(chr, sum.getSkill(), false), false);

                    if (chr.getBuffedEffect(MapleBuffStat.IndiePmdR, 35120002) == null) {
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        int value = eff.getZ();
                        if (chr.getSkillLevel(35120047) > 0) {
                            value += 5;
                        }
                        statups.put(MapleBuffStat.IndiePmdR, new Pair<>(value, (int) chr.getBuffLimit(sum.getSkill())));

                        for (Map.Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
                            final MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(chr, eff, System.currentTimeMillis(), statup.getKey());
                            ScheduledFuture<?> schedule = Timer.BuffTimer.getInstance().schedule(() -> {
                                cancelAction.run();
                            }, statup.getValue().right);

                            chr.registerEffect(eff, System.currentTimeMillis(),
                                    schedule, statup, false, chr.getId());
                        }

                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, chr));
                    }
                }

                Map<MonsterStatus, MonsterStatusEffect> datas = new HashMap<>();

                datas.put(MonsterStatus.MS_Pdr, new MonsterStatusEffect(sum.getSkill(), sum.getDuration(), SkillFactory.getSkill(35111008).getEffect(sum.getSkillLevel()).getW()));
                datas.put(MonsterStatus.MS_Mdr, new MonsterStatusEffect(sum.getSkill(), sum.getDuration(), SkillFactory.getSkill(35111008).getEffect(sum.getSkillLevel()).getW()));

                for (MapleMonster mob : chr.getMap().getAllMonstersThreadsafe()) {
                    if (!mob.isBuffed(sum.getSkill())) {
                        mob.applyStatus(chr.getClient(), datas, eff);
                    }
                }
                break;
            case 1301013:
                Skill bHealing = SkillFactory.getSkill(slea.readInt());
                final int bHealingLvl = chr.getTotalSkillLevel(bHealing);
                final int bDomilvl = chr.getTotalSkillLevel(1310013);
                if (bHealingLvl <= 0 || bHealing == null) {
                    return;
                }
                final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
                if (bHealing.getId() == 1310016) {
                    if (!chr.getBuffedValue(1310016)) {
                        healEffect.applyTo(chr, true);
                    }
                } else if (bHealing.getId() == 1320008) {
                    if (!chr.canSummon(healEffect.getX() * 1000)) {
                        return;
                    }
                    chr.addHP(healEffect.getHp());
                }
                chr.getClient().getSession().writeAndFlush(EffectPacket.showSummonEffect(chr, bDomilvl > 0 ? 1310013 : sum.getSkill(), true));
                chr.getMap().broadcastMessage(SummonPacket.summonSkill(chr.getId(), bDomilvl > 0 ? 1310013 : sum.getSkill(), bHealing.getId() == 1320008 ? 5 : (Randomizer.nextInt(3) + 6)));
                chr.getMap().broadcastMessage(chr, EffectPacket.showSummonEffect(chr, bDomilvl > 0 ? 1310013 : sum.getSkill(), false), false);
                break;
            case 152101000:
                Skill harmonyLink = SkillFactory.getSkill(slea.readInt());
                if (harmonyLink.getId() == 152111007) {
                    if (sum.getCrystalSkills().size() >= 2 && sum.getCrystalSkills().get(1).booleanValue() == true) {
                        chr.getClient().getSession().writeAndFlush(SummonPacket.transformSummon(sum, 1));
                        harmonyLink.getEffect(chr.getSkillLevel(152111007)).applyTo(chr, false);
                        sum.getCrystalSkills().set(1, false);
                        chr.getClient().getSession().writeAndFlush(SummonPacket.transformSummon(sum, 2));
                        chr.getClient().getSession().writeAndFlush(SummonPacket.ElementalRadiance(sum, 3));
                    }

                }
                break;
            case 400001013:
                MapleStatEffect cm = SkillFactory.getSkill(400001016).getEffect(sum.getSkillLevel());
                cm.applyTo(chr, false);

                chr.getClient().getSession().writeAndFlush(CField.skillCooldown(400001016, cm.getCooldown(chr)));
                chr.addCooldown(400001016, System.currentTimeMillis(), cm.getCooldown(chr));
                break;
            case 400041038: // 다크로드의 비전서
                Item nk = null;
                for (short position = 0; position < chr.getInventory(MapleInventoryType.USE).newList().size(); ++position) {
                    nk = chr.getInventory(MapleInventoryType.USE).newList().get(position);
                    if (nk == null) {
                        continue;
                    }
                    if (nk.getItemId() / 10000 == 207) {
                        break;
                    }
                }
                if (nk != null) {
                    final List<MapleMapObject> objs = chr.getMap().getMapObjectsInRange(chr.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                    final List<Integer> monsters = new ArrayList<>();
                    for (int i = 0; i < eff.getMobCount(); i++) {
                        int rand;
                        if (objs.size() <= 1) {
                            rand = 1;
                        } else {
                            rand = Randomizer.nextInt(objs.size());
                        }
                        if (objs.size() < eff.getBulletCount()) {
                            if (i < objs.size()) {
                                monsters.add(objs.get(i).getObjectId());
                            }
                        } else if (objs.size() > 1) {
                            monsters.add(objs.get(rand).getObjectId());
                            objs.remove(rand);
                        }
                    }
                    if (monsters.size() > 0) {
                        List<ForceAtom> atoms = new ArrayList<>();

                        MapleAtom atom = new MapleAtom(false, chr.getId(), 49, true, 400041038, sum.getTruePosition().x, sum.getTruePosition().y - 400);
                        atom.setDwSummonObjectId(sum.getObjectId());
                        int key = 0;

                        for (Integer m : monsters) {
                            for (int i = 0; i < eff.getBulletCount(); i++) {
                                atom.addForceAtom(new ForceAtom(2, Randomizer.rand(40, 44), Randomizer.rand(3, 4), (360 / (monsters.size() * 5 + 7)) * key, (short) 200));
                                key++;
                            }
                        }
                        for (int i = 0; i < eff.getX(); i++) {
                            atom.addForceAtom(new ForceAtom(2, Randomizer.rand(40, 44), Randomizer.rand(3, 4), (360 / (monsters.size() * 5 + 7)) * key, (short) 200));
                            key++;
                        }

                        atom.setDwTargets(monsters);
                        atom.setnItemId(nk.getItemId());
                        chr.getMap().spawnMapleAtom(atom);

                        chr.getClient().getSession().writeAndFlush(EffectPacket.showEffect(chr, sum.getSkill(), sum.getSkill(), 4, 0, 0, (byte) 0, true, null, null, null));
                        chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, sum.getSkill(), sum.getSkill(), 4, 0, 0, (byte) 0, false, null, null, null), false);
                    }
                }
                break;
        }
        if (GameConstants.isAngel(sum.getSkill()) && chr.getBuffedEffect(MapleBuffStat.RepeatEffect) != null) {
            if (sum.getSkill() % 10000 == 1087) {
                MapleItemInformationProvider.getInstance().getItemEffect(2022747).applyTo(chr, true);
            } else if (sum.getSkill() % 10000 == 1085) {
                MapleItemInformationProvider.getInstance().getItemEffect(2022746).applyTo(chr, true);
            } else if (sum.getSkill() % 10000 == 1090) {
                MapleItemInformationProvider.getInstance().getItemEffect(2022764).applyTo(chr, true);
            } else if (sum.getSkill() % 10000 == 1179) {
                MapleItemInformationProvider.getInstance().getItemEffect(2022823).applyTo(chr, true);
            } else {
                MapleItemInformationProvider.getInstance().getItemEffect(2022746).applyTo(chr, true);
            }
            int skillid = chr.getBuffedEffect(MapleBuffStat.RepeatEffect).getSourceId();
            chr.getClient().getSession().writeAndFlush(EffectPacket.showSummonEffect(chr, sum.getSkill(), true));
            chr.getMap().broadcastMessage(chr, EffectPacket.showSummonEffect(chr, sum.getSkill(), false), false);
            EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.RepeatEffect, new Pair<>(1, 0));
            MapleStatEffect effect = MapleItemInformationProvider.getInstance().getItemEffect(skillid);
            chr.getMap().broadcastMessage(BuffPacket.giveForeignBuff(chr, statups, effect));
        }
    }

    public static void replaceSummon(LittleEndianAccessor slea, MapleClient c) {
        int skillId = slea.readInt();
        for (MapleSummon s : c.getPlayer().getSummons()) {
            if (GameConstants.getLinkedSkill(s.getSkill()) == skillId) {
                c.getPlayer().getMap().broadcastMessage(SummonPacket.removeSummon(s, false));
                s.setPosition(c.getPlayer().getTruePosition());
                c.getPlayer().getMap().broadcastMessage(SummonPacket.spawnSummon(s, true));
            } else if (skillId == 400031005 && (s.getSkill() >= 33001007 && s.getSkill() <= 33001015)) {
                c.getPlayer().getMap().broadcastMessage(SummonPacket.removeSummon(s, false));
                s.setPosition(c.getPlayer().getTruePosition());
                c.getPlayer().getMap().broadcastMessage(SummonPacket.spawnSummon(s, true));
            }
        }
    }

    public static void effectSummon(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        MapleSummon target = c.getPlayer().getMap().getSummonByOid(objectId);
        if (target != null) {
            slea.skip(8);
            int skill = slea.readInt();
            if (SkillFactory.getSkill(skill) == null) {
                return;
            }
            SkillFactory.getSkill(skill).getEffect(c.getPlayer().getSkillLevel(skill)).applyTo(c.getPlayer(), true);
            target.SetNoapply(true);
        }
    }

    public static void cancelEffectSummon(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        MapleSummon target = c.getPlayer().getMap().getSummonByOid(objectId);
        if (target != null) {
            target.SetNoapply(false);
        }
        //cancel 패킷 보내야하면 코딩
    }

    public static void specialSummon(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        MapleSummon target = c.getPlayer().getMap().getSummonByOid(objectId);
        if (target != null) {
            if (c.getPlayer().getSkillLevel(33001025) > 0) {
                final List<MapleMapObject> objs = c.getPlayer().getMap().getMapObjectsInRange(target.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                final List<MapleMapObject> skill = new ArrayList<>();
                if (!objs.isEmpty()) {
                    for (int i = 0; i < 10; i++) {
                        if (objs.size() <= i) {
                            break;
                        }
                        skill.add(objs.get(i));
                    }
                    for (MapleMapObject mobs : skill) {
                        MapleStatEffect effect = SkillFactory.getSkill(33001025).getEffect(c.getPlayer().getSkillLevel(33001025));
                        Map<MonsterStatus, MonsterStatusEffect> datas = new HashMap<>();
                        datas.put(MonsterStatus.MS_JaguarProvoke, new MonsterStatusEffect(33001025, effect.getDuration(), (long) 1));
                        datas.put(MonsterStatus.MS_DodgeBodyAttack, new MonsterStatusEffect(33001025, effect.getDuration(), (long) 1));
                        ((MapleMonster) mobs).applyStatus(c, datas, effect);
                    }
                }
                /*        	} else if (target.getSkill() == 152101000) {
                 //        		byte unk = slea.readByte();
        		
                 //      		if (unk == 8) {
                 if (target.getCrystalSkills().size() > 0 && target.getCrystalSkills().get(0).booleanValue() == true) {
                 target.getCrystalSkills().set(0, false);
                 c.getSession().writeAndFlush(SummonPacket.transformSummon(target, 2));
                 c.getSession().writeAndFlush(SummonPacket.ElementalRadiance(target, 3));
                 }
                 //    		}*/
            }
        }
    }
}
