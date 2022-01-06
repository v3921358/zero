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

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer;
import server.Timer.CloneTimer;
import server.enchant.EnchantFlag;
import server.enchant.EquipmentEnchant;
import server.enchant.StarForceStats;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.field.boss.MapleBossManager;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.Event_DojoAgent;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleReactor;
import server.maps.SavedLocationType;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageEventAgent;
import server.marriage.MarriageManager;
import server.marriage.MarriageTicketType;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.PetPacket;

public abstract class AbstractPlayerInteraction {

    protected MapleClient c;
    protected int id, id2;

    public AbstractPlayerInteraction(final MapleClient c, final int id, final int id2) {
        this.c = c;
        this.id = id;
        this.id2 = id2;
    }

    public final MapleClient getClient() {
        return c;
    }

    public final MapleClient getC() {
        return c;
    }

    public MapleCharacter getChar() {
        return c.getPlayer();
    }

    public final ChannelServer getChannelServer() {
        return c.getChannelServer();
    }

    public final MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public final EventManager getEventManager(final String event) {
        return c.getChannelServer().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return c.getPlayer().getEventInstance();
    }

    public final void warp(final int map) {
        final MapleMap mapz = getWarpMap(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void dojowarp(final int map) {
        final MapleMap mapz = getWarpMap(map);
        mapz.resetFully();
        c.getPlayer().setDojoStopTime(0);
        c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        int floor = (mapz.getId() - 925070000) / 100;
        int id = 0, id2 = 0;
        long hp = 0, hp2 = 0;
        switch (floor) {
            case 1:
                id = 9305600;
                hp = 5200000;
                break;
            case 2:
                id = 9305601;
                hp = 5740800;
                break;
            case 3:
                id = 9305602;
                hp = 6307200;
                break;
            case 4:
                id = 9305603;
                hp = 6930000;
                break;
            case 5:
                id = 9305604;
                hp = 7549200;
                break;
            case 6:
                id = 9305605;
                hp = 12342000;
                break;
            case 7:
                id = 9305606;
                hp = 13923000;
                break;
            case 8:
                id = 9305607;
                hp = 15105000;
                break;
            case 9:
                id = 9305608;
                hp = 16846000;
                break;
            case 10:
                id = 9305619;
                hp = 100000000;
                break;
            case 11:
                id = 9305610;
                hp = 40824000;
                break;
            case 12:
                id = 9305617;
                hp = 45404550;
                break;
            case 13:
                id = 9305612;
                hp = 48593250;
                break;
            case 14:
                id = 9305611;
                hp = 55350000;
                break;
            case 15:
                id = 9305628;
                hp = 61600500;
                break;
            case 16:
                id = 9305682;
                hp = 68121000;
                break;
            case 17:
                id = 9305683;
                hp = 12342000;
                break;
            case 18:
                id = 9305614;
                hp = 90011250;
                break;
            case 19:
                id = 9305618;
                hp = 97902000;
                break;
            case 20:
                id = 9305609;
                hp = 1500000000;
                break;
            case 21:
                id = 9305623;
                id2 = 9305644;
                hp = 130536000;
                hp2 = 130536000;
                break;
            case 22:
                id = 9305625;
                id2 = 9305646;
                hp = 159138000;
                hp2 = 159138000;
                break;
            case 23:
                id = 9305624;
                id2 = 9305645;
                hp = 190350000;
                hp2 = 190350000;
                break;
            case 24:
                id = 9305684;
                id2 = 9305685;
                hp = 242424000;
                hp2 = 242424000;
                break;
            case 25:
                id = 9305658;
                id2 = 9305686;
                hp = 405504000;
                hp2 = 405504000;
                break;
            case 26:
                id = 9305687;
                id2 = 9305688;
                hp = 497040000;
                hp2 = 497040000;
                break;
            case 27:
                id = 9305616;
                id2 = 9305689;
                hp = 596496000;
                hp2 = 596496000;
                break;
            case 28:
                id = 9305690;
                id2 = 9305691;
                hp = 706176000;
                hp2 = 706176000;
                break;
            case 29:
                id = 9305692;
                id2 = 9305693;
                hp = 824256000;
                hp2 = 824256000;
                break;
            case 30:
                id = 9305629;
                hp = 3000000000L;
                break;
            case 31:
                id = 9305630;
                hp = 2108240000;
                break;
            case 32:
                id = 9305631;
                hp = 2526520000L;
                break;
            case 33:
                id = 9305659;
                hp = 2976000000L;
                break;
            case 34:
                id = 9305633;
                hp = 3464920000L;
                break;
            case 35:
                id = 9305621;
                hp = 3986640000L;
                break;
            case 36:
                id = 9305632;
                hp = 4551000000L;
                break;
            case 37:
                id = 9305694;
                hp = 5149760000L;
                break;
            case 38:
                id = 9305634;
                hp = 6474960000L;
                break;
            case 39:
                id = 9305656;
                hp = 7971840000L;
                break;
            case 40:
                id = 9305639;
                hp = 8000000000L;
                break;
            case 41:
                id = 9305660;
                hp = 42000000000L;
                break;
            case 42:
                id = 9305661;
                hp = 63000000000L;
                break;
            case 43:
                id = 9305627;
                hp = 84000000000L;
                break;
            case 44:
                id = 9305622;
                hp = 105000000000L;
                break;
            case 45:
                id = 9305662;
                hp = 105000000000L;
                break;
            case 46:
                id = 9305635;
                hp = 210000000000L;
                break;
            case 47:
                id = 9305636;
                hp = 315000000000L;
                break;
            case 48:
                id = 9305637;
                hp = 420000000000L;
                break;
            case 49:
                id = 9305638;
                hp = 525000000000L;
                break;
            case 50:
                id = 9305695;
                hp = 525000000000L;
                break;
            case 51:
                id = 9305696;
                hp = 630000000000L;
                break;
            case 52:
                id = 9305663;
                hp = 735000000000L;
                break;
            case 53:
                id = 9305664;
                hp = 840000000000L;
                break;
            case 54:
                id = 9305665;
                hp = 945000000000L;
                break;
            case 55:
                id = 9305666;
                hp = 1050000000000L;
                break;
            case 56:
                id = 9305667;
                hp = 1155000000000L;
                break;
            case 57:
                id = 9305668;
                hp = 1260000000000L;
                break;
            case 58:
                id = 9305669;
                hp = 1365000000000L;
                break;
            case 59:
                id = 9305670;
                hp = 1470000000000L;
                break;
            case 60:
                id = 9305671;
                hp = 1575000000000L;
                break;
            case 61:
                id = 9305697;
                hp = 1680000000000L;
                break;
            case 62:
                id = 9305698;
                hp = 1785000000000L;
                break;
            case 63:
                id = 9305699;
                hp = 1890000000000L;
                break;
            case 64:
                id = 9305700;
                hp = 1911000000000L;
                break;
            case 65:
                id = 9305701;
                hp = 1932000000000L;
                break;
            case 66:
                id = 9305657;
                hp = 1953000000000L;
                break;
            case 67:
                id = 9305702;
                hp = 1974000000000L;
                break;
            case 68:
                id = 9305703;
                hp = 1995000000000L;
                break;
            case 69:
                id = 9305704;
                hp = 2016000000000L;
                break;
            case 70:
                id = 9305705;
                hp = 2100000000000L;
                break;
            case 71:
                id = 9305706;
                hp = 2310000000000L;
                break;
            case 72:
                id = 9305707;
                hp = 2625000000000L;
                break;
            case 73:
                id = 9305708;
                hp = 2940000000000L;
                break;
            case 74:
                id = 9305672;
                hp = 3255000000000L;
                break;
            case 75:
                id = 9305673;
                hp = 3502380000000L;
                break;
            case 76:
                id = 9305674;
                hp = 3779068020000L;
                break;
            case 77:
                id = 9305675;
                hp = 4088951597640L;
                break;
            case 78:
                id = 9305676;
                hp = 4436512483439L;
                break;
            case 79:
                id = 9305677;
                hp = 4826925581981L;
                break;
            case 80:
                id = 9305640;
                hp = 20000000000000L;
                break;
        }
        MapleMonster mob = MapleLifeFactory.getMonster(id);
        mob.setHp(hp);
        mob.getStats().setHp(hp);
        mapz.spawnMonsterWithEffectBelow(mob, new Point(Randomizer.nextBoolean() ? -304 : 185, 7), 1);
        mob.applyStatus(c, MonsterStatus.MS_AddEffect, new MonsterStatusEffect(0, Short.MAX_VALUE), 0, null);
        if (id2 > 0) {
            for (int i = 0; i < 5; i++) {
                MapleMonster mob2 = MapleLifeFactory.getMonster(id2);
                mob2.setHp(hp2);
                mob2.getStats().setHp(hp2);
                mapz.spawnMonsterWithEffectBelow(mob2, new Point(Randomizer.nextBoolean() ? -304 : 185, 7), 1);
                mob2.applyStatus(c, MonsterStatus.MS_AddEffect, new MonsterStatusEffect(0, Short.MAX_VALUE), 0, null);
            }
        }
    }

    public final void warp_Instanced(final int map) {
        final MapleMap mapz = getMap_Instanced(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        if (portal != 0 && map == c.getPlayer().getMapId()) { //test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                c.getSession().writeAndFlush(CField.instantMapWarp(c.getPlayer(), (byte) portal)); //until we get packet for far movement, this will do
                c.getPlayer().checkFollow();
                c.getPlayer().getMap().movePlayer(c.getPlayer(), portalPos);
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void setDamageSkin(int itemid) {
        MapleQuest quest = MapleQuest.getInstance(7291);
        MapleQuestStatus queststatus = new MapleQuestStatus(quest, (byte) 1);
        int skinnum = GameConstants.getDSkinNum(itemid);
        String skinString = String.valueOf(skinnum);
        queststatus.setCustomData(skinString == null ? "0" : skinString);
        c.getPlayer().updateQuest(queststatus, true);
        c.getPlayer().setKeyValue(7293, "damage_skin", String.valueOf(itemid));
        c.getPlayer().dropMessage(5, "데미지 스킨이 변경되었습니다.");
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.showForeignDamageSkin(c.getPlayer(), skinnum), false);
        c.getPlayer().updateDamageSkin();
    }

    public final void warp(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        if (map == c.getPlayer().getMapId()) { //test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                c.getPlayer().checkFollow();
                c.getSession().writeAndFlush(CField.instantMapWarp(c.getPlayer(), (byte) c.getPlayer().getMap().getPortal(portal).getId()));
                c.getPlayer().getMap().movePlayer(c.getPlayer(), new Point(c.getPlayer().getMap().getPortal(portal).getPosition()));
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warpMap(final int mapid, final int portal) {
        final MapleMap map = getMap(mapid);
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public final void playPortalSE() {
        c.getSession().writeAndFlush(EffectPacket.showPortalEffect(0));
    }

    private final MapleMap getWarpMap(final int map) {
        return ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
    }

    public final MapleMap getMap() {
        return c.getPlayer().getMap();
    }

    public final MapleMap getMap(final int map) {
        return getWarpMap(map);
    }

    public final MapleMap getMap_Instanced(final int map) {
        return c.getPlayer().getEventInstance() == null ? getMap(map) : c.getPlayer().getEventInstance().getMapInstance(map);
    }

    public void spawnMonster(final int id, final int qty) {
        spawnMob(id, qty, c.getPlayer().getTruePosition(), 1);
    }

    public final void spawnMobOnMap(final int id, final int qty, final int x, final int y, final int map) {
        for (int i = 0; i < qty; i++) {
            getMap(map).spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), new Point(x, y));
        }
    }

    public final void spawnMob(final int id, final int qty, final int x, final int y) {
        spawnMob(id, qty, new Point(x, y), 1);
    }

    public final void spawnMob(final int id, final int x, final int y) {
        spawnMob(id, 1, new Point(x, y), 1);
    }

    public final void spawnMobIncrease(final int id, final int x, final int y, int hp) {
        spawnMob(id, 1, new Point(x, y), hp);
    }

    public final void spawnLinkMob(final int id, final int x, final int y) {
        MapleMonster m = MapleLifeFactory.getMonster(id);
        m.setHp((long) (m.getStats().getHp()));
        m.getStats().setHp((long) (m.getStats().getHp()));
        m.setOwner(c.getPlayer().getId());
        c.getPlayer().getMap().spawnMonsterOnGroundBelow(m, new Point(x, y));
    }

    private final void spawnMob(final int id, final int qty, final Point pos, int hp) {
        for (int i = 0; i < qty; i++) {
            MapleMonster m = MapleLifeFactory.getMonster(id);
            m.setHp((long) (m.getStats().getHp() * hp));
            m.getStats().setHp((long) (m.getStats().getHp() * hp));
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(m, pos);
        }
    }

    public final void killMob(int ids) {
        c.getPlayer().getMap().killMonster(ids);
    }

    public final void killAllMob() {
        c.getPlayer().getMap().killAllMonsters(true);
    }

    public final void addHP(final int delta) {
        c.getPlayer().addHP(delta);
    }

    public final int getPlayerStat(final String type) {
        if (type.equals("LVL")) {
            return c.getPlayer().getLevel();
        } else if (type.equals("STR")) {
            return c.getPlayer().getStat().getStr();
        } else if (type.equals("DEX")) {
            return c.getPlayer().getStat().getDex();
        } else if (type.equals("INT")) {
            return c.getPlayer().getStat().getInt();
        } else if (type.equals("LUK")) {
            return c.getPlayer().getStat().getLuk();
        } else if (type.equals("HP")) {
            return (int) c.getPlayer().getStat().getHp();
        } else if (type.equals("MP")) {
            return (int) c.getPlayer().getStat().getMp();
        } else if (type.equals("MAXHP")) {
            return (int) c.getPlayer().getStat().getMaxHp();
        } else if (type.equals("MAXMP")) {
            return (int) c.getPlayer().getStat().getMaxMp();
        } else if (type.equals("RAP")) {
            return c.getPlayer().getRemainingAp();
        } else if (type.equals("RSP")) {
            return c.getPlayer().getRemainingSp();
        } else if (type.equals("GID")) {
            return c.getPlayer().getGuildId();
        } else if (type.equals("GRANK")) {
            return c.getPlayer().getGuildRank();
        } else if (type.equals("ARANK")) {
            return c.getPlayer().getAllianceRank();
        } else if (type.equals("GM")) {
            return c.getPlayer().isGM() ? 1 : 0;
        } else if (type.equals("ADMIN")) {
            return c.getPlayer().isAdmin() ? 1 : 0;
        } else if (type.equals("GENDER")) {
            return c.getPlayer().getGender();
        } else if (type.equals("FACE")) {
            return c.getPlayer().getFace();
        } else if (type.equals("HAIR")) {
            return c.getPlayer().getHair();
        } else if (type.equals("SECONDHAIR")) {
            return c.getPlayer().getSecondHair();
        }
        return -1;
    }

    public final boolean isAngelicBuster() {
        return GameConstants.isAngelicBuster(c.getPlayer().getJob());
    }

    public final boolean isZero() {
        return GameConstants.isZero(c.getPlayer().getJob());
    }

    public final String getName() {
        return c.getPlayer().getName();
    }

    public final boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public final boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
        return c.getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public final boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHoldSlots(final int slot) {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHold(final int itemid) {
        return c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(c, itemid, quantity, "");
    }

    public final MapleQuestStatus getQuestRecord(final int id) {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id));
    }

    public final MapleQuestStatus getQuestNoRecord(final int id) {
        return c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(id));
    }

    public final byte getQuestStatus(final int id) {
        return c.getPlayer().getQuestStatus(id);
    }

    public final boolean isQuestActive(final int id) {
        return getQuestStatus(id) == 1;
    }

    public final boolean isQuestFinished(final int id) {
        return getQuestStatus(id) == 2;
    }

    public final void showQuestMsg(final String msg) {
        c.getSession().writeAndFlush(CWvsContext.showQuestMsg("", msg));
    }

    public final void forceStartQuest(final int id, final String data) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, data);
    }

    public final void forceStartQuest(final int id, final int data, final boolean filler) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, filler ? String.valueOf(data) : null);
    }

    public void forceStartQuest(final int id) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, null);
    }

    public void forceCompleteQuest(final int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public void spawnNpc(final int npcId) {
        c.getPlayer().getMap().spawnNpc(npcId, c.getPlayer().getPosition());
    }

    public final void spawnNpc(final int npcId, final int x, final int y) {
        c.getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public final void spawnNpc(final int npcId, final Point pos) {
        c.getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public final int spawnNpc2(final int npcId, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(pos).getId());
        npc.setCustom(true);
        c.getPlayer().getMap().addMapObject(npc);
        c.getSession().writeAndFlush(NPCPacket.spawnNPC2(npc, true));
        return npc.getObjectId();
    }

    public final void removeNpc(final int mapid, final int npcId) {
        c.getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public final void removeNpc(final int npcId) {
        c.getPlayer().getMap().removeNpc(npcId);
    }

    public final void forceStartReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.forceStartReactor(c);
                break;
            }
        }
    }

    public final void destroyReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final void hitReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final int getJob() {
        return c.getPlayer().getJob();
    }

    public final void gainNX(final int amount) {
        c.getPlayer().modifyCSPoints(1, amount, true);
    }

    public final void gainItemPeriod(final int id, final short quantity, final int period) { //period is in days
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItemPeriod(final int id, final short quantity, final long period, final String owner) { //period is in days
        gainItem(id, quantity, false, period, -1, owner);
    }

    public final void gainItem(final int id, final short quantity) {
        if (java.lang.Math.floor(id / 1000000) == 1) { // isEquip?
            for (int i = 0; i < quantity; i++) {
                gainItem(id, (short) 1, false, 0, -1, "");
            }
        } else {
            gainItem(id, quantity, false, 0, -1, "");
        }
    }

    public final void gainItemSilent(final int id, final short quantity) {
        gainItem(id, quantity, false, 0, -1, "", c, false);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats) {
        gainItem(id, quantity, randomStats, 0, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final int slots) {
        gainItem(id, quantity, randomStats, 0, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final long period) {
        gainItem(id, quantity, false, period, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots) {
        gainItem(id, quantity, randomStats, period, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner) {
        gainItem(id, quantity, randomStats, period, slots, owner, c);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg) {
        gainItem(id, quantity, randomStats, period, slots, owner, cg, true);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period, final int slots, final String owner, final MapleClient cg, final boolean show) {
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);

            if (!MapleInventoryManipulator.checkSpace(cg, id, quantity, "")) {
                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) || type.equals(MapleInventoryType.DECORATION) && !GameConstants.isThrowingStar(id) && !GameConstants.isBullet(id)) {
                final Equip item = (Equip) (ii.getEquipById(id));
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                if (ii.isCash(id)) {
                    item.setUniqueId(MapleInventoryIdentifier.getInstance());
                }
                if (id == 1142249) {
                    item.setEnhance((byte) 20);
                }
                if (id == 1142374) {
                    item.setEnhance((byte) 30);
                }

                int[] pcAb = {1212121, 1213028, 1222114, 1232114, 1242123, 1242124, 1262040, 1302344, 1312204, 1322256, 1332280, 1342105, 1362141, 1372229, 1382266, 1402260, 1412182, 1422190, 1432219, 1442276, 1452258, 1462244, 1472266, 1482222, 1492236, 1522144, 1532148, 1582027, 1272021, 1282022, 1592028, 1292028};
                int[] pcArc = {1212131, 1213030, 1222124, 1232124, 1242123, 1242144, 1262053, 1302359, 1312215, 1322266, 1332291, 1342110, 1362151, 1372239, 1382276, 1402271, 1412191, 1422199, 1432229, 1442287, 1452269, 1462254, 1472277, 1482234, 1492247, 1522154, 1532159, 1582046, 1272043, 1282043, 1592037, 1292030, 1214030};
                for (int pcA : pcAb) {
                    if (pcA == id) {
                        item.setPotential1(60056);
                        item.setPotential2(60057);
                        item.setPotential3(60058);
                        while (item.getEnhance() < 17) {
                            StarForceStats statz = EquipmentEnchant.starForceStats(item);
                            item.setEnhance((byte) (item.getEnhance() + 1));
                            for (Pair<EnchantFlag, Integer> stat : statz.getStats()) {
                                if (EnchantFlag.Watk.check(stat.left.getValue())) {
                                    item.setWatk((short) (item.getWatk() + stat.right));
                                }

                                if (EnchantFlag.Matk.check(stat.left.getValue())) {
                                    item.setMatk((short) (item.getMatk() + stat.right));
                                }

                                if (EnchantFlag.Str.check(stat.left.getValue())) {
                                    item.setStr((short) (item.getStr() + stat.right));
                                }

                                if (EnchantFlag.Dex.check(stat.left.getValue())) {
                                    item.setDex((short) (item.getDex() + stat.right));
                                }

                                if (EnchantFlag.Int.check(stat.left.getValue())) {
                                    item.setInt((short) (item.getInt() + stat.right));
                                }

                                if (EnchantFlag.Luk.check(stat.left.getValue())) {
                                    item.setLuk((short) (item.getLuk() + stat.right));
                                }

                                if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                                    item.setWdef((short) (item.getWdef() + stat.right));
                                }

                                if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                                    item.setMdef((short) (item.getMdef() + stat.right));
                                }

                                if (EnchantFlag.Hp.check(stat.left.getValue())) {
                                    item.setHp((short) (item.getHp() + stat.right));
                                }

                                if (EnchantFlag.Mp.check(stat.left.getValue())) {
                                    item.setMp((short) (item.getMp() + stat.right));
                                }

                                if (EnchantFlag.Acc.check(stat.left.getValue())) {
                                    item.setAcc((short) (item.getAcc() + stat.right));
                                }

                                if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                                    item.setAvoid((short) (item.getAvoid() + stat.right));
                                }
                            }
                        }
                    }
                }

                for (int pcAr : pcArc) {
                    if (pcAr == id) {
                        item.setPotential1(60057);
                        item.setPotential2(60085);
                        item.setPotential3(60086);
                        while (item.getEnhance() < 15) {
                            StarForceStats statz = EquipmentEnchant.starForceStats(item);
                            item.setEnhance((byte) (item.getEnhance() + 1));
                            for (Pair<EnchantFlag, Integer> stat : statz.getStats()) {
                                if (EnchantFlag.Watk.check(stat.left.getValue())) {
                                    item.setWatk((short) (item.getWatk() + stat.right));
                                }

                                if (EnchantFlag.Matk.check(stat.left.getValue())) {
                                    item.setMatk((short) (item.getMatk() + stat.right));
                                }

                                if (EnchantFlag.Str.check(stat.left.getValue())) {
                                    item.setStr((short) (item.getStr() + stat.right));
                                }

                                if (EnchantFlag.Dex.check(stat.left.getValue())) {
                                    item.setDex((short) (item.getDex() + stat.right));
                                }

                                if (EnchantFlag.Int.check(stat.left.getValue())) {
                                    item.setInt((short) (item.getInt() + stat.right));
                                }

                                if (EnchantFlag.Luk.check(stat.left.getValue())) {
                                    item.setLuk((short) (item.getLuk() + stat.right));
                                }

                                if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                                    item.setWdef((short) (item.getWdef() + stat.right));
                                }

                                if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                                    item.setMdef((short) (item.getMdef() + stat.right));
                                }

                                if (EnchantFlag.Hp.check(stat.left.getValue())) {
                                    item.setHp((short) (item.getHp() + stat.right));
                                }

                                if (EnchantFlag.Mp.check(stat.left.getValue())) {
                                    item.setMp((short) (item.getMp() + stat.right));
                                }

                                if (EnchantFlag.Acc.check(stat.left.getValue())) {
                                    item.setAcc((short) (item.getAcc() + stat.right));
                                }

                                if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                                    item.setAvoid((short) (item.getAvoid() + stat.right));
                                }
                            }
                        }
                    }
                }
                item.setGMLog(new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append("(" + this.id + " / " + id2 + ")로부터 gainItem으로 얻은 아이템").toString());
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { //medal
//                    final String msg = "<" + name + "> ?덉옣??吏湲됰릺?덉뒿?덈떎.";
                    //                  cg.getPlayer().dropMessage(-1, msg);
                    //                cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, null, period, new StringBuilder().append("(" + this.id + " / " + id2 + ")로부터 gainItem으로 얻은 아이템").toString());
            }
        } else {
            c.getPlayer().removeItem(id, quantity);
        }
        if (show) {
            cg.getSession().writeAndFlush(InfoPacket.getShowItemGain(id, quantity, false));
        }
    }

    public final boolean removeItem(final int id) { //quantity 1
        if (MapleInventoryManipulator.removeById_Lock(c, GameConstants.getInventoryType(id), id)) {
            c.getSession().writeAndFlush(EffectPacket.showCharmEffect(c.getPlayer(), id, 1, true, ""));
            return true;
        }
        return false;
    }

    public final void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(CField.musicChange(songName));
    }

    public final void changeMusic(boolean cast, final String songName) {
        if (cast) {
            getPlayer().getMap().broadcastMessage(CField.musicChange(songName));
        } else {
            getPlayer().getClient().getSession().writeAndFlush(CField.musicChange(songName));
        }
    }

    public final void worldMessage(final int type, final String message) {
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(type, "", message));
    }

    public static final void discordWorldMessage(final int type, final String message) {
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(type, message.substring(1)));
    }

    // default playerMessage and mapMessage to use type 5
    public final void playerMessage(final String message) {
        playerMessage(5, message);
    }

    public final void mapMessage(final String message) {
        mapMessage(5, message);
    }

    public final void guildMessage(final String message) {
        guildMessage(5, message);
    }

    public final void playerMessage(final int type, final String message) {
        c.getPlayer().dropMessage(type, message);
    }

    public final void mapMessage(final int type, final String message) {
        c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(type, "", message));
    }

    public final void guildMessage(final int type, final String message) {
        if (getPlayer().getGuildId() > 0) {
            World.Guild.guildPacket(getPlayer().getGuildId(), CWvsContext.serverNotice(type, "", message));
        }
    }

    public final MapleGuild getGuild() {
        return getGuild(getPlayer().getGuildId());
    }

    public final MapleGuild getGuild(int guildid) {
        return World.Guild.getGuild(guildid);
    }

    public final MapleGuildAlliance getAlliance(int allianceId) {
        return World.Alliance.getAlliance(allianceId);
    }

    public void setNewAlliance(int gid, int x) {
        World.Alliance.setNewAlliance(gid, x);
    }

    public final MapleParty getParty() {
        return c.getPlayer().getParty();
    }

    public final int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public final boolean isLeader() {
        if (getPlayer().getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == c.getPlayer().getId();
    }

    public final boolean isAllPartyMembersAllowedJob(final int job) {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            if (mem.getJobId() / 100 != job) {
                return false;
            }
        }
        return true;
    }

    public final boolean allMembersHere() {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return false;
            }
        }
        return true;
    }

    public final void setBossRaid(byte count) {
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            final MapleCharacter chr = getChannelServer().getPlayerStorage().getCharacterById(mem.getId());
            chr.getClient().getSession().writeAndFlush(UIPacket.closeUI(62));
            chr.setDeathCount(count);
            chr.getClient().getSession().writeAndFlush(CField.getDeathCount(count));
        }
    }

    public final long getKeyValue(int id, String key) {
        return c.getPlayer().getKeyValue(id, key);
    }

    public final void setKeyValue(int id, String key, String value) {
        c.getPlayer().setKeyValue(id, key, value);
    }

    public final void removeKeyValue(int id, String key) {
        c.getPlayer().removeKeyValue(id, key);
    }

    public final void warpParty(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp(mapId, 0);
            return;
        }
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void warpParty(final int mapId, final int portal) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            if (portal < 0) {
                warp(mapId);
            } else {
                warp(mapId, portal);
            }
            return;
        }
        final boolean rand = portal < 0;
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                if (rand) {
                    try {
                        curChar.changeMap(target, target.getPortal(Randomizer.nextInt(target.getPortals().size())));
                    } catch (Exception e) {
                        curChar.changeMap(target, target.getPortal(0));
                    }
                } else {
                    curChar.changeMap(target, target.getPortal(portal));
                }
            }
        }
    }

    public final void warpParty_Instanced(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp_Instanced(mapId);
            return;
        }
        final MapleMap target = getMap_Instanced(mapId);

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public void gainMeso(long gain) {
        c.getPlayer().gainMeso(gain, true, true);
    }

    public void gainExp(long gain) {
        c.getPlayer().gainExp(gain, true, true, true);
    }

    public void gainExpR(long gain) {
        c.getPlayer().gainExp(gain * c.getChannelServer().getExpRate(), true, true, true);
    }

    public final void givePartyItems(final int id, final short quantity, final List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(chr.getClient(), id, quantity, "Received from party interaction " + id + " (" + id2 + ")");
            } else {
                MapleInventoryManipulator.removeById(chr.getClient(), GameConstants.getInventoryType(id), id, -quantity, true, false);
            }
            chr.getClient().getSession().writeAndFlush(EffectPacket.showCharmEffect(c.getPlayer(), id, 1, true, ""));
        }
    }

    public void addPartyTrait(String t, int e, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.getTrait(MapleTraitType.valueOf(t)).addExp(e, chr);
        }
    }

    public void addPartyTrait(String t, int e) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            addTrait(t, e);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.getTrait(MapleTraitType.valueOf(t)).addExp(e, curChar);
            }
        }
    }

    public void addTrait(String t, int e) {
        getPlayer().getTrait(MapleTraitType.valueOf(t)).addExp(e, getPlayer());
    }

    public final void givePartyItems(final int id, final short quantity) {
        givePartyItems(id, quantity, false);
    }

    public final void givePartyItems(final int id, final short quantity, final boolean removeAll) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainItem(id, (short) (removeAll ? -getPlayer().itemQuantity(id) : quantity));
            return;
        }

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                gainItem(id, (short) (removeAll ? -curChar.itemQuantity(id) : quantity), false, 0, 0, "", curChar.getClient());
            }
        }
    }

    public final void givePartyExp_PQ(final int maxLevel, final double mod, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(chr.getLevel() > maxLevel ? (maxLevel + ((maxLevel - chr.getLevel()) / 10)) : chr.getLevel()) / (Math.min(chr.getLevel(), maxLevel) / 5.0) / (mod * 2.0));
            chr.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
        }
    }

    public final void gainExp_PQ(final int maxLevel, final double mod) {
        final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
        gainExp(amount * c.getChannelServer().getExpRate());
    }

    public final void givePartyExp_PQ(final int maxLevel, final double mod) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
            gainExp(amount * c.getChannelServer().getExpRate());
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(curChar.getLevel() > maxLevel ? (maxLevel + (curChar.getLevel() / 10)) : curChar.getLevel()) / (Math.min(curChar.getLevel(), maxLevel) / 10.0) / mod);
                curChar.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public final void givePartyExp(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
        }
    }

    public final void givePartyExp(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainExp(amount * c.getChannelServer().getExpRate());
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.gainExp(amount * c.getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public final void givePartyNX(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.modifyCSPoints(1, amount, true);
        }
    }

    public final void givePartyNX(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainNX(amount);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.modifyCSPoints(1, amount, true);
            }
        }
    }

    public final void endPartyQuest(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.endPartyQuest(amount);
        }
    }

    public final void endPartyQuest(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getPlayer().endPartyQuest(amount);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.endPartyQuest(amount);
            }
        }
    }

    public final void removeFromParty(final int id, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int possesed = chr.getInventory(GameConstants.getInventoryType(id)).countById(id);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(id), id, possesed, true, false);
                chr.getClient().getSession().writeAndFlush(EffectPacket.showCharmEffect(c.getPlayer(), id, 1, true, ""));
            }
        }
    }

    public final void removeFromParty(final int id) {
        givePartyItems(id, (short) 0, true);
    }

    public final void useSkill(final int skill, final int level) {
        if (level <= 0) {
            return;
        }
        SkillFactory.getSkill(skill).getEffect(level).applyTo(c.getPlayer(), false);
    }

    public final void useItem(final int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer(), true);
        c.getSession().writeAndFlush(InfoPacket.getStatusMsg(id));
    }

    public final void cancelItem(final int id) {
        c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(id), false, -1);
    }

    public final int getMorphState() {
        return c.getPlayer().getMorphState();
    }

    public final void removeAll(final int id) {
        c.getPlayer().removeAll(id);
    }

    public final void gainCloseness(final int closeness, final int index) {
        final MaplePet pet = getPlayer().getPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + (closeness * getChannelServer().getTraitRate()));
            getClient().getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
        }
    }

    public final void gainClosenessAll(final int closeness) {
        for (final MaplePet pet : getPlayer().getPets()) {
            if (pet != null && pet.getSummoned()) {
                pet.setCloseness(pet.getCloseness() + closeness);
                getClient().getSession().writeAndFlush(PetPacket.updatePet(c.getPlayer(), pet, getPlayer().getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), false, c.getPlayer().getPetLoot()));
            }
        }
    }

    public final void resetMap(final int mapid) {
        getMap(mapid).resetFully();
    }

    public final void openNpc(final int id) {
        c.removeClickedNPC();
        NPCScriptManager.getInstance().dispose(c);
        NPCScriptManager.getInstance().start(getClient(), id);
    }

    public final void openNpc(final String script) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().dispose(c);
        for (Entry<Integer, String> data : MapleLifeFactory.getNpcScripts().entrySet()) {
            if (data.getValue().equals(script)) {
                NPCScriptManager.getInstance().start(getClient(), data.getKey(), script);
            }
        }
    }

    public void openNpc(int npc, String script) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().dispose(c);
        NPCScriptManager.getInstance().start(c, npc, script);
    }

    public final void openNpc(final MapleClient cg, final int id) {
        cg.removeClickedNPC();
        NPCScriptManager.getInstance().dispose(cg);
        NPCScriptManager.getInstance().start(cg, id);
    }

    public final int getMapId() {
        return c.getPlayer().getMap().getId();
    }

    public final boolean haveMonster(final int mobid) {
        for (MapleMapObject obj : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
            final MapleMonster mob = (MapleMonster) obj;
            if (mob.getId() == mobid) {
                return true;
            }
        }
        return false;
    }

    public final boolean haveMonster() {
        int count = 0;
        for (MapleMonster monster : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
            if (monster.getId() == 9300216) {
                count++;
            }
        }
        return (c.getPlayer().getMap().getAllMonstersThreadsafe().size() - count) > 0;
    }

    public final int getChannelNumber() {
        return c.getChannel();
    }

    public final int getMonsterCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getNumMonsters();
    }

    public final void teachSkill(final int id, final int level, final byte masterlevel) {
        getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final void teachSkill(final int id, int level) {
        final Skill skil = SkillFactory.getSkill(id);
        if (getPlayer().getSkillLevel(skil) > level) {
            level = getPlayer().getSkillLevel(skil);
        }
        getPlayer().changeSingleSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    public final int getPlayerCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharactersSize();
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(c.getPlayer(), fromresting, c.getPlayer().getMap());
        }
        return Event_DojoAgent.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting, final int mapid) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(c.getPlayer(), fromresting, getMap(mapid));
        }
        return Event_DojoAgent.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final int dojo_getPts() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO);
    }

    public final MapleEvent getEvent(final String loc) {
        return c.getChannelServer().getEvent(MapleEventType.valueOf(loc));
    }

    public final int getSavedLocation(final String loc) {
        final Integer ret = c.getPlayer().getSavedLocation(SavedLocationType.fromString(loc));
        if (ret == null || ret == -1) {
            return ServerConstants.MainTown;
        }
        return ret;
    }

    public final void saveLocation(final String loc) {
        c.getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public final void saveReturnLocation(final String loc) {
        c.getPlayer().saveLocation(SavedLocationType.fromString(loc), c.getPlayer().getMap().getReturnMap().getId());
    }

    public final void clearSavedLocation(final String loc) {
        c.getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public final void showInstruction(final String msg, final int width, final int height) {
        c.getSession().writeAndFlush(CField.sendHint(msg, width, height));
    }

    public final String getInfoQuest(final int id) {
        return c.getPlayer().getInfoQuest(id);
    }

    public final void updateInfoQuest(final int id, final String data) {
        c.getPlayer().updateInfoQuest(id, data);
    }

    public final boolean getEvanIntroState(final String data) {
        return getInfoQuest(22013).equals(data);
    }

    public final void updateEvanIntroState(final String data) {
        updateInfoQuest(22013, data);
    }

    public final void Aran_Start() {
        c.getSession().writeAndFlush(CField.Aran_Start());
    }

    public final void evanTutorial(final String data, final int v1) {
        c.getSession().writeAndFlush(NPCPacket.getEvanTutorial(data));
    }

    public final void showWZEffect(final String data) {
        c.getSession().writeAndFlush(EffectPacket.showWZEffect(data));
    }

    public final void EarnTitleMsg(final String data) {
        c.getSession().writeAndFlush(CWvsContext.getTopMsg(data));
    }

    public final void EnableUI(final short i) {
        c.getSession().writeAndFlush(UIPacket.IntroEnableUI(i));
    }

    public final void DisableUI(final boolean enabled) {
        c.getSession().writeAndFlush(UIPacket.IntroDisableUI(enabled));
    }

    public final void MovieClipIntroUI(final boolean enabled) {
        c.getSession().writeAndFlush(UIPacket.IntroDisableUI(enabled));
        c.getSession().writeAndFlush(UIPacket.IntroLock(enabled));
    }

    public MapleInventoryType getInvType(int i) {
        return MapleInventoryType.getByType((byte) i);
    }

    public String getItemName(final int id) {
        return MapleItemInformationProvider.getInstance().getName(id);
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness, long period, short flags) {
        if (level > 30) {
            level = 30;
        }
        if (closeness > 30000) {
            closeness = 30000;
        }
        if (fullness > 100) {
            fullness = 100;
        }
        try {
            MapleInventoryManipulator.addById(c, id, (short) 1, "", MaplePet.createPet(id, name, level, closeness, fullness, MapleInventoryIdentifier.getInstance(), id == 5000054 ? (int) period : 0, flags), 45, "Pet from interaction " + id + " (" + id2 + ")" + " on " + FileoutputUtil.CurrentReadable_Date());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void removeSlot(int invType, byte slot, short quantity) {
        MapleInventoryManipulator.removeFromSlot(c, getInvType(invType), slot, quantity, true);
    }

    public void gainGP(final int gp) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.gainContribution(getPlayer().getGuildId(), gp); //1 for
    }

    public int getGP() {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return World.Guild.getGP(getPlayer().getGuildId()); //1 for
    }

    public void showMapEffect(String path) {
        getClient().getSession().writeAndFlush(CField.MapEff(path));
    }

    public int itemQuantity(int itemid) {
        return getPlayer().itemQuantity(itemid);
    }

    public EventInstanceManager getDisconnected(String event) {
        EventManager em = getEventManager(event);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if (eim.isDisconnected(c.getPlayer()) && eim.getPlayerCount() > 0) {
                return eim;
            }
        }
        return null;
    }

    public boolean isAllReactorState(final int reactorId, final int state) {
        boolean ret = false;
        for (MapleReactor r : getMap().getAllReactorsThreadsafe()) {
            if (r.getReactorId() == reactorId) {
                ret = r.getState() == state;
            }
        }
        return ret;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPlayer().getTruePosition());
    }

    // summon one monster, remote location
    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    // multiple monsters, remote location
    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    // handler for all spawnMonster
    public void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void sendNPCText(final String text, final int npc) {
        getMap().broadcastMessage(NPCPacket.getNPCTalk(npc, (byte) 0, text, "00 00", (byte) 0));
    }

    public boolean getTempFlag(final int flag) {
        return (c.getChannelServer().getTempFlag() & flag) == flag;
    }

    public void logPQ(String text) {
//	FileoutputUtil.log(FileoutputUtil.PQ_Log, text);
    }

    public void outputFileError(Throwable t) {
        FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, t);
    }

    public void trembleEffect(int type, int delay) {
        c.getSession().writeAndFlush(CField.trembleEffect(type, delay));
    }

    public int nextInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public MapleQuest getQuest(int arg0) {
        return MapleQuest.getInstance(arg0);
    }

    public final MapleInventory getInventory(int type) {
        return c.getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
    }

    public int randInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public void sendDirectionStatus(int key, int value) {
        c.getSession().writeAndFlush(UIPacket.getDirectionInfo(key, value));
        c.getSession().writeAndFlush(UIPacket.getDirectionStatus(true));
    }

    public void sendDirectionInfo(String data) {
        c.getSession().writeAndFlush(UIPacket.getDirectionInfo(data, 2000, 0, -100, 0, 0));
        c.getSession().writeAndFlush(UIPacket.getDirectionInfo(1, 2000));
    }

    public void addEquip(short pos, int itemid, short watk, short wdef, short mdef, byte upslot, short hp, short mp) {
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        Item item = MapleItemInformationProvider.getInstance().getEquipById(itemid);
        Equip eq = (Equip) item;
        eq.setWatk(watk);
        eq.setWdef(wdef);
        eq.setMdef(mdef);
        eq.setMp(mp);
        eq.setHp(hp);
        if (itemid == 1099004) {
            eq.setStr((short) 12);
            eq.setDex((short) 12);
        }
        if (itemid == 1098002) {
            eq.setStr((short) 7);
            eq.setDex((short) 7);
        }
        if (itemid == 1098003) {
            eq.setStr((short) 12);
            eq.setDex((short) 12);
        }
        eq.setUpgradeSlots(upslot);
        eq.setExpiration(-1);
        eq.setPosition(pos);
        equip.addFromDB(eq);
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));
    }

    public final void scheduleTimeMoveMap(final int destid, final int fromid, final int time, final boolean reset) {
        final MapleMap dest = c.getChannelServer().getMapFactory().getMap(destid);
        final MapleMap from = c.getChannelServer().getMapFactory().getMap(fromid);
        from.broadcastMessage(CField.getClock(time));
        from.setMapTimer(System.currentTimeMillis() + ((long) time) * 1000);
        CloneTimer tMan = CloneTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                List<MapleCharacter> chr = new ArrayList<MapleCharacter>();
                for (MapleMapObject chrz : from.getAllChracater()) {
                    chr.add((MapleCharacter) chrz);
                }
                for (MapleCharacter chrz : chr) {
                    chrz.setDeathCount((byte) 0);
                    chrz.changeMap(dest, dest.getPortal(0));
                }
                if (reset) {
                    from.resetFully();
                    from.resetReactors();
                    from.killAllMonsters(false);
                    from.setMapTimer(0);
                    for (final MapleMapObject i : from.getAllItems()) {
                        from.removeMapObject(i);
                    }
                }
            }
        };
        tMan.schedule(r, ((long) time) * 1000);
    }

    public final void scheduleTimeMoveDojang(final int destid, final int fromid, final int time, final boolean reset) {
        final MapleMap dest = c.getChannelServer().getMapFactory().getMap(destid);
        final MapleMap from = c.getChannelServer().getMapFactory().getMap(fromid);
        from.broadcastMessage(CField.getClock(time));
        from.setMapTimer(System.currentTimeMillis() + ((long) time) * 1000);
        CloneTimer tMan = CloneTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                List<MapleCharacter> chr = new ArrayList<MapleCharacter>();
                for (MapleMapObject chrz : from.getAllChracater()) {
                    chr.add((MapleCharacter) chrz);
                }
                for (MapleCharacter chrz : chr) {
                    chrz.setDeathCount((byte) 0);
                    chrz.changeMap(dest, dest.getPortal(0));
                    chrz.setKeyValue(3, "dojang_m", "-1");
                    chrz.setKeyValue(3, "dojang", "-1");
                    NPCScriptManager.getInstance().start(chrz.getClient(), 2007);
                }
                if (reset) {
                    from.resetFully();
                    from.resetReactors();
                    from.killAllMonsters(false);
                    from.setMapTimer(0);
                    for (final MapleMapObject i : from.getAllItems()) {
                        from.removeMapObject(i);
                    }
                }
            }
        };
        tMan.schedule(r, ((long) time) * 1000);
    }

    public void teleport(int portal) {
        c.getSession().writeAndFlush(CField.instantMapWarp(c.getPlayer(), (byte) portal));
        c.getPlayer().getMap().movePlayer(c.getPlayer(), new Point(c.getPlayer().getMap().getPortal(portal).getPosition()));
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public void fakeRelog() {
        c.getSession().writeAndFlush(CField.getCharInfo(c.getPlayer()));
        c.getPlayer().updateSkillPacket();
        c.getPlayer().updateLinkSkillPacket();
    }

    public void updateChar() {
        MapleMap currentMap = c.getPlayer().getMap();
        currentMap.removePlayer(c.getPlayer());
        currentMap.addPlayer(c.getPlayer());
    }

    public void updateKeyValue(int charid, int qid, String keyvalue) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE queststatus SET customData = ? WHERE quest = ? AND characterid = ?");
            ps.setString(1, keyvalue);
            ps.setInt(2, qid);
            ps.setInt(2, charid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException e) {
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
    }

    public String DojangText() {
        StringBuilder str = new StringBuilder().append("무릉도장 랭킹입니다. 상위 50등 까지만 랭킹에 보여집니다.\r\n");
        List<Triple<Integer, Integer, String>> data = DojoRank();
        for (int i = 0; i < data.size(); i++) {
            str.append((i + 1) + "등 : #b" + data.get(i).right + "#k #d" + data.get(i).left + "층#k " + data.get(i).mid + "초\r\n");
            if (i >= 99) {
                break;
            }
        }
        return str.toString();
    }

    public String charName(int id) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public List<Triple<Integer, Integer, String>> DojoRank() {
        List<Triple<Integer, Integer, String>> file = new ArrayList<>(); // floor, time, name
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM keyvalue WHERE `key` = ?");
            ps.setString(1, "dojo");
            rs = ps.executeQuery();
            while (rs.next()) {
                int dataz = Integer.parseInt(rs.getString("value"));

                ps1 = con.prepareStatement("SELECT * FROM keyvalue WHERE `key` = ? AND `id` = ?");
                ps1.setString(1, "dojo_time");
                ps1.setInt(2, rs.getInt("id"));
                rs1 = ps1.executeQuery();
                if (rs1.next()) {
                    int timez = Integer.parseInt(rs.getString("value"));
                    String name = charName(rs.getInt("id"));
                    if (dataz > 0 && timez > 0 && name != null) {
                        file.add(new Triple<>(dataz, timez, name));
                    }
                }
                rs1.close();
                ps1.close();
                if (file.size() == 50) {
                    break;
                }
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (ps1 != null) {
                    ps1.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (rs1 != null) {
                    rs1.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Triple<Integer, Integer, String> temp;

        for (int i = 0; i < file.size(); i++) {
            for (int j = i + 1; j < file.size(); j++) {
                if (file.get(i).left < file.get(j).left) {
                    temp = file.get(i);
                    file.set(i, file.get(j));
                    file.set(j, temp);
                } else if (file.get(i).left == file.get(j).left) {
                    if (file.get(i).mid > file.get(j).mid) {
                        temp = file.get(i);
                        file.set(i, file.get(j));
                        file.set(j, temp);
                    }
                }
            }
        }
        return file;
    }

    public void spawnMonster2(final int id, final int qty) {
        for (int i = 0; i < qty; i++) {
            MapleMonster m = MapleLifeFactory.getMonster(id);
            m.setHp((long) (m.getStats().getHp()));
            m.getStats().setHp((long) (m.getStats().getHp()));
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(m, c.getPlayer().getTruePosition());
        }
    }

    public void openNpcCustom(final MapleClient c, final int id, final String custom) {
        c.removeClickedNPC();
        NPCScriptManager.getInstance().dispose(c);
        NPCScriptManager.getInstance().start(getClient(), id, custom);
    }

    public final int checkWeddingReservation() {
        int ret = checkWeddingInternal();
        if (ret > 0) {
            return ret;
        }
        MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
        if (data.getWeddingStatus() > 0) {
            return 8;
        }
        return 0;
    }

    public final int checkWeddingStart() {
        return checkWeddingInternal();
    }

    /**
     * 결혼 가능 여부 검사. <br/><br/>
     * 0: 성공<br/>
     * 1: 파티가 없다<br/>
     * 2: 파티원이 2명이 아님<br/>
     * 3: 약혼되어있지 않음<br/>
     * 4: 신랑신부가 같은맵에 없음<br/>
     * 5: 신랑신부가 파티에 있어야함<br/>
     * 6: 약혼상태가 아님<br/>
     * 7: 이미 파혼됨<br/>
     *
     * @return 결과값
     */
    private int checkWeddingInternal() {
        if (c.getPlayer().getParty() == null) {
            return 1;
        }
        // 신랑과 신부가 파티를 하고 있는지 체크
        if (c.getPlayer().getParty().getMembers().size() != 2) {
            return 2;
        }
        if (c.getPlayer().getMarriageId() <= 0) {
            return 3;
        }
        MarriageDataEntry data = MarriageManager.getInstance().getMarriage(c.getPlayer().getMarriageId());
        if (data == null) {
            return 7;
        }
        boolean foundGroom = false;
        boolean foundBride = false;
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return 4;
            }
            if (chr.getId() == data.getGroomId()) {
                foundGroom = true;
            } else if (chr.getId() == data.getBrideId()) {
                foundBride = true;
            }
        }
        if (!foundGroom || !foundBride) {
            return 5;
        }
        if (data.getStatus() != 1) {
            return 6;
        }
        return 0;
    }

    public MarriageDataEntry getMarriageData() {
        return getMarriageData(c.getPlayer().getMarriageId());
    }

    public MarriageDataEntry getMarriageData(int marriageId) {
        return MarriageManager.getInstance().getMarriage(marriageId);
    }

    public MarriageEventAgent getMarriageAgent() {
        return getMarriageAgent(c.getChannel());
    }

    public MarriageEventAgent getMarriageAgent(int channel) {
        return MarriageManager.getInstance().getEventAgent(channel);
    }

    public void sendWeddingWishListInputDlg() {
        c.getSession().writeAndFlush(CWvsContext.showWeddingWishInputDialog());
    }

    public final int getInvSlots(final int i) {
        return (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNumFreeSlot());
    }

    public final void partyMessage(final int type, final String message) {
        if (c.getPlayer().getParty() != null) {
            World.Party.partyPacket(c.getPlayer().getParty().getId(), CWvsContext.serverNotice(type, "", message), null);
        }
    }

    public final int makeWeddingReservation(int itemId) {
        int ret = checkWeddingReservation();
        if (ret > 0) {
            return ret;
        }
        MarriageDataEntry data = getMarriageData(getPlayer().getMarriageId());
        data.setWeddingStatus(1);
        if (itemId == 5251004) {
            data.setTicketType(MarriageTicketType.CheapTicket);
        } else if (itemId == 5251005) {
            data.setTicketType(MarriageTicketType.SweetieTicket);
        } else if (itemId == 5251006) {
            data.setTicketType(MarriageTicketType.PremiumTicket);
        }

        final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(getPlayer().getGender() == 1 ? data.getGroomId() : data.getBrideId());
        if (chr != null) {
            NPCScriptManager.getInstance().start(chr.getClient(), 9201013);
        }
        sendWeddingWishListInputDlg();

        return 0;
    }

    public final void MakePmdrItem(int itemid, int count) {
        if (c.getPlayer().getKeyValue(800023, "indiepmer") <= 0) {
            c.getPlayer().setKeyValue(800023, "indiepmer", "0");
        }
        c.getPlayer().setKeyValue(800023, "indiepmer", String.valueOf((c.getPlayer().getKeyValue(800023, "indiepmer")) + count));
        while (c.getPlayer().getBuffedValue(80002387)) {
            c.getPlayer().cancelEffect(c.getPlayer().getBuffedEffect(80002387), false, -1);
        }
        SkillFactory.getSkill(80002387).getEffect(1).applyTo(c.getPlayer());
    }

    public final int rand(final int lbound, final int ubound) {
        return Randomizer.rand(lbound, ubound);
    }
}
