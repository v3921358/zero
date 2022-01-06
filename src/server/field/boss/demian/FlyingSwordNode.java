package server.field.boss.demian;

import java.awt.*;

public class FlyingSwordNode {

    private byte nodeType, collisionType;
    private short pathIndex, nodeIndex, v;
    private int startDelay, endDelay, duration;
    private boolean hide;
    private Point pos;

    public FlyingSwordNode(int i, int j, int k, int l, int startDelay, int endDelay, int duration, boolean hide, int m, Point pos) {
        this.nodeType = (byte) i;
        this.pathIndex = (short) j;
        this.nodeIndex = (short) k;
        this.v = (short) l;
        this.startDelay = startDelay;
        this.endDelay = endDelay;
        this.duration = duration;
        this.hide = hide;
        this.collisionType = (byte) m;
        this.pos = pos;
    }

    public byte getNodeType() {
        return nodeType;
    }

    public void setNodeType(byte nodeType) {
        this.nodeType = nodeType;
    }

    public short getPathIndex() {
        return pathIndex;
    }

    public void setPathIndex(short pathIndex) {
        this.pathIndex = pathIndex;
    }

    public short getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(short nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public short getV() {
        return v;
    }

    public void setV(short v) {
        this.v = v;
    }

    public int getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
    }

    public int getEndDelay() {
        return endDelay;
    }

    public void setEndDelay(int endDelay) {
        this.endDelay = endDelay;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public byte getCollisionType() {
        return collisionType;
    }

    public void setCollisionType(byte collisionType) {
        this.collisionType = collisionType;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

}
