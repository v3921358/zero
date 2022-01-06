package server.maps;

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.handler.PlayerHandler;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import java.awt.Point;
import java.awt.Rectangle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import scripting.EventInstanceManager;
import scripting.EventManager;
import static scripting.NPCConversationManager.writeLog;
import server.AdelProjectile;
import server.MapleCarnivalFactory;
import server.MapleCarnivalFactory.MCSkill;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Obstacle;
import server.Randomizer;
import server.SpeedRunner;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.Timer.MapTimer;
import server.Timer.MobTimer;
import server.events.MapleEvent;
import server.field.boss.MapleBossManager;
import server.field.boss.demian.FlyingSwordNode;
import server.field.boss.demian.MapleDelayedAttack;
import server.field.boss.demian.MapleFlyingSword;
import server.field.boss.demian.MapleIncinerateObject;
import server.field.boss.lotus.MapleEnergySphere;
import server.field.boss.will.SpiderWeb;
import server.field.boss.will.WillPoison;
import server.field.skill.MapleFieldAttackObj;
import server.field.skill.MapleMagicWreck;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.MonsterDropEntry;
import server.life.OverrideMonsterStats;
import server.life.SpawnPoint;
import server.life.SpawnPointAreaBoss;
import server.life.Spawns;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.Environment;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MapleNodes.MonsterPoint;
import server.polofritto.MapleRandomPortal;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CField.AttackObjPacket;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.CWvsContext.PartyPacket;
import tools.packet.MobPacket;
import tools.packet.MobPacket.BossLucid;
import tools.packet.MobPacket.BossWill;
import tools.packet.PetPacket;
import tools.packet.SLFCGPacket;
import tools.packet.SecurityPCPacket;

public final class MapleMap {

    /*
     * Holds mappings of OID -> MapleMapObject separated by MapleMapObjectType.
     * Please acquire the appropriate lock when reading and writing to the LinkedHashMaps.
     * The MapObjectType Maps themselves do not need to synchronized in any way since they should never be modified.
     */
    private final Map<MapleMapObjectType, ConcurrentHashMap<Integer, MapleMapObject>> mapobjects;
    private final List<MapleCharacter> characters = new CopyOnWriteArrayList<MapleCharacter>();
    private int runningOid = 1;
    private final Lock runningOidLock = new ReentrantLock();
    private final List<Spawns> monsterSpawn = new ArrayList<Spawns>();
    private final Map<Integer, MaplePortal> portals = new HashMap<Integer, MaplePortal>();
    private MapleFootholdTree footholds = null;
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private float monsterRate, recoveryRate;
    private MapleMapEffect mapEffect;
    private String fieldType = "";
    private byte channel;
    private short decHP = 0, createMobInterval = 15000, top = 0, bottom = 0, left = 0, right = 0;
    private int consumeItemCoolTime = 0, protectItem = 0, decHPInterval = 10000, mapid, returnMapId, barrier, barrierArc, timeLimit, lucidCount = 0, lucidUseCount = 0,
            fieldLimit, maxRegularSpawn = 0, fixedMob, forcedReturnMap = 999999999, instanceid = -1, candles = 0, lightCandles = 0, reqTouched = 0,
            lvForceMove = 0, lvLimit = 0, permanentWeather = 0, partyBonusRate = 0, burning = 0, burningDecreasetime = 0, runeCurse = 0, stigmaDeath = 0;
    private boolean town, clock, personalShop, everlast = false, dropsDisabled = false, gDropsDisabled = false,
            soaring = false, squadTimer = false, isSpawns = true, checkStates = true, firstUserEnter = true, bingoGame = false, isEliteField = false;
    private String mapName, streetName, onUserEnter, onFirstUserEnter, speedRunLeader = "";
    private List<Integer> dced = new ArrayList<Integer>();
    private ScheduledFuture<?> squadSchedule, catchstart = null, eliteBossSchedule;
    private long speedRunStart = 0, lastSpawnTime = 0, lastHurtTime = 0, timer = 0, sandGlassTime = 0;
    public long lastStigmaTime = 0, lastIncinerateTime = 0, lastCapDropTime = 0, burningIncreasetime = 0;
    private MapleNodes nodes;
    private MapleSquadType squad;
    private List<MapleMagicWreck> wrecks = new ArrayList<>();
    private MapleRune rune;
    private Map<Integer, List<Integer>> monsterDefense = new LinkedHashMap<Integer, List<Integer>>();
    public String name[] = new String[10];
    public int voteamount = 0;
    public boolean dead = false, MapiaIng = false, eliteBossAppeared = false;
    public String names = "";
    public String deadname = "";
    public int MapiaChannel;
    public int aftertime, nighttime, votetime, nightnumber = 0, eliteRequire = 0, killCount = 0, eliteCount = 0;
    public int citizenmap1, citizenmap2, citizenmap3, citizenmap4, citizenmap5, citizenmap6, mapiamap, policemap, drmap, morningmap;
    public int playern;
    public int mbating;
    //루시드
    private long lastButterflyTime = 0;
    public int lucidButterflyCount = 0;
    public boolean isShootTime = false, isLucid3ParseStart = false;
    public byte lucidButterflyReSpawnGage = 5;

    public MapleMap(final int mapid, final int channel, final int returnMapId, final float monsterRate) {
        this.mapid = mapid;
        this.channel = (byte) channel;
        this.returnMapId = returnMapId;
        this.eliteRequire = Randomizer.rand(500, 1500);
        if (this.returnMapId == 999999999) {
            this.returnMapId = mapid;
        }
        if (GameConstants.getPartyPlay(mapid) > 0) {
            this.monsterRate = (monsterRate - 1.0f) * 2.5f + 1.0f;
        } else {
            this.monsterRate = monsterRate;
        }

        this.monsterRate *= 2;
        Map<MapleMapObjectType, ConcurrentHashMap<Integer, MapleMapObject>> objsMap = new ConcurrentHashMap<MapleMapObjectType, ConcurrentHashMap<Integer, MapleMapObject>>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            objsMap.put(type, new ConcurrentHashMap<Integer, MapleMapObject>());
        }
        mapobjects = Collections.unmodifiableMap(objsMap);
    }

    public final void setSpawns(final boolean fm) {
        this.isSpawns = fm;
    }

    public final boolean getSpawns() {
        return isSpawns;
    }

    public final void setFixedMob(int fm) {
        this.fixedMob = fm;
    }

    public final void setForceMove(int fm) {
        this.lvForceMove = fm;
    }

    public final int getForceMove() {
        return lvForceMove;
    }

    public final void setLevelLimit(int fm) {
        this.lvLimit = fm;
    }

    public final int getLevelLimit() {
        return lvLimit;
    }

    public final void setReturnMapId(int rmi) {
        this.returnMapId = rmi;
    }

    public final void setSoaring(boolean b) {
        this.soaring = b;
    }

    public void MapiaMorning(final MapleCharacter player) {
        broadcastMessage(CField.getClock(aftertime));
        final Timer m_timer = new Timer();
        TimerTask m_task = new TimerTask() {
            public void run() {
                m_timer.cancel();
                MapiaVote(player);
            }
        };
        m_timer.schedule(m_task, aftertime * 1000);
    }

    public void MapiaVote(final MapleCharacter player) {
        if (nightnumber == 0) {
            MapiaCompare(player);
        } else {
            broadcastMessage(CField.musicChange("Wps.img/VOTE"));
            broadcastMessage(CField.getClock(votetime));
            broadcastMessage(CWvsContext.serverNotice(5, "", "투표를 진행하시기 바랍니다. 제한시간은 30초 입니다."));
            names = "";
            for (MapleCharacter chr : getCharacters()) {
                names += chr.getName() + ",";
                chr.isVoting = true;
            }
            int i = 0;
            final Timer m_timer = new Timer();
            TimerTask m_task = new TimerTask() {
                public void run() {
                    m_timer.cancel();
                    MapiaCompare(player);

                }
            };
            m_timer.schedule(m_task, votetime * 1000);
        }
    }

    public void MapiaComparable(final MapleCharacter player) {
        //final List<MapleCharacter> players = new ArrayList<MapleCharacter>();
        //players.addAll(getCharacters());
        int playernum = 0;
        for (MapleCharacter chr : getCharacters()) {
            playernum++;
        }
        int i = 0;
        int ii = 0;
        int iii = 0;
        int citizen = 0;
        String deadname = "";
        String deadjob = "";
        String guessname = "";
        for (MapleCharacter chr : getCharacters()) {
            if (chr.getpolicevote == 1 && !chr.isDead) {
                if (chr.mapiajob == "마피아") {
                    iii++;
                }
            }
            if (chr.getmapiavote == 1 && !chr.isDead) {
                if (chr.getdrvote < 1 && !chr.isDead) {
                    chr.isDead = true;
                    deadname = chr.getName();
                    deadjob = chr.mapiajob;
                    chr.warp(910141020); //죽을시 가는맵
                    chr.dropMessage(1, "당신은 마피아에게 암살 당하였습니다.");
                    i++;
                } else {
                    chr.dropMessage(6, "의사가 당신을 살렸습니다.");
                    ii++;
                }
            }
            if (chr.mapiajob == "시민" && !chr.isDead) {
                citizen++;
            }
        }
        for (MapleCharacter chr : getCharacters()) {
            if (iii > 0) {
                chr.dropMessage(6, "경찰은 마피아를 찾았습니다.");
            } else {
                chr.dropMessage(5, "경찰은 마피아를 찾지 못하였습니다.");
            }
            if (i == 0) {
                if (ii > 0) {
                    chr.dropMessage(6, "의사는 마피아가 암살하려던 사람을 살렸습니다.");
                } else {
                    chr.dropMessage(5, "마피아는 아무도 죽이지 못하였습니다.");
                }
            } else {
                chr.dropMessage(5, "의사는 아무도 살리지 못했습니다.");
                chr.dropMessage(5, "마피아는 " + deadname + "님을 죽였습니다. 그의 직업은 " + deadjob + " 이었습니다.");
            }
        }

        if (citizen == 0) {// 마피아 승
            final Timer m_timer = new Timer();
            TimerTask m_task = new TimerTask() {
                public void run() {
                    m_timer.cancel();
                    MapiaWin(player);
                }
            };
            m_timer.schedule(m_task, 15 * 1000);
        } else {
            MapiaMorning(player);
        }
    }

    public void MapiaWin(MapleCharacter player) {
        long fuck = ChannelServer.getInstance(player.getClient().getChannel()).getMapFactory().getMap(234567899).mbating;
        int fuckingmapia = 0;
        ChannelServer.getInstance(player.getClient().getChannel()).getMapFactory().getMap(234567899).mbating = 0;
        MapiaIng = false;
        nightnumber = 0;
        int chan;
        if (MapiaChannel == 1) {
            chan = 20;
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(8, "", "[마피아 알림] " + chan + "세이상 채널에서 마피아의 승리로 게임이 종료 되었습니다."));
        } else {
            chan = MapiaChannel + 1;
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(8, "", "[마피아 알림] " + chan + "채널에서 마피아의 승리로 게임이 종료 되었습니다."));
        }
        int rand = Randomizer.rand(50, 100);
        for (MapleCharacter chr : getCharacters()) {
            if (chr.mapiajob.equals("마피아")) {
                fuckingmapia++;
            }
            chr.isDead = false;
            chr.isDrVote = false;
            chr.isMapiaVote = false;
            chr.isPoliceVote = false;
            chr.getdrvote = 0;
            chr.getmapiavote = 0;
            chr.getpolicevote = 0;
            chr.voteamount = 0;
            chr.dropMessage(5, "수고하셨습니다. 이번 게임은 마피아의 승리입니다!!");
        }
        for (MapleCharacter chr : getCharacters()) {
            if (chr.mapiajob.equals("마피아")) {
                chr.gainMeso((fuck / fuckingmapia), false);
                chr.dropMessage(6, "마피아 게임 승리 보상으로 " + (fuck / fuckingmapia) + "메소를 지급해드렸습니다.");
            }
            chr.warp(910141020); // 퇴장맵
        }
        return;
    }

    public void CitizenWin(MapleCharacter player) {
        long fuck = ChannelServer.getInstance(player.getClient().getChannel()).getMapFactory().getMap(234567899).mbating;
        int fucks = 0;
        ChannelServer.getInstance(player.getClient().getChannel()).getMapFactory().getMap(234567899).mbating = 0;
        MapiaIng = false;
        int chan;
        if (MapiaChannel == 1) {
            chan = 20;
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(8, "", "[마피아 알림] " + chan + "세이상 채널에서 시민의 승리로 게임이 종료 되었습니다."));
        } else {
            chan = MapiaChannel + 1;
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(8, "", "[마피아 알림] " + chan + " 채널에서 시민의 승리로 게임이 종료 되었습니다."));
        }
        int rand = Randomizer.rand(10, 80);
        int rand2 = Randomizer.rand(30, 100);
        for (MapleCharacter chr : getCharacters()) {
            if (!chr.mapiajob.equals("마피아")) {
                fuck += chr.mbating;
            }
            if (!chr.isDead) {
                fucks++;
            }
            chr.isDead = false;
            chr.isDrVote = false;
            chr.isMapiaVote = false;
            chr.isPoliceVote = false;
            chr.getdrvote = 0;
            chr.getmapiavote = 0;
            chr.getpolicevote = 0;
            chr.voteamount = 0;
            chr.dropMessage(5, "수고하셨습니다. 이번 게임은 시민의 승리입니다!!");
        }
        for (MapleCharacter chr : getCharacters()) {
            if (!chr.mapiajob.equals("마피아") && !chr.isDead) {
                chr.gainMeso((fuck / fucks), false);
                chr.dropMessage(6, "마피아 게임 승리 보상으로 " + (fuck / fucks) + "메소를 지급해드렸습니다.");
            }
            chr.warp(910141020); // 퇴장맵
        }
        nightnumber = 0;
        return;
    }

    public void MapiaCompare(MapleCharacter player) {
        int[] voteamount = new int[playern];
        String[] charinfo = new String[2];
        int j = 0;
        for (MapleCharacter chr : getCharacters()) {
            if (!chr.isDead) {
                voteamount[j] = chr.voteamount;
                j++;
            }
        }
        int mapia = 0;
        Arrays.sort(voteamount);
        try {
            for (MapleCharacter chr : getCharacters()) {
                if (chr.voteamount == voteamount[playern - 1]) {
                    charinfo[0] = chr.getName();
                    charinfo[1] = chr.mapiajob;
                }
            }
            if (voteamount[playern - 1] == voteamount[playern - 2]) {
                for (MapleCharacter chr : getCharacters()) {
                    if (nightnumber == 0) {
                        chr.dropMessage(6, "첫째날 낮이 지나고 밤이 찾아옵니다.");
                    } else {
                        chr.dropMessage(6, "투표 결과 아무도 죽지 않았습니다.");
                    }
                    chr.dropMessage(5, "잠시 후 밤이 됩니다.");
                }
                MapiaNight(player);
            } else {
                for (MapleCharacter chr : getCharacters()) {
                    if (charinfo[0] == chr.getName()) {
                        chr.dropMessage(1, "진행자>>당신은 투표 결과로 인해 처형당하였습니다.");
                        chr.isDead = true;
                    } else {
                        chr.dropMessage(6, "투표 결과 " + charinfo[0] + " 님이 처형당했습니다.");
                        chr.dropMessage(6, charinfo[0] + " 님의 직업은 " + charinfo[1] + " 입니다.");
                        chr.dropMessage(5, "잠시 후 밤이 됩니다.");
                    }
                    if (chr.mapiajob == "마피아" && !chr.isDead) {
                        mapia++;
                    }

                }

                if (mapia == 0) {
                    CitizenWin(player);
                } else {
                    MapiaNight(player);
                }

            }
        } catch (Exception e) {
            int chana;
            if (MapiaChannel == 1) {
                chana = 20;
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(8, "", "[마피아 알림] " + chana + "세이상 채널에서 게임이 다시 활성화 되었습니다."));
            } else {
                chana = MapiaChannel + 1;
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(8, "", "[마피아 알림] " + chana + " 채널에서 게임이 다시 활성화 되었습니다."));
            }
            MapiaIng = false;
            nightnumber = 0;
            for (MapleCharacter chr : getCharacters()) {
                chr.warp(ServerConstants.WarpMap);
                chr.dropMessage(1, "오류 입니다. 운영자에게 문의 해 주세요.");
            }
            return;
        }
    }

    public void MapiaNight(final MapleCharacter player) {
        final int maps[] = {citizenmap1, citizenmap2, citizenmap3, citizenmap4, citizenmap5, citizenmap6}; // 시민밤맵
        nightnumber++;
        final Timer m_timer = new Timer();
        final List<MapleCharacter> chars = new ArrayList<MapleCharacter>();
        TimerTask m_task = new TimerTask() {
            int status = 0;

            public void run() {
                int citizen = 0;
                if (status == 0) {
                    names = "";
                    for (MapleCharacter chr : getCharacters()) {
                        if (!chr.isDead) {
                            chars.add(chr);
                            names += chr.getName() + ",";
                            chr.isDrVote = false;
                            chr.isMapiaVote = false;
                            chr.isPoliceVote = false;
                            chr.getdrvote = 0;
                            chr.getmapiavote = 0;
                            chr.getpolicevote = 0;
                            chr.voteamount = 0;
                            if (chr.mapiajob == "시민") {
                                chr.warp(maps[citizen]);
                                chr.dropMessage(5, nightnumber + "번째 밤이 되었습니다. 마피아, 경찰, 의사가 투표를 모두 할때까지 잠시만 기다려 주세요.");
                                citizen++;
                            } else if (chr.mapiajob == "마피아") {
                                chr.warp(mapiamap);
                                chr.isMapiaVote = true;
                                chr.dropMessage(5, nightnumber + "번째 밤이 되었습니다. 바로 옆의 엔피시를 통해 암살할 사람을 지목해 주세요. 제한시간은 " + nighttime + "초 입니다.");
                            } else if (chr.mapiajob == "경찰") {
                                chr.warp(policemap);
                                chr.isPoliceVote = true;
                                chr.dropMessage(5, nightnumber + "번째 밤이 되었습니다. 바로 옆의 엔피시를 통해 마피아 일것 같다는 사람을 지목 해 주세요. 제한시간은 " + nighttime + "초 입니다.");
                            } else if (chr.mapiajob == "의사") {
                                chr.warp(drmap);
                                chr.isDrVote = true;
                                chr.dropMessage(5, nightnumber + "번째 밤이 되었습니다. 바로 옆의 엔피시를 통해 살리고 싶은 사람을 지목 해 주세요. 제한시간은 " + nighttime + "초 입니다.");
                            }
                            chr.getClient().getSession().writeAndFlush(CField.getClock(nighttime));
                        }
                    }
                    status = 1;
                } else if (status == 1) {
                    for (MapleCharacter chr : chars) {
                        if (!chr.isDead) {
                            chr.isVoting = false;
                            chr.warp(morningmap); //아침맵
                            chr.dropMessage(6, "아침이 되었습니다. 투표 결과를 발표하겠습니다.");
                        }
                    }
                    m_timer.cancel();
                    chars.clear();
                    MapiaComparable(player);
                }
            }

        };
        m_timer.schedule(m_task, 3000, nighttime * 1000);
    }

    public final boolean canSoar() {
        return soaring;
    }

    public final void toggleDrops() {
        this.dropsDisabled = !dropsDisabled;
    }

    public final void setDrops(final boolean b) {
        this.dropsDisabled = b;
    }

    public final void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public final int getId() {
        return mapid;
    }

    public final MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public final int getReturnMapId() {
        return returnMapId;
    }

    public final int getForcedReturnId() {
        return forcedReturnMap;
    }

    public final MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public final void setForcedReturnMap(final int map) {
        this.forcedReturnMap = map;
    }

    public final float getRecoveryRate() {
        return recoveryRate;
    }

    public final void setRecoveryRate(final float recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public final int getFieldLimit() {
        return fieldLimit;
    }

    public final void setFieldLimit(final int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public final String getFieldType() {
        return fieldType;
    }

    public final void setFieldType(final String fieldType) {
        this.fieldType = fieldType;
    }

    public final void setCreateMobInterval(final short createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public final void setTimeLimit(final int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public final void setMapName(final String mapName) {
        this.mapName = mapName;
    }

    public final String getMapName() {
        return mapName;
    }

    public final String getStreetName() {
        return streetName;
    }

    public final void setFirstUserEnter(final String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public final void setUserEnter(final String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public final String getFirstUserEnter() {
        return onFirstUserEnter;
    }

    public final String getUserEnter() {
        return onUserEnter;
    }

    public final boolean hasClock() {
        return clock;
    }

    public final void setClock(final boolean hasClock) {
        this.clock = hasClock;
    }

    public final boolean isTown() {
        return town;
    }

    public final void setTown(final boolean town) {
        this.town = town;
    }

    public final boolean allowPersonalShop() {
        return personalShop;
    }

    public final void setPersonalShop(final boolean personalShop) {
        this.personalShop = personalShop;
    }

    public final void setStreetName(final String streetName) {
        this.streetName = streetName;
    }

    public final void setEverlast(final boolean everlast) {
        this.everlast = everlast;
    }

    public final boolean getEverlast() {
        return everlast;
    }

    public final int getHPDec() {
        return decHP;
    }

    public final void setHPDec(final int delta) {
        if (delta > 0 || mapid == 749040100) { //pmd
            lastHurtTime = System.currentTimeMillis(); //start it up
        }
        decHP = (short) delta;
    }

    public final int getHPDecInterval() {
        return decHPInterval;
    }

    public final void setHPDecInterval(final int delta) {
        decHPInterval = delta;
    }

    public final int getHPDecProtect() {
        return protectItem;
    }

    public final void setHPDecProtect(final int delta) {
        this.protectItem = delta;
    }

    public final int getCurrentPartyId() {
        final Iterator<MapleCharacter> ltr = characters.iterator();
        MapleCharacter chr;
        while (ltr.hasNext()) {
            chr = ltr.next();
            if (chr.getParty() != null) {
                return chr.getParty().getId();
            }
        }
        return -1;
    }

    public final void addMapObject(final MapleMapObject mapobject) {
        runningOidLock.lock();
        int newOid;
        try {
            newOid = ++runningOid;
        } finally {
            runningOidLock.unlock();
        }

        mapobject.setObjectId(newOid);

        mapobjects.get(mapobject.getType()).put(newOid, mapobject);
    }

    private void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery) {
        addMapObject(mapobject);

        final Iterator<MapleCharacter> itr = characters.iterator();
        MapleCharacter chr;
        while (itr.hasNext()) {
            chr = itr.next();
            if ((mapobject.getType() == MapleMapObjectType.MIST || chr.getTruePosition().distanceSq(mapobject.getTruePosition()) <= GameConstants.maxViewRangeSq())) {
                if (mapobject.getType() == MapleMapObjectType.MONSTER) {
                    MapleMonster mob = (MapleMonster) mapobject;
                    if (mob.getOwner() != -1) {
                        if (mob.getOwner() == chr.getId()) {
                            packetbakery.sendPackets(chr.getClient());
                        }
                    } else {
                        packetbakery.sendPackets(chr.getClient());
                    }
                } else {
                    packetbakery.sendPackets(chr.getClient());
                }
                chr.addVisibleMapObject(mapobject);
            }
        }
    }

    public final void removeMapObject(final MapleMapObject obj) {
        mapobjects.get(obj.getType()).remove(Integer.valueOf(obj.getObjectId()));
    }

    public final Point calcPointBelow(final Point initial) {
        final MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            final double s1 = Math.abs(fh.getY2() - fh.getY1());
            final double s2 = Math.abs(fh.getX2() - fh.getX1());
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            } else {
                dropY = fh.getY1() + (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            }
        }
        if (initial.x < getLeft()) {
            initial.x = getLeft() + 100;
        } else if (initial.x > getRight()) {
            initial.x = getRight() - 100;
        }
        return new Point(initial.x, dropY);
    }

    public final Point calcDropPos(final Point initial, final Point fallback) {
        final Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(final MapleCharacter chr, final MapleMonster mob, final boolean instanced) {
        if (mob == null || chr == null || ChannelServer.getInstance(channel) == null) {// || dropsDisabled || mob.dropsDisabled() || chr.getPyramidSubway() != null) { //no drops in pyramid ok? no cash either
            return;
        }

        //We choose not to readLock for this.
        //This will not affect the internal state, and we don't want to
        //introduce unneccessary locking, especially since this function
        //is probably used quite often.
        if (!instanced && mapobjects.get(MapleMapObjectType.ITEM).size() >= 250) {
            removeDrops();
        }

        byte d = 1;
        Point pos = new Point(0, mob.getTruePosition().y);
        double showdown = 100.0;
        final MonsterStatusEffect mse = mob.getBuff(MonsterStatus.MS_Showdown);
        if (mse != null) {
            showdown += mse.getValue();
        }

        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        final List<MonsterDropEntry> derp = mi.retrieveDrop(mob.getId());
        final List<MonsterDropEntry> customs = new ArrayList<>();

        if (chr.getBuffedValue(25121133)) {
            customs.add(new MonsterDropEntry(2434851, 100000, 1, 1, 0));
        }

        if (chr.getKeyValue(20210113, "orgelonoff") == 1 && !chr.orgelTime) {
            customs.add(new MonsterDropEntry(2632800, 5500, 1, 1, 0));
        }

        // 기프트 쇼!쇼!쇼! 보상
        if (mob.getId() == 9833971) {
            customs.add(new MonsterDropEntry(4310237, 1000000, 1, 3, 0));
            customs.add(new MonsterDropEntry(4310237, 100000, 1, 3, 0));
            customs.add(new MonsterDropEntry(4310237, 100000, 1, 3, 0));
            customs.add(new MonsterDropEntry(4310266, 1000000, 1, 1, 0));
            customs.add(new MonsterDropEntry(4310266, 100000, 1, 1, 0));
        }

        // 엘리트 몬스터 드롭설정
        if (mob.getEliteGrade() > 0) {
            customs.add(new MonsterDropEntry(4310237, 1000000, 1, 3, 0));
            customs.add(new MonsterDropEntry(4310237, 100000, 1, 3, 0));
            customs.add(new MonsterDropEntry(4310237, 100000, 1, 3, 0));
            customs.add(new MonsterDropEntry(4310266, 1000000, 1, 2, 0));
        }

        /*if (mob.getScale() > 100) { // eliteMonster
            customs.add(new MonsterDropEntry(4001832, 1000000, 1, 100, 0));
            customs.add(new MonsterDropEntry(2711000, 1000000, 10, 20, 0));
            customs.add(new MonsterDropEntry(2431174, 1000000, 1, 10, 0));
            customs.add(new MonsterDropEntry(2711003, 500000, 1, 10, 0));
            customs.add(new MonsterDropEntry(2711004, 250000, 1, 10, 0));
            customs.add(new MonsterDropEntry(2470001, 100000, 1, 1, 0));
            customs.add(new MonsterDropEntry(2049004, 100000, 1, 1, 0));
        }

        if (chr.getV("arcane_quest_2") != null && Integer.parseInt(chr.getV("arcane_quest_2")) >= 0 && Integer.parseInt(chr.getV("arcane_quest_2")) < 6) {
            switch (mob.getId()) {
                case 8641000:
                    customs.add(new MonsterDropEntry(4034922, 1000000, 1, 1, 0));
                    break;
                case 8641001:
                    customs.add(new MonsterDropEntry(4034923, 1000000, 1, 1, 0));
                    break;
                case 8641002:
                    customs.add(new MonsterDropEntry(4034924, 1000000, 1, 1, 0));
                    break;
                case 8641003:
                    customs.add(new MonsterDropEntry(4034925, 1000000, 1, 1, 0));
                    break;
                case 8641004:
                    customs.add(new MonsterDropEntry(4034926, 1000000, 1, 1, 0));
                    break;
                case 8641005:
                    customs.add(new MonsterDropEntry(4034927, 1000000, 1, 1, 0));
                    break;
                case 8641006:
                    customs.add(new MonsterDropEntry(4034928, 1000000, 1, 1, 0));
                    break;
                case 8641007:
                    customs.add(new MonsterDropEntry(4034929, 1000000, 1, 1, 0));
                    break;
                case 8641008:
                    customs.add(new MonsterDropEntry(4034930, 1000000, 1, 1, 0));
                    break;
            }
        }

        if (chr.getV("arcane_quest_3") != null && Integer.parseInt(chr.getV("arcane_quest_3")) >= 0 && Integer.parseInt(chr.getV("arcane_quest_3")) < 4) {
            switch (mob.getId()) {
                case 8642000:
                case 8642001:
                case 8642002:
                case 8642003:
                case 8642004:
                case 8642005:
                case 8642006:
                case 8642007:
                case 8642008:
                case 8642009:
                case 8642010:
                case 8642011:
                case 8642012:
                case 8642013:
                case 8642014:
                case 8642015:
                    customs.add(new MonsterDropEntry(4036571, 1000000, 1, 1, 0));
                    break;
            }
        }

        if (chr.getV("arcane_quest_4") != null && Integer.parseInt(chr.getV("arcane_quest_4")) >= 0 && Integer.parseInt(chr.getV("arcane_quest_4")) < 4) {
            switch (mob.getId()) {
                case 8643000:
                case 8643001:
                case 8643002:
                case 8643003:
                case 8643004:
                case 8643005:
                case 8643006:
                case 8643007:
                case 8643008:
                case 8643009:
                case 8643010:
                case 8643011:
                case 8643012:
                    customs.add(new MonsterDropEntry(4036572, 1000000, 1, 1, 0));
                    break;
            }
        }

        if (chr.getV("arcane_quest_5") != null && Integer.parseInt(chr.getV("arcane_quest_5")) >= 0 && Integer.parseInt(chr.getV("arcane_quest_5")) < 4) {
            switch (mob.getId()) {
                case 8644000:
                case 8644001:
                case 8644002:
                case 8644003:
                case 8644004:
                case 8644005:
                case 8644006:
                    customs.add(new MonsterDropEntry(4036573, 1000000, 1, 1, 0));
                    break;
                case 8644007:
                case 8644008:
                case 8644009:
                case 8644010:
                    customs.add(new MonsterDropEntry(4036574, 1000000, 1, 1, 0));
                    break;
            }
        }

        if (chr.getV("arcane_quest_6") != null && Integer.parseInt(chr.getV("arcane_quest_6")) >= 0 && Integer.parseInt(chr.getV("arcane_quest_6")) < 4) {
            switch (mob.getId()) {
                case 8644400:
                    customs.add(new MonsterDropEntry(4036333, 1000000, 1, 1, 0));
                    break;
                case 8644401:
                    customs.add(new MonsterDropEntry(4036333, 1000000, 1, 1, 0));
                    break;
                case 8644402:
                    customs.add(new MonsterDropEntry(4036329, 1000000, 1, 1, 0));
                    customs.add(new MonsterDropEntry(4036330, 1000000, 1, 1, 0));
                    break;
                case 8644403:
                    customs.add(new MonsterDropEntry(4036330, 1000000, 1, 1, 0));
                    customs.add(new MonsterDropEntry(4036331, 1000000, 1, 1, 0));
                    break;
                case 8644404:
                    customs.add(new MonsterDropEntry(4036330, 1000000, 1, 1, 0));
                    break;
                case 8644405:
                    customs.add(new MonsterDropEntry(4036332, 1000000, 1, 1, 0));
                    break;
                case 8644406:
                    customs.add(new MonsterDropEntry(4036332, 1000000, 1, 1, 0));
                    break;
                case 8644407:
                    customs.add(new MonsterDropEntry(4036334, 1000000, 1, 1, 0));
                    break;
                case 8644410:
                    customs.add(new MonsterDropEntry(4036335, 1000000, 1, 1, 0));
                    break;
                case 8644412:
                    customs.add(new MonsterDropEntry(4036336, 1000000, 1, 1, 0));
                    break;
            }
        }

        if (chr.getV("arcane_quest_7") != null && Integer.parseInt(chr.getV("arcane_quest_7")) >= 0 && Integer.parseInt(chr.getV("arcane_quest_7")) < 4) {
            switch (mob.getId()) {
                case 8644500:
                    customs.add(new MonsterDropEntry(4036398, 1000000, 1, 1, 0));
                    break;
                case 8644501:
                    customs.add(new MonsterDropEntry(4036399, 1000000, 1, 1, 0));
                    break;
                case 8644502:
                    customs.add(new MonsterDropEntry(4036400, 1000000, 1, 1, 0));
                    break;
                case 8644503:
                    customs.add(new MonsterDropEntry(4036401, 1000000, 1, 1, 0));
                    break;
                case 8644504:
                    customs.add(new MonsterDropEntry(4036402, 1000000, 1, 1, 0));
                    break;
                case 8644505:
                    customs.add(new MonsterDropEntry(4036403, 1000000, 1, 1, 0));
                    break;
                case 8644506:
                    customs.add(new MonsterDropEntry(4036404, 1000000, 1, 1, 0));
                    break;
                case 8644507:
                    customs.add(new MonsterDropEntry(4036405, 1000000, 1, 1, 0));
                    break;
                case 8644508:
                    customs.add(new MonsterDropEntry(4036406, 1000000, 1, 1, 0));
                    break;
                case 8644509:
                    customs.add(new MonsterDropEntry(4036407, 1000000, 1, 1, 0));
                    break;
                case 8644510:
                    customs.add(new MonsterDropEntry(4036406, 1000000, 1, 1, 0));
                    break;
                case 8644511:
                    customs.add(new MonsterDropEntry(4036407, 1000000, 1, 1, 0));
                    break;
            }
        }*/
        int[] items = {1004422, 1004423, 1004424, 1004425, 1004426, 1052882, 1052887, 1052888, 1052889, 1052890, 1073030, 1073035, 1073032, 1073033, 1073034, 1082636, 1082637, 1082638, 1082639, 1082640, 1102775, 1102794, 1102795, 1102796, 1102797, 1152174, 1152179, 1152176, 1152177, 1152178, 1212115, 1213017, 1222109, 1232109, 1242116, 1242120, 1262017, 1272016, 1282016, 1292017, 1302333, 1312199, 1322250, 1332274, 1342101, 1362135, 1372222, 1382259, 1402251, 1412177, 1422184, 1432214, 1442268, 1452252, 1462239, 1472261, 1482216, 1492231, 1522138, 1532144, 1582017, 1592019};
        int[] items2 = {1004808, 1004809, 1004810, 1004811, 1004812, 1053063, 1053064, 1053065, 1053066, 1053067, 1073158, 1073159, 1073160, 1073161, 1073162, 1082695, 1082696, 1082697, 1082698, 1082699, 1102940, 1102941, 1102942, 1102943, 1102944, 1152196, 1152197, 1152198, 1152199, 1152200, 1212120, 1213018, 1222113, 1232113, 1242121, 1242122, 1262039, 1272017, 1282017, 1292018, 1302343, 1312203, 1322255, 1332279, 1342104, 1362140, 1372228, 1382265, 1402259, 1412181, 1422189, 1432218, 1442274, 1452257, 1462243, 1472265, 1482221, 1492235, 1522143, 1532150, 1582023, 1592020};

        if (mob.isExtreme()) {

            switch (mob.getId()) {
                case 8880177:
                case 8880302:
                    for (int item : items2) {
                        customs.add(new MonsterDropEntry(item, 125, 1, 1, 0));
                    }
                case 8950002:
                case 8880101:
                    for (int item : items) {
                        customs.add(new MonsterDropEntry(item, 250, 1, 1, 0));
                    }
                    break;
            }
        }

        if (getId() >= 410000500 && getId() <= 410002000) {
            customs.add(new MonsterDropEntry(1713000, 1000, 1, 1, 0));
        }

        if (getId() >= 410003000 && getId() <= 410004000) {
            customs.add(new MonsterDropEntry(1713000, 800, 1, 1, 0));
            customs.add(new MonsterDropEntry(1713001, 1000, 1, 1, 0));
        }

        List<MonsterDropEntry> finals = new ArrayList<>();
        finals.addAll(derp);
        finals.addAll(customs);

        double dropBuff = chr.getStat().dropBuff;

        if (!chr.getBuffedValue(80002282)) {
            dropBuff -= getRuneCurseDecrease();
        }

        //아획 배율설정         
        if (mob.getStats().isBoss()) {  //보스몬스터 최대 아획 확률
            dropBuff = Math.min(dropBuff, 300.0);
        } else { //그외 몬스터 최대 아획 확률
            dropBuff = Math.min(dropBuff, 400.0);
        }

        if (finals != null && !finals.isEmpty()) {
            Collections.shuffle(finals);

            boolean mesoDropped = false;
            for (final MonsterDropEntry de : finals) {
                try {
                    double droprate = 0.0;
                    if (de.itemId == mob.getStolen()) {
                        continue;
                    }
                    int chance = de.chance;
                    if (de.itemId == 2632800) {
                        if (chr.getHgrade() >= 2 && chr.getHgrade() <= 6) {
                            chance += chance / 2;
                        }
                    }
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP && mob.isExtreme()) {
                        droprate = (chance < 250 ? chance : 250 * (dropBuff / 100.0) * (showdown / 100.0)); // fixed
                    } else {
                        droprate = (chance * (dropBuff / 100.0) * (showdown / 100.0));
                    }
                    if (!mob.isExtreme() && de.questid == 1) {
                        continue;
                    }
                    if (mob.getStats().isBoss() || de.itemId == 2434851 || mob.getScale() > 100) {//mob.getStats().isPublicReward()) { // 개인 보상

                        if (chr.getParty() != null) {
                            for (MaplePartyCharacter pc : chr.getParty().getMembers()) {
                                if (pc.isOnline() && pc.getId() != chr.getId()) {
                                    MapleCharacter player = chr.getClient().getChannelServer().getPlayerStorage().getCharacterById(pc.getId());
                                    if (player != null) {
                                        if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP && mob.isExtreme()) {
                                            droprate = (chance < 250 ? chance : 250 * (dropBuff / 100.0) * (showdown / 100.0)); // fixed
                                        } else {
                                            droprate = (chance * (dropBuff / 100.0) * (showdown / 100.0));
                                        }
                                        if (Randomizer.nextInt(999999) < (int) droprate) {
                                            drop(mesoDropped, de, mob, player, pos, d);
                                            d++;
                                        }
                                    }
                                }
                            }
                        }

                        if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP && mob.isExtreme()) {
                            droprate = (chance < 250 ? chance : 250 * (dropBuff / 100.0) * (showdown / 100.0)); // fixed
                        } else {
                            droprate = (chance * (dropBuff / 100.0) * (showdown / 100.0));
                        }
                        if (Randomizer.nextInt(999999) < (int) droprate) {
                            drop(mesoDropped, de, mob, chr, pos, d);
                            d++;
                        }
                    } else {
                        if (Randomizer.nextInt(999999) < (int) droprate) {
                            drop(mesoDropped, de, mob, chr, pos, d);
                            d++;
                            if (de.itemId == 2632800) {
                                chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showOrgelEffect(chr, 80003023, mob.getPosition()));
                                chr.getClient().getSession().writeAndFlush(CField.EffectPacket.ErdaIncrease(mob.getPosition()));
                                chr.erdacount++;
                                chr.updateInfoQuest(100720, "count=" + chr.erdacount + ";fever=0;");
                                if (chr.erdacount == 99999) {
                                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062453, 1300, "마법오르골에 에르다가\r\n#r#e절반#n#k정도 모였어.", ""));
                                }
                                if (chr.erdacount == 99999) {
                                    chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062453, 1300, "마법오르골이 연주되기 #r#e직전#n#k이야!\r\n어떤 음악이 연주될까?", ""));
                                }
                                if (chr.erdacount == 99999) {
                                    chr.erdacount = 0;
                                    chr.updateInfoQuest(100720, "count=" + chr.erdacount + ";fever=0;");
                                    int a = Randomizer.rand(1, 100);
                                    if (a > 80) {
                                        //   chr.getClient().getSession().writeAndFlush(CField.EffectPacket.OrgelStart(80003023, 3));
                                        //  chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062453, 1300, "가슴이 두근거리는 #r경쾌한 음악#k!\r\n기다리고 있었어!", ""));
                                        //  chr.getClient().getSession().writeAndFlush(CField.UIPacket.detailShowInfo("마법오르골이 빛나며 더욱 경쾌한 음악이 연주됩니다.", false));
                                        //  chr.getClient().getSession().writeAndFlush(CField.EffectPacket.OrgelTime(80003023, 60000, 3));
                                    } else {
                                        //  chr.getClient().getSession().writeAndFlush(CField.EffectPacket.OrgelStart(80003023, 2));
                                        //  chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062453, 1300, "#b메이플 월드의 사람들!#k\r\n#b그란디스의 사람들!#k\r\n오르골의 연주을 들어!", ""));
                                        // chr.getClient().getSession().writeAndFlush(CField.UIPacket.GreendetailShowInfo("마법오르골이 빛나며 음악이 연주됩니다."));
                                        //   chr.getClient().getSession().writeAndFlush(CField.EffectPacket.OrgelTime(80003023, 60000, 2));
                                    }
                                    chr.orgelTime = false;
                                    server.Timer.MapTimer.getInstance().schedule(() -> {
                                        // chr.getClient().getSession().writeAndFlush(CField.EffectPacket.Orgel1(80003023));
                                        chr.orgelTime = false;
                                    }, 60 * 1000);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

    }

    public void drop(boolean mesoDropped, MonsterDropEntry de, MapleMonster mob, MapleCharacter chr, Point pos, byte d) {

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : chr.getParty() != null ? 1 : 0);
        final int mobpos = mob.getTruePosition().x, cmServerrate = ChannelServer.getInstance(channel).getMesoRate();
        Item idrop;

        if (mesoDropped && droptype != 3 && de.itemId == 0) { //not more than 1 sack of meso
            return;
        }
        if (de.questid > 0 && chr.getQuestStatus(de.questid) != 1) {
            return;
        }
        if (droptype == 3) {
            pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
        } else {
            pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
        }
        if (de.itemId == 0) { // meso

            //몹메소
            int min = 0;
            int max = 0;

            short mobLevel = mob.getStats().getLevel();
            if (mobLevel < 11) { // 1 ~ 10
                min = 50;
                max = 90;
            } else if (mobLevel < 21) { // 10 ~ 20
                min = 100;
                max = 200;
            } else if (mobLevel < 31) { // 20 ~ 30
                min = 300;
                max = 400;
            } else if (mobLevel < 41) { // 30 ~ 40
                min = 500;
                max = 700;
            } else if (mobLevel < 51) { // 40 ~ 50
                min = 800;
                max = 1000;
            } else if (mobLevel < 61) { // 50 ~ 60
                min = 1200;
                max = 1500;
            } else if (mobLevel < 71) { // 60 ~ 70
                min = 1600;
                max = 1800;
            } else if (mobLevel < 81) { // 70 ~ 80
                min = 2000;
                max = 2400;
            } else if (mobLevel < 91) { // 80 ~ 90
                min = 2200;
                max = 2600;
            } else if (mobLevel < 101) { // 90 ~ 100
                min = 2200;
                max = 2600;
            } else if (mobLevel < 111) { // 100 ~ 110
                min = 2400;
                max = 2800;
            } else if (mobLevel < 121) { // 110 ~ 120
                min = 2600;
                max = 3000;
            } else if (mobLevel < 131) { // 120 ~ 130
                min = 3000;
                max = 3500;
            } else if (mobLevel < 141) { // 130 ~ 140
                min = 3300;
                max = 3900;
            } else if (mobLevel < 151) { // 140 ~ 150
                min = 3800;
                max = 4200;
            } else if (mobLevel < 161) { // 150 ~ 160
                min = 4500;
                max = 5200;
            } else if (mobLevel < 171) { // 160 ~ 170
                min = 5000;
                max = 5800;
            } else if (mobLevel < 181) { // 170 ~ 180
                min = 5500;
                max = 6200;
            } else if (mobLevel < 191) { // 180 ~ 190
                min = 6000;
                max = 6800;
            } else if (mobLevel < 201) { // 190 ~ 200
                min = 6500;
                max = 7300;
            } else if (mobLevel < 211) { // 200 ~ 210
                min = 7000;
                max = 7800;
            } else if (mobLevel < 221) { // 210 ~ 220
                min = 7600;
                max = 8200;
            } else if (mobLevel < 231) { // 220 ~ 230
                min = 8500;
                max = 9200;
            } else if (mobLevel < 241) { // 230 ~ 240
                min = 9600;
                max = 11000;
            } else if (mobLevel < 251) { // 240 ~ 250
                min = 11500;
                max = 12500;
            } else if (mobLevel < 261) { // 250 ~ 260
                min = 12500;
                max = 13500;
            } else if (mobLevel < 271) { // 260 ~ 270
                min = 13500;
                max = 14500;
            } else if (mobLevel < 301) { // 270 ~ 300
                min = 14500;
                max = 15500;
            }

            int mesos = Randomizer.nextInt(1 + Math.abs(max - min)) + min;

            if (mesos > 0) {
                if (GameConstants.isLinkMap(chr.getMapId())) {
                    chr.gainMeso(mesos, true);
                } else {
                    //메획 배율설정  300.0 = 300%
                    double limit = 300.0;
                    double oriProp = chr.getStat().mesoBuff;

                    if (oriProp > limit)
                        oriProp = limit;
                    //     spawnMobMesoDrop((int) (mesos * (Math.min(chr.getStat().mesoBuff, 100.0) / 100.0) * chr.getDropMod() * cmServerrate), calcDropPos(pos, mob.getTruePosition()), mob, chr, false, droptype);
                    spawnMobMesoDrop((int) (mesos * (oriProp / 100.0) * chr.getDropMod() * cmServerrate), calcDropPos(pos, mob.getTruePosition()), mob, chr, false, droptype);
                }
                mesoDropped = true;
            }
        } else if (!GameConstants.isLinkMap(chr.getMapId())) {

            if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(de.itemId) == MapleInventoryType.DECORATION) {
                idrop = ((Equip) ii.getEquipById(de.itemId));

                if (mob.isExtreme()) {
                    int[] items = {1004422, 1004423, 1004424, 1004425, 1004426, 1052882, 1052887, 1052888, 1052889, 1052890, 1073030, 1073035, 1073032, 1073033, 1073034, 1082636, 1082637, 1082638, 1082639, 1082640, 1102775, 1102794, 1102795, 1102796, 1102797, 1152174, 1152179, 1152176, 1152177, 1152178, 1212115, 1213017, 1222109, 1232109, 1242116, 1242120, 1262017, 1272016, 1282016, 1292017, 1302333, 1312199, 1322250, 1332274, 1342101, 1362135, 1372222, 1382259, 1402251, 1412177, 1422184, 1432214, 1442268, 1452252, 1462239, 1472261, 1482216, 1492231, 1522138, 1532144, 1582017, 1592019};
                    int[] items2 = {1004808, 1004809, 1004810, 1004811, 1004812, 1053063, 1053064, 1053065, 1053066, 1053067, 1073158, 1073159, 1073160, 1073161, 1073162, 1082695, 1082696, 1082697, 1082698, 1082699, 1102940, 1102941, 1102942, 1102943, 1102944, 1152196, 1152197, 1152198, 1152199, 1152200, 1212120, 1213018, 1222113, 1232113, 1242121, 1242122, 1262039, 1272017, 1282017, 1292018, 1302343, 1312203, 1322255, 1332279, 1342104, 1362140, 1372228, 1382265, 1402259, 1412181, 1422189, 1432218, 1442274, 1452257, 1462243, 1472265, 1482221, 1492235, 1522143, 1532150, 1582023, 1592020};

                    for (int item : items) {
                        if (item == idrop.getItemId()) {
                            Equip equip = (Equip) idrop;
                            equip.addTotalDamage((byte) 10);
                        }
                    }

                    for (int item : items2) {
                        if (item == idrop.getItemId()) {
                            Equip equip = (Equip) idrop;
                            equip.addTotalDamage((byte) 10);
                        }
                    }

                    switch (mob.getId()) {
                        case 8950002:
                            if (idrop.getItemId() == 1012632) {
                                Equip equip = (Equip) idrop;
                                equip.addTotalDamage((byte) 10);
                            }
                            break;
                        case 8880101:
                            if (idrop.getItemId() == 1022278 || idrop.getItemId() == 1672077) {
                                Equip equip = (Equip) idrop;
                                equip.addTotalDamage((byte) 10);
                            }
                            break;
                        case 8880177:
                            if (idrop.getItemId() == 1132308) {
                                Equip equip = (Equip) idrop;
                                equip.addTotalDamage((byte) 10);
                            }
                            break;
                        case 8880302:
                            if (idrop.getItemId() == 1162080 || idrop.getItemId() == 1162081 || idrop.getItemId() == 1162082 || idrop.getItemId() == 1162083) {
                                Equip equip = (Equip) idrop;
                                equip.addTotalDamage((byte) 15);
                            }
                            break;
                    }
                }

            } else {
                final int range = Math.abs(de.Maximum - de.Minimum);
                idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(range <= 0 ? 1 : range) + de.Minimum : 1), (byte) 0);

                if (mob.isExtreme()) {
                    switch (mob.getId()) {
                        case 8950002:
                        case 8880101:
                        case 8880177:
                        case 8880302:
                            idrop.setQuantity((short) (idrop.getQuantity() * 2));
                            break;
                    }
                }
            }
            idrop.setGMLog(new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(mapid + "맵에서 " + mob.getId() + "를 잡고 얻은 아이템.").toString());
            if (mob.getStats().isBoss() || idrop.getItemId() == 2434851 || mob.getScale() > 100) {
                spawnMobPublicDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid);
            } else {
                spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid);
            }
        }
    }

    public void removeMonster(final MapleMonster monster) {
        if (monster == null) {
            return;
        }
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
        removeMapObject(monster);
        spawnedMonstersOnMap.decrementAndGet();
        monster.killed();
    }

    public void killMonster(final MapleMonster monster, int effect) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        monster.setHp(0);
        if (monster.getLinkCID() <= 0) {
            monster.spawnRevives(this);
        }
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), effect));
        removeMapObject(monster);
        spawnedMonstersOnMap.decrementAndGet();
        monster.killed();
    }

    public void killMonster(final MapleMonster monster) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        monster.setHp(0);
        if (monster.getLinkCID() <= 0) {
            monster.spawnRevives(this);
        }
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), monster.getStats().getSelfD() < 0 ? 1 : monster.getStats().getSelfD()));
        removeMapObject(monster);
        spawnedMonstersOnMap.decrementAndGet();
        monster.killed();
    }

    public void killAllMonster(MapleCharacter chr) {
        for (MapleMonster mob : getAllMonstersThreadsafe()) {
            killMonster(mob, chr, false, false, (byte) 0);
        }
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean second, byte animation) {
        killMonster(monster, chr, withDrops, second, animation, 0);
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, boolean withDrops, final boolean second, byte animation, final int lastSkill) {
        if ((monster.getId() == 8810122 || monster.getId() == 8810018) && !second) {
            MapTimer.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    killMonster(monster, chr, true, true, (byte) 1);
                    killAllMonsters(true);
                }
            }, 3000);
            return;
        }

        removeMapObject(monster);
        monster.killed();
        spawnedMonstersOnMap.decrementAndGet();
        chr.mobKilled(monster.getId(), lastSkill);
        final MapleSquad sqd = getSquadByMap();
        final boolean instanced = sqd != null || monster.getEventInstance() != null || getEMByMap() != null;

        try {

            int dropOwner = monster.killBy(chr, lastSkill);
            if (animation >= 0) {
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));
            }

            if (GameConstants.isLinkMap(mapid)) {
                int count = chr.getLinkMobCount();
                chr.setLinkMobCount(count - 1);
                chr.getClient().getSession().writeAndFlush(SLFCGPacket.FrozenLinkMobCount(count - 1));
                if (count <= 0) {
                    chr.setLinkMobCount(0);

                    for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
                        final MapleMonster mob = (MapleMonster) monstermo;
                        if (mob.getOwner() == chr.getId()) {
                            mob.setHp(0);
                            broadcastMessage(MobPacket.killMonster(mob.getObjectId(), 1));
                            removeMapObject(mob);
                            mob.killed();
                        }
                    }
                }
            }

            if (monster.getId() == 8820014) { //pb sponge, kills pb(w) first before dying
                killMonster(8820000);
            } else if (monster.getId() == 9300166) { //ariant pq bomb
                animation = 4; //or is it 3?
            }

            if (chr.getSkillLevel(GameConstants.isKain(chr.getJob()) ? 60030241 : 80003015) > 0) { //카인 링크 스킬
                PlayerHandler.HandleKainLink(chr, 1, 0);
            }

            if (GameConstants.isKain(chr.getJob()) && chr.getSkillLevel(400031066) > 0) {
                PlayerHandler.HandleGripOfAgony(chr, 1, 0);
            }

            int id = GameConstants.isLala(chr.getJob()) ? 160010001 : 80003058;
            int level = chr.getSkillLevel(id);
            if (level > 0) {
                SkillFactory.getSkill(80003070).getEffect(level).applyTo(chr, false);
            }
            if (chr.getBuffedValue(80003082) && chr.getBuffedValue(MapleBuffStat.EventSpecialSkill) == 1 && !monster.getStats().isBoss()) {

                chr.refreshGiftShowX3();

                int fullStack = 800;
                int halfStack = fullStack / 2;

                int afterStack = (int) chr.getKeyValue(100857, "count") + 1;
                int afterCnt = (int) chr.getKeyValue(100857, "feverCnt") + 1;

                if (afterCnt <= 10) {
                    if (afterStack <= fullStack) {
                        chr.setKeyValue(100857, "count", afterStack + "");
                        if (afterStack == halfStack) {
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062549, 3000, "#r기프트 쇼타임#k이 다가오고 있어.\r\n#r" + halfStack + "마리#k만 더 잡으면 되니 슬슬 준비해!", ""));
                        } else if (afterStack == fullStack) {
                            SkillFactory.getSkill(80003082).getEffect(chr.getSkillLevel(80003082)).applyTo(chr, false);
                            chr.getClient().getSession().writeAndFlush(CField.ShowEventSkillEffect(80003082, 2100));
                            chr.getClient().getSession().writeAndFlush(CField.enforceMSG(chr.getName() + "의 기프트 쇼!쇼!쇼! 소환된 선물들을 어서 찾아봐!", 345, 8000, true));
                            chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062549, 3000, "감사가 듬뿍! 선물이 듬뿍!\r\n#b" + chr.getName() + "#k의~\r\n#r기프트 쇼!쇼!쇼!#k", ""));

                            MapTimer.getInstance().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    chr.startGiftShowX3();
                                }
                            }, 2100);

                        }
                    }
                }
            }

            if (monster.getId() == 9833971) {
                int genMobKillCount = (int) chr.getKeyValue(100857, "genMobKillCount");

                if (genMobKillCount < 5) {
                    genMobKillCount++;
                    chr.setKeyValue(100857, "genMobKillCount", genMobKillCount + "");
                    if (genMobKillCount == 5)
                        chr.finishGiftShowX3(null);
                }
            }



//            if (chr.orgelTime) {
//                chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showOrgelEffect(chr, 80003023, monster.getPosition()));
//                chr.orgelcount--;
//                int a = (int) chr.getKeyValue(100711, "point") + 1;
//                chr.setKeyValue(100711, "point", a + "");
//                chr.updateInfoQuest(100711, "point=" + chr.getKeyValue(100711, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";today=0;total=0;lock=0"); //네오 스톤
//                chr.getClient().getSession().writeAndFlush(CField.EffectPacket.OrgelCount(80003023, chr.orgelcount));
//                if (chr.orgelcount == 0) {
//                    // chr.getClient().getSession().writeAndFlush(CField.EffectPacket.Orgel1(80003023));
//                    //  chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062453, 1300, "오르골이 연주되는 동안\r\n네오 스톤을 #r모두#k 찾았어", ""));
//                    chr.orgelcount = 99999;
//                    chr.orgelTime = false;
//                }
//            }

            int questid = 100417 + Integer.parseInt(chr.getClient().getKeyValue("current"));
            final MapleQuest quest = MapleQuest.getInstance(questid);
            final MapleQuestStatus status = chr.getQuestNAdd(quest);
            if (status.getStatus() == 1) {
                String a = chr.questmobkillcount < 100 && chr.questmobkillcount >= 10 ? "0" + chr.questmobkillcount : chr.questmobkillcount < 10 ? "00" + chr.questmobkillcount : "" + chr.questmobkillcount;
                if (questid == 100418) {
                    chr.questmobkillcount++;
                    chr.questmobkillcount = Math.min(chr.questmobkillcount, 300);
                    chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestMobKill(1, questid, a));
                    if (chr.questmobkillcount == 300) {
                        chr.getClient().setKeyValue("state", "2");
                        //     chr.questmobkillcount = 0;
                    }
                } else if (questid == 100419) {
                    chr.questmobkillcount = chr.getMonsterCombo() + 1;
                    chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestMobKill(1, questid, a));
                    chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(questid, "ComboK=" + chr.questmobkillcount + ";"));
                    if (chr.questmobkillcount == 100) {
                        chr.getClient().setKeyValue("state", "2");
                        //   chr.questmobkillcount = 0;
                    }
                } else if (questid == 100420) {
                    //if 일일보스 설정해서 보스잡으면
                    chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestMobKill(1, questid, "001"));
                    chr.getClient().setKeyValue("state", "2");
                } else if (questid == 100421) {
                    if (chr.getMap().getBarrier() > 0) {
                        chr.questmobkillcount++;
                        chr.questmobkillcount = Math.min(chr.questmobkillcount, 300);
                        chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestMobKill(1, questid, a));
                        if (chr.questmobkillcount == 300) {
                            chr.getClient().setKeyValue("state", "2");
                            //  chr.questmobkillcount = 0;
                        }
                    }
                } else if (questid == 100422) {
                    /*
                    //몬스터파크인데 걍 야매로 처리하삼 시ㅡ발 패킷모름
                    chr.dropMessage(-1, "몬스터파크 클리어 1/1");
                    //chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(questid, "mParkCount=1;"));
                    chr.getClient().setKeyValue("state", "2");
                    몬파아직
                     */
                } else if (questid == 100424) {
                    if (monster.getId() >= 8220022 && monster.getId() <= 8220026) { //elite boss
                        chr.questmobkillcount++;
                        chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestMobKill(1, questid, a));
                        if (chr.questmobkillcount == 3) {
                            chr.getClient().setKeyValue("state", "2");
                        }
                    }
                } else if (questid == 100426) {
                    if (chr.getMap().getBarrierArc() > 0) {
                        chr.questmobkillcount++;
                        chr.questmobkillcount = Math.min(chr.questmobkillcount, 300);
                        chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestMobKill(1, questid, a));
                        if (chr.questmobkillcount == 300) {
                            chr.getClient().setKeyValue("state", "2");
                        }
                    }
                }
            }

            if ((monster.getId() >= 8850000 && monster.getId() <= 8850004) || (monster.getId() >= 8850100 && monster.getId() <= 8850104)) {
                if (getMonsterById(8850011) != null) {
                    MapleMonster cygnus = getMonsterById(8850011);
                    broadcastMessage(CWvsContext.getTopMsg("시그너스가 자신의 심복이 당한것에 분노하여 모든것을 파괴하려 합니다."));
                    broadcastMessage(MobPacket.setAfterAttack(cygnus.getObjectId(), 4, 1, 17, cygnus.isFacingLeft()));
                } else if (getMonsterById(8850111) != null) {
                    MapleMonster cygnus = getMonsterById(8850111);
                    broadcastMessage(CWvsContext.getTopMsg("시그너스가 자신의 심복이 당한것에 분노하여 모든것을 파괴하려 합니다."));
                    broadcastMessage(MobPacket.setAfterAttack(cygnus.getObjectId(), 4, 1, 17, cygnus.isFacingLeft()));
                }
            }

            Date data = new Date();
            String month = data.getMonth() < 10 ? "0" + (data.getMonth() + 1) : String.valueOf((data.getMonth() + 1));
            String date2 = data.getDate() < 10 ? "0" + data.getDate() : String.valueOf(data.getDate());
            String date = (data.getYear() + 1900) + "" + month + "" + date2;
            Calendar now = Calendar.getInstance();
            String week = now.get(now.WEEK_OF_YEAR) + "";
            String weekdate = (data.getYear() + 1900) + "" + week; 
            String[] ilist = {"세르니움", "야영지", "아르카나", "헤이븐1", "소멸의여로", "츄츄", "레헬른", "모라스", "에스페라", "모라스", "에스페라", "문브릿지", "테네브리스"}; //일퀘
            for (String temp : ilist) { 
                if (chr.getKeyValue(201801, date + "_" + temp + "_isclear") == 2 && (chr.getKeyValue(201801, date + "_" + temp + "_count") < chr.getKeyValue(201801, date + "_" + temp + "_mobq"))) {
                    
                    if (GameConstants.getMonsterId(monster.getId()) == chr.getKeyValue(201801, date + "_" + temp + "_mobid")) {
                        chr.setKeyValue(201801, date + "_" + temp + "_count", String.valueOf(chr.getKeyValue(201801, date + "_" + temp + "_count") + 1));
                        chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getKeyValue(201801, date + "_" + temp + "_count") + " / " + chr.getKeyValue(201801, date + "_" + temp + "_mobq"));
                    }
                }
                if (chr.getKeyValue(201801, date + "_" + temp + "_isclear") == 5 && (chr.getKeyValue(201801, date + "_" + temp + "_count") < chr.getKeyValue(201801, date + "_" + temp + "_mobq"))) {
                    if (GameConstants.getMonsterId(monster.getId()) == chr.getKeyValue(201801, date + "_" + temp + "_mobid")) {
                        chr.setKeyValue(201801, date + "_" + temp + "_count", String.valueOf(chr.getKeyValue(201801, date + "_" + temp + "_count") + 1));
                        chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getKeyValue(201801, date + "_" + temp + "_count") + " / " + chr.getKeyValue(201801, date + "_" + temp + "_mobq"));
                    }
                }
            }
            /* Date data = new Date();
            String month = data.getMonth() < 10 ? "0" + (data.getMonth() + 1) : String.valueOf((data.getMonth() + 1));
            String date2 = data.getDate() < 10 ? "0" + data.getDate() : String.valueOf(data.getDate());
            String date = (data.getYear() + 1900) + "" + month + "" + date2;
            Calendar now = Calendar.getInstance();
            String week = now.get(now.WEEK_OF_YEAR) + "";
            String weekdate = (data.getYear() + 1900) + "" + week;

            // 일일퀘스트
            if (chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_isclear") == 0 && chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_count") < chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_mobq")) {
                chr.setKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_count", String.valueOf(chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_count") + 1));
                chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_count") + " / " + chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_mobq"));
                if (chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_count") >= chr.getKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_mobq")) {
                    chr.setKeyValue(Integer.parseInt(date), date + "_" + monster.getId() + "_isclear", String.valueOf(1));
                }
            }*/

 /* String[] qlist = {"river", "chewchew", "rehelen", "arcana", "moras", "esfera"};

            for (String qlis : qlist) {
                if (chr.getV(qlis + "_" + monster.getId() + "_isclear") != null && chr.getV(qlis + "_" + monster.getId() + "_count") != null && chr.getV(qlis + "_" + monster.getId() + "_mobq") != null) {
                    if (chr.getV(qlis + "_" + monster.getId() + "_isclear").equals("0") && Integer.parseInt(chr.getV(qlis + "_" + monster.getId() + "_count")) < Integer.parseInt(chr.getV(qlis + "_" + monster.getId() + "_mobq"))) {
                        int count = Integer.parseInt(chr.getV(qlis + "_" + monster.getId() + "_count")) + 1;
                        chr.addKV(qlis + "_" + monster.getId() + "_count", "" + count);
                        chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getV(qlis + "_" + monster.getId() + "_count") + " / " + chr.getV(qlis + "_" + monster.getId() + "_mobq"));
                        if (Integer.parseInt(chr.getV(qlis + "_" + monster.getId() + "_count")) >= Integer.parseInt(chr.getV(qlis + "_" + monster.getId() + "_mobq"))) {
                            chr.addKV(qlis + "_" + monster.getId() + "_isclear", "1");
                        }
                    }
                }
            }/*

            /*        if (chr.getKeyValue(191017, "travel_quest_prog") != -1) {
             long prog = chr.getKeyValue(191017, "travel_quest_prog");
             long pt = chr.getKeyValue(191017, "travel_quest_type");
             if (pt == 2) {
             if (chr.getKeyValue(191017, "travel_quest_qty") > chr.getKeyValue(191017, "travel_quest_kill")) {
             if (GameConstants.getMonsterId(monster.getId()) == chr.getKeyValue(191017, "travel_quest_need")) {
             chr.setKeyValue(191017, "travel_quest_kill", ""+(chr.getKeyValue(191017, "travel_quest_kill") + 1));
             chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getKeyValue(191017, "travel_quest_kill") + " / " + chr.getKeyValue(191017, "travel_quest_qty"));
             } else {
             //chr.dropMessage(6, "4."+ GameConstants.getMonsterId(monster.getId()) + "/" + chr.getKeyValue(191017, "travel_quest_need"));
             }
             }
             } else if (pt == 5) {
             for (int i = 0; i < chr.getKeyValue(191017, "travel_count"); i++) {
             if (chr.getKeyValue(191017, "travel_quest_qty_"+i) > chr.getKeyValue(191017, "travel_quest_kill_"+i)) {
             if (GameConstants.getMonsterId(monster.getId()) == chr.getKeyValue(191017, "travel_quest_need_"+i)) {
             chr.setKeyValue(191017, "travel_quest_kill_"+i, ""+(chr.getKeyValue(191017, "travel_quest_kill_"+i) + 1));
             chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getKeyValue(191017, "travel_quest_kill_"+i) + " / " + chr.getKeyValue(191017, "travel_quest_qty_"+i));
             } else {
             //chr.dropMessage(6, "4."+ GameConstants.getMonsterId(monster.getId()) + "/" + chr.getKeyValue(191017, "travel_quest_need"));
             }
             }
             }
             } else if (pt == 3) {
             if (chr.getKeyValue(191017, "travel_quest_qty_0") > chr.getKeyValue(191017, "travel_quest_kill_0")) {
             if (GameConstants.getMonsterId(monster.getId()) == chr.getKeyValue(191017, "travel_quest_need_0")) {
             chr.setKeyValue(191017, "travel_quest_kill_0", ""+(chr.getKeyValue(191017, "travel_quest_kill_0") + 1));
             chr.dropMessage(-1, monster.getStats().getName() + " " + chr.getKeyValue(191017, "travel_quest_kill_0") + " / " + chr.getKeyValue(191017, "travel_quest_qty_0"));
             } else {
             //chr.dropMessage(6, "4."+ GameConstants.getMonsterId(monster.getId()) + "/" + chr.getKeyValue(191017, "travel_quest_need"));
             }
             }
             }
             }
        
             //심화퀘스트 몹 코드 추가
        
             int[] questmob = {8641014, 8642012,8643012,8644008,8644410,8644508};
             for (int temp : questmob) {
             if (monster.getId() == temp)
             chr.setKeyValue(20190509, "mobkill_"+temp, ""+(chr.getKeyValue(20190509, "mobkill_"+temp) + 1));
             }
             if (chr.getV("d_mobcount") != null) {
             if (Long.parseLong(chr.getV("d_mobcount")) < 3000) {
             chr.addKV("d_mobcount", ""+(Long.parseLong(chr.getV("d_mobcount")) + 1));
             }
             } else {
             chr.addKV("d_mobcount", "1");
             }
             //todo 엘리트몹 체크
             if (monster.getScale() > 100) {
             if (chr.getV("d_elitemob") == null) {
             chr.addKV("d_elitemob", "1");
             chr.dropMessage(5, "엔젤플래닛 다이어리의 미션 중 하나를 클리어했다!");
             }
             }*/

 /*제로 일일미션*/
            if (chr.getKeyValue(20220311, "ove1") < 0) {
                chr.setKeyValue(20220311, "ove1", 0 + "");
            }
            if (chr.getKeyValue(20220311, "ove2") < 0) {
                chr.setKeyValue(20220311, "ove2", 0 + "");
            }
            if (chr.getKeyValue(20220311, "ove3") < 0) {
                chr.setKeyValue(20220311, "ove3", 0 + "");
            }
            if (chr.getKeyValue(20220311, "ove4") < 0) {
                chr.setKeyValue(20220311, "ove4", 0 + "");
            }
            if (chr.getKeyValue(20220311, "ove5") < 0) {
                chr.setKeyValue(20220311, "ove5", 0 + "");
            }

            if (chr.getLevel() - monster.getStats().getLevel() >= -50 && chr.getLevel() - monster.getStats().getLevel() <= 50) {
                if (chr.getKeyValue(20220311, "ove1") < 5000) {
                    chr.setKeyValue(20220311, "ove1", (int) chr.getKeyValue(20220311, "ove1") + 1 + "");
                    if (chr.getKeyValue(20220311, "ove1") == 5000) {
                        //chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 처지 몬스터 수 "+chr.getKeyValue(20220311,"ove1")+" 마리 달성 !!",0,4));
                    } else if (chr.getKeyValue(20220311, "ove1") % 100 == 0) {
                        //chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr,"#fn나눔고딕 ExtraBold##fs18#[도전 미션] 처지 몬스터 수 "+chr.getKeyValue(20220311,"ove1")+" 마리 달성 !!",0,6));
                    }
                }
            }

            if (chr.getBuffedEffect(MapleBuffStat.SoulMP) != null) { // 소울웨폰 게이지
                Item toDrop, weapon;
                toDrop = new Item(4001536, (byte) 0, (short) 1, (byte) 0);
                weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (weapon != null) {
                    spawnSoul(monster, chr, toDrop, monster.getPosition(), weapon);
                }
            }

            String name = null;

            switch (monster.getId()) {
                case 8880101:
                    name = "하드 데미안";
                    break;
                case 8880153:
                    name = "하드 루시드";
                    break;
                case 8950002:
                    name = "하드 스우";
                    break;
                case 8880302:
                    name = "하드 윌";
                    break;
                case 8880405:
                    name = "진 힐라";
                    break;
                case 8644655:
                    name = "카오스더스크";
                    break;
                case 8645009:
                    name = "듄켈";
                    break;
            }

            String name2 = null;

            switch (monster.getId()) {
                case 8800002:
                    name2 = "노말 자쿰";
                    break;
                case 8810122:
                    name2 = "카오스 혼테일";
                    break;
                case 8870000:
                    name2 = "노말 힐라";
                    break;
                case 8840014:
                    name2 = "하드 반레온";
                    break;
                case 8860000:
                    name2 = "노말 아카이럼";
                    break;
                case 8820001:
                    name2 = "노말 핑크빈";
                    break;
                case 8880002:
                    name2 = "노말 매그너스";
                    break;
                case 8500012:
                    name2 = "노말 파풀라투스";
                    break;
                case 8880200:
                    name2 = "카웅";
                    break;
                case 8900103: //상자
                    name2 = "노말 피에르";
                    break;
                case 8910100:
                    name2 = "노말 반반";
                    break;
                case 8920106:
                    name2 = "노말 블러드 퀸";
                    break;
                case 8930100:
                    name2 = "노말 벨룸";
                    break;
                case 8950102: // 노말 스우 3 페이지
                    name2 = "노말 스우";
                    break;
                case 8880111: // 노말 데미안 2 페이지
                    name2 = "노말 데미안";
                    break;
                case 8850011:
                    name2 = "노말 시그너스";
                    break;
                case 8880167:
                    name2 = "노말 루시드";
                    break;
                case 8880342: // 노말 윌 3 페이지
                    name2 = "노말 윌";
                    break;
                case 8644650: // 노말 더스크
                    name2 = "노말 더스크";
                    break;
                case 8900003: // 카오스 피에르 상자
                    name2 = "카오스 피에르";
                    break;
                case 8910000: // 카오스 반반
                    name2 = "카오스 반반";
                    break;
                case 8930000: // 카오스 벨룸
                    name2 = "카오스 벨룸";
                    break;
                case 8920006: // 카오스 블러드 퀸 상자
                    name2 = "카오스 블러드퀸";
                    break;
                case 8500022: // 하드 파풀라투스 2
                    name2 = "하드 파풀라투스";
                    break;
                case 8880000: // 하드 매그너스
                    name2 = "하드 매그너스";
                    break;
                case 8870100: // 하드 힐라
                    name2 = "하드 힐라";
                    break;
                case 8820200: // 카핑
                    name2 = "카오스 핑크빈";
                    break;
            }

            if (chr.getV("bossPractice") != null && chr.getV("bossPractice").equals("1")) {
                withDrops = false;
            }

            if (monster.isExtreme() && name != null) {
                name = new StringBuilder("익스트림 ").append(name).toString();
            }

            if (name != null && withDrops) {
                if (!chr.isGM()) {
                    // World.Broadcast.broadcastSmega(CField.getGameMessage(12, chr.getName() + "님의 파티가 " + name + " 보스를 격파하였습니다!"));
                }
                for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                    MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                    if (m != null) {
                        if (m.getKeyValue(20220311, "ove2") < 5) {
                            m.setKeyValue(20220311, "ove2", (int) m.getKeyValue(20220311, "ove2") + 1 + "");
                            //m.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(m,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 하드 보스 처치 "+m.getKeyValue(20220311,"ove2")+" 마리 달성 !!",0,4));
                        }
                    }
                }
            }

            if (name2 != null && withDrops) {
                if (!chr.isGM()) {
                    //  World.Broadcast.broadcastSmega(CField.getGameMessage(12, chr.getName() + "님의 파티가 " + name2 + " 보스를 격파하였습니다!"));
                }
                for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                    MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                    if (m != null) {
                        if (m.getKeyValue(20220311, "ove4") < 30) {
                            m.setKeyValue(20220311, "ove4", (int) m.getKeyValue(20220311, "ove4") + 1 + "");
                            // m.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(m,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 일반 보스 처치 "+m.getKeyValue(20220311,"ove4")+" 마리 달성 !!",0,4));
                        }
                    }
                }
            }

            if (monster.getId() == 8930000) {
                animation = 6;
                killMonster(8930001); //꼬리 제거
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 카오스 벨룸을 격파했습니다!"));
            }
            if (monster.getId() == 8880111) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 노말 데미안을 격파했습니다!"));
            }
            if (monster.getId() == 8880101) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 하드 데미안을 격파했습니다!"));
            }

            if (monster.getId() == 9101078) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 불꽃늑대 토벌에 성공 하였습니다."));
            }

            if (monster.getId() == 8881000) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 우르스 토벌에 성공 하였습니다."));
            }

            if (monster.getId() == 9440025) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 크로스 토벌에 성공 하였습니다."));
            }

            if (monster.getId() == 8880405) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 고통의 미궁 : 진힐라를 격파 하였습니다."));
            }

            if (monster.getId() == 9400080) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 텐구를 격파 하였습니다."));
            }
            if (monster.getId() == 8644650) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 노말 더스크를 격파 하였습니다."));
            }
            if (monster.getId() == 8644655) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 카오스더스크를 격파 하였습니다."));
            }
            if (monster.getId() == 8645009) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 듄켈을 격파 하였습니다."));
            }

            if (monster.getId() == 9460030) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 진격의 거인을 격파 하였습니다."));
            }

            if (monster.getId() == 8950002) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 하드 스우를 격파했습니다!"));
            }
            if (monster.getId() == 8950102) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 노말 스우를 격파했습니다!"));
            }
            if (monster.getId() == 8880151) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 노말 루시드를 격파했습니다!"));
                final MapleMap map = chr.getClient().getChannelServer().getMapFactory().getMap(450004300);
                MapleMonster monsters = MapleLifeFactory.getMonster(8880167);
                map.spawnMonsterOnGroundBelow(monsters, new Point(80, 36));
            }
            if (monster.getId() == 8880153) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 하드 루시드를 격파했습니다!"));
                final MapleMap map = chr.getClient().getChannelServer().getMapFactory().getMap(450004300);
                MapleMonster monsters = MapleLifeFactory.getMonster(8880177);
                map.spawnMonsterOnGroundBelow(monsters, new Point(80, 36));
            }
            if (monster.getId() == 8880342) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 노말 윌을 격파했습니다!"));
            }
            if (monster.getId() == 8880302) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 하드 윌을 격파했습니다!"));
            }
            if (monster.getId() == 8880504) {

                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 검은 마법사를 격파했습니다!"));
            }
            if (monster.getId() == 8880614) {
                World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[" + chr.getParty().getLeader().getName() + "]님의 파티가 세렌을 격파했습니다!"));
            }
            if (lastSkill == 400021069 && !chr.getBuffedValue(32121056)) {
                MapleStatEffect reaper = chr.getBuffedEffect(400021069);
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                int localDuration = (int) (chr.getBuffLimit(400021069) + reaper.getT() * 1000);

                MapleBuffStatValueHolder mbsvh = chr.checkBuffStatValueHolder(MapleBuffStat.IndieSummon, 400021069);

                mbsvh.localDuration = localDuration;

                mbsvh.schedule.cancel(false);

                final CancelEffectAction cancelAction = new CancelEffectAction(chr, reaper, System.currentTimeMillis(), MapleBuffStat.IndieSummon);
                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                    cancelAction.run();
                }, localDuration);

                mbsvh.schedule = schedule;
                statups.put(MapleBuffStat.IndieSummon, new Pair<>(400021069, localDuration));

                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, reaper, chr));
            }

            if (lastSkill == 2111010 || monster.isBuffed(2111010)) {
                final MapleSummon tosummon = new MapleSummon(chr, 2111010,
                        monster.getTruePosition(), SummonMovementType.WALK_STATIONARY, (byte) 0, (int) chr.getBuffLimit(2111010));

                if (getAllSummons(2111010).size() < 10) {
                    chr.getMap().spawnSummon(tosummon, (int) chr.getBuffLimit(2111010));
                    chr.addSummon(tosummon);
                    if (getAllSummons(2111010).size() < 10) {
                        chr.getMap().spawnSummon(tosummon, (int) chr.getBuffLimit(2111010));
                        chr.addSummon(tosummon);
                    }
                }
            }
            if (monster.getId() == 8880010 || monster.getId() == 8880002 || monster.getId() == 8880000) {
                broadcastMessage(CWvsContext.getTopMsg("매그너스가 사망하여 더이상 구와르의 힘이 방출되지 않습니다."));
            }
            if (monster.getId() == 8850111 || monster.getId() == 885011) {
                broadcastMessage(CWvsContext.getTopMsg("시그너스를 퇴치하셨습니다. 좌측의 출구를 통해 퇴장하실 수 있습니다."));
            }
            if (monster.getId() == 8930000) {
                animation = 6;
                killMonster(8930001); //꼬리 제거
            }

            if (monster.getId() == 8880300 || monster.getId() == 8880303 || monster.getId() == 8880304 || monster.getId() == 8880340 || monster.getId() == 8880343 || monster.getId() == 8880344) {
                broadcastMessage(CField.enforceMSG("윌이 진지해졌네요. 거울 속 깊은 곳에 윌의 진심이 비춰질 것 같아요.", 245, 7000));
            }

            if (monster.getId() == 8880301 || monster.getId() == 8880341) {
                broadcastMessage(CField.enforceMSG("윌이 여유가 없어졌군요. 거울 세계의 가장 깊은 곳이 드러날 것 같아요.", 245, 7000));
            }

            if (monster.getId() == 8880505) {
                killAllMonsters(false);
                broadcastMessage(CField.enforceMSG("창조와 파괴의 기사가 쓰러져 검은 마법사에게로 가는 길이 열린다.", 265, 3000));
            }

            if (monster.getId() == 8880342 || monster.getId() == 8880302) {
                broadcastMessage(BossWill.willSpider(5, null));
            }

            if (!FieldLimitType.Event.check(getFieldLimit()) && monster.getId() != 9833971) {
                if (monster.getScale() > 100) { // eliteMonster
                    broadcastMessage(CField.startMapEffect(this.eliteCount <= 15 ? "어두운 기운이 사라지지 않아 이곳을 음산하게 만들고 있습니다." : "이곳이 어둠으로 가득차 곧 무슨일이 일어날 듯 합니다.", 5120124, true));
                } else {
                    if (killCount++ >= eliteRequire) {
                        if (this.eliteCount < 20 && (monster.getStats().getLevel() >= chr.getLevel() - 20 && monster.getStats().getLevel() <= chr.getLevel() + 20) && !monster.getStats().isBoss()) {
                            this.eliteCount++;
                            this.killCount = 0;
                            this.eliteRequire = Randomizer.rand(3000, 5000);

                            MapleMonster eliteMonster = MapleLifeFactory.getMonster(monster.getId());
                            eliteMonster.setScale(200);
                            int scale = Randomizer.nextInt(3);
                            eliteMonster.setEliteGrade(scale);
                            eliteMonster.setEliteType(1);

                            switch (scale) {
                                case 0:
                                    eliteMonster.setHp((long) (eliteMonster.getHp() * 15 * eliteMonster.bonusHp()));
                                    break;
                                case 1:
                                    eliteMonster.setHp((long) (eliteMonster.getHp() * 20 * eliteMonster.bonusHp()));
                                    break;
                                case 2:
                                    eliteMonster.setHp((long) (eliteMonster.getHp() * 30 * eliteMonster.bonusHp()));
                                    break;
                            }

                            List<Pair<Integer, Integer>> skills = new ArrayList<>();

                            skills.add(new Pair<>(134, 7));
                            skills.add(new Pair<>(135, 7));
                            skills.add(new Pair<>(136, 12));
                            skills.add(new Pair<>(211, 12));
                            skills.add(new Pair<>(212, 4));

                            eliteMonster.getEliteGradeInfo().add(skills.get(Randomizer.nextInt(skills.size())));

                            spawnMonsterOnGroundBelow(eliteMonster, monster.getTruePosition());
                            broadcastMessage(CField.startMapEffect("어두운 기운과 함께 강력한 몬스터가 출현합니다.", 5120124, true));
                            broadcastMessage(CField.specialMapSound("Field.img/eliteMonster/Regen"));
                        }
                    }
                }
            }

        } catch (Exception e) {
            FileoutputUtil.log(FileoutputUtil.Kill_Log, "" + e);
        }

        int map = 0, nextmob = 0;
        Point poz = null;

        /*if (monster.getId() == 8950000) {
         map = 350060180;
         } else if (monster.getId() == 8950001) {
         map = 350060200;
         } else 
         if (monster.getId() == 8950100) {
         map = 350060190;
         } else if (monster.getId() == 8950101) {
         map = 350060210;
         } else
         if (monster.getId() == 8880140) {
         map = 450004250;
         nextmob = 8880150;
         poz = new Point(701, -194);
         } else if (monster.getId() == 8880141) {
         map = 450004850;
         nextmob = 8880151;
         poz = new Point(701, -194);
         } else if (monster.getId() == 8880151) {
         map = 450004850;
         nextmob = 8880153;
         poz = new Point(701, -194);
         broadcastMessage(CField.getClock(45000)); // 45초
         setMapTimer(System.currentTimeMillis() + (45) * 1000);
         CloneTimer tMan = CloneTimer.getInstance();
         Runnable r = new Runnable() {
         @Override
         public void run() {
         List<MapleCharacter> chr = new ArrayList<MapleCharacter>();
         for (MapleMapObject chrz : getAllChracater()) {
         chr.add((MapleCharacter) chrz);
         }
         for (MapleCharacter chrz : chr) {
         chrz.setDeathCount((byte) 0);
         MapleMap dest = ChannelServer.getInstance(chrz.getClient().getChannel()).getMapFactory().getMap(1000000);
         chrz.changeMap(dest, dest.getPortal(0));
         chrz.dropMessage(6, "[알림] 시간이 초과되어 자동으로 퇴장되었습니다.");
         }
         }
         };
         tMan.schedule(r, (45) * 1000);
         } else if (monster.getId() == 8880150) {
         map = 450004300;
         nextmob = 8880167;
         poz = new Point(80, 36);
         } else if (monster.getId() == 8880153) {
         map = 450004900;
         nextmob = 8880177;
         poz = new Point(80, 36);
         } else */
        if (monster.getId() == 8910001) {
            broadcastMessage(CWvsContext.getTopMsg("시공간 붕괴 실패! 잠시 후, 원래 세계로 돌아갑니다."));
            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() { // 임시로
                    for (MapleCharacter chr : getAllCharactersThreadsafe()) {
                        MapleMap zz = ChannelServer.getInstance(chr.getClient().getChannel()).getMapFactory().getMap(chr.getMapId() - 10);
                        chr.changeMap(zz, zz.getPortal(0));
                    }
                }
            }, 3000);
        }

        if (monster.getId() / 1000 == 9800 && monster.getMobExp() == 0) {
            chr.getClient().getSession().writeAndFlush(CField.removeMapEffect());
            chr.getClient().getSession().writeAndFlush(CField.startMapEffect("누적된 경험치 보상은 맵 퇴장시 한꺼번에 적용됩니다.", 5120162, true));
        }

        if (chr.getBuffedValue(MapleBuffStat.BMageDeath) != null) {
            chr.setDeath((byte) (chr.getDeath() + 1));

            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.BMageDeath, new Pair<>((int) chr.getDeath(), 0));
            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.BMageDeath), chr));
        }

        if (monster.getId() >= 8920000 && monster.getId() <= 8920003 && withDrops && getNumMonsters() == 0) {
            MapleMonster mob = MapleLifeFactory.getMonster(8920006);
            mob.setPosition(calcPointBelow(new Point(34, 134)));
            spawnMonster(mob, -2);
        } else if (monster.getId() >= 8900000 && monster.getId() <= 8900002 && getNumMonsters() == 0) {
            MapleMonster mob = MapleLifeFactory.getMonster(8900003);
            mob.setPosition(calcPointBelow(new Point(570, 550)));
            spawnMonster(mob, -2);
        } else if (monster.getId() >= 8920100 && monster.getId() <= 8920103 && withDrops && getNumMonsters() == 0) {
            spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8920106), new Point(34, 135));
        } else if (monster.getId() >= 8900100 && monster.getId() <= 8900102 && withDrops && getNumMonsters() == 0) {
            spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8900103), new Point(570, 551));
        }

        int[] linkMobs = {9010152, 9010153, 9010154, 9010155, 9010156, 9010157, 9010158, 9010159, 9010160, 9010161, 9010162, 9010163, 9010164, 9010165, 9010166, 9010167,
            9010168, 9010169, 9010170, 9010171, 9010172, 9010173, 9010174, 9010175, 9010176, 9010177, 9010178, 9010179, 9010180, 9010181};

        for (int linkMob : linkMobs) {
            if (monster.getId() == linkMob) {
                if (chr.getLinkMobCount() <= 0) {
                    for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
                        final MapleMonster mob = (MapleMonster) monstermo;
                        if (mob.getOwner() == chr.getId()) {
                            mob.setHp(0);
                            broadcastMessage(MobPacket.killMonster(mob.getObjectId(), 1));
                            removeMapObject(mob);
                            mob.killed();
                        }
                    }
                } else {
                    MapleMonster newMob = MapleLifeFactory.getMonster(monster.getId());
                    newMob.setOwner(chr.getId());
                    spawnMonsterOnGroundBelow(newMob, monster.getTruePosition());
                }
                break;
            }
        }

        if ((chr.getMapId() - 925070000) > 0 && (chr.getMapId() - 925070000) <= 8000) {
            int floor = (chr.getMapId() - 925070000) / 100;
            if (floor > 30 && floor < 38 && chr.mCount < 1) {
                chr.mCount++;
                int id = 0;
                long hp = 0;
                switch (floor) {
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
                }
                if (id > 0) {
                    MapleMonster mob = MapleLifeFactory.getMonster(id);
                    if (mob != null) {
                        mob.setHp(hp);
                        mob.getStats().setHp(hp);
                        server.Timer.MapTimer.getInstance().schedule(() -> {
                            spawnMonsterOnGroundBelow(mob, new Point(Randomizer.nextBoolean() ? -304 : 185, 7));
                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_AddEffect, new MonsterStatusEffect(0, Short.MAX_VALUE), 0, null);
                        }, 2000);
                    }
                }
            } else if (floor >= 38 && floor < 40 && chr.mCount < 2) {
                chr.mCount++;
                int id = 0;
                long hp = 0;
                switch (floor) {
                    case 38:
                        id = 9305634;
                        hp = 6474960000L;
                        break;
                    case 39:
                        id = 9305656;
                        hp = 7971840000L;
                        break;
                }
                if (id > 0) {
                    MapleMonster mob = MapleLifeFactory.getMonster(id);
                    if (mob != null) {
                        mob.setHp(hp);
                        mob.getStats().setHp(hp);
                        server.Timer.MapTimer.getInstance().schedule(() -> {
                            spawnMonsterOnGroundBelow(mob, new Point(Randomizer.nextBoolean() ? -304 : 185, 7));
                            mob.applyStatus(chr.getClient(), MonsterStatus.MS_AddEffect, new MonsterStatusEffect(0, Short.MAX_VALUE), 0, null);
                        }, 2000);
                    }
                }
            } else {
                if (getAllMonster().size() == 0) {
                    MapleMonster mob = MapleLifeFactory.getMonster(9300216);
                    spawnMonsterOnGroundBelow(mob, new Point(Randomizer.nextBoolean() ? -304 : 185, 7));
                    chr.mCount = 0;
                    chr.setDojoStopTime(System.currentTimeMillis());
                    chr.addKV("dojo", String.valueOf(floor));
                    chr.addKV("dojo_time", String.valueOf((int) ((System.currentTimeMillis() - chr.getDojoStartTime() - chr.getDojoCoolTime()) / 1000)));
                    chr.dropMessage(-1, "상대를 격파하였습니다. 10초간 타이머가 정지됩니다.");
                    chr.getClient().getSession().writeAndFlush(CField.environmentChange("Dojang/clear", 5));
                    chr.getClient().getSession().writeAndFlush(CField.environmentChange("dojang/end/clear", 19));
                    chr.getClient().getSession().writeAndFlush(CField.getDojoClockStop(true, (int) (900 - ((System.currentTimeMillis() - chr.getDojoStartTime()) / 1000))));

                    if (getId() == 925075000) {
                        if (chr.getKeyValue(20220311, "ove3") < 50) {
                            chr.setKeyValue(20220311, "ove3", (int) chr.getKeyValue(20220311, "ove3") + 50 + "");
                            //    chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 무릉도장 "+chr.getKeyValue(20220311,"ove3")+" 층 달성 !!",0,4));
                        }
                    }

                } else {
                    MapleMonster clearcheck = getMonsterById(9300216);
                    if (clearcheck != null) {
                        chr.mCount = 0;
                        chr.setDojoStopTime(System.currentTimeMillis());
                        chr.addKV("dojo", String.valueOf(floor));
                        chr.addKV("dojo_time", String.valueOf((int) ((System.currentTimeMillis() - chr.getDojoStartTime() - chr.getDojoCoolTime()) / 1000)));
                        chr.dropMessage(-1, "상대를 격파하였습니다. 10초간 타이머가 정지됩니다.");
                        chr.getClient().getSession().writeAndFlush(CField.environmentChange("Dojang/clear", 5));
                        chr.getClient().getSession().writeAndFlush(CField.environmentChange("dojang/end/clear", 19));
                        chr.getClient().getSession().writeAndFlush(CField.getDojoClockStop(true, (int) (900 - ((System.currentTimeMillis() - chr.getDojoStartTime()) / 1000))));

                        if (getId() == 925075000) {
                            if (chr.getKeyValue(20220311, "ove3") < 50) {
                                chr.setKeyValue(20220311, "ove3", (int) chr.getKeyValue(20220311, "ove3") + 50 + "");
                                //    chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(chr,"#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 무릉도장 "+chr.getKeyValue(20220311,"ove3")+" 층 달성 !!",0,4));
                            }
                        }
                    }
                }
            }
        }
//        if (chr.getPet(0) != null) {
//            if (PetFlag.PET_GIANT != null) {
//                chr.getPet(0).addPetSize((short) 100);
//                chr.getMap().broadcastMessage(chr, PetPacket.showPet(chr, chr.getPet(0), false, false), true);
//                chr.getClient().getSession().writeAndFlush(PetPacket.updatePet(chr, chr.getPet(0), chr.getInventory(MapleInventoryType.CASH).getItem((short) chr.getPet(0).getInventoryPosition()), false, chr.getPetLoot()));
//                chr.dropMessage(5, "자이언트");
//            }
//        }

        if (GameConstants.price.containsKey(monster.getId())) {
            if (chr.getParty() != null) {
                for (MaplePartyCharacter pc : chr.getParty().getMembers()) {
                    MapleCharacter pz = chr.getClient().getChannelServer().getPlayerStorage().getCharacterByName(pc.getName());
                    if (pz != null) {
                        pz.setLastBossId(monster.getId());
                    } else {
                        FileoutputUtil.log(FileoutputUtil.결정석로그, pc.getName() + " 캐릭터 처치한 보스 아이디("+ monster.getId()+ ") 등록 실패");
                    }
                }
            } else {
                chr.setLastBossId(monster.getId());
            }
        }

        if (monster.getBuffToGive() > -1) {
            final int buffid = monster.getBuffToGive();
            final MapleStatEffect buff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);

            final Iterator<MapleCharacter> itr = characters.iterator();
            while (itr.hasNext()) {
                MapleCharacter mc = itr.next();
                if (mc.isAlive()) {
                    buff.applyTo(mc, true);

                    switch (monster.getId()) {
                        case 8810018:
                        case 8810122:
                        case 8810214:
                        case 8820001:
                            mc.getClient().getSession().writeAndFlush(EffectPacket.showNormalEffect(mc, 14, true)); // HT nine spirit
                            broadcastMessage(mc, EffectPacket.showNormalEffect(mc, 14, false), false); // HT nine spirit
                            break;
                    }
                }
            }
        }
        final int mobid = monster.getId();
        ExpeditionType type = null;
        if (!monster.getStats().isBoss() && (chr.getBuffedValue(MapleBuffStat.Reincarnation) != null)) {
            if (chr.getReinCarnation() > 0) {
                chr.setReinCarnation(chr.getReinCarnation() - 1);
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.Reincarnation, new Pair<>(1, (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.Reincarnation))));
                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.Reincarnation), chr));
            }
        }
        /*        if (mobid == 8810018 && mapid == 240060200) { // Horntail
         World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "수많은 도전 끝에 혼테일을 격파한 원정대여! 그대들이 진정한 리프레의 영웅이다!!"));
         //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
         if (speedRunStart > 0) {
         type = ExpeditionType.Horntail;
         }
         doShrine(true);
         } else if (mobid == 8810122 && mapid == 240060201) { // Horntail
         World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "수많은 도전 끝에 카오스 혼테일을 격파한 원정대여! 그대들이 진정한 리프레의 영웅이다!!"));
         //            FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
         if (speedRunStart > 0) {
         type = ExpeditionType.ChaosHT;
         }
         doShrine(true);
         } else */
        if (mobid == 9400266 && mapid == 802000111) {
            doShrine(true);
        } else if (mobid == 9400265 && mapid == 802000211) {
            doShrine(true);
        } else if (mobid == 9400270 && mapid == 802000411) {
            doShrine(true);
        } else if (mobid == 9400273 && mapid == 802000611) {
            doShrine(true);
        } else if (mobid == 9400294 && mapid == 802000711) {
            doShrine(true);
        } else if (mobid == 9400296 && mapid == 802000803) {
            doShrine(true);
        } else if (mobid == 9400289 && mapid == 802000821) {
            doShrine(true);
            //INSERT HERE: 2095_tokyo
        } else if (mobid == 8830000 && mapid == 105100300) {
            if (speedRunStart > 0) {
                type = ExpeditionType.Normal_Balrog;
            }
        } else if ((mobid == 9420544 || mobid == 9420549) && mapid == 551030200 && monster.getEventInstance() != null && monster.getEventInstance().getName().contains(getEMByMap().getName())) {
            doShrine(getAllReactor().isEmpty());
            /*        } else if (mobid == 8820001 && mapid == 270050100) {
             World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "지치지 않는 열정으로 핑크빈을 물리친 원정대여! 그대들이 진정한 시간의 승리자다!!"));
             if (speedRunStart > 0) {
             type = ExpeditionType.Pink_Bean;
             }
             doShrine(true);
             } else if (mobid == 8850011 && mapid == 274040200) {
             World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "미래에서 여제 시그너스를 물리친 원정대여! 그대들이 진정한 시간의 승리자다!!"));
             if (speedRunStart > 0) {
             type = ExpeditionType.Cygnus;
             }
             doShrine(true);*/
        } else if (mobid == 8840000 && mapid == 211070100) {
            if (speedRunStart > 0) {
                type = ExpeditionType.Von_Leon;
            }
            doShrine(true);
        } else if (mobid == 8800002 && mapid == 280030000) {
//            FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Zakum;
            }
            doShrine(true);
        } else if (mobid == 8800102 && mapid == 280030001) {
            //FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Chaos_Zakum;
            }

            doShrine(true);
            /*        } else if (mobid >= 8800003 && mobid <= 8800010) {
             boolean makeZakReal = true;
             final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

             for (final MapleMonster mons : monsters) {
             if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
             makeZakReal = false;
             break;
             }
             }
             if (makeZakReal) {
             for (final MapleMapObject object : monsters) {
             final MapleMonster mons = ((MapleMonster) object);
             if (mons.getId() == 8800000) {
             final Point pos = mons.getTruePosition();
             this.killAllMonsters(true);
             spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), pos);
             break;
             }
             }
             }
             } else if (mobid >= 8800103 && mobid <= 8800110) {
             boolean makeZakReal = true;
             final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

             for (final MapleMonster mons : monsters) {
             if (mons.getId() >= 8800103 && mons.getId() <= 8800110) {
             makeZakReal = false;
             break;
             }
             }
             if (makeZakReal) {
             for (final MapleMonster mons : monsters) {
             if (mons.getId() == 8800102) {
             final Point pos = mons.getTruePosition();
             this.killAllMonsters(true);
             spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800100), pos);
             break;
             }
             }
             }*/
        } else if (mobid == 8820008 || mobid == 8820108) { //wipe out statues and respawn
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820010 && mobid <= 8820014) {
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820000 && mons.getId() != 8820001 && mons.getObjectId() != monster.getObjectId() && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820110 && mobid <= 8820114) {
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820100 && mons.getId() != 8820101 && mons.getObjectId() != monster.getObjectId() && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid / 100000 == 98 && chr.getMapId() / 10000000 == 95 && getAllMonstersThreadsafe().size() == 0) {
            switch ((chr.getMapId() % 1000) / 100) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    chr.getClient().getSession().writeAndFlush(CField.MapEff("monsterPark/clear"));
                    break;
                case 5:
                    if (chr.getMapId() / 1000000 == 952) {
                        chr.getClient().getSession().writeAndFlush(CField.MapEff("monsterPark/clearF"));
                    } else {
                        chr.getClient().getSession().writeAndFlush(CField.MapEff("monsterPark/clear"));
                    }
                    break;
                case 6:
                    chr.getClient().getSession().writeAndFlush(CField.MapEff("monsterPark/clearF"));
                    break;
            }
        }

        if (type != null) {
            if (speedRunStart > 0 && speedRunLeader.length() > 0) {
                long endTime = System.currentTimeMillis();
                String time = StringUtil.getReadableMillis(speedRunStart, endTime);
                broadcastMessage(CWvsContext.serverNotice(5, "", speedRunLeader + "'s squad has taken " + time + " to defeat " + type.name() + "!"));
                getRankAndAdd(speedRunLeader, time, type, (endTime - speedRunStart), (sqd == null ? null : sqd.getMembers()));
                endSpeedRun();
            }

        }

        if (rune == null && !chr.getBuffedValue(80002282) && chr.getEventInstance() == null && !isTown() && !FieldLimitType.Event.check(getFieldLimit())) {
            //스폰 룬 종류 설정
            if (Randomizer.isSuccess2(50)) {
                MapleRune rune = new MapleRune(Randomizer.nextInt(11), monster.getPosition().x, monster.getPosition().y, this);//Randomizer.rand(map.getFootholds().getX1(), map.getFootholds().getX2()), Randomizer.rand(map.getFootholds().getY2(), map.getFootholds().getY1()), map);
                spawnRune(rune);
            }
        }

        if (withDrops && monster.getSeperateSoul() == 0) {
            dropFromMonster(chr, monster, instanced);
        }

        if (monster.getEventInstance() != null) {
            monster.getEventInstance().monsterKilled(chr, monster);
        } else {
            final EventInstanceManager em = chr.getEventInstance();
            if (em != null) {
                em.monsterKilled(chr, monster);
            }
        }
    }

    public void setclearBoss(MapleCharacter chr, Pair<Integer, String> bosskey) {
        Date data = new Date();
        String month = data.getMonth() < 10 ? "0" + (data.getMonth() + 1) : String.valueOf((data.getMonth() + 1));
        String date2 = data.getDate() < 10 ? "0" + data.getDate() : String.valueOf(data.getDate());
        int date = Integer.parseInt((data.getYear() + 1900) + "" + month + "" + date2);
        if (chr.getParty() != null) {
            for (MaplePartyCharacter chs : chr.getParty().getMembers()) {
                for (ChannelServer channel : ChannelServer.getAllInstances()) {
                    MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chs.getId());
                    if (ch != null) { //&& ch.getGMLevel() < 6
                        long getKey = ch.getKeyValue(date, bosskey.getRight()) == -1 ? 1 : ch.getKeyValue(date, bosskey.getRight()) + 1;
                        ch.setKeyValue(date, bosskey.getRight(), String.valueOf(1));
                        if (!bosskey.getRight().equals("AdventureDrill")) {
                            ch.dropMessage(5, "해당 보스에 " + getKey + "번 클리어하셨습니다.");
                        }
                        String logdata = ch.getName() + " | 보스 : " + bosskey.getRight() + " | 클리어 횟수 :" + getKey + "\r\n";
                        writeLog("Log/Boss.log", logdata, true);
                    }
                }
            }
        }
    }

    public List<MapleReactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }

    public List<MapleReactor> getAllReactorsThreadsafe() {
        ArrayList<MapleReactor> ret = new ArrayList<MapleReactor>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ret.add((MapleReactor) mmo);
        }
        return ret;
    }

    public List<MapleSummon> getAllSummonsThreadsafe() {
        ArrayList<MapleSummon> ret = new ArrayList<MapleSummon>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SUMMON).values()) {
            if (mmo instanceof MapleSummon) {
                ret.add((MapleSummon) mmo);
            }
        }
        return ret;
    }

    public List<MapleSummon> getAllSummons(int skillId) {
        ArrayList<MapleSummon> ret = new ArrayList<MapleSummon>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SUMMON).values()) {
            if (mmo instanceof MapleSummon) {
                if (((MapleSummon) mmo).getSkill() == skillId) {
                    ret.add((MapleSummon) mmo);
                }
            }
        }
        return ret;
    }

    public List<MapleFlyingSword> getAllFlyingSwordsThreadsafe() {
        ArrayList<MapleFlyingSword> ret = new ArrayList<MapleFlyingSword>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SWORD).values()) {
            ret.add((MapleFlyingSword) mmo);
        }
        return ret;
    }

    public List<MapleMapObject> getAllDoor() {
        return getAllDoorsThreadsafe();
    }

    public List<MapleMapObject> getAllDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
            if (mmo instanceof MapleDoor) {
                ret.add(mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllMechDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
            if (mmo instanceof MechDoor) {
                ret.add(mmo);
            }
        }
        return ret;
    }

    public List<MapleMapObject> getAllMerchant() {
        return getAllHiredMerchantsThreadsafe();
    }

    public List<MapleMapObject> getAllHiredMerchantsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
            ret.add(mmo);
        }
        return ret;
    }

    public List<MapleSpecialChair> getAllSpecialChairs() {
        ArrayList<MapleSpecialChair> ret = new ArrayList<MapleSpecialChair>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SPECIAL_CHAIR).values()) {
            ret.add((MapleSpecialChair) mmo);
        }
        return ret;
    }

    public List<MapleCharacter> getAllChracater() {
        return getAllCharactersThreadsafe();
    }

    public List<MapleCharacter> getAllCharactersThreadsafe() {
        ArrayList<MapleCharacter> ret = new ArrayList<MapleCharacter>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.PLAYER).values()) {
            ret.add((MapleCharacter) mmo);
        }
        return ret;
    }

    public MapleRandomPortal getPoloFrittoPortal() {
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.RANDOM_PORTAL).values()) {
            MapleRandomPortal p = (MapleRandomPortal) mmo;
            if (p.getPortalType() == 2) {
                return p;
            }
        }
        return null;
    }

    public MapleRandomPortal getFireWolfPortal() {
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.RANDOM_PORTAL).values()) {
            MapleRandomPortal p = (MapleRandomPortal) mmo;
            if (p.getPortalType() == 3) {
                return p;
            }
        }
        return null;
    }

    public List<MapleMonster> getAllMonster() {
        return getAllMonstersThreadsafe();
    }

    public List<MapleMonster> getAllMonstersThreadsafe() {
        ArrayList<MapleMonster> ret = new ArrayList<MapleMonster>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            ret.add((MapleMonster) mmo);
        }
        return ret;
    }

    public List<MapleMonster> getAllNormalMonstersThreadsafe() {
        ArrayList<MapleMonster> ret = new ArrayList<MapleMonster>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            MapleMonster monster = (MapleMonster) mmo;
            if (!monster.getStats().isBoss()) {
                ret.add(monster);
            }
        }
        return ret;
    }

    public List<Integer> getAllUniqueMonsters() {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            final int theId = ((MapleMonster) mmo).getId();
            if (!ret.contains(theId)) {
                ret.add(theId);
            }
        }
        return ret;
    }

    public final void killAllMonsters(final boolean animate) {
        for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            final MapleMonster monster = (MapleMonster) monstermo;
            monster.setHp(0);
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
            removeMapObject(monster);
            monster.killed();
            spawnedMonstersOnMap.decrementAndGet();
        }
    }

    public final void killAllMonsters(MapleCharacter chr) {
        for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            final MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getOwner() == chr.getId()) {
                monster.setHp(0);
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
                removeMapObject(monster);
                monster.killed();
                spawnedMonstersOnMap.decrementAndGet();
            }
        }
    }

    public final void killMonster(final int monsId) {
        for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
            MapleMonster mob = (MapleMonster) mmo;
            if (mob.getId() == monsId) {
                mob.setHp(0);
                broadcastMessage(MobPacket.killMonster(mob.getObjectId(), 1));
                removeMapObject(mob);
                mob.killed();
                spawnedMonstersOnMap.decrementAndGet();
            }
        }
    }

    public final void limitReactor(final int rid, final int num) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        Map<Integer, Integer> contained = new LinkedHashMap<Integer, Integer>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (contained.containsKey(mr.getReactorId())) {
                if (contained.get(mr.getReactorId()) >= num) {
                    toDestroy.add(mr);
                } else {
                    contained.put(mr.getReactorId(), contained.get(mr.getReactorId()) + 1);
                }
            } else {
                contained.put(mr.getReactorId(), 1);
            }
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactors(final int first, final int last) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                toDestroy.add(mr);
            }
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactor(final int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor == null) {
            return;
        }
        broadcastMessage(CField.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);

        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public final void reloadReactors() {
        List<MapleReactor> toSpawn = new ArrayList<MapleReactor>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            final MapleReactor reactor = (MapleReactor) obj;
            broadcastMessage(CField.destroyReactor(reactor));
            reactor.setAlive(false);
            reactor.setTimerActive(false);
            toSpawn.add(reactor);
        }
        for (MapleReactor r : toSpawn) {
            removeMapObject(r);
            respawnReactor(r);
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use - not tested (broken reactors get removed
     * from mapobjects when destroyed) Should create instances for multiple copies of non-respawning reactors...
     */
    public final void resetReactors() {
        setReactorState((byte) 0);
    }

    public final void setReactorState() {
        setReactorState((byte) 1);
    }

    public final void setReactorState(final byte state) {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ((MapleReactor) obj).forceHitReactor((byte) state);
        }
    }

    public final void setReactorDelay(final int state) {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            ((MapleReactor) obj).setDelay(state);
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes (such as ZPQ/LMPQ)
     */
    public final void shuffleReactors() {
        shuffleReactors(0, 9999999); //all
    }

    public final void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<Point>();
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                points.add(mr.getPosition());
            }
        }
        Collections.shuffle(points);
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = (MapleReactor) obj;
            if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                mr.setPosition(points.remove(points.size() - 1));
            }
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars
     * on the map...
     *
     * @param monster
     */
    public final void updateMonsterController(final MapleMonster monster) {

        if (!monster.isAlive() || monster.getLinkCID() > 0 || monster.getStats().isEscort()) {
            return;
        }

        if (monster.getController() != null) {
            if (monster.getController().getMap() != this || monster.getController().getTruePosition().distanceSq(monster.getTruePosition()) > monster.getRange()) {
                monster.getController().stopControllingMonster(monster);
            } else { // Everything is fine :)
                return;
            }
        }
        if (monster.getStats().isMobZone()) {
            byte phase;
            if (monster.getHPPercent() > 75) {
                phase = 1;
            } else if (monster.getHPPercent() > 50) {
                phase = 2;
            } else if (monster.getHPPercent() > 25) {
                phase = 3;
            } else {
                phase = 4;
            }

            if (monster.getId() != 8644650 && monster.getId() != 8644655 && monster.getId() != 8644658 && monster.getId() != 8644659) {  //더스크는 이거안해!
                if (monster.getPhase() != phase) {
                    monster.setPhase(phase);
                }
                broadcastMessage(MobPacket.changePhase(monster));
                broadcastMessage(MobPacket.changeMobZone(monster));
            }

        }

        int mincontrolled = -1;
        MapleCharacter newController = null;

        final Iterator<MapleCharacter> ltr = characters.iterator();
        MapleCharacter chr;
        while (ltr.hasNext()) {
            chr = ltr.next();
            if (!chr.isHidden() && (chr.getControlledSize() < mincontrolled || mincontrolled == -1) && chr.getTruePosition().distanceSq(monster.getTruePosition()) <= monster.getRange()) {
                if (monster.getOwner() == -1) {
                    mincontrolled = chr.getControlledSize();
                    newController = chr;
                } else if (monster.getOwner() == chr.getId()) {
                    mincontrolled = chr.getControlledSize();
                    newController = chr;
                }
            }
        }

        if (newController != null) {
            if (monster.isFirstAttack()) {
                newController.controlMonster(monster, true);
                monster.setControllerHasAggro(true);
            } else {
                newController.controlMonster(monster, false);
            }
        }
    }

    public final MapleMapObject getMapObject(int oid, MapleMapObjectType type) {
        return mapobjects.get(type).get(oid);
    }

    public final boolean containsNPC(int npcid) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC n = (MapleNPC) itr.next();
            if (n.getId() == npcid) {
                return true;
            }
        }
        return false;
    }

    public MapleNPC getNPCById(int id) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC n = (MapleNPC) itr.next();
            if (n.getId() == id) {
                return n;
            }
        }
        return null;
    }

    public MapleMonster getMonsterById(int id) {
        MapleMonster ret = null;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
        while (itr.hasNext()) {
            MapleMonster n = (MapleMonster) itr.next();
            if (n.getId() == id) {
                ret = n;
                break;
            }
        }
        return ret;
    }

    public int countOrgelById(boolean purple) {
        int ret = 0;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
        while (itr.hasNext()) {
            MapleMonster n = (MapleMonster) itr.next();
            if (n.getId() / 10 == (purple ? 983308 : 983307)) {
                ret++;
            }
        }
        return ret;
    }

    public int countMonsterById(int id) {
        int ret = 0;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
        while (itr.hasNext()) {
            MapleMonster n = (MapleMonster) itr.next();
            if (n.getId() == id) {
                ret++;
            }
        }
        return ret;
    }

    public MapleReactor getReactorById(int id) {
        MapleReactor ret = null;
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.REACTOR).values().iterator();
        while (itr.hasNext()) {
            MapleReactor n = (MapleReactor) itr.next();
            if (n.getReactorId() == id) {
                ret = n;
                break;
            }
        }
        return ret;
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public final MapleMonster getMonsterByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MONSTER);
        if (mmo == null) {
            return null;
        }
        return (MapleMonster) mmo;
    }

    public final MapleSummon getSummonByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.SUMMON);
        if (mmo == null) {
            return null;
        }
        return (MapleSummon) mmo;
    }

    public final MapleNPC getNPCByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.NPC);
        if (mmo == null) {
            return null;
        }
        return (MapleNPC) mmo;
    }

    public final MapleReactor getReactorByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.REACTOR);
        if (mmo == null) {
            return null;
        }
        return (MapleReactor) mmo;
    }

    public final MapleReactor getReactorByName(final String name) {
        for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            MapleReactor mr = ((MapleReactor) obj);
            if (mr.getName().equalsIgnoreCase(name)) {
                return mr;
            }
        }
        return null;
    }

    public final void spawnNpc(final int id, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos).getId());
        npc.setCustom(true);
        addMapObject(npc);
        broadcastMessage(NPCPacket.spawnNPC(npc, true));
    }

    public final void removeNpc(final int npcid) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC npc = (MapleNPC) itr.next();
            if (npc.isCustom() && (npcid == -1 || npc.getId() == npcid)) {
                broadcastMessage(NPCPacket.removeNPCController(npc.getObjectId()));
                broadcastMessage(NPCPacket.removeNPC(npc.getObjectId()));
                itr.remove();
            }
        }
    }

    public final void hideNpc(final int npcid) {
        Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
        while (itr.hasNext()) {
            MapleNPC npc = (MapleNPC) itr.next();
            if (npcid == -1 || npc.getId() == npcid) {
                broadcastMessage(NPCPacket.removeNPCController(npc.getObjectId()));
                broadcastMessage(NPCPacket.removeNPC(npc.getObjectId()));
            }
        }
    }

    public final void spawnReactorOnGroundBelow(final MapleReactor mob, final Point pos) {
        mob.setPosition(pos); //reactors dont need FH lol
        mob.setCustom(true);
        spawnReactor(mob);
    }

    public final void spawnMonster_sSack(final MapleMonster mob, final Point pos, final int spawnType) {
        Point newPos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mob.setPosition(newPos == null ? pos : newPos);
        spawnMonster(mob, spawnType);
    }

    public final void spawnMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        spawnMonster_sSack(mob, pos, mob.getId() == 8880512 ? 1 : -2);
    }

    public final int spawnMonsterWithEffectBelow(final MapleMonster mob, final Point pos, final int effect) {
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        return spawnMonsterWithEffect(mob, effect, spos);
    }

    public final void spawnZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800002);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800003, 8800004, 8800005, 8800006, 8800007,
            8800008, 8800009, 8800010};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnChaosZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800102);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800103, 8800104, 8800105, 8800106, 8800107,
            8800108, 8800109, 8800110};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnFakeMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    private void checkRemoveAfter(final MapleMonster monster) {
        final int ra = monster.getStats().getRemoveAfter();

        if (ra > 0 && monster.getLinkCID() <= 0) {
            monster.registerKill(ra * 1000);
        }
    }

    public final void spawnRevives(final MapleMonster monster, final int oid) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        monster.setLinkOid(oid);
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            @Override
            public final void sendPackets(MapleClient c) {

                if (monster.getOwner() == -1) {
                    c.getSession().writeAndFlush(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 ? -3 : monster.getStats().getSummonType(), oid)); // TODO effect
                } else if (monster.getOwner() == c.getPlayer().getId()) {
                    c.getSession().writeAndFlush(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 ? -3 : monster.getStats().getSummonType(), oid)); // TODO effect
                }
            }
        });

        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();

        if (monster.getId() >= 9833070 && monster.getId() <= 9833074) {
            MapTimer.getInstance().schedule(() -> {
                if (monster != null && monster.isAlive() && !getAllCharactersThreadsafe().isEmpty()) {
                    MapleCharacter player = getAllCharactersThreadsafe().get(0);
                    if (player != null) {
                        Point pos = monster.getTruePosition();
                        killMonster(monster, player, false, false, (byte) 1);
                        MapleMonster mob = MapleLifeFactory.getMonster(monster.getId() + 10);
                        mob.setHp(GameConstants.getDreamBreakerHP((int) player.getKeyValue(15901, "stage")));
                        spawnMonsterOnGroundBelow(mob, pos);
                    }
                }
            }, 35000);
        }
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType) {
        spawnMonster(monster, spawnType, false);
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType, final boolean overwrite) {
        monster.setMap(this);
        monster.setSpawnTime(System.currentTimeMillis());
        checkRemoveAfter(monster);

        List<Integer> blocks = new ArrayList<>();
        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {

                if (monster.getOwner() == -1) {
                    c.getSession().writeAndFlush(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite ? spawnType : monster.getStats().getSummonType(), 0));
                } else if (monster.getOwner() == c.getPlayer().getId()) {
                    c.getSession().writeAndFlush(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite ? spawnType : monster.getStats().getSummonType(), 0));
                }
            }
        });
        updateMonsterController(monster);

        if (monster.getOwner() < 0) {
            spawnedMonstersOnMap.incrementAndGet();
        }

        if (monster.getId() == 8880300 || monster.getId() == 8880340) { // 윌 1페이즈
            monster.getWillHplist().add(666);
            monster.getWillHplist().add(333);
            monster.getWillHplist().add(3);
            broadcastMessage(BossWill.setWillHp(monster.getWillHplist(), this, monster.getId(), monster.getId() + 3, monster.getId() + 4));

            for (MobSkill skill : monster.getStats().getSkills()) { // 스킬 쓰지 마렴
                monster.setLastSkillUsed(skill, System.currentTimeMillis(), 30 * 6000 * 1000);
            }

            //공간 붕괴
            MobTimer.getInstance().schedule(() -> {
                MobSkill msi = MobSkillFactory.getMobSkill(242, 5);
                msi.applyEffect(null, monster, true, monster.isFacingLeft());
                monster.setLastSkillUsed(msi, System.currentTimeMillis(), msi.getInterval());
            }, 60 * 1000);
        }

        if (monster.getId() == 8880301 || monster.getId() == 8880341) { // 윌 2페이즈
            monster.getWillHplist().add(503);
            monster.getWillHplist().add(3);
            broadcastMessage(BossWill.setWillHp(monster.getWillHplist()));

            MobSkill msi = MobSkillFactory.getMobSkill(201, 237);
            msi.applyEffect(null, monster, true, monster.isFacingLeft());
        }

        if (monster.getId() == 8880302 || monster.getId() == 8880342) {
            spawnSpiderWeb(new SpiderWeb(2, -683, 395));
            spawnSpiderWeb(new SpiderWeb(1, -701, 182));
            spawnSpiderWeb(new SpiderWeb(2, 702, -280));
            spawnSpiderWeb(new SpiderWeb(0, -711, -254));
            spawnSpiderWeb(new SpiderWeb(1, 718, 432));
            spawnSpiderWeb(new SpiderWeb(0, 712, 310));
            spawnSpiderWeb(new SpiderWeb(1, -577, -298));
            spawnSpiderWeb(new SpiderWeb(0, 552, 459));
            spawnSpiderWeb(new SpiderWeb(0, 531, -268));
            spawnSpiderWeb(new SpiderWeb(1, 699, -82));
            spawnSpiderWeb(new SpiderWeb(0, -594, 251));
            spawnSpiderWeb(new SpiderWeb(2, 378, 480));
            spawnSpiderWeb(new SpiderWeb(1, 577, 345));
            spawnSpiderWeb(new SpiderWeb(0, -506, 432));
            spawnSpiderWeb(new SpiderWeb(1, -733, -122));
            spawnSpiderWeb(new SpiderWeb(0, -626, -179));
            spawnSpiderWeb(new SpiderWeb(0, 604, -153));
            spawnSpiderWeb(new SpiderWeb(1, -405, 484));
            spawnSpiderWeb(new SpiderWeb(0, 736, 56));
            spawnSpiderWeb(new SpiderWeb(0, -749, 17));
            spawnSpiderWeb(new SpiderWeb(2, -366, -325));
            spawnSpiderWeb(new SpiderWeb(1, 391, -307));
            spawnSpiderWeb(new SpiderWeb(0, -197, -300));
            spawnSpiderWeb(new SpiderWeb(1, 458, -163));
            spawnSpiderWeb(new SpiderWeb(0, -282, 488));
            spawnSpiderWeb(new SpiderWeb(1, 80, 482));
            spawnSpiderWeb(new SpiderWeb(1, -485, -148));
            spawnSpiderWeb(new SpiderWeb(0, -606, -75));
            spawnSpiderWeb(new SpiderWeb(1, 772, 169));
            spawnSpiderWeb(new SpiderWeb(2, -84, 481));
            spawnSpiderWeb(new SpiderWeb(1, -650, 45));
            spawnSpiderWeb(new SpiderWeb(2, 558, -58));
            spawnSpiderWeb(new SpiderWeb(2, 164, -308));
            spawnSpiderWeb(new SpiderWeb(1, -61, -275));
        }
        if (monster.getId() == 8880000 || monster.getId() == 8880002 || monster.getId() == 8880010) {
            monster.setSchedule(MapTimer.getInstance().register(() -> {
                MapleBossManager.magnusHandler(monster);
            }, 3000));
        } else if (monster.getId() / 1000 == 8900 && monster.getId() % 10 < 3 && getNumMonsters() <= 2) {
            MapleBossManager.pierreHandler(monster);
        } else if (monster.getId() == 8910000) {
            monster.setSchedule(MapTimer.getInstance().register(() -> {
                if (monster.getHPPercent() <= 10) {
                    List<Obstacle> obs = new ArrayList<>();
                    for (int i = 0; i < Randomizer.rand(1, 3); i++) {
                        int key = Randomizer.rand(1, 6) + 21;
                        int x = Randomizer.rand(1, 1920) - 1140;
                        Obstacle ob = new Obstacle(key, new Point(x, 0), new Point(x, 420), 0x19, (key == 22 || key == 25) ? 100 : (key == 23 || key == 26) ? 50 : 33, Randomizer.rand(1100, 1500), Randomizer.rand(0x60, 0x80), 3, 0x28D);
                        obs.add(ob);
                    }
//                    map.broadcastMessage(MobPacket.createObstacle(obs));
                }
            }, 3000));
        } else if (monster.getId() == 8950000 || monster.getId() == 8950001 || monster.getId() == 8950002 || monster.getId() == 8950100 || monster.getId() == 8950101 || monster.getId() == 8950102) {
            monster.setSchedule(MapTimer.getInstance().register(() -> {
                MapleBossManager.lotusHandler(monster);
            }, 3000));
        } else if (monster.getId() == 8880100 || monster.getId() == 8880110 || monster.getId() == 8880101 || monster.getId() == 8880111) {
            MapleBossManager.demianHandler(monster);
        } else if (monster.getId() == 8880512) {
            MapleBossManager.blackMageHandler(monster);
        } else if (monster.getId() == 8644650 || monster.getId() == 8644655) {
            MapTimer.getInstance().schedule(() -> {
                MapleBossManager.duskHandler(monster, this);
            }, 2000);
        } else if (monster.getStats().getName().contains("세렌") || monster.getId() == 8880605) {
            if (monster.getId() != 8880602) {
                for (MobSkill sk : monster.getSkills()) {
                    monster.setLastSkillUsed(sk, System.currentTimeMillis(), 5000L);
                }
                MapTimer.getInstance().schedule(() -> MapleBossManager.SerenHandler(monster), 5000L);
            }
            else {
                MapleBossManager.SerenHandler(monster);
            }
        }

        if (monster.getId() == 8880400 || monster.getId() == 8880405 || monster.getId() == 8880415) {
            MobSkill msi = MobSkillFactory.getMobSkill(247, 1);
            msi.applyEffect(monster.getController(), monster, true, false);
            monster.setLastSkillUsed(msi, System.currentTimeMillis(), msi.getInterval());
        }

        if (monster.getId() == 8880500) {
            monster.setLastSkillUsed(MobSkillFactory.getMobSkill(170, 62), System.currentTimeMillis(), 40000);
        }

        if (monster.getId() == 8880501) {
            monster.setLastSkillUsed(MobSkillFactory.getMobSkill(170, 64), System.currentTimeMillis(), 40000);
        }

        if (monster.getId() == 8930000) {
            MobSkill msi = MobSkillFactory.getMobSkill(170, 13);
            msi.setHp(100); // 40% 이하
        }

        if (monster.getId() >= 9833070 && monster.getId() <= 9833074) {
            MapTimer.getInstance().schedule(() -> {
                if (monster != null && monster.isAlive() && !getAllCharactersThreadsafe().isEmpty()) {
                    MapleCharacter player = getAllCharactersThreadsafe().get(0);
                    if (player != null) {
                        Point pos = monster.getTruePosition();
                        killMonster(monster, player, false, false, (byte) 1);
                        MapleMonster mob = MapleLifeFactory.getMonster(monster.getId() + 10);
                        mob.setHp(GameConstants.getDreamBreakerHP((int) player.getKeyValue(15901, "stage")));
                        spawnMonsterOnGroundBelow(mob, pos);
                    }
                }
            }, 35000);
        }
    }

    public final int spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        try {
            monster.setMap(this);
            monster.setPosition(pos);

            spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

                @Override
                public final void sendPackets(MapleClient c) {
                    c.getSession().writeAndFlush(MobPacket.spawnMonster(monster, effect, 0));
                }
            });
            updateMonsterController(monster);
            spawnedMonstersOnMap.incrementAndGet();

            return monster.getObjectId();
        } catch (Exception e) {
            return -1;
        }
    }

    public final void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);

        spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {

            @Override
            public final void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(MobPacket.spawnMonster(monster, -4, 0));
            }
        });
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();

    }

    public final void spawnDelayedAttack(MobSkill skill, final MapleDelayedAttack mda) {
        spawnAndAddRangedMapObject(mda, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(MobPacket.onDemianDelayedAttackCreate(skill.getSkillId(), skill.getSkillLevel(), mda));
            }
        });
    }

    public final void spawnDelayedAttack(MapleMonster mob, MobSkill skill, List<MapleDelayedAttack> mda) {
        for (MapleDelayedAttack att : mda) {
            addMapObject(att);
        }
        broadcastMessage(MobPacket.onDemianDelayedAttackCreate(mob, skill.getSkillId(), skill.getSkillLevel(), mda));
    }

    public final void spawnMapleAtom(MapleAtom atom) {
        broadcastMessage(CField.createAtom(atom));
    }

    public final void spawnRune(final MapleRune rune) {

        rune.setMap(this);
        this.rune = rune;

        spawnAndAddRangedMapObject(rune, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                /* Respawn Effect 발동 */
                c.getSession().writeAndFlush(CField.spawnRune(rune, false));
            }
        });
    }

    public final void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);

        spawnAndAddRangedMapObject(reactor, new DelayedPacketCreation() {

            @Override
            public final void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.spawnReactor(reactor));
            }
        });
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public final void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {
                door.sendSpawnData(c);
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        });
    }

    public final void spawnMechDoor(final MechDoor door) {
        spawnAndAddRangedMapObject(door, new DelayedPacketCreation() {

            public final void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.spawnMechDoor(door, true));
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        });

        final MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> schedule = null;
        door.setSchedule(schedule);

        if (door.getDuration() > 0) {
            door.setSchedule(tMan.schedule(new Runnable() {

                @Override
                public void run() {
                    broadcastMessage(CField.removeMechDoor(door, true));
                    removeMapObject(door);
                }
            }, door.getDuration()));
        }
    }

    public final void spawnSummon(final MapleSummon summon) {
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                if (summon != null && c.getPlayer() != null && (!summon.isChangedMap() || summon.getOwner().getId() == c.getPlayer().getId())) {
                    c.getSession().writeAndFlush(SummonPacket.spawnSummon(summon, true));
                }
            }
        });
    }

    public final void spawnAdelProjectile(MapleCharacter chr, List<AdelProjectile> tiles, boolean infinity) {
        for (AdelProjectile tile : tiles) {
            addMapObject(tile);
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ADEL_PROJECTILE).values()) {
                if (tile.getObjectId() == mmo.getObjectId()) {
                    chr.object.add(new Pair<>(tile.getSkillId(), tile.getObjectId()));
                }
            }
        }
        broadcastMessage(CField.spawnAdelProjectiles(chr, tiles, infinity));
    }

    public final List<Integer> getAdelProjectile(MapleCharacter chr, int skillid) {
        List<Integer> adp = new ArrayList<>();
        for (Pair<Integer, Integer> c : chr.object) {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ADEL_PROJECTILE).values()) {
                if (c.getRight() == mmo.getObjectId()) {
                    adp.add(mmo.getObjectId());
                    break;
                }
            }
        }
        return adp;
    }

    public final void removeAdelProjectile(MapleCharacter chr, int objectId) {
        MapleMapObject object = getMapObject(objectId, MapleMapObjectType.ADEL_PROJECTILE);
        if (object != null) {
            removeMapObject(object);
        }
        for (Pair<Integer, Integer> tile : chr.object) {
            if (tile.right == objectId) {
                chr.addHuntingDecree(2);
            }

        }
        broadcastMessage(CField.removeAdelProjectile(chr, objectId));
    }

    public final void spawnRandomPortal(final MapleRandomPortal portal) {
        spawnAndAddRangedMapObject(portal, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.specialMapSound("Field.img/StarPlanet/cashTry"));
                if (portal.getPortalType() == 2) {
                    c.getSession().writeAndFlush(CWvsContext.getTopMsg("현상금 사냥꾼의 포탈이 등장했습니다!"));
                } else if (portal.getPortalType() == 3) {
                    c.getSession().writeAndFlush(CWvsContext.getTopMsg("불꽃늑대의 소굴로 향하는 포탈이 등장했습니다!"));
                }
                if (c.getPlayer().getId() == portal.getCharId()) {
                    c.getSession().writeAndFlush(SLFCGPacket.PoloFrittoPortal(portal));
                }
            }
        });

        MapTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                broadcastMessage(SLFCGPacket.RemovePoloFrittoPortal(portal));
                removeMapObject(portal);
            }
        }, 60000); // 1분
    }

    public final void spawnWillPoison(final WillPoison wp) {
        spawnAndAddRangedMapObject(wp, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                wp.sendSpawnData(c);
            }
        });

        MapTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                broadcastMessage(BossWill.removePoison(wp.getObjectId()));
                removeMapObject(wp);
            }
        }, 7000);
    }

    public final void spawnSpiderWeb(final SpiderWeb web) {
        spawnAndAddRangedMapObject(web, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                web.sendSpawnData(c);
            }
        });
    }

    public final void spawnSummon(final MapleSummon summon, int duration) { //추가
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                if (summon != null && c.getPlayer() != null && (!summon.isChangedMap() || summon.getOwner().getId() == c.getPlayer().getId())) {
                    c.getSession().writeAndFlush(SummonPacket.spawnSummon(summon, true));
                }
            }
        });

        final MapleMap map = this;

        if (summon.getSkill() == 400021047) {
            summon.getOwner().setBHGCCount(summon.getOwner().getBHGCCount() - 1);
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(summon.getOwner().getBHGCCount(), 0));

            summon.getOwner().getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(summon.getSkill()).getEffect(summon.getSkillLevel()), summon.getOwner()));
        }

        if (summon.getSkill() == 152101000) {
            summon.getOwner().getClient().getSession().writeAndFlush(SummonPacket.transformSummon(summon, 2));
        }

        if (duration > 0 && summon.getSummonType() != 7 && (summon.getMovementType() != SummonMovementType.SUMMON_JAGUAR)) { // 데스, 재규어는 제외
            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    if (summon.getSkill() != 400041038) {
                        summon.removeSummon(map, false);
                    }
                }
            }, duration);
        }
    }

    public final void spawnExtractor(final MapleExtractor ex) {
        spawnAndAddRangedMapObject(ex, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                ex.sendSpawnData(c);
            }
        });
    }

    public final void spawnSpecialChair(final MapleSpecialChair ex) {
        spawnAndAddRangedMapObject(ex, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                ex.sendSpawnData(c);
            }
        });
    }

    public final void spawnMagicWreck(final MapleMagicWreck mw) {
        spawnAndAddRangedMapObject(mw, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                mw.sendSpawnData(c);
            }
        });

        List<MapleMagicWreck> mws = new ArrayList<>();

        getWrecks().add(mw);

        final MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> schedule = null;
        mw.setSchedule(schedule);

        if (mw.getDuration() > 0) {
            mw.setSchedule(tMan.schedule(new Runnable() {
                @Override
                public void run() {
                    broadcastMessage(CField.removeMagicWreck(mw.getChr(), mws));
                    removeMapObject(mw);
                    getWrecks().remove(mw);
                }
            }, mw.getDuration()));
        }
    }

    public final void spawnFlyingSword(final MapleFlyingSword mfs) {
        spawnAndAddRangedMapObject(mfs, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                mfs.sendSpawnData(c);
            }
        });
    }

    public final void removeAllFlyingSword() {
        for (MapleFlyingSword sword : getAllFlyingSwordsThreadsafe()) {
            broadcastMessage(MobPacket.FlyingSword(sword, false));
            removeMapObject(sword);
        }
    }

    public final void setNewFlyingSwordNode(final MapleFlyingSword mfs, Point point) {
        FlyingSwordNode msn = new FlyingSwordNode(1, 0, 0, 30, 0, 0, 0, false, 0, new Point(point.x, -180)); // 데미안 머리위에 띄워야함
        List<FlyingSwordNode> nodes = new ArrayList<>();
        nodes.add(msn);
        mfs.setNodes(nodes);
        broadcastMessage(MobPacket.FlyingSwordNode(mfs));
        mfs.updateTarget(this);

        MapleMap map = this;

        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (mfs != null) {
                    mfs.updateFlyingSwordNode(map);
                }
            }
        }, 2500);
    }

    public final void spawnIncinerateObject(final MapleIncinerateObject mio) {
        spawnAndAddRangedMapObject(mio, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                mio.sendSpawnData(c);
            }
        });

        final MapTimer tMan = MapTimer.getInstance();

        mio.setSchedule(tMan.schedule(new Runnable() {

            @Override
            public void run() {
                broadcastMessage(MobPacket.incinerateObject(mio, false));
                removeMapObject(mio);
            }
        }, 10000));
    }

    public final void spawnFieldAttackObj(final MapleFieldAttackObj fao) {
        spawnAndAddRangedMapObject(fao, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                fao.sendSpawnData(c);
                fao.onSetAttack(c);
            }
        });

        final MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> schedule = null;
        fao.setSchedule(schedule);

        if (fao.getDuration() > 0) {
            fao.setSchedule(tMan.schedule(new Runnable() {

                @Override
                public void run() {
                    broadcastMessage(AttackObjPacket.ObjRemovePacketByOid(fao.getObjectId()));
                    removeMapObject(fao);
                }
            }, fao.getDuration()));
        }
    }

    public void spawnEnergySphere(int objectId, int skillLevel, MapleEnergySphere sp) {
        spawnAndAddRangedMapObject(sp, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                broadcastMessage(MobPacket.createEnergySphere(objectId, skillLevel, sp));
            }
        });

    }

    public final void spawnMist(final MapleMist mist, boolean fake) {

        spawnAndAddRangedMapObject(mist, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                mist.sendSpawnData(c);
            }
        });
        if (mist.getStartTime() == 0) {
            mist.setStartTime(System.currentTimeMillis());
        }

//        final MapTimer tMan = MapTimer.getInstance();
//        final ScheduledFuture<?> poisonSchedule;
/*        switch (mist.isPoisonMist()) {
         case 1: { //포이즌
         //poison: 0 = none, 1 = poisonous, 4 = recovery
         final MapleCharacter owner = getCharacterById(mist.getOwnerId());
         final boolean pvp = owner.inPVP();
         poisonSchedule = tMan.register(new Runnable() {

         @Override
         public void run() {
         for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER))) {
         if (!pvp && mist.makeChanceResult() && !((MapleMonster) mo).isBuffed(MonsterStatus.MS_Burned)) {
         ((MapleMonster) mo).applyStatus(owner.getClient(), MonsterStatus.MS_Burned, new MonsterStatusEffect(mist.getSourceSkill().getId(), mist.getDuration()), 1, mist.getSource());
         }
         }
         }
         }, 2000, 2500);
         break;
         }
         case 4: { //리커버리
         poisonSchedule = tMan.register(new Runnable() {

         @Override
         public void run() {
         for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
         final MapleCharacter chr = ((MapleCharacter) mo);
         if (mist.makeChanceResult()) {
         chr.addHP((int) (mist.getSource().getX() * (chr.getStat().getMaxHp() / 100.0)));
         chr.addMP((int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0)));
         }
         }
         }
         }, 2000, 2500);
         break;
         }
         case 6: { //속박술
         final MapleCharacter owner = getCharacterById(mist.getOwnerId());
         poisonSchedule = tMan.scheduleAtTimestamp(new Runnable() {
         @Override
         public void run() {
         for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
         if (mist.makeChanceResult()) {
         ((MapleMonster) mo).applyStatus(owner.getClient(), MonsterStatus.MS_Freeze, new MonsterStatusEffect(mist.getSourceSkill().getId(), mist.getDuration()), 1, mist.getSource());
         }
         }
         }
         }, mist.getDuration() - 2000);
         break;
         }
         default:
         poisonSchedule = null;
         break;
         }
         if (poisonSchedule != null) {
         mist.setPoisonSchedule(poisonSchedule);
         }*/
    }

    public final void removeMist(int skillid) {
        for (MapleMist mist : getAllMistsThreadsafe()) {
            if (mist.getSourceSkill() != null) {
                if (mist.getSourceSkill().getId() == skillid) {
                    broadcastMessage(CField.removeMist(mist));
                    removeMapObject(mist);
                }
            }
        }
    }

    public final void removeMistByOwner(MapleCharacter chr, int skillid) {
        for (MapleMist mist : getAllMistsThreadsafe()) {
            if (mist.getSourceSkill() != null) {
                if (mist.getOwnerId() == chr.getId()) {
                    if (mist.getSourceSkill().getId() == skillid) {
                        broadcastMessage(CField.removeMist(mist));
                        removeMapObject(mist);
                    }
                }
            }
        }
    }

    public final void disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(CField.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 3, owner.getBuffedEffect(MapleBuffStat.PickPocket) != null), drop.getTruePosition());
    }

    public final void spawnMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, dropper.getTruePosition(), droppos, (byte) 1, owner.getBuffedEffect(MapleBuffStat.PickPocket) != null));
            }
        });
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    public final void spawnMobMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final MapleMapItem mdrop = new MapleMapItem(meso, position, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, dropper.getTruePosition(), position, (byte) 1, owner.getBuffedEffect(MapleBuffStat.PickPocket) != null));
            }
        });
        boolean magnetpet = false;
        for (int i = 0; i < owner.getPets().length; ++i) {
            if (owner.getPets()[i] != null) {
                if (owner.getPets()[i].getPetItemId() == 5000930 || owner.getPets()[i].getPetItemId() == 5000931 || owner.getPets()[i].getPetItemId() == 5000932) {
                    magnetpet = true;
                }
            }
        }
        //자석펫
        /* if (magnetpet) {
         owner.gainMeso(meso, true);
         InventoryHandler.removeItem(owner, mdrop, getMapObject(mdrop.getObjectId(), MapleMapObjectType.ITEM));
         owner.getClient().getSession().writeAndFlush(CWvsContext.enableActions(owner));
         }*/
        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }

    public final void spawnFlyingDrop(MapleCharacter chr, Point startPos, Point dropPos, Item idrop) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, null, chr, (byte) 2, false, 0);
        mdrop.setFlyingSpeed(Randomizer.rand(50, 150));
        mdrop.setFlyingAngle(Randomizer.rand(0x37, 0xC7));
        mdrop.setTouchDrop(true);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, startPos, dropPos, (byte) 1, false));
            }
        });

        mdrop.registerExpire(120000);
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnMobFlyingDrop(MapleCharacter chr, MapleMonster mob, Point dropPos, Item idrop) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, (byte) 2, false, 0);
        mdrop.setFlyingSpeed(0x96);
        mdrop.setFlyingAngle(Randomizer.rand(0x37, 0xC7));
        mdrop.setTouchDrop(true);

        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 1, false));
            }
        });

        mdrop.registerExpire(120000);
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnMobDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final int questid) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);

        if (mdrop.getItemId() != 4001536) {
            spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
                @Override
                public void sendPackets(MapleClient c) {
                    if (c != null && (questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1) && mob != null && dropPos != null) {
                        c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 1, chr.getBuffedEffect(MapleBuffStat.PickPocket) != null));
                    }
                }
            });
        }
        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnMobPublicDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final int questid) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, (byte) 3, false, questid);

        switch (idrop.getItemId()) {
            case 2023484:
            case 2023494:
            case 2023495:
            case 2023669:
                mdrop.setTouchDrop(true);
                break;
            case 2434851:
                mdrop.setTouchDrop(true);
                mdrop.setDropType((byte) 0);
                break;
        }

        mdrop.setPublicDropId(chr.getClient().getAccID());

        if (mdrop.getItemId() != 4001536) {
            spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {
                @Override
                public void sendPackets(MapleClient c) {
                    if (c != null && (questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1) && mob != null && dropPos != null && c.getAccID() == chr.getClient().getAccID()) {
                        c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 1, chr.getBuffedEffect(MapleBuffStat.PickPocket) != null));
                    }
                }
            });
        }
        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnRandDrop() {
        if (mapid != 910000000 || channel != 1) {
            return; //fm, ch1
        }

        for (MapleMapObject o : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            if (((MapleMapItem) o).isRandDrop()) {
                return;
            }
        }
        MapTimer.getInstance().schedule(new Runnable() {

            public void run() {
                final Point pos = new Point(Randomizer.nextInt(800) + 531, -806);
                final int theItem = Randomizer.nextInt(1000);
                int itemid = 0;
                if (theItem < 950) { //0-949 = normal, 950-989 = rare, 990-999 = super
                    itemid = GameConstants.normalDrops[Randomizer.nextInt(GameConstants.normalDrops.length)];
                } else if (theItem < 990) {
                    itemid = GameConstants.rareDrops[Randomizer.nextInt(GameConstants.rareDrops.length)];
                } else {
                    itemid = GameConstants.superDrops[Randomizer.nextInt(GameConstants.superDrops.length)];
                }
                spawnAutoDrop(itemid, pos);
            }
        }, 20000);
    }

    public final void spawnAutoDrop(final int itemid, final Point pos) {
        Item idrop = null;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(itemid) == MapleInventoryType.DECORATION) {
            idrop = ((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, (byte) 0);
        }
        idrop.setGMLog("Dropped from auto " + " on " + mapid);
        final MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, new DelayedPacketCreation() {

            @Override
            public void sendPackets(MapleClient c) {
                c.getSession().writeAndFlush(CField.dropItemFromMapObject(mdrop, pos, pos, (byte) 1, false));
            }
        });
        broadcastMessage(CField.dropItemFromMapObject(mdrop, pos, pos, (byte) 0, false));
        if (itemid / 10000 != 291) {
            mdrop.registerExpire(120000);
        }
    }

    public final void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point droppos = calcDropPos(pos, pos);
        Equip equip = null;
        if (item.getType() == 1) {
            equip = (Equip) item;
        }
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 2, playerDrop, equip);

        try {

            spawnAndAddRangedMapObject(drop, new DelayedPacketCreation() {

                @Override
                public void sendPackets(MapleClient c) {
                    c.getSession().writeAndFlush(CField.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 1, false));
                }
            });
            broadcastMessage(CField.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 0, false));

            if (!everlast) {
                drop.registerExpire(120000);
                activateItemReactors(drop, owner.getClient());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void activateItemReactors(final MapleMapItem drop, final MapleClient c) {
        final Item item = drop.getItem();

        for (final MapleMapObject o : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
            final MapleReactor react = (MapleReactor) o;

            if (react.getReactorType() == 100) {
                if (item.getItemId() == GameConstants.getCustomReactItem(react.getReactorId(), react.getReactItem().getLeft()) && react.getReactItem().getRight() == item.getQuantity()) {
                    if (react.getArea().contains(drop.getTruePosition())) {
                        if (!react.isTimerActive()) {
                            MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                            react.setTimerActive(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    public int getItemsSize() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getExtractorSize() {
        return mapobjects.get(MapleMapObjectType.EXTRACTOR).size();
    }

    public List<MapleMapItem> getAllItems() {
        return getAllItemsThreadsafe();
    }

    public List<MapleMapItem> getAllItemsThreadsafe() {
        ArrayList<MapleMapItem> ret = new ArrayList<MapleMapItem>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            ret.add((MapleMapItem) mmo);
        }
        return ret;
    }

    public Point getPointOfItem(int itemid) {
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            MapleMapItem mm = ((MapleMapItem) mmo);
            if (mm.getItem() != null && mm.getItem().getItemId() == itemid) {
                return mm.getPosition();
            }
        }
        return null;
    }

    public List<MapleMist> getAllMistsThreadsafe() {
        ArrayList<MapleMist> ret = new ArrayList<MapleMist>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MIST).values()) {
            ret.add((MapleMist) mmo);
        }
        return ret;
    }

    public final void returnEverLastItem(final MapleCharacter chr) {
        for (final MapleMapObject o : getAllItemsThreadsafe()) {
            final MapleMapItem item = ((MapleMapItem) o);
            if (item.getOwner() == chr.getId()) {
                item.setPickedUp(true);
                broadcastMessage(CField.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getTruePosition());
                if (item.getMeso() > 0) {
                    chr.gainMeso(item.getMeso(), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
                }
                removeMapObject(item);
            }
        }
        spawnRandDrop();
    }

    public final void talkMonster(final String msg, final int itemId, final int objectid) {
        if (itemId > 0) {
            startMapEffect(msg, itemId, false);
        }
        broadcastMessage(MobPacket.talkMonster(objectid, itemId, msg)); //5120035
        broadcastMessage(MobPacket.removeTalkMonster(objectid));
    }

    public final void startMapEffect(final String msg, final int itemId) {
        startMapEffect(msg, itemId, false, 30000);
    }

    public final void startMapEffect(final String msg, final int itemId, final boolean jukebox) {
        startMapEffect(msg, itemId, jukebox, 30000);
    }

    public final void startMapEffect(final String msg, final int itemId, int time) {
        startMapEffect(msg, itemId, false, time);
    }

    public final void startMapEffect(final String msg, final int itemId, final boolean jukebox, int time) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        mapEffect.setJukebox(jukebox);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (mapEffect != null) {
                    broadcastMessage(mapEffect.makeDestroyData());
                    mapEffect = null;
                }
            }
        }, jukebox ? 300000 : time);
    }

    public final void startExtendedMapEffect(final String msg, final int itemId) {
        broadcastMessage(CField.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                broadcastMessage(CField.removeMapEffect());
                broadcastMessage(CField.startMapEffect(msg, itemId, false));
                //dont remove mapeffect.
            }
        }, 60000);
    }

    public final void startSimpleMapEffect(final String msg, final int itemId) {
        broadcastMessage(CField.startMapEffect(msg, itemId, true));
    }

    public final void startJukebox(final String msg, final int itemId) {
        startMapEffect(msg, itemId, true);
    }

    public final void addPlayer(final MapleCharacter chr) {
        mapobjects.get(MapleMapObjectType.PLAYER).put(chr.getObjectId(), chr);

        characters.add(chr);

        chr.setChangeTime();
        if (GameConstants.isZero(chr.getJob())) {
            Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            Item subWeapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            if (weapon != null && subWeapon != null) {
                if (weapon.getItemId() / 1000 == 1562 && subWeapon.getItemId() / 1000 == 1572) { // 무기 위치가 다를 때
                    chr.getInventory(MapleInventoryType.EQUIPPED).move((short) -10, (short) -11, (short) 1);
                    chr.getClient().getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, weapon));
                    chr.getClient().getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, subWeapon));
                }
            }
        }

        chr.getClient().getSession().writeAndFlush(SecurityPCPacket.sendSPC1());
        chr.getClient().getSession().writeAndFlush(SecurityPCPacket.sendSPC2());
        chr.getClient().getSession().writeAndFlush(SecurityPCPacket.sendSPC3());

//        chr.getClient().getSession().writeAndFlush(CField.UIPacket.openUI(154)); //리턴쇼 방송 팝업창
        if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -27) != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -28) != null && chr.getMapId() != ServerConstants.WarpMap) {
            if (chr.getAndroid() == null) {
                chr.setAndroid(chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -27).getAndroid());
            } else {
                chr.updateAndroid();
            }
        }
        if (GameConstants.isDemonAvenger(chr.getJob())) {
            EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.LifeTidal, new Pair<>((int) 3, 0));
            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, chr));
        }
        if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
            chr.setTeam(getAndSwitchTeam() ? 0 : 1);
        }
        final byte[] packet = CField.spawnPlayerMapobject(chr);
        final byte[] packet2 = SLFCGPacket.SetupZodiacInfo();
        final byte[] packet3 = SLFCGPacket.ZodiacRankInfo(chr.getId(), (int) chr.getKeyValue(190823, "grade"));

        if (!chr.isHidden()) {
            broadcastMessage(chr, packet, false);

            if (chr.getKeyValue(190823, "grade") > 0) {
                broadcastMessage(chr, packet2, true);
                broadcastMessage(chr, packet3, true);
            }

            if (chr.isIntern() && speedRunStart > 0) {
                endSpeedRun();
                broadcastMessage(CWvsContext.serverNotice(5, "", "The speed run has ended."));
            }
        } else {
            broadcastGMMessage(chr, packet, false);
        }
        if (GameConstants.isPhantom(chr.getJob())) {
            chr.getClient().getSession().writeAndFlush(CField.updateCardStack(false, chr.getCardStack()));
        }

        sendObjectPlacement(chr);

        chr.getClient().getSession().writeAndFlush(packet);

        if (chr.getGuild() != null && chr.getGuild().getCustomEmblem() != null) {
            broadcastMessage(chr, CField.loadGuildIcon(chr), false);
        }
        if (chr.getPickPocket() > 0) {
            chr.setPickPocket(0);
        }
        if (chr.getDeathCount() > 0) {
            if (chr.getMapId() >= 450013000 && chr.getMapId() <= 450013800) {
                chr.getClient().getSession().writeAndFlush(UIPacket.openUI(1204));
            }
            if (chr.getMapId() == 450010500) {
                chr.getClient().getSession().writeAndFlush(CField.JinHillah(3, chr));
            } else {
                chr.getClient().getSession().writeAndFlush(CField.getDeathCount(chr.getDeathCount()));
            }
        }

        if (isTown()) {
            chr.getClient().getSession().writeAndFlush(CField.quickMove(ServerConstants.quicks));
        } else {
            chr.getClient().getSession().writeAndFlush(CField.quickMove(ServerConstants.quicks));
        }

        GameConstants.achievementRatio(chr.getClient(), mapid);
        switch (mapid) {
            case 809000101:
            case 809000201:
                chr.getClient().getSession().writeAndFlush(CField.showEquipEffect());
                break;
            case 109090300: //양떼목장
                chr.getClient().getSession().writeAndFlush(CField.showEquipEffect(chr.isCatching ? 1 : 0));
                break;
            case 450002011:
            case 450002012:
            case 450002013:
            case 450002014:
            case 450002015:
            case 450002021:
            case 450002200:
            case 450002201:
            case 450002301:
            case 921170050:
            case 921170100:
            case 921171200:
            case 993000868:
            case 993000869:
            case 993000870:
            case 993000871:
            case 993000872:
            case 993000873:
            case 993000874:
            case 993000875:
            case 993000877:
                chr.getClient().getSession().writeAndFlush(CField.momentAreaOnOffAll(Collections.singletonList("swim01")));
                break;
            case 350160100:
            case 350160200:
                chr.getMap().broadcastMessage(CField.UseSkillWithUI(13, 80001974, 1));
                chr.getClient().getSession().writeAndFlush(MobPacket.CorruptionChange((byte) 0, getStigmaDeath()));
                break;
            case 350160140:
            case 350160240:
                chr.getMap().broadcastMessage(CField.UseSkillWithUI(13, 80001974, 1));
                chr.getClient().getSession().writeAndFlush(MobPacket.CorruptionChange((byte) 1, getStigmaDeath()));
                break;
            case 450004150:
            case 450004250:
            case 450004450:
            case 450004550:
                chr.getClient().getSession().writeAndFlush(BossLucid.changeStatueState(false, getLucidCount(), false));
                break;
            case 450008150:
            case 450008750:
                chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.setMoonGauge(100, 45));
                chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(chr.getMoonGauge()));
                break;
            case 450008250:
            case 450008850:
                chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.setMoonGauge(100, 50));
                chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(chr.getMoonGauge()));
                break;
            case 450008350:
            case 450008950:
                chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.setMoonGauge(100, 25));
                chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(chr.getMoonGauge()));
                break;
            case 35016260:
            case 35016220:
            case 35016160:
            case 35016120:
                chr.getClient().getSession().writeAndFlush(CField.UseSkillWithUI(0, 0, 0));
                chr.getClient().getSession().writeAndFlush(MobPacket.CorruptionChange((byte) 0, 0));
                break;
            default:
                chr.getClient().getSession().writeAndFlush(CField.UseSkillWithUI(0, 0, 0));
                chr.getClient().getSession().writeAndFlush(MobPacket.CorruptionChange((byte) 0, 0));
                if (chr.Stigma > 0) {
                    chr.Stigma = 0;
                    Map<MapleBuffStat, Pair<Integer, Integer>> cancelList = new HashMap<>();
                    cancelList.put(MapleBuffStat.Stigma, new Pair<>(0, 0));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(cancelList, chr));
                    chr.getMap().broadcastMessage(chr, BuffPacket.cancelForeignBuff(chr, cancelList), false);
                    chr.getMap().broadcastMessage(MobPacket.addStigma(chr, 0));
                }
                break;
        }

        for (int i = 0; i < 3; ++i) {
            if (chr.getPet(i) != null) { // 마을에선 펫 감추게
                chr.getClient().getSession().writeAndFlush(PetPacket.updatePet(chr, chr.getPet(i), chr.getInventory(MapleInventoryType.CASH).getItem((short) chr.getPet(i).getInventoryPosition()), false, chr.getPetLoot()));
                //  if (chr.getMapId() != ServerConstants.WarpMap) { // 마을에선 펫 감추게
                chr.getPet(i).setPos(new Point(0, 18)); //펫 좌표 업데이트
                broadcastMessage(chr, PetPacket.showPet(chr, chr.getPet(i), false, false), true);
                chr.getClient().getSession().writeAndFlush(PetPacket.petExceptionList(chr, chr.getPet(i)));
//                  chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr, false, true));
                //   }
            }
        }

        if (chr.getAndroid() != null && chr.getMapId() != ServerConstants.WarpMap) {
            chr.getAndroid().setPos(chr.getPosition());
            broadcastMessage(CField.spawnAndroid(chr, chr.getAndroid()));
        }
        if (chr.getParty() != null) {
            chr.silentPartyUpdate();
            chr.getClient().getSession().writeAndFlush(PartyPacket.updateParty(chr.getClient().getChannel(), chr.getParty(), PartyOperation.SILENT_UPDATE, null));
            chr.updatePartyMemberHP();
            chr.receivePartyMemberHP();
        }

        if (!onFirstUserEnter.isEmpty()) {
            if (isFirstUserEnter()) {
                setFirstUserEnter(false);
                MapScriptMethods.startScript_FirstUser(chr.getClient(), onFirstUserEnter);
            }
        }

        if (!onUserEnter.isEmpty()) {
            MapScriptMethods.startScript_User(chr.getClient(), onUserEnter);
        }

        List<MapleSummon> allSummons = chr.getSummons();

        Iterator<MapleSummon> s = allSummons.iterator();
        while (s.hasNext()) {
            MapleSummon summon = s.next();
            if (summon.getMovementType() != SummonMovementType.STATIONARY || summon.getSkill() == 152101000) {
                summon.setPosition(chr.getTruePosition());
                chr.addVisibleMapObject(summon);
                this.spawnSummon(summon);
            }
        }

        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (chr.getBuffedValue(MapleBuffStat.RideVehicle) != null && !GameConstants.isResist(chr.getJob())) {
            if (FieldLimitType.Mount.check(fieldLimit)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
            }
        }

        if (chr.getBuffedValue(80001757)) {
            chr.cancelEffect(SkillFactory.getSkill(80001757).getEffect(1), true, -1);
        }

        if (!chr.getBuffedValue(80003082)) {
            SkillFactory.getSkill(80003082).getEffect(chr.getSkillLevel(80003082)).applyTo(chr, true);
        }

        if (chr.getBuffedValue(80001752)) {
            chr.cancelEffect(SkillFactory.getSkill(80001752).getEffect(1), true, -1);
        }

        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            if (chr.inPVP()) {
                chr.getClient().getSession().writeAndFlush(CField.getPVPClock(Integer.parseInt(chr.getEventInstance().getProperty("type")), (int) (chr.getEventInstance().getTimeLeft() / 1000)));
            } else {
                chr.getClient().getSession().writeAndFlush(CField.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
            }
        }
        if (hasClock()) {
            final Calendar cal = Calendar.getInstance();
            chr.getClient().getSession().writeAndFlush((CField.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }

        if (getMapTimer() > 0) {
            chr.getClient().getSession().writeAndFlush(CField.getClock((int) ((getMapTimer() - System.currentTimeMillis()) / 1000)));
        }
        if (chr.getCarnivalParty() != null && chr.getEventInstance() != null) {
            chr.getEventInstance().onMapLoad(chr);
        }

        chr.getClient().getSession().writeAndFlush(CField.specialChair(chr, true, true, true, null));

        if (eliteBossAppeared) {
            broadcastMessage(CField.specialMapEffect(2, false, "Bgm36.img/RoyalGuard", "", "Effect/EliteMobEff.img/eliteMonster", "", ""));
        }

        if (burning > 0 && chr.getEventInstance() == null) {
            chr.getClient().getSession().writeAndFlush(CField.playSound("Sound/FarmSE.img/boxResult"));
            chr.getClient().getSession().writeAndFlush(EffectPacket.showBurningFieldEffect("#fn나눔고딕 ExtraBold##fs26#          버닝 " + burning + "단계 : 경험치 " + burning * 10 + "% 추가지급!!          "));
        }

        if (!chr.getBuffedValue(80002282) && runeCurse > 0) {
            chr.getClient().getSession().writeAndFlush(CField.runeCurse("룬을 해방하여 엘리트 보스의 저주를 풀어야 합니다!!\\n저주 " + runeCurse + "단계 :  경험치 획득, 드롭률 " + getRuneCurseDecrease() + "% 감소 효과 적용 중", false));
        }

        MapleEvent.mapLoad(chr, channel);
        if (getSquadBegin() != null && getSquadBegin().getTimeLeft() > 0 && getSquadBegin().getStatus() == 1) {
            chr.getClient().getSession().writeAndFlush(CField.getClock((int) (getSquadBegin().getTimeLeft() / 1000)));
        }
        if (mapid / 1000 != 105100 && mapid / 100 != 8020003 && mapid / 100 != 8020008 && mapid != 271040100) { //no boss_balrog/2095/coreblaze/auf/cygnus. but coreblaze/auf/cygnus does AFTER
            final MapleSquad sqd = getSquadByMap(); //for all squads
            final EventManager em = getEMByMap();
            if (!squadTimer && sqd != null && chr.getName().equals(sqd.getLeaderName()) && em != null && em.getProperty("leader") != null && em.getProperty("leader").equals("true") && checkStates) {
                //leader? display
                doShrine(false);
                squadTimer = true;
            }
        }
        if (getNumMonsters() > 0 && (mapid == 280030001 || mapid == 240060201 || mapid == 280030000 || mapid == 240060200 || mapid == 220080001 || mapid == 541020800 || mapid == 541010100)) {
            String music = "Bgm09/TimeAttack";
            switch (mapid) {
                case 240060200:
                case 240060201:
                    music = "Bgm14/HonTale";
                    break;
                case 280030000:
                case 280030001:
                    music = "Bgm06/FinalFight";
                    break;
            }
            chr.getClient().getSession().writeAndFlush(CField.musicChange(music));
            //maybe timer too for zak/ht
        }

        if (GameConstants.isEvan(chr.getJob()) && chr.getJob() >= 2200) {
            if (chr.getDragon() == null) {
                chr.makeDragon();
            } else {
                chr.getDragon().setPosition(chr.getPosition());
            }
            if (chr.getDragon() != null) {
                broadcastMessage(CField.spawnDragon(chr.getDragon()));
            }
        }

        if (permanentWeather > 0) {
            chr.getClient().getSession().writeAndFlush(CField.startMapEffect("", permanentWeather, false)); //snow, no msg
        }
        if (mapid == 450004250 || mapid == 450004550) {
            //내일 루시드 코딩 부탁
        } else if (getNodez().getEnvironments().size() > 0) {
            chr.getClient().getSession().writeAndFlush(CField.getUpdateEnvironment(getNodez().getEnvironments()));
        }
        //if (partyBonusRate > 0) {
        //    chr.dropMessage(-1, partyBonusRate + "% additional EXP will be applied per each party member here.");
        //    chr.dropMessage(-1, "You've entered the party play zone.");
        //}
        if (isTown()) {
//            chr.cancelEffectFromBuffStat(MapleBuffStat.RAINING_MINES);
        }
        if (!canSoar()) {
//            chr.cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        }
        if (chr.getBuffedValue(MapleBuffStat.RepeatEffect) != null) {
            int skillid = chr.getBuffedEffect(MapleBuffStat.RepeatEffect).getSourceId();
            if (GameConstants.isAngelicBlessBuffEffectItem(skillid)) {
                EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
                statups.put(MapleBuffStat.RepeatEffect, new Pair<>(1, 0));
                broadcastMessage(BuffPacket.giveForeignBuff(chr, statups, chr.getBuffedEffect(MapleBuffStat.RepeatEffect)));
            }
        }

        for (int i = 0; i < 8; i++) {
            chr.setEffect(i, 0);
        }

        chr.refreshGiftShowX3();
    }

    public int getNumItems() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getNumMonsters() {
        int size = 0;
        for (MapleMonster mob : getAllMonster()) {
            if (!(mob.getId() >= 8920004 && mob.getId() <= 8920006))
                size += 1;
        }
        return size;
    }

    public void doShrine(final boolean spawned) { //false = entering map, true = defeated
        if (squadSchedule != null) {
            cancelSquadSchedule(true);
        }
        final MapleSquad sqd = getSquadByMap();
        if (sqd == null) {
            return;
        }
        final int mode = (mapid == 280030000 ? 1 : (mapid == 280030001 ? 2 : (mapid == 240060200 || mapid == 240060201 ? 3 : 0)));
        //chaos_horntail message for horntail too because it looks nicer
        final EventManager em = getEMByMap();
        if (sqd != null && em != null && getCharactersSize() > 0) {
            final String leaderName = sqd.getLeaderName();
            final String state = em.getProperty("state");
            final Runnable run;
            MapleMap returnMapa = getForcedReturnMap();
            if (returnMapa == null || returnMapa.getId() == mapid) {
                returnMapa = getReturnMap();
            }
            if (mode == 1 || mode == 2) { //chaoszakum
                broadcastMessage(CField.showChaosZakumShrine(spawned, 5));
            } else if (mode == 3) { //ht/chaosht
                broadcastMessage(CField.showChaosHorntailShrine(spawned, 5));
            } else {
                broadcastMessage(CField.showHorntailShrine(spawned, 5));
            }
            if (spawned) { //both of these together dont go well
                broadcastMessage(CField.getClock(300)); //5 min
            }
            final MapleMap returnMapz = returnMapa;
            if (!spawned) { //no monsters yet; inforce timer to spawn it quickly
                final List<MapleMonster> monsterz = getAllMonstersThreadsafe();
                final List<Integer> monsteridz = new ArrayList<Integer>();
                for (MapleMapObject m : monsterz) {
                    monsteridz.add(m.getObjectId());
                }
                run = new Runnable() {

                    public void run() {
                        final MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        if (MapleMap.this.getCharactersSize() > 0 && MapleMap.this.getNumMonsters() == monsterz.size() && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            boolean passed = monsterz.isEmpty();
                            for (MapleMapObject m : MapleMap.this.getAllMonstersThreadsafe()) {
                                for (int i : monsteridz) {
                                    if (m.getObjectId() == i) {
                                        passed = true;
                                        break;
                                    }
                                }
                                if (passed) {
                                    break;
                                } //even one of the monsters is the same
                            }
                            if (passed) {
                                //are we still the same squad? are monsters still == 0?
                                byte[] packet;
                                if (mode == 1 || mode == 2) { //chaoszakum
                                    packet = CField.showChaosZakumShrine(spawned, 0);
                                } else {
                                    packet = CField.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
                                }
                                for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                    chr.getClient().getSession().writeAndFlush(packet);
                                    chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                                }
                                checkStates("");
                                resetFully();
                            }
                        }

                    }
                };
            } else { //inforce timer to gtfo
                run = new Runnable() {

                    public void run() {
                        MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        //we dont need to stop clock here because they're getting warped out anyway
                        if (MapleMap.this.getCharactersSize() > 0 && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            //are we still the same squad? monsters however don't count
                            byte[] packet;
                            if (mode == 1 || mode == 2) { //chaoszakum
                                packet = CField.showChaosZakumShrine(spawned, 0);
                            } else {
                                packet = CField.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
                            }
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                chr.getClient().getSession().writeAndFlush(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }
                };
            }
            squadSchedule = MapTimer.getInstance().schedule(run, 300000); //5 mins
        }
    }

    public final MapleSquad getSquadByMap() {
        MapleSquadType zz = null;
        switch (mapid) {
            case 105100400:
            case 105100300:
                zz = MapleSquadType.bossbalrog;
                break;
            case 280030000:
                zz = MapleSquadType.zak;
                break;
            case 280030001:
                zz = MapleSquadType.chaoszak;
                break;
            case 240060200:
                zz = MapleSquadType.horntail;
                break;
            case 240060201:
                zz = MapleSquadType.chaosht;
                break;
            case 270050100:
                zz = MapleSquadType.pinkbean;
                break;
            case 802000111:
                zz = MapleSquadType.nmm_squad;
                break;
            case 802000211:
                zz = MapleSquadType.vergamot;
                break;
            case 802000311:
                zz = MapleSquadType.tokyo_2095;
                break;
            case 802000411:
                zz = MapleSquadType.dunas;
                break;
            case 802000611:
                zz = MapleSquadType.nibergen_squad;
                break;
            case 802000711:
                zz = MapleSquadType.dunas2;
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                zz = MapleSquadType.core_blaze;
                break;
            case 802000821:
            case 802000823:
                zz = MapleSquadType.aufheben;
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                zz = MapleSquadType.vonleon;
                break;
            case 551030200:
                zz = MapleSquadType.scartar;
                break;
            case 271040100:
                zz = MapleSquadType.cygnus;
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getMapleSquad(zz);
    }

    public final MapleSquad getSquadBegin() {
        if (squad != null) {
            return ChannelServer.getInstance(channel).getMapleSquad(squad);
        }
        return null;
    }

    public final EventManager getEMByMap() {
        String em = null;
        switch (mapid) {
            case 105100400:
                em = "BossBalrog_EASY";
                break;
            case 105100300:
                em = "BossBalrog_NORMAL";
                break;
            case 280030000:
                em = "ZakumBattle";
                break;
            case 240060200:
                em = "HorntailBattle";
                break;
            case 280030001:
                em = "ChaosZakum";
                break;
            case 240060201:
                em = "ChaosHorntail";
                break;
            case 270050100:
                em = "PinkBeanBattle";
                break;
            case 802000111:
                em = "NamelessMagicMonster";
                break;
            case 802000211:
                em = "Vergamot";
                break;
            case 802000311:
                em = "2095_tokyo";
                break;
            case 802000411:
                em = "Dunas";
                break;
            case 802000611:
                em = "Nibergen";
                break;
            case 802000711:
                em = "Dunas2";
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                em = "CoreBlaze";
                break;
            case 802000821:
            case 802000823:
                em = "Aufhaven";
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                em = "VonLeonBattle";
                break;
            case 551030200:
                em = "ScarTarBattle";
                break;
            case 271040100:
                em = "CygnusBattle";
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getEventSM().getEventManager(em);
    }

    public final void removePlayer(final MapleCharacter chr) {
        //log.warn("[dc] [level2] Player {} leaves map {}", new Object[] { chr.getName(), mapid });

        if (everlast) {
            returnEverLastItem(chr);
        }

        characters.remove(chr);

        removeMapObject(chr);
        chr.checkFollow();
        chr.removeExtractor();
        broadcastMessage(CField.removePlayerFromMap(chr.getId()));

        if (characters.size() == 0) {
            setFirstUserEnter(true);
        }

        List<MapleSummon> allSummons = chr.getSummons();

        for (final MapleSummon summon : allSummons) {
            if (summon.getSkill() == 152101000) {
                chr.CrystalCharge = summon.getEnergy();
            } else if (summon.getMovementType() != SummonMovementType.STATIONARY) {
                summon.removeSummon(this, true);
//                chr.dispelSkill(summon.getSkill()); //remove the buff
            }
        }

        checkStates(chr.getName());
        if (mapid == 109020001) {
            chr.canTalk(true);
        }
        chr.leaveMap(this);
    }

    public final void broadcastMessage(final byte[] packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getTruePosition());
    }

    /*	public void broadcastMessage(MapleCharacter source, byte[] packet, boolean repeatToSource, boolean ranged) {
     broadcastMessage(repeatToSource ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ : Double.POSITIVE_INFINITY, source.getPosition());
     }*/
    public final void broadcastMessage(final byte[] packet, final Point rangedFrom) {
        broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final Point rangedFrom) {
        broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(final MapleCharacter source, final byte[] packet, final double rangeSq, final Point rangedFrom) {
        final Iterator<MapleCharacter> itr = characters.iterator();
        while (itr.hasNext()) {
            MapleCharacter chr = itr.next();
            if (chr != source) {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getTruePosition()) <= rangeSq) {
                        chr.getClient().getSession().writeAndFlush(packet);
                    }
                } else {
                    chr.getClient().getSession().writeAndFlush(packet);
                }
            }
        }
    }

    private void sendObjectPlacement(final MapleCharacter c) {
        if (c == null) {
            return;
        }
        for (final MapleMapObject o : getMapObjectsInRange(c.getTruePosition(), c.getRange(), GameConstants.rangedMapobjectTypes)) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (!((MapleReactor) o).isAlive()) {
                    continue;
                }
            }
            o.sendSpawnData(c.getClient());
            c.addVisibleMapObject(o);
        }
    }

    public final List<MaplePortal> getPortalsInRange(final Point from, final double rangeSq) {
        final List<MaplePortal> ret = new ArrayList<MaplePortal>();
        for (MaplePortal type : portals.values()) {
            if (from.distanceSq(type.getPosition()) <= rangeSq && type.getTargetMapId() != mapid && type.getTargetMapId() != 999999999) {
                ret.add(type);
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
            while (itr.hasNext()) {
                MapleMapObject mmo = itr.next();
                if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                    ret.add(mmo);
                }
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Arrays.asList(MapleMapObjectType.ITEM));
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapObject_types) {
            Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
            while (itr.hasNext()) {
                MapleMapObject mmo = itr.next();

                if (from.distanceSq(mmo.getTruePosition()) <= rangeSq || mmo.getType() == MapleMapObjectType.RUNE) {
                    ret.add(mmo);
                }
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRect(final Rectangle box, final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<MapleMapObject>();
        for (MapleMapObjectType type : MapObject_types) {
            Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
            while (itr.hasNext()) {
                MapleMapObject mmo = itr.next();
                if (box.contains(mmo.getTruePosition())) {
                    ret.add(mmo);
                }
            }
        }
        return ret;
    }

    public final List<MapleCharacter> getCharactersIntersect(final Rectangle box) {
        final List<MapleCharacter> ret = new ArrayList<MapleCharacter>();
        final Iterator<MapleCharacter> itr = characters.iterator();
        while (itr.hasNext()) {
            MapleCharacter chr = itr.next();
            if (chr.getBounds().intersects(box)) {
                ret.add(chr);
            }
        }
        return ret;
    }

    public final List<MapleCharacter> getPlayersInRectAndInList(final Rectangle box, final List<MapleCharacter> chrList) {
        final List<MapleCharacter> character = new LinkedList<MapleCharacter>();

        final Iterator<MapleCharacter> ltr = characters.iterator();
        MapleCharacter a;
        while (ltr.hasNext()) {
            a = ltr.next();
            if (chrList.contains(a) && box.contains(a.getTruePosition())) {
                character.add(a);
            }
        }
        return character;
    }

    public final void addPortal(final MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public final MaplePortal getPortal(final String portalname) {
        for (final MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public final MaplePortal getPortal(final int portalid) {
        return portals.get(portalid);
    }

    public final List<MaplePortal> getPortalSP() {
        List<MaplePortal> res = new LinkedList<MaplePortal>();
        for (final MaplePortal port : portals.values()) {
            if (port.getName().equals("sp")) {
                res.add(port);
            }
        }
        return res;
    }

    public final void resetPortals() {
        for (final MaplePortal port : portals.values()) {
            port.setPortalState(true);
        }
    }

    public final void setFootholds(final MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public final MapleFootholdTree getFootholds() {
        return footholds;
    }

    public final int getNumSpawnPoints() {
        return monsterSpawn.size();
    }

    public final List<Spawns> getSpawnPoints() {
        return monsterSpawn;
    }

    public final void loadMonsterRate(final boolean first) {
        final int spawnSize = monsterSpawn.size();
        if (spawnSize >= 20 || partyBonusRate > 0) {
            maxRegularSpawn = Math.round(spawnSize / monsterRate);
        } else {
            maxRegularSpawn = (int) Math.ceil(spawnSize * monsterRate);
        }
        if (fixedMob > 0) {
            maxRegularSpawn = fixedMob;
        } else if (maxRegularSpawn <= 2) {
            maxRegularSpawn = 2;
        } else if (maxRegularSpawn > spawnSize) {
            maxRegularSpawn = Math.max(10, spawnSize);
        }

        maxRegularSpawn += maxRegularSpawn * 5;
        Collection<Spawns> newSpawn = new LinkedList<Spawns>();
        Collection<Spawns> newBossSpawn = new LinkedList<Spawns>();
        for (final Spawns s : monsterSpawn) {
            if (s.getCarnivalTeam() >= 2) {
                continue; // Remove carnival spawned mobs
            }
            if (s.getMonster().isBoss()) {
                newBossSpawn.add(s);
            } else {
                newSpawn.add(s);
            }
        }
        monsterSpawn.clear();
        monsterSpawn.addAll(newBossSpawn);
        monsterSpawn.addAll(newSpawn);

        //2배
        if (first && spawnSize > 0) {
            lastSpawnTime = System.currentTimeMillis();
            if (GameConstants.isForceRespawn(mapid)) {
                createMobInterval = 30000;
            }
            respawn(false); // this should do the trick, we don't need to wait upon entering map
        }
    }

    public final SpawnPoint addMonsterSpawn(final MapleMonster monster, final int mobTime, final byte carnivalTeam, final String msg) {
        final Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        final SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg);
        if (carnivalTeam > -1) {
            monsterSpawn.add(0, sp); //at the beginning
        } else {
            monsterSpawn.add(sp);
        }
        return sp;
    }

    public final void addAreaMonsterSpawn(final MapleMonster monster, Point pos1, Point pos2, Point pos3, final int mobTime, final String msg, final boolean shouldSpawn) {
        pos1 = calcPointBelow(pos1);
        pos2 = calcPointBelow(pos2);
        pos3 = calcPointBelow(pos3);
        if (monster == null) {
            System.out.println(mapid + "맵의 addAreaMonsterSpawn의 몹 데이터가 없음.");
            return;
        }
        if (pos1 != null) {
            pos1.y -= 1;
        }
        if (pos2 != null) {
            pos2.y -= 1;
        }
        if (pos3 != null) {
            pos3.y -= 1;
        }
        if (pos1 == null && pos2 == null && pos3 == null) {
            System.out.println("WARNING: mapid " + mapid + ", monster " + monster.getId() + " could not be spawned.");

            return;
        } else if (pos1 != null) {
            if (pos2 == null) {
                pos2 = new Point(pos1);
            }
            if (pos3 == null) {
                pos3 = new Point(pos1);
            }
        } else if (pos2 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos2);
            }
            if (pos3 == null) {
                pos3 = new Point(pos2);
            }
        } else if (pos3 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos3);
            }
            if (pos2 == null) {
                pos2 = new Point(pos3);
            }
        }
        monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg, shouldSpawn));
    }

    public final List<MapleCharacter> getCharacters() {
        return getCharactersThreadsafe();
    }

    public final List<MapleCharacter> getCharactersThreadsafe() {
        final List<MapleCharacter> chars = new ArrayList<MapleCharacter>();

        final Iterator<MapleCharacter> itr = characters.iterator();
        while (itr.hasNext()) {
            MapleCharacter chr = itr.next();
            chars.add(chr);
        }
        return chars;
    }

    public final MapleCharacter getCharacterByName(final String id) {
        final Iterator<MapleCharacter> itr = characters.iterator();
        while (itr.hasNext()) {
            MapleCharacter mc = itr.next();
            if (mc.getName().equalsIgnoreCase(id)) {
                return mc;
            }
        }
        return null;
    }

    public final MapleCharacter getCharacterById_InMap(final int id) {
        return getCharacterById(id);
    }

    public final MapleCharacter getCharacterById(final int id) {
        final Iterator<MapleCharacter> itr = characters.iterator();
        while (itr.hasNext()) {
            MapleCharacter mc = itr.next();
            if (mc.getId() == id) {
                return mc;
            }
        }
        return null;
    }

    public final void updateMapObjectVisibility(final MapleCharacter chr, final MapleMapObject mo) {
        if (chr == null) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.MIST || mo.getType() == MapleMapObjectType.EXTRACTOR || mo.getType() == MapleMapObjectType.SUMMON || mo.getType() == MapleMapObjectType.RUNE || mo instanceof MechDoor || mo.getTruePosition().distanceSq(chr.getTruePosition()) <= mo.getRange()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else { // monster left view range
            if (!(mo instanceof MechDoor) && mo.getType() != MapleMapObjectType.MIST && mo.getType() != MapleMapObjectType.EXTRACTOR && mo.getType() != MapleMapObjectType.SUMMON && mo.getType() != MapleMapObjectType.RUNE && mo.getTruePosition().distanceSq(chr.getTruePosition()) > mo.getRange()) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            } else if (mo.getType() == MapleMapObjectType.MONSTER) { //monster didn't leave view range, and is visible
                if (chr.getPosition().distanceSq(mo.getPosition()) <= GameConstants.maxViewRangeSq()) {
                    updateMonsterController((MapleMonster) mo);
                }
            }
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);

        final Iterator<MapleCharacter> itr = characters.iterator();
        while (itr.hasNext()) {
            MapleCharacter mc = itr.next();
            updateMapObjectVisibility(mc, monster);
        }
    }

    public void movePlayer(final MapleCharacter player, final Point newPosition) {
        player.setPosition(newPosition);
        Iterator<MapleMapObject> itr = player.getVisibleMapObjects().iterator();
        while (itr.hasNext()) {
            MapleMapObject mo = itr.next();
            if (mo != null && getMapObject(mo.getObjectId(), mo.getType()) == mo) {
                updateMapObjectVisibility(player, mo);
            } else if (mo != null) {
                player.getVisibleMapObjects().remove(mo);
            }
        }
        for (MapleMapObject mo : getMapObjectsInRange(player.getTruePosition(), player.getRange())) {
            if (mo != null && !player.getVisibleMapObjects().contains(mo)) {
                mo.sendSpawnData(player.getClient());
                player.getVisibleMapObjects().add(mo);
            }
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public String spawnDebug() {
        StringBuilder sb = new StringBuilder("Mobs in map : ");
        sb.append(this.getNumMonsters());
        sb.append(" spawnedMonstersOnMap: ");
        sb.append(spawnedMonstersOnMap);
        sb.append(" spawnpoints: ");
        sb.append(monsterSpawn.size());
        sb.append(" maxRegularSpawn: ");
        sb.append(maxRegularSpawn);
        sb.append(" monster rate: ");
        sb.append(monsterRate);
        sb.append(" fixed: ");
        sb.append(fixedMob);

        return sb.toString();
    }

    public int characterSize() {
        return characters.size();
    }

    public final int getMapObjectSize() {
        return mapobjects.size() + getCharactersSize() - characters.size();
    }

    public final int getCharactersSize() {
        int ret = 0;
        final Iterator<MapleCharacter> ltr = characters.iterator();
        MapleCharacter chr;
        while (ltr.hasNext()) {
            chr = ltr.next();
            ret++;
        }
        return ret;
    }

    public MapleCharacter getCharacter(int cid) {
        MapleCharacter ret = null;
        Iterator<MapleCharacter> ltr = characters.iterator();
        MapleCharacter chr;
        while (ltr.hasNext()) {
            chr = ltr.next();
            if (chr.getId() == cid) {
                return chr;
            }
        }
        return ret;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId(), mapitem.getType()) && !mapitem.isPickedUp()) {
                mapitem.expire(MapleMap.this);
                reactor.hitReactor(c);
                reactor.setTimerActive(false);

                if (reactor.getDelay() > 0) {
                    MapTimer.getInstance().schedule(new Runnable() {

                        @Override
                        public void run() {
                            reactor.forceHitReactor((byte) 0);
                        }
                    }, reactor.getDelay());
                }
            } else {
                reactor.setTimerActive(false);
            }
        }
    }

    public void respawn(final boolean force) {
        respawn(force, System.currentTimeMillis());
    }

    public void respawn(final boolean force, final long now) {
        if (eliteBossAppeared) {
            return;
        }
        lastSpawnTime = now;

        int num = getNumMonsters();
        if (spawnedMonstersOnMap.get() != num) {
            spawnedMonstersOnMap.set(num);
        }

        if (force) { //cpq quick hack
            final int numShouldSpawn = monsterSpawn.size() - spawnedMonstersOnMap.get();

            if (numShouldSpawn > 0) {
                int spawned = 0;

                for (Spawns spawnPoint : monsterSpawn) {
                    if (spawnPoint.getMonster().getLevel() < 200) {
                        if (createMobInterval != 8000) {
                            maxRegularSpawn = 200;
                            createMobInterval = 8000;
                        }
                    }
                }

                for (Spawns spawnPoint : monsterSpawn) {
                    spawnPoint.spawnMonster(this);
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else {
	    final int numShouldSpawn = (GameConstants.isForceRespawn(mapid) ? monsterSpawn.size() : maxRegularSpawn) - spawnedMonstersOnMap.get();
            if (numShouldSpawn > 0) {
                int spawned = 0;

                final List<Spawns> randomSpawn = new ArrayList<Spawns>(monsterSpawn);
                Collections.shuffle(randomSpawn);

                final List<Spawns> realSpawn = new ArrayList<>();

                for (Spawns spawnPoint : randomSpawn) {
                    /*                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                     continue;
                     }*/
                    if (spawnPoint.getMonster().getLevel() < 200) {
                        if (createMobInterval != 8000) {
                            maxRegularSpawn = 200;
                            createMobInterval = 8000;
                        }
                    }
                    if (spawnPoint.shouldSpawn(lastSpawnTime)) { // || GameConstants.isForceRespawn(mapid) || (maxRegularSpawn > monsterSpawn.size() && partyBonusRate > 0)) {
                        realSpawn.add(spawnPoint);
                        spawnPoint.spawnMonster(this);
                        spawned++;
                        if (spawned >= numShouldSpawn) {
                            break;
                        }
                    }
                }

                /*                if (spawned < numShouldSpawn) {
                 killAllMonsters(true);
                 respawn(true);
                 } else {
                 for (Spawns spawnPoint : realSpawn) {
                 spawnPoint.spawnMonster(this);
                 }
                 }*/
            }
        }
    }

    private static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    public String getSnowballPortal() {
        int[] teamss = new int[2];
        for (MapleCharacter chr : characters) {
            if (chr.getTruePosition().y > -80) {
                teamss[0]++;
            } else {
                teamss[1]++;
            }
        }
        if (teamss[0] > teamss[1]) {
            return "st01";
        } else {
            return "st00";
        }
    }

    public boolean isDisconnected(int id) {
        return dced.contains(Integer.valueOf(id));
    }

    public void addDisconnected(int id) {
        dced.add(Integer.valueOf(id));
    }

    public void resetDisconnected() {
        dced.clear();
    }

    public void startSpeedRun() {
        final MapleSquad squad = getSquadByMap();
        if (squad != null) {
            Iterator<MapleCharacter> itr = characters.iterator();
            while (itr.hasNext()) {
                MapleCharacter chr = itr.next();
                if (chr.getName().equals(squad.getLeaderName()) && !chr.isIntern()) {
                    startSpeedRun(chr.getName());
                    return;
                }
            }
        }
    }

    public void startSpeedRun(String leader) {
        speedRunStart = System.currentTimeMillis();
        speedRunLeader = leader;
    }

    public void endSpeedRun() {
        speedRunStart = 0;
        speedRunLeader = "";
    }

    public void getRankAndAdd(String leader, String time, ExpeditionType type, long timz, Collection<String> squad) {
        try {
            long lastTime = SpeedRunner.getSpeedRunData(type) == null ? 0 : SpeedRunner.getSpeedRunData(type).right;
            //if(timz > lastTime && lastTime > 0) {
            //return;
            //}
            //Pair<String, Map<Integer, String>>
            StringBuilder rett = new StringBuilder();
            if (squad != null) {
                for (String chr : squad) {
                    rett.append(chr);
                    rett.append(",");
                }
            }
            String z = rett.toString();
            if (squad != null) {
                z = z.substring(0, z.length() - 1);
            }
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO speedruns(`type`, `leader`, `timestring`, `time`, `members`) VALUES (?,?,?,?,?)");
            ps.setString(1, type.name());
            ps.setString(2, leader);
            ps.setString(3, time);
            ps.setLong(4, timz);
            ps.setString(5, z);
            ps.executeUpdate();
            ps.close();
            con.close();

            if (lastTime == 0) { //great, we just add it
                SpeedRunner.addSpeedRunData(type, SpeedRunner.addSpeedRunData(new StringBuilder(SpeedRunner.getPreamble(type)), new HashMap<Integer, String>(), z, leader, 1, time), timz);
            } else {
                //i wish we had a way to get the rank
                //TODO revamp
                SpeedRunner.removeSpeedRunData(type);
                SpeedRunner.loadSpeedRunData(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getSpeedRunStart() {
        return speedRunStart;
    }

    public final void disconnectAll() {
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (!chr.isGM()) {
                chr.getClient().disconnect(true, false);
                chr.getClient().getSession().close();
            }
        }
    }

    public List<MapleNPC> getAllNPCs() {
        return getAllNPCsThreadsafe();
    }

    public List<MapleNPC> getAllNPCsThreadsafe() {
        ArrayList<MapleNPC> ret = new ArrayList<MapleNPC>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.NPC).values()) {
            ret.add((MapleNPC) mmo);
        }
        return ret;
    }

    public final void resetNPCs() {
        removeNpc(-1);
    }

    public final void resetPQ(int level) {
        resetFully();
        for (MapleMonster mons : getAllMonstersThreadsafe()) {
            mons.changeLevel(level, true);
        }
        resetSpawnLevel(level);
    }

    public final void resetSpawnLevel(int level) {
        for (Spawns spawn : monsterSpawn) {
            if (spawn instanceof SpawnPoint) {
                ((SpawnPoint) spawn).setLevel(level);
            }
        }
    }

    public final void resetFully() {
        resetFully(true);
    }

    public final void resetFully(final boolean respawn) {
        killAllMonsters(true);
        reloadReactors();
        removeDrops();
        removeMists();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        cancelSquadSchedule(true);
        resetPortals();
        setFirstUserEnter(true);
        resetEnvironment();
        setLucidCount(0);
        setLucidUseCount(0);
        setReqTouched(0);
        if (respawn) {
            respawn(true);
        }
    }

    public final void cancelSquadSchedule(boolean interrupt) {
        squadTimer = false;
        checkStates = true;
        if (squadSchedule != null) {
            squadSchedule.cancel(interrupt);
            squadSchedule = null;
        }
    }

    public final void removeDrops() {
        List<MapleMapItem> items = this.getAllItemsThreadsafe();
        for (MapleMapItem i : items) {
            i.expire(this);
        }
    }

    public final void removeMists() {
        List<MapleMist> mists = this.getAllMistsThreadsafe();
        for (MapleMist m : mists) {
            broadcastMessage(CField.removeMist(m));
            removeMapObject(m);
        }
    }

    public final void resetAllSpawnPoint(int mobid, int mobTime) {
        Collection<Spawns> sss = new LinkedList<Spawns>(monsterSpawn);
        resetFully();
        monsterSpawn.clear();
        for (Spawns s : sss) {
            MapleMonster newMons = MapleLifeFactory.getMonster(mobid);
            newMons.setF(s.getF());
            newMons.setFh(s.getFh());
            newMons.setPosition(s.getPosition());
            addMonsterSpawn(newMons, mobTime, (byte) -1, null);
        }
        loadMonsterRate(true);
    }

    public final void resetSpawns() {
        boolean changed = false;
        Iterator<Spawns> sss = monsterSpawn.iterator();
        while (sss.hasNext()) {
            if (sss.next().getCarnivalId() > -1) {
                sss.remove();
                changed = true;
            }
        }
        setSpawns(true);
        if (changed) {
            loadMonsterRate(true);
        }
    }

    public final boolean makeCarnivalSpawn(final int team, final MapleMonster newMons, final int num) {
        MonsterPoint ret = null;
        for (MonsterPoint mp : getNodez().getMonsterPoints()) {
            if (mp.team == team || mp.team == -1) {
                final Point newpos = calcPointBelow(new Point(mp.x, mp.y));
                newpos.y -= 1;
                boolean found = false;
                for (Spawns s : monsterSpawn) {
                    if (s.getCarnivalId() > -1 && (mp.team == -1 || s.getCarnivalTeam() == mp.team) && s.getPosition().x == newpos.x && s.getPosition().y == newpos.y) {
                        found = true;
                        break; //this point has already been used.
                    }
                }
                if (!found) {
                    ret = mp; //this point is safe for use.
                    break;
                }
            }
        }
        if (ret != null) {
            newMons.setCy(ret.cy);
            newMons.setF(0); //always.
            newMons.setFh(ret.fh);
            newMons.setRx0(ret.x + 50);
            newMons.setRx1(ret.x - 50); //does this matter
            newMons.setPosition(new Point(ret.x, ret.y));
            newMons.setHide(false);
            final SpawnPoint sp = addMonsterSpawn(newMons, 1, (byte) team, null);
            sp.setCarnival(num);
        }
        return ret != null;
    }

    public final boolean makeCarnivalReactor(final int team, final int num) {
        final MapleReactor old = getReactorByName(team + "" + num);
        if (old != null && old.getState() < 5) { //already exists
            return false;
        }
        Point guardz = null;
        final List<MapleReactor> react = getAllReactorsThreadsafe();
        for (Pair<Point, Integer> guard : getNodez().getGuardians()) {
            if (guard.right == team || guard.right == -1) {
                boolean found = false;
                for (MapleReactor r : react) {
                    if (r.getTruePosition().x == guard.left.x && r.getTruePosition().y == guard.left.y && r.getState() < 5) {
                        found = true;
                        break; //already used
                    }
                }
                if (!found) {
                    guardz = guard.left; //this point is safe for use.
                    break;
                }
            }
        }
        if (guardz != null) {
            final MapleReactor my = new MapleReactor(MapleReactorFactory.getReactor(9980000 + team), 9980000 + team);
            my.setState((byte) 1);
            my.setName(team + "" + num); //lol
            //with num. -> guardians in factory
            spawnReactorOnGroundBelow(my, guardz);
            final MCSkill skil = MapleCarnivalFactory.getInstance().getGuardian(num);
            for (MapleMonster mons : getAllMonstersThreadsafe()) {
                if (mons.getCarnivalTeam() == team) {
                    skil.getSkill().applyEffect(null, mons, false, false);
                }
            }
        }
        return guardz != null;
    }

    public final void blockAllPortal() {
        for (MaplePortal p : portals.values()) {
            p.setPortalState(false);
        }
    }

    public boolean getAndSwitchTeam() {
        return getCharactersSize() % 2 != 0;
    }

    public void setSquad(MapleSquadType s) {
        this.squad = s;

    }

    public int getChannel() {
        return channel;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int ciit) {
        this.consumeItemCoolTime = ciit;
    }

    public void setPermanentWeather(int pw) {
        this.permanentWeather = pw;
    }

    public int getPermanentWeather() {
        return permanentWeather;
    }

    public void checkStates(final String chr) {
        if (!checkStates) {
            return;
        }
        final MapleSquad sqd = getSquadByMap();
        final EventManager em = getEMByMap();
        final int size = getCharactersSize();
        if (sqd != null && sqd.getStatus() == 2) {
            sqd.removeMember(chr);
            if (em != null) {
                if (sqd.getLeaderName().equalsIgnoreCase(chr)) {
                    em.setProperty("leader", "false");
                }
                if (chr.equals("") || size == 0) {
                    em.setProperty("state", "0");
                    em.setProperty("leader", "true");
                    cancelSquadSchedule(!chr.equals(""));
                    sqd.clear();
                    sqd.copy();
                }
            }
        }
        if (em != null && em.getProperty("state") != null && (sqd == null || sqd.getStatus() == 2) && size == 0) {
            em.setProperty("state", "0");
            if (em.getProperty("leader") != null) {
                em.setProperty("leader", "true");
            }
        }
        if (speedRunStart > 0 && size == 0) {
            endSpeedRun();
        }
        //if (squad != null) {
        //    final MapleSquad sqdd = ChannelServer.getInstance(channel).getMapleSquad(squad);
        //    if (sqdd != null && chr != null && chr.length() > 0 && sqdd.getAllNextPlayer().contains(chr)) {
        //	sqdd.getAllNextPlayer().remove(chr);
        //	broadcastMessage(CWvsContext.serverNotice(5, "The queued player " + chr + " has left the map."));
        //    }
        //}
    }

    public void setCheckStates(boolean b) {
        this.checkStates = b;
    }

    public void setNodes(final MapleNodes mn) {
        this.setNodez(mn);
    }

    public final List<MaplePlatform> getPlatforms() {
        return getNodez().getPlatforms();
    }

    public Collection<MapleNodeInfo> getNodes() {
        return getNodez().getNodes();
    }

    public MapleNodeInfo getNode(final int index) {
        return getNodez().getNode(index);
    }

    public boolean isLastNode(final int index) {
        return getNodez().isLastNode(index);
    }

    public final List<Rectangle> getAreas() {
        return getNodez().getAreas();
    }

    public final Rectangle getArea(final int index) {
        return getNodez().getArea(index);
    }

    public final void changeEnvironment(final String ms, final int type) {
        broadcastMessage(CField.environmentChange(ms, type));
    }

    public final int getNumPlayersInArea(final int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public final int getNumPlayersInRect(final Rectangle rect) {
        int ret = 0;

        final Iterator<MapleCharacter> ltr = characters.iterator();
        while (ltr.hasNext()) {
            if (rect.contains(ltr.next().getTruePosition())) {
                ret++;
            }
        }
        return ret;
    }

    public final int getNumPlayersItemsInArea(final int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public final int getNumPlayersItemsInRect(final Rectangle rect) {
        int ret = getNumPlayersInRect(rect);

        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
            if (rect.contains(mmo.getTruePosition())) {
                ret++;
            }
        }
        return ret;
    }

    public void broadcastGMMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet);
    }

    private void broadcastGMMessage(MapleCharacter source, byte[] packet) {
        if (source == null) {
            final Iterator<MapleCharacter> itr = characters.iterator();
            while (itr.hasNext()) {
                MapleCharacter chr = itr.next();

                if (source == null) {
                    if (chr.isStaff()) {
                        chr.getClient().getSession().writeAndFlush(packet);
                    }
                } else {
                    if (chr != source && (chr.getGMLevel() >= source.getGMLevel())) {
                        chr.getClient().getSession().writeAndFlush(packet);
                    }
                }
            }
        }
    }

    public final List<Pair<Integer, Integer>> getMobsToSpawn() {
        return getNodez().getMobsToSpawn();
    }

    public final List<Integer> getSkillIds() {
        return getNodez().getSkillIds();
    }

    public final boolean canSpawn(long now) {
        return lastSpawnTime > 0 && lastSpawnTime + createMobInterval < now;
    }

    public final boolean canHurt(long now) {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < now) {
            lastHurtTime = now;
            return true;
        }
        return false;
    }

    public final void resetShammos(final MapleClient c) {
        killAllMonsters(true);
        broadcastMessage(CWvsContext.serverNotice(5, "", "A player has moved too far from Shammos. Shammos is going back to the start."));
        EtcTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (c.getPlayer() != null) {
                    c.getPlayer().changeMap(MapleMap.this, getPortal(0));
                    if (getCharactersThreadsafe().size() > 1) {
                        MapScriptMethods.startScript_FirstUser(c, "shammos_Fenter");
                    }
                }
            }
        }, 500); //avoid dl
    }

    public int getInstanceId() {
        return instanceid;
    }

    public void setInstanceId(int ii) {
        this.instanceid = ii;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int ii) {
        this.partyBonusRate = ii;
    }

    public short getTop() {
        return top;
    }

    public short getBottom() {
        return bottom;
    }

    public short getLeft() {
        return left;
    }

    public short getRight() {
        return right;
    }

    public void setTop(int ii) {
        this.top = (short) ii;
    }

    public void setBottom(int ii) {
        this.bottom = (short) ii;
    }

    public void setLeft(int ii) {
        this.left = (short) ii;
    }

    public void setRight(int ii) {
        this.right = (short) ii;
    }

    public List<Pair<Point, Integer>> getGuardians() {
        return getNodez().getGuardians();
    }

    public DirectionInfo getDirectionInfo(int i) {
        return getNodez().getDirection(i);
    }

    public Collection<MapleCharacter> getNearestPvpChar(Point attacker, double maxRange, double maxHeight, boolean isLeft, Collection<MapleCharacter> chr) {
        Collection<MapleCharacter> character = new LinkedList<MapleCharacter>();
        for (MapleCharacter a : characters) {
            if (chr.contains(a.getClient().getPlayer())) {
                Point attackedPlayer = a.getPosition();
                MaplePortal Port = a.getMap().findClosestSpawnpoint(a.getPosition());
                Point nearestPort = Port.getPosition();
                double safeDis = attackedPlayer.distance(nearestPort);
                double distanceX = attacker.distance(attackedPlayer.getX(), attackedPlayer.getY());
                // System.out.print ("distanceX"+ distanceX);
                // System.out.print ("maxRange"+ maxRange);
                // System.out.print ("maxHeight"+ maxRange);

                if (isLeft) {
                    if (attacker.x < attackedPlayer.x && distanceX < maxRange && distanceX > 1
                            && attackedPlayer.y >= attacker.y - maxHeight && attackedPlayer.y <= attacker.y + maxHeight) {
                        character.add(a);
                    }
                } else {
                    if (attacker.x > attackedPlayer.x && distanceX < maxRange && distanceX > 1
                            && attackedPlayer.y >= attacker.y - maxHeight && attackedPlayer.y <= attacker.y + maxHeight) {
                        character.add(a);
                    }
                }
            }
        }
        return character;
    }

    public void startCatch() {
        if (catchstart == null) {
            broadcastMessage(CField.getClock(180));
            catchstart = MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    broadcastMessage(CWvsContext.serverNotice(1, "", "[술래잡기 알림]\r\n제한시간 2분이 지나 양이 승리하였습니다!\r\n모든 분들은 게임 보상맵으로 이동됩니다."));
                    for (MapleCharacter chr : getCharacters()) {
                        chr.getStat().setHp(chr.getStat().getMaxHp(), chr);
                        chr.updateSingleStat(MapleStat.HP, chr.getStat().getMaxHp());
                        if (chr.isCatching) {
                            chr.changeMap(chr.getClient().getChannelServer().getMapFactory().getMap(910040005), chr.getClient().getChannelServer().getMapFactory().getMap(910040005).getPortalSP().get(0));
                            chr.isWolfShipWin = false;
                        } else {
                            chr.changeMap(chr.getClient().getChannelServer().getMapFactory().getMap(910040004), chr.getClient().getChannelServer().getMapFactory().getMap(910040004).getPortalSP().get(0));
                            chr.isWolfShipWin = true;
                        }
                    }
                    stopCatch();
                }
            }, 180000);
        }
    }

    public void stopCatch() {
        if (catchstart != null) {
            catchstart.cancel(true);
            catchstart = null;
        }
    }

    public List<MapleMonster> getAllButterFly() {
        ArrayList<MapleMonster> ret = new ArrayList<>();
        for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
            MapleMonster monster = ((MapleMonster) mmo);
            if ((monster.getId() == 8880175 || monster.getId() == 8880178 || monster.getId() == 8880179) && (mapid == 450004250 || mapid == 450004550)) {
                ret.add(monster);
            } else if ((monster.getId() == 8880165 || monster.getId() == 8880168 || monster.getId() == 8880169) && (mapid == 450004150 || mapid == 450004450)) {
                ret.add(monster);
            }
        }
        return ret;
    }

    public final void killMonsters(List<MapleMonster> mon) {
        for (final MapleMonster monster : mon) {
            monster.setHp(0);
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
            removeMapObject(monster);
            monster.killed();
            spawnedMonstersOnMap.decrementAndGet();
        }
    }

    public final void killButterflys(List<MapleMonster> mon) {
        for (final MapleMonster monster : mon) {
            broadcastMessage(MobPacket.BossBlodyQueen.killMonster(monster.getObjectId()));
            removeMapObject(monster);
            monster.killed();
            spawnedMonstersOnMap.decrementAndGet();
        }
    }

    public long getMapTimer() {
        return timer;
    }

    public void setMapTimer(long timer) {
        this.timer = timer;
    }

    public final void spawnSoul(final MapleMapObject dropper, final MapleCharacter chr, final Item item, Point pos, Item weapon) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, chr, (byte) 0, true);
        broadcastMessage(CField.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0, false));
        broadcastMessage(CField.removeItemFromMap(drop.getObjectId(), 2, chr.getId(), 0));

        chr.setSoulMP((Equip) weapon);
    }

    public int getBurning() {
        return burning;
    }

    public void setBurning(int burning) {
        this.burning = burning;
    }

    public long getBurningIncreasetime() {
        return burningIncreasetime;
    }

    public void setBurningIncreasetime(long burningtime) {
        this.burningIncreasetime = burningtime;
    }

    public int getBurningDecreasetime() {
        return burningDecreasetime;
    }

    public void setBurningDecreasetime(int burningtime) {
        this.burningDecreasetime = burningtime;
    }

    public List<Rectangle> makeRandomSplitAreas(Point position, Point lt, Point rb, int i, boolean b) {
        List<Rectangle> splitArea = new ArrayList<>();
        for (byte count = 0; count < i; count++) {
            splitArea.add(new Rectangle());
        }
        return splitArea;
    }

    public MapleNodes getNodez() {
        return nodes;
    }

    public void setNodez(MapleNodes nodes) {
        this.nodes = nodes;
    }

    public void updateEnvironment(List<String> updateLists) {
        for (Environment ev : getNodez().getEnvironments()) {
            if (updateLists.contains(ev.getName())) {
                ev.setShow(true);
            } else {
                ev.setShow(false);
            }
        }
        broadcastMessage(CField.getUpdateEnvironment(getNodez().getEnvironments()));
    }

    public void resetEnvironment() {
        for (Environment ev : getNodez().getEnvironments()) {
            ev.setShow(false);
        }
        broadcastMessage(CField.getUpdateEnvironment(getNodez().getEnvironments()));
    }

    public List<MapleMagicWreck> getWrecks() {
        return wrecks;
    }

    public void setWrecks(List<MapleMagicWreck> wrecks) {
        this.wrecks = wrecks;
    }

    public boolean isFirstUserEnter() {
        return firstUserEnter;
    }

    public void setFirstUserEnter(boolean firstUserEnter) {
        this.firstUserEnter = firstUserEnter;
    }

    public int getRuneCurse() {
        return runeCurse;
    }

    public void setRuneCurse(int runeCurse) {
        this.runeCurse = runeCurse;
    }

    public int getRuneCurseDecrease() {
        switch (runeCurse) {
            case 1:
                return 50;
            case 2:
                return 65;
            case 3:
                return 80;
            case 4:
                return 100;
            default:
                return 0;
        }
    }

    public final int getBarrier() {
        return barrier;
    }

    public final void setBarrier(final int Barrier) {
        this.barrier = Barrier;
    } //barrierArc

    public final int getBarrierArc() {
        return barrierArc;
    }

    public final void setBarrierArc(final int BarrierArc) {
        this.barrierArc = BarrierArc;
    }

    public boolean isBingoGame() {
        return bingoGame;
    }

    public void setBingoGame(boolean bingoGame) {
        this.bingoGame = bingoGame;
    }

    public boolean isEliteField() {
        return isEliteField;
    }

    public void setEliteField(boolean isEliteField) {
        this.isEliteField = isEliteField;
    }

    public MapleRune getRune() {
        return rune;
    }

    public void setRune(MapleRune rune) {
        this.rune = rune;
    }

    public MapleMonster makePyramidMonster(final MapleMonster monster, long hp, int level, int exp) {
        final MapleMonster mob = MapleLifeFactory.getMonster(monster.getId());
        final OverrideMonsterStats ostats = new OverrideMonsterStats();
        ostats.setOHp(hp);
        ostats.setOMp(mob.getMobMaxMp());
        ostats.setOExp(exp);
        mob.setOverrideStats(ostats);
        mob.setPosition(monster.getTruePosition());
        mob.setFh(monster.getFh());
        mob.getStats().setLevel((short) level);
        return mob;
    }

    public void addmonsterDefense(Map<Integer, List<Integer>> info) {
        monsterDefense.putAll(info);
    }

    public Map<Integer, List<Integer>> getmonsterDefense() {
        return monsterDefense;
    }

    public ScheduledFuture<?> getEliteBossSchedule() {
        return eliteBossSchedule;
    }

    public void setEliteBossSchedule(ScheduledFuture<?> eliteBossSchedule) {
        this.eliteBossSchedule = eliteBossSchedule;
    }

    public int getStigmaDeath() {
        return stigmaDeath;
    }

    public void setStigmaDeath(int stigmaDeath) {
        this.stigmaDeath = stigmaDeath;
    }

    public int getCandles() {
        return candles;
    }

    public void setCandles(int candles) {
        this.candles = candles;
    }

    public int getLightCandles() {
        return lightCandles;
    }

    public void setLightCandles(int lightCandles) {
        this.lightCandles = lightCandles;
    }

    public int getReqTouched() {
        return reqTouched;
    }

    public void setReqTouched(int reqTouched) {
        this.reqTouched = reqTouched;
    }

    public long getSandGlassTime() {
        return sandGlassTime;
    }

    public void setSandGlassTime(long sandGlassTime) {
        this.sandGlassTime = sandGlassTime;
    }

    public int getLucidCount() {
        return lucidCount;
    }

    public void setLucidCount(int lucidCount) {
        this.lucidCount = lucidCount;
    }

    public int getLucidUseCount() {
        return lucidUseCount;
    }

    public void setLucidUseCount(int lucidUseCount) {
        this.lucidUseCount = lucidUseCount;
    }

    public long getLastButterFlyTime() {
        return lastButterflyTime;
    }

    public void setLastButterFlyTime(long ltime) {
        this.lastButterflyTime = ltime;
    }

    public void CreateObstacle(MapleMonster monster, List<Obstacle> obs) {
        broadcastMessage(MobPacket.createObstacle(monster, obs, (byte) 0));
    }

    public void killMonsterType(MapleMonster mob, int type) {
        if (mob != null && mob.isAlive()) {
            removeMapObject((MapleMapObject) mob);
            mob.killed();
            this.spawnedMonstersOnMap.decrementAndGet();
            broadcastMessage(MobPacket.killMonster(mob.getObjectId(), type));
            broadcastMessage(MobPacket.stopControllingMonster(mob.getObjectId()));
        }
    }

}
