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

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessor;
import constants.ServerConstants.CommandType;
import handling.RecvPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.World;
import java.util.ArrayList;
import java.util.List;
import log.DBLogger;
import log.LogType;
import server.MapleItemInformationProvider;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class ChatHandler {

    public static final void GeneralChat(final String text, final byte unk, final MapleClient c, final MapleCharacter chr, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
        if (text.length() > 0 && chr != null && chr.getMap() != null && !CommandProcessor.processCommand(c, text, CommandType.NORMAL)) {
            if (!chr.isIntern() && text.length() >= 80) {
                return;
            }

            int grade = (int) chr.getKeyValue(190823, "grade");

            String[] gradeNameList = { "뉴비", "Bronze", "Silver", "Gold", "Platinum", "Diamond", "Master", "Grand Master", "Challenger", "Overload" };
            String gradeName = gradeNameList[grade];

            if (c.getPlayer().getV("chatReq") != null) {
                int i = Integer.parseInt(c.getPlayer().getV("chatReq")) + 1;
                c.getPlayer().addKV("chatReq", "" + i);
            } else {
                c.getPlayer().addKV("chatReq", "1");
            }
            Item item = null;
            if (recv == RecvPacketOpcode.GENERAL_CHAT_ITEM) {
                byte invType = (byte) slea.readInt();
                byte pos = (byte) slea.readInt();
                item = c.getPlayer().getInventory(MapleInventoryType.getByType((pos > 0 ? invType : -1))).getItem(pos);
            }
            if (chr.getCanTalk() || chr.isStaff()) {
                //Note: This patch is needed to prevent chat packet from being broadcast to people who might be packet sniffing.
                if (chr.isHidden()) {
                    if (chr.isIntern() && !chr.isSuperGM() && unk == 0) {
                        chr.getMap().broadcastGMMessage(chr, CField.getChatText(chr, text, false, (byte) 1, item), true);
                        if (unk == 0) {
                            World.Broadcast.broadcastSmega(CWvsContext.serverNotice(3, c.getChannel(), chr.getName(), chr.getName() + " : " + text.substring(1), true));
                        }
                    } else {
                        chr.getMap().broadcastGMMessage(chr, CField.getChatText(chr, text, c.getPlayer().isSuperGM(), unk, item), true);
                    }
                } else {
                    if (chr.isIntern() && !chr.isSuperGM() && unk == 0) {
                        if (text.startsWith("~") && chr.isGM()) {
                            World.Broadcast.broadcastMessage(CField.getGameMessage(1, "[운영자] " + chr.getName() + "[W: " + String.valueOf(c.getChannel() == 0 ? 1 : c.getChannel()) + "] : " + text.substring(1)));
                        } else if (unk == 0 || text.startsWith("~")) {
                            int sex = c.getChannel() == 0 ? 1 : c.getChannel();
                            World.Broadcast.broadcastMessage(CField.getGameMessage(2, "채널 : <" + sex + "> [" + gradeName + "] " + chr.getName() + " : " + text.substring(1)));
                        } else {
                            chr.getMap().broadcastMessage(CField.getChatText(chr, text, false, (byte) 1, item), c.getPlayer().getTruePosition());
                        }
                    } else {
                        if (text.startsWith("~") && chr.isGM()) {
                            World.Broadcast.broadcastMessage(CField.getGameMessage(1, "[운영자] " + chr.getName() + "[W: " + String.valueOf(c.getChannel() == 0 ? 1 : c.getChannel()) + "] : " + text.substring(1)));
                        } else if (text.startsWith("~")) {
                            int sex = c.getChannel() == 0 ? 1 : c.getChannel();
                            World.Broadcast.broadcastMessage(CField.getGameMessage(2, "채널 : <" + sex + "> [" + gradeName + "] " + chr.getName() + " : " + text.substring(1)));
                        } else {
                            chr.getMap().broadcastMessage(CField.getChatText(chr, text, c.getPlayer().isSuperGM(), unk, item), c.getPlayer().getTruePosition());
                        }
                    }
                }
                DBLogger.getInstance().logChat(LogType.Chat.General, c.getPlayer().getId(), c.getPlayer().getName(), text, c.getPlayer().getMap().getStreetName() + " - " + c.getPlayer().getMap().getMapName() + " (" + c.getPlayer().getMap().getId() + ")");
            } else {
                c.getSession().writeAndFlush(CWvsContext.serverNotice(6, "대화 금지 상태이므로 채팅이 불가능합니다."));
            }
        }
    }

    public static final void Others(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final RecvPacketOpcode recv) {
        final int type = slea.readByte();
        final short numRecipients = slea.readShort();
        if (numRecipients <= 0) {
            return;
        }
        int recipients[] = new int[numRecipients];

        for (byte i = 0; i < numRecipients; i++) {
            recipients[i] = slea.readInt();
        }
        final String chattext = slea.readMapleAsciiString();
        if (chr == null || !chr.getCanTalk()) {
            c.getSession().writeAndFlush(CWvsContext.serverNotice(6, "", "You have been muted and are therefore unable to talk."));
            return;
        }

        if (c.isMonitored()) {
            String chattype = "Unknown";
            switch (type) {
                case 0:
                    chattype = "Buddy";
                    break;
                case 1:
                    chattype = "Party";
                    break;
                case 2:
                    chattype = "Guild";
                    break;
                case 3:
                    chattype = "Alliance";
                    break;
                case 4:
                    chattype = "Expedition";
                    break;
            }

        }
        if (chattext.equals("Unknown") || CommandProcessor.processCommand(c, chattext, CommandType.NORMAL)) {
            return;
        }
        switch (type) {
            case 0:
                World.Buddy.buddyChat(recipients, chr, chattext, slea, recv);
                break;
            case 1:
                if (chr.getParty() == null) {
                    break;
                }
                World.Party.partyChat(chr, chattext, slea, recv);
                break;
            case 2:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                World.Guild.guildChat(chr, chattext, slea, recv);
                break;
            case 3:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                World.Alliance.allianceChat(chr, chattext, slea, recv);
                break;
            /*            case 4:
             if (chr.getParty() == null || chr.getParty().getExpeditionId() <= 0) {
             break;
             }
             World.Party.expedChat(chr.getParty().getExpeditionId(), chattext, chr.getName(), slea, recv);
             break;*/
        }
    }

    public static void Messenger(final LittleEndianAccessor slea, final MapleClient c) {
        String input;
        MapleMessenger messenger = c.getPlayer().getMessenger();
        switch (slea.readByte()) {
            case 0x00: // 오픈
                if (messenger == null) {
                    byte available = slea.readByte();
                    int messengerid = slea.readInt();
                    if (messengerid == 0) { // create
                        c.getPlayer().setMessenger(World.Messenger.createMessenger(new MapleMessengerCharacter(c.getPlayer())));
                    } else { // join
                        messenger = World.Messenger.getMessenger(messengerid);
                        if (messenger != null) {
                            final int position = messenger.getLowestPosition();
                            if (messenger.getMembers().size() < available) {
                                if (position > -1 && position < 7) {
                                    c.getPlayer().setMessenger(messenger);
                                    World.Messenger.joinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()), c.getPlayer().getName(), c.getChannel());
                                }
                            } else {
                                c.getPlayer().dropMessage(5, "이미 해당 메신저는 최대 인원 입니다.");
                            }
                        }
                    }
                    break;
                }
            case 0x02: // 나가기
                if (messenger != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
                    World.Messenger.leaveMessenger(messenger.getId(), messengerplayer);
                    c.getPlayer().setMessenger(null);
                }
                break;
            case 0x03: // 초대
                if (messenger != null) {
                    final int position = messenger.getLowestPosition();
                    if (position <= -1 || position >= 7) {
                        return;
                    }
                    input = slea.readMapleAsciiString();
                    final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);

                    if (target != null) {
                        if (target.getMessenger() == null) {
                            if (!target.isIntern() || c.getPlayer().isIntern()) {
                                c.getSession().writeAndFlush(CField.messengerNote(input, 4, 1));
                                target.getClient().getSession().writeAndFlush(CField.messengerInvite(c.getPlayer().getName(), messenger.getId()));
                            } else {
                                c.getSession().writeAndFlush(CField.messengerNote(input, 4, 0));
                            }
                        } else {
                            c.getSession().writeAndFlush(CField.messengerChat(c.getPlayer().getName(), " : " + target.getName() + " is already using Maple Messenger."));
                        }
                    } else {
                        if (World.isConnected(input)) {
                            World.Messenger.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannel(), c.getPlayer().isIntern());
                        } else {
                            c.getSession().writeAndFlush(CField.messengerNote(input, 4, 0));
                        }
                    }
                }
                break;
            case 0x05: // 거절
                final String targeted = slea.readMapleAsciiString();
                final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) { // This channel
                    if (target.getMessenger() != null) {
                        target.getClient().getSession().writeAndFlush(CField.messengerNote(c.getPlayer().getName(), 5, 0));
                    }
                } else { // Other channel
                    if (!c.getPlayer().isIntern()) {
                        World.Messenger.declineChat(targeted, c.getPlayer().getName());
                    }
                }
                break;
            case 0x06: // 메세지
                if (messenger != null) {
                    final String charname = slea.readMapleAsciiString();
                    final String text = slea.readMapleAsciiString();
                    if (!c.getPlayer().isIntern() && text.length() >= 1000) {
                        return;
                    }
                    final String chattext = charname + "" + text;
                    World.Messenger.messengerChat(messenger.getId(), charname, text, c.getPlayer().getName());
                    if (messenger.isMonitored() && chattext.length() > c.getPlayer().getName().length() + 3) { //name : NOT name0 or name1
//                        World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "", "[GM Message] " + MapleCharacterUtil.makeMapleReadable(c.getPlayer().getName()) + "(Messenger: " + messenger.getMemberNamesDEBUG() + ") said: " + chattext));
                    }
                    DBLogger.getInstance().logChat(LogType.Chat.Messenger, c.getPlayer().getId(), c.getPlayer().getName(), chattext, "메신저 : " + messenger.getMemberNamesDEBUG());
                }
                break;
            case 0x09: //호감주기
                /*
                 [Send]
                 C6 03 
                 0A 00 
                 05 00 B6 D1 6F 49 50 
                 0C 00 C1 A6 B3 ED B0 FA C6 D2 C5 D2 32 31
                 */
                if (messenger != null) {
                    short like = slea.readShort();
                    String charname = slea.readMapleAsciiString();
                    MapleCharacter character = c.getChannelServer().getPlayerStorage().getCharacterByName(charname);
                    c.getSession().writeAndFlush(CField.messengerCharInfo(character));
                }
                break;
            case 0x0A: //guidance
                if (messenger != null) {
                    slea.readByte();
                    String charname = slea.readMapleAsciiString();
                    String targetname = slea.readMapleAsciiString();
                    //todo send guide packet here
                }
                break;
            case 0x0C: //캐릭터 정보
                if (messenger != null) {
                    String charname = slea.readMapleAsciiString();
                    MapleCharacter character = c.getChannelServer().getPlayerStorage().getCharacterByName(charname);
                    c.getSession().writeAndFlush(CField.messengerCharInfo(character));
                }
                break;
            case 0x0F: //귓속말
                if (messenger != null) {
                    final String charname = slea.readMapleAsciiString();
                    final String text = slea.readMapleAsciiString();
                    slea.readByte();
                    if (!c.getPlayer().isIntern() && text.length() >= 1000) {
                        return;
                    }
                    final String chattext = charname + "" + text;
                    World.Messenger.messengerWhisperChat(messenger.getId(), charname, text, c.getPlayer().getName());
                    if (messenger.isMonitored() && chattext.length() > c.getPlayer().getName().length() + 3) { //name : NOT name0 or name1
//                        World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "[GM Message] " + MapleCharacterUtil.makeMapleReadable(c.getPlayer().getName()) + "(Messenger: " + messenger.getMemberNamesDEBUG() + ") said: " + chattext));
                    }
                }
                break;
        }
    }

    public static final void Whisper_Find(final LittleEndianAccessor slea, final MapleClient c, RecvPacketOpcode recv) {
        final byte mode = slea.readByte();
        slea.readInt(); //ticks
        switch (mode) {
            case 34: {
                final String recipient = slea.readMapleAsciiString();
                MapleCharacter player = null;
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    player = cserv.getPlayerStorage().getCharacterByName(recipient);
                    if (player != null) {
                        break;
                    }
                }
                if (player != null) {
                    c.getSession().writeAndFlush(CField.getWhisperReply(c.getPlayer().getName(), (byte) 0x22, (byte) 0));
                }
                break;
            }
            case 68: //buddy
            case 5: { // Find

                final String recipient = slea.readMapleAsciiString();
                MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player != null) {
                    if (!player.isIntern() || c.getPlayer().isIntern() && player.isIntern()) {

                        c.getSession().writeAndFlush(CField.getFindReplyWithMap(player.getName(), player.getMap().getId(), mode == 68));
                    } else {
                        c.getSession().writeAndFlush(CField.getWhisperReply(recipient, (byte) 0));
                    }
                } else { // Not found
                    int ch = World.Find.findChannel(recipient);
                    if (ch > 0) {
                        player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(recipient);
                        if (player == null) {
                            break;
                        }
                        if (player != null) {
                            if (!player.isIntern() || (c.getPlayer().isIntern() && player.isIntern())) {
                                c.getSession().writeAndFlush(CField.getFindReply(recipient, (byte) ch, mode == 68));
                            } else {
                                c.getSession().writeAndFlush(CField.getWhisperReply(recipient, (byte) 0));
                            }
                            return;
                        }
                    }
                    if (ch == -10) {
                        c.getSession().writeAndFlush(CField.getFindReplyWithCS(recipient, mode == 68));
                    } else {
                        c.getSession().writeAndFlush(CField.getWhisperReply(recipient, (byte) 0));
                    }
                }
                break;
            }
            case 6: { // Whisper
                if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
                    return;
                }
                if (!c.getPlayer().getCanTalk()) {
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(6, "", "채팅 금지 상태입니다."));
                    return;
                }
                final String recipient = slea.readMapleAsciiString();

                Item item = null;
                if (recv == RecvPacketOpcode.WHISPERITEM) {//306추가됨
                    byte invType = (byte) slea.readInt();
                    byte pos = (byte) slea.readInt();
                    item = c.getPlayer().getInventory(MapleInventoryType.getByType((pos > 0 ? invType : -1))).getItem(pos);
                }
                final String text = slea.readMapleAsciiString();
                final int ch = World.Find.findChannel(recipient);
                if (ch > 0) {
                    MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(recipient);
                    if (player == null) {
                        break;
                    }
                    player.getClient().getSession().writeAndFlush(CField.getWhisper(c.getPlayer().getName(), c.getChannel(), text, item));
                    if (!c.getPlayer().isIntern() && player.isIntern()) {
                        c.getSession().writeAndFlush(CField.getWhisperReply(recipient, (byte) 0));
                    } else {
                        c.getSession().writeAndFlush(CField.getWhisperReply(recipient, (byte) 1));
                    }
                    /*                    if (c.isMonitored()) {
                     World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "", c.getPlayer().getName() + " whispered " + recipient + " : " + text));
                     } else if (player.getClient().isMonitored()) {
                     World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, c.getPlayer().getName() + " whispered " + recipient + " : " + text));
                     }*/
                    DBLogger.getInstance().logChat(LogType.Chat.Whisper, c.getPlayer().getId(), c.getPlayer().getName(), text, "대상 : " + recipient);
                } else {
                    c.getSession().writeAndFlush(CField.getWhisperReply(recipient, (byte) 0));
                }
            }
            break;
        }
    }
}
