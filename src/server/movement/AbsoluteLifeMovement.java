/*
 * ArcStory Project
 * 理쒖＜??sch2307@naver.com
 * ?댁? junny_adm@naver.com
 * ?곗???raccoonfox69@gmail.com
 * 媛뺤젙洹?ku3135@nate.com
 * 源吏꾪솉 designer@inerve.kr
 */
package server.movement;

import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;

public class AbsoluteLifeMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond, offset;
    private short v307;
    private int nAttr;

    public AbsoluteLifeMovement(int type, Point position, int duration, int newstate, short FH, byte unk) {
        super(type, position, duration, newstate, FH, unk);
    }

    public void setPixelsPerSecond(Point wobble) {
        this.pixelsPerSecond = wobble;
    }

    public void setOffset(Point wobble) {
        this.offset = wobble;
    }

    public void setnAttr(int nAttr) {
        this.nAttr = nAttr;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter packet) {
        packet.write(getType());
        packet.writePos(getPosition());
        packet.writePos(pixelsPerSecond);
        packet.writeShort(getFootHolds());
        if (getType() == 15 || getType() == 17) {
            packet.writeShort(nAttr);
        }
        packet.writePos(offset);
        packet.writeShort(v307);
        if (getType() != 73 && getType() != 75) {
            packet.write(getNewstate());
            packet.writeShort(getDuration());
            packet.write(getUnk());
        }
    }

    public short getV307() {
        return v307;
    }

    public void setV307(short v307) {
        this.v307 = v307;
    }
}
