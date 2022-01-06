package server.maps;

import java.awt.Point;

public class ForceAtom {

    private int nAttackCount, nInc, nFirstImpact, nSecondImpact, nAngle, nStartDelay, nStartX, nStartY, nMaxHitCount, nEffectIdx;
    private long dwCreateTime;

    public ForceAtom(int nInc, int nFirstImpact, int nSecondImpact, int nAngle, int nStartDelay) {
        this.nAttackCount = 1;
        this.nInc = nInc;
        this.nFirstImpact = nFirstImpact;
        this.nSecondImpact = nSecondImpact;
        this.nAngle = nAngle;
        this.nStartDelay = nStartDelay;
        this.nStartX = 0;
        this.nStartY = 0;
        this.dwCreateTime = System.currentTimeMillis();
        this.nMaxHitCount = 0;
        this.nEffectIdx = 0;
    }

    public ForceAtom(int nInc, int nFirstImpact, int nSecondImpact, int nAngle, int nStartDelay, Point pos) {
        this.nAttackCount = 1;
        this.nInc = nInc;
        this.nFirstImpact = nFirstImpact;
        this.nSecondImpact = nSecondImpact;
        this.nAngle = nAngle;
        this.nStartDelay = nStartDelay;
        this.nStartX = pos.x;
        this.nStartY = pos.y;
        this.dwCreateTime = System.currentTimeMillis();
        this.nMaxHitCount = 0;
        this.nEffectIdx = 0;
    }

    public ForceAtom(int nInc, int nFirstImpact, int nSecondImpact, int nAngle, int nStartDelay, int nStartX, int nStartY, long dwCreateTime, int nMaxHitCount, int nEffectIdx) {
        this.nAttackCount = 1;
        this.nInc = nInc;
        this.nFirstImpact = nFirstImpact;
        this.nSecondImpact = nSecondImpact;
        this.nAngle = nAngle;
        this.nStartDelay = nStartDelay;
        this.nStartX = nStartX;
        this.nStartY = nStartY;
        this.dwCreateTime = dwCreateTime;
        this.nMaxHitCount = nMaxHitCount;
        this.nEffectIdx = nEffectIdx;
    }

    public int getnInc() {
        return nInc;
    }

    public void setnInc(int nInc) {
        this.nInc = nInc;
    }

    public int getnFirstImpact() {
        return nFirstImpact;
    }

    public void setnFirstImpact(int nFirstImpact) {
        this.nFirstImpact = nFirstImpact;
    }

    public int getnSecondImpact() {
        return nSecondImpact;
    }

    public void setnSecondImpact(int nSecondImpact) {
        this.nSecondImpact = nSecondImpact;
    }

    public int getnAngle() {
        return nAngle;
    }

    public void setnAngle(int nAngle) {
        this.nAngle = nAngle;
    }

    public int getnStartDelay() {
        return nStartDelay;
    }

    public void setnStartDelay(int nStartDelay) {
        this.nStartDelay = nStartDelay;
    }

    public int getnStartX() {
        return nStartX;
    }

    public void setnStartX(int nStartX) {
        this.nStartX = nStartX;
    }

    public int getnStartY() {
        return nStartY;
    }

    public void setnStartY(int nStartY) {
        this.nStartY = nStartY;
    }

    public long getDwCreateTime() {
        return dwCreateTime;
    }

    public void setDwCreateTime(long dwCreateTime) {
        this.dwCreateTime = dwCreateTime;
    }

    public int getnMaxHitCount() {
        return nMaxHitCount;
    }

    public void setnMaxHitCount(int nMaxHitCount) {
        this.nMaxHitCount = nMaxHitCount;
    }

    public int getnEffectIdx() {
        return nEffectIdx;
    }

    public void setnEffectIdx(int nEffectIdx) {
        this.nEffectIdx = nEffectIdx;
    }

    public int getnAttackCount() {
        return nAttackCount;
    }

    public void setnAttackCount(int nAttackCount) {
        this.nAttackCount = nAttackCount;
    }
}
