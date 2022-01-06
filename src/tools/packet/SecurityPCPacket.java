/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import handling.SendPacketOpcode;
import tools.HexTool;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author tnhf0
 *
 * 472 pc 알림창 474 보안 창 내용 쏘는것. 475 내용변경 저장
 *
 *
 * flag 100 ~ 999 red and percent? 1000 yellow 10000
 */
public class SecurityPCPacket {

    public static byte[] sendSPC1() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(474);
        mplew.write(HexTool.getByteArrayFromHexString("02 00 00 00 00 00 2E 00 31 30 30 31 30 2D 30 38 36 45 34 39 2D 35 36 45 46 43 42 2D 32 38 30 38 36 30 2D 31 37 42 39 44 36 2D 31 45 39 42 35 45 39 2D 30 38 31 42 00 00 00 00 00 00 00 00 01 00 00"));
        return mplew.getPacket();
    }

    public static byte[] sendSPC2() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(477);
        mplew.write(HexTool.getByteArrayFromHexString("01 00 00 00 00 00 2E 00 31 30 30 30 30 2D 44 39 41 30 42 33 2D 36 44 45 38 42 39 2D 41 39 32 42 39 42 2D 34 37 38 35 41 30 2D 42 46 30 41 30 33 37 2D 30 42 43 42 00 00 01 02 08 0C 00 00 00 01 00 00 00 00 00 00 00 00 00 01 00 00 00 00 01 00"));
        return mplew.getPacket();
    }

    public static byte[] sendSPC3() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(476);
        //mplew.write(HexTool.getByteArrayFromHexString("04 00 00 00 3E F9 04 69 00 00 00 00 2E 00 31 30 30 30 30 2D 36 42 33 38 36 42 2D 30 43 36 33 45 39 2D 31 33 38 46 36 41 2D 37 44 39 46 46 35 2D 42 41 34 42 32 44 35 2D 30 37 35 30 00 08 00 55 6E 74 69 74 6C 65 64 CD 00 00 00 C0 43 AF 43 94 ED D5 01 A0 03 23 29 20 40 D6 01 00 00 00 00 00 A0 03 23 29 20 40 D6 01 B2 DB CC 27 00 00 00 00 3E F9 04 69 00 00 00 00 2E 00 31 30 30 30 30 2D 44 39 41 30 42 33 2D 36 44 45 38 42 39 2D 41 39 32 42 39 42 2D 34 37 38 35 41 30 2D 42 46 30 41 30 33 37 2D 30 42 43 42 00 06 00 B3 EB C6 AE BA CF E8 03 00 00 10 99 B1 2B CD 06 D6 01 A0 99 4F CD 22 40 D6 01 00 00 00 00 00 C0 2C 4E C0 C0 3E D6 01 4C ED 06 38 00 00 00 00 3E F9 04 69 00 00 00 00 06 00 31 30 32 30 34 39 01 0C 00 55 6E 74 69 74 6C 65 64 61 73 64 66 6E 02 00 00 F0 2F 88 06 F7 04 D6 01 90 E7 AA 68 76 3F D6 01 00 32 00 00 00 F0 2F 88 06 F7 04 D6 01 AA 7F 53 37 00 00 00 00 3E F9 04 69 00 00 00 00 06 00 31 31 33 36 34 34 01 08 00 55 6E 74 69 74 6C 65 64 A6 01 00 00 F0 3B CA 89 CC 0D D6 01 80 79 4C 43 7D 2B D6 01 00 00 00 00 00 F0 3B CA 89 CC 0D D6 01 AC 7B 0A 00 00 00 00 00"));
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00"));
        return mplew.getPacket();
    }

}
