package server.life;

import server.Randomizer;

public class Ignition {

    private int ownerId, skill, interval, duration;
    private long damage, startTime;
    private int IgnitionKey;

    public Ignition(int ownerId, int skill, long damage, int interval, int duration) {
        this.ownerId = ownerId;
        this.skill = skill;
        this.damage = damage;
        this.interval = interval;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.IgnitionKey = Randomizer.nextInt();
    }

    public int getIgnitionKey() {
        return IgnitionKey;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public long getDamage() {
        return damage;
    }

    public void setDamage(long damage) {
        this.damage = damage;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

}
