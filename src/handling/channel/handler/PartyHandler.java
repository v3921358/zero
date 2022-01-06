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
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import server.maps.Event_DojoAgent;
import server.maps.FieldLimitType;
import server.quest.MapleQuest;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.ExpeditionPacket;
import tools.packet.CWvsContext.PartyPacket;

import java.util.ArrayList;
import java.util.List;

public class PartyHandler {

    public static final void DenyPartyRequest(final LittleEndianAccessor slea, final MapleClient c) {
        final int action = slea.readByte();
        final int partyid = slea.readInt();
        if (c.getPlayer().getParty() == null && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null) {
                if (party.getExpeditionId() > 0) {
                    c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
                    return;
                }
                if (action == 0x27) {//was 31 // 35== inv
                    if (party.getMembers().size() < 6) {
                        c.getPlayer().setParty(party);
                        World.Party.updateParty(partyid, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                        World.Party.updateParty(partyid, PartyOperation.LOG_ONOFF, new MaplePartyCharacter(c.getPlayer()));
                        c.getPlayer().receivePartyMemberHP();
                        c.getPlayer().updatePartyMemberHP();
                    } else {
                        c.getPlayer().dropMessage(5, c.getPlayer().getName() + "님이 파티를 수락하셨습니다.");
                    }
                } else if (action == 0x26) {//30 == party join
                    MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeader().getId());
                    if (cfrom != null) {
                        cfrom.dropMessage(5, c.getPlayer().getName() + "님이 파티 초대를 거절하셨습니다.");
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "가입하려는 파티가 존재하지 않습니다.");
            }
        } else {
            c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있어 파티에 가입할 수 없습니다.");
        }

    }

    public static final void PartyOperation(final LittleEndianAccessor slea, final MapleClient c) {
        final int operation = slea.readByte();
        MapleParty party = c.getPlayer().getParty();
        MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());
        switch (operation) {
            case 1: // create
                final byte visible = slea.readByte();
                final String titlename = slea.readMapleAsciiString();
                if (party == null) {
                    party = World.Party.createParty(partyplayer);
                    party.setVisible(visible);
                    party.setPartyTitle(titlename);
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                } else {
                    if (party.getExpeditionId() > 0) {
                        c.getPlayer().dropMessage(5, "파티에 이미 가입되어 있어 파티를 만들 수 없습니다.");
                        return;
                    }
                    if (partyplayer.equals(party.getLeader()) && party.getMembers().size() == 1) { //only one, reupdate
                        c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                    } else {
                        c.getPlayer().dropMessage(5, "You can't create a party as you are already in one");
                    }
                }
                break;
            case 2: // 탈퇴
                if (party != null) { //are we in a party? o.O"
                    if (party.getExpeditionId() > 0) {
                        c.getPlayer().dropMessage(5, "원정대에 가입된 상태로는 탈퇴를 할 수 없습니다.");
                        return;
                    }
                    party.getVisible();
                    if (partyplayer.equals(party.getLeader())) { // disband
                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                    } else {
                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                        }
                    }
                    c.getPlayer().setParty(null);
                    c.getPlayer().setBlessingAnsanble((byte) 1);
                }
                break;
            case 3: // 초대 수락
                final int partyid = slea.readInt();
                if (party == null) {
                    party = World.Party.getParty(partyid);
                    if (party != null) {
                        if (party.getExpeditionId() > 0) {
                            c.getPlayer().dropMessage(5, "원정대에 가입된 상태로는 수락할 수 없습니다.");
                            return;
                        }
                        if (party.getMembers().size() < 6 && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.getPlayer().dropMessage(5, "해당 파티는 이미 파티원이 꽉 찼습니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "가입하려는 파티는 존재하지 않습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있어 파티에 가입할 수 없습니다.");
                }
                break;
            case 4: // 초대
                if (party == null) { //파티가 없을때 자동 만들기 및 초대
                    party = World.Party.createParty(partyplayer);
                    party.setPartyTitle(c.getPlayer().getName() + "님의 파티");
                    c.getPlayer().setParty(party);
                    party.setPartyTitle(party.getPatryTitle());
                    c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                    // TODO store pending invitations and check against them
                    final String theName = slea.readMapleAsciiString();
                    final int theCh = World.Find.findChannel(theName);
                    if (theCh > 0) {
                        final MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(theName);
                        if (invited != null && invited.getParty() == null && invited.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE)) == null) {
                            if (party.getExpeditionId() > 0) {
                                c.getPlayer().dropMessage(5, "You may not do party operations while in a raid.");
                                return;
                            }
                            if (party.getMembers().size() < 6) {
                                c.getPlayer().dropMessage(1, invited.getName() + "님을 파티에 초대했습니다.");
                                invited.getClient().getSession().writeAndFlush(PartyPacket.partyInvite(c.getPlayer()));
                            } else {
                                c.getPlayer().dropMessage(5, "이미 파티원이 최대로 가득 찬 상태입니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있는 대상입니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "대상을 찾지 못했습니다.");
                    }
                } else {
                    final String theName = slea.readMapleAsciiString();
                    final int theCh = World.Find.findChannel(theName);
                    if (theCh > 0) {
                        MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(theName);
                        if ((invited != null) && (invited.getParty() == null) && (invited.getQuestNoAdd(MapleQuest.getInstance(122901)) == null)) {
                            if (party.getExpeditionId() > 0) {
                                c.getPlayer().dropMessage(5, "원정대에 가입된 상태로는 파티초대를 할 수 없습니다.");
                                return;
                            }
                            if (party.getMembers().size() < 6) {
                                c.getPlayer().dropMessage(1, invited.getName() + "님을 파티에 초대했습니다.");
                                invited.getClient().getSession().writeAndFlush(CWvsContext.PartyPacket.partyInvite(c.getPlayer()));
                            } else {
                                c.getPlayer().dropMessage(5, "이미 파티원이 최대로 가득 찬 상태입니다.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "이미 파티에 가입되어 있는 대상입니다.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "대상을 찾지 못했습니다.");
                    }
                }
                break;
            case 6: // 추방
                if (party != null && partyplayer != null && partyplayer.equals(party.getLeader())) {
                    if (party.getExpeditionId() > 0) {
                        c.getPlayer().dropMessage(5, "원정대에 가입된 상태로는 추방을 할 수 없습니다.");
                        return;
                    }
                    final MaplePartyCharacter expelled = party.getMemberById(slea.readInt());
                    if (expelled != null) {
                        if (GameConstants.isDojo(c.getPlayer().getMapId()) && expelled.isOnline()) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        World.Party.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                        c.getPlayer().setBlessingAnsanble((byte) (c.getPlayer().getBlessingAnsanble() - 1));
                        if (c.getPlayer().getEventInstance() != null) {
                            /*if leader wants to boot someone, then the whole party gets expelled
                             TODO: Find an easier way to get the character behind a MaplePartyCharacter
                             possibly remove just the expellee.*/
                            if (expelled.isOnline()) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        }
                    }
                }
                break;
            case 7: // 리더 위임
                if (party != null) {
                    if (party.getExpeditionId() > 0) {
                        c.getPlayer().dropMessage(5, "원정대에 가입된 상태로는 파티장을 줄 수 없습니다.");
                        return;
                    }
                    final MaplePartyCharacter newleader = party.getMemberById(slea.readInt());
                    if (newleader != null && partyplayer.equals(party.getLeader())) {
                        World.Party.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newleader);
                    }
                }
                break;
            case 8: //allow party requests
                if (slea.readByte() > 0) {
                    c.getPlayer().getQuest_Map().remove(MapleQuest.getInstance(GameConstants.PARTY_REQUEST));
                } else {
                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PARTY_REQUEST));
                }
                break;
            case 13: // 파티명 변경
                final byte visible__ = slea.readByte();
                final String newTitle = slea.readMapleAsciiString();
                if (newTitle.length() < 0) {
                    c.getPlayer().dropMessage(1, "한 글자 이상의 변경할 파티명을 입력해주십시오.");
                    return;
                }
                party.setVisible(visible__);
                party.setPartyTitle(newTitle);
                c.getSession().writeAndFlush(PartyPacket.updateParty(c.getChannel(), party, PartyOperation.CHANGE_PARTY_TITLE, partyplayer));
                break;
            default:
                System.out.println("Unhandled Party function." + operation);
                break;
        }
    }

    public static final void AllowPartyInvite(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.readByte() > 0) {
            c.getPlayer().getQuest_Map().remove(MapleQuest.getInstance(GameConstants.PARTY_INVITE));
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PARTY_INVITE));
        }
    }

    public static final void MemberSearch(final LittleEndianAccessor slea, final MapleClient c) {
        if (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(5, "You may not do party search here.");
            return;
        }
        c.getSession().writeAndFlush(PartyPacket.showMemberSearch(c.getPlayer().getMap().getCharactersThreadsafe()));
    }

    public static final void PartySearch(final LittleEndianAccessor slea, final MapleClient c) {
        if (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())) {
            c.getPlayer().dropMessage(5, "You may not do party search here.");
            return;
        }
        List<MapleParty> parties = new ArrayList<MapleParty>();
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            if (chr.getParty() != null) {
                if (chr.getParty().getId() != c.getPlayer().getParty().getId() && !parties.contains(chr.getParty())) {
                    parties.add(chr.getParty());
                }
            }
        }
        c.getSession().writeAndFlush(PartyPacket.showPartySearch(parties));
    }

    public static final void PartyListing(final LittleEndianAccessor slea, final MapleClient c) {
        final int mode = slea.readByte();
        PartySearchType pst;
        MapleParty party;
        switch (mode) {
            case 81: //make
            case 0x9F:
            case -97:
            case -105:
                pst = PartySearchType.getById(slea.readInt());
                if (pst == null || c.getPlayer().getLevel() > pst.maxLevel || c.getPlayer().getLevel() < pst.minLevel) {
                    return;
                }
                if (c.getPlayer().getParty() == null && World.Party.searchParty(pst).size() < 10) {
                    party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), pst.id);
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                    final PartySearch ps = new PartySearch(slea.readMapleAsciiString(), pst.exped ? party.getExpeditionId() : party.getId(), pst);
                    World.Party.addSearch(ps);
                    if (pst.exped) {
                        c.getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(World.Party.getExped(party.getExpeditionId()), true));
                    }
                    c.getSession().writeAndFlush(PartyPacket.partyListingAdded(ps));
                } else {
                    c.getPlayer().dropMessage(1, "Unable to create. Please leave the party.");
                }
                break;
            case 83: //display
            case 0xA1:
            case -95:
            case -103:
                pst = PartySearchType.getById(slea.readInt());
                if (pst == null || c.getPlayer().getLevel() > pst.maxLevel || c.getPlayer().getLevel() < pst.minLevel) {
                    return;
                }
                c.getSession().writeAndFlush(PartyPacket.getPartyListing(pst));
                break;
            case 84: //close
            case 0xA2:
            case -94:
            case -102:
                break;
            case 85: //join
            case 0xA3:
            case -93:
            case -101:
                party = c.getPlayer().getParty();
                final MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());
                if (party == null) { //are we in a party? o.O"
                    final int theId = slea.readInt();
                    party = World.Party.getParty(theId);
                    if (party != null) {
                        PartySearch ps = World.Party.getSearchByParty(party.getId());
                        if (ps != null && c.getPlayer().getLevel() <= ps.getType().maxLevel && c.getPlayer().getLevel() >= ps.getType().minLevel && party.getMembers().size() < 6) {
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.JOIN, partyplayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.getSession().writeAndFlush(PartyPacket.partyStatusMessage(21, null));
                        }
                    } else {
                        MapleExpedition exped = World.Party.getExped(theId);
                        if (exped != null) {
                            PartySearch ps = World.Party.getSearchByExped(exped.getId());
                            if (ps != null && c.getPlayer().getLevel() <= ps.getType().maxLevel && c.getPlayer().getLevel() >= ps.getType().minLevel && exped.getAllMembers() < exped.getType().maxMembers) {
                                int partyId = exped.getFreeParty();
                                if (partyId < 0) {
                                    c.getSession().writeAndFlush(PartyPacket.partyStatusMessage(21, null));
                                } else if (partyId == 0) { //signal to make a new party
                                    party = World.Party.createPartyAndAdd(partyplayer, exped.getId());
                                    c.getPlayer().setParty(party);
                                    c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(exped, true));
//                                    World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                                    //                                  World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                } else {
                                    c.getPlayer().setParty(World.Party.getParty(partyId));
                                    World.Party.updateParty(partyId, PartyOperation.JOIN, partyplayer);
                                    c.getPlayer().receivePartyMemberHP();
                                    c.getPlayer().updatePartyMemberHP();
                                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(exped, true));
//                                    World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                                }
                            } else {
                                c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(0, c.getPlayer().getName()));
                            }
                        }
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    System.out.println("Unknown PartyListing : " + mode + "\n" + slea);
                }
                break;
        }
    }

    public static final void Expedition(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final int mode = slea.readByte();
        MapleParty part, party;
        String name;
        switch (mode) {
            case 64: //create [PartySearchID]
            case 134:
                //case 119:
                final ExpeditionType et = ExpeditionType.getById(slea.readInt());
                if (et != null && c.getPlayer().getParty() == null && c.getPlayer().getLevel() <= et.maxLevel && c.getPlayer().getLevel() >= et.minLevel) {
                    party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), et.exped);
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(World.Party.getExped(party.getExpeditionId()), true));
                } else {
                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(0, ""));
                }
                break;
            case 65: //invite [name]
            case 135:
                //case 120:
                name = slea.readMapleAsciiString();
                final int theCh = World.Find.findChannel(name);
                if (theCh > 0) {
                    final MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(name);
                    party = c.getPlayer().getParty();
                    if (invited != null && invited.getParty() == null && party != null && party.getExpeditionId() > 0) {
                        MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                        if (me != null && me.getAllMembers() < me.getType().maxMembers && invited.getLevel() <= me.getType().maxLevel && invited.getLevel() >= me.getType().minLevel) {
                            c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(7, invited.getName()));
                            invited.getClient().getSession().writeAndFlush(ExpeditionPacket.expeditionInvite(c.getPlayer(), me.getType().exped));
                        } else {
                            c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(3, invited.getName()));
                        }
                    } else {
                        c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(2, name));
                    }
                } else {
                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(0, name));
                }
                break;
            case 66: //accept invite [name] [int - 7, then int 8? lol.]
            case 136:
                // case 121:
                name = slea.readMapleAsciiString();
                final int action = slea.readInt();
                final int theChh = World.Find.findChannel(name);
                if (theChh > 0) {
                    final MapleCharacter cfrom = ChannelServer.getInstance(theChh).getPlayerStorage().getCharacterByName(name);
                    if (cfrom != null && cfrom.getParty() != null && cfrom.getParty().getExpeditionId() > 0) {
                        party = cfrom.getParty();
                        MapleExpedition exped = World.Party.getExped(party.getExpeditionId());
                        if (exped != null && action == 8) {
                            if (c.getPlayer().getLevel() <= exped.getType().maxLevel && c.getPlayer().getLevel() >= exped.getType().minLevel && exped.getAllMembers() < exped.getType().maxMembers) {
                                int partyId = exped.getFreeParty();
                                if (partyId < 0) {
                                    c.getSession().writeAndFlush(PartyPacket.partyStatusMessage(21, null));
                                } else if (partyId == 0) { //signal to make a new party
                                    party = World.Party.createPartyAndAdd(new MaplePartyCharacter(c.getPlayer()), exped.getId());
                                    c.getPlayer().setParty(party);
                                    c.getSession().writeAndFlush(PartyPacket.partyCreated(party));
                                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(exped, true));
//                                    World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                                    //                                  World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                } else {
                                    c.getPlayer().setParty(World.Party.getParty(partyId));
                                    World.Party.updateParty(partyId, PartyOperation.JOIN, new MaplePartyCharacter(c.getPlayer()));
                                    c.getPlayer().receivePartyMemberHP();
                                    c.getPlayer().updatePartyMemberHP();
                                    c.getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(exped, true));
//                                    World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                                }
                            } else {
                                c.getSession().writeAndFlush(ExpeditionPacket.expeditionError(3, cfrom.getName()));
                            }
                        } else if (action == 9) {
                            cfrom.getClient().getSession().writeAndFlush(PartyPacket.partyStatusMessage(23, c.getPlayer().getName()));
                        }
                    }
                }
                break;
            case 67: //leaving
            case 137:
                //case 122:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null) {
                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                            Event_DojoAgent.failed(c.getPlayer());
                        }
                        if (exped.getLeader() == c.getPlayer().getId()) { // disband
                            World.Party.disbandExped(exped.getId()); //should take care of the rest
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
                        } else if (part.getLeader().getId() == c.getPlayer().getId()) {
                            World.Party.updateParty(part.getId(), PartyOperation.DISBAND, new MaplePartyCharacter(c.getPlayer()));
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().disbandParty();
                            }
//                            World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionLeft(c.getPlayer().getName()), null);
                        } else {
                            World.Party.updateParty(part.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                            if (c.getPlayer().getEventInstance() != null) {
                                c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                            }
                            //                          World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionLeft(c.getPlayer().getName()), null);
                        }
                        c.getPlayer().setParty(null);
                    }
                }
                break;
            case 68: //kick [cid]
            case 138:
                //case 123:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int cid = slea.readInt();
                        for (int i : exped.getParties()) {
                            final MapleParty par = World.Party.getParty(i);
                            if (par != null) {
                                final MaplePartyCharacter expelled = par.getMemberById(cid);
                                if (expelled != null) {
                                    if (expelled.isOnline() && GameConstants.isDojo(c.getPlayer().getMapId())) {
                                        Event_DojoAgent.failed(c.getPlayer());
                                    }
                                    World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                    if (c.getPlayer().getEventInstance() != null) {
                                        if (expelled.isOnline()) {
                                            c.getPlayer().getEventInstance().disbandParty();
                                        }
                                    }
//                                    World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionLeft(expelled.getName()), null);
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            case 69: //give exped leader [cid]
            //case 124:
            case 139:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final MaplePartyCharacter newleader = part.getMemberById(slea.readInt());
                        if (newleader != null) {
                            World.Party.updateParty(part.getId(), PartyOperation.CHANGE_LEADER, newleader);
                            exped.setLeader(newleader.getId());
//                            World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionLeaderChanged(0), null);
                        }
                    }
                }
                break;
            case 70: //give party leader [cid]
            //case 125:
            case 140:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int cid = slea.readInt();
                        for (int i : exped.getParties()) {
                            final MapleParty par = World.Party.getParty(i);
                            if (par != null) {
                                final MaplePartyCharacter newleader = par.getMemberById(cid);
                                if (newleader != null && par.getId() != part.getId()) {
                                    World.Party.updateParty(par.getId(), PartyOperation.CHANGE_LEADER, newleader);
                                }
                            }
                        }
                    }
                }
                break;
            case 71: //change party of diff player [partyIndexTo] [cid]
            //case 126:
            case 141:
                part = c.getPlayer().getParty();
                if (part != null && part.getExpeditionId() > 0) {
                    final MapleExpedition exped = World.Party.getExped(part.getExpeditionId());
                    if (exped != null && exped.getLeader() == c.getPlayer().getId()) {
                        final int partyIndexTo = slea.readInt();
                        if (partyIndexTo < exped.getType().maxParty && partyIndexTo <= exped.getParties().size()) {
                            final int cid = slea.readInt();
                            for (int i : exped.getParties()) {
                                final MapleParty par = World.Party.getParty(i);
                                if (par != null) {
                                    final MaplePartyCharacter expelled = par.getMemberById(cid);
                                    if (expelled != null && expelled.isOnline()) {
                                        final MapleCharacter chr = World.getStorage(expelled.getChannel()).getCharacterById(expelled.getId());
                                        if (chr == null) {
                                            break;
                                        }
                                        if (partyIndexTo < exped.getParties().size()) { //already exists
                                            party = World.Party.getParty(exped.getParties().get(partyIndexTo));
                                            if (party == null || party.getMembers().size() >= 6) {
                                                c.getPlayer().dropMessage(5, "Invalid party.");
                                                break;
                                            }
                                        }
                                        if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                                            Event_DojoAgent.failed(c.getPlayer());
                                        }
                                        World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                        if (partyIndexTo < exped.getParties().size()) { //already exists
                                            party = World.Party.getParty(exped.getParties().get(partyIndexTo));
                                            if (party != null && party.getMembers().size() < 6) {
                                                World.Party.updateParty(party.getId(), PartyOperation.JOIN, expelled);
                                                chr.receivePartyMemberHP();
                                                chr.updatePartyMemberHP();
//                                                chr.getClient().getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(exped, true));
                                            }
                                        } else {
                                            party = World.Party.createPartyAndAdd(expelled, exped.getId());
                                            chr.setParty(party);
                                            chr.getClient().getSession().writeAndFlush(PartyPacket.partyCreated(party));
                                            //                                          chr.getClient().getSession().writeAndFlush(ExpeditionPacket.expeditionStatus(exped, true));
//                                            World.Party.expedPacket(exped.getId(), ExpeditionPacket.expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                        }
                                        if (c.getPlayer().getEventInstance() != null) {
                                            if (expelled.isOnline()) {
                                                c.getPlayer().getEventInstance().disbandParty();
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    System.out.println("Unknown Expedition : " + mode + "\n" + slea);
                }
                break;
        }
    }
}
