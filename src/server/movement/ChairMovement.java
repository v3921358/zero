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

public class ChairMovement extends AbstractLifeMovement {

    private int unk;

    public ChairMovement(int type, Point position, int duration, int newstate, short unk, byte unk2) {
        super(type, position, duration, newstate, unk, unk2);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter packet) {
        packet.write(getType());
        packet.writePos(getPosition());
        packet.writeShort(getFootHolds());
        packet.writeInt(unk);
        packet.write(getNewstate());
        packet.writeShort(getDuration());
        packet.write(getUnk());
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }
}
