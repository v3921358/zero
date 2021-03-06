package server;

import client.MapleBuffStat;
import client.MapleBuffStatValueHolder;
import client.MapleCharacter;
import client.MapleCoolDownValueHolder;
import client.MapleStat;
import client.MapleTrait.MapleTraitType;
import client.PlayerStats;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;
import client.custom.inventory.CustomItem;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleDataType;
import server.Timer.BuffTimer;
import server.life.MapleMonster;
import server.maps.ForceAtom;
import server.maps.MapleAtom;
import server.maps.MapleDoor;
import server.maps.MapleExtractor;
import server.maps.MapleFoothold;
import server.maps.MapleFootholdTree;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.maps.MapleSummon;
import server.maps.MechDoor;
import server.maps.SummonMovementType;
import tools.CaltechEval;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;

public class MapleStatEffect implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private byte mastery, mobCount, attackCount, bulletCount, reqGuildLevel, period, expR, iceGageCon, // 1
            // =
            // party
            // 2
            // =
            // nearby
            recipeUseCount, recipeValidDay, reqSkillLevel, slotCount, effectedOnAlly, effectedOnEnemy, type,
            preventslip, immortal, bs, powerCon;
    private short hp, hpFX, hcHp, mp, mhpR, mmpR, pad, padR, mad, madR, pdd, mdef, acc, avoid, hands, speed, jump, psdSpeed, psdJump, mdf,
            mpCon, hpCon, forceCon, comboConAran, bdR, damage, prop, subprop, emhp, emmp, epad, emad, epdd, emdd, ignoreMobpdpR, ignoreMobDamR,
            dot, dotTime, dotInterval, dotSuperpos, criticaldamage, pddX, mddX, pddR, mddR, asrR, terR, er, padX, madX, mesoR, thaw,
            selfDestruction, PVPdamage, indiePad, indiePadR, indieMad, indieDamReduceR, indieMadR, indiePMd,
            fatigueChange, onActive, str, dex, int_, luk, strX, dexX, intX, lukX, lifeId, imhp, immp, inflation,
            useLevel, mpConReduce, soulmpCon, indieDEX, indieCr, indieMhp, indieMmp, indieStance, indieAllStat, indieSpeed,
            indieBooster, indieJump, indieAcc, indieEva, indieEvaR, indiePdd, indieMdd, incPVPdamage, indieMhpR, indieMmpR,
            indieAsrR, indieTerR, indieDamR, indieBDR, indieCD, indieIgnoreMobpdpR, indiePddR, IndieExp, indieStatRBasic, indieCooltimeReduce, mobSkill, mobSkillLevel,
            indiePmdR, morph, lv2mhp, lv2mmp, bufftimeR, summonTimeR, killRecoveryR, dotHealHPPerSecondR, targetPlus, targetPlus_5th, nocoolProp, damAbsorbShieldR; // ar = accuracy rate
    private double hpR, hpRCon, mpR, expRPerM;
    private Map<MapleTraitType, Integer> traits = new HashMap<>();
    private int duration, subTime, ppcon, ppReq, ppRecovery, sourceid, recipe, moveTo, stanceProp, t, u, u2, v, v2, w, w2, x, y, z,
            s, s2, q, q2, cr, itemCon, itemConNo, bulletConsume, moneyCon, damR, speedMax, accX, mhpX, mmpX, cooltime, cooltimeMS, coolTimeR,
            morphId = 0, expinc, exp, monsterRidingId, consumeOnPickup, range, price, extendPrice,
            charColor, interval, rewardMeso, totalprob, cosmetic, kp;
    private boolean skill, partyBuff = true;
    private Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
    private ArrayList<Pair<Integer, Integer>> availableMap;
//    private EnumMap<MonsterStatus, Integer> monsterStatus;
    private Point lt, rb;
    private boolean energyChargeCooling = false, energyChargeActived = false;
    private int expBuff, itemup, mesoup, cashup, berserk, illusion, berserk2, cp, nuffSkill, eqskill1, eqskill2,
            eqskill3;
    private long starttime;
    private byte level;
    private List<Integer> petsCanConsume, randomPickup;
    private List<Triple<Integer, Integer, Integer>> rewardItem;

    public static final MapleStatEffect loadSkillEffectFromData(final MapleData source, final int skillid,
            final boolean overtime, final int level, final String variables) {
        return loadFromData(source, skillid, true, overtime, level, variables);
    }

    public static final MapleStatEffect loadItemEffectFromData(final MapleData source, final int itemid) {
        return loadFromData(source, itemid, false, false, 1, null);
    }

    private final static Point parsePoint(String path, MapleData source, Point def, String variables, int level) {
        if (variables == null) {
            return MapleDataTool.getPoint(path, source, def);
        } else {
            final MapleData dd = source.getChildByPath(path);
            if (dd == null) {
                return def;
            }
            if (dd.getType() != MapleDataType.STRING) {
                return MapleDataTool.getPoint(path, source, def);
            }
            //System.out.println("DATA : " + MapleDataTool.getString(dd));
        }
        return null;
    }

    public static int parseEval(String data, int level) {
        String variables = "x";
        String dddd = data.replace(variables, String.valueOf(level));
        if (dddd.substring(0, 1).equals("-")) { // -30+3*x
            if (dddd.substring(1, 2).equals("u") || dddd.substring(1, 2).equals("d")) { // -u(x/2)
                dddd = "n(" + dddd.substring(1, dddd.length()) + ")"; // n(u(x/2))
            } else {
                dddd = "n" + dddd.substring(1, dddd.length()); // n30+3*x
            }
        } else if (dddd.substring(0, 1).equals("=")) { // lol nexon and their mistakes
            dddd = dddd.substring(1, dddd.length());
        }
        return (int) (new CaltechEval(dddd.replace("\\r\\n", "")).evaluate());
    }

    private final static int parseEval(String path, MapleData source, int def, String variables, int level) {
        if (variables == null) {
            return MapleDataTool.getIntConvert(path, source, def);
        } else {
            final MapleData dd = source.getChildByPath(path);
            if (dd == null) {
                return def;
            }
            if (dd.getType() != MapleDataType.STRING) {
                return MapleDataTool.getIntConvert(path, source, def);
            }
            String ddd = MapleDataTool.getString(dd).replace("y", "x").replace("X", "x");
            String dddd = ddd.replace(variables, String.valueOf(level));
            if (dddd.length() >= 3 && dddd.substring(0, 3).equals("log")) {
                dddd = dddd.replaceAll("\\(", "").replaceAll("\\)", "");
                double base = baseLog(Double.parseDouble(dddd.substring(5, level >= 10 ? 7 : 6)), Double.parseDouble(dddd.substring(3, 5)));
                dddd = String.valueOf(base) + dddd.substring(level >= 10 ? 7 : 6);
            } else if (dddd.substring(0, 1).equals("-")) { // -30+3*x
                if (dddd.substring(1, 2).equals("u") || dddd.substring(1, 2).equals("d")) { // -u(x/2)
                    dddd = "n(" + dddd.substring(1, dddd.length()) + ")"; // n(u(x/2))
                } else {
                    dddd = "n" + dddd.substring(1, dddd.length()); // n30+3*x
                }
            } else if (dddd.substring(0, 1).equals("=")) { // lol nexon and their mistakes
                dddd = dddd.substring(1, dddd.length());
            }

            if (dddd.equals("2*u") || dddd.equals("n2*u")) {
                dddd = "2*0";
            }

            return (int) (new CaltechEval(dddd.replace("\\r\\n", "")).evaluate());
        }
    }

    private static double baseLog(double x, double base) {
        return Math.log10(x) / Math.log10(base);
    }

    private static MapleStatEffect loadFromData(final MapleData source, final int sourceid, final boolean skill,
            final boolean overTime, final int level, final String variables) {
        final MapleStatEffect ret = new MapleStatEffect();
        try {
            ret.sourceid = sourceid;
            ret.skill = skill;
            ret.level = (byte) level;
            if (source == null) {
                return ret;
            }
            ret.duration = parseEval("time", source, -1, variables, level);
            ret.subTime = parseEval("subTime", source, -1, variables, level); // used for debuff time..
            ret.hp = (short) parseEval("hp", source, 0, variables, level);
            ret.hpFX = (short) parseEval("hpFX", source, 0, variables, level);
            ret.hcHp = (short) parseEval("hchp", source, 0, variables, level);
            ret.hpR = parseEval("hpR", source, 0, variables, level) / 100.0;
            ret.hpRCon = parseEval("hpRCon", source, 0, variables, level) / 100.0;
            ret.mp = (short) parseEval("mp", source, 0, variables, level);
            ret.mpR = parseEval("mpR", source, 0, variables, level) / 100.0;
            ret.ppRecovery = (short) parseEval("ppRecovery", source, 0, variables, level);
            ret.mhpR = (short) parseEval("mhpR", source, 0, variables, level);
            ret.mmpR = (short) parseEval("mmpR", source, 0, variables, level);
            ret.pddR = (short) parseEval("pddR", source, 0, variables, level);
            ret.mddR = (short) parseEval("mddR", source, 0, variables, level);
            ret.ignoreMobpdpR = (short) parseEval("ignoreMobpdpR", source, 0, variables, level);
            ret.ignoreMobDamR = (short) parseEval("ignoreMobDamR", source, 0, variables, level);
            ret.asrR = (short) parseEval("asrR", source, 0, variables, level);
            ret.terR = (short) parseEval("terR", source, 0, variables, level);
            ret.setBdR((short) parseEval("bdR", source, 0, variables, level));
            ret.damR = parseEval("damR", source, 0, variables, level);
            ret.mesoR = (short) parseEval("mesoR", source, 0, variables, level);
            ret.thaw = (short) parseEval("thaw", source, 0, variables, level);
            ret.padX = (short) parseEval("padX", source, 0, variables, level);
            ret.pddX = (short) parseEval("pddX", source, 0, variables, level);
            ret.mddX = (short) parseEval("mddX", source, 0, variables, level);
            ret.madX = (short) parseEval("madX", source, 0, variables, level);
            ret.dot = (short) parseEval("dot", source, 0, variables, level);
            ret.dotTime = (short) parseEval("dotTime", source, 0, variables, level);
            ret.dotInterval = (short) parseEval("dotInterval", source, 1, variables, level);
            ret.setDotSuperpos((short) parseEval("dotSuperpos", source, 0, variables, level));
            ret.criticaldamage = (short) parseEval("criticaldamage", source, 0, variables, level);
            ret.mpConReduce = (short) parseEval("mpConReduce", source, 0, variables, level);
            ret.soulmpCon = (short) parseEval("soulmpCon", source, 0, variables, level);
            ret.setForceCon((short) parseEval("forceCon", source, 0, variables, level));
            ret.mpCon = (short) parseEval("mpCon", source, 0, variables, level);
            ret.hpCon = (short) parseEval("hpCon", source, 0, variables, level);
            ret.comboConAran = (short) parseEval("comboConAran", source, 0, variables, level);
            ret.prop = (short) parseEval("prop", source, 100, variables, level);
            ret.subprop = (short) parseEval("subProp", source, 100, variables, level);
            ret.damAbsorbShieldR = (short) parseEval("damAbsorbShieldR", source, 0, variables, level);
            ret.cooltime = Math.max(0, parseEval("cooltime", source, 0, variables, level));
            ret.cooltimeMS = Math.max(0, parseEval("cooltimeMS", source, 0, variables, level));
            ret.coolTimeR = Math.max(0, parseEval("coolTimeR", source, 0, variables, level));
            ret.interval = parseEval("interval", source, 0, variables, level);
            ret.expinc = parseEval("expinc", source, 0, variables, level);
            ret.exp = parseEval("exp", source, 0, variables, level);
            ret.range = parseEval("range", source, 0, variables, level);
            ret.morphId = parseEval("morph", source, 0, variables, level);
            ret.cp = parseEval("cp", source, 0, variables, level);
            ret.cosmetic = parseEval("cosmetic", source, 0, variables, level);
            ret.er = (short) parseEval("er", source, 0, variables, level);
            ret.ppcon = parseEval("ppCon", source, 0, variables, level);
            ret.ppReq = (parseEval("ppReq", source, 0, variables, level));
            ret.ppRecovery = (short) parseEval("ppRecovery", source, 0, variables, level);
            ret.slotCount = (byte) parseEval("slotCount", source, 0, variables, level);
            ret.preventslip = (byte) parseEval("preventslip", source, 0, variables, level);
            ret.useLevel = (short) parseEval("useLevel", source, 0, variables, level);
            ret.nuffSkill = parseEval("nuffSkill", source, 0, variables, level);
            ret.mobCount = (byte) parseEval("mobCount", source, 1, variables, level);
            ret.immortal = (byte) parseEval("immortal", source, 0, variables, level);
            ret.iceGageCon = (byte) parseEval("iceGageCon", source, 0, variables, level);
            ret.expR = (byte) parseEval("expR", source, 0, variables, level);
            ret.expRPerM = parseEval("expRPerM", source, 0, variables, level) / 100.0;
            ret.reqGuildLevel = (byte) parseEval("reqGuildLevel", source, 0, variables, level);
            ret.period = (byte) parseEval("period", source, 0, variables, level);
            ret.type = (byte) parseEval("type", source, 0, variables, level);
            ret.bs = (byte) parseEval("bs", source, 0, variables, level);
            ret.mdf = (byte) parseEval("MDF", source, 0, variables, level);
            ret.attackCount = (byte) parseEval("attackCount", source, 1, variables, level);
            ret.bulletCount = (byte) parseEval("bulletCount", source, 1, variables, level);
            ret.speedMax = parseEval("speedMax", source, 0, variables, level);
            ret.accX = parseEval("accX", source, 0, variables, level);
            ret.setMhpX(parseEval("mhpX", source, 0, variables, level));
            ret.mmpX = parseEval("mmpX", source, 0, variables, level);
            int priceUnit = parseEval("priceUnit", source, 0, variables, level);
            ret.indieDamReduceR = (short) parseEval("indieDamReduceR", source, 0, variables, level);
            ret.lv2mhp = (short) parseEval("lv2mhp", source, 0, variables, level);
            ret.lv2mmp = (short) parseEval("lv2mmp", source, 0, variables, level);
            ret.lt = parsePoint("lt", source, new Point(0, 0), variables, level);
            ret.rb = parsePoint("rb", source, new Point(0, 0), variables, level);
            ret.setBufftimeR((short) parseEval("bufftimeR", source, 0, variables, level));
            ret.summonTimeR = ((short) parseEval("summonTimeR", source, 0, variables, level));
            ret.stanceProp = parseEval("stanceProp", source, 0, variables, level);
            ret.setKillRecoveryR((short) parseEval("killRecoveryR", source, 0, variables, level));
            ret.dotHealHPPerSecondR = ((short) parseEval("dotHealHPPerSecondR", source, 0, variables, level));
            ret.targetPlus = (short) parseEval("targetPlus", source, 0, variables, level);
            ret.targetPlus_5th = (short) parseEval("targetPlus_5th", source, 0, variables, level);
            ret.nocoolProp = (short) parseEval("nocoolProp", source, 0, variables, level);
            if (priceUnit > 0) {
                ret.price = parseEval("price", source, 0, variables, level) * priceUnit;
                ret.extendPrice = parseEval("extendPrice", source, 0, variables, level) * priceUnit;
            } else {
                ret.price = 0;
                ret.extendPrice = 0;
            }
            if (!ret.skill && ret.duration > -1) {

//                ret.overTime = true;
            } else {
                ret.duration *= 1000; // items have their times stored in ms, of course
                ret.subTime *= 1000;
            }

            ret.cooltime *= 1000; // ms
            ret.dotTime *= 1000; // ms
            ret.dotInterval *= 1000; // ms

            ret.mastery = (byte) parseEval("mastery", source, 0, variables, level);
            ret.pad = (short) parseEval("pad", source, 0, variables, level);
            ret.padR = (short) parseEval("padR", source, 0, variables, level);
            ret.setPdd((short) parseEval("pdd", source, 0, variables, level));
            ret.mad = (short) parseEval("mad", source, 0, variables, level);
            ret.madR = (short) parseEval("madR", source, 0, variables, level);
            ret.mdef = (short) parseEval("mdd", source, 0, variables, level);
            ret.emhp = (short) parseEval("emhp", source, 0, variables, level);
            ret.emmp = (short) parseEval("emmp", source, 0, variables, level);
            ret.epad = (short) parseEval("epad", source, 0, variables, level);
            ret.emad = (short) parseEval("emad", source, 0, variables, level);
            ret.epdd = (short) parseEval("epdd", source, 0, variables, level);
            ret.emdd = (short) parseEval("emdd", source, 0, variables, level);
            ret.acc = (short) parseEval("acc", source, 0, variables, level);
            ret.avoid = (short) parseEval("eva", source, 0, variables, level);
            ret.speed = (short) parseEval("speed", source, 0, variables, level);
            ret.jump = (short) parseEval("jump", source, 0, variables, level);
            ret.psdSpeed = (short) parseEval("psdSpeed", source, 0, variables, level);
            ret.psdJump = (short) parseEval("psdJump", source, 0, variables, level);
            ret.indieDEX = (short) parseEval("indieDEX", source, 0, variables, level);
            ret.indieCr = (short) parseEval("indieCr", source, 0, variables, level);
            ret.indiePad = (short) parseEval("indiePad", source, 0, variables, level);
            ret.indiePadR = (short) parseEval("indiePadR", source, 0, variables, level);
            ret.indieMad = (short) parseEval("indieMad", source, 0, variables, level);
            ret.indieMadR = (short) parseEval("indieMadR", source, 0, variables, level);
            ret.indiePMd = (short) parseEval("indiePMd", source, 0, variables, level);
            ret.indieMhp = (short) parseEval("indieMhp", source, 0, variables, level);
            ret.indieMmp = (short) parseEval("indieMmp", source, 0, variables, level);
            ret.indieBooster = (short) parseEval("indieBooster", source, 0, variables, level);
            ret.indieSpeed = (short) parseEval("indieSpeed", source, 0, variables, level);
            ret.indieJump = (short) parseEval("indieJump", source, 0, variables, level);
            ret.indieAcc = (short) parseEval("indieAcc", source, 0, variables, level);
            ret.indieEva = (short) parseEval("indieEva", source, 0, variables, level);
            ret.indieEvaR = (short) parseEval("indieEvaR", source, 0, variables, level);
            ret.indiePdd = (short) parseEval("indiePdd", source, 0, variables, level);
            ret.indieMdd = (short) parseEval("indieMdd", source, 0, variables, level);
            ret.indieDamR = (short) parseEval("indieDamR", source, 0, variables, level);
            ret.indieBDR = (short) parseEval("indieBDR", source, 0, variables, level);
            ret.indieCD = (short) parseEval("indieCD", source, 0, variables, level);
            ret.indieIgnoreMobpdpR = (short) parseEval("indieIgnoreMobpdpR", source, 0, variables, level);
            ret.indiePddR = (short) parseEval("indiePddR", source, 0, variables, level);
            ret.IndieExp = (short) parseEval("IndieExp", source, 0, variables, level);
            ret.indieStatRBasic = (short) parseEval("indieStatRBasic", source, 0, variables, level);
            ret.indieCooltimeReduce = (short) parseEval("indieCooltimeReduce", source, 0, variables, level);
            ret.indieAllStat = (short) parseEval("indieAllStat", source, 0, variables, level);
            ret.indieStance = (short) parseEval("indieStance", source, 0, variables, level);
            ret.setIndieMhpR((short) parseEval("indieMhpR", source, 0, variables, level));
            ret.indieMmpR = (short) parseEval("indieMmpR", source, 0, variables, level);
            ret.indieAsrR = (short) parseEval("indieAsrR", source, 0, variables, level);
            ret.indieTerR = (short) parseEval("indieTerR", source, 0, variables, level);
            ret.onActive = (short) parseEval("onActive", source, 0, variables, level);
            ret.str = (short) parseEval("str", source, 0, variables, level);
            ret.dex = (short) parseEval("dex", source, 0, variables, level);
            ret.int_ = (short) parseEval("int", source, 0, variables, level);
            ret.luk = (short) parseEval("luk", source, 0, variables, level);
            ret.strX = (short) parseEval("strX", source, 0, variables, level);
            ret.dexX = (short) parseEval("dexX", source, 0, variables, level);
            ret.intX = (short) parseEval("intX", source, 0, variables, level);
            ret.lukX = (short) parseEval("lukX", source, 0, variables, level);
            ret.expBuff = parseEval("expBuff", source, 0, variables, level);
            ret.cashup = parseEval("cashBuff", source, 0, variables, level);
            ret.itemup = parseEval("itemupbyitem", source, 0, variables, level);
            ret.mesoup = parseEval("mesoupbyitem", source, 0, variables, level);
            ret.berserk = parseEval("berserk", source, 0, variables, level);
            ret.berserk2 = parseEval("berserk2", source, 0, variables, level);
            ret.lifeId = (short) parseEval("lifeId", source, 0, variables, level);
            ret.inflation = (short) parseEval("inflation", source, 0, variables, level);
            ret.imhp = (short) parseEval("imhp", source, 0, variables, level);
            ret.immp = (short) parseEval("immp", source, 0, variables, level);
            ret.illusion = parseEval("illusion", source, 0, variables, level);
            ret.consumeOnPickup = parseEval("consumeOnPickup", source, 0, variables, level);
            ret.setIndiePmdR((short) parseEval("indiePMdR", source, 0, variables, level));
            ret.morph = (short) parseEval("morph", source, 0, variables, level);
            ret.kp = parseEval("kp", source, 0, variables, level);
            if (ret.consumeOnPickup == 1) {
                if (parseEval("party", source, 0, variables, level) > 0) {
                    ret.consumeOnPickup = 2;
                }
            }
            ret.charColor = 0;
            String cColor = MapleDataTool.getString("charColor", source, null);
            if (cColor != null) {
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(0, 2));
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(2, 4) + "00");
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(4, 6) + "0000");
                ret.charColor |= Integer.parseInt("0x" + cColor.substring(6, 8) + "000000");
            }
            ret.traits = new EnumMap<MapleTraitType, Integer>(MapleTraitType.class);
            for (MapleTraitType t : MapleTraitType.values()) {
                int expz = parseEval(t.name() + "EXP", source, 0, variables, level);
                if (expz != 0) {
                    ret.traits.put(t, expz);
                }
            }

            ret.recipe = parseEval("recipe", source, 0, variables, level);
            ret.recipeUseCount = (byte) parseEval("recipeUseCount", source, 0, variables, level);
            ret.recipeValidDay = (byte) parseEval("recipeValidDay", source, 0, variables, level);
            ret.reqSkillLevel = (byte) parseEval("reqSkillLevel", source, 0, variables, level);
            ret.powerCon = (byte) parseEval("powerCon", source, 0, variables, level);

            ret.effectedOnAlly = (byte) parseEval("effectedOnAlly", source, 0, variables, level);
            ret.effectedOnEnemy = (byte) parseEval("effectedOnEnemy", source, 0, variables, level);

            ret.petsCanConsume = new ArrayList<Integer>();
            for (int i = 0; true; i++) {
                final int dd = parseEval(String.valueOf(i), source, 0, variables, level);
                if (dd > 0) {
                    ret.petsCanConsume.add(dd);
                } else {
                    break;
                }
            }
            final MapleData mdd = source.getChildByPath("0");
            if (mdd != null && mdd.getChildren().size() > 0) {
                ret.mobSkill = (short) parseEval("mobSkill", mdd, 0, variables, level);
                ret.mobSkillLevel = (short) parseEval("level", mdd, 0, variables, level);
            } else {
                ret.mobSkill = 0;
                ret.mobSkillLevel = 0;
            }
            final MapleData pd = source.getChildByPath("randomPickup");
            if (pd != null) {
                ret.randomPickup = new ArrayList<Integer>();
                for (MapleData p : pd) {
                    ret.randomPickup.add(MapleDataTool.getInt(p));
                }
            }
            final MapleData ltd = source.getChildByPath("lt");
            if (ltd != null) {
                ret.setLt((Point) ltd.getData());
                ret.rb = (Point) source.getChildByPath("rb").getData();
            }

            final MapleData ltc = source.getChildByPath("con");
            if (ltc != null) {
                ret.availableMap = new ArrayList<Pair<Integer, Integer>>();
                for (MapleData ltb : ltc) {
                    ret.availableMap.add(new Pair<Integer, Integer>(MapleDataTool.getInt("sMap", ltb, 0),
                            MapleDataTool.getInt("eMap", ltb, 999999999)));
                }
            }

            ret.fatigueChange = 0;

            int totalprob = 0;
            final MapleData lta = source.getChildByPath("reward");
            if (lta != null) {
                ret.rewardMeso = parseEval("meso", lta, 0, variables, level);
                final MapleData ltz = lta.getChildByPath("case");
                if (ltz != null) {
                    ret.rewardItem = new ArrayList<Triple<Integer, Integer, Integer>>();
                    for (MapleData lty : ltz) {
                        ret.rewardItem.add(new Triple<Integer, Integer, Integer>(MapleDataTool.getInt("id", lty, 0),
                                MapleDataTool.getInt("count", lty, 0), MapleDataTool.getInt("prop", lty, 0)));
                        totalprob += MapleDataTool.getInt("prob", lty, 0);
                    }
                }
            } else {
                ret.rewardMeso = 0;
            }
            ret.totalprob = totalprob;
            ret.cr = parseEval("cr", source, 0, variables, level);
            ret.t = parseEval("t", source, 0, variables, level);
            ret.u = parseEval("u", source, 0, variables, level);
            ret.setU2(parseEval("u2", source, 0, variables, level));
            ret.v = parseEval("v", source, 0, variables, level);
            ret.v2 = parseEval("v2", source, 0, variables, level);
            ret.w = parseEval("w", source, 0, variables, level);
            ret.setW2(parseEval("w2", source, 0, variables, level));
            ret.x = parseEval("x", source, 0, variables, level);
            ret.y = parseEval("y", source, 0, variables, level);
            ret.z = parseEval("z", source, 0, variables, level);
            ret.s = parseEval("s", source, 0, variables, level);
            ret.setS2(parseEval("s2", source, 0, variables, level));
            ret.q = parseEval("q", source, 0, variables, level);
            ret.q2 = parseEval("q2", source, 0, variables, level);
            ret.damage = (short) parseEval("damage", source, 0, variables, level);
            ret.PVPdamage = (short) parseEval("PVPdamage", source, 0, variables, level);
            ret.incPVPdamage = (short) parseEval("incPVPDamage", source, 0, variables, level);
            ret.selfDestruction = (short) parseEval("selfDestruction", source, 0, variables, level);
            ret.bulletConsume = parseEval("bulletConsume", source, 0, variables, level);
            ret.moneyCon = parseEval("moneyCon", source, 0, variables, level);

            ret.itemCon = parseEval("itemCon", source, 0, variables, level);
            ret.itemConNo = parseEval("itemConNo", source, 0, variables, level);
            ret.moveTo = parseEval("moveTo", source, -1, variables, level);
//        ret.monsterStatus = new EnumMap<MonsterStatus, Integer>(MonsterStatus.class);
            if (ret.skill) { // hack because we can't get from the datafile...
                switch (sourceid) {
                    case 1101004:
                    case 1201004:
                    case 1301004:
                    case 2101008:
                    case 2201010:
                    case 2301008:
                    case 3101002:
                    case 3201002:
                    case 3301010:
                    case 4101003:
                    case 4201002:
                    case 4301002:
                    case 4311009:
                    case 5101006:
                    case 5201003:
                    case 5301002:
                    case 11101024:
                    case 12101004:
                    case 13101023:
                    case 14101022:
                    case 15101002:
                    case 15101022:
                    case 22111020:
                    case 23101002:
                    case 24101005:
                    case 27101004:
                    case 31001001:
                    case 31201002:
                    case 32101005:
                    case 33101012:
                    case 35101006:
                    case 36101004:
                    case 37101003:
                    case 51101003:
                    case 64101003:
                    case 151101005:
                    case 152101007:
                    case 155101005:
                    case 164101005:
                    case 63101010:
                    case 162101013:
                        ret.statups.put(MapleBuffStat.Booster, new Pair<>(ret.x, ret.duration));
                        break;
                    case 261:
                    case 262:
                    case 263:
                    case 80002770:
                    case 80002771:
                    case 80002772:
                    case 80002773:
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 1121000:
                    case 1221000:
                    case 1321000:
                    case 2121000:
                    case 2221000:
                    case 2321000:
                    case 3121000:
                    case 3221000:
                    case 3321023:
                    case 4121000:
                    case 4221000:
                    case 4341000:
                    case 5121000:
                    case 5221000:
                    case 5321005:
                    case 11121000:
                    case 12121000:
                    case 13121000:
                    case 14121000:
                    case 15121000:
                    case 21121000:
                    case 22171068:
                    case 23121005:
                    case 24121008:
                    case 25121108:
                    case 27121009:
                    case 31121004:
                    case 31221008:
                    case 32121007:
                    case 33121007:
                    case 35121007:
                    case 36121008:
                    case 37121006:
                    case 51121005:
                    case 61121014:
                    case 63121009:
                    case 64121004:
                    case 65121009:
                    case 100001268:
                    case 142121016:
                    case 151121005:
                    case 152121009:
                    case 155121008:
                    case 164121009:
                    case 162121023:
                        ret.statups.put(MapleBuffStat.BasicStatUp, new Pair<>(ret.x, ret.duration));
                        break;

                    case 1121053:
                    case 1221053:
                    case 1321053:
                    case 2121053:
                    case 2221053:
                    case 2321053:
                    case 3121053:
                    case 3221053:
                    case 3321041:
                    case 4121053:
                    case 4221053:
                    case 4341053:
                    case 5121053:
                    case 5221053:
                    case 5321053:
                    case 11121053:
                    case 12121053:
                    case 13121053:
                    case 14121053:
                    case 15121053:
                    case 21121053:
                    case 22171082:
                    case 23121053:
                    case 24121053:
                    case 25121132:
                    case 27121053:
                    case 31121053:
                    case 31221053:
                    case 32121053:
                    case 33121053:
                    case 35121053:
                    case 37121053:
                    case 51121053:
                    case 151121042:
                    case 152121042:
                    case 155121042:
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;

                    case 1005:
                    case 10001005:
                    case 10001215:
                    case 20001005:
                    case 20011005:
                    case 20021005:
                    case 20031005:
                    case 20041005:
                    case 20051005:
                    case 30001005:
                    case 30011005:
                    case 30021005:
                    case 50001005:
                    case 50001215:
                    case 60001005:
                    case 60011005:
                    case 60021005:
                    case 100001005:
                    case 140001005:
                    case 150001005:
                    case 150011005:
                        ret.statups.put(MapleBuffStat.MaxLevelBuff, new Pair<>(ret.x, ret.duration));
                        break;

                    // ?????????
                    case 1101006: // ??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.PowerGaurd, new Pair<>(ret.x, ret.duration));
                        break;
                    case 1101013: // ?????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ComboCounter, new Pair<>(1, ret.duration));
                        break;
                    case 1121010: // ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.Enrage, new Pair<>(ret.x, ret.duration)); // + ret.mobCount
                        ret.statups.put(MapleBuffStat.EnrageCr, new Pair<>(0, ret.duration));
                        ret.statups.put(MapleBuffStat.EnrageCrDamMin, new Pair<>(ret.y, ret.duration));
                        break;
                    case 1121054: // ?????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieCr, new Pair<>((int) ret.indieCr, ret.duration));
                        ret.statups.put(MapleBuffStat.Stance, new Pair<>(100, ret.duration));
                        ret.statups.put(MapleBuffStat.Asr, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.Ter, new Pair<>(ret.y, ret.duration));
                        break;

                    // ?????????
                    case 1200014:
                    case 1220010: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.ElementalCharge, new Pair<>(1, ret.duration));
                        break;
                    case 1210016: //????????? ??????
                        ret.statups.put(MapleBuffStat.BlessingArmor, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.BlessingArmorIncPad, new Pair<>((int) ret.epad, ret.duration));
                        break;
                    case 1211010: // ??????????????????
                        ret.statups.put(MapleBuffStat.Listonation, new Pair<>(ret.x, ret.duration));
                        break;
                    case 1211011: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.CombatOrders, new Pair<>(ret.x, ret.duration));
                        break;
                    case 1221015: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        break;
                    case 1221016: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(ret.duration, ret.duration));
                        break;
                    case 1221054: // ??????????????????
                        ret.statups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(1, ret.duration));
                        break;

                    // ???????????????
                    case 1301006: // ????????? ???
                        ret.statups.put(MapleBuffStat.Pdd, new Pair<>((int) ret.pdd, ret.duration));
                        break;
                    case 1301007: // ???????????????
                        ret.statups.put(MapleBuffStat.MaxHP, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.MaxMP, new Pair<>(ret.x, ret.duration));
                        break;
                    case 1301013: // ?????????
                        ret.statups.put(MapleBuffStat.Beholder, new Pair<>(1, ret.duration));
                        break;
                    case 1311015: // ????????? ?????? ??????
                        ret.statups.put(MapleBuffStat.CrossOverChain, new Pair<>(ret.x, ret.duration));
                        break;
                    case 1321015: // ??????????????????
                        ret.hpR = ret.y / 100.0;
                        ret.statups.put(MapleBuffStat.IgnoreTargetDEF, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        break;
                    case 1320019: // ??????????????????
                        ret.statups.put(MapleBuffStat.Reincarnation, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(1, ret.duration));
                        break;
                    case 1321054: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.AuraRecovery, new Pair<>(ret.x, ret.duration));
                        break;
                    // ?????????
                    case 2001002: // ?????? ??????
                    case 12001001:
                    case 22001012:
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.MagicGaurd, new Pair<>(ret.x, ret.duration));
                        break;
                    case 2111008: // ???????????? ??????
                    case 2211008:
                    case 12101005:
                    case 22141016:
                        ret.statups.put(MapleBuffStat.ElementalReset, new Pair<>(ret.x, ret.duration));
                        break;
                    case 2111007: // ???????????? ????????????
                    case 2211007:
                    case 2311007:
                    case 32111010:
                    case 22161005: // ???????????? ????????????
                        ret.mpCon = (short) ret.y;
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.TeleportMastery, new Pair<>(ret.x, ret.duration));
                        break;
                    // ??????
                    case 2101001: // ???????????????
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        break;
                    case 2101010: // ???????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.WizardIgnite, new Pair<>(1, 0));
                        break;
                    case 2121054: // ????????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.FireAura, new Pair<>(1, ret.duration));
                        break;
                    // ??????
                    case 2201001: // ???????????????
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        break;
                    case 2221011: // ????????? ?????????
                        ret.duration = 1000;
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(1, ret.duration));
                        break;
                    case 2221045: // ???????????? ???????????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.TeleportMasteryRange, new Pair<>(ret.x, ret.duration));
                        break;
                    // ??????
                    case 2321006: // ????????????
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(ret.duration, ret.duration));
                        break;
                    case 2321054: // ????????? ?????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        ret.statups.put(MapleBuffStat.IgnoreTargetDEF, new Pair<>(0, ret.duration));
                        ret.statups.put(MapleBuffStat.VengeanceOfAngel, new Pair<>(1, ret.duration));
                        break;
                    // ??????
                    case 13121005: // ?????? ?????????
                    case 33121004:
                    case 400001002:
                        ret.statups.put(MapleBuffStat.SharpEyes, new Pair<>((ret.x << 8) + ret.y, ret.duration));
                        break;
                    // ???????????????
                    case 3101004: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.SoulArrow, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedPad, new Pair<>((int) ret.epad, ret.duration));
                        ret.statups.put(MapleBuffStat.Concentration, new Pair<>(1, ret.duration)); // sniff
                        break;
                    case 3111011: // ???????????? ????????? : ???
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ExtremeArchery, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        break;
                    case 3121016: // ??????????????? ??????
                        ret.statups.put(MapleBuffStat.AdvancedQuiver, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 3121007: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDex, new Pair<>((int) ret.indieDEX, ret.duration));
                        ret.statups.put(MapleBuffStat.Eva, new Pair<>(ret.x, ret.duration));
                        break;
                    case 3121054: // ??????????????????
                        ret.statups.put(MapleBuffStat.Stance, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>(ret.y, ret.duration));
                        break;

                    // ??????
                    case 3201004: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.SoulArrow, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.Concentration, new Pair<>(1, ret.duration)); // Sniff
                        ret.statups.put(MapleBuffStat.EnhancedPad, new Pair<>((int) ret.epad, ret.duration));
                        break;
                    case 3211011: // ????????????
                        ret.statups.put(MapleBuffStat.Asr, new Pair<>((int) ret.asrR, ret.duration));
                        break;
                    case 3211012: // ???????????? ????????? : ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ExtremeArchery, new Pair<>(ret.z, ret.duration));
                        break;
                    case 3221006: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDex, new Pair<>((int) ret.indieDEX, ret.duration));
                        ret.statups.put(MapleBuffStat.Eva, new Pair<>((int) ret.x, ret.duration));
                        break;
                    case 3321036: // ???????????? ????????????(????????????)
                    case 3321038: // ???????????? ????????????(????????????)
                        ret.statups.put(MapleBuffStat.IndieDamageReduce, new Pair<>((int) ret.indieDamReduceR, ret.duration));
                        break;
                    case 3221054: // ????????????
                        ret.statups.put(MapleBuffStat.IgnoreTargetDEF, new Pair<>(0, ret.duration));
                        ret.statups.put(MapleBuffStat.BullsEye, new Pair<>((ret.x << 8) + ret.y, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 400031055:
                        ret.statups.put(MapleBuffStat.RepeatinCartrige, new Pair<>((int) ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.ignoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieCr, new Pair<>((int) ret.cr, ret.duration));
                        break;
                    // ???????????????
                    case 3310006: // ???????????? ????????????
                        ret.statups.put(MapleBuffStat.AncientGuidance, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        break;
                    case 3311012: // ?????? ????????????
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>(ret.s, ret.duration));
                        break;
                    case 3321040:
                        ret.duration = ret.u * 1000;
                        ret.statups.put(MapleBuffStat.IndieKeyDownMoving, new Pair<>(0, ret.duration));
                        ret.statups.put(MapleBuffStat.Stance, new Pair<>(100, ret.duration));
                        break;
                    // ??????
                    case 14001023:
                        ret.statups.put(MapleBuffStat.DarkSight, new Pair<>(ret.x, ret.duration));
                        break;
                    case 4001005: // ????????????
                    case 14001022:
                        ret.statups.put(MapleBuffStat.Speed, new Pair<>((int) ret.speed, ret.duration));
                        ret.statups.put(MapleBuffStat.Jump, new Pair<>((int) ret.jump, ret.duration));
                        break;
                    case 4301003:
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>((int) ret.indieSpeed, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieJump, new Pair<>((int) ret.indieJump, ret.duration));
                        break;
                    case 4111002: // ????????? ?????????
                    case 4211008: //
                    case 4331002: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.ShadowPartner, new Pair<>(ret.x, ret.duration));
                        break;
                    // ???????????????
                    case 4101011: // ?????? ?????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.MarkofNightLord, new Pair<>(1, ret.duration));
                        break;
                    case 4121054: // ????????? ??????
                        ret.statups.put(MapleBuffStat.BleedingToxin, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        break;
                    case 400041061:
                        ret.statups.put(MapleBuffStat.ThrowBlasting, new Pair<>((int) ret.x, ret.duration));
                        break;
                    // ?????????
                    case 4201011: // ?????? ??????
                        ret.statups.put(MapleBuffStat.MesoGuard, new Pair<>(ret.x, ret.duration));
                        break;
                    case 4211003: // ??? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.PickPocket, new Pair<>(1, ret.duration));
                        break;
                    // ??????????????????
                    case 4341052: // ?????????
                        ret.statups.put(MapleBuffStat.Asura, new Pair<>(100, ret.duration));
                        break;
                    case 4341054: // ?????? ????????????
                        ret.statups.put(MapleBuffStat.WindBreakerFinal, new Pair<>(1, ret.duration)); // 1
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    // ??????
                    case 5001005: // Dash
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.DashSpeed, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.DashJump, new Pair<>(ret.y, ret.duration));
                        break;
                    case 5120011:
                    case 5220012: // ????????? ??????
                        ret.statups.put(MapleBuffStat.DamageReduce, new Pair<>(ret.y, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration)); // i think
                        break;

                    // ?????????
                    case 5100015: // ????????? ??????
                    case 5110014:
                    case 5120018:
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.EnergyCharged, new Pair<>(1, ret.duration));
                        break;
                    case 5121009: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.PartyBooster, new Pair<>(-2, ret.duration));
                        break;
                    case 5121015: // ???????????????
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        break;

                    // ??????
                    case 5221015: // ???????????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.GuidedBullet, new Pair<>(1, 0));
                        break;
                    case 5221018: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.Eva, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) ret.indieAsrR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieTerR, new Pair<>((int) ret.indieTerR, ret.duration));
                        break;
                    case 5221054: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) ret.indieAsrR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieTerR, new Pair<>((int) ret.indieTerR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieCr, new Pair<>((int) ret.indieCr, ret.duration));
                        ret.statups.put(MapleBuffStat.DamageReduce, new Pair<>(ret.w, ret.duration));
                        break;
                    // ???????????????
                    case 5301003: // ????????????
                    case 5320008: // ????????? ????????????
                        ret.statups.put(MapleBuffStat.IndieHp, new Pair<>((int) ret.indieMhp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMp, new Pair<>((int) ret.indieMmp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieJump, new Pair<>((int) ret.indieJump, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>((int) ret.indieSpeed, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAllStat, new Pair<>((int) ret.indieAllStat, ret.duration));
                        break;
                    case 5321010: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.Stance, new Pair<>((int) ret.x, ret.duration));
                        break;
                    case 5321054: // ??? ???
                        ret.statups.put(MapleBuffStat.Buckshot, new Pair<>(ret.x, ret.duration)); // 3
                        //ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>(-ret.y, ret.duration));
                        break;
                    //?????????
                    case 9001004: // ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.IndieEva, new Pair<>(1, ret.duration));
                        break;
                    // ???????????????
                    case 11001022: // ???????????? : ??????
                        ret.statups.put(MapleBuffStat.CygnusElementSkill, new Pair<>(1, ret.duration));
                        break;
                    case 11121054: // ?????? ??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        break;
                    // ??????????????????
                    case 12101022: // ??? ??? ?????????
                        ret.mpR = ret.x / 100.0;
                        break;
                    case 12101023: // ??? ?????? ?????????
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>((int) ret.indieBooster, ret.duration));
                        break;
                    case 12101024: // ????????????
                        ret.statups.put(MapleBuffStat.Ember, new Pair<>(1, ret.duration));
                        break;
                    case 12121003: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.DamageReduce, new Pair<>(ret.x, ret.duration));
                        break;
                    case 12121043: // ?????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.AddRange, new Pair<>(100, ret.duration));
                        break;
                    // ??????????????????
                    case 13001022: // ???????????? : ??????
                        ret.statups.put(MapleBuffStat.CygnusElementSkill, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 13101024: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.SoulArrow, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.CriticalIncrease, new Pair<>(ret.x, ret.duration));
                        break;
                    case 13110026: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieEvaR, new Pair<>((int) ret.er, ret.duration));
                        ret.statups.put(MapleBuffStat.Pdd, new Pair<>((int) ret.pddX, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        break;
                    case 13111023: // ???????????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieHp, new Pair<>((int) ret.indieMhp, ret.duration));
                        ret.statups.put(MapleBuffStat.Albatross, new Pair<>((int) ret.level, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieCr, new Pair<>((int) ret.indieCr, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>((int) ret.indieBooster, ret.duration));
                        break;
                    case 13120008: // ??????????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.ignoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieCr, new Pair<>((int) ret.indieCr, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) ret.indieAsrR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieTerR, new Pair<>((int) ret.indieTerR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>((int) ret.indieBooster, ret.duration));
                        ret.statups.put(MapleBuffStat.Albatross, new Pair<>((int) ret.level, ret.duration));
                        break;

                    case 13121004: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.IndieEvaR, new Pair<>((int) ret.prop, ret.duration));
                        break;
                    case 13121054: // ???????????????
                        ret.statups.put(MapleBuffStat.StormBringer, new Pair<>(1, ret.duration));
                        break;
                    // ???????????????
                    case 14001021: // ???????????? : ????????????
                        ret.statups.put(MapleBuffStat.ElementDarkness, new Pair<>(1, ret.duration));
                        break;
                    case 14001027: // ????????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ShadowBatt, new Pair<>(1, ret.duration));
                        break;
                    case 14111024: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.ShadowServant, new Pair<>(ret.x, ret.duration));
                        break;
                    case 14121054: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.ShadowIllusion, new Pair<>(ret.x, ret.duration));
                        break;
                    // ???????????????
                    case 15121004: // ??????
                        ret.statups.put(MapleBuffStat.ShadowPartner, new Pair<>(ret.x, ret.duration));
                        break;
                    case 15121005: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.PartyBooster, new Pair<>(-2, ret.duration));
                        break;
                    case 15121054: // ?????? ??????
                        ret.statups.put(MapleBuffStat.StrikerHyperElectric, new Pair<>(ret.x, ret.duration));
                        break;
                    // ??????
                    case 21001003: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.Booster, new Pair<>(-ret.y, ret.duration));
                        break;
                    case 21001008: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.BodyPressure, new Pair<>((int) ret.damage, ret.duration));
                        break;
                    case 21101005: // ?????????
                        ret.statups.put(MapleBuffStat.AranDrain, new Pair<>(ret.x, ret.duration));
                        break;
                    case 21101006: // ????????? ??????
                        ret.statups.put(MapleBuffStat.SnowCharge, new Pair<>(ret.w, ret.duration));
                        break;
                    case 21111012: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        break;
                    case 21120022: // ????????? 1???
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.BeyondNextAttackProb, new Pair<>(1, ret.duration));
                        break;
                    case 21121016: // ????????? 2????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.BeyondNextAttackProb, new Pair<>(2, ret.duration));
                        break;
                    case 21120026: // ????????? ?????????
                        ret.duration = 10000;
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(1, ret.duration));
                        break;
                    // ??????
                    case 22140013: // ????????? - ?????????!
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, ret.duration));
                        break;
                    case 22171073: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.EnhancedMad, new Pair<>((int) ret.emad, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedPdd, new Pair<>((int) ret.epdd, ret.duration));
                        break;
                    // ???????????????
                    case 23121004: // ???????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedMaxHp, new Pair<>((int) ret.emhp, ret.duration));
                        break;
                    case 23121054: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.Stance, new Pair<>(ret.x, ret.duration));
                        break;
                    // ??????
                    case 24121004: // ????????? ?????? ?????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        break;
                    // ????????????
                    case 20040216: // ????????????
                    case 20040217: // ????????????
                    case 20040219: // ??????????????????
                    case 20040220: // ??????????????????
                        ret.duration = 0;
                        break;
                    case 27101202: // ????????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.KeyDownAreaMoving, new Pair<>(16, ret.duration));
                        break;
                    case 27111005: // ?????????????????? ??????
                        ret.statups.put(MapleBuffStat.IndiePdd, new Pair<>((int) ret.indiePdd, ret.duration));
                        break;
                    case 27111006: // ?????? ???????????????
                        ret.statups.put(MapleBuffStat.EnhancedMad, new Pair<>((int) ret.emad, ret.duration));
                        break;
                    case 27121006: // ???????????? ?????????
                        ret.statups.put(MapleBuffStat.ElementalReset, new Pair<>(ret.y, ret.duration));
                        break;
                    // ??????
                    case 20050286: // ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.PreReviveOnce, new Pair<>(1, ret.duration));
                        break;
                    case 25101009: // ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.HiddenPossession, new Pair<>(1, ret.duration));
                        break;
                    case 25121030:
                        ret.statups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 0));
                        ret.statups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 0));
                        break;
                    case 25121131:
                    case 25121133: // ?????? ?????? ?????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, ret.duration));
                        break;
                    // ??????????????????
                    case 31101003: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.PowerGaurd, new Pair<>(ret.y, ret.duration));
                        break;
                    case 31120045: // ?????? ????????? : ????????? ?????? ????????????
                        ret.statups.put(MapleBuffStat.NextAttackEnhance, new Pair<>(ret.x, ret.duration));
                        break;
                    case 31121002: // ????????? ??????
                        ret.statups.put(MapleBuffStat.DrainHp, new Pair<>(3, ret.duration));
                        break;
                    case 31121007: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.InfinityForce, new Pair<>(1, ret.duration));
                        break;
                    case 31121054: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.ShadowPartner, new Pair<>(ret.x, ret.duration));
                        break;
                    // ???????????????
                    case 31011001: // ????????? ????????????
                        ret.hpR = ret.x / 100.0;
                        break;
                    case 31211004: // ???????????? ????????????
                        ret.statups.put(MapleBuffStat.IndieHpR, new Pair<>((int) ret.getIndieMhpR(), ret.duration));
                        ret.statups.put(MapleBuffStat.DiabloicRecovery, new Pair<>(ret.x, ret.duration));
                        break;
                    case 31221054: // ????????? ????????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    // ???????????????
                    case 32001014:
                    case 32100010:
                    case 32110017:
                    case 32120019:
                        ret.duration = 0;
                        break;
                    case 32120044: // ???????????? ???????????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.TeleportMasteryRange, new Pair<>(ret.x, ret.duration));
                        break;
                    case 32121010: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.Enrage, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.EnrageCr, new Pair<>(ret.z, ret.duration));
                        ret.statups.put(MapleBuffStat.EnrageCrDamMin, new Pair<>(ret.y, ret.duration));
                        break;
                    case 32121056: // ????????? ?????? ??????
                        ret.statups.put(MapleBuffStat.AttackCountX, new Pair<>((int) ret.attackCount, ret.duration));
                        break;
                    // ???????????????
                    case 33001007: // ?????? ?????????
                    case 33001008: // ?????? ?????????
                    case 33001009: // ?????? ?????????
                    case 33001010: // ?????? ?????????
                    case 33001011: // ?????? ?????????
                    case 33001012: // ?????? ?????????
                    case 33001013: // ?????? ?????????
                    case 33001014: // ?????? ?????????
                    case 33001015: // ?????? ?????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.JaguarSummoned, new Pair<>((ret.asrR << 8) + ret.criticaldamage, ret.duration));
                        break;
                    case 33101003: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.SoulArrow, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        break;
                    case 33101005: // ?????????
                        //    ret.statups.put(MapleBuffStat.HowlingDefence, new Pair<>(ret.x, ret.duration));
                        //     ret.statups.put(MapleBuffStat.HowlingEvasion, new Pair<>(ret.x, ret.duration));
                        //     ret.statups.put(MapleBuffStat.HowlingMaxMp, new Pair<>(ret.x, ret.duration));
                        //     ret.statups.put(MapleBuffStat.HowlingCritical, new Pair<>(ret.y, ret.duration)); // ?????????
                        ret.statups.put(MapleBuffStat.HowlingParty, new Pair<>(ret.z, ret.duration));
                        break;
                    case 33111011: // ????????? ???
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.DrawBack, new Pair<>(0, ret.duration));
                        break;
                    case 33121054: // ???????????? ?????????
                        ret.statups.put(MapleBuffStat.FinalAttackProp, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    // ?????????
                    case 35001002: // ???????????? : ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.EnhancedMaxHp, new Pair<>((int) ret.emhp, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedMaxMp, new Pair<>((int) ret.emmp, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedPad, new Pair<>((int) ret.epad, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedPdd, new Pair<>((int) ret.epdd, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>(20, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, ret.duration));
                        ret.statups.put(MapleBuffStat.Mechanic, new Pair<>(30, ret.duration)); // parseMountInfo
                        break;
                    case 35101007: // ????????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.PerfectArmor, new Pair<>(ret.x, ret.duration));
                        break;
                    case 35111003: // ???????????? : ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.EnhancedMaxHp, new Pair<>((int) ret.emhp, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedMaxMp, new Pair<>((int) ret.emmp, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedPad, new Pair<>((int) ret.epad, ret.duration));
                        ret.statups.put(MapleBuffStat.EnhancedPdd, new Pair<>((int) ret.epdd, ret.duration));
                        ret.statups.put(MapleBuffStat.CriticalIncrease, new Pair<>(ret.cr, ret.duration));
                        ret.statups.put(MapleBuffStat.Mechanic, new Pair<>(30, ret.duration)); // parseMountInfo
                        break;
                    case 35121055: // ?????? ??????
                        ret.statups.put(MapleBuffStat.BombTime, new Pair<>(ret.x, ret.duration));
                        break;
                    // ??????
                    case 36001002: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        break;
                    case 36001005: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.PinPointRocket, new Pair<>(1, ret.duration));
                        break;
                    case 36101003: // ???????????? ???????????????
                        ret.statups.put(MapleBuffStat.IndieHpR, new Pair<>((int) ret.getIndieMhpR(), ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMpR, new Pair<>((int) ret.getIndieMhpR(), ret.duration));
                        break;
                    case 36111004: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.AegisSystem, new Pair<>(1, ret.duration));
                        break;
                    case 36111006: // ????????? ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ShadowPartner, new Pair<>((int) ret.x, ret.duration)); // x??? ?????? ???????????? ?????? ????????????..
                        break;
                    case 36121003: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        break;
                    case 36121007: // ?????? ??????
                        ret.statups.put(MapleBuffStat.OnCapsule, new Pair<>(20, ret.duration));
                        break;
                    case 400041057:
                        ret.statups.put(MapleBuffStat.PhotonRay, new Pair<>(1, ret.duration));
                        break;
                    // ????????????
                    case 37121054: // ??????????????? ??????
                        ret.statups.put(MapleBuffStat.RWMaximizeCannon, new Pair<>(ret.x, ret.duration));
                        break;
                    // ?????????
                    case 51101004: // ??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        break;
                    case 51120003: // ??????????????? ?????? ??????
                        ret.statups.put(MapleBuffStat.DamageDecreaseWithHP, new Pair<>(ret.y, ret.duration));
                        break;
                    case 51121006: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.Enrage, new Pair<>(ret.x, ret.duration)); // + ret.mobCount
                        ret.statups.put(MapleBuffStat.EnrageCr, new Pair<>(ret.z, ret.duration));
                        ret.statups.put(MapleBuffStat.EnrageCrDamMin, new Pair<>(ret.y, ret.duration));
                        break;
                    case 51121054: // ??????????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieHpR, new Pair<>((int) ret.getIndieMhpR(), ret.duration));
                        break;
                    // ?????????
                    case 60001216: // ????????????
                    case 60001217: // ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ReshuffleSwitch, new Pair<>(0, ret.duration));
                        break;
                    case 61101004: // ???????????? ???
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.Booster, new Pair<>(-2, ret.duration));
                        break;
                    case 61101002: // ??? ?????? ??????
                    case 61110211: // ??? ?????? ?????? (????????????????????????)
                    case 61120007: // ??????????????? ??? ?????? ??????
                    case 61121217: // ??????????????? ??? ?????? ?????? (????????????????????????)
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.StopForceAtominfo, new Pair<>(ret.cooltime / 1000, ret.duration));
                        break;
                    case 61111003: // ????????? ????????????
                        ret.statups.put(MapleBuffStat.Asr, new Pair<>((int) ret.asrR, ret.duration));
                        ret.statups.put(MapleBuffStat.Ter, new Pair<>((int) ret.terR, ret.duration));
                        break;
                    case 61111008: // ????????? ???????????????
                    case 61120008: // ????????? ???????????????
                    case 61121053: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        ret.statups.put(MapleBuffStat.CriticalIncrease, new Pair<>(ret.cr, ret.duration));
                        ret.statups.put(MapleBuffStat.Stance, new Pair<>(100, ret.duration));
                        ret.statups.put(MapleBuffStat.Speed, new Pair<>((int) ret.speed, ret.duration));
                        ret.statups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IgnorePImmune, new Pair<>(1, ret.duration));
                        break;
                    // ?????????
                    case 64001007:
                    case 64001008:
                    case 64001009:
                    case 64001010:
                    case 64001011:
                    case 64001012: // ????????????: ?????????
                        ret.duration = 2000;
                        ret.statups.put(MapleBuffStat.DarkSight, new Pair<>(10, ret.duration));
                        break;
                    case 64121053: // ??????????????? ????????????
                        ret.statups.put(MapleBuffStat.BonusAttack, new Pair<>(ret.x, ret.duration));
                        break;
                    case 64121054: // ????????? ?????? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieCr, new Pair<>((int) ret.indieCr, ret.duration));
                        break;
                    // ??????????????????
                    case 60011219: // ?????? ????????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 65001002: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, ret.duration));
                        break;
                    case 65121003: // ?????? ????????????
                        ret.duration = 8000;
                        ret.statups.put(MapleBuffStat.SoulResonance, new Pair<>(ret.y, ret.duration));
                        break;
                    case 65121004: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.SoulGazeCriDamR, new Pair<>(ret.x, ret.duration));
                        break;
                    case 65121011: // ???????????? ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.SoulSeekerExpert, new Pair<>(ret.x, ret.duration));
                        break;
                    case 65121053: // ????????? ????????????
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.CriticalIncrease, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.Asr, new Pair<>((int) ret.asrR, ret.duration));
                        ret.statups.put(MapleBuffStat.Ter, new Pair<>((int) ret.terR, ret.duration));
                        break;
                    case 65121054: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        ret.statups.put(MapleBuffStat.SoulExalt, new Pair<>(ret.x, ret.duration));
                        break;
                    // ????????????
                    case 80000169: // ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.PreReviveOnce, new Pair<>(1, ret.duration));
                        break;
                    case 80001140: // ?????? ??????
                    case 50001214: // ?????? ??????
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>(100, ret.duration));
                        break;
                    case 80001155: // ?????? ????????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    //??????
                    case 80001242: // ?????????
                        ret.duration = Integer.MAX_VALUE;
                        ret.statups.put(MapleBuffStat.NewFlying, new Pair<>(1, ret.duration));
                        break;
                    case 80001427: // ????????? ??? ??????
                        ret.statups.put(MapleBuffStat.IndieJump, new Pair<>(130, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>(150, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-3, ret.duration));
                        break;
                    case 80001432: // ????????? ??? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>(50, ret.duration));
                        break;
                    case 80001455: // ?????????????????? ???
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMadR, new Pair<>((int) ret.indieMadR, ret.duration));
                        break;
                    case 80001456: // ??????????????? ???
                        ret.statups.put(MapleBuffStat.SetBaseDamageByBuff, new Pair<>(2000000, ret.duration));
                        break;
                    case 80001457: // ?????? ???
                        ret.statups.put(MapleBuffStat.LimitMP, new Pair<>(500, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        break;
                    case 80001458: // ????????? ???
                        ret.statups.put(MapleBuffStat.MHPCutR, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        break;
                    case 80001459: // ????????? ???
                        ret.statups.put(MapleBuffStat.MMPCutR, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        break;
                    case 80001460: // ??????????????? ???
                        ret.statups.put(MapleBuffStat.IndieHpR, new Pair<>((int) ret.indieMhpR, ret.duration));
                        break;
                    case 80001461: // ??????????????? ???
                        ret.statups.put(MapleBuffStat.IndieCD, new Pair<>(ret.x, ret.duration));
                        break;
                    case 80001757: // ????????? ??? ??????
                        ret.statups.put(MapleBuffStat.IndieJump, new Pair<>(100, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>(100, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.Inflation, new Pair<>(500, ret.duration));
                        break;
                    case 80001762: // ????????? ??? ??????
                        ret.statups.put(MapleBuffStat.RandAreaAttack, new Pair<>(3, ret.duration));
                        break;
                    case 80001752:
                        ret.statups.put(MapleBuffStat.RandAreaAttack, new Pair<>(3, ret.duration));
                        break;
                    case 80001878: // ????????? ??? ??????
                        ret.statups.put(MapleBuffStat.FixCooltime, new Pair<>(5, ret.duration));
                        break;
                    case 80002404: // ??? 2????????? ?????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.DebuffIncHp, new Pair<>(50, ret.duration));
                        break;
                    case 80002280: // ????????? ?????? ???
                        ret.duration = 3 * 60 * 1000;
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>(100, ret.duration));
                        break;
                    case 80002281: // ????????? ??? ??????
                        ret.statups.put(MapleBuffStat.RuneOfGreed, new Pair<>(100, ret.duration));
                        break;
                    case 80002282: // ????????? ?????? ???
                        ret.duration = 15 * 60 * 1000;
                        ret.statups.put(MapleBuffStat.CooldownRune, new Pair<>((int) 1, ret.duration));
                        break;
                    case 80002888:
                        ret.statups.put(MapleBuffStat.RuneOfPure, new Pair<>(100, ret.duration));
                        break;
                    case 80002544:
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        break;
                    case 91001022:
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        break;
                    case 91001023:
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        break;
                    case 91001024:
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 91001025:
                        ret.statups.put(MapleBuffStat.IndieCD, new Pair<>((int) ret.indieCD, ret.duration));
                        break;
                    // ?????????
                    case 131001000:
                    case 131001001:
                    case 131001002:
                    case 131001003: // ????????? ??????
                        ret.statups.put(MapleBuffStat.PinkbeanAttackBuff, new Pair<>((int) ret.x, ret.duration));
                        break;
                    case 131001009:
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>((int) ret.IndieExp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>((int) ret.indieSpeed, ret.duration));
                        break;
                    case 131001015:
                        ret.statups.put(MapleBuffStat.PinkbeanMinibeenMove, new Pair<>((int) 1, 0));
                        break;
                    case 131001021:
                        ret.statups.put(MapleBuffStat.KeyDownMoving, new Pair<>((int) ret.x, ret.duration));
                        break;
                    case 131001106:
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>((int) ret.IndieExp, ret.duration));
                        break;
                    case 131001206:
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>((int) ret.IndieExp, ret.duration));
                        ret.statups.put(MapleBuffStat.DotHealHPPerSecond, new Pair<>((int) ret.dotHealHPPerSecondR, ret.duration));
                        break;
                    case 131001306:
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>((int) ret.IndieExp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        break;
                    case 131001406:
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>((int) ret.IndieExp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) ret.indieAsrR, ret.duration));
                        break;
                    case 131001506:
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>((int) ret.IndieExp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) ret.indieAsrR, ret.duration));
                        break;
                    // ????????????
                    case 142001003: // ESP ?????????
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>((int) ret.indieBooster, ret.duration));
                        break;
                    case 142001007: // ????????? ????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.KinesisPsychicEnergeShield, new Pair<>(1, ret.duration));
                        break;
                    case 142111010: // ????????? ??????
                        ret.statups.put(MapleBuffStat.NewFlying, new Pair<>(1, ret.duration));
                        break;
                    case 142121032: // ????????? ??????
                        ret.statups.put(MapleBuffStat.KinesisPsychicOver, new Pair<>(50, ret.duration));
                        break;
                    // ??????
                    case 151001004: // ???????????????
                        ret.duration = 1500;
                        ret.statups.put(MapleBuffStat.IndieFloating, new Pair<>(1, 1500));
                        ret.statups.put(MapleBuffStat.NewFlying, new Pair<>(1, 1500));
                        break;
                    case 151101006: // ???????????????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.Creation, new Pair<>(1, ret.duration));
                        break;
                    case 151101013: // ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.Wonder, new Pair<>(1, ret.duration));
                        break;
                    case 151121001: // ????????????
                        ret.statups.put(MapleBuffStat.Grave, new Pair<>(1, ret.duration));
                        break;
                    // ?????????
                    // ??????
                    case 155001205: // ??????
                        ret.statups.put(MapleBuffStat.IndieFloating, new Pair<>(1, ret.duration / 1000));
                        ret.statups.put(MapleBuffStat.NewFlying, new Pair<>(1, ret.duration / 1000));
                        break;
                    case 155101008: // ???????????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ComingDeath, new Pair<>(1, ret.duration));
                        break;
                    //??????
                    case 400031062:
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.x));
                        ret.statups.put(MapleBuffStat.DamR, new Pair<>((int) ret.damR, ret.x));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.x));
                        ret.statups.put(MapleBuffStat.ThanatosDescent, new Pair<>(1, ret.x));
                        break;
                    case 63121044: //???????????????
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.ZeroAuraSpd, new Pair<>((int) 1, ret.duration));
                        break;
                    // 5??? ??????
                    case 400001003: // ????????? ????????? ??????
                        ret.statups.put(MapleBuffStat.MaxHP, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.MaxMP, new Pair<>(ret.y, ret.duration));
                        break;
                    case 400001004: // ????????? ?????? ?????????
                        ret.statups.put(MapleBuffStat.CombatOrders, new Pair<>(1, ret.duration));
                        break;
                    case 400001005: // ????????? ??????????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>(ret.y, ret.duration));
//                        ret.statups.put(MapleBuffStat.IndiePdd, new Pair<>(ret.z, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieHp, new Pair<>((int) ret.indieMhp, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMp, new Pair<>((int) ret.indieMmp, ret.duration));
                        break;
                    case 400001006: // ????????? ?????? ?????????
                        ret.statups.put(MapleBuffStat.PartyBooster, new Pair<>(-2, (180000 + (ret.level * 3000))));
                        break;
                    case 400001020: // ????????? ?????? ??????
                        ret.statups.put(MapleBuffStat.HolySymbol, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.DropRate, new Pair<>(ret.v, ret.duration));
                        break;
                    case 400001023: // ??????????????? ???????????????
                        ret.statups.put(MapleBuffStat.DarkSight, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>(ret.y, ret.duration));
                        break;
                    case 400001025: // ???????????? ?????? 1??????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) ret.indieCooltimeReduce, ret.duration));
                        ret.statups.put(MapleBuffStat.FreudsProtection, new Pair<>(sourceid - 400001024, ret.duration));
                        break;
                    case 400001026: // ???????????? ?????? 2??????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) ret.indieCooltimeReduce, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.FreudsProtection, new Pair<>(sourceid - 400001024, ret.duration));
                        break;
                    case 400001027: // ???????????? ?????? 3??????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) ret.indieCooltimeReduce, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAllStat, new Pair<>((int) ret.indieAllStat, ret.duration));
                        ret.statups.put(MapleBuffStat.FreudsProtection, new Pair<>(sourceid - 400001024, ret.duration));
                        break;
                    case 400001028: // ???????????? ?????? 4??????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) ret.indieCooltimeReduce, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAllStat, new Pair<>((int) ret.indieAllStat, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.FreudsProtection, new Pair<>(sourceid - 400001024, ret.duration));
                        break;
                    case 400001029: // ???????????? ?????? 5??????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) ret.indieCooltimeReduce, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAllStat, new Pair<>((int) ret.indieAllStat, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.FreudsProtection, new Pair<>(sourceid - 400001024, ret.duration));
                        break;
                    case 400001030: // ???????????? ?????? 6??????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) ret.indieCooltimeReduce, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAllStat, new Pair<>((int) ret.indieAllStat, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.FreudsProtection, new Pair<>(sourceid - 400001024, ret.duration));
                        break;
                    case 400001037: // ?????? ?????? ???????????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>(20 + ret.level, ret.duration));
                        ret.statups.put(MapleBuffStat.MagicCircuitFullDrive, new Pair<>(ret.y, ret.duration));
                        break;
                    case 400001043: // ???????????? ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.q, ret.duration));
                        ret.statups.put(MapleBuffStat.Bless5th, new Pair<>(1, ret.duration));
                        break;
                    case 400001044: // ????????? ??????????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.q, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamageReduce, new Pair<>((int) ret.z, ret.duration));
                        ret.statups.put(MapleBuffStat.Bless5th, new Pair<>(1, ret.duration));
                        break;
                    case 400001047: // ?????? ????????? ?????? (??????)
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.Bless5th, new Pair<>(1, ret.duration));
                        break;
                    case 400001048: // ?????? ????????? ?????? (??????)
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>((int) ret.indiePad, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>((int) ret.indieMad, ret.duration));
                        ret.statups.put(MapleBuffStat.Bless5th, new Pair<>(1, ret.duration));
                        break;
                    case 400001049: // ?????? ????????? ?????? (?????????)
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.Bless5th, new Pair<>(ret.x, ret.duration));
                        break;
                    //5??? ??????
                    //V Update
                    case 400011000: // ?????? ??????
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) ret.indieIgnoreMobpdpR, ret.duration));
                        ret.statups.put(MapleBuffStat.AuraWeapon, new Pair<>((int) ret.z, ret.duration));
                        break;
                    case 400011015: // ????????? ????????????
                        ret.statups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>(ret.q, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-2, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        break;
                    case 400011017: // ?????? ?????????
                        ret.statups.put(MapleBuffStat.BonusAttack, new Pair<>((int) ret.level, ret.duration));
                        break;
                    //Beyond
                    case 400011039: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieJointAttack, new Pair<>(1, ret.duration));
                        break;
                    case 400011055: // ????????????
                        ret.statups.put(MapleBuffStat.Ellision, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400011066: // ?????? ?????? ??????
                        ret.statups.put(MapleBuffStat.IndieSuperStance, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) ret.asrR, ret.duration));
                        ret.statups.put(MapleBuffStat.BodyOfSteal, new Pair<>(1, ret.duration));
                        break;
                    // The Black
                    case 400011072: // ????????? ?????????
                        ret.duration = 10000;
                        ret.statups.put(MapleBuffStat.IndieDamageReduce, new Pair<>((int) ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.GrandCrossSize, new Pair<>(1, ret.duration));
                        break;
                    case 400011073: // ?????? ????????????
                        ret.statups.put(MapleBuffStat.ComboInstict, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400011109: // ????????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>(ret.y, ret.duration));
                        ret.statups.put(MapleBuffStat.Restore, new Pair<>(1, ret.duration));
                        break;
                    //5??? ?????????
                    //V Update
                    case 400021000: // ???????????? ??????
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.OverloadMana, new Pair<>(ret.z, ret.duration));
                        break;
                    case 400021003: // ?????????
                        ret.statups.put(MapleBuffStat.Pray, new Pair<>((int) ret.level, ret.duration));
                        break;
                    //Beyond
                    case 400021060: // ???????????? ???
                        ret.statups.put(MapleBuffStat.Etherealform, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieShotDamage, new Pair<>(1, ret.duration));
                        break;
                    //The Black

                    //5??? ??????
                    //V Update
                    case 400031000: // ???????????? ?????????
                        ret.statups.put(MapleBuffStat.GuidedArrow, new Pair<>(ret.z, ret.duration));//ret.z
                        break;
                    case 400031002: // ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 400031015: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.SplitArrow, new Pair<>((int) ret.level, ret.duration));
                        break;
                    //Beyond
                    case 400031017: // ????????????
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>((int) ret.indiePadR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieDamageReduce, new Pair<>((int) ret.indieDamReduceR, ret.duration));
                        break;
                    case 400031020: // ????????? ???
                        ret.statups.put(MapleBuffStat.BonusAttack, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400031021:
                        ret.statups.put(MapleBuffStat.BonusAttack, new Pair<>(2, ret.duration));
                        break;
                    case 400031023: // ???????????? ????????????
                        ret.statups.put(MapleBuffStat.CriticalReinForce, new Pair<>(ret.x, ret.duration));
                        break;
                    //5??? ??????
                    case 400041001: // ???????????? ?????????
                        ret.statups.put(MapleBuffStat.SpreadThrow, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400041029: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.Overload, new Pair<>(ret.x, ret.duration));
                        break;
                    case 400041035: // ????????????:??????
                        ret.statups.put(MapleBuffStat.ChainArtsFury, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400041052: // ?????? : ?????? ????????????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    //5??? ??????
                    case 400051006: // ?????? ??????
                        ret.statups.put(MapleBuffStat.BulletParty, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400051007: // ?????? ??????
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) ret.indiePmdR, ret.duration));
                        ret.statups.put(MapleBuffStat.Striker1st, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400051009: // ????????? ??????
                        ret.statups.put(MapleBuffStat.MultipleOption, new Pair<>(ret.q2, ret.duration));
                        break;
                    case 400051018: // ??????????????????
                        ret.statups.put(MapleBuffStat.Spotlight, new Pair<>((int) ret.level, ret.duration));
                        break;
                    case 400051033: // ?????? ????????????
                        ret.statups.put(MapleBuffStat.OverDrive, new Pair<>(ret.x, ret.duration));
                        break;
                    case 400051036: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.InfinitySpell, new Pair<>(ret.x, ret.duration));
                        break;
                    case 162001005:
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.GuardOfMountain, new Pair<>(1, ret.duration));
                        break;
                    case 162101000:
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.ReadVeinOfInfinity, new Pair<>(1, ret.duration));
                        break;
                    case 162101012:
                        ret.statups.put(MapleBuffStat.SeedOfMountain, new Pair<>(0, ret.duration));
                        break;
                    case 162111006:
                        ret.statups.put(MapleBuffStat.TraceOfVein, new Pair<>(0, ret.duration));
                        break;
                    case 162101009:
                        ret.statups.put(MapleBuffStat.IndieWickening, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSummon, new Pair<>(1, ret.duration));
                        break;
                    case 162121042:
                        ret.statups.put(MapleBuffStat.FreeVein, new Pair<>(0, ret.duration));
                        break;
                    case 162121003:
                        ret.statups.put(MapleBuffStat.AbsorptionRiver, new Pair<>(1, ret.duration));
                        break;
                    case 162121006:
                        ret.statups.put(MapleBuffStat.AbsorptionWind, new Pair<>(1, ret.duration));
                        break;
                    case 162121009:
                        ret.statups.put(MapleBuffStat.AbsorptionSun, new Pair<>(1, ret.duration));
                        break;
                    case 160010001:
                    case 80003058:
                        ret.statups.put(MapleBuffStat.IndieNormalMobDamage, new Pair<>(ret.w, ret.duration));
                        break;
                    case 162110007:
                        ret.statups.put(MapleBuffStat.IndiePmdR, new Pair<>(ret.x, ret.duration));
                        break;
                    case 162121044:
                        ret.statups.put(MapleBuffStat.IndieWickening, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSummon, new Pair<>(1, ret.duration));
                        break;
                    case 162121043:
                        ret.statups.put(MapleBuffStat.IndieCD, new Pair<>(ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>((int) ret.indieBDR, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieStance, new Pair<>((int) ret.indieStance, ret.duration));
                        break;
                    case 162111004:
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>((int) ret.indieDamR, ret.duration));
                        break;
                    case 162111002:
                        ret.statups.put(MapleBuffStat.IndieSummon, new Pair<>(1, ret.duration));
                        break;
                    case 162111001:
                        ret.statups.put(MapleBuffStat.IndieJump, new Pair<>((int) ret.indieJump, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieSpeed, new Pair<>((int) ret.indieSpeed, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>((int) ret.indieBooster, ret.duration));
                        break;
                    case 80003059:
                        ret.statups.put(MapleBuffStat.NewFlying, new Pair<>(1, ret.duration));
                        break;
                    case 162121022:
                        ret.duration = ret.q * 1000;
                        ret.statups.put(MapleBuffStat.AntiMagicShell, new Pair<>(1, 0));
                        ret.statups.put(MapleBuffStat.DreamDowon, new Pair<>(1, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieBarrierDischarge, new Pair<>(-ret.x, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieWickening, new Pair<>(1, ret.duration));
                        break;
                    default:
                        break;
                }

                if (GameConstants.isBeginnerJob(sourceid / 10000)) {
                    switch (sourceid % 10000) {
                        case 1001:
                            if (sourceid / 10000 == 3001 || sourceid / 10000 == 3000) { // resistance is diff
                                ret.statups.put(MapleBuffStat.Infiltrate, new Pair<>(ret.x, ret.duration));
                            } else {
                                ret.statups.put(MapleBuffStat.Recovery, new Pair<>(ret.x, ret.duration));
                            }
                            break;
                        case 1002:
                            ret.statups.put(MapleBuffStat.Speed, new Pair<>(ret.x, ret.duration));
                            break;
                        case 1005:
                            ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>(ret.x, ret.duration));
                            ret.statups.put(MapleBuffStat.IndieMadR, new Pair<>(ret.x, ret.duration));
                            break;
                        case 8000:
                            ret.statups.put(MapleBuffStat.Speed, new Pair<>((int) ret.speed, ret.duration));
                            ret.statups.put(MapleBuffStat.Jump, new Pair<>((int) ret.jump, ret.duration));
                            break;
                        case 8002:
                            ret.statups.put(MapleBuffStat.SharpEyes, new Pair<>((10 << 8) + 8, ret.duration));
                            break;
                        case 8003:
                            ret.statups.put(MapleBuffStat.MaxHP, new Pair<>(40, ret.duration));
                            ret.statups.put(MapleBuffStat.MaxMP, new Pair<>(40, ret.duration));
                            break;
                        case 8004:
                            ret.statups.put(MapleBuffStat.CombatOrders, new Pair<>(1, ret.duration));
                            break;
                        case 8005:
                            ret.statups.put(MapleBuffStat.IndiePad, new Pair<>(20, ret.duration));
                            ret.statups.put(MapleBuffStat.IndieMad, new Pair<>(20, ret.duration));
                            ret.statups.put(MapleBuffStat.IndieHp, new Pair<>(475, ret.duration));
                            ret.statups.put(MapleBuffStat.IndieMp, new Pair<>(475, ret.duration));
                            break;
                        case 8006:
                            ret.statups.put(MapleBuffStat.PartyBooster, new Pair<>(-1, ret.duration));
                            break;
                    }
                } else {
                    switch (sourceid % 10000) {
                        case 1085: //????????? ?????????
                        case 1087: //?????? ????????? ?????????
                        case 1090: //?????? ????????? ?????????
                        case 1179: //????????? ????????? ?????????
                            ret.duration = 0;
                            break;
                    }
                }
            } else {
                switch (sourceid) {
                    case 2022125:
                        ret.statups.put(MapleBuffStat.Pdd, new Pair<>(1, ret.duration));
                        break;
                    case 2022126:
                        ret.statups.put(MapleBuffStat.Pdd, new Pair<>(1, ret.duration));
                        break;
                    case 2022127:
                        ret.statups.put(MapleBuffStat.Acc, new Pair<>(1, ret.duration));
                        break;
                    case 2022128:
                        ret.statups.put(MapleBuffStat.Eva, new Pair<>(1, ret.duration));
                        break;
                    case 2022129:
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>(1, ret.duration));
                        break;
                    case 2022747: // Angelic Bless
                        ret.statups.clear();
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>(10, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMadR, new Pair<>(10, ret.duration));
                        ret.statups.put(MapleBuffStat.RepeatEffect, new Pair<>(1, ret.duration));
                        break;
                    case 2022746:
                    case 2022764:
                        ret.statups.clear();
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>(5, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMadR, new Pair<>(5, ret.duration));
                        ret.statups.put(MapleBuffStat.RepeatEffect, new Pair<>(1, ret.duration));
                        break;
                    case 2022823:
                        ret.statups.clear();
                        ret.duration = 0;
                        ret.statups.put(MapleBuffStat.IndiePadR, new Pair<>(12, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMadR, new Pair<>(12, ret.duration));
                        ret.statups.put(MapleBuffStat.RepeatEffect, new Pair<>(1, ret.duration));
                        break;
                    case 2003516:
                    case 2003517:
                    case 2003518:
                    case 2003519:
                    case 2003520:
                    case 2003552:
                    case 2003553:
                    case 2003561:
                    case 2003566:
                    case 2003568:
                    case 2003570:
                    case 2003571:
                    case 2003572:
                    case 2003576:
                    case 2003591: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.Inflation, new Pair<>((int) ret.inflation, ret.duration));
                        break;
                    case 2450064: // ????????? 2??? ??????
                        ret.statups.put(MapleBuffStat.ExpBuffRate, new Pair<>(100, ret.duration));
                        break;
                    case 2450134: // ????????? 3??? ??????
                        ret.statups.put(MapleBuffStat.ExpBuffRate, new Pair<>(200, ret.duration));
                        break;
                    case 2023072: // ????????? 2??? ??????
                        ret.statups.put(MapleBuffStat.ItemUpByItem, new Pair<>(100, ret.duration));
                        break;
                    case 2450124: // MVP ????????? ?????????
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>(150, ret.duration));
                        break;
                    case 2002093: // ????????? ????????? ???
                        ret.hpR = 100;
                        ret.mpR = 100;
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>(300, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>(300, ret.duration));
                        break;
                    case 2003550: // ?????? ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>(10, ret.duration));
                        break;
                    case 2003551: // ?????? ????????? ??????
                        ret.statups.put(MapleBuffStat.DropItemRate, new Pair<>(20, ret.duration));
                        break;
                    case 2003596: // ?????? ?????? ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieBDR, new Pair<>(20, ret.duration));
                        break;
                    case 2003597: // ?????? ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndieDamR, new Pair<>(10, ret.duration));
                        break;
                    case 2003598: // ?????? ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(20, ret.duration));
                        break;
                    case 2003599: // ?????? ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndieAllStatR, new Pair<>(10, ret.duration));
                        break;
                    case 2023520: // ????????? ?????????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>(30, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>(30, ret.duration));
                        break;
                    case 2023553: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>(30, ret.duration));
                        break;
                    case 2023554: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, ret.duration));
                        break;
                    case 2023555: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>(30, ret.duration));
                        break;
                    case 2023556: // ???????????? ??????
                        ret.statups.put(MapleBuffStat.IndieHp, new Pair<>(2000, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMp, new Pair<>(2000, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>(10, ret.duration));
                        break;
                    case 2023558: // MVP ????????? ??????
                        ret.statups.put(MapleBuffStat.IndieExp, new Pair<>(50, ret.duration));
                        break;
                    case 2023658: // ???????????? ??? 1??????
                    case 2023659: // ???????????? ??? 2??????
                    case 2023660: // ???????????? ??? 3??????
                        ret.statups.put(MapleBuffStat.IndiePad, new Pair<>(30, ret.duration));
                        ret.statups.put(MapleBuffStat.IndieMad, new Pair<>(30, ret.duration));
                        break;
                    case 2023661: // ???????????? ?????? 1??????
                    case 2023662: // ???????????? ?????? 2??????
                    case 2023663: // ???????????? ?????? 3??????
                        ret.statups.put(MapleBuffStat.LuckOfUnion, new Pair<>(50, ret.duration));
                        break;
                    case 2023664: // ???????????? ??? 1??????
                    case 2023665: // ???????????? ??? 2??????
                    case 2023666: // ???????????? ??? 3??????
                        ret.statups.put(MapleBuffStat.WealthOfUnion, new Pair<>(50, ret.duration));
                        break;
                    case 2450147: // ???????????? ?????? 1??????
                    case 2450148: // ???????????? ?????? 2??????
                    case 2450149: // ???????????? ?????? 3??????
                        ret.statups.put(MapleBuffStat.ExpBuffRate, new Pair<>(100, ret.duration));
                        break;
                }
            }

            if (ret.isMorph()) {
                ret.statups.put(MapleBuffStat.Morph, new Pair<>(ret.morphId, ret.duration));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public final boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyToBuff(MapleCharacter chr) {
        return applyTo(chr, chr, true, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), true);
    }

    public final boolean applyToBuff(MapleCharacter chr, int duration) {
        return applyTo(chr, chr, true, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto) {
        return applyTo(applyfrom, applyto, true, applyto.getTruePosition(), duration, (byte) (applyfrom.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary) {
        return applyTo(applyfrom, applyto, primary, applyto.getTruePosition(), duration, (byte) (applyfrom.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, boolean primary) {
        return applyTo(chr, chr, primary, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, boolean primary, boolean showEffect) {
        return applyTo(chr, chr, primary, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), showEffect);
    }

    public final boolean applyTo(MapleCharacter chr, int duration) {
        return applyTo(chr, chr, true, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, boolean primary, int duration) {
        return applyTo(chr, chr, primary, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, boolean primary, int duration, boolean showEffect) {
        return applyTo(chr, chr, primary, chr.getTruePosition(), duration, (byte) (chr.isFacingLeft() ? 1 : 0), showEffect);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos, boolean primary, int duration) {
        return applyTo(chr, chr, primary, pos, duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos, boolean primary) {
        return applyTo(chr, chr, primary, pos, duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos, duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, boolean primary, Point pos) {
        return applyTo(chr, chr, primary, pos, duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos, int duration) {
        return applyTo(chr, chr, true, pos, duration, (byte) (chr.isFacingLeft() ? 1 : 0), false);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos, byte rltype) {
        return applyTo(chr, chr, true, pos, duration, rltype, false);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos, byte rltype, boolean showEffect) {
        return applyTo(chr, chr, true, pos, duration, rltype, showEffect);
    }

    public final boolean applyTo(MapleCharacter chr, Point pos, boolean primary, byte rltype) {
        return applyTo(chr, chr, primary, pos, duration, rltype, false);
    }

    public final boolean applyTo(final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary,
            Point pos, int localDuration, byte rltype, boolean showEffect) {
        if (applyfrom.getMapId() == ServerConstants.WarpMap && skill && !applyfrom.isGM() && (getSummonMovementType() != null || isMist())) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
            return false;
        } else if (isHeal() && (applyfrom.getMapId() == 749040100 || applyto.getMapId() == 749040100)) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
            return false; // z
/*        } else if ((isSoaring_Mount() && applyfrom.getBuffedValue(MapleBuffStat.RideVehicle) == null)
             || (isSoaring_Normal() && !applyfrom.getMap().canSoar())) {
             applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
             return false;*/
        } else if (sourceid == 4341006 && applyfrom.getBuffedValue(MapleBuffStat.ShadowPartner) == null) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
            return false;
        } else if (sourceid == 33101008
                && (applyfrom.getBuffedValue(MapleBuffStat.IndieSummon) != null || !applyfrom.canSummon())) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
            return false;
        } else if (sourceid == 33101004 && applyfrom.getMap().isTown()) {
            applyfrom.dropMessage(5, "You may not use this skill in towns.");
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
            return false;
        }
        long hpchange = calcHPChange(applyfrom, primary);
        long mpchange = calcMPChange(applyfrom, primary);
        int powerchange = calcPowerChange(applyfrom, primary);

        final PlayerStats stat = applyto.getStat();
        if (primary) {
            if (itemConNo != 0 && !applyto.inPVP()) {
                if (!applyto.haveItem(itemCon, itemConNo, false, true)) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
                    return false;
                }
                MapleInventoryManipulator.removeById(applyto.getClient(), GameConstants.getInventoryType(itemCon),
                        itemCon, itemConNo, false, true);
            }
        }
        if (isResurrection()) {
            hpchange = stat.getMaxHp();
            applyto.setStance(0); // TODO fix death bug, player doesnt spawn on other screen
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        } else if (isHeroWill() || (sourceid == 80001478 && makeChanceResult())) {
            applyto.dispelDebuffs();
        } else if (isMPRecovery()) {
            final long toDecreaseHP = ((stat.getMaxHp() / 100) * 10);
            if (stat.getHp() > toDecreaseHP) {
                hpchange += -toDecreaseHP; // -10% of max HP
                mpchange += ((toDecreaseHP / 100) * getY());
            } else {
                hpchange = stat.getHp() == 1 ? 0 : stat.getHp() - 1;
            }
        }
        if (applyfrom.getId() != applyto.getId()) {
            mpchange = 0;
            if (!isHeal() && !isResurrection()) {
                hpchange = 0;
            }
        }

        final Map<MapleStat, Long> hpmpupdate = new EnumMap<MapleStat, Long>(MapleStat.class);

        if (applyto.getBuffedValue(MapleBuffStat.DarknessAura) != null && sourceid == 400011047) {
            mpchange = 0;
        }

        if (hpchange != 0 && applyto.isAlive()) {
            if (hpchange < 0 && (-hpchange) > stat.getHp() && !applyto.hasDisease(MapleBuffStat.Undead)) {
                applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
                return false;
            }
            stat.setHp(stat.getHp() + hpchange, applyto);
            hpmpupdate.put(MapleStat.HP, Long.valueOf(stat.getHp()));
        }
        if (mpchange != 0 && applyto.isAlive()) {
            if (mpchange < 0 && (-mpchange) > stat.getMp()) {
                applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
                return false;
            }
            // short converting needs math.min cuz of overflow
            if ((mpchange < 0 && GameConstants.isDemonSlayer(applyto.getJob())) || !GameConstants.isDemonSlayer(applyto.getJob())) { // heal
                stat.setMp(stat.getMp() + mpchange, applyto);
            }
            hpmpupdate.put(MapleStat.MP, Long.valueOf(stat.getMp()));
        }

        /*        if (primary || applyfrom.getId() != applyto.getId()) {
         boolean itemReaction = false;
         if (MapleItemInformationProvider.getInstance().getItemEffect(sourceid) != null) {
         itemReaction = true;
         }
         applyto.getClient().getSession().writeAndFlush(CWvsContext.updatePlayerStats(hpmpupdate, itemReaction, false, applyto, false));
         if (!itemReaction && overTime) {
         applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
         }
         }*/
        if (primary || applyfrom.getId() != applyto.getId()) {
            applyto.getClient().getSession().writeAndFlush(CWvsContext.updatePlayerStats(hpmpupdate, !skill, false, applyto, false));
        }

        if (!applyto.isAlive()) {
            return false;
        }

        if (sourceid == 36121054) {
            powerchange = -20;
        }

        if (sourceid == 2001009 && applyfrom.getBuffedValue(2201009) && applyfrom.getBuffedEffect(2201009).makeChanceResult()) {
            SkillFactory.getSkill(2201009).getEffect(applyfrom.getSkillLevel(2201009)).applyTo(applyfrom, false);
        }

        if (powerchange != 0) {
            if (applyto.getXenonSurplus() - powerchange < 0) {
                return false;
            }
            applyto.gainXenonSurplus((short) -powerchange, SkillFactory.getSkill(getSourceId()));
        }
        if (expinc != 0) {
            applyto.gainExp(expinc, true, true, false);
            applyto.getClient().getSession().writeAndFlush(EffectPacket.showNormalEffect(applyto, 24, true));
        } else if (isReturnScroll()) {
            applyReturnScroll(applyto);
        } else if (useLevel > 0 && !skill) {
            applyto.setExtractor(new MapleExtractor(applyto, sourceid, useLevel * 50, 1440)); // no clue about time left
            applyto.getMap().spawnExtractor(applyto.getExtractor());
        } else if (cosmetic > 0) {
            if (cosmetic >= 30000) {
                applyto.setHair(cosmetic);
                applyto.updateSingleStat(MapleStat.HAIR, cosmetic);
            } else if (cosmetic >= 20000) {
                applyto.setFace(cosmetic);
                applyto.updateSingleStat(MapleStat.FACE, cosmetic);
            } else if (cosmetic < 100) {
                applyto.setSkinColor((byte) cosmetic);
                applyto.updateSingleStat(MapleStat.SKIN, cosmetic);
            }
            applyto.equipChanged();
        } else if (sourceid == 11101022 || sourceid == 11111022) {
            if (applyfrom.getBuffedEffect(MapleBuffStat.PoseType) != null) {
                if (applyfrom.getBuffedEffect(MapleBuffStat.PoseType).getSourceId() != sourceid) {
                    applyfrom.cancelEffectFromBuffStat(MapleBuffStat.PoseType);
                }
            }
        }
        for (Entry<MapleTraitType, Integer> t : traits.entrySet()) {
            applyto.getTrait(t.getKey()).addExp(t.getValue(), applyto);
        }
//        applyto.dropMessageGM(6, "skill " + ((sourceid == 12120013 || sourceid == 12120014) && primary));
        //      applyto.dropMessageGM(6, "applyto skill : " + sourceid);

        if (sourceid == 2121003 && !applyto.getPosionNovas().isEmpty()) {
            applyto.getClient().getSession().writeAndFlush(CField.poisonNova(applyto, applyto.getPosionNovas()));
            applyto.setPosionNovas(new ArrayList<>());
        }
        /* if (sourceid == 12120014 && applyfrom.getBuffedValue(12120013)) {
         MapleSummon summona = null;
         for (MapleSummon summon : applyto.getSummons()) {
         if (summon.getSkill() == 12120013) {
         summona = summon;
         }
         }
         if (summona != null) {
         summona.removeSummon(applyto.getMap(), false);
         applyto.removeSummon(summona);
         }*/
        if ((sourceid == 12120013 || sourceid == 12120014) && primary) {//&& applyfrom.getBuffedValue(12120014)) {
            /* MapleSummon summona = null;
             for (MapleSummon summon : applyto.getSummons()) {
             if (summon.getSkill() == 12120014) {
             summona = summon;
             }
             }
             if (summona != null) {
             summona.removeSummon(applyto.getMap(), false);
             applyto.removeSummon(summona);
             }*/
            if (applyfrom.getBuffedValue(12120013)) {
                applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 12120013);
            }
            if (applyfrom.getBuffedValue(12120014)) {
                applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 12120014);
            }
        }

        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && (((sourceid == 400021071 || sourceid == 35111008 || sourceid == 35120002 || sourceid == 162101012) && primary) || (sourceid != 400021071 && sourceid != 35111008 && sourceid != 35120002 && sourceid != 162101012))) {
            if (sourceid == 400011001) {
                /*if(!applyto.getBuffedValue(400011001)){
                    return false;
                }*/

                MapleSummon summon = applyto.getSummon(400011001);
                if (summon != null) {
                    summon.setSkill(400011002);
                    summon.setMovementType(SummonMovementType.STATIONARY);
                    applyto.getMap().broadcastMessage(SummonPacket.removeSummon(summon, true));
                    applyto.getMap().broadcastMessage(SummonPacket.spawnSummon(summon, true, (int) applyto.getBuffLimit(400011001)));
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
                    return true;
                } else {
                    summon = applyto.getSummon(400011002);
                    if (summon != null) {
                        summon.setSkill(400011001);
                        summon.setMovementType(SummonMovementType.FOLLOW);
                        applyto.getMap().broadcastMessage(SummonPacket.removeSummon(summon, true));
                        applyto.getMap().broadcastMessage(SummonPacket.spawnSummon(summon, true, (int) applyto.getBuffLimit(400011001)));
                        applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
                        return true;
                    }
                }
            } else if (sourceid == 1301013) {
                applyto.cancelEffectFromBuffStat(MapleBuffStat.Beholder, sourceid);

            } else if ((sourceid != 35111002 && sourceid != 400021047 && sourceid != 400011065 && sourceid != 14000027 && sourceid != 14100027 && sourceid != 14110029 && sourceid != 14120008 && sourceid != 151100002) && primary) {
                applyto.cancelEffect(this, true, -1);

                List<MapleSummon> toRemove = new ArrayList<>();

                int maxSize = 1;
                if (sourceid == 162101012) {
                    maxSize = 4;
                }

                if (!applyto.getSummons().isEmpty()) {
                    Iterator<MapleSummon> s = applyto.getSummons().iterator();
                    while (s.hasNext()) {
                        MapleSummon summon = s.next();
                        if (summon.getSkill() == sourceid) { //removes bots n tots
                            maxSize -= 1;
                            if (maxSize <= 0)
                                toRemove.add(summon);
                        }
                    }
                }
                if (sourceid == 152121005) {
                    toRemove.add(applyto.getSummon(152001003));
                    toRemove.add(applyto.getSummon(152101008));
                }

                for (MapleSummon summon : toRemove) {
                    summon.removeSummon(applyto.getMap(), false);
                }

                if (sourceid == 400021005) {
                    long duration = applyto.getBuffLimit(applyto.getBuffedEffect(MapleBuffStat.Larkness).getSourceId());
                    applyto.setUseTruthDoor(true);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                    SkillFactory.getSkill(20040220).getEffect(1).applyTo(applyto, (int) duration);
                } else if (sourceid == 35111008) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 35111008);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 35120002);
                }
            }

            boolean spawn;

            if (sourceid == 400051011) {
                spawn = showEffect;
            } else {
                spawn = true;
            }

            if (sourceid == 35111002 && applyfrom.getSkillLevel(35120044) > 0) {
                localDuration += SkillFactory.getSkill(35120044).getEffect(1).getY() * 1000;
            }

            if (sourceid == 35120002 && applyfrom.getSkillLevel(35120048) > 0) {
                localDuration += SkillFactory.getSkill(35120048).getEffect(1).getDuration();
            }

            if ((sourceid == 36121002 || sourceid == 36121013 || sourceid == 36121014) && applyfrom.getSkillLevel(36120051) > 0) {
                localDuration += SkillFactory.getSkill(36120051).getEffect(1).getDuration();
            }

            if (skill && SkillFactory.getSkill(sourceid) != null && !SkillFactory.getSkill(sourceid).isHyper() && !SkillFactory.getSkill(sourceid).isVMatrix() && !SkillFactory.getSkill(sourceid).isNotIncBuffDuration()) {
                localDuration = alchemistModifyVal(applyfrom, localDuration, false);
            }

            if (sourceid == 400051017) {
                localDuration = 0;
            }

            if (sourceid == 151111001 && applyfrom.getSkillLevel(151120035) > 0) {
                localDuration += SkillFactory.getSkill(151120035).getEffect(1).getDuration();
            }

            if (spawn) {
                int summId = sourceid;
                applyto.dropMessageGM(-8, "summon sourceId : " + summId);
                if (sourceid == 3111002) {
                    final Skill elite = SkillFactory.getSkill(3120012);
                    if (applyfrom.getTotalSkillLevel(elite) > 0) {
                        return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary,
                                pos, localDuration, rltype, showEffect);
                    }
                } else if (sourceid == 3211002) {
                    final Skill elite = SkillFactory.getSkill(3220012);
                    if (applyfrom.getTotalSkillLevel(elite) > 0) {
                        return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary,
                                pos, localDuration, rltype, showEffect);
                    }
                }

                if (sourceid == 400011012 || sourceid == 400011013 || sourceid == 400011014) {
                    pos = applyfrom.getTruePosition();
                }
                if (sourceid == 400031051) {
                    Point my = applyto.getPosition();
                    int x = (int) my.getX();
                    int y = (int) my.getY();
                    //??????????????? ?????? ???????????? ???????????? ?????? ??????
                    Point Top = new Point(x, y + 250);
                    Point Bottom = new Point(x, y - 250);
                    Point Right = new Point(x + 605, y);
                    Point Left = new Point(x - 600, y);

                    MapleFootholdTree fht = applyto.getMap().getFootholds();
                    MapleFoothold fhright = fht.findBelow(Right);
                    MapleFoothold fhleft = fht.findBelow(Left);
                    MapleFoothold fhtop = fht.findBelow(Top);
                    MapleFoothold fhbottom = fht.findBelow(Bottom);
                    int count = 0;
                    if (fhright != null && count < 2) {
                        MapleSummon tosummon = new MapleSummon(applyfrom, summId, new Point(fhright.getPoint1()), summonMovementType, rltype, localDuration);
                        applyfrom.dropMessageGM(-8, "spawn summon R : " + summId);
                        applyfrom.getMap().spawnSummon(tosummon, localDuration);
                        applyfrom.addSummon(tosummon);
                        count++;
                    }
                    if (fhleft != null && count < 2) {
                        MapleSummon tosummon = new MapleSummon(applyfrom, summId, new Point(fhleft.getPoint1()), summonMovementType, rltype, localDuration);
                        applyfrom.dropMessageGM(-8, "spawn summon L : " + summId);
                        applyfrom.getMap().spawnSummon(tosummon, localDuration);
                        applyfrom.addSummon(tosummon);
                        count++;
                    }
                    if (fhtop != null && count < 2) {
                        MapleSummon tosummon = new MapleSummon(applyfrom, summId, new Point(fhtop.getPoint1()), summonMovementType, rltype, localDuration);
                        applyfrom.dropMessageGM(-8, "spawn summon T : " + summId);
                        applyfrom.getMap().spawnSummon(tosummon, localDuration);
                        applyfrom.addSummon(tosummon);
                        count++;
                    }
                    if (fhbottom != null && count < 2) {
                        MapleSummon tosummon = new MapleSummon(applyfrom, summId, new Point(fhbottom.getPoint1()), summonMovementType, rltype, localDuration);
                        applyfrom.dropMessageGM(-8, "spawn summon B : " + summId);
                        applyfrom.getMap().spawnSummon(tosummon, localDuration);
                        applyfrom.addSummon(tosummon);
                        count++;
                    }
                }
                if (summId == 400001039) {
                    summId = 400001040;
                    localDuration = 50000;
                } else if (summId == 400001059) {
                    summId = 400001060;
                    localDuration = 51000;
                } else if (summId == 152101000) {
                    localDuration = Integer.MAX_VALUE;
                    if (applyto.getSkillLevel(152120014) > 0) {
                        SkillFactory.getSkill(152120014).getEffect(1).applyTo(applyto, false);
                    } else if (applyto.getSkillLevel(152110008) > 0) {
                        SkillFactory.getSkill(152110008).getEffect(1).applyTo(applyto, false);
                    } else {
                        SkillFactory.getSkill(152100010).getEffect(1).applyTo(applyto, false);
                    }
                }

                final MapleSummon tosummon = new MapleSummon(applyfrom, summId, new Point(pos == null ? applyfrom.getTruePosition() : pos), summonMovementType, rltype, localDuration);
                applyfrom.dropMessageGM(-8, "spawn summon : " + summId + " / duration : " + localDuration + " / pos : " + tosummon.getTruePosition());
                applyfrom.getMap().spawnSummon(tosummon, localDuration);
                applyfrom.addSummon(tosummon);

                tosummon.addHP(x);

                if (sourceid == 12111022) {
                    applyfrom.maelstrom = 0;
                }

                if (sourceid == 400011077) {
                    SkillFactory.getSkill(400011078).getEffect(level).applyTo(applyto, false, localDuration);
                } else if (sourceid == 14121054) {
                    SkillFactory.getSkill(14121055).getEffect(level).applyTo(applyto, false, localDuration);
                } else if (sourceid == 14121055) {
                    SkillFactory.getSkill(14121056).getEffect(level).applyTo(applyto, false, localDuration);
                } else if (sourceid == 131001019 && primary) {
                    int size = Randomizer.rand(x, y);
                    for (int i = 0; i < size - 1; ++i) {
                        applyTo(applyto, false);
                    }
                } else if (sourceid == 131001022) {
                    SkillFactory.getSkill(131002022).getEffect(level).applyTo(applyto, false, localDuration);
                    SkillFactory.getSkill(131003022).getEffect(level).applyTo(applyto, false, localDuration);
                    SkillFactory.getSkill(131004022).getEffect(level).applyTo(applyto, false, localDuration);
                    SkillFactory.getSkill(131005022).getEffect(level).applyTo(applyto, false, localDuration);
                    SkillFactory.getSkill(131006022).getEffect(level).applyTo(applyto, false, localDuration);
                } else if (sourceid == 400031007) {
                    SkillFactory.getSkill(400031008).getEffect(level).applyTo(applyto, false, localDuration);
                } else if (sourceid == 400031008) {
                    SkillFactory.getSkill(400031009).getEffect(level).applyTo(applyto, false, localDuration);
                } else if (sourceid >= 5210015 && sourceid <= 5210018 && primary) {
                    int newSource = Randomizer.rand(5210015, 5210018);
                    while (newSource == sourceid) {
                        newSource = Randomizer.rand(5210015, 5210018);
                    }
                    SkillFactory.getSkill(newSource).getEffect(level).applyTo(applyto, false, localDuration);

                    if (applyto.getSkillLevel(5220019) > 0) {
                        SkillFactory.getSkill(5220019).getEffect(applyto.getSkillLevel(5220019)).applyTo(applyto, false, localDuration);
                    }

                } else if (sourceid == 400051038) {
                    MapleSummon summon = applyto.getSummon(400051052);
                    if (summon != null) {
                        summon.removeSummon(applyto.getMap(), false);
                    }

                    summon = applyto.getSummon(400051053);
                    if (summon != null) {
                        summon.removeSummon(applyto.getMap(), false);
                    }

                    try {

                        final MapleSummon tosummon2 = new MapleSummon(applyfrom, 400051052,
                                new Point(applyfrom.getTruePosition().x + 100, applyfrom.getTruePosition().y), summonMovementType, rltype, localDuration);

                        applyfrom.getMap().spawnSummon(tosummon2, localDuration);
                        applyfrom.addSummon(tosummon2);
                        tosummon2.addHP(x);

                        final MapleSummon tosummon3 = new MapleSummon(applyfrom, 400051053,
                                new Point(applyfrom.getTruePosition().x + 200, applyfrom.getTruePosition().y), summonMovementType, rltype, localDuration);

                        applyfrom.getMap().spawnSummon(tosummon3, localDuration);
                        applyfrom.addSummon(tosummon3);
                        tosummon3.addHP(x);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (sourceid == 5321004) {

                    if (applyfrom.getSkillLevel(5320044) > 0) {
                        localDuration += SkillFactory.getSkill(5320044).getEffect(1).getDuration();
                    }

                    MapleSummon summon = applyto.getSummon(5320011);
                    if (summon != null) {
                        summon.removeSummon(applyto.getMap(), false);
                    }

                    try {

                        final MapleSummon tosummon2 = new MapleSummon(applyfrom, 5320011,
                                new Point(applyfrom.getTruePosition().x + 50, applyfrom.getTruePosition().y), summonMovementType, rltype, localDuration);

                        applyfrom.getMap().spawnSummon(tosummon2, localDuration);
                        applyfrom.addSummon(tosummon2);
                        tosummon2.addHP(x);

                        if (applyfrom.getSkillLevel(5320045) > 0) {
                            final MapleSummon tosummon3 = new MapleSummon(applyfrom, 5320011,
                                    new Point(applyfrom.getTruePosition().x + 80, applyfrom.getTruePosition().y), summonMovementType, rltype, localDuration);

                            applyfrom.getMap().spawnSummon(tosummon3, localDuration);
                            applyfrom.addSummon(tosummon3);
                            tosummon3.addHP(x);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (tosummon.getSkill() == 131001017) {

                    MapleSummon summon = applyto.getSummon(131001017);
                    if (summon != null) {
                        summon.removeSummon(applyto.getMap(), false);
                    }

                    summon = applyto.getSummon(131002017);
                    if (summon != null) {
                        summon.removeSummon(applyto.getMap(), false);
                    }

                    summon = applyto.getSummon(131003017);
                    if (summon != null) {
                        summon.removeSummon(applyto.getMap(), false);
                    }

                    try {
                        final MapleSummon tosummon2 = new MapleSummon(applyfrom, 131002017,
                                new Point(applyfrom.getTruePosition().x + 50, applyfrom.getTruePosition().y), summonMovementType, rltype, localDuration);

                        applyfrom.getMap().spawnSummon(tosummon2, localDuration);
                        applyfrom.addSummon(tosummon2);
                        tosummon2.addHP(x);

                        final MapleSummon tosummon3 = new MapleSummon(applyfrom, 131003017,
                                new Point(applyfrom.getTruePosition().x + 80, applyfrom.getTruePosition().y), summonMovementType, rltype, localDuration);

                        applyfrom.getMap().spawnSummon(tosummon3, localDuration);
                        applyfrom.addSummon(tosummon3);
                        tosummon3.addHP(x);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (sourceid == 4341006) {
                    applyfrom.cancelEffectFromBuffStat(MapleBuffStat.ShadowPartner);
                } else if (sourceid == 35111002) {
                    List<Integer> count = new ArrayList<Integer>();

                    Iterator<MapleSummon> s = applyto.getSummons().iterator();
                    while (s.hasNext()) {
                        MapleSummon summon = s.next();
                        if (summon.getSkill() == sourceid) {
                            count.add(summon.getObjectId());
                        }
                    }

                    if (count.size() == 3) {
                        applyfrom.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, getCooldown(applyfrom)));
                        applyfrom.addCooldown(sourceid, System.currentTimeMillis(), getCooldown(applyfrom));

                        applyfrom.getMap().broadcastMessage(
                                CField.teslaTriangle(applyfrom.getId(), count.get(0), count.get(1), count.get(2)));
                    }
                } else if (sourceid == 35121003) {
                    applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyfrom)); // doubt we need
                } else if (sourceid == 400051017) {
                    MapleAtom atom = new MapleAtom(false, applyfrom.getId(), 30, true, sourceid, 0, 0);

                    final List<MapleMapObject> objs = applyfrom.getMap().getMapObjectsInRange(applyto.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                    List<Integer> monsters = new ArrayList<>();

                    for (MapleMapObject obj : objs) {
                        atom.addForceAtom(new ForceAtom(2, 0x11, 0x1A, 0x27, (short) 630, applyfrom.getTruePosition()));
                        monsters.add(obj.getObjectId());
                    }

                    atom.setDwTargets(monsters);
                    ForceAtom forceAtom = new ForceAtom(1, 0x31, 5, Randomizer.rand(45, 90), (short) 1440);
                    atom.addForceAtom(forceAtom);
                    applyfrom.getMap().spawnMapleAtom(atom);

                }
            }
        } else if (isMechDoor()) {
            int newId = 0;
            boolean applyBuff = false;
            if (applyto.getMechDoors().size() >= 2) {
                final MechDoor remove = applyto.getMechDoors().remove(0);
                newId = remove.getId();
                applyto.getMap().broadcastMessage(CField.removeMechDoor(remove, true));
                applyto.getMap().removeMapObject(remove);
            } else {
                for (MechDoor d : applyto.getMechDoors()) {
                    if (d.getId() == newId) {
                        applyBuff = true;
                        newId = 1;
                        break;
                    }
                }
            }
            final MechDoor door = new MechDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos),
                    newId, localDuration);
            applyto.getMap().spawnMechDoor(door);
            applyto.addMechDoor(door);
            if (!applyBuff) {
                return true; // do not apply buff until 2 doors spawned
            }
        }
        if (primary && availableMap != null) {
            for (Pair<Integer, Integer> e : availableMap) {
                if (applyto.getMapId() < e.left || applyto.getMapId() > e.right) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
                    return true;
                }
            }
        }

        applyBuffEffect(applyfrom, applyto, primary, localDuration, pos, showEffect);

        if (applyfrom.getId() == applyto.getId() && applyfrom.getParty() != null) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
            final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds,
                    Arrays.asList(MapleMapObjectType.PLAYER));

            for (final MapleMapObject affectedmo : affecteds) {
                final MapleCharacter affected = (MapleCharacter) affectedmo;
                if (affected.getParty() != null && applyfrom.getId() != affected.getId()) {
                    if (isPartyBuff(applyfrom, affected) && calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft()).contains(applyto.getTruePosition()) && (applyfrom.getParty().getId() == affected.getParty().getId())) {
                        applyTo(applyto, affected, primary, pos, localDuration, (byte) 0, false);
                        affected.getClient().getSession().writeAndFlush(EffectPacket.showEffect(affected, 0, sourceid, 4, 0, 0, (byte) (affected.getTruePosition().x > pos.x ? 1 : 0), true, pos, null, null));
                        affected.getMap().broadcastMessage(affected, EffectPacket.showEffect(affected, 0, sourceid, 4, 0, 0, (byte) (affected.getTruePosition().x > pos.x ? 1 : 0), false, pos, null, null), false);
                    }
                }
            }
        }

        if (GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1005) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
            final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds,
                    Arrays.asList(MapleMapObjectType.PLAYER));
            for (final MapleMapObject affectedmo : affecteds) {
                final MapleCharacter affected = (MapleCharacter) affectedmo;
                if (applyto == applyfrom && affected != applyfrom) {
                    applyTo(applyto, affected, primary, pos, localDuration, (byte) 0, false);
                    affected.getClient().getSession().writeAndFlush(EffectPacket.showEffect(affected, 0, sourceid, 4, 0, 0, (byte) (affected.getTruePosition().x > pos.x ? 1 : 0), true, pos, null, null));
                    affected.getMap().broadcastMessage(affected, EffectPacket.showEffect(affected, 0, sourceid, 4, 0, 0, (byte) (affected.getTruePosition().x > pos.x ? 1 : 0), false, pos, null, null), false);
                }
            }
        }

        if (isMagicDoor()) { // Magic Door
            MapleDoor door = new MapleDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), sourceid); // Current
            // Map
            // door
            if (door.getTownPortal() != null) {

                applyto.getMap().spawnDoor(door);
                applyto.addDoor(door);

                MapleDoor townDoor = new MapleDoor(door); // Town door
                applyto.addDoor(townDoor);
                door.getTown().spawnDoor(townDoor);

                if (applyto.getParty() != null) { // update town doors
                    applyto.silentPartyUpdate();
                }
            } else {
                applyto.dropMessage(5, "You may not spawn a door because all doors in the town are taken.");
            }

        } else if (isMist()) {
            if (sourceid == 33111013 || sourceid == 33121016) {
                pos = null;
            }

            if (sourceid == 400011098 || sourceid == 400011100) {
                localDuration = cooltime;
            }

            if (sourceid == 35121052) {
                localDuration = 4000;
            }

            if (sourceid == 61121116) {
                localDuration = 750;
            }

            if (sourceid == 101120104) {
                localDuration = 9000;
            }

            if (sourceid == 400011058 || sourceid == 400031012) {
                localDuration = 10000;
            }

            if (sourceid == 61121105 && applyto.getSkillLevel(61120047) > 0) {
                localDuration += SkillFactory.getSkill(61120047).getEffect(1).getDuration();
            }

            if (sourceid == 400031001) {
                pos = applyfrom.getPosition();
            }
//
            if (((sourceid == 400040008 || sourceid == 400041008) && !primary) || sourceid == 2111003 || ((sourceid == 12121005 || sourceid == 400001017 || sourceid == 100001261 || sourceid == 151121041 || sourceid == 4221006 || sourceid == 162121043) && primary) || (sourceid != 12121005 && sourceid != 400001017 && sourceid != 100001261 && sourceid != 151121041 && sourceid != 4221006 && sourceid != 400040008 && sourceid != 400041008 && sourceid != 162121043)) {
                final Rectangle bounds = calculateBoundingBox(pos != null ? pos : applyfrom.getTruePosition(), (sourceid == 35121052 ? applyto.getKeyValue(2, "fa") == 1 : applyfrom.isFacingLeft()));
                final MapleMist mist = new MapleMist(bounds, applyfrom, this, localDuration, rltype);

                if (sourceid == 101120104) {
                    mist.setEndTime(9000);
                }

                if (sourceid == 151121041) {
                    mist.setDuration(1050);
                    mist.setEndTime(1050);
                }

                mist.setPosition(pos == null ? applyto.getTruePosition() : pos);
                if (sourceid == 4121015) {
                    applyfrom.getMap().removeMistByOwner(applyfrom, 4121015);
                }
                applyfrom.getMap().spawnMist(mist, false);

                if (applyfrom.isGM()) {
                    applyfrom.dropMessage(6, "spawn Mist : " + localDuration);
                }

                if (sourceid == 400051025) {
                    applyfrom.getMap().broadcastMessage(CField.ICBM(true, sourceid, calculateBoundingBox(pos, applyfrom.isFacingLeft())));
                }
            }

        } else if (isTimeLeap() && (System.currentTimeMillis() - applyto.lastTimeleapTime) >= duration) { // Time Leap
            for (MapleCoolDownValueHolder i : applyto.getCooldowns()) {
                if (i.skillId != 5121010 && !SkillFactory.getSkill(i.skillId).isHyper() && i.skillId / 10000 <= applyto.getJob()) {
                    applyto.lastTimeleapTime = System.currentTimeMillis();
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(i.skillId, 0));
                }
            }
        }
        if (rewardMeso != 0) {
            applyto.gainMeso(rewardMeso, false);
        }
        if (rewardItem != null && totalprob > 0) {
            for (Triple<Integer, Integer, Integer> reward : rewardItem) {
                if (MapleInventoryManipulator.checkSpace(applyto.getClient(), reward.left, reward.mid, "")
                        && reward.right > 0 && Randomizer.nextInt(totalprob) < reward.right) { // Total prob
                    if (GameConstants.getInventoryType(reward.left) == MapleInventoryType.EQUIP || GameConstants.getInventoryType(reward.left) == MapleInventoryType.DECORATION) {
                        final Item item = MapleItemInformationProvider.getInstance().getEquipById(reward.left);
                        item.setGMLog(
                                "Reward item (effect): " + sourceid + " on " + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.addbyItem(applyto.getClient(), item);
                    } else {
                        MapleInventoryManipulator.addById(applyto.getClient(), reward.left, reward.mid.shortValue(),
                                "Reward item (effect): " + sourceid + " on " + FileoutputUtil.CurrentReadable_Date());
                    }
                }
            }
        }
        return true;
    }

    public final boolean applyReturnScroll(final MapleCharacter applyto) {
        if (moveTo != -1) {
            if (applyto.getMap().getReturnMapId() != applyto.getMapId() || sourceid == 2031010 || sourceid == 2030021) {
                MapleMap target;
                if (moveTo == 999999999) {
                    target = applyto.getMap().getReturnMap();
                } else {
                    target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                    if (target.getId() / 10000000 != 60 && applyto.getMapId() / 10000000 != 61) {
                        if (target.getId() / 10000000 != 21 && applyto.getMapId() / 10000000 != 20) {
                            if (target.getId() / 10000000 != applyto.getMapId() / 10000000) {
                                return false;
                            }
                        }
                    }
                }
                applyto.changeMap(target, target.getPortal(0));
                return true;
            }
        }
        return false;
    }

    /*
     * public final void applyMonsterBuff(final MapleCharacter applyfrom) { final
     * Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(),
     * applyfrom.isFacingLeft()); final boolean pvp = applyfrom.inPVP(); final
     * MapleMapObjectType type = pvp ? MapleMapObjectType.PLAYER :
     * MapleMapObjectType.MONSTER; final List<MapleMapObject> affected = sourceid ==
     * 35111005 ?
     * applyfrom.getMap().getMapObjectsInRange(applyfrom.getTruePosition(),
     * Double.POSITIVE_INFINITY, Arrays.asList(type)) :
     * applyfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(type)); int i =
     * 0;
     * 
     * for (final MapleMapObject mo : affected) { // if (makeChanceResult()) { for
     * (Map.Entry<MonsterStatus, Integer> stat : getMonsterStati().entrySet()) { if
     * (!pvp) { MapleMonster mons = (MapleMonster) mo; if (sourceid == 35111005 &&
     * mons.getStats().isBoss()) { break; } mons.applyStatus(applyfrom.getClient(),
     * stat.getKey(), new MonsterStatusEffect(getSourceId(), isSubTime(sourceid) ?
     * getSubTime() : getDuration()), stat.getValue()); } } if (pvp && skill) {
     * MapleCharacter chr = (MapleCharacter) mo; handleExtraPVP(applyfrom, chr); }
     * // } i++; if (i >= mobCount && sourceid != 35111005) { break; } } }
     */
    public final Rectangle calculateBoundingBox(int skillid, int level, final Point posFrom, final boolean facingLeft) {
        return calculateBoundingBox(posFrom, facingLeft, SkillFactory.getSkill(skillid).getEffect(level).lt,
                SkillFactory.getSkill(skillid).getEffect(level).rb, range);
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, range);
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, int addedRange) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, range + addedRange);
    }

    public static Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, final Point lt, final Point rb, final int range) {
        if (lt == null || rb == null) {
            return new Rectangle((facingLeft ? (-200 - range) : 0) + posFrom.x, (-100 - range) + posFrom.y, 200 + range, 100 + range);
        }
        if (lt.getX() == 0 && lt.getY() == 0 && rb.getX() == 0 && rb.getY() == 0) {
            return new Rectangle((facingLeft ? -range : 0) + posFrom.x, -100 + posFrom.y, 200 + range, 200);
        }
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x - range, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x + range, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public final double getMaxDistanceSq() { // lt = infront of you, rb = behind you; not gonna distanceSq the two
        // points since this is in relative to player position which is (0,0)
        // and not both directions, just one
        final int maxX = Math.max(Math.abs(getLt() == null ? 0 : getLt().x), Math.abs(rb == null ? 0 : rb.x));
        final int maxY = Math.max(Math.abs(getLt() == null ? 0 : getLt().y), Math.abs(rb == null ? 0 : rb.y));
        return (maxX * maxX) + (maxY * maxY);
    }

    public final void setDuration(int d) {
        this.duration = d;
    }

    public final void silentApplyBuff(final MapleCharacter chr, final long starttime,
            final Map<MapleBuffStat, Pair<Integer, Integer>> statup, int cid) {

        for (Entry<MapleBuffStat, Pair<Integer, Integer>> statupz : statup.entrySet()) {
            long remainDuration = statupz.getValue().right - (System.currentTimeMillis() - starttime);
            if (remainDuration > 0) {
                final CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime, statupz.getKey());
                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                    cancelAction.run();
                }, remainDuration);

                chr.registerEffect(this, starttime,
                        schedule,
                        statupz, true, cid);

                final SummonMovementType summonMovementType = getSummonMovementType();
                if (summonMovementType != null) {
                    final MapleSummon tosummon = new MapleSummon(chr, this, chr.getTruePosition(), summonMovementType);
                    if (!tosummon.isPuppet()) {
                        chr.getMap().spawnSummon(tosummon, (int) remainDuration);
                        chr.addSummon(tosummon);
                        tosummon.addHP(x);
                    }
                }
            } else if (statupz.getValue().right == 0) {
                chr.registerEffect(this, starttime,
                        null, statupz, true, cid);

                final SummonMovementType summonMovementType = getSummonMovementType();
                if (summonMovementType != null) {
                    final MapleSummon tosummon = new MapleSummon(chr, this, chr.getTruePosition(), summonMovementType);
                    if (!tosummon.isPuppet()) {
                        chr.getMap().spawnSummon(tosummon, statupz.getValue().right);
                        chr.addSummon(tosummon);
                        tosummon.addHP(x);
                    }
                }
            }
        }
    }

    public final void applyKaiserCombo(MapleCharacter applyto, short combo) {
        final EnumMap<MapleBuffStat, Pair<Integer, Integer>> stat = new EnumMap<MapleBuffStat, Pair<Integer, Integer>>(MapleBuffStat.class);
        stat.put(MapleBuffStat.SmashStack, new Pair<>((int) combo, 0));
        applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(stat, null, applyto));
    }

    private final void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto,
            final boolean primary, final int newDuration, Point pos, boolean showEffect) {

        int localDuration = newDuration;

        if (pos == null) {
            pos = applyto.getTruePosition() == null ? applyto.getPosition() : applyto.getTruePosition();
        }

        Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
        boolean cancel = true, aftercancel = false, bufftimeR = true, isPetBuff = false, isStackBuff = false;

        for (MaplePet pet : applyto.getPets()) {
            if (pet != null) {
                if (pet.getBuffSkillId() == sourceid) {
                    isPetBuff = true;
                }
            }
        }

        switch (sourceid) {
            case 80002770: {
                localstatups.clear();
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(getLevel() * 3, 10000));
                break;
            }
            case 164121042: { // ?????? : ????????????
                if (!applyto.getBuffedValue(164121042)) {
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>((int) 1, 0));
                    localstatups.put(MapleBuffStat.DreamDowon, new Pair<>((int) 1, y * 1000));
                }
                break;
            }
            case 1121010: { // ????????????
                applyfrom.handleOrbconsume(1121010);
                break;
            }
            case 151111005: { // ????????????
                localstatups.put(MapleBuffStat.Novility, new Pair<>((int) level, localDuration));
                localstatups.put(MapleBuffStat.RwBarrier, new Pair<>(applyto.getNovilityBarrier(), localDuration));
                break;
            }
            case 1200014:
            case 1220010: { // ???????????? ??????
                if (applyfrom.getElementalCharge() > 0 && applyfrom.getElementalCharge() <= getZ()) {
                    localstatups.clear();
                    localstatups.put(MapleBuffStat.ElementalCharge, new Pair<>(x * applyto.getElementalCharge(), localDuration));
//                    localstatups.put(MapleBuffStat.Asr, new Pair<>(u, localDuration));
                }
                break;
            }
            case 1211010: { // ??????????????????
                bufftimeR = false;
                if (applyfrom.getListonation() < 5) {
                    applyfrom.setListonation(applyfrom.getListonation() + 1);
                }
                localstatups.clear();
                localstatups.put(MapleBuffStat.Listonation, new Pair<>(applyfrom.getListonation() * y, localDuration));
                break;
            }
            case 13100022: // ??????????????? ???
            case 13100027:
            case 13101022:
            case 13110022:
            case 13110027:
            case 13120003:
            case 13120010:
                int sk2 = 13101022;
                if (applyfrom.getSkillLevel(13120003) > 0) {
                    sk2 = 13120003;
                } else if (applyfrom.getSkillLevel(13110022) > 0) {
                    sk2 = 13110022;
                }
                sourceid = sk2;
                localDuration = 0;
                localstatups.put(MapleBuffStat.TryflingWarm, new Pair<>(1, localDuration));

                break;
            case 1211014: { // ???????????? ??????
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) indiePad, 0));
                localstatups.put(MapleBuffStat.IndiePddR, new Pair<>(z, 0));
                if (primary) {
                    localstatups.put(MapleBuffStat.KnightsAura, new Pair<>((int) y, 0));
                }
                break;
            }
            case 400031055: {
                applyto.setStartRepeatinCartrigeTime(System.currentTimeMillis());
                applyto.RepeatinCartrige = 7;
                localstatups.put(MapleBuffStat.RepeatinCartrige, new Pair<>((int) applyto.RepeatinCartrige, localDuration));
                break;
            }
            case 1301013: { // ?????????
                bufftimeR = false;
                if (applyfrom.getSkillLevel(1310013) > 0) {
                    applyfrom.setBeholderSkill1(1310013);
                } else {
                    applyfrom.setBeholderSkill1(1301013);
                }
                break;
            }
            case 1310016: { // ???????????? ??????
                if (applyfrom.getSkillLevel(1320044) > 0) {
                    localstatups.put(MapleBuffStat.EnhancedPad, new Pair<>((int) (epad + SkillFactory.getSkill(1320044).getEffect(1).getX()), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.EnhancedPad, new Pair<>((int) epad, localDuration));
                }
                localstatups.put(MapleBuffStat.EnhancedPdd, new Pair<>((int) epdd, localDuration));
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) indieCr, localDuration));
                break;
            }
            case 1320019: { // ??????????????????
                int data = z;
                if (applyfrom.getSkillLevel(1320047) > 0) {
                    data -= z * SkillFactory.getSkill(1320047).getEffect(1).getX() / 100;
                }
                applyfrom.setReinCarnation(data);
                break;
            }
            case 2121004:
            case 2221004:
            case 2321004: { // ????????????
                localstatups.clear();
                localstatups.put(MapleBuffStat.Stance, new Pair<>((int) prop, localDuration));
                localstatups.put(MapleBuffStat.Infinity, new Pair<>(1, localDuration));
                applyfrom.setInfinity((byte) 0);
                break;
            }
            case 2111011:
            case 2211012:
            case 2311012: {
                aftercancel = true;
                localstatups.put(MapleBuffStat.AntiMagicShell, new Pair<>(1, 0)); // 1.2.307
                applyfrom.setAntiMagicShell((byte) y);
                break;
            }
            case 2120010:
            case 2220010:
            case 2320011: { // ????????? ??????
                bufftimeR = false;
                localstatups.clear();
                localstatups.put(MapleBuffStat.ArcaneAim, new Pair<>(applyto.getArcaneAim(), 5000));
                break;
            }

            case 2201009: { // ?????? ??????
                if (!applyfrom.getBuffedValue(2201009)) {
                    localstatups.put(MapleBuffStat.ChillingStep, new Pair<>(1, 0));
                    bufftimeR = false;
                    aftercancel = true;
                } else {
                    return;
                }
                break;
            }
            case 2221054: { // ????????? ??????
                if (primary) {
                    localDuration = 0;
                    localstatups.put(MapleBuffStat.IceAura, new Pair<>(1, 0));
                } else {
                    localstatups.put(MapleBuffStat.IndieStance, new Pair<>(x, localDuration));
                }
                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>(z, localDuration));
                localstatups.put(MapleBuffStat.IndieTerR, new Pair<>(w, localDuration));
                break;
            }
            case 2300009:
            case 2320013: {
                aftercancel = true;
                localstatups.put(MapleBuffStat.BlessingAnsanble, new Pair<>((int) (applyto.getBlessingAnsanble() * x), 0));
                break;
            }
            case 80002927: // VIP ?????? : ?????????/??????  
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) padX, 0));
                localstatups.put(MapleBuffStat.IndieMad, new Pair<>((int) madX, 0));
                break;
            case 80002928: // VIP ?????? : ?????????/HP/MP
                localstatups.put(MapleBuffStat.IndieStr, new Pair<>((int) strX, 0));
                localstatups.put(MapleBuffStat.IndieDex, new Pair<>((int) dexX, 0));
                localstatups.put(MapleBuffStat.IndieLuk, new Pair<>((int) lukX, 0));
                localstatups.put(MapleBuffStat.IndieInt, new Pair<>((int) intX, 0));
                localstatups.put(MapleBuffStat.IndieHp, new Pair<>((int) mhpX, 0));
                localstatups.put(MapleBuffStat.IndieMp, new Pair<>((int) mmpX, 0));
                break;
            case 80002929: // VIP ?????? : ????????? ?????? 
                localstatups.put(MapleBuffStat.IgnoreMobPdpR, new Pair<>((int) ignoreMobpdpR, 0));
                break;
            case 80002930: // VIP ?????? : ?????? ????????? ????????? 
                // client?
                break;
            case 80002931: // VIP ?????? : ?????? ?????????
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>((int) bdR, 0));
                break;
            case 80002932: // VIP ?????? : ?????? ?????? 
                // playerstats calculate
                localstatups.put(MapleBuffStat.Event, new Pair<>(0, 0));
                break;
            case 80002933: // VIP ?????? : ???????????? ??????
                // playerstats calculate
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) cr, 0));
                break;
            case 80002934: // VIP ?????? : ????????? ??????
                // client? ida no data
                break;
            case 80002935: // VIP ?????? : ?????? ?????????
                // playerstats calculate
                localstatups.put(MapleBuffStat.ExpBuffRate, new Pair<>((int) expRPerM, 0));
                break;
            case 2301004: { // ?????????
                if (applyto.getBuffedValue(2321005)) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.AdvancedBless, 2321005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndiePad, 2321005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieMad, 2321005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieHp, 2321005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieMp, 2321005);
                }
                localstatups.put(MapleBuffStat.Bless, new Pair<>((int) level, localDuration));
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>(x, localDuration));
                localstatups.put(MapleBuffStat.IndieMad, new Pair<>(y, localDuration));
                localstatups.put(MapleBuffStat.IndiePdd, new Pair<>(z, localDuration));
                break;
            }

            case 2310013: { // ?????? ?????? ??? ????????? ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.CooltimeHolyMagicShell, new Pair<>(1, SkillFactory.getSkill(2311009).getEffect(level).getY() * 1000));
                break;
            }
            case 2311003: { // ?????? ??????
                if (applyfrom.getId() == applyto.getId() && applyfrom.getSkillLevel(2320046) > 0) {
                    localstatups.put(MapleBuffStat.HolySymbol, new Pair<>(x + SkillFactory.getSkill(2320046).getEffect(1).getY(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.HolySymbol, new Pair<>(x, localDuration));
                }
                if (applyfrom.getSkillLevel(2320047) > 0) {
                    localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) SkillFactory.getSkill(2320047).getEffect(1).asrR, localDuration));
                    localstatups.put(MapleBuffStat.IndieTerR, new Pair<>((int) SkillFactory.getSkill(2320047).getEffect(1).asrR, localDuration));
                }

                if (applyfrom.getSkillLevel(2320048) > 0) {
                    localstatups.put(MapleBuffStat.DropRate, new Pair<>((int) SkillFactory.getSkill(2320048).getEffect(1).v, localDuration));
                }
                break;
            }
            case 400051015: // ????????? ?????????
                if (!applyfrom.getBuffedValue(400051015)) {
                    applyfrom.setScrewTime(starttime);
                    localstatups.put(MapleBuffStat.DevilishPower, new Pair<>(damage / 10, localDuration));
                }
                break;
            case 2311009: { // ?????? ?????? ???
                localstatups.clear();
                byte data = (byte) x;

                if (applyfrom.getSkillLevel(2320043) > 0) {
                    data += SkillFactory.getSkill(2320043).getEffect(1).getX();
                }
                if (applyto.getBuffedEffect(MapleBuffStat.CooltimeHolyMagicShell) == null) {
                    applyto.setHolyMagicShell(data);
                    hpR = z / 100.0;
                    //SkillFactory.getSkill(2310013).getEffect(level).applyTo(applyfrom, applyto);
                    if (applyfrom.getSkillLevel(2320044) > 0) {
                        localDuration += SkillFactory.getSkill(2320044).getEffect(applyfrom.getSkillLevel(2320044)).getDuration();
                    }
                    localstatups.put(MapleBuffStat.HolyMagicShell, new Pair<>((int) applyto.getHolyMagicShell(), localDuration));
                }
                break;
            }
            case 2321001: { // ??????
                localstatups.clear();
                if (!primary) {
                    if (localDuration != duration) {
                        bufftimeR = false;
//                        localstatups.put(MapleBuffStat.KeyDownTimeIgnore, new Pair<>(1, localDuration));
                    }
                } else {
                    return;
                }
                break;
            }
            case 2321054: {
                if (applyto.getBuffedEffect(400021032) != null) {
                    long bufftime = applyto.getBuffLimit(400021032);
                    applyto.cancelEffect(applyto.getBuffedEffect(400021032), false, -1);
                    if (applyto.getBuffedEffect(400021052) != null) {
                        applyto.cancelEffect(applyto.getBuffedEffect(400021052), false, -1);
                    }
                    SkillFactory.getSkill(400021033).getEffect(applyto.getSkillLevel(400021032)).applyTo(applyto, false, (int) bufftime);
                }
                break;
            }
            case 400041010: {
                localDuration = 3000;
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, localDuration));
                break;
            }
            case 3121002: // ?????? ?????????
            case 3221002:
            case 3321022: {
                localstatups.clear();

                if (applyfrom.getSkillLevel(3120043) > 0) {
                    localDuration += SkillFactory.getSkill(3120043).getEffect(1).getDuration();
                }

                if (applyfrom.getSkillLevel(3220043) > 0) {
                    localDuration += SkillFactory.getSkill(3220043).getEffect(1).getDuration();
                }

                if (applyfrom.getSkillLevel(3320025) > 0) {
                    localDuration += SkillFactory.getSkill(3320025).getEffect(1).getDuration();
                }

                if (applyfrom.getSkillLevel(3120044) > 0) {
                    localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) SkillFactory.getSkill(3120044).getEffect(1).ignoreMobpdpR, localDuration));
                }

                if (applyfrom.getSkillLevel(3220044) > 0) {
                    localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) SkillFactory.getSkill(3220044).getEffect(1).ignoreMobpdpR, localDuration));
                }

                if (applyfrom.getSkillLevel(3320026) > 0) {
                    localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) SkillFactory.getSkill(3320026).getEffect(1).ignoreMobpdpR, localDuration));
                }

                localstatups.put(MapleBuffStat.SharpEyes, new Pair<>(((applyfrom.getSkillLevel(3120045) > 0 ? 5 : applyfrom.getSkillLevel(3220045) > 0 ? 5 : applyfrom.getSkillLevel(3320027) > 0 ? 5 : 0) + x << 8) + y, localDuration));
                break;
            }
            case 3311002:
            case 3311003:
            case 3321006:
            case 3321007: {
                showEffect = false;
                applyto.setBHGCCount(applyto.getBHGCCount() - 1);

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(applyto.getBHGCCount(), 0));

                applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, this, applyto));
                break;
            }
            case 3320008: {
                showEffect = false;
                applyto.curseBound = y;
                localstatups.put(MapleBuffStat.BonusAttack, new Pair<>(y, localDuration));
                break;
            }
            case 3321034: { // ?????? ????????????
                localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));

                applyto.energy = 1000; // max
                applyto.ancientGauge = 1000;

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.RelikGauge, new Pair<>(applyto.energy, 0));
                statups.put(MapleBuffStat.AncientGuidance, new Pair<>(-1, 0));

                applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, this, applyto));

                applyto.ancientGauge = 0;

                SkillFactory.getSkill(3310006).getEffect(applyto.getSkillLevel(3310006)).applyTo(applyto);
                break;
            }
            case 400051077: {
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, localDuration));
                break;
            }
            case 400041061: {
                applyto.setStartThrowBlastingTime(System.currentTimeMillis());
                applyto.ThrowBlasting = 47;
                break;
            }
            case 400051068: {
                localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
                break;
            }
            case 400011118: {
                if (!applyfrom.getBuffedValue(400011118)) {
                    localstatups.put(MapleBuffStat.DevilishPower, new Pair<>(6, localDuration));
                }
                break;
            }
            case 400011038: {// ????????? ?????????
                localstatups.clear();
                localstatups.put(MapleBuffStat.IndieJointAttack, new Pair<>(1, localDuration));
                break;
            }
            case 2321005: { // ??????????????? ?????????
                if (applyto.getBuffedValue(2301004)) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.Bless, 2301004);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndiePad, 2301004);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieMad, 2301004);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndiePdd, 2301004);
                }

                if (applyto.getBuffedValue(400001005)) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieMp, 400001005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndiePad, 400001005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieMad, 400001005);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieHp, 400001005);
                }

                localstatups.put(MapleBuffStat.AdvancedBless, new Pair<>((int) level, localDuration));

                if (applyfrom.getSkillLevel(2320051) > 0) {
                    localstatups.put(MapleBuffStat.IndieMp, new Pair<>(indieMmp + SkillFactory.getSkill(2320051).getEffect(1).indieMmp, localDuration));
                    localstatups.put(MapleBuffStat.IndieHp, new Pair<>(indieMhp + SkillFactory.getSkill(2320051).getEffect(1).indieMhp, localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndieMp, new Pair<>((int) indieMmp, localDuration));
                    localstatups.put(MapleBuffStat.IndieHp, new Pair<>((int) indieMhp, localDuration));
                }

                if (applyfrom.getSkillLevel(2320049) > 0) {
                    localstatups.put(MapleBuffStat.IndieMad, new Pair<>(y + SkillFactory.getSkill(2320049).getEffect(1).getX(), localDuration));
                    localstatups.put(MapleBuffStat.IndiePad, new Pair<>(x + SkillFactory.getSkill(2320049).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndieMad, new Pair<>(y, localDuration));
                    localstatups.put(MapleBuffStat.IndiePad, new Pair<>(x, localDuration));
                }

                break;
            }
            case 400031047:
            case 400031049:
            case 400031051: {
                localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
                break;
            }
            case 400031044: {
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.RoyalKnights, new Pair<>(1, localDuration));
                    SkillFactory.getSkill(sourceid).getEffect(level).applyTo(applyto, false);
                } else {
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000)); // duration 11640
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 3000));
                }
                break;
            }
            case 400011111: {
                if (applyto.getBuffedEffect(MapleBuffStat.IndieNotDamaged) == null) {
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 3000));
                }
                break;
            }
            case 2321052: { // 
                localstatups.clear();
                if (applyto.getBuffedEffect(MapleBuffStat.CooldownHeavensDoor) == null) {
                    bufftimeR = false;
                    localDuration = 0;
                    aftercancel = true;
                    localstatups.put(MapleBuffStat.HeavensDoor, new Pair<>(1, localDuration));
                    //SkillFactory.getSkill(2321055).getEffect(applyto.getSkillLevel(2321055)).applyTo(applyto);
                }
                break;
            }
            case 2321055: { // ????????? ??????
                cancel = false;
                localDuration = 60000;
                localstatups.put(MapleBuffStat.CooldownHeavensDoor, new Pair<>(1, localDuration));
                break;
            }
            case 3101009: { // ??????????????? ??????
                aftercancel = true;
                if (primary) {
                    applyto.setQuiverType((byte) 1);
                    if (applyto.getSkillLevel(3121016) > 0) {
                        applyto.getRestArrow()[0] = SkillFactory.getSkill(3121016).getEffect(applyto.getSkillLevel(3121016)).getY() * 10000;
                        applyto.getRestArrow()[1] = SkillFactory.getSkill(3121016).getEffect(applyto.getSkillLevel(3121016)).getY() * 100;
                        applyto.getRestArrow()[2] = SkillFactory.getSkill(3121016).getEffect(applyto.getSkillLevel(3121016)).getZ();
                    } else {
                        applyto.getRestArrow()[0] = 100000;
                        applyto.getRestArrow()[1] = 1000;
                        applyto.getRestArrow()[2] = 10;
                    }
                }
                switch (applyto.getQuiverType()) {
                    case 1:
                        localstatups.put(MapleBuffStat.QuiverCatridge, new Pair<>(applyto.getRestArrow()[0], 0));
                        break;
                    case 2:
                        localstatups.put(MapleBuffStat.QuiverCatridge, new Pair<>(applyto.getRestArrow()[1], 0));
                        break;
                    case 3:
                        localstatups.put(MapleBuffStat.QuiverCatridge, new Pair<>(applyto.getRestArrow()[2], 0));
                        break;
                }
                break;
            }
            case 27121052: {
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 2000));
                break;
            }
            case 3110001:
            case 3210001: { // ?????? ?????????
                bufftimeR = false;
                aftercancel = true;
                localDuration = 0;
                if (applyto.getMortalBlow() == x) {
                    applyto.setMortalBlow((byte) 0);
                } else {
                    applyto.setMortalBlow((byte) (applyto.getMortalBlow() + 1));
                }
                localstatups.put(MapleBuffStat.MortalBlow, new Pair<>((int) applyto.getMortalBlow(), localDuration));
                break;
            }
            case 3110012: { // ??????????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.BowMasterConcentration, new Pair<>((int) (applyto.getConcentration()), localDuration));
                break;
            }
            case 3210013: { // ????????? ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.PowerTransferGauge, new Pair<>(applyto.getBarrier(), localDuration));
                break;
            }
            case 4001003: { // ???????????????
                localstatups.put(MapleBuffStat.DarkSight, new Pair<>(x, localDuration));
                int[] skills = {4210015, 4330001};
                for (int skill : skills) {
                    if (applyto.getSkillLevel(skill) > 0) {
                        localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(5, localDuration));
                    }
                }
                if (applyto.getSkillLevel(4211016) > 0) {
                    applyto.addCooldown(4211016, System.currentTimeMillis(), cooltime);
                    applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(4211016, cooltime));
                }
                break;
            }
            case 4111009: // ????????? ?????????
            case 5201008: // ????????? ??????
            case 14111025: { // ????????? ?????????
                localDuration = duration;
                if (newDuration / 10000 == 207 && sourceid != 5201008) {
                    localstatups.put(MapleBuffStat.NoBulletConsume, new Pair<>(newDuration - 2070000 + 1, localDuration));
                } else if (newDuration / 10000 == 233 && sourceid == 5201008) {
                    localstatups.put(MapleBuffStat.NoBulletConsume, new Pair<>(newDuration - 2330000 + 1, localDuration));
                }
                break;
            }
            case 4200013:
            case 4220015: { // ???????????? ?????????
//                effect = false;
                aftercancel = true;
                localstatups.put(MapleBuffStat.CriticalGrowing, new Pair<>(applyto.criticalGrowing, 0));
                break;
            }
            case 4211016: { // ?????? ????????????
                applyto.addCooldown(4001003, System.currentTimeMillis(), 3000);
                applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(4001003, 3000));

                applyto.addCooldown(4211016, System.currentTimeMillis(), 3000);
                applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(4211016, 3000));

                if (applyto.getSkillLevel(4001003) > 0) {
                    SkillFactory.getSkill(4001003).getEffect(applyto.getSkillLevel(4001003)).applyTo(applyto, false);
                }
                break;
            }
            case 4221006: { // ?????????
                localstatups.clear();
                if (!primary) {
                    localstatups.put(MapleBuffStat.DamageDecreaseWithHP, new Pair<>(y, 1800));
                }
                break;
            }
            case 4221013: { // ?????? ?????????
                localstatups.clear();
                if (localDuration == 0) {
//                    effect = false;
                    aftercancel = true;
                    localstatups.put(MapleBuffStat.KillingPoint, new Pair<>(applyto.killingpoint, 0));
                } else {
                    localstatups.put(MapleBuffStat.IndiePad, new Pair<>(40 + applyto.killingpoint * kp, localDuration));

                    if (applyto.getCooldownLimit(400041039) > 0) {
                        applyto.changeCooldown(400041039, -500 * applyto.killingpoint);
                    }

                    applyto.killingpoint = 0;

                }
                break;
            }
            case 4221016: {
                if (applyto.getSkillLevel(400041025) > 0 && applyto.shadowerDebuffOid != 0) {
                    localDuration = 10000;
                    localstatups.put(MapleBuffStat.ShadowerDebuff, new Pair<>(applyto.shadowerDebuff, localDuration));
                }
                break;
            }
            case 4221054: { // ?????? ??? ??????
                bufftimeR = false;
                if (applyto.getFlip() < 5) {
                    applyto.setFlip((byte) (applyto.getFlip() + 1));
                }
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR * applyto.getFlip(), localDuration));
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>(x * applyto.getFlip(), localDuration));
                localstatups.put(MapleBuffStat.FlipTheCoin, new Pair<>((int) applyto.getFlip(), localDuration));
                break;
            }
            case 4331006: { // ?????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, localDuration));
                break;
            }
            case 4330009: { // ????????? ????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) indiePad, localDuration));
                break;
            }
            case 4341002: { // ????????? ???
                localstatups.clear();
                if (primary) {
                    bufftimeR = true;
                    localstatups.put(MapleBuffStat.FinalCut, new Pair<>(y, localDuration));
                    applyTo(applyfrom, false, 3000);
                }
                if (!primary && localDuration == 3000) {
                    Map<MapleBuffStat, Pair<Integer, Integer>> finalcut = new HashMap<>();
                    bufftimeR = false;
                    localDuration = 3000;
                    localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 3000));
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));
                }
                break;
            }
            case 5111007: // ?????? ?????????
            case 5120012: // ?????? ?????? ?????????
            case 5211007: // ?????? ?????????
            case 5220014: // ?????? ?????? ?????????
            case 5311005: // ?????? ?????????
            case 5320007: // ?????? ?????? ?????????
            case 35111013: // ?????? ?????????
            case 35120014: // ?????? ?????? ?????????
            case 400051001: // ?????? ?????????
            {
                boolean extra = false;
                if (applyfrom.getSkillLevel(5120044) > 0 || applyfrom.getSkillLevel(5220044) > 0) {
                    extra = true;
                }

                boolean third = applyto.getBuffedEffect(MapleBuffStat.SelectDice) != null;

                int thirddice = 0;

                if (third) {
                    thirddice = applyto.getBuffedValue(MapleBuffStat.SelectDice);
                }

                byte dice = (byte) Randomizer.rand(1, extra ? 7 : 6);

                byte doubledice = 1;

                if (isDoubleDice()) {
                    if (makeChanceResult()) {
                        doubledice = (byte) Randomizer.rand(2, extra ? 7 : 6);
                    }
                }

                if (applyto.isOneMoreChance()) {
                    applyto.setOneMoreChance(false);
                    dice = (byte) Randomizer.rand(4, extra ? 7 : 6);
                    if (doubledice > 1) {
                        doubledice = (byte) Randomizer.rand(4, extra ? 7 : 6);
                    }
                }

                applyto.setDice(thirddice * 100 + doubledice * 10 + dice);

                if (applyfrom.getSkillLevel(5120043) > 0 && applyto.getDice() == 11) {
                    applyfrom.setOneMoreChance(true);
                    if (SkillFactory.getSkill(5120043).getEffect(1).makeChanceResult()) {
                        applyfrom.removeCooldown(sourceid);
                        applyfrom.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, 0));
                    }
                }

                if (applyfrom.getSkillLevel(5220043) > 0 && applyto.getDice() == 11) {
                    applyfrom.setOneMoreChance(true);
                    if (SkillFactory.getSkill(5220043).getEffect(1).makeChanceResult()) {
                        applyfrom.removeCooldown(sourceid);
                        applyfrom.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, 0));
                    }
                }

                localstatups.put(MapleBuffStat.DiceRoll, new Pair<>((int) (thirddice * 100 + doubledice * 10 + dice), localDuration));

                int sk = third ? applyto.getBuffSource(MapleBuffStat.SelectDice) : sourceid;

                if (third) {
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto, 0, sk, -1, 1, false), false);
                    applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(applyto, 0, sk, -1, 1, true));
                }

                if (isDoubleDice() && doubledice > 0) {
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto, 0, sk, dice, -1, false), false);
                    applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(applyto, 0, sk, dice, -1, true));

                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto, 1, sk, doubledice, -1, false), false);
                    applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(applyto, 1, sk, doubledice, -1, true));
                    if (dice == 1 && doubledice == 1) {
                        applyto.dropMessage(5, "?????? ?????? ????????? ????????? [" + dice + "], [" + doubledice + "]?????? ?????? ????????? ????????? ?????? ???????????????.");
                        if (thirddice == 1) {
                            return;
                        }
                    } else if (dice == 1) {
                        applyto.dropMessage(5, "?????? ?????? ????????? ????????? [" + doubledice + "]??? ????????? ?????? ???????????????.");
                    } else if (doubledice == 1) {
                        applyto.dropMessage(5, "?????? ?????? ????????? ????????? [" + dice + "]??? ????????? ?????? ???????????????.");
                    } else {
                        applyto.dropMessage(5, "?????? ?????? ????????? ????????? [" + dice + "], [" + doubledice + "]??? ????????? ?????? ???????????????.");
                    }
                } else {
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto, 0, sk, dice, -1, false), false);
                    applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(applyto, 0, sk, dice, -1, true));
                    if (dice == 1) {
                        applyto.dropMessage(5, "?????? ????????? ????????? [" + dice + "]?????? ?????? ????????? ????????? ?????? ???????????????.");
                        if (thirddice == 1) {
                            return;
                        }
                    } else {
                        applyto.dropMessage(5, "?????? ????????? ????????? [" + dice + "]??? ????????? ?????? ???????????????.");
                    }
                }

                if (applyto.getBuffedEffect(MapleBuffStat.SelectDice) != null) {
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto, 1, sk, thirddice, 0, false), false);
                    applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(applyto, 1, sk, thirddice, 0, true));
                }

                applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto, 1, sk, -1, 2, false), false);
                applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(applyto, 1, sk, -1, 2, true));

                break;
            }

            case 5121052:
            case 5121055: { // ????????? ?????? ??????
                bufftimeR = false;
                if (applyto.getUnityofPower() < 4) {
                    applyto.setUnityofPower((byte) (applyto.getUnityofPower() + 1));
                }
                localstatups.put(MapleBuffStat.UnityOfPower, new Pair<>((int) applyto.getUnityofPower(), localDuration));
                break;
            }

            case 5121054: { // ??????????????????
                localstatups.put(MapleBuffStat.Stimulate, new Pair<>(1, duration));
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, duration));

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                MapleStatEffect energyCharge = applyto.getBuffedEffect(MapleBuffStat.EnergyCharged);
                if (energyCharge == null) {
                    if (applyto.getSkillLevel(5120018) > 0) {
                        SkillFactory.getSkill(5120018).getEffect(applyto.getSkillLevel(5120018)).applyTo(applyto, false);
                    } else if (applyto.getSkillLevel(5110014) > 0) {
                        SkillFactory.getSkill(5110014).getEffect(applyto.getSkillLevel(5110014)).applyTo(applyto, false);
                    } else {
                        SkillFactory.getSkill(5100015).getEffect(applyto.getSkillLevel(5100015)).applyTo(applyto, false);
                    }
                    energyCharge = applyto.getBuffedEffect(MapleBuffStat.EnergyCharged);
                }

                int max = energyCharge.getZ(); // ????????? ????????? ???????????? ?????? ?????? ???

                applyto.energy = max;
                applyto.energyCharge = true; // ????????? ??????????????? ????????????.
                energyCharge.setEnergyChargeActived(true);
                energyCharge.setEnergyChargeCooling(false);

                statups.put(MapleBuffStat.EnergyCharged, new Pair<>(applyto.energy, 0));

                applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, energyCharge, applyto));
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, statups, energyCharge), false);
                break;
            }

            case 5220019: { // ?????? ????????????
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>(w, localDuration));

                if (applyto.getBuffedValue(5210015)) {
                    localstatups.put(MapleBuffStat.EnrageCrDamMin, new Pair<>(z, localDuration));
                }

                if (applyto.getBuffedValue(5210016)) {
                    localstatups.put(MapleBuffStat.CriticalIncrease, new Pair<>(s, localDuration));
                }

                if (applyto.getBuffedValue(5210017)) {
                    localstatups.put(MapleBuffStat.IndieHpR, new Pair<>(x, localDuration));
                    localstatups.put(MapleBuffStat.IndieMpR, new Pair<>(x, localDuration));
                    localstatups.put(MapleBuffStat.IndieSpeed, new Pair<>(u, localDuration));
                }

                if (applyto.getBuffedValue(5210018)) {
                    localstatups.put(MapleBuffStat.DamageReduce, new Pair<>(y, localDuration));
                }

                break;
            }

            case 5221021: { // ??? ?????????
                if (applyto.getBuffedValue(5221021)) {
                    localstatups.put(MapleBuffStat.QuickDraw, new Pair<>(damR, localDuration));
                } else {
                    bufftimeR = false;
                    localstatups.put(MapleBuffStat.QuickDraw, new Pair<>(1, 0));
                }
                break;
            }
            case 5311004: { // ????????? ??????
                localstatups.put(MapleBuffStat.Roulette, new Pair<>(Randomizer.rand(1, 4), localDuration));
                break;
            }
            case 5310008: { // ?????? ????????? ????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.KeyDownTimeIgnore, new Pair<>(1, 15000));
                break;
            }
            case 5311002: { // ?????? ?????????
                localstatups.put(MapleBuffStat.IndieCD, new Pair<>(x, subTime));
                break;
            }
            case 11101022: { // ?????? ???
                aftercancel = applyto.getBuffedValue(11121005);
                localstatups.put(MapleBuffStat.PoseType, new Pair<>(1, 0));
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) indieCr, 0));
                localstatups.put(MapleBuffStat.Buckshot, new Pair<>(x, 0));
                break;
            }
            case 11111022: // ????????? ???
                aftercancel = applyto.getBuffedValue(11121005);
                localstatups.put(MapleBuffStat.PoseType, new Pair<>(2, 0));
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>((int) indieBooster, 0));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, 0));
                break;
            case 11121005: { // ????????? ??????
                localstatups.put(MapleBuffStat.GlimmeringTime, new Pair<>((int) 1, localDuration));
                if (applyto.getBuffedValue(11101022)) {
                    SkillFactory.getSkill(11121011).getEffect(level).applyBuffEffect(applyfrom, applyto, primary, localDuration, pos, false);
                } else if (applyto.getBuffedValue(11111022)) {
                    SkillFactory.getSkill(11121012).getEffect(level).applyBuffEffect(applyfrom, applyto, primary, localDuration, pos, false);
                }
                break;
            }
            case 11121011: // ????????? ?????? : ?????? ???
            case 11121012: { // ????????? ?????? : ????????? ???
//            	effect = false;
                localDuration = 0;
                applyto.lastPoseType = sourceid;
                if (sourceid == 11121011) {
                    localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) indieCr, localDuration));
                    localstatups.put(MapleBuffStat.Buckshot, new Pair<>(x, localDuration)); // 2
                } else if (sourceid == 11121012) {
                    localstatups.put(MapleBuffStat.IndieBooster, new Pair<>((int) indieBooster, localDuration));
                    localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, localDuration));
                }
                break;
            }
            case 11121014: { // ????????? ?????????
                showEffect = false;
                break;
            }
            case 12000022:
            case 12100026:
            case 12110024:
            case 12120007: { // ????????????: ?????????
                localstatups.put(MapleBuffStat.IndieMad, new Pair<>(x, localDuration));
                break;
            }
            case 12111023: { // ??? ?????????
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.FlareTrick, new Pair<>(y, localDuration));
                } else {
                    bufftimeR = false;
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, x * 1000));
                }
                break;
            }
            case 12121005: { // ?????? ??????
                localstatups.clear();
                if (!primary) {
                    //            	effect = false;
                    localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, localDuration));
                    localstatups.put(MapleBuffStat.IndieBooster, new Pair<>((int) indieBooster, localDuration));
                }
                break;
            }
            case 12121052: { // ???????????????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));
                break;
            }
            case 14110030: { // ???????????? ?????????
                localstatups.clear();
//            	effect = false;
                if (primary) {
                    localstatups.put(MapleBuffStat.ReviveOnce, new Pair<>(x, 0));
                } else {
                    bufftimeR = false;
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                    localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000));
                }
                break;
            }
            case 14120009: { // ????????? ???????????????
                int barrier = y;
                if (applyto.getSkillLevel(14120049) > 0) {
                    barrier += SkillFactory.getSkill(14120049).getEffect(1).getX();
                }
                if (applyto.siphonVitality == x) {
                    applyto.siphonVitality = 0;
                    localstatups.put(MapleBuffStat.Protective, new Pair<>(barrier, subTime));

                    if (applyto.getSkillLevel(14120050) > 0) {
                        localstatups.put(MapleBuffStat.IndiePad, new Pair<>(SkillFactory.getSkill(14120050).getEffect(1).getX(), subTime));
                    }

                    if (applyto.getSkillLevel(14120051) > 0) {
                        localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>(SkillFactory.getSkill(14120051).getEffect(1).getX(), subTime));
                    }
                } else {
                    applyto.siphonVitality++;
                    localstatups.put(MapleBuffStat.SiphonVitality, new Pair<>(applyto.siphonVitality, subTime));
                }

                break;
            }
            case 14121004: { // ????????? ?????????
                localstatups.put(MapleBuffStat.IndieStance, new Pair<>(100, x * 1000));
                break;
            }
            case 14121052: { // ????????????
                bufftimeR = false;
                localstatups.clear();
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3500));
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 3500));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(20, localDuration));
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>(100, localDuration));
                localstatups.put(MapleBuffStat.IndieStance, new Pair<>(100, localDuration));
                localstatups.put(MapleBuffStat.Dominion, new Pair<>(700, localDuration));
                break;
            }
            case 15111022: { // ??????
                if (applyto.lightning < 0) {
                    applyto.lightning = 0;
                }
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(applyto.lightning * y, localDuration));
                break;
            }
            case 15120003: { // ??????
                if (applyto.lightning < 0) {
                    applyto.lightning = 0;
                }
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(applyto.lightning * y, localDuration));
                break;
            }

            case 15121052: { // ?????? ??????
                localDuration = 2000;
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, localDuration));
                break;
            }
            case 15001022: { // ???????????? : ????????????
                bufftimeR = false;
                localstatups.clear();

                if (primary) {
                    localstatups.put(MapleBuffStat.CygnusElementSkill, new Pair<>(1, localDuration)); // ?????? ????????????
                } else {
                    localstatups.put(MapleBuffStat.IgnoreTargetDEF, new Pair<>(
                            (applyto.getSkillLevel(15121054) > 0 ? 9 : x) * applyto.lightning, y * 1000));
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((applyto.getSkillLevel(15121054) > 0 ? 5 : 0) * applyto.lightning, y * 1000));
                }
                break;
            }
            case 21100015:
            case 21120021: { // ?????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.AranSmashSwing, new Pair<>((0xF00 << 8) + 20, 4000));
                break;
            }
            case 21121017: { // ????????? 3???
                applyto.cancelEffectFromBuffStat(MapleBuffStat.BeyondNextAttackProb);
                break;
            }
            case 21110016: // ??????????????? ?????????
            case 21121058: { // ??????????????? ???????????????
                bufftimeR = false;
                applyto.setCombo((short) 1001);
                applyto.getClient().getSession().writeAndFlush(CField.aranCombo(1001));
                localDuration = 15000;
                if (applyfrom.getSkillLevel(21120064) > 0) {
                    localDuration += SkillFactory.getSkill(21120064).getEffect(1).getDuration();
                }

                localstatups.put(MapleBuffStat.AdrenalinBoost, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.AranBoostEndHunt, new Pair<>(1, localDuration)); // ??????
                break;
            }
            case 30021237: { // ?????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NewFlying, new Pair<>(1, x * 1000));
                break;
            }
            case 22171080: { // ????????? ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 10000));
                localstatups.put(MapleBuffStat.NewFlying, new Pair<>(1, 10000));
                localstatups.put(MapleBuffStat.RideVehicleExpire, new Pair<>(1939007, 10000));
                break;
            }
            case 23110004: { // ???????????? ??????
                int stack;
                if (applyto.getBuffedValue(23110004)) {
                    stack = applyto.getBuffedValue(MapleBuffStat.IgnisRore);
                } else {
                    stack = 0;
                }

                bufftimeR = false;
                localstatups.put(MapleBuffStat.IgnisRore, new Pair<>(Math.min(stack + 1, 10), subTime));
                break;
            }
            case 23111005: { // ?????? ??????

                if (applyfrom.getSkillLevel(23120046) > 0) {
                    localstatups.put(MapleBuffStat.DamAbsorbShield, new Pair<>(x + SkillFactory.getSkill(23120046).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.DamAbsorbShield, new Pair<>(x, localDuration));
                }

                if (applyfrom.getSkillLevel(23120047) > 0) {
                    localstatups.put(MapleBuffStat.Asr, new Pair<>(asrR + SkillFactory.getSkill(23120047).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.Asr, new Pair<>((int) asrR, localDuration));
                }

                if (applyfrom.getSkillLevel(23120048) > 0) {
                    localstatups.put(MapleBuffStat.Ter, new Pair<>(terR + SkillFactory.getSkill(23120048).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.Ter, new Pair<>((int) terR, localDuration));
                }
                break;
            }

            case 20031205: { // ?????? ????????????
//            	effect = false;
                bufftimeR = false;
                applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
                localstatups.put(MapleBuffStat.Invisible, new Pair<>(z * 100, localDuration));
                break;
            }
            case 20031209:
            case 20031210: { // ????????????
                bufftimeR = false;
                int judgement = Randomizer.rand(1, sourceid == 20031210 ? 5 : 2);
                if (judgement == 4) {
                    judgement++;
                }
                applyto.cancelEffect(this, false, -1);
                switch (judgement) {
                    case 1:
                        localstatups.put(MapleBuffStat.Judgement, new Pair<>(1, localDuration));
                        localstatups.put(MapleBuffStat.IndieCr, new Pair<>(v, localDuration));
                        break;
                    case 2:
                        localstatups.put(MapleBuffStat.Judgement, new Pair<>(2, localDuration));
                        localstatups.put(MapleBuffStat.DropRIncrease, new Pair<>(w, localDuration));
                        break;
                    case 3:
                        localstatups.put(MapleBuffStat.Judgement, new Pair<>(3, localDuration));
                        localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>(x, localDuration));
                        localstatups.put(MapleBuffStat.IndieTerR, new Pair<>(y, localDuration));
                        break;
                    case 4: // ?????????
                        break;
                    case 5:
                        localstatups.put(MapleBuffStat.Judgement, new Pair<>(5, localDuration));
                        localstatups.put(MapleBuffStat.DrainHp, new Pair<>(z, localDuration));
                        break;
                }
                applyto.setCardStack((byte) 1);
                applyto.getClient().getSession().writeAndFlush(CField.updateCardStack(false, applyto.getCardStack()));
                break;
            }
            case 20040216: { // ????????????
                localstatups.put(MapleBuffStat.Larkness, new Pair<>(applyto.getLuminusMorphUse(), localDuration));
                break;
            }
            case 20040217: { // ????????????
                localstatups.put(MapleBuffStat.Larkness, new Pair<>(applyto.getLuminusMorphUse(), localDuration));
                break;
            }
            case 20040219:
            case 20040220: { // ??????????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.Larkness, new Pair<>(applyto.getLuminusMorphUse(), localDuration + SkillFactory.getSkill(27120008).getEffect(applyto.getSkillLevel(27120008)).duration));
                break;
            }
            case 21000000: { // ?????? ????????????
                aftercancel = true;
                localstatups.put(MapleBuffStat.AranCombo, new Pair<>((int) applyto.getCombo(), 0));
                break;
            }
            case 24111002: { // ??? ?????? ????????????
                localstatups.clear();
                if (primary) {
                    aftercancel = true;
                    localstatups.put(MapleBuffStat.ReviveOnce, new Pair<>(x, 0));
                } else {
                    bufftimeR = false;
                    //  localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000)); ??? ???????
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, getY() * 1000));
                }
                break;
            }
            case 24111003: { // ???????????? ????????????
                if (applyfrom.getSkillLevel(24120049) > 0) {
                    localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) x + 5, localDuration));
                    localstatups.put(MapleBuffStat.IndieTerR, new Pair<>((int) y + SkillFactory.getSkill(24120049).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) x, localDuration));
                    localstatups.put(MapleBuffStat.IndieTerR, new Pair<>((int) y, localDuration));
                }
                if (applyfrom.getSkillLevel(24120050) > 0) {
                    localstatups.put(MapleBuffStat.IndieHpR, new Pair<>((int) indieMhpR + SkillFactory.getSkill(24120050).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndieHpR, new Pair<>((int) indieMhpR, localDuration));
                }

                if (applyfrom.getSkillLevel(24120051) > 0) {
                    localstatups.put(MapleBuffStat.IndieMpR, new Pair<>((int) indieMmpR + SkillFactory.getSkill(24120051).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndieMpR, new Pair<>((int) indieMmpR, localDuration));
                }
                break;
            }
            case 25111209: { // ?????? ??????
                bufftimeR = false;
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.ReviveOnce, new Pair<>(100, 0));
                } else {
                    localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, getY() * 1000));
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, getY() * 1000));
                }
            }
            case 25111211: {
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                break;
            }
            case 25121209: { // ?????? ??????
                applyto.setSpiritGuard(3);
                localstatups.put(MapleBuffStat.SpiritGuard, new Pair<>(3, localDuration));
                break;
            }
            case 27100003: { // ????????? ?????? ????????????
                localstatups.put(MapleBuffStat.BlessOfDarkness, new Pair<>((int) applyto.getBlessofDarkness(), 0));
                aftercancel = true;
                break;
            }
            case 27111004: { // ?????? ?????? ???
                localstatups.put(MapleBuffStat.AntiMagicShell, new Pair<>(3, 0));
                aftercancel = true;
                applyto.setAntiMagicShell((byte) 3);
                break;
            }
            case 27121005: { // ?????? ????????????
                if (applyto.stackbuff == 0) {
                    applyto.stackbuff++;
                }
                localstatups.put(MapleBuffStat.StackBuff, new Pair<>(applyto.stackbuff * 2, (int) (applyto.stackbuff == 1 ? localDuration : applyto.getBuffLimit(sourceid))));
                break;
            }
            case 27121054: { // ???????????????
                bufftimeR = false;
                if (applyto.getBuffedValue(20040219) || applyto.getBuffedValue(20040220)) {
                    applyto.dropMessage(5, "?????????????????? ??????????????? ???????????? ??? ????????????.");
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto));
                    applyto.removeCooldown(27121054);
                } else if (applyto.getBuffedValue(20040216)) {
                    SkillFactory.getSkill(20040220).getEffect(1).applyTo(applyto, false);
                } else {
                    SkillFactory.getSkill(20040219).getEffect(1).applyTo(applyto, false);
                }
                return;
            }
            case 30010230: { // ?????????
                aftercancel = true;
                localstatups.put(MapleBuffStat.OverloadCount, new Pair<>((int) applyto.getOverloadCount(), 0));
                break;
            }
            case 31011001: { // ????????? ????????????
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));
                applyto.cancelEffectFromBuffStat(MapleBuffStat.OverloadCount);
                break;
            }
            case 31111003: { // ????????? ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000));
                break;
            }
            case 31120046: {
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, localDuration * y / 100));
                localstatups.put(MapleBuffStat.IgnorePImmune, new Pair<>(1, localDuration * y / 100));
                break;
            }
            case 31121005: { // ??????????????????
                localstatups.put(MapleBuffStat.IndieHpR, new Pair<>((int) getIndieMhpR(), localDuration));
                localstatups.put(MapleBuffStat.DamR, new Pair<>(35, localDuration));
                localstatups.put(MapleBuffStat.DevilishPower, new Pair<>(damage / 10, localDuration));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 3000));
                if (applyfrom.getSkillLevel(31120046) > 0) {
                    SkillFactory.getSkill(31120046).getEffect(1).applyTo(applyfrom, localDuration);
                }
                break;
            }
            case 31211003: { // ???????????? ??????
                if (applyfrom.getSkillLevel(31220046) > 0) {
                    localstatups.put(MapleBuffStat.DamAbsorbShield, new Pair<>(x + SkillFactory.getSkill(31220046).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.DamAbsorbShield, new Pair<>(x, localDuration));
                }
                if (applyfrom.getSkillLevel(31220047) > 0) {
                    localstatups.put(MapleBuffStat.Asr, new Pair<>(y + SkillFactory.getSkill(31220047).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.Asr, new Pair<>(y, localDuration));
                }
                if (applyfrom.getSkillLevel(31220048) > 0) {
                    localstatups.put(MapleBuffStat.Ter, new Pair<>(z + SkillFactory.getSkill(31220048).getEffect(1).getX(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.Ter, new Pair<>(z, localDuration));
                }
                break;
            }
            case 32001014: // ??????
            case 32100010: // ?????? ????????????
            case 32110017: // ?????? ????????????2
            case 32120019: { // ?????? ????????????3
                aftercancel = true;
                localstatups.put(MapleBuffStat.BMageDeath, new Pair<>((int) applyto.getDeath(), localDuration));
                break;
            }
            case 32001016: { // ????????? ??????
                if (primary) {
                    localDuration = 0;
                    localstatups.put(MapleBuffStat.YellowAura, new Pair<>((int) level, 0));
                }
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>((int) -1, localDuration));
                localstatups.put(MapleBuffStat.IndieSpeed, new Pair<>((int) indieSpeed, (int) localDuration));
                break;
            }
            case 32101009: { // ????????? ??????
                localstatups.put(MapleBuffStat.DrainAura, new Pair<>((int) level, 0));
                break;
            }
            case 32111012: { // ?????? ??????
                if (primary) {
                    localDuration = 0;
                    localstatups.put(MapleBuffStat.BlueAura, new Pair<>((int) level, 0));
                }
                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) indieAsrR, localDuration));
                break;
            }
            case 32111016: { // ?????? ????????????
                localDuration = 0;
                localstatups.put(MapleBuffStat.DarkLighting, new Pair<>(1, localDuration));
                break;
            }
            case 32121017: { // ?????? ??????
                if (primary) {
                    localDuration = 0;
                    localstatups.put(MapleBuffStat.DarkAura, new Pair<>((int) level, 0));
                }
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, localDuration));
                break;
            }
            case 32121018: { // ????????? ??????
                localstatups.put(MapleBuffStat.DebuffAura, new Pair<>((int) level, localDuration));
                // ???????????? ??????
                break;
            }
            case 32120045: {
                if (applyfrom.getBuffedValue(MapleBuffStat.TeleportMastery) != null) {
                    applyfrom.cancelEffectFromBuffStat(MapleBuffStat.TeleportMastery);
                }
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, w * 1000));
                break;
            }
            case 33001001: { // ????????? ?????????
                applyto.cancelEffectFromBuffStat(MapleBuffStat.JaguarSummoned);
                break;
            }

            case 33111007: { // ????????? ???
                localstatups.put(MapleBuffStat.Speed, new Pair<>(x, localDuration));
                if (applyfrom.getSkillLevel(33120043) > 0) {
                    localstatups.put(MapleBuffStat.BeastFormDamage, new Pair<>(z + SkillFactory.getSkill(33120043).getEffect(1).getZ(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.BeastFormDamage, new Pair<>(z, localDuration));
                }

                if (applyfrom.getSkillLevel(33120044) > 0) {
                    localstatups.put(MapleBuffStat.IndieHpR, new Pair<>((int) SkillFactory.getSkill(33120044).getEffect(1).mhpR, localDuration));
                }

                if (applyfrom.getSkillLevel(33120045) > 0) {
                    localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-w - SkillFactory.getSkill(33120045).getEffect(1).getW(), localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-w, localDuration));
                }
                break;
            }
            case 33110014: { // ????????? ??????
                int size = 0;
                for (String str : applyto.getInfoQuest(23008).split(";")) {
                    if (str.contains("=1")) {
                        size++;
                    }
                }
                localstatups.put(MapleBuffStat.JaguarCount, new Pair<>(size * (y << 8) + z * size, 0));
                break;
            }
            case 35001002: { // ???????????? : ??????
                if (applyto.getBuffedValue(35111003)) {
                    applyto.cancelEffect(SkillFactory.getSkill(35111003).getEffect(1), false, -1);
                }
                break;
            }
            case 35111002: {
                bufftimeR = false;
                if (applyto.getCooldownLimit(35111002) > 0) {
                    localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
                }
                break;
            }
            case 35111003: { // ???????????? : ??????
                if (applyto.getBuffedValue(35001002)) {
                    applyto.cancelEffect(SkillFactory.getSkill(35001002).getEffect(1), false, -1);
                }
                break;
            }
            case 35120002: { // ????????? ????????? ??????
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
                } else {
//            		effect = false;

                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(z, localDuration));
                }
                break;
            }
            case 36111003: { // ??????????????? ????????????
                if (!primary) {
                    applyto.stackbuff--;
                } else {
                    applyto.stackbuff = x;
                }
                localstatups.put(MapleBuffStat.DamAbsorbShield, new Pair<>(z, primary ? duration : (int) applyto.getBuffLimit(sourceid)));
                localstatups.put(MapleBuffStat.StackBuff, new Pair<>(x, primary ? duration : (int) applyto.getBuffLimit(sourceid)));
                break;
            }
            case 36121052: { // ???????????? ???????????????
                localstatups.clear();
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(w, y * 1000));
                break;
            }
            case 36121054: { // ???????????? ???????????????
                bufftimeR = false;
                applyto.updateXenonSurplus((short) 20, SkillFactory.getSkill(sourceid));
                localstatups.put(MapleBuffStat.AmaranthGenerator, new Pair<>(1, localDuration));
                break;
            }
            case 37000006: { // ??????????????? ??????
                aftercancel = true;
                //applyto.setBarrier((int) Math.min(applyto.getBarrier() + localDuration, applyto.getStat().getCurrentMaxHp()));
                localstatups.put(MapleBuffStat.RwBarrier, new Pair<>(applyto.getBarrier(), 0));
                break;
            }
            case 37000013:
            case 37000012:
            case 37000011:
            case 37001002: { // ????????? ?????? ??????
                applyto.Cylinder = 6;
                // applyto.Bullet = 0;
                localstatups.put(MapleBuffStat.RWOverHeat, new Pair<>(1, applyto.getBuffedEffect(MapleBuffStat.RWMaximizeCannon) != null ? 1000 : 7000));
                break;
            }
            case 37100002: // ??????
            case 37110004: { // ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.RWMovingEvar, new Pair<>(x, 1500));
                break;
            }
            case 37110009: { // ??????????????? ???????????? I
                if (applyto.combinationBuff < x) {
                    applyto.combinationBuff++;
                }
                localstatups.put(MapleBuffStat.RWCombination, new Pair<>(applyto.combinationBuff, localDuration));
                if (applyto.combinationBuff >= z) {
                    localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, localDuration));
                }
                break;
            }
            case 37120012: { // ??????????????? ???????????? II
                if (applyto.combinationBuff < x) {
                    applyto.combinationBuff++;
                }
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>(q * applyto.combinationBuff, localDuration));
                localstatups.put(MapleBuffStat.RWCombination, new Pair<>(applyto.combinationBuff, localDuration));
                if (applyto.combinationBuff >= z) {
                    localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, localDuration));
                }
                break;
            }
            case 37121005: { // ?????? ???????????????
                applyto.addHP((x * applyto.getStat().getCurrentMaxHp() / 100) + applyto.getBarrier());
                applyto.cancelEffectFromBuffStat(MapleBuffStat.RwBarrier);
                localstatups.put(MapleBuffStat.RWBarrierHeal, new Pair<>(1, localDuration));
                break;
            }
            case 37121055: { // ????????? ????????? ??????
                localstatups.put(MapleBuffStat.RwMagnumBlow, new Pair<>(1, localDuration));
                break;
            }
            case 37121056: { // ????????? ????????? ??????
                localstatups.put(MapleBuffStat.RwMagnumBlow, new Pair<>(2, localDuration));
                break;
            }
            case 37121057: { // ????????? ????????? ??????
                localstatups.put(MapleBuffStat.RwMagnumBlow, new Pair<>(3, localDuration));
                break;
            }
            case 37121058: { // ????????? ????????? ??????
                localstatups.put(MapleBuffStat.RwMagnumBlow, new Pair<>(4, localDuration));
                break;
            }
            case 37120059: { // ????????? ????????? ??????
                localstatups.put(MapleBuffStat.RwMagnumBlow, new Pair<>(4, localDuration));
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 2000));
                break;
            }
            case 51001005: { // ?????? ?????? ??????
                int pad = 0;
                switch (applyto.getRoyalStack()) {
                    case 1:
                        pad = dotTime / 1000;
                        break;
                    case 2:
                        pad = dotInterval / 1000;
                        break;
                    case 3:
                        pad = range;
                        break;
                    case 4:
                        pad = 30; // w2
                        break;
                    case 5:
                        pad = 45; // u
                        break;
                }
                bufftimeR = false;
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.IndiePad, new Pair<>(pad, x * 1000));
                    localstatups.put(MapleBuffStat.RoyalGuardState, new Pair<>((int) applyto.getRoyalStack(), x * 1000));
                } else {
                    localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) (localDuration * 0.5), x * 1000));
                    localstatups.put(MapleBuffStat.IndieMad, new Pair<>((int) (localDuration * 0.5), x * 1000));
                }

                if (primary) {
                    if (applyto.getParty() != null) {
                        for (MaplePartyCharacter pc : applyto.getParty().getMembers()) {
                            MapleCharacter member = applyto.getClient().getChannelServer().getPlayerStorage().getCharacterById(pc.getId());
                            if (member != null && member.getBuffedValue(51111008) && member.getId() != applyto.getId()) {
                                this.applyTo(member, false, pad);
                            }
                        }
                    }
                }
                break;
            }
            case 51001006:
            case 51001007:
            case 51001008:
            case 51001009:
            case 51001010: { // ?????? ?????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.RoyalGuardPrepare, new Pair<>(1, 1000));
                break;
            }
            case 51001011:
            case 51001012:
            case 51001013: { // ????????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 4000));
                break;
            }
            case 12120013: // ????????? ?????? ?????????
            case 12120014: {
                localstatups.clear();
                localstatups.put(MapleBuffStat.IgnoreTargetDEF, new Pair<>(y, localDuration));
                break;
            }
            case 51111004: { // ?????? ?????????
                int asr = y;
                int ter = z;
                if (!primary) {
                    asr = (int) (asr * 0.2);
                    ter = (int) (ter * 0.2);
                }

                int dfr = x;

                if (applyfrom.getSkillLevel(51120044) > 0 && primary) {
                    dfr += SkillFactory.getSkill(51120044).getEffect(1).getX();
                }

                if (!primary) {
                    dfr = (int) (dfr * 0.3);
                }

                if (applyfrom.getSkillLevel(51120043) > 0) {
                    localDuration += SkillFactory.getSkill(51120043).getEffect(1).getDuration();
                }

                if (applyfrom.getSkillLevel(51120045) > 0) {
                    int data = SkillFactory.getSkill(51120045).getEffect(1).getY();
                    ter += data;
                }

                localstatups.put(MapleBuffStat.Ter, new Pair<>(ter, localDuration));
                localstatups.put(MapleBuffStat.Asr, new Pair<>(asr, localDuration));
                localstatups.put(MapleBuffStat.IncDefenseR, new Pair<>(dfr, localDuration));
                if (primary) {
                    if (applyto.getParty() != null) {
                        for (MaplePartyCharacter pc : applyto.getParty().getMembers()) {
                            MapleCharacter member = applyto.getClient().getChannelServer().getPlayerStorage().getCharacterById(pc.getId());
                            if (member != null && member.getBuffedValue(51111008) && member.getId() != applyto.getId()) {
                                this.applyTo(member, false);
                            }
                        }
                    }
                }
                break;
            }
            case 51110009: { // ??????????????? ?????? ?????? ??????
                int pad = 0;
                switch (applyto.getRoyalStack()) {
                    case 4:
                        pad = getW2();
                        break;
                    case 5:
                        pad = v;
                        break;
                }
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>(pad, localDuration));
                localstatups.put(MapleBuffStat.RoyalGuardState, new Pair<>((int) applyto.getRoyalStack(), localDuration));
                break;
            }
            case 51111008: { // ?????? ??????
                if (applyfrom.getId() == applyto.getId()) {
                    localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(
                            indieDamR * (applyto.getParty() == null ? 1 : applyto.getParty().getMembers().size()), 0));
                }
                localstatups.put(MapleBuffStat.MichaelSoulLink, new Pair<>(1, 0));
                break;
            }
            case 61111008: // ????????? ???????????????
            case 61120008: // ????????? ???????????????
            case 61121053: { // ????????? ?????????
                applyto.maxKaiserCombo();
                if (applyto.getBuffedValue(MapleBuffStat.StopForceAtominfo) != null) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.StopForceAtominfo, -1);
                    if (sourceid == 61120008 || sourceid == 61121053) {
                        SkillFactory.getSkill(61121217).getEffect(applyto.getSkillLevel(61120007)).applyTo(applyto, false);
                    } else {
                        SkillFactory.getSkill(61110211).getEffect(applyto.getSkillLevel(61101002)).applyTo(applyto, false);
                    }
                }
                break;
            }
            case 61121052: { // ???????????????
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                break;
            }
            case 61121009: { // ???????????? ??????
                if (applyfrom.getId() == applyto.getId()) {
                    localstatups.put(MapleBuffStat.RoburstArmor, new Pair<>(v, localDuration));
                } else {
                    localstatups.put(MapleBuffStat.RoburstArmor, new Pair<>(w, localDuration));
                }
                break;
            }

            case 61121054: { // ???????????? ?????? ?????????
                bufftimeR = false;

                for (MapleCoolDownValueHolder i : applyto.getCooldowns()) {
                    if (i.skillId != 61121054 && !SkillFactory.getSkill(i.skillId).isHyper() && GameConstants.isKaiser(i.skillId / 10000)) {
                        applyto.removeCooldown(i.skillId);
                        applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(i.skillId, 0));
                    }
                }

                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) indiePad, localDuration));
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, localDuration));
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IgnorePImmune, new Pair<>(1, localDuration));
                break;
            }
            case 64100004:
            case 64110005:
            case 64120006: { // ?????? ???????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.WeaponVariety, new Pair<>(applyto.getWeaponChanges().size(), localDuration));
                List<Triple<Integer, Integer, Integer>> finalMobList = new ArrayList<>();

                if (System.currentTimeMillis() - applyto.lastBonusAttckTime > 500) {
                    applyto.lastBonusAttckTime = System.currentTimeMillis();
                    applyto.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(sourceid == 64120006 ? 64121020 : sourceid == 64110005 ? 64111013 : 64101009, finalMobList, true, 0));
                    if (applyto.VarietyFinale < 4) {
                        applyto.VarietyFinale++;
                    }
                    if (applyto.VarietyFinale == 4 && applyto.VarietyFinaleCount > 0) {
                        applyto.VarietyFinale = 0;
                        applyto.VarietyFinaleCount--;
                        MapleStatEffect effect = SkillFactory.getSkill(400041074).getEffect(applyto.getSkillLevel(400041074));
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(applyto.VarietyFinaleCount, 0));
                        applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, applyto));
                        applyto.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400041074, finalMobList, true, 0));
                    }
                }
                break;
            }
            case 64121001: { // ????????????: ???????????????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 10000));
                break;
            }
            case 65101002: { // ?????? ????????????
                if (primary) {
                    applyto.setBarrier(1000);
                }
                localstatups.put(MapleBuffStat.PowerTransferGauge, new Pair<>(applyto.getBarrier(), localDuration));
                break;
            }
            case 65111004: { // ????????? ?????????
                localstatups.put(MapleBuffStat.Stance, new Pair<>((int) prop, localDuration));
                break;
            }
            case 65120006: { // ????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.Affinity, new Pair<>(1, 5000));
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) y, 5000));
                break;
            }
            /*            case 65121101: { // ????????????
             if (applyto.getTrinity() < y) {
             applyto.setTrinity(applyto.getTrinity() + 1);
             }
            	
             localstatups.put(MapleBuffStat.Trinity, new Pair<>(applyto.getTrinity());
             break;
             }*/
            case 80000268:
            case 150000017: { // ????????? ??????
                bufftimeR = false;
//            	effect = false;
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) y * applyto.FlowofFight, localDuration));
                localstatups.put(MapleBuffStat.FlowOfFight, new Pair<>(applyto.FlowofFight, localDuration));
                break;
            }
            case 150010241:
            case 80000514: { // ??????
                bufftimeR = false;
//            	effect = false;
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) y * applyto.LinkofArk, localDuration));
                localstatups.put(MapleBuffStat.LinkOfArk, new Pair<>(applyto.LinkofArk, localDuration));
                break;
            }
            case 80000329: { // ????????? ?????? ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, level * 1000));
                break;
            }
            case 80001428: { // ????????? ??? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>(20, localDuration));
                localstatups.put(MapleBuffStat.IndieTerR, new Pair<>(20, localDuration));
                localstatups.put(MapleBuffStat.IndieStance, new Pair<>(100, localDuration));
                localstatups.put(MapleBuffStat.DotHealHPPerSecond, new Pair<>((int) (applyto.getStat().getCurrentMaxHp() / 10), localDuration)); // ?
                localstatups.put(MapleBuffStat.DotHealMPPerSecond, new Pair<>((int) (applyto.getStat().getCurrentMaxMp(applyto) / 10), localDuration)); // ?
                break;
            }
            case 80002888: // ????????? ???
            case 80002889:// ????????? ???
            {
                localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
                break;
            }
            case 80001462: { // ??????????????? ???
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(applyto.getStat().critical_rate * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 80001463: { // ??????????????? ???
                bufftimeR = false;
                int value = 0;
                if (GameConstants.isXenon(applyto.getJob())) {
                    value = applyto.getStat().getStr() + applyto.getStat().getDex() + applyto.getStat().getLuk();
                } else if (GameConstants.isDemonAvenger(applyto.getJob())) {
                    value = (int) applyto.getStat().getCurrentMaxHp();
                } else if (GameConstants.isWarrior(applyto.getJob())) {
                    value = applyto.getStat().getStr();
                } else if (GameConstants.isMagician(applyto.getJob())) {
                    value = applyto.getStat().getInt();
                } else if (GameConstants.isArcher(applyto.getJob()) || GameConstants.isCaptain(applyto.getJob()) || GameConstants.isMechanic(applyto.getJob()) || GameConstants.isAngelicBuster(applyto.getJob())) {
                    value = applyto.getStat().getDex();
                } else if (GameConstants.isThief(applyto.getJob())) {
                    value = applyto.getStat().getLuk();
                } else if (GameConstants.isPirate(applyto.getJob())) {
                    value = applyto.getStat().getStr();
                }
                localstatups.put(MapleBuffStat.IndieCD, new Pair<>(value * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 80001464: { // ?????????????????? ???
                bufftimeR = false;
                int value = 0;
                if (GameConstants.isXenon(applyto.getJob())) {
                    value = applyto.getStat().getStr() + applyto.getStat().getDex() + applyto.getStat().getLuk();
                } else if (GameConstants.isDemonAvenger(applyto.getJob())) {
                    value = (int) applyto.getStat().getCurrentMaxHp();
                } else if (GameConstants.isWarrior(applyto.getJob())) {
                    value = applyto.getStat().getStr();
                } else if (GameConstants.isMagician(applyto.getJob())) {
                    value = applyto.getStat().getInt();
                } else if (GameConstants.isArcher(applyto.getJob()) || GameConstants.isCaptain(applyto.getJob()) || GameConstants.isMechanic(applyto.getJob()) || GameConstants.isAngelicBuster(applyto.getJob())) {
                    value = applyto.getStat().getDex();
                } else if (GameConstants.isThief(applyto.getJob())) {
                    value = applyto.getStat().getLuk();
                } else if (GameConstants.isPirate(applyto.getJob())) {
                    value = applyto.getStat().getStr();
                }
                localstatups.put(MapleBuffStat.Stance, new Pair<>(value * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 80001465: { // ??? ?????? ???
                bufftimeR = false;
                int value = x * (applyto.getStat().getStr() + applyto.getStat().getDex() + applyto.getStat().getInt() + applyto.getStat().getLuk()) / 100;

                if (GameConstants.isXenon(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieStr, new Pair<>(value / 3, localDuration)); // ???????????? ??????
                    localstatups.put(MapleBuffStat.IndieDex, new Pair<>(value / 3, localDuration)); // ???????????? ??????
                    localstatups.put(MapleBuffStat.IndieLuk, new Pair<>(value / 3, localDuration)); // ???????????? ??????
                } else if (GameConstants.isDemonAvenger(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieHp, new Pair<>(value, localDuration)); // ???????????? ??????
                } else if (GameConstants.isWarrior(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieStr, new Pair<>(value, localDuration)); // ???????????? ??????
                } else if (GameConstants.isMagician(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieInt, new Pair<>(value, localDuration)); // ???????????? ??????
                } else if (GameConstants.isArcher(applyto.getJob()) || GameConstants.isCaptain(applyto.getJob()) || GameConstants.isMechanic(applyto.getJob()) || GameConstants.isAngelicBuster(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieDex, new Pair<>(value, localDuration)); // ???????????? ??????
                } else if (GameConstants.isThief(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieLuk, new Pair<>(value, localDuration)); // ???????????? ??????
                } else if (GameConstants.isPirate(applyto.getJob())) {
                    localstatups.put(MapleBuffStat.IndieStr, new Pair<>(value, localDuration)); // ???????????? ??????
                }
                break;
            }
            case 80001466: { // ???????????? - S???
                localstatups.put(MapleBuffStat.IndieStr, new Pair<>((applyto.getLevel() + 1) * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 80001467: { // ???????????? - D???
                localstatups.put(MapleBuffStat.IndieDex, new Pair<>((applyto.getLevel() + 1) * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 400011047: { //???????????? ??????
                if (applyto.getBuffedValue(MapleBuffStat.DarknessAura) == null) {
                    applyto.setDarknessAura(0);
                    localstatups.put(MapleBuffStat.DarknessAura, new Pair<>(u, localDuration));
                    applyto.setStartDarknessAuraTime(System.currentTimeMillis());
                } else {
                    if (applyto.getStat().getHp() < applyto.getStat().getMaxHp()) {
                        applyto.addHP((int) (applyto.getStat().getMaxHp() * 0.06));
                    }
                    if (applyto.getBuffedValue(1301007)) {
                        localstatups.put(MapleBuffStat.IndieDarknessAura, new Pair<>((int) (applyto.getStat().getMaxHp() * (getY() * 0.01)), localDuration));
                    } else {
                        if (applyto.getStat().getHp() == applyto.getStat().getMaxHp()) {
                            localstatups.put(MapleBuffStat.IndieDarknessAura, new Pair<>((int) (applyto.getStat().getMaxHp() * 0.06), localDuration));
                        }
                    }
                    if (applyto.getDarknessAura() < 15) {
                        localstatups.put(MapleBuffStat.DarknessAura, new Pair<>(u, localDuration));
                        long time = System.currentTimeMillis() - applyto.getStartDarknessAuraTime();
                        localDuration = getDuration() - (int) time;
                        applyto.addDarknessAura(1);
                    }
                }
                break;
            }
            case 80001468: { // ???????????? - I???
                localstatups.put(MapleBuffStat.IndieInt, new Pair<>((applyto.getLevel() + 1) * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 80001469: { // ???????????? - L???
                localstatups.put(MapleBuffStat.IndieLuk, new Pair<>((applyto.getLevel() + 1) * x / 100, localDuration)); // ???????????? ??????
                break;
            }
            case 80001470: { // ???????????? - S???
                Equip eq = (Equip) applyto.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (eq == null) {
                    System.out.println("?????? ?????? ????????? ????????? ??? ?????? " + applyto.getName());
                } else {
                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(eq.getItemId());
                    localstatups.put(MapleBuffStat.IndieStr, new Pair<>(eq.getTotalWatk() * x / 100, localDuration)); // ??????
                }
                break;
            }
            case 80001471: { // ???????????? - D???
                Equip eq = (Equip) applyto.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (eq == null) {
                    System.out.println("?????? ?????? ????????? ????????? ??? " + applyto.getName());
                } else {
                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(eq.getItemId());
                    localstatups.put(MapleBuffStat.IndieDex, new Pair<>(eq.getTotalWatk() * x / 100, localDuration)); // ??????
                }
                break;
            }
            case 80001472: { // ???????????? - I???
                Equip eq = (Equip) applyto.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (eq == null) {
                    System.out.println("?????? ?????? ????????? ????????? ??? " + applyto.getName());
                } else {
                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(eq.getItemId());
                    localstatups.put(MapleBuffStat.IndieInt, new Pair<>(eq.getTotalMatk() * x / 100, localDuration)); // ??????
                }
                break;
            }
            case 80001473: { // ???????????? - L???
                Equip eq = (Equip) applyto.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (eq == null) {
                    System.out.println("?????? ?????? ????????? ????????? ??? ?????? " + applyto.getName());
                } else {
                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(eq.getItemId());
                    localstatups.put(MapleBuffStat.IndieLuk, new Pair<>(eq.getTotalWatk() * x / 100, localDuration)); // ??????
                }
                break;
            }
            case 80001474: { // ???????????? ???
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-2, localDuration));
                break;
            }
            case 80001475: { // ???????????? ???
                localstatups.put(MapleBuffStat.IgnoreAllCounter, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IgnoreAllImmune, new Pair<>(1, localDuration));
                break;
            }
            case 80001476: { // ??????????????? ???
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) indieIgnoreMobpdpR, localDuration));
                localstatups.put(MapleBuffStat.IndiePddR, new Pair<>((int) indiePddR, localDuration));
                break;
            }
            case 80001477: { // ??????????????? ???
                localstatups.put(MapleBuffStat.ReflectDamR, new Pair<>(x, localDuration));
                break;
            }
            case 80001479: { // ?????????????????? ???
                localstatups.put(MapleBuffStat.IndiePadR, new Pair<>((int) indiePadR, localDuration));
                localstatups.put(MapleBuffStat.IndieMadR, new Pair<>((int) indieMadR, localDuration));
                break;
            }
            case 80001535: {
                aftercancel = true;
                localDuration = 0;
                localstatups.put(MapleBuffStat.IndieExp, new Pair<>(20, 0));
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>(20, 0));
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(20, 0));
                break;
            }
            case 80001536: {
                aftercancel = true;
                localDuration = 0;
                localstatups.put(MapleBuffStat.IndiePadR, new Pair<>(20, 0));
                localstatups.put(MapleBuffStat.IndieMadR, new Pair<>(20, 0));
                localstatups.put(MapleBuffStat.IndieExp, new Pair<>(60, 0));
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>(60, 0));
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(60, 0));
                break;
            }
            case 80001537: {
                aftercancel = true;
                localDuration = 0;
                localstatups.put(MapleBuffStat.IndiePadR, new Pair<>(30, 0));
                localstatups.put(MapleBuffStat.IndieMadR, new Pair<>(30, 0));
                localstatups.put(MapleBuffStat.IndieExp, new Pair<>(80, 0));
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>(80, 0));
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(80, 0));
                break;
            }
            case 80001538: {
                aftercancel = true;
                localDuration = 0;
                localstatups.put(MapleBuffStat.IndiePadR, new Pair<>(50, 0));
                localstatups.put(MapleBuffStat.IndieMadR, new Pair<>(50, 0));
                localstatups.put(MapleBuffStat.IndieExp, new Pair<>(100, 0));
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>(100, 0));
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(100, 0));
                break;
            }
            case 80001539: {
                aftercancel = true;
                localDuration = 0;
                localstatups.put(MapleBuffStat.IndieExp, new Pair<>(150, 0));
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>(150, 0));
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(100, 0));
                localstatups.put(MapleBuffStat.IndiePadR, new Pair<>(70, 0));
                localstatups.put(MapleBuffStat.IndieMadR, new Pair<>(70, 0));
                break;
            }
            case 80002543: {
                localDuration = 0;
                localstatups.put(MapleBuffStat.DebuffIncHp, new Pair<>(50, localDuration)); // ?????????
                break;
            }
            case 80002758: {
                for (int i = 1; i <= 3; ++i) {
                    BuffTimer.getInstance().schedule(() -> {
                        applyto.addHP(y);
                    }, 1000 * i);
                }
                break;
            }
            case 80002762: { // ???????????? ?????????
                if (applyto.empiricalStack < x) {
                    applyto.empiricalStack++;
                }

                localstatups.put(MapleBuffStat.EmpiricalKnowledge, new Pair<>(applyto.empiricalStack, localDuration));
                break;
            }
            case 100001261: { // ?????? ????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>(-1, 5000));
                break;
            }
            case 100001263: { // ????????? ??????
                if (primary) {
                    localDuration = 0;
                    localstatups.put(MapleBuffStat.ZeroAuraStr, new Pair<>(1, 0));
                }
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) indiePad, localDuration));
                localstatups.put(MapleBuffStat.IndieMad, new Pair<>((int) indieMad, localDuration));
                localstatups.put(MapleBuffStat.IndiePdd, new Pair<>((int) indiePdd, localDuration));
                localstatups.put(MapleBuffStat.IndieTerR, new Pair<>((int) indieTerR, localDuration));
                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) indieAsrR, localDuration));
                if (applyto.getBuffedValue(MapleBuffStat.ZeroAuraSpd) != null) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.ZeroAuraSpd, 100001264);
                }
                break;
            }
            case 100001264: { // ????????? ????????????
                if (primary) {
                    localDuration = 0;
                    localstatups.put(MapleBuffStat.ZeroAuraSpd, new Pair<>(1, 0));
                }
                localstatups.put(MapleBuffStat.IndieSpeed, new Pair<>((int) indieSpeed, localDuration));
                localstatups.put(MapleBuffStat.IndieJump, new Pair<>((int) indieJump, localDuration));
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>((int) -1, localDuration));
                if (applyto.getBuffedValue(MapleBuffStat.ZeroAuraStr) != null) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.ZeroAuraStr, 100001263);
                }
                break;
            }
            case 100000276: { // ????????? ?????? (?????????)
                bufftimeR = false;
                localstatups.clear();
                localstatups.put(MapleBuffStat.TimeFastABuff, new Pair<>((int) applyto.RapidTimeDetect, 20000));
                break;
            }
            case 100000277: { // ????????? ?????? (????????????)
                bufftimeR = false;
                localstatups.clear();
                localstatups.put(MapleBuffStat.TimeFastBBuff, new Pair<>((int) applyto.RapidTimeStrength, 20000));
                break;
            }
            case 100001272: { // ?????? ????????????
                bufftimeR = false;
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.ReviveOnce, new Pair<>(100, 0));
                } else {
                    localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 2000));
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                }
                break;
            }
            case 400051041: {
                localDuration = 10000;
                localstatups.clear();
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IndieBlockSkill, new Pair<>(1, localDuration));
                break;
            }
            case 100001274: { // ?????? ??????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, localDuration));
                for (Entry<Skill, SkillEntry> z : applyto.getSkills().entrySet()) {
                    if (z.getKey().getId() != sourceid && GameConstants.isZero(z.getKey().getId() / 10000) && !z.getKey().isHyper()) {
                        if (applyto.skillisCooling(z.getKey().getId())) {
                            applyto.removeCooldown(z.getKey().getId());
                        }
                    }
                }
                if (applyto.getLevel() >= 200) {
                    SkillFactory.getSkill(100001281).getEffect(1).applyTo(applyto, false);
                }
                break;
            }
            case 100001281: { // ?????? ?????? 200?????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, localDuration));
                break;
            }
            case 101120109: { // ?????? ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.Stance, new Pair<>(100, localDuration));
                localstatups.put(MapleBuffStat.ImmuneBarrier, new Pair<>((int) (applyto.getStat().getCurrentMaxHp() * x), localDuration));
                break;
            }
            case 131001018: { // ???????????? ??????
                localstatups.put(MapleBuffStat.IndieStatR, new Pair<>(applyto.getLevel() / y, localDuration));
                break;
            }
            case 142101004: { // ????????? ??????
                if (applyto.getSkillLevel(142110009) > 0) {
                    SkillFactory.getSkill(142110009).getEffect(applyto.getSkillLevel(142110009)).applyTo(applyto, false);
                } else {
                    localstatups.put(MapleBuffStat.IndiePdd, new Pair<>((int) indiePdd, localDuration));
                    localstatups.put(MapleBuffStat.KinesisPsychicShield, new Pair<>((int) er, localDuration));
                }
                break;
            }
            case 142121030: { // ???????????????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 1000));
                break;
            }
            case 151101010: { // ????????????
                if (applyto.adelResonance < x) {
                    applyto.adelResonance++;
                }
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>(y * applyto.adelResonance, localDuration));
                localstatups.put(MapleBuffStat.AdelResonance, new Pair<>(applyto.adelResonance, localDuration));
                break;
            }
            case 151121004: { // ?????????
                localstatups.put(MapleBuffStat.IndieSuperStance, new Pair<>(1, 8000));
                localstatups.put(MapleBuffStat.IndieBarrierDischarge, new Pair<>(-x, 8000));
                localstatups.put(MapleBuffStat.IndieFloating, new Pair<>(1, 390));
                localstatups.put(MapleBuffStat.AntiMagicShell, new Pair<>(1, 8000));
                localstatups.put(MapleBuffStat.DreamDowon, new Pair<>(1, 8000)); // 601
                localstatups.put(MapleBuffStat.Dike, new Pair<>(1, 390));
                break;
            }
            case 151121011: { // ????????? ??????
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 4000 + (applyto.getSkillLevel(151120039) > 0 ? 1000 : 0)));
                break;
            }
            case 142110009: { // ????????? ??????2
                localstatups.put(MapleBuffStat.IndiePdd, new Pair<>((int) indiePdd, 180000));
                localstatups.put(MapleBuffStat.Stance, new Pair<>(stanceProp, 180000));
                localstatups.put(MapleBuffStat.KinesisPsychicShield, new Pair<>((int) er, 180000));
                break;
            }
            case 152001005: { // ???????????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NewFlying, new Pair<>(1, 1000));
                localstatups.put(MapleBuffStat.IndieFloating, new Pair<>(1, 1000));
                break;
            }
            case 152100010:
            case 152110008:
            case 152120014: { // ???????????? ??????
                aftercancel = true;
                localstatups.put(MapleBuffStat.CrystalBattery, new Pair<>(1, 0));
                break;
            }
            case 152000009: { // ????????? ??????
                bufftimeR = false;

                int max = 0;
                switch (applyto.blessMarkSkill) { // maximum
                    case 152000007:
                        max = 3;
                        break;
                    case 152110009:
                        max = 6;
                        break;
                    case 152120012:
                        max = 10;
                        break;
                }
                if (applyto.blessMark < max) {
                    applyto.blessMark++;
                }
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>(applyto.blessMark * 2, localDuration));
                localstatups.put(MapleBuffStat.IndieMad, new Pair<>(applyto.blessMark * 2, localDuration));
                localstatups.put(MapleBuffStat.BlessMark, new Pair<>(applyto.blessMark, localDuration));
                break;
            }
            case 152111003: { // ???????????? ??????:????????? ???
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>((int) indieBDR, localDuration));
                localstatups.put(MapleBuffStat.IndieStance, new Pair<>((int) indieStance, localDuration));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));

                localstatups.put(MapleBuffStat.NewFlying, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IndieFloating, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.GloryWing, new Pair<>(1, localDuration));

                applyto.canUseMortalWingBeat = true;
                break;
            }
            case 152111007: { // ???????????? ??????:????????? ??????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.HarmonyLink, new Pair<>(x, 15000));
                break;
            }
            case 152120003: { // ????????????:??????II
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IncreaseJabelinDam, new Pair<>(1, 2000));
                localstatups.put(MapleBuffStat.IndieFloating, new Pair<>(y, 2000));
                break;
            }
            case 152121043: { // ???????????? ????????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, Math.max(1, applyto.blessMark * 1000)));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, Math.max(1, applyto.blessMark * 1000)));
                break;
            }
            case 152121011: { // ????????? ??????
                localstatups.put(MapleBuffStat.IndieUnkIllium, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.FastCharge, new Pair<>(1, localDuration));
                break;
            }
            case 155001103: { // ?????? ??????
                if (localDuration > 1) {
                    localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((localDuration - 1) * y, z * 1000));
                }
                break;
            }
            case 155101006: { // ?????? ??????
                localstatups.put(MapleBuffStat.SpectorTransForm, new Pair<>(1, 0));
                break;
            }
            case 155001001: { // ????????? ??????
                localstatups.put(MapleBuffStat.Speed, new Pair<>((int) speed * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                localstatups.put(MapleBuffStat.IndieStance, new Pair<>((int) indieStance * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                break;
            }
            case 155101003: { // ????????? ??????
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) indiePad * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) indieCr * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                break;
            }
            case 155111005: { // ????????? ??????
                localstatups.put(MapleBuffStat.IndieBooster, new Pair<>((int) (applyto.getBuffedValue(155121043) ? -2 : -1), localDuration));
                localstatups.put(MapleBuffStat.IndieEvaR, new Pair<>((int) indieEvaR * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                break;
            }
            case 155111306: { // ?????? ????????? ??????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 0));
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 0));
                break;
            }
            case 155120014: { // ?????? ??????
                applyto.fightJazz = Math.min(applyto.fightJazz + 1, x);
                localstatups.put(MapleBuffStat.FightJazz, new Pair<>(applyto.fightJazz, localDuration));
            }
            case 155121005: { // ????????? ??????
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) indieIgnoreMobpdpR * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                localstatups.put(MapleBuffStat.IndieBDR, new Pair<>((int) indieBDR * (applyto.getBuffedValue(155121043) ? 2 : 1), localDuration));
                break;
            }
            case 155121043: { // ?????? ?????? ??????????????????
                localstatups.put(MapleBuffStat.ChargeSpellAmplification, new Pair<>(1, localDuration));
                break;
            }
            case 164001004: { // ?????????
                bufftimeR = false;
                localstatups.put(MapleBuffStat.NewFlying, new Pair<>(1, 1000));
                localstatups.put(MapleBuffStat.IndieFloating, new Pair<>(1, 1000));
                break;
            }
            case 164101003: { // ?????? ?????????
                applyto.giveHoyoungGauge(sourceid);
                localstatups.put(MapleBuffStat.Alterego, new Pair<>(1, localDuration));
                break;
            }
            case 164111007: // ?????? ?????????
            case 164121006: { // ?????? ?????????
                applyto.giveHoyoungGauge(sourceid);
                break;
            }
            case 164121007: { // ?????? : ????????????
                applyto.giveHoyoungGauge(sourceid);
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));
                localstatups.put(MapleBuffStat.ButterflyDream, new Pair<>(1, localDuration));
                break;
            }
            case 164121008: { // ?????? : ????????????
                applyto.giveHoyoungGauge(sourceid);
                break;
            }
            case 164121041: { // ?????? : ?????? ????????????
                localstatups.put(MapleBuffStat.Sungi, new Pair<>((int) 1, localDuration));
                break;
            }
            case 400001010: { // ????????? ??????
                applyto.setBlitzShield((int) (applyto.getStat().getCurrentMaxHp() * x / 100));
                localstatups.put(MapleBuffStat.BlitzShield, new Pair<>(applyto.getBlitzShield(), localDuration));
                break;
            }
            case 400001014:
            case 400001015: { // ?????????
                localstatups.clear();
                localstatups.put(MapleBuffStat.HeavensDoor, new Pair<>(1, x * 1000));
                break;
            }
            case 400001017: { //????????? ?????????
//            	effect = false;
                localstatups.clear();
                if (!primary) {
                    localstatups.put(MapleBuffStat.IndieAllStatR, new Pair<>((int) indieStatRBasic, localDuration));
                }
                break;
            }
            case 400001045: { // ????????? ????????? ??????
                for (MapleCoolDownValueHolder cooldown : applyto.getCooldowns()) {
                    if (cooldown.skillId != sourceid && GameConstants.isZero(cooldown.skillId / 10000) && !SkillFactory.getSkill(cooldown.skillId).isHyper()) {
                        if (applyto.skillisCooling(cooldown.skillId)) {
                            applyto.removeCooldown(cooldown.skillId);
                        }
                    }
                }
                localstatups.put(MapleBuffStat.Bless5th, new Pair<>((int) x, localDuration));
                localstatups.put(MapleBuffStat.IndiePad, new Pair<>((int) indiePad, localDuration));
                break;
            }
            case 400001050: { // ?????? ????????? ??????
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, duration));
                localstatups.put(MapleBuffStat.Bless5th, new Pair<>(1, duration));
                break;
            }
            case 400011010: { // ?????? ?????????
                if (applyfrom.getBuffedValue(400011010)) {
                    applyfrom.cancelEffectFromBuffStat(MapleBuffStat.DemonFrenzy);
                    applyfrom.getClient().getSession().writeAndFlush(CField.skillCooldown(400011010, z * 1000));
                    applyfrom.addCooldown(400011010, System.currentTimeMillis(), z * 1000);
                } else {
                    localstatups.put(MapleBuffStat.DemonFrenzy, new Pair<>(1, localDuration));
                }
                break;
            }
            case 400011011: { // ??? ????????????
                localstatups.clear();
                if (applyfrom.getBuffedValue(400011011)) {
                    applyfrom.cancelEffectFromBuffStat(MapleBuffStat.RhoAias);
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(getW2() + (applyfrom.getRhoAias() / q), q2 * 1000));
                } else {
                    applyfrom.setRhoAias(y + w + z);
                    localstatups.put(MapleBuffStat.RhoAias, new Pair<>(x, localDuration));
                }
                break;
            }
            case 400011016: { // ????????? ??????
                applyfrom.getMap().removeMistByOwner(applyfrom, 21121057);
                final long curr = System.currentTimeMillis();
                applyfrom.setCombo((short) Math.min(999, applyfrom.getCombo() + z));
                applyfrom.setLastCombo(curr);
                short combo = applyfrom.getCombo();
                int ability = combo / 50;
                applyfrom.getClient().getSession().writeAndFlush(CField.aranCombo(combo));
                if (applyfrom.getSkillLevel(21000000) > 0 && ability != (combo / 50)) {
                    SkillFactory.getSkill(21000000).getEffect(applyfrom.getSkillLevel(21000000)).applyTo(applyfrom, false);
                }
                localstatups.put(MapleBuffStat.InstallMaha, new Pair<>((int) level, localDuration));
                localstatups.put(MapleBuffStat.IndiePadR, new Pair<>((int) indiePadR, localDuration));
                break;
            }
            case 400011052: { // ???????????? ??????
                aftercancel = true;
                localstatups.put(MapleBuffStat.BlessedHammer, new Pair<>(applyto.getElementalCharge(), 0));
                break;
            }
            case 400011083: { // ?????? ?????? ?????? ?????????
                if (!applyto.getBuffedValue(400011083)) {
                    localstatups.put(MapleBuffStat.IndiePadR, new Pair<>((int) indiePadR, localDuration));
                    localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) indieCr, localDuration));
                    localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) indieIgnoreMobpdpR, localDuration));
                    localstatups.put(MapleBuffStat.SwordOfSoulLight, new Pair<>(1, localDuration));
                } else {
                    return;
                }
                break;
            }
            case 400031030: { // ?????? ???
                localstatups.clear();
                localstatups.put(MapleBuffStat.WindWall, new Pair<>(w, localDuration));
                break;
            }
            case 80002644: {
                localstatups.clear();
                break;
            }
            case 80002632: {
                localstatups.clear();
                localstatups.put(MapleBuffStat.YalBuff, new Pair<>(15, localDuration));
                break;
            }
            case 80002633: {
                localstatups.clear();
                if (!applyto.getBuffedValue(80002633)) {
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, localDuration));
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, localDuration));
                    localstatups.put(MapleBuffStat.IonBuff, new Pair<>(1, localDuration));
                } else {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieNotDamaged, 80002633);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.NotDamaged, 80002633);
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IonBuff, 80002633);
                }
                break;
            }
            case 400011003: { // ?????? ?????????
                localstatups.clear();
                if (primary) {
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(z, localDuration));
                } else {
                    localstatups.put(MapleBuffStat.HolyUnity, new Pair<>(applyto.getId(), localDuration));
                }
                break;
            }
            case 400011027: { // ?????? ????????????
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 5000));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 5000));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(60, 5000)); // ????????? 10% * ?????? ????????? 6???
                break;
            }

            case 400011053: { // ???????????? ??????
                localstatups.put(MapleBuffStat.BlessedHammer2, new Pair<>(applyto.getElementalCharge(), SkillFactory.getSkill(400011052).getEffect(level).getV() * 1000));
                break;
            }

            case 400011088: {
                if (applyto.getBuffedValue(400011088)) {
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 3000));
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));
                }
                break;
            }

            case 400011089: {
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 3000));
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));

                MapleSummon s = applyto.getSummon(400011088);
                if (s != null) {
                    s.removeSummon(applyto.getMap(), false);
                }
                break;
            }
            case 400011090: {
                localstatups.put(MapleBuffStat.IgnoreMobPdpR, new Pair<>((int) ignoreMobpdpR, 40000));
                break;
            }
            case 400011102: { // ????????? ??????
                localstatups.put(MapleBuffStat.DevilishPower, new Pair<>((int) 6, localDuration));
                break;
            }
            case 400011108: { // ????????????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, x));
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, x));

                List<AdelProjectile> swords = new ArrayList<>();
                for (int i = 0; i < bulletCount; ++i) {
                    AdelProjectile sword = new AdelProjectile(8, applyto.getId(), 0, 400011108, y * 1000, 0, 65, new Point(pos.x + 240, pos.y - 175), new ArrayList<>());
                    sword.setDelay(1320);
                    swords.add(sword);
                }
                applyto.getMap().spawnAdelProjectile(applyto, swords, true);
                break;
            }
            case 400020009: {
                applyto.cancelEffectFromBuffStat(MapleBuffStat.PsychicTornado);
                showEffect = false;
                break;
            }
            case 400021006: { // ????????? ??????
                if (applyto.getBuffedValue(400021006)) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.UnionAura, 400021006);
                    applyto.addCooldown(sourceid, System.currentTimeMillis(), getCooldown(applyto));
                    applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, getCooldown(applyto)));
                    return;
                } else {
                    localstatups.put(MapleBuffStat.YellowAura, new Pair<>((int) level, localDuration));
                    localstatups.put(MapleBuffStat.DrainAura, new Pair<>((int) level, localDuration));
                    localstatups.put(MapleBuffStat.BlueAura, new Pair<>((int) level, localDuration));
                    localstatups.put(MapleBuffStat.DarkAura, new Pair<>((int) level, localDuration));
                    localstatups.put(MapleBuffStat.DebuffAura, new Pair<>((int) level, localDuration));
                    localstatups.put(MapleBuffStat.UnionAura, new Pair<>((int) level, localDuration));
                    localstatups.put(MapleBuffStat.IndieMad, new Pair<>((int) indieMad, localDuration));
//            		localstatups.put(MapleBuffStat.IndieCr, new Pair<>((int) indieCr, localDuration));
                    //          		localstatups.put(MapleBuffStat.IndieIgnoreMobPdpR, new Pair<>((int) indieIgnoreMobpdpR, localDuration));
                }
                break;
            }
            case 400021008: { // ????????? ????????????
                localstatups.put(MapleBuffStat.PsychicTornado, new Pair<>((int) level, localDuration));
                break;
            }
            case 400021012: { // ???????????? ????????????
                localDuration = 10000;
                MapleBuffStatValueHolder mbsvh = applyto.checkBuffStatValueHolder(MapleBuffStat.IndiePmdR, 400021012);
                if (mbsvh != null) {
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(mbsvh.value + 5, localDuration));
                } else {
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(5, localDuration));
                }
                break;
            }
            case 400021032:
            case 400021033: { // ?????? ?????? ?????????
                if (applyto.getBuffedValue(2321003)) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 2321003);
                }
                break;
            }
            case 400021047: { // ?????? ?????? ??????s
                break;
            }
            case 400021052: {
                localstatups.put(MapleBuffStat.DamR, new Pair<>(Math.min(w + applyto.getStat().getInt() / x, z), localDuration));
                break;
            }
            case 400021071: { // ?????? ????????? ??????
                if (!primary) {
                    if (applyto.getPerfusion() < x) {
                        applyto.setPerfusion(applyto.getPerfusion() + 1);
                    } else if (applyto.getPerfusion() == x) {
                        applyto.setPerfusion(0);
                        if (applyto.getCooldownLimit(400021071) > 0) {
                            applyto.removeCooldown(400021071);
                        }
                    }
                } else {
                    applyto.setPerfusion(0);
                }
                localstatups.put(MapleBuffStat.LuminousPerfusion, new Pair<>(applyto.getPerfusion(), primary ? localDuration : 0));
                break;
            }
            case 400021070: { // ???????????????
                if (primary) {
                    applyfrom.peaceMaker = w;
                } else {
                    applyfrom.peaceMaker--;
                }
                applyto.addHP(applyfrom.getStat().getCurrentMaxHp() * 100 / hp);
                break;
            }
            case 400021077: {// ???????????????
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(applyfrom.getPeaceMaker(), localDuration));
                break;
            }
            case 400031005: { // ????????? ??????
                localstatups.put(MapleBuffStat.BonusAttack, new Pair<>(1, localDuration));
                int size = 0;
                for (int i = 33001007; i < 33001015; i++) {
                    if (GameConstants.getJaguarSummonId(i) > 0) {
                        SkillFactory.getSkill(i).getEffect(1).applyTo(applyto, localDuration);
                        size++;
                    }
                    if (size == 6) {
                        break;
                    }
                }
                break;
            }
            case 400031006: { // ?????? ????????????
                if (primary) {
                    applyto.trueSniping = x;
                }
                localstatups.put(MapleBuffStat.TrueSniping, new Pair<>((int) applyto.trueSniping, primary ? localDuration : (int) applyto.getBuffLimit(sourceid)));
                break;
            }
            case 400031017: {
                localstatups.put(MapleBuffStat.RideVehicle, new Pair<>(parseMountInfo(applyto, sourceid), localDuration));
                localstatups.put(MapleBuffStat.UnkBuffStat2, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(25, localDuration));
                break;
            }
            case 400031012: { // ????????? ?????????

                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(x, 10000));
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(x, 10000));
                break;
            }
            case 400031014: {
                if (applyto.getBuffedEffect(MapleBuffStat.RideVehicle) == null) {
                    SkillFactory.getSkill(33001001).getEffect(1).applyTo(applyto, 0);
                }
                break;
            }
            case 400031028: { // ?????? ????????????
                if (applyto.getBuffedEffect(MapleBuffStat.AdvancedQuiver) != null) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.AdvancedQuiver);
                }
                localstatups.put(MapleBuffStat.QuiverFullBurst, new Pair<>(x, localDuration));
                break;
            }
            case 400031034: { // ????????? ????????????

                if (applyto.getCooldownLimit(400031034) == 0) {
                    //            	effect = true;
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 1200));
                    localstatups.put(MapleBuffStat.IgnorePCounter, new Pair<>(1, 1200));
                } else if (applyto.getBuffedEffect(MapleBuffStat.NextAttackEnhance) == null) {
//            		effect = false;

                    if (applyto.energy == 1000) {
                        localstatups.put(MapleBuffStat.NextAttackEnhance, new Pair<>(100, 0));
                    } else if (applyto.energy >= 750) {
                        localstatups.put(MapleBuffStat.NextAttackEnhance, new Pair<>(75, 0));
                    } else if (applyto.energy >= 500) {
                        localstatups.put(MapleBuffStat.NextAttackEnhance, new Pair<>(50, 0));
                    } else if (applyto.energy >= 250) {
                        localstatups.put(MapleBuffStat.NextAttackEnhance, new Pair<>(25, 0));
                    }

                    applyto.energy = 0;
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.RelikGauge, new Pair<>(applyto.energy, 0));

                    applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, this, applyto));
                }
                break;
            }
            case 400031036: // ????????? ????????????
            case 400031037: // ???????????? ?????????
            case 400031038:
            case 400031039:
            case 400031040:
            case 400031041:
            case 400031042:
            case 400031043: {
                if (sourceid == 400031038) {
                    localstatups.put(MapleBuffStat.IndieUnkIllium, new Pair<>(1, localDuration));
                    localstatups.put(MapleBuffStat.IndieBarrierDischarge, new Pair<>(-x, localDuration));
                }

                if (primary) {
                    if (sourceid == 400031042) {
                        localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
                        localstatups.put(MapleBuffStat.IndieFloating, new Pair<>(1, localDuration));
                    } else if (sourceid == 400031039) {
                        localstatups.put(MapleBuffStat.IndieUnkIllium, new Pair<>(1, localDuration));
                    } else if (sourceid == 400031040) {
                        localstatups.put(MapleBuffStat.IndieUnkIllium, new Pair<>(1, localDuration));
                    }

                    applyto.energy -= forceCon;
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.RelikGauge, new Pair<>(applyto.energy, 0));

                    applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, this, applyto));
                }
                break;
            }
            case 400041002: { // ????????? ????????? 1???
                aftercancel = true;
                showEffect = false;
                localstatups.put(MapleBuffStat.ShadowAssult, new Pair<>(3, 0));
                break;
            }
            case 400041003: { // ????????? ????????? 2???
                aftercancel = true;
                showEffect = false;
                localstatups.put(MapleBuffStat.ShadowAssult, new Pair<>(2, 0));
                break;
            }
            case 400041004: { // ????????? ????????? 3???
                aftercancel = true;
                showEffect = false;
                localstatups.put(MapleBuffStat.ShadowAssult, new Pair<>(1, 0));
                break;
            }
            case 400041005: { // ????????? ????????? 4???
                showEffect = false;
                applyto.cancelEffectFromBuffStat(MapleBuffStat.ShadowAssult);
                break;
            }
            case 400041008: { // ????????? ?????????
                if (!applyto.getBuffedValue(400041008)) {
                    localstatups.put(MapleBuffStat.ShadowSpear, new Pair<>((int) level, localDuration));
                } else {
                    return;
                }
                break;
            }
            case 400041009: { // ?????? ??????
                if (primary) {
                    localstatups.put(MapleBuffStat.IndieDamageReduce, new Pair<>(y, 0));
                } else {
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));
                }
                break;
            }
            case 400041011: { // ?????????
                localstatups.put(MapleBuffStat.IndieHpR, new Pair<>((int) getIndieMhpR(), localDuration));
                break;
            }
            case 400041012: { // ????????? ??????
                localstatups.put(MapleBuffStat.DamageDecreaseWithHP, new Pair<>((int) z, localDuration));
                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) indieAsrR, localDuration));
                break;
            }
            case 400041013: { // ????????????
                localstatups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) x, localDuration));
                break;
            }
            case 400041014: { // ???????????? ???
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));
                break;
            }
            case 400041015: { // ??????
                localstatups.put(MapleBuffStat.IndieHpR, new Pair<>((int) getIndieMhpR(), localDuration));
                localstatups.put(MapleBuffStat.DamageDecreaseWithHP, new Pair<>((int) z, localDuration));
                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>((int) indieAsrR, localDuration));
                localstatups.put(MapleBuffStat.IndieReduceCooltime, new Pair<>((int) x, localDuration));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));
                break;
            }
            case 400041026:
            case 400041025:
            case 400041027: { // ??????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, s * 1000));
                break;
            }

            case 400041032: { // ?????? ??? ??????
                localstatups.clear();
                if (applyto.getBuffedValue(400041032)) {
                    localstatups.put(MapleBuffStat.IndieEvasion, new Pair<>(w, (int) (applyto.getBuffLimit(400041032) * u / 100)));
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(q, (int) (applyto.getBuffLimit(400041032) * u / 100)));
                    localstatups.put(MapleBuffStat.IndieShotDamage, new Pair<>(s, (int) (applyto.getBuffLimit(400041032) * u / 100)));
                    localstatups.put(MapleBuffStat.ReadyToDie, new Pair<>(2, (int) (applyto.getBuffLimit(400041032) * u / 100)));
                } else {
                    localstatups.put(MapleBuffStat.IndieEvasion, new Pair<>(x, localDuration));
                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(y, localDuration));
                    localstatups.put(MapleBuffStat.IndieShotDamage, new Pair<>(z, localDuration));
                    localstatups.put(MapleBuffStat.ReadyToDie, new Pair<>(1, localDuration));
                }
                break;
            }
            case 400041037: { // ????????? ?????????
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(applyto.shadowBite, localDuration));
                applyto.shadowBite = 0;
                break;
            }
            case 400041040: { // ?????? ?????? ??????
                localstatups.clear();
                if (primary) {

                    MapleMonster mob = applyto.getMap().getMonsterByOid(applyto.getMarkOfPhantomOid());

                    applyto.setMarkofPhantom(0);
                    applyto.setMarkOfPhantomOid(0);

//                    effect = false;
                    aftercancel = true;

                    if (mob != null) {
                        pos = mob.getTruePosition();
                    } else {
                        pos = applyto.getTruePosition();
                    }

                    List<Integer> skills = new ArrayList<>();
                    skills.add(400041045);
                    skills.add(400041045);
                    skills.add(400041045);
                    skills.add(400041045);
                    skills.add(400041045);
                    skills.add(400041045);
                    skills.add(400041045);
                    skills.add(400041046);
                    applyto.getClient().getSession().writeAndFlush(CField.rangeAttack(400041040, skills, 0, pos, applyto.isFacingLeft()));
                    localstatups.put(MapleBuffStat.MarkOfPhantomDebuff, new Pair<>(applyto.getMarkofPhantom(), 0)); // ??????
                    localstatups.put(MapleBuffStat.MarkOfPhantomStack, new Pair<>(applyto.getMarkofPhantom(), 0));
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 2000));
                } else {
//                    effect = false;
                    aftercancel = true;
                    if (applyto.getMarkofPhantom() < x) {
                        applyto.setMarkofPhantom(applyto.getMarkofPhantom() + 1);
                    }
                    localstatups.put(MapleBuffStat.MarkOfPhantomDebuff, new Pair<>(applyto.getMarkofPhantom(), 0)); // ??????
                    localstatups.put(MapleBuffStat.MarkOfPhantomStack, new Pair<>(applyto.getMarkofPhantom(), 0));
                }
                break;
            }
            case 400041047: {
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(1, localDuration));
                break;
            }
            case 400041048: { // ?????? : ?????? ????????????
            	System.out.println("???????????? ?????? ??? ,"+MapleBuffStat.AltergoReinforce);
                localstatups.put(MapleBuffStat.AltergoReinforce, new Pair<>(1, localDuration));
                break;
            }
            case 400041053: { // ?????? : ?????? ????????????
                applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 400041052);
                break;
            }
            case 400051000: {
                if (!primary) {
                    aftercancel = true;
                    localstatups.put(MapleBuffStat.SelectDice, new Pair<>(localDuration, 0));
                }
                break;
            }
            case 400051002: { // ????????? ???
                if (applyto.getBuffedEffect(MapleBuffStat.Transform, 400051002) == null) {
                    applyto.transformEnergyOrb = w;

                    localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));
                    localstatups.put(MapleBuffStat.Transform, new Pair<>((int) w, localDuration));

                    if (applyto.getBuffedEffect(MapleBuffStat.EnergyCharged) == null) {
                        if (applyto.getSkillLevel(5120018) > 0) {
                            SkillFactory.getSkill(5120018).getEffect(applyto.getSkillLevel(5120018)).applyTo(applyto, false);
                        } else if (applyto.getSkillLevel(5110014) > 0) {
                            SkillFactory.getSkill(5110014).getEffect(applyto.getSkillLevel(5110014)).applyTo(applyto, false);
                        } else {
                            SkillFactory.getSkill(5100015).getEffect(applyto.getSkillLevel(5100015)).applyTo(applyto, false);
                        }
                    }

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    MapleStatEffect energyCharge = applyto.getBuffedEffect(MapleBuffStat.EnergyCharged);
                    applyto.energy = energyCharge.getZ();
                    applyto.energyCharge = true;
                    statups.put(MapleBuffStat.EnergyCharged, new Pair<>(applyto.energy, localDuration));
                    applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, applyto.getBuffedEffect(MapleBuffStat.EnergyCharged), applyto));
                    applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, statups, applyto.getBuffedEffect(MapleBuffStat.EnergyCharged)), false);
                }
                break;
            }
            case 400051010: { // ????????????
                for (MapleCoolDownValueHolder cooldown : applyto.getCooldowns()) {
                    if (!SkillFactory.getSkill(cooldown.skillId).isHyper() && GameConstants.isEunWol(cooldown.skillId / 10000)) {
                        applyto.removeCooldown(cooldown.skillId);
                    }
                }
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) indiePmdR, localDuration));
                localstatups.put(MapleBuffStat.BonusAttack, new Pair<>((int) level, localDuration));
                break;
            }
            case 400051011: { // ????????? ?????????
                localstatups.clear();
                if (!applyto.getBuffedValue(400051011) && applyto.Energy_Bust_State == 1) {
                    localstatups.clear();
                    localstatups.put(MapleBuffStat.IndieSummon, new Pair<>((int) 1, localDuration));
                    localstatups.put(MapleBuffStat.EnergyBurst, new Pair<>((int) 1, localDuration));
                    applyto.Energy_Bust_State = 2;
                } else if (applyto.getBuffedValue(400051011) && applyto.Energy_Bust_State >= 2) {
                    localstatups.clear();
                    int Dummy1 = 0;
                    if (applyto.Energy_Bust_Num >= 25 && applyto.Energy_Bust_Num <= 49) {
                        Dummy1 = 2000;
                    } else if (applyto.Energy_Bust_Num >= 50) {
                        Dummy1 = 4000;
                    }
                    localDuration = 6000 + Dummy1;
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, localDuration));
                    applyto.Energy_Bust_Num = 0;
                    applyto.Energy_Bust_State = 0;
                }
                break;
            }
            case 400051024: { // ICBM ??????
                localDuration = 5000;
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, localDuration));
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, localDuration));
                break;
            }
            case 400051025: { // ICBM ??????
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 3000));
                localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 3000));
                break;
            }
            case 400051027: { // ??????????????????
                int stack = localDuration;

                localstatups.put(MapleBuffStat.IndieAsrR, new Pair<>(indieAsrR * stack, 10000));
                localstatups.put(MapleBuffStat.IndieCr, new Pair<>(indieCr * stack, 10000));
                localstatups.put(MapleBuffStat.IndieStance, new Pair<>(indieStance * stack, 10000));
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>(indiePmdR * stack, 10000));
                localstatups.put(MapleBuffStat.BonusAttack, new Pair<>(stack, 10000));
                break;
            }
            case 400051334: { // ????????? ??????
                localstatups.clear();
                if (primary) {
//            		effect = false;
                    localstatups.put(MapleBuffStat.MemoryOfSource, new Pair<>((int) level, 30000)); // duration 30000
                    SkillFactory.getSkill(sourceid).getEffect(level).applyTo(applyto, false);
                } else {
                    localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 10000)); // duration 11640
                    localstatups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 10000));
                }
                break;
            }
            case 400051044: { // ????????????
                if (applyto.striker3rdStack < 8) {
                    applyto.striker3rdStack++;
                } else {
                    List<Integer> skills = new ArrayList<>();
                    skills.add(400051044);
                    skills.add(400051045);
                    skills.add(400051045);
                    skills.add(400051045);
                    skills.add(400051045);
                    applyto.getClient().getSession().writeAndFlush(CField.rangeAttack(400051044, skills, -1, pos, applyto.isFacingLeft()));
                    applyto.striker3rdStack = 0;
                }
                aftercancel = true;
                bufftimeR = false;
                localstatups.put(MapleBuffStat.Striker3rd, new Pair<>(applyto.striker3rdStack, 0));
                break;
            }
            case 80001965: { //????????? ??????
                localstatups.put(MapleBuffStat.DashSpeed, new Pair<>((int) 300, localDuration));
                localstatups.put(MapleBuffStat.IndieForceSpeed, new Pair<>((int) 300, localDuration));
                localstatups.put(MapleBuffStat.DashJump, new Pair<>((int) 3, localDuration));
                localstatups.put(MapleBuffStat.IndieForceJump, new Pair<>((int) 3, localDuration));
                break;
            }
            case 400011136: { // ?????? ??????
                localstatups.clear();
                bufftimeR = false;
                int attackcount = 2;
                if (applyfrom.getHuntingDecreeSize() >= 2) {
                    attackcount += (applyfrom.getHuntingDecreeSize() - 1) * 2;
                }
                if (applyfrom.getBuffedEffect(MapleBuffStat.DevilishPower, 400011136) == null) {
                    localstatups.put(MapleBuffStat.DevilishPower, new Pair<>(attackcount, localDuration));
                }
                break;
            }
            case 63101001: //?????????
                localstatups.put(MapleBuffStat.Possession, new Pair<>(1, localDuration));
                applyto.MalicePoint -= 100;
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.Malice, new Pair<>(applyto.MalicePoint, 0));
                applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, applyto));
                break;
            case 63101005://????????? ???
                localstatups.put(MapleBuffStat.DragonPang, new Pair<>(1, 0));
                break;
            case 63111009:
                localstatups.put(MapleBuffStat.RemainInsence, new Pair<>(1, 0));
                break;
            case 63001002:
            case 63001003:
            case 63001005:
                localstatups.put(MapleBuffStat.DarkSight, new Pair<>(1, 420));//ret.subTime
                break;
            case 400031064://????????????????????? ????????? ???????????? ??????..
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, duration / 1000));
                break;
            case 63111013: //?????? ?????????
                localstatups.put(MapleBuffStat.DeathBlessing, new Pair<>(applyto.getSkillLevel(63120001) > 0 ? 2 : 1, 5000));
                break;
            case 63121008:
                localstatups.put(MapleBuffStat.NotDamaged, new Pair<>(1, 0));
                break;
            case 400031066: //?????? ?????? ?????????
                if (primary) {
                    applyto.AgonyCount = 0;
                } else {
                    aftercancel = true;
                    localstatups.put(MapleBuffStat.GripOfAgony, new Pair<>(applyto.AgonyCount, 0));
                }
                break;
            case 80003015:
            case 60030241:
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>(getY() * applyto.getSkillLevel(GameConstants.isKain(applyto.getJob()) ? 60030241 : 80003015), 20000));
                break;
            case 80003018:
                aftercancel = true;
                localstatups.put(MapleBuffStat.KainLink, new Pair<>(applyto.KainLinkCount, 0));
                break;
            case 80002387:
                localstatups.clear();
                localstatups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) applyto.getKeyValue(800023, "indiepmer"), 0));
                localDuration = 0;
                break;
            case 2023287: {
                applyto.MakeCDitem(2023287, 1, true);
                break;
            }
            case 80002388: {
                localstatups.clear();
                localstatups.put(MapleBuffStat.IndieCD, new Pair<>((int) applyto.getKeyValue(800023, "IndieCrMax"), 0));
                localDuration = 0;
            }
            case 80002902:
                localstatups.put(MapleBuffStat.DuskDarkness, new Pair<>(1, 20000));
                break;
            case 400001042: // ??????????????? ????????? ??????
                double a = 6.6666666;
                int alstatper = (int) Math.floor(x / a);
                localstatups.put(MapleBuffStat.IndieAllStatR, new Pair<>(alstatper, duration));
                localstatups.put(MapleBuffStat.IndieDamR, new Pair<>((int) indieDamR, duration));
                break;
            case 80002419: {//` ??????
                localstatups.clear();

                List<Integer> effects = applyfrom.getBonusEffect();
                MapleBuffStat[] stats = { MapleBuffStat.IndieDamR, MapleBuffStat.IndieExp, MapleBuffStat.DropRate, MapleBuffStat.MesoUp, MapleBuffStat.IndieCD, MapleBuffStat.IndieBDR, MapleBuffStat.IndieAllStatR, MapleBuffStat.IndiePmdR };

                for (int i = 0; i < 8; i++) {
                    int value = effects.get(i);
                    applyfrom.setEffect(i, effects.get(i));
                    if (value > 0)
                        localstatups.put(stats[i], new Pair<>(value, 0));
                }

                break;
            }
            case 400041063: {
                localstatups.put(MapleBuffStat.SageElementalClone, new Pair<>(1, localDuration));
                break;
            }
            case 162101012: {
                if (primary) {
                    int afterStack = applyto.getSkillCustomValue(sourceid) - 1;
                    if (afterStack < 0)
                        afterStack = 0;
                    applyto.setSkillCustomValue(sourceid, afterStack);
                    localstatups.put(MapleBuffStat.SeedOfMountain, new Pair<>(afterStack, 0));
                } else {
                    localstatups.put(MapleBuffStat.SeedOfMountain, new Pair<>(applyto.getSkillCustomValue(sourceid), 0));
                }
                isStackBuff = true;
                break;
            }
            case 162111006: {
                if (primary) {
                    int afterStack = applyto.getSkillCustomValue(sourceid) - 1;
                    if (afterStack < 0)
                        afterStack = 0;
                    applyto.setSkillCustomValue(sourceid, afterStack);
                    localstatups.put(MapleBuffStat.TraceOfVein, new Pair<>(afterStack, 0));
                } else {
                    localstatups.put(MapleBuffStat.TraceOfVein, new Pair<>(applyto.getSkillCustomValue(sourceid), 0));
                }
                isStackBuff = true;
                break;
            }
            case 162121042: {
                if (primary) {
                    int afterStack = applyto.getSkillCustomValue(sourceid) - 1;
                    if (afterStack < 0)
                        afterStack = 0;
                    applyto.setSkillCustomValue(sourceid, afterStack);
                    localstatups.put(MapleBuffStat.FreeVein, new Pair<>(afterStack, 0));
                } else {
                    localstatups.put(MapleBuffStat.FreeVein, new Pair<>(applyto.getSkillCustomValue(sourceid), 0));
                }
                isStackBuff = true;
                break;
            }
            case 80003070: {
                int id = GameConstants.isLala(applyto.getJob()) ? 160010001 : 80003058;
                int afterStack = applyto.getSkillCustomValue(sourceid) + 1;
                if (applyto.getCooldownLimit(id) == 0) {
                    if (afterStack >= 20) {
                        int level = applyto.getSkillLevel(id);
                        if (level > 0) {
                            SkillFactory.getSkill(id).getEffect(level).applyTo(applyto, true);
                        }
                        afterStack = 0;
                        applyto.addCooldown(id, System.currentTimeMillis(), 30000);
                        applyto.getClient().getSession().writeAndFlush(CField.skillCooldown(id, 30000));
                    }

                    applyto.setSkillCustomValue(sourceid, afterStack);
                    if (afterStack == 0) {
                        Map<MapleBuffStat, Pair<Integer, Integer>> statup = new HashMap<>();
                        statup.put(MapleBuffStat.FriendOfNature, new Pair<>(1, 0));
                        applyto.getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(statup, applyto));
                    } else {
                        localstatups.put(MapleBuffStat.FriendOfNature, new Pair<>(applyto.getSkillCustomValue(sourceid), 0));
                    }
                }

                isStackBuff = true;
                break;
            }
            case 162110007: {
                applyto.addHP((long) (applyto.getStat().getCurrentMaxHp() * (u / 100.0)));
                applyto.addMP((long) (applyto.getStat().getCurrentMaxMp(applyto) * (u / 100.0)));
                break;
            }
            case 162111004: {
                if (!primary) {
                    localDuration = x * 1000;
                }
                break;
            }
            case 162111002: {
                if (primary) {
                    localDuration = s * 1000;
                } else {
                    localDuration = s2 * 1000;
                }
                break;
            }
            case 162111001: {
                if (primary) {
                    localDuration = duration;
                } else {
                    localDuration = x * 1000;
                }
                break;
            }
            case 80003082: {
                int afterStack = applyto.getSkillCustomValue(sourceid) + 1;

                if (primary)
                    afterStack = 1;

                localstatups.put(MapleBuffStat.EventSpecialSkill, new Pair<>(afterStack, 0));
                if (afterStack >= 2 && !primary) {
                    localDuration = afterStack == 2 ? 2100 : 30005;
                    localstatups.put(MapleBuffStat.IndieWickening, new Pair<>(1, localDuration));
                }

                applyto.setSkillCustomValue(sourceid, afterStack);

                break;
            }
            default:
                if (localDuration == Integer.MAX_VALUE) {
                    localDuration = 0;
                }
                break;
        }

        if (getSummonMovementType() != null && sourceid != 1301013 && sourceid != 14000027 && sourceid != 35111002 && sourceid != 35120002 && sourceid != 400051011 && sourceid != 32001014 && sourceid != 32100010 && sourceid != 32110017 && sourceid != 32120019 && sourceid != 80002888 && sourceid != 151100002 && sourceid != 400011065 && sourceid != 400051017) {
            localstatups.put(MapleBuffStat.IndieSummon, new Pair<>(1, localDuration));
        }

        for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
            if (!localstatups.containsKey(statup.getKey())) {
                localstatups.put(statup.getKey(), new Pair<>(statup.getValue().left, localDuration));
            }
        }

        if (sourceid == 400001012) {
            applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 3111005);
            applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 3211005);
            applyto.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 3311009);
        }

        if (isMonsterRiding() && !localstatups.containsKey(MapleBuffStat.RideVehicle)) {
            localstatups.put(MapleBuffStat.RideVehicle, new Pair<>(parseMountInfo(applyto, sourceid), 0));
        }

        if (skill && !applyto.isHidden() && showEffect && !isPetBuff && (applyfrom.getId() == applyto.getId())) {
            applyto.getClient().getSession().writeAndFlush(EffectPacket.showEffect(applyto, 0, sourceid, 1, 0, 0, (byte) (applyto.getTruePosition().x > pos.x ? 1 : 0), true, pos, null, null));
            if (!GameConstants.isLinkMap(applyto.getMapId())) {
                if (sourceid != 400051334) {
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showEffect(applyto, 0, sourceid, 1, 0, 0, (byte) (applyto.getTruePosition().x > pos.x ? 1 : 0), false, pos, null, null), false);
                }
                /*            } else {
                 if (isPartyBuff(applyfrom, applyto)) {
                 final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
                 final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds,
                 Arrays.asList(MapleMapObjectType.PLAYER));

                 for (final MapleMapObject affectedmo : affecteds) {
                 final MapleCharacter affected = (MapleCharacter) affectedmo;
                 if (applyfrom.getId() != affected.getId()) {
                 applyBuffEffect(applyto, affected, primary, localDuration, pos, showEffect);
                 affected.getClient().getSession().writeAndFlush(EffectPacket.showEffect(affected, 0, sourceid, 4, 0, 0, (byte) (affected.getTruePosition().x > pos.x ? 1 : 0), true, pos, null, null));
                 affected.getMap().broadcastMessage(affected, EffectPacket.showEffect(affected, 0, sourceid, 4, 0, 0, (byte) (affected.getTruePosition().x > pos.x ? 1 : 0), false, pos, null, null), false);
                 }
                 }
                 }*/
            }
        }

        if (localstatups.containsKey(MapleBuffStat.JaguarSummoned)) {
            applyto.cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
        }

        MapleBuffStat[] bMageAuras = {MapleBuffStat.BlueAura, MapleBuffStat.DarkAura, MapleBuffStat.DebuffAura, MapleBuffStat.DrainAura, MapleBuffStat.YellowAura};
        for (MapleBuffStat bMageAura : bMageAuras) {
            if (localstatups.containsKey(bMageAura)) {
                for (MapleBuffStat bMageAuraz : bMageAuras) {
                    if (applyto.getBuffedEffect(bMageAuraz) != null) {
                        applyto.cancelEffect(applyto.getBuffedEffect(bMageAuraz), false, -1);
                    }
                }
                break;
            }
        }

        int[] FreudsProtections = {400001025, 400001026, 400001027, 400001028, 400001029, 400001030};

        for (int FreudsProtection : FreudsProtections) {
            if (sourceid == FreudsProtection) {
                for (int s : FreudsProtections) {
                    applyto.cancelEffect(SkillFactory.getSkill(s).getEffect(level), true, -1);
                }
                break;
            }
        }

        // Broadcast effect to self
        final long starttime = System.currentTimeMillis();

        boolean exit = false;

        this.setStarttime((int) (starttime % 1000000000));

        List<Pair<MapleBuffStat, MapleBuffStatValueHolder>> addV = new ArrayList<>();
        if (!isStackBuff) {
            for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : localstatups.entrySet()) {

                if (applyto.getBuffedEffect(statup.getKey(), sourceid) != null) {
                    applyto.cancelEffect(this, true, -1, statup.getKey(), true);

                    if (statup.getValue().right == 0 && !aftercancel) {
                        exit = true;
                        continue;
                    }
                }

                if (statup.getValue().right > 0) {

                    aftercancel = true;

                    if (skill && SkillFactory.getSkill(sourceid) != null && !SkillFactory.getSkill(sourceid).isHyper() && bufftimeR && sourceid < 400000000 && statup.getKey() != MapleBuffStat.IndieNotDamaged && statup.getKey() != MapleBuffStat.NotDamaged) {
                        statup.getValue().right = alchemistModifyVal(applyto, statup.getValue().right, false);
                    }

                    final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, statup.getKey());
                    ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                        cancelAction.run();
                    }, statup.getValue().right);

                    applyto.registerEffect(this, starttime, schedule, statup, false, applyto.getId());
                } else {
                    addV.add(new Pair<>(statup.getKey(), new MapleBuffStatValueHolder(this, starttime, null,
                            statup.getValue().left, statup.getValue().right, applyto.getId())));
                }
            }
        }

        if (exit) {
            applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
            return;
        }

        if (!addV.isEmpty()) {
            applyto.getEffects().addAll(addV);
        }

        if (isHide()) {
            applyto.getMap().broadcastMessage(applyto, CField.removePlayerFromMap(applyto.getId()), false);
        }

        if ((sourceid >= 400041002 && sourceid <= 400041005) || sourceid == 11121014) {
            showEffect = true;
        }

        if (applyto.isGM() && localstatups.size() > 0) {
            applyto.dropMessageGM(5, sourceid + " ????????? ???????????? ?????? <" + localstatups.size() + "???> ?????????.");
            if (localstatups != null) {
                for (Entry<MapleBuffStat, Pair<Integer, Integer>> buff : localstatups.entrySet()) {
                    applyto.dropMessageGM(-8, "name : " + buff.getKey().name() + " value : " + buff.getValue().left + " duration : " + buff.getValue().right);
                }
            }
        }

        if (showEffect) {
            if (primary && getSummonMovementType() == null) {
                if (SkillFactory.getSkill(sourceid) != null && (sourceid == 24121003 || sourceid == 30010186 || isHeroWill() || localstatups.size() > 0 || isMist() || damage > 0 || SkillFactory.getSkill(sourceid).getType() == 41 || SkillFactory.getSkill(sourceid).getType() == 51)) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
                } else {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, false, false));
                }
            }
        }

        if (localstatups.size() > 0) {
            applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, this, applyto));
            applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, localstatups, this), false);
            if (showEffect || sourceid == 3110001 || sourceid == 3210001 || sourceid == 3110012 || sourceid == 3101009) {
                applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, false, true));
            }
            if (sourceid == 162101012 || sourceid == 162111006 || sourceid == 162121042) {
                applyto.getClient().getSession().writeAndFlush(CField.UpdateSeed(sourceid, (byte) 1));
            }
            if (sourceid == 162101003 || sourceid == 162121012) {
                List<Integer> skillList = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    skillList.add(sourceid == 162121012 ? 162121013 : 162101004);
                }
                applyto.getClient().getSession().writeAndFlush(CField.UseSummonSkill(sourceid, skillList));
            }
        }

        if ((sourceid == 4211016 || sourceid == 12101022 || sourceid == 12101025 || getSummonMovementType() != null) && primary) {
            applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions(applyto, true, false));
        }
    }

    public int getMonsterRidingId() {
        return monsterRidingId;
    }

    public static final int parseMountInfo(final MapleCharacter player, final int skillid) {

        switch (skillid) {
            case 1004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 30001004:
            case 20021004:
            case 20031004:
            case 30011004:
            case 50001004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -122) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -122).getItemId();
                }
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -22) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -22).getItemId();
                }
                return 0;
            case 35001002:
            case 35111003:
            case 35120000:
                return 1932016;
            case 400031017:
                return 1932417;

            default: // default parses all
                return GameConstants.getMountItem(skillid, player);
        }
    }

    private final int calcHPChange(final MapleCharacter applyfrom, final boolean primary) {
        int hpchange = 0;
        if (hp != 0 && applyfrom.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
                if (applyfrom.hasDisease(MapleBuffStat.Undead)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                if (getSourceId() == 2321007) {
                    hpchange += applyfrom.getStat().getMaxHp() * (hp / 100.0);
                } else {
                    hpchange += makeHealHP(hp / 100.0, applyfrom.getStat().getTotalMagic(), 3, 5);
                }
                if (applyfrom.hasDisease(MapleBuffStat.Undead)) {
                    hpchange = -hpchange;
                }
            }
        }
        if (hpR != 0 && applyfrom.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
            hpchange += (int) (applyfrom.getStat().getCurrentMaxHp() * hpR)
                    / (applyfrom.hasDisease(MapleBuffStat.Undead) ? 2 : 1);
        }
        if (hpRCon != 0) {
            hpchange -= (int) (applyfrom.getStat().getCurrentMaxHp() * hpRCon);
        }
        if (sourceid == 4341002 && primary) { //???????????? hp??????
            hpchange -= (int) (applyfrom.getStat().getCurrentMaxHp() * x / 100.0);
            applyfrom.addCooldown(sourceid, System.currentTimeMillis(), getCooldown(applyfrom));
            applyfrom.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, getCooldown(applyfrom)));
        }
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }

        if (applyfrom.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
            switch (this.sourceid) {
                case 1211010:
                    hpchange = (int) (applyfrom.getStat().getCurrentMaxHp()
                            * Math.max(10, (x - (applyfrom.getListonation() * 10))) / 100);
                    break;
                case 1320019:
                    hpchange = 0;
                    break;
            }
        }

        if (hpchange < 0 && applyfrom.getBuffedValue(31221054)) {
            hpchange = 0;
        }

        return hpchange;
    }

    private static final int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1))
                + (int) (stat * lowerfactor * rate));
    }

    private final int calcMPChange(final MapleCharacter applyfrom, final boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, false); // recovery up doesn't apply for mp
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getStat().getCurrentMaxMp(applyfrom) * mpR);
        }
        if (GameConstants.isDemonSlayer(applyfrom.getJob())) {
            mpchange = 0;
        }
        if (primary) {
            if (mpCon != 0 && !GameConstants.isDemonSlayer(applyfrom.getJob())) {
                if (applyfrom.getBuffedValue(MapleBuffStat.InfinityForce) != null
                        || applyfrom.energyCharge) {
                    mpchange = 0;
                } else {
                    mpchange -= (mpCon - (mpCon * applyfrom.getStat().mpconReduce / 100))
                            * (applyfrom.getStat().mpconPercent / 100.0);
                }
            } else if (getForceCon() != 0 && GameConstants.isDemonSlayer(applyfrom.getJob())) {
                if (applyfrom.getBuffedValue(MapleBuffStat.InfinityForce) != null) {
                    mpchange = 0;
                } else {
                    mpchange -= getForceCon();
                }

                if (applyfrom.getSkillLevel(31120048) > 0 && sourceid == 31121005) {
                    mpchange /= 2;
                }

                if (applyfrom.getSkillLevel(31120051) > 0 && sourceid == 31121001) {
                    mpchange /= 2;
                }

                if (applyfrom.getSkillLevel(31121054) > 0) {
                    mpchange = mpchange * 8 / 10;
                }
            }
        }
        if (applyfrom.getBuffedValue(MapleBuffStat.Overload) != null) {
            mpchange = 0;
        }
        switch (this.sourceid) {
            case 1320019:
                mpchange = 0;
                break;
        }
        return mpchange;
    }

    public final int alchemistModifyVal(final MapleCharacter chr, final int val, final boolean withX) {
        if (!skill) { // RecoveryUP only used for hp items and skills
            return (val * (100 + (withX ? chr.getStat().RecoveryUP : chr.getStat().BuffUP)) / 100);
        }
        return (val * (100 + (withX ? chr.getStat().RecoveryUP
                : (chr.getStat().BuffUP_Skill + (getSummonMovementType() == null ? 0 : chr.getStat().BuffUP_Summon))))
                / 100);
    }

    public final int calcPowerChange(final MapleCharacter applyfrom, boolean primary) {
        int powerchange = 0;
        if (!primary) {
            return 0;
        }
        if (powerCon != 0 && GameConstants.isXenon(applyfrom.getJob())) {
            if (applyfrom.getBuffedValue(MapleBuffStat.AmaranthGenerator) != null) {
                powerchange = 0;
            } else {
                powerchange = powerCon;
            }
        }
        return powerchange;
    }

    public final void setSourceId(final int newid) {
        sourceid = newid;
    }

    public final boolean isInflation() {
        return inflation > 0;
    }

    public final int getInflation() {
        return inflation;
    }

    public final void setPartyBuff(boolean pb) {
        this.partyBuff = pb;
    }

    private boolean isPartyBuff(MapleCharacter applyfrom, MapleCharacter applyto) {
        if (lt == null || rb == null) {
            return false;
        }
        MapleBuffStat[] partybuff = {MapleBuffStat.BasicStatUp, MapleBuffStat.MaxLevelBuff};
        for (MapleBuffStat buff : partybuff) {
            if (statups.containsKey(buff)) {
                return true;
            }
        }
        if (SkillFactory.getSkill(sourceid).isHyper() && (sourceid % 100 == 53 || sourceid == 25121132)) {
            int job = sourceid / 10000;
            if (job < 1000 && applyfrom.getJob() < 1000 && applyto.getJob() < 1000) { // ????????? ?????? ????????????
                return true;
            } else if (job >= 1000 && job < 2000 && applyfrom.getJob() >= 1000 && applyfrom.getJob() < 2000 && applyto.getJob() >= 1000 && applyto.getJob() < 2000) { // ???????????? ?????? ????????????
                return true;
            } else if (job >= 2000 && job < 3000 && applyfrom.getJob() >= 1000 && applyfrom.getJob() < 3000 && applyto.getJob() >= 2000 && applyto.getJob() < 3000) { // ?????? ?????? ????????????
                return true;
            } else if (job >= 3000 && job < 4000 && applyfrom.getJob() >= 1000 && applyfrom.getJob() < 4000 && applyto.getJob() >= 3000 && applyto.getJob() < 4000) { // ??????, ???????????????, ?????? ?????? ????????????
                return true;
            }
        }

        switch (sourceid) {
            case 1101006:
            case 1211011:
            case 1301006:
            case 1301007:
            case 2101001:
            case 2201001:
            case 2301002:
            case 2301004:
            case 2311001:
            case 2311003:
            case 2311009:
            case 2321005:
            case 2321007:
            case 2321052:
            case 2321055:
            case 3121002:
            case 3221002:
            case 3321022:
            case 4001005:
            case 4101004:
            case 4111001:
            case 4201003:
            case 4301003:
            case 5121009:
            case 5301003:
            case 5320008:
            case 12101000:
            case 13121005:
            case 14101003:
            case 14001022:
            case 15121005:
            case 21111012:
            case 22151003:
            case 22171054:
            case 27111006:
            case 27111101:
            case 32001003:
            case 32101003:
            case 33101005:
            case 33121004:
            case 51101004:
            case 51111008:
            case 61121009:
            case 131001009:
            case 131001013:
            case 131001113:
            case 152121043:
            case 400011003:
            case 400021077:
            case 400031038:
            case 400041011:
            case 400041012:
            case 400041013:
            case 400041014:
            case 400041015:
                return true;
            case 155001001:
            case 155101003:
            case 155111005:
            case 155121005: { // ????????? ??????
                return applyfrom.getBuffedValue(155121043);
            }
            case 1221015:
            case 1221054: {
                return applyto.getBuffedValue(400011003) && applyto.getBuffedValue(MapleBuffStat.HolyUnity) == applyfrom.getId();
            }
            /*   case 63121044: //?????? ??????????????? ???????????? ??????
                return true;*/
        }

        if (isHeal() || isResurrection() || isTimeLeap()) {
            return true;
        }

        return false;
    }

    public final boolean isHeal() {
        return skill && (sourceid == 2301002 || sourceid == 2321007 || sourceid == 9101000 || sourceid == 9001000);
    }

    public final boolean isResurrection() {
        return skill && (sourceid == 9001005 || sourceid == 9101005 || sourceid == 2321006 || sourceid == 1221016);
    }

    public final boolean isTimeLeap() {
        return skill && sourceid == 5121010;
    }

    public final short getHp() {
        return hp;
    }

    public final short getMp() {
        return mp;
    }

    public final double getHpR() {
        return hpR;
    }

    public final double getMpR() {
        return mpR;
    }

    public final byte getMastery() {
        return mastery;
    }

    public final short getWatk() {
        return pad;
    }

    public final short getMatk() {
        return mad;
    }

    public final short getMdef() {
        return mdef;
    }

    public final short getAcc() {
        return acc;
    }

    public final short getAvoid() {
        return avoid;
    }

    public final short getHands() {
        return hands;
    }

    public final short getSpeed() {
        return speed;
    }

    public final short getJump() {
        return jump;
    }

    public final short getPassiveSpeed() {
        return psdSpeed;
    }

    public final short getPassiveJump() {
        return psdJump;
    }

    public final int getDuration() {
        return duration;
    }

    public final int getSubTime() {
        return subTime;
    }

    public final Map<MapleBuffStat, Pair<Integer, Integer>> getStatups() {
        return statups;
    }

    public final boolean sameSource(final MapleStatEffect effect) {
        boolean sameSrc = this.sourceid == effect.sourceid;
        switch (this.sourceid) { // All these are passive skills, will have to cast the normal ones.
            case 32120013: // ????????????????????????????????
                sameSrc = effect.sourceid == 32001003;
                break;
            case 32120015: // ??????????????????????????? ???????
                sameSrc = effect.sourceid == 32111012;
                break;
            case 32120014: // ??????????????????????????????????
                sameSrc = effect.sourceid == 32101003;
                break;
            case 35120000: // Extreme Mech
                sameSrc = effect.sourceid == 35001002;
                break;
            case 35121013: // Mech: Siege Mode
                sameSrc = effect.sourceid == 35111004;
                break;
        }
        return effect != null && sameSrc && this.skill == effect.skill;
    }

    public final short getNocoolProp() {
        return nocoolProp;
    }

    public final int getCr() {
        return cr;
    }

    public final int getT() {
        return t;
    }

    public final int getU() {
        return u;
    }

    public final int getV() {
        return v;
    }

    public final void setV(final int newvalue) {
        v = newvalue;
    }

    public final int getW() {
        return w;
    }

    public final int getX() {
        return x;
    }

    public final int addX(int b) {
        return x + b;
    }

    public final int getY() {
        return y;
    }

    public final void setY(final int newvalue) {
        y = newvalue;
    }

    public final int getZ() {
        return z;
    }

    public final int getS() {
        return s;
    }

    public final short getDamage() {
        return damage;
    }

    public final short getPVPDamage() {
        return PVPdamage;
    }

    public final byte getAttackCount() {
        return attackCount;
    }

    public final byte getBulletCount() {
        return bulletCount;
    }

    public final int getBulletConsume() {
        return bulletConsume;
    }

    public final byte getMobCount() {
        return mobCount;
    }

    public final int getMoneyCon() {
        return moneyCon;
    }

    public boolean cantIgnoreCooldown() {
        switch (sourceid) {
            case 1320016:
            case 1320019:
            case 2121004:
            case 2221004:
            case 2321004:
            case 2321055:
            case 5121010:
            case 12111023:
            case 12111029:
            case 14110030:
            case 21121058:
            case 24111002:
            case 30001062:
            case 64121001:
            case 64121053:
            case 80000299:
            case 80000300:
            case 80000301:
            case 80000302:
            case 80000303:
            case 80001455:
            case 80001456:
            case 80001457:
            case 80001458:
            case 80001459:
            case 80001460:
            case 80001461:
            case 80001462:
            case 80001463:
            case 80001464:
            case 80001465:
            case 80001466:
            case 80001467:
            case 80001468:
            case 80001469:
            case 80001470:
            case 80001471:
            case 80001472:
            case 80001473:
            case 80001474:
            case 80001475:
            case 80001476:
            case 80001477:
            case 80001478:
            case 80001479:
            case 100001272:
            case 100001274:
            case 100001281:
            case 150011074:
            case 155111306:
            case 155121306:
            case 155121341:
            case 164121000:
            case 164121041:
            case 164121042:
                return true;
        }
        return false;
    }

    public boolean ignoreCooldown(final MapleCharacter chra) {
        Skill skill = SkillFactory.getSkill(sourceid);
        if (skill.isHyper() || skill.isVMatrix() || skill.isNotCooltimeReset() || cantIgnoreCooldown()) {
            return false;
        }
        if (!Randomizer.isSuccess(chra.getStat().randCooldown)) {
            return false;
        }
        return true;
    }

    public final int getCooldown(final MapleCharacter chra) { // ?????????
        int localCooltime = 0;
        int ItemCooltime = 0;
        int minusCooltime = 0;

        if (cooltime == 0 && cooltimeMS == 0) {
            return 0;
        }

        if (cooltime > 0) {
            if (cooltime < 5000) {
                return cooltime;
            }
            /* if (cooltime > 10) {
                localCooltime = (int) (cooltime - (chra.getStat().reduceCooltime * 1000));
            } else {
                localCooltime = Math.max(5, (cooltime * (100 - (chra.getStat().reduceCooltime * 5)) / 100));
            }*/

            localCooltime = cooltime;

            ItemCooltime = (int) chra.getStat().reduceCooltime * 1000;
            minusCooltime = 0;

            if (chra.getBuffedEffect(MapleBuffStat.IndieReduceCooltime) != null && !GameConstants.isBeginnerJob(sourceid / 10000) && !SkillFactory.getSkill(sourceid).isVMatrix() && ((sourceid / 1000) != 8000)) {
                localCooltime -= localCooltime * chra.getBuffedValue(MapleBuffStat.IndieReduceCooltime) / 100;
            }
        } else if (cooltimeMS > 0) {
            if (cooltimeMS < 5000) {
                return cooltimeMS;
            }
            if (cooltimeMS > 10000) {
                localCooltime = (int) (cooltimeMS - (chra.getStat().reduceCooltime * 1000));
            } else {
                localCooltime = Math.max(5, (cooltimeMS * (100 - (chra.getStat().reduceCooltime * 5)) / 100));
            }
            if (chra.getBuffedEffect(MapleBuffStat.IndieReduceCooltime) != null && !GameConstants.isBeginnerJob(sourceid / 10000) && !SkillFactory.getSkill(sourceid).isVMatrix() && ((sourceid / 1000) != 8000)) {
                localCooltime -= localCooltime * chra.getBuffedValue(MapleBuffStat.IndieReduceCooltime) / 100;
            }
        }

        if (sourceid == 31121003 && chra.getBuffedEffect(MapleBuffStat.InfinityForce) != null) {
            localCooltime /= 2;
        }

        if (chra.getSkillLevel(71000231) > 0) {
            localCooltime -= localCooltime * SkillFactory.getSkill(71000231).getEffect(chra.getSkillLevel(71000231)).coolTimeR / 100;
        }

        if (chra.getSkillLevel(1220051) > 0 && sourceid == 1221011) {
            localCooltime -= localCooltime * SkillFactory.getSkill(1220051).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(2120051) > 0 && sourceid == 2121003) {
            localCooltime -= localCooltime * SkillFactory.getSkill(2120051).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(14120046) > 0 && sourceid == 14121003) {
            localCooltime -= localCooltime * SkillFactory.getSkill(14120046).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(22170087) > 0 && (sourceid == 22141012 || sourceid == 22140022)) {
            localCooltime -= localCooltime * SkillFactory.getSkill(22170087).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(22170090) > 0 && sourceid == 22171063) {
            localCooltime -= localCooltime * SkillFactory.getSkill(22170090).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(22170084) > 0 && sourceid == 22110023) {
            localCooltime -= localCooltime * SkillFactory.getSkill(22170084).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(24120044) > 0 && sourceid == 24121005) {
            localCooltime -= localCooltime * SkillFactory.getSkill(24120044).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(32120057) > 0 && sourceid == 32121004) {
            localCooltime -= localCooltime * SkillFactory.getSkill(32120057).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(32120063) > 0 && sourceid == 32121006) {
            localCooltime -= localCooltime * SkillFactory.getSkill(32120063).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(33120048) > 0 && (sourceid == 33111006 || sourceid == 33101215 || sourceid == 33121002)) {
            localCooltime -= localCooltime * SkillFactory.getSkill(33120048).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(35120045) > 0 && sourceid == 35111002) {
            localCooltime -= localCooltime * SkillFactory.getSkill(35120045).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(64120051) > 0 && sourceid == 64121001) {
            localCooltime -= localCooltime * SkillFactory.getSkill(64120051).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(65120048) > 0 && sourceid == 65121002) {
            localCooltime -= localCooltime * SkillFactory.getSkill(65120048).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(142120040) > 0 && sourceid == 142121004) {
            localCooltime -= localCooltime * SkillFactory.getSkill(142120040).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(152120036) > 0 && sourceid == 152121004) {
            localCooltime -= localCooltime * SkillFactory.getSkill(152120036).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(155120038) > 0 && sourceid == 155111306) {
            localCooltime -= localCooltime * SkillFactory.getSkill(155120038).getEffect(1).coolTimeR / 100;
        }

        if (chra.getSkillLevel(151120036) > 0 && sourceid == 151121003) {
            localCooltime -= localCooltime * SkillFactory.getSkill(151120036).getEffect(1).coolTimeR / 100;
        }

        if (localCooltime > 0 && chra.getBuffedValue(MapleBuffStat.FixCooltime) != null && sourceid / 10000 != 8000 && sourceid / 10000 <= chra.getJob()) {
            localCooltime = chra.getBuffedValue(MapleBuffStat.FixCooltime) * 1000;
        }

        if (chra.getSkillLevel(162120035) > 0 && sourceid == 162111005) {
            localCooltime -= (int) (localCooltime * (SkillFactory.getSkill(162120035).getEffect(1).coolTimeR / 100.0));
        }

        if (localCooltime <= 10000) {
            if (ItemCooltime > 0) {
                localCooltime -= ItemCooltime * 0.5;
            }
        } else {
            minusCooltime = localCooltime - 10000;
            if (ItemCooltime <= minusCooltime) {
                localCooltime -= ItemCooltime;
            } else {
                ItemCooltime -= minusCooltime;
                localCooltime -= minusCooltime;
                if (ItemCooltime > 0) {
                    localCooltime -= ItemCooltime * 0.5;
                }
            }
        }
        if (localCooltime <= 5000) {
            localCooltime = Math.max(5000, localCooltime);
        }

//        if (chra.isGM()) { //?????? ??????
//            if (localCooltime > 0) {
//                localCooltime = 1000;
//            }
//        }
        if (chra.getStat().coolTimeR > 0) {
            localCooltime -= (int) (cooltime * (chra.getStat().coolTimeR / 100.0));
        }

        return Math.max(0, localCooltime); //??? ?????? 5??????????
    }

    public final int getBerserk() {
        return berserk;
    }

    public final boolean isHide() {
        return skill && (sourceid == 9001004 || sourceid == 9101004);
    }

    public final boolean isDragonBlood() {
        return skill && sourceid == 1311008;
    }

    public final boolean isRecovery() {
        return skill && (sourceid == 1001 || sourceid == 10001001 || sourceid == 20001001 || sourceid == 20011001
                || sourceid == 20021001 || sourceid == 11001 || sourceid == 35121005);
    }

    public final boolean isBerserk() {
        return skill && sourceid == 1320006;
    }

    public final boolean isMPRecovery() {
        return skill && sourceid == 5101005;
    }

    public final boolean isMonsterRiding_() {
        return skill && (sourceid == 1004 || sourceid == 10001004 || sourceid == 20001004 || sourceid == 20011004
                || sourceid == 30001004 && (sourceid >= 80001000 && sourceid <= 80001033) || sourceid == 80001037
                || sourceid == 80001038 || sourceid == 80001039 || sourceid == 80001044
                || (sourceid >= 80001082 && sourceid <= 80001090) || sourceid == 30011159 || sourceid == 30011109
                || sourceid == 1004 // Monster riding
                || sourceid == 10001004 || sourceid == 20001004 || sourceid == 20011004 || sourceid == 30001004
                || sourceid == 20021004 || sourceid == 20031004 || sourceid == 30011004 || sourceid == 50001004
                || sourceid == 35120000 || sourceid == 400031017
                || sourceid == 33001001 || sourceid == 35001002 || sourceid == 35111003);
    }

    public final boolean isMonsterRiding() {
        return skill && (isMonsterRiding_() || GameConstants.checkMountItem(sourceid) != 0);
    }

    public final boolean isMagicDoor() {
        return skill && (sourceid == 2311002 || sourceid % 10000 == 8001 || sourceid == 400001001);
    }

    public final boolean isMesoGuard() {
        return skill && sourceid == 4211005;
    }

    public final boolean isMechDoor() {
        return skill && sourceid == 35101005;
    }

    public final boolean isDragonBlink() {
        return skill && sourceid == 22141004;
    }

    public final boolean isCharge() {
        switch (sourceid) {
            case 1211003:
            case 1211008:
            case 11111007:
            case 12101005:
            case 15101006:
            case 21111005:
                return skill;
        }
        return false;
    }

    public final boolean isPoison() {
        return dot > 0 && dotTime > 0;
    }

    public final boolean isMist() {
        if (skill) {
            switch (sourceid) {
                case 2111003:
                case 2100010:
                case 2201009:
                case 2311011:
                case 4121015:
                case 4221006:
                case 12111005:
                case 22161003:
                case 32121006:
                case 1076:
                case 11076:
                case 25111206:
                case 35120002:
                case 22170093:
                case 12121005:
                case 24121052:
                case 33111013:
                case 33121016:
                case 36121007:
                case 51120057:
                case 35121052:
                case 61121105:
                case 21121057:
                case 100001261:
                case 101120104:
                case 400031039:
                case 400031040:
                case 400020051:
                case 61121116:
                case 151121041:
                case 101120206:
                case 400010010:
                case 400011058:
                case 400021041:
                case 152121041:
                case 400021049:
                case 400021050:
                case 400001017:
                case 400020002:
                case 400040008:
                case 400041008:
                case 400011098:
                case 400011100:
                case 400051025:
                case 155121006:
                case 400030002:
                case 400031012:
                case 400041041:
                case 400020046:
                case 80001455:
                case 162121018:
                case 162101010:
                case 162121043:
                case 162111003:
                case 162111000:
                    return true;
            }
        }
        return false;
    }

    private final boolean isDispel() {
        return skill && (sourceid == 2311001 || sourceid == 9001000 || sourceid == 9101000);
    }

    private final boolean isHeroWill() {
        switch (sourceid) {
            case 1121011:
            case 1221012:
            case 1321010:
            case 2121008:
            case 2221008:
            case 2321009:
            case 3121009:
            case 3221008:
            case 3321024:
            case 4121009:
            case 4221008:
            case 4341008:
            case 5121008:
            case 5221010:
            case 5321008:
            case 21121008:
            case 22171004:
            case 22171069:
            case 23121008:
            case 24121009:
            case 25121211:
            case 27121010:
            case 32121008:
            case 33121008:
            case 35121008:
            case 36121009:
            case 37121007:
            case 61121015:
            case 61121220:
            case 64121005:
            case 65121010:
            case 151121006:
            case 152121010:
            case 155121009:
            case 164121010:
            case 400001009:
                return skill;
        }
        return false;
    }

    public final boolean isCombo() {
        switch (sourceid) {
            case 1101013:
            case 11111001: // Combo
                return skill;
        }
        return false;
    }

    public final boolean isMorph() {
        return morphId > 0;
    }

    public final boolean isDivineBody() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1010;
    }

    public final boolean isDivineShield() {
        switch (sourceid) {
            case 1220013:
                return skill;
        }
        return false;
    }

    public final boolean isBerserkFury() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1011;
    }

    public final byte getLevel() {
        return level;
    }

    public final SummonMovementType getSummonMovementType() {
        if (!skill || SkillFactory.getSkill(sourceid) == null) {
            return null;
        }
        if (GameConstants.isAngelicBlessBuffEffectItem(sourceid)) {
            return SummonMovementType.FOLLOW;
        }
        switch (sourceid) {
            case 3211002: // puppet sniper
            case 3111002: // puppet ranger
            case 33111003:
            case 13111004: // puppet cygnus
            case 5211001: // octopus - pirate
            case 5220002: // advanced octopus - pirate
            case 4341006:
            case 35111002:
            case 35111005:
            case 35111011:
            case 35121009:
            case 35121010:
            case 35121011:
            case 4111007: // dark flare
            case 4211007: // dark flare
            case 33101008:
            case 3120012:
            case 35121003:
            case 3220012:
            case 5321003:

            case 3221014: // ????????? ?????????
            case 5220023:
            case 5220024:
            case 5220025:
            case 5211014: // ???????????????
            case 5321052: // ?????? ?????? ????????????
            case 13111024: // ???????????? ?????????
            case 13120007: // ???????????? ?????????
            case 14121003: // ???????????? ??????
            case 61111002: // ????????? ?????????
            case 61111220:
            case 12111022:
            case 5321004:
            case 5320011:
            case 35120002:
            case 35111008:
            case 35101012:
            case 36121002:
            case 36121013:
            case 36121014:
            case 80002888:
            case 131001007:
            case 131001019:
            case 131001022:
            case 131001107:
            case 131001207:
            case 131001307:
            case 131002022:
            case 131003022:
            case 131003023:
            case 131004022:
            case 131004023:
            case 131005022:
            case 131005023:
            case 131006022:
            case 131006023:
            case 151111001:
            case 151100002:
            case 164121006:
            case 400021005:
            case 400011057:
            case 400001019:
            case 400001022:
            case 400051022:
            case 400051011:
            case 400001039:
            case 400001059:
            case 400041038:
            case 400021071:
            case 400021047:
            case 400021063:
            case 400021067:
            case 400021073:
            case 400041044:
            case 400041033:
            case 400021069:
            case 400051017:
            case 400011002:
            case 400011065:
            case 164121008:
            case 400021095:
            case 400041050:
            case 400041052:
            case 400031047:
            case 400031049:
            case 400031051:
            case 162101003:
            case 162101006:
            case 162121012:
            case 162121015:
                return SummonMovementType.STATIONARY;
            case 32111006: // reaper
            case 2111010:
            case 5201012: // ??????? ?????? 1
            case 5201013: // ??????? ?????? 2
            case 5201014: // ??????? ?????? 3
            case 5210015: // ????????????????? 1
            case 5210016: // ????????????????? 2
            case 5210017: // ????????????????? 3
            case 5210018: // ????????????????? 4
            case 22171081:
            case 400011012: // ???????????? ??????? ???????
            case 400011013: // ???????????? ??????? ???????
            case 400011014: // ???????????? ??????? ???????
            case 162101012:
                return SummonMovementType.WALK_STATIONARY;
            case 1301013: // ?????????
            case 2121005: // elquines
            case 2221005: // ifrit
            case 2321003: // bahamut
            case 12111004: // Ifrit
            case 11001004: // soul
            case 12001004: // flame
            case 13001004: // storm
            case 14001005: // darkness
            case 15001004: // lightning
            case 35111001:
            case 35111010:
            case 35111009:
            case 2211011: // ??????? ???????
            case 12000022:
            case 12100026:
            case 12110024:
            case 12120007: // ???????????????? ?????????
            case 23111008:
            case 23111009:
            case 23111010:
            case 25121133:
            case 32001014: // ??????
            case 32100010: // ?????? ????????????
            case 32110017: // ?????? ????????????2
            case 32120019: // ?????? ????????????3
            case 80001266:
            case 80001269:
            case 80001270:
            case 80001322:
            case 80001323:
            case 80001341:
            case 80001395:
            case 80001396:
            case 80001493:
            case 80001494:
            case 80001495:
            case 80001496:
            case 80001497:
            case 80001498:
            case 80001499:
            case 80001500:
            case 80001501:
            case 80001502:
            case 80001681:
            case 80001682:
            case 80001683:
            case 80001685:
            case 80001690:
            case 80001691:
            case 80001692:
            case 80001693:
            case 80001695:
            case 80001696:
            case 80001697:
            case 80001698:
            case 80001700:
            case 80001804:
            case 80001806:
            case 80001807:
            case 80001808:
            case 80001984:
            case 80001985:
            case 80002639:
            case 80002405:
            case 80002406:
            case 80002230:
            case 80002231:
            case 80002641:
            case 152001003:
            case 152101008:
            case 152121005:
//            case 131001015:
            case 400011001:
            case 400021032:
            case 400021033:
            case 400031001:
            case 400051009:
            case 400001013:
            case 400011077:
            case 400011078:
            case 400051038:
            case 400051046:
            case 400051052:
            case 400051053:
            case 400011090:
                return SummonMovementType.FOLLOW;
            case 101100100:
            case 101100101:
            case 400011006: // ok
                return SummonMovementType.ZEROWEAPON;
            case 12120013:
            case 12120014:
                return SummonMovementType.FLAME_SUMMON;
            case 14120008:
            case 14110029:
            case 14100027:
            case 14000027: // ??????????????????
            case 3211005: // golden eagle
            case 3111005: // golden hawk
            case 33111005:
            case 2311006: // summon dragon
            case 3221005: // frostprey
            case 3311009:
            case 3121006: // phoenix
            case 5211002: // bird - pirate
            case 164111007:
            case 400001012:
                return SummonMovementType.BIRD_FOLLOW;
            case 14111024:
            case 14121054:
            case 14121055:
            case 14121056:
            case 131001017:
            case 131002017:
            case 131003017:
            case 400011005:
            case 400031007:
            case 400031008:
            case 400031009:
                return SummonMovementType.ShadowServant;
            case 33001007:
            case 33001008:
            case 33001009:
            case 33001010:
            case 33001011:
            case 33001012:
            case 33001013:
            case 33001014:
            case 33001015:
                return SummonMovementType.SUMMON_JAGUAR;
            case 152101000:
            case 400041028:
            case 400021068:
            case 400011088:
                return SummonMovementType.ShadowServantExtend;
            case 400051068:
                return SummonMovementType.MeachCarrier;
        }
        if (isAngel()) {
            return SummonMovementType.FOLLOW;
        }
        return null;
    }

    public final boolean isAngel() {
        return GameConstants.isAngel(sourceid);
    }

    public final boolean isSkill() {
        return skill;
    }

    public final int getSourceId() {
        return sourceid;
    }

    public final boolean isIceKnight() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1105;
    }

    public final boolean isSoaring() {
        return isSoaring_Normal() || isSoaring_Mount();
    }

    public final boolean isSoaring_Normal() {
        return skill && GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1026;
    }

    public final boolean isSoaring_Mount() {
        return skill && ((GameConstants.isBeginnerJob(sourceid / 10000) && sourceid % 10000 == 1142)
                || sourceid == 80001089);
    }

    public final boolean isMechPassive() {
        switch (sourceid) {
            // case 35121005:
            case 35121013:
                return true;
        }
        return false;
    }

    /**
     *
     * @return true if the effect should happen based on it's probablity, false
     * otherwise
     */
    public final boolean makeChanceResult() {
        if (subprop != 100) {
            return subprop >= 100 || Randomizer.nextInt(100) < subprop;
        }
        return prop >= 100 || Randomizer.nextInt(100) < prop;
    }

    public final short getProp() {
        return prop;
    }

    public final short getIgnoreMob() {
        return ignoreMobpdpR;
    }

    public final int getEnhancedHP() {
        return emhp;
    }

    public final int getEnhancedMP() {
        return emmp;
    }

    public final int getEnhancedWatk() {
        return epad;
    }

    public final int getEnhancedWdef() {
        return epdd;
    }

    public final int getEnhancedMdef() {
        return emdd;
    }

    public final short getDOT() {
        return dot;
    }

    public final short getDOTTime() {
        return dotTime;
    }

    public final short getCriticalDamage() {
        return criticaldamage;
    }

    public final short getASRRate() {
        return asrR;
    }

    public final short getTERRate() {
        return terR;
    }

    public final int getDAMRate() {
        return damR;
    }

    public final short getMesoRate() {
        return mesoR;
    }

    public final int getEXP() {
        return exp;
    }

    public final short getAttackX() {
        return padX;
    }

    public final short getMagicX() {
        return madX;
    }

    public final int getPercentHP() {
        return mhpR;
    }

    public final int getPercentMP() {
        return mmpR;
    }

    public final int getConsume() {
        return consumeOnPickup;
    }

    public final int getSelfDestruction() {
        return selfDestruction;
    }

    public final int getCharColor() {
        return charColor;
    }

    public final int getSpeedMax() {
        return speedMax;
    }

    public final int getAccX() {
        return accX;
    }

    public final int getMaxHpX() {
        return getMhpX();
    }

    public final int getMaxMpX() {
        return mmpX;
    }

    public final List<Integer> getPetsCanConsume() {
        return petsCanConsume;
    }

    public final boolean isReturnScroll() {
        return skill && (sourceid == 80001040 || sourceid == 20021110);
    }

    public final boolean isMechChange() {
        switch (sourceid) {
            case 35111004: // siege
            case 35001001: // flame
            case 35101009:
            case 35121013:
            case 35121005:
            case 35121054: // ??????????? return skill;
        }
        return false;
    }

    public final int getRange() {
        return range;
    }

    public final short getER() {
        return er;
    }

    public final int getPrice() {
        return price;
    }

    public final int getExtendPrice() {
        return extendPrice;
    }

    public final byte getPeriod() {
        return period;
    }

    public final byte getReqGuildLevel() {
        return reqGuildLevel;
    }

    public final byte getEXPRate() {
        return expR;
    }

    public final short getLifeID() {
        return lifeId;
    }

    public final short getUseLevel() {
        return useLevel;
    }

    public final byte getSlotCount() {
        return slotCount;
    }

    public final short getStr() {
        return str;
    }

    public final short getStrX() {
        return strX;
    }

    public final short getDex() {
        return dex;
    }

    public final short getDexX() {
        return dexX;
    }

    public final short getInt() {
        return int_;
    }

    public final short getIntX() {
        return intX;
    }

    public final short getLuk() {
        return luk;
    }

    public final short getLukX() {
        return lukX;
    }

    public final short getComboConAran() {
        return comboConAran;
    }

    public final short getMPCon() {
        return mpCon;
    }

    public final short getMPConReduce() {
        return mpConReduce;
    }

    public final int getSoulMPCon() {
        return soulmpCon;
    }

    public final short getIndieMHp() {
        return indieMhp;
    }

    public final short getIndieMMp() {
        return indieMmp;
    }

    public final short getIndieAllStat() {
        return indieAllStat;
    }

    public final byte getType() {
        return type;
    }

    public int getBossDamage() {
        return getBdR();
    }

    public int getInterval() {
        return interval;
    }

    public ArrayList<Pair<Integer, Integer>> getAvailableMaps() {
        return availableMap;
    }

    public short getWDEFRate() {
        return pddR;
    }

    public short getMDEFRate() {
        return mddR;
    }

    public short getOnActive() {
        return onActive;
    }

    public boolean isDoubleDice() {
        switch (sourceid) {
            case 5120012:
            case 5220014:
            case 5320007:
            case 35120014:
                return true;
        }
        return false;
    }

    public void applyTo(MapleCharacter player, MapleCharacter player0, boolean b, Point truePosition, int i, byte b0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static class CancelEffectAction implements Runnable {

        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;
        private final MapleBuffStat buffStat;

        public CancelEffectAction(final MapleCharacter target, final MapleStatEffect effect, final long startTime, final MapleBuffStat buffStat) {
            this.effect = effect;
            this.target = new WeakReference<MapleCharacter>(target);
            this.startTime = startTime;
            this.buffStat = buffStat;
        }

        @Override
        public void run() {
            final MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime, buffStat, false);
            }
        }
    }

    public static class CancelDiseaseAction implements Runnable {

        private final WeakReference<MapleCharacter> target;
        private final Map<MapleBuffStat, Pair<Integer, Integer>> statup;

        public CancelDiseaseAction(final MapleCharacter target,
                final Map<MapleBuffStat, Pair<Integer, Integer>> statup) {
            this.target = new WeakReference<MapleCharacter>(target);
            this.statup = statup;
        }

        @Override
        public void run() {
            final MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelDisease(statup);
            }
        }
    }

    public int getPPCon() {
        return ppcon;
    }

    public int getPPRecovery() {
        return ppRecovery;
    }

    public int getEqskill1() {
        return eqskill1;
    }

    public void setEqskill1(int eqskill1) {
        this.eqskill1 = eqskill1;
    }

    public int getEqskill2() {
        return eqskill2;
    }

    public void setEqskill2(int eqskill2) {
        this.eqskill2 = eqskill2;
    }

    public int getEqskill3() {
        return eqskill3;
    }

    public void setEqskill3(int eqskill3) {
        this.eqskill3 = eqskill3;
    }

    public void setMaxHpX(int hp2) {
        this.setMhpX(hp2);
    }

    public short getPdd() {
        return pdd;
    }

    public void setPdd(short pdd) {
        this.pdd = pdd;
    }

    public short getIndiePmdR() {
        return indiePmdR;
    }

    public void setIndiePmdR(short IndiePmdR) {
        this.indiePmdR = IndiePmdR;
    }

    public int getQ() {
        return q;
    }

    public int getQ2() {
        return q2;
    }

    public int getMhpX() {
        return mhpX;
    }

    public void setMhpX(int mhpX) {
        this.mhpX = mhpX;
    }

    public short getLv2mhp() {
        return lv2mhp;
    }

    public void setLv2mhp(short lv2mhp) {
        this.lv2mhp = lv2mhp;
    }

    public Point getRb() {
        return rb;
    }

    public void setRb(Point rb) {
        this.rb = rb;
    }

    public Point getLt() {
        return lt;
    }

    public void setLt(Point lt) {
        this.lt = lt;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public short getBufftimeR() {
        return bufftimeR;
    }

    public void setBufftimeR(short bufftimeR) {
        this.bufftimeR = bufftimeR;
    }

    public short getLv2mmp() {
        return lv2mmp;
    }

    public void setLv2mmp(short lv2mmp) {
        this.lv2mmp = lv2mmp;
    }

    public short getIndieMhpR() {
        return indieMhpR;
    }

    public void setIndieMhpR(short indieMhpR) {
        this.indieMhpR = indieMhpR;
    }

    public int getS2() {
        return s2;
    }

    public void setS2(int s2) {
        this.s2 = s2;
    }

    public int getPPReq() {
        return ppReq;
    }

    public void setPPReq(int ppReq) {
        this.ppReq = ppReq;
    }

    public short getDotInterval() {
        return dotInterval;
    }

    public void setDotInterval(short dotInterval) {
        this.dotInterval = dotInterval;
    }

    public short getDotSuperpos() {
        return dotSuperpos;
    }

    public void setDotSuperpos(short dotSuperpos) {
        this.dotSuperpos = dotSuperpos;
    }

    public int getU2() {
        return u2;
    }

    public void setU2(int u2) {
        this.u2 = u2;
    }

    public short getIgnoreMobDamR() {
        return ignoreMobDamR;
    }

    public void setIgnoreMobDamR(short ignoreMobDamR) {
        this.ignoreMobDamR = ignoreMobDamR;
    }

    public short getKillRecoveryR() {
        return killRecoveryR;
    }

    public void setKillRecoveryR(short killRecoveryR) {
        this.killRecoveryR = killRecoveryR;
    }

    public int getW2() {
        return w2;
    }

    public void setW2(int w2) {
        this.w2 = w2;
    }

    public short getMaxDemonForce() {
        return mdf;
    }

    public short getForceCon() {
        return forceCon;
    }

    public void setForceCon(short forceCon) {
        this.forceCon = forceCon;
    }

    public short getBdR() {
        return bdR;
    }

    public void setBdR(short bdR) {
        this.bdR = bdR;
    }

    public double getExpRPerM() {
        return expRPerM;
    }

    public void setExpRPerM(double expRPerM) {
        this.expRPerM = expRPerM;
    }

    public short getHpFX() {
        return hpFX;
    }

    public void setHpFX(short hpFX) {
        this.hpFX = hpFX;
    }

    public short getSummonTimeR() {
        return summonTimeR;
    }

    public void setSummonTimeR(short summonTimeR) {
        this.summonTimeR = summonTimeR;
    }

    public short getTargetPlus() {
        return targetPlus;
    }

    public void setTargetPlus(short targetPlus) {
        this.targetPlus = targetPlus;
    }

    public short getTargetPlus_5th() {
        return targetPlus_5th;
    }

    public void setTargetPlus_5th(short targetPlus_5th) {
        this.targetPlus_5th = targetPlus_5th;
    }

    public short getIndieExp() {
        return IndieExp;
    }

    public int getCooltime() {
        return cooltime;
    }

    public boolean isEnergyChargeCooling() {
        return energyChargeCooling;
    }

    public void setEnergyChargeCooling(boolean energyChargeCooling) {
        this.energyChargeCooling = energyChargeCooling;
    }

    public boolean isEnergyChargeActived() {
        return energyChargeActived;
    }

    public void setEnergyChargeActived(boolean energyChargeActived) {
        this.energyChargeActived = energyChargeActived;
    }

    public short getDamAbsorbShieldR() {
        return damAbsorbShieldR;
    }

    public void setDamAbsorbShieldR(short damAbsorbShieldR) {
        this.damAbsorbShieldR = damAbsorbShieldR;
    }

    public int getCoolTimeR() {
        return coolTimeR;
    }
}
