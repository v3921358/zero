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
package client.status;

import constants.GameConstants;
import handling.Buffstat;

import java.io.Serializable;

public enum MonsterStatus implements Serializable, Buffstat {
    MS_IgnoreDefence(0), //?..??
    MS_1(1), //?..??
    MS_Pad(4),
    MS_Pdr(5),
    MS_Mad(6),
    MS_Mdr(7),
    MS_Acc(8),
    MS_Eva(9),
    MS_Speed(10),
    MS_Stun(11),
    MS_Freeze(12),
    MS_Poison(13),
    MS_Seal(14),
    MS_Darkness(15),
    MS_Powerup(16),
    MS_Magicup(17),
    MS_PGuardup(18),
    MS_MGuardup(19),
    MS_PImmune(20),
    MS_MImmune(21),
    MS_Web(22),
    MS_Hardskin(23),
    MS_Ambush(24),
    MS_Venom(25),
    MS_Blind(26),
    MS_SealSkill(27),
    MS_Dazil(28),
    MS_PCounter(29),
    MS_MCounter(30),
    MS_RiseByToss(31),
    MS_BodyPressure(32),
    MS_Weakness(33),
    MS_Showdown(34), //잘됨!
    MS_MagicCrash(35),
    //336
    MS_AdddamParty(37),
    MS_HitCritDamR(38),
    MS_Fatality(39),
    MS_Lifting(40),
    MS_DeadlyCharge(42),
    MS_Smite(43),
    MS_AdddamSkill(44),//잘되는듯?
    MS_Incizing(45), // 1.2.351 IDA
    MS_DodgeBodyAttack(46),
    MS_DebuffHealing(47),
    MS_AdddamSkill2(48),
    MS_BodyAttack(49),
    MS_TempMoveAbility(50),
    //336
    MS_SpiritGate(51),
    MS_FixdamRBuff(52),
    MS_ElementDarkness(53),
    MS_AreaInstallByHit(54),
    MS_BMageDebuff(55), // 1.2.351 IDA
    MS_JaguarProvoke(56),
    MS_JaguarBleeding(57),
    MS_DarkLightning(59), // 1.2.351 IDA
    MS_PinkbeanFlowerPot(59),
//    MS_PvPHelenaMark(60),
    MS_PsychicLock(61),
    MS_PsychicLockCooltime(62),
    MS_PsychicGroundMark(63),
    MS_PowerImmune(64),
    MS_PsychicForce(65),
    MS_MultiPMDR(65), // 1.2.351 IDA
    MS_UnkFlameWizard(66), // 1.2.351 IDA
    MS_ElementResetBySummon(67), // 1.2.351 IDA
    MS_CurseMark(69),
    MS_DragonStrike(71),
    MS_MinusDef(74),
    MS_PopulatusTimer(76),
    MS_BahamutLightElemAddDam(77), // 0x800000
    MS_BossPropPlus(78), //?

    MS_FinalDam(82), // ? 은월 최종뎀 15%증가 버프 아무도 안해놧는데?
    MS_82(82),
    MS_83(83),
    MS_84(84),
    MS_Calabash(85),//343
    MS_Gathering(86), //343
    MS_87(87),
    MS_MultiDamSkill(89),//?  위치모름
    MS_RWLiftPress(90),//?
    MS_RWChoppingHammer(91),//?
    MS_TimeBomb(92),
    MS_Treasure(93),
    MS_AddEffect(94), //0x10000, 1.2.295 기준 무릉 버프  데미지 1/10 인듯?
    MS_Invincible(95), //여기부턴 다시맞음 343
    MS_Explosion(97),
    MS_HangOver(98),
    MS_Burned(99),
    MS_BalogDisable(100),
    MS_ExchangeAttack(101),
    MS_AddBuffStat(102),
    MS_LinkTeam(103),
    MS_SoulExplosion(104),
    MS_SeperateSoulP(105),
    MS_SeperateSoulC(106),
    MS_Ember(107),
    MS_TrueSight(108), //0x8, 1.2.307
    MS_Laser(109),
    MS_StatResetSkill(110),
    MS_111(111),
    MS_112(112),
    MS_113(113),
    MS_114(114),
    MS_115(115),
    MS_116(116),
    MS_117(117),
    MS_118(118),
    MS_119(119),
    MS_120(120),
    MS_121(121),
    MS_122(122),
    MS_123(123),
    MS_124(124),
    MS_125(125),
    MS_126(126),
    MS_127(127),;
    // 307 ++

    static final long serialVersionUID = 0L;
    private final int i;
    private final int first;
    private final int flag;
    private final boolean end;

    private MonsterStatus(int flag) {
        this.i = (1 << (31 - (flag % 32)));
        this.first = (GameConstants.MAX_MOB_BUFFSTAT - (byte) Math.floor(flag / 32));
        this.flag = flag;
        this.end = false;
    }

    public int getPosition() {
        return first;
    }

    public boolean isEmpty() {
        return end;
    }

    public int getValue() {
        return i;
    }

    public int getFlag() {
        return flag;
    }

    public static boolean IsMovementAffectingStat(final MonsterStatus skill) {
        switch (skill) {
            case MS_Stun:
            case MS_Speed:
            case MS_Freeze:
            case MS_RiseByToss:
            case MS_Lifting:
            case MS_Smite:
            case MS_TempMoveAbility:
            case MS_StatResetSkill:
            case MS_RWLiftPress:
            case MS_AdddamSkill2:
            case MS_PCounter:
            case MS_MCounter:
                return true;
        }
        return false;
    }
}
