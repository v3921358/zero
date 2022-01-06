package client;

import java.awt.*;

public class RangeAttack {
    private int skill, delay, attackCount;
    private short direction;
    private Point pos;

    public RangeAttack(int skill, Point pos, short direction, int delay, int attackCount) {
        this.skill = skill;
        this.delay = delay;
        this.attackCount = attackCount;
        this.direction = direction;
        this.pos = pos;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public void setAttackCount(int attackCount) {
        this.attackCount = attackCount;
    }

    public short getDirection() {
        return direction;
    }

    public void setDirection(short direction) {
        this.direction = direction;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }
}
