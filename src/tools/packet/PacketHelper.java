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
package tools.packet;

import client.*;
import client.MapleTrait.MapleTraitType;
import client.inventory.*;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.Buffstat;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageManager;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.*;
import tools.*;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CWvsContext.BuffPacket;

import java.util.*;
import java.util.Map.Entry;

public class PacketHelper {

    public final static long FT_UT_OFFSET = 116445060000000000L; // EDT
    public final static long MAX_TIME = 150842304000000000L; // 00 80 05 BB 46 E6 17 02
    public final static long ZERO_TIME = 94354848000000000L; // 00 40 E0 FD 3B 37 4F 01
    public final static long PERMANENT = 150841440000000000L; // 00 C0 9B 90 7D E5 17 02
    public final static long ZERO_TIME_REVERSE = -153052018564450501L; // 3B 37 4F 01 00 40 E0 FD

    public static final long getKoreanTimestamp(final long realTimestamp) {
        return getTime(realTimestamp);
    }

    public static final long getTime(long realTimestamp) {
        if (realTimestamp == -1) {
            return MAX_TIME;
        } else if (realTimestamp == -2) {
            return ZERO_TIME;
        } else if (realTimestamp == -3) {
            return PERMANENT;
        } else if (realTimestamp == -4) {
            return ZERO_TIME_REVERSE;
        }
        return ((realTimestamp * 10000) + FT_UT_OFFSET);
    }

    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (SimpleTimeZone.getDefault().inDaylightTime(new Date())) {
            timeStampinMillis -= 3600000L;
        }
        long time;
        if (roundToMinutes) {
            time = (timeStampinMillis / 1000 / 60) * 600000000;
        } else {
            time = timeStampinMillis * 10000;
        }
        return time + FT_UT_OFFSET;
    }

    public static byte[] sendPacket(String args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(HexTool.getByteArrayFromHexString(args));
        return mplew.getPacket();
    }

    public static void addQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.write(1);
        mplew.writeShort(started.size());
        for (final MapleQuestStatus q : started) {
            mplew.writeInt(q.getQuest().getId()); //Short -> Int
            if (q.hasMobKills()) {
                StringBuilder sb = new StringBuilder();
                for (Iterator i$ = q.getMobKills().values().iterator(); i$.hasNext();) {
                    int kills = ((Integer) i$.next()).intValue();
                    sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
                }
                mplew.writeMapleAsciiString(sb.toString());
            } else {
                mplew.writeMapleAsciiString(q.getCustomData() == null ? "" : q.getCustomData());
            }

        }
        //0x4000
        mplew.write(1);
        final List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        for (MapleQuestStatus q : completed) {
            mplew.writeInt(q.getQuest().getId()); //Short -> Int
            mplew.writeLong(getTime(q.getCompletionTime()));
        }
    }

    public static final void addSkillInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.write(1); // true
        Map<Skill, SkillEntry> skills = chr.getSkills();

        mplew.writeShort(skills.size());
        for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            mplew.writeInt(skill.getKey().getId());
            mplew.writeInt(skill.getValue().skillevel);
            mplew.writeLong(getTime(skill.getValue().expiration));
            if (SkillFactory.sub_60A550(skill.getKey().getId())) {
                mplew.writeInt(skill.getValue().masterlevel);
            }
        }

        mplew.writeShort(0); // unk
        for (int i = 0; i < 0; i++) {
            mplew.writeInt(0);
            mplew.writeShort(0);
        }

        final List<Triple<Skill, SkillEntry, Integer>> linkskills = chr.getLinkSkills();
        mplew.writeInt(linkskills.size()); //보유한 링크스킬 목록
        for (Triple<Skill, SkillEntry, Integer> linkskil : linkskills) { // 여기는 문제 없음.
            addLinkSkillInfo(mplew, linkskil.getLeft().getId(), linkskil.getRight(), chr.getSkillLevel(linkskil.getLeft().getId()) == 0 ? linkskil.getRight() : chr.getId(), linkskil.getMid().skillevel);
        }
    }

    public static final void addLinkSkillInfo(final MaplePacketLittleEndianWriter mplew, int skillid, int sendid, int recvid, int level) {
        mplew.writeInt(sendid); // 시그너스 스킬 레벨
        mplew.writeInt(recvid);
        mplew.writeInt(skillid);
        mplew.writeShort(level); // 일반 스킬 레벨
        mplew.writeLong(PacketHelper.getTime(-2));
        mplew.writeInt(0); // 324++, 하루 제한 횟수 or 제한 한 수
    }

    public static final void addCoolDownInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
        mplew.writeShort(cd.size());
        for (final MapleCoolDownValueHolder cooling : cd) {
            mplew.writeInt(cooling.skillId);
            mplew.writeInt((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static final void addRocksInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        int[] mapz = chr.getRegRocks();
        for (int i = 0; i < 5; i++) {
            mplew.writeInt(mapz[i]);
        }

        int[] map = chr.getRocks();
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(map[i]);
        }

        int[] maps = chr.getHyperRocks();
        for (int i = 0; i < 13; i++) {
            mplew.writeInt(maps[i]);
        }
    }

    public static final void addRingInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {

        //01 00 = size
        //01 00 00 00 = gametype?
        //03 00 00 00 = win
        //00 00 00 00 = tie/loss
        //01 00 00 00 = tie/loss
        //16 08 00 00 = points
        //0x800
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
        List<MapleRing> cRing = aRing.getLeft();
        mplew.writeShort(cRing.size());
        for (MapleRing ring : cRing) { // 33
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
        }
        List<MapleRing> fRing = aRing.getMid();
        mplew.writeShort(fRing.size());
        for (MapleRing ring : fRing) { // 37
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 13);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
        List<MapleRing> mRing = aRing.getRight();
        mplew.writeShort(mRing.size());
        for (MapleRing ring : mRing) { // 48
            MarriageDataEntry data = MarriageManager.getInstance().getMarriage(chr.getMarriageId());
            if (data == null) {
                System.out.println(chr.getName() + " 캐릭터는 웨딩 데이터가 존재하지 않음.");
                mplew.writeZeroBytes(48);
            } else {
                mplew.writeInt(data.getMarriageId());
                mplew.writeInt(data.getBrideId() == chr.getId() ? data.getBrideId() : data.getGroomId());
                mplew.writeInt(data.getBrideId() == chr.getId() ? data.getGroomId() : data.getBrideId());
                mplew.writeShort(data.getStatus() == 2 ? 3 : data.getStatus()); //status 1 : 약혼  3 : 결혼
                mplew.writeInt(ring.getItemId());
                mplew.writeInt(ring.getItemId());
                mplew.writeAsciiString(data.getBrideId() == chr.getId() ? data.getBrideName() : data.getGroomName(), 13);
                mplew.writeAsciiString(data.getBrideId() == chr.getId() ? data.getGroomName() : data.getBrideName(), 13);
            }
        }
    }

    public static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        /*
         * //flag 8000 mplew.writeInt(0); // potion pot mplew.writeInt(chr.getId());
         * //v66 for (int i = 0; i < 3; i++) { mplew.writeInt(0); mplew.writeInt(0); }
         * mplew.writeInt(0); //v71 mplew.write(0); mplew.write(0); mplew.write(0);
         * //v102
         */

        // 모든 캐릭터는 최대치의 인벤토리를 갖는다.
        mplew.write(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit()); // equip slots
        mplew.write(chr.getInventory(MapleInventoryType.USE).getSlotLimit()); // use slots
        mplew.write(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit()); // set-up slots
        mplew.write(chr.getInventory(MapleInventoryType.ETC).getSlotLimit()); // etc slots
        mplew.write(chr.getInventory(MapleInventoryType.CASH).getSlotLimit()); // cash slots
        mplew.write(chr.getInventory(MapleInventoryType.DECORATION).getSlotLimit()); //치장 슬롯

        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)); // 0x200000
        // : int
        // + int
        // actually
        if (stat != null && stat.getCustomData() != null
                && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
            mplew.writeLong(getTime(Long.parseLong(stat.getCustomData())));
        } else {
            mplew.writeLong(getTime(-1)); // 모든 캐릭터는 펜던트 슬롯 2개 //getTime(-2)
        }

        mplew.write(0);
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (Item item : iv.list()) {
            if (GameConstants.isArcaneSymbol(item.getItemId()) || GameConstants.isAscenticSymbol(item.getItemId())) {
                chr.getSymbol().add((Equip) item);
            }
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }

        mplew.writeShort(0); // start of evan equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1000 && item.getPosition() > -1100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }

        mplew.writeShort(0); // start of mechanic equips, ty KDMS
        for (Item item : equipped) {
            if (item.getPosition() <= -1100 && item.getPosition() > -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }



        mplew.writeShort(0); // start of Beta Equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1500 && item.getPosition() > -1600) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0);
        for (Item item : equipped) {
            if (item.getPosition() <= -5000 && item.getPosition() >= -5002) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); //심볼
        for (Item item : equipped) {
            if (item.getPosition() <= -1600 && item.getPosition() > -1700) {
                chr.getAcSymbol().add((Equip) item);
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); //++342 어센틱
        for (Item item : equipped) {
            if (item.getPosition() <= -1700 && item.getPosition() > -1800) {
                chr.getAsSymbol().add((Equip) item);
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }

        mplew.writeShort(0); // ?
        mplew.writeShort(0); // ?
        mplew.writeShort(0); // ?

        mplew.write(0);

        for (Item item : equipped) {
            if (item.getPosition() <= -100 && item.getPosition() > -1000) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }

        mplew.writeShort(0);
        iv = chr.getInventory(MapleInventoryType.DECORATION);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }

        mplew.writeShort(0);
        mplew.writeShort(0);
        for (Item item : equipped) {
            if (item.getPosition() <= -1300 && item.getPosition() > -1400) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0);

        mplew.writeShort(0); // start of USE inventory
        iv = chr.getInventory(MapleInventoryType.USE);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); // start of set-up inventory
        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); // start of etc inventory
        iv = chr.getInventory(MapleInventoryType.ETC);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); // start of cash inventory
        iv = chr.getInventory(MapleInventoryType.CASH);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); // start of extended slots
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(chr.getExtendedSlots().size());
        for (int i = 0; i < chr.getExtendedSlots().size(); i++) {
            mplew.writeInt(i);
            mplew.writeInt(chr.getExtendedSlot(i));
            for (Item item : chr.getInventory(MapleInventoryType.ETC).list()) {
                if (item.getPosition() > 10000 && item.getPosition() < 10200) {
                    addItemPosition(mplew, item, false, true);
                    addItemInfo(mplew, item, chr);
                }
            }
            mplew.writeInt(-1);
        }
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0); // 아이템 팟 슬롯, v416
    }

    public static final void addCharStats(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        for (int i = 0; i < 2; i++) {
            mplew.writeInt(chr.getId()); //1.2.239+
        }
        mplew.writeInt(0);
        mplew.writeAsciiString(chr.getName(), 13);
        mplew.write(chr.getGender());
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        if (chr.getBaseColor() != -1) {
            mplew.writeInt(chr.getHair() / 10 * 10 + chr.getBaseColor());
        } else {
            mplew.writeInt(chr.getHair());
        }
        mplew.write(chr.getBaseColor());
        mplew.write(chr.getAddColor());
        mplew.write(chr.getBaseProb());
        mplew.writeInt(chr.getLevel());
        mplew.writeShort(chr.getJob());
        chr.getStat().connectData(mplew);
        mplew.writeShort(chr.getRemainingAp()); // remaining ap

        int size = chr.getRemainingSpSize();
        if (GameConstants.isSeparatedSp(chr.getJob())) {
            mplew.write(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    mplew.write(i + 1);
                    mplew.writeInt(chr.getRemainingSp(i));
                }
            }
        } else {
            mplew.writeShort(chr.getRemainingSp());
        }

        mplew.writeLong(chr.getExp());
        mplew.writeInt(chr.getFame());
        mplew.writeInt(GameConstants.isZero(chr.getJob()) ? chr.getStat().getMp() : 99999);
        mplew.writeInt(chr.getMapId());
        mplew.write(chr.getInitialSpawnpoint());
        mplew.writeShort(chr.getSubcategory());
        if (GameConstants.isDemonSlayer(chr.getJob()) || GameConstants.isXenon(chr.getJob()) || GameConstants.isDemonAvenger(chr.getJob())
                || GameConstants.isArk(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        } else if (GameConstants.isHoyeong(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        }

        mplew.write(0); //332++
        mplew.writeLong(getTime(-2)); //332++

        mplew.writeShort(chr.getFatigue());
        mplew.writeInt(chr.getCreateDate());

        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.writeInt(chr.getTrait(t).getTotalExp()); // total trait point
        }

        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.writeShort(chr.getTrait(t).getExp()); // today's trait points
        }

        mplew.writeZeroBytes(12); // 350 new

        mplew.write(0);
        mplew.writeLong(getTime(-2));

        //pvp data 수정 필요
        mplew.writeInt(0); //pvp exp
        mplew.write(10); //pvp rank
        mplew.writeInt(0); //pvp points

        mplew.write(5); //pvp level
        mplew.write(5); //pvp type

        mplew.writeInt(0); //event type

        mplew.writeLong(getTime(-4));
//        }
        //burning data
        String burningCid = chr.getClient().getKeyValue("TeraBurning");

        boolean isBurn = burningCid != null && Integer.parseInt(burningCid) == chr.getId();
        mplew.writeLong(getTime(isBurn ? System.currentTimeMillis() : -2)); // 시작 날짜
        mplew.writeLong(getTime( isBurn ? -1 : -1)); // 마지막 날짜
        mplew.writeInt(isBurn ? 10 : 0); //몇렙부터
        mplew.writeInt(isBurn ? 240 : 0); //몇렙까지
        mplew.writeInt(isBurn ? 2 : 0); //추가 레벨업 값
        mplew.write(isBurn ? 3 : 0);
        mplew.write(isBurn);
    }

    public static final void addCharLook(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr, final boolean mega, boolean second) {
        boolean isAlpha = GameConstants.isZero(chr.getJob()) && chr.getGender() == 0 && chr.getSecondGender() == 1;
        boolean isBeta = GameConstants.isZero(chr.getJob()) && chr.getGender() == 1 && chr.getSecondGender() == 0;
        mplew.write(second || isBeta ? chr.getSecondGender() : chr.getGender());
        mplew.write(second || isBeta ? chr.getSecondSkinColor() : chr.getSkinColor());
        mplew.writeInt(second || isBeta ? chr.getSecondFace() : chr.getFace());
        mplew.writeInt(chr.getJob());
        mplew.write(mega ? 0 : 1);
        if (second || isBeta) {
            int hair = chr.getSecondHair();
            if (chr.getSecondBaseColor() != -1) {
                hair = chr.getSecondHair() / 10 * 10 + chr.getSecondBaseColor();
            }
            mplew.writeInt(hair);
        } else {
            int hair = chr.getHair();
            if (chr.getBaseColor() != -1) {
                hair = chr.getHair() / 10 * 10 + chr.getBaseColor();
            }
            mplew.writeInt(hair);
        }
        final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> totemEquip = new LinkedHashMap<>();
        final Map<Short, Integer> equip = second ? chr.getSecondEquips() : chr.getEquips();
        for (final Entry<Short, Integer> item : equip.entrySet()) {
            if (item.getKey() < -2000) {
                continue;
            }
            short pos = (short) (item.getKey() * -1);
            Equip item_ = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -pos);
            if (item_ == null) {
                continue;
            }
            if (GameConstants.isAngelicBuster(chr.getJob()) && second) {
                if ((pos >= 1300) && (pos < 1400)) {
                    pos = (short) (pos - 1300);
                    switch (pos) {
                        case 0:
                            pos = 1;
                            break;
                        case 1:
                            pos = 9;
                            break;
                        case 4:
                            pos = 8;
                            break;
                        case 5:
                            pos = 3;
                            break;
                        case 6:
                            pos = 4;
                            break;
                        case 7:
                            pos = 5;
                            break;
                        case 8:
                            pos = 6;
                            break;
                        case 9:
                            pos = 7;
                            break;
                    }
                    if (myEquip.get((byte) pos) != null) {
                        maskedEquip.put((byte) pos, myEquip.get((byte) pos));
                    }
                    String lol = ((Integer) item.getValue()).toString();
                    String ss = lol.substring(0, 3);
                    int moru = Integer.parseInt(ss + ((Integer) item_.getMoru()).toString());
                    myEquip.put((byte) pos, item_.getMoru() != 0 ? moru : item.getValue());
                } else if (((pos > 100) && (pos < 200)) && (pos != 111)) {
                    pos = (short) (pos - 100);
                    switch (pos) {
                        case 10:
                        case 12:
                        case 13:
                        case 15:
                        case 16:
                            if (myEquip.get((byte) pos) != null) {
                                maskedEquip.put((byte) pos, myEquip.get((byte) pos));
                            }
                            String lol = ((Integer) item.getValue()).toString();
                            String ss = lol.substring(0, 3);
                            int moru = Integer.parseInt(ss + ((Integer) item_.getMoru()).toString());
                            myEquip.put((byte) pos, item_.getMoru() != 0 ? moru : item.getValue());
                            break;
                    }
                }
                if ((pos < 100)) {
                    if (myEquip.get((byte) pos) == null) {
                        String lol = ((Integer) item.getValue()).toString();
                        String ss = lol.substring(0, 3);
                        int moru = Integer.parseInt(ss + ((Integer) item_.getMoru()).toString());
                        myEquip.put((byte) pos, item_.getMoru() != 0 ? moru : item.getValue());
                    } else {
                        maskedEquip.put((byte) pos, item.getValue());
                    }
                }
            } else if (isBeta) {
                //제로이면서 베타일 때
                if ((pos < 100) && (myEquip.get((byte) pos) == null)) {
                    String lol = ((Integer) item.getValue()).toString();
                    String ss = lol.substring(0, 3);
                    int moru = Integer.parseInt(ss + ((Integer) item_.getMoru()).toString());
                    myEquip.put((byte) pos, item_.getMoru() != 0 ? moru : item.getValue());
                } else if (pos > 1500 && pos != 1511) {
                    if (pos > 1500) {
                        pos = (short) (pos - 1500);
                    }
                    myEquip.put((byte) pos, item.getValue());
                }

            } else if (isAlpha || (GameConstants.isAngelicBuster(chr.getJob()) && !second) || (!GameConstants.isZero(chr.getJob()) && !GameConstants.isAngelicBuster(chr.getJob()))) {
                //엔버 드레스업이 아니거나, 제로 알파이거나, 나머지 직업일 때
                if ((pos < 100) && (myEquip.get((byte) pos) == null)) {
                    String lol = ((Integer) item.getValue()).toString();
                    String ss = lol.substring(0, 3);
                    int moru = Integer.parseInt(ss + ((Integer) item_.getMoru()).toString());
                    myEquip.put((byte) pos, item_.getMoru() != 0 ? moru : item.getValue());
                    //myEquip.put((byte) pos, item.getValue());
                } else if ((pos > 100) && (pos != 111)) {

                    pos -= 100;
                    if (myEquip.get((byte) pos) != null) {
                        maskedEquip.put((byte) pos, myEquip.get((byte) pos));
                    }
                    String lol = ((Integer) item.getValue()).toString();
                    String ss = lol.substring(0, 3);
                    int moru = Integer.parseInt(ss + ((Integer) item_.getMoru()).toString());
                    myEquip.put((byte) pos, item_.getMoru() != 0 ? moru : item.getValue());

                    /*pos = (byte) (pos - 100);
                     if (myEquip.get(pos) != null) {
                     maskedEquip.put((byte) pos, myEquip.get(pos));
                     }
                     myEquip.put((byte) pos, item.getValue());*/
                } else if (myEquip.get((byte) pos) != null) {
                    maskedEquip.put((byte) pos, item.getValue());
                }
            }
        }
        for (final Entry<Byte, Integer> totem : chr.getTotems().entrySet()) {
            byte pos = (byte) ((totem.getKey()).byteValue() * -1);
            if (pos < 0 || pos > 2) { //3 totem slots
                continue;
            }
            if (totem.getValue() < 1200000 || totem.getValue() >= 1210000) {
                continue;
            }
            totemEquip.put(Byte.valueOf(pos), totem.getValue());
        }

        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            int weapon = ((Integer) entry.getValue()).intValue();

            if (isAlpha && (GameConstants.getWeaponType(weapon) == MapleWeaponType.BIG_SWORD)) {
                continue;
            } else if (isBeta && (GameConstants.getWeaponType(weapon) == MapleWeaponType.LONG_SWORD)) {
                continue;
            } else if (isBeta && (GameConstants.getWeaponType(weapon) == MapleWeaponType.BIG_SWORD)) {
                mplew.write(11);
                mplew.writeInt(((Integer) entry.getValue()).intValue());
            } else {
                mplew.write(((Byte) entry.getKey()).byteValue());
                mplew.writeInt(((Integer) entry.getValue()).intValue());
            }
        }
        mplew.write(-1);

        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(((Byte) entry.getKey()).byteValue());
            mplew.writeInt(((Integer) entry.getValue()).intValue());
        }
        mplew.write(-1);

        if (isBeta) {
            Integer cWeapon = equip.get((short) -1511);
            mplew.writeInt(cWeapon != null ? cWeapon.intValue() : 0);
            Integer Weapon = equip.get((short) -11);
            mplew.writeInt(Weapon != null ? Weapon.intValue() : 0);
            mplew.writeInt(0);
        } else {
            Integer cWeapon = equip.get((short) -111);
            mplew.writeInt(cWeapon != null ? cWeapon.intValue() : 0);
            Integer Weapon = equip.get((short) -11);
            mplew.writeInt(Weapon != null ? Weapon.intValue() : 0);
            Integer Shield = equip.get((short) -10);
            if (GameConstants.isZero(chr.getJob()) || Shield == null) {
                mplew.writeInt(0);
            } else {
                mplew.writeInt(Shield.intValue());
            }
        }

        mplew.writeInt(0);//엘프귀
        mplew.writeInt(chr.getKeyValue(100229, "hue")); //324++ 핑크빈 색 바꾸기
        mplew.write(second ? chr.getSecondBaseColor() : chr.getBaseColor());//

        for (int i = 0; i < 3; i++) {
            if (chr.getPet(i) != null) {
                mplew.writeInt(chr.getPet(i).getPetItemId());
            } else {
                mplew.writeInt(0);
            }
        }

        if (GameConstants.isDemonSlayer(chr.getJob()) || GameConstants.isXenon(chr.getJob()) || GameConstants.isDemonAvenger(chr.getJob())
                || GameConstants.isArk(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        } else if (GameConstants.isHoyeong(chr.getJob())) {
            mplew.writeInt(chr.getDemonMarking());
        } else if (GameConstants.isZero(chr.getJob())) {
            mplew.write(second || isBeta ? chr.getSecondGender() : chr.getGender());
        }

        mplew.write(second || isBeta ? chr.getSecondAddColor() : chr.getAddColor());
        mplew.write(second || isBeta ? chr.getSecondBaseProb() : chr.getBaseProb());
        mplew.writeInt(0);
    }

    public static final void addExpirationTime(final MaplePacketLittleEndianWriter mplew, final long time) {
        mplew.writeLong(getTime(time));
    }

    public static void addItemPosition(final MaplePacketLittleEndianWriter mplew, final Item item, final boolean trade, final boolean bagSlot) {
        if (item == null) {
            mplew.write(0);
            return;
        }
        short pos = item.getPosition();
        if (pos <= -1) {
            pos *= -1;
            if (pos > 100 && pos < 1000) {
                pos -= 100;
            }
        }
        if (bagSlot) {
            mplew.writeInt((pos % 100) - 1);
        } else if (!trade && item.getType() == 1) {
            mplew.writeShort(pos);
        } else {
            mplew.writeShort(pos);
        }
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item) {
        addItemInfo(mplew, item, null);
    }

    public static final void addItemInfo(final MaplePacketLittleEndianWriter mplew, final Item item, final MapleCharacter chr) {
        mplew.write(item.getPet() != null ? 3 : item.getType());
        mplew.writeInt(item.getItemId());

        boolean hasUniqueId = item.getUniqueId() > 0 && !GameConstants.isMarriageRing(item.getItemId()) && item.getItemId() / 10000 != 166;
        mplew.write(hasUniqueId ? 1 : 0);
        if (hasUniqueId) {
            mplew.writeLong(item.getUniqueId());
        }

        if (item.getPet() != null) {
            addPetItemInfo(mplew, chr, item, item.getPet(), true, false);
        } else {
            addExpirationTime(mplew, item.getExpiration());
            mplew.writeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(item.getItemId()));
            mplew.write(item.getType() == 1 || item.getItemId() == 4001886);
            if (item.getType() == 1) {
                final Equip equip = Equip.calculateEquipStats((Equip) item);
                addEquipStats(mplew, equip);
                addEquipBonusStats(mplew, equip, hasUniqueId, chr);
                addAndroidInfo(mplew, equip, chr);
            } else if (item.getItemId() == 4001886) {
                if (item.getReward() != null) {
                    mplew.writeInt(1);
                    mplew.writeMapleAsciiString(item.getOwner());
                    mplew.writeLong(item.getReward().getObjectId());
                    mplew.writeInt(0);
                } else {
                    mplew.writeInt(1);
                    mplew.writeMapleAsciiString(item.getOwner());
                    mplew.writeLong(1);
                    mplew.writeInt(0);
                }
            } else {
                mplew.writeShort(item.getQuantity());
                mplew.writeMapleAsciiString(item.getOwner());
                mplew.writeShort(item.getFlag());
                mplew.writeInt(0);
                if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId()) || item.getItemId() / 10000 == 287) {
                    mplew.writeLong(item.getInventoryId() <= 0 ? -1 : item.getInventoryId());
                }
            }
        }
    }

    public static void addEquipStats(MaplePacketLittleEndianWriter mplew, Equip equip) {
        int head = 0;
        if (equip.getStats().size() > 0) {
            for (EquipStat stat : equip.getStats()) {
                head |= stat.getValue();
            }
        }

        mplew.writeInt(head);

        if (head != 0) {
            if (equip.getStats().contains(EquipStat.SLOTS)) {
                mplew.write(equip.getUpgradeSlots());
            }
            if (equip.getStats().contains(EquipStat.LEVEL)) {
                mplew.write(equip.getLevel());
            }
            if (equip.getStats().contains(EquipStat.STR)) {
                mplew.writeShort(equip.getTotalStr());
            }
            if (equip.getStats().contains(EquipStat.DEX)) {
                mplew.writeShort(equip.getTotalDex());
            }
            if (equip.getStats().contains(EquipStat.INT)) {
                mplew.writeShort(equip.getTotalInt());
            }
            if (equip.getStats().contains(EquipStat.LUK)) {
                mplew.writeShort(equip.getTotalLuk());
            }
            if (equip.getStats().contains(EquipStat.MHP)) {
                mplew.writeShort(equip.getTotalHp());
            }
            if (equip.getStats().contains(EquipStat.MMP)) {
                mplew.writeShort(equip.getTotalMp());
            }
            if (equip.getStats().contains(EquipStat.WATK)) {
                mplew.writeShort(equip.getTotalWatk());
            }
            if (equip.getStats().contains(EquipStat.MATK)) {
                mplew.writeShort(equip.getTotalMatk());
            }
            if (equip.getStats().contains(EquipStat.WDEF)) {
                mplew.writeShort(equip.getTotalWdef());
            }
            if (equip.getStats().contains(EquipStat.MDEF)) {
                mplew.writeShort(equip.getTotalMdef());
            }
            if (equip.getStats().contains(EquipStat.ACC)) {
                mplew.writeShort(equip.getTotalAcc());
            }
            if (equip.getStats().contains(EquipStat.AVOID)) {
                mplew.writeShort(equip.getTotalAvoid());
            }
            if (equip.getStats().contains(EquipStat.HANDS)) {
                mplew.writeShort(equip.getHands());
            }
            if (equip.getStats().contains(EquipStat.SPEED)) {
                mplew.writeShort(equip.getSpeed());
            }
            if (equip.getStats().contains(EquipStat.JUMP)) {
                mplew.writeShort(equip.getJump());
            }
            if (equip.getStats().contains(EquipStat.FLAG)) {
                mplew.writeShort(equip.getFlag());
            }
            if (equip.getStats().contains(EquipStat.INC_SKILL)) {
                mplew.write(equip.getIncSkill() > 0 ? 1 : 0);
            }
            if (equip.getStats().contains(EquipStat.ITEM_LEVEL)) {
                mplew.write(Math.max(equip.getBaseLevel(), equip.getEquipLevel()));
            }
            if (equip.getStats().contains(EquipStat.ITEM_EXP)) {
                mplew.writeLong(equip.getExpPercentage() * 100000);
            }
            if (equip.getStats().contains(EquipStat.DURABILITY)) {
                mplew.writeInt(equip.getDurability());
            }
            if (equip.getStats().contains(EquipStat.VICIOUS_HAMMER)) {
                mplew.writeInt(equip.getViciousHammer());
            }
            if (equip.getStats().contains(EquipStat.PVP_DAMAGE)) {
                mplew.writeShort(equip.getPVPDamage());
            }
            if (equip.getStats().contains(EquipStat.DOWNLEVEL)) {
                mplew.write(-equip.getReqLevel());
            }
            if (equip.getStats().contains(EquipStat.ENHANCT_BUFF)) {
                mplew.writeShort(equip.getEnchantBuff());
            }
            if (equip.getStats().contains(EquipStat.DURABILITY_SPECIAL)) {
                mplew.writeInt(equip.getDurability());
            }
            if (equip.getStats().contains(EquipStat.REQUIRED_LEVEL)) {
                mplew.write(equip.getReqLevel());
            }
            if (equip.getStats().contains(EquipStat.YGGDRASIL_WISDOM)) {
                mplew.write(equip.getYggdrasilWisdom());
            }
            if (equip.getStats().contains(EquipStat.FINAL_STRIKE)) {
                mplew.write(equip.getFinalStrike());
            }
            if (equip.getStats().contains(EquipStat.IndieBdr)) {
                mplew.write(equip.getBossDamage());
            }
            if (equip.getStats().contains(EquipStat.IGNORE_PDR)) {
                mplew.write(equip.getIgnorePDR());
            }
        }
        addEquipSpecialStats(mplew, equip);
    }

    public static void addEquipSpecialStats(MaplePacketLittleEndianWriter mplew, Equip equip) {
        int head = 0;
        if (equip.getSpecialStats().size() > 0) {
            for (EquipSpecialStat stat : equip.getSpecialStats()) {
                head |= stat.getValue();
            }
        }
        mplew.writeInt(head);
        if (head != 0) {
            if (equip.getSpecialStats().contains(EquipSpecialStat.TOTAL_DAMAGE)) {
                mplew.write(equip.getTotalDamage());
            }
            if (equip.getSpecialStats().contains(EquipSpecialStat.ALL_STAT)) {
                mplew.write(equip.getAllStat());
            }
            if (equip.getSpecialStats().contains(EquipSpecialStat.KARMA_COUNT)) {
                mplew.write(equip.getKarmaCount());
            }
            if (equip.getSpecialStats().contains(EquipSpecialStat.REBIRTH_FIRE)) {
                mplew.writeLong(equip.getFire());
            }
            if (equip.getSpecialStats().contains(EquipSpecialStat.EQUIPMENT_TYPE)) {
                mplew.writeInt(equip.getEquipmentType());
            }
        }
    }

    public static void addEquipBonusStats(MaplePacketLittleEndianWriter mplew, Equip equip, boolean hasUniqueId, MapleCharacter chr) {
        mplew.writeMapleAsciiString(equip.getOwner());
        mplew.write(equip.getState());
        mplew.write(equip.getEnhance());
        mplew.writeShort(equip.getPotential1());
        mplew.writeShort(equip.getPotential2());
        mplew.writeShort(equip.getPotential3());
        mplew.writeShort(equip.getPotential4());
        mplew.writeShort(equip.getPotential5());
        mplew.writeShort(equip.getPotential6());
        mplew.writeShort(equip.getMoru());

        if (!hasUniqueId) {
            mplew.writeLong(equip.getInventoryId() < 0 ? -1 : equip.getInventoryId()); //-1 보내면 리턴스크롤이 안발리는 좆버그가 있따..
        }

        mplew.writeLong(0);
        mplew.writeLong(getTime(-2));
        mplew.writeZeroBytes(16);
        mplew.writeShort(equip.getSoulName());
        mplew.writeShort(equip.getSoulEnchanter());
        mplew.writeShort(equip.getSoulPotential());

        if (GameConstants.isArcaneSymbol(equip.getItemId()) || GameConstants.isAscenticSymbol(equip.getItemId())) {
            mplew.writeShort(equip.getArc());
            mplew.writeInt(equip.getArcEXP());
            mplew.writeShort(equip.getArcLevel());
        }

        mplew.write(-1);
        mplew.write(-1);
        mplew.writeLong(PacketHelper.getTime(-1));
        mplew.writeLong(PacketHelper.getTime(-2));
        mplew.writeLong(PacketHelper.getTime(-1));
    }

    public static void addAndroidInfo(MaplePacketLittleEndianWriter mplew, Equip equip, MapleCharacter chr) {
        if (equip.getItemId() >= 1662000 && equip.getItemId() < 1663000) {
            MapleAndroid android = equip.getAndroid();
            mplew.writeShort(android != null ? android.getSkin() : 0);
            mplew.writeShort(android != null ? android.getHair() - 30000 : 0);
            mplew.writeShort(android != null ? android.getFace() - 20000 : 0);
            mplew.writeMapleAsciiString(android != null ? android.getName() : "");
            mplew.writeInt(android != null ? android.getEar() ? 0 : 1032024 : 0);
            mplew.writeLong(getTime(-2));
        }
    }

    public static final void serializeMovementList(final MaplePacketLittleEndianWriter lew, final List<LifeMovementFragment> moves) {
        lew.writeShort(moves.size()); // 351 modify
        for (LifeMovementFragment move : moves) {
            move.serialize(lew);
        }
    }

    public static final void addAnnounceBox(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        if (chr.getPlayerShop() != null && chr.getPlayerShop().isOwner(chr) && chr.getPlayerShop().getShopType() != 1 && chr.getPlayerShop().isAvailable()) {
            addInteraction(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
    }

    public static final void addInteraction(final MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop) {
        mplew.write(shop.getGameType());
        mplew.writeInt(((AbstractPlayerStore) shop).getObjectId());
        mplew.writeMapleAsciiString(shop.getDescription());
        if (shop.getShopType() != 1) {
            mplew.write(shop.getPassword().length() > 0 ? 1 : 0); //password = false
        }
        if (shop.getItemId() == 5250500) {
            mplew.write(0);
        } else if (shop.getItemId() == 4080100) {
            mplew.write(((MapleMiniGame) shop).getPieceType());
        } else if (shop.getItemId() >= 4080000 && shop.getItemId() < 4080100) {
            mplew.write(((MapleMiniGame) shop).getPieceType());
        } else {
            mplew.write(shop.getItemId() % 10);
        }
        mplew.write(shop.getSize()); //current size
        mplew.write(shop.getMaxSize()); //full slots... 4 = 4-1=3 = has slots, 1-1=0 = no slots
        if (shop.getShopType() != 1) {
            mplew.write(shop.isOpen() ? 0 : 1);
        }

        //325++
        ChatPacket(mplew, shop.getOwnerName(), "[미니룸]" + shop.getDescription());
    }

    public static final void addCharacterInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) { //완벼크

        mplew.writeLong(-1); // flag

        mplew.write(0); // nCombatOrders
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(-3); // 펫 액티브 스킬 쿨타임
        }

        mplew.write(0); // 추가패킷 필요.
        mplew.writeInt(0); // 추가패킷 필요.
        mplew.write(0); // 추가패킷 필요.

        // flag 1
        addCharStats(mplew, chr); // 완벽

        mplew.write(chr.getBuddylist().getCapacity());

        if (chr.getBlessOfFairyOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
        } else {
            mplew.write(0);
        }

        if (chr.getBlessOfEmpressOrigin() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
        } else {
            mplew.write(0);
        }

        final MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
        if (ultExplorer != null && ultExplorer.getCustomData() != null) {
            mplew.write(1);
            mplew.writeMapleAsciiString(ultExplorer.getCustomData());
        } else {
            mplew.write(0);
        }
        // flag 2
        mplew.writeLong(chr.getMeso()); // mesos

        mplew.writeInt(0); // 4 ints per size dummyBLD

        // flag 8
        addInventoryInfo(mplew, chr);
        //          mplew.write(HexTool.getByteArrayFromHexString("00 00 01 02 00 49 00 00 00 00 00 00 00 00 80 05 BB 46 E6 17 02 0C 00 00 00 00 00 00 00 00 80 05 BB 46 E6 17 02 00 00 00 00 00 00 00 00 01 31 00 2C 3B 00 00 00 00 0B A6 07 00 00 00 13 A6 07 00 00 00 1B A6 07 00 00 00 5F 38 00 00 00 00 A5 81 00 00 00 00 99 3E 00 00 00 00 63 25 00 00 01 00 30 D9 7D 00 00 01 00 31 0C A6 07 00 00 00 14 A6 07 00 00 00 1C A6 07 00 00 00 A6 81 00 00 00 00 D6 79 00 00 01 00 30 0D A6 07 00 00 00 15 A6 07 00 00 00 1D A6 07 00 00 00 18 51 00 00 01 00 30 93 3E 00 00 00 00 9B 3E 00 00 00 00 C5 1D 00 00 00 00 0E A6 07 00 00 00 16 A6 07 00 00 00 1E A6 07 00 00 00 6D 65 00 00 01 00 30 07 A6 07 00 00 00 0F A6 07 00 00 00 17 A6 07 00 00 00 1F A6 07 00 00 00 95 3E 00 00 00 00 9D 3E 00 00 00 00 F1 81 00 00 00 00 08 A6 07 00 00 00 76 A4 00 00 01 00 30 10 A6 07 00 00 00 FA 49 00 00 00 00 18 A6 07 00 00 00 09 A6 07 00 00 00 11 A6 07 00 00 00 19 A6 07 00 00 00 97 3E 00 00 00 00 6B 1B 00 00 01 00 30 7A 1C 00 00 01 00 30 F3 81 00 00 00 00 DB 81 00 00 00 00 0A A6 07 00 00 00 A8 65 00 00 00 00 12 A6 07 00 00 00 1A A6 07 00 00 00 01 06 00 1B 1E 00 00 00 40 A5 62 36 E5 D6 01 EA 41 00 00 00 66 ED 74 9D 3A D6 01 B0 88 01 00 00 66 ED 74 9D 3A D6 01 EB 41 00 00 00 66 ED 74 9D 3A D6 01 EC 41 00 00 00 66 ED 74 9D 3A D6 01 CA 7D 00 00 00 28 00 3A C7 3A D6 01 00 00 00 00 00 00 00 00"));

        addSkillInfo(mplew, chr); // 완벽

        //0x8000
        addCoolDownInfo(mplew, chr); // 완벽

        // 0x200 && 0x4000
        addQuestInfo(mplew, chr); // 완벽

        // 0x400
        mplew.writeShort(0); //GW_MiniGameRecord //ok

        //0x800
        addRingInfo(mplew, chr); // 완벽

        // 0x1000
        addRocksInfo(mplew, chr); // 완벽

        // 0x40000
        chr.QuestInfoPacket(mplew);//ok

        // 0x2000
        mplew.writeShort(0); //ok

        //0x80000
        mplew.writeShort(0);//ok

        mplew.write(1); //334++  v292 1이면 플래그 100화긴 0이면 안감.

        // 0x100
        mplew.writeInt(0); // 스트링 에 인트하나 붙는거 사이즈

        //0x1000 완벽
        mplew.writeInt(0);//0으로 바꾸면 아래 두개 안감.
//        mplew.writeInt(chr.getAccountID()); // accid chr.getAccountID()
//        mplew.writeInt(-1); //334++

        // 0x200000 와벽
        if (GameConstants.isWildHunter(chr.getJob())) {
            addJaguarInfo(mplew, chr);
        }

        // 0x800 완벽
        if (GameConstants.isZero(chr.getJob())) {
            addZeroInfo(mplew, chr);
        }

        // 0x4000000
        mplew.writeShort(0); // 4Byte -> 2Byte 완벽 GW_NpcShopBuyLimit

        mplew.writeShort(0); // 305 ++ ?gotoLabel_659 후에나옴 얘 안드가면 바로 아래로.

        // 0x20000000
        addStealSkills(mplew, chr); // 위치 확실 INT 20개 나와야되는데

        mplew.writeInt(0); // 350 new

        // 0x80000000
        addAbilityInfo(mplew, chr); // 위치 확실

        // 0x10000
        mplew.writeShort(0); //ok

        mplew.writeInt(0); // v342 MONSTERLIFE_INVITEINFO::DecodeMonsterLifeInviteInfo

        mplew.write(0); // ((_BYTE *)v429 + 3827)

        // flag 1
        addHonorInfo(mplew, chr); // 위치확실 ok

        // flag 2
        mplew.write(1); //1
        mplew.writeShort(0);

        // flag 4
        mplew.write(chr.returnscroll != null); //ReturnEffectInfo::Decode
        if (chr.returnscroll != null) {
            PacketHelper.addItemInfo(mplew, chr.returnscroll);
            mplew.writeInt(chr.returnSc);
        }

        // flag 8 GW_DressUpInfo::Decode
        boolean tr = GameConstants.isAngelicBuster(chr.getJob());
        mplew.writeInt(tr ? chr.getSecondFace() : 0); //nFace
        int hair = chr.getSecondHair();
        if (chr.getSecondBaseColor() != -1) {
            hair = chr.getSecondHair() / 10 * 10 + chr.getSecondBaseColor();
        }
        mplew.writeInt(tr ? hair : 0); //nHair
        mplew.writeInt(tr ? 1051291 : 0); // dressup suit cant unequip nClothe
        mplew.write(tr ? chr.getSecondSkinColor() : 0); //Skin
        mplew.writeInt(chr.getSecondBaseColor()); //nMixBaseHairColor
        mplew.writeInt(chr.getSecondAddColor()); //>nMixAddHairColor
        mplew.writeInt(chr.getSecondBaseProb()); //MixHairBasePro

        // flag 0x10
        mplew.writeShort(0);
        mplew.writeShort(0);

        // flag 0x20
        mplew.write(0);

        // flag 0x40
        addFarmInfo(mplew, chr.getClient(), 0);
        mplew.writeInt(0); // -1 => 1
        mplew.writeInt(0);

        // flag 0x80 MemorialCubeInfo::Decode
        mplew.write(chr.choicepotential != null && chr.memorialcube != null);
        if (chr.choicepotential != null && chr.memorialcube != null) {
            PacketHelper.addItemInfo(mplew, chr.choicepotential);
            mplew.writeInt(chr.memorialcube.getItemId());
            mplew.writeInt(chr.choicepotential.getPosition());
        }

        //sub_5DAAB0
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        // flag  0x400  sub_5DB9A0
        mplew.writeInt(0);
        mplew.writeShort(0); //342??
        mplew.writeLong(getTime(-2));
        mplew.writeInt(0);

        //flag 0x2000 RunnerGameRecord::Decode
        mplew.writeInt(chr.getId());
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeLong(getTime(-2));
        mplew.writeInt(10);

        //flag 0x8000
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        //end

        mplew.writeShort(0); // 350 new
        mplew.writeShort(0); // 350 new
        mplew.writeShort(0); // 350 new
        mplew.writeShort(0); // 350 new

        // 블록버스터 Act 패킷.
        mplew.writeShort(0);

        //flag 0x4000
        mplew.writeShort(0);

        // 몬스터라이프 온라인여부
//        mplew.write(chr.getClient().isFarm()); 1124 제거?

        //CharacterData::DecodeTextEquipInfo
        mplew.writeInt(0);

        //flag 0x100000
        mplew.writeShort(0);

        //flag 0x200000
        addMatrixInfo(mplew, chr);

        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeInt(0);

        mplew.writeInt(0);

//        // 324 ++
//        addHairRoomInfo(mplew, chr);
//        addFaceRoomInfo(mplew, chr);
//
//        //333++
//        addSkinRoomInfo(mplew, chr);

        mplew.write(HexTool.getByteArrayFromHexString("09 03 01 00 00 00 00 00 00 FF FF 00 01 00 00 00 00 00 00 FF FF 00 01 00 00 00 00 00 00 FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00"));
        mplew.write(HexTool.getByteArrayFromHexString("09 03 01 00 00 00 00 00 00 FF FF 00 01 00 00 00 00 00 00 FF FF 00 01 00 00 00 00 00 00 FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00"));

        mplew.write(HexTool.getByteArrayFromHexString("06 03 01 00 00 00 00 00 00 FF FF 00 01 00 00 00 00 00 00 FF FF 00 01 00 00 00 00 00 00 FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00"));

        //332++ emogi
        mplew.writeInt(0); // size
        mplew.writeInt(0); // size
        mplew.writeInt(8); // 350 new
        mplew.writeShort(0);

        mplew.writeInt(0); // size

        mplew.writeInt(0); // size

    }

    public static void addFaceRoomInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(9); // 사용할 수 있는 최대 사이즈

        mplew.write(chr.getFaceRoom().size());

        for (int i = 1; i <= 9; ++i) {
            if (chr.getFaceRoom().size() < i) {
                mplew.write(false);
            } else {
                mplew.write(i);
                MapleMannequin hair = chr.getFaceRoom().get(i - 1);
                mplew.write(0);//chr.getGender());
                mplew.write(0);//chr.getSkinColor());
                mplew.writeInt(hair.getValue());
                mplew.write(hair.getBaseProb());
                mplew.write(hair.getBaseColor());
                mplew.write(hair.getAddColor());
            }
        }
    }

    public static void addHairRoomInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(9); // 사용할 수 있는 최대 사이즈

        mplew.write(chr.getHairRoom().size());

        for (int i = 1; i <= 9; ++i) {
            if (chr.getHairRoom().size() < i) {
                mplew.write(false);
            } else {

                mplew.write(i); // slot

                MapleMannequin hair = chr.getHairRoom().get(i - 1);

                mplew.write(0);//chr.getGender());
                mplew.write(0);//chr.getSkinColor());
                mplew.writeInt(hair.getValue());
                mplew.write(hair.getBaseProb());
                mplew.write(hair.getBaseColor());
                mplew.write(hair.getAddColor());
            }
        }
    }

    public static void addSkinRoomInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(8); // 사용할 수 있는 최대 사이즈

        mplew.write(chr.getSkinRoom().size());

        for (int i = 1; i <= 9; ++i) {
            if (chr.getSkinRoom().size() < i) {
                mplew.write(false);
            } else {

                mplew.write(i); // slot

                MapleMannequin skin = chr.getSkinRoom().get(i - 1);

                mplew.write(0);//chr.getGender());
                mplew.write(0);//chr.getSkinColor());
                mplew.writeInt(skin.getValue());
                mplew.write(skin.getBaseProb());
                mplew.write(skin.getBaseColor());
                mplew.write(skin.getAddColor());
            }
        }
    }

    public static void addMatrixInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getCore().size());
        for (Core m : chr.getCore()) {
            mplew.writeLong(m.getCrcId());
            mplew.writeInt(m.getCoreId());
            mplew.writeInt(m.getLevel());
            mplew.writeInt(m.getExp());
            mplew.writeInt(m.getState());
            mplew.writeInt(m.getSkill1());
            mplew.writeInt(m.getSkill2());
            mplew.writeInt(m.getSkill3());
            mplew.writeInt(m.getPosition());
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000));
            mplew.write(0);//342++ corelock
        }

        mplew.writeInt(chr.getMatrixs().size());

        for (VMatrix matrix : chr.getMatrixs()) {
            mplew.writeInt(matrix.getId()); // 코어ID
            mplew.writeInt(matrix.getPosition()); // 매트릭스 POS
            mplew.writeInt(matrix.getLevel()); // 강화 Lv
            mplew.write(matrix.isUnLock()); // 메소 주고 추가로 뚫은 칸인지 여부
        }
    }

    public static void addFarmInfo(MaplePacketLittleEndianWriter mplew, MapleClient c, int idk) {

        mplew.writeMapleAsciiString(c.getFarmName()); //농장 이름
        mplew.writeInt(0);
        mplew.writeInt(1); //농장 레벨
        mplew.writeInt(10); //농장 경험치
        mplew.writeInt(0); //농장 포인트
        mplew.writeInt(0); //잼

        mplew.writeInt(0); // FarmGender
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(1);
    }

    public static void addZeroInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeShort(-1); // flag
        mplew.write(chr.getGender());
        mplew.writeInt(chr.getStat().getHp()); //TODO :: 현재 HP
        mplew.writeInt(chr.getStat().getMp()); //TODO :: 현재 MP
        mplew.write(chr.getSecondSkinColor());
        int hair = chr.getSecondHair();
        if (chr.getSecondBaseColor() != -1) {
            hair = chr.getSecondHair() / 10 * 10 + chr.getSecondBaseColor();
        }
        mplew.writeInt(hair);
        mplew.writeInt(chr.getSecondFace());
        mplew.writeInt(chr.getStat().getMaxHp());
        mplew.writeInt(chr.getStat().getMaxMp());
        mplew.writeInt(0);
        //262 ++
        mplew.writeInt(chr.getSecondBaseColor());
        mplew.writeInt(chr.getSecondAddColor());
        mplew.writeInt(chr.getSecondBaseProb());
    }

    public static void addAbilityInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final List<InnerSkillValueHolder> skills = chr.getInnerSkills();
        mplew.writeShort(skills.size());
        for (int i = 0; i < skills.size(); ++i) {
            mplew.write(i + 1); // key
            mplew.writeInt(skills.get(i).getSkillId()); //d 7000000 id ++, 71 = char cards
            mplew.write(skills.get(i).getSkillLevel()); // level
            mplew.write(skills.get(i).getRank()); //rank, C, B, A, and S
        }
    }

    public static void addHonorInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getHonorLevel()); //honor lvl
        mplew.writeInt(chr.getHonourExp()); //honor exp
    }

    public static void addStolenSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int jobNum) {
        int count = 0;
        if (chr.getStolenSkills() != null) {
            for (Pair<Integer, Boolean> sk : chr.getStolenSkills()) {
                if (GameConstants.getJobNumber(sk.left) == jobNum) {
                    mplew.writeInt(sk.left);
                    count++;
                    if (count >= GameConstants.getNumSteal(jobNum)) {
                        break;
                    }
                }
            }
        }
        while (count < GameConstants.getNumSteal(jobNum)) { //for now?
            mplew.writeInt(0);
            count++;
        }
    }

    public static void addChosenSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        for (int i = 1; i <= 5; i++) {
            boolean found = false;
            if (chr.getStolenSkills() != null) {
                for (Pair<Integer, Boolean> sk : chr.getStolenSkills()) {
                    if (GameConstants.getJobNumber(sk.left) == i && sk.right) {
                        mplew.writeInt(sk.left);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                mplew.writeInt(0);
            }
        }
    }

    public static void addStealSkills(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        for (int i = 1; i <= 5; i++) { //262 ++
            addStolenSkills(mplew, chr, i); // 60 훔친 스킬목록
        }
        //0x10000000
        addChosenSkills(mplew, chr); // 20 장착중인 스킬목록
    }

    public static final void addPetItemInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter player, final Item item, final MaplePet pet, boolean unequip, boolean petLoot) {
        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            PacketHelper.addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1 : item.getExpiration());
        }

        mplew.writeInt(-1);
        mplew.write(1);
        mplew.writeAsciiString(pet.getName() == null ? "" : pet.getName(), 13);
        mplew.write(pet.getLevel());
        mplew.writeShort(pet.getCloseness());
        mplew.write(pet.getFullness());

        if (item == null) {
            mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
        } else {
            mplew.writeLong(PacketHelper.getTime(item.getExpiration()));
        }

        mplew.writeShort(0);
        mplew.writeShort(pet.getFlags());
        mplew.writeInt(0);
        mplew.writeShort(item != null && ItemFlag.KARMA_USE.check(item.getFlag()) ? 1 : 0);
        mplew.write(unequip ? 0 : player.getPetIndex(pet) + 1);

        if (player == null) {
            mplew.writeInt(0);
        } else {
            mplew.writeInt(pet.getBuffSkillId());
        }

        mplew.writeInt(pet.getColor());
        mplew.writeShort(pet.getPetSize());
        mplew.writeShort(pet.getWonderGrade());

        /*mplew.write(HexTool.getByteArrayFromHexString("" +
                "00 80 05 BB 46 E6 17 02 " +
                "FF FF FF FF " +
                "01 " +
                "BF AC BE EE 20 C3 CA B9 E4 00 00 00 00 " +
                "07 " +
                "4C 00 " +
                "58 " +
                "00 A4 16 C6 5D 1B D7 01 " +
                "00 00 " +
                "85 00 " +
                "00 00 00 00 " +
                "00 00 " +
                "01 " +
                "FA 15 42 00 " +
                "FF FF FF FF " +
                "64 00 " +
                "00 00"));*/

        // System.out.println(mplew);

    }

    public static void addShopInfo(final MaplePacketLittleEndianWriter mplew, final MapleShop shop, final MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        mplew.writeShort(shop.getItems().size());// + c.getPlayer().getRebuy().size());
        for (MapleShopItem item : shop.getItems()) {
            addShopItemInfo(mplew, item, shop, ii, null, c.getPlayer());
        }
        /*        int i = 0;
         for (Item item : c.getPlayer().getRebuy()) {
         i++;
         addShopItemInfo(mplew, new MapleShopItem((short) 1000, item.getItemId(), (int) ii.getPrice(item.getItemId()), 0, (byte) 0, (short) item.getQuantity(), 0, i), shop, ii, item, c.getPlayer());
         }*/
    }

    public static void addShopItemInfo(final MaplePacketLittleEndianWriter mplew, final MapleShopItem item, final MapleShop shop, final MapleItemInformationProvider ii, final Item i, final MapleCharacter chr) {
        mplew.writeInt(0); // 남은 물량 갯수

        //sub_7C21F0
        mplew.writeInt(item.getItemId()); // itemId
        mplew.writeInt(item.getTab()); // 307++
        mplew.writeInt(0); // 남은 물량 갯수
        mplew.writeInt(0); // 구입 후 사용 가능한 일수
        mplew.writeInt(0); // 307++

        /*
         * nPrice
         * nTokenItemId
         * nTokenPrice
         */
        if (shop.getCoinKey() > 0) { // 포인트 상점
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        } else if (item.getPriceQuantity() > 0) { // 코인 상점
            mplew.writeInt(0);
            mplew.writeInt(item.getPrice());
            mplew.writeInt(item.getPriceQuantity());
        } else { // 메소 상점
            mplew.writeInt(item.getPrice());
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        mplew.writeInt(shop.getCoinKey()); // nPointQuestID
        mplew.writeInt(shop.getCoinKey() > 0 ? item.getPrice() : 0); // nPointPrice
        mplew.writeInt(0); // nStarCoin

        /* 307 */
        mplew.write(shop.getSaleString().length() > 0);
        if (shop.getSaleString().length() > 0) {
            //sub_75F190
            mplew.writeInt(30);
            mplew.write(1);
            mplew.write(0);
            mplew.writeMapleAsciiString("");

            mplew.writeInt(0);
            mplew.writeMapleAsciiString("");

            mplew.writeLong(getTime(-2L));
            mplew.writeLong(getTime(-2L));
//            mplew.writeMapleAsciiString(shop.getSaleString()); 325에서 사라짐?

            mplew.writeInt(0); // size, 세일에 나온 날짜를 getTime으로 보내는데.. 필요한가?
        }

        mplew.writeInt(0); // n개 구입 가능
        mplew.writeInt(0); // 월드 내 남은 수량 n개
        mplew.writeInt(7); // 307++

        mplew.writeShort(0); // ShowLevMin
        mplew.writeShort(0); // ShowLevMax

        //sub_75E530
        mplew.write(0); //DecodeResetInfo

        mplew.write(0); //307 ++

        mplew.writeLong(getTime(-2L)); //ftSellStart
        mplew.writeLong(getTime(-1L)); //ftSellEnd

        mplew.writeInt(0); // TabIndex
        mplew.writeShort(1);
        mplew.write(0); // 307++

        /* 1029랑 순서 바뀜 */
        mplew.writeInt(shop.getQuestEx()); // QuestExID
        mplew.writeMapleAsciiString(shop.getShopString()); // QuestExKey
        mplew.writeInt(item.getItemRate()); // QuestExValue
        mplew.writeInt(0); // ItemPeriod

        mplew.write(0); // 324 ++

        mplew.writeShort(0); // 351 new

        if ((!GameConstants.isThrowingStar(item.getItemId())) && (!GameConstants.isBullet(item.getItemId()))) {
            mplew.writeShort(item.getQuantity() > 1 ? item.getQuantity() : 1); //nQuantity
            mplew.writeShort(item.getBuyable()); // nMaxPerSlot
        } else {
            //8Byte : dUnitPrice
            mplew.write(HexTool.getByteArrayFromHexString("9A 99 99 99 99 99")); // 6Byte
            mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));// 2Byte

            //nMaxPerSlot
            mplew.writeShort(ii.getSlotMax(item.getItemId()));
        }

        mplew.writeLong(getTime(-1L)); // new

        //nUserLevel
        mplew.write(i == null ? 0 : 1);
        if (i != null) {
            addItemInfo(mplew, i);
        }
    }

    public static final void addJaguarInfo(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.write(chr.getInfoQuest(123456).equals("") ? 0 : Byte.parseByte(chr.getInfoQuest(123456)));
        for (int i = 0; i < 5; i++) {
            mplew.writeInt(0);
        }
    }

    public static <E extends Buffstat> void writeMonsterMask(MaplePacketLittleEndianWriter mplew, Map<E, MonsterStatusEffect> statups) {
        int[] mask = new int[GameConstants.MAX_MOB_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        for (int i = GameConstants.MAX_BUFFSTAT; i >= 1; i--) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0); //자리 위치가 안맞을시 무조건 0
        }
    }

    public static <E extends Buffstat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Pair<Integer, Integer>>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Pair<Integer, Integer>> statup : statups) {
            mask[statup.left.getPosition() - 1] |= statup.left.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition() - 1] |= statup.getValue();
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[i - 1]);
        }
    }

    public static List<Pair<MapleBuffStat, Pair<Integer, Integer>>> sortBuffStats(Map<MapleBuffStat, Pair<Integer, Integer>> statups) {
        List<Pair<MapleBuffStat, Pair<Integer, Integer>>> statvals = new ArrayList<>();
        /*        //항상 첫번째에 오는 버프스탯 여기에 추가.
         List<MapleBuffStat> alwaysFirst = Collections.unmodifiableList(Arrays.asList(
         MapleBuffStat.IndiePad, MapleBuffStat.IndieMad, MapleBuffStat.IndieAcc, MapleBuffStat.IndieEva, MapleBuffStat.IndieSpeed, MapleBuffStat.IndieJump,
         MapleBuffStat.EnhancedPad, MapleBuffStat.EnhancedMad, MapleBuffStat.EnhancedPdd, MapleBuffStat.EnhancedMaxHp, MapleBuffStat.EnhancedMaxMp,
         MapleBuffStat.IndieHp, MapleBuffStat.IndieMp, MapleBuffStat.IndieHpR, MapleBuffStat.IndieMpR,
         MapleBuffStat.IndieDamR, MapleBuffStat.IndieBooster, MapleBuffStat.Infinity
         ));*/
        for (Entry<MapleBuffStat, Pair<Integer, Integer>> stat : statups.entrySet()) {
            statvals.add(new Pair<>(stat.getKey(), stat.getValue()));
        }
        //TODO: 버프스탯 조합 방식이 더 있을 수 있으므로, 추후에 분석 요망.
        boolean changed;
        int i;
        int k;
        //정렬 소스 시작.
        do {
            changed = false;
            i = 0;
            k = 1;
            for (int iter = 0; iter < statvals.size() - 1; iter++) {
                Pair<MapleBuffStat, Pair<Integer, Integer>> a = statvals.get(i);
                Pair<MapleBuffStat, Pair<Integer, Integer>> b = statvals.get(k);
                if (a != null && b != null) {
                    if (a.left.getFlag() > b.left.getFlag()) { //만약 a가 b보다 크다면 -> a가 뒤에 놓여야 한다.
                        Pair<MapleBuffStat, Pair<Integer, Integer>> swap = new Pair<>(a.left, a.right);
                        statvals.remove(i);
                        statvals.add(i, b);
                        statvals.remove(k);
                        statvals.add(k, swap);
                        changed = true;
                    }
                }
                i++;
                k++;
            }
        } while (changed);

        return statvals;
    }

    public static List<Pair<MapleBuffStat, List<MapleBuffStatValueHolder>>> sortIndieBuffStats(Map<MapleBuffStat, List<MapleBuffStatValueHolder>> statups) {
        List<Pair<MapleBuffStat, List<MapleBuffStatValueHolder>>> statvals = new ArrayList<>();
        /*        //항상 첫번째에 오는 버프스탯 여기에 추가.
         List<MapleBuffStat> alwaysFirst = Collections.unmodifiableList(Arrays.asList(
         MapleBuffStat.IndiePad, MapleBuffStat.IndieMad, MapleBuffStat.IndieAcc, MapleBuffStat.IndieEva, MapleBuffStat.IndieSpeed, MapleBuffStat.IndieJump,
         MapleBuffStat.EnhancedPad, MapleBuffStat.EnhancedMad, MapleBuffStat.EnhancedPdd, MapleBuffStat.EnhancedMaxHp, MapleBuffStat.EnhancedMaxMp,
         MapleBuffStat.IndieHp, MapleBuffStat.IndieMp, MapleBuffStat.IndieHpR, MapleBuffStat.IndieMpR,
         MapleBuffStat.IndieDamR, MapleBuffStat.IndieBooster, MapleBuffStat.Infinity
         ));*/
        for (Entry<MapleBuffStat, List<MapleBuffStatValueHolder>> stat : statups.entrySet()) {
            statvals.add(new Pair<>(stat.getKey(), stat.getValue()));
        }
        //TODO: 버프스탯 조합 방식이 더 있을 수 있으므로, 추후에 분석 요망.
        boolean changed;
        int i;
        int k;
        //정렬 소스 시작.
        do {
            changed = false;
            i = 0;
            k = 1;
            for (int iter = 0; iter < statvals.size() - 1; iter++) {
                Pair<MapleBuffStat, List<MapleBuffStatValueHolder>> a = statvals.get(i);
                Pair<MapleBuffStat, List<MapleBuffStatValueHolder>> b = statvals.get(k);
                if (a != null && b != null) {
                    if (a.left.getFlag() > b.left.getFlag()) { //만약 a가 b보다 크다면 -> a가 뒤에 놓여야 한다.
                        Pair<MapleBuffStat, List<MapleBuffStatValueHolder>> swap = new Pair<>(a.left, a.right);
                        statvals.remove(i);
                        statvals.add(i, b);
                        statvals.remove(k);
                        statvals.add(k, swap);
                        changed = true;
                    }
                }
                i++;
                k++;
            }
        } while (changed);

        return statvals;
    }

    public static void ArcaneSymbol(final MaplePacketLittleEndianWriter mplew, Item item) {
        Equip equip = (Equip) item;
        mplew.writeInt(0);
        mplew.writeInt(equip.getArcLevel()); // 레벨
        mplew.writeInt((equip.getArcLevel() * equip.getArcLevel()) + 11); // 성장치
        mplew.writeLong(12440000 + (6600000 * equip.getArcLevel())); // 필요메소
        mplew.writeLong(0);
        for (byte i = 0; i < 12; i++) {
            mplew.writeShort(0);
        }
    }

    public static void addSymbolInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        for (Equip symbol : chr.getSymbol()) {
            mplew.writeInt(symbol.getPosition());
            ArcaneSymbol(mplew, symbol);
        }
        mplew.writeInt(0);
    }

    public static void encodeForRemote(MaplePacketLittleEndianWriter mplew, Map<MapleBuffStat, Pair<Integer, Integer>> statups, MapleCharacter chr) {
        if (statups.containsKey(MapleBuffStat.Speed)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.Speed));
        }
        if (statups.containsKey(MapleBuffStat.ComboCounter)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.ComboCounter));
        }

        if (statups.containsKey(MapleBuffStat.WeaponCharge)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.WeaponCharge));
        }

        if (statups.containsKey(MapleBuffStat.BlessedHammer)) {
            mplew.writeShort(chr.getElementalCharge());
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlessedHammer));
        }

        if (statups.containsKey(MapleBuffStat.SnowCharge)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SnowCharge));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SnowCharge));
        }

        if (statups.containsKey(MapleBuffStat.ElementalCharge)) // ElementCharge
        {
            mplew.writeShort(chr.getElementalCharge());
        }
        if (statups.containsKey(MapleBuffStat.Stun)) // Stun
        {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Stun));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Stun));
        }
        if (statups.containsKey(MapleBuffStat.Shock)) // Shock
        {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.Shock));
        }

        if (statups.containsKey(MapleBuffStat.Darkness)) // darkness
        {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Darkness));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Darkness));
        }
        if (statups.containsKey(MapleBuffStat.Seal)) // seal
        {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Seal));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Seal));
        }
        if (statups.containsKey(MapleBuffStat.Weakness)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Weakness));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Weakness));
        }
        if (statups.containsKey(MapleBuffStat.WeaknessMdamage)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.WeaknessMdamage));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.WeaknessMdamage));
        }
        if (statups.containsKey(MapleBuffStat.Curse)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Curse));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Curse));
        }
        if (statups.containsKey(MapleBuffStat.Slow)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Slow));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Slow));
        }
        if (statups.containsKey(MapleBuffStat.PvPRaceEffect)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.PvPRaceEffect));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PvPRaceEffect));
        }
        if (statups.containsKey(MapleBuffStat.TimeBomb)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.TimeBomb));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.TimeBomb));
        }
        if (statups.containsKey(MapleBuffStat.Team)) // Team
        {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.Team));
        }
        if (statups.containsKey(MapleBuffStat.DisOrder)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DisOrder));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DisOrder));
        }
        if (statups.containsKey(MapleBuffStat.Thread)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Thread));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Thread));
        }
        if (statups.containsKey(MapleBuffStat.Poison)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Poison));
        }
        if (statups.containsKey(MapleBuffStat.Poison)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Poison));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Poison));
        }
        if (statups.containsKey(MapleBuffStat.ShadowPartner)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ShadowPartner));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ShadowPartner));
        }
        if (statups.containsKey(MapleBuffStat.Morph)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Morph));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Morph));
        }
        if (statups.containsKey(MapleBuffStat.Ghost)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Ghost));
        }
        if (statups.containsKey(MapleBuffStat.Attract)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Attract));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Attract));
        }
        if (statups.containsKey(MapleBuffStat.Magnet)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Magnet));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Magnet));
        }
        if (statups.containsKey(MapleBuffStat.MagnetArea)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.MagnetArea));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MagnetArea));
        }
        if (statups.containsKey(MapleBuffStat.NoBulletConsume)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.NoBulletConsume));
        }
        if (statups.containsKey(MapleBuffStat.BanMap)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BanMap));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BanMap));
        }
        if (statups.containsKey(MapleBuffStat.Barrier)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Barrier));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Barrier));
        }
        if (statups.containsKey(MapleBuffStat.DojangShield)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DojangShield));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DojangShield));
        }
        if (statups.containsKey(MapleBuffStat.ReverseInput)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ReverseInput));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ReverseInput));
        }
        if (statups.containsKey(MapleBuffStat.RespectPImmune)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RespectPImmune));
        }
        if (statups.containsKey(MapleBuffStat.RespectMImmune)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RespectMImmune));
        }
        if (statups.containsKey(MapleBuffStat.DefenseAtt)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DefenseAtt));
        }
        if (statups.containsKey(MapleBuffStat.DefenseState)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DefenseState));
        }
        if (statups.containsKey(MapleBuffStat.DojangBerserk)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DojangBerserk));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DojangBerserk));
        }
        if (statups.containsKey(MapleBuffStat.RepeatEffect)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.RepeatEffect));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RepeatEffect));
        }
        if (statups.containsKey(MapleBuffStat.StopPortion)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.StopPortion));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StopPortion));
        }
        if (statups.containsKey(MapleBuffStat.StopMotion)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.StopMotion));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StopMotion));
        }
        if (statups.containsKey(MapleBuffStat.Fear)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Fear));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Fear));
        }
        if (statups.containsKey(MapleBuffStat.MagicShield)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MagicShield));
        }
        if (statups.containsKey(MapleBuffStat.Frozen)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Frozen));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Frozen));
        }
        if (statups.containsKey(MapleBuffStat.Frozen2)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Frozen2));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Frozen2));
        }
        if (statups.containsKey(MapleBuffStat.Web)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Web));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Web));
        }
        if (statups.containsKey(MapleBuffStat.DrawBack)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DrawBack));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DrawBack));
        }
        if (statups.containsKey(MapleBuffStat.FinalCut)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.FinalCut));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.FinalCut));
        }
        if (statups.containsKey(MapleBuffStat.OnCapsule)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.OnCapsule));
        }
        if (statups.containsKey(MapleBuffStat.Mechanic)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Mechanic));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Mechanic));
        }
        if (statups.containsKey(MapleBuffStat.Inflation)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Inflation));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Inflation));
        }
        if (statups.containsKey(MapleBuffStat.Explosion)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Explosion));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Explosion));
        }
        if (statups.containsKey(MapleBuffStat.DarkTornado)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DarkTornado));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DarkTornado));
        }
        if (statups.containsKey(MapleBuffStat.AmplifyDamage)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AmplifyDamage));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AmplifyDamage));
        }
        if (statups.containsKey(MapleBuffStat.HideAttack)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HideAttack));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.HideAttack));
        }
        if (statups.containsKey(MapleBuffStat.DevilishPower)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DevilishPower));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DevilishPower));
        }
        if (statups.containsKey(MapleBuffStat.SpiritLink)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SpiritLink));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SpiritLink));
        }
        if (statups.containsKey(MapleBuffStat.Event)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Event));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Event));
        }
        if (statups.containsKey(MapleBuffStat.Event2)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Event2));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Event2));
        }
        if (statups.containsKey(MapleBuffStat.DeathMark)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DeathMark));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DeathMark));
        }
        if (statups.containsKey(MapleBuffStat.PainMark)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.PainMark));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PainMark));
        }
        if (statups.containsKey(MapleBuffStat.Lapidification)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Lapidification));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Lapidification));
        }
        if (statups.containsKey(MapleBuffStat.VampDeath)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.VampDeath));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.VampDeath));
        }
        if (statups.containsKey(MapleBuffStat.VampDeathSummon)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.VampDeathSummon));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.VampDeathSummon));
        }
        if (statups.containsKey(MapleBuffStat.VenomSnake)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.VenomSnake));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.VenomSnake));
        }
        if (statups.containsKey(MapleBuffStat.PyramidEffect)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PyramidEffect));
        }
        if (statups.containsKey(MapleBuffStat.KillingPoint)) {
            mplew.write(chr.killingpoint);
        }
        if (statups.containsKey(MapleBuffStat.PinkbeanRollingGrade)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.PinkbeanRollingGrade));
        }
        if (statups.containsKey(MapleBuffStat.IgnoreTargetDEF)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IgnoreTargetDEF));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IgnoreTargetDEF));
        }
        if (statups.containsKey(MapleBuffStat.Invisible)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Invisible));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Invisible));
        }
        if (statups.containsKey(MapleBuffStat.Judgement)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Judgement));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Judgement));
        }
        if (statups.containsKey(MapleBuffStat.KeyDownAreaMoving)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.KeyDownAreaMoving));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.KeyDownAreaMoving));
        }
        if (statups.containsKey(MapleBuffStat.StackBuff)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.StackBuff));
        }
        if (statups.containsKey(MapleBuffStat.BlessOfDarkness)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlessOfDarkness));
        }
        if (statups.containsKey(MapleBuffStat.Larkness)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Larkness));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Larkness));
        }
        if (statups.containsKey(MapleBuffStat.ReshuffleSwitch)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ReshuffleSwitch));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ReshuffleSwitch));
        }
        if (statups.containsKey(MapleBuffStat.SpecialAction)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SpecialAction));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SpecialAction));
        }
        if (statups.containsKey(MapleBuffStat.StopForceAtominfo)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.StopForceAtominfo));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StopForceAtominfo));
        }

        if (statups.containsKey(MapleBuffStat.SoulGazeCriDamR)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SoulGazeCriDamR));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SoulGazeCriDamR));
        }

        if (statups.containsKey(MapleBuffStat.BossShield)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BossShield));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BossShield));
        }
        if (statups.containsKey(MapleBuffStat.PowerTransferGauge)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.PowerTransferGauge));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PowerTransferGauge));
        }
        if (statups.containsKey(MapleBuffStat.BlitzShield)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BlitzShield));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlitzShield));
        }
        if (statups.containsKey(MapleBuffStat.AffinitySlug)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AffinitySlug));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AffinitySlug));
        }
        if (statups.containsKey(MapleBuffStat.SoulExalt)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SoulExalt));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SoulExalt));
        }
        if (statups.containsKey(MapleBuffStat.HiddenPieceOn)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HiddenPieceOn));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.HiddenPieceOn));
        }
        if (statups.containsKey(MapleBuffStat.SmashStack)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SmashStack));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SmashStack));
        }
        if (statups.containsKey(MapleBuffStat.MobZoneState)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.MobZoneState));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MobZoneState));
        }
        if (statups.containsKey(MapleBuffStat.GiveMeHeal)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.GiveMeHeal));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.GiveMeHeal));
        }
        if (statups.containsKey(MapleBuffStat.TouchMe)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.TouchMe));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.TouchMe));
        }
        if (statups.containsKey(MapleBuffStat.Contagion)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Contagion));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Contagion));
        }
        if (statups.containsKey(MapleBuffStat.Contagion)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Contagion));
        }
        if (statups.containsKey(MapleBuffStat.ComboUnlimited)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ComboUnlimited));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ComboUnlimited));
        }
        if (statups.containsKey(MapleBuffStat.IgnorePCounter)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IgnorePCounter));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IgnorePCounter));
        }
        if (statups.containsKey(MapleBuffStat.IgnoreAllCounter)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IgnoreAllCounter));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IgnoreAllCounter));
        }
        if (statups.containsKey(MapleBuffStat.IgnorePImmune)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IgnorePImmune));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IgnorePImmune));
        }
        if (statups.containsKey(MapleBuffStat.IgnoreAllImmune)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IgnoreAllImmune));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IgnoreAllImmune));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat6)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat6));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat6));
        }
        if (statups.containsKey(MapleBuffStat.FireAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.FireAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.FireAura));
        }
        if (statups.containsKey(MapleBuffStat.HeavensDoor)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HeavensDoor));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.HeavensDoor));
        }
        if (statups.containsKey(MapleBuffStat.DamAbsorbShield)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DamAbsorbShield));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DamAbsorbShield));
        }
        if (statups.containsKey(MapleBuffStat.AntiMagicShell)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AntiMagicShell));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AntiMagicShell));
        }
        if (statups.containsKey(MapleBuffStat.NotDamaged)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.NotDamaged));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.NotDamaged));
        }
        if (statups.containsKey(MapleBuffStat.BleedingToxin)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BleedingToxin));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BleedingToxin));
        }
        if (statups.containsKey(MapleBuffStat.WindBreakerFinal)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.WindBreakerFinal));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.WindBreakerFinal));
        }
        if (statups.containsKey(MapleBuffStat.IgnoreMobDamR)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IgnoreMobDamR));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IgnoreMobDamR));
        }
        if (statups.containsKey(MapleBuffStat.Asura)) // asura
        {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Asura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Asura));
        }
        if (statups.containsKey(MapleBuffStat.MegaSmasher)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.MegaSmasher));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MegaSmasher));
        }
        if (statups.containsKey(MapleBuffStat.MegaSmasher)) {
            mplew.writeInt(0); // 333++
        }
        if (statups.containsKey(MapleBuffStat.UnityOfPower)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnityOfPower));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnityOfPower));
        }
        if (statups.containsKey(MapleBuffStat.Stimulate)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Stimulate));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Stimulate));
        }
        if (statups.containsKey(MapleBuffStat.ReturnTeleport)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ReturnTeleport));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ReturnTeleport));
        }
        if (statups.containsKey(MapleBuffStat.CapDebuff)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.CapDebuff));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.CapDebuff));
        }
        if (statups.containsKey(MapleBuffStat.OverloadCount)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.OverloadCount));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.OverloadCount));
        }
        if (statups.containsKey(MapleBuffStat.FireBomb)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.FireBomb));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.FireBomb));
        }
        if (statups.containsKey(MapleBuffStat.SurplusSupply)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.SurplusSupply));
        }
        if (statups.containsKey(MapleBuffStat.NewFlying)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.NewFlying));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.NewFlying));
        }
        if (statups.containsKey(MapleBuffStat.NaviFlying)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.NaviFlying));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.NaviFlying));
        }
        if (statups.containsKey(MapleBuffStat.AmaranthGenerator)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AmaranthGenerator));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AmaranthGenerator));
        }
        if (statups.containsKey(MapleBuffStat.CygnusElementSkill)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.CygnusElementSkill));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.CygnusElementSkill));
        }
        if (statups.containsKey(MapleBuffStat.StrikerHyperElectric)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.StrikerHyperElectric));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StrikerHyperElectric));
        }
        if (statups.containsKey(MapleBuffStat.EventPointAbsorb)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.EventPointAbsorb));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.EventPointAbsorb));
        }
        if (statups.containsKey(MapleBuffStat.EventAssemble)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.EventAssemble));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.EventAssemble));
        }
        if (statups.containsKey(MapleBuffStat.Albatross)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Albatross));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Albatross));
        }
        if (statups.containsKey(MapleBuffStat.Translucence)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Translucence));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Translucence));
        }
        if (statups.containsKey(MapleBuffStat.PoseType)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.PoseType));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PoseType));
        }
        if (statups.containsKey(MapleBuffStat.LightOfSpirit)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.LightOfSpirit));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.LightOfSpirit));
        }
        if (statups.containsKey(MapleBuffStat.ElementSoul)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ElementSoul));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ElementSoul));
        }
        if (statups.containsKey(MapleBuffStat.GlimmeringTime)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.GlimmeringTime));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.GlimmeringTime));
        }
        if (statups.containsKey(MapleBuffStat.Reincarnation)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Reincarnation));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Reincarnation));
        }
        if (statups.containsKey(MapleBuffStat.Beholder)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Beholder));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Beholder));
        }
        if (statups.containsKey(MapleBuffStat.QuiverCatridge)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.QuiverCatridge));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.QuiverCatridge));
        }
        if (statups.containsKey(MapleBuffStat.ArmorPiercing)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ArmorPiercing));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ArmorPiercing));
        }
        if (statups.containsKey(MapleBuffStat.ImmuneBarrier)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ImmuneBarrier));
        }
        if (statups.containsKey(MapleBuffStat.ImmuneBarrier)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ImmuneBarrier));
        }
        if (statups.containsKey(MapleBuffStat.FullSoulMP)) {
            Equip weapon = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            int skillid = weapon != null ? weapon.getSoulSkill() : 0;
            MapleStatEffect skill = SkillFactory.getSkill(skillid).getEffect(chr.getSkillLevel(skillid));
            if (chr.getBuffedValue(MapleBuffStat.SoulMP) < skill.getSoulMPCon()) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            } else {
                mplew.writeInt(skillid);
                mplew.writeInt(0);
            }

        }
        if (statups.containsKey(MapleBuffStat.AntiMagicShell)) // antimagicshell
        {
            mplew.write(chr.getAntiMagicShell()); //boolean
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.Dance)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.Dance));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Dance));
        }
        if (statups.containsKey(MapleBuffStat.SpiritGuard)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.SpiritGuard));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SpiritGuard));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat9)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat9));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat9));
        }
        if (statups.containsKey(MapleBuffStat.ComboTempest)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.ComboTempest));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ComboTempest));
        }
        if (statups.containsKey(MapleBuffStat.HalfstatByDebuff)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HalfstatByDebuff));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.HalfstatByDebuff));
        }
        if (statups.containsKey(MapleBuffStat.ComplusionSlant)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ComplusionSlant));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ComplusionSlant));
        }
        if (statups.containsKey(MapleBuffStat.JaguarSummoned)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.JaguarSummoned));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.JaguarSummoned));
        }
        if (statups.containsKey(MapleBuffStat.BombTime)) //408
        {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BombTime));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BombTime));
        }
        if (statups.containsKey(MapleBuffStat.Transform)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Transform));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Transform));
        }
        if (statups.containsKey(MapleBuffStat.EnergyBurst)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.EnergyBurst));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.EnergyBurst));
        }
        if (statups.containsKey(MapleBuffStat.Striker1st)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Striker1st));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Striker1st));
        }
        if (statups.containsKey(MapleBuffStat.BulletParty)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BulletParty));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BulletParty));
        }
        if (statups.containsKey(MapleBuffStat.SelectDice)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SelectDice));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SelectDice));
        }
        if (statups.containsKey(MapleBuffStat.Pray)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Pray));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Pray));
        }
        if (statups.containsKey(MapleBuffStat.DarkLighting)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DarkLighting));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DarkLighting));
        }
        if (statups.containsKey(MapleBuffStat.AttackCountX)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AttackCountX));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AttackCountX));
        }
        if (statups.containsKey(MapleBuffStat.FireBarrier)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.FireBarrier));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.FireBarrier));
        }
        if (statups.containsKey(MapleBuffStat.KeyDownMoving)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.KeyDownMoving));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.KeyDownMoving));
        }
        if (statups.containsKey(MapleBuffStat.MichaelSoulLink)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.MichaelSoulLink));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MichaelSoulLink));
        }
        if (statups.containsKey(MapleBuffStat.KinesisPsychicEnergeShield)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.KinesisPsychicEnergeShield));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.KinesisPsychicEnergeShield));
        }
        if (statups.containsKey(MapleBuffStat.BladeStance)) // bladestance
        {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BladeStance));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BladeStance));
        }
        if (statups.containsKey(MapleBuffStat.BladeStance)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BladeStance));
        }
        if (statups.containsKey(MapleBuffStat.Fever)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Fever));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Fever));
        }
        if (statups.containsKey(MapleBuffStat.AdrenalinBoost)) { // 343
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AdrenalinBoost));
        }
        if (statups.containsKey(MapleBuffStat.RwBarrier)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RwBarrier));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat12)) { // 343
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat12));
        }
        if (statups.containsKey(MapleBuffStat.RwMagnumBlow)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RwMagnumBlow));
        }
        if (statups.containsKey(MapleBuffStat.GuidedArrow)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.GuidedArrow));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.GuidedArrow));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat4)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat4));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat4));
        }
        if (statups.containsKey(MapleBuffStat.BlessMark)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BlessMark));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlessMark));
        }
        if (statups.containsKey(MapleBuffStat.BonusAttack)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BonusAttack));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BonusAttack));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat5)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat5));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat5));
        }
        if (statups.containsKey(MapleBuffStat.Stigma)) {
            mplew.writeShort(chr.Stigma);
            mplew.writeInt(237); //chr.getBuffSource(MapleBuffStat.Stigma)
        }
        if (statups.containsKey(MapleBuffStat.HolyUnity)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HolyUnity));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.HolyUnity));
        }
        if (statups.containsKey(MapleBuffStat.RhoAias)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.RhoAias));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RhoAias));
        }
        if (statups.containsKey(MapleBuffStat.PsychicTornado)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PsychicTornado));
        }
        if (statups.containsKey(MapleBuffStat.InstallMaha)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.InstallMaha));
        }
        if (statups.containsKey(MapleBuffStat.OverloadMana)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.OverloadMana));
        }
        if (statups.containsKey(MapleBuffStat.TrueSniping)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.TrueSniping));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.TrueSniping));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat16)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat16));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat16));
        }
        if (statups.containsKey(MapleBuffStat.Spotlight)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Spotlight));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Spotlight));
        }
        if (statups.containsKey(MapleBuffStat.Overload)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Overload));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Overload));
        }
        if (statups.containsKey(MapleBuffStat.FreudsProtection)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.FreudsProtection));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.FreudsProtection));
        }
        if (statups.containsKey(MapleBuffStat.BlessedHammer2)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BlessedHammer2));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlessedHammer2));
        }
        if (statups.containsKey(MapleBuffStat.OverDrive)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.OverDrive));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.OverDrive));
        }
        if (statups.containsKey(MapleBuffStat.Etherealform)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Etherealform));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Etherealform));
        }
        if (statups.containsKey(MapleBuffStat.ReadyToDie)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ReadyToDie));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ReadyToDie));
        }
        if (statups.containsKey(MapleBuffStat.CriticalReinForce)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.CriticalReinForce));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.CriticalReinForce));
        }
        if (statups.containsKey(MapleBuffStat.CurseOfCreation)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.CurseOfCreation));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.CurseOfCreation));
        }
        if (statups.containsKey(MapleBuffStat.CurseOfDestruction)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.CurseOfDestruction));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.CurseOfDestruction));
        }
        if (statups.containsKey(MapleBuffStat.BlackMageDebuff)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BlackMageDebuff));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlackMageDebuff));
        }
        if (statups.containsKey(MapleBuffStat.BodyOfSteal)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BodyOfSteal));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BodyOfSteal));
        }
        if (statups.containsKey(MapleBuffStat.GloryWing)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.GloryWing));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.GloryWing));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat18)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat18));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat18));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat18)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat18));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat19)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat19));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat19));
        }
        if (statups.containsKey(MapleBuffStat.HarmonyLink)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HarmonyLink));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.HarmonyLink));
        }
        if (statups.containsKey(MapleBuffStat.FastCharge)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.FastCharge));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.FastCharge));
        }
        if (statups.containsKey(MapleBuffStat.SpectorTransForm)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.SpectorTransForm));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.SpectorTransForm));
        }
        if (statups.containsKey(MapleBuffStat.ComingDeath)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ComingDeath));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ComingDeath));
        }
        if (statups.containsKey(MapleBuffStat.WillPoison)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.WillPoison));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.WillPoison));
        }
        // 527 int
        if (statups.containsKey(MapleBuffStat.GrandCrossSize)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.GrandCrossSize));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.GrandCrossSize));
        }
        if (statups.containsKey(MapleBuffStat.Protective)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Protective));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Protective));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat38)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat38));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat38));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat40)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat40));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat40));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat41)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat41));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat41));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat42)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat42));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat42));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat45)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat45));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat45));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat50)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat50));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat50));
        }
        if (statups.containsKey(MapleBuffStat.AltergoReinforce)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AltergoReinforce));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AltergoReinforce));
        }
        if (statups.containsKey(MapleBuffStat.YalBuff)) { //확실
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.YalBuff));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.YalBuff));
        }
        if (statups.containsKey(MapleBuffStat.IonBuff)) { //확실
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IonBuff));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IonBuff));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat53)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat53));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat53));
        }
        if (statups.containsKey(MapleBuffStat.Graffiti)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Graffiti));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Graffiti));
        }
        if (statups.containsKey(MapleBuffStat.Novility)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.Novility));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Novility));
        }
        if (statups.containsKey(MapleBuffStat.RuneOfPure)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.RuneOfPure));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.RuneOfPure));
        }
        if (statups.containsKey(MapleBuffStat.DuskDarkness)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DuskDarkness));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DuskDarkness));
        }
        if (statups.containsKey(MapleBuffStat.YellowAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.YellowAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.YellowAura));
        }
        if (statups.containsKey(MapleBuffStat.DrainAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DrainAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DrainAura));
        }
        if (statups.containsKey(MapleBuffStat.BlueAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.BlueAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlueAura));
        }
        if (statups.containsKey(MapleBuffStat.DarkAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DarkAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DarkAura));
        }
        if (statups.containsKey(MapleBuffStat.DebuffAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DebuffAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DebuffAura));
        }
        if (statups.containsKey(MapleBuffStat.UnionAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnionAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnionAura));
        }
        if (statups.containsKey(MapleBuffStat.IceAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.IceAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.IceAura));
        }
        if (statups.containsKey(MapleBuffStat.KnightsAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.KnightsAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.KnightsAura));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat622)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat622));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat622));
        }
        if (statups.containsKey(MapleBuffStat.ZeroAuraStr)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ZeroAuraStr));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ZeroAuraStr));
        }
        if (statups.containsKey(MapleBuffStat.ZeroAuraSpd)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ZeroAuraSpd));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ZeroAuraSpd));
        }
        if (statups.containsKey(MapleBuffStat.UNK634)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UNK634));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UNK634));
        }
        if (statups.containsKey(MapleBuffStat.PhotonRay)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.PhotonRay));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.PhotonRay));
        }
        if (statups.containsKey(MapleBuffStat.DarknessAura)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DarknessAura));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.DarknessAura));
        }
        if (statups.containsKey(MapleBuffStat.UNK633)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UNK633));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UNK633));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat60)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UnkBuffStat60));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat60));
        }
        if (statups.containsKey(MapleBuffStat.ThanatosDescent)){
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ThanatosDescent));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.ThanatosDescent));
        }
        if (statups.containsKey(MapleBuffStat.UNK668)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UNK668));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UNK668));
        }
        if (statups.containsKey(MapleBuffStat.UNK672)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.UNK672));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UNK672));
        }
        if (statups.containsKey(MapleBuffStat.AbsorptionRiver)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AbsorptionRiver));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AbsorptionRiver));
        }
        if (statups.containsKey(MapleBuffStat.AbsorptionWind)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AbsorptionWind));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AbsorptionWind));
        }
        if (statups.containsKey(MapleBuffStat.AbsorptionSun)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.AbsorptionSun));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.AbsorptionSun));
        }

        mplew.write(statups.containsKey(MapleBuffStat.DefenseAtt));
        mplew.write(statups.containsKey(MapleBuffStat.DefenseState));
        mplew.write(statups.containsKey(MapleBuffStat.PVPDamage));
        mplew.writeInt(0); // 307 ++
        mplew.writeInt(0);
        /* boolean active = false;
         if (chr != null) {
         MapleStatEffect eff = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
         if (eff != null) {
         activ0*e = eff.isEnergyChargeCooling() || eff.isEnergyChargeActived();
         }
         }
         mplew.writeInt(active ? chr.getBuffSource(MapleBuffStat.EnergyCharged) : 0);*/

        if (statups.containsKey(MapleBuffStat.CurseOfCreation)) { // 307 ++
            mplew.writeInt(chr.getDisease(MapleBuffStat.CurseOfCreation).right);
        }

        if (statups.containsKey(MapleBuffStat.CurseOfDestruction)) { // 307 ++
            mplew.writeInt(chr.getDisease(MapleBuffStat.CurseOfDestruction).right);
        }

        if (statups.containsKey(MapleBuffStat.PoseType)) {


            mplew.write(chr.getBuffedSkill(MapleBuffStat.PoseType));
        }
        if (statups.containsKey(MapleBuffStat.BattlePvP_Helena_Mark)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BattlePvP_Helena_Mark));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BattlePvP_Helena_Mark));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BattlePvP_Helena_Mark));
        }
        if (statups.containsKey(MapleBuffStat.BattlePvP_LangE_Protection)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.BattlePvP_LangE_Protection));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BattlePvP_LangE_Protection));
        }

        if (statups.containsKey(MapleBuffStat.MichaelSoulLink)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MichaelSoulLink)); // 전수대상 id
            mplew.write(chr.getBuffedSkill(MapleBuffStat.MichaelSoulLink));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MichaelSoulLink));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.MichaelSoulLink));
        }

        if (statups.containsKey(MapleBuffStat.AdrenalinBoost)) {
            mplew.write(chr.getBuffedSkill(MapleBuffStat.AdrenalinBoost));
        }

        if (statups.containsKey(MapleBuffStat.Stigma)) {
            mplew.writeInt(7); //chr.getBuffSource(MapleBuffStat.Stigma)
        }

        if (statups.containsKey(MapleBuffStat.HolyUnity)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.HolyUnity));
        }

        if (statups.containsKey(MapleBuffStat.DemonFrenzy)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.DemonFrenzy));
        }

        if (statups.containsKey(MapleBuffStat.ShadowSpear)) {
            mplew.writeShort(chr.getBuffedSkill(MapleBuffStat.ShadowSpear));
        }

        if (statups.containsKey(MapleBuffStat.RhoAias)) {
            mplew.writeInt(chr.getId());
            MapleStatEffect effect = chr.getBuffedEffect(MapleBuffStat.RhoAias);
            if (effect != null) {
                if (chr.getRhoAias() <= effect.getY()) { // 1 ~ 10
                    mplew.writeInt(3);
                } else if (chr.getRhoAias() <= effect.getY() + effect.getW()) { // 11 ~ 20
                    mplew.writeInt(2);
                } else {
                    mplew.writeInt(1);
                }
                mplew.writeInt(chr.getRhoAias());
                if (chr.getRhoAias() <= effect.getY()) { // 1 ~ 10
                    mplew.writeInt(3);
                } else if (chr.getRhoAias() <= effect.getY() + effect.getW()) { // 11 ~ 20
                    mplew.writeInt(2);
                } else {
                    mplew.writeInt(1);
                }
            } else {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }

        if (statups.containsKey(MapleBuffStat.VampDeath)) // 256
        {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.VampDeath));
        }

        if (statups.containsKey(MapleBuffStat.GloryWing)) {
            mplew.writeInt(chr.canUseMortalWingBeat ? 1 : 0);
            mplew.writeInt(1);
        }

        if (statups.containsKey(MapleBuffStat.BlessMark)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.BlessMark));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.BlessMark));
        }
        if (statups.containsKey(MapleBuffStat.UnkBuffStat22)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat22));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat22));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat22));
        }

        if (statups.containsKey(MapleBuffStat.UnkBuffStat36)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat36));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat36));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat36));
        }

        if (statups.containsKey(MapleBuffStat.UnkBuffStat39)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat39));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat39));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat39));
        }

        if (statups.containsKey(MapleBuffStat.UnkBuffStat41)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat41));
        }

        if (statups.containsKey(MapleBuffStat.StopForceAtominfo)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StopForceAtominfo) == 61121217 ? 4 : chr.getBuffSource(MapleBuffStat.StopForceAtominfo) == 61110211 ? 3 : (chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61101002 && chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61110211) ? 2 : 1); //
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61101002 && chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61110211 ? 5 : 3);
            mplew.writeInt(chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11) == null ? 0 : chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId());
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61101002 && chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61110211 ? 5 : 3);
            mplew.writeZeroBytes(chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61101002 && chr.getBuffSource(MapleBuffStat.StopForceAtominfo) != 61110211 ? 20 : 12);
        } else {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        for (Entry<MapleBuffStat, Pair<Integer, Integer>> stat : statups.entrySet()) {
            if (!stat.getKey().canStack() && stat.getKey().isSpecialBuff()) {
                mplew.writeInt(stat.getValue().left);
                mplew.writeInt(chr.getBuffSource(stat.getKey()));
                if (stat.getKey() == MapleBuffStat.RideVehicleExpire) {
                    mplew.write(0);
                    mplew.writeInt(0);
                    mplew.writeShort(stat.getValue().right / 1000);
                } else if (stat.getKey() == MapleBuffStat.PartyBooster) {
                    mplew.write(1);
                    mplew.writeInt(chr.getBuffedEffect(stat.getKey()).getStarttime()); // ??
                } else if (stat.getKey() == MapleBuffStat.EnergyCharged) {
                    mplew.write(chr.energyCharge);
                }
                mplew.write(0);
                mplew.writeInt(0);
                if (stat.getKey() == MapleBuffStat.GuidedBullet) {
                    mplew.writeInt(chr.guidedBullet);
                    mplew.writeInt(0);
                } else if (stat.getKey() == MapleBuffStat.RideVehicleExpire || stat.getKey() == MapleBuffStat.PartyBooster || stat.getKey() == MapleBuffStat.DashJump || stat.getKey() == MapleBuffStat.DashSpeed) {
                    mplew.writeShort(stat.getValue().right / 1000);
                } else if (stat.getKey() == MapleBuffStat.Grave) {
                    mplew.writeInt(chr.graveObjectId); // objectId
                    mplew.writeInt(0);
                }
            }
        }

        List<Pair<MapleBuffStat, Pair<Integer, Integer>>> newstatups = PacketHelper.sortBuffStats(statups);

        BuffPacket.DecodeIndieTempStat(mplew, newstatups, chr);

        if (statups.containsKey(MapleBuffStat.UnkBuffStat16)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat16));
        }

        if (statups.containsKey(MapleBuffStat.KeyDownMoving)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.KeyDownMoving));
        }

        if (statups.containsKey(MapleBuffStat.WillPoison)) {
            mplew.writeInt(100);//chr.getBuffSource(MapleBuffStat.WillPoison));
        }

        if (statups.containsKey(MapleBuffStat.ComboCounter)) {
            mplew.writeInt(chr.getBuffedSkill(MapleBuffStat.ComboCounter));
        }

        if (statups.containsKey(MapleBuffStat.UnkBuffStat50)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat50));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat50));
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.UnkBuffStat50));
        }

        if (statups.containsKey(MapleBuffStat.Graffiti)) {
            mplew.writeInt(chr.getBuffSource(MapleBuffStat.Graffiti));
        }

        mplew.write(0); // 324 ++ //v483 = CInPacket::Decode1((int)a3);

        if (statups.containsKey(MapleBuffStat.Novility)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.YellowAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.DrainAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.BlueAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.DarkAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.DebuffAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.UnionAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.IceAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.KnightsAura)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.ZeroAuraStr)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.ZeroAuraSpd)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.UNK634)) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.PhotonRay)) {
            mplew.writeInt(chr.getId());
        }
        if (statups.containsKey(MapleBuffStat.UNK633)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.BlessOfDarkness)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.UNK672)) {
            mplew.writeInt(0);
        }

    }

    public static void ChatPacket(final MaplePacketLittleEndianWriter mplew, String name, String chat) {
        ////306 추가됨
        //뭐에쓰이는진모름
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chat);
        mplew.writeInt(0); // idk
        mplew.writeInt(0);
        //307++
        mplew.write(-1);
        mplew.writeInt(0);
    }

    public static void chairPacket(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int itemId) {

        /* type
         * 1 : normalChairInfo
         * 2 : timeChairInfo
         * 3 : popChairInfo
         * 4 : starForceChairInfo
         * 5 : trickOrTreatChairInfo
         * 6 : celebChairInfo
         * 7 : randomChairInfo
         * 8 : identityChairInfo
         * 9 : mirrorChairInfo
         * 10 : popButtonChairInfo
         * 11 : rollingHouseChairInfo
         * 12 : androidChairInfo
         * 13 : mannequinChairInfo
         * 14 : rotatedSleepingBagChairInfo
         * 15 : eventPointChairInfo
         * 16 : hashTagChairInfo
         * 17 : petChairInfo
         * 18 : charLvChairInfo
         * 19 : scoreChairInfo
         * 20 : arcaneForceChairInfo
         * 21 : scaleAvatar
         * 26 : wasteChairInfo
         */
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (ii.getItemInformation(itemId) == null) {
            System.out.println(itemId + " null chair Packet.");
            return;
        }
        String type = ii.getItemInformation(itemId).chairType;

        switch (type) {
            case "timeChair":
                mplew.writeInt(0);
                break;
            case "popChair":
                mplew.writeInt(0); // size
                break;
            case "starForceChair":
                break;
            case "trickOrTreatChair":
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
            case "celebChair":
                mplew.writeInt(0);
                break;
            case "randomChair":
                break;
            case "identityChair":
                mplew.write(true);
                mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
                break;
            case "mirrorChair":
                break;
            case "popButtonChair":
                mplew.writeInt(chr.getFame());
                break;
            case "rollingHouseChair":
                mplew.writeInt(itemId);

                mplew.writeInt(0); // size
                break;
            case "androidChair":
                break;
            case "mannequinChair":
                mplew.writeInt(chr.getHairRoom().size()); // Hair? Face?
                for (MapleMannequin hr : chr.getHairRoom()) {
                    mplew.write(0);//chr.getGender());
                    mplew.write(0);//chr.getSkinColor());
                    mplew.writeInt(hr.getValue());
                    mplew.write(hr.getBaseProb());
                    mplew.write(hr.getBaseColor());
                    mplew.write(hr.getAddColor());
                }
                break;
            case "rotatedSleepingBagChair":
                break;
            case "eventPointChair":
                break;
            case "hashTagChair":
                for (int i = 0; i < 18; ++i) {
                    mplew.writeMapleAsciiString(""); // questEx 16721
                }
                break;
            case "petChair":
                for (int i = 0; i < 3; ++i) {
                    MaplePet pet = chr.getPet(i);
                    if (pet != null) {
                        mplew.writeInt(pet.getPetItemId());
                        mplew.writeInt(pet.getPos().x); // unk
                        mplew.writeInt(pet.getPos().y); // unk
                    } else {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                }
                break;
            case "charLvChair":
                mplew.writeInt(chr.getLevel());
                break;
            case "scoreChair":
                mplew.writeInt(0); // score
                break;
            case "arcaneForceChair":
                break;
            case "scaleAvatarChair":
                mplew.write(false); // if true -> int
                break;
            case "wasteChair":
                mplew.writeLong(chr.getMesoChairCount());
                break;
            default:
                if (GameConstants.isTextChair(itemId)) { // normalChair
                    mplew.writeMapleAsciiString(chr.getChairText());
                    PacketHelper.ChatPacket(mplew, chr.getName(), "[의자]" + chr.getChairText());
                } else if (GameConstants.isTowerChair(itemId)) {
                    String towerchair = chr.getInfoQuest(7266);
                    if (towerchair.equals("")) {
                        mplew.writeInt(0);
                    } else {
                        String[] temp = towerchair.split(";");
                        mplew.writeInt(temp.length);
                        for (int a = 0; a < temp.length; a++) {
                            int chairid = Integer.parseInt(temp[a].substring(2));
                            mplew.writeInt(chairid);
                        }
                    }
                } else if (itemId == 3015520 || itemId == 3018071 || itemId == 3018352 || itemId == 3018464 || itemId == 3015798 || itemId == 3015895) { // worldLvChairNonShowLevel
                    //e91e60
                    mplew.write(false);
                } else if (itemId == 3015440 || itemId == 3015650 || itemId == 3015651 || itemId == 3015897 || itemId == 3018430 | itemId == 3018450) { // mesoChair
                    mplew.writeLong(chr.getMesoChairCount());
                }
                break;
        }
    }
}
