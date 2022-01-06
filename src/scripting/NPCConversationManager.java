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
package scripting;

import client.*;
import client.inventory.*;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.PlayersHandler;
import handling.login.LoginInformationProvider;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.Timer;
import server.*;
import server.Timer.CloneTimer;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.maps.Event_DojoAgent;
import server.maps.MapleMap;
import server.marriage.MarriageDataEntry;
import server.quest.MapleQuest;
import server.quest.party.MapleNettPyramid;
import server.shops.MapleShopFactory;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.GuildPacket;
import tools.packet.PacketHelper;
import tools.packet.SLFCGPacket;

import javax.script.Invocable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;

public class NPCConversationManager extends AbstractPlayerInteraction {

    private String getText, script;
    private byte type; // -1 = NPC, 0 = start quest, 1 = end quest
    private byte lastMsg = -1;
    public boolean pendingDisposal = false;
    private Invocable iv;

    public NPCConversationManager(MapleClient c, int npc, int questid, byte type, Invocable iv, String script) {
        super(c, npc, questid);
        this.type = type;
        this.iv = iv;
        this.script = script;
    }

    public void sendConductExchange(String text) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCConductExchangeTalk(id, text));
        lastMsg = 0;
    }

    public void sendPacket(short a, String b) {
        c.getSession().writeAndFlush(SLFCGPacket.SendPacket(a, b));
    }

    public Invocable getIv() {
        return iv;
    }

    public String getScript() {
        return script;
    }

    public int getNpc() {
        return id;
    }

    public int getQuest() {
        return id2;
    }

    public byte getType() {
        return type;
    }

    public void safeDispose() {
        pendingDisposal = true;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public void askMapSelection(final String sel) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getMapSelection(id, sel));
        lastMsg = (byte) 0x11;
    }

    public void sendNext(String text) {
        sendNext(text, id);
    }

    public void sendNext(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 01", (byte) 0));
        lastMsg = 0;
    }

    public void sendPlayerToNpc(String text) {
        sendNextS(text, (byte) 3, id);
    }

    public void StartSpiritSavior() {

    }

    public void StartBlockGame() {
        MapleClient c = getClient();
        c.getSession().writeAndFlush(SLFCGPacket.CharReLocationPacket(0xFFFF, 0));
        c.getSession().writeAndFlush(UIPacket.IntroDisableUI(true));
        c.getSession().writeAndFlush(UIPacket.IntroLock(true));
        Timer.EventTimer.getInstance().schedule(() -> {
            c.getSession().writeAndFlush(SLFCGPacket.BlockGameCommandPacket(1));
            c.getSession().writeAndFlush(SLFCGPacket.BlockGameCommandPacket(2));
            c.getSession().writeAndFlush(SLFCGPacket.BlockGameControlPacket(100, 5));
        }, 2000);
    }

    public boolean setZodiacGrade(int grade) {
        if (c.getPlayer().getKeyValue(190823, "grade") >= grade) {
            return false;
        }
        c.getPlayer().setKeyValue(190823, "grade", String.valueOf(grade));
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), SLFCGPacket.ZodiacRankInfo(c.getPlayer().getId(), grade), true);
        c.getSession().writeAndFlush(SLFCGPacket.playSE("Sound/MiniGame.img/Result_Yut"));
        showEffect(false, "Effect/CharacterEff.img/gloryonGradeup");
//        MapleStatEffect eff = SkillFactory.getSkill(80002419).getEffect(1);
//        c.getPlayer().cancelEffect(eff, true, -1);
//        eff.applyTo(c.getPlayer());
//        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
        return true;
    }

    public void sendNextNoESC(String text) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextNoESC(String text, int id) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextS(String text, byte type) {
        sendNextS(text, type, 0);
    }

    public void sendNextS(String text, byte type, int id, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 01", type, idd));
        lastMsg = 0;
    }

    public void sendNextS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 01", type, idd));
        lastMsg = 0;
    }

    public void sendPrev(String text) {
        sendPrev(text, id);
    }

    public void sendPrev(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendPrevS(String text, byte type) {
        sendPrevS(text, type, 0);
    }

    public void sendPrevS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 00", type, idd));
        lastMsg = 0;
    }

    public void sendNextPrev(String text) {
        sendNextPrev(text, id);
    }

    public void sendNextPrev(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 01", (byte) 0));
        lastMsg = 0;
    }

    public void PlayerToNpc(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public String getMobName(int mobid) {
        MapleData data = null;
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/" + "String.wz"));
        String ret = "";
        List<String> retMobs = new ArrayList<String>();

        data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<>();
        for (MapleData mobIdData : data.getChildren()) {
            mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getLeft() == mobid) {
                ret = mobPair.getRight();
            }
        }
        return ret;
    }

    public void sendNextPrevS(String text, byte type) {
        sendNextPrevS(text, type, 0);
    }

    public void sendNextPrevS(String text, byte type, int id, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 01", type, idd));
        lastMsg = 0;
    }

    public void sendNextPrevS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 01", type, idd));
        lastMsg = 0;
    }

    public void sendDimensionGate(String text) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0x13, text, "00 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendOk(String text) {
        sendOk(text, id);
    }

    public void sendOk(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendOkS(String text, byte type) {
        sendOkS(text, type, 0);
    }

    public void sendOkS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 00", type, idd));
        lastMsg = 0;
    }

    public void sendYesNo(String text) {
        sendYesNo(text, id);
    }

    public void sendYesNo(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 3, text, "", (byte) 0));
        lastMsg = 2;
    }

    public void sendYesNoS(String text, byte type) {
        sendYesNoS(text, type, 0);
    }

    public void sendYesNoS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 3, text, "", type, idd));
        lastMsg = 2;
    }

    public void sendAcceptDecline(String text) {
        askAcceptDecline(text);
    }

    public void sendAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text);
    }

    public void askAcceptDecline(String text) {
        askAcceptDecline(text, id);
    }

    public void askAcceptDecline(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = (byte) 16;
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) lastMsg, text, "", (byte) 0));
    }

    public void askAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text, id);
    }

    public void askAcceptDeclineNoESC(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = (byte) 16;
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) lastMsg, text, "", (byte) 1));
    }

    public void sendStyle(String text, int... args) {
        askAvatar(text, args);
    }

    public void askCustomMixHairAndProb(String text) {
        c.getSession().writeAndFlush(NPCPacket.getNPCTalkMixStyle(id, text, GameConstants.isZero(c.getPlayer().getJob()) ? c.getPlayer().getGender() == 1 ? true : false : false, GameConstants.isAngelicBuster(c.getPlayer().getJob()) ? c.getPlayer().getDressup() ? true : false : false));
        lastMsg = 0x2C;
    }

    public void askAvatar(String text, int... args) {
        c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyle(c.getPlayer(), id, text, args));
        lastMsg = 9;
    }

    public void askAvatar(String text, int[] args1, int[] args2) {
        if (lastMsg > -1) {
            return;
        }
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyleZero(id, text, args1, args2));
        } else {
            c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyle(c.getPlayer(), id, text, args1));
        }
        lastMsg = 9;
    }

    public void askAvatarAndroid(String text, int... args) {
        c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyleAndroid(id, text, args));
    }

    public void sendSimple(String text) {
        sendSimple(text, id);
    }

    public void sendSimple(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNext(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 6, text, "", (byte) 0));
        lastMsg = 5;
    }

    public void sendSimpleS(String text, byte type) {
        sendSimpleS(text, type, 0);
    }

    public void sendSimpleS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNextS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 6, text, "", (byte) type, idd));
        lastMsg = 5;
    }

    public void sendSimpleS(String text, byte type, int id, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNextS(text, type);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 6, text, "", (byte) type, idd));
        lastMsg = 5;
    }

    public void sendStyle(String text, int styles1[], int styles2[]) {
        if (lastMsg > -1) {
            return;
        }
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyleZero(id, text, styles1, styles2));
            lastMsg = 32;
        } else {
            c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyle(c.getPlayer(), id, text, styles1));
            lastMsg = 9;
        }
    }

    public void sendIllustYesNo(String text, int face, boolean isLeft) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendSimple will dc otherwise!
            sendIllustSimple(text, face, isLeft);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 28, text, "", (byte) 0, face, true, isLeft));
        lastMsg = 28; // 틀렸음.
    }

    public void sendIllustSimple(String text, int face, boolean isLeft) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendIllustNext(text, face, isLeft);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 30, text, "", (byte) 0, face, true, isLeft));
        lastMsg = 30;
    }

    public void sendIllustNext(String text, int face, boolean isLeft) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendIllustSimple(text, face, isLeft);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 26, text, "00 01", (byte) 0, face, true, isLeft));
        lastMsg = 26;
    }

    public void sendIllustPrev(String text, int face, boolean isLeft) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendIllustSimple(text, face, isLeft);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 26, text, "01 00", (byte) 0, face, true, isLeft));
        lastMsg = 26;
    }

    public void sendIllustNextPrev(String text, int face, boolean isLeft) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendIllustSimple(text, face, isLeft);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 26, text, "01 01", (byte) 0, face, true, isLeft));
        lastMsg = 26;
    }

    public void sendIllustOk(String text, int face, boolean isLeft) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendIllustSimple(text, face, isLeft);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalk(id, (byte) 26, text, "00 00", (byte) 0, face, true, isLeft));
        lastMsg = 26;
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalkNum(id, text, def, min, max));
        lastMsg = 4;
    }

    public void sendGetText(String text) {
        sendGetText(text, id);
    }

    public void sendGetText(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.getNPCTalkText(id, text));
        lastMsg = 3;
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public void setZeroSecondHair(int hair) {
        getPlayer().setSecondHair(hair);
        getPlayer().updateZeroStats();
        getPlayer().equipChanged();
    }

    public void setZeroSecondFace(int face) {
        getPlayer().setSecondFace(face);
        getPlayer().updateZeroStats();
        getPlayer().equipChanged();
    }

    public void setZeroSecondSkin(int color) {
        getPlayer().setSecondSkinColor((byte) color);
        getPlayer().updateZeroStats();
        getPlayer().equipChanged();
    }

    public void setAngelicSecondHair(int hair) {
        getPlayer().setSecondHair(hair);
        getPlayer().updateAngelicStats();
        getPlayer().equipChanged();
    }

    public void setAngelicSecondFace(int face) {
        getPlayer().setSecondFace(face);
        getPlayer().updateAngelicStats();
        getPlayer().equipChanged();
    }

    public void setAngelicSecondSkin(int color) {
        getPlayer().setSecondSkinColor((byte) color);
        getPlayer().updateAngelicStats();
        getPlayer().equipChanged();
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor((byte) color);
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        gainItem(ticket, (short) -1);

        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setAvatar(int ticket, int args) {
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setZeroAvatar(int ticket, int args1, int args2) {
        gainItem(ticket, (short) -1);

        if (args1 < 100 || args1 < 100) {
            c.getPlayer().setSkinColor((byte) args1);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args1);
            c.getPlayer().setSecondSkinColor((byte) args2);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args2);
        } else if (args1 < 30000 || args2 < 30000) {
            c.getPlayer().setFace(args1);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args1);
            c.getPlayer().setSecondFace(args2);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args2);
        } else {
            c.getPlayer().setHair(args1);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args1);
            c.getPlayer().setSecondHair(args2);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args2);
        }
        c.getPlayer().equipChanged();
        c.getPlayer().fakeRelog(); //TODO 임시:: 업데이트 패킷 필요.
        return 1;
    }

    public void sendNoButtonSay(String text, short type, int delay, int npc) {
        c.getSession().writeAndFlush(NPCPacket.getNpcTalkNoButton(npc, type, delay, text));
    }

    public void setFaceAndroid(int faceId) {
        c.getPlayer().getAndroid().setFace(faceId);
        c.getPlayer().updateAndroid();
    }

    public void setHairAndroid(int hairId) {
        c.getPlayer().getAndroid().setHair(hairId);
        c.getPlayer().updateAndroid();
    }

    public void setSkinAndroid(int color) {
        c.getPlayer().getAndroid().setSkin(color);
        c.getPlayer().updateAndroid();
    }

    public void sendStorage() {
        c.getPlayer().setStorageNPC(id);
        c.getSession().writeAndFlush(NPCPacket.getStorage((byte) 0));
    }

    public void openShop(int idd) {
        MapleShopFactory.getInstance().getShop(idd).sendShop(c);
    }

    public int gainGachaponItem(int id, int quantity) {
        return gainGachaponItem(id, quantity, c.getPlayer().getMap().getStreetName());
    }

    public int gainGachaponItem(int id, int quantity, final String msg) {
        try {
            if (!MapleItemInformationProvider.getInstance().itemExists(id)) {
                return -1;
            }
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, id, (short) quantity);

            if (item == null) {
                return -1;
            }
            final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness > 0) {
                World.Broadcast.broadcastMessage(CWvsContext.getGachaponMega(c.getPlayer().getName(), " : got a(n)", item, rareness, msg));
            }
            c.getSession().writeAndFlush(EffectPacket.showCharmEffect(c.getPlayer(), id, 1, true, ""));
            return item.getItemId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void changeJob(int job) {
        c.getPlayer().changeJob(job);
    }

    public void startQuest(int idd) {
        MapleQuest.getInstance(idd).start(getPlayer(), id);
    }

    public void completeQuest(int idd) {
        MapleQuest.getInstance(idd).complete(getPlayer(), id);
    }

    public void forfeitQuest(int idd) {
        MapleQuest.getInstance(idd).forfeit(getPlayer());
    }

    public void forceStartQuest() {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(int idd) {
        MapleQuest.getInstance(idd).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(String customData) {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), customData);
    }

    public void forceCompleteQuest() {
        MapleQuest.getInstance(id2).forceComplete(getPlayer(), getNpc());
    }

    public void forceCompleteQuest(final int idd) {
        MapleQuest.getInstance(idd).forceComplete(getPlayer(), getNpc());
    }

    public String getQuestCustomData(int id2) {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).getCustomData();
    }

    public void setQuestCustomData(int id2, String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).setCustomData(customData);
    }

    public long getMeso() {
        return getPlayer().getMeso();
    }

    public void gainAp(final int amount) {
        c.getPlayer().gainAp((short) amount);
    }

    public void expandInventory(byte type, int amt) {
        c.getPlayer().expandInventory(type, amt);
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> ids = new LinkedList<Short>();
        for (Item item : equipped.newList()) {
            ids.add(item.getPosition());
        }
        for (short id : ids) {
            MapleInventoryManipulator.unequip(getC(), id, equip.getNextFreeSlot(), MapleInventoryType.EQUIP);
        }
    }

    public void gainItemInStorages(int id) {

        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        int itemid = 0;
        int str = 0, dex = 0, int_ = 0, luk = 0, watk = 0, matk = 0;
        int hp = 0, upg = 0, slot = 0;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM cashstorages WHERE id = ? and charid = ?");
            ps.setInt(1, id);
            ps.setInt(2, c.getPlayer().getId());
            rs = ps.executeQuery();

            if (rs.next()) {
                itemid = rs.getInt("itemid");
                str = rs.getInt("str");
                dex = rs.getInt("dex");
                int_ = rs.getInt("int_");
                luk = rs.getInt("luk");
                watk = rs.getInt("watk");
                matk = rs.getInt("matk");
                hp = rs.getInt("maxhp");
                upg = rs.getInt("upg");
                slot = rs.getInt("slot");
            }
            ps.close();
            rs.close();

            ps2 = con.prepareStatement("DELETE FROM cashstorages WHERE id = ?");
            ps2.setInt(1, id);
            ps2.executeUpdate();
            ps2.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps2 != null) {
                try {
                    ps2.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (rs != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip item = (Equip) (Equip) (ii.getEquipById(itemid));
        item.setStr((short) str);
        item.setDex((short) dex);
        item.setInt((short) int_);
        item.setLuk((short) luk);
        item.setWatk((short) watk);
        item.setMatk((short) matk);
        item.setHp((short) hp);
        item.setUpgradeSlots((byte) slot);
        item.setLevel((byte) upg);
        MapleInventoryManipulator.addbyItem(c, item);
        //아이템지급
    }

    public void StoreInStorages(int charid, int itemid, int str, int dex, int int_, int luk, int watk, int matk) {

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO cashstorages (charid, itemid, str, dex, int_, luk, watk, matk) VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, charid);
            ps.setInt(2, itemid);
            ps.setInt(3, str);
            ps.setInt(4, dex);
            ps.setInt(5, int_);
            ps.setInt(6, luk);
            ps.setInt(7, watk);
            ps.setInt(8, matk);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getCashStorages(int charid) {
        String ret = "";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM cashstorages WHERE charid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();

            if (rs.next()) {
                ret += "#L" + rs.getInt("id") + "##i" + rs.getInt("itemid") + "##z" + rs.getInt("itemid") + "#\r\n";
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    public String getCharacterList(int accountid) {
        String ret = "";

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
            ps.setInt(1, accountid);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret += "#L" + rs.getInt("id") + "#" + rs.getString("name") + "\r\n";
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    public final void clearSkills() {
        final Map<Skill, SkillEntry> skills = new HashMap<>(getPlayer().getSkills());
        final Map<Skill, SkillEntry> newList = new HashMap<>();
        for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            newList.put(skill.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
        }
        getPlayer().changeSkillsLevel(newList);
        newList.clear();
        skills.clear();
    }

    public final void skillmaster() {
        MapleData data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(StringUtil.getLeftPaddedStr("" + c.getPlayer().getJob(), '0', 3) + ".img");
        for (MapleData skill : data) {
            if (skill != null) {
                for (MapleData skillId : skill.getChildren()) {
                    if (!skillId.getName().equals("icon")) {
                        byte maxLevel = (byte) MapleDataTool.getIntConvert("maxLevel", skillId.getChildByPath("common"), 0);
                        if (maxLevel < 0) { // 배틀메이지 데스는 왜 만렙이 250이지?
                            maxLevel = 1;
                        }
                        if (maxLevel > 30) {
                            maxLevel = 30;
                        }
                        if (MapleDataTool.getIntConvert("invisible", skillId, 0) == 0) { //스킬창에 안보이는 스킬은 올리지않음
                            if (c.getPlayer().getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), maxLevel, maxLevel);
                            }
                        }
                    }
                }
            }
        }
        if (GameConstants.isZero(c.getPlayer().getJob())) {
            int jobs[] = {10000, 10100, 10110, 10111, 10112};
            for (int job : jobs) {
                data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(job + ".img");
                for (MapleData skill : data) {
                    if (skill != null) {
                        for (MapleData skillId : skill.getChildren()) {
                            if (!skillId.getName().equals("icon")) {
                                byte maxLevel = (byte) MapleDataTool.getIntConvert("maxLevel", skillId.getChildByPath("common"), 0);
                                if (maxLevel < 0) { // 배틀메이지 데스는 왜 만렙이 250이지?
                                    maxLevel = 1;
                                }
                                if (maxLevel > 30) {
                                    maxLevel = 30;
                                }
                                if (MapleDataTool.getIntConvert("invisible", skillId, 0) == 0) { //스킬창에 안보이는 스킬은 올리지않음
                                    if (c.getPlayer().getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                        c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), maxLevel, maxLevel);
                                    }
                                }
                            }
                        }
                    }
                }
                if (c.getPlayer().getLevel() >= 200) {
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(100001005), (byte) 1, (byte) 1);
                }
            }
        }
        if (GameConstants.isKOC(c.getPlayer().getJob()) && c.getPlayer().getLevel() >= 100) {
            c.getPlayer().changeSkillLevel(11121000, (byte) 30, (byte) 30);
            c.getPlayer().changeSkillLevel(12121000, (byte) 30, (byte) 30);
            c.getPlayer().changeSkillLevel(13121000, (byte) 30, (byte) 30);
            c.getPlayer().changeSkillLevel(14121000, (byte) 30, (byte) 30);
            c.getPlayer().changeSkillLevel(15121000, (byte) 30, (byte) 30);
        }
    }

    public static void writeLog(String path, String data, boolean writeafterend) {
        try {
            File fFile = new File(path);
            if (!fFile.exists()) {
                fFile.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(path, true);
            long time = System.currentTimeMillis();
            java.text.SimpleDateFormat dayTime = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String str = dayTime.format(new java.util.Date(time));
            String msg = str + " | " + data;
            out.write(msg.getBytes());
            out.close();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addBoss(String boss) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                MapleCharacter ch = c.getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null && ch.getGMLevel() < 6) {
                    String k = ch.getV(boss);
                    int key = k == null ? 0 : Integer.parseInt(ch.getV(boss));
                    ch.addKV(boss, String.valueOf(key + 1));
                    ch.addKV("bossPractice", "0");
                    ch.dropMessage(5, (key + 1) + "번 입장하셨습니다.");
                    String logdata = ch.getName() + " | 컨텐츠 : " + boss + " | 입장 횟수 :" + (key + 1) + "\r\n";
                    writeLog("Log/Contents.log", logdata, true);
                }
            }
        }
    }

    public void addBossPractice(String boss) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                MapleCharacter ch = c.getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) {
                    ch.addKV("bossPractice", "1");
                    ch.dropMessage(5, "연습모드에 입장했습니다. 연습모드에서는 보스 처치시에도 아이템이 드랍되지 않습니다.");
                    ch.dropMessage(5, "현재 페이즈를 넘기고 싶다면 @페이즈스킵 명령어를 이용해주세요.");
                    String logdata = ch.getName() + " | 컨텐츠 : " + boss + " | 연습모드\r\n";
                    writeLog("Log/Practice.log", logdata, true);
                }
            }
        }
    }

    public Object[] BossNotAvailableChrList(String boss, int limit) {
        Object[] arr = {};
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                    if (ch != null && ch.getGMLevel() < 6) {
                        String k = ch.getV(boss);
                        int key = k == null ? 0 : Integer.parseInt(ch.getV(boss));
                        if (key >= limit - 1) {
                            arr = add(arr, ch.getName());
                        }
                    }
                }
            }
        }
        return arr;
    }

    public Object[] LevelNotAvailableChrList(int level) {
        Object[] arr = {};
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                    if (ch != null && ch.getGMLevel() < 6) {
                        if (ch.getLevel() < level) {
                            arr = add(arr, ch.getName());
                        }
                    }
                }
            }
        }
        return arr;
    }

    public static Object[] add(Object[] arr, Object... elements) {
        Object[] tempArr = new Object[arr.length + elements.length];
        System.arraycopy(arr, 0, tempArr, 0, arr.length);

        for (int i = 0; i < elements.length; i++) {
            tempArr[arr.length + i] = elements[i];
        }
        return tempArr;

    }

    public boolean partyhaveItem(int itemid, int qty) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                    if (ch != null && ch.getGMLevel() <= 6) {
                        int getqty = itemQuantity(itemid);
                        if (getqty < qty) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean isBossAvailable(String boss, int limit) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                    if (ch != null && !ch.isGM()) {
                        String k = ch.getV(boss);
                        int key = k == null ? 0 : Integer.parseInt(ch.getV(boss));
                        ch.dropMessage(5, "하루에 " + limit + "번 입장하실 수 있습니다.");
                        if (key > limit - 1) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean isLevelAvailable(int level) {
        if (c.getPlayer().getParty() != null) {
            for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                    if (ch != null && ch.getGMLevel() <= 6) {
                        if (ch.getLevel() < level) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean hasSkill(int skillid) {
        Skill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return c.getPlayer().getSkillLevel(theSkill) > 0;
        }
        return false;
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.showEffect(effect));
        } else {
            c.getSession().writeAndFlush(CField.showEffect(effect));
        }
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.playSound(sound));
        } else {
            c.getSession().writeAndFlush(CField.playSound(sound));
        }
    }

    public void environmentChange(boolean broadcast, String env) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.environmentChange(env, 2));
        } else {
            c.getSession().writeAndFlush(CField.environmentChange(env, 2));
        }
    }

    public void updateBuddyCapacity(int capacity) {
        c.getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return c.getPlayer().getBuddyCapacity();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        if (getPlayer().getParty() == null) {
            return inMap;
        }
        for (MapleCharacter char2 : getPlayer().getMap().getCharactersThreadsafe()) {
            if (char2.getParty() != null && char2.getParty().getId() == getPlayer().getParty().getId()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>(); // creates an empty array full of shit..
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) { // double check <3
                    chars.add(ch);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            gainMeso(meso);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public MapleSquad getSquad(String type) {
        return c.getChannelServer().getMapleSquad(type);
    }

    public int getSquadAvailability(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    public boolean registerSquad(String type, int minutes, String startText) {
        if (c.getChannelServer().getMapleSquad(type) == null) {
            final MapleSquad squad = new MapleSquad(c.getChannel(), type, c.getPlayer(), minutes * 60 * 1000, startText);
            final boolean ret = c.getChannelServer().addMapleSquad(squad, type);
            if (ret) {
                final MapleMap map = c.getPlayer().getMap();

                map.broadcastMessage(CField.getClock(minutes * 60));
                map.broadcastMessage(CWvsContext.serverNotice(6, "", c.getPlayer().getName() + startText));
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean getSquadList(String type, byte type_) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad == null) {
                return false;
            }
            if (type_ == 0 || type_ == 3) { // Normal viewing
                sendNext(squad.getSquadMemberString(type_));
            } else if (type_ == 1) { // Squad Leader banning, Check out banned participant
                sendSimple(squad.getSquadMemberString(type_));
            } else if (type_ == 2) {
                if (squad.getBannedMemberSize() > 0) {
                    sendSimple(squad.getSquadMemberString(type_));
                } else {
                    sendNext(squad.getSquadMemberString(type_));
                }
            }
            return true;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return false;
        }
    }

    public byte isSquadLeader(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getLeader() != null && squad.getLeader().getId() == c.getPlayer().getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public MapleCharacter getChar(int id) {
        MapleCharacter chr = null;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            chr = cs.getPlayerStorage().getCharacterById(id);
            if (chr != null) {
                return chr;
            }
        }
        return null;
    }

    public void makeRing(int itemid, MapleCharacter chr) {
        try {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            Item item = ii.getEquipById(itemid);
            Item item1 = ii.getEquipById(itemid);
            item.setUniqueId(MapleInventoryIdentifier.getInstance());
            item1.setUniqueId(MapleInventoryIdentifier.getInstance());
            MapleRing.makeRing(itemid, chr, item.getUniqueId(), item1.getUniqueId()); //파트너꺼
            MapleRing.makeRing(itemid, getPlayer(), item1.getUniqueId(), item.getUniqueId());//내꺼
            MapleInventoryManipulator.addbyItem(getClient(), item);
            MapleInventoryManipulator.addbyItem(chr.getClient(), item1);
            chr.reloadChar();
            c.getPlayer().reloadChar();
            sendOk("선택하신 반지를 제작 완료 하였습니다. 인벤토리를 확인해 봐주시길 바랍니다.");
            chr.dropMessage(5, getPlayer().getName() + "님으로 부터 반지가 도착 하였습니다. 인벤토리를 확인해 주시길 바랍니다.");
        } catch (Exception ex) {
            sendOk("반지를 제작하는데 오류가 발생 하였습니다.");
        }
    }

    public void makeRingRC(int itemid, MapleCharacter chr) {
        try {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            Item item = ii.getEquipById(itemid);
            Item item1 = ii.getEquipById(itemid);
            item.setUniqueId(MapleInventoryIdentifier.getInstance());
            Equip eitem = (Equip) item;
            eitem.setStr((short) 300);
            eitem.setDex((short) 300);
            eitem.setInt((short) 300);
            eitem.setLuk((short) 300);
            eitem.setWatk((short) 300);
            eitem.setMatk((short) 300);
            item1.setUniqueId(MapleInventoryIdentifier.getInstance());
            Equip eitem1 = (Equip) item1;
            eitem1.setStr((short) 300);
            eitem1.setDex((short) 300);
            eitem1.setInt((short) 300);
            eitem1.setLuk((short) 300);
            eitem1.setWatk((short) 300);
            eitem1.setMatk((short) 300);
            MapleRing.makeRing(itemid, chr, eitem.getUniqueId(), eitem1.getUniqueId()); //파트너꺼
            MapleRing.makeRing(itemid, getPlayer(), eitem1.getUniqueId(), eitem.getUniqueId());//내꺼
            MapleInventoryManipulator.addbyItem(getClient(), item);
            MapleInventoryManipulator.addbyItem(chr.getClient(), item1);
            chr.reloadChar();
            c.getPlayer().reloadChar();
            sendOk("선택하신 반지를 제작 완료 하였습니다. 인벤토리를 확인해 봐주시길 바랍니다.");
            chr.dropMessage(5, getPlayer().getName() + "님으로 부터 반지가 도착 하였습니다. 인벤토리를 확인해 주시길 바랍니다.");
        } catch (Exception ex) {
            sendOk("반지를 제작하는데 오류가 발생 하였습니다.");
        }
    }

    public void makeRingHB(int itemid, MapleCharacter chr) {
        try {
            int asd = 300;
            int asd2 = 300;
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            Item item = ii.getEquipById(itemid);
            Item item1 = ii.getEquipById(itemid);
            item.setUniqueId(MapleInventoryIdentifier.getInstance());
            Equip eitem = (Equip) item;
            eitem.setStr((short) asd);
            eitem.setDex((short) asd);
            eitem.setInt((short) asd);
            eitem.setLuk((short) asd);
            eitem.setWatk((short) asd2);
            eitem.setMatk((short) asd2);
            item1.setUniqueId(MapleInventoryIdentifier.getInstance());
            Equip eitem1 = (Equip) item1;
            eitem1.setStr((short) asd);
            eitem1.setDex((short) asd);
            eitem1.setInt((short) asd);
            eitem1.setLuk((short) asd);
            eitem1.setWatk((short) asd2);
            eitem1.setMatk((short) asd2);
            MapleRing.makeRing(itemid, chr, eitem.getUniqueId(), eitem1.getUniqueId()); //파트너꺼
            MapleRing.makeRing(itemid, getPlayer(), eitem1.getUniqueId(), eitem.getUniqueId());//내꺼
            MapleInventoryManipulator.addbyItem(getClient(), item);
            MapleInventoryManipulator.addbyItem(chr.getClient(), item1);
            chr.reloadChar();
            c.getPlayer().reloadChar();
            sendOk("선택하신 반지를 제작 완료 하였습니다. 인벤토리를 확인해 봐주시길 바랍니다.");
            chr.dropMessage(5, getPlayer().getName() + "님으로 부터 반지가 도착 하였습니다. 인벤토리를 확인해 주시길 바랍니다.");
        } catch (Exception ex) {
            sendOk("반지를 제작하는데 오류가 발생 하였습니다.");
        }
    }

    public boolean reAdd(String eim, String squad) {
        EventInstanceManager eimz = getDisconnected(eim);
        MapleSquad squadz = getSquad(squad);
        if (eimz != null && squadz != null) {
            squadz.reAddMember(getPlayer());
            eimz.registerPlayer(getPlayer());
            return true;
        }
        return false;
    }

    public void banMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    public void MapiaStart(final MapleCharacter player, int time, final int morningmap, final int citizenmap1, final int citizenmap2, final int citizenmap3, final int citizenmap4, final int citizenmap5, final int citizenmap6, final int mapiamap, final int policemap, final int drmap, final int after, final int night, final int vote, int bating) {
        String[] job = {"시민", "마피아", "경찰", "의사", "시민", "시민", "마피아", "경찰", "시민", "마피아"};
        String name = "";
        String mapia = "";
        String police = "";
        int playernum = 0;
        int citizennumber = 0;
        //final List<MapleCharacter> players = new ArrayList<MapleCharacter>();

        final MapleMap map = ChannelServer.getInstance(getClient().getChannel()).getMapFactory().getMap(morningmap);
        for (MapleCharacter chr : player.getMap().getCharacters()) {
            playernum++;
        }
        int[] iNumber = new int[playernum];
        for (int i = 1; i <= iNumber.length; i++) {
            iNumber[i - 1] = i;
        }
        for (int i = 0; i < iNumber.length; i++) {
            int iRandom = (int) (Math.random() * playernum);
            int t = iNumber[0];
            iNumber[0] = iNumber[iRandom];
            iNumber[iRandom] = t;
        }
        for (int i = 0; i < iNumber.length; i++) {
            System.out.print(iNumber[i] + ",");
        }
        int jo = 0;
        map.names = "";
        map.mbating = bating * playernum;
        for (MapleCharacter chr : player.getMap().getCharacters()) {
            chr.warp(morningmap);
            map.names += chr.getName() + ",";
            chr.mapiajob = job[iNumber[jo] - 1];
            if (chr.mapiajob.equals("마피아")) {
                mapia += chr.getName() + ",";
            } else if (chr.mapiajob.equals("경찰")) {
                police += chr.getName() + ",";
            } else if (chr.mapiajob.equals("시민")) {
                citizennumber++;
            }
            chr.dropMessage(5, "잠시 후 마피아 게임이 시작됩니다. 총 배팅금은 " + bating * playernum + "메소 입니다.");
            chr.dropMessage(5, "당신의 직업은 " + job[iNumber[jo] - 1] + " 입니다.");
            chr.dropMessage(-1, time + "초 후 마피아 게임이 시작됩니다.");
            jo++;
        }
        final String mapialist = mapia;
        final String policelist = police;
        final int citizennum = citizennumber;
        final int playernuma = playernum;
        final java.util.Timer m_timer = new java.util.Timer();
        TimerTask m_task = new TimerTask() {
            public void run() {
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    if (chr.mapiajob == "마피아") {
                        chr.isMapiaVote = true;
                        chr.dropMessage(6, "마피아인 당신 동료는 " + mapialist + " 들이 있습니다. 밤이되면 같이 의논하여 암살할 사람을 선택해 주시기 바랍니다.");
                    } else if (chr.mapiajob == "경찰") {
                        chr.isPoliceVote = true;
                        chr.dropMessage(6, "경찰인 당신 동료는 " + policelist + " 들이 있습니다. 밤이되면 마피아같다는 사람을 지목하면 마피아인지 아닌지를 알 수 있습니다.");
                    } else if (chr.mapiajob == "의사") {
                        chr.isDrVote = true;
                        chr.dropMessage(6, "당신은 하나밖에 없는 의사입니다. 당신에게 부여된 임무는 시민과 경찰을 살리는 것입니다. 밤이되면 마피아가 지목했을것 같은 사람을 선택하면 살리실 수 있습니다.");
                    } else if (chr.mapiajob == "시민") {
                        chr.dropMessage(6, "당신은 시민입니다. 낮이되면 대화를 통해 마피아를 찾아내 투표로 처형시키면 됩니다.");
                    }
                    chr.getmapiavote = 0;
                    chr.voteamount = 0;
                    chr.getpolicevote = 0;
                    chr.isDead = false;
                    chr.isDrVote = true;
                    chr.isMapiaVote = true;
                    chr.isPoliceVote = true;
                    chr.getdrvote = 0;
                    chr.isVoting = false;
                }
                map.broadcastMessage(CWvsContext.serverNotice(1, "", "진행자>>낮이 되었습니다. 마피아를 찾아내 모두 처형하면 시민의 승리이며, 마피아가 경찰 또는 시민을 모두 죽일시 마피아의 승리입니다.(직업 : 시민,경찰,마피아,의사)"));
                map.playern = playernuma;
                //map.mbating = bating;
                map.morningmap = morningmap;
                map.aftertime = after;
                map.nighttime = night;
                map.votetime = vote;
                map.citizenmap1 = citizenmap1;
                map.citizenmap2 = citizenmap2;
                map.citizenmap3 = citizenmap3;
                map.citizenmap4 = citizenmap4;
                map.citizenmap5 = citizenmap5;
                map.citizenmap6 = citizenmap6;
                map.MapiaIng = true;

                map.mapiamap = mapiamap;
                map.policemap = policemap;
                map.drmap = drmap;
                m_timer.cancel();
                map.MapiaMorning(player);
                map.MapiaChannel = player.getClient().getChannel();
                return;
            }
        };
        m_timer.schedule(m_task, time * 1000);
    }

    public void acceptMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    public int addMember(String type, boolean join) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad != null) {
                return squad.addMember(c.getPlayer(), join);
            }
            return -1;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return -1;
        }
    }

    public byte isSquadMember(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getMembers().contains(c.getPlayer())) {
                return 1;
            } else if (squad.isBanned(c.getPlayer())) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void genericGuildMessage(int code) {
        c.getSession().writeAndFlush(GuildPacket.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0 || c.getPlayer().getGuildRank() != 1) {
            return;
        }
        World.Guild.disbandGuild(gid);
    }

    public void increaseGuildCapacity(boolean trueMax) {
        if (c.getPlayer().getMeso() < 500000 && !trueMax) {
            c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "500,000 메소가 필요합니다."));
            return;
        }
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        if (World.Guild.increaseGuildCapacity(gid, trueMax)) {
            if (!trueMax) {
                c.getPlayer().gainMeso(-500000, true, true);
            }
            sendNext("증가되었습니다.");
        } else if (!trueMax) {
            sendNext("이미 한계치입니다. (Limit: 100)");
        } else {
            sendNext("이미 한계치입니다. (Limit: 200)");
        }
    }

    public void displayGuildRanks() {
        c.getSession().writeAndFlush(GuildPacket.guildRankingRequest());
    }

    public boolean removePlayerFromInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            c.getPlayer().getEventInstance().removePlayer(c.getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            return true;
        }
        return false;
    }

    public void changeStat(byte slot, int type, int amount) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr((short) amount);
                break;
            case 1:
                sel.setDex((short) amount);
                break;
            case 2:
                sel.setInt((short) amount);
                break;
            case 3:
                sel.setLuk((short) amount);
                break;
            case 4:
                sel.setHp((short) amount);
                break;
            case 5:
                sel.setMp((short) amount);
                break;
            case 6:
                sel.setWatk((short) amount);
                break;
            case 7:
                sel.setMatk((short) amount);
                break;
            case 8:
                sel.setWdef((short) amount);
                break;
            case 9:
                sel.setMdef((short) amount);
                break;
            case 10:
                sel.setAcc((short) amount);
                break;
            case 11:
                sel.setAvoid((short) amount);
                break;
            case 12:
                sel.setHands((short) amount);
                break;
            case 13:
                sel.setSpeed((short) amount);
                break;
            case 14:
                sel.setJump((short) amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            case 18:
                sel.setEnhance((byte) amount);
                break;
            case 19:
                sel.setPotential1(amount);
                break;
            case 20:
                sel.setPotential2(amount);
                break;
            case 21:
                sel.setPotential3(amount);
                break;
            case 22:
                sel.setPotential4(amount);
                break;
            case 23:
                sel.setPotential5(amount);
                break;
            case 24:
                sel.setOwner(getText());
                break;
            default:
                break;
        }
        c.getPlayer().equipChanged();
        c.getPlayer().fakeRelog();
    }

    public String searchCashItem(String t) {
        Pattern name2Pattern = Pattern.compile("^[가-힣a-zA-Z0-9]*$");
        if (!name2Pattern.matcher(t).matches()) {
            return "검색할 수 없는 아이템입니다.";
        }
        StringBuilder sb = new StringBuilder();
        for (Pair<Integer, String> item : MapleItemInformationProvider.getInstance().getAllEquips()) {

            if (item.right.contains(t)) {
                if (MapleItemInformationProvider.getInstance().isCash(item.left)) {
                    sb.append("#b#L" + item.left + "# #i" + item.left + "##t" + item.left + "##l\r\n");
                }
            }
        } // 최혁 병신 ㅋㅋ
        return sb.toString();
    }

    public void changeDamageSkin(int skinnum) {
        MapleQuest quest = MapleQuest.getInstance(7291);
        MapleQuestStatus queststatus = new MapleQuestStatus(quest, (byte) 1);
        String skinString = String.valueOf(skinnum);
        queststatus.setCustomData(skinString == null ? "0" : skinString);
        getPlayer().updateQuest(queststatus, true);
        getPlayer().dropMessage(5, "데미지 스킨이 변경되었습니다.");
        getPlayer().getMap().broadcastMessage(getPlayer(), CField.showForeignDamageSkin(getPlayer(), skinnum), false);
    }

    public void openDuey() {
        c.getPlayer().setConversation(2);
        c.getSession().writeAndFlush(CField.sendDuey((byte) 9, null, null));
    }

    public void openMerchantItemStore() {
        c.getPlayer().setConversation(3);
        HiredMerchantHandler.displayMerch(c);
        //c.getSession().writeAndFlush(PlayerShopPacket.merchItemStore((byte) 0x22));
        //c.getPlayer().dropMessage(5, "Please enter ANY 13 characters.");
    }

    public void sendUI(int op) {
        c.getSession().writeAndFlush(UIPacket.openUI(op));
    }

    public void sendUIJobChange() {
        if (c.getPlayer().getJob() / 1000 != 0) {
            c.getPlayer().dropMessage(1, "모험가 직업군만 자유전직이 가능합니다.");
            return;
        }
        Equip test2 = null;
        if (c.getPlayer().getJob() / 100 == 4) {
            test2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
        }
        if (test2 == null) {
            c.getSession().writeAndFlush(UIPacket.openUI(164)); //<- UI번호 따기
        } else {
            c.getPlayer().dropMessage(1, "도적 직업군은 보조무기/방패/블레이드를 해제하셔야 합니다.");
            return;
        }
    }

    public void sendRepairWindow() {
        c.getSession().writeAndFlush(UIPacket.openUIOption(33, id));
    }

    public void sendNameChangeWindow() {
        c.getSession().writeAndFlush(UIPacket.openUIOption(1110, 4034803));
    }

    public void sendProfessionWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(42));
    }

    public final int getDojoPoints() {
        return dojo_getPts();
    }

    public final int getDojoRecord() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO_RECORD);
    }

    public void setDojoRecord(final boolean reset) {
        if (reset) {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData("0");
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData("0");
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData(String.valueOf(c.getPlayer().getIntRecord(GameConstants.DOJO_RECORD) + 1));
        }
    }

    public boolean start_DojoAgent(final boolean dojo, final boolean party) {
        if (dojo) {
            return Event_DojoAgent.warpStartDojo(c.getPlayer(), party);
        }
        return Event_DojoAgent.warpStartAgent(c.getPlayer(), party);
    }

    public final short getKegs() {
        return c.getChannelServer().getFireWorks().getKegsPercentage();
    }

    public void giveKegs(final int kegs) {
        c.getChannelServer().getFireWorks().giveKegs(c.getPlayer(), kegs);
    }

    public final short getSunshines() {
        return c.getChannelServer().getFireWorks().getSunsPercentage();
    }

    public void addSunshines(final int kegs) {
        c.getChannelServer().getFireWorks().giveSuns(c.getPlayer(), kegs);
    }

    public final short getDecorations() {
        return c.getChannelServer().getFireWorks().getDecsPercentage();
    }

    public void addDecorations(final int kegs) {
        try {
            c.getChannelServer().getFireWorks().giveDecs(c.getPlayer(), kegs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final MapleCarnivalParty getCarnivalParty() {
        return c.getPlayer().getCarnivalParty();
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return c.getPlayer().getNextCarnivalRequest();
    }

    public final MapleCarnivalChallenge getCarnivalChallenge(MapleCharacter chr) {
        return new MapleCarnivalChallenge(chr);
    }

    public void maxStats() {
        Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);
        c.getPlayer().getStat().str = (short) 32767;
        c.getPlayer().getStat().dex = (short) 32767;
        c.getPlayer().getStat().int_ = (short) 32767;
        c.getPlayer().getStat().luk = (short) 32767;

        int overrDemon = GameConstants.isDemonSlayer(c.getPlayer().getJob()) ? GameConstants.getMPByJob(c.getPlayer()) : 500000;
        c.getPlayer().getStat().maxhp = 500000;
        c.getPlayer().getStat().maxmp = overrDemon;
        c.getPlayer().getStat().setHp(500000, c.getPlayer());
        c.getPlayer().getStat().setMp(overrDemon, c.getPlayer());

        statup.put(MapleStat.STR, Long.valueOf(32767));
        statup.put(MapleStat.DEX, Long.valueOf(32767));
        statup.put(MapleStat.LUK, Long.valueOf(32767));
        statup.put(MapleStat.INT, Long.valueOf(32767));
        statup.put(MapleStat.HP, Long.valueOf(500000));
        statup.put(MapleStat.MAXHP, Long.valueOf(500000));
        statup.put(MapleStat.MP, Long.valueOf(overrDemon));
        statup.put(MapleStat.MAXMP, Long.valueOf(overrDemon));
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
        c.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, c.getPlayer()));
    }

    public Triple<String, Map<Integer, String>, Long> getSpeedRun(String typ) {
        final ExpeditionType type = ExpeditionType.valueOf(typ);
        if (SpeedRunner.getSpeedRunData(type) != null) {
            return SpeedRunner.getSpeedRunData(type);
        }
        return new Triple<String, Map<Integer, String>, Long>("", new HashMap<Integer, String>(), 0L);
    }

    public boolean getSR(Triple<String, Map<Integer, String>, Long> ma, int sel) {
        if (ma.mid.get(sel) == null || ma.mid.get(sel).length() <= 0) {
            dispose();
            return false;
        }
        sendOk(ma.mid.get(sel));
        return true;
    }

    public String getAllItem() {
        StringBuilder string = new StringBuilder();
        for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP).list()) {
            string.append("#L" + item.getUniqueId() + "##i " + item.getItemId() + "#\r\n");
        }
        return string.toString();
    }

    public Equip getEquip(int itemid) {
        return (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
    }

    public void setExpiration(Object statsSel, long expire) {
        if (statsSel instanceof Equip) {
            ((Equip) statsSel).setExpiration(System.currentTimeMillis() + (expire * 24 * 60 * 60 * 1000));
        }
    }

    public void setLock(Object statsSel) {
        if (statsSel instanceof Equip) {
            Equip eq = (Equip) statsSel;
            if (eq.getExpiration() == -1) {
                eq.setFlag((eq.getFlag() | ItemFlag.LOCK.getValue()));
            } else {
                eq.setFlag((eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
            }
        }
    }

    public boolean addFromDrop(Object statsSel) {
        if (statsSel instanceof Item) {
            final Item it = (Item) statsSel;
            return MapleInventoryManipulator.checkSpace(getClient(), it.getItemId(), it.getQuantity(), it.getOwner()) && MapleInventoryManipulator.addFromDrop(getClient(), it, false);
        }
        return false;
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type) {
        return replaceItem(slot, invType, statsSel, offset, type, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type, boolean takeSlot) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        Item item = getPlayer().getInventory(inv).getItem((short) slot);
        if (item == null || statsSel instanceof Item) {
            item = (Item) statsSel;
        }
        if (offset > 0) {
            if (inv != MapleInventoryType.EQUIP) {
                return false;
            }
            Equip eq = (Equip) item;
            if (takeSlot) {
                if (eq.getUpgradeSlots() < 1) {
                    return false;
                } else {
                    eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() - 1));
                }
                if (eq.getExpiration() == -1) {
                    eq.setFlag((eq.getFlag() | ItemFlag.LOCK.getValue()));
                } else {
                    eq.setFlag((eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
                }
            }
            if (type.equalsIgnoreCase("Slots")) {
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + offset));
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("Level")) {
                eq.setLevel((byte) (eq.getLevel() + offset));
            } else if (type.equalsIgnoreCase("Hammer")) {
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("STR")) {
                eq.setStr((short) (eq.getStr() + offset));
            } else if (type.equalsIgnoreCase("DEX")) {
                eq.setDex((short) (eq.getDex() + offset));
            } else if (type.equalsIgnoreCase("INT")) {
                eq.setInt((short) (eq.getInt() + offset));
            } else if (type.equalsIgnoreCase("LUK")) {
                eq.setLuk((short) (eq.getLuk() + offset));
            } else if (type.equalsIgnoreCase("HP")) {
                eq.setHp((short) (eq.getHp() + offset));
            } else if (type.equalsIgnoreCase("MP")) {
                eq.setMp((short) (eq.getMp() + offset));
            } else if (type.equalsIgnoreCase("WATK")) {
                eq.setWatk((short) (eq.getWatk() + offset));
            } else if (type.equalsIgnoreCase("MATK")) {
                eq.setMatk((short) (eq.getMatk() + offset));
            } else if (type.equalsIgnoreCase("WDEF")) {
                eq.setWdef((short) (eq.getWdef() + offset));
            } else if (type.equalsIgnoreCase("MDEF")) {
                eq.setMdef((short) (eq.getMdef() + offset));
            } else if (type.equalsIgnoreCase("ACC")) {
                eq.setAcc((short) (eq.getAcc() + offset));
            } else if (type.equalsIgnoreCase("Avoid")) {
                eq.setAvoid((short) (eq.getAvoid() + offset));
            } else if (type.equalsIgnoreCase("Hands")) {
                eq.setHands((short) (eq.getHands() + offset));
            } else if (type.equalsIgnoreCase("Speed")) {
                eq.setSpeed((short) (eq.getSpeed() + offset));
            } else if (type.equalsIgnoreCase("Jump")) {
                eq.setJump((short) (eq.getJump() + offset));
            } else if (type.equalsIgnoreCase("ItemEXP")) {
                eq.setItemEXP(eq.getItemEXP() + offset);
            } else if (type.equalsIgnoreCase("Expiration")) {
                eq.setExpiration((long) (eq.getExpiration() + offset));
            } else if (type.equalsIgnoreCase("Flag")) {
                eq.setFlag((eq.getFlag() + offset));
            }
            item = eq.copy();
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, item.getQuantity(), false);
        return MapleInventoryManipulator.addFromDrop(getClient(), item, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int upgradeSlots) {
        return replaceItem(slot, invType, statsSel, upgradeSlots, "Slots");
    }

    public boolean isCash(final int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public int getTotalStat(final int itemId) {
        return MapleItemInformationProvider.getInstance().getTotalStat((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
    }

    public int getReqLevel(final int itemId) {
        return MapleItemInformationProvider.getInstance().getReqLevel(itemId);
    }

    public MapleStatEffect getEffect(int buff) {
        return MapleItemInformationProvider.getInstance().getItemEffect(buff);
    }

    public void buffGuild(final int buff, final int duration, final String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getItemEffect(buff) != null && getPlayer().getGuildId() > 0) {
            final MapleStatEffect mse = ii.getItemEffect(buff);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters().values()) {
                    if (chr.getGuildId() == getPlayer().getGuildId()) {
                        mse.applyTo(chr, chr, true, chr.getTruePosition(), duration, (byte) 0, true);
                        chr.dropMessage(5, "Your guild has gotten a " + msg + " buff.");
                    }
                }
            }
        }
    }

    public long getRemainPremium(int accid) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        long ret = 0;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM premium WHERE accid = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = rs.getLong("period");
            }

            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
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
        return ret;
    }

    public boolean existPremium(int aci) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        boolean ret = false;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM premium WHERE accid = ?");
            ps.setInt(1, aci);
            rs = ps.executeQuery();
            ret = rs.next();

            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
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
        return ret;
    }

    public void gainAllAccountPremium(int v3, int v4) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList<Integer> chrs = new ArrayList<>();
        Date adate = new Date();
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts");
            rs = ps.executeQuery();
            while (rs.next()) {
                chrs.add(rs.getInt("id"));
            }
            rs.close();
            ps.close();

            for (int i = 0; i < chrs.size(); i++) {
                if (existPremium(chrs.get(i))) {
                    if (getRemainPremium(chrs.get(i)) > adate.getTime()) {
                        ps = con.prepareStatement("UPDATE premium SET period = ? WHERE accid = ?");
                        ps.setLong(1, getRemainPremium(chrs.get(i)) + (v3 * 24 * 60 * 60 * 1000));
                        ps.setInt(2, chrs.get(i));
                        ps.executeUpdate();

                        ps.close();
                    } else {
                        ps = con.prepareStatement("UPDATE premium SET period = ? and `name` = ? and `buff` = ? WHERE accid = ?");
                        ps.setLong(1, adate.getTime() + (v3 * 24 * 60 * 60 * 1000));
                        ps.setString(2, "일반");
                        ps.setInt(3, 80001535);
                        ps.setInt(4, chrs.get(i));
                        ps.executeUpdate();

                        ps.close();
                    }
                } else {

                    ps = con.prepareStatement("INSERT INTO premium(accid, name, buff, period) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, chrs.get(i));
                    ps.setString(2, "일반");
                    ps.setInt(3, 80001535);
                    ps.setLong(4, adate.getTime() + (v3 * 24 * 60 * 60 * 1000));
                    ps.executeUpdate();
                    ps.close();
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
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
    }

    public void gainAccountPremium(String acc, int v3, boolean v4) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Date adate = new Date();
        int accid = 0;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, acc);
            rs = ps.executeQuery();
            if (rs.next()) {
                accid = rs.getInt("id");
            }
            rs.close();
            ps.close();

            if (existPremium(accid)) {
                if (getRemainPremium(accid) > adate.getTime()) {
                    ps = con.prepareStatement("UPDATE premium SET period = ? WHERE accid = ?");
                    if (v4) {
                        ps.setLong(1, getRemainPremium(accid) + (v3 * 24 * 60 * 60 * 1000));
                    } else {
                        ps.setLong(1, getRemainPremium(accid) - (v3 * 24 * 60 * 60 * 1000));
                    }
                    ps.setInt(2, accid);
                    ps.executeUpdate();

                    ps.close();
                } else {
                    if (v4) {
                        ps = con.prepareStatement("UPDATE premium SET period = ? and `name` = ? and `buff` = ? WHERE accid = ?");
                        ps.setLong(1, adate.getTime() + (v3 * 24 * 60 * 60 * 1000));
                        ps.setString(2, "일반");
                        ps.setInt(3, 80001535);
                        ps.setInt(4, accid);
                        ps.executeUpdate();

                        ps.close();
                    }
                }
            } else {
                if (v4) {
                    ps = con.prepareStatement("INSERT INTO premium(accid, name, buff, period) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, accid);
                    ps.setString(2, "일반");
                    ps.setInt(3, 80001535);
                    ps.setLong(4, adate.getTime() + (v3 * 24 * 60 * 60 * 1000));
                    ps.executeUpdate();
                    ps.close();
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
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
    }

    public boolean createAlliance(String alliancename) {
        MapleParty pt = c.getPlayer().getParty();
        MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(pt.getMemberByIndex(1).getId());
        if (otherChar == null || otherChar.getId() == c.getPlayer().getId()) {
            return false;
        }
        try {
            return World.Alliance.createAlliance(alliancename, c.getPlayer().getId(), otherChar.getId(), c.getPlayer().getGuildId(), otherChar.getGuildId());
        } catch (Exception re) {
            re.printStackTrace();
            return false;
        }
    }

    public boolean addCapacityToAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.changeAllianceCapacity(gs.getAllianceId())) {
                    gainMeso(-MapleGuildAlliance.CHANGE_CAPACITY_COST);
                    return true;
                }
            }
        } catch (Exception re) {
            re.printStackTrace();
        }
        return false;
    }

    public boolean disbandAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.disbandAlliance(gs.getAllianceId())) {
                    return true;
                }
            }
        } catch (Exception re) {
            re.printStackTrace();
        }
        return false;
    }

    public byte getLastMsg() {
        return lastMsg;
    }

    public final void setLastMsg(final byte last) {
        this.lastMsg = last;
    }

    public final void maxAllSkills() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId()) && skil.getId() < 90000000) { //no db/additionals/resistance skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void maxSkillsByJob() {
        c.getPlayer().SkillMasterByJob();
    }

    public final void resetStats(int str, int dex, int z, int luk) {
        c.getPlayer().resetStats(str, dex, z, luk);
    }

    public final boolean dropItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        return MapleInventoryManipulator.drop(c, inv, (short) slot, (short) quantity, true);
    }

    public final void setQuestRecord(Object ch, final int questid, final String data) {
        ((MapleCharacter) ch).getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(data);
    }

    public final void doWeddingEffect(final Object ch) {
        final MapleCharacter chr = (MapleCharacter) ch;
        final MapleCharacter player = getPlayer();
        getMap().broadcastMessage(CWvsContext.yellowChat(player.getName() + ", do you take " + chr.getName() + " as your wife and promise to stay beside her through all downtimes, crashes, and lags?"));
        CloneTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (chr == null || player == null) {
                    warpMap(680000500, 0);
                } else {
                    chr.getMap().broadcastMessage(CWvsContext.yellowChat(chr.getName() + ", do you take " + player.getName() + " as your husband and promise to stay beside him through all downtimes, crashes, and lags?"));
                }
            }
        }, 10000);
        CloneTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (chr == null || player == null) {
                    if (player != null) {
                        setQuestRecord(player, 160001, "3");
                        setQuestRecord(player, 160002, "0");
                    } else if (chr != null) {
                        setQuestRecord(chr, 160001, "3");
                        setQuestRecord(chr, 160002, "0");
                    }
                    warpMap(680000500, 0);
                } else {
                    setQuestRecord(player, 160001, "2");
                    setQuestRecord(chr, 160001, "2");
                    sendNPCText(player.getName() + " and " + chr.getName() + ", I wish you two all the best on your " + chr.getClient().getChannelServer().getServerName() + " journey together!", 9201002);
                    chr.getMap().startExtendedMapEffect("You may now kiss the bride, " + player.getName() + "!", 5120006);
                    if (chr.getGuildId() > 0) {
                        World.Guild.guildPacket(chr.getGuildId(), CWvsContext.sendMarriage(false, chr.getName()));
                    }
                    if (player.getGuildId() > 0) {
                        World.Guild.guildPacket(player.getGuildId(), CWvsContext.sendMarriage(false, player.getName()));
                    }
                }
            }
        }, 20000); //10 sec 10 sec

    }

    public void putKey(int key, int type, int action) {
        getPlayer().changeKeybinding(key, (byte) type, action);
        getClient().getSession().writeAndFlush(CField.getKeymap(getPlayer().getKeyLayout()));
    }

    public void logDonator(String log, int previous_points) {
        final StringBuilder logg = new StringBuilder();
        logg.append(MapleCharacterUtil.makeMapleReadable(getPlayer().getName()));
        logg.append(" [CID: ").append(getPlayer().getId()).append("] ");
        logg.append(" [Account: ").append(MapleCharacterUtil.makeMapleReadable(getClient().getAccountName())).append("] ");
        logg.append(log);
        logg.append(" [Previous: " + previous_points + "] [Now: " + getPlayer().getPoints() + "]");

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO donorlog VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, MapleCharacterUtil.makeMapleReadable(getClient().getAccountName()));
            ps.setInt(2, getClient().getAccID());
            ps.setString(3, MapleCharacterUtil.makeMapleReadable(getPlayer().getName()));
            ps.setInt(4, getPlayer().getId());
            ps.setString(5, log);
            ps.setString(6, FileoutputUtil.CurrentReadable_Time());
            ps.setInt(7, previous_points);
            ps.setInt(8, getPlayer().getPoints());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        FileoutputUtil.log(FileoutputUtil.Donator_Log, logg.toString());
    }

    public void doRing(final String name, final int itemid) {
        PlayersHandler.DoRing(getClient(), name, itemid);
    }

    public int getNaturalStats(final int itemid, final String it) {
        Map<String, Integer> eqStats = MapleItemInformationProvider.getInstance().getEquipStats(itemid);
        if (eqStats != null && eqStats.containsKey(it)) {
            return eqStats.get(it);
        }
        return 0;
    }

    public boolean isEligibleName(String t) {
        return MapleCharacterUtil.canCreateChar(t, getPlayer().isGM()) && (!LoginInformationProvider.getInstance().isForbiddenName(t) || getPlayer().isGM());
    }

    public String checkDrop(int mobId) {
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId = 0, ch = 0;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (num == 0) {
                        name.append("Drops for #o" + mobId + "#\r\n");
                        name.append("--------------------------------------\r\n");
                    }
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        namez = (de.Minimum * getClient().getChannelServer().getMesoRate()) + " to " + (de.Maximum * getClient().getChannelServer().getMesoRate()) + " meso";
                    }
                    ch = de.chance * getClient().getChannelServer().getDropRate();
                    name.append((num + 1) + ") #v" + itemId + "#" + namez + " - " + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "% chance. " + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("Requires quest " + MapleQuest.getInstance(de.questid).getName() + " to be started.") : "") + "\r\n");
                    num++;
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }

        }
        return "No drops was returned.";
    }

    public String getLeftPadded(final String in, final char padchar, final int length) {
        return StringUtil.getLeftPaddedStr(in, padchar, length);
    }

    public void handleDivorce() {
        if (getPlayer().getMarriageId() <= 0) {
            sendNext("Please make sure you have a marriage.");
            return;
        }
        final int chz = World.Find.findChannel(getPlayer().getMarriageId());
        if (chz == -1) {
            //sql queries
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("UPDATE queststatus SET customData = ? WHERE characterid = ? AND (quest = ? OR quest = ?)");
                ps.setString(1, "0");
                ps.setInt(2, getPlayer().getMarriageId());
                ps.setInt(3, 160001);
                ps.setInt(4, 160002);
                ps.executeUpdate();
                ps.close();

                ps = con.prepareStatement("UPDATE characters SET marriageid = ? WHERE id = ?");
                ps.setInt(1, 0);
                ps.setInt(2, getPlayer().getMarriageId());
                ps.executeUpdate();
                ps.close();
                con.close();
            } catch (SQLException e) {
                outputFileError(e);
                return;
            }
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("You have been successfully divorced...");
            return;
        } else if (chz < -1) {
            sendNext("Please make sure your partner is logged on.");
            return;
        }
        MapleCharacter cPlayer = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterById(getPlayer().getMarriageId());
        if (cPlayer != null) {
            cPlayer.dropMessage(1, "Your partner has divorced you.");
            cPlayer.setMarriageId(0);
            setQuestRecord(cPlayer, 160001, "0");
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(cPlayer, 160002, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("You have been successfully divorced...");
        } else {
            sendNext("An error occurred...");
        }
    }

    public String getReadableMillis(long startMillis, long endMillis) {
        return StringUtil.getReadableMillis(startMillis, endMillis);
    }

    public void sendUltimateExplorer() {
        getClient().getSession().writeAndFlush(CWvsContext.ultimateExplorer());
    }

    public void sendPendant(boolean b) {
        c.getSession().writeAndFlush(CWvsContext.pendantSlot(b));
    }

    public int getCompensation(String id) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM compensationlog_confirmed WHERE chrname = ?");
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("value");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e);
        }
        return 0;
    }

    public boolean deleteCompensation(String id) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM compensationlog_confirmed WHERE chrname = ?");
            ps.setString(1, id);
            ps.executeUpdate();
            ps.close();
            con.close();
            return true;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e);
            return false;
        }
    }

    /*Start of Custom Features*/
    public void gainAPS(int gain) {
        getPlayer().gainAPS(gain);
    }

    public void forceCompleteQuest(MapleCharacter chr, final int idd) {
        MapleQuest.getInstance(idd).forceComplete(chr, getNpc());
    }

    public void setInnerStats(MapleCharacter chr, int line) {

        InnerSkillValueHolder isvh = InnerAbillity.getInstance().renewSkill(0, false);
        chr.getInnerSkills().add(isvh);
        chr.changeSkillLevel(SkillFactory.getSkill(isvh.getSkillId()), isvh.getSkillLevel(), isvh.getSkillLevel());
        chr.getClient().getSession().writeAndFlush(CField.updateInnerPotential((byte) line, isvh.getSkillId(), isvh.getSkillLevel(), isvh.getRank()));
    }

    /*End of Custom Features*/
    public void setInnerStats(int line) {
        InnerSkillValueHolder isvh = InnerAbillity.getInstance().renewSkill(0, false);
        c.getPlayer().getInnerSkills().add(isvh);
        c.getPlayer().changeSkillLevel(SkillFactory.getSkill(isvh.getSkillId()), isvh.getSkillLevel(), isvh.getSkillLevel());
        c.getSession().writeAndFlush(CField.updateInnerPotential((byte) line, isvh.getSkillId(), isvh.getSkillLevel(), isvh.getRank()));
    }

    public void openAuctionUI() {
        c.getSession().writeAndFlush(UIPacket.openUI(0xA1));
    }

    public void gainSponserItem(int item, final String name, short allstat, short damage, byte upgradeslot) {
        if (GameConstants.isEquip(item)) {
            Equip Item = (Equip) MapleItemInformationProvider.getInstance().getEquipById(item);
            Item.setOwner(name);
            Item.setStr(allstat);
            Item.setDex(allstat);
            Item.setInt(allstat);
            Item.setLuk(allstat);
            Item.setWatk(damage);
            Item.setMatk(damage);
            Item.setUpgradeSlots(upgradeslot);
            MapleInventoryManipulator.addFromDrop(c, Item, false);
        } else {
            gainItem(item, allstat, damage);
        }
    }

    public void askAvatar(String text, List<Integer> args) {
        c.getSession().writeAndFlush(NPCPacket.getNPCTalkStyle(id, text, args));
        lastMsg = 9;
    }

    public void SearchItem(String text, int type) {
        NPCConversationManager cm = this;
        if (text.getBytes().length < 4) {
            cm.sendOk("검색어는 두글자 이상으로 해주세요.");
            cm.dispose();
            return;
        }
        if (text.contains("헤어") || text.contains("얼굴")) {
            cm.sendOk("헤어, 얼굴 단어는 생략하고 검색해주세요.");
            cm.dispose();
            return;
        }
        String kk = "";
        String chat = "";
        String nchat = "";
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int i = 0;
        for (Pair<Integer, String> item : ii.getAllEquips()) {
            if (item.getRight().toLowerCase().contains(text.toLowerCase())) {
                String color = "#b";
                String isuse = "";
                if (cm.getPlayer().getCashWishList().contains(item.getLeft())) {
                    color = "#Cgray#";
                    isuse = " (선택된 항목)";
                }
                if (type == 1 && ii.isCash(item.getLeft()) && item.getLeft() >= 1000000 && item.getLeft() / 1000000 == 1) {
                    chat += "\r\n" + color + "#L" + item.getLeft() + "##i" + item.getLeft() + " ##z" + item.getLeft() + "#" + isuse;
                    i++;
                } else if (type == 0 && (item.getLeft() / 10000 >= 2 && item.getLeft() / 10000 < 3)) {
                    chat += "\r\n" + color + "#L" + item.getLeft() + "##i" + item.getLeft() + " ##z" + item.getLeft() + "#" + isuse;
                    i++;
                } else if (type == 2 && (item.getLeft() / 10000 >= 3 && item.getLeft() / 10000 <= 5)) {
                    chat += "\r\n" + color + "#L" + item.getLeft() + "##i" + item.getLeft() + " ##z" + item.getLeft() + "#" + isuse;
                    i++;
                }
            }
        }
        if (i != 0) {
            kk += "총 " + i + "개 검색되었습니다. 추가 하실 항목을 선택해주세요.";
            kk += "\r\n#L0#항목 선택을 마칩니다.  \r\n#L1#항목을 재검색합니다.";
            nchat = kk + chat;
            cm.sendSimple(nchat);
        } else {
            kk += "검색된 아이템이 없습니다.";
            cm.sendOk(kk);
            cm.dispose();
        }
    }

    public void sendPacket(String args) {
        c.getSession().writeAndFlush(PacketHelper.sendPacket(args));
    }

    public void enableMatrix() {
        final MapleQuest quest = MapleQuest.getInstance(1465);
        MapleQuestStatus qs = c.getPlayer().getQuest(quest);
        if (quest != null && qs.getStatus() != (byte) 2) {
            qs.setStatus((byte) 2);
            c.getPlayer().updateQuest(c.getPlayer().getQuest(quest), true);
        }
    }

    public void gainCorebit(int g) {
        getPlayer().setKeyValue(1477, "count", String.valueOf(getPlayer().getKeyValue(1477, "count") + g)); // 10
    }

    public long getCorebit() {
        return getPlayer().getKeyValue(1477, "count");
    }

    public void setDeathcount(byte de) {
        c.getPlayer().setDeathCount(de);
        c.getSession().writeAndFlush(CField.getDeathCount(de));
    }

    public void UserSoulHandle(int selection) {
        for (List<Pair<Integer, MapleCharacter>> souls : c.getChannelServer().getSoulmatch()) {
            c.getPlayer().dropMessageGM(6, "1");
            if (souls.size() == 1 && souls.get(0).left == 0 && selection == 0) { // 캐릭터가 1개고, 레디중이 아니며, 팀 매칭을 선택했을경우.
                souls.add(new Pair<>(selection, c.getPlayer()));
                c.getPlayer().dropMessageGM(6, "2 : " + souls.size());
                c.getSession().writeAndFlush(CWvsContext.onUserSoulMatching(selection, souls));
                return;
            }
        }
        c.getPlayer().dropMessageGM(6, "3");
        List<Pair<Integer, MapleCharacter>> chrs = new ArrayList<>();
        chrs.add(new Pair<>(selection, c.getPlayer()));
        c.getSession().writeAndFlush(CWvsContext.onUserSoulMatching(selection, chrs));
        if (selection == 0) { // 팀 매칭일경우 추가.
            c.getPlayer().dropMessageGM(6, "4");
            c.getChannelServer().getSoulmatch().add(chrs);
        }
    }

    public void startExpRate(int hour) {
        c.getSession().writeAndFlush(CField.getClock(hour * 60 * 60));
        ExpRating();
        //시간 끝나면 이동.
        Timer.MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                warp(1000000);
            }
        }, hour * 60 * 60 * 1000);
    }

    public void ExpRating() {
        Timer.BuffTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (c.getPlayer().getMapId() == 925080000) { // 분당 10%
                    c.getPlayer().gainExp(GameConstants.getExpNeededForLevel(c.getPlayer().getLevel()) / 100, true, false, false);
                    ExpRating();
                } else {
                    stopExpRate();
                }
            }
        }, 6 * 1000);
    }

    public void stopExpRate() {
        c.getSession().writeAndFlush(CField.getClock(-1));
    }

    public int getFrozenMobCount() {
        return getPlayer().getLinkMobCount();
    }

    public void addFrozenMobCount(int a1) {
        int val = (getFrozenMobCount() + a1) > 9999 ? 9999 : getFrozenMobCount() + a1;
        getPlayer().setLinkMobCount(val);
        getClient().getSession().writeAndFlush(SLFCGPacket.FrozenLinkMobCount(val));
        getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(1052230, 3500, "#face1# 몬스터수를 충전했어!", ""));
    }

    public long getStarDustCoin() {
        return getPlayer().getKeyValue(100592, "point");
    }

    public void addStarDustCoin(int a) {
        getPlayer().AddStarDustCoin(a);
    }

    public void openWeddingPresent(int type, int gender) {
        MarriageDataEntry dataEntry = getMarriageAgent().getDataEntry();
        if (dataEntry != null) {
            if (type == 1) { // give
                c.getPlayer().setWeddingGive(gender);
                List<String> wishes;
                if (gender == 0) {
                    wishes = dataEntry.getGroomWishList();
                } else {
                    wishes = dataEntry.getBrideWishList();
                }
                c.getSession().writeAndFlush(CWvsContext.showWeddingWishGiveDialog(wishes));
            } else if (type == 2) { // recv
                List<Item> gifts;
                if (gender == 0) {
                    gifts = dataEntry.getGroomPresentList();
                } else {
                    gifts = dataEntry.getBridePresentList();
                }
                c.getSession().writeAndFlush(CWvsContext.showWeddingWishRecvDialog(gifts));
            }
        }
    }

    public void ShowDreamBreakerRanking() {
        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerRanking(c.getPlayer().getName()));
    }

    public String getItemNameById(int itemid) {
        String itemname = "";
        for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
            if (itemPair.getLeft() == itemid) {
                itemname = itemPair.getRight();
            }
        }
        return itemname;
    }

    /*    public void enterAuction() {
     AuctionHandler.EnterAuction(c.getPlayer(), c);
     }*/
    public long getFWolfMeso() {
        if (c.getPlayer().getFWolfAttackCount() > 15) {
            long BaseMeso = 10_000_000L;
            long FWolfMeso = 0L;

            if (c.getPlayer().getFWolfDamage() >= 900_000_000_000L) {
                FWolfMeso = BaseMeso * 100L;
            } else {
                float ratio = (900000000000L / c.getPlayer().getFWolfDamage()) * 100;
                FWolfMeso = (long) (BaseMeso * ratio);
            }
            return FWolfMeso;
        } else {
            return 100000 * c.getPlayer().getFWolfAttackCount();
        }
    }

    public long getFWolfEXP() {
        long expneed = GameConstants.getExpNeededForLevel(c.getPlayer().getLevel());
        long exp = 0;
        if (c.getPlayer().getFWolfDamage() >= 75000000000L * 500) {
            exp = (long) (expneed * 0.25);
        } else if (c.getPlayer().getFWolfDamage() >= 12500000000L * 500) {
            exp = (long) (expneed * 0.20);
        } else if (c.getPlayer().getFWolfDamage() >= 1250000000L * 500) {
            exp = (long) (expneed * 0.15);
        } else {
            exp = (long) (expneed * 0.10);
        }

        if (c.getPlayer().isFWolfKiller()) {
            exp = (long) (expneed * 0.5);
        }
        return exp;
    }

    public void gainItemAllStat(int itemid, short quantity, short allstat) {
        gainItemAllStat(itemid, quantity, allstat, (short) -1);
    }

    public void gainItemAllStat(int itemid, short quantity, short allstat, short wmtk) {
        Equip equip = new Equip(itemid, quantity, (byte) 0);
        equip.setStr(allstat);
        equip.setDex(allstat);
        equip.setInt(allstat);
        equip.setLuk(allstat);
        if (wmtk != -1) {
            equip.setWatk(wmtk);
            equip.setMatk(wmtk);
        }
        MapleInventoryManipulator.addFromDrop(c, equip, true);
    }

    public void gainItemAllStat(int itemid, short quantity, short allstat, short wmtk, int up) {
        Equip equip = new Equip(itemid, quantity, (byte) 0);
        equip.setStr(allstat);
        equip.setDex(allstat);
        equip.setInt(allstat);
        equip.setLuk(allstat);
        equip.setUpgradeSlots((byte) up);
        if (wmtk != -1) {
            equip.setWatk(wmtk);
            equip.setMatk(wmtk);
        }
        MapleInventoryManipulator.addFromDrop(c, equip, true);
    }

    public void gainItemAllStat(int itemid, short quantity, short allstat, short wmtk, byte ups, byte level) {
        Equip equip = new Equip(itemid, quantity, (byte) 0);
        equip.setStr(allstat);
        equip.setDex(allstat);
        equip.setInt(allstat);
        equip.setLuk(allstat);
        if (ups != -1) {
            equip.setUpgradeSlots(ups);
        }
        if (wmtk != -1) {
            equip.setWatk(wmtk);
            equip.setMatk(wmtk);
        }
        if (level != -1) {
            equip.setLevel((byte) (equip.getLevel() - level));
        }
        MapleInventoryManipulator.addFromDrop(c, equip, true);
    }

    public void gainItemAllStat(int itemid, short quantity, short allstat, short wmtk, byte ups, byte level, byte ign,
            byte boss) {
        Equip equip = new Equip(itemid, quantity, (byte) 0);
        equip.setStr(allstat);
        equip.setDex(allstat);
        equip.setInt(allstat);
        equip.setLuk(allstat);
        if (ign != -1) {
            equip.setIgnorePDR(ign);
        }
        if (boss != -1) {
            equip.setBossDamage(boss);
        }
        if (ups != -1) {
            equip.setUpgradeSlots(ups);
        }
        if (wmtk != -1) {
            equip.setWatk(wmtk);
            equip.setMatk(wmtk);
        }
        if (level != -1) {
            equip.setLevel((byte) (equip.getLevel() - level));
        }
        MapleInventoryManipulator.addFromDrop(c, equip, true);
    }

    public void showDimentionMirror() {
        c.getSession().writeAndFlush(CField.dimentionMirror(ServerConstants.mirrors));
    }

    public void warpNettPyramid(boolean hard) {
        MapleNettPyramid.warpNettPyramid(c.getPlayer(), hard);
    }

    public void startDamageMeter() {
        c.getPlayer().setDamageMeter(0);
        MapleMap map = c.getChannelServer().getMapFactory().getMap(120000102);
        map.killAllMonsters(false);
        warp(120000102);
        c.getSession().writeAndFlush(CField.getClock(30));
        c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9063152, 3000, "20초에 허수아비가 소환되고 측정이 시작됩니다.", ""));

        MapleMonster mob = MapleLifeFactory.getMonster(9305653);
        Timer.MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                map.spawnMonsterOnGroundBelow(mob, new Point(-140, 150));
            }
        }, 5 * 1000);
        Timer.MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                c.getPlayer().dropMessage(5, "누적 데미지 : " + c.getPlayer().getDamageMeter());
                updateDamageMeter(c.getPlayer(), c.getPlayer().getDamageMeter());
                warp(123456788);
            }
        }, 25 * 1000);
    }

    public static void updateDamageMeter(MapleCharacter chr, long damage) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM damagemeter WHERE cid = ?");
            ps.setInt(1, chr.getId());
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO damagemeter(cid, name, damage) VALUES (?, ?, ?)");
            ps.setInt(1, chr.getId());
            ps.setString(2, chr.getName());
            ps.setLong(3, damage);
            ps.executeUpdate();
            ps.close();
            con.close();
            chr.setDamageMeter(0);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public String getDamageMeterRank(int limit) {
        String text = "#fn나눔고딕 Extrabold##fs13# ";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM damagemeter ORDER BY damage DESC LIMIT " + limit);
            ResultSet rs = ps.executeQuery();
            int i = 1;
            while (rs.next()) {
                text += (i != 10 ? " " : "") + i + "위 " + rs.getString("name") + " #r" + Comma(rs.getLong("damage")) + "#e\r\n";
                i++;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        if (text.equals("#b")) {
            text = "#r아직까지 딜량 미터기를 갱신한 유저가 없습니다.";
        }
        return text;
    }

    public String DamageMeterRank() {
        String text = "#b";
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM damagemeter ORDER BY damage DESC LIMIT 10");
            ResultSet rs = ps.executeQuery();
            int i = 1;
            while (rs.next()) {
                text += "#r#e" + (i != 10 ? "0" : "") + i + "#n#b위 #r닉네임#b " + rs.getString("name") + " #r누적 데미지#b " + Comma(rs.getLong("damage")) + "\r\n";
                i++;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        if (text.equals("#b")) {
            text = "#r아직까지 딜량 미터기를 갱신한 유저가 없습니다.";
        }
        return text;
    }

    public boolean isDamageMeterRanker(int cid) {
        boolean value = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM damagemeter ORDER BY damage DESC LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("cid") == cid) {
                    value = true;
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return value;
    }

    public int getDamageMeterRankerId() {
        int value = -1;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM damagemeter ORDER BY damage DESC LIMIT 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                value = rs.getInt("cid");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return value;
    }

    public String Comma(long r) {
        String re = "";
        for (int i = String.valueOf(r).length(); i >= 1; i--) {
            if (i != 1 && i != String.valueOf(r).length() && i % 3 == 0) {
                re += ",";
            }
            re += String.valueOf(r).charAt(i - 1);

        }
        return new StringBuilder().append(re).reverse().toString();
    }

    public void gainCashItem(MapleClient c, Item item) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!ii.itemExists(item.getItemId())) {
            return;
        }
        if (!ii.isCash(item.getItemId())) {
            return;
        }

        Item items = null;
        if ((GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(item.getItemId()) == MapleInventoryType.DECORATION) && ii.isCash(item.getItemId())) {
            items = (Equip) ii.getEquipById(item.getItemId());
        }
        int flag = items.getFlag();

        if (ii.isCash(items.getItemId())) {
            flag |= ItemFlag.KARMA_EQUIP.getValue();
            items.setUniqueId(MapleInventoryIdentifier.getInstance());
        }
        item.setFlag(flag);
        MapleInventoryManipulator.addbyItem(c, items);
    }

    public void gainDonationSkill(int skillid) {
        if (c.getPlayer().getKeyValue(201910, "DonationSkill") < 0) {
            c.getPlayer().setKeyValue(201910, "DonationSkill", "0");
        }

        MapleDonationSkill dskill = MapleDonationSkill.getBySkillId(skillid);
        if (dskill != null && (c.getPlayer().getKeyValue(201910, "DonationSkill") & dskill.getValue()) == 0) {
            int data = (int) c.getPlayer().getKeyValue(201910, "DonationSkill");
            data |= dskill.getValue();
            c.getPlayer().setKeyValue(201910, "DonationSkill", data + "");
            SkillFactory.getSkill(skillid).getEffect(SkillFactory.getSkill(skillid).getMaxLevel()).applyTo(c.getPlayer(), 0);
        }
    }

    public boolean hasDonationSkill(int skillid) {
        if (c.getPlayer().getKeyValue(201910, "DonationSkill") < 0) {
            c.getPlayer().setKeyValue(201910, "DonationSkill", "0");
        }

        MapleDonationSkill dskill = MapleDonationSkill.getBySkillId(skillid);
        if (dskill == null) {
            return false;
        } else if ((c.getPlayer().getKeyValue(201910, "DonationSkill") & dskill.getValue()) == 0) {
            return false;
        }
        return true;
    }

    public void upDateTearBuff() {
//        SkillFactory.getSkill(80002419).getEffect(1).applyTo(c.getPlayer());
    }

    public void getMaxSkillList() {
        String text = "#fs11#한계돌파를 진행할 #b스킬#k을 선택하여 주세요.\r\n자신의 직업에 맞는 스킬을 강화하시기 바랍니다.\r\n\r\n";
        int g = 0;
        NPCConversationManager cm = this;
        Map<Skill, SkillEntry> skills = new HashMap<>();
        for (Entry<Skill, SkillEntry> skill : getPlayer().getSkills().entrySet()) {
            skills.put(skill.getKey(), skill.getValue());
        }
        for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            Skill maxskill = SkillFactory.getSkill(skill.getKey().getId());
            int level = getPlayer().getSkillLevel(maxskill.getId()) + 1;
            if (maxskill.getMaxLevel() > 30) {
                if (getPlayer().getSkillLevel(maxskill.getId()) < maxskill.getMaxLevel()) {
                    text += "#L" + maxskill.getId() + "# #r[" + SkillFactory.getSkillName(maxskill.getId()) + "]#k [#b " + getPlayer().getSkillLevel(maxskill.getId()) + " / " + maxskill.getMaxLevel() + "#k]\r\n";
                    g++;
                }
                //text += "#L"+maxskill.getId()+"# #e#b["+SkillFactory.getSkillName(maxskill.getId())+"] 을/를 ["+ getPlayer().getSkillLevel(maxskill.getId()) + 1+"]#k 레벨로 레벨업 하겟습니다.n\r\n";
            }
        }
        if (g != 0) {
            cm.sendSimple(text);
        } else {
            cm.sendOk("레벨업 할 수 있는 스킬이 존재 하지 않습니다.");
            cm.dispose();
        }
    }

    public void ChangeMaxSkillLevel(int skillid) {
        int level = getPlayer().getSkillLevel(skillid) + 1;
        getPlayer().changeSkillLevel(skillid, (byte) level, (byte) SkillFactory.getSkill(skillid).getMaxLevel());
        sendOk("[" + SkillFactory.getSkillName(skillid) + "] 스킬을 강화 하여 레벨이 #r[Lv. " + level + "]#k 로 증가 하였습니다.");
        dispose();
    }

}
