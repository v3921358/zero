package server;

import java.awt.*;

public class Obstacle {

    private Point oldPosition, newPosition;
    private int key, range, trueDamage, delay, height, VperSec, maxP, length, angle, unk;

    public Obstacle(int key, Point pos1, Point pos2, int range, int trueDamage, int height, int MaxP, int length, int angle, int unk) {
        this.key = key;
        this.oldPosition = pos1;
        this.newPosition = pos2;
        this.range = range;
        this.trueDamage = trueDamage;
        this.delay = 0;
        this.setHeight(height);
        this.maxP = MaxP;
        this.length = length;
        this.angle = angle;
        this.unk = unk;
    }

    public Obstacle(int key, Point pos1, Point pos2, int range, int trueDamage, int height, int MaxP, int length, int angle) {
        this.key = key;
        this.oldPosition = pos1;
        this.newPosition = pos2;
        this.range = range;
        this.trueDamage = trueDamage;
        this.delay = 0;
        this.setHeight(height);
        this.maxP = MaxP;
        this.length = length;
        this.angle = angle;
        this.unk = 0;
    }

    public Point getOldPosition() {
        return oldPosition;
    }

    public void setOldPosition(Point oldPosition) {
        this.oldPosition = oldPosition;
    }

    public Point getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(Point newPosition) {
        this.newPosition = newPosition;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getTrueDamage() {
        return trueDamage;
    }

    public void setTrueDamage(int trueDamage) {
        this.trueDamage = trueDamage;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getVperSec() {
        return VperSec;
    }

    public void setVperSec(int vperSec) {
        VperSec = vperSec;
    }

    public int getMaxP() {
        return maxP;
    }

    public void setMaxP(int maxP) {
        this.maxP = maxP;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

}
