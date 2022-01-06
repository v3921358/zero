/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import tools.HexTool;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author Syon
 */
public class ForceAtomParser {

    public static void main(String[] args) {
        byte[] data = HexTool.getByteArrayFromHexString("00 17 95 27 02 1C 00 00 00 01 0F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 09 D6 D7 17 01 02 00 00 00 00 00 00 00 2A 00 00 00 03 00 00 00 FF 00 00 00 D0 02 00 00 A3 FD FF FF 56 FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 03 00 00 00 00 00 00 00 29 00 00 00 03 00 00 00 0C 00 00 00 D0 02 00 00 52 FF FF FF 30 FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 04 00 00 00 00 00 00 00 2C 00 00 00 03 00 00 00 80 00 00 00 D0 02 00 00 03 FF FF FF D3 FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 05 00 00 00 00 00 00 00 2B 00 00 00 04 00 00 00 4F 01 00 00 D0 02 00 00 2E 00 00 00 A7 FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 06 00 00 00 00 00 00 00 2A 00 00 00 04 00 00 00 03 00 00 00 D0 02 00 00 CC FF FF FF C9 FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 07 00 00 00 00 00 00 00 29 00 00 00 03 00 00 00 19 01 00 00 D0 02 00 00 AD FF FF FF 8A FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 08 00 00 00 00 00 00 00 2C 00 00 00 04 00 00 00 5E 01 00 00 D0 02 00 00 8C FF FF FF CC FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 09 00 00 00 00 00 00 00 2A 00 00 00 03 00 00 00 DC 00 00 00 D0 02 00 00 96 00 00 00 9E FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 0A 00 00 00 00 00 00 00 29 00 00 00 03 00 00 00 2D 00 00 00 D0 02 00 00 B5 00 00 00 84 FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 0B 00 00 00 00 00 00 00 2A 00 00 00 03 00 00 00 37 00 00 00 D0 02 00 00 06 FF FF FF BE FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 0C 00 00 00 00 00 00 00 2B 00 00 00 03 00 00 00 25 00 00 00 D0 02 00 00 92 00 00 00 B7 FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 0D 00 00 00 00 00 00 00 2B 00 00 00 04 00 00 00 04 00 00 00 D0 02 00 00 3F FE FF FF CE FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 0E 00 00 00 00 00 00 00 2C 00 00 00 03 00 00 00 51 00 00 00 D0 02 00 00 83 00 00 00 74 FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 0F 00 00 00 00 00 00 00 2A 00 00 00 04 00 00 00 97 00 00 00 D0 02 00 00 3E 00 00 00 68 FF FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 01 10 00 00 00 00 00 00 00 29 00 00 00 03 00 00 00 3B 00 00 00 D0 02 00 00 8B FD FF FF BB FE FF FF 95 31 35 10 00 00 00 00 00 00 00 00 00 00 00 00 00 44 FD FF FF A8 FD FF FF BC 02 00 00 58 02 00 00 14 00 00 00");
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream((byte[]) data));
        //System.out.println("byte1 = " + slea.readByte());
        //System.out.println("Int = " + slea.readInt());
        //System.out.println("type = " + slea.readInt());
        //System.out.println("byte = " + slea.readByte());
        int lopsi = slea.readInt();
        //System.out.println("Objects = " + lopsi);
        for (int i = 0; i < lopsi; i++) {
            //System.out.println("Object[" + i + "] = " + slea.readInt());
        }
        //System.out.println("skillid = " + slea.readInt());
        while (slea.readByte() == 1) {
            //System.out.println("ForceAtom atom = new ForceAtom(0, " + slea.readInt() + ", " + slea.readInt() + ", " + slea.readInt() + ", " + slea.readInt() + ", " + slea.readInt() + ", " + slea.readInt() + ", new Point(" + slea.readInt() + ", " + slea.readInt() + "));");
            slea.skip(16);
        }
        //System.out.println("-pos.getX() = " + slea.readInt());
        //System.out.println("-pos.getY() = " + slea.readInt());
        //System.out.println("pos.getX() = " + slea.readInt());
        //System.out.println("pos.getY() = " + slea.readInt());
        //System.out.println("duration = " + slea.readInt());
        //System.out.println("available bytes = " + slea.available());
    }
}
