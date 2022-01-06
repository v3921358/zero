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
import constants.GameConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.StructSetItem;
import server.StructSetItem.SetItem;
import server.maps.FieldLimitType;
import server.movement.LifeMovementFragment;
import tools.data.LittleEndianAccessor;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.PetPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PetHandler {

    public static void SpawnPet(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(4);
        byte slot = slea.readByte();
        slea.readByte();
        Item item = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        if (ItemFlag.KARMA_USE.check(item.getFlag())) {
            item.setFlag(item.getFlag() - ItemFlag.KARMA_USE.getValue());
        }

        MaplePet pet = item.getPet();
        if (pet != null) {
            if (chr.getPetIndex(pet) != -1) { //
                chr.unequipPet(pet, false, false);

                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                updatePetSkills(chr, pet);

                return;
            }
            if (item.getExpiration() > System.currentTimeMillis()) {
                Point pos = chr.getPosition();
                pet.setPos(pos);

                if (chr.getMap().getFootholds() != null) {
                    if (chr.getMap().getFootholds().findBelow(pet.getPos()) != null) {
                        pet.setFh(chr.getMap().getFootholds().findBelow(pet.getPos()).getId());
                    }
                }

                pet.setStance(0);
                chr.addPet(pet);
                chr.getMap().broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), true);
                c.getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), pet, c.getPlayer().getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
                //  c.getSession().writeAndFlush(PetPacket.petExceptionList(c.getPlayer(), pet));

                updatePetSkills(chr, null);
            } else {
                c.getPlayer().getInventory(MapleInventoryType.CASH).removeItem(slot);
                c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(MapleInventoryType.CASH, slot, false));
            }
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static final void Pet_AutoPotion(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(1);
        slea.readInt();
        final short slot = slea.readShort();
        if (chr == null || !chr.isAlive() || chr.getBuffedEffect(MapleBuffStat.DebuffIncHp) != null || chr.getMap() == null || chr.hasDisease(MapleBuffStat.StopPortion)) {
            return;
        }
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != slea.readInt() || chr.getBuffedEffect(MapleBuffStat.Reincarnation) != null) {
//            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
//            chr.dropMessage(5, "You may not use this item yet.");
//            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr, true)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
        } else {
//            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        }
    }

    public static final void PetChat(final int petid, final short command, final String text, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getPet(petid) == null) {
            return;
        }
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), command, text, (byte) petid), true);
    }

    public static final void PetCommand(final MaplePet pet, final PetCommand petCommand, final MapleClient c, final MapleCharacter chr) {
        if (petCommand == null) {
            return;
        }
        byte petIndex = (byte) chr.getPetIndex(pet);
        boolean success = false;
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + (petCommand.getIncrease() * c.getChannelServer().getTraitRate());
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().writeAndFlush(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), true));
                    chr.getMap().broadcastMessage(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), false));
                }
                c.getSession().writeAndFlush(PetPacket.updatePet(chr, pet, chr.getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, chr.getPetLoot()));
            }
        }
        chr.getMap().broadcastMessage(PetPacket.commandResponse(chr.getId(), (byte) petCommand.getSkillId(), petIndex, success));
    }

    public static void PetFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        int previousFullness = 100;
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));

        for (MaplePet pet : chr.getPets()) {
            if (pet == null) {
                continue;
            }
            if (pet.getFullness() < previousFullness) {
                previousFullness = pet.getFullness();

                slea.skip(6);
                int itemId = slea.readInt();

                boolean gainCloseness = false;

                if (Randomizer.nextInt(99) <= 50) {
                    gainCloseness = true;
                }
                if (pet.getFullness() < 100) {
                    int newFullness = pet.getFullness() + 30;
                    if (newFullness > 100) {
                        newFullness = 100;
                    }
                    pet.setFullness(newFullness);
                    int index = chr.getPetIndex(pet);

                    if (gainCloseness && pet.getCloseness() < 30000) {
                        int newCloseness = pet.getCloseness() + 1;
                        if (newCloseness > 30000) {
                            newCloseness = 30000;
                        }
                        pet.setCloseness(newCloseness);
                        if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                            pet.setLevel(pet.getLevel() + 1);

                            c.getSession().writeAndFlush(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), true));
                            chr.getMap().broadcastMessage(EffectPacket.showPetLevelUpEffect(c.getPlayer(), pet.getPetItemId(), false));
                        }
                    }
                    c.getSession().writeAndFlush(PetPacket.updatePet(chr, pet, chr.getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, chr.getPetLoot()));
                    chr.getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(chr.getId(), (byte) 1, (byte) index, true), true);
                } else {
                    if (gainCloseness) {
                        int newCloseness = pet.getCloseness() - 1;
                        if (newCloseness < 0) {
                            newCloseness = 0;
                        }
                        pet.setCloseness(newCloseness);
                        if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                            pet.setLevel(pet.getLevel() - 1);
                        }
                    }
                    c.getSession().writeAndFlush(PetPacket.updatePet(chr, pet, chr.getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, chr.getPetLoot()));
                    chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, (byte) chr.getPetIndex(pet), false), true);
                }
                MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, true, false);
                return;
            }
        }
    }

    public static final void MovePet(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int petId = slea.readInt();
        slea.skip(13); // 5 + pos(4) + 4
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);

        if (res != null && chr != null && res.size() != 0 && chr.getMap() != null) { // map crash hack
            final MaplePet pet = chr.getPet(petId);
            if (pet == null) {
                return;
            }
            pet.updatePosition(res);
            chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), pet.getUniqueId(), (byte) petId, res, pet.getPos()), false);
        }
    }

    public static void ChangePetBuff(final LittleEndianAccessor slea, final MapleCharacter chr) {
        int petindex = slea.readInt();
        int skillId = slea.readInt();
        int mode = slea.readByte();
        MaplePet pet = chr.getPet(petindex);
        if (pet == null) {
            chr.dropMessage(1, "펫이 존재하지 않습니다.");
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        if (mode == 0) { //register
            pet.setBuffSkillId(skillId);
        }
        chr.getClient().getSession().writeAndFlush(PetPacket.updatePet(chr, pet, chr.getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, chr.getPetLoot()));
    }

    public static void petExceptionList(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int petindex = slea.readInt();
        byte size = slea.readByte();

        MaplePet pet = chr.getPet(petindex);
        if (pet == null) {
            chr.dropMessage(1, "펫이 존재하지 않습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }

        chr.setExceptionList(new ArrayList<>());

        for (int i = 0; i < size; ++i) {
            chr.getExceptionList().add(slea.readInt());
        }

        c.getSession().writeAndFlush(PetPacket.petExceptionList(chr, pet));
    }

    public static void updatePetSkills(MapleCharacter player, MaplePet unequip) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        final Map<Skill, SkillEntry> newL = new HashMap<>();
        List<Integer> petItemIds = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            if (player.getPet(i) != null) {
                petItemIds.add(player.getPet(i).getPetItemId());
            }
        }

        if (unequip != null) {
            int level = 0;
            StructSetItem setItem = ii.getSetItem(ii.getSetItemID(unequip.getPetItemId()));
            for (int petId : petItemIds) {
                if (ii.getSetItemID(petId) == ii.getSetItemID(unequip.getPetItemId())) {
                    level++;
                }
            }

            if (setItem != null) {
                for (Entry<Integer, SetItem> set : setItem.items.entrySet()) {
                    if (set.getKey() <= level) {
                        for (Entry<Integer, Byte> skill : set.getValue().activeSkills.entrySet()) {
                            newL.put(SkillFactory.getSkill(skill.getKey()), new SkillEntry(skill.getValue(), (byte) SkillFactory.getSkill(skill.getKey()).getMasterLevel(), -1L));
                        }
                    } else {
                        for (Entry<Integer, Byte> skill : set.getValue().activeSkills.entrySet()) {
                            newL.put(SkillFactory.getSkill(skill.getKey()), new SkillEntry(-1, (byte) 0, -1L));
                        }
                    }
                }
            }
        } else {
            for (int petId : petItemIds) {
                int level = 0;
                StructSetItem setItem = ii.getSetItem(ii.getSetItemID(petId));
                if (setItem != null) {
                    for (int setItemId : setItem.itemIDs) {
                        if (petItemIds.contains(setItemId)) {
                            level++;
                        }
                    }

                    for (Entry<Integer, SetItem> set : setItem.items.entrySet()) {
                        if (set.getKey() <= level) {
                            for (Entry<Integer, Byte> skill : set.getValue().activeSkills.entrySet()) {
                                newL.put(SkillFactory.getSkill(skill.getKey()), new SkillEntry(skill.getValue(), (byte) SkillFactory.getSkill(skill.getKey()).getMasterLevel(), -1L));
                            }
                        } else {
                            for (Entry<Integer, Byte> skill : set.getValue().activeSkills.entrySet()) {
                                newL.put(SkillFactory.getSkill(skill.getKey()), new SkillEntry(-1, (byte) 0, -1L));
                            }
                        }
                    }
                }
            }
        }

        if (!newL.isEmpty()) {
            player.getClient().getSession().writeAndFlush(CWvsContext.updateSkills(newL));
        }
    }
}
