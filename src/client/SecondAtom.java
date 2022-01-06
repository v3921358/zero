/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zz
 */
public class SecondAtom {

    private int dataIndex, target, createDelay, enableDelay, expire, enableCustom, attackableCount, rotate, sourceId;
    private Point pos;
    private List<Point> extraPos;
    private List<Integer> custom;

    public SecondAtom(SecondAtom sa) {
        this.dataIndex = sa.getDataIndex();
        this.target = sa.getTarget();
        this.createDelay = sa.getCreateDelay();
        this.enableDelay = sa.getEnableDelay();
        this.expire = sa.getExpire();
        this.enableCustom = sa.getEnableCustom();
        this.attackableCount = sa.getAttackableCount();
        this.pos = sa.getPos();
        this.rotate = sa.getRotate();
        this.extraPos = sa.getExtraPos();
        this.custom = sa.getCustom();
        this.sourceId = sa.getSourceId();
    }

    public SecondAtom(int dataIndex, int target, int createDelay, int enableDelay, int expire, int enableCustom, int attackableCount, Point pos, int rotate, List<Point> extraPos, List<Integer> custom, int sourceId) {
        this.dataIndex = dataIndex;
        this.target = target;
        this.createDelay = createDelay;
        this.enableDelay = enableDelay;
        this.expire = expire;
        this.enableCustom = enableCustom;
        this.attackableCount = attackableCount;
        this.pos = pos;
        this.rotate = rotate;

        if (extraPos == null) {
            this.extraPos = new ArrayList<>();
        } else {
            this.extraPos = extraPos;
        }

        if (custom == null) {
            this.custom = new ArrayList<>();
        } else {
            this.custom = custom;
        }

        this.sourceId = sourceId;
    }

    public int getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(int dataIndex) {
        this.dataIndex = dataIndex;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getCreateDelay() {
        return createDelay;
    }

    public void setCreateDelay(int createDelay) {
        this.createDelay = createDelay;
    }

    public int getEnableDelay() {
        return enableDelay;
    }

    public void setEnableDelay(int enableDelay) {
        this.enableDelay = enableDelay;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getEnableCustom() {
        return enableCustom;
    }

    public void setEnableCustom(int enableCustom) {
        this.enableCustom = enableCustom;
    }

    public int getAttackableCount() {
        return attackableCount;
    }

    public void setAttackableCount(int attackableCount) {
        this.attackableCount = attackableCount;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public List<Point> getExtraPos() {
        return extraPos;
    }

    public void setExtraPos(List<Point> extraPos) {
        this.extraPos = extraPos;
    }

    public List<Integer> getCustom() {
        return custom;
    }

    public void setCustom(List<Integer> custom) {
        this.custom = custom;
    }

}
