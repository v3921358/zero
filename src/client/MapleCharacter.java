package client;

import client.MapleTrait.MapleTraitType;
import client.custom.inventory.CustomItem;
import client.damage.CalcDamage;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.ItemLoader;
import client.inventory.MapleAndroid;
import client.inventory.MapleImp;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import client.inventory.MapleRing;
import client.management.ByNameValue;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.channel.ChannelServer;
import handling.channel.handler.AttackInfo;
import handling.channel.handler.MatrixHandler;
import handling.channel.handler.PlayerHandler;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.PlayerBuffValueHolder;
import handling.world.World;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import server.MapleDonationSkill;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.EventInstanceManager;
import scripting.NPCScriptManager;
import server.AdelProjectile;
import server.CashShop;
import server.InnerAbillity;
import server.MapleCarnivalChallenge;
import server.MapleCarnivalParty;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelDiseaseAction;
import server.MapleStorage;
import server.MapleTrade;
import server.Randomizer;
import server.Timer;
import server.Timer.BuffTimer;
import server.field.skill.MapleFieldAttackObj;
import server.games.BattleReverse;
import server.games.BingoGame;
import server.games.DetectiveGame;
import server.games.MonsterPyramid;
import server.games.MultiYutGame;
import server.games.OXQuizGame;
import server.games.OneCardGame;
import server.life.*;
import server.maps.AnimatedMapleMapObject;
import server.maps.FieldLimitType;
import server.maps.ForceAtom;
import server.maps.MapleAtom;
import server.maps.MapleDoor;
import server.maps.MapleDragon;
import server.maps.MapleExtractor;
import server.maps.MapleFoothold;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.MechDoor;
import server.maps.SavedLocationType;
import server.maps.SummonMovementType;
import server.marriage.MarriageMiniBox;
import server.movement.LifeMovementFragment;
import server.polofritto.BountyHunting;
import server.polofritto.DefenseTowerWave;
import server.polofritto.FrittoDancing;
import server.polofritto.FrittoEagle;
import server.polofritto.FrittoEgg;
import server.quest.MapleQuest;
import server.quest.party.MapleNettPyramid;
import server.shops.IMaplePlayerShop;
import server.shops.MapleShop;
import tools.AttackPair;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CSPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuddylistPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.MobPacket;
import tools.packet.PetPacket;
import tools.packet.PlayerShopPacket;
import tools.packet.SLFCGPacket;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable {

    private static final long serialVersionUID = 845748950829L;
    private int HowlingGaleCount = 0, YoyoCount = 0, WildGrenadierCount = 0, VerseOfRelicsCount = 0;
    private int BHGCCount = 0, createDate = 0, huntingdecree = 0;

    public int RepeatinCartrige = 0;
    public long startRepeatinCartrigeTime = 0;
    public List<Pair<Integer, Integer>> object = new ArrayList<>();

    // 종우기 구역
    private int RandomPortal = 0;
    private int fwolfattackcount = 0;
    private int BlockCount = 0;
    private int BlockCoin = 0;
    private int MesoChairCount = 0;
    private int tempmeso = 0;
    public int PhotonRay_plus = 0;
    private int duskGauge = 0;
    private long fwolfdamage = 0L, LastMovement = 0L, PlatformerStageEnter = 0L;
    private boolean hasfwolfportal = false;
    private boolean isfwolfkiller = false;
    private boolean isWatchingWeb = false;
    private boolean oneMoreChance = false;
    private boolean isDuskBlind = false;
    private String chairtext, auth;
    private BingoGame BingoInstance = null;
    private OXQuizGame OXInstance = null;
    private DetectiveGame DetectiveGameInstance = null;
    private BattleReverse BattleReverseInstance = null;
    private OneCardGame oneCardInstance = null;
    private MultiYutGame multiYutInstance = null;
    private MonsterPyramid monsterPyramidInstance = null;
    private Point Resolution = new Point(0, 0);
    private List<PlatformerRecord> PfRecords = new ArrayList<>();
    public long lastSaveTime = 0, lastReportTime = 0, lastMacroTime = System.currentTimeMillis();
    //종우기 구역 끝

    public long lastConcentrationTime, lastVerseOfRelicsTime, lastTimeleapTime, lastDemonicFrenzyTime, lastChainArtsFuryTime, lastFireArrowTime, lastThunderTime, lastChairPointTime = 0, lastVamTime = 0, lastAltergoTime = 0, lastButterflyTime = 0, lastMjollnirTime;
    private long lastFishingTime;
    private String name, chalktext, BlessOfFairy_Origin, BlessOfEmpress_Origin, teleportname;
    private long exp, meso, lastCombo, lastfametime, keydown_skill, nextConsume, pqStartTime, lastDragonBloodTime,
            lastBerserkTime, lastRecoveryTime, lastSummonTime, mapChangeTime, lastFairyTime, lastExceedTime = System.currentTimeMillis(),
            lastHPTime, lastMPTime, lastDOTTime, monsterComboTime = 0, lastBulletUsedTime = 0, lastCreationTime = 0;
    private byte deathcount, gmLevel, gender, secondgender, initialSpawnPoint, skinColor, secondSkinColor, guildrank = 5, allianceRank = 5, cardStack, wolfscore, sheepscore, pandoraBoxFever,
            world, fairyExp, numClones, subcategory;
    public byte RapidTimeDetect = 0, RapidTimeStrength = 0, acaneAim = 0;
    private short level, mulung_energy, combo, force, availableCP, fatigue, totalCP, hpApUsed, job, remainingAp, scrolledPosition, xenonSurplus = 0, kaiserCombo, monsterCombo = 0, forcingItem = 0, relicGauge = 0, hoyoungForce = 0, tidalForce = 0;
    private int betaclothes = 0, zeroCubePosition = 0, moonGauge = 0, overloadCount = 0, exceed = 0;
    private int accountid, id;
    private transient CalcDamage calcDamage;
    public int batt = 0, clearWeb = 0, forceBlood = 0, fightJazzSkill = 0, nextBlessSkill = 0, empiricalStack = 0, adelResonance = 0, ThrowBlasting = 0, FullMakerSize = 0, FullMakerBoxCount = 0;
    public MapleMonster empiricalKnowledge = null;
    private long dojoStartTime = 0, dojoStopTime = 0, dojoCoolTime = 0, damageMeter;
    private boolean deadEffect = false, noneDestroy = false;
    private int hair;
    private int basecolor = -1;
    private int addcolor;
    private int baseprob;
    private int secondbasecolor = -1;
    private int secondaddcolor;
    private int secondbaseprob;
    private int secondhair;
    private int face;
    private int secondface;
    private int demonMarking;
    private int mapid;
    private int fame;
    private int pvpExp;
    private int pvpPoints;
    private int totalWins;
    private int totalLosses;
    private int guildid = 0;
    private int fallcounter;
    private int maplepoints;
    private int nxcredit;
    private int acash;
    private int chair;
    private int itemEffect;
    private int points;
    private int vpoints;
    private int itcafetime;
    private int rank = 1;
    private int rankMove = 0;
    private int jobRank = 1;
    private int jobRankMove = 0;
    private int marriageId;
    private int marriageItemId;
    private int dotHP;
    private int honourExp;
    private int honorLevel;
    public int questmobkillcount = 0;
    public short combo_k;

    //SkillData
    private int ignitionstack = 0;
    private int arcaneAim = 0;
    private int listonation = 0;
    private int ElementalCharge = 0;
    private int ElementalCharge_ID = 0;
    private int reinCarnation = 0, transformCooldown = 0;
    private byte poisonStack = 0, unityofPower = 0, concentration = 0, mortalBlow = 0, death = 0, royalStack = 0;
    private int beholderSkill1 = 0, beholderSkill2 = 0, barrier = 0, energyBurst = 0, trinity = 0, blitzShield = 0;
    private byte infinity = 1, holyPountin = 0, blessingAnsanble = 0, quiverType = 0, flip = 0, holyMagicShell = 0, antiMagicShell = 0, blessofDarkness = 0;
    private int currentrep, dice = 0, holyPountinOid = 0, blackMagicAlter = 0, judgementType = 0;
    private int[] RestArrow = new int[3], deathCounts = new int[5];
    private List<Integer> weaponChanges = new ArrayList<>(), posionNovas = new ArrayList<>(), exceptionList = new ArrayList<>();
    private int mparkexp = 0, pickPocket = 0, markofPhantom = 0, ultimateDriverCount = 0, markOfPhantomOid = 0, rhoAias = 0, perfusion = 0, spiritGuard = 0;
    private MapleFieldAttackObj fao = null;
    public long startFullMakerTime = 0, lastInstallMahaTime = 0, lastRecoverScrollGauge = 0, cooldownforceBlood = 0, cooldownEllision = 0, lastDistotionTime = 0, lastNemeaAttackTime = 0, lastGerionAttackTime = 0, lastBonusAttckTime = 0, lastDanceTime = 0, lastElementalGhostTime = 0, lastDrainAuraTime = 0, lastShardTime = 0, lastPinPointRocketTime = 0, lastDeathAttackTime = 0, lastChargeEnergyTime = 0;
    public int unstableMemorize = 0, ignoreDraco = 0, lastHowlingGaleObjectId = -1, scrollGauge = 0, ancientGauge = 0, shadowBite = 0, curseBound = 0, editionalTransitionAttack = 0, lastCardinalForce = 0, cardinalMark = 0, flameDischargeRegen = 0, striker3rdStack = 0, mascotFamilier = 0, shadowerDebuff = 0, shadowerDebuffOid = 0, maelstrom = 0, lastPoseType = 0, energy = 0, blessMark = 0, blessMarkSkill = 0, fightJazz = 0, guidedBullet = 0, graveObjectId = 0, Mjollnir = 0, VarietyFinale = 0, VarietyFinaleCount = 0;
    public boolean useChun = false, useJi = false, useIn = false, wingDagger = false, canUseMortalWingBeat = false;

    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);
    private Pair<Integer, Integer> recipe = new Pair<>(0, 0);
    private ScheduledFuture<?> PlatformerTimer, MesoChairTimer;

    private int totalrep;
    private int coconutteam;
    private int followid;
    private int battleshipHP;
    private int challenge;
    private int guildContribution = 0;
    private int storageNpc = 0, lastBossId = 0;
    private int TouchedRune;
    private boolean luminusMorph = false, useBuffFreezer = false, extremeMode = false;
    private Point flameHeiz = null;
    private int lumimorphuse = 5000;
    private long lastTouchedRuneTime = 0;
    private Point old;
    private long DotDamage = 0;
    public long lastHowlingGaleTime, lastYoyoTime, lastWildGrenadierTime, lastRandomAttackTime, lastVarietyFinaleTime;
    private int[] rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10];
    private int[] wishlist = new int[12];
    private transient AtomicInteger inst, insd;
    private transient List<LifeMovementFragment> lastres;
    private Map<String, String> keyValues = new HashMap<>();
    private List<Integer> lastmonthfameids, lastmonthbattleids, extendedSlots, cashwishlist = new ArrayList<Integer>();
    private List<MapleDoor> doors;
    private List<MechDoor> mechDoors;
    public MaplePet[] pets = new MaplePet[3];
    private List<Item> rebuy;
    private List<InnerSkillValueHolder> innerSkills;
    public List<InnerSkillValueHolder> innerCirculator = new ArrayList<>();
    private MapleImp[] imps = new MapleImp[3];
    private List<Equip> symbol;
    private List<Pair<Integer, Boolean>> stolenSkills = new ArrayList<>();
    private transient List<MapleMonster> controlled;
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient MapleAndroid android;
    private transient MapleHaku haku;
    private Map<MapleQuest, MapleQuestStatus> quests;
    private Map<Integer, String> questinfo = new ConcurrentHashMap<Integer, String>();
    private Map<Skill, SkillEntry> skills;
    private List<Triple<Skill, SkillEntry, Integer>> linkskills;
    private transient Map<Integer, Integer> customValue = null;
    private transient Map<Integer, Long> customTime = null;
    private List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effects;
    private List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> removeEffects;
    private transient List<MapleSummon> summons;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();
    ;
    private transient Map<MapleBuffStat, Pair<MobSkill, Integer>> diseases;
    private CashShop cs;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private transient MapleCarnivalParty carnivalParty;
    private BuddyList buddylist;
    private UnionList unions;
    private MapleClient client;
    private int bufftest = 0;
    private transient MapleParty party;
    private PlayerStats stats;
    private MapleCharacterCards characterCard;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleDragon dragon;
    private transient MapleExtractor extractor;
    private List<Core> cores = new ArrayList<Core>();
    private List<VMatrix> matrixs = new ArrayList<VMatrix>();
    private List<MapleMannequin> hairRoom;
    private List<MapleMannequin> faceRoom;
    private List<MapleMannequin> skinRoom;
    private MapleStorage storage;
    private transient MapleTrade trade;
    private MapleMount mount;
    private MapleMessenger messenger;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, followinitiator, followon, smega;
    public boolean petLoot, shield = false;
    private MapleGuildCharacter mgc;
    private transient EventInstanceManager eventInstance;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private EnumMap<MapleTraitType, MapleTrait> traits;
    private MapleKeyLayout keylayout = new MapleKeyLayout();
    public long lastBHGCGiveTime = System.currentTimeMillis(), lastDrainTime = System.currentTimeMillis(), lastFreezeTime = System.currentTimeMillis(), lastHealTime = System.currentTimeMillis(), lastAngelTime = System.currentTimeMillis(), lastInfinityTime = System.currentTimeMillis(), lastCygnusTime = System.currentTimeMillis(), lastSpiritGateTime = System.currentTimeMillis(), lastDriveTime = System.currentTimeMillis(), lastRoyalKnightsTime = System.currentTimeMillis();
    private transient ScheduledFuture<?> itcafetimer, diabolicRecoveryTask, LastTouchedRune = null;
    public transient static ScheduledFuture<?> XenonSupplyTask = null;
    public boolean vh;
    List<Integer> allpetbufflist = new ArrayList<>(3);
    private transient List<Integer> pendingExpiration = null;
    private transient Map<Skill, SkillEntry> pendingSkills = null;
    private transient Map<Integer, Integer> linkMobs;
    private boolean changed_wishlist, changed_trocklocations, changed_regrocklocations, changed_hyperrocklocations, changed_skillmacros,
            changed_savedlocations, changed_questinfo, changed_skills, changed_reports, changed_extendedSlots;
    private boolean innerskill_changed = true, fishing = false;

    private int premiumbuff = 0;
    private long premiumPeriod = 0;
    private String premium = "";
    /*Start of Custom Feature*/
 /*All custom shit declare here*/
    private int reborns, apstorage;
    /*End of Custom Feature*/
    public boolean pvp = false, isTrade = false, isCatching = false, isCatched = false, isWolfShipWin = false;
    public boolean isVoting = false,
            isDead = false,
            isMapiaVote = false,
            isDrVote = false,
            isPoliceVote = false;
    private List<Item> auctionitems;
    public String mapiajob = "";
    public short blackRebirthPos = 0;
    public int voteamount = 0, getmapiavote = 0, getpolicevote = 0, getdrvote = 0, mbating = 0, CrystalCharge = 0, returnSc = 0, peaceMaker = 0, mecahCarriercount = 9, erdacount = 0, orgelcount = 20;
    public boolean orgelTime = false;
    public long blackRebirth = 0;
    public Equip choicepotential = null, returnscroll = null, blackRebirthScroll = null;
    public Item memorialcube = null;
    private int slowAttackCount = 0;
    public boolean isdressup = false, useBlackJack = false;
    private int LinkMobCount = 0, lastCharGuildId = 0;
    private int weddingGiftGive;
    private int arcSpell = 0;
    private List<Integer> arcSpellSkills = new ArrayList<>();
    private Point specialChairPoint = new Point();
    private boolean useTruthDoor = false;
    public int nettDifficult = 0;
    private transient MapleNettPyramid NettPyramid = null;
    private DefenseTowerWave defenseTowerWave = null;
    private BountyHunting bountyHunting = null;
    private FrittoEagle frittoEagle = null;
    private FrittoEgg frittoEgg = null;
    private FrittoDancing frittoDancing = null;
    private MarriageMiniBox mg = null;
    private int novilitybarrier;

    private transient ScheduledFuture<?> brune = null;
    private int 보물룬아이템카운트;

    public long lastEvanMagicWtime = System.currentTimeMillis();
    public long bpoint, basebpoint;
    public int bosstier;
    public int tier;

    //카인 작업
    public int MalicePoint = 0, DragonPangStack = 0, KainLinkCount = 0, KainLinkBattackCount = 0, KainLinkKillCount, AgonyCount = 0, AgonyKillCount = 0, AgonyBattackCount = 0;
    public int KainscatteringShot = 0, KainshaftBreak = 0, KainfallingDust = 0, KainsneakySnipingPre = 0;
    public long lastKainscatteringShot = System.currentTimeMillis();
    public long lastKainshaftBreak = System.currentTimeMillis();
    public long lastKainfallingDust = System.currentTimeMillis();
    public long lastKainsneakySnipingPre = System.currentTimeMillis();
    public long lastKainremainInsence = System.currentTimeMillis();
    public long lastMaliceChargeTime = 0, lastThanatosDescentattack = System.currentTimeMillis();
    public List<Triple<MapleMonster, Integer, Long>> kainblessmobs = new ArrayList<>(); //mob , stack, timesec

    //오브네 구간
    private List<ByNameValue> NameValue = new ArrayList<>();
    private long levelpoint = 0;
    private long PoloMobexp = 0;
    private long PoloMobhp = 0;
    private int PoloMobLevel = 0;
    public MapleMonster polom = null;
    private long lastSpawnBlindMobtime = System.currentTimeMillis();

    public long DamageMeter = 0;
    private int PeaceMaker = 0;
    public int Energy_Bust_State = 0;
    public int Energy_Bust_Num = 0;
    public long lastSungiAttackTime = 0;

    public int SerenStunGauge = 0;

    private MapleCharacter(final boolean ChannelServer) {
        setStance(0);
        setPosition(new Point(0, 0));
        lastSaveTime = System.currentTimeMillis();

        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        quests = new ConcurrentHashMap<MapleQuest, MapleQuestStatus>(); // Stupid erev quest.
        skills = new ConcurrentHashMap<Skill, SkillEntry>(); //Stupid UAs.
        linkskills = new ArrayList<>();
        stats = new PlayerStats();
        innerSkills = new LinkedList<>();
        characterCard = new MapleCharacterCards();
        setHairRoom(new ArrayList<>());
        setFaceRoom(new ArrayList<>());
        skinRoom = new ArrayList<>();
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
        traits = new EnumMap<MapleTraitType, MapleTrait>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            traits.put(t, new MapleTrait(t));
        }
        if (ChannelServer) {
            changed_reports = false;
            changed_skills = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_extendedSlots = false;
            changed_questinfo = false;
            canTalk = true;
            scrolledPosition = 0;
            lastCombo = 0;
            mulung_energy = 0;
            combo = 0;
            force = 0;
            keydown_skill = 0;
            nextConsume = 0;
            pqStartTime = 0;
            fairyExp = 0;
            cardStack = 0;
            mapChangeTime = 0;
            lastRecoveryTime = 0;
            lastDragonBloodTime = 0;
            lastBerserkTime = 0;
            lastFairyTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            old = new Point(0, 0);
            coconutteam = 0;
            followid = 0;
            battleshipHP = 0;
            marriageItemId = 0;
            fallcounter = 0;
            challenge = 0;
            dotHP = 0;
            itcafetime = 0;
            lastSummonTime = 0;
            invincible = false;
            followinitiator = false;
            followon = false;
            rebuy = new ArrayList<Item>();
            symbol = new ArrayList<Equip>();
            setAuctionitems(new ArrayList<Item>());
            linkMobs = new HashMap<Integer, Integer>();
            teleportname = "";
            smega = true;
            wishlist = new int[12];
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            extendedSlots = new ArrayList<Integer>();
            effects = new CopyOnWriteArrayList<>();
            removeEffects = new CopyOnWriteArrayList<>();
            diseases = new ConcurrentHashMap<MapleBuffStat, Pair<MobSkill, Integer>>();
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Duey, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            doors = new ArrayList<MapleDoor>();
            mechDoors = new ArrayList<MechDoor>();
            controlled = new CopyOnWriteArrayList<MapleMonster>();
            summons = new CopyOnWriteArrayList<MapleSummon>();
            visibleMapObjects = new CopyOnWriteArraySet<>();
            pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();

            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            customValue = new HashMap<>();
            customTime = new HashMap<>();
            deathcount = -1;

        }
    }

    public static MapleCharacter getDefault(final MapleClient client, final JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.exp = 0;
        ret.gmLevel = 0;
        ret.job = (short) type.id;
        ret.meso = 0;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList((byte) 20);
        ret.unions = new UnionList();

        ret.stats.str = 12;
        ret.stats.dex = 5;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 50;
        ret.stats.mp = 50;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();

            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.nxcredit = rs.getInt("nxCredit");
                ret.acash = rs.getInt("ACash");
                ret.maplepoints = rs.getInt("mPoints");
                ret.points = rs.getInt("points");
                ret.vpoints = rs.getInt("vpoints");
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
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
        return ret;
    }

    public final static MapleCharacter ReconstructChr(final CharacterTransfer ct, final MapleClient client, final boolean isChannel) {
        final MapleCharacter ret = new MapleCharacter(true); // Always true, it's change channel
        ret.client = client;
        if (!isChannel) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame;

        ret.setCalcDamage(new CalcDamage());

        ret.stats.str = ct.str;
        ret.stats.dex = ct.dex;
        ret.stats.int_ = ct.int_;
        ret.stats.luk = ct.luk;
        ret.stats.maxhp = ct.maxhp;
        ret.stats.maxmp = ct.maxmp;
        ret.stats.hp = ct.hp;
        ret.stats.mp = ct.mp;

        ret.characterCard.setCards(ct.cardsInfo);
        ret.customValue.putAll(ct.customValue);
        ret.customTime.putAll(ct.customTime);

        ret.chalktext = ct.chalkboard;
        ret.gmLevel = ct.gmLevel;
        ret.LinkMobCount = ct.LinkMobCount;
        ret.exp = ct.exp;
        ret.hpApUsed = ct.hpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso = ct.meso;
        ret.stolenSkills = ct.stolenSkills;
        ret.skinColor = ct.skinColor;
        ret.secondSkinColor = ct.secondSkinColor;
        ret.gender = ct.gender;
        ret.secondgender = ct.secondgender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.secondhair = ct.secondhair;
        ret.face = ct.face;
        ret.secondface = ct.secondface;
        ret.demonMarking = ct.demonMarking;
        ret.accountid = ct.accountid;
        ret.totalWins = ct.totalWins;
        ret.totalLosses = ct.totalLosses;
        client.setAccID(ct.accountid);
        ret.mapid = ct.mapid;
        ret.initialSpawnPoint = ct.initialSpawnPoint;
        ret.world = ct.world;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.guildContribution = ct.guildContribution;
        ret.allianceRank = ct.alliancerank;
        ret.points = ct.points;
        ret.vpoints = ct.vpoints;
        ret.fairyExp = ct.fairyExp;
        ret.cardStack = ct.cardStack;
        ret.marriageId = ct.marriageId;
        ret.currentrep = ct.currentrep;
        ret.totalrep = ct.totalrep;;
        ret.pvpExp = ct.pvpExp;
        ret.pvpPoints = ct.pvpPoints;
        /*Start of Custom Feature*/
        ret.reborns = ct.reborns;
        ret.apstorage = ct.apstorage;
        /*End of Custom Feature*/
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.fatigue = ct.fatigue;
        ret.buddylist = new BuddyList(ct.buddysize);
        ret.setUnions(new UnionList());
        ret.subcategory = ct.subcategory;
        ret.keyValues.putAll(ct.keyValues);
        ret.removeEffects.addAll(ct.removeEffects);
        ret.auth = ct.auth;

        if (isChannel) {
            final MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                ret.map = mapFactory.getMap(ServerConstants.WarpMap);
            } else if (ret.map.getForcedReturnId() != 999999999 && ret.map.getForcedReturnMap() != null) {
                ret.map = ret.map.getForcedReturnMap();
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());

            final int messengerid = ct.messengerid;
            if (messengerid > 0) {
                ret.messenger = World.Messenger.getMessenger(messengerid);
            }
        } else {
            ret.messenger = null;
        }
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }

        MapleQuestStatus queststatus_from;
        for (final Map.Entry<Integer, Object> qs : ct.Quest.entrySet()) {
            queststatus_from = (MapleQuestStatus) qs.getValue();
            queststatus_from.setQuest(qs.getKey());
            ret.quests.put(queststatus_from.getQuest(), queststatus_from);
        }
        for (final Map.Entry<Integer, SkillEntry> qs : ct.Skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), qs.getValue());
        }
        for (Entry<MapleTraitType, Integer> t : ct.traits.entrySet()) {
            ret.traits.get(t.getKey()).setExp(t.getValue());
        }
        ret.inventory = (MapleInventory[]) ct.inventorys;
        ret.BlessOfFairy_Origin = ct.BlessOfFairy;
        ret.BlessOfEmpress_Origin = ct.BlessOfEmpress;
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        ret.keylayout = new MapleKeyLayout(ct.keymap);
        ret.questinfo = ct.InfoQuest;
        ret.savedLocations = ct.savedlocation;
        ret.wishlist = ct.wishlist;
        ret.rocks = ct.rocks;
        ret.regrocks = ct.regrocks;
        ret.hyperrocks = ct.hyperrocks;
        ret.buddylist.loadFromTransfer(ct.buddies);
        ret.unions.loadFromTransfer(ct.unions);
        // ret.lastfametime
        // ret.lastmonthfameids
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastfametime = ct.lastfametime;
        ret.lastmonthfameids = ct.famedcharacters;
        ret.lastmonthbattleids = ct.battledaccs;
        ret.extendedSlots = ct.extendedSlots;
        ret.itcafetime = ct.itcafetime;
        ret.storage = (MapleStorage) ct.storage;
        ret.cs = (CashShop) ct.cs;
        client.setAccountName(ct.accountname);
        client.setSecondPassword(ct.secondPassword);
        client.setLogin(ct.login);
        ret.nxcredit = ct.nxCredit;
        ret.acash = ct.ACash;
        ret.maplepoints = ct.MaplePoints;
        ret.numClones = ct.clonez;
        ret.pets = ct.pets;
        ret.imps = ct.imps;
        ret.rebuy = ct.rebuy;
        ret.cores = ct.cores;
        ret.matrixs = ct.matrixs;
        ret.symbol = ct.symbol;
        ret.setAuctionitems(ct.auctionitems);
        ret.basecolor = ct.basecolor;
        ret.addcolor = ct.addcolor;
        ret.baseprob = ct.baseprob;

        ret.secondbasecolor = ct.secondbasecolor;
        ret.secondaddcolor = ct.secondaddcolor;
        ret.secondbaseprob = ct.secondbaseprob;

        ret.linkskills = ct.linkskills;
        ret.mount = new MapleMount(ret, ct.mount_itemid, PlayerStats.getSkillByJob(1004, ret.job), ct.mount_Fatigue, ct.mount_level, ct.mount_exp);
        ret.honourExp = ct.honourexp;
        ret.honorLevel = ct.honourlevel;
        ret.innerSkills = (LinkedList<InnerSkillValueHolder>) ct.innerSkills;
        ret.returnscroll = (Equip) ct.returnscroll;
        ret.choicepotential = (Equip) ct.choicepotential;
        ret.memorialcube = (Item) ct.memorialcube;
        ret.returnSc = ct.returnSc;
        ret.lastCharGuildId = ct.lastCharGuildId;
        ret.betaclothes = ct.betaclothes;
        ret.arcSpellSkills = ct.arcSpellSkills;
        ret.energy = ct.energy;
        ret.energyCharge = ct.energycharge;
        ret.hairRoom = ct.hairRoom;
        ret.faceRoom = ct.faceRoom;
        ret.skinRoom = ct.skinRoom;
        ret.bpoint = ct.bpoint;
        ret.basebpoint = ct.basebpoint;
        ret.bosstier = ct.bosstier;
        ret.tier = ct.tier;
        ret.setLevelPoint(ct.levelpoint);
        ret.expirationTask(false, false);
        ret.stats.recalcLocalStats(true, ret);
        ret.NameValue = ct.NameValue;
        client.setTempIP(ct.tempIP);
        return ret;
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        return loadCharFromDB(charid, client, channelserver, null);
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver, final Map<Integer, CardData> cads) {
        final MapleCharacter ret = new MapleCharacter(channelserver);
        ret.client = client;
        ret.id = charid;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        Connection con = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading the Char Failed (char not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame");
            ret.MesoChairCount = rs.getInt("mesochair");
            ret.stats.str = rs.getShort("str");
            ret.stats.dex = rs.getShort("dex");
            ret.stats.int_ = rs.getShort("int");
            ret.stats.luk = rs.getShort("luk");
            ret.stats.maxhp = rs.getInt("maxhp");
            ret.stats.maxmp = rs.getInt("maxmp");
            ret.stats.hp = rs.getInt("hp");
            ret.stats.mp = rs.getInt("mp");
            ret.job = rs.getShort("job");
            ret.gmLevel = rs.getByte("gm");
            ret.LinkMobCount = rs.getInt("LinkMobCount");
            ret.exp = rs.getLong("exp");
            ret.hpApUsed = rs.getShort("hpApUsed");
            final String[] sp = rs.getString("sp").split(",");
            for (int i = 0; i < ret.remainingSp.length; i++) {
                ret.remainingSp[i] = 0;//Integer.parseInt(sp[i]);
            }
            ret.remainingAp = rs.getShort("ap");
            ret.meso = rs.getLong("meso");
            ret.skinColor = rs.getByte("skincolor");
            ret.secondSkinColor = rs.getByte("secondSkincolor");
            ret.gender = rs.getByte("gender");
            ret.secondgender = rs.getByte("secondgender");
            ret.hair = rs.getInt("hair");
            ret.basecolor = rs.getInt("basecolor");
            ret.addcolor = rs.getInt("addcolor");
            ret.baseprob = rs.getInt("baseprob");

            ret.secondbasecolor = rs.getInt("secondbasecolor");
            ret.secondaddcolor = rs.getInt("secondaddcolor");
            ret.secondbaseprob = rs.getInt("secondbaseprob");

            ret.secondhair = rs.getInt("secondhair");
            ret.face = rs.getInt("face");
            ret.secondface = rs.getInt("secondface");
            ret.demonMarking = rs.getInt("demonMarking");
            ret.accountid = rs.getInt("accountid");
            client.setAccID(ret.accountid);
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getByte("spawnpoint");
            ret.world = rs.getByte("world");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getByte("guildrank");
            ret.allianceRank = rs.getByte("allianceRank");
            ret.guildContribution = rs.getInt("guildContribution");
            ret.totalWins = rs.getInt("totalWins");
            ret.totalLosses = rs.getInt("totalLosses");
            ret.currentrep = rs.getInt("currentrep");
            ret.totalrep = rs.getInt("totalrep");
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.buddylist = new BuddyList(rs.getByte("buddyCapacity"));
            ret.setUnions(new UnionList());
            ret.honourExp = rs.getInt("honourExp");
            ret.honorLevel = rs.getInt("honourLevel");
            ret.subcategory = rs.getByte("subcategory");
            ret.mount = new MapleMount(ret, 0, ret.stats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            ret.marriageId = rs.getInt("marriageId");
            ret.fatigue = rs.getShort("fatigue");
            ret.pvpExp = rs.getInt("pvpExp");
            ret.pvpPoints = rs.getInt("pvpPoints");
            ret.itcafetime = rs.getInt("itcafetime");
            /*Start of Custom Features*/
            ret.reborns = rs.getInt("reborns");
            ret.apstorage = rs.getInt("apstorage");
            ret.betaclothes = rs.getInt("betaclothes");

            long choiceId = rs.getLong("choicepotential"), memorialId = rs.getLong("memorialcube"), returnscroll = rs.getLong("returnscroll");

            ret.returnSc = rs.getInt("returnsc");
            if (rs.getString("exceptionlist").length() > 0) {
                final String[] exceptionList = rs.getString("exceptionlist").split(",");
                for (String str : exceptionList) {
                    ret.getExceptionList().add(Integer.parseInt(str));
                }
            }
            ret.bpoint = rs.getLong("bpoint");
            ret.basebpoint = rs.getLong("basebpoint");
            ret.bosstier = rs.getInt("bosstier");
            ret.tier = rs.getInt("tier");
            ret.setLevelPoint(rs.getLong("levelpoint"));
            /*End of Custom Features*/
            for (MapleTrait t : ret.traits.values()) {
                t.setExp(rs.getInt(t.getType().name()));
            }

            if (channelserver) {

                ret.setCalcDamage(new CalcDamage());
                MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                    ret.map = mapFactory.getMap(ServerConstants.WarpMap);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = World.Party.getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                }

                /* Pet Loading */
                final String[] pets = rs.getString("pets").split(",");

                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM inventoryitemscash WHERE uniqueid = ?");
                for (int next = 0; next < 3; ++next) {
                    if (!pets[next].equals("-1")) {
                        int petid = Integer.parseInt(pets[next]);
                        ps.setInt(1, petid);
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            MaplePet pet = MaplePet.loadFromDb(rs.getInt("itemid"), petid, rs.getShort("position"));
                            ret.addPetBySlotId(pet, (byte) next);
                        }
                    }
                }
            }

            ps.close();
            rs.close();

            if (cads != null) { // so that we load only once.
                ret.characterCard.setCards(cads);
            } else { // load
                ret.characterCard.loadCards(client, channelserver);
            }

            if (channelserver) {

                ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    final int id = rs.getInt("quest");
                    final MapleQuest q = MapleQuest.getInstance(id);
                    final byte stat = rs.getByte("status");
//	                if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { //bigbang
                    //                    continue;
                    //              }
                    //            if (stat == 1 && channelserver && !q.canStart(ret, null)) { //bigbang
                    //              continue;
                    //        }
                    final MapleQuestStatus status = new MapleQuestStatus(q, stat);
                    final long cTime = rs.getLong("time");
                    if (cTime > -1) {
                        status.setCompletionTime(cTime * 1000);
                    }
                    status.setForfeited(rs.getInt("forfeited"));
                    status.setCustomData(rs.getString("customData"));
                    ret.quests.put(q, status);

                    pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
                    pse.setInt(1, rs.getInt("queststatusid"));
                    final ResultSet rsMobs = pse.executeQuery(); //CPU렉 1위

                    if (rsMobs.next()) {
                        status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                    }
                    pse.close();
                    rsMobs.close();
                }

                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
                } else {
                    ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getShort("equip"));
                    ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getShort("use"));
                    ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getShort("setup"));
                    ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getShort("etc"));
                    ret.getInventory(MapleInventoryType.CASH).setSlotLimit(rs.getShort("cash"));
                    ret.getInventory(MapleInventoryType.DECORATION).setSlotLimit(rs.getShort("decoration"));
                }
                ps.close();
                rs.close();

                for (MapleInventoryType type : MapleInventoryType.values()) {
                    if (type.getType() != 0) {
                        for (Entry<Long, Item> mit : ItemLoader.INVENTORY.loadItems(false, charid, type).entrySet()) {
                            if (mit.getValue().getInventoryId() == choiceId && choiceId > 0) {
                                ret.choicepotential = (Equip) mit.getValue();
                            } else if (mit.getValue().getInventoryId() == memorialId && memorialId > 0) {
                                ret.memorialcube = mit.getValue();
                            } else if (mit.getValue().getInventoryId() == returnscroll && returnscroll > 0) {
                                ret.returnscroll = (Equip) mit.getValue();
                            } else {
                                ret.getInventory(type.getType()).addFromDB(mit.getValue());
                            }
                            if (mit.getValue().getPet() != null) {
//                                ret.addPet(mit.getValue().getPet());
                            }
                        }
                    }
                }

                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.getClient().setSecondPassword(rs.getString("2ndpassword"));
                    ret.nxcredit = rs.getInt("nxCredit");
                    ret.acash = rs.getInt("ACash");
                    ret.maplepoints = rs.getInt("mPoints");
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");

                    if (rs.getTimestamp("lastlogon") != null) {
                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
                        if (cal.get(Calendar.DAY_OF_WEEK) + 1 == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                            ret.acash += 500;
                        }
                    }
                    if (rs.getInt("banned") > 0) {
                        rs.close();
                        ps.close();
                        ret.getClient().getSession().close();
                        throw new RuntimeException("Loading a banned character");
                    }
                    rs.close();
                    ps.close();

                    ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    ps.executeUpdate();
                } else {
                    rs.close();
                }
                ps.close();

                if (ServerConstants.authlist2.get(ret.getClient().getAccountName()) != null) {
                    ret.auth = ServerConstants.authlist2.get(ret.getClient().getAccountName()).right;
                }

                try {
                    ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                    ps.setInt(1, charid);
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
                    }
                } finally {
                    rs.close();
                    ps.close();
                }

                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? AND level >= 70");
                ps.setInt(1, ret.getAccountID());
                rs = ps.executeQuery();
                while (rs.next()) {
                    int skid = GameConstants.getLinkedSkillByJob((rs.getShort("job")));
                    if (skid == 0 || ret.getName().equals(rs.getString("name"))) { // 留곹겕 ?ㅽ궗???녿뒗 吏곸뾽援곗씠?? ?뱀떆 ?덈꺼 泥댄겕 ?섎せ?섎㈃ 嫄대꼫?곌린
//                		System.out.println("嫄대꼫??罹먮┃??: " + rs.getString("name"));
                        continue;
                    }
                    Skill skil = SkillFactory.getSkill(skid);
                    int skl = skil.getId() == 80000110 ? (rs.getInt("level") >= 200 ? 5 : rs.getInt("level") >= 180 ? 4 : rs.getInt("level") >= 160 ? 3 : rs.getInt("level") >= 140 ? 2 : 1) : rs.getInt("level") >= 120 ? 2 : 1;
                    boolean pass = false;
                    for (Triple<Skill, SkillEntry, Integer> a : ret.linkskills) {
                        if (a.getLeft().getId() == skid) {
                            if (!(a.getMid().skillevel < skl)) {
                                pass = true;
                            }
                        }
                    }
                    byte max = skil.getId() == 80000110 ? (byte) 5 : 2;
                    //              	System.out.println(rs.getString("name") + " 罹먮┃?곗쓽 留곹겕?ㅽ궗 ?덈꺼 : " + skl);
                    if (!pass) {
                        ret.linkskills.add(new Triple<Skill, SkillEntry, Integer>(skil, new SkillEntry(skl, max, -1), rs.getInt("id")));
                    }
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel, expiration FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Skill skil;
                while (rs.next()) {
                    final int skid = rs.getInt("skillid");
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel");
                    byte msl = rs.getByte("masterlevel");
                    if (skil != null) {// && GameConstants.isApplicableSkill(skid)) {
                        if (skl > skil.getMaxLevel() && skid < 92000000) {
                            if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret) && !skil.isSpecialSkill()) {
                                ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += (skl - skil.getMaxLevel());
                            }
                            skl = (byte) skil.getMaxLevel();
                        }
                        if (msl > skil.getMaxLevel()) {
                            msl = (byte) skil.getMaxLevel();
                        }
                        ret.skills.put(skil, new SkillEntry(skl, msl, rs.getLong("expiration")));
                    } else if (skil == null) { //doesnt. exist. e.g. bb
                        if (!GameConstants.isBeginnerJob(skid / 10000) && skid / 10000 != 900 && skid / 10000 != 800 && skid / 10000 != 9000) {
                            ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += skl;
                        }
                    }
                }
                rs.close();
                ps.close();

                try {
                    ps = con.prepareStatement("SELECT * FROM core WHERE charid = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
                    ps.setInt(1, ret.id);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        Core core = new Core(rs.getLong("crcid"), rs.getInt("coreid"), ret.id, rs.getInt("level"), rs.getInt("exp"), rs.getInt("state"), rs.getInt("maxlevel"), rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getInt("position"));
                        ret.cores.add(core);
                        core.setId(ret.cores.indexOf(core));
                    }
                    ps.close();
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    ps = con.prepareStatement("SELECT * FROM matrix WHERE charid = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
                    ps.setInt(1, ret.id);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        VMatrix matrix = new VMatrix(rs.getInt("id"), rs.getInt("position"), rs.getInt("level"), rs.getByte("unlock") == 1);
                        ret.matrixs.add(matrix);
                    }
                    ps.close();
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ret.expirationTask(false, true); //do it now

                // Bless of Fairy handling
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                int maxlevel_ = 0, maxlevel_2 = 0;
                while (rs.next()) {
                    if (rs.getInt("id") != charid) { // Not this character
                        if (GameConstants.isKOC(rs.getShort("job"))) {
                            int maxlevel = (rs.getShort("level") / 5);

                            if (maxlevel > 24) {
                                maxlevel = 24;
                            }
                            if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                maxlevel_2 = maxlevel;
                                ret.BlessOfEmpress_Origin = rs.getString("name");
                            }
                        }
                        int maxlevel = (rs.getShort("level") / 10);

                        if (maxlevel > 20) {
                            maxlevel = 20;
                        }
                        if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                            maxlevel_ = maxlevel;
                            ret.BlessOfFairy_Origin = rs.getString("name");
                        }

                    }
                }
                /*if (!compensate_previousSP) {
                 for (Entry<Skill, SkillEntry> skill : ret.skills.entrySet()) {
                 if (!skill.getKey().isBeginnerSkill() && !skill.getKey().isSpecialSkill()) {
                 ret.remainingSp[GameConstants.getSkillBookForSkill(skill.getKey().getId())] += skill.getValue().skillevel;
                 skill.getValue().skillevel = 0;
                 }
                 }
                 ret.setQuestAdd(MapleQuest.getInstance(170000), (byte) 0, null); //set it so never again
                 }*/
                if (ret.BlessOfFairy_Origin == null) {
                    ret.BlessOfFairy_Origin = ret.name;
                }
                ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.job)), new SkillEntry(maxlevel_, (byte) 0, -1));
                if (SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)) != null) {
                    if (ret.BlessOfEmpress_Origin == null) {
                        ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                    }
                    ret.skills.put(SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)), new SkillEntry(maxlevel_2, (byte) 0, -1));
                }
                ps.close();
                rs.close();
                // END

                ps = con.prepareStatement("SELECT skill_id, skill_level, max_level, rank FROM inner_ability_skills WHERE player_id = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.innerSkills.add(new InnerSkillValueHolder(rs.getInt("skill_id"), rs.getByte("skill_level"), rs.getByte("max_level"), rs.getByte("rank")));
                }
                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mannequins WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int type = rs.getInt("type");
                    MapleMannequin mq = new MapleMannequin(rs.getInt("value"), rs.getInt("baseProb"), rs.getInt("baseColor"), rs.getInt("addColor"));
                    switch (type) {
                        case 0:
                            ret.hairRoom.add(mq);
                            break;
                        case 1:
                            ret.faceRoom.add(mq);
                            break;
                        case 2:
                            ret.skinRoom.add(mq);
                            break;
                    }
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                final Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(Integer.valueOf(rs.getInt("key")), new Pair<Byte, Integer>(rs.getByte("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ret.keylayout.unchanged();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `accid_to`,`when` FROM battlelog WHERE accid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                ret.lastmonthbattleids = new ArrayList<Integer>();
                while (rs.next()) {
                    ret.lastmonthbattleids.add(Integer.valueOf(rs.getInt("accid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM keyvalue WHERE id = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.keyValues.put(rs.getString("key"), rs.getString("value"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `itemId` FROM extendedSlots WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.extendedSlots.add(Integer.valueOf(rs.getInt("itemId")));
                }
                rs.close();
                ps.close();

                ret.buddylist.loadFromDb(ret.accountid);
                ret.storage = MapleStorage.loadStorage(ret.accountid);
                ret.getUnions().loadFromDb(ret.accountid);

                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());

                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 12) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int r = 0;
                while (rs.next()) {
                    ret.rocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.regrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 5) {
                    ret.regrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM hyperrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.hyperrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 13) {
                    ret.hyperrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * from stolen WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.stolenSkills.add(new Pair<>(rs.getInt("skillid"), rs.getInt("chosen") > 0));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM imps WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.imps[r] = new MapleImp(rs.getInt("itemid"));
                    ret.imps[r].setLevel(rs.getByte("level"));
                    ret.imps[r].setState(rs.getByte("state"));
                    ret.imps[r].setCloseness(rs.getShort("closeness"));
                    ret.imps[r].setFullness(rs.getShort("fullness"));
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("No mount data found on SQL column");
                }
                final Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -23);
                ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, PlayerStats.getSkillByJob(1004, ret.job), rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();

                ret.stats.recalcLocalStats(true, ret);
            } else { // Not channel server
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    if (type.getType() != 0) {
                        for (Entry<Long, Item> mit : ItemLoader.INVENTORY.loadItems(false, charid, type).entrySet()) {
                            ret.getInventory(type.getType()).addFromDB(mit.getValue());
                            if (mit.getValue().getPet() != null) {
//                                ret.addPet(mit.getValue().getPet());
                            }
                        }
//                        System.out.println("[Load] " + type.name() + " : " + ret.getInventory(type).newList().size());
                    }
                }
                ret.stats.recalcPVPRank(ret);
            }
        } catch (SQLException ess) {
            ess.printStackTrace();
            System.out.println("Failed to load character..");
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ess);
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
            } catch (SQLException ignore) {
            }
        }
        return ret;
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db) {
        Connection con = null;

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, ap, skincolor, secondSkinColor, gender, secondgender, job, hair, secondhair, face, secondface, demonMarking, map, meso, party, buddyCapacity, subcategory, accountid, name, world, itcafetime, basecolor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, chr.level); // Level
            final PlayerStats stat = chr.stats;
            ps.setShort(2, stat.getStr()); // Str
            ps.setShort(3, stat.getDex()); // Dex
            ps.setShort(4, stat.getInt()); // Int
            ps.setShort(5, stat.getLuk()); // Luk
            ps.setLong(6, stat.getHp()); // HP
            ps.setLong(7, stat.getMp());
            ps.setLong(8, stat.getMaxHp()); // MP
            ps.setLong(9, stat.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < chr.remainingSp.length; i++) {
                sps.append(chr.remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(10, sp.substring(0, sp.length() - 1));
            ps.setShort(11, (short) chr.remainingAp); // Remaining AP
            ps.setByte(12, chr.skinColor);
            ps.setByte(13, chr.secondSkinColor);
            ps.setByte(14, chr.gender);
            ps.setByte(15, chr.secondgender);
            ps.setShort(16, chr.job);
            ps.setInt(17, chr.hair);
            ps.setInt(18, chr.secondhair);
            ps.setInt(19, chr.face);
            ps.setInt(20, chr.secondface);
            ps.setInt(21, chr.demonMarking);
            if (db < 0 || db > 2) { //todo legend
                db = 0;
            }
            ps.setInt(22, type.map);
            ps.setLong(23, chr.meso); // Meso
            ps.setInt(24, -1); // Party
            ps.setByte(25, chr.buddylist.getCapacity()); // Buddylist
            ps.setInt(26, db); //for now
            ps.setInt(27, chr.getAccountID());
            ps.setString(28, chr.name);
            ps.setByte(29, chr.world);
            ps.setInt(30, chr.getInternetCafeTime());
            ps.setInt(31, -1); //basecolor
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                ps.close();
                rs.close();
                throw new DatabaseException("Inserting char failed.");
            }
            ps.close();
            rs.close();

            for (final MapleQuestStatus q : chr.quests.values()) {
                ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
                ps.setInt(1, chr.id);
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                        pse.close();
                    }
                }
                ps.close();
                rs.close();
            }

            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);

            for (final Entry<Skill, SkillEntry> skill : chr.skills.entrySet()) {
//                if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                ps.setInt(2, skill.getKey().getId());
                ps.setInt(3, skill.getValue().skillevel);
                ps.setByte(4, skill.getValue().masterlevel);
                ps.setLong(5, skill.getValue().expiration);
                ps.execute();
//                }
            }
            ps.close();

            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`,`decoration`) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setShort(2, (short) 128); // Eq
            ps.setShort(3, (short) 128); // Use
            ps.setShort(4, (short) 128); // Setup
            ps.setShort(5, (short) 128); // ETC
            ps.setShort(6, (short) 128); // Cash
            ps.setShort(7, (short) 128); // Decoration
            ps.execute();
            ps.close();

            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();

            final int[] array1 = {2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 33, 34, 35, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 50, 51, 56, 57, 59, 60, 61, 62, 63, 64, 65, 83, 1, 70};
            final int[] array2 = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 0, 4, 4};
            final int[] array3 = {10, 12, 13, 18, 23, 28, 8, 5, 0, 4, 27, 30, 32, 1, 24, 19, 14, 15, 52, 2, 25, 17, 11, 3, 20, 26, 16, 22, 9, 50, 51, 6, 31, 29, 7, 33, 53, 54, 100, 101, 102, 103, 104, 105, 106, 52, 46, 47};

            for (int i = 0; i < array1.length; i++) {
                ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
                ps.setInt(1, chr.id);
                ps.setInt(2, array1[i]);
                ps.setInt(3, array2[i]);
                ps.setInt(4, array3[i]);
                ps.execute();
                ps.close();
            }

            new MapleCharacterSave(chr).saveInventory(con, true);
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println("[charsave] Error saving character data");
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
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
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                e.printStackTrace();
                System.err.println("[charsave] Error going back to autocommit mode");
            }
        }
    }

    public void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.execute();
        ps.close();
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final void QuestInfoPacket(final tools.data.MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(questinfo.size()); // // Party Quest data (quest needs to be added in the quests list)

        for (final Entry<Integer, String> q : questinfo.entrySet()) {
            mplew.writeInt(q.getKey());
            mplew.writeMapleAsciiString(q.getValue() == null ? "" : q.getValue());
        }
    }

    public final void updateInfoQuest(final int questid, final String data) {
        questinfo.put(questid, data);
        changed_questinfo = true;
        client.getSession().writeAndFlush(InfoPacket.updateInfoQuest(questid, data));
    }

    public final String getInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public final int getNumQuest() {
        int i = 0;

        for (final MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        final MapleQuest qq = MapleQuest.getInstance(quest);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public final MapleQuestStatus getQuest(final MapleQuest quest) {

        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, (byte) 0);
        }

        return quests.get(quest);
    }

    public final void setQuestAdd(final MapleQuest quest, final byte status, final String customData) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest, stat);
        }
    }

    public final MapleQuestStatus getQuestNAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }

        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestNoAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return null;
        }
        return quests.get(quest);
    }

    public final void updateQuest(final MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public final void updateQuest(final MapleQuestStatus quest, final boolean update) {
        quests.put(quest.getQuest(), quest);
        if (!(quest.isCustom()) && update) {
            client.getSession().writeAndFlush(InfoPacket.updateQuest(quest));
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuest_Map() {
        return quests;
    }

    public MapleStatEffect getBuffedEffect(MapleBuffStat effect) {
        return getBuffedEffect(effect, getBuffSource(effect));
    }

    public boolean checkBuffStat(MapleBuffStat stat) {
        if (stat == null) {
            return false;
        }

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> eff = effect.next();
            if (stat == eff.left) {
                return true;
            }
        }
        return false;
    }

    public MapleBuffStatValueHolder checkBuffStatValueHolder(MapleBuffStat stat) {
        if (stat == null || effects == null) {
            return null;
        }

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> eff = effect.next();
            if (stat == eff.left) {
                return eff.right;
            }
        }
        return null;
    }

    public MapleBuffStatValueHolder checkBuffStatValueHolder(MapleStatEffect ef, Entry<MapleBuffStat, Pair<Integer, Integer>> stat) {
        if (stat == null || effects == null) {
            return null;
        }

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> eff = effect.next();
            if (stat.getKey() == eff.getLeft() && ef.getSourceId() == eff.getRight().effect.getSourceId()) {
                return eff.right;
            }
        }
        return null;
    }

    public MapleBuffStatValueHolder checkBuffStatValueHolder(MapleBuffStat stat, int skillId) {
        if (stat == null || effects == null) {
            return null;
        }

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> eff = effect.next();
            if (stat == eff.left && skillId == eff.right.effect.getSourceId()) {
                return eff.right;
            }
        }

        return null;
    }

    public MapleStatEffect getBuffedEffect(int skillId) {

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> eff = effect.next();
            if (skillId == eff.right.effect.getSourceId()) {
                return eff.right.effect;
            }
        }
        return null;
    }

    public MapleStatEffect getBuffedEffect(MapleBuffStat effect, int skillid) {
        if (!checkBuffStat(effect)) {
            return null;
        }
        MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect, skillid);
        if (mbsvh == null) {
            return null;
        }

        return mbsvh.effect;
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        return mbsvh == null ? null : Integer.valueOf(mbsvh.value);
    }

    public Integer getBuffedValue(MapleBuffStat effect, int skillId) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect, skillId);
        return mbsvh == null ? null : Integer.valueOf(mbsvh.value);
    }

    public int getBuffedSkill(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        return mbsvh == null ? 0 : Integer.valueOf(mbsvh.value);
    }

    public boolean getBuffedValue(int skillid) {
        if (SkillFactory.getSkill(skillid) == null) {
            return false;
        }

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> eff = effect.next();
            if (skillid == eff.right.effect.getSourceId()) {
                return true;
            }
        }
        return false;
    }

    public final Integer getBuffedSkill_X(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getX();
    }

    public final Integer getBuffedSkill_Y(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getY();
    }

    public void setDressup(boolean isdress) {
        isdressup = isdress;
    }

    public boolean getDressup() {
        return isdressup;
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(stat);
        if (mbsvh == null || mbsvh.effect == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public int getBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(stat);
        return mbsvh == null ? 0 : mbsvh.effect.getSourceId();
    }

    public int getTrueBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(stat);
        return mbsvh == null ? 0 : (mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setBuffedValue(MapleBuffStat effect, int skillid, int value) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        if (mbsvh == null || !checkBuffStat(effect)) {
            return;
        }
        if (skillid == -1) {
            if (checkBuffStatValueHolder(effect) != null) {
                checkBuffStatValueHolder(effect).value = value;
            }
        } else if (mbsvh.effect.getSourceId() == skillid) {
            mbsvh.value = value;
        }
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(effect);
        return mbsvh == null ? null : Long.valueOf(mbsvh.startTime);
    }

    public final boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    private void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getBuffedEffect(MapleBuffStat.Recovery);
        if (bloodEffect == null) {
            lastRecoveryTime = 0;
            return;
        } else {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.Recovery);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public final boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }

    public boolean canDOT(long now) {
        return lastDOTTime > 0 && lastDOTTime + 8000 < now;
    }

    public boolean hasDOT() {
        return dotHP > 0;
    }

    public void doDOT() {
        addHP(-(dotHP * 4));
        dotHP = 0;
        lastDOTTime = 0;
    }

    public long getNeededExp() {
        return GameConstants.getExpNeededForLevel(level);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, Entry<MapleBuffStat, Pair<Integer, Integer>> statup, boolean silent, final int cid) {
        if (effect.isDragonBlood()) {
            prepareDragonBlood();
        } else if (effect.isRecovery()) {
            prepareRecovery();
        } else if (effect.isMonsterRiding_()) {
            getMount().startSchedule();
        }
        int value = statup.getValue().left.intValue();
        if (statup.getKey() != null && statup.getValue() != null) {
            effects.add(new Pair<>(statup.getKey(), new MapleBuffStatValueHolder(effect, starttime, schedule, value, statup.getValue().right, cid)));
        } else {
          //  System.out.println("NULL EFFECT : " + effect.getSourceId());
        }

        if (!silent) {
            stats.recalcLocalStats(this);
        }
    }

    public List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> bstats = new ArrayList<MapleBuffStat>();

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects) {
            if (stateffect.right.effect.sameSource(effect)) {
                bstats.add(stateffect.left);
            }
        }
        return bstats;
    }

    public void cancelBuffStat(MapleStatEffect effect, Map<MapleBuffStat, Pair<Integer, Integer>> statups, List<MapleSummon> summons) {
        List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effectz = new ArrayList<>();

        for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
            MapleBuffStatValueHolder mbsvh = checkBuffStatValueHolder(statup.getKey(), effect.getSourceId());

            if (mbsvh != null) {
                effectz.add(new Pair<>(statup.getKey(), mbsvh));
            }
        }

        if (client.isAuction()) {
            client.getPlayer().getRemoveEffects().addAll(effectz);
            return;
        }

        effects.removeAll(effectz);

        dropMessageGM(-8, "현재 버프 사이즈 : " + effects.size() + " / 삭제한 버프 사이즈 : " + effectz.size());

        if (effect.getSourceId() == 400011001) {
            MapleSummon summon = getSummon(400011001);
            MapleSummon summon2 = getSummon(400011002);
            if (summon != null) {
                summon.removeSummon(map, false);
            }
            if (summon2 != null) {
                summon2.removeSummon(map, false);
            }

        }

        if (effect.getSourceId() == 400051022) {
            MapleSummon summon = getSummon(400051023);
            if (summon != null) {
                summons.add(summon);
            }
        }

        for (MapleSummon remove : summons) {
            remove.removeSummon(map, false);
        }

        if (effect.getSourceId() == 5321004) {
            MapleSummon summon = getSummon(5320011);
            if (summon != null) {
                summon.removeSummon(map, false);
            }

            if (getSkillLevel(5320045) > 0) {
                MapleSummon summon2 = getSummon(5320011);
                if (summon2 != null) {
                    summon2.removeSummon(map, false);
                }
            }
        }

        if (effect.getSourceId() == 400051038) {
            MapleSummon summon = getSummon(400051052);
            if (summon != null) {
                summon.removeSummon(map, false);
            }

            MapleSummon summon2 = getSummon(400051053);
            if (summon2 != null) {
                summon2.removeSummon(map, false);
            }
        }

        if (effect.getSourceId() == 35111002) {
            MapleSummon summon = getSummon(35111002);
            if (summon != null) {
                summon.removeSummon(map, false);
            }

            MapleSummon summon2 = getSummon(35111002);
            if (summon2 != null) {
                summon2.removeSummon(map, false);
            }
        }

        if (effect.getSourceId() == 61121053) {
            backKaiserCombo();
        } else if (effect.getSourceId() == 61120008) {
            resetKaiserCombo();
        }
    }

    public void removeAllBuffSchedule() {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> effect : effects) {
            if (effect.right.schedule != null && !effect.right.schedule.isCancelled()) {
                effect.right.schedule.cancel(false);
            }
        }
    }

    public Map<MapleBuffStat, Pair<Integer, Integer>> deregisterBuffStat(MapleStatEffect effect, MapleBuffStat stat, boolean forcecancel) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        List<MapleSummon> toRemove = new ArrayList<>();

        long time = System.currentTimeMillis();

        dropMessageGM(6, "autoCancelBuff : " + effect.getSourceId());

        MapleBuffStatValueHolder mbsvh;
        if (stat.isStacked()) { // indie는 같은 스킬만 없애면 된다.
            mbsvh = checkBuffStatValueHolder(stat, effect.getSourceId());
        } else {
            mbsvh = checkBuffStatValueHolder(stat);
        }
        if (mbsvh != null) {

            if (forcecancel || (mbsvh.localDuration == 0 || (time - mbsvh.startTime >= mbsvh.localDuration))) {
                statups.put(stat, new Pair<>(mbsvh.value, mbsvh.localDuration));
                dropMessageGM(-8, "addAutoCancelBuff : " + stat.name() + "  " + mbsvh.localDuration);
                if (mbsvh.schedule != null && !mbsvh.schedule.isCancelled()) {
                    mbsvh.schedule.cancel(false);
                }
            }
        } else {
//        	System.out.println(effect.getSourceId() + " / " + stat.name() + " 버프에 오류 발생.");
        }

        MapleSummon summon = getSummon(effect.getSourceId());
        if (summon != null) {
            if (summon.getSkill() != 400021047) {
                toRemove.add(summon);
                dropMessageGM(-8, "Add CancelSummon : " + effect.getSourceId());
            }
        }

        cancelBuffStat(effect, statups, toRemove);
        return statups;
    }

    public Map<MapleBuffStat, Pair<Integer, Integer>> deregisterBuffStats(MapleStatEffect effect) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        List<MapleSummon> toRemove = new ArrayList<>();

        //autoCancel이면 지속시간 지난 애들만, autoCancel아니면 해당 스킬 ID에 해당하는 모든 버프 전부 다.
        dropMessageGM(6, "cancelBuff : " + effect.getSourceId());
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> eff : effects) {
            if (eff.right.effect.getSourceId() == effect.getSourceId()) {
//                if ((autoCancel && (time - eff.right.startTime >= eff.right.localDuration)) || !autoCancel) {
                dropMessageGM(-8, "addCancelBuff : " + eff.left.name());
                statups.put(eff.left, new Pair<>(eff.right.value, eff.right.localDuration));

                if (eff.right.schedule != null && !eff.right.schedule.isCancelled()) {
                    eff.right.schedule.cancel(false);
                }

                MapleSummon summon = getSummon(effect.getSourceId());
                if (summon != null) {
                    toRemove.add(summon);
                    dropMessageGM(-8, "Add CancelSummon : " + effect.getSourceId());
                }
//                }
            } else {
                if (effect.getSourceId() == 33001007 || effect.getSourceId() == 400031005) {
                    for (int z = 33001007; z <= 33001015; ++z) {
                        if (eff.right.effect.getSourceId() == z) {
                            dropMessageGM(-8, "addCancelBuff : " + eff.left.name());
                            statups.put(eff.left, new Pair<>(eff.right.value, eff.right.localDuration));

                            if (eff.right.schedule != null && !eff.right.schedule.isCancelled()) {
                                eff.right.schedule.cancel(false);
                            }

                            MapleSummon summon = getSummon(z);
                            if (summon != null) {
                                toRemove.add(summon);
                                dropMessageGM(-8, "Add CancelSummon : " + z);
                            }
                        }
                    }
                }
            }
        }

        cancelBuffStat(effect, statups, toRemove);

        return statups;
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime) {
        cancelEffect(effect, overwrite, startTime, deregisterBuffStats(effect));
    }

    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime, MapleBuffStat stat, boolean forcecancel) {
        cancelEffect(effect, overwrite, startTime, deregisterBuffStat(effect, stat, forcecancel));
    }

    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime, Map<MapleBuffStat, Pair<Integer, Integer>> statups) {
        try {
            if (effect == null) {
                return;
            }
            if (effect.getSourceId() == 400051068) {
                mecahCarriercount = 9;
            }
            if (effect.getSourceId() == 400031051) {
                setStartRelicUnboundTime(0);
            }
            if (effect.getSourceId() == 400041057) {
                PhotonRay_plus = 0;
            }
            if (effect.isMonsterRiding()) {
                statups.put(MapleBuffStat.RideVehicle, new Pair<>(MapleStatEffect.parseMountInfo(this, effect.getSourceId()), effect.getDuration()));
                if (getBuffedValue(80001242)) {
                    cancelEffect(getBuffedEffect(80001242), false, -1);
                }
            }

            int[] summonz = {3111013, 3121013, 3111015, 12120013, 12120014};
            for (int summon : summonz) {
                if (effect.getSourceId() == summon && !overwrite) {
                    if (getSummon(summon) != null) {
                        getSummon(summon).removeSummon(getMap(), false);
                        removeSummon(getSummon(summon));
                    }
                }
            }

            if (effect.getSourceId() == 12101024 && !overwrite) {
                setIgnition(0);
            }

            if (effect.getSourceId() == 400051046) {
                setUseTruthDoor(false);
            }

            if (effect.getSourceId() == 80002633) {
                List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
                getClient().getSession().writeAndFlush(CField.bonusAttackRequest(80002634, mobList, true, 0));
            }

            if (effect.getSourceId() == 2321054) {
                if (getBuffedEffect(400021033) != null) {
                    long duration = getBuffLimit(400021033);
                    cancelEffect(getBuffedEffect(400021033), false, -1);
                    SkillFactory.getSkill(400021032).getEffect(getSkillLevel(400021032)).applyTo(this, false, (int) duration);
                }
            }

            if (effect.getSourceId() == 400031044) {
                MapleStatEffect eff = SkillFactory.getSkill(400031044).getEffect(getSkillLevel(400031044));
                Map<MapleBuffStat, Pair<Integer, Integer>> statup = new HashMap<>();
                statup.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 1600));
                getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statup, eff, this));
            }

            if (effect.isMagicDoor()) {
                if (!getDoors().isEmpty()) {
                    removeDoor();
                    silentPartyUpdate();
                }
            } else if (effect.isMechDoor()) {
                if (!getMechDoors().isEmpty()) {
                    removeMechDoor();
                }
            } else if (statups.containsKey(MapleBuffStat.Reincarnation)) {
                if (getReinCarnation() > 0) {
                    addHP(-getStat().getCurrentMaxHp());
                } else {
                    addHP(getStat().getCurrentMaxHp());
                    addMP(getStat().getCurrentMaxMp(this));
                }
                setReinCarnation(0);
            } else if (effect.isMonsterRiding_()) {
                getMount().cancelSchedule();
            } else if (statups.containsKey(MapleBuffStat.BulletParty)) {
                bulletParty = 0;
            } else if (effect.getSourceId() == 400041029) {
                setXenonSurplus(getXenonSurplus() >= 20 ? 20 : getXenonSurplus(), SkillFactory.getSkill(30020232));
            } else if (effect.getSourceId() == 400051002 && !overwrite) {
                transformEnergyOrb = 0;
            } else if (statups.containsKey(MapleBuffStat.ElementalCharge) && !overwrite) {
                setElementalCharge(0);
                if (getSkillLevel(400011052) > 0 && getBuffedValue(MapleBuffStat.BlessedHammer) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.BlessedHammer);
                }
                if (getSkillLevel(400011053) > 0 && getBuffedValue(MapleBuffStat.BlessedHammer2) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.BlessedHammer2);
                }
                ElementalCharge_ID = 0;
            } else if (effect.getSourceId() == 400011052 && !overwrite) {
                setElementalCharge(0);
                statups.put(MapleBuffStat.BlessedHammer, new Pair<>(0, 0));
                getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
                getMap().broadcastMessage(this, BuffPacket.giveForeignBuff(this, statups, effect), false);
            } else if (effect.getSourceId() == 400011053 && !overwrite) {
                setElementalCharge(0);
                statups.put(MapleBuffStat.BlessedHammer2, new Pair<>(0, 0));
                getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
                getMap().broadcastMessage(this, BuffPacket.giveForeignBuff(this, statups, effect), false);
            }
            if (statups.containsKey(MapleBuffStat.Infinity) && !overwrite) {
                infinity = 0;
            }
            if (statups.containsKey(MapleBuffStat.TimeFastABuff) && !overwrite) {
                RapidTimeDetect = 0;
            }
            if (statups.containsKey(MapleBuffStat.TimeFastBBuff) && !overwrite) {
                RapidTimeStrength = 0;
            }
            if (statups.containsKey(MapleBuffStat.ArcaneAim) && !overwrite) {
                arcaneAim = 0;
            }
            if (statups.containsKey(MapleBuffStat.StackBuff) && !overwrite) {
                stackbuff = 0;
            }
            if (statups.containsKey(MapleBuffStat.BlessMark) && !overwrite) {
                blessMark = 0;
            }
            if ((statups.containsKey(MapleBuffStat.RwBarrier) || statups.containsKey(MapleBuffStat.PowerTransferGauge)) && !overwrite) {
                barrier = 0;
            }
            if (statups.containsKey(MapleBuffStat.RWOverHeat) && !overwrite) {
                Cylinder = 0;

                Map<MapleBuffStat, Pair<Integer, Integer>> statupz = new HashMap<>();
                statupz.put(MapleBuffStat.RWCylinder, new Pair<>(1, 0));
                getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statupz, null, this));
            }
            if (statups.containsKey(MapleBuffStat.OverloadCount) && !overwrite) {
                overloadCount = 0;
            }
            if (statups.containsKey(MapleBuffStat.Exceed) && !overwrite) {
                exceed = 0;
            }
            if (statups.containsKey(MapleBuffStat.GloryWing) && !overwrite) {
                canUseMortalWingBeat = false;
            }
            if (effect.getSourceId() == 2311009 && !overwrite) {
                holyMagicShell = 0;
            }
            if (effect.getSourceId() == 3110012 && !overwrite) {
                concentration = 0;
            }
            if (effect.getSourceId() == 4221054 && !overwrite) {
                flip = 0;
            }
            if (statups.containsKey(MapleBuffStat.UnityOfPower) && !overwrite) {
                unityofPower = 0;
            }
            if (statups.containsKey(MapleBuffStat.BlitzShield) && !overwrite) {
                blitzShield = 0;
            }
            if (statups.containsKey(MapleBuffStat.IgnoreTargetDEF) && !overwrite) {
                lightning = 0;
            }
            if (effect.getSourceId() == 15111022 && !overwrite) {
                cancelEffectFromBuffStat(MapleBuffStat.IndieDamR, 15120003);
            }
            if (effect.getSourceId() == 36121007) {
                getMap().removeMistByOwner(this, 36121007);
            }
            if (effect.getSourceId() == 15120003 && !overwrite) {
                cancelEffectFromBuffStat(MapleBuffStat.IndieDamR, 15111022);
            }
            if (effect.getSourceId() == 51001005 && !overwrite) {
                royalStack = 0;
            }
            if (effect.getSourceId() == 80002544 && !getBuffedValue(80002543)) {
                SkillFactory.getSkill(80002543).getEffect(1).applyTo(this, false);
            }
            if (effect.getSourceId() == 14120009 && !overwrite) {
                siphonVitality = 0;
            }
            if (statups.containsKey(MapleBuffStat.AdrenalinBoost)) {
                combo = 500;
            }

            if (effect.getSourceId() == 400011011 && !overwrite) {
                rhoAias = 0;
            }

            if (effect.getSourceId() == 4221016 && !overwrite) {
                shadowerDebuff = 0;
                shadowerDebuffOid = 0;
            }

            if (effect.getSourceId() == 80000268 && !overwrite) {
                FlowofFight = 0;
            }

            if (effect.getSourceId() == 80000514 && !overwrite) {
                LinkofArk = 0;
            }

            if (statups.containsKey(MapleBuffStat.WeaponVariety) && !overwrite) {
                weaponChanges.clear();
            }

            if (statups.containsKey(MapleBuffStat.GlimmeringTime) && !overwrite) {
                cancelEffectFromBuffStat(MapleBuffStat.PoseType, 11101022);
                cancelEffectFromBuffStat(MapleBuffStat.PoseType, 11111022);
                cancelEffect(SkillFactory.getSkill(11121011).getEffect(effect.getLevel()), false, -1);
                cancelEffect(SkillFactory.getSkill(11121012).getEffect(effect.getLevel()), false, -1);
            }

            if (statups.containsKey(MapleBuffStat.Trinity) && !overwrite) {
                trinity = 0;
            }

            if (statups.containsKey(MapleBuffStat.EnergyBurst) && !overwrite) {
                effect.applyTo(this, false);
                energyBurst = 0;
            }

            if (statups.containsKey(MapleBuffStat.AntiMagicShell) && !overwrite) {
                antiMagicShell = 0;
            }

            if (statups.containsKey(MapleBuffStat.EmpiricalKnowledge) && !overwrite) {
                empiricalKnowledge = null;
                empiricalStack = 0;
            }

            if (statups.containsKey(MapleBuffStat.FightJazz) && !overwrite) {
                fightJazz = 0;
            }

            if (statups.containsKey(MapleBuffStat.AdelResonance) && !overwrite) {
                adelResonance = 0;
            }

            if (statups.containsKey(MapleBuffStat.KainLink) && !overwrite) {
                KainLinkCount = 0;
            }

            if (GameConstants.getLinkedSkill(effect.getSourceId()) == 400001024 && !overwrite) {
                if (getCooldownLimit(400001024) == 0) {
                    addCooldown(400001024, System.currentTimeMillis(), 240000);
                    client.getSession().writeAndFlush(CField.skillCooldown(400001024, 240000));
                }
            }

            if (effect.getSourceId() == 152111003 && !overwrite) {
                MapleSummon summon = getSummon(152101000);
                if (summon != null) {
                    summon.setEnergy(0);
                    summon.getCrystalSkills().clear();
                    getClient().getSession().writeAndFlush(SummonPacket.transformSummon(summon, 2));

                    getClient().getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
                    getClient().getSession().writeAndFlush(SummonPacket.specialSummon(summon, 3));
                }
            }

            // check if we are still logged in o.o
            client.getSession().writeAndFlush(BuffPacket.cancelBuff(statups, this));
            map.broadcastMessage(this, BuffPacket.cancelForeignBuff(this, statups), false);
            if (effect.isHide()) { //Wow this is so fking hacky...
                map.broadcastMessage(this, CField.spawnPlayerMapobject(this), false);
            }

            if (effect.getSourceId() == 35121013 && !overwrite) { //when siege 2 deactivates, missile re-activates
                SkillFactory.getSkill(35121005).getEffect(getTotalSkillLevel(35121005)).applyTo(this, false);
            }

            if (statups.containsKey(MapleBuffStat.RideVehicle)) { // 대가리 라이딩 안풀리는거 업데이트
                equipChanged();
            }

            if (statups.containsKey(MapleBuffStat.SpectorTransForm)) {
                client.getSession().writeAndFlush(CWvsContext.enableActions(this));
            }

            if (effect.getSourceId() == 20040219) {
                if (!getUseTruthDoor()) {
                    if (getLuminusMorph()) {
                        cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                        setLuminusMorph(false);
                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(this, false);
                        getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(getLuminusMorphUse(), getLuminusMorph()));
                        setUseTruthDoor(false);
                    } else {
                        cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                        setLuminusMorph(true);
                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(this, false);
                        getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(getLuminusMorphUse(), getLuminusMorph()));
                        setUseTruthDoor(false);
                    }
                }
            } else if (effect.getSourceId() == 20040220) {
                if (getLuminusMorph()) {
                    cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                    setLuminusMorph(false);
                    SkillFactory.getSkill(20040217).getEffect(1).applyTo(this, false);
                    getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(getLuminusMorphUse(), getLuminusMorph()));
                    setUseTruthDoor(false);
                } else {
                    cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                    setLuminusMorph(true);
                    SkillFactory.getSkill(20040216).getEffect(1).applyTo(this, false);
                    getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(getLuminusMorphUse(), getLuminusMorph()));
                    setUseTruthDoor(false);
                }
            }
//
//            if (effect.getSourceId() == 400001042) {
//                if (getBuffedValue(MapleBuffStat.BasicStatUp) != null) {
//                    cancelEffectFromBuffStat(MapleBuffStat.BasicStatUp);
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*    public void cancelBuffStats(MapleBuffStat... stat) {
     List<MapleBuffStat> buffStatList = Arrays.asList(stat);
     deregisterBuffStats(buffStatList);
     cancelPlayerBuffs(buffStatList, false);
     } */
    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        if (checkBuffStatValueHolder(stat) != null) {
            cancelEffect(checkBuffStatValueHolder(stat).effect, false, -1, stat, true);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        if (checkBuffStatValueHolder(stat, from) != null && checkBuffStatValueHolder(stat, from).effect.getSourceId() == from) {
            cancelEffect(checkBuffStatValueHolder(stat, from).effect, false, -1, stat, true);
        }
    }

    public void dispel() {
        if (!isHidden()) {
            for (Pair<MapleBuffStat, MapleBuffStatValueHolder> data : effects) {
                MapleBuffStatValueHolder mbsvh = data.right;
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph() && !mbsvh.effect.isMonsterRiding() && !mbsvh.effect.isMechChange()) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            if (mbsvh.right.effect.isSkill() && mbsvh.right.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.right.effect, false, mbsvh.right.startTime);
                break;
            }
        }
    }

    public void dispelSummons() {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            if (mbsvh.right.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.right.effect, false, mbsvh.right.startTime);
            }
        }
    }

    public void dispelBuff(int skillid) {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            if (mbsvh.right.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.right.effect, false, mbsvh.right.startTime);
                break;
            }
        }
    }

    public void cancelAllBuffs_() {
        getEffects().clear();
    }

    public void cancelAllBuffs() {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> data : effects) {
            MapleBuffStatValueHolder mbsvh = data.right;
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void cancelMorphs() {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            switch (mbsvh.right.effect.getSourceId()) {
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                case 61111008: // ?????????源???? ?????筌??????源???? (3??????)
                case 61120008: // ?????????源???? ?????筌??????源???? (4??????)
                case 61121053: //?????????源???? ?硫????????????
                    return; // Since we can't have more than 1, save up on loops
                default:
                    if (mbsvh.right.effect.isMorph()) {
                        cancelEffect(mbsvh.right.effect, false, mbsvh.right.startTime);
                        continue;
                    }
            }
        }
    }

    public int getMorphState() {
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            if (mbsvh.right.effect.isMorph()) {
                return mbsvh.right.effect.getSourceId();
            }
        }
        return -1;
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.statup, mbsvh.cid);
        }
    }

    public void checkBuffDurationRemain() {
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : removeEffects) {
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            long remainDuration = mbsvh.right.localDuration - (System.currentTimeMillis() - mbsvh.right.startTime);
            if (remainDuration < 0 && mbsvh.right.localDuration != 0) {
                statups.put(mbsvh.left, new Pair<>(mbsvh.right.value, mbsvh.right.localDuration));
            }

            if (!statups.isEmpty()) {
                cancelEffect(mbsvh.right.effect, false, -1, statups);
            }
        }

        removeEffects.clear();
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        final List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
        final Map<Pair<Integer, Byte>, Integer> alreadyDone = new HashMap<Pair<Integer, Byte>, Integer>();

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            final Pair<Integer, Byte> key = new Pair<Integer, Byte>(mbsvh.getRight().effect.getSourceId(), mbsvh.getRight().effect.getLevel());
            if (alreadyDone.containsKey(key)) {
                ret.get(alreadyDone.get(key)).statup.put(mbsvh.getLeft(), new Pair<>(mbsvh.getRight().value, mbsvh.getRight().localDuration));
            } else {
                alreadyDone.put(key, ret.size());
                final EnumMap<MapleBuffStat, Pair<Integer, Integer>> list = new EnumMap<>(MapleBuffStat.class);
                list.put(mbsvh.getLeft(), new Pair<>(mbsvh.getRight().value, mbsvh.right.localDuration));
                ret.add(new PlayerBuffValueHolder(mbsvh.getRight().startTime, mbsvh.getRight().effect, list, mbsvh.getRight().localDuration, mbsvh.getRight().cid));
            }
        }
        return ret;
    }

    public void cancelMagicDoor() {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : effects) {
            if (mbsvh.right.effect.isMagicDoor()) {
                cancelEffect(mbsvh.right.effect, false, mbsvh.right.startTime);
                break;
            }
        }
    }

    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    public final void handleOrbgain(int skillid) {
        int orbcount = getBuffedValue(MapleBuffStat.ComboCounter);
        Skill combo = SkillFactory.getSkill(1101013);
        Skill comboSynergy = SkillFactory.getSkill(1110013);
        Skill advcombo = SkillFactory.getSkill(1120003);
        int advComboSkillLevel = getTotalSkillLevel(advcombo);

        int max = 5;
        if (advComboSkillLevel > 0) {
            max = 10;
        }

        max += 1; // default가 1임

        if (orbcount < max) {
//            if (mon != null) {
            if (getTotalSkillLevel(comboSynergy) > 0) {
                //3차스킬 : 콤보 시너지
                if (comboSynergy.getEffect(getTotalSkillLevel(comboSynergy)).makeChanceResult()) {
                    orbcount++;
                }
            } else //2차스킬 : 콤보
            if (combo.getEffect(getTotalSkillLevel(combo)).makeChanceResult()) {
                orbcount++;
            }
            //4차스킬 : 어드밴스드 콤보
            if (max == 11 && orbcount < max) {
                int prop = advcombo.getEffect(advComboSkillLevel).getProp();

                //하이퍼스킬 : 어드밴스드 콤보-보너스 찬스
                if (getSkillLevel(1120044) > 0) {
                    prop += SkillFactory.getSkill(1120044).getEffect(1).getProp();
                }

                if (Randomizer.isSuccess(Math.min(100, prop))) {
                    orbcount++;
                }
            }
//            }

            EnumMap<MapleBuffStat, Pair<Integer, Integer>> stat = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class);
            stat.put(MapleBuffStat.ComboCounter, new Pair<>(orbcount, 0));
            setBuffedValue(MapleBuffStat.ComboCounter, orbcount);

            client.getSession().writeAndFlush(BuffPacket.giveBuff(stat, combo.getEffect(getTotalSkillLevel(combo)), this));
            map.broadcastMessage(this, BuffPacket.giveForeignBuff(this, stat, combo.getEffect(getTotalSkillLevel(combo))), false);
        }
    }

    public void handleOrbconsume(int skillId) {
        Skill combo = SkillFactory.getSkill(1101013);
        int howmany;
        if (getSkillLevel(combo) <= 0) {
            return;
        }
        MapleStatEffect ceffect = getBuffedEffect(MapleBuffStat.ComboCounter);
        if (ceffect == null) {
            return;
        }
        if (skillId == 1121010) {
            howmany = 1;
        } else {
            howmany = SkillFactory.getSkill(skillId).getEffect(getTotalSkillLevel(skillId)).getY();
        }

        EnumMap<MapleBuffStat, Pair<Integer, Integer>> stat = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class);
        stat.put(MapleBuffStat.ComboCounter, new Pair<>(Math.max(1, getBuffedValue(MapleBuffStat.ComboCounter) - howmany), 0));
        setBuffedValue(MapleBuffStat.ComboCounter, Math.max(1, getBuffedValue(MapleBuffStat.ComboCounter) - howmany));

        client.getSession().writeAndFlush(BuffPacket.giveBuff(stat, ceffect, this));
        map.broadcastMessage(this, BuffPacket.giveForeignBuff(this, stat, ceffect), false);
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);
    }

    public void enforceMaxHpMp() {
        Map<MapleStat, Long> statups = new EnumMap<MapleStat, Long>(MapleStat.class);
        if (stats.getMp() > stats.getCurrentMaxMp(this)) {
            stats.setMp(stats.getMp(), this);
            statups.put(MapleStat.MP, Long.valueOf(stats.getMp()));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statups.put(MapleStat.HP, Long.valueOf(stats.getHp()));
        }
        if (statups.size() > 0) {
            client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public final String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }

    public final short getLevel() {
        return level;
    }

    public final int getFame() {
        return fame;
    }

    public final int getFallCounter() {
        return fallcounter;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final void setClient(final MapleClient client) {
        this.client = client;
    }

    public long getExp() {
        return exp;
    }

    public short getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBook(job, 0)]; //default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int i = 0; i < remainingSp.length; i++) {
            if (remainingSp[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public boolean isHidden() {
        return getBuffedValue(9001004);
    }

    boolean dominant = false;

    public boolean isDominant() {
        return dominant;
    }

    public void setDominant(boolean active) {
        dominant = active;
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public byte getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    public byte getSecondSkinColor() {
        return secondSkinColor;
    }

    public void setSecondSkinColor(byte secondSkinColor) {
        this.secondSkinColor = secondSkinColor;
    }

    public short getJob() {
        return job;
    }

    public byte getGender() {
        return gender;
    }

    public byte getSecondGender() {
        return secondgender;
    }

    public int getHair() {
        return hair;
    }

    public int getSecondHair() {
        return secondhair;
    }

    public int getFace() {
        return face;
    }

    public int getSecondFace() {
        return secondface;
    }

    public int getDemonMarking() {
        return demonMarking;
    }

    public void setDemonMarking(int mark) {
        this.demonMarking = mark;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setSecondHair(int secondhair) {
        this.secondhair = secondhair;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setSecondFace(int secondface) {
        this.secondface = secondface;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    public void setRemainingAp(short remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBook(job, 0)] = remainingSp; //default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setSecondGender(byte secondgender) {
        this.secondgender = secondgender;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
        getTrait(MapleTraitType.charm).addLocalExp(famechange);
    }

    public void updateFame() {
        updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        final MapleMap map = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map, map.getPortal(portal), true);
    }

    public void warp(final int Mapid) {
        ChannelServer cserv = getClient().getChannelServer();
        MapleMap target = cserv.getMapFactory().getMap(Mapid);
        changeMap(target, target.getPortal(0));
    }

    public void warpdelay(final int Mapid, final int Delay) {
        Timer.MapTimer.getInstance().schedule(() -> {
            ChannelServer cserv = getClient().getChannelServer();
            MapleMap target = cserv.getMapFactory().getMap(Mapid);
            changeMap(target, target.getPortal(0));
        }, Delay * 1000);
    }

    public void changeMap(int Mapid, final Point pos) {
        MapleMap to = getClient().getChannelServer().getMapFactory().getMap(Mapid);
        changeMapInternal(to, pos, CField.getWarpToMap(to, 0x81, this), null, false);
    }

    public void changeMap(int Mapid, int portalid) {
        MapleMap to = getClient().getChannelServer().getMapFactory().getMap(Mapid);
        MaplePortal pto = to.getPortal(portalid);
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), null, false);
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, CField.getWarpToMap(to, 0x81, this), null, false);
    }

    public void changeMap(final MapleMap to, MaplePortal maplePortal, boolean banish) {
        changeMapInternal(to, to.getPortal(0).getPosition(), CField.getWarpToMap(to, 0, this), to.getPortal(0), banish);
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), null, false);
    }

    public void changeMapPortal(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), pto, false);
    }

    public void changeMapChannel(final MapleMap to, int channel) {
        changeMapChannel(to, to.getPortal(0), channel);
    }

    private void changeMapChannel(final MapleMap to, final MaplePortal pto, int channel) {
        changeChannel(channel);
        changeMap(to, pto);
    }

    private void changeMapInternal(final MapleMap to, final Point pos, byte[] warpPacket, final MaplePortal pto, boolean banish) {
        if (to == null) {
            return;
        }

        final int nowmapid = map.getId();

        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }

        if (client.getPlayer().FullMakerSize > 0) {
            client.getPlayer().FullMakerSize = 0;
            client.getPlayer().getMap().broadcastMessage(CField.StartFullMaker(client.getPlayer().FullMakerSize, 0));
            client.getPlayer().setFullMakerBox((byte) 0);
            client.getPlayer().setStartFullMakerTime(0);
        }

        if (map.getId() == nowmapid) {
            if ((getDeathCount() > 0 || liveCounts() > 0) && !banish && (to.getId() == map.getId())) {
                switch (map.getId()) {
                    case 450004150:
                    case 450004250:
                    case 450004450:
                    case 450004550:
                    case 450004750:
                    case 450004850:
                        int x = map.getId() == 450004150 || map.getId() == 450004450 || map.getId() == 450004750 ? 157 : Randomizer.nextBoolean() ? 316 : 1027;
                        int y = map.getId() == 450004150 || map.getId() == 450004450 || map.getId() == 450004750 ? 48 : Randomizer.nextBoolean() ? -855 : -842;

                        getMap().broadcastMessage(CField.Respawn(getId(), (int) getStat().getHp()));
                        client.getSession().writeAndFlush(CField.onUserTeleport(x, y));
                        break;
                    case 450008150:
                    case 450008250:
                    case 450008350:
                    case 450008750:
                    case 450008850:
                    case 450008950:
                    case 450010500:
                        getTruePosition().x = 0;
                        getMap().broadcastMessage(CField.Respawn(getId(), (int) getStat().getHp()));
                        client.getSession().writeAndFlush(CField.onUserTeleport(0, getTruePosition().y));
                        break;
                    case 410002020:
                    case 410002060:
                        MaplePortal mp = getMap().getPortal("sp");
                        getMap().broadcastMessage(CField.Respawn(getId(), (int) getStat().getHp()));
                        if (mp != null) {
                            getClient().getSession().writeAndFlush(CField.onUserTeleport(mp.getPosition().x, mp.getPosition().y));
                        } else {
                            getClient().getSession().writeAndFlush(CField.onUserTeleport(getPosition().x, getPosition().y));
                        }
                        break;
                    default: {
                        client.getSession().writeAndFlush(warpPacket);
                        break;
                    }
                }
            } else {
                client.getSession().writeAndFlush(warpPacket);
            }

            final boolean shouldChange = client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            final boolean shouldState = map.getId() == to.getId();
            if (shouldChange && shouldState) {
                to.setCheckStates(false);
            }
            if (shouldChange) {
                map.removePlayer(this);
                map = to;

                setPosition(pos);

                Map<MapleStat, Long> updates = new EnumMap<MapleStat, Long>(MapleStat.class);
                if (stats.getMp() == 0) {
                    stats.setMp(stats.getCurrentMaxMp(this), this);
                    updates.put(MapleStat.MP, Long.valueOf(stats.getMp()));
                }

                if (stats.getHp() == 0) {
                    stats.setHp(stats.getCurrentMaxHp(), this);
                    updates.put(MapleStat.HP, Long.valueOf(stats.getHp()));
                }
                if (getMjollnir() > 0 && getSkillLevel(400011131) > 0) {
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(getMjollnir(), 0));
                    MapleStatEffect effect = SkillFactory.getSkill(400011131).getEffect(getSkillLevel(400011131));
                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
                }

                if (!updates.isEmpty()) {
                    client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(updates, this));
                }

                to.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    to.setCheckStates(true);
                }
            }

            if (getDebuffValue(MapleBuffStat.FireBomb) > 0) {//디버프 헤제
                cancelDisease(MapleBuffStat.FireBomb);
            }
            if (getDebuffValue(MapleBuffStat.CapDebuff) > 0) {
                cancelDisease(MapleBuffStat.CapDebuff);
            }
//            if (getParty() == null) {
//                if (getMap().getAllMonster().size() > 0) { //오르골
//                    MapleStatEffect eff = SkillFactory.getSkill(80003023).getEffect(getSkillLevel(80003023));
//                    Map<MapleBuffStat, Pair<Integer, Integer>> statup = new HashMap<>();
//                    statup.put(MapleBuffStat.EventSpecialSkill, new Pair<>(1, 0));
//                    if (getKeyValue(20210113, "orgelonoff") == 0) {
//                        setKeyValue(20210113, "orgelonoff", "1");
//                    }
//                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statup, eff, this));
//                } else {
//                    MapleStatEffect eff = SkillFactory.getSkill(80003023).getEffect(getSkillLevel(80003023));
//                    Map<MapleBuffStat, Pair<Integer, Integer>> statup = new HashMap<>();
//                    statup.put(MapleBuffStat.EventSpecialSkill, new Pair<>(1, 0));
//                    if (getKeyValue(20210113, "orgelonoff") == 1) {
//                        getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(statup, this));
//                        getClient().getSession().writeAndFlush(CWvsContext.showPopupMessage("몬스터가 없거나 르네의 마법오르골을 사용할 수 없는 곳입니다."));
//                        setKeyValue(20210113, "orgelonoff", "0");
//                    }
//                }
//
//            } else {
//                MapleStatEffect eff = SkillFactory.getSkill(80003023).getEffect(getSkillLevel(80003023));
//                Map<MapleBuffStat, Pair<Integer, Integer>> statup = new HashMap<>();
//                statup.put(MapleBuffStat.EventSpecialSkill, new Pair<>(1, 0));
//                if (getKeyValue(20210113, "orgelonoff") == 1) {
//                    getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(statup, this));
//                    getClient().getSession().writeAndFlush(CWvsContext.showPopupMessage("파티를 하여 마법오르골을 사용할 수 없습니다."));
//                    setKeyValue(20210113, "orgelonoff", "0");
//                }
//                gainDonationSkills();
//            }

            int lock = isLockNeoCore() ? 1 : 0;
            getClient().getSession().writeAndFlush(SLFCGPacket.StarDustUI("UI/UIWindowEvent.img/2020neoCoin", 0, 1));
            updateInfoQuest(501215, "point=" + getKeyValue(501215, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";week=0;total=0;today=" + getKeyValue(501215, "today") + ";lock=" + lock + ""); //네오 코어

        }
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            final MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " has denied your request.");
                chr.setChallenge(0);
            }
            dropMessage(6, "Denied the challenge.");
            challenge = 0;
        }
    }

    public void leaveMap(MapleMap map) {

        for (MapleMonster mons : controlled) {
            if (mons != null) {
                mons.setController(null);
                mons.setControllerHasAggro(false);
                map.updateMonsterController(mons);
            }
        }
        controlled.clear();
        visibleMapObjects.clear();

        if (chair != 0) {
            chair = 0;
        }
        clearLinkMid();
        cancelChallenge();
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
        finishGiftShowX3(map);
    }

    public void SkillMasterJob(int jobid) {

        MapleData data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(StringUtil.getLeftPaddedStr("" + jobid, '0', 3) + ".img");
        dropMessage(5, "새로운 직업의 모든 스킬을 배웠습니다.");
        Map<Skill, SkillEntry> updates = new HashMap<>();

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
                            if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                updates.put(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), new SkillEntry(maxLevel, maxLevel, -1));
                            }
                        }
                    }
                }
            }
        }

        if (GameConstants.isZero(jobid)) {
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
                                    if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                        updates.put(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), new SkillEntry(maxLevel, maxLevel, -1));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (GameConstants.isKOC(jobid) && getLevel() >= 100) {
            updates.put(SkillFactory.getSkill(11121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(12121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(13121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(14121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(15121000), new SkillEntry(30, (byte) 30, -1));
        }
        if (GameConstants.isMichael(jobid) && getLevel() >= 100) {
            updates.put(SkillFactory.getSkill(51121005), new SkillEntry(30, (byte) 30, -1));
        }
        changeSkillsLevel(updates);
    }

    public void SkillMasterByJob() {

        MapleData data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(StringUtil.getLeftPaddedStr("" + getJob(), '0', 3) + ".img");
        dropMessage(5, "새로운 직업의 모든 스킬을 배웠습니다.");
        Map<Skill, SkillEntry> updates = new HashMap<>();

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
                            if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                updates.put(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), new SkillEntry(maxLevel, maxLevel, -1));
                            }
                        }
                    }
                }
            }
        }
        if (GameConstants.isZero(getJob())) {
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
                                    if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                        updates.put(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), new SkillEntry(maxLevel, maxLevel, -1));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (GameConstants.isKOC(getJob()) && getLevel() >= 100) {
            updates.put(SkillFactory.getSkill(11121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(12121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(13121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(14121000), new SkillEntry(30, (byte) 30, -1));
            updates.put(SkillFactory.getSkill(15121000), new SkillEntry(30, (byte) 30, -1));
        }
        if (GameConstants.isMichael(getJob()) && getLevel() >= 100) {
            updates.put(SkillFactory.getSkill(51121005), new SkillEntry(30, (byte) 30, -1));
        }
        changeSkillsLevel(updates);
    }

    public void changeJob(int newJob) {
        if (MapleCarnivalChallenge.getJobNameById(newJob).equals("")) {
            System.out.println(name + " 에서 비정상적인 전직 시도 감지 : " + newJob);
            return;
        }

        try {
            cancelEffectFromBuffStat(MapleBuffStat.ShadowPartner);
            this.job = (short) newJob;
            updateSingleStat(MapleStat.JOB, newJob);
            if (!GameConstants.isBeginnerJob(newJob)) {
                if (GameConstants.isEvan(newJob) || GameConstants.isResist(newJob) || GameConstants.isMercedes(newJob)) {
                    int changeSp = (newJob == 2200 || newJob == 2210 || newJob == 2211 || newJob == 2213 ? 3 : 5);
                    if (GameConstants.isResist(job) && newJob != 3100 && newJob != 3200 && newJob != 3300 && newJob != 3500) {
                        changeSp = 3;
                    }
                    remainingSp[GameConstants.getSkillBook(newJob, 0)] += changeSp;
                    client.getSession().writeAndFlush(InfoPacket.getSPMsg((byte) changeSp, (short) newJob));
                } else if (GameConstants.isPhantom(job)) {
                    if (job == 2412) { //Job advancement to Phantom 4th Job, Judgement Draw II replaces Judgement Draw I.
                        final Skill skil1 = SkillFactory.getSkill(20031209);
                        changeSingleSkillLevel(skil1, 0, (byte) 0);

                        final Skill skil2 = SkillFactory.getSkill(20031210);
                        changeSingleSkillLevel(skil2, 1, (byte) skil2.getMaxLevel());
                    }
                    client.getSession().writeAndFlush(CField.updateCardStack(false, 0));
                } else {
                    remainingSp[GameConstants.getSkillBook(newJob, 0)]++;
                    if (newJob % 10 >= 2) {
                        remainingSp[GameConstants.getSkillBook(newJob, 0)] += 2;
                    }
                }
                if (newJob % 10 >= 1 && level >= 60) { //3rd job or higher. lucky for evans who get 80, 100, 120, 160 ap...
                    remainingAp += 5;
                    updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
                }
                if (!isGM()) {
                    resetStatsByJob(true);
                    if (!GameConstants.isEvan(newJob)) {
                        if (getLevel() > (newJob == 200 ? 8 : 10) && newJob % 100 == 0 && (newJob % 1000) / 100 > 0) { //first job
                            remainingSp[GameConstants.getSkillBook(newJob, 0)] += 3 * (getLevel() - (newJob == 200 ? 8 : 10));
                        }
                    }
                }
                updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
            }

            if (GameConstants.isDemonAvenger(job)) {
                changeSkillLevel(30010230, (byte) 1, (byte) 1);
                changeSkillLevel(30010231, (byte) 1, (byte) 1);
                changeSkillLevel(30010232, (byte) 1, (byte) 1);
                changeSkillLevel(30010242, (byte) 1, (byte) 1);
            }

            if (GameConstants.isDemonSlayer(job)) {
                changeSkillLevel(30010111, (byte) 1, (byte) 1);
            }

            for (MapleUnion union : getUnions().getUnions()) {
                if (union.getCharid() == id) {
                    union.setJob(newJob);
                }
            }

            long maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            switch (job) {
                case 100: // Warrior
                case 1100: // Soul Master
                case 2100: // Aran
                case 3200:
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3100:
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3110:
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 200: // Magician
                case 2200: //evan
                case 2210: //evan
                    maxmp += Randomizer.rand(100, 150);
                    break;
                case 300: // Bowman
                case 400: // Thief
                case 500: // Pirate
                case 2300:
                case 3300:
                case 3500:
                    maxhp += Randomizer.rand(100, 150);
                    maxmp += Randomizer.rand(25, 50);
                    break;
                case 110: // Fighter
                case 120: // Page
                case 130: // Spearman
                case 1110: // Soul Master
                case 2110: // Aran
                case 3210:
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 210: // FP
                case 220: // IL
                case 230: // Cleric
                    maxmp += Randomizer.rand(400, 450);
                    break;
                case 310: // Bowman
                case 320: // CROSSBOWman
                case 410: // Assasin
                case 420: // Bandit
                case 430: // Semi Dualer
                case 510:
                case 520:
                case 530:
                case 2310:
                case 1310: // Wind Breaker
                case 1410: // Night Walker
                case 3310:
                case 3510:
                    maxhp += Randomizer.rand(200, 250);
                    maxhp += Randomizer.rand(150, 200);
                    break;
                case 900: // GM
                case 800: // Manager
                    maxhp += 500000;
                    maxmp += 500000;
                    break;
            }
            if (maxhp >= 500000) {
                maxhp = 500000;
            }
            if (maxmp >= 500000) {
                maxmp = 500000;
            }
            if (GameConstants.isDemonSlayer(job)) {
                maxmp = GameConstants.getMPByJob(this);
            }
            if (job == 410) {
                changeSingleSkillLevel(SkillFactory.getSkill(4101011), 1, (byte) 1);
            }
            if (job == 15510) {
                changeSingleSkillLevel(SkillFactory.getSkill(155101006), 1, (byte) 1);
            }
            stats.setInfo(maxhp, maxmp, maxhp, maxmp);
            characterCard.recalcLocalStats(this);
            stats.recalcLocalStats(this);
            Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);
            statup.put(MapleStat.MAXHP, stats.getCurrentMaxHp());
            statup.put(MapleStat.MAXMP, stats.getCurrentMaxMp(this));
            statup.put(MapleStat.HP, stats.getCurrentMaxHp());
            statup.put(MapleStat.MP, stats.getCurrentMaxMp(this));
            client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, this));
            map.broadcastMessage(this, EffectPacket.showNormalEffect(this, 14, false), false);
            silentPartyUpdate();
            guildUpdate();
            if (dragon != null) {
                map.broadcastMessage(CField.removeDragon(this.id));
                dragon = null;
            }
            if (newJob >= 2200 && newJob <= 2218) { //make new
                if (getBuffedValue(MapleBuffStat.RideVehicle) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
                }
                makeDragon();
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void changeJob4310086(int newJob) {
        if (MapleCarnivalChallenge.getJobNameById(newJob).equals("")) {
            System.out.println(name + " 에서 비정상적인 전직 시도 감지 : " + newJob);
            return;
        }

        try {
            cancelEffectFromBuffStat(MapleBuffStat.ShadowPartner);
            this.job = (short) newJob;
            updateSingleStat(MapleStat.JOB, newJob);
            if (!GameConstants.isBeginnerJob(newJob)) {
                if (GameConstants.isEvan(newJob) || GameConstants.isResist(newJob) || GameConstants.isMercedes(newJob)) {
                    /*int changeSp = (newJob == 2200 || newJob == 2210 || newJob == 2211 || newJob == 2213 ? 3 : 5);
                     if (GameConstants.isResist(job) && newJob != 3100 && newJob != 3200 && newJob != 3300 && newJob != 3500) {
                     changeSp = 3;
                     }
                     remainingSp[GameConstants.getSkillBook(newJob, 0)] += changeSp;
                     client.getSession().writeAndFlush(InfoPacket.getSPMsg((byte) changeSp, (short) newJob));*/
                } else if (GameConstants.isPhantom(job)) {
                    if (job == 2412) { //Job advancement to Phantom 4th Job, Judgement Draw II replaces Judgement Draw I.
                        final Skill skil1 = SkillFactory.getSkill(20031209);
                        changeSingleSkillLevel(skil1, 0, (byte) 0);

                        final Skill skil2 = SkillFactory.getSkill(20031210);
                        changeSingleSkillLevel(skil2, 1, (byte) skil2.getMaxLevel());
                    }
                    // client.getSession().writeAndFlush(CField.updateCardStack(0));
                } else {
                    //remainingSp[GameConstants.getSkillBook(newJob, 0)]++;
                    if (newJob % 10 >= 2) {
                        //remainingSp[GameConstants.getSkillBook(newJob, 0)] += 2;
                    }
                }
                if (newJob % 10 >= 1 && level >= 60) { //3rd job or higher. lucky for evans who get 80, 100, 120, 160 ap...
                    /*remainingAp += 5;
                     updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);*/
                }
                if (!isGM()) {
                    //resetStatsByJob(true);
                    if (!GameConstants.isEvan(newJob)) {
                        if (getLevel() > (newJob == 200 ? 8 : 10) && newJob % 100 == 0 && (newJob % 1000) / 100 > 0) { //first job
                            //remainingSp[GameConstants.getSkillBook(newJob, 0)] += 3 * (getLevel() - (newJob == 200 ? 8 : 10));
                        }
                    }
                }
                //updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
            }
            for (MapleUnion union : getClient().getUnions()) {
                if (union.getCharid() == id) {
                    union.setJob(newJob);
                }
            }

            long maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            if (job == 410) {
                changeSingleSkillLevel(SkillFactory.getSkill(4101011), 1, (byte) 1);
            }
            if (job == 15510) {
                changeSingleSkillLevel(SkillFactory.getSkill(155101006), 1, (byte) 1);
            }
            stats.setInfo(maxhp, maxmp, maxhp, maxmp);
            Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);
            statup.put(MapleStat.MAXHP, Long.valueOf(maxhp));
            statup.put(MapleStat.MAXMP, Long.valueOf(maxmp));
            statup.put(MapleStat.HP, Long.valueOf(maxhp));
            statup.put(MapleStat.MP, Long.valueOf(maxmp));
            characterCard.recalcLocalStats(this);
            stats.recalcLocalStats(this);
            client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, this));
            map.broadcastMessage(this, EffectPacket.showNormalEffect(this, 14, false), false);
            silentPartyUpdate();
            guildUpdate();
            if (dragon != null) {
                map.broadcastMessage(CField.removeDragon(this.id));
                dragon = null;
            }
            baseSkills();
            if (newJob >= 2200 && newJob <= 2218) { //make new
                if (getBuffedValue(MapleBuffStat.RideVehicle) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
                }
                if (getBuffedValue(MapleBuffStat.RideVehicleExpire) != null) {
                    cancelEffectFromBuffStat(MapleBuffStat.RideVehicleExpire);
                }
                makeDragon();
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void setSkillBuffTest(int buf) {
        this.bufftest = buf;
    }

    public void baseSkills() {
    }

    public int getSkillBuffTest() {
        return bufftest;
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
        map.broadcastMessage(CField.spawnDragon(dragon));
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public short getAp() {
        return remainingAp;
    }

    public void gainAp(short ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBook(job, 0)] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.getSession().writeAndFlush(InfoPacket.getSPMsg((byte) sp, (short) job));
    }

    public void gainSP(int sp, final int skillbook) {
        this.remainingSp[skillbook] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.getSession().writeAndFlush(InfoPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
    }

    public void resetAPSP() {
        resetSP(0);
        gainAp((short) -this.remainingAp);
    }

    public List<Integer> getProfessions() {
        List<Integer> prof = new ArrayList<Integer>();
        for (int i = 9200; i <= 9204; i++) {
            if (getProfessionLevel(id * 10000) > 0) {
                prof.add(i);
            }
        }
        return prof;
    }

    public byte getProfessionLevel(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (byte) ((ret >>> 24) & 0xFF); //the last byte
    }

    public short getProfessionExp(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (short) (ret & 0xFFFF); //the first two byte
    }

    public boolean addProfessionExp(int id, int expGain) {
        int ret = getProfessionLevel(id);
        if (ret <= 0 || ret >= 10) {
            return false;
        }
        int newExp = getProfessionExp(id) + expGain;
        if (newExp >= GameConstants.getProfessionEXP(ret)) {
            //gain level
            changeProfessionLevelExp(id, ret + 1, newExp - GameConstants.getProfessionEXP(ret));
            int traitGain = (int) Math.pow(2, ret + 1);
            switch (id) {
                case 92000000:
                    traits.get(MapleTraitType.sense).addExp(traitGain, this);
                    break;
                case 92010000:
                    traits.get(MapleTraitType.will).addExp(traitGain, this);
                    break;
                case 92020000:
                case 92030000:
                case 92040000:
                    traits.get(MapleTraitType.craft).addExp(traitGain, this);
                    break;
            }
            return true;
        } else {
            changeProfessionLevelExp(id, ret, newExp);
            return false;
        }
    }

    public void changeProfessionLevelExp(int id, int level, int exp) {
        changeSingleSkillLevel(SkillFactory.getSkill(id), ((level & 0xFF) << 24) + (exp & 0xFFFF), (byte) 10);
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration)) { // no loop, only 1
            list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.getSession().writeAndFlush(CWvsContext.updateSkills(skills));
        reUpdateStat(hasRecovery, recalculate);
    }

    public void changeSkillsLevel(final Map<Skill, SkillEntry> ss) {
        if (ss.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        for (final Entry<Skill, SkillEntry> data : ss.entrySet()) {
            if (changeSkillData(data.getKey(), data.getValue().skillevel, data.getValue().masterlevel, data.getValue().expiration)) {
                list.put(data.getKey(), data.getValue());
                if (GameConstants.isRecoveryIncSkill(data.getKey().getId())) {
                    hasRecovery = true;
                }
                if (data.getKey().getId() < 90000000 || data.getKey().isVMatrix()) {
                    recalculate = true;
                }
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.getSession().writeAndFlush(CWvsContext.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    private void reUpdateStat(boolean hasRecovery, boolean recalculate) {
        changed_skills = true;
        if (hasRecovery) {
            stats.relocHeal(this);
        }
        if (recalculate) {
            stats.recalcLocalStats(this);
        }
    }

    public boolean changeSkillData(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null) {// || (!GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId()))) {
            return false;
        }
        if (newLevel < newMasterlevel) {
            newMasterlevel = (byte) newLevel;
        }
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return false; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
        }
        return true;
    }

    public void changeSkillLevel(final int skill, byte newLevel, byte newMasterLevel) {
        changeSkillLevel(SkillFactory.getSkill(skill), newLevel, newMasterLevel);
    }

    public void changeSkillLevel(Skill skill, byte newLevel, byte newMasterlevel) {
        changeSkillLevel_Skip(skill, newLevel, newMasterlevel);
    }

    public void changeSkillLevel_Inner(final int skill, byte newLevel, byte newMasterLevel) {
        changeSkillLevel_Inner(SkillFactory.getSkill(skill), newLevel, newMasterLevel);
    }

    public void changeSkillLevel_Inner(Skill skil, int skilLevel, byte masterLevel) {
        final Map<Skill, SkillEntry> enry = new HashMap<>(1);
        enry.put(skil, new SkillEntry(skilLevel, masterLevel, -1L));
        changeSkillLevel_Skip(enry, false);
    }

    public void changeSkillLevel_Skip(Skill skil, int skilLevel, byte masterLevel) {
        final Map<Skill, SkillEntry> enry = new HashMap<>(1);
        enry.put(skil, new SkillEntry(skilLevel, masterLevel, -1L));
        changeSkillLevel_Skip(enry, true);
    }

    public void changeSkillLevel_Skip(final Map<Skill, SkillEntry> skill, final boolean write) { // only used for temporary skills (not saved into db)
        if (skill.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> newL = new HashMap<>();
        for (final Entry<Skill, SkillEntry> z : skill.entrySet()) {
            if (z.getKey() == null) {
                continue;
            }
            newL.put(z.getKey(), z.getValue());
            if (z.getValue().skillevel <= 0 && z.getValue().masterlevel == 0) {
                if (skills.containsKey(z.getKey())) {
                    skills.remove(z.getKey());
                } else {
                    continue;
                }
            } else {
                skills.put(z.getKey(), z.getValue());
            }
        }
        if (write && !newL.isEmpty()) {
            client.getSession().writeAndFlush(CWvsContext.updateSkills(newL));
        }
    }

    public void playerDead() {
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        dispelSummons();
        checkFollow();
        if (getBuffedValue(5321054)) {
            cancelEffect(getBuffedEffect(5321054), false, -1);
        }
        if (getBuffedValue(2001002)) {
            cancelEffect(getBuffedEffect(2001002), false, -1);
        }
        if (getBuffedValue(2311003)) {
            cancelEffect(getBuffedEffect(2311003), false, -1);
        }
        if (getBuffedValue(5121009)) {
            cancelEffect(getBuffedEffect(5121009), false, -1);
        }
        dotHP = 0;
        lastDOTTime = 0;
        BHGCCount = 0;
        HowlingGaleCount = 0;
        WildGrenadierCount = 0;
        useBuffFreezer = false;

        MapleMonster will = map.getMonsterById(8880300);
        if (will == null) {
            will = map.getMonsterById(8880340);
            if (will == null) {
                will = map.getMonsterById(8880301);
                if (will == null) {
                    will = map.getMonsterById(8880341);
                }
            }
        }

        if (will != null) {
            if (will.isUseSpecialSkill()) {
                will.setUseSpecialSkill(false);
            }
        }

        if (getMapId() == 863010240 || getMapId() == 863010330 || getMapId() == 863010430 || getMapId() == 863010600) {
            MapleCharacter pchr = getClient().getChannelServer().getPlayerStorage().getCharacterById(getParty().getLeader().getId());
            if (pchr == null) {
                dropMessage(5, "파티의 상태가 변경되어 골럭스 원정대가 해체됩니다.");
                setKeyValue(200106, "golrux_in", "0");
            } else {

                if (pchr.getKeyValue(200106, "golrux_in") == 1) {
                    if (pchr.getKeyValue(200106, "golrux_dc") <= 0) {
                        //데카아웃
                        pchr.setKeyValue(200106, "golrux_in", "0");
                        warp(ServerConstants.WarpMap);
                        if (pchr.getId() != getId()) {
                            dropMessage(5, "데스카운트가 모두 소모되어 골럭스 원정대가 해체됩니다.");
                            pchr.dropMessage(5, "데스카운트가 모두 소모되어 골럭스 원정대가 해체됩니다.");
                        } else {
                            dropMessage(5, "데스카운트가 모두 소모되어 골럭스 원정대가 해체됩니다.");
                        }
                    } else {
                        if (pchr.getId() != getId()) {
                            pchr.setKeyValue(200106, "golrux_dc", String.valueOf((getKeyValue(200106, "golrux_dc") - 1)));
                            pchr.dropMessage(5, "데스카운트가 " + getKeyValue(200106, "golrux_dc") + "만큼 남았습니다.");
                            dropMessage(5, "데스카운트가 " + getKeyValue(200106, "golrux_dc") + "만큼 남았습니다.");
                        } else {
                            setKeyValue(200106, "golrux_dc", String.valueOf((getKeyValue(200106, "golrux_dc") - 1)));
                            dropMessage(5, "데스카운트가 " + getKeyValue(200106, "golrux_dc") + "만큼 남았습니다.");
                        }
                    }
                }
            }
        }

        if (!inPVP()) {
            int charms = getItemQuantity(5130000, false);
            if (charms > 0) {
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);

                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.getSession().writeAndFlush(EffectPacket.showCharmEffect(this, 0, charms > 0 ? 1 : 0, true, ""));
            }
            if (deathcount > 0) {
                setDeathCount((byte) (getDeathCount() - 1));
                client.getSession().writeAndFlush(CField.sendDeathCountRespawn());
            } else {
                for (int i = 0; i < deathCounts.length; ++i) {
                    if (deathCounts[i] != 2) {
                        deathCounts[i] = 2;
                        break;
                    }
                }

                if (liveCounts() > 0) {
                    client.getSession().writeAndFlush(CField.sendDeathCountRespawn());
                }
            }
            if (deadEffect) {
                setDeadEffect(false);
                client.getSession().writeAndFlush(CField.setPlayerDead());
            }

            /* try {
                int buffFreezer = getItemQuantity(5133000, false);

                if (buffFreezer > 0) {
                    client.getSession().writeAndFlush(CField.OpenDeadUI(this, 3));
                } else {
                    client.getSession().writeAndFlush(CField.OpenDeadUI(this, 1));
                }
            } finally {
                client.getSession().writeAndFlush(CField.OpenDeadUI(this, 1));
            }*/
            int buffFreezer = getItemQuantity(5133000, false);

            if (buffFreezer > 0) {
                client.getSession().writeAndFlush(CField.OpenDeadUI(this, 3));
            } else {
                client.getSession().writeAndFlush(CField.OpenDeadUI(this, 1));
            }

        }
        if (!stats.checkEquipDurabilitys(this, -100)) { //i guess this is how it works ?
            dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
        } //lol
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().getSession().writeAndFlush(CField.updatePartyMemberHP(getId(), (int) stats.getHp(), (int) stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.getSession().writeAndFlush(CField.updatePartyMemberHP(other.getId(), (int) other.getStat().getHp(), (int) other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void healHP(long delta) {
        addHP(delta);
        client.getSession().writeAndFlush(EffectPacket.showHealEffect(this, (int) delta, true));
        getMap().broadcastMessage(this, EffectPacket.showHealEffect(this, (int) delta, false), false);
    }

    public void healMP(long delta) {
        addMP(delta);
        client.getSession().writeAndFlush(EffectPacket.showHealEffect(this, (int) delta, true));
        getMap().broadcastMessage(this, EffectPacket.showHealEffect(this, (int) delta, false), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @param delta
     */
    public void addHP(long delta) {
        if (getBuffedEffect(MapleBuffStat.NotDamaged) == null && stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @param delta
     */
    public void addMP(long delta) {
        addMP(delta, false);
    }

    public void reloadChar() {
        getClient().getSession().writeAndFlush(CField.getCharInfo(this));
        getMap().removePlayer(this);
        getMap().addPlayer(this);
    }

    public void addMP(long delta, boolean ignore) {
        if ((delta < 0 && GameConstants.isDemonSlayer(getJob())) || !GameConstants.isDemonSlayer(getJob()) || ignore) {
            if (stats.setMp(stats.getMp() + delta, this)) {
                updateSingleStat(MapleStat.MP, stats.getMp());
            }
        }
    }

    public void addMPHP(long hpDiff, long mpDiff) {
        Map<MapleStat, Long> statups = new EnumMap<MapleStat, Long>(MapleStat.class);

        if (stats.setHp(stats.getHp() + hpDiff, this)) {
            statups.put(MapleStat.HP, Long.valueOf(stats.getHp()));
        }
        if ((mpDiff < 0 && GameConstants.isDemonSlayer(getJob())) || !GameConstants.isDemonSlayer(getJob())) {
            if (stats.setMp(stats.getMp() + mpDiff, this)) {
                statups.put(MapleStat.MP, Long.valueOf(stats.getMp()));
            }
        }
        if (statups.size() > 0) {
            client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public void updateZeroStats() {
        client.getSession().writeAndFlush(CWvsContext.updateZeroSecondStats(this));
    }

    public void updateAngelicStats() {
        client.getSession().writeAndFlush(CWvsContext.updateAngelicBusterInfo(this));
    }

    public void updateSingleStat(MapleStat stat, long newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, long newval, boolean itemReaction) {
        Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);
        statup.put(stat, newval);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, itemReaction, this));
    }

    public void gainExp(long total, final boolean show, final boolean inChat, final boolean white) {
        try {
            long prevexp = getExp();
            long needed = getNeededExp();
            if (total > 0) {
                stats.checkEquipLevels(this, (int) total); //gms like
            }
            if (level >= GameConstants.MaxLevel) {
                setExp(0);
                total = 0;
            } else if (level == GameConstants.MaxLevel) {
                setExp(0);
                total = 0;
            } else {
                boolean leveled = false;
                long tot = exp + total;
                if (tot >= needed) {
                    exp += total;
                    while ((exp >= needed) && level < GameConstants.MaxLevel) {
                        levelUp();
                        if (getSkillLevel(80000545) > 0 && level >= 10 && level < 240 && !leveled) {
                            levelUp();
                            levelUp();
                            exp = 0;
                        }
                        leveled = true;
                        needed = getNeededExp();
                        if (level >= GameConstants.MaxLevel) {
                            setExp(0);
                        }/* else
                         if (exp >= needed) {
                         setExp(needed - 1);
                         }*/

                    }
                } else {
                    exp += total;
                }
            }
            if (total != 0) {
                if (exp < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(needed);
                    } else if (total < 0) {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.EXP, getExp());
                if (show) { // still show the expgain even if it's not there
                    client.getSession().writeAndFlush(InfoPacket.GainEXP_Others(total, inChat, white));
                }
            }
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }
    public void gainExpMonster(final long gain, final boolean show, final boolean white, Map<MonsterStatus, MonsterStatusEffect> stati) {
        long total = gain;
        int flag = 0;

        int eventBonusExp = (int) (((double) ServerConstants.EventBonusExp / 100) * gain);
        int weddingExp = (int) (((double) ServerConstants.WeddingExp / 100) * gain);
        int partyExp = (int) (((double) ServerConstants.PartyExp / 100) * gain);

        int itemEquipExp = 0;

        if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -17) != null) {
            if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -17).getItemId() == 1122017) {
                itemEquipExp += (int) (gain * 0.1);
            }
        } else if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -31) != null) {
            if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -31).getItemId() == 1122017) {
                itemEquipExp += (int) (gain * 0.1);
            }
        }

        /*if (this.hasEquipped(1032024)) { //투명 귀고리
            itemEquipExp += (long) (gain * 0.1);
        } else*/ if (this.hasEquipped(1032206)) { //경험치 귀고리 1단계
            itemEquipExp += (int) (gain * 0.2);
        } else if (this.hasEquipped(1032207)) { //경험치 귀고리 2단계
            itemEquipExp += (int) (gain * 0.3);
        } else if (this.hasEquipped(1032208)) { //경험치 귀고리 3단계
            itemEquipExp += (int) (gain * 0.4);
        } else if (this.hasEquipped(1032209)) { //경험치 귀고리 4단계
            itemEquipExp += (int) (gain * 0.5);
        }

        int pcExp = (int) (((double) ServerConstants.PcRoomExp / 100) * gain);
        int rainbowWeekExp = (int) (((double) ServerConstants.RainbowWeekExp / 100) * gain);
        int boomupExp = (int) (((double) ServerConstants.BoomupExp / 100) * gain);
        int portionExp = (int) (((double) ServerConstants.PortionExp / 100) * gain);

        int skillExp = 0;

        if (getSkillLevel(20021110) > 0) {
            skillExp += gain * SkillFactory.getSkill(20021110).getEffect(getSkillLevel(20021110)).getEXPRate() / 100;
        }

        if (getStat().expBuffZero > 0) {
            skillExp += gain * getStat().expBuffZero / 100;
        }

        if (getStat().expBuffUnion > 0) {
            skillExp += gain * getStat().expBuffUnion / 100;
        }

        if (getSkillLevel(80002935) > 0) {
            skillExp += gain * SkillFactory.getSkill(80002935).getEffect(getSkillLevel(80002935)).getExpRPerM() / 100;
        }

        if (getSkillLevel(80001040) > 0) {
            skillExp += gain * SkillFactory.getSkill(80001040).getEffect(getSkillLevel(80001040)).getEXPRate() / 100;
        }

        if (getSkillLevel(80000420) > 0) {
            skillExp += gain * SkillFactory.getSkill(80002935).getEffect(getSkillLevel(80002935)).getExpRPerM() / 10000;
        }

        if (getSkillLevel(91000001) > 0) {
            skillExp += gain * SkillFactory.getSkill(91000001).getEffect(getSkillLevel(91000001)).getEXPRate() / 100;
        }

        if (getSkillLevel(131000016) > 0) {
            skillExp += gain * SkillFactory.getSkill(131000016).getEffect(getSkillLevel(131000016)).getEXPRate() / 100;
        }

        if (getBuffedEffect(MapleBuffStat.DiceRoll) != null) {
            MapleStatEffect effect = getBuffedEffect(MapleBuffStat.DiceRoll);

            int doubledice, dice, thirddice, value;
            if (getDice() >= 100) {
                thirddice = (getDice() / 100);
                doubledice = (getDice() - (thirddice * 100)) / 10;
                dice = getDice() - (getDice() / 10 * 10);
            } else {
                thirddice = 1;
                doubledice = (getDice() / 10);
                dice = (getDice() - (doubledice * 10));
            }

            if (dice == 6 || doubledice == 6 || thirddice == 6) {
                if (dice == 6 && doubledice == 6 && thirddice == 6) {
                    value = (effect.getEXPRate() + 15); //ok
                } else if ((dice == 6 && doubledice == 6) || (dice == 6 && thirddice == 6) || (thirddice == 6 && doubledice == 6)) {
                    value = (effect.getEXPRate() + 10); //ok
                } else {
                    value = (effect.getEXPRate()); //ok
                }
            } else {
                value = (0);
            }

            skillExp += gain * value / 100;
        }

        if (getKeyValue(19019, "id") >= 1) {
            int headId = (int) getKeyValue(19019, "id");
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            if (ii.getItemInformation(headId) != null) {
                int nickSkill = ii.getItemInformation(headId).nickSkill;
                if (SkillFactory.getSkill(nickSkill) != null) {
                    MapleStatEffect nickEffect = SkillFactory.getSkill(nickSkill).getEffect(1);
                    skillExp += gain * nickEffect.getIndieExp() / 100;
                }
            }
        }

        if (GameConstants.partyExpMapList.contains(this.getMapId())) {
            if (this.getParty() != null && this.getParty().getMembers().size() >= 2) {
                skillExp += gain * ((this.getParty().getMembers().size() - 1) / 2.0);
            }
        }

        if (stati != null) {
            final MonsterStatusEffect ms = stati.get(MonsterStatus.MS_Showdown);
            if (ms != null) {
                skillExp += (int) (gain * (ms.getValue() / 100.0));
            }
        }

        int buffExp = 0;

        Iterator<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effect = effects.iterator();
        while (effect.hasNext()) {
            Pair<MapleBuffStat, MapleBuffStatValueHolder> buff = effect.next();
            if (buff.left == MapleBuffStat.IndieExp) {
                buffExp += gain * buff.right.value / 100;
            }
        }

        int restExp = (int) (((double) ServerConstants.RestExp / 100) * gain);
        int itemExp = (int) (((double) ServerConstants.ItemExp / 100) * gain);
        int valueExp = (int) (((double) ServerConstants.ValueExp / 100) * gain);

        buffExp += (int) (((double) getStat().expBuff / 100) * gain);

        int bloodExp = 0;

        if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -12) != null) {
            if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -12).getItemId() == 1114000) {
                bloodExp = (int) (gain * 0.1);
            }
        } else if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -13) != null) {
            if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -13).getItemId() == 1114000) {
                bloodExp = (int) (gain * 0.1);
            }
        } else if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -15) != null) {
            if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -15).getItemId() == 1114000) {
                bloodExp = (int) (gain * 0.1);
            }
        } else if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -16) != null) {
            if (getInventory(MapleInventoryType.EQUIPPED).getItem((short) -16).getItemId() == 1114000) {
                bloodExp = (int) (gain * 0.1);
            }
        }
        final Integer holySymbol = getBuffedValue(MapleBuffStat.HolySymbol);
        if (holySymbol != null) {
            buffExp += gain * (holySymbol.doubleValue() / 100.0);
            total += buffExp;
        }
        int iceExp = (int) (((double) ServerConstants.IceExp / 100) * gain);

        Pair<Long, Integer> burningExp = new Pair<Long, Integer>((long) (gain * getMap().getBurning() / 10), getMap().getBurning() * 10);

        int hpLiskExp = (int) (((double) ServerConstants.HpLiskExp / 100) * gain);
        int fieldBonusExp = (int) (((double) ServerConstants.FieldBonusExp / 100) * gain);
        int eventBonusExp2 = (int) (((double) ServerConstants.EventBonusExp / 100) * gain);
        int fieldBonusExp2 = (int) (((double) ServerConstants.FieldBonusExp2 / 100) * gain);

        if (eventBonusExp > 0) {
            flag += 1;
            total += eventBonusExp;
        }

        if (weddingExp > 0) {
            flag += 0x20;
            total += weddingExp;
        }

        if (partyExp > 0) {
            flag += 0x10;
            total += partyExp;
        }

        if (itemEquipExp > 0) {
            flag += 0x40;
            total += itemEquipExp;
        }

        if (pcExp > 0) {
            //flag += 0x80;
            total += pcExp;
        }

        if (rainbowWeekExp > 0) {
            //flag += 0x100;
            total += rainbowWeekExp;
        }

        if (boomupExp > 0) {
            //flag += 0x200;
            total += boomupExp;
        }

        if (portionExp > 0) {
            //flag += 0x400;
            total += portionExp;
        }

        int burnExp = (int) (gain * 0.5);

        if (getSkillLevel(80000545) > 0) {
            skillExp += burnExp;
        }

        if (skillExp > 0) {
            flag += 0x800;
            total += skillExp;
        }

        if (buffExp > 0) {
            flag += 0x1000;
            total += buffExp;
        }

        if (restExp > 0) {
            //flag += 0x2000;
            total += restExp;
        }

        if (itemExp > 0) {
            flag += 0x4000;
            total += itemExp;
        }

        if (valueExp > 0) {
            //flag += 0x20000;
            total += valueExp;
        }

        int bonusExp = 0;
        if (bonusExp > 0) {
            flag += 0x80000;
            total += bonusExp;
        }

        if (bloodExp > 0) {
            //flag += 0x100000;
            total += bloodExp;
        }

        if (iceExp > 0) {
            //flag += 0x200000;
            total += iceExp;
        }

        if (burningExp.left > 0) {
            flag += 0x400000;
            total += burningExp.left;
        }

        if (hpLiskExp > 0) {
            //flag += 0x800000;
            total += hpLiskExp;
        }

        if (fieldBonusExp > 0) {
            //flag += 0x1000000;
            total += fieldBonusExp;
        }

        if (eventBonusExp2 > 0) {
            //flag += 0x4000000;
            total += eventBonusExp2;
        }

        if (fieldBonusExp2 > 0) {
            //flag += 0x10000000;
            total += fieldBonusExp2;
        }

        if (gain > 0 && total < gain) { //just in case
            total = Integer.MAX_VALUE;
        }

        if (total > 0) {
            stats.checkEquipLevels(this, total); //gms like
        }
        long needed = getNeededExp();
        if (level >= GameConstants.MaxLevel) {
            setExp(0);
            total = 0;
            //if (exp + total > needed) {
            //    setExp(needed);
            //} else {
            //    exp += total;
            //}
        } else {
            boolean leveled = false;
            // System.err.println("" + exp + total +" " + needed);
            if (exp + total >= needed) {
                exp += total;
                while (exp >= needed) {
                    levelUp();
                    if (getSkillLevel(80000545) > 0 && level >= 10 && level < 240 && !leveled) {
                        levelUp();
                        levelUp();
                        exp = 0;
                    }
                    leveled = true;
                    if (level >= 999) {
                        setExp(0);
                        total = 0;
                    }
                    /*else {
                     needed = getNeededExp();
                     if (exp >= needed) {
                     setExp(needed);
                     }
                     }*/

                }
            } else {
                if (level >= 600 && getExp() >= 200000000000000L) {
                    setExp(0);
                    setLevelPoint(getLevelPoint() + 3);
                    dropMessage(-1, "만렙포인트를 3점을 얻었습니다, 누적 : " + getLevelPoint());
                } else {
                    exp += total;
                }
            }
        }
        if (gain != 0) {
            if (exp < 0) { // After adding, and negative
                if (gain > 0) {
                    setExp(getNeededExp());
                } else if (gain < 0) {
                    setExp(0);
                }
            }
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                client.getSession().writeAndFlush(InfoPacket.GainEXP_Monster(this, total, white, flag, 0, weddingExp, partyExp, itemEquipExp, 0, 0, 0, 0, skillExp, buffExp, 0, itemExp, 0, bonusExp, 0, 0, burningExp, 0, 0, 0, 0));
            }
        }
    }

    public void forceReAddItem_NoUpdate(Item item, MapleInventoryType type) {
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item, MapleInventoryType type) { //used for stuff like durability, item exp/level, probably owner?
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, type, item));
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            World.Party.updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
        }
    }

    public boolean isSuperGM() {
        return gmLevel >= PlayerGMRank.SUPERGM.getLevel();
    }

    public boolean isIntern() {
        return gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isGM() {
        return gmLevel >= PlayerGMRank.GM.getLevel();
    }

    public boolean isAdmin() {
        return gmLevel >= PlayerGMRank.ADMIN.getLevel();
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public void setGMLevel(byte level) {
        this.gmLevel = level;
    }

    public int getLinkMobCount() {
        return LinkMobCount;
    }

    public void setLinkMobCount(int count) {
        LinkMobCount = count > 9999 ? 9999 : count;
    }

    public void gainItem(int code, int quantity) {
        if (quantity >= 0) {
            MapleInventoryManipulator.addById(client, code, (short) quantity, new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append("gainItem로 얻은 아이템").toString());
        } else {
            MapleInventoryManipulator.removeById(this.client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
    }

    public void gainSpecialItem(int code, int quantity) {
        if (quantity >= 0) {
            MapleInventoryManipulator.addById(client, code, (short) quantity, new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append("gainSpecialItem로 얻은 아이템").toString(), true);
        } else {
            MapleInventoryManipulator.removeById(this.client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
    }

    public final Equip gainItem(int id, short quantity, boolean randomStats, long period, String gm_log) {
        Equip equip = null;
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType type = GameConstants.getInventoryType(id);

            if (!MapleInventoryManipulator.checkSpace(this.client, id, quantity, "")) {
                return equip;
            }

            if (((type.equals(MapleInventoryType.EQUIP)) || (type.equals(MapleInventoryType.DECORATION))) && (!GameConstants.isThrowingStar(id)) && (!GameConstants.isBullet(id))) {
                Equip item = (Equip) (Equip) (ii.getEquipById(id));
                if (period > 0L) {
                    item.setExpiration(System.currentTimeMillis() + period);
                }
                item.setGMLog(new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(gm_log).toString());
                MapleInventoryManipulator.addbyItem(this.client, item);
                return item;
            } else {
                MapleInventoryManipulator.addById(this.client, id, quantity, "", null, period, new StringBuilder().append(StringUtil.getAllCurrentTime()).append("에 ").append(gm_log).toString());
            }
        } else {
            MapleInventoryManipulator.removeById(this.client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        this.client.getSession().writeAndFlush(InfoPacket.getShowItemGain(id, quantity, false));
        return equip;
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory getInventory(byte type) {
        return inventory[MapleInventoryType.getByType(type).ordinal()];
    }

    public final MapleInventory[] getInventorys() {
        return inventory;
    }

    public final void expirationTask(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.getSession().writeAndFlush(InfoPacket.itemExpired(z.intValue()));
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessageGM(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
            if (pendingSkills != null) {
                client.getSession().writeAndFlush(CWvsContext.updateSkills(pendingSkills));
                for (Skill z : pendingSkills.keySet()) {
                    client.getSession().writeAndFlush(CWvsContext.serverNotice(5, name, "[" + SkillFactory.getSkillName(z.getId()) + "] skill has expired and will not be available for use."));
                }
            } //not real msg
            pendingSkills = null;
            return;
        }
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<Integer>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<Triple<MapleInventoryType, Item, Boolean>>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<Item>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration) || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                } else if (item.getPosition() == -59) {
                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, true));
                    }
                }
            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem = null;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(replace.left) == MapleInventoryType.DECORATION) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    getInventory(itemz.getLeft()).addFromDB(theNewItem);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;

        final Map<Skill, SkillEntry> skilz = new HashMap<>();
        final List<Skill> toberem = new ArrayList<Skill>();
        for (Entry<Skill, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttime > skil.getValue().expiration) {
                toberem.add(skil.getKey());
            }
        }
        for (Skill skil : toberem) {
            skilz.put(skil, new SkillEntry(0, (byte) 0, -1));
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = skilz;

        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) < currenttime) { //expired bro
            quests.remove(MapleQuest.getInstance(7830));
            quests.remove(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        }
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public long getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.getValue()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public void gainMeso(long gain, boolean show) {
        gainMeso(gain, show, false);
    }

    public void gainMeso(long gain, boolean show, boolean inChat) {
        gainMeso(gain, show, inChat, false);
    }

    public void gainMeso(long gain, boolean show, boolean inChat, boolean isPet) {
        if (meso + gain < 0) {
            client.getSession().writeAndFlush(CWvsContext.enableActions(this));
            return;
        }
        meso += gain;

        Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);
        statup.put(MapleStat.MESO, meso);

        if (isPet) {
            client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, false, false, this, true));
            client.getSession().writeAndFlush(CWvsContext.onMesoPickupResult((int) gain));
        } else {
            client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, true, false, this, false));
        }

//        if (!isPet) {
//        	client.getSession().writeAndFlush(CWvsContext.enableActions(this));
        //      }
        if (show) {
            client.getSession().writeAndFlush(InfoPacket.showMesoGain(gain, isPet, inChat));
        }
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        if (monster == null) {
            return;
        }
        monster.setController(this);
        controlled.add(monster);
        if (monster.getOwner() == -1) {
            client.getSession().writeAndFlush(MobPacket.controlMonster(monster, false, aggro));
        } else if (monster.getOwner() == this.getId()) {
            client.getSession().writeAndFlush(MobPacket.controlMonster(monster, false, aggro));
        }
        monster.sendStatus(client);
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        if (controlled.contains(monster)) {
            controlled.remove(monster);
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(final int id, final int skillID) {

        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID, this)) {
                client.getSession().writeAndFlush(InfoPacket.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().writeAndFlush(CWvsContext.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public final List<MapleQuestStatus> getStartedQuests() {

        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {

        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<Pair<Integer, Long>>();

        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0 && GameConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<Integer, Long>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public Map<Skill, SkillEntry> getSkills() {
        Map<Skill, SkillEntry> list = new HashMap<>();
        for (Entry<Skill, SkillEntry> data : skills.entrySet()) {
            if (data.getKey().getId() / 10000 != 7000) {
                list.put(data.getKey(), data.getValue());
            }
        }
        return Collections.unmodifiableMap(list);
    }

    public int getTotalSkillLevel(Skill skill) {
        if (skill == null) {
            return 0;
        }
        if (skill.getId() == 33000036 || skill.getId() == 80001770 || skill.getId() == 80002887 || skill.getId() == 80001242 || skill.getId() == 80001965 || skill.getId() == 80001966 || skill.getId() == 80001967 || skill.getId() == 155001205) {
            return 1;
        }
        if (GameConstants.isAngelicBlessSkill(skill)) {
            return (byte) 1;
        }
        if (GameConstants.isSaintSaverSkill(skill.getId())) {
            return (byte) 1;
        }
        if (skill.getId() == 155101204) {
            skill = SkillFactory.getSkill(155101104);
        }
        if (skill.getId() == 155111202) {
            skill = SkillFactory.getSkill(155111102);
        }
        if (skill.getId() == 11121014) {
            skill = SkillFactory.getSkill(11121005);
        }
        if (skill.getId() == 400011089) {
            skill = SkillFactory.getSkill(400011088);
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return Math.min(skill.getTrueMax(), ret.skillevel + (skill.isBeginnerSkill() ? 0 : (stats.combatOrders + (skill.getMaxLevel() > 10 ? stats.incAllskill : 0) + stats.getSkillIncrement(skill.getId()))));
    }

    public long getSkillExpiry(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    public int getSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.skillevel;
    }

    public byte getMasterLevel(final int skill) {
        return getMasterLevel(SkillFactory.getSkill(skill));
    }

    public byte getMasterLevel(final Skill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public void levelUp() {
        remainingAp += 5;
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        long maxhp = stats.getMaxHp();
        long maxmp = stats.getMaxMp();

        if (GameConstants.isWarrior(job)) {
            maxhp += Randomizer.rand(160, 240);
            maxmp += Randomizer.rand(80, 120);
        } else if (GameConstants.isMagician(job)) {
            maxhp += Randomizer.rand(80, 120);
            maxmp += Randomizer.rand(160, 240);
        } else if (GameConstants.isDemonAvenger(job)) {
            maxhp += 0;
            maxmp += 0;
        } else {
            maxhp += Randomizer.rand(100, 150);
            maxmp += Randomizer.rand(100, 250);
        }

        exp -= getNeededExp();
        level += 1;

        boolean unionz = false;
        for (MapleUnion union : getUnions().getUnions()) {
            if (union.getCharid() == id) {
                union.setLevel(level);
                unionz = true;
            }
        }

        if (!unionz) {
            if (level >= 60 && !GameConstants.isZero(job)) {
                getUnions().getUnions().add(new MapleUnion(id, level, job, 0, 0, -1, 0, name));
            } else if (level >= 130 && GameConstants.isZero(job)) {
                getUnions().getUnions().add(new MapleUnion(id, level, job, 0, 0, -1, 0, name));
            }
        }

        if (level == GameConstants.MaxLevel) {
            exp = 0;
        }
        if (level <= 100) {
            autoJob();
        }
        if (level == 200 && client.getKeyValue("levelup2") == null) {      //첫계정 레벨보상
            client.setKeyValue("levelup2", "1");
            gainItem(5062010, 200);
            gainItem(5062500, 100);
            gainItem(2435719, 200);
            gainItem(2049704, 5);
            gainItem(4001716, 1); 
            this.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062294, 3000, "첫 캐릭터 200레벨 달성 보상이 지급되었다네.", ""));
        }

        if (level == 160) {
            skillMaster();
        }
        
        int passLevel = (int) getKeyValue(100592, "point");
        if (passLevel == 10) {
            this.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062294, 3000, "       [#r성장패스#k]\\r\\n   받을 수 있는 보상이 있다네!\\r\\n   #b성장 컨텐츠 -> 성장패스#k를\\r\\n  확인해보게나", ""));
        }
        
        if (level >= 200) {
            if (getQuestStatus(1465) != 2 || isGM()) {
                forceCompleteQuest(1465);
                MatrixHandler.gainMatrix(this);
                client.getSession().writeAndFlush(CField.environmentChange("Effect/5skill.img/screen", 16));
                client.getSession().writeAndFlush(CField.playSound("Sound/SoundEff.img/5thJob"));
                dropMessage(6, "5차 전직이 완료되었습니다.");
                MatrixHandler.gainVCoreLevel(this);
                if (GameConstants.isZero(job)) {
                    changeSingleSkillLevel(SkillFactory.getSkill(100001005), (byte) 1, (byte) 1);
                }
            }
            if (getQuestStatus(1466) != 2) {
                forceCompleteQuest(1466);
            }
        }
        if (level >= 260) {
            if (getQuestStatus(1484) != 2) {
                forceCompleteQuest(1484);
                dropMessage(6, "260레벨을 달성하여 어센틱 포스가 개방되었습니다.");
            }
        }

        if (level >= 250 && (level % 25 == 0) && !isGM()) {
            final StringBuilder sb = new StringBuilder("[공지] ");
            final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((short) -21);
            if (medal != null) { // Medal
                sb.append("<");
                sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                sb.append("> ");
            }
            sb.append(getName());
            sb.append("님이 " + level + "레벨을 달성했습니다! 모두 축하해 주세요!");
            World.Broadcast.broadcastMessage(CField.getGameMessage(8, sb.toString()));
        }
        maxhp = Math.min(500000, Math.abs(maxhp));
        maxmp = Math.min(500000, Math.abs(maxmp));
        if (GameConstants.isDemonSlayer(job)) { //TODO: use shield instead of df per job
            maxmp = GameConstants.getMPByJob(this);
        } else if (GameConstants.isZero(job)) {
            maxmp = 100;
        }
        final Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);

        stats.setInfo(maxhp, maxmp, getStat().getCurrentMaxHp(), getStat().getCurrentMaxMp(this));
        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, getStat().getHp());
        statup.put(MapleStat.MP, getStat().getMp());
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (long) level);

        if (level <= 10) {
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (long) stats.getStr());
        }
        /*        if (LoginInformationProvider.isExtendedSpJob(job)) {
         if (GameConstants.isZero(job) && level >= 100) {
         remainingSp[0] += 3; //alpha gets 3sp
         remainingSp[1] += 3; //beta gets 3sp
         } else if (level >= 11) {
         remainingSp[GameConstants.getSkillBook(job, level)] += 3;
         }
         } else if (level >= 11) {
         remainingSp[GameConstants.getSkillBook(job, 0)] += 3;
         }*/

        statup.put(MapleStat.AVAILABLEAP, (long) remainingAp);
//        statup.put(MapleStat.AVAILABLESP, (long) remainingSp[GameConstants.getSkillBook(job, level)]);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showNormalEffect(this, 0, false), false);
        silentPartyUpdate();
        guildUpdate();
        //if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        //    changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        //    dropMessageGM(-1, "You have been expelled from the map.");
        //}
    }

    public boolean existPremium() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        boolean ret = false;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM premium WHERE accid = ?");
            ps.setInt(1, this.getAccountID());
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

    public long getRemainPremium() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        long ret = 0;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM premium WHERE accid = ?");
            ps.setInt(1, this.getAccountID());
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

    public void loadPremium() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM premium WHERE accid = ?");
            ps.setInt(1, this.getAccountID());
            rs = ps.executeQuery();

            if (rs.next()) {
                premium = rs.getString("name");
                premiumbuff = rs.getInt("buff");
                premiumPeriod = rs.getLong("period");
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

    public void gainPremium(int v3) {
        Date adate = new Date();
        Date bdate = new Date();

        if (premiumPeriod != 0) {
            bdate.setTime(premiumPeriod + (v3 * 24 * 60 * 60 * 1000));
            if (adate.getTime() > bdate.getTime()) {
                premiumPeriod = (adate.getTime() + (v3 * 24 * 60 * 60 * 1000));
                premium = "일반";
                premiumbuff = 80001535;
            } else {
                premiumPeriod = (bdate.getTime() + (v3 * 24 * 60 * 60 * 1000));
            }
        }

        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (existPremium()) {
                if (getRemainPremium() > adate.getTime()) {
                    con = DatabaseConnection.getConnection();
                    ps = con.prepareStatement("UPDATE premium SET period = ? WHERE accid = ?");
                    ps.setLong(1, getRemainPremium() + (v3 * 24 * 60 * 60 * 1000));
                    ps.setInt(2, this.getAccountID());
                    ps.executeUpdate();

                    ps.close();
                    con.close();
                } else {
                    con = DatabaseConnection.getConnection();
                    ps = con.prepareStatement("UPDATE premium SET period = ? and `name` = ? and `buff` = ? WHERE accid = ?");
                    ps.setLong(1, adate.getTime() + (v3 * 24 * 60 * 60 * 1000));
                    ps.setString(2, "일반");
                    ps.setInt(3, 80001535);
                    ps.setInt(4, this.getAccountID());
                    ps.executeUpdate();

                    ps.close();
                    con.close();
                }
            } else {

                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("INSERT INTO premium(accid, name, buff, period) VALUES (?, ?, ?, ?)");
                ps.setInt(1, this.getAccountID());
                ps.setString(2, "일반");
                ps.setInt(3, 80001535);
                ps.setLong(4, v3);
                ps.executeUpdate();
                ps.close();
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    }

    public void setPremium(String v1, int v2, long v3) {
        if (SkillFactory.getSkill(premiumbuff) != null) {
            this.changeSingleSkillLevel(SkillFactory.getSkill(premiumbuff), 0, (byte) 0);
        }
        premium = v1;
        premiumbuff = v2;
        premiumPeriod = v3;

        Connection con = null;
        PreparedStatement ps = null;

        try {
            if (existPremium()) {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE premium SET name = ?, buff = ?, period = ? WHERE accid = ?");
                ps.setString(1, v1);
                ps.setInt(2, v2);
                ps.setLong(3, v3);
                ps.setInt(4, this.getAccountID());
                ps.executeUpdate();

                ps.close();
                con.close();
            } else {

                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("INSERT INTO premium(accid, name, buff, period) VALUES (?, ?, ?, ?)");
                ps.setInt(1, this.getAccountID());
                ps.setString(2, premium);
                ps.setInt(3, premiumbuff);
                ps.setLong(4, premiumPeriod);
                ps.executeUpdate();
                ps.close();
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    }

    public String getPremium() {
        return premium;
    }

    public int getPremiumBuff() {
        return premiumbuff;
    }

    public Long getPremiumPeriod() {
        return premiumPeriod;
    }

    public void autoJob() {
        if (GameConstants.isZero(job)) {
            return;
        }
        if (job % 100 == 0 || job % 100 == 1) { // 1차전직
            if (getReborns() != 0) { // 모험가
                if ((getReborns() == 430 && level >= 20) || level >= 30) {
                    changeJob(getReborns());
                    setReborns(0);
                    SkillMasterByJob();
                }
            } else if (level >= 30 && job == 501) {
                changeJob(530);
                setReborns(0);
                SkillMasterByJob();
            } else if (job == 2200 && level >= 30) {
                changeJob(2211);
                SkillMasterByJob();
            } else if (job == 3101 && level >= 30) {
                changeJob(3120);
                SkillMasterByJob();
            } else if (level >= 30) {
                changeJob(job + 10);
                SkillMasterByJob();
            }
        } else if (GameConstants.isDualBlade(job)) {
            if (job % 100 == 30 && level >= 30) {
                changeJob(job + 1);
                SkillMasterByJob();
            } else if (job % 100 == 31 && level >= 45) {
                changeJob(job + 1);
                SkillMasterByJob();
            } else if (job % 100 == 32 && level >= 60) {
                changeJob(job + 1);
                SkillMasterByJob();
            } else if (job % 100 == 33 && level >= 100) {
                changeJob(job + 1);
                SkillMasterByJob();
            }
        } else if (GameConstants.isEvan(job)) {
            if (job == 2211 && level >= 60) {
                changeJob(2214);
                SkillMasterByJob();
            } else if (job == 2214 && level >= 100) {
                changeJob(2217);
                SkillMasterByJob();
            }
        } else if ((job % 100 == 10 || job % 100 == 20 || job % 100 == 30) && level >= 60) {
            changeJob(job + 1);
            SkillMasterByJob();
        } else if ((job % 100 == 11 || job % 100 == 21 || job % 100 == 31) && level >= 100) {
            changeJob(job + 1);
            SkillMasterByJob();
        }
    }

    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(Integer.valueOf(key), new Pair<Byte, Integer>(type, action));
        } else {
            keylayout.Layout().remove(Integer.valueOf(key));
        }
    }

    public void sendMacros() {
        client.getSession().writeAndFlush(CField.getMacros(skillMacros));
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public void setMarriage(MarriageMiniBox mgs) {
        this.mg = mgs;
    }

    public MarriageMiniBox getMarriage() {
        return mg;
    }

    public void tempban(String reason, Calendar duration, int greason, boolean IPMac) {
        if (IPMac) {
            client.banMacs();
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            if (IPMac) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSession().remoteAddress().toString().split(":")[0]);
                ps.execute();
                ps.close();
            }

            client.getSession().close();

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
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

    public final boolean ban(String reason, boolean IPMac, boolean autoban, boolean hellban) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement psa = null;
        PreparedStatement pss = null;
        ResultSet rsa = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.execute();
            ps.close();

            if (IPMac) {
                client.banMacs();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();

                if (hellban) {
                    psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, accountid);
                    rsa = psa.executeQuery();
                    if (rsa.next()) {
                        pss = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE email = ? OR SessionIP = ?");
                        pss.setInt(1, autoban ? 2 : 1);
                        pss.setString(2, reason);
                        pss.setString(3, rsa.getString("email"));
                        pss.setString(4, client.getSessionIPAddress());
                        pss.execute();
                        pss.close();
                    }
                    rsa.close();
                    psa.close();
                }
            }
            con.close();
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
            return false;
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (pss != null) {
                    pss.close();
                }
                if (psa != null) {
                    psa.close();
                }
                if (rsa != null) {
                    rsa.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        client.getSession().close();
        return true;
    }

    public static boolean ban(String id, String reason, boolean accountId, int gmlevel, boolean hellban) {
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement psa = null;
        PreparedStatement psb = null;
        PreparedStatement psz = null;
        PreparedStatement pss = null;
        ResultSet rs = null;
        ResultSet rsa = null;
        try {
            con = DatabaseConnection.getConnection();
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.execute();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                int z = rs.getInt(1);
                psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ? AND gm < ?");
                psb.setString(1, reason);
                psb.setInt(2, z);
                psb.setInt(3, gmlevel);
                psb.execute();
                psb.close();

                if (gmlevel > 100) { //admin ban
                    psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, z);
                    rsa = psa.executeQuery();
                    if (rsa.next()) {
                        String sessionIP = rsa.getString("sessionIP");
                        if (sessionIP != null && sessionIP.matches("/[0-9]{1,3}\\..*")) {
                            psz = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                            psz.setString(1, sessionIP);
                            psz.execute();
                            psz.close();
                        }
                        if (rsa.getString("macs") != null) {
                            String[] macData = rsa.getString("macs").split(", ");
                            if (macData.length > 0) {
                                MapleClient.banMacs(macData);
                            }
                        }
                        if (hellban) {
                            pss = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE email = ?" + (sessionIP == null ? "" : " OR SessionIP = ?"));
                            pss.setString(1, reason);
                            pss.setString(2, rsa.getString("email"));
                            if (sessionIP != null) {
                                pss.setString(3, sessionIP);
                            }
                            pss.execute();
                            pss.close();
                        }
                    }
                    rsa.close();
                    psa.close();
                }
                ret = true;
            }
            rs.close();
            ps.close();
            con.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (psa != null) {
                    psa.close();
                }
                if (psb != null) {
                    psb.close();
                }
                if (psz != null) {
                    psz.close();
                }
                if (pss != null) {
                    pss.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (rsa != null) {
                    rsa.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Oid of players is always = the cid
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        getVisibleMapObjects().add(mo);
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        getVisibleMapObjects().remove(mo);
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        return getVisibleMapObjects().contains(mo);
    }

    public String getChairText() {
        return this.chairtext;
    }

    public void setChairText(String chairtext) {
        this.chairtext = chairtext;
    }

    public ScheduledFuture<?> getMesoChairTimer() {
        return MesoChairTimer;
    }

    public void setMesoChairTimer(ScheduledFuture<?> a1) {
        MesoChairTimer = a1;
        tempmeso = 0;
    }

    public int getMesoChairCount() {
        return MesoChairCount > 999999999 ? 999999999 : MesoChairCount;
    }

    public void UpdateMesoChairCount(int a1) {
        if (tempmeso >= a1) {
            MesoChairTimer.cancel(true);
            MesoChairTimer = null;
            setChair(0);
            setChairText("");
            getClient().getSession().writeAndFlush(CField.cancelChair(-1, this));
            getMap().broadcastMessage(this, CField.showChair(this, 0), true);
            return;
        }
        MesoChairCount = MesoChairCount + 500;
        gainMeso(-500, false);
        tempmeso += 500;
        getMap().broadcastMessage(SLFCGPacket.MesoChairPacket(getId(), 500, getChair()));
    }

    public long getStarDustPoint() {
        return Long.valueOf(getKeyValue(100592, "total"));
    }

    public long getStarDustCoin() {
        return Long.valueOf(getKeyValue(100592, "point"));
    }

    public void AddStarDustPoint(int a, Point point) {
        if (getKeyValue(100592, "total") + a >= 100) {
            if (getKeyValue(100592, "point") == Integer.MAX_VALUE || getKeyValue(100592, "point") < 0) {
                return;
            }
            //    setKeyValue(91632, "unioncoin", String.valueOf(getKeyValue(91632, "unioncoin") + 1));
            setKeyValue(100592, "point", String.valueOf(getKeyValue(100592, "point") + 1));
            setKeyValue(100592, "total", String.valueOf(a));
            client.getSession().writeAndFlush(SLFCGPacket.StarDustIncrease((int) getKeyValue(100592, "total"), a, false, (int) getKeyValue(100592, "point"), 1, point));
        } else {
            setKeyValue(100592, "total", String.valueOf(getKeyValue(100592, "total") + a));
            client.getSession().writeAndFlush(SLFCGPacket.StarDustIncrease((int) getKeyValue(100592, "total"), a, false, (int) getKeyValue(100592, "point"), 0, point));
        }
    }

    public void AddStarDustCoin(int a) {
        setKeyValue(100592, "point", String.valueOf(getKeyValue(100592, "point") + a));
        if (a < 0) {
            client.getSession().writeAndFlush(SLFCGPacket.StarDustIncrease((int) getKeyValue(100592, "total"), a * 100, false, (int) getKeyValue(100592, "point"), a, new Point(-1, -1)));
        } else {
            if (getKeyValue(100592, "point") + a > Integer.MAX_VALUE) {
                setKeyValue(100592, "point", "99147483647");
            }
            client.getSession().writeAndFlush(SLFCGPacket.StarDustIncrease((int) getKeyValue(100592, "total"), a * 100, false, (int) getKeyValue(100592, "point"), a, new Point(-1, -1)));
        }
    }

    public void addDojoCoin(int a) {
        setKeyValue(3887, "point", String.valueOf(getKeyValue(3887, "point") + a));
    }

    public void getDonationPoint(int a) {
        setKeyValue(9797, "point", String.valueOf(getKeyValue(9797, "point") + a));
    }

    public Point getResolution() {
        return Resolution;
    }

    public void setResolution(final int Width, final int Height) {
        Resolution = new Point(Width, Height);
    }

    public DetectiveGame getDetectiveGame() {
        return DetectiveGameInstance;
    }

    public void setDetectiveGame(DetectiveGame a1) {
        DetectiveGameInstance = a1;
    }

    public OXQuizGame getOXGame() {
        return OXInstance;
    }

    public void setOXGame(OXQuizGame a1) {
        OXInstance = a1;
    }

    public BingoGame getBingoGame() {
        return BingoInstance;
    }

    public void setBingoGame(BingoGame a1) {
        BingoInstance = a1;
    }

    public BattleReverse getBattleReverseInstance() {
        return this.BattleReverseInstance;
    }

    public void setBattleReverseInstance(BattleReverse a1) {
        this.BattleReverseInstance = a1;
    }

    public boolean isAFK(long currenttick) {
        if (LastMovement == 0L || /*!getMap().isTown() ||*/ getMap().getId() != ServerConstants.WarpMap || getChair() == 0) {
            return false;
        }
        long temp = currenttick - LastMovement;
        return (temp / 1000) >= 60;
    }

    public void setLastMovement(long a) {
        LastMovement = a;
    }

    public void setPlatformerStageEnter(long a) {
        PlatformerStageEnter = a;
    }

    public long getPlatformerStageEnter() {
        return PlatformerStageEnter;
    }

    public void setPlatformerTimer(ScheduledFuture<?> a) {
        PlatformerTimer = a;
    }

    public ScheduledFuture<?> getPlatformerTimer() {
        return PlatformerTimer;
    }

    public int getBlockCount() {
        return BlockCount;
    }

    public void setBlockCount(int a1) {
        BlockCount = a1;
    }

    public void setBlockCoin(int a1) {
        BlockCoin = a1;
    }

    public int getBlockCoin() {
        return BlockCoin;
    }

    public void addBlockCoin(int a1) {
        BlockCoin += a1;
    }

    public int getRandomPortal() {
        return RandomPortal;
    }

    public void setRandomPortal(int a1) {
        RandomPortal = a1;
    }

    public boolean hasFWolfPortal() {
        return hasfwolfportal;
    }

    public void setFWolfPortal(boolean a1) {
        hasfwolfportal = a1;
    }

    public boolean isWatchingWeb() {
        return isWatchingWeb;
    }

    public void setWatchingWeb(boolean a1) {
        isWatchingWeb = a1;
    }

    public boolean isFWolfKiller() {
        return isfwolfkiller;
    }

    public void setFWolfKiller(boolean a1) {
        isfwolfkiller = a1;
    }

    public long getFWolfDamage() {
        return fwolfdamage;
    }

    public void setFWolfDamage(long a1) {
        fwolfdamage = a1;
    }

    public int getFWolfAttackCount() {
        return fwolfattackcount;
    }

    public void setFWolfAttackCount(int a1) {
        fwolfattackcount = a1;
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(CField.removePlayerFromMap(this.getObjectId()));
        //don't need this, client takes care of it
        /*if (dragon != null) {
         client.getSession().writeAndFlush(CField.removeDragon(this.getId()));
         }
         if (android != null) {
         client.getSession().writeAndFlush(CField.deactivateAndroid(this.getId()));
         }*/
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().allowedToTarget(this)) {
            client.getSession().writeAndFlush(CField.spawnPlayerMapobject(this));

            if (getKeyValue(190823, "grade") > 0) {
                client.getSession().writeAndFlush(SLFCGPacket.SetupZodiacInfo());
                client.getSession().writeAndFlush(SLFCGPacket.ZodiacRankInfo(getId(), (int) getKeyValue(190823, "grade")));
            }

            client.getPlayer().receivePartyMemberHP();

            for (int i = 0; i < 3; ++i) {
                if (pets[i] != null) { // && client.getPlayer().getMapId() != ServerConstants.WarpMap && this.mapid != ServerConstants.WarpMap
                    if (this.getPet(i).getPos() == null) {
                        this.getPet(i).setPos(getTruePosition());
                    }
                    client.getSession().writeAndFlush(PetPacket.showPet(this, this.getPet(i), false, false));
                    client.getSession().writeAndFlush(PetPacket.petExceptionList(this, this.getPet(i)));
//                    client.getSession().writeAndFlush(CWvsContext.enableActions(this));
                }
            }
            if (dragon != null) {
                client.getSession().writeAndFlush(CField.spawnDragon(dragon));
            }
            if (android != null && this.mapid != ServerConstants.WarpMap) {
                client.getSession().writeAndFlush(CField.spawnAndroid(this, android));
            }

            if (getGuild() != null && getGuild().getCustomEmblem() != null && client.getAccID() != getAccountID()) {
                client.getSession().writeAndFlush(CField.loadGuildIcon(this));
            }

            for (final MapleSummon summon : summons) {
                if (summon.getMovementType() != SummonMovementType.STATIONARY) {
                    client.getSession().writeAndFlush(SummonPacket.spawnSummon(summon, true));
                }
            }

            if (getBuffedValue(MapleBuffStat.RepeatEffect) != null) {
                int skillid = getBuffedEffect(MapleBuffStat.RepeatEffect).getSourceId();
                if (GameConstants.isAngelicBlessBuffEffectItem(skillid)) {
                    EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
                    statups.put(MapleBuffStat.RepeatEffect, new Pair<>(1, 0));
                    MapleStatEffect effect = MapleItemInformationProvider.getInstance().getItemEffect(skillid);
                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
                }
            }
        }
    }

    public final void equipChanged() {
        if (map == null) {
            return;
        }
        boolean second = false;
        if (GameConstants.isAngelicBuster(getJob())) {
            second = getDressup();
        }
        if (GameConstants.isZero(getJob())) {
            second = getGender() == 1;
        }
        map.broadcastMessage(this, CField.updateCharLook(this, second), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            World.Messenger.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    public final MaplePet getPet(final long index) { //Check
        return pets[(int) index];
    }

    public void updatePet() {
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                getClient().getSession().writeAndFlush(PetPacket.updatePet(this, pets[i], getInventory(MapleInventoryType.CASH).getItem((short) pets[i].getInventoryPosition()), false, petLoot));
            }
        }
    }

    public void addPet(final MaplePet pet) { //Check
        for (int i = 0; i < 3; ++i) {
            if (pets[i] == null) {
                pets[i] = pet;
                return;
            }
        }
    }

    public void addPetBySlotId(final MaplePet pet, int slotid) {
        if (pets[slotid] == null) {
            pets[slotid] = pet;
            pets[slotid].setPos(getPosition());
        }
    }

    public void setDotDamage(long dmg) {
        DotDamage = dmg;
    }

    public long getDotDamage() {
        return DotDamage;
    }

    public Point getFlameHeiz() {
        return flameHeiz;
    }

    public void setFlameHeiz(Point flameHeiz) {
        this.flameHeiz = flameHeiz;
    }

    public void removePet(MaplePet pet, boolean shiftLeft) { //Check
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    pets[i] = null;
                    slot = i;
                    break;
                }
            }
        }
        if (shiftLeft) {
            if (slot > -1) {
                for (int i = slot; i < 3; i++) {
                    if (i != 2) {
                        pets[i] = pets[i + 1];
                    } else {
                        pets[i] = null;
                    }
                }
            }
        }
    }

    public final int getPetIndex(final MaplePet pet) { //Check
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public final int getPetIndex(final int petId) { //Check
        for (int i = 0; i < 3; ++i) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == petId) {
                    return i;
                }
            }
        }
        return -1;
    }

    public final List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<MaplePet>();
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                ret.add(pet);
            }
        }
        return ret;
    }

    public final byte getPetById(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getPetItemId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final MaplePet[] getPets() { //Check
        return pets;
    }

    public final void unequipAllPets() {
        for (final MaplePet pet : pets) {
            if (pet != null) {
                unequipPet(pet, true, false);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shiftLeft, boolean hunger) { //Check
        pet.setSummoned((byte) 0);
        client.getSession().writeAndFlush(PetPacket.updatePet(this, pet, getInventory(MapleInventoryType.CASH).getItem((short) pet.getInventoryPosition()), true, petLoot));
        if (map != null) {
            map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
        }
        removePet(pet, shiftLeft);
        client.getSession().writeAndFlush(CWvsContext.enableActions(this));
    }

    public final long getLastFameTime() {
        return lastfametime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public final List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
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

    public boolean canBattle(MapleCharacter to) {
        if (to == null || lastmonthbattleids == null || lastmonthbattleids.contains(Integer.valueOf(to.getAccountID()))) {
            return false;
        }
        return true;
    }

    public void hasBattled(MapleCharacter to) {
        lastmonthbattleids.add(Integer.valueOf(to.getAccountID()));
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO battlelog (accid, accid_to) VALUES (?, ?)");
            ps.setInt(1, getAccountID());
            ps.setInt(2, to.getAccountID());
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing battlelog for char " + getName() + " to " + to.getName() + e);
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

    public final MapleKeyLayout getKeyLayout() {

        return this.keylayout;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public void addMechDoor(MechDoor door) {
        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MechDoor> getMechDoors() {
        return new ArrayList<MechDoor>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public List<MapleSummon> getSummons() {
        return summons;
    }

    public MapleSummon getSummon(int skillId) {

        Iterator<MapleSummon> summon = summons.iterator();

        while (summon.hasNext()) {
            MapleSummon s = summon.next();
            if (s.getSkill() == skillId) {
                return s;
            }
        }

        return null;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void addSummon(MapleSummon s) {
        summons.add(s);
    }

    public void removeSummon(MapleSummon s) {
        summons.remove(s);
        if (s.getSkill() == 400011065) {
            cooldownEllision = System.currentTimeMillis();
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getCurrentRep() {
        return currentrep;
    }

    public int getTotalRep() {
        return totalrep;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void increaseTotalWins() {
        totalWins++;
    }

    public void increaseTotalLosses() {
        totalLosses++;
    }

    public int getGuildId() {
        return guildid;
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public void setGuildRank(byte _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setGuildContribution(int _c) {
        this.guildContribution = _c;
        if (mgc != null) {
            mgc.setGuildContribution(_c);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void setAllianceRank(byte rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return World.Guild.getGuild(getGuildId());
    }

    public void setJob(int j) {
        this.job = (short) j;
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel((short) level);
        mgc.setJobId(job);
        World.Guild.memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void modifyCSPoints(int type, int quantity) {
        modifyCSPoints(type, quantity, false);
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {

        switch (type) {
            case 1:
                if (nxcredit + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                ///if (quantity > 0) {
                //    quantity = (quantity / 2); //stuff is cheaper lol
                //}
                nxcredit += quantity;
                break;
            case 4:
                if (acash + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                //if (quantity > 0) {
                //    quantity = (quantity / 2); //stuff is cheaper lol
                //}
                acash += quantity;
                break;
            default:
                break;
        }
        if (show && quantity != 0) {

            dropMessage(-1, quantity + (type == 1 ? " 캐시를 " : " 메이플포인트를 ") + (quantity > 0 ? "얻었습니다." : "잃었습니다."));
            client.getSession().writeAndFlush(EffectPacket.showNormalEffect(this, 24, true));

        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return nxcredit;
            case 2:
                return maplepoints;
            case 4:
                return acash;
            default:
                return 0;
        }
    }

    public final boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public final boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP || checkEquipped && type == MapleInventoryType.DECORATION) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public final boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public final boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }

    public static enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public void teachSkill(final int id, final int level, final byte masterlevel) {

        this.changeSingleSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public void teachSkill(final int id, int level) {
        final Skill skil = SkillFactory.getSkill(id);
        if (this.getSkillLevel(skil) > level) {
            level = this.getSkillLevel(skil);
        }
        this.changeSingleSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    public byte getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().writeAndFlush(BuddylistPacket.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        if (!GameConstants.isNotCooldownSkill(skillId)) {
            coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length - 125));
        }
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(Integer.valueOf(skillId))) {
            coolDowns.remove(Integer.valueOf(skillId));
            getClient().getSession().writeAndFlush(CField.skillCooldown(skillId, 0));
        }
    }

    public void changeCooldown(int skillId, int reduce) {
        if (coolDowns.containsKey(skillId)) {
            coolDowns.get(skillId).length += reduce;
            getClient().getSession().writeAndFlush(CField.skillCooldown(skillId, (int) Math.max(0, coolDowns.get(skillId).length - (System.currentTimeMillis() - coolDowns.get(skillId).startTime))));
            if (System.currentTimeMillis() - coolDowns.get(Integer.valueOf(skillId)).startTime >= coolDowns.get(skillId).length) {
                coolDowns.remove(Integer.valueOf(skillId));
                return;
            }
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        addCooldown(skillid, starttime, length);
    }

    public void giveCoolDowns(final List<MapleCoolDownValueHolder> cooldowns) {
        int time;

        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
                ps.setInt(1, getId());
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                }
                ps.close();
                rs.close();
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

                con.close();
            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
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
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public int getDiseaseSize() {
        return getDiseases().size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<MapleCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    public Pair<MobSkill, Integer> getDisease(MapleBuffStat d) {
        return getDiseases().get(d);
    }

    public final boolean hasDisease(final MapleBuffStat dis) {
        return getDiseases().containsKey(dis);
    }

    public void disease(int skillId, int mobSkillLevel) {
        MobSkill ms = MobSkillFactory.getMobSkill(skillId, mobSkillLevel);
        MapleBuffStat disease = MapleBuffStat.getBySkill(skillId);
        if (disease != null) {
            giveDebuff(disease, ms);
        }
    }

    public Integer getDebuffValue(MapleBuffStat stat) {
        for (Entry<MapleBuffStat, Pair<MobSkill, Integer>> disease : getDiseases().entrySet()) {
            if (disease.getKey() == stat) {
                return disease.getValue().right;
            }
        }
        return -1;
    }

    public void giveDebuff(MapleBuffStat disease, MobSkill skill) {
        Map<MapleBuffStat, Pair<Integer, Integer>> diseases = new HashMap<>();
        diseases.put(disease, new Pair<>(skill.getX(), (int) skill.getDuration()));
        giveDebuff(diseases, skill);
    }

    public void giveDebuff(final Map<MapleBuffStat, Pair<Integer, Integer>> diseases, MobSkill skill) {

        if (map != null && skill != null) {
            Iterator<Entry<MapleBuffStat, Pair<Integer, Integer>>> diseasez = diseases.entrySet().iterator();
            while (diseasez.hasNext()) {
                Entry<MapleBuffStat, Pair<Integer, Integer>> disease = diseasez.next();
                if (hasDisease(disease.getKey())) {
                    diseasez.remove();
                } else if (antiMagicShell > 0 && getBuffedEffect(MapleBuffStat.AntiMagicShell) != null) {
                    diseasez.remove();
                    if (getBuffSource(MapleBuffStat.AntiMagicShell) < 3000000) { // 모험가 법사
                        if (getBuffedEffect(MapleBuffStat.AntiMagicShell).makeChanceResult()) {
                            antiMagicShell--;
                        } else {
                            cancelEffectFromBuffStat(MapleBuffStat.AntiMagicShell);
                        }
                    } else {
                        antiMagicShell--;
                        if (antiMagicShell == 0) {
                            cancelEffectFromBuffStat(MapleBuffStat.AntiMagicShell);
                        } else {
                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                            statups.put(MapleBuffStat.AntiMagicShell, new Pair<>((int) antiMagicShell, (int) getBuffLimit(getBuffSource(MapleBuffStat.AntiMagicShell))));
                            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, getBuffedEffect(MapleBuffStat.AntiMagicShell), this));
                        }
                    }
                } else if (getSpiritGuard() > 0) {
                    diseasez.remove();
                    setSpiritGuard(getSpiritGuard() - 1);
                    if (getSpiritGuard() == 0) {
                        cancelEffectFromBuffStat(MapleBuffStat.SpiritGuard);
                    } else {
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        statups.put(MapleBuffStat.SpiritGuard, new Pair<>((int) getSpiritGuard(), (int) getBuffLimit(getBuffSource(MapleBuffStat.SpiritGuard))));
                        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, getBuffedEffect(MapleBuffStat.SpiritGuard), this));
                    }
                } else if (skill.getDuration() > 0 || disease.getKey() == MapleBuffStat.Stigma) {
                    this.getDiseases().put(disease.getKey(), new Pair<>(skill, disease.getValue().left));
                }
            }

            dropMessageGM(6, "Debuff " + skill.getSkillId() + " " + skill.getSkillLevel() + " " + skill.getDuration());
            client.getSession().writeAndFlush(BuffPacket.giveDisease(diseases, skill, this));
            map.broadcastMessage(this, BuffPacket.giveForeignDeBuff(this, diseases), false);

            if (skill.getDuration() > 0) {
                BuffTimer.getInstance().schedule(new CancelDiseaseAction(this, diseases),
                        skill.getDuration());
            } else {
                client.getSession().writeAndFlush(BuffPacket.cancelBuff(diseases, this));
                map.broadcastMessage(this, BuffPacket.cancelForeignBuff(this, diseases), false);
            }

            if (curseBound < 5 && getBuffedEffect(MapleBuffStat.StopPortion) != null) {
                curseBound++;

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.StopPortion, new Pair<>((int) curseBound, (int) getBuffLimit(getBuffSource(MapleBuffStat.StopPortion))));
                getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, getBuffedEffect(MapleBuffStat.StopPortion), this));
            }
        }
    }

    public void cancelDisease(MapleBuffStat debuff) {
        if (diseases.containsKey(debuff)) {
            Pair<MobSkill, Integer> de = diseases.get(debuff);
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(debuff, new Pair<Integer, Integer>(de.getLeft().getX(), de.getRight()));
            cancelDisease(statups);
        }
    }

    public void cancelDisease(Map<MapleBuffStat, Pair<Integer, Integer>> statups) {
        Map<MapleBuffStat, Pair<Integer, Integer>> cancelList = new HashMap<>();
        if (statups != null) {
            for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
                if (!getDiseases().containsKey(statup.getKey())) {
                    continue;
                } else {
                    getDiseases().remove(statup.getKey());
                    cancelList.put(statup.getKey(), statup.getValue());
                }
            }
        }

        client.getSession().writeAndFlush(BuffPacket.cancelBuff(cancelList, this));
        map.broadcastMessage(this, BuffPacket.cancelForeignBuff(this, cancelList), false);

        if (cancelList.containsKey(MapleBuffStat.VampDeath)) {
            addHP(-getStat().getCurrentMaxHp());
        }

        if (cancelList.containsKey(MapleBuffStat.Lapidification)) {//석화 이후
            getStat().setHp(0, this);  //원래 못풀면 죽지않냐?
            //giveDebuff(MapleBuffStat.Stun, MobSkillFactory.getMobSkill(123, 57));
        }
    }

    public void dispelDebuff(Entry<MapleBuffStat, Pair<MobSkill, Integer>> d) {
        if (hasDisease(d.getKey())) {
            getDiseases().remove(d);
        }
    }

    public void dispelDebuffs() {
        Map<MapleBuffStat, Pair<Integer, Integer>> statupz = new HashMap<>();
        for (Entry<MapleBuffStat, Pair<MobSkill, Integer>> d : getDiseases().entrySet()) {
            dispelDebuff(d);
            statupz.put(d.getKey(), new Pair<>(d.getValue().right, (int) d.getValue().left.getDuration()));
        }

        client.getSession().writeAndFlush(BuffPacket.cancelBuff(statupz, this));
        map.broadcastMessage(this, BuffPacket.cancelForeignBuff(this, statupz), false);

        client.getSession().writeAndFlush(CWvsContext.enableActions(this));
    }

    public void cancelAllDebuffs() {
        getDiseases().clear();
    }

    public void setLevel(final short level) {
        this.level = (short) (level - 1); // why
    }

    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void showNote() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, getName());
            rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.getSession().writeAndFlush(CSPacket.showNotes(rs, count));
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
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
    }

    public void deleteNote(int id, int fame) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.FAME, getFame());
                    client.getSession().writeAndFlush(InfoPacket.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
            con.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
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
    }

    public int getMulungEnergy() {
        return mulung_energy;
    }

    public final short getCombo() {
        return combo;
    }

    public void setCombo(final short combo) {
        this.combo = combo;
    }

    public final long getLastCombo() {
        return lastCombo;
    }

    public void setLastCombo(final long combo) {
        this.lastCombo = combo;
    }

    public final boolean getUseTruthDoor() {
        return useTruthDoor;
    }

    public void setUseTruthDoor(final boolean used) {
        this.useTruthDoor = used;
    }

    public final long getKeyDownSkill_Time() {
        return keydown_skill;
    }

    public void setKeyDownSkill_Time(final long keydown_skill) {
        this.keydown_skill = keydown_skill;
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(CSPacket.useChalkboard(getId(), text));
        }
    }

    public String getChalkboard() {
        return chalktext;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void clearWishlist() {
        for (int i = 0; i < 12; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 12; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        rocks[getRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_regrocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        regrocks[getRegRockSize()] = getMapId();
        changed_regrocklocations = true;
    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_hyperrocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_hyperrocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public void dropMessageGM(int type, String message) {
        if (isGM()) {
            dropMessage(type, message);
        }
    }

    public void dropMessage(int type, String message) {
        if (type == -1) {
            client.getSession().writeAndFlush(CWvsContext.getTopMsg(message));
        } else if (type == -2) {
            client.getSession().writeAndFlush(PlayerShopPacket.shopChat(name, this.id, message, 0)); //0 or what
        } else if (type == -3) {
            client.getSession().writeAndFlush(CField.getChatText(this, message, isSuperGM(), 0, null)); //1 = hide
        } else if (type == -4) {
            client.getSession().writeAndFlush(CField.getChatText(this, message, isSuperGM(), 1, null)); //1 = hide
        } else if (type == -5) {
            client.getSession().writeAndFlush(CField.getGameMessage(6, message)); //pink
        } else if (type == -6) {
            client.getSession().writeAndFlush(CField.getGameMessage(11, message)); //white bg
        } else if (type == -7) {

            client.getSession().writeAndFlush(CWvsContext.getMidMsg(message, 0)); // itemid 0으로 하면 튕길거임 아마도.

        } else if (type == -8) {
            client.getSession().writeAndFlush(CField.getGameMessage(8, message)); //yellow
        } else {
            client.getSession().writeAndFlush(CWvsContext.serverNotice(type, name, message));
        }
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();
    }

    public boolean getCanTalk() {
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    public double getEXPMod() {
        return stats.expMod;
    }

    public int getDropMod() {
        return stats.dropMod;
    }

    public int getCashMod() {
        return stats.cashMod;
    }

    public void setPoints(int p) {
        this.points = p;
    }

    public int getPoints() {
        return points;
    }

    public void setVPoints(int p) {
        this.vpoints = p;
    }

    public int getVPoints() {
        return vpoints;
    }

    public void gainVPoints(int vpoints) {
        this.vpoints += vpoints;
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
        MapleInventoryManipulator.addFromDrop(client, equip, true);
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        client.getSession().writeAndFlush(InfoPacket.getShowItemGain(id, (short) quantity, false));
    }

    public void removeAll(int id) {
        removeAll(id, true);
    }

    public void removeAll(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().writeAndFlush(InfoPacket.getShowItemGain(id, (short) -possessed, false));
            }
        }
        if (type == MapleInventoryType.EQUIP || type == MapleInventoryType.DECORATION) { //check equipped
            type = MapleInventoryType.EQUIPPED;
            possessed = getInventory(type).countById(id);

            if (possessed > 0) {
                MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
                getClient().getSession().writeAndFlush(InfoPacket.getShowItemGain(id, (short) -possessed, false));
            }
        }
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        List<MapleRing> crings = new ArrayList<MapleRing>(), frings = new ArrayList<MapleRing>(), mrings = new ArrayList<MapleRing>();
        MapleRing ring;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (GameConstants.isEffectRing(item.getItemId())) {
                    if (equip) {
                        if (GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        }
                    } else if (crings.size() == 0 && GameConstants.isCrushRing(item.getItemId())) {
                        crings.add(ring);
                    } else if (frings.size() == 0 && GameConstants.isFriendshipRing(item.getItemId())) {
                        frings.add(ring);
                    } else if (mrings.size() == 0 && GameConstants.isMarriageRing(item.getItemId())) {
                        if (getMarriageId() > 0) {
                            mrings.add(ring);
                        }
                    } //for 3rd person the actual slot doesnt matter, so we'll use this to have both shirt/ring same?
                    //however there seems to be something else behind this, will have to sniff someone with shirt and ring, or more conveniently 3-4 of those
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null) {//GameConstants.isCrushRing(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (GameConstants.isFriendshipRing(item.getItemId())) {
                        frings.add(ring);
                    } else if (GameConstants.isCrushRing(item.getItemId())) {
                        crings.add(ring);
                    } else if (GameConstants.isMarriageRing(item.getItemId())) {
                        if (getMarriageId() > 0) {
                            mrings.add(ring);
                        }
                    }
                }
            }
        }
        Collections.sort(frings, new MapleRing.RingComparator());
        Collections.sort(crings, new MapleRing.RingComparator());
        Collections.sort(mrings, new MapleRing.RingComparator());
        return new Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>>(crings, frings, mrings);
    }

    public int getFH() {
        MapleFoothold fh = getMap().getFootholds().findBelow(getTruePosition());
        if (fh != null) {
            return fh.getId();
        }
        return 0;
    }

    public void startFairySchedule(boolean exp) {
        startFairySchedule(exp, false);
    }

    public void startFairySchedule(boolean exp, boolean equipped) {
        cancelFairySchedule(exp || stats.equippedFairy == 0);
        if (fairyExp <= 0) {
            fairyExp = (byte) stats.equippedFairy;
        }
        if (equipped && fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            dropMessage(5, "정령의 펜던트를 착용한지 1시간이 지나 " + (fairyExp + stats.equippedFairy) + "%의 추가 경험치를 획득합니다.");
        }
        lastFairyTime = System.currentTimeMillis();
    }

    public final boolean canFairy(long now) {
        return lastFairyTime > 0 && lastFairyTime + (60 * 60 * 1000) < now;
    }

    public final boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public void cancelFairySchedule(boolean exp) {
        lastFairyTime = 0;
        if (exp) {
            this.fairyExp = 0;
        }
    }

    public void doFairy() {
        if (fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            fairyExp += stats.equippedFairy;
            dropMessage(5, "정령의 펜던트를 통해 " + fairyExp + "%의 추가경험치를 획득합니다.");
        }
        traits.get(MapleTraitType.will).addExp(5, this); //willpower every hour
        startFairySchedule(false, true);
    }

    public byte getFairyExp() {
        return fairyExp;
    }

    public int getTeam() {
        return coconutteam;
    }

    public void setTeam(int v) {
        this.coconutteam = v;
    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.ArcaneAim);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm.intValue();
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public MapleCharacter cloneLooks() {
        MapleClient cs = new MapleClient(client.getSession(), null, null);

        final int minus = (getId() + Randomizer.nextInt(Integer.MAX_VALUE - getId())); // really randomize it, dont want it to fail

        MapleCharacter ret = new MapleCharacter(true);
        ret.id = minus;
        ret.client = cs;
        ret.exp = 0;
        ret.meso = 0;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.name = name;
        ret.level = level;
        ret.fame = fame;
        ret.job = job;
        ret.hair = hair;
        ret.face = face;
        ret.demonMarking = demonMarking;
        ret.skinColor = skinColor;
        ret.mount = mount;
        ret.setCalcDamage(new CalcDamage());
        ret.gmLevel = gmLevel;
        ret.LinkMobCount = LinkMobCount;
        ret.gender = gender;
        ret.mapid = map.getId();
        ret.map = map;
        ret.setStance(getStance());
        ret.chair = chair;
        ret.itemEffect = itemEffect;
        ret.guildid = guildid;
        ret.currentrep = currentrep;
        ret.totalrep = totalrep;
        ret.stats = stats;
        ret.effects = effects;
        ret.dispelSummons();
        ret.guildrank = guildrank;
        ret.guildContribution = guildContribution;
        ret.allianceRank = allianceRank;
        ret.setPosition(getTruePosition());
        for (Item equip : getInventory(MapleInventoryType.EQUIPPED).newList()) {
            ret.getInventory(MapleInventoryType.EQUIPPED).addFromDB(equip.copy());
        }
        ret.skillMacros = skillMacros;
        ret.keylayout = keylayout;
        ret.questinfo = questinfo;
        ret.savedLocations = savedLocations;
        ret.wishlist = wishlist;
        ret.buddylist = buddylist;
        ret.keydown_skill = 0;
        ret.lastmonthfameids = lastmonthfameids;
        ret.lastfametime = lastfametime;
        ret.storage = storage;
        ret.cs = this.cs;
        ret.client.setAccountName(client.getAccountName());
        ret.acash = acash;
        ret.maplepoints = maplepoints;
        ret.client.setChannel(this.client.getChannel());
        while (map.getCharacterById(ret.id) != null || client.getChannelServer().getPlayerStorage().getCharacterById(ret.id) != null) {
            ret.id++;
        }
        ret.client.setPlayer(ret);
        return ret;
    }

    public void setDragon(MapleDragon d) {
        this.dragon = d;
    }

    public MapleExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(MapleExtractor me) {
        removeExtractor();
        this.extractor = me;
    }

    public void removeExtractor() {
        if (extractor != null) {
            map.broadcastMessage(CField.removeExtractor(this.id));
            map.removeMapObject(extractor);
            extractor = null;
        }
    }

    public void resetStatsN(final int str, final int dex, final int int_, final int luk) {
        Map<MapleStat, Long> stat = new EnumMap<MapleStat, Long>(MapleStat.class
        );
        stats.str = (short) str;
        stats.dex = (short) dex;
        stats.int_ = (short) int_;
        stats.luk = (short) luk;
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, (long) str);
        stat.put(MapleStat.DEX, (long) dex);
        stat.put(MapleStat.INT, (long) int_);
        stat.put(MapleStat.LUK, (long) luk);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(stat, false, this));
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
        Map<MapleStat, Long> stat = new EnumMap<MapleStat, Long>(MapleStat.class
        );
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();
        total -= str;
        stats.str = (short) str;
        total -= dex;
        stats.dex = (short) dex;
        total -= int_;
        stats.int_ = (short) int_;
        total -= luk;
        stats.luk = (short) luk;
        setRemainingAp((short) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, (long) str);
        stat.put(MapleStat.DEX, (long) dex);
        stat.put(MapleStat.INT, (long) int_);
        stat.put(MapleStat.LUK, (long) luk);
        stat.put(MapleStat.AVAILABLEAP, (long) total);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(stat, false, this));
    }

    public void resetStatDonation(int tf, int tft) {
        Map<MapleStat, Long> stat = new EnumMap<MapleStat, Long>(MapleStat.class
        );
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();
        int tstat = tft == 1 ? tf * 200 : tft == 2 ? tf * 100 : 4;
        if (tft == 2 && tf == 6) {
            tstat += 100;
        }

        total -= tstat;
        stats.str = (short) tstat;
        total -= tstat;
        stats.dex = (short) tstat;
        total -= tstat;
        stats.int_ = (short) tstat;
        total -= tstat;
        stats.luk = (short) tstat;
        setRemainingAp((short) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, (long) tstat);
        stat.put(MapleStat.DEX, (long) tstat);
        stat.put(MapleStat.INT, (long) tstat);
        stat.put(MapleStat.LUK, (long) tstat);
        stat.put(MapleStat.AVAILABLEAP, (long) total);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(stat, false, this));
    }

    public void resetStatsDV() {
        Map<MapleStat, Long> stat = new EnumMap<MapleStat, Long>(MapleStat.class
        );
        int apss = (getLevel() - 10) * 5 * 15;
        int total = ((getLevel() - 10) * 5) + getRemainingAp();
        stats.setMaxHp(getStat().getMaxHp() - apss, this);
        setRemainingAp((short) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.MAXHP, (long) getStat().getMaxHp());
        stat.put(MapleStat.AVAILABLEAP, (long) total);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(stat, false, this));
    }

    public byte getSubcategory() {
        if (job >= 430 && job <= 434) {
            return 1; //dont set it
        }
        if (GameConstants.isCannon(job)) {
            return 2;
        }
        if (job != 0 && job != 400) {
            return 0;
        }
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public int itemQuantity(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).countById(itemid);
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public void changeChannelMap(int channel, int map) {
        final ChannelServer toch = ChannelServer.getInstance(channel);

        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            return;
        }
        changeRemoval();

        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        getMap().removePlayer(this);
        this.map = toch.getMapFactory().getMap(map);
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), channel);
        ch.removePlayer(this);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        client.getSession().writeAndFlush(CField.getChannelChange(client, Integer.parseInt(toch.getIP().split(":")[1])));
        new MapleCharacterSave(this).saveToDB(this, true, false);
        client.setPlayer(null);
//        client.setReceiving(false);

        if (OneCardGame.oneCardMatchingQueue.contains(this)) {
            OneCardGame.oneCardMatchingQueue.remove(this);
        }

        if (BattleReverse.BattleReverseMatchingQueue.contains(this)) {
            BattleReverse.BattleReverseMatchingQueue.remove(this);
        }
    }

    public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(channel);

        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            return;
        }
        changeRemoval();

        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), channel);
        ch.removePlayer(this);
        client.setChannel(channel);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        client.getSession().writeAndFlush(CField.getChannelChange(client, Integer.parseInt(toch.getIP().split(":")[1])));
        new MapleCharacterSave(this).saveToDB(this, true, false);
        getMap().removePlayer(this);
        client.setPlayer(null);
//        client.setReceiving(false);

        if (OneCardGame.oneCardMatchingQueue.contains(this)) {
            OneCardGame.oneCardMatchingQueue.remove(this);
        }

        if (BattleReverse.BattleReverseMatchingQueue.contains(this)) {
            BattleReverse.BattleReverseMatchingQueue.remove(this);
        }
    }

    public void expandInventory(byte type, int amount) {
        final MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((byte) amount);
        client.getSession().writeAndFlush(InventoryPacket.getSlotUpdate(type, (byte) inv.getSlotLimit()));
    }

    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && (!other.isHidden() || getGMLevel() >= other.getGMLevel());
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowOn() {
        return followon;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }

        if (followon) {
            map.broadcastMessage(CField.followEffect(id, 0, null));
            map.broadcastMessage(CField.followEffect(followid, 0, null));
        }

        MapleCharacter target = map.getCharacter(followid);

        client.getSession().writeAndFlush(CField.getGameMessage(11, "따라가기가 해제되었습니다."));
        if (target != null) {
            target.setFollowId(0);
            target.getClient().getSession().writeAndFlush(CField.getGameMessage(11, "따라가기가 해제되었습니다."));
            setFollowId(0);
        }
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(final int mi) {
        this.marriageId = mi;
    }

    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.marriageItemId = mi;
    }

    public boolean isStaff() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.INTERN.getLevel();
    }

    public boolean isDonator() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.DONATOR.getLevel();
    }

    // TODO: gvup, vic, lose, draw, VR
    public boolean startPartyQuest(final int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }

        if (!quests.containsKey(q) || !questinfo.containsKey(questid)) {
            final MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300:
                case 1301:
                case 1302: //carnival, ariants.
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: //ghost pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0");
                    break;
                case 1204: //herb town pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: //ellin pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        }
        return ret;
    }

    public String getOneInfo(final int questid, final String key) {
        if (!questinfo.containsKey(questid) || key == null || MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return null;
        }
        final String[] split = questinfo.get(questid).split(";");
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        if (!questinfo.containsKey(questid) || key == null || value == null || MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        final String[] split = questinfo.get(questid).split(";");
        boolean changed = false;
        final StringBuilder newQuest = new StringBuilder();
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                newQuest.append(key).append("=").append(value);
            } else {
                newQuest.append(x);
            }
            newQuest.append(";");
            changed = true;
        }

        updateInfoQuest(questid, changed ? newQuest.toString().substring(0, newQuest.toString().length() - 1) : newQuest.toString());
    }

    public void updateSkillPacket() {
        client.getSession().writeAndFlush(CWvsContext.updateSkills(this.getSkills()));
    }

    public void updateLinkSkillPacket() {
        changeSingleSkillLevel(SkillFactory.getSkill(80000055), 0, (byte) 10);
        changeSingleSkillLevel(SkillFactory.getSkill(80000329), 0, (byte) 8);
        changeSingleSkillLevel(SkillFactory.getSkill(80002758), 0, (byte) 6);
        changeSingleSkillLevel(SkillFactory.getSkill(80002762), 0, (byte) 6);
        changeSingleSkillLevel(SkillFactory.getSkill(80002766), 0, (byte) 6);
        changeSingleSkillLevel(SkillFactory.getSkill(80002770), 0, (byte) 6);
        changeSingleSkillLevel(SkillFactory.getSkill(80002774), 0, (byte) 6);
        final List<Triple<Skill, SkillEntry, Integer>> skills = this.getLinkSkills();
        for (Triple<Skill, SkillEntry, Integer> linkskil : skills) {
            if (getSkillLevel(linkskil.getLeft().getId()) != 0) {
                int totalskilllv = 0;
                if (linkskil.getLeft().getId() >= 80000066 && linkskil.getLeft().getId() <= 80000070) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80000055);
                    changeSingleSkillLevel(SkillFactory.getSkill(80000055), totalskilllv, (byte) 10);
                }
                if (linkskil.getLeft().getId() >= 80000333 && linkskil.getLeft().getId() <= 80000335 || linkskil.getLeft().getId() == 80000378) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80000329);
                    changeSingleSkillLevel(SkillFactory.getSkill(80000329), totalskilllv, (byte) 8);
                }
                if (linkskil.getLeft().getId() >= 80002759 && linkskil.getLeft().getId() <= 80002761) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80002758);
                    changeSingleSkillLevel(SkillFactory.getSkill(80002758), totalskilllv, (byte) 6);
                }
                if (linkskil.getLeft().getId() >= 80002763 && linkskil.getLeft().getId() <= 80002765) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80002762);
                    changeSingleSkillLevel(SkillFactory.getSkill(80002762), totalskilllv, (byte) 6);
                }
                if (linkskil.getLeft().getId() >= 80002767 && linkskil.getLeft().getId() <= 80002769) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80002766);
                    changeSingleSkillLevel(SkillFactory.getSkill(80002766), totalskilllv, (byte) 6);
                }
                if (linkskil.getLeft().getId() >= 80002771 && linkskil.getLeft().getId() <= 80002773) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80002770);
                    changeSingleSkillLevel(SkillFactory.getSkill(80002770), totalskilllv, (byte) 6);
                }
                if (linkskil.getLeft().getId() >= 80002775 && linkskil.getLeft().getId() <= 80002776 || linkskil.getLeft().getId() == 80000000) {
                    totalskilllv = linkskil.getMid().skillevel + getSkillLevel(80002774);
                    changeSingleSkillLevel(SkillFactory.getSkill(80002774), totalskilllv, (byte) 6);
                }
                getClient().getSession().writeAndFlush(CWvsContext.Linkskill(linkskil.getLeft().getId(), linkskil.getRight(), getId(), linkskil.getMid().skillevel, totalskilllv));
            }
        }
    }

    public void recalcPartyQuestRank(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            final String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank = null;
            if (oldRank.equals("A")) {
                newRank = "S";
            } else if (oldRank.equals("B")) {
                newRank = "A";
            } else if (oldRank.equals("C")) {
                newRank = "B";
            } else if (oldRank.equals("D")) {
                newRank = "C";
            } else if (oldRank.equals("F")) {
                newRank = "D";
            } else {
                return;
            }
            final List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid).getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            for (Pair<String, Pair<String, Integer>> q : questInfo) {
                boolean found = false;
                final String val = getOneInfo(questid, q.right.left);
                if (val == null) {
                    return;
                }
                int vall = 0;
                try {
                    vall = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return;
                }
                if (q.left.equals("less")) {
                    found = vall < q.right.right;
                } else if (q.left.equals("more")) {
                    found = vall > q.right.right;
                } else if (q.left.equals("equal")) {
                    found = vall == q.right.right;
                }
                if (!found) {
                    return;
                }
            }
            //perfectly safe
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public int getIgnition() {
        return ignitionstack;
    }

    public void setIgnition(int stack) {
        ignitionstack = stack;
        MapleStatEffect effect = SkillFactory.getSkill(12101024).getEffect(getTotalSkillLevel(12101024));
        EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class
        );
        statups.put(MapleBuffStat.Ember, new Pair<>(1, (int) getBuffLimit(12101024)));
        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
    }

    public void gainIgnition() {
        if (ignitionstack < 6) {
            ignitionstack++;
        }
        MapleStatEffect effect = SkillFactory.getSkill(12101024).getEffect(getTotalSkillLevel(12101024));
        EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class
        );
        statups.put(MapleBuffStat.Ember, new Pair<>(1, (int) getBuffLimit(12101024)));
        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
    }

    public void gainfamilier() {
        if (ignitionstack < 6) {
            ignitionstack++;
        }
        MapleStatEffect effect = SkillFactory.getSkill(12101024).getEffect(getTotalSkillLevel(12101024));
        EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class
        );
        statups.put(MapleBuffStat.Ember, new Pair<>(1, (int) getBuffLimit(12101024)));
        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
    }

    public void tryPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (pqStartTime > 0) {
                final long changeTime = System.currentTimeMillis() - pqStartTime;
                final int mins = (int) (changeTime / 1000 / 60), secs = (int) (changeTime / 1000 % 60);
                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));
                if (mins2 <= 0 || mins < mins2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
                }
                final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("endPartyQuest error");
        }

    }

    public void havePartyQuest(final int itemId) {
        int questid = 0, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; //henesys
                break;
            case 1072369:
                questid = 1201; //kerning
                break;
            case 1022073:
                questid = 1202; //ludi
                break;
            case 1082232:
                questid = 1203; //orbis
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; //herbtown
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; //ghost
                break;
            case 1102227:
                questid = 1303; //ghost
                index = 0;
                break;
            case 1122010:
                questid = 1205; //magatia
                break;
            case 1032061:
            case 1032060:
                questid = 1206; //ellin
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; //ariant
                break;
            case 1122007:
                questid = 1301; //carnival
                break;
            case 1122058:
                questid = 1302; //carnival2
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");
    }

    public void resetStatsByJob(boolean beginnerJob) {
        int baseJob = (beginnerJob ? (job % 1000) : (((job % 1000) / 100) * 100)); //1112 -> 112 -> 1 -> 100
        boolean UA = getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER)) != null;
        if (baseJob == 100) { //first job = warrior
            resetStats(UA ? 4 : 35, 4, 4, 4);
        } else if (baseJob == 200) {
            resetStats(4, 4, UA ? 4 : 20, 4);
        } else if (baseJob == 300 || baseJob == 400) {
            resetStats(4, UA ? 4 : 25, 4, 4);
        } else if (baseJob == 500) {
            resetStats(4, UA ? 4 : 20, 4, 4);
        } else if (baseJob == 0) {
            resetStats(4, 4, 4, 4);
        }
    }

    public boolean hasSummon(int sourceid) {

        for (MapleSummon summon : summons) {
            if (summon.getSkill() == sourceid) {
                return true;
            }
        }
        return false;
    }

    public void removeDoor() {
        final MapleDoor door = getDoors().iterator().next();
        for (final MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (final MechDoor destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        dispelSummons();
        if (!dc) {
//            cancelEffectFromBuffStat(MapleBuffStat.SOARING);
            cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
            cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
            cancelEffectFromBuffStat(MapleBuffStat.Recovery);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        NPCScriptManager.getInstance().dispose(client);
        cancelFairySchedule(false);
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(final String tname) {
        teleportname = tname;
    }

    public int maxBattleshipHP(int skillid) {
        return (getTotalSkillLevel(skillid) * 5000) + ((getLevel() - 120) * 3000);
    }

    public int currentBattleshipHP() {
        return battleshipHP;
    }

    public void setBattleshipHP(int v) {
        this.battleshipHP = v;
    }

    public void decreaseBattleshipHP() {
        this.battleshipHP--;
    }

    public boolean isInTownMap() {
        if (!getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit()) || getEventInstance() != null) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }

    public void startPartySearch(final List<Integer> jobs, final int maxLevel, final int minLevel, final int membersNeeded) {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel && (jobs.isEmpty() || jobs.contains(Integer.valueOf(chr.getJob()))) && (isGM() || !chr.isGM())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < membersNeeded) {
                    chr.setParty(party);
                    World.Party.updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(chr));
                    chr.receivePartyMemberHP();
                    chr.updatePartyMemberHP();
                } else {
                    break;
                }
            }
        }
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public short getFatigue() {
        return fatigue;
    }

    public void setFatigue(int j) {
        this.fatigue = (short) Math.max(0, j);
        updateSingleStat(MapleStat.FATIGUE, this.fatigue);
    }

    public void updateDamageSkin() {
        client.getSession().writeAndFlush(CWvsContext.updateDamageSkin(this));
    }

    public void fakeRelog() {
        client.getSession().writeAndFlush(CField.getCharInfo(this));
        final MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);
    }

    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int g) {
        if (lastSummonTime + g < System.currentTimeMillis()) {
            lastSummonTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getIntNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public void updatePetAuto() {
        if (getIntNoRecord(GameConstants.HP_ITEM) > 0) {
            client.getSession().writeAndFlush(CField.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        }
        if (getIntNoRecord(GameConstants.MP_ITEM) > 0) {
            client.getSession().writeAndFlush(CField.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        }
    }

    public void sendEnglishQuiz(String msg) {
        //client.getSession().writeAndFlush(CField.englishQuizMsg(msg));
    }

    public void setChangeTime() {
        mapChangeTime = System.currentTimeMillis();
    }

    public long getChangeTime() {
        return mapChangeTime;
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public MapleTrait getTrait(MapleTraitType t) {
        return traits.get(t);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 2007);
    }

    public List<Integer> getExtendedSlots() {
        return extendedSlots;
    }

    public int getExtendedSlot(int index) {
        if (extendedSlots.size() <= index || index < 0) {
            return -1;
        }
        return extendedSlots.get(index);
    }

    public void changedExtended() {
        changed_extendedSlots = true;
    }

    public MapleAndroid getAndroid() {
        return android;
    }

    public void removeAndroid() {
        if (map != null) {
            map.broadcastMessage(CField.deactivateAndroid(this.id));
        }
        android = null;
    }

    public void setAndroid(MapleAndroid and) {
        this.android = and;
        if (map != null && and != null) { //Set
            android.setStance(0);
            android.setPos(getPosition());
            map.broadcastMessage(this, CField.spawnAndroid(this, android), true);
            map.broadcastMessage(this, CField.showAndroidEmotion(this.getId(), Randomizer.nextInt(17) + 1), true);
        } else if (map != null && and == null) { //Remove
            map.broadcastMessage(this, CField.deactivateAndroid(this.getId()), true);
        }
    }

    public void updateAndroid() {
        if (map != null && android != null) { //Set
            map.broadcastMessage(this, CField.spawnAndroid(this, android), true);
        } else if (map != null && android == null) { //Remove
            map.broadcastMessage(this, CField.deactivateAndroid(this.getId()), true);
        }
    }

    public List<Item> getRebuy() {
        return rebuy;
    }

    public MapleImp[] getImps() {
        return imps;
    }

    public int getBattlePoints() {
        return pvpPoints;
    }

    public int getTotalBattleExp() {
        return pvpExp;
    }

    public void setBattlePoints(int p) {
        if (p != pvpPoints) {
            client.getSession().writeAndFlush(InfoPacket.getBPMsg(p - pvpPoints));
            updateSingleStat(MapleStat.BATTLE_POINTS, p);
        }
        this.pvpPoints = p;
    }

    public int getPeaceMaker() {
        return PeaceMaker;
    }

    /**
     * @param PeaceMaker the PeaceMaker to set
     */
    public void setPeaceMaker(int PeaceMaker) {
        this.PeaceMaker = PeaceMaker;
    }

    public void setTotalBattleExp(int p) {
        final int previous = pvpExp;
        this.pvpExp = p;
        if (p != previous) {
            stats.recalcPVPRank(this);

            updateSingleStat(MapleStat.BATTLE_EXP, stats.pvpExp);
            updateSingleStat(MapleStat.BATTLE_RANK, stats.pvpRank);
        }
    }

    public boolean inPVP() {
        return eventInstance != null && eventInstance.getName().startsWith("PVP");
    }

    public void clearCooldowns(List<MapleCoolDownValueHolder> cooldowns) {
        Map<Integer, Integer> datas = new HashMap<>();
        for (MapleCoolDownValueHolder m : cooldowns) {
            final int skil = m.skillId;
            removeCooldown(skil);
            datas.put(skil, 0);
        }
        client.getSession().writeAndFlush(CField.skillCooldown(datas));
    }

    public void clearAllCooldowns() {
        Map<Integer, Integer> datas = new HashMap<>();
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            removeCooldown(skil);
            datas.put(skil, 0);
        }
        client.getSession().writeAndFlush(CField.skillCooldown(datas));
    }

    public void clearAllCooldowns(int skillid) {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            if (skil != skillid) {
                removeCooldown(skil);
                client.getSession().writeAndFlush(CField.skillCooldown(skil, 0));
            }
        }
    }

    public void handleForceGain(int oid, int skillid) {
        handleForceGain(oid, skillid, 0);
    }

    public void handleForceGain(int oid, int skillid, int extraForce) {
        if (skillid == 31121052) {
            extraForce = 50;
        }
        int forceGain = 1;
        if (getLevel() >= 30 && getLevel() < 70) {
            forceGain = 2;
        } else if (getLevel() >= 70 && getLevel() < 120) {
            forceGain = 3;
        } else if (getLevel() >= 120) {
            forceGain = 4;
        }
        if (getCooldownLimit(31121054) > 0) {
            forceBlood += extraForce > 0 ? extraForce : forceGain;
            if (forceBlood >= 50) {
                changeCooldown(31121054, -3000);
                forceBlood -= 50;
            }
        }
        force++; // counter
        addMP(extraForce > 0 ? extraForce : forceGain, true);

        MapleAtom atom = new MapleAtom(false, id, 0, true, skillid, getTruePosition().x, getTruePosition().y);
        atom.addForceAtom(new ForceAtom(forceGain, 46, 6, 31, 0));

        atom.setDwFirstTargetId(0);
        getMap().spawnMapleAtom(atom);
    }

    public void afterAttack(AttackInfo attack) {
        int skillid = attack.skill;

        switch (getJob()) {
            case 110:
            case 111:
            case 112:
                if (!PlayerHandler.isFinisher(skillid) & getBuffedValue(MapleBuffStat.ComboCounter) != null) { // shout should not give orbs
                    handleOrbgain(skillid);
                }
                break;
        }
        /*        if (getBuffedValue(MapleBuffStat.OWL_SPIRIT) != null) {
         if (currentBattleshipHP() > 0) {
         decreaseBattleshipHP();
         }
         if (currentBattleshipHP() <= 0) {
         cancelEffectFromBuffStat(MapleBuffStat.OWL_SPIRIT);
         }
         } */
        if (!isIntern()) {
            cancelEffectFromBuffStat(MapleBuffStat.WindWalk);
            cancelEffectFromBuffStat(MapleBuffStat.Infiltrate);
        }
    }

    public void applyIceGage(int x) {
        updateSingleStat(MapleStat.ICE_GAGE, x);
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    public Map<Short, Integer> getEquips() {
        final Map<Short, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            int itemId = item.getItemId();
            eq.put(item.getPosition(), itemId);
        }
        return eq;
    }

    public Map<Short, Integer> getSecondEquips() {
        final Map<Short, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            int itemId = item.getItemId();
            if (item instanceof Equip) {
                /*  if (GameConstants.isAngelicBuster(getJob()) && GameConstants.isOverall(itemId)) {
                 itemId = 1051291; //ab def overall
                 }*/
            }
            /* if (GameConstants.isAngelicBuster(getJob())) {
             if (!GameConstants.isOverall(itemId) && !GameConstants.isSecondaryWeapon(itemId)
             && !GameConstants.isWeapon(itemId) && !GameConstants.isMedal(itemId)) {
             continue;
             }
             }*/
            eq.put(item.getPosition(), itemId);
        }
        return eq;
    }


    /*Start of Custom Feature*/
    public int getReborns() {
        return reborns;
    }

    public void setReborns(int data) {
        this.reborns = data;
    }

    public int getAPS() {
        return apstorage;
    }

    public void gainAPS(int aps) {
        apstorage += aps;
    }

    public void doReborn() {
        Map<MapleStat, Long> stat = new EnumMap<>(MapleStat.class
        );
        this.reborns += 1;
        setLevel((short) 12); // = 11
        setExp(0);
        setRemainingAp((short) 0);
        final int oriStats = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt();
        final int str = Randomizer.rand(25, stats.getStr());
        final int dex = Randomizer.rand(25, stats.getDex());
        final int int_ = Randomizer.rand(25, stats.getInt());
        final int luk = Randomizer.rand(25, stats.getLuk());
        final int afterStats = str + dex + int_ + luk;
        final int MAS = (oriStats - afterStats) + getRemainingAp();
        client.getPlayer().gainAPS(MAS);
        stats.recalcLocalStats(this);
        stats.setStr((short) str, client.getPlayer());
        stats.setDex((short) dex, client.getPlayer());
        stats.setInt((short) int_, client.getPlayer());
        stats.setLuk((short) luk, client.getPlayer());
        stat.put(MapleStat.STR, (long) str);
        stat.put(MapleStat.DEX, (long) dex);
        stat.put(MapleStat.INT, (long) int_);
        stat.put(MapleStat.LUK, (long) luk);
        stat.put(MapleStat.AVAILABLEAP, (long) 0);
        updateSingleStat(MapleStat.LEVEL, 11);
        updateSingleStat(MapleStat.JOB, 0); // check in Command instead. @rb, @rbd, @rbc.. whatever..
        updateSingleStat(MapleStat.EXP, 0);
        client.getSession().writeAndFlush(CWvsContext.updatePlayerStats(stat, false, this));
    }

    /*End of Custom Feature*/
    public List<InnerSkillValueHolder> getInnerSkills() {
        return innerSkills;
    }

    public int getHonourExp() {
        return honourExp;
    }

    public void setHonourExp(int exp) {
        this.honourExp = exp;
    }

    public int getHonorLevel() {
        if (honorLevel == 0) {
            honorLevel++;
        }
        return honorLevel;
    }

    public int getHonourNextExp() {
        if (getHonorLevel() == 0) {
            return 0;
        }
        return (getHonorLevel() + 1) * 500;
    }

    public Map<Byte, Integer> getTotems() {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            eq.put((byte) (item.getPosition() + 5000), item.getItemId());
        }
        return eq;
    }

    public void setCardStack(byte amount) {
        this.cardStack = amount;
    }

    public byte getCardStack() {
        return cardStack;
    }

    public final MapleCharacterCards getCharacterCard() {
        return characterCard;
    }

    public void setPetLoot(boolean status) {
        this.petLoot = status;
    }

    public boolean getPetLoot() {
        return petLoot;
    }

    public int getStorageNPC() {
        return storageNpc;
    }

    public void setStorageNPC(int id) {
        this.storageNpc = id;
    }

    public boolean getPvpStatus() {
        return pvp;
    }

    public void togglePvP() {
        pvp = !pvp;
    }

    public void enablePvP() {
        pvp = true;
    }

    public void disablePvP() {
        pvp = false;
    }

    public void addHonorExp(int amount) {
        setHonourExp(getHonourExp() + amount);
        client.getSession().writeAndFlush(CWvsContext.updateAzwanFame(getHonourExp()));
//        client.getSession().writeAndFlush(CWvsContext.enableActions(this));
    }

    public void gainHonor(int honor) {
        addHonorExp(honor);
        if (getKeyValue(5, "show_honor") > 0) {
            dropMessage(5, "명성치 " + honor + "을 얻었습니다.");
        }
    }

    public List<Integer> HeadTitle() {
        List<Integer> num_ = new ArrayList<Integer>();
        num_.add(Integer.valueOf(0));
        num_.add(Integer.valueOf(0));
        num_.add(Integer.valueOf(0));
        num_.add(Integer.valueOf(0));
        num_.add(Integer.valueOf(0));
        return num_;
    }

    public int getInternetCafeTime() {
        return itcafetime;
    }

    public void setInternetCafeTime(int itcafetime) {
        this.itcafetime = itcafetime;
    }

    public void InternetCafeTimer() {
        if (itcafetimer != null) {
            itcafetimer.cancel(false);
        }
        itcafetimer = Timer.CloneTimer.getInstance().register((new Runnable() {
            public void run() {
                if (getInternetCafeTime() < 1) {
                    client.getSession().writeAndFlush(CField.getInternetCafe((byte) 4, 0));
                    return;
                }
                setInternetCafeTime(getInternetCafeTime() - 1);
            }
        }), 1000 * 60);
    }

    public short getMonsterCombo() {
        return monsterCombo;
    }

    public void setMonsterCombo(short count) {
        monsterCombo = count;
    }

    public void addMonsterCombo(short amount) {
        monsterCombo += amount;
    }

    public long getMonsterComboTime() {
        return monsterComboTime;
    }

    public void setMonsterComboTime(long count) {
        monsterComboTime = count;
    }

    public long getRuneTimeStamp() {
        return lastTouchedRuneTime;
    }

    public void setRuneTimeStamp(long lastTouchedRuneTime) {
        this.lastTouchedRuneTime = lastTouchedRuneTime;
    }

    public int getTouchedRune() {
        return TouchedRune;
    }

    public void setTouchedRune(int type) {
        TouchedRune = type;
    }

    /* skillInfo */
    public ScheduledFuture<?> rapidtimer1 = null, rapidtimer2 = null;
    public int trueSniping = 0;
    public int shadowAssault = 0;
    public int transformEnergyOrb = 0;
    public boolean isMegaSmasherCharging = false;
    public long megaSmasherChargeStartTime = 0, lastWindWallTime = 0, startThrowBlastingTime = 0;
    public long lastArrowRain = 0;
    public byte battAttackCount = 0;
    public int criticalGrowing = 0;
    public int criticalDamageGrowing = 0;
    public int bodyOfSteal = 0;
    public byte mCount = 0;
    public boolean energyCharge = false;
    public int lightning = 0;
    public int siphonVitality = 0;
    public int armorSplit = 0;
    public int PPoint = 0;
    public int combination = 0;
    public int killingpoint = 0;
    public int stackbuff = 0;
    public int combinationBuff = 0;
    public byte Bullet = 6, Cylinder = 0;
    public int BULLET_SKILL_ID = 0;
    public int SpectorGauge = 0;
    public int LinkofArk = 0;
    public int FlowofFight = 0;
    public int Stigma = 0;
    public int bulletParty = 0;

    public void cancelRapidTime(final byte type) {

        if (type == 1) {
            if (rapidtimer1 != null) {
                rapidtimer1.cancel(false);
            }
            rapidtimer1 = BuffTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    changeSkillLevel(SkillFactory.getSkill(100000276), (byte) 0, (byte) 0);
                }
            }, 20000);
        } else if (type == 2) {
            if (rapidtimer2 != null) {
                rapidtimer2.cancel(false);
            }
            rapidtimer2 = BuffTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    changeSkillLevel(SkillFactory.getSkill(100000277), (byte) 0, (byte) 0);
                }
            }, 20000);
        }
    }

    public long getnHPoint() {
        try {
            return Long.parseLong(client.getKeyValue("nHpoint"));
        } catch (Exception e) {
            return 0;
        }
    }

    public int getHgrade() {
        try {
            return Integer.parseInt(client.getKeyValue("hGrade"));
        } catch (Exception e) {
            return 0;
        }
    }

    public void setHgrade(int a) {
        client.setKeyValue("hGrade", String.valueOf(a));
    }

    public String getHgrades() {
        switch (getHgrade()) {
            case 1:
                return "MVP브론즈";
            case 2:
                return "MVP실버";
            case 3:
                return "MVP골드";
            case 4:
                return "MVP다이아";
            case 5:
                return "MVP레드";
            case 6:
                return "MVP크라운";
            default:
                return "일반";
        }
    }

    public int getPgrade() {
        try {
            return Integer.parseInt(client.getKeyValue("pGrade"));
        } catch (Exception e) {
            return 0;
        }
    }

    public void setPgrade(int a) {
        client.setKeyValue("pGrade", String.valueOf(a));
    }

    public String getPgrades() {
        switch (client.getKeyValue("pGrade")) {
            case "1":
                return "비기닝";
            case "2":
                return "라이징";
            case "3":
                return "플라잉";
            case "4":
                return "샤이닝";
            case "5":
                return "아이돌";
            case "6":
                return "슈퍼스타";
            default:
                return "일반";
        }
    }

    public void gainnHPoint(int a) {
        client.setKeyValue("nHpoint", String.valueOf(getHPoint() + a));
    }

    public void setnHPoint(int a) {
        client.setKeyValue("nHpoint", String.valueOf(a));
    }

    public long getnDonationPoint() {
        try {
            return Long.parseLong(client.getKeyValue("nDpoint"));
        } catch (Exception e) {
            return 0;
        }
    }

    public void gainnDonationPoint(int a) {
        client.setKeyValue("nDpoint", String.valueOf(getnDonationPoint() + a));
    }

    public void setnDonationPoint(int a) {
        client.setKeyValue("nDpoint", String.valueOf(a));
    }

    public long getHPoint() {
        try {
            return Long.parseLong(client.getKeyValue("HPoint"));
        } catch (Exception e) {
            return 0;
        }
    }

    public void gainHPoint(int a) {
        if (a > 0) {
            gainnHPoint(a);
        }
        long before = getHPoint();
        long after = before + a;
        if (after < 0) {
            after = 0;
            FileoutputUtil.log(FileoutputUtil.음수로그, "[홍보 포인트] " + this.getName() + " before : " + before + " after : " + after);
        }

        client.setKeyValue("HPoint", String.valueOf(after));
    }

    public void setHPoint(int a) {
        client.setKeyValue("HPoint", String.valueOf(a));
    }

    public long getDonationPoint() {
        try {
            return Long.parseLong(client.getKeyValue("DPoint"));
        } catch (Exception e) {
            return 0;
        }
    }

    public void gainDonationPoint(int a) {
        if (a > 0) {
            gainnDonationPoint(a);
        }

        long before = getDonationPoint();
        long after = before + a;
        if (after < 0) {
            after = 0;
            FileoutputUtil.log(FileoutputUtil.음수로그, "[후원 포인트] " + this.getName() + " before : " + before + " after : " + after);
        }

        client.setKeyValue("DPoint", String.valueOf(after));
        updateDonationPoint();
    }

    public void setDonationPoint(int a) {
        client.setKeyValue("DPoint", String.valueOf(a));
        updateDonationPoint();
    }

    public void updateDonationPoint() {
        client.getSession().writeAndFlush(CWvsContext.updateMaplePoint(this));
    }

    public int getBetaClothes() {
        return betaclothes;
    }

    public void pBetaClothes(int value) {
        betaclothes += value;
    }

    public void mBetaClothes(int value) {
        betaclothes -= value;
    }

    public int getArcaneAim() {
        return arcaneAim;
    }

    public void setArcaneAim(int a) {
        this.arcaneAim = a;
    }

    public static boolean updateNameChangeCoupon(final MapleClient c) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET nameChange = ? WHERE id = ?");
            ps.setByte(1, c.getNameChangeEnable());
            ps.setInt(2, c.getAccID());
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException sqlE) {
            sqlE.printStackTrace();
            return false;
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
        return true;
    }

    public static boolean saveNameChange(final String name, int cid) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE characters SET name = ? WHERE id = ?");
            ps.setString(1, name);
            ps.setInt(2, cid);
            ps.executeUpdate();
            ps.close();
            con.close();
        } catch (SQLException sqlE) {
            sqlE.printStackTrace();
            return false;
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
        return true;
    }

    public Map<Integer, Integer> getSkillCustomValues() {
        return customValue;
    }

    public int getSkillCustomValue(int skillId) {
        if (customValue.containsKey(skillId)) {
            return customValue.get(skillId);
        }
        return 0;
    }

    public void setSkillCustomValue(final int skillid, final int value) {
        if (customValue.get(skillid) != null) {
            customValue.remove(skillid);
        }
        customValue.put(skillid, value);
    }

    public Map<Integer, Long> getSkillCustomTimes() {
        return customTime;
    }

    public void setSkillCustomTime(final int skillid, final long time) {
        if (customTime.get(skillid) != null) {
            customTime.remove(skillid);
        }
        customTime.put(skillid, time);
    }

    public long getSkillCustomTime(int skillId) {
        if (customTime.containsKey(skillId)) {
            return customTime.get(skillId);
        }
        return 0;
    }

    public void unchooseStolenSkill(int skillID) { //base skill
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final int stolenjob = GameConstants.getJobNumber(skillID);
        boolean changed = false;
        for (Pair<Integer, Boolean> sk : stolenSkills) {
            if (sk.right && GameConstants.getJobNumber(sk.left) == stolenjob) {
                cancelStolenSkill(sk.left);
                sk.right = false;
                changed = true;
            }
        }
        if (changed) {
            final Skill skil = SkillFactory.getSkill(skillID);
            changeSkillLevel_Skip(skil, getSkillLevel(skil), (byte) 0);
            client.getSession().writeAndFlush(CField.replaceStolenSkill(GameConstants.getStealSkill(stolenjob), 0));
        }
    }

    public void cancelStolenSkill(int skillID) {
        final Skill skk = SkillFactory.getSkill(skillID);
        final MapleStatEffect eff = skk.getEffect(getTotalSkillLevel(skk));

        /*        if (eff.isMonsterBuff() || (eff.getStatups().isEmpty() && !eff.getMonsterStati().isEmpty())) {
         for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
         for (MonsterStatus b : eff.getMonsterStati().keySet()) {
         //                    if (mons.isBuffed(b) && mons.getBuff(b).getFromID() == this.id) {
         //                      mons.cancelStatus(b);
         //                }
         }
         }
         } else */ if (eff.getDuration() > 0 && !eff.getStatups().isEmpty()) {
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                chr.cancelEffect(eff, false, -1);

            }
        }
    }

    public void chooseStolenSkill(int skillID) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final Pair<Integer, Boolean> dummy = new Pair<>(skillID, false);
        if (stolenSkills.contains(dummy)) {
            unchooseStolenSkill(skillID);
            stolenSkills.get(stolenSkills.indexOf(dummy)).right = true;

            client.getSession().writeAndFlush(CField.replaceStolenSkill(GameConstants.getStealSkill(GameConstants.getJobNumber(skillID)), skillID));
        }
    }

    public void addStolenSkill(int skillID, int skillLevel) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final Pair<Integer, Boolean> dummy = new Pair<>(skillID, true);
        final Skill skil = SkillFactory.getSkill(skillID);
        if (!stolenSkills.contains(dummy)) {// && GameConstants.canSteal(skil)) {
            dummy.right = false;
            skillLevel = Math.min(skil.getMaxLevel(), skillLevel);
            final int jobid = GameConstants.getJobNumber(skillID);
            if (!stolenSkills.contains(dummy)) {
                int count = 0;
                skillLevel = Math.min(getSkillLevel(GameConstants.getStealSkill(jobid)), skillLevel);
                for (Pair<Integer, Boolean> sk : stolenSkills) {
                    if (GameConstants.getJobNumber(sk.left) == jobid) {
                        count++;
                    }
                }
                if (count < GameConstants.getNumSteal(jobid)) {
                    stolenSkills.add(dummy);
                    changed_skills = true;
                    changeSkillLevel_Skip(skil, skillLevel, (byte) skillLevel);
                    client.getSession().writeAndFlush(CField.addStolenSkill(jobid, count, skillID, skillLevel));
                    //client.getSession().writeAndFlush(MaplePacketCreator.updateStolenSkills(this, jobid));
                }
            }
        }
    }

    public void removeStolenSkill(int skillID) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final int jobid = GameConstants.getJobNumber(skillID);
        final Pair<Integer, Boolean> dummy = new Pair<>(skillID, false);
        int count = -1, cc = 0;
        for (int i = 0; i < stolenSkills.size(); i++) {
            if (stolenSkills.get(i).left == skillID) {
                if (stolenSkills.get(i).right) {
                    unchooseStolenSkill(skillID);
                }
                count = cc;
                break;
            } else if (GameConstants.getJobNumber(stolenSkills.get(i).left) == jobid) {
                cc++;
            }
        }
        if (count >= 0) {
            cancelStolenSkill(skillID);
            stolenSkills.remove(dummy);
            dummy.right = true;
            stolenSkills.remove(dummy);
            changed_skills = true;
//            changeSingleSkillLevel(SkillFactory.getSkill(skillID), 0, (byte) 0);
            //hacky process begins here
            client.getSession().writeAndFlush(CField.replaceStolenSkill(GameConstants.getStealSkill(jobid), 0));
            for (int i = 0; i < GameConstants.getNumSteal(jobid); i++) {
                client.getSession().writeAndFlush(CField.removeStolenSkill(jobid, i));
            }
            count = 0;
            for (Pair<Integer, Boolean> sk : stolenSkills) {
                if (GameConstants.getJobNumber(sk.left) == jobid) {
                    //client.getSession().writeAndFlush(CField.addStolenSkill(jobid, count, sk.left, getSkillLevel(sk.left)));
                    if (sk.right) {
                        client.getSession().writeAndFlush(CField.replaceStolenSkill(GameConstants.getStealSkill(jobid), sk.left));
                    }
                    count++;
                }
            }
            client.getSession().writeAndFlush(CField.removeStolenSkill(jobid, count));
        }
    }

    public List<Pair<Integer, Boolean>> getStolenSkills() {
        return stolenSkills;
    }

    public final void startDiabolicRecovery(final MapleStatEffect eff) {
        BuffTimer tMan = BuffTimer.getInstance();
        final int regenHP = (int) (getStat().getCurrentMaxHp() * (eff.getX() / 100.0D));
        if (diabolicRecoveryTask != null) {
            diabolicRecoveryTask.cancel(true);
            diabolicRecoveryTask = null;
        }
        Runnable r = new Runnable() {

            @Override
            public void run() {
                if (isAlive()) {
                    MapleCharacter.this.addHP(regenHP);
                    if (getStat().getCurrentMaxHp() - regenHP > 0) {
                        client.getSession().writeAndFlush(EffectPacket.showHealEffect(client.getPlayer(), (int) Math.min(getStat().getCurrentMaxHp() - regenHP, regenHP), true)); // or 42, 43
                    }
                }
            }
        };
        diabolicRecoveryTask = tMan.register(r, eff.getW() * 1000);
        tMan.schedule(new Runnable() {

            @Override
            public void run() {
                if (diabolicRecoveryTask != null) {
                    diabolicRecoveryTask.cancel(true);
                    diabolicRecoveryTask = null;
                }
            }
        }, eff.getDuration());
    }

    public short getXenonSurplus() {
        return xenonSurplus;
    }

    public void setXenonSurplus(short amount, Skill skill) {
        int maxSupply = level >= 100 ? 20 : level >= 60 ? 15 : level >= 30 ? 10 : 5;
        if (getBuffedValue(MapleBuffStat.Overload) != null) {
            maxSupply = 40;
        }
        if (xenonSurplus + amount > maxSupply) {
            this.xenonSurplus = (short) maxSupply;
            updateXenonSurplus(xenonSurplus, skill);
            return;
        }
        this.xenonSurplus = amount;
        updateXenonSurplus(xenonSurplus, skill);
    }

    public void gainXenonSurplus(short amount, Skill skill) {
        int maxSupply = level >= 100 ? 20 : level >= 60 ? 15 : level >= 30 ? 10 : 5;
        if (getBuffedValue(MapleBuffStat.Overload) != null) {
            maxSupply = 40;
        }
        if (xenonSurplus + amount > maxSupply) {
            this.xenonSurplus = (short) maxSupply;
            updateXenonSurplus(xenonSurplus, skill);
            return;
        }
        this.xenonSurplus += amount;
        updateXenonSurplus(xenonSurplus, skill);
    }

    public void updateXenonSurplus(short amount, Skill skill) {
        int maxSupply = level >= 100 ? 20 : level >= 60 ? 15 : level >= 30 ? 10 : 5;
        if (getBuffedValue(MapleBuffStat.Overload) != null) {
            maxSupply = 40;
        }
        if (amount > maxSupply) {
            amount = (short) maxSupply;
        }
        MapleStatEffect effect = SkillFactory.getSkill(30020232).getEffect(getTotalSkillLevel(skill));
        EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class
        );
        statups.put(MapleBuffStat.SurplusSupply, new Pair<>((int) amount, 0));
        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, this));
    }

    public final void startXenonSupply(final Skill skill) {
        BuffTimer tMan = BuffTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int maxSupply = level >= 100 ? 20 : level >= 60 ? 15 : level >= 30 ? 10 : 5;
                if (getBuffedValue(MapleBuffStat.Overload) != null) {
                    maxSupply = 40;
                }
                if (maxSupply > getXenonSurplus()) {
                    gainXenonSurplus((short) 1, skill);
                }
            }
        };
        if (client.isLoggedIn()) {
            XenonSupplyTask = tMan.register(r, 4000);
        }
    }

    public void gainExceed(short amount, Skill skill) {

        int max = (getSkillLevel(31220044) >= 1 ? 18 : 20);

        if (this.overloadCount < max) {

            this.overloadCount += amount;

            //직전과 다른 스킬 사용 시 +1 추가 증가
            if (getBuffedEffect(MapleBuffStat.Exceed) != null) {
                if (GameConstants.getLinkedSkill(getBuffSource(MapleBuffStat.Exceed)) != GameConstants.getLinkedSkill(skill.getId()) && !skill.isHyper() && GameConstants.isDemonAvenger(skill.getId() / 10000)) { // link
                    this.overloadCount++;
                }
            }

            if (max < overloadCount) {
                this.overloadCount = (short) max;
            }

            SkillFactory.getSkill(30010230).getEffect(1).applyTo(this, false);
        }
        /*	    
         if (this.exceed < 4) {
         this.exceed++;
         if (checkBuffStatValueHolder(MapleBuffStat.Exceed) != null) {
         cancelEffect(checkBuffStatValueHolder(MapleBuffStat.Exceed).effect, true, -1, MapleBuffStat.Exceed, true);
         }
		    
         Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
         localstatups.put(MapleBuffStat.Exceed, new Pair<>(exceed, 10000));
		    
         client.getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, skill.getEffect(getSkillLevel(GameConstants.getLinkedSkill(skill.getId()))), this));
         final long starttime = System.currentTimeMillis();
	        
         for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : localstatups.entrySet()) {

         MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(this, skill.getEffect(getSkillLevel(GameConstants.getLinkedSkill(skill.getId()))), starttime, statup.getKey());
         ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
         cancelAction.run();
         }, statup.getValue().right);
	            
         registerEffect(skill.getEffect(getSkillLevel(GameConstants.getLinkedSkill(skill.getId()))), starttime,
         schedule,
         statup, false, getId());
         }
         }*/
    }

    public void setLuminusMorph(boolean morph) {
        luminusMorph = morph;
    }

    public boolean getLuminusMorph() {
        return luminusMorph;
    }

    public void setLuminusMorphUse(int use) {
        lumimorphuse = use;
    }

    public int getLuminusMorphUse() {
        return lumimorphuse;
    }

    public int getForcingItem() {
        return forcingItem;
    }

    public void setForcingItem(short forcingItem) {
        this.forcingItem = forcingItem;
    }

    public long getCooldownLimit(int skillid) {
        for (MapleCoolDownValueHolder mcdvh : getCooldowns()) {
            if (mcdvh.skillId == skillid) {
                return mcdvh.length - (System.currentTimeMillis() - mcdvh.startTime);
            }
        }
        return 0;
    }

    public long getBuffLimit(int skillid) {

        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mcdvh : effects) {
            if (mcdvh.right.effect.getSourceId() == skillid) {
                return mcdvh.right.localDuration - (System.currentTimeMillis() - mcdvh.right.startTime);
            }
        }
        return 0;
    }

    /* ?뚣뀿???????????? ???????? ?????紐??*/
    public void setFishing(boolean a) {
        this.fishing = a;
    }

    public boolean Fishing() {
        return fishing;
    }

    public byte getWolfScore() {
        return wolfscore;
    }

    public void setWolfScore(byte farmscore) {
        this.wolfscore = farmscore;
    }

    public byte getSheepScore() {
        return sheepscore;
    }

    public void setSheepScore(byte farmscore) {
        this.sheepscore = farmscore;
    }

    public void addWolfScore() {
        this.wolfscore++;
    }

    public void addSheepScore() {
        this.sheepscore--;
    }

    public byte getPandoraBoxFever() {
        return pandoraBoxFever;
    }

    public void setPandoraBoxFever(byte pandoraBoxFever) {
        this.pandoraBoxFever = pandoraBoxFever;
    }

    public void addPandoraBoxFever(byte pandoraBoxFever) {
        this.pandoraBoxFever += pandoraBoxFever;
    }

    public void handleKaiserCombo() {
        if (getKaiserCombo() < 1000) {
            setKaiserCombo((short) (getKaiserCombo() + Randomizer.rand(1, 5)));
        }
        SkillFactory.getSkill(61111008).getEffect(1).applyKaiserCombo(this, getKaiserCombo());
    }

    public void resetKaiserCombo() {
        setKaiserCombo((short) 0);
        SkillFactory.getSkill(61111008).getEffect(1).applyKaiserCombo(this, getKaiserCombo());
    }

    public void maxKaiserCombo() {
        combo_k = getKaiserCombo();
        setKaiserCombo((short) 1000);
        SkillFactory.getSkill(61111008).getEffect(1).applyKaiserCombo(this, getKaiserCombo());
    }

    public void backKaiserCombo() {
        setKaiserCombo((short) combo_k);
        SkillFactory.getSkill(61111008).getEffect(1).applyKaiserCombo(this, getKaiserCombo());
    }

    public List<Core> getCore() {
        return cores;
    }

    public int getBaseColor() {
        return basecolor;
    }

    public void setBaseColor(int basecolor) {
        this.basecolor = basecolor;
    }

    public int getAddColor() {
        return addcolor;
    }

    public void setAddColor(int addcolor) {
        this.addcolor = addcolor;
    }

    public int getBaseProb() {
        return baseprob;
    }

    public void setBaseProb(int baseprob) {
        this.baseprob = baseprob;
    }

    public int getSecondBaseColor() {
        return secondbasecolor;
    }

    public void setSecondBaseColor(int basecolor) {
        this.secondbasecolor = basecolor;
    }

    public int getSecondAddColor() {
        return secondaddcolor;
    }

    public void setSecondAddColor(int addcolor) {
        this.secondaddcolor = addcolor;
    }

    public int getSecondBaseProb() {
        return secondbaseprob;
    }

    public void setSecondBaseProb(int baseprob) {
        this.secondbaseprob = baseprob;
    }

    public List<Integer> getCashWishList() {
        return cashwishlist;
    }

    public void addCashWishList(int id) {
        cashwishlist.add(id);
    }

    public void removeCashWishList(int id) {
        cashwishlist.remove(id);
    }

    public void giveHoyoungGauge(int skillid) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

        Skill sk = SkillFactory.getSkill(skillid);
        if (sk != null) {
            MapleStatEffect effect = SkillFactory.getSkill(164000010).getEffect(1);
            MapleStatEffect advance = null;

            if (job == 16411 || job == 16412) {
                advance = SkillFactory.getSkill(164110014).getEffect(1);
            }

            int add = 0;
            int max = 100;

            switch (skillid) {
                case 164111000:
                case 164121000: // 천
                    if (useChun) {
                        add = 0;
                    } else {
                        useChun = true;
                        add = effect.getU();
                    }
                    break;
                case 164101000:
                case 164111003: // 지
                    if (useJi) {
                        add = 0;
                    } else {
                        useJi = true;
                        add = effect.getV();
                    }
                    break;
                case 164001000:
                case 164121003: // 인
                    if (useIn) {
                        add = 0;
                    } else {
                        useIn = true;
                        add = effect.getW();
                    }
                    break;
                case 164001001:
                case 164101003:
                case 164111007:
                case 164121006: // 부적 도술
                    energy = 0;
                    if (advance != null) {
                        scrollGauge = Math.min(scrollGauge + 200, 900);
                    }
                    break;
                case 164111008:
                case 164121007:
                case 164121008:
                case 400041050: // 두루마리 도술
                    scrollGauge = 0;
                    break;
            }

            scrollGauge += 15;
            energy = Math.min(max, energy + add);

            //천, 지, 인 다 쓰면 초기화
            if (useChun && useJi && useIn) {
                useChun = false;
                useJi = false;
                useIn = false;
            }

            statups.put(MapleBuffStat.HoyoungThirdProperty, new Pair<>(useChun ? 1 : 0, 0));
            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));

            statups.clear();
            statups.put(MapleBuffStat.TidalForce, new Pair<>(energy, 0));
            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
        }
    }

    public void decreaseOrderEtherGauge() {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

        energy -= 100;

        energy = Math.max(0, energy);

        statups.put(MapleBuffStat.AdelGauge, new Pair<>(energy, 0));

        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
    }

    public long getLastCreationTime() {
        return lastCreationTime;
    }

    public void setLastCreationTime(long time) {
        lastCreationTime = time;
    }

    public void giveEtherGauge(int skillid) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

        Skill sk = SkillFactory.getSkill(151100017);
        int skLv = getSkillLevel(151100017);
        if (skLv > 0) {
            int advancedSkLv = getSkillLevel(151120012);
            MapleStatEffect ether = sk.getEffect(skLv);
            if (SkillFactory.getSkill(skillid) == null) {
                return;
            }
            MapleStatEffect aEff = SkillFactory.getSkill(skillid).getEffect(GameConstants.getLinkedSkill(getSkillLevel(skillid)));
            switch (skillid) {
                case 151101000:
                case 151111000:
                case 151121000:
                case 151121002: {

                    energy += ether.getS();

                    if (getBuffedEffect(MapleBuffStat.Restore) != null) {
                        energy += ether.getS() * getBuffedEffect(MapleBuffStat.Restore).getX() / 100;
                    }

                    energy = Math.min(advancedSkLv > 0 ? 400 : 300, energy);

                    statups.put(MapleBuffStat.AdelGauge, new Pair<>(energy, 0));

                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));

                    MapleStatEffect wonder = getBuffedEffect(151101013);

                    if (wonder != null && System.currentTimeMillis() - lastShardTime >= wonder.getX() * 1000) {
                        lastShardTime = System.currentTimeMillis();
                        addMP(-wonder.getY());

                        List<AdelProjectile> shards = new ArrayList<>();

                        for (int i = 0; i < 5; ++i) {
                            AdelProjectile shard = new AdelProjectile(0, getId(), 0, 151001001, 5000, 0, 1, new Point(getTruePosition().x + (-30 * (i - 2)), getTruePosition().y - 100), new ArrayList<>());
                            shard.setDelay(1000);
                            shards.add(shard);
                        }

                        getMap().spawnAdelProjectile(this, shards, false);
                    }
                    break;
                }
                case 151101001: {
                    if (getSkillLevel(151120034) > 0) {
                        return;
                    }
                    energy -= aEff.getY();

                    energy = Math.max(0, energy);

                    statups.put(MapleBuffStat.AdelGauge, new Pair<>(energy, 0));

                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
                    break;
                }
                case 151101003: {
                    if (getSkillLevel(151120034) > 0) {
                        energy += SkillFactory.getSkill(151120034).getEffect(getSkillLevel(151120034)).getX();
                    }
                    energy = Math.max(0, energy);

                    statups.put(MapleBuffStat.AdelGauge, new Pair<>(energy, 0));

                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
                    break;
                }
                case 151111003:
                case 151121003: {
                    if (aEff.makeChanceResult() && getSkillLevel(151100002) > 0) {
                        SkillFactory.getSkill(151100002).getEffect(getSkillLevel(151100002)).applyTo(this, false);
                    }
                    break;
                }
            }
        }
    }

    public void giveMaliceGauge(int skillid) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        MapleStatEffect effect = SkillFactory.getSkill(63101001).getEffect(1);
        MapleStatEffect effect2 = SkillFactory.getSkill(63120001).getEffect(1);
        if (GameConstants.isKainRevelationSkill(skillid)) {
            MalicePoint += 13;
            if (getSkillLevel(63120000) > 0) {//카인 포제션II
                MalicePoint += 5;
                if (MalicePoint > 500) {
                    MalicePoint = 500;
                }
            } else {
                if (MalicePoint > 300) {
                    MalicePoint = 300;
                }
            }
            statups.put(MapleBuffStat.Malice, new Pair<>(MalicePoint, 0));
            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
        }
        if (MapleBuffStat.DeathBlessing != null) {
            if (GameConstants.isKainDoDeaathBlessingSkill(skillid)) {
                MalicePoint += 10;
                if (getSkillLevel(63120001) > 0) {
                    MalicePoint += effect2.getS();
                }
                if (MalicePoint > 500) {
                    MalicePoint = 500;
                }
                statups.put(MapleBuffStat.Malice, new Pair<>(MalicePoint, 0));
                getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
            }
        }
        return;
    }

    public void giveRelikGauge(int skillid, AttackInfo info) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

        if (skillid == 0 && info == null) {
            statups.put(MapleBuffStat.RelikGauge, new Pair<>(0, 0));
            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
            return;
        }

        Skill sk = SkillFactory.getSkill(skillid);
        if (sk != null && job != 301) {
            MapleStatEffect eff = sk.getEffect(getSkillLevel(GameConstants.getLinkedSkill(sk.getId())));
            MapleStatEffect effect = SkillFactory.getSkill(3300000).getEffect(1);
            MapleStatEffect advance = null;
            MapleStatEffect gaudiance = null;

            if (job == 332) {
                advance = SkillFactory.getSkill(3320000).getEffect(1);
            }

            if (getSkillLevel(3310006) > 0) {
                gaudiance = SkillFactory.getSkill(3310006).getEffect(getSkillLevel(3310006));
            }

            int max = effect.getU();

            int add = 0;

            int oldMark = cardinalMark;

            switch (skillid) {

                case 3011004:
                case 3300002:
                case 3300005:
                case 3321003:
                    //카디널 디스차지
                    this.cardinalMark = 1;
                    if (advance != null) {
                        add = info == null ? 0 : advance.getX() * info.targets;
                    } else {
                        add = info == null ? 0 : effect.getX() * info.targets;
                    }
                    break;
                case 3301004:
                case 3310004:
                case 3311013:
                case 3321005:
                    //카디널 블래스트
                    this.cardinalMark = 2;
                    if (advance != null) {
                        add = advance.getY();
                    } else {
                        add = effect.getY();
                    }
                    break;
                case 3311003:
                case 3321007:
                    //카디널 트랜지션
                    this.cardinalMark = 3;
                    if (advance != null) {
                        add = advance.getY();
                    } else {
                        add = effect.getY();
                    }
                    break;
                case 400031036:
                    if (info != null) {
                        add += eff.getV();
                    }
                    break;
                case 3301008:
                    if (info == null) {
                        energy -= eff.getForceCon();
                    }
                    break;
                case 3321035:
//                case 3321036:
                case 3321037:
                case 3321038:
                case 3321039:
//                case 3321040:
                    break;
                default:
                    energy -= eff.getForceCon();
                    break;
            }

            if (oldMark == 1 && this.cardinalMark == 2) {
                //좌표 이상한 애
                if (getSkillLevel(3300005) > 0 && skillid != 3311013) {
                    MapleStatEffect editionalDischarge = SkillFactory.getSkill(3300005).getEffect(getSkillLevel(3300005));
                    if (editionalDischarge.makeChanceResult()) {
                        List<ForceAtom> atoms = new ArrayList<>();
                        List<MapleMapObject> monsters = getMap().getMapObjectsInRange(getTruePosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));

                        MapleAtom atom = new MapleAtom(false, id, 57, true, 3300005, getTruePosition().x, getTruePosition().y);
                        List<Integer> mobs = new ArrayList<>();

                        for (int i = 0; i < editionalDischarge.getBulletCount() + (getBuffedValue(3321034) ? 1 : 0); ++i) {
                            MapleMapObject mob = null;
                            if (!monsters.isEmpty()) {
                                mob = monsters.get(Randomizer.nextInt(monsters.size()));
                            }
                            mobs.add(mob != null ? mob.getObjectId() : 0);
                            atom.addForceAtom(new ForceAtom(2, 0x2A, 4, 0, (short) 0x3C, getTruePosition()));
                        }

                        atom.setDwTargets(mobs);
                        getMap().spawnMapleAtom(atom);
                    }
                }
            }

            if (info != null && (skillid == 3321007 || skillid == 3321016 || skillid == 3321018 || skillid == 3321020 || (getBuffedValue(3320008) && (this.cardinalMark == 1 || this.cardinalMark == 2)))) {
                MapleStatEffect curseTransition = SkillFactory.getSkill(3320001).getEffect(1);
                MapleStatEffect editionalTransition = getBuffedEffect(3320008);

                int plus = 1;

                if (skillid == 3321016 || skillid == 3321018 || skillid == 3321020) {
                    plus = eff.getX();
                }

                for (AttackPair attack : info.allDamage) {
                    MapleMonster mob = map.getMonsterByOid(attack.objectid);

                    if (curseBound > 0 && mob != null) {
                        if (mob.getCurseBound() < curseTransition.getX() && editionalTransition != null) {
                            if (editionalTransition.makeChanceResult()) {
                                curseBound--;
                                mob.setCurseBound(Math.min(curseTransition.getX(), mob.getCurseBound() + plus));
                                mob.applyStatus(getClient(), MonsterStatus.MS_CurseMark, new MonsterStatusEffect(3320001, curseTransition.getDuration()), mob.getCurseBound(), curseTransition);
                            }
                        }
                    } else {
                        break;
                    }
                }

                if (curseBound > 0) {
                    statups.put(MapleBuffStat.BonusAttack, new Pair<>(curseBound, (int) getBuffLimit(3320008)));
                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, editionalTransition, this));

                    statups.clear();

                } else {
                    cancelEffectFromBuffStat(MapleBuffStat.BonusAttack, 3320008);
                }
            } else if (oldMark == 3 && (this.cardinalMark == 1 || this.cardinalMark == 2)) {
                if (getSkillLevel(3320008) > 0) {
                    MapleStatEffect editionalTransition = SkillFactory.getSkill(3320008).getEffect(getSkillLevel(3320008));
                    editionalTransition.applyTo(this, false, editionalTransition.getU() * 1000);
                }
            }

            energy += add;

            if (energy > max) {
                energy = max;
            } else if (energy < 0) {
                energy = 0;
            }
            //System.err.println("" + add);
            statups.put(MapleBuffStat.RelikGauge, new Pair<>(energy, 0));

            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));

            statups.clear();

            statups.put(MapleBuffStat.CardinalMark, new Pair<>(cardinalMark, 0));

            getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));

            statups.clear();

            if (gaudiance != null && getBuffedEffect(MapleBuffStat.AncientGuidance) == null) {
                this.ancientGauge += add;
                if (this.ancientGauge >= 1000) {
                    this.ancientGauge = 0;
                    gaudiance.applyTo(this, false);
                } else {
                    statups.put(MapleBuffStat.AncientGuidance, new Pair<>(-1, 0));

                    getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));

                    statups.clear();
                }
            }

            int[] ancientSkills = {3301008, 3311010, 3321012, 3321014, 3321035, 3321036, 3321038, 3321040};

            if (lastCardinalForce != skillid && skillid != 3310004 && skillid != 3300005) {
                for (int ancientSkill : ancientSkills) {
                    if (getCooldownLimit(ancientSkill) > 0) {
                        changeCooldown(ancientSkill, advance != null ? (-advance.getW() * 1000) : (-effect.getT() * 1000));
                    }
                }
            }

            lastCardinalForce = skillid;
        }
    }

    public void giveCylinderGauge(int skillId) {

        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        List<Integer> skills = new ArrayList<>();

        switch (skillId) {
            case 37001000:
            case 37101000:
            case 37111000:
            case 37121003:
            case 37120022:
                skills.add(37000007);
                client.getSession().writeAndFlush(CField.rangeAttack(skillId, skills, Randomizer.nextBoolean() ? -1 : 1, getTruePosition(), isFacingLeft()));
                break;
            case 37000013: // 6
                skills.add(37120013);
            case 37000012: // 5
                skills.add(37110010);
            case 37000011: // 4
                skills.add(37100009);
            case 37001002: // 3
                skills.add(37000008);
                client.getSession().writeAndFlush(CField.rangeAttack(skillId, skills, Randomizer.nextBoolean() ? -1 : 1, getTruePosition(), isFacingLeft()));
                break;
            case 37110001:
                skills.add(37110002);
                skills.add(37000007);
                client.getSession().writeAndFlush(CField.rangeAttack(skillId, skills, Randomizer.nextBoolean() ? -1 : 1, getTruePosition(), isFacingLeft()));
                break;
            case 37121000:
                skills.add(37120001);
                skills.add(37000007);
                client.getSession().writeAndFlush(CField.rangeAttack(skillId, skills, Randomizer.nextBoolean() ? -1 : 1, getTruePosition(), isFacingLeft()));
                break;
        }

        switch (skillId) {
            case 37000009:
            case 37100008:
            case 37100002:
            case 37110001:
            case 37110004:
                //   if (getBuffedEffect(MapleBuffStat.RWOverHeat) == null) {
                this.Bullet--;
                statups.put(MapleBuffStat.RWCylinder, new Pair<>(1, 0));
                client.getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(skillId).getEffect(getSkillLevel(GameConstants.getLinkedSkill(skillId))), this));
                //    }
                break;
        }

        if (this.Bullet == 0 /*&& getBuffedEffect(MapleBuffStat.RWOverHeat) == null*/) {
            if (this.Cylinder < 6 && getBuffedEffect(MapleBuffStat.RWOverHeat) == null) {
                this.Cylinder++;
            }
            this.Bullet = 6;
            client.getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(37000010).getEffect(getSkillLevel(GameConstants.getLinkedSkill(skillId))), this));
        }
    }

    public void givePPoint(int skillid, boolean special) {
        if (SkillFactory.getSkill(skillid) == null) {
            return;
        }
        MapleStatEffect effects = SkillFactory.getSkill(skillid).getEffect(getSkillLevel(skillid));
        int MaxPPoint = 0;

        switch (getJob()) {
            case 14200:
                MaxPPoint = 10;
                break;
            case 14210:
                MaxPPoint = 20;
                break;
            case 14211:
                MaxPPoint = 25;
                break;
            case 14212:
                MaxPPoint = 30;
                break;
        }

        if (effects != null && !special) {
            if (skillid == 142120002 || skillid == 142111007 || skillid == 400021008 || skillid == 400021048) {
                return;
            } else if (effects.getPPCon() > 0 || effects.getPPReq() > 0) {
                if (skillid == 400021048 && getCooldownLimit(400021048) == 0) {
                    PPoint -= effects.getPPCon() / (getBuffedEffect(MapleBuffStat.KinesisPsychicOver) != null ? 2 : 1);
                } else if (getBuffedEffect(MapleBuffStat.KinesisPsychicOver) != null) {
                    PPoint -= ((effects.getPPCon() > 0 ? effects.getPPCon() : effects.getPPReq()) / 2);
                } else {
                    PPoint -= effects.getPPCon() > 0 ? effects.getPPCon() : effects.getPPReq();
                }
            } else if (effects.getPPRecovery() > 0) {
                PPoint += effects.getPPRecovery();
            } else if (skillid == 142111010) {
                PPoint -= effects.getX();
            } else if (skillid == 142121008) {
                PPoint += ((MaxPPoint - PPoint) / 2);
            } else {
                PPoint += 1;
            }
        } else {
            if (effects != null) {
                if (skillid == 142120002 || skillid == 142111007 || skillid == 400021008 || skillid == 400021048) {
                    if (effects.getPPCon() > 0 || effects.getPPReq() > 0) {
                        if (getBuffedEffect(MapleBuffStat.KinesisPsychicOver) != null) {
                            PPoint -= ((effects.getPPCon() > 0 ? effects.getPPCon() : effects.getPPReq()) / 2);
                        } else {
                            PPoint -= effects.getPPCon() > 0 ? effects.getPPCon() : effects.getPPReq();
                        }
                    }
                }
            }
        }
        if (PPoint < 0) {
            PPoint = 0;
        }
        if (MaxPPoint < PPoint || skillid == 142120030) {
            PPoint = MaxPPoint;
        }
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        statups.put(MapleBuffStat.KinesisPsychicPoint, new Pair<>(PPoint, 0));

        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effects, this));
    }

    public void giverelic(int skillid) {
        switch (skillid) {
            case 155001100:
                if (getArcSpell() < 5) {
                    arcSpell++;
                    if (getBuffedEffect(MapleBuffStat.InfinitySpell) != null) {
                        arcSpell = 5;
                    }
                } else {
                    return;
                }
                break;
            case 155101100:
            case 155101101:
                if (getArcSpell() < 5 && !getArcSpellSkills().contains(155101100)) {
                    arcSpell++;
                    getArcSpellSkills().add(155101100);
                } else {
                    return;
                }
                break;
            case 155111102:
                if (getArcSpell() < 5 && !getArcSpellSkills().contains(skillid)) {
                    arcSpell++;
                    getArcSpellSkills().add(skillid);
                } else {
                    return;
                }
                break;
            case 155121102:
                if (getArcSpell() < 5 && !getArcSpellSkills().contains(skillid)) {
                    arcSpell++;
                    getArcSpellSkills().add(skillid);
                } else {
                    return;
                }
                break;
        }
        updateSpectorGuage(skillid);
    }

    public long getDamageMeter() {
        return damageMeter;
    }

    public void setDamageMeter(long damageMeter) {
        this.damageMeter = damageMeter;
    }

    public void updateSpectorGuage(int skillid) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        switch (skillid) {
            case 155001100:
                statups.put(MapleBuffStat.PlainBuff, new Pair<>(Math.max(0, arcSpell * 2 - (getArcSpellSkills().contains(155101100) ? 2 : 0) - (getArcSpellSkills().contains(155111102) ? 2 : 0) - (getArcSpellSkills().contains(155121102) ? 2 : 0)), 0));
                break;
            case 155101100:
            case 155101101:
                statups.put(MapleBuffStat.ScarletBuff, new Pair<>(1, 0));
                break;
            case 155111102:
                statups.put(MapleBuffStat.GustBuff, new Pair<>(1, 0));
                break;
            case 155121102:
                statups.put(MapleBuffStat.AbyssBuff, new Pair<>(1, 0));
                break;
        }

        switch (skillid) {
            case 155001100:
            case 155120001:
                fightJazzSkill = skillid;
                break;
            case 155001102:
            case 155110000:
            case 155120000:
            case 155101100:
            case 155101101:
            case 155101112:
            case 155101104:
            case 155101114:
            case 155111102:
            case 155111111:
            case 155121102:
                if (fightJazzSkill == 155001100) {
                    if (getSkillLevel(155120014) > 0) {
                        SkillFactory.getSkill(155120014).getEffect(getSkillLevel(155120014)).applyTo(this, false);
                    }
                }
                fightJazzSkill = 0;
                break;
            case 155101200:
            case 155101201:
            case 155101212:
            case 155101204:
            case 155101214:
            case 155111202:
            case 155111211:
            case 155121202:
            case 155121215:
                if (fightJazzSkill == 155120001) {
                    if (getSkillLevel(155120014) > 0) {
                        SkillFactory.getSkill(155120014).getEffect(getSkillLevel(155120014)).applyTo(this, false);
                    }
                }
                fightJazzSkill = 0;
                break;
            case 155111306:
            case 155121306:
            case 155121341:
            case 400051334:
                if (fightJazzSkill == 155001100 || fightJazzSkill == 155120001) {
                    if (getSkillLevel(155120014) > 0) {
                        SkillFactory.getSkill(155120014).getEffect(getSkillLevel(155120014)).applyTo(this, false);
                    }
                }
                fightJazzSkill = 0;
                break;
        }

        getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, this));
    }

    public void giveSpectorGauge(int skillid) {
        if (SkillFactory.getSkill(skillid) == null) {
            return;
        }

        if (skillid == 400051334 || skillid == 155121341 || skillid == 155111006 || skillid == 155121306) {
            SkillFactory.getSkill(155101006).getEffect(1).applyTo(this, false);
        }
    }

    public byte getDeathCount() {
        return deathcount;
    }

    public void setDeathCount(byte de) {
        deathcount = de;
    }

    public List<Equip> getSymbol() {
        return symbol;
    }

    public List<Equip> getAcSymbol() {
        List<Equip> as = new ArrayList<>();
        for (Equip e : symbol) {
            if (GameConstants.isArcaneSymbol(e.getItemId())) {
                as.add(e);
            }
        }
        return as;
    }

    public List<Equip> getAsSymbol() {
        List<Equip> as = new ArrayList<>();
        for (Equip e : symbol) {
            if (GameConstants.isAscenticSymbol(e.getItemId())) {
                as.add(e);
            }
        }
        return as;
    }

    public void setSymbol(List<Equip> symbol) {
        this.symbol = symbol;
    }

    public List<Triple<Skill, SkillEntry, Integer>> getLinkSkills() {
        return linkskills;
    }

    public List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> getEffects() {
        return effects;
    }

    public List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> getRemoveEffects() {
        return removeEffects;
    }

    public void setEffects(List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effects) {
        this.effects = effects;
    }

    public void elementalChargeHandler(int Count) {
        if (this.ElementalCharge < 5) {
            this.ElementalCharge += Count;
        }
        Skill skill = SkillFactory.getSkill(1200014);
        int skillLevel = getTotalSkillLevel(skill);
        MapleStatEffect effect = getBuffedEffect(MapleBuffStat.ElementalCharge);
        if (effect == null) {
            effect = skill.getEffect(skillLevel);
        }

        if (effect.getSourceId() != 1220010 && getSkillLevel(1220010) > 0) {
            effect = SkillFactory.getSkill(1220010).getEffect(getSkillLevel(1220010));
        }
        effect.applyTo(this);

        if (getSkillLevel(400011052) > 0) {
            SkillFactory.getSkill(400011052).getEffect(getSkillLevel(400011052)).applyTo(this, false);
        }
    }

    public void SetSkillid(int id) {
        this.ElementalCharge_ID = id;
    }

    public int GetSkillid() {
        return ElementalCharge_ID;
    }

    public int getMparkexp() {
        return mparkexp;
    }

    public void setMparkexp(int mparkexp) {
        this.mparkexp = mparkexp;
    }

    public void removeKeyValue(int type, String key) {
        String questInfo = getInfoQuest(type);
        if (questInfo == null) {
            return;
        }
        String[] data = questInfo.split(";");
        for (String s : data) {
            if (s.startsWith(key + "=")) {
                String newkey = questInfo.replace(s + ";", "");
                updateInfoQuest(type, newkey);
                return;
            }
        }
        updateInfoQuest(type, questInfo);
    }

    public void removeKeyValue(int type) {
        String questInfo = getInfoQuest(type);
        if (questInfo == null) {
            return;
        }
        updateInfoQuest(type, "");
    }

    public void setKeyValue(int type, String key, String value) {
        String questInfo = getInfoQuest(type);
        if (questInfo == null) {
            updateInfoQuest(type, key + "=" + value + ";");
            return;
        }
        String[] data = questInfo.split(";");
        for (String s : data) {
            if (s.startsWith(key + "=")) {
                String newkey = questInfo.replace(s, key + "=" + value);
                updateInfoQuest(type, newkey);
                return;
            }
        }
        updateInfoQuest(type, questInfo + key + "=" + value + ";");
        //this.dropMessage(5, "키벨류 : " + type + " / " + key + " / " + value);
    }

    public String getKeyValueStr(int type, String key) {
        String questInfo = getInfoQuest(type);
        if (questInfo == null) {
            return null;
        }
        String[] data = questInfo.split(";");
        for (String s : data) {
            if (s.startsWith(key + "=")) {
                String newkey = s.replace(key + "=", "");
                String newkey2 = newkey.replace(";", "");
                return newkey2;
            }
        }
        return null;
    }

    public long getKeyValue(int type, String key) {
        String questInfo = getInfoQuest(type);
        if (questInfo == null) {
            return -1;
        }
        String[] data = questInfo.split(";");
        for (String s : data) {
            if (s.startsWith(key + "=")) {
                String newkey = s.replace(key + "=", "");
                String newkey2 = newkey.replace(";", "");
                long dd = Long.valueOf(newkey2);
                return dd;
            }
        }
        return -1;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>(); // creates an empty array full of shit..
        for (MaplePartyCharacter chr : getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) { // double check <3
                    chars.add(ch);
                }
            }
        }
        return chars;
    }

    public int getElementalCharge() {
        return ElementalCharge;
    }

    public void setElementalCharge(int Elemental) {
        if (ElementalCharge > 5) {
            ElementalCharge = 5;
        } else {
            ElementalCharge = Elemental;
        }
    }

    public int getBHGCCount() {
        return BHGCCount;
    }

    public void setBHGCCount(int bHGCCount) {
        BHGCCount = bHGCCount;
    }

    public int getHowlingGaleCount() {
        return HowlingGaleCount;
    }

    public void setHowlingGaleCount(int howlingGaleCount) {
        HowlingGaleCount = howlingGaleCount;
    }

    public List<Item> getAuctionitems() {
        return auctionitems;
    }

    public void setAuctionitems(List<Item> auctionitems) {
        this.auctionitems = auctionitems;
    }

    public int getSlowAttackCount() {
        return slowAttackCount;
    }

    public void setSlowAttackCount(int slowAttackCount) {
        this.slowAttackCount = slowAttackCount;
    }

    public int getYoyoCount() {
        return YoyoCount;
    }

    public void setYoyoCount(int yoyoCount) {
        YoyoCount = yoyoCount;
    }

    public MapleHaku getHaku() {
        return haku;
    }

    public void setHaku(MapleHaku haku) {
        this.haku = haku;
    }

    public int getListonation() {
        return listonation;
    }

    public void setListonation(int listonation) {
        this.listonation = listonation;
    }

    public int getBeholderSkill1() {
        return beholderSkill1;
    }

    public void setBeholderSkill1(int beholderSkill1) {
        this.beholderSkill1 = beholderSkill1;
    }

    public int getBeholderSkill2() {
        return beholderSkill2;
    }

    public void setBeholderSkill2(int beholderSkill2) {
        this.beholderSkill2 = beholderSkill2;
    }

    public int getReinCarnation() {
        return reinCarnation;
    }

    public void setReinCarnation(int reinCarnation) {
        this.reinCarnation = reinCarnation;
    }

    public byte getPoisonStack() {
        return poisonStack;
    }

    public void setPoisonStack(byte poisonStack) {
        this.poisonStack = poisonStack;
    }

    public byte getInfinity() {
        return infinity;
    }

    public void setInfinity(byte infinity) {
        this.infinity = infinity;
    }

    public byte getHolyPountin() {
        return holyPountin;
    }

    public void setHolyPountin(byte holyPountin) {
        this.holyPountin = holyPountin;
    }

    public int getHolyPountinOid() {
        return holyPountinOid;
    }

    public void setHolyPountinOid(int holyPountinOid) {
        this.holyPountinOid = holyPountinOid;
    }

    public byte getBlessingAnsanble() {
        return blessingAnsanble;
    }

    public void setBlessingAnsanble(byte blessingAnsanble) {
        this.blessingAnsanble = blessingAnsanble;
    }

    public byte getQuiverType() {
        return quiverType;
    }

    public void setQuiverType(byte quiverType) {
        this.quiverType = quiverType;
    }

    public int[] getRestArrow() {
        return RestArrow;
    }

    public int getBarrier() {
        return barrier;
    }

    public void setBarrier(int barrier) {
        this.barrier = barrier;
    }

    public byte getFlip() {
        return flip;
    }

    public void setFlip(byte flip) {
        this.flip = flip;
    }

    public int getDice() {
        return dice;
    }

    public void setDice(int dice) {
        this.dice = dice;
    }

    public byte getUnityofPower() {
        return unityofPower;
    }

    public void setUnityofPower(byte unityofPower) {
        this.unityofPower = unityofPower;
    }

    public byte getConcentration() {
        return concentration;
    }

    public void setConcentration(byte concentration) {
        this.concentration = concentration;
    }

    public byte getMortalBlow() {
        return mortalBlow;
    }

    public void setMortalBlow(byte mortalBlow) {
        this.mortalBlow = mortalBlow;
    }

    public int getPickPocket() {
        return pickPocket;
    }

    public void setPickPocket(int pickPocket) {
        this.pickPocket = pickPocket;
    }

    public byte getHolyMagicShell() {
        return holyMagicShell;
    }

    public void setHolyMagicShell(byte holyMagicShell) {
        this.holyMagicShell = holyMagicShell;
    }

    public byte getAntiMagicShell() {
        return antiMagicShell;
    }

    public void setAntiMagicShell(byte antiMagicShell) {
        this.antiMagicShell = antiMagicShell;
    }

    public byte getBlessofDarkness() {
        return blessofDarkness;
    }

    public void setBlessofDarkness(byte blessofDarkness) {
        this.blessofDarkness = blessofDarkness;
    }

    public byte getDeath() {
        return death;
    }

    public void setDeath(byte death) {
        this.death = death;
    }

    public byte getRoyalStack() {
        return royalStack;
    }

    public void setRoyalStack(byte royalStack) {
        this.royalStack = royalStack;
    }

    public short getKaiserCombo() {
        return kaiserCombo;
    }

    public void setKaiserCombo(short kaiserCombo) {
        this.kaiserCombo = (short) Math.min(1000, kaiserCombo);
    }

    public List<Integer> getWeaponChanges() {
        return weaponChanges;
    }

    public void setWeaponChanges(List<Integer> weaponChanges) {
        this.weaponChanges = weaponChanges;
    }

    public CalcDamage getCalcDamage() {
        return calcDamage;
    }

    public void setCalcDamage(CalcDamage calcDamage) {
        this.calcDamage = calcDamage;
    }

    public int getEnergyBurst() {
        return energyBurst;
    }

    public void setEnergyBurst(int energyBurst) {
        this.energyBurst = energyBurst;
    }

    public int getMarkofPhantom() {
        return markofPhantom;
    }

    public void setMarkofPhantom(int markofPhantom) {
        this.markofPhantom = markofPhantom;
    }

    public int getUltimateDriverCount() {
        return ultimateDriverCount;
    }

    public void setUltimateDriverCount(int ultimateDriverCount) {
        this.ultimateDriverCount = ultimateDriverCount;
    }

    public int getMarkOfPhantomOid() {
        return markOfPhantomOid;
    }

    public void setMarkOfPhantomOid(int markOfPhantomOid) {
        this.markOfPhantomOid = markOfPhantomOid;
    }

    public int getRhoAias() {
        return rhoAias;
    }

    public void setRhoAias(int rhoAias) {
        this.rhoAias = rhoAias;
    }

    public int getPerfusion() {
        return perfusion;
    }

    public void setPerfusion(int perfusion) {
        this.perfusion = perfusion;
    }

    public MapleFieldAttackObj getFao() {
        return fao;
    }

    public void setFao(MapleFieldAttackObj fao) {
        this.fao = fao;
    }

    public int getBlackMagicAlter() {
        return blackMagicAlter;
    }

    public void setBlackMagicAlter(int blackMagicAlter) {
        this.blackMagicAlter = blackMagicAlter;
    }

    public int getWildGrenadierCount() {
        return WildGrenadierCount;
    }

    public void setWildGrenadierCount(int wildGrenadierCount) {
        WildGrenadierCount = wildGrenadierCount;
    }

    public int getVerseOfRelicsCount() {
        return VerseOfRelicsCount;
    }

    public void setVerseOfRelicsCount(int VerseOfRelics) {
        VerseOfRelicsCount = VerseOfRelics;
    }

    public int getJudgementType() {
        return judgementType;
    }

    public void setJudgementType(int judgementType) {
        this.judgementType = judgementType;
    }

    public int getAllUnion() {
        int ret = 0;
        for (MapleUnion union : getUnions().getUnions()) {
            ret += union.getLevel();
        }
        return ret;
    }

    public void setLastCharGuildId(int a) {
        lastCharGuildId = a;
    }

    public int getLastCharGuildId() {
        return lastCharGuildId;
    }

    public void removeAllEquip(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            //DBLogger.getInstance().logItem(LogType.Item.FromScript, getId(), getName(), id, -possessed, MapleItemInformationProvider.getInstance().getName(id), 0, "Map : " + getMapId() + " Interaction ? : " + getConversation());

            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.getShowItemGain(id, (short) -possessed, false));
            }
        }
        if (type == MapleInventoryType.EQUIP) { //check equipped
            type = MapleInventoryType.EQUIPPED;
            possessed = getInventory(type).countById(id);

            if (possessed > 0) {
                Item equip = getInventory(type).findById(id);
                if (equip != null) {
                    getInventory(type).removeSlot(equip.getPosition());
                    equipChanged();
                    getClient().getSession().writeAndFlush(InventoryPacket.clearInventoryItem(MapleInventoryType.EQUIP, equip.getPosition(), false));
                }
            }
        }
    }

    public void LoadPlatformerRecords() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        this.PfRecords.clear();
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM platformerreocrd WHERE cid = ? ORDER BY stage ASC");
            ps.setInt(1, this.id);

            rs = ps.executeQuery();
            while (rs.next()) {
                int Stage = rs.getInt("stage");
                int ClearTime = rs.getInt("cleartime");
                int Stars = rs.getInt("star");
                PfRecords.add(new PlatformerRecord(Stage, ClearTime, Stars));
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
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
    }

    public void SavePlatformerRecords() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            for (PlatformerRecord rec : this.PfRecords) {
                ps = con.prepareStatement("SELECT * FROM platformerreocrd WHERE cid = ? AND stage = ?");
                ps.setInt(1, this.id);
                ps.setInt(2, rec.getStage());
                rs = ps.executeQuery();
                if (rs.next()) {
                    ps = con.prepareStatement("UPDATE platformerreocrd SET cleartime = ?, star = ? WHERE stage = ?");
                    ps.setInt(1, rec.getClearTime());
                    ps.setInt(2, rec.getStars());
                    ps.setInt(3, rec.getStage());
                    ps.executeUpdate();
                } else {
                    SaveNewRecord(con, ps, rec);
                }
                ps.close();
                rs.close();
            }
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
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
    }

    public List<PlatformerRecord> getPlatformerRecords() {
        return PfRecords;
    }

    public void SaveNewRecord(Connection con, PreparedStatement ps, PlatformerRecord rec) {
        try {
            ps = con.prepareStatement("INSERT INTO platformerreocrd (cid, stage, cleartime, star) VALUES (?, ?, ?, ?)");
            ps.setInt(1, this.id);
            ps.setInt(2, rec.getStage());
            ps.setInt(3, rec.getClearTime());
            ps.setInt(4, rec.getStars());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void RegisterPlatformerRecord(int Stage) {
        int time = (int) ((System.currentTimeMillis() - this.PlatformerStageEnter) / 1000);
        int star = 0;
        if (time <= GameConstants.StarInfo[Stage - 1][0]) {
            star = 3;
        } else if (time <= GameConstants.StarInfo[Stage - 1][1]) {
            star = 2;
        } else if (time <= GameConstants.StarInfo[Stage - 1][2]) {
            star = 1;
        }
        if (this.PfRecords.size() < Stage) { //새로운 도전
            PfRecords.add(new PlatformerRecord(Stage, time, star));
        } else { //기록갱신
            for (PlatformerRecord record : PfRecords) {
                if (record.getStage() == Stage) {
                    if (record.getClearTime() > time || record.getStars() < star) {
                        record.setClearTime(time);
                        record.setStars(star);
                    }
                }
            }
        }
        if (star > 0) {
            for (MapleCharacter chr : this.map.getAllCharactersThreadsafe()) {
                chr.dropMessage(-1, this.name + "님이 별 " + star + "개로 스테이지를 클리어하였습니다!");
            }
            if (this.getKeyValue(20190409, "Stage_" + Stage + "_Received") == -1) {
                this.setKeyValue(20190409, "Stage_" + Stage + "_Received", "0");
            }
            if (this.getKeyValue(20190409, "Stage_" + Stage + "_Received") == 0) {
                if (star == 3) {
                    this.AddStarDustCoin(100);
                } else if (star == 2) {
                    this.AddStarDustCoin(50);
                } else if (star == 1) {
                    this.AddStarDustCoin(10);
                }
                this.setKeyValue(20190409, "Stage_" + Stage + "_Received", "1");
            }
        }
        this.SavePlatformerRecords();
    }

    public void setWeddingGive(int l) {
        weddingGiftGive = l;
    }

    public int getWeddingGive() {
        return weddingGiftGive;
    }

    public long getDojoStartTime() {
        return dojoStartTime;
    }

    public void setDojoStartTime(long dojoStartTime) {
        this.dojoStartTime = dojoStartTime;
    }

    public long getDojoStopTime() {
        return dojoStopTime;
    }

    public void setDojoStopTime(long dojoStopTime) {
        this.dojoStopTime = dojoStopTime;
    }

    public long getDojoCoolTime() {
        return dojoCoolTime;
    }

    public void setDojoCoolTime(long dojoCoolTime) {
        this.dojoCoolTime = dojoCoolTime;
    }

    public boolean isDeadEffect() {
        return deadEffect;
    }

    public void setDeadEffect(boolean deadEffect) {
        this.deadEffect = deadEffect;
    }

    public void applySkill(final int skillid, final int level) {
        SkillFactory.getSkill(skillid).getEffect(level).applyTo(this, true);
    }

    public int getZeroCubePosition() {
        return zeroCubePosition;
    }

    public void setZeroCubePosition(int zeroCubePosition) {
        this.zeroCubePosition = zeroCubePosition;
    }

    public Pair<Integer, Integer> getRecipe() {
        return recipe;
    }

    public void setRecipe(Pair<Integer, Integer> recipe) {
        this.recipe = recipe;
    }

    public final void setAuth(String a) {
        this.auth = a;
    }

    public String getAuth() {
        return this.auth;
    }

    public int getArcSpell() {
        return arcSpell;
    }

    public void setArcSpell(int arcSpell) {
        this.arcSpell = arcSpell;
    }

    public List<Integer> getArcSpellSkills() {
        return arcSpellSkills;
    }

    public void setArcSpellSkills(List<Integer> arcSpellSkills) {
        this.arcSpellSkills = arcSpellSkills;
    }

    public int getTransformCooldown() {
        return transformCooldown;
    }

    public void setTransformCooldown(int transformCooldown) {
        this.transformCooldown = transformCooldown;
    }

    public Set<MapleMapObject> getVisibleMapObjects() {
        return visibleMapObjects;
    }

    public void setVisibleMapObjects(Set<MapleMapObject> visibleMapObjects) {
        this.visibleMapObjects = visibleMapObjects;
    }

    public Map<MapleBuffStat, Pair<MobSkill, Integer>> getDiseases() {
        return diseases;
    }

    public void setDiseases(Map<MapleBuffStat, Pair<MobSkill, Integer>> diseases) {
        this.diseases = diseases;
    }

    public boolean isNoneDestroy() {
        return noneDestroy;
    }

    public void setNoneDestroy(boolean noneDestroy) {
        this.noneDestroy = noneDestroy;
    }

    public List<Integer> getPosionNovas() {
        return posionNovas;
    }

    public void setPosionNovas(List<Integer> posionNovas) {
        this.posionNovas = posionNovas;
    }

    public int getLastBossId() {
        return lastBossId;
    }

    public void setLastBossId(int lastBossId) {
        this.lastBossId = lastBossId;
    }

    public int getMoonGauge() {
        return moonGauge;
    }

    public void setMoonGauge(int lunaGauge) {
        this.moonGauge = lunaGauge;
    }

    public OneCardGame getOneCardInstance() {
        return oneCardInstance;
    }

    public void setOneCardInstance(OneCardGame oneCardInstance) {
        this.oneCardInstance = oneCardInstance;
    }

    public MultiYutGame getMultiYutInstance() {
        return multiYutInstance;
    }

    public void setMultiYutInstance(MultiYutGame multiYutInstance) {
        this.multiYutInstance = multiYutInstance;
    }

    public boolean isOneMoreChance() {
        return oneMoreChance;
    }

    public void setOneMoreChance(boolean oneMoreChance) {
        this.oneMoreChance = oneMoreChance;
    }

    public List<Integer> getExceptionList() {
        return exceptionList;
    }

    public void setExceptionList(List<Integer> exceptionList) {
        this.exceptionList = exceptionList;
    }

    public int getTrinity() {
        return trinity;
    }

    public void setTrinity(int trinity) {
        this.trinity = trinity;
    }

    public UnionList getUnions() {
        return unions;
    }

    public void setUnions(UnionList unions) {
        this.unions = unions;
    }

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    public String getV(String k) {
        if (keyValues.containsKey(k)) {
            return keyValues.get(k);
        }
        return null;
    }

    public void addKV(String k, String v) {
        keyValues.put(k, v);
    }

    public void removeV(String k) {
        keyValues.remove(k);
    }

    public Point getSpecialChairPoint() {
        return specialChairPoint;
    }

    public void setSpecialChairPoint(Point point) {
        this.specialChairPoint = point;
    }

    public void setNettPyramid(MapleNettPyramid mnp) {
        this.NettPyramid = mnp;
    }

    public MapleNettPyramid getNettPyramid() {
        return this.NettPyramid;
    }

    public final boolean isLeader() {
        if (getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == getId();
    }

    public void Message(String msg) {
        client.getSession().writeAndFlush(CField.getGameMessage(8, msg));
    }

    public void changeMap(final MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), CField.getWarpToMap(to, 0, this), to.getPortal(0), false);
    }

    public void message(String msg) {
        client.getSession().writeAndFlush(CWvsContext.serverNotice(5, name, msg));
    }

    public final MapleMap getWarpMap(int map) {
        if (getEventInstance() != null) {
            return getEventInstance().getMapFactory().getMap(map);
        }
        return ChannelServer.getInstance(this.client.getChannel()).getMapFactory().getMap(map);
    }

    public int getSpiritGuard() {
        return spiritGuard;
    }

    public void setSpiritGuard(int spiritGuard) {
        this.spiritGuard = spiritGuard;
    }

    public List<MapleMannequin> getHairRoom() {
        return hairRoom;
    }

    public void setHairRoom(List<MapleMannequin> hairRoom) {
        this.hairRoom = hairRoom;
    }

    public List<MapleMannequin> getFaceRoom() {
        return faceRoom;
    }

    public void setFaceRoom(List<MapleMannequin> faceRoom) {
        this.faceRoom = faceRoom;
    }

    public List<VMatrix> getMatrixs() {
        return matrixs;
    }

    public void setMatrixs(List<VMatrix> matrixs) {
        this.matrixs = matrixs;
    }

    public void setInnerStats(int line) {
        InnerSkillValueHolder isvh = InnerAbillity.getInstance().renewSkill(0, false);
        getInnerSkills().add(isvh);
        changeSkillLevel(SkillFactory.getSkill(isvh.getSkillId()), isvh.getSkillLevel(), isvh.getSkillLevel());
        getClient().getSession().writeAndFlush(CField.updateInnerPotential((byte) line, isvh.getSkillId(), isvh.getSkillLevel(), isvh.getRank()));
    }

    public long getRadomPotal(String k) {
        return getKeyValue(10124, k);
    }

    public void addRadomPotal(String k, String v) {
        setKeyValue(10124, k, v);
    }

    public void removeRadomPotal(String k) {
        removeKeyValue(10124);
    }

    public DefenseTowerWave getDefenseTowerWave() {
        return defenseTowerWave;
    }

    public void setDefenseTowerWave(DefenseTowerWave defenseTowerWave) {
        this.defenseTowerWave = defenseTowerWave;
    }

    public BountyHunting getBountyHunting() {
        return bountyHunting;
    }

    public void setBountyHunting(BountyHunting bountyhunting) {
        this.bountyHunting = bountyhunting;
    }

    public int getBlitzShield() {
        return blitzShield;
    }

    public void setBlitzShield(int blitzShield) {
        this.blitzShield = blitzShield;
    }

    public int[] getDeathCounts() {
        return deathCounts;
    }

    public void setDeathCounts(int[] deathCounts) {
        this.deathCounts = deathCounts;
    }

    public void resetDeathCounts() {
        for (int i = 0; i < deathCounts.length; ++i) {
            deathCounts[i] = 1;
        }
    }

    public int liveCounts() {
        int c = 0;
        for (int i = 0; i < deathCounts.length; ++i) {
            if (deathCounts[i] == 1) {
                c++;
            }
        }
        return c;
    }

    public int getCreateDate() {
        if (createDate > 0) {
            return createDate;
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters where `id` = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("createdate");
                if (ts != null) {
                    createDate = ts.getYear() * 1000 + ts.getMonth() * 100 + ts.getDate();
                }
            }
            ps.close();
            rs.close();
            con.close();
        } catch (SQLException se) {
            se.printStackTrace();
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
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return createDate;
    }

    public void setCreateDate(int createDate) {
        this.createDate = createDate;
    }

    public boolean isUseBuffFreezer() {
        return useBuffFreezer;
    }

    public void setUseBuffFreezer(boolean useBuffFreezer) {
        this.useBuffFreezer = useBuffFreezer;
    }

    public void setSoulMP(Equip weapon) {
        if (weapon != null) {

            int soulSkillID = weapon.getSoulSkill();
            Skill soulSkill = SkillFactory.getSkill(soulSkillID);
            if (soulSkill != null && soulSkillID != 0) {
                if (getSkillLevel(soulSkillID) == 0) {
                    changeSkillLevel(soulSkill, (byte) 1, (byte) 1);
                }

                MapleStatEffect effect = soulSkill.getEffect(1);

                if (getBuffedEffect(MapleBuffStat.SoulMP) != null) {
                    int soulCount = getBuffedValue(MapleBuffStat.SoulMP);
                    if (soulCount < 1000) {
                        setBuffedValue(MapleBuffStat.SoulMP, soulCount + 1);

                        Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
                        localstatups.put(MapleBuffStat.SoulMP, new Pair<>(soulCount + 1, 0));

                        client.getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, effect, this));
                        map.broadcastMessage(BuffPacket.giveForeignBuff(this, localstatups, effect));
                    }

                    if (getBuffedValue(MapleBuffStat.SoulMP) >= effect.getSoulMPCon() && getCooldownLimit(soulSkillID) == 0 && getBuffedEffect(MapleBuffStat.FullSoulMP) == null) {
                        Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
                        localstatups.put(MapleBuffStat.FullSoulMP, new Pair<>(0, 0));
                        client.getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, effect, this));
                        map.broadcastMessage(BuffPacket.giveForeignBuff(this, localstatups, effect));

                        for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : localstatups.entrySet()) {
                            effects.add(new Pair<>(statup.getKey(), new MapleBuffStatValueHolder(effect, System.currentTimeMillis(), null, getBuffedValue(MapleBuffStat.SoulMP), 0, getId())));
                        }

                        client.getSession().writeAndFlush(CWvsContext.enableActions(this, true, false));
                        client.getSession().writeAndFlush(CWvsContext.enableActions(this, false, true));
                    }
                } else {
                    Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
                    localstatups.put(MapleBuffStat.SoulMP, new Pair<>((int) 0, 0));
                    client.getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, effect, this));
                    map.broadcastMessage(BuffPacket.giveForeignBuff(this, localstatups, effect));

                    for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : localstatups.entrySet()) {
                        effects.add(new Pair<>(statup.getKey(), new MapleBuffStatValueHolder(effect, System.currentTimeMillis(), null, 0, 0, getId())));
                    }

                    client.getSession().writeAndFlush(CWvsContext.enableActions(this, true, false));
                    client.getSession().writeAndFlush(CWvsContext.enableActions(this, false, true));
                }
            }
        } else {
            cancelEffectFromBuffStat(MapleBuffStat.SoulMP);
            cancelEffectFromBuffStat(MapleBuffStat.FullSoulMP);
        }
    }

    public void useSoulSkill() {
        if (getBuffedEffect(MapleBuffStat.SoulMP) != null) {
            MapleStatEffect effect = getBuffedEffect(MapleBuffStat.SoulMP);
            if (effect.getSoulMPCon() <= getBuffedValue(MapleBuffStat.SoulMP) && getBuffedEffect(MapleBuffStat.FullSoulMP) != null) {
                setBuffedValue(MapleBuffStat.SoulMP, getBuffedValue(MapleBuffStat.SoulMP) - effect.getSoulMPCon());

                Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
                localstatups.put(MapleBuffStat.SoulMP, new Pair<>(0, 0));
                client.getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, effect, this));
                map.broadcastMessage(BuffPacket.giveForeignBuff(this, localstatups, effect));

                cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, effect.getSourceId());
                cancelEffectFromBuffStat(MapleBuffStat.FullSoulMP);

                client.getSession().writeAndFlush(CWvsContext.enableActions(this, true, false));
                client.getSession().writeAndFlush(CWvsContext.enableActions(this, false, true));
            }
        }
    }

    public List<MapleMannequin> getSkinRoom() {
        return skinRoom;
    }

    public void setSkinRoom(List<MapleMannequin> skinRoom) {
        this.skinRoom = skinRoom;
    }

    public int getOverloadCount() {
        return overloadCount;
    }

    public void setOverloadCount(int overloadCount) {
        this.overloadCount = overloadCount;
    }

    public int getExceed() {
        return exceed;
    }

    public void setExceed(int exceed) {
        this.exceed = exceed;
    }

    public MonsterPyramid getMonsterPyramidInstance() {
        return monsterPyramidInstance;
    }

    public void setMonsterPyramidInstance(MonsterPyramid monsterPyramidInstance) {
        this.monsterPyramidInstance = monsterPyramidInstance;
    }

    public FrittoEagle getFrittoEagle() {
        return frittoEagle;
    }

    public void setFrittoEagle(FrittoEagle frittoEagle) {
        this.frittoEagle = frittoEagle;
    }

    public FrittoEgg getFrittoEgg() {
        return frittoEgg;
    }

    public void setFrittoEgg(FrittoEgg frittoEgg) {
        this.frittoEgg = frittoEgg;
    }

    public FrittoDancing getFrittoDancing() {
        return frittoDancing;
    }

    public void setFrittoDancing(FrittoDancing frittoDancing) {
        this.frittoDancing = frittoDancing;
    }

    public boolean isDuskBlind() {
        return isDuskBlind;
    }

    public void setDuskBlind(boolean duskBlind) {
        isDuskBlind = duskBlind;
    }

    public int getDuskGauge() {
        return duskGauge;
    }

    public void setDuskGauge(int duskGauge) {
        this.duskGauge = duskGauge;
    }

    public long getLastSpawnBlindMobTime() {
        return lastSpawnBlindMobtime;
    }

    public void setLastSpawnBlindMobTime(long lastSpawnBlindMobtime) {
        this.lastSpawnBlindMobtime = lastSpawnBlindMobtime;
    }

    public boolean isExtremeMode() {
        return extremeMode;
    }

    public void setExtremeMode(boolean extremeMode) {
        this.extremeMode = extremeMode;

        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        statups.put(MapleBuffStat.PmdReduce, new Pair<>(-90, -90));
        client.getSession().writeAndFlush(BuffPacket.cancelBuff(statups, this));
    }

    public short getRelicGauge() {
        return relicGauge;
    }

    public void setRelicGauge(short relicGauge) {
        if (!this.getBuffedValue(3310006) && relicGauge == 1000) {
            MapleStatEffect effect = SkillFactory.getSkill(3310006).getEffect(this.getSkillLevel(3310006));
            effect.applyTo(this);
        }
        this.relicGauge = (short) Math.min(1000, relicGauge);
    }

    public void gainRelicGauge(short relicGauge) {
        this.relicGauge += relicGauge;
    }

    public void useRelicGauge(int skillid) {
        if (GameConstants.isPathFinder(getJob())) {
            switch (skillid) {
                case 3301008:
                case 3311010:
                    setRelicGauge((short) (getRelicGauge() - 50));
                    break;
                case 3321012:
                    setRelicGauge((short) (getRelicGauge() - 100));
                    break;
                case 3321036:
                case 3321038:
                case 3321040:
                    MapleStatEffect effect1 = SkillFactory.getSkill(3321035).getEffect(getSkillLevel(3321035));
                    getClient().getSession().writeAndFlush(CField.skillCooldown(3321035, effect1.getCooldown(this)));
                    coolDowns.put(Integer.valueOf(3321014), new MapleCoolDownValueHolder(3321014, System.currentTimeMillis(), effect1.getCooldown(this)));
                    break;
                case 3321014:
                case 3321016:
                case 3321018:
                case 3321020:
                    MapleStatEffect effect2 = SkillFactory.getSkill(3321014).getEffect(getSkillLevel(3321014));
                    getClient().getSession().writeAndFlush(CField.skillCooldown(3321014, effect2.getCooldown(this)));
                    coolDowns.put(Integer.valueOf(3321014), new MapleCoolDownValueHolder(3321014, System.currentTimeMillis(), effect2.getCooldown(this)));
                    //  addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown(this));
                    setRelicGauge((short) (getRelicGauge() - 150));
                    break;
                case 400031034:
                    setRelicGauge(getRelicGauge());
                    break;
                case 400031037:
                case 400031038:
                case 400031039:
                case 400031040:
                    setRelicGauge((short) (getRelicGauge() - 500));
                    break;
            }
            MapleStatEffect effect = SkillFactory.getSkill(skillid).getEffect(getSkillLevel(skillid));
            SkillFactory.getSkill(effect.getSourceId()).getEffect(1).applyKaiserCombo(this, getRelicGauge());
        }
    }

    public void maxskill(int i) {

        if (GameConstants.isHoyeong(i)) {
            if (getSkillLevel(160000076) < 10)
                changeSkillLevel(SkillFactory.getSkill(160000076), (byte) 10, (byte) 10);
        }
        MapleData data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(StringUtil.getLeftPaddedStr("" + i, '0', 3) + ".img");
        byte maxLevel = 0;
        for (MapleData skill : data) {
            if (skill != null) {
                for (MapleData skillId : skill.getChildren()) {
                    if (!skillId.getName().equals("icon")) {
                        maxLevel = (byte) MapleDataTool.getIntConvert("maxLevel", skillId.getChildByPath("common"), 0);
                        if (maxLevel > 30) {
                            maxLevel = 30;
                        }
                        if (MapleDataTool.getIntConvert("invisible", skillId, 0) == 0) { //스킬창에 안보이는 스킬은 올리지않음
                            if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                try {
                                    changeSkillLevel(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), maxLevel, maxLevel);
                                } catch (NumberFormatException ex) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public long getStartThrowBlastingTime() {
        return this.startThrowBlastingTime;
    }

    public void setStartThrowBlastingTime(long time) {
        this.startThrowBlastingTime = time;
    }

    public void createRoyalKnights(AttackInfo attack) {
        if (getBuffedEffect(MapleBuffStat.RoyalKnights) != null && getSkillLevel(400031044) > 0 && (System.currentTimeMillis() - lastRoyalKnightsTime >= 1400)) {
            lastRoyalKnightsTime = System.currentTimeMillis();
            Point pos = attack.position;
            List<AdelProjectile> atoms = new ArrayList<>();
            int knight = Randomizer.rand(1, 4);
            for (int i = 0; i < knight; i++) {
                atoms.add(new AdelProjectile(14, getId(), 0, 400031045, 10000, 0, 1, pos, new ArrayList<>()));
            }
            if (!atoms.isEmpty()) {
                getMap().spawnAdelProjectile(this, atoms, false);
            }
        }
    }

    public int getMjollnir() {
        return Mjollnir;
    }

    public void setMjollnir(int m) {
        Mjollnir = m;
    }

    public int getHuntingDecreeSize() {
        return huntingdecree;
    }

    public void setHuntingDecree(int i) {
        this.huntingdecree = i;
    }

    public void addHuntingDecree(int i) {
        if (huntingdecree >= 0 && huntingdecree < 6) {
            this.huntingdecree += i;
        } else {
            huntingdecree = 0;
        }
    }

    public long getStartRelicUnboundTime() {
        return this.startThrowBlastingTime;
    }

    public void setStartRelicUnboundTime(long time) {
        this.startThrowBlastingTime = time;
    }

    public long getStartFullMakerTime() {
        return this.startFullMakerTime;
    }

    public void setStartFullMakerTime(long time) {
        this.startFullMakerTime = time;
    }

    public int getFullMakerBox() {
        return this.FullMakerBoxCount;
    }

    public void setFullMakerBox(int count) {
        this.FullMakerBoxCount = count;
    }

    public long getStartRepeatinCartrigeTime() {
        return this.startRepeatinCartrigeTime;
    }

    public void setStartRepeatinCartrigeTime(long time) {
        this.startRepeatinCartrigeTime = time;
    }

    public int getHotelEventExp() {
        if (getClient().getKeyValue("hotelExp") == null) {
            getClient().setKeyValue("hotelExp", "0");
        }
        return Integer.valueOf(getClient().getKeyValue("hotelExp"));
    }

    public void setHotelEventExp(int exp) {
        getClient().setKeyValue("hotelExp", exp + "");
    }

    public void addHotelEventExp(int exp) {
        int expplus = getHotelEventExp() + exp;
        getClient().setKeyValue("hotelExp", expplus + "");
    }
    long screwtime = 0;

    public long getScrewTime() {
        return Long.valueOf(getV("screw"));
    }

    public void setScrewTime(long time) {
        addKV("screw", "" + time);
    }

    public int getNovilityBarrier() {
        return novilitybarrier;
    }

    public void setNovilityBarrier(int barrier) {
        this.novilitybarrier = barrier;
    }

    public int reduceNovilityhandle(int damage) {
        int dam = damage;
        if (getParty() != null) {
            for (MaplePartyCharacter partychr : getParty().getMembers()) {
                MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(partychr.getId());
                if (chr != null && GameConstants.isAdele(chr.getJob()) && chr.getBuffedValue(151111005) && dam > 0) {
                    dam = damage - (int) (damage * 0.3);
                }
                if (chr.getId() == getId() && GameConstants.isAdele(getJob()) && getBuffedValue(151111005)) {
                    addHP((int) (-damage * 0.3));
                }
            }
        }
        return dam;
    }

    public void isNovilityhandle(int damage) {
        if (getParty() != null) {
            for (MaplePartyCharacter partychr : getParty().getMembers()) {
                MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(partychr.getId());
                if (chr != null && GameConstants.isAdele(chr.getJob()) && chr.getBuffedValue(151111005) && damage > 0) {
                    long barrier = (long) (damage * 0.8);
                    long duration = chr.getBuffLimit(151111005);
                    setNovilityBarrier(damage);
                    if (getSkillLevel(151111005) < 0) {
                        changeSkillLevel(151111005, (byte) chr.getSkillLevel(151111005), (byte) chr.getSkillLevel(151111005));
                    }
                    SkillFactory.getSkill(151111005).getEffect(getSkillLevel(151111005)).applyTo(this, false, (int) duration);
                }
            }
        } else {
            if (GameConstants.isAdele(getJob()) && getBuffedValue(151111005) && damage > 0) {
                long barrier = (long) (damage * 0.8);
                long duration = getBuffLimit(151111005);
                setNovilityBarrier(damage);
                SkillFactory.getSkill(151111005).getEffect(getSkillLevel(151111005)).applyTo(this, false, (int) duration);
            }
        }
    }

    public long lastskill = 0;

    public long lastSkill() {
        return this.lastskill;
    }

    public void skillReset() {
        List<Skill> skillss = new ArrayList<Skill>();
        for (Skill skill : skills.keySet()) {
            skillss.add(skill);
        }
        for (Skill i : skillss) {
            changeSkillLevel(i, (byte) 0, (byte) 0);
        }
    }
    private int darknessaura = 0;
    private long startDarknessAuraTime = 0;

    public int getDarknessAura() {
        return darknessaura;
    }

    public void setDarknessAura(int aura) {
        darknessaura = aura;
    }

    public void addDarknessAura(int aura) {
        if (darknessaura < 15) {
            darknessaura += aura;
        }
    }

    public long getStartDarknessAuraTime() {
        return this.startDarknessAuraTime;
    }

    public void setStartDarknessAuraTime(long time) {
        this.startDarknessAuraTime = time;
    }

    public void calcKainDeathStackMobs() {
        List<Triple<MapleMonster, Integer, Long>> ms = new ArrayList<>(); //mob , stack, timesec
        int a = kainblessmobs.size();
        ms = kainblessmobs;

        for (int i = 0; i < ms.size(); i++) {
            if (!ms.get(i).getLeft().isAlive() || ms.get(i).mid <= 0 || System.currentTimeMillis() - ms.get(i).getRight() > 90000) { //몹이 죽은 경우, 스택 0 이하인경우, 스택시간 지난경우
                kainblessmobs.remove(i);
            }
        }
    }

    public void 보물의룬시작() {
        MapleCharacter chr = this;
        BuffTimer tMan = BuffTimer.getInstance();

        Runnable r;
        final MapleMonster mob1 = MapleLifeFactory.getMonster(8220028);
        chr.getMap().spawnMonsterOnGroundBelow(mob1, new Point(chr.getPosition().x, chr.getPosition().y - 50));

        r = new Runnable() {
            @Override
            public void run() {
                MapleMonster dropper = chr.getMap().getMonsterByOid(mob1.getObjectId());
                Item toDrop;
                if (chr.보물룬아이템카운트 >= 10) {
                    if (brune != null) {
                        brune.cancel(true);
                        brune = null;
                    }
                    map.killMonster(dropper, chr, false, false, (byte) 1);
                    chr.보물룬아이템카운트 = 0;
                }
                if (chr.보물룬아이템카운트 <= 1) {
                    toDrop = new client.inventory.Item(4001714, (byte) 0, (short) 1, (byte) 0);
                    chr.getMap().spawnItemDrop(dropper, chr, toDrop, dropper.getTruePosition(), true, false);
                } else if (chr.보물룬아이템카운트 <= 3) {
                    toDrop = new client.inventory.Item(4310229, (byte) 0, (short) 1, (byte) 0);
                    chr.getMap().spawnItemDrop(dropper, chr, toDrop, dropper.getTruePosition(), true, false);
                } else if (chr.보물룬아이템카운트 <= 6) {
                    toDrop = new client.inventory.Item(4310237, (byte) 0, (short) 1, (byte) 0);
                    chr.getMap().spawnItemDrop(dropper, chr, toDrop, dropper.getTruePosition(), true, false);
                } else if (chr.보물룬아이템카운트 <= 9) {
                    toDrop = new client.inventory.Item(2048716, (byte) 0, (short) 1, (byte) 0);
                    chr.getMap().spawnItemDrop(dropper, chr, toDrop, dropper.getTruePosition(), true, false);
                }
                chr.보물룬아이템카운트++;
            }
        };
        brune = tMan.register(r, 1000, 1000);
    }

    public void setByNameValue(String name, int value, long time) {
        for (int i = 0; i < NameValue.size(); i++) {
            if (NameValue.get(i).getName().equals(name)) {
                NameValue.set(i, new ByNameValue(name, value, time));
            }
        }
        NameValue.add(new ByNameValue(name, value, time));
    }

    public ByNameValue getByNameValue(String name) {
        for (ByNameValue bnv : NameValue) {
            if (bnv.getName().equals(name)) {
                return bnv;
            }
        }
        return null;
    }

    public String getByNameStringValue(String name) {
        for (ByNameValue bnv : NameValue) {
            if (bnv.getName().equals(name)) {
                return bnv.getName();
            }
        }
        return null;
    }

    public int getByNameIntValue(String name) {
        for (ByNameValue bnv : NameValue) {
            if (bnv.getName().equals(name)) {
                return bnv.getValue();
            }
        }
        return -1;
    }

    public long getByNameLongValue(String name) {
        for (ByNameValue bnv : NameValue) {
            if (bnv.getName().equals(name)) {
                return bnv.getTime();
            }
        }
        return -1;
    }

    public List<ByNameValue> getNameValues() {
        return NameValue;
    }

    public void setPoloMobExp(long a) {
        PoloMobexp = a;
    }

    public long getPoloMobexp() {
        return PoloMobexp;
    }

    public int getPoloMobLevel() {
        return PoloMobLevel;
    }

    public long getPoloMobHp() {
        return PoloMobhp;
    }

    public boolean setPoloMob() {
        if (getMap().getAllMonstersThreadsafe().size() <= 0) {
            return false;
        }
        for (MapleMonster m : getMap().getAllMonstersThreadsafe()) {
            MapleMonsterStats st = m.getStats();
            PoloMobexp = st.getExp();
            PoloMobLevel = st.getLevel();
            PoloMobhp = st.getHp();
            break;
        }
        return true;
    }

    public void gainNeoCore(int core) {
        if (getKeyValue(501215, "point") < 0) {
            setKeyValue(501215, "point", 0 + "");
        }
        if (getKeyValue(501215, "today") < 0) {
            setKeyValue(501215, "today", 0 + "");
        }
        if (isLockNeoCore()) {
            dropMessage(-8, "하루동안 획득 가능한 네오 코어량을 초과하여 네오 코어를 획득하지 않습니다.");
        } else {
            int a = (int) getKeyValue(501215, "today");
            int todaycore = a + core;
            if (todaycore > ServerConstants.MaxNeoCore) {
                core = todaycore - ServerConstants.MaxNeoCore;
                todaycore = ServerConstants.MaxNeoCore;
            }

            setKeyValue(501215, "today", todaycore + "");
            int corecount = (int) getKeyValue(501215, "point") + core;
            setKeyValue(501215, "point", corecount + "");

            int lock = isLockNeoCore() ? 1 : 0;

            getClient().getSession().writeAndFlush(SLFCGPacket.StarDustUI("UI/UIWindowEvent.img/2020neoCoin", 0, 1));
            updateInfoQuest(501215, "point=" + getKeyValue(501215, "point") + ";sum=0;date=" + GameConstants.getCurrentDate_NoTime() + ";week=0;total=0;today=" + getKeyValue(501215, "today") + ";lock=" + lock + ""); //네오 코어
            dropMessage(-8, "[알림] " + core + " 개의 네오 코어를 획득하였습니다.");
        }
    }

    public boolean isLockNeoCore() {
        if (getKeyValue(501215, "today") >= ServerConstants.MaxNeoCore) {
            return true;
        } else {
            return false;
        }
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public void gainTier(int tier) {
        this.tier += tier;
    }

    public int getBossTier() {
        return bosstier;
    }

    public void setBossTier(int tier) {
        this.bosstier = tier;
    }

    public void gainBossTier(int tier) {
        this.bosstier += tier;
    }

    public boolean hasDonationSkill(int skillid) {
        if (this.getKeyValue(201910, "DonationSkill") < 0) {
            this.setKeyValue(201910, "DonationSkill", "0");
        }

        MapleDonationSkill dskill = MapleDonationSkill.getBySkillId(skillid);
        if (dskill == null) {
            return false;
        } else if ((this.getKeyValue(201910, "DonationSkill") & dskill.getValue()) == 0) {
            return false;
        }
        return true;
    }

    public void gainDonationSkills() {
        if (getKeyValue(201910, "DonationSkill") > 0) {
            for (final MapleDonationSkill stat : MapleDonationSkill.values()) {

                //  if (stat.getSkillId() != 5321054) {
                if ((getKeyValue(201910, "DonationSkill") & stat.getValue()) != 0) {
                    getStat().setMp(getStat().getCurrentMaxMp(this), this);
                    if (!getBuffedValue(stat.getSkillId())) {
                        SkillFactory.getSkill(stat.getSkillId()).getEffect(SkillFactory.getSkill(stat.getSkillId()).getMaxLevel()).applyTo(this, 0);
                    }
                    //      }
                }
            }
        }
    }

    public void gainDonationSkill(int skillid) {
        if (this.getKeyValue(201910, "DonationSkill") < 0) {
            this.setKeyValue(201910, "DonationSkill", "0");
        }

        MapleDonationSkill dskill = MapleDonationSkill.getBySkillId(skillid);
        if (dskill != null && (this.getKeyValue(201910, "DonationSkill") & dskill.getValue()) == 0) {
            int data = (int) this.getKeyValue(201910, "DonationSkill");
            data |= dskill.getValue();
            this.setKeyValue(201910, "DonationSkill", data + "");
            SkillFactory.getSkill(skillid).getEffect(SkillFactory.getSkill(skillid).getMaxLevel()).applyTo(this, 0);
        }
    }

    public long getBaseBossPoint() {
        return basebpoint;
    }

    public long getBossPoint() {
        return bpoint;
    }

    public void gainBossPoint(long point) {
        this.bpoint += point;
        this.basebpoint += point;
    }

    public void loseBossPoint(long point) {
        this.bpoint -= point;
    }

    public void setBossPoint(long point) {
        this.bpoint = point;
    }

    public final void MakeCDitem(int itemid, int count, boolean a) {
        if (getKeyValue(800023, "IndieCrMax") <= 0) {
            setKeyValue(800023, "IndieCrMax", "0");
        }
        if (a) {
            setKeyValue(800023, "IndieCrMax", String.valueOf((getKeyValue(800023, "IndieCrMax")) + count));
        }
        while (getBuffedValue(80002388)) {
            cancelEffect(getBuffedEffect(80002388), false, -1);
        }
        SkillFactory.getSkill(80002388).getEffect(1).applyTo(this);
        //gainItem(itemid, -1);
    }

    /**
     * @return the lastFishingTime
     */
    public long getLastFishingTime() {
        return lastFishingTime;
    }

    /**
     * @param lastFishingTime the lastFishingTime to set
     */
    public void setLastFishingTime(long lastFishingTime) {
        this.lastFishingTime = lastFishingTime;
    }

    public long getLevelPoint() {
        return levelpoint;
    }

    /**
     * @param levelpoint the levelpoint to set
     */
    public void setLevelPoint(long levelpoint) {
        this.levelpoint = levelpoint;
    }

    public int getCustomItem(int id) {
        int size = (int) getKeyValue(100000, id+"");
        if (size == -1) {
            size = 0;
            setKeyValue(100000, id+"", "0");
        }
        return size;
    }

    public List<Integer> getCustomInventory() {
        List<Integer> inventory = new ArrayList<>();
        List<CustomItem> list = GameConstants.customItems;
        for (CustomItem item : list) {
            int size = getCustomItem(item.getId());
            inventory.add(size);
        }

        return inventory;
    }

    public void addCustomItem(int id) {
        int size = getCustomItem(id);
        setKeyValue(100000, id+"", ++size+"");
    }

    public int equippedCustomItem(CustomItem.CustomItemType type) {
        int id = (int) getKeyValue(100000 + type.ordinal(), "equip");
        return id;
    }

    public void equipCustomItem(int id) {
        List<CustomItem> list = GameConstants.customItems;
        CustomItem ci = list.get(id);
        setKeyValue(100000 + ci.getType().ordinal(), "equip", id+"");
    }

    public void unequipCustomItem(int id) {
        List<CustomItem> list = GameConstants.customItems;
        CustomItem ci = list.get(id);
        setKeyValue(100000 + ci.getType().ordinal(), "equip", "-1");
    }

    public void refreshGiftShowX3() {
        String currentDate = GameConstants.getCurrentDate_NoTime2();
        String dbDate = getKeyValueStr(100857, "date");

        if (getKeyValue(100857, "count") == -1 || (dbDate != null && Integer.parseInt(currentDate.replace("/", "")) != Integer.parseInt(dbDate.replace("/", "")))) {
            setKeyValue(100857, "count", "0");
            setKeyValue(100857, "feverCnt", "0");
            setKeyValue(100857, "date", GameConstants.getCurrentDate_NoTime2());
        }
    }

    public void finishGiftShowX3(MapleMap currentMap) {

        if (getBuffedValue(80003082) && getBuffedValue(MapleBuffStat.EventSpecialSkill) > 1) {
            setKeyValue(100857, "genMobKillCount", "0");

            MapleStatEffect effect = SkillFactory.getSkill(80003082).getEffect(getSkillLevel(80003082));

            cancelEffect(effect, true, -1);
            effect.applyTo(this, true);
            getClient().getSession().writeAndFlush(CField.ClearClock());
            getClient().getSession().writeAndFlush(CField.ShowDetailShowInfo(3, 0x14, 0x14, "오늘의 쇼타임 횟수 : " + (int) getKeyValue(100857, "feverCnt") + "/10회"));
            getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062549, 3000, "선물을 주는 건 참 행복한 일이야.\r\n#b" + getName() + "#k!\r\n너도 행복하지", ""));

            if (currentMap != null)
                for (MapleMonster mob : currentMap.getAllMonstersThreadsafe()) {
                    if (mob.getId() == 9833971) {
                        currentMap.killMonster(mob);
                    }
                }
        }
    }

    public void startGiftShowX3() {
        setKeyValue(100857, "count", "0");
        int afterCnt = (int) getKeyValue(100857, "feverCnt") + 1;

        MapleStatEffect effect = SkillFactory.getSkill(80003082).getEffect(getSkillLevel(80003082));
        setKeyValue(100857, "feverCnt", afterCnt + "");
        setKeyValue(100857, "genMobKillCount", "0");

        effect.applyTo(this, false);
        getClient().getSession().writeAndFlush(CField.ShowMSClock(30000));

        final List<Spawns> randomSpawn = new ArrayList<Spawns>(getMap().getSpawnPoints());
        Collections.shuffle(randomSpawn);

        int i = 0;
        for (Spawns spawnPoint : randomSpawn) {
            MapleMonster mob = MapleLifeFactory.getMonster(9833971);
            mob.setOwner(getId());
            getMap().spawnMonsterOnGroundBelow(mob, spawnPoint.getPosition());
            if (++i >= 5)
                break;
        }

        MapleCharacter chr = this;
        MapleMap currentMap = chr.getMap();
        Timer.MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (chr == null)
                    return;

                chr.finishGiftShowX3(currentMap);
            }
        }, 30000);


    }

    public void setEffect(int index, int value) {
        setKeyValue(0, "effect-" + index, String.valueOf(value));
    }

    public int getEffectValue(int index) {
        int value = (int) getKeyValue(0, "effect-" + index);
        if (value == -1) {
            setEffect(index, 0);
            return 0;
        }
        return value;
    }

    public List<Integer> getPrevBonusEffect() {
        List<Integer> effects = new ArrayList<>();

        for (int i = 0; i < 8; i++)
            effects.add(getEffectValue(i));

        return effects;
    }

    public List<Integer> getBonusEffect() {
        List<Integer> effects = new ArrayList<>();
        int damR = 0;
        int expR = 0;
        int dropR = 0;
        int mesoR = 0;
        int crD = 0;
        int bdR = 0;
        int allStatR = 0;
        int pmdR = 0;

        long zodiacRank = getKeyValue(190823, "grade");
        if (zodiacRank > 0) {
            dropR += 10 * zodiacRank;
            mesoR += 10 * zodiacRank;
            crD += 5 * zodiacRank;
            bdR += 5 * zodiacRank;
        }

        int bossTier = getBossTier();
        if (bossTier > 0) {
            bdR += bossTier * 10;
        }

        for (CustomItem.CustomItemType type : CustomItem.CustomItemType.values()) {
            if (type.ordinal() == 0)
                continue;

            int id = equippedCustomItem(type);
            if (id >= 0) {
                for (Pair<CustomItem.CustomItemEffect, Integer> effect : GameConstants.customItems.get(id).getEffects()) {
                    if (effect.getLeft() == CustomItem.CustomItemEffect.BdR) {
                        bdR += effect.getRight();
                    } else if (effect.getLeft() == CustomItem.CustomItemEffect.CrD) {
                        crD += effect.getRight();
                    } else if (effect.getLeft() == CustomItem.CustomItemEffect.DropR) {
                        dropR += effect.getRight();
                    } else if (effect.getLeft() == CustomItem.CustomItemEffect.MesoR) {
                        mesoR += effect.getRight();
                    } else if (effect.getLeft() == CustomItem.CustomItemEffect.AllStatR) {
                        allStatR += effect.getRight();
                    }
                }
            }
        }

        if (getKeyValue(999, "DamageTear") > 0) {
            int tear = (int) getKeyValue(999, "DamageTear");
            damR += tear == 8 ? 80 : tear == 7 ? 60 : tear == 6 ? 40 : tear == 5 ? 30 : tear == 4 ? 25 : tear == 3 ? 20 : tear == 2 ? 15 : 10;
        }
        if (getKeyValue(999, "ExpTear") > 0) {
            int tear = (int) getKeyValue(999, "ExpTear");
            expR += tear == 8 ? 50 : tear == 7 ? 40 : tear == 6 ? 30 : tear == 5 ? 23 : tear == 4 ? 18 : tear == 3 ? 13 : tear == 2 ? 7 : 3;
        }
        if (getKeyValue(999, "DropTear") > 0) {
            int tear = (int) getKeyValue(999, "DropTear");
            dropR += tear == 8 ? 300 : tear == 7 ? 180 : tear == 6 ? 120 : tear == 5 ? 80 : tear == 4 ? 60 : tear == 3 ? 40 : tear == 2 ? 20 : 10;
        }
        if (getKeyValue(999, "MesoTear") > 0) {
            int tear = (int) getKeyValue(999, "MesoTear");
            mesoR += tear == 8 ? 120 : tear == 7 ? 100 : tear == 6 ? 90 : tear == 5 ? 80 : tear == 4 ? 60 : tear == 3 ? 40 : tear == 2 ? 20 : 10;
        }
        if (getKeyValue(999, "CridamTear") > 0) {
            int tear = (int) getKeyValue(999, "CridamTear");
            crD += tear == 8 ? 100 : tear == 7 ? 70 : tear == 6 ? 50 : tear == 5 ? 30 : tear == 4 ? 20 : tear == 3 ? 15 : tear == 2 ? 10 : 5;
        }
        if (getKeyValue(999, "BossdamTear") > 0) {
            int tear = (int) getKeyValue(999, "BossdamTear");
            bdR += tear == 8 ? 150 : tear == 7 ? 120 : tear == 6 ? 100 : tear == 5 ? 70 : tear == 4 ? 50 : tear == 3 ? 30 : tear == 2 ? 20 : 10;
        }

        int hGrade = getHgrade();

        if (hGrade == 1) {
            damR += 5;
            bdR += 10;
            crD += 5;
            allStatR += 10;
        } else if (hGrade == 2) {
            expR += 10;
            damR += 10;
            bdR += 20;
            crD += 10;
            allStatR += 15;
        } else if (hGrade == 3) {
            expR += 20;
            damR += 20;
            bdR += 30;
            crD += 15;
            allStatR += 25;
        } else if (hGrade == 4) {
            expR += 30;
            pmdR += 20;
            damR += 30;
            bdR += 40;
            crD += 30;
            allStatR += 40;
        } else if (hGrade == 5) {
            expR += 40;
            pmdR += 30;
            damR += 40;
            bdR += 70;
            crD += 50;
            allStatR += 50;
        } else if (hGrade == 6) {
            expR += 50;
            pmdR += 40;
            damR += 50;
            bdR += 100;
            crD += 70;
            allStatR += 60;
        }

        effects.add(damR);
        effects.add(expR);
        effects.add(dropR);
        effects.add(mesoR);
        effects.add(crD);
        effects.add(bdR);
        effects.add(allStatR);
        effects.add(pmdR);

        return effects;
    }

    public void InGameDirectionEvent(String str, int... args) {
        this.getClient().getSession().writeAndFlush(SLFCGPacket.InGameDirectionEvent(str, args));
    }

    public void setInGameDirectionMode(boolean Enable, boolean BlackFrame, boolean ForceMouseOver, boolean ShowUI) {
        this.getClient().getSession().writeAndFlush(SLFCGPacket.SetIngameDirectionMode(Enable, BlackFrame, ForceMouseOver, ShowUI));
    }

    public void removeInGameDirectionMode() {
        this.getClient().getSession().writeAndFlush(SLFCGPacket.removeIngameDirectionMode());
    }

    public void skillMaster() {
        MapleData data = MapleDataProviderFactory.getDataProvider(MapleDataProviderFactory.fileInWZPath("Skill.wz")).getData(StringUtil.getLeftPaddedStr("" + getJob(), '0', 3) + ".img");
        dropMessage(5, "스킬마스터가 완료되었습니다.");
        if (getLevel() < 10) {
            dropMessage(1, "레벨 10 이상 부터 사용 할 수 있습니다.");
            return;
        }
        for (int i = 0; i < (getJob() % 10) + 1; i++) {
            maxskill(((i + 1) == ((getJob() % 10) + 1)) ? getJob() - (getJob() % 100) : getJob() - (i + 1));
        }
        maxskill(getJob());
        if (GameConstants.isDemonAvenger(getJob())) {
            maxskill(3101);
        }

        if (GameConstants.isZero(getJob())) {
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
                                if (maxLevel > 30) { // 배틀메이지 데스는 왜 만렙이 250이지?
                                    maxLevel = 30;
                                }
                                if (MapleDataTool.getIntConvert("invisible", skillId, 0) == 0) { //스킬창에 안보이는 스킬은 올리지않음
                                    if (getLevel() >= MapleDataTool.getIntConvert("reqLev", skillId, 0)) {
                                       changeSingleSkillLevel(SkillFactory.getSkill(Integer.parseInt(skillId.getName())), maxLevel, maxLevel);
                                    }
                                }
                            }
                        }
                    }
                }
                if (getLevel() >= 200) {
                    changeSingleSkillLevel(SkillFactory.getSkill(100001005), (byte) 1, (byte) 1);
                }
            }
        }
        if (GameConstants.isKOC(getJob()) && getLevel() >= 100) {
            changeSkillLevel(11121000, (byte) 30, (byte) 30);
            changeSkillLevel(12121000, (byte) 30, (byte) 30);
            changeSkillLevel(13121000, (byte) 30, (byte) 30);
            changeSkillLevel(14121000, (byte) 30, (byte) 30);
            changeSkillLevel(15121000, (byte) 30, (byte) 30);
        }
    }

    public int getSerenStunGauge() {
        return SerenStunGauge;
    }

    public void setSerenStunGauge(int SerenStunGauge) {
        SerenStunGauge = SerenStunGauge;
    }

    public void addSerenGauge(int add) {
        if (!hasDisease(MapleBuffStat.SerenDebuff)) {
            SerenStunGauge += add;
            if (SerenStunGauge >= 1000) {
                SerenStunGauge = 0;
                Map<MapleBuffStat, Pair<Integer, Integer>> diseases = new EnumMap<>(MapleBuffStat.class);
                diseases.put(MapleBuffStat.GiveMeHeal, new Pair(Integer.valueOf(1), Integer.valueOf(5000)));
                diseases.put(MapleBuffStat.SerenDebuff, new Pair(Integer.valueOf(1), Integer.valueOf(5000)));
                diseases.put(MapleBuffStat.SerenDebuffUnk, new Pair(Integer.valueOf(1), Integer.valueOf(5000)));

                client.getSession().writeAndFlush(SLFCGPacket.playSound("Sound/Field.img/SerenDeath/effect"));
                client.getSession().writeAndFlush(SLFCGPacket.PoloFrittoEffect(4, "UI/UIWindow7.img/SerenDeath"));
                giveDebuff(diseases, MobSkillFactory.getMobSkill(182, 3));
                if (getMapId() == 410002060) {
                    MapleMonster seren = getMap().getMonsterById(8880602);
                    if (seren != null) {
                        seren.setSerenMidNightSetTotalTime(seren.getSerenMidNightSetTotalTime() - 1);
                        if (seren.getSerenMidNightSetTotalTime() <= 0) {
                            getMap().broadcastMessage(MobPacket.BossSeren.SerenTimer(1, new int[]{120, 120, 0, 120, (seren.getSerenTimetype() == 4) ? -1 : 1}));
                            getMap().killAllMonsters(false);
                            getMap().broadcastMessage(SLFCGPacket.BlackLabel("#fn나눔고딕 ExtraBold##fs32##r#e태양이 지지 않는다면 누구도 나에게 대항할 수 없다.", 100, 1500, 4, 0, 0, 1, 4));
                            for (MapleCharacter chr : getMap().getAllCharactersThreadsafe()) {
                                if (chr != null) {
                                    chr.warpdelay(410000670, 7);
                                }
                            }
                        } else {
                            switch (seren.getSerenTimetype()) {
                                case 1:
                                    seren.setSerenNoonNowTime(seren.getSerenNoonNowTime() + 1);
                                    seren.setSerenNoonTotalTime(seren.getSerenNoonTotalTime() + 1);
                                    break;
                                case 2:
                                    seren.setSerenSunSetNowTime(seren.getSerenSunSetNowTime() + 1);
                                    seren.setSerenSunSetTotalTime(seren.getSerenSunSetTotalTime() + 1);
                                    break;
                                case 4:
                                    seren.setSerenDawnSetNowTime(seren.getSerenDawnSetNowTime() + 1);
                                    seren.setSerenDawnSetTotalTime(seren.getSerenDawnSetTotalTime() + 1);
                                    break;
                            }
                            if (seren.getSerenTimetype() != 3) {
                                seren.AddSerenTotalTimeHandler(seren.getSerenTimetype(), 1, (seren.getSerenTimetype() == 4) ? -1 : 1);
                            }
                        }
                    }
                }
            }
            if (this.SerenStunGauge < 0) {
                this.SerenStunGauge = 0;
            }
            this.client.getSession().writeAndFlush(MobPacket.BossSeren.SerenUserStunGauge(1000, this.SerenStunGauge));
        }
    }

    public void giveBlackMageDebuff() {
        int blackMageWB = 1;

        this.getDiseases().put(MapleBuffStat.BlackMageDebuff, new Pair<>(null, 0));

        Map<MapleBuffStat, Pair<Integer, Integer>> diseases = new HashMap<>();
        diseases.put(MapleBuffStat.BlackMageDebuff, new Pair<>(blackMageWB, 0));

        client.getSession().writeAndFlush(BuffPacket.giveDisease(diseases, null, this));
        map.broadcastMessage(this, BuffPacket.giveForeignDeBuff(this, diseases), false);
    }
}

