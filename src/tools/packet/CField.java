package tools.packet;

import client.*;
import client.inventory.AuctionHistory;
import client.inventory.AuctionItem;
import client.inventory.Equip;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.inventory.MapleAndroid;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleRing;
import constants.GameConstants;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.channel.handler.AttackInfo;
import handling.channel.handler.PlayerInteractionHandler;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.w3c.dom.ranges.Range;
import scripting.EventInstanceManager;
import server.AdelProjectile;
import server.DailyGiftItemInfo;
import server.DimentionMirrorEntry;
import server.MapleDueyActions;
import server.MapleItemInformationProvider;
import server.MapleTrade;
import server.QuickMoveEntry;
import server.Randomizer;
import server.enchant.EnchantFlag;
import server.enchant.StarForceStats;
import server.field.skill.MapleFieldAttackObj;
import server.field.skill.MapleMagicWreck;
import server.life.MapleHaku;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.ForceAtom;
import server.maps.MapleAtom;
import server.maps.MapleDragon;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMist;
import server.maps.MapleNodes.Environment;
import server.maps.MapleReactor;
import server.maps.MapleRune;
import server.maps.MapleSpecialChair;
import server.maps.MapleSummon;
import server.maps.MechDoor;
import server.maps.SummonMovementType;
import server.movement.LifeMovementFragment;
import server.shops.MapleShop;
import tools.AttackPair;
import tools.HexTool;
import tools.Pair;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;
import static tools.packet.PacketHelper.getTime;

/**
 *
 * @author AlphaEta
 */
public class CField {

    public static byte[] getPacketFromHexString(final String hex) {
        return HexTool.getByteArrayFromHexString(hex);
    }

    public static byte[] getServerIP(final MapleClient c, final int port, final int clientId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVER_IP.getValue());
        mplew.writeShort(0); // 350 new
        mplew.writeShort(0);

        mplew.write(GameConstants.getServerIp(ServerConstants.Gateway_IP));
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.writeInt(1); // 350 modify
        mplew.writeInt(1); // 350 modify
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getChannelChange(final MapleClient c, final int port) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        mplew.write(GameConstants.getServerIp(ServerConstants.Gateway_IP));
        mplew.writeShort(port);
        return mplew.getPacket();
    }

    public static class EffectPacket {

        public static byte[] showSkillEffect(MapleCharacter chr, int skillid, boolean own) {
            return showEffect(chr, 0, skillid, 1, 0, 0, (byte) 0, own, null, null, null);
        }

        public static byte[] showSummonEffect(MapleCharacter chr, int skillid, boolean own) { // case 3
            return showEffect(chr, 0, 0, 3, 0, 0, (byte) 0, own, null, null, null);
        }

        public static byte[] showPortalEffect(int skillid) { // case 7
            return showEffect(null, 0, 0, 7, 0, 0, (byte) 0, true, null, null, null);
        }

        public static byte[] showDiceEffect(MapleCharacter chr, int oldskillid, int skillid, int subeffectid, int subeffectid2, boolean own) { // case 6
            return showEffect(chr, oldskillid, skillid, 6, subeffectid, subeffectid2, (byte) (chr.isFacingLeft() ? 1 : 0), own, chr.getTruePosition(), "", null);
        }

        public static byte[] showCharmEffect(MapleCharacter chr, int skillid, int subeffectid2, boolean own, String txt) { // case 8
            return showEffect(chr, 0, skillid, 8, 0, subeffectid2, (byte) 0, own, null, txt, null);
        }

        public static byte[] showPetLevelUpEffect(MapleCharacter chr, int skillid, boolean own) { // case 9
            return showEffect(chr, 0, skillid, 9, 0, 0, (byte) 0, own, null, null, null);
        }

        public static byte[] showRewardItemEffect(MapleCharacter chr, int skillid, boolean own, String txt) { // case 20
            return showEffect(chr, 0, skillid, 20, 0, 0, (byte) 0, own, null, txt, null);
        }

        public static byte[] showItemMakerEffect(MapleCharacter chr, int direction, boolean own) { // case 22
            return showEffect(chr, 0, 0, 22, 0, 0, (byte) direction, own, null, null, null);
        }

        public static byte[] showWheelEffect(int skillid) { // case 27
            return showEffect(null, 0, 0, 27, 0, 0, (byte) 0, true, null, null, null);
        }

        public static byte[] showWZEffect(String txt) { // case 28
            return showEffect(null, 0, 0, 28, 0, 0, (byte) 0, true, null, txt, null);
        }

        public static byte[] showHealEffect(MapleCharacter chr, int skillid, boolean own) { // case 37
            return showEffect(chr, 0, skillid, 37, 0, 0, (byte) 0, own, null, null, null);
        }

        public static byte[] showBoxEffect(MapleCharacter chr, int oldskillid, int skillid, boolean own) { // case 53
            return showEffect(chr, oldskillid, skillid, 53, 0, 0, (byte) 0, own, null, null, null);
        }

        public static byte[] showBurningFieldEffect(String txt) { // case 61
            return showEffect(null, 0, 0, 61, 0, 0, (byte) 0, true, null, txt, null);
        }

        public static byte[] showNormalEffect(MapleCharacter chr, int effectid, boolean own) { // default
            return showEffect(chr, 0, 0, effectid, 0, 0, (byte) 0, own, null, null, null);
        }

        public static byte[] showWillEffect(MapleCharacter chr, int subeffectid, int skillid, int skillLevel) { // default
            return showEffect(chr, skillLevel, skillid, 73, subeffectid, 0, (byte) 0, true, null, null, null);
        }

        public static byte[] showFieldSkillEffect(int skillid, int skillLevel) {
            return showEffect(null, skillLevel, skillid, 74, 0, 0, (byte) 0, true, null, null, null);
        }

        public static byte[] showOrgelEffect(MapleCharacter chr, int skillid, Point position) {
            return showEffect(chr, 0, skillid, 80, 0, 0, (byte) 0, true, position, null, null);
        }

        public static byte[] showTextEffect(MapleCharacter chr, String text, int flip, int textheight) {
            return showEffect(chr, flip, textheight, 61, 0, 0, (byte) 0, true, null, text, null);
        }

        public static byte[] showEffect(MapleCharacter chr, int oldskillid, int skillid, int effectid, int subeffectid, int subeffectid2, byte direction, boolean own, Point pos, String txt, Item item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            if (own) {
                mplew.writeShort(SendPacketOpcode.SHOW_EFFECT.getValue());
            } else {
                mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
                mplew.writeInt(chr.getId());
            }
            mplew.write(effectid); // 1,2

            switch (effectid) {
                case 26: {
                    mplew.write(false); // Flip
                    mplew.writeInt(0); // Range
                    mplew.writeInt(0); // NameHeight
                    mplew.writeMapleAsciiString(txt); // Msg
                    break;
                }
                case 29: {
                    mplew.writeMapleAsciiString(txt); // ItemTypeName
                    break;
                }
                case 30: {
                    boolean a = false;
                    mplew.write(a);
                    if (a) {
                        mplew.writeMapleAsciiString(txt); // ItemTypeName
                        mplew.writeInt(0); // Duration
                        mplew.writeInt(0); // Flip
                    }
                    break;
                }
                case 31: {
                    mplew.writeMapleAsciiString(txt); // ItemTypeName
                    mplew.writeInt(0); // Duration
                    mplew.writeInt(0); // Flip
                    break;
                }
                case 33: { // PlaySoundWithMuteBgm
                    mplew.writeMapleAsciiString(txt); // Smile
                    break;
                }
                case 34: { // PlaySoundWithMuteBgm
                    mplew.writeMapleAsciiString(txt); // Msg
                    mplew.writeInt(100); // Smile 0
                    break;
                }
                case 44: { // RegisterFadeInOutAnimation
                    mplew.writeInt(0); // FadeIn
                    mplew.writeInt(0); // Nameheight
                    mplew.writeInt(0); // Flip
                    mplew.write(0); // Alpha
                    break;
                }
                case 47: { // SetUserBlindEffect
                    mplew.write(false); // Flip
                    break;
                }
                case 55: {
                    mplew.writeMapleAsciiString(txt); // ItemName
                    boolean a = false;
                    mplew.write(a);
                    if (a) {
                        boolean b = false;
                        mplew.write(b);
                        if (b) {
                            mplew.write(false);
                            mplew.writeInt(0); // TextY
                            mplew.writeInt(0); // Flip
                        }
                    } else {
                        mplew.writeInt(0); // rx
                        mplew.writeInt(0); // ry
                        mplew.writeInt(0); // Duration (x1000안해도됨)
                    }
                    break;
                }
                case 60: { // Effect_NormalSpeechBalloon
                    mplew.write(false); // Normal
                    mplew.writeInt(0); // Range
                    mplew.writeInt(0); // NameHeight
                    mplew.writeMapleAsciiString(txt); // nX
                    mplew.writeInt(0); // SLV
                    mplew.writeInt(0); // Layer.Interface
                    mplew.writeInt(0); // basefont
                    mplew.writeInt(0); // v32
                    mplew.writeInt(0); // ItemTypeName
                    mplew.writeInt(0); // Smile
                    if (txt.length() > 0) {
                        mplew.writeInt(0); // Flip
                        mplew.writeInt(0);
                    }
                    break;
                }
                case 61: {
                    mplew.writeMapleAsciiString(txt);
                    mplew.writeInt(50); //textSpeed
                    mplew.writeInt(1500); //TextWidth
                    mplew.writeInt(4); //NameHeight
                    mplew.writeInt(0); //nameWidth
                    mplew.writeInt(-200); //Textheight
                    mplew.writeInt(1); //BaseHeight
                    mplew.writeInt(4); //TextY
                    mplew.writeInt(2); //bFlip
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeMapleAsciiString("");
                    mplew.writeInt(0); // 333++
                    mplew.write(0); // 333++
                    break;
                }
                case 63: { // FindNpcByTemplateID
                    mplew.writeInt(0); // npcId
                    mplew.writeInt(0);
                    mplew.writeInt(0); // PlateNo
                    mplew.writeInt(0); // Range
                    mplew.writeInt(0); // NameHeight
                    mplew.writeInt(0); // Flip
                    break;
                }
                case 64: {
                    mplew.writeInt(0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                    break;
                }
                case 65: { // UI/UIWindow.img/FloatNotice/%d/DrawOrigin/icon
                    PacketHelper.addItemInfo(mplew, item);
                    break;
                }
                case 80: { //오르골
                    mplew.writeInt(skillid);
                    mplew.writeInt(0);
                    mplew.writeInt(pos.x);
                    mplew.writeInt(pos.y);
                    break;
                }
                case 1:
                case 2: {
                    if (effectid == 2) {
                        mplew.writeInt(subeffectid2); // 274 ++, 스킬 시전자의 ID가 아닐까
                    }
                    mplew.writeInt(skillid);
                    mplew.writeInt(chr.getLevel()); //player level 307 Byte -> Int

                    mplew.writeInt(chr.getTotalSkillLevel(skillid) == 0 ? 1 : chr.getTotalSkillLevel(skillid)); //skill level
                    if (skillid == 22170074) {
                        mplew.write(0);
                    }
                    if (skillid == 1320016) {
                        mplew.write(chr.getReinCarnation());
                    }
                    if (skillid == 4331006) {
                        mplew.write(0);
                        mplew.writeInt(0);
                    }
                    if (skillid == 3211010 || skillid == 3111010 || skillid == 1100012) {
                        mplew.write(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    if (skillid == 64001000 || (skillid > 64001006 && skillid <= 64001008)) {
                        mplew.write(direction); // maybe?
                    }
                    if (((skillid - 64001009) >= -2 && (skillid - 64001009) <= 2)) {
                        mplew.write(direction);
                        mplew.writeInt(chr.getFH()); // FH는 아닌듯 하고.. Oid인가
                        chr.dropMessageGM(6, "FH : " + chr.getFH());
                        mplew.writeInt(pos.x);
                        mplew.writeInt(pos.y);
                    }
                    if (skillid == 64001012) {
                        mplew.write(direction);
                        mplew.writeInt(pos.x);
                        mplew.writeInt(pos.y);
                        mplew.writeInt(oldskillid);
                    }
                    if (skillid == 30001062) { // 헌터의 부름
                        mplew.write(0); // position of monster spawned
                        mplew.writeShort(pos.x);
                        mplew.writeShort(pos.y);
                    }
                    if (skillid == 30001061) { // 포획
                        mplew.write(direction); // boolean
                    }
                    if (skillid == 60001218 || skillid == 60011218 || skillid == 400001000) { // 로프 커넥트
                        mplew.writeInt(oldskillid);
                        mplew.writeInt(pos.x);
                        mplew.writeInt(pos.y);
                        mplew.write(true); // 332++
                    }
                    if (skillid == 400051025) {
                        mplew.writeInt(pos.x);
                        mplew.writeInt(pos.y);
                    }
                    if (skillid == 0x131CE06
                            || skillid == 0xE4E5BC + 1
                            || skillid == 0x131F552 + 2
                            || skillid == 0x404146 + 2
                            || skillid == 400041026
                            || skillid == 152001004) {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    if (skillid == 0x40687C || skillid == 65121052) { // screenEnter
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    if (GameConstants.sub_7F9870(skillid) > 0) { // 307++
                        mplew.writeInt(0);
                    }
                    if (skillid == 400041019) {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    if (skillid == 400041009) {
                        mplew.writeInt(0);
                    }
                    if ((skillid - 400041011) >= -4 && (skillid - 400041011) <= 4) {
                        mplew.writeInt(0);
                    }
                    if (skillid == 400041036) {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    if (skillid == 162121010) {
                        mplew.write(direction);
                        mplew.writeInt(pos.x);
                        mplew.writeInt(pos.y);
                    }
                    if (skillid != 152111005 && skillid != 152111006) {
                        if (skillid == 80002393 || skillid == 80002394 || skillid == 80002395 || skillid == 80002421) {
                            mplew.writeInt(0);
                        }
                        if (GameConstants.sub_8242D0(skillid)) {
                            mplew.write(0);
                        }
                    }
                    break;
                }
                case 3: {
                    mplew.writeInt(skillid);
                    mplew.writeInt(chr.getLevel());
                    mplew.write(chr.getSkillLevel(GameConstants.getLinkedSkill(skillid)));
                    break;
                }
                case 4: {
                    mplew.writeInt(skillid);
                    mplew.write(chr.getSkillLevel(GameConstants.getLinkedSkill(skillid)));
                    if (skillid == 31111003) {
                        mplew.writeInt(0);
                    }

                    if (skillid == 25121006) {
                        mplew.writeInt(0);
                    }
                    break;
                }
                case 5: {
                    mplew.writeInt(skillid);
                    mplew.write(chr.getSkillLevel(GameConstants.getLinkedSkill(skillid)));
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                }
                case 6: {
                    mplew.writeInt(subeffectid);
                    mplew.writeInt(subeffectid2);
                    mplew.writeInt(skillid);
                    mplew.write(chr.getSkillLevel(GameConstants.getLinkedSkill(skillid)));
                    mplew.write(oldskillid); // isDoubleDice?
                    break;
                }
                case 7: {
                    mplew.writeInt(skillid);
                    mplew.write(0);//chr.getSkillLevel(GameConstants.getLinkedSkill(skillid)));
                    break;
                }
                case 8: { // useCharm
                    mplew.write(subeffectid2); // size
                    for (int j = 0; j < subeffectid2; j++) {
                        mplew.writeInt(oldskillid); // itemId
                        mplew.writeInt(skillid); // quantity
                    }
                    mplew.write(0); //343++
//                    mplew.writeMapleAsciiString(txt);
                    //                  mplew.writeInt(skillid);
                    break;
                }
                case 9: { // showOwnPetLevelUp
                    mplew.write(0);
                    mplew.writeInt(chr.getPetIndex(skillid)); // pet index
                    break;
                }
                case 10: {
                    mplew.writeInt(skillid);
                    if (GameConstants.sub_1F04F40(skillid)) {
                        mplew.writeInt(pos.x);
                        mplew.writeInt(pos.y);
                        mplew.writeInt(chr.getSkillLevel(GameConstants.getLinkedSkill(skillid)));
                    }
                    if (skillid == 32111016) {
                        mplew.writeInt(0);
                        mplew.write(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    if (skillid == 0x4C4BC9E || skillid == 0x4C4B501 || skillid == 0x4C4B504 || skillid == 0x4C4BE27) {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                    break;
                }
                case 12: {
                    boolean i = false;
                    mplew.write(i);
                    mplew.write(0);
                    mplew.write(0); // LightnessOn
                    if (!false) {
                        mplew.writeInt(0);
                    }
                    break;
                }
                case 16: { // MakeIncDecHpEffect
                    mplew.write(subeffectid);
                    break;
                }
                case 17: { // Effect_BuffItemUse
                    mplew.writeInt(skillid);
                    break;
                }
                case 18: {
                    mplew.writeMapleAsciiString(txt);
                    break;
                }
                case 20: { // play_item_sound
                    mplew.writeInt(skillid);
                    mplew.write(txt.length() > 0);
                    if (txt.length() > 0) {
                        mplew.writeMapleAsciiString(txt);
                    }
                    break;
                }
                case 22: { // ItemMakerSuccess
                    mplew.writeInt(direction); // 0 = success, 1 = fail
                    break;
                }
                case 23: {
                    mplew.writeInt(0);
                    break;
                }
                case 25: {
                    mplew.writeInt(skillid);
                    break;
                }
                case 27: { // useWheel
                    mplew.write(chr.getInventory(MapleInventoryType.CASH).countById(skillid)); // 5510000
                    break;
                }
                case 28: { // showWzEffect
                    mplew.writeMapleAsciiString(txt);
                    break;
                }
                case 32: { // showWzEffect (Maybe)
                    mplew.writeInt(skillid);
                    mplew.writeMapleAsciiString(txt);
                    break;
                }
                case 36: { // TryHPRecoveryJust
                    mplew.writeInt(skillid);// Delta
                    mplew.write(subeffectid); // bGuard
                    mplew.write(false);
                    mplew.writeInt(skillid);
                    break;
                }
                case 37: { // TryHPRecoveryJust
                    mplew.writeInt(skillid); // Delta
                    break;
                }
                case 38: { // showCraftingEffect
                    mplew.writeMapleAsciiString(txt);
                    mplew.write(1);
                    mplew.writeInt(oldskillid); // time
                    mplew.writeInt(subeffectid);
                    if (subeffectid == 2) {
                        //Effect/BasicEff.img/JobChangedElf
                        mplew.writeInt(skillid); //itemID : 5155000
                    }
                    break;
                }
                case 39: { // DelayedPvPEffectTime
                    mplew.writeInt(0); // duration
                    break;
                }
                case 40: { // LoadPvPChampionEffect
                    mplew.writeInt(0); // duration
                    break;
                }
                case 45: { // ShowMobSkillHitEffect
                    mplew.writeInt(0); // MobSkillId
                    mplew.writeInt(0); // MobSKillLv
                    break;
                }
                case 46: { // Aswan/DefenceFail
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                }
                case 48: { // SetHitBossShieldEffect
                    mplew.writeInt(0); // itemId
                    mplew.writeInt(0); // count
                    break;
                }
                case 50: { // Effect_ItemUpgrade
                    int b = 0;
                    mplew.write(b);
                    switch (b) {
                        case 0:
                        case 2:
                        case 3:
                            mplew.writeInt(0); // itemId
                            break;
                        case 1:
                        case 4:
                            mplew.writeInt(0); // itemId
                            break;
                    }
                    break;
                }
                case 51: {
                    mplew.writeInt(0); // MagLevel
                    break;
                }
                case 53: {
                    mplew.write(0);
                    mplew.write(1);
                    mplew.writeInt(item.getItemId()); // ItemId
                    mplew.writeInt(1); // ItemQuantity
                    mplew.write(0);
                    break;
                }
                case 54: { // SetEffectLeftMonsterNumber
                    mplew.writeInt(0); // count
                    break;
                }
                case 56: { // RobbinsBombEffect
                    boolean reset = false;
                    mplew.write(reset);
                    if (!reset) {
                        mplew.writeInt(0); // BombCount
                        mplew.write(0); // NumberOnly
                    }
                    break;
                }
                case 57: {
                    mplew.writeInt(skillid);
                    mplew.writeInt(0); // mode
                    mplew.writeInt(0); // modeStatus
                    break;
                }
                case 58: { // Effect_ActQuestClear
                    mplew.writeInt(0); // QuestId
                    break;
                }
                case 59: { // Effect_Point
                    mplew.writeInt(pos.x); // pos.x
                    mplew.writeInt(pos.y);
                    break;
                }
                case 62: { // RegisterPreLoopEndEffect
                    mplew.writeInt(skillid);
                    mplew.writeInt(0); // duration
                    break;
                }
                case 66: { // Inflation
                    mplew.writeInt(0);
                    mplew.writeMapleAsciiString(txt);
                    break;
                }
                case 69: { // TryRegisterIncDecHPEffect
                    mplew.writeInt(0); // delta
                    mplew.writeInt(0); // delay
                    break;
                }
                case 70: {
                    boolean z = false;
                    mplew.write(z);
                    if (z) {
                        mplew.writeInt(0);
                    }
                    break;
                }
                case 71: { // OnFoxManActionSetUsed
                    mplew.writeShort(0); // type
                    mplew.writeInt(0); // EventID
                    mplew.write(false); // bUpgrade
                    mplew.write(0); // SLv
                    mplew.write(false); // ByServer
                    break;
                }
                case 72: {
                    mplew.writeInt(0);
                    PacketHelper.addItemInfo(mplew, item);
                    break;
                }
                case 73: {
                    sub_1E4D510(mplew, subeffectid, skillid, oldskillid);
                    break;
                }
                case 74: {
                    sub_1E4DCD0(mplew, skillid, oldskillid);
                    break;
                }
                case 75: {
                    mplew.writeInt(0);
                    break;
                }
                case 77: {
                    mplew.writeMapleAsciiString(txt);
                    break;
                }
                case 76: {
                    mplew.writeMapleAsciiString(txt);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.write(false);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                }
            }
            mplew.writeZeroBytes(4 * 10);
            return mplew.getPacket();
        }

        public static byte[] ShowRandomBoxEffect(int effectid, int itemid, List<Integer> item) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.OPEN_RANDOM_BOX.getValue());
            mplew.writeShort(effectid); //effectid?
            mplew.writeInt(itemid);
            mplew.write(0);
            mplew.writeInt(item.size());
            for (Integer i : item) {
                mplew.writeInt(i);
            }
            System.out.println("ShowRandomBoxEffect" + mplew);
            return mplew.getPacket();
        }

        public static byte[] KadenaMove(int cid, int skillid, int x, int y, int isLeft, short fh) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            if (cid == -1) {
                mplew.writeShort(SendPacketOpcode.SHOW_EFFECT.getValue());
            } else {
                mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
                mplew.writeInt(cid);
            }
            mplew.write(1);
            mplew.writeInt(skillid);
            mplew.writeInt(201); // 316 1 -> 4
            mplew.writeInt(1); // 316 1 -> 4
            mplew.writeInt(isLeft);
            mplew.write(0);
            mplew.writeInt(x);
            mplew.writeInt(y);
            mplew.writeShort(fh);
            return mplew.getPacket();
        }

        public static byte[] Orgel1(int skillid) { //초기화?
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MAGIC_ORGEL.getValue());
            mplew.writeInt(skillid);
            return mplew.getPacket();
        }

        public static byte[] OrgelCount(int skillid, int count) { //없앨땐 00 00 00 00 00 00 00 00
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MAGIC_ORGEL_COUNT.getValue());
            mplew.writeInt(skillid);
            mplew.writeInt(count);
            return mplew.getPacket();
        }

        public static byte[] OrgelTime(int skillid, int time, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MAGIC_ORGEL_TIME.getValue());
            mplew.writeInt(skillid);
            mplew.writeInt(time);
            mplew.writeInt(type); //type 2,3
            return mplew.getPacket();
        }

        public static byte[] OrgelStart(int skillid, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MAGIC_ORGEL_START.getValue());
            mplew.writeInt(skillid);
            mplew.writeInt(type);
            mplew.writeInt(20); //count
            return mplew.getPacket();
        }

        public static byte[] ErdaIncrease(Point position) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.ERDA_INCREASE.getValue());
            mplew.writeInt(position.x);
            mplew.writeInt(position.y);
            mplew.writeInt(6);
            int a = Randomizer.rand(5, 8);
            mplew.writeInt(a); //에르다갯수
            return mplew.getPacket();
        }

        public static byte[] sub_1E4D510(MaplePacketLittleEndianWriter mplew, int subeffectid, int skillid, int skillLevel) {
            mplew.write(subeffectid);
            mplew.writeInt(skillid);
            mplew.writeInt(skillLevel);
            return mplew.getPacket();

        }

        public static byte[] sub_1E4DCD0(MaplePacketLittleEndianWriter mplew, int skillId, int skillLv) {
            mplew.writeInt(skillId); // FieldSkillID 100006
            mplew.writeInt(skillLv); // FieldSkillLv

            if (skillId == 100017) {
                mplew.writeShort(Randomizer.nextInt(3)); // subSkillIdx
            }
            return mplew.getPacket();

        }

    }

    public static class UIPacket {

        public static byte[] getDirectionStatus(boolean enable) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.DIRECTION_STATUS.getValue());
            mplew.write(enable ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] openUI(final int type) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
            // 1207 : 아무것도 안했는데
            mplew.writeShort(SendPacketOpcode.OPEN_UI.getValue());
            mplew.writeInt(type);

            return mplew.getPacket();
        }

        public static byte[] closeUI(final int type) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.CLOSE_UI.getValue());
            mplew.writeInt(type);

            return mplew.getPacket();
        }

        public static byte[] openUIOption(final int type, final int option) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // 1st int: 3(Skill), 7(Maple User), 21(Party Search), 33(Repair)
            // 0: Buddy, 1: Party, 2: Expedition, 3: Guild, 4: Alliance, 5: Blacklist or npc id
            mplew.writeShort(SendPacketOpcode.OPEN_UI_OPTION.getValue());
            mplew.writeInt(type);
            mplew.writeInt(option);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] IntroLock(boolean enable) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.CYGNUS_INTRO_LOCK.getValue());
            mplew.write(enable ? 1 : 0); // 0 -> show
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] IntroEnableUI(int wtf) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.CYGNUS_INTRO_ENABLE_UI.getValue());
            mplew.write(wtf > 0 ? 1 : 0); // enable
            mplew.write(0); // blockFrame
            if (wtf > 0) {
                mplew.write(false); // forceMouseOver
                mplew.write(false); // showUI
            }
            return mplew.getPacket();
        }

        public static byte[] IntroDisableUI(boolean enable) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.CYGNUS_INTRO_DISABLE_UI.getValue());
            mplew.write(enable ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] summonHelper(boolean summon) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SUMMON_HINT.getValue());
            mplew.write(summon ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] summonMessage(int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SUMMON_HINT_MSG.getValue());
            mplew.write(1);
            mplew.writeInt(type);
            mplew.writeInt(7000); // probably the delay

            return mplew.getPacket();
        }

        public static byte[] summonMessage(String message) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SUMMON_HINT_MSG.getValue());
            mplew.write(0);
            mplew.writeMapleAsciiString(message);
            mplew.writeInt(200); // IDK
            mplew.writeShort(0);
            mplew.writeInt(10000); // Probably delay

            return mplew.getPacket();
        }

        public static byte[] getDirectionInfo(int type, int value) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.UserInGameDirectionEvent.getValue());
            mplew.write(type);
            mplew.writeLong(value);

            return mplew.getPacket();
        }

        public static byte[] getDirectionInfo(String data, int value, int x, int y, int a, int b) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.UserInGameDirectionEvent.getValue());
            mplew.write(2);
            mplew.writeMapleAsciiString(data);
            mplew.writeInt(value);
            mplew.writeInt(x);
            mplew.writeInt(y);
            mplew.write(a);
            if (a > 0) {
                mplew.writeInt(0);
            }
            mplew.write(b);
            if (b > 1) {
                mplew.writeInt(0);
                mplew.write(a);
                mplew.write(b);
            }

            return mplew.getPacket();
        }

        public static final byte[] playMovie(final String data, boolean show) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAY_MOVIE.getValue());
            mplew.writeMapleAsciiString(data);
            mplew.write(show ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] detailShowInfo(String msg, boolean RuneSystem) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.DETAIL_SHOW_INFO.getValue());
            mplew.writeInt(3); //color
            mplew.writeInt(RuneSystem ? 0x11 : 0x14); //width
            mplew.writeInt(RuneSystem ? 0 : 0x4); //heigh
            mplew.writeInt(0); //Unk
            mplew.write(false); // 325++
            mplew.writeMapleAsciiString(msg);

            return mplew.getPacket();
        }

        public static byte[] GreendetailShowInfo(String msg) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.DETAIL_SHOW_INFO.getValue());
            mplew.writeInt(3); //color
            mplew.writeInt(0x14); //width
            mplew.writeInt(0x14); //heigh
            mplew.writeInt(0); //Unk
            mplew.write(false); // 325++
            mplew.writeMapleAsciiString(msg);

            return mplew.getPacket();
        }

        public static byte[] OnSetMirrorDungeonInfo(boolean clear) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.MIRROR_DUNGEON_INFO.getValue());
            mplew.writeInt(clear ? 0 : GameConstants.dList.size());
            for (Pair<String, String> d : GameConstants.dList) {
                mplew.writeMapleAsciiString(d.left);
                mplew.writeInt(0);
                mplew.writeMapleAsciiString(d.right);
            }
            return mplew.getPacket();
        }
    }

    public static class AttackObjPacket {

        public static byte[] ObjCreatePacket(MapleFieldAttackObj fao) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SPAWN_FIELDATTACK_OBJ.getValue());
            mplew.writeInt(fao.getObjectId());
            mplew.writeInt(1); // key
            mplew.writeInt(fao.getChr().getId());
            mplew.writeInt(0); // ReserveCid
            mplew.write(false); // 307++
            mplew.writeInt(fao.getTruePosition().x);
            mplew.writeInt(fao.getTruePosition().y);
            mplew.write(fao.isFacingleft());
            return mplew.getPacket();
        }

        public static byte[] ObjRemovePacketByOid(int objectid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.REMOVE_FIELDATTACK_OBJ_KEY.getValue());
            mplew.writeInt(objectid);
            return mplew.getPacket();
        }

        public static byte[] ObjRemovePacketByList(List<MapleFieldAttackObj> removes) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.REMOVE_FIELDATTACK_OBJ_LIST.getValue());
            mplew.writeInt(removes.size());
            for (MapleMapObject obj : removes) {
                mplew.writeInt(obj.getObjectId());
            }
            return mplew.getPacket();
        }

        public static byte[] OnSetAttack(MapleFieldAttackObj fao) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FIELDATTACK_OBJ_ATTACK.getValue());
            mplew.writeInt(fao.getObjectId());
            mplew.writeInt(0);
            return mplew.getPacket();
        }
    }

    public static class SummonPacket {

        public static byte[] spawnSummon(MapleSummon summon, boolean animated) {
            return spawnSummon(summon, animated, summon.getDuration());
        }

        public static byte[] spawnSummon(MapleSummon summon, boolean animated, int newDuration) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPAWN_SUMMON.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(summon.getSkill());
            mplew.writeInt(summon.getOwner().getLevel()); // 왜 얘는 여전히 byte인가
            mplew.writeInt(summon.getSkillLevel());
            mplew.writePos(summon.getPosition());
            mplew.write(summon.getSkill() == 5320011 || summon.getSkill() == 61101002 || summon.getSkill() == 101100100 || summon.getSkill() == 14000027 || summon.getSkill() == 22171081 || summon.getSkill() == 400051046 || summon.getSkill() == 400031049 || summon.getSkill() == 400031051 ? 5 : 4); //reaper = 5?
            short Foothold = 0;
            if (summon.getOwner().getMap().getFootholds().findBelow(summon.getPosition()) != null) {
                Foothold = (short) summon.getOwner().getMap().getFootholds().findBelow(summon.getPosition()).getId();
            }
            mplew.writeShort(Foothold);
            mplew.write(summon.getMovementType().getValue());
            mplew.write(summon.getSummonType()); // 0 = Summon can't attack - but puppets don't attack with 1 either ^.-
            mplew.write(animated ? 1 : 0);
            mplew.writeInt(summon.getOwner().maelstrom); // MobId
            mplew.write(0);//0x100);
            mplew.write(1);//0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(summon.getSkill() == 4341006 || summon.getMovementType() == SummonMovementType.ShadowServant || summon.getSkill() == 400041028);
            final MapleCharacter chr = summon.getOwner();
            if (chr != null && (summon.getSkill() == 4341006 || summon.getMovementType() == SummonMovementType.ShadowServant || summon.getSkill() == 400041028)) {
                PacketHelper.addCharLook(mplew, chr, true, false);
            }
            if (summon.getSkill() == 35111002) {
                List<Point> teslaz = new ArrayList<>();

                for (MapleSummon tesla : chr.getSummons()) {
                    if (tesla.getSkill() == 35111002) {
                        teslaz.add(new Point(tesla.getTruePosition()));
                    }
                }

                if (teslaz.size() != 3) {
                    mplew.write(false); // teslaCoilState
                } else {
                    mplew.write(true);
                    for (Point pos : teslaz) {
                        mplew.writePos(pos);
                    }

                }
            }
            if (isSpecial(summon.getSkill())) { // 분신을 사용하는 친구들임.
                mplew.writeInt((summon.getSkill() - GameConstants.getLinkedSkill(summon.getSkill()) + 1) * 400);
                mplew.writeInt((summon.getSkill() - GameConstants.getLinkedSkill(summon.getSkill()) + 1) * 30);
            }
            mplew.write(summon.getOwner().getBuffedValue(MapleBuffStat.JaguarSummoned) != null);
            mplew.writeInt(newDuration);
            mplew.write(1);//summon.getSkill() == 400021071);
            mplew.writeInt(summon.getOwner().isFacingLeft() ? 1 : 0);// mplew.writeInt(summon.getSummonRLType());//summon.getOwner().isFacingLeft() ? 1 : 0);
            mplew.writeInt(500); // 351 new
            if ((summon.getSkill() - 33001007) >= 0 && (summon.getSkill() - 33001007) <= 8) {
                mplew.write(summon.getOwner().getBuffedValue(MapleBuffStat.JaguarSummoned) != null);
                mplew.write(0);
            }
            mplew.write(summon.isControlCrystal() || summon.getSkill() == 400051046); // 152101000
            if (summon.isControlCrystal() || summon.getSkill() == 400051046) {
                mplew.writeInt(summon.getEnergy()); // 크리스탈 차지 값

                mplew.writeInt(1);
                mplew.writeInt(0);
            }
            mplew.writeInt(0);
            mplew.writeInt(0); // 332++
            if (summon.getSkill() == 151111001) {
                mplew.writeInt(0); //++342
            }
            return mplew.getPacket();
        }

        private static boolean isSpecial(int a1) {
            boolean v2; // zf

            if (a1 > 131003017) {
                if (a1 == 400011005 || a1 == 400031007) {
                    return true;
                }
                v2 = a1 == 400041028;
                if (!v2) {
                    return (a1 - 400031007) >= -2 && (a1 - 400031007) <= 2;
                }
                return true;
            }
            if (a1 == 131003017) {
                return true;
            }
            if (a1 > 131001017) {
                v2 = a1 == 131002017;
                if (!v2) {
                    return (a1 - 400031007) >= -2 && (a1 - 400031007) <= 2;
                }
                return true;
            }
            if (a1 == 131001017
                    || a1 == 14111024
                    || (a1 > 14121053 && a1 <= 14121056)) {
                return true;
            }
            return (a1 - 400031007) >= -2 && (a1 - 400031007) <= 2;
        }

        public static byte[] removeSummon(MapleSummon summon, boolean animated) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.REMOVE_SUMMON.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            if (animated) {
                switch (summon.getSkill()) {
                    case 35121003:
                    case 14000027:
                    case 14111024: // 쉐도우 서번트
                    case 14121054: // 쉐도우 일루전
                    case 400051011:
                        mplew.write(10);
                        break;
                    case 35111001:
                    case 35111010:
                    case 35111009:
                    case 35111002:
                    case 35111005:
                    case 35111011:
                    case 35121009:
                    case 35121010:
                    case 35121011:
                    case 33101008:
                        mplew.write(5);
                        break;
                    case 400051017:
                    case 101100100:
                    case 101100101:
                    case 14121003:
                    case 36121002:
                    case 36121013:
                    case 36121014:
                    case 5321052:
                        mplew.write(0);
                        break;
                    default:
                        mplew.write(4);
                        break;
                }
            } else if (summon.getSkill() == 14000027 || summon.getSkill() == 14100027 || summon.getSkill() == 14110029
                    || summon.getSkill() == 14120008) {
                mplew.write(16);
            } else {
                mplew.write(summon.getSkill() == 35121003 ? 10 : 1);
            }
            return mplew.getPacket();
        }

        public static byte[] moveSummon(int cid, int oid, Point startPos, List<LifeMovementFragment> moves) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.MOVE_SUMMON.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(oid);
            mplew.writeInt(0);
            mplew.writePos(startPos);
            mplew.writeInt(0);
            PacketHelper.serializeMovementList(mplew, moves);

            return mplew.getPacket();
        }

        public static byte[] summonAttack(MapleSummon summon, int skillid, final byte animation, final byte tbyte, final List<Pair<Integer, List<Long>>> allDamage, final int level, Point pos, final boolean darkFlare) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SUMMON_ATTACK.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(summon.getOwner().getLevel()); // 307++
            mplew.write(animation);
            mplew.write(tbyte); //

            for (final Pair<Integer, List<Long>> attackEntry : allDamage) {
                mplew.writeInt(attackEntry.left); // oid
                if (attackEntry.left > 0) {
                    mplew.write(7); // who knows
                    for (Long damage : attackEntry.right) {
                        mplew.writeLong(damage);
                    }
                }
            }
            mplew.write(darkFlare ? 1 : 0);
            mplew.write(summon.isNoapply()); // noAction (이펙트 안보임)
            mplew.writePos(pos);
            mplew.writeInt(skillid); // 274 ++ (maybe)
            mplew.write(false); // 307++
            mplew.writePos(new Point(0, 0)); // 333++

            return mplew.getPacket();
        }

        public static byte[] updateSummon(MapleSummon summon, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.UPDATE_SUMMON.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.write(type); // 13 : 1단계, 14 : 2단계
            mplew.writeInt(summon.getSkill());

            return mplew.getPacket();
        }

        public static byte[] summonSkill(int cid, int summonskillid, int newStance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SUMMON_SKILL.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(summonskillid);
            mplew.write(newStance);

            return mplew.getPacket();
        }

        public static byte[] damageSummon(int cid, int summonskillid, int damage, int unkByte, int monsterIdFrom) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.DAMAGE_SUMMON.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(summonskillid);
            mplew.writeInt(unkByte);
            mplew.writeInt(damage);
            mplew.writeInt(monsterIdFrom);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] damageSummon(MapleSummon summon) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.DAMAGE_SUMMON_2.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(8);

            return mplew.getPacket();
        }

        public static byte[] BeholderRevengeAttack(MapleCharacter chr, short damage, int oid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BEHOLDER_REVENGE.getValue());
            mplew.writeInt(chr.getId());
            mplew.writeInt(damage);
            mplew.writeInt(oid);

            return mplew.getPacket();
        }

        public static byte[] transformSummon(MapleSummon summon, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.TRANSFORM_SUMMON.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(type);

            if (type == 2) {
                mplew.writeInt(summon.getCrystalSkills().size());

                for (int i = 1; i <= summon.getCrystalSkills().size(); ++i) {
                    mplew.writeInt(i);
                    mplew.writeInt(summon.getCrystalSkills().get(i - 1) ? 1 : 0);
                }
            }
            return mplew.getPacket();
        }

        public static byte[] DeathAttack(MapleSummon summon) {
            return DeathAttack(summon, 0);
        }

        public static byte[] DeathAttack(MapleSummon summon, int skillvalue) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.DEATH_ATTACK.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(skillvalue);

            return mplew.getPacket();
        }

        public static byte[] ElementalRadiance(MapleSummon summon, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ELEMENTAL_RADIANCE.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(type);
            switch (type) {
                case 2:
                    mplew.writeInt(summon.getEnergy());
                    if (summon.getEnergy() >= 150) {
                        mplew.writeInt(4);
                    } else if (summon.getEnergy() >= 90) {
                        mplew.writeInt(3);
                    } else if (summon.getEnergy() >= 60) {
                        mplew.writeInt(2);
                    } else if (summon.getEnergy() >= 30) {
                        mplew.writeInt(1);
                    } else {
                        mplew.writeInt(0);
                    }
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] specialSummon(MapleSummon summon, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPECIAL_SUMMON.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(type);

            switch (type) {
                case 2:
                    mplew.writeInt(summon.getEnergy());
                    mplew.writeInt(0);
                    break;
                case 3:
                    mplew.writeInt(0);
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] specialSummon2(MapleSummon summon, int skill) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPECIAL_SUMMON2.getValue());
            mplew.writeInt(summon.getOwner().getId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(skill);

            return mplew.getPacket();
        }
    }

    public static class NPCPacket {

        public static byte[] spawnNPC(MapleNPC life, boolean show) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
            mplew.writeInt(life.getObjectId());
            mplew.writeInt(life.getId());
            mplew.writeShort(life.getPosition().x);
            mplew.writeShort(life.getCy());

            mplew.writeInt(-1); // 350 new
            mplew.writeInt(-1); // 350 new

            mplew.write(0);
            mplew.write(life.getF() == 1 ? 0 : 1);
            mplew.writeShort(life.getFh());
            mplew.writeShort(life.getRx0());
            mplew.writeShort(life.getRx1());
            mplew.write(show ? 1 : 0); // bEnabled
            mplew.writeInt(0); // PresentItem
            mplew.write(0); // nPresentTimeState
            mplew.writeInt(-1); // tPresent
            mplew.writeInt(0); // nNoticeBoardType (1 => send nNoticeBoardValue as int)
            mplew.writeInt(0); // alpha
            mplew.writeMapleAsciiString("");

            // boolean useScreenInfo = life.getId() == 1540000 && ServerConstants.chr != null;
            mplew.write(0);
            /* if (useScreenInfo) {

             if (life.getId() == 1540000) {
             mplew.write(3);

             mplew.writeInt(7); // typeMiniGame
             mplew.writeInt(1); // rank
             mplew.writeInt(0); // starPoint
             MapleCharacter c = ServerConstants.chr;

             mplew.writeMapleAsciiString(c.getName());

             PacketProvider.encodePackedCharacterLook(mplew, c);
             } else {
             mplew.write(0); // ???
             // ?곗씠??(?꾧킅?먭컳?嫄?
             }
             }*/
            return mplew.getPacket();
        }

        public static byte[] spawnNPC2(MapleNPC life, boolean show) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
            mplew.writeInt(life.getObjectId());
            mplew.writeInt(life.getId());
            mplew.writeShort(life.getPosition().x);
            mplew.writeShort(life.getCy());
            mplew.writeInt(-1); // 350 new
            mplew.writeInt(-1); // 350 new
            mplew.write(1);
            mplew.write(1);
            mplew.writeShort(life.getFh());
            mplew.writeShort(life.getRx0());
            mplew.writeShort(life.getRx1());
            mplew.write(show ? 1 : 0); // bEnabled
            mplew.writeInt(0); // PresentItem
            mplew.write(0); // nPresentTimeState
            mplew.writeInt(-1); // tPresent
            mplew.writeInt(0); // nNoticeBoardType (1 => send nNoticeBoardValue as int)
            mplew.writeInt(1000); // alpha
            mplew.writeMapleAsciiString("");
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] removeNPC(final int objectid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.REMOVE_NPC.getValue());
            mplew.writeInt(objectid);

            return mplew.getPacket();
        }

        public static byte[] removeNPCController(final int objectid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
            mplew.write(0);
            mplew.writeInt(objectid);

            return mplew.getPacket();
        }

        public static byte[] spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
            mplew.write(1);
            mplew.writeInt(life.getObjectId());
            mplew.writeInt(life.getId());
            mplew.writeShort(life.getPosition().x);
            mplew.writeShort(life.getCy());
            mplew.writeInt(-1); // 350 new
            mplew.writeInt(-1); // 350 new
            mplew.write(life.getF() == 1 ? 0 : 1);
            mplew.write(0);
            mplew.writeShort(life.getFh());
            mplew.writeShort(life.getRx0());
            mplew.writeShort(life.getRx1());
            mplew.writeShort(MiniMap ? 1 : 0);
            mplew.writeInt(0);
            mplew.writeInt(-1);
            mplew.writeZeroBytes(11);
            return mplew.getPacket();
        }

        public static byte[] setNPCScriptable(List<Pair<Integer, String>> npcs) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.NPC_SCRIPTABLE.getValue());
            mplew.write(npcs.size());
            for (Pair<Integer, String> s : npcs) {
                mplew.writeInt(s.left);
                mplew.writeMapleAsciiString(s.right);
                mplew.writeInt(0); // start time
                mplew.writeInt(Integer.MAX_VALUE); // end time
            }
            return mplew.getPacket();
        }

        public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type) {
            return getNPCTalk(npc, msgType, talk, endBytes, type, npc, false, false);
        }

        public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, int npc2) {
            return getNPCTalk(npc, msgType, talk, endBytes, type, npc2, false, false);
        }

        public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, int diffNPC, boolean illust, boolean isLeft) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());

            mplew.write(4);
            mplew.writeInt(npc);
            mplew.write(0);
            mplew.write(msgType);
            mplew.writeShort(type); // mask; 1 = no ESC, 2 = playerspeaks, 4 = diff NPC 8 = something, ty KDMS
            mplew.write((type & 0x4) != 0 ? 1 : 0); //1.2.274 ++
            if (msgType == 0) {
                mplew.writeInt(0); // 351 new
                mplew.writeInt(0); // 351 new
            }
            if ((type & 0x4) != 0) {
                if (diffNPC == 0) {
                    diffNPC = npc;
                }
                mplew.writeInt(diffNPC);
            }
            if (msgType == 0x13) {
                mplew.writeLong(5);
            }
            mplew.writeMapleAsciiString(talk);
            if (msgType != 0x13) {
                if (msgType != 28 && msgType != 30) {
                    mplew.write(HexTool.getByteArrayFromHexString(endBytes));
                }
                if (msgType == 0) {
                    mplew.writeInt(illust ? npc : 0);
                    if (illust) {
                        mplew.writeInt(diffNPC); // face ?숈떆?ъ슜
                        mplew.write(isLeft);
                    }
                    mplew.writeShort(0); //위치모름 if문위일수도 
                }
            }
            return mplew.getPacket();
        }

        public static byte[] getNpcTalkNoButton(int npc, short type, int delay, String talk) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
            mplew.write(3);
            mplew.writeInt(0);
            mplew.writeInt(1);
            mplew.writeShort(0);
            mplew.writeShort(type);
            mplew.write(1);
            mplew.writeInt(npc);
            mplew.writeMapleAsciiString(talk);
            mplew.write(0);
            mplew.write(1);
            mplew.writeInt(delay);
            return mplew.getPacket();
        }

        public static byte[] getNPCConductExchangeTalk(int npc, String msg) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
            mplew.write(4);
            mplew.writeInt(npc);
            mplew.write(0);
            mplew.writeShort(3);
            mplew.writeShort(1);
            mplew.writeMapleAsciiString(msg);
            return mplew.getPacket();
        }

        public static byte[] getMapSelection(final int npcid, final String sel) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
//            System.out.println("bbb");
            mplew.write(4);
            mplew.writeInt(npcid);
            mplew.write(0);
            mplew.write(0x11);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(npcid == 2083006 ? 1 : 0); //neo city
            mplew.write(0);
            mplew.writeInt(npcid == 9010022 ? 1 : 0); //dimensional
            mplew.writeMapleAsciiString(sel);

            return mplew.getPacket();
        }

        public static byte[] getNPCTalkMixStyle(int npcId, String talk, boolean isZeroBeta, boolean isAngelicBuster) {
            MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
            packet.writeShort(SendPacketOpcode.NPC_TALK.getValue());
            packet.write(4);
            packet.writeInt(npcId);
            packet.write(0);
            packet.write(0x2C);
            packet.writeShort(0);
            packet.write(0);
            packet.writeInt(0); //1.2.257+
            packet.write(isAngelicBuster ? 1 : 0);
            packet.writeInt(isZeroBeta ? 1 : 0); //bZeroBeta
            packet.writeInt(0x32);
            packet.writeMapleAsciiString(talk);

            return packet.getPacket();
        }

        public static byte[] getNPCTalkStyle(int npc, String talk, List<Integer> args) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
//            System.out.println("cccc");
            mplew.write(4);
            mplew.writeInt(npc);
            mplew.write(0);
            mplew.write(10);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeMapleAsciiString(talk);
            mplew.writeInt(0); // 307++
            mplew.write(args.size());

            for (int i = 0; i < args.size(); i++) {
                mplew.writeInt(args.get(i));
            }
            //       System.out.print("토크 스타일" + mplew.toString());
            return mplew.getPacket();
        }

        public static byte[] getNPCTalkStyle(MapleCharacter chr, int npc, String talk, int... args) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
//            System.out.println("cccc");
            mplew.write(4);
            mplew.writeInt(npc);
            mplew.write(0);
            mplew.write(10);
            mplew.writeShort(0);
            mplew.write(0);

            mplew.write(GameConstants.isAngelicBuster(chr.getJob()) ? chr.getDressup() ? (byte) 1 : (byte) 0 : (byte) 0);
            mplew.write(GameConstants.isZero(chr.getJob()) ? chr.getGender() == 1 ? (byte) 1 : (byte) 0 : (byte) 0);
            mplew.writeMapleAsciiString(talk);

            mplew.write(0);
            mplew.writeInt(chr.getHair());
            mplew.write(-1);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(chr.getFace());

            mplew.write(args.length);

            for (int i = 0; i < args.length; i++) {
                mplew.writeInt(args[i]);
            }
//            System.out.print("?좏겕?ㅽ??? + mplew);
            return mplew.getPacket();
        }

        public static byte[] getNPCTalkStyleAndroid(int npcId, String talk, int... args) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
            mplew.write(4);
            mplew.writeInt(npcId);
            mplew.write(0);

            mplew.write(11);
            mplew.writeShort(0);
            mplew.write(0);

            //mplew.write(0);
            mplew.writeMapleAsciiString(talk);
            //mplew.writeInt(0); // 307++
            mplew.write(args.length);

            for (int i = 0; i < args.length; i++) {
                mplew.writeInt(args[i]);
            }
            return mplew.getPacket();
        }

        public static byte[] getNPCTalkStyleZero(int npcId, String talk, int[] args1, int[] args2) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
            mplew.write(4);
            mplew.writeInt(npcId);
            mplew.write(0);
            mplew.write(32);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeMapleAsciiString(talk);
            mplew.writeInt(0);
            mplew.write(args1.length);

            for (int i = 0; i < args1.length; i++) {
                mplew.writeInt(args1[i]);
            }

            mplew.write(args2.length);

            for (int i = 0; i < args2.length; i++) {
                mplew.writeInt(args2[i]);
            }
            return mplew.getPacket();
        }

        public static byte[] getNPCTalkNum(int npc, String talk, int def, int min, int max) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
//            System.out.println("afffffffffaa");
            mplew.write(4);
            mplew.writeInt(npc);
            mplew.write(0);
            mplew.write(5);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeMapleAsciiString(talk);
            mplew.writeInt(def);
            mplew.writeInt(min);
            mplew.writeInt(max);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] getNPCTalkText(int npc, String talk) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
//            System.out.println("ggggg");
            mplew.write(4);
            mplew.writeInt(npc);
            mplew.write(0);
            mplew.write(4);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeMapleAsciiString(talk);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] getEvanTutorial(String data) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
//            System.out.println("hhhhh");
            mplew.write(8);
            mplew.writeInt(0);
            mplew.write(1);
            mplew.write(1);
            mplew.writeShort(0);
            mplew.write(1);
            mplew.writeMapleAsciiString(data);
            mplew.writeInt(0); //1.2.274 ++

            return mplew.getPacket();
        }

        public static byte[] getNPCShop(int sid, MapleShop shop, MapleClient c) {
            c.getPlayer().setKeyValue(16180, "point", c.getPlayer().getDonationPoint() + "");
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
            mplew.writeInt(sid);
            mplew.write(0);

            mplew.writeInt(0); // selectNpcItemID
            mplew.writeInt(0); // dwNpcTemplateID
            mplew.writeInt(0); // StarCoin
            mplew.writeInt(2); // ShopVerNo
            mplew.writeInt(0); // 306 ++
            PacketHelper.addShopInfo(mplew, shop, c);
            return mplew.getPacket();
        }

        public static byte[] confirmShopTransactionItem(byte code, MapleShop shop, MapleClient c, int indexBought, int itemId) {
            return confirmShopTransactionItem(code, shop, c, indexBought, itemId, false, false);
        }

        public static byte[] confirmShopTransactionItem(byte code, MapleShop shop, MapleClient c, int indexBought, int itemId, boolean repurchase, boolean limit) {
            c.getPlayer().setKeyValue(16180, "point", c.getPlayer().getDonationPoint() + "");
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            /*
             * code  343 Change Ove
             * 0 : 구매
             * 1, 9, 15 : 물품이 부족합니다
             * 2, 18 : 메소가 부족합니다
             * 3 : 포인트가 부족합니다
             * 4 : %d층 이상 클리어해야 구매가 가능합니다.
             * 5 : 퀘스트 조건이 충족되지 않아 구매 불가
             * 6 : 요구조건을 충족하지 않아 구매하실 수 없습니다.
             * 7 : 아이템을 구매할 수 있는 기간이 아닙니다.
             * 8 : 오류가 발생하여 거래 하지 못함.
             * 9 : 등급부터 이용할 수 잇습니다.
             * 10 : 남은 아이템 슬롯이 부족하지 않은지 확인해보세요.
             * 11 : 현재 남아있는 재고량은 구입하시는 물품수량이 전부입니다.
             * 14 : 소지 가능한 메소를 넘어 판매 불가
             * 15 : 1회에 판매 가능한 총 금액은 최대 20억 메소입니다.
             * 21 : 아이템이 부족합니다.
             * 22 : 스타코인 개수가 부족합니다.
             * 23 : %d레벨 이하만 구입이 가능합니다.
             * 24 : %d레벨 이상만 구입이 가능합니다.
             * 25 : 더 이상 가질 수 없는 아이템입니다.
             * 26 : %s(아이템 이름) 구매 가능 횟수를 모두 사용하여 더이상 구매할 수 없습니다.
             * 27 : 핑크빈은 할 수 없는 일입니다. / 아이템이나 메소를 이동할 수 없습니다.
             * 28 : 상점 물품이 갱신되었습니다. 창을 다시 열어주세요.
             * 29 : 해당 물품은 현재 %d개 만큼 구매하실 수 있습니다.
             * 31 : 비활성 게임ID 보호정책으로 인해 구매가 불가능합니다. +2 ?
             * 32 : 이전 접속IP와 다른 IP에서 접속하셨습니다.
             .//* 33 : 해당 기능은 현재 사용할 수 없는 상태입니다.
             * 35 : 현재 월드에서는 할 수 없는 일입니다.
             * 36 : true일 경우 상점 데이터 초기화 + 판매물품의 정보가 변경되어 구매하지 못했습니다.
             * 37 : 재구매 할수 없는 아이템 입니다.
             * 38 : 판매 할 수 없는 아이템입니다.
             * 39 : 더이상 판매 할 수 없습니다.
             * 40 : 판매 할 수 없는 아이템입니다.

             * default : 오류가 발생해 거래를 하지 못했습니다.
             */

            mplew.writeShort(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
            mplew.write(code);

            switch (code) {
                case 0:
                    mplew.write(repurchase); // updateRepurchaseItem
                    if (repurchase) {
                        mplew.writeInt(indexBought);
                    } else {
                        mplew.writeInt(itemId);
                        mplew.writeInt(9999999); // 남은 물품 갯수
                        mplew.writeInt(0); //StarCoin
                    }
                    break;
                case 4:
                    mplew.writeInt(0); // 층수
                    break;
                case 6:
                case 8:
                case 9:
                    mplew.writeInt(0);
                    break;
                case 11: // 307++?
                    mplew.writeInt(0); // selectNpcItemID
                    mplew.writeInt(0); // dwNpcTemplateID
                    mplew.writeInt(0); // StarCoin
                    mplew.writeInt(2); // ShopVerNo
                    mplew.writeInt(0); // 306 ++
                    PacketHelper.addShopInfo(mplew, shop, c);
                    break;
                case 21:
                case 23:
                case 24:
                    mplew.writeInt(0); // 레벨 제한
                    break;
                case 26:
                    mplew.writeInt(itemId);
                    break;
                case 32:
                    mplew.writeInt(0); // 거래 제한 시간 (분)
                    break;
                case 33:
                    mplew.write(true);
                    mplew.writeInt(0); // selectNpcItemID
                    mplew.writeInt(0); // dwNpcTemplateID
                    mplew.writeInt(0); // StarCoin
                    mplew.writeInt(2); // ShopVerNo
                    mplew.writeInt(0); // 306 ++
                    PacketHelper.addShopInfo(mplew, shop, c);
                    break;
                case 36:
                    mplew.write(0);
                    break;
            }
            return mplew.getPacket();
        }

        public static byte[] getStorage(int npcId, short slots, Collection<Item> items, long meso) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(24);
            mplew.writeInt(npcId);
            mplew.write(126); // slot
            mplew.writeLong(-1); // flag
            mplew.writeLong(meso);
            mplew.write(0);
            mplew.write((byte) items.size());
            for (Item item : items) {
                PacketHelper.addItemInfo(mplew, item);
            }
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] getStorage(byte status) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(0x17);
            mplew.write(status);

            return mplew.getPacket();
        }

        public static byte[] getStorageFull() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(0x11);

            return mplew.getPacket();
        }

        public static byte[] mesoStorage(byte slots, long meso) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(0x13);
            mplew.write(slots);
            mplew.writeShort(2);
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.writeLong(meso);

            return mplew.getPacket();
        }

        public static byte[] arrangeStorage(byte slots, Collection<Item> items, boolean changed) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(0x0F);
            mplew.write(slots);
            mplew.write(0x7C); //4 | 8 | 10 | 20 | 40
            mplew.writeZeroBytes(10);
            mplew.write(items.size());
            for (Item item : items) {
                PacketHelper.addItemInfo(mplew, item);
            }
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(0x0D);
            mplew.write(slots);
            mplew.writeShort(type.getBitfieldEncoding());
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.write(items.size());
            for (Item item : items) {
                PacketHelper.addItemInfo(mplew, item);
            }
            return mplew.getPacket();
        }

        public static byte[] takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
            mplew.write(0x9);
            mplew.write(slots);
            mplew.writeShort(type.getBitfieldEncoding());
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.write(items.size());
            for (Item item : items) {
                PacketHelper.addItemInfo(mplew, item);
            }
            return mplew.getPacket();
        }
    }

    public static class InteractionPacket {

        public static byte[] getTradeInvite(MapleCharacter c, boolean isTrade) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.INVITE_TRADE.action);
            mplew.write(isTrade ? 4 : 3);
            mplew.writeMapleAsciiString(c.getName());
            mplew.writeInt(c.getJob());
            mplew.writeInt(0);//c.getId()); // 332++

            return mplew.getPacket();
        }

        public static byte[] getMarriageInvite(MapleCharacter c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.INVITE_TRADE.action);
            mplew.write(8);
            mplew.writeMapleAsciiString(c.getName());
            mplew.writeInt(c.getJob());
            mplew.writeInt(0);//c.getId()); // 332++

            return mplew.getPacket();
        }

        public static byte[] getCashTradeInvite(MapleCharacter c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.INVITE_TRADE.action);
            mplew.write(7);
            mplew.writeMapleAsciiString(c.getName());
            mplew.writeInt(c.getJob());
            mplew.writeInt(0);//c.getId()); // 332++

            return mplew.getPacket();
        }

        public static byte[] getTradeMesoSet(byte number, long meso) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.SET_MESO1.action);
            mplew.write(number);
            mplew.writeLong(meso);

            return mplew.getPacket();
        }

        public static byte[] getTradeItemAdd(byte number, Item item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.SET_ITEMS1.action);
            mplew.write(number);
            mplew.write(item.getPosition()); // Only in inv and not equipped //PacketHelper.addItemPosition(mplew, item, true, false);
            PacketHelper.addItemInfo(mplew, item);

            return mplew.getPacket();
        }

        public static byte[] getTradeStart(MapleClient c, MapleTrade trade, byte number, boolean isTrade) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(20);
            mplew.write(isTrade ? 4 : 3);
            mplew.write(2);
            mplew.write(number);

            if (number == 1) {
                mplew.write(0);
                PacketHelper.addCharLook(mplew, trade.getPartner().getChr(), false, false);
                mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
                mplew.writeShort(trade.getPartner().getChr().getJob());
                mplew.writeInt(0);
            }
            mplew.write(number);
            PacketHelper.addCharLook(mplew, c.getPlayer(), false, false);
            mplew.writeMapleAsciiString(c.getPlayer().getName());
            mplew.writeShort(c.getPlayer().getJob());
            mplew.writeInt(0);
            mplew.write(0xFF);

            return mplew.getPacket();
        }

        public static byte[] getCashTradeStart(MapleClient c, MapleTrade trade, byte number) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(20);
            mplew.write(7);
            mplew.write(2);
            mplew.write(number);
            if (number == 1) {
                mplew.write(0);
                PacketHelper.addCharLook(mplew, trade.getPartner().getChr(), false, false);
                mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
                mplew.writeShort(trade.getPartner().getChr().getJob());
                mplew.writeInt(0);
            }
            mplew.write(number);
            PacketHelper.addCharLook(mplew, c.getPlayer(), false, false);
            mplew.writeMapleAsciiString(c.getPlayer().getName());
            mplew.writeShort(c.getPlayer().getJob());
            mplew.writeInt(0);
            mplew.write(0xFF);

            return mplew.getPacket();
        }

        public static byte[] getTradeConfirmation() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.CONFIRM_TRADE1.action);

            return mplew.getPacket();
        }

        public static byte[] TradeMessage(final byte UserSlot, final byte message) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.EXIT.action);
            mplew.write(UserSlot);
            mplew.write(message);
            //0x02 = cancelled
            //0x07 = success [tax is automated]
            //0x08 = unsuccessful
            //0x09 = "You cannot make the trade because there are some items which you cannot carry more than one."
            //0x0A = "You cannot make the trade because the other person's on a different map."

            return mplew.getPacket();
        }

        public static byte[] getTradeCancel(final byte UserSlot) { //0 = canceled 1 = invent space 2 = pickuprestricted
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
            mplew.write(PlayerInteractionHandler.Interaction.EXIT.action);
            mplew.write(UserSlot);
            mplew.write(2);

            return mplew.getPacket();
        }
    }

    public static byte[] getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count); // number of macros
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getCharInfo(final MapleCharacter chr) {

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());

        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.write(0); //274 ++
        mplew.writeInt(3471);//2378
        mplew.writeInt(1191);//1587

        mplew.write(1); //seed if문
        mplew.writeShort(ServerConstants.serverMessage.length() > 0 ? 1 : 0);
        if (ServerConstants.serverMessage.length() > 0) {
            mplew.writeMapleAsciiString(ServerConstants.serverMessage);
            mplew.writeMapleAsciiString(ServerConstants.serverMessage);
        }
        //Random Seed
        int seed1 = Randomizer.nextInt();
        int seed2 = Randomizer.nextInt();
        int seed3 = Randomizer.nextInt();
        chr.getCalcDamage().SetSeed(seed1, seed2, seed3);
        mplew.writeInt(seed1);
        mplew.writeInt(seed2);
        mplew.writeInt(seed3);
        //End random seed

        PacketHelper.addCharacterInfo(mplew, chr);

        mplew.write(1);//0이면 아래 두개 X
        mplew.write(0);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.write(0);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.write(0); //  CWvsContext::SetWhiteFadeInOut
        mplew.write(0); //  pChatBlockReason

        mplew.writeLong(getTime(System.currentTimeMillis()));

        mplew.writeInt(100); // paramFieldInit.nMobStatAdjustRate
        mplew.write(0); // CFieldCustom::Decode
        mplew.write(0); // CWvsContext::OnInitPvPStat

        mplew.write(GameConstants.isPhantom(chr.getJob()) ? 0 : 1); //bCanNotifyAnnouncedQuest
        if (((chr.getMapId() / 10) == 10520011) || ((chr.getMapId() / 10) == 10520051) || chr.getMapId() == 105200519) { // is_banban_field
            mplew.write(0);
        }
        mplew.write(0);
        mplew.write(0);//CUser::StarPlanetRank::Decode
        mplew.write(0);// CWvsContext::DecodeStarPlanetRoundInfo
        mplew.writeInt(0);// CUser::DecodeTextEquipInfo
        mplew.write(0);  //CUser::DecodeFreezeHotEventInfo 5byte
        mplew.writeInt(0);
        mplew.writeShort(0); // 350 modify
        boolean read = true;
        mplew.write(read);
        if (read) {
            mplew.writeInt(-1);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(999999999);
            mplew.writeInt(999999999);
            mplew.writeMapleAsciiString("");//ServerConstants.WORLD_UI
        }
        boolean sundayMaple = false;
        Date time = new Date();
        int day = time.getDay();
        if (day == 5 || day == 6 || day == 0) { //목 ~ 일요일까지 선데이 메이플 UI적용
            //sundayMaple = true;
        }
        mplew.write(sundayMaple); // 완료
        if (sundayMaple) {
            mplew.writeMapleAsciiString(ServerConstants.SundayMapleUI);
            mplew.writeMapleAsciiString(ServerConstants.SundayMapleTEXTLINE_1);
            mplew.writeMapleAsciiString(ServerConstants.SundayMapleTEXTLINE_2);
            mplew.writeInt(60); //세로 사이즈?
            mplew.writeInt(220); //가로 사이즈?
        }
        mplew.writeInt(0);
        mplew.writeInt(0); // 350 new
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getWarpToMap(final MapleMap to, final int spawnPoint, final MapleCharacter chr) {

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.write(0);
        mplew.writeInt(0);

        mplew.write(2); //274 ++
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeShort(0);
        mplew.write(0); // 273 ++

        mplew.writeInt(to.getId());
        mplew.write(spawnPoint);
        mplew.writeInt(chr.getStat().getHp());

        mplew.write(0);
        mplew.write(1);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));

        mplew.writeInt(100);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        if (((to.getId() / 10) == 10520011) || ((to.getId() / 10) == 10520051) || to.getId() == 105200519) { // is_banban_field
            mplew.write(0);
        }
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeShort(0); // 350 modify
        mplew.write(true);
        mplew.writeInt(-1);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(999999999);
        mplew.writeInt(999999999);
        mplew.writeMapleAsciiString("");
        mplew.write(false);

        /*        mplew.writeMapleAsciiString(ServerConstants.WORLD_UI);
         mplew.writeMapleAsciiString(ServerConstants.SUNDAY_TEXT);
         mplew.writeMapleAsciiString(ServerConstants.SUNDAY_DATE);
         mplew.writeInt(50);
         mplew.writeInt(232);*/
        mplew.writeInt(0);
        mplew.writeInt(0); // 350 new
        mplew.write(false);
        return mplew.getPacket();
    }

    public static byte[] showEquipEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect(int team) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());
        mplew.writeShort(team);
        return mplew.getPacket();
    }

    public static byte[] multiChat(MapleCharacter chr, String chattext, int mode, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(item == null ? SendPacketOpcode.MULTICHAT.getValue() : SendPacketOpcode.MULTICHATITEM.getValue());
        mplew.write(mode); //  0 buddychat; 1 partychat; 2 guildchat 
        mplew.writeInt(chr.getAccountID()); // accId
        mplew.writeInt(chr.getId()); // charId
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeMapleAsciiString(chattext);
        PacketHelper.ChatPacket(mplew, chr.getName(), chattext);
        mplew.write(item != null);
        if (item != null) {
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(item.getItemId()));
        }
        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithCS(String target, final boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(2);
        mplew.writeInt(-1);

        return mplew.getPacket();
    }

    public static byte[] getWhisper(String sender, int channel, String text, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString(sender);
        mplew.writeInt(0); // 332++
        mplew.writeShort(channel - 1);
        mplew.writeMapleAsciiString(text);
        PacketHelper.ChatPacket(mplew, sender, text);
        mplew.write(item != null);
        if (item != null) {
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(item.getItemId()));
        }

        return mplew.getPacket();
    }

    public static byte[] getWhisperReply(String target, byte reply) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x0A); // whisper? 
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);//  0x0 = cannot find char, 0x1 = success 

        return mplew.getPacket();
    }

    public static byte[] getWhisperReply(String target, byte write, byte reply) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(write); // whisper? 
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);//  0x0 = cannot find char, 0x1 = success 
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithMap(String target, int mapid, final boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(1);
        mplew.writeInt(mapid);
        mplew.writeZeroBytes(8); // ?? official doesn't send zeros here but whatever

        return mplew.getPacket();
    }

    public static byte[] getFindReply(String target, int channel, final boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(3);
        mplew.writeInt(channel - 1);

        return mplew.getPacket();
    }

    public static final byte[] MapEff(final String path) {
        return environmentChange(path, 12);
    }

    public static final byte[] MapNameDisplay(final int mapid) {
        return environmentChange("maplemap/enter/" + mapid, 12);
    }

    public static final byte[] Aran_Start() {
        return environmentChange("Aran/balloon", 4);
    }

    public static byte[] musicChange(String song) {
        return environmentChange(song, 7);
    }

    public static byte[] showEffect(String effect) {
        return environmentChange(effect, 4);
    }

    public static byte[] playSound(String sound) {
        return environmentChange(sound, 5);
    }

    public static byte[] environmentChange(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(env);
        if (mode != 4 && mode != 11 && mode != 19 && mode != 20 && mode != 16) {
            mplew.writeInt(100);
        }
        if (mode == 7 || mode == 19) {
            mplew.writeInt(0);
        }
        if (mode == 20) {
            mplew.write(7);
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static byte[] KaiserChangeColor(int cid, int color1, int color2, byte premium) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KAISER_CHANGE_COLOR.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(color1);
        mplew.writeInt(color2);
        mplew.write(premium);
        return mplew.getPacket();
    }

    public static byte[] trembleEffect(int type, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(1);
        mplew.write(type);
        mplew.writeInt(delay);

        return mplew.getPacket();
    }

    public static byte[] environmentMove(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_ENV.getValue());
        mplew.writeMapleAsciiString(env);
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    public static byte[] getUpdateEnvironment(final List<Environment> list) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_ENV.getValue());
        mplew.writeInt(list.size());
        for (Environment mp : list) {
            mplew.writeMapleAsciiString(mp.getName());
            mplew.write(false);
            mplew.writeInt(mp.isShow() ? 1 : 0);
            mplew.writeInt(mp.getX());
            mplew.writeInt(mp.getY());
        }

        return mplew.getPacket();
    }

    public static byte[] startMapEffect(String msg, int itemid, boolean active) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleAsciiString(msg);
            mplew.writeInt(3); // 307++
            mplew.write(0); // 274++
        }
        return mplew.getPacket();
    }

    public static byte[] removeMapEffect() {
        return startMapEffect(null, 0, false);
    }

    public static byte[] getPVPClock(int type, int time) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(3);
        mplew.write(type);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getVanVanClock(byte type, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(type); // 4 : 시간 줄어듬, 5 : 시간 늘어남
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getDojoClockStop(boolean stop, int time) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(7); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.write(stop); // ?
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getDojoClock(int endtime, int starttime) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(8); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(endtime);
        mplew.writeInt(starttime);

        return mplew.getPacket();
    }

    public static byte[] getClock(int time) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getClockTime(int hour, int min, int sec) { // Current Time
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(1); //Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);

        return mplew.getPacket();
    }

    public static byte[] boatPacket(int effect, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOAT_MOVE.getValue());
        mplew.write(effect); // 8 = start, 10 = move, 12 = end
        mplew.write(mode);
        // Effect 8: 2 = ship go
        // Effect 10: 4 = appears, 5 = disappears
        // Effect 12: 6 = ship arrives

        return mplew.getPacket();
    }

    public static byte[] stopClock() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.STOP_CLOCK.getValue());

        return mplew.getPacket();
    }

    public static byte[] achievementRatio(int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ACHIEVEMENT_RATIO.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] spawnPlayerMapobject(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_PLAYER.getValue());

        mplew.writeLong(PacketHelper.getKoreanTimestamp(System.currentTimeMillis()));
        mplew.writeInt(chr.getId());
        mplew.writeInt(0); // 351 new

        mplew.writeInt(chr.getLevel());
        mplew.writeMapleAsciiString(chr.getName());

        mplew.writeMapleAsciiString(""); //ParentName

        final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
        if (gs != null && gs.getCustomEmblem() == null) {
            mplew.writeInt(chr.getGuildId()); // 333++
            mplew.writeMapleAsciiString(gs.getName());
            mplew.writeShort(gs.getLogoBG());
            mplew.write(gs.getLogoBGColor());
            mplew.writeShort(gs.getLogo());
            mplew.write(gs.getLogoColor());
            mplew.writeInt(chr.getGuildId()); // 333++
        } else {
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("");
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeInt(0);
        }

        mplew.writeInt(0);
        mplew.write(chr.getGender());
        mplew.writeInt(25);
        mplew.writeInt(1);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);

        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

        chr.getEffects().stream().forEach((effect) -> {
            if (effect.getLeft() != MapleBuffStat.EnergyCharged) {
                statups.put(effect.getLeft(), new Pair<>(effect.getRight().value, effect.right.localDuration));
            }
        });

        PacketHelper.writeBuffMask(mplew, PacketHelper.sortBuffStats(statups));
        PacketHelper.encodeForRemote(mplew, statups, chr);

        mplew.writeShort(chr.getJob());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0); //++342

        PacketHelper.addCharLook(mplew, chr, true, false);
        if (GameConstants.isZero(chr.getJob())) {
            PacketHelper.addCharLook(mplew, chr, true, true);
        }

        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(chr.getItemEffect());
        mplew.writeInt(0);
        mplew.writeInt(chr.getKeyValue(19019, "id")); // headtitle

        mplew.write(0); // bool, if true send string

        mplew.writeInt(Math.max(0, GameConstants.getDSkinNum((int) chr.getKeyValue(7293, "damage_skin"))));
        mplew.writeInt(Math.max(0, GameConstants.getDSkinNum((int) chr.getKeyValue(7293, "damage_skin"))));
        mplew.writeInt(0); // 332++
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString("");
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        mplew.write(true); // 324 ++ true -> false
        mplew.writeInt(0); // 324 ++
        mplew.writeShort(-1);
        mplew.writeInt(chr.getChair());
        mplew.writePos(chr.getTruePosition());
        mplew.write(chr.getStance());
        mplew.writeShort(chr.getFH());

        mplew.write(chr.getChair() != 0);

        if (chr.getChair() != 0) {
            PacketHelper.chairPacket(mplew, chr, chr.getChair());
        }

        int petindex = 0;
        if (chr.getPets().length > 0 && chr.getMapId() != ServerConstants.WarpMap) {
            for (final MaplePet pet : chr.getPets()) {
                if (pet != null) {
                    mplew.write(true);
                    mplew.writeInt(petindex++);
                    mplew.writeInt(pet.getPetItemId());
                    mplew.writeMapleAsciiString(pet.getName());
                    mplew.writeLong(pet.getUniqueId());
                    mplew.writeShort(pet.getPos().x);
                    mplew.writeShort(pet.getPos().y - 20);
                    mplew.write(pet.getStance());
                    mplew.writeShort(pet.getFh());
                    mplew.writeInt(pet.getColor()); // Pet Color, RGB.
                    mplew.writeShort(pet.getWonderGrade());
                    mplew.writeInt(pet.getPetSize()); // size
                }
            }
        }

        mplew.write(false); // pet end

        mplew.write(false);

        mplew.writeInt(chr.getMount().getLevel()); // mount lvl
        mplew.writeInt(chr.getMount().getExp()); // exp
        mplew.writeInt(chr.getMount().getFatigue()); // tiredness

        mplew.write(0); //1.2.274 ++

        PacketHelper.addAnnounceBox(mplew, chr);

        mplew.write(chr.getChalkboard() != null && chr.getChalkboard().length() > 0 ? 1 : 0);
        if (chr.getChalkboard() != null && chr.getChalkboard().length() > 0) {
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }

        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(mplew, rings.getLeft());
        addRingInfo(mplew, rings.getMid());
        addMRingInfo(mplew, rings.getRight(), chr);

        mplew.write(true); // 332++ false -> true 342++

        mplew.write(0); // flag

        mplew.writeInt(0); //++342
        mplew.writeInt(0);

        if (GameConstants.isKaiser(chr.getJob())) {
            mplew.writeInt(chr.getKeyValue(12860, "extern") == -1 ? 0 : chr.getKeyValue(12860, "extern"));
            mplew.writeInt(chr.getKeyValue(12860, "inner") == -1 ? 1 : chr.getKeyValue(12860, "inner"));
            mplew.write(chr.getKeyValue(12860, "premium") == -1 ? 0 : (byte) chr.getKeyValue(12860, "premium"));
//            mplew.writeInt(1);
            //          mplew.writeShort(0);
        }
        mplew.writeInt(0);
        PacketHelper.addFarmInfo(mplew, chr.getClient(), 0);

        for (int i = 0; i < 5; i++) {
            mplew.write(-1);
        }

        mplew.writeInt(0);
        mplew.write(1);

        if (chr.getBuffedValue(MapleBuffStat.RideVehicle) != null && chr.getBuffedValue(MapleBuffStat.RideVehicle) == 1932249) {
            mplew.writeInt(0);
        }

        // mplew.write(chr.getBuffedValue(MapleBuffStat.KinesisPsychicEnergeShield) != null ? 1 : 0);
        mplew.write(0);

        mplew.writeInt(0);

        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0); // EventBestFriendInfo > eventbestfriendAID

        mplew.write(false); // kinesisPsychicEnergyShieldEffect
        mplew.write(false); // 306 
        mplew.write(false); // 306 

        mplew.writeInt(1051291);//chr.getMesoChairCount()); // mesochairCount

        mplew.write(false);

        mplew.writeInt(0);
        mplew.writeInt(0); // 351 new
        return mplew.getPacket();
    }

    public static byte[] removePlayerFromMap(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] getChatText(MapleCharacter chr, String text, boolean whiteBG, int show, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(item == null ? SendPacketOpcode.CHATTEXT.getValue() : SendPacketOpcode.CHATTEXTITEM.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(whiteBG ? 1 : 0);
        mplew.writeMapleAsciiString(text);
        PacketHelper.ChatPacket(mplew, chr.getName(), text);
        mplew.write(show);
        mplew.write(0);
        if (item != null) {
            mplew.write(-1);
            mplew.write(true);
            PacketHelper.addItemInfo(mplew, item);
            mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(item.getItemId()));
        } else {
            mplew.write(false);
        }
        return mplew.getPacket();
    }

    public static byte[] getUniverseChat(boolean disableworldname, String name, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MULTICHAT.getValue() + 2);
        mplew.write(disableworldname ? 1 : 0);
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(text);
        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit, int scrollid, int victimid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);

        switch (scrollSuccess) {
            case SUCCESS:
                mplew.write(1);
                mplew.write(legendarySpirit ? 1 : 0);
                mplew.writeInt(scrollid);
                mplew.writeInt(victimid);
                break;
            case FAIL:
                mplew.write(0);
                mplew.write(legendarySpirit ? 1 : 0);
                mplew.writeInt(scrollid);
                mplew.writeInt(victimid);
                break;
            case CURSE:
                mplew.write(2);
                mplew.write(legendarySpirit ? 1 : 0);
                mplew.writeInt(scrollid);
                mplew.writeInt(victimid);
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] showMagnifyingEffect(final int chr, final short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNIFYING_EFFECT.getValue());
        mplew.writeInt(chr);
        mplew.writeShort(pos);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showPotentialReset(final int chr, final boolean success, final int itemid, final int equipId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_POTENTIAL_RESET.getValue());
        mplew.writeInt(chr);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemid); // fireworks, Item/Cash/0506.img/%08d/effect/default
        mplew.writeInt(0); // 307++
        mplew.writeInt(equipId); // 307++

        return mplew.getPacket();
    }

    public static byte[] getRedCubeStart(MapleCharacter chr, Item item, boolean up, int cubeId, int remainCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_REDCUBE_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(up);
        mplew.writeInt(cubeId);
        mplew.writeInt(item.getPosition());
        mplew.writeInt(remainCount); // 325++
        PacketHelper.addItemInfo(mplew, item);
        return mplew.getPacket();
    }

    public static byte[] getCubeStart(MapleCharacter chr, Item item, boolean up, int cubeId, int remainCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_CUBE_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(up);
        mplew.writeInt(cubeId);
        mplew.writeInt(item.getPosition());
        mplew.writeInt(remainCount); // 325++
        PacketHelper.addItemInfo(mplew, item);
        return mplew.getPacket();
    }

    public static byte[] getEditionalCubeStart(MapleCharacter chr, Item item, boolean up, int cubeId, int remainCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_EDITIONALCUBE_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(up);
        mplew.writeInt(cubeId);
        mplew.writeInt(item.getPosition());
        mplew.writeInt(remainCount); // 325++
        PacketHelper.addItemInfo(mplew, item);
        return mplew.getPacket();
    }

    public static byte[] getWhiteCubeStart(MapleCharacter chr, Item item, int cubeId, int cubeQuantity, int cubePosition) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHITE_CUBE_WINDOW.getValue());
        mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
        mplew.write(1);
        PacketHelper.addItemInfo(mplew, item);
        mplew.writeInt(cubeId);
        mplew.writeInt(item.getPosition());
        mplew.writeInt(cubeQuantity);
        mplew.writeInt(cubePosition);
        return mplew.getPacket();
    }

    public static byte[] getBlackCubeStart(MapleCharacter chr, Item item, boolean up, int cubeId, int cubePosition, int remainCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BLACK_CUBE_WINDOW.getValue());
        mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
        mplew.write(1);
        PacketHelper.addItemInfo(mplew, item);
        mplew.writeInt(cubeId);
        mplew.writeInt(item.getPosition());
        mplew.writeInt(remainCount); // 325++
        mplew.writeInt(cubePosition);
        return mplew.getPacket();
    }

    public static byte[] getBlackCubeEffect(int cid, boolean up, int cubeId, int equipId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_BLACKCUBE_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(cubeId);
        mplew.writeInt(2460000); // 돋보기 ID
        mplew.writeInt(equipId);
        return mplew.getPacket();
    }

    public static byte[] getAnvilStart(Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
        mplew.write(0);
        mplew.writeInt(2);
        mplew.write(0);
        mplew.write(3);
        mplew.write(1);
        mplew.writeShort(item.getPosition());
        mplew.write(0);
        mplew.write(0);
        mplew.write(1);
        mplew.writeShort(item.getPosition());
        PacketHelper.addItemInfo(mplew, item);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showEnchanterEffect(int cid, byte result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_ENCHANTER_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(result);
        return mplew.getPacket();
    }

    public static byte[] showSoulScrollEffect(int cid, byte result, boolean destroyed, Equip equip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_SOULSCROLL_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(result);
        mplew.write(destroyed ? 1 : 0);
        mplew.writeInt(equip.getItemId()); //1.2.258+
        mplew.writeInt(equip.getSoulPotential()); //1.2.258+
        return mplew.getPacket();
    }

    public static byte[] showSoulEffect(MapleCharacter chr, byte on) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SHOW_SOULEFFECT_RESPONSE.getValue());
        packet.writeInt(chr.getId());
        packet.write(on);
        return packet.getPacket();
    }

    public static byte[] showSoulEffect(MapleCharacter chr, byte use, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_SOUL_EFFECT.getValue());
        mplew.writeInt(use);
        mplew.writeInt(skillid);
        mplew.writeInt(chr.getId());
        return mplew.getPacket();
    }

    public static byte[] teslaTriangle(int cid, int sum1, int sum2, int sum3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TESLA_TRIANGLE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(sum1);
        mplew.writeInt(sum2);
        mplew.writeInt(sum3);

        return mplew.getPacket();
    }

    public static byte[] harvestResult(int cid, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HARVESTED.getValue());
        mplew.writeInt(cid);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] playerDamaged(int cid, int dmg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_DAMAGED.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(dmg);
        mplew.write(false); // 332++

        return mplew.getPacket();
    }

    public static byte[] spawnDragon(MapleDragon d) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DRAGON_SPAWN.getValue());
        mplew.writeInt(d.getOwner());
        mplew.writeInt(d.getPosition().x);
        mplew.writeInt(d.getPosition().y);
        mplew.write(d.getStance()); //stance?
        mplew.writeShort(0);
        mplew.writeShort(d.getJobId());

        return mplew.getPacket();
    }

    public static byte[] removeDragon(int chrid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DRAGON_REMOVE.getValue());
        mplew.writeInt(chrid);

        return mplew.getPacket();
    }

    public static byte[] moveDragon(MapleDragon d, Point startPos, List<LifeMovementFragment> moves) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DRAGON_MOVE.getValue());
        mplew.writeInt(d.getOwner());
        mplew.writeInt(0);
        mplew.writePos(startPos);
        mplew.writeInt(0);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] spawnAndroid(MapleCharacter cid, MapleAndroid android) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_SPAWN.getValue());
        mplew.writeInt(cid.getId());
        mplew.write(GameConstants.getAndroidType(android.getItemId()));
        mplew.writePos(android.getPos());
        mplew.write(android.getStance());
        mplew.writeShort(0/*cid.getMap().getFootholds().findBelow(android.getPos()).getId()*/); // FH, fix the dropping of andriods upon changing map 
        //   mplew.writeShort(0); // andriod skin
        mplew.writeInt(0); // 333++
        mplew.writeShort(android.getSkin());
        mplew.writeShort(android.getHair() - 30000);
        mplew.writeShort(android.getFace() - 20000);
        mplew.writeMapleAsciiString(android.getName());
        mplew.writeInt(android.getEar() ? 0 : 1032024);
        mplew.writeLong(PacketHelper.getTime(-2)); //274++
        for (short i = -1200; i > -1207; i--) {
            final Item item = cid.getInventory(MapleInventoryType.EQUIPPED).getItem(i);
            mplew.writeInt(item != null ? item.getItemId() : 0);
        }

        return mplew.getPacket();
    }

    public static byte[] moveAndroid(int cid, Point pos, List<LifeMovementFragment> res, int unk1, int unk2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_MOVE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        mplew.writePos(pos);
        mplew.writeInt(0); //finish Movement Point maybe
        PacketHelper.serializeMovementList(mplew, res);

        return mplew.getPacket();
    }

    public static byte[] showAndroidEmotion(int cid, int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_EMOTION.getValue());
        mplew.writeInt(cid);
        mplew.write(0);
        mplew.write(animation); //1234567 = default smiles, 8 = throwing up, 11 = kiss, 14 = googly eyes, 17 = wink...

        return mplew.getPacket();
    }

    public static byte[] spawnHaku(MapleCharacter cid, MapleHaku haku) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HAKU_SPAWN.getValue());
        mplew.writeInt(cid.getId());
        mplew.writeShort(1); // type?
        mplew.writePos(haku.getPos());
        mplew.write(haku.getStance()); // moveAction
        mplew.writeShort(0/*cid.getMap().getFootholds().findBelow(android.getPos()).getId()*/); // FH, fix the dropping of andriods upon changing map 
        mplew.writeInt(0);//haku.getHair()); // upgrade
        mplew.writeInt(0);//haku.getFace()); // foxmanEquip
        return mplew.getPacket();
    }

    public static byte[] moveHaku(int cid, Point pos, List<LifeMovementFragment> res) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HAKU_MOVE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        mplew.writePos(pos);
        mplew.writeInt(Integer.MAX_VALUE); //time left in milliseconds? this appears to go down...slowly 1377440900
        PacketHelper.serializeMovementList(mplew, res);

        return mplew.getPacket();
    }

    public static byte[] updateAndroidLook(boolean itemOnly, MapleCharacter cid, MapleAndroid android) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_UPDATE.getValue());
        mplew.writeInt(cid.getId());
        mplew.write(itemOnly ? 1 : 0);
        if (itemOnly) {
            for (short i = -1200; i > -1207; i--) {
                final Item item = cid.getInventory(MapleInventoryType.EQUIPPED).getItem(i);
                mplew.writeInt(item != null ? item.getItemId() : 0); // cash item
            }
        } else {
            mplew.writeShort(0); // skin
            mplew.writeShort(android.getHair() - 30000);
            mplew.writeShort(android.getFace() - 20000);
            mplew.writeMapleAsciiString(android.getName());
        }

        return mplew.getPacket();
    }

    public static byte[] deactivateAndroid(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_DEACTIVATED.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] NameChanger(byte status) {
        return NameChanger(status, 0);
    }

    /*
     00 - 성공, 채널창이동
     01 - 알수없는이유 실패
     02 - 유효하지 않은 요청
     03 - 이름변경권x
     04 - 메이플 포인트 부족
     05 - 해당기능 사용불가
     06 - 변경할 수 없는 이름
     07 - 이미 사용중인이름
     08 - 이전요청처리중
     09 - 창띄우기, int형 아이템id
     0A - 2차비번 일치X
     0B - 거짓말탐지기 테스트 실패 10분후 다시이용
     */
    public static byte[] NameChanger(byte status, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NAME_CHANGER.getValue());
        mplew.write(status);
        if (status == 9) {
            mplew.writeInt(itemid);
        }
        return mplew.getPacket();
    }

    public static byte[] movePlayer(int cid, List<LifeMovementFragment> moves, Point startPos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        mplew.writePos(startPos);
        mplew.writePos(new Point(0, 0)); // finishPos?
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    // 0 = melee, 1 = range, 2 = strafe, 3 = magic, 4 = energy
    public static byte[] addAttackInfo(final int type, MapleCharacter chr, AttackInfo attack) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        switch (type) {
            case 0:
                mplew.writeShort(SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
                break;
            case 1:
            case 2:
                mplew.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
                break;
            case 3:
                mplew.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
                break;
            default:
                mplew.writeShort(SendPacketOpcode.BUFF_ATTACK.getValue());
                break;
        }

        mplew.writeInt(chr.getId());
        mplew.write(GameConstants.isEvan(chr.getJob()));
        mplew.write(attack.tbyte);
        mplew.writeInt(chr.getLevel());
        mplew.writeInt(attack.skilllevel);

        if (attack.skilllevel > 0) {
            mplew.writeInt(attack.skill);
        }

        if (GameConstants.isZeroSkill(attack.skill)) {
            mplew.write(attack.asist);
            if (attack.asist > 0) {
                mplew.writePos(attack.position);
            }
        }

        if ((type == 1 || type == 2) && (GameConstants.bullet_count_bonus(attack.skill) != 0 || GameConstants.attack_count_bonus(attack.skill) != 0)) {
            int passiveId = 0;
            int passiveLv = 0;
            if (GameConstants.bullet_count_bonus(attack.skill) == 0) {
                if (GameConstants.attack_count_bonus(attack.skill) == 0) {
                    passiveId = 0;
                    passiveLv = 0;
                } else {
                    passiveId = GameConstants.attack_count_bonus(attack.skill);
                    passiveLv = chr.getSkillLevel(passiveId);
                }
            } else {
                passiveId = GameConstants.bullet_count_bonus(attack.skill);
                passiveLv = chr.getSkillLevel(passiveId);
            }

            mplew.writeInt(passiveLv);

            if (passiveLv != 0) {
                mplew.writeInt(passiveId);
            }
        }

        if (attack.skill == 80001850) {
            int passiveLv = chr.getSkillLevel(80001851);
            mplew.writeInt(passiveLv);
            if (passiveLv != 0) {
                mplew.writeInt(80001851);
            }
        }

        mplew.write(attack.isShadowPartner);
        mplew.write(attack.isBuckShot);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        if ((attack.isBuckShot & 0x2) != 0) {
            if (chr.getBuffedValue(MapleBuffStat.Buckshot) == null) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            } else {
                mplew.writeInt(chr.getBuffSource(MapleBuffStat.Buckshot));
                mplew.writeInt(chr.getBuffedValue(MapleBuffStat.Buckshot));
            }
        }

        if ((attack.isBuckShot & 0x8) != 0) {
            mplew.write(attack.skilllevel);
        }

        mplew.writeShort(attack.display);
        mplew.write(attack.nMoveAction);

        if (GameConstants.isZero(chr.getJob()) && chr.getGender() == 1) {
            mplew.writeShort(0);
            mplew.writeShort(0);
        } else {
            if (attack.position != null && !GameConstants.isZeroSkill(attack.skill) && attack.skill != 400031016) {
                mplew.writeShort(attack.position.x);
                mplew.writeShort(attack.position.y);
            } else {
                mplew.writeShort(0);
                mplew.writeShort(0);
            }
        }

        mplew.write(attack.bShowFixedDamage);
        mplew.write(0);
        mplew.write(attack.speed);
        mplew.write(chr.getStat().passive_mastery());
        mplew.writeInt(attack.item);
        if (attack.allDamage != null) {
            for (final AttackPair oned : attack.allDamage) {
                if (oned.attack != null) {
                    mplew.writeInt(oned.objectid);
                    if (oned.objectid == 0) {
                        continue;
                    }
                    mplew.write(7);
                    mplew.write(0);
                    mplew.write(0);
                    mplew.writeShort(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    if (attack.skill == 80001835) {
                        mplew.write(oned.attack.size());
                        for (Pair<Long, Boolean> eachd : oned.attack) {
                            mplew.writeLong(eachd.left);
                        }
                    } else {
                        for (final Pair<Long, Boolean> eachd : oned.attack) {
                            if (eachd.right) {
                                mplew.writeLong(eachd.left | -Long.MAX_VALUE);
                            } else {
                                mplew.writeLong(eachd.left);
                            }
                        }
                    }
                    if (sub_6F2500(attack.skill) > 0) {
                        mplew.writeInt(0);
                    }
                    if (attack.skill == 37111005) {
                        mplew.write(chr.getPosition().x < attack.position.x ? 1 : 0);
                    } else if (attack.skill == 164001002) {
                        mplew.writeInt(0);
                    }
                }
            }
        }

        if (attack.skill == 2321001 || attack.skill == 2221052 || attack.skill == 11121052 || attack.skill == 12121054) {
            mplew.writeInt(attack.charge);
        }

        if (GameConstants.is_screen_attack_skill(attack.skill) || GameConstants.is_screen_attack(attack.skill) || attack.skill == 101000202 || attack.skill == 101000102 || GameConstants.is_thunder_rune(attack.skill) || attack.skill == 400041019 || attack.skill == 400031016 || attack.skill == 400041024 || GameConstants.sub_84ABA0(attack.skill) || attack.skill == 400021075 || attack.skill == 400001055 || attack.skill == 400001056) {
            mplew.writeInt(attack.position.x);
            mplew.writeInt(attack.position.y);
        }

        if (attack.skill == 80002452) {
            mplew.writeInt(attack.position.x);
            mplew.writeInt(attack.position.y);
        }
        if (attack.skill == 400011132 || attack.skill == 400011134) {
            mplew.writeInt(attack.position.x);
            mplew.writeInt(attack.position.y);
        }
        if (attack.skill == 400021097 || attack.skill == 400021098) {
            mplew.writeInt(attack.position.x);
            mplew.writeInt(attack.position.y);
        }
        if (attack.skill == 400051075) {
            mplew.write(0);
        } else {
            if (attack.skill == 400041062
                    || attack.skill == 400041079
                    || attack.skill == 400051080
                    || attack.skill == 400041074
                    || attack.skill == 400041064
                    || attack.skill == 400041065
                    || attack.skill == 400041066
                    || attack.skill == 400011125
                    || attack.skill == 400011126
                    || attack.skill == 155121007
                    || attack.skill == 80003017) {
                mplew.writeInt(attack.position.x);
                mplew.writeInt(attack.position.y);
            }
            if (attack.skill == 400051065 || attack.skill == 400051067) {
                mplew.writeInt(attack.plusPosition.x);
                mplew.writeInt(attack.plusPosition.y);
            }
            if (attack.skill == 400021107) {
                mplew.writeInt(0);
            }
            if (attack.skill == 0x3C2FF5D || attack.skill == 0x3C2FFC1 || attack.skill == 0x3C2FFC2) {
                mplew.writeInt(attack.position.x);
                mplew.writeInt(attack.position.y);
            }
            if (attack.skill == 162101009 || attack.skill == 162121017) {
                mplew.writeInt(attack.position.x);
                mplew.writeInt(attack.position.y);
            }
        }


        if (attack.skill == 13111020) {
            mplew.writePos(attack.plusPosition2);
        }

        if (attack.skill == 51121009) {
            mplew.write(attack.bShowFixedDamage);
        }

        if (attack.skill == 21120019 || attack.skill == 37121052 || GameConstants.is_shadow_assult(attack.skill) || attack.skill == 11121014 || attack.skill == 5101004) {
            mplew.write(attack.plusPos);
            mplew.writeInt(attack.plusPosition.x);
            mplew.writeInt(attack.plusPosition.y);
        }
        if (attack.skill == 400021088 || attack.skill == 400021113) {
            mplew.writeInt(attack.position.x);
            mplew.writeInt(attack.position.y);
            mplew.writeInt(attack.plusPosition.x);
            mplew.writeInt(attack.plusPosition.y);
        }
        if (GameConstants.sub_896310(attack.skill)) {
            mplew.writePos(attack.position);
            if (GameConstants.is_pathfinder_blast_skill(attack.skill)) {
                mplew.write(attack.skilllevel);
                mplew.writeInt(attack.skill);
            }
        }

        if (GameConstants.sub_896160(attack.skill) || attack.skill == 3301008 || attack.skill == 400031012) {
            mplew.writeInt(attack.skill);
            mplew.write(attack.skilllevel);
        }

        if (attack.skill == 155101104 || attack.skill == 155101204 || attack.skill == 400051042 || attack.skill == 151101003 || attack.skill == 151101004) {
            mplew.write(attack.across);
            if (attack.across) {
                mplew.writeInt(attack.acrossPosition.width);
                mplew.writeInt(attack.acrossPosition.height);
            }
        }

        if (attack.skill == 23121011 || attack.skill == 80001913) {
            mplew.write(attack.skilllevel);
        }

        if (attack.skill == 400011048 || attack.skill == 400041059) {
            mplew.writeZeroBytes(20);
        }

        mplew.writeZeroBytes(20);

        return mplew.getPacket();
    }

    public static int sub_6F2500(int a1) {
        if (a1 > 142111002) {
            if (a1 < 142120000 || a1 > 142120002 && a1 != 142120014) {
                return 0;
            }
        } else if (a1 != 142111002 && a1 != 142100010 && a1 != 142110003 && a1 != 142110015) {
            return 0;
        }
        return 1;
    }

    public static byte[] skillEffect(MapleCharacter from, int skillid, int level, short display, byte unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillid);
        mplew.write(level);
        mplew.writeShort(display);
        mplew.write(unk);

        if (skillid == 13111020) {
            mplew.writePos(from.getTruePosition());
        }

        return mplew.getPacket();
    }

    public static byte[] skillCancel(MapleCharacter from, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillid);

        return mplew.getPacket();
    }

    public static byte[] damagePlayer(int skill, int monsteridfrom, int cid, int damage) {
        return damagePlayer(cid, skill, damage, monsteridfrom, (byte) 0, 0, 0, false, 0, (byte) 0, null, (byte) 0, 0, 0);
    }

    public static byte[] damagePlayer(int cid, int type, int damage, int monsteridfrom, byte direction, int skillid, int pDMG, boolean pPhysical, int pID, byte pType, Point pPos, byte offset, int offset_d, int fake) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(type);
        mplew.writeInt(damage);
        mplew.write(0);
        mplew.write(false); // 274 ++
        mplew.write(0); // 332 ++
        if (type < -1) {
            if (type == -8) {
                mplew.writeInt(skillid);
                mplew.writeInt(pDMG);
                mplew.writeInt(0);
                mplew.write(0); //343+ ?
            }
        } else {
            mplew.writeInt(monsteridfrom);
            mplew.write(direction);
            mplew.writeInt(skillid);
            mplew.writeInt(0); // 274 ++
            mplew.writeInt(pDMG);
            mplew.write(0); // ?
            if (pDMG > 0) {
                mplew.write(pPhysical ? 1 : 0);
                mplew.writeInt(pID);
                mplew.write(pType);
                mplew.writePos(pPos);
            }

            mplew.write(offset);
            if ((offset & 0x1) != 0) {
                mplew.writeInt(offset_d);
            }
        }
        mplew.writeInt(damage);
        if (damage == -1) {
            mplew.writeInt(fake);
        }
        // System.out.println(mplew+"데미지센드");
        return mplew.getPacket();
    }

    public static byte[] facialExpression(MapleCharacter from, int expression) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        mplew.writeInt(-1); //itemid of expression use
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showTitle(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_TITLE.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        mplew.write(false);

        return mplew.getPacket();
    }

    public static void specialChairPacket(MaplePacketLittleEndianWriter mplew, MapleSpecialChair chair) {

        //sub_65B960
        mplew.writeInt(chair.getItemId());
        mplew.writeInt(chair.getPlayers().size());

        //초대할 수 있는 범위를 의미하는것 같은데
        mplew.writeRect(chair.getRect());

        //recv로 날라옴
        mplew.writeInt(chair.getPoint().x);
        mplew.writeInt(chair.getPoint().y);

        mplew.writeInt(chair.getPlayers().size());
        for (int i = 0; i < chair.getPlayers().size(); ++i) {
            boolean isCharEnable = chair.getPlayers().get(i).getPlayer() != null;
            mplew.writeInt(isCharEnable ? chair.getPlayers().get(i).getPlayer().getId() : 0); // charID
            mplew.write(isCharEnable); // isCharEnable
            mplew.writeInt(chair.getPlayers().get(i).getEmotion()); // randEmotion
        }
    }

    public static byte[] specialChair(MapleCharacter chr, boolean isCreate, boolean isShow, boolean isUpdate, MapleSpecialChair myChair) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPECIAL_CHAIR.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(isCreate); // 새로운 생성 시
        mplew.write(isShow); // 맵 첫 접속 시

        if (isShow) { // 기존의 의자, 현재 맵의 모든 스페셜 의자 표시
            //sub_65C680
            mplew.writeInt(chr.getMap().getAllSpecialChairs().size()); // size
            for (MapleSpecialChair chair : chr.getMap().getAllSpecialChairs()) {
                mplew.writeInt(chair.getObjectId()); // objectId
                mplew.write(isCreate);

                if (isCreate) {
                    specialChairPacket(mplew, chair);
                }
            }
        } else { // 새로운 의자, update 형식으로 추가
            //sub_65C4F0

            mplew.writeInt(myChair.getObjectId()); //  objectId
            mplew.write(isUpdate); // isUpdate

            if (isUpdate) {
                specialChairPacket(mplew, myChair);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] showChair(MapleCharacter chr, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(itemid);

        mplew.write(itemid != 0); // isCancel?

        if (itemid != 0) {
            PacketHelper.chairPacket(mplew, chr, itemid);
        }

        return mplew.getPacket();
    }

    public static byte[] updateCharLook(MapleCharacter chr, boolean DressUp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        byte flag = 1;
        if (GameConstants.isZero(chr.getJob())) {
            flag += 8;
        }
        mplew.write(flag);
        PacketHelper.addCharLook(mplew, chr, false, DressUp);
        if (GameConstants.isZero(chr.getJob())) {
            PacketHelper.addCharLook(mplew, chr, false, !DressUp);
        }
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(mplew, rings.getLeft());
        addRingInfo(mplew, rings.getMid());
        addMRingInfo(mplew, rings.getRight(), chr);
        mplew.writeInt(0);
        mplew.writeInt(0); // -> charid to follow (4)
        mplew.writeInt(0); // 262 ++
        mplew.writeInt(0); // 262 ++
        return mplew.getPacket();
    }

    public static byte[] ZeroTagUpdateCharLook(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        byte flag = 1;
        if (GameConstants.isZero(chr.getJob())) {
            flag += 8;
        }
        mplew.write(flag);
        PacketHelper.addCharLook(mplew, chr, false, chr.getGender() == 1);
        if (GameConstants.isZero(chr.getJob())) {
            PacketHelper.addCharLook(mplew, chr, false, chr.getGender() != 1);
        }
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(mplew, rings.getLeft());
        addRingInfo(mplew, rings.getMid());
        addMRingInfo(mplew, rings.getRight(), chr);
        mplew.writeInt(0); // -> charid to follow (4)
        mplew.writeInt(0); // 262 ++
        mplew.writeInt(0); // 262 ++
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] updatePartyMemberHP(int cid, int curhp, int maxhp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);
        return mplew.getPacket();
    }

    public static byte[] loadGuildName(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOAD_GUILD_NAME.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getGuildId() <= 0) {
            mplew.writeShort(0);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
            } else {
                mplew.writeShort(0);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] loadGuildIcon(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOAD_GUILD_ICON.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getGuildId() <= 0) {
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeInt(gs.getId());
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
                mplew.writeInt(gs.getCustomEmblem() != null && gs.getCustomEmblem().length > 0 ? 1 : 0);
                if (gs.getCustomEmblem() != null && gs.getCustomEmblem().length > 0) {
                    mplew.writeInt(gs.getId());
                    mplew.writeInt(gs.getCustomEmblem().length);
                    mplew.write(gs.getCustomEmblem());
                }
            } else {
                mplew.writeInt(0);
                mplew.writeShort(0);
                mplew.write(0);
                mplew.writeShort(0);
                mplew.write(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] showHarvesting(int cid, int tool) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_HARVEST.getValue());
        mplew.writeInt(cid);
        if (tool > 0) {
            mplew.writeInt(1); // update time
            mplew.writeInt(tool);
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] cancelChair(int id, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_CHAIR.getValue());

        mplew.writeInt(chr.getId());
        mplew.write(id != -1);
        if (id != -1) {
            mplew.writeShort(id);
        }

        return mplew.getPacket();
    }

    public static byte[] instantMapWarp(MapleCharacter chr, final byte portal) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CURRENT_MAP_WARP.getValue());
        mplew.write(0); // tick
        mplew.write(portal);
        mplew.writeInt(chr.getId());
        if (portal != 0) {
            mplew.writeShort(chr.getMap().getPortal(portal).getPosition().x);
            mplew.writeShort(chr.getMap().getPortal(portal).getPosition().y - 20);
        }

        return mplew.getPacket();
    }

    public static byte[] sendHint(String hint, int width, int height) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width < 1 ? Math.max(hint.length() * 10, 40) : width);
        mplew.writeShort(Math.max(height, 5));
        mplew.write(1); // if this is 0, 2 more int

        return mplew.getPacket();
    }

    public static byte[] aranCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AranCombo.getValue());
        mplew.writeInt(value);
        mplew.write(0); // 1.2.324 ++

        return mplew.getPacket();
    }

    public static byte[] rechargeCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AranCombo_RECHARGE.getValue());
        mplew.writeInt(value);
        mplew.write(0); // 1.2.324 ++

        return mplew.getPacket();
    }

    public static byte[] getGameMessage(int type, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAME_MESSAGE.getValue());
        mplew.writeShort(type);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] createUltimate(int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CREATE_ULTIMATE.getValue());
        mplew.writeInt(amount); //2 = no slots, 1 = success, 0 = failed

        return mplew.getPacket();
    }

    public static byte[] harvestMessage(int oid, int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HARVEST_MESSAGE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(msg);
        return mplew.getPacket();
    }

    public static byte[] openBag(int index, int itemId, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_BAG.getValue());
        mplew.writeInt(index);
        mplew.writeInt(itemId);
        mplew.writeShort(1); //this might actually be 2 bytes

        return mplew.getPacket();
    }

    public static byte[] fireBlink(int cid, Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CURRENT_MAP_WARP.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.writeInt(cid);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] skillCooldown(int sid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(1); // size
        mplew.writeInt(sid == 63101104 ? sid : sid == 63121102 ? sid : GameConstants.getLinkedSkill(sid));
        mplew.writeInt(time - 125); // ms

        return mplew.getPacket();
    }

    public static byte[] skillCooldown(Map<Integer, Integer> datas) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(datas.size()); // size
        for (Entry<Integer, Integer> data : datas.entrySet()) {
            mplew.writeInt(GameConstants.getLinkedSkill(data.getKey()));
            mplew.writeInt(data.getValue()); // ms
        }

        return mplew.getPacket();
    }

    public static byte[] dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte mod, boolean pickPocket) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(0);
        mplew.write(mod); // 1 animation, 2 no animation, 3 spawn disappearing item [Fade], 4 spawn disappearing item
        mplew.writeInt(drop.getObjectId()); // item owner id
        mplew.write(drop.getMeso() > 0 ? 1 : 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        mplew.writeInt(drop.isFlyingDrop() ? 1 : 0); //0 = 일반 드롭, 1 = 떨어지는 드롭
        mplew.writeInt(drop.getFlyingSpeed()); //떨어지는 속도 0x96
        mplew.writeInt(drop.getFlyingAngle()); //기울기 0x37 - 0xc7
        mplew.writeInt(drop.getItemId()); // drop object ID
        mplew.writeInt(drop.isFlyingDrop() ? 0 : drop.getOwner()); // owner charid
        mplew.write(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        mplew.writePos(dropto);
        mplew.writeInt(pickPocket ? 4048947 : /*drop.isFlyingDrop() ?*/ 0);// : 0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.write(0); // 350 new
        ///////////////////////////////
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        ////////////////////////////////
        mplew.write((drop.getItemId() / 1000000) == 1);
        mplew.write(0);
        if (mod != 2) {
            mplew.writePos(dropfrom);
            mplew.writeInt(pickPocket ? 735 : 120); // delay?
        }
        mplew.write(0);
        if (drop.getMeso() == 0) {
            PacketHelper.addExpirationTime(mplew, drop.getItem().getExpiration());
        }
        mplew.write(drop.isPlayerDrop() ? 0 : 1);
        mplew.write(0);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeInt(drop.isTouchDrop() ? 1 : 0); // 1 = 터치되면 먹어짐
        if ((drop.getItemId() / 1000000) == 1 && drop.getMeso() == 0 && drop.getEquip() != null) {
            if (drop.getEquip().getState() <= 4) {
                mplew.write(drop.getEquip().getState());
            } else if (drop.getEquip().getState() <= 20) {
                mplew.write(drop.getEquip().getState() - 16);
            } else {
                mplew.write(0);
            }
        } else {
            mplew.write(0);
        }
        mplew.write(drop.getItemId() == 2434851); // 274 ++
        mplew.writeInt(0); // 332++

        return mplew.getPacket();
    }

    public static byte[] explodeDrop(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(4); // 4 = Explode
        mplew.writeInt(oid);
        mplew.writeShort(0);
        mplew.writeShort(655);

        return mplew.getPacket();
    }

    public static byte[] removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, 0);
    }

    public static byte[] removeItemFromMap(int oid, int animation, int cid, int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); // 0 = Expire, 1 = without animation, 2 = pickup, 4 = explode, 5 = pet pickup
        mplew.writeInt(oid);
        switch (animation) {
            case 2:
            case 3:
            case 5:
                mplew.writeInt(cid);
                break;
            case 4:
                mplew.writeShort(0); // ?
                break;

        }
        if (animation == 5 || animation == 7) {
            mplew.writeInt(index); // pet Index
        }
        return mplew.getPacket();
    }

    public static byte[] spawnMist(final MapleMist mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(mist.getObjectId());
        mplew.write(mist.isMobMist() ? 1 : mist.isPoisonMist());
        mplew.writeInt(mist.getOwnerId());

        int skillId = mist.getSourceSkill() != null ? mist.getSourceSkill().getId() : mist.getMobSkill() != null ? mist.getMobSkill().getSkillId() : 0;
        if (mist.getMobSkill() == null) {
            switch (skillId) {
                case 21121057:
                    skillId = 21121068;
                    break;
                case 400011058:
                    skillId = 400011060;
                    break;
            }
            mplew.writeInt(skillId);
        } else {
            mplew.writeInt(mist.getMobSkill().getSkillId() == 202 ? 131 : mist.getMobSkill().getSkillId());
        }

        if (mist.getMobSkill() != null) {
            mplew.writeShort(mist.getMobSkill().getSkillId() == 202 ? 16 : mist.getMobSkill().getSkillLevel());
        } else {
            mplew.writeShort(mist.getSkillLevel());
        }

        mplew.writeShort(mist.getSkillDelay());
        mplew.writeRect(mist.getBox());
        if (skillId == 162111000) {
            mplew.writeRect(mist.getBox());
        }
        mplew.writeInt(mist.isPoisonMist());

        if (mist.getTruePosition() != null) {
            mplew.writePos(mist.getTruePosition());
        } else if (mist.getPosition() != null) {
            mplew.writePos(mist.getPosition());
        } else if (mist.getOwner() != null) {
            mplew.writePos(mist.getOwner().getTruePosition());
        } else if (mist.getMob() != null) {
            mplew.writePos(mist.getMob().getTruePosition());
        } else {
            mplew.writeShort(mist.getBox().x);
            mplew.writeShort(mist.getBox().y);
        }
        if (skillId == 186) { //빨려가는위치?
            if (mist.getSkillLevel() == 1) {
                mplew.writeShort(mist.getMob().getPosition().x);
                mplew.writeShort(0x8C);
            } else if (mist.getSkillLevel() == 2) {
                mplew.writeShort(mist.getMob().getPosition().x);
                mplew.writeShort(0xA0);
            } else if (mist.getSkillLevel() == 5) {
                mplew.writeShort(mist.getPosition().x == 965 ? -1035 : 290);
                mplew.writeShort(100);
            } else if (mist.getSkillLevel() == 6) {
                mplew.writeShort(mist.getPosition().x == 965 ? -1035 : 290);
                mplew.writeShort(150);
            } else if (mist.getSkillLevel() == 11) {
                mplew.writeShort(-45);
                mplew.writeShort(150);
            } else {
                mplew.writeInt(0);
            }
        } else {
            mplew.writeInt(0);
        }

        mplew.writeInt(skillId == 400011060 ? 100 : (skillId == 131 && mist.getSkillLevel() == 28) ? 5 : 0);
        mplew.write((skillId == 131 && mist.getSkillLevel() == 28));
        mplew.writeInt(skillId == 400011060 ? 200 : (mist.getMob() != null && mist.getMob().getId() / 10000 == 895) ? 210 : skillId == 186 ? 190 : 0);

        if (mist.getSource() != null) {
            if (sub_783400(mist.getSourceSkill().getId())) {
                mplew.write(mist.getRltype() == 0 ? 1 : 0);
            }
        }

        mplew.writeInt(mist.getDuration());
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(mist.getSource() != null ? mist.getSourceSkill().getId() == 151121041 ? true : false : false);
        mplew.write(false);
        //  System.out.println("spawnMist"+mplew);
        return mplew.getPacket();
    }

    public static boolean sub_783400(int a1) {
        boolean v1, v2;
        if (a1 > 35121052) {
            if (a1 == 400020046) {
                return true;
            }
            v1 = a1 == 400020051;
        } else {
            if (a1 == 35121052 || a1 == 33111013 || a1 - 33111013 == 9999) {
                return true;
            }
            v1 = a1 - 33111013 == 10003;
        }
        if (!v1) {
            if (a1 > 131001207) {
                if (a1 == 152121041 || a1 == 400001017) {
                    return true;
                }
                v2 = a1 == 400041041;
            } else {
                if (a1 == 131001207 || a1 == 4121015 || a1 == 0x30C07B9) {
                    return true;
                }
                v2 = a1 == 131001107;
            }
            if (!v2) {
                return false;
            }
        }
        return true;
    }

    public static byte[] removeMist(MapleMist mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(mist.getObjectId());
        mplew.writeInt(0); // 351 new
        if (mist.getSourceSkill() != null) {
            if (mist.getSourceSkill().getId() == 2111003) {
                mplew.write(0);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] spawnDoor(final int oid, final Point pos, final boolean animation) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_DOOR.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(oid);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] removeDoor(int oid, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_DOOR.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] spawnMechDoor(MechDoor md, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MECH_DOOR_SPAWN.getValue());
        mplew.write(animated ? 0 : 1);
        mplew.writeInt(md.getOwnerId());
        mplew.writePos(md.getTruePosition());
        mplew.write(md.getId());
//        mplew.writeInt(md.getPartyId());
        return mplew.getPacket();
    }

    public static byte[] removeMechDoor(MechDoor md, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MECH_DOOR_REMOVE.getValue());
        mplew.write(animated ? 0 : 1);
        mplew.writeInt(md.getOwnerId());
        mplew.write(md.getId());

        return mplew.getPacket();
    }

    public static byte[] triggerReactor(MapleReactor reactor, int stance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.writeShort(0); //274 ++
//        mplew.write(-1); //274 ++
        mplew.write(0); //274 ++
        mplew.writeInt(stance);
        mplew.writeInt(0); // 324 ++
        return mplew.getPacket();
    }

    public static byte[] spawnReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getReactorId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.write(reactor.getFacingDirection()); // stance
        mplew.writeMapleAsciiString(reactor.getName());
        return mplew.getPacket();
    }

    public static byte[] destroyReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(false); // 307++
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getPosition());
        mplew.write(0); // 333++
        return mplew.getPacket();
    }

    public static byte[] makeExtractor(int cid, String cname, Point pos, int timeLeft, int itemId, int fee) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_EXTRACTOR.getValue());
        mplew.writeInt(cid);
        mplew.writeMapleAsciiString(cname);
        mplew.writeInt(pos.x);
        mplew.writeInt(pos.y);
        mplew.writeShort(timeLeft); //fh or time left, dunno
        mplew.writeInt(itemId); //3049000, 3049001...
        mplew.writeInt(fee);

        return mplew.getPacket();
    }

    public static byte[] removeExtractor(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_EXTRACTOR.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(1); //probably 1 = animation, 2 = make something?

        return mplew.getPacket();
    }

    public static byte[] showChaosZakumShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAOS_ZAKUM_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] showChaosHorntailShrine(boolean spawned, int time) {
        return showHorntailShrine(spawned, time);
    }

    public static byte[] showHorntailShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HORNTAIL_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] messengerInvite(String from, int messengerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(3);
        mplew.writeMapleAsciiString(from);
        mplew.writeInt(0); // 332++, channel?
        mplew.write(1);//channel?
        mplew.writeInt(messengerid);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0);
        mplew.write(position);
        mplew.writeInt(0); // 332++
        PacketHelper.addCharLook(mplew, chr, true, GameConstants.isZero(chr.getJob()) && chr.getGender() == 1);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(position); // v140
        mplew.writeInt(chr.getJob());

        return mplew.getPacket();
    }

    public static byte[] removeMessengerPlayer(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(2);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static byte[] updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(8); // v140.
        mplew.write(position);
        PacketHelper.addCharLook(mplew, chr, true, GameConstants.isZero(chr.getJob()) && chr.getGender() == 1);
        mplew.writeMapleAsciiString(from);

        return mplew.getPacket();
    }

    public static byte[] joinMessenger(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(1);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static byte[] messengerChat(String charname, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(6);
        mplew.writeMapleAsciiString(charname);
        mplew.writeMapleAsciiString(text);
        PacketHelper.ChatPacket(mplew, charname, text);
        return mplew.getPacket();
    }

    public static byte[] messengerWhisperChat(String charname, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(7);
        mplew.writeMapleAsciiString(charname);
        mplew.writeMapleAsciiString(text);
        PacketHelper.ChatPacket(mplew, charname, text);
        return mplew.getPacket();
    }

    public static byte[] messengerNote(String text, int mode, int mode2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(text);
        mplew.write(mode2);

        return mplew.getPacket();
    }

    public static byte[] messengerLike(short like, String charname, String othername) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.writeShort(like);
        mplew.writeMapleAsciiString(charname);
        mplew.writeMapleAsciiString(othername);
        return mplew.getPacket();
    }

    public static byte[] resultSkill(MapleCharacter chr, int update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.writeShort(chr.getMatrixs().size());
        for (VMatrix matrix : chr.getMatrixs()) {
            mplew.writeInt(matrix.getId()); // 코어ID
            mplew.writeInt(matrix.getLevel()); // 매트릭스 POS
            mplew.writeInt(matrix.getMaxLevel()); // 강화 Lv
            mplew.writeLong(PacketHelper.getTime(-1));
        }
        mplew.write(update);
        return mplew.getPacket();
    }

    public static byte[] messengerCharInfo(final MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x0B);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeInt(chr.getLevel()); // 307++
        mplew.writeShort(chr.getJob());
        mplew.writeShort(chr.getSubcategory());
        mplew.writeInt(chr.getFame());
        mplew.writeInt(0); //?멸컧??
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
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] removeItemFromDuey(boolean remove, int Package) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x18);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);

        return mplew.getPacket();
    }

    public static byte[] checkFailedDuey() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(9);
        mplew.write(-1); // 0xFF = error
        return mplew.getPacket();
    }

    public static byte[] sendDuey(byte operation, List<MapleDueyActions> packages, List<MapleDueyActions> expired) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(operation);
        if (packages == null) {
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            return mplew.getPacket();
        }
        switch (operation) {
            case 9: { // Request 13 Digit AS
                mplew.write(1);
                // 0xFF = error
                break;
            }
            case 10: { // Open duey
                mplew.write(0);
                mplew.write(packages.size());
                for (MapleDueyActions dp : packages) {
                    mplew.writeInt(dp.getPackageId());
                    mplew.writeAsciiString(dp.getSender(), 13);
                    mplew.writeLong(dp.getMesos());
                    mplew.writeLong(PacketHelper.getTime(dp.getExpireTime()));
                    mplew.write(dp.isQuick() ? 1 : 0);
                    mplew.writeAsciiString(dp.getContent(), 100);
                    mplew.writeZeroBytes(101);
                    if (dp.getItem() != null) {
                        mplew.write(1);
                        PacketHelper.addItemInfo(mplew, dp.getItem());
                    } else {
                        mplew.write(0);
                    }
                }
                if (expired == null) {
                    mplew.write(0);
                    return mplew.getPacket();
                }
                mplew.write(expired.size());
                for (MapleDueyActions dp : expired) {
                    mplew.writeInt(dp.getPackageId());
                    mplew.writeAsciiString(dp.getSender(), 13);
                    mplew.writeLong(dp.getMesos());
                    if (dp.canReceive()) {
                        mplew.writeLong(PacketHelper.getTime(dp.getExpireTime()));
                    } else {
                        mplew.writeLong(0);
                    }
                    mplew.write(dp.isQuick() ? 1 : 0);
                    mplew.writeAsciiString(dp.getContent(), 100);
                    mplew.writeZeroBytes(101);
                    if (dp.getItem() != null) {
                        mplew.write(1);
                        PacketHelper.addItemInfo(mplew, dp.getItem());
                    } else {
                        mplew.write(0);
                    }
                }
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] receiveParcel(String from, boolean quick) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x1A);
        mplew.writeMapleAsciiString(from);
        mplew.write(quick ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] getKeymap(MapleKeyLayout layout) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KEYMAP.getValue());
        if (layout != null) {
            mplew.write(0);
            layout.writeData(mplew);
        } else {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static byte[] petAutoHP(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_HP.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] petAutoMP(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_MP.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static void addRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
    }

    public static void addMRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings, MapleCharacter chr) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeInt(ring.getItemId());
        }
    }

    public static byte[] updateInnerPotential(byte ability, int skill, int level, int rank) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ENABLE_INNER_ABILITY.getValue());
        mplew.write(1);
        mplew.write(1);
        mplew.writeShort(ability);
        mplew.writeInt(skill);
        mplew.writeShort(level);
        mplew.writeShort(rank);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] updateInnerAbility(final InnerSkillValueHolder skill, int index, boolean last) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.ENABLE_INNER_ABILITY.getValue());
        packet.write(last ? 1 : 0);
        packet.write(1);
        packet.writeShort(index);
        packet.writeInt(skill.getSkillId());
        packet.writeShort(skill.getSkillLevel());
        packet.writeShort(skill.getRank());
        packet.write(last ? 1 : 0);

        return packet.getPacket();
    }

    public static byte[] HeadTitle(List<Integer> num) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HEAD_TITLE.getValue());
        for (Integer num_ : num) {
            mplew.writeMapleAsciiString("");
            mplew.write(num_.intValue() == 0 ? -1 : num_.intValue());
        }
        return mplew.getPacket();
    }

    public static byte[] getInternetCafe(byte type, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //0A 00 03 3C 00 00 00
        mplew.writeShort(SendPacketOpcode.INTERNET_CAFE.getValue());

        mplew.write(type);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static class AuctionPacket {

        public static void auctionHistory(MaplePacketLittleEndianWriter mplew, AuctionHistory history) {
            //52 -> 60 Byte
            mplew.writeLong(history.getId()); // 307++
            mplew.writeInt(history.getAuctionId());
            mplew.writeInt(history.getAccountId());
            mplew.writeInt(history.getCharacterId());
            mplew.writeInt(history.getItemId());
            mplew.writeInt(history.getState());
            mplew.writeLong(history.getPrice()); // 아이템 템가격
            mplew.writeLong(PacketHelper.getTime(history.getBuyTime()));
            mplew.writeInt(history.getDeposit());
            mplew.writeInt(history.getDeposit());
            mplew.writeInt(history.getQuantity());
            mplew.writeInt(history.getWorldId());
        }

        public static void auctionItem(MaplePacketLittleEndianWriter mplew, AuctionItem item) { // 351 modify
            //122 -> 138 Byte
            mplew.writeInt(item.getAuctionId()); // dwAuctionID
            mplew.writeInt(item.getAuctionType()); // nAuctionType
            mplew.writeInt(item.getState()); // nState
            mplew.writeInt(item.getWorldId()); // nWorldID
            mplew.writeLong(0/*item.getPrice()*/); // nPrice
            mplew.writeLong(item.getSecondPrice()); // nSecondPrice
            mplew.writeLong(item.getDirectPrice()); // nDirectPrice
            mplew.writeLong(item.getDirectPrice()); // nDirectPrice
            mplew.writeLong(0);
            mplew.writeLong(PacketHelper.getTime(item.getEndDate())); // ftEndDate
            mplew.writeLong(PacketHelper.getTime(item.getRegisterDate())); // ftRegisterDate
            mplew.writeLong(item.getDeposit()); // nDeposit
            mplew.writeInt(1);
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.write(1);
            mplew.writeLong(0);
            mplew.writeInt(item.getAccountId()); // dwAccountID
            mplew.writeInt(item.getCharacterId()); // dwCharacterID
            mplew.writeAsciiString(item.getName(), 13); // sCharName
        }

        public static byte[] AuctionCompleteItems(List<AuctionItem> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(51);
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeInt(items.size());

            for (AuctionItem item : items) {

                //60바이트
                auctionHistory(mplew, item.getHistory());

                //boolean = 판매 성공 실패, 구매 성공 시 메소, 혹은 아이템을 받았는가?
                mplew.write(item.getState() <= 4);

                if (item.getState() <= 4) {
                    auctionItem(mplew, item);
                    PacketHelper.addItemInfo(mplew, item.getItem());
                }
            }
            return mplew.getPacket();
        }

        public static byte[] AuctionCompleteItemUpdate(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(71);
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeLong(item.getItem().getInventoryId()); // 이상한 헥스
            mplew.write(true);

            //60바이트
            auctionHistory(mplew, item.getHistory());

            //boolean = 판매 성공 실패, 구매 성공 시 메소, 혹은 아이템을 받았는가?
            mplew.write(item.getState() <= 4);

            if (item.getState() <= 4) {
                //138바이트
                auctionItem(mplew, item);

                PacketHelper.addItemInfo(mplew, item.getItem());
            }
            return mplew.getPacket();
        }

        public static byte[] AuctionCompleteItemUpdate(AuctionItem item, Item item2) {
            //item : 구매한 아이템 데이터, item2 : 판매한 아이템 데이터

            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(71);
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeLong(item.getItem().getInventoryId()); // 이상한 헥스
            mplew.write(true);

            auctionHistory(mplew, item.getHistory());
            mplew.write(true);

            if (true) {
                //138바이트
                auctionItem(mplew, item);

                PacketHelper.addItemInfo(mplew, item2);
            }
            return mplew.getPacket();
        }

        public static byte[] AuctionBuyItemUpdate(AuctionItem item, boolean remain) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(73);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());
            mplew.write(remain);

            if (remain) {
                //138 Byte
                auctionItem(mplew, item);

                PacketHelper.addItemInfo(mplew, item.getItem());
            }
            return mplew.getPacket();
        }

        public static byte[] AuctionSellingMyItems(List<AuctionItem> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(50);
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeInt(items.size());

            for (AuctionItem item : items) {

                //138Byte
                auctionItem(mplew, item);
                //End
                PacketHelper.addItemInfo(mplew, item.getItem());
            }
            return mplew.getPacket();
        }

        public static byte[] AuctionStopSell(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(12);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());

            return mplew.getPacket();
        }

        public static byte[] AuctionCompleteMesoResult() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(30);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] AuctionCompleteItemResult() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(31);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] AuctionWishlist(List<AuctionItem> wishItems) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(46);
            mplew.writeInt(0);
            mplew.writeInt(0);

            mplew.writeInt(wishItems.size()); // size, 일단 넘기겠삼

            for (AuctionItem item : wishItems) {
                //138Byte
                auctionItem(mplew, item);
                //End
                PacketHelper.addItemInfo(mplew, item.getItem());
            }

            return mplew.getPacket();
        }

        public static byte[] AuctionBuyEquipResult(int type, int dwAuctionID) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(20);
            mplew.writeInt(type);
            mplew.writeInt(dwAuctionID);

            return mplew.getPacket();
        }

        public static byte[] AuctionBuyItemResult(int type, int dwAuctionID) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(21);
            mplew.writeInt(type);
            mplew.writeInt(dwAuctionID);

            return mplew.getPacket();
        }

        public static byte[] AuctionWishlistUpdate(int dwAuctionID) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(72);
            mplew.writeInt(0);
            mplew.writeInt(dwAuctionID);

            mplew.write(false);
            return mplew.getPacket();
        }

        public static byte[] AuctionWishlistDeleteResult(int dwAuctionId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(47);
            mplew.writeInt(0);
            mplew.writeInt(dwAuctionId);
            return mplew.getPacket();
        }

        public static byte[] AuctionAddWishlist(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(72);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());

            mplew.write(true);

            //138Byte
            auctionItem(mplew, item);
            //End
            PacketHelper.addItemInfo(mplew, item.getItem());

            return mplew.getPacket();
        }

        public static byte[] AuctionOn() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] AuctionOff() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(1);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] AuctionWishlistResult(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(45);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());

            return mplew.getPacket();
        }

        public static byte[] AuctionMarketPrice(List<AuctionItem> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(41);
            mplew.writeInt(1000);
            mplew.writeInt(0);

            mplew.write(true); // ?
            mplew.writeShort(1);

            mplew.writeInt(items.size());

            List<AuctionItem> itemz = new CopyOnWriteArrayList<>();
            itemz.addAll(items);

            for (AuctionItem item : itemz) {
                item.setState(3);

                auctionItem(mplew, item);
                //End
                PacketHelper.addItemInfo(mplew, item.getItem());
            }
            System.err.println("옥션 : " + mplew.toString());
            return mplew.getPacket();
        }

        public static byte[] AuctionSearchItems(List<AuctionItem> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(40);
            mplew.writeInt(1000); // nMaxViewCountMaybe
            mplew.writeInt(0);

            mplew.write(true);

            mplew.writeShort(1);

            mplew.writeInt(items.size());

            for (AuctionItem item : items) {
                auctionItem(mplew, item);
                //End
                PacketHelper.addItemInfo(mplew, item.getItem());
            }

            return mplew.getPacket();
        }

        public static byte[] AuctionSellItemUpdate(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(70);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());

            mplew.write(true);

            //138Byte
            auctionItem(mplew, item);
            //End
            PacketHelper.addItemInfo(mplew, item.getItem());

            return mplew.getPacket();
        }

        public static byte[] AuctionSellItem(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(10);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());

            return mplew.getPacket();
        }

        public static byte[] AuctionReSellItem(AuctionItem item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.AUCTION.getValue());
            mplew.writeInt(11);
            mplew.writeInt(0);
            mplew.writeInt(item.getAuctionId());

            return mplew.getPacket();
        }
    }

    public static byte[] showSpineScreen(boolean isBinary, boolean isLoop, boolean isPostRender, String path, String animationName, int endDelay, boolean useKey, String key) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x1E);
        mplew.write(isBinary);//not .json file
        mplew.write(isLoop);
        mplew.write(isPostRender);
        mplew.writeInt(endDelay);
        mplew.writeMapleAsciiString(path);//e.g. "Map/Effect3.img/BossLucid/Lucid/lusi"
        mplew.writeMapleAsciiString(animationName);//e.g. "animation"
        mplew.writeMapleAsciiString(""); // 325++
        mplew.write(0); // 325 ++
        mplew.writeInt(0); // 325++
        mplew.writeInt(0); // 325++
        mplew.writeInt(0); // 332++
        mplew.writeInt(0); // 332++
        mplew.write(useKey);//use key
        if (useKey) {
            mplew.writeMapleAsciiString(key);//the key to stop animation?
        }
        return mplew.getPacket();
    }

    public static byte[] showBlackOutScreen(int delay, String path, String animationName, int unk, int unk2, int unk3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x17);
        mplew.write(0);
        mplew.writeInt(delay);
        mplew.writeMapleAsciiString(path);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(unk);
        mplew.writeMapleAsciiString(animationName);//e.g. "animation"
        mplew.writeInt(unk2);
        mplew.write(true);
        mplew.writeInt(unk3);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] removeBlackOutScreen(int delay, String path) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x17);
        mplew.write(2);
        mplew.writeInt(delay);
        mplew.writeMapleAsciiString(path);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] removeIntro(String animationName, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x1F);
        mplew.writeMapleAsciiString(animationName);//e.g. "animation"
        mplew.writeInt(delay);

        return mplew.getPacket();
    }

    public static byte[] spawnRune(MapleRune rune, boolean respawn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(respawn ? SendPacketOpcode.RESPAWN_RUNE.getValue() : SendPacketOpcode.SPAWN_RUNE.getValue());
        mplew.writeInt(respawn ? 1 : 0);
        mplew.writeInt(0);
        mplew.writeInt(2);
        mplew.writeInt(rune.getRuneType());
        mplew.writeInt(rune.getPositionX());
        mplew.writeInt(rune.getPositionY());
        mplew.write(0);

        /*
        01 00 
        00 00 00 00 00 00 
        02 00 00 00 
        03 00 00 00 
        12 0B 00 00 
        25 03 00 00 
        00


        00 00 00 00
        00 00 00 00
        02 00 00 00
        06 00 00 00
        F7 05 00 00
        E5 FE FF FF
        00
        342룬
         */
        return mplew.getPacket();
    }

    public static byte[] removeRune(MapleRune rune, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_RUNE.getValue());
        mplew.writeInt(0);
        mplew.writeInt(chr.getId());
        mplew.writeInt(100); // 200일 때도 있음
        mplew.write(0); // 307++
        mplew.write(0); // 307++
        return mplew.getPacket();
    }

    public static byte[] RuneAction(int type, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RUNE_ACTION.getValue());
        mplew.writeInt(type);
        if (type == 9) {
            mplew.write(0);
            mplew.write(1);
            mplew.write(0); // 351 new
            mplew.write(HexTool.getByteArrayFromHexString("E1 6B 18 90 18 00 00 00 E2 BC DE FF 7B 1D F3 E2 76 1D F3 E2 75 1D F3 E2 77 1D F3 E2 75 1D F3 E2"));
        } else {
            mplew.writeInt(time);
            mplew.writeInt(2);
            mplew.writeInt(3);
            mplew.writeInt(0);
        }
        /*
        * 09 00 00 00 00
        * 01
        * 35 A3 9F B0 18 00 00 00 2A 2D 58 74 17 CB 35 C2 12 CB 35 C2 1D CB 35 C2 12 CB 35 C2 13 CB 35 C2*/

        return mplew.getPacket();
    }

    public static byte[] showRuneEffect(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RUNE_EFFECT.getValue());
        mplew.writeInt(type);
        mplew.write(0); // 307++

        return mplew.getPacket();
    }

    public static byte[] MultiTag(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ZERO_MUlTITAG.getValue());
        mplew.writeInt(chr.getId());
        PacketHelper.addCharLook(mplew, chr, false, chr.getGender() == 1);
        return mplew.getPacket();
    }

    public static byte[] MultiTagRemove(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ZERO_MUlTITAG_REMOVE.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static byte[] getWpGain(final int gain) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());

        mplew.write(37); // or 36
        mplew.writeInt(gain);

        return mplew.getPacket();
    }

    public static byte[] updateWP(int wp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WP_UPDATE.getValue());
        mplew.writeInt(wp);
        return mplew.getPacket();
    }

    public static byte[] ZeroScroll(int scroll) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ZERO_SCROLL.getValue());
        mplew.write(0);
        mplew.writeInt(scroll);
        if (scroll == 0) { // fire
            mplew.writeInt(50000);
            mplew.writeInt(500);
        } else if (scroll == 1) { // cube
            mplew.writeInt(100000);
            mplew.writeInt(600);
        }
        mplew.write(false);
        mplew.write(false);
        mplew.write(false); // 325 ++
        return mplew.getPacket();
    }

    public static byte[] ZeroScrollStart() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ZERO_SCROLL_START.getValue());

        return mplew.getPacket();
    }

    public static byte[] WeaponInfo(int type, int level, int action, int weapon, int itemid, int quantity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ZERO_WEAPON_INFO.getValue());
        mplew.write(1);
        mplew.write(action);
        mplew.writeInt(type);
        mplew.writeInt(level);
        mplew.writeInt(weapon + 10001);
        mplew.writeInt(weapon + 1);
        mplew.writeInt(type + 1); // new
        mplew.writeInt(itemid); // new
        mplew.writeInt(quantity); // new
        mplew.write(0); // new
        return mplew.getPacket();
    }

    public static byte[] WeaponLevelUp() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ZERO_WEAPON_UPGRADE.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] Clothes(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ZERO_TAG.getValue());
        mplew.write(0);
        mplew.write(1);
        mplew.writeInt(value);
        return mplew.getPacket();
    }

    public static byte[] ZeroTag(MapleCharacter chr, byte Gender) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.ZERO_TAG.getValue());
        packet.writeShort(0xC7);
        packet.write(Gender);
        packet.writeInt(chr.getStat().getHp());
        packet.writeInt(chr.getStat().getMp() > 100 ? 100 : chr.getStat().getMp());
        packet.writeInt(chr.getStat().getMaxHp());
        packet.writeInt(chr.getStat().getMaxMp() > 100 ? 100 : chr.getStat().getMaxMp());

        return packet.getPacket();
    }

    public static byte[] Reaction() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INVENTORY_OPERATION.getValue());
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] OnOffFlipTheCoin(boolean on) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FlipTheCoin.getValue() + 2);
        mplew.write(on ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] replaceStolenSkill(int base, int skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REPLACE_SKILLS.getValue());
        mplew.write(1);
        mplew.write(skill > 0 ? 1 : 0);
        mplew.writeInt(base);
        mplew.writeInt(skill);

        return mplew.getPacket();
    }

    public static byte[] addStolenSkill(int jobNum, int index, int skill, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_STOLEN_SKILLS.getValue());
        mplew.write(1);
        mplew.write(0);
        mplew.writeInt(jobNum);
        mplew.writeInt(index);
        mplew.writeInt(skill);
        mplew.writeInt(level);
        mplew.writeInt(0);
//        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] removeStolenSkill(int jobNum, int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_STOLEN_SKILLS.getValue());
        mplew.write(1);
        mplew.write(3);
        mplew.writeInt(jobNum);
        mplew.writeInt(index);
//        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] viewSkills(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TARGET_SKILL.getValue());
        List<Integer> skillz = new ArrayList<Integer>();
        for (Skill sk : chr.getSkills().keySet()) {
            if (sk.canBeLearnedBy(chr) && !chr.getStolenSkills().contains(new Pair<>(sk.getId(), true)) && !chr.getStolenSkills().contains(new Pair<>(sk.getId(), false))) {
                skillz.add(Integer.valueOf(sk.getId()));
            }
        }
        mplew.write(1);
        mplew.writeInt(chr.getId());
        mplew.writeInt(skillz.isEmpty() ? 2 : 4);
        mplew.writeInt(chr.getJob());
        mplew.writeInt(skillz.size());
        for (Iterator<Integer> i$ = skillz.iterator(); i$.hasNext();) {
            int i = ((Integer) i$.next()).intValue();
            mplew.writeInt(i);
        }
        return mplew.getPacket();
    }

    public static byte[] updateCardStack(boolean unk, int total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PHANTOM_CARD.getValue());
        mplew.write(unk); // true : 40스택 쌓고 한번 더때려야 나감, false : 40스택 쌓으면 바로 나감
        mplew.write(total); // 324 ++

        return mplew.getPacket();
    }

    public static byte[] showVoydPressure(int cid, List<Byte> arrays) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_VOYD_PRESSURE.getValue());
        mplew.writeInt(cid);
        mplew.write(arrays.size());
        for (Byte aray : arrays) {
            mplew.write(aray);
        }

        return mplew.getPacket();
    }

    public static byte[] TheSidItem(int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.THE_SEED_ITEM.getValue());
        mplew.writeShort(7);
        mplew.writeInt(2028272);
        mplew.write(1);
        mplew.writeInt(args.length);
        for (int i = 0; i < args.length; i++) {
            mplew.writeInt(args[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] showForeignDamageSkin(MapleCharacter chr, int skinid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_DAMAGE_SKIN.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(skinid);

        //324++
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString("");
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] updateDress(int code, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_DRESS.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(code);

        return mplew.getPacket();
    }

    public static byte[] keepDress(boolean isDress) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KEEP_DRESSUP.getValue());
        mplew.write(isDress); // isDressup?
        mplew.write(1); // isInfinity

        return mplew.getPacket();
    }

    public static byte[] lockSkill(int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOCK_SKILL.getValue());
        mplew.writeInt(skillid);

        return mplew.getPacket();
    }

    public static byte[] unlockSkill() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UNLOCK_SKILL.getValue());

        return mplew.getPacket();
    }

    public static byte[] setPlayerDead() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SET_DEAD.getValue());
        packet.write(1);
        packet.writeInt(0); // 307++
        return packet.getPacket();
    }

    public static byte[] OpenDeadUI(MapleCharacter chr, int flag) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.OPEN_UI_DEAD.getValue());
        packet.writeInt(flag); //header,
        packet.write(0);
        int flag2 = (chr.getDeathCount() > 0 || chr.liveCounts() > 0) ? 4 : 0;
        packet.writeInt(flag2); // 부활 타입. 1 : 파티포인트
        packet.writeInt(flag == 3 ? 0 : -1); // 351 new
        packet.write(flag2 == 4); // 351 new
        packet.writeInt(flag2 == 4 ? 30 : 0); // 351 new
        packet.writeInt(flag2 == 4 ? 5 : 0); // 351 new
        packet.write(flag2 == 4 && (chr.getDeathCount() == 1 || chr.liveCounts() == 1)); // 351 new
        return packet.getPacket();
    }

    public static byte[] getDeathCount(byte count) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.DEATH_COUNT.getValue());
        packet.writeInt(count);

        return packet.getPacket();
    }

    public static byte[] sendDeathCountRespawn() {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BOSS_RESPAWN.getValue());
        packet.writeInt(30); //
        packet.writeInt(5); //
        packet.write(0); // 진힐라 구분용인가?

        return packet.getPacket();
    }

    public static byte[] enterAuction(final MapleCharacter chr) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENTER_AUCTION.getValue());
        PacketHelper.addCharacterInfo(mplew, chr);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static byte[] dailyGift(final MapleCharacter chr, int type, int itemId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAILY_GIFT.getValue());

        mplew.write(type != 1 ? 2 : 0);
        if (type != 1) {
            mplew.writeInt(type);
            mplew.writeInt(itemId);
        } else {
            mplew.write(1);
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
            mplew.writeLong(PacketHelper.getTime(-1));
            mplew.writeInt(28); // maxDate
            mplew.writeInt(2);
            mplew.writeInt(16700); // questExID
            mplew.writeInt(300); // reqMobCount

            mplew.writeInt(GameConstants.dailyItems.size());
            for (DailyGiftItemInfo item : GameConstants.dailyItems) {
                mplew.writeInt(item.getId());
                mplew.writeInt(item.getItemId());
                mplew.writeInt(item.getQuantity());
                mplew.write(1); // ?
                mplew.writeInt(item.getSN() > 0 ? 0 : 10080);//GameConstants.dailyGifts[i]);
                mplew.write(item.getSN() > 0); // isCash
                mplew.writeInt(item.getSN()); // CommodityId
                mplew.writeInt(0); // 274++
                mplew.write(0);
            }

            mplew.writeInt(ServerConstants.ReqDailyLevel);

            mplew.writeInt(0);

            mplew.writeInt(0); // MVP Bonus count

            //        for (int i = 0; i < 2; i++) {
            //      	mplew.writeInt(100 + i);
            //    	mplew.writeInt(40914);
            //  }
            mplew.writeInt(0); // MVP Bonus count

            /*        for (int i = 0; i < 7; i++) {
             mplew.writeInt(0);
             mplew.writeInt(0);
             mplew.writeInt(0);
             mplew.writeInt(0);
             }*/
        }
        return mplew.getPacket();
    }

    public static byte[] momentAreaOnOffAll(List<String> info) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.MOMENT_AREA_ON_OFF_ALL.getValue());
        packet.writeShort(0);
        packet.write(info.size() > 0 ? 1 : 0);
        if (info.size() > 0) {
            packet.writeInt(info.size());
            for (String list : info) {
                packet.writeMapleAsciiString(list);
            }
        }
        return packet.getPacket();
    }

    public static byte[] onUserTeleport(int x, int y) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.USER_TELEPORT.getValue());
        packet.writeInt(x);
        packet.writeInt(y);
        return packet.getPacket();
    }

    public static byte[] Respawn(int cid, int hp) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.RESPAWN.getValue());
        packet.writeInt(cid);
        packet.writeInt(hp);
        return packet.getPacket();
    }

    public static byte[] showProjectileEffect(MapleCharacter chr, int x, int y, int delay, int skillId, int level, int unk, byte facingleft, int objectId) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.SHOW_PROJECTILE_EFFECT.getValue());
        pw.writeInt(chr.getId());
        pw.writeInt(1);
        pw.writeInt(x);
        pw.writeInt(y);
        pw.writeInt(delay);
        pw.writeInt(skillId);
        pw.writeInt(unk);
        pw.writeInt(level);
        pw.write(facingleft);
        pw.writeInt(objectId);
        pw.writeInt(0); // 351 NEW
        pw.writeInt(0); // 351 NEW

        return pw.getPacket();
    }

    public static byte[] updateProjectileEffect(int id, int unk1, int unk2, int unk3, int unk4, byte facingleft) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.UPDATE_PROJECTILE_EFFECT.getValue());
        pw.writeInt(id);
        pw.writeInt(unk1);
        pw.writeInt(unk2);
        pw.writeInt(unk3);
        pw.writeInt(unk4);
        pw.write(facingleft);

        return pw.getPacket();
    }

    public static byte[] removeProjectile(int unk) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.REMOVE_PROJECTILE.getValue());
        pw.writeInt(unk);

        return pw.getPacket();
    }

    public static byte[] removeProjectileEffect(int id, int unk) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.REMOVE_PROJECTILE_EFFECT.getValue());
        pw.writeInt(id);
        pw.writeInt(unk);

        return pw.getPacket();
    }

    public static byte[] bonusAttackRequest(int skillid, List<Triple<Integer, Integer, Integer>> mobList, boolean unk, int jaguarBleedingAttackCount) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.BONUS_ATTACK_REQUEST.getValue());
        pw.writeInt(skillid);
        pw.writeInt(mobList.size());
        pw.write(unk);
        pw.writeInt(0);
        pw.writeInt(jaguarBleedingAttackCount);

        for (Triple<Integer, Integer, Integer> mob : mobList) {
            pw.writeInt(mob.getLeft());
            pw.writeInt(mob.getMid());
            if (skillid == 400041030) {
                pw.writeInt(mob.getRight());
            }
        }

        if (skillid == 400051067 || skillid == 400051065) {
            pw.writeInt(0);
            pw.writeInt(0);
            pw.writeInt(0);
            pw.writeInt(0);
            pw.writeInt(0);
            pw.write(0);
        }
        if (skillid == 400011133) {
            pw.writeInt(0);
        }

        return pw.getPacket();
    }

    public static byte[] ShadowServentExtend(Point newpos) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.SHADOW_SERVENT_EXTEND.getValue());
        pw.writeInt(newpos.x);
        pw.writeInt(newpos.y);

        return pw.getPacket();
    }

    public static byte[] DebuffObjON(int data) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.DEBUFF_OBJECT.getValue());
        pw.write(1);
        pw.writeInt(data);
        pw.writeInt(1);
        pw.writeMapleAsciiString("sleepGas" + data * 10);
        pw.writeMapleAsciiString("sleepGas");
        pw.write(0);

        return pw.getPacket();
    }

    public static byte[] lightningUnionSubAttack(int attackskillid, int skillid, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LIGHTING_ATTACK.getValue());
        mplew.writeInt(attackskillid);
        mplew.writeInt(skillid);
        mplew.writeInt(skillLevel);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] openUnionUI(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        List<MapleUnion> equipped = new ArrayList<>();

        for (MapleUnion union : c.getPlayer().getUnions().getUnions()) {
            if (union.getPosition() != -1) {
                equipped.add(union);
            }
        }

        mplew.writeShort(SendPacketOpcode.OPEN_UNION.getValue());
        mplew.writeInt(0); // 획득 가능한 코인 수
        mplew.writeInt(0); // 유니온 등급?
        mplew.writeInt(c.getPlayer().getUnions().getUnions().size());
        for (MapleUnion chr : c.getPlayer().getUnions().getUnions()) { // 모든 유니온 (밑에 카드)
            mplew.writeInt(1); // 2라면 아스키 1개 더 추가
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

    public static byte[] finalAttackRequest(int a1, int skillId, int FinalAttackId, int weaponType, List<MapleMonster> monster) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.FINAL_ATTACK_REQUEST.getValue());
        packet.writeInt(a1);
        packet.writeInt(skillId);
        packet.writeInt(FinalAttackId);

        packet.writeInt(weaponType);
        packet.writeInt(monster.size());
        for (MapleMonster m : monster) {
            packet.writeInt(m.getObjectId());
        }
        return packet.getPacket();
    }

    public static byte[] RoyalGuardDamage() {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.ROYAL_DAMAGE.getValue());
        pw.write(0);
        return pw.getPacket();
    }

    public static byte[] EnterFieldPyschicInfo() {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        pw.writeShort(SendPacketOpcode.ENTER_FIELD_PSYCHIC_INFO.getValue());
        pw.write(0);
        return pw.getPacket();
    }

    public static byte[] enforceMSG(String a, int id, int delay) {
        return enforceMSG(a, id, delay, false);
    }

    public static byte[] enforceMSG(String a, int id, int delay, boolean unk) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.ENFORCE_MSG.getValue());
        packet.writeMapleAsciiString(a);
        packet.writeInt(id);
        packet.writeInt(delay);
        packet.write(unk);

        return packet.getPacket();
    }

    public static byte[] spawnSubSummon(short type, int key) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.SPAWN_SUB_SUMMON.getValue());
        packet.writeInt(type);
        packet.writeInt(key);

        return packet.getPacket();
    }

    public static byte[] jaguarAttack(int skillid) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.JAGUAR_ATTACK.getValue());
        packet.writeInt(skillid);
        return packet.getPacket();
    }

    public static byte[] B2BodyResult(MapleCharacter chr, short type, short type2, int key, Point pos, Point oldPos, short unk1, int sourceid, int level, int duration, short unk2, boolean isFacingLeft, int unk3, int unk4, String unk) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.B2BODY_RESULT.getValue());
        packet.writeShort(type);
        packet.writeInt(chr.getId());
        packet.writeInt(chr.getMapId());

        if (type == 0) {
            packet.writeShort(1);
            packet.writeInt(key); // bodyId
            packet.write(type2);
            packet.write(0);
            packet.writePos(pos);
            if (type2 == 5) {
                packet.writePos(oldPos);
            } else if (type2 == 6) {
                packet.writeInt(0);
            }

            //?
            packet.writeShort(unk1);
            packet.writeInt(duration);
            packet.writeShort(unk2);

            packet.writeInt(sourceid);
            packet.writeShort(level);
            packet.write(0);
        } else if (type == 3) {
            packet.writeInt(chr.getId());
            packet.writeInt(sourceid);
            packet.writeInt(unk3);
            packet.writeInt(unk4);
        } else if (type == 4) {
            packet.writeShort(1);
            packet.write(0);
            packet.writePos(pos);
            packet.writeInt(900); // delay
            packet.writeShort(type2);
            packet.writeShort(unk1);
            packet.writeShort(unk2);
            packet.write(unk3);
            if (unk3 > 0) {
                packet.writeMapleAsciiString(unk);
            }
            packet.writeInt(unk4);
            packet.writeInt(sourceid);
            packet.write(isFacingLeft);
            packet.writeInt(0);
            packet.writeInt(0);
            packet.write(0);

            packet.writeInt(isFacingLeft ? -oldPos.x : oldPos.x);
            packet.writeInt(oldPos.y);
        }
        return packet.getPacket();
    }

    public static byte[] blackJack(Point point) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.writeShort(SendPacketOpcode.BLACKJACK.getValue());
        packet.writeInt(400041024);
        packet.writeInt(1);
        packet.writeInt(1);
        packet.writeInt(point.x);
        packet.writeInt(point.y);
        return packet.getPacket();
    }

    public static byte[] rangeAttack(int firstSkill, List<Integer> skills, int ballcount, Point pos, boolean isLeft) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();

        packet.writeShort(SendPacketOpcode.RANGE_ATTACK.getValue());
        packet.writeInt(firstSkill);
        packet.writeShort(skills.size());

        for (int skill : skills) {
            packet.writeInt(0); // 351 new
            packet.writeInt(skill);
            packet.writeInt(pos.x);
            packet.writeInt(pos.y);
            packet.writeShort(isLeft ? 1 : 0);
            packet.writeInt(0);
            packet.writeInt(ballcount);
            int v1 = 0, v2 = 0;
            packet.writeInt(v1); //unk342
            for (int i = 0; i < v1; i++) {
                packet.writeInt(0);
            }
            packet.writeInt(v2); //unk342
            for (int i = 0; i < v2; i++) {
                packet.writeInt(0);
            }
        }

        return packet.getPacket();
    }

    public static byte[] rangeAttack(int useSkill, List<RangeAttack> list) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();

        packet.writeShort(SendPacketOpcode.RANGE_ATTACK.getValue());
        packet.writeInt(useSkill);
        packet.writeShort(list.size());

        for (RangeAttack skill : list) {
            packet.writeInt(0); // 351 new
            packet.writeInt(skill.getSkill());
            packet.writeInt(skill.getPos().x);
            packet.writeInt(skill.getPos().y);
            packet.writeShort(skill.getDirection());
            packet.writeInt(skill.getDelay());
            packet.writeInt(skill.getAttackCount());
            int v1 = 0, v2 = 0;
            packet.writeInt(v1); //unk342
            for (int i = 0; i < v1; i++) {
                packet.writeInt(0);
            }
            packet.writeInt(v2); //unk342
            for (int i = 0; i < v2; i++) {
                packet.writeInt(0);
            }
        }

        return packet.getPacket();
    }

    public static byte[] createMagicWreck(MapleMagicWreck mw) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();

        packet.writeShort(SendPacketOpcode.CREATE_MAGIC_WRECK.getValue());
        packet.writeInt(mw.getChr().getId());
        packet.writeInt(mw.getTruePosition().x);
        packet.writeInt(mw.getTruePosition().y);
        packet.writeInt(mw.getDuration());
        packet.writeInt(mw.getObjectId());
        packet.writeInt(mw.getSourceid());
        packet.writeInt(0);
        packet.writeInt(mw.getChr().getMap().getWrecks().size());

        return packet.getPacket();
    }

    public static byte[] removeMagicWreck(MapleCharacter chr, List<MapleMagicWreck> mws) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();

        packet.writeShort(SendPacketOpcode.REMOVE_MAGIC_WRECK.getValue());
        packet.writeInt(chr.getId());
        packet.writeInt(mws.size());
        packet.write(0);
        packet.write(0);//342++
        for (MapleMagicWreck mw : mws) {
            packet.writeInt(mw.getObjectId());
        }

        return packet.getPacket();
    }

    public static byte[] ForceAtomAttack(int atomid, int cid, int mobid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FORCE_ATOM_ATTACK.getValue());
        mplew.writeInt(atomid);
        mplew.writeInt(cid);
        mplew.writeInt(1);
        mplew.writeInt(1);
        mplew.writeInt(1);
        mplew.writeInt(mobid);
        return mplew.getPacket();
    }

    public static byte[] GuidedArrow(MapleCharacter chr, int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CREATE_FORCE_ATOM.getValue() + 3);
        mplew.writeInt(2);
        mplew.writeInt(chr.getId());
        mplew.writeInt(1);
        mplew.writeInt(1);
        mplew.writeInt(1);
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static byte[] screenAttack(int mobId, int skillId, int skillLevel, long damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SCREEN_ATTACK.getValue());
        mplew.writeInt(mobId);
        mplew.writeInt(skillId);
        mplew.writeInt(skillLevel);
        mplew.writeLong(damage);
        return mplew.getPacket();
    }

    public static byte[] mutoSetTime(int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HUGNRY_MUTO.getValue());
        mplew.writeInt(1);
        mplew.writeInt(time); // normal : 600000 (10m)
        return mplew.getPacket();
    }

    public static byte[] finishMuto() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HUGNRY_MUTO.getValue());
        mplew.writeInt(2);

        return mplew.getPacket();
    }

    public static byte[] setMutoNewRecipe(int[] recipe, int length, EventInstanceManager eim) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HUGNRY_MUTO.getValue());
        mplew.writeInt(3);

        mplew.writeInt(recipe[0]);
        mplew.writeInt(recipe[1]);
        mplew.writeInt(recipe[2]);
        mplew.writeInt(recipe[3]);
        mplew.writeInt(recipe[4]);

        mplew.writeInt(length);

        for (int i = 0; i < length; ++i) {
            if (eim.getProperty("recipeHidden" + i) != null) {
                mplew.writeInt(0);
            } else {
                mplew.writeInt(Integer.parseInt(eim.getProperty("recipeItem" + i)));
            }
            mplew.writeInt(Integer.parseInt(eim.getProperty("recipeReq" + i)));
            mplew.writeInt(Integer.parseInt(eim.getProperty("recipeCount" + i)));
        }
        return mplew.getPacket();
    }

    public static byte[] setMutoRecipe(int[] recipe, int length, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HUGNRY_MUTO.getValue());
        mplew.writeInt(4);

        mplew.writeInt(recipe[0]);
        mplew.writeInt(recipe[1]);
        mplew.writeInt(recipe[2]);

        mplew.writeInt(length);

        for (int i = 0; i < length; ++i) {
            mplew.writeInt(Integer.parseInt(chr.getEventInstance().getProperty("recipeItem" + i)));
            mplew.writeInt(Integer.parseInt(chr.getEventInstance().getProperty("recipeReq" + i)));
            mplew.writeInt(Integer.parseInt(chr.getEventInstance().getProperty("recipeCount" + i)));
        }
        return mplew.getPacket();
    }

    public static byte[] addItemMuto(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.HUGNRY_MUTO.getValue());
        mplew.writeInt(5);

        mplew.writeInt(1);

        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getRecipe().left - 1599086);
        mplew.writeInt(chr.getRecipe().right);
        return mplew.getPacket();
    }

    public static byte[] ChainArtsFury(Point truePosition) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHAINARTS_FURY.getValue());
        mplew.writeInt(truePosition.x);
        mplew.writeInt(truePosition.y);
        return mplew.getPacket();
    }

    public static byte[] ICBM(boolean cancel, int skillid, Rectangle calculateBoundingBox) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ICBM.getValue());
        mplew.writeInt(cancel ? 0 : 1);
        mplew.writeInt(skillid);
        mplew.writeInt(0);
        mplew.writeInt(calculateBoundingBox.x);
        mplew.writeInt(calculateBoundingBox.y);
        mplew.writeInt(calculateBoundingBox.width);
        mplew.writeInt(calculateBoundingBox.height);
        return mplew.getPacket();
    }

    public static byte[] specialMapSound(String str) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPECIAL_MAP_SOUND.getValue());
        mplew.writeMapleAsciiString(str);
        return mplew.getPacket();
    }

    public static byte[] specialMapEffect(int type, boolean isEliteMonster, String bgm, String back, String effect, String obj, String tile) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPECIAL_MAP_EFFECT.getValue());
        mplew.writeInt(type);
        mplew.writeInt(isEliteMonster ? 1 : 0);
        mplew.writeInt(0); // 325++

        switch (type - 2) {
            case 0:
            case 3:
                mplew.writeMapleAsciiString(bgm);
                mplew.writeMapleAsciiString(back);
                mplew.writeMapleAsciiString(effect);
                break;
            case 1:
            case 2:
                mplew.write(true); // false하면 아무일도 안일어남
                mplew.writeMapleAsciiString(bgm);
                mplew.writeMapleAsciiString(back);
                mplew.writeMapleAsciiString(effect);
                mplew.writeMapleAsciiString(obj);
                mplew.writeMapleAsciiString(tile);
                mplew.write(0);
                break;
            case 4:
                mplew.writeMapleAsciiString(bgm);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] unstableMemorize(int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNSTABLE_MEMORIZE.getValue());

        mplew.writeInt(skillId);
        mplew.writeInt(16);

        return mplew.getPacket();
    }

    public static byte[] SpiritFlow(List<Pair<Integer, Integer>> skills) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SPIRIT_FLOW.getValue());

        mplew.writeInt(skills.size());
        for (Pair<Integer, Integer> skill : skills) {
            mplew.writeInt(skill.left);
            mplew.writeInt(skill.right);
        }

        return mplew.getPacket();
    }

    public static byte[] airBone(MapleCharacter chr, MapleMonster mob, int skill, int level, int end) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.AIRBONE.getValue());

        mplew.writeInt(chr.getId());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(skill);
        mplew.writeInt(level);
        mplew.writeInt(end);
        mplew.writeInt(0); // 332++
        return mplew.getPacket();
    }

    public static byte[] poisonNova(MapleCharacter chr, List<Integer> novas) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.POISON_NOVA.getValue());

        mplew.writeInt(chr.getId());
        mplew.writeInt(novas.size());
        for (int nova : novas) {
            mplew.writeInt(nova);
        }

        return mplew.getPacket();
    }

    public static byte[] runeCurse(String string, boolean delete) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RUNE_CURSE.getValue());
        mplew.writeMapleAsciiString(string);
        mplew.writeInt(231);
        mplew.write(delete);
        mplew.write(delete);
        mplew.writeInt(0); // 332++
        mplew.writeInt(0); // 351 new
        return mplew.getPacket();
    }

    public static byte[] buffFreezer(int itemId, boolean use) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BUFF_FREEZER.getValue());
        mplew.writeInt(itemId);
        mplew.write(use);
        return mplew.getPacket();
    }

    public static byte[] quickSlot(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.QUICK_SLOT.getValue());
        mplew.write(true);
        for (int i = 0; i < 32; ++i) {
            mplew.writeInt(chr.getKeyValue(333333, "quick" + i) < 0 ? 0 : chr.getKeyValue(333333, "quick" + i));
        }

        return mplew.getPacket();
    }

    public static byte[] ignitionBomb(int skillId, int objectId, Point pos) {
        //BD A1 B8 00 91 FD FF FF FE FF FF FF E9 4D A9 01 05 00 00 00

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.IGNITION_BOMB.getValue());
        mplew.writeInt(skillId);
        mplew.writeInt(pos.x);
        mplew.writeInt(pos.y);
        mplew.writeInt(objectId);
        mplew.writeInt(5); // dmg?

        return mplew.getPacket();
    }

    public static byte[] quickMove(List<QuickMoveEntry> quicks) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.QUICK_MOVE.getValue());
        mplew.write(quicks.size());

        for (QuickMoveEntry quick : quicks) {
            mplew.writeInt(quick.getType());
            mplew.writeInt(quick.getId());
            mplew.writeInt(quick.getIcon());
            mplew.writeInt(quick.getLevel());
            mplew.writeMapleAsciiString(quick.getDesc());
            mplew.writeLong(PacketHelper.getTime(-2));
            mplew.writeLong(PacketHelper.getTime(-1));
        }

        return mplew.getPacket();
    }

    public static byte[] dimentionMirror(List<DimentionMirrorEntry> quicks) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.DIMENTION_MIRROR.getValue());
        mplew.writeInt(quicks.size());

        for (DimentionMirrorEntry quick : quicks) {
            mplew.writeMapleAsciiString(quick.getName());
            mplew.writeMapleAsciiString(quick.getDesc());
            mplew.writeInt(quick.getLevel());
            mplew.writeInt(quick.getType()); // id
            mplew.writeInt(quick.getId()); // index
            mplew.writeInt(0);
            mplew.writeInt(0); // questId
            mplew.writeInt(0); // 333++
            mplew.writeMapleAsciiString(""); // 333++
            mplew.write(false);
            mplew.writeInt(quick.getItems().size());
            for (Item item : quick.getItems()) {
                mplew.writeInt(item.getItemId());
            }
        }

        return mplew.getPacket();
    }

    public static byte[] TimeCapsule() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TIME_CAPSULE.getValue());

        return mplew.getPacket();
    }

    public static byte[] NettPyramidWave(int wave) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NETT_PYRAMID_WAVE.getValue());
        mplew.writeInt(wave);
        return mplew.getPacket();
    }

    public static byte[] NettPyramidLife(int life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NETT_PYRAMID_LIFE.getValue());
        mplew.writeInt(life);
        return mplew.getPacket();
    }

    public static byte[] NettPyramidPoint(int point) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NETT_PYRAMID_POINT.getValue());
        mplew.writeInt(point);
        return mplew.getPacket();
    }

    public static byte[] NettPyramidClear(boolean clear, int wave, int life, int point, int exp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NETT_PYRAMID_CLEAR.getValue());
        mplew.write(clear);
        mplew.writeInt(wave);
        mplew.writeInt(life);
        mplew.writeInt(point);
        mplew.writeInt(exp);

        return mplew.getPacket();
    }

    public static byte[] ImageTalkNpc(int npcid, int time, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.IMAGE_NPC_TALK.getValue());
        mplew.writeInt(npcid);
        mplew.writeInt(time);
        mplew.writeMapleAsciiString(message);
        mplew.writeMapleAsciiString("");
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] inviteChair(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INVITE_CHAIR.getValue());
        mplew.writeInt(value);
        return mplew.getPacket();
    }

    public static byte[] requireChair(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REQUIRE_CHAIR.getValue());
        mplew.writeInt(id);
        return mplew.getPacket();
    }

    public static byte[] resultChair(int v1, int v2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RESULT_CHAIR.getValue());
        mplew.writeInt(v1);
        mplew.writeInt(v2);
        return mplew.getPacket();
    }

    public static byte[] fishing(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FISHING.getValue());
        mplew.writeInt(type);

        switch (type) {
            case 0:
            case 1:
            case 3:
                break;
            case 2:
                //임시 처리
                mplew.write(HexTool.getByteArrayFromHexString("00 00 00 40 0B 16 40 40 00 00 00 00 00 00 00 2E 40 00 00 00 00 00 80 41 40 01 00 00 00 00 00 00 00 00 C0 58 40 04 00 00 00 00 00 00 00 F4 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 B8 0B 00 00 B8 0B 00 00 00 00 00 A0 99 99 B9 3F 00 00 00 A0 99 99 C9 3F 02 00 00 00 E8 03 00 00 D0 07 00 00 00 00 00 A0 99 99 A9 BF 00 00 00 00 00 00 00 00 03 00 00 00 F4 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] fishingResult(int cid, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FISHING_RESULT.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(itemId);
        return mplew.getPacket();
    }

    public static byte[] ReturnSynthesizing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.RETURN_SYNTHESIZING.getValue());
        mplew.writeInt(0);
        mplew.writeInt(2432805); // itemid
        mplew.writeInt(0);
        mplew.writeInt(0xA609ED48); // unk
        return mplew.getPacket();
    }

    public static byte[] StigmaTime(int i) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.STIGMA_TIME.getValue());
        mplew.writeInt(i);
        return mplew.getPacket();
    }

    public static byte[] UseSkillWithUI(int unk, int skillid, int skilllevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USE_SKILL_WITH_UI.getValue());
        mplew.writeInt(unk);
        if (unk > 0) {
            mplew.writeInt(unk); // key위치?
            mplew.write(false);
            mplew.writeInt(1);
            mplew.write(false);
            mplew.writeInt(skillid);
            mplew.writeInt(skilllevel);
            mplew.writeZeroBytes(23); // unk
        }
        return mplew.getPacket();
    }

    public static byte[] potionCooldown() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.POTION_COOLDOWN.getValue());
        return mplew.getPacket();
    }

    public static byte[] JinHillah(int type, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.JIN_HILLAH.getValue());

        mplew.writeInt(type);

        switch (type) {
            case 0: // updateCandle
                mplew.writeInt(chr.getMap().getCandles()); // All candles
                mplew.write(false); // unk
                break;
            case 1: // handleCandle
                mplew.writeInt(chr.getMap().getLightCandles()); // candles with fire
                break;
            case 2: // clearCandle
                break;
            case 3: // updateMyDeathCount
                mplew.writeInt(chr.getDeathCounts().length); //All deathCount
                for (int i = 0; i < chr.getDeathCounts().length; ++i) {
                    mplew.writeInt(0); // unk
                    mplew.write(chr.getDeathCounts()[i]); // 0 : red, 1 : green, 2 : none
                }
                break;
            case 4: // makeSandGlass
                mplew.writeInt(chr == null ? 150000 : chr.getMap().getSandGlassTime() - System.currentTimeMillis()); // Duration
                mplew.writeInt(247); // skillId
                mplew.writeInt(1); // skillLv
                break;
            case 5: // clearSandGlass
                break;
            case 6: // spawnAlter
                mplew.writeInt(0); // x
                mplew.writeInt(266); // y
                mplew.writeInt(30);//chr.getMap().getReqTouched()); // reqTouched
                break;
            case 7: // updateAlter
                mplew.writeInt(30 - chr.getMap().getReqTouched());
                break;
            case 8: // removeAlter
                mplew.write(chr.getMap().getReqTouched() == 0); // isSuccess
                break;
            //There is no no.9
            case 10: // updateDeathCounts
                mplew.writeInt(chr.getId());
                mplew.writeInt(chr.liveCounts());
                break;
            case 11: // successAlter
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] showICBM(int id, int readInt, int readInt2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ICBM.getValue());

        mplew.writeInt(id);
        mplew.writeInt(readInt);
        mplew.writeInt(readInt2);

        return mplew.getPacket();
    }

    public static byte[] followEffect(int initiator, int replier, Point toMap) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_EFFECT.getValue());
        mplew.writeInt(initiator);
        mplew.writeInt(replier);
        if (replier == 0) { //cancel
            mplew.write(toMap == null ? 0 : 1); //1 -> x (int) y (int) to change map
            if (toMap != null) {
                mplew.writeInt(toMap.x);
                mplew.writeInt(toMap.y);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] moveFollow(Point otherStart, Point myStart, Point otherEnd, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_MOVE.getValue());
        mplew.writePos(otherStart);
        mplew.writePos(myStart);
        PacketHelper.serializeMovementList(mplew, moves);
        mplew.write(0x11); //what? could relate to movePlayer
        for (int i = 0; i < 8; i++) {
            mplew.write(0); //?? sometimes 0x44 sometimes 0x88 sometimes 0x4.. etc.. buffstat or what
        }
        mplew.write(0); //?
        mplew.writePos(otherEnd);
        mplew.writePos(otherStart);

        return mplew.getPacket();
    }

    public static byte[] getFollowMsg(int opcode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_MSG.getValue());
        mplew.writeLong(opcode); //5 = canceled request.

        return mplew.getPacket();
    }

    public static byte[] battleStatistics() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.BATTLE_STATISTICS.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] createAtom(MapleAtom atom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CREATE_FORCE_ATOM.getValue());
        mplew.write(atom.isByMob());

        if (atom.isByMob()) {
            mplew.writeInt(atom.getDwUserOwner());
        }

        mplew.writeInt(atom.getDwTargetId());
        mplew.writeInt(atom.getnForceAtomType());

        if (atom.getnForceAtomType() != 36 && atom.getnForceAtomType() != 37) {
            switch (atom.getnForceAtomType()) {
                case 0:
                case 9:
                case 14:
                case 29:
                case 42:
                    if (atom.getnForceAtomType() == 29 || atom.getnForceAtomType() == 42) {
                        mplew.writeInt(atom.getnSkillId());
                        if (atom.getnSkillId() == 400021069) {
                            mplew.writeInt(0);
                        }
                    }
                    break;
                default:
                    mplew.write(atom.isToMob());
                    switch (atom.getnForceAtomType()) {
                        case 2:
                        case 3:
                        case 6:
                        case 7:
                        case 11:
                        case 12:
                        case 13:
                        case 17:
                        case 19:
                        case 20:
                        case 23:
                        case 24:
                        case 25:
                        case 27:
                        case 28:
                        case 30:
                        case 32:
                        case 34:
                        case 38:
                        case 39:
                        case 40:
                        case 41:
                        case 47:
                        case 48:
                        case 49:
                        case 52:
                        case 53:
                        case 54:
                        case 55:
                        case 56:
                        case 57:
                        case 58:
                        case 60:
                        case 64:
                        case 65:
                        case 67:
                        case 72:
                            mplew.writeInt(atom.getDwTargets().size());
                            for (Integer dwTarget : atom.getDwTargets()) {
                                mplew.writeInt(dwTarget);
                            }
                            break;
                        default:
                            if (atom.getnForceAtomType() == 62) {
                                mplew.writeInt(atom.getDwTargets().size());
                                for (Integer dwTarget : atom.getDwTargets()) {
                                    mplew.writeInt(dwTarget);
                                }
                            } else {
                                mplew.writeInt(atom.getDwFirstTargetId());
                            }
                            break;
                    }
            }
        }

        if (atom.getnForceAtomType() != 29 && atom.getnForceAtomType() != 42) {
            mplew.writeInt(atom.getnSkillId());
        }

        for (ForceAtom forceAtom : atom.getForceAtoms()) {
            mplew.write(true);
            mplew.writeInt(forceAtom.getnAttackCount());
            mplew.writeInt(forceAtom.getnInc());
            mplew.writeInt(forceAtom.getnFirstImpact());
            mplew.writeInt(forceAtom.getnSecondImpact());
            mplew.writeInt(forceAtom.getnAngle());
            mplew.writeInt(forceAtom.getnStartDelay());
            mplew.writeInt(forceAtom.getnStartX());
            mplew.writeInt(forceAtom.getnStartY());
            mplew.writeInt(forceAtom.getDwCreateTime());
            mplew.writeInt(forceAtom.getnMaxHitCount());
            mplew.writeInt(forceAtom.getnEffectIdx());
            mplew.writeInt(0);
        }

        mplew.write(false);

        switch (atom.getnForceAtomType()) {
            case 24:
                return mplew.getPacket();
            case 11:
                mplew.writeInt(atom.getnForcedTargetX() - 240);
                mplew.writeInt(atom.getnForcedTargetY() - 120);
                mplew.writeInt(atom.getnForcedTargetX() + 240);
                mplew.writeInt(atom.getnForcedTargetY() + 120);
                mplew.writeInt(atom.getnItemId());
                return mplew.getPacket();
            case 9:
                mplew.writeInt((int) -atom.getnForcedTargetX());
                mplew.writeInt((int) -atom.getnForcedTargetY());
                mplew.writeInt((int) atom.getnForcedTargetX());
                mplew.writeInt((int) atom.getnForcedTargetY());
                return mplew.getPacket();
            case 15:
                mplew.writeInt(atom.getnForcedTargetX() - 10);
                mplew.writeInt(atom.getnForcedTargetY() - 10);
                mplew.writeInt(atom.getnForcedTargetX() + 10);
                mplew.writeInt(atom.getnForcedTargetY() + 10);
                mplew.write(false);
                return mplew.getPacket();
            case 29:
                mplew.writeInt(atom.getnForcedTargetX() - 100);
                mplew.writeInt(atom.getnForcedTargetY() - 100);
                mplew.writeInt(atom.getnForcedTargetX() + 100);
                mplew.writeInt(atom.getnForcedTargetY() + 100);
                mplew.writeInt(atom.getnForcedTargetX());
                mplew.writeInt(atom.getnForcedTargetY());
                return mplew.getPacket();
        }

        switch (atom.getnForceAtomType()) {
            case 16:
            case 4:
            case 20:
            case 26:
            case 30:
            case 33:
            case 61:
            case 64:
                mplew.writeInt(atom.getnForcedTargetX());
                mplew.writeInt(atom.getnForcedTargetY());
                mplew.writeInt(0);
        }

        switch (atom.getnForceAtomType()) {
            case 17:
                mplew.writeInt(atom.getnArriveDir());
                mplew.writeInt(atom.getnArriveRange());
                return mplew.getPacket();
            case 18:
                mplew.writeInt(atom.getnForcedTargetX());
                mplew.writeInt(atom.getnForcedTargetY());
                return mplew.getPacket();
            case 27:
                mplew.writeInt((int) -atom.getnForcedTargetX());
                mplew.writeInt((int) -atom.getnForcedTargetY());
                mplew.writeInt((int) atom.getnForcedTargetX());
                mplew.writeInt((int) atom.getnForcedTargetY());
                mplew.writeInt(0);
                return mplew.getPacket();
            case 28:
            case 34:
                mplew.writeInt((int) -atom.getnForcedTargetX());
                mplew.writeInt((int) -atom.getnForcedTargetY());
                mplew.writeInt((int) atom.getnForcedTargetX());
                mplew.writeInt((int) atom.getnForcedTargetY());
                mplew.writeInt(20);
                return mplew.getPacket();
            case 57:
            case 58:
                mplew.writeInt((int) atom.getnForcedTargetX() - 50);
                mplew.writeInt((int) atom.getnForcedTargetY() - 50);
                mplew.writeInt((int) atom.getnForcedTargetX() + 50);
                mplew.writeInt((int) atom.getnForcedTargetY() + 50);
                mplew.writeInt(2);
                mplew.writeInt(atom.getnForcedTargetX());
                mplew.writeInt(atom.getnForcedTargetY());
                return mplew.getPacket();
            case 36:
            case 39:
                mplew.writeInt(5);
                mplew.writeInt(550);
                mplew.writeInt(3);
                mplew.writeInt(-300);
                mplew.writeInt(-300);
                mplew.writeInt(300);
                mplew.writeInt(300);
                if (atom.getnForceAtomType() == 36) {
                    mplew.writeInt(-50);
                    mplew.writeInt(-50);
                    mplew.writeInt(50);
                    mplew.writeInt(50);
                    mplew.writeInt(atom.getDwUnknownPoint());
                }
                return mplew.getPacket();
            case 37:
                mplew.writeInt(0);
                mplew.writeInt(-300);
                mplew.writeInt(-300);
                mplew.writeInt(300);
                mplew.writeInt(300);
                mplew.writeInt(200);
                mplew.writeInt(atom.getDwUnknownPoint());
                return mplew.getPacket();
            case 42:
                mplew.writeInt(atom.getnForcedTargetX() - 240);
                mplew.writeInt(atom.getnForcedTargetY() - 120);
                mplew.writeInt(atom.getnForcedTargetX() + 240);
                mplew.writeInt(atom.getnForcedTargetY() + 120);
                return mplew.getPacket();
            case 49:
                mplew.writeInt(atom.getnItemId());
                mplew.writeInt(atom.getDwSummonObjectId());
                mplew.writeInt(atom.getnForcedTargetX() - 50);
                mplew.writeInt(atom.getnForcedTargetY() - 100);
                mplew.writeInt(atom.getnForcedTargetX() + 50);
                mplew.writeInt(atom.getnForcedTargetY() + 100);
                return mplew.getPacket();
            case 50:
                mplew.writeInt(atom.getnForcedTargetX());
                mplew.writeInt(atom.getnForcedTargetY());
                return mplew.getPacket();
            case 67:
                mplew.writeInt(atom.getnForcedTargetX());
                mplew.writeInt(atom.getnForcedTargetY());
                mplew.writeInt(0x8000);
                mplew.write(4);
                return mplew.getPacket();
        }

        if ( atom.getnSkillId() > 400011131 )
        {
            if ( atom.getnSkillId() == 400041023 )
            {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        } else if (atom.getnSkillId() == 25100010 || atom.getnSkillId() == 25120115) {
            mplew.writeInt(25001002);
        } else if (atom.getnSkillId() == 400011131) {
            mplew.writeInt(0);
            mplew.write(0);
        }

        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] onUIEventInfo(int objectId, long finishDate, long startDate, int maxDate, String info, List<Triple<Integer, Integer, Integer>> items, int windowId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UI_EVENT_INFO.getValue());

        /* mplew.writeInt(objectId);
        mplew.writeShort(1);
        //sub_8F1C50
        mplew.writeLong(PacketHelper.getTime(finishDate));
        mplew.writeLong(PacketHelper.getTime(startDate));
        mplew.writeInt(maxDate);
        mplew.writeInt(objectId);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(info);
        mplew.writeMapleAsciiString("");
        mplew.writeInt(0); // size1

        mplew.writeInt(items.size()); // size2

        for (Triple<Integer, Integer, Integer> item : items) {
            mplew.writeInt(item.left);
            mplew.writeInt(item.mid);

            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);

            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);
        }

        mplew.writeInt(0); // size3

        mplew.writeInt(windowId);*/
        mplew.writeInt(objectId);

        mplew.writeLong(PacketHelper.getTime(finishDate));
        mplew.writeLong(PacketHelper.getTime(startDate));

        mplew.writeInt(maxDate);
        mplew.writeInt(objectId);
        mplew.writeInt(0);
        mplew.write(0);

        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        mplew.writeMapleAsciiString(info);
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString("");

        mplew.writeInt(0); // size1
        mplew.writeInt(items.size()); // size2

        for (Triple<Integer, Integer, Integer> item : items) {
            mplew.writeInt(item.left);
            mplew.writeInt(item.mid);

            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);

            mplew.writeInt(0);
            mplew.write(0);

            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.write(0);
        }

        mplew.writeInt(0); // size3
        mplew.writeInt(windowId);
        return mplew.getPacket();
    }

    public static byte[] onUIEventSet(int objectId, int windowId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UI_EVENT_SET.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(windowId);
        return mplew.getPacket();
    }

    public static byte[] portalTeleport(String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PORTAL_TELEPORT.getValue());
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] spawnAdelProjectiles(MapleCharacter chr, List<AdelProjectile> tiles, boolean infinity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_ADEL_PROJECTILES.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(tiles.size());

        int i = 0;
        for (AdelProjectile tile : tiles) {
            mplew.writeInt(tile.getObjectId());
            mplew.writeInt(tile.getSkillId() == 400021122 ? 8 : tile.getSkillId() == 63101104 || tile.getSkillId() == 162111005 ? 1 : 0); //342
            mplew.writeInt(tile.getProjectileType());
            mplew.writeInt(i);
            mplew.writeInt(tile.getOwnerId());
            mplew.writeInt(tile.getSkillId() == 400011047 ? chr.getId() : tile.getTargetId());
            mplew.writeInt(tile.getCreateDelay());
            mplew.writeInt(tile.getDelay());
            mplew.writeInt(tile.getSkillId() == 63101104 || tile.getSkillId() == 400031063 ? tile.getStartX() : 0);
            mplew.writeInt(tile.getSkillId());
            mplew.writeInt(0);
            mplew.writeInt(tile.getSkillId() == 162111002 ? 20 : tile.getSkillId() == 400051069 || tile.getSkillId() == 162101000 ? 1 : 0);
            mplew.writeInt(tile.getDuration());
            mplew.writeInt(tile.getStartX());
            mplew.writeInt(tile.getStartY());
            mplew.writeInt(tile.getSkillId() == 162101000 ? 0 : tile.getSkillId() == 400011047 ? 0x1 : 0x12b);
            mplew.writeInt(tile.getIdk2()); //+342
            mplew.writeInt(tile.getPoint().x);
            mplew.writeInt(tile.getPoint().y);
            mplew.write(tile.getSkillId() == 400011119 || tile.getSkillId() == 162101000);
            mplew.write(false);
            mplew.write(false);
            mplew.writeInt(tile.getPoints().size());
            for (int point : tile.getPoints()) {
                mplew.writeInt(point);
            }
            ++i;
        }

        mplew.writeInt(infinity ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] attackAdelProjectile(MapleCharacter chr, int objid, int attackCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ATTACK_ADEL_PROJECTILE.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(objid);
        mplew.writeInt(attackCount);
        return mplew.getPacket();
    }

    public static byte[] removeAdelProjectile(MapleCharacter chr, int objectId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_ADEL_PROJECTILE.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(1);
        mplew.writeInt(objectId);
        mplew.writeInt(0);
        mplew.writeInt(0); // 351 new
        return mplew.getPacket();
    }

    public static byte[] showUnionRaidHpUI(int mobid, long currenthp, long maxhp, int mobid2, long currenthp2, long maxhp2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNION_RAID_HP.getValue());
        mplew.writeInt(mobid);
        mplew.writeLong(currenthp);
        mplew.writeLong(maxhp);
        mplew.writeInt(mobid2);
        mplew.writeLong(currenthp2);
        mplew.writeLong(maxhp2);
        return mplew.getPacket();
    }

    public static byte[] setUnionRaidScore(long score) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNION_RAID_SCORE.getValue());
        mplew.writeLong(score);
        return mplew.getPacket();
    }

    public static byte[] setUnionRaidCoinNum(int qty, boolean set) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UNION_RAID_COIN.getValue());
        mplew.writeInt(qty);
        mplew.write(set); // if false -> add
        return mplew.getPacket();
    }

    public static byte[] showScrollOption(int itemId, int scrollId, StarForceStats es) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SCROLL_CHAT.getValue());
        mplew.writeInt(scrollId);
        mplew.writeInt(itemId);
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
        return mplew.getPacket();
    }

    public static class FarmPacket {

        public static byte[] onEnterFarm(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.ENTER_FARM.getValue());
            PacketHelper.addCharacterInfo(mplew, chr);

            int v13 = 0;
            while (v13 < 37500) {
                mplew.writeInt(v13 < 3000 ? 4150001 : 0); // tileId
                mplew.writeInt(0);
                mplew.write(0);
                mplew.writeInt(0);
                mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
                v13 += 60;
            }
            mplew.writeInt(14);
            mplew.writeInt(14);
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
            return mplew.getPacket();
        }

        public static byte[] onSetFarmUser(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.SET_FARM_USER.getValue());
            mplew.writeInt(chr.getClient().getAccID()); // farmId
            mplew.writeInt(0); // farmToday
            mplew.writeLong(0); // farmTotal
            PacketHelper.addFarmInfo(mplew, chr.getClient(), 0);
            farmUserGameInfo(mplew, false, chr);
            PacketHelper.addFarmInfo(mplew, chr.getClient(), 0);
            farmUserGameInfo(mplew, false, chr);

            //farmUsetInfoEx
            mplew.writeInt(0);

            //DecodeSocialConnectionInfo
            mplew.writeInt(-1);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static void farmUserGameInfo(MaplePacketLittleEndianWriter mplew, boolean unk, MapleCharacter chr) {
            mplew.write(unk);
            if (unk) {
                mplew.writeInt(chr.getClient().getWorld());
                mplew.writeMapleAsciiString(LoginServer.getServerName()); // worldName
                mplew.writeInt(chr.getId());
                mplew.writeMapleAsciiString(chr.getName());
            }
        }

        public static byte[] onFarmNotice(String str) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FARM_NOTICE.getValue());
            mplew.writeMapleAsciiString(str);
            return mplew.getPacket();
        }

        public static byte[] onFarmSetInGameInfo(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FARM_SET_INGAME_INFO.getValue());
            farmUserGameInfo(mplew, false, chr);
            return mplew.getPacket();
        }

        public static byte[] onFarmRequestSetInGameInfo(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FARM_REQ_SET_INGAME_INFO.getValue());
            farmUserGameInfo(mplew, false, chr);
            mplew.writeInt(chr.getClient().getWorld());
            mplew.writeMapleAsciiString(LoginServer.getServerName()); // worldName
            mplew.writeInt(chr.getId());
            mplew.writeMapleAsciiString(chr.getName());
            return mplew.getPacket();
        }

        public static byte[] onFarmImgUpdate(MapleClient c, int length, byte[] img) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.FARM_IMG_UPDATE.getValue());
            mplew.writeInt(c.getAccID());
            mplew.writeInt(length);
            mplew.write(img);
            return mplew.getPacket();
        }
    }

    public static byte[] getPhotoResult(MapleClient c, byte[] farmImg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PHOTO_RESULT.getValue());
        mplew.writeInt(c.getAccID());
        mplew.writeInt(farmImg.length);
        if (farmImg.length > 0) {
            mplew.write(farmImg);
        }
        return mplew.getPacket();
    }

    public static byte[] updateGuildScore(int guildScore) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_GUILD_SCORE.getValue());
        mplew.writeInt(guildScore);
        return mplew.getPacket();
    }

    public static byte[] updateShapeShift(int id, boolean use) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHAPE_SHIFT.getValue());
        mplew.writeInt(id);
        mplew.write(use);

        return mplew.getPacket();
    }

    public static byte[] CreateForceAtom(int fromId, int toId, List<ForceAtoms> atoms, int type, int skillid, Point pos) {
        return CreateForceAtom(fromId, toId, atoms, type, skillid, pos, 0, 0, 0, null, 0);
    }

    public static byte[] CreateForceAtom(int fromId, int toId, List<ForceAtoms> atoms, int type, int skillid, Point pos, int itemid, int dir, int range) {
        return CreateForceAtom(fromId, toId, atoms, type, skillid, pos, itemid, dir, range, null, 0);
    }

    public static byte[] CreateForceAtom(int fromId, int toId, List<ForceAtoms> atoms, int type, int skillid, Point pos, int itemid, int dir, int range, List<Integer> attackmobs, int objectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CREATE_FORCE_ATOM.getValue());
        mplew.write(fromId > 0);

        if (fromId > 0) {
            mplew.writeInt(fromId);
        }

        mplew.writeInt(toId);
        mplew.writeInt(type);

        if (type != 36 && type != 37) {
            switch (type) {
                case 0:
                case 9:
                case 14:
                case 29:
                case 42:
                    break;
                default:
                    mplew.write(skillid == 25100010 || skillid == 164120007 ? 0 : 1);
                    switch (type) {
                        case 2:
                        case 3:
                        case 6:
                        case 11:
                        case 12:
                        case 13:
                        case 17:
                        case 19:
                        case 20:
                        case 23:
                        case 24:
                        case 25:
                        case 27:
                        case 28:
                        case 30:
                        case 32:
                        case 34:
                        case 38:
                        case 39:
                        case 40:
                        case 41:
                        case 47:
                        case 48:
                        case 52:
                        case 53:
                        case 54:
                        case 55:
                        case 56:
                        case 60:
                        case 67:
                            mplew.writeInt(atoms.size());
                            for (ForceAtoms atom : atoms) {
                                mplew.writeInt(atom.getObjectId());
                            }
                            break;
                        case 7:
                        case 49:
                            mplew.writeInt(attackmobs.size());
                            for (Integer mobid : attackmobs) {
                                mplew.writeInt(mobid);
                            }
                            break;
                        case 61:
                            mplew.writeInt(0);
                            break;
                        case 63:
                            mplew.writeInt(fromId);
                            break;
                        default:
                            mplew.writeInt(atoms.get(0).getObjectId());
                            break;
                    }
            }
            if (skillid != 0) {
                mplew.writeInt(skillid);
            }
        }

        for (ForceAtoms atom : atoms) {
            mplew.write(1);
            mplew.writeInt(atom.getKey());
            mplew.writeInt(atom.getInc());
            mplew.writeInt(atom.getFirstimpact());
            mplew.writeInt(atom.getSecondimpact());
            mplew.writeInt(atom.getAngle());
            mplew.writeInt(atom.getDelay());
            mplew.writeInt(atom.getPos().x);
            mplew.writeInt(atom.getPos().y);
            mplew.writeInt(System.currentTimeMillis());
            mplew.writeInt(type == 0x11 ? 8 : type == 0x30 ? 8 : 0);
            mplew.writeInt(0);
            mplew.writeInt(skillid == 400011058 || skillid == 400011059 ? 2000 : 0);
        }

        mplew.write(0);

        switch (type) {
            case 11:
                mplew.writeInt((int) pos.getX() - 50);
                mplew.writeInt((int) pos.getY() - 100);
                mplew.writeInt((int) pos.getX() + 50);
                mplew.writeInt((int) pos.getY() + 100);
                mplew.writeInt(itemid);
                return mplew.getPacket();
            case 9:
                mplew.writeInt((int) -pos.getX());
                mplew.writeInt((int) -pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                return mplew.getPacket();
            case 15:
                mplew.writeInt((int) pos.getX() - 50);
                mplew.writeInt((int) pos.getY() - 100);
                mplew.writeInt((int) pos.getX() + 50);
                mplew.writeInt((int) pos.getY() + 100);
                mplew.write(0);
                return mplew.getPacket();
            case 29:
                mplew.writeInt((int) -pos.getX());
                mplew.writeInt((int) -pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                return mplew.getPacket();
            case 67:
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                mplew.write(0);
                mplew.writeInt(0);
                return mplew.getPacket();
            case 56:
                mplew.writeInt(0);
                return mplew.getPacket();
        }

        if (type == 16 || type == 4 || type == 26 || type == 33 || type == 61) {
            if (pos != null) {
                if (type == 4) {
                    mplew.writeInt((int) pos.getX());
                    mplew.writeInt((int) pos.getY());
                } else {
                    mplew.writeInt((int) pos.getX());
                    mplew.writeInt((int) pos.getY());
                }
            } else {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
            return mplew.getPacket();
        }

        switch (type) {
            case 20:
                mplew.writeLong(0);
                return mplew.getPacket();
            case 17:
                mplew.writeInt(dir);
                mplew.writeInt(range);
                return mplew.getPacket();
            case 18:
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                return mplew.getPacket();
            case 27:
                mplew.writeInt((int) -pos.getX());
                mplew.writeInt((int) -pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                mplew.writeInt(0);
                return mplew.getPacket();
            case 28:
                mplew.writeInt(-700);
                mplew.writeInt(-600);
                mplew.writeInt(700);
                mplew.writeInt(600);
                mplew.writeInt(20);
                return mplew.getPacket();
            case 34:
                mplew.writeInt((int) -pos.getX());
                mplew.writeInt((int) -pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                mplew.writeInt(0);
                return mplew.getPacket();
            case 36:
            case 39:
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt((int) -pos.getX());
                mplew.writeInt((int) -pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                if (type == 36) {
                    mplew.writeInt((int) -pos.getX());
                    mplew.writeInt((int) -pos.getY());
                    mplew.writeInt((int) pos.getX());
                    mplew.writeInt((int) pos.getY());
                    mplew.writeInt(0);
                }
                return mplew.getPacket();
            case 37:
                mplew.writeInt(0);
                mplew.writeInt((int) -pos.getX());
                mplew.writeInt((int) -pos.getY());
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                mplew.writeInt(0);
                mplew.writeInt(0);
                return mplew.getPacket();
            case 42:
                mplew.writeInt((int) pos.getX() - 50);
                mplew.writeInt((int) pos.getY() - 100);
                mplew.writeInt((int) pos.getX() + 50);
                mplew.writeInt((int) pos.getY() + 100);
                return mplew.getPacket();
        }

        if (type != 49) {
            if (type == 50) {
                mplew.writeInt((int) pos.getX());
                mplew.writeInt((int) pos.getY());
                return mplew.getPacket();
            } else {
                return mplew.getPacket();
            }
        }

        mplew.writeInt(itemid);
        mplew.writeInt(objectid);
        mplew.writeInt((int) pos.getX());
        mplew.writeInt((int) pos.getY());
        mplew.writeInt((int) pos.getX());
        mplew.writeInt((int) pos.getY());

        return mplew.getPacket();
    }

    public static byte[] TigerSpecialAttack(int cid, int oid, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TIGER_SPECIAL_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.writeInt(skillid);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] StartFullMaker(int count, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FullMaker.getValue());
        if (count == 0) {
            mplew.write(0);
            return mplew.getPacket();
        }
        mplew.write(true);
        mplew.writeInt(count);

        mplew.writeInt(duration);
        return mplew.getPacket();
    }

    public static byte[] TriangleDamage(MapleMonster monster) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.TRIANGLE_DAMAGE.getValue());
        mplew.writeInt(36110005);
        mplew.writePosInt(monster.getPosition());
        mplew.writeInt(monster.getObjectId());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] HommingRoket(int charid, byte use, int skill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(663);
        mplew.writeInt(charid);
        mplew.write(use);
        mplew.writeInt(skill);
        return mplew.getPacket();
    }

    public static byte[] KainStackSkill(int skillid, int unk1, int unk2, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KainStackSkill_S.getValue());
        mplew.writeInt(skillid);
        mplew.writeInt(unk1);//stack
        mplew.writeInt(unk2);//maxstack
        mplew.writeInt(time);//reloadtime
        return mplew.getPacket();
    }

    public static byte[] setStacktoMonster(boolean isStack, List<Triple<MapleMonster, Integer, Integer>> send, int stackDuration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KainStackToMonster.getValue());
        mplew.write(isStack);
        if (isStack) {
            int a = send.size();
            mplew.writeInt(send.size());
            for (int i = 0; i < a; i++) {
                mplew.writeInt(send.get(i).getLeft().getObjectId());
            }
            mplew.writeInt(a);
            for (int i = 0; i < a; i++) {
                mplew.writeInt(send.get(i).getLeft().getObjectId());
                mplew.writeInt(send.get(i).getMid());
                mplew.writeInt(send.get(i).getRight());
                mplew.writeInt(stackDuration);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] KainDeathBlessAttack(int skillid, List<MapleMonster> m) {  //파티원들 공격도 이걸로 하지않을까싶음
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KainDeathBlessing.getValue());
        mplew.writeInt(skillid);
        mplew.writeInt(0);
        mplew.writeInt(m.size());
        for (MapleMonster m2 : m) {
            mplew.writeInt(m2.getObjectId());
            mplew.writeInt(1);//??
            mplew.writeInt(0);//??
        }
        return mplew.getPacket();
    }

    public static byte[] KainRemainInsenceAttack(MapleCharacter chr, List<MapleMagicWreck> mw) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KainRemainInsence.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(mw.size());
        for (MapleMagicWreck mw2 : mw) {
            mplew.writeInt(mw2.getObjectId());
            mplew.writeInt((int) mw2.getTruePosition().getX());//??
            mplew.writeInt((int) mw2.getTruePosition().getY());//??
        }
        return mplew.getPacket();
    }

    public static byte[] UpdateSeed(int skillId, byte value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.UPDATE_SEED.getValue());
        mplew.writeInt(skillId);
        mplew.write(value);
        return mplew.getPacket();
    }

    public static byte[] UseSummonSkill(int skillId, List<Integer> skillList) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.USE_SUMMON_SKILL.getValue());
        mplew.writeInt(skillId);
        mplew.writeInt(skillList.size());
        for (Integer skill : skillList) {
            mplew.writeInt(skill);
        }
        return mplew.getPacket();
    }

    public static byte[] ShowEventSkillEffect(int skillId, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.EVENT_SKILL_EFFECT.getValue());
        mplew.writeInt(skillId);
        mplew.writeInt(duration);
        return mplew.getPacket();
    }

    public static byte[] ShowMSClock(int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MS_CLOCK.getValue());
        mplew.writeInt(duration);
        return mplew.getPacket();
    }

    public static byte[] ClearClock() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLEAR_CLOCK.getValue());
        return mplew.getPacket();
    }

    public static byte[] ShowDetailShowInfo(int color, int width, int height, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DETAIL_SHOW_INFO.getValue());
        mplew.writeInt(color);
        mplew.writeInt(width);
        mplew.writeInt(height);
        mplew.writeInt(0);
        mplew.write(false);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] thunderAttack(int x, int y, int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.THUNDER_ATTACK.getValue());
        mplew.writeInt(80001762);
        mplew.writeInt(1);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }


    public static byte[] CreateJupiterThunder(MapleCharacter chr, int skillid, Point pos, int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(695);
        mplew.writeInt(chr.getId());
        mplew.writeInt(1);

        mplew.write(1);
        mplew.writeInt(1);
        mplew.writeInt(Randomizer.rand(2, 500000));
        mplew.writeInt(chr.getId());
        mplew.writePosInt(pos);
        mplew.writeInt(chr.isFacingLeft() ? -18 : 18);
        mplew.writeInt(args[1]);
        mplew.writeInt(skillid);
        mplew.writeInt(args[2]);
        mplew.writeInt(args[3]);
        mplew.writeInt(args[4]);
        mplew.writeInt(args[5]);
        mplew.writeInt(args[6]);
        mplew.writeInt(args[7]);
        return mplew.getPacket();
    }

    public static byte[] removeOrb(int ownerId, int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(696);
        mplew.writeInt(ownerId);

        mplew.writeInt(1); // size
        mplew.writeInt(oid);

        return mplew.getPacket();

    }

    public static byte[] MoveJupiterThunder(MapleCharacter chr, int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(697);
        mplew.writeInt(chr.getId());
        mplew.writeInt(args[0]);
        mplew.writeInt(args[1]);
        mplew.writeInt(args[2]);
        mplew.writeInt(args[3]);
        mplew.writeInt(args[4]);
        mplew.writeInt(args[5]);
        mplew.writeInt(args[6]);
        return mplew.getPacket();
    }
}
