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

public class RelativeLifeMovement extends AbstractLifeMovement {

    private int nAttr;
    private Point v307;

    public RelativeLifeMovement(int type, Point position, int duration, int newstate, byte unk) {
        super(type, position, duration, newstate, (short) 0, unk);
    }

    public void setAttr(int nAttr) {
        this.nAttr = nAttr;
    }

    public void setV307(Point v307) {
        this.v307 = v307;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter packet) {
        packet.write(getType());
        packet.writePos(getPosition());
        if (getType() == 21 || getType() == 22) {
            packet.writeShort(nAttr);
        }
        if (getType() == 59) {
            packet.writePos(v307);
        }
        packet.write(getNewstate());
        packet.writeShort(getDuration());
        packet.write(getUnk());
    }
}
