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
package tools.packet;

import client.MapleCharacter;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import handling.SendPacketOpcode;
import server.MapleStatEffect;
import server.Obstacle;
import server.Randomizer;
import server.field.FieldSkill;
import server.field.boss.demian.FlyingSwordNode;
import server.field.boss.demian.MapleDelayedAttack;
import server.field.boss.demian.MapleFlyingSword;
import server.field.boss.demian.MapleIncinerateObject;
import server.field.boss.lotus.MapleEnergySphere;
import server.field.boss.lucid.Butterfly;
import server.field.boss.lucid.FairyDust;
import server.field.boss.will.SpiderWeb;
import server.life.Ignition;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.MapleMap;
import server.maps.MapleNodes;
import server.movement.LifeMovementFragment;
import tools.HexTool;
import tools.Pair;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MobPacket {

    public static byte[] damageMonster(final int oid, final long damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeLong(damage);
        return mplew.getPacket();
    }

    public static byte[] damageFriendlyMob(final MapleMonster mob, final long damage, final boolean display) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(display ? 1 : 2); //false for when shammos changes map!
        mplew.writeLong(damage);
        mplew.writeLong(mob.getHp());
        mplew.writeLong((int) mob.getMobMaxHp());
        return mplew.getPacket();
    }

    public static byte[] setAfterAttack(int objectid, int afterAttack, int attackCount, int attackIdx, boolean left) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SET_MONSTER_AFTER_ATTACK.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(afterAttack);
        mplew.writeInt(attackCount);
        mplew.writeInt(attackIdx); // 274 ++
        mplew.write(left); // isLeft
        return mplew.getPacket();
    }

    public static byte[] killMonster(final int oid, final int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation); // 0 = dissapear, 1 = fade out, 2+ = special
        mplew.writeInt(0);
        mplew.writeInt(0); // 332++
        if (animation == 9) {
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    public static byte[] suckMonster(final int oid, final int chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(9);
        mplew.writeInt(0);
        mplew.writeInt(chr);

        return mplew.getPacket();
    }

    public static byte[] healMonster(final int oid, final int heal) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeLong(-heal);

        return mplew.getPacket();
    }

    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(remhppercentage);
        mplew.write(1);//306 추가됨

        return mplew.getPacket();
    }

    public static byte[] showBossHP(final MapleMonster mob) { //보스 체력바
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(6);
        mplew.writeInt(mob.getId()); //hack: MV cant have boss hp bar
        mplew.writeLong(mob.getHp() + mob.getBarrier());
        mplew.writeLong((long) (mob.getMobMaxHp() * mob.bonusHp() + mob.getBarrier()));
        mplew.write(mob.getStats().getTagColor());
        mplew.write(mob.getStats().getTagBgColor()); //5 = 보스 알림이
        return mplew.getPacket();
    }

    public static byte[] showBossHP(final int monsterId, final long currentHp, final long maxHp) { //보스 죽이고나서 체력바 없어짐.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(6);
        mplew.writeInt(monsterId); //has no image
        mplew.writeLong((int) (currentHp <= 0 ? -1 : currentHp));
        mplew.writeLong((int) maxHp);
        mplew.write(6);
        mplew.write(5);

        //colour legend: (applies to both colours)
        //1 = red, 2 = dark blue, 3 = light green, 4 = dark green, 5 = black, 6 = light blue, 7 = purple
        return mplew.getPacket();
    }

    public static byte[] moveMonster(byte bOption, int skill, long targetInfo, int tEncodedGatherDuration, int oid, Point startPos, Point startWobble, List<LifeMovementFragment> moves, List<Point> multiTargetForBall, List<Short> randTimeForAreaAttack, boolean cannotUseSkill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(bOption); //?? I THINK
        mplew.write(skill); //pCenterSplit
        mplew.writeLong(targetInfo); //307 ++    

        mplew.write(multiTargetForBall.size());
        for (int i = 0; i < multiTargetForBall.size(); i++) {
            mplew.writeShort(multiTargetForBall.get(i).x);
            mplew.writeShort(multiTargetForBall.get(i).y);
        }

        mplew.write(randTimeForAreaAttack.size());
        for (int i = 0; i < randTimeForAreaAttack.size(); i++) {
            mplew.writeShort(randTimeForAreaAttack.get(i));
        }

        mplew.writeInt(0); // 1.2.332++
        mplew.writeInt(0); // 1.2.342++
        mplew.writeInt(tEncodedGatherDuration); //1.2.192+
        mplew.writePos(startPos);
        mplew.writePos(startWobble);
        PacketHelper.serializeMovementList(mplew, moves);
        mplew.write(cannotUseSkill);

        return mplew.getPacket();
    }

    public static byte[] spawnMonster(MapleMonster life, int spawnType, int link) {
        return spawnMonster(life, spawnType, link, 1);
    }

    public static byte[] spawnMonster(MapleMonster life, int spawnType, int link, int control) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        mplew.write(0);
        mplew.writeInt(life.getObjectId());
        mplew.write(control); // 1 = Control normal, 3 = update, 5 = Control none
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getPosition().y);
        mplew.write(life.getStance());
        if ((life.getId() == 8910000) || (life.getId() == 8910100)) {
            mplew.write(0); //1이면 뭐가 활성화됨.
            //추가바이트 필요?
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getFh());
        mplew.write(spawnType);

        if (spawnType == -3 || spawnType >= 0) {
            mplew.writeInt(link);
        }

        mplew.write(0xFF);
        mplew.writeLong(life.getHp());
        mplew.writeInt(0); // eff ItemId
        mplew.writeInt(life.getSeperateSoul()); // seperateSoulC
        int per = life.getHPPercent();
        mplew.writeInt(!life.getStats().isMobZone() ? 0 : per == 25 ? 4 : per == 50 ? 3 : per == 75 ? 2 : 1); // MobZoneUpdate
        mplew.writeInt(0); // 1.2.273 ++
        mplew.write(0); //1.2.273 ++
        mplew.writeInt(-1); //afterAction
        mplew.writeInt(0); //1.2.273 ++
        mplew.writeInt(0);//control == 3 ? 1 : -1); //currentAction
        mplew.write(life.isFacingLeft()); //isLeft

        mplew.writeInt(0); //LastAttackTime
        mplew.writeInt(life.getScale()); //scale
        mplew.writeInt(life.getEliteGrade()); //elite grade
        if (life.getEliteGrade() >= 0) {
            mplew.writeInt(life.getEliteGradeInfo().size());
            for (Pair<Integer, Integer> info : life.getEliteGradeInfo()) {
                mplew.writeInt(info.left);
                mplew.writeInt(info.right);
            }
            mplew.writeInt(life.getEliteType());
        }
        mplew.write(false);
        mplew.write(false);
        mplew.writeInt(0);
        mplew.write(false); // 120Byte

        mplew.writeLong(0);

        boolean isUrus = life.getId() == 8881000;
        mplew.writeInt(isUrus ? 1 : 0); // 325 ++, size 우르스 기준 1주고 1250
        if (isUrus) {
            mplew.writeInt(1250);
        }

        mplew.writeInt(0);
        mplew.writeInt(0); // 332++
        mplew.writeInt(0); // 332++
        mplew.writeInt(0); // 332++

        mplew.write(life.getStats().getSkeleton().size());

        for (Triple<String, Integer, Integer> skeleton : life.getStats().getSkeleton()) {
            mplew.writeMapleAsciiString(skeleton.left); // HitPartsName
            mplew.write(skeleton.mid); // unk
            mplew.writeInt(skeleton.right); // HitPartsDurability
        }

        if (life.getId() == 8880102 || life.getId() == 8644650) {
            mplew.writeInt(0);
        }

        mplew.writeZeroBytes(20);

        return mplew.getPacket();
    }

    public static void addMonsterStatus(MaplePacketLittleEndianWriter mplew, MapleMonster life) {
        if (life.getStati().size() <= 1) {
//            life.addEmpty(); //not done yet lulz ok so we add it now for the lulz
        }
        mplew.write(life.getChangedStats() != null ? 1 : 0);

        if (life.getChangedStats() != null) {
            mplew.writeLong(life.getChangedStats().hp);
            mplew.writeInt(life.getChangedStats().mp);
            mplew.writeInt(life.getChangedStats().exp);
            mplew.writeInt(life.getChangedStats().watk);
            mplew.writeInt(life.getChangedStats().matk);
            mplew.writeInt(life.getChangedStats().PDRate);
            mplew.writeInt(life.getChangedStats().MDRate);
            mplew.writeInt(life.getChangedStats().acc);
            mplew.writeInt(life.getChangedStats().eva);
            mplew.writeInt(life.getChangedStats().pushed);
            mplew.writeInt(life.getChangedStats().speed);
            mplew.writeInt(life.getChangedStats().level);
            mplew.writeInt(2100000000); //UserCount?
            mplew.write(0); //335 result?
        }

        PacketHelper.writeMonsterMask(mplew, life.getStati());
        for (Entry<MonsterStatus, MonsterStatusEffect> ms : life.getStati().entrySet()) {
            if (ms.getKey().getFlag() <= MonsterStatus.MS_HangOver.getFlag()) {
                mplew.writeInt(ms.getValue().getValue());
                if (SkillFactory.getSkill(ms.getValue().getSkill()) == null) {
                    mplew.writeShort(ms.getValue().getSkill());
                    mplew.writeShort(ms.getValue().getLevel());
                } else {
                    mplew.writeInt(ms.getValue().getSkill());
                }
                mplew.writeShort(ms.getValue().getDuration() / 1000); // Monsterstatus는 아직 밀리 아님
            }
        }
        DecodeTemporary(mplew, life, life.getStati(), null);
    }

    public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(aggro ? 2 : 1);
        mplew.writeInt(life.getObjectId());

        mplew.write(1);//1); // 1 = Control normal, 5 = Control none
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);

        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getPosition().y);
        mplew.write(life.getStance());
        if ((life.getId() == 8910000) || (life.getId() == 8910100)) {
            mplew.write(0); //1이면 뭐가 활성화됨.
            //추가바이트 필요?
        }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getFh());
        mplew.write(newSpawn ? -2 : life.isFake() ? -4 : -1);

        mplew.write(0xFF);
        mplew.writeLong(life.getHp()); // hp
        mplew.writeInt(0); // eff ItemId
        mplew.writeInt(life.getSeperateSoul()); // seperateSoulC
        int per = life.getHPPercent();
        mplew.writeInt(!life.getStats().isMobZone() ? 0 : per == 25 ? 4 : per == 50 ? 3 : per == 75 ? 2 : 1); // MobZoneUpdate
        mplew.writeInt(0); // 1.2.273 ++
        mplew.write(0); //1.2.273 ++
        mplew.writeInt(-1); //afterAction
        mplew.writeInt(0); //1.2.273 ++
        mplew.writeInt(-1); //currentAction
        mplew.write(life.isFacingLeft()); //isLeft
        mplew.writeInt(0); //LastAttackTime
        mplew.writeInt(life.getScale()); //scale
        mplew.writeInt(life.getEliteGrade()); //elite grade
        if (life.getEliteGrade() >= 0) {
            mplew.writeInt(life.getEliteGradeInfo().size());
            for (Pair<Integer, Integer> info : life.getEliteGradeInfo()) {
                mplew.writeInt(info.left);
                mplew.writeInt(info.right);
            }
            mplew.writeInt(life.getEliteType());
        }
        mplew.write(false);
        mplew.write(false);
        mplew.writeInt(0);
        mplew.write(false); // 120Byte

        mplew.writeLong(0);

        boolean isUrus = life.getId() == 8881000;
        mplew.writeInt(isUrus ? 1 : 0); // 325 ++, size 우르스 기준 1주고 1250
        if (isUrus) {
            mplew.writeInt(1250);
        }

        mplew.writeInt(0);
        mplew.writeInt(0); // 332++
        mplew.writeInt(0); // 332++
        mplew.writeInt(0); // 332++

        mplew.write(life.getStats().getSkeleton().size());

        for (Triple<String, Integer, Integer> skeleton : life.getStats().getSkeleton()) {
            mplew.writeMapleAsciiString(skeleton.left); // HitPartsName
            mplew.write(skeleton.mid); // unk
            mplew.writeInt(skeleton.right); // HitPartsDurability
        }

        if (life.getId() == 8880102 || life.getId() == 8644650) {
            mplew.writeInt(0);
        }

        mplew.writeZeroBytes(20);

        return mplew.getPacket();
    }

    public static byte[] stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] makeMonsterReal(MapleMonster life) {
        return spawnMonster(life, -1, 0);
    }

    public static byte[] makeMonsterFake(MapleMonster life) {
        return spawnMonster(life, -4, 0);
    }

    public static byte[] makeMonsterEffect(MapleMonster life, int effect) {
        return spawnMonster(life, effect, 0);
    }

    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel, int attackIdx) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.write(useSkills); // nextAttackPossible
        mplew.writeInt(currentMp);
        mplew.writeInt(skillId);
        mplew.writeShort(skillLevel);
        mplew.writeInt(attackIdx);//skill == null ? 0 : skill.isOnlyFsm() ? skill.getAction() : 0); // attack Number
        mplew.writeInt(0); // 공격 후딜레이 같음
        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(final MapleMonster mons, final Map<MonsterStatus, MonsterStatusEffect> mse, boolean fromMonster, MapleStatEffect effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(mons.getObjectId());
        PacketHelper.writeMonsterMask(mplew, mse);
        for (Entry<MonsterStatus, MonsterStatusEffect> ms : mse.entrySet()) {
            if (ms.getKey().getFlag() <= MonsterStatus.MS_HangOver.getFlag()) {
                mplew.writeInt(ms.getValue().getValue());
                if (fromMonster) {
                    mplew.writeShort(ms.getValue().getSkill());
                    mplew.writeShort(ms.getValue().getLevel());
                } else {
                    mplew.writeInt(ms.getValue().getSkill());
                    /*if(ms.getValue().getSkill() == 24121010){ //몹스탯0 은 조합이 좀다른듯? 팬텀트와일라잇만 보여서 일단 요렇게
                        mplew.writeInt(-5-(effect.getLevel()/2));
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }*/
                }
                mplew.writeShort(ms.getValue().getDuration() / 1000); // Monsterstatus는 아직 밀리 아님
                /*if(ms.getValue().getSkill() == 24121010){
                    mplew.writeShort(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                }*/
            }
        }
        DecodeTemporary(mplew, mons, mse, effect);
        mplew.writeShort(effect == null ? 0 : effect.getDotInterval()); // delay
        mplew.write(mse.containsKey(MonsterStatus.MS_Freeze) ? 2 : 0);
        for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
            if (MonsterStatus.IsMovementAffectingStat(stat.getKey())) { //
                mplew.write(1);
                break;
            }
        }

        //  System.out.println("몹스태더스 "+mplew);
        /*mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);*/
        return mplew.getPacket();
    }

    private static void DecodeTemporary(MaplePacketLittleEndianWriter mplew, MapleMonster mob, Map<MonsterStatus, MonsterStatusEffect> mse, MapleStatEffect effect) {
        /*if (mse.containsKey(MonsterStatus.MS_Poison)) { // 이거 더미용인데 자꾸 어디서 쓰이는거지 ;;  없음.
            mplew.write(0);
        }
         */
        if (mse.containsKey(MonsterStatus.MS_Pdr)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_Mdr)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_Speed)) {
            mplew.write(mob.getFreezingOverlap());
        }
        if (mse.containsKey(MonsterStatus.MS_Freeze)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_PCounter)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_MCounter)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_PCounter) || mse.containsKey(MonsterStatus.MS_MCounter)) {
            mplew.writeInt(0); //nCounterProb_
            mplew.write(0);  //!= 0 bCounterDelay_
            mplew.writeInt(0); //nAggroRank_
        }

        if (mse.containsKey(MonsterStatus.MS_AdddamParty)) {
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_AdddamParty) {
                    eff = stat.getValue();
                    break;
                }
            }
            mplew.writeInt(eff.getChr().getId());
            //mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
            if (eff.getSkill() == 4321002) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            } else {
                //   mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getParty() == null ? 0 : eff.getChr().getParty().getId() : 0 : 0);
                  mplew.writeInt(effect == null ? 0 : effect.getX()); // 피니투라 페투치아 한정. (파티원 데미지 증가 값임)
                mplew.writeInt(0);
            }

        }

        if (mse.containsKey(MonsterStatus.MS_HitCritDamR)) {
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_HitCritDamR) {
                    eff = stat.getValue();
                    break;
                }
            }
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            //   mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
            //   mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getParty() == null ? 0 : eff.getChr().getParty().getId() : 0 : 0);
            //  mplew.writeInt(effect == null ? 0 : effect.getX());
        }

        if (mse.containsKey(MonsterStatus.MS_Lifting)) { // 0x800000
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_Lifting) {
                    eff = stat.getValue();
                    break;
                }
            }
            //   mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
            //   mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getParty() == null ? 0 : eff.getChr().getParty().getId() : 0 : 0);
            //    mplew.writeInt(effect == null ? 0 : effect.getX());
            //   mplew.writeInt(0); // 343 맞추면서 보니 4개임.
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_FixdamRBuff)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_TempMoveAbility)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_ElementDarkness)) {
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_DeadlyCharge)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_Incizing)) {
            mplew.writeInt(mob.getObjectId()); // ObjectId
            mplew.writeInt(10); // ?
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_BMageDebuff)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_DarkLightning)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_MultiPMDR)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_UnkFlameWizard)) {
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_UnkFlameWizard) {
                    eff = stat.getValue();
                    break;
                }
            }
            mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
            mplew.writeInt(0);
            mplew.writeInt(100);
            mplew.writeInt(1);
        }

        if (mse.containsKey(MonsterStatus.MS_ElementResetBySummon)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_DragonStrike)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_CurseMark)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_PopulatusTimer)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_BahamutLightElemAddDam)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_Ambush)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(83)) {  //0x1000
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_Calabash)) {
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_Calabash) {
                    eff = stat.getValue();
                    break;
                }
            }
            mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
        }
        if (mse.containsKey(MonsterStatus.MS_Gathering)) {
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_Gathering) {
                    eff = stat.getValue();
                    break;
                }
            }
            mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
        }
        if (mse.containsKey(MonsterStatus.MS_Invincible)) {
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_Burned)) {
            mplew.write(mob.getIgnitions().size()); // count for special packet
            for (Ignition ignition : mob.getIgnitions()) {
                mplew.writeInt(ignition.getOwnerId()); // Mob ownerId
                mplew.writeInt(0);
                mplew.writeInt(ignition.getSkill());
                mplew.writeLong(ignition.getDamage()); // damage
                mplew.writeInt(ignition.getInterval());
                mplew.writeInt(ignition.getIgnitionKey()); // ?
                mplew.writeInt(ignition.getDuration());
                mplew.writeInt(ignition.getDuration() / 1000);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(700); //was 300
            }
        }
        if (mse.containsKey(MonsterStatus.MS_BalogDisable)) {
            mplew.write(0);
            mplew.write(0);
        }
        if (mse.containsKey(MonsterStatus.MS_ExchangeAttack)) {
            mplew.write(1);
        }
        if (mse.containsKey(MonsterStatus.MS_AddBuffStat)) { //0x2000000
            mplew.write(0); // 1 > writeInt 4개
        }
        if (mse.containsKey(MonsterStatus.MS_LinkTeam)) {
            mplew.writeMapleAsciiString("");
        }
        if (mse.containsKey(MonsterStatus.MS_112)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_113)) {
            mplew.writeLong(0);
        }
        if (mse.containsKey(MonsterStatus.MS_118)) {
            mplew.write(0);
        }
        if (mse.containsKey(MonsterStatus.MS_114)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_115)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_116)) {
            mplew.writeInt(0);
        }
        if (mse.containsKey(MonsterStatus.MS_117)) {
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_SoulExplosion)) {
            mplew.writeInt(50);
            mplew.writeInt(mob.getObjectId()); // oid
            mplew.writeInt(43);
        }

        if (mse.containsKey(MonsterStatus.MS_SeperateSoulP)) {
            mplew.writeInt(50);
            mplew.writeInt(mob.getObjectId()); // oid
            mplew.writeShort(17425);
            mplew.writeInt(43);
            mplew.writeInt(mse.get(MonsterStatus.MS_SeperateSoulP).getSkill()); // skillid
        }

        if (mse.containsKey(MonsterStatus.MS_SeperateSoulC)) {
            mplew.writeInt(50);
            mplew.writeInt(mob.getSeperateSoul()); // 본체의 objectId
            mplew.writeShort(17425);
            mplew.writeInt(43);
        }

        if (mse.containsKey(MonsterStatus.MS_Ember)) {
            MonsterStatusEffect eff = null;
            for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
                if (stat.getKey() == MonsterStatus.MS_Ember) {
                    eff = stat.getValue();
                    break;
                }
            }
            mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getBuffedEffect(12101024) != null ? eff.getChr().getBuffedEffect(12101024).getDOTTime() : 0 : 0 : 0);
            mplew.writeInt(mse.get(MonsterStatus.MS_Ember).getSkill());
            mplew.writeInt(0);
            mplew.writeInt(eff != null ? eff.getChr() != null ? eff.getChr().getId() : 0 : 0);
            mplew.writeInt(5);
        }

        if (mse.containsKey(MonsterStatus.MS_TrueSight)) {
            mplew.writeInt(0);
            mplew.writeInt(mse.get(MonsterStatus.MS_TrueSight).getSkill());
            mplew.writeInt(0); // CD D8 27 10 오브젝트 아이디, 캐릭터 아이디 아님
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_Laser)) {
            mplew.writeInt(1);
            mplew.writeShort(mse.get(MonsterStatus.MS_Laser).getSkill());
            mplew.writeShort(mse.get(MonsterStatus.MS_Laser).getLevel());
            mplew.writeInt(1955733479); // unk
            mplew.writeInt(1);
            mplew.writeInt(0x8D);
        }

        if (mse.containsKey(MonsterStatus.MS_111)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        if (mse.containsKey(MonsterStatus.MS_119)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        /*
        if ( *((_BYTE *)v8 + 0xC) & 0x80 )
        {
    *((_DWORD *)v5 + 343) = CInPacket::Decode4(v6);
    *((_DWORD *)v5 + 344) = CInPacket::Decode4(v6);
    *((_DWORD *)v5 + 345) = CInPacket::Decode4(v6);
        }*/

        if (mse.containsKey(MonsterStatus.MS_1)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
    }

    public static byte[] cancelMonsterStatus(final MapleMonster mons, final Map<MonsterStatus, MonsterStatusEffect> mse) {
        List<Ignition> removes = new ArrayList<>();
        return cancelMonsterStatus(mons, mse, removes);
    }

    public static byte[] cancelMonsterStatus(final MapleMonster mons, final Map<MonsterStatus, MonsterStatusEffect> mse, List<Ignition> removes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(mons.getObjectId());
        PacketHelper.writeMonsterMask(mplew, mse);

        if (mse.containsKey(MonsterStatus.MS_Burned)) {
            mplew.writeInt(0);
            if (removes.size() > 0) {
                mplew.writeInt(removes.size());
                for (Ignition remove : removes) {
                    // System.out.println(remove.getSkill()+" monsterBurned");
                    mplew.writeInt(remove.getOwnerId());
                    mplew.writeInt(remove.getSkill());
                    mplew.writeInt(1000);
                }
            } else {
                mplew.writeInt(mons.getIgnitions().size());
                for (Ignition ig : mons.getIgnitions()) {
                    mplew.writeInt(ig.getOwnerId());
                    mplew.writeInt(ig.getSkill());
                    mplew.writeInt(0);
                }
            }
            mplew.write(0x0A);
        }

        for (Entry<MonsterStatus, MonsterStatusEffect> stat : mse.entrySet()) {
            if (MonsterStatus.IsMovementAffectingStat(stat.getKey())) { //
                mplew.write(1);
                break;
            }
        }

        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] applyStatusAttack(int oid, int skillId, MapleStatEffect effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.APPLY_STATUS_ATTACK.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        mplew.writeShort(effect == null ? 0 : effect.getDotInterval());
        mplew.write(1);
        mplew.writeInt(effect == null ? 0 : effect.getDuration() / 1000);

        return mplew.getPacket();
    }

    public static byte[] talkMonster(int oid, int itemId, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TALK_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(500); //?
        mplew.writeInt(itemId);
        mplew.write(itemId <= 0 ? 0 : 1);
        mplew.write(msg == null || msg.length() <= 0 ? 0 : 1);
        if (msg != null && msg.length() > 0) {
            mplew.writeMapleAsciiString(msg);
        }
        mplew.writeInt(1); //?

        return mplew.getPacket();
    }

    public static byte[] removeTalkMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_TALK_MONSTER.getValue());
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static final byte[] SpeakingMonster(MapleMonster mob, int type, int unk2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPEAK_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(type);
        mplew.writeInt(unk2);

        return mplew.getPacket();
    }

    public static byte[] showMagnet(int mobid, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success ? 1 : 0);
        mplew.write(0); // times, 0 = once, > 0 = twice

        return mplew.getPacket();
    }

    public static byte[] catchMonster(int mobid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CATCH_MONSTER.getValue());
        mplew.writeInt(mobid);
        mplew.write(success);
        mplew.write(success);

        return mplew.getPacket();
    }

    public static byte[] changePhase(MapleMonster mob) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHANGE_PHASE.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(mob.getPhase());
        mplew.write(mob.getPhase());
        return mplew.getPacket();
    }

    public static byte[] changeMobZone(MapleMonster mob) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHANGE_MOBZONE.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(mob.getPhase());
        return mplew.getPacket();
    }

    public static byte[] dropStone(String name, List<Point> data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DROP_STONE.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeInt(0);
        mplew.writeInt(data.size());
        for (Point a : data) {
            mplew.writeInt(a.x);
            mplew.writeInt(a.y);
        }
        return mplew.getPacket();
    }

    public static byte[] createObstacle(MapleMonster mob, List<Obstacle> obs, byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CREATE_OBSTACLE.getValue());
        mplew.writeInt(0); // objectId?
        mplew.writeInt(obs.size());
        mplew.write(type);

        if (type == 4) {
            mplew.writeInt(mob.getId());

            //포지션
            mplew.writeInt(mob.getPosition().x);
            mplew.writeInt(obs.get(0).getOldPosition().y);
            mplew.writeInt(obs.get(0).getHeight());
            mplew.writeInt(0);
        }
        for (Obstacle ob : obs) {
            mplew.write(1);
            mplew.writeInt(ob.getKey()); // type
            mplew.writeInt(Randomizer.nextInt()); // crc
            mplew.writeInt(ob.getOldPosition().x);
            mplew.writeInt(ob.getOldPosition().y);
            mplew.writeInt(ob.getNewPosition().x);
            mplew.writeInt(ob.getNewPosition().y);
            mplew.writeInt(ob.getRange());
            mplew.writeZeroBytes(16); // 351 new
            mplew.writeInt(ob.getTrueDamage()); // HP% Damage
            mplew.writeInt(ob.getDelay());
            mplew.writeInt(ob.getHeight());
            mplew.writeInt(ob.getVperSec());
            mplew.writeInt(ob.getMaxP()); // 바닥 도달까지 속도
            mplew.writeInt(ob.getLength());
            mplew.writeInt(ob.getAngle());
            mplew.writeInt(ob.getUnk());
            if (type == 5) {
                mplew.writeInt(mob.getId());

                //포지션
                mplew.writeInt(mob.getPosition().x);
                mplew.writeInt(obs.get(0).getOldPosition().y);
                mplew.writeInt(obs.get(0).getHeight());
                mplew.writeInt(0);
            }
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] createObstacle2(MapleMonster mob, Obstacle ob, byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CREATE_OBSTACLE2.getValue());

        //1개씩만 쏘는 패킷
        mplew.write(type);

        mplew.write(ob != null);

        if (ob != null) {
            mplew.writeInt(ob.getKey()); // type
            mplew.writeInt(Randomizer.nextInt()); // crc
            mplew.writeInt(ob.getOldPosition().x);
            mplew.writeInt(ob.getOldPosition().y);
            mplew.writeInt(ob.getNewPosition().x);
            mplew.writeInt(ob.getNewPosition().y);
            mplew.writeInt(ob.getRange());
            mplew.writeZeroBytes(16); // 351 new
            mplew.writeInt(ob.getTrueDamage()); // HP% Damage
            mplew.writeInt(ob.getDelay());
            mplew.writeInt(ob.getHeight());
            mplew.writeInt(ob.getVperSec());
            mplew.writeInt(ob.getMaxP()); // 바닥 도달까지 속도
            mplew.writeInt(ob.getLength());
            mplew.writeInt(ob.getAngle());
            mplew.writeInt(ob.getUnk());
            mplew.writeInt(0); // 351 new

            mplew.write(false);
        }

        mplew.writeInt(0); // 351 new
        mplew.writeInt(0); // 351 new

        return mplew.getPacket();
    }

    public static byte[] BlockAttack(MapleMonster mob, List<Integer> ids) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BLOCK_ATTACK.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(ids.size());
        for (Integer a : ids) {
            mplew.writeInt(a);
        }
        return mplew.getPacket();
    }

    public static byte[] TeleportMonster(MapleMonster monster_, boolean afterAction, int type, Point point) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TELEPORT_MONSTER.getValue());
        mplew.writeInt(monster_.getObjectId());
        mplew.write(afterAction);
        if (!afterAction) {
            mplew.writeInt(type);
            switch (type) {
                case 3:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 14:
                    mplew.writeInt(point.x);
                    mplew.writeInt(point.y);
                    break;
                case 4:
                    mplew.writeInt(0); // ?
                    break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] createEnergySphere(int oid, int skillLevel, MapleEnergySphere mes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENERGY_SPHERE.getValue()); // op
        mplew.writeInt(oid);
        mplew.writeInt(217); //id
        mplew.writeInt(skillLevel); // lv
        mplew.write(mes.isOk());

        if (mes.isOk()) {
            mplew.writeInt(mes.getSize()); // size
            mplew.write(mes.isDelayed()); // isDelayed
            mplew.writeInt(mes.getY()); // Y
            mplew.writeInt(mes.getDensity()); // 밀도?
            mplew.writeInt(mes.getFriction()); // 마찰?

            mplew.writeInt(mes.getStartDelay());
            mplew.writeInt(mes.getDestroyDelay());

            //for
            for (int i = 0; i < mes.getSize(); i++) {
                mplew.writeInt(mes.getObjectId() + i);
            }
        } else {
            mplew.writeInt(0);
            mplew.writeInt(mes.getX());
            mplew.writeInt(1); // size

            mplew.writeInt(mes.getObjectId()); // objectSN

            mplew.writeInt(25);
            mplew.writeInt(25);

            mplew.writeInt(0); // objectSN
            mplew.writeInt(mes.getRetitution()); // retitution
            mplew.writeInt(mes.getDestroyDelay()); // destroydelay
            mplew.writeInt(mes.getStartDelay()); // startdelay
            mplew.writeInt(0); // 1.2.324++
            mplew.write(mes.isNoGravity()); // nogravity
            mplew.write(mes.isNoDeleteFromOthers()); // nodeletefrom?
            if (skillLevel == 3 || skillLevel == 4 || skillLevel == 21) { // 217 AND 3 or 4
                mplew.writeInt(5); // maxspeedX
                mplew.writeInt(5); // maxspeedY
            }
            if (mes.isNoDeleteFromOthers()) { // nodeletefrom?
                mplew.writeInt(mes.getRetitution());
                mplew.writeInt(skillLevel == 17 ? 250 : skillLevel == 15 ? 200 : 300);
                mplew.writeInt(24);
                mplew.writeInt(8);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] blockMoving(int objectId, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BLOCK_MOVING.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(value);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] MobSkillDelay(int objectId, int skillID, int skillLv, int skillAfter, short sequenceDelay, List<Rectangle> skillRectInfo) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOBSKILL_DELAY.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(skillAfter);
        mplew.writeInt(skillID);
        mplew.writeInt(skillLv);
        mplew.writeInt(skillID == 230 ? 900 : 0); // 307++
        if (skillRectInfo == null || skillRectInfo.isEmpty()) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        } else {
            mplew.writeInt(sequenceDelay);
            mplew.writeInt(skillRectInfo.size());
            for (Rectangle rect : skillRectInfo) {
                mplew.writeInt(rect.x);
                mplew.writeInt(rect.y);
                mplew.writeInt(rect.x + rect.width);
                mplew.writeInt(rect.y + rect.height);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] incinerateObject(MapleIncinerateObject mio, boolean isSpawn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.STIGMA_INCINERATE_OBJECT.getValue());
        mplew.writeInt(isSpawn ? 0 : 1);
        if (isSpawn) {
            mplew.writeInt(mio.getX());
            mplew.writeInt(mio.getY());
            mplew.writeInt(2500); // delay?
            mplew.writeInt(mio.getObjectId());//mio.getObjectId()); // 아마 objectId?
            mplew.writeMapleAsciiString("Map/Obj/BossDemian.img/demian/altar");
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static byte[] FlyingSword(MapleFlyingSword mfs, boolean isCreate) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FLYING_SWORD_CREATE.getValue());
        mplew.write(isCreate);
        mplew.writeInt(mfs.getObjectId());
        if (isCreate) {
            mplew.write(mfs.getObjectType());
            mplew.write(4);
            mplew.writeInt(mfs.getOwner().getId());
            mplew.writeInt(mfs.getOwner().getTruePosition().x);
            mplew.writeInt(mfs.getOwner().getTruePosition().y);
        }
        return mplew.getPacket();
    }

    public static byte[] FlyingSwordNode(MapleFlyingSword mfs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FLYING_SWORD_NODE.getValue());
        mplew.writeInt(mfs.getObjectId());
        mplew.writeInt(mfs.getTarget() == null ? 0 : mfs.getTarget().getId());
        mplew.write(mfs.isStop());
        mplew.writeInt(mfs.getNodes().size());

        for (FlyingSwordNode fsn : mfs.getNodes()) {
            mplew.write(fsn.getNodeType());
            mplew.writeShort(fsn.getPathIndex());
            mplew.writeShort(fsn.getNodeIndex());
            mplew.writeShort(fsn.getV());
            mplew.writeInt(fsn.getStartDelay());
            mplew.writeInt(fsn.getEndDelay());
            mplew.writeInt(fsn.getDuration());
            mplew.write(fsn.isHide());
            mplew.write(fsn.getCollisionType());
            mplew.writeInt(fsn.getPos().x);
            mplew.writeInt(fsn.getPos().y);
        }

        return mplew.getPacket();
    }

    public static byte[] FlyingSwordTarget(MapleFlyingSword mfs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FLYING_SWORD_TARGET.getValue());
        mplew.writeInt(mfs.getObjectId());
        mplew.writeInt(mfs.getTarget().getId());
        return mplew.getPacket();
    }

    public static byte[] FlyingSwordMakeEnterRequest(MapleFlyingSword mfs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FLYING_SWORD_MAKE_ENTER_REQUEST.getValue());
        mplew.writeInt(mfs.getObjectId());
        mplew.writeInt(mfs.getTarget().getId());
        return mplew.getPacket();
    }

    public static byte[] ZakumAttack(MapleMonster mob, String skeleton) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ZAKUM_ATTACK.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeMapleAsciiString(skeleton);

        return mplew.getPacket();
    }

    public static byte[] onDemianDelayedAttackCreate(int skillId, int skillLevel, MapleDelayedAttack mda) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DEMIAN_ATTACK_CREATE.getValue());
        mplew.writeInt(mda.getOwner().getObjectId());
        mplew.writeInt(skillId);
        mplew.writeInt(skillLevel);
        switch (skillLevel) {
            /*        	case 42: {
             mplew.writeInt(mob.isFacingLeft() ? 1 : 0);
             }*/
            case 44:
            case 45:
            case 46:
            case 47: {
                mplew.write(mda.isIsfacingLeft()); // bFlip
                mplew.writeInt(1);
                mplew.writeInt(mda.getObjectId());
                mplew.writeInt(mda.getPos().x); // 
                mplew.writeInt(mda.getPos().y); // 
                break;
            }
        }

        return mplew.getPacket();
    }

    public static byte[] onDemianDelayedAttackCreate(MapleMonster mob, int skillId, int skillLevel, List<MapleDelayedAttack> mda) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DEMIAN_ATTACK_CREATE.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(skillId);
        mplew.writeInt(skillLevel);
        switch (skillLevel) {
            case 42: {
                mplew.write(mob.isFacingLeft() ? 1 : 0);
                mplew.writeInt(1); // AttackIdx

                mplew.writeInt(mda.size());
                for (MapleDelayedAttack att : mda) {
                    mplew.writeInt(att.getObjectId());
                    mplew.writeInt(att.getPos().x);
                    mplew.writeInt(att.getPos().y);
                    mplew.writeInt(att.getAngle());
                }
                break;
            }
        }

        return mplew.getPacket();
    }

    public static class BossLucid {

        public static byte[] createButterfly(int initId, List<Butterfly> butterflies) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_BUTTERFLY_CREATE.getValue());
            mplew.writeInt(initId);//not sure
            mplew.writeInt(butterflies.size());
            for (Butterfly butterfly : butterflies) {
                mplew.writeInt(butterfly.type);
                mplew.writeInt(butterfly.pos.x);
                mplew.writeInt(butterfly.pos.y);
            }
            return mplew.getPacket();
        }

        /**
         * @param mode Butterfly.Mode ADD/MOVE/ATTACK/ERASE
         * @param args ADD: initId(not sure), typeId, posX, posY MOVE: posX,
         * poxY ATTACK: count, startDelay
         * @return
         */
        public static byte[] setButterflyAction(Butterfly.Mode mode, int... args) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_BUTTERFLY_ACTION.getValue());
            mplew.writeInt(mode.code);
            switch (mode) {
                case ADD:
                    mplew.writeInt(args[0]);
                    mplew.writeInt(args[1]);
                    mplew.writeInt(args[2]);
                    mplew.writeInt(args[3]);
                    break;
                case MOVE:
                    mplew.writeInt(args[0]);
                    mplew.writeInt(args[1]);
                    break;
                case ATTACK:
                    mplew.writeInt(args[0]);
                    mplew.writeInt(args[1]);
                    break;
                default:
                    break;
            }
            return mplew.getPacket();
        }

        public static byte[] createDragon(int phase, int posX, int posY, int createPosX, int createPosY, boolean isLeft) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_DRAGON_CREATE.getValue());
            mplew.writeInt(phase);
            mplew.writeInt(posX);
            mplew.writeInt(posY);
            mplew.writeInt(createPosX);
            mplew.writeInt(createPosY);
            mplew.write(isLeft);
            return mplew.getPacket();
        }

        public static byte[] doFlowerTrapSkill(int level, int pattern, int x, int y, boolean flip) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_DO_SKILL.getValue());
            mplew.writeInt(238);
            mplew.writeInt(level);//1~3
            mplew.writeInt(pattern);//0~2
            mplew.writeInt(x);
            mplew.writeInt(y);
            mplew.write(flip);//not sure
            return mplew.getPacket();
        }

        public static byte[] doLaserRainSkill(int startDelay, List<Integer> intervals) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_DO_SKILL.getValue());
            mplew.writeInt(238);
            mplew.writeInt(5);
            mplew.writeInt(startDelay);
            mplew.writeInt(intervals.size());
            for (int interval : intervals) {
                mplew.writeInt(interval);
            }
            return mplew.getPacket();
        }

        public static byte[] doFairyDustSkill(int level, List<FairyDust> fairyDust) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_DO_SKILL.getValue());
            mplew.writeInt(238);
            mplew.writeInt(level);//4 or 10
            mplew.writeInt(fairyDust.size());
            for (FairyDust fd : fairyDust) {
                mplew.writeInt(fd.scale);
                mplew.writeInt(fd.createDelay);
                mplew.writeInt(fd.moveSpeed);
                mplew.writeInt(fd.angle);
            }
            return mplew.getPacket();
        }

        public static byte[] doForcedTeleportSkill(int splitId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_DO_SKILL.getValue());
            mplew.writeInt(238);
            mplew.writeInt(6);
            mplew.writeInt(splitId);//0~7
            return mplew.getPacket();
        }

        public static byte[] doRushSkill() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_DO_SKILL.getValue());
            mplew.writeInt(238);
            mplew.writeInt(8);
            mplew.writeInt(0);//only path0 exists o.O
            return mplew.getPacket();
        }

        public static byte[] setStainedGlassOnOff(boolean enable, List<String> tags) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_STAINED_GLASS_ON_OFF.getValue());
            mplew.write(enable);
            mplew.writeInt(tags.size());
            for (String name : tags) {
                mplew.writeMapleAsciiString(name);
            }
            return mplew.getPacket();
        }

        public static byte[] breakStainedGlass(List<String> tags) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_STAINED_GLASS_BREAK.getValue());
            mplew.writeInt(tags.size());
            for (String name : tags) {
                mplew.writeMapleAsciiString(name);
            }
            return mplew.getPacket();
        }

        public static byte[] setFlyingMode(boolean enable) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_SET_FLYING_MODE.getValue());
            mplew.write(enable);
            return mplew.getPacket();
        }

        public static byte[] changeStatueState(boolean placement, int gauge, boolean used) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID_STATUE_STATE_CHANGE.getValue());
            mplew.writeInt(placement ? 1 : 0);
            if (placement) {
                mplew.write(used);
            } else {
                mplew.writeInt(gauge);
                mplew.write(used);
            }
            return mplew.getPacket();
        }

        public static byte[] doShoot(int angle, int speed) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_WELCOME_BARRAGE.getValue());
            mplew.writeInt(0);
            mplew.writeInt(angle);
            mplew.writeInt(speed);
            return mplew.getPacket();
        }

        public static byte[] doBidirectionShoot(int angleRate, int speed, int interval, int shotCount) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_WELCOME_BARRAGE.getValue());
            mplew.writeInt(3);
            mplew.writeInt(angleRate);
            mplew.writeInt(speed);
            mplew.writeInt(interval);
            mplew.writeInt(shotCount);
            return mplew.getPacket();
        }

        public static byte[] doSpiralShoot(int angle, int angleRate, int angleDiff, int speed, int interval, int shotCount, int bulletAngleRate, int bulletSpeedRate) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_WELCOME_BARRAGE.getValue());
            mplew.writeInt(4);
            mplew.writeInt(angle);
            mplew.writeInt(angleRate);
            mplew.writeInt(angleDiff);
            mplew.writeInt(speed);
            mplew.writeInt(interval);
            mplew.writeInt(shotCount);
            mplew.writeInt(bulletAngleRate);
            mplew.writeInt(bulletSpeedRate);
            return mplew.getPacket();
        }

        /**
         * @param type should be 1/2/5. 1 : ??? 2 : Start skill action 5 : Stop
         * skill action (for delaying or... idk) 0/3/4 see below
         */
        public static byte[] doWelcomeBarrageSkill(int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID2_WELCOME_BARRAGE.getValue());
            mplew.writeInt(type);
            return mplew.getPacket();
        }

        public static byte[] Lucid3rdPhase(int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUCID3_PHASE.getValue());
            mplew.writeInt(type);
            return mplew.getPacket();
        }
    }

    public static class BossWill {

        public static byte[] setMoonGauge(int max, int min) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_SET_MOONGAUGE.getValue());
            mplew.writeInt(max); // 최댓값
            mplew.writeInt(min); // 사용하기 위한 최솟값

            return mplew.getPacket();
        }

        public static byte[] addMoonGauge(int add) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_MOONGAUGE.getValue());
            mplew.writeInt(add);

            return mplew.getPacket();
        }

        public static byte[] cooldownMoonGauge(int length) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_COOLTIME_MOONGAUGE.getValue());
            mplew.writeInt(length);

            return mplew.getPacket();
        }

        public static byte[] createBulletEyes(int... args) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_CREATE_BULLETEYE.getValue());
            mplew.writeInt(args[0]);
            mplew.writeInt(args[1]);
            mplew.writeInt(args[2]);
            mplew.writeInt(args[3]);
            if (args[0] == 1) {
                mplew.writeInt(1800);
                mplew.writeInt(5);
                mplew.write(true);
                mplew.writeInt(args[4]);
                mplew.writeInt(args[5]);
                mplew.writeInt(args[6]);
                mplew.writeInt(args[7]);
            }
            return mplew.getPacket();
        }

        public static byte[] setWillHp(List<Integer> counts, MapleMap map, int mobId1, int mobId2, int mobId3) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_SET_HP.getValue());
            mplew.writeInt(counts.size());
            for (int i : counts) {
                mplew.writeInt(i);
            }

            MapleMonster life1 = map.getMonsterById(mobId1);
            MapleMonster life2 = map.getMonsterById(mobId2);
            MapleMonster life3 = map.getMonsterById(mobId3);

            mplew.write(life1 != null);
            if (life1 != null) {
                mplew.writeInt(life1.getId());
                mplew.writeLong(life1.getHp());
                mplew.writeLong((long) (life1.getMobMaxHp() * life1.bonusHp()));
            }

            mplew.write(life2 != null);
            if (life2 != null) {
                mplew.writeInt(life2.getId());
                mplew.writeLong(life2.getHp());
                mplew.writeLong((long) (life2.getMobMaxHp() * life2.bonusHp()));
            }

            mplew.write(life3 != null);
            if (life3 != null) {
                mplew.writeInt(life3.getId());
                mplew.writeLong(life3.getHp());
                mplew.writeLong((long) (life3.getMobMaxHp() * life3.bonusHp()));
            }

            return mplew.getPacket();
        }

        public static byte[] setWillHp(List<Integer> counts) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_SET_HP2.getValue());
            mplew.writeInt(counts.size());
            for (int i : counts) {
                mplew.writeInt(i);
            }

            return mplew.getPacket();
        }

        public static byte[] WillSpiderAttack(int id, int skill, int level, int type, List<Triple<Integer, Integer, Integer>> values) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_SPIDER_ATTACK.getValue());
            mplew.writeInt(id);
            mplew.writeInt(skill);
            mplew.writeInt(level);

            switch (level) {
                case 1:
                case 2:
                case 3:
                case 14: {
                    if (level == 14) {
                        mplew.writeInt(type); // 거울 깨짐 여부
                    }
                    mplew.writeInt(level == 14 ? 9 : 4);
                    mplew.writeInt(1200); // 공격 맞을 때까지 딜레이
                    mplew.writeInt(level == 14 ? 5000 : 9000); // duration

                    //lt, rb
                    mplew.writeInt((level == 14 && type == 2) ? - 60 : -40);
                    mplew.writeInt(-600);
                    mplew.writeInt((level == 14 && type == 2) ? 60 : 40);
                    mplew.writeInt(10);

                    mplew.writeInt(values.size());

                    for (Triple<Integer, Integer, Integer> value : values) {
                        mplew.writeInt(value.left);
                        //level == 14 ? (1500 * (1 + (i / 4))) : (1800 * (1 + (i / 6)))
                        mplew.writeInt(value.mid); // 300++, 딜레이
                        //-650 + (130 * Randomizer.rand(1, 9))
                        mplew.writeInt(value.right); // 130++, 시작 x좌표
                        mplew.writeInt(0);
                    }
                    break;
                }
                case 4: {
                    //1이면 위쪽, 나머지는 아래쪽으로 워프
                    mplew.writeInt(type);
                    mplew.write(type != 0);
                    break;
                }
                case 5: {
                    mplew.writeInt(2);

                    if (type == 0) {
                        mplew.write(0);

                        mplew.writeInt(-690);
                        mplew.writeInt(-455);
                        mplew.writeInt(695);
                        mplew.writeInt(160);

                        mplew.write(1);

                        mplew.writeInt(-690);
                        mplew.writeInt(-2378);
                        mplew.writeInt(695);
                        mplew.writeInt(-2019);
                    } else {
                        mplew.write(0);

                        mplew.writeInt(-690);
                        mplew.writeInt(-2378);
                        mplew.writeInt(695);
                        mplew.writeInt(-2019);

                        mplew.write(1);

                        mplew.writeInt(-690);
                        mplew.writeInt(-455);
                        mplew.writeInt(695);
                        mplew.writeInt(160);
                    }
                    break;
                }
            }

            return mplew.getPacket();
        }

        public static byte[] willUseSpecial() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_USE_SPECIAL.getValue());

            return mplew.getPacket();
        }

        public static byte[] willStun() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_STUN.getValue());

            return mplew.getPacket();
        }

        public static byte[] willThirdOne() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_THIRD_ONE.getValue());
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] willSpider(int type, SpiderWeb web) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_SPIDER.getValue());
            mplew.writeInt(type); // 3 : create, 4 : destroy, 5 : disable
            if (type == 3 || type == 4) {
                mplew.writeInt(web.getObjectId());

                mplew.writeInt(web.getPattern());

                mplew.writeInt(web.getX1());
                mplew.writeInt(web.getY1());

                switch (web.getPattern()) {
                    case 0:
                        mplew.writeInt(100);
                        mplew.writeInt(100);
                        break;
                    case 1:
                        mplew.writeInt(160);
                        mplew.writeInt(160);
                        break;
                    case 2:
                        mplew.writeInt(270);
                        mplew.writeInt(270);
                        break;
                    default:
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        break;
                }
            }
            return mplew.getPacket();
        }

        public static byte[] teleport() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_TELEPORT.getValue());
            mplew.writeInt(1);

            return mplew.getPacket();
        }

        public static byte[] poison(int objectId, int ownerId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_POISON.getValue());
            mplew.writeInt(objectId); // objectId
            mplew.writeInt(ownerId);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] removePoison(int objectId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.WILL_POSION_REMOVE.getValue());
            mplew.writeInt(1);
            mplew.writeInt(objectId); // objectId

            return mplew.getPacket();
        }

    }

    public static byte[] demianRunaway(MapleMonster monster, byte type, MobSkill mobSkill, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DEMIAN_RUNAWAY.getValue());
        mplew.writeInt(monster.getObjectId());
        mplew.write(type);

        switch (type) {
            case 0:
                mplew.writeInt(mobSkill.getSkillLevel());
                mplew.writeInt(duration); // 10000ms
                mplew.writeShort(0);
                mplew.write(1);
                break;
            case 1:
                mplew.write(0);
                mplew.writeInt(30); // ?
                mplew.writeInt(mobSkill.getSkillId());
                mplew.writeInt(mobSkill.getSkillLevel());
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] enableOnlyFsmAttack(MapleMonster mob, int skill, int unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENABLE_ONLYFSM_ATTACK.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(skill);
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static byte[] ChangePhaseDemian(MapleMonster mob, int unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DEMIAN_PHASE_CHANGE.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(unk);
        return mplew.getPacket();
    }

    public static byte[] CorruptionChange(byte phase, int qty) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CORRUPTION_CHANGE.getValue());
        mplew.write(phase);
        mplew.writeInt(qty);
        return mplew.getPacket();
    }

    public static byte[] addStigma(MapleCharacter chr, int count) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ADD_STIGMA.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(count); //제단 풀기 활성비활성화 논리문이였네
        return mplew.getPacket();
    }

    public static byte[] jinHillahBlackHand(int objectId, int skillId, int skillLv, List<Triple<Point, Integer, List<Rectangle>>> points) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.JINHILLAH_BLACK_HAND.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(points.size());

        int i = 0;
        for (Triple<Point, Integer, List<Rectangle>> point : points) {
            i++;
            mplew.writeInt(1);

            mplew.writeInt(i);
            mplew.writeInt(skillId);
            mplew.writeInt(skillLv);
            mplew.write(true);

            mplew.writeInt(1);

            mplew.writePos(point.left);

            mplew.writeInt(point.mid); // 딜레이
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeInt(point.right.size());

            for (Rectangle rect : point.right) {
                mplew.writeRect(rect);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] jinHillahSpirit(int objectId, int cid, Rectangle rect, Point pos, int skillLv) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.JINHILLAH_SPIRIT.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(cid); // ?

        mplew.writeInt(rect.x);
        mplew.writeInt(rect.y);
        mplew.writeInt(rect.width);
        mplew.writeInt(rect.height);

        mplew.writePos(pos);

        mplew.writeInt(1); // Idx?
        mplew.writeInt(246); // skillId
        mplew.writeInt(skillLv);
        mplew.write(true);

        mplew.writeInt(1);

        int a = -3;
        if (pos.getX() >= -870 && pos.getX() < -620) {
            a = -3;
        } else if (pos.getX() >= -620 && pos.getX() < -470) {
            a = -2;
        } else if (pos.getX() >= -470 && pos.getX() < -220) {
            a = -1;
        } else if (pos.getX() >= -220 && pos.getX() < 30) {
            a = 0;
        } else if (pos.getX() >= 30 && pos.getX() < 280) {
            a = 1;
        } else if (pos.getX() >= 280 && pos.getX() < 530) {
            a = 2;
        } else {
            a = 3;
        }
        int t = 0;
        switch (a) {
            case -3:
                t = 0;
                break;
            case -2:
                t = 1;
                break;
            case -1:
                t = 2;
                break;
            case 0:
                t = 3;
                break;
            case 1:
                t = 4;
                break;
            case 2:
                t = 5;
                break;
            case 3:
                t = 6;
                break;
        }

        mplew.writePos(new Point(280 * a, -260)); // ? 280 * (Randomizer.rand(-3, 3)

        int delay = (t + 1) * 250;

        mplew.writeInt(250 * t); // 딜레이
        mplew.writeInt(0);
        mplew.writeInt(0);

        int[][][] rectXs = {{{-75, 50}, {13, -50}, {83, 72}, {83, 72}},
        {{-81, 90}, {-59, -20}, {-25, 13}, {123, 31}, {138, -54}},
        {{-78, 28}, {-13, -50}, {42, 81}, {75, -18}, {133, 4}},
        {{-75, 50}, {13, -60}, {83, 72}, {83, 72}},
        {{-81, 90}, {-59, -20}, {-25, 13}, {123, 31}, {138, -54}},
        {{-78, 28}, {-13, -50}, {42, 81}, {75, -18}, {133, 4}},
        {{-78, 28}, {-13, -50}, {42, 81}, {75, -18}, {133, 4}},};
        int[][] rectX = rectXs[t];
        List<Rectangle> rectz = new ArrayList<>();

        for (int j = 0; j < rectX.length; ++j) {
            rectz.add(new Rectangle(rectX[j][0], -80, rectX[j][1], 640));
        }

        mplew.writeInt(rectz.size());

        for (Rectangle recta : rectz) {
            mplew.writeRect(recta);
        }

        return mplew.getPacket();
    }

    public static byte[] forcedSkillAction(int objectId, int value, boolean unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FORCE_ACTION.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(value);
        mplew.write(unk);
        return mplew.getPacket();
    }

    public static byte[] useFieldSkill(FieldSkill fieldSkill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FIELD_SKILL.getValue());
        mplew.writeInt(fieldSkill.getSkillId());
        mplew.writeInt(fieldSkill.getSkillLevel());

        switch (fieldSkill.getSkillId()) {
            case 100008: {
                mplew.writeInt(fieldSkill.getSummonedSequenceInfoList().size());
                for (FieldSkill.SummonedSequenceInfo info : fieldSkill.getSummonedSequenceInfoList()) {
                    mplew.writeInt(info.getPosition().x);
                    mplew.writeInt(info.getPosition().y);
                }
                break;
            }
            case 100011: {
                mplew.writeInt(fieldSkill.getLaserInfoList().size());
                for (FieldSkill.LaserInfo info : fieldSkill.getLaserInfoList()) {
                    mplew.writeInt(info.getPosition().x);
                    mplew.writeInt(info.getPosition().y);
                    mplew.writeInt(info.getUnk1());
                    mplew.writeInt(info.getUnk2());
                }
                break;
            }
            case 100013: {
                mplew.writeInt(fieldSkill.getEnvInfo().size());
                for (MapleNodes.Environment env : fieldSkill.getEnvInfo()) {
                    mplew.writeInt(env.getX());
                    mplew.writeInt(env.getY());
                    mplew.writeMapleAsciiString(env.getName());
                    mplew.writeInt(env.isShow() ? 1 : 0); // visible
                }
                break;
            }
            case 100014:
            case 100016: {
                mplew.writeInt(fieldSkill.getThunderInfo().size());
                mplew.writeInt(fieldSkill.getSkillId() == 100016 ? 1400 : 2700);
                mplew.write(1);
                for (FieldSkill.ThunderInfo th : fieldSkill.getThunderInfo()) {
                    mplew.writePosInt(th.getStartPosition());
                    mplew.writePosInt(th.getEndPosition());
                    mplew.writeInt(th.getInfo());
                    mplew.writeInt(th.getDelay());
                }
            }
            case 100020: {
                mplew.writeInt(0);
                mplew.writeInt(0); // nDuration

                for (FieldSkill.FieldFootHold fh : fieldSkill.getFootHolds()) {
                    mplew.write(true);

                    mplew.writeInt(fh.getDuration()); // duration
                    mplew.writeInt(fh.getInterval()); // interval
                    mplew.writeInt(fh.getAngleMin()); // angleMin
                    mplew.writeInt(fh.getAngleMax()); // angleMax
                    mplew.writeInt(fh.getAttackDelay()); // attackDelay
                    mplew.writeInt(fh.getZ() + fh.getSet()); // z + set
                    mplew.writeInt(fh.getZ()); // z

                    mplew.writeMapleAsciiString("");

                    mplew.writeMapleAsciiString(""); // unk

                    //Rect
                    mplew.writeRect(fh.getRect());

                    mplew.writeInt(fh.getPos().x); // pos.x
                    mplew.writeInt(fh.getPos().y); // pos.y
                    mplew.write(fh.isFacingLeft()); // isFacingLeft
                }

                mplew.write(false);
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] blackMageTamporarySkill(int type) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BLACKMAGE_TEMPORARY_SKILL.getValue());
        packet.writeInt(type);
        packet.writeInt(39);
        if (type == 8) {
            packet.writeInt(1);
            packet.writeInt(80002623);
            packet.writeInt(3);
            packet.writeInt(1);
            packet.writeInt(0);
        }
        return packet.getPacket();
    }

    public static byte[] useFieldSkill(int skillId, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FIELD_SKILL.getValue());
        mplew.writeInt(skillId);
        mplew.writeInt(skillLevel);

        if (skillId == 100008) {
            Rectangle rect = new Rectangle(-1998, 84, 4000, 84);
            mplew.writeInt(2);
            mplew.writeRect(rect);
        } else if (skillId == 100023) {
            int result = 2;
            mplew.writeInt(8880608);
            mplew.writeInt(1);
            mplew.writeInt(0);
            mplew.writeInt(100);
            mplew.writeInt(80);
            mplew.writeInt(240);
            mplew.writeInt(1530);
            mplew.writeInt(250);
            mplew.writeInt(result);
            for (int i = 0; i < result; i++) {
                if (i == 0) {
                    mplew.writeInt(Randomizer.rand(-864, 10));
                    mplew.writeInt(Randomizer.rand(30, 915));
                    mplew.writeInt(Randomizer.rand(810, 3420));
                } else {
                    mplew.writeInt(Randomizer.rand(300, 915));
                    mplew.writeInt(Randomizer.rand(-864, 10));
                    mplew.writeInt(Randomizer.rand(810, 3420));
                }
            }
        } else if (skillId == 100024) {
            mplew.writeInt(7);
            mplew.writeInt(Randomizer.rand(0, 6));
            mplew.writeInt(3060);
            mplew.writeInt(2700);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] fieldSkillRemove(int skillId, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FIELD_SKILL_REMOVE.getValue());
        mplew.writeInt(skillId);
        mplew.writeInt(skillLevel);

        return mplew.getPacket();
    }

    public static byte[] mobBarrier(int objectId, int percent) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_BARRIER.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(percent);
        mplew.writeLong(0); //?
        return mplew.getPacket();
    }

    public static byte[] mobBarrier(MapleMonster monster) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_BARRIER.getValue());
        mplew.writeInt(monster.getObjectId());
        mplew.writeInt(monster.getShieldPercent());
        mplew.writeLong(monster.getStats().getHp());
        return mplew.getPacket();
    }

    public static byte[] mobBarrierEffect(int objectId, String eff, String sound, String ui) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_BARRIER_EFFECT.getValue());
        mplew.writeInt(objectId);
        mplew.write(true);
        mplew.writeMapleAsciiString(eff);

        mplew.writeInt(1);
        mplew.write(true);
        mplew.writeMapleAsciiString(sound);

        mplew.write(true);
        mplew.writeMapleAsciiString(ui);

        mplew.writeInt(-1);
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] monsterResist(MapleMonster monster, MapleCharacter player, int time, int skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_RESIST.getValue());
        mplew.writeInt(monster.getObjectId());
        mplew.writeInt(2); // type
        mplew.writeInt(skill);
        mplew.writeShort(time);//time);
        mplew.writeInt(player.getId());
        mplew.write(false);
        mplew.writeInt(0); // 329++

        return mplew.getPacket();
    }

    public static class BossDusk {

        public static byte[] handleDuskGauge(boolean decrease, int gauge, int full) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.DUSK_GAUGE.getValue());
            mplew.write(decrease);
            mplew.writeInt(gauge);
            mplew.writeInt(full);
            return mplew.getPacket();
        }

        public static byte[] spawnDrillAttack(int x, boolean left, int level) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FIELD_SKILL.getValue());
            mplew.writeInt(100020);
            mplew.writeInt(level); //2 normal ,4 hard
            mplew.writeInt(0);

            mplew.writeInt(1501); // duration
            mplew.write(true);
            mplew.writeInt(1501); // duration
            mplew.writeInt(1);
            mplew.writeInt(0x23);
            mplew.writeInt(0x4B);
            mplew.writeInt(1020);
            mplew.writeInt(6);
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeInt(-2185);
            mplew.writeInt(300);
            mplew.writeInt(-120);
            mplew.writeInt(120);
            mplew.writeInt(x);
            mplew.writeInt(-157); //y;
            mplew.write(left);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] spawnTempFoothold(int level) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FIELD_SKILL.getValue());
            mplew.writeInt(100020);
            mplew.writeInt(level);
            mplew.writeInt(3);
            mplew.writeInt(23160); // duration;
            mplew.write(true);
            mplew.writeInt(20400); // duration;
            mplew.writeInt(2760); // delay (duration1 - duration2);
            mplew.writeInt(0x4B); // angle
            mplew.writeInt(0x4B); // angle
            mplew.writeInt(1980);
            mplew.writeInt(5);
            mplew.writeInt(3);
            mplew.writeShort(2);

            mplew.writeInt(12642);
            mplew.writeInt(-2185);
            mplew.writeInt(300);
            mplew.writeInt(-100);
            mplew.writeInt(100);
            mplew.writeInt(-610);
            mplew.writeInt(-159);
            mplew.write(0);
            mplew.write(1);

            mplew.writeInt(21900);
            mplew.writeInt(660);
            mplew.writeInt(-81);
            mplew.writeInt(-81);
            mplew.writeInt(1980);
            mplew.writeInt(6);
            mplew.writeInt(3);
            mplew.write(2);
            mplew.write(0);

            mplew.writeInt(12898);
            mplew.writeInt(-2185);
            mplew.writeInt(300);
            mplew.writeInt(-100);
            mplew.writeInt(100);
            mplew.writeInt(600);
            mplew.writeInt(-159);
            mplew.write(0);
            mplew.write(0);
            return mplew.getPacket();
        }
    }

    public static class BossBlodyQueen {

        public static byte[] QueenBreathAttack(int unk1, byte unk2) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.QueenBreathAttack.getValue());
            mplew.writeInt(unk1);
            mplew.write(unk2);
            mplew.write(1);
            return mplew.getPacket();
        }

        public static byte[] killMonster(final int oid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
            mplew.writeInt(oid);
            mplew.write(2);
            return mplew.getPacket();
        }
    }

    public static class BossDunKel {

        public static byte[] eliteBossAttack(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            /*   mplew.writeShort(SendPacketOpcode.WILL_SPIDER_ATTACK.getValue());
            mplew.write(1);
            mplew.writeShort(1);
            mplew.writeShort(2);
            mplew.writeInt(3000); //??
            mplew.writeInt(1620); //구체기준 구체 날라가는 delay
            mplew.writeInt(1);
            mplew.writeInt(4);
            mplew.writeInt(77); //듄켈 오브젝트 아디
            mplew.writeInt(chr.getId()); // 캐릭아디
            mplew.writeInt(1); // fancingleft
            mplew.writeInt(300);//?
            mplew.writeInt(0);
            mplew.writeInt(1200);//?
            mplew.writeShort(65);
            mplew.writeShort(100); //구체기준 날아가는 속도
            mplew.writeShort(0);
            mplew.write(0); //?
            mplew.write(1); //?
            mplew.writePosInt(new Point(-100,-75) );   //마법구체는 new Point(-100,-75) 고정
            mplew.writeLong(0);
            mplew.writeLong(0);

            mplew.writeShort(0);
            //for pos

            mplew.writeShort(2);  //구체날라가는거?
            mplew.write(HexTool.getByteArrayFromHexString("49 FD FF FF FB FF FF FF 00 00 00 00 E4 02 00 00 FB FF FF FF 01 00 00 00"));
            //for pos +4

            mplew.write(0);*/
 /* mplew.writeShort(1860);
            mplew.writeInt(1);
            mplew.writeMapleAsciiString("코데나");
            mplew.writeInt(100);
            mplew.writeInt(0);
            mplew.writeInt(100);*/
 /* mplew.writeShort(838);
            mplew.writeInt(1);
            mplew.writeMapleAsciiString("코데나");*/
 /* mplew.writeShort(365);
            mplew.writeInt(1);*/
            mplew.writeShort(SendPacketOpcode.STIGMA_INCINERATE_OBJECT.getValue());
            mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 11 02 00 00 10 00 00 00 C4 09 00 00 01 00 00 00 23 00 4D 61 70 2F 4F 62 6A 2F 42 6F 73 73 44 65 6D 69 61 6E 2E 69 6D 67 2F 64 65 6D 69 61 6E 2F 61 6C 74 61 72 00"));
            return mplew.getPacket();
        }

    }


    public static class BossSeren {

        public static byte[] SerenChangeBackground(int code) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.CHANGE_SEREN_MAP_TYPE.getValue());
            mplew.writeInt(code);
            return mplew.getPacket();
        }

        public static byte[] SerenUserStunGauge(int max, int now) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.APPLY_SEREN_GAUGE.getValue());
            mplew.writeInt(max);
            mplew.writeInt(now);
            return mplew.getPacket();
        }

        public static byte[] SerenMobLazer(MapleMonster mob, int skilllv, int delaytime) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SPAWN_SEREN_LASER.getValue());
            mplew.writeInt(mob.getObjectId());
            mplew.writeInt(skilllv);
            mplew.writeInt(delaytime);

            List<Point> pos = new ArrayList<>();

            if (skilllv == 1) {
                int rand = Randomizer.rand(2, 6);
                if (rand == 0) {
                    pos.add(new Point(-2511, -780));
                    pos.add(new Point(-1300, -2359));
                    pos.add(new Point(673, -2618));
                    pos.add(new Point(2252, -1407));
                    pos.add(new Point(2511, 566));
                    pos.add(new Point(1300, 2145));
                    pos.add(new Point(-673, 2404));
                    pos.add(new Point(-2252, 1193));
                } else if (rand == 1) {
                    pos.add(new Point(-2252, -1407));
                    pos.add(new Point(-673, -2618));
                    pos.add(new Point(1300, -2359));
                    pos.add(new Point(2511, -780));
                    pos.add(new Point(2252, 1193));
                    pos.add(new Point(673, 2404));
                    pos.add(new Point(-1300, 2145));
                    pos.add(new Point(-2511, 566));
                } else if (rand == 2) {
                    pos.add(new Point(-2600, -107));
                    pos.add(new Point(-1838, -1945));
                    pos.add(new Point(0, -2707));
                    pos.add(new Point(1838, -1945));
                    pos.add(new Point(2600, -107));
                    pos.add(new Point(1838, 1731));
                    pos.add(new Point(0, 2493));
                    pos.add(new Point(-1838, 1731));
                } else if (rand == 3) {
                    pos.add(new Point(-2561, -558));
                    pos.add(new Point(-1491, -2237));
                    pos.add(new Point(451, -2668));
                    pos.add(new Point(2130, -1598));
                    pos.add(new Point(2561, 344));
                    pos.add(new Point(1491, 2023));
                    pos.add(new Point(-451, 2454));
                    pos.add(new Point(-2130, 1384));
                } else if (rand == 4) {
                    pos.add(new Point(-2130, -1598));
                    pos.add(new Point(-451, -2668));
                    pos.add(new Point(1491, -2237));
                    pos.add(new Point(2561, -558));
                    pos.add(new Point(2130, 1384));
                    pos.add(new Point(451, 2454));
                    pos.add(new Point(-1491, 2023));
                    pos.add(new Point(-2561, 344));
                } else if (rand == 5) {
                    pos.add(new Point(-1992, -1778));
                    pos.add(new Point(-227, -2697));
                    pos.add(new Point(1671, -2099));
                    pos.add(new Point(2590, -334));
                    pos.add(new Point(1992, 1564));
                    pos.add(new Point(227, 2483));
                    pos.add(new Point(-1671, 1885));
                    pos.add(new Point(-2590, 120));
                } else if (rand == 6) {
                    pos.add(new Point(-2443, -996));
                    pos.add(new Point(-1099, -2463));
                    pos.add(new Point(889, -2550));
                    pos.add(new Point(2356, -1206));
                    pos.add(new Point(2443, 782));
                    pos.add(new Point(1099, 2249));
                    pos.add(new Point(-889, 2336));
                    pos.add(new Point(-2356, 992));
                }
            } else {
                pos.add(new Point(1300, 0));
                pos.add(new Point(1800, -651));
                pos.add(new Point(1300, -1400));
                pos.add(new Point(750, -3500));
                pos.add(new Point(-750, -3500));
                pos.add(new Point(-1900, -1700));
                pos.add(new Point(-1900, -500));
                pos.add(new Point(-800, 200));
                pos.add(new Point(-800, 900));
                pos.add(new Point(2200, 1600));
                pos.add(new Point(-10, 0));
                pos.add(new Point(40, 0));
            }

            mplew.writeInt(pos.size());
            for (int i = 0; i < pos.size(); i++) {
                mplew.writeInt(0);
                mplew.writeInt(-107);
                mplew.writePosInt(pos.get(i));
            }
            return mplew.getPacket();
        }

        public static byte[] SerenTimer(int value, int... info) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.HANDLE_SEREN_CLOCK.getValue());
            mplew.writeInt(value);
            switch (value) {
                case 0:
                    mplew.writeInt(info[0]);
                    mplew.writeInt(info[1]);
                    mplew.writeInt(info[2]);
                    mplew.writeInt(info[3]);
                    mplew.writeInt(info[4]);
                    break;
                case 1:
                    mplew.writeInt(info[0]);
                    mplew.writeInt(info[1]);
                    mplew.writeInt(info[2]);
                    mplew.writeInt(info[3]);
                    mplew.writeInt(info[4]);
                    break;
                case 2:
                    mplew.write(info[0]);
                    break;
                case 3:
                    mplew.writeInt(0);
                    break;
            }
            return mplew.getPacket();
        }

        public static byte[] SerenSpawnOtherMist(int oid, boolean left, Point pos) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.APPLY_SEREN_SPECIAL_ATTACK.getValue());
            mplew.write(0);
            mplew.writeInt(263);
            mplew.writeInt(1);
            mplew.writeInt(oid);
            mplew.write(left);
            mplew.writePosInt(pos);
            return mplew.getPacket();
        }

        public static byte[] SerenUnk(int max, int now) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SEREN_DELAY.getValue());
            mplew.writeInt(max);
            mplew.writeInt(now);
            return mplew.getPacket();
        }

        public static byte[] SerenChangePhase(String str, int type, MapleMonster mob) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SEREN_CHANGE_PHASE.getValue());
            mplew.writeInt(mob.getObjectId());
            mplew.writeMapleAsciiString(str);
            mplew.writeInt(type);
            mplew.writeInt(0);
            mplew.writeInt(1);
            mplew.writeInt(mob.getId());
            return mplew.getPacket();
        }

    }
}
