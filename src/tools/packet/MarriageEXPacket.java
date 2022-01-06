/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;
import handling.SendPacketOpcode;
import handling.channel.handler.PlayerInteractionHandler;
import server.marriage.MarriageMiniBox;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Administrator
 */
public class MarriageEXPacket {

    public static byte[] MarriageRoom(MapleClient c, MarriageMiniBox marriage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(20);
        mplew.write(8); // gametype
        mplew.write(marriage.getMaxSize());
        mplew.writeShort(marriage.getVisitorSlot(c.getPlayer()));
        PacketHelper.addCharLook(mplew, marriage.getMCOwner(), false, GameConstants.isZero(c.getPlayer().getJob()) && c.getPlayer().getGender() == 1);
        mplew.writeMapleAsciiString(marriage.getOwnerName());
        mplew.writeShort(marriage.getMCOwner().getJob());
        mplew.writeInt(0);
        for (Pair<Byte, MapleCharacter> visitorz : marriage.getVisitors()) {
            mplew.write(visitorz.getLeft());
            PacketHelper.addCharLook(mplew, visitorz.getRight(), false, GameConstants.isZero(visitorz.right.getJob()) && visitorz.right.getGender() == 1);
            mplew.writeMapleAsciiString(visitorz.getRight().getName());
            mplew.writeShort(visitorz.getRight().getJob());
            mplew.writeInt(0);
        }
        mplew.write(-1);
        mplew.writeZeroBytes(10);
        return mplew.getPacket();
    }

    public static final byte[] MarriageVisit(final MapleCharacter chr, final int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(PlayerInteractionHandler.Interaction.VISIT.action);
        mplew.write(slot);
        PacketHelper.addCharLook(mplew, chr, false, GameConstants.isZero(chr.getJob()) && chr.getGender() == 1);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeShort(chr.getJob());
        mplew.writeInt(0);
        return mplew.getPacket();
    }
}
