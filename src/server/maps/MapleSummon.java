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
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;
import server.MapleStatEffect;
import tools.packet.CField;
import tools.packet.CField.SummonPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MapleSummon extends AnimatedMapleMapObject {

    private MapleCharacter owner;
    private final int skillLevel;
    private MapleMap map; //required for instanceMaps
    private byte rltype;
    private int hp, duration, skill;
    private boolean changedMap = false, controlCrystal = false, noapply = false;
    private SummonMovementType movementType;
    // Since player can have more than 1 summon [Pirate] 
    // Let's put it here instead of cheat tracker
    private int lastSummonTickCount, energy = 0;
    private byte Summon_tickResetCount, changePositionCount = 0;
    private long Server_ClientSummonTickDiff;
    private long lastAttackTime;
    private List<Integer> magicSkills = new ArrayList<>();
    private List<Boolean> crystalSkills = new ArrayList<>();

    public MapleSummon(final MapleCharacter owner, final MapleStatEffect skill, final Point pos, final SummonMovementType movementType) {
        this(owner, skill.getSourceId(), pos, movementType, (byte) 0, skill.getDuration());
    }

    public MapleSummon(final MapleCharacter owner, final MapleStatEffect skill, final Point pos, final SummonMovementType movementType, byte rltype) {
        this(owner, skill.getSourceId(), pos, movementType, rltype, skill.getDuration());
    }

    public MapleSummon(final MapleCharacter owner, final int sourceid, final Point pos, final SummonMovementType movementType, byte rltype, int duration) {
        super();
        this.owner = owner;
        this.skill = sourceid;
        this.map = owner.getMap();
        this.skillLevel = owner.getTotalSkillLevel(GameConstants.getLinkedSkill(skill));
        this.rltype = rltype;
        this.movementType = movementType;
        this.duration = duration;
        setPosition(pos);

        if (!isPuppet()) { // Safe up 12 bytes of data, since puppet doesn't attack.
            lastSummonTickCount = 0;
            Summon_tickResetCount = 0;
            Server_ClientSummonTickDiff = 0;
            lastAttackTime = 0;
        }

        if (sourceid == 152101000 || sourceid == 400021073 || sourceid == 164121008) {
            controlCrystal = true;
            if (sourceid == 152101000) {
                energy = owner.CrystalCharge;
            }
        }
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        client.getSession().writeAndFlush(SummonPacket.removeSummon(this, false));
    }

    public final void updateMap(final MapleMap map) {
        this.map = map;
    }

    public final int getSkill() {
        return skill;
    }

    public final void setSkill(int skillid) {
        skill = skillid;
    }

    public final int getHP() {
        return hp;
    }

    public final int getSummonRLType() {
        return rltype;
    }

    public final void addHP(final int delta) {
        this.hp += delta;
    }

    public final SummonMovementType getMovementType() {
        return movementType;
    }

    public final void setMovementType(SummonMovementType type) {
        this.movementType = type;
    }

    public final boolean isPuppet() {
        switch (skill) {
            case 3111002:
            case 3211002:
            case 3120012:
            case 3220012:
            case 13111004:
            case 4341006:
            case 33111003:
            case 13111024: //?먮찓?꾨뱶 ?뚮씪??            
            case 13120007: //?먮찓?꾨뱶 ?붿뒪??                
                return true;
        }
        return isAngel();
    }

    public final boolean isAngel() {
        return GameConstants.isAngel(skill);
    }

    public final boolean isMultiAttack() {
        if (skill != 35111002 && skill != 35121003 && skill != 61111002 && skill != 61111220 && (isGaviota() || skill == 33101008 || skill >= 35000000) && skill != 35111009 && skill != 35111010 && skill != 35111001) {
            return false;
        }
        return true;
    }

    public final boolean isGaviota() {
        return skill == 5211002;
    }

    public final boolean isBeholder() {
        return skill == 1301013;
    }

    public final int getSkillLevel() {
        return skillLevel;
    }

    public final int getSummonType() {
        if (isAngel()) {
            return 2;
        } else if ((skill != 33111003 && skill != 3120012 && skill != 3220012 && isPuppet()) || skill == 33101008 || skill == 35111002 || skill == 14111024 || skill == 14121054 || skill == 14121055 || skill == 14121056 || skill == 151100002 || skill == 164121006 || skill == 400051017) {
            return 0;
        }
        switch (skill) {
            case 400031051:
            case 400031049:
                return 1;
            case 1301013:
                return 2; //buffs and stuff
            case 36121002:
            case 36121014:
                return 3;
            case 164111007:
                return 5;
            case 35121009:
                return 6;
            case 25121133:
            case 32001014: // 데스
            case 32100010: // 데스 컨트랙트
            case 32110017: // 데스 컨트랙트2
            case 32120019: // 데스 컨트랙트3
            case 35121003:
            case 152001003:
            case 152101000:
            case 400011077:
            case 400011078:
            case 400021068:
            case 400041052:
                return 7;
            case 5220023:
            case 5220024:
            case 5220025:
                return 12;
            case 400041038:
                return 13;
            case 400051009:
                return 15;
            case 400021047:
            case 400021063:
                return 16;
            case 400001040:
            case 400001060:
            case 400021071:
            case 400051046:
            case 400031047:
            case 400051068:
            case 400021092:
            case 162101003:
            case 162101006:
            case 162121012:
            case 162121015:
                return 17;
        }
        return 1;
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }

    public final void CheckSummonAttackFrequency(final MapleCharacter chr, final int tickcount) {
        final int tickdifference = (tickcount - lastSummonTickCount);
        final long STime_TC = System.currentTimeMillis() - tickcount;
        final long S_C_Difference = Server_ClientSummonTickDiff - STime_TC;
        Summon_tickResetCount++;
        if (Summon_tickResetCount > 4) {
            Summon_tickResetCount = 0;
            Server_ClientSummonTickDiff = STime_TC;
        }
        lastSummonTickCount = tickcount;
    }

    public final void CheckPVPSummonAttackFrequency(final MapleCharacter chr) {
        final long tickdifference = (System.currentTimeMillis() - lastAttackTime);
        lastAttackTime = System.currentTimeMillis();
    }

    public final boolean isChangedMap() {
        return changedMap;
    }

    public final void setChangedMap(boolean cm) {
        this.changedMap = cm;
    }

    public final void removeSummon(final MapleMap map, boolean changechannel) {
        removeSummon(map, true, changechannel);
    }

    public final void removeSummon(final MapleMap map, final boolean animation, boolean changechannel) {
        map.broadcastMessage(CField.SummonPacket.removeSummon(this, animation));
        map.removeMapObject(this);
        if (!changechannel) {
            getOwner().removeVisibleMapObject(this);
            getOwner().removeSummon(this);
        }
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public void setOwner(MapleCharacter owner) {
        this.owner = owner;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public List<Integer> getMagicSkills() {
        return magicSkills;
    }

    public void setMagicSkills(List<Integer> magicSkills) {
        this.magicSkills = magicSkills;
    }

    public boolean isControlCrystal() {
        return controlCrystal;
    }

    public void setControlCrystal(boolean controlCrystal) {
        this.controlCrystal = controlCrystal;
    }

    public List<Boolean> getCrystalSkills() {
        return crystalSkills;
    }

    public void setCrystalSkills(List<Boolean> crystalSkills) {
        this.crystalSkills = crystalSkills;
    }

    public boolean isNoapply() {
        return noapply;
    }

    public void SetNoapply(boolean noapply) {
        this.noapply = noapply;
    }

    public byte getChangePositionCount() {
        return changePositionCount;
    }

    public void setChangePositionCount(byte changePositionCount) {
        this.changePositionCount = changePositionCount;
    }
}
