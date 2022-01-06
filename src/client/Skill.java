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
package client;

import constants.GameConstants;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import provider.MapleData;
import provider.MapleDataTool;
import server.AdelProjectile;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.Element;
import tools.Pair;
import tools.StringUtil;

public class Skill {

    private String name = "", psdDamR = "", desc = "";
    private final List<MapleStatEffect> effects = new ArrayList<MapleStatEffect>();
    private List<MapleStatEffect> pvpEffects = null;
    private List<Integer> animation = null, psdSkills = new ArrayList<>();
    private final List<Pair<String, Integer>> requiredSkill = new ArrayList<>();
    private Element element = Element.NEUTRAL;
    private List<Integer> customs = new CopyOnWriteArrayList<>();
    private List<Point> extraPos = new CopyOnWriteArrayList<>();
    private int id, animationTime = 0, type = 0, masterLevel = 0, maxLevel = 0, delay = 0, trueMax = 0, eventTamingMob = 0, skillType = 0, psd = 0, dataIndex = -1, enableDelay = 0, expire = 0, groupEvent = 0, attackableCount = 0;
    private boolean invisible = false, chargeskill = false, timeLimited = false, combatOrders = false, pvpDisabled = false, magic = false, casterMove = false, pushTarget = false, pullTarget = false, hyper = false, chainAttack = false, notCooltimeReset = false, vSkill = false, notIncBuffDuration = false, ignoreCounter = false;
    private List<AdelProjectile> AdelProjectile = new ArrayList<>();

    public Skill(final int id) {
        super();
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDesc(final String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<AdelProjectile> getSecondAtoms() {
        return AdelProjectile;
    }

    public static final Skill loadFromData(final int id, final MapleData data, final MapleData delayData) {
        Skill ret = new Skill(id);

        boolean isBuff = false;
        final int skillType = MapleDataTool.getInt("skillType", data, -1);
        final String elem = MapleDataTool.getString("elemAttr", data, null);
        if (elem != null) {
            ret.element = Element.getFromChar(elem.charAt(0));
        }
        ret.skillType = skillType;
        ret.invisible = MapleDataTool.getInt("invisible", data, 0) > 0;
        ret.timeLimited = MapleDataTool.getInt("timeLimited", data, 0) > 0;
        ret.combatOrders = MapleDataTool.getInt("combatOrders", data, 0) > 0;
        ret.masterLevel = MapleDataTool.getInt("masterLevel", data, 0);
        ret.hyper = (data.getChildByPath("hyper") != null);
        ret.vSkill = (data.getChildByPath("vSkill") != null);

        ret.psd = MapleDataTool.getInt("psd", data, 0);
        final MapleData psdskill = data.getChildByPath("psdSkill");
        if (psdskill != null) {
            for (MapleData d : data.getChildByPath("psdSkill").getChildren()) {
                ret.psdSkills.add(Integer.parseInt(d.getName()));
            }
            for (int pskill : ret.psdSkills) {
                Skill skil = SkillFactory.getSkill(pskill);
                if (skil != null) {
                    skil.getPsdSkills().add(id);
                }
            }
        }
        if (id == 22140000 || id == 22141002) {
            ret.masterLevel = 5; //hack
        }
        ret.notCooltimeReset = (data.getChildByPath("notCooltimeReset") != null);
        ret.notIncBuffDuration = (data.getChildByPath("notIncBuffDuration") != null);
        ret.eventTamingMob = MapleDataTool.getInt("eventTamingMob", data, 0);
        final MapleData inf = data.getChildByPath("info");
        if (inf != null) {
            ret.type = MapleDataTool.getInt("type", inf, 0);
            ret.pvpDisabled = MapleDataTool.getInt("pvp", inf, 1) <= 0;
            ret.magic = MapleDataTool.getInt("magicDamage", inf, 0) > 0;
            ret.casterMove = MapleDataTool.getInt("casterMove", inf, 0) > 0;
            ret.pushTarget = MapleDataTool.getInt("pushTarget", inf, 0) > 0;
            ret.pullTarget = MapleDataTool.getInt("pullTarget", inf, 0) > 0;
            ret.chainAttack = MapleDataTool.getInt("chainAttack", inf, 0) > 0;
        }
        final MapleData inf2 = data.getChildByPath("info2");
        if (inf2 != null) {
            ret.ignoreCounter = true;
        }
        final MapleData effect = data.getChildByPath("effect");
        if (skillType == 1) { // mastery
            isBuff = false;
        } else if (skillType == 2) { // booster
            isBuff = true;
        } else if (skillType == 3) { //final attack
            ret.animation = new ArrayList<Integer>();
            ret.animation.add(0);
            isBuff = effect != null;
        } else {
            MapleData action_ = data.getChildByPath("action");
            final MapleData hit = data.getChildByPath("hit");
            final MapleData ball = data.getChildByPath("ball");

            boolean action = false;
            if (action_ == null) {
                if (data.getChildByPath("prepare/action") != null) {
                    action_ = data.getChildByPath("prepare/action");
                    action = true;
                }
            }
            isBuff = effect != null && hit == null && ball == null;
            if (action_ != null) {
                String d = null;
                if (action) { //prepare
                    d = MapleDataTool.getString(action_, null);
                } else {
                    d = MapleDataTool.getString("0", action_, null);
                }
                if (d != null) {
                    isBuff |= d.equals("alert2");
                    final MapleData dd = delayData.getChildByPath(d);
                    if (dd != null) {
                        for (MapleData del : dd) {
                            ret.delay += Math.abs(MapleDataTool.getInt("delay", del, 0));
                        }
                        if (ret.delay > 30) { //then, faster(2) = (10+2)/16 which is basically 3/4
                            ret.delay = (int) Math.round(ret.delay * 11.0 / 16.0); //fastest(1) lolol
                            ret.delay -= (ret.delay % 30); //round to 30ms
                        }
                    }
                    if (SkillFactory.getDelay(d) != null) { //this should return true always
                        ret.animation = new ArrayList<Integer>();
                        ret.animation.add(SkillFactory.getDelay(d));
                        if (!action) {
                            for (MapleData ddc : action_) {
                                if (!MapleDataTool.getString(ddc, d).equals(d)) {
                                    String c = MapleDataTool.getString(ddc);
                                    if (SkillFactory.getDelay(c) != null) {
                                        ret.animation.add(SkillFactory.getDelay(c));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (StringUtil.getLeftPaddedStr(String.valueOf(id / 10000), '0', 3).equals("8000")) { //소울, 룬 스킬 등.
            isBuff = true;
        }

        ret.chargeskill = data.getChildByPath("keydown") != null;
        //some skills have old system, some new		
        final MapleData level = data.getChildByPath("common");
        if (level != null) {
            ret.maxLevel = MapleDataTool.getInt("maxLevel", level, 1); //10 just a failsafe, shouldn't actually happens
            ret.psdDamR = MapleDataTool.getString("damR", level, ""); //for the psdSkill tag
            ret.trueMax = ret.maxLevel + (ret.combatOrders ? 2 : 0) + (ret.isVMatrix() ? 5 : 0);

            for (int i = 1; i <= ret.trueMax; i++) {
                ret.getEffects().add(MapleStatEffect.loadSkillEffectFromData(level, id, isBuff, i, "x"));
            }

        } else {
            for (final MapleData leve : data.getChildByPath("level")) {

                ret.getEffects().add(MapleStatEffect.loadSkillEffectFromData(leve, id, isBuff, Byte.parseByte(leve.getName()), null));
            }
            ret.maxLevel = ret.getEffects().size();
            ret.trueMax = ret.getEffects().size();
        }
        final MapleData level2 = data.getChildByPath("PVPcommon");
        if (level2 != null) {
            ret.pvpEffects = new ArrayList<MapleStatEffect>();
            for (int i = 1; i <= ret.trueMax; i++) {
                ret.pvpEffects.add(MapleStatEffect.loadSkillEffectFromData(level2, id, isBuff, i, "x"));
            }
        }
        final MapleData reqDataRoot = data.getChildByPath("req");
        if (reqDataRoot != null) {
            for (final MapleData reqData : reqDataRoot.getChildren()) {
                ret.requiredSkill.add(new Pair<>(reqData.getName(), MapleDataTool.getInt(reqData, 1)));
            }
        }

        ret.animationTime = 0;

        if (effect != null) {
            for (final MapleData effectEntry : effect) {
                ret.animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }
        final MapleData SecondAtom = data.getChildByPath("SecondAtom");
        if (SecondAtom != null) {
            ret.setDataIndex(MapleDataTool.getInt("dataIndex", SecondAtom, -1));
            ret.setEnableDelay(MapleDataTool.getInt("enableDelay", SecondAtom, -1));
            ret.setExpire(MapleDataTool.getInt("expire", SecondAtom, -1));
            ret.setGroupEvent(MapleDataTool.getInt("groupEvent", SecondAtom, -1));
            ret.setAttackableCount(MapleDataTool.getInt("attackableCount", SecondAtom, -1));
            MapleData extraPosData = SecondAtom.getChildByPath("extraPos");
            if (extraPosData != null) {
                for (MapleData c : extraPosData) {
                    ret.getExtraPos().add(MapleDataTool.getPoint(c));
                }
            }
            MapleData customData = SecondAtom.getChildByPath("custom");
            if (customData != null) {
                for (MapleData c : customData) {
                    ret.getCustoms().add(MapleDataTool.getInt(c));
                }
            }
            final MapleData atoms = SecondAtom.getChildByPath("atom");
            if (atoms != null) {
                ret.AdelProjectile = new ArrayList<>();
                for (final MapleData atom : atoms.getChildren()) {
                    int createDelay = MapleDataTool.getInt("createDelay", atom, -1);
                    int enableDelay = MapleDataTool.getInt("enableDelay", atom, -1);
                    int rotate = MapleDataTool.getInt("rotate", atom, -1);
                    int expire = MapleDataTool.getInt("expire", atom, -1);
                    Point pos = MapleDataTool.getPoint("pos", atom, new Point(0, 0));
                    int dataIndex = MapleDataTool.getInt("dataIndex", atom, -1);
                    int attackableCount = MapleDataTool.getInt("attackableCount", atom, 1);
                    int groupEvent = MapleDataTool.getInt("groupEvent", atom, 0);
                    List<Point> extraPos = new ArrayList<>();
                    final MapleData extra = atom.getChildByPath("extraPos");
                    if (extra != null) {
                        for (MapleData ex : extra) {
                            Point expos = MapleDataTool.getPoint(ex);
                            extraPos.add(expos);
                        }
                    }
                    List<Integer> customs = new ArrayList<>();
                    final MapleData custom = atom.getChildByPath("custom");

                    if (custom != null) {
                        for (MapleData c : custom) {
                            customs.add(MapleDataTool.getInt(c));
                        }
                    }
                    int Index = Integer.parseInt(atom.getName());
                    AdelProjectile a = new AdelProjectile(dataIndex, 0, 0, 0, expire, 0, attackableCount, pos, customs);

                    ret.AdelProjectile.add(a);
                }
            }
        }
        return ret;
    }

    public MapleStatEffect getEffect(final int level) {
        if (getEffects().size() < level) {
            if (getEffects().size() > 0) { //incAllskill
                return getEffects().get(getEffects().size() - 1);
            }
            return null;
        } else if (level <= 0) {
            return getEffects().get(0);
        }
        return getEffects().get(level - 1);
    }

    public MapleStatEffect getPVPEffect(final int level) {
        if (pvpEffects == null) {
            return getEffect(level);
        }
        if (pvpEffects.size() < level) {
            if (pvpEffects.size() > 0) { //incAllskill
                return pvpEffects.get(pvpEffects.size() - 1);
            }
            return null;
        } else if (level <= 0) {
            return pvpEffects.get(0);
        }
        return pvpEffects.get(level - 1);
    }

    public int getSkillType() {
        return skillType;
    }

    public List<Integer> getAllAnimation() {
        return animation;
    }

    public int getAnimation() {
        if (animation == null) {
            return -1;
        }
        return animation.get(Randomizer.nextInt(animation.size()));
    }

    public boolean isPVPDisabled() {
        return pvpDisabled;
    }

    public boolean isChargeSkill() {
        return chargeskill;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public boolean hasRequiredSkill() {
        return requiredSkill.size() > 0;
    }

    public List<Pair<String, Integer>> getRequiredSkills() {
        return requiredSkill;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getTrueMax() {
        return trueMax;
    }

    public boolean combatOrders() {
        return combatOrders;
    }

    public boolean canBeLearnedBy(MapleCharacter chr) {
        short job = chr.getJob();
        int jid = job;
        int skillForJob = id / 10000;
        if (skillForJob == 2001) {
            return GameConstants.isEvan(job); //special exception for beginner -.-
        } else if (chr.getSubcategory() == 1) {
            return GameConstants.isDualBlade(job);
        } else if (chr.getSubcategory() == 2) {
            return GameConstants.isCannon(job); //special exception for beginner
        } else if (skillForJob == 0) {
            return GameConstants.isAdventurer(job); //special exception for beginner
        } else if (skillForJob == 1000) {
            return GameConstants.isKOC(job); //special exception for beginner
        } else if (skillForJob == 2000) {
            return GameConstants.isAran(job); //special exception for beginner
        } else if (skillForJob == 3000) {
            return GameConstants.isResist(job); //special exception for beginner
        } else if (skillForJob == 3001) {
            return GameConstants.isDemonSlayer(job); //special exception for beginner
        } else if (skillForJob == 2002) {
            return GameConstants.isMercedes(job); //special exception for beginner
        } else if (jid / 100 != skillForJob / 100) { // wrong job
            return false;
        } else if (jid / 1000 != skillForJob / 1000) { // wrong job
            return false;
        } else if (GameConstants.isCannon(skillForJob) && !GameConstants.isCannon(job)) {
            return false;
        } else if (GameConstants.isDemonSlayer(skillForJob) && !GameConstants.isDemonSlayer(job)) {
            return false;
        } else if (GameConstants.isAdventurer(skillForJob) && !GameConstants.isAdventurer(job)) {
            return false;
        } else if (GameConstants.isKOC(skillForJob) && !GameConstants.isKOC(job)) {
            return false;
        } else if (GameConstants.isAran(skillForJob) && !GameConstants.isAran(job)) {
            return false;
        } else if (GameConstants.isEvan(skillForJob) && !GameConstants.isEvan(job)) {
            return false;
        } else if (GameConstants.isMercedes(skillForJob) && !GameConstants.isMercedes(job)) {
            return false;
        } else if (GameConstants.isResist(skillForJob) && !GameConstants.isResist(job)) {
            return false;
        } else if ((jid / 10) % 10 == 0 && (skillForJob / 10) % 10 > (jid / 10) % 10) { // wrong 2nd job
            return false;
        } else // wrong 3rd/4th job
            if ((skillForJob / 10) % 10 != 0 && (skillForJob / 10) % 10 != (jid / 10) % 10) { //wrong 2nd job
                return false;
            } else return skillForJob % 10 <= jid % 10;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public boolean sub_4FD900(int a1) {
        boolean v1; // zf@9

        if (a1 <= 0x512D47) {
            if (a1 == 0x512D47) {
                return true;
            }
            if (a1 > 4210012) {
                if (a1 > 5220012) {
                    if (a1 == 0x4FA6AD + 1) {
                        return true;
                    }
                    v1 = a1 == 0x4FAA9E;
                } else {
                    if (a1 == 0x4FA6AA + 2) {
                        return true;
                    }
                    if (a1 > 0x42392C) {
                        if (a1 < 5120011 || a1 > 5120012) {
                            return false;
                        }
                        return true;
                    }
                    if (a1 == 0x42392C) {
                        return true;
                    }
                    v1 = a1 == 0x423928 + 2;
                }
            } else {
                if (a1 == 0x403D57 + 5) {
                    return true;
                }
                if (a1 > 2221009) {
                    if (a1 == 2321010 || a1 == 3210015) {
                        return true;
                    }
                    v1 = a1 == 4110012;
                } else {
                    if (a1 == 2221009 || a1 == 1120012 || a1 == 1320011) {
                        return true;
                    }
                    v1 = a1 == 2121009;
                }
            }
        }
        if (a1 > 0x160C888 + 3) {
            if (a1 > 0x217E38E) {
                if (a1 == 0x30C0780) {
                    return true;
                }
                v1 = a1 == 80001913;
            } else {
                if (a1 == 0x217E38E
                        || a1 == 0x160C88C + 1
                        || a1 == 0x160CC6D + 3) {
                    return true;
                }
                v1 = a1 == 0x1F95F0A;
            }
            if (!v1) {
                return false;
            }
            return true;
        }
        if (a1 == 0x160C888 + 3) {
            return true;
        }
        if (a1 <= 0x142440C + 2) {
            if (a1 == 0x142440C + 2
                    || a1 == 0x51312B + 1
                    || (a1 - 0x51312B + 1) == 2) {
                return true;
            }
            v1 = (a1 - 0x51312B + 1) - 2 == 0xF112DC + 1;
            if (!v1) {
                return false;
            }
            return true;
        }
        if (a1 > 0x14247EC + 4) {
            v1 = a1 == 0x1524DBD;
            if (!v1) {
                return false;
            }
            return true;
        }
        if (a1 == 0x14247EC + 4) {
            return true;
        }
        if (a1 >= 0x1424413 + 1) {
            if (a1 > 0x1424413 + 2) {
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean sub_4FDA20(int a1) {
        int v1; // esi@3
        boolean result; // eax@5

        result = false;
        if ((a1 - 92000000) >= 0xF4240 || (a1 % 10000) != 0) {
            v1 = 10000 * (a1 / 10000);
            if ((v1 - 92000000) < 0xF4240 && (v1 % 10000) == 0) {
                result = true;
            }
        }
        return result;
    }

    public boolean sub_4FD870(int a1) {
        int v1; // eax@1

        v1 = a1 / 10000;
        if (a1 / 10000 == 8000) {
            v1 = a1 / 100;
        }
        return (v1 - 800000) <= 0x63;
    }

    public boolean sub_48AEF0(int a1) {
        int v1; // ecx@1
        boolean result; // eax@4

        v1 = a1 / 10000;
        if (a1 / 10000 == 8000) {
            v1 = a1 / 100;
        }
        if ((v1 - 40000) > 5) {
            result = sub_48A360(v1);
        } else {
            result = false;
        }
        return result;
    }

    public boolean sub_48A360(int a1) {
        boolean v2; // zf@8

        if (a1 > 6001) {
            if (a1 == 13000) {
                return true;
            }
            v2 = a1 == 14000;
        } else {
            if (a1 >= 6000) {
                return true;
            }
            if (a1 <= 3002) {
                if (a1 >= 3001 || a1 >= 2001 && a1 <= 2005) {
                    return true;
                }
                if ((a1 - 40000) <= 5) {
                    return false;
                }
                if ((a1 % 1000) == 0) {
                    return true;
                }
            }
            v2 = a1 == 5000;
        }
        if (v2) {
            return true;
        }
        return (a1 - 800000) < 0x64;
    }

    public boolean sub_4FD8B0(int a1) {
        int v1; // eax@3
        boolean result; // al@5

        if (a1 >= 0) {
            v1 = a1 / 10000;
            if (a1 / 10000 == 8000) {
                v1 = a1 / 100;
            }
            result = v1 == 9500;
        } else {
            result = false;
        }
        return result;
    }

    public int sub_48A160(int a1) {
        int result; // eax@1

        result = a1 / 10000;
        if (a1 / 10000 == 8000) {
            result = a1 / 100;
        }
        return result;
    }

    public int sub_489A10(int a1) {
        int result; // eax@6

        if (sub_48A360(a1) || (a1 % 100) == 0 || a1 == 501 || a1 == 3101) {
            result = 1;
        } else if ((a1 - 2200) < 0x64 || a1 == 2001) {
            switch (a1) {
                case 2200:
                case 2210:
                    result = 1;
                    break;
                case 2211:
                case 2212:
                case 2213:
                    result = 2;
                    break;
                case 2214:
                case 2215:
                case 2216:
                    result = 3;
                    break;
                case 2217:
                case 2218:
                    result = 4;
                    break;
                default:
                    result = 0;
                    break;
            }
        } else if (a1 / 10 == 43) {
            result = 0;
            if (((a1 - 430) / 2) <= 2) {
                result = (a1 - 430) / 2 + 2;
            }
        } else {
            result = 0;
            if ((a1 % 10) <= 2) {
                result = a1 % 10 + 2;
            }
        }
        return result;
    }

    public boolean sub_4FD7F0(int a1) {
        boolean v1; // zf@7

        if (a1 > 101100101) {
            if (a1 > 101110203) {
                if (a1 == 101120104) {
                    return true;
                }
                v1 = a1 - 101120104 == 100;
            } else {
                if (a1 == 101110203 || a1 == 101100201 || a1 == 101110102) {
                    return true;
                }
                v1 = a1 - 101110102 == 98;
            }
        } else {
            if (a1 == 101100101) {
                return true;
            }
            if (a1 > 0x4215FA) {
                if (a1 == 0x423923 + 4 || a1 == 0x423D06 + 6) {
                    return true;
                }
                v1 = a1 == 101000101;
            } else {
                if (a1 == 0x4215FA || a1 == 0x41C7D6 + 5 || a1 == 0x41EEEE) {
                    return true;
                }
                v1 = a1 == 0x421216 + 3;
            }
        }
        if (!v1) {
            return false;
        }
        return true;
    }

    public boolean isFourthJob() { // 274 idb 4fdaa0
        boolean result; // eax@2
        int a1 = id;
        int v2; // edi@9
        int v3; // ebx@9

        if (sub_4FD900(a1)
                || (a1 - 92000000) < 0xF4240 && (a1 % 10000) == 0
                || sub_4FDA20(a1)
                || sub_4FD870(a1)
                || sub_48AEF0(a1)
                || sub_4FD8B0(a1)) {
            result = false;
        } else {
            v2 = sub_48A160(a1);
            v3 = sub_489A10(v2);
            result = (v2 - 40000) > 5 && (sub_4FD7F0(a1) || v3 == 4 && !GameConstants.isZero(v2));
        }
        return result;
    }

    public Element getElement() {
        return element;
    }

    public int getAnimationTime() {
        return animationTime;
    }

    public int getMasterLevel() {
        return masterLevel;
    }

    public int getDelay() {
        return delay;
    }

    public int getTamingMob() {
        return eventTamingMob;
    }

    public boolean isBeginnerSkill() {
        int jobId = id / 10000;
        return GameConstants.isBeginnerJob(jobId);
    }

    public boolean isMagic() {
        return magic;
    }

    public boolean isMovement() {
        return casterMove;
    }

    public boolean isPush() {
        return pushTarget;
    }

    public boolean isPull() {
        return pullTarget;
    }

    public int getPsd() {
        return psd;
    }

    public String getPsdDamR() {
        return psdDamR;
    }

    public boolean isHyper() {
        return hyper;
    }

    public boolean isVMatrix() {
        return vSkill;
    }

    public boolean isNotCooltimeReset() {
        return notCooltimeReset;
    }

    public boolean isNotIncBuffDuration() {
        return notIncBuffDuration;
    }

    public boolean isSpecialSkill() {
        int jobId = id / 10000;
        return jobId == 900 || jobId == 800 || jobId == 9000 || jobId == 9200 || jobId == 9201 || jobId == 9202 || jobId == 9203 || jobId == 9204;
    }

    public List<MapleStatEffect> getEffects() {
        return effects;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Integer> getPsdSkills() {
        return psdSkills;
    }

    public void setPsdSkills(List<Integer> psdSkills) {
        this.psdSkills = psdSkills;
    }

    public boolean isChainAttack() {
        return chainAttack;
    }

    public void setChainAttack(boolean chainAttack) {
        this.chainAttack = chainAttack;
    }

    public int getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(int dataIndex) {
        this.dataIndex = dataIndex;
    }

    public int getEnableDelay() {
        return enableDelay;
    }

    public void setEnableDelay(int enableDelay) {
        this.enableDelay = enableDelay;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getGroupEvent() {
        return groupEvent;
    }

    public void setGroupEvent(int groupEvent) {
        this.groupEvent = groupEvent;
    }

    public int getAttackableCount() {
        return attackableCount;
    }

    public void setAttackableCount(int attackableCount) {
        this.attackableCount = attackableCount;
    }

    public List<Integer> getCustoms() {
        return customs;
    }

    public void setCustoms(List<Integer> customs) {
        this.customs = customs;
    }

    public List<Point> getExtraPos() {
        return extraPos;
    }

    public void setExtraPos(List<Point> extraPos) {
        this.extraPos = extraPos;
    }

    public boolean isIgnoreCounter() {
        return ignoreCounter;
    }
}
