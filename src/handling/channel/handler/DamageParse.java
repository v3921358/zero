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

import client.ForceAtoms;
import client.MapleBuffStat;
import client.MapleBuffStatValueHolder;
import client.MapleCharacter;
import client.PlayerStats;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import org.w3c.dom.css.Rect;
import server.AdelProjectile;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Randomizer;
import server.Timer.BuffTimer;
import server.field.skill.MapleMagicWreck;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.*;
import tools.AttackPair;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.MobPacket;

public class DamageParse {

    public static void applyAttack(final AttackInfo attack, final Skill theSkill, final MapleCharacter player, final double maxDamagePerMonster, final MapleStatEffect effect, final AttackType attack_type, boolean BuffAttack, boolean energy) {
        if (player.getMapId() == ServerConstants.WarpMap && !player.isGM()) {
            if (attack.skill % 10000 != 1092 && attack.skill % 10000 != 1094 && attack.skill % 10000 != 1095) {
                player.changeMap(ServerConstants.WarpMap, 0);
                //player.dropMessage(1, "스킬 쓰지마세요.");
                return;
            }
        }
        if (attack.skill != 0) {
            if (effect == null) {
                player.getClient().getSession().writeAndFlush(CWvsContext.enableActions(player));
                return;
            }

            if (GameConstants.isMulungSkill(attack.skill)) {
                if (player.getMapId() / 10000 != 92502) {
                    return;
                } else {
                    if (player.getMulungEnergy() < 10000) {
                        return;
                    }
                }
            } else if (GameConstants.isPyramidSkill(attack.skill)) {
                if (player.getMapId() / 1000000 != 926) {
                    return;
                }
            } else if (GameConstants.isInflationSkill(attack.skill)) {
                if (player.getBuffedValue(MapleBuffStat.Inflation) == null) {
                    return;
                }
            } else if (!GameConstants.isNoApplySkill(attack.skill)) { // Must be done here, since NPE with normal atk
                MapleStatEffect oldEffect;
                switch (attack.skill) { // 호영 개새끼
                    case 164101002:
                    case 164111010:
                    case 164121002:
                        oldEffect = SkillFactory.getSkill(attack.skill - 1).getEffect(attack.skilllevel);
                        break;
                    default:
                        oldEffect = SkillFactory.getSkill(attack.skill).getEffect(attack.skilllevel);
                        break;
                }
                int target = oldEffect.getMobCount();
                for (Skill skill : player.getSkills().keySet()) {
                    int bonusSkillLevel = player.getSkillLevel(skill);
                    if (bonusSkillLevel > 0 && skill.getId() != attack.skill) {
                        MapleStatEffect bonusEffect = skill.getEffect(bonusSkillLevel);

                        target += bonusEffect.getTargetPlus();
                        target += bonusEffect.getTargetPlus_5th();
                    }
                }

                if (oldEffect.getMobCount() > 0 && player.getSkillLevel(70000047) > 0) {
                    target += SkillFactory.getSkill(70000047).getEffect(player.getSkillLevel(70000047)).getTargetPlus();
                }

                boolean useBulletCount = oldEffect.getBulletCount() > 1;
                int attackCount = useBulletCount ? oldEffect.getBulletCount() : oldEffect.getAttackCount();

                if (attack.skill == 33001205 || attack.skill == 33101213 || attack.skill == 33111212) {
                    attackCount *= oldEffect.getBulletCount();
                }

                if (attack.skill == 61121201 || attack.skill == 400011080) {
                    attackCount += 2;
                }

                if (attack.skill == 22141017 || attack.skill == 22170070) { //마법 잔해
                    attackCount *= attack.hits;
                }

                int bulletBonus = GameConstants.bullet_count_bonus(attack.skill);
                int attackBonus = GameConstants.attack_count_bonus(attack.skill);
                if (bulletBonus != 0 && useBulletCount) {
                    if (player.getSkillLevel(bulletBonus) > 0) {
                        attackCount += SkillFactory.getSkill(bulletBonus).getEffect(player.getSkillLevel(bulletBonus)).getBulletCount();
                    }
                } else if (attackBonus != 0 && !useBulletCount) {
                    if (player.getSkillLevel(attackBonus) > 0) {
                        attackCount += SkillFactory.getSkill(attackBonus).getEffect(player.getSkillLevel(attackBonus)).getAttackCount();
                    }
                }

                if (attack.skill == 400030002) {
                    attackCount += effect.getY();
                }

                Integer plusCount = player.getBuffedValue(MapleBuffStat.Buckshot);
                if (plusCount != null) {
                    attackCount *= plusCount;
                }

                if (player.getBuffedEffect(MapleBuffStat.ShadowPartner) != null || player.getBuffedEffect(MapleBuffStat.Larkness) != null) {
                    attackCount *= 2;
                }

                if (player.getSkillLevel(1220010) > 0) {
                    int skillid[] = {1201011, 1201012, 1211004, 1211006, 1211008, 1221004, 1221009};
                    for (int skill : skillid) {
                        if (attack.skill == skill) {
                            target += SkillFactory.getSkill(1220010).getEffect(player.getSkillLevel(1220010)).getTargetPlus();
                            attackCount += SkillFactory.getSkill(1220010).getEffect(player.getSkillLevel(1220010)).getAttackCount();
                        }
                    }
                }

                if (player.getBuffedEffect(MapleBuffStat.AdrenalinBoost) != null && player.getSkillLevel(21110016) > 0) {
                    attackCount += SkillFactory.getSkill(21110016).getEffect(player.getSkillLevel(21110016)).getX();
                    target += SkillFactory.getSkill(21110016).getEffect(player.getSkillLevel(21110016)).getY();
                }

                if (player.getSkillLevel(3220015) > 0 && attackCount >= 2) {
                    attackCount += SkillFactory.getSkill(3220015).getEffect(player.getSkillLevel(3220015)).getX();
                }

                Integer attackCountX = player.getBuffedValue(MapleBuffStat.AttackCountX);
                int[] blowSkills = {32001000, 32101000, 32111002, 32121002, 400021007};
                if (attackCountX != null) {
                    for (int blowSkill : blowSkills) {
                        if (attack.skill == blowSkill) {
                            attackCount += attackCountX;
                        }
                    }
                }

                if (attack.targets > target) {
                    player.dropMessageGM(-5, attack.skill + " 몹 개체수 > 클라이언트 계산 : " + attack.targets + " / 서버 계산 : " + target);
                    player.dropMessageGM(-6, "개체수가 계산값보다 많습니다.");
//            		return;
                }

                if (attack.hits > attackCount) {
                    player.dropMessageGM(-5, attack.skill + " 공격 횟수 > 클라이언트 계산 : " + attack.hits + " / 서버 계산 : " + attackCount);
                    player.dropMessageGM(-6, "공격 횟수가 계산값보다 많습니다.");
//            		return;
                }
            }
        }
        /*        if (attack.hits > 0 && attack.targets > 0) {
         // Don't ever do this. it's too expensive.
         if (!player.getStat().checkEquipDurabilitys(player, -1)) { //i guess this is how it works ?
         player.dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
         return;
         } //lol
         }*/
        long totDamage = 0;
        final MapleMap map = player.getMap();
        long fixeddmg, totDamageToOneMonster = 0;
        long hpMob = 0;
        final PlayerStats stats = player.getStat();
        byte multikill = 0;
        int CriticalDamage = stats.critical_rate;
        int ShdowPartnerAttackPercentage = 0;

        if (((attack.skill - 64001009) >= -2 && (attack.skill - 64001009) <= 2)) {
            player.getClient().getSession().writeAndFlush(CField.EffectPacket.KadenaMove(-1, attack.skill, attack.chain.x, attack.chain.y, player.isFacingLeft() ? 1 : 0, (short) player.getFH()));
            player.getMap().broadcastMessage(CField.EffectPacket.KadenaMove(player.getId(), attack.skill, attack.chain.x, attack.chain.y, player.isFacingLeft() ? 1 : 0, (short) player.getFH()), player.getPosition());
        }

        if (attack_type == AttackType.RANGED_WITH_SHADOWPARTNER || attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
            final MapleStatEffect shadowPartnerEffect = player.getBuffedEffect(MapleBuffStat.ShadowPartner);
            if (shadowPartnerEffect != null) {
                ShdowPartnerAttackPercentage += shadowPartnerEffect.getX();
            }
        }
        ShdowPartnerAttackPercentage *= (CriticalDamage + 100) / 100;
        if (attack.skill == 4221001) { //amplifyDamage
            ShdowPartnerAttackPercentage *= 10;
        }

        byte overallAttackCount; // Tracking of Shadow Partner additional damage.
        double maxDamagePerHit = 0;
        MapleMonster monster;
        MapleMonsterStats monsterstats;
        List<Triple<Integer, Integer, Integer>> finalMobList = new ArrayList<>();

        if (attack.allDamage != null) {
            for (final AttackPair oned : attack.allDamage) {
                monster = map.getMonsterByOid(oned.objectid);
                if (monster != null && monster.getLinkCID() <= 0) {
                    totDamageToOneMonster = 0;
                    hpMob = monster.getMobMaxHp();
                    monsterstats = monster.getStats();
                    fixeddmg = monsterstats.getFixedDamage();

                    overallAttackCount = 0; // Tracking of Shadow Partner additional damage.
                    long eachd;

                    if (monster.getId() >= 9833070 && monster.getId() <= 9833074) {
                        continue;
                    }

                    for (Pair<Long, Boolean> eachde : oned.attack) {
                        eachd = eachde.left;
                        overallAttackCount++;
                        if (fixeddmg != -1) {
                            if (monsterstats.getOnlyNoramlAttack()) {
                                eachd = attack.skill != 0 ? 0 : fixeddmg;
                            } else {
                                eachd = fixeddmg;
                            }
                        }

                        totDamageToOneMonster += eachd;

                        if (GameConstants.isKaiser(player.getJob()) && player.getSkillLevel(400011118) > 0 && attack.skill != 400011118 && attack.skill != 400011120 && player.getCooldownLimit(400011120) == 0) {
                            List<AdelProjectile> atoms = new ArrayList<>();
                            List<Point> point = new ArrayList<>();

                            point.add(new Point((int) player.getTruePosition().getX(), (int) player.getTruePosition().getY() - 200));
                            point.add(new Point((int) player.getTruePosition().getX() - 200, (int) player.getTruePosition().getY() - 100));
                            point.add(new Point((int) player.getTruePosition().getX(), (int) player.getTruePosition().getY() + 200));
                            point.add(new Point((int) player.getTruePosition().getX() + 200, (int) player.getTruePosition().getY() - 100));
                            point.add(new Point((int) player.getTruePosition().getX() - 200, (int) player.getTruePosition().getY() + 100));
                            point.add(new Point((int) player.getTruePosition().getX() + 200, (int) player.getTruePosition().getY() + 100));
                            for (Point p : point) {
                                AdelProjectile atom = new AdelProjectile(13, player.getId(), monster.getId(), 400011120, 1400, 0, 1, p, new ArrayList<>());
                                atoms.add(atom);
                            }
                            MapleStatEffect eff = SkillFactory.getSkill(400011120).getEffect(player.getSkillLevel(400011120));
                            player.getMap().spawnAdelProjectile(player, atoms, false);
                            player.getClient().getSession().writeAndFlush(CField.skillCooldown(400011120, eff.getW() * 1000));
                            player.addCooldown(400011120, System.currentTimeMillis(), eff.getW() * 1000);
                        }

                        if (player.getBuffedValue(400031030) && System.currentTimeMillis() - player.lastWindWallTime >= player.getBuffedEffect(400031030).getW2() * 1000) {
                            player.lastWindWallTime = System.currentTimeMillis();
                            MapleAtom atom = new MapleAtom(false, player.getId(), 51, true, 400031031, player.getTruePosition().x, player.getTruePosition().y);
                            for (int i = 0; i < player.getBuffedEffect(400031030).getQ2(); ++i) {
                                atom.addForceAtom(new ForceAtom(Randomizer.nextBoolean() ? 1 : 3, Randomizer.rand(30, 60), 10, Randomizer.nextBoolean() ? Randomizer.rand(0, 5) : Randomizer.rand(180, 185), (short) (i * 300)));
                            }
                            player.getMap().spawnMapleAtom(atom);
                        }
                        if (player.getBuffedValue(64121053)) {
                            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
                            mobList.add(new Triple(monster.getObjectId(), (int) monster.getPosition().getY(), monster.getPosition().getY()));
                            if (attack.skill != 64121055) {
                                player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(64121055, mobList, true, 0));
                            }
                        }
                        // 크리에이션
                        if (attack.skill == 151101000 || attack.skill == 151111000 || attack.skill == 151121000 || attack.skill == 151121002) {
                            if (player.getBuffedValue(151101006)) {
                                int skilid = 151101009;
                                Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                                int cooldowntime = skilid == 151101007 ? 9500 : skilid == 151101008 ? 5500 : 1500;
                                if (System.currentTimeMillis() - (player.getLastCreationTime()) >= 0 && weapon != null) {
                                    MapleStatEffect fa = SkillFactory.getSkill(skilid).getEffect(player.getSkillLevel(GameConstants.getLinkedSkill(skilid)));
                                    List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
                                    player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(skilid, mobList, true, 0));
                                    player.setLastCreationTime(System.currentTimeMillis() + cooldowntime);
                                    if (fa.makeChanceResult() && player.getSkillLevel(151100002) > 0) {
                                        SkillFactory.getSkill(151100002).getEffect(player.getSkillLevel(151100002)).applyTo(player, monster.getPosition(), false);
                                    }
                                }
                            }
                        }

                        if (monster != null && player.getBuffedValue(MapleBuffStat.PickPocket) != null) {
                            MapleStatEffect eff = player.getBuffedEffect(MapleBuffStat.PickPocket);
                            switch (attack.skill) {
                                case 0:
                                case 4001334:
                                case 4201004:
                                case 4201005:
                                case 4201012:
                                case 4211002:
                                case 4211004:
                                case 4211011:
                                case 4221007:
                                case 4221010:
                                case 4221014:
                                case 4221016:
                                case 4221052:
                                case 400041002:
                                case 400041003:
                                case 400041004:
                                case 400041005:
                                case 400041025:
                                case 400041026:
                                case 400041027:
                                case 400041039:
                                    handlePickPocket(player, monster, eff);
                                    break;
                            }
                        }
                    }
                    totDamage += totDamageToOneMonster;
                    player.checkMonsterAggro(monster);
                    if (attack.skill != 0 && !SkillFactory.getSkill(attack.skill).isChainAttack() && !effect.isMist() && effect.getSourceId() != 400021030 && !GameConstants.isLinkedSkill(attack.skill) && !GameConstants.isNoApplySkill(attack.skill) && !GameConstants.isNoDelaySkill(attack.skill) && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                        player.dropMessageGM(-5, "타겟이 범위를 벗어났습니다.");
//                	continue;
                    }
                    //제로 - 래피드 타임
                    if (player.getSkillLevel(100000267) > 0) {
                        switch (attack.skill) {
                            //알파
                            case 101001200:
                            case 101000200:
                            case 101000201:
                            case 101101200:
                            case 101100200:
                            case 101100201:
                            case 101111200:
                            case 101110200:
                            case 101110202:
                            case 101110203:
                            case 101120201:
                            case 101120202:
                            case 101120204: {
                                if (player.RapidTimeDetect < 10) {
                                    player.RapidTimeDetect++;
//                                player.getClient().getSession().writeAndFlush(EffectPacket.showBuffeffect(applyto, (101120207, 3)); //이펙트
                                }
                                for (Entry<Skill, SkillEntry> z : player.getSkills().entrySet()) {
                                    if (GameConstants.isZero(z.getKey().getId() / 10000)) {
                                        if (player.skillisCooling(z.getKey().getId())) {
                                            player.changeCooldown(z.getKey().getId(), -4000);
                                        }
                                    }
                                }
                                MapleStatEffect eff = SkillFactory.getSkill(100000276).getEffect(player.getSkillLevel(100000267));
                                eff.applyTo(player, false);
                                break;
                            }
                            //베타
                            case 101001100:
                            case 101000100:
                            case 101000101:
                            case 101101100:
                            case 101100100:
                            case 101100101:
                            case 101111100:
                            case 101110100:
                            case 101110102:
                            case 101110103:
                            case 101120101:
                            case 101120102:
                            case 101120104: {
                                if (player.RapidTimeStrength < 10) {
                                    player.RapidTimeStrength++;
                                }
                                for (Entry<Skill, SkillEntry> z : player.getSkills().entrySet()) {
                                    if (GameConstants.isZero(z.getKey().getId() / 10000)) {
                                        if (player.skillisCooling(z.getKey().getId())) {
                                            player.changeCooldown(z.getKey().getId(), -4000);
                                        }
                                    }
                                }
                                MapleStatEffect eff = SkillFactory.getSkill(100000277).getEffect(player.getSkillLevel(100000267));
                                eff.applyTo(player, false);
                                break;
                            }
                        }
                    }

                    if (player.getSkillLevel(5100015) > 0) {
                        if (player.getBuffedEffect(MapleBuffStat.EnergyCharged) == null) {
                            if (player.getSkillLevel(5120018) > 0) {
                                SkillFactory.getSkill(5120018).getEffect(player.getSkillLevel(5120018)).applyTo(player, false);
                            } else if (player.getSkillLevel(5110014) > 0) {
                                SkillFactory.getSkill(5110014).getEffect(player.getSkillLevel(5110014)).applyTo(player, false);
                            } else {
                                SkillFactory.getSkill(5100015).getEffect(player.getSkillLevel(5100015)).applyTo(player, false);
                            }
                        }

                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

                        MapleStatEffect energyCharge = player.getBuffedEffect(MapleBuffStat.EnergyCharged);

                        int max = energyCharge.getZ(); // 에너지 차지에 도달하기 까지 최대 값

                        int add = energyCharge.getX(); // 에너지 차지량, 타수마다인지 공격마다인지는 모름.

                        if (monster.getStats().isBoss()) {
                            add *= 2; // 보스는 에너지가 2배로 찬다.
                        }

                        if (attack.skill == 400051015) {
                            add = 0;
                        }

                        if (!player.energyCharge && player.energy + add < max) {
                            player.energy = Math.min(max, player.energy + add);
                            player.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeCooling(false);
                            player.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeActived(false);
                        } else if (!player.energyCharge && player.energy + add >= max) {
                            player.energyCharge = true;
                            player.energy = max;
                            player.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeActived(true);
                        } else if (player.energyCharge) {

                            int forceCon = 0;
                            if (attack.skill == 400051015) {
                                forceCon = effect.getX() / attack.targets;
                                if (monster.getStats().isBoss()) {
                                    forceCon = forceCon * (100 - effect.getZ()) / 100;
                                }
                            } else if (attack.skill != 0) {
                                forceCon = effect.getForceCon() / attack.targets;// 공격 스킬마다 에너지 사용량이 다르다. (타겟수로 나누면 1공격당 1회만 까인 것과 같다.)
                            }

                            player.energy = Math.max(player.energy - forceCon, 0);

                            if (player.energy > 0) {
                                player.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeCooling(true);
                            } else {
                                player.getBuffedEffect(MapleBuffStat.EnergyCharged).setEnergyChargeCooling(false);
                                player.energyCharge = false;
                            }
                        }

                        statups.put(MapleBuffStat.EnergyCharged, new Pair<>(player.energy, 0));

                        player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(MapleBuffStat.EnergyCharged), player));
                        player.getMap().broadcastMessage(player, BuffPacket.giveForeignBuff(player, statups, player.getBuffedEffect(MapleBuffStat.EnergyCharged)), false);
                    }

                    if (attack.skill == 2100001) {
                        List<Triple<MonsterStatus, MonsterStatusEffect, Long>> statusz = new ArrayList<>();
                        Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
                        MapleStatEffect eff = SkillFactory.getSkill(2100001).getEffect(player.getSkillLevel(2100001));
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, eff.getDOTTime()), (long) (eff.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                        monster.applyStatus(player.getClient(), applys, effect);
                    }

                    if (player.getBuffedValue(MapleBuffStat.QuiverCatridge) != null && attack.skill != 3100010 && attack.skill != 400031029 && attack.skill != 95001000) {
                        boolean adquiver = player.getBuffedValue(MapleBuffStat.AdvancedQuiver) != null;
                        boolean quiverFoolburst = player.getBuffedValue(MapleBuffStat.QuiverFullBurst) != null;
                        boolean reset = false;
                        MapleStatEffect quiverEff = SkillFactory.getSkill(3101009).getEffect(player.getSkillLevel(3101009));
                        if (quiverFoolburst) {
                            MapleStatEffect quiverFoolEff = player.getBuffedEffect(MapleBuffStat.QuiverFullBurst);
                            if (Randomizer.isSuccess(quiverEff.getW()) && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                                player.addHP((int) (player.getStat().getCurrentMaxHp() * (quiverEff.getX() / 100.0D)));
                            }

                            monster.applyStatus(player.getClient(), MonsterStatus.MS_Burned, new MonsterStatusEffect(quiverEff.getSourceId(), quiverEff.getDuration()), quiverEff.getDOT(), effect);

                            MapleAtom atom = new MapleAtom(false, player.getId(), 10, true, 3100010, player.getTruePosition().x, player.getTruePosition().y);
                            atom.setDwFirstTargetId(0);
                            ForceAtom forceAtom = new ForceAtom(0, Randomizer.rand(0xA, 0x14), Randomizer.rand(0x5, 0xA), Randomizer.rand(0x4, 0x12D), (short) Randomizer.rand(0x14, 0x30));
                            atom.addForceAtom(forceAtom);
                            player.getMap().spawnMapleAtom(atom);

                            if (System.currentTimeMillis() - player.lastFireArrowTime >= 2000) {
                                player.lastFireArrowTime = System.currentTimeMillis();

                                MapleAtom atom2 = new MapleAtom(false, player.getId(), 50, true, 400031029, monster.getTruePosition().x, monster.getTruePosition().y);
                                for (int i = 0; i < quiverFoolEff.getY(); ++i) {
                                    atom2.addForceAtom(new ForceAtom(
                                            1 // 방향?
                                            ,
                                             Randomizer.rand(30, 60) //Randomizer.rand(0, 1) == 1 ? 1 : 28 // ? 포물선?
                                            ,
                                             10 // 속도?
                                            ,
                                             Randomizer.nextBoolean() ? Randomizer.rand(10, 15) : Randomizer.rand(190, 195) // 각도
                                            ,
                                             (short) (i * 100))); // 딜레이);
                                }

                                atom2.setDwFirstTargetId(0);

                                player.getMap().spawnMapleAtom(atom2);

                            }

                        } else {
                            switch (player.getQuiverType()) {
                                case 1:
                                    if (Randomizer.isSuccess(quiverEff.getW()) && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                                        if (!adquiver) {
                                            player.getRestArrow()[0] -= 10000;
                                        }
                                        player.addHP((int) (player.getStat().getCurrentMaxHp() * (quiverEff.getX() / 100.0D)));
                                    }
                                    break;
                                case 2:
                                    if (!adquiver) {
                                        player.getRestArrow()[1] -= 100;
                                    }
                                    monster.applyStatus(player.getClient(), MonsterStatus.MS_Burned, new MonsterStatusEffect(quiverEff.getSourceId(), quiverEff.getDOTTime()), (int) (quiverEff.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000), effect);
                                    break;
                                case 3:
                                    if (Randomizer.isSuccess(quiverEff.getU())) {
                                        if (!adquiver) {
                                            player.getRestArrow()[2] -= 1;
                                        }

                                        MapleAtom atom = new MapleAtom(false, player.getId(), 10, true, 3100010, player.getTruePosition().x, player.getTruePosition().y);
                                        atom.setDwFirstTargetId(0);
                                        ForceAtom forceAtom = new ForceAtom(0, Randomizer.rand(0xA, 0x14), Randomizer.rand(0x5, 0xA), Randomizer.rand(0x4, 0x12D), (short) Randomizer.rand(0x14, 0x30));
                                        atom.addForceAtom(forceAtom);
                                        player.getMap().spawnMapleAtom(atom);
                                    }
                                    break;
                            }
                        }
                        if (player.getQuiverType() == 0) {
                            player.setQuiverType((byte) 1);
                        }
                        if (player.getRestArrow()[player.getQuiverType() - 1] == 0) {
                            if (player.getRestArrow()[0] == 0 && player.getRestArrow()[1] == 0 && player.getRestArrow()[2] == 0) {
                                reset = true;
                            } else {
                                player.setQuiverType((byte) (player.getQuiverType() == 3 ? 1 : player.getQuiverType() + 1));
                            }
                        }
                        if (!adquiver && !quiverFoolburst) {
                            quiverEff.applyTo(player, reset, false);
                        }
                    }

                    if (player.getBuffedEffect(MapleBuffStat.PinkbeanMinibeenMove) != null) {
                        if (player.getBuffedEffect(MapleBuffStat.PinkbeanMinibeenMove).makeChanceResult()) {
                            player.getBuffedEffect(MapleBuffStat.PinkbeanMinibeenMove).applyTo(player, false);
                        }
                    }

                    //쉐도우 배트
                    if (player.getBuffedValue(MapleBuffStat.ShadowBatt) != null && attack.skill != 14000028 && attack.skill != 14000029) {
                        MapleStatEffect b_eff = SkillFactory.getSkill(14000027).getEffect(player.getSkillLevel(14001027));
                        int BatLimit = 3;
                        int Chance = b_eff.getProp();
                        int skillids[] = {14100027, 14110029, 14120008};
                        int bullets[] = {14001020, 14101020, 14101021, 14111020, 14111022, 14111023, 14121001, 14121002};

                        for (int skill : skillids) {
                            if (player.getSkillLevel(skill) > 0) {
                                Chance += SkillFactory.getSkill(skill).getEffect(player.getSkillLevel(skill)).getProp();
                                BatLimit += SkillFactory.getSkill(skill).getEffect(player.getSkillLevel(skill)).getMobCount();
                            }
                        }

                        List<MapleSummon> summons = new ArrayList<MapleSummon>();

                        for (MapleSummon summon : player.getSummons()) {
                            if (summon.getSkill() == 14000027) {
                                summons.add(summon);
                            }
                        }

                        for (int bullet : bullets) {
                            if (attack.skill == bullet) {
                                player.battAttackCount++;
                                if (summons.size() > 0) { // 이미 소환된 박쥐가 있을 경우
                                    if (Randomizer.isSuccess(Chance)) { // prop확률로 날아가 공격
                                        MapleSummon deleted = summons.get(Randomizer.nextInt(summons.size()));

                                        //박쥐를 투사체로 만들어서 공격.
                                        MapleAtom atom = new MapleAtom(false, player.getId(), 15, true, 14000028, deleted.getTruePosition().x, deleted.getTruePosition().y);
                                        atom.setDwFirstTargetId(monster.getObjectId());
                                        ForceAtom forceAtom = new ForceAtom(player.getSkillLevel(14120008) > 0 ? 2 : 1, 1, 5, Randomizer.rand(45, 90), (short) 500);
                                        atom.addForceAtom(forceAtom);
                                        player.getMap().spawnMapleAtom(atom);

                                        //투사체로 변한 박쥐는 서몬에서 사라짐.
                                        deleted.removeSummon(player.getMap(), false, false);
                                        player.removeSummon(deleted);
                                    }
                                }
                            }
                        }

                        if (player.battAttackCount == b_eff.getZ() || player.getBuffedValue(14121052) || attack.skill == 400041037) {
                            player.battAttackCount = 0;
                            if (summons.size() < BatLimit) {
                                b_eff.applyTo(player, player.getPosition(), false, b_eff.getW() * 1000);
                            }
                        }
                    }

                    if (player.getBuffedValue(MapleBuffStat.HiddenPossession) != null) {
                        MapleStatEffect b_eff = SkillFactory.getSkill(25100009).getEffect(player.getSkillLevel(25101009));
                        List<ForceAtom> atoms = new ArrayList<>();

                        final List<MapleMapObject> objs = player.getMap().getMapObjectsInRange(player.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                        final List<Integer> monsters = new ArrayList<>();
                        for (int i = 0; i < b_eff.getBulletCount() + (player.getSkillLevel(25120153) > 0 ? 2 : 1); i++) {
                            int rand;
                            if (objs.size() <= 1) {
                                rand = 1;
                            } else {
                                rand = Randomizer.nextInt(objs.size());
                            }
                            if (objs.size() < b_eff.getBulletCount()) {
                                if (i < objs.size()) {
                                    monsters.add(objs.get(i).getObjectId());
                                }
                            } else if (objs.size() > 1) {
                                monsters.add(objs.get(rand).getObjectId());
                                objs.remove(rand);
                            }
                        }

                        if (monsters.size() > 0 && attack.skill != 25120115 && attack.skill != 25100010) {
                            if (player.getSkillLevel(25120110) > 0) {
                                MapleStatEffect c_eff = SkillFactory.getSkill(25120110).getEffect(player.getSkillLevel(25120110));
                                int fireProp = c_eff.getProp();
                                if (player.getSkillLevel(25120154) > 0) {
                                    fireProp += SkillFactory.getSkill(25120154).getEffect(1).getProp();
                                }
                                if (Randomizer.isSuccess(fireProp)) { //불 여우령 공격
                                    MapleAtom atom = new MapleAtom(false, player.getId(), 14, true, 25120115, player.getTruePosition().x, player.getTruePosition().y);
                                    for (Integer m : monsters) {
                                        atom.addForceAtom(new ForceAtom(5, 0x2C, 3, 240, (short) 0));
                                    }
                                    atom.setDwFirstTargetId(monster.getObjectId());
                                    player.getMap().spawnMapleAtom(atom);
                                } else if (b_eff.makeChanceResult()) {
                                    MapleAtom atom = new MapleAtom(false, player.getId(), 13, true, 25100010, player.getTruePosition().x, player.getTruePosition().y);
                                    for (Integer m : monsters) {
                                        atom.addForceAtom(new ForceAtom(2, 0x11, 0x1A, 0x27, (short) 630));
                                    }
                                    atom.setDwFirstTargetId(monster.getObjectId());
                                    player.getMap().spawnMapleAtom(atom);
                                }
                            } else if (b_eff.makeChanceResult()) {
                                MapleAtom atom = new MapleAtom(false, player.getId(), 13, true, 25100010, player.getTruePosition().x, player.getTruePosition().y);
                                for (Integer m : monsters) {
                                    atom.addForceAtom(new ForceAtom(2, 0x11, 0x1A, 0x27, (short) 630));
                                }
                                atom.setDwTargets(monsters);
                                player.getMap().spawnMapleAtom(atom);
                            }
                        }
                    }

                    if (GameConstants.isAngelicBuster(player.getJob())) {
                        if (player.getBuffedValue(65121011) && attack.skill != 65120011 && attack.skill != 65111007 && attack.skill != 60011216) {
                            int prop = player.getBuffedEffect(65121011).getProp();
                            if (attack.skill == 65121100) {
                                prop += player.getBuffedEffect(65121011).getZ();
                            }
                            if (player.getBuffedEffect(MapleBuffStat.SoulExalt) != null) {
                                prop += player.getBuffedValue(MapleBuffStat.SoulExalt);
                            }
                            if (Randomizer.isSuccess(prop)) {
                                MapleAtom atom = new MapleAtom(false, player.getId(), 25, true, 65111007, player.getTruePosition().x, player.getTruePosition().y);
                                List<Integer> monsters = new ArrayList<>();
                                monsters.add(monster.getObjectId());
                                for (Integer m : monsters) {
                                    atom.addForceAtom(new ForceAtom(1, Randomizer.rand(10, 20), Randomizer.rand(40, 65), 0, (short) 0));
                                }

                                atom.setDwTargets(monsters);
                                player.getMap().spawnMapleAtom(atom);
                            }
                        }

                        if (player.getBuffedValue(400051011)) {
                            MapleSummon s = player.getSummon(400051011);
                            if (s == null) {
                                player.dropMessageGM(6, "EnergyBurst Null Point");
                            } else {

                                MapleAtom atom = new MapleAtom(true, monster.getObjectId(), 29, true, 400051011, monster.getTruePosition().x, monster.getTruePosition().y);
                                atom.setDwUserOwner(player.getId());
                                atom.setDwFirstTargetId(monster.getObjectId());
                                atom.addForceAtom(new ForceAtom(Randomizer.rand(1, 3), Randomizer.rand(30, 50), Randomizer.rand(0, 10), Randomizer.rand(40, 65), (short) 0));

                                player.getMap().spawnMapleAtom(atom);
                                player.setEnergyBurst(player.getEnergyBurst() + 1);

                                if (player.getEnergyBurst() == 50) {
                                    player.setBuffedValue(MapleBuffStat.EnergyBurst, 3);

                                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                    statups.put(MapleBuffStat.EnergyBurst, new Pair<>(player.getBuffedValue(MapleBuffStat.EnergyBurst), (int) player.getBuffLimit(400051011)));

                                    player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(MapleBuffStat.EnergyBurst), player));

                                    player.getClient().getSession().writeAndFlush(SummonPacket.updateSummon(s, 14));
                                } else if (player.getEnergyBurst() == 25) {
                                    player.setBuffedValue(MapleBuffStat.EnergyBurst, 2);

                                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                    statups.put(MapleBuffStat.EnergyBurst, new Pair<>(player.getBuffedValue(MapleBuffStat.EnergyBurst), (int) player.getBuffLimit(400051011)));

                                    player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(MapleBuffStat.EnergyBurst), player));
                                    player.getClient().getSession().writeAndFlush(SummonPacket.updateSummon(s, 13));
                                }
                            }
                        }

                        if (player.getBuffedValue(400051046)) {

                            MapleSummon summon = player.getSummon(400051046);

                            if (summon != null) {
                                player.getClient().getSession().writeAndFlush(SummonPacket.DeathAttack(summon, Randomizer.rand(8, 9)));
                            }
                        }
                    }

                    if (GameConstants.isKadena(player.getJob())) {
                        int[] changes = {64001002, 64101001, 64101002, 64111002, 64111003, 64111012, 64121003, 64121021, 64121011, 64121016, 64121023, 64121022, 64121024};
                        for (int change : changes) {
                            if (attack.skill == change) {
                                if (!player.getWeaponChanges().contains(change) && player.getWeaponChanges().size() < 8) { // 없으면 넣어.
                                    player.getWeaponChanges().add(change);
                                }
                                if ((attack.skill == 64101002 && player.wingDagger) || attack.skill != 64101002) {
                                    if (attack.skill == 64101002) {
                                        player.wingDagger = false;
                                    }
                                    if (player.getSkillLevel(64120006) > 0) {
                                        SkillFactory.getSkill(64120006).getEffect(player.getSkillLevel(64120006)).applyTo(player, false);
                                    } else if (player.getSkillLevel(64110005) > 0) {
                                        SkillFactory.getSkill(64110005).getEffect(player.getSkillLevel(64110005)).applyTo(player, false);
                                    } else if (player.getSkillLevel(64100004) > 0) {
                                        SkillFactory.getSkill(64100004).getEffect(player.getSkillLevel(64100004)).applyTo(player, false);
                                    }
                                }
                            }
                        }
                    }

                    if (GameConstants.isFusionSkill(attack.skill)) {
                        MapleStatEffect magicWreck;
                        if (player.getSkillLevel(22170070) > 0) {
                            magicWreck = SkillFactory.getSkill(22170070).getEffect(player.getSkillLevel(22170070));
                        } else {
                            magicWreck = SkillFactory.getSkill(22141017).getEffect(player.getSkillLevel(22141017));
                        }
                        if (System.currentTimeMillis() - player.lastEvanMagicWtime >= magicWreck.getU()) {
                            List<MapleMagicWreck> mw = new ArrayList<>();
                            for (MapleMagicWreck mw2 : player.getMap().getWrecks()) {
                                if (mw2.getChr().getId() == player.getId()) {
                                    mw.add(mw2);
                                }
                            }
                            if (mw.size() < magicWreck.getMobCount()) {
                                MapleMagicWreck mw1 = new MapleMagicWreck(player, magicWreck.getSourceId(), monster.getTruePosition(), magicWreck.getDuration() / 1000);
                                player.lastEvanMagicWtime = System.currentTimeMillis();
                                player.getMap().spawnMagicWreck(mw1);

                            }
                        }
                    }

                    if (!player.getBuffedValue(15121054)) {
                        if ((attack.skill == 15111022 || attack.skill == 15120003) && player.getBuffedEffect(MapleBuffStat.CygnusElementSkill, 15001022) != null) {
                            MapleStatEffect lightning = SkillFactory.getSkill(attack.skill).getEffect(attack.skilllevel);
                            lightning.applyTo(player, false);

                            player.cancelEffectFromBuffStat(MapleBuffStat.IgnoreTargetDEF, 15001022);
                            player.cancelEffectFromBuffStat(MapleBuffStat.IndiePmdR, 15001022);
                        }
                    }

                    if (attack.skill == 4201004) {
                        monster.handleSteal(player);
                    }

                    if (player.getSkillLevel(3110001) > 0) {
                        if (attack.skill != 95001000 && attack.skill != 3100010 && attack.skill != 400031029 && attack.skill != 5221022 && attack.skill != 5220025 && attack.skill != 5220024 && attack.skill != 5220023) {
                            SkillFactory.getSkill(3110001).getEffect(player.getSkillLevel(3110001)).applyTo(player, false, false);
                        }
                    }

                    if (player.getSkillLevel(3210001) > 0) {
                        if (attack.skill != 95001000 && attack.skill != 3100010 && attack.skill != 400031029) {
                            SkillFactory.getSkill(3210001).getEffect(player.getSkillLevel(3210001)).applyTo(player, false, false);
                        }
                    }

                    if (player.getSkillLevel(3110012) > 0) {
                        if (attack.skill != 95001000 && attack.skill != 3100010 && attack.skill != 400031029) {
                            MapleStatEffect concentration = SkillFactory.getSkill(3110012).getEffect(player.getSkillLevel(3110012));
                            if (System.currentTimeMillis() - player.lastConcentrationTime >= concentration.getY()) {
                                player.lastConcentrationTime = System.currentTimeMillis();
                                if (player.getConcentration() < 100) {
                                    player.setConcentration((byte) (player.getConcentration() + concentration.getX()));
                                }
                                if ((player.getBuffedValue(3110012) && player.getConcentration() < player.getSkillLevel(3110012)) || !player.getBuffedValue(3110012)) {
                                    concentration.applyTo(player, false, false);
                                }
                            }
                        }
                    }

                    if (attack.skill == 3301008) {

                        MapleAtom atom = new MapleAtom(true, player.getId(), 58, true, 3301009, player.getTruePosition().x, player.getTruePosition().y);
                        atom.setDwUserOwner(player.getId());
                        List<Integer> monsters = new ArrayList<>();
                        monsters.add(monster == null ? 0 : monster.getObjectId());
                        monsters.add(0);
                        for (Integer m : monsters) {
                            atom.addForceAtom(new ForceAtom(Randomizer.nextBoolean() ? 1 : 3, 0x19, 4, 0x15, (short) 120, player.getTruePosition()));
                            atom.addForceAtom(new ForceAtom(Randomizer.nextBoolean() ? 1 : 3, 0x19, 4, 0x15, (short) 120, player.getTruePosition()));
                        }

                        atom.setDwTargets(monsters);
                        player.getMap().spawnMapleAtom(atom);

                    }

                    if (GameConstants.isKaiser(player.getJob()) && player.getBuffedEffect(MapleBuffStat.Morph) == null) {
                        player.handleKaiserCombo();
                    }
                    if (totDamageToOneMonster > 0 || attack.skill == 1221011 || attack.skill == 21120006) {
                        if (GameConstants.isDemonSlayer(player.getJob()) && attack.skill != 31101002) {
                            player.handleForceGain(monster.getObjectId(), attack.skill);
                        }
                        if ((GameConstants.isPhantom(player.getJob())) && (player.getSkillLevel(24120002) > 0 || player.getSkillLevel(24100003) > 0)) {

                            //느와르 카르트 발사
                            Skill noir = SkillFactory.getSkill(24120002);
                            Skill blanc = SkillFactory.getSkill(24100003);
                            MapleStatEffect ceffect = null;
                            int advSkillLevel = player.getTotalSkillLevel(noir);
                            boolean active = true;
                            if (advSkillLevel > 0) {
                                ceffect = noir.getEffect(advSkillLevel);
                            } else if (player.getSkillLevel(blanc) > 0) {
                                ceffect = blanc.getEffect(player.getTotalSkillLevel(blanc));
                            } else {
                                active = false;
                            }

                            //느와르로 느와르가 다시 발동하지 않는다.
                            if (attack.skill == noir.getId() || attack.skill == blanc.getId()) {
                                active = false;
                            }

                            //발동 할 경우에
                            if (active) {

                                //카드 스택 증가 시키기
                                if (player.getCardStack() < (advSkillLevel > 0 ? 40 : 20)) {
                                    player.setCardStack((byte) (player.getCardStack() + 1));
                                    player.getClient().getSession().writeAndFlush(CField.updateCardStack(false, player.getCardStack()));
                                }

                                //카르트 공격 실행
                                MapleAtom atom = new MapleAtom(false, player.getId(), 1, true, advSkillLevel > 0 ? 24120002 : 24100003, player.getTruePosition().x, player.getTruePosition().y);
                                atom.setDwFirstTargetId(monster.getObjectId());
//                            for (Integer m : monsters) {
                                atom.addForceAtom(new ForceAtom(2, Randomizer.rand(15, 28), Randomizer.rand(7, 11), 8, (short) 0));
                                //                          }
                                player.getMap().spawnMapleAtom(atom);
                            }

                            //마크 오브 팬텀 스택
                            if (player.getSkillLevel(400041040) > 0) {
                                MapleStatEffect eff = SkillFactory.getSkill(400041040).getEffect(player.getSkillLevel(400041040));
                                if (attack.skill == 24001000 || attack.skill == 24111000) {
                                    player.setMarkOfPhantomOid(monster.getObjectId());
                                    eff.applyTo(player, false);
                                } else if (attack.skill == 24121000 || attack.skill == 24121005) {
                                    player.setMarkOfPhantomOid(monster.getObjectId());
                                    player.setUltimateDriverCount(player.getUltimateDriverCount() + 1);
                                    if (player.getUltimateDriverCount() >= eff.getY()) {
                                        player.setUltimateDriverCount(0);
                                        eff.applyTo(player, false);
                                    }
                                }
                            }
                        }

                        if (player.getSkillLevel(80002762) > 0) {
                            if (player.getBuffedEffect(MapleBuffStat.EmpiricalKnowledge) != null && player.empiricalKnowledge != null) {
                                if (map.getMonsterByOid(player.empiricalKnowledge.getObjectId()) != null) {
                                    if (monster.getObjectId() != player.empiricalKnowledge.getObjectId() && monster.getMobMaxHp() > player.empiricalKnowledge.getMobMaxHp()) {
                                        player.empiricalStack = 0;
                                        player.empiricalKnowledge = monster;
                                    }
                                } else {
                                    player.empiricalStack = 0;
                                    player.empiricalKnowledge = monster;
                                }
                            } else {
                                if (player.empiricalKnowledge != null) {
                                    if (monster.getMobMaxHp() > player.empiricalKnowledge.getMobMaxHp()) {
                                        player.empiricalKnowledge = monster;
                                    }
                                } else {
                                    player.empiricalKnowledge = monster;
                                }
                            }
                        }
                        if (attack.skill == 80001762 || attack.skill == 80001770 || attack.skill == 80001761) {
                            if (monster.getScale() <= 100 && !monster.getStats().isBoss())
                                totDamageToOneMonster = monster.getStats().getHp();
                        }
                        monster.damage(player, totDamageToOneMonster, true, attack.skill);

                        Item nk = player.getInventory(MapleInventoryType.USE).getItem(attack.slot);
                        MapleStatEffect markOf = player.getBuffedEffect(MapleBuffStat.MarkofNightLord);

                        if (markOf != null && nk != null) {
                            if (player.getSkillLevel(4120018) > 0) {
                                markOf = SkillFactory.getSkill(4120018).getEffect(player.getSkillLevel(4120018));
                                markOf.setDuration(20000);
                            } else {
                                markOf = SkillFactory.getSkill(4100011).getEffect(1);
                                markOf.setDuration(20000);
                            }

                            int bulletCount = markOf.getBulletCount();

                            if (markOf.makeChanceResult() && attack.skill != 4120019 && attack.skill != 4100012) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(markOf.getSourceId(), markOf.getDOTTime());
                                Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
                                applys.put(MonsterStatus.MS_Burned, monsterStatusEffect);
                                if (!monster.isBuffed(markOf.getSourceId())) {
                                    monster.applyStatus(player.getClient(), applys, markOf);
                                } else {
                                    player.dropMessageGM(6, "마크오브나이트로드 스킬" + effect.getSourceId() + " 은 이미 적용중이라 넘어감.");
                                }

                            }

                            if (attack.skill != 4120019 && attack.skill != 4100012) {
                                List<ForceAtoms> atoms = new ArrayList<>();
                                int key = 0;
                                if (monster.getBuff(4120018) != null || monster.getBuff(4100011) != null) {
                                    if (attack.skill != 400041020 || (attack.skill == 400041020 && Randomizer.isSuccess(SkillFactory.getSkill(400041020).getEffect(player.getSkillLevel(400041020)).getW()))) {
                                        monster.cancelStatus(MonsterStatus.MS_Burned, player.getSkillLevel(4120018) > 0 ? 4120018 : 4100011);
                                        for (int i = 0; i < bulletCount; i++) {
                                            ForceAtoms atom = new ForceAtoms(monster.getObjectId(), 2 + key, 2, Randomizer.rand(0x29, 0x2C), Randomizer.rand(0x3, 0x4), Randomizer.rand(0x43, 0x124), (short) 200);
                                            atoms.add(atom);
                                            key++;
                                        }
                                    }
                                    player.getMap().broadcastMessage(CField.CreateForceAtom(player.getId(), monster.getObjectId(), atoms, 11, markOf.getSourceId() + 1, monster.getTruePosition(), nk.getItemId(), 0, 0));
                                }
                                /* for(MapleMonster m : attack.mobs){
                                    //if(m.isAlive()){
                                        if(m.getBuff(4120018) != null || m.getBuff(4100011) != null){
                                            if (attack.skill != 400041020 || (attack.skill == 400041020 && Randomizer.isSuccess(SkillFactory.getSkill(400041020).getEffect(player.getSkillLevel(400041020)).getW()))) {
                                                m.cancelStatus(MonsterStatus.MS_Burned,player.getSkillLevel(4120018) > 0 ? 4120018 : 4100011);
                                                for(int i = 0 ; i<  bulletCount; i++){
                                                    ForceAtoms atom = new ForceAtoms(m.getObjectId(), 2 + key, 2, Randomizer.rand(0x29, 0x2C), Randomizer.rand(0x3, 0x4), Randomizer.rand(0x43, 0x124), (short) 200);
                                                    atoms.add(atom);
                                                    key++;
                                                }
                                            }
                                            player.getMap().broadcastMessage(CField.CreateForceAtom(player.getId(), m.getObjectId(), atoms, 11, markOf.getSourceId() + 1, m.getTruePosition(), nk.getItemId(), 0, 0));
                                        }
                                }*/
                            }

                            /*  if (attack.skill != 4120019 && attack.skill != 4100012) {
                                if (attack.skill != 400041020 || (attack.skill == 400041020 && Randomizer.isSuccess(SkillFactory.getSkill(400041020).getEffect(player.getSkillLevel(400041020)).getW()))) {
                                    final List<MapleMapObject> objs = player.getMap().getMapObjectsInRange(player.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                                    final List<MapleMonster> monsters = new ArrayList<>();
                                    for (int i = 0; i < bulletCount; i++) {
                                        int rand;
                                        if (objs.size() <= 1) {
                                            rand = 1;
                                        } else {
                                            rand = Randomizer.nextInt(objs.size());
                                        }
                                        if (objs.size() < bulletCount) {
                                            if (i < objs.size()) {
                                                monsters.add((MapleMonster) objs.get(i));
                                            }
                                        } else if (objs.size() > 1) {
                                            monsters.add((MapleMonster) objs.get(rand));
                                            objs.remove(rand);
                                        }
                                    }

                                    final List<Point> points = new ArrayList<>();
                                    for (MapleMonster mob : monsters) {
                                        points.add(mob.getPosition());
                                    }

                                    if (monsters.size() > 0) {
                                        List<ForceAtoms> atoms = new ArrayList<>();
                                        int key = 0;
                                        for (MapleMonster m : monsters) {
                                            m.cancelStatus(MonsterStatus.MS_Burned);
                                            ForceAtoms atom = new ForceAtoms(m.getObjectId(), 2 + key, 2, Randomizer.rand(0x29, 0x2C), Randomizer.rand(0x3, 0x4), Randomizer.rand(0x43, 0x124), (short) 200);
                                            atoms.add(atom);
                                            key++;
                                        }
                                        player.getMap().broadcastMessage(CField.CreateForceAtom(player.getId(), monsters.get(monsters.size() - 1).getObjectId(), atoms, 11, markOf.getSourceId() + 1, player.getTruePosition(), nk.getItemId(), 0, 0));
                                    }
                                }
                            }*/
                        }

                        if (monster.getId() >= 9500650 && monster.getId() <= 9500654 && totDamageToOneMonster > 0 && player.getGuild() != null) {
                            player.getGuild().updateGuildScore(totDamageToOneMonster);
                        }
                        if (attack.skill == 155121306) {
                            if (player.getBuffedEffect(155101006) == null) {
                                SkillFactory.getSkill(155101006).getEffect(1).applyTo(player, false);
                            }
                        }
                        if (monster.isBuffed(MonsterStatus.MS_PCounter) && player.getBuffedEffect(MapleBuffStat.IgnorePImmune) == null && player.getBuffedEffect(MapleBuffStat.IgnorePCounter) == null && player.getBuffedEffect(MapleBuffStat.IgnoreAllCounter) == null && player.getBuffedEffect(MapleBuffStat.IgnoreAllImmune) == null && !GameConstants.isNoReflectDamageSkill(SkillFactory.getSkill(attack.skill)) && !energy) { //공반
                            player.addHP(-monster.getBuff(MonsterStatus.MS_PCounter).getValue());
                        }

                        if (player.getBuffedEffect(MapleBuffStat.Alterego) != null && System.currentTimeMillis() - player.lastAltergoTime >= 1500) {
                            player.lastAltergoTime = System.currentTimeMillis();

                            MapleAtom atom = new MapleAtom(false, player.getId(), 60, true, 164101004, player.getTruePosition().x, player.getTruePosition().y);
                            List<Integer> monsters = new ArrayList<>();
                            monsters.add(monster.getObjectId());
                            monsters.add(monster.getObjectId());
                            monsters.add(monster.getObjectId());
                            for (Integer m : monsters) {
                                atom.addForceAtom(new ForceAtom(1, 0x29, 3, 0xD4, (short) 0));
                            }

                            atom.setDwTargets(monsters);
                            player.getMap().spawnMapleAtom(atom);
                        }

                        if (!monster.isAlive()) {
                            multikill++;
                        }

                        if (attack.skill == 164001001 && monster.isBuffed(MonsterStatus.MS_Calabash)) {

                            MapleAtom atom = new MapleAtom(true, monster.getObjectId(), 63, true, 164001001, monster.getTruePosition().x, monster.getTruePosition().y);
                            atom.setDwUserOwner(player.getId());
                            atom.setDwFirstTargetId(monster.getObjectId());
                            atom.addForceAtom(new ForceAtom(1, 5, 30, 0, (short) 0));

                            player.getMap().spawnMapleAtom(atom);
                        }

                        if (player.getBuffedValue(MapleBuffStat.ButterflyDream) != null && attack.skill != 164120007) { // 호접지몽 나비 소환
                            if (player.getCooldownLimit(164120007) == 0) {
                                Skill butterfly = SkillFactory.getSkill(164121007);
                                MapleStatEffect eff = butterfly.getEffect(player.getSkillLevel(164121007));
                                final List<MapleMapObject> objs = player.getMap().getMapObjectsInRange(player.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                                if (objs.size() > 0) {
                                    List<Integer> mobs = new ArrayList<>();
                                    int count = Randomizer.rand(1, eff.getU2());
                                    for (int i = 0; i < count; i++) {
                                        int random = Randomizer.nextInt(objs.size());
                                        if (!mobs.contains(objs.get(random).getObjectId())) {
                                            mobs.add(objs.get(random).getObjectId());
                                        }
                                    }
                                    if (attack.skill != 164120007 && mobs.size() > 0) {
                                        List<ForceAtoms> atoms = new ArrayList<>();
                                        for (int i = 0; i < (count * 1); i++) {
                                            ForceAtoms atom = new ForceAtoms(0, 2 + i, 1 + i, Randomizer.rand(30, 60), 10, Randomizer.nextBoolean() ? Randomizer.rand(0, 5) : Randomizer.rand(180, 185), (short) 0);
                                            atoms.add(atom);
                                        }
                                        player.getMap().broadcastMessage(CField.CreateForceAtom(0, player.getId(), atoms, 61, 164120007, player.getTruePosition(), 0, 0, 0, mobs, 0));
                                    }
                                }
                                player.addCooldown(164120007, System.currentTimeMillis(), eff.getX() * 1000);
                            }
                        }

                        if (effect != null && monster.isAlive()) {
                            //////////////////////////////////////
                            //재코딩 - 예인
                            List<Triple<MonsterStatus, MonsterStatusEffect, Long>> statusz = new ArrayList<>();
                            List<Triple<MonsterStatus, MonsterStatusEffect, Long>> statusz2 = new ArrayList<>();
                            Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
                            Map<MonsterStatus, MonsterStatusEffect> applys2 = new HashMap<>();
                            switch (attack.skill) {
                                case 1101012: // 콤보 포스 (아랫키X)
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                case 1111003: // 패닉
//                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Blind, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pad, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getZ()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mad, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getZ()));
                                    break;
                                case 1121015: // 인사이징
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Incizing, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                case 1201011: { // 플레임 차지

                                    int[] charges = {1201011, 1201012, 1211008, 1221004};

                                    for (int charge : charges) {
                                        if (monster.isBuffed(charge)) {
                                            monster.cancelSingleStatus(monster.getBuff(charge));
                                        }
                                    }
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }

                                case 1201012: { // 블리자드 차지

                                    int[] charges = {1201011, 1201012, 1211008, 1221004};

                                    for (int charge : charges) {
                                        if (monster.isBuffed(charge)) {
                                            monster.cancelSingleStatus(monster.getBuff(charge));
                                        }
                                    }
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    break;
                                }
                                case 1201013: { // 페이지 오더
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 1211008: { // 라이트닝 차지

                                    int[] charges = {1201011, 1201012, 1211008, 1221004};

                                    for (int charge : charges) {
                                        if (monster.isBuffed(charge)) {
                                            monster.cancelSingleStatus(monster.getBuff(charge));
                                        }
                                    }

                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 1221004: { // 디바인 차지

                                    int[] charges = {1201011, 1201012, 1211008, 1221004};

                                    for (int charge : charges) {
                                        if (monster.isBuffed(charge)) {
                                            monster.cancelSingleStatus(monster.getBuff(charge));
                                        }
                                    }

                                    if (!monster.getStats().isBoss()) {
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Seal, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getDuration()));
                                    }
                                    break;
                                }
                                case 1301012: { // 스피어 풀링
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, 1000), (long) 1));
                                    break;
                                }
                                case 2111007: { // 텔레포트 마스터리
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 2121055: { // 메기도 플레임
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                //썬콜 빙결 중첩
                                case 2221012: {
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getV()));

                                    if (monster.getFreezingOverlap() < 5) {
                                        monster.setFreezingOverlap((byte) (monster.getFreezingOverlap() + 1));
                                    }

                                    if (attack.skill == 2221011 && !monster.isBuffed(2221011)) {
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, 13000), (long) effect.getDuration()));
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(attack.skill, 13000), (long) effect.getY()));
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, 13000), (long) effect.getX()));
                                    }
                                    break;
                                }
                                case 2221052: {
                                    if (monster.getFreezingOverlap() > 0) {
                                        monster.setFreezingOverlap((byte) (monster.getFreezingOverlap() - 1));
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, 8000), (long) (-15 * monster.getFreezingOverlap())));
                                    }
                                    break;
                                }

                                case 2211007:
                                case 2311007: { //텔레포트 마스터리
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                    break;
                                }

                                case 3101005: { // 애로우 봄
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 3111003: { // 플레임 샷
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 3121014: { // 운즈 샷
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_DebuffHealing, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getW()));
                                    break;
                                }
                                case 3121052: { // 윈드 오브 프레이
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 3201008: { // 네트 쓰로잉
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getZ()));
                                    break;
                                }
                                case 4111003: { // 쉐도우 웹
                                    if (!monster.getStats().isBoss()) {
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Web, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    }
                                    break;
                                }
                                case 4121016: { // 써든레이드
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 4121017: { // 쇼다운 챌린지
                                    int data = effect.getX();
                                    if (player.getSkillLevel(4120045) > 0) {
                                        data += SkillFactory.getSkill(4120045).getEffect(1).getX();
                                    }
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Showdown, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) data));
                                    break;
                                }
                                case 4201004: { // 스틸
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 4221010: { // 써든 레이드
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 4321002: { // 플래시 뱅
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Blind, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_AdddamParty, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 10));
                                    break;
                                }
                                case 4321004: { // 어퍼 스텝
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_RiseByToss, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 100));
                                    break;
                                }
                                case 4331006: { // 사슬 지옥
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 4341011: { // 써든 레이드`
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 5011002: { // 기간틱 벡스텝
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getZ()));
                                    break;
                                }
                                case 5121001: { // 드래곤 스트라이크
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_MultiPMDR, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    break;
                                }
                                case 5111002: { // 에너지 버스터
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 5311002: { // 몽키 웨이브
                                    if (attack.charge == 1000) {
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    }
                                    break;
                                }
                                case 5310011: { // 몽키 퓨리어스
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_AdddamSkill, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getZ()));
                                    break;
                                }
                                case 13121052: { // 몬순
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 21100002: { // 파이널 차지
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 21100013: { // 롤링 스핀
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 21101016: { // 파이널 토스
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_RiseByToss, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 100));
                                    break;
                                }
                                case 21110011:
                                case 21110024:
                                case 21110025:
                                case 21111017: { // 저지먼트
                                    if (!monster.getStats().isBoss()) {
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    }
                                    break;
                                }
                                case 23111002: { // 유니콘 스파이크
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_AdddamParty, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    break;
                                }
                                case 23121002: { // 레전드리 스피어
                                    long data = effect.getY();
                                    if (player.getSkillLevel(23120050) > 0) {
                                        data += SkillFactory.getSkill(23120050).getEffect(1).getX();
                                    }
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), data));
                                    break;
                                }
                                case 23121003: { // 라이트닝 엣지
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_AdddamSkill2, new MonsterStatusEffect(23121000, effect.getDuration()), (long) effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_DodgeBodyAttack, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 23120013: { // 어드밴스드 스트라이크 듀얼샷
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, 15000), (long) 2));
                                    break;
                                }
//                                case 25101004:
//                                case 25101003:
//                                case 25111004: { // 파쇄철조
//                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_FinalDam, new MonsterStatusEffect(25100011, 15000), (long) 1));
//                                    break;
//                                }
                                case 25120003: { // 폭류권 4타
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 25121006: { // 사혼 각인
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 25121007: { // 분혼 격참
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_SeperateSoulP, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getLevel()));
                                    break;
                                }
                                case 31101002: { // 데몬 트레이스
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 31111001: { // 데쓰 드로우
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 31111005: { // 데모닉 브레스
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 31121001: { // 데몬 임팩트
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    break;
                                }
                                case 31101003: { // 다크 리벤지
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 31121000: { // 데몬 익스플로젼
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_RiseByToss, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 100));
                                    break;
                                }
                                case 31121003: { // 데빌 크라이
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pad, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mad, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    //         statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Blind, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getZ()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Showdown, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getW()));
                                    break;
                                }
                                case 31211011: { // 실드 차지
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 31221002: { // 아머 브레이크
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getY()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getY()));
                                    break;
                                }
                                case 33101215: { // 크로스 로드
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 51121007: // 소울 어썰트
                                case 51121009: { // 샤이닝 크로스
//                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Blind, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    break;
                                }
                                case 51121052: { // 데들리 차지
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_DeadlyCharge, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getX()));
                                    break;
                                }
                                case 61101101:
                                case 61111217: { // 피어스 러쉬
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 61111100:
                                case 61111113:
                                case 61111218: { // 윙비트
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getZ()));
                                    break;
                                }
                                case 61111101:
                                case 61111219: { // 체인 풀링
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 64001000: { // 체인아츠:체이스
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getS2()), (long) -effect.getX()));
                                    break;
                                }
                                case 64001001: { // 체인아츠:스트로크
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -effect.getX()));
                                    break;
                                }
                                case 64001009:
                                case 64001010:
                                case 64001011: { // 체인아츠:체이스
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) -10));
                                    break;
                                }
                                case 64111003: { // 서먼 슬래싱 나이프
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getW()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getW()));
                                    break;
                                }
                                case 64121016: { // 서먼 비팅 니들배트 3타
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pad, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getU()));
                                    break;
                                }
                                case 65101100: { // 스팅 익스플로젼
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Explosion, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 65121002: { // 피니투라 페투치아
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_AdddamParty, new MonsterStatusEffect(attack.skill, 20000), (long) 25));
                                    break;
                                }
                                case 65121100: { // 프라이멀 로어
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 100001283: { // 쉐도우 레인
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, effect.getZ() * 1000), (long) 1));
                                    break;
                                }
                                case 151111002: { // 게더링
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Gathering, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getX()));
                                    break;
                                }
                                case 164001001: { // 마봉 호로부
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Calabash, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                    break;
                                }
                                case 164121044: { // 선기 : 분신 둔갑 태을선인
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill,27000), (long) 1));
                                    break;
                                }
                                case 400021001: { // 도트 퍼니셔
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 400031022: { // 아이들 웜
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    break;
                                }
                                case 24121010: { //이친구는 이상하다 버리자..
                                    //  statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_IgnoreDefence, new MonsterStatusEffect(attack.skill, effect.getDuration()),(long) 1));
                                    break;
                                }
                                // 바인드
                                case 1221052:
                                case 31121006:
                                case 11121013:
                                case 11121004:
                                case 36121053:
                                case 14121004:
                                case 101120110:
                                case 400001008:
                                case 400011015:
                                case 151121040:
                                case 155121007:
                                case 155121306:
                                case 64121001:
                                case 142110011:
                                case 400011121:
                                case 25111206:
                                case 31221003: {
                                    if (!monster.isBuffed(MonsterStatus.MS_Freeze)) {
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, effect.getSourceId() == 25111206 ? effect.getSubTime() / 100 : attack.skill == 400011015 ? effect.getW() * 1000 : attack.skill == 64121001 ? effect.getDuration() : effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getDuration()));//effect.getDuration()
                                        break;
                                    }
                                }
                                default: { //패시브 스킬 효과
                                    if (player.getSkillLevel(5110000) > 0) {
                                        MapleStatEffect stunMastery = SkillFactory.getSkill(5110000).getEffect(player.getSkillLevel(5110000));
                                        if (stunMastery.makeChanceResult()) {
                                            applys.put(MonsterStatus.MS_Stun, new MonsterStatusEffect(5110000, 1000, 1));
                                        }
                                        break;
                                    }
                                    if (player.getBuffedEffect(MapleBuffStat.SnowCharge) != null) {
                                        applys.put(MonsterStatus.MS_Speed, new MonsterStatusEffect(player.getBuffSource(MapleBuffStat.SnowCharge), player.getBuffedEffect(MapleBuffStat.SnowCharge).getY() * (monster.getStats().isBoss() ? 500 : 1000), -player.getBuffedEffect(MapleBuffStat.SnowCharge).getQ() / (monster.getStats().isBoss() ? 2 : 1)));
                                        break;
                                    }
                                    if (player.getSkillLevel(25110210) > 0) {
                                        MapleStatEffect weakness = SkillFactory.getSkill(25110210).getEffect(player.getSkillLevel(25110210));
                                        if (!monster.isBuffed(25110210)) {
                                            if (weakness.makeChanceResult()) {
                                                applys.put(MonsterStatus.MS_Acc, new MonsterStatusEffect(25110210, weakness.getDuration(), weakness.getY()));
                                                applys.put(MonsterStatus.MS_Eva, new MonsterStatusEffect(25110210, weakness.getDuration(), weakness.getZ()));
                                                applys.put(MonsterStatus.MS_AdddamSkill2, new MonsterStatusEffect(25110210, weakness.getDuration(), weakness.getX()));
                                            }
                                        }
                                        break;
                                    }

                                    if (player.getSkillLevel(36110005) > 0) {
                                        MapleStatEffect triangleFormation = SkillFactory.getSkill(36110005).getEffect(player.getSkillLevel(36110005));
                                        if (triangleFormation.makeChanceResult()) {
                                            monster.setAirFrame(monster.getAirFrame() + 1);
                                            applys.put(MonsterStatus.MS_Invincible, new MonsterStatusEffect(36110005, triangleFormation.getDuration(), monster.getAirFrame()));
                                            if (monster.getAirFrame() < 3) {
                                                applys.put(MonsterStatus.MS_Blind, new MonsterStatusEffect(36110005, triangleFormation.getDuration(), -triangleFormation.getX() * monster.getAirFrame()));
                                                applys.put(MonsterStatus.MS_Eva, new MonsterStatusEffect(36110005, triangleFormation.getDuration(), -triangleFormation.getX() * monster.getAirFrame()));
                                            } else {
                                                monster.setAirFrame(0);
                                                player.getMap().broadcastMessage(CField.TriangleDamage(monster));
                                            }
                                            player.getClient().getSession().writeAndFlush(MobPacket.applyStatusAttack(monster.getObjectId(), attack.skill, triangleFormation));
                                        }
                                        break;

                                    }
                                    if (player.getSkillLevel(101120110) > 0 && player.getGender() == 1) {
                                        int resist = SkillFactory.getSkill(101120110).getEffect(player.getSkillLevel(101120110)).getW() * 1000;
                                        if (System.currentTimeMillis() - monster.getLastCriticalBindTime() > resist) {
                                            statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(101120110, SkillFactory.getSkill(101120110).getEffect(player.getSkillLevel(101120110)).getDuration()), (long) effect.getDuration()));
                                            monster.setLastCriticalBindTime(System.currentTimeMillis());
                                        } else {
                                            player.getClient().getSession().writeAndFlush(MobPacket.monsterResist(monster, player, (int) ((resist - (System.currentTimeMillis() - monster.getLastCriticalBindTime())) / 1000), 101120110));
                                        }
                                        break;
                                    }
                                    if (player.getSkillLevel(101110103) > 0 && player.getGender() == 1) {
                                        if (player.armorSplit < SkillFactory.getSkill(101110103).getEffect(player.getSkillLevel(101110103)).getX()) {
                                            player.armorSplit++;
                                        }
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(101110103, SkillFactory.getSkill(101110103).getEffect(player.getSkillLevel(101110103)).getDuration()), (long) (effect.getY() * player.armorSplit)));
                                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(101110103, SkillFactory.getSkill(101110103).getEffect(player.getSkillLevel(101110103)).getDuration()), (long) (effect.getY() * player.armorSplit)));
                                        break;
                                    }
                                    break;
                                }
                            }

                            int sk = 0;
                            boolean enhance = false;
                            int[] venoms = {4110011, 4210010, 4320005};
                            for (int venom : venoms) {
                                if (player.getSkillLevel(venom) > 0) {
                                    sk = venom;
                                }
                            }

                            if (sk > 0) {
                                int[] fatals = {4120011, 4220011, 4340012};
                                for (int fatal : fatals) {
                                    if (player.getSkillLevel(fatal) > 0) {
                                        enhance = true;
                                        sk = fatal;
                                    }
                                }

                                MapleStatEffect venomEffect = SkillFactory.getSkill(sk).getEffect(player.getSkillLevel(sk));
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(venomEffect.getSourceId(), venomEffect.getDOTTime());
                                if (venomEffect.makeChanceResult()) {
                                    if (monster.isBuffed(MonsterStatus.MS_Burned)) {
                                        if (monster.getBurnedBuffSize(sk) < (enhance ? venomEffect.getDotSuperpos() : 1)) {
                                            statusz2.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, monsterStatusEffect, (long) (venomEffect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                        }
                                    } else {
                                        statusz2.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, monsterStatusEffect, (long) (venomEffect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                    }
                                }
                            }

                            if (player.getBuffedValue(MapleBuffStat.BleedingToxin) != null) { // 블리딩 톡신
                                final MapleStatEffect eff = player.getBuffedEffect(MapleBuffStat.BleedingToxin);
                                if (eff != null && eff.makeChanceResult()) {
                                    final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(eff.getSourceId(), eff.getDOTTime());
                                    statusz2.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, monsterStatusEffect, (long) (eff.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                }
                            } else if (player.getBuffedValue(MapleBuffStat.ElementDarkness) != null) {
                                final MapleStatEffect eff = player.getBuffedEffect(MapleBuffStat.ElementDarkness);
                                if (eff != null && eff.makeChanceResult()) {
                                    applys.put(MonsterStatus.MS_Burned, new MonsterStatusEffect(eff.getSourceId(), eff.getDOTTime(), eff.getDOT()));
                                    applys.put(MonsterStatus.MS_ElementDarkness, new MonsterStatusEffect(eff.getSourceId(), eff.getDuration(), monster.getStati().containsKey(MonsterStatus.MS_ElementDarkness) ? 2 : 1));
                                }
                                if (player.getSkillLevel(14120009) > 0 && player.getBuffedEffect(MapleBuffStat.Protective) == null) {
                                    SkillFactory.getSkill(14120009).getEffect(player.getSkillLevel(14120009)).applyTo(player, false);
                                }
                            }

                            for (Triple<MonsterStatus, MonsterStatusEffect, Long> status : statusz) {
                                if (status.left != null && status.mid != null) {
                                    if (!status.mid.shouldCancel(System.currentTimeMillis())) {
                                        if (status.left == MonsterStatus.MS_Burned && status.right < 0) {
                                            status.right = status.right & 0xFFFFFFFFL;
                                        }
                                        if (status.mid.getSkill() == 51121009 ? Randomizer.isSuccess(effect.getY()) : status.mid.getSkill() == 64121016 ? Randomizer.isSuccess(effect.getS2()) : effect.makeChanceResult()) {
                                            status.mid.setValue(status.right);
                                            applys.put(status.left, status.mid);
                                        } else if (attack.skill == 1211008) {
                                            status.mid.setValue(status.right);
                                            applys.put(status.left, status.mid);
                                        }
                                    }
                                }
                            }

                            for (Triple<MonsterStatus, MonsterStatusEffect, Long> status : statusz2) {
                                if (status.left != null && status.mid != null) {
                                    if (!status.mid.shouldCancel(System.currentTimeMillis())) {
                                        if (status.left == MonsterStatus.MS_Burned && status.right < 0) {
                                            status.right = status.right & 0xFFFFFFFFL;
                                        }
                                        if (status.mid.getSkill() == 51121009 ? Randomizer.isSuccess(effect.getY()) : status.mid.getSkill() == 64121016 ? Randomizer.isSuccess(effect.getS2()) : effect.makeChanceResult()) {
                                            status.mid.setValue(status.right);
                                            applys2.put(status.left, status.mid);
                                        } else if (attack.skill == 1211008) {
                                            status.mid.setValue(status.right);
                                            applys2.put(status.left, status.mid);
                                        }
                                    }
                                }
                            }

                            MapleStatEffect elementSoul = player.getBuffedEffect(11001022);
                            if (elementSoul != null) {
                                if (elementSoul.makeChanceResult()) {
                                    applys2.put(MonsterStatus.MS_Freeze, new MonsterStatusEffect(elementSoul.getSourceId(), elementSoul.getSubTime(), elementSoul.getSubTime()));
                                }
                            }

                            if (attack.skill == 13111021 && attack.hits == 2) {
                                applys.put(MonsterStatus.MS_AdddamSkill2, new MonsterStatusEffect(effect.getSourceId(), 0, effect.getX()));
                            }
                            if (attack.skill == 63121006) { //도트뎀 계산이 다이상해 ㅠㅠ
                                applys.put(MonsterStatus.MS_Burned, new MonsterStatusEffect(effect.getSourceId(), (int) effect.getDOTTime(), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                            }

                            if (monster != null && monster.isAlive()) {
                                if (!monster.isBuffed(effect.getSourceId())) {
                                    monster.applyStatus(player.getClient(), applys, effect);
                                } else {
                                    player.dropMessageGM(6, "몬스터디버프 스킬" + effect.getSourceId() + " 은 이미 적용 중이라 넘어감.");
                                }
                                if (applys2.size() > 0) {
                                    monster.applyStatus(player.getClient(), applys2, effect);
                                }
                            }

                            if (!applys.isEmpty()) {
                                if (player.getSkillLevel(80002770) > 0) {
                                    SkillFactory.getSkill(80002770).getEffect(player.getSkillLevel(80002770)).applyTo(player, false);
                                }
                            }
                        }

                        if (monster.isBuffed(MonsterStatus.MS_ElementResetBySummon)) {
                            monster.cancelStatus(MonsterStatus.MS_ElementResetBySummon);
                        }

                        if (monster.isBuffed(MonsterStatus.MS_JaguarBleeding)) {
                            if (monster.getBuff(MonsterStatus.MS_JaguarBleeding).getValue() == 3) {
                                finalMobList.add(new Triple<>(monster.getObjectId(), 0, 0));
                            }
                        }

                        if (player.getBuffedValue(400031000)) {
                            player.getMap().broadcastMessage(CField.ForceAtomAttack(1, player.getId(), monster.getObjectId()));
                        }

                        if (attack.skill != 400041035 && attack.skill != 400041036 && player.getBuffedValue(400041035) && (System.currentTimeMillis() - player.lastChainArtsFuryTime >= 1000)) {
                            player.lastChainArtsFuryTime = System.currentTimeMillis();
                            player.getMap().broadcastMessage(CField.ChainArtsFury(monster.getTruePosition()));
                        }

                        if (player.getBuffedValue(400011016)) {
                            MapleStatEffect installMaha = player.getBuffedEffect(400011016);
                            if (System.currentTimeMillis() - player.lastInstallMahaTime >= installMaha.getX() * 1000) {
                                player.lastInstallMahaTime = System.currentTimeMillis();
                                List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
                                player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011020, mobList, true, 0));
                            }
                        }

                        //////////////////////////////////////
                        if (attack.skill == 31211001 && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                            player.addHP(player.getStat().getCurrentMaxHp() * effect.getY() / 100);
                        }

                        if (totDamage > 0 && attack.skill == 4221016 && player.getSkillLevel(400041025) > 0) {
                            if (player.shadowerDebuffOid == 0) {
                                player.shadowerDebuff = Math.min(3, player.shadowerDebuff + 1);
                                player.shadowerDebuffOid = monster.getObjectId();
                            } else {
                                if (player.shadowerDebuffOid != monster.getObjectId()) {
                                    player.shadowerDebuff = 1;
                                    player.shadowerDebuffOid = monster.getObjectId();
                                } else {
                                    player.shadowerDebuff = Math.min(3, player.shadowerDebuff + 1);
                                }
                            }
                        }

                        if (attack.skill == 400041026) {
                            player.shadowerDebuff = 0;
                            player.shadowerDebuffOid = 0;
                            player.cancelEffectFromBuffStat(MapleBuffStat.ShadowerDebuff);
                        }

                        if (attack.skill == 5221015) {
                            player.guidedBullet = monster.getObjectId();
                        }

                        if (attack.skill == 151121001) {
                            player.graveObjectId = monster.getObjectId();
                        }

                        if (player.getBuffedValue(13121054) && attack.skill != 13121054) {
                            if (player.getBuffedEffect(MapleBuffStat.StormBringer).makeChanceResult()) {
                                MapleAtom atom = new MapleAtom(false, player.getId(), 8, true, 13121054, player.getTruePosition().x, player.getTruePosition().y);
                                atom.setDwFirstTargetId(0);
                                atom.addForceAtom(new ForceAtom(Randomizer.nextBoolean() ? 1 : 3, Randomizer.rand(30, 60), 10, Randomizer.nextBoolean() ? Randomizer.rand(0, 5) : Randomizer.rand(180, 185), (short) 0));
                                player.getMap().spawnMapleAtom(atom);
                            }
                        }

                        if (player.getBuffedValue(400031002) && attack.skill != 400030002) {
                            if (player.lastArrowRain == 0 || player.lastArrowRain < System.currentTimeMillis()) {
                                MapleStatEffect arrowRain = player.getBuffedEffect(400031002);
                                SkillFactory.getSkill(400030002).getEffect(arrowRain.getLevel()).applyTo(player, monster.getTruePosition(), arrowRain.getT() * 1000);
                                player.lastArrowRain = System.currentTimeMillis() + arrowRain.getX() * 1000;
                            }
                        }

                        if (player.getBuffedValue(400041008) && attack.skill != 14121003 && attack.skill != 400040008) { //쉐도우스피어 재코딩..
                            if (player.getBuffedEffect(400041008).makeChanceResult()) {
                                MapleStatEffect shdowspeer = SkillFactory.getSkill(400040008).getEffect(player.getSkillLevel(400041008));
                                Rectangle r = shdowspeer.calculateBoundingBox(monster.getPosition(), monster.isFacingLeft());
                                MapleMist mist = new MapleMist(r, player, shdowspeer, 1800, (byte) 0);
                                mist.setPosition(monster.getPosition());
                                map.spawnMist(mist, false);
                            }
                        }

                        if (player.getSkillLevel(32101009) > 0 && !monster.isAlive() && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                            player.addHP(player.getStat().getCurrentMaxHp() * SkillFactory.getSkill(32101009).getEffect(player.getSkillLevel(32101009)).getKillRecoveryR() / 100);
                        }

                        if (attack.skill == 400041037) {
                            MapleStatEffect shadowBite = SkillFactory.getSkill(400041037).getEffect(player.getSkillLevel(400041037));
                            if (!monster.isAlive()) {
                                player.shadowBite = Math.min(shadowBite.getQ(), player.shadowBite + shadowBite.getY());

                                MapleAtom atom = new MapleAtom(true, monster.getObjectId(), 42, true, 400041037, monster.getTruePosition().x, monster.getTruePosition().y);
                                atom.setDwUserOwner(player.getId());
                                atom.setDwFirstTargetId(0);
                                atom.addForceAtom(new ForceAtom(2, 42, 6, 33, (short) Randomizer.rand(2500, 3000)));
                                player.getMap().spawnMapleAtom(atom);
                            }
                            if (monster.getStats().isBoss()) {
                                player.shadowBite = Math.min(shadowBite.getQ(), player.shadowBite + shadowBite.getW());
                                MapleAtom atom = new MapleAtom(true, monster.getObjectId(), 42, true, 400041037, monster.getTruePosition().x, monster.getTruePosition().y);
                                atom.setDwUserOwner(player.getId());
                                atom.setDwFirstTargetId(0);
                                atom.addForceAtom(new ForceAtom(2, 42, 6, 33, (short) Randomizer.rand(2500, 3000)));
                                player.getMap().spawnMapleAtom(atom);
                            }
                        }

                        if (attack.skill == 155001000) {
                            SkillFactory.getSkill(155001001).getEffect(attack.skilllevel).applyTo(player, false);
                        }

                        if (attack.skill == 155101002) {
                            SkillFactory.getSkill(155101003).getEffect(attack.skilllevel).applyTo(player, false);
                        }

                        if (attack.skill == 155111003) {
                            SkillFactory.getSkill(155111005).getEffect(attack.skilllevel).applyTo(player, false);
                        }

                        if (attack.skill == 155121003) {
                            SkillFactory.getSkill(155121005).getEffect(attack.skilllevel).applyTo(player, false);
                        }

                        if (attack.skill == 155100009 && player.getSkillLevel(155111207) > 0) {
                            MapleStatEffect mark = SkillFactory.getSkill(155111207).getEffect(player.getSkillLevel(155111207));
                            if (Randomizer.isSuccess(mark.getS())) {
                                if (player.getMap().getWrecks().size() < (player.getKeyValue(1544, "155111207") == 1 ? mark.getY() : mark.getZ())) {
                                    MapleMagicWreck mw = new MapleMagicWreck(player, mark.getSourceId(), monster.getTruePosition(), mark.getQ() * 1000);
                                    player.getMap().spawnMagicWreck(mw);
                                }
                            }
                        }

                        if (player.getBuffedValue(400051007) && attack.skill != 400051007 && attack.skill != 400051013 && (System.currentTimeMillis() - player.lastThunderTime >= player.getBuffedEffect(400051007).getY() * 1000)) {
                            player.lastThunderTime = System.currentTimeMillis();
                            player.getClient().getSession().writeAndFlush(CField.lightningUnionSubAttack(attack.skill, 400051007, player.getSkillLevel(400051007)));
                        }

                        if (player.getTotalSkillLevel(4221054) > 0) { //플립 더 코인
                            player.getClient().getSession().writeAndFlush(CField.OnOffFlipTheCoin(true));
                        }

                        if (attack.skill == 5311002) {
                            player.cancelEffectFromBuffStat(MapleBuffStat.KeyDownTimeIgnore, 5310008);
                        } else if (player.getSkillLevel(5311002) > 0 && !player.getBuffedValue(5310008) && attack.skill != 400051008) {
                            SkillFactory.getSkill(5310008).getEffect(player.getSkillLevel(5311002)).applyTo(player, false);
                        }

                        if ((player.getBuffedValue(MapleBuffStat.PinPointRocket) != null) && attack.skill != 36001005 && System.currentTimeMillis() - player.lastPinPointRocketTime >= player.getBuffedEffect(MapleBuffStat.PinPointRocket).getX() * 1000) {
                            player.lastPinPointRocketTime = System.currentTimeMillis();

                            MapleAtom atom = new MapleAtom(false, player.getId(), 6, true, 36001005, player.getTruePosition().x, player.getTruePosition().y);
                            List<Integer> monsters = new ArrayList<>();

                            monsters.add(0);
                            atom.addForceAtom(new ForceAtom(0, 19, Randomizer.rand(20, 40), Randomizer.rand(40, 200), (short) 0));

                            atom.setDwTargets(monsters);

                            player.getMap().spawnMapleAtom(atom);
                        }

                        if (attack.skill == 400011058 && monster != null) {
                            effect.applyTo(player, false, monster.getTruePosition());
                        }
                        if (multikill >= 3) { //TODO :: 10移댁슫???좎떆 10源뚯? 利앷??⑦궥 ?섏젙
                            long comboexp = monster.getStats().getExp() / 3;
                            player.getClient().getSession().writeAndFlush(InfoPacket.multiKill(multikill, comboexp));
                            player.gainExp(comboexp, false, false, false);
                            if (player.getQuestStatus(100425) == 1) {
                                player.questmobkillcount++;
                                player.questmobkillcount = Math.min(player.questmobkillcount, 100);
                                player.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(100425, "MultiKC=" + player.questmobkillcount + ";"));
                                if (player.questmobkillcount == 100) {
                                    player.getClient().setKeyValue("state", "2");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (attack.skill == 5121013 || attack.skill == 5221013 || attack.skill == 400051040) {
            if (player.getSkillLevel(5121013) > 0 && attack.skill == 5121013 && player.getSkillLevel(400051040) > 0 && player.getCooldownLimit(400051040) <= 8000) {
                player.getClient().getSession().writeAndFlush(CField.skillCooldown(400051040, 8000));
                player.addCooldown(400051040, System.currentTimeMillis(), 8000);
            } else if (player.getSkillLevel(5221013) > 0 && attack.skill == 5221013) {
                if (player.getSkillLevel(400051040) > 0 && player.getCooldownLimit(400051040) <= 8000) {
                    player.getClient().getSession().writeAndFlush(CField.skillCooldown(400051040, 8000));
                    player.addCooldown(400051040, System.currentTimeMillis(), 8000);
                }

                int[] reduceSkills = {5210015, 5210016, 5210017, 5210018, 5220014, 5211007, 5221022, 5220023, 5220024, 5220025};

                for (int reduceSkill : reduceSkills) {
                    if (player.getSkillLevel(reduceSkill) > 0 && player.getCooldownLimit(reduceSkill) > 0) {
                        player.changeCooldown(reduceSkill, (int) -(player.getCooldownLimit(reduceSkill) / 2));
                    }
                }
            } else if (player.getSkillLevel(400051040) > 0 && attack.skill == 400051040 && player.getSkillLevel(5121013) > 0 && player.getCooldownLimit(5121013) <= 8000) {
                player.getClient().getSession().writeAndFlush(CField.skillCooldown(5121013, 8000));
                player.addCooldown(5121013, System.currentTimeMillis(), 8000);
            } else if (player.getSkillLevel(400051040) > 0 && attack.skill == 400051040 && player.getSkillLevel(5221013) > 0 && player.getCooldownLimit(5221013) <= 8000) {
                player.getClient().getSession().writeAndFlush(CField.skillCooldown(5221013, 8000));
                player.addCooldown(5221013, System.currentTimeMillis(), 8000);
            }
        }

        if (!finalMobList.isEmpty()) {
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(33000036, finalMobList, false, 0));
        }

        if (attack.skill >= 400051059 && attack.skill <= 400051067) {
            if (attack.skill != 400051065 && attack.skill != 400051067) {// && attack.bAddAttackProc) {
                SkillFactory.getSkill(400051058).getEffect(attack.skilllevel).applyTo(player);
                player.addCooldown(400051058, System.currentTimeMillis(), 120000);
            }
        }


        // 파이널 어택
        if (totDamage > 0) {
            if (player.getMapId() == 993000500) {
                player.setFWolfDamage(player.getFWolfDamage() + totDamage);
                player.setFWolfAttackCount(player.getFWolfAttackCount() + 1);
                player.dropMessageGM(5, "total damage : " + player.getFWolfDamage());
            }

            int[] finalAttacks = {1100002, 1120013, 1200002, 1300002, 3100001, 3120008, 3200001, 5220020, 11101002, 13101002, 21100010, 21120012, 23100006, 23120012, 33100009, 33120011, 51100002, 51120002};
            Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            for (int finalAttack : finalAttacks) {
                if (player.getSkillLevel(finalAttack) > 0 && weapon != null) {
                    MapleStatEffect fa = SkillFactory.getSkill(finalAttack).getEffect(player.getSkillLevel(finalAttack));

                    int prop = fa.getProp();
                    prop += (player.getBuffedEffect(MapleBuffStat.FinalAttackProp) != null ? player.getBuffedValue(MapleBuffStat.FinalAttackProp) : 0);

                    if (finalAttack == 1100002) {
                        if (player.getSkillLevel(1120013) > 0) {
                            continue;
                        }
                    }
                    if (finalAttack == 1120013) {
                        if (player.getSkillLevel(1120048) > 0) {
                            prop += SkillFactory.getSkill(1120048).getEffect(1).getProp();
                        }
                    }
                    List<MapleMonster> finalattckmobs = new ArrayList<>();
                    for (MapleMonster mat : attack.mobs) {
                        if (mat == null) {
                            continue;
                        }
                        if (mat.isAlive()) {
                            if (Randomizer.isSuccess(Math.min(100, prop))) {
                                finalattckmobs.add(mat);
                            }
                        }
                    }
                    player.getClient().getSession().writeAndFlush(CField.finalAttackRequest(1, attack.skill, finalAttack, (weapon.getItemId() - 1000000) / 10000, finalattckmobs));

                }
            }

            int[] finalAttacks2 = {2120013, 2220014, 5120021, 32121011};
            List<MapleMonster> finalattckmobs = new ArrayList<>();

            for (int finalAttack : finalAttacks2) {
                if (player.getCooldownLimit(GameConstants.getLinkedSkill(finalAttack)) > 0 && weapon != null) {
                    MapleStatEffect fa = SkillFactory.getSkill(finalAttack).getEffect(player.getSkillLevel(GameConstants.getLinkedSkill(finalAttack)));
                    for (MapleMonster m : attack.mobs) {
                        if (m == null) {
                            continue;
                        }
                        if (m.isAlive()) {
                            if (Randomizer.isSuccess(fa.getProp() + (player.getBuffedEffect(MapleBuffStat.FinalAttackProp) != null ? player.getBuffedValue(MapleBuffStat.FinalAttackProp) : 0))) {
                                finalattckmobs.add(m);
                            }
                        }
                    }
                    player.getClient().getSession().writeAndFlush(CField.finalAttackRequest(1, attack.skill, finalAttack, (weapon.getItemId() - 1000000) / 10000, finalattckmobs));
                }
            }

            if (attack.skill == 5321001) {
                if (player.skillisCooling(5311004)) {
                    player.changeCooldown(5311004, (int) -(player.getCooldownLimit(5311004) / 2));
                }

                if (player.skillisCooling(5311005)) {
                    player.changeCooldown(5311005, (int) -(player.getCooldownLimit(5311005) / 2));
                }

                if (player.skillisCooling(5320007)) {
                    player.changeCooldown(5320007, (int) -(player.getCooldownLimit(5320007) / 2));
                }
            }

            if (player.getBuffedValue(4341054)) {
                List<MapleMonster> m = attack.mobs;
                if (m != null) {
                    player.getClient().getSession().writeAndFlush(CField.finalAttackRequest(1, attack.skill, 4341054, (weapon.getItemId() - 1000000) / 10000, m));
                }
            }
            if(attack.skill == 164101004) {
                if (player.getCooldownLimit(400041048) == 0) {
                    player.addCooldown(400041048, System.currentTimeMillis(), (long) (3 * 1000));
                }
            }
            if (GameConstants.isArcher(player.getJob()) && attack.skill != 400031021 && attack.skill != 400031020) {
                if (player.getSkillLevel(400031020) > 0) {
                    if (player.getCooldownLimit(400031020) > 0) {
                        player.setVerseOfRelicsCount(player.getVerseOfRelicsCount() + 1);
                        MapleStatEffect att = SkillFactory.getSkill(400031021).getEffect(player.getSkillLevel(400031020));
                        if (System.currentTimeMillis() - player.lastVerseOfRelicsTime >= (att.getSubTime() / 100) && player.getVerseOfRelicsCount() >= 10) {
                            player.lastVerseOfRelicsTime = System.currentTimeMillis();
                            player.setVerseOfRelicsCount(0);
                            att.applyTo(player, false, 1000);
                        }
                    }
                }
            }

            if (attack.skill == 400011056 && player.getBuffedValue(MapleBuffStat.Ellision) != null) {
                MapleSummon ellision = player.getSummon(400011065);
                if (ellision == null) {
                    SkillFactory.getSkill(400011065).getEffect(player.getSkillLevel(400011055)).applyTo(player, attack.position, player.getBuffedEffect(MapleBuffStat.Ellision).getY() * 1000);
                } else {
                    ellision.setEnergy(ellision.getEnergy() + 1);
                    if (ellision.getEnergy() % 3 == 0) {
                        player.getMap().broadcastMessage(SummonPacket.transformSummon(ellision, (ellision.getEnergy() % 3) + 1));
                    }
                }
            }

            if (GameConstants.isLuminous(player.getJob())) {
                if (!player.getBuffedValue(20040216) && !player.getBuffedValue(20040217) && !player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                    if (GameConstants.isLightSkills(attack.skill)) {
                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
                        player.setLuminusMorph(true);
                    } else if (GameConstants.isDarkSkills(attack.skill)) {
                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
                        player.setLuminusMorph(false);
                    }
                    player.getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(player.getLuminusMorphUse(), player.getLuminusMorph()));
                } else {
                    if (!player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                        if (player.getLuminusMorph()) { // 빛
                            if (GameConstants.isLightSkills(attack.skill)) {
                                if ((player.getLuminusMorphUse() - GameConstants.isLightSkillsGaugeCheck(attack.skill)) <= 0) {
                                    if (player.getSkillLevel(20040219) > 0) {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040219).getEffect(1).applyTo(player, false);
                                    } else {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        player.setLuminusMorph(false);
                                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
                                    }
                                } else {
                                    player.setLuminusMorphUse(player.getLuminusMorphUse() - GameConstants.isLightSkillsGaugeCheck(attack.skill));
                                }
                                if (!player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                                    if (player.getLuminusMorph()) {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
                                    }
                                }
                            }
                        } else {// 어둠
                            if (GameConstants.isDarkSkills(attack.skill)) {
                                if ((player.getLuminusMorphUse() + GameConstants.isDarkSkillsGaugeCheck(player, attack.skill)) >= 10000) {
                                    if (player.getSkillLevel(20040219) > 0) { // 스킬 배우는건 1개임
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040220).getEffect(1).applyTo(player, false);
                                    } else {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        player.setLuminusMorph(true);
                                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
                                    }
                                } else {
                                    player.setLuminusMorphUse(player.getLuminusMorphUse() + GameConstants.isDarkSkillsGaugeCheck(player, attack.skill));
                                }
                                if (!player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                                    if (!player.getLuminusMorph()) {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
                                    }
                                }
                            }
                        }
                        player.getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(player.getLuminusMorphUse(), player.getLuminusMorph()));
                    }
                }
            }

            /*&& !player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)
             }) {
             switch (attack.skill) {
             case 27001100:
             case 27101100:
             case 27101101:
             case 27111100:
             case 27111101:
             case 27121100:
             if (!player.getBuffedValue(20040217)) {
             SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
             }
             break;
             case 27001201:
             case 27101202:
             case 27111202:
             case 27120211:
             case 27121201:
             case 27121202:
             if (!player.getBuffedValue(20040216)) {
             SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
             }
             break;
             }*/
            if (attack.skill == 27121303 && player.getSkillLevel(400021071) > 0) {
                SkillFactory.getSkill(400021071).getEffect(player.getSkillLevel(400021071)).applyTo(player, false);
            }

            if (player.getBuffedValue(32101009) && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                player.addHP(totDamage * player.getBuffedEffect(32101009).getX() / 100);
                if (player.getParty() != null) {
                    for (MaplePartyCharacter pc : player.getParty().getMembers()) {
                        if (pc.getId() != player.getId() && pc.isOnline()) {
                            int ch = World.Find.findChannel(pc.getName());
                            if (ch > 0) {
                                MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(pc.getId());
                                if (chr != null && chr.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                                    chr.addHP(totDamage * player.getBuffedEffect(32101009).getX() / 100);
                                }
                            }
                        }
                    }
                }
            }

            if (player.getBuffedValue(31121002) && System.currentTimeMillis() - player.lastVamTime >= player.getBuffedEffect(31121002).getY() && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                player.lastVamTime = System.currentTimeMillis();
                player.addHP(Math.min(player.getBuffedEffect(31121002).getW(), totDamage * player.getBuffedEffect(31121002).getX() / 100));
                if (player.getParty() != null) {
                    for (MaplePartyCharacter pc : player.getParty().getMembers()) {
                        if (pc.getId() != player.getId() && pc.isOnline()) {
                            MapleCharacter chr = player.getClient().getChannelServer().getPlayerStorage().getCharacterById(pc.getId());
                            if (chr != null && chr.isAlive() && chr.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                                chr.addHP(totDamage * player.getBuffedEffect(31121002).getX() / 100);
                            }
                        }
                    }
                }
            }
        }

        if (player.getBuffSource(MapleBuffStat.DrainHp) == 20031210) {
            player.addHP(totDamage * player.getBuffedValue(MapleBuffStat.DrainHp) / 100);
        }

        if (player.getSkillLevel(1200014) > 0) {
            int skillid[] = {1201011, 1201012, 1211004, 1211006, 1211008, 1221004};
//            if (player.getElementalCharge() < 5) {
            for (int i = 0; i < skillid.length; i++) {
                if (attack.skill == skillid[i]) {
                    if (player.GetSkillid() != skillid[i]) {
                        player.SetSkillid(attack.skill);
                        if (player.GetSkillid() != 0) {
                            player.elementalChargeHandler(1);
                        }
                    }
                }
            }
//            }
        }

        if (attack.skill == 155121306) {
            SkillFactory.getSkill(155121006).getEffect(attack.skilllevel).applyTo(player, false);
        }

        if (attack.skill == 23121000) {
            MapleBuffStatValueHolder ignisRore = player.checkBuffStatValueHolder(MapleBuffStat.IgnisRore);
            if (ignisRore != null) {
                ignisRore.schedule.cancel(false);
                ignisRore.localDuration += 100;

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.IgnisRore, new Pair<>(player.getBuffedValue(MapleBuffStat.IgnisRore), (int) player.getBuffLimit(ignisRore.effect.getSourceId())));

                final CancelEffectAction cancelAction = new CancelEffectAction(player, ignisRore.effect, System.currentTimeMillis(), MapleBuffStat.IgnisRore);
                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                    cancelAction.run();
                }, player.getBuffLimit(ignisRore.effect.getSourceId()));

                ignisRore.schedule = schedule;

                player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, ignisRore.effect, player));

            }
        }

        if (attack.skill != 1311020 && attack.skill != 13120003 && attack.skill != 13120010 && attack.skill != 13110027 && attack.skill != 13110022 && attack.skill != 13100022 && attack.skill != 13100027 && attack.skill != 13101022 && player.getBuffedEffect(MapleBuffStat.TryflingWarm) != null) {
            if (attack.targets > 0) {
                int skillid = player.getBuffedEffect(MapleBuffStat.TryflingWarm).getSourceId();

                if (skillid != 0) {
                    Skill trskill = SkillFactory.getSkill(skillid);
                    if (Randomizer.rand(1, 100) <= (skillid == 13100022 ? 5 : skillid == 13110022 ? 10 : 20)) {
                        skillid = skillid == 13120003 ? 13120010 : skillid == 13110022 ? 13110027 : 13100027;
                        if (player.getSkillLevel(skillid) <= 0) {
                            player.changeSkillLevel(SkillFactory.getSkill(skillid), (byte) player.getSkillLevel(trskill), (byte) player.getSkillLevel(trskill));
                        }
                    }

                    final List<MapleMapObject> objs = player.getMap().getMapObjectsInRange(player.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                    if (objs.size() > 0) {
                        MapleStatEffect eff = trskill.getEffect(player.getSkillLevel(skillid));
                        List<Integer> mobs = new ArrayList<>();
                        int count = Randomizer.rand(1, eff.getX());
                        for (int i = 0; i < count; i++) {
                            if (Randomizer.isSuccess(eff.getProp())) {
                                int random = Randomizer.nextInt(objs.size());
                                if (!mobs.contains(objs.get(random).getObjectId())) {
                                    mobs.add(objs.get(random).getObjectId());
                                    count++;
                                }
                            }
                        }

                        int trychance = trskill.getEffect(player.getSkillLevel(trskill)).getProp();
                        if (player.getSkillLevel(13120044) > 0) {
                            trychance += SkillFactory.getSkill(13120044).getEffect(1).getProp();
                        }

                        if (!GameConstants.isTryFling(attack.skill) && attack.skill != 13121054 && mobs.size() > 0 && (trskill.getEffect(player.getSkillLevel(trskill)).makeChanceResult() || player.hasDonationSkill(13100022))) {
                            MapleAtom atom = new MapleAtom(false, player.getId(), 7, true, skillid, player.getTruePosition().x, player.getTruePosition().y);
                            List<Integer> monsters = new ArrayList<>();
                            int bonusAttack = (int) player.getKeyValue(99999, "triplingBonus");
                            for (int i = 0; i < ((count * (player.getSkillLevel(13120045) == 1 ? 2 : 1)) * (bonusAttack == -1 ? 1 : bonusAttack)); i++) {
                                monsters.add(0);
                                atom.addForceAtom(new ForceAtom(Randomizer.nextBoolean() ? 1 : 3, Randomizer.rand(30, 60), 10, Randomizer.nextBoolean() ? Randomizer.rand(0, 5) : Randomizer.rand(180, 185), (short) 0));
                            }
                            atom.setDwTargets(monsters);
                            player.getMap().spawnMapleAtom(atom);
                        }
                    }
                }
            }
        }

        int[] linkCooldownSkills = {23121052, 400031007, 23111002, 23121002};
        for (int ck : linkCooldownSkills) {
            if (attack.skill == ck && player.getCooldownLimit(ck) > 0 && attack.isLink) {
                player.changeCooldown(ck, -1000);
            }
        }

        if (attack.skill == 400031032 || attack.skill == 400031033) {
            player.setWildGrenadierCount(player.getWildGrenadierCount() - 1);
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.WildGrenadier, new Pair<>(player.getWildGrenadierCount(), 0));
            player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(400031032).getEffect(attack.skilllevel), player));
        }

        if (attack.skill == 400041037) {
            SkillFactory.getSkill(400041037).getEffect(attack.skilllevel).applyTo(player, false);
        }

        if (attack.skill == 400051042) {
            player.setBHGCCount(player.getBHGCCount() - 1);
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(player.getBHGCCount(), 0));

            player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(attack.skill).getEffect(attack.skilllevel), player));
        }

        if (totDamage > 0 && player.getSkillLevel(3210013) > 0) {
            MapleStatEffect bar = SkillFactory.getSkill(3210013).getEffect(player.getSkillLevel(3210013));
            player.setBarrier((int) Math.min(player.getStat().getCurrentMaxHp() * bar.getZ() / 100, totDamage * bar.getY() / 100));
            bar.applyTo(player, false);
        }

        if (totDamage > 0 && player.getBuffedValue(65101002)) {
            MapleStatEffect bar = SkillFactory.getSkill(65101002).getEffect(player.getSkillLevel(65101002));
            player.setBarrier((int) Math.min(player.getStat().getCurrentMaxHp(), totDamage * bar.getY() / 100));
            if (player.getBarrier() >= player.getStat().getCurrentMaxHp()) {
                bar.applyTo(player, false, (int) player.getBuffLimit(65101002));
            }
        }

        if (totDamage > 0 && player.getSkillLevel(4221013) > 0) {
            MapleStatEffect instict = SkillFactory.getSkill(4221013).getEffect(player.getSkillLevel(4221013));
            if (player.killingpoint < 3 && attack.skill != 4221016) {
                player.killingpoint++;
                instict.applyTo(player, false, 0);
            } else if (player.killingpoint == 3 && attack.skill == 4221016) {
                if (player.skillisCooling(400041039)) {
                    player.changeCooldown(400041039, -(player.killingpoint * 500));
                }
                player.killingpoint = 0;
                instict.applyTo(player, false, 0);
            }
        }

        if (GameConstants.isDemonSlash(attack.skill)) {
            if (player.getBuffedValue(MapleBuffStat.NextAttackEnhance) == null) {
                if (player.getSkillLevel(31120045) > 0) {
                    SkillFactory.getSkill(31120045).getEffect(1).applyTo(player, false);
                }
            }
        } else {
            if (player.getBuffedValue(MapleBuffStat.NextAttackEnhance) != null) {
                player.cancelEffectFromBuffStat(MapleBuffStat.NextAttackEnhance);
            }
        }

        if (attack.skill == 4341011 && player.getCooldownLimit(4341002) > 0) {
            MapleStatEffect suddenRade = SkillFactory.getSkill(4341011).getEffect(attack.skilllevel);
            player.changeCooldown(4341002, (int) (-player.getCooldownLimit(4341002) * suddenRade.getX() / 100));
        }

        if (player.getBuffedValue(155101008) && attack.skill != 155100009 && attack.skill != 155111207 && player.getBuffedEffect(MapleBuffStat.SpectorTransForm) != null) {
            final List<MapleMapObject> objs = player.getMap().getMapObjectsInRange(player.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
            int max = Math.max(2, objs.size());

            max = Math.min(max, player.getBuffedEffect(155101008).getZ());

            if (player.getBuffedEffect(MapleBuffStat.InfinitySpell) != null) {
                max += 2;
            }

            MapleAtom atom = new MapleAtom(false, player.getId(), 47, true, 155100009, player.getTruePosition().x, player.getTruePosition().y);
            List<Integer> monsters = new ArrayList<>();
            for (int i = 0; i < max; ++i) {
                monsters.add(0);
                atom.addForceAtom(new ForceAtom(0, Randomizer.rand(0xA, 0x14), Randomizer.rand(0x5, 0xA), Randomizer.rand(0x4, 0x12D), (short) 0));
            }

            atom.setDwTargets(monsters);
            player.getMap().spawnMapleAtom(atom);
        }

        if (player.getSkillLevel(80002762) > 0) {
            MapleStatEffect stst = SkillFactory.getSkill(80002762).getEffect(player.getSkillLevel(80002762));

            if (stst.makeChanceResult()) {
                stst.applyTo(player, false);
            }
        }

        if (attack.skill == 5221015 || attack.skill == 151121001) {
            SkillFactory.getSkill(attack.skill).getEffect(attack.skilllevel).applyTo(player, false);
        }

        if (attack.skill == 61121104 || attack.skill == 61121124 || attack.skill == 61121221 || attack.skill == 61121223 || attack.skill == 61121225) {
            SkillFactory.getSkill(61121116).getEffect(attack.skilllevel).applyTo(player, false);
        }

        if (totDamage > 0 && player.getSkillLevel(4200013) > 0) {
            MapleStatEffect criticalGrowing = SkillFactory.getSkill(4200013).getEffect(player.getSkillLevel(4200013));
            if (player.getSkillLevel(4220015) > 0) {
                criticalGrowing = SkillFactory.getSkill(4220015).getEffect(player.getSkillLevel(4220015));
            }
            if (player.criticalGrowing + player.getStat().critical_rate >= 100) {
                player.criticalGrowing = 0;
                player.criticalDamageGrowing = 0;
            } else {
                player.criticalGrowing += criticalGrowing.getX();
                player.criticalDamageGrowing = Math.min(player.criticalDamageGrowing + criticalGrowing.getW(), criticalGrowing.getQ());
            }
            criticalGrowing.applyTo(player, false, 0);
        }

        if (totDamage > 0 && player.getSkillLevel(5221021) > 0) {
            MapleStatEffect quickDraw = SkillFactory.getSkill(5221021).getEffect(player.getSkillLevel(5221021));
            if (!player.getBuffedValue(5221021)) {
                if (quickDraw.makeChanceResult()) {
                    quickDraw.applyTo(player, false);
                }
            } else {
                if (player.getBuffedValue(MapleBuffStat.QuickDraw) > 1) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.QuickDraw, 5221021);
                }
            }
        }

        if (player.getBuffedValue(15001022) && attack.skill != 15111022 && attack.skill != 15120003) {
            MapleStatEffect dkeffect = player.getBuffedEffect(15001022);
            if (attack.skill == 400051016) {
                player.lightning -= 2;
            } else {
                int prop = dkeffect.getProp(), maxcount = dkeffect.getV();
                int[] skills = {15000023, 15100025, 15110026, 15120008};
                for (int skill : skills) {
                    if (player.getSkillLevel(skill) > 0) {
                        prop += SkillFactory.getSkill(skill).getEffect(player.getSkillLevel(skill)).getProp();
                        maxcount += SkillFactory.getSkill(skill).getEffect(player.getSkillLevel(skill)).getV();
                    }
                }
                if (Randomizer.nextInt(100) < prop) {
                    if (player.lightning < maxcount) {
                        player.lightning++;
                    }
                }
                if (player.getBuffedValue(15121004)) { //
                    player.changeCooldown(15121004, -6000);
                }
            }

            if (player.lightning < 0) {
                player.lightning = 0;
            }

            MapleBuffStatValueHolder lightning = player.checkBuffStatValueHolder(MapleBuffStat.CygnusElementSkill);
            if (lightning != null) {
                lightning.effect.applyTo(player, false);
            }
        }

        if (totDamage > 0 && player.getBuffedValue(MapleBuffStat.BMageDeath) != null && attack.skill != player.getBuffSource(MapleBuffStat.BMageDeath)) {
            if (System.currentTimeMillis() - player.lastDeathAttackTime >= player.getBuffedEffect(MapleBuffStat.BMageDeath).getDuration() && (player.getDeath() >= player.getBuffedEffect(MapleBuffStat.BMageDeath).getX() || player.getBuffedEffect(MapleBuffStat.AttackCountX) != null)) {
                player.setDeath((byte) 0);
                player.lastDeathAttackTime = System.currentTimeMillis();
                MapleSummon summon = player.getSummon(player.getBuffSource(MapleBuffStat.BMageDeath));
                if (summon != null) {
                    player.getClient().getSession().writeAndFlush(SummonPacket.DeathAttack(summon));
                }

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.BMageDeath, new Pair<>((int) player.getDeath(), 0));
                player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(MapleBuffStat.BMageDeath), player));
            }
        }

        if (attack.skill == 21000006 || attack.skill == 21000007 || attack.skill == 21001010) {
            if (player.getSkillLevel(21120021) > 0) {
                SkillFactory.getSkill(21120021).getEffect(player.getSkillLevel(21120021)).applyTo(player, false);
            } else if (player.getSkillLevel(21100015) > 0) {
                SkillFactory.getSkill(21100015).getEffect(player.getSkillLevel(21100015)).applyTo(player, false);
            }
        }

        if (attack.skill == 400011079) {
            List<Integer> skills = new ArrayList<>();
            skills.add(400011081);
            player.getClient().getSession().writeAndFlush(CField.rangeAttack(400011079, skills, 0, player.getTruePosition(), player.isFacingLeft()));
        }

        if (attack.skill == 400011080) {
            List<Integer> skills = new ArrayList<>();
            skills.add(400011082);
            player.getClient().getSession().writeAndFlush(CField.rangeAttack(400011080, skills, 0, player.getTruePosition(), player.isFacingLeft()));
        }

        if (attack.isLink && player.getSkillLevel(23110004) > 0) {
            if (attack.skill != 0) {
                if (player.getBuffedValue(23110004)) {
                    if (player.getBuffedValue(MapleBuffStat.IgnisRore) <= 10) {
                        SkillFactory.getSkill(23110004).getEffect(player.getSkillLevel(23110004)).applyTo(player, false);
                    }
                } else {
                    SkillFactory.getSkill(23110004).getEffect(player.getSkillLevel(23110004)).applyTo(player, false);
                }
            }
        }

        if (totDamage > 0 && attack.isLink && player.getSkillLevel(400051044) > 0) {
            if (attack.skill != 0) {
                if (player.getBuffedValue(400051044)) {
                    if (player.getBuffedValue(MapleBuffStat.Striker3rd) <= 10) {
                        SkillFactory.getSkill(400051044).getEffect(player.getSkillLevel(400051044)).applyTo(player, false);
                    }
                } else {
                    SkillFactory.getSkill(400051044).getEffect(player.getSkillLevel(400051044)).applyTo(player, false);
                }
            }
        }

        if (GameConstants.isAngelicBuster(player.getJob()) && totDamage > 0 && attack.skill != 65121101 && attack.skill != 65121007) {
            player.getClient().getSession().writeAndFlush(CField.lockSkill(GameConstants.getLinkedSkill(attack.skill)));
            player.getClient().getSession().writeAndFlush(CField.unlockSkill()); // 안하면 트리니티 잠김 ㅡㅡ
            player.getClient().getSession().writeAndFlush(EffectPacket.showNormalEffect(player, 49, true));
            player.getMap().broadcastMessage(player, EffectPacket.showNormalEffect(player, 49, false), false);

            if (player.getSkillLevel(65120006) > 0) {
                MapleStatEffect affinity = SkillFactory.getSkill(65120006).getEffect(player.getSkillLevel(65120006));
                if (Randomizer.nextBoolean()) { // 50%
                    affinity.applyTo(player, false);
                }
            }
        }

        if (GameConstants.isArk(player.getJob()) && totDamage > 0) {
            if (player.getBuffedEffect(MapleBuffStat.SpectorTransForm) == null) {
                player.giveSpectorGauge(attack.skill);
            }
            player.giverelic(attack.skill);
        }

        if (player.getBuffedValue(400031006) && attack.skill == 400031010) {
            player.trueSniping--;
            if (player.trueSniping <= 0) {
                player.cancelEffectFromBuffStat(MapleBuffStat.TrueSniping);
            } else {
                player.getBuffedEffect(400031006).applyTo(player, false);
            }
        }

        if (attack.skill == 4331003 && (hpMob <= 0 || totDamageToOneMonster < hpMob)) {
            return;
        }
        if (hpMob > 0 && totDamageToOneMonster > 0) {
            player.afterAttack(attack);
        }

        if (player.getBuffedValue(400031007) && totDamageToOneMonster > 0) {
            if (System.currentTimeMillis() - player.lastElementalGhostTime >= player.getBuffedEffect(400031007).getS2() * 1000) {
                player.lastElementalGhostTime = System.currentTimeMillis();
                List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
                player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400031011, mobList, true, 0));
            }
        }

        if (player.getBuffedValue(400011005) && totDamageToOneMonster > 0) {
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            if (System.currentTimeMillis() - player.lastDanceTime >= player.getBuffedEffect(400011005).getS2() * 1000) {
                player.lastDanceTime = System.currentTimeMillis();
                if (player.getBuffedValue(11121011) || player.getBuffedValue(11101022)) {
                    player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011022, mobList, true, 0));
                } else if (player.getBuffedValue(11121012) || player.getBuffedValue(11111022)) {
                    player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011023, mobList, true, 0));
                }
            }
        }

        if (player.getBuffedValue(400051010) && attack.skill >= 25000000 && attack.skill < 26000000) {
            if (System.currentTimeMillis() - player.lastRandomAttackTime >= player.getBuffedEffect(400051010).getX() * 1000) {
                player.lastRandomAttackTime = System.currentTimeMillis();
                List<Integer> skills = new ArrayList<>();

                List<Pair<Integer, Integer>> realSkills = new ArrayList<>();

                switch (Randomizer.nextInt(5)) {
                    case 0:
                        realSkills.add(new Pair<Integer, Integer>(25121000, 0));
                        realSkills.add(new Pair<Integer, Integer>(25120001, 360));
                        realSkills.add(new Pair<Integer, Integer>(25120002, 720));
                        realSkills.add(new Pair<Integer, Integer>(25120003, 1080));
                        break;
                    case 1:
                        realSkills.add(new Pair<Integer, Integer>(25101000, 0));
                        realSkills.add(new Pair<Integer, Integer>(25100001, 360));
                        break;
                    case 2:
                        realSkills.add(new Pair<Integer, Integer>(25110000, 0));
                        realSkills.add(new Pair<Integer, Integer>(25110001, 360));
                        realSkills.add(new Pair<Integer, Integer>(25110002, 720));
                        break;
                    case 3:
                        realSkills.add(new Pair<Integer, Integer>(25111012, 0));
                        break;
                    case 4:
                        realSkills.add(new Pair<Integer, Integer>(25121055, 0));
                        break;
                    case 5:
                        realSkills.add(new Pair<Integer, Integer>(20121005, 0));
                        break;
                }
                player.getClient().getSession().writeAndFlush(CField.SpiritFlow(realSkills));

                if (realSkills.get(0).getLeft() == 25111012) {
                    MapleStatEffect test2 = SkillFactory.getSkill(25111012).getEffect(player.getSkillLevel(25111012));
                    MapleMist mist = new MapleMist(test2.calculateBoundingBox(player.getTruePosition(), player.isFacingLeft()), player, test2, player.getBuffedEffect(400051010).getW() * 1000, (byte) (player.isFacingLeft() ? 1 : 0));
                    mist.setPosition(player.getTruePosition());
                    player.getMap().spawnMist(mist, false);
                }

                if (realSkills.get(0).getLeft() == 25121055) {
                    MapleStatEffect test2 = SkillFactory.getSkill(25121055).getEffect(player.getSkillLevel(25121055));
                    MapleMist mist = new MapleMist(test2.calculateBoundingBox(player.getTruePosition(), player.isFacingLeft()), player, test2, player.getBuffedEffect(400051010).getW() * 1000, (byte) (player.isFacingLeft() ? 1 : 0));
                    mist.setPosition(player.getTruePosition());
                    player.getMap().spawnMist(mist, false);
                }

            }
        }

        int prop = 0;

        if (player.getBuffedValue(MapleBuffStat.DarkSight) != null && !player.getBuffedValue(400001023)) {
            for (MapleMist mist : player.getMap().getAllMistsThreadsafe()) {
                if (mist.getOwnerId() == player.getId() && mist.getSource().getSourceId() == 4221006) {
                    prop = 100;
                    break;
                }
            }

            if (player.getSkillLevel(4210015) > 0) {
                prop = SkillFactory.getSkill(4210015).getEffect(player.getSkillLevel(4210015)).getProp();
            }

            if (player.getSkillLevel(4330001) > 0) {
                prop = SkillFactory.getSkill(4330001).getEffect(player.getSkillLevel(4330001)).getProp();
            }

            if (!Randomizer.isSuccess(prop)) {
                player.cancelEffectFromBuffStat(MapleBuffStat.DarkSight);
            }
        }

        for (MapleMist mist : player.getMap().getAllMistsThreadsafe()) {
            if (mist.getSource() != null) {
                if (mist.getSourceSkill().getId() == attack.skill) {
                    return;
                }
            }
        }

        if (player.getSkillLevel(101110205) > 0 && player.getGender() == 0 && totDamageToOneMonster > 0) {
            MapleStatEffect combatRecovery = SkillFactory.getSkill(101110205).getEffect(player.getSkillLevel(101110205));
            if (combatRecovery.makeChanceResult()) {
                player.addMP(combatRecovery.getZ());
            }
        }

        if (totDamage > 0 && player.getBuffedValue(400021073)) {
            MapleSummon summon = player.getSummon(400021073);
            if (summon != null && summon.getEnergy() < 22) {
                switch (attack.skill) {
                    case 22170060:
                    case 22110022:
                    case 22110023:
                    case 22111012:
                    case 22170070:
                    case 400021012:
                    case 400021014:
                    case 400021015: {
                        MapleAtom atom = new MapleAtom(true, summon.getObjectId(), 29, true, 400021073, summon.getTruePosition().x, summon.getTruePosition().y);
                        atom.setDwUserOwner(summon.getOwner().getId());

                        atom.setDwFirstTargetId(0);
                        atom.addForceAtom(new ForceAtom(1, 0x25, Randomizer.rand(0x5, 0xA), 0x3E, (short) 0));
                        player.getMap().spawnMapleAtom(atom);

                        summon.setEnergy(Math.min(22, summon.getEnergy() + 1));
                        player.getClient().getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
                        player.getClient().getSession().writeAndFlush(SummonPacket.specialSummon(summon, 2));
                        if (summon.getEnergy() >= 22) {
                            player.getClient().getSession().writeAndFlush(SummonPacket.damageSummon(summon));
                        }
                        break;
                    }
                    case 22171063:
                    case 22111011:
                    case 22110014:
                    case 22110024:
                    case 22110025:
                    case 22140014:
                    case 22140015:
                    case 22140023:
                    case 22140024:
                    case 22141011:
                    case 22171095:
                    case 22171083:
                    case 22170064:
                    case 22170065:
                    case 22170066:
                    case 22170067:
                    case 22170093:
                    case 22170094:
                    case 400021013: {
                        if (!summon.getMagicSkills().contains(attack.skill)) {
                            summon.getMagicSkills().add(attack.skill);
                            summon.setEnergy(Math.min(22, summon.getEnergy() + 3));

                            MapleAtom atom = new MapleAtom(true, summon.getObjectId(), 29, true, 400021073, summon.getTruePosition().x, summon.getTruePosition().y);
                            atom.setDwUserOwner(summon.getOwner().getId());
                            List<Integer> monsters = new ArrayList<>();

                            monsters.add(0);
                            atom.addForceAtom(new ForceAtom(1, 0x25, Randomizer.rand(0x5, 0xA), 0x3E, (short) 0));
                            atom.setDwTargets(monsters);
                            player.getMap().spawnMapleAtom(atom);

                            player.getClient().getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
                            player.getClient().getSession().writeAndFlush(SummonPacket.specialSummon(summon, 2));
                            if (summon.getEnergy() >= 22) {
                                player.getClient().getSession().writeAndFlush(SummonPacket.damageSummon(summon));
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (GameConstants.isKinesis(player.getJob())) {
            player.givePPoint(attack.skill, false);
        }

        if (player.getBuffedEffect(MapleBuffStat.ComboInstict) != null && (attack.skill == 1121008 || attack.skill == 1120017)) {
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011074, mobList, true, 0));
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011075, mobList, true, 0));
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011076, mobList, true, 0));
        }

        if (player.getBuffedValue(400011017) && attack.skill != 400011019) {
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400011019, mobList, true, 0));
        }

        if (player.getBuffedValue(400001037) && attack.skill != 400001038 && (System.currentTimeMillis() - player.lastAngelTime >= player.getBuffedEffect(400001037).getZ() * 1000)) {
            player.lastAngelTime = System.currentTimeMillis();
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            int i = 0;
            for (AttackPair a : attack.allDamage) {
                mobList.add(new Triple<Integer, Integer, Integer>(a.objectid, 291 + 70 * i, 0));
                i++;
            }

            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400001038, mobList, true, 0));
        }

        if (attack.skill == 155120000 && player.getSkillLevel(400051047) > 0 && player.getCooldownLimit(400051047) == 0) {
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400051047, mobList, true, 0));
            player.addCooldown(400051047, System.currentTimeMillis(), 10000);
        }

        if (attack.skill == 155120001 && player.getSkillLevel(400051047) > 0 && player.getCooldownLimit(400051047) == 0) {
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(400051048, mobList, true, 0));
        }

        if (attack.skill == 3321014 || attack.skill == 3321016 || attack.skill == 3321018 || attack.skill == 3321020) {
            List<Triple<Integer, Integer, Integer>> mobList = new ArrayList<>();
            player.getClient().getSession().writeAndFlush(CField.bonusAttackRequest(attack.skill + 1, mobList, true, 1000));
        }

        if (attack.skill == 400041042) {
            List<Integer> skills = new ArrayList<>();
            skills.add(400041043);
            player.getClient().getSession().writeAndFlush(CField.rangeAttack(attack.skill, skills, 1, attack.position, player.isFacingLeft()));
        }

        if (player.getBuffedValue(21101005) && totDamageToOneMonster > 0 && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
            player.addHP(player.getStat().getCurrentMaxHp() * player.getBuffedEffect(21101005).getX() / 100);
        }

        if (player.getSkillLevel(1310009) > 0 && totDamageToOneMonster > 0 && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
            if (SkillFactory.getSkill(1310009).getEffect(player.getSkillLevel(1310009)).makeChanceResult()) {
                player.addHP(player.getStat().getCurrentMaxHp() * SkillFactory.getSkill(1310009).getEffect(player.getSkillLevel(1310009)).getX() / 100);
            }
        }

        if (player.getBuffedValue(1321054) && totDamageToOneMonster > 0 && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
            player.addHP(player.getStat().getCurrentMaxHp() * player.getBuffedEffect(1321054).getX() / 100);
        }

        if (player.getSkillLevel(31010002) > 0 && totDamageToOneMonster > 0) {
            MapleStatEffect absorbLife = SkillFactory.getSkill(31010002).getEffect(player.getSkillLevel(31010002));
            if (absorbLife.makeChanceResult()) {
                player.addHP(player.getStat().getCurrentMaxHp() * absorbLife.getX() / 100);
            }
        }

        if (attack.skill == 1221009 && player.getElementalCharge() == 5) {
            player.elementalChargeHandler(-1);
        }

        if (GameConstants.isBlaster(player.getJob())) {
            player.giveCylinderGauge(attack.skill);
        }

        if (GameConstants.isPathFinder(player.getJob())) {
            player.giveRelikGauge(attack.skill, attack);
        }

        if (GameConstants.isHoyeong(player.getJob())) {
            player.giveHoyoungGauge(attack.skill);

            switch (attack.skill) { //공격시 암행버프 은신 시전
                case 164101000:
                case 164111003:
                case 164121000:
                    MapleStatEffect dark = SkillFactory.getSkill(164101006).getEffect(player.getSkillLevel(164101006));
                    dark.applyTo(player);
                    break;
            }

            if (attack.skill == 164121042) { //권술 : 몽유도원
                MapleStatEffect dark = SkillFactory.getSkill(164121042).getEffect(player.getSkillLevel(164121042));
                dark.applyTo(player);
            }
            if (player.getBuffedValue(400041052) && attack.targets != 0) {
                player.setInfinity((byte) (player.getInfinity() + 1));
                if (player.getInfinity() == 12) {
                    for (MapleSummon summon : player.getSummons()) {
                        if (summon.getSkill() == 400041052) {
                            player.setInfinity((byte) 0);
                            player.getClient().getSession().writeAndFlush(SummonPacket.DeathAttack(summon, Randomizer.rand(8, 10)));
                            break;
                        }
                    }
                }
            }
        }

        if (player.getSkillLevel(400041063) > 0) {
            Integer[] skills = {164001000, 164001002, 164101000, 164111000, 164111003, 164111008, 164121000, 164121003, 164121005};
            if (Arrays.asList(skills).contains(attack.skill)) {
                MapleStatEffect sungi = SkillFactory.getSkill(400041063).getEffect(player.getSkillLevel(400041063));
                List<Integer> sungi_skills = new ArrayList<>();

                if (!player.useChun) {
                    sungi_skills.add(400041064);
                }

                if (!player.useJi) {
                    sungi_skills.add(400041065);
                }

                if (!player.useIn) {
                    sungi_skills.add(400041066);
                }

                if (player.getBuffedEffect(MapleBuffStat.SageElementalClone) != null && System.currentTimeMillis() - player.lastSungiAttackTime >= 2 * 1000) {
                    if (!sungi_skills.isEmpty()) {
                        player.lastSungiAttackTime = System.currentTimeMillis();
                        for (int sungi_skill : sungi_skills) {
                            player.getClient().getSession().writeAndFlush(CField.rangeAttack(sungi_skill, Arrays.asList(sungi_skill),1, attack.position, player.isFacingLeft()));
                        }
                    }
                } else if (player.getBuffedEffect(MapleBuffStat.SageElementalClone) == null && System.currentTimeMillis() - player.lastSungiAttackTime >= sungi.getQ() * 1000) {
                    if (!sungi_skills.isEmpty()) {
                        player.lastSungiAttackTime = System.currentTimeMillis();
                        int sungi_skill = sungi_skills.get(Randomizer.nextInt(sungi_skills.size()));
                        player.getClient().getSession().writeAndFlush(CField.rangeAttack(sungi_skill, Arrays.asList(sungi_skill), 1, attack.position, player.isFacingLeft()));
                    }
                }
            }
        }

        if (GameConstants.isAdel(player.getJob()) && attack.targets > 0) {
            player.giveEtherGauge(attack.skill);
        }
        if (player.getSkillLevel(GameConstants.isKain(player.getJob()) ? 60030241 : 80003015) > 0 && attack.targets > 0) { //카인 링크 스킬
            for (MapleMonster m : attack.mobs) {
                if (m != null) {
                    if (m.getStats().isBoss()) {
                        PlayerHandler.HandleKainLink(player, 0, 1);
                    }
                }
            }
        }

        if (GameConstants.isKain(player.getJob())) {
            if (player.getBuffedValue(MapleBuffStat.Possession) != null) {
                if (GameConstants.isKainExpressionSkill(attack.skill)) {

                }
            }

            if (attack.skill == 63121002) {
                int cooltime = effect.getCooltime() == 0 ? effect.getU() * 1000 : effect.getCooltime();
                player.KainfallingDust -= 1;
                player.getClient().getSession().writeAndFlush(CField.KainStackSkill(63121002, player.KainfallingDust, effect.getW(), cooltime));
                player.lastKainfallingDust = System.currentTimeMillis();
            }

            if (player.getBuffedValue(MapleBuffStat.RemainInsence) != null) { //리메인 인센스
                if (attack.targets > 0) {
                    if (attack.skill != 63111010 && attack.skill != 63101006 && attack.skill != 400031063 && attack.skill != 63111010) {
                        List<MapleMagicWreck> removes = new ArrayList<>();
                        for (MapleMapObject mo : map.getMapObjectsInRange(player.getPosition(), 500000, Arrays.asList(MapleMapObjectType.WRECK))) {
                            MapleMagicWreck mw = (MapleMagicWreck) mo;
                            if (player == mw.getChr() && mw.getSourceid() == 63111010) {
                                removes.add(mw);
                                player.getMap().removeMapObject(mo);
                                player.getMap().getWrecks().remove(mw);
                            }
                        }
                        if (removes.size() > 0) {
                            player.getMap().broadcastMessage(CField.removeMagicWreck(player, removes));
                            player.getMap().broadcastMessage(CField.KainRemainInsenceAttack(player, removes));
                        }
                    }
                }
                if (System.currentTimeMillis() - player.lastKainremainInsence >= 300) { //0.3sec
                    if (player.getSkillLevel(63111009) > 0) {
                        MapleStatEffect mark = SkillFactory.getSkill(63111010).getEffect(player.getSkillLevel(63111009));
                        int count = 0;
                        switch (attack.skill) {
                            case 63001100:
                                count = 1;
                                break;
                            case 63101100:
                                count = 2;
                                break;
                            case 63101104:
                                count = 2; //ok
                                break;
                            case 63111105:
                                count = 4;
                                break;
                            case 63121103:
                                count = 8; //ok
                                break;
                            case 63121141:
                                count = 6;
                                break;
                            case 400031061:
                                count = 7;
                                break;
                            case 400031064:
                                count = 8;
                                break;
                        }
                        List<MapleFoothold> f = player.getMap().getFootholds().getAllRelevants();
                        // player.dropMessageGM(6,"count"+count);
                        for (int i = 0; i < count; i++) {
                            for (MapleFoothold f2 : f) {
                                if (!f2.isWall() && Math.abs(player.getFH() - f2.getId()) <= 10) {
                                    MapleMagicWreck mw = new MapleMagicWreck(player, mark.getSourceId(), Randomizer.isSuccess(2) ? f2.getPoint1() : f2.getPoint2(), 10000);
                                    player.getMap().spawnMagicWreck(mw);
                                    Collections.shuffle(f);
                                    break;
                                }
                            }
                        }
                        if (count > 0) {
                            player.lastKainremainInsence = System.currentTimeMillis();
                        }
                    }
                }
            }

            if (attack.targets > 0) {
                if (attack.skill == 63111007
                        ||//쉐도우 스텝 쿨초기화
                        attack.skill == 63121004
                        || attack.skill == 63121006
                        || attack.skill == 400031065) {
                    player.changeCooldown(63001002, -9000);
                    player.changeCooldown(63001003, -9000);
                    player.changeCooldown(63001005, -9000);
                }

                if (player.getSkillLevel(400031066) > 0) {
                    for (MapleMonster m : attack.mobs) {
                        if (m != null) {
                            if (m.getStats().isBoss()) {
                                PlayerHandler.HandleGripOfAgony(player, 0, 1);
                            }
                        }
                    }
                }

                if (attack.skill == 63001000 || attack.skill == 63101003 || attack.skill == 63111002) {
                    List<Integer> skill = new ArrayList<>();
                    skill.add(63001001);
                    player.getClient().getSession().writeAndFlush(CField.rangeAttack(attack.skill, skill, 1, player.getPosition(), player.isFacingLeft()));
                }

                if (player.getBuffedValue(MapleBuffStat.DragonPang) != null && attack.targets > 0 && attack.skill != 63101006 && attack.skill != 400031063 && attack.skill != 63111010) {
                    List<AdelProjectile> swords = new ArrayList<>();
                    List<Integer> points = new ArrayList<>();
                    points.add(player.isFacingLeft() ? 1 : 0);
                    player.DragonPangStack++;
                    List<Integer> ballcount = player.getMap().getAdelProjectile(player, 63101006);
                    if (player.DragonPangStack >= 7 && ballcount.size() < 3) {
                        AdelProjectile sword = new AdelProjectile(0x11, player.getId(), 0, 63101006, 40000, 0, 0x32, new Point(0, 0), points);
                        sword.setDelay(0);
                        swords.add(sword);
                        player.getMap().spawnAdelProjectile(player, swords, false);
                        player.DragonPangStack = 0;
                    }
                    ballcount = player.getMap().getAdelProjectile(player, 63101006);
                    //player.dropMessage(6,"맵에존재하는 드래곤팡 "+ballcount.size());
                    for (int i = 0; i < ballcount.size(); i++) {
                        player.getMap().broadcastMessage(CField.attackAdelProjectile(player, ballcount.get(i), 1));

                    }
                }

                if (player.getBuffedValue(MapleBuffStat.ThanatosDescent) != null && attack.targets > 0 && attack.skill != 400031063 && attack.skill != 63101006 && attack.skill != 63111010) { //스킬 하나쓰면 여러개 나가는것들 추가해야됨
                    if (System.currentTimeMillis() - player.lastThanatosDescentattack >= 3000) {//아톰 공격 재발동 대기시간
                        List<AdelProjectile> swords = new ArrayList<>();
                        List<Integer> points = new ArrayList<>();
                        points.add(player.isFacingLeft() ? 1 : 0);
                        int left = -90;
                        int right = 90;
                        int delay = 600;
                        List<Point> randpos = new ArrayList<>();
                        randpos.add(new Point(100, -100));
                        randpos.add(new Point(200, -50));
                        randpos.add(new Point(0, -150));
                        randpos.add(new Point(-100, -50));
                        randpos.add(new Point(-200, -100));
                        randpos.add(new Point(300, -150));
                        Point pos = player.getTruePosition();
                        for (int i = 0; i < 6; i++) {
                            AdelProjectile sword = new AdelProjectile(0x13, player.getId(), 0, 400031063, 1800, player.isFacingLeft() ? left : right, 1, pos, points);
                            sword.setDelay(delay);
                            pos = new Point((int) player.getTruePosition().getX() + (int) randpos.get(i).getX() + Randomizer.rand(10, 50), (int) player.getTruePosition().getY() + (int) randpos.get(i).getY() - Randomizer.rand(10, 50));
                            delay += 60;
                            swords.add(sword);
                        }
                        player.lastThanatosDescentattack = System.currentTimeMillis();
                        player.getMap().spawnAdelProjectile(player, swords, false);
                    }
                }

                //카인 몬스터 죽음의 축복 스택 쌓기  Triple 구조 <몬스터, 스택 수, 스택쌓은 시간(sec)>
                //   player.dropMessageGM(6,"어플라이어택"+attack.skill);
                if (attack.skill == 63111105 || attack.skill == 63121102 || attack.skill == 400031064 || attack.skill == 400031061) {

                    List<Triple<MapleMonster, Integer, Integer>> send = new ArrayList<>();
                    boolean isBlesStack = true;

                    player.calcKainDeathStackMobs();

                    List<Triple<MapleMonster, Integer, Long>> check = new ArrayList<>();

                    check = player.kainblessmobs;

                    for (MapleMonster m : attack.mobs) {
                        boolean isFrist = true;
                        if (m != null) {
                            if (m.isAlive()) { //몹이 살아있을 경우
                                for (int i = 0; i < check.size(); i++) {
                                    if (check.get(i).left.getObjectId() == m.getObjectId()) {//이미 쌓은 스택이 있는지 찾아본다
                                        player.kainblessmobs.set(i, new Triple<>(m, check.get(i).getMid() >= 15 ? 15 : check.get(i).getMid() + 1, System.currentTimeMillis()));
                                        isFrist = false;
                                        break;
                                    }
                                }
                                if (isFrist) {//첫스택이면 추가
                                    player.kainblessmobs.add(new Triple<>(m, 1, System.currentTimeMillis()));
                                }
                            }
                        }
                    }

                    for (int i = 0; i < player.kainblessmobs.size(); i++) {
                        long duration = System.currentTimeMillis() - player.kainblessmobs.get(i).getRight();
                        send.add(new Triple<>(player.kainblessmobs.get(i).getLeft(), player.kainblessmobs.get(i).getMid(), (int) duration));
                    }

                    if (send.size() <= 0) {
                        isBlesStack = false;
                    }
                    player.getClient().getSession().writeAndFlush(CField.setStacktoMonster(isBlesStack, send, 90000));

                }
                if (attack.skill == 63111008 || attack.skill == 63111007 || attack.skill == 63121005 || attack.skill == 63121007 || attack.skill == 400031065) { //죽음의 축복 실현 스킬

                    List<Triple<MapleMonster, Integer, Integer>> send = new ArrayList<>();
                    List<MapleMonster> deathAttack = new ArrayList<>();
                    boolean isBlesStack = true;
                    boolean isStack = false;

                    player.calcKainDeathStackMobs();

                    List<Triple<MapleMonster, Integer, Long>> check = new ArrayList<>();

                    check = player.kainblessmobs;

                    for (MapleMonster m : attack.mobs) {
                        if (m != null) {
                            if (m.isAlive()) { //몹이 살아있을 경우
                                for (int i = 0; i < check.size(); i++) {
                                    if (check.get(i).left.getObjectId() == m.getObjectId()) {//이미 쌓은 스택이 있는지 찾아본다
                                        player.kainblessmobs.set(i, new Triple<>(m, check.get(i).getMid() - 1, System.currentTimeMillis()));
                                        deathAttack.add(m);
                                        isStack = true;
                                        break;
                                    }
                                }
                            }
                        }

                    }

                    for (int i = 0; i < player.kainblessmobs.size(); i++) {
                        long duration = System.currentTimeMillis() - player.kainblessmobs.get(i).getRight();
                        send.add(new Triple<>(player.kainblessmobs.get(i).getLeft(), player.kainblessmobs.get(i).getMid(), (int) duration));
                    }

                    if (send.size() <= 0) {
                        isBlesStack = false;
                    }

                    player.getClient().getSession().writeAndFlush(CField.setStacktoMonster(isBlesStack, send, 90000));

                    if (isStack) { // 죽음의 축복 실현
                        Skill s = SkillFactory.getSkill(63111013);
                        final MapleStatEffect effectdeath = s.getEffect(player.getSkillLevel(63111013));
                        effectdeath.applyTo(player);
                        player.getMap().broadcastMessage(CField.KainDeathBlessAttack(63111012, deathAttack));
                    }

                }

                if (player.getSkillLevel(63101001) > 0) {
                    for (int i = 0; i < attack.targets; i++) {
                        player.giveMaliceGauge(attack.skill);
                    }
                }

            }
        }
        /* 카인파티버프는 스니핑..
        if(player.getBuffedValue(MapleBuffStat.ZeroAuraSpd,63121044) != null){
            List<Triple<MapleMonster,Integer,Integer>> send = new ArrayList<>();
            List<MapleMonster> deathAttack = new ArrayList<>();

            for(MapleMonster m : attack.mobs){
                if(m.isAlive()){ //몹이 살아있을 경우
                    deathAttack.add(m);
                }
            }

            if(deathAttack.size() > 0){
                player.getMap().broadcastMessage(CField.KainDeathBlessAttack(63111011,deathAttack));
            }
        }*/

        if (player.getBuffedValue(MapleBuffStat.GlimmeringTime) != null) {
            int level = player.getSkillLevel(11121005);
            final int skilla = attack.skill;
            int delay = 100;
            //일단 4차만
            switch (attack.skill) {
                case 11121101:
                    delay = 360;
                    break;
                case 11121102:
                    delay = 180;
                    break;
                case 11121103:
                case 11121201:
                case 11121202:
                case 11121203:
                    delay = 240;
                    break;
                case 11121052:
                case 11121055:
                    delay = 990;
                    break;
            }
            BuffTimer.getInstance().schedule(() -> {
                if (skilla != 11121052 && skilla != 11121055 && skilla / 10000 <= 1112 && level > 0 && player != null) {
                    if (player.lastPoseType == 11121011) {
                        SkillFactory.getSkill(11101022).getEffect(level).applyTo(player, false);
                        SkillFactory.getSkill(11121012).getEffect(level).applyTo(player, false);
                    } else if (player.lastPoseType == 11121012) {
                        SkillFactory.getSkill(11111022).getEffect(level).applyTo(player, false);
                        SkillFactory.getSkill(11121011).getEffect(level).applyTo(player, false);
                    }
                }
            }, delay);
        }
    }

    public static final void applyAttackMagic(final AttackInfo attack, final Skill theSkill, final MapleCharacter player, final MapleStatEffect effect, double maxDamagePerHit) {
        if (player.getMapId() == ServerConstants.WarpMap && !player.isGM()) {
            if (attack.skill % 10000 != 1092 && attack.skill % 10000 != 1094 && attack.skill % 10000 != 1095) {
                player.changeMap(ServerConstants.WarpMap, 0);
                //player.dropMessage(1, "스킬 쓰지마세요.");
                return;
            }
        }
        if (attack.skill != 0) {
            if (effect == null) {
                player.getClient().getSession().writeAndFlush(CWvsContext.enableActions(player));
                return;
            }
            if (GameConstants.isMulungSkill(attack.skill)) {
                if (player.getMapId() / 10000 != 92502) {
                    return;
                } else {
                    if (player.getMulungEnergy() < 10000) {
                        return;
                    }
                }
            } else if (GameConstants.isPyramidSkill(attack.skill)) {
                if (player.getMapId() / 1000000 != 926) {
                    return;
                }
            } else if (GameConstants.isInflationSkill(attack.skill)) {
                if (player.getBuffedValue(MapleBuffStat.Inflation) == null) {
                    return;
                }
            } else if (!GameConstants.isNoApplySkill(attack.skill)) { // Must be done here, since NPE with normal atk
                MapleStatEffect oldEffect = SkillFactory.getSkill(attack.skill).getEffect(attack.skilllevel);
                int target = oldEffect.getMobCount();

                for (Skill skill : player.getSkills().keySet()) {
                    int bonusSkillLevel = player.getSkillLevel(skill);
                    if (bonusSkillLevel > 0 && skill.getId() != attack.skill) {
                        MapleStatEffect bonusEffect = skill.getEffect(bonusSkillLevel);

                        target += bonusEffect.getTargetPlus();
                        target += bonusEffect.getTargetPlus_5th();
                    }
                }

                if (oldEffect.getMobCount() > 0 && player.getSkillLevel(70000047) > 0) {
                    target += SkillFactory.getSkill(70000047).getEffect(player.getSkillLevel(70000047)).getTargetPlus();
                }

                boolean useBulletCount = oldEffect.getBulletCount() > 1;
                int attackCount = useBulletCount ? oldEffect.getBulletCount() : oldEffect.getAttackCount();

                int bulletBonus = GameConstants.bullet_count_bonus(attack.skill);
                int attackBonus = GameConstants.attack_count_bonus(attack.skill);
                if (bulletBonus != 0 && useBulletCount) {
                    if (player.getSkillLevel(bulletBonus) > 0) {
                        attackCount += SkillFactory.getSkill(bulletBonus).getEffect(player.getSkillLevel(bulletBonus)).getBulletCount();
                    }
                } else if (attackBonus != 0 && !useBulletCount) {
                    if (player.getSkillLevel(attackBonus) > 0) {
                        attackCount += SkillFactory.getSkill(attackBonus).getEffect(player.getSkillLevel(attackBonus)).getAttackCount();
                    }
                }

                Integer plusCount = player.getBuffedValue(MapleBuffStat.Buckshot);
                if (plusCount != null) {
                    attackCount *= plusCount;
                }

                if (player.getBuffedEffect(MapleBuffStat.ShadowPartner) != null || player.getBuffedEffect(MapleBuffStat.Larkness) != null) {
                    attackCount *= 2;
                }

                if (player.getSkillLevel(3220015) > 0 && attackCount >= 2) {
                    attackCount += SkillFactory.getSkill(3220015).getEffect(player.getSkillLevel(3220015)).getX();
                }

                if (player.getBuffedEffect(MapleBuffStat.VengeanceOfAngel) != null && attack.skill == 2321007) {
                    attackCount += player.getBuffedEffect(MapleBuffStat.VengeanceOfAngel).getY();
                }

                Integer attackCountX = player.getBuffedValue(MapleBuffStat.AttackCountX);
                int[] blowSkills = {32001000, 32101000, 32111002, 32121002, 400021007};
                if (attackCountX != null) {
                    for (int blowSkill : blowSkills) {
                        if (attack.skill == blowSkill) {
                            attackCount += attackCountX;
                        }
                    }
                }

                if (attack.targets > target) {
                    player.dropMessageGM(-5, attack.skill + " 몹 개체수 > 클라이언트 계산 : " + attack.targets + " / 서버 계산 : " + target);
                    player.dropMessageGM(-6, "개체수가 계산값보다 많습니다.");
//            		return;
                }

                if (attack.hits > attackCount) {
                    player.dropMessageGM(-5, attack.skill + " 공격 횟수 > 클라이언트 계산 : " + attack.hits + " / 서버 계산 : " + attackCount);
                    player.dropMessageGM(-6, "공격 횟수가 계산값보다 많습니다.");
//            		return;
                }
            }
        }

        final PlayerStats stats = player.getStat();
        final Element element = player.getBuffedValue(MapleBuffStat.ElementalReset) != null ? Element.NEUTRAL : theSkill.getElement();

        double MaxDamagePerHit = 0;
        long totDamageToOneMonster, totDamage = 0, fixeddmg;
        byte overallAttackCount;
        boolean Tempest, heiz = false;
        byte multikill = 0;
        MapleMonsterStats monsterstats;
        int CriticalDamage = stats.critical_rate;

        final MapleMap map = player.getMap();

        /*        if (player.getSummonCount(400051011) > 0) {
         if (attack.skill != 65111007 && attack.skill != 65120011) {
         MapleSummon summon = (MapleSummon) player.getMap().getSummonObjects(player, 400051011).get(0);

         player.send(MainPacketCreator.angelicBusterEnergyBurst(player, summon.getPosition()));
         }
         }*/
        for (final AttackPair oned : attack.allDamage) {
            final MapleMonster monster = map.getMonsterByOid(oned.objectid);
            if (monster != null && monster.getLinkCID() <= 0) {
                Tempest = false;
                totDamageToOneMonster = 0;
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                if (!Tempest && !player.isGM()) {
                    if (!monster.isBuffed(MonsterStatus.MS_PowerImmune) && !monster.isBuffed(MonsterStatus.MS_MImmune)) {
                        MaxDamagePerHit = CalculateMaxMagicDamagePerHit(player, theSkill, monster, monsterstats, stats, element, CriticalDamage, maxDamagePerHit, effect);
                    } else {
                        MaxDamagePerHit = 1;
                    }
                }
                overallAttackCount = 0;
                long eachd;

                if (monster.getId() >= 9833070 && monster.getId() <= 9833074) {
                    continue;
                }

                for (Pair<Long, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    overallAttackCount++;
                    if (fixeddmg != -1) {
                        eachd = (long) (monsterstats.getOnlyNoramlAttack() ? 0 : fixeddmg); // Magic is always not a normal attack
                    } else if (monsterstats.getOnlyNoramlAttack()) {
                        eachd = 0; // Magic is always not a normal attack
                    }
                    totDamageToOneMonster += eachd;
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);
                if (attack.skill != 0 && !SkillFactory.getSkill(attack.skill).isChainAttack() && !effect.isMist() && effect.getSourceId() != 400021030 && !GameConstants.isLinkedSkill(attack.skill) && !GameConstants.isNoApplySkill(attack.skill) && !GameConstants.isNoDelaySkill(attack.skill) && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                    player.dropMessageGM(-5, "타겟이 범위를 벗어났습니다.");
//                	continue;
                }
                if (attack.skill == 400021002) {
                    MapleStatEffect iceAge = SkillFactory.getSkill(400020002).getEffect(player.getSkillLevel(400021002));
//                    int y = monster.getTruePosition().y + 25;
                    //                  int xMin = monster.getTruePosition().x - 250;
                    //                int xMax = xMin + 500;
                    //              int max = Randomizer.rand(3, 5);
                    //            for (int i = 0; i < max; i++) {
                    iceAge.applyTo(player, false, new Point(monster.getTruePosition().x, monster.getTruePosition().y - 25));
                    //          }
                }

                if (player.getBuffedValue(27121005)) {
                    if (player.getBuffedEffect(MapleBuffStat.StackBuff).makeChanceResult()) {
                        if (player.stackbuff < player.getBuffedEffect(MapleBuffStat.StackBuff).getX()) {
                            player.stackbuff++;
                            player.getBuffedEffect(MapleBuffStat.StackBuff).applyTo(player, false);
                        }
                    }
                }

                if (player.getSkillLevel(80002762) > 0) {
                    if (player.getBuffedEffect(MapleBuffStat.EmpiricalKnowledge) != null && player.empiricalKnowledge != null) {
                        if (map.getMonsterByOid(player.empiricalKnowledge.getObjectId()) != null) {
                            if (monster.getObjectId() != player.empiricalKnowledge.getObjectId() && monster.getMobMaxHp() > player.empiricalKnowledge.getMobMaxHp()) {
                                player.empiricalStack = 0;
                                player.empiricalKnowledge = monster;
                            }
                        } else {
                            player.empiricalStack = 0;
                            player.empiricalKnowledge = monster;
                        }
                    } else {
                        if (player.empiricalKnowledge != null) {
                            if (monster.getMobMaxHp() > player.empiricalKnowledge.getMobMaxHp()) {
                                player.empiricalKnowledge = monster;
                            }
                        } else {
                            player.empiricalKnowledge = monster;
                        }
                    }
                }

                if (totDamageToOneMonster > 0) {

                    monster.damage(player, totDamageToOneMonster, true, attack.skill);

                    if (monster.getId() >= 9500650 && monster.getId() <= 9500654 && totDamageToOneMonster > 0 && player.getGuild() != null) {
                        player.getGuild().updateGuildScore(totDamageToOneMonster);
                    }

                    if (player.getBuffedValue(400021073)) {
                        List<ForceAtom> atoms = new ArrayList<>();
                        MapleSummon s = null;
                        for (MapleSummon summon : player.getSummons()) {
                            if (summon.getSkill() == 400021073) {
                                s = summon;
                            }
                        }
                        if (s == null) {
                            player.dropMessage(6, "Zodiac Ray Null Point");
                        } else {
                            MapleAtom atom = new MapleAtom(true, monster.getObjectId(), 29, true, 400021073, monster.getTruePosition().x, monster.getTruePosition().y);
                            atom.setDwUserOwner(player.getId());

                            atom.setDwFirstTargetId(0);
                            atom.addForceAtom(new ForceAtom(1, 0x25, Randomizer.rand(0x5, 0xA), 0x3E, (short) 0));
                            player.getMap().spawnMapleAtom(atom);

                            player.setEnergyBurst(player.getEnergyBurst() + 1);
                            player.getClient().getSession().writeAndFlush(SummonPacket.updateSummon(s, 13));

                            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                            statups.put(MapleBuffStat.IndieSummon, new Pair<>(player.getEnergyBurst() + 21, (int) player.getBuffLimit(400021073)));
                            player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(400021073), player));
                        }
                    }

                    if (GameConstants.isFusionSkill(attack.skill)) {
                        MapleStatEffect magicWreck;
                        if (player.getSkillLevel(22170070) > 0) {
                            magicWreck = SkillFactory.getSkill(22170070).getEffect(player.getSkillLevel(22170070));
                        } else {
                            magicWreck = SkillFactory.getSkill(22141017).getEffect(player.getSkillLevel(22141017));
                        }
                        if (System.currentTimeMillis() - player.lastEvanMagicWtime >= magicWreck.getU()) {
                            List<MapleMagicWreck> mw = new ArrayList<>();
                            for (MapleMagicWreck mw2 : player.getMap().getWrecks()) {
                                if (mw2.getChr().getId() == player.getId()) {
                                    mw.add(mw2);
                                }
                            }
                            if (mw.size() < magicWreck.getMobCount()) {
                                MapleMagicWreck mw1 = new MapleMagicWreck(player, magicWreck.getSourceId(), monster.getTruePosition(), magicWreck.getDuration() / 1000);
                                player.lastEvanMagicWtime = System.currentTimeMillis();
                                player.getMap().spawnMagicWreck(mw1);

                            }
                        }
                    }

                    if (player.getSkillLevel(32101009) > 0 && !monster.isAlive() && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                        player.addHP(player.getStat().getCurrentMaxHp() * SkillFactory.getSkill(32101009).getEffect(player.getSkillLevel(32101009)).getKillRecoveryR() / 100);
                    }

                    if (player.getSkillLevel(142110011) > 0) {
                        if (SkillFactory.getSkill(142110011).getEffect(player.getSkillLevel(142110011)).makeChanceResult()) {
                            switch (attack.skill) { // 텔레키네시스
                                case 142001002:
                                case 142101003:
                                case 142111007:
                                case 142120002:
                                case 142121005:
                                case 142110000:
                                case 142110001:
                                case 142100000:
                                case 142100001:
                                case 142110011:
                                case 142001000:
                                case 142101009:
                                case 142120030:
                                case 142121030:
                                    break;
                                default: {
                                    final List<MapleMapObject> objs = player.getMap().getMapObjectsInRange(player.getTruePosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER));
                                    final List<Integer> monsters = new ArrayList<>();
                                    for (int i = 0; i < 1; i++) {
                                        int rand;
                                        if (objs.size() <= 1) {
                                            rand = 1;
                                        } else {
                                            rand = Randomizer.nextInt(objs.size());
                                        }
                                        if (objs.size() < 1) {
                                            if (i < objs.size()) {
                                                monsters.add(objs.get(i).getObjectId());
                                            }
                                        } else if (objs.size() > 1) {
                                            monsters.add(objs.get(rand).getObjectId());
                                            objs.remove(rand);
                                        }
                                    }
                                    if (monsters.size() > 0) {
                                        List<ForceAtom> atoms = new ArrayList<>();

                                        MapleAtom atom = new MapleAtom(false, player.getId(), 22, true, 142110011, player.getTruePosition().x, player.getTruePosition().y);

                                        for (Integer m : monsters) {
                                            atom.addForceAtom(new ForceAtom(0, 0x15, 9, 0x44, (short) 960));
                                        }
                                        atom.setDwFirstTargetId(0);
                                        player.getMap().spawnMapleAtom(atom);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    //////////////////////////////////////
                    if (monster.isBuffed(MonsterStatus.MS_MCounter) && player.getBuffedEffect(MapleBuffStat.IgnorePImmune) == null && player.getBuffedEffect(MapleBuffStat.IgnorePCounter) == null && player.getBuffedEffect(MapleBuffStat.IgnoreAllCounter) == null && player.getBuffedEffect(MapleBuffStat.IgnoreAllImmune) == null && !GameConstants.isNoReflectDamageSkill(SkillFactory.getSkill(attack.skill))) { //마반
                        player.addHP(-monster.getBuff(MonsterStatus.MS_MCounter).getValue());
                    }

                    switch (attack.skill) {
                        //불독 이그나이트
                        case 2101004:
                        case 2111002:
                        case 2121006:
                        case 2121005:
                        case 2121007: {
                            if (player.getBuffedEffect(MapleBuffStat.WizardIgnite) != null) {
                                if (SkillFactory.getSkill(2100010).getEffect(player.getSkillLevel(2101010)).makeChanceResult()) {
                                    SkillFactory.getSkill(2100010).getEffect(player.getSkillLevel(2101010)).applyTo(player, monster.getTruePosition());
                                }
                            }
                            break;
                        }
                    }

                    if (effect != null && monster.isAlive()) {
                        //재코딩 - 예인
                        List<Triple<MonsterStatus, MonsterStatusEffect, Long>> statusz = new ArrayList<>();
                        Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
                        switch (attack.skill) {
                            //불독 이그나이트
                            case 2101004:
                            case 2111002:
                            case 2121006:
                            case 2121005:
                            case 2121007: {
                                if (attack.skill == 2121006) {
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 700000)));
                                }
                                break;
                            }
                            //플레임 헤이즈:미스트 설치
                            case 2121011: {
                                player.setDotDamage((long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000));

                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Showdown, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getX()));
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) player.getDotDamage()));

                                player.setFlameHeiz(monster.getTruePosition());
                                heiz = true;
                                break;
                            }
                            case 2101005: { // 포이즌 브레스
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 100000)));
                                break;
                            }
                            case 2111003: { // 포이즌 미스트
                                if (!monster.isBuffed(2121011)) {
                                    MapleStatEffect bonusTime = null, bonusDam = null;
                                    if (player.getSkillLevel(2120044) > 0) {
                                        bonusTime = SkillFactory.getSkill(2120044).getEffect(player.getSkillLevel(2120044));
                                    }
                                    if (player.getSkillLevel(2120045) > 0) {
                                        bonusDam = SkillFactory.getSkill(2120045).getEffect(player.getSkillLevel(2120045));
                                    }
                                    player.setDotDamage((long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 100000));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime() + (bonusTime != null ? bonusTime.getDOTTime() : 0)), (long) player.getDotDamage()));
                                }
                                break;
                            }
                            case 2121055: { // 메기도 플레임
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                break;
                            }
                            //썬콜 빙결 중첩
                            case 2201004:
                            case 2201008:
                            case 2201009:
                            case 2211002:
                            case 2211006:
                            case 2211010:
                            case 2221003:
                            case 2220014:
                            case 2221011:
                            case 2221012:
                            case 2221054: {
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getV()));

                                if (monster.getFreezingOverlap() < 5) {
                                    monster.setFreezingOverlap((byte) (monster.getFreezingOverlap() + 1));
                                }

                                if (attack.skill == 2221011 && !monster.isBuffed(2221011)) {
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, 13000), (long) effect.getDuration()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(attack.skill, 13000), (long) effect.getY()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, 13000), (long) effect.getX()));
                                }
                                break;
                            }
                            case 2311004: { // 샤이닝 레이
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                break;
                            }
                            case 2321001: { // 빅뱅
                                if (monster.getBigbangCount() < effect.getY()) {
                                    monster.setBigbangCount((byte) (monster.getBigbangCount() + 1));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Mdr, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) monster.getBigbangCount() * effect.getX()));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Pdr, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) monster.getBigbangCount() * effect.getX()));
                                }
                                break;
                            }
                            case 2201005:
                            case 2211003:
                            case 2211011:
                            case 2221006: {

                                if (attack.skill == 2221006) {
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                }

                                if (monster.getFreezingOverlap() > 0) {
                                    monster.setFreezingOverlap((byte) (monster.getFreezingOverlap() - 1));
                                    statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Speed, new MonsterStatusEffect(attack.skill, 8000), (long) (-15 * monster.getFreezingOverlap())));
                                }

                                break;
                            }

                            case 27101101: { // 인바리어빌리티
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                break;
                            }

                            case 27121052: { // 아마겟돈
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getDuration()));
                                break;
                            }

                            case 32101001: { // 다크 체인
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                break;
                            }

                            case 32111016: { // 다크 라이트닝
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_DarkLightning, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                break;
                            }

                            case 32121004:
                            case 32121011: { // 다크 제네시스
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                break;
                            }

                            case 142001000:
                            case 142100000:
                            case 142100001:
                            case 142110000:
                            case 142110001: { // 싸이킥 포스
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) 1));
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_PsychicForce, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getV()));
                                break;
                            }

                            case 142111006: // 싸이킥 그라운드
                            case 142120003: { // 싸이킥 그라운드2
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_PsychicGroundMark, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) effect.getS()));
                                break;
                            }

                            case 142121031: { // 바인드
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Freeze, new MonsterStatusEffect(attack.skill, attack.skill == 400011015 ? effect.getW() * 1000 : effect.getSubTime() > 0 ? effect.getSubTime() : effect.getDuration()), (long) effect.getDuration()));
                                break;
                            }
                            case 400021028: { // 포이즌 노바
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Burned, new MonsterStatusEffect(attack.skill, effect.getDOTTime()), (long) (effect.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 10000)));
                                break;
                            }
                            case 162121041: {
                                statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Long>(MonsterStatus.MS_Stun, new MonsterStatusEffect(attack.skill, effect.getDuration()), (long) 1));
                                break;
                            }
                            default: //패시브 스킬 효과
                                if (attack.skill != 12100029 && player.getBuffedValue(12101024) && !monster.isBuffed(MonsterStatus.MS_Ember)) { // 플위 : 이그니션
                                    MapleStatEffect ignition = player.getBuffedEffect(12101024);
                                    if (ignition.makeChanceResult()) {
                                        player.gainIgnition();
                                        applys.put(MonsterStatus.MS_UnkFlameWizard, new MonsterStatusEffect(12101024, ignition.getDOTTime(), -ignition.getDOTTime() / 1000));
                                        applys.put(MonsterStatus.MS_Ember, new MonsterStatusEffect(12101024, ignition.getDOTTime(), 1));
                                        applys.put(MonsterStatus.MS_Burned, new MonsterStatusEffect(12101024, ignition.getDOTTime(), (long) (ignition.getDOT() * totDamageToOneMonster / attack.allDamage.size() / 1000)));
                                    }
                                    break;
                                }
                                break;
                        }

                        for (Triple<MonsterStatus, MonsterStatusEffect, Long> status : statusz) {
                            if (status.left != null && status.mid != null) {
                                if (effect.makeChanceResult()) {
                                    if (status.left == MonsterStatus.MS_Burned && status.right < 0) {
                                        status.right = status.right & 0xFFFFFFFFL;
                                    }
                                    status.mid.setValue(status.right);
                                    applys.put(status.left, status.mid);
                                }
                            }
                        }

                        if (monster != null && monster.isAlive()) {
                            if (!monster.isBuffed(effect.getSourceId())) {
                                monster.applyStatus(player.getClient(), applys, effect);
                            } else {
                                player.dropMessageGM(6, "몬스터디버프[2] 스킬" + effect.getSourceId() + " 은 이미 적용중이라 넘어감.");
                            }
                        }

                        if (monster.getStati().containsKey(MonsterStatus.MS_ElementResetBySummon)) {
                            monster.cancelStatus(MonsterStatus.MS_ElementResetBySummon);
                        }
                    }

                    //////////////////////////////
                    if (attack.skill == 2121003 && monster.getBurnedBuffSize() >= 5) {
                        player.changeCooldown(2121003, -2000);
                        if (player.getCooldownLimit(2121011) > 0) {
                            player.removeCooldown(2121011);
                        }
                    }

                    if (!monster.isAlive()) {
                        multikill++;
                    }
                    if (multikill >= 3) { //TODO :: 10移댁슫???좎떆 10源뚯? 利앷??⑦궥 ?섏젙
                        long comboexp = monster.getStats().getExp() / 3;
                        player.getClient().getSession().writeAndFlush(InfoPacket.multiKill(multikill, comboexp));
                        player.gainExp(comboexp, false, false, false);
                        if (player.getQuestStatus(100425) == 1) {
                            player.questmobkillcount++;
                            player.questmobkillcount = Math.min(player.questmobkillcount, 100);
                            player.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(100425, "MultiKC=" + player.questmobkillcount + ";"));
                            if (player.questmobkillcount == 100) {
                                player.getClient().setKeyValue("state", "2");
                            }
                        }
                    }
                }
            }
        }

        if (player.getBuffedValue(MapleBuffStat.OverloadMana) != null) {
            if (GameConstants.isKinesis(player.getJob())) {
                player.addHP((int) -(player.getStat().getCurrentMaxHp() * player.getBuffedEffect(MapleBuffStat.OverloadMana).getY() / 100));
            } else {
                player.addMP((int) -(player.getStat().getCurrentMaxMp(player) * player.getBuffedEffect(MapleBuffStat.OverloadMana).getX() / 100));
            }
        }

        if (player.getSkillLevel(2120010) > 0) {
            MapleStatEffect arcaneAim = SkillFactory.getSkill(2120010).getEffect(player.getSkillLevel(2120010));
            if (arcaneAim.makeChanceResult()) {
                if (player.getArcaneAim() < 5) {
                    player.setArcaneAim(player.getArcaneAim() + 1);
                }
                arcaneAim.applyTo(player, false);
            }
        }
        if (player.getSkillLevel(2220010) > 0) {
            MapleStatEffect arcaneAim = SkillFactory.getSkill(2220010).getEffect(player.getSkillLevel(2220010));
            if (arcaneAim.makeChanceResult()) {
                if (player.getArcaneAim() < 5) {
                    player.setArcaneAim(player.getArcaneAim() + 1);
                }
                arcaneAim.applyTo(player, false);
            }
        }
        if (player.getSkillLevel(2320011) > 0) {
            MapleStatEffect arcaneAim = SkillFactory.getSkill(2320011).getEffect(player.getSkillLevel(2320011));
            if (arcaneAim.makeChanceResult()) {
                if (player.getArcaneAim() < 5) {
                    player.setArcaneAim(player.getArcaneAim() + 1);
                }
                arcaneAim.applyTo(player, false);
            }
        }



        //파이널 어택
        if (totDamage > 0) {

            if (player.getMapId() == 993000500) {
                player.setFWolfDamage(player.getFWolfDamage() + totDamage);
                player.setFWolfAttackCount(player.getFWolfAttackCount() + 1);
            }

            int[] finalAttacks = {22000015, 22110021, 22150004};
            List<MapleMonster> finalattckmobs = new ArrayList<>();

            Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            for (int finalAttack : finalAttacks) {
                if (player.getSkillLevel(GameConstants.getLinkedSkill(finalAttack)) > 0 && weapon != null) {
                    MapleStatEffect fa = SkillFactory.getSkill(finalAttack).getEffect(player.getSkillLevel(GameConstants.getLinkedSkill(finalAttack)));
                    for (MapleMonster m : attack.mobs) {
                        if (m == null) {
                            continue;
                        }
                        if (m.isAlive()) {
                            if (Randomizer.isSuccess(fa.getProp() + (player.getBuffedEffect(MapleBuffStat.FinalAttackProp) != null ? player.getBuffedValue(MapleBuffStat.FinalAttackProp) : 0))) {
                                finalattckmobs.add(m);
                            }
                        }
                    }
                    player.getClient().getSession().writeAndFlush(CField.finalAttackRequest(1, attack.skill, finalAttack, (weapon.getItemId() - 1000000) / 10000, finalattckmobs));
                }
            }

            int[] finalAttacks2 = {2120013, 2220014, 32121011};
            List<MapleMonster> finalattckmobs2 = new ArrayList<>();

            for (int finalAttack : finalAttacks2) {
                if (player.getCooldownLimit(GameConstants.getLinkedSkill(finalAttack)) > 0 && weapon != null) {
                    MapleStatEffect fa = SkillFactory.getSkill(finalAttack).getEffect(player.getSkillLevel(GameConstants.getLinkedSkill(finalAttack)));
                    for (MapleMonster m : attack.mobs) {
                        if (Randomizer.isSuccess(fa.getProp() + (player.getBuffedEffect(MapleBuffStat.FinalAttackProp) != null ? player.getBuffedValue(MapleBuffStat.FinalAttackProp) : 0))) {
                            finalattckmobs2.add(m);
                        }
                    }
                    player.getClient().getSession().writeAndFlush(CField.finalAttackRequest(1, attack.skill, finalAttack, (weapon.getItemId() - 1000000) / 10000, finalattckmobs2));
                }
            }
            /*
             if (GameConstants.isLuminous(player.getJob()) && !player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
             switch (attack.skill) {
             case 27001100:
             case 27101100:
             case 27101101:
             case 27111100:
             case 27111101:
             case 27121100:
             if (!player.getBuffedValue(20040217)) {
             SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
             }
             break;
             case 27001201:
             case 27101202:
             case 27111202:
             case 27120211:
             case 27121201:
             case 27121202:
             if (!player.getBuffedValue(20040216)) {
             SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
             }
             break;
             }
             }
             */
            if (GameConstants.isLuminous(player.getJob())) {
                if (!player.getBuffedValue(20040216) && !player.getBuffedValue(20040217) && !player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                    if (GameConstants.isLightSkills(attack.skill)) {
                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
                        player.setLuminusMorph(true);
                    } else if (GameConstants.isDarkSkills(attack.skill)) {
                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
                        player.setLuminusMorph(false);
                    }
                    player.getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(player.getLuminusMorphUse(), player.getLuminusMorph()));
                } else {
                    if (!player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                        if (player.getLuminusMorph()) { // 빛
                            if (GameConstants.isLightSkills(attack.skill)) {
                                if ((player.getLuminusMorphUse() - GameConstants.isLightSkillsGaugeCheck(attack.skill)) <= 0) {
                                    if (player.getSkillLevel(20040219) > 0) {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040219).getEffect(1).applyTo(player, false);
                                        player.setUseTruthDoor(false);
                                    } else {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        player.setLuminusMorph(false);
                                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
                                    }
                                } else {
                                    player.setLuminusMorphUse(player.getLuminusMorphUse() - GameConstants.isLightSkillsGaugeCheck(attack.skill));
                                }
                                if (!player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                                    if (player.getLuminusMorph()) {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
                                    }
                                }
                            }
                        } else { // 어둠
                            if (GameConstants.isDarkSkills(attack.skill)) {
                                if ((player.getLuminusMorphUse() + GameConstants.isDarkSkillsGaugeCheck(player, attack.skill)) >= 10000) {
                                    if (player.getSkillLevel(20040219) > 0) { // 스킬 배우는건 1개임
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040220).getEffect(1).applyTo(player, false);
                                        player.setUseTruthDoor(false);
                                    } else {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        player.setLuminusMorph(true);
                                        SkillFactory.getSkill(20040216).getEffect(1).applyTo(player, false);
                                    }
                                } else {
                                    player.setLuminusMorphUse(player.getLuminusMorphUse() + GameConstants.isDarkSkillsGaugeCheck(player, attack.skill));
                                }
                                if (!player.getBuffedValue(20040219) && !player.getBuffedValue(20040220)) {
                                    if (!player.getLuminusMorph()) {
                                        player.cancelEffectFromBuffStat(MapleBuffStat.Larkness);
                                        SkillFactory.getSkill(20040217).getEffect(1).applyTo(player, false);
                                    }
                                }
                            }
                        }
                        player.getClient().getSession().writeAndFlush(BuffPacket.LuminusMorph(player.getLuminusMorphUse(), player.getLuminusMorph()));
                    }
                }
            }
            if (attack.skill == 27121303 && player.getSkillLevel(400021071) > 0) {
                SkillFactory.getSkill(400021071).getEffect(player.getSkillLevel(400021071)).applyTo(player, false);
            }

            if (player.getBuffedValue(32101009) && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                player.addHP(totDamage * player.getBuffedEffect(32101009).getX() / 100);
                if (player.getParty() != null) {
                    for (MaplePartyCharacter pc : player.getParty().getMembers()) {
                        if (pc.getId() != player.getId() && pc.isOnline()) {
                            int ch = World.Find.findChannel(pc.getName());
                            if (ch > 0) {
                                MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(pc.getId());
                                if (chr != null && chr.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null) {
                                    chr.addHP(totDamage * player.getBuffedEffect(32101009).getX() / 100);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (player.getSkillLevel(51120057) > 0 && attack.skill == 51121009) {
            SkillFactory.getSkill(51120057).getEffect(1).applyTo(player, false);
        }

        if (totDamage > 0 && player.getBuffedValue(400021073)) {
            MapleSummon summon = player.getSummon(400021073);
            if (summon != null && summon.getEnergy() < 22) {
                switch (attack.skill) {
                    case 22170060:
                    case 22110022:
                    case 22110023:
                    case 22111012:
                    case 22170070:
                    case 400021012:
                    case 400021014:
                    case 400021015: {
                        MapleAtom atom = new MapleAtom(true, summon.getObjectId(), 29, true, 400021073, summon.getTruePosition().x, summon.getTruePosition().y);
                        atom.setDwUserOwner(summon.getOwner().getId());

                        atom.setDwFirstTargetId(0);
                        atom.addForceAtom(new ForceAtom(1, 0x25, Randomizer.rand(0x5, 0xA), 0x3E, (short) 0));
                        player.getMap().spawnMapleAtom(atom);

                        player.getClient().getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
                        player.getClient().getSession().writeAndFlush(SummonPacket.specialSummon(summon, 2));
                        if (summon.getEnergy() >= 22) {
                            player.getClient().getSession().writeAndFlush(SummonPacket.damageSummon(summon));
                        }
                        break;
                    }
                    case 22171063:
                    case 22111011:
                    case 22110014:
                    case 22110024:
                    case 22110025:
                    case 22140014:
                    case 22140015:
                    case 22140023:
                    case 22140024:
                    case 22141011:
                    case 22171095:
                    case 22171083:
                    case 22170064:
                    case 22170065:
                    case 22170066:
                    case 22170067:
                    case 22170093:
                    case 22170094:
                    case 400021013: {
                        if (!summon.getMagicSkills().contains(attack.skill)) {
                            summon.getMagicSkills().add(attack.skill);
                            summon.setEnergy(Math.min(22, summon.getEnergy() + 3));

                            MapleAtom atom = new MapleAtom(true, summon.getObjectId(), 29, true, 400021073, summon.getTruePosition().x, summon.getTruePosition().y);
                            atom.setDwUserOwner(summon.getOwner().getId());

                            atom.setDwFirstTargetId(0);
                            atom.addForceAtom(new ForceAtom(1, 0x25, Randomizer.rand(0x5, 0xA), 0x3E, (short) 0));
                            player.getMap().spawnMapleAtom(atom);

                            player.getClient().getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
                            player.getClient().getSession().writeAndFlush(SummonPacket.specialSummon(summon, 2));
                            if (summon.getEnergy() >= 22) {
                                player.getClient().getSession().writeAndFlush(SummonPacket.damageSummon(summon));
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (totDamage > 0 && player.getBuffedValue(MapleBuffStat.BMageDeath) != null) {
            if (System.currentTimeMillis() - player.lastDeathAttackTime >= player.getBuffedEffect(MapleBuffStat.BMageDeath).getDuration() && (player.getDeath() >= player.getBuffedEffect(MapleBuffStat.BMageDeath).getX() || player.getBuffedEffect(MapleBuffStat.AttackCountX) != null)) {
                player.setDeath((byte) 0);
                player.lastDeathAttackTime = System.currentTimeMillis();
                MapleSummon summon = player.getSummon(player.getBuffSource(MapleBuffStat.BMageDeath));
                if (summon != null) {
                    player.getClient().getSession().writeAndFlush(SummonPacket.DeathAttack(summon));
                }

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.BMageDeath, new Pair<>((int) player.getDeath(), 0));
                player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(MapleBuffStat.BMageDeath), player));
            }
        }

        if (attack.skill == 2121003) {
            for (MapleMist mist : player.getMap().getAllMistsThreadsafe()) {
                if (mist.getSource() != null) {
                    if (mist.getSource().getSourceId() == 2111003) {
                        player.getMap().removeMistByOwner(player, mist.getSource().getSourceId());
                        if (player.getCooldownLimit(2121011) > 0) {
                            player.removeCooldown(2121011);
                        }
                    }
                }
            }
        }

        if (totDamage > 0 && attack.skill >= 400021013 && attack.skill <= 400021016) {
            SkillFactory.getSkill(400021012).getEffect(attack.skilllevel).applyTo(player, false);
        }

        if (player.getSkillLevel(80002762) > 0) {
            MapleStatEffect stst = SkillFactory.getSkill(80002762).getEffect(player.getSkillLevel(80002762));

            if (stst.makeChanceResult()) {
                stst.applyTo(player, false);
            }
        }

        //제네시스 쓰면 빅뱅 노쿨
        if (attack.skill == 2321008) {
//            SkillFactory.getSkill(2321001).getEffect(player.getSkillLevel(2321008)).applyTo(player, false, SkillFactory.getSkill(2321008).getEffect(player.getSkillLevel(2321008)).getCooldown(player));
        }

        if (attack.skill == 152121007 && player.getBuffedEffect(152111003) != null) {
            player.canUseMortalWingBeat = false;
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.GloryWing, new Pair<>(1, (int) player.getBuffLimit(152111003)));
            player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, player.getBuffedEffect(152111003), player));
        }

        if (attack.skill == 400021012) {
            List<Integer> skills = new ArrayList<>();
            skills.add(400021013);
            skills.add(400021014);
            skills.add(400021015);
            player.getClient().getSession().writeAndFlush(CField.rangeAttack(attack.skill, skills, 0, player.getPosition(), false));
        }

        if (GameConstants.isKinesis(player.getJob())) {
            player.givePPoint(attack.skill, false);
        }

        if (attack.skill == 152001002 || attack.skill == 152120003) {
            if (player.getSkillLevel(152120012) > 0) {
                player.blessMarkSkill = 152120012;
            } else if (player.getSkillLevel(152110009) > 0) {
                player.blessMarkSkill = 152110009;
            } else if (player.getSkillLevel(152000007) > 0) {
                player.blessMarkSkill = 152000007;
            }

            if (player.blessMarkSkill != 0) {
                SkillFactory.getSkill(152000009).getEffect(player.getSkillLevel(152000009)).applyTo(player, false);
            }
        }

        if (attack.skill == 162001000 || attack.skill == 162121021) {
            if (player.getSkillLevel(162000003) > 0) {
                MapleStatEffect eff = SkillFactory.getSkill(162000003).getEffect(player.getSkillLevel(162000003));
                if (Randomizer.isSuccess(eff.getProp()))
                    player.getClient().getSession().writeAndFlush(CField.rangeAttack(162001000, Arrays.asList(162001004), 1, attack.position, player.isFacingLeft()));
            }
            if (player.getBuffedValue(MapleBuffStat.AbsorptionRiver) != null) {
                if (player.getCooldownLimit(162121004) == 0) {
                    player.getClient().getSession().writeAndFlush(CField.rangeAttack(162121004, Arrays.asList(162121004), 1, attack.position, player.isFacingLeft()));
                    player.getClient().getSession().writeAndFlush(CField.skillCooldown(162121004, 2500));
                    player.addCooldown(162121004, System.currentTimeMillis(), 2500);
                }
            }
            if (player.getBuffedValue(MapleBuffStat.AbsorptionWind) != null) {
                if (player.getCooldownLimit(162121007) == 0) {
                    player.getClient().getSession().writeAndFlush(CField.rangeAttack(162121007, Arrays.asList(162121007), 1, attack.position, player.isFacingLeft()));
                    player.getClient().getSession().writeAndFlush(CField.skillCooldown(162121007, 2500));
                    player.addCooldown(162121007, System.currentTimeMillis(), 2500);
                }
            }
            if (player.getBuffedValue(MapleBuffStat.AbsorptionSun) != null) {
                if (player.getCooldownLimit(162121010) == 0) {
                    player.getClient().getSession().writeAndFlush(CField.rangeAttack(162121010, Arrays.asList(162121010), 1, attack.position, player.isFacingLeft()));
                    player.getClient().getSession().writeAndFlush(CField.skillCooldown(162121010, 2500));
                    player.addCooldown(162121010, System.currentTimeMillis(), 2500);
                }
            }
        }
    }

    private static final double CalculateMaxMagicDamagePerHit(final MapleCharacter chr, final Skill skill, final MapleMonster monster, final MapleMonsterStats mobstats, final PlayerStats stats, final Element elem, final Integer sharpEye, final double maxDamagePerMonster, final MapleStatEffect attackEffect) {
        final int dLevel = Math.max(mobstats.getLevel() - chr.getLevel(), 0) * 2;
        int HitRate = Math.min((int) Math.floor(Math.sqrt(stats.getAccuracy())) - (int) Math.floor(Math.sqrt(mobstats.getEva())) + 100, 100);
        if (dLevel > HitRate) {
            HitRate = dLevel;
        }
        HitRate -= dLevel;
        if (HitRate <= 0 && !(GameConstants.isBeginnerJob(skill.getId() / 10000) && skill.getId() % 10000 == 1000)) { // miss :P or HACK :O
            return 0;
        }
        double elemMaxDamagePerMob;
        int CritPercent = sharpEye;
        final ElementalEffectiveness ee = monster.getEffectiveness(elem);
        switch (ee) {
            case IMMUNE:
                elemMaxDamagePerMob = 1;
                break;
            default:
                elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * ee.getValue(), stats);
                break;
        }
        // Calculate monster magic def
        // Min damage = (MIN before defense) - MDEF*.6
        // Max damage = (MAX before defense) - MDEF*.5
        int MDRate = monster.getStats().getMDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.MS_Mdr);
        if (pdr != null) {
            MDRate += pdr.getValue();
        }
        elemMaxDamagePerMob -= elemMaxDamagePerMob * (Math.max(MDRate - stats.ignoreTargetDEF - attackEffect.getIgnoreMob(), 0) / 100.0);
        // Calculate Sharp eye bonus
        elemMaxDamagePerMob += ((double) elemMaxDamagePerMob / 100.0) * CritPercent;
//	if (skill.isChargeSkill()) {
//	    elemMaxDamagePerMob = (float) ((90 * ((System.currentTimeMillis() - chr.getKeyDownSkill_Time()) / 1000) + 10) * elemMaxDamagePerMob * 0.01);
//	}
//      if (skill.isChargeSkill() && chr.getKeyDownSkill_Time() == 0) {
//          return 1;
//      }
        elemMaxDamagePerMob *= (monster.getStats().isBoss() ? chr.getStat().bossdam_r : chr.getStat().dam_r) / 100.0;
//        final MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.IMPRINT);
        //      if (imprint != null) {
        //        elemMaxDamagePerMob += (elemMaxDamagePerMob * imprint.getX() / 100.0);
        //  }
        elemMaxDamagePerMob += (elemMaxDamagePerMob * chr.getDamageIncrease(monster.getObjectId()) / 100.0);
        if (GameConstants.isBeginnerJob(skill.getId() / 10000)) {
            switch (skill.getId() % 10000) {
                case 1000:
                    elemMaxDamagePerMob = 40;
                    break;
                case 1020:
                    elemMaxDamagePerMob = 1;
                    break;
                case 1009:
                    elemMaxDamagePerMob = (monster.getStats().isBoss() ? monster.getMobMaxHp() / 30 * 100 : monster.getMobMaxHp());
                    break;
            }
        }
        switch (skill.getId()) {
            case 32001000:
            case 32101000:
            case 32111002:
            case 32121002:
                elemMaxDamagePerMob *= 1.5;
                break;
        }
        if (elemMaxDamagePerMob > 999999) {
            elemMaxDamagePerMob = 999999;
        } else if (elemMaxDamagePerMob <= 0) {
            elemMaxDamagePerMob = 1;
        }

        return elemMaxDamagePerMob;
    }

    private static final double ElementalStaffAttackBonus(final Element elem, double elemMaxDamagePerMob, final PlayerStats stats) {
        switch (elem) {
            case FIRE:
                return (elemMaxDamagePerMob / 100) * (stats.element_fire + stats.getElementBoost(elem));
            case ICE:
                return (elemMaxDamagePerMob / 100) * (stats.element_ice + stats.getElementBoost(elem));
            case LIGHTING:
                return (elemMaxDamagePerMob / 100) * (stats.element_light + stats.getElementBoost(elem));
            case POISON:
                return (elemMaxDamagePerMob / 100) * (stats.element_psn + stats.getElementBoost(elem));
            default:
                return (elemMaxDamagePerMob / 100) * (stats.def + stats.getElementBoost(elem));
        }
    }

    private static void handlePickPocket(final MapleCharacter player, final MapleMonster mob, MapleStatEffect eff) {
        int rand = eff.getProp();
        int max = eff.getY();
        if (player.getSkillLevel(4220045) > 0) {
            rand += SkillFactory.getSkill(4220045).getEffect(player.getSkillLevel(4220045)).getProp();
            max += SkillFactory.getSkill(4220045).getEffect(player.getSkillLevel(4220045)).getBulletCount();
        }
        if (Randomizer.isSuccess(Math.min(100, rand)) && player.getPickPocket() < max) {
            player.getMap().spawnMesoDrop(1, mob.getTruePosition(), mob, player, true, (byte) 0);
            player.setPickPocket(Math.min(max, player.getPickPocket() + 1));
            player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(eff.getStatups(), eff, player));
        }
    }

    public static final AttackInfo DivideAttack(final AttackInfo attack, final int rate) {
        attack.real = false;
        if (rate <= 1) {
            return attack; //lol
        }
        for (AttackPair p : attack.allDamage) {
            if (p.attack != null) {
                for (Pair<Long, Boolean> eachd : p.attack) {
                    eachd.left /= rate; //too ex.
                }
            }
        }
        return attack;
    }

    public static final AttackInfo parseDmgMa(final LittleEndianAccessor lea, final MapleCharacter chr, boolean chilling, boolean orbital) {
        final AttackInfo ret = new AttackInfo();
        LittleEndianAccessor data = lea;

        if (orbital) {
            ret.skill = lea.readInt();
            lea.skip(4);
            lea.skip(4);
            lea.skip(4);
            lea.skip(4);
        }
        lea.skip(1);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.skip(4); // 324 ++
        ret.skill = lea.readInt();
        ret.skilllevel = lea.readInt(); // 324 ++
        try {
            if (orbital) {
                lea.skip(1);
            }

            lea.skip(4); //crc
            lea.skip(4); // 307++

            GameConstants.AttackBonusRecv(lea, ret);
            GameConstants.calcAttackPosition(lea, ret);

            switch (ret.skill) { // 소닉 블로우랑 똑같음 -_-;
                case 400021072:
                    lea.skip(4);
                    break;
            }

            if (GameConstants.sub_781900(ret.skill) && ret.skill != 2321001) {
                ret.charge = lea.readInt();
            }

            if (ret.skill == 400021086 || ret.skill == 22140024) {
                lea.skip(4);
            }

            if (orbital) {
                lea.skip(4);
            }

            ret.isShadowPartner = lea.readByte();
            ret.isBuckShot = lea.readByte();

            ret.display = lea.readShort();
            lea.skip(4); //big bang
            ret.attacktype = lea.readByte(); // Weapon class
            if (GameConstants.is_evan_force_skill(ret.skill)) {
                lea.readByte();
            }
            ret.speed = lea.readByte(); // Confirmed
            ret.lastAttackTickCount = lea.readInt(); // Ticks
            lea.readInt();

            // System.out.println(ret.skill+"      "+ret.attacktype+"      "+ret.speed+lea);
            int chillingoid = 0;
            if (chilling) {
                chillingoid = lea.readInt();
            }

            if (orbital) {
                lea.skip(4);
            }
            long damage;
            int oid;
            List<Pair<Long, Boolean>> allDamageNumbers;

            ret.allDamage = new ArrayList<AttackPair>();
            for (int i = 0; i < ret.targets; i++) {
                oid = lea.readInt();
                ret.mobs.add(chr.getMap().getMonsterByOid(oid));
                // [1] Always 7?, [1] unk, [1] unk, [1] unk, [1] unk, [4] mobid, [1] unk, [4] Pos1, [4] Pos2, [6] seems to change randomly for some attack
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                int a = lea.readInt(); // mobId
                // chr.dropMessageGM(6,ret.skill+"DamageMa"+oid+"  "+a+"  "+ ret.skill+"  "+lea);
                lea.readByte();

                lea.readShort();
                lea.readShort();
                lea.readShort();
                lea.readShort();
                if (!orbital) {
                    lea.skip(1);
                }
                lea.skip(2);//????? 2개해야됨.
                if (ret.skill == 80001835) {
                    int cc = lea.readByte();
                    for (int ii = 0; ii < cc; ii++) {
                        lea.readLong();
                    }
                } else {
                    lea.readByte();
                }
                lea.skip(4);
                lea.skip(4); // 307++

                allDamageNumbers = new ArrayList<Pair<Long, Boolean>>();
                String str2 = "";
                for (int j = 0; j < ret.hits; j++) {
                    damage = lea.readLong();
                    if (damage < 0) {
                        damage = damage & 0xFFFFFFFFL;
                    }
                    chr.dropMessageGM(6, "damage" + damage);
                    str2 += damage + " ";

                    allDamageNumbers.add(new Pair<Long, Boolean>(Long.valueOf(damage), false));
                }
                lea.skip(4);
                lea.skip(4);
                if (ret.skill == 37111005) {
                    lea.skip(1);
                }
                if (ret.skill == 142120001 || ret.skill == 142120002 || ret.skill == 142110003) {
                    lea.skip(8);
                }

                byte unk = lea.readByte();
                if (unk == 1) {
                    lea.readMapleAsciiString(); // skeleton
                    lea.readInt();
                    int b = lea.readInt();
                    if (b > 0) {
                        for (int k = 0; k < b; ++k) {
                            lea.readMapleAsciiString();
                        }
                    }
                } else if (unk == 2) {
                    lea.readMapleAsciiString(); // skeleton
                    lea.readInt();
                }

                lea.skip(1);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(1);
                lea.skip(4);
                //      lea.skip(1);
                ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
                if (chilling) {
                    ret.allDamage.add(new AttackPair(Integer.valueOf(chillingoid), allDamageNumbers));
                }
            }
            ret.position = lea.readPos();
            if (GameConstants.isEvan(ret.skill / 10000)) {
                lea.skip(10);
                ret.nMoveAction = lea.readByte();
                ret.bShowFixedDamage = lea.readByte();
            }
            if (lea.available() > 0 && !orbital) {
                lea.skip(1);
            }

        } catch (Exception e) {
//        	e.printStackTrace();
            FileoutputUtil.log(FileoutputUtil.Attack_Log, "error in MagicAttack.\r\n ordinary : " + HexTool.toString(data.getByteArray()) + "\r\n error : " + e);
        }
        return ret;
    }

    public static final AttackInfo parseDmgB(final LittleEndianAccessor lea, final MapleCharacter chr) {
        final AttackInfo ret = new AttackInfo();

        LittleEndianAccessor data = lea;

        lea.skip(1);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.readInt();
        ret.skill = lea.readInt();
        ret.skilllevel = lea.readInt();
        try {
            lea.skip(4);
            lea.skip(4);

            GameConstants.AttackBonusRecv(lea, ret);
            GameConstants.calcAttackPosition(lea, ret);

            if (GameConstants.isZeroSkill(ret.skill)) {
                ret.asist = lea.readByte();
            }

            ret.isShadowPartner = lea.readByte();
            ret.isBuckShot = lea.readByte();
            ret.display = lea.readShort();
            lea.readInt();
            ret.attacktype = lea.readByte();
            ret.speed = lea.readByte();
            ret.lastAttackTickCount = lea.readInt();
            ret.charge = lea.readInt();

            if (ret.skill == 4211006) {
                return parseMesoExplosion(lea, ret, chr);
            }

            ret.allDamage = new ArrayList<AttackPair>();
            long damage;
            int oid;
            List<Pair<Long, Boolean>> allDamageNumbers;

            for (int i = 0; i < ret.targets; i++) {
                oid = lea.readInt();
                ret.mobs.add(chr.getMap().getMonsterByOid(oid));
                // [1] Always 7?, [1] unk, [1] unk, [1] unk, [1] unk, [4] mobid, [1] unk, [4] Pos1, [4] Pos2, [6] seems to change randomly for some attack
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readInt(); // mobId
                lea.readByte();
                ret.mobpos.add(new Point(lea.readShort(), lea.readShort()));
                lea.readShort();
                lea.readShort();
                lea.readShort();
                lea.readInt();
                lea.readInt();
                lea.readByte();
                allDamageNumbers = new ArrayList<Pair<Long, Boolean>>();

                for (int j = 0; j < ret.hits; j++) {
                    damage = lea.readLong();
                    if (damage < 0) {
                        damage = damage & 0xFFFFFFFFL;
                    }
                    allDamageNumbers.add(new Pair<Long, Boolean>(Long.valueOf(damage), false));
                }
                lea.skip(4);
                lea.skip(4);
                if (ret.skill == 37111005) {
                    lea.skip(1);
                }
                byte unk = lea.readByte();
                if (unk == 1) {
                    lea.readMapleAsciiString(); // skeleton
                    lea.readInt();
                    int a = lea.readInt();
                    if (a > 0) {
                        for (int k = 0; k < a; ++k) {
                            lea.readMapleAsciiString();
                        }
                    }
                } else if (unk == 2) {
                    lea.readMapleAsciiString(); // skeleton
                    lea.readInt();
                }

                lea.skip(1);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(1);
                lea.skip(4);
                ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
            }
            if (ret.skill == 151121041) {
                lea.readByte();
                lea.readByte();
            }
            ret.position = lea.readPos();
            lea.readInt();
        } catch (Exception e) {
            FileoutputUtil.log(FileoutputUtil.Attack_Log, "error in BuffAttack.\r\n ordinary : " + HexTool.toString(data.getByteArray()) + "\r\n error : " + e);
        }
        return ret;
    }

    public static final AttackInfo parseDmgM(final LittleEndianAccessor lea, final MapleCharacter chr, boolean dot) {
        final AttackInfo ret = new AttackInfo();

        LittleEndianAccessor data = lea;

        lea.skip(1);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.skip(4); // 324 ++
        ret.skill = lea.readInt();
        ret.skilllevel = lea.readInt(); // 324 ++
        try {
            if (!dot) {
                ret.isLink = lea.readByte() == 1;
            }
            lea.skip(4);
            lea.skip(4);

            GameConstants.AttackBonusRecv(lea, ret);
            GameConstants.calcAttackPosition(lea, ret);

            if ((GameConstants.sub_8166C0(ret.skill) && ret.skill != 35121015) || GameConstants.is_screen_attack_skill(ret.skill) || ret.skill == 400010030 || ret.skill == 400031065 || ret.skill == 400031061 || ret.skill == 63121006) {
                lea.readInt();
            }

            if (GameConstants.sub_8845D0(ret.skill) || ret.skill == 5300006 || ret.skill == 27120211 || ret.skill == 400031003 || ret.skill == 400031004 || ret.skill == 14111023 || ret.skill == 64101008) {
                ret.charge = lea.readInt();
            }

            if (GameConstants.isZeroSkill(ret.skill)) {
                ret.asist = lea.readByte();
            }

            /* if (GameConstants.sub_57DCA0(ret.skill) && !GameConstants.isEvan(chr.getJob())  //이쯤되면 얘도 없애는게 맞는듯?
                    && !GameConstants.isKain(chr.getJob()) && !GameConstants.isFlameWizard(chr.getJob())
                    && !GameConstants.isPhantom(chr.getJob()) & !GameConstants.isKadena(chr.getJob())
            && !GameConstants.isWindBreaker(chr.getJob()) && !GameConstants.isStriker(chr.getJob())
                    && !GameConstants.isNightWalker(chr.getJob()) && !GameConstants.isAran(chr.getJob())
                    && !GameConstants.isDualBlade(chr.getJob()) && !GameConstants.isXenon(chr.getJob())
                    && !GameConstants.isCannon(chr.getJob()) && !GameConstants.isAngelicBuster(chr.getJob())
                    && !GameConstants.isNightLord(chr.getJob()) && !GameConstants.isShadower(chr.getJob())
                    && !GameConstants.isWildHunter(chr.getJob())){
                ret.charge = lea.readInt();
            }*/
            if (ret.skill == 400031010 || ret.skill == 80002823) {
                lea.skip(4);
                lea.skip(4);
            }

            if (ret.skill == 400041019) {
                lea.readInt();
                lea.readInt();
            }

            if (ret.skill == 400051041 || ret.skill == 40031046
                    || ret.skill == 3321040 || ret.skill == 3321036
                    || ret.skill == 400041039 || ret.skill == 400011110 || ret.skill == 400011111
                    || ret.skill == 400051334 || ret.skill == 155121341 || ret.skill == 400011068) {
                lea.readInt();
            }

            if (ret.skill == 11101220 || ret.skill == 11101221 || ret.skill == 11101120 || ret.skill == 11111120
                    || ret.skill == 11101121 || ret.skill == 11111121 || ret.skill == 11111220
                    || ret.skill == 11111221 || ret.skill == 11121103 || ret.skill == 11121102
                    || ret.skill == 11121203 || ret.skill == 11121201 || ret.skill == 11121101
                    || ret.skill == 14111022 || ret.skill == 14111023 || ret.skill == 400021131) {
                ret.charge = lea.readInt();
            }

            if (ret.skill == 80001762)
                lea.readInt();

            ret.isShadowPartner = lea.readByte();
            ret.isBuckShot = lea.readByte();
            ret.display = lea.readShort();

            lea.readInt();

            ret.attacktype = lea.readByte();
            ret.speed = lea.readByte();
            ret.lastAttackTickCount = lea.readInt();
            // System.out.println(ret.skill+"      "+ret.attacktype+"      "+ret.speed+lea);


            if (ret.skill == 23111002) {
                lea.skip(4);
            }

            if (ret.skill == 23111003) {
                lea.skip(4);
            }

            if (ret.skill == 400041059) {
                lea.skip(4);
                lea.skip(4);

            }
            if (ret.skill == 400041060) {
                lea.skip(4);
            }

            if (ret.skill == 23121011) {
                lea.skip(4);
            }

            if (ret.skill == 5111007) {
                lea.readByte();
            }

            if (ret.skill == 400051078) {
                lea.skip(4);
            }

            if (ret.skill == 5111009 || ret.skill == 80001913) {
                lea.skip(1);
            } else if (ret.skill == 25111005) {
                lea.skip(4);
            }

            ret.allDamage = new ArrayList<AttackPair>();

            int[] finalAttacks = {1100002, 1120013, 1200002, 1300002, 3100001, 3120008, 3200001, 4341054, 5120021, 5220020, 11101002, 13101002, 21100010, 21120012, 23100006, 23120012, 33100009, 33120011, 51100002, 51120002};
            for (int finalAttack : finalAttacks) {
                if (finalAttack == ret.skill) {
                    lea.skip(1);
                    break;
                }
            }
            if (ret.skill >= 400051018 && ret.skill <= 400051020 || ret.skill == 155121006) {
                lea.skip(4);//342++.
            } else {
                lea.skip(4);//342++.
                lea.skip(4);//342++.
            }

            if (ret.skill == 400031024) {
                lea.skip(8);
            }
            //  System.out.println(lea);
            int oid;
            long damage;
            List<Pair<Long, Boolean>> allDamageNumbers;
            for (int i = 0; i < ret.targets; i++) {
                oid = lea.readInt();
                ret.mobs.add(chr.getMap().getMonsterByOid(oid));
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                int mobid = lea.readInt();
                //   chr.dropMessageGM(6,ret.skill+"damageM debug"+mobid+"  "+oid+"   "+ret.speed+"     "+lea);
                lea.readByte();
                MapleMonster m = chr.getMap().getMonsterByOid(oid);
                if (m != null) {
                    if (m.getId() == 8880153) {
                        lea.skip(4);
                        ret.mobpos.add(new Point(727, -580));
                    } else {
                        ret.mobpos.add(new Point(lea.readShort(), lea.readShort()));
                    }
                } else {
                    ret.mobpos.add(new Point(lea.readShort(), lea.readShort()));
                }
                lea.readShort();
                lea.readShort();
                lea.readShort();
                lea.readInt();
                lea.readInt();
                lea.readByte();//++342

                allDamageNumbers = new ArrayList<Pair<Long, Boolean>>();
                for (int j = 0; j < ret.hits; j++) {
                    damage = lea.readLong();
                    if (damage < 0) {
                        damage = damage & 0xFFFFFFFFL;
                    }
                    allDamageNumbers.add(new Pair<Long, Boolean>(Long.valueOf(damage), false));
                }

                lea.readInt();
                lea.readInt();

                if (ret.skill == 37111005 || ret.skill == 400021029) {
                    lea.skip(1);
                }

                GameConstants.sub_2224400(lea, ret);

                //chr.dropMessage(5, oid + "ㅇㅇ");
                ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
            }

            if (GameConstants.is_screen_attack_skill(ret.skill) || ret.skill == 101000102 || ret.skill == 400031016 || ret.skill == 400041024 || ret.skill == 80002452 || GameConstants.sub_84ABA0(ret.skill)) {
                ret.position = lea.readPos();
            } else {
                ret.position = lea.readPos();
            }

            if (GameConstants.sub_896310(ret.skill)) {
                lea.readInt();
                ret.position = lea.readPos();
                lea.readByte();
            }

            if (ret.skill == 21121056) {
                ret.position = lea.readPos();
            }

            if (GameConstants.sub_893240(ret.skill) > 0 || GameConstants.sub_893F80(ret.skill)) {
                lea.skip(1);
            }

            if (ret.skill == 21120019 || ret.skill == 37121052 || GameConstants.is_shadow_assult(ret.skill) || ret.skill == 11121014 || ret.skill == 5101004) {
                ret.plusPos = lea.readByte();
                ret.plusPosition = new Point(lea.readInt(), lea.readInt());
            }
            if (dot) {
                lea.skip(4);
            }

            if (ret.skill == 61121105 || ret.skill == 61121222 || ret.skill == 24121052) {
                short count = lea.readShort();
                while (count > 0) {
                    ret.mistPoints.add(new Point(lea.readShort(), lea.readShort()));
                    count--;
                }
            } else if (ret.skill == 101120104) {
                if (lea.available() >= 2) {
                    short count = lea.readShort();
                    while (count > 0) {
                        ret.mistPoints.add(new Point(lea.readShort(), lea.readShort()));
                        count--;
                    }
                }
            }

            if (ret.skill == 14111006) {
                lea.skip(2);
                lea.skip(2);
            } else if (ret.skill == 80002686) {
                int count = lea.readInt();
                for (int i = 0; i < count; i++) {
                    lea.readInt();
                }
            }

            if (lea.available() > 0) {
                //     System.out.println(ret.skill + " has another byte : " + lea);
            }
        } catch (Exception e) {
            FileoutputUtil.log(FileoutputUtil.Attack_Log, "error in CloseRangeAttack.\r\n ordinary : " + HexTool.toString(data.getByteArray()) + "\r\n error : " + e);
        }
        return ret;
    }

    public static final AttackInfo parseDmgR(final LittleEndianAccessor lea, final MapleCharacter chr) {
        final AttackInfo ret = new AttackInfo();

        LittleEndianAccessor data = lea;

        lea.skip(1); // 0
        lea.skip(1); // portal count
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        lea.skip(4); // 324 ++
        ret.skill = lea.readInt();
        ret.skilllevel = lea.readInt(); // 324 ++
        try {
            ret.isLink = lea.readByte() == 1;

            lea.skip(4);
            lea.skip(4);

            GameConstants.AttackBonusRecv(lea, ret);
            GameConstants.calcAttackPosition(lea, ret); // 307++

            if (GameConstants.sub_781900(ret.skill)) {
                ret.charge = lea.readInt();
            }

            if (GameConstants.isZeroSkill(ret.skill)) {
                ret.asist = lea.readByte();
            }

            if (GameConstants.sub_53F780(ret.skill)) {
                lea.skip(4);
            }

            switch (ret.skill) {
                case 400031033:
                    lea.skip(8);
                    break;
            }

            if (ret.skill == 5220023 || ret.skill == 5220024 || ret.skill == 5220025) {
                lea.readInt();
                lea.readInt();
            }

            ret.isShadowPartner = lea.readByte();
            ret.isBuckShot = lea.readByte();
            lea.skip(4);
            lea.skip(1);

            ret.display = lea.readShort();
            lea.skip(4); //big bang
            ret.attacktype = lea.readByte(); // Weapon class
            if (ret.skill == 23111001 || ret.skill == 36111010) {
                lea.skip(4);
                lea.skip(4);
                lea.skip(4);
            }
            if (ret.skill == 3111013 || ret.skill == 95001000) {
                lea.skip(4);
                lea.skip(4);
            }

            ret.speed = lea.readByte(); // Confirmed
            ret.lastAttackTickCount = lea.readInt(); // Ticks
            lea.readInt();
//        ret.charge = lea.readInt(); //0
            lea.skip(4); // 307++

            if (ret.skill == 5310011 || ret.skill == 5311010) { //임시
                lea.skip(4);
                lea.skip(4);
            }

            int[] finalAttacks = {1100002, 1120013, 1200002, 1300002, 3100001, 3120008, 3200001, 4341054, 5120021, 5220020, 11101002, 13101002, 21100010, 21120012, 23100006, 23120012, 33100009, 33120011, 51100002, 51120002};
            for (int finalAttack : finalAttacks) {
                if (finalAttack == ret.skill) {
                    lea.skip(1);
                    break;
                }
            }

            lea.skip(2);
            lea.skip(1);
            lea.skip(2);
            lea.skip(2);
            ret.csstar = lea.readShort();
            ret.AOE = lea.readShort(); // is AOE or not, TT/ Avenger = 41, Showdown = 0


            long damage;
            int oid;
            List<Pair<Long, Boolean>> allDamageNumbers;
            ret.allDamage = new ArrayList<AttackPair>();

            for (int i = 0; i < ret.targets; i++) {
                oid = lea.readInt();
                ret.mobs.add(chr.getMap().getMonsterByOid(oid));
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                lea.readByte();
                int a = lea.readInt(); // mobId
                //  chr.dropMessageGM(6,ret.skill+"damageR debug"+a+"  "+oid+"   "+ret.speed+"      " +lea);
                lea.readByte();
                MapleMonster m = chr.getMap().getMonsterByOid(oid);
                if (m != null) {
                    if (m.getId() == 8880153) {
                        lea.skip(4);
                        ret.mobpos.add(new Point(727, -580));
                    } else {
                        ret.mobpos.add(new Point(lea.readShort(), lea.readShort()));
                    }
                } else {
                    ret.mobpos.add(new Point(lea.readShort(), lea.readShort()));
                }
                /*ret.value = */
                lea.skip(4);// pos2
                lea.skip(2);
                lea.readInt();
                lea.readInt(); // 307++
                lea.readByte();
                //if (ret.skill != 5221004 || ret.skill != 14001020) {
                //     lea.readInt();
                //  }

                allDamageNumbers = new ArrayList<Pair<Long, Boolean>>();
                for (int j = 0; j < ret.hits; j++) {
                    damage = lea.readLong();
                    if (damage < 0) {
                        damage = damage & 0xFFFFFFFFL;
                    }
                    allDamageNumbers.add(new Pair<Long, Boolean>(Long.valueOf(damage), false));
                }
                lea.skip(4);
                lea.skip(4);

                byte unk = lea.readByte();
                if (unk == 1) {
                    lea.readMapleAsciiString(); // skeleton
                    lea.readInt();
                    int x = lea.readInt();
                    if (x > 0) {
                        for (int k = 0; k < x; ++k) {
                            lea.readMapleAsciiString();
                        }
                    }
                } else if (unk == 2) {
                    lea.readMapleAsciiString(); // skeleton
                    lea.readInt();
                }

                lea.skip(1);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(2);
                lea.skip(1);
                lea.skip(4);

                ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
            }
            ret.position = lea.readPos();
            if (GameConstants.sub_6F3100(ret.skill) > 0) {
                lea.skip(1);
            }
            if (GameConstants.isWildHunter(ret.skill / 10000)) {
                ret.plusPosition = lea.readPos();
            }
            if (GameConstants.sub_6E84F0(ret.skill)) {
                ret.plusPosition2 = lea.readPos();
            }
            if (((ret.skill - 64001009) >= -2 && (ret.skill - 64001009) <= 2)) {
                lea.skip(1); // 1
                ret.chain = lea.readPos();
            }

        } catch (Exception e) {
//            e.printStackTrace();
            FileoutputUtil.log(FileoutputUtil.Attack_Log, "error in Rangedattack.\r\n ordinary : " + HexTool.toString(data.getByteArray()) + "\r\n error : " + e);
        }
        return ret;
    }

    public static final AttackInfo parseMesoExplosion(final LittleEndianAccessor lea, final AttackInfo ret, final MapleCharacter chr) {
        byte bullets;
        if (ret.hits == 0) {
            lea.skip(4);
            bullets = lea.readByte();
            for (int j = 0; j < bullets; j++) {
                ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
                lea.skip(1);
            }
            lea.skip(2); // 8F 02
            return ret;
        }
        int oid;
        List<Pair<Long, Boolean>> allDamageNumbers;

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            ret.mobs.add(chr.getMap().getMonsterByOid(oid));
            //if (chr.getMap().isTown()) {
            //    final MapleMonster od = chr.getMap().getMonsterByOid(oid);
            //    if (od != null && od.getLinkCID() > 0) {
            //	    return null;
            //    }
            //}
            lea.skip(12);
            bullets = lea.readByte();
            allDamageNumbers = new ArrayList<Pair<Long, Boolean>>();
            for (int j = 0; j < bullets; j++) {
                allDamageNumbers.add(new Pair<Long, Boolean>(Long.valueOf(lea.readLong()), false)); //m.e. never crits
            }
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
            lea.skip(4); // C3 8F 41 94, 51 04 5B 01
        }
        lea.skip(4);
        bullets = lea.readByte();

        for (int j = 0; j < bullets; j++) {
            ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
            lea.skip(2);
        }
        // 8F 02/ 63 02

        return ret;
    }
}
