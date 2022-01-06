package tools.packet;

import client.BuddylistEntry;
import client.Core;
import client.InnerSkillValueHolder;
import client.MapleBuffStat;
import client.MapleBuffStatValueHolder;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleMannequin;
import client.MapleQuestStatus;
import client.MapleStat;
import client.MapleTrait;
import client.MapleTrait.MapleTraitType;
import client.MapleUnion;
import client.MatrixSkill;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;
import client.inventory.AuctionItem;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleRing;
import constants.GameConstants;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.channel.MapleGuildRanking;
import handling.channel.MapleGuildRanking.GuildRankingInfo;
import handling.channel.handler.InventoryHandler;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildCharacter;
import handling.world.guild.MapleGuildSkill;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.enchant.EnchantFlag;
import server.enchant.EquipmentEnchant;
import server.enchant.EquipmentScroll;
import server.enchant.StarForceStats;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.PlayerNPC;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageManager;
import server.shops.HiredMerchant;
import server.shops.MaplePlayerShopItem;
import tools.HexTool;
import tools.Pair;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;

public class CWvsContext {

    public static class InventoryPacket {

        public static byte[] addInventorySlot(final MapleInventoryType type, final Item item) {
            return addInventorySlot(type, item, false);
        }

        public static byte[] addInventorySlot(final MapleInventoryType type, final Item item, final boolean fromDrop) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(fromDrop);
            mplew.writeInt(1);
            mplew.write(0);
            mplew.write(GameConstants.isInBag(item.getPosition(), type.getType()) ? 9 : 0);
            mplew.write(Math.max(1, type.getType()));
            mplew.writeShort(item.getPosition());
            PacketHelper.addItemInfo(mplew, item);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] updateInventoryItem(boolean fromDrop, MapleInventoryType type, Item item) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(fromDrop);
            mplew.writeInt(2);
            mplew.write(0);
            mplew.write(3);
            mplew.write(Math.max(1, type.getType()));
            mplew.writeShort(item.getPosition());
            mplew.write(0);
            mplew.write(Math.max(1, type.getType()));
            mplew.writeShort(item.getPosition());
            PacketHelper.addItemInfo(mplew, item);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] updateInventorySlot(final MapleInventoryType type, final Item item, final boolean fromDrop) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(fromDrop);
            mplew.writeInt(1);
            mplew.write(0);
            mplew.write(GameConstants.isInBag(item.getPosition(), type.getType()) ? 6 : 1);
            mplew.write(Math.max(1, type.getType()));
            mplew.writeShort(item.getPosition());
            mplew.writeShort(item.getQuantity());
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] moveInventoryItem(final MapleInventoryType type, List<Pair<Short, Short>> updateSlots) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(1);
            mplew.writeInt(updateSlots.size());
            mplew.write(0);

            for (Pair<Short, Short> updateSlot : updateSlots) {
                mplew.write(2);
                mplew.write(Math.max(1, type.getType()));
                mplew.writeShort(updateSlot.left);
                mplew.writeShort(updateSlot.right);
            }

            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] moveInventoryItem(final MapleInventoryType type, final short source, final short target, final boolean bag, final boolean bothBag) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(1);
            mplew.writeInt(1);
            mplew.write(0);
            mplew.write(bag ? (bothBag ? 8 : 5) : 2);
            mplew.write(Math.max(1, type.getType()));
            mplew.writeShort(source);

            if (bag && !bothBag) {
                mplew.writeInt(target);
            } else {
                mplew.writeShort(target);
            }

            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] scrolledItem(Item scroll, Item item, boolean destroyed, boolean potential, boolean equipped) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(1);
            mplew.writeInt(3);
            mplew.write(0);

            if (scroll == null) {
                mplew.write(item.getQuantity() > 0 ? 1 : 3);
                mplew.write(GameConstants.getInventoryType(item.getItemId()).getType());
                mplew.writeShort(item.getPosition());
                if (item.getQuantity() > 0) {
                    mplew.writeShort(item.getQuantity());
                }
            } else {
                mplew.write(scroll.getQuantity() > 0 ? 1 : 3);
                mplew.write(GameConstants.getInventoryType(scroll.getItemId()).getType());
                mplew.writeShort(scroll.getPosition());
                if (scroll.getQuantity() > 0) {
                    mplew.writeShort(scroll.getQuantity());
                }
            }

            mplew.write(3);
            mplew.write(1);
            mplew.writeShort(item.getPosition());

            if (destroyed) {
                mplew.write(3);
                mplew.write(MapleInventoryType.EQUIP.getType());
                mplew.writeShort(item.getPosition());
                mplew.write(0);
            } else if (scroll != null && ((scroll.getItemId() >= 2048000 && scroll.getItemId() <= 2048099) || (scroll.getItemId() >= 2048800 && scroll.getItemId() <= 2048837))) {
                mplew.write(0);
                mplew.write(MapleInventoryType.DECORATION.getType());
                mplew.writeShort(item.getPosition());
                PacketHelper.addItemInfo(mplew, item, null);
            } else {
                mplew.write(0);
                mplew.write(MapleInventoryType.EQUIP.getType());
                mplew.writeShort(item.getPosition());
                PacketHelper.addItemInfo(mplew, item, null);
            }

            mplew.write(0);

            if (item.getPosition() < 0) {
                mplew.write(1);
            }

            return mplew.getPacket();
        }

        public static byte[] updateScrollandItem(Item scroll, Item item) {
            return updateScrollandItem(scroll, item, false);
        }

        public static byte[] updateScrollandItem(Item scroll, Item item, boolean destroyed) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(false);
            mplew.writeInt(2);
            mplew.write(0);

            if (scroll.getQuantity() > 0) {
                mplew.write(1);
                mplew.write(GameConstants.getInventoryType(scroll.getItemId()).getType());
                mplew.writeShort(scroll.getPosition());
                mplew.writeShort(scroll.getQuantity());
            } else {
                mplew.write(3);
                mplew.write(GameConstants.getInventoryType(scroll.getItemId()).getType());
                mplew.writeShort(scroll.getPosition());
            }

            if (!destroyed) {
                mplew.write(0);
                mplew.write(GameConstants.getInventoryType(item.getItemId()).getType());
                mplew.writeShort(item.getPosition());
                PacketHelper.addItemInfo(mplew, item);
            } else {
                mplew.write(3);
                mplew.write(GameConstants.getInventoryType(item.getItemId()).getType());
                mplew.writeShort(item.getPosition());
            }

            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] moveAndCombineItem(Item src, Item dst) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(1);
            mplew.writeInt(2);
            mplew.write(0);
            mplew.write(3);
            mplew.write(GameConstants.getInventoryType(src.getItemId()).getType());
            mplew.writeShort(src.getPosition());
            mplew.write(1);
            mplew.write(GameConstants.getInventoryType(dst.getItemId()).getType());
            mplew.writeShort(dst.getPosition());
            mplew.writeShort(dst.getQuantity());
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] moveAndCombineWithRestItem(Item src, Item dst) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(1);
            mplew.writeInt(2);
            mplew.write(0);
            mplew.write(1);
            mplew.write(GameConstants.getInventoryType(src.getItemId()).getType());
            mplew.writeShort(src.getPosition());
            mplew.writeShort(src.getQuantity());
            mplew.write(1);
            mplew.write(GameConstants.getInventoryType(dst.getItemId()).getType());
            mplew.writeShort(dst.getPosition());
            mplew.writeShort(dst.getQuantity());
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] clearInventoryItem(final MapleInventoryType type, final short slot, final boolean fromDrop) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(fromDrop ? 1 : 0);
            mplew.writeInt(1);
            mplew.write(0);
            mplew.write(3);
            mplew.write(Math.max(1, type.getType()));
            mplew.writeShort(slot);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] getInventoryFull() {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] getInventoryStatus(boolean fromDrop) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
            mplew.write(fromDrop ? 1 : 0);
            mplew.writeInt(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] getSlotUpdate(final byte invType, final byte newSlots) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.INVENTORY_GROW.getValue());
            mplew.write(invType);
            mplew.write(newSlots);

            return mplew.getPacket();
        }

        public static byte[] getShowInventoryFull() {
            return InfoPacket.getShowInventoryStatus(0xFF);
        }

        public static byte[] showItemUnavailable() {
            return InfoPacket.getShowInventoryStatus(0xFE);
        }
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="BuffPacket">

    public static class BuffPacket {

        public static byte[] checkSunfireSkill(int gauge) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUMINOUS_MORPH.getValue());
            mplew.writeInt(Math.min(gauge, 9999));
            mplew.write(gauge <= 1 ? 1 : 2); // 이퀄리브리엄
            return mplew.getPacket();
        }

        public static byte[] checkEclipseSkill(int gauge) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUMINOUS_MORPH.getValue());
            mplew.writeInt(Math.min(gauge, 9999));
            mplew.write(gauge >= 9999 ? 2 : 1); // 이퀄리브리엄
            return mplew.getPacket();
        }

        public static byte[] LuminusMorph(int gauge, boolean islight) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LUMINOUS_MORPH.getValue());
            mplew.writeInt(Math.min(gauge, 9999));
            mplew.write(islight ? 2 : 1); // 1 빛으로감 2어둠으로가는중
            return mplew.getPacket();
        }

        public static byte[] giveBuff(Map<MapleBuffStat, Pair<Integer, Integer>> statups, MapleStatEffect effect, MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
            /*  if(GameConstants.isEvan(chr.getJob())){
                mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 00 00 00 00 00 00 00 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 C8 4D 52 01 10 27 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 3F 96 1D 00 C8 4D 52 01 00 00 00 00 00 0A 00 01 00 00 00 C8 4D 52 01 01 00 00 00 01 8A F8 1F 00 00 00 00 10 27 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 01 04 00 00 00 00"));
                return mplew.getPacket();
            }*/

            mplew.writeInt(0);
            mplew.writeInt(0);

            List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);
            PacketHelper.writeBuffMask(mplew, newstatups);
            for (Pair<MapleBuffStat, Pair<Integer, Integer>> stat : newstatups) {
                if (!stat.getLeft().canStack() && !stat.getLeft().isSpecialBuff() && !statups.containsKey(MapleBuffStat.KillingPoint)) {
                    if (MapleBuffStat.isEncode4Byte(statups)) {
                        mplew.writeInt(stat.getRight().left);
                    } else {
                        mplew.writeShort(stat.getRight().left);
                    }

                    if (stat.getLeft() == MapleBuffStat.KinesisPsychicPoint || stat.getLeft().SpectorEffect() || stat.getLeft() == MapleBuffStat.HoyoungThirdProperty || stat.getLeft() == MapleBuffStat.TidalForce) {
                        mplew.writeInt(chr.getJob());
                    } else if (stat.getLeft() == MapleBuffStat.CardinalMark) {
                        mplew.writeInt(chr.cardinalMark);
                    } else if (stat.getLeft() == MapleBuffStat.AdelGauge) {
                        mplew.writeInt(15002); // 0th job?
                    } else if (stat.getLeft() == MapleBuffStat.Malice) {
                        mplew.writeInt(6003);
                    } else if (effect == null) {
                        if (stat.getLeft() == MapleBuffStat.AncientGuidance) {
                            mplew.writeInt(chr.getJob());
                        } else if (stat.getLeft() == MapleBuffStat.RWCylinder) {
                            mplew.writeInt(1);
                        } else {
                            mplew.writeInt(0);
                        }
                    } else if (stat.getLeft().isItemEffect() || !effect.isSkill() || SkillFactory.getSkill(effect.getSourceId()) == null) {
                        mplew.writeInt(-effect.getSourceId());
                    } else {
                        mplew.writeInt(effect.getSourceId());
                    }

                    mplew.writeInt(effect == null ? 0 : stat.right.right);
                }
            }

            //A8A8AE
            if (statups.containsKey(MapleBuffStat.SoulMP)) {
                mplew.writeInt(1000); // unk
                mplew.writeInt(effect.getSourceId());
            }

            if (statups.containsKey(MapleBuffStat.FullSoulMP)) {
                mplew.writeInt(0);
            }

            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);

            mplew.writeInt(0); //size, 28183line 342v

            if (statups.containsKey(MapleBuffStat.DiceRoll)) { // int * 22
                giveDice(mplew, effect, chr);
            }

            if (statups.containsKey(MapleBuffStat.CurseOfCreation)) {
                mplew.writeInt(10); // 최종 데미지 10% 감소
            }

            if (statups.containsKey(MapleBuffStat.CurseOfDestruction)) {
                mplew.writeInt(15); // 피격 데미지 15% 증가
            }


            if (statups.containsKey(MapleBuffStat.UnkBuffStat28)) {
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.KeyDownMoving)) {
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.KillingPoint)) {
                mplew.write(chr.killingpoint);
            }

            if (statups.containsKey(MapleBuffStat.PinkbeanRollingGrade)) {
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.Judgement)) {
                if (statups.get(MapleBuffStat.Judgement).left == 1) { // 크확
                    mplew.writeInt(effect.getV());
                } else if (statups.get(MapleBuffStat.Judgement).left == 2) { // 아이템 드랍
                    mplew.writeInt(effect.getW());
                } else if (statups.get(MapleBuffStat.Judgement).left == 3) { // 내성
                    mplew.writeInt((effect.getX() << 8) + effect.getY());
                } else { // 드레인
                    mplew.writeInt(0);
                }
            }

            if (statups.containsKey(MapleBuffStat.StackBuff)) {
                mplew.write(chr.stackbuff);
            }

            if (statups.containsKey(MapleBuffStat.Trinity)) {
                mplew.write(statups.get(MapleBuffStat.Trinity).left); // Trinity count
            }

            if (statups.containsKey(MapleBuffStat.ElementalCharge)) {
                mplew.write(chr.getElementalCharge()); // 스택
                mplew.writeShort(effect.getY() * chr.getElementalCharge()); // 공격력
                mplew.write(effect.getU() * chr.getElementalCharge()); // 최대 타겟
                mplew.write(effect.getW() * chr.getElementalCharge()); // 타겟 횟수
            }

            if (statups.containsKey(MapleBuffStat.LifeTidal)) {
                switch (statups.get(MapleBuffStat.LifeTidal).left) {
                    case 1:
                        mplew.writeInt(effect.getX());
                        break;
                    case 2:
                        mplew.writeInt(effect.getProp());
                        break;
                    case 3:
                        mplew.writeInt(chr.getStat().getCurrentMaxHp());
                        break;
                    default:
                        mplew.writeInt(0);
                        break;
                }
            }

            if (statups.containsKey(MapleBuffStat.AntiMagicShell)) {
                mplew.write(chr.getAntiMagicShell());
                mplew.writeInt(effect.getDuration()); // 324 ++
            }

            if (statups.containsKey(MapleBuffStat.Larkness)) {
                mplew.writeInt(effect.getSourceId()); // skillid
                mplew.writeInt(effect.getDuration());
                mplew.writeInt((effect.getSourceId() == 20040216 || effect.getSourceId() == 20040217) ? 0 : effect.getSourceId() == 20040219 ? 20040220 : 20040219);
                mplew.writeInt(effect.getDuration());
                mplew.writeInt(effect.getSourceId() == 20040217 ? 10000 : -1);
                mplew.writeInt(effect.getSourceId() == 20040216 ? -1 : 1);
                mplew.writeInt(chr.getSkillLevel(400021005) > 0 && !chr.getUseTruthDoor() && (effect.getSourceId() == 20040219 || effect.getSourceId() == 20040220) ? 1 : 0);
            }

            if (statups.containsKey(MapleBuffStat.IgnoreTargetDEF)) {
                mplew.writeInt(chr.lightning);
            }

            if (statups.containsKey(MapleBuffStat.StopForceAtominfo)) {
                mplew.writeInt(effect.getSourceId() == 61121217 ? 4 : effect.getSourceId() == 61110211 ? 3 : effect.getSourceId() != 61101002 && effect.getSourceId() != 61110211 ? 2 : 1); // 공격 타입
                mplew.writeInt(effect.getSourceId() != 61101002 && effect.getSourceId() != 61110211 ? 5 : 3); // 공격 횟수
                mplew.writeInt(chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11) == null ? 0 : chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId()); // 무기 ID
                mplew.writeInt(effect.getSourceId() != 61101002 && effect.getSourceId() != 61110211 ? 5 : 3); // 공격 횟수
                mplew.writeZeroBytes(effect.getSourceId() != 61101002 && effect.getSourceId() != 61110211 ? 20 : 12); // ?
            }

            if (statups.containsKey(MapleBuffStat.SmashStack)) {
                if (GameConstants.isKaiser(chr.getJob())) {
                    mplew.writeInt(chr.getKaiserCombo());
                } else if (GameConstants.isPathFinder(chr.getJob())) {
                    mplew.writeInt(0);
                } else if (GameConstants.isHoyeong(chr.getJob())) {
                    mplew.writeInt(chr.energy);
                }
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.MobZoneState)) {
                mplew.writeInt(statups.get(MapleBuffStat.MobZoneState).right); //oid, 2615240?
                mplew.writeInt(-1);
            }

            if (statups.containsKey(MapleBuffStat.IncreaseJabelinDam)) {
                mplew.writeInt(2);
                mplew.writeInt(152120001);
                mplew.writeInt(400021000); // ?
            }

            if (statups.containsKey(MapleBuffStat.Slow)) {
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.IgnoreMobPdpR)) {
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.BdR)) {
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.DropRIncrease)) {
                mplew.writeInt(0);
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.PoseType)) {
                mplew.write(0);//chr.getBuffedValue(11121005));
            }

            if (statups.containsKey(MapleBuffStat.Beholder)) {
                mplew.writeInt(chr.getBeholderSkill1()); // beholder dominent
            }

            if (statups.containsKey(MapleBuffStat.CrossOverChain)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Reincarnation)) {
                mplew.writeInt(chr.getReinCarnation());
            }

            if (statups.containsKey(MapleBuffStat.ExtremeArchery)) {
                mplew.writeInt(effect.getX());
                mplew.writeInt(effect.getZ());
            }

            if (statups.containsKey(MapleBuffStat.QuiverCatridge)) {
                mplew.writeInt(chr.getQuiverType()); //
            }

            if (statups.containsKey(MapleBuffStat.ImmuneBarrier)) {
                mplew.writeInt(statups.get(MapleBuffStat.ImmuneBarrier).left);
            }

            if (statups.containsKey(MapleBuffStat.ArmorPiercing)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.SharpEyes)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.AdvancedBless)) {
                if (chr.getSkillLevel(2320050) > 0) {
                    mplew.writeInt(SkillFactory.getSkill(2320050).getEffect(1).getBdR());
                } else {
                    mplew.writeInt(0);
                }
                mplew.writeInt(0); // 351 new
            }

            if (statups.containsKey(MapleBuffStat.Infiltrate)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Bless)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.DotHealHPPerSecond)) {
                mplew.writeInt(180);
            }

            if (statups.containsKey(MapleBuffStat.DotHealMPPerSecond)) {
                mplew.writeInt(180);
            }

            if (statups.containsKey(MapleBuffStat.SpiritGuard)) {
                mplew.writeInt(chr.getSpiritGuard());
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat9)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.KnockBack)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.ShieldAttack)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.SSFShootingAttack)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.BattlePvP_Helena_Mark)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.PinkbeanAttackBuff)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.RoyalGuardState)) {
                mplew.writeInt(statups.get(MapleBuffStat.IndiePad).left);
                mplew.writeInt(statups.get(MapleBuffStat.RoyalGuardState).left);
            }

            if (statups.containsKey(MapleBuffStat.MichaelSoulLink)) {
                boolean isParty = chr.getParty() != null;
                mplew.writeInt(isParty ? chr.getParty().getMembers().size() : 1);
                mplew.write(isParty ? (chr.getParty().getLeader().getId() == chr.getId()) : true);
                mplew.writeInt(isParty ? chr.getParty().getId() : 0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.AdrenalinBoost)) {
                mplew.write(1);
            }

            if (statups.containsKey(MapleBuffStat.RWCylinder)) {
                mplew.write(chr.Bullet);
                mplew.writeShort(chr.Cylinder);
                mplew.write(0); // 351 new
            }

            if (statups.containsKey(MapleBuffStat.BodyOfSteal)) {
                mplew.writeInt(chr.bodyOfSteal);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat12)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.RwMagnumBlow)) {
                mplew.writeShort(0);
                mplew.write(0);
            }

            if (effect != null) {
                boolean active = effect.isEnergyChargeCooling() || effect.isEnergyChargeActived();
                mplew.writeInt((chr.getBuffedEffect(MapleBuffStat.EnergyCharged) != null && active) ? chr.getBuffSource(MapleBuffStat.EnergyCharged) : 0);
            } else {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.BladeStance)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.DarkSight)) {
                mplew.writeInt(1000000);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Stigma)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.BonusAttack)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.CriticalGrowing)) {
                mplew.writeInt(chr.criticalDamageGrowing);
            }

            if (statups.containsKey(MapleBuffStat.Ember)) {
                mplew.writeInt(chr.getIgnition());
            }

            if (statups.containsKey(MapleBuffStat.PickPocket)) {
                mplew.writeInt(chr.getPickPocket());
            }

            if (statups.containsKey(MapleBuffStat.HolyUnity)) {
                mplew.writeShort(effect.getLevel());
            }

            if (statups.containsKey(MapleBuffStat.DemonFrenzy)) {
                mplew.writeShort(effect.getLevel());
            }

            if (statups.containsKey(MapleBuffStat.ShadowSpear)) {
                mplew.writeShort(0);
            }

            if (statups.containsKey(MapleBuffStat.RhoAias)) {
                mplew.writeInt(chr.getId());
                if (chr.getRhoAias() <= effect.getY()) { // 1 ~ 10
                    mplew.writeInt(3);
                } else if (chr.getRhoAias() <= effect.getY() + effect.getW()) { // 11 ~ 20
                    mplew.writeInt(2);
                } else {
                    mplew.writeInt(1);
                }
                mplew.writeInt(chr.getRhoAias());
                if (chr.getRhoAias() <= effect.getY()) { // 1 ~ 10
                    mplew.writeInt(3);
                } else if (chr.getRhoAias() <= effect.getY() + effect.getW()) { // 11 ~ 20
                    mplew.writeInt(2);
                } else {
                    mplew.writeInt(1);
                }
            }

            if (statups.containsKey(MapleBuffStat.VampDeath)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.HolyMagicShell)) {
                mplew.writeInt(chr.getHolyMagicShell()); // 307 ++
            }

            for (Pair<MapleBuffStat, Pair<Integer, Integer>> stat : newstatups) {
                if (!stat.left.canStack() && stat.left.isSpecialBuff()) {
                    mplew.writeInt(stat.right.left);
                    mplew.writeInt(chr.getBuffSource(stat.left));
                    if (stat.left == MapleBuffStat.RideVehicleExpire) {
                        mplew.write(0);
                        mplew.writeInt(0);
                        mplew.writeShort(stat.right.right / 1000);
                    }
                    if (stat.left == MapleBuffStat.PartyBooster) {
                        mplew.write(1);
                        mplew.writeInt(effect.getStarttime()); // ?? 53321
                    }
                    if (stat.left == MapleBuffStat.EnergyCharged) {
                        mplew.write(effect.isEnergyChargeCooling());
                    }
                    mplew.write(stat.left == MapleBuffStat.PartyBooster); // Dash는 0
                    mplew.writeInt(stat.left == MapleBuffStat.PartyBooster ? 1 : 0); // Dash는 0
                    if (stat.left == MapleBuffStat.GuidedBullet) {
                        mplew.writeInt(chr.guidedBullet);
                        mplew.writeInt(0);
                    } else if (stat.left == MapleBuffStat.RideVehicleExpire || stat.left == MapleBuffStat.PartyBooster || stat.left == MapleBuffStat.DashJump || stat.left == MapleBuffStat.DashSpeed) {
                        mplew.writeShort(stat.right.right / 1000);
                    } else if (stat.left == MapleBuffStat.Grave) {
                        mplew.writeInt(chr.graveObjectId); // objectId
                        mplew.writeInt(0);
                    }
                }
            }

            DecodeIndieTempStat(mplew, newstatups, chr);

            if (statups.containsKey(MapleBuffStat.UsingScouter)) {
                mplew.writeInt(effect.getSourceId());
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat16)) {
                mplew.writeInt(effect.getSourceId());
            }

            if (statups.containsKey(MapleBuffStat.GloryWing)) {
                mplew.writeInt(chr.canUseMortalWingBeat ? 1 : 0); // 모탈 윙 비트
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.BlessMark)) {
                mplew.writeInt(chr.blessMarkSkill);
                switch (chr.blessMarkSkill) { // maximum
                    case 152000007:
                        mplew.writeInt(3);
                        break;
                    case 152110009:
                        mplew.writeInt(6);
                        break;
                    case 152120012:
                        mplew.writeInt(10);
                        break;
                }
            }

            if (statups.containsKey(MapleBuffStat.ShadowerDebuff)) { // 307 ++
                mplew.writeInt(chr.shadowerDebuffOid);
            }

            if (statups.containsKey(MapleBuffStat.WeaponVariety)) { // 307++
                int flag = 0;
                if (chr.getWeaponChanges().contains(64001002)) {
                    flag += 0x1;
                }
                if (chr.getWeaponChanges().contains(64101001)) {
                    flag += 0x2;
                }
                if (chr.getWeaponChanges().contains(64101002)) {
                    flag += 0x4;
                }
                if (chr.getWeaponChanges().contains(64111002)) {
                    flag += 0x8;
                }
                if (chr.getWeaponChanges().contains(64111003)) {
                    flag += 0x10;
                }
                if (chr.getWeaponChanges().contains(64111012)) {
                    flag += 0x20;
                }
                if (chr.getWeaponChanges().contains(64121003) || chr.getWeaponChanges().contains(64121011) || chr.getWeaponChanges().contains(64121016)) {
                    flag += 0x40;
                }
                if (chr.getWeaponChanges().contains(64121021) || chr.getWeaponChanges().contains(64121022) || chr.getWeaponChanges().contains(64121023) || chr.getWeaponChanges().contains(64121024)) {
                    flag += 0x80;
                }
                mplew.writeInt(flag);
            }

            if (statups.containsKey(MapleBuffStat.Overload)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.SpectorGauge)) { // 307++
                mplew.writeInt(chr.SpectorGauge);
            }

            if (statups.containsKey(MapleBuffStat.PlainBuff)) {
                mplew.writeInt(Math.max(0, chr.getArcSpell() * 2 - (chr.getArcSpellSkills().contains(155101100) ? 2 : 0) - (chr.getArcSpellSkills().contains(155111102) ? 2 : 0) - (chr.getArcSpellSkills().contains(155121102) ? 2 : 0)));
                mplew.writeInt(Math.max(0, chr.getArcSpell() * 2 - (chr.getArcSpellSkills().contains(155101100) ? 2 : 0) - (chr.getArcSpellSkills().contains(155111102) ? 2 : 0) - (chr.getArcSpellSkills().contains(155121102) ? 2 : 0)));
            }

            if (statups.containsKey(MapleBuffStat.ScarletBuff)) {
                mplew.writeInt(chr.getArcSpellSkills().contains(155101100) ? 1 : 0);
                mplew.writeInt(chr.getArcSpellSkills().contains(155101100) ? 1 : 0);
            }

            if (statups.containsKey(MapleBuffStat.GustBuff)) {
                mplew.writeInt(chr.getArcSpellSkills().contains(155111102) ? 1 : 0);
                mplew.writeInt(chr.getArcSpellSkills().contains(155111102) ? 1 : 0);
            }

            if (statups.containsKey(MapleBuffStat.AbyssBuff)) {
                mplew.writeInt(chr.getArcSpellSkills().contains(155121102) ? 1 : 0);
                mplew.writeInt(chr.getArcSpellSkills().contains(155121102) ? 1 : 0);
            }

            if (statups.containsKey(MapleBuffStat.WillPoison)) {
                mplew.writeInt(100); // 최종 데미지 증가 %
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat29)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.MarkOfPhantomStack)) {
                mplew.writeInt(chr.getMarkofPhantom()); // 표식 횟수
            }

            if (statups.containsKey(MapleBuffStat.MarkOfPhantomDebuff)) {
                mplew.writeInt(chr.getMarkOfPhantomOid()); // 표식 대상
            }

            if (statups.containsKey(MapleBuffStat.EventSpecialSkill)) {
                int count = 0;
                if (effect.getSourceId() == 80003082)
                    count = (int) (10 - chr.getKeyValue(100857, "feverCnt"));
                mplew.writeInt(count);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat46)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.PapyrusOfLuck)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.PmdReduce)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.ForbidEquipChange)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.YalBuff)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.ComboCounter)) {
                mplew.writeInt(0); // 324 ++
            }

            if (statups.containsKey(MapleBuffStat.Bless5th)) {
                if (effect.getSourceId() == 400001047) {
                    mplew.writeInt(effect.getX());
                } else if (effect.getSourceId() == 400001050) {
                    mplew.writeInt(chr.nextBlessSkill);
                } else {
                    mplew.writeInt(0);
                }
            }

            if (statups.containsKey(MapleBuffStat.AncientGuidance)) {
                mplew.writeInt(effect != null ? 3000 : 0);
                mplew.writeInt(chr.ancientGauge);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat39)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat41)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.HolySymbol)) { // 324 ++
                mplew.writeInt(chr.getId());
                mplew.writeInt(effect.getLevel());
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.write(1);
                mplew.write(0);
                mplew.writeInt(0); // 351 new
            }

            if (statups.containsKey(MapleBuffStat.HoyoungThirdProperty)) {
                mplew.writeInt(chr.useChun ? 1 : 0);
                mplew.writeInt(chr.useJi ? 1 : 0);
                mplew.writeInt(chr.useIn ? 1 : 0);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat50)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.TidalForce)) {
                mplew.writeInt(chr.scrollGauge);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat51)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.EmpiricalKnowledge)) {
                mplew.writeInt(chr.empiricalKnowledge == null ? 0 : chr.empiricalKnowledge.getObjectId());
            }

            if (statups.containsKey(MapleBuffStat.Graffiti)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Novility)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Revenant)) {
                mplew.writeInt(chr.getStat().getMaxHp()); //자기체력?
            }

            if (statups.containsKey(MapleBuffStat.RevenantDamage)) {
                mplew.writeInt(0); //받는 피해량?
                mplew.writeInt(0); //  25번 카운트
            }

            if (statups.containsKey(MapleBuffStat.UNK633)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.RuneOfPure)) {
                mplew.writeInt(0);
            }

            //Aura
            if (statups.containsKey(MapleBuffStat.YellowAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.DrainAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.BlueAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.DarkAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.DebuffAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.UnionAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.IceAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.KnightsAura)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat622)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.ZeroAuraStr)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.ZeroAuraSpd)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1); //인카네이션 파티버프는 0
            }

            if (statups.containsKey(MapleBuffStat.UNK634)) {
                mplew.writeInt(chr.getId());
                mplew.writeInt(1);
            }

            if (statups.containsKey(MapleBuffStat.PhotonRay)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK636)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.RepeatinCartrige)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.ThrowBlasting)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK646)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK647)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.DarknessAura)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UnkBuffStat60)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.BlessOfDarkness)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.ThanatosDescent)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.DragonPang)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Magnet)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK668)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK671)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK672)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.LuckOfUnion)) {
                mplew.writeInt(0);
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.UNK265)) {
                mplew.writeInt(0);
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.NewFlying)) {
                mplew.writeInt(chr.getSkillCustomValue(effect.getSourceId()));
            }

            // DecodeForLocal end
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);

            boolean unk = false;
            if (statups.containsKey(MapleBuffStat.Etherealform)
                    || statups.containsKey(MapleBuffStat.Transform)
                    || statups.containsKey(MapleBuffStat.SwordOfSoulLight)
                    || statups.containsKey(MapleBuffStat.Dike)
                    || statups.containsKey(MapleBuffStat.IceAura)
                    || statups.containsKey(MapleBuffStat.Novility)
                    || statups.containsKey(MapleBuffStat.IndieCD)
                    || statups.containsKey(MapleBuffStat.IndieIgnoreMobPdpR)
                    || statups.containsKey(MapleBuffStat.IndieDamR)
                    || statups.containsKey(MapleBuffStat.BodyPressure)
                    || statups.containsKey(MapleBuffStat.IndieJointAttack)
                    || statups.containsKey(MapleBuffStat.RideVehicle)
                    || statups.containsKey(MapleBuffStat.SpreadThrow)
                    || statups.containsKey(MapleBuffStat.ShadowPartner)
                    || statups.containsKey(MapleBuffStat.ReadyToDie)
                    || statups.containsKey(MapleBuffStat.SharpEyes)
                    || statups.containsKey(MapleBuffStat.Wonder)
                    || statups.containsKey(MapleBuffStat.Creation)
                    || statups.containsKey(MapleBuffStat.BlitzShield)
                    || statups.containsKey(MapleBuffStat.DeathBlessing)
                    || statups.containsKey(MapleBuffStat.DragonPang)
                    || statups.containsKey(MapleBuffStat.ThanatosDescent)) {
                unk = true;
            }

            mplew.write(unk);
            mplew.write(true);
            mplew.write(true);

            if (statups.containsKey(MapleBuffStat.Acc)) {
                mplew.write(19);
            }

            for (Entry<MapleBuffStat, Pair<Integer, Integer>> stat : statups.entrySet()) {
                if (GameConstants.MovementAffectingStat(stat.getKey())) {
                    if (effect == null) {
                        mplew.write(0);
                    } else {
                        mplew.write(stat.getKey() == MapleBuffStat.KeyDownMoving ? 30 : stat.getKey() == MapleBuffStat.EnergyCharged ? 140 : effect.getSourceId() == 35111003 ? 16 : effect.getSourceId() == 35001002 ? 6 : 0);
                    }
                    break;
                }
            }

            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] giveDisease(Map<MapleBuffStat, Pair<Integer, Integer>> statups, MobSkill skill, MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());

            /* SecondaryStat::DecodeForLocal */
            List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);
            mplew.writeInt(0);
            mplew.writeInt(0);
            PacketHelper.writeBuffMask(mplew, newstatups);
            for (Pair<MapleBuffStat, Pair<Integer, Integer>> stat : newstatups) {
                if (!stat.getLeft().canStack() && !stat.getLeft().isSpecialBuff()) {
                    if (MapleBuffStat.isEncode4Byte(statups)) {
                        mplew.writeInt(stat.getRight().left);
                    } else {
                        mplew.writeShort(stat.getRight().left);
                    }
                    if (stat.left == MapleBuffStat.WillPoison) {
                        mplew.writeInt(9);
                    } else {
                        mplew.writeShort(skill.getSkillId());
                        mplew.writeShort(skill.getSkillId() == 237 ? 0 : skill.getSkillLevel());
                    }
                    mplew.writeInt(skill.getSkillId() == 237 ? 0 : skill.getDuration());
                }
            }

            mplew.writeShort(0); //size, Int + write, v1411, 21041 line
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(0);

            if (statups.containsKey(MapleBuffStat.CurseOfCreation)) {
                mplew.writeInt(10);
            }
            if (statups.containsKey(MapleBuffStat.CurseOfDestruction)) {
                mplew.writeInt(15);
            }
            if (statups.containsKey(MapleBuffStat.Slow)) {
                mplew.write(0);
            }

            if (statups.containsKey(MapleBuffStat.VampDeath)) {
                mplew.writeInt(0);
            }

            mplew.writeInt(0);

            if (statups.containsKey(MapleBuffStat.Stigma)) {
                mplew.writeInt(7); // 최대 중첩 수
            }

            if (statups.containsKey(MapleBuffStat.GiveMeHeal)) {
                mplew.writeInt(0);
            }

            DecodeIndieTempStat(mplew, newstatups, chr);

            for (Pair<MapleBuffStat, Pair<Integer, Integer>> stat : newstatups) {
                if (!stat.left.canStack() && stat.left.isSpecialBuff()) {
                    mplew.writeInt(stat.right.left);
                    mplew.writeInt(chr.getBuffSource(stat.left));
                    if (stat.left == MapleBuffStat.RideVehicleExpire) {
                        mplew.writeInt(0);
                    } else if (stat.left == MapleBuffStat.PartyBooster || stat.left == MapleBuffStat.EnergyCharged) {
                        mplew.write(1);
                    }
                    if (stat.left == MapleBuffStat.PartyBooster) {
                        mplew.writeInt(0);
                    }
                    mplew.write(stat.left == MapleBuffStat.PartyBooster);
                    mplew.writeInt(stat.left == MapleBuffStat.PartyBooster ? 1 : 0);
                    if (stat.left == MapleBuffStat.GuidedBullet) {
                        mplew.writeInt(chr.guidedBullet);
                        mplew.writeInt(0);
                    } else if (stat.left == MapleBuffStat.RideVehicleExpire || stat.left == MapleBuffStat.PartyBooster || stat.left == MapleBuffStat.DashJump || stat.left == MapleBuffStat.DashSpeed) {
                        mplew.writeShort(stat.right.right / 1000);
                    } else if (stat.left == MapleBuffStat.Grave) {
                        mplew.writeInt(chr.graveObjectId);
                        mplew.writeInt(0);
                    }
                    if (stat.left == MapleBuffStat.PartyBooster || stat.left == MapleBuffStat.DashJump || stat.left == MapleBuffStat.DashSpeed) {
                        mplew.writeShort(0);
                    }
                }
            }

            if (statups.containsKey(MapleBuffStat.VampDeath)) {
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.WillPoison)) {
                mplew.writeInt(100);
            }

            if (statups.containsKey(MapleBuffStat.Magnet)) {
                mplew.writeInt(0);
            }

            //DecodeForLocal end
            mplew.writeShort(0); // delay
            mplew.write(0);
            mplew.write(0);

            mplew.write(statups.containsKey(MapleBuffStat.Transform)); // fixed 0

            mplew.write(1);
            mplew.write(1);

            for (Entry<MapleBuffStat, Pair<Integer, Integer>> stat : statups.entrySet()) {
                if (GameConstants.MovementAffectingStat(stat.getKey())) { //
                    mplew.write(0);
                    break;
                }
            }

            mplew.writeInt(0);
            mplew.writeInt(0);
            //System.out.println("기브 질병"+mplew.toString());
            return mplew.getPacket();
        }

        public static void DecodeIndieTempStat(MaplePacketLittleEndianWriter mplew, List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups, MapleCharacter chr) {
            Map<MapleBuffStat, List<MapleBuffStatValueHolder>> indiestats = new HashMap<>();

            newstatups.stream().filter((statup) -> (statup.getLeft().canStack())).map((statup) -> {
                indiestats.put(statup.getLeft(), new ArrayList<>());
                return statup;
            }).forEach((statup) -> {
                Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effects = chr.getEffects().iterator();
                while (effects.hasNext()) {
                    Pair<MapleBuffStat, MapleBuffStatValueHolder> effect = effects.next();
                    if (statup.getLeft() == effect.left) {
                        indiestats.get(statup.getLeft()).add(effect.right);
                    }
                }
            });

            List<Pair<MapleBuffStat, List<MapleBuffStatValueHolder>>> indiestatz = PacketHelper.sortIndieBuffStats(indiestats);

            for (Pair<MapleBuffStat, List<MapleBuffStatValueHolder>> indiestat : indiestatz) {
                mplew.writeInt(indiestat.getRight().size());
                for (MapleBuffStatValueHolder indie : indiestat.getRight()) {
                    if (indiestat.getLeft().isItemEffect() || !indie.effect.isSkill() || SkillFactory.getSkill(indie.effect.getSourceId()) == null) {
                        mplew.writeInt(-indie.effect.getSourceId());
                    } else {
                        mplew.writeInt(indie.effect.getSourceId());
                    }
                    mplew.writeInt(indie.value);
                    mplew.writeInt(indie.startTime);
                    mplew.writeInt(indiestat.getLeft() == MapleBuffStat.IndieJointAttack ? indie.value : 0);
                    mplew.writeInt(indie.localDuration);
                    mplew.writeInt(0); // 324 ++
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                }
            }
        }

        private static void giveDice(MaplePacketLittleEndianWriter mplew, MapleStatEffect effect, MapleCharacter chr) {
            int doubledice, dice, thirddice;
            if (chr.getDice() >= 100) {
                thirddice = (chr.getDice() / 100);
                doubledice = (chr.getDice() - (thirddice * 100)) / 10;
                dice = chr.getDice() - (chr.getDice() / 10 * 10);
            } else {
                thirddice = 1;
                doubledice = (chr.getDice() / 10);
                dice = (chr.getDice() - (doubledice * 10));
            }
            //int * 22
            if (dice == 3 || doubledice == 3 || thirddice == 3) {
                if (dice == 3 && doubledice == 3 && thirddice == 3) {
                    mplew.writeInt(effect.getPercentHP() + 15); //MAX HP
                    mplew.writeInt(effect.getPercentMP() + 15); //MAX MP
                } else if ((dice == 3 && doubledice == 3) || (dice == 3 && thirddice == 3) || (thirddice == 3 && doubledice == 3)) {
                    mplew.writeInt(effect.getPercentHP() + 10); //MAX HP
                    mplew.writeInt(effect.getPercentMP() + 10); //MAX MP
                } else {
                    mplew.writeInt(effect.getPercentHP()); //MAX HP
                    mplew.writeInt(effect.getPercentMP()); //MAX MP
                }
            } else {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
            if (dice == 4 || doubledice == 4 || thirddice == 4) {
                if (dice == 4 && doubledice == 4 && thirddice == 4) {
                    mplew.writeInt(effect.getCr() + 15); //ok
                } else if ((dice == 4 && doubledice == 4) || (dice == 4 && thirddice == 4) || (thirddice == 4 && doubledice == 4)) {
                    mplew.writeInt(effect.getCr() + 10); //ok
                } else {
                    mplew.writeInt(effect.getCr()); //ok
                }
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            if (dice == 2 || doubledice == 2 || thirddice == 2) {
                if (dice == 2 && doubledice == 2 && thirddice == 2) {
                    mplew.writeInt(effect.getWDEFRate() + 15); //Physical Defense
                } else if ((dice == 2 && doubledice == 2) || (dice == 2 && thirddice == 2) || (thirddice == 2 && doubledice == 2)) {
                    mplew.writeInt(effect.getWDEFRate() + 10); //Physical Defense
                } else {
                    mplew.writeInt(effect.getWDEFRate()); //Physical Defense
                }
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            if (dice == 5 || doubledice == 5 || thirddice == 5) {
                if (dice == 5 && doubledice == 5 && thirddice == 5) {
                    mplew.writeInt(effect.getDAMRate() + 15); //Increase Damage
                } else if ((dice == 5 && doubledice == 5) || (dice == 5 && thirddice == 5) || (thirddice == 5 && doubledice == 5)) {
                    mplew.writeInt(effect.getDAMRate() + 10); //Increase Damage
                } else {
                    mplew.writeInt(effect.getDAMRate()); //Increase Damage
                }
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            if (dice == 6 || doubledice == 6 || thirddice == 6) {
                if (dice == 6 && doubledice == 6 && thirddice == 6) {
                    mplew.writeInt(effect.getEXPRate() + 15); //ok
                } else if ((dice == 6 && doubledice == 6) || (dice == 6 && thirddice == 6) || (thirddice == 6 && doubledice == 6)) {
                    mplew.writeInt(effect.getEXPRate() + 10); //ok
                } else {
                    mplew.writeInt(effect.getEXPRate()); //ok
                }
            } else {
                mplew.writeInt(0);
            }
            if (dice == 7 || doubledice == 7 || thirddice == 7) {
                if (dice == 7 && doubledice == 7 && thirddice == 7) {
                    mplew.writeInt(effect.getIgnoreMob() + 15); //ok
                } else if ((dice == 7 && doubledice == 7) || (dice == 7 && thirddice == 7) || (thirddice == 7 && doubledice == 7)) {
                    mplew.writeInt(effect.getIgnoreMob() + 10); //ok
                } else {
                    mplew.writeInt(effect.getIgnoreMob()); //ok
                }
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        public static byte[] cancelBuff(Map<MapleBuffStat, Pair<Integer, Integer>> statups, MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
            mplew.writeInt(0);
            mplew.write(0); // 332++
            mplew.write(0); // 333++

            mplew.write(0); // 351 new
            List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);

            PacketHelper.writeBuffMask(mplew, newstatups);

            /*            int size = 0, size2 = 0;
             for (Entry<MapleBuffStat, Integer> stat : statupz.entrySet()) {
             if (stat.getKey().canStack()) { // 
             size++;
             }
             }*/
            //SecondaryStat::DecodeIndieTempStat
            DecodeIndieTempStat(mplew, newstatups, chr);

            /*            for (Entry<MapleBuffStat, Integer> statup : statupz.entrySet()) {
             if (statup.getKey().canStack()) {
             for (Entry<MapleBuffStat, List<Pair<Integer, MapleStatEffect>>> indiestat : chr.getIndietempstat().entrySet()) {
             if (statup.getKey() == indiestat.getKey()) {
             size2++;
             }
             }
             }
             }

             if (size > size2) {
             mplew.writeZeroBytes((size - size2) * 4);
             }*/
            mplew.write(0); // 332++?

            mplew.write(0); // 333++

            for (Entry<MapleBuffStat, Pair<Integer, Integer>> stat : statups.entrySet()) {
                if (GameConstants.MovementAffectingStat(stat.getKey())) { // 
                    mplew.write(1);
                    break;
                }
            }

            mplew.write(statups.containsKey(MapleBuffStat.Transform) || statups.containsKey(MapleBuffStat.IndieJointAttack) || statups.containsKey(MapleBuffStat.RideVehicle) || statups.containsKey(MapleBuffStat.SpreadThrow)); // fixed 0

            mplew.write(statups.containsKey(MapleBuffStat.Transform) || statups.containsKey(MapleBuffStat.IndieJointAttack) || statups.containsKey(MapleBuffStat.RideVehicle) || statups.containsKey(MapleBuffStat.SpreadThrow)); // fixed 0

            mplew.writeInt(0); // 팅 방지

            return mplew.getPacket();
        }

        public static byte[] giveForeignBuff(MapleCharacter chr, Map<MapleBuffStat, Pair<Integer, Integer>> statups, MapleStatEffect effect) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
            mplew.writeInt(chr.getId());

            List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);
            PacketHelper.writeBuffMask(mplew, newstatups);
            PacketHelper.encodeForRemote(mplew, statups, chr);

            mplew.writeShort(0);
            mplew.write(statups.containsKey(MapleBuffStat.BlessedHammer) || statups.containsKey(MapleBuffStat.IceAura));

            return mplew.getPacket();
        }

        public static byte[] giveForeignDeBuff(MapleCharacter chr, Map<MapleBuffStat, Pair<Integer, Integer>> statups) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
            mplew.writeInt(chr.getId());

            List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);
            PacketHelper.writeBuffMask(mplew, newstatups);
            PacketHelper.encodeForRemote(mplew, statups, chr);

            mplew.writeShort(0);
            mplew.write(statups.containsKey(MapleBuffStat.Novility) ? (byte) 1 : (byte) 0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);

            return mplew.getPacket();
        }

        public static byte[] cancelForeignBuff(MapleCharacter chr, Map<MapleBuffStat, Pair<Integer, Integer>> statups) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
            mplew.writeInt(chr.getId());

            List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);
            PacketHelper.writeBuffMask(mplew, newstatups);
            DecodeIndieTempStat(mplew, newstatups, chr);

            if (statups.containsKey(MapleBuffStat.PoseType)) {
                mplew.write(true);
            }

            mplew.write(statups.containsKey(MapleBuffStat.PoseType) || statups.containsKey(MapleBuffStat.BlessedHammer));
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeLong(0);

            return mplew.getPacket();
        }
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="InfoPacket">

    public static class InfoPacket {

        public static byte[] showMesoGain(final long gain, boolean pet, final boolean inChat) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(0);
            mplew.write(pet);
            mplew.write(1); // A portion was not found after falling on the ground
            mplew.write(0);//
            mplew.writeInt(gain); //1.2.203+
            mplew.writeShort(0);//길드 메소 줍기?
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] getShowInventoryStatus(int mode) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(0);
            mplew.write(0);
            mplew.write(mode);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0); // 1.2.331
            return mplew.getPacket();
        }

        public static byte[] getShowItemGain(int itemId, short quantity, boolean pet) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(0);
            mplew.write(pet);
            mplew.write(0);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            mplew.write(0); // 329++

            return mplew.getPacket();
        }

        public static byte[] updateQuest(final MapleQuestStatus quest) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(1);
            mplew.writeInt(quest.getQuest().getId());
            mplew.write(quest.getStatus());
            switch (quest.getStatus()) {
                case 0:
                    mplew.write(0);
                    mplew.write(0); //++342
                    break;
                case 1:
                    mplew.writeMapleAsciiString(quest.getCustomData() != null ? quest.getCustomData() : "");
                    break;
                case 2:
                    mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] updateQuestMobKill(int status, int questid, String mobcount) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(status);
            mplew.writeInt(questid);
            mplew.write(1);
            mplew.writeMapleAsciiString(mobcount);
            return mplew.getPacket();
        }

        public static byte[] updateQuestInfo(int status, int type, int questid) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(status);
            mplew.writeInt(questid);
            mplew.write(type);
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

            return mplew.getPacket();
        }

        public static byte[] updateAngelicBusterDressUp() {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(1);
            mplew.writeInt(7707);
            mplew.write(2);
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis() * 24 * 60 * 60 * 1000));

            return mplew.getPacket();
        }

        public static byte[] updateQuestMobKills(final MapleQuestStatus status) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(1);
            mplew.writeInt(status.getQuest().getId());
            mplew.write(1);
            final StringBuilder sb = new StringBuilder();
            for (final int kills : status.getMobKills().values()) {
                sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
            }
            mplew.writeMapleAsciiString(sb.toString());
            mplew.writeLong(0);

            return mplew.getPacket();
        }

        public static byte[] itemExpired(int itemid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(2); // ++
            mplew.writeInt(itemid);

            return mplew.getPacket();
        }

        public static byte[] GainEXP_Monster(MapleCharacter chr, final long gain, final boolean white, long flag, int eventBonusExp, int weddingExp, int partyExp, int itemEquipExp, int pcExp, int rainbowWeekExp, int boomupExp, int portionExp, int skillExp, int buffExp, int restExp, int itemExp, int valueExp, int bonusExp, int bloodExp, int iceExp, Pair<Long, Integer> burningExp, int hpLiskExp, int fieldBonusExp, int eventBonusExp2, int fieldBonusExp2) {
            final MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
            packet.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            packet.write(3);
            packet.write(white ? 1 : 0);
            packet.writeLong(gain);

            packet.write(0); // Not in chat
            packet.writeLong(0); // 351 new
            packet.writeLong(flag);

            if ((flag & 1) != 0) {
                packet.writeInt(eventBonusExp); // 이벤트 보너스 경험치
            }

            if ((flag & 4) != 0) {
                byte a = 1;
                packet.write(a);
                if (a > 0) {
                    packet.write(2);
                }
            }

            if ((flag & 0x10) != 0) {
                packet.writeInt(partyExp); // 파티
            }
            if ((flag & 0x20) != 0) {
                packet.writeInt(weddingExp); // 웨딩 보너스
            }
            if ((flag & 0x40) != 0) {
                packet.writeInt(itemEquipExp); // 아이템 장착 보너스
            }
            if ((flag & 0x80) != 0) {
                packet.writeInt(pcExp); // PC방 보너스
            }
            if ((flag & 0x100) != 0) {
                packet.writeInt(rainbowWeekExp); //레인보우 위크 보너스
            }
            if ((flag & 0x200) != 0) {
                packet.writeInt(boomupExp); //붐 업 보너스
            }
            if ((flag & 0x400) != 0) {
                packet.writeInt(portionExp); //비약 보너스
            }
            if ((flag & 0x800) != 0) {
                packet.writeInt(skillExp); // 버닝 경험치
            }
            if ((flag & 0x1000) != 0) {
                packet.writeInt(buffExp); //버프 보너스
            }
            if ((flag & 0x2000) != 0) {
                packet.writeInt(restExp); //휴식 보너스
            }
            if ((flag & 0x4000) != 0) {
                packet.writeInt(itemExp); //아이템 보너스
            }
            if ((flag & 0x10000) != 0) {
                packet.writeInt(14); //아이템에 의한 %증가 경험치
            }
            if ((flag & 0x20000) != 0) {
                packet.writeInt(valueExp); // 밸류팩 경험치 보너스
            }
            if ((flag & 0x40000) != 0) {
                packet.writeInt(16); // 아이템에 의한 파티퀘스트 %증가 경험치
            }
            if ((flag & 0x80000) != 0) {
                packet.writeInt(bonusExp); // 추가경험치 보너스
            }
            if ((flag & 0x100000) != 0) {
                packet.writeInt(bloodExp); // 혈맹 경험치 보너스
            }
            if ((flag & 0x200000) != 0) {
                packet.writeInt(iceExp); // 냉동용사 경험치 보너스
            }
            if ((flag & 0x400000) != 0) {
                packet.writeInt(burningExp.left); //버닝 필드 보너스 경험치
                packet.writeInt(burningExp.right); //%값
            }
            if ((flag & 0x800000) != 0) {
                packet.writeInt(hpLiskExp); // HP 리스크 경험치
            }
            if ((flag & 0x1000000) != 0) {
                packet.writeInt(fieldBonusExp); // 필드 보너스 경험치
            }
            if ((flag & 0x2000000) != 0) {
                packet.writeInt(23); // 누적 사냥 수 보너스 경험치
            }
            if ((flag & 0x4000000) != 0) {
                packet.writeInt(eventBonusExp2); // 이벤트 보너스 경험치
            }
            if ((flag & 0x8000000) != 0) {
                packet.writeInt(25); // PC방 단짝 보너스 경험치
            }
            if ((flag & 0x10000000) != 0) {
                packet.writeInt(fieldBonusExp2); // 필드 보너스 경험치
            }
            if ((flag & 0x20000000) != 0) {
                packet.writeInt(27); // 슈퍼돼지 럭키 보너스 경험치
            }
            if ((flag & 0x40000000) != 0) {
                packet.writeInt(28); // 월드게이지 이벤트 보너스 경험치
            }
            return packet.getPacket();
        }

        public static byte[] GainEXP_Others(final long gain, final boolean inChat, final boolean white) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(3); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
            mplew.write(white ? 1 : 0);
            mplew.writeLong(gain);
            mplew.write(0); // Not in chat
            mplew.writeLong(0); // ?? 274 ++
            mplew.writeLong(0); // 351 new
            mplew.writeInt(0); // Bonus Event EXP (+%d) (does not work with White on)
            mplew.write(0);
            mplew.write(0); // (A bonus EXP %d%% is awarded for every 3rd monster defeated.)
            mplew.write(0); // Bonus Wedding EXP
            mplew.writeInt(0); // Party EXP bonus. -1 for default +30, or value / 100.0 = result
            mplew.writeInt(0); // Bonus EXP for PARTY (+%d) || Bonus Event Party EXP (+%d) x%d
            // Class_Bonus_EXP
            mplew.writeInt(0); // Equip Item Bonus EXP
            mplew.writeInt(0); // Internet Cafe EXP Bonus (+%d)
            mplew.writeInt(0); // Rainbow Week Bonus EXP (+%d)
            mplew.writeInt(0); // Monster Card Completion Set +exp
            mplew.writeInt(0); // Boom Up Bonus EXP (+%d)
            mplew.writeInt(0); // Potion Bonus EXP (+%d)
            mplew.writeInt(0/*20021110*/); // %s Bonus EXP (+%d) (string bonus exp?)
            mplew.writeInt(0); // Buff Bonus EXP (+%d)
            mplew.writeInt(0); // Rest Bonus EXP (+%d)
            mplew.writeInt(0); // Item Bonus EXP (+%d)
            mplew.writeInt(0); // Party Ring Bonus EXP(+%d)
            mplew.writeInt(0); // Cake vs Pie Bonus EXP(+%d)
            mplew.writeInt(0); // 274 ++
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] getSPMsg(byte sp, short job) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(4);
            mplew.writeShort(job);
            mplew.write(sp);

            return mplew.getPacket();
        }

        public static byte[] getShowFameGain(final int gain) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(6);
            mplew.writeInt(gain);

            return mplew.getPacket();
        }

        public static byte[] getGPMsg(int itemid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(8);
            mplew.writeInt(itemid);
            mplew.writeInt(itemid);
            mplew.writeInt(itemid);
            return mplew.getPacket();
        }

        public static byte[] getGPContribution(int itemid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(9);
            mplew.writeInt(itemid);
            mplew.write(0); //274 +

            return mplew.getPacket();
        }

        public static byte[] getStatusMsg(int itemid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(10);
            mplew.writeInt(itemid);

            return mplew.getPacket();
        }

        public static byte[] updateInfoQuest(final int quest, final String data) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(13); //334 -1
            mplew.writeInt(quest);
            mplew.writeMapleAsciiString(data);

            return mplew.getPacket();
        }

        public static byte[] updateClientInfoQuest(final int quest, final String data) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(13);
            mplew.writeInt(quest);
            mplew.writeMapleAsciiString(data);

            return mplew.getPacket();
        }

        public static byte[] updatePandoraBox(String text) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(15);
            mplew.writeInt(14940);
            mplew.write(HexTool.getByteArrayFromHexString(text));

            return mplew.getPacket();
        }

        public static byte[] showItemReplaceMessage(final List<String> message) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(17);
            mplew.write(message.size());
            for (final String x : message) {
                mplew.writeMapleAsciiString(x);
            }

            return mplew.getPacket();
        }

        public static byte[] showTraitGain(MapleTraitType trait, int amount) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(19);
            mplew.writeInt(trait.getStat().getValue());
            mplew.writeInt(amount);

            return mplew.getPacket();
        }

        public static byte[] showTraitMaxed(MapleTraitType trait) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(19);
            mplew.writeLong(trait.getStat().getValue());

            return mplew.getPacket();
        }

        public static byte[] getBPMsg(int amount) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(20);
            mplew.writeInt(amount); // Battle Points
            mplew.writeInt(0); // Battle EXP

            return mplew.getPacket();
        }

        public static byte[] updateDailyGift(String key) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(34);
            mplew.writeInt(15); // what
            mplew.writeMapleAsciiString(key);

            return mplew.getPacket();
        }

        public static byte[] showExpireMessage(final byte type, final List<Integer> item) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4 + (item.size() * 4));

            // normal = 10, seal = 13, skill = 15;
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(type);
            mplew.write(item.size());
            for (final Integer it : item) {
                mplew.writeInt(it);
            }

            return mplew.getPacket();
        }

        public static byte[] showStatusMessage(final int mode, final String info, final String data) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // 19: The Android is not powered. Please insert a Mechanical Heart.
            // 20: You recovered some fatigue by resting.
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(mode);
            if (mode == 27) { // 274 ++
                mplew.writeMapleAsciiString(info); //name got Shield.
                mplew.writeMapleAsciiString(data); //Shield applied to name.
            }

            return mplew.getPacket();
        }

        public static byte[] showReturnStone(final int act) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // 0: You can't use that item during a divorce.
            // 1: The location of your spouse is unknown.
            // 2: Your spouse is in an area where Return Stones cannot be used.
            // 3: Return Stones cannot be used here.
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(28); //274 ++
            mplew.write(act);

            return mplew.getPacket();
        }

        public static final byte[] comboKill(int combo, int monster) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(38);
            mplew.write(1);
            mplew.writeInt(combo);
            mplew.writeInt(monster);
            mplew.writeInt(0); //콤보킬 스킨 기본 : 0
            mplew.writeInt(combo);
            return mplew.getPacket();
        }

        public static byte[] multiKill(int count, long exp) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(38);
            mplew.write(0);
            mplew.writeLong(exp);
            mplew.writeInt(count);
            mplew.writeInt(count);
            mplew.writeInt(0); //멀티킬 스킨 기본 : 0
            return mplew.getPacket();
        }
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="GuildPacket">
    public static class GuildPacket {

        public static byte[] changeCustomGuildEmblem(MapleCharacter chr, byte[] imgdata) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(132); // 1.2.351 -2
            mplew.writeInt(chr.getGuildId());
            mplew.writeInt(chr.getId());
            mplew.write(1);

            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.write(0);

            writeImageData(mplew, imgdata);
            mplew.writeInt(1);
            return mplew.getPacket();
        }

        public static void writeImageData(MaplePacketLittleEndianWriter mplew, byte[] imgdata) {
            int size = imgdata.length;
            mplew.writeInt(size);
            if (size > 0) {
                mplew.write(imgdata);
            }
        }

        public static byte[] useNoblessSkill(int skid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(169);
            mplew.writeInt(0);
            mplew.writeInt(skid);
            return mplew.getPacket();
        }

        public static byte[] doGuildAttendance(MapleCharacter chr) { // 1.2.329 OK
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(0xB0);
            mplew.writeInt(chr.getGuildId());
            mplew.writeInt(chr.getId());
            mplew.writeInt(GameConstants.getCurrentDate());
            return mplew.getPacket();
        }

        public static byte[] LooksGuildInformation(MapleGuild guild) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(0x3F); //+4
            mplew.writeInt(guild.getId());
            mplew.writeAsciiString(""); //YQWNzzUyWxT7sjZg999SkN8lIk4KAVcioW12lcqBS0vHxDwLu6Tb8pqKmUqR0ElPLBDsTvh9VeSUZhgmvjjKWqn7gFs3G9WzgtijfHyVOONSU831W3pwPQBZoC8hSOslLlcu3g9p4ZW
            getGuildInfo(mplew, guild);
            mplew.writeShort(1000);
            mplew.writeShort(100);
            mplew.writeShort(10);
            return mplew.getPacket();
        }

        public static byte[] showSearchGuildInfo(byte mode, List<MapleGuild> gss, String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SEARCH_GUILD.getValue());
            mplew.write(mode);
            if (mode != 4) {
                mplew.write(0);
                mplew.writeMapleAsciiString(name);
            } else {
                mplew.writeInt(0);
                mplew.write(0);
                mplew.write(1);
                mplew.writeInt(1);
                mplew.writeShort(0);
                mplew.writeLong(0);
                mplew.write(1);
                mplew.write(1);
            }
            mplew.writeInt(0);
            if (gss.size() != 0) {
                mplew.writeInt(0);
                mplew.writeInt(gss.size());
            } else {
                mplew.writeInt(gss.size());
                mplew.writeInt(0);
            }
            int i = 0;
            for (MapleGuild gs : gss) {
                i++;
                mplew.writeInt(gs.getId());
                mplew.write(gs.getLevel());
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeMapleAsciiString(gs.getLeaderName());
                mplew.writeShort(gs.getMembers().size());
                mplew.writeShort(gs.avergeMemberLevel());
                mplew.write(mode == 4); // boolean
                mplew.writeLong(0);
                mplew.write(1);
                mplew.writeMapleAsciiString(gs.getNotice());
                mplew.writeInt(143);
                mplew.writeInt(63);
                mplew.writeInt(2);
                mplew.write(i <= 5 ? 1 : 0);
            }
            //mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] guildInvite(int gid, String guildname, MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(103); // 1.2.351 -7
            mplew.writeInt(gid);
            mplew.writeMapleAsciiString(guildname);
            mplew.writeInt(chr.getId());
            mplew.writeMapleAsciiString(chr.getName());
            mplew.writeInt(chr.getLevel());
            mplew.writeInt(chr.getJob());
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] cancelGuildRequest(int gid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(72); // 1.2.351 -1
            mplew.writeInt(gid);
            mplew.writeInt(0); // 332++
            return mplew.getPacket();
        }

        public static byte[] requestGuild(MapleGuild g, final MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(81); // 1.2.351 -3
            mplew.writeInt(g.getId());
            mplew.write(g.getLevel());
            mplew.writeMapleAsciiString(g.getName());
            mplew.writeMapleAsciiString(g.getLeaderName()); // 332++
            mplew.writeShort(g.getMembers().size()); // 길드 인원수
            mplew.writeShort(g.avergeMemberLevel()); // 332++
            mplew.write(0);
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.write(1);
            mplew.writeMapleAsciiString("");

            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(false); // 332++
            return mplew.getPacket();
        }

        public static byte[] showGuildInfo(MapleCharacter c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(55); // 1.2.351 -7
            if ((c == null) || (c.getMGC() == null)) {
                mplew.writeInt(0);
                mplew.write(0);
                return mplew.getPacket();
            }
            MapleGuild g = World.Guild.getGuild(c.getGuildId());
            if (g == null) {
                mplew.writeInt(0);
                mplew.write(0);
                return mplew.getPacket();
            }
            mplew.writeInt(0); //50000자리
            mplew.write(true);
//            mplew.writeInt(g.getNoblessSkillPoint()); // 1.2.329++ g.getNoblessSkillPoint()
            getGuildInfo(mplew, g);
            mplew.writeInt(30);
            for (int i = 0; i < 30; i++) {
                mplew.writeInt(GameConstants.getGuildExpNeededForLevel(i)); // 1.2.329++;
            }
            return mplew.getPacket();
        }

        public static byte[] getGuildInfo(MapleGuild g) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(56); // 1.2.351 -7
            mplew.writeInt(g.getId());
            mplew.writeMapleAsciiString("");
            getGuildInfo(mplew, g);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static void getGuildInfo(MaplePacketLittleEndianWriter mplew, MapleGuild guild) { // 1.2.329 OK
            mplew.writeInt(guild.getId());
            mplew.writeMapleAsciiString(guild.getName());
            for (int i = 1; i <= 10; i++) {
                mplew.writeMapleAsciiString(i > 5 ? "" : guild.getRankTitle(i));
                mplew.writeInt(i > 5 ? 0x400 : guild.getRankRole(i)); // 1.2.329++ (길드 역할)
            }

            guild.addMemberData(mplew);
            guild.addRequestMemberData(mplew);

            mplew.writeInt(guild.getCapacity());
            mplew.writeShort(guild.getLogoBG());
            mplew.write(guild.getLogoBGColor());
            mplew.writeShort(guild.getLogo());
            mplew.write(guild.getLogoColor());
            mplew.writeMapleAsciiString(guild.getNotice());
            mplew.writeInt(guild.getGP()); // 명성치
            mplew.writeInt(guild.getGP()); // 명성치
            mplew.writeInt(guild.getAllianceId() > 0 ? guild.getAllianceId() : 0);
            mplew.write(guild.getLevel()); //guild.getLevel()
            mplew.writeInt(guild.getGP()); // 길드 포인트
            mplew.writeInt(0); // 어제 출석 추가 명성치
            mplew.writeInt(GameConstants.getCurrentDateYesterday()); // 어제

            mplew.write(1);
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeInt(0); // 출석 인원수
            mplew.writeInt(0); // 가입 설정 (주요 활동 Flag)
            mplew.writeInt(0); // 가입 설정 (연령대 Flag)
            mplew.writeShort(guild.getSkills().size());
            for (MapleGuildSkill i : guild.getSkills()) {
                mplew.writeInt(i.skillID);
                mplew.writeShort(i.level);
                mplew.writeLong(PacketHelper.getTime(i.timestamp));
                mplew.writeMapleAsciiString(i.purchaser);
                mplew.writeMapleAsciiString(i.activator);
            }
            mplew.write(false);
            mplew.write(-1); // 351 new
            int size = guild.getCustomEmblem() == null ? 0 : guild.getCustomEmblem().length;
            mplew.writeInt(size); // customGuildEmblemSize
            if (size > 0) {
                mplew.write(guild.getCustomEmblem()); // customGuildEmblemData
            }
            mplew.writeInt(0);
        }

        public static byte[] newGuildInfo(final MapleCharacter c) { // 1.2.329 OK
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(62); // 1.2.351 -8
            if (c == null || c.getMGC() == null) { // Show empty guild (used for leaving, expelled)
                return genericGuildMessage((byte) 0x5C);
            }
            MapleGuild g = World.Guild.getGuild(c.getGuildId());
            if (g == null) {
                return genericGuildMessage((byte) 0x5C);
            }
            getGuildInfo(mplew, g); // All empty data

            return mplew.getPacket();
        }

        public static byte[] newGuildMember(MapleGuildCharacter mgc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(70); // 1.2.351 -8
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            addNewMemberData(mplew, mgc);
            return mplew.getPacket();
        }

        public static byte[] addRegisterRequest(MapleGuildCharacter mgc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(79);// 1.2.351 -8
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            mplew.writeMapleAsciiString("Zero"); // 자기소개
            addNewMemberData(mplew, mgc);
            return mplew.getPacket();
        }

        public static void addNewMemberData(MaplePacketLittleEndianWriter mplew, MapleGuildCharacter mgc) {
            mplew.writeInt(mgc.getId());
            mplew.writeAsciiString(mgc.getName(), 13);
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeInt(mgc.getAllianceRank());
            mplew.writeInt(mgc.getGuildContribution());
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeLong(System.currentTimeMillis()); // 343+
        }

        public static byte[] RequestDeny(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(100); // 1.2.351 -7
            mplew.writeMapleAsciiString(chr.getName());
            return mplew.getPacket();
        }

        public static byte[] DelayRequest() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(84); // 1.2.351 -7
            return mplew.getPacket();
        }

        public static byte[] memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(bExpelled ? 88 : 91); // 1.2.351 -7
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            mplew.writeMapleAsciiString(mgc.getName());
            return mplew.getPacket();
        }

        public static byte[] guildDisband(int gid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(94); // 1.2.351 -7
            mplew.writeInt(gid);

            return mplew.getPacket();
        }

        public static byte[] guildDisband2() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(157);
            mplew.writeMapleAsciiString("");
            return mplew.getPacket();
        }

        public static byte[] guildCapacityChange(int gid, int capacity) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(111); // 1.2.351 -2
            mplew.writeInt(gid);
            mplew.write(capacity);

            return mplew.getPacket();
        }

        public static byte[] guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(115); // 1.2.351 -7
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getJobId());

            return mplew.getPacket();
        }

        public static byte[] guildMemberOnline(int gid, int cid, boolean bOnline) { // 1.2.329 OK
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(116);  // 1.2.351 -7
            mplew.writeInt(gid);
            mplew.writeInt(cid);
            mplew.write(bOnline ? 1 : 0);
            if (!bOnline) {
                mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
            }
            mplew.write(1); // 274 ++

            return mplew.getPacket();
        }

        public static byte[] rankTitleChange(MapleCharacter chr, String[] ranks, int[] roles) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(122); // 1.2.351 -6
            mplew.writeInt(chr.getGuildId());
            mplew.writeInt(chr.getId());
            for (int i = 0; i < ranks.length; i++) {
                mplew.writeInt(roles[i]);
                mplew.writeMapleAsciiString(ranks[i]);
            }
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(0);
                mplew.writeMapleAsciiString("");
            }


            return mplew.getPacket();
        }

        public static byte[] changeRank(MapleGuildCharacter mgc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(128); // 1.2.351 -2
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            mplew.write(mgc.getGuildRank());

            return mplew.getPacket();
        }

        public static byte[] guildContribution(int gid, int cid, int c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(130);  // 1.2.351 -2
            mplew.writeInt(gid);
            mplew.writeInt(cid);
            mplew.writeInt(c); // TODO : 현재 기여도
            mplew.writeInt(Math.min(c, 5000)); // TODO : 오늘 기여도
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis() + (12 * 60 * 60 * 1000)));
            return mplew.getPacket();
        }

        public static byte[] guildEmblemChange(MapleCharacter chr, short bg, byte bgcolor, short logo, byte logocolor) { // 1.2.329 OK
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(132); // 1.2.351 -2
            mplew.writeInt(chr.getGuildId());
            mplew.writeInt(chr.getId());
            mplew.write(0); //
            mplew.writeShort(bg);
            mplew.write(bgcolor);
            mplew.writeShort(logo);
            mplew.write(logocolor);
            mplew.write(0); // 351 new
            mplew.writeInt(0); // customEmblemPacket
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] updateGP(int gid, int GP, int glevel) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(151); // 1.2.351
            mplew.writeInt(gid);
            mplew.writeInt(GP); // 길드 명성치
            mplew.writeInt(glevel);
            mplew.writeInt(150); // 길드 포인트
            mplew.writeInt(0); // 332 ++

            return mplew.getPacket();
        }

        public static byte[] guildNotice(MapleCharacter chr, String notice) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(141); // 1.2.351
            mplew.writeInt(chr.getGuildId());
            mplew.writeInt(chr.getId()); // 329++
            mplew.writeMapleAsciiString(notice);

            return mplew.getPacket();
        }

        public static byte[] guildRankingRequest() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(147); // 1.2.351
            return mplew.getPacket();
        }

        public static byte[] showGuildRanks(byte type, MapleGuildRanking ranks) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SHOW_GUILD_RANK.getValue());
            mplew.write(type);

            if (type == 1 || type == 2) {
                for (int i = 0; i < 3; ++i) {
                    List<GuildRankingInfo> rank = null;
                    switch (i) {
                        case 0:
                            rank = ranks.getFlagRaceRank();
                            break;
                        case 1:
                            rank = ranks.getHonorRank();
                            break;
                        case 2:
                            rank = ranks.getCulvertRank();
                            break;
                    }
                    if (rank == null) {
                        mplew.writeInt(0);
                    } else {
                        mplew.writeInt(rank.size());
                        for (GuildRankingInfo info : rank) {
                            mplew.writeInt(info.getId());
                            mplew.writeInt(info.getScore());
                            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
                            mplew.writeMapleAsciiString(info.getName());
                        }
                    }
                }
            }

            return mplew.getPacket();
        }

        public static byte[] getGuildRanksInfo(int rank1, int rank2, int rank3) { // 1.2.343
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(0x40); //+4
            mplew.writeShort(rank1);
            mplew.writeShort(rank2);
            mplew.writeShort(rank3);
            return mplew.getPacket();
        }

        public static byte[] guildSkillPurchased(int gid, int sid, int level, long expiration, String purchase, String activate) { // 1.2.329 OK
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(159);
            mplew.writeInt(gid);
            mplew.writeInt(sid);
            mplew.writeInt(0);
            mplew.writeShort(level);
            mplew.writeLong(PacketHelper.getTime(expiration));
            mplew.writeMapleAsciiString(purchase);
            mplew.writeMapleAsciiString(activate);
            return mplew.getPacket();
        }

        public static byte[] guildLeaderChanged(int gid, int oldLeader, int newLeader, int allianceId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(0xAE); // +5 343>?
            mplew.writeInt(gid);
            mplew.writeInt(oldLeader);
            mplew.writeInt(newLeader);
            mplew.write(0); //
            mplew.write(1); //new rank lol
            mplew.writeInt(allianceId);

            return mplew.getPacket();
        }

        public static byte[] denyGuildInvitation(String charname) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(103); // maybe
            mplew.writeMapleAsciiString(charname);

            return mplew.getPacket();
        }

        public static byte[] genericGuildMessage(int code) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
            mplew.write(code);

            switch (code) {
                case 0x87:
                    mplew.writeInt(0);// 보유 노블레스스포
                    break;
            }
            if (code == 92 || code == 127 || code == 135 || code == 118) {
                mplew.writeInt(11);
            }
            if (code == 5 || code == 0x67 || code == 0x68 || code == 0x64 || code == 0x65 || code == 0x66) {
                mplew.writeMapleAsciiString("");
            }
            return mplew.getPacket();
        }
    }

    public static class PartyPacket {

        public static byte[] partyCreated(MapleParty party) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(16); //
            mplew.writeInt(party.getId());
            mplew.writeInt(party.getLeader().getDoorTown());
            mplew.writeInt(party.getLeader().getDoorTarget());
            mplew.writeInt(0); //Grade? What does mean?
            mplew.writeShort(party.getLeader().getDoorPosition().x);
            mplew.writeShort(party.getLeader().getDoorPosition().y);
            mplew.write(party.getVisible());
            mplew.write(0); //262 ++ unk
            mplew.write(1); //262 ++ unk
            mplew.writeMapleAsciiString(party.getPatryTitle());
            return mplew.getPacket();
        }

        public static byte[] partyInvite(MapleCharacter from) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(4);
            mplew.writeInt(from.getParty() == null ? 0 : from.getParty().getId());
            mplew.writeMapleAsciiString(from.getName());
            mplew.writeInt(from.getLevel()); //
            mplew.writeInt(from.getJob()); //
            mplew.writeInt(1);
            mplew.write(0); // > 0 then won't send..
            mplew.write(0); // 274 ++

            return mplew.getPacket();
        }

        public static byte[] partyRequestInvite(MapleCharacter from) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(7); //274 +1
            mplew.writeInt(from.getId());
            mplew.writeMapleAsciiString(from.getName());
            mplew.writeInt(from.getLevel());
            mplew.writeInt(from.getJob());
            mplew.writeInt(0); //274 ++

            return mplew.getPacket();
        }

        public static byte[] partyStatusMessage(int message, String charname) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // 13: Already have joined a party.
            // 14: A beginner can't create a party.
            // 17: You have yet to join a party.
            // 20: You have joined the party.
            // 21: Already have joined a party.
            // 22: The party you're trying to join is already in full capacity.
            // 26: You have invited '%s' to your party.
            // 33: Cannot kick another user in this map
            // 36: This can only be given to a party member within the vicinity.
            // 37: Unable to hand over the leadership post; No party member is currently within the vicinity of the party leader.
            // 38: You may only change with the party member that's on the same channel.
            // 40: As a GM, you're forbidden from creating a party.
            // 45: The party leader has changed the party join request acceptance setting.
            // 46: Party settings could not be changed. Please try again later.
            // 51: Cannot be done in the current map.
            // 52: You've requested to join %s's party.
            // default: Your request for a party didn't work due to an unexpected error.
            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(message);
            if (message == 26 || message == 52) {
                mplew.writeMapleAsciiString(charname);
            } else if (message == 45) {
                mplew.write(0); // some mode?..
            }
            return mplew.getPacket();
        }

        public static void addPartyStatus(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew, boolean leaving) {
            addPartyStatus(forchannel, party, lew, leaving, false);
        }

        public static void addPartyStatus(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew, boolean leaving, boolean exped) {
            List<MaplePartyCharacter> partymembers;
            if (party == null) {
                partymembers = new ArrayList<>();
            } else {
                partymembers = new ArrayList<>(party.getMembers());
            }
            while (partymembers.size() < 6) {
                partymembers.add(new MaplePartyCharacter());
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getId()); // 24
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeAsciiString(partychar.getName(), 13); // 78
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getJobId()); // 24
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.isOnline() ? 1 : 0);
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getLevel()); // 24
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.isOnline() ? (partychar.getChannel() - 1) : -2); //24
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(0); //24
            }
            for (MaplePartyCharacter partychar : partymembers) { // 274 ++
                lew.writeInt(0); //24
            }
            lew.writeInt(party == null ? 0 : party.getLeader().getId());
            if (exped) {
                return;
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getChannel() == forchannel ? partychar.getMapid() : 0);  // 24
            }
            for (MaplePartyCharacter partychar : partymembers) { // 120
                if (partychar.getChannel() == forchannel && !leaving) {
                    lew.writeInt(partychar.getDoorTown());
                    lew.writeInt(partychar.getDoorTarget());
                    lew.writeInt(partychar.getDoorSkill());
                    lew.writeInt(partychar.getDoorPosition().x);
                    lew.writeInt(partychar.getDoorPosition().y);
                } else {
                    lew.writeInt(leaving ? 999999999 : 0);
                    lew.writeLong(leaving ? 999999999 : 0);
                    lew.writeLong(leaving ? -1 : 0);
                }
            }
            lew.write(party.getVisible());
            lew.write(0); // 274 ++
            lew.writeMapleAsciiString(party.getPatryTitle());
        }

        public static byte[] updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            switch (op) {
                case DISBAND:
                case EXPEL:
                case LEAVE:
                    mplew.write(21); //342 ok
                    mplew.writeInt(party.getId());
                    mplew.writeInt(target.getId());
                    mplew.write(op == PartyOperation.DISBAND ? 0 : 1);
                    if (op == PartyOperation.DISBAND) {
                        mplew.writeInt(target.getId());
                        break;
                    } else {
                        if (op == PartyOperation.EXPEL) {
                            mplew.write(1);
                        } else {
                            mplew.write(0);
                        }
                        mplew.writeMapleAsciiString(target.getName());
                        addPartyStatus(forChannel, party, mplew, op == PartyOperation.LEAVE);
                        break;
                    }
                case JOIN:
                    mplew.write(25); //342 ok
                    mplew.writeZeroBytes(5);
                    /*mplew.writeInt(party.getId());
                    mplew.writeMapleAsciiString(target.getName());
                    mplew.write(0); //274 ++
                    mplew.writeInt(0); //274 ++
                    addPartyStatus(forChannel, party, mplew, false);*/
                    break;
                case SILENT_UPDATE:
                case LOG_ONOFF:
                    mplew.write(15); //342
                    mplew.writeInt(party.getId());
                    addPartyStatus(forChannel, party, mplew, op == PartyOperation.LOG_ONOFF);
                    break;
                case CHANGE_LEADER:
                case CHANGE_LEADER_DC:
                    mplew.write(51); //274 +3
                    mplew.writeInt(target.getId());
                    mplew.write(op == PartyOperation.CHANGE_LEADER_DC ? 1 : 0);
                    break;
                case CHANGE_PARTY_TITLE:
                    mplew.write(82);
                    mplew.write(party.getVisible());
                    mplew.writeMapleAsciiString(party.getPatryTitle());
                    mplew.writeInt(target.getId());
                    mplew.writeInt(party.getId());
                    mplew.writeInt(1);
                    mplew.writeInt(1);
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] partyPortal(int townId, int targetId, int skillId, Point position, boolean animation) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(86);
            mplew.write(animation ? 0 : 1);
            mplew.writeInt(townId);
            mplew.writeInt(targetId);
            mplew.writeInt(skillId);
            mplew.writePos(position);

            return mplew.getPacket();
        }

        //no clue for below atm
        public static byte[] getPartyListing(final PartySearchType pst) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(83);
            mplew.writeInt(pst.id);
            final List<PartySearch> parties = World.Party.searchParty(pst);
            mplew.writeInt(parties.size());
            for (PartySearch party : parties) {
                mplew.writeInt(0); //ive no clue,either E8 72 94 00 or D8 72 94 00
                mplew.writeInt(2); //again, no clue, seems to remain constant?
                if (pst.exped) {
                    MapleExpedition me = World.Party.getExped(party.getId());
                    mplew.writeInt(me.getType().maxMembers);
                    mplew.writeInt(party.getId());
                    mplew.writeAsciiString(party.getName(), 48);
                    for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                        if (i < me.getParties().size()) {
                            MapleParty part = World.Party.getParty(me.getParties().get(i));
                            if (part != null) {
                                addPartyStatus(-1, part, mplew, false, true);
                            } else {
                                mplew.writeZeroBytes(202); //length of the addPartyStatus.
                            }
                        } else {
                            mplew.writeZeroBytes(202); //length of the addPartyStatus.
                        }
                    }
                } else {
                    mplew.writeInt(0);
                    mplew.writeInt(party.getId());
                    mplew.writeAsciiString(party.getName(), 48);
                    addPartyStatus(-1, World.Party.getParty(party.getId()), mplew, false, true); //if exped, send 0, if not then skip
                }

                mplew.writeShort(0); //wonder if this goes here or at bottom
            }

            return mplew.getPacket();
        }

        public static byte[] partyListingAdded(final PartySearch ps) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
            mplew.write(93);
            mplew.writeInt(ps.getType().id);
            mplew.writeInt(0); //ive no clue,either 48 DB 60 00 or 18 DB 60 00
            mplew.writeInt(1);
            if (ps.getType().exped) {
                MapleExpedition me = World.Party.getExped(ps.getId());
                mplew.writeInt(me.getType().maxMembers);
                mplew.writeInt(ps.getId());
                mplew.writeAsciiString(ps.getName(), 48);
                for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                    if (i < me.getParties().size()) {
                        MapleParty party = World.Party.getParty(me.getParties().get(i));
                        if (party != null) {
                            addPartyStatus(-1, party, mplew, false, true);
                        } else {
                            mplew.writeZeroBytes(202); //length of the addPartyStatus.
                        }
                    } else {
                        mplew.writeZeroBytes(202); //length of the addPartyStatus.
                    }
                }
            } else {
                mplew.writeInt(0); //doesn't matter
                mplew.writeInt(ps.getId());
                mplew.writeAsciiString(ps.getName(), 48);
                addPartyStatus(-1, World.Party.getParty(ps.getId()), mplew, false, true); //if exped, send 0, if not then skip
            }
            mplew.writeShort(0); //wonder if this goes here or at bottom

            return mplew.getPacket();
        }

        public static byte[] showMemberSearch(List<MapleCharacter> chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MEMBER_SEARCH.getValue());
            mplew.write(chr.size());
            for (MapleCharacter c : chr) {
                mplew.writeInt(c.getId());
                mplew.writeMapleAsciiString(c.getName());
                mplew.writeShort(c.getJob());
                mplew.write(c.getLevel());
            }
            return mplew.getPacket();
        }

        public static byte[] showPartySearch(List<MapleParty> chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.PARTY_SEARCH.getValue());
            mplew.write(chr.size());
            for (MapleParty c : chr) {
                mplew.writeInt(c.getId());
                mplew.writeMapleAsciiString(c.getLeader().getName());
                mplew.write(c.getLeader().getLevel());
                mplew.write(c.getLeader().isOnline() ? 1 : 0);
                mplew.write(c.getMembers().size());
                for (MaplePartyCharacter ch : c.getMembers()) {
                    mplew.writeInt(ch.getId());
                    mplew.writeMapleAsciiString(ch.getName());
                    mplew.writeShort(ch.getJobId());
                    mplew.write(ch.getLevel());
                    mplew.write(ch.isOnline() ? 1 : 0);
                }
            }
            return mplew.getPacket();
        }
    }
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="ExpeditionPacket">

    public static class ExpeditionPacket {

        public static byte[] expeditionStatus(final MapleExpedition me, boolean created) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(created ? 74 : 76); // 72(silent), 74(A new expedition has been created), 76("You have joined the expedition)
            mplew.writeInt(me.getType().exped);
            mplew.writeInt(0); //eh?
            for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                if (i < me.getParties().size()) {
                    MapleParty party = World.Party.getParty(me.getParties().get(i));
                    if (party != null) {
                        PartyPacket.addPartyStatus(-1, party, mplew, false, true);
                    } else {
                        mplew.writeZeroBytes(202); //length of the addPartyStatus.
                    }
                } else {
                    mplew.writeZeroBytes(202); //length of the addPartyStatus.
                }
            }
            //mplew.writeShort(0); //wonder if this goes here or at bottom

            return mplew.getPacket();
        }

        public static byte[] expeditionError(final int errcode, final String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // 0 : '<Name>' could not be found in the current server.
            // 1 : Admins can only invite other admins.
            // 2 : '<Name>' is already in a party.
            // 3 : '<Name>' does not meet the level requirement for the expedition.
            // 4 : '<Name>' is currently not accepting any expedition invites.
            // 5 : '<Name>' is taking care of another invitation.
            // 6 : You have already invited '<Name>' to the expedition.
            // 7 : '<Name>' has been invited to the expedition.
            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(88);
            mplew.writeInt(errcode);
            mplew.writeMapleAsciiString(name);

            return mplew.getPacket();
        }

        public static byte[] expeditionMessage(final int code) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // 73 : Silent remove
            // 77 : You have joined the expedition.
            // 80 : You have left the expedition.
            // 82 : You have been kicked out of the expedition.
            // 83 : The Expedition has been disbanded.
            // 89 : You cannot create a Cygnus Expedition because you have reached the limit on Cygnus Clear Points.\r\nCygnus can be defeated up to 3 times a week, and the Clear
            // 90 : You cannot invite this character to a Cygnus Expedition because he has reached the limit on Cygnus Clear Points. Cygnus can be defeated up to 3 times a week, and the Clear Points reset on Wednesdays at midnight.
            // 91 : You cannot join the Cygnus Expedition because you have reached the limit on Cygnus Clear Points.\r\nCygnus can be defeated up to 3 times a week, and the Clear Points reset on Wednesdays at midnight.
            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(code);

            return mplew.getPacket();
        }

        public static byte[] expeditionJoined(final String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(75); // 75 : '<Name>' has joined the expedition.
            mplew.writeMapleAsciiString(name);

            return mplew.getPacket();
        }

        public static byte[] expeditionLeft(final String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(79); // 79 : '<Name>' has left the expedition.
            mplew.writeMapleAsciiString(name);
            // 81 : '<Name>' has been kicked out of the expedition.

            return mplew.getPacket();
        }

        public static byte[] expeditionLeaderChanged(final int newLeader) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(84);
            mplew.writeInt(newLeader);

            return mplew.getPacket();
        }

        public static byte[] expeditionUpdate(final int partyIndex, final MapleParty party) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(85);
            mplew.writeInt(0); //lol?
            mplew.writeInt(partyIndex);
            if (party == null) {
                mplew.writeZeroBytes(202); //length of the addPartyStatus.
            } else {
                PartyPacket.addPartyStatus(-1, party, mplew, false, true);
            }
            return mplew.getPacket();
        }

        public static byte[] expeditionInvite(MapleCharacter from, int exped) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
            mplew.write(87);
            mplew.writeInt(from.getLevel());
            mplew.writeInt(from.getJob());
            mplew.writeMapleAsciiString(from.getName());
            mplew.writeInt(exped);

            return mplew.getPacket();
        }
    }

    public static class BuddylistPacket {

        public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist, BuddylistEntry buddies, byte op) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(op);
            if (op == 20) {
                mplew.writeInt(buddylist.size());
                for (BuddylistEntry buddy : buddylist) {
                    mplew.writeInt(buddy.getCharacterId()); // dwCharacterID
                    mplew.writeAsciiString(buddy.getName(), 13); // sFriendName
                    mplew.write(7); // bAccountFriend
                    mplew.writeInt(buddy.getChannel() == -1 ? -1 : (buddy.getChannel() - 1));
                    mplew.writeAsciiString(buddy.getGroupName(), 18); // sGroupName
                    mplew.writeInt(buddy.getAccountId()); // dwAccountID
                    mplew.writeAsciiString(buddy.getRepName(), 13); // sFriendNick
                    mplew.writeAsciiString(buddy.getMemo(), 260); // sFriendMemo
                }

            } else if (op == 23) { //updateFriend
                mplew.writeInt(buddies.getCharacterId());
                mplew.writeInt(buddies.getAccountId());

                mplew.writeInt(buddies.getCharacterId());
                mplew.writeAsciiString(buddies.getName(), 13);
                mplew.write(7);//buddies.isVisible() ? 0 : 7);
                mplew.writeInt(buddies.getChannel() == -1 ? -1 : (buddies.getChannel() - 1));
                mplew.writeAsciiString(buddies.getGroupName(), 18);
                mplew.writeInt(buddies.getAccountId());
                mplew.writeAsciiString(buddies.getRepName(), 13);
                mplew.writeAsciiString(buddies.getMemo(), 261);
            } else if (op == 38) {
                mplew.writeInt(buddies.getCharacterId());
                mplew.writeAsciiString(buddies.getName(), 13);
                mplew.write(7);//buddies.isVisible() ? 0 : 7);
                mplew.writeInt(buddies.getChannel() == -1 ? -1 : (buddies.getChannel() - 1));
                mplew.writeAsciiString(buddies.getGroupName(), 18);
                mplew.writeInt(buddies.getAccountId());
                mplew.writeAsciiString(buddies.getRepName(), 13);
                mplew.writeAsciiString(buddies.getMemo(), 260);
            }
            return mplew.getPacket();
        }

        public static byte[] deleteBuddy(int accId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(41);
            mplew.write(1);
            mplew.writeInt(accId);
            return mplew.getPacket();
        }

        public static byte[] buddyAddMessage(String charname) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(25); //1++
            mplew.writeMapleAsciiString(charname);
            return mplew.getPacket();
        }

        public static byte[] buddyDeclineMessage(String charname) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(50); //1++
            mplew.writeMapleAsciiString(charname);
            return mplew.getPacket();
        }

        public static byte[] requestBuddylistAdd(int cidFrom, int accId, String nameFrom, int levelFrom, int jobFrom, MapleClient c, String groupName, String memo) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(24); //19
            mplew.write(1); // isAccountFriend
            mplew.writeInt(cidFrom);
            mplew.writeInt(accId);
            mplew.writeMapleAsciiString(nameFrom);
            mplew.writeInt(levelFrom);
            mplew.writeInt(jobFrom);
            mplew.writeInt(0); // subJob

            mplew.writeInt(cidFrom);
            mplew.writeAsciiString(nameFrom, 13);
            mplew.write(6);
            mplew.writeInt(c.getChannel() - 1);
            mplew.writeAsciiString(groupName, 18);

            mplew.writeInt(accId);
            mplew.writeAsciiString(nameFrom, 13);
            mplew.writeAsciiString(memo, 260);
            return mplew.getPacket();
        }

        public static byte[] updateBuddyChannel(int characterid, int accountId, int channel, String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(43); // 342 45 -> 42
            mplew.writeInt(characterid);
            mplew.writeInt(accountId);
            mplew.write(1);
            mplew.writeInt(channel);
            mplew.write(1); // isAccountFriend
            mplew.write(1);
            mplew.writeMapleAsciiString(name); //1.2.220+

            return mplew.getPacket();
        }

        public static byte[] updateBuddyCapacity(int capacity) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(44);
            mplew.write(capacity);

            return mplew.getPacket();
        }

        public static byte[] buddylistMessage(byte message) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
            mplew.write(message);
            return mplew.getPacket();
        }
    }

    public static class AlliancePacket {

        public static byte[] getAllianceInfo(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(12);
            mplew.write(alliance == null ? 0 : 1); //in an alliance
            if (alliance != null) {
                addAllianceInfo(mplew, alliance);
            }

            return mplew.getPacket();
        }

        private static void addAllianceInfo(MaplePacketLittleEndianWriter mplew, MapleGuildAlliance alliance) {
            mplew.writeInt(alliance.getId());
            mplew.writeMapleAsciiString(alliance.getName());
            for (int i = 1; i <= 5; i++) {
                mplew.writeMapleAsciiString(alliance.getRank(i));
            }
            mplew.write(alliance.getNoGuilds());
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                mplew.writeInt(alliance.getGuildId(i));
            }
            mplew.writeInt(alliance.getCapacity()); // maxMemberNum
            mplew.writeMapleAsciiString(alliance.getNotice());
        }

        public static byte[] getGuildAlliance(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(13);
            if (alliance == null) {
                mplew.writeInt(0);
                return mplew.getPacket();
            }
            final int noGuilds = alliance.getNoGuilds();
            MapleGuild[] g = new MapleGuild[noGuilds];
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                g[i] = World.Guild.getGuild(alliance.getGuildId(i));
                if (g[i] == null) {
                    continue;
                }
            }
            mplew.writeInt(noGuilds);
            for (MapleGuild gg : g) {
                if (gg != null) {
                    GuildPacket.getGuildInfo(mplew, gg);
                }
            }
            return mplew.getPacket();
        }

        public static byte[] allianceMemberOnline(int alliance, int gid, int id, boolean online) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(14);
            mplew.writeInt(alliance);
            mplew.writeInt(gid);
            mplew.writeInt(id);
            mplew.write(online ? 1 : 0);
            mplew.write(online ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] removeGuildFromAlliance(MapleGuildAlliance alliance, MapleGuild expelledGuild, boolean expelled) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(16);
            addAllianceInfo(mplew, alliance);
            mplew.writeInt(expelledGuild.getId()); // 325++
            if (expelledGuild != null) {
                GuildPacket.getGuildInfo(mplew, expelledGuild);
            }
            mplew.write(expelled ? 1 : 0); //1 = expelled, 0 = left

            return mplew.getPacket();
        }

        public static byte[] addGuildToAlliance(MapleGuildAlliance alliance, MapleGuild newGuild) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(18);
            addAllianceInfo(mplew, alliance);
            mplew.writeInt(newGuild.getId());
            if (newGuild != null) {
                GuildPacket.getGuildInfo(mplew, newGuild);
            }
            mplew.write(0); // not here

            return mplew.getPacket();
        }

        public static byte[] sendAllianceInvite(String allianceName, MapleCharacter inviter) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(3);
            mplew.writeInt(inviter.getGuildId());
            mplew.writeMapleAsciiString(inviter.getName());
            mplew.writeMapleAsciiString(allianceName);

            return mplew.getPacket();
        }

        public static byte[] getAllianceUpdate(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(23);
            addAllianceInfo(mplew, alliance);

            return mplew.getPacket();
        }

        public static byte[] createGuildAlliance(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(15);
            addAllianceInfo(mplew, alliance);
            final int noGuilds = alliance.getNoGuilds();
            MapleGuild[] g = new MapleGuild[noGuilds];
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                g[i] = World.Guild.getGuild(alliance.getGuildId(i));
                if (g[i] == null) {
                    continue;
                }
            }
            for (MapleGuild gg : g) {
                if (gg != null) {
                    GuildPacket.getGuildInfo(mplew, gg);
                }
            }
            return mplew.getPacket();
        }

        public static byte[] updateAlliance(MapleGuildCharacter mgc, int allianceid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(24);
            mplew.writeInt(allianceid);
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getJobId());

            return mplew.getPacket();
        }

        public static byte[] updateAllianceLeader(int allianceid, int newLeader, int oldLeader) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(25);
            mplew.writeInt(allianceid);
            mplew.writeInt(oldLeader);
            mplew.writeInt(newLeader);

            return mplew.getPacket();
        }

        public static byte[] allianceRankChange(int aid, String[] ranks) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(26);
            mplew.writeInt(aid);
            for (String r : ranks) {
                mplew.writeMapleAsciiString(r); // x5
            }

            return mplew.getPacket();
        }

        public static byte[] updateAllianceRank(MapleGuildCharacter mgc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(27);
            mplew.writeInt(mgc.getId());
            mplew.write(mgc.getAllianceRank());

            return mplew.getPacket();
        }

        public static byte[] changeAllianceNotice(int allianceid, String notice) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(29);
            mplew.writeInt(allianceid);
            mplew.writeMapleAsciiString(notice);

            return mplew.getPacket();
        }

        public static byte[] disbandAlliance(int alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(30);
            mplew.writeInt(alliance);

            return mplew.getPacket();
        }

        public static byte[] changeAlliance(MapleGuildAlliance alliance, final boolean in) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(1);
            mplew.write(in ? 1 : 0);
            mplew.writeInt(in ? alliance.getId() : 0);
            final int noGuilds = alliance.getNoGuilds();
            MapleGuild[] g = new MapleGuild[noGuilds];
            for (int i = 0; i < noGuilds; i++) {
                g[i] = World.Guild.getGuild(alliance.getGuildId(i));
                if (g[i] == null) {
                    continue;
                }
            }
            mplew.write(noGuilds);
            for (int i = 0; i < noGuilds; i++) {
                if (g[i] != null) {
                    mplew.writeInt(g[i].getId());
                    //must be world
                    Collection<MapleGuildCharacter> members = g[i].getMembers();
                    mplew.writeInt(members.size());
                    for (MapleGuildCharacter mgc : members) {
                        mplew.writeInt(mgc.getId());
                        mplew.write(in ? mgc.getAllianceRank() : 0);
                    }
                }
            }

            return mplew.getPacket();
        }

        public static byte[] changeAllianceLeader(int allianceid, int newLeader, int oldLeader) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(2);
            mplew.writeInt(allianceid);
            mplew.writeInt(oldLeader);
            mplew.writeInt(newLeader);

            return mplew.getPacket();
        }

        public static byte[] changeGuildInAlliance(MapleGuildAlliance alliance, MapleGuild guild, final boolean add) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(4);
            mplew.writeInt(add ? alliance.getId() : 0);
            mplew.writeInt(guild.getId());
            Collection<MapleGuildCharacter> members = guild.getMembers();
            mplew.writeInt(members.size());
            for (MapleGuildCharacter mgc : members) {
                mplew.writeInt(mgc.getId());
                mplew.write(add ? mgc.getAllianceRank() : 0);
            }

            return mplew.getPacket();
        }

        public static byte[] changeAllianceRank(int allianceid, MapleGuildCharacter player) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
            mplew.write(5);
            mplew.writeInt(allianceid);
            mplew.writeInt(player.getId());
            mplew.writeInt(player.getAllianceRank());

            return mplew.getPacket();
        }
    }
    //</editor-fold>

    public static byte[] enableActions(MapleCharacter chr) {
        return updatePlayerStats(new EnumMap<MapleStat, Long>(MapleStat.class), true, false, chr, false);
    }

    public static byte[] enableActions(MapleCharacter chr, boolean itemReaction) {
        return updatePlayerStats(new EnumMap<MapleStat, Long>(MapleStat.class), itemReaction, chr);
    }

    public static byte[] enableActions(MapleCharacter chr, boolean itemReaction, boolean itemReaction2) {
        return updatePlayerStats(new EnumMap<MapleStat, Long>(MapleStat.class), itemReaction, itemReaction2, chr, false);
    }

    public static byte[] updatePlayerStats(final Map<MapleStat, Long> stats, final MapleCharacter chr) {
        return updatePlayerStats(stats, true, chr);
    }

    public static byte[] onSkillUseResult(int skillId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SKILL_USE_RESULT.getValue());
        mplew.write(true);
        mplew.writeInt(skillId);
        return mplew.getPacket();
    }

    public static byte[] updateZeroSecondStats(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_ZERO_STATS.getValue());
        PacketHelper.addZeroInfo(mplew, chr);
        return mplew.getPacket();
    }

    public static byte[] updateAngelicBusterInfo(MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_ANGELIC_STATS.getValue());
        mplew.writeInt(chr.getSecondFace());
        int hair = chr.getSecondHair();
        if (chr.getSecondBaseColor() != -1) {
            hair = chr.getSecondHair() / 10 * 10 + chr.getSecondBaseColor();
        }
        mplew.writeInt(hair);
        mplew.writeInt(1051291); //dressup suit cant unequip
        mplew.write(chr.getSecondSkinColor());
        mplew.writeInt(chr.getSecondBaseColor());
        mplew.writeInt(chr.getSecondAddColor());
        mplew.writeInt(chr.getSecondBaseProb());
        return mplew.getPacket();
    }

    public static byte[] updatePlayerStats(final Map<MapleStat, Long> mystats, final boolean itemReaction, final MapleCharacter chr) {
        return updatePlayerStats(mystats, itemReaction, itemReaction, chr, false);
    }

    public static byte[] updatePlayerStats(final Map<MapleStat, Long> mystats, final boolean itemReaction, final boolean itemReaction2, final MapleCharacter chr, boolean isPet) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(itemReaction ? 1 : 0);
        mplew.write(isPet);
        mplew.write(0); // 350 new
        int updateMask = 0;
        for (MapleStat statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeInt(updateMask);
        for (final Entry<MapleStat, Long> statupdate : mystats.entrySet()) {
            switch (statupdate.getKey()) {
                case SKIN:
                case BATTLE_RANK:
                case ICE_GAGE:
                    mplew.write((statupdate.getValue()).byteValue());
                    break;
                case JOB:
                    mplew.writeShort(statupdate.getValue().shortValue());
                    mplew.writeShort(chr.getSubcategory());
                    break;
                case STR:
                case DEX:
                case INT:
                case LUK:
                case AVAILABLEAP:
                case FATIGUE:
                    mplew.writeShort((statupdate.getValue()).shortValue());
                    break;
                case AVAILABLESP:
                    if (GameConstants.isSeparatedSp(chr.getJob())) {
                        mplew.write(chr.getRemainingSpSize());
                        for (int i = 0; i < chr.getRemainingSps().length; i++) {
                            if (chr.getRemainingSp(i) > 0) {
                                mplew.write(i + 1);
                                mplew.writeInt(chr.getRemainingSp(i));
                            }
                        }
                    } else {
                        mplew.writeShort(0);//chr.getRemainingSp());
                    }
                    break;
                case TRAIT_LIMIT:
                    for (MapleTraitType t : MapleTraitType.values()) {
                        mplew.writeShort(chr.getTrait(t).getExp()); // today's trait points
                    }
                    mplew.write(0);
                    mplew.writeLong(PacketHelper.getTime(-2));
                    break;
                case EXP:
                case MESO:
                    mplew.writeLong((statupdate.getValue()).longValue());
                    break;
                case PET:
                    mplew.writeLong((statupdate.getValue()).intValue());
                    mplew.writeLong((statupdate.getValue()).intValue());
                    mplew.writeLong((statupdate.getValue()).intValue());
                    break;
                case BATTLE_POINTS:
                case VIRTUE:
                    mplew.writeLong((statupdate.getValue()).longValue());
                    break;
                default:
                    mplew.writeInt((statupdate.getValue()).intValue());
                    break;
            }
        }
        mplew.write(chr == null ? -1 : chr.getBaseColor());
        mplew.write(chr == null ? 0 : chr.getAddColor());
        mplew.write(chr == null ? 0 : chr.getBaseProb());
        mplew.write(itemReaction2);
        if (itemReaction2) {
            mplew.write(1);
        }
        mplew.write(0);
        /*        if (false) {
         mplew.writeInt(0);
         mplew.writeInt(0);
         }*/
        return mplew.getPacket();
    }

    public static byte[] temporaryStats_Aran() { // used for mercedes tutorial also
        final Map<MapleStat.Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);

        stats.put(MapleStat.Temp.STR, 999);
        stats.put(MapleStat.Temp.DEX, 999);
        stats.put(MapleStat.Temp.INT, 999);
        stats.put(MapleStat.Temp.LUK, 999);
        stats.put(MapleStat.Temp.WATK, 255);
        stats.put(MapleStat.Temp.ACC, 999);
        stats.put(MapleStat.Temp.AVOID, 999);
        stats.put(MapleStat.Temp.SPEED, 140);
        stats.put(MapleStat.Temp.JUMP, 120);

        return temporaryStats(stats);
    }

    public static byte[] temporaryStats_Balrog(final MapleCharacter chr) {
        final Map<MapleStat.Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);

        int offset = 1 + (chr.getLevel() - 90) / 20; //every 20 levels above 90, +1
        stats.put(MapleStat.Temp.STR, chr.getStat().getTotalStr() / offset);
        stats.put(MapleStat.Temp.DEX, chr.getStat().getTotalDex() / offset);
        stats.put(MapleStat.Temp.INT, chr.getStat().getTotalInt() / offset);
        stats.put(MapleStat.Temp.LUK, chr.getStat().getTotalLuk() / offset);
        stats.put(MapleStat.Temp.WATK, chr.getStat().getTotalWatk() / offset);
        stats.put(MapleStat.Temp.MATK, chr.getStat().getTotalMagic() / offset);

        return temporaryStats(stats);
    }

    public static byte[] updateHyperSp(String value, int array, int mode, int table) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.HYPER.getValue());
        packet.writeMapleAsciiString(value);
        packet.writeInt(array);
        packet.writeInt(mode);
        packet.write(1);
        packet.writeInt(table);

        packet.writeZeroBytes(120);
        return packet.getPacket();
    }

    public static byte[] temporaryStats(final Map<MapleStat.Temp, Integer> mystats) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TEMP_STATS.getValue());
        int updateMask = 0;
        for (MapleStat.Temp statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeInt(updateMask);
        for (final Entry<MapleStat.Temp, Integer> statupdate : mystats.entrySet()) {
            switch (statupdate.getKey()) {
                case SPEED:
                case JUMP:
                case UNKNOWN:
                    mplew.write(statupdate.getValue().byteValue());
                    break;
                default:
                    mplew.writeShort(statupdate.getValue().shortValue());
                    break;
            }
        }

        return mplew.getPacket();
    }

    public static byte[] temporaryStats_Reset() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TEMP_STATS_RESET.getValue());

        return mplew.getPacket();
    }

    public static byte[] updateSkills(final Map<Skill, SkillEntry> update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.write(0);
        mplew.write(0);
        mplew.writeShort(update.size());
        for (final Entry<Skill, SkillEntry> z : update.entrySet()) {
            mplew.writeInt(z.getKey().getId());
            mplew.writeInt(z.getValue().skillevel);
            mplew.writeInt(z.getValue().masterlevel);
            PacketHelper.addExpirationTime(mplew, z.getValue().expiration);
        }
        mplew.write(4);

        return mplew.getPacket();
    }

    public static byte[] OnFameResult(final int op, final String charname, final boolean raise, final int newFame) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 1: The user name is incorrectly entered.
        // 2: Users under level l5 are unable to toggle with fame.
        // 3: You can't raise or drop a level anymore for today.
        // 4: You can't raise or drop a level of fame of that character anymore for this month.
        // default: The level of fame has neither been raise or dropped due to an unexpected error.
        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(op);

        mplew.writeMapleAsciiString(charname);
        mplew.write(raise);
        mplew.writeInt(newFame); //Change Short -> Int
        /* if (op == 0 || op == 5) { // Give / Receive Fame
         mplew.writeMapleAsciiString(charname == null ? "" : charname);
         mplew.write(raise ? 1 : 0); // 1 raise, 0 drop
         if (op == 0) { // Give				
         mplew.writeInt(newFame);
         }
         }*/

        return mplew.getPacket();
    }

    public static byte[] BombLieDetector(final boolean error, final int mapid, final int channel) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOMB_LIE_DETECTOR.getValue());
        mplew.write(error ? 2 : 1);
        mplew.writeInt(mapid);
        mplew.writeInt(channel); // 255 for all channels

        return mplew.getPacket();
    }

    public static byte[] report(final int mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 3: You have been reported by a user.
        // 65: Please try again later.
        // 66: Please re-check the character name, then try again.
        // 67: You do not have enough mesos to report.
        // 68: Unable to connect to the server.
        // 69: You have exceeded the number of reports available.
        // 71: You may only report from 0 to 0. -> Based on last packet.
        // 72: Unable to report due to previously being cited for a false report.
        mplew.writeShort(SendPacketOpcode.REPORT_RESPONSE.getValue());
        mplew.write(mode);
        if (mode == 2) { // You have successfully registered.
            mplew.write(0); // 0 or 1 only
            mplew.writeInt(1); // 오늘 신고 가능한 남은 횟수
            mplew.writeInt(0); // 325 추가됨
        }

        return mplew.getPacket();
    }

    public static byte[] OnSetClaimSvrAvailableTime(final int from, final int to) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);

        // You may only report from 8 to 10.
        mplew.writeShort(SendPacketOpcode.REPORT_TIME.getValue());
        mplew.write(from);
        mplew.write(to);

        return mplew.getPacket();
    }

    public static byte[] OnCoreEnforcementResult(int nSlot, int maxLevel, int currentlevel, int afterlevel) {
        MaplePacketLittleEndianWriter p = new MaplePacketLittleEndianWriter();
        p.writeShort(SendPacketOpcode.ENFORCE_CORE.getValue());
        p.writeInt(nSlot);
        p.writeInt(maxLevel);
        p.writeInt(currentlevel);
        p.writeInt(afterlevel);
        return p.getPacket();
    }

    public static byte[] OnClaimSvrStatusChanged(final boolean enable) { // Enable Report
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.REPORT_STATUS.getValue());
        mplew.write(enable ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] updateMount(MapleCharacter chr, boolean levelup) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getMount().getLevel());
        mplew.writeInt(chr.getMount().getExp());
        mplew.writeInt(chr.getMount().getFatigue());
        mplew.write(levelup ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getShowQuestCompletion(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeInt(id);

        return mplew.getPacket();
    }

    public static byte[] useSkillBook(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        mplew.write(0); //?
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] useAPSPReset(boolean spReset, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(spReset ? SendPacketOpcode.SP_RESET.getValue() : SendPacketOpcode.AP_RESET.getValue());
        mplew.write(1); // update tick
        mplew.writeInt(cid);
        mplew.write(1); // 0 = fail

        return mplew.getPacket();
    }

    public static byte[] expandCharacterSlots(final int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // -1: Your characters slots have already been expanded.
        // 0: Failed to expand character slots.
        // 1: You've increased your number of character slots.
        mplew.writeShort(SendPacketOpcode.EXPAND_CHARACTER_SLOTS.getValue());
        mplew.writeInt(mode);
        mplew.write(0); // idk, a boolean

        return mplew.getPacket();
    }

    public static byte[] finishedGather(int type) {
        return gatherSortItem(true, type);
    }

    public static byte[] finishedSort(int type) {
        return gatherSortItem(false, type);
    }

    public static byte[] gatherSortItem(boolean gather, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(gather ? SendPacketOpcode.FINISH_GATHER.getValue() : SendPacketOpcode.FINISH_SORT.getValue());
        mplew.write(type != 5);
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] updateGender(MapleCharacter chr) { // Send this upon entering cs
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_GENDER.getValue());
        mplew.write(chr.getGender());

        return mplew.getPacket();
    }

    public static byte[] updateDamageSkin(final MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_DAMAGE_SKIN.getValue());
        mplew.write(2);
        mplew.write(4);
        DamageSkinInfo(chr, mplew);
        return mplew.getPacket();
    }

    public static byte[] updateDailyGift(String key) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(34);
        mplew.writeInt(15); // key
        mplew.writeMapleAsciiString(key);

        return mplew.getPacket();
    }

    public static void DamageSkinInfo(final MapleCharacter chr, MaplePacketLittleEndianWriter mplew) {
        int skinid = (int) chr.getKeyValue(7293, "damage_skin");
        mplew.write(1);

        int skinnum = GameConstants.getDSkinNum(skinid);
        mplew.writeInt(skinnum);
        mplew.writeInt(skinid);
        mplew.write(0);
        mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(skinid) + "이다.\\r\\n\\r\\n\\r\\n\\r\\n\\r\\n");
        mplew.writeInt(0); // 324 ++

        mplew.writeInt(-1);
        mplew.writeInt(0);
        mplew.write(1); // 324 0-> 1
        mplew.writeMapleAsciiString("");
        mplew.writeInt(0); // 324 ++

        //332++
        mplew.writeInt(-1);
        mplew.writeInt(0);
        mplew.write(1); // 324 0-> 1
        mplew.writeMapleAsciiString("");
        mplew.writeInt(0); // 324 ++

        int skinroom = (int) chr.getKeyValue(13191, "skinroom");
        int skinsize = (int) chr.getKeyValue(13191, "skins");
        mplew.writeShort(skinroom == -1 ? 0 : skinroom);
        mplew.writeShort(skinsize == -1 ? 0 : skinsize);

        for (int i = 0; i < skinsize; i++) {
            int skin = (int) chr.getKeyValue(13191, i + "");
            mplew.writeInt(skin);
            mplew.writeInt(GameConstants.getItemIdbyNum(skin));
            mplew.write(0);
            mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(GameConstants.getItemIdbyNum(skin)) + "이다.\\r\\n\\r\\n\\r\\n\\r\\n\\r\\n");
            mplew.writeInt(0); // 324 ++
        }
    }

    public static byte[] charInfo(final MapleCharacter chr, final boolean isSelf) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId());
//        mplew.write(0);
        mplew.writeInt(chr.getLevel()); // 307++
        mplew.writeShort(chr.getJob());
        mplew.writeShort(chr.getSubcategory());

        mplew.write(chr.getStat().pvpRank);
        mplew.writeInt(chr.getFame());
        if (chr.getMarriageId() <= 0) {
            mplew.write(0);
        } else {
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(chr.getMarriageId());
            if (data == null || data.getStatus() < 1) {
                mplew.write(0);
            } else if (data.getStatus() >= 2) {
                mplew.write(1);
                MapleRing ring = chr.getRings(true).getRight().listIterator().next();
                if (ring == null) {
                    mplew.writeZeroBytes(48);
                } else {
                    mplew.writeInt(chr.getMarriageId());
                    mplew.writeInt(data.getBrideId() == chr.getId() ? data.getBrideId() : data.getGroomId());
                    mplew.writeInt(data.getBrideId() == chr.getId() ? data.getGroomId() : data.getBrideId());
                    mplew.writeShort(data.getStatus() == 2 ? 3 : data.getStatus()); //status 1 : 약혼  3 : 결혼
                    mplew.writeInt(ring.getItemId());
                    mplew.writeInt(ring.getItemId());
                    mplew.writeAsciiString(data.getBrideId() == chr.getId() ? data.getBrideName() : data.getGroomName(), 13);
                    mplew.writeAsciiString(data.getBrideId() == chr.getId() ? data.getGroomName() : data.getBrideName(), 13);
                }
            }
        }
        List<Integer> prof = chr.getProfessions();
        mplew.write(prof.size());

        for (Iterator<Integer> i$ = prof.iterator(); i$.hasNext(); ) {
            int i = ((Integer) i$.next()).intValue();
            mplew.writeShort(i);
        }

        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString("-");
            mplew.writeMapleAsciiString("");
        } else {
            MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                if (gs.getAllianceId() > 0) {
                    MapleGuildAlliance allianceName = World.Alliance.getAlliance(gs.getAllianceId());
                    if (allianceName != null) {
                        mplew.writeMapleAsciiString(allianceName.getName());
                    } else {
                        mplew.writeMapleAsciiString("");
                    }
                } else {
                    mplew.writeMapleAsciiString("");
                }
            } else {
                mplew.writeMapleAsciiString("-");
                mplew.writeMapleAsciiString("");
            }
        }

        mplew.write(isSelf ? -1 : 0);
        mplew.write(0);
        mplew.write(chr.getPets().length > 0 ? 1 : 0);

        Item inv = null;
        int peteqid = 0, petindex = 0, position = 114;
        for (final MaplePet pet : chr.getPets()) {
            if (pet != null) {
                if (petindex >= 1) {
                    position = 123 + petindex;
                }
                inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -position);
                peteqid = inv != null ? inv.getItemId() : 0;
                mplew.write(true);
                mplew.writeInt(petindex++);
                mplew.writeInt(pet.getPetItemId()); // petid
                mplew.writeMapleAsciiString(pet.getName());
                mplew.write(pet.getLevel()); // pet level
                mplew.writeShort(pet.getCloseness()); // pet closeness
                mplew.write(100);// pet.getFullness()); // pet fullness
                mplew.writeShort(pet.getFlags());
                mplew.writeInt(peteqid);
                mplew.writeInt(pet.getColor());
            }
        }

        mplew.write(false); // pet end

        Item medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -21);
        mplew.writeInt(medal == null ? 0 : medal.getItemId());
        List<Pair<Integer, Long>> medalQuests = chr.getCompletedMedals();
        mplew.writeShort(medalQuests.size());

        for (Pair<Integer, Long> x : medalQuests) {
            mplew.writeInt(((Integer) x.left).intValue());
            mplew.writeLong(((Long) x.right).longValue());
        }

        DamageSkinInfo(chr, mplew);

        for (MapleTrait.MapleTraitType t : MapleTrait.MapleTraitType.values()) {
            mplew.write(chr.getTrait(t).getLevel());
        }

        mplew.writeInt(chr.getAccountID()); // farm id
        PacketHelper.addFarmInfo(mplew, chr.getClient(), 0);
        return mplew.getPacket();
    }

    public static byte[] spawnPortal(final int townId, final int targetId, final int skillId, final Point pos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (townId != 999999999 && targetId != 999999999) {
            mplew.writeInt(skillId);
            mplew.writePos(pos);
        }

        return mplew.getPacket();
    }

    public static byte[] echoMegaphone(String name, String message) { // RAWR, removed by nexon
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ECHO_MESSAGE.getValue());
        mplew.write(0); //1 = Your echo message has been successfully sent
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.writeMapleAsciiString(name); //name
        mplew.writeMapleAsciiString(message); //message
        return mplew.getPacket();
    }

    public static byte[] showQuestMsg(String name, final String msg) {
        return serverNotice(5, name, msg);
    }

    public static byte[] Mulung_Pts(int recv, int total) {
        return showQuestMsg("", "You have received " + recv + " training points, for the accumulated total of " + total + " training points.");
    }

    public static byte[] serverMessage(String name, String message) {
        return serverMessage(4, 0, name, message, false);
    }

    public static byte[] serverNotice(int type, String name, String message) {
        return serverMessage(type, 0, name, message, false);
    }

    public static byte[] serverNotice(int type, String message) {
        return serverMessage(type, 0, "", message, false);
    }

    public static byte[] serverNotice(int type, int channel, String name, String message) {
        return serverMessage(type, channel, name, message, false);
    }

    public static byte[] serverNotice(int type, int channel, String name, String message, boolean smegaEar) {
        return serverMessage(type, channel, name, message, smegaEar);
    }

    public static byte[] serverMessage(int type, int channel, String name, String message, boolean megaEar) {
        return serverMessage(type, channel, name, message, megaEar, null);
    }

    public static byte[] serverMessage(int type, int channel, String name, String message, boolean megaEar, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 0: [Notice] <Msg>
        // 1: 창 띄우기
        // 2: 일반 확성기
        // 3: 고성능 확성기
        // 4: ?
        // 5: 분홍
        // 6: 파랑
        // 7: ?
        // 8: 아이템 확성기
        // 9: 초록색
        // 10: 세줄 확성기
        // 11 : 로얄스타일
        // 12 : ?
        // 15 : ?
        // 16 : 분홍
        // 18 : 파랑
        // 19 : 노랑
        // 23 : 고성능 확성기
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        if (type == 4 || type == 26) {
            mplew.write(true);
        }
        if (type != 13 && type != 14 && type != 27) {
            mplew.writeMapleAsciiString(message);
        }
        if (type == 3 || type == 23) {
            PacketHelper.ChatPacket(mplew, name, message);
            mplew.write(channel - 1);
            mplew.write(megaEar ? 1 : 0);
        } else if (type == 8) {
            PacketHelper.ChatPacket(mplew, name, message);
            mplew.write(channel - 1);
            mplew.write(megaEar ? 1 : 0);
            mplew.write(item != null); // isItem?
            if (item != null) {
                PacketHelper.addItemInfo(mplew, item);
            }
        } else if (type == 9) {
            PacketHelper.ChatPacket(mplew, name, message);
            mplew.write(channel - 1);
        } else if (type == 10) {
            PacketHelper.ChatPacket(mplew, name, message);
            mplew.write(0); // triple mega
            mplew.write(channel - 1);
            mplew.write(megaEar ? 1 : 0);
        } else if (type == 12) {
            mplew.writeInt(channel); // item id
        } else if (type == 24) {
            mplew.writeInt(0);
            mplew.writeInt(0); // time(second)
            mplew.write(false);//item != null); // isItem?
/*            if (item != null) {
             PacketHelper.addItemInfo(mplew, item); // 120 Byte
             }*/
        } else if (type == 17) {
            mplew.write(item != null); // isItem?
            if (item != null) {
                PacketHelper.addItemInfo(mplew, item);
            }
        }

        if (type != 21) {
            if (type == 22) {
                PacketHelper.addItemInfo(mplew, item);
            } else if (type == 26) {
                mplew.writeInt(channel - 1);
                mplew.writeInt(megaEar ? 1 : 0);
            }
        } else {
            PacketHelper.addItemInfo(mplew, item);
        }

        switch (type) {
            case 2:
                PacketHelper.ChatPacket(mplew, name, message);
                break;
            case 6:
            case 18:
                mplew.writeInt(channel >= 1000000 && channel < 6000000 ? channel : 0); // Item Id
                //E.G. All new EXP coupon {Ruby EXP Coupon} is now available in the Cash Shop!
                break;
            case 7:
                mplew.writeInt(channel); // ItemId
                break;
            case 11:
                mplew.writeInt(channel);
                mplew.write(item != null); // isItem?
                if (item != null) {
                    PacketHelper.addItemInfo(mplew, item);
                }
                break;
            case 16:
                mplew.writeInt(channel);
                break;
            case 20:
                mplew.writeInt(channel);
                mplew.writeInt(0);
                break;
//            case 8:
//                mplew.writeZeroBytes(30);
//                break;
        }

        return mplew.getPacket();
    }

    public static byte[] getGachaponMega(final String name, final String message, final Item item, final byte rareness, final String gacha) {
        return getGachaponMega(name, message, item, rareness, false, gacha);
    }

    public static byte[] getGachaponMega(final String name, final String message, final Item item, final byte rareness, final boolean dragon, final String gacha) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(13); // 15, 16 = twin dragon egg
        mplew.writeMapleAsciiString(name + message);
        if (!dragon) { // only for gachapon
            mplew.writeInt(0); // 0/1 = light blue
            mplew.writeInt(item.getItemId()); // item id
        }
        mplew.writeMapleAsciiString(gacha); // Gachapon Name
        PacketHelper.addItemInfo(mplew, item);

        return mplew.getPacket();
    }

    public static byte[] getAniMsg(final int questID, final int time, String name, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(23);
        PacketHelper.ChatPacket(mplew, name, text);
        mplew.writeInt(questID);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] tripleSmega(String name, List<String> message, boolean ear, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(10);
        if (message.get(0) != null) {
            mplew.writeMapleAsciiString(message.get(0));
        }
        PacketHelper.ChatPacket(mplew, name, message.get(0));
        mplew.write(message.size());
        for (int i = 1; i < message.size(); i++) {
            if (message.get(i) != null) {
                mplew.writeMapleAsciiString(message.get(i));
                PacketHelper.ChatPacket(mplew, name, message.get(i));
            }
        }
        mplew.write(channel - 1);
        mplew.write(ear ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] itemMegaphone(String name, String msg, boolean whisper, int channel, Item item, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(8);
        mplew.writeMapleAsciiString(msg);
        PacketHelper.ChatPacket(mplew, name, msg);
        mplew.write(channel - 1);
        mplew.write(whisper ? 1 : 0);
        mplew.writeInt(itemId);
        mplew.write(item != null);
        //PacketHelper.addItemPosition(mplew, item, true, false);
        if (item != null) {
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(item.getItemId()));
        }

        return mplew.getPacket();
    }

    public static byte[] getOwlOpen() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(0x0A);
        mplew.write(GameConstants.owlItems.length);
        for (int i : GameConstants.owlItems) {
            mplew.writeInt(i); //these are the most searched items. too lazy to actually make
        }
        return mplew.getPacket();
    }

    public static byte[] getOwlSearched(final int itemSearch, final List<HiredMerchant> hms) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(9);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(itemSearch);
        int size = 0;

        for (HiredMerchant hm : hms) {
            size += hm.searchItem(itemSearch).size();
        }
        mplew.writeInt(size);
        for (HiredMerchant hm : hms) {
            final List<MaplePlayerShopItem> items = hm.searchItem(itemSearch);
            for (MaplePlayerShopItem item : items) {
                mplew.writeMapleAsciiString(hm.getOwnerName());
                mplew.writeInt(hm.getMap().getId());
                mplew.writeMapleAsciiString(hm.getDescription());
                mplew.writeInt(item.item.getQuantity()); //I THINK.
                mplew.writeInt(item.bundles); //I THINK.
                mplew.writeLong(item.price);
                switch (InventoryHandler.OWL_ID) {
                    case 0:
                        mplew.writeInt(hm.getOwnerId()); //store ID
                        break;
                    case 1:
                        mplew.writeInt(hm.getStoreId());
                        break;
                    default:
                        mplew.writeInt(hm.getObjectId());
                        break;
                }
                mplew.write(hm.getFreeSlot() == -1 ? 1 : 0);
                mplew.write(GameConstants.getInventoryType(itemSearch).getType()); //position?
                if (GameConstants.getInventoryType(itemSearch) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(itemSearch) == MapleInventoryType.DECORATION) {
                    PacketHelper.addItemInfo(mplew, item.item);
                }
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getOwlMessage(final int msg) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        // 0: Success
        // 1: The room is already closed.
        // 2: You can't enter the room due to full capacity.
        // 3: Other requests are being fulfilled this minute.
        // 4: You can't do it while you're dead.
        // 7: You are not allowed to trade other items at this point.
        // 17: You may not enter this store.
        // 18: The owner of the store is currently undergoing store maintenance. Please try again in a bit.
        // 23: This can only be used inside the Free Market.
        // default: This character is unable to do it.		
        mplew.writeShort(SendPacketOpcode.OWL_RESULT.getValue());
        mplew.write(msg);

        return mplew.getPacket();
    }

    public static byte[] showWeddingInvitation(String groom, String bride, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENGAGE_RESULT.getValue());
        mplew.write(0x11);
        mplew.writeMapleAsciiString(groom);
        mplew.writeMapleAsciiString(bride);
        mplew.writeShort(type); //wedding type  0 : 조촐한, 1 : 스위티, 2 : 프리미엄
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishInputDialog() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENGAGE_REQUEST.getValue());
        mplew.write(9);
        return mplew.getPacket();
    }

    public static byte[] sendEngagementRequest(String name, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENGAGE_REQUEST.getValue());
        mplew.write(0); //mode, 0 = engage, 1 = cancel, 2 = answer.. etc
        mplew.writeMapleAsciiString(name); // name
        mplew.writeInt(cid); // playerid

        return mplew.getPacket();
    }

    public static byte[] sendEngagement(final byte msg, final int item, final MapleCharacter male, final MapleCharacter female) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // 11: You are now engaged.
        // 12: You are now married!
        // 13: Your engagement has been broken.
        // 14: You are no longer married.
        // 16: Congratulations!\r\nYour reservation was successfully made!
        // 18: You have entered the wrong character name.
        // 19: Your partner has to be in the same map.
        // 20: Your ETC slot is full.\r\nPlease remove some items.
        // 21: Your partner's ETC slots are full.
        // 22: You cannot be engaged to the same gender.
        // 23: You are already engaged.
        // 25: You are already married.
        // 24: She is already engaged.
        // 26: This person is already married.
        // 27: You're already in middle or proposing a person.
        // 28: She is currently being asked by another suitor.
        // 29: Unfortunately, the man who proposed to you has withdrawn his request for an engagement.
        // 30: She has politely declined your engagement request.
        // 31: The reservation has been canceled. Please try again later.
        // 32: You can't break the engagement after making reservations.
        // 34: This invitation is not valid.
        // 36: POPUP
        mplew.writeShort(SendPacketOpcode.ENGAGE_RESULT.getValue());
        mplew.write(msg); // 1103 custom quest
        if (msg == 13 || msg == 14) { // engage = 11, married = 12
            mplew.writeInt(male.getMarriageId()); // ringid or uniqueid
            mplew.writeInt(male.getId());
            mplew.writeInt(female.getId());
            mplew.writeShort(1); //always
            mplew.writeInt(item);
            mplew.writeInt(item); // wtf?repeat?
            mplew.writeAsciiString(male.getName(), 13);
            mplew.writeAsciiString(female.getName(), 13);
        } else if (msg == 15) { // Open Wedding invitation card
            mplew.writeAsciiString(male.getName(), 13);
            mplew.writeAsciiString(female.getName(), 13);
            mplew.writeShort(0); // type (Cathedral = 2, Vegas = other)
        }

        return mplew.getPacket();
    }

    public static byte[] sendWeddingGive() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(9);
        mplew.write(0); // item size, for each, additempos and additeminfo

        return mplew.getPacket();
    }

    public static byte[] sendWeddingReceive() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(10);
        mplew.writeLong(-1); // ?
        mplew.writeInt(0); // ?
        mplew.write(0);  // item size, for each, additempos and additeminfo		

        return mplew.getPacket();
    }

    public static byte[] giveWeddingItem() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(11); // 12: You cannot give more than one present for each wishlist. 13/14: Failed to send the gift.
        mplew.write(0); // for each : String
        mplew.writeLong(0); // could this be time?
        mplew.write(0); // size: For each: additeminfo (without pos)

        return mplew.getPacket();
    }

    public static byte[] receiveWeddingItem() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(15); // 16: idk.. 17: Item could not be retrieved\r\nbecause there was an item that\r\ncould only be acquired once.
        mplew.writeLong(0); // could this be time?
        mplew.write(0); // size: For each: additeminfo (without pos)

        return mplew.getPacket();
    }

    public static byte[] yellowChat(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.YELLOW_CHAT.getValue());
        mplew.write(-1); //could be something like mob displaying message.
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] catchMob(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CATCH_MOB.getValue());
        mplew.write(success);
        mplew.writeInt(itemid);
        mplew.writeInt(mobid);

        return mplew.getPacket();
    }

    public static byte[] spawnPlayerNPC(PlayerNPC npc, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_NPC.getValue());
        mplew.write(1); // Size
        mplew.writeInt(npc.getId());
        mplew.writeMapleAsciiString(npc.getName());
        PacketHelper.addCharLook(mplew, chr, true, GameConstants.isZero(chr.getJob()) && chr.getGender() == 1); // remove npc.getPet(i), npc.getF()?

        return mplew.getPacket();
    }

    public static byte[] sendLevelup(boolean family, int level, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LEVEL_UPDATE.getValue());
        mplew.write(family ? 1 : 2);
        mplew.writeInt(level);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendMarriage(boolean family, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MARRIAGE_UPDATE.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendJobup(boolean family, int jobid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.JOB_UPDATE.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeInt(jobid); //or is this a short
        mplew.writeMapleAsciiString((!family ? "> " : "") + name);

        return mplew.getPacket();
    }

    public static byte[] getAvatarMega(final MapleCharacter chr, final int channel, final int itemId, final List<String> text, final boolean ear) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(chr.getName());
        for (final String i : text) {
            mplew.writeMapleAsciiString(i);
        }
        PacketHelper.ChatPacket(mplew, chr.getName(), "");
        mplew.writeInt(channel - 1); // channel
        mplew.write(ear ? 1 : 0);
        PacketHelper.addCharLook(mplew, chr, true, GameConstants.isZero(chr.getJob()) && chr.getGender() == 1);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] pendantSlot(boolean p) { //slot -59
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PENDANT_SLOT.getValue());
        mplew.write(p ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getTopMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TOP_MSG.getValue());
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getMidMsg(String msg, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MID_MSG.getValue());
        mplew.writeInt(itemid); //where the message should appear on the screen
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] clearMidMsg() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLEAR_MID_MSG.getValue());

        return mplew.getPacket();
    }

    public static byte[] updateJaguar(MapleCharacter from) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_JAGUAR.getValue());
        PacketHelper.addJaguarInfo(mplew, from);

        return mplew.getPacket();
    }

    public static byte[] ultimateExplorer() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ULTIMATE_EXPLORER.getValue());

        return mplew.getPacket();
    }

    public static byte[] updateSpecialStat(String stat, int array, int mode, int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPECIAL_STAT.getValue());
        mplew.writeMapleAsciiString(stat);
        mplew.writeInt(array);
        mplew.writeInt(mode);
        mplew.write(1);
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] popupHomePage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.POPUP_HOMEPAGE.getValue());
        mplew.write(0); //274 ++
        mplew.write(1); //1 = enable, 0 = disable
        mplew.writeMapleAsciiString("http://www.naver.com");

        return mplew.getPacket();
    }

    public static byte[] updateAzwanFame(int fame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_HONOR.getValue());
//        mplew.write(0); // update
        mplew.writeInt(fame);

        return mplew.getPacket();
    }

    public static byte[] showPopupMessage(final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.POPUP_MSG.getValue());
        mplew.writeShort(11);
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static byte[] mannequinRes(byte type, byte result, int type2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MANNEQUIN_RES.getValue());
        mplew.write(type); // hair : 0, face : 1
        mplew.write(result);
        mplew.writeInt(type2);

        return mplew.getPacket();
    }

    public static byte[] mannequin(byte type, byte result, byte type2, byte slot, MapleMannequin mannequin) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MANNEQUIN.getValue());
        mplew.write(type); // hair : 0, face : 1, skin : 2
        mplew.write(result); // add : 1, save : 2, delete : 3, change : 4
        mplew.write(type2); // save : 2, add : 3, result : 4

        switch (type2) {
            case 1: // extend
                mplew.write(9); // 최대 사이즈
                mplew.write(9); // 현재 사용중인 사이즈
                break;
            case 2: // save
                mplew.write(slot);
                mplew.write(0);
                mplew.write(0);
                mplew.writeInt(mannequin.getValue());
                mplew.write(mannequin.getBaseProb());
                mplew.write(mannequin.getBaseColor());
                mplew.write(mannequin.getAddColor());
                break;
            case 3:
                mplew.write(8); // 최대 사이즈
                mplew.write(slot); // 현재 사용중인 사이즈
                mplew.write(slot - 1); // 새로 추가되는 슬롯 (항상 사이즈 - 1이 됨 ㅇㅇ)

                mplew.write(0);
                mplew.write(0);
                mplew.writeInt(0);
                mplew.write(-1);
                mplew.write(type == 1 || type == 2 ? -1 : 0);
                mplew.write(0);
                break;
            case 5:
                mplew.writeInt(0); // 333++
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] nameChangeUI(boolean use) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_NAME_CHANGE.getValue());
        mplew.writeInt(use ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] PsychicGrabPreparation(MapleCharacter chr, int skillid, short level, int unk, int speed, int[] unk2, int[] mob, short[] unk3, byte[] unk4, Point[] pos1, Point[] pos2, java.awt.Point[] pos3, java.awt.Point[] pos4, Point[] pos5) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PSYCHIC_GREP.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeShort(level);
        mplew.writeInt(unk);
        mplew.writeInt(speed);
        /* First AttackInfo End */
        MapleMonster target = null;
        int k = skillid == 142120000 ? 5 : 3;
        for (int i = 0; i < k; i++) {
            mplew.write(1);
            mplew.write(1);
            mplew.writeInt(unk2[i]);
            mplew.writeInt(i + 1);
            mplew.writeInt(mob[i]);
            mplew.writeShort(unk3[i]);
            if (mob[i] != 0) {
                target = chr.getMap().getMonsterByOid(mob[i]);
            }
            mplew.writeLong(mob[i] != 0 ? (int) target.getHp() : 100);
            mplew.writeLong(mob[i] != 0 ? (int) target.getHp() : 100);
            mplew.write(unk4[i]);
            mplew.writePos(pos1[i]);
            mplew.writePos(pos2[i]);
            mplew.writePos(pos3[i]);
            mplew.writePos(pos4[i]);
            mplew.writePos(pos5[i]);
        }
        mplew.write(0);
        /* PPoint Check */
        return mplew.getPacket();
    }

    public static byte[] UltimateMaterial(int code, int speed, int unk0, int skill, short level, byte unk1, short unk2, short unk3, short unk4, int posx, int posy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ULTIMATE_MATERIAL.getValue());
        mplew.writeInt(85930057); // ?
        mplew.writeInt(1);
        mplew.writeInt(code);
        mplew.writeInt(speed);
        mplew.writeInt(unk0);
        mplew.writeInt(skill);
        mplew.writeShort(level);
        mplew.writeInt(1);
        mplew.writeInt(14000);
        mplew.write(unk1);
        mplew.writeShort(unk2);
        mplew.writeShort(unk3);
        mplew.writeShort(unk4);
        mplew.writeInt(posx);
        mplew.writeInt(posy);
        return mplew.getPacket();
    }

    public static byte[] Test() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(448);
        mplew.write(HexTool.getByteArrayFromHexString("49 30 1F 05 00 00 00 00"));
        return mplew.getPacket();
    }

    public static byte[] Test1() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(449);
        mplew.write(HexTool.getByteArrayFromHexString("49 30 1F 05 00 00 00 00"));
        return mplew.getPacket();
    }

    public static byte[] AddCore(Core core) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ADD_CORE.getValue());
        mplew.writeInt(core.getCoreId());
        mplew.writeInt(core.getLevel());
        mplew.writeInt(core.getSkill1());
        mplew.writeInt(core.getSkill2());
        mplew.writeInt(core.getSkill3());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] UpdateCore(MapleCharacter chr) {
        return UpdateCore(chr, 0);
    }

    public static byte[] UpdateCore(MapleCharacter chr, int unk, int unk2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CORE_LIST.getValue());
        PacketHelper.addMatrixInfo(mplew, chr);
        mplew.write(unk);
        if (unk > 0) {
            mplew.writeInt(unk2);
        }
        return mplew.getPacket();
    }

    public static byte[] UpdateCore(MapleCharacter chr, int equip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CORE_LIST.getValue());
        PacketHelper.addMatrixInfo(mplew, chr);
        mplew.write(equip > 0); // ??
        if (equip > 0) {
            mplew.writeInt(equip);//0);//chr.getLevel() - 200 > 0 ? chr.getLevel() - 200 : 0); // ??
        }
        return mplew.getPacket();
    }

    public static byte[] DeleteCore(int count) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DELETE_CORE.getValue());

        mplew.writeInt(count); // 갯수

        return mplew.getPacket();
    }

    public static byte[] ViewNewCore(Core core, int nCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VIEW_CORE.getValue());
        mplew.writeInt(core.getCoreId());
        mplew.writeInt(core.getLevel());
        mplew.writeInt(core.getSkill1());
        mplew.writeInt(core.getSkill2());
        mplew.writeInt(core.getSkill3());
        mplew.writeInt(nCount);
        return mplew.getPacket();
    }

    public static byte[] openCore() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.OPEN_CORE.getValue());
        mplew.writeInt(2);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] PsychicUltimateDamager(int c, MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PSYCHIC_ULTIMATE.getValue());
        mplew.writeInt(player.getId());
        mplew.writeInt(c);

        return mplew.getPacket();

    }

    public static byte[] PsychicDamage(LittleEndianAccessor slea, final MapleClient c) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.PSYCHIC_DAMAGE.getValue());
        packet.writeInt(slea.readInt());
        packet.writeInt(1);

        return packet.getPacket();
    }

    public static byte[] OnCreatePsychicArea(int cid, int nAction, int ActionSpeed, int LocalKey, int SkillID, int SLV,
                                             int PsychicAreaKey, int DurationTime, int second, int SkeletonFieldPathIdx, int SkeletonAnildx,
                                             int SkeletonLoop, int mask8, int mask9) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.PSYCHIC_ATTACK.getValue());
        packet.writeInt(cid);
        packet.write(1);
        packet.writeInt(nAction);
        packet.writeInt(ActionSpeed); // ActionSpeed
        packet.writeInt(LocalKey); // LocalKey
        packet.writeInt(SkillID); // SkillID
        packet.writeShort(SLV); // SLV
        packet.writeInt(PsychicAreaKey); // PsychicAreaKey
        packet.writeInt(DurationTime + 4000); // DurationTime
        packet.write(second); // second
        packet.writeShort(SkeletonFieldPathIdx);
        packet.writeShort(SkeletonAnildx);
        packet.writeShort(SkeletonLoop);
        packet.writeInt(mask8);
        packet.writeInt(mask9);
        return packet.getPacket();
    }

    public static void CancelPsychicGrep(final LittleEndianAccessor rh, final MapleClient c) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.CANCEL_PSYCHIC_GREP.getValue());
        packet.writeInt(c.getPlayer().getId());
        packet.writeInt(rh.readInt());

        c.getSession().writeAndFlush(packet.getPacket());
    }

    public static byte[] MatrixSkill(int skillid, int level, List<MatrixSkill> skills) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MATRIX_SKILL.getValue());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(skills.size());
        for (MatrixSkill skill : skills) {
            mplew.writeInt(skill.getUnk1());
        }
        return mplew.getPacket();
    }

    public static byte[] MatrixSkillMulti(MapleCharacter chr, int skillid, int level, int unk1, int unk2, int bullet, List<MatrixSkill> skills) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MATRIX_MULTI.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(unk1); // unk
        mplew.writeInt(unk2); // unk
        mplew.writeInt(bullet); // bullet
        mplew.write(0); //상대방에게 이펙트 보임?

        mplew.writeInt(skills.size()); // size
        for (MatrixSkill skill : skills) {
            mplew.writeInt(skill.getSkill());
            mplew.writeInt(skill.getLevel());
            mplew.writeInt(skill.getUnk1());
            mplew.writeShort(skill.getUnk2());
            mplew.writePos(skill.getAngle());
            mplew.writeInt(skill.getUnk3());
            mplew.write(skill.getUnk4());
            mplew.write(skill.getUnk5()); // unk5?
            if (skill.getUnk5() > 0) {
                mplew.writeInt(skill.getX());
                mplew.writeInt(skill.getY());
            }
            mplew.write(skill.getUnk6());
            if (skill.getUnk6() > 0) {
                mplew.writeInt(skill.getX2());
                mplew.writeInt(skill.getY2());
            }
        }
        return mplew.getPacket();
    }

    public static byte[] Unlinkskill(int skillid, int linkedcid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNLINK_SKILL.getValue());
        mplew.writeInt(1);
        mplew.writeInt(skillid);
        mplew.writeInt(linkedcid); // ??
        return mplew.getPacket();
    }

    public static byte[] Unlinkskillunlock(int skillid, int unlock) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNLINK_SKILL_UNLOCK.getValue());
        mplew.writeInt(skillid);
        mplew.writeInt(unlock); // ??
        return mplew.getPacket();
    }

    public static byte[] Unlocklinkskill(int oriskillid, Map<Integer, Integer> unlock) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNLINK_SKILL.getValue());
        mplew.writeInt(unlock.size() + 1);
        mplew.writeInt(oriskillid);
        mplew.writeInt(unlock.size());
        for (Entry<Integer, Integer> skill : unlock.entrySet()) {
            mplew.writeInt(skill.getKey());
            mplew.writeInt(skill.getValue());
        }
        return mplew.getPacket();
    }

    public static byte[] Linkskill(int skillid, int sendid, int recvid, int level, int totalskilllv) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LINK_SKILL.getValue());
        PacketHelper.addLinkSkillInfo(mplew, skillid, sendid, recvid, level);

        boolean ordinarySkill = true;

        if (skillid >= 80000066 && skillid <= 80000070) {
            mplew.writeInt(80000055);
        } else if (skillid >= 80000333 && skillid <= 80000335 || skillid == 80000378) {
            mplew.writeInt(80000329);
        } else if (skillid >= 80002759 && skillid <= 80002761) {
            mplew.writeInt(80002758);
        } else if (skillid >= 80002763 && skillid <= 80002765) {
            mplew.writeInt(80002762);
        } else if (skillid >= 80002767 && skillid <= 80002769) {
            mplew.writeInt(80002766);
        } else if (skillid >= 80002771 && skillid <= 80002773) {
            mplew.writeInt(80002770);
        } else if (skillid >= 80002775 && skillid <= 80002776 || skillid == 80000000) {
            mplew.writeInt(80002774);
        } else {
            ordinarySkill = false;
            mplew.writeInt(0); // 사이즈 돌리는게 꼭 레지스탕스나 시그너스 스킬 같음
        }
        if (ordinarySkill) {
            mplew.writeInt(totalskilllv);
        }
        return mplew.getPacket();
    }

    public static byte[] AlarmAuction(MapleCharacter chr, AuctionItem item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ALARM_AUCTION.getValue());

        mplew.write(0);
        mplew.write(HexTool.getByteArrayFromHexString("41 54 5E 11")); //21 E2 7F 13 
        mplew.writeInt(0);
        mplew.writeLong(System.currentTimeMillis());
        mplew.writeInt(chr.getId());
        mplew.writeInt(item.getItem().getItemId());
        mplew.writeInt(item.getState());
        mplew.writeLong(item.getPrice());
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.writeInt(2000);
        mplew.writeInt(0);

        mplew.writeInt(item.getItem().getQuantity());
        mplew.writeInt(5);
        return mplew.getPacket();
    }

    public static byte[] onUserSoulMatching(int type, List<Pair<Integer, MapleCharacter>> chrs) {
        //UI : 184
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SOUL_MATCHING.getValue());
        mplew.writeInt(type);
        if (type == 0 || type == 1 || type == 2 || type == 10) {
            mplew.writeShort(chrs.size());
            for (Pair<Integer, MapleCharacter> chr : chrs) {
                mplew.writeInt(chr.getRight().getLevel());
                mplew.writeInt(chr.getRight().getJob());
                mplew.writeInt(chr.getLeft());
                mplew.writeInt(chr.getRight().getId());
            }
        } else if (type == 3) {
            mplew.writeInt(0); // ?
        }
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishGiveDialog(List<String> wishes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(9);
        mplew.write(wishes.size());
        for (String s : wishes) {
            mplew.writeMapleAsciiString(s);
        }
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishRecvDialog(Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(10);
        mplew.writeLong(0x7E);
        mplew.write(items.size()); //equip
        for (Item i : items) {
            PacketHelper.addItemInfo(mplew, i);
        }
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishRecvToLocalResult(Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(15);
        mplew.writeLong(0x7E);
        mplew.write(items.size()); //equip
        for (Item i : items) {
            PacketHelper.addItemInfo(mplew, i);
        }
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    // 아마 오류메시지를 dropMessage 1번으로 표시하고 이 패킷을 보내면 될 듯 하다.
    public static byte[] showWeddingWishRecvDisableHang() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(16);
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishGiveToServerResult(List<String> wishes, MapleInventoryType type, List<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(11);
        mplew.write(wishes.size());
        for (String s : wishes) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeZeroBytes(6);
        mplew.write(items.size()); //equip
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item);
        }
        return mplew.getPacket();
    }

    public static byte[] MiracleCirculator(List<InnerSkillValueHolder> newValues, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MIRACLE_CIRCULATOR.getValue());
        mplew.writeInt(newValues.size());
        for (int i = 0; i < newValues.size(); ++i) {
            mplew.writeInt(newValues.get(i).getSkillId());
            mplew.write(newValues.get(i).getSkillLevel());
            mplew.write(i + 1);
            mplew.write(newValues.get(i).getRank());
        }
        mplew.writeInt(itemId);
        mplew.writeLong(PacketHelper.getTime(-2)); // Idk, but it's longByte
        mplew.writeInt(6); //what
        return mplew.getPacket();
    }

    public static byte[] setBossReward(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_REWARD.getValue());
        Iterator<Item> ite = player.getInventory(MapleInventoryType.ETC).iterator();
        List<Item> items = new ArrayList<>();
        while (ite.hasNext()) {
            Item item = ite.next();
            if (item.getReward() != null) {
                items.add(item);
            }
        }

        mplew.writeInt(items.size());

        for (int i = 0; i < items.size(); i++) {
            mplew.writeLong(items.get(i).getReward().getObjectId());
            mplew.writeInt(items.get(i).getReward().getMobId());
            mplew.writeInt(items.get(i).getReward().getPartyId());
            mplew.writeInt(items.get(i).getReward().getPrice());
            mplew.writeLong(0);
        }

        return mplew.getPacket();
    }

    public static byte[] VMatrixOpen() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.VMATRIX_OPEN.getValue());
        mplew.writeInt(2);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] returnEffectConfirm(Equip item, int scrollId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RETURNEFFECT_CONFIRM.getValue());
        mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
        mplew.write(1);
        PacketHelper.addItemInfo(mplew, item);
        mplew.writeInt(scrollId);

        return mplew.getPacket();
    }

    public static byte[] returnEffectModify(Equip item, int scrollId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RETURNEFFECT_MODIFY.getValue());

        mplew.write(item != null);
        if (item != null) {
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeInt(scrollId);
        }

        return mplew.getPacket();
    }

    public static byte[] eliteWarning(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ELITE_WARNING.getValue());
        mplew.write(0);
        mplew.write(0); // 333++
        mplew.writeInt(0);
        mplew.writeInt(id);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] mixLense(int itemId, int baseFace, int newFace, boolean isDreeUp, boolean isBeta, boolean isAlphaBeta, MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MIX_LENSE.getValue());
        mplew.writeInt(itemId);
        mplew.write(true);
        mplew.write(isDreeUp || isBeta); // 엔버 드레스업?
        mplew.write(isAlphaBeta); // 제로 베타?
        mplew.write(0); //++342
        mplew.writeInt(1); // size

        mplew.writeInt(1); // size2

        mplew.write(2);
        mplew.writeInt(newFace);
        mplew.writeInt(baseFace);

        mplew.write(0);
        mplew.write(0);
        mplew.write(-1);
        mplew.write(-1);
        mplew.write(0);

        mplew.writeInt(0); // size3
        return mplew.getPacket();
    }

    public static byte[] setUnion(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SET_UNION.getValue());

        List<MapleUnion> equipped = new ArrayList<>();

        for (MapleUnion union : c.getPlayer().getUnions().getUnions()) {
            if (union.getPosition() >= 0) {
                equipped.add(union);
            }
        }

        mplew.writeInt(0);

        mplew.writeInt(c.getPlayer().getUnions().getUnions().size());
        for (MapleUnion chr : c.getPlayer().getUnions().getUnions()) {
            mplew.writeInt(1);
            mplew.writeInt(chr.getCharid());
            mplew.writeInt(chr.getLevel());
            mplew.writeInt(chr.getJob());
            mplew.writeInt(0); //
            mplew.writeInt(0); //
            mplew.writeInt(-1); // 여기선 무조건 -1
            mplew.writeInt(chr.getUnk3()); // ?
            mplew.writeMapleAsciiString(chr.getName());
        }

        mplew.writeInt(equipped.size());
        for (MapleUnion chr : equipped) { // 착용한 유니온 (위에 테트리스)
            mplew.writeInt(1);
            mplew.writeInt(chr.getCharid());
            mplew.writeInt(chr.getLevel());
            mplew.writeInt(chr.getJob());
            mplew.writeInt(chr.getUnk1());
            mplew.writeInt(chr.getUnk2());
            mplew.writeInt(chr.getPosition());
            mplew.writeInt(chr.getUnk3()); // ?
            mplew.writeMapleAsciiString(""); // 여기선 이름 안부르던데
        }
        mplew.write(0); // 메M 캐릭터

        return mplew.getPacket();
    }

    public static byte[] unionFreeset(MapleClient c, int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNION_FREESET.getValue());

        List<MapleUnion> equipped = new ArrayList<>();

        for (MapleUnion union : c.getPlayer().getUnions().getUnions()) {
            if (union.getPosition() != -1) {
                equipped.add(union);
            }
        }

        mplew.writeInt(id); // 0 ~ 4번 프리셋
        mplew.write(true);

        for (int i = 0; i < 8; ++i) {
            mplew.writeInt(i);
        }

        mplew.writeInt(equipped.size());
        for (MapleUnion chr : equipped) { // 착용한 유니온 (위에 테트리스)
            mplew.writeInt(1);
            mplew.writeInt(chr.getCharid());
            mplew.writeInt(chr.getLevel());
            mplew.writeInt(chr.getJob());
            mplew.writeInt(chr.getUnk1());
            mplew.writeInt(chr.getUnk2());
            mplew.writeInt(chr.getPosition());
            mplew.writeInt(chr.getUnk3()); // ?
            mplew.writeMapleAsciiString(""); // 여기선 이름 안부르던데
        }

        return mplew.getPacket();
    }

    public static byte[] equipmentEnchantResult(int op, Equip item, Equip item2, EquipmentScroll scroll, StarForceStats stats, int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.EQUIPMENT_ENCHANT.getValue());
        mplew.write(op);
        switch (op) {
            case 50: {
                mplew.write(false); // isFeverTime

                List<EquipmentScroll> ess = EquipmentEnchant.equipmentScrolls((Equip) item);

                mplew.write(ess.size());

                for (EquipmentScroll es : ess) {
                    mplew.writeInt(EquipmentEnchant.scrollType(es.getName()));
                    mplew.writeMapleAsciiString(es.getName());
                    mplew.writeInt(es.getName().contains("순백") ? 2 : es.getName().contains("이노센트") ? 1 : 0); // 특수 주문서
                    mplew.writeInt(es.getName().contains("아크") ? 4 : (es.getName().contains("이노센트") || es.getName().contains("순백")) ? 1 : 0); // 특수 주문서

                    mplew.writeInt(es.getFlag());

                    if (EnchantFlag.Watk.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Watk).right);
                    }

                    if (EnchantFlag.Matk.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Matk).right);
                    }

                    if (EnchantFlag.Str.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Str).right);
                    }

                    if (EnchantFlag.Dex.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Dex).right);
                    }

                    if (EnchantFlag.Int.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Int).right);
                    }

                    if (EnchantFlag.Luk.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Luk).right);
                    }

                    if (EnchantFlag.Wdef.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Wdef).right);
                    }

                    if (EnchantFlag.Mdef.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Mdef).right);
                    }

                    if (EnchantFlag.Hp.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Hp).right);
                    }

                    if (EnchantFlag.Mp.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Mp).right);
                    }

                    if (EnchantFlag.Acc.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Avoid).right);
                    }

                    if (EnchantFlag.Avoid.check(es.getFlag())) {
                        mplew.writeInt(es.getFlag(EnchantFlag.Avoid).right);
                    }

                    mplew.writeInt(es.getJuhun());
                    mplew.writeInt(es.getJuhun());
                    mplew.write(es.getName().contains("100%"));
                }

                break;
            }
            case 52: {
                //down, destroy, success, meso
                mplew.write(args[0] > 0 ? args[1] > 0 ? 2 : 1 : 0);

                double rate = (100 - ServerConstants.starForceSalePercent) / 100.0;

                long meso = (long) (args[3] * rate);
                if (meso < 0) {
                    meso = meso & 0xFFFFFFFFL;
                }
                mplew.writeLong(meso);
                mplew.writeLong(0);
                mplew.writeLong(ServerConstants.starForceSalePercent > 0 ? args[3] : 0);
                mplew.write(0); // mvp
                mplew.write(0); // pcRoom
                mplew.writeInt(args[2]);
                mplew.writeInt(args[1]);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.write(item.getEnchantBuff() & 0x20); // isChanceTime

                mplew.writeInt(stats.getFlag());

                if (EnchantFlag.Watk.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Watk).right);
                }

                if (EnchantFlag.Matk.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Matk).right);
                }

                if (EnchantFlag.Str.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Str).right);
                }

                if (EnchantFlag.Dex.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Dex).right);
                }

                if (EnchantFlag.Int.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Int).right);
                }

                if (EnchantFlag.Luk.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Luk).right);
                }

                if (EnchantFlag.Wdef.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Wdef).right);
                }

                if (EnchantFlag.Mdef.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Mdef).right);
                }

                if (EnchantFlag.Hp.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Hp).right);
                }

                if (EnchantFlag.Mp.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Mp).right);
                }

                if (EnchantFlag.Acc.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Avoid).right);
                }

                if (EnchantFlag.Avoid.check(stats.getFlag())) {
                    mplew.writeInt(stats.getFlag(EnchantFlag.Avoid).right);
                }

                break;
            }
            case 53: {
                mplew.write(false);
                mplew.writeInt(args[0]); // maybe tick
                break;
            }
            case 100: {
                mplew.write(false);
                mplew.writeInt(args[0]);
                mplew.writeMapleAsciiString(scroll.getName()); // scrollName
                PacketHelper.addItemInfo(mplew, item);

                PacketHelper.addItemInfo(mplew, item2);
                break;
            }
            case 101: {
                mplew.write(args[0]); // isSuccess
                mplew.writeInt(0);
                PacketHelper.addItemInfo(mplew, item);
                if (args[0] != 4) {
                    PacketHelper.addItemInfo(mplew, item2);
                }
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] onMesoPickupResult(int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MOB_DROP_MESO_PICKUP.getValue());
        mplew.writeInt(meso);

        return mplew.getPacket();
    }

    public static byte[] onSessionValue(String key, String value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SESSION_VALUE.getValue());
        mplew.writeMapleAsciiString(key);
        mplew.writeMapleAsciiString(value);
        return mplew.getPacket();
    }

    public static byte[] onFieldSetVariable(String key, String value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FIELD_SET_VARIABLE.getValue());
        mplew.writeMapleAsciiString(key);
        mplew.writeMapleAsciiString(value);
        return mplew.getPacket();
    }

    public static byte[] quickPass() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.QUICK_PASS.getValue());
        mplew.writeInt(1);
        return mplew.getPacket();
    }

    public static byte[] updateMaplePoint(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_MAPLEPOINT.getValue());
        mplew.writeInt(chr.getDonationPoint());
        return mplew.getPacket();
    }

    public static byte[] ArcaneCatalyst(final Equip equip, int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ARCANE_CATALYST.getValue());
        PacketHelper.addItemInfo(mplew, equip);
        mplew.writeInt(slot);
        return mplew.getPacket();
    }

    public static byte[] ArcaneCatalyst2(final Equip equip) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ARCANE_CATALYST2.getValue());
        PacketHelper.addItemInfo(mplew, equip);
        return mplew.getPacket();
    }

    public static byte[] useBlackRebirthScroll(Equip item, Item rebirth, long newRebirth, boolean result) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BLACK_REBIRTH_SCROLL.getValue());
        mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
        mplew.writeLong(result ? 0 : newRebirth); // 새로운 환불값
        mplew.writeInt(result ? 0 : item.getPosition()); // 빼박
        mplew.writeInt(result ? 0 : rebirth.getItemId());
        mplew.writeShort(0);//++342
        ////////////////
        mplew.writeInt(0); // ?
        mplew.writeInt(0); // ?
        return mplew.getPacket();
    }

    public static byte[] blackRebirthResult(boolean before, long newRebirth, Equip equip) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BLACK_REBIRTH_RESULT.getValue());
        mplew.writeInt(0);
        mplew.write(1); //0 > @렉 써야됨
        mplew.write(before ? 1 : 0); // 1 > before 0 > after
        GameConstants.sendFireOption(mplew, newRebirth, equip);
        PacketHelper.addItemInfo(mplew, equip);
        return mplew.getPacket();
    }

    public static byte[] goldApple(Item item, Item apple) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GOLD_APPLE.getValue());
        mplew.write(item != null);

        if (item != null) {
            mplew.writeInt(item.getItemId());
            mplew.writeShort(item.getQuantity());

            mplew.writeInt(apple.getItemId());
            mplew.writeInt(apple.getPosition());

            mplew.writeInt(2435458);
            mplew.writeInt(1);

            mplew.write(item.getType() == 1);

            if (item.getType() == 1) {
                PacketHelper.addItemInfo(mplew, item);
            }
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] followRequest(int chrid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_REQUEST.getValue());
        mplew.writeInt(chrid);

        return mplew.getPacket();
    }

    public static byte[] HyperMegaPhone(String msg, String name, String rawmsg, int channel, boolean ear, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(8);
        mplew.writeMapleAsciiString(msg);
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(rawmsg);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(channel - 1);
        mplew.write(ear ? 1 : 0);
        mplew.writeInt(5076100); // itemid
        mplew.write(item == null ? 0 : 1);
        if (item != null) {
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(item.getItemId()));
        }
        return mplew.getPacket();
    }

    public static byte[] getWhiteAdditionalCubeStart(MapleCharacter chr, Item item, boolean up, int cubeId, int cubeQuantity, int cubePosition) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHITE_ADDITIONAL_CUBE_WINDOW.getValue());
        mplew.writeLong(PacketHelper.getTime(-2));
        mplew.write(1);

        PacketHelper.addItemInfo(mplew, item);
        mplew.writeInt(cubeId);
        mplew.writeInt(item.getPosition());
        mplew.writeInt(cubeQuantity);
        mplew.writeInt(cubePosition);
        return mplew.getPacket();
    }
}
