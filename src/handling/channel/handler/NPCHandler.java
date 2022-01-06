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
import client.MapleQuestStatus;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import server.DimentionMirrorEntry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStorage;
import server.life.MapleNPC;
import server.maps.MapScriptMethods;
import server.quest.MapleQuest;
import server.shops.MapleShop;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.SLFCGPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class NPCHandler {

    public static final void NPCAnimation(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getMapId() == 910143000) {
            return;
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
        final int length = (int) slea.available();
        if (length == 10) { // NPC Talk
            mplew.writeInt(slea.readInt());
            mplew.write(slea.readByte());
            mplew.write(slea.readByte()); // 이게 -1이라면 무브먼트를 보내야 함.. 아마 리시브로 날아오지 않을까?
            mplew.writeInt(slea.readInt());
        } else if (length > 10) { // NPC Move
            mplew.write(slea.read(length - 9));
        } else {
            return;
        }
        c.getSession().writeAndFlush(mplew.getPacket());
    }

    public static final void NPCShop(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte bmode = slea.readByte();
        if (chr == null) {
            return;
        }

        switch (bmode) {
            case 0: {
                MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                short slot = slea.readShort();
                slot++;
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                // int unitprice = slea.readInt();
                shop.buy(c, itemId, quantity, slot);
                break;
            }
            case 1: {
                MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                short slot = slea.readShort();
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                shop.sell(c, GameConstants.getInventoryType(itemId), slot, quantity);
                break;
            }
            case 2: {
                MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                short slot = slea.readShort();
                shop.recharge(c, slot);
                break;
            }
            default:
                chr.setConversation(0);
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void NPCTalk(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleNPC npc = chr.getMap().getNPCByOid(slea.readInt());
        if (npc == null) {
            return;
        }
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            NPCScriptManager.getInstance().start(c, npc.getId(), null);
            //NPCScriptManager.getInstance().start(c, npc.getId(), MapleLifeFactory.getNpcScripts().get(npc.getId()));
        }
    }

    public static final void QuestAction(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte action = slea.readByte();
        int quest = slea.readInt();
        if (chr == null) {
            return;
        }
        final MapleQuest q = MapleQuest.getInstance(quest);
        switch (action) {
            case 0: { // Restore lost item
                //chr.updateTick(slea.readInt());
                slea.readInt();
                final int itemid = slea.readInt();
                q.RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // Start Quest
                final int npc = slea.readInt();
                if (!(quest >= 1115 && quest <= 1124)) {
                    //저장 안하고 패킷만 보내기  대체왜? 아직 이유 모르겠음.
                     c.getSession().writeAndFlush(InfoPacket.updateQuestInfo(1, 2, quest));
//                    MapleQuest.getInstance(quest).forceComplete(chr, npc);
                } else if (!q.hasStartScript()) {
                    q.start(chr, npc);
                }
                break;
            }
            case 2: { // Complete Quest
                final int npc = slea.readInt();
                //chr.updateTick(slea.readInt());
                slea.readInt();
                if (q.hasEndScript() && !(quest >= 1115 && quest <= 1124)) {
                    return;
                }
                if (slea.available() >= 4) {
                    q.complete(chr, npc, slea.readInt());
                } else {
                    q.complete(chr, npc);
                }
                // c.getSession().writeAndFlush(CField.completeQuest(c.getPlayer(), quest));
                //c.getSession().writeAndFlush(CField.updateQuestInfo(c.getPlayer(), quest, npc, (byte)14));
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // Forefit Quest
                if (GameConstants.canForfeit(q.getId())) {
                    q.forfeit(chr);
                } else {
                    chr.dropMessage(1, "You may not forfeit this quest.");
                }
                break;
            }
            case 4: { // Scripted Start Quest
                final int npc = slea.readInt();
                final int a = slea.readInt();
                if (quest == 100879) {
                    chr.changeSkillLevel(80003082, (byte) 1, (byte) 1);
                    MapleQuest.getInstance(quest).forceComplete(chr, npc);
                } else if (quest == 16014) {//유니온
                    NPCScriptManager.getInstance().start(c, npc);
                } else if (quest >= 37151 && quest <= 37180) { //엘로딘
                    NPCScriptManager.getInstance().startQuest(c, npc, quest);
                } else if (quest == 100114 || quest == 100188) { // 어드벤처
                    NPCScriptManager.getInstance().startQuest(c, npc, quest);
                } else if (quest == 100825 || quest == 100880) {
                    NPCScriptManager.getInstance().startQuest(c, npc, quest);
                } else if (quest == 39819) {
                    NPCScriptManager.getInstance().start(c, npc);
                } else {
                    c.getSession().writeAndFlush(InfoPacket.updateQuestInfo(1, 2, quest));
//                    MapleQuest.getInstance(quest).forceComplete(chr, npc);
                }
                break;
            }
            case 5: { // Scripted End Quest
                slea.readInt();
                final int npc = slea.readInt();
                NPCScriptManager.getInstance().endQuest(c, npc, quest, false);
                c.getSession().writeAndFlush(EffectPacket.showNormalEffect(chr, 15, true)); // Quest completion
                chr.getMap().broadcastMessage(chr, EffectPacket.showNormalEffect(chr, 15, false), false);
                break;
            }
        }
    }

    public static final void Storage(LittleEndianAccessor slea, final MapleClient c, MapleCharacter chr) {
        byte mode = slea.readByte();
        if (chr == null) {
            return;
        }
        MapleStorage storage = chr.getStorage();

        switch (mode) {
            case 3: {
                if (c.CheckSecondPassword(slea.readMapleAsciiString())) {
                    c.getPlayer().getStorage().sendStorage(c, chr.getStorageNPC());
                } else {
                    c.getSession().writeAndFlush(NPCPacket.getStorage((byte) 1));
                }
                break;
            }
            case 4: {
                byte type = slea.readByte();
                byte slot = storage.getSlot(MapleInventoryType.getByType(type), slea.readByte());
                Item item = storage.takeOut(slot);
//                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

                if (item != null) {
                    if (c.getPlayer().getInventory(MapleInventoryType.getByType(type)).getNextFreeSlot() <= -1) {
//                    if (!MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        storage.store(item);
                        chr.dropMessage(1, "인벤토리의 공간이 부족합니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        /*                        if (ii.isDropRestricted(item.getItemId())) {
                         if (ItemFlag.KARMA_EQ.check(item.getFlag())) {
                         item.setFlag(item.getFlag() - ItemFlag.KARMA_EQ.getValue());
                         } else if (ItemFlag.KARMA_USE.check(item.getFlag())) {
                         item.setFlag(item.getFlag() - ItemFlag.KARMA_USE.getValue());
                         } else if (ItemFlag.SHARE_ACC.check(item.getFlag())) {
                         item.setFlag(item.getFlag() - ItemFlag.SHARE_ACC.getValue());
                         } else if (ItemFlag.SHARE_ACC_USE.check(item.getFlag())) {
                         item.setFlag(item.getFlag() - ItemFlag.SHARE_ACC_USE.getValue());
                         } else {
                         c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                         return;
                         }
                         }*/
                    } else {
                        MapleInventoryManipulator.addbyItem(c, item, false);
                        storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
                    }
                } else {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                }
                break;
            }
            case 5: {
                final short slot = slea.readShort();
                int itemId = slea.readInt();
                final MapleInventoryType type = GameConstants.getInventoryType(itemId);
                short quantity = slea.readShort();
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (quantity < 1) {
                    return;
                }
                if (storage.isFull()) {
                    c.getSession().writeAndFlush(NPCPacket.getStorageFull());
                    return;
                }
                if (chr.getInventory(type).getItem((short) slot) == null) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }

                if (chr.getMeso() < 100L) {
                    chr.dropMessage(1, "아이템을 맡기려면 100메소가 필요합니다.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                } else {
                    Item item = chr.getInventory(type).getItem((short) slot).copy();

                    if (GameConstants.isPet(item.getItemId())) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                    if ((ii.isPickupRestricted(item.getItemId())) && (storage.findById(item.getItemId()) != null)) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                    if ((item.getItemId() == itemId) && ((item.getQuantity() >= quantity) || (GameConstants.isThrowingStar(itemId)) || (GameConstants.isBullet(itemId)))) {
                        if ((GameConstants.isThrowingStar(itemId)) || (GameConstants.isBullet(itemId))) {
                            quantity = item.getQuantity();
                        }
                        MapleInventoryManipulator.removeFromSlot(c, type, (short) slot, quantity, false);
                        chr.gainMeso(-100L, false, false);
                        item.setQuantity(quantity);
                        storage.store(item);
                        storage.sendStored(c, GameConstants.getInventoryType(itemId));
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                }
                break;
            }
            case 6:
                storage.arrange();
                storage.update(c);
                break;
            case 7: {

                long meso = slea.readLong();

                long storageMesos = storage.getMeso();
                long playerMesos = chr.getMeso();
                if (meso < 0) { // 창고에 메소 넣음
                    if (-meso <= playerMesos) {
                        storage.setMeso(storageMesos - meso);
                        chr.gainMeso(meso, false, false);
                    }
                } else if (meso > 0) { // 창고에서 메소 뺌
                    if (meso <= storageMesos) { // 불가능
                        storage.setMeso(storageMesos - meso);
                        chr.gainMeso(meso, false, false);
                    }
                }
//            	c.getPlayer().dropMessage(1, "메소옮기기는 사용하실 수 없습니다.");
                storage.sendMeso(c);
//            	c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                break;
            }
            case 8:
                storage.close();
                chr.setConversation(0);
                break;
            default:
                System.out.println("Unhandled Storage mode : " + mode);
                break;
        }
    }

    public static final void NPCMoreTalk(final LittleEndianAccessor slea, final MapleClient c) {
        final byte lastMsg = slea.readByte(); // 00 (last msg type I think)
        if (lastMsg == 0) {
            slea.readInt();
            slea.readInt();
            slea.readMapleAsciiString();
        }
        if (c.getPlayer() == null) {
            return;
        }
        if (c.getPlayer().isWatchingWeb() && lastMsg == 0x16) {
            c.getPlayer().setWatchingWeb(false);
            c.getSession().writeAndFlush(SLFCGPacket.ChangeVolume(100, 1000));
        }
        if (lastMsg == 10 && slea.available() >= 4) {
            slea.skip(2);
        } else if (lastMsg == 0x2C) {
            byte dispose = slea.readByte();
            if (dispose == 0) {
                return;
            }
        }

        if (lastMsg == 0x25) {
            NPCScriptManager.getInstance().action(c, (byte) 1, lastMsg, -1);
            return;
        }

        final byte action = slea.readByte(); // 00 = end chat, 01 == follow
        //todo legend
//        if (((lastMsg == 0x12 && c.getPlayer().getDirection() >= 0) || (lastMsg == 0x14 && c.getPlayer().getDirection() == -1)) && action == 1) {
//            MapScriptMethods.startDirectionInfo(c.getPlayer(), lastMsg == 0x14);
//            return;
//        }
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);

        if (cm == null) {
            return;
        }

        if (c.getPlayer().getConversation() == 0) {// || cm.getLastMsg() != lastMsg) {
            cm.dispose();
            return;
        }

        if (lastMsg == 0x06) {
            if (action == 0 && !(slea.available() >= 1)) {
                c.removeClickedNPC();
                NPCScriptManager.getInstance().dispose(c);
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
        }
        cm.setLastMsg((byte) -1);
        if (lastMsg == 4) { //257 +
            if (action != 0) {
                cm.setGetText(slea.readMapleAsciiString());
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, -1);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, -1);
                } else {
                    NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                }
            } else {
                cm.dispose();
            }
        } else {
            int selection = -1;
            int selection2 = -1;
            if (slea.available() >= 4) {
                selection = slea.readInt();
            } else if (slea.available() > 0) {
                if (GameConstants.isZero(c.getPlayer().getJob()) && lastMsg == 32) {
                    selection = slea.readByte();
                    selection2 = slea.readByte();
                } else {
                    if (slea.available() > 1)
                        slea.readByte(); //++343
                    selection = slea.readByte();
                }
            }
            if (lastMsg == 0x2C) {
                slea.skip(2);
                int nMixBaseHairColor = slea.readInt();
                int nMixAddHairColor = slea.readInt();
                int nMixHairBaseProb = slea.readInt();
                if (GameConstants.isZero(c.getPlayer().getJob())) {
                    if (c.getPlayer().getGender() == 1) {
                        c.getPlayer().setSecondBaseColor(nMixBaseHairColor);
                        c.getPlayer().setSecondAddColor(nMixAddHairColor);
                        c.getPlayer().setSecondBaseProb(nMixHairBaseProb);
                        c.getPlayer().updateZeroStats();
                    } else {
                        c.getPlayer().setBaseColor(nMixBaseHairColor);
                        c.getPlayer().setAddColor(nMixAddHairColor);
                        c.getPlayer().setBaseProb(nMixHairBaseProb);
                    }
                } else if (GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                    if (c.getPlayer().getDressup()) {
                        c.getPlayer().setSecondBaseColor(nMixBaseHairColor);
                        c.getPlayer().setSecondAddColor(nMixAddHairColor);
                        c.getPlayer().setSecondBaseProb(nMixHairBaseProb);
                        c.getPlayer().updateAngelicStats();
                    } else {
                        c.getPlayer().setBaseColor(nMixBaseHairColor);
                        c.getPlayer().setAddColor(nMixAddHairColor);
                        c.getPlayer().setBaseProb(nMixHairBaseProb);
                    }
                } else {
                    c.getPlayer().setBaseColor(nMixBaseHairColor);
                    c.getPlayer().setAddColor(nMixAddHairColor);
                    c.getPlayer().setBaseProb(nMixHairBaseProb);
                }
                c.getPlayer().equipChanged();
                c.removeClickedNPC();
                NPCScriptManager.getInstance().dispose(c);
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (lastMsg == 4 && selection == -1 && selection2 == -1) {
                cm.dispose();
                return;//h4x
            }
            if (selection >= -1 && action != -1) { // action : 0 ESC
                if (cm.getType() == 0) {
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg, selection);
                } else if (cm.getType() == 1) {
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg, selection);
                } else {
                    if (GameConstants.isZero(c.getPlayer().getJob()) && lastMsg == 32) { //?쒕줈 ?깊삎 ?≪뀡
                        NPCScriptManager.getInstance().zeroaction(c, action, lastMsg, selection, selection2);
                    } else {
                        NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
                    }
                }
            } else {
                //ESC, ??붽렇留뚰븯湲?                
                cm.dispose();
            }
        }
    }

    public static final void repairAll(final MapleClient c) {
        if (c.getPlayer().getMapId() != 240000000) {
            return;
        }
        Equip eq;
        double rPercentage;
        int price = 0;
        Map<String, Integer> eqStats;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<Equip, Integer> eqs = new HashMap<Equip, Integer>();
        final MapleInventoryType[] types = {MapleInventoryType.EQUIP, MapleInventoryType.EQUIPPED};
        for (MapleInventoryType type : types) {
            for (Item item : c.getPlayer().getInventory(type).newList()) {
                if (item instanceof Equip) { //redundant
                    eq = (Equip) item;
                    if (eq.getDurability() >= 0) {
                        eqStats = ii.getEquipStats(eq.getItemId());
                        if (eqStats.containsKey("durability") && eqStats.get("durability") > 0 && eq.getDurability() < eqStats.get("durability")) {
                            rPercentage = (100.0 - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
                            eqs.put(eq, eqStats.get("durability"));
                            price += (int) Math.ceil(rPercentage * ii.getPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0));
                        }
                    }
                }
            }
        }
        if (eqs.size() <= 0 || c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, true);
        Equip ez;
        for (Entry<Equip, Integer> eqqz : eqs.entrySet()) {
            ez = eqqz.getKey();
            ez.setDurability(eqqz.getValue());
            c.getPlayer().forceReAddItem(ez.copy(), ez.getPosition() < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP);
        }
    }

    public static final void repair(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getMapId() != 240000000 || slea.available() < 4) { //leafre for now
            return;
        }
        final int position = slea.readInt(); //who knows why this is a int
        final MapleInventoryType type = position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
        final Item item = c.getPlayer().getInventory(type).getItem((short) position);
        if (item == null) {
            return;
        }
        final Equip eq = (Equip) item;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<String, Integer> eqStats = ii.getEquipStats(item.getItemId());
        if (eq.getDurability() < 0 || !eqStats.containsKey("durability") || eqStats.get("durability") <= 0 || eq.getDurability() >= eqStats.get("durability")) {
            return;
        }
        final double rPercentage = (100.0 - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
        //drpq level 105 weapons - ~420k per %; 2k per durability point
        //explorer level 30 weapons - ~10 mesos per %
        final int price = (int) Math.ceil(rPercentage * ii.getPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0)); // / 100 for level 30?
        //TODO: need more data on calculating off client
        if (c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, false);
        eq.setDurability(eqStats.get("durability"));
        c.getPlayer().forceReAddItem(eq.copy(), type);
    }

    public static final void UpdateQuest(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleQuest quest = MapleQuest.getInstance(slea.readInt());
        if (quest != null) {
            c.getPlayer().updateQuest(c.getPlayer().getQuest(quest), true);
        }
    }

    public static final void UseItemQuest(final LittleEndianAccessor slea, final MapleClient c) {
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        final int qid = slea.readInt();
        final MapleQuest quest = MapleQuest.getInstance(qid);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, List<Integer>> questItemInfo = null;
        boolean found = false;
        for (Item i : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
            if (i.getItemId() / 10000 == 422) {
                questItemInfo = ii.questItemInfo(i.getItemId());
                if (questItemInfo != null && questItemInfo.getLeft() == qid && questItemInfo.getRight() != null && questItemInfo.getRight().contains(itemId)) {
                    found = true;
                    break; //i believe it's any order
                }
            }
        }
        if (quest != null && found && item != null && item.getQuantity() > 0 && item.getItemId() == itemId) {
            final int newData = slea.readInt();
            final MapleQuestStatus stats = c.getPlayer().getQuestNoAdd(quest);
            if (stats != null && stats.getStatus() == 1) {
                stats.setCustomData(String.valueOf(newData));
                c.getPlayer().updateQuest(stats, true);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
            }
        }
    }

    public static void quickMove(LittleEndianAccessor slea, MapleClient c) {
        c.removeClickedNPC();
        NPCScriptManager.getInstance().dispose(c);
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        NPCScriptManager.getInstance().start(c, slea.readInt());
    }

    public static void dimentionMirror(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int id = slea.readInt();
        for (DimentionMirrorEntry dm : ServerConstants.mirrors) {
            if (dm.getId() == id) {
                NPCScriptManager.getInstance().start(c, dm.getScript());
            }
        }
    }

}
