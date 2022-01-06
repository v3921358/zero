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
package handling.channel.handler;

import client.*;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import handling.world.World.Find;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuddylistPacket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static client.BuddyList.BuddyOperation.ADDED;
import static client.BuddyList.BuddyOperation.DELETED;

public class BuddyListHandler {

    private static final class CharacterIdNameBuddyCapacity extends CharacterNameAndId {

        private int buddyCapacity;

        public CharacterIdNameBuddyCapacity(int id, int accId, String name, String repName, int level, int job, int buddyCapacity, String groupname, String memo) {
            super(id, accId, name, repName, level, job, groupname, memo);
            this.buddyCapacity = buddyCapacity;
        }

        public int getBuddyCapacity() {
            return buddyCapacity;
        }
    }

    private static final void nextPendingRequest(final MapleClient c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.getSession().writeAndFlush(BuddylistPacket.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getAccId(), pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob(), c, pendingBuddyRequest.getGroupName(), pendingBuddyRequest.getMemo()));
        }
    }

    private static final CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(final String name, String groupname, String memo) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        CharacterIdNameBuddyCapacity ret = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE name LIKE ?");
            ps.setString(1, name);
            rs = ps.executeQuery();

            if (rs.next()) {
                ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getInt("accountid"), rs.getString("name"), rs.getString("name"), rs.getInt("level"), rs.getInt("job"), rs.getInt("buddyCapacity"), groupname, memo);
            }
            rs.close();

            ps.close();
            con.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static final void BuddyOperation(final LittleEndianAccessor slea, final MapleClient c) {
        final int mode = slea.readByte();
        final BuddyList buddylist = c.getPlayer().getBuddylist();
        if (mode == 1) { // 친구 추가
            final String addName = slea.readMapleAsciiString();
            final int accid = MapleCharacterUtil.getAccByName(addName);
            final String groupName = slea.readMapleAsciiString();
            final String memo = slea.readMapleAsciiString();
            byte accountBuddyCheck = slea.readByte();
            String nickName = "";
            if (accountBuddyCheck == 1) {//계정친구일경우에만
                nickName = slea.readMapleAsciiString();
            }
            final BuddylistEntry ble = buddylist.get(accid);
            if (addName.length() > 13 || groupName.length() > 16 || nickName.length() > 13 || memo.length() > 260) {
                return;
            }
            if (ble != null && !ble.isVisible()) { //이미 친구로 등록되어 있습니다.
                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "이미 친구로 등록되어 있습니다."));
                return;
            } else if (buddylist.isFull()) { //친구리스트가 꽉 찼습니다.
                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "친구리스트가 꽉 찼습니다."));
                return;
            } else if (accid == c.getAccID()) {
                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "자기계정에 있는 캐릭터는 친구추가 하실 수 없습니다."));
                return;
            } else if (accountBuddyCheck == 0) {
                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "현재 이 기능은 사용하실 수 없습니다.\r\n아래 계정 통합 체크를 해주세요."));
                return;
            } else {
                try {
                    CharacterIdNameBuddyCapacity charWithId = null;
                    int channel;
                    final MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterByName(addName);
                    if (otherChar != null) {
                        channel = c.getChannel();
                        if (!otherChar.isGM() || c.getPlayer().isGM()) {
                            charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getAccountID(), otherChar.getName(), otherChar.getName(), otherChar.getLevel(), otherChar.getJob(), otherChar.getBuddylist().getCapacity(), groupName, memo);
                        }
                    } else {
                        channel = Find.findChannel(addName);
                        charWithId = getCharacterIdAndNameFromDatabase(addName, groupName, memo);
                    }

                    if (charWithId != null) {
                        BuddyAddResult buddyAddResult = null;
                        if (channel != -1) {
                            buddyAddResult = World.Buddy.requestBuddyAdd(addName, c.getAccID(), c.getChannel(), c.getPlayer().getId(), c.getPlayer().getName(), c.getPlayer().getLevel(), c.getPlayer().getJob(), groupName, memo);
                        } else {
                            Connection con = DatabaseConnection.getConnection();
                            PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE accid = ? AND pending = 0");
                            ps.setInt(1, charWithId.getAccId());
                            ResultSet rs = ps.executeQuery();

                            if (!rs.next()) {
                                ps.close();
                                rs.close();
                                throw new RuntimeException("Result set expected");
                            } else {
                                int count = rs.getInt("buddyCount");
                                if (count >= charWithId.getBuddyCapacity()) {
                                    buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                }
                            }
                            rs.close();
                            ps.close();

                            ps = con.prepareStatement("SELECT pending FROM buddies WHERE accid = ? AND buddyaccid = ?");
                            ps.setInt(1, charWithId.getAccId());
                            ps.setInt(2, c.getAccID());
                            rs = ps.executeQuery();
                            if (rs.next()) {
                                buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                            }
                            rs.close();
                            ps.close();
                            con.close();
                        }
                        if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) { //상대 친구추가창이 꽉 찼습니다.
                            c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "상대 친구목록이 꽉 찼습니다."));
                            return;
                        } else {
                            int displayChannel = -1;
                            int otherCid = charWithId.getId();

                            if ((buddyAddResult == BuddyAddResult.ALREADY_ON_LIST)) { //캐릭터가 오프일때도 친구가 있는지 없는지 체크
                                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "이미 대상의 친구목록에 캐릭터가 있습니다."));
                                return;
                                //notifyRemoteChannel(c, channel, otherCid, ADDED);
                            } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (`accid`, `buddyaccid`, `groupname`, `pending`, `memo`) VALUES (?, ?, ?, 1, ?)");
                                ps.setInt(1, charWithId.getAccId());
                                ps.setInt(2, c.getAccID());
                                ps.setString(3, groupName);
                                ps.setString(4, memo == null ? "" : memo);
                                ps.executeUpdate();
                                ps.close();
                                con.close();
                            }
                            buddylist.put(new BuddylistEntry(charWithId.getName(), charWithId.getName(), accid, otherCid, groupName, displayChannel, true, charWithId.getLevel(), charWithId.getJob(), memo));
                            c.getSession().writeAndFlush(BuddylistPacket.buddyAddMessage(addName)); //친구요청 메세지
                            c.getSession().writeAndFlush(BuddylistPacket.updateBuddylist(buddylist.getBuddies(), ble, (byte) 20));
                        }
                    } else { //캐릭터를 발견하지 못했습니다.
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "대상을 발견하지 못했습니다."));
                    }
                } catch (SQLException e) {
                    System.err.println("SQLError" + e);
                    e.printStackTrace();
                }
            }
        } else if (mode == 2 || mode == 3) { // 친구수락 (일반, 통합)
//            int otherCid = slea.readInt(); mode 2
            int otherAccId = slea.readInt();
            if (!buddylist.isFull()) {
                String otherName = null;
                String groupName = "그룹 미지정", otherMemo = "";
                int otherLevel = 0, otherJob = 0;
                int ch = World.Find.findAccChannel(otherAccId);
                if (ch > 0) {
                    MapleCharacter otherChar = ChannelServer.getInstance(ch).getPlayerStorage().getClientById(otherAccId).getPlayer();
                    if (otherChar == null) {
                        for (CharacterNameAndId ca : c.getPlayer().getBuddylist().getPendingRequests()) {
                            if (ca.getAccId() == otherAccId) {
                                otherName = ca.getName();
                                otherLevel = ca.getLevel();
                                otherJob = ca.getJob();
                                otherMemo = ca.getMemo();
                                break;
                            }
                        }
                    } else {
                        otherName = otherChar.getName();
                        otherLevel = otherChar.getLevel();
                        otherJob = otherChar.getJob();
                    }
                    if (otherName != null) {
                        BuddylistEntry ble = new BuddylistEntry(otherName, otherName, otherAccId, otherChar.getId(), groupName, otherChar.getClient().getChannel(), true, otherLevel, otherJob, otherMemo);
                        buddylist.put(ble);
                        c.getSession().writeAndFlush(BuddylistPacket.updateBuddylist(buddylist.getBuddies(), ble, (byte) 20));
                        notifyRemoteChannel(c, otherChar.getClient().getChannel(), otherChar.getId(), ADDED, otherMemo);
                    }
                }
            }
            nextPendingRequest(c);
//
        } else if (mode == 4) { // 상대방이 친추된 친구가 없을때 삭제
            final int accId = slea.readInt();

            if (buddylist.contains(accId)) {
                buddylist.remove(accId);
                c.getSession().writeAndFlush(BuddylistPacket.deleteBuddy(accId));
            }
            /*            int ch = World.Find.findAccChannel(accId);
             if (ch > 0) {
             MapleCharacter otherChar = ChannelServer.getInstance(ch).getPlayerStorage().getClientById(accId).getPlayer();
             if (buddylist.containsVisible(accId) && otherChar != null) {
             notifyRemoteChannel(c, otherChar.getClient().getChannel(), otherChar.getId(), DELETED, "");
             }
             }*/
            nextPendingRequest(c);
        } else if (mode == 5) { // 친추된 캐릭터 삭제
            final int accId = slea.readInt();

            if (buddylist.contains(accId)) {
                buddylist.remove(accId);
                c.getSession().writeAndFlush(BuddylistPacket.deleteBuddy(accId));
            }
            /*            int ch = World.Find.findAccChannel(accId);
             if (ch > 0) {
             MapleCharacter otherChar = ChannelServer.getInstance(ch).getPlayerStorage().getClientById(accId).getPlayer();
             if (buddylist.containsVisible(accId) && otherChar != null) {
             notifyRemoteChannel(c, otherChar.getClient().getChannel(), otherChar.getId(), DELETED, "");
             }
             }*/
            nextPendingRequest(c);
        } else if (mode == 6 || mode == 7) { // 거절(일반, 계정)
            int accId = slea.readInt();
            String otherMemo = "";
            int ch = World.Find.findAccChannel(accId);
            if (ch > 0) {
                MapleCharacter otherChar = ChannelServer.getInstance(ch).getPlayerStorage().getClientById(accId).getPlayer();
                if (buddylist.containsVisible(accId) && otherChar != null) {
                    notifyRemoteChannel(c, otherChar.getClient().getChannel(), otherChar.getId(), DELETED, otherMemo);
                }
                if (otherChar != null) {
                    otherChar.getClient().getSession().writeAndFlush(BuddylistPacket.buddyDeclineMessage(c.getPlayer().getName()));
                }
                buddylist.remove(accId);
                c.getSession().writeAndFlush(BuddylistPacket.deleteBuddy(accId));
                nextPendingRequest(c);
            }
        } else if (mode == 10) {//친구 늘리기
            if (c.getPlayer().getMeso() >= 50000 && c.getPlayer().getBuddyCapacity() < 100) {
                c.getPlayer().setBuddyCapacity((byte) (c.getPlayer().getBuddyCapacity() + 5));
                c.getPlayer().gainMeso(-50000, false);
            } else {
                c.getPlayer().dropMessage(1, "메소가 부족하거나 이미 친구 목록이 최대입니다.");
            }
            c.getSession().writeAndFlush(BuddylistPacket.updateBuddyCapacity(c.getPlayer().getBuddyCapacity()));
        } else if (mode == 11) { //계정친구 전환
            c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "현재 이 기능은 사용하실 수 없습니다."));
        } else if (mode == 12) { //별명, 메모 편집
            slea.skip(1);
            final int otherCid = slea.readInt();
            final int otherAccId = slea.readInt();
            final String charname = slea.readMapleAsciiString();
            final String memo = slea.readMapleAsciiString();
            final BuddylistEntry blz = buddylist.get(otherAccId);
            blz.setMemo(memo);
            c.getSession().writeAndFlush(BuddylistPacket.updateBuddylist(buddylist.getBuddies(), blz, (byte) 23));
        } else if (mode == 13) { //그룹 편집
            final int otherCid = slea.readInt();
            final String groupname = slea.readMapleAsciiString();
            final BuddylistEntry blz = buddylist.get(otherCid);
            blz.setGroupName(groupname);
            c.getSession().writeAndFlush(BuddylistPacket.updateBuddylist(buddylist.getBuddies(), blz, (byte) 23));
        } else if (mode == 14) { //그룹 편집
            final int otherCid = slea.readInt();
            final String groupname = slea.readMapleAsciiString();
            final BuddylistEntry blz = buddylist.get(otherCid);
            blz.setGroupName(groupname);
            c.getSession().writeAndFlush(BuddylistPacket.updateBuddylist(buddylist.getBuddies(), blz, (byte) 23));
        } else if (mode == 15) { //상태 변경 (오프라인)
            World.Buddy.loggedOff(c.getPlayer().getName(), c.getPlayer().getId(), c.getChannel(), c.getAccID(), buddylist.getBuddyIds());
            c.getPlayer().dropMessage(5, "오프라인 상태로 변경되었습니다.");
        }
    }

    private static final void notifyRemoteChannel(final MapleClient c, final int remoteChannel, final int otherCid, final BuddyOperation operation, String memo) {
        final MapleCharacter player = c.getPlayer();
        if (remoteChannel > 0) {
            World.Buddy.buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation, player.getLevel(), player.getJob(), c.getAccID(), memo);
        }
    }
}
