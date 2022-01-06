/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Administrator
 */
public class MarriagePacket {

    public static byte[] onMarriage(int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(1571); // opcode

        return mplew.getPacket();
    }
}
