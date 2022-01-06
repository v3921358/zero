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
import constants.GameConstants;
import constants.JobConstants;
import constants.JobConstants.LoginJob;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import tools.HexTool;
import tools.data.MaplePacketLittleEndianWriter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginPacket {

    private static final String version;

    static {
        int ret = 0;
        ret ^= (ServerConstants.MAPLE_VERSION & 0x7FFF);
        ret ^= (1 << 15);
        ret ^= ((ServerConstants.MAPLE_PATCH & 0xFF) << 16);
        version = String.valueOf(ret);
    }

    public static final byte[] initializeConnection(final short mapleVersion, final byte[] sendIv, final byte[] recvIv,
                                                    final boolean ingame) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        int ret = 0;
        ret ^= (mapleVersion & 0x7FFF);
        ret ^= (ServerConstants.check << 15);
        ret ^= ((ServerConstants.MAPLE_PATCH & 0xFF) << 16);
        String version = String.valueOf(ret);

        int packetsize = ingame ? 15 : 43 + version.length();

        w.writeShort(packetsize);

        if (!ingame) {
            w.writeShort(291);
            w.writeMapleAsciiString(version);
            w.write(recvIv);
            w.write(sendIv);
            w.write(1); // locale
            w.write(0); // single thread loading
        }

        w.writeShort(291);
        w.writeInt(mapleVersion);
        w.write(recvIv);
        w.write(sendIv);
        w.write(1); // locale

        if (!ingame) {
            w.writeInt(mapleVersion * 100 + ServerConstants.MAPLE_PATCH);
            w.writeInt(mapleVersion * 100 + ServerConstants.MAPLE_PATCH); // next subversion
            w.writeInt(0); // unknown
            w.write(false);
            w.write(false);
        }

        return w.getPacket();
    }


    public static final byte[] getHotfix() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HOTFIX.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final byte[] WHITE_BACKGROUND_LODING() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHITE_BACKGROUND_LODING.getValue());
        mplew.write(0);
//        mplew.write(HexTool.getByteArrayFromHexString("0D 00 00 00 73 F8 FA D9 C3 DD CB DD C4 C8 00 00 00"));

        return mplew.getPacket();
    }

    public static final byte[] SessionCheck(int value) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);

        mplew.writeShort(SendPacketOpcode.SESSION_CHECK.getValue());
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static final byte[] HackShield() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.HACKSHIELD.getValue());
        mplew.write(1);
        mplew.write(0); // 324 ++

        return mplew.getPacket();
    }

    public static final byte[] EnableLogin() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.ENABLE_LOGIN.getValue());

        return mplew.getPacket();
    }

    public static final byte[] getPing() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);

        mplew.writeShort(SendPacketOpcode.PING.getValue());

        return mplew.getPacket();
    }

    public static final byte[] getAuthSuccessRequest(MapleClient client, final String id, String pwd) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0);
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(id);
        mplew.writeLong(-1); // 324 ++, Auction에도 쓰임 1090907166
        mplew.writeInt(client.getAccID());
        mplew.write(/*client.isGm() ? 1 : */0); // Admin byte - Find, Trade, etc.
        mplew.writeInt(/*client.isGm() ? 0xA2030 : */0); // gm flag
        /*
         *  int v68 = (v34 >> 4) & 1; // 0x10
         int v35 = (v34 >> 5) & 1; // 0x20
         int v36 = (v34 >> 13) & 1; // 0x2000
         int v69 = (v34 >> 19) & 1; // 0x80000
         int v80 = (v34 >> 17) & 1; // 0x20000
         */
        mplew.writeInt(/*client.getVIPGrade() */0); // MVP Grade
        mplew.writeInt(20); //  nAge
        mplew.write(/*client.purchaseExp()*/0); //nPurchaseExp
        mplew.write(client.getChatBlockedTime() > 0 ? 1 : 0); //ChatBlockReason
        mplew.writeLong(client.getChatBlockedTime()); //ChatUnBlockDate
        mplew.write(0); //고정값!
        mplew.writeMapleAsciiString(id); // pw
        mplew.write(0);
        mplew.writeMapleAsciiString(pwd);
        mplew.write(JobConstants.enableJobs);
        if (JobConstants.enableJobs) {
            mplew.write(JobConstants.jobOrder); //Job Order (orders are located in wz)

            for (LoginJob j : LoginJob.values()) { // 개수 1개 적음.
                mplew.write(j.getFlag());
                mplew.writeShort(j.getFlag()); //2 = 레벨 제한
            }
        }
        mplew.write(0);
        mplew.writeInt(-1);

        return mplew.getPacket();
    }

    public static final byte[] getLoginFailed(final int reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(reason);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static final byte[] getPermBan(final byte reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeShort(2); // Account is banned
        mplew.writeInt(0);
        mplew.writeShort(reason);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));

        return mplew.getPacket();
    }

    public static final byte[] getTempBan(final long timestampTill, final byte reason) {

        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter(17);

        w.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        w.write(2);
        w.write(HexTool.getByteArrayFromHexString("00 00 00 00 00"));
        w.write(reason);
        w.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.

        return w.getPacket();
    }

    public static final byte[] deleteCharResponse(final int cid, final int state) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);
        if (state == 69) {
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(0); // > 0 -> BUffer(16);
        } else if (state == 71) {
            mplew.write(0);
        }
        mplew.write(1); // 307++
        mplew.write(0); // 307++

        return mplew.getPacket();
    }

    public static final byte[] secondPwError(final byte mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.SECONDPW_ERROR.getValue());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] enableRecommended() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        mplew.writeInt(0); //worldID with most characters
        return mplew.getPacket();
    }

    public static byte[] sendRecommended(int world, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        mplew.write(message != null ? 1 : 0); //amount of messages
        if (message != null) {
            mplew.writeInt(world);
            mplew.writeMapleAsciiString(message);
        }
        return mplew.getPacket();
    }

    public static final byte[] getServerList(final int serverId, final Map<Integer, Integer> channelLoad) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(serverId);
        final String worldName = LoginServer.getServerName(); //remove the SEA
        mplew.writeMapleAsciiString(worldName);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(LoginServer.getFlag());
        mplew.writeMapleAsciiString(LoginServer.getEventMessage());

        mplew.write(0); // 350 new

        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel);
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (ChannelServer.getInstance(i) != null) {
                load = Math.max(1, ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters().size());
            } else {
                load = 1;
            }
            mplew.writeMapleAsciiString(worldName + ((i == 1) ? "-" + i : ((i == 2) ? "- 20세이상" : "-" + (i - 1))));
            mplew.writeInt(load);
            mplew.write(serverId);
            mplew.write(i - 1);
            mplew.write(0);//i == 2 ? 1 : 0); //20세 이상 입장.
        }
        mplew.writeShort(0); //size: (short x, short y, string msg)
        mplew.writeInt(0); //Offset?
        mplew.write(1); // 350 new
        mplew.writeInt(41); // 350 new
        for (int i = 40; i >= 0; i--) { // 350 new
            mplew.write(i);
            mplew.writeShort(1);
        }
        return mplew.getPacket();
    }

    public static final byte[] LeavingTheWorld() {
        MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.LEAVING_WORLD.getValue());
        w.write(3);
        w.writeMapleAsciiString("main");
        w.write(1);
        w.writeZeroBytes(8);
        w.writeMapleAsciiString("sub");
        w.writeZeroBytes(9);
        w.writeMapleAsciiString("sub_2");
        w.writeZeroBytes(9);
        w.write(1);
        return w.getPacket();
    }

    public static final byte[] getEndOfServerList() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);
        /* 1.2.287 광고 추가 */
        int advertisement = 0; //1.2.287(1) 기준.
        mplew.write(advertisement);
        for (int i = 0; i < advertisement; i++) {
            mplew.writeMapleAsciiString("");//http://maplestory.nexon.com/MapleStory/news/2016/login_Banner.html"); //띄울 이미지.
            mplew.writeMapleAsciiString("");//http://maplestory.nexon.com/MapleStory/news/2016/login_Banner.html"); //이동할 주소.
            mplew.writeInt(5000); // 시간
            mplew.writeInt(310); // width
            /* 광고 width, weight 고정 */
            mplew.writeInt(60); // height
            mplew.writeInt(235); // x
            mplew.writeInt(538); // y
        }
        /* 1.2.287 광고 추가 */
        mplew.write(0); // NotActiveAccountDlgFocus
        mplew.writeInt(49); // lockAccount Connection count.
        mplew.writeInt(-1); // 350 new

        return mplew.getPacket();
    }

    public static final byte[] getServerStatus(final int status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.writeShort(status);

        return mplew.getPacket();
    }

    public static final byte[] getCharList(final String secondpw, final List<MapleCharacter> chars, int charslots, byte nameChange) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHARLIST.getValue());
        mplew.write(0);
        mplew.writeShort(0); // 350 new
        mplew.writeInt(1);//++343
        mplew.writeInt(1);//++342
        mplew.writeInt(charslots); //trunkchar slotcount
        mplew.write(0);
        mplew.write(0); // 324 ++
        mplew.writeInt(0); //캐릭터 지워질때 유령되는 패킷ㅇㅇ
        mplew.writeLong(PacketHelper.getKoreanTimestamp(System.currentTimeMillis())); //v216
        mplew.write(0); //IsEditedList
        mplew.writeInt(chars.size());

        for (final MapleCharacter chr : chars) {
            mplew.writeInt(chr.getId());
        }

        mplew.write(chars.size());

        for (final MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, !chr.isGM() && chr.getLevel() >= 30, false);
        }

        mplew.write(secondpw != null && secondpw.length() > 0 ? 1 : (secondpw != null && secondpw.length() <= 0 ? 2 : 0)); // second pw request
        mplew.write(0);
        mplew.write(1); // 캐릭터 생성 2차비번체크여부 1 = 활성화, 0 = 비활성
        mplew.writeInt(charslots);

        mplew.writeInt(0); // 더블 클릭 UI 활성화 캐릭터 사이즈.
        mplew.writeInt(-1); // 신캐 이벤트. 카이저/엔버/듀얼블레이드/메카닉/데몬
        mplew.writeLong(PacketHelper.getKoreanTimestamp(System.currentTimeMillis()));
        mplew.write(1);
        mplew.write(nameChange); // 캐릭터 이름 변경
        boolean hasBurn = false;
        if (chars.size() > 0) {
            if (chars.get(0).getClient().getKeyValue("TeraBurning") != null) {
                hasBurn = true;
            }
        }
        mplew.write(hasBurn);
        mplew.write(0);
        mplew.writeInt(0); //273 ++
        mplew.writeInt(0); //273 ++
        mplew.writeInt(0); //332 ++
        return mplew.getPacket();
    }

    public static final byte[] addNewCharEntry(final MapleCharacter chr, final boolean worked) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(worked ? 0 : 1);
        mplew.writeInt(0); // worldID
        addCharEntry(mplew, chr, false, false);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final byte[] charNameResponse(final String charname, final boolean nameUsed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    private static final void addCharEntry(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr, boolean ranking, boolean viewAll) {
        PacketHelper.addCharStats(mplew, chr);
        mplew.writeInt(0); //273 ++
        mplew.writeInt(0); //338++
        mplew.writeLong(0);
        mplew.writeInt(0); // 350 new
        if (GameConstants.isZero(chr.getJob())) {
            byte gender = chr.getGender(), secondGender = chr.getSecondGender();

            chr.setGender((byte) 0);
            chr.setSecondGender((byte) 1);
            PacketHelper.addCharLook(mplew, chr, true, false);

            chr.setGender((byte) 1);
            chr.setSecondGender((byte) 0);
            PacketHelper.addCharLook(mplew, chr, true, false);

            chr.setGender(gender);
            chr.setSecondGender(secondGender);

        } else {
            PacketHelper.addCharLook(mplew, chr, true, false);
        }
    }

    public static final byte[] getSecondPasswordConfirm(byte op) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.AUTH_STATUS_WITH_SPW.getValue());
        mplew.write(op); // 0x48 "오늘은 더이상 캐릭터를 생성할 수 없습니다."
        if (op == 0) {
            // Jobs
            mplew.write(JobConstants.enableJobs ? 1 : 0); //toggle
            mplew.write(JobConstants.jobOrder); //Job Order (orders are located in wz)
            for (LoginJob j : LoginJob.values()) {
                mplew.write(j.getFlag());
                mplew.writeShort(j.getFlag()); //2 = 레벨 제한
            }
            // End of Jobs
        }
        return mplew.getPacket();
    }

    public static byte[] NewSendPasswordWay(MapleClient c) {
        MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();

        w.writeShort(SendPacketOpcode.NEW_PASSWORD_CHECK.getValue());
        w.write(c.getSecondPassword() != null ? 1 : 0);
        w.write(0);

        return w.getPacket();
    }

    public static final byte[] getSecondPasswordResult(final boolean success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.AUTH_STATUS_WITH_SPW_RESULT.getValue());
        mplew.write(success ? 0 : 0x14);
        return mplew.getPacket();
    }

    public static final byte[] MapleExit() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MAPLE_EXIT.getValue());
        return mplew.getPacket();
    }

    public static byte[] ChannelBackImg(boolean isSunday) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHANNEL_BACK_IMG.getValue());
        mplew.write(1);
//        if (isSunday) {
//            mplew.writeMapleAsciiString("sundayMaple");
//        } else {
            mplew.writeMapleAsciiString("Lara");
//        }
        mplew.writeInt(1);
        mplew.writeInt(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getSelectedChannelFailed(byte data, int ch) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SELECT_CHANNEL_LIST.getValue());
        mplew.write(data);
        mplew.writeShort(0); // 350 new
        mplew.writeInt(ch);
        mplew.writeInt(-1); // 332++

        return mplew.getPacket();
    }

    public static byte[] getSelectedChannelResult(int ch) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SELECT_CHANNEL_LIST.getValue());
        mplew.write(0);
        mplew.writeShort(0); // 350 new
        mplew.writeInt(ch);
        mplew.writeInt(-1); // 332++

        return mplew.getPacket();
    }

    public static byte[] getSelectedWorldResult(int world) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SELECTED_WORLD.getValue());

        mplew.writeInt(world);

        return mplew.getPacket();
    }

    public static final byte[] getKeyGuardResponse(String Key) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LOG_OUT.getValue());
        mplew.writeMapleAsciiString(Key);

        return mplew.getPacket();
    }

    public static final byte[] getAuthSuccessRequest(final MapleClient client) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        w.write(0);
        w.writeMapleAsciiString(client.getAccountName()); //sID
        w.writeLong(0); // 324 ++
        w.writeInt(client.getAccID());//dwAccountID
        w.write(client.getGender()); //Gender
        w.write(client.isGm() ? 1 : 0); //Admin byte
        w.writeInt(0); // admin flag
        w.writeInt(0); // vip grade
        w.writeInt(0); // nBlockReason
        w.write(client.getChatBlockedTime() > 0 ? 1 : 0); //pBlockReason.Interface //ChatBlockReason
        w.writeLong(client.getChatBlockedTime()); //ChatUnBlockDate
        if (true) { // NexonLogin
            w.write(1);
        } else {
            w.write(0);
            w.writeMapleAsciiString(client.getAccountName());
        }
        w.write(0);
        w.writeMapleAsciiString("");
        w.write(JobConstants.enableJobs);
        if (JobConstants.enableJobs) {
            w.write(JobConstants.jobOrder); //Job Order (orders are located in wz)

            for (LoginJob j : LoginJob.values()) { // 개수 1개 적음.
                w.write(j.getFlag());
                w.writeShort(j.getFlag()); //2 = 레벨 제한
            }
        }
        w.write(0);
        w.writeInt(-1);
        return w.getPacket();
    }

    public static final byte[] getCharEndRequest(final MapleClient client, String Acc, String Pwd, boolean Charlist) {
        final MaplePacketLittleEndianWriter w = new MaplePacketLittleEndianWriter();
        w.writeShort(SendPacketOpcode.CHAR_END_REQUEST.getValue());
        w.write(0);
        w.writeInt(client.getAccID());
        w.write(client.getGender());
        w.write(client.isGm() ? 1 : 0); // Admin byte
        w.writeZeroBytes(21);
        w.writeMapleAsciiString(Pwd); //패스워드.
        w.writeMapleAsciiString(Acc); //게임 아이디.
        w.writeShort(0);
        w.write(1);

        w.write(0x20); //1.2.250+
        for (int i = 0; i < 32; i++) {
            w.write(1);
            w.writeShort(1);
        }
        w.writeInt(-1);
        w.writeShort(Charlist ? 1 : 0);

        return w.getPacket();
    }

    public static byte[] OnOpcodeEncryption(int nBlockSize, byte[] aBuffer) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(43);

        mplew.writeInt(nBlockSize);
        mplew.writeInt(aBuffer.length);
        mplew.write(aBuffer);
        return mplew.getPacket();
    }
}
