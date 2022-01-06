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
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.World;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.MapleMonster;
import server.maps.*;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageEventAgent;
import server.marriage.MarriageManager;
import server.marriage.MarriageTicketType;
import server.polofritto.MapleRandomPortal;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.awt.*;
import java.util.List;

public class PlayersHandler {

    public static void Note(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final byte type = slea.readByte();

        switch (type) {
            case 0:
                System.out.printf("0");
                String name = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                System.out.printf("1");
                boolean fame = slea.readByte() > 0;
                //Item itemz = chr.getCashInventory().findByCashId((int) slea.readLong());
//                if (itemz == null || !itemz.getGiftFrom().equalsIgnoreCase(name) || !chr.getCashInventory().canSendNote(itemz.getUniqueId())) {
//                    return;
//                }
                try {
                    System.out.printf("2");
                    chr.sendNote(name, msg, fame ? 1 : 0);
                    //chr.getCashInventory().sendedNote(itemz.getUniqueId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                short num = slea.readShort();
                if (num < 0) { // note overflow, shouldn't happen much unless > 32767 
                    num = 32767;
                }
                slea.skip(1); // first byte = wedding boolean?
                for (int i = 0; i < num; i++) {
                    final int id = slea.readInt();
                    chr.deleteNote(id, slea.readByte() > 0 ? 1 : 0);
                }
                break;
            default:
                System.out.println("Unhandled note action, " + type + "");
        }
    }

    public static void GiveFame(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int who = slea.readInt();
        final int mode = slea.readByte();

        final int famechange = mode == 0 ? -1 : 1;
        final MapleCharacter target = chr.getMap().getCharacterById(who);

        if (target == null || target == chr) { // faming self
            c.getSession().writeAndFlush(CWvsContext.OnFameResult(1, target.getName(), false, target.getFame()));
            return;
        } else if (chr.getLevel() < 15) {
            c.getSession().writeAndFlush(CWvsContext.OnFameResult(2, target.getName(), false, target.getFame()));
            return;
        }
        switch (chr.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame() + famechange) <= 99999) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.FAME, target.getFame());
                }
                if (!chr.isGM()) {
                    chr.hasGivenFame(target);
                }
                c.getSession().writeAndFlush(CWvsContext.OnFameResult(0, target.getName(), famechange == 1, target.getFame()));
                target.getClient().getSession().writeAndFlush(CWvsContext.OnFameResult(5, chr.getName(), famechange == 1, 0));

                if (chr.getKeyValue(20220311, "ove6") < 0) {
                    chr.setKeyValue(20220311, "ove6", 0 + "");
                }

                if (chr.getKeyValue(20220311, "ove6") < 1) {
                    chr.setKeyValue(20220311, "ove6", (int) chr.getKeyValue(20220311, "ove6") + 1 + "");
                    //chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr, "#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 인기도 올리기 " + chr.getKeyValue(20220311, "ove6") + " 회 달성 !!", 0, 4));
                }

                break;
            case NOT_TODAY:
                c.getSession().writeAndFlush(CWvsContext.OnFameResult(3, target.getName(), false, target.getFame()));
                break;
            case NOT_THIS_MONTH:
                c.getSession().writeAndFlush(CWvsContext.OnFameResult(4, target.getName(), false, target.getFame()));
                break;
        }
    }

    public static void UseDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final boolean mode = slea.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town

        for (MapleMapObject obj : chr.getMap().getAllDoorsThreadsafe()) {
            final MapleDoor door = (MapleDoor) obj;
            if (door.getOwnerId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    public static void UseRandomDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        MapleRandomPortal portal = (MapleRandomPortal) chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.RANDOM_PORTAL);
        if (portal != null) {
            if (portal.getCharId() == chr.getId() && chr.getMapId() == portal.getMapId()) {
                if (portal.getPortalType() == 2) { // 폴로&프리토
                    NPCScriptManager.getInstance().start(chr.getClient(), portal.ispolo() ? 9001059 : 9001060, portal.ispolo() ? "poloEnter" : "FrittoEnter");
                } else if (portal.getPortalType() == 3) { // 불꽃늑대
                    NPCScriptManager.getInstance().start(chr.getClient(), 9001059, "FireWolfEnter");
                }
            }
        }
    }

    public static void UseMechDoor(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int oid = slea.readInt();
        final Point pos = slea.readPos();
        final int mode = slea.readByte(); // specifies if backwarp or not, 1 town to target, 0 target to town
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
        for (MapleMapObject obj : chr.getMap().getAllMechDoorsThreadsafe()) {
            final MechDoor door = (MechDoor) obj;
            if (door.getOwnerId() == oid && door.getId() == mode) {
                chr.checkFollow();
                chr.getMap().movePlayer(chr, pos);
                break;
            }
        }
    }

    public static void FollowRequest(final LittleEndianAccessor slea, final MapleClient c) {
        MapleCharacter tt = c.getPlayer().getMap().getCharacterById(slea.readInt());
        if (slea.readByte() > 0) {
            //1 when changing map
            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getFollowId() == c.getPlayer().getId()) {
                tt.setFollowOn(true);
                c.getPlayer().setFollowOn(true);
            } else {
                c.getPlayer().checkFollow();
            }
            return;
        }
        if (slea.readByte() > 0) { //cancelling follow
            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getFollowId() == c.getPlayer().getId() && c.getPlayer().isFollowOn()) {
                c.getPlayer().checkFollow();
            }
            return;
        }
        //if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && c.getPlayer().getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
        tt.setFollowId(c.getPlayer().getId());
        tt.setFollowOn(false);
        tt.setFollowInitiator(false);
        c.getPlayer().setFollowOn(false);
        c.getPlayer().setFollowInitiator(false);
        tt.getClient().getSession().writeAndFlush(CWvsContext.followRequest(c.getPlayer().getId()));
        //} else {
        //    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "너무 멀리 있습니다."));
        //}
    }

    public static void FollowReply(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getFollowId() > 0 && c.getPlayer().getFollowId() == slea.readInt()) {
            MapleCharacter tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getTruePosition().distanceSq(c.getPlayer().getTruePosition()) < 10000 && tt.getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
                boolean accepted = slea.readByte() > 0;
                if (accepted) {
                    tt.setFollowId(c.getPlayer().getId());
                    tt.setFollowOn(true);
                    tt.setFollowInitiator(false);
                    c.getPlayer().setFollowOn(true);
                    c.getPlayer().setFollowInitiator(true);
                    c.getPlayer().getMap().broadcastMessage(CField.followEffect(tt.getId(), c.getPlayer().getId(), null));
                } else {
                    c.getPlayer().setFollowId(0);
                    tt.setFollowId(0);
                    tt.getClient().getSession().writeAndFlush(CField.getFollowMsg(5));
                }
            } else {
                if (tt != null) {
                    tt.setFollowId(0);
                    c.getPlayer().setFollowId(0);
                }
                c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "너무 멀리 있습니다."));
            }
        } else {
            c.getPlayer().setFollowId(0);
        }
    }

    public static void HitReactor(final LittleEndianAccessor slea, final MapleClient c) {
        final int oid = slea.readInt();
        final int charPos = slea.readInt();
        final short stance = slea.readShort();
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(charPos, stance, c);
        if (c.getPlayer().getMapId() == 109090300) { //양떼 목장
            int rand = Randomizer.rand(1, 10), itemid = 0;
            if (rand <= 2) {
                itemid = 2022163;
            } else if (rand > 2 && rand <= 4) {
                itemid = 2022165;
            } else if (rand > 4 && rand <= 6) {
                itemid = 2022166;
            }
            c.getPlayer().getMap().destroyReactor(oid);
            Item idrop = new Item(itemid, (byte) 0, (short) (1), (byte) 0);
            c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), idrop, reactor.getPosition(), true, true);
        }
    }

    public static void TouchReactor(final LittleEndianAccessor slea, final MapleClient c) {

        if (c.getPlayer().getMapId() == 993000100) {
            slea.skip(4);
            final int oid = slea.readInt();
            MapleMonster m = c.getPlayer().getMap().getMonsterByOid(oid);
            if (m == null) {
                return;
            }
            c.getPlayer().getMap().killMonster(m);
            c.getPlayer().getDefenseTowerWave().attacked(c);
            return;
        }

        final int oid = slea.readInt();
        if (c.getPlayer().getNettPyramid() != null) {
            c.getPlayer().getNettPyramid().minusLife(slea.readInt());
            return;
        }
        final boolean touched = slea.available() == 0 || slea.readByte() > 0; //the byte is probably the state to set it to
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (!touched || reactor == null || !reactor.isAlive() || reactor.getTouch() == 0) {
            return;
        }
        if (c.getPlayer().getMap().getId() >= 105200310 && c.getPlayer().getMap().getId() <= 105200319 || c.getPlayer().getMap().getId() >= 105200710 && c.getPlayer().getMap().getId() <= 105200719) {
            switch (reactor.getState()) {
                case 0:
                    c.getPlayer().getMap().broadcastMessage(CField.startMapEffect("어머, 귀여운 손님들이 찾아왔네.", 5120099, true));
                    break;
                case 1:
                    c.getPlayer().getMap().broadcastMessage(CField.startMapEffect("무엄하다! 감히 대전을 함부로 드나들다니!", 5120100, true));
                    break;
                case 2:
                    c.getPlayer().getMap().broadcastMessage(CField.startMapEffect("킥킥, 여기가 죽을 자리인 줄도 모르고 왔구나.", 5120101, true));
                    break;
                case 3:
                    c.getPlayer().getMap().broadcastMessage(CField.startMapEffect("흑흑, 당신의 죽음을 미리 슬퍼해드리지요.", 5120102, true));
                    break;
            }
        }
        if (reactor.getTouch() == 2) {
            ReactorScriptManager.getInstance().act(c, reactor); //not sure how touched boolean comes into play
        } else if (reactor.getTouch() == 1 && !reactor.isTimerActive()) {
            if (reactor.getReactorType() == 100) {
                final int itemid = GameConstants.getCustomReactItem(reactor.getReactorId(), reactor.getReactItem().getLeft());
                if (c.getPlayer().haveItem(itemid, reactor.getReactItem().getRight())) {
                    if (reactor.getArea().contains(c.getPlayer().getTruePosition())) {
                        MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemid), itemid, reactor.getReactItem().getRight(), true, false);
                        reactor.hitReactor(c);
                    } else {
                        c.getPlayer().dropMessage(5, "너무 멀리 있습니다.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "You don't have the item required.");
                }
            } else {
                //just hit it
                reactor.hitReactor(c);
            }
        }
    }

    public static void SpaceReactor(final LittleEndianAccessor slea, final MapleClient c) {

        final int oid = slea.readInt();
        final boolean touched = slea.available() == 0 || slea.readByte() > 0; //the byte is probably the state to set it to
        final MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);

        if (reactor == null) {
            return;
        }
        ReactorScriptManager.getInstance().act(c, reactor); //not sure how touched boolean comes into play

    }

    public static void DoRing(final MapleClient c, final String name, final int itemid) {
        final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
        final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        int errcode = 0;
        if (c.getPlayer().getMarriageId() > 0) {
            errcode = 0x1B;
        } else if (chr == null) {
            errcode = 0x16;
        } else if (chr.getMapId() != c.getPlayer().getMapId()) {
            //errcode = 0x13;
            c.getPlayer().dropMessage(1, "상대방이 같은 맵에 없습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        } else if (!c.getPlayer().haveItem(itemid, 1) || itemid < 2240000 || itemid > 2240015) {
            errcode = 0x0F;
        } else if (chr.getMarriageId() > 0 || chr.getMarriageItemId() > 0) {
            errcode = 0x1C;
        } else if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "")) {
            errcode = 0x18;
        } else if (!MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
            errcode = 0x19;
        }
        if (errcode > 0) {
            c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) errcode, 0, null, null));
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        c.getPlayer().setMarriageItemId(itemid);
        chr.getClient().getSession().writeAndFlush(CWvsContext.sendEngagementRequest(c.getPlayer().getName(), c.getPlayer().getId()));
    }

    public static void RingAction(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        if (mode == 0) {
            DoRing(c, slea.readMapleAsciiString(), slea.readInt());
            //1112300 + (itemid - 2240004)
        } else if (mode == 1) {
            c.getPlayer().setMarriageItemId(0);
        } else if (mode == 2) { //accept/deny proposal
            final boolean accepted = slea.readByte() > 0;
            final String name = slea.readMapleAsciiString();
            final int id = slea.readInt();
            final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (c.getPlayer().getMarriageId() > 0 || chr == null || chr.getId() != id || chr.getMarriageItemId() <= 0 || !chr.haveItem(chr.getMarriageItemId(), 1) || chr.getMarriageId() > 0 || !chr.isAlive() || chr.getEventInstance() != null || !c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null) {
                c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x21, 0, null, null));
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (accepted) {
                final int itemid = chr.getMarriageItemId();
                final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
                if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                    c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x19, 0, null, null));
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }

                try {
                    MarriageDataEntry data = MarriageManager.getInstance().makeNewMarriage(chr.getId());
                    data.setStatus(1);
                    data.setGroomId(chr.getId());
                    data.setBrideId(c.getPlayer().getId());
                    data.setBrideName(c.getPlayer().getName());
                    data.setGroomName(chr.getName());

                    final int[] ringID = MapleRing.makeRing(newItemId, c.getPlayer(), chr);
                    Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[1]);
                    eq.setStr((short) 300);
                    eq.setDex((short) 300);
                    eq.setInt((short) 300);
                    eq.setLuk((short) 300);
                    eq.setWatk((short) 300);
                    eq.setMatk((short) 300);
                    MapleRing ring = MapleRing.loadFromDb(ringID[1]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(c, eq);

                    ring = MapleRing.loadFromDb(ringID[0]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(chr.getClient(), eq);

                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);

                    c.getPlayer().setMarriageId(data.getMarriageId());
                    chr.setMarriageId(data.getMarriageId());

                    chr.getClient().getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 13, newItemId, chr, c.getPlayer()));
                    c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 13, newItemId, c.getPlayer(), chr));

                    data.setEngagementTime(System.currentTimeMillis());
                } catch (Exception e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                }
                /*try {
                 final int[] ringID = MapleRing.makeRing(newItemId, c.getPlayer(), chr);
                 Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[1]);
                 MapleRing ring = MapleRing.loadFromDb(ringID[1]);
                 if (ring != null) {
                 eq.setRing(ring);
                 }
                 MapleInventoryManipulator.addbyItem(c, eq);

                 eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[0]);
                 ring = MapleRing.loadFromDb(ringID[0]);
                 if (ring != null) {
                 eq.setRing(ring);
                 }
                 MapleInventoryManipulator.addbyItem(chr.getClient(), eq);

                 MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);

                 chr.getClient().getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 13, newItemId, chr, c.getPlayer()));
                 chr.setMarriageId(c.getPlayer().getId());
                 c.getPlayer().setMarriageId(chr.getId());

                 chr.fakeRelog();
                 c.getPlayer().fakeRelog();
                 } catch (Exception e) {
                 FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                 }*/

            } else {
                chr.getClient().getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x22, 0, null, null));
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            chr.setMarriageItemId(0);
        } else if (mode == 3) { //drop, only works for ETC
            final int itemId = slea.readInt();
            final MapleInventoryType type = GameConstants.getInventoryType(itemId);
            final Item item = c.getPlayer().getInventory(type).findById(itemId);
            if (item != null && type == MapleInventoryType.ETC && itemId / 10000 == 421) {
                MapleInventoryManipulator.drop(c, type, item.getPosition(), item.getQuantity());
            }
        } else if (mode == 5) {
            // 청첩장 보내기
            // 73 00 05 04 00 BC BD BD BA 01 00 00 00 0C 00 00 00
            String receiver = slea.readMapleAsciiString();
            int marriageId = slea.readInt();
            int slot = slea.readInt();
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (data != null) {
                if (data.getMarriageId() == marriageId) {
                    Item invatation = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) slot);
                    if (invatation != null && invatation.getItemId() == data.getTicketType().getInvitationItemId()) {
                        // Send Invitation
                        int channel = World.Find.findChannel(receiver);
                        MapleCharacter chr = null;
                        if (channel >= 0) {
                            chr = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(receiver);
                        }
                        if (chr != null) {
                            if (data.getReservedPeopleList().contains(Integer.valueOf(chr.getId()))) {
                                c.getPlayer().dropMessage(1, "대상은 이미 결혼식에 초대되었습니다.");
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                                return;
                            }
                            MarriageTicketType type = data.getTicketType();
                            if (MapleInventoryManipulator.checkSpace(chr.getClient(), type.getInvitedItemId(), 1, "")) {
                                MapleCharacterUtil.sendNote(receiver, c.getPlayer().getName(), "Congratulations! 당신은 결혼식에 초대되었습니다! 기타창을 확인해주세요.", 0);
                                Item item = new Item(type.getInvitedItemId(), (short) 0, (short) 1, (short) 0);
                                item.setMarriageId(data.getMarriageId());

                                MapleInventoryManipulator.addbyItem(chr.getClient(), item);
                                c.getPlayer().dropMessage(1, "청첩장을 보냈습니다.");
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                                data.getReservedPeopleList().add(Integer.valueOf(chr.getId()));
                                MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, invatation.getItemId(), 1, true, false);
                            } else {
                                c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x19, 0, null, null));
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            }
                        } else {
                            c.getPlayer().dropMessage(1, "초대 받을 하객이 접속중이 아닙니다.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        }
                    }
                }
            }
        } else if (mode == 6) {
            // 청첩장 읽기
            // 73 00 06 07 00 00 00 22 45 40 00
            int slot = slea.readInt();
            int itemid = slea.readInt();
            Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem((short) slot);
            if (item != null && item.getItemId() == itemid && item.getMarriageId() > 0) {
                MarriageDataEntry data = MarriageManager.getInstance().getMarriage(item.getMarriageId());
                if (data != null) {
                    c.getSession().writeAndFlush(CWvsContext.showWeddingInvitation(data.getGroomName(), data.getBrideName(), data.getTicketType().getItemId() - 5251004));
                    return;
                }
            }
            c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x22, 0, null, null));
        } else if (mode == 9) {
            int wishes = slea.readByte();
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (data != null) {
                if (data.getStatus() == 1 && data.getWeddingStatus() >= 1 && data.getWeddingStatus() < 8) {
                    if (data.getGroomId() == c.getPlayer().getId()) {
                        if ((data.getStatus() & 2) > 0) {
                            return;
                        }
                        data.getGroomWishList().clear();
                        for (int i = 0; i < wishes; ++i) {
                            data.getGroomWishList().add(slea.readMapleAsciiString());
                        }
                    } else if (data.getBrideId() == c.getPlayer().getId()) {
                        if ((data.getStatus() & 4) > 0) {
                            return;
                        }
                        data.getBrideWishList().clear();
                        for (int i = 0; i < wishes; ++i) {
                            data.getBrideWishList().add(slea.readMapleAsciiString());
                        }
                    }
                    if (data.getGroomId() == c.getPlayer().getId()) {
                        MarriageTicketType type = data.getTicketType();
                        if (MapleInventoryManipulator.checkSpace(c, type.getInvitationItemId(), type.getInvitationQuantity(), "")) {
                            data.setWeddingStatus(data.getWeddingStatus() | 2);
                            if (data.getWeddingStatus() < 7) {
                                c.getPlayer().dropMessage(1, "위시리스트를 등록했습니다. 여성분이 위시리스트 등록을 끝낼 때 까지 잠시 기다려주세요.");
                            }
                        } else {
                            c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x18, 0, null, null));
                            data.getGroomWishList().clear();
                        }
                    } else if (data.getBrideId() == c.getPlayer().getId()) {
                        MarriageTicketType type = data.getTicketType();
                        if (MapleInventoryManipulator.checkSpace(c, type.getInvitationItemId(), type.getInvitationQuantity(), "")) {
                            data.setWeddingStatus(data.getWeddingStatus() | 4);
                            if (data.getWeddingStatus() < 7) {
                                c.getPlayer().dropMessage(1, "위시리스트를 등록했습니다. 남성분이 위시리스트 등록을 끝낼 때 까지 잠시 기다려주세요.");
                            }
                        } else {
                            c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x18, 0, null, null));
                            data.getBrideWishList().clear();
                        }
                    }
                    if (data.getWeddingStatus() == 7) {
                        // Do Register Reservation
                        MarriageTicketType type = data.getTicketType();
                        c.getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x12, 0, null, null)); // 예약 완료
                        MapleInventoryManipulator.addById(c, type.getInvitationItemId(), (short) type.getInvitationQuantity(), "");
                        //DBLogger.getInstance().logItem(LogType.Item.FromScript, c.getPlayer().getId(), c.getPlayer().getName(), type.getInvitationItemId(), type.getInvitationQuantity(), MapleItemInformationProvider.getInstance().getName(type.getInvitationItemId()), 0, "결혼식 예약 완료");
                        int channel = World.Find.findChannel(data.getPartnerId(c.getPlayer().getId()));
                        if (channel >= 0) {
                            MapleCharacter chr = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(data.getPartnerId(c.getPlayer().getId()));
                            if (chr != null) {
                                MapleInventoryManipulator.addById(chr.getClient(), type.getInvitationItemId(), (short) type.getInvitationQuantity(), "");
                                //DBLogger.getInstance().logItem(LogType.Item.FromScript, chr.getId(), chr.getName(), type.getInvitationItemId(), type.getInvitationQuantity(), MapleItemInformationProvider.getInstance().getName(type.getInvitationItemId()), 0, "결혼식 예약 완료");
                                chr.getClient().getSession().writeAndFlush(CWvsContext.sendEngagement((byte) 0x12, 0, null, null)); // 예약 완료
                            }
                        }
                        data.setMakeReservationTime(System.currentTimeMillis());
                    }

                }
            }
            // 결혼 선물 등록
            // [R] 73 00 09 08 00 04 00 BE C8 B3 C9 04 00 31 32 33 34 04 00 35 36 37 38 09 00 31 32 33 34 35 33 32 34 35 04 00 BE EE BE EE 06 00 BE EE C7 E3 C0 CC 04 00 C0 CC B7 B1 08 00 C0 E7 A4 C4 A4 A4 C0 E5
            // s......안냥..1234..5678..123453245..어어..어허이..이런..재ㅔㄴ장
        }
    }

    public static void Report(final LittleEndianAccessor slea, final MapleClient c) {
        byte type = slea.readByte();
        String name = slea.readMapleAsciiString();
        String desc = slea.readMapleAsciiString();

        MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(name);

        if (target != null) {
            if (System.currentTimeMillis() - c.getPlayer().lastReportTime >= 60 * 10 * 1000) {
                c.getSession().writeAndFlush(CWvsContext.report(2));
                String data = c.getPlayer().getName() + " 캐릭터가 " + target.getName() + " 캐릭터를 " + target.getMapId() + " 맵에서 신고 | 타입 : " + type + " / 내용 : " + desc + "\r\n\r\n";
                NPCConversationManager.writeLog("Log/Report.log", data, true);
            } else {
                c.getPlayer().dropMessage(1, "10분마다 신고하실 수 있습니다.");
            }
        } else {
            c.getPlayer().dropMessage(1, "대상을 찾을 수 없습니다.");
        }
    }

    public static final void StealSkill(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || !GameConstants.isPhantom(c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final int skill = slea.readInt();
        final int cid = slea.readInt();
        final byte action = slea.readByte();
        //then a byte, 0 = learning, 1 = removing, but it doesnt matter since we can just use cid
        if (action == 0) {
            final MapleCharacter other = c.getPlayer().getMap().getCharacterById(cid);
            if (other != null && other.getId() != c.getPlayer().getId() && other.getTotalSkillLevel(skill) > 0) {
                c.getPlayer().addStolenSkill(skill, other.getTotalSkillLevel(skill));
            } else {
                c.getPlayer().dropMessage(1, "상대방이 해당 스킬을 올리지 않았습니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } else if (action == 1) {
            c.getPlayer().removeStolenSkill(skill);
        }
    }

    public static final void ChooseSkill(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || !GameConstants.isPhantom(c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final int base = slea.readInt();
        final int skill = slea.readInt();
        if (skill <= 0) {
            c.getPlayer().unchooseStolenSkill(base);
        } else {
            c.getPlayer().chooseStolenSkill(skill);
        }
    }

    public static final void viewSkills(final LittleEndianAccessor slea, final MapleClient c) {
        int victim = slea.readInt();
        int jobid = c.getChannelServer().getPlayerStorage().getCharacterById(victim).getJob();
        if (!c.getChannelServer().getPlayerStorage().getCharacterById(victim).getSkills().isEmpty() && GameConstants.isAdventurer(jobid)) {
            c.getSession().writeAndFlush(CField.viewSkills(c.getChannelServer().getPlayerStorage().getCharacterById(victim)));
        } else {
            c.getPlayer().dropMessage(6, "훔칠 수 있는 스킬이 없습니다.");
        }
    }

    private static MapleMonster pvpMob;

    public static boolean inArea(MapleCharacter chr) {
        for (Rectangle rect : chr.getMap().getAreas()) {
            if (rect.contains(chr.getTruePosition())) {
                return true;
            }
        }
        for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
            if (mist.getOwnerId() == chr.getId() && mist.isPoisonMist() == 2 && mist.getBox().contains(chr.getTruePosition())) {
                return true;
            }
        }
        return false;
    }

    public static final void TouchRune(final LittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(4);
        int type = slea.readInt();
        MapleRune rune = chr.getMap().getRune();

        long time = System.currentTimeMillis();

        if (rune != null && rune.getRuneType() == type) {
            if (time - chr.getRuneTimeStamp() >= 5 * 1000) {
                chr.setRuneTimeStamp(time);
                chr.setTouchedRune(type);
                chr.getClient().getSession().writeAndFlush(CField.RuneAction(9, 0));
            } else {
                chr.getClient().getSession().writeAndFlush(CField.RuneAction(2, (int) ((5 * 1000) - (time - chr.getRuneTimeStamp()))));
            }
        }
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static final void UseRune(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int result = slea.readInt();
        final int a = slea.readInt();
        final MapleRune rune = chr.getMap().getRune();
        MapleStatEffect effect;
        if (chr.getBuffedValue(80002282) && !chr.isGM()) {
            chr.dropMessage(1, "룬 재사용 대기시간 중에는 룬을 사용하실 수 없습니다.");
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        if (result == 3 && a == 3) {
            chr.getMap().broadcastMessage(CField.showRuneEffect(chr.getTouchedRune()));
            chr.getMap().broadcastMessage(CField.removeRune(rune, chr));
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
            effect = SkillFactory.getSkill(80002280).getEffect(1);
            effect.applyTo(chr, false);
            switch (chr.getTouchedRune()) {
                case 0: //신속의 룬
                    effect = SkillFactory.getSkill(80001427).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
                case 1: //재생의 룬
                    effect = SkillFactory.getSkill(80001428).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
                case 2: //파멸의 룬
                    effect = SkillFactory.getSkill(80001432).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
                case 3: //천둥의 룬
                    effect = SkillFactory.getSkill(80001752).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
                case 4: //지진의 룬
                    effect = SkillFactory.getSkill(80001757).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
                case 5: //어둠의 룬
//                    effect = SkillFactory.getSkill(80001432).getEffect(1);
//                    effect.applyTo(chr);
                    break;
                case 6: //보물의 룬
                    chr.보물의룬시작();
                    break;
                case 7: //초월의 룬
//                    effect = SkillFactory.getSkill(80001432).getEffect(1);
//                    effect.applyTo(chr);
                    break;
                case 8: //정화
                    effect = SkillFactory.getSkill(80002888).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
                case 9: //광선의 룬
                    effect = SkillFactory.getSkill(80002889).getEffect(1);
                    effect.applyTo(chr, false);
                    break;
            }
            chr.getMap().setRuneCurse(0);
            chr.getMap().broadcastMessage(CField.runeCurse("엘리트 보스의 저주가 해제되었습니다!!", true));
            chr.getMap().removeMapObject(rune);
            chr.getMap().setRune(null);
            effect = SkillFactory.getSkill(80002282).getEffect(1);
            effect.applyTo(chr, false);

            if (chr.getQuestStatus(16408) == 1) {
                if (chr.getKeyValue(16408, "runeAct") < 5) {
                    chr.setKeyValue(16408, "runeAct", String.valueOf(Math.max(0, chr.getKeyValue(16408, "runeAct") + 1)));
                } else {
                    MapleQuest.getInstance(16408).forceComplete(chr, 0);
                }
            } else if (chr.getQuestStatus(100423) == 1) {
                chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(100423, "RunAct=1;"));
                chr.getClient().setKeyValue("state", "2");
            }
        } else {
            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.RUNE)).setCustomData(String.valueOf(System.currentTimeMillis()));
        }
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static void WeddingPresent(final LittleEndianAccessor slea, final MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 7) {
            // receive present.
            byte invtype = slea.readByte();
            byte slot = slea.readByte();
            MarriageDataEntry entry = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
            if (entry != null) {
                List<Item> items = c.getPlayer().getGender() == 0 ? entry.getGroomPresentList() : entry.getBridePresentList();
                if (null != items) {
                    try {
                        Item item = items.get(slot);
                        if (item != null && MapleInventoryType.getByType(invtype) == GameConstants.getInventoryType(item.getItemId())) {
                            if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), "")) {
                                items.remove(slot);
                                MapleInventoryManipulator.addbyItem(c, item);
                                c.getSession().writeAndFlush(CWvsContext.showWeddingWishRecvToLocalResult(items));
                            } else {
                                c.getSession().writeAndFlush(CWvsContext.showWeddingWishRecvDisableHang());
                                c.getPlayer().dropMessage(1, "인벤토리 공간이 부족합니다.");
                                return;
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException d) {
                    }
                }
            }
        } else if (mode == 6) {
            // give present
            short slot = slea.readShort();
            int itemid = slea.readInt();
            short quantity = slea.readShort();
            MapleInventoryType type = MapleInventoryType.getByType((byte) (itemid / 1000000));
            Item item = c.getPlayer().getInventory(type).getItem(slot);
            MarriageEventAgent agent = MarriageManager.getInstance().getEventAgent(c.getChannel());
            if (agent != null) {
                MarriageDataEntry dataEntry = agent.getDataEntry();
                if (dataEntry != null) {
                    if (item != null && item.getItemId() == itemid && item.getQuantity() >= quantity) {
                        Item item2 = item.copy();
                        if (GameConstants.isRechargable(itemid)) {
                            quantity = item.getQuantity();
                        }
                        item2.setQuantity(quantity);
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        if (c.getPlayer().getWeddingGive() == 0) {
                            dataEntry.getGroomPresentList().add(item2);
                            c.getSession().writeAndFlush(CWvsContext.showWeddingWishGiveToServerResult(dataEntry.getGroomWishList(), type, dataEntry.getGroomPresentList()));
                            // DBLogger.getInstance().logTrade(LogType.Trade.WeddingPresent, c.getPlayer().getId(), c.getPlayer().getName(), dataEntry.getGroomName(), MapleItemInformationProvider.getInstance().getName(itemid), "결혼 선물 (" + c.getPlayer().getName() + "->" + dataEntry.getGroomName() + ")");
                        } else {
                            dataEntry.getBridePresentList().add(item2);
                            c.getSession().writeAndFlush(CWvsContext.showWeddingWishGiveToServerResult(dataEntry.getBrideWishList(), type, dataEntry.getBridePresentList()));
                            //DBLogger.getInstance().logTrade(LogType.Trade.WeddingPresent, c.getPlayer().getId(), c.getPlayer().getName(), dataEntry.getBrideName(), MapleItemInformationProvider.getInstance().getName(itemid), "결혼 선물 (" + c.getPlayer().getName() + "->" + dataEntry.getBrideName() + ")");
                        }
                    }
                }
            }
            // [R] 74 00 06 02 00 80 E2 0F 00 01 00
            // [R] 74 00 06 1F 00 81 EF 14 00 01 00
            // [R] 74 00 06 06 00 85 84 1E 00 07 00
        }
    }

    public static void followCancel(LittleEndianAccessor slea, MapleClient c) {
        if (slea.readByte() == 0) {
            c.getPlayer().checkFollow();
        }
    }

    public static void auraPartyBuff(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4); // 1
        int duration = slea.readInt(); // 1

        int skillId = slea.readInt();
        int skillLv = slea.readInt();
        int ownerId = slea.readInt();
        int ownerPosX = slea.readInt();
        int ownerPosY = slea.readInt();

        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(skillLv);
        MapleCharacter owner = c.getPlayer().getMap().getCharacterById(ownerId);
        MapleCharacter chr = c.getPlayer();
        if (owner != null && effect != null && chr != null) {
            if (!chr.getBuffedValue(skillId)) {
                if (effect.calculateBoundingBox(new Point(ownerPosX, ownerPosY), owner.isFacingLeft()).contains(chr.getTruePosition())) {
                    effect.applyTo(chr, false, duration * 1000);
                } else {
                    chr.cancelEffect(effect, false, -1);
                }
            }
        }
    }
}
