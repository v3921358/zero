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

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import scripting.EventInstanceManager;
import server.MapleInventoryManipulator;
import server.Randomizer;
import server.Timer;
import server.Timer.ShowTimer;
import server.field.boss.MapleBossManager;
import server.field.boss.demian.MapleFlyingSword;
import server.field.boss.lucid.Butterfly;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMist;
import server.maps.MapleNodes.MapleNodeInfo;
import server.movement.LifeMovementFragment;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.MobPacket;
import tools.packet.MobPacket.BossLucid;
import tools.packet.SLFCGPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobHandler {

    public static final void MoveMonster(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {

        //System.out.println(slea.toString());
        long time = System.currentTimeMillis();
        final int oid = slea.readInt();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);
        if (monster == null) { // movin something which is not a monster
            return;
        }

        final short moveid = slea.readShort();
        byte bOption = slea.readByte();
        int actionAndDir = slea.readByte();
        final long targetInfo = slea.readLong(); // 2바이트 ID, 2바이트 레벨, 2바이트 옵션, 2바이트 ??
        byte unk1 = slea.readByte(); //+342
        byte unk2 = slea.readByte(); //+342

        final int skillId = (int) (targetInfo & 0xFFFF);
        final int skillLevel = (int) ((targetInfo >> 16) & 0xFFFF);
        final int option = (int) ((targetInfo >> 32) & 0xFFFF);

        boolean setNextSkill = true;

        int action = actionAndDir;
        if (action < 0) {
            action = -1;
        } else {
            action = action >> 1;
        }

        final boolean changeController = (bOption & 16) == 16;
        final boolean movingAttack = (bOption & 1) == 1;

//        chr.dropMessageGM(5, "bOption : " + bOption);
        //      chr.dropMessageGM(5, "option : " + option);
        //    chr.dropMessageGM(5, "---------------------------");
        int count = slea.readByte();
        final List<Point> multiTargetForBall = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // multiTargetForBall
            multiTargetForBall.add(new Point(slea.readShort(), slea.readShort()));
        }

        count = slea.readByte();
        final List<Short> randTimeForAreaAttack = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // randTimeForAreaAttack
            randTimeForAreaAttack.add(slea.readShort());
        }

        slea.readByte(); // (m_bActive == 0) | 16 * !(IsCheatMobMoveRand() == 0)

        slea.readInt(); // getHackedCode();
        slea.readInt(); // 332++
        slea.readInt(); // m_moveCtx.fc.ptTarget.xy
        slea.readInt(); // m_moveCtx.fc.ptTarget.xy2
        slea.readInt(); // m_dwHackedCodeCRC

        slea.readInt(); // 342+
        slea.readByte(); // unknown

        int tEncodedGatherDuration = slea.readInt(); // m_tEncodedGatherDuration

        final Point startPos = slea.readPos();
        final Point startWobble = slea.readPos(); // original wobble

        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 2);

        /*        count = slea.readByte();

         for (int i = 0; i < count; i += 2) {
         slea.readByte();
         }*/
        slea.readByte();
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt(); // 307++
        slea.readByte();
        slea.readInt();
        slea.readByte();
        slea.readByte();
        boolean cannotUseSkill = slea.readByte() == 1;

        MapleBossManager.changePhase(monster);

        MapleBossManager.setBlockAttack(monster);

        if (!monster.isSkillForbid()) {// && !cannotUseSkill) {
            if (action >= 13 && action <= 29) { // 몹 공격
                int attackIdx = action - 13;

                if (attackIdx < monster.getStats().getAttacks().size()) {
                    MobAttack attack = monster.getStats().getAttacks().get(attackIdx);

                    chr.dropMessageGM(-8, "mobId : " + monster.getId());
                                      chr.dropMessageGM(-8, "attackId : " + attackIdx);
                    chr.dropMessageGM(5, "---------------------------");
                    if (attack.getAfterAttack() >= 0) {
                        monster.getMap().broadcastMessage(MobPacket.setAfterAttack(oid, attack.getAfterAttack(), attack.getAfterAttackCount(), action, (actionAndDir & 0x01) != 0));
                                   chr.dropMessageGM(-8, "AfterAttack : " + attack.getAfterAttack());
                    } else {
                        if ((attackIdx == 0 || attackIdx == 1 || attackIdx == 6 || attackIdx == 7 || attackIdx == 12) && (monster.getId() == 8930000 || monster.getId() == 8930100)) {
                            List<Point> pos = new ArrayList<>();
                            pos.add(new Point(810, 443));
                            pos.add(new Point(-2190, 443));
                            pos.add(new Point(-1690, 443));
                            pos.add(new Point(560, 443));
                            pos.add(new Point(-190, 443));
                            pos.add(new Point(-690, 443));
                            pos.add(new Point(-1940, 443));
                            pos.add(new Point(1310, 443));
                            pos.add(new Point(-1190, 443));
                            pos.add(new Point(1060, 443));
                            pos.add(new Point(-940, 443));
                            pos.add(new Point(-1440, 443));
                            pos.add(new Point(1560, 443));
                            pos.add(new Point(-440, 443));
                            monster.getMap().broadcastMessage(MobPacket.dropStone("DropStone", pos));
                        }
                        if (monster.getMap().countMonsterById(8930001) > 4) {
                            monster.getMap().killMonster(8930001);
                        }
                    }

                    for (Triple<Integer, Integer, Integer> skill : attack.getSkills()) {
                        chr.dropMessageGM(-8, "attack.getSkills() : " + skill.getLeft() + " " + skill.getMid());
                        MobSkill msi = MobSkillFactory.getMobSkill(skill.left, skill.mid); // 자기가 안가지고 있는 경우가 있음.
                        if (msi != null) {
                            if (skill.right > 0) {
                                msi.setMobSkillDelay(c.getPlayer(), monster, skill.right, (short) option, (actionAndDir & 0x01) != 0);
                            } else if (msi.getSkillAfter() > 0) {
                                msi.setMobSkillDelay(c.getPlayer(), monster, msi.getSkillAfter(), (short) option, (actionAndDir & 0x01) != 0);
                            } else {
/*                                if (option > 0) {
                                    ShowTimer.getInstance().schedule(() -> {
                                        if (monster.isAlive()) {
                                            chr.dropMessageGM(-11, "op1 : " + option);
                                            msi.applyEffect(chr, monster, true, (actionAndDir & 0x01) != 0);
                                        }
                                    }, option);
                                } else {*/
                                if (monster.isAlive()) {
                                    msi.applyEffect(chr, monster, true, (actionAndDir & 0x01) != 0);
                                }
//                                }
                            }
                        }
                    }
                }
            } else if (action >= 30 && action <= 46) {
                if (monster.getNextSkill() == skillId && monster.getNextSkillLvl() == skillLevel) {
                    MobSkill msi = monster.getStats().getSkill(skillId, skillLevel);

                    if (msi != null) {
                        if (msi.getSkillAfter() > 0) {
                            chr.dropMessageGM(6, "[MobSkillDelay] " + skillId + "/" + skillLevel + "이 발동되었습니다.");

                            msi.setMobSkillDelay(c.getPlayer(), monster, msi.getSkillAfter(), (short) option, (actionAndDir & 0x01) != 0);

                            monster.setNextSkill(0);
                            monster.setNextSkillLvl(0);
                            if (msi.getOtherSkillID() > 0) {
                                Timer.MobTimer.getInstance().schedule(() -> {
                                    MobSkill nextSkill = MobSkillFactory.getMobSkill(msi.getOtherSkillID(), msi.getOtherSkillLev());
                                    if (nextSkill != null) {
                                        nextSkill.applyEffect(chr, monster, true, (actionAndDir & 0x01) != 0);
                                    }
                                }, msi.getSkillAfter() + 5000);
                            }
                        } else if (msi.onlyOnce() && monster.getLastSkillUsed(skillId, skillLevel) != 0 && skillId != 241) {
                            if (chr.isGM()) {
                                chr.dropMessage(-8, monster.getId() + " 몬스터의 " + skillId + " / " + skillLevel + " 스킬이 1회용이지만, 여러번 사용되었습니다.");
                            }
                        } else {
                            if (monster.isAlive()) {
                                msi.applyEffect(chr, monster, true, (actionAndDir & 0x01) != 0);
                            }

                            monster.setNextSkill(0);
                            monster.setNextSkillLvl(0);

                            if (msi.getOtherSkillID() > 0) {
                                Timer.MobTimer.getInstance().schedule(() -> {
                                    MobSkill nextSkill = MobSkillFactory.getMobSkill(msi.getOtherSkillID(), msi.getOtherSkillLev());
                                    if (nextSkill != null) {
                                        nextSkill.applyEffect(chr, monster, true, (actionAndDir & 0x01) != 0);
                                    }
                                }, option + 3000);
                            }
                            chr.dropMessageGM(6, "[MobSkill] " + skillId + "/" + skillLevel + " 스킬을 성공적으로 사용하였습니다. [action : " + action + "] [option : " + option + "]");

                        }


                        if (msi.getAfterAttack() >= 0) {
                            monster.getMap().broadcastMessage(MobPacket.setAfterAttack(oid, msi.getAfterAttack(), msi.getAfterAttackCount(), action, (actionAndDir & 0x01) != 0));
                            //           chr.dropMessage(-8, "AfterAttack : " + attack.getAfterAttack());
                        }
                    } else {
                        if (chr.isGM()) {
                            chr.dropMessage(-8, monster.getId() + " 몬스터의 " + skillId + " / " + skillLevel + " 스킬이 캐싱되지 않았습니다.");
                        }
                    }
                } else {
                    if (chr.isGM()) {
                        chr.dropMessage(-8, monster.getId() + " 몬스터의 " + skillId + " / " + skillLevel + " 스킬 사용중 오류가 발생했습니다.");
                    }
                }
            } else if (action > 46) {
                // 특수 패킷이 필요한 친구들.
            } else {
                final MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(monster, action);
                if (attackInfo != null && attackInfo.getSkill() != null) {
                    monster.setNextSkill(attackInfo.getSkill().getSkill());
                    monster.setNextSkillLvl(attackInfo.getSkill().getLevel());

                } else if (monster.getNoSkills() > 0) {
                    List<MobSkill> useableSkills = new ArrayList<>();

                    for (MobSkill msi : monster.getSkills()) { // 얘가 가지고 있는 스킬 중에
                        if ((time - monster.getLastSkillUsed(msi.getSkillId(), msi.getSkillLevel())) >= 0) { // 마지막으로 사용한 시간으로부터 지나거나, 처음 사용하는 스킬
                            if (monster.getHPPercent() <= msi.getHP() && !msi.isOnlyOtherSkill() && !msi.isOnlyFsm() && !msi.checkCurrentBuff(chr, monster)) { // 일정 수치 이하의 HP%여야 써지는 스킬
                                if (msi.onlyOnce() || msi.getSkillId() == 247) {
                                    useableSkills.clear(); // 초기화하고
                                    useableSkills.add(msi); // 추가 한다
                                    break; // 한번 쓰는 스킬을 우선으로 쓰는 것으로..
                                } else {
                                    useableSkills.add(msi); // 추가 한다
                                }
                            }
                        }
                    }
                    if (monster.getId() >= 8800103 && monster.getId() <= 8800110) {
                        setNextSkill = false;
                        MapleBossManager.ZakumArmHandler(monster, chr, moveid, movingAttack, (actionAndDir & 0x01) != 0);
                    } else if (!useableSkills.isEmpty()) {
                        MobSkill nextSkill = useableSkills.get(Randomizer.nextInt(useableSkills.size())); // 랜덤으로 다음 스킬을 골라서
                        monster.setNextSkill(nextSkill.getSkillId()); // 데이터
                        monster.setNextSkillLvl(nextSkill.getSkillLevel()); // 저장
                        monster.setLastSkillUsed(nextSkill, time, nextSkill.getInterval());
                    }
                }
            }
        }

        if (monster.getController() != null && monster.getController().getId() != c.getPlayer().getId()) {
            if (!changeController) { // 동시에 컨트롤 방지.. 안그럼 문워크함
                c.getSession().writeAndFlush(MobPacket.stopControllingMonster(oid));
                return;
            } else {
                monster.switchController(chr, true);
            }
        }

        if (monster != null && c != null) {
            if (setNextSkill) {
                //    c.getPlayer().dropMessageGM(6,"칵퉤네요 "+monster.getNextSkill()+"   "+monster.getNextSkillLvl()+"  ");
                c.getSession().writeAndFlush(MobPacket.moveMonsterResponse(oid, moveid, monster.getMp(), movingAttack, monster.getNextSkill(), monster.getNextSkillLvl(), 0));
            }
        }

        if (res != null && c != null && c.getPlayer() != null && monster != null) {
            final MapleMap map = c.getPlayer().getMap();
            MovementParse.updatePosition(res, monster, -1);
            map.moveMonster(monster, monster.getPosition());
            map.broadcastMessage(chr, MobPacket.moveMonster(bOption, actionAndDir, targetInfo, tEncodedGatherDuration, oid, startPos, startWobble, res, multiTargetForBall, randTimeForAreaAttack, cannotUseSkill), monster.getPosition());
        }
    }

    public static final void FriendlyDamage(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4); // Player ID
        final MapleMonster mobto = map.getMonsterByOid(slea.readInt());

        if (mobfrom != null && mobto != null && mobto.getStats().isFriendly()) {
            final int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel())) / 2; // Temp for now until I figure out something more effective
            mobto.damage(chr, damage, true);
            checkShammos(chr, mobto, map);
        }
    }

    public static final void BindMonster(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleMap map = c.getPlayer().getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mob = map.getMonsterByOid(slea.readInt());
//        c.getSession().writeAndFlush(MobPacket.BindMonster(mob));
    }

    public static final void MobBomb(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4); // something, 9E 07
        slea.readInt(); //-204?

        if (mobfrom != null && mobfrom.getBuff(MonsterStatus.MS_TimeBomb) != null) {
            /* not sure
             12D -    0B 3D 42 00 EC 05 00 00 32 FF FF FF 00 00 00 00 00 00 00 00
             <monsterstatus done>
             108 - 07 0B 3D 42 00 EC 05 00 00 32 FF FF FF 01 00 00 00 7B 00 00 00
             */
        }
    }

    public static final void checkShammos(final MapleCharacter chr, final MapleMonster mobto, final MapleMap map) {
        if (!mobto.isAlive() && mobto.getStats().isEscort()) { //shammos
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) { //check for 2022698
                if (chrz.getParty() != null && chrz.getParty().getLeader().getId() == chrz.getId()) {
                    //leader
                    if (chrz.haveItem(2022698)) {
                        MapleInventoryManipulator.removeById(chrz.getClient(), MapleInventoryType.USE, 2022698, 1, false, true);
                        mobto.heal((int) mobto.getMobMaxHp(), mobto.getMobMaxMp(), true);
                        return;
                    }
                    break;
                }
            }
            map.broadcastMessage(CWvsContext.serverNotice(6, "", "Your party has failed to protect the monster."));
            final MapleMap mapp = chr.getMap().getForcedReturnMap();
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                chrz.changeMap(mapp, mapp.getPortal(0));
            }
        } else if (mobto.getStats().isEscort() && mobto.getEventInstance() != null) {
            mobto.getEventInstance().setProperty("HP", String.valueOf(mobto.getHp()));
        }
    }

    public static final void MonsterBomb(final int oid, final MapleCharacter chr) {
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if (monster == null || !chr.isAlive() || chr.isHidden() || monster.getLinkCID() > 0) {
            return;
        }
        final byte selfd = monster.getStats().getSelfD();
        if (selfd != -1) {
            chr.getMap().killMonster(monster, chr, false, false, selfd);
        }
    }

    public static final void AutoAggro(final int monsteroid, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.isHidden()) { //no evidence :)
            return;
        }
        final MapleMonster monster = chr.getMap().getMonsterByOid(monsteroid);

        if (monster != null && chr.getTruePosition().distanceSq(monster.getTruePosition()) < 200000 && monster.getLinkCID() <= 0) {
            if (monster.getController() != null) {
                if (chr.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(chr, true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                monster.switchController(chr, true);
            }
        }
    }

    public static final void HypnotizeDmg(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        slea.skip(4); // Player ID
        final int to = slea.readInt(); // mobto
        slea.skip(1); // Same as player damage, -1 = bump, integer = skill ID
        final int damage = slea.readInt();
//	slea.skip(1); // Facing direction
//	slea.skip(4); // Some type of pos, damage display, I think

        final MapleMonster mob_to = chr.getMap().getMonsterByOid(to);

        if (mob_from != null && mob_to != null && mob_to.getStats().isFriendly()) { //temp for now
            if (damage > 30000) {
                return;
            }
            mob_to.damage(chr, damage, true);
            checkShammos(chr, mob_to, chr.getMap());
        }
    }

    public static final void MobNode(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        final int newNode = slea.readInt();
        final int nodeSize = chr.getMap().getNodes().size();
        if (mob_from != null && nodeSize > 0) {
            final MapleNodeInfo mni = chr.getMap().getNode(newNode);
            if (mni == null) {
                return;
            }
            if (mni.attr == 2) { //talk
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                        chr.getMap().talkMonster("Please escort me carefully.", 5120035, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().talkMonster("Please escort me carefully.", 5120051, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                }
            }
            mob_from.setLastNode(newNode);
            if (chr.getMap().isLastNode(newNode)) { //the last node on the map.
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().broadcastMessage(CWvsContext.serverNotice(5, "", "Proceed to the next stage."));
                        chr.getMap().removeMonster(mob_from);
                        break;

                }
            }
        }
    }

    public static final void OrgelHit(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster Attacker = chr.getMap().getMonsterByOid(slea.readInt());
        slea.skip(4);
        final MapleMonster Defender = chr.getMap().getMonsterByOid(slea.readInt());
        slea.skip(3);
        final int Damage = slea.readInt();
        final EventInstanceManager em = chr.getEventInstance();
        if (Attacker == null || Defender == null || em == null) {
            return;
        }
        if (Attacker.getId() >= 9833090 && Attacker.getId() <= 9833099) { //악몽의 가고일, 악몽의 클리너
            if ((Defender.getId() >= 9833070 && Defender.getId() <= 9833074) || Defender.getId() == 9833100) {
                final int HP = (int) (Defender.getHp() - Damage) < 0 ? 0 : (int) (Defender.getHp() - Damage);
                Defender.setHp(HP);
                if (Defender.getHp() <= 0) {
                    Defender.getMap().killMonster(Defender);
                }
            }
        }
    }

    public static final void SpiritHit(final LittleEndianAccessor slea, final MapleCharacter chr) {
        int oid = slea.readInt();
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMonster mob = chr.getMap().getMonsterByOid(oid); // From
        final EventInstanceManager em = chr.getEventInstance();
        if (mob == null || em == null) {
            chr.getMap().killMonster(mob);
            return;
        }
        if (mob.getId() == 8920004 || mob.getId() == 8920104) { //유혹의 하트
            chr.getMap().broadcastMessage(MobPacket.BossBlodyQueen.killMonster(oid));
            chr.getMap().removeMapObject(mob);
            mob.killed();
            return;
        }

        int life = (int) chr.getKeyValue(16215, "life");
        switch (mob.getId()) {
            case 8644201:
                chr.setKeyValue(16215, "life", "" + (life - 5));
                break;
            case 8644301:
            case 8644302:
            case 8644303:
            case 8644304:
            case 8644305:
                chr.setKeyValue(16215, "life", "" + (life - 50));
                int asdf = 0;
                for (int i = 0; i < 5; i++) {
                    if (em.getProperty("ObjectId" + i) != null && !em.getProperty("ObjectId" + i).equals("")) {
                        chr.getClient().getSession().writeAndFlush(SLFCGPacket.SpawnPartner(false, Integer.parseInt(em.getProperty("ObjectId" + i)), 0));
                        em.setProperty("ObjectId" + i, "");
                        asdf++;
                    }
                }
                if (asdf == 5) {
                    chr.getClient().getSession().writeAndFlush(CField.startMapEffect("", 0, false));
                    chr.getClient().getSession().writeAndFlush(CField.startMapEffect("친구들이... 맹독의 정령에게 당하고 말았담!", 5120175, true));
                    chr.getClient().getSession().writeAndFlush(CField.environmentChange("Map/Effect3.img/savingSpirit/failed", 19));

                }
                chr.setKeyValue(16215, "chase", "0");
                break;
            case 8644101:
            case 8644102:
            case 8644103:
            case 8644104:
            case 8644105:
            case 8644106:
            case 8644107:
            case 8644108:
            case 8644109:
            case 8644110:
            case 8644111:
            case 8644112:
                chr.setKeyValue(16215, "life", "" + (life - 5));
                break;
            case 8880315:
            case 8880316:
            case 8880317:
            case 8880318:
            case 8880319:
            case 8880345:
            case 8880346:
            case 8880347:
            case 8880348:
            case 8880349:
                chr.addHP((long) (-chr.getStat().getCurrentMaxHp() * 0.1));
                break;
        }
        chr.getMap().killMonster(mob, chr, false, false, (byte) 1);

        if (chr.getKeyValue(16215, "life") <= 0 && chr.getMapId() == 921172300) { // 왠진 모르겠는데 파풀라투스랑 충돌남, MapId 체크로 임시처리함 - 멜론 -
            MapleMap target = chr.getClient().getChannelServer().getMapFactory().getMap(921172400);
            chr.changeMap(target, target.getPortal(0));
            chr.getClient().getSession().writeAndFlush(CField.environmentChange("Map/Effect2.img/event/gameover", 16));
        }
    }

    public static void AirAttack(LittleEndianAccessor slea, MapleClient c) {
        /*		int objectId = slea.readInt();
         MapleMonster mob = c.getPlayer().getMap().getMonsterByOid(objectId);
		
         int skillId = slea.readInt();
         int skillLev = slea.readInt();
		
         if (skillId == 230) {
         byte size = slea.readByte();
			
         for (int i = 0; i < size; ++i) {
         int id = slea.readInt();
         Rectangle angle = mob.getSkillRectInfo().get(id - 1);
         if (angle != null) {
         if (angle.contains(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y)) {
         c.getPlayer().addHP(-c.getPlayer().getStat().getCurrentMaxHp());
         }
         }
         }
         }*/
    }

    public static void demianBind(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        int result = slea.readInt();

        Map<MapleBuffStat, Pair<Integer, Integer>> cancelList = new HashMap<>();

        if (result == 0) {
            if (chr.getDiseases().containsKey(MapleBuffStat.Lapidification)) {
                chr.getDiseases().remove(MapleBuffStat.Lapidification);

                cancelList.put(MapleBuffStat.Lapidification, new Pair<>(0, 0));
                c.getSession().writeAndFlush(BuffPacket.cancelBuff(cancelList, chr));
                chr.getMap().broadcastMessage(chr, BuffPacket.cancelForeignBuff(chr, cancelList), false);
            }
        }
    }

    public static void demianAttacked(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();

        MapleMonster mob = c.getPlayer().getMap().getMonsterByOid(objectId);

        if (mob != null) {
            int skillId = slea.readInt();
//			int skillLv = slea.readInt();

            if (skillId == 214) {
                MobSkillFactory.getMobSkill(170, 51).applyEffect(c.getPlayer(), mob, true, mob.isFacingLeft());
            }
        }
    }

    public static void useStigmaIncinerate(LittleEndianAccessor slea, MapleClient c) {
        int state = slea.readInt();

        MapleCharacter chr = c.getPlayer();
        Map<MapleBuffStat, Pair<Integer, Integer>> cancelList = new HashMap<>();
        switch (state) {
            case 0: // 시작
                break;
            case 1: // 성공
                int playerid = slea.readInt();
                int stigmacount = slea.readInt();
                if (chr.Stigma > 0) {
                    chr.Stigma = 0;
                    cancelList.put(MapleBuffStat.Stigma, new Pair<>(0, 0));
                    c.getSession().writeAndFlush(BuffPacket.cancelBuff(cancelList, chr));
                    chr.getMap().broadcastMessage(chr, BuffPacket.cancelForeignBuff(chr, cancelList), false);
                    chr.getMap().broadcastMessage(MobPacket.addStigma(chr, 0));
                    if (playerid == 0) {
                        chr.getMap().broadcastMessage(MobPacket.incinerateObject(null, false));
                    } else {
                        MapleCharacter stchr = c.getPlayer().getMap().getCharacterById(playerid);
                        if (stchr == null) {
                            return;
                        }
                        stchr.Stigma += stigmacount;
                        Map<MapleBuffStat, Pair<Integer, Integer>> dds = new HashMap<>();
                        dds.put(MapleBuffStat.Stigma, new Pair<>(stchr.Stigma, 237));
                        if (stchr.Stigma >= 7) {
                            stchr.Stigma = 0;
                            stchr.addHP(-stchr.getStat().getCurrentMaxHp());
                            stchr.getMap().broadcastMessage(MobPacket.CorruptionChange((byte) 0, stchr.getMap().getStigmaDeath()));
                            stchr.getMap().broadcastMessage(CField.environmentChange("Effect/OnUserEff.img/demian/screen", 4));

                            MapleMonster m = null;
                            for (MapleMonster monster : chr.getMap().getAllMonstersThreadsafe()) {
                                if (monster.isAlive()) {
                                    if (monster.getId() == 8880100 || monster.getId() == 8880101 || monster.getId() == 8880110 || monster.getId() == 8880111) {
                                        m = monster;
                                    }
                                }
                            }
                            if (m != null) {
                                MapleFlyingSword mfs = new MapleFlyingSword(1, m);
                                stchr.getMap().spawnFlyingSword(mfs);
                                stchr.getMap().setNewFlyingSwordNode(mfs, m.getTruePosition());
                            }
                            stchr.getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(dds, stchr));
                            stchr.getMap().broadcastMessage(stchr, BuffPacket.cancelForeignBuff(stchr, dds), false);
                            stchr.getMap().broadcastMessage(MobPacket.addStigma(stchr, 0));
                        } else {
                            stchr.getClient().getSession().writeAndFlush(BuffPacket.giveDisease(dds, MobSkillFactory.getMobSkill(237, 1), stchr));
                            stchr.getMap().broadcastMessage(stchr, BuffPacket.giveForeignDeBuff(stchr, dds), false);
                            stchr.getMap().broadcastMessage(MobPacket.addStigma(stchr, 1));
                            stchr.getClient().getSession().writeAndFlush(CField.specialMapSound("SoundEff/BossDemian/incStigma"));
                        }
                    }
                    chr.getClient().getSession().writeAndFlush(CField.specialMapSound("SoundEff/BossDemian/decStigma"));
                }
                break;
            case 2: // 취소
                break;
        }
    }

    public static void stoneAttacked(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        if (slea.readInt() == 0) {
            if (chr.getMap().getId() % 1000 == 410 || chr.getMap().getId() % 1000 == 810) { // 벨룸
                MobSkillFactory.getMobSkill(123, 44).applyEffect(chr, null, true, false);
            } else if (chr.getMap().getId() % 1000 == 210 || chr.getMap().getId() % 1000 == 610) { // 삐에르
                MobSkillFactory.getMobSkill(174, 3).applyEffect(chr, null, true, false);
            }
        }
    }

    public static void jinHillahBlackHand(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        chr.dropMessageGM(5, "db : " + slea);
        MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
        if (mob != null) {
            int skillLevel = slea.readInt();
            int id = slea.readInt();
            Point pos = slea.readPos();

            if (chr.getId() == id) {
                Rectangle rect = new Rectangle(slea.readInt(), slea.readInt(), slea.readInt(), slea.readInt());
                if (skillLevel == 2) { // 2는왜팅기지?
                    skillLevel = 1;
                }

                chr.getMap().broadcastMessage(MobPacket.jinHillahSpirit(mob.getObjectId(), chr.getId(), rect, pos, skillLevel));

                //only have 1 dc
                if (chr.liveCounts() == 1) {
                    for (int i = 0; i < chr.getDeathCounts().length; ++i) {
                        if (chr.getDeathCounts()[i] == 1) {
                            chr.getDeathCounts()[i] = 0;
                            break;
                        }
                    }
                    chr.addHP(-chr.getStat().getCurrentMaxHp());
                } else {
                    if (chr.getDebuffValue(MapleBuffStat.Stun) > 0) {
                        chr.dropMessageGM(6, "이미 실에 맞거나 스턴상태라 양초 무시");
                    } else {
                        chr.giveDebuff(MapleBuffStat.Stun, MobSkillFactory.getMobSkill(123, 83));
                        for (int i = 0; i < chr.getDeathCounts().length; ++i) {
                            if (chr.getDeathCounts()[i] == 1) {
                                chr.getDeathCounts()[i] = 0;
                                break;
                            }
                        }
                        if (chr.getMap().getLightCandles() < chr.getMap().getCandles()) {
                            //update candles
                            chr.getMap().setLightCandles(chr.getMap().getLightCandles() + 1);
                            chr.getMap().broadcastMessage(CField.JinHillah(1, chr));
                        }
                    }
                }

                //update dc
                c.getSession().writeAndFlush(CField.JinHillah(3, chr));
                c.getPlayer().getMap().broadcastMessage(CField.JinHillah(10, c.getPlayer()));

                chr.dropMessageGM(6, "candle : " + chr.getMap().getCandles() + " / " + chr.getMap().getLightCandles());

                //create alter
                if (chr.getMap().getCandles() == chr.getMap().getLightCandles() && chr.getMap().getReqTouched() == 0) {
                    chr.getMap().broadcastMessage(CField.enforceMSG("힐라가 접근하여 사라지기 전에 제단에서 채집키를 연타하여 영혼을 회수해야 한다.", 254, 6000));
                    chr.getMap().setReqTouched(30);
                    chr.getMap().broadcastMessage(CField.JinHillah(6, chr));
                }
            }
        }
    }

    public static void touchAlter(LittleEndianAccessor slea, MapleClient c) {
        MapleMap map = c.getPlayer().getMap();

        if (map.getReqTouched() > 0) {
            map.setReqTouched(map.getReqTouched() - 1);
            if (map.getReqTouched() != 0) {
                map.broadcastMessage(CField.JinHillah(7, c.getPlayer()));
            } else {
                map.broadcastMessage(CField.JinHillah(8, c.getPlayer()));
                map.setLightCandles(0);
                map.broadcastMessage(CField.JinHillah(1, c.getPlayer()));

                for (MapleCharacter chr : map.getAllCharactersThreadsafe()) {
                    SkillFactory.getSkill(80002544).getEffect(1).applyTo(chr, false);
                    chr.cancelEffectFromBuffStat(MapleBuffStat.DebuffIncHp, 80002543);

                    for (int i = 0; i < chr.getDeathCounts().length; ++i) {
                        int dc = chr.getDeathCounts()[i];
                        if (dc == 0) { // red
                            chr.getDeathCounts()[i] = 1;
                        }
                    }

                    chr.getClient().getSession().writeAndFlush(CField.JinHillah(3, chr));
                    c.getPlayer().getMap().broadcastMessage(CField.JinHillah(10, c.getPlayer()));
                }
            }
        }
    }

    public static void unkJinHillia(LittleEndianAccessor slea, MapleClient c) {
        // TODO Auto-generated method stub
    }

    public static void lucidStateChange(MapleCharacter chr) {
        if (chr.getMap().getLucidCount() > 0) {
            chr.getMap().setLucidCount(chr.getMap().getLucidCount() - 1);
            chr.getMap().setLucidUseCount(chr.getMap().getLucidUseCount() + 1);
            chr.getMap().broadcastMessage(BossLucid.changeStatueState(false, chr.getMap().getLucidCount(), true));

            for (MapleMonster mob : chr.getMap().getAllButterFly()) {
                chr.getMap().killMonster(mob, chr, false, false, (byte) 0);
            }

            chr.getMap().broadcastMessage(BossLucid.setButterflyAction(Butterfly.Mode.ERASE, 0));
            chr.getMap().lucidButterflyCount = 0;
        }
    }

    public static void mobSkillDelay(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        int skillId = slea.readInt();
        int skillLevel = slea.readInt();

        switch (skillId) {
            case 213:
            case 217:
            case 241:
                return;
        }

        MobSkill msi = MobSkillFactory.getMobSkill(skillId, skillLevel);
        if (c.getPlayer() != null && msi != null) {
            MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(objectId);
            if (monster != null) {
                msi.applyEffect(c.getPlayer(), monster, true, monster.isFacingLeft());
            }
        }
    }

    public static void handlePierreMist(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        int skillId = slea.readInt();
        int skillLevel = slea.readInt();
        Point pos = slea.readPos();
        MobSkill msi = MobSkillFactory.getMobSkill(skillId, skillLevel);
        if (c.getPlayer() != null) {
            MapleMonster monster = c.getPlayer().getMap().getMonsterByOid(objectId);
            if (monster != null) {
                MapleMist mist = new MapleMist(new Rectangle(pos.x - 30, 496, 53, 62), monster, msi, (int) 4000);
                mist.setPosition(pos);
                monster.getMap().spawnMist(mist, false);
            }
        }
    }
}
