/*
 * ArcStory Project
 * 理쒖＜??sch2307@naver.com
 * ?댁? junny_adm@naver.com
 * ?곗???raccoonfox69@gmail.com
 * 媛뺤젙洹?ku3135@nate.com
 * 源吏꾪솉 designer@inerve.kr
 */
package server.movement;

import java.awt.*;

public abstract class AbstractLifeMovement implements LifeMovement {

    private final Point position;
    private final int duration;
    private final int newstate;
    private final int type;
    private final short foodholds;
    private final byte unk;

    public AbstractLifeMovement(int type, Point position, int duration, int newstate, short FH, byte unk) {
        super();
        this.type = type;
        this.position = position;
        this.duration = duration;
        this.newstate = newstate;
        this.foodholds = FH;
        this.unk = unk;
    }

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getNewstate() {
        return newstate;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    @Override
    public short getFootHolds() {
        return foodholds;
    }

    public byte getUnk() {
        return unk;
    }
}
