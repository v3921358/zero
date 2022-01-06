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
package handling.login.handler;

import client.*;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.JobConstants;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;
import java.io.UnsupportedEncodingException;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;
import tools.packet.PacketHelper;

import java.nio.charset.Charset;
import java.util.*;

public class CharLoginHandler {

    private static final boolean loginFailCount(final MapleClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    public static final void login(final LittleEndianAccessor slea, final MapleClient c) {
        byte[] m = slea.read(6);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.length; i++) {
            sb.append(String.format("%02X%s", m[i], (i < m.length - 1) ? "-" : ""));
        }
        slea.skip(15);
        boolean nexonTab = slea.readByte() == 1;
        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();
        int loginok = 0;
        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();
        if (AutoRegister.autoRegister) {
            if (AutoRegister.getAccountExists(login)) {
                loginok = c.login(login, pwd, sb.toString(), ipBan || macBan);
            } else if (!nexonTab && !c.hasBannedIP() && !c.hasBannedMac()) {
                if (AutoRegister.createAccount(login, pwd, c.getSession().remoteAddress().toString())) {
                    loginok = c.login(login, pwd, sb.toString(), ipBan || macBan);
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "[시스템]\r\n가입이 성공적으로 완료되었습니다!"));
                    c.getSession().writeAndFlush(LoginPacket.getLoginFailed(21));
                    return;
                }
            }
        } else {
            loginok = c.login(login, pwd, sb.toString(), ipBan || macBan);
        }
        final Calendar tempbannedTill = c.getTempBanCalendar();

        if (loginok == 0 && (ipBan || macBan) && !c.isGm()) {
            loginok = 3;
            if (macBan) {
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more readable
                MapleCharacter.ban(c.getSession().remoteAddress().toString().split(":")[0], "Enforcing account ban, account " + login, false, 4, false);
            }
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().writeAndFlush(LoginPacket.getLoginFailed(loginok));
            } else {
                c.getSession().close();
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().writeAndFlush(LoginPacket.getTempBan(PacketHelper.getTime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            } else {
                c.getSession().close();
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c, login, pwd);
        }
    }

    public static final void HackShield(final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().writeAndFlush(LoginPacket.HackShield());
//        c.getSession().writeAndFlush(LoginPacket.EnableLogin());
    }

    public static final void SessionCheck(final LittleEndianAccessor slea, final MapleClient c) {
        int pRequest = slea.readInt();
        int pResponse = pRequest ^ SendPacketOpcode.SESSION_CHECK.getValue();
        c.getSession().writeAndFlush(LoginPacket.SessionCheck(pResponse));
    }

    public static void getLoginRequest(LittleEndianAccessor slea, MapleClient c) {
        short webStart = slea.readByte();
        if (webStart == 1) {
            String token = slea.readMapleAsciiString();

            String[] sp = token.split(",");
            String login = "", pwd = "";
            if (ServerConstants.authlist.get(sp[0]) != null) {
                login = ServerConstants.authlist.get(sp[0]).left;
                pwd = ServerConstants.authlist.get(sp[0]).mid;
            }
            int loginok = c.login(login, pwd, "", true, c.hasBannedIP());
            /*            if (loginok != 0) { // hack
             System.out.println("로그인 실패 WebStart : id : " + login + " / pw : " + pwd + " / 로그인 번호 : " + loginok);
             c.getSession().close(); // 여기로 와서 튕기는거같은데
             return;
             }*/

            if (loginok == 0) {
                c.setAccountName(login);
                c.setPlayer(null);
//                c.getSession().writeAndFlush(LoginPacket.getRelogResponse());
                c.getSession().writeAndFlush(LoginPacket.getAuthSuccessRequest(c));
                c.getSession().writeAndFlush(LoginPacket.getCharEndRequest(c, login, pwd, false));
            } else {
                c.getSession().writeAndFlush(LoginPacket.getLoginFailed(20));
            }
        } else {
            System.out.println("로그인시도 오류 발생?");
            /*final String account = slea.readMapleAsciiString();
             String login = null, pwd = null;
             if (ServerConstants.authlist.get(account.split(",")[0]) != null) {
             login = ServerConstants.authlist.get(account.split(",")[0]).left;
             pwd = ServerConstants.authlist.get(account.split(",")[0]).mid;
             }
             int loginok = c.login(login, pwd, "", c.hasBannedIP());
             if (loginok != 0) { // hack
             c.getSession().close();
             return;
             }
             if (c.finishLogin() == 0) {
             c.setAccountName(login);
             c.getSession().writeAndFlush(LoginPacket.getRelogResponse());
             c.getSession().writeAndFlush(LoginPacket.getCharEndRequest(c, login, pwd, false));
             } else {
             c.getSession().writeAndFlush(LoginPacket.getLoginFailed(20));
             }*/
        }
    }

    public static final void ServerListRequest(final MapleClient c, boolean leaving) {
        /*if (leaving) {
         c.getSession().writeAndFlush(LoginPacket.LeavingTheWorld());
         }*/
        Date time = new Date();

        if (time.getDay() == 0) {
            c.getSession().writeAndFlush(LoginPacket.ChannelBackImg(true));
        } else {
            c.getSession().writeAndFlush(LoginPacket.ChannelBackImg(false));
        }
        c.getSession().writeAndFlush(LoginPacket.getServerList(0, LoginServer.getLoad()));
        c.getSession().writeAndFlush(LoginPacket.getEndOfServerList());

        c.getSession().writeAndFlush(LoginPacket.enableRecommended());
//        c.getSession().writeAndFlush(LoginPacket.sendRecommended(0, null));
    }

    public static final void ServerStatusRequest(final MapleClient c) {
        // 0 = Select world normally
        // 1 = "Since there are many users, you may encounter some..."
        // 2 = "The concurrent users in this world have reached the max"
        final int numPlayer = LoginServer.getUsersOn();
        final int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
            c.getSession().writeAndFlush(LoginPacket.getServerStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.getSession().writeAndFlush(LoginPacket.getServerStatus(1));
        } else {
            c.getSession().writeAndFlush(LoginPacket.getServerStatus(0));
        }
    }

    public static final void CharlistRequest(final LittleEndianAccessor slea, final MapleClient c) {
        final boolean isFirstLogin = slea.readByte() == 0;
        final int server = slea.readByte();
        final int channel = slea.readByte() + 1;
        int gameend = slea.readByte(); // 306 추가됨
        if (!World.isChannelAvailable(channel) || server != 0) { //TODOO: MULTI WORLDS
            c.getSession().writeAndFlush(LoginPacket.getLoginFailed(10)); //cannot process so many
            return;
        }

        boolean check = false;
        if (gameend == 1) {
            slea.skip(1);
            String idpw[] = slea.readMapleAsciiString().split(",");
            byte[] m = slea.read(6);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < m.length; i++) {
                sb.append(String.format("%02X%s", m[i], (i < m.length - 1) ? "-" : ""));
            }

            String login = idpw[0], pwd = idpw[1];
            if (ServerConstants.authlist.get(idpw[0]) != null) {
                login = ServerConstants.authlist.get(idpw[0]).left;
                pwd = ServerConstants.authlist.get(idpw[0]).mid;
            }

            if (!isFirstLogin) {
                c.getSession().writeAndFlush(LoginPacket.getCharEndRequest(c, login, pwd, true));
                c.getSession().writeAndFlush(LoginPacket.getSelectedWorldResult(server));
            }

            c.updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, c.getSessionIPAddress(), login);

            int loginok = c.login(login, pwd, sb.toString(), false);
            c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, login);
            if (loginok != 0) {
                c.getSession().close();
            } else {
                c.setAccountName(idpw[0]);
                c.getSession().writeAndFlush(LoginPacket.getAuthSuccessRequest(c, idpw[0], idpw[1]));
                check = true;
            }
        }

        if (!c.isLoggedIn() && !check && !c.isGm()) {
            c.getSession().close();
            return;
        }

        //System.out.println("Client " + c.getSession().getRemoteAddress().toString().split(":")[0] + " is connecting to server " + server + " channel " + channel + "");
        final List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            c.getSession().writeAndFlush(LoginPacket.getCharList(c.getSecondPassword(), chars, c.getCharacterSlots(), c.getNameChangeEnable())); //c.getCharacterSlots() 캐릭터 슬롯 임시 잠금
        } else {
            c.getSession().close();
        }
    }

    public static final void SelectChannelList(MapleClient c, int world) {
        if (!GameConstants.isOpen) {
            c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "[시스템]\r\n데이터 로딩중입니다.\r\n잠시만 기다려주세요."));
            c.getSession().writeAndFlush(LoginPacket.getSelectedChannelFailed((byte) 21, world));
            /*        } else if (!ServerConstants.ConnectorSetting && !c.isGm() && !c.isAllowedClient()) {
             c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "[시스템]\r\n서버 점검중입니다."));
             c.getSession().writeAndFlush(LoginPacket.getSelectedChannelFailed((byte) 21, world));*/
        } else {
            for (ChannelServer csv : ChannelServer.getAllInstances()) {
                if (csv.isShutdown()) {
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "[시스템]\r\n서버 종료중입니다."));
                    c.getSession().writeAndFlush(LoginPacket.getSelectedChannelFailed((byte) 21, world));
                    return;
                }
            }
            c.getSession().writeAndFlush(LoginPacket.getSelectedChannelResult(world));
        }
    }

    public static final void CheckCharName(final String name, final MapleClient c) {
        c.getSession().writeAndFlush(LoginPacket.charNameResponse(name, !(MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()))));
    }

    public static final void CheckCharNameChange(final LittleEndianAccessor slea, final MapleClient c) {
        final int cid = slea.readInt();
        final String beforename = slea.readMapleAsciiString();
        final String afterName = slea.readMapleAsciiString();
        c.setNameChangeEnable((byte) 0);
        MapleCharacter.saveNameChange(afterName, cid);
        MapleCharacter.updateNameChangeCoupon(c);
        c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "캐릭터 이름이 성공적으로 변경되었습니다. 변경을 위해 다시 로그인 바랍니다."));
    }

    public static void CreateChar(final LittleEndianAccessor slea, final MapleClient c) throws UnsupportedEncodingException {
        final String name = slea.readMapleAsciiString();
        slea.readInt(); //선택형 키보드 (0 : 기존 / 1 : 수정)
        slea.readInt(); //-1
        final JobType jobType = JobType.getByType(slea.readInt());
        if (jobType == null) {
            System.out.println("새로운 직업군 코드 : " + jobType);
            return;
        }
        for (JobConstants.LoginJob j : JobConstants.LoginJob.values()) {
            if (j.getJobType() == jobType.type) {
                if (j.getFlag() != JobConstants.LoginJob.JobFlag.ENABLED.getFlag()) {
                    c.getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "현재 이 직업군은 구현중이므로 생성하실 수 없습니다. \r\n ESC키를 눌러 다시 로그인하시길 바랍니다."));
                    return;
                }
            }
        }
        final short subcategory = slea.readShort(); //듀얼블레이드 = 1, 모험가직업군 = 0
        final byte gender = slea.readByte();
        byte skinColor = slea.readByte();
        byte itemcount = slea.readByte();
        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        for (int i = 0; i < itemcount; i++) {
            int itemid = slea.readInt();
            if (GameConstants.isFace(itemid)) {
                newchar.setFace(itemid);
            } else if (GameConstants.isHair(itemid)) {
                newchar.setHair(itemid);
            } else if (GameConstants.isAccessory(itemid)) {
                newchar.setDemonMarking(itemid);
            } else {
                byte wherepot = GameConstants.EqitemPostionById(itemid);
                Item item = li.getEquipById(itemid);
                item.setPosition((byte) wherepot);
                equip.addFromDB(item);
            }
        }
        newchar.setWorld((byte) c.getWorld());
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skinColor);
        if (c.isGm()) { //관리자일때 캐릭터 생성시 GM설정. 
            newchar.setGMLevel((byte) 10);
        }
        int[][] skills = new int[][]{
            {30011000, 30011001, 30020002, 30001281},//레지스탕스
            {1281},//모험가
            {10001244, 10000252, 80001152, 10001253, 10001254},//시그너스
            {20000194},//Aran
            {20010022, 20010194},//Evan
            {20020109, 20021110, 20020111, 20020112}, //Mercedes
            {30010110, 30010185},//데몬
            {20031208, 20040190, 20031203, 20031205, 20030206, 20031207, 20031209, 20031251, 20031260},//Phantom
            {},//Dualblade
            {50001214},//Mihile
            {20040216, 20040217, 20040218, 20040219, 20040221, 20041222},//Luminous
            {60001216, 60001217},//Kaiser
            {60011216, 60010217, 60011218, 60011219, 60011220, 60011221, 60011222},//AngelicBuster
            {},//Cannoneer
            {30020232, 30020233, 30020234, 30020240, 30021235, 30021236, 30021237},//Xenon
            {100000279, 100000282, 100001262, 100001263, 100001264, 100001265, 100001266, 100001268},//Zero
            {20051284, 20050285, 20050286},//EunWol
            {},//PinkBean
            {140000291, 14200, 14210, 14211, 14212},//Kinesis
            {60020216, 60021217},//Kadena
            {},//Iliume
            {150010079, 150011005},//ark
            {},//PathFinder
            {160001075, 160000000, 160000076},//호영
            {150021000, 150020006, 150021005}, //아델
            {},
            {},
            {160010000, 160011074, 160011075}
        };
        if (skills[jobType.type].length > 0) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            Skill s;
            for (int i : skills[jobType.type]) {
                s = SkillFactory.getSkill(i);
                if (s != null) {
                    byte maxLevel = (byte) s.getMaxLevel();
                    if (maxLevel < 1) {
                        maxLevel = (byte) s.getMasterLevel();
                    }
                    byte level = 1;
                    if (i == 150020006 )
                        level = maxLevel;
                    if (i == 160000076)
                        level = 10;
                    ss.put(s, new SkillEntry(level, maxLevel, -1));
                }
            }
            if (jobType == JobType.EunWol) {
                ss.put(SkillFactory.getSkill(25001000), new SkillEntry((byte) 0, (byte) 25, -1));
                ss.put(SkillFactory.getSkill(25001002), new SkillEntry((byte) 0, (byte) 25, -1));
            }
            newchar.changeSkillLevel_Skip(ss, false);
        }
        switch (jobType) {
            case Zero:
                newchar.setSecondGender((byte) 1);
                newchar.setSecondFace(21290);
                newchar.setSecondHair(37623);
                newchar.setLevel((short) 101);
                newchar.setJob(10112);
                newchar.getStat().str = 518;
                newchar.getStat().maxhp = 6910;
                newchar.getStat().hp = 6910;
                newchar.getStat().maxmp = 100;
                newchar.getStat().mp = 100;
                newchar.setRemainingSp(3, 0); //alpha
                newchar.setRemainingSp(3, 1); //beta
                break;
        }
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()) && (c.isGm() || c.canMakeCharacter(c.getWorld()))) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, subcategory);
            c.getSession().writeAndFlush(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.getSession().writeAndFlush(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static final void Character_WithoutSecondPassword(final LittleEndianAccessor slea, final MapleClient c) {
        int key = slea.readInt();
        short size = slea.readShort();
        byte[] newpass = slea.read(size);
        int charId = slea.readInt();
        if (!c.isLoggedIn() || loginFailCount(c) || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
            c.getSession().close();
            return;
        }

        byte[] nowpass = c.getSecondPassword().getBytes(Charset.forName("MS949"));
        for (int i = 0; i < nowpass.length; i++) {
            int real = nowpass[i] & 0xFF;
            real <<= 3;
            for (int j = 0; j < key; j++) {
                real *= 2;
            }
            nowpass[i] = (byte) ((byte) real - ((int) (real / 255) * (int) 255));
        }
        boolean login = true;
        if (nowpass.length == newpass.length) {
            for (int i = 0; i < nowpass.length; i++) {
                if (nowpass[i] != newpass[i]) {
                    login = false;
                    break;
                }
            }
        } else {
            login = false;
        }

        if (login && newpass.length >= 4 && newpass.length <= 16) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            final String s = c.getSessionIPAddress();
            LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
            c.getSession().writeAndFlush(CField.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
        } else {
            c.getSession().writeAndFlush(LoginPacket.secondPwError((byte) 0x14));
        }
    }

    public static final void LoginWithCreateCharacter(final LittleEndianAccessor slea, final MapleClient c) {
        final int charId = slea.readInt();
        if (!c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
            c.getSession().close();
            return;
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        final String s = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
        c.getSession().writeAndFlush(CField.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }

    public static final void CreateUltimate(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn() || c.getPlayer() == null || c.getPlayer().getLevel() < 120 || c.getPlayer().getMapId() != 130000000 || c.getPlayer().getQuestStatus(20734) != 0 || c.getPlayer().getQuestStatus(20616) != 2 || !GameConstants.isKOC(c.getPlayer().getJob()) || !c.canMakeCharacter(c.getPlayer().getWorld())) {
            c.getPlayer().dropMessage(1, "You have no character slots.");
            c.getSession().writeAndFlush(CField.createUltimate(0));
            return;
        }
        final String name = slea.readMapleAsciiString();
        final int job = slea.readInt(); //job ID
        if (job < 110 || job > 520 || job % 10 > 0 || (job % 100 != 10 && job % 100 != 20 && job % 100 != 30) || job == 430) {
            c.getPlayer().dropMessage(1, "An error has occurred.");
            c.getSession().writeAndFlush(CField.createUltimate(0));
            return;
        }
        final int face = slea.readInt();
        final int hair = slea.readInt();

        final int hat = slea.readInt();
        final int top = slea.readInt();
        final int glove = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();

        //final byte gender = c.getPlayer().getGender();
        JobType jobType = JobType.Adventurer;
        //if (!LoginInformationProvider.getInstance().isEligibleItem(gender, 0, jobType.type, face) || !LoginInformationProvider.getInstance().isEligibleItem(gender, 1, jobType.type, hair)) {
        //    c.getPlayer().dropMessage(1, "An error occurred.");
        //    c.getSession().writeAndFlush(CField.createUltimate(0));
        //    return;
        //}

        jobType = JobType.UltimateAdventurer;
        if (!LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, hat) || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, top)
                || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, glove) || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, shoes)
                || !LoginInformationProvider.getInstance().isEligibleItem(-1, job, jobType.type, weapon)) {
            c.getPlayer().dropMessage(1, "An error occured.");
            c.getSession().writeAndFlush(CField.createUltimate(0));
            return;
        }

        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setJob(job);
        newchar.setWorld((byte) c.getPlayer().getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        //newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor((byte) 3); //troll
        newchar.setLevel((short) 51);
        newchar.getStat().str = (short) 4;
        newchar.getStat().dex = (short) 4;
        newchar.getStat().int_ = (short) 4;
        newchar.getStat().luk = (short) 4;
        newchar.setRemainingAp((short) 254); //49*5 + 25 - 16
        newchar.setRemainingSp(job / 100 == 2 ? 128 : 122); //2 from job advancements. 120 from leveling. (mages get +6)
        newchar.getStat().maxhp += 150; //Beginner 10 levels
        newchar.getStat().maxmp += 125;
        switch (job) {
            case 110:
            case 120:
            case 130:
                newchar.getStat().maxhp += 600; //Job Advancement
                newchar.getStat().maxhp += 2000; //Levelup 40 times
                newchar.getStat().maxmp += 200;
                break;
            case 210:
            case 220:
            case 230:
                newchar.getStat().maxmp += 600;
                newchar.getStat().maxhp += 500; //Levelup 40 times
                newchar.getStat().maxmp += 2000;
                break;
            case 310:
            case 320:
            case 410:
            case 420:
            case 520:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 900; //Levelup 40 times
                newchar.getStat().maxmp += 600;
                break;
            case 510:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 450; //Levelup 20 times
                newchar.getStat().maxmp += 300;
                newchar.getStat().maxhp += 800; //Levelup 20 times
                newchar.getStat().maxmp += 400;
                break;
            default:
                return;
        }
        for (int i = 2490; i < 2507; i++) {
            newchar.setQuestAdd(MapleQuest.getInstance(i), (byte) 2, null);
        }
        newchar.setQuestAdd(MapleQuest.getInstance(29947), (byte) 2, null);
        newchar.setQuestAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER), (byte) 0, c.getPlayer().getName());

        final Map<Skill, SkillEntry> ss = new HashMap<>();
        ss.put(SkillFactory.getSkill(1074 + (job / 100)), new SkillEntry((byte) 5, (byte) 5, -1));
        ss.put(SkillFactory.getSkill(80), new SkillEntry((byte) 1, (byte) 1, -1));
        newchar.changeSkillLevel_Skip(ss, false);
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();

        int[] items = new int[]{1142257, hat, top, shoes, glove, weapon, hat + 1, top + 1, shoes + 1, glove + 1, weapon + 1}; //brilliant = fine+1
        for (byte i = 0; i < items.length; i++) {
            Item item = li.getEquipById(items[i]);
            item.setPosition((byte) (i + 1));
            newchar.getInventory(MapleInventoryType.EQUIP).addFromDB(item);
        }
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        c.getPlayer().fakeRelog();
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm())) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, (short) 0);
            MapleQuest.getInstance(20734).forceComplete(c.getPlayer(), 1101000);
            c.getSession().writeAndFlush(CField.createUltimate(1));
        } else {
            c.getSession().writeAndFlush(CField.createUltimate(0));
        }
    }

    public static final void DeleteChar(final LittleEndianAccessor slea, final MapleClient c) {
        String Secondpw_Client = slea.readMapleAsciiString();
        final int Character_ID = slea.readInt();
        if (!c.login_Auth(Character_ID) || !c.isLoggedIn()) {// || loginFailCount(c)) {
            c.getSession().close();
            return; // Attempting to delete other character
        }
        byte state = 0;
        if (c.getSecondPassword() != null) { // On the server, there's a second password
            if (Secondpw_Client == null) { // Client's hacking
                c.getSession().close();
                return;
            } else {
                if (!c.CheckSecondPassword(Secondpw_Client)) { // Wrong Password
                    state = 20;
                }
            }
        }
        if (state == 0) {
            state = (byte) c.deleteCharacter(Character_ID);
        }
        c.getSession().writeAndFlush(LoginPacket.deleteCharResponse(Character_ID, state));
    }

    public static final void checkSecondPassword(final LittleEndianAccessor rh, final MapleClient c) {
        String code = rh.readMapleAsciiString();
        if (!c.CheckSecondPassword(code)) {
            c.getSession().writeAndFlush(LoginPacket.secondPwError((byte) 0x14));
        } else {
            c.getSession().writeAndFlush(LoginPacket.getSecondPasswordConfirm((byte) 0));
        }
    }

    public static void NewPassWordCheck(MapleClient c) {
        c.getSession().writeAndFlush(LoginPacket.NewSendPasswordWay(c));
    }

    public static final void onlyRegisterSecondPassword(final LittleEndianAccessor slea, final MapleClient c) {
        String secondpw = slea.readMapleAsciiString();
        if (secondpw.length() >= 6 && secondpw.length() <= 16) {
            c.setSecondPassword(secondpw);
            c.getSession().writeAndFlush(LoginPacket.getSecondPasswordResult(true));
            c.updateSecondPassword();
        } else {
            c.getSession().writeAndFlush(LoginPacket.secondPwError((byte) 0x14));
        }
    }
}
