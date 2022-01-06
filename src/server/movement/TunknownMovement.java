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

public class TunknownMovement extends AbstractLifeMovement {

    private Point offset;

    public TunknownMovement(int type, Point position, int duration, int newstate, byte unk) {
        super(type, position, duration, newstate, (short) 0, unk);
    }

    public void setOffset(Point wobble) {
        this.offset = wobble;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter packet) {
        packet.write(getType());
        packet.writePos(getPosition());
        packet.writePos(offset);
        packet.write(getNewstate());
        packet.writeShort(getDuration());
        packet.write(getUnk());
    }
}
