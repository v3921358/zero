/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import constants.GameConstants;
import handling.SendPacketOpcode;
import server.CashItemFactory;
import server.CashItemInfo.CashModInfo;
import server.CashShop;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import tools.HexTool;

public class CSPacket {

    public static byte[] warpCS(MapleClient c) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CS_OPEN.getValue());

        PacketHelper.addCharacterInfo(packet, c.getPlayer());
        packet.write(HexTool.getByteArrayFromHexString("39 00 8B 77 8E 06 04 00 00 00 00 00 00 00 0A 00 00 00 8C 77 8E 06 04 00 00 00 00 00 00 00 0A 00 00 00 C0 84 91 06 04 00 00 00 00 00 00 00 2C 10 00 00 C1 84 91 06 04 00 00 00 00 00 00 00 10 0E 00 00 C2 84 91 06 04 00 00 00 00 00 00 00 84 12 00 00 C3 84 91 06 04 00 00 00 00 00 00 00 34 17 00 00 C4 84 91 06 04 00 00 00 00 00 00 00 7C 0B 00 00 C5 84 91 06 04 00 00 00 00 00 00 00 24 09 00 00 C6 84 91 06 04 00 00 00 00 00 00 00 78 0F 00 00 36 0E 27 07 04 00 00 00 00 00 00 00 E0 10 00 00 37 0E 27 07 04 00 00 00 00 00 00 00 10 0E 00 00 BB 94 28 07 04 00 00 00 00 00 00 00 F8 16 00 00 BC 94 28 07 04 00 00 00 00 00 00 00 2C 10 00 00 27 A5 BF 07 04 00 00 00 00 00 00 00 2E 22 00 00 3E A5 BF 07 04 00 00 00 00 00 00 00 DA 2F 00 00 3F A5 BF 07 04 00 00 00 00 00 00 00 34 3A 00 00 40 A5 BF 07 04 00 00 00 00 00 00 00 08 52 00 00 50 A5 BF 07 04 00 00 00 00 00 00 00 DC 05 00 00 53 A5 BF 07 04 00 00 00 00 00 00 00 DC 05 00 00 54 A5 BF 07 04 00 00 00 00 00 00 00 10 0E 00 00 79 3C 58 08 10 00 00 00 00 00 00 00 6A 7A 3C 58 08 10 00 00 00 00 00 00 00 6A 86 3C 58 08 10 00 00 00 00 00 00 00 6A 8E 3C 58 08 10 00 00 00 00 00 00 00 6A D0 C3 59 08 04 00 00 00 00 00 00 00 4E 0C 00 00 D1 C3 59 08 04 00 00 00 00 00 00 00 C0 08 00 00 D2 C3 59 08 04 00 00 00 00 00 00 00 06 09 00 00 D3 C3 59 08 04 00 00 00 00 00 00 00 92 09 00 00 A7 C4 59 08 10 00 00 00 00 00 00 00 6A A8 C4 59 08 10 00 00 00 00 00 00 00 6A A9 C4 59 08 10 00 00 00 00 00 00 00 6A D2 C4 59 08 10 00 00 00 00 00 00 00 6A D3 C4 59 08 10 00 00 00 00 00 00 00 6A F2 C4 59 08 10 00 00 00 00 00 00 00 6A F3 C4 59 08 10 00 00 00 00 00 00 00 6A 22 49 5B 08 10 00 00 00 00 00 00 00 6A 23 49 5B 08 10 00 00 00 00 00 00 00 6A 3F D0 5C 08 04 00 00 00 00 00 00 00 34 08 00 00 40 D0 5C 08 04 00 00 00 00 00 00 00 34 08 00 00 41 D0 5C 08 04 00 00 00 00 00 00 00 34 08 00 00 42 D0 5C 08 04 00 00 00 00 00 00 00 06 09 00 00 43 D0 5C 08 04 00 00 00 00 00 00 00 AA 0A 00 00 CC D0 5C 08 10 00 00 00 00 00 00 00 6A CD D0 5C 08 10 00 00 00 00 00 00 00 6A CE D0 5C 08 10 00 00 00 00 00 00 00 6A CF D0 5C 08 10 00 00 00 00 00 00 00 6A D0 D0 5C 08 10 00 00 00 00 00 00 00 6A DD D0 5C 08 10 00 00 00 00 00 00 00 6A F4 D0 5C 08 10 00 00 00 00 00 00 00 6A F5 D0 5C 08 10 00 00 00 00 00 00 00 6A 89 56 5E 08 04 00 00 00 00 00 00 00 D6 06 00 00 EA DC 5F 08 04 00 00 00 00 00 00 00 32 05 00 00 BB 63 61 08 10 00 00 00 00 00 00 00 6A BC 63 61 08 10 00 00 00 00 00 00 00 6A C1 63 61 08 10 00 00 00 00 00 00 00 6A C8 63 61 08 10 00 00 00 00 00 00 00 6A B8 7D 67 08 10 00 00 00 00 00 00 00 6A 00 00 2F 00 00 00 4C 6D 54 00 06 00 00 00 19 63 3D 01 02 63 3D 01 05 63 3D 01 06 63 3D 01 0A 63 3D 01 0C 63 3D 01 64 6D 54 00 05 00 00 00 D1 70 64 08 CD 70 64 08 CE 70 64 08 CF 70 64 08 D0 70 64 08 54 6D 54 00 04 00 00 00 78 F6 41 01 79 F6 41 01 7A F6 41 01 7B F6 41 01 A4 6D 54 00 06 00 00 00 EF 70 64 08 EA 70 64 08 EB 70 64 08 EC 70 64 08 ED 70 64 08 EE 70 64 08 AC 6D 54 00 06 00 00 00 9D C4 59 08 9A C4 59 08 9B C4 59 08 9C C4 59 08 1C 49 5B 08 1D 49 5B 08 9C 6D 54 00 08 00 00 00 61 C4 59 08 62 C4 59 08 63 C4 59 08 64 C4 59 08 65 C4 59 08 66 C4 59 08 67 C4 59 08 68 C4 59 08 55 6D 54 00 09 00 00 00 2F 2F 31 01 30 2F 31 01 31 2F 31 01 32 2F 31 01 33 2F 31 01 34 2F 31 01 35 FE FD 02 36 FE FD 02 37 FE FD 02 A5 6D 54 00 06 00 00 00 86 C3 59 08 82 C3 59 08 83 C3 59 08 84 C3 59 08 85 C3 59 08 87 C3 59 08 5D 6D 54 00 05 00 00 00 3D 63 3D 01 3E 63 3D 01 3F 63 3D 01 40 63 3D 01 41 63 3D 01 AD 6D 54 00 06 00 00 00 A6 C4 59 08 A3 C4 59 08 A4 C4 59 08 A5 C4 59 08 20 49 5B 08 21 49 5B 08 B5 6D 54 00 06 00 00 00 C1 C4 59 08 84 3C 58 08 C2 C4 59 08 C3 C4 59 08 C4 C4 59 08 C5 C4 59 08 9D 6D 54 00 05 00 00 00 5D C4 59 08 0F 49 5B 08 5E C4 59 08 5F C4 59 08 60 C4 59 08 4E 6D 54 00 05 00 00 00 61 C3 59 08 5D C3 59 08 5E C3 59 08 03 C3 59 08 60 C3 59 08 66 6D 54 00 05 00 00 00 D2 70 64 08 D3 70 64 08 D4 70 64 08 D5 70 64 08 D6 70 64 08 56 6D 54 00 08 00 00 00 CE 2E 31 01 CF 2E 31 01 D0 2E 31 01 D1 2E 31 01 D2 2E 31 01 D3 2E 31 01 D4 2E 31 01 D5 2E 31 01 5E 6D 54 00 03 00 00 00 91 F6 41 01 92 F6 41 01 93 F6 41 01 96 6D 54 00 06 00 00 00 56 C4 59 08 51 C4 59 08 52 C4 59 08 53 C4 59 08 54 C4 59 08 55 C4 59 08 B6 6D 54 00 06 00 00 00 2A 49 5B 08 CC C4 59 08 CA C4 59 08 CB C4 59 08 C9 C4 59 08 2B 49 5B 08 4F 6D 54 00 05 00 00 00 18 2F 31 01 19 2F 31 01 1A 2F 31 01 1B 2F 31 01 1C 2F 31 01 87 6D 54 00 05 00 00 00 34 C4 59 08 02 49 5B 08 35 C4 59 08 36 C4 59 08 37 C4 59 08 6F 6D 54 00 08 00 00 00 12 C4 59 08 13 C4 59 08 14 C4 59 08 15 C4 59 08 16 C4 59 08 17 C4 59 08 18 C4 59 08 19 C4 59 08 5F 6D 54 00 05 00 00 00 71 2F 31 01 72 2F 31 01 73 2F 31 01 74 2F 31 01 75 2F 31 01 AF 6D 54 00 05 00 00 00 F2 70 64 08 B3 C4 59 08 B4 C4 59 08 B5 C4 59 08 B6 C4 59 08 B7 6D 54 00 06 00 00 00 2D 49 5B 08 D0 C4 59 08 CD C4 59 08 CE C4 59 08 CF C4 59 08 2C 49 5B 08 48 6D 54 00 08 00 00 00 CE 2E 31 01 CF 2E 31 01 D0 2E 31 01 D1 2E 31 01 D2 2E 31 01 D3 2E 31 01 D4 2E 31 01 D5 2E 31 01 60 6D 54 00 04 00 00 00 B7 C3 59 08 B8 C3 59 08 B9 C3 59 08 BA C3 59 08 50 6D 54 00 03 00 00 00 75 F6 41 01 76 F6 41 01 77 F6 41 01 A8 6D 54 00 05 00 00 00 51 C4 59 08 90 C4 59 08 91 C4 59 08 92 C4 59 08 93 C4 59 08 49 6D 54 00 06 00 00 00 03 63 3D 01 04 63 3D 01 07 63 3D 01 08 63 3D 01 09 63 3D 01 0B 63 3D 01 81 6D 54 00 05 00 00 00 E6 70 64 08 2E C4 59 08 2F C4 59 08 F5 F6 65 08 E5 70 64 08 51 6D 54 00 05 00 00 00 1F 2F 31 01 20 2F 31 01 21 2F 31 01 22 2F 31 01 23 2F 31 01 59 6D 54 00 04 00 00 00 4E A1 98 00 4F A1 98 00 50 A1 98 00 51 A1 98 00 71 6D 54 00 06 00 00 00 7A C3 59 08 1A C4 59 08 7B C3 59 08 7C C3 59 08 7D C3 59 08 7E C3 59 08 A9 6D 54 00 06 00 00 00 26 3C 58 08 77 3C 58 08 CC 3B 58 08 CD 3B 58 08 CE 3B 58 08 CF 3B 58 08 B1 6D 54 00 06 00 00 00 26 49 5B 08 B7 C4 59 08 B8 C4 59 08 B9 C4 59 08 27 49 5B 08 81 3C 58 08 B9 6D 54 00 06 00 00 00 DC C4 59 08 D7 C4 59 08 D8 C4 59 08 D9 C4 59 08 DA C4 59 08 DB C4 59 08 4A 6D 54 00 05 00 00 00 CC 3B 58 08 CD 3B 58 08 CE 3B 58 08 26 3C 58 08 CF 3B 58 08 62 6D 54 00 06 00 00 00 C3 C3 59 08 C4 C3 59 08 C5 C3 59 08 C6 C3 59 08 C7 C3 59 08 C8 C3 59 08 6A 6D 54 00 05 00 00 00 5D C3 59 08 5E C3 59 08 03 C3 59 08 60 C3 59 08 61 C3 59 08 A2 6D 54 00 0A 00 00 00 49 3C 58 08 4A 3C 58 08 4B 3C 58 08 4C 3C 58 08 4D 3C 58 08 4E 3C 58 08 4F 3C 58 08 50 3C 58 08 51 3C 58 08 52 3C 58 08 5A 6D 54 00 03 00 00 00 81 F6 41 01 82 F6 41 01 83 F6 41 01 C2 6D 54 00 0B 00 00 00 FD F6 65 08 33 49 5B 08 34 49 5B 08 EA C4 59 08 EB C4 59 08 EC C4 59 08 ED C4 59 08 A5 56 5E 08 05 DD 5F 08 F1 D0 5C 08 C7 63 61 08 7A 6D 54 00 05 00 00 00 89 7D 67 08 8A 7D 67 08 8B 7D 67 08 8C 7D 67 08 8D 7D 67 08 B2 6D 54 00 06 00 00 00 28 49 5B 08 BA C4 59 08 BB C4 59 08 BC C4 59 08 29 49 5B 08 82 3C 58 08 4B 6D 54 00 07 00 00 00 E5 2E 31 01 E6 2E 31 01 E7 2E 31 01 E8 2E 31 01 E9 2E 31 01 EA 2E 31 01 EB 2E 31 01 53 6D 54 00 05 00 00 00 27 2F 31 01 28 2F 31 01 29 2F 31 01 2A 2F 31 01 2B 2F 31 01 C3 6D 54 00 0B 00 00 00 35 49 5B 08 36 49 5B 08 EE C4 59 08 EF C4 59 08 F0 C4 59 08 F1 C4 59 08 A6 56 5E 08 06 DD 5F 08 F2 D0 5C 08 C6 63 61 08 FE F6 65 08 2B 00 00 00 04 CB FE 21 0A 02 80 77 8E 06 04 BC 77 8E 06 04 BD 77 8E 06 02 9A 77 8E 06 04 99 77 8E 06 04 E8 DE F3 08 00 EE DE F3 08 02 ED DE F3 08 02 A4 D1 F0 08 02 20 58 F2 08 04 B2 0C 25 0A 03 E1 85 23 0A 03 FB 85 23 0A 03 FC 85 23 0A 03 F2 C4 59 08 03 F3 C4 59 08 03 F4 D0 5C 08 03 F5 D0 5C 08 04 F6 B1 C2 07 02 AF D1 F0 08 02 2F 58 F2 08 02 48 0E 27 07 02 4A 0E 27 07 04 A2 FE 21 0A 04 76 A5 BF 07 02 78 A5 BF 07 04 79 A5 BF 07 04 B3 0C 25 0A 04 B4 0C 25 0A 04 B5 0C 25 0A 04 95 D1 F0 08 04 24 58 F2 08 04 8A FE 21 0A 02 8B FE 21 0A 04 8C FE 21 0A 04 82 A5 BF 07 04 61 86 23 0A 04 62 86 23 0A 04 BA D1 F0 08 04 BB D1 F0 08 04 BC D1 F0 08 04 BD D1 F0 08 05 00 00 00 04 CB FE 21 0A 04 98 FE 21 0A 04 C8 FE 21 0A 04 C9 FE 21 0A 04 CA FE 21 0A 03 00 00 00 04 00 B7 CE BE E2 42 00 B4 F5 BF ED B4 F5 20 BF B9 BB DA B0 ED 20 B8 DA C1 F8 20 C4 B3 B8 AF C5 CD B8 A6 20 BF F8 C7 D8 BF E4 3F 20 B1 D7 20 BA F1 B9 FD C0 CC 20 B1 C3 B1 DD C7 CF B8 E9 20 2D 2D 2D 2D 2D 3E 20 C5 AC B8 AF 0B 00 B8 C5 C1 F6 C4 C3 20 C7 CF C7 C1 53 00 C7 C7 B9 F6 C0 C7 20 C1 A4 BB F3 C0 BB 20 C7 E2 C7 D8 21 20 C7 C7 B9 F6 C0 C7 20 BC F8 B0 A3 BF A1 B4 C2 21 20 C1 C1 C0 BA 20 BE C6 C0 CC C5 DB C0 CC 20 C3 A3 BE C6 BF C3 20 B0 A1 B4 C9 BC BA C0 CC 20 B4 F5 BA ED B7 CE 20 55 50 B5 CB B4 CF B4 D9 2E 12 00 B8 DE C0 CC C7 C3 20 B7 CE BE E2 20 BD BA C5 B8 C0 CF 3E 00 B0 AD B7 C2 C7 D1 20 BF C9 BC C7 C0 CC 20 BA D9 BE EE C0 D6 B4 C2 20 B1 E2 B0 A3 C7 D1 C1 A4 20 C6 AF B1 DE 20 BE C6 C0 CC C5 DB B5 E9 C0 BB 20 B3 F5 C4 A1 C1 F6 20 B8 B6 BC BC BF E4 2E 2C 00 20 00 A4 C2 AC D0 20 00 AC C0 A9 C6 20 00 DC C2 20 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 58 C7 20 00 AC C7 AC C0 A9 C6 20 00 00 B3 30 AE DC C2 04 AC 20 00 08 CD 30 AE 54 D6 5C 00 6E 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 5C B8 20 00 F5 AC A9 AC 20 00 DC C2 20 00 F5 AC 04 AC 44 C7 20 00 A0 BC B4 C5 20 00 23 00 79 00 08 CD 20 00 D9 B3 48 C5 20 00 E0 AD F4 C5 44 C7 20 00 A8 B0 40 AE 2C 00 20 00 E0 AD F4 C5 74 C7 20 00 DD C0 31 C1 1C B4 20 00 C0 C9 ED C5 44 C7 20 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 5C B8 20 00 C4 AC 8D C1 20 00 F5 AC A9 AC 58 D5 74 BA 20 00 E0 AD F4 C5 74 C7 20 00 10 C8 28 CC 20 00 E4 CE C0 C9 70 BA 20 00 C8 B9 C0 C9 C9 B9 D0 C5 20 00 ED D3 1C BC 58 D5 EC C5 20 00 5C CD 00 B3 20 00 23 00 6D 00 6F 00 62 00 43 00 6F 00 75 00 6E 00 74 00 85 BA 58 C7 20 00 01 C8 44 C7 20 00 23 00 64 00 61 00 6D 00 61 00 67 00 65 00 25 00 58 C7 20 00 70 B3 F8 BB C0 C9 5C B8 20 00 23 00 61 00 74 00 74 00 61 00 63 00 6B 00 43 00 6F 00 75 00 6E 00 74 00 88 BC 20 00 F5 AC A9 AC 2C 00 20 00 E0 AD F4 C5 40 C7 20 00 D9 B3 DC C2 D0 C5 20 00 32 00 1C AC 20 00 74 C7 C1 C0 20 00 DD C0 31 C1 20 00 88 BD 00 AC 58 D5 E0 AC 20 00 ED D3 1C BC 20 00 C4 D6 20 00 23 00 7A 00 08 CD 20 00 C4 D6 D0 C5 20 00 AC C7 DD C0 31 C1 20 00 00 AC A5 B2 5C 00 6E 00 AC C7 AC C0 A9 C6 20 00 00 B3 30 AE DC C2 04 AC 20 00 23 00 63 00 6F 00 6F 00 6C 00 74 00 69 00 6D 00 65 00 20 00 08 CD 00 00 00 00 00 00 00 00 54 00 00 54 79 4F 22 00 D0 A0 E0 0A C8 CA BA 08 20 00 23 00 6D 00 70 00 43 00 6F 00 6E 00 20 00 8C C1 44 BE 2C 00 20 00 23 00 74 00 69 00 6D 00 65 00 08 CD 20 00 D9 B3 48 C5 20 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 58 C7 20 00 AC C7 AC C0 A9 C6 20 00 00 B3 30 AE DC C2 04 AC 74 C7 20 00 01 C8 A9 C6 18 B4 C0 C9 20 00 4A C5 3C C7 70 BA 20 00 28 CC C0 C9 58 D5 C0 C9 20 00 4A C5 44 C5 C4 B3 20 00 5C CD 00 B3 20 00 28 CC C0 C9 5C B8 20 00 1C BC D9 B3 58 D5 98 B0 20 00 5C CD 85 C8 20 00 70 B3 F8 BB C0 C9 20 00 35 00 30 00 25 00 20 00 10 AC 8C C1 2C 00 20 00 A4 C2 AC D0 20 00 AC C0 A9 C6 20 00 DC C2 20 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 58 C7 20 00 AC C7 AC C0 A9 C6 20 00 00 B3 30 AE DC C2 04 AC 20 00 08 CD 30 AE 54 D6 5C 00 6E 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 5C B8 20 00 F5 AC A9 AC 20 00 DC C2 20 00 F5 AC 04 AC 44 C7 20 00 A0 BC B4 C5 20 00 23 00 79 00 08 CD 20 00 D9 B3 48 C5 20 00 E0 AD F4 C5 44 C7 20 00 A8 B0 40 AE 2C 00 20 00 E0 AD F4 C5 74 C7 20 00 DD C0 31 C1 1C B4 20 00 C0 C9 ED C5 44 C7 20 00 6C D0 5C B8 A4 C2 20 00 54 B3 20 00 A4 C2 F1 D2 A4 C2 5C B8 20 00 C4 AC 8D C1 20 00 F5 AC A9 AC 58 D5 74 BA 20 00 E0 AD F4 C5 74 C7 20 00 10 C8 28 CC 20 00 E4 CE C0 C9 70 BA 20 00 C8 B9 C0 C9 C9 B9 D0 C5 20 00 ED D3 1C BC 58 D5 EC C5 20 00 5C CD 00 B3 20 00 23 00 6D 00 6F 00 62 00 43 00 6F 00 75 00 6E 00 74 00 85 BA 58 C7 20 00 01 C8 44 C7 20 00 23 00 64 00 61 00 6D 00 61 00 67 00 65 00 25 00 58 C7 20 00 70 B3 F8 BB C0 C9 5C B8 20 00 23 00 61 00 74 00 74 00 61 00 63 00 6B 00 43 00 6F 00 75 00 6E 00 74 00 88 BC 20 00 F5 AC A9 AC 2C 00 20 00 E0 AD F4 C5 40 C7 20 00 D9 B3 DC C2 D0 C5 20 00 32 00 1C AC 20 00 74 C7 C1 C0 20 00 DD C0 31 C1 20 00 88 BD 00 AC 58 D5 E0 AC 20 00 ED D3 1C BC 20 00 C4 D6 20 00 23 00 00 00 04 00 6E E3 8A 00 20 FE 8F 06 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 F4 01 00 00 F4 01 00 00 00 00 00 00 03 00 00 00 DC 2A 33 01 2A 2B 33 01 FF FF FF FF FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 3C 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 FF FF FF FF FF FF FF FF 00 31 39 30 0A 00 00 00 3B 78 33 01 9D 78 33 01 0C 00 00 00 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 67 65 7C 00 00 00 00 5A 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 FF FF FF FF FF FF FF FF 00 00 00 00 0A 00 00 00 82 78 33 01 9D 78 33 01 10 00 00 00 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 5A 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 FF FF FF FF FF FF FF FF 00 82 8D 59 0A 00 00 00 8D 78 33 01 9A 78 33 01 10 00 00 00 15 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 EB 49 25 00 00 00 00 78 00 00 00 00 00 01 00 00 00 00 BF 5C 00 D0 3C D5 01"));

        /*
         for (int i = 0; i < 10; i++) {
         packet.writeShort(0);
         packet.writeShort(0);
         packet.writeInt(0);
         }
         packet.writeInt(4);
         //for byte int

         packet.write(1);
         packet.writeInt(150000000);

         packet.write(2);
         packet.writeInt(150000001);

         packet.write(3);
         packet.writeInt(150100000);

         packet.write(4);
         packet.writeInt(150200000);
         //////////////
         packet.writeInt(0);
         packet.writeInt(0);
         packet.writeZeroBytes(1080);
         packet.writeShort(0);
         packet.writeShort(0);
         */
        return packet.getPacket();
    }

    public static void addModCashItemInfo(MaplePacketLittleEndianWriter mplew, CashModInfo item) {
        int flags = item.flags;
        mplew.writeInt(item.sn);
        mplew.writeInt(flags);
        if ((flags & 0x1) != 0) {
            mplew.writeInt(item.itemid);
        }
        if ((flags & 0x2) != 0) {
            mplew.writeShort(item.count);
        }
        if ((flags & 0x10) != 0) {
            mplew.write(item.priority);
        }
        if ((flags & 0x4) != 0) {
            mplew.writeInt(item.discountPrice);
        }
        if ((flags & 0x8) != 0) {
            mplew.write(item.unk_1 - 1);
        }
        if ((flags & 0x20) != 0) {
            mplew.writeShort(item.period);
        }
        if ((flags & 0x20000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x40000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x40) != 0) {
            mplew.writeInt(0); // maple point
        }
        if ((flags & 0x80) != 0) {
            mplew.writeInt(item.meso);
        }
        if ((flags & 0x100) != 0) {
            mplew.write(item.unk_2 - 1); // ForPremiumUser
        }
        if ((flags & 0x200) != 0) {
            mplew.write(item.gender);
        }
        if ((flags & 0x400) != 0) {
            mplew.write(item.showUp ? 1 : 0); // onSale
        }
        if ((flags & 0x800) != 0) {
            mplew.write(item.mark); // Class
        }
        if ((flags & 0x1000) != 0) {
            mplew.write(item.unk_3 - 1); // reqLevel
        }
        if ((flags & 0x2000) != 0) {
            mplew.writeShort(0); // PbCash
        }
        if ((flags & 0x4000) != 0) {
            mplew.writeShort(0); // PbPoint
        }
        if ((flags & 0x8000) != 0) {
            mplew.writeShort(0); // PbGift
        }
        if ((flags & 0x10000) != 0) {
            List<Integer> pack = CashItemFactory.getInstance().getPackageItems(item.sn);
            if (pack == null) {
                mplew.write(0);
            } else {
                mplew.write(pack.size());
                for (int i = 0; i < pack.size(); i++) {
                    mplew.writeInt(pack.get(i));
                }
            }
        }
        if ((flags & 0x80000) != 0) {
            // none
        }
        if ((flags & 0x100000) != 0) {
            // none
        }
        if ((flags & 0x200000) != 0) {
            mplew.write(0);
        }
    }

    public static byte[] showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        mplew.writeInt(chr.getCSPoints(1)); // NX Credit
        mplew.writeInt(chr.getCSPoints(2)); // MPoint
        mplew.writeInt(chr.getCSPoints(4)); // NX Prepaid
        return mplew.getPacket();
    }

    public static byte[] LunaCrystal(Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LUNA_CRYSTAL.getValue());
        mplew.write(true); // 324 ++
        PacketHelper.addItemInfo(mplew, item); // 324 ++
        mplew.writeInt(0x84);
        mplew.write(1);
        mplew.write(GameConstants.getInventoryType(item.getItemId()).getType());
        mplew.writeInt(item.getPosition());
        return mplew.getPacket();
    }

    public static byte[] WonderBerry(byte effect, Item item, int useitemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WONDER_BERRY.getValue());
        mplew.write(effect);
        mplew.writeInt(useitemid);
        PacketHelper.addItemInfo(mplew, item);
        return mplew.getPacket();
    }

    public static byte[] getCSInventory(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(6);
        CashShop mci = c.getPlayer().getCashInventory();
        int size = 0;
        mplew.writeShort(mci.getItemsSize());
        for (Item itemz : mci.getInventory()) {
            addCashItemInfo(mplew, itemz, c.getAccID(), 0);
        }
        if (mci.getInventory().size() > 0) {
            mplew.writeInt(0);
        }
        mplew.writeShort(c.getPlayer().getStorage().getSlots());
        mplew.writeInt(c.getCharacterSlots());
        mplew.writeShort(3);
        return mplew.getPacket();
    }

    public static byte[] getCSGifts(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(8); // 7 = Failed + transfer
        List<Pair<Item, String>> mci = c.getPlayer().getCashInventory().loadGifts();
        mplew.writeShort(mci.size());
        for (Pair<Item, String> mcz : mci) { // 70 Bytes, need to recheck.
            mplew.writeLong(mcz.getLeft().getUniqueId());
            mplew.writeInt(mcz.getLeft().getItemId());
            mplew.writeAsciiString(mcz.getLeft().getGiftFrom(), 13);
            mplew.writeAsciiString(mcz.getRight(), 73);
        }

        return mplew.getPacket();
    }

    public static byte[] sendWishList(MapleCharacter chr, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(update ? 12 : 10); // 9 = Failed + transfer, 16 = Failed.
        int[] list = chr.getWishlist();
        for (int i = 0; i < 12; i++) {
            mplew.writeInt(list[i] != -1 ? list[i] : 0);
        }

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItem(Item item, int sn, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(14);
        addCashItemInfo(mplew, item, accid, sn);
        mplew.writeZeroBytes(5);
        return mplew.getPacket();
    }

    public static byte[] showBoughtCSPackage(Map<Integer, Item> ccc, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(72); // 72 = Similar structure to showBoughtCSItemFailed
        mplew.write(ccc.size());
        for (Entry<Integer, Item> sn : ccc.entrySet()) {
            addCashItemInfo(mplew, sn.getValue(), accid, sn.getKey().intValue());
        }
        mplew.writeShort(0); // Purchase Maple Points = 1, Item = 0
        mplew.writeZeroBytes(5);
        return mplew.getPacket();
    }

    public static byte[] sendGift(int price, int itemid, int quantity, String receiver, boolean packages) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(packages ? 130 : 81); // 74 = Similar structure to showBoughtCSItemFailed
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);
        if (packages) {
            mplew.writeShort(0); //maplePoints
        }
        mplew.writeInt(price);

        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItem(Map<Integer, Item> items, int mesos, int maplePoints, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(61);
        mplew.write(items.size());
        for (Entry<Integer, Item> item : items.entrySet()) {
            addCashItemInfo(mplew, item.getValue(), c.getAccID(), item.getKey().intValue());
        }
        mplew.writeInt(maplePoints);
        mplew.writeInt(0); // Normal items size
        //for (Pair<Integer, Integer> item : items2) {
        //    mplew.writeInt(item.getRight()); // Count
        //    mplew.writeInt(item.getLeft());  // Item ID
        //}
        mplew.writeInt(mesos);

        return mplew.getPacket();
    }

    public static byte[] confirmFromCSInventory(Item item, short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x23); // 37 = Failed
        mplew.write(1);
        mplew.writeShort(pos);
        PacketHelper.addItemInfo(mplew, item);
//        mplew.writeInt(1);
//        mplew.writeInt(53888384);
        mplew.writeZeroBytes(10); //1.2.239+

        return mplew.getPacket();
    }

    public static byte[] confirmToCSInventory(Item item, int accId, int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x25); // 39 = Failed
        mplew.write(0);//306 異붽
        if (false) {
            mplew.writeLong(0);
            mplew.writeInt(0);
        } else {
            addCashItemInfo(mplew, item, accId, sn, false);
        }

        return mplew.getPacket();
    }

    public static byte[] cashItemExpired(int uniqueid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(95);
        mplew.writeLong(uniqueid);

        return mplew.getPacket();
    }

    public static byte[] sendBoughtRings(boolean couple, Item item, int sn, int accid, String receiver) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(couple ? 126 : 116);
        addCashItemInfo(mplew, item, accid, sn);
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(item.getItemId());
        mplew.writeShort(1); // Count

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSQuestItem(int price, short quantity, byte position, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(388); // 76 = Failed.
        mplew.writeInt(1); // size. below gets repeated for each.
        mplew.writeInt(quantity);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] updatePurchaseRecord() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(151); // 95 = Failed.
        mplew.writeInt(0);
        mplew.write(1); // boolean

        return mplew.getPacket();
    }

    public static byte[] sendRandomBox(long uniqueid, Item item, short pos) { // have to revise this
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(154); // 100 = Failed
        mplew.writeLong(uniqueid);
        mplew.writeInt(0);
        PacketHelper.addItemInfo(mplew, item);
        mplew.writeShort(pos);
        mplew.writeInt(0); // Item Size.->For each 8 bytes.

        return mplew.getPacket();
    }

    public static byte[] changeNameCheck(final String charname, final boolean nameUsed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_NAME_CHECK.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] changeNameResponse(final int mode, final int pic) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 0: Success
        // 1: The name change is already submitted \r\ndue to the item purchase
        // 2: This applies to the limitations on the request.\r\nPlease check if you were recently banned \r\nwithin 3 months.
        // 3: This applies to the limitations on the request.\r\nPlease check if you requested \r\nfor the name change within a month.
        // default: An unknown error has occured.
        mplew.writeShort(SendPacketOpcode.CHANGE_NAME_RESPONSE.getValue());
        mplew.writeInt(0);
        mplew.write(mode);
        mplew.writeInt(pic); // pic or birthdate

        return mplew.getPacket();
    }

    public static byte[] playCashSong(int itemid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CASH_SONG.getValue());
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] ViciousHammer(boolean start, boolean hammered) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(0);
        mplew.write(start ? 0 : 2);
//        mplew.writeInt(start ? 1 : 0);
        //      if (start) {
        mplew.writeInt(hammered ? 1 : 0);
        //    }

        return mplew.getPacket();
    }

    public static byte[] changePetFlag(int uniqueId, boolean added, int flagAdded) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_FLAG_CHANGE.getValue());

        mplew.writeLong(uniqueId);
        mplew.write(added ? 1 : 0);
        mplew.writeShort(flagAdded);

        return mplew.getPacket();
    }

    public static byte[] changePetName(MapleCharacter chr, String newname, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());

        mplew.writeInt(chr.getId());
        mplew.writeInt(slot);
        mplew.writeMapleAsciiString(newname);

        return mplew.getPacket();
    }

    public static byte[] OnMemoResult(final byte act, final byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        //08 // The note has successfully been sent
        //09 00 // The other character is online now. Please use the whisper function.
        //09 01 // Please check the name of the receiving character.
        //09 02 // The receiver's inbox is full. Please try again.
        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(act);
        if (act == 7 || act == 9) {
            mplew.write(mode);
        }

        return mplew.getPacket();
    }

    public static byte[] showNotes(final ResultSet notes, final int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(6);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.write(0); // 333++
            mplew.writeInt(0); // 333++
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(notes.getInt("gift"));
            
            mplew.writeMapleAsciiString("머시다냐1"); // 351 new

            mplew.writeMapleAsciiString("머시다냐2"); // 351 new
            mplew.writeInt(0); // 351 new
            mplew.writeInt(0); // 351 new
            mplew.write(0); // 351 new
            mplew.writeInt(0); // 351 new
            notes.next();
        }

        return mplew.getPacket();
    }

    public static byte[] useChalkboard(final int charid, final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());

        mplew.writeInt(charid);
        if (msg == null || msg.length() <= 0) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(msg);
        }

        return mplew.getPacket();
    }

    public static byte[] OnMapTransferResult(MapleCharacter chr, byte vip, boolean delete) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 29 00 05/08 00 // You cannot go to that place.
        // 29 00 06 00 // (null) is currently difficult to locate, so the teleport will not take place.
        // 29 00 09 00 // It's the map you're currently on.
        // 29 00 0A 00 // This map is not available to enter for the list.
        // 29 00 0B 00 // Users below level 7 are not allowed to go out from Maple Island.
        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(delete ? 2 : 3);
        mplew.write(vip);
        if (vip == 1) {
            int[] map = chr.getRegRocks();
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(map[i]);
            }
        } else if (vip == 2) {
            int[] map = chr.getRocks();
            for (int i = 0; i < 10; i++) {
                mplew.writeInt(map[i]);
            }
        } else if (vip == 3) {
            int[] map = chr.getHyperRocks();
            for (int i = 0; i < 13; i++) {
                mplew.writeInt(map[i]);
            }
        }

        return mplew.getPacket();
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, Item item, int accId, int sn) {
        addCashItemInfo(mplew, item, accId, sn, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, Item item, int accId, int sn, boolean isFirst) {
        addCashItemInfo(mplew, item.getUniqueId(), accId, item.getItemId(), sn, item.getQuantity(), item.getGiftFrom(), item.getExpiration(), isFirst); //owner for the lulz
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire) {
        addCashItemInfo(mplew, uniqueid, accId, itemid, sn, quantity, sender, expire, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire, boolean isFirst) {
        mplew.writeLong(uniqueid > 0 ? uniqueid : 0);
        mplew.writeInt(accId);
        mplew.writeInt(accId);
        mplew.writeInt(itemid);
        mplew.writeInt(isFirst ? sn : 0);
        mplew.writeShort(quantity);
        mplew.writeAsciiString(sender, 13);
        PacketHelper.addExpirationTime(mplew, expire);
        mplew.writeInt(isFirst ? 0 : sn);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(1);
        mplew.write(0);
        mplew.writeLong(0);
        mplew.writeLong(PacketHelper.getTime(-2));
        mplew.writeInt(0);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(0);
        }
    }

    public static byte[] sendCSFail(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(22);
        mplew.write(err);

        return mplew.getPacket();
    }

    public static byte[] enableCSUse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_USE.getValue());
        mplew.write(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] buyCharacterSlot() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x1F); // ++2
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] buyPendantSlot(short date) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x21); // ++2
        mplew.writeShort(0);
        mplew.writeShort(date);

        return mplew.getPacket();
    }

    public static byte[] enableUse() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CS_USE.getValue());
        packet.write(1);
        packet.writeInt(0);

        return packet.getPacket();
    }
}
