/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.*;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import client.messages.commands.InternCommand.밴;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.handler.AutoRegister;
import handling.world.World;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.NPCScriptManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import server.shops.MapleShopFactory;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.MobPacket;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import server.Randomizer;
import server.polofritto.MapleRandomPortal;

/**
 *
 * @author Emilyx3
 */
public class SuperGMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }

    public static class 영구밴 extends 밴 {

        public 영구밴() {
            hellban = true;
        }
    }

    public static class 영구밴해제 extends 밴해제 {

        public 영구밴해제() {
            hellban = true;
        }
    }

    public static class 밴해제 extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            if (hellban) {
                return "영구밴해제";
            } else {
                return "밴해제";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !" + getCommand() + " <IGN>");
                return 0;
            }
            byte ret;
            if (hellban) {
                ret = MapleClient.unHellban(splitted[1]);
            } else {
                ret = MapleClient.unban(splitted[1]);
            }
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] SQL error.");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] The character does not exist.");
                return 0;
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] Successfully unbanned!");

            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }

    public static class 아이피밴해제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[Syntax] !unbanip <IGN>");
                return 0;
            }
            byte ret = MapleClient.unbanIPMacs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
            } else if (ret == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            if (ret > 0) {
                return 1;
            }
            return 0;
        }
    }

    public static class 관리자권한부여 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                MapleCharacter player = null;
                player = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (player != null) {
                    byte number = Byte.parseByte(splitted[2]);
                    player.setGMLevel(number);
                    player.dropMessage(5, "[알림] " + splitted[1] + " 플레이어가 GM레벨 " + splitted[2] + " (으)로 설정되었습니다.");
                }
                c.getPlayer().dropMessage(5, "[알림] " + splitted[1] + " 플레이어가 GM레벨 " + splitted[2] + " (으)로 설정되었습니다.");
            }
            return 1;
        }
    }

    public static class 캐릭터좌표이동 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                MapleCharacter player = null;
                player = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if (player != null) {
                    player.getClient().getSession().writeAndFlush(SLFCGPacket.CharReLocationPacket(Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3])));
                }
            }
            return 1;
        }
    }

    public static class 스킬초기화 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().skillReset();
            return 1;
        }
    }

    public static class 키벨류조작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 5) {
                c.getPlayer().dropMessage(6, "!키벨류조작 <닉네임> <번호> <키> <벨류>");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "같은 채널에 없는듯?");
            } else {
                int t = Integer.parseInt(splitted[2]);
                String key = splitted[3];
                String value = splitted[4];
                chrs.setKeyValue(t, key, value);
            }
            return 1;
        }
    }

    public static class 계정키벨류조작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 5) {
                c.getPlayer().dropMessage(6, "!계정키벨류조작 <닉네임> <키> <벨류>");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "같은 채널에 없는듯?");
            } else {
                String key = splitted[2];
                String value = splitted[3];
                chrs.getClient().setKeyValue(key, value);
            }
            return 1;
        }
    }

    public static class 모두저장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "", "게임 데이터 저장을 시작합니다. 잠시 렉이 걸려도 나가지 말아주세요."));
            int saved = 0;

            if (splitted[1] == "-1") {
                for (ChannelServer cs : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters().values()) {
                        new MapleCharacterSave(chr).saveToDB(chr, false, false);
                        chr.dropMessage(5, "저장되었습니다.");
                        saved++;
                    }
                }
            } else {
                ChannelServer ch = ChannelServer.getInstance(Integer.parseInt(splitted[1]));
                if (ch != null) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters().values()) {
                        new MapleCharacterSave(chr).saveToDB(chr, false, false);
                        chr.dropMessage(5, "저장되었습니다.");
                        saved++;
                    }
                } else {
                    c.getPlayer().dropMessageGM(6, "존재하지 않는 채널입니다.");
                }
            }

            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "", "총  " + saved + "명의 데이터가 저장되었습니다."));
            return 1;
        }
    }

    public static class 맥스스킬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleData skill_ : MapleDataProviderFactory.getDataProvider(new File("wz/String.wz")).getData("Skill.img").getChildren()) {
                try {
                    Skill skill = SkillFactory.getSkill(Integer.parseInt(skill_.getName()));
                    if ((skill.getId() < 1009 || skill.getId() > 1011));
                    c.getPlayer().changeSkillLevel(skill, (byte) skill.getMaxLevel(), (byte) skill.getMaxLevel());
                } catch (NumberFormatException nfe) {
                    break;
                } catch (NullPointerException npe) {
                    continue;
                }
            }
            return 1;
        }
    }

    public static class 스킬주기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            victim.changeSingleSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    public static class 인벤잠금해제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Item, MapleInventoryType> eqs = new HashMap<Item, MapleInventoryType>();
            boolean add = false;
            if (splitted.length < 2 || splitted[1].equals("모두")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        if (ItemFlag.LOCK.check(item.getFlag())) {
                            item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                            add = true;
                            //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                        }
                        if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                            item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                            add = true;
                            //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                        }
                        if (add) {
                            eqs.put(item, type);
                        }
                        add = false;
                    }
                }
            } else if (splitted[1].equals("장착장비")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("장비")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("소비")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.USE);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("설치")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.SETUP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("기타")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.ETC);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("캐시")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.CASH);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("치장")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.DECORATION)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().writeAndFlush(CField.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.DECORATION);
                    }
                    add = false;
                }
            } else {
                c.getPlayer().dropMessage(6, "[모두/장착장비/장비/소비/설치/기타/캐시/치장]");
            }

            for (Entry<Item, MapleInventoryType> eq : eqs.entrySet()) {
                c.getPlayer().forceReAddItem_NoUpdate(eq.getKey().copy(), eq.getValue());
            }
            return 1;
        }
    }

    public static class 드롭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {

                    toDrop = ii.getEquipById(itemId);
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                }
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setGMLog(c.getPlayer().getName() + " used !drop");
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
            return 1;
        }
    }

    public static class 결혼 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[2]);
            if (!GameConstants.isEffectRing(itemId)) {
                c.getPlayer().dropMessage(6, "Invalid itemID.");
            } else {
                MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (fff == null) {
                    c.getPlayer().dropMessage(6, "Player must be online");
                } else {
                    int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                    MapleCharacter[] chrz = {fff, c.getPlayer()};
                    for (int i = 0; i < chrz.length; i++) {
                        Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId, ringID[i]);
                        if (eq == null) {
                            c.getPlayer().dropMessage(6, "Invalid itemID.");
                            return 0;
                        }
                        MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                        chrz[i].dropMessage(6, "Successfully married with " + chrz[i == 0 ? 1 : 0].getName());
                    }
                    MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                }
            }
            return 1;
        }
    }

    public static class 포인트주기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need playername and amount.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                chrs.setPoints(chrs.getPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getPoints() + " points, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class 후원포인트지급 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need playername and amount.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                chrs.gainDonationPoint(Integer.parseInt(splitted[2]));
                //chrs.gainnDonationPoint(Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getDonationPoint() + " Dpoints, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class 스타포인트지급 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need playername and amount.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                chrs.AddStarDustCoin(Integer.parseInt(splitted[2]));
                //chrs.gainnDonationPoint(Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, "ok");
            }
            return 1;
        }
    }

    public static class 홍보포인트지급 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need playername and amount.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                chrs.gainHPoint(Integer.parseInt(splitted[2]));
                //chrs.gainnHPoint(Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getHPoint() + " Hpoints, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class V포인트주기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need playername and amount.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                chrs.setVPoints(chrs.getVPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getVPoints() + " vpoints, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class ResetOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forfeit(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]));
            return 1;
        }
    }

    public static class FStartOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]), splitted.length > 4 ? splitted[4] : null);
            return 1;
        }
    }

    public static class FCompleteOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class 리엑터스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(splitted[1])), Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            c.getPlayer().getMap().spawnReactorOnGroundBelow(reactor, new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y - 20));
            return 1;
        }
    }

    public static class 현재맵엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            c.getPlayer().dropMessage(6, "현재 맵에 있는 엔피시 리스트입니다.");
            for (MapleMapObject mo : map.getAllNPCs()) {
                c.getPlayer().dropMessage(6, ((MapleNPC) mo).getId() + " : " + ((MapleNPC) mo).getName());
            }
            return 1;
        }
    }

    public static class 몬스터데미지OID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            int damage = Integer.parseInt(splitted[2]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                monster.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class 올몹데미지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "Map does not exist");
                return 0;
            }
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                mob.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class 몹데미지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[2])) {
                    map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                    mob.damage(c.getPlayer(), damage, false);
                }
            }
            return 1;
        }
    }

    public static class 현재맵 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            c.getPlayer().dropMessage(6, "현재 맵 : " + map.getId() + " - " + map.getStreetName() + " : " + map.getMapName());
            return 1;
        }
    }

    public static class 몬스터소환개체 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<MapleMonster> map = c.getPlayer().getMap().getAllMonster();
            MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/" + "String.wz"));

            MapleData data = dataProvider.getData("Mob.img");
            List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
            for (MapleData mobIdData : data.getChildren()) {
                mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
            }
            c.getPlayer().dropMessage(6, "현재 맵에 있는 몬스터 입니다.");
            for (int i = 0; i < map.size(); i++) {
                Pair<Integer, String> mob = null;
                for (Pair<Integer, String> mobPair : mobPairList) {
                    if (mobPair.getLeft() == map.get(i).getId()) {
                        mob = mobPair;
                    }
                }
                if (mob != null) {
                    c.getPlayer().dropMessage(6, (i + 1) + ". " + mob.getRight() + "(" + map.get(i).getId() + ") {hp = " + map.get(i).getHp() + "}");
                } else {
                    c.getPlayer().dropMessage(6, (i + 1) + ". " + "이름없음" + "(" + map.get(i).getId() + ") {hp = " + map.get(i).getHPPercent() + "/100}");
                }
            }
            return 1;
        }
    }

    public static class 킬몹 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "맵이 존재하지 않습니다.");
                return 0;
            }
            MapleMonster mob;

            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                mob.damage(c.getPlayer(), mob.getMobMaxHp() / 5, false);
//                map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
            }
            return 1;
        }
    }

    public static class 엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId); //1501012
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "WZ데이터에 없는 엔피시코드를 입력하였습니다.");
                return 0;
            }
            return 1;
        }
    }

    public static class 고정엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "WZ에 존재하지 않는 NPC를 입력했습니다.");
            }
            Connection con = null;
            PreparedStatement ps = null;
            try {
                String sql = "INSERT INTO `spawn`(`lifeid`, `rx0`, `rx1`, `cy`, `fh`, `type`, `dir`, `mapid`, `mobTime`) VALUES (? ,? ,? ,? ,? ,? ,? ,? ,?)";
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement(sql);
                ps.setInt(1, npcId);
                ps.setInt(2, c.getPlayer().getPosition().x - 50);
                ps.setInt(3, c.getPlayer().getPosition().x + 50);
                ps.setInt(4, c.getPlayer().getPosition().y);
                ps.setInt(5, c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                ps.setString(6, "n");
                ps.setInt(7, c.getPlayer().getFacingDirection() == 1 ? 0 : 1);
                ps.setInt(8, c.getPlayer().getMapId());
                ps.setInt(9, 0);
                ps.executeUpdate();
                ps.close();
                con.close();
            } catch (Exception e) {
                System.err.println("[오류] 엔피시를 고정 등록하는데 실패했습니다.");
                e.printStackTrace();
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
            return 1;
        }
    }

    public static class 고정몹 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Connection con = null;
            PreparedStatement ps = null;
            int mobId = Integer.parseInt(splitted[1]);
            MapleMonster mob = MapleLifeFactory.getMonster(mobId);
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            try {
                String sql = "INSERT INTO `spawn`(`lifeid`, `rx0`, `rx1`, `cy`, `fh`, `type`, `dir`, `mapid`, `mobTime`) VALUES (? ,? ,? ,? ,? ,? ,? ,? ,?)";
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement(sql);
                ps.setInt(1, mobId);
                ps.setInt(2, c.getPlayer().getPosition().x - 50);
                ps.setInt(3, c.getPlayer().getPosition().x + 50);
                ps.setInt(4, c.getPlayer().getPosition().y);
                ps.setInt(5, c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                ps.setString(6, "m");
                ps.setInt(7, c.getPlayer().getFacingDirection() == 1 ? 0 : 1);
                ps.setInt(8, c.getPlayer().getMapId());
                ps.setInt(9, 0);
                ps.executeUpdate();
                ps.close();
            } catch (Exception e) {
                System.err.println("[오류] 몬스터를 고정 등록하는데 실패했습니다.");
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (Exception e) {
                    }
                }
            }
            return 1;
        }
    }

    public static class 플레이어엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "플레이어엔피시를 제작중입니다.");
                MapleCharacter chhr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + "님은 온라인이 아니거나, 존재하지 않는 닉네임입니다. 상태를 확인하고 다시 시도해주세요.");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "완료!");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "플레이어엔피시를 제작하는데 실패하였습니다. : " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static class 오프플레이어엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "오프 플레이어엔피시를 제작중입니다.");
                MapleClient cs = new MapleClient(c.getSession(), null, null);
                MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(splitted[1]), cs, false);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + "님이 존재하지 않습니다.");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "완료!");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "오프 플레이어엔피시를 제작하는데 실패하였습니다. : " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static class 플레이어엔피시삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "플레이어엔피시가 삭제중 입니다.");
                final MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                if (npc instanceof PlayerNPC) {
                    ((PlayerNPC) npc).destroy(true);
                    c.getPlayer().dropMessage(6, "완료!");
                } else {
                    c.getPlayer().dropMessage(6, "사용법 : !플레이어엔피시삭제 [엔피시코드]");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "엔피시를 삭제하는데 실패하였습니다. : " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static class 서버메세지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(outputMessage);
            }
            return 1;
        }
    }

    public static class 스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mid = Integer.parseInt(splitted[1]);
            final int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 500);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Long exp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "Mob does not exist");
                return 0;
            }

            long newhp = 0;
            long newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.longValue();
            } else if (pexp != null) {
                newexp = (long) (onemob.getMobExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                if (level != null) {
                    mob.changeLevel(level.intValue(), false);
                } else {
                    mob.setOverrideStats(overrideStats);
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
                c.getPlayer().dropMessage(6, "oid : " + mob.getObjectId());
            }
            return 1;
        }
    }

    public static class 패킷 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                c.getSession().writeAndFlush(CField.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
            } else {
                /*for (MapleCharacter chr : c.getPlayer().getMap().getAllChracater()) {
                 List<MapleCharacter> temp = chr.getMap().getAllChracater();
                 temp.remove(chr);
                 chr.getClient().getSession().writeAndFlush(SLFCGPacket.MiniGameWindow(17, chr, temp));
                 MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                 mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
                 mplew.write(0x77);
                 mplew.write(0);
                 mplew.write(chr == c.getPlayer() ? 2 : 0x0E);
                 mplew.writeInt(10);
                 chr.getClient().getSession().writeAndFlush(mplew.getPacket());
                 }*/
 /* MapleCharacter other = c.getPlayer().getMap().getCharacterByName("패팬텀");
                 MapleCharacter Me = c.getPlayer().getMap().getCharacterByName("세쿠스");
                 for (MapleCharacter chr : c.getPlayer().getMap().getAllChracater()) {
                 chr.getClient().getSession().writeAndFlush(MultiOthelloGamePacket.CreateUI(Me, other, chr));
                 chr.getClient().getSession().writeAndFlush(MultiOthelloGamePacket.OnInit(new ArrayList<BattleReverseStone>()));
                 }*/
                //c.getPlayer().setWatchingWeb(true);
                //c.getSession().writeAndFlush(SLFCGPacket.ChangeVolume(0, 2000));
                //c.getSession().writeAndFlush(SLFCGPacket.ShowWeb("https://www.youtube.com/embed/yUpl_HQrBnM?vq=highres&autoplay=1&controls=0&showinfo=0&autohide=1&iv_load_policy=3&rel=0&theme=dark&disablekb=1&fs=1&modestbranding=1&cc_load_policy=0&loop=1"));

                MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
                /*
                 mplew.writeShort(565);
                 mplew.write(0x11);
                 mplew.write(1);
                 mplew.writeInt(1);
                 mplew.writeInt(1);
                 mplew.writeInt(1);
                 mplew.writeInt(1);
                 mplew.writeInt(1);
                 mplew.writeInt(993001000);
                 */
                c.getSession().writeAndFlush(mplew.getPacket());

                //MapleRandomPortal portal = new MapleRandomPortal(2, c.getPlayer().getTruePosition(), c.getPlayer().getMapId(), c.getPlayer().getId(), Randomizer.nextBoolean());
                //c.getPlayer().getMap().spawnRandomPortal(portal);
            }
            return 1;
        }
    }

    public static class 맵 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap target = null;
            ChannelServer cserv = c.getChannelServer();
            target = cserv.getMapFactory().getMap(Integer.parseInt(splitted[1]));
            MaplePortal targetPortal = null;
            if (splitted.length > 2) {
                try {
                    targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                } catch (IndexOutOfBoundsException e) {
                    // noop, assume the gm didn't know how many portals there are
                    c.getPlayer().dropMessage(5, "Invalid portal selected.");
                } catch (NumberFormatException a) {
                    // noop, assume that the gm is drunk
                }
            }
            if (targetPortal == null) {
                targetPortal = target.getPortal(0);
            }
            c.getPlayer().changeMap(target, targetPortal);
            return 1;
        }
    }

    public static class 맵리로딩 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mapId = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                    c.getPlayer().dropMessage(5, "There exists characters on channel " + cserv.getChannel());
                    return 0;
                }
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)) {
                    cserv.getMapFactory().removeMap(mapId);
                }
            }
            return 1;
        }
    }

    public static class 리스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().respawn(true);
            return 1;
        }
    }

    public static class 맥스메소 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso((long) (9999999999L - c.getPlayer().getMeso()), true);
            return 1;
        }
    }

    public static class 메소 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(Long.parseLong(splitted[1]), true);
            return 1;
        }
    }

    public static class 캐시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Need amount.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 메이플포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Need amount.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Need amount.");
                return 0;
            }
            c.getPlayer().setPoints(c.getPlayer().getPoints() + Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class V포인트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Need amount.");
                return 0;
            }
            c.getPlayer().setVPoints(c.getPlayer().getVPoints() + Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class 옵코드리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            return 1;
        }
    }

    public static class 드롭리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            return 1;
        }
    }

    public static class 포탈리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            return 1;
        }
    }

    public static class 엔피시리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().scriptClear();
            return 1;
        }
    }

    public static class 상점리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            return 1;
        }
    }

    public static class 이벤트리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "이벤트 리셋 시작");
//            for (ChannelServer instance : ChannelServer.getAllInstances()) {
            c.getChannelServer().reloadEvents();
            //          }
            c.getPlayer().dropMessage(6, "이벤트 리셋 종료");
            return 1;
        }
    }

    public static class 맵리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            return 1;
        }
    }

    public static class 퀘스트리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            return 1;
        }
    }

    public static class 퀘스트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class 퀘스트완료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class F퀘스트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return 1;
        }
    }

    public static class F퀘스트완료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class 리엑터데미지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
            return 1;
        }
    }

    public static class 계정생성 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String id = splitted[1];
            String pw = splitted[2];
            if (AutoRegister.getAccountExists(id) == true) {
                c.getPlayer().dropMessage(6, "이미 존재하는 계정입니다.");
            } else {
                if (AutoRegister.createAccount(id, pw, "/" + id + ":")) {
                    c.getPlayer().dropMessage(6, "계정생성에 성공하였습니다.");
                } else {
                    c.getPlayer().dropMessage(6, "계정생성에 실패하였습니다.");
                }
            }
            return 1;
        }
    }

    public static class F리엑터데미지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(Byte.parseByte(splitted[2]));
            return 1;
        }
    }

    public static class 리엑터삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
            if (splitted[1].equals("all")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
            return 1;
        }
    }

    public static class 리엑터세팅 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class 리셋리엑터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetReactors();
            return 1;
        }
    }

    public static class 모두에게쪽지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (splitted.length >= 1) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters().values()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                }
            } else {
                c.getPlayer().dropMessage(6, "Use it like this, !sendallnote <text>");
                return 0;
            }
            return 1;
        }
    }

    public static class 버프스킬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2])).applyTo(c.getPlayer(), true);
            return 0;
        }
    }

    public static class 버프아이템 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleItemInformationProvider.getInstance().getItemEffect(Integer.parseInt(splitted[1])).applyTo(c.getPlayer(), true);
            return 0;
        }
    }

    public static class 버프아이템EX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleItemInformationProvider.getInstance().getItemEffectEX(Integer.parseInt(splitted[1])).applyTo(c.getPlayer(), true);
            return 0;
        }
    }

    public static class 아이템사이즈 extends CommandExecute { //test

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Number of items: " + MapleItemInformationProvider.getInstance().getAllItems().size());
            return 0;
        }
    }

    public static class 스타더스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 2) {
                if (splitted[0].equals("포인트")) {
                    c.getPlayer().AddStarDustPoint(Integer.parseInt(splitted[1]), new Point(-1, -1));
                } else if (splitted[0].equals("코인")) {
                    c.getPlayer().AddStarDustCoin(Integer.parseInt(splitted[1]));
                }
            }
            return 0;
        }
    }

    public static class 아무것도안했는데 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getSession().writeAndFlush(InfoPacket.updateClientInfoQuest(217, "reward=" + GameConstants.getCurrentDate() + ";count=0;uDate=" + FileoutputUtil.CurrentReadable_Date() + ";qState=2;logis=0CL=9;T=20190114093031;use=47;total=" + splitted[1] + ";exp=" + splitted[2]));
            c.getSession().writeAndFlush(UIPacket.openUI(1207));
            return 0;
        }
    }

    public static class 패킷출력 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ServerConstants.DEBUG_RECEIVE = !ServerConstants.DEBUG_RECEIVE;
            ServerConstants.DEBUG_SEND = !ServerConstants.DEBUG_SEND;
            return 0;
        }
    }

    public static class 리시브출력 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ServerConstants.DEBUG_RECEIVE = !ServerConstants.DEBUG_RECEIVE;
            return 0;
        }
    }

    public static class 센드출력 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ServerConstants.DEBUG_SEND = !ServerConstants.DEBUG_SEND;
            return 0;
        }
    }
}
