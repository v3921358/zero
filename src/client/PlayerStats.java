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
package client;

import client.MapleTrait.MapleTraitType;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.GameConstants;
import handling.channel.handler.UnionHandler;
import server.*;
import server.StructSetItem.SetItem;
import server.life.Element;
import tools.Pair;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InventoryPacket;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStats implements Serializable {

    private static final long serialVersionUID = -679541993413738569L;
    private final Map<Integer, Integer> setHandling = new ConcurrentHashMap<>(), skillsIncrement = new HashMap<>(), damageIncrease = new HashMap<>();
    private EnumMap<Element, Integer> elemBoosts = new EnumMap<>(Element.class);
    private transient WeakReference<MapleCharacter> chr;
    private transient Map<Integer, Integer> demonForce = new HashMap<Integer, Integer>();
    private List<Equip> durabilityHandling = new ArrayList<>(), equipLevelHandling = new ArrayList<>();
    private List<Triple<Integer, String, Integer>> psdSkills = new ArrayList<>();
    private transient float shouldHealHP, shouldHealMP;
    public short str, dex, luk, int_;
    public long hp = 50, maxhp = 50, mp = 5, maxmp = 5;
    public transient short critical_rate, critical_damage, damAbsorbShieldR;
    private transient byte passive_mastery;
    private transient int localstr, localdex, localluk, localint_, ms_maxhp, ms_maxmp;
    private transient long localmaxhp, localmaxmp;
    private transient int magic, watk, hands, accuracy;
    public transient boolean equippedWelcomeBackRing, hasClone, hasPartyBonus, Berserk;
    public transient double expBuff, expBuffZero, expBuffUnion, dropBuff, mesoBuff, cashBuff, MesoGuard, MesoGuardMeso, expMod, pickupRange;
    public transient double dam_r, bossdam_r;
    public transient int recoverHP, recoverMP, mpconReduce, mpconPercent, incMesoProp, coolTimeR, suddenDeathR, expLossReduceR, DAMreflect, DAMreflect_rate, ignoreDAMr, ignoreDAMr_rate, ignoreDAM, ignoreDAM_rate, mpRestore,
            hpRecover, hpRecoverProp, hpRecoverPercent, mpRecover, mpRecoverProp, RecoveryUP, BuffUP, RecoveryUP_Skill, BuffUP_Skill,
            incAllskill, combatOrders, ignoreTargetDEF, defRange, BuffUP_Summon, dodgeChance, speed, jump, harvestingTool, evaR,
            equipmentBonusExp, dropMod, cashMod, levelBonus, ASR, TER, pickRate, decreaseDebuff, equippedFairy, equippedSummon,
            percent_hp, percent_mp, multi_lateral_hp, multi_lateral_mp, percent_str, percent_dex, percent_int, percent_luk, percent_acc, percent_atk, percent_matk, percent_wdef, percent_mdef,
            pvpDamage, hpRecoverTime = 0, mpRecoverTime = 0, dot, dotTime, questBonus, pvpRank, pvpExp, wdef, mdef, trueMastery, damX, DAMreduceR, randCooldown;
    public transient long fixHp;
    private transient float localmaxbasedamage, localmaxbasepvpdamage, localmaxbasepvpdamageL;
    public transient int def, element_ice, element_fire, element_light, element_psn, reduceCooltime, reduceCooltimeS;
    private double sword, blunt, axe, spear, polearm, claw, dagger, staffwand, CROSSBOW, bow;
    private int skill = 0;
    private Skill skil;

    // TODO: all psd skills (Passive)
    public final void init(MapleCharacter chra) {
        recalcLocalStats(chra);
    }

    public final short getStr() {
        return str;
    }

    public final short getDex() {
        return dex;
    }

    public final short getLuk() {
        return luk;
    }

    public final short getInt() {
        return int_;
    }

    public final void setStr(final short str, MapleCharacter chra) {
        this.str = str;
        recalcLocalStats(chra);
    }

    public final void setDex(final short dex, MapleCharacter chra) {
        this.dex = dex;
        recalcLocalStats(chra);
    }

    public final void setLuk(final short luk, MapleCharacter chra) {
        this.luk = luk;
        recalcLocalStats(chra);
    }

    public final void setInt(final short int_, MapleCharacter chra) {
        this.int_ = int_;
        recalcLocalStats(chra);
    }

    public final boolean setHp(final long newhp, MapleCharacter chra) {
        return setHp(newhp, false, chra);
    }

    public final boolean setHp(long newhp, boolean silent, final MapleCharacter chra) {
        final long oldHp = hp;
        long thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > this.hp) {
            if (this.hp > 0 && chra.getBuffedEffect(MapleBuffStat.DebuffIncHp) != null) {
                return false;
            }
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;

        if (chra != null) {
            if (!silent) {
                chra.updatePartyMemberHP();
            }
            if (oldHp > hp && !chra.isAlive()) {
                if (!chra.skillisCooling(1320016) && !chra.skillisCooling(1320019) && chra.getSkillLevel(1320016) > 0) {
                    MapleStatEffect rein = SkillFactory.getSkill(1320019).getEffect(chra.getSkillLevel(1320016));
                    rein.applyTo(chra, true);

                    chra.addCooldown(1320019, System.currentTimeMillis(), rein.getCooldown(chra));
                    chra.getClient().getSession().writeAndFlush(CField.skillCooldown(1320019, rein.getCooldown(chra)));
                    this.hp = localmaxhp;
                } else if (chra.getBuffedEffect(MapleBuffStat.HeavensDoor) != null) {
                    chra.cancelEffect(chra.getBuffedEffect(MapleBuffStat.HeavensDoor), false, -1);
                    this.hp = localmaxhp;
                } else if (chra.getBuffedEffect(MapleBuffStat.FlareTrick) != null) { // 본 피닉스
                    chra.getBuffedEffect(MapleBuffStat.FlareTrick).applyTo(chra, false); // 무적 발동

                    chra.addCooldown(chra.getBuffSource(MapleBuffStat.FlareTrick), System.currentTimeMillis(), chra.getBuffedEffect(MapleBuffStat.FlareTrick).getCooldown(chra));
                    chra.getClient().getSession().writeAndFlush(CField.skillCooldown(chra.getBuffSource(MapleBuffStat.FlareTrick), chra.getBuffedEffect(MapleBuffStat.FlareTrick).getCooldown(chra)));

                    chra.cancelEffect(chra.getBuffedEffect(MapleBuffStat.FlareTrick), false, -1);
                    this.hp = localmaxhp;
                } else if (chra.getBuffedEffect(MapleBuffStat.ReviveOnce) != null) { // 럭 오브 팬텀시프, 환령강신, 타임 리와인드
                    if (chra.getBuffedEffect(MapleBuffStat.ReviveOnce).getSourceId() == 24111002) {
                        chra.getClient().getSession().writeAndFlush(EffectPacket.showEffect(chra, chra.getBuffSource(MapleBuffStat.ReviveOnce), chra.getBuffSource(MapleBuffStat.ReviveOnce), 1, 0, 0, (byte) 0, true, null, null, null));
                        chra.getMap().broadcastMessage(chra, EffectPacket.showEffect(chra, chra.getBuffSource(MapleBuffStat.ReviveOnce), chra.getBuffSource(MapleBuffStat.ReviveOnce), 1, 0, 0, (byte) 0, false, null, null, null), false);
                    }

                    chra.getBuffedEffect(MapleBuffStat.ReviveOnce).applyTo(chra, false); // 무적 발동

                    chra.addCooldown(chra.getBuffSource(MapleBuffStat.ReviveOnce), System.currentTimeMillis(), chra.getBuffedEffect(MapleBuffStat.ReviveOnce).getCooldown(chra));
                    chra.getClient().getSession().writeAndFlush(CField.skillCooldown(chra.getBuffSource(MapleBuffStat.ReviveOnce), chra.getBuffedEffect(MapleBuffStat.ReviveOnce).getCooldown(chra)));

                    chra.cancelEffect(chra.getBuffedEffect(MapleBuffStat.ReviveOnce), false, -1);
                    this.hp = localmaxhp;
                } else if (chra.getBuffedEffect(MapleBuffStat.PreReviveOnce) != null) { // 구사일생
                    if (chra.getBuffedEffect(MapleBuffStat.PreReviveOnce).makeChanceResult()) {
                        chra.cancelEffect(chra.getBuffedEffect(MapleBuffStat.PreReviveOnce), false, -1);
                        this.hp = localmaxhp;
                    } else {
                        chra.cancelEffect(chra.getBuffedEffect(MapleBuffStat.PreReviveOnce), false, -1);
                        chra.updateSingleStat(MapleStat.HP, this.hp);
                        chra.playerDead();
                    }
                } else {
                    chra.updateSingleStat(MapleStat.HP, this.hp);
                    chra.playerDead();
                }
            }
        }
        if (GameConstants.isDemonAvenger(chra.getJob())) {
            EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.LifeTidal, new Pair<>((int) 3, 0));
            chra.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chra));
        }
        return hp != oldHp;
    }

    public final boolean setMp(final long newmp, final MapleCharacter chra) {
        final long oldMp = mp;
        long tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
        return mp != oldMp;
    }

    public final void setInfo(final long maxhp, final long maxmp, final long hp, final long mp) {
        this.maxhp = maxhp;
        this.maxmp = maxmp;
        this.hp = hp;
        this.mp = mp;
    }

    public final void setMaxHp(final long hp, MapleCharacter chra) {
        this.maxhp = hp;
        recalcLocalStats(chra);
    }

    public final void setMaxMp(final long mp, MapleCharacter chra) {
        this.maxmp = mp;
        recalcLocalStats(chra);
    }

    public final long getHp() {
        return hp;
    }

    public final long getMaxHp() {
        return maxhp;
    }

    public final long getMp() {
        return mp;
    }

    public final long getMaxMp() {
        return maxmp;
    }

    public final int getTotalDex() {
        return localdex;
    }

    public final int getTotalInt() {
        return localint_;
    }

    public final int getTotalStr() {
        return localstr;
    }

    public final int getTotalLuk() {
        return localluk;
    }

    public final int getTotalMagic() {
        return magic;
    }

    public final int getSpeed() {
        return speed;
    }

    public final int getJump() {
        return jump;
    }

    public final int getTotalWatk() {
        return watk;
    }

    public final long getCurrentMaxHp() {
        return localmaxhp;
    }

    public final long getCurrentMaxMp(MapleCharacter chr) {
        if (GameConstants.isDemonSlayer(chr.getJob())) {
            return GameConstants.getMPByJob(chr);
        }
        return localmaxmp;
    }

    public final int getHands() {
        return hands;
    }

    public final float getCurrentMaxBaseDamage() {
        return localmaxbasedamage;
    }

    public final float getCurrentMaxBasePVPDamage() {
        return localmaxbasepvpdamage;
    }

    public final float getCurrentMaxBasePVPDamageL() {
        return localmaxbasepvpdamageL;
    }

    private void resetLocalStats(final int job) {
        accuracy = 0;
        wdef = 0;
        mdef = 0;
        damX = 0;
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        speed = 100;
        jump = 100;
        pickupRange = 0.0;
        decreaseDebuff = 0;
        ASR = 0;
        TER = 0;
        dot = 0;
        questBonus = 1;
        dotTime = 0;
        trueMastery = 0;
        percent_wdef = 0;
        percent_mdef = 0;
        percent_hp = 0;
        percent_mp = 0;
        multi_lateral_hp = 0;
        multi_lateral_mp = 0;
        percent_str = 0;
        percent_dex = 0;
        percent_int = 0;
        percent_luk = 0;
        percent_acc = 0;
        percent_atk = 0;
        percent_matk = 0;
        critical_rate = 5;
        critical_damage = 0;
        magic = 0;
        watk = 0;
        evaR = 0;
        pvpDamage = 0;
        MesoGuard = 50.0;
        MesoGuardMeso = 0.0;
        dam_r = 100.0;
        bossdam_r = 100.0;
        fixHp = 0;
        expBuff = 0;//100.0;
        expBuffZero = 0;
        expBuffUnion = 0;
        cashBuff = 100.0;
        dropBuff = 100.0;
        mesoBuff = 100.0;
        reduceCooltime = 0;
        randCooldown = 0;
        recoverHP = 0;
        recoverMP = 0;
        mpconReduce = 0;
        mpconPercent = 100;
        incMesoProp = 0;
        coolTimeR = 0;
        suddenDeathR = 0;
        expLossReduceR = 0;
        DAMreflect = 0;
        DAMreflect_rate = 0;
        ignoreDAMr = 0;
        ignoreDAMr_rate = 0;
        ignoreDAM = 0;
        ignoreDAM_rate = 0;
        ignoreTargetDEF = 0;
        hpRecover = 0;
        hpRecoverProp = 0;
        hpRecoverPercent = 0;
        mpRecover = 0;
        mpRecoverProp = 0;
        pickRate = 0;
        equippedWelcomeBackRing = false;
        equippedFairy = 0;
        equippedSummon = 0;
        hasClone = false;
        Berserk = false;
        equipmentBonusExp = 0;
        RecoveryUP = 0;
        BuffUP = 0;
        RecoveryUP_Skill = 0;
        BuffUP_Skill = 0;
        BuffUP_Summon = 0;
        dropMod = 1;
        expMod = 1.0;
        cashMod = 1;
        levelBonus = 0;
        incAllskill = 0;
        combatOrders = 0;
        defRange = isRangedJob(job) ? 200 : 0;
        durabilityHandling.clear();
        equipLevelHandling.clear();
        skillsIncrement.clear();
        damageIncrease.clear();

        setHandling.clear();
        harvestingTool = 0;
        element_fire = 100;
        element_ice = 100;
        element_light = 100;
        element_psn = 100;
        def = 100;
        damAbsorbShieldR = 0;
    }

    public void recalcLocalStats(MapleCharacter chra) {
        recalcLocalStats(false, chra);
    }

    public void recalcLocalStats(boolean first_login, MapleCharacter chra) {
        /*
         * 307 기준으로 다시 작업중.
         * 완료된 목록
         * localmaxhp
         * localmaxmp
         * critical_rate
         * mesobuff
         * dropbuff
         * BuffTime
         * IgnoreTargetDEF 하는중 : ignoreTargetDEF += ignoreTargetDEF * (double) (1 - (ignoreTargetDEF / 100.0));
         */

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        long oldmaxhp = localmaxhp;
        long localmaxhp_ = getMaxHp();
        long localmaxmp_ = getMaxMp();

        resetLocalStats(chra.getJob());

        localmaxhp_ += Math.floor((ms_maxhp * localmaxhp_) / 100.0f);
        localmaxmp_ += Math.floor((ms_maxmp * localmaxmp_) / 100.0f);

        for (MapleTraitType t : MapleTraitType.values()) {
            chra.getTrait(t).clearLocalExp();
        }

        int jokerItemId = 0;

        final Map<Skill, SkillEntry> sData = new HashMap<>();

        synchronized (chra.getInventory(MapleInventoryType.EQUIPPED)) {

            final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIPPED).newList().iterator();
            while (itera.hasNext()) {
                final Equip equip = (Equip) itera.next();
                if (equip.getPosition() == -11) {
                    if (GameConstants.isMagicWeapon(equip.getItemId())) {
                        final Map<String, Integer> eqstat = MapleItemInformationProvider.getInstance().getEquipStats(equip.getItemId());
                        if (eqstat != null) { //slow, poison, darkness, seal, freeze
                            if (eqstat.containsKey("incRMAF")) {
                                element_fire = eqstat.get("incRMAF");
                            }
                            if (eqstat.containsKey("incRMAI")) {
                                element_ice = eqstat.get("incRMAI");
                            }
                            if (eqstat.containsKey("incRMAL")) {
                                element_light = eqstat.get("incRMAL");
                            }
                            if (eqstat.containsKey("incRMAS")) {
                                element_psn = eqstat.get("incRMAS");
                            }
                            if (eqstat.containsKey("elemDefault")) {
                                def = eqstat.get("elemDefault");
                            }
                        }
                    }
                }
                if (equip.getItemId() / 1000 == 1672) { // 안드로이드 하트는, 안드로이드를 착용해야 효고가 적용됨
                    final Item android = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -27);
                    if (android == null) {
                        continue;
                    }
                }
                List<Integer> potentials = new ArrayList<>();
                potentials.add(equip.getPotential1());
                potentials.add(equip.getPotential2());
                potentials.add(equip.getPotential3());
                potentials.add(equip.getPotential4());
                potentials.add(equip.getPotential5());
                potentials.add(equip.getPotential6());
                for (Integer potential : potentials) {
                    int lv = (ii.getReqLevel(equip.getItemId()) / 10) - 1;
                    if (lv < 0) {
                        lv = 0;
                    }
                    if (potential == 0 || ii.getPotentialInfo(potential) == null || ii.getPotentialInfo(potential).get(lv) == null) {
                        continue;
                    }
                    localmaxhp_ += ii.getPotentialInfo(potential).get(lv).incMHP / (GameConstants.isDemonAvenger(chra.getJob()) ? 2 : 1);
                    localmaxmp_ += ii.getPotentialInfo(potential).get(lv).incMMP;
                    percent_hp += ii.getPotentialInfo(potential).get(lv).incMHPr / (GameConstants.isDemonAvenger(chra.getJob()) ? 2 : 1);
                    percent_mp += ii.getPotentialInfo(potential).get(lv).incMMPr;
                    mesoBuff += ii.getPotentialInfo(potential).get(lv).incMesoProp;
                    dropBuff += ii.getPotentialInfo(potential).get(lv).incRewardProp;
                    reduceCooltime += ii.getPotentialInfo(potential).get(lv).reduceCooltime;
                }

                chra.getTrait(MapleTraitType.craft).addLocalExp(equip.getHands());
                accuracy += equip.getAcc();
                localmaxhp_ += equip.getHp();
                localmaxmp_ += equip.getMp();
                localdex += equip.getDex();
                localint_ += equip.getInt();
                localstr += equip.getStr();
                localluk += equip.getLuk();
                watk += equip.getWatk();
                magic += equip.getMatk();
                wdef += equip.getWdef();
                mdef += equip.getMdef();
                speed += equip.getSpeed();
                jump += equip.getJump();
                pvpDamage += equip.getPVPDamage();
                switch (equip.getItemId()) {
                    case 1112127:
                        equippedWelcomeBackRing = true;
                        break;
                    case 1122017:
                        equippedFairy = 10;
                        break;
                    case 1122158:
                        equippedFairy = 5;
                        break;
                    case 1112594:
                        equippedSummon = 1090;
                        break;
                    case 1112585:
                        equippedSummon = 1085;
                        break;
                    case 1112586:
                        equippedSummon = 1087;
                        break;
                    case 1112663:
                        equippedSummon = 1179;
                        break;
                    case 1112735:
                        equippedSummon = 1179;
                        break;
                }
                final Integer set = ii.getSetItemID(equip.getItemId());
                if (set != null && set > 0) {
                    int value = 1;
                    if (setHandling.containsKey(set)) {
                        value += setHandling.get(set).intValue();
                    }

                    setHandling.put(set, value); //id of Set, number of items to go with the set
                }
                if (ii.isJokerToSetItem(equip.getItemId()) && jokerItemId > equip.getItemId()) {
                    jokerItemId = equip.getItemId();
                }
                if (equip.getIncSkill() > 0 && ii.getEquipSkills(equip.getItemId()) != null) {
                    for (final int zzz : ii.getEquipSkills(equip.getItemId())) {
                        final Skill skil = SkillFactory.getSkill(zzz);
                        if (skil != null && skil.canBeLearnedBy(chra)) { //dont go over masterlevel :D
                            int value = 1;
                            if (skillsIncrement.get(skil.getId()) != null) {
                                value += skillsIncrement.get(skil.getId());
                            }
                            skillsIncrement.put(skil.getId(), value);
                        }
                    }
                }
                Pair<Long, Long> add = handleEquipAdditions(ii, chra, first_login, sData, equip.getItemId());
                localmaxhp_ += add.left;
                localmaxmp_ += add.right;
                if (ii.getEquipStats(equip.getItemId()) != null) {
                    if (ii.getEquipStats(equip.getItemId()).get("MHPr") != null) {
                        percent_hp += ii.getEquipStats(equip.getItemId()).get("MHPr");
                    }
                }
                if (ii.getEquipStats(equip.getItemId()) != null) {
                    if (ii.getEquipStats(equip.getItemId()).get("MMPr") != null) {
                        percent_mp += ii.getEquipStats(equip.getItemId()).get("MMPr");
                    }
                }
                if (equip.getDurability() > 0) {
                    durabilityHandling.add((Equip) equip);
                }
                if (GameConstants.getMaxLevel(equip.getItemId()) > 0 && equip.getEquipLevel() <= GameConstants.getMaxLevel(equip.getItemId())) {
                    equipLevelHandling.add((Equip) equip);
                }
            }
        }

        final Iterator<Entry<Integer, Integer>> iter = setHandling.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<Integer, Integer> entry = iter.next();
            final StructSetItem set = ii.getSetItem(entry.getKey());
            if (set != null) {
                final Map<Integer, SetItem> itemz = set.getItems();
                if (set.jokerPossible && jokerItemId > 0 && entry.getValue() < set.completeCount) {
                    for (int itemId : set.itemIDs) {
                        if (GameConstants.isWeapon(itemId) && GameConstants.isWeapon(jokerItemId)) {
                            entry.setValue(entry.getValue() + 1);
                            break;
                        } else if (!GameConstants.isWeapon(itemId) && !GameConstants.isWeapon(jokerItemId) && itemId / 10000 == jokerItemId / 10000 && chra.getInventory(MapleInventoryType.EQUIPPED).findById(itemId) == null) {
                            entry.setValue(entry.getValue() + 1);
                            break;
                        }
                    }
                }
                for (Entry<Integer, SetItem> ent : itemz.entrySet()) {
                    if (ent.getKey() <= entry.getValue()) {
                        SetItem se = ent.getValue();
                        localstr += se.incSTR + se.incAllStat;
                        localdex += se.incDEX + se.incAllStat;
                        localint_ += se.incINT + se.incAllStat;
                        localluk += se.incLUK + se.incAllStat;
                        watk += se.incPAD;
                        magic += se.incMAD;
                        speed += se.incSpeed;
                        accuracy += se.incACC;
                        localmaxhp_ += se.incMHP;
                        localmaxmp_ += se.incMMP;
                        percent_hp += se.incMHPr;
                        percent_mp += se.incMMPr;
                        wdef += se.incPDD;
                        mdef += se.incMDD;
                    }
                }
            }
        }

        handleProfessionTool(chra);

        if (first_login && chra.getLevel() >= 30) {
            if (chra.isGM()) {
                for (int i = 0; i < allJobs.length; i++) {
                    sData.put(SkillFactory.getSkill(1085 + allJobs[i]), new SkillEntry((byte) 1, (byte) 0, -1));
                    sData.put(SkillFactory.getSkill(1087 + allJobs[i]), new SkillEntry((byte) 1, (byte) 0, -1));
                }
            } else {
                sData.put(SkillFactory.getSkill(getSkillByJob(1085, chra.getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
                sData.put(SkillFactory.getSkill(getSkillByJob(1087, chra.getJob())), new SkillEntry((byte) 1, (byte) 0, -1));
            }
        }
        
        if (chra.getKeyValue(19019, "id") >= 1) {
            int headId = (int) chra.getKeyValue(19019, "id");
            if (ii.getItemInformation(headId) != null) {
                int nickSkill = ii.getItemInformation(headId).nickSkill;
                if (SkillFactory.getSkill(nickSkill) != null) {
                    MapleStatEffect nickEffect = SkillFactory.getSkill(nickSkill).getEffect(1);
                    localmaxhp_ += nickEffect.getMhpX();
                    localmaxmp_ += nickEffect.getMhpX();
                }
            }
        }

        // add to localmaxhp_ if percentage plays a role in it, else add_hp
        Pair<Long, Long> buffstats = handleBuffStats(chra); //甕?????? ????揶?
        localmaxhp_ += buffstats.left;
        localmaxmp_ += buffstats.right;

        Integer buff = chra.getBuffedValue(MapleBuffStat.EnhancedMaxHp);
        if (buff != null) {
            localmaxhp_ += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.EnhancedMaxMp);
        if (buff != null) {
            localmaxmp_ += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieHp);
        if (buff != null) {
            localmaxhp_ += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMp);
        if (buff != null) {
            localmaxmp_ += buff.intValue();
        }

        for (InnerSkillValueHolder ISVH : chra.getInnerSkills()) {
            int x = ISVH.getSkillLevel();
            switch (ISVH.getSkillId()) {
                case 70000000:
                    localstr += MapleStatEffect.parseEval("x", x);
                    break; //strFX = x
                case 70000001:
                    localdex += MapleStatEffect.parseEval("x", x);
                    break;
                case 70000002:
                    localint_ += MapleStatEffect.parseEval("x", x);
                    break;
                case 70000003:
                    localluk += MapleStatEffect.parseEval("x", x);
                    break;
                case 70000004:
                    accuracy += MapleStatEffect.parseEval("(10 * x)", x);
                    break; //accX = 10 * x
                case 70000005:
                    evaR += MapleStatEffect.parseEval("(10 * x)", x);
                    break;
                case 70000006:
                    wdef += MapleStatEffect.parseEval("(10 * x)", x);
                    break;
                case 70000007:
                    mdef += MapleStatEffect.parseEval("(10 * x)", x);
                    break;
                case 70000008:
                    localmaxhp += MapleStatEffect.parseEval("(15 * x)", x);
                    break;//mhpX = x * 15
                case 70000009:
                    localmaxmp += MapleStatEffect.parseEval("(15 * x)", x);
                    break;
                case 70000010:
                    jump += MapleStatEffect.parseEval("2*u(x/3)", x);
                    break; //psdJump = 2 * u(x / 3)
                case 70000011:
                    speed += MapleStatEffect.parseEval("2*u(x/3)", x);
                    break;
                case 70000012:
                    watk += MapleStatEffect.parseEval("3*u(x/3)", x);
                    break; //padX = 3 * u(x / 3)
                case 70000013:
                    magic += MapleStatEffect.parseEval("3*u(x/3)", x);
                    break;
                case 70000014:
                    critical_rate += MapleStatEffect.parseEval("cr", x);
                    break;
                case 70000015: { //lukFX = x	strFX = x	dexFX = x	intFX = x
                    localstr += MapleStatEffect.parseEval("x", x);
                    localdex += MapleStatEffect.parseEval("x", x);
                    localint_ += MapleStatEffect.parseEval("x", x);
                    localluk += MapleStatEffect.parseEval("x", x);
                }
                break;
                case 70000016:
                    break; //actionSpeed ??= -1 //this is attack speed, isn't it?  client-sided
                case 70000017:
                    mdef += (wdef * MapleStatEffect.parseEval("u(x / 4)", x) / 100);
                    break;// pdd2mdd = u (x / 4)
                case 70000018:
                    wdef += (mdef * MapleStatEffect.parseEval("u(x / 4)", x) / 100);
                    break;
                case 70000019:
                    localmaxmp += (accuracy * MapleStatEffect.parseEval("(5 * u(x / 4))", x) / 100);
                    break;//acc2mp = 5 * u (x / 4)
                case 70000020:
                    localmaxhp += (evaR * MapleStatEffect.parseEval("(5 * u(x / 4))", x) / 100);
                    break;
                case 70000021:
                    localdex += (str * MapleStatEffect.parseEval("u(x / 4)", x) / 100);
                    break; //str2dex = u (x / 4) (str is base, localstr is bonus included)
                case 70000022:
                    localstr += (dex * MapleStatEffect.parseEval("u(x / 4)", x) / 100);
                    break;
                case 70000023:
                    localluk += (int_ * MapleStatEffect.parseEval("u(x / 4)", x) / 100);
                    break;
                case 70000024:
                    localdex += (luk * MapleStatEffect.parseEval("u(x / 4)", x) / 100);
                    break;
                case 70000025: {
                    int perLevelGain = MapleStatEffect.parseEval("(20 - (2 * d(x / 2)))", x);
                    watk += d(chra.getLevel() / perLevelGain);
                }
                break; //lv2pad = 20-2 * d (x / 2)	
                case 70000026: {
                    int perLevelGain = MapleStatEffect.parseEval("(20 - (2 * d(x / 2)))", x);
                    magic += d(chra.getLevel() / perLevelGain);
                }
                break;
                case 70000027:
                    percent_hp += SkillFactory.getSkill(ISVH.getSkillId()).getEffect(x).getPercentHP();
                    break;//accR = x
                case 70000028:
                    percent_hp += SkillFactory.getSkill(ISVH.getSkillId()).getEffect(x).getPercentMP();
                    break;
                case 70000029:
                    wdef += (wdef * (MapleStatEffect.parseEval("x", x) / 100));
                    break;
                case 70000030:
                    mdef += (mdef * (MapleStatEffect.parseEval("x", x) / 100));
                    break;
                case 70000031:
                    percent_hp += SkillFactory.getSkill(ISVH.getSkillId()).getEffect(x).getPercentHP();
                    break;
                case 70000032:
                    percent_hp += SkillFactory.getSkill(ISVH.getSkillId()).getEffect(x).getPercentMP();
                    break;
                case 70000033:
                    accuracy += (accuracy * (MapleStatEffect.parseEval("u(x / 2)", x) / 100));
                    break; //ar = u (x/2)
                case 70000034:
                    evaR += (evaR * (MapleStatEffect.parseEval("u(x / 2)", x) / 100));
                    break;
                case 70000035:
                    break;//bossdam_r += MapleStatEffect.parseEval("x", x); break;//bdR = x //Clientsided?
                case 70000036:
                    break;//+% Damage to Norm Mobs//Clientsided?
                case 70000037:
                    break;//+% Damage to Towers//Clientsided?
                case 70000038:
                    break;//+% Chance to insta-kill in Azwan Supply//Clientsided?
                case 70000039:
                    break;//+% Damage when attacking abnormal ailment targets.//Clientsided?
                case 70000040: { //Definitely needs to be looked into. "% of Wep Acc or Magic ACC (>) added to additional damage.
                    watk += (accuracy * (MapleStatEffect.parseEval("x * 2 + u (x / 2)", x) / 100));//acc2dam = x * 2 + u (x / 2)
                    magic += (accuracy * (MapleStatEffect.parseEval("x * 2 + u (x / 2)", x) / 100));
                    //Assuming only one type of attack (magic or weapon) can be active at a time, just apply them to both;
                }
                break;
                case 70000041: {
                    watk += (wdef * (MapleStatEffect.parseEval("x * 2 + u (x / 2)", x) / 100));//pdd2dam = x * 2 + u (x / 2)
                    magic += (wdef * (MapleStatEffect.parseEval("x * 2 + u (x / 2)", x) / 100));
                }
                break;
                case 70000042: {
                    watk += (wdef * (MapleStatEffect.parseEval("x * 2 + u (x / 2)", x) / 100));//pdd2dam = x * 2 + u (x / 2)
                    magic += (mdef * (MapleStatEffect.parseEval("x * 2 + u (x / 2)", x) / 100));
                }
                break;
                case 70000043:
                    critical_rate += SkillFactory.getSkill(ISVH.getSkillId()).getEffect(x).getCr();
                    break;
                case 70000044:
                    break; //When hit with physical attack, damage equal to % of MDEF is ignored. //Clientsided?
                case 70000045: //Cooldown not applied at % Chance should be hooked elsewhere. //Clientsided? TEST.
                    randCooldown += SkillFactory.getSkill(ISVH.getSkillId()).getEffect(x).getNocoolProp();
                    break;
                case 70000046: //Increase Skill level of passive skills by # //Pretty sure Clientsided
                case 70000047: //Number of enemies hit by multi target skills +1 //Clientsided.
                    break;
                case 70000048: //Buff skill duration +% Should be hooked elsewhere. //Clientsided? TEST.
                    BuffUP_Skill += (MapleStatEffect.parseEval("x+u(x/4)", x));
                    break;
                case 70000049:
                    dropBuff += (MapleStatEffect.parseEval("u(x/2)", x));
                    break; //dropR = u (x / 2)	
                case 70000050:
                    mesoBuff += (MapleStatEffect.parseEval("u(x / 2)", x));
                    break;
                case 70000051: {
                    localstr += MapleStatEffect.parseEval("x", x);
                    localdex += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000052: {
                    localstr += MapleStatEffect.parseEval("x", x);
                    localint_ += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000053: {
                    localstr += MapleStatEffect.parseEval("x", x);
                    localluk += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000054: {
                    localdex += MapleStatEffect.parseEval("x", x);
                    localint_ += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000055: {
                    localdex += MapleStatEffect.parseEval("x", x);
                    localluk += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000056: {
                    localint_ += MapleStatEffect.parseEval("x", x);
                    localluk += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000057: {
                    localdex += MapleStatEffect.parseEval("x", x);
                    localstr += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000058: {
                    localint_ += MapleStatEffect.parseEval("x", x);
                    localstr += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000059: {
                    localluk += MapleStatEffect.parseEval("x", x);
                    localstr += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000060: {
                    localint_ += MapleStatEffect.parseEval("x", x);
                    localdex += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000061: {
                    localluk += MapleStatEffect.parseEval("x", x);
                    localdex += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                case 70000062: {
                    localluk += MapleStatEffect.parseEval("x", x);
                    localint_ += MapleStatEffect.parseEval("u(x / 2)", x);
                }
                break;
                default:
                    break;
            }
        }

        Pair<Long, Long> hpmp = handlePassiveSkills(chra);
        localmaxhp_ += hpmp.left;
        localmaxmp_ += hpmp.right;

        Pair<Long, Long> unions = handleUnionSkills(chra);

        localmaxhp_ += unions.left;
        localmaxmp_ += unions.right;

        localstr += Math.floor((localstr * percent_str) / 100.0f);
        localdex += Math.floor((localdex * percent_dex) / 100.0f);
        localint_ += Math.floor((localint_ * percent_int) / 100.0f);
        localluk += Math.floor((localluk * percent_luk) / 100.0f);
        if (localint_ > localdex) {
            accuracy += localint_ + Math.floor(localluk * 1.2);
        } else {
            accuracy += localluk + Math.floor(localdex * 1.2);
        }
        watk += Math.floor((watk * percent_atk) / 100.0f);
        magic += Math.floor((magic * percent_matk) / 100.0f);
        localint_ += Math.floor((localint_ * percent_matk) / 100.0f);

        wdef += Math.floor((localstr * 1.2) + ((localdex + localluk) * 0.5) + (localint_ * 0.4));
        mdef += Math.floor((localstr * 0.4) + ((localdex + localluk) * 0.5) + (localint_ * 1.2));
        wdef += Math.min(30000, Math.floor((wdef * percent_wdef) / 100.0f));
        mdef += Math.min(30000, Math.floor((wdef * percent_mdef) / 100.0f));

        critical_rate = (short) Math.min(100, critical_rate);

        localmaxhp_ += Math.floor((multi_lateral_hp * localmaxhp_) / 100.0f);
        localmaxhp_ += Math.floor((percent_hp * localmaxhp_) / 100.0f);

        if (multi_lateral_hp >= 30) {
            localmaxhp_ *= 0.977202581369248;
        }

        localmaxhp_ += chra.getTrait(MapleTraitType.will).getLevel() * 20;
        localmaxhp_ += fixHp;

        localmaxmp_ += Math.floor((multi_lateral_mp * localmaxmp_) / 100.0f);

        localmaxmp_ += Math.floor((percent_mp * localmaxmp_) / 100.0f);
        localmaxmp_ += chra.getTrait(MapleTraitType.sense).getLevel() * 20;

        localmaxhp = Math.min(500000, Math.abs(Math.max(-500000, localmaxhp_)));
        localmaxhp = Math.max(1, localmaxhp);

        localmaxmp = Math.min(500000, Math.abs(Math.max(-500000, localmaxmp_)));

        if (chra.getBuffedEffect(MapleBuffStat.LimitMP) != null) {
            localmaxmp = chra.getBuffedValue(MapleBuffStat.LimitMP);
        }

        if (hp > localmaxhp) {
            chra.addHP(-(hp - localmaxhp));
        }

        if (mp > localmaxmp) {
            chra.addMP(-(mp - localmaxmp));
        }

        hands = localdex + localint_ + localluk;
        calculateFame(chra);
//        ignoreTargetDEF += ((100 - ignoreTargetDEF) * ((chra.getTrait(MapleTraitType.charisma).getLevel() / 10) / (double) 100));
        pvpDamage += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;
        ASR += chra.getTrait(MapleTraitType.will).getLevel() / 5;

        accuracy += Math.floor((accuracy * percent_acc) / 100.0f);
        accuracy += chra.getTrait(MapleTraitType.insight).getLevel() * 15 / 10;

        // custom
        if (chra.getParty() != null) {
            mesoBuff += Math.max(0, (chra.getParty().getMembers().size() - 1) * 5);
        }
        //

        chra.changeSkillLevel_Skip(sData, false);
        if (GameConstants.isDemonSlayer(chra.getJob())) {
            localmaxmp = GameConstants.getMPByJob(chra);
        } else if (GameConstants.isZero(chra.getJob())) {
            localmaxmp = 100;
        }
        if (GameConstants.isDemonAvenger(chra.getJob())) {
            EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.LifeTidal, new Pair<>((int) 3, 0));
            chra.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chra));
        }
        CalcPassive_Mastery(chra);
        recalcPVPRank(chra);
        if (first_login) {
            chra.silentEnforceMaxHpMp();
            relocHeal(chra);
        } else {
            chra.enforceMaxHpMp();
        }
        calculateMaxBaseDamage(Math.max(magic, watk), pvpDamage, chra);
        trueMastery = Math.min(100, trueMastery);
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            chra.updatePartyMemberHP();
        }
    }

    private Pair<Long, Long> handleUnionSkills(MapleCharacter chra) {
        long localmaxhp_ = 0, localmaxmp_ = 0;

        List<Point> setPoints = new ArrayList<>();

        try {
            for (MapleUnion union : chra.getUnions().getUnions()) {
                if (union.getPosition() < 0) {
                    continue;
                }

                int level = 0;
                if (!GameConstants.isZero(union.getJob())) {
                    if (union.getLevel() >= 250) {
                        level = 5;
                    } else if (union.getLevel() >= 200) {
                        level = 4;
                    } else if (union.getLevel() >= 140) {
                        level = 3;
                    } else if (union.getLevel() >= 100) {
                        level = 2;
                    } else if (union.getLevel() >= 60) {
                        level = 1;
                    }
                } else {
                    if (union.getLevel() >= 250) {
                        level = 5;
                    } else if (union.getLevel() >= 200) {
                        level = 4;
                    } else if (union.getLevel() >= 180) {
                        level = 3;
                    } else if (union.getLevel() >= 160) {
                        level = 2;
                    } else if (union.getLevel() >= 130) {
                        level = 1;
                    }
                }

                int type = 0;
                if (GameConstants.isXenon(union.getJob())) {
                    type = 36;
                } else if (GameConstants.isWarrior(union.getJob())) {
                    type = 1;
                } else if (GameConstants.isMagician(union.getJob())) {
                    type = 2;
                } else if (GameConstants.isArcher(union.getJob())) {
                    type = 3;
                } else if (GameConstants.isThief(union.getJob())) {
                    type = 4;
                } else if (GameConstants.isPirate(union.getJob())) {
                    type = 5;
                }

                int unionLevel = (int) chra.getKeyValue(18771, "rank");

                if (level > 0 && type > 0) {

                    //직업 스킬 계산
                    if (UnionHandler.cardSkills.containsKey(union.getJob() / 10)) {

                        int jobSkill = UnionHandler.cardSkills.get(union.getJob() / 10);

                        MapleStatEffect jobSkillEffect = SkillFactory.getSkill(jobSkill).getEffect(level);
                        if (jobSkillEffect != null) {
                            switch (jobSkill) {
                                case 71000013:
                                    percent_hp += jobSkillEffect.getPercentHP();
                                    break;
                                case 71000021:
                                    percent_mp += jobSkillEffect.getPercentMP();
                                    break;
                                case 71000052:
                                    BuffUP_Summon += jobSkillEffect.getSummonTimeR();
                                    break;
                                case 71000111:
                                case 71000511:
                                    fixHp += jobSkillEffect.getHpFX();
                                    break;
                                case 71000351:
                                    BuffUP_Skill += jobSkillEffect.getBufftimeR();
                                    break;
                                case 71000711:
                                    expBuffZero += jobSkillEffect.getEXPRate();
                                    break;
                                case 71000231:
                                    coolTimeR += jobSkillEffect.getCoolTimeR();
                                    break;
                            }
                        }
                    }

                    //점령 스킬 계산
                    if (UnionHandler.characterSizes.containsKey(type) && UnionHandler.characterSizes.get(type).containsKey(level - 1)) {

                        Map<Integer, Integer> skills = new HashMap<>();

                        List<Point> characterData = UnionHandler.characterSizes.get(type).get(level - 1);
                        for (Point addPos : characterData) {
                            Point pos = UnionHandler.boardPos.get(union.getPosition());
                            int angle = union.getUnk2(); // 각도
                            Point poszz = new Point(pos.x + addPos.x, pos.y + addPos.y);
                            if (angle >= 1000) {
                                if (angle >= 3000) {
                                    poszz.x *= -1;
                                    poszz.y *= -1;
                                } else if (angle >= 2000) {
                                    poszz.y *= -1;
                                } else if (angle >= 1000) {
                                    poszz.x *= -1;
                                }
                            }

                            if (angle >= 270) {
                                poszz = new Point(-poszz.y, -poszz.x);
                            } else if (angle >= 180) {
                                poszz.x *= -1;
                                poszz.y *= -1;
                            } else if (angle >= 90) {
                                poszz = new Point(poszz.y, poszz.x);
                            }

                            if (setPoints.contains(poszz)) {
                                continue;
                            } else {
                                setPoints.add(poszz);
                            }

                            for (Point realPos : UnionHandler.boardPos) {
                                if (realPos.x == poszz.x && realPos.y == poszz.y) {
                                    int index = UnionHandler.groupIndex.get(UnionHandler.boardPos.indexOf(realPos));
                                    int reqLevel = UnionHandler.openLevels.get(UnionHandler.boardPos.indexOf(realPos));
                                    if (unionLevel >= reqLevel) {
                                        int skillID = UnionHandler.skills.get(index);
                                        if (skills.containsKey(skillID)) {
                                            skills.put(skillID, skills.get(skillID) + 1);
                                        } else {
                                            skills.put(skillID, 1);
                                        }
                                    }
                                }
                            }
                        }

                        for (Entry<Integer, Integer> skill : skills.entrySet()) {
                            MapleStatEffect fieldEffect = SkillFactory.getSkill(skill.getKey()).getEffect(skill.getValue());
                            if (fieldEffect != null) {
                                switch (skill.getKey()) {
                                    case 71004006:
                                        localmaxhp_ += fieldEffect.getMhpX();
                                        break;
                                    case 71004007:
                                        localmaxmp_ += fieldEffect.getMaxMpX();
                                        break;
                                    case 71004010:
                                        expBuffUnion += fieldEffect.getExpRPerM();
                                        break;
                                    case 71004014:
                                        BuffUP_Skill += fieldEffect.getBufftimeR();
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Pair<>(localmaxhp_, localmaxmp_);
    }

    public List<Triple<Integer, String, Integer>> getPsdSkills() {
        return psdSkills;
    }

    private Pair<Long, Long> handlePassiveSkills(final MapleCharacter chra) {
        Skill bx;
        int bof;
        MapleStatEffect eff = null;
        DAMreduceR = 0;
        long localmaxhp_ = 0, localmaxmp_ = 0;

        psdSkills.clear();

        if (chra.getGuild() != null) {
            bx = SkillFactory.getSkill(91000034);
            bof = chra.getGuild().getSkillLevel(91000034);
            if (bof > 0) {
                localmaxhp_ += bx.getEffect(bof).getMaxHpX();
            }
        }

        if (chra.getKeyValue(501046, "1") > 0) {
            bx = SkillFactory.getSkill(80002928);
            bof = (int) chra.getKeyValue(501046, "1");
            if (bof > 0 && bx.getEffect(bof) != null) {
                localmaxhp_ += bx.getEffect(bof).getMaxHpX();
                localmaxmp_ += bx.getEffect(bof).getMaxMpX();
            }
        }

        bx = SkillFactory.getSkill(80002774);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            localmaxhp_ += bx.getEffect(bof).getMaxHpX();
            localmaxmp_ += bx.getEffect(bof).getMaxMpX();
        }

        bx = SkillFactory.getSkill(80002775);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            localmaxhp_ += bx.getEffect(bof).getMaxHpX();
            localmaxmp_ += bx.getEffect(bof).getMaxMpX();
        }

        bx = SkillFactory.getSkill(80002776);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            localmaxhp_ += bx.getEffect(bof).getMaxHpX();
            localmaxmp_ += bx.getEffect(bof).getMaxMpX();
        }

        bx = SkillFactory.getSkill(80000006);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            percent_hp += bx.getEffect(bof).getPercentHP();
            percent_mp += bx.getEffect(bof).getPercentMP();
        }

        bx = SkillFactory.getSkill(60000222);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            percent_hp += bx.getEffect(bof).getPercentHP();
            percent_mp += bx.getEffect(bof).getPercentMP();
        }

        bx = SkillFactory.getSkill(80000409);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            critical_rate += bx.getEffect(bof).getCr();
        }

        bx = SkillFactory.getSkill(80000002); // Phantom Instinct
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            critical_rate += eff.getCr();
        }
        //custom Buff
        if (chra.getBuffedValue(80001535)) {
            dropBuff += 20.0;
        }

        if (chra.getBuffedValue(80001536)) {
            dropBuff += 60.0;
            mesoBuff += 30.0;
        }

        if (chra.getBuffedValue(80001537)) {
            dropBuff += 70.0;
            mesoBuff += 40.0;
        }

        if (chra.getBuffedValue(80001538)) {
            dropBuff += 100.0;
            mesoBuff += 50.0;
        }

        if (chra.getBuffedValue(80001539)) {
            dropBuff += 120.0;
            mesoBuff += 70.0;
        }

        //end
        switch (chra.getJob()) {
            case 16200:
            case 16210:
            case 16211:
            case 16212: {
                bx = SkillFactory.getSkill(162000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    damAbsorbShieldR += bx.getEffect(bof).getDamAbsorbShieldR();
                }
                bx = SkillFactory.getSkill(162110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                break;
            }
            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132: {
                bx = SkillFactory.getSkill(1000009);
                bof = chra.getTotalSkillLevel(1000009);
                if (bof > 0) {
                    localmaxhp_ += bx.getEffect(bof).getLv2mhp() * chra.getLevel();
                }
                bx = SkillFactory.getSkill(1000003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(1210001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }
                bx = SkillFactory.getSkill(1220005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    DAMreduceR += (5 + (0.5d * bof)); //achilles doesn't use X
                }
                bx = SkillFactory.getSkill(1220018);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    trueMastery += bx.getEffect(bof).getMastery();
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(1310000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getX();
                }
                bx = SkillFactory.getSkill(1310009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                    critical_damage += bx.getEffect(bof).getCriticalDamage();
                }
                bx = SkillFactory.getSkill(1320016);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    if (chra.getStat().getHPPercent() >= eff.getX()) {
                        critical_rate += eff.getCr();
                        critical_damage += bx.getEffect(bof).getCriticalDamage();

                        bx = SkillFactory.getSkill(1320048);
                        bof = chra.getTotalSkillLevel(bx);
                        if (bof > 0) {
                            critical_rate += bx.getEffect(bof).getCr();
                        }
                    }
                }
                break;
            }
            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232: {
                bx = SkillFactory.getSkill(2000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localmaxmp_ += bx.getEffect(bof).getLv2mmp() * chra.getLevel();
                    percent_mp += bx.getEffect(bof).getPercentMP();
                    Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon != null) {
                        if (weapon.getItemId() / 1000 == 1372) { // wand
                            critical_rate += 3;
                        }
                    }
                }

                bx = SkillFactory.getSkill(2120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    BuffUP_Skill += bx.getEffect(bof).getBufftimeR();
                }

                bx = SkillFactory.getSkill(80002932);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    BuffUP_Skill += bx.getEffect(bof).getBufftimeR();
                }

                bx = SkillFactory.getSkill(2220013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    BuffUP_Skill += bx.getEffect(bof).getBufftimeR();
                }

                bx = SkillFactory.getSkill(2110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(2210009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(2310010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(2310008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(2320012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    BuffUP_Skill += bx.getEffect(bof).getBufftimeR();
                }
                break;
            }
            case 300:
            case 301:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322:
            case 330:
            case 331:
            case 332: {
                bx = SkillFactory.getSkill(3000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(3000002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }

                bx = SkillFactory.getSkill(3010003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }

                bx = SkillFactory.getSkill(3100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(3001004, eff.getX());
                    damageIncrease.put(3001005, eff.getY());
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(3110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(3120005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    watk += bx.getEffect(bof).getX();
                }

                bx = SkillFactory.getSkill(3120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(3120006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && chra.getBuffedValue(MapleBuffStat.SpiritLink) != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getX();
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }

                bx = SkillFactory.getSkill(3220004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                }
                bx = SkillFactory.getSkill(3220009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(3310006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                break;
            }
            case 14200:
            case 14210:
            case 14211:
            case 14212: {
                bx = SkillFactory.getSkill(142000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(142100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(142120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    BuffUP_Skill += bx.getEffect(bof).getBufftimeR();
                }
                break;
            }
            case 15100:
            case 15110:
            case 15111:
            case 15112: {
                bx = SkillFactory.getSkill(151000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localmaxhp_ += bx.getEffect(bof).getMaxHpX();
                }
                bx = SkillFactory.getSkill(151110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(151120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 15200:
            case 15210:
            case 15211:
            case 15212: {
                bx = SkillFactory.getSkill(152000007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                break;
            }
            case 16400:
            case 16410:
            case 16411:
            case 16412: {
                bx = SkillFactory.getSkill(164110012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 1200:
            case 1210:
            case 1211:
            case 1212: {
                bx = SkillFactory.getSkill(12000025);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                    localmaxmp_ += bx.getEffect(bof).getLv2mmp() * chra.getLevel();
                    Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon != null) {
                        if (weapon.getItemId() / 1000 == 1372) { // wand
                            critical_rate += 3;
                        }
                    }
                }
                bx = SkillFactory.getSkill(12000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                bx = SkillFactory.getSkill(12110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getX();
                }

                bx = SkillFactory.getSkill(12110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(12110026); // Typhoon Crush
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                break;
            }
            case 1100:
            case 1110:
            case 1111:
            case 1112: {
                bx = SkillFactory.getSkill(11000023);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(11110024);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localmaxhp_ += bx.getEffect(bof).getMaxHpX();
                }
                bx = SkillFactory.getSkill(11120006); // Typhoon Crush
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                break;
            }
            case 6000:
            case 6100:
            case 6110:
            case 6111:
            case 6112: {
                bx = SkillFactory.getSkill(61100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(61110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 3101:
            case 3120:
            case 3121:
            case 3122: {
                bx = SkillFactory.getSkill(31010003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(31200006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localmaxhp_ += bx.getEffect(bof).getMaxHpX();
                }
                bx = SkillFactory.getSkill(31221008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 10000:
            case 10100:
            case 10110:
            case 10111:
            case 10112: {
                bx = SkillFactory.getSkill(101100203);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 3200:
            case 3210:
            case 3211:
            case 3212: {
                bx = SkillFactory.getSkill(32000015);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(32100006);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(32100008);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(32121010);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                }
                break;
            }
            case 2400:
            case 2410:
            case 2411:
            case 2412: { // Phantom
                // Blanc Carte - 24100003
                // Cane Mastery - 24100004
                // Luck of Phantom Thief - 24111002
                // 24111003- uses monlight effect, but is Misfortune Protection
                // 24110004 - Flash and Flee -> active
                // 24111005 - Moonlight
                // 24111006 - Phantom Charge
                // 24111008- Breeze Carte, (hidden), linked to phantom charge
                // 24121000 - Ultimate Drive
                // 24120002 - Noir Carte
                // 24121003 - Twilight
                // 24121004 - Pray of Aria
                // 24121005 - Tempest of Card
                // 24120006 - Cane Expert
                // 24121007 - Soul Steal
                // 24121008 - Maple Warrior
                // 24121009 - Hero's will
                // 24121010 - Some linked skill (Twilight)
                bx = SkillFactory.getSkill(24001002); // Swift Phantom
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                }
                bx = SkillFactory.getSkill(24000003); // Quick Evasion
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    evaR += eff.getX();
                }
                bx = SkillFactory.getSkill(24100006); //Luck Monopoly
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(24110007); // Acute Sense
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(20030204); // Phantom Instinct
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(20030206); // Dexterous Training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(24111002); //Luck of Phantom Thief
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(24110003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                break;
            }
            case 2700:
            case 2710:
            case 2711:
            case 2712: {
                bx = SkillFactory.getSkill(27000004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }

                bx = SkillFactory.getSkill(27110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    if (chra.getStat().getHPPercent() >= chra.getStat().getMPPercent()) {
                        critical_rate += bx.getEffect(bof).getProp();
                    }
                }

                bx = SkillFactory.getSkill(27120007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                break;
            }
            case 501:
            case 530:
            case 531:
            case 532:
                bx = SkillFactory.getSkill(5010003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }

                bx = SkillFactory.getSkill(5300004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5311002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5300008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }

                bx = SkillFactory.getSkill(5311001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(5301001, (int) bx.getEffect(bof).getDAMRate());
                }

                bx = SkillFactory.getSkill(5310007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    percent_wdef += eff.getWDEFRate();
                }

                bx = SkillFactory.getSkill(5310006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }

                bx = SkillFactory.getSkill(5321009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                break;
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112: {
                mpRecoverProp = 100;
                bx = SkillFactory.getSkill(31000003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(31100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(31000004, (int) eff.getDAMRate());
                    damageIncrease.put(31001006, (int) eff.getDAMRate());
                    damageIncrease.put(31001007, (int) eff.getDAMRate());
                    damageIncrease.put(31001008, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(31100005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(31100010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(31000004, (int) eff.getX());
                    damageIncrease.put(31001006, (int) eff.getX());
                    damageIncrease.put(31001007, (int) eff.getX());
                    damageIncrease.put(31001008, (int) eff.getX());
                }
                bx = SkillFactory.getSkill(31111007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(31110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    evaR += eff.getX();
                    // HACK: shouldn't be here
                    //hpRecoverPercent += eff.getY();
                    //hpRecoverProp += eff.getX();
                    //mpRecover += eff.getY(); // handle in takeDamage
                    //mpRecoverProp += eff.getX();
                }
                bx = SkillFactory.getSkill(31110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpRecover += 1;
                    mpRecoverProp += eff.getProp();
                }
                bx = SkillFactory.getSkill(31100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(31120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(31000004, (int) eff.getX());
                    damageIncrease.put(31001006, (int) eff.getX());
                    damageIncrease.put(31001007, (int) eff.getX());
                    damageIncrease.put(31001008, (int) eff.getX());
                }
                bx = SkillFactory.getSkill(31120008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    trueMastery += eff.getMastery();
                }
                bx = SkillFactory.getSkill(31120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_wdef += bx.getEffect(bof).getT();
                }
                bx = SkillFactory.getSkill(30010112);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    bossdam_r += eff.getBossDamage();
                    mpRecover += eff.getX();
                    mpRecoverProp += eff.getBossDamage(); //yes
                }
                bx = SkillFactory.getSkill(30010185);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    chra.getTrait(MapleTraitType.will).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getY()));
                    chra.getTrait(MapleTraitType.charisma).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getZ()));
                }
                bx = SkillFactory.getSkill(30010111);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverPercent += eff.getX();
                    hpRecoverProp += eff.getProp(); //yes
                }
                bx = SkillFactory.getSkill(31120009); //Obsidian Skin
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    DAMreduceR += (int) (5 + (0.5d * bof)); //achilles doesn't use X
                }
                break;
            }
            case 510:
            case 511:
            case 512: {
                bx = SkillFactory.getSkill(5100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 1500:
            case 1510:
            case 1511:
            case 1512: {
                bx = SkillFactory.getSkill(15000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(15110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getProp();
                }

                bx = SkillFactory.getSkill(15110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(15120007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }

                bx = SkillFactory.getSkill(15120008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                break;
            }
            case 400: // Thief
            case 410: // Assassin
            case 411: // Hermit
            case 412: // Night Lord
            case 420: // Bandit
            case 421: // Chief Bandit
            case 422: { // Shadower
                bx = SkillFactory.getSkill(4001005); // Haste
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    speed += eff.getSpeedMax();
                }

                // 4000010: Magic Theft, invisible.
                if (chra.getJob() >= 410 && chra.getJob() <= 412) {

                    bx = SkillFactory.getSkill(4000001); // Haste
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        defRange += bx.getEffect(bof).getRange();
                    }

                    bx = SkillFactory.getSkill(4100007); // Physical Training
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                        localluk += eff.getLukX();
                        localdex += eff.getDexX();
                    }
                }
                if (chra.getJob() >= 420 && chra.getJob() <= 422) {
                    bx = SkillFactory.getSkill(4200007); // Physical Training
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                        localluk += eff.getLukX();
                        localdex += eff.getDexX();
                    }
                }
                if (chra.getJob() == 411 || chra.getJob() == 412) {
                    bx = SkillFactory.getSkill(4110008); // Enveloping Darkness
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                        percent_hp += eff.getPercentHP();
                        ASR += eff.getASRRate();
                        TER += eff.getTERRate();
                    }
                    bx = SkillFactory.getSkill(4110012); // Expert Throwing Star Handling
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                        damageIncrease.put(4001344, eff.getDAMRate());
                        damageIncrease.put(4101008, eff.getDAMRate());
                        damageIncrease.put(4101009, eff.getDAMRate());
                        damageIncrease.put(4101010, eff.getDAMRate());
                    }
                    bx = SkillFactory.getSkill(4110014);
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                        RecoveryUP += eff.getX() - 100;
                    }
                }
                if (chra.getJob() == 412) {
                    bx = SkillFactory.getSkill(4121014); // Dark Harmony
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                    }
                }

                bx = SkillFactory.getSkill(4200006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(4210000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }
                break;
            }
            case 431: // Blade Acolyte
            case 432: // Blade Specialist
            case 433: // Blade Lord
            case 434: { // Blade Master
                bx = SkillFactory.getSkill(4001006); // Haste
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    speed += eff.getSpeedMax();
                }

                bx = SkillFactory.getSkill(4330001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4331000, eff.getY());
                    damageIncrease.put(4331006, eff.getY());
                    damageIncrease.put(4341002, eff.getY());
                    damageIncrease.put(4341004, eff.getY());
                    damageIncrease.put(4341009, eff.getY());
                    damageIncrease.put(4341011, eff.getY());

                }

                bx = SkillFactory.getSkill(4310004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }

                bx = SkillFactory.getSkill(4341006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
                }

                bx = SkillFactory.getSkill(4340010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }

                bx = SkillFactory.getSkill(4310006); //physical training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    localluk += eff.getLukX();
                }

                bx = SkillFactory.getSkill(4330008); //Enveloping Darkness
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                break;
            }
            case 3500:
            case 3510:
            case 3511:
            case 3512: {
                bx = SkillFactory.getSkill(35001002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localmaxhp_ += eff.getMaxHpX();
                    localmaxmp_ += eff.getMaxHpX();
                }
                bx = SkillFactory.getSkill(35100000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(35120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getIndieMhpR();
                    trueMastery += bx.getEffect(bof).getMastery();
                }
                bx = SkillFactory.getSkill(35100011); //physical training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(35110016);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(35110018);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                }
                bx = SkillFactory.getSkill(35120018);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                }
                break;
            }
            case 3300:
            case 3310:
            case 3311:
            case 3312: {
                bx = SkillFactory.getSkill(33000005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    defRange += eff.getRange();
                }
                bx = SkillFactory.getSkill(33120000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                }
                bx = SkillFactory.getSkill(33110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(33120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    evaR += eff.getER();
                }
                bx = SkillFactory.getSkill(32110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                break;
            }
            case 5000: // Mihile 0
            case 5100: // Mihile 1
            case 5110: // Mihile 2
            case 5111: // Mihile 3
            case 5112: { // Mihile 4

                // Mihile 1st Job Passive Skills
                bx = SkillFactory.getSkill(51000000); // Mihile || HP Boost
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(51000001); // Mihile || Soul Shield
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                }
                bx = SkillFactory.getSkill(51000002); // Mihile || Soul Devotion
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAccX();
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                }

                // Mihile 2nd Job Passive Skills
                bx = SkillFactory.getSkill(51100000); // Mihile || Physical Training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(5001002, eff.getX());
                    damageIncrease.put(5001003, eff.getY());
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }

                bx = SkillFactory.getSkill(51110001); // Mihile || Final Attack && Advanced Final Attack
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }

                bx = SkillFactory.getSkill(51120002); // Mihile || Final Attack && Advanced Final Attack
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(51100002, (int) eff.getDamage());
                }

                // Mihile 3rd Job Passive Skills
                bx = SkillFactory.getSkill(51110000); // Mihile || Self Recovery
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    /*eff = bx.getEffect(bof);
                     hpRecoverProp += eff.getProp();
                     hpRecover += eff.getX();
                     mpRecoverProp += eff.getProp();
                     mpRecover += eff.getX();*/
                    eff = bx.getEffect(bof);
                    final int mpToHeal = (int) (eff.getMp());
                    final int hpToHeal = (int) (eff.getHp());
                }
                bx = SkillFactory.getSkill(51110001); // Mihile || Intense Focus
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    // Add Attack Speed here
                }
                bx = SkillFactory.getSkill(51110002); // Mihile || Righteous Indignation
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getX();
                    percent_atk += eff.getX();
                }

                bx = SkillFactory.getSkill(51120001); // Mihile || Expert Sword Mastery
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += bx.getEffect(bof).getX();
                    trueMastery += eff.getMastery();
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(51120003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    DAMreduceR += eff.getT();
                }
                break;
            }
            case 2200:
            case 2210:
            case 2211:
            case 2212:
            case 2213:
            case 2214:
            case 2215:
            case 2216:
            case 2217:
            case 2218: {
                bx = SkillFactory.getSkill(22000014);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localmaxmp_ += bx.getEffect(bof).getLv2mmp() * chra.getLevel();
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }

                bx = SkillFactory.getSkill(22140000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(22140018);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                magic += chra.getTotalSkillLevel(SkillFactory.getSkill(22000000));
                bx = SkillFactory.getSkill(22150000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                bx = SkillFactory.getSkill(22160000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(22110018);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getX();
                    trueMastery += eff.getMastery();
                    critical_rate += eff.getCr();
                    Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon != null) {
                        if (weapon.getItemId() / 1000 == 1372) { // wand
                            critical_rate += 3;
                        }
                    }
                }

                bx = SkillFactory.getSkill(22120002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getX();
                    trueMastery += eff.getMastery();
                    critical_rate += eff.getCr();
                    Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon != null) {
                        if (weapon.getItemId() / 1000 == 1372) { // wand
                            critical_rate += 3;
                        }
                    }
                }
                break;
            }
            case 2100:
            case 2110:
            case 2111:
            case 2112: {
                bx = SkillFactory.getSkill(21101005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }

                bx = SkillFactory.getSkill(21110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                break;
            }
            case 3600:
            case 3610:
            case 3611:
            case 3612: {
                bx = SkillFactory.getSkill(36100002);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(36101003);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localmaxhp_ += eff.getMhpX();
                    localmaxmp_ += eff.getMhpX();
                }

                bx = SkillFactory.getSkill(36000004);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    if (str >= eff.getX() && dex >= eff.getX() && luk >= eff.getX()) {
                        multi_lateral_hp += eff.getS();
                        multi_lateral_mp += eff.getS();
                    }
                }
                bx = SkillFactory.getSkill(36100007);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    if (str >= eff.getX() && dex >= eff.getX() && luk >= eff.getX()) {
                        multi_lateral_hp += eff.getS();
                        multi_lateral_mp += eff.getS();
                    }
                }
                bx = SkillFactory.getSkill(36110007);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    if (str >= eff.getX() && dex >= eff.getX() && luk >= eff.getX()) {
                        multi_lateral_hp += eff.getS();
                        multi_lateral_mp += eff.getS();
                    }
                    if (chra.getLevel() >= 200) {
                        bx = SkillFactory.getSkill(36120010);
                        eff = bx.getEffect(1); // skillLv 1
                        if (str >= eff.getX() && dex >= eff.getX() && luk >= eff.getX()) {
                            multi_lateral_hp += eff.getS();
                            multi_lateral_mp += eff.getS();
                        }
                    }
                }
                break;
            }
            case 3710:
            case 3711:
            case 3712: {
                bx = SkillFactory.getSkill(37100004);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                break;
            }
            case 6400:
            case 6410:
            case 6411:
            case 6412: {
                bx = SkillFactory.getSkill(64110006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                break;
            }
            case 6500:
            case 6510:
            case 6511:
            case 6512: {
                bx = SkillFactory.getSkill(65120005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(65001002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localmaxhp_ += eff.getMhpX();
                }
                bx = SkillFactory.getSkill(65110005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localmaxhp_ += eff.getMhpX();
                }
                break;
            }
            case 15500:
            case 15510:
            case 15511:
            case 15512: {
                bx = SkillFactory.getSkill(155100010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                }
                break;
            }
            case 2500:
            case 2510:
            case 2511:
            case 2512: {
                bx = SkillFactory.getSkill(25000105);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                }

                bx = SkillFactory.getSkill(25120214);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                break;
            }
        }
        bx = SkillFactory.getSkill(3111010);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            percent_hp += eff.getPercentHP();
        }
        bx = SkillFactory.getSkill(3211010);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            percent_hp += eff.getPercentHP();
        }
        bx = SkillFactory.getSkill(5220019);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            localmaxhp_ += eff.getMhpX();
        }
        bx = SkillFactory.getSkill(33111007);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            percent_hp += eff.getPercentHP();
        }
        bx = SkillFactory.getSkill(110);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            percent_hp += eff.getHpR();
            percent_mp += eff.getMpR();
        }
        bx = SkillFactory.getSkill(80000000);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            percent_hp += eff.getHpR();
            percent_mp += eff.getMpR();
        }
        bx = SkillFactory.getSkill(80000001);
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            bossdam_r += eff.getBossDamage();
        }

        if (GameConstants.isAdventurer(chra.getJob())) {
            bx = SkillFactory.getSkill(74);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(80);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(10074);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(10080);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }

            bx = SkillFactory.getSkill(110);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                localstr += eff.getStrX();
                localdex += eff.getDexX();
                localint_ += eff.getIntX();
                localluk += eff.getLukX();
                percent_hp += eff.getHpR();
                percent_mp += eff.getMpR();
            }

            bx = SkillFactory.getSkill(10110);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                localstr += eff.getStrX();
                localdex += eff.getDexX();
                localint_ += eff.getIntX();
                localluk += eff.getLukX();
                percent_hp += eff.getHpR();
                percent_mp += eff.getMpR();
            }
        }
        bx = SkillFactory.getSkill(GameConstants.getBOF_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getX();
        }

        bx = SkillFactory.getSkill(GameConstants.getEmpress_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getZ();
        }
        switch (chra.getJob()) {
            case 6300:
            case 6310:
            case 6311:
            case 6312: { //카인 패시브스킬
                bx = SkillFactory.getSkill(63000007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    critical_rate += bx.getEffect(bof).getCr();
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(63110015);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                bx = SkillFactory.getSkill(63120014);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    bossdam_r *= bx.getEffect(bof).getBossDamage() / 100.0;
                    dam_r *= bx.getEffect(bof).getDAMRate() / 100.0;
                }
                bx = SkillFactory.getSkill(63120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    trueMastery += bx.getEffect(bof).getMastery();
                    critical_damage += bx.getEffect(bof).getCriticalDamage();
                    watk += bx.getEffect(bof).getAttackX();
                }
                break;
            }

            case 210:
            case 211:
            case 212: { // IL
                bx = SkillFactory.getSkill(2100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(2110000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dotTime += eff.getX();
                    dot += eff.getZ();
                }
                bx = SkillFactory.getSkill(2110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                bx = SkillFactory.getSkill(2121003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(2111003, (int) eff.getX());
                }
                bx = SkillFactory.getSkill(2121005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getTERRate();
                }
                bx = SkillFactory.getSkill(2121009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    magic += bx.getEffect(bof).getMagicX();
                }
                bx = SkillFactory.getSkill(2120010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                }
                break;
            }
            case 220:
            case 221:
            case 222: { // IL
                bx = SkillFactory.getSkill(2200007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(2210000);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    dot += bx.getEffect(bof).getZ();
                }
                bx = SkillFactory.getSkill(2210001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }
                bx = SkillFactory.getSkill(2221009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                bx = SkillFactory.getSkill(2221005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getTERRate();
                }
                bx = SkillFactory.getSkill(2221009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    magic += bx.getEffect(bof).getMagicX();
                }
                bx = SkillFactory.getSkill(2220010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                }
                break;
            }
            case 1211:
            case 1212: { // flame
                bx = SkillFactory.getSkill(12110001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    dam_r *= eff.getY() / 100.0;
                    bossdam_r *= eff.getY() / 100.0;
                }

                bx = SkillFactory.getSkill(12111004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    TER += bx.getEffect(bof).getTERRate();
                }
                break;
            }
            case 230:
            case 231:
            case 232: { // Bishop
                bx = SkillFactory.getSkill(2300007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(2321010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getX();
                }
                bx = SkillFactory.getSkill(2320005);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                }
                bx = SkillFactory.getSkill(2320011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                    bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                }
                break;
            }
            case 6400:
            case 6410:
            case 6411:
            case 6412: {
                bx = SkillFactory.getSkill(64100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(64120008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(64120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                break;
            }
            case 2002:

            case 2300:
            case 2310:
            case 2311:
            case 2312: {
                bx = SkillFactory.getSkill(20020012); //Blessing of the Fairy for Mercedes
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getX();
                    accuracy += bx.getEffect(bof).getX();
                    watk += bx.getEffect(bof).getX();
                    magic += bx.getEffect(bof).getX();
                }

                bx = SkillFactory.getSkill(20020112);
                bof = chra.getSkillLevel(bx);
                if (bof > 0) {
                    chra.getTrait(MapleTraitType.charm).addLocalExp(GameConstants.getTraitExpNeededForLevel(30));
                }
                bx = SkillFactory.getSkill(23000001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(23000003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(23100003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }
                bx = SkillFactory.getSkill(23100008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(23110004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getProp();
                }
                bx = SkillFactory.getSkill(23110004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(23101001, (int) bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(23121004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getProp();
                }
                bx = SkillFactory.getSkill(23120009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += bx.getEffect(bof).getX();
                    trueMastery += eff.getMastery();
                }
                bx = SkillFactory.getSkill(23120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(23101001, (int) bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(23120012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312: {
                bx = SkillFactory.getSkill(13000023);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    defRange += bx.getEffect(bof).getRange();
                }
                bx = SkillFactory.getSkill(13110008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(13120004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                break;
            }
            case 422: {
                bx = SkillFactory.getSkill(4221007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Savage Blow, Steal, and Assaulter
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4201005, (int) eff.getDAMRate());
                    damageIncrease.put(4201004, (int) eff.getDAMRate());
                    damageIncrease.put(4211002, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(4210012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mesoBuff *= (eff.getMesoRate() + 100.0) / 100.0;
                    pickRate += eff.getU();
                    //MesoGuard -= eff.getV(); //handle this in maplestateffect is better, since it needs to be part of the buffstat
                    //MesoGuardMeso -= eff.getW();
                    damageIncrease.put(4211006, eff.getX());
                }
                bx = SkillFactory.getSkill(4210013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                    percent_hp += eff.getPercentHP();
                }
                break;
            }
            case 433:
            case 434: {
                bx = SkillFactory.getSkill(4330007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverProp += eff.getProp();
                    hpRecoverPercent += eff.getX();
                }

                bx = SkillFactory.getSkill(4330009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    evaR += eff.getER();
                }

                bx = SkillFactory.getSkill(4341002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(4311002, (int) eff.getDAMRate());
                    damageIncrease.put(4311003, (int) eff.getDAMRate());
                    damageIncrease.put(4321000, (int) eff.getDAMRate());
                    damageIncrease.put(4321001, (int) eff.getDAMRate());
                    damageIncrease.put(4331000, (int) eff.getDAMRate());
                    damageIncrease.put(4331004, (int) eff.getDAMRate());
                    damageIncrease.put(4331005, (int) eff.getDAMRate());
                }

                bx = SkillFactory.getSkill(4341006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    evaR += bx.getEffect(bof).getER();
                }
                break;
            }
            case 2110:
            case 2111:
            case 2112: { // Aran
                bx = SkillFactory.getSkill(21101006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(21110002);
                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0) {
                //                  damageIncrease.put(21000004, bx.getEffect(bof).getW());
                //            }
                bx = SkillFactory.getSkill(21111010);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    bossdam_r += bx.getEffect(bof).getBossDamage();
                }
                bx = SkillFactory.getSkill(21120002);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    damageIncrease.put(21100007, bx.getEffect(bof).getZ());
                }
                bx = SkillFactory.getSkill(21120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(21100002, (int) eff.getDAMRate());
                    damageIncrease.put(21110003, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(21100008); //[hysical training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(21120004); //High Defense
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    DAMreduceR += (int) (5 + (1.5d * Math.floor(bof / 2.0d))); //5+1.5*u(x/2)
                    percent_hp += eff.getPercentHP();
                }
                break;
            }
            case 3511:
            case 3512: {
                bx = SkillFactory.getSkill(35110014);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //ME-07 Drillhands, Atomic Hammer
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35001003, (int) eff.getDAMRate());
                    damageIncrease.put(35101003, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(35121006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Satellite
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35111001, (int) eff.getDAMRate());
                    damageIncrease.put(35111009, (int) eff.getDAMRate());
                    damageIncrease.put(35111010, (int) eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(35120001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) { //Satellite
                    eff = bx.getEffect(bof);
                    damageIncrease.put(35111005, eff.getX());
                    damageIncrease.put(35111011, eff.getX());
                    damageIncrease.put(35121009, eff.getX());
                    damageIncrease.put(35121010, eff.getX());
                    damageIncrease.put(35121011, eff.getX());
                    BuffUP_Summon += eff.getY();
                }
                break;
            }
            case 110:
            case 111:
            case 112: {
                bx = SkillFactory.getSkill(1100009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(1001004, eff.getX());
                    damageIncrease.put(1001005, eff.getY());
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(1110009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    critical_rate += eff.getCr();
                }
                bx = SkillFactory.getSkill(1120013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    damageIncrease.put(1100002, (int) eff.getDamage());
                }
                break;
            }
            case 120:
            case 121:
            case 122: {
                bx = SkillFactory.getSkill(1200009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(1001004, eff.getX());
                    damageIncrease.put(1001005, eff.getY());
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(1220006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    ASR += bx.getEffect(bof).getASRRate();
                    TER += bx.getEffect(bof).getTERRate();
                }
                break;
            }
            case 500:
            case 510:
            case 511:
            case 512:
            case 520:
            case 521:
            case 522: {
                bx = SkillFactory.getSkill(5000007); // Typhoon Crush
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5100008); // Typhoon Crush
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5110011); // Typhoon Crush
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5100010); // Physical Training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    //damageIncrease.put(5001002, eff.getX());
                    //damageIncrease.put(5001003, eff.getY());
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }

                bx = SkillFactory.getSkill(5121015); // CrossOverChainbones
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    TER += bx.getEffect(bof).getTERRate();
                    ASR += bx.getEffect(bof).getASRRate();
                }
                bx = SkillFactory.getSkill(5100013);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                }

                bx = SkillFactory.getSkill(5200007); // Typhoon Crush
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5210013); // Fullmetal Jacket
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    critical_rate += bx.getEffect(bof).getCr();
                }

                bx = SkillFactory.getSkill(5210012);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localmaxhp_ += eff.getMaxHpX();
                    localmaxmp_ += eff.getMaxMpX();
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
                }

                bx = SkillFactory.getSkill(5220020); // Majestic Presence
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                }

                bx = SkillFactory.getSkill(5200009); // Physical Training
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                break;
            }
            case 130:
            case 131:
            case 132: {
                bx = SkillFactory.getSkill(1300009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    damageIncrease.put(1001004, eff.getX());
                    damageIncrease.put(1001005, eff.getY());
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(1310009);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverProp += eff.getProp();
                    hpRecoverPercent += eff.getX();
                }
                bx = SkillFactory.getSkill(1320006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    dam_r *= (eff.getDamage() + 100.0) / 100.0;
                    bossdam_r *= (eff.getDamage() + 100.0) / 100.0;
                }
                break;
            }
            case 1400:
            case 1410:
            case 1411:
            case 1412: {
                bx = SkillFactory.getSkill(14110003);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    RecoveryUP += eff.getX() - 100;
                    BuffUP += eff.getY() - 100;
                }
                break;
            }
        }
        if (GameConstants.isResist(chra.getJob())) {
            bx = SkillFactory.getSkill(30000002);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                RecoveryUP += bx.getEffect(bof).getX() - 100;
            }
        }
        return new Pair<>(localmaxhp_, localmaxmp_);
    }

    private Pair<Long, Long> handleBuffStats(MapleCharacter chra) {
        long localmaxhp_ = 0, localmaxmp_ = 0;
        MapleStatEffect eff = chra.getBuffedEffect(MapleBuffStat.RideVehicle);
        if (eff != null && eff.getSourceId() == 33001001) { // jaguar
            percent_hp += eff.getZ();
        }
        Integer buff = chra.getBuffedValue(MapleBuffStat.DiceRoll);
        if (buff != null) {
            percent_wdef += GameConstants.getDiceStat(buff.intValue(), 2);
            percent_mdef += GameConstants.getDiceStat(buff.intValue(), 2);
            percent_hp += GameConstants.getDiceStat(buff.intValue(), 3);
            percent_mp += GameConstants.getDiceStat(buff.intValue(), 3);
            dam_r *= (GameConstants.getDiceStat(buff.intValue(), 5) + 100.0) / 100.0;
            bossdam_r *= (GameConstants.getDiceStat(buff.intValue(), 5) + 100.0) / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieHpR);
        if (buff != null) {
            percent_hp += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMpR);
        if (buff != null) {
            percent_mp += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieCr);
        if (buff != null) {
            critical_rate += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.Asr);
        if (buff != null) {
            ASR += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.Ter);
        if (buff != null) {
            TER += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.Infinity);
        if (buff != null) {
            percent_matk += buff.intValue() - 1;
        }

        buff = chra.getBuffedValue(MapleBuffStat.MaxHP);
        if (buff != null) {
            percent_hp += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.MaxMP);
        if (buff != null) {
            percent_mp += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.STR);
        if (buff != null) {
            localstr += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.DEX);
        if (buff != null) {
            localdex += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.INT);
        if (buff != null) {
            localint_ += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.LUK);
        if (buff != null) {
            localluk += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieAllStat);
        if (buff != null) {
            localstr += buff.intValue();
            localdex += buff.intValue();
            localint_ += buff.intValue();
            localluk += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.EnhancedPdd);
        if (buff != null) {
            def += buff.intValue();
            def += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.Pdd);
        if (buff != null) {
            def += buff.intValue();
            def += buff.intValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.BasicStatUp);
        if (buff != null) {
            final double d = buff.doubleValue() / 100.0;
            localstr += d * str; //base only
            localdex += d * dex;
            localluk += d * luk;
            localint_ += d * int_;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MaxLevelBuff);
        if (buff != null) {
            final double d = buff.doubleValue() / 100.0;
            watk += (int) (watk * d);
            magic += (int) (magic * d);
        }
        buff = chra.getBuffedValue(MapleBuffStat.AranCombo);
        if (buff != null) {
            watk += buff.intValue() / 10;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MesoGuard);
        if (buff != null) {
            MesoGuardMeso += buff.doubleValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.ExpBuffRate);
        if (buff != null) {
            expBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.DropItemRate);
        if (buff != null) {
            mesoBuff += buff.doubleValue();
            dropBuff += buff.doubleValue();
        }

        if (chra.getSkillLevel(80000545) > 0) {
            mesoBuff += 40.0;
            dropBuff += 40.0;
        }

        buff = chra.getBuffedValue(MapleBuffStat.MesoUpByItem);
        if (buff != null) {
            mesoBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.ItemUpByItem);
        if (buff != null) {
            dropBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.WealthOfUnion);
        if (buff != null) {
            mesoBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.LuckOfUnion);
        if (buff != null) {
            dropBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.DropRate);
        if (buff != null) {
            dropBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.MesoUp);
        if (buff != null) {
            mesoBuff += buff.doubleValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.Acc);
        if (buff != null) {
            accuracy += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.IndieAcc);
        if (buff != null) {
            accuracy += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.IndiePad);
        if (buff != null) {
            watk += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.IndieMad);
        if (buff != null) {
            magic += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.IndiePadR);
        if (buff != null) {
            watk += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.EnhancedPad);
        if (buff != null) {
            watk += buff.intValue();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.EnergyCharged);
        if (eff != null) {
            watk += eff.getWatk();
            accuracy += eff.getAcc();
        }

        buff = chra.getBuffedValue(MapleBuffStat.IndieMadR);
        if (buff != null) {
            magic += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.Speed);
        if (buff != null) {
            speed += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.Jump);
        if (buff != null) {
            jump += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.DashSpeed);
        if (buff != null) {
            speed += buff.intValue();
        }

        buff = chra.getBuffedValue(MapleBuffStat.DashJump);
        if (buff != null) {
            jump += buff.intValue();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.HiddenPieceOn);
        if (eff != null) {
            ASR = 100;
            wdef += eff.getX();
            mdef += eff.getX();
            watk += eff.getX();
            magic += eff.getX();
        }

        buff = chra.getBuffedValue(MapleBuffStat.IndieDamR);
        if (buff != null) {
            dam_r *= (buff.doubleValue() + 100.0) / 100.0;
            bossdam_r *= (buff.doubleValue() + 100.0) / 100.0;
        }

        buff = chra.getBuffedSkill_Y(MapleBuffStat.FinalCut);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }

        eff = chra.getBuffedEffect(MapleBuffStat.Bless);
        if (eff != null) {
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getV();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.AdvancedBless);
        if (eff != null) {
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getV();
            mpconReduce += eff.getMPConReduce();
            localmaxhp_ += eff.getIndieMHp();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.ComboCounter);
        buff = chra.getBuffedValue(MapleBuffStat.ComboCounter);
        if (eff != null && buff != null) {
            dam_r *= ((100.0 + ((eff.getV() + eff.getDAMRate()) * (buff.intValue() - 1))) / 100.0);
            bossdam_r *= ((100.0 + ((eff.getV() + eff.getDAMRate()) * (buff.intValue() - 1))) / 100.0);
        }

        eff = chra.getBuffedEffect(MapleBuffStat.IndieSummon);
        if (eff != null) {
            if (eff.getSourceId() == 35121010) { //amp
                dam_r *= (eff.getX() + 100.0) / 100.0;
                bossdam_r *= (eff.getX() + 100.0) / 100.0;
            }
        }

        eff = chra.getBuffedEffect(MapleBuffStat.Beholder);
        if (eff != null) {
            trueMastery += eff.getMastery();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.WeaponCharge);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
        }

        eff = chra.getBuffedEffect(MapleBuffStat.PickPocket);
        if (eff != null) {
            pickRate = eff.getProp();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.IndieDamR);
        if (eff != null) {
            dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
            bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
        }

        eff = chra.getBuffedEffect(MapleBuffStat.WindWalk);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }

        eff = chra.getBuffedEffect(MapleBuffStat.BlessingArmor);
        if (eff != null) {
            watk += eff.getEnhancedWatk();
        }

        buff = chra.getBuffedSkill_Y(MapleBuffStat.DarkSight);
        if (buff != null) {
            dam_r *= (buff.intValue() + 100.0) / 100.0;
            bossdam_r *= (buff.intValue() + 100.0) / 100.0;
        }

        buff = chra.getBuffedSkill_X(MapleBuffStat.CombatOrders);
        if (buff != null) {
            combatOrders += buff.intValue();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.SharpEyes);
        if (eff != null) {
            critical_rate += eff.getX();
            critical_damage += eff.getY();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.BullsEye);
        if (eff != null) {
            critical_rate += eff.getX();
            critical_damage += eff.getY();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.CriticalGrowing);
        if (eff != null) {
            critical_rate += eff.getX();
            critical_damage += eff.getW();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.CriticalIncrease);
        if (eff != null) {
            critical_rate += eff.getX();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.EnrageCr);
        if (eff != null) {
            critical_rate += eff.getZ();
        }

        eff = chra.getBuffedEffect(MapleBuffStat.JaguarCount);
        if (eff != null) {
            critical_rate += eff.getY();
        }

        eff = chra.getBuffedEffect(60001217);
        if (eff != null) {
            critical_rate += eff.getCr();
            if (chra.getSkillLevel(61100008) > 0) {
                eff = SkillFactory.getSkill(61100008).getEffect(chra.getSkillLevel(61100008));
                critical_rate += eff.getCr();
            }

            if (chra.getSkillLevel(61110010) > 0) {
                eff = SkillFactory.getSkill(61110010).getEffect(chra.getSkillLevel(61110010));
                critical_rate += eff.getCr();
            }

            if (chra.getSkillLevel(61120013) > 0) {
                eff = SkillFactory.getSkill(61120013).getEffect(chra.getSkillLevel(61120013));
                critical_rate += eff.getCr();
            }
        }

        eff = chra.getBuffedEffect(MapleBuffStat.FlipTheCoin);
        if (eff != null) {
            critical_rate += eff.getX();
        }

        if (chra.getBuffedEffect(MapleBuffStat.MHPCutR) != null) {
            percent_hp -= chra.getBuffedValue(MapleBuffStat.MHPCutR);
        }

        if (chra.getBuffedEffect(MapleBuffStat.MMPCutR) != null) {
            percent_mp -= chra.getBuffedValue(MapleBuffStat.MMPCutR);
        }
        if (jump > 123) {
            jump = 123;
        }
        buff = chra.getBuffedValue(MapleBuffStat.RideVehicle);
        if (buff != null) {
            jump = 120;
            switch (buff.intValue()) {
                case 1:
                    speed = 150;
                    break;
                case 2:
                    speed = 170;
                    break;
                case 3:
                    speed = 180;
                    break;
                default:
                    speed = 200; //lol
                    break;
            }
        }
        return new Pair<>(localmaxhp_, localmaxmp_);
    }

    public boolean checkEquipLevels(final MapleCharacter chr, long gain) {
        boolean changed = false;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Equip> all = new ArrayList<>(equipLevelHandling);
        for (Equip eq : all) {
            int lvlz = eq.getEquipLevel();
            eq.setItemEXP((int) (eq.getItemEXP() + gain));

            if (eq.getEquipLevel() > lvlz) { //lvlup
                for (int i = eq.getEquipLevel() - lvlz; i > 0; i--) {
                    //now for the equipment increments...
                    final Map<Integer, Map<String, Integer>> inc = ii.getEquipIncrements(eq.getItemId());
                    if (inc != null && inc.containsKey(lvlz + i)) { //flair = 1
                        eq = ii.levelUpEquip(eq, inc.get(lvlz + i));
                    }
                    //UGH, skillz
//                    if (GameConstants.getStatFromWeapon(eq.getItemId()) == null && GameConstants.getMaxLevel(eq.getItemId()) < (lvlz + i) && Math.random() < 0.1 && eq.getIncSkill() <= 0 && ii.getEquipSkills(eq.getItemId()) != null) {
                    if (ii.getEquipSkills(eq.getItemId()) != null) {
                        for (int zzz : ii.getEquipSkills(eq.getItemId())) {
                            final Skill skil = SkillFactory.getSkill(zzz);
                            if (skil != null && skil.canBeLearnedBy(chr)) { //dont go over masterlevel :D
                                eq.setIncSkill(skil.getId());
                                chr.dropMessage(5, "Your skill has gained a levelup: " + skil.getName() + " +1");
                            }
                        }
                    }
//                    }
                }
                changed = true;
            }
            chr.forceReAddItem(eq.copy(), MapleInventoryType.EQUIPPED);
        }
        if (changed) {
            chr.equipChanged();
            chr.getClient().getSession().writeAndFlush(EffectPacket.showNormalEffect(chr, 21, true));
            chr.getMap().broadcastMessage(chr, EffectPacket.showNormalEffect(chr, 21, false), false);
        }
        return changed;
    }

    public boolean checkEquipDurabilitys(final MapleCharacter chr, int gain) {
        return checkEquipDurabilitys(chr, gain, false);
    }

    public boolean checkEquipDurabilitys(final MapleCharacter chr, int gain, boolean aboveZero) {
        if (chr.inPVP()) {
            return true;
        }
        List<Equip> all = new ArrayList<>(durabilityHandling);
        for (Equip item : all) {
            if (item != null && ((item.getPosition() >= 0) == aboveZero)) {
                item.setDurability(item.getDurability() + gain);
                if (item.getDurability() < 0) { //shouldnt be less than 0
                    item.setDurability(0);
                }
            }
        }
        for (Equip eqq : all) {
            if (eqq != null && eqq.getDurability() == 0 && eqq.getPosition() < 0) { //> 0 went to negative
                if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    chr.getClient().getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    chr.getClient().getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                durabilityHandling.remove(eqq);
                final short pos = chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
                MapleInventoryManipulator.unequip(chr.getClient(), eqq.getPosition(), pos, MapleInventoryType.EQUIP);  //치장여기도해야됨?..
            } else if (eqq != null) {
                chr.forceReAddItem(eqq.copy(), MapleInventoryType.EQUIPPED);
            }
        }
        return true;
    }

    public final void handleProfessionTool(final MapleCharacter chra) {
        if (chra.getProfessionLevel(92000000) > 0 || chra.getProfessionLevel(92010000) > 0) {
            synchronized (chra.getInventory(MapleInventoryType.EQUIP)) {
                final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIP).newList().iterator();
                while (itera.hasNext()) { //goes to first harvesting tool and stops
                    final Equip equip = (Equip) itera.next();
                    if (equip.getDurability() != 0 && (equip.getItemId() / 10000 == 150 && chra.getProfessionLevel(92000000) > 0) || (equip.getItemId() / 10000 == 151 && chra.getProfessionLevel(92010000) > 0)) {
                        if (equip.getDurability() > 0) {
                            durabilityHandling.add(equip);
                        }
                        harvestingTool = equip.getPosition();
                        break;
                    }
                }
            }
        }
    }

    private void CalcPassive_Mastery(final MapleCharacter player) {
        if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11) == null) {
            passive_mastery = 0;
            return;
        }
        final int skil;
        final MapleWeaponType weaponType = GameConstants.getWeaponType(player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId());
        boolean acc = true;
        switch (weaponType) {
            case BOW:
                skil = GameConstants.isKOC(player.getJob()) ? 13100000 : 3100000;
                break;
            case CLAW:
                skil = 4100000;
                break;
            case CANE:
                skil = player.getTotalSkillLevel(24120006) > 0 ? 24120006 : 24100004;
                break;
            case HANDCANNON:
                skil = 5300005;
                break;
            case KATARA:
            case DAGGER:
                skil = player.getJob() >= 430 && player.getJob() <= 434 ? 4300000 : 4200000;
                break;
            case CROSSBOW:
                skil = GameConstants.isResist(player.getJob()) ? 33100000 : 3200000;
                break;
            case AXE1H:
            case BLUNT1H:
                skil = GameConstants.isResist(player.getJob()) ? 31100004 : (GameConstants.isKOC(player.getJob()) ? 11100000 : (player.getJob() > 112 ? 1200000 : 1100000)); //hero/pally
                break;
            case AXE2H:
            case SWORD1H:
            case SWORD2H:
            case BLUNT2H:
                skil = GameConstants.isKOC(player.getJob()) ? 11100000 : (player.getJob() > 112 ? 1200000 : 1100000); //hero/pally
                break;
            case POLE_ARM:
                skil = GameConstants.isAran(player.getJob()) ? 21100000 : 1300000;
                break;
            case SPEAR:
                skil = 1300000;
                break;
            case KNUCKLE:
                skil = GameConstants.isKOC(player.getJob()) ? 15100001 : 5100001;
                break;
            case GUN:
                skil = GameConstants.isResist(player.getJob()) ? 35100000 : 5200000;
                break;
            case DUAL_BOW:
                skil = 23100005;
                break;
            case WAND:
            case STAFF:
                acc = false;
                skil = GameConstants.isResist(player.getJob()) ? 32100006 : (player.getJob() <= 212 ? 2100006 : (player.getJob() <= 222 ? 2200006 : (player.getJob() <= 232 ? 2300006 : (player.getJob() <= 2000 ? 12100007 : 22120002))));
                break;
            default:
                passive_mastery = 0;
                return;

        }
        if (player.getSkillLevel(skil) <= 0) {
            passive_mastery = 0;
            return;
        }
        final MapleStatEffect eff = SkillFactory.getSkill(skil).getEffect(player.getTotalSkillLevel(skil));
        if (acc) {
            accuracy += eff.getX();
            if (skil == 35100000) {
                watk += eff.getX();
            }
        } else {
            magic += eff.getX();
        }
        passive_mastery = (byte) eff.getMastery(); //after bb, simpler?
        trueMastery += eff.getMastery() + weaponType.getBaseMastery();
    }

    private void calculateFame(final MapleCharacter player) {
        player.getTrait(MapleTraitType.charm).addLocalExp(player.getFame());
        for (MapleTraitType t : MapleTraitType.values()) {
            player.getTrait(t).recalcLevel();
        }
    }

    public final byte passive_mastery() {
        return passive_mastery; //* 5 + 10 for mastery %
    }

    public int calculateMinBaseDamage(MapleCharacter player) {
        int minbasedamage = 0;
        int atk = player.getStat().getTotalWatk();
        if (atk == 0) {
            minbasedamage = 1;
        } else {
            Item weapon_item = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) - 11);
            if (weapon_item != null) {
                MapleWeaponType weapon = GameConstants.getWeaponType(weapon_item.getItemId());
                //mastery start
                if (player.getJob() == 110) {
                    skil = SkillFactory.getSkill(1100000);
                    skill = player.getSkillLevel(skil);
                    if (skill > 0) {
                        sword = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                    } else {
                        sword = 0.1;
                    }
                } else {
                    skil = SkillFactory.getSkill(1200000);
                    skill = player.getSkillLevel(skil);
                    if (skill > 0) {
                        sword = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                    } else {
                        sword = 0.1;
                    }
                }
                skil = SkillFactory.getSkill(1100001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    axe = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    axe = 0.1;
                }
                skil = SkillFactory.getSkill(1200001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    blunt = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    blunt = 0.1;
                }
                skil = SkillFactory.getSkill(1300000);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    spear = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    spear = 0.1;
                }
                skil = SkillFactory.getSkill(1300001);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    polearm = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    polearm = 0.1;
                }
                skil = SkillFactory.getSkill(3200000);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    CROSSBOW = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    CROSSBOW = 0.1;
                }
                skil = SkillFactory.getSkill(3100000);
                skill = player.getSkillLevel(skil);
                if (skill > 0) {
                    bow = ((skil.getEffect(player.getSkillLevel(skil)).getMastery() * 5 + 10) / 100);
                } else {
                    bow = 0.1;
                }
                //end mastery
                if (weapon == MapleWeaponType.CROSSBOW) {
                    minbasedamage = (int) (localdex * 0.9 * 3.6 * CROSSBOW + localstr) / 100 * (atk + 15);
                }
                if (weapon == MapleWeaponType.BOW) {
                    minbasedamage = (int) (localdex * 0.9 * 3.4 * bow + localstr) / 100 * (atk + 15);
                }
                if (player.getJob() == 400 && (weapon == MapleWeaponType.DAGGER)) {
                    minbasedamage = (int) (localluk * 0.9 * 3.6 * dagger + localstr + localdex) / 100 * atk;
                }
                if (player.getJob() != 400 && (weapon == MapleWeaponType.DAGGER)) {
                    minbasedamage = (int) (localstr * 0.9 * 4.0 * dagger + localdex) / 100 * atk;
                }
                if (player.getJob() == 400 && (weapon == MapleWeaponType.CLAW)) {
                    minbasedamage = (int) (localluk * 0.9 * 3.6 * claw + localstr + localdex) / 100 * (atk + 15);
                }
                if (weapon == MapleWeaponType.SPEAR) {
                    minbasedamage = (int) (localstr * 0.9 * 3.0 * spear + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.POLE_ARM) {
                    minbasedamage = (int) (localstr * 0.9 * 3.0 * polearm + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.SWORD1H) {
                    minbasedamage = (int) (localstr * 0.9 * 4.0 * sword + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.SWORD2H) {
                    minbasedamage = (int) (localstr * 0.9 * 4.6 * sword + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.AXE1H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.2 * axe + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.BLUNT1H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.2 * blunt + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.AXE2H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.4 * axe + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.BLUNT2H) {
                    minbasedamage = (int) (localstr * 0.9 * 3.4 * blunt + localdex) / 100 * atk;
                }
                if (weapon == MapleWeaponType.STAFF || weapon == MapleWeaponType.WAND) {
                    minbasedamage = (int) (localstr * 0.9 * 3.0 * staffwand + localdex) / 100 * atk;
                }
            }
        }
        return minbasedamage;
    }

    public final float calculateMaxBaseDamage(final int watk) { //?뚣끉??? PVP 甕???? ??沃섎챷?
        final MapleCharacter chra = chr.get();
        if (chra == null) {
            return 0;
        }
        float maxbasedamage;
        if (watk == 0) {
            maxbasedamage = 1;
        } else {
            final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            final int job = chra.getJob();
            final MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.NOT_A_WEAPON : GameConstants.getWeaponType(weapon_item.getItemId());
            int mainstat, secondarystat;

            switch (weapon) {
                case BOW:
                case CROSSBOW:
                    mainstat = localdex;
                    secondarystat = localstr;
                    break;
                case CLAW:
                case DAGGER:
                case KATARA:
                    if ((job >= 400 && job <= 434) || (job >= 1400 && job <= 1412)) {
                        mainstat = localluk;
                        secondarystat = localdex + localstr;
                    } else { // Non Thieves
                        mainstat = localstr;
                        secondarystat = localdex;
                    }
                    break;
                case KNUCKLE:
                    mainstat = localstr;
                    secondarystat = localdex;
                    break;
                case GUN:
                    mainstat = localdex;
                    secondarystat = localstr;
                    break;
                case NOT_A_WEAPON:
                    if ((job >= 500 && job <= 522) || (job >= 1500 && job <= 1512) || (job >= 3500 && job <= 3512)) {
                        mainstat = localstr;
                        secondarystat = localdex;
                    } else {
                        mainstat = 0;
                        secondarystat = 0;
                    }
                    break;
                default:
                    mainstat = localstr;
                    secondarystat = localdex;
                    break;
            }
            maxbasedamage = ((weapon.getMaxDamageMultiplier() * mainstat) + secondarystat) * watk / 100;
        }
        return maxbasedamage;
    }

    public final void calculateMaxBaseDamage(final int watk, final int pvpDamage, MapleCharacter chra) {
        if (watk <= 0) {
            localmaxbasedamage = 1;
            localmaxbasepvpdamage = 1;
        } else {
            final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            final Item weapon_item2 = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            final int job = chra.getJob();
            final MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.NOT_A_WEAPON : GameConstants.getWeaponType(weapon_item.getItemId());
            final MapleWeaponType weapon2 = weapon_item2 == null ? MapleWeaponType.NOT_A_WEAPON : GameConstants.getWeaponType(weapon_item2.getItemId());
            int mainstat, secondarystat, mainstatpvp, secondarystatpvp;
            final boolean mage = (job >= 200 && job <= 232) || (job >= 1200 && job <= 1212) || (job >= 2200 && job <= 2218) || (job >= 3200 && job <= 3212);
            switch (weapon) {
                case BOW:
                case CROSSBOW:
                case GUN:
                    mainstat = localdex;
                    secondarystat = localstr;
                    mainstatpvp = dex;
                    secondarystatpvp = str;
                    break;
                case DAGGER:
                case KATARA:
                case CLAW:
                case CANE:
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                    mainstatpvp = luk;
                    secondarystatpvp = dex + str;
                    break;
                default:
                    if (mage) {
                        mainstat = localint_;
                        secondarystat = localluk;
                        mainstatpvp = int_;
                        secondarystatpvp = luk;
                    } else {
                        mainstat = localstr;
                        secondarystat = localdex;
                        mainstatpvp = str;
                        secondarystatpvp = dex;
                    }
                    break;
            }
            localmaxbasepvpdamage = weapon.getMaxDamageMultiplier() * (4 * mainstatpvp + secondarystatpvp) * (100.0f + (pvpDamage / 100.0f));
            localmaxbasepvpdamageL = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (100.0f + (pvpDamage / 100.0f));
            if (weapon2 != MapleWeaponType.NOT_A_WEAPON && weapon_item != null && weapon_item2 != null) {
                Equip we1 = (Equip) weapon_item;
                Equip we2 = (Equip) weapon_item2;
                localmaxbasedamage = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * ((watk - (mage ? we2.getMatk() : we2.getWatk())) / 100.0f);
                localmaxbasedamage += weapon2.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * ((watk - (mage ? we1.getMatk() : we1.getWatk())) / 100.0f);
            } else {
                localmaxbasedamage = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (watk / 100.0f);
            }
        }
    }

    public final float getHealHP() {
        return shouldHealHP;
    }

    public final float getHealMP() {
        return shouldHealMP;
    }

    public final void relocHeal(MapleCharacter chra) {
        final int playerjob = chra.getJob();

        shouldHealHP = 10 + recoverHP; // Reset
        shouldHealMP = GameConstants.isDemonSlayer(chra.getJob()) ? 0 : (3 + mpRestore + recoverMP + (localint_ / 10)); // i think
        mpRecoverTime = 0;
        hpRecoverTime = 0;
        if (playerjob == 111 || playerjob == 112) {
            final Skill effect = SkillFactory.getSkill(1110000); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                MapleStatEffect eff = effect.getEffect(lvl);
                if (eff.getHp() > 0) {
                    shouldHealHP += eff.getHp();
                    hpRecoverTime = 4000;
                }
                shouldHealMP += eff.getMp();
                mpRecoverTime = 4000;
            }

        } else if (playerjob == 1111 || playerjob == 1112) {
            final Skill effect = SkillFactory.getSkill(11110000); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealMP += effect.getEffect(lvl).getMp();
                mpRecoverTime = 4000;
            }
        } else if (GameConstants.isMercedes(playerjob)) {
            final Skill effect = SkillFactory.getSkill(20020109); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealHP += (effect.getEffect(lvl).getX() * localmaxhp) / 100;
                hpRecoverTime = 4000;
                shouldHealMP += (effect.getEffect(lvl).getX() * localmaxmp) / 100;
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 3111 || playerjob == 3112) {
            final Skill effect = SkillFactory.getSkill(31110009); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealMP += effect.getEffect(lvl).getY();
                mpRecoverTime = 4000;
            }
        }
        if (chra.getChair() != 0) { // Is sitting on a chair.
            shouldHealHP += 99; // Until the values of Chair heal has been fixed,
            shouldHealMP += 99; // MP is different here, if chair data MP = 0, heal + 1.5
        } else if (chra.getMap() != null) { // Because Heal isn't multipled when there's a chair :)
            final float recvRate = chra.getMap().getRecoveryRate();
            if (recvRate > 0) {
                shouldHealHP *= recvRate;
                shouldHealMP *= recvRate;
            }
        }
    }

    public final void connectData(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(str); // str
        mplew.writeShort(dex); // dex
        mplew.writeShort(int_); // int
        mplew.writeShort(luk); // luk

        mplew.writeInt(hp); // hp -- INT after bigbang
        mplew.writeInt(maxhp); // maxhp
        mplew.writeInt(mp); // mp
        mplew.writeInt(maxmp); // maxmp
    }
    private final static int[] allJobs = {0, 10000, 10000000, 20000000, 20010000, 20020000, 30000000, 30010000};
    public final static int[] pvpSkills = {1000007, 2000007, 3000006, 4000010, 5000006, 5010004, 11000006, 12000006, 13000005, 14000006, 15000005, 21000005, 22000002, 23000004, 31000005, 32000012, 33000004, 35000005};

    public static int getSkillByJob(final int skillID, final int job) {
        if (GameConstants.isKOC(job)) {
            return skillID + 10000000;
        } else if (GameConstants.isAran(job)) {
            return skillID + 20000000;
        } else if (GameConstants.isEvan(job)) {
            return skillID + 20010000;
        } else if (GameConstants.isMercedes(job)) {
            return skillID + 20020000;
        } else if (GameConstants.isPhantom(job)) {
            return skillID + 20030000;
        } else if (GameConstants.isLuminous(job)) {
            return skillID + 20040000;
        } else if (GameConstants.isEunWol(job)) {
            return skillID + 20050000;
        } else if (GameConstants.isResist(job)) {
            return skillID + 30000000;
        } else if (GameConstants.isDemonSlayer(job)) {
            return skillID + 30010000;
        } else if (GameConstants.isXenon(job)) {
            return skillID + 30020000;
        } else if (GameConstants.isMichael(job)) {
            return skillID + 50000000;
        } else if (GameConstants.isKaiser(job)) {
            return skillID + 60000000;
        } else if (GameConstants.isAngelicBuster(job)) {
            return skillID + 60010000;
        } else if (GameConstants.isKadena(job)) {
            return skillID + 60020000;
        } else if (GameConstants.isKain(job)) {
            return skillID + 60030000;
        } else if (GameConstants.isZero(job)) {
            return skillID + 100000000;
        } else if (GameConstants.isIllium(job)) {
            return skillID + 150000000;
        } else if (GameConstants.isArk(job)) {
            return skillID + 150010000;
        } else if (GameConstants.isAdele(job)) {
            return skillID + 150020000;
        } else if (GameConstants.isHoyeong(job)) {
            return skillID + 160000000;
        } else if (GameConstants.isLala(job)) {
            return skillID + 160010000;
        }
        return skillID;
    }

    public final int getSkillIncrement(final int skillID) {
        if (skillsIncrement.containsKey(skillID)) {
            return skillsIncrement.get(skillID);
        }
        return 0;
    }

    public final int getElementBoost(final Element key) {
        if (elemBoosts.containsKey(key)) {
            return elemBoosts.get(key);
        }
        return 0;
    }

    public final int getDamageIncrease(final int key) {
        if (damageIncrease.containsKey(key)) {
            return damageIncrease.get(key) + damX;
        }
        return damX;
    }

    public final int getAccuracy() {
        return accuracy;
    }

    public void heal_noUpdate(MapleCharacter chra) {
        setHp(getCurrentMaxHp(), chra);
        setMp(getCurrentMaxMp(chra), chra);
    }

    public void heal(MapleCharacter chra) {
        heal_noUpdate(chra);
        chra.updateSingleStat(MapleStat.HP, getCurrentMaxHp());
        chra.updateSingleStat(MapleStat.MP, getCurrentMaxMp(chra));
    }

    public Pair<Long, Long> handleEquipAdditions(MapleItemInformationProvider ii, MapleCharacter chra, boolean first_login, Map<Skill, SkillEntry> sData, final int itemId) {
        final List<Triple<String, String, String>> additions = ii.getEquipAdditions(itemId);
        final Map<Integer, Map<String, Integer>> addz = ii.getEquipIncrements(itemId);
        int skillid = 0, skilllevel = 0;
        long localmaxhp_a = 0, localmaxmp_a = 0;
        String craft, job, level;
        if (additions != null) {
            for (final Triple<String, String, String> add : additions) {
                if (add.getMid().contains("con")) {
                    continue;
                }
                final int right = Integer.parseInt(add.getRight());

                switch (add.getLeft()) {
                    /*                case "elemboost":
                     craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                     if (add.getMid().equals("elemVol") && (craft == null || craft != null && chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
                     int value = Integer.parseInt(add.getRight().substring(1, add.getRight().length()));
                     final Element key = Element.getFromChar(add.getRight().charAt(0));
                     if (elemBoosts.get(key) != null) {
                     value += elemBoosts.get(key);
                     }
                     elemBoosts.put(key, value);
                     }
                     break;*/
                    case "mobcategory": //skip the category, thinkings too expensive to have yet another Map<Integer, Integer> for damage calculations
                        if (add.getMid().equals("damage")) {
                            dam_r *= (right + 100.0) / 100.0;
                            bossdam_r += (right + 100.0) / 100.0;
                        }
                        break;
                    case "critical": // lv critical lvl?
                        boolean canJob = false,
                         canLevel = false;
                        job = ii.getEquipAddReqs(itemId, add.getLeft(), "job");
                        if (job != null) {
                            if (job.contains(",")) {
                                final String[] jobs = job.split(",");
                                for (final String x : jobs) {
                                    if (chra.getJob() == Integer.parseInt(x)) {
                                        canJob = true;
                                    }
                                }
                            } else {
                                if (chra.getJob() == Integer.parseInt(job)) {
                                    canJob = true;
                                }
                            }
                        }
                        level = ii.getEquipAddReqs(itemId, add.getLeft(), "level");
                        if (level != null) {
                            if (chra.getLevel() >= Integer.parseInt(level)) {
                                canLevel = true;
                            }
                        }
                        if ((job != null && canJob || job == null) && (level != null && canLevel || level == null)) {
                            switch (add.getMid()) {
                                case "prop":
                                    critical_rate += right;
                                    break;
                                case "damage":
                                    critical_damage += right;
                                    break;
                            }
                        }
                        break;
                    case "boss": // ignore prob, just add
                        craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                        if (add.getMid().equals("damage") && (craft == null || craft != null && chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
                            bossdam_r *= (right + 100.0) / 100.0;
                        }
                        break;
                    case "mobdie": // lv, hpIncRatioOnMobDie, hpRatioProp, mpIncRatioOnMobDie, mpRatioProp, modify =D, don't need mob to die
                        craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                        if ((craft == null || craft != null && chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
                            switch (add.getMid()) {
                                case "hpIncOnMobDie":
                                    hpRecover += right;
                                    hpRecoverProp += 5;
                                    break;
                                case "mpIncOnMobDie":
                                    mpRecover += right;
                                    mpRecoverProp += 5;
                                    break;
                            }
                        }
                        break;
                    case "skill": // all these are additional skills
                        if (first_login) {
                            craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                            if ((craft == null || craft != null && chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
                                switch (add.getMid()) {
                                    case "id":
                                        skillid = right;
                                        break;
                                    case "level":
                                        skilllevel = right;
                                        break;
                                }
                            }
                        }
                        break;
                    case "hpmpchange":
                        switch (add.getMid()) {
                            case "hpChangerPerTime":
                                recoverHP += right;
                                break;
                            case "mpChangerPerTime":
                                recoverMP += right;
                                break;
                        }
                        break;
                    case "statinc":
                        boolean canJobx = false,
                         canLevelx = false;
                        job = ii.getEquipAddReqs(itemId, add.getLeft(), "job");
                        if (job != null) {
                            if (job.contains(",")) {
                                final String[] jobs = job.split(",");
                                for (final String x : jobs) {
                                    if (chra.getJob() == Integer.parseInt(x)) {
                                        canJobx = true;
                                    }
                                }
                            } else if (chra.getJob() == Integer.parseInt(job)) {
                                canJobx = true;
                            }
                        }
                        level = ii.getEquipAddReqs(itemId, add.getLeft(), "level");
                        if (level != null && chra.getLevel() >= Integer.parseInt(level)) {
                            canLevelx = true;
                        }
                        if ((!canJobx && job != null) || (!canLevelx && level != null)) {
                            continue;
                        }
                        if (itemId == 1142367) {
                            final int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                            if (day != 1 && day != 7) {
                                continue;
                            }
                        }
                        switch (add.getMid()) {
                            case "incPAD":
                                watk += right;
                                break;
                            case "incMAD":
                                magic += right;
                                break;
                            case "incSTR":
                                localstr += right;
                                break;
                            case "incDEX":
                                localdex += right;
                                break;
                            case "incINT":
                                localint_ += right;
                                break;
                            case "incLUK":
                                localluk += right;
                                break;
                            case "incJump":
                                jump += right;
                                break;
                            case "incMHP":
                                localmaxhp_a += right;
                                break;
                            case "incMHPr":
                                percent_hp += right;
                                break;
                            case "incMMP":
                                localmaxmp_a += right;
                                break;
                            case "incMMPr":
                                percent_mp += right;
                                break;
                            case "incPDD":
                                wdef += right;
                                break;
                            case "incMDD":
                                mdef += right;
                                break;
                            case "incACC":
                                accuracy += right;
                                break;
                            case "incEVA":
                                break;
                            case "incSpeed":
                                speed += right;
                                break;
                        }
                        break;
                }
            }
        }
        if (skillid != 0 && skilllevel != 0) {
            sData.put(SkillFactory.getSkill(skillid), new SkillEntry((byte) skilllevel, (byte) 0, -1));
        }
        return new Pair<>(localmaxhp_a, localmaxmp_a);
    }

    public void recalcPVPRank(MapleCharacter chra) {
        this.pvpRank = 10;
        this.pvpExp = chra.getTotalBattleExp();
        for (int i = 0; i < 10; i++) {
            if (pvpExp > GameConstants.getPVPExpNeededForLevel(i + 1)) {
                pvpRank--;
                pvpExp -= GameConstants.getPVPExpNeededForLevel(i + 1);
            }
        }
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / localmaxhp);
    }

    public int getMPPercent() {
        return (int) Math.ceil((mp * 100.0) / localmaxmp);
    }

    public int getForce(int room) {
        if (demonForce.containsKey(room)) {
            return demonForce.get(room);
        }
        return 0;
    }

    public final boolean isRangedJob(final int job) {
        if (GameConstants.isArcher(job) || GameConstants.isCannon(job) || GameConstants.isNightLord(job) || GameConstants.isCaptain(job) || GameConstants.isNightWalker(job) || GameConstants.isMechanic(job) || GameConstants.isAngelicBuster(job)) {
            return true;
        }
        return false;
    }

    public final int d(int variable) {
        return (int) Math.floor(variable);
    }

    public int getRandomage(MapleCharacter player) {
        int maxdamage = (int) localmaxbasedamage;
        int mindamage = calculateMinBaseDamage(player);
        return Randomizer.rand(mindamage, maxdamage);
    }

}
