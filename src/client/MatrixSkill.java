package client;

import java.awt.*;

public class MatrixSkill {

    private short unk2;
    private int skill, level, unk1, unk3, unk4, unk5, unk6, x, y, x2, y2;
    private Point angle;

    public MatrixSkill(int skill, int level, int unk1, short unk2, Point angle, int unk3, int unk4) {
        this.setSkill(skill);
        this.setLevel(level);
        this.setUnk1(unk1);
        this.setUnk2(unk2);
        this.setAngle(angle);
        this.setUnk3(unk3);
        this.setUnk4(unk4);
        this.setUnk5(0, 0, 0);
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getUnk1() {
        return unk1;
    }

    public void setUnk1(int unk1) {
        this.unk1 = unk1;
    }

    public short getUnk2() {
        return unk2;
    }

    public void setUnk2(short unk2) {
        this.unk2 = unk2;
    }

    public Point getAngle() {
        return angle;
    }

    public void setAngle(Point angle) {
        this.angle = angle;
    }

    public int getUnk3() {
        return unk3;
    }

    public void setUnk3(int unk3) {
        this.unk3 = unk3;
    }

    public int getUnk4() {
        return unk4;
    }

    public void setUnk4(int unk4) {
        this.unk4 = unk4;
    }

    public int getUnk5() {
        return unk5;
    }

    public void setUnk5(int unk5, int x, int y) {
        this.unk5 = unk5;
        this.setX(x);
        this.setY(y);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getUnk6() {
        return unk6;
    }

    public void setUnk6(int unk6, int x2, int y2) {
        this.unk6 = unk6;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }
}
