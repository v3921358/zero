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
import client.inventory.*;
import client.inventory.Equip.ScrollResult;
import client.inventory.MaplePet.PetFlag;
import constants.GameConstants;
import constants.SpecialItemConstants;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.login.LoginServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import log.DBLogger;
import log.LogType;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import server.*;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import server.enchant.EquipmentEnchant;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.maps.*;
import server.quest.MapleQuest;
import server.shops.HiredMerchant;
import server.shops.IMaplePlayerShop;
import server.shops.MapleShopFactory;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.*;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

public class InventoryHandler {

    public static final void ItemMove(final LittleEndianAccessor slea, final MapleClient c) {
        try {
            c.getPlayer().setScrolledPosition((short) 0);
            slea.readInt();
            final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte()); //04
            final short src = slea.readShort();                                            //01 00
            final short dst = slea.readShort();                                            //00 00
            final short quantity = slea.readShort();
            c.getPlayer().dropMessageGM(5, src + " / " + dst);
            if (src < 0 && dst > 0) {
                MapleInventoryManipulator.unequip(c, src, dst, type);
            } else if (dst < 0) {
                if ((dst <= (short) -100 && dst > (short) -200) && !MapleItemInformationProvider.getInstance().isCash(((Equip) c.getPlayer().getInventory(MapleInventoryType.DECORATION).getItem(src)).getItemId())) {

                    String data;
                    data = "캐릭터명 : " + c.getPlayer().getName() + ", itemID : " + ((Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src)).getItemId() + ") 이 장착됨.\r\n";
                    data += "포지션 : " + dst + "\r\n\r\n";
                    NPCConversationManager.writeLog("Log/CashFucker2.log", data, true);
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                MapleInventoryManipulator.equip(c, src, dst, type);
            } else if (dst == 0) {
                MapleInventoryManipulator.drop(c, type, src, quantity);
            } else {
                MapleInventoryManipulator.move(c, type, src, dst);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void SwitchBag(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().setScrolledPosition((short) 0);
        slea.readInt();
        final short src = (short) slea.readInt();                                       //01 00
        final short dst = (short) slea.readInt();                                            //00 00
        if (src < 100 || dst < 100) {
            return;
        }
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, src, dst);
    }

    public static final void MoveBag(final LittleEndianAccessor slea, final MapleClient c) {
        c.getPlayer().setScrolledPosition((short) 0);
        // 99 6C C7 00 
        // 00 00 00 00 
        // 04 
        // 75 27 00 00 
        // 05 00

        //63 64 C9 00 
        //00 00 00 00 
        //04 
        //78 27 00 00 
        //03 00
        slea.readInt();
        final boolean srcFirst = slea.readInt() > 0;                                //01 00
        if (slea.readByte() != 4) { //must be etc) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        short dst = (short) slea.readInt();
        short src = slea.readShort();                                            //00 00
        MapleInventoryManipulator.move(c, MapleInventoryType.ETC, srcFirst ? dst : src, srcFirst ? src : dst);
    }

    public static final void ItemSort(final LittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        final MapleInventoryType pInvType = MapleInventoryType.getByType(slea.readByte());
        if (pInvType == MapleInventoryType.UNDEFINED) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final MapleInventory pInv = c.getPlayer().getInventory(pInvType); //Mode should correspond with MapleInventoryType
        final List<Item> itemMap = new LinkedList<Item>();
        for (Item item : pInv.list()) {
            itemMap.add(item.copy()); // clone all  items T___T.
        }

        List<Pair<Short, Short>> updateSlots = new ArrayList<>();

        for (int i = 1; i <= pInv.getSlotLimit(); i++) {
            Item item = pInv.getItem((short) i);
            if (item == null) {
                Item nextItem = pInv.getItem(pInv.getNextItemSlot((short) i));
                if (nextItem != null) {
                    short oldPos = nextItem.getPosition();
                    pInv.removeItem(nextItem.getPosition());
                    short nextPos = pInv.addItem(nextItem);
                    updateSlots.add(new Pair<>(oldPos, nextPos));
                }
            }
        }

        c.getSession().writeAndFlush(InventoryPacket.moveInventoryItem(pInvType, updateSlots));
        c.getSession().writeAndFlush(CWvsContext.finishedSort(pInvType.getType()));
    }

    public static final void ItemGather(final LittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        final byte mode = slea.readByte();
        final MapleInventoryType invType = MapleInventoryType.getByType(mode);
        MapleInventory Inv = c.getPlayer().getInventory(invType);

        final List<Item> itemMap = new LinkedList<Item>();
        for (Item item : Inv.list()) {
            itemMap.add(item.copy()); // clone all items T___T.
        }
        for (Item itemStats : itemMap) {

            // c.getPlayer().dropMessage(6,"아이템 정리 디버깅 코드 : "+ itemStats.getItemId()+ " 갯수 " +itemStats.getQuantity() );
            MapleInventoryManipulator.removeFromSlot(c, invType, itemStats.getPosition(), itemStats.getQuantity(), true,
                    false);
        }

        final List<Item> sortedItems = sortItems(itemMap);
        for (Item item : sortedItems) {
            MapleInventoryManipulator.addFromDrop(c, item, false);
        }
        c.getSession().writeAndFlush(CWvsContext.finishedGather(mode));
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        itemMap.clear();
        sortedItems.clear();
    }

    private static final List<Item> sortItems(final List<Item> passedMap) {
        final List<Integer> itemIds = new ArrayList<Integer>(); // empty list.
        for (Item item : passedMap) {
            itemIds.add(item.getItemId()); // adds all item ids to the empty list to be sorted.
        }
        Collections.sort(itemIds); // sorts item ids

        final List<Item> sortedList = new LinkedList<Item>(); // ordered list pl0x <3.

        for (Integer val : itemIds) {
            for (Item item : passedMap) {
                if (val == item.getItemId()) { // Goes through every index and finds the first value that matches
                    sortedList.add(item);
                    passedMap.remove(item);
                    break;
                }
            }
        }
        return sortedList;
    }

    public static final boolean UseRewardItem(final short slot, final int itemId, final byte a1, final byte a2, final MapleClient c, final MapleCharacter chr) {
        if (a1 == 1) { //1이면  short 가 이펙트 id로 오는거같음
            if (a2 == 0) { // 1이면 랜덤 빙빙 시작 중간에 헤제가능이라서 경우 넣음
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);
                Item toUse = null;
                for (Item item : c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).list()) {
                    if (item.getItemId() == itemId) {
                        toUse = item;
                        break;
                    }
                }
                if (toUse == null) {
                    chr.dropMessage(6, "오류가 발생하였습니다.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return false;
                }
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.getByType((byte) (toUse.getItemId() / 1000000)), toUse.getPosition(), (short) 1, false);
                if (itemId == 2028273) {
                    int reward = 1113097 + Randomizer.rand(1, 31);
                    Equip item = (Equip) ii.getEquipById(reward);

                    MapleInventoryManipulator.addbyItem(c, item);

                    c.getSession().writeAndFlush(EffectPacket.showEffect(c.getPlayer(), reward, 0, 53, 1, 0, (byte) 0, true, null, "", item));
                    c.getSession().writeAndFlush(EffectPacket.showRewardItemEffect(c.getPlayer(), itemId, true, ""));

                    if (item.getBaseLevel() >= 4 && (reward == 1113098 || reward == 1113099 || (reward >= 1113113 && reward <= 1113116) || reward == 1113122)) {
                        World.Broadcast.broadcastMessage(CWvsContext.serverMessage(11, c.getChannel(), c.getPlayer().getName(), c.getPlayer().getName() + "님이 숨겨진 반지 상자에서 [" + ii.getName(reward) + "] 아이템을 획득했습니다.", true, item));
                    }

                }
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return true;
        } else {
            final Item toUse = c.getPlayer().getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId) {
                if (chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.USE).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.SETUP).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.ETC).getNextFreeSlot() > -1 && chr.getInventory(MapleInventoryType.DECORATION).getNextFreeSlot() > -1) {
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    final Pair<Integer, List<StructRewardItem>> rewards = ii.getRewardItem(itemId);
                    if (rewards != null && rewards.getLeft() > 0) {
                        while (true) {
                            for (StructRewardItem reward : rewards.getRight()) {
                                if (reward.prob > 0 && Randomizer.nextInt(rewards.getLeft()) < reward.prob) { // Total prob
                                    if (GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(reward.itemid) == MapleInventoryType.DECORATION) {
                                        final Item item = ii.getEquipById(reward.itemid);
                                        if (reward.period > 0) {
                                            item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                                        }
                                        item.setGMLog("Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                        MapleInventoryManipulator.addbyItem(c, item);
                                    } else {
                                        MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                                    }
                                    MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemId), itemId, 1, false, false);

                                    c.getSession().writeAndFlush(EffectPacket.showRewardItemEffect(chr, reward.itemid, true, reward.effect));
                                    chr.getMap().broadcastMessage(chr, EffectPacket.showRewardItemEffect(chr, reward.itemid, false, reward.effect), false);
                                    return true;
                                }
                            }
                        }
                    } else if (itemId == 2028273) {
                        if (a1 == 0 && a2 == 0) {
                            List<Integer> rewardlist = new ArrayList<>();
                            for (int i = 0; i < 32; i++) {
                                rewardlist.add(1113098 + i);
                            }

                            Collections.shuffle(rewardlist);

                            c.getSession().writeAndFlush(EffectPacket.ShowRandomBoxEffect(20, 2028273, rewardlist));
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        }
                    } else if (itemId == 2028272) {
                        int reward = RandomRewards.getTheSeedReward();
                        if (reward == 0) {
                            c.getSession().writeAndFlush(NPCPacket.getNPCTalk(9000155, (byte) 0, "아쉽지만, 꽝이 나왔습니다. 다음 기회에 다시 이용해주세요!", "00 00", (byte) 0));
                        } else if (reward == 1) {
                            chr.gainMeso(10000000, true);
                            c.getSession().writeAndFlush(NPCPacket.getNPCTalk(9000155, (byte) 0, "1천만메소를 획득하셨습니다!", "00 00", (byte) 0));
                        } else {
                            int max_quantity = 1;
                            switch (reward) {
                                case 4310034:
                                    max_quantity = 10;
                                    break;
                                case 4310014:
                                    max_quantity = 10;
                                    break;
                                case 4310016:
                                    max_quantity = 10;
                                    break;
                                case 4001547:
                                case 4001548:
                                case 4001549:
                                case 4001550:
                                case 4001551:
                                case 4001208:
                                case 4001209:
                                case 4001210:
                                case 4001211:
                                    max_quantity = 1;
                                    break;
                            }
                            c.getSession().writeAndFlush(NPCPacket.getNPCTalk(9000155, (byte) 0, "축하드립니다!!\r\n돌림판에서 [#b#i" + reward + "##z" + reward + "#](이)가 나왔습니다.", "00 00", (byte) 0));
                            c.getPlayer().gainItem(reward, max_quantity);
                            c.getSession().writeAndFlush(EffectPacket.showCharmEffect(c.getPlayer(), reward, 1, true, ""));
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        }
                    } else if (itemId == 2028208 || itemId == 2028209) {
                        NPCScriptManager.getInstance().startItem(c, 9000162, "consume_" + itemId);
                    } else {
                        chr.dropMessage(6, "아이템 보상 정보를 찾을 수 없습니다.");
                    }
                } else {
                    chr.dropMessage(6, "Insufficient inventory slot.");
                }
            }
            return false;
        }
    }

    public static final void UseItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getBuffedEffect(MapleBuffStat.DebuffIncHp) != null || chr.getMap() == null || chr.hasDisease(MapleBuffStat.StopPortion)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        try {
            final long time = System.currentTimeMillis();
//        if (chr.getNextConsume() > time) {
//            chr.dropMessage(5, "You may not use this item yet.");
//            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
//            return;
//        }
            slea.skip(4);
            final short slot = slea.readShort();
            final int itemId = slea.readInt();
            final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
            if (toUse.getItemId() != itemId) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
//        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
//            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
//            return; 
//        }
            if (itemId == 2023287) {
                c.getPlayer().MakeCDitem(2023287, 3, false);
            }

            if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
                if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr, true)) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                    if (chr.getMap().getConsumeItemCoolTime() > 0) {
                        c.getSession().writeAndFlush(CField.potionCooldown());
                        chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                    }
                }

            } else {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void UseReturnScroll(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getMapId() == 749040100 || chr.inPVP()) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        slea.readInt();
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) {
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyReturnScroll(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            } else {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } else {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        }
    }

    public static void UseMagnify(final LittleEndianAccessor slea, final MapleClient c) {
        try {
            slea.skip(4);
            boolean useGlass = false;
            boolean isEquipped = false;
            short useSlot = slea.readShort(); // 돋보기가 있을시 돋보기의 인벤토리상의 위치값.
            short equSlot = slea.readShort(); // 장비아이템의 인벤토리상 혹은 장착된 위치값.
            Equip equip;
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (equSlot < 0) { // 인벤토리에 아이템이 있는건지 장착된 아이템인지 구별.
                equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(equSlot);
                isEquipped = true;
            } else {
                equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(equSlot);
            }
            Item glass = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(useSlot); // 돋보기 아이템. (없을시 null)
            if (useSlot != 20000) {
                if (glass == null || equip == null) {
                    c.getPlayer().dropMessage(1, "GLASS NULL!");
                    c.getSession().writeAndFlush(CWvsContext.InventoryPacket.getInventoryFull());
                    return;
                } else {
                    useGlass = true;
                }
            } else {
                final long price = GameConstants.getMagnifyPrice(equip);
                c.getPlayer().gainMeso(-price, false);
            }

            /*잠재감정 시작*/
            if (equip.getState() == 1) { // 일반 잠재능력 랜덤 확률로 등급 감정.
                int rank = Randomizer.nextInt(100);
                if (equip.getLines() == 0) {
                    equip.setLines((byte) 2);
                }
                if (rank < 5) { // 에픽아이템이 될 확률 (5%)
                    equip.setState((byte) 18);
                } else if (rank < 3) {// 유니크아이템이 될 확률 (3%)
                    equip.setState((byte) 19);
                } else {
                    equip.setState((byte) 17);
                }
            } else {
                equip.setState((byte) (equip.getState() + 16));
            }
            int level = equip.getState() - 16;
            equip.setPotential1(potential(equip.getItemId(), level));
            equip.setPotential2(potential(equip.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1)));
            equip.setPotential3(equip.getLines() == 3 ? potential(equip.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1)) : 0);

            /*잠재감정 종료*/
            if (useGlass) {
                MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
                useInventory.removeItem(useSlot, (short) 1, false);
            }
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, equip));
            c.getPlayer().getTrait(MapleTrait.MapleTraitType.insight).addExp(10, c.getPlayer()); //전문기술 능력치
            c.getPlayer().getMap().broadcastMessage(CField.showMagnifyingEffect(c.getPlayer().getId(), equSlot)); // 아이템업데이트
            if (isEquipped) { // 아이템이 플레이어한테 장착되어있을시.
                c.getPlayer().forceReAddItem_NoUpdate(equip, MapleInventoryType.EQUIPPED);
            } else {
                c.getPlayer().forceReAddItem_NoUpdate(equip, MapleInventoryType.EQUIP);
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int potential(int itemid, int level) {
        return potential(itemid, level, false);
    }

    public static int potential(int itemid, int level, boolean editional) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int itemtype = itemid / 1000;
        return ii.getPotentialOptionID(Math.max(1, level), editional, itemtype);
    }

    public static void UseStamp(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4);
        short slot = slea.readShort();
        short dst = slea.readShort();
        boolean sucstamp = false;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip toStamp;
        if (dst < 0) {
            toStamp = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            toStamp = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        Item stamp = useInventory.getItem(slot);
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            Equip toStamp2;
            toStamp2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            if (Randomizer.isSuccess(ii.getSuccess(toStamp2.getItemId(), c.getPlayer(), toStamp2))) {
                toStamp2.setLines((byte) 3);
                int level = toStamp2.getState() - 16;
                int temp = level;
                int a = 0;
                while (temp > 1) {
                    if (temp > 1) {
                        --temp;
                        ++a;
                    }
                }
                toStamp2.setPotential3(potential(toStamp2.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1)));
                sucstamp = true;
            }
            c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(stamp, toStamp2));
        }
        if (Randomizer.isSuccess(ii.getSuccess(toStamp.getItemId(), c.getPlayer(), toStamp))) {
            toStamp.setLines((byte) 3);
            int level = toStamp.getState() - 16;
            int temp = level;
            int a = 0;
            while (temp > 1) {
                if (temp > 1) {
                    --temp;
                    ++a;
                }
            }
            toStamp.setPotential3(potential(toStamp.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1)));
            sucstamp = true;
        }

        useInventory.removeItem(stamp.getPosition(), (short) 1, false);
        c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(stamp, toStamp));
        c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(c.getPlayer().getId(), sucstamp, stamp.getItemId(), toStamp.getItemId()));
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static void UseEditionalStamp(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4);
        short slot = slea.readShort();
        short dst = slea.readShort();
        boolean sucstamp = false;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip toStamp;
        if (dst < 0) {
            toStamp = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            toStamp = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        Item stamp = useInventory.getItem(slot);
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            Equip toStamp2;
            toStamp2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            int level = toStamp2.getState() - 16;
            if (Randomizer.isSuccess(ii.getSuccess(toStamp2.getItemId(), c.getPlayer(), toStamp2))) {
                toStamp2.setPotential6(potential(toStamp2.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1), true));
                sucstamp = true;
            }
            c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(stamp, toStamp));
        }
        if (Randomizer.isSuccess(ii.getSuccess(toStamp.getItemId(), c.getPlayer(), toStamp))) {
            //    toStamp.setLines((byte) 3);
            int level = toStamp.getState() - 16;
            toStamp.setPotential6(potential(toStamp.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1), true));
            sucstamp = true;
        }
        useInventory.removeItem(stamp.getPosition(), (short) 1, false);
        c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(stamp, toStamp));
        c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(c.getPlayer().getId(), sucstamp, stamp.getItemId(), toStamp.getItemId()));
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static void UseChooseCube(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4);
        byte type = slea.readByte();
        Equip equip = null;
        Equip zeroequip = null;
        if (c.getPlayer().choicepotential.getPosition() > 0) {
            equip = (Equip) (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(c.getPlayer().choicepotential.getPosition()));
        } else {
            equip = (Equip) (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(c.getPlayer().choicepotential.getPosition()));
        }
        if (type == 6) { //after 옵션 선택

            if (c.getPlayer().choicepotential.getPosition() > 0) {
                equip.set(c.getPlayer().choicepotential);
            } else {
                equip.set(c.getPlayer().choicepotential);
            }
        }

        if (GameConstants.isZeroWeapon(c.getPlayer().choicepotential.getItemId())) {
            zeroequip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            zeroequip.setState(equip.getState());
            zeroequip.setLines(equip.getLines());
            zeroequip.setPotential1(equip.getPotential1());
            zeroequip.setPotential2(equip.getPotential2());
            zeroequip.setPotential3(equip.getPotential3());
        }
        c.getPlayer().choicepotential = null;
        c.getPlayer().memorialcube = null;
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, equip));
        if (zeroequip != null) {
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, zeroequip));
        }

        c.getPlayer().forceReAddItem(equip, MapleInventoryType.EQUIP);
    }

    public static final void addToScrollLog(int accountID, int charID, int scrollID, int itemID, byte oldSlots, byte newSlots, byte viciousHammer, String result, boolean ws, boolean ls, int vega) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO scroll_log VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, accountID);
            ps.setInt(2, charID);
            ps.setInt(3, scrollID);
            ps.setInt(4, itemID);
            ps.setByte(5, oldSlots);
            ps.setByte(6, newSlots);
            ps.setByte(7, viciousHammer);
            ps.setString(8, result);
            ps.setByte(9, (byte) (ws ? 1 : 0));
            ps.setByte(10, (byte) (ls ? 1 : 0));
            ps.setInt(11, vega);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void useSilverKarma(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.skip(4);
        Item scroll = chr.getInventory(MapleInventoryType.USE).getItem(slea.readShort());
        Item toScroll = chr.getInventory(MapleInventoryType.getByType((byte) slea.readShort())).getItem(slea.readShort());

        if (scroll.getItemId() == 2720000 || scroll.getItemId() == 2720001) {
            if (MapleItemInformationProvider.getInstance().isKarmaEnabled(toScroll.getItemId())) {
                if (toScroll.getType() == 1) {
                    Equip nEquip = (Equip) toScroll;
                    if (nEquip.getKarmaCount() > 0) {
                        nEquip.setKarmaCount((byte) (nEquip.getKarmaCount() - 1));
                    } else if (nEquip.getKarmaCount() == 0) {
                        chr.dropMessage(5, "가위를 사용할 수 없는 아이템입니다.");
                        return;
                    }
                }
                int flag = toScroll.getFlag();
                if (toScroll.getType() == 1) {
                    flag += ItemFlag.KARMA_EQUIP.getValue();
                } else {
                    flag += ItemFlag.KARMA_USE.getValue();
                }

                toScroll.setFlag(flag);
            } else {
                chr.dropMessage(5, "가위를 사용할 수 없는 아이템입니다.");
                return;
            }
        }

        chr.removeItem(scroll.getItemId(), -1);

        chr.getClient().getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, GameConstants.getInventoryType(toScroll.getItemId()), toScroll));
    }

    public static boolean UseUpgradeScroll(RecvPacketOpcode header, byte slot, byte dst, byte ws, MapleClient c,
                                           MapleCharacter chr) {
        return UseUpgradeScroll(header, slot, (byte) 1, dst, ws, c, chr);
    }

    public static boolean UseUpgradeScroll(RecvPacketOpcode header, byte slot, byte inv, byte dst, byte ws, MapleClient c,
                                           MapleCharacter chr) {
        boolean whiteScroll = false; // white scroll being used?
        boolean legendarySpirit = false; // legendary spirit skill
        boolean recovery = false;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll = null;
        Equip toScroll2 = null;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
            if (toScroll.getPosition() == -11) {
                toScroll2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            }
        } else { // legendary spirit
//            legendarySpirit = true; 장인의 혼
            toScroll = (Equip) chr.getInventory(inv).getItem(dst);
        }

        if (toScroll == null) {
            return false;
        }
        byte oldLevel = toScroll.getLevel();
        byte oldEnhance = toScroll.getEnhance();
        byte oldState = toScroll.getState();
        int oldFlag = toScroll.getFlag();
        byte oldSlots = toScroll.getUpgradeSlots();

        Item scroll = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (scroll == null || header == RecvPacketOpcode.USE_FLAG_SCROLL) {
            scroll = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        } else if (!GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId())
                && !GameConstants.isEquipScroll(scroll.getItemId())
                && !GameConstants.isPotentialScroll(scroll.getItemId())
                && !GameConstants.isRebirthFireScroll(scroll.getItemId()) && (scroll.getItemId() / 10000 != 204)
                && (scroll.getItemId() / 10000 != 272)) {
            scroll = chr.getInventory(ws).getItem(slot);
        }
        if (header == RecvPacketOpcode.USE_BLACK_REBIRTH_SCROLL) {
            long newRebirth = toScroll.newRebirth(ii.getReqLevel(toScroll.getItemId()), scroll.getItemId(), false);
            c.getSession().writeAndFlush(CWvsContext.useBlackRebirthScroll(toScroll, scroll, newRebirth, false));

            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(scroll.getItemId()), scroll.getPosition(), (short) 1, false, false);
            //before
            c.getSession().writeAndFlush(CWvsContext.blackRebirthResult(true, toScroll.getFire(), toScroll));

            //after
            c.getSession().writeAndFlush(CWvsContext.blackRebirthResult(false, newRebirth, toScroll));

            chr.blackRebirth = newRebirth;

            chr.blackRebirthScroll = (Equip) toScroll.copy();

            chr.blackRebirthPos = slot;
            return false;
        }

        if ((scroll.getItemId() / 100) == 20496) {
            Equip origin = (Equip) MapleItemInformationProvider.getInstance().getEquipById(toScroll.getItemId());
            toScroll.setAcc(origin.getAcc());
            toScroll.setAvoid(origin.getAvoid());
            toScroll.setDex(origin.getDex());
            toScroll.setHands(origin.getHands());
            toScroll.setHp(origin.getHp());
            toScroll.setInt(origin.getInt());
            toScroll.setJump(origin.getJump());
            toScroll.setLevel(origin.getLevel());
            toScroll.setLuk(origin.getLuk());
            toScroll.setMatk(origin.getMatk());
            toScroll.setMdef(origin.getMdef());
            toScroll.setMp(origin.getMp());
            toScroll.setSpeed(origin.getSpeed());
            toScroll.setStr(origin.getStr());
            toScroll.setUpgradeSlots(origin.getUpgradeSlots());
            toScroll.setWatk(origin.getWatk());
            toScroll.setWdef(origin.getWdef());
            toScroll.setEnhance((byte) 0);
            toScroll.setViciousHammer((byte) 0);
            chr.getInventory(MapleInventoryType.USE).removeItem(scroll.getPosition());
            c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(scroll, toScroll));
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, legendarySpirit, scroll.getItemId(), toScroll.getItemId()), true);
            return false;
        }

        if (!GameConstants.isSpecialScroll(scroll.getItemId()) && !GameConstants.isCleanSlate(scroll.getItemId())
                && !GameConstants.isEquipScroll(scroll.getItemId()) && scroll.getItemId() != 2049360
                && scroll.getItemId() != 2049361 && !GameConstants.isPotentialScroll(scroll.getItemId())
                && !GameConstants.isRebirthFireScroll(scroll.getItemId())
                && !GameConstants.isLuckyScroll(scroll.getItemId())) {
            /*
             * 1. 프로텍트, 세이프티 등 특수 주문서가 아니고, 2. 백의 주문서가 아니고, 3. 장비강화 주문서가 아니고, 4. 잠재부여 주문서가
             * 아니고, 5. 안드로이드 기계심장이 아니고 6. 환생의 불꽃이 아니고, 7. 에픽 잠재 부여 주문서가 아니고 마법화살이 아닐때, 8. 제로
             * 럭키 스크롤이 아닐때,
             *
             */
            if (toScroll.getUpgradeSlots() < 1) {
                /*
                 * 업그레이드 슬롯이 없을 때
                 */
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.dropMessage(1, "업그레이드 슬롯이 부족합니다.");
                return false;
            }
        } else if (GameConstants.isEquipScroll(scroll.getItemId())) {
            /*
             * 1. 장비강화 주문서일때, 2. 놀라운 장비강화 주문서가 아닐때, 장비 업횟수가 1 이상 남아있거나, 장비강화 횟수가 15강 이상이거나,
             * 캐시 장비 아이템일때
             *
             */
            if ((scroll.getItemId() != 2049360 && scroll.getItemId() != 2049361 && toScroll.getUpgradeSlots() >= 1)
                    || ii.isCash(toScroll.getItemId())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.dropMessage(1, "더 이상 강화할 수 없는 아이템입니다.");
                return false;
            }
        } else if (GameConstants.isPotentialScroll(scroll.getItemId())) {
            /*
             * 1. 잠재 부여 주문서이고, 이미 잠재가 붙어있을때
             */
            if (toScroll.getState() >= 1) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.dropMessage(1, "이미 잠재능력이 부여된 아이템입니다.");
                return false;
            }
        }
        if (toScroll.getItemId() / 1000 != 1672 && !GameConstants.canScroll(toScroll.getItemId())
                && GameConstants.isChaosScroll(scroll.getItemId())) {
            /*
             * 주문서를 바를 수 없는 템 (메카닉템이거나, 에반드래곤템이거나, 몬스터라이딩 아이템) 이면서 동시에 혼돈의 주문서일때
             */
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "주문서를 사용하실 수 없는 아이템입니다.");
            return false;
        }
        if (ii.isCash(toScroll.getItemId())) {
            /* 캐시 장비 아이템은 강화 자체가 불가능.
             */
            if (toScroll.getItemId() / 1000 != 1802) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.dropMessage(1, "캐시 아이템은 강화가 불가능합니다.");
                return false;
            } else { // 펫장비
                if (!ii.getName(scroll.getItemId()).contains("펫장비")) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                    chr.dropMessage(1, "펫장비에는 펫장비 주문서만 사용하실 수 있습니다.");
                    return false;
                }
            }
        }
        if (GameConstants.isTablet(scroll.getItemId()) && toScroll.getDurability() < 0) { // not a durability item
            /*
             * 내구도가 없는 아이템에 연성서를 사용할 경우.
             */
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "연성서를 사용하실 수 없는 아이템입니다.");
            return false;
        }

        Item wscroll = null;
        // Anti cheat and validation
        List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "RETURN 8");
            return false;
        }

        if (whiteScroll) { // 요것?
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000);
            if (wscroll == null) {
                whiteScroll = false;
            }
        }

        if (scroll.getItemId() == 2041200 && toScroll.getItemId() != 1122000 && toScroll.getItemId() != 1122076) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "드래곤의 돌은 혼테일의 목걸이에만 사용 가능한 아이템입니다.");
            return false;
        }

        if ((scroll.getItemId() == 2046856 || scroll.getItemId() == 2046857)
                && (toScroll.getItemId() / 1000 == 1152 || !GameConstants.isAccessory(toScroll.getItemId()))) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "악세서리에만 사용 가능한 주문서입니다.");
            return false;
        }

        if ((scroll.getItemId() == 2046991 || scroll.getItemId() == 2046992 || scroll.getItemId() == 2046996
                || scroll.getItemId() == 2046997) && GameConstants.isTwoHanded(toScroll.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "한손무기에만 사용 가능한 주문서입니다.");
            return false;
        }

        if ((scroll.getItemId() == 2047814 || scroll.getItemId() == 2047818)
                && !GameConstants.isTwoHanded(toScroll.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "두손무기에만 사용 가능한 주문서입니다.");
            return false;
        }

        if (GameConstants.isAccessoryScroll(scroll.getItemId()) && !GameConstants.isAccessory(toScroll.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            chr.dropMessage(1, "악세서리 주문서를 사용하실 수 없는 아이템입니다.");
            return false;
        }

        if (scroll.getQuantity() <= 0) {
            chr.dropMessage(1, "존재하지 않는 주문서는 사용할 수 없습니다.");
            return false;
        }
        if (toScroll.getUpgradeSlots() > 0) {
            if (scroll.getItemId() >= 2049370 && scroll.getItemId() <= 2049380) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.dropMessage(1, "아직 업그레이드 슬롯이 남아있습니다.");
                return false;
            }
        }
        if (scroll.getItemId() == 2049099 && (toScroll.getEnhance() < 22 || toScroll.getEnhance() >= 25)) {
            chr.dropMessage(1, "22성 이상 25성 미만인 아이템에만 사용할 수 있습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return false;
        }
        if (header == RecvPacketOpcode.USE_BLACK_REBIRTH_SCROLL) {
            long newRebirth = toScroll.newRebirth(ii.getReqLevel(toScroll.getItemId()), scroll.getItemId(), false);
            c.getSession().writeAndFlush(CWvsContext.useBlackRebirthScroll(toScroll, scroll, newRebirth, false));

            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(scroll.getItemId()), scroll.getPosition(), (short) 1, false, false);
            //before
            c.getSession().writeAndFlush(CWvsContext.blackRebirthResult(true, toScroll.getFire(), toScroll));

            //after
            c.getSession().writeAndFlush(CWvsContext.blackRebirthResult(false, newRebirth, toScroll));

            chr.blackRebirth = newRebirth;

            chr.blackRebirthScroll = (Equip) toScroll.copy();

            chr.blackRebirthPos = slot;
            return false;
        }

        if (ItemFlag.RETURN_SCROLL.check(toScroll.getFlag())) {
            chr.returnscroll = (Equip) toScroll.copy();
        }

        // Scroll Success/Failure/Curse
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr);
        ScrollResult scrollSuccess;
        if (scrolled == null) {
            scrollSuccess = Equip.ScrollResult.CURSE;
        } else if (GameConstants.isRebirthFireScroll(scroll.getItemId())) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else if (scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance
                || scrolled.getState() > oldState || scrolled.getFlag() > oldFlag) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else if ((GameConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots)) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        } else {
            scrollSuccess = Equip.ScrollResult.FAIL;
            if (ItemFlag.RECOVERY_SHIELD.check(toScroll.getFlag())) {
                recovery = true;
            }
        }

        // Update
        if (recovery) {
            chr.dropMessage(5, "주문서의 효과로 사용된 주문서가 차감되지 않았습니다.");
        } else if (GameConstants.isZero(chr.getJob()) && toScroll.getPosition() == -11) {

        } else {
            chr.getInventory(GameConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false);
        }
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(scroll.getItemId()), wscroll.getPosition(), (short) 1, false, false);
        }

        if (header != RecvPacketOpcode.USE_FLAG_SCROLL) {
            if (ItemFlag.RECOVERY_SHIELD.check(toScroll.getFlag())) {
                toScroll.setFlag((toScroll.getFlag() - ItemFlag.RECOVERY_SHIELD.getValue()));
                if (GameConstants.isZero(chr.getJob()) && toScroll2 != null) {
                    toScroll2.setFlag((toScroll2.getFlag() - ItemFlag.RECOVERY_SHIELD.getValue()));
                }
            }

            if (ItemFlag.SAFETY_SHIELD.check(toScroll.getFlag())) {
                toScroll.setFlag((toScroll.getFlag() - ItemFlag.SAFETY_SHIELD.getValue()));
                if (GameConstants.isZero(chr.getJob()) && toScroll2 != null) {
                    toScroll2.setFlag((toScroll2.getFlag() - ItemFlag.SAFETY_SHIELD.getValue()));
                }
            }

            if (ItemFlag.PROTECT_SHIELD.check(toScroll.getFlag())) {
                toScroll.setFlag((toScroll.getFlag() - ItemFlag.PROTECT_SHIELD.getValue()));
                if (GameConstants.isZero(chr.getJob()) && toScroll2 != null) {
                    toScroll2.setFlag((toScroll2.getFlag() - ItemFlag.PROTECT_SHIELD.getValue()));
                }
            }

            if (ItemFlag.LUCKY_PROTECT_SHIELD.check(toScroll.getFlag())) {
                toScroll.setFlag((toScroll.getFlag() - ItemFlag.LUCKY_PROTECT_SHIELD.getValue()));
                if (GameConstants.isZero(chr.getJob()) && toScroll2 != null) {
                    toScroll2.setFlag((toScroll2.getFlag() - ItemFlag.LUCKY_PROTECT_SHIELD.getValue()));
                }
            }
        }

        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            c.getSession().writeAndFlush(InventoryPacket.scrolledItem(scroll, toScroll, true, false, dst < 0));
            if (dst < 0) {
                chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            c.getSession().writeAndFlush(InventoryPacket.scrolledItem(scroll, scrolled, false, false, dst < 0));
            if (c.getPlayer().returnscroll != null) {
                c.getSession().writeAndFlush(CWvsContext.returnEffectConfirm(c.getPlayer().returnscroll, scroll.getItemId()));
                c.getSession().writeAndFlush(CWvsContext.returnEffectModify(c.getPlayer().returnscroll, scroll.getItemId()));
            }
        }
        // equipped item was scrolled and changed
        if (GameConstants.isZero(chr.getJob()) && toScroll.getPosition() == -11) {
        } else {
            chr.getMap().broadcastMessage(chr, CField.getScrollEffect(c.getPlayer().getId(), scrollSuccess,
                    legendarySpirit, scroll.getItemId(), toScroll.getItemId()), true);
        }
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE)) {
            chr.equipChanged();
        }

        //아이템 인벤토리 작 날라가는거 여기다 저장한번?
        return true;
    }

    public static void UseEditionalScroll(final LittleEndianAccessor slea, final MapleClient c) {
        try {
            slea.skip(4);
            short mode = slea.readShort();
            Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(mode);
            if ((toUse.getItemId() >= 2048305 && toUse.getItemId() <= 2048316)) {
                short slot = slea.readShort();
                Item item;
                if (slot < 0) {
                    item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
                } else {
                    item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
                }

                if (GameConstants.isZero(c.getPlayer().getJob()) && slot < 0) {
                    Item item1;
                    Item item2;
                    item1 = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
                    item2 = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    Equip eq1 = (Equip) item1;
                    Equip eq2 = (Equip) item2;
                    if ((eq1.getState() == 0 || eq2.getState() == 0) || (eq1.getState() == 1 && eq1.getPotential1() == 0) || (eq2.getState() == 1 && eq2.getPotential1() == 0)) {
                        c.getPlayer().dropMessage(1, "먼저 잠재능력을 열어주세요.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }

                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    boolean succes = Randomizer.isSuccess(ii.getSuccess(item.getItemId(), c.getPlayer(), item));
                    if (succes) {
                        int alpha_option = 0, alpha_option2 = 0, alpha_option3_sbal = 0, alpha_level = 2;
                        int beta_option = 0, beta_option2 = 0, beta_option3_sbal = 0, beta_level = 2;
                        alpha_option = potential(eq1.getItemId(), alpha_level, true);
                        alpha_option2 = potential(eq1.getItemId(), alpha_level, true);
                        alpha_option3_sbal = potential(eq1.getItemId(), alpha_level, true);

                        beta_option = potential(eq2.getItemId(), beta_level, true);
                        beta_option2 = potential(eq2.getItemId(), beta_level, true);
                        beta_option3_sbal = potential(eq2.getItemId(), beta_level, true);

                        if (Randomizer.nextInt(100) < 20 || toUse.getItemId() == 2048306) { //20퍼센트 확률로 3줄
                            eq1.setPotential4(alpha_option);
                            eq1.setPotential5(alpha_option2);
                            eq1.setPotential6(alpha_option3_sbal);

                            eq2.setPotential4(beta_option);
                            eq2.setPotential5(beta_option2);
                            eq2.setPotential6(beta_option3_sbal);
                        } else {
                            eq1.setPotential4(alpha_option);
                            eq1.setPotential5(alpha_option2);

                            eq2.setPotential4(beta_option);
                            eq2.setPotential5(beta_option2);
                        }
                    }

                    c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(toUse, item1));
                    c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(toUse, item2));
                    c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(c.getPlayer().getId(), succes, toUse.getItemId(), item.getItemId()));
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false);
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                } else {
                    Equip eq = (Equip) item;
                    if (eq.getState() == 0 || eq.getPotential1() == 0) {
                        c.getPlayer().dropMessage(1, "먼저 잠재능력을 열어주세요.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    boolean succes = Randomizer.isSuccess(ii.getSuccess(item.getItemId(), c.getPlayer(), item));
                    if (succes) {
                        int option = 0, option2 = 0, option3_sbal = 0, level = 2;
                        option = potential(eq.getItemId(), level, true);
                        option2 = potential(eq.getItemId(), level, true);
                        option3_sbal = potential(eq.getItemId(), level, true);

                        if (Randomizer.nextInt(100) < 20 || toUse.getItemId() == 2048306) { //20퍼센트 확률로 3줄
                            eq.setPotential4(option);
                            eq.setPotential5(option2);
                            eq.setPotential6(option3_sbal);
                        } else {
                            eq.setPotential4(option);
                            eq.setPotential5(option2);
                        }
                    }

                    c.getSession().writeAndFlush(InventoryPacket.updateScrollandItem(toUse, item));
                    c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(c.getPlayer().getId(), succes, toUse.getItemId(), eq.getItemId()));
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false);
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static final boolean UseSkillBook(final short slot, final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            return false;
        }
        final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getEquipStats(toUse.getItemId());
        if (skilldata == null) { // Hacking or used an unknown item
            return false;
        }
        boolean canuse = false, success = false;
        int skill = 0, maxlevel = 0;

        final Integer SuccessRate = skilldata.get("success");
        final Integer ReqSkillLevel = skilldata.get("reqSkillLevel");
        final Integer MasterLevel = skilldata.get("masterLevel");

        byte i = 0;
        Integer CurrentLoopedSkillId;
        while (true) {
            CurrentLoopedSkillId = skilldata.get("skillid" + i);
            i++;
            if (CurrentLoopedSkillId == null || MasterLevel == null) {
                break; // End of data
            }
            final Skill CurrSkillData = SkillFactory.getSkill(CurrentLoopedSkillId);
            if (CurrSkillData != null && CurrSkillData.canBeLearnedBy(chr) && (ReqSkillLevel == null || chr.getSkillLevel(CurrSkillData) >= ReqSkillLevel) && chr.getMasterLevel(CurrSkillData) < MasterLevel) {
                canuse = true;
                if (SuccessRate == null || Randomizer.nextInt(100) <= SuccessRate) {
                    success = true;
                    chr.changeSingleSkillLevel(CurrSkillData, chr.getSkillLevel(CurrSkillData), (byte) (int) MasterLevel);
                } else {
                    success = false;
                }
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(itemId), slot, (short) 1, false);
                break;
            }
        }
        c.getPlayer().getMap().broadcastMessage(CWvsContext.useSkillBook(chr, skill, maxlevel, canuse, success));
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        return canuse;
    }

    public static final void UseCatchItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        final short slot = slea.readShort();
        final int itemid = slea
                .readInt();
        final MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMap map = chr.getMap();

        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mob != null && itemid / 10000 == 227 && MapleItemInformationProvider.getInstance().getCardMobId(itemid) == mob.getId()) {
            if (!MapleItemInformationProvider.getInstance().isMobHP(itemid) || mob.getHp() <= mob.getMobMaxHp() / 2) {
                map.broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), (byte) 1));
                map.killMonster(mob, chr, true, false, (byte) 1);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
                if (MapleItemInformationProvider.getInstance().getCreateId(itemid) > 0) {
                    MapleInventoryManipulator.addById(c, MapleItemInformationProvider.getInstance().getCreateId(itemid), (short) 1, "Catch item " + itemid + " on " + FileoutputUtil.CurrentReadable_Date());
                }
            } else {
                map.broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), (byte) 0));
                c.getSession().writeAndFlush(CWvsContext.catchMob(mob.getId(), itemid, (byte) 0));
            }
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void UseMountFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.readInt();
        final short slot = slea.readShort();
        final int itemid = slea.readInt(); //2260000 usually
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        final MapleMount mount = chr.getMount();

        if (itemid / 10000 == 226 && toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && mount != null) {
            final int fatigue = mount.getFatigue();

            boolean levelup = false;
            mount.setFatigue((byte) -30);

            if (fatigue > 0) {
                mount.increaseExp();
                final int level = mount.getLevel();
                if (level < 30 && mount.getExp() >= GameConstants.getMountExpNeededForLevel(level + 1)) {
                    mount.setLevel((byte) (level + 1));
                    levelup = true;
                }
            }
            chr.getMap().broadcastMessage(CWvsContext.updateMount(chr, levelup));
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void UseScriptedNPCItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(4);
//        int crcid = slea.readInt();
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(GameConstants.getInventoryType(itemId)).getItem(slot);
        long expiration_days = 0;
        int mountid = 0;

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && !chr.inPVP()) {
            switch (toUse.getItemId()) {
                case 2430732: {
                    int id = 0;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430885: {
                    int id = 1;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430886: {
                    int id = 2;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430887: {
                    int id = 3;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430888: {
                    int id = 4;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430889: {
                    int id = 5;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430890: {
                    int id = 6;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430891: {
                    int id = 7;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430892: {
                    int id = 8;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430893: {
                    int id = 9;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430894: {
                    int id = 10;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430945: {
                    int id = 11;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2430946: {
                    int id = 12;
                    chr.addCustomItem(id);
                    c.getPlayer().dropMessage(5, GameConstants.customItems.get(id).getName() + " 를 획득하였습니다. 소비칸에 특수 장비창 -> 인벤토리를 확인해주세요.");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2431940: {
                    int pirodo = 1000;
                    switch (c.getPlayer().getTier()) {
                        case 1: {
                            pirodo = 60;
                            break;
                        }
                        case 2: {
                            pirodo = 80;
                            break;
                        }
                        case 3: {
                            pirodo = 100;
                            break;
                        }
                        case 4: {
                            pirodo = 120;
                            break;
                        }
                        case 5: {
                            pirodo = 160;
                            break;
                        }
                        case 6: {
                            pirodo = 160;
                            break;
                        }
                        case 7: {
                            pirodo = 160;
                            break;
                        }
                        case 8: {
                            pirodo = 160;
                            break;
                        }
                    }
                    long point = c.getPlayer().getKeyValue(123, "pp") + 10;
                    if (c.getPlayer().getKeyValue(123, "pp") >= pirodo) {
                        c.getPlayer().dropMessage(5, "이미 모든 피로도가 충전되있습니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                    if (c.getPlayer().getKeyValue(123, "pp") + 10 > pirodo) {
                        point = pirodo;
                    }
                    c.getPlayer().setKeyValue(123, "pp", String.valueOf(point));
                    c.getPlayer().dropMessage(5, "피로도가 증가했습니다. 피로도 : " + c.getPlayer().getKeyValue(123, "pp"));
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                }
                case 2433509: {
                    NPCScriptManager.getInstance().startItem(c, 9000162, "consume_2433509");
                    break;
                }
                case 2433510: {
                    NPCScriptManager.getInstance().startItem(c, 9000162, "consume_2433510");
                    break;
                }
                case 2434006: {
                    NPCScriptManager.getInstance().startItem(c, 9000162, "consume_2434006");
                    break;
                }
                case 5680222: {
                    NPCScriptManager.getInstance().startItem(c, 9000216, "mannequin_add");
                    break;
                }
                case 5680531: {
                    NPCScriptManager.getInstance().startItem(c, 9000216, "mannequin_slotadd");
                    break;
                }
                case 2435719:
                case 2435902: {
                    MatrixHandler.UseCoreJamStone(c, toUse.getItemId(), Randomizer.nextLong());
                    break;
                }
                case 2631527: {
                    MatrixHandler.UseEnforcedCoreJamStone(c, toUse.getItemId(), Randomizer.nextInt());
                    break;
                }
                case 2438411:
                case 2438412: {
                    MatrixHandler.UseMirrorCoreJamStone(c, toUse.getItemId(), Randomizer.nextInt());
                    break;
                }
                case 2430007: { // Blank Compass
                    final MapleInventory inventory = chr.getInventory(MapleInventoryType.SETUP);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);

                    if (inventory.countById(3994102) >= 20 // Compass Letter "North"
                            && inventory.countById(3994103) >= 20 // Compass Letter "South"
                            && inventory.countById(3994104) >= 20 // Compass Letter "East"
                            && inventory.countById(3994105) >= 20) { // Compass Letter "West"
                        MapleInventoryManipulator.addById(c, 2430008, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date()); // Gold Compass
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994102, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994103, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994104, 20, false, false);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.SETUP, 3994105, 20, false, false);
                    } else {
                        MapleInventoryManipulator.addById(c, 2430007, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date()); // Blank Compass
                    }
                    NPCScriptManager.getInstance().startItem(c, 9900000, "consume_2084001");
                    break;
                }
                case 2430008: { // Gold Compass
                    chr.saveLocation(SavedLocationType.RICHIE);
                    MapleMap map;
                    boolean warped = false;

                    for (int i = 390001000; i <= 390001004; i++) {
                        map = c.getChannelServer().getMapFactory().getMap(i);

                        if (map.getCharactersSize() == 0) {
                            chr.changeMap(map, map.getPortal(0));
                            warped = true;
                            break;
                        }
                    }
                    if (warped) { // Removal of gold compass
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    } else { // Or mabe some other message.
                        c.getPlayer().dropMessage(5, "All maps are currently in use, please try again later.");
                    }
                    break;
                }
                case 2430112: //miracle cube fragment
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430112) >= 25) {
                            if (MapleInventoryManipulator.checkSpace(c, 2049400, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 25, true, false)) {
                                MapleInventoryManipulator.addById(c, 2049400, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430112) >= 10) {
                            if (MapleInventoryManipulator.checkSpace(c, 2049400, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 10, true, false)) {
                                MapleInventoryManipulator.addById(c, 2049401, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 10 Fragments for a Potential Scroll, 25 for Advanced Potential Scroll.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430481: //super miracle cube fragment
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430481) >= 30) {
                            if (MapleInventoryManipulator.checkSpace(c, 2049701, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 30, true, false)) {
                                MapleInventoryManipulator.addById(c, 2049701, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430481) >= 20) {
                            if (MapleInventoryManipulator.checkSpace(c, 2049300, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 20, true, false)) {
                                MapleInventoryManipulator.addById(c, 2049300, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 20 Fragments for a Advanced Equip Enhancement Scroll, 30 for Epic Potential Scroll 80%.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430691: // nebulite diffuser fragment
                    if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430691) >= 10) {
                            if (MapleInventoryManipulator.checkSpace(c, 5750001, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 10, true, false)) {
                                MapleInventoryManipulator.addById(c, 5750001, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 10 Fragments for a Nebulite Diffuser.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430748: // premium fusion ticket 
                    if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.USE).countById(2430748) >= 20) {
                            if (MapleInventoryManipulator.checkSpace(c, 4420000, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 20, true, false)) {
                                MapleInventoryManipulator.addById(c, 4420000, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 20 Fragments for a Premium Fusion Ticket.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 5680019: {//starling hair 
                    //if (c.getPlayer().getGender() == 1) {
                    int hair = 32150 + (c.getPlayer().getHair() % 10);
                    c.getPlayer().setHair(hair);
                    c.getPlayer().updateSingleStat(MapleStat.HAIR, hair);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (byte) 1, false);
                    //}
                    break;
                }
                case 5680020: {//starling hair 
                    //if (c.getPlayer().getGender() == 0) {
                    int hair = 32160 + (c.getPlayer().getHair() % 10);
                    c.getPlayer().setHair(hair);
                    c.getPlayer().updateSingleStat(MapleStat.HAIR, hair);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (byte) 1, false);
                    //}
                    break;
                }
                case 3994225:
                    c.getPlayer().dropMessage(5, "Please bring this item to the NPC.");
                    break;
                case 2430212: //energy drink
                    MapleQuestStatus marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.ENERGY_DRINK));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    long lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + (600000) > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "You can only use one energy drink per 10 minutes.");
                    } else if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 5);
                    }
                    break;
                case 2430213: //energy drink
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.ENERGY_DRINK));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + (600000) > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "You can only use one energy drink per 10 minutes.");
                    } else if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 10);
                    }
                    break;
                case 2430220: //energy drink
                case 2430214: //energy drink
                    if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 30);
                    }
                    break;
                case 2430227: //energy drink
                    if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 50);
                    }
                    break;
                case 2430231: //energy drink
                    marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.ENERGY_DRINK));
                    if (marr.getCustomData() == null) {
                        marr.setCustomData("0");
                    }
                    lastTime = Long.parseLong(marr.getCustomData());
                    if (lastTime + (600000) > System.currentTimeMillis()) {
                        c.getPlayer().dropMessage(5, "You can only use one energy drink per 10 minutes.");
                    } else if (c.getPlayer().getFatigue() > 0) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().setFatigue(c.getPlayer().getFatigue() - 40);
                    }
                    break;
                case 2430144: //smb
                    final int itemid = Randomizer.nextInt(373) + 2290000;
                    if (MapleItemInformationProvider.getInstance().itemExists(itemid) && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Special") && !MapleItemInformationProvider.getInstance().getName(itemid).contains("Event")) {
                        MapleInventoryManipulator.addById(c, itemid, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                case 2430370:
                    if (MapleInventoryManipulator.checkSpace(c, 2028062, (short) 1, "")) {
                        MapleInventoryManipulator.addById(c, 2028062, (short) 1, "Reward item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    }
                    break;
                case 2430158: //lion king
                    if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000630) >= 100) {
                            if (MapleInventoryManipulator.checkSpace(c, 4310010, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 4310010, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000630) >= 50) {
                            if (MapleInventoryManipulator.checkSpace(c, 4310009, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 4310009, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 50 Purification Totems for a Noble Lion King Medal, 100 for Royal Lion King Medal.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430159:
                    MapleQuest.getInstance(3182).forceComplete(c.getPlayer(), 2161004);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                    break;
                case 2430200: //thunder stone
                    if (c.getPlayer().getQuestStatus(31152) != 2) {
                        c.getPlayer().dropMessage(5, "You have no idea how to use it.");
                    } else if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000660) >= 1 && c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000661) >= 1 && c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000662) >= 1 && c.getPlayer().getInventory(MapleInventoryType.ETC).countById(4000663) >= 1) {
                            if (MapleInventoryManipulator.checkSpace(c, 4032923, 1, "") && MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, toUse.getItemId(), 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000660, 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000661, 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000662, 1, true, false) && MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000663, 1, true, false)) {
                                MapleInventoryManipulator.addById(c, 4032923, (short) 1, "Scripted item: " + toUse.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                            } else {
                                c.getPlayer().dropMessage(5, "Please make some space.");
                            }
                        } else {
                            c.getPlayer().dropMessage(5, "There needs to be 1 of each Stone for a Dream Key.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Please make some space.");
                    }
                    break;
                case 2430130:
                case 2430131: //energy charge
                    if (GameConstants.isResist(c.getPlayer().getJob())) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                        c.getPlayer().gainExp(20000 + (c.getPlayer().getLevel() * 50 * c.getChannelServer().getExpRate()), true, true, false);
                    } else {
                        c.getPlayer().dropMessage(5, "You may not use this item.");
                    }
                    break;
                case 2430132:
                case 2430133:
                case 2430134: //resistance box
                case 2430142:
                    if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 1) {
                        if (c.getPlayer().getJob() == 3200 || c.getPlayer().getJob() == 3210 || c.getPlayer().getJob() == 3211 || c.getPlayer().getJob() == 3212) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                            MapleInventoryManipulator.addById(c, 1382101, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else if (c.getPlayer().getJob() == 3300 || c.getPlayer().getJob() == 3310 || c.getPlayer().getJob() == 3311 || c.getPlayer().getJob() == 3312) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                            MapleInventoryManipulator.addById(c, 1462093, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else if (c.getPlayer().getJob() == 3500 || c.getPlayer().getJob() == 3510 || c.getPlayer().getJob() == 3511 || c.getPlayer().getJob() == 3512) {
                            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                            MapleInventoryManipulator.addById(c, 1492080, (short) 1, "Scripted item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else {
                            c.getPlayer().dropMessage(5, "You may not use this item.");
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "Make some space.");
                    }
                    break;
                case 2430036: //크로코 1일권
                    mountid = 1027;
                    expiration_days = 1;
                    break;
                case 2430053: //크로코 30일권
                    mountid = 1027;
                    expiration_days = 30;
                    break;
                case 2430037: //네이키드 바이크 1일권
                    mountid = 1028;
                    expiration_days = 1;
                    break;
                case 2430054: //네이키드 바이크 30일권
                    mountid = 1028;
                    expiration_days = 30;
                    break;
                case 2430038: //핑크 스쿠터 1일권
                    mountid = 1029;
                    expiration_days = 1;
                    break;
                case 2430257: //핑크 스쿠터 7일권
                    mountid = 1029;
                    expiration_days = 7;
                    break;
                case 2430055: //핑크 스쿠터 30일권
                    mountid = 1029;
                    expiration_days = 30;
                    break;
                case 2430039: //근두운 1일권
                    mountid = 1030;
                    expiration_days = 1;
                    break;
                case 2430040: //발록 1일권
                    mountid = 1031;
                    expiration_days = 1;
                    break;
                case 2430259: //발록 3일권
                    mountid = 1031;
                    expiration_days = 3;
                    break;
                case 2430225: //발록 10일권
                    mountid = 1031;
                    expiration_days = 10;
                    break;
                case 2430242: //오토바이 10일권
                    mountid = 1063;
                    expiration_days = 10;
                    break;
                case 2430261: //파워드 수트 3일권
                    mountid = 1064;
                    expiration_days = 3;
                    break;
                case 2430243: //파워드 수트 10일권
                    mountid = 1064;
                    expiration_days = 10;
                    break;
                case 2430249: //나무 비행기 3일권
                    mountid = 80001027;
                    expiration_days = 3;
                    break;
                case 2430056: //미스트 발록 30일권
                    mountid = 1035;
                    expiration_days = 30;
                    break;
                case 2430057: //경주용 카트 30일권
                    mountid = 1033;
                    expiration_days = 30;
                    break;
                case 2430072: //ZD타이거 7일권
                    mountid = 1034;
                    expiration_days = 7;
                    break;
                case 2430272: //로우 라이더 3일권 - Test
                    mountid = 80001032;
                    expiration_days = 3;
                    break;
                case 2430275: //spiegelmann
                    mountid = 80001033;
                    expiration_days = 7;
                    break;
                case 2430075: //low rider 15 day
                    mountid = 1038;
                    expiration_days = 15;
                    break;
                case 2430076: //red truck 15 day
                    mountid = 1039;
                    expiration_days = 15;
                    break;
                case 2430077: //gargoyle 15 day
                    mountid = 1040;
                    expiration_days = 15;
                    break;
                case 2430080: //shinjo 20 day
                    mountid = 1042;
                    expiration_days = 20;
                    break;
                case 2430082: //orange mush 7 day
                    mountid = 1044;
                    expiration_days = 7;
                    break;
                case 2430260: //orange mush 7 day
                    mountid = 1044;
                    expiration_days = 3;
                    break;
                case 2430091: //nightmare 10 day
                    mountid = 1049;
                    expiration_days = 10;
                    break;
                case 2430092: //yeti 10 day
                    mountid = 1050;
                    expiration_days = 10;
                    break;
                case 2430263: //yeti 10 day
                    mountid = 1050;
                    expiration_days = 3;
                    break;
                case 2430093: //ostrich 10 day
                    mountid = 1051;
                    expiration_days = 10;
                    break;
                case 2430101: //pink bear 10 day
                    mountid = 1052;
                    expiration_days = 10;
                    break;
                case 2430102: //transformation robo 10 day
                    mountid = 1053;
                    expiration_days = 10;
                    break;
                case 2430103: //chicken 30 day
                    mountid = 1054;
                    expiration_days = 30;
                    break;
                case 2430266: //chicken 30 day
                    mountid = 1054;
                    expiration_days = 3;
                    break;
                case 2430265: //chariot
                    mountid = 1151;
                    expiration_days = 3;
                    break;
                case 2430258: //law officer
                    mountid = 1115;
                    expiration_days = 365;
                    break;
                case 2430117: //lion 1 year
                    mountid = 1036;
                    expiration_days = 365;
                    break;
                case 2430118: //red truck 1 year
                    mountid = 1039;
                    expiration_days = 365;
                    break;
                case 2430119: //gargoyle 1 year
                    mountid = 1040;
                    expiration_days = 365;
                    break;
                case 2430120: //unicorn 1 year
                    mountid = 1037;
                    expiration_days = 365;
                    break;
                case 2430271: //부엉이 라이딩 3일 이용권
                    mountid = 80001191;
                    expiration_days = 3;
                    break;
                case 2430149: //레오나르도 라이딩 30일권
                    mountid = 1072;
                    expiration_days = 30;
                    break;
                case 2430262: //레오나르도 라이딩 3일권
                    mountid = 1072;
                    expiration_days = 3;
                    break;
                case 2430264: //마녀의 빗자루 3일권
                    mountid = 1019;
                    expiration_days = 3;
                    break;
                case 2430179: //마녀의 빗자루 15일권
                    mountid = 80001026;
                    expiration_days = 15;
                    break;
                case 2430283: //돌격 목마 10일권
                    mountid = 1025;
                    expiration_days = 10;
                    break;
                case 2430313: //helicopter
                    mountid = 1156;
                    expiration_days = -1;
                    break;
                case 2430317: //frog
                    mountid = 1121;
                    expiration_days = -1;
                    break;
                case 2430319: //turtle
                    mountid = 1122;
                    expiration_days = -1;
                    break;
                case 2430321: //buffalo
                    mountid = 1123;
                    expiration_days = -1;
                    break;
                case 2430323: //tank
                    mountid = 1124;
                    expiration_days = -1;
                    break;
                case 2430325: //viking
                    mountid = 1129;
                    expiration_days = -1;
                    break;
                case 2430327: //pachinko
                    mountid = 1130;
                    expiration_days = -1;
                    break;
                case 2430329: //kurenai
                    mountid = 1063;
                    expiration_days = -1;
                    break;
                case 2430331: //horse
                    mountid = 1025;
                    expiration_days = -1;
                    break;
                case 2430333: //tiger
                    mountid = 1034;
                    expiration_days = -1;
                    break;
                case 2430335: //hyena
                    mountid = 1136;
                    expiration_days = -1;
                    break;
                case 2430337: //ostrich
                    mountid = 1051;
                    expiration_days = -1;
                    break;
                case 2430339: //low rider
                    mountid = 1138;
                    expiration_days = -1;
                    break;
                case 2430341: //napoleon
                    mountid = 1139;
                    expiration_days = -1;
                    break;
                case 2430343: //croking
                    mountid = 1027;
                    expiration_days = -1;
                    break;
                case 2430346: //lovely
                    mountid = 1029;
                    expiration_days = -1;
                    break;
                case 2430348: //retro
                    mountid = 1028;
                    expiration_days = -1;
                    break;
                case 2430350: //f1
                    mountid = 1033;
                    expiration_days = -1;
                    break;
                case 2430352: //power suit
                    mountid = 1064;
                    expiration_days = -1;
                    break;
                case 2430354: //giant rabbit
                    mountid = 1096;
                    expiration_days = -1;
                    break;
                case 2430356: //small rabit
                    mountid = 1101;
                    expiration_days = -1;
                    break;
                case 2430358: //rabbit rickshaw
                    mountid = 1102;
                    expiration_days = -1;
                    break;
                case 2430360: //chicken
                    mountid = 1054;
                    expiration_days = -1;
                    break;
                case 2430362: //transformer
                    mountid = 1053;
                    expiration_days = -1;
                    break;
                case 2430292: //hot air
                    mountid = 1145;
                    expiration_days = 90;
                    break;
                case 2430294: //nadeshiko
                    mountid = 1146;
                    expiration_days = 90;
                    break;
                case 2430296: //pegasus
                    mountid = 1147;
                    expiration_days = 90;
                    break;
                case 2430298: //dragon
                    mountid = 1148;
                    expiration_days = 90;
                    break;
                case 2430300: //broom
                    mountid = 1149;
                    expiration_days = 90;
                    break;
                case 2430302: //cloud
                    mountid = 1150;
                    expiration_days = 90;
                    break;
                case 2430304: //chariot
                    mountid = 1151;
                    expiration_days = 90;
                    break;
                case 2430306: //nightmare
                    mountid = 1152;
                    expiration_days = 90;
                    break;
                case 2430308: //rog
                    mountid = 1153;
                    expiration_days = 90;
                    break;
                case 2430310: //mist rog
                    mountid = 1154;
                    expiration_days = 90;
                    break;
                case 2430312: //owl
                    mountid = 1156;
                    expiration_days = 90;
                    break;
                case 2430314: //helicopter
                    mountid = 1156;
                    expiration_days = 90;
                    break;
                case 2430316: //pentacle
                    mountid = 1118;
                    expiration_days = 90;
                    break;
                case 2430318: //frog
                    mountid = 1121;
                    expiration_days = 90;
                    break;
                case 2430320: //turtle
                    mountid = 1122;
                    expiration_days = 90;
                    break;
                case 2430322: //buffalo
                    mountid = 1123;
                    expiration_days = 90;
                    break;
                case 2430326: //viking
                    mountid = 1129;
                    expiration_days = 90;
                    break;
                case 2430328: //pachinko
                    mountid = 1130;
                    expiration_days = 90;
                    break;
                case 2430330: //kurenai
                    mountid = 1063;
                    expiration_days = 90;
                    break;
                case 2430332: //horse
                    mountid = 1025;
                    expiration_days = 90;
                    break;
                case 2430334: //tiger
                    mountid = 1034;
                    expiration_days = 90;
                    break;
                case 2430336: //hyena
                    mountid = 1136;
                    expiration_days = 90;
                    break;
                case 2430338: //ostrich
                    mountid = 1051;
                    expiration_days = 90;
                    break;
                case 2430340: //low rider
                    mountid = 1138;
                    expiration_days = 90;
                    break;
                case 2430342: //napoleon
                    mountid = 1139;
                    expiration_days = 90;
                    break;
                case 2430344: //croking
                    mountid = 1027;
                    expiration_days = 90;
                    break;
                case 2430347: //lovely
                    mountid = 1029;
                    expiration_days = 90;
                    break;
                case 2430349: //retro
                    mountid = 1028;
                    expiration_days = 90;
                    break;
                case 2430369: //나이트메어 10일권
                    mountid = 1049;
                    expiration_days = 10;
                    break;
                case 2430392: //시그너스 전차 90일권
                    mountid = 80001038;
                    expiration_days = 90;
                    break;
                case 2430232: //복만이 10일권
                    mountid = 1106;
                    expiration_days = 10;
                    break;
                case 2430206: //경주용 카트 7일권
                    mountid = 1033;
                    expiration_days = 7;
                    break;
                case 2430211: //경주용 카트 30일권
                    mountid = 80001009;
                    expiration_days = 30;
                    break;
                case 2430934: //더블 신수라이딩 영구권
                    mountid = 1042;
                    expiration_days = 0;
                    break;
                case 2430458: //꼬마 토끼 라이딩
                    mountid = 80001044;
                    expiration_days = 7;
                    break;
                case 2430521: //꼬마토끼 라이딩 30일권
                    mountid = 80001044;
                    expiration_days = 30;
                    break;
                case 2430506: //버팔로 라이딩
                    mountid = 80001082;
                    expiration_days = 30;
                    break;
                case 2430507: //토끼수레 라이딩
                    mountid = 80001083;
                    expiration_days = 30;
                    break;
                case 2430508: //슈퍼래빗
                    mountid = 80001175;
                    expiration_days = 30;
                    break;
                case 2430518: //인디언호그
                    mountid = 80001090;
                    expiration_days = 30;
                    break;
                case 2430908: //더블 슈퍼래빗
                    mountid = 80001175;
                    expiration_days = 30;
                    break;
                case 2430927: //더블 트랜스폼
                    mountid = 80001183;
                    expiration_days = 30;
                    break;
                case 2430727: //빨간 트럭 30일권
                    mountid = 80001148;
                    expiration_days = 30;
                    break;
                case 2430938: //빨간 트럭 영구권
                    mountid = 80001148;
                    expiration_days = 0;
                    break;
                case 2430937: //더블 막시무스 영구권
                    mountid = 80001193;
                    expiration_days = 0;
                    break;
                case 2430939: //더블 파워수트 영구권
                    mountid = 80001195;
                    expiration_days = 0;
                    break;
                case 2434290:
                    chr.gainHonor(10000);
                    chr.removeItem(2434290, -1);
                    break;
                case 2434287:
                    chr.gainHonor(-10000);
                    chr.gainItem(2432970, 1);
                    chr.removeItem(2434287, -1);
                    break;
                case 2434813:
                    chr.gainItem(4001852, 1);
                    chr.removeItem(2434813, -1);
                    break;
                case 2434814:
                    chr.gainItem(4001853, 1);
                    chr.removeItem(2434814, -1);
                    break;
                case 2434815:
                    chr.gainItem(4001854, 1);
                    chr.removeItem(2434815, -1);
                    break;
                case 2434816:
                    chr.gainItem(4001862, 1);
                    chr.removeItem(2434816, -1);
                    break;
                /*
                 case 2431965: // - 기본 데미지 스킨
                 case 2431966: // - 디지털라이즈 데미지 스킨
                 case 2431967: // - 크리티아스 데미지 스킨
                 case 2432084: // - 디지털라이즈 데미지 스킨
                 case 2432131: // - 파티 퀘스트 데미지 스킨
                 case 2432153: // - 임팩티브 데미지 스킨
                 case 2432154: // - 달콤한 전통 한과 데미지 스킨
                 case 2432207: // - 클럽 헤네시스 데미지 스킨
                 case 2432354: // - 메리 크리스마스 데미지 스킨
                 case 2432355: // - 눈 꽃송이 데미지 스킨
                 case 2432465: // - 알리샤의 데미지 스킨
                 case 2432479: // - 도로시의 데미지 스킨
                 case 2432526: // - 키보드 워리어 데미지 스킨
                 case 2432532: // - 살랑살랑 봄바람 데미지 스킨
                 case 2432592: // - 솔로부대 데미지 스킨
                 case 2432637: // - 달콤한 전통 한과 데미지 스킨
                 case 2432638: // - 임팩티브 데미지 스킨
                 case 2432639: // - 키보드 워리어 데미지 스킨
                 case 2432640: // - 레미너선스 데미지 스킨
                 case 2432657: // - 데미지 스킨 저장 스크롤?
                 case 2432658: // - 달콤한 전통 한과 데미지 스킨
                 case 2432659: // - 임팩티브 데미지 스킨
                 case 2432660: // - 키보드 워리어 데미지 스킨
                 case 2432661: // - 레미너선스 데미지 스킨
                 case 2432710: // - 주황버섯 데미지 스킨
                 case 2432836: // - 왕관 데미지 스킨
                 case 2432972: // - 눈 꽃송이 데미지 스킨
                 case 2432973: // - 모노톤 데미지 스킨
                 case 2433063: // - 스타플래닛 데미지 스킨
                 case 2433178: // - 할로윈 데미지 스킨
                 case 2433456: // - 한글날 데미지 스킨
                 case 2433631: // - 네네치킨 데미지 스킨
                 case 2433655: // - 네네치킨 데미지 스킨
                 case 2433715: // - 색동 데미지 스킨
                 case 2433804: // - 커플부대 데미지 스킨
                 case 2433913: // - 예티X페페 데미지 스킨
                 case 2433919: // - 주황버섯 데미지 스킨
                 case 2433980: // - 슬라임X주황버섯 데미지 스킨
                 case 2433981: // - 핑크빈 데미지 스킨
                 case 2433990: // - 돼지바 데미지 스킨
                 case 2434248: // - 무지개 봉봉 데미지 스킨
                 case 2434273: // - 밤하늘 데미지 스킨
                 case 2434274: // - 마시멜로 데미지 스킨
                 case 2434289: // - 무릉도장 데미지 스킨
                 case 2434390: // - 곰돌이 데미지 스킨
                 case 2434391: // - 파왕 데미지 스킨
                 case 2434528: // - USA 데미지 스킨
                 case 2434529: // - 츄러스 데미지 스킨
                 case 2434530: // - 싱가폴 야경 데미지 스킨
                 case 2434542: // - 노히메 데미지 스킨
                 case 2434546: // - 낙서쟁이 데니스 데미지 스킨
                 case 2434574: // - 만월 데미지 스킨
                 case 2434575 : //- 햇님이 반짝 데미지 스킨
                 case 2434654: // - 무르무르 데미지 스킨
                 case 2434655: // - 구미호 데미지 스킨
                 case 2434661: // - 좀비 데미지 스킨
                 case 2434710: // - MVP 스페셜 전용 데미지 스킨
                 case 2434734: // - 블랙헤븐 데미지 스킨
                 case 2434824: // - 몬스터파크 데미지 스킨
                 case 2434950: // - 젤리빈 데미지 스킨
                 case 2434951: // - 소프트 콘 데미지 스킨
                 case 2435005: // - 데미지 스킨 선택권
                 case 2435023: // - 메리 크리스마스 데미지 스킨
                 case 2435024: // - 색동 데미지 스킨
                 case 2435025: // - 예티X페페 데미지 스킨
                 case 2435026: // - 슬라임X주황버섯 데미지 스킨
                 case 2435027: // - 무지개 봉봉 데미지 스킨
                 case 2435028: // - 밤하늘 데미지 스킨
                 case 2435029: // - 마시멜로 데미지 스킨
                 case 2435030: // - 크리스마스 전구 데미지 스킨
                 case 2435043: // - 히어로즈 팬텀 데미지 스킨
                 case 2435044: // - 히어로즈 메르세데스 데미지 스킨
                 case 2435045: // - 달콤한 전통 한과 데미지 스킨
                 case 2435046: // - 폭죽 데미지 스킨
                 case 2435047: // - 하트풍선 데미지 스킨
                 case 2435140: // - 네온사인 데미지 스킨
                 case 2435141: // - 얼음땡 데미지 스킨
                 case 2435157: // - 붓글씨 데미지 스킨
                 case 2435158: // - 익스플로전 데미지 스킨
                 case 2435159: // - 스노윙 데미지 스킨
                 case 2435160: // - 미호 데미지 스킨
                 case 2435161: // - 도넛 데미지 스킨
                 case 2435162: // - 앤티크 골드 데미지 스킨
                 case 2435163: // - 랜덤 데미지 스킨 상자
                 case 2435166: // - 월묘 데미지 스킨
                 case 2435168: // - 커플부대 데미지 스킨
                 case 2435169: // - 할로윈 데미지 스킨
                 case 2435170: // - 주황버섯 데미지 스킨
                 case 2435171: // - 햇님이 반짝 데미지 스킨
                 case 2435172: // - 디지털라이즈 데미지 스킨
                 case 2435173: // - 눈꽃송이 데미지 스킨
                 case 2435174: // - 키보드 워리어 데미지 스킨
                 case 2435175: // - 레미너선스 데미지 스킨
                 case 2435176: // - 왕관 데미지 스킨
                 case 2435177: // - 모노톤 데미지 스킨
                 case 2435179: // - 캔디 데미지 스킨
                 case 2435182: // - 악보 데미지 스킨
                 case 2435184: // - 근성의 숲 데미지 스킨
                 case 2435222: // - 페스티벌 별주부 데미지 스킨
                 case 2435293: // - 만우절 데미지 스킨(오리지널)
                 case 2435313: // - 블랙데이 데미지 스킨
                 case 2435316: // - 헤이스트 데미지 스킨
                 case 2435325: // - 무르무르 데미지 스킨
                 case 2435326: // - 구미호 데미지 스킨
                 case 2435331: // - 만우절 데미지 스킨(둥근체)
                 case 2435332: // - 만우절 데미지 스킨(이펙트체)
                 case 2435333: // - 만우절 데미지 스킨(흑백 궁서체)
                 case 2435334: // - 만우절 데미지 스킨(컬러 궁서체)
                 case 2435408: // - 13주년 단풍잎 데미지 스킨
                 case 2435424: // - 헤네시스 데미지 스킨
                 case 2435425: // - 리프레 데미지 스킨
                 case 2435427: // - 전자 방식 데미지 스킨
                 case 2435428: // - 스페이스 데미지 스킨
                 case 2435429: // - 초코도넛 데미지 스킨
                 case 2435430: // - 푸른화염 데미지 스킨
                 case 2435431: // - 시크릿 수학 데미지 스킨
                 case 2435432: // - 퍼플 데미지 스킨
                 case 2435433: // - 나노픽셀 데미지 스킨
                 case 2435456: // - 러블리 데미지 스킨
                 case 2435461: // - 풍선 데미지 스킨
                 case 2435473: // - 만우절 데미지 스킨(둥근체)
                 case 2435474: // - 만우절 데미지 스킨(컬러 궁서체)
                 case 2435477: // - 무지개 봉봉 데미지 스킨
                 case 2435478: // - 살랑살랑 봄바람 데미지 스킨
                 case 2435490: // - 마시멜로 데미지 스킨
                 case 2435491: // - 밤하늘 데미지 스킨
                 case 2435493: // - 몬스터풍선 데미지 스킨
                 case 2435516: // - 투명 데미지 스킨
                 case 2435521: // - 수정 데미지 스킨
                 case 2435522: // - 크로우 데미지 스킨
                 case 2435523: // - 초콜릿 데미지 스킨
                 case 2435524: // - 스파크 데미지 스킨
                 case 2435538: // - 크라운 데미지 스킨
                 case 2435972: // - 소멸의 여로 데미지 스킨?
                 case 2436023: // - 츄츄 데미지 스킨?
                 case 2436024: // - 레헬른 데미지 스킨?
                 case 2436026: // - 포이즌 플레임 데미지 스킨?
                 case 2436027: // - 블루 스트라이크 데미지 스킨?
                 case 2436028: // - 뮤직파워 데미지 스킨?
                 case 2436029: // - 콜라쥬 데미지 스킨
                 case 2435832: // - 라운딩 데미지 스킨(Ver.1)?
                 case 2435833: // - V 네온 데미지 스킨?
                 case 2435839: // - 스페이드 데미지 스킨?
                 case 2435840: // - 홀리 데미지 스킨?
                 case 2435841: // - 데모닉 데미지 스킨?
                 case 2436045: // - 별빛 오로라 데미지 스킨?
                 case 2436083: // - 빛과 어둠 데미지 스킨
                 case 2436084: // - 퓨리 데미지 스킨
                 case 2436085: // - 알밤 데미지 스킨
                 case 2436098: // - 붓글씨 데미지 스킨
                 case 2436103: // - 문라이트 데미지 스킨
                 case 2436140: // - 은행나무 데미지 스킨
                 case 2436182: // - 고스트 데미지 스킨
                 case 2436206: // - 명탐정 데미지 스킨
                 case 2436212: // - 할로캣 데미지 스킨
                 case 2436215: // - 샤방샤방 데미지 스킨
                 case 2435905: // - [해외]냥냥이 데미지 스킨
                 case 2435906: // - [해외]블랙캣 데미지 스킨
                 case 2435948: // - [해외]할로윈 데미지 스킨
                 case 2435949: // - [해외]고스트 데미지 스킨
                 case 2435955: // - [해외]푸른정령 데미지 스킨
                 case 2435956: // - [해외]블루로즈 데미지 스킨
                 case 2436132: // - [해외]레인보우 데미지 스킨
                 case 2436258: // -? 유물 데미지 스킨 146
                 case 2436259: // - 상형 문자 데미지 스킨 147
                 case 2436268: // - 호빵 데미지 스킨 145
                 case 2436400: // - 한계돌파 데미지 스킨 148
                 case 2436437: // - 보안관 데미지 스킨 149
                 case 2436521: // - 눈송송 데미지 스킨 150
                 case 2436522: // - 여신의 흔적 데미지 스킨 154
                 case 2436528: // - 하트뿅뿅 데미지 스킨 152
                 case 2436529: // - 양 데미지 스킨 153
                 case 2436553: // - 전설의 데미지 스킨 155
                 case 2436560: // - 메카 데미지 스킨 156

                 case 2436578: // - 라떼 데미지 스킨 157
                 case 2436596: // - 마스터 아이스 데미지 스킨 160
                 case 2436611: // - 까치 깃털 데미지 스킨 158
                 case 2436612: // - 감나무 데미지 스킨 159
                 case 2436679: // - 아르카나 데미지 스킨 161
                 case 2436680: // - 엠퍼러스 데미지 스킨 162
                 case 2436681: // - 파프니르 데미지 스킨 163
                 case 2436682: // - 앱솔랩스 데미지 스킨 164
                 case 2436683: // - 병아리 데미지 스킨 165
                 case 2436684: // - XOXO 데미지 스킨 166
                 case 2436785: // - 꿀꿀비 데미지 스킨
                 case 2436810: // - 이볼빙 데미지 스킨
                 case 2436951: // - 별자리 데미지 스킨
                 case 2436952: // - 외계인 데미지 스킨
                 case 2436953: // - 아이스크림 데미지 스킨
                 case 2437009: // - 파티 퀘스트 데미지 스킨
                 case 2437022: // - 솔루나 데미지 스킨
                 case 2437023: // - 일루미네이션 데미지 스킨
                 case 2437024: // - 매지컬스타 데미지 스킨
                 case 2437164: // - 카데나 데미지 스킨
                 case 2437238: // - 블랙로즈 데미지 스킨
                 case 2437239: // - 체스 데미지 스킨
                 case 2437243: // - 퍼즐 데미지 스킨
                 case 2437482: // - 일리움 데미지 스킨
                 case 2437495: // - 먹구름 데미지 스킨
                 case 2437496: // - 소나기 데미지 스킨
                 case 2437498: // - 돈많이 데미지 스킨
                 case 2437515: // - 야자수 데미지 스킨
                 case 2437691: // - 라이트닝 데미지 스킨
                 case 2437877: // - 마스터 블러드 데미지 스킨
                 case 2438143: // - 시간의 초월자 데미지 스킨
                 case 2438144: // - 슈퍼스타★ 데미지 스킨
                 case 2438352: // - ARK 데미지 스킨
                 case 2438378: // - 왈왈 데미지 스킨
                 case 2438379: // - 설렘 하트 데미지 스킨
                 case 2438413: // - 디스커버리 데미지 스킨
                 case 2438415: // - 에스페라 데미지 스킨
                 case 2438417: // - 천공의 데미지 스킨
                 case 2438419: // - 하이브리드 데미지 스킨
                 case 2438460: // - 레드 써킷 데미지 스킨
                 {
                 if (!GameConstants.isZero(chr.getJob())) {
                 MapleQuest quest = MapleQuest.getInstance(7291);
                 MapleQuestStatus queststatus = new MapleQuestStatus(quest, (byte) 1);
                 int skinnum = GameConstants.getDamageSkinNumberByItem(toUse.getItemId());
                 String skinString = String.valueOf(skinnum);
                 queststatus.setCustomData(skinString == null ? "0" : skinString);
                 chr.updateQuest(queststatus, true);
                 chr.setKeyValue(7293, "damage_skin", String.valueOf(toUse.getItemId()));
                 chr.dropMessage(5, "데미지 스킨이 변경되었습니다.");
                 chr.getMap().broadcastMessage(chr, CField.showForeignDamageSkin(chr, skinnum), false);
                 MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                 } else {
                 chr.dropMessage(5, "제로 직업군은 데미지 스킨을 사용해도 아무 효과도 얻을 수 없 다.");
                 }
                 break;
                 }
                 */
                case 2436784:
                case 2439631:
                case 2435513:
                case 2435122:
                    if (!GameConstants.isZero(chr.getJob())) {
                        NPCScriptManager.getInstance().startItem(c, 9010000, "consume_" + toUse.getItemId());
                    } else {
                        chr.dropMessage(5, "제로 직업군은 데미지 스킨을 사용해도 아무 효과도 얻을 수 없다.");
                    }
                    break;
                case 2432636: //데미지 저장 스크롤
                    if (!GameConstants.isZero(chr.getJob())) {
                        NPCScriptManager.getInstance().startItem(c, 9010000, "consume_2411020");
                    } else {
                        chr.dropMessage(5, "제로 직업군은 데미지 스킨을 사용해도 아무 효과도 얻을 수 없다.");
                    }
                    break;
                case 2430469: // 정령의 펜던트 7
                    chr.gainItem(1122017, (short) 1, false, System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000, "정령의 펜던트");
                    chr.removeItem(toUse.getItemId(), -1);
                    break;
                case 2434021:
                    chr.gainHonor(10000);
                    chr.removeItem(toUse.getItemId(), -1);
                    break;
                default:
                    NPCScriptManager.getInstance().startItem(c, 9010060, "consume_" + toUse.getItemId());
                    break;
            }
            if (GameConstants.getDSkinNum(toUse.getItemId()) != -1) {
                if (!GameConstants.isZero(chr.getJob())) {
                    MapleQuest quest = MapleQuest.getInstance(7291);
                    MapleQuestStatus queststatus = new MapleQuestStatus(quest, (byte) 1);
                    int skinnum = GameConstants.getDSkinNum(toUse.getItemId());
                    String skinString = String.valueOf(skinnum);
                    queststatus.setCustomData(skinString == null ? "0" : skinString);
                    chr.updateQuest(queststatus, true);
                    chr.setKeyValue(7293, "damage_skin", String.valueOf(toUse.getItemId()));
                    chr.dropMessage(5, "데미지 스킨이 변경되었습니다.");
                    chr.getMap().broadcastMessage(chr, CField.showForeignDamageSkin(chr, skinnum), false);
                    chr.updateDamageSkin();
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                } else {
                    chr.dropMessage(5, "제로 직업군은 데미지 스킨을 사용해도 아무 효과도 얻을 수 없다.");
                }
            }

            if (GameConstants.getRidingNum(toUse.getItemId()) != -1) {
                int skinnum = GameConstants.getRidingNum(toUse.getItemId());
                //chr.setKeyValue(7293, "damage_skin", String.valueOf(toUse.getItemId()));
                chr.changeSkillLevel(skinnum, (byte) 1, (byte) 1);
                //getRidingItemIdbyNum
                chr.dropMessage(5, MapleItemInformationProvider.getInstance().getName(GameConstants.getRidingItemIdbyNum(skinnum)) + "(이)가 등록 되었습니다.");
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
            }
        }
        if (mountid > 0) {
            mountid = c.getPlayer().getStat().getSkillByJob(mountid, c.getPlayer().getJob());
            final int fk = GameConstants.getMountItem(mountid, c.getPlayer());
            if (fk > 0 && mountid < 80001000) { //TODO JUMP
                for (int i = 80001001; i < 80001999; i++) {
                    final Skill skill = SkillFactory.getSkill(i);
                    if (skill != null && GameConstants.getMountItem(skill.getId(), c.getPlayer()) == fk) {
                        mountid = i;
                        break;
                    }
                }
            }
            if (c.getPlayer().getSkillLevel(mountid) > 0) {
                c.getPlayer().dropMessage(5, "이미 해당 라이딩스킬이 있습니다.");
            } else if (SkillFactory.getSkill(mountid) == null) {
                c.getPlayer().dropMessage(5, "해당스킬은 얻으실 수 없습니다.");
            } else if (expiration_days > 0) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(mountid), (byte) 1, (byte) 1, System.currentTimeMillis() + (long) (expiration_days * 24 * 60 * 60 * 1000));
                c.getPlayer().dropMessage(-1, "[" + SkillFactory.getSkillName(mountid) + "] 스킬을 얻었습니다.");
            } else if (expiration_days == 0) { //영구 사용
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (byte) 1, false);
                c.getPlayer().changeSkillLevel(SkillFactory.getSkill(mountid), (byte) 1, (byte) 1);
                c.getPlayer().dropMessage(-1, "[" + SkillFactory.getSkillName(mountid) + "] 스킬을 얻었습니다.");
            }
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void UseSummonBag(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.inPVP()) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        slea.readInt();
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse != null && toUse.getQuantity() >= 1 && toUse.getItemId() == itemId && (c.getPlayer().getMapId() < 910000000 || c.getPlayer().getMapId() > 910000022)) {
            final Map<String, Integer> toSpawn = MapleItemInformationProvider.getInstance().getEquipStats(itemId);

            if (toSpawn == null) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            MapleMonster ht = null;
            int type = 0;
            for (Entry<String, Integer> i : toSpawn.entrySet()) {
                if (i.getKey().startsWith("mob") && Randomizer.nextInt(99) <= i.getValue()) {
                    ht = MapleLifeFactory.getMonster(Integer.parseInt(i.getKey().substring(3)));
                    chr.getMap().spawnMonster_sSack(ht, chr.getPosition(), type);
                }
            }
            if (ht == null) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void UseCashItem(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null || c.getPlayer().inPVP()) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        slea.readInt();
        c.getPlayer().setScrolledPosition((short) 0);
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        boolean used = false, cc = false;

        switch (itemId) {
            case 5043001: // NPC Teleport Rock
            case 5043000: { // NPC Teleport Rock
                final short questid = slea.readShort();
                final int npcid = slea.readInt();
                final MapleQuest quest = MapleQuest.getInstance(questid);

                if (c.getPlayer().getQuest(quest).getStatus() == 1 && quest.canComplete(c.getPlayer(), npcid)) {
                    final int mapId = MapleLifeFactory.getNPCLocation(npcid);
                    if (mapId != -1) {
                        final MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
                        if (map.containsNPC(npcid) && !FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(map.getFieldLimit())) {
                            c.getPlayer().changeMap(map, map.getPortal(0));
                        }
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "Unknown error has occurred.");
                    }
                }
                break;
            }
            case 5041001:
            case 5040004:
            case 5040003:
            case 5040002:
            case 2320000: // The Teleport Rock
            case 5041000: // VIP Teleport Rock
            case 5040000: // The Teleport Rock
            case 5040001: { // Teleport Coke
                used = UseTeleRock(slea, c, itemId);
                break;
            }
            case 5450005: {
                c.getPlayer().setConversation(4);
                c.getPlayer().getStorage().sendStorage(c, 1022005);
                break;
            }
            case 5050000: { // AP Reset
                Map<MapleStat, Long> statupdate = new EnumMap<MapleStat, Long>(MapleStat.class);
                final int apto = slea.readInt();
                final int apfrom = slea.readInt();

                if (apto == apfrom) {
                    break; // Hack
                }
                final int job = c.getPlayer().getJob();
                final PlayerStats playerst = c.getPlayer().getStat();
                used = true;

                switch (apto) { // AP to
                    case 64: // str
                        if (playerst.getStr() >= 999) {
                            used = false;
                        }
                        break;
                    case 128: // dex
                        if (playerst.getDex() >= 999) {
                            used = false;
                        }
                        break;
                    case 256: // int
                        if (playerst.getInt() >= 999) {
                            used = false;
                        }
                        break;
                    case 512: // luk
                        if (playerst.getLuk() >= 999) {
                            used = false;
                        }
                        break;
                    case 2048: // hp
                        if (playerst.getMaxHp() >= 500000) {
                            used = false;
                        }
                        break;
                    case 8192: // mp
                        if (playerst.getMaxMp() >= 500000) {
                            used = false;
                        }
                        break;
                }
                switch (apfrom) { // AP to
                    case 64: // str
                        if (playerst.getStr() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 1 && playerst.getStr() <= 35)) {
                            used = false;
                        }
                        break;
                    case 128: // dex
                        if (playerst.getDex() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 3 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 4 && playerst.getDex() <= 25) || (c.getPlayer().getJob() % 1000 / 100 == 5 && playerst.getDex() <= 20)) {
                            used = false;
                        }
                        break;
                    case 256: // int
                        if (playerst.getInt() <= 4 || (c.getPlayer().getJob() % 1000 / 100 == 2 && playerst.getInt() <= 20)) {
                            used = false;
                        }
                        break;
                    case 512: // luk
                        if (playerst.getLuk() <= 4) {
                            used = false;
                        }
                        break;
                    case 2048: // hp
                        if (/*playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) + 134) || */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                            used = false;
                            c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                        }
                        break;
                    case 8192: // mp
                        if (/*playerst.getMaxMp() < ((c.getPlayer().getLevel() * 14) + 134) || */c.getPlayer().getHpApUsed() <= 0 || c.getPlayer().getHpApUsed() >= 10000) {
                            used = false;
                            c.getPlayer().dropMessage(1, "You need points in HP or MP in order to take points out.");
                        }
                        break;
                }
                if (used) {
                    switch (apto) { // AP to
                        case 64: { // str
                            final int toSet = playerst.getStr() + 1;
                            playerst.setStr((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.STR, (long) toSet);
                            break;
                        }
                        case 128: { // dex
                            final int toSet = playerst.getDex() + 1;
                            playerst.setDex((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.DEX, (long) toSet);
                            break;
                        }
                        case 256: { // int
                            final int toSet = playerst.getInt() + 1;
                            playerst.setInt((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.INT, (long) toSet);
                            break;
                        }
                        case 512: { // luk
                            final int toSet = playerst.getLuk() + 1;
                            playerst.setLuk((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.LUK, (long) toSet);
                            break;
                        }
                        case 2048: // hp
                            long maxhp = playerst.getMaxHp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxhp += Randomizer.rand(4, 8);
                            } else if ((job >= 100 && job <= 132) || (job >= 3200 && job <= 3212) || (job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112)) { // Warrior
                                maxhp += Randomizer.rand(36, 42);
                            } else if ((job >= 200 && job <= 232) || (GameConstants.isEvan(job)) || (job >= 1200 && job <= 1212)) { // Magician
                                maxhp += Randomizer.rand(10, 12);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman
                                maxhp += Randomizer.rand(14, 18);
                            } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) {
                                maxhp += Randomizer.rand(24, 28);
                            } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
                                maxhp += Randomizer.rand(16, 20);
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxhp += Randomizer.rand(34, 38);
                            } else { // GameMaster
                                maxhp += Randomizer.rand(50, 100);
                            }
                            maxhp = Math.min(500000, Math.abs(maxhp));
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                            playerst.setMaxHp(maxhp, c.getPlayer());
                            statupdate.put(MapleStat.MAXHP, maxhp);
                            break;

                        case 8192: // mp
                            long maxmp = playerst.getMaxMp();

                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxmp += Randomizer.rand(6, 8);
                            } else if (job >= 3100 && job <= 3112) {
                                break;
                            } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112) || (job >= 2000 && job <= 2112)) { // Warrior
                                maxmp += Randomizer.rand(4, 9);
                            } else if ((job >= 200 && job <= 232) || (GameConstants.isEvan(job)) || (job >= 3200 && job <= 3212) || (job >= 1200 && job <= 1212)) { // Magician
                                maxmp += Randomizer.rand(32, 36);
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 500 && job <= 532) || (job >= 3200 && job <= 3212) || (job >= 3500 && job <= 3512) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 2300 && job <= 2312)) { // Bowman
                                maxmp += Randomizer.rand(8, 10);
                            } else { // GameMaster
                                maxmp += Randomizer.rand(50, 100);
                            }
                            maxmp = Math.min(500000, Math.abs(maxmp));
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() + 1));
                            playerst.setMaxMp(maxmp, c.getPlayer());
                            statupdate.put(MapleStat.MAXMP, maxmp);
                            break;
                    }
                    switch (apfrom) { // AP from
                        case 64: { // str
                            final int toSet = playerst.getStr() - 1;
                            playerst.setStr((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.STR, (long) toSet);
                            break;
                        }
                        case 128: { // dex
                            final int toSet = playerst.getDex() - 1;
                            playerst.setDex((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.DEX, (long) toSet);
                            break;
                        }
                        case 256: { // int
                            final int toSet = playerst.getInt() - 1;
                            playerst.setInt((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.INT, (long) toSet);
                            break;
                        }
                        case 512: { // luk
                            final int toSet = playerst.getLuk() - 1;
                            playerst.setLuk((short) toSet, c.getPlayer());
                            statupdate.put(MapleStat.LUK, (long) toSet);
                            break;
                        }
                        case 2048: // HP
                            long maxhp = playerst.getMaxHp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxhp -= 12;
                            } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                maxhp -= 10;
                            } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312)) { // Bowman, Thief
                                maxhp -= 15;
                            } else if ((job >= 500 && job <= 532) || (job >= 1500 && job <= 1512)) { // Pirate
                                maxhp -= 22;
                            } else if (((job >= 100 && job <= 132) || job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112)) { // Soul Master
                                maxhp -= 32;
                            } else if ((job >= 2000 && job <= 2112) || (job >= 3200 && job <= 3212)) { // Aran
                                maxhp -= 40;
                            } else { // GameMaster
                                maxhp -= 20;
                            }
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                            playerst.setMaxHp(maxhp, c.getPlayer());
                            statupdate.put(MapleStat.MAXHP, maxhp);
                            break;
                        case 8192: // MP
                            long maxmp = playerst.getMaxMp();
                            if (GameConstants.isBeginnerJob(job)) { // Beginner
                                maxmp -= 8;
                            } else if (job >= 3100 && job <= 3112) {
                                break;
                            } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112)) { // Warrior
                                maxmp -= 4;
                            } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212)) { // Magician
                                maxmp -= 30;
                            } else if ((job >= 500 && job <= 532) || (job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312)) { // Pirate, Bowman. Thief
                                maxmp -= 10;
                            } else if (job >= 2000 && job <= 2112) { // Aran
                                maxmp -= 5;
                            } else { // GameMaster
                                maxmp -= 20;
                            }
                            c.getPlayer().setHpApUsed((short) (c.getPlayer().getHpApUsed() - 1));
                            playerst.setMaxMp(maxmp, c.getPlayer());
                            statupdate.put(MapleStat.MAXMP, maxmp);
                            break;
                    }
                    c.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statupdate, false, c.getPlayer()));
                }
                break;
            }
            case 5050001: // SP Reset (1st job)
            case 5050002: // SP Reset (2nd job)
            case 5050003: // SP Reset (3rd job)
            case 5050004:  // SP Reset (4th job)
            case 5050005: //evan sp resets
            case 5050006:
            case 5050007:
            case 5050008:
            case 5050009: {
                if (itemId >= 5050005 && !GameConstants.isEvan(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(1, "This reset is only for Evans.");
                    break;
                } //well i dont really care other than this o.o
                if (itemId < 5050005 && GameConstants.isEvan(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(1, "This reset is only for non-Evans.");
                    break;
                } //well i dont really care other than this o.o
                int skill1 = slea.readInt();
                int skill2 = slea.readInt();
                for (int i : GameConstants.blockedSkills) {
                    if (skill1 == i) {
                        c.getPlayer().dropMessage(1, "You may not add this skill.");
                        return;
                    }
                }

                Skill skillSPTo = SkillFactory.getSkill(skill1);
                Skill skillSPFrom = SkillFactory.getSkill(skill2);

                if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                    c.getPlayer().dropMessage(1, "You may not add beginner skills.");
                    break;
                }
                if (GameConstants.getSkillBookForSkill(skill1) != GameConstants.getSkillBookForSkill(skill2)) { //resistance evan
                    c.getPlayer().dropMessage(1, "You may not add different job skills.");
                    break;
                }
                //if (GameConstants.getJobNumber(skill1) > GameConstants.getJobNumber(skill2)) { //putting 3rd job skillpoints into 4th job for example
                //    c.getPlayer().dropMessage(1, "You may not add skillpoints to a higher job.");
                //    break;
                //}
                if ((c.getPlayer().getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && c.getPlayer().getSkillLevel(skillSPFrom) > 0 && skillSPTo.canBeLearnedBy(c.getPlayer())) {
                    if (skillSPTo.isFourthJob() && (c.getPlayer().getSkillLevel(skillSPTo) + 1 > c.getPlayer().getMasterLevel(skillSPTo))) {
                        c.getPlayer().dropMessage(1, "You will exceed the master level.");
                        break;
                    }
                    if (itemId >= 5050005) {
                        if (GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 && GameConstants.getSkillBookForSkill(skill1) != (itemId - 5050005) * 2 + 1) {
                            c.getPlayer().dropMessage(1, "You may not add this job SP using this reset.");
                            break;
                        }
                    } else {
                        int theJob = GameConstants.getJobNumber(skill2);
                        switch (skill2 / 10000) {
                            case 430:
                                theJob = 1;
                                break;
                            case 432:
                            case 431:
                                theJob = 2;
                                break;
                            case 433:
                                theJob = 3;
                                break;
                            case 434:
                                theJob = 4;
                                break;
                        }
                        if (theJob != itemId - 5050000) { //you may only subtract from the skill if the ID matches Sp reset
                            c.getPlayer().dropMessage(1, "You may not subtract from this skill. Use the appropriate SP reset.");
                            break;
                        }
                    }
                    final Map<Skill, SkillEntry> sa = new HashMap<>();
                    sa.put(skillSPFrom, new SkillEntry((byte) (c.getPlayer().getSkillLevel(skillSPFrom) - 1), c.getPlayer().getMasterLevel(skillSPFrom), SkillFactory.getDefaultSExpiry(skillSPFrom)));
                    sa.put(skillSPTo, new SkillEntry((byte) (c.getPlayer().getSkillLevel(skillSPTo) + 1), c.getPlayer().getMasterLevel(skillSPTo), SkillFactory.getDefaultSExpiry(skillSPTo)));
                    c.getPlayer().changeSkillsLevel(sa);
                    used = true;
                }
                break;
            }
            case 5062800:
            case 5062801: {
                List<InnerSkillValueHolder> newValues = new LinkedList<InnerSkillValueHolder>();
                InnerSkillValueHolder ivholder = null;
                InnerSkillValueHolder ivholder2 = null;
                for (InnerSkillValueHolder isvh : c.getPlayer().getInnerSkills()) {
                    if (ivholder == null) { //1번째
                        int nowrank = -1;
                        int rand = Randomizer.nextInt(100);
                        if (isvh.getRank() == 3) {
                            nowrank = 3;
                        } else if (isvh.getRank() == 2) {
                            if (rand < 5) {
                                nowrank = 3; // 상승
                            } else {
                                nowrank = 2; // 유지
                            }
                        } else if (isvh.getRank() == 1) {
                            if (rand < 10) {
                                nowrank = 2; // 상승
                            } else {
                                nowrank = 1; // 유지
                            }
                        } else if (rand < 40) {
                            nowrank = 1; // 상승
                        } else {
                            nowrank = 0; // 유지
                        }
                        ivholder = InnerAbillity.getInstance().renewSkill(nowrank, true);
                        while (isvh.getSkillId() == ivholder.getSkillId()) {
                            ivholder = InnerAbillity.getInstance().renewSkill(nowrank, true);
                        }
                        newValues.add(ivholder);
                    } else if (ivholder2 == null) {
                        ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), true);
                        while (isvh.getSkillId() == ivholder2.getSkillId() || ivholder.getSkillId() == ivholder2.getSkillId()) {
                            ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), true);
                        }
                        newValues.add(ivholder2);
                    } else {
                        InnerSkillValueHolder ivholder3 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), true);
                        while (isvh.getSkillId() == ivholder3.getSkillId() || ivholder.getSkillId() == ivholder3.getSkillId() || ivholder2.getSkillId() == ivholder3.getSkillId()) {
                            ivholder3 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), true);
                        }
                        newValues.add(ivholder3);
                    }
                }
                c.getPlayer().innerCirculator = newValues;
                c.getSession().writeAndFlush(CWvsContext.MiracleCirculator(newValues, itemId));
                used = true;
                break;
            }
            case 5060048: { // 애플
                final List<Pair<Integer, Short>> nitemlist = SpecialItemConstants.GoldAppleNormalList;
                final List<Pair<Integer, Short>> sitemlist = SpecialItemConstants.GoldAppleSpecialList;
                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() > 0) {
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2) {
                        if (c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() > 0) {
                            if (c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() > 0) {
                                boolean fromSpecial = Randomizer.isSuccess(1);
                                int i;
                                if (fromSpecial) {
                                    i = Randomizer.nextInt(sitemlist.size());
                                } else {
                                    i = Randomizer.nextInt(nitemlist.size());
                                }

                                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                Item item;

                                if (!fromSpecial) {
                                    if (GameConstants.getInventoryType(nitemlist.get(i).getLeft()) == MapleInventoryType.EQUIP) {
                                        item = (Equip) ii.getEquipById(nitemlist.get(i).getLeft());
                                    } else {
                                        item = new client.inventory.Item(nitemlist.get(i).getLeft(), (byte) 0, (short) nitemlist.get(i).getRight(), (byte) ItemFlag.KARMA_EQUIP.getValue());
                                    }
                                } else {
                                    if (GameConstants.getInventoryType(sitemlist.get(i).getLeft()) == MapleInventoryType.EQUIP) {
                                        item = (Equip) ii.getEquipById(sitemlist.get(i).getLeft());
                                    } else {
                                        item = new client.inventory.Item(sitemlist.get(i).getLeft(), (byte) 0, (short) sitemlist.get(i).getRight(), (byte) ItemFlag.KARMA_EQUIP.getValue());
                                    }
                                }

                                if (MapleItemInformationProvider.getInstance().isCash(item.getItemId())) {
                                    item.setUniqueId(MapleInventoryIdentifier.getInstance());
                                }

                                if (GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP && !ItemFlag.KARMA_EQUIP.check(item.getFlag())) {
                                    item.setFlag(item.getFlag() + ItemFlag.KARMA_EQUIP.getValue());
                                }

                                if (fromSpecial) { // isSpecial
                                    World.Broadcast.broadcastSmega(CWvsContext.serverMessage(11, c.getChannel(), "", c.getPlayer().getName() + "님이 골드애플에서 {" + ii.getName(item.getItemId()) + "}을 얻었습니다.", true, item));
                                }

                                MapleInventoryManipulator.addbyItem(c, item);

                                c.getPlayer().gainItem(2435458, 1);

                                c.getSession().writeAndFlush(CWvsContext.goldApple(item, toUse));

                                used = true;
                            }
                        }
                    }
                }

                if (!used) {
                    c.getSession().writeAndFlush(CWvsContext.goldApple(null, toUse));
                }
                break;
            }

            case 5155000: // 카르타의 진주
            case 5155005:
            case 5155004: {
                int type = slea.readInt();
                String effect = "";
                switch (type) {
                    case 0:
                        if (GameConstants.isMercedes(c.getPlayer().getJob())) {
                            type = 1;
                        }
                        if (GameConstants.isIllium(c.getPlayer().getJob())) {
                            type = 2;
                        }
                        if (GameConstants.isAdel(c.getPlayer().getJob()) || GameConstants.isArk(c.getPlayer().getJob())) {
                            type = 3;
                        }
                        effect = "Effect/BasicEff.img/JobChanged";
                        break;
                    case 1:
                        if (GameConstants.isMercedes(c.getPlayer().getJob())) {
                            type = 0;
                        }
                        effect = "Effect/BasicEff.img/JobChangedElf";
                        break;
                    case 2:
                        if (GameConstants.isIllium(c.getPlayer().getJob())) {
                            type = 0;
                        }
                        effect = "Effect/BasicEff.img/JobChangedIlliumFront";
                        break;
                    case 3:
                        if (GameConstants.isAdel(c.getPlayer().getJob()) || GameConstants.isArk(c.getPlayer().getJob())) {
                            type = 0;
                        }
                        effect = "Effect/BasicEff.img/JobChangedArkFront";
                        break;
                }
                c.getPlayer().setKeyValue(7784, "sw", String.valueOf(type));
                c.getSession().writeAndFlush(EffectPacket.showEffect(c.getPlayer(), 0, itemId, 38, 2, 0, (byte) 0, true, null, effect, null));
                used = true;
                break;
            }
            case 5155001: { // 드래곤 테일 쉬프트
                if (GameConstants.isKaiser(c.getPlayer().getJob())) {
                    if (c.getPlayer().getKeyValue(7786, "sw") == 0) {
                        c.getPlayer().setKeyValue(7786, "sw", "1");
                    } else {
                        c.getPlayer().setKeyValue(7786, "sw", "0");
                    }
                    c.getPlayer().dropMessage(6, "드래곤 테일 쉬프트의 신비로운 힘으로 모습이 바뀌었습니다.");
                    used = true;
                } else {
                    c.getPlayer().dropMessage(6, "드래곤 테일 쉬프트는 카이저에게만 효과가 있는것 같다.");
                }
                break;
            }
            case 5500000: { // Magic Hourglass 1 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 1;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "It may not be used on this item.");
                    }
                }
                break;
            }
            case 5500001: { // Magic Hourglass 7 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 7;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "It may not be used on this item.");
                    }
                }
                break;
            }
            case 5500002: { // Magic Hourglass 20 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 20;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "It may not be used on this item.");
                    }
                }
                break;
            }
            case 5500005: { // Magic Hourglass 50 day
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 50;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "It may not be used on this item.");
                    }
                }
                break;
            }
            case 5044000:
            case 5044001:
            case 5044002:
            case 5044006:
            case 5044007: { // 텔레포트 월드맵
                slea.readByte();
                /*   if (c.getPlayer().getKeyValue(8090, "cash_" + toUse.getItemId() + "_lastUsed") == -1) {
                 c.getPlayer().setKeyValue(8090, "cash_" + toUse.getItemId() + "_lastUsed", "0");
                 }
                 long lastused = c.getPlayer().getKeyValue(8090, "cash_" + toUse.getItemId() + "_lastUsed");
                 long time = 180000;
                 if (lastused + time > System.currentTimeMillis()) {
                 long lefttime = lastused + time - System.currentTimeMillis();
                 lefttime /= 1000;
                 int min = (int) (lefttime / 60);
                 int sec = (int) (lefttime % 60);
                 String msg = min + "분 " + sec + "초 후 아이템을 사용할 수 있습니다.";
                 c.getPlayer().dropMessage(1, "아이템을 아직 사용할 수 없습니다.\r\n\r\n같은 아이템은 하나의 아이템으로 인식됩니다.\r\n\r\n" + msg);
                 break;
                 }
                 c.getSession().writeAndFlush(CField.itemCooldown(itemId, toUse.getUniqueId()));
                 c.getPlayer().setKeyValue(8090, "cash_" + toUse.getItemId() + "_lastUsed", System.currentTimeMillis() + "");*/
                int mapid = slea.readInt();
                if (mapid == 180000000 || mapid == 261020700) {
                    c.getPlayer().dropMessage(1, "그곳으로 이동하실 수 없습니다.");
                    c.getPlayer().ban("텔레포트월드맵 악용", true, true, true);
                    return;
                }
                MapleMap target = c.getChannelServer().getMapFactory().getMap(mapid);
                c.getPlayer().changeMap(target, target.getPortal(0));
                break;
            }
            case 5500006: { // 마법의 모래시계 [99일]
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final int days = 99;
                if (item != null && !GameConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1 || item.getOwner().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "It may not be used on this item.");
                    }
                }
                break;
            }
            case 5064200: {
                Equip toScroll;
                slea.skip(4);
                short dst = slea.readShort();
                if (dst < 0) {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                } else {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                }
                if (toScroll != null) {
                    Equip origin = (Equip) MapleItemInformationProvider.getInstance().getEquipById(toScroll.getItemId());
                    toScroll.setAcc(origin.getAcc());
                    toScroll.setAvoid(origin.getAvoid());
                    toScroll.setDex(origin.getDex());
                    toScroll.setHands(origin.getHands());
                    toScroll.setHp(origin.getHp());
                    toScroll.setInt(origin.getInt());
                    toScroll.setJump(origin.getJump());
                    toScroll.setLevel(origin.getLevel());
                    toScroll.setLuk(origin.getLuk());
                    toScroll.setMatk(origin.getMatk());
                    toScroll.setMdef(origin.getMdef());
                    toScroll.setMp(origin.getMp());
                    toScroll.setSpeed(origin.getSpeed());
                    toScroll.setStr(origin.getStr());
                    toScroll.setUpgradeSlots(origin.getUpgradeSlots());
                    toScroll.setWatk(origin.getWatk());
                    toScroll.setWdef(origin.getWdef());
                    toScroll.setEnhance((byte) 0);
                    toScroll.setViciousHammer((byte) 0);
                    c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, toScroll));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, cc, toUse.getItemId(), toScroll.getItemId()), true);
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "널 오류가 발생했습니다. 오류게시판에 어떤아이템을 사용하셨는지 자세히 설명해주세요.");
                }
                break;
            }
            case 5060000: { // 이름 새기기
                final Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                if (item != null && item.getOwner().equals("")) {
                    boolean change = true;
                    for (String z : GameConstants.RESERVED) {
                        if (c.getPlayer().getName().indexOf(z) != -1) {
                            change = false;
                        }
                    }
                    if (change) {
                        item.setOwner(c.getPlayer().getName());
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIPPED);
                        used = true;
                    }
                }
                break;
            }
            case 5062500: { // 에디셔널 큐브
                boolean up = false;
                int pos = slea.readInt();
                Item item = c.getPlayer().getInventory(pos < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) pos);
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    Equip equip = (Equip) item;
                    if (equip.getPotential4() <= 0) {
                        c.getPlayer().dropMessage(1, "에디셔널 잠재능력이 부여되지 않았습니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {

                        int level = 0;
                        level = equip.getPotential4() >= 10000 ? (equip.getPotential4() / 10000) : (equip.getPotential4() / 100);
                        if (level >= 4) {
                            level = 4;
                        }
                        int rate = level == 3 ? 3 : level == 2 ? 5 : level == 1 ? 10 : 0;
                        if (Randomizer.nextInt(100) < rate) {
                            up = true;
                            level++;
                        }
                        int temp = level;
                        int a = 0;
                        while (temp > 1) {
                            if (temp > 1) {
                                temp--;
                                a++;
                            }
                        }
                        if (equip.getPotential6() > 0) {
                            equip.setPotential4(potential(equip.getItemId(), level, true));
                            equip.setPotential5(potential(equip.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1), true));
                            equip.setPotential6(potential(equip.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1), true));
                        } else {
                            equip.setPotential4(potential(equip.getItemId(), level, true));
                            equip.setPotential5(potential(equip.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1), true));
                        }

                        if (GameConstants.isZeroWeapon(equip.getItemId())) {
                            Equip zeroequip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                            zeroequip.setPotential4(equip.getPotential4());
                            zeroequip.setPotential5(equip.getPotential5());
                            zeroequip.setPotential6(equip.getPotential6());
                            if (zeroequip != null) {
                                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, zeroequip));
                            }
                        }

                        c.getSession().writeAndFlush(CField.getEditionalCubeStart(c.getPlayer(), item, up, itemId, c.getPlayer().itemQuantity(toUse.getItemId()) - 1));
                        c.getSession().writeAndFlush(CField.showPotentialReset(c.getPlayer().getId(), true, itemId, equip.getItemId()));
                        c.getPlayer().forceReAddItem(item, pos < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP);
                        MapleInventoryManipulator.addById(c, 2430915, (short) 1, null, null, 0, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                        used = true;
                        c.getPlayer().gainMeso(-GameConstants.getCubeMeso(equip.getItemId()), false);

                    } else {
                        c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재능력 재설정을 실패하였습니다.");
                    }
                }
                break;
            }
            case 5062503: {
                int pos = slea.readInt();
                Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) pos);
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    Equip equip = (Equip) item;
                    Equip neq = (Equip) equip.copy();

                    if (equip.getPotential4() <= 0) {
                        c.getPlayer().dropMessage(1, "에디셔널 잠재능력이 부여되지 않았습니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                    if (c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        if (GameConstants.isZero(c.getPlayer().getJob())) {
                            Item item2 = c.getPlayer().getInventory(pos < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) -11);
                            Equip eq2 = (Equip) item2;
                            if (eq2 != null) {
                                eq2.setState((byte) (equip.getState() + 32));
                                c.getSession().writeAndFlush(InventoryPacket.scrolledItem(toUse, item2, false, true, true));
                                c.getPlayer().forceReAddItem_NoUpdate(item2, MapleInventoryType.EQUIPPED);
                            }

                        }
                        int level = equip.getPotential4() >= 10000 ? (equip.getPotential4() / 10000)
                                : (equip.getPotential4() / 100);
                        if (level >= 4) {
                            level = 4;
                        }
                        int poten = potential(equip.getItemId(), level, true);
                        neq.setPotential4(poten);
                        neq.setPotential5(poten);
                        neq.setPotential6(poten);

                        c.getPlayer().choicepotential = neq;
                        c.getSession().writeAndFlush(CField.getWhiteCubeStart(c.getPlayer(), neq, 5062503, toUse.getQuantity(), toUse.getPosition()));
                        c.getSession().writeAndFlush(
                                CField.showPotentialReset(c.getPlayer().getId(), true, itemId, equip.getItemId()));
                        used = true;

                    } else {
                        c.getPlayer().dropMessage(5, "소비 아이템 여유 공간이 부족하여 잠재능력 재설정을 실패하였습니다.");
                    }
                }
                break;
            }
            case 5062005: { //어메이징 미라클 큐브
                Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slea.readInt());
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    Equip eq = (Equip) item;
                    if (GameConstants.isZero(c.getPlayer().getJob())) {
                        Item item2 = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) -11);
                        Equip eq2 = (Equip) item2;
                        eq2.renewPotential();
                        c.getSession().writeAndFlush(InventoryPacket.scrolledItem(toUse, item2, false, true, true));
                        c.getPlayer().forceReAddItem_NoUpdate(item2, MapleInventoryType.EQUIPPED);
                    }
                    int level = eq.getState() - 16;
                    int poten = potential(item.getItemId(), level);
                    eq.setPotential1(poten);
                    eq.setPotential2(poten);
                    eq.setPotential3(poten);
                    c.getSession().writeAndFlush(InventoryPacket.scrolledItem(toUse, item, false, true, false));
                    c.getPlayer().getMap().broadcastMessage(CField.showPotentialReset(c.getPlayer().getId(), true, itemId, eq.getItemId()));
                    c.getPlayer().forceReAddItem_NoUpdate(item, MapleInventoryType.EQUIP);
                    used = true;
                } else {
                    c.getPlayer().getMap()
                            .broadcastMessage(CField.showPotentialReset(c.getPlayer().getId(), false, itemId, 0));
                }
                break;
            }
            case 5062009: { //레드 큐브
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                int pos = slea.readInt();
                Item item = c.getPlayer().getInventory(pos < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) pos);
                boolean up = false;
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    Equip eq = (Equip) item;
                    int rand = Randomizer.nextInt(100);
                    MapleInventoryManipulator.addById(c, 2431893, (short) 1, null, null, 0L, "Reward item: " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                    if (eq.getState() == 1 || eq.getState() == 17) {
                        if (rand < 10) {
                            up = true;
                            eq.setState((byte) 18);
                        } else {
                            eq.setState((byte) 17);
                        }
                    } else if (eq.getState() == 18 && !ii.isCash(eq.getItemId())) {
                        if (rand < 5) {
                            up = true;
                            eq.setState((byte) 19);
                        } else {
                            eq.setState((byte) 18);
                        }
                    } else if (eq.getState() == 19) {
                        if (rand < 3) {
                            up = true;
                            eq.setState((byte) 20);
                        } else {
                            eq.setState((byte) 19);
                        }
                    }
                    int level = eq.getState() - 16;
                    eq.setPotential1(potential(item.getItemId(), level));
                    eq.setPotential2(potential(item.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1)));
                    eq.setPotential3(eq.getPotential3() != 0 ? potential(item.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1)) : 0);
                    eq.setLines((byte) (eq.getPotential3() > 0 ? 3 : 2));
                    c.getPlayer().gainMeso(-GameConstants.getCubeMeso(eq.getItemId()), false);

                    if (GameConstants.isZeroWeapon(eq.getItemId())) {
                        Equip zeroequip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                        zeroequip.setState(eq.getState());
                        zeroequip.setLines(eq.getLines());
                        zeroequip.setPotential1(eq.getPotential1());
                        zeroequip.setPotential2(eq.getPotential2());
                        zeroequip.setPotential3(eq.getPotential3());
                        if (zeroequip != null) {
                            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, zeroequip));
                        }
                    }

                    c.getPlayer().forceReAddItem(item, pos < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP);
                    c.getSession().writeAndFlush(CField.showPotentialReset(c.getPlayer().getId(), true, itemId, eq.getItemId()));
                    c.getSession().writeAndFlush(CField.getRedCubeStart(c.getPlayer(), item, up, itemId, c.getPlayer().itemQuantity(toUse.getItemId()) - 1));
//                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "소비 인벤토리의 공간이 부족하여 잠재 설정을 할 수 없습니다.");
                }
                break;
            }
            case 5062010: { //블랙 큐브
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                int pos = slea.readInt();
                Item item = c.getPlayer().getInventory(pos < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) pos);
                boolean up = false;
                if (item != null && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    Equip eq = (Equip) item;
                    int rand = Randomizer.nextInt(100);
                    MapleInventoryManipulator.addById(c, 2431894, (short) 1, null, null, 0, "");
                    Equip neq = (Equip) eq.copy();
                    if (neq.getState() >= 17) {
                        if (neq.getState() == 1 || neq.getState() == 17) {
                            if (rand < 10) {
                                up = true;
                                neq.setState((byte) 18);
                            } else {
                                neq.setState((byte) 17);
                            }
                        } else if (neq.getState() == 18 && !ii.isCash(neq.getItemId())) {
                            if (rand < 5) {
                                up = true;
                                neq.setState((byte) 19);
                            } else {
                                neq.setState((byte) 18);
                            }
                        } else if (neq.getState() == 19) {
                            if (rand < 3) {
                                up = true;
                                neq.setState((byte) 20);
                            } else {
                                neq.setState((byte) 19);
                            }
                        }
                        int level = neq.getState() - 16;
                        neq.setPotential1(potential(item.getItemId(), level));
                        neq.setPotential2(potential(item.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1)));
                        neq.setPotential3(neq.getPotential3() != 0 ? potential(item.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1)) : 0);
                        neq.setLines((byte) (neq.getPotential3() > 0 ? 3 : 2));
                        c.getPlayer().gainMeso(-GameConstants.getCubeMeso(neq.getItemId()), false);
//                        c.getSession().writeAndFlush(CField.showPotentialReset(c.getPlayer().getId(), true, itemId, neq.getItemId()));
                        c.getSession().writeAndFlush(CField.getBlackCubeStart(c.getPlayer(), neq, up, 5062010, toUse.getPosition(), c.getPlayer().itemQuantity(toUse.getItemId()) - 1));
                        c.getPlayer().getMap().broadcastMessage(CField.getBlackCubeEffect(c.getPlayer().getId(), up, 5062010, neq.getItemId()));

                        c.getPlayer().choicepotential = neq;

                        if (c.getPlayer().memorialcube == null) {
                            c.getPlayer().memorialcube = toUse.copy();
                        }
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "Make sure your equipment has a potential.");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "소비 인벤토리의 공간이 부족하여 잠재 설정을 할 수 없습니다.");
                }
                break;
            }
            case 5152300: { // 믹스렌즈
                boolean dressUp = slea.readByte() == 1;
                boolean isBeta = slea.readByte() == 1;
                boolean isAlphaBeta = slea.readByte() == 1;

                if (MapleItemInformationProvider.getInstance().getAllSpecialHairFaces().contains((dressUp || isBeta) ? c.getPlayer().getSecondFace() : c.getPlayer().getFace())) {
                    c.getPlayer().dropMessage(1, "믹스렌즈를 할 수 없는 얼굴입니다.");
                    return;
                }

                slea.readByte();

                int ordinaryColor = slea.readInt();
                int addColor = slea.readInt();

                int baseFace = c.getPlayer().getFace() < 100000 ? c.getPlayer().getFace() : c.getPlayer().getFace() / 1000;
                if (dressUp || isBeta) {
                    baseFace = c.getPlayer().getSecondFace() < 100000 ? c.getPlayer().getSecondFace() : c.getPlayer().getSecondFace() / 1000;
                }
                baseFace = baseFace - baseFace % 1000 + baseFace % 100 + ordinaryColor * 100;

                int newFace = baseFace * 1000 + addColor * 100 + slea.readInt(); // %값

                if (dressUp) {
                    c.getPlayer().setSecondFace(newFace);
                    c.getPlayer().updateAngelicStats();
                } else if (isBeta) {
                    c.getPlayer().setSecondFace(newFace);
                    c.getPlayer().updateZeroStats();
                } else if (isAlphaBeta) {
                    c.getPlayer().setFace(newFace);
                    c.getPlayer().setSecondFace(newFace);
                    c.getPlayer().updateSingleStat(MapleStat.FACE, newFace);
                } else {
                    c.getPlayer().setFace(newFace);
                    c.getPlayer().updateSingleStat(MapleStat.FACE, newFace);
                }

                c.getSession().writeAndFlush(CWvsContext.mixLense(itemId, baseFace, newFace, dressUp, isBeta, isAlphaBeta, c.getPlayer()));
                c.getPlayer().equipChanged();
                used = true;

                break;
            }
            case 5152301: { // 알쏭달쏭 믹스렌즈
                int[] forbiddenFaces = {22100, 22200, 22300, 22400, 22500, 22600, 22700, 22800};

                for (int face : forbiddenFaces) {
                    if (c.getPlayer().getFace() == face) {
                        used = false;
                        c.getPlayer().dropMessage(1, "믹스렌즈가 불가능한 성형입니다.");
                        return;
                    }
                }
                boolean dressUp = slea.readByte() == 1;
                boolean isBeta = slea.readByte() == 1;
                boolean isAlphaBeta = slea.readByte() == 1;
                int baseFace = c.getPlayer().getFace() < 100000 ? c.getPlayer().getFace() : c.getPlayer().getFace() / 1000;

                int ordinaryColor = Randomizer.nextInt(8);
                int addColor = Randomizer.nextInt(8);
                while (addColor == ordinaryColor) {
                    addColor = Randomizer.nextInt(8);
                }

                baseFace = baseFace - baseFace % 1000 + baseFace % 100 + ordinaryColor * 100;

                int newFace = baseFace * 1000 + addColor * 100 + Randomizer.rand(1, 99);

                c.getSession().writeAndFlush(CWvsContext.mixLense(itemId, baseFace, newFace, dressUp, isBeta, isAlphaBeta, c.getPlayer()));

                if (dressUp) {
                    c.getPlayer().setSecondFace(newFace);
                    if (c.getPlayer().getDressup()) {
                        c.getPlayer().updateSingleStat(MapleStat.FACE, newFace);
                    }
                } else if (isBeta) {
                    c.getPlayer().setSecondFace(newFace);
                    if (c.getPlayer().getGender() == 1) {
                        c.getPlayer().updateSingleStat(MapleStat.FACE, newFace);
                    }
                } else if (isAlphaBeta) {
                    c.getPlayer().setFace(newFace);
                    c.getPlayer().updateSingleStat(MapleStat.FACE, newFace);
                } else {
                    c.getPlayer().setFace(newFace);
                    c.getPlayer().updateSingleStat(MapleStat.FACE, newFace);
                }

                c.getPlayer().equipChanged();
                used = true;
                break;
            }
            case 5521000: { // 쉐어 네임 텍
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((short) slea.readInt());

                if (item != null && !ItemFlag.TRADEABLE_ONETIME_EQUIP.check(item.getFlag())) {
                    if (MapleItemInformationProvider.getInstance().isShareTagEnabled(item.getItemId())) {
                        int flag = item.getFlag();
                        if (type == MapleInventoryType.EQUIP) {
                            flag += ItemFlag.TRADEABLE_ONETIME_EQUIP.getValue();
                        } else {
                            return;
                        }
                        item.setFlag(flag);
                        c.getPlayer().forceReAddItem_NoUpdate(item, type);
                        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));
                        used = true;
                    }
                }
                break;
            }
            case 5069100: { // 루나 크리스탈
                int itemid[] = {5002033, 5002034, 5002035, 5002082, 5002083, 5002084, 5002137, 5002138, 5002139, 5002197, 5002198, 5002199, 1802653};
                slea.skip(4);
                short baseslot = slea.readShort();
                slea.skip(12);
                short usingslot = slea.readShort();
                final Item baseitem = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(baseslot);
                final Item usingitem = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(usingslot);
                final int basegrade = baseitem.getPet().getWonderGrade();
                if (basegrade == 1 || basegrade == 4) { // 기본, 레드라벨
                    if (usingitem.getPet().getWonderGrade() == 1) { // 기본 원더펫이여야함.
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, baseitem.getPosition(), (short) 1, false);
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, usingitem.getPosition(), (short) 1, false);
                        // 우선 템 2개를 제거한다.
                        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                        Item item;
                        int rand = Randomizer.nextInt(10000);
                        int i;
                        if (rand < 196) {
                            i = 12;
                        } else if (rand < 1360) {
                            i = Randomizer.rand(9, 11);
                        } else {
                            i = Randomizer.rand(0, 8);
                        }
                        if (GameConstants.isPet(itemid[i])) {
                            MaplePet pet = MaplePet.createPet(itemid[i], -1);
                            if (PetDataFactory.getWonderGrade(itemid[i]) != 6) { // P펫이 아니라면
                                if (basegrade == 1) {
                                    pet.setWonderGrade(4);
                                } else if (basegrade == 4) {
                                    pet.setWonderGrade(5);
                                }
                            }
                            item = MapleInventoryManipulator.addId_Item(c, itemid[i], (short) 1, "", pet, 30, "", false);
                        } else {
                            //   short flag = (short) ItemFlag.KARMA_EQ.getValue();
                            if (GameConstants.getInventoryType(itemid[i]) == MapleInventoryType.EQUIP) {
                                item = (Equip) ii.getEquipById(itemid[i]);
                            } else {
                                item = new client.inventory.Item(itemid[i], (byte) 0, (short) 1, (byte) ItemFlag.KARMA_EQUIP.getValue());
                            }
                            item.setUniqueId(MapleInventoryIdentifier.getInstance());
                            //   item.setFlag(flag);
                            MapleInventoryManipulator.addbyItem(c, item);
                        }
                        if (item != null) {
                            c.getSession().writeAndFlush(CSPacket.LunaCrystal(item));
                        }
                        used = true;
                        //2B 01 84 00 00 00 01 05 01 00 00 00
                    }
                }
                break;
            }

            case 5068305: { // 블랙베리
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    int itemid[][] = {{2438145, 1}, {2046076, 1}, {2046077, 1}, {2046150, 1}, {2046340, 1}, {2046341, 1}, {2048047, 1}, {2048048, 1}, {4310266, 200}, {4310266, 250}, {4310266, 300}, {4310237, 900}, {4310229, 500}, {4310229, 300}, {4310229, 200}, {4310229, 500}, {4310229, 500}, {4310229, 500}, {4310237, 700}, {4310237, 1000}, {4031227, 200}, {4031227, 300}, {4031227, 100}, {2430041, 1}, {2430042, 1}, {2430043, 1}, {2430044, 1}, {5062005, 2}, {5062005, 3}, {5062005, 5}, {2046991, 10}, {2046992, 10}, {2047814, 10}, {2048753, 30}, {2630127, 1}, {2630127, 1}, {2431940, 4}, {2048753, 20}, {2048753, 30}, {4001716, 10}, {4001716, 4}, {4001716, 5}, {4009005, 200}, {4021031, 1500}, {4021031, 2000}, {4001716, 10}};
                    int i = Randomizer.nextInt(itemid.length);
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    Item item;
                    if (GameConstants.isPet(itemid[i][0])) {
                        item = MapleInventoryManipulator.addId_Item(c, itemid[i][0], (short) itemid[i][1], "", MaplePet.createPet(itemid[i][0], -1), 30, "", false);
                        //World.Broadcast.broadcastMessage(CWvsContext.serverMessage(11, c.getChannel(), c.getPlayer().getName(), c.getPlayer().getName() + "님이 블랙 베리 에서 [" + ii.getName(itemid[i][0]) + "] 을(를) 획득했습니다.", true, item));
                    } else {
                        short flag = (short) ItemFlag.UNTRADEABLE.getValue();
                        if (GameConstants.getInventoryType(itemid[i][0]) == MapleInventoryType.EQUIP) {
                            item = (Equip) ii.getEquipById(itemid[i][0]);
                        } else {
                            item = new client.inventory.Item(itemid[i][0], (byte) 0, (short) itemid[i][1], (byte) ItemFlag.UNTRADEABLE.getValue());
                        }
                        if (MapleItemInformationProvider.getInstance().isCash(itemid[i][0])) {
                            item.setUniqueId(MapleInventoryIdentifier.getInstance());
                        }
                        item.setFlag(flag);
                        MapleInventoryManipulator.addbyItem(c, item);
                        //  World.Broadcast.broadcastMessage(CWvsContext.serverMessage(11, c.getChannel(), c.getPlayer().getName(), c.getPlayer().getName() + "님이 블랙 베리 에서 [" + ii.getName(itemid[i][0]) + "] 을(를) 획득했습니다.", true, item));
                    }
                    if (item != null) {
                        c.getSession().writeAndFlush(CSPacket.WonderBerry((byte) 1, item, toUse.getItemId()));
                    }
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "소비, 캐시, 장비 여유 공간이 각각 한칸이상 부족합니다.");
                }
                break;
            }

            case 5068300: { // 위습의 원더베리
                if (c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 1 && c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                    int itemid[] = {5069100, 5002226, 5002227, 5002228, 5002230, 5002231, 5000939, 5000940,4001716,2022996,1113070}; //5069100
                    int rand = Randomizer.nextInt(10000);
                    int i;
                    if (rand < 996) {
                        i = Randomizer.rand(0, 3);
                    } else if (rand < 6996) {
                        i = Randomizer.rand(4, 8);
                    } else {
                        i = Randomizer.rand(9, 10);
                    }
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    Item item;
                    if (GameConstants.isPet(itemid[i])) {
                        item = MapleInventoryManipulator.addId_Item(c, itemid[i], (short) 1, "", MaplePet.createPet(itemid[i], -1), 30, "", false);
                    } else {
                        //   short flag = (short) ItemFlag.KARMA_EQ.getValue();
                        if (GameConstants.getInventoryType(itemid[i]) == MapleInventoryType.EQUIP) {
                            item = (Equip) ii.getEquipById(itemid[i]);
                        } else {
                            item = new client.inventory.Item(itemid[i], (byte) 0, (short) 1);
                        }
                        if (MapleItemInformationProvider.getInstance().isCash(itemid[i])) {
                            item.setUniqueId(MapleInventoryIdentifier.getInstance());
                        }
                        //   item.setFlag(flag);
                        MapleInventoryManipulator.addbyItem(c, item);
                    }
                    if (item != null) {
                        c.getSession().writeAndFlush(CSPacket.WonderBerry((byte) 1, item, toUse.getItemId()));
                    }
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "소비, 캐시, 장비 여유 공간이 각각 한칸이상 부족합니다.");
                }
                break;
            }
            case 5520001: //p.karma
            case 5520000: { // Karma
                final MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                final Item item = c.getPlayer().getInventory(type).getItem((short) slea.readInt());

                if (item != null && !ItemFlag.KARMA_EQUIP.check(item.getFlag()) && !ItemFlag.KARMA_USE.check(item.getFlag())) {
                    if ((itemId == 5520000 && MapleItemInformationProvider.getInstance().isKarmaEnabled(item.getItemId())) || (itemId == 5520001 && MapleItemInformationProvider.getInstance().isPKarmaEnabled(item.getItemId()))) {
                        int flag = item.getFlag();
                        if (type == MapleInventoryType.EQUIP) {
                            flag += ItemFlag.KARMA_EQUIP.getValue();
                        } else {
                            flag += ItemFlag.KARMA_USE.getValue();
                        }

                        if (item.getType() == 1) {
                            Equip eq = (Equip) item;
                            if (eq.getKarmaCount() > 0) {
                                eq.setKarmaCount((byte) (eq.getKarmaCount() - 1));
                            }
                        }

                        item.setFlag(flag);
                        c.getPlayer().forceReAddItem_NoUpdate(item, type);
                        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, type, item));
                        used = true;
                    }
                }
                break;
            }
            case 5570000: { // Vicious Hammer
                slea.readInt(); // Inventory type, Hammered eq is always EQ.
                final Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slea.readInt());
                // another int here, D3 49 DC 00
                if (item != null) {
                    if (GameConstants.canHammer(item.getItemId()) && MapleItemInformationProvider.getInstance().getSlots(item.getItemId()) > 0 && item.getViciousHammer() < 2) {
                        item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                        c.getPlayer().forceReAddItem(item, MapleInventoryType.EQUIP);
                        c.getSession().writeAndFlush(CSPacket.ViciousHammer(true, true));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(5, "You may not use it on this item.");
                        c.getSession().writeAndFlush(CSPacket.ViciousHammer(true, false));
                    }
                }

                break;
            }
            case 5063000: {
                Equip toScroll;
                slea.skip(2);
                short dst = slea.readShort();
                if (dst < 0) {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                } else {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                }

                int flag = toScroll.getFlag();
                flag |= ItemFlag.LUCKY_PROTECT_SHIELD.getValue();
                toScroll.setFlag(flag);
                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, toScroll));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, false, toUse.getItemId(), toScroll.getItemId()), true);
                used = true;
                break;
            }
            case 5064000: {
                Equip toScroll;
                slea.skip(2);
                short dst = slea.readShort();
                if (dst < 0) {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                } else {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                }
                if (toScroll.getEnhance() >= 12) {
                    break;
                }
                int flag = toScroll.getFlag();
                flag |= ItemFlag.PROTECT_SHIELD.getValue();
                toScroll.setFlag(flag);

                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, toScroll));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, false, toUse.getItemId(), toScroll.getItemId()), true);
                used = true;
                break;
            }
            case 5064100: {
                Equip toScroll;
                slea.skip(2);
                short pos = slea.readShort();
                if (pos < 0) {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                } else {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                }
                if (toScroll.getUpgradeSlots() == 0) {
                    break;
                }
                int flag = toScroll.getFlag();
                flag |= ItemFlag.SAFETY_SHIELD.getValue();
                toScroll.setFlag(flag);

                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, toScroll));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, false, toUse.getItemId(), toScroll.getItemId()), true);
                used = true;
                break;
            }
            case 5064300: {
                Equip toScroll;
                slea.skip(2);
                short dst = slea.readShort();
                if (dst < 0) {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                } else {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                }
                int flag = toScroll.getFlag();
                flag |= ItemFlag.RECOVERY_SHIELD.getValue();
                toScroll.setFlag(flag);
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(MapleInventoryType.EQUIP, toScroll));
                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, toScroll));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, false, toUse.getItemId(), toScroll.getItemId()), true);
                used = true;
                break;
            }
            case 5064400: {
                Equip toScroll;
                slea.skip(2);
                short dst = slea.readShort();

                if (dst < 0) {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                } else {
                    toScroll = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
                }
                int flag = toScroll.getFlag();
                flag |= ItemFlag.RETURN_SCROLL.getValue();
                toScroll.setFlag(flag);

                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, toScroll));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, false, toUse.getItemId(), toScroll.getItemId()), true);
                used = true;

//            	used = false;
                //          	c.getPlayer().dropMessage(1, "리턴 스크롤 오류로 인해 잠시 사용이 불가능해집니다.");
                break;
            }
            case 5060004:
            case 5060003: {//peanut
                Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).findById(itemId == 5060003 ? 4170023 : 4170024);
                if (item == null || item.getQuantity() <= 0) { // hacking{
                    return;
                }
                break;
            }

            case 5070000: { // Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(2, c.getPlayer().getName(), sb.toString()));
                    DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                    used = true;
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5071000: { // Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 0) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        c.getChannelServer().broadcastSmegaPacket(CWvsContext.serverNotice(2, c.getPlayer().getName(), sb.toString()));
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5077000: { // 3 line Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final byte numLines = slea.readByte();
                    if (numLines > 3) {
                        return;
                    }
                    final List<String> messages = new LinkedList<String>();
                    String message;
                    for (int i = 0; i < numLines; i++) {
                        message = slea.readMapleAsciiString();
                        if (message.length() > 65) {
                            break;
                        }
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        messages.add(c.getPlayer().getName() + " : " + message);
                    }
                    final boolean ear = slea.readByte() > 0;

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        World.Broadcast.broadcastSmega(CWvsContext.tripleSmega(c.getPlayer().getName(), messages, ear, c.getChannel()));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5079004: { // Heart Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        World.Broadcast.broadcastSmega(CWvsContext.echoMegaphone(c.getPlayer().getName(), message));
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5073000: { // Heart Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        World.Broadcast.broadcastSmega(CWvsContext.serverNotice(9, c.getChannel(), c.getPlayer().getName(), sb.toString(), ear));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5074000: { // Skull Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        World.Broadcast.broadcastSmega(CWvsContext.serverNotice(22, c.getChannel(), c.getPlayer().getName(), sb.toString(), ear));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5072000: { // Super Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() != 0;

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        World.Broadcast.broadcastSmega(CWvsContext.serverNotice(3, c.getChannel(), c.getPlayer().getName(), sb.toString(), ear));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5076000: { // Item Megaphone
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final String message = slea.readMapleAsciiString();

                    if (message.length() > 65) {
                        break;
                    }
                    final StringBuilder sb = new StringBuilder();
                    addMedalString(c.getPlayer(), sb);
                    sb.append(c.getPlayer().getName());
                    sb.append(" : ");
                    sb.append(message);

                    final boolean ear = slea.readByte() > 0;

                    Item item = null;
                    if (slea.readByte() == 1) { //item
                        byte invType = (byte) slea.readInt();
                        byte pos = (byte) slea.readInt();
                        if (pos <= 0) {
                            invType = -1;
                        }
                        item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                    }

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                        World.Broadcast.broadcastSmega(CWvsContext.itemMegaphone(c.getPlayer().getName(), sb.toString(), ear, c.getChannel(), item, itemId));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5076100: {
                final String message = slea.readMapleAsciiString();
                final StringBuilder sb = new StringBuilder();
                addMedalString(c.getPlayer(), sb);
                sb.append(c.getPlayer().getName());
                sb.append(" : ");
                sb.append(message);
                final boolean ear = slea.readByte() > 0;
                Item item = null;
                if (slea.readByte() == 1) { // item
                    byte invType = (byte) slea.readInt();
                    byte pos = (byte) slea.readInt();
                    if (pos <= 0) {
                        invType = -1;
                    }
                    item = c.getPlayer().getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                }
                World.Broadcast.broadcastSmega(CWvsContext.HyperMegaPhone(sb.toString(), c.getPlayer().getName(), message,
                        c.getChannel(), ear, item));
                used = true;
                break;
            }
            case 5075003:
            case 5075004:
            case 5075005: {
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "Must be level 10 or higher.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "Cannot be used here.");
                    break;
                }
                int tvType = itemId % 10;
                if (tvType == 3) {
                    slea.readByte(); //who knows
                }
                boolean ear = tvType != 1 && tvType != 2 && slea.readByte() > 1; //for tvType 1/2, there is no byte. 
                MapleCharacter victim = tvType == 1 || tvType == 4 ? null : c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString()); //for tvType 4, there is no string.
                if (tvType == 0 || tvType == 3) { //doesn't allow two
                    victim = null;
                } else if (victim == null) {
                    c.getPlayer().dropMessage(1, "That character is not in the channel.");
                    break;
                }
                String message = slea.readMapleAsciiString();

                if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                    World.Broadcast.chatDelay = System.currentTimeMillis();
                    DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), message, "채널 : " + c.getChannel());
                    World.Broadcast.broadcastSmega(CWvsContext.serverNotice(3, c.getChannel(), c.getPlayer().getName(), c.getPlayer().getName() + " : " + message, ear));
                    used = true;
                } else {
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                }
                break;
            }
            case 5090100: // Wedding Invitation Card
            case 5090000: { // Note
                final String sendTo = slea.readMapleAsciiString();
                final String msg = slea.readMapleAsciiString();
                if (MapleCharacterUtil.canCreateChar(sendTo, false)) { // Name does not exist 
                    c.getSession().writeAndFlush(CSPacket.OnMemoResult((byte) 9, (byte) 1));
                } else {
                    int ch = World.Find.findChannel(sendTo);
                    if (ch <= 0) { // offline 
                        c.getPlayer().sendNote(sendTo, msg);
                        c.getSession().writeAndFlush(CSPacket.OnMemoResult((byte) 8, (byte) 0));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CSPacket.OnMemoResult((byte) 9, (byte) 0));
                    }
                }
                break;
            }
            case 5100000: { // Congratulatory Song
                c.getPlayer().getMap().broadcastMessage(CField.musicChange("Jukebox/Congratulation"));
                used = true;
                break;
            }
            case 5190000: //아이템 줍기 스킬
            case 5190001: //HP 물약충전 스킬
            case 5190002: //이동반경 확대 스킬
            case 5190003: //자동 줍기 스킬
            case 5190004: //소유권 없는 아이템, 메소 획득 스킬
            case 5190005: //특정 아이템 줍지 않기 스킬
            case 5190006: //MP 물약충전 스킬
            case 5190010: //펫 버프 자동스킬
            case 5190011: //펫 훈련 스킬
            case 5190012: //펫 자이언트 스킬
            case 5190013: //펫 상점 열기 스킬
            {
                int uniqueId = slea.readInt();
                MaplePet pet = null;
                int petIndex = c.getPlayer().getPetIndex(uniqueId);
                if (petIndex >= 0) { //장착중인 펫일때
                    pet = c.getPlayer().getPet(petIndex);
                } else {
                    pet = c.getPlayer().getInventory(MapleInventoryType.CASH).findByUniqueId(uniqueId).getPet();
                }
                if (pet == null) {
                    c.getPlayer().dropMessage(1, "펫을 찾는데 실패하였습니다!");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                PetFlag zz = PetFlag.getByAddId(itemId);
                pet.setFlags(pet.getFlags() | zz.getValue());
                c.getPlayer().getMap().broadcastMessage(PetPacket.updatePet(c.getPlayer(), pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
                used = true;
                break;
            }
            case 5191000: //아이템 줍기 삭제
            case 5191001: //HP 물약충전 삭제
            case 5191002: //이동반경 확대 삭제
            case 5191003: //자동 줍기 삭제
            case 5191004: { //소유권 없는 아이템, 메소 획득 삭제
                int uniqueId = slea.readInt();
                MaplePet pet = null;
                int petIndex = c.getPlayer().getPetIndex(uniqueId);
                if (petIndex >= 0) { //장착중인 펫일때
                    pet = c.getPlayer().getPet(petIndex);
                } else {
                    pet = c.getPlayer().getInventory(MapleInventoryType.CASH).findByUniqueId(uniqueId).getPet();
                }
                if (pet == null) {
                    c.getPlayer().dropMessage(1, "펫을 찾는데 실패하였습니다!");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                PetFlag zz = PetFlag.getByAddId(itemId);
                pet.setFlags(pet.getFlags() - zz.getValue());
                c.getPlayer().getMap().broadcastMessage(PetPacket.updatePet(c.getPlayer(), pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
                used = true;
                break;
            }
            case 5781002: { //펫 염색 쿠폰
                final int uniqueid = (int) slea.readLong();
                int color = slea.readInt();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;
                if (pet == null) {
                    break;
                }
                if (pet.getUniqueId() != uniqueid) {
                    pet = c.getPlayer().getPet(1);
                    slo = 1;
                    if (pet != null) {
                        if (pet.getUniqueId() != uniqueid) {
                            pet = c.getPlayer().getPet(2);
                            slo = 2;
                            if (pet != null) {
                                if (pet.getUniqueId() != uniqueid) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                pet.setColor(color);
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.showPet(c.getPlayer(), pet, false, false), true);
                break;
            }
            case 5501001:
            case 5501002: { //expiry mount
                final Skill skil = SkillFactory.getSkill(slea.readInt());
                if (skil == null || skil.getId() / 10000 != 8000 || c.getPlayer().getSkillLevel(skil) <= 0 || !skil.isTimeLimited() || GameConstants.getMountItem(skil.getId(), c.getPlayer()) <= 0) {
                    break;
                }
                final long toAdd = (itemId == 5501001 ? 30 : 60) * 24 * 60 * 60 * 1000L;
                final long expire = c.getPlayer().getSkillExpiry(skil);
                if (expire < System.currentTimeMillis() || (long) (expire + toAdd) >= System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)) {
                    break;
                }
                c.getPlayer().changeSingleSkillLevel(skil, c.getPlayer().getSkillLevel(skil), c.getPlayer().getMasterLevel(skil), (long) (expire + toAdd));
                used = true;
                break;
            }
            case 5170000: { // 펫 작명 쿠폰
                final int uniqueid = (int) slea.readLong();
                MaplePet pet = c.getPlayer().getPet(0);
                int slo = 0;

                if (pet == null) {
                    break;
                }
                if (pet.getUniqueId() != uniqueid) {
                    pet = c.getPlayer().getPet(1);
                    slo = 1;
                    if (pet != null) {
                        if (pet.getUniqueId() != uniqueid) {
                            pet = c.getPlayer().getPet(2);
                            slo = 2;
                            if (pet != null) {
                                if (pet.getUniqueId() != uniqueid) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                String nName = slea.readMapleAsciiString();
                for (String z : GameConstants.RESERVED) {
                    if (pet.getName().indexOf(z) != -1 || nName.indexOf(z) != -1) {
                        break;
                    }
                }
                pet.setName(nName);
                c.getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                c.getPlayer().getMap().broadcastMessage(CSPacket.changePetName(c.getPlayer(), nName, slo));
                used = true;
                break;
            }
            case 5700000: { //안드로이드 작명 쿠폰
                slea.skip(8);
                if (c.getPlayer().getAndroid() == null) {
                    c.getPlayer().dropMessage(1, "장착중인 안드로이드가 없어 작명 할 수 없습니다.");
                    break;
                }
                String nName = slea.readMapleAsciiString();
                for (String z : GameConstants.RESERVED) {
                    if (c.getPlayer().getAndroid().getName().indexOf(z) != -1 || nName.indexOf(z) != -1) {
                        break;
                    }
                }
                c.getPlayer().getAndroid().setName(nName);
                c.getPlayer().setAndroid(c.getPlayer().getAndroid()); //respawn it
                used = true;
                break;
            }
            case 5240000:
            case 5240001:
            case 5240002:
            case 5240003:
            case 5240004:
            case 5240005:
            case 5240006:
            case 5240007:
            case 5240008:
            case 5240009:
            case 5240010:
            case 5240011:
            case 5240012:
            case 5240013:
            case 5240014:
            case 5240015:
            case 5240016:
            case 5240017:
            case 5240018:
            case 5240019:
            case 5240020:
            case 5240021:
            case 5240022:
            case 5240023:
            case 5240024:
            case 5240025:
            case 5240026:
            case 5240027:
            case 5240029:
            case 5240030:
            case 5240031:
            case 5240032:
            case 5240033:
            case 5240034:
            case 5240035:
            case 5240036:
            case 5240037:
            case 5240038:
            case 5240039:
            case 5240040:
            case 5240028:
            case 5240088: { // Pet food
                for (MaplePet pet : c.getPlayer().getPets()) {
                    if (!pet.canConsume(itemId)) {
                        int petindex = c.getPlayer().getPetIndex(pet);
                        pet.setFullness(100);
                        if (pet.getCloseness() < 30000) {
                            if (pet.getCloseness() + 100 > 30000) {
                                pet.setCloseness(30000);
                            } else {
                                pet.setCloseness(pet.getCloseness() + 100);
                            }
                            if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                pet.setLevel(pet.getLevel() + 1);
                                c.getSession().writeAndFlush(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), true));
                                c.getPlayer().getMap().broadcastMessage(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), false));
                            }
                        }
                        c.getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(c.getPlayer().getId(), (byte) 1, (byte) petindex, true), true);
                    }
                }
                used = true;
                break;
            }
            case 5230001:
            case 5230000: { //미네르바의 부엉이
                final int itemSearch = slea.readInt();
                final List<HiredMerchant> hms = c.getChannelServer().searchMerchant(itemSearch);
                if (hms.size() > 0) {
                    c.getSession().writeAndFlush(CWvsContext.getOwlSearched(itemSearch, hms));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "아이템을 찾을 수 없습니다.");
                }
                break;
            }
            case 5370001:
            case 5370000: { // Chalkboard
/*                for (MapleEventType t : MapleEventType.values()) {
                 final MapleEvent e = ChannelServer.getInstance(c.getChannel()).getEvent(t);
                 if (e.isRunning()) {
                 for (int i : e.getType().mapids) {
                 if (c.getPlayer().getMapId() == i) {
                 c.getPlayer().dropMessage(5, "You may not use that here.");
                 c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                 return;
                 }
                 }
                 }
                 }*/
                c.getPlayer().setChalkboard(slea.readMapleAsciiString());
                break;
            }
            case 5079000:
            case 5079001:
            case 5390007:
            case 5390008:
            case 5390009:
            case 5390000: // Diablo Messenger
            case 5390001: // 아이리스 메가폰
            case 5390002: // Loveholic Messenger
            case 5390003: // New Year Megassenger 1
            case 5390004: // New Year Megassenger 2
            case 5390005: // Cute Tiger Messenger
            case 5390006:
            case 5390010:
            case 5390011:
            case 5390012:
            case 5390013:
            case 5390014:
            case 5390015:
            case 5390016:
            case 5390017:
            case 5390018:
            case 5390019:
            case 5390020:
            case 5390021:
            case 5390022:
            case 5390023:
            case 5390024:
            case 5390025:
            case 5390026:
            case 5390027:
            case 5390028:
            case 5390029:
            case 5390030:
            case 5390031:
            case 5390032:
            case 5390033: { // Tiger Roar's Messenger
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "10 레벨 이상이어야합니다.");
                    break;
                }
                if (c.getPlayer().getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(5, "여기에서는 사용하실 수 없습니다.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    final List<String> lines = new LinkedList<>();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 4; i++) {
                        final String text = slea.readMapleAsciiString();
                        if (text.length() > 55) {
                            lines.add("");
                        } else {
                            lines.add(text);
                            sb.append(text);
                        }
                    }
                    final boolean ear = slea.readByte() != 0;

                    if (System.currentTimeMillis() - World.Broadcast.chatDelay >= 3000) {
                        World.Broadcast.chatDelay = System.currentTimeMillis();
                        DBLogger.getInstance().logChat(LogType.Chat.Megaphone, c.getPlayer().getId(), c.getPlayer().getName(), sb.toString(), "채널 : " + c.getChannel());
                        World.Broadcast.broadcastSmega(CWvsContext.getAvatarMega(c.getPlayer(), c.getChannel(), itemId, lines, ear));
                        used = true;
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "전체 채팅은 3초마다 하실 수 있습니다."));
                    }
                } else {
                    c.getPlayer().dropMessage(5, "The usage of Megaphone is currently disabled.");
                }
                break;
            }
            case 5452001:
            case 5450003:
            case 5450000: { // Mu Mu the Travelling Merchant
                for (int i : GameConstants.blockedMaps) {
                    if (c.getPlayer().getMapId() == i) {
                        c.getPlayer().dropMessage(5, "You may not use this command here.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        return;
                    }
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
                } else if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                } else if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                } else {
                    MapleShopFactory.getInstance().getShop(61).sendShop(c);
                }
                //used = true;
                break;
            }
            case 5300000:
            case 5300001:
            case 5300002: { // Cash morphs
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                ii.getItemEffect(itemId).applyTo(c.getPlayer(), true);
                used = true;
                break;
            }
            case 5330000: { //퀵배송 이용권
                c.getPlayer().setConversation(2);
                c.getSession().writeAndFlush(CField.sendDuey((byte) 9, null, null));
                break;
            }
            case 5062402:
            case 5062400:
            case 5062405: {
                /*
                 * short viewSlot = (short) slea.readInt(); short descSlot = (short)
                 * slea.readInt(); Equip view_Item = (Equip)
                 * c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(viewSlot); Equip
                 * desc_Item = (Equip)
                 * c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(descSlot);
                 * c.getSession().writeAndFlush(CField.addMoruItem(desc_Item)); used = true;
                 * break;
                 */

                short viewSlot = (short) slea.readInt();
                short descSlot = (short) slea.readInt();
                Equip view_Item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(viewSlot);
                Equip desc_Item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(descSlot);
                if (view_Item.getMoru() != 0) {
                    desc_Item.setMoru(view_Item.getMoru());
                } else {
                    String lol = ((Integer) view_Item.getItemId()).toString();
                    String ss = lol.substring(3, 7);
                    desc_Item.setMoru(Integer.parseInt(ss));
                }
                c.getPlayer().forceReAddItem(desc_Item, MapleInventoryType.EQUIP);
                //c.getSession().writeAndFlush(CField.addMoruItem(desc_Item));
                used = true;
                break;
            }
            default:
                if (itemId / 10000 == 512 || itemId == 2432290) {
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    String msg = ii.getMsg(itemId);
                    final String ourMsg = slea.readMapleAsciiString();
                    /*                    if (!msg.contains("%s")) {
                     msg = ourMsg;
                     } else {
                     msg = msg.replaceFirst("%s", c.getPlayer().getName());
                     if (!msg.contains("%s")) {
                     msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                     } else {
                     try {
                     msg = msg.replaceFirst("%s", ourMsg);
                     } catch (Exception e) {
                     msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                     }
                     }
                     }*/
                    c.getPlayer().getMap().startMapEffect(ourMsg, itemId);

                    final int buff = ii.getStateChangeItem(itemId);
                    if (buff != 0) {
                        for (MapleCharacter mChar : c.getPlayer().getMap().getCharactersThreadsafe()) {
                            ii.getItemEffect(buff).applyTo(mChar, true);
                        }
                    }
                    used = true;
                } else if (itemId / 10000 == 510) {
                    c.getPlayer().getMap().startJukebox(c.getPlayer().getName(), itemId);
                    used = true;
                } else if (itemId / 10000 == 562) {
                    if (UseSkillBook(slot, itemId, c, c.getPlayer())) {
                        c.getPlayer().gainSP(1);
                    } //this should handle removing
                } else if (itemId / 10000 == 553) {
                    UseRewardItem(slot, itemId, (byte) 0, (byte) 0, c, c.getPlayer());// this too
                } else if (itemId / 10000 == 524) { //펫먹이
                    for (MaplePet pet : c.getPlayer().getPets()) {
                        if (pet != null) {
                            if (pet.canConsume(itemId)) {
                                int petindex = c.getPlayer().getPetIndex(pet);
                                pet.setFullness(100);
                                if (pet.getCloseness() < 30000) {
                                    if (pet.getCloseness() + 100 > 30000) {
                                        pet.setCloseness(30000);
                                    } else {
                                        pet.setCloseness(pet.getCloseness() + 100);
                                    }
                                    if (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                                        pet.setLevel(pet.getLevel() + 1);
                                        c.getSession().writeAndFlush(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), true));
                                        c.getPlayer().getMap().broadcastMessage(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), false));
                                    }
                                }
                                c.getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
                                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(c.getPlayer().getId(), (byte) 1, (byte) petindex, true), true);
                            }
                        }
                    }
                    used = true;
                } else if (itemId / 10000 != 519) {
                    System.out.println("Unhandled CS item : " + itemId);
                    System.out.println(slea.toString(true));
                }
                break;
        }

        if (used) {
            if (ItemFlag.KARMA_USE.check(toUse.getFlag())) {
                toUse.setFlag(toUse.getFlag() - ItemFlag.KARMA_USE.getValue() + ItemFlag.UNTRADEABLE.getValue());
                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.CASH, toUse));
            }

            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);

        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        if (cc) {
            if (!c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null || FieldLimitType.ChannelSwitch.check(c.getPlayer().getMap().getFieldLimit())) {
                c.getPlayer().dropMessage(1, "Auto relog failed.");
                return;
            }
            c.getPlayer().dropMessage(5, "Auto relogging. Please wait.");
            c.getPlayer().fakeRelog();
        }
    }

    public static final void Pickup_Player(final LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        slea.readInt();
        slea.skip(1); // or is this before tick?
        final Point Client_Reportedpos = slea.readPos();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        chr.setScrolledPosition((short) 0);

        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        pickupItem(ob, c, chr);
    }

    public static final void pickupItem(MapleMapObject ob, MapleClient c, MapleCharacter chr) {
        final MapleMapItem mapitem = (MapleMapItem) ob;
        try {
            mapitem.getLock().lock();
            if (mapitem.isPickedUp()) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (mapitem.getItemId() == 2431174 || mapitem.getItemId() == 2434021) { // 명예의 훈장.
                int rand = Randomizer.rand(10, 40);
                c.getPlayer().gainHonor(rand);
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() == 2632800) { //네오 스톤
                int a = (int) c.getPlayer().getKeyValue(100711, "point") + 1;
                c.getPlayer().setKeyValue(100711, "point", a + "");
                c.getPlayer().updateInfoQuest(100711, "point=" + c.getPlayer().getKeyValue(100711, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";today=0;total=0;lock=0"); //네오 스톤
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() == 2432393) {
                chr.gainMeso(Randomizer.rand(10000, 30000), true, false, false);
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() == 2432394) {
                chr.gainMeso(Randomizer.rand(30000, 50000), true, false, false);
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() == 2432395) {
                mapitem.getItem().setItemId(2000005);
            }
            if (mapitem.getItemId() == 2023484 || mapitem.getItemId() == 2023494
                    || mapitem.getItemId() == 2023495 || mapitem.getItemId() == 2023669) {
                if (mapitem.getDropper() instanceof MapleMonster) {
                    int bonus;
                    if (mapitem.getItemId() % 100 == 84) {
                        bonus = 1;
                    } else if (mapitem.getItemId() % 100 == 94) {
                        bonus = 1;
                    } else if (mapitem.getItemId() % 100 == 95) {
                        bonus = 1;
                    } else {
                        bonus = 1;
                    }

                    if (chr.getSkillLevel(20000297) > 0) {
                        bonus *= SkillFactory.getSkill(20000297).getEffect(chr.getSkillLevel(20000297)).getX();
                        bonus /= 100;
                    } else if (chr.getSkillLevel(80000370) > 0) {
                        bonus *= SkillFactory.getSkill(80000370).getEffect(chr.getSkillLevel(80000370)).getX();
                        bonus /= 100;
                    }

                    bonus *= 20;

                    MapleMonster mob = (MapleMonster) mapitem.getDropper();
                    chr.gainExp(mob.getMobExp() * c.getChannelServer().getExpRate() * bonus, true, true, false);
                    chr.getClient().getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, (int) mob.getMobExp() * c.getChannelServer().getExpRate() * bonus, 25, 0, 0, (byte) 0, true, null, "", null));
                }
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() == 2434851) {
                if (c.getPlayer().getBuffedValue(25121133)) {

                    List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> mbsvhZ = new ArrayList<>();

                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieDamR, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieDamR, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndiePad, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndiePad, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieBDR, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieBDR, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieIgnoreMobPdpR, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieIgnoreMobPdpR, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieBooster, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieBooster, 25121133)));

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : mbsvhZ) {
                        if (mbsvh != null) {
                            mbsvh.right.schedule.cancel(false);
                            mbsvh.right.localDuration += 4000;

                            statups.put(mbsvh.left, new Pair<>(mbsvh.right.value, (int) c.getPlayer().getBuffLimit(mbsvh.right.effect.getSourceId())));

                            final CancelEffectAction cancelAction = new CancelEffectAction(c.getPlayer(), mbsvh.right.effect, System.currentTimeMillis(), mbsvh.left);
                            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                                cancelAction.run();
                            }, c.getPlayer().getBuffLimit(mbsvh.right.effect.getSourceId()));

                            mbsvh.right.schedule = schedule;

                        }
                    }
                    c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, c.getPlayer().getBuffedEffect(25121133), c.getPlayer()));
                }
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() >= 4034942 && mapitem.getItemId() <= 4034958) {
                if (c.getPlayer().getRecipe().left == mapitem.getItemId()) {
                    c.getPlayer().setRecipe(new Pair<>(mapitem.getItemId(), c.getPlayer().getRecipe().right + 1));
                } else {
                    c.getPlayer().setRecipe(new Pair<>(mapitem.getItemId(), 1));
                }
                chr.getMap().broadcastMessage(CField.addItemMuto(chr));
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (mapitem.getMeso() > 0) {
                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                        if (m != null && m.getId() != chr.getId()) {
                            toGive.add(m);
                        }
                    }
                    for (final MapleCharacter m : toGive) {
                        int mesos = splitMeso / toGive.size() + (m.getStat().hasPartyBonus ? (int) (mapitem.getMeso() / 20.0) : 0);
                        if (mapitem.getDropper() instanceof MapleMonster && m.getStat().incMesoProp > 0) {
                            mesos += Math.floor((m.getStat().incMesoProp * mesos) / 100.0f);
                        }
                        m.gainMeso(mesos, true, false, false);
                    }
                    int mesos = mapitem.getMeso() - splitMeso;
                    if (mapitem.getDropper() instanceof MapleMonster && chr.getStat().incMesoProp > 0) {
                        mesos += Math.floor((chr.getStat().incMesoProp * mesos) / 100.0f);
                    }
                    chr.gainMeso(mesos, true, false, false);
                } else {
                    int mesos = mapitem.getMeso();
                    if (mapitem.getDropper() instanceof MapleMonster && chr.getStat().incMesoProp > 0) {
                        mesos += Math.floor((chr.getStat().incMesoProp * mesos) / 100.0f);
                    }
                    chr.gainMeso(mesos, true, false, false);
                }
                if (mapitem.getDropper().getType() == MapleMapObjectType.MONSTER) {
                    c.getSession().writeAndFlush(CWvsContext.onMesoPickupResult(mapitem.getMeso()));
                }
                removeItem(chr, mapitem, ob);
            } else if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                c.getPlayer().dropMessage(5, "This item cannot be picked up.");
            } else if (c.getPlayer().inPVP() && Integer.parseInt(c.getPlayer().getEventInstance().getProperty("ice")) == c.getPlayer().getId()) {
                c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            } else if (mapitem.getItemId() == 2431850) {
                MapleItemInformationProvider.getInstance().getItemEffect(2002093).applyTo(chr, true);
                removeItem(chr, mapitem, ob);
            } else if (mapitem.getItemId() / 10000 != 291 && MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItemId() == 2340000) {
                    c.setMonitored(true); //hack check
                }
                for (int id : GameConstants.showEffectDropItems) {
                    if (id == mapitem.getItemId()) {
                        c.getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, 0, 65, 0, 0, (byte) 0, true, null, null, mapitem.getItem()));
                        break;
                    }
                }
                if (GameConstants.isArcaneSymbol(mapitem.getItemId())) {
                    final Equip equip = (Equip) mapitem.getEquip();
                    equip.setArc((short) 30);
                    equip.setArcLevel((byte) 1);
                    if (equip.getArcEXP() == 0)
                        equip.setArcEXP(1);

                    if (GameConstants.isXenon(c.getPlayer().getJob())) {
                        equip.setStr((short) 117);
                        equip.setDex((short) 117);
                        equip.setLuk((short) 117);
                    } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                        equip.setHp((short) 525);
                    } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                        equip.setStr((short) 300);
                    } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                        equip.setInt((short) 300);
                    } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                        equip.setDex((short) 300);
                    } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                        equip.setLuk((short) 300);
                    } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                        equip.setStr((short) 300);
                    }
                }

                if (GameConstants.isAscenticSymbol(mapitem.getItemId())) {
                    final Equip equip = (Equip) mapitem.getEquip();
                    if (equip.getArcLevel() == 0) {
                        equip.setArc((short) 10);
                        equip.setArcLevel((byte) 1);
                        equip.setArcEXP(1);
                        if (GameConstants.isXenon(c.getPlayer().getJob())) {
                            equip.setStr((short) 195);
                            equip.setDex((short) 195);
                            equip.setLuk((short) 195);
                        } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                            equip.setHp((short) 8750);
                        } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                            equip.setStr((short) 500);
                        } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                            equip.setInt((short) 500);
                        } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                            equip.setDex((short) 500);
                        } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                            equip.setLuk((short) 500);
                        } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                            equip.setStr((short) 500);
                        }
                    }
                }
                //System.out.println(mapitem.getItem().getReward());
                if (mapitem.getItem().getItemId() == 4001886) {
                    if (chr.getLastBossId() != 0) {
                        int party = chr.getParty() != null ? chr.getParty().getMembers().size() : 1;
                        int price = GameConstants.price.containsKey(chr.getLastBossId()) ? GameConstants.price.get(chr.getLastBossId()) : 1;
                        mapitem.getItem().setReward(new BossReward(chr.getInventory(MapleInventoryType.ETC).countById(4001886) + 1, chr.getLastBossId(), party, price * price * 80));
                        chr.setLastBossId(0);
                    } else {
                        mapitem.getItem().setReward(new BossReward(chr.getInventory(MapleInventoryType.ETC).countById(4001886) + 1, 100100, 1, 1));
                    }
                }
                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster, false);
                removeItem(chr, mapitem, ob);
            } else {
                c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } finally {
            mapitem.getLock().unlock();
        }
    }

    public static final void Pickup_Pet(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (c.getPlayer().inPVP()) { //hack
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        final byte petz = (byte) slea.readInt();
        final MaplePet pet = chr.getPet(petz);
        slea.skip(1); // [4] Zero, [4] Seems to be tickcount, [1] Always zero
        slea.readInt();
        final Point Client_Reportedpos = slea.readPos();
        final MapleMapObject ob = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.ITEM);

        if (ob == null || pet == null) {
            return;
        }
        final MapleMapItem mapitem = (MapleMapItem) ob;
        try {
            mapitem.getLock().lock();
            if (mapitem.isPickedUp()) {
                //          c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                //        	c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (mapitem.getItemId() == 2431174 || mapitem.getItemId() == 2434021) { // 명예의 훈장.
                int rand = Randomizer.rand(10, 40);
                c.getPlayer().gainHonor(rand);
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
                //            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (mapitem.getItemId() == 2632800) { //네오 스톤
                int a = (int) c.getPlayer().getKeyValue(100711, "point") + 1;
                c.getPlayer().setKeyValue(100711, "point", a + "");
                c.getPlayer().updateInfoQuest(100711, "point=" + c.getPlayer().getKeyValue(100711, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";today=0;total=0;lock=0"); //네오 스톤
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem(chr, mapitem, ob);
                return;
            }
            if (mapitem.getItemId() == 2023484 || mapitem.getItemId() == 2023494
                    || mapitem.getItemId() == 2023495 || mapitem.getItemId() == 2023669) {
                if (mapitem.getDropper() instanceof MapleMonster) {
                    int bonus;
                    if (mapitem.getItemId() % 100 == 84) {
                        bonus = 1;
                    } else if (mapitem.getItemId() % 100 == 94) {
                        bonus = 1;
                    } else if (mapitem.getItemId() % 100 == 95) {
                        bonus = 1;
                    } else {
                        bonus = 1;
                    }

                    if (chr.getSkillLevel(20000297) > 0) {
                        bonus *= SkillFactory.getSkill(20000297).getEffect(chr.getSkillLevel(20000297)).getX();
                        bonus /= 100;
                    } else if (chr.getSkillLevel(80000370) > 0) {
                        bonus *= SkillFactory.getSkill(80000370).getEffect(chr.getSkillLevel(80000370)).getX();
                        bonus /= 100;
                    }
                    
                    bonus *= 30; 

                    MapleMonster mob = (MapleMonster) mapitem.getDropper();
                    chr.gainExp(mob.getMobExp() * c.getChannelServer().getExpRate() * bonus, true, true, false);
                    chr.getMap().broadcastMessage(CField.EffectPacket.showEffect(chr, 0, (int) mob.getMobExp() * c.getChannelServer().getExpRate() * bonus, 25, 0, 0, (byte) 0, true, null, "", null));
                }
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
                return;
            }
            if (mapitem.getItemId() == 2434851) {
                if (c.getPlayer().getBuffedValue(25121133)) {

                    List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> mbsvhZ = new ArrayList<>();

                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieDamR, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieDamR, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndiePad, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndiePad, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieBDR, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieBDR, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieIgnoreMobPdpR, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieIgnoreMobPdpR, 25121133)));
                    mbsvhZ.add(new Pair<>(MapleBuffStat.IndieBooster, c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IndieBooster, 25121133)));

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : mbsvhZ) {
                        if (mbsvh != null && mbsvh.right != null && mbsvh.right.schedule != null) {
                            mbsvh.right.schedule.cancel(false);
                            mbsvh.right.localDuration += 4000;

                            statups.put(mbsvh.left, new Pair<>(mbsvh.right.value, (int) c.getPlayer().getBuffLimit(mbsvh.right.effect.getSourceId())));

                            final CancelEffectAction cancelAction = new CancelEffectAction(c.getPlayer(), mbsvh.right.effect, System.currentTimeMillis(), mbsvh.left);
                            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                                cancelAction.run();
                            }, c.getPlayer().getBuffLimit(mbsvh.right.effect.getSourceId()));

                            mbsvh.right.schedule = schedule;

                        }
                    }
                    c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, c.getPlayer().getBuffedEffect(25121133), c.getPlayer()));
                }
                c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus(true));
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
                return;
            }
            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                //            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (!mapitem.isPlayerDrop() && mapitem.getDropType() == 1 && mapitem.getOwner() != chr.getId() && (chr.getParty() == null || chr.getParty().getMemberById(mapitem.getOwner()) == null)) {
                //            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            /*            final double Distance = Client_Reportedpos.distanceSq(mapitem.getPosition());
             if (Distance > 10000 && (mapitem.getMeso() > 0 || mapitem.getItemId() != 4001025)) {
             chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_CLIENT, String.valueOf(Distance));
             } else if (pet.getPos().distanceSq(mapitem.getPosition()) > 640000.0) {
             chr.getCheatTracker().registerOffense(CheatingOffense.PET_ITEMVAC_SERVER);

             }*/

            if (mapitem.getMeso() > 0) {
                chr.gainMeso(mapitem.getMeso(), true, false, true);
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
            } else if (MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItemId()) || mapitem.getItemId() / 10000 == 291) {
                //            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            } else if (useItem(c, mapitem.getItemId())) {
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
            } else if (mapitem.getItemId() == 2431850) {
                MapleItemInformationProvider.getInstance().getItemEffect(2002093).applyTo(chr, true);
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
            } else if (MapleInventoryManipulator.checkSpace(c, mapitem.getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                if (mapitem.getItem().getItemId() == 4001886) {
                    if (chr.getLastBossId() != 0) {
                        int party = chr.getParty() != null ? chr.getParty().getMembers().size() : 1;
                        int price = GameConstants.price.containsKey(chr.getLastBossId()) ? GameConstants.price.get(chr.getLastBossId()) : 1;
                        mapitem.getItem().setReward(new BossReward(chr.getInventory(MapleInventoryType.ETC).countById(4001886) + 1, chr.getLastBossId(), party, price * price * 80));
                        chr.setLastBossId(0);
                    } else {
                        mapitem.getItem().setReward(new BossReward(chr.getInventory(MapleInventoryType.ETC).countById(4001886) + 1, 100100, 1, 1));
                    }
                }
                MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), false, mapitem.getDropper() instanceof MapleMonster, true);
                /////////   c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                removeItem_Pet(chr, mapitem, petz, pet.getPetItemId());
                for (int id : GameConstants.showEffectDropItems) {
                    if (id == mapitem.getItemId()) {
                        c.getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, 0, 65, 0, 0, (byte) 0, true, null, null, mapitem.getItem()));
                        break;
                    }
                }
            }
        } finally {
            mapitem.getLock().unlock();
        }
    }

    public static final boolean useItem(final MapleClient c, final int id) {
        if (GameConstants.isUse(id)) { // TO prevent caching of everything, waste of mem
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleStatEffect eff = ii.getItemEffect(id);
            if (eff == null) {
                return false;
            }
            //must hack here for ctf
            if (id / 10000 == 291) {
                boolean area = false;
                for (Rectangle rect : c.getPlayer().getMap().getAreas()) {
                    if (rect.contains(c.getPlayer().getTruePosition())) {
                        area = true;
                        break;
                    }
                }
                if (!c.getPlayer().inPVP() || (c.getPlayer().getTeam() == (id - 2910000) && area)) {
                    return false; //dont apply the consume
                }
            }
            final int consumeval = eff.getConsume();

            if (consumeval > 0) {
                if (c.getPlayer().getMapId() == 109090300) {
                    for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                        if (chr != null && ((id == 2022163 && c.getPlayer().isCatched == chr.isCatched) || ((id == 2022165 || id == 2022166) && c.getPlayer().isCatched != chr.isCatched))) {
                            if (id == 2022163) {
                                ii.getItemEffect(id).applyTo(chr);
                            } else if (id == 2022166) {
                                chr.giveDebuff(MapleBuffStat.Stun, MobSkillFactory.getMobSkill(123, 1));
                            } else if (id == 2022165) {
                                chr.giveDebuff(MapleBuffStat.Slow, MobSkillFactory.getMobSkill(126, 1));
                            }
                        }
                    }
                    c.getSession().writeAndFlush(InfoPacket.getShowItemGain(id, (byte) -1, false));
//                    c.getPlayer().getClient().getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return true;
                }
                consumeItem(c, eff);
                consumeItem(c, ii.getItemEffectEX(id));
                c.getSession().writeAndFlush(InfoPacket.getShowItemGain(id, (byte) -1, false));
                return true;
            }
        }
        return false;
    }

    public static final void consumeItem(final MapleClient c, final MapleStatEffect eff) {
        if (eff == null) {
            return;
        }
        if (eff.getConsume() == 2) {
            if (c.getPlayer().getParty() != null && c.getPlayer().isAlive()) {
                for (final MaplePartyCharacter pc : c.getPlayer().getParty().getMembers()) {
                    final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(pc.getId());
                    if (chr != null && chr.isAlive()) {
                        eff.applyTo(chr, true);
                    }
                }
            } else {
                eff.applyTo(c.getPlayer(), true);
            }
        } else if (c.getPlayer().isAlive()) {
            eff.applyTo(c.getPlayer(), true);
        }
    }

    public static final void removeItem_Pet(final MapleCharacter chr, final MapleMapItem mapitem, int index, int id) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 5, chr.getId(), index));
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    public static final void removeItem(final MapleCharacter chr, final MapleMapItem mapitem, final MapleMapObject ob) {
        mapitem.setPickedUp(true);
        chr.getMap().broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 2, chr.getId()), mapitem.getPosition());
        chr.getMap().removeMapObject(mapitem);
        if (mapitem.isRandDrop()) {
            chr.getMap().spawnRandDrop();
        }
    }

    public static final void addMedalString(final MapleCharacter c, final StringBuilder sb) {
//        final Item medal = c.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -21);
        //      if (medal != null) { // Medal
        sb.append("<");
        /*            if (medal.getItemId() == 1142257 && GameConstants.isAdventurer(c.getJob())) {
         MapleQuestStatus stat = c.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
         if (stat != null && stat.getCustomData() != null) {
         sb.append(stat.getCustomData());
         sb.append("의 후계자");
         } else {
         sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
         }
         } else {
         sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
         }*/
        sb.append(LoginServer.getServerName());
        sb.append("> ");
//        }
    }

    public static final void OwlMinerva(final LittleEndianAccessor slea, final MapleClient c) {
        final short slot = slea.readShort();
        final int itemid = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0 && toUse.getItemId() == itemid && itemid == 2310000) {
            final int itemSearch = slea.readInt();
            final List<HiredMerchant> hms = c.getChannelServer().searchMerchant(itemSearch);
            if (hms.size() > 0) {
                c.getSession().writeAndFlush(CWvsContext.getOwlSearched(itemSearch, hms));
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemid, 1, true, false);
            } else {
                c.getPlayer().dropMessage(1, "아이템을 찾을 수 없습니다.");
            }
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void Owl(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
            c.getSession().writeAndFlush(CWvsContext.getOwlOpen());
        }
    }

    public static final int OWL_ID = 2; //don't change. 0 = owner ID, 1 = store ID, 2 = object ID

    public static final void OwlWarp(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().writeAndFlush(CWvsContext.getOwlMessage(4));
            return;
        } else if (c.getPlayer().getTrade() != null) {
            c.getSession().writeAndFlush(CWvsContext.getOwlMessage(7));
            return;
        }
        if (c.getPlayer().getMapId() >= 910000000 && c.getPlayer().getMapId() <= 910000022) {
            final int id = slea.readInt();
            slea.skip(1);
            final int map = slea.readInt();
            if (map >= 910000001 && map <= 910000022) {
                c.getSession().writeAndFlush(CWvsContext.getOwlMessage(0));
                final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(map);
                c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                HiredMerchant merchant = null;
                List<MapleMapObject> objects;
                switch (OWL_ID) {
                    case 0:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getOwnerId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case 1:
                        objects = mapp.getAllHiredMerchantsThreadsafe();
                        for (MapleMapObject ob : objects) {
                            if (ob instanceof IMaplePlayerShop) {
                                final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                                if (ips instanceof HiredMerchant) {
                                    final HiredMerchant merch = (HiredMerchant) ips;
                                    if (merch.getStoreId() == id) {
                                        merchant = merch;
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        final MapleMapObject ob = mapp.getMapObject(id, MapleMapObjectType.HIRED_MERCHANT);
                        if (ob instanceof IMaplePlayerShop) {
                            final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                            if (ips instanceof HiredMerchant) {
                                merchant = (HiredMerchant) ips;
                            }
                        }
                        break;
                }
                if (merchant != null) {
                    if (merchant.isOwner(c.getPlayer())) {
                        merchant.setOpen(false);
                        merchant.removeAllVisitors((byte) 16, (byte) 0);
                        c.getPlayer().setPlayerShop(merchant);
                        c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    } else if (!merchant.isOpen() || !merchant.isAvailable()) {
                        c.getPlayer().dropMessage(1, "The owner of the store is currently undergoing store maintenance. Please try again in a bit.");
                    } else if (merchant.getFreeSlot() == -1) {
                        c.getPlayer().dropMessage(1, "You can't enter the room due to full capacity.");
                    } else if (merchant.isInBlackList(c.getPlayer().getName())) {
                        c.getPlayer().dropMessage(1, "You may not enter this store.");
                    } else {
                        c.getPlayer().setPlayerShop(merchant);
                        merchant.addVisitor(c.getPlayer());
                        c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(c.getPlayer(), merchant, false));
                    }
                } else {
                    c.getPlayer().dropMessage(1, "The room is already closed.");
                }
            } else {
                c.getSession().writeAndFlush(CWvsContext.getOwlMessage(23));
            }
        } else {
            c.getSession().writeAndFlush(CWvsContext.getOwlMessage(23));
        }
    }

    public static final void TeleRock(LittleEndianAccessor slea, MapleClient c) {
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 10000 != 232) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        boolean used = UseTeleRock(slea, c, itemId);
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final boolean UseTeleRock(LittleEndianAccessor slea, MapleClient c, int itemId) {
        boolean used = false;
        if (slea.readByte() == 0) { // Rocktype
            final MapleMap target = c.getChannelServer().getMapFactory().getMap(slea.readInt());
            if (target != null && ((itemId == 5041000 && c.getPlayer().isRockMap(target.getId())) || ((itemId == 5040000 || itemId == 5040001) && c.getPlayer().isRegRockMap(target.getId())) || ((itemId == 5040004 || itemId == 5041001) && (c.getPlayer().isHyperRockMap(target.getId()) || GameConstants.isHyperTeleMap(target.getId()))))) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(target.getFieldLimit())) { //Makes sure this map doesn't have a forced return map
                    c.getPlayer().changeMap(target, target.getPortal(0));
                    used = true;
                } else {
                    c.getPlayer().dropMessage(1, "You cannot go to that place.");
                }
            } else {
                c.getPlayer().dropMessage(1, "You cannot go to that place.");
            }
        } else {
            final String name = slea.readMapleAsciiString();
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (victim != null && !victim.isIntern() && c.getPlayer().getEventInstance() == null && victim.getEventInstance() == null) {
                if (!FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(c.getChannelServer().getMapFactory().getMap(victim.getMapId()).getFieldLimit())) {
                    if (itemId == 5041000 || itemId == 5040004 || itemId == 5041001 || (victim.getMapId() / 100000000) == (c.getPlayer().getMapId() / 100000000)) { // Viprock or same continent
                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestPortal(victim.getTruePosition()));
                        used = true;
                    } else {
                        c.getPlayer().dropMessage(1, "You cannot go to that place.");
                    }
                } else {
                    c.getPlayer().dropMessage(1, "You cannot go to that place.");
                }
            } else {
                c.getPlayer().dropMessage(1, "(" + name + ") is currently difficult to locate, so the teleport will not take place.");
            }
        }
        return used;
    }

    public static void UsePetLoot(final LittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        short mode = slea.readShort();
        c.getPlayer().setPetLoot(mode == 1);
        for (int i = 0; i < c.getPlayer().getPets().length; ++i) {
            if (c.getPlayer().getPet(i) != null) {
                c.getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), c.getPlayer().getPet(i), c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) c.getPlayer().getPet(i).getInventoryPosition()), false, c.getPlayer().getPetLoot()));
            }
        }
        c.getSession().writeAndFlush(PetPacket.updatePetLootStatus(mode));
    }

    public static void SelectPQReward(final LittleEndianAccessor slea, final MapleClient c) {
        //byte select = slea.readByte();
        slea.skip(1); //상자 위치
        MapleMap map;
        int randval = RandomRewards.getRandomReward();
        short quantity = (short) Randomizer.rand(1, 10);
        MapleInventoryManipulator.addById(c, randval, quantity, "Reward item: " + randval + " on " + FileoutputUtil.CurrentReadable_Date());
        if (c.getPlayer().getMapId() == 100000203) {
            map = c.getChannelServer().getMapFactory().getMap(960000000);
            c.getPlayer().changeMap(map, map.getPortal(0)); //TODO :: 센드 패킷 못찾음.
        } else {
            c.getPlayer().fakeRelog();
        }
        c.getSession().writeAndFlush(EffectPacket.showCharmEffect(c.getPlayer(), randval, 1, true, ""));
    }

    public static void resetZeroWeapon(final MapleCharacter chr) {
        Equip newa = (Equip) MapleItemInformationProvider.getInstance().getEquipById(chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId());
        Equip newb = (Equip) MapleItemInformationProvider.getInstance().getEquipById(chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10).getItemId());
        ((Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11)).set(newa);
        ((Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10)).set(newb);
        chr.getClient().getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11)));
        chr.getClient().getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10)));
        chr.dropMessage(5, "제로의 장비는 파괴되는대신 처음 상태로 되돌아갑니다.");
    }

    public static void UseNameChangeCoupon(final LittleEndianAccessor slea, final MapleClient c) {
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().writeAndFlush(CWvsContext.nameChangeUI(false));
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        } else {
            c.setNameChangeEnable((byte) 1);
            MapleCharacter.updateNameChangeCoupon(c);
            c.getSession().writeAndFlush(CWvsContext.nameChangeUI(true));
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
    }

    public static void UseKaiserColorChange(final LittleEndianAccessor slea, final MapleClient c) {
        final short slot = slea.readShort();
        slea.skip(2);
        final int itemId = slea.readInt();
        final Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            return;
        }
        int[] colors = {
            841,
            842,
            843,
            758,
            291,
            317,
            338,
            339,
            444,
            445,
            446,
            458,
            461,
            447,
            450,
            454,
            455,
            456,
            457,
            459,
            460,
            462,
            463,
            464,
            289,
            4,
            34,
            35,
            64,
            9,
            10,
            12,
            11,
            16,
            17,
            22,
            24,
            53,
            61,
            62,
            63,
            67,
            68,
            109,
            110,
            111,
            112,
            113,
            114,
            115,
            116,
            117,
            121,
            125,
            128,
            129,
            145,
            150};
        if (itemId == 2350004) { // 메인컬러 변경권
            c.getPlayer().setKeyValue(12860, "extern", colors[Randomizer.nextInt(colors.length)] + "");
        } else if (itemId == 2350005) { // 서브컬러 변경권
            c.getPlayer().setKeyValue(12860, "inner", colors[Randomizer.nextInt(colors.length)] + "");
        } else if (itemId == 2350006) { // 프리미엄 블랙 변경권
            c.getPlayer().setKeyValue(12860, "extern", "842");
        } else if (itemId == 2350007) { // 초기화권
            c.getPlayer().setKeyValue(12860, "premium", "0");
            c.getPlayer().setKeyValue(12860, "inner", "0");
            c.getPlayer().setKeyValue(12860, "extern", "0");
        }
        if (c.getPlayer().getKeyValue(12860, "extern") == -1) {
            c.getPlayer().setKeyValue(12860, "extern", "0");
        }
        if (c.getPlayer().getKeyValue(12860, "inner") == -1) {
            c.getPlayer().setKeyValue(12860, "inner", "0");
        }
        if (c.getPlayer().getKeyValue(12860, "premium") == -1) {
            c.getPlayer().setKeyValue(12860, "premium", "0");
        }
        c.getPlayer().getMap().broadcastMessage(CField.KaiserChangeColor(c.getPlayer().getId(), c.getPlayer().getKeyValue(12860, "extern") == -1 ? 0 : (int) c.getPlayer().getKeyValue(12860, "extern"), c.getPlayer().getKeyValue(12860, "inner") == -1 ? 0 : (int) c.getPlayer().getKeyValue(12860, "inner"), c.getPlayer().getKeyValue(12860, "premium") == -1 ? 0 : (byte) c.getPlayer().getKeyValue(12860, "premium")));
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public final static void UseSoulEnchanter(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(4);
        short useslot = slea.readShort();
        short slot = slea.readShort();
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        Item equip;
        Item enchanter = useInventory.getItem(useslot);
        if (slot == (short) -11) {
            equip = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        } else {
            equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
        }
        Equip nEquip = (Equip) equip;
        nEquip.setSoulEnchanter((short) 9);
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, nEquip));
        chr.getMap().broadcastMessage(chr, CField.showEnchanterEffect(chr.getId(), (byte) 1), true);

        MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, enchanter.getItemId(), 1, true, false);
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public final static void UseSoulScroll(LittleEndianAccessor rh, MapleClient c, MapleCharacter chr) {
        rh.skip(4);
        short useslot = rh.readShort();
        short slot = rh.readShort();
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        Item equip;
        Item soul = useInventory.getItem(useslot);
        int soula = soul.getItemId() - 2590999;
        int soulid = soul.getItemId();
        boolean great = false;
        MapleDataProvider sourceData;
        sourceData = MapleDataProviderFactory.getDataProvider(new File("wz/Item.wz"));
        MapleData dd = sourceData.getData("SkillOption.img");
        int skillid = MapleDataTool.getIntConvert(dd.getChildByPath("skill/" + soula + "/skillId"));
        if (slot == (short) -11) {
            equip = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        } else {
            equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
        }
        if (slot == (short) -11) {
            chr.setSoulMP((Equip) equip);
        }
        if (dd.getChildByPath("skill/" + soula + "/tempOption/1/id") != null) {
            great = true;
        }
        short statid = 0;
        if (great) {
            statid = (short) (MapleDataTool.getIntConvert(dd.getChildByPath("skill/" + soula + "/tempOption/" + Randomizer.nextInt(7) + "/id")));
        } else {
            statid = (short) (MapleDataTool.getIntConvert(dd.getChildByPath("skill/" + soula + "/tempOption/0/id")));
        }
        Equip nEquip = (Equip) equip;

        if (SkillFactory.getSkill(nEquip.getSoulSkill()) != null) {
            chr.changeSkillLevel(nEquip.getSoulSkill(), (byte) -1, (byte) 0);
        }

        nEquip.setSoulName(GameConstants.getSoulName(soulid));
        nEquip.setSoulPotential(statid);
        nEquip.setSoulSkill(skillid);
        Equip zeros = null;
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            if (slot == -11) {
                zeros = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            } else if (slot == -10) {
                zeros = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            }
        }
        if (zeros != null) {

            if (SkillFactory.getSkill(zeros.getSoulSkill()) != null) {
                chr.changeSkillLevel(zeros.getSoulSkill(), (byte) -1, (byte) 0);
            }

            zeros.setSoulName(nEquip.getSoulName());
            zeros.setSoulPotential(nEquip.getSoulPotential());
            zeros.setSoulSkill(nEquip.getSoulSkill());
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, zeros));
        }
        chr.changeSkillLevel(skillid, (byte) 1, (byte) 1);
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, nEquip));
        chr.getMap().broadcastMessage(chr, CField.showSoulScrollEffect(chr.getId(), (byte) 1, false, nEquip), true);
        //useInventory.removeItem(useslot, (short) 1, false);
        MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, soulid, 1, true, false);
        c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static void UseCube(LittleEndianAccessor slea, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Item cube = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((slea.readShort()));
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            Equip eq = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            Equip eq2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            int rand = Randomizer.nextInt(100);
            boolean up = false;
            if (c.getPlayer().getMeso() < GameConstants.getCubeMeso(eq.getItemId())) {
                c.getPlayer().dropMessage(6, "메소가 부족합니다.");
                return;
            }
            if (eq.getState() == 17) {
                if (rand < 10) {
                    eq.setState((byte) 18);
                    eq2.setState((byte) 18);
                    up = true;
                } else {
                    eq.setState((byte) 17);
                    eq2.setState((byte) 17);
                }
            } else if (eq.getState() == 18) {
                if (rand < 5 && !ii.isCash(eq.getItemId()) && !GameConstants.isStrangeCube(cube.getItemId())) {
                    eq.setState((byte) 19);
                    eq2.setState((byte) 19);
                    up = true;
                } else {
                    eq.setState((byte) 18);
                    eq2.setState((byte) 18);
                }
            } else if (eq.getState() == 19) {
                if (rand < 3 && cube.getItemId() == 2711004) {
                    eq.setState((byte) 20);
                    eq2.setState((byte) 20);
                    up = true;
                } else {
                    eq.setState((byte) 19);
                    eq2.setState((byte) 19);
                }
            }
            int level = eq.getState() - 16;
            int potential1 = potential(eq.getItemId(), level);
            int potential2 = potential(eq.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1));
            int potential3 = eq.getPotential3() != 0 ? potential(eq.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1)) : 0;
            eq.setPotential1(potential1);
            eq.setPotential2(potential2);
            eq.setPotential3(potential3);
            eq2.setPotential1(potential1);
            eq2.setPotential2(potential2);
            eq2.setPotential3(potential3);
            c.getPlayer().gainMeso(-GameConstants.getCubeMeso(eq.getItemId()), false);
            c.getPlayer().removeItem(cube.getItemId(), -1);
            c.getSession().writeAndFlush(CField.showPotentialReset(c.getPlayer().getId(), true, cube.getItemId(), eq.getItemId()));
            //          c.getSession().writeAndFlush(CField.getCubeStart(c.getPlayer(), eq, up, cube.getItemId(), cube.getQuantity()));
            c.getPlayer().forceReAddItem(eq, MapleInventoryType.EQUIPPED);
            c.getPlayer().forceReAddItem(eq2, MapleInventoryType.EQUIPPED);
        } else {
            Equip eq = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((slea.readShort()));
            int rand = Randomizer.nextInt(100);
            boolean up = false;
            if (c.getPlayer().getMeso() < GameConstants.getCubeMeso(eq.getItemId())) {
                c.getPlayer().dropMessage(6, "메소가 부족합니다.");
                return;
            }
            if (eq.getState() == 17) {
                if (rand < 10) {
                    eq.setState((byte) 18);
                    up = true;
                } else {
                    eq.setState((byte) 17);
                }
            } else if (eq.getState() == 18) {
                if (rand < 5 && !ii.isCash(eq.getItemId()) && !GameConstants.isStrangeCube(cube.getItemId())) {
                    eq.setState((byte) 19);
                    up = true;
                } else {
                    eq.setState((byte) 18);
                }
            } else if (eq.getState() == 19) {
                if (rand < 3 && cube.getItemId() == 2711004) {
                    eq.setState((byte) 20);
                    up = true;
                } else {
                    eq.setState((byte) 19);
                }
            }
            int level = eq.getState() - 16;
            eq.setPotential1(potential(eq.getItemId(), level));
            eq.setPotential2(potential(eq.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 2)) ? level : (level - 1)));
            eq.setPotential3(eq.getPotential3() != 0 ? potential(eq.getItemId(), ((level == 1) || (Randomizer.nextInt(100) < 1)) ? level : (level - 1)) : 0);

            c.getPlayer().gainMeso(-GameConstants.getCubeMeso(eq.getItemId()), false);
            c.getPlayer().removeItem(cube.getItemId(), -1);
            c.getSession().writeAndFlush(CField.showPotentialReset(c.getPlayer().getId(), true, cube.getItemId(), eq.getItemId()));
            c.getSession().writeAndFlush(CField.getCubeStart(c.getPlayer(), eq, up, cube.getItemId(), cube.getQuantity()));
            c.getPlayer().forceReAddItem(eq, MapleInventoryType.EQUIP);
        }
    }

    public static void UseGoldenHammer(LittleEndianAccessor rh, MapleClient c) {
        c.getPlayer().vh = false;
        rh.skip(4); // Tick
        byte slot = (byte) rh.readInt();
        int itemId = rh.readInt();
        rh.skip(4);
        byte victimslot = (byte) rh.readInt();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        Equip victim = null;
        Equip victim_ = null;
        if (victimslot < 0) {
            victim = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(victimslot);
            if (GameConstants.isZero(c.getPlayer().getJob())) {
                victim_ = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            }
        } else {
            victim = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(victimslot);
        }
        if (victim == null || toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        c.getSession().writeAndFlush(CSPacket.ViciousHammer(true, c.getPlayer().vh));
        victim.setViciousHammer((byte) 1);
        if (victim_ != null) {
            victim_.setViciousHammer((byte) 1);
        }

        if ((itemId == 2470001 || itemId == 2470002) && Randomizer.nextInt(100) > 50) {
            victim.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            if (victim_ != null) {
                victim_.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            }
            c.getPlayer().vh = true;
        } else if (itemId == 2470000) {
            victim.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            if (victim_ != null) {
                victim_.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            }
            c.getPlayer().vh = true;
        } else if (itemId == 2470003) {
            victim.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            if (victim_ != null) {
                victim_.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            }
            c.getPlayer().vh = true;
        } else if (itemId == 2470007) {
            victim.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            if (victim_ != null) {
                victim_.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            }
            c.getPlayer().vh = true;
        } else if (itemId == 2470010) {
            victim.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            if (victim_ != null) {
                victim_.setUpgradeSlots((byte) (victim.getUpgradeSlots() + 1));
            }
            c.getPlayer().vh = true;
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (byte) slot, (short) 1, false);
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, victim));
        if (victim_ != null) {
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, victim_));
        }
    }

    public static void Todd(LittleEndianAccessor slea, MapleClient c) {
        /*        short type = slea.readShort();
         Equip desc = null, sort = null;
         switch (type) {
         case 0: {
         short pos1 = slea.readShort();
         short pos2 = slea.readShort();
         if (pos1 < 0) {
         desc = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos1);
         } else {
         desc = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos1);
         }
         if (pos2 < 0) {
         sort = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos2);
         } else {
         sort = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos2);
         }
         EquipmentEnchant.AddTodd(c, desc, sort, slea.readShort());
         break;
         }
         case 1: {
         short pos1 = slea.readShort();
         short pos2 = slea.readShort();
         if (pos1 < 0) {
         desc = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos1);
         } else {
         desc = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos1);
         }
         if (pos2 < 0) {
         sort = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos2);
         } else {
         sort = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos2);
         }
         EquipmentEnchant.ToddResult(c, desc, sort, slea.readShort());
         break;
         }
         case 2: {
         Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slea.readShort());
         EquipmentEnchant.ToddItemScroll(c, item);
         break;
         }
         }*/
    }

    public static void returnScrollResult(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        byte type = slea.readByte();
        Equip equip = null;
        Equip zeroequip = null;

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (c.getPlayer().returnscroll == null) {
            c.getPlayer().dropMessage(1, "리턴 스크롤 사용 중 오류가 발생하였습니다.");
            return;
        }

        if (c.getPlayer().returnscroll.getPosition() > 0) {
            equip = (Equip) (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(c.getPlayer().returnscroll.getPosition()));
        } else {
            equip = (Equip) (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(c.getPlayer().returnscroll.getPosition()));
        }

        if (equip == null) {
            c.getPlayer().dropMessage(1, "리턴 스크롤 사용 중 오류가 발생하였습니다.");
            return;
        }

        if (type == 1) { //되돌림 선택
            if (c.getPlayer().returnscroll.getPosition() > 0) {
                equip.set(c.getPlayer().returnscroll);
            } else {
                equip.set(c.getPlayer().returnscroll);
            }
        }

        equip.setFlag(equip.getFlag() - ItemFlag.RETURN_SCROLL.getValue());

        if (GameConstants.isZeroWeapon(c.getPlayer().returnscroll.getItemId())) {
            zeroequip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            zeroequip.set(c.getPlayer().returnscroll);

            zeroequip.setFlag(equip.getFlag() - ItemFlag.RETURN_SCROLL.getValue());
        }
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, equip));
        if (zeroequip != null) {
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, zeroequip));
        }

        if (type == 1) {
            c.getSession().writeAndFlush(CField.getGameMessage(11, "리턴 주문서의 힘으로 " + ii.getName(equip.getItemId()) + "가 " + ii.getName(c.getPlayer().returnSc) + " 사용 이전 상태로 돌아왔습니다."));
        } else {
            c.getSession().writeAndFlush(CField.getGameMessage(11, "리턴 주문서의 효과가 사라졌습니다."));
        }
        c.getSession().writeAndFlush(CWvsContext.returnEffectModify(null, 0));

        c.getPlayer().returnscroll = null;
        c.getPlayer().returnSc = 0;
    }

    public static void ArcaneCatalyst(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int slot = slea.readInt();
        Equip equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slot).copy();
        equip.setEquipmentType(equip.getEquipmentType() | 0x4000);
        equip.setArcLevel(1);
        int totalexp = 0;
        for (int i = 1; i < equip.getArcLevel(); i++) {
            totalexp += GameConstants.ArcaneNextUpgrade(i);
        }
        totalexp += equip.getArcEXP();

        equip.setArcEXP((int) Math.floor(totalexp * 0.8));

        if (GameConstants.isXenon(c.getPlayer().getJob())) {
            equip.setStr((short) 117);
            equip.setDex((short) 117);
            equip.setLuk((short) 117);
        } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
            equip.setHp((short) 4200);
        } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
            equip.setStr((short) 300);
        } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
            equip.setInt((short) 300);
        } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
            equip.setDex((short) 300);
        } else if (GameConstants.isThief(c.getPlayer().getJob())) {
            equip.setLuk((short) 300);
        } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
            equip.setStr((short) 300);
        }

        c.getSession().writeAndFlush(CWvsContext.ArcaneCatalyst(equip, slot));
    }

    public static void ArcaneCatalyst2(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int slot = slea.readInt();
        Equip equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slot);
        if ((equip.getEquipmentType() & 0x4000) == 0) {
            equip.setArc((short) 30);
            int totalexp = 0;
            for (int i = 1; i < equip.getArcLevel(); i++) {
                totalexp += GameConstants.ArcaneNextUpgrade(i);
            }
            totalexp += equip.getArcEXP();
            equip.setArcEXP((int) Math.floor(totalexp * 0.8));
            equip.setArcLevel((byte) 1);
            if (GameConstants.isXenon(c.getPlayer().getJob())) {
                equip.setStr((short) 117);
                equip.setDex((short) 117);
                equip.setLuk((short) 117);
            } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                equip.setHp((short) 4200);
            } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                equip.setStr((short) 300);
            } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                equip.setInt((short) 300);
            } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                equip.setDex((short) 300);
            } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                equip.setLuk((short) 300);
            } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                equip.setStr((short) 300);
            }
            equip.setEquipmentType(equip.getEquipmentType() | 0x4000);
        } else {
            equip.setEquipmentType(equip.getEquipmentType() - 0x4000);
        }
        c.getSession().writeAndFlush(CWvsContext.ArcaneCatalyst2(equip));
        c.getPlayer().removeItem(2535000, -1);
        c.getPlayer().forceReAddItem(equip, MapleInventoryType.EQUIP);
    }

    public static void ArcaneCatalyst3(LittleEndianAccessor slea, MapleClient c) {
        int slot = slea.readInt();
        Equip equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slot).copy();
        equip.setEquipmentType(equip.getEquipmentType() - 0x4000);

        c.getSession().writeAndFlush(CWvsContext.ArcaneCatalyst(equip, slot));
    }

    public static void ArcaneCatalyst4(LittleEndianAccessor slea, MapleClient c) {
        int slot = slea.readInt();
        Equip equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slot);
        equip.setEquipmentType(equip.getEquipmentType() - 0x4000);

        c.getSession().writeAndFlush(CWvsContext.ArcaneCatalyst2(equip));
        c.getPlayer().forceReAddItem(equip, MapleInventoryType.EQUIP);
    }

    public static void ReturnSynthesizing(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int scrollId = slea.readInt();
        slea.skip(4);
        int eqpId = slea.readInt();
        int eqpslot = slea.readInt();
        Equip equip = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) eqpslot);
        if (equip.getItemId() == eqpId) {
            equip.setMoru(0);
            c.getPlayer().forceReAddItem(equip, MapleInventoryType.EQUIP);
            StringBuilder msg = new StringBuilder("[");
            msg.append(MapleItemInformationProvider.getInstance().getName(equip.getItemId()));
            msg.append("]의 외형이 원래대로 복구되었습니다.");
            c.getSession().writeAndFlush(CWvsContext.showPopupMessage(msg.toString()));
            c.getPlayer().gainItem(scrollId, -1);
        }
    }

    public static void blackRebirthResult(LittleEndianAccessor slea, MapleClient c) {
        int result = slea.readInt();

        // 0 : 기본, 1 : before, 2 : after, 3 : 한번 더
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        Equip eq = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(c.getPlayer().blackRebirthScroll.getPosition());
        if (result == 2) {
            eq.resetRebirth(ii.getReqLevel(eq.getItemId()));

            int[] rebirth = new int[4];
            String fire = String.valueOf(c.getPlayer().blackRebirth);

            Equip ordinary = (Equip) MapleItemInformationProvider.getInstance().getEquipById(eq.getItemId(), false);

            int ordinaryPad = ordinary.getWatk() > 0 ? ordinary.getWatk() : ordinary.getMatk();
            int ordinaryMad = ordinary.getMatk() > 0 ? ordinary.getMatk() : ordinary.getWatk();

            if (fire.length() == 12) {
                rebirth[0] = Integer.parseInt(fire.substring(0, 3));
                rebirth[1] = Integer.parseInt(fire.substring(3, 6));
                rebirth[2] = Integer.parseInt(fire.substring(6, 9));
                rebirth[3] = Integer.parseInt(fire.substring(9));
            } else if (fire.length() == 11) {
                rebirth[0] = Integer.parseInt(fire.substring(0, 2));
                rebirth[1] = Integer.parseInt(fire.substring(2, 5));
                rebirth[2] = Integer.parseInt(fire.substring(5, 8));
                rebirth[3] = Integer.parseInt(fire.substring(8));
            } else if (fire.length() == 10) {
                rebirth[0] = Integer.parseInt(fire.substring(0, 1));
                rebirth[1] = Integer.parseInt(fire.substring(1, 4));
                rebirth[2] = Integer.parseInt(fire.substring(4, 7));
                rebirth[3] = Integer.parseInt(fire.substring(7));
            }

            eq.setFire(c.getPlayer().blackRebirth);

            for (int i = 0; i < rebirth.length; ++i) {
                int value = rebirth[i] - (rebirth[i] / 10 * 10);

                eq.setFireOption(rebirth[i] / 10, ii.getReqLevel(eq.getItemId()), value, ordinaryPad, ordinaryMad); // value
            }

            c.getPlayer().forceReAddItem(eq, MapleInventoryType.EQUIP);
        } else if (result == 3) {

            Item scroll = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(c.getPlayer().blackRebirthPos);

            if (scroll != null) {
                long newRebirth = eq.newRebirth(ii.getReqLevel(eq.getItemId()), scroll.getItemId(), false);
                c.getSession().writeAndFlush(CWvsContext.useBlackRebirthScroll(eq, scroll, newRebirth, false));
                MapleInventoryManipulator.removeFromSlot(c, GameConstants.getInventoryType(scroll.getItemId()), scroll.getPosition(), (short) 1, false, false);

                //before
                c.getSession().writeAndFlush(CWvsContext.blackRebirthResult(true, eq.getFire(), eq));

                //after
                c.getSession().writeAndFlush(CWvsContext.blackRebirthResult(false, newRebirth, eq));

                c.getPlayer().blackRebirth = newRebirth;

                c.getPlayer().blackRebirthScroll = (Equip) eq.copy();
            }
        }

        if (result == 1 || result == 2) {
            c.getSession().writeAndFlush(CWvsContext.useBlackRebirthScroll(eq, null, 0, true));
        }
    }

    public static void UseCirculator(LittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        int slot = slea.readInt();
        Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem((short) slot);
        if (item.getItemId() == itemId) {

            List<InnerSkillValueHolder> newValues = new LinkedList<InnerSkillValueHolder>();
            InnerSkillValueHolder ivholder = null;
            InnerSkillValueHolder ivholder2 = null;
            int nowrank = -1;
            switch (itemId) {
                case 2702003: // 카오스 서큘레이터
                case 2702004: // 카오스 서큘레이터
                    if (c.getPlayer().getInnerSkills().size() > 0)
                        nowrank = c.getPlayer().getInnerSkills().get(0).getRank();

                    for (InnerSkillValueHolder isvh : c.getPlayer().getInnerSkills()) {
                        newValues.add(InnerAbillity.getInstance().renewLevel(nowrank, isvh.getSkillId()));
                        c.getPlayer().changeSkillLevel_Inner(SkillFactory.getSkill(isvh.getSkillId()), (byte) 0, (byte) 0);
                    }
                    break;
                case 2702006: // 레전드리 서큘레이터
                    nowrank = 3;
                    for (InnerSkillValueHolder isvh : c.getPlayer().getInnerSkills()) {
                        if (ivholder == null) {
                            ivholder = InnerAbillity.getInstance().renewSkill(nowrank, false);
                            while (isvh.getSkillId() == ivholder.getSkillId()) {
                                ivholder = InnerAbillity.getInstance().renewSkill(nowrank, false);
                            }
                            newValues.add(ivholder);
                        } else if (ivholder2 == null) {
                            ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                            while (isvh.getSkillId() == ivholder2.getSkillId() || ivholder.getSkillId() == ivholder2.getSkillId()) {
                                ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                            }
                            newValues.add(ivholder2);
                        } else {
                            InnerSkillValueHolder ivholder3 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                            while (isvh.getSkillId() == ivholder3.getSkillId() || ivholder.getSkillId() == ivholder3.getSkillId() || ivholder2.getSkillId() == ivholder3.getSkillId()) {
                                ivholder3 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                            }
                            newValues.add(ivholder3);
                        }
                        c.getPlayer().changeSkillLevel_Inner(SkillFactory.getSkill(isvh.getSkillId()), (byte) 0, (byte) 0);
                    }
                    break;
            }
            if (newValues.size() == 3) {
                c.getPlayer().getInnerSkills().clear();
                for (InnerSkillValueHolder isvh : newValues) {
                    c.getPlayer().getInnerSkills().add(isvh);
                    c.getPlayer().changeSkillLevel_Inner(SkillFactory.getSkill(isvh.getSkillId()), isvh.getSkillLevel(), isvh.getSkillLevel());
                    c.getPlayer().getClient().getSession().writeAndFlush(CField.updateInnerAbility(isvh, c.getPlayer().getInnerSkills().size(), c.getPlayer().getInnerSkills().size() == 3));
                }
                c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, (short) slot, (short) 1, false);
            }
        }
    }

}
