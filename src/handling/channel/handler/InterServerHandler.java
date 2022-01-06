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
import client.inventory.AuctionItem;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import connector.ConnectorClient;
import connector.ConnectorServer;
import constants.GameConstants;
import constants.ServerConstants;
import handling.RecvPacketOpcode;
import handling.auction.AuctionServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.World.Guild;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import server.Randomizer;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import server.quest.QuestCompleteStatus;
import tools.Pair;
import tools.TripleDESCipher;
import tools.data.LittleEndianAccessor;
import tools.packet.*;
import tools.packet.CWvsContext.BuddylistPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.GuildPacket;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Timer;

public class InterServerHandler {

    public static final void EnterCS(final MapleClient c, final MapleCharacter chr, boolean npc) {
        chr.getClient().getSession().writeAndFlush(CField.UIPacket.OnSetMirrorDungeonInfo(false));  //두개 주석풀면 캐샵 미러던전 나옴.
        c.getSession().writeAndFlush(CField.UIPacket.openUI(152));
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));

        //UI띄우기
/*        try {
         if (npc) {
         chr.getClient().removeClickedNPC();
         NPCScriptManager.getInstance().dispose(chr.getClient());
         chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
         NPCScriptManager.getInstance().start(c, ServerConstants.csNpc);
         } else {
         if (chr.getMap() == null || chr.getEventInstance() != null || c.getChannelServer() == null) {
         c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
         return;
         }

         if (World.getPendingCharacterSize() >= 10) {
         chr.dropMessage(1, "The server is busy at the moment. Please try again in a minute or less.");
         c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
         return;
         }

         final ChannelServer ch = ChannelServer.getInstance(c.getChannel());
         chr.changeRemoval();

         if (chr.getMessenger() != null) {
         MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
         World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
         }

         PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
         PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
         World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
         ch.removePlayer(chr);
         c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
         chr.saveToDB(false, false);
         chr.getMap().removePlayer(chr);
         c.getSession().writeAndFlush(CField.getChannelChange(c, Integer.parseInt(CashShopServer.getIP().split(":")[1])));
         c.setPlayer(null);
         c.setReceiving(false);
         }
         } catch (Exception e) {
         e.printStackTrace();
         }*/
    }

    public static final void Loggedin(final int playerid, final MapleClient c) {
        try {
            final ChannelServer channelServer = c.getChannelServer();
            MapleCharacter player;
            final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);

            if (transfer == null) { // Player isn't in storage, probably isn't CC
                player = MapleCharacter.loadCharFromDB(playerid, c, true);
                Pair<String, String> ip = LoginServer.getLoginAuth(playerid);
                String s = c.getSessionIPAddress();
                if (ip == null || !s.substring(s.indexOf('/') + 1, s.length()).equals(ip.left)) {
                    if (ip != null) {
                        LoginServer.putLoginAuth(playerid, ip.left, ip.right);
                    }
                    c.disconnect(true, false, false);
                    c.getSession().close();
                    return;
                }
                c.setTempIP(ip.right);
            } else {
                player = MapleCharacter.ReconstructChr(transfer, c, true);
            }
            c.setPlayer(player);
            c.setAccID(player.getAccountID());
            c.loadKeyValues();

            if (!c.CheckIPAddress()) { // Remote hack
                c.disconnect(true, false, false);
                c.getSession().close();
                return;
            }

            channelServer.removePlayer(player);

            World.isCharacterListConnected(player.getName(), c.loadCharacterNames(c.getWorld()));

            c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
            channelServer.addPlayer(player);


            int[] bossquests = new int[]{33565,
                31851,
                31833,
                3496,
                3470,
                30007,
                3170,
                31179,
                3521,
                31152,
                34015,
                33294,
                34330,
                34585,
                35632,
                35731,
                35815,
                34478,
                34331, // ??
                34478,
                100114, // 어드벤처 아일랜드
                16013, // 유니온
                34120,
                34218,
                34330,
                34331,
                34478,
                34269,
                34272,
                34585,
                34586,
                6500, // 포켓
                1465,
                1466,
                32240,
                35940,
                26607, 39921, 16880, 35800, 35700, 35740, 34450, 39439, 39572, 39684, 34965, 34701, 31800, 31852, 34839, 34840, 2646, 500978
            };

            // 보스 UI 활성화
            for (int questid : bossquests) {
                if (player.getQuestStatus(questid) != 2) {
                    if (questid == 1465 || questid == 1466) {
                        if (player.getLevel() >= 200) {
                            MapleQuest.getInstance(questid).forceComplete(player, 0, false);
                        }
                    } else {
                        MapleQuest.getInstance(questid).forceComplete(player, 0, false);
                    }
                }
            }

            //퀘스트 npc 활성화
            for (int questid : QuestCompleteStatus.completeQuests) {
                if (player.getQuestStatus(questid) != 2) {
                    MapleQuest.getInstance(questid).forceComplete(player, 0, false);
                }
            }

            if ((GameConstants.isKOC(c.getPlayer().getJob()) || GameConstants.isMichael(c.getPlayer().getJob())) && c.getPlayer().getLevel() >= 245) { //각성한 시그너스 ( 초월자 시그너스 스킬 )
                if (player.getQuestStatus(35635) != 2) {
                    MapleQuest.getInstance(35635).forceComplete(player, 0, false);
                }
            }

            if (c.getPlayer().getKeyValue(100229, "hue") < 0) { //캐릭터 색변
                c.getPlayer().setKeyValue(100229, "hue", 0 + "");
            }
            if (c.getPlayer().getKeyValue(125, "date") != GameConstants.getCurrentDate_NoTime()) {
                c.getPlayer().setKeyValue(125, "date", String.valueOf(GameConstants.getCurrentDate_NoTime()));
                int pirodo = 0;
                switch (c.getPlayer().getTier()) {
                    case 1: {
                        pirodo = 80;
                        break;
                    }
                    case 2: {
                        pirodo = 120;
                        break;
                    }
                    case 3: {
                        pirodo = 160;
                        break;
                    }
                    case 4: {
                        pirodo = 200;
                        break;
                    }
                    case 5: {
                        pirodo = 240;
                        break;
                    }
                    case 6: {
                        pirodo = 280;
                        break;
                    }
                    case 7: {
                        pirodo = 320;
                        break;
                    }
                    case 8: {
                        pirodo = 360;
                        break;
                    }
                }
                c.getPlayer().setKeyValue(123, "pp", String.valueOf(pirodo));
            }
            int chrcolor = (int) c.getPlayer().getKeyValue(100229, "hue");
            c.getPlayer().getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(100229, "change=1;hue=" + chrcolor + ";first=0"));

            // 유니온 자동정리
            if (player.getKeyValue(18771, "rank") == -1 || player.getKeyValue(18771, "rank") == 100) {
                player.setKeyValue(18771, "rank", "101"); // 기본지정
            }

            if (player.getSkillLevel(151001004) < 0 && GameConstants.isAdele(player.getJob())) {
                player.changeSkillLevel(151001004, (byte) 1, (byte) 1);
            }

            if (c.getKeyValue("rank") == null) {
                c.setKeyValue("rank", String.valueOf(player.getKeyValue(18771, "rank")));
            }

            if (Integer.parseInt(c.getKeyValue("rank")) < player.getKeyValue(18771, "rank")) {
                c.setKeyValue("rank", String.valueOf(player.getKeyValue(18771, "rank")));
            }

            if (Integer.parseInt(c.getKeyValue("rank")) > player.getKeyValue(18771, "rank")) {
                player.setKeyValue(18771, "rank", c.getKeyValue("rank"));
            }

            if (player.getKeyValue(16180, "point") == -1) {
                player.setKeyValue(16180, "point", "0");
            }

            if (player.getInnerSkills().size() == 0) {
                player.getInnerSkills().add(new InnerSkillValueHolder(70000004, (byte) 1, (byte) 1, (byte) 0));
                player.getInnerSkills().add(new InnerSkillValueHolder(70000004, (byte) 1, (byte) 1, (byte) 0));
                player.getInnerSkills().add(new InnerSkillValueHolder(70000004, (byte) 1, (byte) 1, (byte) 0));
            }

            // 유니온끝
            player.LoadPlatformerRecords();
            player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
            player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));

            if (player.choicepotential != null && player.memorialcube != null) {
                Item ordinary = player.getInventory(MapleInventoryType.EQUIP).getItem(player.choicepotential.getPosition());
                if (ordinary != null) {
                    player.choicepotential.setInventoryId(ordinary.getInventoryId());
                }
            }

            if (transfer == null) {
                if (GameConstants.isPathFinder(player.getJob())) {
                    player.giveRelikGauge(0, null);
                }
            }

            if (c.getKeyValue("PNumber") == null) {
                c.setKeyValue("PNumber", "0");
            }

            if (player.returnscroll != null) {
                Item ordinary = player.getInventory(MapleInventoryType.EQUIP).getItem(player.returnscroll.getPosition());
                if (ordinary != null) {
                    player.returnscroll.setInventoryId(ordinary.getInventoryId());
                }
            }
            sendOpcodeEncryption(c);
            c.getSession().writeAndFlush(CField.getCharInfo(player));
            c.getSession().writeAndFlush(CSPacket.enableCSUse());
            c.getSession().writeAndFlush(SLFCGPacket.SetupZodiacInfo());
            if (player.getKeyValue(190823, "grade") == -1) {
                player.setKeyValue(190823, "grade", "0");
            }


            int lock = c.getPlayer().isLockNeoCore() ? 1 : 0;
            c.getPlayer().updateInfoQuest(501215, "point=" + c.getPlayer().getItemQuantity(4310308, false) + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";;week=0;total=0;today=0;lock=" + lock + ""); //네오 코어

            player.updateLinkSkillPacket();

            c.getSession().writeAndFlush(CWvsContext.updateMaplePoint(player));
            MapleQuest.getInstance(500978).forceComplete(player, 30748204); //레드피시 튜토리얼 제거 (지우면 계속 뜸)
            player.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestInfo(1, 2, 500978)); //전구 개빡침 이거
            if (player.returnscroll != null) {
                c.getSession().writeAndFlush(CWvsContext.returnEffectConfirm(player.returnscroll, player.returnSc));
                c.getSession().writeAndFlush(CWvsContext.returnEffectModify(player.returnscroll, player.returnSc));
            }

            if (player.choicepotential != null && player.memorialcube != null) {
                //after 값 출력
                c.getSession().writeAndFlush(CField.getBlackCubeStart(player, player.choicepotential, false, player.memorialcube.getItemId(), player.memorialcube.getPosition(), player.getItemQuantity(5062010, false)));
            }

            if (player.isGM() && !player.getBuffedValue(9001004)) {
//            	SkillFactory.getSkill(GameConstants.GMS ? 9101004 : 9001004).getEffect(1).applyTo(player);
            }
            if (GameConstants.isZero(player.getJob())) {
                int[] ZeroQuest = new int[]{32550, 33565, 3994, 6000, 39001, 40000, 40001, 7049, 40002, 40003, 40004, 40100, 40101, 6995, 40102, 40103, 40104, 40105, 40106, 40107, 40200, 40108, 40201, 40109, 40202, 40110, 40203, 40111, 40204, 40050, 40112, 40205, 40051, 40206, 40052, 40207, 40300, 40704, 40053, 40208, 40301, 7783, 40054, 40209, 40302, 40705, 40055, 40210, 40303, 40056, 40304, 7600, 40800, 40057, 40305, 40801, 40058, 40306, 40059, 40307, 40400, 40060, 40308, 40401, 40061, 40309, 40402, 40960, 40062, 40310, 40403, 40930, 40961, 40063, 40404, 40900, 40931, 40962, 40405, 7887, 40901, 40932, 40963, 40406, 40902, 40933, 40964, 40407, 40500, 40903, 40934, 40408, 40501, 40904, 40409, 40502, 7860, 40905, 40503, 7892, 7707, 40504, 40505, 40970, 40506, 40940, 40971, 41250, 41312, 40600, 40910, 40941, 40972, 41251, 40601, 40911, 40942, 40973, 41252, 40602, 40912, 40943, 40974, 41253, 41315, 41408, 40603, 40913, 40944, 41254, 41316, 40604, 40914, 41255, 41317, 40605, 41256, 40606, 41257, 41350, 40607, 40700, 41103, 41258, 41351, 40701, 40980, 41104, 41352, 40702, 40950, 41105, 41353, 40703, 40920, 40951, 41106, 41261, 41354, 40921, 40952, 40922, 40953, 41263, 40923, 40954, 41264, 41357, 40924, 41358, 41111, 41359, 41050, 41360, 41114, 41269, 41300, 41115, 41270, 41301, 41363, 41302, 41364, 41303, 41365, 41055, 41304, 41366, 41305, 41925, 41306, 41926, 41307, 41400, 41370, 41401};
                for (int questid : ZeroQuest) {
                    if (player.getQuestStatus(questid) != 2) {
                        MapleQuest.getInstance(questid).forceComplete(player, 0);
                    }
                }
            }

            if (c.getPlayer().FullMakerSize > 0) {
                c.getPlayer().FullMakerSize = 0;
                c.getPlayer().getMap().broadcastMessage(CField.StartFullMaker(c.getPlayer().FullMakerSize, 0));
                c.getPlayer().setFullMakerBox((byte) 0);
                c.getPlayer().setStartFullMakerTime(0);
            }

            player.addKV("bossPractice", "0");

            if (player.getKeyValue(1477, "count") == -1) {
                player.setKeyValue(1477, "count", "0");
            }

            if (player.getKeyValue(19019, "id") == -1) {
                player.setKeyValue(19019, "id", "0");
            }

            if (player.getKeyValue(7293, "damage_skin") == -1) {
                player.setKeyValue(7293, "damage_skin", "2438159");
            }

            if (player.getKeyValue(1068, "count") == -1) {
                player.setKeyValue(1068, "count", "30");
            }
            if (player.getKeyValue(501215, "today") < 0) {
                player.setKeyValue(501215, "today", "0");
            }

            if (player.getKeyValue(100592, "point") == -1) {
                player.setKeyValue(100592, "point", "0");
                player.setKeyValue(100592, "sum", "0");
                player.setKeyValue(100592, "date", String.valueOf(GameConstants.getCurrentDate_NoTime()));
                player.setKeyValue(100592, "today", "0");
                player.setKeyValue(100592, "total", "0");
            }

            if (player.getKeyValue(100592, "date") != GameConstants.getCurrentDate_NoTime()) {
                player.setKeyValue(100592, "date", String.valueOf(GameConstants.getCurrentDate_NoTime()));
                player.setKeyValue(100592, "today", "0");
            }

            if (player.getKeyValue(501045, "point") == -1) {
                player.setKeyValue(501045, "point", "0");
                player.setKeyValue(501045, "lv", "1");
                player.setKeyValue(501045, "sp", "0");
                player.setKeyValue(501045, "reward0", "0");
                player.setKeyValue(501045, "reward1", "0");
                player.setKeyValue(501045, "reward2", "0");
                player.setKeyValue(501045, "mapTuto", "2");
                player.setKeyValue(501045, "skillTuto", "1");
                player.setKeyValue(501045, "payTuto", "1");
            }

            if (player.getKeyValue(501046, "start") == -1) {
                player.setKeyValue(501046, "start", "1");
                for (int i = 0; i < 9; ++i) {
                    player.setKeyValue(501046, String.valueOf(i), "0");
                }
            }
            if (player.getKeyValue(800023, "indiepmer") > 0) {
                while (player.getBuffedValue(80002387)) {
                    player.cancelEffect(c.getPlayer().getBuffedEffect(80002387), false, -1);
                }
                SkillFactory.getSkill(80002387).getEffect(1).applyTo(player);
            }

            if (player.getKeyValue(800023, "IndieCrMax") > 0) {
                while (player.getBuffedValue(80002388)) {
                    player.cancelEffect(c.getPlayer().getBuffedEffect(80002388), false, -1);
                }
                SkillFactory.getSkill(80002388).getEffect(1).applyTo(player);
            }

            if (player.getKeyValue(20210113, "orgelonoff") == -1) {
                c.getPlayer().setKeyValue(20210113, "orgelonoff", "0");
                c.getPlayer().updateInfoQuest(100720, "count=0;fever=0;");
            }

            if (c.getPlayer().getKeyValue(20210113, "orgelonoff") == 1) { //오르겔
                c.getPlayer().setKeyValue(20210113, "orgelonoff", "0");
                c.getPlayer().updateInfoQuest(100720, "count=0;fever=0;");
            }

            if (c.getKeyValue("state") == null) {
                c.setKeyValue("s1", "0");
                c.setKeyValue("s2", "0");
                c.setKeyValue("s3", "0");
                c.setKeyValue("s4", "0");
                c.setKeyValue("s5", "0");
                c.setKeyValue("s6", "0");
                c.setKeyValue("s7", "0");
                c.setKeyValue("s8", "0");
                c.setKeyValue("s9", "0");
                c.setKeyValue("state", "0");
                c.setKeyValue("current", "1");
                c.setKeyValue("total", "0");
            }

            if (c.getKeyValue("dailyGiftDay") == null) {
                c.setKeyValue("dailyGiftDay", "0");
            }

            if (c.getKeyValue("dailyGiftComplete") == null) {
                c.setKeyValue("dailyGiftComplete", "0");
            }

            if (player.getKeyValue(16700, "date") != GameConstants.getCurrentDate_NoTime()) {
                player.setKeyValue(16700, "count", "0");
                player.setKeyValue(16700, "date", String.valueOf(GameConstants.getCurrentDate_NoTime()));
            }

            for (AuctionItem item : AuctionServer.getItems().values()) {
                if (item.getCharacterId() == player.getId() && item.getState() == 4) {
                    player.getClient().getSession().writeAndFlush(CWvsContext.AlarmAuction(player, item));
                    break;
                }
            }

            if (player.getKeyValue(19019, "id") > 0) {
                if (!player.haveItem((int) player.getKeyValue(19019, "id"))) {
                    player.setKeyValue(19019, "id", "0");
                    player.getMap().broadcastMessage(player, CField.showTitle(player.getId(), 0), false);
                }
            }

            c.getPlayer().loadPremium();
            StringBuilder sb3 = new StringBuilder();
            Date data = new Date();
            sb3.append((data.getYear() + 1900));
            sb3.append(data.getMonth() < 10 ? "0" + (data.getMonth() + 1) : String.valueOf((data.getMonth() + 1)));
            sb3.append(data.getDate() < 10 ? "0" + data.getDate() : String.valueOf(data.getDate()));

            /* 소울인챈터 */
            Equip weapon = (Equip) player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);

            if (weapon != null && player.getBuffedEffect(MapleBuffStat.SoulMP) == null) {
                player.setSoulMP((Equip) weapon);
            }

            int soulid = weapon != null ? weapon.getSoulSkill() : 0;
            if ((soulid > 0) && (transfer == null)) {
                player.setSoulMP((Equip) weapon);
            }

            boolean cgr = false;
            for (MapleGuild mg : Guild.getGuilds(1, 999, 1, 999, 1, 999)) {
                if (mg.getRequest(player.getId()) != null) {
                    c.getPlayer().setKeyValue(26015, "name", mg.getName());
                    cgr = true;
                }
            }
            if (!cgr || c.getPlayer().getGuild() != null) {
                c.getPlayer().setKeyValue(26015, "name", "");
            }

            if (player.getKeyValue(333333, "quick0") > 0) {
                c.getSession().writeAndFlush(CField.quickSlot(player));
            }

            MatrixHandler.calcSkillLevel(player, -1);

            int linkSkill = GameConstants.getMyLinkSkill(player.getJob());
            if (linkSkill > 0 && player.getSkillLevel(linkSkill) == 0) {
                player.changeSkillLevel(linkSkill, (byte) (player.getLevel() >= 120 ? 2 : 1), (byte) 2);
            }

            if (c.getKeyValue("goldT") == null || c.getKeyValue("goldT").equals("0")) {
                c.setKeyValue("goldCount", "0");
                c.setKeyValue("goldT", GameConstants.getCurrentFullDate());
                //  c.getSession().writeAndFlush(CField.getGameMessage(7, "황금마차 시간 기록이 시작되었습니다."));
            }

            if (GameConstants.isWildHunter(player.getJob())) {
                boolean change = false;
                for (int a = 9304000; a <= 9304008; ++a) {
                    int jaguarid = GameConstants.getJaguarType(a);
                    String info = player.getInfoQuest(23008);
                    for (int i = 0; i <= 8; ++i) {
                        if (info.contains(i + "=1")) { // 이미 있을 경우
                            continue; // 패스
                        } else if (i == jaguarid) {
                            info += i + "=1;";
                        }
                    }
                    player.updateInfoQuest(23008, info);
                    player.updateInfoQuest(123456, String.valueOf(jaguarid * 10)); // 마지막으로 사용한 포획
                    change = true;
                }
                if (change) {
                    c.getSession().writeAndFlush(CWvsContext.updateJaguar(player));
                }
            }

            String burningCid = c.getKeyValue("TeraBurning");
            boolean isBurn = burningCid != null && Integer.parseInt(burningCid) == player.getId();
            if (isBurn && player.getKeyValue(190823, "grade") == 0) {
                player.setKeyValue(190823, "grade", "1");
            }

            final MapleQuestStatus stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            c.getSession().writeAndFlush(CWvsContext.pendantSlot(true));//stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
            c.getSession().writeAndFlush(CWvsContext.temporaryStats_Reset());
            player.getMap().addPlayer(player);
            c.getSession().writeAndFlush(CWvsContext.setBossReward(player));
            c.getSession().writeAndFlush(CWvsContext.onSessionValue("kill_count", "0"));
            c.getSession().writeAndFlush(CWvsContext.updateDailyGift("count=" + c.getKeyValue("dailyGiftComplete") + ";day=" + c.getKeyValue("dailyGiftDay") + ";date=" + player.getKeyValue(16700, "date")));
            c.getSession().writeAndFlush(CField.dailyGift(player, 1, 0));

            player.checkBuffDurationRemain();

            if (isBurn) {
                if (player.getSkillLevel(80000545) == 0 && player.getLevel() < 240)
                    player.changeSkillLevel(80000545, (byte) 1, (byte) 1);
            }

            if (player.getSkillLevel(80003082) == 0)
                player.changeSkillLevel(80003082, (byte) 1, (byte) 1);

            try {
                // Start of buddylist
                final int buddyIds[] = player.getBuddylist().getBuddyIds();
                World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), c.getAccID(), buddyIds);
                if (player.getParty() != null) {
                    final MapleParty party = player.getParty();
                    World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));

                    if (party != null && party.getExpeditionId() > 0) {
                        final MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                        if (me != null) {
                            c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket.expeditionStatus(me, false));
                        }
                    }
                }

                final AccountIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getBuddylist(), player.getId(), buddyIds);
                for (AccountIdChannelPair onlineBuddy : onlineBuddies) {
                    player.getBuddylist().get(onlineBuddy.getAcountId()).setChannel(onlineBuddy.getChannel());
                }
                player.getBuddylist().setChanged(true);
                c.getSession().writeAndFlush(BuddylistPacket.updateBuddylist(player.getBuddylist().getBuddies(), null, (byte) 20));
                // Start of Messenger
                final MapleMessenger messenger = player.getMessenger();
                if (messenger != null) {
                    World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                    World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
                }

                // Start of Guild and alliance
                if (player.getGuildId() > 0) {
                    World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                    c.getSession().writeAndFlush(GuildPacket.showGuildInfo(player));
                    final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                    if (gs != null) {
                        final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                        if (packetList != null) {
                            for (byte[] pack : packetList) {
                                if (pack != null) {
                                    c.getSession().writeAndFlush(pack);
                                }
                            }
                        }
                    } else { //guild not found, change guild id
                        player.setGuildId(0);
                        player.setGuildRank((byte) 5);
                        player.setAllianceRank((byte) 5);
                        player.saveGuildStatus();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
            if (pendingBuddyRequest != null) {
                player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getRepName(), pendingBuddyRequest.getAccId(), pendingBuddyRequest.getId(), pendingBuddyRequest.getGroupName(), -1, false, pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob(), pendingBuddyRequest.getMemo()));
                c.getSession().writeAndFlush(CWvsContext.BuddylistPacket.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getAccId(), pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(), pendingBuddyRequest.getJob(), c, pendingBuddyRequest.getGroupName(), pendingBuddyRequest.getMemo()));
            }

            player.getClient().getSession().writeAndFlush(CWvsContext.serverMessage("", channelServer.getServerMessage()));
            player.sendMacros();
            if (player.isGM()) {
                player.showNote();
            }
            player.updatePartyMemberHP();
            player.startFairySchedule(false);
            player.gainDonationSkills();
            c.getSession().writeAndFlush(CField.getKeymap(player.getKeyLayout()));
            c.getSession().writeAndFlush(CWvsContext.OnClaimSvrStatusChanged(true)); //신고하기 버튼 활성화
            player.updatePetAuto();
            player.expirationTask(true, transfer == null);

            c.getSession().writeAndFlush(CWvsContext.setUnion(c)); //ㄴ
            player.getStat().recalcLocalStats(player);
            c.getSession().writeAndFlush(CWvsContext.unionFreeset(c, 0)); // 일단 1번 프리셋만

            if (GameConstants.isXenon(player.getJob())) {
                player.startXenonSupply(SkillFactory.getSkill(30020232));
            }

            if (GameConstants.isBlaster(player.getJob())) {
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.RWCylinder, new Pair<>(1, 0));
                c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, player));
            }

            c.getPlayer().updateSingleStat(MapleStat.FATIGUE, c.getPlayer().getFatigue()); //피로도
            if (player.getStat().equippedSummon > 0) {
                SkillFactory.getSkill(player.getStat().equippedSummon).getEffect(1).applyTo(player, true);
            }

            c.getSession().writeAndFlush(CField.HeadTitle(player.HeadTitle()));

            PetHandler.updatePetSkills(player, null);

            String towerchair = c.getPlayer().getInfoQuest(7266);
            if (!towerchair.equals("")) {
                c.getPlayer().updateInfoQuest(7266, towerchair);
            }//point=102;start=1;date=21/01/29;today=100;start2=1;lock=1
            if (player.getKeyValue(100711, "point") < 0) {
                player.setKeyValue(100711, "point", "0");
            }
            if (player.getKeyValue(100712, "point") < 0) {
                player.setKeyValue(100712, "point", "0");
            }
            if (player.getKeyValue(501215, "point") < 0) {
                player.setKeyValue(501215, "point", "0");
            }

            c.getSession().writeAndFlush(SLFCGPacket.StarDustUI("UI/UIWindowEvent.img/2020neoCoin", 0, 1));       //player.getKeyValue(100592, "total"), player.getKeyValue(100592, "point")


            c.getPlayer().updateInfoQuest(100711, "point=" + player.getKeyValue(100711, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";today=0;total=0;lock=0"); //네오 스톤
            c.getPlayer().updateInfoQuest(100712, "point=" + player.getKeyValue(100712, "point") + ";start=1;date=" + GameConstants.getCurrentDate_NoTime() + ";today=0;start2=1;lock=0"); //네오 젬
//            c.getPlayer().updateInfoQuest(501215, "point=" + player.getKeyValue(501215, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";week=0;total=0;today=" + player.getKeyValue(501215, "today") + ";lock=" + lock + ""); //네오 코어

            if (player.getClient().isFirstLogin() && !player.isGM() && !player.getName().equals("오브")) {
                World.Broadcast.broadcastMessage(CField.UIPacket.detailShowInfo(player.getName() + "님, 접속을 환영합니다. 오늘도 " + LoginServer.getServerName() + "에서 즐거운 시간 되세요.", false));
                player.getClient().setLogin(false);
            }

            /*
            String 스킬[][] = {{"2311003", "HolySymbol", "20"}, {"5121009", "WindBooster", "20"}, {"3121002", "SharpEyes", "30"}, {"2001002", "MagicGuard", "10"}, {"1311015", "CrossOverChain", "20"}, {"24121004", "PrayOfAria", "30"}, {"400051015", "Screw", "25"}};
            for (int i = 0; i < 스킬.length; i++) {
                if (player.getKeyValue(207720, 스킬[i][1]) == -1) {
                    player.setKeyValue(207720, 스킬[i][1], "0");
                }
                if (player.getKeyValue(207720, 스킬[i][1]) == 1) {
                    SkillFactory.getSkill(Integer.parseInt(스킬[i][0])).getEffect(Integer.parseInt(스킬[i][2])).applyTo(player);
                }
            }
             */
            if (ServerConstants.ConnectorSetting) {
                ConnectorClient cli = ConnectorServer.getInstance().getClientStorage().getClientByName(player.getClient().getAccountName());
                if (cli != null) {
                    player.getClient().setconnecterClient(cli);
                    if (player.getClient().getAccountName().equals(cli.getId())) {
                        cli.setChar(player);
                    } else if (player.getClient().getAccountName().equals(cli.getSecondId())) {
                        cli.setSecondChar(player);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean room) {
        try {
            if (chr == null || chr.getEventInstance() != null || chr.getMap() == null || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (World.getPendingCharacterSize() >= 10) {
                chr.dropMessage(1, "채널 이동중인 사람이 많습니다. 잠시 후 시도해주세요.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            final int chc = slea.readByte() + 1;
            int mapid = 0;
            if (room) {
                mapid = slea.readInt();
            }
            slea.readInt();
            if (!World.isChannelAvailable(chc)) {
                chr.dropMessage(1, "현재 해당 채널이 혼잡하여 이동하실 수 없습니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (room && (mapid < 910000001 || mapid > 910000022)) {
                chr.dropMessage(1, "현재 해당 채널이 혼잡하여 이동하실 수 없습니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (room) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.DevilishPower, 400051015);
                if (chr.getMapId() == mapid) {
                    if (c.getChannel() == chc) {
//                        chr.dropMessage(1, "You are already in " + chr.getMap().getMapName());
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    } else { // diff channel
                        chr.changeChannel(chc);
                    }
                } else { // diff map
                    if (c.getChannel() != chc) {
                        chr.changeChannel(chc);
                    }
                    final MapleMap warpz = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid);
                    if (warpz != null) {
                        chr.changeMap(warpz, warpz.getPortal("out00"));
                    } else {
                        chr.dropMessage(1, "현재 해당 채널이 혼잡하여 이동하실 수 없습니다.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    }
                }
            } else {
                chr.cancelEffectFromBuffStat(MapleBuffStat.DevilishPower, 400051015);
                chr.changeChannel(chc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getGameQuitRequest(RecvPacketOpcode header, LittleEndianAccessor rh, MapleClient c) {
        if (header == RecvPacketOpcode.GAME_EXIT) {
            rh.skip(8);
        }
        String account = rh.readMapleAsciiString();
        if (account == null || account.equals("")) {
            account = c.getAccountName();
        }
        if (c == null) { // ??????
            return;
        }

        if (account == null || header == RecvPacketOpcode.GAME_EXIT) {
            c.disconnect(true, false, false);
            c.getSession().close();
            return;
        }
        if (!c.isLoggedIn() && !c.getAccountName().equals(account)) { // hack
            c.disconnect(true, false, false);
            c.getSession().close();
            return;
        }

        c.disconnect(true, false, false);
        c.getSession().writeAndFlush(LoginPacket.getKeyGuardResponse((account) + "," + (c.getPassword(account))));
    }

    public static void sendOpcodeEncryption(MapleClient c) {
        c.mEncryptedOpcode.clear();
        byte[] aKey = new byte[24];

//        String key = "M@PleStoryMaPLe!";
        String key = "G0dD@mnN#H@ckEr!";
        for (int i = 0; i < key.length(); i++) {
            aKey[i] = (byte) key.charAt(i);
        }
        System.arraycopy(aKey, 0, aKey, 16, 8);

        List<Integer> aUsed = new ArrayList<>();
        String sOpcode = "";
        for (int i = 184; i < 2000; i++) {
            int nNum = Randomizer.rand(184, 9999);
            while (aUsed.contains(nNum)) {
                nNum = Randomizer.rand(184, 9999);
            }
            String sNum = String.format("%04d", nNum);
            if (!aUsed.contains(nNum)) {
                c.mEncryptedOpcode.put(nNum, i);
                aUsed.add(nNum);
                sOpcode += sNum;
            }
        }
        aUsed.clear();

        TripleDESCipher pCipher = new TripleDESCipher(aKey);
        try {
            byte[] aBuffer = new byte[Short.MAX_VALUE + 1];
            byte[] aEncrypt = pCipher.Encrypt(sOpcode.getBytes());
            System.arraycopy(aEncrypt, 0, aBuffer, 0, aEncrypt.length);
            for (int i = aEncrypt.length; i < aBuffer.length; i++) {
                aBuffer[i] = (byte) Math.random();
            }
            c.getSession().writeAndFlush(LoginPacket.OnOpcodeEncryption(4, aBuffer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
