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
 GNU Affero General Public License for more details.미ㅣ

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client.inventory;

import constants.GameConstants;
import server.MapleItemInformationProvider;
import server.Randomizer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Equip extends Item implements Serializable {

    public static enum ScrollResult {

        SUCCESS, FAIL, CURSE
    }
    public static final int ARMOR_RATIO = 350000;
    public static final int WEAPON_RATIO = 700000;
    //charm: -1 = has not been initialized yet, 0 = already been worn, >0 = has teh charm exp
    private byte state = 0, lines = 0, upgradeSlots = 0, level = 0, vicioushammer = 0, enhance = 0, reqLevel = 0, yggdrasilWisdom = 0, totalDamage = 0, allStat = 0, karmaCount = -1;
    private short str = 0, dex = 0, _int = 0, luk = 0, arc = 0, hp = 0, mp = 0, watk = 0, matk = 0, wdef = 0, mdef = 0, acc = 0, avoid = 0, hands = 0, speed = 0, jump = 0, charmExp = 0, pvpDamage = 0, bossDamage = 0, ignorePDR = 0, soulname = 0, soulenchanter = 0, soulpotential = 0, enchantBuff = 0;
    private short enchantStr = 0, enchantDex = 0, enchantInt = 0, enchantLuk = 0, enchantHp = 0, enchantMp = 0, enchantWdef = 0, enchantMdef = 0, enchantAcc = 0, enchantAvoid = 0, enchantWatk = 0, enchantMatk = 0;
    private int arcexp = 0, arclevel = 0, itemEXP = 0, durability = -1, incSkill = -1, potential1 = 0, potential2 = 0, potential3 = 0, potential4 = 0, potential5 = 0, potential6 = 0, soulskill = 0, moru = 0, equipmentType = 0x1100;
    private long fire = 0;
    private boolean finalStrike = false;
    private List<EquipStat> stats = new LinkedList<>();
    private List<EquipSpecialStat> specialStats = new LinkedList<>();

    public Equip(int id, short position, int flag) {
        super(id, position, (short) 1, flag);
    }

    public Equip(int id, short position, int uniqueid, int flag) {
        super(id, position, (short) 1, flag, uniqueid);
    }

    public void set(Equip set) {
        str = set.str;
        dex = set.dex;
        _int = set._int;
        luk = set.luk;
        arc = set.arc;
        arclevel = set.arclevel;
        arcexp = set.arcexp;
        hp = set.hp;
        mp = set.mp;
        matk = set.matk;
        mdef = set.mdef;
        watk = set.watk;
        wdef = set.wdef;
        acc = set.acc;
        avoid = set.avoid;
        hands = set.hands;
        speed = set.speed;
        jump = set.jump;
        enhance = set.enhance;
        upgradeSlots = set.upgradeSlots;
        level = set.level;
        itemEXP = set.itemEXP;
        durability = set.durability;
        vicioushammer = set.vicioushammer;
        potential1 = set.potential1;
        potential2 = set.potential2;
        potential3 = set.potential3;
        potential4 = set.potential4;
        potential5 = set.potential5;
        potential6 = set.potential6;
        charmExp = set.charmExp;
        pvpDamage = set.pvpDamage;
        incSkill = set.incSkill;
        enchantBuff = set.enchantBuff;
        reqLevel = set.reqLevel;
        yggdrasilWisdom = set.yggdrasilWisdom;
        finalStrike = set.finalStrike;
        bossDamage = set.bossDamage;
        ignorePDR = set.ignorePDR;
        totalDamage = set.totalDamage;
        allStat = set.allStat;
        karmaCount = set.karmaCount;
        soulname = set.soulname;
        soulenchanter = set.soulenchanter;
        soulpotential = set.soulpotential;
        soulskill = set.soulskill;
        stats = set.stats;
        specialStats = set.specialStats;
        state = set.state;
        lines = set.lines;
        fire = set.fire;
        moru = set.moru;
        enchantStr = set.enchantStr;
        enchantDex = set.enchantDex;
        enchantInt = set.enchantInt;
        enchantLuk = set.enchantLuk;
        enchantHp = set.enchantHp;
        enchantMp = set.enchantMp;
        enchantAcc = set.enchantAcc;
        enchantAvoid = set.enchantAvoid;
        enchantWatk = set.enchantWatk;
        enchantMatk = set.enchantMatk;
        enchantWdef = set.enchantWdef;
        enchantMdef = set.enchantMdef;
    }

    @Override
    public Item copy() {
        Equip ret = new Equip(getItemId(), getPosition(), getUniqueId(), getFlag());
        ret.str = str;
        ret.dex = dex;
        ret._int = _int;
        ret.luk = luk;
        ret.arc = arc;
        ret.arcexp = arcexp;
        ret.arclevel = arclevel;
        ret.hp = hp;
        ret.mp = mp;
        ret.matk = matk;
        ret.mdef = mdef;
        ret.watk = watk;
        ret.wdef = wdef;
        ret.acc = acc;
        ret.avoid = avoid;
        ret.hands = hands;
        ret.speed = speed;
        ret.jump = jump;
        ret.enhance = enhance;
        ret.upgradeSlots = upgradeSlots;
        ret.level = level;
        ret.itemEXP = itemEXP;
        ret.durability = durability;
        ret.vicioushammer = vicioushammer;
        ret.potential1 = potential1;
        ret.potential2 = potential2;
        ret.potential3 = potential3;
        ret.potential4 = potential4;
        ret.potential5 = potential5;
        ret.potential6 = potential6;
        ret.charmExp = charmExp;
        ret.pvpDamage = pvpDamage;
        ret.incSkill = incSkill;
        ret.enchantBuff = enchantBuff;
        ret.reqLevel = reqLevel;
        ret.yggdrasilWisdom = yggdrasilWisdom;
        ret.finalStrike = finalStrike;
        ret.bossDamage = bossDamage;
        ret.ignorePDR = ignorePDR;
        ret.totalDamage = totalDamage;
        ret.allStat = allStat;
        ret.karmaCount = karmaCount;
        ret.soulname = soulname;
        ret.soulenchanter = soulenchanter;
        ret.soulpotential = soulpotential;
        ret.setInventoryId(getInventoryId());
        ret.setGiftFrom(getGiftFrom());
        ret.setOwner(getOwner());
        ret.setQuantity(getQuantity());
        ret.setExpiration(getExpiration());
        ret.stats = stats;
        ret.specialStats = specialStats;
        ret.state = state;
        ret.lines = lines;
        ret.fire = fire;
        ret.soulskill = soulskill;
        ret.equipmentType = equipmentType;
        ret.moru = moru;
        ret.enchantStr = enchantStr;
        ret.enchantDex = enchantDex;
        ret.enchantInt = enchantInt;
        ret.enchantLuk = enchantLuk;
        ret.enchantHp = enchantHp;
        ret.enchantMp = enchantMp;
        ret.enchantAcc = enchantAcc;
        ret.enchantAvoid = enchantAvoid;
        ret.enchantWatk = enchantWatk;
        ret.enchantMatk = enchantMatk;
        ret.enchantWdef = enchantWdef;
        ret.enchantMdef = enchantMdef;
        return ret;
    }

    @Override
    public byte getType() {
        return 1;
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    public short getStr() {
        return str;
    }

    public short getDex() {
        return dex;
    }

    public short getInt() {
        return _int;
    }

    public short getLuk() {
        return luk;
    }

    public short getArc() {
        return arc;
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public short getWatk() {
        return watk;
    }

    public short getMatk() {
        return matk;
    }

    public short getWdef() {
        return wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public short getAcc() {
        return acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public short getHands() {
        return hands;
    }

    public short getSpeed() {
        return speed;
    }

    public short getJump() {
        return jump;
    }

    public void setStr(short str) {
        if (str < 0) {
            str = 0;
        }
        this.str = str;
    }

    public void addStr(short str) {
        if (this.str + str < 0) {
            this.str = 0;
        } else {
            this.str += str;
        }
    }

    public void setDex(short dex) {
        if (dex < 0) {
            dex = 0;
        }
        this.dex = dex;
    }

    public void addDex(short dex) {
        if (this.dex + dex < 0) {
            this.dex = 0;
        } else {
            this.dex += dex;
        }
    }

    public void setInt(short _int) {
        if (_int < 0) {
            _int = 0;
        }
        this._int = _int;
    }

    public void addInt(short dex) {
        if (this._int + dex < 0) {
            this._int = 0;
        } else {
            this._int += dex;
        }
    }

    public void setLuk(short luk) {
        if (luk < 0) {
            luk = 0;
        }
        this.luk = luk;
    }

    public void addLuk(short dex) {
        if (this.luk + dex < 0) {
            this.luk = 0;
        } else {
            this.luk += dex;
        }
    }

    public void setArc(short arc) {
        if (arc < 0) {
            arc = 0;
        }
        this.arc = arc;
    }

    public void setHp(short hp) {
        if (hp < 0) {
            hp = 0;
        }
        this.hp = hp;
    }

    public void addHp(short dex) {
        if (this.hp + dex < 0) {
            this.hp = 0;
        } else {
            this.hp += dex;
        }
    }

    public void setMp(short mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public void addMp(short dex) {
        if (this.mp + dex < 0) {
            this.mp = 0;
        } else {
            this.mp += dex;
        }
    }

    public void setWatk(short watk) {
        if (watk < 0) {
            watk = 0;
        }
        this.watk = watk;
    }

    public void addWatk(short watk) {
        if (this.watk + watk < 0) {
            this.watk = 0;
        } else {
            this.watk += watk;
        }
    }

    public void setMatk(short matk) {
        if (matk < 0) {
            matk = 0;
        }
        this.matk = matk;
    }

    public void addMatk(short watk) {
        if (this.matk + watk < 0) {
            this.matk = 0;
        } else {
            this.matk += watk;
        }
    }

    public void setWdef(short wdef) {
        if (wdef < 0) {
            wdef = 0;
        }
        this.wdef = wdef;
    }

    public void addWdef(short wdef) {
        if (wdef + this.wdef < 0) {
            this.wdef = 0;
        } else {
            this.wdef += wdef;
        }
    }

    public void setMdef(short mdef) {
        if (mdef < 0) {
            mdef = 0;
        }
        this.mdef = mdef;
    }

    public void addMdef(short mdef) {
        if (mdef + this.mdef < 0) {
            this.mdef = 0;
        } else {
            this.mdef += mdef;
        }
    }

    public void setAcc(short acc) {
        if (acc < 0) {
            acc = 0;
        }
        this.acc = acc;
    }

    public void addAcc(short acc) {
        if (acc + this.acc < 0) {
            this.acc = 0;
        } else {
            this.acc += acc;
        }
    }

    public void setAvoid(short avoid) {
        if (avoid < 0) {
            avoid = 0;
        }
        this.avoid = avoid;
    }

    public void setHands(short hands) {
        if (hands < 0) {
            hands = 0;
        }
        this.hands = hands;
    }

    public void setSpeed(short speed) {
        if (speed < 0) {
            speed = 0;
        }
        this.speed = speed;
    }

    public void addSpeed(short speed) {
        if (speed + this.speed < 0) {
            this.speed = 0;
        } else {
            this.speed += speed;
        }
    }

    public void addJump(short jump) {
        if (jump + this.jump < 0) {
            this.jump = 0;
        } else {
            this.jump += jump;
        }
    }

    public void setJump(short speed) {
        if (speed < 0) {
            speed = 0;
        }
        this.jump = speed;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public void addUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots += upgradeSlots;
    }

    public byte getLevel() {
        if (getItemId() >= 1113098 && getItemId() <= 1113128) {
            return 0;
        }
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getViciousHammer() {
        return vicioushammer;
    }

    public void setViciousHammer(byte ham) {
        vicioushammer = ham;
    }

    public int getItemEXP() {
        return itemEXP;
    }

    public void setItemEXP(int itemEXP) {
        if (itemEXP < 0) {
            itemEXP = 0;
        }
        this.itemEXP = itemEXP;
    }

    public int getEquipExp() {
        if (itemEXP <= 0) {
            return 0;
        }
        //aproximate value
        if (GameConstants.isWeapon(getItemId())) {
            return itemEXP / WEAPON_RATIO;
        } else {
            return itemEXP / ARMOR_RATIO;
        }
    }

    public int getEquipExpForLevel() {
        if (getEquipExp() <= 0) {
            return 0;
        }
        int expz = getEquipExp();
        for (int i = getBaseLevel(); i <= GameConstants.getMaxLevel(getItemId()); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else { //for 0, dont continue;
                break;
            }
        }
        return expz;
    }

    public int getExpPercentage() {
        if (getEquipLevel() < getBaseLevel() || getEquipLevel() > GameConstants.getMaxLevel(getItemId()) || GameConstants.getExpForLevel(getEquipLevel(), getItemId()) <= 0) {
            return 0;
        }
        return getEquipExpForLevel() * 100 / GameConstants.getExpForLevel(getEquipLevel(), getItemId());
    }

    public int getEquipLevel() {
        if (GameConstants.getMaxLevel(getItemId()) <= 0) {
            return 0;
        } else if (getEquipExp() <= 0) {
            return getBaseLevel();
        }
        int levelz = getBaseLevel();
        int expz = getEquipExp();
        for (int i = levelz; i <= GameConstants.getMaxLevel(getItemId()); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                levelz++;
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else { //for 0, dont continue;
                break;
            }
        }
        return levelz;
    }

    public int getBaseLevel() {
        if (getItemId() >= 1113098 && getItemId() <= 1113128) {
            return this.level;
        }
        return 0;
    }

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("Setting the quantity to " + quantity + " on an equip (itemid: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(final int dur) {
        this.durability = dur;
    }

    public byte getEnhance() {
        return enhance;
    }

    public void setEnhance(final byte en) {
        this.enhance = en;
    }

    public int getPotential1() {
        return potential1;
    }

    public void setPotential1(final int en) {
        this.potential1 = en;
    }

    public int getPotential2() {
        return potential2;
    }

    public void setPotential2(final int en) {
        this.potential2 = en;
    }

    public int getPotential3() {
        return potential3;
    }

    public void setPotential3(final int en) {
        this.potential3 = en;
    }

    public int getPotential4() {
        return potential4;
    }

    public void setPotential4(final int en) {
        this.potential4 = en;
    }

    public int getPotential5() {
        return potential5;
    }

    public void setPotential5(final int en) {
        this.potential5 = en;
    }

    public int getPotential6() {
        return potential6;
    }

    public void setPotential6(final int en) {
        this.potential6 = en;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public byte getLines() {
        return lines;
    }

    public void setLines(byte lines) {
        this.lines = lines;
    }

    public void resetPotential_Fuse(boolean half, int potentialState) { //maker skill - equip first receive
        //no legendary, 0.16% chance unique, 4% chance epic, else rare
        potentialState = -potentialState;
        if (Randomizer.nextInt(100) < 4) {
            potentialState -= Randomizer.nextInt(100) < 4 ? 2 : 1;
        }
        setPotential1(potentialState);
        setPotential2((Randomizer.nextInt(half ? 5 : 10) == 0 ? potentialState : 0)); //1/10 chance of 3 line
        setPotential3(0); //just set it theoretically
        setPotential4(0); //just set it theoretically
        setPotential5(0); //just set it theoretically
    }

    public void resetPotential() { //equip first one, scroll hidden on it
        //no legendary, 0.16% chance unique, 4% chance epic, else rare
        final int rank = Randomizer.nextInt(100) < 4 ? (Randomizer.nextInt(100) < 4 ? -19 : -18) : -17;
        setPotential1(rank);
        setPotential2((Randomizer.nextInt(10) == 0 ? rank : 0)); //1/10 chance of 3 line
        setPotential3(0); //just set it theoretically
        setPotential4(0); //just set it theoretically
        setPotential5(0); //just set it theoretically
    }

    public void renewPotential() {
        int epic = 7;
        int unique = 5;
        if (getState() == 17 && Randomizer.nextInt(100) <= epic) {
            setState((byte) 2);
            return;
        } else if (getState() == 18 && Randomizer.nextInt(100) <= unique) {
            setState((byte) 3);
            return;
        } else if (getState() == 19 && Randomizer.nextInt(100) <= 2) {
            setState((byte) 4);
            return;
        }
        setState((byte) (getState() - 16));
    }

    /*
     * 환생의 불꽃
     * 00 : STR
     * 10 : DEX
     * 20 : INT
     * 30 : LUK
     * 40 : STR + DEX
     * 50 : STR + INT
     * 60 : STR + LUK
     * 70 : DEX + INT
     * 80 : DEX + LUK
     * 90 : INT + LUK
     * 100 : MAXHP
     * 110 : MAXMP
     * 120 : WDEF
     * 130 : MDEF
     * 140 : ACC
     * 150 : AVOID
     * 160 : HANDS
     * 170 : WATK
     * 180 : MATK
     * 190 : SPEED
     * 200 : JUMP
     * 210 : BOSSDAMAGE
     * 220 : REQLEVEL
     * 230 : TOTALDAMAGE
     * 240 : ALLSTAT
     */
    public long getFire() {
        return fire;
    }

    public void setFire(long fire) {
        this.fire = fire;
    }

    public int getIncSkill() {
        return incSkill;
    }

    public void setIncSkill(int inc) {
        this.incSkill = inc;
    }

    public short getCharmEXP() {
        return charmExp;
    }

    public short getPVPDamage() {
        return pvpDamage;
    }

    public void setCharmEXP(short s) {
        this.charmExp = s;
    }

    public void setPVPDamage(short p) {
        this.pvpDamage = p;
    }

    public short getEnchantBuff() {
        return enchantBuff;
    }

    public void setEnchantBuff(short enchantBuff) {
        this.enchantBuff = enchantBuff;
    }

    public byte getReqLevel() {
        return reqLevel;
    }

    public void setReqLevel(byte reqLevel) {
        this.reqLevel = reqLevel;
    }

    public byte getYggdrasilWisdom() {
        return yggdrasilWisdom;
    }

    public void setYggdrasilWisdom(byte yggdrasilWisdom) {
        this.yggdrasilWisdom = yggdrasilWisdom;
    }

    public boolean getFinalStrike() {
        return finalStrike;
    }

    public void setFinalStrike(boolean finalStrike) {
        this.finalStrike = finalStrike;
    }

    public short getBossDamage() {
        return bossDamage;
    }

    public void setBossDamage(short bossDamage) {
        this.bossDamage = bossDamage;
    }

    public void addBossDamage(byte dmg) {
        this.bossDamage += dmg;
    }

    public short getIgnorePDR() {
        return ignorePDR;
    }

    public void setIgnorePDR(short ignorePDR) {
        this.ignorePDR = ignorePDR;
    }

    public void addIgnoreWdef(short ignorePDR) {
        this.ignorePDR += ignorePDR;
    }

    public byte getTotalDamage() {
        return totalDamage;
    }

    public void setTotalDamage(byte totalDamage) {
        this.totalDamage = totalDamage;
    }

    public void addTotalDamage(byte totalDamage) {
        this.totalDamage += totalDamage;
    }

    public byte getAllStat() {
        return allStat;
    }

    public void setAllStat(byte allStat) {
        this.allStat = allStat;
    }

    public void addAllStat(byte allStat) {
        this.allStat += allStat;
    }

    public byte getKarmaCount() {
        return karmaCount;
    }

    public void setKarmaCount(byte karmaCount) {
        this.karmaCount = karmaCount;
    }

    public short getSoulName() {
        return soulname;
    }

    public void setSoulName(final short soulname) {
        this.soulname = soulname;
    }

    public short getSoulEnchanter() {
        return soulenchanter;
    }

    public void setSoulEnchanter(final short soulenchanter) {
        this.soulenchanter = soulenchanter;
    }

    public short getSoulPotential() {
        return soulpotential;
    }

    public void setSoulPotential(final short soulpotential) {
        this.soulpotential = soulpotential;
    }

    public int getSoulSkill() {
        return soulskill;
    }

    public void setSoulSkill(final int skillid) {
        this.soulskill = skillid;
    }

    public List<EquipStat> getStats() {
        return stats;
    }

    public List<EquipSpecialStat> getSpecialStats() {
        return specialStats;
    }

    public static Equip calculateEquipStats(Equip eq) {
        eq.getStats().clear();
        eq.getSpecialStats().clear();
        if (eq.getUpgradeSlots() > 0) {
            eq.getStats().add(EquipStat.SLOTS);
        }
        if (eq.getLevel() > 0) {
            eq.getStats().add(EquipStat.LEVEL);
        }
        if (eq.getTotalStr() > 0) {
            eq.getStats().add(EquipStat.STR);
        }
        if (eq.getTotalDex() > 0) {
            eq.getStats().add(EquipStat.DEX);
        }
        if (eq.getTotalInt() > 0) {
            eq.getStats().add(EquipStat.INT);
        }
        if (eq.getTotalLuk() > 0) {
            eq.getStats().add(EquipStat.LUK);
        }
        if (eq.getTotalHp() > 0) {
            eq.getStats().add(EquipStat.MHP);
        }
        if (eq.getTotalMp() > 0) {
            eq.getStats().add(EquipStat.MMP);
        }
        if (eq.getTotalWatk() > 0) {
            eq.getStats().add(EquipStat.WATK);
        }
        if (eq.getTotalMatk() > 0) {
            eq.getStats().add(EquipStat.MATK);
        }
        if (eq.getTotalWdef() > 0) {
            eq.getStats().add(EquipStat.WDEF);
        }
        /*        if (eq.getMdef() > 0) {
         eq.getStats().add(EquipStat.MDEF);
         }
         if (eq.getAcc() > 0) {
         eq.getStats().add(EquipStat.ACC);
         }
         if (eq.getAvoid() > 0) {
         eq.getStats().add(EquipStat.AVOID);
         }*/
        if (eq.getHands() > 0) {
            eq.getStats().add(EquipStat.HANDS);
        }
        if (eq.getSpeed() > 0) {
            eq.getStats().add(EquipStat.SPEED);
        }
        if (eq.getJump() > 0) {
            eq.getStats().add(EquipStat.JUMP);
        }
        if (eq.getFlag() != 0) {
            eq.getStats().add(EquipStat.FLAG);
        }
        if (eq.getIncSkill() > 0) {
            eq.getStats().add(EquipStat.INC_SKILL);
        }
        if (eq.getEquipLevel() > 0 || eq.getBaseLevel() > 0) {
            eq.getStats().add(EquipStat.ITEM_LEVEL);
        }
        if (eq.getItemEXP() > 0) {
            eq.getStats().add(EquipStat.ITEM_EXP);
        }
        if (eq.getDurability() > -1) {
            eq.getStats().add(EquipStat.DURABILITY);
        }
        if (eq.getViciousHammer() > 0) {
            eq.getStats().add(EquipStat.VICIOUS_HAMMER);
        }
        if (eq.getPVPDamage() > 0) {
            eq.getStats().add(EquipStat.PVP_DAMAGE);
        }
        if (eq.getEnchantBuff() > 0) {
            eq.getStats().add(EquipStat.ENHANCT_BUFF);
        }
        if (eq.getReqLevel() > 0) {
            eq.getStats().add(EquipStat.REQUIRED_LEVEL);
        } else if (eq.getReqLevel() < 0) {
            eq.getStats().add(EquipStat.DOWNLEVEL);
        }
        if (eq.getYggdrasilWisdom() > 0) {
            eq.getStats().add(EquipStat.YGGDRASIL_WISDOM);
        }
        if (eq.getFinalStrike()) {
            eq.getStats().add(EquipStat.FINAL_STRIKE);
        }
        if (eq.getBossDamage() > 0) {
            eq.getStats().add(EquipStat.IndieBdr);
        }
        if (eq.getIgnorePDR() > 0) {
            eq.getStats().add(EquipStat.IGNORE_PDR);
        }

        //SPECIAL STATS:
        if (eq.getTotalDamage() > 0) {
            eq.getSpecialStats().add(EquipSpecialStat.TOTAL_DAMAGE);
        }
        if (eq.getAllStat() > 0) {
            eq.getSpecialStats().add(EquipSpecialStat.ALL_STAT);
        }
        if (eq.getFire() >= -1) {
            eq.getSpecialStats().add(EquipSpecialStat.KARMA_COUNT); //no count = -1
        }
        if (eq.getFire() > 0) {
            eq.getSpecialStats().add(EquipSpecialStat.REBIRTH_FIRE); // test
        }
        if (eq.getEquipmentType() > 0) {
            eq.getSpecialStats().add(EquipSpecialStat.EQUIPMENT_TYPE); // test
        }
        return (Equip) eq.copy();
    }

    public int getArcEXP() {
        // TODO Auto-generated method stub
        return arcexp;
    }

    public int getArcLevel() {
        // TODO Auto-generated method stub
        return arclevel;
    }

    public void setArcEXP(int exp) {
        arcexp = exp;
    }

    public void setArcLevel(int lv) {
        arclevel = lv;
    }

    public void resetRebirth(int reqLevel) {

//		반지, 방패, 어깨장식, 훈장, 심장, 보조무기, 엠블렘, 뱃지, 안드로이드, 펫장비, 심볼은 추가옵션이 붙지 않음
        if (GameConstants.isRing(getItemId()) || (getItemId() / 1000) == 1092 || (getItemId() / 1000) == 1342 || (getItemId() / 1000) == 1712 || (getItemId() / 1000) == 1713 || (getItemId() / 1000) == 1152 || (getItemId() / 1000) == 1143 || (getItemId() / 1000) == 1672 || GameConstants.isSecondaryWeapon(getItemId()) || (getItemId() / 1000) == 1190 || (getItemId() / 1000) == 1191 || (getItemId() / 1000) == 1182 || (getItemId() / 1000) == 1662 || (getItemId() / 1000) == 1802) {
            return;
        }

        if (getFire() == 0) {
            return;
        }

        Equip ordinary = (Equip) MapleItemInformationProvider.getInstance().getEquipById(getItemId(), false);

        int ordinaryPad = ordinary.watk > 0 ? ordinary.watk : ordinary.matk;
        int ordinaryMad = ordinary.matk > 0 ? ordinary.matk : ordinary.watk;

        int[] rebirth = new int[4];
        String fire = String.valueOf(getFire());
        if (fire.length() == 12) {
            rebirth[0] = Integer.parseInt(fire.substring(0, 3));
            rebirth[1] = Integer.parseInt(fire.substring(3, 6));
            rebirth[2] = Integer.parseInt(fire.substring(6, 9));
            rebirth[3] = Integer.parseInt(fire.substring(9));
        } else if (fire.length() == 11) {
            rebirth[0] = Integer.parseInt(fire.substring(0, 2));
            rebirth[1] = Integer.parseInt(fire.substring(2, 5));
            rebirth[2] = Integer.parseInt(fire.substring(5, 8));
            rebirth[3] = Integer.parseInt(fire.substring(8));
        } else if (fire.length() == 10) {
            rebirth[0] = Integer.parseInt(fire.substring(0, 1));
            rebirth[1] = Integer.parseInt(fire.substring(1, 4));
            rebirth[2] = Integer.parseInt(fire.substring(4, 7));
            rebirth[3] = Integer.parseInt(fire.substring(7));
        } else {
            return;
        }

        for (int i = 0; i < 4; ++i) {
            int randomOption = rebirth[i] / 10;
            int randomValue = rebirth[i] - (rebirth[i] / 10 * 10);

            switch (randomOption) {
                case 0:
                    addStr((short) -((reqLevel / 20 + 1) * randomValue));
                    break;
                case 1:
                    addDex((short) -((reqLevel / 20 + 1) * randomValue));
                    break;
                case 2:
                    addInt((short) -((reqLevel / 20 + 1) * randomValue));
                    break;
                case 3:
                    addLuk((short) -((reqLevel / 20 + 1) * randomValue));
                    break;
                case 4:
                    addStr((short) -((reqLevel / 40 + 1) * randomValue));
                    addDex((short) -((reqLevel / 40 + 1) * randomValue));
                    break;
                case 5:
                    addStr((short) -((reqLevel / 40 + 1) * randomValue));
                    addInt((short) -((reqLevel / 40 + 1) * randomValue));
                    break;
                case 6:
                    addStr((short) -((reqLevel / 40 + 1) * randomValue));
                    addLuk((short) -((reqLevel / 40 + 1) * randomValue));
                    break;
                case 7:
                    addDex((short) -((reqLevel / 40 + 1) * randomValue));
                    addInt((short) -((reqLevel / 40 + 1) * randomValue));
                    break;
                case 8:
                    addDex((short) -((reqLevel / 40 + 1) * randomValue));
                    addLuk((short) -((reqLevel / 40 + 1) * randomValue));
                    break;
                case 9:
                    addInt((short) -((reqLevel / 40 + 1) * randomValue));
                    addLuk((short) -((reqLevel / 40 + 1) * randomValue));
                    break;
                case 10:
                    addHp((short) -((reqLevel / 10 * 10) * 3 * randomValue));
                    break;
                case 11:
                    addMp((short) -((reqLevel / 10 * 10) * 3 * randomValue));
                    break;
                case 13:
                    addWdef((short) -((reqLevel / 20 + 1) * randomValue));
                    break;
                case 17: {
                    if (GameConstants.isWeapon(getItemId())) {
                        switch (randomValue) {
                            case 3:
                                if (reqLevel <= 150) {
                                    addWatk((short) -(((ordinaryPad * 1200) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addWatk((short) -(((ordinaryPad * 1500) / 10000) + 1));
                                } else {
                                    addWatk((short) -(((ordinaryPad * 1800) / 10000) + 1));
                                }
                                break;
                            case 4:
                                if (reqLevel <= 150) {
                                    addWatk((short) -(((ordinaryPad * 1760) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addWatk((short) -(((ordinaryPad * 2200) / 10000) + 1));
                                } else {
                                    addWatk((short) -(((ordinaryPad * 2640) / 10000) + 1));
                                }
                                break;
                            case 5:
                                if (reqLevel <= 150) {
                                    addWatk((short) -(((ordinaryPad * 2420) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addWatk((short) -(((ordinaryPad * 3025) / 10000) + 1));
                                } else {
                                    addWatk((short) -(((ordinaryPad * 3630) / 10000) + 1));
                                }
                                break;
                            case 6:
                                if (reqLevel <= 150) {
                                    addWatk((short) -(((ordinaryPad * 3200) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addWatk((short) -(((ordinaryPad * 4000) / 10000) + 1));
                                } else {
                                    addWatk((short) -(((ordinaryPad * 4800) / 10000) + 1));
                                }
                                break;
                            case 7:
                                if (reqLevel <= 150) {
                                    addWatk((short) -(((ordinaryPad * 4100) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addWatk((short) -(((ordinaryPad * 5125) / 10000) + 1));
                                } else {
                                    addWatk((short) -(((ordinaryPad * 6150) / 10000) + 1));
                                }
                                break;
                        }
                    } else {
                        addWatk((short) -randomValue);
                    }
                    break;
                }
                case 18: {
                    if (GameConstants.isWeapon(getItemId())) {
                        switch (randomValue) {
                            case 3:
                                if (reqLevel <= 150) {
                                    addMatk((short) -(((ordinaryMad * 1200) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addMatk((short) -(((ordinaryMad * 1500) / 10000) + 1));
                                } else {
                                    addMatk((short) -(((ordinaryMad * 1800) / 10000) + 1));
                                }
                                break;
                            case 4:
                                if (reqLevel <= 150) {
                                    addMatk((short) -(((ordinaryMad * 1760) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addMatk((short) -(((ordinaryMad * 2200) / 10000) + 1));
                                } else {
                                    addMatk((short) -(((ordinaryMad * 2640) / 10000) + 1));
                                }
                                break;
                            case 5:
                                if (reqLevel <= 150) {
                                    addMatk((short) -(((ordinaryMad * 2420) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addMatk((short) -(((ordinaryMad * 3025) / 10000) + 1));
                                } else {
                                    addMatk((short) -(((ordinaryMad * 3630) / 10000) + 1));
                                }
                                break;
                            case 6:
                                if (reqLevel <= 150) {
                                    addMatk((short) -(((ordinaryMad * 3200) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addMatk((short) -(((ordinaryMad * 4000) / 10000) + 1));
                                } else {
                                    addMatk((short) -(((ordinaryMad * 4800) / 10000) + 1));
                                }
                                break;
                            case 7:
                                if (reqLevel <= 150) {
                                    addMatk((short) -(((ordinaryMad * 4100) / 10000) + 1));
                                } else if (reqLevel <= 160) {
                                    addMatk((short) -(((ordinaryMad * 5125) / 10000) + 1));
                                } else {
                                    addMatk((short) -(((ordinaryMad * 6150) / 10000) + 1));
                                }
                                break;
                        }
                    } else {
                        addMatk((short) -randomValue);
                    }
                    break;
                }
                case 19:
                    addSpeed((short) -randomValue);
                    break;
                case 20:
                    addJump((short) -randomValue);
                    break;
                case 21:
                    addBossDamage((byte) -(randomValue * 2));
                    break;
                case 22:
                    setReqLevel((byte) 0);
                    break;
                case 23:
                    addTotalDamage((byte) -randomValue);
                    break;
                case 24:
                    addAllStat((byte) -randomValue);
                    break;
            }
        }

        setFire(0);

    }

    public long newRebirth(int reqLevel, int scrollId, boolean update) {
//		반지, 방패, 어깨장식, 훈장, 심장, 보조무기, 엠블렘, 뱃지, 안드로이드, 펫장비, 심볼은 추가옵션이 붙지 않음
        if (GameConstants.isRing(getItemId()) || (getItemId() / 1000) == 1092 || (getItemId() / 1000) == 1342 || (getItemId() / 1000) == 1712 || (getItemId() / 1000) == 1713 || (getItemId() / 1000) == 1152 || (getItemId() / 1000) == 1142 || (getItemId() / 1000) == 1143 || (getItemId() / 1000) == 1672 || GameConstants.isSecondaryWeapon(getItemId()) || (getItemId() / 1000) == 1190 || (getItemId() / 1000) == 1191 || (getItemId() / 1000) == 1182 || (getItemId() / 1000) == 1662 || (getItemId() / 1000) == 1802) {
            return 0;
        }

        int maxValue = 5;

        if (scrollId == 2048716 || scrollId == 2048720 || scrollId == 2048724 || scrollId == 2048745) { // 강환불
            maxValue = 6;
        }

        if (scrollId == 2048717 || scrollId == 2048721 || scrollId == 2048723 || scrollId == 2048746 || scrollId == 2048747 || scrollId == 2048753 || scrollId == 2048755) { // 영환불, 검환불
            maxValue = 7;
        }

        Equip ordinary = (Equip) MapleItemInformationProvider.getInstance().getEquipById(getItemId(), false);

        int ordinaryPad = ordinary.watk > 0 ? ordinary.watk : ordinary.matk;
        int ordinaryMad = ordinary.matk > 0 ? ordinary.matk : ordinary.watk;

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isKarmaEnabled(getItemId()) || ii.isPKarmaEnabled(getItemId())) && getKarmaCount() < 0) {
            setKarmaCount((byte) 10);
        }

        long[] rebirth = {-1, -1, -1, -1};
        int[] rebirthOptions = {-1, -1, -1, -1};

        for (int i = 0; i < 4; ++i) {
            int randomOption = Randomizer.nextInt(25);
            if (scrollId == 2048753 || scrollId == 2048755) {
                while (randomOption == 22) {
                    randomOption = Randomizer.nextInt(25);
                }
            }

            while ((rebirthOptions[0] == randomOption || rebirthOptions[1] == randomOption || rebirthOptions[2] == randomOption || rebirthOptions[3] == randomOption) || (randomOption == 12 || randomOption == 14 || randomOption == 15 || randomOption == 16 || (!GameConstants.isWeapon(getItemId()) && (randomOption == 21 || randomOption == 23)))) {
                randomOption = Randomizer.nextInt(25);
                if (scrollId == 2048753 || scrollId == 2048755) { //검환불이면서 착감 옵일때는 다시 돌리기
                    while (randomOption == 22) {
                        randomOption = Randomizer.nextInt(25);
                    }
                }

            }

            rebirthOptions[i] = randomOption;
            //  System.out.println(randomOption);
            int randomValue = 0; // 보통 3~7 -> 흔히 말하는 1~5급이 됨

            if (((randomOption == 17 || randomOption == 18) && !GameConstants.isWeapon(getItemId())) || randomOption == 22) {
                randomValue = Randomizer.rand(1, maxValue);
            } else {
                randomValue = Randomizer.rand(3, maxValue);
            }

            if (scrollId == 2048753 || scrollId == 2048755) {
                randomValue += Randomizer.rand(1, 3);
                if (randomValue > maxValue)
                    randomValue = maxValue;
            }

            rebirth[i] = (randomOption * 10 + randomValue);

            for (int j = 0; j < i; ++j) {
                rebirth[i] *= 1000;
            }

            if (update) {
                setFireOption(randomOption, reqLevel, randomValue, ordinaryPad, ordinaryMad);
            }
        }

        return (rebirth[0] + rebirth[1] + rebirth[2] + rebirth[3]);
    }

    public int getMoru() {
        return moru;
    }

    public void setMoru(int moru) {
        this.moru = moru;
    }

    public void setFireOption(int randomOption, int reqLevel, int randomValue, int ordinaryPad, int ordinaryMad) {
        switch (randomOption) {
            case 0:
                addStr((short) ((reqLevel / 20 + 1) * randomValue));
                break;
            case 1:
                addDex((short) ((reqLevel / 20 + 1) * randomValue));
                break;
            case 2:
                addInt((short) ((reqLevel / 20 + 1) * randomValue));
                break;
            case 3:
                addLuk((short) ((reqLevel / 20 + 1) * randomValue));
                break;
            case 4:
                addStr((short) ((reqLevel / 40 + 1) * randomValue));
                addDex((short) ((reqLevel / 40 + 1) * randomValue));
                break;
            case 5:
                addStr((short) ((reqLevel / 40 + 1) * randomValue));
                addInt((short) ((reqLevel / 40 + 1) * randomValue));
                break;
            case 6:
                addStr((short) ((reqLevel / 40 + 1) * randomValue));
                addLuk((short) ((reqLevel / 40 + 1) * randomValue));
                break;
            case 7:
                addDex((short) ((reqLevel / 40 + 1) * randomValue));
                addInt((short) ((reqLevel / 40 + 1) * randomValue));
                break;
            case 8:
                addDex((short) ((reqLevel / 40 + 1) * randomValue));
                addLuk((short) ((reqLevel / 40 + 1) * randomValue));
                break;
            case 9:
                addInt((short) ((reqLevel / 40 + 1) * randomValue));
                addLuk((short) ((reqLevel / 40 + 1) * randomValue));
                break;
            case 10:
                addHp((short) ((reqLevel / 10 * 10) * 3 * randomValue));
                break;
            case 11:
                addMp((short) ((reqLevel / 10 * 10) * 3 * randomValue));
                break;
            case 13:
                addWdef((short) ((reqLevel / 20 + 1) * randomValue));
                break;
            case 17: {
                if (GameConstants.isWeapon(getItemId())) {
                    switch (randomValue) {
                        case 3:
                            if (reqLevel <= 150) {
                                addWatk((short) (((ordinaryPad * 1200) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addWatk((short) (((ordinaryPad * 1500) / 10000) + 1));
                            } else {
                                addWatk((short) (((ordinaryPad * 1800) / 10000) + 1));
                            }
                            break;
                        case 4:
                            if (reqLevel <= 150) {
                                addWatk((short) (((ordinaryPad * 1760) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addWatk((short) (((ordinaryPad * 2200) / 10000) + 1));
                            } else {
                                addWatk((short) (((ordinaryPad * 2640) / 10000) + 1));
                            }
                            break;
                        case 5:
                            if (reqLevel <= 150) {
                                addWatk((short) (((ordinaryPad * 2420) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addWatk((short) (((ordinaryPad * 3025) / 10000) + 1));
                            } else {
                                addWatk((short) (((ordinaryPad * 3630) / 10000) + 1));
                            }
                            break;
                        case 6:
                            if (reqLevel <= 150) {
                                addWatk((short) (((ordinaryPad * 3200) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addWatk((short) (((ordinaryPad * 4000) / 10000) + 1));
                            } else {
                                addWatk((short) (((ordinaryPad * 4800) / 10000) + 1));
                            }
                            break;
                        case 7:
                            if (reqLevel <= 150) {
                                addWatk((short) (((ordinaryPad * 4100) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addWatk((short) (((ordinaryPad * 5125) / 10000) + 1));
                            } else {
                                addWatk((short) (((ordinaryPad * 6150) / 10000) + 1));
                            }
                            break;
                    }
                } else {
                    addWatk((short) randomValue);
                }
                break;
            }
            case 18: {
                if (GameConstants.isWeapon(getItemId())) {
                    switch (randomValue) {
                        case 3:
                            if (reqLevel <= 150) {
                                addMatk((short) (((ordinaryMad * 1200) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addMatk((short) (((ordinaryMad * 1500) / 10000) + 1));
                            } else {
                                addMatk((short) (((ordinaryMad * 1800) / 10000) + 1));
                            }
                            break;
                        case 4:
                            if (reqLevel <= 150) {
                                addMatk((short) (((ordinaryMad * 1760) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addMatk((short) (((ordinaryMad * 2200) / 10000) + 1));
                            } else {
                                addMatk((short) (((ordinaryMad * 2640) / 10000) + 1));
                            }
                            break;
                        case 5:
                            if (reqLevel <= 150) {
                                addMatk((short) (((ordinaryMad * 2420) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addMatk((short) (((ordinaryMad * 3025) / 10000) + 1));
                            } else {
                                addMatk((short) (((ordinaryMad * 3630) / 10000) + 1));
                            }
                            break;
                        case 6:
                            if (reqLevel <= 150) {
                                addMatk((short) (((ordinaryMad * 3200) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addMatk((short) (((ordinaryMad * 4000) / 10000) + 1));
                            } else {
                                addMatk((short) (((ordinaryMad * 4800) / 10000) + 1));
                            }
                            break;
                        case 7:
                            if (reqLevel <= 150) {
                                addMatk((short) (((ordinaryMad * 4100) / 10000) + 1));
                            } else if (reqLevel <= 160) {
                                addMatk((short) (((ordinaryMad * 5125) / 10000) + 1));
                            } else {
                                addMatk((short) (((ordinaryMad * 6150) / 10000) + 1));
                            }
                            break;
                    }
                } else {
                    addMatk((short) randomValue);
                }
                break;
            }
            case 19:
                addSpeed((short) randomValue);
                break;
            case 20:
                addJump((short) randomValue);
                break;
            case 21:
                addBossDamage((byte) (randomValue * 2));
                break;
            case 22:
                setReqLevel((byte) (-5 * randomValue));
                break;
            case 23:
                addTotalDamage((byte) randomValue);
                break;
            case 24:
                addAllStat((byte) randomValue);
                break;
        }
    }

    public int getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(int equipmentType) {
        this.equipmentType = equipmentType;
    }

    public short getEnchantStr() {
        return enchantStr;
    }

    public void setEnchantStr(short enchantStr) {
        this.enchantStr = enchantStr;
    }

    public short getEnchantDex() {
        return enchantDex;
    }

    public void setEnchantDex(short enchantDex) {
        this.enchantDex = enchantDex;
    }

    public short getEnchantInt() {
        return enchantInt;
    }

    public void setEnchantInt(short enchantInt) {
        this.enchantInt = enchantInt;
    }

    public short getEnchantLuk() {
        return enchantLuk;
    }

    public void setEnchantLuk(short enchantLuk) {
        this.enchantLuk = enchantLuk;
    }

    public short getEnchantHp() {
        return enchantHp;
    }

    public void setEnchantHp(short enchantHp) {
        this.enchantHp = enchantHp;
    }

    public short getEnchantMp() {
        return enchantMp;
    }

    public void setEnchantMp(short enchantMp) {
        this.enchantMp = enchantMp;
    }

    public short getEnchantAcc() {
        return enchantAcc;
    }

    public void setEnchantAcc(short enchantAcc) {
        this.enchantAcc = enchantAcc;
    }

    public short getEnchantAvoid() {
        return enchantAvoid;
    }

    public void setEnchantAvoid(short enchantAvoid) {
        this.enchantAvoid = enchantAvoid;
    }

    public short getEnchantWatk() {
        return enchantWatk;
    }

    public void setEnchantWatk(short enchantWatk) {
        this.enchantWatk = enchantWatk;
    }

    public short getEnchantMatk() {
        return enchantMatk;
    }

    public void setEnchantMatk(short enchantMatk) {
        this.enchantMatk = enchantMatk;
    }

    public short getTotalStr() {
        return (short) (str + enchantStr);
    }

    public short getTotalDex() {
        return (short) (dex + enchantDex);
    }

    public short getTotalInt() {
        return (short) (_int + enchantInt);
    }

    public short getTotalLuk() {
        return (short) (luk + enchantLuk);
    }

    public short getTotalHp() {
        return (short) (hp + enchantHp);
    }

    public short getTotalMp() {
        return (short) (mp + enchantMp);
    }

    public short getTotalAcc() {
        return (short) (acc + enchantAcc);
    }

    public short getTotalAvoid() {
        return (short) (avoid + enchantAvoid);
    }

    public short getTotalWatk() {
        return (short) (watk + enchantWatk);
    }

    public short getTotalMatk() {
        return (short) (matk + enchantMatk);
    }

    public short getTotalWdef() {
        return (short) (wdef + enchantWdef);
    }

    public short getTotalMdef() {
        return (short) (mdef + enchantMdef);
    }

    public short getEnchantWdef() {
        return enchantWdef;
    }

    public void setEnchantWdef(short enchantWdef) {
        this.enchantWdef = enchantWdef;
    }

    public short getEnchantMdef() {
        return enchantMdef;
    }

    public void setEnchantMdef(short enchantMdef) {
        this.enchantMdef = enchantMdef;
    }
}
