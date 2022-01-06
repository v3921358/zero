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

import client.MapleCharacter;

import java.util.concurrent.ScheduledFuture;

public class MonsterStatusEffect {

    private final int skill;
    private int duration, level, cid, interval;
    private long value;
    private long cancelTask, startTime, lastPoisonTime;
    private MonsterStatus ms;
    private MapleCharacter chr;
    private ScheduledFuture<?> schedule;

    public MonsterStatusEffect(int skill, int duration) {
        this.skill = skill;
        this.duration = duration;
        this.setStartTime(System.currentTimeMillis());
        setCancelTask(duration);
    }

    public MonsterStatusEffect(int skill, int duration, long value) {
        this.skill = skill;
        this.duration = duration;
        this.value = value;
        this.setStartTime(System.currentTimeMillis());
        setCancelTask(duration);
    }

    public final boolean shouldCancel(long now) {
        return (cancelTask > 0 && cancelTask <= now);
    }

    public final void cancelTask() {
        cancelTask = 0;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSkill() {
        return skill;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getCancelTask() {
        return cancelTask;
    }

    public void setCancelTask(long cancelTask) {
        this.cancelTask = System.currentTimeMillis() + cancelTask;
    }

    public MonsterStatus getStati() {
        return ms;
    }

    public void setStati(MonsterStatus ms) {
        this.ms = ms;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public MapleCharacter getChr() {
        return chr;
    }

    public void setChr(MapleCharacter cid) {
        this.chr = cid;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public long getLastPoisonTime() {
        return lastPoisonTime;
    }

    public void setLastPoisonTime(long lastPoisonTime) {
        this.lastPoisonTime = lastPoisonTime;
    }
}
