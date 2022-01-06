package server.movement;

import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;

public class AranMovement extends AbstractLifeMovement {

    public AranMovement(int type, Point position, int duration, int newstate, byte unk) {
        super(type, position, duration, newstate, (short) 0, unk);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter packet) {
        packet.write(getType());
        packet.write(getNewstate());
        packet.writeShort(getDuration());
        packet.write(getUnk());
    }
}
