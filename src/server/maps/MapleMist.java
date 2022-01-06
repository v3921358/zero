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
import client.Skill;
import client.SkillFactory;
import constants.GameConstants;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.packet.CField;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;

public class MapleMist extends MapleMapObject {

    private MapleCharacter owner;
    private MapleMonster mob;
    private Rectangle mistPosition;
    private MapleStatEffect source;
    private MobSkill skill;
    private boolean isMobMist;
    private byte rltype;
    private int skillDelay, skilllevel, isMistType = 0, ownerId, duration = 0, endtime = 0;
    private long startTime = 0;
    private ScheduledFuture<?> schedule = null, poisonSchedule = null;

    public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill, int duration) {
        this.mistPosition = mistPosition;
        this.setMob(mob);
        this.ownerId = mob.getObjectId();
        this.skill = skill;
        this.skilllevel = skill.getSkillLevel();
        this.duration = duration;
        this.setStartTime(System.currentTimeMillis());

        isMobMist = true;
        skillDelay = 0;

        if (skill.getSkillId() == 191) {
            skillDelay = 6;
        }

        if (skill.getSkillId() == 131 && skill.getSkillLevel() == 28) {
            skillDelay = 9;
        }

        if (skill.getSkillId() == 186) {
            isMistType = 8;
        }
    }

    public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source, int duration, byte rltype) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.ownerId = owner.getId();
        this.source = source;
        this.skillDelay = 8;
        this.isMobMist = false;
        this.skilllevel = owner.getTotalSkillLevel(GameConstants.getLinkedSkill(source.getSourceId()));
        this.setDuration(duration);
        this.setRltype(rltype);
        this.setStartTime(System.currentTimeMillis());

        if (owner.isGM()) {
            owner.dropMessage(6, "mist lv : " + this.skilllevel + " / duration : " + this.duration + " / Postion : " + this.getPosition());
        }

        switch (source.getSourceId()) {
            case 14111006:
            case 1076:
            case 11076:
            case 2111003: // FP mist
            case 12111005: // Flame wizard, [Flame Gear]
                isMistType = 1;
                break;
            case 4221006: // Smoke Screen
            case 400001017:
            case 400021030:
            case 400021031:
                isMobMist = true;
                isMistType = 0;
                break;
            case 32121006: //Party Shield
                isMistType = 2; //TODO
                isMobMist = true;
                break;
//                isMistType = 3;
            //              break;
            case 22161003: //Recovery Aura
                isMistType = 4;
                break;
            case 12121005: //踰꾨떇由ъ졏
                isMistType = 6;
                break;
            case 61121116:
                skillDelay = 0;
                break;
            case 152121041:
            case 162121043:
            case 162111003:
            case 162111000:
                skillDelay = 2;
                break;
        }

        if (source.getSourceId() == 400001017) {
            //      	this.skillDelay = 2;
//        	this.mistPosition = new Rectangle(owner.getTruePosition().x - 150, owner.getTruePosition().y, 445, 0);
        }
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MIST;
    }

    public Skill getSourceSkill() {
        if (source == null) {
            return null;
        }
        return SkillFactory.getSkill(source.getSourceId());
    }

    public void setSchedule(ScheduledFuture<?> s) {
        this.schedule = s;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setPoisonSchedule(ScheduledFuture<?> s) {
        this.poisonSchedule = s;
    }

    public ScheduledFuture<?> getPoisonSchedule() {
        return poisonSchedule;
    }

    public int getEndTime() {
        return endtime;
    }

    public void setEndTime(int time) {
        this.endtime = time;
    }

    public boolean isMobMist() {
        return isMobMist;
    }

    public int isPoisonMist() {
        return isMistType;
    }

    public void setSkillLevel(int skilllv) {
        this.skilllevel = skilllv;
    }

    public int getSkillDelay() {
        return skillDelay;
    }

    public int getSkillLevel() {
        return skilllevel;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public MobSkill getMobSkill() {
        return this.skill;
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    public void setBox(Rectangle r) {
        this.mistPosition = r;
    }

    public MapleStatEffect getSource() {
        return source;
    }

    public byte[] fakeSpawnData(int level) {
        return CField.spawnMist(this);
    }

    @Override
    public void sendSpawnData(final MapleClient c) {
        c.getSession().writeAndFlush(CField.spawnMist(this));
    }

    @Override
    public void sendDestroyData(final MapleClient c) {
        c.getSession().writeAndFlush(CField.removeMist(this));
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public MapleMonster getMob() {
        return mob;
    }

    public void setMob(MapleMonster mob) {
        this.mob = mob;
    }

    public int getDuration() {
        return duration;
    }

    public void setDelay(int delay) {
        this.skillDelay = delay;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public byte getRltype() {
        return rltype;
    }

    public void setRltype(byte rltype) {
        this.rltype = rltype;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
