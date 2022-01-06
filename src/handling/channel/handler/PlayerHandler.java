package handling.channel.handler;

import client.*;
import client.custom.inventory.CustomItem;
import client.inventory.ClothesStats;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleAndroid;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import handling.RecvPacketOpcode;
import handling.channel.ChannelServer;
import handling.channel.FishingHandler;
import handling.login.LoginInformationProvider;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import scripting.EventInstanceManager;
import scripting.NPCScriptManager;
import server.AdelProjectile;
import server.CashItemFactory;
import server.CashItemInfo;
import server.DailyGiftItemInfo;
import server.InnerAbillity;
import server.ItemInformation;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Randomizer;
import server.Timer;
import server.Timer.BuffTimer;
import server.Timer.MapTimer;
import server.events.MapleSnowball.MapleSnowballs;
import server.field.boss.will.SpiderWeb;
import server.field.skill.MapleFieldAttackObj;
import server.field.skill.MapleMagicWreck;
import server.games.DetectiveGame;
import server.life.Ignition;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobAttackInfoFactory;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.FieldLimitType;
import server.maps.ForceAtom;
import server.maps.MapleAtom;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import server.maps.MapleSpecialChair;
import server.maps.MapleSpecialChair.MapleSpecialChairPlayer;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import server.movement.LifeMovementFragment;
import server.polofritto.FrittoDancing;
import server.polofritto.FrittoEagle;
import server.quest.MapleQuest;
import tools.CurrentTime;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CField.AttackObjPacket;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CSPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.MobPacket;
import tools.packet.SLFCGPacket;

public class PlayerHandler {

    public static long acCheckLong;

    public static long getAcCheckLong() {
        acCheckLong++;
        return acCheckLong;
    }

    public static long resetAcCheckLong() {
        acCheckLong = 0;
        return acCheckLong;
    }

    public static boolean isFinisher(final int skillid) {
        switch (skillid) {
            case 1111003:
            case 1111005:
            case 1111008:
            case 1121015:
            case 1121010:
            case 400011027:
                return true;
        }
        return false;
    }

    public static void ChangeSkillMacro(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int num = slea.readByte();
        String name;
        int shout, skill1, skill2, skill3;
        SkillMacro macro;

        for (int i = 0; i < num; i++) {
            name = slea.readMapleAsciiString();
            shout = slea.readByte();
            skill1 = slea.readInt();
            skill2 = slea.readInt();
            skill3 = slea.readInt();

            macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            chr.updateMacros(i, macro);
        }
    }

    public static final void ChangeKeymap(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (slea.available() > 8 && chr != null) { // else = pet auto pot
            slea.skip(4); //0
            final int numChanges = slea.readInt();

            for (int i = 0; i < numChanges; i++) {
                final int key = slea.readInt();
                final byte type = slea.readByte();
                final int action = slea.readInt();
                if (type == 1 && action >= 1000) { //0 = normal key, 1 = skill, 2 = item
                    final Skill skil = SkillFactory.getSkill(action);
                    if (skil != null) { //not sure about aran tutorial skills..lol
                        if ((!skil.isFourthJob() && !skil.isBeginnerSkill() && skil.isInvisible() && chr.getSkillLevel(skil) <= 0) || action >= 91000000 && action < 100000000) { //cannot put on a key
                            continue;
                        }
                    }
                }
                //훈장 26 막음
                if (action != 26) {
                    chr.changeKeybinding(key, type, action);
                }
            }
        } else if (chr != null) {
            final int type = slea.readInt(), data = slea.readInt();
            switch (type) {
                case 1:
                    if (data <= 0) {
                        chr.getQuest_Map().remove(MapleQuest.getInstance(GameConstants.HP_ITEM));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.HP_ITEM)).setCustomData(String.valueOf(data));
                    }
                    break;
                case 2:
                    if (data <= 0) {
                        chr.getQuest_Map().remove(MapleQuest.getInstance(GameConstants.MP_ITEM));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.MP_ITEM)).setCustomData(String.valueOf(data));
                    }
                    break;
            }
        }
    }

    public static final void UseTitle(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        int itemId = slea.readInt();
        final Item toUse = chr.getInventory(MapleInventoryType.SETUP).getItem((short) slea.readInt());
        if (toUse == null || (itemId != 0 && toUse.getItemId() != itemId)) {
            return;
        }
              
        if (itemId <= 0) {
            chr.setKeyValue(19019, "id", "0");
            chr.setKeyValue(19019, "date", "0");
        } else {
            chr.setKeyValue(19019, "expired", "0");
            chr.setKeyValue(19019, "id", String.valueOf(itemId));
            chr.setKeyValue(19019, "date", "2079/01/01 00:00:00:000");
        }
        
        MapleQuest q = MapleQuest.getInstance(7290);
        final MapleQuestStatus status = chr.getQuestNAdd(q);
        status.setStatus((byte) 1);
        status.setCustomData(String.valueOf(itemId));
        chr.updateQuest(status);

        chr.getStat().recalcLocalStats(chr);

        chr.getMap().broadcastMessage(chr, CField.showTitle(chr.getId(), itemId), false);
        c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static final void UseChair(final int itemId, final MapleClient c, final MapleCharacter chr, final LittleEndianAccessor slea) {
        int index = slea.readInt(); // itemPos
        slea.skip(1);

        Point pos = new Point(slea.readInt(), slea.readInt()); // charPos

        if (chr == null || chr.getMap() == null) {
            return;
        }

        if (chr.getMapId() == FishingHandler.FishingMap && itemId == FishingHandler.FishingChair && chr.Fishing()) {

            chr.dropMessage(1, "낚시를 끝낸 후 10초간은 다시 시작 할 수 없습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }

        if (GameConstants.isTextChair(itemId)) {
            final String Special = slea.readMapleAsciiString();
            chr.setChairText(Special);
        }

        if (itemId == 3015440 || itemId == 3015650 || itemId == 3015651) {
            int maxmeso = slea.readInt();
            chr.getMap().broadcastMessage(SLFCGPacket.MesoChairPacket(chr.getId(), chr.getMesoChairCount(), itemId));
            ScheduledFuture<?> qwer = Timer.ShowTimer.getInstance().register(() -> {
                if (chr != null && chr.getChair() != 0) {
                    chr.UpdateMesoChairCount(maxmeso);
                }
            }, 2000);
            chr.setMesoChairTimer(qwer);
        }

        chr.setChair(itemId);

        if (itemId / 100 == 30162) {
            //초대한 사람 ID 넣으면 됨.
            List<MapleSpecialChairPlayer> players = new ArrayList<>();
            MapleSpecialChair chair = new MapleSpecialChair(itemId, new Rectangle(pos.x - 142, pos.y - 410, 284, 420), pos, chr, players);

            int[] randEmotions = {2, 10, 14, 17};
            chair.addPlayer(chr, randEmotions[Randomizer.nextInt(randEmotions.length)]);
            chair.addPlayer(null, -1);
            chair.addPlayer(null, -1);
            chair.addPlayer(null, -1);

            chr.getMap().spawnSpecialChair(chair);
        }

        chr.getMap().broadcastMessage(chr, CField.showChair(chr, itemId), false);

        /* if (itemId == FishingHandler.FishingChair && chr.getMapId() == FishingHandler.FishingMap) {
            //progress=1;4035000=99

            int quantity = chr.getItemQuantity(4035000, false);

            if (quantity > 0) {
                chr.setKeyValue(100393, "progress", "1");
                chr.setKeyValue(100393, "4035000", String.valueOf(quantity));
                c.getSession().writeAndFlush(CField.fishing(0));
            }
        }*/

 /*  if (chr.getMapId() == FishingHandler.FishingMap && itemId == FishingHandler.FishingChair && !chr.Fishing()) {
            FishingHandler.StartFishing(chr);
        }*/
        if (chr.getMapId() == ServerConstants.WarpMap) {
            chr.lastChairPointTime = System.currentTimeMillis();
            chr.getClient().getSession().writeAndFlush(CField.UIPacket.detailShowInfo("의자에 앉아 있을 시 1분마다 네오 젬 1개를 획득합니다.", false));
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.playSE("Sound/MiniGame.img/14thTerra/reward"));
        }

        c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static final void CancelChair(final short id, final MapleClient c, final MapleCharacter chr) {
        if (id == -1) { // Cancel Chair
            final int itemId = chr.getChair();
            if (chr.getMesoChairTimer() != null) {
                chr.getMesoChairTimer().cancel(true);
                chr.setMesoChairTimer(null);
            }
            if (chr.getMapId() == 993174800 && chr.getChair() == 3015394) {
                //     chr.dropMessage(6, "낚시 취소");
            }
            chr.setChairText(null);
            chr.setChair(0);

            if (itemId / 100 == 30162) {
                for (MapleSpecialChair chair : chr.getMap().getAllSpecialChairs()) {
                    //내 의자에서 내가 내린다면
                    if (chair.getOwner().getId() == chr.getId()) {
                        //나부터 내리고
                        chair.getPlayers().remove(chr);

                        //내 의자에 같이 앉은 사람들 다 내리게 하고
                        for (MapleSpecialChairPlayer player : chair.getPlayers()) {
                            if (player.getPlayer() != null) {
                                MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterById(player.getPlayer().getId());
                                if (target != null) {
                                    target.setChair(0);
                                    target.getMap().broadcastMessage(CField.specialChair(target, false, false, false, chair));
                                    target.getClient().getSession().writeAndFlush(CField.cancelChair(-1, target));
                                    if (target.getMap() != null) {
                                        target.getMap().broadcastMessage(target, CField.showChair(target, 0), false);
                                    }
                                }
                            }
                        }
                        chr.getMap().broadcastMessage(CField.specialChair(chr, false, false, false, chair));
                        chr.getMap().removeMapObject(chair);
                        break;
                    } else {
                        //다른 사람의 의자에 내가 탑승 중
                        for (MapleSpecialChairPlayer player : chair.getPlayers()) {
                            if (player.getPlayer() != null) {
                                if (player.getPlayer().getId() == chr.getId()) {
                                    player.setPlayer(null);
                                    player.setEmotion(-1);
                                    chr.getMap().broadcastMessage(CField.specialChair(chr, false, false, false, chair));
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            c.getSession().writeAndFlush(CField.cancelChair(-1, chr));
            if (chr.getMap() != null) {
                chr.getMap().broadcastMessage(chr, CField.showChair(chr, 0), false);
            }
        } else { // Use In-Map Chair
            chr.setChair(id);
            c.getSession().writeAndFlush(CField.cancelChair(id, chr));
        }
        if (chr.getMapId() == FishingHandler.FishingMap) {
            FishingHandler.StopFishing(chr);
        }
        if (chr.getMapId() == ServerConstants.WarpMap) {
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9062294, 2500, "#face1#휴식을 그만 둡니다.", ""));
            chr.getClient().getSession().writeAndFlush(SLFCGPacket.playSE("Sound/MiniGame.img/Timer"));
        }

        c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static final void TrockAddMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte addrem = slea.readByte();
        final byte vip = slea.readByte();

        if (vip == 1) { // Regular rocks
            if (addrem == 0) {
                chr.deleteFromRegRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRegRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        } else if (vip == 2) { // VIP Rock
            if (addrem == 0) {
                chr.deleteFromRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        } else if (vip == 3) { // Hyper Rocks
            if (addrem == 0) {
                chr.deleteFromHyperRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addHyperRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        }
        c.getSession().writeAndFlush(CSPacket.OnMapTransferResult(chr, vip, addrem == 0));
    }

    public static final void CharInfoRequest(final int objectid, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final MapleCharacter player = c.getPlayer().getMap().getCharacterById(objectid);
//        c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
        if (player != null) {
            c.getSession().writeAndFlush(CWvsContext.charInfo(player, c.getPlayer().getId() == objectid));
            chr.setLastCharGuildId(player.getGuildId());
            byte[] img = player.getClient().getFarmImg();
            if (img != null) {
                c.getSession().writeAndFlush(CField.getPhotoResult(player.getClient(), img));
            }
        }
    }

    public static final void TakeDamage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(4); // randomized
        slea.skip(4); // 0, 324 ++
        slea.readInt();
        final byte type = slea.readByte(); //-4 is mist, -3 and -2 are map damage. -5 운석 (더스크만 봣음)
        slea.skip(1); // Element - 0x00 = elementless, 0x01 = ice, 0x02 = fire, 0x03 = lightning
        slea.skip(1); // 338 ++
        int damage = slea.readInt();
        slea.skip(2);
        boolean isDeadlyAttack = false;
        boolean pPhysical = false;
        int oid = 0;
        int monsteridfrom = 0;
        int fake = 0;
        int mpattack = 0;
        int skillid = 0;
        int pID = 0;
        int pDMG = 0;
        byte direction = 0;
        byte pType = 0;
        Point pPos = new Point(0, 0);
        MapleMonster attacker = null;
        if (chr == null || chr.getMap() == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        if ((chr.isGM() && chr.isInvincible()) || chr.getBuffedValue(1320019)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        final PlayerStats stats = chr.getStat();
        if (type > -2) { // Not map damage
            slea.readInt(); // oid, 2踰덈텋?ъ샂
            // 인트3 개  0 -1 0 생김 + 342
            slea.readInt(); //0+342
            slea.readInt();//-1+342
            slea.readInt();//0 +342

            slea.readInt(); //-1 +343
            slea.readInt(); //0 +343
            slea.readInt(); //0 +343
            slea.readInt(); //0 +343
            slea.readInt(); //0 +343

            slea.readInt();
            slea.readByte(); // 350 new

            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            attacker = chr.getMap().getMonsterByOid(oid);
            direction = slea.readByte();
            damage = chr.reduceNovilityhandle(damage);
            if (attacker == null) { // 자폭 공격
                chr.addHP(-damage);

                MapleMonster ordi = MapleLifeFactory.getMonster(monsteridfrom);

                final MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(ordi, 0);

                if (attackInfo != null) {
                    if (attackInfo.getDiseaseSkill() != 0) {
                        MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel()).applyEffect(chr, ordi, false, false);
                    }
                }

                chr.getMap().broadcastMessage(chr, CField.damagePlayer(chr.getId(), type, damage, monsteridfrom, direction, skillid, pDMG, pPhysical, pID, pType, pPos, (byte) 0, 0, fake), false);
                return;
            }

            if (attacker.getId() != monsteridfrom || attacker.getLinkCID() > 0 || attacker.isFake() || attacker.getStats().isFriendly()) {
                return;
            }

            if (chr.getBuffedValue(400051009) && ((double) chr.getStat().getCurrentMaxHp() * 0.9 <= damage)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 400051009);
                return;
            }

            chr.isNovilityhandle(damage); // 노빌리티

            if (chr.getBuffedValue(400031030)) {
                final MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, type);
                if (attackInfo != null) {
                    int damagePercent = attackInfo.getFixDamR();
                    int windWall = (int) Math.max(0, chr.getBuffedValue(MapleBuffStat.WindWall) - (damagePercent * chr.getBuffedEffect(MapleBuffStat.WindWall).getZ()));
                    if (windWall > 0) {
                        chr.setBuffedValue(MapleBuffStat.WindWall, windWall);
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        statups.put(MapleBuffStat.WindWall, new Pair<>(windWall, (int) chr.getBuffLimit(400031030)));
                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.WindWall), chr));
                    } else {
                        chr.cancelEffectFromBuffStat(MapleBuffStat.WindWall);
                    }
                }
            }

            if (GameConstants.isXenon(chr.getJob())) {
                if (chr.getBuffedValue(MapleBuffStat.AegisSystem) != null) {
                    MapleAtom atom = new MapleAtom(false, chr.getId(), 5, true, 36110004, chr.getTruePosition().x, chr.getTruePosition().y);

                    atom.setDwFirstTargetId(oid);
                    atom.addForceAtom(new ForceAtom(0, 35, 5, Randomizer.rand(80, 120), (short) Randomizer.rand(0, 500)));
                    atom.addForceAtom(new ForceAtom(0, 36, 5, Randomizer.rand(80, 120), (short) Randomizer.rand(0, 500)));
                    atom.addForceAtom(new ForceAtom(0, 37, 5, Randomizer.rand(80, 120), (short) Randomizer.rand(0, 500)));

                    if (chr.getSummon(400041044) != null) {
                        Rectangle box = new Rectangle(chr.getSummon(400041044).getTruePosition().x - 320, chr.getSummon(400041044).getTruePosition().y - 490, 640, 530);
                        if (box.contains(chr.getTruePosition())) {
                            for (int i = 0; i < 5; ++i) {
                                atom.addForceAtom(new ForceAtom(0, 38 + i, 5, Randomizer.rand(80, 120), (short) Randomizer.rand(0, 500)));
                            }
                        }
                    }

                    c.getPlayer().getMap().spawnMapleAtom(atom);
                }
            }

            if (chr.getBuffedValue(36121007)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.OnCapsule);
            }

            if (!attacker.getStats().isBoss()) {
                if (chr.getBuffedValue(142001007)) {
                    chr.givePPoint(142001007, false);
                }
            }

            if (chr.getBuffedEffect(MapleBuffStat.BodyOfSteal) != null) {
                MapleStatEffect bodyOfSteal = chr.getBuffedEffect(MapleBuffStat.BodyOfSteal);
                if (chr.bodyOfSteal < bodyOfSteal.getY()) {
                    chr.bodyOfSteal++;
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.BodyOfSteal, new Pair<>(chr.bodyOfSteal, (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.BodyOfSteal))));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, bodyOfSteal, chr));
                }
            }

            if (chr.getSkillLevel(1320011) > 0 && (chr.getBuffedEffect(MapleBuffStat.Beholder) != null)) {
                MapleStatEffect revenge = SkillFactory.getSkill(1320011).getEffect(chr.getTotalSkillLevel(1320011));
                if (revenge.makeChanceResult()) {
                    chr.getClient().getSession().writeAndFlush(SummonPacket.BeholderRevengeAttack(chr, revenge.getDamage(), oid));
                }
            }

            if (chr.getSkillLevel(101120109) > 0 && chr.getGender() == 1) {
                MapleStatEffect immuneBarrier = SkillFactory.getSkill(101120109).getEffect(chr.getSkillLevel(101120109));
                if (immuneBarrier.makeChanceResult()) {
                    immuneBarrier.applyTo(chr, false);
                }
            }

            if (chr.getSkillLevel(5120011) > 0) {
                MapleStatEffect counterAttack = SkillFactory.getSkill(5120011).getEffect(chr.getSkillLevel(5120011));
                if (counterAttack.makeChanceResult()) {
                    counterAttack.applyTo(chr, false);
                }
            }

            if (chr.getSkillLevel(5220012) > 0) {
                MapleStatEffect counterAttack = SkillFactory.getSkill(5220012).getEffect(chr.getSkillLevel(5220012));
                if (counterAttack.makeChanceResult()) {
                    counterAttack.applyTo(chr, false);
                }
            }

            //다이크 무적
            if (chr.getBuffedEffect(MapleBuffStat.Dike) != null) {
                if (chr.getBuffedEffect(151121011) == null) {
                    if ((((int) (System.currentTimeMillis() % 1000000000)) - chr.getBuffedEffect(MapleBuffStat.Dike).getStarttime()) <= 1000) {
                        SkillFactory.getSkill(151121011).getEffect(GameConstants.getLinkedSkill(151121011)).applyTo(chr, false);
                    }
                }
            }

            if (chr.getBuffedEffect(MapleBuffStat.RoyalGuardPrepare) != null) {
                c.getSession().writeAndFlush(CField.RoyalGuardDamage());

                SkillFactory.getSkill(51001011).getEffect(GameConstants.getLinkedSkill(51001011)).applyTo(chr, false);

                if (chr.getSkillLevel(51120003) > 0) {
                    SkillFactory.getSkill(51120003).getEffect(chr.getSkillLevel(51120003)).applyTo(chr, false);
                }

                if (chr.getRoyalStack() >= 3 && chr.getSkillLevel(51110009) > 0) {
                    if (chr.getRoyalStack() < 5) {
                        chr.setRoyalStack((byte) (chr.getRoyalStack() + 1));
                    }
//                    SkillFactory.getSkill(51001005).getEffect(chr.getSkillLevel(51001005)).applyTo(chr, false);
                } else if (chr.getRoyalStack() <= 3 && chr.getSkillLevel(51001005) > 0) {
                    if (chr.getRoyalStack() < 3) {
                        chr.setRoyalStack((byte) (chr.getRoyalStack() + 1));
                    }
//                    SkillFactory.getSkill(51001005).getEffect(chr.getSkillLevel(51001005)).applyTo(chr, false);
                }
            }

            if (damage > 0) {
                MapleMonster seren = chr.getMap().getMonsterById(8880602);
                MapleMonster serenDawn = chr.getMap().getMonsterById(8880603);
                switch(attacker.getId()) {
                    case 8880600:
                        chr.addSerenGauge(type == 2 ? 150 : (type == 1 ? 100 : 150));
                        break;
                    case 8880601:
                        chr.addSerenGauge(150);
                        break;
                    case 8880602:
                        chr.addSerenGauge(1000);
                        break;
                    case 8880603:
                        chr.addSerenGauge(100);
                        if (seren != null && serenDawn != null) {
                            seren.gainShield(seren.getStats().getHp() / 100L, seren.getShield() <= 0L, 0);
                            serenDawn.getMap().broadcastMessage(MobPacket.BossSeren.SerenChangePhase("Mob/8880603.img/info/shield", 2, serenDawn));
                        }
                        break;
                    case 8880604:
                        chr.addSerenGauge(100);
                        if (seren != null && serenDawn != null) {
                            seren.gainShield(seren.getStats().getHp() / 100L, seren.getShield() <= 0L, 0);
                            serenDawn.getMap().broadcastMessage(MobPacket.BossSeren.SerenChangePhase("Mob/8880603.img/info/shield", 2, serenDawn));
                        }
                        break;
                    case 8880605:
                    case 8880606:
                        if (seren != null && serenDawn != null) {
                            seren.gainShield(seren.getStats().getHp() / 100L, seren.getShield() <= 0L, 0);
                            serenDawn.getMap().broadcastMessage(MobPacket.BossSeren.SerenChangePhase("Mob/8880603.img/info/shield", 2, serenDawn));
                        }
                        break;
                    case 8880607:
                        chr.addSerenGauge(type == 2 ? 200 : (type == 4 ? 200 : 100));
                        break;
                    case 8880608:
                        chr.addSerenGauge(100);
                        break;
                    case 8880609:
                        chr.addSerenGauge(type == 2 ? 200 : (type == 4 ? 200 : 100));
                    case 8880610:
                    case 8880611:
                    case 8880612:
                    default:
                        break;
                    case 8880613:
                        MobSkill ms = MobSkillFactory.getMobSkill(120, 1);
                        ms.setDuration(3000L);
                        c.getPlayer().giveDebuff(MapleBuffStat.Seal, ms);
                }

            }


            if (type != -1 && damage > 0) { // Bump damage
                final MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, type);
                if (attackInfo != null) {
                    final MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                    if (skill != null && (damage == -1 || damage > 0)) {
                        skill.applyEffect(chr, attacker, false, attacker.isFacingLeft());
                    }
                    attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                }
            }
            skillid = slea.readInt();
            pDMG = slea.readInt(); //
            final byte defType = slea.readByte();
            slea.skip(1); // ?
            if (defType == 1) { // 媛??
                final Skill bx = SkillFactory.getSkill(31110008);
                final int bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    final MapleStatEffect eff = bx.getEffect(bof);
                    if (Randomizer.nextInt(100) <= eff.getX()) { // estimate
                        chr.addHP(eff.getY() * chr.getStat().getCurrentMaxHp() / 100);
                        chr.handleForceGain(oid, 31110008, eff.getZ());
                    }
                }
            }
            if (skillid != 0 || chr.getSkillLevel(14120010) > 0) {
                pPhysical = slea.readByte() > 0;
                pID = slea.readInt();
                pType = slea.readByte();
                // slea.skip(4); // Mob position garbage
                pPos = slea.readPos();
                if (pID != 14120010) {
                    attacker.damage(chr, pDMG, true, skillid); // 諛섏궗 ?곕?吏
                } else {
                    damage -= damage * SkillFactory.getSkill(pID).getEffect(chr.getSkillLevel(14120010)).getIgnoreMobDamR() / 100;
                }
                if (skillid == 31101003) {
                    attacker.applyStatus(c, MonsterStatus.MS_Stun, new MonsterStatusEffect(skillid, SkillFactory.getSkill(31101003).getEffect(chr.getSkillLevel(31101003)).getSubTime()), 1, SkillFactory.getSkill(31101003).getEffect(chr.getSkillLevel(31101003)));
                }
            }
        } // map damage End

        /*   if (c.getPlayer().getMapId() >= 105200210 && c.getPlayer().getMapId() <= 105200219 || c.getPlayer().getMapId() >= 105200610 && c.getPlayer().getMapId() <= 105200619) { //피에르 장판 스킬봉인
            if (type == -4) {
                Map<MapleBuffStat, Pair<Integer, Integer>> diseases = new EnumMap<>(MapleBuffStat.class);
                diseases.put(MapleBuffStat.Seal, new Pair<>(1, (int) 1500));
                MobSkill ms = MobSkillFactory.getMobSkill(120, 1);
                chr.giveDebuff(diseases, ms);
            }
        }*/
        if (damage == -1) {
            if (GameConstants.isNightLord(chr.getJob())) {
                fake = 4120002;
            } else if (GameConstants.isShadower(chr.getJob())) {
                fake = 4220002;
            } else if (GameConstants.isMercedes(chr.getJob())) {
                if (chr.getSkillLevel(23110004) > 0) {
                    fake = 23110004;
                } else {
                    fake = 23000001;
                }
            } else if (GameConstants.isPhantom(chr.getJob())) {
                fake = 24110004;
            } else if (type == -1 && chr.getJob() == 122 && attacker != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10) != null) {
                if (chr.getTotalSkillLevel(1220006) > 0) {
                    final MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(chr.getTotalSkillLevel(1220006));
                    attacker.applyStatus(c, MonsterStatus.MS_Stun, new MonsterStatusEffect(1220006, eff.getDuration()), 1, eff);
                    fake = 1220006;
                }
            }
        } else if (damage < -1) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        if (damage == 0) {

            if (chr.getBuffedEffect(MapleBuffStat.BlessingArmor) != null) {
                if (chr.getBuffedValue(MapleBuffStat.BlessingArmor) == 1) {
                    chr.addCooldown(1210016, System.currentTimeMillis(), chr.getBuffedEffect(MapleBuffStat.BlessingArmor).getCooldown(chr));
                    chr.cancelEffectFromBuffStat(MapleBuffStat.BlessingArmor);
                    chr.cancelEffectFromBuffStat(MapleBuffStat.BlessingArmorIncPad);
                } else {
                    chr.setBuffedValue(MapleBuffStat.BlessingArmor, chr.getBuffedValue(MapleBuffStat.BlessingArmor) - 1);
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.BlessingArmor, new Pair<>((int) chr.getBuffedValue(MapleBuffStat.BlessingArmor), (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.BlessingArmor))));
                    statups.put(MapleBuffStat.BlessingArmorIncPad, new Pair<>((int) chr.getBuffedValue(MapleBuffStat.BlessingArmorIncPad), (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.BlessingArmorIncPad))));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.BlessingArmor), chr));
                }
            }

            if (chr.getBuffedEffect(MapleBuffStat.HolyMagicShell) != null) {
                if (chr.getHolyMagicShell() > 0) {
                    chr.setHolyMagicShell((byte) (chr.getHolyMagicShell() - 1));
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.HolyMagicShell, new Pair<>((int) chr.getHolyMagicShell(), (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.HolyMagicShell))));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.HolyMagicShell), chr));
                } else {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.HolyMagicShell);
                }
            }

            if (chr.getSkillLevel(13110026) > 0) {
                return;
            }

            if (chr.getBuffedValue(36111003)) {
                if (chr.stackbuff == 1) {
                    chr.cancelEffect(chr.getBuffedEffect(MapleBuffStat.StackBuff), false, -1);
                } else {
                    chr.getBuffedEffect(MapleBuffStat.StackBuff).applyTo(chr, false);
                }
            }
            if (chr.getSkillLevel(4330009) > 0 && !chr.getBuffedValue(4330009)) {
                SkillFactory.getSkill(4330009).getEffect(chr.getSkillLevel(4330009)).applyTo(chr, false);
            }
        }
        if (chr.getJob() == 2711 || chr.getJob() == 2712) {
            if (chr.getSkillLevel(27110007) > 0) { //
                Skill skill = SkillFactory.getSkill(27110007);
                int critical = chr.getSkillLevel(skill);
                EnumMap<MapleBuffStat, Pair<Integer, Integer>> statups = new EnumMap<>(MapleBuffStat.class);
                if ((chr.getStat().getHp() / chr.getStat().getCurrentMaxHp()) * 100 < (chr.getStat().getMp() / chr.getStat().getCurrentMaxMp(chr)) * 100) {
                    statups.put(MapleBuffStat.LifeTidal, new Pair<>(2, 0));
                    c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, skill.getEffect(critical), chr));
                } else if ((chr.getStat().getHp() / chr.getStat().getCurrentMaxHp()) * 100 > (chr.getStat().getMp() / chr.getStat().getCurrentMaxMp(chr)) * 100) {
                    if (critical > 0) {
                        statups.put(MapleBuffStat.LifeTidal, new Pair<>(1, 0));
                        c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, skill.getEffect(critical), chr));
                    }
                }
            }
        }
        if (pPhysical && skillid == 1201007 && chr.getTotalSkillLevel(1201007) > 0) { // Only Power Guard decreases damage
            damage = (damage - pDMG);
            if (damage > 0) {
                final MapleStatEffect eff = SkillFactory.getSkill(1201007).getEffect(chr.getTotalSkillLevel(1201007));
                long enemyDMG = Math.min((damage * (eff.getY() / 100)), (attacker.getMobMaxHp() / 2));
                if (enemyDMG > pDMG) {
                    enemyDMG = pDMG; // ;)
                }
                if (enemyDMG > 1000) { // just a rough estimation, we cannot reflect > 1k
                    enemyDMG = 1000; // too bad
                }
                attacker.damage(chr, enemyDMG, true, 1201007);
            } else {
                damage = 1;
            }
        }
        if (damage > 0) {
            if (attacker != null) {
                if ((attacker.getId() == 8880302 || attacker.getId() == 8880342) || (type == 5 && (attacker.getId() == 8880303 || attacker.getId() == 8880304 || attacker.getId() == 8880343 || attacker.getId() == 8880344))) {
                    chr.setMoonGauge(Math.max(0, chr.getMoonGauge() - 3));
                    chr.getClient().getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(chr.getMoonGauge()));
                }
            }
            if (chr.getSkillLevel(37000006) > 0 && chr.getBuffedEffect(MapleBuffStat.RwBarrier) == null) {
                MapleStatEffect barrier = SkillFactory.getSkill(37000006).getEffect(chr.getSkillLevel(37000006));
                chr.setBarrier((int) Math.min(damage * 2/*damage * barrier.getX()*/, chr.getStat().getCurrentMaxHp()));
                barrier.applyTo(chr, false); //, damage * barrier.getX()\
            }
            if (chr.getBuffedValue(MapleBuffStat.Morph) != null) {
                chr.cancelMorphs();
            }

            if (chr.getBuffedValue(MapleBuffStat.MesoGuard) != null && chr.getBuffedValue(MapleBuffStat.PickPocket) != null) {
                damage /= 2;
                chr.gainMeso(-damage * chr.getBuffedEffect(MapleBuffStat.MesoGuard).getX() / 100, false);

                MapleStatEffect eff = chr.getBuffedEffect(MapleBuffStat.MesoGuard);

                int rand = eff.getProp();
                int max = eff.getY();
                if (chr.getSkillLevel(4220009) > 0) {
                    rand += SkillFactory.getSkill(4220009).getEffect(chr.getSkillLevel(4220009)).getU();
                }
                if (chr.getSkillLevel(4220045) > 0) {
                    rand += SkillFactory.getSkill(4220045).getEffect(chr.getSkillLevel(4220045)).getProp();
                    max += SkillFactory.getSkill(4220045).getEffect(chr.getSkillLevel(4220045)).getBulletCount();
                }

                for (int i = 0; i < 5; ++i) {
                    if (Randomizer.isSuccess(Math.min(100, rand)) && chr.getPickPocket() < max && attacker != null) {
                        chr.getMap().spawnMesoDrop(1, attacker.getTruePosition(), attacker, chr, true, (byte) 0);
                        chr.setPickPocket(Math.min(max, chr.getPickPocket() + 1));
                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(eff.getStatups(), eff, chr));
                    }
                }
            }

            if (chr.getBuffedValue(MapleBuffStat.DarkSight) == null && chr.getMapId() / 10000 != 10904 && chr.getMapId() / 10000 != 91013 && chr.getMapId() / 10000 != 91015) {
                int[] skills = {4210015, 4330001};
                for (int skill : skills) {
                    if (chr.getSkillLevel(skill) > 0 && chr.getSkillLevel(4001003) > 0) {
                        if (Randomizer.isSuccess(SkillFactory.getSkill(skill).getEffect(chr.getSkillLevel(skill)).getX())) {
                            SkillFactory.getSkill(4001003).getEffect(chr.getSkillLevel(4001003)).applyTo(chr, false);
                            break;
                        }
                    }
                }
            }

            if (!chr.getBuffedValue(1210016) && chr.getSkillLevel(1210016) > 0) {
                MapleStatEffect blessingArmor = SkillFactory.getSkill(1210016).getEffect(chr.getSkillLevel(1210016));
                if (blessingArmor.makeChanceResult() && chr.getCooldownLimit(1210016) == 0) {
                    blessingArmor.applyTo(chr, false);
                }
            }

            if (chr.getBuffedValue(MapleBuffStat.RhoAias) != null) {
                MapleStatEffect eff = chr.getBuffedEffect(MapleBuffStat.RhoAias);
                if (chr.getRhoAias() > 1) {
                    chr.setRhoAias(chr.getRhoAias() - 1);
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.RhoAias, new Pair<>(eff.getX(), (int) chr.getBuffLimit(400011011)));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, chr));
                } else {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.RhoAias);
                }
            }
            if (chr.getBuffedValue(MapleBuffStat.UnkBuffStat2) != null) {
                int reducedamage = (int) damage - (damage / 100 * 25);
                if (chr.isGM()) {
                    //  System.out.println("원래 데미지 : " + damage + " : " + "실피디아 감소후 데미지 : " + reducedamage);
                }
                damage = reducedamage;
            }

            MapleStatEffect eff = chr.getBuffedEffect(MapleBuffStat.GuardOfMountain);
            if (eff != null) {
                int usedMP = eff.getU();

                if (usedMP <= chr.getStat().getMp()) {
                    chr.addMP(usedMP);
                    c.getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, 162001005, 10, 0, 0, (byte) 0, true, null, null, null));
                    chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, 0, 162001005, 10, 0, 0, (byte) 0, false, null, null, null), chr.getTruePosition());
                }
            }
            short damAbsorbShieldR = chr.getStat().damAbsorbShieldR;
            if (damAbsorbShieldR > 0) {
                int reducedDam = (int) Math.round(damage * (damAbsorbShieldR / 100.0));

                damage -= reducedDam;
            }

            boolean damaged = false;

            if (type == -1 || type == 0) {
                if (chr.getBuffedValue(MapleBuffStat.MagicGaurd) != null || chr.getSkillLevel(12000024) > 0 || chr.getSkillLevel(27000003) > 0) {
                    int hploss = 0, mploss = 0;
                    if (isDeadlyAttack) {
                        if (stats.getHp() > 1) {
                            hploss = (int) (stats.getHp() - 1);
                        }
                        if (stats.getMp() > 1) {
                            mploss = (int) (stats.getMp() - 1);
                        }
                        if (chr.getBuffedValue(MapleBuffStat.Infinity) != null) {
                            mploss = 0;
                        }
                        chr.addMPHP(-hploss, -mploss);
                        //} else if (mpattack > 0) {
                        //    chr.addMPHP(-damage, -mpattack);
                    } else {
                        if (chr.getSkillLevel(12000024) > 0) {
                            Skill skill = SkillFactory.getSkill(12000024);
                            eff = skill.getEffect(chr.getSkillLevel(skill));
                            mploss = (int) (damage * eff.getX() / 100) + mpattack;
                        } else if (chr.getSkillLevel(27000003) > 0) {
                            Skill skill = SkillFactory.getSkill(27000003);
                            eff = skill.getEffect(chr.getSkillLevel(skill));
                            mploss = (int) (damage * eff.getX() / 100) + mpattack;
                        } else if (chr.getBuffedEffect(MapleBuffStat.MagicGaurd) != null) {
                            mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MagicGaurd).doubleValue() / 100.0)) + mpattack;
                        }
                        hploss = damage - mploss;
                        if (chr.getBuffedValue(MapleBuffStat.Infinity) != null) {
                            mploss = 0;
                        } else if (mploss > stats.getMp()) {
                            mploss = (int) stats.getMp();
                            hploss = damage - mploss + mpattack;
                        }
                        chr.addMPHP(-hploss, -mploss);
                    }
                    damaged = true;
                } else if (chr.getBuffedValue(MapleBuffStat.BlessOfDarkness) != null) {
                    attacker = (MapleMonster) chr.getMap().getMapObject(oid, MapleMapObjectType.MONSTER);
                    if (attacker != null) {
                        int reducedamage = (int) (damage * (chr.getBuffedValue(MapleBuffStat.BlessOfDarkness).doubleValue() / 100));
                        damage = reducedamage;
                        chr.setBlessofDarkness((byte) (chr.getBlessofDarkness() - 1));
                        if (chr.getBlessofDarkness() == 0) {
                            chr.cancelEffectFromBuffStat(MapleBuffStat.BlessOfDarkness);
                        } else {
                            SkillFactory.getSkill(27100003).getEffect(chr.getSkillLevel(27100003)).applyTo(chr, false);
                        }
                    }
                } else if (chr.getBuffedEffect(MapleBuffStat.PowerTransferGauge) != null) {
                    damaged = true;
                    if (chr.getBarrier() < damage) {
                        damage -= chr.getBarrier();
                        chr.addHP(-damage);
                        chr.setBarrier(0);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.PowerTransferGauge);
                    } else {
                        chr.setBarrier(chr.getBarrier() - damage);
                        chr.getBuffedEffect(MapleBuffStat.PowerTransferGauge).applyTo(chr, false, (int) chr.getBuffLimit(chr.getBuffSource(MapleBuffStat.PowerTransferGauge)));
                    }
                } else if (chr.getBuffedEffect(MapleBuffStat.Etherealform) != null) {
                    damaged = true;
                    if (GameConstants.isKinesis(chr.getJob())) {
                        chr.addHP(-chr.getBuffedEffect(MapleBuffStat.Etherealform).getY());
                    } else {
                        chr.addMP(-chr.getBuffedEffect(MapleBuffStat.Etherealform).getX());
                    }
                } else if (chr.getBuffedEffect(MapleBuffStat.RwBarrier) != null) {
                    damaged = true;
                    if (chr.getBarrier() < damage) {
                        damage -= chr.getBarrier();
                        chr.addHP(-damage);
                        chr.setBarrier(0);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.RwBarrier);
                    } else {
                        chr.setBarrier(chr.getBarrier() - damage);
                        chr.getBuffedEffect(MapleBuffStat.RwBarrier).applyTo(chr, false, (int) chr.getBarrier());
                    }
                }
            }

            if (!damaged) {
                if (isDeadlyAttack) {
                    chr.addMPHP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0, stats.getMp() > 1 ? -(stats.getMp() - 1) : 0);
                } else {
                    chr.addMPHP(-damage, -mpattack);
                }
                if (chr.getBuffedValue(80001479)) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.IndiePadR, 80001479);
                    chr.cancelEffectFromBuffStat(MapleBuffStat.IndieMadR, 80001479);
                }
            } else {
                if (chr.getBuffedValue(80001479)) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.IndiePadR, 80001479);
                    chr.cancelEffectFromBuffStat(MapleBuffStat.IndieMadR, 80001479);
                }
            }
        }
        byte offset = 0;
        int offset_d = 0;
        if (slea.available() == 1) {
            offset = slea.readByte();
            if (offset == 1 && slea.available() >= 4) {
                offset_d = slea.readInt();
            }
            if (offset < 0 || offset > 2) {
                offset = 0;
            }
        }
        chr.getMap().broadcastMessage(chr, CField.damagePlayer(chr.getId(), type, damage, monsteridfrom, direction, skillid, pDMG, pPhysical, pID, pType, pPos, offset, offset_d, fake), false);
    }

    public static final void AranCombo(final MapleClient c, final MapleCharacter chr, int skillid) {
        if (chr != null && chr.getJob() >= 2000 && chr.getJob() <= 2112) {
            MapleStatEffect skill = SkillFactory.getSkill(skillid).getEffect(chr.getSkillLevel(skillid));

            int toAdd = skill.getAttackCount();
            short combo = chr.getCombo();
            final long curr = System.currentTimeMillis();

            int ability = combo / 50;

            combo = (short) Math.min(30000, combo + toAdd);
            chr.setLastCombo(curr);
            if (combo > 1000) {
                combo = 1001;
            }

            chr.setCombo(combo);

            c.getSession().writeAndFlush(CField.aranCombo(combo));

            if (chr.getSkillLevel(21000000) > 0 && ability != (combo / 50)) {
                SkillFactory.getSkill(21000000).getEffect(chr.getSkillLevel(21000000)).applyTo(chr, false);
            }

            if (combo >= 1000) {
                final Skill ad = SkillFactory.getSkill(21110016);
                final MapleStatEffect effect = ad.getEffect(chr.getSkillLevel(21110016));
                effect.applyTo(chr, true);
            }
        }
    }

    public static void AndroidEar(MapleClient c, final LittleEndianAccessor slea) {
        MapleAndroid android = c.getPlayer().getAndroid();
        if (android == null) {
            c.getPlayer().dropMessage(1, "알 수 없는 오류가 발생 하였습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        short slot = slea.readShort();
        final Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (item != null && item.getItemId() == 2892000) {
            android.setEar(!android.getEar());
            c.getPlayer().updateAndroid();
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, true);
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        } else {
            c.getPlayer().dropMessage(1, "알 수 없는 오류가 발생 하였습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        }
    }

    public static final void LossAranCombo(final MapleClient c, final MapleCharacter chr, int toAdd) {
        if (chr != null && chr.getJob() >= 2000 && chr.getJob() <= 2112) {
            short combo = chr.getCombo();
            final long curr = System.currentTimeMillis();
            if (combo <= 0) {
                combo = 0;
            }
            combo = (short) Math.min(30000, combo - toAdd);
            chr.setLastCombo(curr);
            chr.setCombo(combo);
            c.getSession().writeAndFlush(CField.aranCombo(combo));
        }
    }

    public static final void BossWarp(final LittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(8);
        int mapid = slea.readInt();
        if (mapid == 401000001)
            mapid = 401060000;

        // if (mapid == 271041000 || mapid == 970072200) {
        MapleMap target = chr.getClient().getChannelServer().getMapFactory().getMap(mapid);
        chr.changeMap(target, target.getPortal(0));
        chr.getClient().getSession().writeAndFlush(UIPacket.closeUI(1266));
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
        // }
    }

    public static final void BossMatching(final LittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(4);
        int type = slea.readInt();
        int mapid = -1;
        switch (type) {
            case 0x01: // 발록
                mapid = 105100100;
                break;
            case 0x02: // 자쿰
            case 0x03:
            case 0x04:
                mapid = 211042300;
                break;
            case 0x07: // 힐라
            case 0x08:
                mapid = 262000000;
                break;
            case 0x15: // 매그너스
            // 이지는 다른데인걸로앎
            // mapid = 401000001;
            // break;
            case 0x16: // 매그너스
            case 0x17:
                mapid = 401060000;
                break;
            case 0x25: // 카웅
                mapid = 221030900;
                break;
            case 0x26: // 파풀라투스
            case 0x27:
            case 0x28:
                mapid = 220080000;
                break;
            case 0x09: // 피에르
            case 0x0A:
                mapid = 105200000;
                break;
            case 0x0B: // 반반
            case 0x0C:
                mapid = 105200000;
                break;
            case 0x0D: // 블러디 퀸
            case 0x0E:
                mapid = 105200000;
                break;
            case 0x0F: // 벨룸
            case 0x10:
                mapid = 105200000;
                break;
            case 0x11: // 반레온
            case 0x12:
            case 0x24:
                mapid = 211070000;
                break;
            case 0x1B: // 혼테일
            case 0x05:
            case 0x06:
                mapid = 240040700;
                break;
            case 0x13: // 아카이럼
            case 0x14:
                mapid = 272000000;
                break;
            case 0x18: // 핑크빈
            case 0x19:
                mapid = 270040000;
                break;
            case 0x1A: // 시그너스 노말
                mapid = 271041000;
                break;
            case 0x1D: // 스우
            case 0x1C:
                mapid = 350060300;
                break;
            case 0x20: // 데미안
            case 0x21:
                mapid = 105300303;
                break;
            case 0x22: // 루시드
            case 0x23:
            case 0x31:
                mapid = 450004000;
                break;
            case 0x2A: // 윌
            case 0x29:
                mapid = 450007240;
                break;
            case 0x2D: // 더스크
                mapid = 450009301;
                break;
            case 0x2B: // 진 힐라
                mapid = 450011990;
                break;
            case 0x2E: // 듄켈
                mapid = 450012200;
                break;
            case 0x2C: // 검은마법사
                mapid = 450012500;
                break;
            default:
                System.out.println("해당 보스와 연결된 맵이 없습니다. type : 0x" + Integer.toHexString(type).toUpperCase() + "");
                break;
        }
        if (mapid != -1) {
            MapleMap target = chr.getClient().getChannelServer().getMapFactory().getMap(mapid);
            if (chr.getParty() != null) {
                for (final MaplePartyCharacter chrz : chr.getParty().getMembers()) {
                    final MapleCharacter curChar = chr.getClient().getChannelServer().getPlayerStorage().getCharacterById(chrz.getId());
                    if (curChar != null && (curChar.getMapId() == chr.getMapId() || curChar.getEventInstance() == chr.getEventInstance())) {
                        curChar.changeMap(target, target.getPortal(0));
                    }
                }
            } else {
                chr.changeMap(target, target.getPortal(0));
            }
        }
        chr.getClient().getSession().writeAndFlush(UIPacket.closeUI(7));
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
        //   chr.getClient().getSession().writeAndFlush();
    }

    public static final void BlessOfDarkness(MapleCharacter chr) {
        if (chr.getBlessofDarkness() < 3) {
            chr.setBlessofDarkness((byte) (chr.getBlessofDarkness() + 1));
        }
        if (chr.getSkillLevel(27100003) > 0) {
            SkillFactory.getSkill(27100003).getEffect(chr.getSkillLevel(27100003)).applyTo(chr, false);
        }
    }

    public static final void UseItemEffect(final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(MapleInventoryType.CASH).findById(itemId);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        if (itemId != 5510000) {
            chr.setItemEffect(itemId);
        }
        chr.getMap().broadcastMessage(chr, CField.itemEffect(chr.getId(), itemId), false);
    }

    public static final void CancelItemEffect(final int id, final MapleCharacter chr) {
        chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-id), false, -1);
    }

    public static final void CancelBuffHandler(final LittleEndianAccessor slea, final MapleCharacter chr) {
        int sourceid = slea.readInt();
        if (chr == null || chr.getMap() == null || SkillFactory.getSkill(sourceid) == null) {
            return;
        }

        int level = chr.getSkillLevel(GameConstants.getLinkedSkill(sourceid));
        MapleStatEffect effect = SkillFactory.getSkill(sourceid).getEffect(level);

        if (sourceid == 400051334) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.IndieNotDamaged);
        } else if (sourceid == 400031044) {
            chr.cancelEffect(effect, false, -1);
            SkillFactory.getSkill(sourceid).getEffect(level).applyTo(chr, false);
        } else {
            chr.cancelEffect(effect, false, -1);
        }
        chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);

        if (sourceid == 164121042) {
            int passedTime = (int) (System.currentTimeMillis() - chr.getKeyDownSkill_Time());
            int remainedCoolTime = (20000 - passedTime) / 1000;
            int reducingTime = remainedCoolTime * 10 * 1000;
            chr.changeCooldown(sourceid, -reducingTime);
            chr.setKeyDownSkill_Time(0);
        }

        if (SkillFactory.getSkill(sourceid).isChargeSkill() && sourceid != 162121022) {
            chr.setKeyDownSkill_Time(System.currentTimeMillis());
        }

        if (effect.getCooldown(chr) > 0 && !chr.skillisCooling(sourceid) && GameConstants.isAfterCooltimeSkill(sourceid)) {
            chr.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, effect.getCooldown(chr)));
            chr.addCooldown(sourceid, System.currentTimeMillis(), effect.getCooldown(chr));
        }

        if (sourceid == 400041009) {
            int[] skills = new int[]{400041011, 400041012, 400041013, 400041014, 400041015};

            int jokerSkill = skills[Randomizer.nextInt(skills.length)];
            SkillFactory.getSkill(400041010).getEffect(1).applyTo(chr);
            chr.getMap().broadcastMessage(CField.EffectPacket.showEffect(chr, 0, sourceid, 2, 0, 0, (byte) 1, true, null, null, null));
            chr.getMap().broadcastMessage(CField.EffectPacket.showEffect(chr, 0, jokerSkill, 2, 0, 0, (byte) 1, true, null, null, null));
            if (jokerSkill == 400041015) {
                SkillFactory.getSkill(400041011).getEffect(level).applyTo(chr);
                SkillFactory.getSkill(400041012).getEffect(level).applyTo(chr);
                SkillFactory.getSkill(400041013).getEffect(level).applyTo(chr);
                SkillFactory.getSkill(400041014).getEffect(level).applyTo(chr);
            }
            SkillFactory.getSkill(jokerSkill).getEffect(level).applyTo(chr);
        }
        if (sourceid == 400011047) {
            int cout = chr.getDarknessAura() / 3;
            List<Integer> skills = new ArrayList<>();
            for (int i = 0; i < cout; i++) {
                skills.add(400011085);
            }

            if (cout <= 0) {
                cout = 1;
                skills.add(400011085);
            }

            chr.getClient().getSession().writeAndFlush(CField.rangeAttack(sourceid, skills, cout, chr.getTruePosition(), chr.isFacingLeft()));
            chr.setDarknessAura(0);
        }
    }

    public static final void NameChanger(final boolean isspcheck, final LittleEndianAccessor slea, final MapleClient c) {
        if (isspcheck) {
            final String secondPassword = slea.readMapleAsciiString();
            if (c.CheckSecondPassword(secondPassword)) {
                // 아이템 사용체크
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x09, 4034803));
            } else {
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x0A));
            }
        } else {
            int chrid = slea.readInt();
            byte status = slea.readByte();
            int itemuse = slea.readInt();
            String oriname = slea.readMapleAsciiString();
            String newname = slea.readMapleAsciiString();
            if (c.getPlayer().getId() != chrid) {
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x02));
                return;
            }
            if (itemuse != 4034803) {
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x02));
                return;
            }
            if (status != 1) {
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x02));
                return;
            }
            if (!c.getPlayer().getName().equals(oriname)) {
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x02));
                return;
            }
            if (c.getPlayer().haveItem(4034803, 1)) {
                if (MapleCharacterUtil.canCreateChar(newname)) {
                    if (MapleCharacterUtil.isEligibleCharNameTwo(newname, c.getPlayer().isGM()) && !LoginInformationProvider.getInstance().isForbiddenName(newname)) {
                        if (MapleCharacterUtil.getIdByName(newname) == -1) {
                            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4034803, 1, false, false);
                            c.getPlayer().setName(newname);
                            MapleCharacter.saveNameChange(newname, c.getPlayer().getId());
                            for (MapleUnion union : c.getPlayer().getUnions().getUnions()) {
                                if (union.getCharid() == c.getPlayer().getId()) {
                                    union.setName(newname);
                                }
                            }
                            c.getSession().writeAndFlush(CField.NameChanger((byte) 0x00));
                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                        } else {
                            c.getSession().writeAndFlush(CField.NameChanger((byte) 0x07));
                        }
                    } else {
                        c.getSession().writeAndFlush(CField.NameChanger((byte) 0x06));
                    }
                } else {
                    c.getSession().writeAndFlush(CField.NameChanger((byte) 0x02));
                }
            } else {
                c.getSession().writeAndFlush(CField.NameChanger((byte) 0x03));
            }
            //    && (!LoginInformationProvider.getInstance().isForbiddenName(newname))
            //         }) {

            // }
            //23 00 00 00 01 F3 90 3D 00 0C 00 BF A3 C1 A9 B8 AF B9 F6 BD BA C5 CD 08 00 A4 B1 A4 A4 A4 B7 A4 A9
        }
    }

    public static final void CancelMech(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        int sourceid = slea.readInt();
        int level = slea.readInt();
        if (sourceid % 10000 < 1000 && SkillFactory.getSkill(sourceid) == null) {
            sourceid += 1000; // ??
        }
        final Skill skill = SkillFactory.getSkill(sourceid);
        if (skill == null) { //not sure
            return;
        }
        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0);

            /*            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
             statups.put(MapleBuffStat.KeyDownMoving, new Pair<>(0, 0));
             chr.getClient().getSession().writeAndFlush(BuffPacket.cancelBuff(statups, chr));
             chr.getMap().broadcastMessage(chr, BuffPacket.cancelForeignBuff(chr, statups), false);*/
            chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
        } else {
            chr.cancelEffect(skill.getEffect(level), false, -1);
        }

        if (skill.getEffect(level).getCooldown(chr) > 0) {
            chr.getClient().getSession().writeAndFlush(CField.skillCooldown(sourceid, skill.getEffect(level).getCooldown(chr)));
            chr.addCooldown(sourceid, System.currentTimeMillis(), skill.getEffect(level).getCooldown(chr));
        }
    }

    public static final void SkillEffect(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int skillId = slea.readInt();
        final int level = slea.readInt();

        slea.skip(5);

        short display = slea.readByte();
        byte unk = slea.readByte();

        if (display == -1 && unk == -1) {
            slea.skip(1);
            display = slea.readByte();
            unk = slea.readByte();
        }

        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedSkill(skillId));
        if (chr == null || skill == null || chr.getMap() == null) {
            return;
        }

        final int skilllevel_serv = chr.getTotalSkillLevel(skill);

        MapleStatEffect eff = skill.getEffect(skilllevel_serv);
        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(level);

        if (skilllevel_serv > 0 && skilllevel_serv == level) {
            if (skill.isChargeSkill()) {
                chr.setKeyDownSkill_Time(System.currentTimeMillis());
            }
            if (skillId == 400041053) {
                SkillFactory.getSkill(400041010).getEffect(1).applyTo(chr);
            }
            if (skillId == 33101005) {
                chr.setLinkMid(slea.readInt(), 0);
            }
            if (skillId == 164121042) {
                chr.setKeyDownSkill_Time(System.currentTimeMillis());
            }
            if (skillId == 400041053) {
                SkillFactory.getSkill(400041010).getEffect(1).applyTo(chr);
            }
            //    chr.getMap().broadcastMessage(chr, CField.skillEffect(chr, skillId, skilllevel_serv, display, unk), false);
        }

        if ((skillId - 64001009) >= -2 && (skillId - 64001009) <= 2) {
            return;
        }

        if (skillId == 4341002) {
            effect.applyTo(chr, false);
            return;
        }

        if (skillId >= 3321034 && skillId <= 3321040 || skillId == 400041053) {
            effect.applyTo(chr);
        } else if (skillId != 2321001 && skillId != 3111013 && skillId != 5311002 && skillId != 11121052 && skillId != 14121003 && skillId != 400041009 && skillId != 22171083) {
            eff.applyTo(chr);
        }

        if (eff != null && eff.getCooldown(chr) > 0 && skillId != 3111013 && skillId != 22171083 && !GameConstants.isAfterCooltimeSkill(skillId)) {
            chr.addCooldown(skillId, System.currentTimeMillis(), eff.getCooldown(chr));
            chr.getClient().getSession().writeAndFlush(CField.skillCooldown(skillId, eff.getCooldown(chr)));
        }

        if (GameConstants.isKain(chr.getJob())) {
            if (skillId == 63121040) {
                int cooltime = effect.getCooltime() == 0 ? effect.getU() * 1000 : effect.getCooltime();
                MapleStatEffect effect1 = SkillFactory.getSkill(63121040).getEffect(chr.getSkillLevel(GameConstants.getLinkedSkill(63121040)));
                chr.KainsneakySnipingPre -= 1;
                chr.getClient().getSession().writeAndFlush(CField.KainStackSkill(63121040, chr.KainsneakySnipingPre, effect1.getW(), cooltime));
                chr.lastKainsneakySnipingPre = System.currentTimeMillis();
            } else if (skillId == 63121140) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.Possession);
            } else if (skillId == 400031064) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.IndieDamR, 400031062);
                chr.cancelEffectFromBuffStat(MapleBuffStat.DamR, 400031062);
                chr.cancelEffectFromBuffStat(MapleBuffStat.IndieStance, 400031062);
                chr.cancelEffectFromBuffStat(MapleBuffStat.ThanatosDescent, 400031062);
            }
        }

    }

    public static final void SpecialMove(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || slea.available() < 9) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        Point pos = slea.readPos();

        int skillid = slea.readInt();

        if (chr.isGM()) {
            chr.dropMessage(6, "SkillID : " + skillid + "  SpecialMove : " + slea);
        }

        if (skillid == 400051046) {
            chr.setUseTruthDoor(false);
        }
        switch (skillid) {
            case 400031066: {//그립오브애거니
                List<AdelProjectile> Agony = new ArrayList<>();
                MapleStatEffect agef = SkillFactory.getSkill(400031066).getEffect(chr.getSkillLevel(400031066));
                AdelProjectile ag = new AdelProjectile(0x12, c.getPlayer().getId(), 0, 400031066, 6000 + chr.AgonyCount * 1000, 0, -1, chr.getTruePosition(), new ArrayList<>());
                ag.setDelay(990);
                Agony.add(ag);
                c.getPlayer().getMap().spawnAdelProjectile(c.getPlayer(), Agony, false);
                c.getPlayer().AgonyCount = 0;
                c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.GripOfAgony);
                break;
            }
            case 63111009: {
                if (c.getPlayer().getBuffedValue(MapleBuffStat.RemainInsence) != null) { //비활성화시 결정 삭제
                    List<MapleMagicWreck> removes = new ArrayList<>();
                    for (MapleMapObject mo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 500000, Arrays.asList(MapleMapObjectType.WRECK))) {
                        MapleMagicWreck mw = (MapleMagicWreck) mo;
                        if (c.getPlayer() == mw.getChr() && mw.getSourceid() == 63111010) {
                            removes.add(mw);
                            c.getPlayer().getMap().removeMapObject(mo);
                            c.getPlayer().getMap().getWrecks().remove(mw);
                        }
                    }
                    if (removes.size() > 0) {
                        c.getPlayer().getMap().broadcastMessage(CField.removeMagicWreck(c.getPlayer(), removes));
                    }
                }
                break;
            }
            case 63121102: {
                chr.cancelEffectFromBuffStat(MapleBuffStat.Possession);
                break;
            }
            case 23111008:
                skillid += Randomizer.nextInt(3);
                break;
            case 5201012:
                skillid += Randomizer.nextInt(3);
                break;
            case 5221022:
                skillid += Randomizer.rand(1, 3);
                skillid -= 1000;
                break;
            case 5210015:
            case 5210016:
            case 5210017:
            case 5210018:
                skillid = Randomizer.rand(5210015, 5210018);
                SkillFactory.getSkill(skillid).getEffect(1).applyTo(chr, false);
                int killids = Randomizer.rand(5210015, 5210018);
                while (skillid == killids) {
                    killids = Randomizer.rand(5210015, 5210018);
                }
                SkillFactory.getSkill(killids).getEffect(1).applyTo(chr, false);
                chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
                return;
            case 61101002:
                if (chr.getSkillLevel(61120007) > 0) {
                    skillid = 61120007;
                } else if (chr.getBuffedValue(MapleBuffStat.Morph) != null) {
                    if (chr.getBuffSource(MapleBuffStat.Morph) == 61111008) {
                        skillid = 61110211;
                    } else {
                        skillid = 61121217;
                    }
                }
                break;
        }

        if (chr.isGM()) {
            // chr.dropMessage(6, "스킬 아이디 : " + skillid);
        }

        if (GameConstants.isZeroSkill(skillid)) {
            slea.skip(1);
        }

        if (skillid == 2321007) {
            if (chr.getParty() != null) {
                for (MapleCharacter cchr : chr.getPartyMembers()) {
                    if (!cchr.equals(chr)) {
                        if (cchr.isAlive()) {
                            cchr.addHP((long) (chr.getStat().getMaxHp() * 0.2));
                        }
                    }
                }
            }
        }

        if (GameConstants.isKinesis(chr.getJob())) {
            chr.givePPoint(skillid, skillid != 142121008);
        }

        int skillLevel = slea.readInt();

        final Skill skill = SkillFactory.getSkill(skillid);
        if (skill == null || (GameConstants.isAngel(skillid) && (chr.getStat().equippedSummon % 10000) != (skillid % 10000)) || (chr.inPVP() && skill.isPVPDisabled())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            System.out.println("세션클로즈 888");
            return;
        }
        if (skillid == 5321052) {
            slea.skip(13);
            byte rltype = slea.readByte();
            SkillFactory.getSkill(skillid).getEffect(skillLevel).applyTo(chr, chr.getTruePosition(), rltype);
            return;
        }
        if (skillid == 400041058) {
            List<AdelProjectile> atoms = new ArrayList<>();
            slea.readInt();
            slea.readInt();
            slea.readShort();
            slea.readInt();
            int mobcnt = slea.readInt();
            //  int bullet = 15 + chr.PhotonRay_plus;  // ? 이제 클라에서 쏘는거같음..
            for (int i = 0; i < mobcnt; i++) {
                int mobid = slea.readInt();
                int bullet = slea.readInt();
                for (int j = 0; j < bullet; j++) {
                    AdelProjectile sa = new AdelProjectile(9, chr.getId(), mobid, 400041058, 3600, 0, 1, chr.getPosition(), new ArrayList<>());
                    atoms.add(sa);
                }
            }
            if (!atoms.isEmpty()) {
                chr.getMap().spawnAdelProjectile(chr, atoms, false);
            }
            chr.cancelEffectFromBuffStat(MapleBuffStat.PhotonRay);
            return;
        }
        if (skillid == 400011136) {
            chr.setHuntingDecree(chr.getSummons().size());
        }

        if (chr.getTotalSkillLevel(GameConstants.getLinkedSkill(skillid)) <= 0 || chr.getTotalSkillLevel(GameConstants.getLinkedSkill(skillid)) != skillLevel) {
            if (!GameConstants.isMulungSkill(skillid) && !GameConstants.isPyramidSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedSkill(skillid)) <= 0 && !GameConstants.isAngel(skillid) && !GameConstants.isFusionSkill(skillid)) {

                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                System.out.println("세션클로즈 999 : " + skillid + " lv : " + chr.getTotalSkillLevel(GameConstants.getLinkedSkill(skillid)));
                return;
            }
        }

        skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedSkill(skillid));
        final MapleStatEffect effect = chr.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
        if (effect.isMPRecovery() && chr.getStat().getHp() < (chr.getStat().getMaxHp() / 100) * 10) { //less than 10% hp
            c.getPlayer().dropMessage(5, "You do not have the HP to use this skill.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        /*if (chr.getSkillLevel(80002632) > 0) {
         Item weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
         if (weapon_item != null) {
         // 무기가 null이 아닌지 체크
         String weapon_name = MapleItemInformationProvider.getInstance().getName(weapon_item.getItemId());
         if (weapon_name != null) {
         if (weapon_name.startsWith("제네시스 ")) { // 무기명이 제네시스로 시작하는지 체크
         if (!chr.skillisCooling(80002632)) { // 스킬 쿨이 아니라면
         MapleStatEffect effcts = SkillFactory.getSkill(80002632).getEffect(chr.getSkillLevel(80002632));
         effcts.applyTo(chr);
         }
         }
         }
         }
         }*/
        MapleStatEffect linkEffect = SkillFactory.getSkill(GameConstants.getLinkedSkill(skillid)).getEffect(skillLevel);

        if (linkEffect.getCooldown(chr) > 0 && !effect.ignoreCooldown(chr) && skillid != 400021122) {
            if (chr.skillisCooling(linkEffect.getSourceId()) && !GameConstants.isCooltimeKeyDownSkill(skillid) && !GameConstants.isNoApplySkill(skillid) && !chr.getBuffedValue(skillid) && skillid != 155001104 && skillid != 155001204 && chr.unstableMemorize != skillid && skillid != 162111002) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            } else if (skillid == 35111002) {
                List<Integer> count = new ArrayList<Integer>();

                Iterator<MapleSummon> summon = chr.getSummons().iterator();
                while (summon.hasNext()) {
                    MapleSummon s = summon.next();
                    if (s.getSkill() == skillid) {
                        count.add(s.getObjectId());
                    }
                }

                if (count.size() == 2) {
                    c.getSession().writeAndFlush(CField.skillCooldown(skillid, linkEffect.getCooldown(chr)));
                    chr.addCooldown(skillid, System.currentTimeMillis(), linkEffect.getCooldown(chr));
                }
            } else if (chr.getBuffedValue(20040219) || chr.getBuffedValue(20040220)) {
                if (skill.isHyper() || !GameConstants.isLuminous(skillid / 10000)) {
                    c.getSession().writeAndFlush(CField.skillCooldown(skillid, linkEffect.getCooldown(chr)));
                    chr.addCooldown(skillid, System.currentTimeMillis(), linkEffect.getCooldown(chr));
                }
            } else if (skillid == 25121133) {
                c.getSession().writeAndFlush(CField.skillCooldown(25121133, linkEffect.getCooldown(chr)));
                chr.addCooldown(25121133, System.currentTimeMillis(), linkEffect.getCooldown(chr));
                chr.addCooldown(skillid, System.currentTimeMillis(), linkEffect.getX() * 1000);
            } else if (chr.unstableMemorize == skillid) {
                chr.unstableMemorize = 0;
            } else if (!GameConstants.isAfterCooltimeSkill(skillid)) {
                c.getSession().writeAndFlush(CField.skillCooldown(skillid, linkEffect.getCooldown(chr)));
                chr.addCooldown(skillid, System.currentTimeMillis(), linkEffect.getCooldown(chr));
            }
        } else if (skillid == 400011001 && !chr.getBuffedValue(400011001)) {
            c.getSession().writeAndFlush(CField.skillCooldown(skillid, linkEffect.getX() * 1000));
        } else if (skillid == 63101104) {
            c.getSession().writeAndFlush(CField.skillCooldown(63101104, effect.getCooldown(chr)));
            chr.addCooldown(63101104, System.currentTimeMillis(), effect.getCooldown(chr));
        }

        if (skillid == 4221054) { //플립 더 코인
            c.getSession().writeAndFlush(CField.OnOffFlipTheCoin(false));
        } else if (skillid == 31211004) { //디아볼릭 리커버리
            chr.startDiabolicRecovery(effect);
        }

        if (GameConstants.isSoulSummonSkill(skillid)) {
            chr.useSoulSkill();
        }

        if (skillid == 36121007) {
            chr.getClient().getSession().writeAndFlush(CField.TimeCapsule());
            chr.setChair(3010587);
            chr.getMap().broadcastMessage(chr, CField.showChair(chr, 3010587), false);
        }

        AttackInfo ret = new AttackInfo();

        GameConstants.calcAttackPosition(slea, ret);

        int check = slea.readInt();

        if (skillid == 3101009) {
            if (chr.getBuffedValue(3101009)) {
                byte quiver = chr.getQuiverType();
                if (chr.getRestArrow()[quiver == 3 ? 0 : quiver] > 0) {
                    chr.setQuiverType((byte) (quiver == 3 ? 1 : quiver + 1));
                    effect.applyTo(chr, false);
                } else {
                    chr.dropMessage(6, "남은 화살촉이 없어서 바꿀 수 없습니다.");
                }
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                return;
            }
        } else if (skillid == 12101025) {
            pos = slea.readPos();
            c.getSession().writeAndFlush(CField.fireBlink(chr.getId(), pos));
        }
        chr.checkFollow();

        switch (skillid) {
            case 1211013: { // 위협
                pos = slea.readPos();
                chr.setPosition(pos);
                byte count = slea.readByte();

                MapleStatEffect bonusTime = null;
                if (chr.getSkillLevel(1220043) > 0) {
                    bonusTime = SkillFactory.getSkill(1220043).getEffect(chr.getSkillLevel(1220043));
                }

                MapleStatEffect bonusChance = null;
                if (chr.getSkillLevel(1220044) > 0) {
                    bonusChance = SkillFactory.getSkill(1220044).getEffect(chr.getSkillLevel(1220044));
                }

                MapleStatEffect enhance = null;
                if (chr.getSkillLevel(1220045) > 0) {
                    enhance = SkillFactory.getSkill(1220045).getEffect(chr.getSkillLevel(1220045));
                }

                MonsterStatus ms = null;
                MonsterStatusEffect mse = new MonsterStatusEffect(skillid, effect.getDuration() + (bonusTime != null ? bonusTime.getDuration() : 0));

                for (byte i = 0; i < count; ++i) {
                    MapleMonster monster = chr.getMap().getMonsterByOid(slea.readInt());
                    List<Triple<MonsterStatus, MonsterStatusEffect, Integer>> statusz = new ArrayList<>();

                    if (monster != null) {
                        ms = MonsterStatus.MS_Pad;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getX() + (enhance != null ? enhance.getX() : 0)));

                        ms = MonsterStatus.MS_Pdr;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getX() + (enhance != null ? enhance.getX() : 0)));

                        ms = MonsterStatus.MS_Mad;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getX() + (enhance != null ? enhance.getX() : 0)));

                        ms = MonsterStatus.MS_Mdr;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getX() + (enhance != null ? enhance.getX() : 0)));

                        ms = MonsterStatus.MS_Darkness;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getZ() + (enhance != null ? enhance.getY() : 0)));

                        Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();

                        for (Triple<MonsterStatus, MonsterStatusEffect, Integer> status : statusz) {
                            if (status.left != null && status.mid != null) {
                                //                            if (!mse.shouldCancel(System.currentTimeMillis())) {
                                if (Randomizer.isSuccess(effect.getProp() + (bonusChance != null ? bonusChance.getProp() : 0))) {
                                    status.mid.setValue(status.right);
                                    applys.put(status.left, status.mid);
                                }
                                //                            }
                            }
                        }
                        if (!monster.isBuffed(effect.getSourceId())) {
                            monster.applyStatus(c, applys, effect);
                        } else {
                            c.getPlayer().dropMessageGM(6, "몬스터디버프[3] 스킬" + effect.getSourceId() + " 은 이미 적용중이라 넘어감.");
                        }

                    }

                }

                effect.applyToBuff(chr);
                break;
            }
            //매직 크래쉬
            case 1121016:
            case 1221014:
            case 1321014: {
                pos = slea.readPos();
                chr.setPosition(pos);
                byte size = slea.readByte();
                for (byte i = 0; i < size; ++i) {
                    MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
                    if (mob != null) {
                        mob.applyStatus(c, MonsterStatus.MS_MagicCrash, new MonsterStatusEffect(effect.getSourceId(), effect.getDuration()), effect.getDuration(), effect);
                    }
                }
                effect.applyToBuff(chr);
                break;
            }
            case 2121052: { // 메기도 플레임
                byte count = slea.readByte();

                MapleAtom atom = new MapleAtom(false, chr.getId(), 3, true, 2121055, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (byte i = 0; i < count; ++i) {
                    monsters.add(slea.readInt());
                    atom.addForceAtom(new ForceAtom(2, Randomizer.rand(1, 17), 27, Randomizer.rand(40, 52), (short) 630));
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));
                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            //힐
            case 2301002: {
                pos = slea.readPos();
                // chr.setPosition(pos);
                byte size = slea.readByte();
                int healz = 0;
                for (byte i = 0; i < size; ++i) {
                    MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
                    if (mob != null) {
                        mob.applyStatus(c, MonsterStatus.MS_AdddamParty, new MonsterStatusEffect(effect.getSourceId(), effect.getDuration()), effect.getX(), effect);
                    }
                }

                effect.applyToBuff(chr);
                if (chr.getParty() != null) {
                    for (MaplePartyCharacter zz : chr.getParty().getMembers()) {
                        int ch = World.Find.findChannel(zz.getId());
                        if (ch > 0) {
                            MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(zz.getId());
                            if (player != null && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null && player.getId() != chr.getId() && player.isAlive() && effect.calculateBoundingBox(chr.getPosition(), true).contains(player.getPosition())) {
                                healz++;
                                effect.applyTo(chr, player);
                            }
                        }
                    }
                }

                c.getSession().writeAndFlush(CField.skillCooldown(skillid, Math.max(0, linkEffect.getCooldown(chr) - healz * 2000)));
                chr.addCooldown(skillid, System.currentTimeMillis(), Math.max(0, linkEffect.getCooldown(chr) - healz * 2000));
                break;
            }
            //엔젤레이
            case 2321007: {
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                /* if (chr.getParty() != null) { 엥 클라에서 자동처리인가?
                    for (MaplePartyCharacter zz : chr.getParty().getMembers()) {
                        int ch = World.Find.findChannel(zz.getId());
                        if (ch > 0) {
                            MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(zz.getId());
                            if (player != null && player.getBuffedEffect(MapleBuffStat.DebuffIncHp) == null && player.getId() != chr.getId() && player.isAlive() && effect.calculateBoundingBox(chr.getTruePosition(), chr.isFacingLeft()).contains(player.getTruePosition())) {
                       //         effect.applyTo(chr, player);
                            }
                        }
                    }
                }*/
                break;
            }
            //카디널 디스차지
            case 3011004:
            case 3300002:
            case 3321003: {
                slea.skip(4);
                byte size = slea.readByte();
                List<ForceAtom> atoms = new ArrayList<>();

                List<Integer> objectIds = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    objectIds.add(slea.readInt());
                }

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                MapleAtom atom = new MapleAtom(false, chr.getId(), 57, true, 3310004, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                if (chr.cardinalMark == 2 && !objectIds.isEmpty()) {
                    if (chr.getSkillLevel(3310004) > 0) {
                        MapleStatEffect editionalBlast = SkillFactory.getSkill(3310004).getEffect(chr.getSkillLevel(3310004));
                        if (editionalBlast.makeChanceResult()) {
                            for (byte i = 0; i < editionalBlast.getBulletCount(); ++i) {
                                MapleMapObject mob = null;
                                if (objectIds.size() > i) {
                                    mob = chr.getMap().getMonsterByOid(objectIds.get(i));
                                }
                                monsters.add(mob != null ? mob.getObjectId() : 0);
                                atom.addForceAtom(new ForceAtom(2, 0x2A, 4, 0, (short) 0x3C, mob != null ? mob.getTruePosition() : chr.getTruePosition()));
                            }

                            atom.setDwTargets(monsters);
                            chr.getMap().spawnMapleAtom(atom);
                        }
                    }
                }

                atom = new MapleAtom(false, chr.getId(), 56, true, skillid, chr.getTruePosition().x, chr.getTruePosition().y);
                monsters.clear();

                for (byte i = 0; i < objectIds.size(); ++i) {
                    monsters.add(objectIds.get(i));
                    atom.addForceAtom(new ForceAtom(2, 0x17, 0xA, Randomizer.rand(5, 15), (short) 0x3C));
                }

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);

                chr.giveRelikGauge(skillid, null);
                break;
            }
            case 4111009: // 스피릿 스로잉
            case 5201008: // 인피닛 불릿
            case 14111025: {
                int projectile = slea.readInt();
                if (!MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, effect.getBulletConsume(), false, true)) {
                    chr.dropMessage(5, "불릿이 부족합니다.");
                } else {
                    effect.applyTo(chr, projectile);
                }
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                break;
            }
            //메소 익스플로전
            case 4211006: {
                List<MapleMapObject> drops = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                        500000, Arrays.asList(MapleMapObjectType.ITEM));
                final List<MapleMapItem> allmesos = new ArrayList<>();
                int max = effect.getBulletCount();
                if (chr.getSkillLevel(4220045) > 0) {
                    max += SkillFactory.getSkill(4220045).getEffect(chr.getSkillLevel(4220045)).getBulletCount();
                }
                for (int i = 0; i < drops.size(); i++) { // 범위 내에 있는 1메소이며, 소유권이 자신에게 있는 메소
                    MapleMapItem drop = (MapleMapItem) drops.get(i);
                    if (drop.getMeso() == 1 && drop.getOwner() == c.getPlayer().getId() && allmesos.size() < max) {
                        allmesos.add(drop);
                    }
                }

                MapleAtom atom = new MapleAtom(false, chr.getId(), 12, true, 4210014, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                List<MapleMapObject> mobs = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                        500000, Arrays.asList(MapleMapObjectType.MONSTER));

                for (int i = 0; i < mobs.size(); i++) {
                    if (i < mobs.size() && i < allmesos.size() && monsters.size() < max) {
                        monsters.add(mobs.get(i).getObjectId());
                        atom.addForceAtom(new ForceAtom(1, 0x2A + (Randomizer.nextBoolean() ? 1 : 0), 4, Randomizer.rand(10, 65), (short) 300, allmesos.get(i).getTruePosition()));
                    }
                }

                for (int i = 0; i < allmesos.size(); i++) {
                    chr.getMap().broadcastMessage(CField.removeItemFromMap(allmesos.get(i).getObjectId(), 0, chr.getId()));
                    chr.getMap().removeMapObject(allmesos.get(i));
                    chr.setPickPocket(Math.max(0, chr.getPickPocket() - 1));
                }

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));
                if (monsters.isEmpty()) {
                    return;
                }

                MapleStatEffect eff = chr.getBuffedEffect(MapleBuffStat.PickPocket);

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);

                c.getSession().writeAndFlush(BuffPacket.giveBuff(eff.getStatups(), eff, chr));
                c.getSession().writeAndFlush(CField.EffectPacket.showEffect(chr, 0, 4211006, 2, 0, 0, (byte) 1, true, null, null, null));
                break;
            }
            case 4221052: { // 베일 오브 섀도우
                c.getSession().writeAndFlush(CWvsContext.onSkillUseResult(skillid));
                effect.applyTo(chr);
                break;
            }
            case 4331006: { // 사슬 지옥
                c.getSession().writeAndFlush(CWvsContext.onSkillUseResult(skillid));
                effect.applyTo(chr);
                break;
            }
            case 11111023: { // 트루 사이트  	
                pos = slea.readPos();
                chr.setPosition(pos);
                byte size = slea.readByte();
                for (byte i = 0; i < size; ++i) {
                    MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
                    Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();

                    int duration = effect.getDuration();

                    if (mob != null) {
                        if (chr.getSkillLevel(11120043) > 0) {
                            duration += SkillFactory.getSkill(11120043).getEffect(1).getDuration();
                        }

                        applys.put(MonsterStatus.MS_TrueSight, new MonsterStatusEffect(effect.getSourceId(), duration, effect.getV()));
                        applys.put(MonsterStatus.MS_AdddamSkill2, new MonsterStatusEffect(effect.getSourceId(), duration, effect.getS()));

                        if (chr.getSkillLevel(11120045) > 0) {
                            applys.put(MonsterStatus.MS_Pdr, new MonsterStatusEffect(effect.getSourceId(), duration, effect.getV() + SkillFactory.getSkill(11120045).getEffect(1).getW()));
                            applys.put(MonsterStatus.MS_Mdr, new MonsterStatusEffect(effect.getSourceId(), duration, effect.getV() + SkillFactory.getSkill(11120045).getEffect(1).getW()));
                        } else {
                            applys.put(MonsterStatus.MS_Pdr, new MonsterStatusEffect(effect.getSourceId(), duration, effect.getV()));
                            applys.put(MonsterStatus.MS_Mdr, new MonsterStatusEffect(effect.getSourceId(), duration, effect.getV()));
                        }
                        mob.applyStatus(c, applys, effect);
                    }
                }
                effect.applyToBuff(chr);
                break;
            }
            case 22110013: { // 스위프트 - 돌아와!
                byte size = slea.readByte();
                for (byte i = 0; i < size; ++i) {
                    MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
                    if (mob != null) {
                        mob.applyStatus(c, MonsterStatus.MS_Weakness, new MonsterStatusEffect(effect.getSourceId(), effect.getDuration()), effect.getX(), effect);
                    }
                }
                effect.applyToBuff(chr);
                break;
            }
            case 22141017: // 마법 잔해
            case 22170070: { // 강화된 마법 잔해
                List<MapleMagicWreck> removes = new ArrayList<>();
                for (MapleMagicWreck mw : chr.getMap().getWrecks()) {
                    if (mw.getChr().getId() == c.getPlayer().getId()) {
                        removes.add(mw);
                    }
                }
                for (MapleMagicWreck mw : removes) {
                    MapleAtom atom = new MapleAtom(false, chr.getId(), 24, true, skillid, mw.getPosition().x, mw.getPosition().y);
                    atom.addForceAtom(new ForceAtom(1, 0x28, 3, 0xC6, (short) 0, mw.getPosition()));
                    chr.getMap().spawnMapleAtom(atom);
                    chr.getMap().removeMapObject(mw);
                    chr.getMap().getWrecks().remove(mw);
                }
                c.getPlayer().getMap().broadcastMessage(CField.removeMagicWreck(chr, removes));
                break;
            }
            case 22170064: {
                byte size = slea.readByte();
                Skill mist = SkillFactory.getSkill(22170093);
                chr.getMap().removeMistByOwner(chr, 22170093);
                for (int i = 0; i < size; ++i) {
                    int objectId = slea.readInt();
                    MapleMonster mob = chr.getMap().getMonsterByOid(objectId);
                    if (mob != null) {
                        mist.getEffect(skillLevel).applyTo(chr, mob.getTruePosition());
                    }
                }
                break;
            }
            case 24121007: { // 소울 스틸
                pos = slea.readPos();
                byte size = slea.readByte();
                Map<MonsterStatus, MonsterStatusEffect> mses = new HashMap<>();
                for (int i = 0; i < size; ++i) {
                    MapleMonster life = chr.getMap().getMonsterByOid(slea.readInt());
                    if (life != null) {
                        if (life.isBuffed(MonsterStatus.MS_PImmune)) {
                            mses.put(MonsterStatus.MS_PImmune, life.getBuff(MonsterStatus.MS_PImmune));
                        }
                        if (life.isBuffed(MonsterStatus.MS_MImmune)) {
                            mses.put(MonsterStatus.MS_MImmune, life.getBuff(MonsterStatus.MS_MImmune));
                        }
                        if (life.isBuffed(MonsterStatus.MS_PCounter)) {
                            mses.put(MonsterStatus.MS_PCounter, life.getBuff(MonsterStatus.MS_PCounter));
                        }
                        if (life.isBuffed(MonsterStatus.MS_MCounter)) {
                            mses.put(MonsterStatus.MS_MCounter, life.getBuff(MonsterStatus.MS_MCounter));
                        }
                        if (mses.size() > 0) {
                            life.cancelStatus(mses);
                        }
                    }
                }
                effect.applyToBuff(chr);
                break;
            }
            case 25100002: { // 파력권
                effect.applyTo(chr);

                byte size = slea.readByte();
                for (byte i = 0; i < size; ++i) {
                    MapleMonster mob = chr.getMap().getMonsterByOid(slea.readInt());
                    Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();
                    if (mob != null) {
                        applys.put(MonsterStatus.MS_Speed, new MonsterStatusEffect(effect.getSourceId(), 5000, -linkEffect.getS()));

                        mob.applyStatus(c, applys, effect);
                    }
                }
                break;
            }
            case 400001011: {
                chr.cancelEffect(SkillFactory.getSkill(400001010).getEffect(c.getPlayer().getSkillLevel(400001010)), true, System.currentTimeMillis());
                chr.getMap().broadcastMessage(CField.bonusAttackRequest(400001011, Collections.EMPTY_LIST, true, 0));
                break;
            }
            case 31221001: { // 쉴드 체이싱
                pos = slea.readPos();
                chr.setPosition(pos);

                byte count = slea.readByte();

                MapleAtom atom = new MapleAtom(false, chr.getId(), 3, true, 31221014, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (byte i = 0; i < count; ++i) {
                    monsters.add(slea.readInt());
                    atom.addForceAtom(new ForceAtom(3, Randomizer.rand(10, 20), Randomizer.rand(20, 35), Randomizer.rand(50, 65), (short) 660));
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 30001061: { // 포획
                int objectId = slea.readInt();
                MapleMonster mob = chr.getMap().getMonsterByOid(objectId);
                if (mob != null) {
                    boolean success = mob.getId() >= 9304000 && mob.getId() <= 9304008;
                    c.getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, skillid, 1, 0, 0, (byte) (success ? 1 : 0), true, null, null, null));
                    chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, 0, skillid, 1, 0, 0, (byte) (success ? 1 : 0), false, null, null, null), chr.getTruePosition());
                    chr.getMap().broadcastMessage(MobPacket.catchMonster(mob.getObjectId(), (byte) (success ? 1 : 0)));
                    if (success) {
                        int jaguarid = GameConstants.getJaguarType(mob.getId());
                        String info = chr.getInfoQuest(23008);
                        for (int i = 0; i <= 8; ++i) {
                            if (info.contains(i + "=1")) { // 이미 있을 경우
                                continue; // 패스
                            } else if (i == jaguarid) {
                                info += i + "=1;";
                            }
                        }
                        chr.updateInfoQuest(23008, info);
                        chr.updateInfoQuest(123456, String.valueOf(jaguarid * 10)); // 마지막으로 사용한 포획
                        chr.getMap().killMonster(mob, chr, true, false, (byte) 1);
                        c.getSession().writeAndFlush(CWvsContext.updateJaguar(chr));
                    } else {
                        chr.dropMessage(5, "몬스터의 체력이 너무 많아 포획할 수 없습니다.");
                    }
                }
                break;
            }
            case 33001016: // 클로우 컷
            case 33001025: // 프로보크
            case 33101115: // 크로스 로드
            case 33111015: // 소닉 붐
            case 33121017: // 재규어 소울
            case 33121255: // 램피지 애즈 원
            {
                pos = slea.readPos();
                chr.setPosition(pos);
                c.getPlayer().getMap().broadcastMessage(CField.jaguarAttack(skillid));
                effect.applyTo(chr);
                break;
            }
            case 35101002:
            case 35110017:
            case 35120017: { // 호밍 미사일
                byte size = slea.readByte();

                int bulletCount = SkillFactory.getSkill(35101002).getEffect(skillLevel).getBulletCount();

                MapleAtom atom = new MapleAtom(false, chr.getId(), 20, true, skillid, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (byte i = 0; i < size; ++i) {
                    monsters.add(slea.readInt());
                    atom.addForceAtom(new ForceAtom(2, 50, Randomizer.rand(10, 15), Randomizer.rand(0, 25), (short) 500));
                }

                while (size < bulletCount) {
                    size++;
                    atom.addForceAtom(new ForceAtom(2, 50, Randomizer.rand(10, 15), Randomizer.rand(0, 25), (short) 500));
                }

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);

                if (chr.getBuffedEffect(MapleBuffStat.BombTime) != null) {
                    for (byte i = 0; i < chr.getBuffedEffect(MapleBuffStat.BombTime).getX(); ++i) {
                        atom.addForceAtom(new ForceAtom(2, 50, Randomizer.rand(10, 15), Randomizer.rand(0, 25), (short) 500));
                    }
                }
                if (chr.getBuffedEffect(400051041) != null) {
                    for (byte i = 0; i < chr.getBuffedEffect(400051041).getX(); ++i) {
                        atom.addForceAtom(new ForceAtom(2, 50, Randomizer.rand(10, 15), Randomizer.rand(0, 25), (short) 500));
                    }
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 65111100: { // 소울 시커
                pos = slea.readPos();
                chr.setPosition(pos);

                byte count = slea.readByte();

                MapleAtom atom = new MapleAtom(false, chr.getId(), 3, true, 65111007, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (byte i = 0; i < count; ++i) {
                    monsters.add(slea.readInt());
                    atom.addForceAtom(new ForceAtom(1, Randomizer.rand(10, 20), Randomizer.rand(40, 65), 0, (short) 500));
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 131001106: {
                effect.applyTo(chr);
                byte count = slea.readByte();

                MonsterStatus ms = null;
                MonsterStatusEffect mse = new MonsterStatusEffect(skillid, effect.getDuration());

                for (byte i = 0; i < count; ++i) {
                    MapleMonster monster = chr.getMap().getMonsterByOid(slea.readInt());
                    List<Triple<MonsterStatus, MonsterStatusEffect, Integer>> statusz = new ArrayList<>();

                    if (monster != null) {
                        ms = MonsterStatus.MS_Pdr;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getZ()));

                        ms = MonsterStatus.MS_Mdr;
                        statusz.add(new Triple<MonsterStatus, MonsterStatusEffect, Integer>(ms, mse, effect.getZ()));

                        Map<MonsterStatus, MonsterStatusEffect> applys = new HashMap<>();

                        for (Triple<MonsterStatus, MonsterStatusEffect, Integer> status : statusz) {
                            if (status.left != null && status.mid != null) {
                                //                            if (!mse.shouldCancel(System.currentTimeMillis())) {
                                if (Randomizer.isSuccess(effect.getProp())) {
                                    status.mid.setValue(status.right);
                                    applys.put(status.left, status.mid);
                                }
                                //                            }
                            }
                        }
                        if (!monster.isBuffed(effect.getSourceId())) {
                            monster.applyStatus(c, applys, effect);
                        } else {
                            c.getPlayer().dropMessageGM(6, "몬스터디버프[4] 스킬" + effect.getSourceId() + " 은 이미 적용중이라 넘어감.");
                        }
                    }
                }
                break;
            }
            case 400021001: { // 도트 퍼니셔
                int xMin = c.getPlayer().getPosition().x - 500;
                int xMax = c.getPlayer().getPosition().x + 500;
                int yMin = c.getPlayer().getPosition().y - 550;
                int yMax = c.getPlayer().getPosition().y - 150;

                List<MapleMapObject> mobs_objects = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 320000, Arrays.asList(MapleMapObjectType.MONSTER));
                int addball = 0;
                int moid = 0;
                for (int i = 0; i < mobs_objects.size(); i++) {
                    MapleMonster mob = (MapleMonster) mobs_objects.get(i);
                    moid = mob.getObjectId();
                    Iterator<Ignition> lp = mob.getIgnitions().iterator();//.getStati().entrySet().iterator();
                    boolean moab = false;
                    while (lp.hasNext()) {
                        Ignition zz = lp.next();
                        if (zz.getOwnerId() == c.getPlayer().getId()) {
                            if (!moab) {
                                moab = true;
                                addball++;
                            }
                        }
                    }
                }

                MapleAtom atom = new MapleAtom(false, chr.getId(), 28, true, 400021001, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (int i = 0; i < 15 + addball; i++) {
                    monsters.add(0);
                    ForceAtom atoms = new ForceAtom(moid, Randomizer.rand(41, 44), Randomizer.rand(3, 4), Randomizer.rand(0, 360), (short) 720, new Point(Randomizer.rand(xMin, xMax), Randomizer.rand(yMin, yMax)));
                    List<Integer> mobid = new ArrayList<>();
                    mobid.add(moid);
                    atom.setDwTargets(mobid);
                    atom.addForceAtom(atoms);
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 400021030: { // 썬더 브레이크
                MapleStatEffect sub = SkillFactory.getSkill(400021031).getEffect(skillLevel);
                //pos = slea.readPos();
                //chr.setPosition(pos);
                slea.skip(5);
                int size = slea.readInt();
                List<Point> posz = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    Point poss = new Point(slea.readInt(), slea.readInt());
                    posz.add(poss);
                }

                int i = 737;
                int zz = -2310;
                for (Point poss : posz) {
                    i += 350;
                    MapleMist mist = new MapleMist(sub.calculateBoundingBox(poss, chr.isFacingLeft()), chr, sub, zz, (byte) (chr.isFacingLeft() ? 1 : 0));
                    mist.setDelay(0x1A);
                    mist.setPosition(poss);
                    mist.setEndTime(i);
                    zz += 350;
                    chr.getMap().spawnMist(mist, false);
                }

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                break;
            }
            case 36111008: { // 엑스트라 서플라이
                c.getPlayer().gainXenonSurplus((short) 10, SkillFactory.getSkill(30020232));
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                break;
            }
            case 151001001: { // 샤드
                slea.skip(4);
                byte size = slea.readByte();
                List<Integer> objects = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    int objectId = slea.readInt();
                    objects.add(objectId);
                }
                slea.skip(3);
                pos = new Point(slea.readInt(), slea.readInt());
                effect.applyTo(chr, pos);

                List<AdelProjectile> shards = new ArrayList<>();
                for (int i = 0; i < 5; ++i) {
                    AdelProjectile shard = new AdelProjectile(0, chr.getId(), 0, 151001001, 5000, 0, 1, new Point(pos.x + (-30 * (i - 2)), pos.y - 100), new ArrayList<>());
                    shard.setDelay(1000);
                    shards.add(shard);
                }

                chr.getMap().spawnAdelProjectile(chr, shards, false);
                break;
            }
            case 151111002: { // 게더링
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.onSkillUseResult(0));
                break;
            }
            case 151101003: { // 레조넌스
                int objectId = slea.readInt();
                MapleSummon summon = chr.getMap().getSummonByOid(objectId);
                if (summon != null) {
                    summon.removeSummon(chr.getMap(), false);
                }
                SkillFactory.getSkill(151101010).getEffect(skillLevel).applyTo(chr, false);
                break;
            }
            case 151101004: { // 레조넌스 임페일 연계시
                SkillFactory.getSkill(151101010).getEffect(skillLevel).applyTo(chr, false);
                break;
            }
            case 151111003: { // 오더
                byte size = slea.readByte();
                List<Integer> objects = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    int objectId = slea.readInt();
                    objects.add(objectId);
                }

                slea.skip(3);

                pos = new Point(slea.readInt(), slea.readInt());
                effect.applyTo(chr, pos);

                List<Integer> points = new ArrayList<Integer>() {
                    {
                        for (int a = 0; a < 3; a++) {
                            add(-135 - (a * 15));
                        }
                    }
                };

                List<AdelProjectile> swords = new ArrayList<>();
                swords.add(new AdelProjectile(7, chr.getId(), objects.isEmpty() ? 0 : objects.get(Randomizer.nextInt(objects.size())), skillid, effect.getX() * 1000, -135, 100, new Point(pos.x - 24, pos.y - 16), points));

                points = new ArrayList<Integer>() {
                    {
                        for (int a = 0; a < 3; a++) {
                            add(135 + (a * 15));
                        }
                    }
                };

                swords.add(new AdelProjectile(7, chr.getId(), objects.isEmpty() ? 0 : objects.get(Randomizer.nextInt(objects.size())), skillid, effect.getX() * 1000, -135, 100, new Point(pos.x + 28, pos.y - 16), points));
                chr.getMap().spawnAdelProjectile(chr, swords, false);
                chr.decreaseOrderEtherGauge();
                break;
            }
            case 151121041: { // 마커
                effect.applyTo(chr);
                for (Point point : ret.mistPoints) {
                    effect.applyTo(chr, point, false, 1020);
                    if (chr.getSkillLevel(151100002) > 0) {
                        SkillFactory.getSkill(151100002).getEffect(chr.getSkillLevel(151100002)).applyTo(chr, point, false);
                    }
                }
                break;
            }
            case 152001001:
            case 152120001: {
                pos = slea.readPos();
                slea.skip(7);

                MapleAtom atom = new MapleAtom(false, chr.getId(), 36, true, skillid, chr.getTruePosition().x, chr.getTruePosition().y);
                atom.setDwFirstTargetId(0);
                atom.addForceAtom(new ForceAtom(2, 50, 50, 0, 470));
                atom.setDwUnknownPoint(slea.readInt());
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 152101004: {
                slea.skip(5);

                MapleSummon summon = chr.getSummon(152101000);

                if (summon != null) {
                    chr.CrystalCharge = summon.getEnergy();
                }

                SkillFactory.getSkill(152101000).getEffect(1).applyTo(chr, slea.readPos());
                break;
            }
            case 152110004: { // 강화 자벨린
                slea.skip(9);

                MapleAtom atom = new MapleAtom(false, chr.getId(), 37, true, 152110004, chr.getTruePosition().x, chr.getTruePosition().y);
                atom.setDwFirstTargetId(0);
                atom.addForceAtom(new ForceAtom(1, 46, 60, 7, 300));
                atom.setDwUnknownPoint(slea.readInt());
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                chr.getMap().spawnMapleAtom(atom);

                atom = new MapleAtom(false, chr.getId(), 38, true, 152120016, chr.getTruePosition().x, chr.getTruePosition().y);

                /*                List<Integer> monsters = new ArrayList<>();
                
                 final List<MapleMapObject> mobjects = c.getPlayer().getMap().getMapObjectsInRange(
                 c.getPlayer().getPosition(), 320000, Arrays.asList(MapleMapObjectType.MONSTER));
                 int randmob_count = Math.min(3, mobjects.size());
                 for (int i = 0; i < randmob_count; i++) {
                 final int randmob_remove = Randomizer.rand(0, mobjects.size() - 1);
                 monsters.add(mobjects.get(randmob_remove).getObjectId());
                 atom.addForceAtom(new ForceAtom(2, 0x29, 6, 180, (short) 450));
                 }
                
                 atom.setDwTargets(monsters);*/
                chr.getMap().spawnMapleAtom(atom);

                break;
            }
            case 155001103: { // 스펠 불릿
                int size = slea.readByte();
                List<ForceAtom> atoms = new ArrayList<>();
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                chr.setArcSpell(0);

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                for (int i = 0; i < size; ++i) {
                    atoms.clear();
                    int mobId = slea.readInt();
                    MapleMonster mob = chr.getMap().getMonsterByOid(mobId);
                    if (mob != null) {

                        MapleAtom atom;
                        ForceAtom forceAtom = new ForceAtom(0, Randomizer.rand(0xA, 0x14), Randomizer.rand(0x5, 0xA), Randomizer.rand(0x4, 0x12D), (short) 0);
                        if (chr.getArcSpellSkills().contains(155101100)) {
                            atom = new MapleAtom(false, chr.getId(), 44, true, 155101002, chr.getTruePosition().x, chr.getTruePosition().y);
                            atom.addForceAtom(forceAtom);
                            for (int j = 0; i < chr.getArcSpellSkills().size(); ++i) {
                                if (chr.getArcSpellSkills().get(j) == 155101100) {
                                    chr.getArcSpellSkills().remove(j);
                                }
                            }
                        } else if (chr.getArcSpellSkills().contains(155111102)) {
                            atom = new MapleAtom(false, chr.getId(), 45, true, 155111003, chr.getTruePosition().x, chr.getTruePosition().y);
                            atom.addForceAtom(forceAtom);
                            for (int j = 0; i < chr.getArcSpellSkills().size(); ++i) {
                                if (chr.getArcSpellSkills().get(j) == 155111102) {
                                    chr.getArcSpellSkills().remove(j);
                                }
                            }
                        } else if (chr.getArcSpellSkills().contains(155121102)) {
                            atom = new MapleAtom(false, chr.getId(), 46, true, 155121003, chr.getTruePosition().x, chr.getTruePosition().y);
                            atom.addForceAtom(forceAtom);
                            for (int j = 0; i < chr.getArcSpellSkills().size(); ++i) {
                                if (chr.getArcSpellSkills().get(j) == 155121102) {
                                    chr.getArcSpellSkills().remove(j);
                                }
                            }
                        } else {
                            atom = new MapleAtom(false, chr.getId(), 43, true, 155001000, chr.getTruePosition().x, chr.getTruePosition().y);
                            atom.addForceAtom(forceAtom);
                        }
                        atom.setDwFirstTargetId(mobId);
                        chr.getMap().spawnMapleAtom(atom);
                    }
                }
                chr.getArcSpellSkills().clear();

                statups.put(MapleBuffStat.ScarletBuff, new Pair<>(1, 0));
                statups.put(MapleBuffStat.GustBuff, new Pair<>(1, 0));
                statups.put(MapleBuffStat.AbyssBuff, new Pair<>(1, 0));
                statups.put(MapleBuffStat.PlainBuff, new Pair<>(0, 0));
                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
                break;
            }
            case 155111207: { // 돌아오는 증오
                List<MapleMagicWreck> removes = new ArrayList<>();
                slea.skip(3);
                int size = slea.readInt();
                MapleAtom a = new MapleAtom(false, chr.getId(), 48, true, 155111207, chr.getTruePosition().x, chr.getTruePosition().y);
                for (int i = 0; i < size; i++) {
                    int oid = slea.readInt();
                    MapleMagicWreck wreck = (MapleMagicWreck) chr.getMap().getMapObject(oid, MapleMapObjectType.WRECK);
                    if (wreck != null) {
                        chr.getMap().getWrecks().remove(wreck);
                        a.addForceAtom(new ForceAtom(0, Randomizer.rand(0xA, 0x14), Randomizer.rand(0x5, 0xA), Randomizer.rand(0x4, 0x12D), (short) 0, wreck.getTruePosition()));
                    }
                }
                a.setDwUserOwner(chr.getId());
                chr.getMap().spawnMapleAtom(a);
                c.getPlayer().getMap().broadcastMessage(CField.removeMagicWreck(chr, removes));
                effect.applyTo(chr);
                break;
            }
            case 160001075:
            case 160011075: { // 형상변이
                boolean use = false;
                if (c.getPlayer().getKeyValue(7786, "sw") == 0) {
                    use = true;
                    c.getPlayer().setKeyValue(7786, "sw", "1");
                } else {
                    c.getPlayer().setKeyValue(7786, "sw", "0");
                }
                chr.getMap().broadcastMessage(CField.updateShapeShift(chr.getId(), use));
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));
                chr.equipChanged();
                break;
            }
            case 400021044: { // 플레임 디스차지
                byte count = slea.readByte();

                MapleAtom atom = new MapleAtom(false, chr.getId(), 3, true, 400021045, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (byte i = 0; i < count; ++i) {
                    monsters.add(slea.readInt());
                    atom.addForceAtom(new ForceAtom(6, 15, 0x22, 30, (short) 720));
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                atom.setDwTargets(monsters);
                chr.setIgnition(0);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 400021068: {
                chr.setBHGCCount(chr.getBHGCCount() - 1);
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.getBHGCCount(), 0));

                c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(skillid).getEffect(skillLevel), chr));

                effect.applyTo(chr);
                break;
            }
            case 400031000: { // 가이디드 애로우

                MapleAtom atom = new MapleAtom(false, chr.getId(), 27, true, 400031000, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

//                for (int i = 0; i < effect.getX(); i++) {
                monsters.add(0);
                ForceAtom forceAtom = new ForceAtom(1, 40, 3, 90, 840);
                atom.addForceAtom(forceAtom);
                //              }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                //     atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 400031022: { // 아이들 웜
                MapleAtom atom = new MapleAtom(false, chr.getId(), 34, true, 400031022, chr.getTruePosition().x, chr.getTruePosition().y);
                List<Integer> monsters = new ArrayList<>();

                for (int i = 0; i < effect.getX(); i++) {
                    monsters.add(0);
                    atom.addForceAtom(new ForceAtom(Randomizer.nextBoolean() ? 1 : 3, Randomizer.rand(30, 60), 10, Randomizer.nextBoolean() ? Randomizer.rand(0, 5) : Randomizer.rand(180, 185), (short) 720, chr.getTruePosition()));
                }

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                atom.setDwTargets(monsters);
                chr.getMap().spawnMapleAtom(atom);
                break;
            }
            case 400041000: { // 베놈 버스트
                List<MapleMapObject> mobs_objects = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 320000, Arrays.asList(MapleMapObjectType.MONSTER));
                final List<MapleMonster> allmobs = new ArrayList<>();
                for (int i = 0; i < mobs_objects.size(); i++) {
                    MapleMonster mob1 = (MapleMonster) mobs_objects.get(i);
//                    mob1.applyStatus(c, MonsterStatus.MS_Burned, new MonsterStatusEffect(skillid, 10000), (int) Randomizer.rand(100, 500), effect);
                    if (mob1.isBuffed(MonsterStatus.MS_Burned) || mob1.isBuffed(MonsterStatus.MS_Poison)) {
                        allmobs.add(mob1);
                    }
                }
                final int maxmob_count = Math.min(effect.getMobCount(), allmobs.size());
                final List<MapleMonster> mobs = new ArrayList<>();
                for (int i = 0; i < maxmob_count; i++) {
                    final int randmob_remove = Randomizer.rand(0, allmobs.size() - 1);
                    mobs.add(allmobs.get(randmob_remove));
                    allmobs.remove(randmob_remove);
                }

                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                if (mobs.isEmpty()) {
                    return;
                }
                List<Triple<Integer, Integer, Integer>> finalMobList = new ArrayList<>();
                int test = 0;
                for (MapleMonster mob1 : mobs) {
                    finalMobList.add(new Triple<Integer, Integer, Integer>(mob1.getObjectId(), 0, test));
                    test++;
                }
                c.getSession().writeAndFlush(CField.bonusAttackRequest(400041030, finalMobList, false, 0));

                /*                 MapleWorldMapItem remove;
                 for (int i = 0; i < mesos.size(); i++) {
                 remove = (MapleWorldMapItem) c.getPlayer().getMap().getMapObject(mesos.get(i).left);
                 c.getPlayer().getMap().removeMapObject(remove);
                 c.getPlayer().getMap().broadcastMessage(MainPacketCreator.removeItemFromMap(remove.getObjectId(), 0, c.getPlayer().getId()));
                 }
                 c.getPlayer().getMap().broadcastMessage(MainPacketCreator.giveMesoExplosion(c.getPlayer().getId(), moids, mesos));
                 c.getSession().writeAndFlush(MainPacketCreator.resetActions());
                 */
                break;
            }
            case 400041021: { // 카르마 퓨리
                final int izz = skillid;
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.onSkillUseResult(izz));
                break;
            }
            case 400031044: {
                effect.applyTo(chr);
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.IndieNotDamaged, new Pair<>(1, 2600));
                c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, c.getPlayer()));
                break;
            }
            case 400051074: {
                chr.FullMakerSize = 20;
                chr.setStartFullMakerTime(System.currentTimeMillis());
                c.getSession().writeAndFlush(CField.StartFullMaker(chr.FullMakerSize, 20000));
                break;
            }
            case 400041022: { // 블랙잭
                byte size = slea.readByte();
                chr.useBlackJack = false;
                MapleAtom atom = new MapleAtom(false, chr.getId(), 33, true, 400041023, chr.getTruePosition().x, chr.getTruePosition().y);

                for (int i = 0; i < size; i++) {
                    atom.setDwFirstTargetId(slea.readInt()); // what
                    atom.addForceAtom(new ForceAtom(32, 31, 21, 144, (short) 760));
                }
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));

                chr.getMap().spawnMapleAtom(atom);

                break;
            }
            case 400011131: {
                MapleAtom atom = new MapleAtom(false, chr.getId(), 67, true, 400011131, chr.getTruePosition().x, chr.getTruePosition().y);
                ForceAtom forceAtom = new ForceAtom(0x01, 0x5A, 0x28, 360, (short) 420);
                atom.addForceAtom(forceAtom);
                effect.applyTo(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));
                chr.getMap().spawnMapleAtom(atom);

                chr.setMjollnir(chr.getMjollnir() - 1);
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.getMjollnir(), 0));

                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, SkillFactory.getSkill(skillid).getEffect(skillLevel), chr));
                break;
            }
            case 400041024: { // 블랙잭 즉시 폭발
                chr.useBlackJack = true;
                c.getPlayer().getClient().getSession().writeAndFlush(CField.blackJack(slea.readPos()));
                break;
            }
            case 400051025: { // ICBM
                pos = new Point(slea.readInt(), slea.readInt());
                c.getPlayer().getMap().broadcastMessage(CField.ICBM(false, skillid, effect.calculateBoundingBox(pos, c.getPlayer().isFacingLeft())));
                effect.applyTo(chr, pos);
                break;
            }
            case 400031047:
            case 400031049:
            case 400031051: {
                effect.applyTo(chr);
                return;
            }
            case 400001021: { // 언스태이블 메모라이즈
                List<Integer> skills = new ArrayList<>();
                for (Entry<Skill, SkillEntry> skillz : chr.getSkills().entrySet()) {
                    if (!skillz.getKey().isHyper() && !skillz.getKey().isVMatrix()
                            && ((GameConstants.isFPMage(skillz.getKey().getId() / 10000) && GameConstants.isFPMage(chr.getJob()))
                            || (GameConstants.isILMage(skillz.getKey().getId() / 10000) && GameConstants.isILMage(chr.getJob()))
                            || (GameConstants.isBishop(skillz.getKey().getId() / 10000) && GameConstants.isBishop(chr.getJob()))
                            || GameConstants.isDefaultMagician(skillz.getKey().getId() / 10000))) {
                        if (skillz.getKey().getEffect(1) != null) {
                            MapleStatEffect randEff = skillz.getKey().getEffect(1);
                            if (randEff.getDamage() > 0 || randEff.getDuration() > 0) {
                                skills.add(skillz.getKey().getId());
                            }
                        }
                    }
                }

                if (skills.size() > 0) {
                    int nextSkill = skills.get(Randomizer.nextInt(skills.size()));
                    chr.unstableMemorize = nextSkill;
                    c.getSession().writeAndFlush(CField.unstableMemorize(nextSkill));
                }
                effect.applyToBuff(chr);
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr, true, false));
                break;
            }
            case 400011038: { // 블러드 피스트
                effect.applyToBuff(chr);
                //    c.getPlayer().cancelEffect(effect, false, -1);
                break;
            }
            case 63101104: { //발현 스캐터링 샷
                c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Possession);
                byte size = slea.readByte();
                MapleMonster target = null;
                for (int i = 0; i < size; ++i) {
                    int objectId = slea.readInt();
                    MapleMonster monster = chr.getMap().getMonsterByOid(objectId);
                    if (target == null) {
                        target = monster;
                    }
                    if (monster.getMobMaxHp() > target.getMobMaxHp()) {
                        target = monster;
                    }
                }

                slea.skip(3);
                pos = new Point(slea.readInt(), slea.readInt());
                List<AdelProjectile> swords = new ArrayList<>();

                int left = -45;
                int right = 45;
                for (int i = 0; i < 6; i++) {
                    swords.add(new AdelProjectile(0x10, chr.getId(), target.isAlive() ? target.getObjectId() : 0, 63101104, 2400, chr.isFacingLeft() ? left : right, 1, chr.getPosition(), new ArrayList<>()));
                    left -= 15;
                    right += 15;
                }
                chr.getMap().spawnAdelProjectile(chr, swords, false);
                // chr.getClient().getSession().writeAndFlush(CField.skillCooldown(63100104,effect.getCooltime()));
                //   chr.addCooldown(63101104,System.currentTimeMillis(),effect.getCooltime());

                break;
            }
            case 162101011:
            case 162121019: {
                byte size = slea.readByte();
                List<Integer> mobs = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    mobs.add(slea.readInt());
                }

                slea.skip(3);
                int x = slea.readInt();
                int y = slea.readInt();

                List<Integer> points = new ArrayList<>();
                points.add(x);
                points.add(y);

                List<AdelProjectile> tiles = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    AdelProjectile tile = new AdelProjectile(0x15, chr.getId(), i < mobs.size() ? mobs.get(i) : 0, skillid, 4000, 0, 1, new Point(x + Randomizer.rand(-340, 340), y + Randomizer.rand(-420, 20)), points);
                    tile.setDelay(750);
                    tiles.add(tile);
                }
                chr.getMap().spawnAdelProjectile(chr, tiles, false);
                break;
            }
            case 162111005: {
                byte size = slea.readByte();
                List<Integer> mobs = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    mobs.add(slea.readInt());
                }

                slea.skip(3);
                int x = slea.readInt();
                int y = slea.readInt();

                List<Integer> points = new ArrayList<>();
                points.add(x);
                points.add(y);

                int tileSize = 7;
                if (tileSize < mobs.size())
                    tileSize = mobs.size();

                List<AdelProjectile> tiles = new ArrayList<>();
                for (int i = 0; i < tileSize; i++) {

                    int index = i;
                    if (mobs.size() > 0)
                        index %= mobs.size();
                    AdelProjectile tile = new AdelProjectile(0x19, chr.getId(), index < mobs.size() ? mobs.get(index) : 0, 162111005, 4000, 0, 1, new Point(x + Randomizer.rand(-495, 5), y + Randomizer.rand(-270, 60)), points);
                    tile.setCreateDelay(600);
                    tile.setDelay(1140);
                    tiles.add(tile);
                }

                chr.getMap().spawnAdelProjectile(chr, tiles, false);
                break;
            }
            case 162121010: {
                byte size = slea.readByte();
                List<Integer> mobs = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    mobs.add(slea.readInt());
                }

                slea.skip(3);
                int x = slea.readInt();
                int y = slea.readInt();
                byte direction = slea.readByte();

                List<Integer> points = new ArrayList<>();
                points.add(x);
                points.add(y);

                List<AdelProjectile> tiles = new ArrayList<>();

                // todo wz에서 캐싱해서 사용해야함
                int[] rotate = {100, 20, 40, 60, 80};
                Point[] plusPos = {new Point(-55, -90), new Point(-60, -85), new Point(-65, -80), new Point(-55, -90), new Point(-60, -80)};
                int enableDelay = 90;
                int baseDelay = effect.getW();
                int plusDelay = effect.getU();

                List<Integer> order = Arrays.asList(0, 1, 2, 3, 4);
                Collections.shuffle(order);

                for (int i = 0; i < 5; i++) {
                    int index = i;
                    if (mobs.size() > 0 && mobs.size() < 5)
                        index %= mobs.size();

                    int rand = order.get(i);
                    AdelProjectile tile = new AdelProjectile(0x17, chr.getId(), index < mobs.size() ? mobs.get(index) : 0, 162121010, 4000, rotate[rand], 1, new Point(x + plusPos[rand].x, y + plusPos[rand].y), points);

                    int delay = baseDelay + plusDelay * i;
                    tile.setCreateDelay(delay);
                    tile.setDelay(delay + enableDelay);
                    tiles.add(tile);
                }

                chr.getMap().spawnAdelProjectile(chr, tiles, false);
                c.getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, skillid, 1, 0, 0, direction, true, ret.plusPosition2, null, null));
                chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, 0, skillid, 1, 0, 0, direction, false, ret.plusPosition2, null, null), chr.getTruePosition());
                break;
            }
            case 400021122: {
                byte size = slea.readByte();
                List<Integer> mobs = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    mobs.add(slea.readInt());
                }

                slea.skip(3);
                int x = slea.readInt();
                int y = slea.readInt();

                int type = ret.unk;
                if (type == 4)
                    type--;
                List<AdelProjectile> tiles = new ArrayList<>();
                for (int i = 0; i < 7; i++) {

                    int index = i;
                    if (mobs.size() > 0 && mobs.size() < 7)
                        index %= mobs.size();

                    AdelProjectile tile = new AdelProjectile(0x17 + type, chr.getId(), index < mobs.size() ? mobs.get(index) : 0, 400021122, 4800, 0, 1, new Point(x + Randomizer.rand(-160, 160), y + Randomizer.rand(-450, -110)), new ArrayList<>());
                    tile.setCreateDelay(660);
                    tile.setDelay(1320);
                    tile.setIdk2(type);
                    tiles.add(tile);
                }

                chr.getMap().spawnAdelProjectile(chr, tiles, false);
                if (ret.usedCount > 0) {
                    int cooltime = effect.getCooldown(chr);
                    cooltime -= (5 - ret.usedCount) * 12 * 1000;
                    c.getSession().writeAndFlush(CField.skillCooldown(skillid, cooltime));
                    chr.addCooldown(skillid, System.currentTimeMillis(), cooltime);
                }
                break;
            }
            case 400021123: {
                List<RangeAttack> list = new ArrayList<>();

                list.add(new RangeAttack(400021128, ret.plusPosition2, (short) chr.getFacingDirection(), 5850, 8));
                list.add(new RangeAttack(400021127, ret.plusPosition2, (short) chr.getFacingDirection(), 3150, 4));
                list.add(new RangeAttack(400021126, ret.plusPosition2, (short) chr.getFacingDirection(), 2250, 4));
                list.add(new RangeAttack(400021125, ret.plusPosition2, (short) chr.getFacingDirection(), 1560, 4));
                list.add(new RangeAttack(400021124, ret.plusPosition2, (short) chr.getFacingDirection(), 630, 4));

                c.getSession().writeAndFlush(CField.rangeAttack(skillid, list));
                break;
            }
            case 162121042: {
                List<Integer> points = new ArrayList<>();
                points.add(8);
                points.add(ret.unk);

                List<AdelProjectile> tiles = new ArrayList<>();

                AdelProjectile tile = new AdelProjectile(0x14, chr.getId(), 0, 162101000, 0, 0, 1, ret.plusPosition2, points);
                tile.setCreateDelay(540);
                tile.setIdk2(ret.usedCount);
                tiles.add(tile);

                chr.getMap().spawnAdelProjectile(chr, tiles, false);
                effect.applyTo(c.getPlayer(), true);
                break;
            }
            case 162111002: {

                List<AdelProjectile> tiles = new ArrayList<>();
                List<Integer> points = new ArrayList<>();
                points.add(ret.unk);
                AdelProjectile tile;
                if (check > 0) { // 두번째
                    tile = new AdelProjectile(0x20, chr.getId(), 0, 162111002, effect.getS2() * 1000, 0, 1, ret.plusPosition2, points);
                    effect.applyTo(c.getPlayer(), false);
                } else { // 첫번째
                    tile = new AdelProjectile(0x1F, chr.getId(), 0, 162111002, effect.getS() * 1000, 0, 1, ret.plusPosition2, points);
                    effect.applyTo(c.getPlayer(), true);
                }
                tiles.add(tile);
                chr.getMap().spawnAdelProjectile(chr, tiles, false);
                break;
            }
            case 162101001: {
                if (chr.getKeyValue(1544, String.valueOf(162101012)) == 1) {
                    SkillFactory.getSkill(162110007).getEffect(chr.getSkillLevel(162110007)).applyTo(chr, true);
                }
                break;
            }
            default:
                if (skillid == 64001012) {
                    slea.skip(3);
                    byte direction = slea.readByte();
                    pos = new Point(slea.readInt(), slea.readInt());
                    int oldskillid = slea.readInt();
                    chr.getClient().getSession().writeAndFlush(EffectPacket.showEffect(chr, oldskillid, skillid, 1, 0, 0, direction, true, pos, null, null));
                    chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, oldskillid, skillid, 1, 0, 0, direction, false, pos, null, null), false);
                    break;
                }
                if (skillid == 400041041) {
                    for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
                        if (mist.getSourceSkill().getId() == 400041041) {
                            chr.getMap().removeMistByOwner(chr, 400041041);
                            break;
                        }
                    }

                    effect.applyTo(c.getPlayer(), ret.plusPosition2, ret.rlType);
                    break;
                }
                if (skillid == 162101009) {
                    for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
                        if (mist.getSourceSkill().getId() == 162101010) {
                            chr.getMap().removeMistByOwner(chr, 162101010);
                            break;
                        }
                    }

                    SkillFactory.getSkill(162101010).getEffect(skillLevel).applyTo(chr, ret.plusPosition2);
                }
                if (skillid == 162121017) {
                    for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
                        if (mist != null && mist.getSourceSkill() != null && mist.getSourceSkill().getId() == 162121018) {
                            chr.getMap().removeMistByOwner(chr, 162121018);
                            break;
                        }
                    }

                    SkillFactory.getSkill(162121018).getEffect(skillLevel).applyTo(chr, ret.plusPosition2);
                }
                if (skillid == 12111022) {
                    pos = slea.readPos();
                    byte rltype = slea.readByte();
                    slea.skip(2);
                    int objectId = slea.readInt();
                    MapleMonster maelstrom = chr.getMap().getMonsterByOid(objectId);
                    if (maelstrom == null) {
                        System.out.println("maelstrom error");
                    } else {
                        chr.maelstrom = maelstrom.getId();
                        effect.applyTo(c.getPlayer(), maelstrom.getTruePosition(), true, rltype);
                    }
                    break;
                }
                if (skillid == 35111002) {
                    byte tesla = slea.readByte();
                    if (tesla == 2) {
                        slea.skip(8); // 1번째, 2번째의 object Id 출력.
                    }
                }
                if (skillid == 400021047) {
                    pos = slea.readPos();
                } else if (slea.available() == 12) {
                    pos = slea.readPos();
                } else if (slea.available() == 11) {
                    slea.skip(4);
                    pos = slea.readPos();
                } else //            	if (GameConstants.sub_82A590(skillid) > 0 || GameConstants.sub_82AE80(skillid) > 0) {
                if (slea.available() <= 9 && slea.available() >= 5) {
//                    if (effect.isMist() || effect.getSummonMovementType() != null || effect.isMagicDoor() || effect.isMechDoor()) {
                    pos = slea.readPos();
                }
//                }
                if (effect.isMagicDoor()) { // Mystic Door
                    if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                        effect.applyTo(c.getPlayer(), pos);
                    }
                } else if (skillid == 400011015 && chr.getBuffedValue(400011015)) {

                } else if (skillid == 162121043) {
                    effect.applyTo(c.getPlayer(), ret.plusPosition2, true);
                    SkillFactory.getSkill(162121044).getEffect(skillLevel).applyTo(c.getPlayer(), true);
                } else if (skillid == 162111003 || skillid == 162111000) {
                    effect.applyTo(c.getPlayer(), ret.plusPosition2, true);
                } else {
                    slea.skip((int) (slea.available() - 1));
                    final byte rltype = slea.readByte(); // 0 : right, 1 : left
                    effect.applyTo(c.getPlayer(), pos, rltype, true);
                }
                if (skill.getId() == 400011012) {
                    SkillFactory.getSkill(400011013).getEffect(chr.getSkillLevel(400011012)).applyTo(c.getPlayer(),
                            pos);
                    SkillFactory.getSkill(400011014).getEffect(chr.getSkillLevel(400011012)).applyTo(c.getPlayer(),
                            pos);
                }
                break;
        }
    }

    public static final void closeRangeAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean energy) {
        if (chr == null) {
            return;
        }

        if (chr.getMap() == null) {
            return;
        }

        AttackInfo attack = DamageParse.parseDmgM(slea, chr, energy);
        if (attack == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        final boolean mirror = chr.getBuffedValue(MapleBuffStat.ShadowPartner) != null || chr.getBuffedValue(MapleBuffStat.Buckshot) != null;
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        final Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        if (attack.skill == 4341011) {
            if (chr.getCooldownLimit(4341002) > 0) {
                long cool = chr.getCooldownLimit(4341002) - (chr.getCooldownLimit(4341002) / 100 * 20);
                chr.removeCooldown(4341002);
                chr.addCooldown(4341002, System.currentTimeMillis(), cool);
            }
        }

        if (GameConstants.isLinkMap(chr.getMapId()) && attack.skill == 80001770) {
            return;
        }

        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(attack.skill);
            if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }

            if (attack.skill == 400041053) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.NotDamaged);
            }
            if (GameConstants.isDualBlade(chr.getJob())) {
                if (chr.getSkillLevel(4330007) > 0) {
                    if (Randomizer.nextInt(100) < 10) {
                        chr.addHP(chr.getStat().getMaxHp() / 100 * 20);
                    }
                }
            }
            if ((attack.skill == 155101212 || attack.skill == 155121202 || attack.skill == 155120001 || attack.skill == 155111212) && attack.targets > 0) {
                chr.addHP(chr.getStat().getMaxHp() / 100 * effect.getW());
            }

            if (attack.skill == 400011047) {
                List<AdelProjectile> atoms = new ArrayList<>();
                int mobcnt = attack.mobpos.size();

                for (int i = 0; i < mobcnt; i++) {
                    AdelProjectile sa = new AdelProjectile(15, chr.getId(), 0, 400011047, 15000, 0, 1, new Point(attack.mobpos.get(i).x + (-30 * (i - 2)), attack.mobpos.get(i).y - 100), new ArrayList<>());
                    atoms.add(sa);
                }
                if (!atoms.isEmpty()) {
                    chr.getMap().spawnAdelProjectile(chr, atoms, false);
                }
                if (c.getPlayer().getDarknessAura() < 15) {
                    effect = SkillFactory.getSkill(400011047).getEffect(attack.skilllevel);
                    effect.applyTo(chr);
                }
            }
            if (attack.skill == 4341004) {
                if (!chr.skillisCooling(400041075)) {
                    List<Integer> skills = new ArrayList<>();
                    skills.add(400041076);
                    MapleStatEffect huntied = SkillFactory.getSkill(400041075).getEffect(chr.getSkillLevel(400041075));
                    chr.getClient().getSession().writeAndFlush(CField.rangeAttack(4341004, skills, 6, c.getPlayer().getPosition(), chr.isFacingLeft()));
                    chr.addCooldown(400041075, System.currentTimeMillis(), huntied.getCooldown(chr));
                    chr.getClient().getSession().writeAndFlush(CField.skillCooldown(400041075, huntied.getCooldown(chr)));
                }
            }
            if (attack.skill == 4341009) {
                if (!chr.skillisCooling(400041075)) {
                    List<Integer> skills = new ArrayList<>();
                    skills.add(400041078);
                    MapleStatEffect huntied = SkillFactory.getSkill(400041075).getEffect(chr.getSkillLevel(400041075));
                    c.getSession().writeAndFlush(CField.rangeAttack(4341009, skills, 6, c.getPlayer().getPosition(), chr.isFacingLeft()));
                    chr.addCooldown(400041075, System.currentTimeMillis(), huntied.getCooldown(chr));
                    chr.getClient().getSession().writeAndFlush(CField.skillCooldown(400041075, huntied.getCooldown(chr)));
                }
            }
            if (attack.skill == 31111003) {
                int recover = (int) (chr.getStat().getCurrentMaxHp() * (effect.getX() / 100.0D));
                chr.addHP(recover);
            }

            if (GameConstants.isDemonAvenger(chr.getJob())) {
                chr.gainExceed((short) (attack.skill == 31221052 ? 5 : 1), skill);
            }
            if (attack.skill == 400041069) {
                List<Integer> skills = new ArrayList<>();
                skills.add(400041073);
                skills.add(400041071);
                skills.add(400041070);
                skills.add(400041072);
                skills.add(400041071);
                skills.add(400041070);
                skills.add(400041072);
                skills.add(400041071);
                skills.add(400041070);
                skills.add(400041072);
                skills.add(400041071);
                skills.add(400041070);
                c.getSession().writeAndFlush(CField.rangeAttack(400041069, skills, 0, c.getPlayer().getPosition(), chr.isFacingLeft()));
            }
            if (attack.skill == 400051074 || attack.skill == 400051075) {
                MapleStatEffect poll = SkillFactory.getSkill(400051074).getEffect(skillLevel);
                MapleStatEffect pollbox = SkillFactory.getSkill(400051076).getEffect(skillLevel);
                long time = System.currentTimeMillis() - c.getPlayer().getStartFullMakerTime();
                int sendDuration = poll.getCooldown(chr) - (int) time;
                chr.FullMakerSize -= 1;
                if (chr.FullMakerSize <= 0) {
                    chr.FullMakerSize = 0;
                    c.getSession().writeAndFlush(CField.StartFullMaker(chr.FullMakerSize, sendDuration));
                    chr.setFullMakerBox((byte) 0);
                    chr.setStartFullMakerTime(0);
                } else {
                    c.getSession().writeAndFlush(CField.StartFullMaker(chr.FullMakerSize, sendDuration));
                    if (attack.targets == 0) {
                        if (chr.getFullMakerBox() < 2) {
                            Point pos = attack.position;
                            Timer.MapTimer.getInstance().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    MapleMist mist = new MapleMist(pollbox.calculateBoundingBox(pos, chr.isFacingLeft()), chr, pollbox, 2000, (byte) (chr.isFacingLeft() ? 1 : 0));
                                    mist.setDelay(0);
                                    mist.setEndTime(20000);
                                    chr.FullMakerBoxCount++;
                                    chr.getMap().spawnMist(mist, false);
                                }
                            }, 0);
                        }
                    }
                }
            }

            if (attack.skill == 400041056) {
                MapleStatEffect eff = SkillFactory.getSkill(400041055).getEffect(skillLevel);
                c.getSession().writeAndFlush(CField.skillCooldown(eff.getSourceId(), eff.getCooldown(chr)));
                chr.addCooldown(eff.getSourceId(), System.currentTimeMillis(), eff.getCooldown(chr));
            }
            if (attack.skill == 61121105 || attack.skill == 61121222 || attack.skill == 24121052) {
                for (Point mistPos : attack.mistPoints) {
                    effect.applyTo(chr, false, mistPos);
                }
            } else if (((attack.skill - 64001009) >= -2 && (attack.skill - 64001009) <= 2)) {
                effect.applyTo(chr, attack.chain);
            } else if (GameConstants.isExceedAttack(attack.skill) || attack.skill == 400011084) {
                SkillFactory.getSkill(attack.skill).getEffect(attack.skilllevel).applyTo(chr, attack.position, false);
            } else if (attack.skill != 400011089 && attack.skill != 400041021 && attack.skill != 27101202 && (attack.skill == 4341002 || (GameConstants.isCooltimeKeyDownSkill(attack.skill) && chr.getCooldownLimit(GameConstants.getLinkedSkill(attack.skill)) == 0) || (!GameConstants.isCooltimeKeyDownSkill(attack.skill) && !GameConstants.isNoDelaySkill(attack.skill) && !GameConstants.isNoApplySkill(attack.skill)))) {
                int[] finalAttacks = {1100002, 1120013, 1200002, 1300002, 3100001, 3120008, 3200001, 4341054, 11101002, 13101002, 21100010, 21120012, 23100006, 23120012, 33100009, 33120011, 51100002, 51120002};
                for (int finalAttack : finalAttacks) {
                    if (finalAttack == attack.skill) {
                        return;
                    }
                }
                int[] finalAttacks2 = {2120013, 2220014, 32121011};
                for (int finalAttack : finalAttacks2) {
                    if (finalAttack == attack.skill) {
                        return;
                    }
                }
                if (!energy) {
                    if (attack.skill >= 400041002 && attack.skill <= 400041005) {
                        skill.getEffect(skillLevel).applyTo(chr, attack.position);
                    } else if (GameConstants.isDemonSlash(attack.skill)) {
                        skill.getEffect(attack.skilllevel).applyTo(chr);
                    } else {
                        if (attack.skill == 35121052) {
                            chr.setKeyValue(2, "fa", String.valueOf(chr.isFacingLeft() ? 1 : 0));
                        }
                        effect.applyTo(chr, attack.position);
                    }
                }
            }

            if (GameConstants.isPathFinder(chr.getJob())) {
                MapleStatEffect e = SkillFactory.getSkill(400031049).getEffect(c.getPlayer().getSkillLevel(400031049));
                for (MapleSummon s : c.getPlayer().getMap().getAllSummonsThreadsafe()) {
                    if (s.getOwner().getId() == c.getPlayer().getId()) {
                        if (s.getSkill() == 400031047) {
                            if (c.getPlayer().getBuffedEffect(MapleBuffStat.RelicUnbound) == null) {
                                MapleStatEffect eff = SkillFactory.getSkill(400031048).getEffect(c.getPlayer().getSkillLevel(400031048));
                                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                                statups.put(MapleBuffStat.RelicUnbound, new Pair<>(1, 4000));
                                c.getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(statups, eff, c.getPlayer()));
                            }
                        }
                        if (s.getSkill() == 400031049) {
                            if (c.getPlayer().getStartRelicUnboundTime() <= 0) {
                                c.getPlayer().setStartRelicUnboundTime(System.currentTimeMillis());
                                c.getSession().writeAndFlush(CField.TigerSpecialAttack(c.getPlayer().getId(), s.getObjectId(), s.getSkill()));
                            }
                            long time = System.currentTimeMillis() - c.getPlayer().getStartRelicUnboundTime();
                            if (time >= 2000) {
                                c.getPlayer().setStartRelicUnboundTime(System.currentTimeMillis());
                                c.getSession().writeAndFlush(CField.TigerSpecialAttack(c.getPlayer().getId(), s.getObjectId(), s.getSkill()));
                            }
                        }
                        if (s.getSkill() == 400031051 && c.getPlayer().getStartRelicUnboundTime() == 0) {
                            c.getPlayer().setStartRelicUnboundTime(System.currentTimeMillis());
                            c.getSession().writeAndFlush(CField.TigerSpecialAttack(c.getPlayer().getId(), s.getObjectId(), s.getSkill()));
                        }
                    }
                }
            }

            if (attack.skill == 400011131) {
                List<Integer> skills = new ArrayList<>();
                int mobcnt = attack.mobpos.size();
                skills.add(400011132);
                for (int i = 0; i < mobcnt; i++) {
                    chr.getClient().getSession().writeAndFlush(CField.rangeAttack(400011131, skills, 0, attack.mobpos.get(i), chr.isFacingLeft()));
                }
            }

            switch (attack.skill) {
                case 61101002:
                case 61110211:
                case 61120007:
                case 61121217:
                    effect = attack.getAttackEffect(chr, chr.getSkillLevel(attack.skill), skill);
                    DamageParse.applyAttack(attack, skill, c.getPlayer(), maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED, false, energy);
                    chr.cancelEffectFromBuffStat(MapleBuffStat.StopForceAtominfo);
                    MapleStatEffect realEffect = SkillFactory.getSkill(61101002).getEffect(c.getPlayer().getSkillLevel(61101002));
                    if (!effect.ignoreCooldown(chr)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(61101002, realEffect.getCooldown(chr)));
                    }
                    chr.cancelEffectFromBuffStat(MapleBuffStat.StopForceAtominfo);
                    break;
            }

            if (!chr.skillisCooling(attack.skill) && (attack.skill == 155120000 || attack.skill == 155110000)) {
                c.getSession().writeAndFlush(CField.skillCooldown(155001102, 2200));
            }

            if (GameConstants.isSoulMaster(chr.getJob()) && chr.getSkillLevel(400011048) > 0) {
                if (!chr.skillisCooling(400011048)) {
                    int skillid = 0;
                    if (chr.getBuffedValue(400011005)) {
                        skillid = 400011049;
                    } else if (chr.getBuffedEffect(MapleBuffStat.PoseType) != null) {
                        skillid = 400011048;
                    }
                    if (skillid != 0) {
                        c.getSession().writeAndFlush(CField.EffectPacket.showSkillEffect(chr, skillid, true));
                        c.getPlayer().getMap().broadcastMessage(chr, CField.EffectPacket.showSkillEffect(chr, skillid, false), false);
                        List<Integer> attackList = new ArrayList<>();
                        attackList.add(skillid);
                        chr.getClient().getSession().writeAndFlush(CField.rangeAttack(skillid, attackList, 1, attack.position, chr.isFacingLeft()));
                        chr.addCooldown(400011048, System.currentTimeMillis(), 12000);
                    }
                }
            }

            if (chr.getSkillLevel(80002632) > 0) {
                Item weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (weapon_item != null) {
                    // 무기가 null이 아닌지 체크
                    String weapon_name = MapleItemInformationProvider.getInstance().getName(weapon_item.getItemId());
                    if (weapon_name != null) {
                        if (weapon_name.startsWith("제네시스 ") && !chr.getBuffedValue(80002632)) { // 무기명이 제네시스로 시작하는지 체크
                            if (!chr.skillisCooling(80002632)) { // 스킬 쿨이 아니라면
                                MapleStatEffect effcts = SkillFactory.getSkill(80002632).getEffect(chr.getSkillLevel(80002632));
                                effcts.applyTo(chr);
                                c.getSession().writeAndFlush(CField.skillCooldown(80002632, 90000));
                                chr.addCooldown(80002632, System.currentTimeMillis(), 90000);
                            }
                        }
                    }
                }
            }
            c.getPlayer().createRoyalKnights(attack);
            if (effect.getCooldown(chr) > 0 && !effect.ignoreCooldown(chr) && attack.skill != 400021122) {
                if (!energy && chr.skillisCooling(effect.getSourceId()) && !GameConstants.isCooltimeKeyDownSkill(effect.getSourceId()) && !GameConstants.isNoApplySkill(effect.getSourceId()) && !GameConstants.isLinkedSkill(attack.skill) && !chr.getBuffedValue(effect.getSourceId())) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                } else if (effect.getSourceId() == 15111022 || effect.getSourceId() == 15120003) {
                    if (!chr.getBuffedValue(15121054)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (effect.getSourceId() == 1321013) {
                    if (!chr.getBuffedValue(1321015) && chr.getBuffedEffect(MapleBuffStat.Reincarnation) == null) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (effect.getSourceId() == 11121055) {
                    if (chr.getBuffedEffect(MapleBuffStat.Ellision) == null) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (chr.getBuffedValue(20040219) || chr.getBuffedValue(20040220)) {
                    if (skill.isHyper() || !GameConstants.isLuminous(effect.getSourceId() / 10000)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (effect.getSourceId() == 400011079) {
                    if (chr.ignoreDraco == 0) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    } else {
                        chr.ignoreDraco--;
                    }
                } else if (!chr.skillisCooling(effect.getSourceId()) && !GameConstants.isAutoAttackSkill(effect.getSourceId()) && !GameConstants.isAfterCooltimeSkill(effect.getSourceId())) {
                    c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                    chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                }
            }
        }
        if (!energy) {
            if ((chr.getMapId() == 109060000 || chr.getMapId() == 109060002 || chr.getMapId() == 109060004) && attack.skill == 0) {
                MapleSnowballs.hitSnowball(chr);
            }
            // handle combo orbconsume
            int numFinisherOrbs = 0;
            final Integer comboBuff = chr.getBuffedValue(MapleBuffStat.ComboCounter);

            if (isFinisher(attack.skill)) { // finisher
                if (comboBuff != null) {
                    numFinisherOrbs = comboBuff.intValue() - 1;
                }
                if (numFinisherOrbs <= 0) {
                    return;
                }
                chr.handleOrbconsume(attack.skill);

//                maxdamage *= numFinisherOrbs;
            }
        }

        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, CField.addAttackInfo(0, chr, attack), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.addAttackInfo(0, chr, attack), false);
        }
        DamageParse.applyAttack(attack, skill, c.getPlayer(), maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED, false, energy);
    }

    public static final void BuffAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (chr.getMap() == null) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgB(slea, chr);
        if (attack == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final boolean mirror = chr.getBuffedValue(MapleBuffStat.ShadowPartner) != null || chr.getBuffedValue(MapleBuffStat.Buckshot) != null;
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        final Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedSkill(attack.skill));
            if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }

            if (GameConstants.isDemonAvenger(chr.getJob())) {
                chr.gainExceed((short) (attack.skill == 31221052 ? 5 : 1), skill);
            }
            switch (attack.skill) {
                case 61101002:
                case 61110211:
                case 61120007:
                case 61121217:
                    effect = attack.getAttackEffect(chr, chr.getSkillLevel(attack.skill), skill);
                    DamageParse.applyAttack(attack, skill, c.getPlayer(), maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED, false, false);
                    chr.cancelEffectFromBuffStat(MapleBuffStat.StopForceAtominfo);
                    MapleStatEffect realEffect = SkillFactory.getSkill(61101002).getEffect(c.getPlayer().getSkillLevel(61101002));
                    if (!effect.ignoreCooldown(chr)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(61101002, realEffect.getCooldown(chr)));
                    }
                    chr.cancelEffectFromBuffStat(MapleBuffStat.StopForceAtominfo);
                    break;

            }
            if (chr.getSkillLevel(80002632) > 0) {
                Item weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (weapon_item != null) {
                    // 무기가 null이 아닌지 체크
                    String weapon_name = MapleItemInformationProvider.getInstance().getName(weapon_item.getItemId());
                    if (weapon_name != null) {
                        if (weapon_name.startsWith("제네시스 ")) { // 무기명이 제네시스로 시작하는지 체크
                            if (!chr.skillisCooling(80002632) && !chr.getBuffedValue(80002632)) { // 스킬 쿨이 아니라면
                                MapleStatEffect effcts = SkillFactory.getSkill(80002632).getEffect(chr.getSkillLevel(80002632));
                                effcts.applyTo(chr);
                                c.getSession().writeAndFlush(CField.skillCooldown(80002632, 90000));
                                chr.addCooldown(80002632, System.currentTimeMillis(), 90000);
                            }
                        }
                    }
                }
            }

            if (effect.getCooldown(chr) > 0 && !effect.ignoreCooldown(chr)) {
                if (chr.skillisCooling(attack.skill) && !GameConstants.isCooltimeKeyDownSkill(attack.skill) && !GameConstants.isNoApplySkill(attack.skill) && !GameConstants.isLinkedSkill(attack.skill) && !chr.getBuffedValue(attack.skill)) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                if (attack.skill == 15111022 || attack.skill == 15120003) {
                    if (!chr.getBuffedValue(15121054)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                        chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (attack.skill == 1321013) {
                    if (!chr.getBuffedValue(1321015) && chr.getBuffedEffect(MapleBuffStat.Reincarnation) == null) {
                        c.getSession().writeAndFlush(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                        chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (chr.getBuffedValue(20040219) || chr.getBuffedValue(20040220)) {
                    if (skill.isHyper() || !GameConstants.isLuminous(attack.skill / 10000)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                        chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (!chr.skillisCooling(attack.skill)) {
                    c.getSession().writeAndFlush(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                    chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr));
                }

                if (GameConstants.isLinkedSkill(attack.skill) && !chr.skillisCooling(GameConstants.getLinkedSkill(attack.skill))) {
                    c.getSession().writeAndFlush(CField.skillCooldown(GameConstants.getLinkedSkill(attack.skill), effect.getCooldown(chr)));
                    chr.addCooldown(GameConstants.getLinkedSkill(attack.skill), System.currentTimeMillis(), effect.getCooldown(chr));
                }
            }

        }
        if ((chr.getMapId() == 109060000 || chr.getMapId() == 109060002 || chr.getMapId() == 109060004) && attack.skill == 0) {
            MapleSnowballs.hitSnowball(chr);
        }
        // handle combo orbconsume
        int numFinisherOrbs = 0;
        final Integer comboBuff = chr.getBuffedValue(MapleBuffStat.ComboCounter);

        if (isFinisher(attack.skill)) { // finisher
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff.intValue() - 1;
            }
            if (numFinisherOrbs <= 0) {
                return;
            }
            chr.handleOrbconsume(attack.skill);

//                maxdamage *= numFinisherOrbs;
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, CField.addAttackInfo(4, chr, attack), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.addAttackInfo(4, chr, attack), false);
        }
        DamageParse.applyAttack(attack, skill, c.getPlayer(), maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED, true, false);

    }

    public static final void rangedAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (chr.getMap() == null) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgR(slea, chr);
        if (attack == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        int bulletCount = 1, skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        boolean AOE = attack.skill == 4111004;
        boolean noBullet = (chr.getJob() >= 300 && chr.getJob() <= 322 && chr.getTotalSkillLevel(3000002) > 0) || (chr.getJob() >= 510 && chr.getJob() <= 512) || (chr.getJob() >= 3500 && chr.getJob() <= 3512) || GameConstants.isCannon(chr.getJob()) || GameConstants.isPhantom(chr.getJob()) || GameConstants.isMercedes(chr.getJob()) || GameConstants.isZero(chr.getJob()) || GameConstants.isXenon(chr.getJob()) || GameConstants.isKaiser(chr.getJob()) || GameConstants.isAngelicBuster(chr.getJob()) || GameConstants.isKadena(chr.getJob()) || GameConstants.isPathFinder(chr.getJob());
        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedSkill(attack.skill));
            if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                c.getPlayer().dropMessage(1, "Range Skill Null!");
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }

            if (attack.skill == 61121105 || attack.skill == 61121222 || attack.skill == 24121052) {
                for (Point mistPos : attack.mistPoints) {
                    skill.getEffect(skillLevel).applyTo(chr, false, mistPos);
                }
            } else if (((attack.skill - 64001009) >= -2 && (attack.skill - 64001009) <= 2)) {
                SkillFactory.getSkill(attack.skill).getEffect(skillLevel).applyTo(chr, attack.chain, true);
            } else if (!GameConstants.isNoDelaySkill(attack.skill) && !GameConstants.isNoApplySkill(attack.skill)) {
                int[] finalAttacks = {1100002, 1120013, 1200002, 1300002, 3100001, 3120008, 3200001, 4341054, 11101002, 13101002, 21100010, 21120012, 23100006, 23120012, 33100009, 33120011, 51100002, 51120002};
                for (int finalAttack : finalAttacks) {
                    if (finalAttack == attack.skill) {
                        return;
                    }
                }
                int[] finalAttacks2 = {2120013, 2220014, 32121011};
                for (int finalAttack : finalAttacks2) {
                    if (finalAttack == attack.skill) {
                        return;
                    }
                }
                skill.getEffect(skillLevel).applyTo(chr, attack.position);
            }

            int[] linkCooldownSkills = {23121052, 400031007, 23111002, 23121002};
            for (int ck : linkCooldownSkills) {
                if (attack.skill == ck && chr.getCooldownLimit(ck) > 0 && attack.isLink) {
                    chr.changeCooldown(ck, -1000);
                }
            }

            switch (attack.skill) {
                case 13101005:
                case 21110004: // Ranged but uses attackcount instead
                case 14101006: // Vampure
                case 21120006:
                case 11101004:
                case 1077:
                case 1078:
                case 1079:
                case 11077:
                case 11078:
                case 11079:
                case 15111007:
                case 13111007: //Wind Shot
                case 33101007:
                case 33101002:
                case 33121002:
                case 33121001:
                case 21100004:
                case 21110011:
                case 21100007:
                case 21110027:
                case 21110028:
                case 21000004:
                case 21001009:
                case 5121002:
                case 4121003:
                case 4221003:
                case 3111004: // arrow rain
                case 3211004: // arrow eruption
                case 4121017:
                case 4121016: //?⑤뱺?덉씠??                
                case 4121052: //?ъ떆利?                
                case 13101020: //?섏뼱由???                
                case 33121052: //?⑦뵾吏 ?좎쫰 ??                
                case 35121054: //?몃쾭留?                
                case 51001004: //?뚯슱 釉붾젅?대뱶
                case 51111007: //?ㅼ씠??踰꾩뒪??                
                case 51121008: //?ㅼ씠??釉붾씪?ㅽ듃
                case 22110025:
                case 400010000:
                    AOE = true;
                    bulletCount = effect.getAttackCount();
                    break;
                case 5220023:
                case 5220024:
                case 5220025:
                case 35121005:
                case 35111004:
                case 35121013:
                    AOE = true;
                    bulletCount = 6;
                    break;
                case 13001020: //釉뚮━利??좊줈??                
                case 13121002: //?ㅽ뙆?대윺 蹂쇳뀓??                
                case 13111020: //?쒕━諛붾엺??援곕Т
                case 13121052: //紐ъ닚
                case 5211008: //?붾툝諛곕윺??                
                case 5221017: //?쇱떎?덉씠??                
                case 5221052: //?ㅽ듃?덉씤吏 遊?                
                case 5221022: //諛고???遊꾨쾭
                    bulletCount = effect.getAttackCount();
                    break;
                default:
                    bulletCount = effect.getBulletCount();
                    break;
            }
            if (noBullet && effect.getBulletCount() < effect.getAttackCount()) {
                bulletCount = effect.getAttackCount();
            }
            if (chr.getSkillLevel(80002632) > 0) {
                Item weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (weapon_item != null) {
                    // 무기가 null이 아닌지 체크
                    String weapon_name = MapleItemInformationProvider.getInstance().getName(weapon_item.getItemId());
                    if (weapon_name != null) {
                        if (weapon_name.startsWith("제네시스 ")) { // 무기명이 제네시스로 시작하는지 체크
                            if (!chr.skillisCooling(80002632) && !chr.getBuffedValue(80002632)) { // 스킬 쿨이 아니라면
                                MapleStatEffect effcts = SkillFactory.getSkill(80002632).getEffect(chr.getSkillLevel(80002632));
                                effcts.applyTo(chr);
                                c.getSession().writeAndFlush(CField.skillCooldown(80002632, 90000));
                                chr.addCooldown(80002632, System.currentTimeMillis(), 90000);
                            }
                        }
                    }
                }
            }

            c.getPlayer().createRoyalKnights(attack);
            if (chr.getBuffedEffect((MapleBuffStat.ThrowBlasting)) != null) {
                if (attack.skill / 400040000 <= 0) {
                    MapleStatEffect effcts = SkillFactory.getSkill(400041061).getEffect(chr.getSkillLevel(400041061));

                    List<Integer> skills = new ArrayList<>();
                    skills.add(400041079);

                    int cout = Randomizer.rand(2, 4);
                    if (c.getPlayer().ThrowBlasting < cout) {
                        cout = c.getPlayer().ThrowBlasting;
                    }
                    if (c.getPlayer().ThrowBlasting > 0) {
                        c.getPlayer().ThrowBlasting -= cout;
                        for (Point mobpos : attack.mobpos) {
                            c.getSession().writeAndFlush(CField.rangeAttack(400041061, skills, cout, mobpos, chr.isFacingLeft()));
                            break;
                        }
                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        long time = System.currentTimeMillis() - c.getPlayer().getStartThrowBlastingTime();
                        int sendDuration = effcts.getDuration() - (int) time;
                        statups.put(MapleBuffStat.ThrowBlasting, new Pair<>(c.getPlayer().ThrowBlasting, sendDuration));
                        c.getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(statups, effcts, c.getPlayer()));
                    }

                    if (c.getPlayer().ThrowBlasting <= 0 && chr.getBuffedEffect((MapleBuffStat.ThrowBlasting)) != null) {
                        c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.ThrowBlasting);
                    }
                }

            } else if (chr.getBuffedEffect((MapleBuffStat.ThrowBlasting)) == null && chr.getSkillLevel(400041061) > 0 && !chr.skillisCooling(400041062)) {
                MapleStatEffect effcts = SkillFactory.getSkill(400041061).getEffect(chr.getSkillLevel(400041061));

                List<Integer> skills = new ArrayList<>();
                skills.add(400041062);

                for (Point mobpos : attack.mobpos) {
                    c.getSession().writeAndFlush(CField.rangeAttack(400041061, skills, 1, mobpos, c.getPlayer().isFacingLeft()));
                    break;
                }

                int time = effcts.getSubTime() / 1000;
                chr.addCooldown(400041062, System.currentTimeMillis(), time);
                c.getSession().writeAndFlush(CField.skillCooldown(400041062, time));
            }

            if (effect.getCooldown(chr) > 0 && !effect.ignoreCooldown(chr)) {
                if (chr.skillisCooling(effect.getSourceId()) && !GameConstants.isCooltimeKeyDownSkill(effect.getSourceId()) && !GameConstants.isNoApplySkill(attack.skill) && !GameConstants.isLinkedSkill(effect.getSourceId()) && !chr.getBuffedValue(effect.getSourceId())) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                }
                if (effect.getSourceId() == 400031000 || effect.getSourceId() == 400031001) {
                    
                } else if (effect.getSourceId() == 15111022 || effect.getSourceId() == 15120003) {
                    if (!chr.getBuffedValue(15121054)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (effect.getSourceId() == 3221007) {
                    if (chr.getSkillLevel(3220051) == 0) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (effect.getSourceId() == 1321013) {
                    if (!chr.getBuffedValue(1321015) && chr.getBuffedEffect(MapleBuffStat.Reincarnation) == null) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (chr.getBuffedValue(20040219) || chr.getBuffedValue(20040220)) {
                    if (skill.isHyper() || !GameConstants.isLuminous(effect.getSourceId() / 10000)) {
                        c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                        chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                    }
                } else if (!chr.skillisCooling(effect.getSourceId()) && !GameConstants.isAutoAttackSkill(effect.getSourceId()) && !GameConstants.isAfterCooltimeSkill(effect.getSourceId())) {
                    c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                    chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                }
            }

        }

        final Integer ShadowPartner = chr.getBuffedValue(MapleBuffStat.ShadowPartner);
        if (ShadowPartner != null) {
            bulletCount *= 2;
        }
        int projectile = 0, visProjectile = 0;
        if (!AOE && chr.getBuffedValue(MapleBuffStat.SoulArrow) == null && !noBullet) {
            if (!GameConstants.isKain(chr.getJob()) && !GameConstants.isEvan(chr.getJob())) {  //아이템 눌인경우를.. 왜 ..
                Item ipp = chr.getInventory(MapleInventoryType.USE).getItem(attack.slot);
                if (attack.item == 0 && ipp != null) {
                    attack.item = ipp.getItemId();
                } else if (ipp == null || (ipp.getItemId() != attack.item)) {
                    return;
                }
                projectile = ipp.getItemId();
                visProjectile = projectile;

                /*            if (attack.csstar > 0) {
             if (chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar) == null) {
             return;
             }
             visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar).getItemId();
             } else { */
//            }
                // Handle bulletcount
                if (chr.getBuffedValue(MapleBuffStat.NoBulletConsume) == null) {
                    int bulletConsume = bulletCount;
                    if (effect != null && effect.getBulletConsume() != 0) {
                        bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
                    }
                    if (chr.getJob() == 412 && bulletConsume > 0 && ipp.getQuantity() < MapleItemInformationProvider.getInstance().getSlotMax(projectile)) {
                        final Skill expert = SkillFactory.getSkill(4120010);
                        if (chr.getTotalSkillLevel(expert) > 0) {
                            final MapleStatEffect eff = expert.getEffect(chr.getTotalSkillLevel(expert));
                            if (eff.makeChanceResult()) {
                                ipp.setQuantity((short) (ipp.getQuantity() + 1));
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(MapleInventoryType.USE, ipp, false));
                                bulletConsume = 0; //regain a star after using
                                // c.getSession().writeAndFlush(InventoryPacket.getInventoryStatus());
                                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                            }
                        }
                    }
                    if (bulletConsume > 0) {
                        if (!MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true)) {
                            chr.dropMessage(5, "You do not have enough arrows/bullets/stars.");
                            return;
                        }
                    }
                }
            }
        } else if (chr.getJob() >= 3500 && chr.getJob() <= 3512) {
            visProjectile = 2333000;
        } else if (GameConstants.isCannon(chr.getJob())) {
            visProjectile = 2333001;
        }
        double basedamage;
        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }

        final PlayerStats statst = chr.getStat();
        switch (attack.skill) {
            case 4001344: // Lucky Seven
            case 4121007: // Triple Throw
            case 14001004: // Lucky seven
            case 14111005: // Triple Throw
                basedamage = Math.max(statst.getCurrentMaxBaseDamage(), (float) ((float) ((statst.getTotalLuk() * 5.0f) * (statst.getTotalWatk() + projectileWatk)) / 100));
                break;
            case 4111004: // Shadow Meso
//		basedamage = ((effect.getMoneyCon() * 10) / 100) * effect.getProp(); // Not sure
                basedamage = 53000;
                break;
            default:
                basedamage = statst.getCurrentMaxBaseDamage();
                switch (attack.skill) {
                    case 3101005: // arrowbomb is hardcore like that
                        basedamage *= effect.getX() / 100.0;
                        break;
                }
                break;
        }
        if (effect != null) {
            basedamage *= (effect.getDamage() + statst.getDamageIncrease(attack.skill)) / 100.0;

            long money = effect.getMoneyCon();
            if (money != 0) {
                if (money > chr.getMeso()) {
                    money = chr.getMeso();
                }
                chr.gainMeso(-money, false);
            }
        }

        //캐시 표창
        for (Item item : chr.getInventory(MapleInventoryType.CASH).newList()) {
            if (item.getItemId() / 1000 == 5021) {
                attack.item = item.getItemId();
            }
        }

        chr.checkFollow();
        if (!chr.isHidden()) {
            if (attack.skill == 3211006) {
                chr.getMap().broadcastMessage(chr, CField.addAttackInfo(2, chr, attack), chr.getTruePosition());
            } else {
                chr.getMap().broadcastMessage(chr, CField.addAttackInfo(1, chr, attack), chr.getTruePosition());
            }
        } else if (attack.skill == 3211006) {
            chr.getMap().broadcastGMMessage(chr, CField.addAttackInfo(2, chr, attack), false);
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.addAttackInfo(1, chr, attack), false);
        }

        DamageParse.applyAttack(attack, skill, chr, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED, false, false);
    }

    public static final void MagicDamage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, boolean chilling, boolean orbital) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgMa(slea, chr, chilling, orbital);
        if (attack == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedSkill(attack.skill));
        if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }
        final int skillLevel = chr.getTotalSkillLevel(skill);
        final MapleStatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);
        if (effect == null) {
            return;
        }

        if (attack.skill == 2111003 || (!GameConstants.isNoDelaySkill(attack.skill) && !GameConstants.isNoApplySkill(attack.skill)) && !orbital && !GameConstants.isFusionSkill(attack.skill)) {
            int[] finalAttacks = {1100002, 1120013, 1200002, 1300002, 3100001, 3120008, 3200001, 4341054, 11101002, 13101002, 21100010, 21120012, 23100006, 23120012, 33100009, 33120011, 51100002, 51120002};
            for (int finalAttack : finalAttacks) {
                if (finalAttack == attack.skill) {
                    return;
                }
            }
            int[] finalAttacks2 = {2120013, 2220014, 32121011};
            for (int finalAttack : finalAttacks2) {
                if (finalAttack == attack.skill) {
                    return;
                }
            }
            if (GameConstants.is_evan_force_skill(attack.skill)) {
                if (!chr.skillisCooling(attack.skill)) {
                    skill.getEffect(skillLevel).applyTo(chr);
                }
            } else if (attack.skill == 2111003) {
                skill.getEffect(skillLevel).applyTo(chr, attack.position);
            } else {
                skill.getEffect(skillLevel).applyTo(chr);
            }
        }

        double maxdamage = chr.getStat().getCurrentMaxBaseDamage() * (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0;
        if (GameConstants.isPyramidSkill(attack.skill)) {
            maxdamage = 1;
        } else if (GameConstants.isBeginnerJob(skill.getId() / 10000) && skill.getId() % 10000 == 1000) {
            maxdamage = 40;
        }
        if (chr.getSkillLevel(80002632) > 0) {
            Item weapon_item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            if (weapon_item != null) {
                // 무기가 null이 아닌지 체크
                String weapon_name = MapleItemInformationProvider.getInstance().getName(weapon_item.getItemId());
                if (weapon_name != null) {
                    if (weapon_name.startsWith("제네시스 ")) { // 무기명이 제네시스로 시작하는지 체크
                        if (!chr.skillisCooling(80002632) && !chr.getBuffedValue(80002632)) { // 스킬 쿨이 아니라면
                            MapleStatEffect effcts = SkillFactory.getSkill(80002632).getEffect(chr.getSkillLevel(80002632));
                            effcts.applyTo(chr);
                            c.getSession().writeAndFlush(CField.skillCooldown(80002632, 90000));
                            chr.addCooldown(80002632, System.currentTimeMillis(), 90000);
                        }
                    }
                }
            }
        }

        if (effect.getCooldown(chr) > 0 && !effect.ignoreCooldown(chr)) {
            if (chr.skillisCooling(effect.getSourceId()) && !GameConstants.isCooltimeKeyDownSkill(effect.getSourceId()) && !GameConstants.isNoApplySkill(attack.skill) && !GameConstants.isLinkedSkill(effect.getSourceId()) && !chr.getBuffedValue(effect.getSourceId()) && chr.unstableMemorize != effect.getSourceId()
                    && effect.getSourceId() != 2301002 && effect.getSourceId() != 400021129 && effect.getSourceId() != 22111012 && effect.getSourceId() != 22110022 && effect.getSourceId() != 22110023) {
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                return;
            }
            if (effect.getSourceId() == 15111022 || effect.getSourceId() == 15120003) {
                if (!chr.getBuffedValue(15121054)) {
                    c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                    chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                }
            } else if (effect.getSourceId() == 1321013) {
                if (!chr.getBuffedValue(1321015) && chr.getBuffedEffect(MapleBuffStat.Reincarnation) == null) {
                    c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                    chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                }
            } else if (chr.getBuffedValue(20040219) || chr.getBuffedValue(20040220)) {
                if (skill.isHyper() || !GameConstants.isLuminous(effect.getSourceId() / 10000)) {
                    c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                    chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
                }
            } else if (chr.unstableMemorize == effect.getSourceId()) {
                chr.unstableMemorize = 0;
            } else if (!chr.skillisCooling(effect.getSourceId()) && !GameConstants.isAutoAttackSkill(effect.getSourceId()) && !GameConstants.isAfterCooltimeSkill(effect.getSourceId())) {
                c.getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(chr)));
                chr.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(chr));
            }
        }

        chr.checkFollow();

        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, CField.addAttackInfo(3, chr, attack), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.addAttackInfo(3, chr, attack), false);
        }
        DamageParse.applyAttackMagic(attack, skill, chr, effect, maxdamage);
    }

    public static final void DropMeso(final int meso, final MapleCharacter chr) {
        if (!chr.isAlive() || (meso < 10 || meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        chr.gainMeso(-meso, false, true);
        chr.getMap().spawnMesoDrop(meso, chr.getTruePosition(), chr, chr, true, (byte) 0);
    }

    public static final void ChangeAndroidEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 0 && chr != null && chr.getMap() != null && !chr.isHidden() && emote <= 17 && chr.getAndroid() != null) { //O_o
            chr.getMap().broadcastMessage(CField.showAndroidEmotion(chr.getId(), emote));
        }
    }

    public static final void MoveAndroid(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(8);
        int unk1 = slea.readInt();
        int unk2 = slea.readInt();
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);

        if (res != null && chr != null && res.size() != 0 && chr.getMap() != null && chr.getAndroid() != null) { // map crash hack
            if (chr.getMapId() == ServerConstants.WarpMap) {
                return;
            }
            final Point pos = new Point(chr.getAndroid().getPos());
            chr.getAndroid().updatePosition(res);
            chr.getMap().broadcastMessage(chr, CField.moveAndroid(chr.getId(), pos, res, unk1, unk2), false);
        }
    }

    public static final void MoveHaku(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(12);
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);

        if (res != null && chr != null && res.size() != 0 && chr.getMap() != null && chr.getAndroid() != null) { // map crash hack
            final Point pos = new Point(chr.getAndroid().getPos());
            chr.getHaku().updatePosition(res);
            chr.getMap().broadcastMessage(chr, CField.moveHaku(chr.getId(), pos, res), false);
        }
    }

    public static final void ChangeEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 7) {
            final int emoteid = 5159992 + emote;
            final MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {
                return;
            }
        }
        if (emote > 0 && chr != null && chr.getMap() != null && !chr.isHidden()) { //O_o
            chr.getMap().broadcastMessage(chr, CField.facialExpression(chr, emote), false);
        }
    }

    public static final void Heal(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        slea.readInt();
        if (slea.available() >= 8) {
            slea.skip(4);
        }
        int healHP = slea.readShort();
        int healMP = slea.readShort();

        final PlayerStats stats = chr.getStat();

        if (stats.getHp() <= 0) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (healHP != 0 && chr.canHP(now + 1000) && chr.isAlive() && !chr.getMap().isTown()) {
            if (healHP > stats.getHealHP()) {
                healHP = (int) stats.getHealHP();
            }
            chr.addHP(healHP);
        }
        if (healMP != 0 && !GameConstants.isDemonSlayer(chr.getJob()) && chr.canMP(now + 1000) && chr.isAlive() && !chr.getMap().isTown()) { //just for lag
            if (healMP > stats.getHealMP()) {
                healMP = (int) stats.getHealMP();
            }
            chr.addMP(healMP);
        }
    }

    public static final void MovePlayer(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
//        if (getAcCheckLong() > 100) {
////            System.out.println("getAcCheckLong > 100");
//            if (!c.getAcCheck()) { // Check Connector Connection status
//                // System.out.println(c.getSession() + "AC Connector is closed");
//                 c.getSession().close();
//                return;
//            }
//            resetAcCheckLong();
//        } else {
//            getAcCheckLong();
//        }
        
        chr.setLastMovement(System.currentTimeMillis());
        slea.skip(22); // portal count
        final Point Original_Pos = chr.getPosition(); // 4 bytes Added on v.80 MSEA
        List<LifeMovementFragment> res;
        try {
            res = MovementParse.parseMovement(slea, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("unk movement type \n : " + slea.toString(true));
            return;
        }

        if (res != null && c.getPlayer().getMap() != null) {
            final MapleMap map = c.getPlayer().getMap();

            if (chr.isHidden()) {
                chr.setLastRes(res);
                c.getPlayer().getMap().broadcastGMMessage(chr, CField.movePlayer(chr.getId(), res, Original_Pos), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.movePlayer(chr.getId(), res, Original_Pos), false);
            }

            MovementParse.updatePosition(res, chr, 0);
            final Point pos = chr.getTruePosition();
            map.movePlayer(chr, pos);
            if (chr.getFollowId() > 0 && chr.isFollowOn() && chr.isFollowInitiator()) {
                final MapleCharacter fol = map.getCharacterById(chr.getFollowId());
                if (fol != null) {
                    final Point original_pos = fol.getPosition();
                    MovementParse.updatePosition(res, fol, 0);
                    map.movePlayer(fol, pos);
                    map.broadcastMessage(fol, CField.movePlayer(fol.getId(), res, original_pos), false);
                } else {
                    chr.checkFollow();
                }
            }
            int count = c.getPlayer().getFallCounter();
            final boolean samepos = pos.y > c.getPlayer().getOldPosition().y && Math.abs(pos.x - c.getPlayer().getOldPosition().x) < 5;
            if (samepos && (pos.y > (map.getBottom() + 250) || map.getFootholds().findBelow(pos) == null)) {
                if (count > 5) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                    c.getPlayer().setFallCounter(0);
                } else {
                    c.getPlayer().setFallCounter(++count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
            c.getPlayer().setOldPosition(pos);
//            if (!samepos && c.getPlayer().getBuffSource(MapleBuffStat.DARK_AURA) == 32120013) { //?대뱶諛댁뒪???ㅽ겕 ?ㅻ씪
            //              c.getPlayer().getBuffedEffect(MapleBuffStat.DARK_AURA).applyMonsterBuff(c.getPlayer());
            //        } else if (!samepos && c.getPlayer().getBuffSource(MapleBuffStat.YELLOW_AURA) == 32120014) { //?대뱶諛댁뒪???먮줈???ㅻ씪
            //          c.getPlayer().getBuffedEffect(MapleBuffStat.YELLOW_AURA).applyMonsterBuff(c.getPlayer());
            //    }
        }
    }

    public static final void ChangeMapSpecial(final String portal_name, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MaplePortal portal = chr.getMap().getPortal(portal_name);

        if (portal != null) {
            portal.enterPortal(c);
        } else {
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
        }
    }

    public static final void ChangeMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        if (chr.getMapId() == 100000203 && chr.getPvpStatus()) {

            c.getPlayer().dropMessage(5, "PVP도중에는 나가실 수 없습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        } else if ((chr.getMapId() == 931050800 || chr.getMapId() == 931050810 || chr.getMapId() == 931050820) && chr.getEventInstance() != null) {
            c.getPlayer().dropMessage(5, "보스레이드 도중에는 나가실 수 없습니다. 나가시려면 문 교수를 통해 나가주세요.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            return;
        }
        if (slea.available() != 0) {
            slea.skip(15);
            int targetid = slea.readInt(); // FF FF FF FF
            final MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
            slea.readShort();
            boolean useFreezer = (slea.readByte() == 1);

            if (useFreezer) {
                int buffFreezer = 0;
                if (c.getPlayer().itemQuantity(5133000) > 0) {
                    buffFreezer = 5133000;
                } else if (c.getPlayer().itemQuantity(5133001) > 0) {
                    buffFreezer = 5133001;
                }

                if (buffFreezer > 0) {
                    c.getPlayer().setUseBuffFreezer(true);
                    c.getPlayer().removeItem(buffFreezer, -1);

                    c.getSession().writeAndFlush(CField.buffFreezer(buffFreezer, useFreezer));
                }
            }

            if (chr.getMapId() == 180010003) {
                chr.getStat().setHp((short) 50, chr);
                final MapleMap map = chr.getMap();
                MapleMap to = null;
                if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                    to = map.getForcedReturnMap();
                } else {
                    to = map.getReturnMap();
                }
                chr.changeMap(to, to.getPortal(0));
                NPCScriptManager.getInstance().start(c, 2007);
                return;
            }

            final boolean wheel = useFreezer && chr.haveItem(5510000, 1, false, true) && chr.getMapId() / 1000000 != 925;

            if (targetid != -1 && !chr.isAlive()) {
                chr.setStance(0);
                if (chr.getEventInstance() != null && chr.getEventInstance().revivePlayer(chr) && chr.isAlive()) {
                    return;
                }

                if (!chr.isUseBuffFreezer()) {
                    if (GameConstants.isViper(chr.getJob())) {
                        chr.energy = 0;
                        chr.energyCharge = false;

                        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                        MapleStatEffect energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
                        if (energyCharge == null) {
                            if (chr.getSkillLevel(5120018) > 0) {
                                SkillFactory.getSkill(5120018).getEffect(chr.getSkillLevel(5120018)).applyTo(chr, false);
                            } else if (chr.getSkillLevel(5110014) > 0) {
                                SkillFactory.getSkill(5110014).getEffect(chr.getSkillLevel(5110014)).applyTo(chr, false);
                            } else {
                                SkillFactory.getSkill(5100015).getEffect(chr.getSkillLevel(5100015)).applyTo(chr, false);
                            }
                            energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
                        }

                        energyCharge.setEnergyChargeCooling(false);
                        energyCharge.setEnergyChargeActived(false);

                        statups.put(MapleBuffStat.EnergyCharged, new Pair<>(chr.energy, 0));

                        chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, energyCharge, chr));
                        chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, energyCharge), false);
                    }

                    chr.cancelAllBuffs_();
                }

                if (wheel) {
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                    c.getSession().writeAndFlush(EffectPacket.showWheelEffect(5510000));
                    chr.getStat().setHp(chr.getStat().getCurrentMaxHp(), chr);
                    final MapleMap to = chr.getMap();
                    chr.changeMap(to, to.getPortal(0));
                } else if (chr.getDeathCount() > 0 || chr.liveCounts() > 0) {
                    chr.getStat().setHp(chr.getStat().getCurrentMaxHp(), chr);
                    chr.getStat().setMp(chr.getStat().getCurrentMaxMp(chr), chr);
                    MapleMap to = chr.getMap();
                    if (chr.getMapId() == 272020200) {
                        to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(272020400);
                    } else if (chr.getMapId() == 260230300) {
                        to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(260230310);
                    }
                    chr.changeMap(to, to.getPortal(0));
                } else {
                    chr.getStat().setHp((short) 50, chr);
                    final MapleMap map = chr.getMap();
                    MapleMap to = null;
                    if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                        to = map.getForcedReturnMap();
                    } else {
                        to = map.getReturnMap();
                    }

                    chr.changeMap(to, to.getPortal(0));
                }

                if (chr.getSkillLevel(80000329) > 0) {
                    SkillFactory.getSkill(80000329).getEffect(chr.getSkillLevel(80000329)).applyTo(chr, false);
                }
            } else if (targetid != -1 && chr.isIntern()) {
                final MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                if (to != null) {
                    chr.changeMap(to, to.getPortal(0));
                } else {
                    chr.dropMessage(5, "Map is NULL. Use !warp <mapid> instead.");
                }
            } else if (targetid != -1 && !chr.isIntern()) {
                final int divi = chr.getMapId() / 100;
                boolean unlock = false, warp = false;
                if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack
                    warp = targetid / 100 == 9130400 || targetid / 100 == 9130401; // Cygnus introduction
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9130400) { // Only allow warp if player is already in Intro map, or else = hack
                    warp = targetid / 100 == 9130400 || targetid / 100 == 9130401; // Cygnus introduction
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9140900) { // Aran Introductio
                    warp = targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000;
                } else if (divi == 9120601 || divi == 9140602 || divi == 9140603 || divi == 9140604 || divi == 9140605) {
                    warp = targetid == 912060100 || targetid == 912060200 || targetid == 912060300 || targetid == 912060400 || targetid == 912060500 || targetid == 3000100;
                    unlock = true;
                } else if (divi == 9101500) {
                    warp = targetid == 910150006 || targetid == 101050010;
                    unlock = true;
                } else if (divi == 9140901 && targetid == 140000000) {
                    unlock = true;
                    warp = true;
                } else if (divi == 9240200 && targetid == 924020000) {
                    unlock = true;
                    warp = true;
                } else if (targetid == 980040000 && divi >= 9800410 && divi <= 9800450) {
                    warp = true;
                } else if (divi == 9140902 && (targetid == 140030000 || targetid == 140000000)) { //thing is. dont really know which one!
                    unlock = true;
                    warp = true;
                } else if (divi == 9000900 && targetid / 100 == 9000900 && targetid > chr.getMapId()) {
                    warp = true;
                } else if (divi / 1000 == 9000 && targetid / 100000 == 9000) {
                    unlock = targetid < 900090000 || targetid > 900090004; //1 movie
                    warp = true;
                } else if (divi / 10 == 1020 && targetid == 1020000) { // Adventurer movie clip Intro
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 900090101 && targetid == 100030100) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 2010000 && targetid == 104000000) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 106020001 || chr.getMapId() == 106020502) {
                    if (targetid == (chr.getMapId() - 1)) {
                        unlock = true;
                        warp = true;
                    }
                } else if (chr.getMapId() == 0 && targetid == 10000) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 931000011 && targetid == 931000012) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 931000021 && targetid == 931000030) {
                    unlock = true;
                    warp = true;
                }

                if (unlock) {
                    c.getSession().writeAndFlush(UIPacket.IntroDisableUI(false));
                    c.getSession().writeAndFlush(UIPacket.IntroLock(false));
                    c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
                }
                if (warp) {
                    final MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                }
            } else if (portal != null) {
                portal.enterPortal(c);
            } else {
                c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
            }
        }
    }

    public static final void InnerPortal(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
        final int toX = slea.readShort();
        final int toY = slea.readShort();

        if (portal == null) {
            return;
        } else if (portal.getPosition().distanceSq(chr.getTruePosition()) > 22500 && !chr.isGM()) {
            return;
        }
        chr.getMap().movePlayer(chr, new Point(toX, toY));

        chr.checkFollow();
    }

    public static final void snowBall(LittleEndianAccessor slea, MapleClient c) {
        //B2 00
        //01 [team]
        //00 00 [unknown]
        //89 [position]
        //01 [stage]
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        //empty, we do this in closerange
    }

    public static final void UpdateDamageSkin(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte status = slea.readByte();
        switch (status) {
            case 0: {
                long skinroom = chr.getKeyValue(13191, "skinroom") == -1 ? 0 : chr.getKeyValue(13191, "skinroom");
                long skinsize = chr.getKeyValue(13191, "skins") == -1 ? 0 : chr.getKeyValue(13191, "skins");
                if (skinsize < skinroom) {
                    boolean isalready = false;
                    for (int i = 0; i < skinsize; i++) {
                        if (chr.getKeyValue(13191, i + "") == GameConstants.getDSkinNum((int) chr.getKeyValue(7293, "damage_skin"))) {
                            isalready = true;
                        }
                    }
                    if (!isalready) {
                        chr.setKeyValue(13191, "skins", (skinsize + 1) + "");
                        chr.setKeyValue(13191, skinsize + "", GameConstants.getDSkinNum((int) chr.getKeyValue(7293, "damage_skin")) + "");
                    }
                }
                break;
            }
            case 1:
            case 2: {
                int skinid = slea.readShort();
                if (status == 1) {
                    boolean finded = false;
                    long skinsize = chr.getKeyValue(13191, "skins") == -1 ? 0 : chr.getKeyValue(13191, "skins");
                    for (int i = 0; i < skinsize; i++) {
                        if (chr.getKeyValue(13191, i + "") == skinid) {
                            finded = true;
                        }
                        if (finded) {
                            if (chr.getKeyValue(13191, (i + 1) + "") != -1) {
                                chr.setKeyValue(13191, i + "", chr.getKeyValue(13191, (i + 1) + "") + "");
                                if ((i + 1) == skinsize || chr.getKeyValue(13191, (i + 2) + "") == -1) {
                                    chr.removeKeyValue(13191, (i + 1) + "");
                                }
                            }
                        }
                    }
                    if (finded) {
                        chr.setKeyValue(13191, "skins", (skinsize - 1) + "");
                    }
                } else {
                    boolean finded = false;
                    long skinsize = chr.getKeyValue(13191, "skins") == -1 ? 0 : chr.getKeyValue(13191, "skins");
                    for (int i = 0; i < skinsize; i++) {
                        if (chr.getKeyValue(13191, i + "") == skinid) {
                            finded = true;
                        }
                    }
                    if (finded) {
                        MapleQuest quest = MapleQuest.getInstance(7291);
                        MapleQuestStatus queststatus = new MapleQuestStatus(quest, (byte) 1);
                        String skinString = String.valueOf(skinid);
                        queststatus.setCustomData(skinString == null ? "0" : skinString);
                        chr.updateQuest(queststatus, true);
                        chr.setKeyValue(7293, "damage_skin", String.valueOf(GameConstants.getItemIdbyNum(skinid)));
                        chr.dropMessage(5, "데미지 스킨이 변경되었습니다.");
                        chr.getMap().broadcastMessage(chr, CField.showForeignDamageSkin(chr, skinid), false);
                    }
                }
                break;
            }

        }
        chr.updateDamageSkin();
        c.getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    static int Rank = 0;

    public static final void ChangeInner(LittleEndianAccessor slea, MapleClient c) {
        int rank = 0;
        if (c.getPlayer().getInnerSkills().size() > 0)
            rank = c.getPlayer().getInnerSkills().get(0).getRank();
        int count = slea.readInt(); //고정한 어빌리티 갯수
        int consume = 100 + (rank == 1 ? 100 : rank == 2 ? 1400 : rank == 3 ? 7900 : 0) + (count == 1 ? (rank == 2 ? 1500 : 3000) : count == 2 ? (rank == 2 ? 2500 : 5000) : 0);
        c.getPlayer().addHonorExp(-consume);
        List<InnerSkillValueHolder> newValues = new LinkedList<InnerSkillValueHolder>();
        int i = 1;
        int line = count >= 1 ? slea.readInt() : 0;
        int line2 = count >= 2 ? slea.readInt() : 0;
        boolean check_rock = false;
        InnerSkillValueHolder ivholder = null;
        InnerSkillValueHolder ivholder2 = null;
        for (InnerSkillValueHolder isvh : c.getPlayer().getInnerSkills()) {
            switch (count) {
                case 1:
                    check_rock = line == i;
                    break;
                case 2:
                    check_rock = line == i || line2 == i;
                    break;
                default:
                    check_rock = false;
                    break;
            }
            if (check_rock) {
                newValues.add(isvh);
                if (ivholder == null) {
                    ivholder = isvh;
                } else if (ivholder2 == null) {
                    ivholder2 = isvh;
                }
            } else if (ivholder == null) { //1번째
                int nowrank = -1;
                int rand = Randomizer.nextInt(100);
                if (isvh.getRank() == 3) {
                    nowrank = 3; // 유지
                } else if (isvh.getRank() == 2) {
                    if (rand < 5) {
                        nowrank = 3; // 상승
                    } else {
                        nowrank = 2; // 유지
                    }
                } else if (isvh.getRank() == 1) {
                    if (rand < 10) {
                        nowrank = 2; // 상승
                    } else {
                        nowrank = 1; // 유지
                    }
                } else if (rand < 40) {
                    nowrank = 1; // 상승
                } else {
                    nowrank = 0; // 유지
                }
                ivholder = InnerAbillity.getInstance().renewSkill(nowrank, false);
                boolean breakout = false;
                while (!breakout) {
                    if (count != 0) {
                        if (count == 1) {
                            if (ivholder.getSkillId() == c.getPlayer().getInnerSkills().get(line - 1).getSkillId()) {
                                ivholder = InnerAbillity.getInstance().renewSkill(nowrank, false);
                            } else {
                                breakout = true;
                            }
                        } else if (ivholder.getSkillId() == c.getPlayer().getInnerSkills().get(line - 1).getSkillId() || ivholder.getSkillId() == c.getPlayer().getInnerSkills().get(line2 - 1).getSkillId()) {
                            ivholder = InnerAbillity.getInstance().renewSkill(nowrank, false);
                        } else {
                            breakout = true;
                        }
                    } else {
                        ivholder = InnerAbillity.getInstance().renewSkill(nowrank, false);
                        breakout = true;
                    }
                }
                newValues.add(ivholder);
            } else if (ivholder2 == null) {
                ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                boolean breakout = false;
                while (!breakout) {
                    breakout = true;
                    if (count != 0) {
                        if (count == 1) {
                            if (ivholder2.getSkillId() == c.getPlayer().getInnerSkills().get(line - 1).getSkillId()) {
                                ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                                breakout = false;
                            }
                        } else if (ivholder2.getSkillId() == c.getPlayer().getInnerSkills().get(line - 1).getSkillId() || ivholder2.getSkillId() == c.getPlayer().getInnerSkills().get(line2 - 1).getSkillId()) {
                            ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                            breakout = false;
                        }
                    }
                    if (ivholder.getSkillId() == ivholder2.getSkillId()) {
                        ivholder2 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                        breakout = false;
                    }
                }
                newValues.add(ivholder2);
            } else {
                InnerSkillValueHolder ivholder3 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                while (ivholder.getSkillId() == ivholder3.getSkillId() || ivholder2.getSkillId() == ivholder3.getSkillId()) {
                    ivholder3 = InnerAbillity.getInstance().renewSkill(ivholder.getRank() == 0 ? 0 : ivholder.getRank(), false);
                }
                newValues.add(ivholder3);
            }
            c.getPlayer().changeSkillLevel_Inner(SkillFactory.getSkill(isvh.getSkillId()), (byte) 0, (byte) 0);
            i++;
        }

        c.getPlayer().getInnerSkills().clear();
        for (InnerSkillValueHolder isvh : newValues) {
            c.getPlayer().getInnerSkills().add(isvh);
            c.getPlayer().changeSkillLevel_Inner(SkillFactory.getSkill(isvh.getSkillId()), isvh.getSkillLevel(), isvh.getSkillLevel());
            c.getPlayer().getClient().getSession().writeAndFlush(CField.updateInnerAbility(isvh, c.getPlayer().getInnerSkills().size(), c.getPlayer().getInnerSkills().size() == 3));
        }
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
        c.getPlayer().dropMessage(5, "어빌리티 재설정에 성공 하였습니다.");

    }

    public static void absorbingRegen(LittleEndianAccessor slea, final MapleClient c) {
        int wsize = slea.readInt();
        int uu = slea.readInt(); // skillId or 0
        int fsize = slea.readInt();

        //why Exist :C
        for (int i = 0; i < fsize; ++i) {
            int skillId = slea.readInt();
            int attackCount = slea.readInt();
            int unk = slea.readInt();
            int x = slea.readInt();
            int y = slea.readInt();
        }

        for (int i = 0; i < wsize; ++i) {
            int attackCount = slea.readInt();
            slea.skip(1);
            int nextObjectId = slea.readInt();
            slea.skip(4); // Tick
            int prevObjectId = slea.readInt();
            int unk = slea.readInt();
            int x = slea.readInt();
            int y = slea.readInt();
            slea.skip(1);
            int skillId = slea.readInt();
            ForceAtom forceAtom;
            if (skillId == 400031000) {
                return;
            }
            if (c.getPlayer() == null || SkillFactory.getSkill(skillId) == null) {
                return;
            }

            MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(c.getPlayer().getSkillLevel(GameConstants.getLinkedSkill(skillId)));

            switch (skillId) {
                case 14000028:
                case 14000029: {
                    int skillids[] = {14100027, 14110029, 14120008};
                    int BatLimit = 3;
                    for (int skill : skillids) {
                        BatLimit += SkillFactory.getSkill(skill).getEffect(c.getPlayer().getSkillLevel(skill)).getMobCount();
                    }
                    if (attackCount < BatLimit) {

                        List<MapleMapObject> mobs_objects = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 250000, Arrays.asList(MapleMapObjectType.MONSTER));

                        if (mobs_objects.size() > 0) {
                            MapleAtom atom = new MapleAtom(true, prevObjectId, 16, true, 14000029, x, y);
                            atom.setDwUserOwner(c.getPlayer().getId());

                            forceAtom = new ForceAtom(c.getPlayer().getSkillLevel(14120008) > 0 ? 2 : 1, 5, 5, Randomizer.rand(0, 45), (short) Randomizer.rand(10, 30));
                            forceAtom.setnAttackCount(attackCount + 1);

                            atom.setDwFirstTargetId(mobs_objects.get(Randomizer.nextInt(mobs_objects.size())).getObjectId());
                            atom.addForceAtom(forceAtom);

                            c.getPlayer().getMap().spawnMapleAtom(atom);
                        }
                    }
                    break;
                }
                case 31221014: {
                    if (attackCount < effect.getZ()) {
                        MapleAtom atom = new MapleAtom(true, nextObjectId, 4, true, 31221014, x, y);
                        atom.setDwUserOwner(c.getPlayer().getId());

                        forceAtom = new ForceAtom(3, Randomizer.rand(40, 45), Randomizer.rand(3, 4), Randomizer.rand(10, 340), (short) 0);
                        forceAtom.setnAttackCount(attackCount + 1);

                        atom.setDwFirstTargetId(nextObjectId);
                        atom.addForceAtom(forceAtom);

                        c.getPlayer().getMap().spawnMapleAtom(atom);
                    }
                    break;
                }
                case 65111007: {
                    if (attackCount < 8) {
                        int prop = effect.getProp();
                        if (c.getPlayer().getSkillLevel(65120044) > 0) {
                            prop += SkillFactory.getSkill(65120044).getEffect(1).getProp();
                        }
                        if (Randomizer.isSuccess(prop)) {
                            MapleAtom atom = new MapleAtom(true, nextObjectId, 4, true, 65111007, x, y);
                            atom.setDwUserOwner(c.getPlayer().getId());

                            forceAtom = new ForceAtom(1, Randomizer.rand(40, 45), Randomizer.rand(3, 4), Randomizer.rand(10, 340), (short) 0);
                            forceAtom.setnAttackCount(attackCount + 1);

                            atom.setDwFirstTargetId(nextObjectId);
                            atom.addForceAtom(forceAtom);

                            c.getPlayer().getMap().spawnMapleAtom(atom);
                        }
                    }
                    break;
                }
                case 65120011: {
                    int prop = effect.getProp();
                    if (c.getPlayer().getSkillLevel(65120044) > 0) {
                        prop += SkillFactory.getSkill(65120044).getEffect(1).getProp();
                    }
                    if (Randomizer.isSuccess(prop)) {
                        MapleAtom atom = new MapleAtom(true, nextObjectId, 26, true, 65120011, x, y);
                        atom.setDwUserOwner(c.getPlayer().getId());

                        forceAtom = new ForceAtom(1, Randomizer.rand(40, 45), Randomizer.rand(3, 4), Randomizer.rand(10, 340), (short) 0);
                        forceAtom.setnAttackCount(attackCount + 1);

                        atom.setDwFirstTargetId(nextObjectId);
                        atom.addForceAtom(forceAtom);

                        c.getPlayer().getMap().spawnMapleAtom(atom);
                    }
                    break;
                }
                case 400041023: {
                    if ((attackCount == effect.getZ() || nextObjectId == 0) && !c.getPlayer().useBlackJack) {
                        c.getPlayer().getMap().broadcastMessage(CField.blackJack(new Point(x, y)));
                    } else if (attackCount < effect.getZ() && !c.getPlayer().useBlackJack) {
                        MapleAtom atom = new MapleAtom(true, nextObjectId, 33, true, 400041023, x, y);
                        atom.setDwUserOwner(c.getPlayer().getId());

                        forceAtom = new ForceAtom(32, 31, 21, 144, (short) 0);
                        forceAtom.setnAttackCount(attackCount + 1);

                        atom.setDwFirstTargetId(nextObjectId);
                        atom.addForceAtom(forceAtom);

                        c.getPlayer().getMap().spawnMapleAtom(atom);
                    }
                    break;
                }
            }
        }
    }

    public static void ZeroScrollUI(int scroll, MapleClient c) {
        c.getSession().writeAndFlush(CField.ZeroScroll(scroll));
    }

    public static void ZeroScrollLucky(final LittleEndianAccessor slea, final MapleClient c) {
        int s_type = slea.readInt();
        int pos = slea.readShort();
        c.getPlayer().setZeroCubePosition(pos);
        Equip equip1 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        Equip equip2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        Equip nEquip = (Equip) equip1;
        Equip nEquip2 = (Equip) equip2;
        if ((equip1.getItemId() > 1560000) && (equip1.getItemId() < 1570000) && (equip2.getItemId() > 1572000) && (equip2.getItemId() < 1573000)) {
            InventoryHandler.UseUpgradeScroll(null, (byte) pos, (byte) nEquip.getPosition(), (byte) 0, c, c.getPlayer());
            InventoryHandler.UseUpgradeScroll(null, (byte) pos, (byte) nEquip2.getPosition(), (byte) 0, c, c.getPlayer());
            c.getPlayer().setZeroCubePosition(-1);
        }
    }

    public static void ZeroScroll(final LittleEndianAccessor slea, final MapleClient c) {
        int s_type = slea.readInt();
        int pos = slea.readInt();
        slea.skip(8);
        int s_pos = slea.readInt();

        c.getPlayer().setZeroCubePosition(pos);

        Equip equip1 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        Equip equip2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        Equip nEquip2 = (Equip) equip2;
        if ((equip1.getItemId() > 1560000) && (equip1.getItemId() < 1570000) && (equip2.getItemId() > 1572000) && (equip2.getItemId() < 1573000)) {
            InventoryHandler.UseUpgradeScroll(null, (byte) pos, (byte) nEquip2.getPosition(), (byte) 0, c, c.getPlayer());
            c.getPlayer().setZeroCubePosition(-1);
        }
    }

    public static void ZeroScrollStart(RecvPacketOpcode header, final LittleEndianAccessor slea, final MapleClient c) {
        c.getSession().writeAndFlush(CField.ZeroScrollStart());
        Equip equip1 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        Equip equip2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        Equip nEquip = (Equip) equip1;
        Equip nEquip2 = (Equip) equip2;
        if (c.getPlayer().getZeroCubePosition() > 0 && (equip1.getItemId() > 1560000) && (equip1.getItemId() < 1570000) && (equip2.getItemId() > 1572000) && (equip2.getItemId() < 1573000)) {
            InventoryHandler.UseUpgradeScroll(header, (byte) c.getPlayer().getZeroCubePosition(), (byte) nEquip.getPosition(), (byte) 0, c, c.getPlayer());
            InventoryHandler.UseUpgradeScroll(header, (byte) c.getPlayer().getZeroCubePosition(), (byte) nEquip2.getPosition(), (byte) 0, c, c.getPlayer());
            c.getPlayer().setZeroCubePosition(-1);
        }
    }

    public static void ZeroWeaponInfo(final LittleEndianAccessor slea, final MapleClient c) {
        MapleCharacter player = c.getPlayer();
        final Item alpha = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        int action = 1, level = 0, type = 0, itemid = 0, quantity = 0;
        switch (alpha.getItemId()) {
            case 1562000:
                type = 1;
                level = 100;
                break;
            case 1562001:
                type = 2;
                level = 110;
                break;
            case 1562002:
                type = 2;
                level = 120;
                break;
            case 1562003:
                type = 2;
                level = 130;
                break;
            case 1562004:
                type = 4;
                level = 140;
                break;
            case 1562005:
                type = 5;
                level = 150;
                break;
            case 1562006:
                type = 6;
                level = 160;
                break;
            case 1562007:
                type = 7;
                level = 160;
                itemid = 4310216;
                quantity = 1;
                break;
            case 1562008:
                type = 8;
                level = 200;
                itemid = 4310217;
                quantity = 1;
                break;
            case 1562009:
                type = 9;
                level = 200;
                itemid = 4310260;
                quantity = 1;
                break;
            case 1562010:
                action = 0;
                type = 0;
                level = 0;
                break;
        }
        if (player.getLevel() < level) {
            action = 0;
        }
        c.getSession().writeAndFlush(CField.WeaponInfo(type, level, action, alpha.getItemId(), itemid, quantity));
    }

    public static void ZeroWeaponLevelUp(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(7);
        final Item alpha = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        final Item beta = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        byte betatype = 11;
        byte alphatype = 12;
        Equip nalphatype = (Equip) alpha;
        Equip nbetatype = (Equip) beta;
        if (nbetatype.getItemId() == 1562007) {
            if (!c.getPlayer().haveItem(4310216, 1)) {
                return;
            }
        } else if (nbetatype.getItemId() == 1562008) {
            if (!c.getPlayer().haveItem(4310217, 1)) {
                return;
            }
        } else if (nbetatype.getItemId() == 1562009) {
            if (!c.getPlayer().haveItem(4310260, 1)) {
                return;
            }
        }
        nbetatype.setItemId(nbetatype.getItemId() + 1);
        nalphatype.setItemId(nalphatype.getItemId() + 1);
        if (nbetatype.getItemId() == 1562001) {
            nalphatype.setWatk((short) 100);

            nbetatype.setWatk((short) 102);
            nbetatype.setWdef((short) 80);
            nbetatype.setMdef((short) 35);
            nalphatype.addUpgradeSlots((byte) 7);
            nbetatype.addUpgradeSlots((byte) 7);
        } else if (nbetatype.getItemId() == 1562002) {
            nalphatype.addWatk((short) 3); // 103

            nbetatype.addWatk((short) 3); // 105
            nbetatype.addWdef((short) 10); // 90
            nbetatype.addMdef((short) 5); // 40
        } else if (nbetatype.getItemId() == 1562003) {
            nalphatype.addWatk((short) 2); // 105

            nbetatype.addWatk((short) 2); // 107
            nbetatype.addWdef((short) 10); // 100
            nbetatype.addMdef((short) 5); // 45
        } else if (nbetatype.getItemId() == 1562004) {
            nalphatype.addWatk((short) 7); // 112

            nbetatype.addWatk((short) 7); // 114
            nbetatype.addWdef((short) 10); // 110
            nbetatype.addMdef((short) 5); // 50
        } else if (nbetatype.getItemId() == 1562005) {
            nalphatype.addStr((short) 8);
            nalphatype.addDex((short) 4);
            nalphatype.addWatk((short) 5); // 117
            nalphatype.addAcc((short) 50); // 50
            nalphatype.addUpgradeSlots((byte) 1);

            nbetatype.addStr((short) 8);
            nbetatype.addDex((short) 4);
            nbetatype.addWatk((short) 7); // 121
            nbetatype.addWdef((short) 10); // 120
            nbetatype.addMdef((short) 5); // 55
            nbetatype.addAcc((short) 50); // 50
            nbetatype.addUpgradeSlots((byte) 1);
        } else if (nbetatype.getItemId() == 1562006) {
            nalphatype.addStr((short) 27); // 35
            nalphatype.addDex((short) 16); // 20
            nalphatype.addWatk((short) 18); // 135
            nalphatype.addAcc((short) 50); // 100

            nbetatype.addStr((short) 27); // 35
            nbetatype.addDex((short) 16);  // 20
            nbetatype.addWatk((short) 18); // 139
            nbetatype.addWdef((short) 10); // 130
            nbetatype.addMdef((short) 5); // 60
            nbetatype.addAcc((short) 50); // 100
        } else if (nbetatype.getItemId() == 1562007) {
            nalphatype.addStr((short) 5); // 40
            nalphatype.addDex((short) 20); // 40
            nalphatype.addWatk((short) 34); // 169
            nalphatype.addAcc((short) 20); // 120
            nalphatype.addBossDamage((byte) 30); // 30
            nalphatype.addIgnoreWdef((short) 10); // 10

            nbetatype.addStr((short) 5); // 40
            nbetatype.addDex((short) 20);  // 40
            nbetatype.addWatk((short) 34); // 174
            nbetatype.addWdef((short) 20); // 150
            nbetatype.addMdef((short) 10); // 70
            nbetatype.addAcc((short) 20); // 120
            nbetatype.addBossDamage((byte) 30); // 30
            nbetatype.addIgnoreWdef((short) 10); // 10
        } else if (nbetatype.getItemId() == 1562008) {
            nalphatype.addStr((short) 20); // 60
            nalphatype.addDex((short) 20); // 60
            nalphatype.addWatk((short) 34); // 169
            nalphatype.addAcc((short) 20); // 120

            nbetatype.addStr((short) 20); // 60
            nbetatype.addDex((short) 20);  // 60
            nbetatype.addWatk((short) 34); // 174
            nbetatype.addWdef((short) 10); // 150
            nbetatype.addMdef((short) 10); // 70
            nbetatype.addAcc((short) 20); // 120
            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4310216, 1, false, false);
        } else if (nbetatype.getItemId() == 1562009) {
            nalphatype.addStr((short) 40); // 40
            nalphatype.addDex((short) 40); // 40
            nalphatype.addWatk((short) 90); // 169
            nalphatype.addAcc((short) 20); // 120
            nalphatype.addIgnoreWdef((short) 10); // 10

            nbetatype.addStr((short) 40);
            nbetatype.addDex((short) 40);
            nbetatype.addWatk((short) 90); // 174
            nbetatype.addWdef((short) 40); // 150
            nbetatype.addMdef((short) 40); // 70
            nbetatype.addAcc((short) 20); // 120
            nbetatype.addIgnoreWdef((short) 10); // 10
            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4310217, 1, false, false);
        } else if (nbetatype.getItemId() == 1562010) {
            nalphatype.addStr((short) 50); // 40
            nalphatype.addDex((short) 50); // 40
            nalphatype.addWatk((short) 44); // 169
            nalphatype.addAcc((short) 20); // 120

            nbetatype.addStr((short) 50);
            nbetatype.addDex((short) 50);
            nbetatype.addWatk((short) 45); // 174
            nbetatype.addWdef((short) 50); // 150
            nbetatype.addMdef((short) 50); // 70
            nbetatype.addAcc((short) 20); // 120
            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4310260, 1, false, false);
        }
        c.getSession().writeAndFlush(CField.WeaponLevelUp());
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, nalphatype));
        c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, nbetatype));
    }

    public static void ZeroTag(final LittleEndianAccessor slea, final MapleClient c) {
        MapleCharacter player = c.getPlayer();
        slea.skip(4); //RealTimeTF
        slea.skip(4); //ChangeTF

        c.getSession().writeAndFlush(CField.ZeroTag(player, player.getSecondGender())); //제로 어시시트 이펙트
        player.getMap().broadcastMessage(CField.MultiTag(player)); //제로 어시시트 이펙트

        if (player.getGender() == 0 && player.getSecondGender() == 1) {
            player.setGender((byte) 1);
            player.setSecondGender((byte) 0);
        } else if (player.getGender() == 1 && player.getSecondGender() == 0) {
            player.setGender((byte) 0);
            player.setSecondGender((byte) 1);
            player.armorSplit = 0;
        }

        player.getMap().broadcastMessage(player, CField.ZeroTagUpdateCharLook(player), player.getPosition()); //상대방 메인 캐릭터
    }

    public static void ZeroTagRemove(final MapleClient c) {
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.MultiTagRemove(c.getPlayer().getId()), false);
    }

    public static void subActiveSkills(final LittleEndianAccessor slea, final MapleClient c) {
        int skillid = slea.readInt();
        switch (skillid) {
            case 1201012: //釉붾━?먮뱶 李⑥?
                slea.skip(4);
                int mobId = slea.readInt();
                final MapleStatEffect effect = SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getTotalSkillLevel(skillid));
                final MapleMonster mob = c.getPlayer().getMap().getMonsterByOid(mobId);
                if (mob != null) {
                    if (effect.makeChanceResult()) {
                        mob.applyStatus(c, MonsterStatus.MS_Freeze, new MonsterStatusEffect(skillid, effect.getDuration()), 1, effect);
                    }
                }
                break;
            default:
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "SUB ACTIVE SKILL : " + skillid);
                }
                break;
        }
    }

    public static void ZeroClothes(final LittleEndianAccessor slea, final MapleClient c) {
        int kind = slea.readInt(); // 좌표
        byte check = slea.readByte(); // 0 : 해제 or 동시 미착용, 1 : 착용
        int value = ClothesStats.getValueByOrder(kind), bc = c.getPlayer().getBetaClothes();
        if (check == 1 && (bc & value) == 0) {
            c.getPlayer().pBetaClothes(value);
        } else if (check == 0 && (bc & value) != 0) {
            c.getPlayer().mBetaClothes(value);
        }
        c.getSession().writeAndFlush(CField.Clothes(c.getPlayer().getBetaClothes()));
        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
    }

    public static void FieldAttackObjAttack(final LittleEndianAccessor slea, final MapleCharacter chr) {
        short type = slea.readShort();
        int cid = slea.readInt();
        int key = slea.readInt();
        if (chr.getId() != cid) {
            return;
        }
        if (type == 0) {
            byte type2 = slea.readByte();
            Point pos = slea.readPos();
            Point oldPos = null;
            if (type2 == 5) {
                oldPos = slea.readPos();
            }
            short unk1 = slea.readShort();
            int sourceid = slea.readInt();
            int level = slea.readInt();
            int duration = slea.readInt();
            short unk2 = slea.readShort();
            chr.getMap().broadcastMessage(CField.B2BodyResult(chr, type, type2, key, pos, oldPos, unk1, sourceid, level, duration, unk2, chr.isFacingLeft(), 0, 0, null));
            chr.getMap().broadcastMessage(CField.spawnSubSummon(type, key));
        } else if (type == 3) {
            int sourceid = slea.readInt();
            int level = slea.readInt();
            int unk3 = slea.readInt();
            int unk4 = slea.readInt();
            chr.getMap().broadcastMessage(CField.B2BodyResult(chr, type, (short) 0, key, null, null, (short) 0, sourceid, level, 0, (short) 0, chr.isFacingLeft(), unk3, unk4, null));
            chr.getMap().broadcastMessage(CField.spawnSubSummon(type, key));
        } else if (type == 4) {
            slea.skip(4); // oldPos
            Point pos = slea.readPos();
            if (slea.available() > 0) {
                int sourceid = slea.readInt();
                boolean facingleft = slea.readByte() == 1;
                slea.skip(10);
//                if (sourceid == 3111013 || sourceid == 5220025 || sourceid == 95001000) {
//                    int duration = slea.readInt();

//                } else {
                short type2 = slea.readShort();
                short unk1 = slea.readShort();
                short unk2 = slea.readShort();
                byte unk3 = slea.readByte();
                String unk = "";
                if (unk3 > 0) {
                    unk = slea.readMapleAsciiString();
                }
                int unk4 = slea.readInt();
                Point oldPos = new Point(slea.readInt(), slea.readInt());
                MapleFieldAttackObj fao = new MapleFieldAttackObj(chr, sourceid, facingleft, pos, type2 * 1000);

                if (chr.getFao() == null) {
                    chr.setFao(fao);
                }

                chr.getMap().broadcastMessage(CField.spawnSubSummon(type, key));
                chr.getMap().broadcastMessage(CField.B2BodyResult(chr, (short) type, type2, key, pos, oldPos, unk1, sourceid, 0, 0, unk2, facingleft, unk3, unk4, unk));
//                }

            } else {
                //플레이어 스킬이 아닐 때는 여기인데, 아직까진 쓸 일이 없을 듯
                return;
            }
        }

//    	fao.onSetAttack(chr.getClient());
    }

    public static void FieldAttackObjAction(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final boolean isLeft = slea.readByte() > 0;
        final int x = slea.readInt();
        final int y = slea.readInt();
        boolean disable = chr.isDominant();

        for (MapleMapObject obj : chr.getMap().getMapObjectsInRange(chr.getTruePosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.FIELD))) {
            MapleFieldAttackObj fao = (MapleFieldAttackObj) obj;
            if (fao.getChr().getId() == chr.getId()) {
                List<MapleFieldAttackObj> removes = new ArrayList<>();
                removes.add(fao);
                chr.getMap().broadcastMessage(AttackObjPacket.ObjRemovePacketByList(removes));
                chr.getMap().removeMapObject(fao);
                break;
            }
        }

        if (chr.getFao() != null) {
            chr.getFao().setFacingleft(isLeft);
            chr.getFao().setPosition(new Point(x, y));
            chr.getMap().spawnFieldAttackObj(chr.getFao());
            chr.setDominant(!disable);
        }
    }

    public static void OrbitalFlame(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getMapId() == ServerConstants.WarpMap && !c.getPlayer().isGM()) {
            c.getPlayer().changeMap(ServerConstants.WarpMap, 0);
            //c.getPlayer().dropMessage(1, "스킬 쓰지마세요.");
            return;
        }
        MapleCharacter chr = c.getPlayer();
        int tempskill = slea.readInt();
        byte level = slea.readByte();
        int direction = slea.readShort();
        int skillid = 0;
        int elementid = 0;
        int effect = 0;
        switch (tempskill) {
            case 12001020:
                skillid = 12000026;
                elementid = 12000022;
                effect = 1;
                break;
            case 12100020:
                skillid = 12100028;
                elementid = 12100026;
                effect = 2;
                break;
            case 12110020:
                skillid = 12110028;
                elementid = 12110024;
                effect = 3;
                break;
            case 12120006:
                skillid = 12120010;
                elementid = 12120007;
                effect = 4;
                break;
        }
        MapleStatEffect flame = SkillFactory.getSkill(tempskill).getEffect(level);
        if (flame != null && chr.getSkillLevel(elementid) > 0) {
            if (!chr.getBuffedValue(elementid)) {
                MapleStatEffect element = SkillFactory.getSkill(elementid).getEffect(chr.getSkillLevel(elementid));
                element.applyTo(chr, false);
            }
        }

        MapleStatEffect orbital = SkillFactory.getSkill(skillid).getEffect(chr.getSkillLevel(skillid));
        orbital.applyTo(chr);

        MapleAtom atom = new MapleAtom(false, chr.getId(), 17, true, skillid, chr.getTruePosition().x, chr.getTruePosition().y);
        ForceAtom forceAtom = new ForceAtom(effect, 17, 17, 90, (short) 0);
        forceAtom.setnMaxHitCount(flame.getMobCount());
        atom.addForceAtom(forceAtom);
        atom.setDwTargets(new ArrayList<>());
        if (chr.getBuffedEffect(MapleBuffStat.AddRange) != null) {
            atom.setnArriveRange(flame.getRange() + chr.getBuffedValue(MapleBuffStat.AddRange));
        } else {
            atom.setnArriveRange(flame.getRange());
        }
        atom.setnArriveDir(direction);
        chr.getMap().spawnMapleAtom(atom);
    }

    public static void VoydPressure(final LittleEndianAccessor slea, final MapleCharacter chr) {
        List<Byte> arrays = new ArrayList<>();
        byte size = slea.readByte();
        for (int i = 0; i < size; ++i) {
            arrays.add(slea.readByte());
        }
        chr.getMap().broadcastMessage(chr, CField.showVoydPressure(chr.getId(), arrays), false);
    }

    public static void absorbingSword(final LittleEndianAccessor slea, final MapleCharacter chr) {
        int skill = slea.readInt();
        int mobSize = slea.readInt();

        MapleAtom atom = new MapleAtom(false, chr.getId(), skill == 400011058 ? 32 : 2, true, skill, chr.getTruePosition().x, chr.getTruePosition().y);
        List<Integer> monsters = new ArrayList<>();

        for (int i = 0; i < mobSize; i++) {
            monsters.add(slea.readInt());
            atom.addForceAtom(new ForceAtom(chr.getBuffedValue(61121217) ? 4 : 2, 18, Randomizer.rand(20, 40), 0, (short) Randomizer.rand(1000, 1500)));
        }
        while (atom.getForceAtoms().size() < (chr.getBuffedValue(61120007) || chr.getBuffedValue(61121217) ? 5 : 3)) {
            atom.addForceAtom(new ForceAtom(chr.getBuffedValue(61121217) ? 4 : 2, 18, Randomizer.rand(20, 40), 0, (short) Randomizer.rand(1000, 1500)));
        }
        if (skill != 0) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.StopForceAtominfo);

            atom.setDwTargets(monsters);
            chr.getMap().spawnMapleAtom(atom);

            if (skill == 400011058) {
                chr.ignoreDraco = 3;
            }
        }
    }

    public static void DressUpRequest(final MapleCharacter chr, final LittleEndianAccessor slea) {
        int code = slea.readInt();
        switch (code) {
            case 5010093:
                // 드레스업 해제
                chr.setDressup(false);
                chr.getMap().broadcastMessage(CField.updateCharLook(chr, chr.getDressup()));
                chr.getMap().broadcastMessage(CField.updateDress(code, chr));
                break;
            case 5010094:
                // 드레스업 요청
                chr.setDressup(true);
                chr.getMap().broadcastMessage(CField.updateCharLook(chr, chr.getDressup()));
                chr.getMap().broadcastMessage(CField.updateDress(code, chr));
                break;

        }
    }

    public static final void DressUpTime(final LittleEndianAccessor rh, final MapleClient c) { // ?ъ퐫???꾩슂??
        byte type = rh.readByte();
        if (type == 1) {
            if (GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.updateCharLook(c.getPlayer(), c.getPlayer().getDressup()), false);
            }
        } else {
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.updateCharLook(c.getPlayer(), c.getPlayer().getDressup()), false);
        }
    }

    public static final void test(final LittleEndianAccessor slea, MapleClient c) {
        slea.skip(6);
        c.getSession().writeAndFlush(CWvsContext.Test());
        c.getSession().writeAndFlush(CWvsContext.Test1());
    }

    public static final void PsychicGrabPreparation(final LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        try {
            int skillid = slea.readInt();
            short level = slea.readShort();
            int unk = slea.readInt();
            int speed = slea.readInt();
            int k = skillid == 142120000 ? 5 : 3;

            int[] unk2 = new int[k];
            int[] mob = new int[k];
            short[] unk3 = new short[k];
            byte[] unk4 = new byte[k];
            Point[] pos1 = new Point[k];
            Point[] pos2 = new Point[k];
            Point[] pos3 = new Point[k];
            Point[] pos4 = new Point[k];
            Point[] pos5 = new Point[k];
            for (int i = 0; i < k; i++) {
                slea.skip(1);
                unk2[i] = slea.readInt();
                mob[i] = slea.readInt();
                unk3[i] = slea.readShort();
                slea.skip(2);
                unk4[i] = slea.readByte();
                slea.skip(4);
                pos1[i] = slea.readPos();
                pos2[i] = slea.readPos();
                pos3[i] = slea.readPos();
                pos4[i] = slea.readPos();
                pos5[i] = slea.readPos();
            }
            c.getPlayer().givePPoint(skillid, false);
            c.getSession().writeAndFlush(CWvsContext.PsychicGrabPreparation(chr, skillid, level, unk, speed, unk2, mob, unk3, unk4, pos1, pos2, pos3, pos4, pos5));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void PsychicGrab(final LittleEndianAccessor slea, MapleCharacter chr, MapleClient c) {
        int skillid = slea.readInt(); // 142110003
/*        slea.readShort(); // skillLevel
         slea.readInt(); // unk
         slea.readInt(); // 
         slea.readInt();
         slea.readByte(); // boolean
         slea.readInt();

         int count = slea.readInt();

         for (int i = 0; i < count; i++) {
         slea.readInt();
         slea.readInt();
         }*/

        if (skillid == 142120002) {
            c.getPlayer().givePPoint(skillid, true);
        }
    }

    public static void MatrixSkill(LittleEndianAccessor slea, MapleClient c) {
        //sub_227EE70
        if (c.getPlayer().getMapId() == ServerConstants.WarpMap && !c.getPlayer().isGM()) {
            c.getPlayer().changeMap(ServerConstants.WarpMap, 0);
            //c.getPlayer().dropMessage(1, "스킬 쓰지마세요.");
            return;
        }
        int skillid = slea.readInt();
        int level = slea.readInt();
        AttackInfo ret = new AttackInfo();
        GameConstants.calcAttackPosition(slea, ret);
        int unk1 = slea.readInt();
        int unk2 = slea.readInt();
        int bullet = slea.readInt();
        int bc = 0;
        switch (skillid) {
            case 400031056: {
                if (c.getPlayer().getBuffedValue(400031055)) {
                    if (c.getPlayer().getBuffedValue(MapleBuffStat.RepeatinCartrige) == 0) {
                        c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RepeatinCartrige, 400031055);
                        c.getPlayer().RepeatinCartrige = 0;
                    }
                    c.getPlayer().RepeatinCartrige -= 1;
                    c.getPlayer().setBuffedValue(MapleBuffStat.RepeatinCartrige, c.getPlayer().RepeatinCartrige);
                }
                break;
            }
            case 400041001:
                bc = 0;
                break;
            case 400041016:
                bc = 2;
                break;
            case 400041017:
                bc = 3;
                break;
            case 400041018:
                bc = 4;
                break;
            case 400041020:
                bc = 1;
                break;
        }
        /*        if (bc > 0) {
         if (c.getPlayer().getBuffedEffect(MapleBuffStat.ShadowPartner) != null) {
         bc *= 2;
         }
         c.getPlayer().removeItem(bullet, bc);
         }*/
        slea.readInt();
        slea.readByte();
        slea.readInt();
        slea.readByte();
        slea.readShort();
        slea.readShort();
        byte enable2 = slea.readByte();
        List<Integer> data = new ArrayList<>();
        if (enable2 > 0) {
            data.add(slea.readInt());
            data.add(slea.readInt());
            data.add(slea.readInt());
            data.add(slea.readInt());
            data.add(slea.readInt());
            data.add((int) slea.readByte());
            data.add(slea.readInt());//?
        }
        List<MatrixSkill> skills = GameConstants.matrixSkills(slea);
        Skill skill = SkillFactory.getSkill(skillid);
        MapleStatEffect effect = skill.getEffect(c.getPlayer().getSkillLevel(skillid));
        c.getSession().writeAndFlush(CWvsContext.MatrixSkill(skillid, level, skills));
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CWvsContext.MatrixSkillMulti(c.getPlayer(), skillid, level, unk1, unk2, bullet, skills), false);

        if (effect.getCooldown(c.getPlayer()) > 0) {
            c.getSession().writeAndFlush(CField.skillCooldown(skillid, effect.getCooldown(c.getPlayer())));
            c.getPlayer().addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown(c.getPlayer()));
        }

        if (!GameConstants.isNoApplySkill(skillid)) {
            effect.applyTo(c.getPlayer(), false);
        }

        if (skillid == 400031026) {
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.skillCancel(c.getPlayer(), 0), false);
        }

        if (skillid == 400021048) {
            c.getPlayer().givePPoint(skillid, false);
        }

        if (GameConstants.isPathFinder(c.getPlayer().getJob())) {
            c.getPlayer().giveRelikGauge(skillid, null);
        }

        if (skillid == 151101001) {
            c.getPlayer().giveEtherGauge(skillid);
        }

        //   c.getPlayer().dropMessage(5, "스킬 : " + skillid);
        switch (skillid) {
            case 400011048: {
                c.getSession().writeAndFlush(CField.skillCooldown(400011048, effect.getCooldown(c.getPlayer())));
                c.getPlayer().addCooldown(400011048, System.currentTimeMillis(), effect.getCooldown(c.getPlayer()));
                break;
            }
            case 400011049: {
                c.getSession().writeAndFlush(CField.skillCooldown(400011049, effect.getCooldown(c.getPlayer())));
                c.getPlayer().addCooldown(400011049, System.currentTimeMillis(), effect.getCooldown(c.getPlayer()));
                break;
            }
            case 400051003: {
                if (c.getPlayer().transformEnergyOrb == 0) {
                    return;
                }

                MapleStatEffect eff = SkillFactory.getSkill(400051002).getEffect(c.getPlayer().getSkillLevel(400051002));
                c.getPlayer().transformEnergyOrb--;
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.IndiePmdR, new Pair<>((int) eff.getIndiePmdR(), (int) c.getPlayer().getBuffLimit(400051002)));
                statups.put(MapleBuffStat.Transform, new Pair<>(c.getPlayer().transformEnergyOrb, (int) c.getPlayer().getBuffLimit(400051002)));
                c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, c.getPlayer()));
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), BuffPacket.giveForeignBuff(c.getPlayer(), statups, eff), false);
                break;
            }
            case 400051008:
            case 400051042: {
                c.getPlayer().setBHGCCount(c.getPlayer().getBHGCCount() - 1);

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(c.getPlayer().getBHGCCount(), 0));

                c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, c.getPlayer()));
                break;
            }
        }
        if (GameConstants.isKain(c.getPlayer().getJob())) {
            int cooltime = effect.getCooltime() == 0 ? effect.getU() * 1000 : effect.getCooltime();
            if (skillid == 63101004) {
                c.getPlayer().KainscatteringShot -= 1;
                c.getPlayer().getClient().getSession().writeAndFlush(CField.KainStackSkill(63101004, c.getPlayer().KainscatteringShot, effect.getW(), cooltime));
                c.getPlayer().lastKainscatteringShot = System.currentTimeMillis();
            } else if (skillid == 63111003) {
                c.getPlayer().KainshaftBreak -= 1;
                c.getPlayer().getClient().getSession().writeAndFlush(CField.KainStackSkill(63111003, c.getPlayer().KainshaftBreak, effect.getW(), cooltime));
                c.getPlayer().lastKainshaftBreak = System.currentTimeMillis();
            } else if (skillid == 63101100 || skillid == 63111103) {
                c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Possession);
            }
        }
    }

    public static void UpdateSymbol(LittleEndianAccessor slea, MapleClient c, int plus) {
        try {
            int pos = slea.readInt() * -1;
            Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) pos);
            if (item.getArcEXP() >= GameConstants.ArcaneNextUpgrade(item.getArcLevel())) {
                if (c.getPlayer().getMeso() >= (12440000 + (6600000 * item.getArcLevel()))) {
                    c.getPlayer().gainMeso(-(12440000 + (6600000 * item.getArcLevel())), false);
                    item.setArcEXP(item.getArcEXP() - GameConstants.ArcaneNextUpgrade(item.getArcLevel()));
                    item.setArcLevel(item.getArcLevel() + 1);
                    item.setArc((short) (10 * (item.getArcLevel() + 2)));
                    if (GameConstants.isXenon(c.getPlayer().getJob())) {
                        item.setStr((short) (item.getStr() + 39));
                        item.setDex((short) (item.getDex() + 39));
                        item.setLuk((short) (item.getLuk() + 39));
                    } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                        item.setHp((short) (item.getHp() + 175));
                    } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                        item.setStr((short) (item.getStr() + 100));
                    } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                        item.setInt((short) (item.getInt() + 100));
                    } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                        item.setDex((short) (item.getDex() + 100));
                    } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                        item.setLuk((short) (item.getLuk() + 100));
                    } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                        item.setStr((short) (item.getStr() + 100));
                    }
                    c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));
                } else {
                    c.getPlayer().dropMessage(1, "메소가 부족합니다.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                }
            } else {
                c.getPlayer().dropMessage(1, "필요 성장치가 부족합니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void UpdateAsSymbol(LittleEndianAccessor slea, MapleClient c, int plus) {
        try {
            int pos = slea.readInt() * -1;
            Equip item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) pos);
            if (item.getArcEXP() >= GameConstants.AscenticNextUpgrade(item.getArcLevel())) {
                if (c.getPlayer().getMeso() >= (99000000 + (88500000 * item.getArcLevel()))) {
                    c.getPlayer().gainMeso(-(99000000 + (88500000 * item.getArcLevel())), false);
                    item.setArcEXP(item.getArcEXP() - GameConstants.AscenticNextUpgrade(item.getArcLevel()));
                    item.setArcLevel(item.getArcLevel() + 1);
                    item.setArc((short) (10 * (item.getArcLevel() + 2)));
                    if (GameConstants.isXenon(c.getPlayer().getJob())) {
                        item.setStr((short) (item.getStr() + 78));
                        item.setDex((short) (item.getDex() + 78));
                        item.setLuk((short) (item.getLuk() + 78));
                    } else if (GameConstants.isDemonAvenger(c.getPlayer().getJob())) {
                        item.setHp((short) (item.getHp() + 3500));
                    } else if (GameConstants.isWarrior(c.getPlayer().getJob())) {
                        item.setStr((short) (item.getStr() + 200));
                    } else if (GameConstants.isMagician(c.getPlayer().getJob())) {
                        item.setInt((short) (item.getInt() + 200));
                    } else if (GameConstants.isArcher(c.getPlayer().getJob()) || GameConstants.isCaptain(c.getPlayer().getJob()) || GameConstants.isMechanic(c.getPlayer().getJob()) || GameConstants.isAngelicBuster(c.getPlayer().getJob())) {
                        item.setDex((short) (item.getDex() + 200));
                    } else if (GameConstants.isThief(c.getPlayer().getJob())) {
                        item.setLuk((short) (item.getLuk() + 200));
                    } else if (GameConstants.isPirate(c.getPlayer().getJob())) {
                        item.setStr((short) (item.getStr() + 200));
                    }
                    c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));
                } else {
                    c.getPlayer().dropMessage(1, "메소가 부족합니다.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                }
            } else {
                c.getPlayer().dropMessage(1, "필요 성장치가 부족합니다.");
                c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SymbolExp(LittleEndianAccessor slea, MapleClient c) {
        try {
            Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slea.readShort());
            if (source == null) {
                return;
            }
            Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1600);
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1601);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1601);
            }
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1602);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1602);
            }
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1603);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1603);
            }
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1604);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1604);
            }
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1605);
                }
            }
            if (target == null) {
                return;
            } else if (source.getItemId() != target.getItemId()) {
                return;
            }
            target.setArcEXP(target.getArcEXP() + source.getArcEXP() / 2 + 1);
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(source.getPosition());
            c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(MapleInventoryType.EQUIP, (short) source.getPosition(), false));
            c.getPlayer().getSymbol().remove(source);
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, target));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SymbolExp2(LittleEndianAccessor slea, MapleClient c) {
        try {
            Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slea.readShort());
            if (source == null) {
                return;
            }
            Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1700);
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1701);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1701);
            }
            if (target != null) {
                if (source.getItemId() != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1702);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1702);
            }
            if (target == null) {
                return;
            } else if (source.getItemId() != target.getItemId()) {
                return;
            }
            target.setArcEXP(target.getArcEXP() + source.getArcEXP() / 2 + 1);
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(source.getPosition());
            c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(MapleInventoryType.EQUIP, (short) source.getPosition(), false));
            c.getPlayer().getSymbol().remove(source);
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, target));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SymbolMultiExp(LittleEndianAccessor slea, MapleClient c) {

        try {
            int itemid = slea.readInt();
            int count = slea.readInt();
            //  Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slea.readShort());
            int havecount = slea.readInt();
            //  if (source == null) {
            //      return;
            //  }
            Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1600);
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1601);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1601);
            }
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1602);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1602);
            }
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1603);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1603);
            }
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1604);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1604);
            }
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1605);
                }
            }
            if (target == null) {
                return;
            } else if (itemid != target.getItemId()) {
                return;
            }
            List<Equip> removeitems = new ArrayList<>();
            for (Entry<Short, Item> item : c.getPlayer().getInventory(MapleInventoryType.EQUIP).lists().entrySet()) {
                if (item.getValue().getItemId() == itemid) {
                    if (((Equip) item.getValue()).getArcEXP() == 1 && ((Equip) item.getValue()).getArcLevel() == 1) { // 레벨, EXP가 1이면
                        if (removeitems.size() < count) { // 갯수적힌거대로
                            Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) item.getKey());
                            removeitems.add(source);
                        }
                    }
                }
            }
            if (removeitems.size() != count) {
                FileoutputUtil.log(FileoutputUtil.PacketEx_Log, c.getPlayer().getName() + " 캐릭터 심볼 비정상 사용발견");
            }
            target.setArcEXP(target.getArcEXP() + removeitems.size());
            for (Equip item : removeitems) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot((short) item.getPosition());
                c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(MapleInventoryType.EQUIP, (short) item.getPosition(), false));
                c.getPlayer().getSymbol().remove(item);
            }
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, target));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SymbolMultiExp2(LittleEndianAccessor slea, MapleClient c) {

        try {
            int itemid = slea.readInt();
            int count = slea.readInt();
            //  Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) slea.readShort());
            int havecount = slea.readInt();
            //  if (source == null) {
            //      return;
            //  }
            Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1700);
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1701);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1701);
            }
            if (target != null) {
                if (itemid != target.getItemId()) {
                    target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1702);
                }
            }
            if ((target == null)) {
                target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -1702);
            }
            if (target == null) {
                return;
            } else if (itemid != target.getItemId()) {
                return;
            }
            List<Equip> removeitems = new ArrayList<>();
            for (Entry<Short, Item> item : c.getPlayer().getInventory(MapleInventoryType.EQUIP).lists().entrySet()) {
                if (item.getValue().getItemId() == itemid) {
                    if (((Equip) item.getValue()).getArcEXP() == 1 && ((Equip) item.getValue()).getArcLevel() == 1) { // 레벨, EXP가 1이면
                        if (removeitems.size() < count) { // 갯수적힌거대로
                            Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) item.getKey());
                            removeitems.add(source);
                        }
                    }
                }
            }
            if (removeitems.size() != count) {
                FileoutputUtil.log(FileoutputUtil.PacketEx_Log, c.getPlayer().getName() + " 캐릭터 심볼 비정상 사용발견");
            }
            target.setArcEXP(target.getArcEXP() + removeitems.size());
            for (Equip item : removeitems) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot((short) item.getPosition());
                c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(MapleInventoryType.EQUIP, (short) item.getPosition(), false));
                c.getPlayer().getSymbol().remove(item);
            }
            c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, target));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void UnlinkSkill(int skillid, MapleClient c) {
        if (skillid == 80000055) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80000066; i <= 80000070; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                    ////////////      c.getSession().writeAndFlush(CWvsContext.Unlinkskill(i, c.getPlayer().getId()));
                }
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else if (skillid == 80000329) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80000333; i <= 80000335; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                    /////////      c.getSession().writeAndFlush(CWvsContext.Unlinkskill(i, c.getPlayer().getId()));
                }
            }
            if (c.getPlayer().getTotalSkillLevel(80000378) > 0) {
                skills.put(80000378, c.getPlayer().getId());
                for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                    if (a.left.getId() == 80000378) {
                        skills.put(80000378, a.right);
                    }
                }
                c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(80000378), 0, (byte) 0);
                c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(80000378, 0));
                //////////      c.getSession().writeAndFlush(CWvsContext.Unlinkskill(80000378, c.getPlayer().getId()));
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else if (skillid == 80002758) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80002759; i <= 80002761; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                    ////////////      c.getSession().writeAndFlush(CWvsContext.Unlinkskill(i, c.getPlayer().getId()));
                }
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else if (skillid == 80002762) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80002763; i <= 80002765; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                }
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else if (skillid == 80002766) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80002767; i <= 80002769; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                }
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else if (skillid == 80002770) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80002771; i <= 80002773; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                }
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else if (skillid == 80002774) {
            Map<Integer, Integer> skills = new HashMap<>();
            for (int i = 80002775; i <= 80002776; i++) {
                if (c.getPlayer().getTotalSkillLevel(i) > 0) {
                    for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                        if (a.left.getId() == i) {
                            skills.put(i, a.right);
                        }
                    }
                    c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(i), 0, (byte) 0);
                    c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(i, 0));
                }
            }
            if (c.getPlayer().getTotalSkillLevel(80000000) > 0) {
                skills.put(80000378, c.getPlayer().getId());
                for (Triple<Skill, SkillEntry, Integer> a : c.getPlayer().getLinkSkills()) {
                    if (a.left.getId() == 80000000) {
                        skills.put(80000000, a.right);
                    }
                }
                c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(80000000), 0, (byte) 0);
                c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(80000000, 0));
            }
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlocklinkskill(skillid, skills));
        } else {
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), 0, (byte) 0);
            c.getSession().writeAndFlush(CWvsContext.Unlinkskill(skillid, 0)); //이거짤려면 개고생함
        }
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
    }

    public static void LinkSkill(int skillid, int sendid, int recvid, MapleClient c) {
        if (c.getPlayer().getTotalSkillLevel(skillid) > 0) {

            c.getPlayer().dropMessage(6, "동일한 링크를 중복해서 착용하실 수 없습니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));

        }
        int skilllevel = MapleCharacter.loadCharFromDB(sendid, c, false).getLevel() >= 120 ? 2 : 1;
        int totalskilllv = 0;

        int ordinarySkill = 0;
        byte odrinaryMaxLevel = 0;
        if (skillid >= 80000066 && skillid <= 80000070) {
            ordinarySkill = 80000055;
            odrinaryMaxLevel = 10;
        } else if (skillid >= 80000333 && skillid <= 80000335 || skillid == 80000378) {
            ordinarySkill = 80000329;
            odrinaryMaxLevel = 8;
        } else if (skillid >= 80002759 && skillid <= 80002761) {
            ordinarySkill = 80002758;
            odrinaryMaxLevel = 6;
        } else if (skillid >= 80002763 && skillid <= 80002765) {
            ordinarySkill = 80002762;
            odrinaryMaxLevel = 6;
        } else if (skillid >= 80002767 && skillid <= 80002769) {
            ordinarySkill = 80002766;
            odrinaryMaxLevel = 6;
        } else if (skillid >= 80002771 && skillid <= 80002773) {
            ordinarySkill = 80002770;
            odrinaryMaxLevel = 6;
        } else if (skillid >= 80002775 && skillid <= 80002776 || skillid == 80000000) {
            ordinarySkill = 80002774;
            odrinaryMaxLevel = 6;
        }

        if (ordinarySkill > 0) {
            totalskilllv = skilllevel + c.getPlayer().getSkillLevel(ordinarySkill);
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), skilllevel, (byte) 2);
            c.getSession().writeAndFlush(CWvsContext.Unlinkskillunlock(skillid, 1));
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(ordinarySkill), totalskilllv, odrinaryMaxLevel);
        } else {
            c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillid), skilllevel, (byte) 2);
        }

        c.getSession().writeAndFlush(CWvsContext.Linkskill(skillid, sendid, c.getPlayer().getId(), skilllevel, totalskilllv));
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
    }

    public static void IncreaseDuration(MapleCharacter player, int skillid) {
        if (skillid == 400051006 && player.bulletParty < 6) {
            MapleBuffStatValueHolder ignisRore = player.checkBuffStatValueHolder(MapleBuffStat.BulletParty);
            if (ignisRore != null) {
                ignisRore.schedule.cancel(false);
                ignisRore.localDuration += 1000;
                player.bulletParty++;

                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.BulletParty, new Pair<>(player.getBuffedValue(MapleBuffStat.BulletParty), (int) player.getBuffLimit(ignisRore.effect.getSourceId())));

                final CancelEffectAction cancelAction = new CancelEffectAction(player, ignisRore.effect, System.currentTimeMillis(), MapleBuffStat.BulletParty);
                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                    cancelAction.run();
                }, player.getBuffLimit(ignisRore.effect.getSourceId()));

                ignisRore.schedule = schedule;

                player.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, ignisRore.effect, player));
            }
        }
    }

    public static void Respawn(LittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        boolean useFreezer = slea.readByte() == 1;
        if (useFreezer) {
            int buffFreezer = 0;
            if (c.getPlayer().itemQuantity(5133000) > 0) {
                buffFreezer = 5133000;
            } else if (c.getPlayer().itemQuantity(5133001) > 0) {
                buffFreezer = 5133001;
            }

            if (buffFreezer > 0) {
                c.getPlayer().setUseBuffFreezer(true);
                c.getPlayer().removeItem(buffFreezer, -1);

                c.getSession().writeAndFlush(CField.buffFreezer(buffFreezer, useFreezer));
            }
        }

        MapleCharacter chr = c.getPlayer();
        if (chr.getDeathCount() > 0 || chr.liveCounts() > 0) {
            if (chr.isUseBuffFreezer() && useFreezer) {
                chr.setUseBuffFreezer(false);
            } else {
                if (GameConstants.isViper(chr.getJob())) {
                    chr.energy = 0;
                    chr.energyCharge = false;

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    MapleStatEffect energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
                    if (energyCharge == null) {
                        if (chr.getSkillLevel(5120018) > 0) {
                            SkillFactory.getSkill(5120018).getEffect(chr.getSkillLevel(5120018)).applyTo(chr, false);
                        } else if (chr.getSkillLevel(5110014) > 0) {
                            SkillFactory.getSkill(5110014).getEffect(chr.getSkillLevel(5110014)).applyTo(chr, false);
                        } else {
                            SkillFactory.getSkill(5100015).getEffect(chr.getSkillLevel(5100015)).applyTo(chr, false);
                        }
                        energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
                    }

                    energyCharge.setEnergyChargeCooling(false);
                    energyCharge.setEnergyChargeActived(false);

                    statups.put(MapleBuffStat.EnergyCharged, new Pair<>(chr.energy, 0));

                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, energyCharge, chr));
                    chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, energyCharge), false);
                }
                chr.cancelAllBuffs();
            }

            final MapleMap to = chr.getMap();
            chr.changeMap(to, to.getPortal(0));
        } else {
            chr.getStat().setHp((short) 50, chr);
            final MapleMap map = chr.getMap();
            MapleMap to = null;
            if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                to = map.getForcedReturnMap();
            } else {
                to = map.getReturnMap();
            }

            chr.changeMap(to, to.getPortal(0));
        }
    }

    public static void RespawnLucid(LittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        boolean useFreezer = slea.readByte() == 1;
        if (useFreezer) {
            int buffFreezer = 0;
            if (c.getPlayer().itemQuantity(5133000) > 0) {
                buffFreezer = 5133000;
            } else if (c.getPlayer().itemQuantity(5133001) > 0) {
                buffFreezer = 5133001;
            }

            if (buffFreezer > 0) {
                c.getPlayer().setUseBuffFreezer(true);
                c.getPlayer().removeItem(buffFreezer, -1);

                c.getSession().writeAndFlush(CField.buffFreezer(buffFreezer, useFreezer));
            }
        }

        MapleCharacter chr = c.getPlayer();
        if (chr.getDeathCount() > 0 || chr.liveCounts() > 0) {
            if (chr.isUseBuffFreezer() && useFreezer) {
                chr.setUseBuffFreezer(false);
            } else {
                if (GameConstants.isViper(chr.getJob())) {
                    chr.energy = 0;
                    chr.energyCharge = false;

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    MapleStatEffect energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
                    if (energyCharge == null) {
                        if (chr.getSkillLevel(5120018) > 0) {
                            SkillFactory.getSkill(5120018).getEffect(chr.getSkillLevel(5120018)).applyTo(chr, false);
                        } else if (chr.getSkillLevel(5110014) > 0) {
                            SkillFactory.getSkill(5110014).getEffect(chr.getSkillLevel(5110014)).applyTo(chr, false);
                        } else {
                            SkillFactory.getSkill(5100015).getEffect(chr.getSkillLevel(5100015)).applyTo(chr, false);
                        }
                        energyCharge = chr.getBuffedEffect(MapleBuffStat.EnergyCharged);
                    }

                    energyCharge.setEnergyChargeCooling(false);
                    energyCharge.setEnergyChargeActived(false);

                    statups.put(MapleBuffStat.EnergyCharged, new Pair<>(chr.energy, 0));

                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, energyCharge, chr));
                    chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, energyCharge), false);
                }
                chr.cancelAllBuffs();
            }

            final MapleMap to = chr.getMap();
            chr.changeMap(to, to.getPortal(0));
        } else {
            chr.getStat().setHp((short) 50, chr);
            final MapleMap map = chr.getMap();
            MapleMap to = null;
            if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                to = map.getForcedReturnMap();
            } else {
                to = map.getReturnMap();
            }

            chr.changeMap(to, to.getPortal(0));
        }
        if (chr.getSkillLevel(80000329) > 0) {
            SkillFactory.getSkill(80000329).getEffect(chr.getSkillLevel(80000329)).applyTo(chr);
        }
    }

    public static void megaSmasherRequest(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getMapId() == ServerConstants.WarpMap && !c.getPlayer().isGM()) {
            c.getPlayer().changeMap(ServerConstants.WarpMap, 0);
            //c.getPlayer().dropMessage(1, "스킬 쓰지마세요.");
            return;
        }
        final MapleCharacter chr = c.getPlayer();
        boolean start = slea.readByte() == 1;

        if (start) {
            MapleStatEffect effect = SkillFactory.getSkill(400041007).getEffect(chr.getSkillLevel(400041007));

            int localDuration = 0;
            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.MegaSmasher, new Pair<>(-1, localDuration));

            c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
            final long starttime = System.currentTimeMillis();

            chr.getEffects().add(new Pair<>(MapleBuffStat.MegaSmasher, new MapleBuffStatValueHolder(effect, starttime, null, -1, localDuration, c.getPlayer().getId())));

            chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, effect), false);

            chr.isMegaSmasherCharging = true;
            chr.megaSmasherChargeStartTime = System.currentTimeMillis();
        } else {
            if (!chr.isMegaSmasherCharging) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.MegaSmasher, 400041007);
                chr.cancelEffectFromBuffStat(MapleBuffStat.NotDamaged, 400041007);
                return;
            }

            chr.isMegaSmasherCharging = false;

            chr.cancelEffectFromBuffStat(MapleBuffStat.MegaSmasher, 400041007);

            MapleStatEffect effect = SkillFactory.getSkill(400041007).getEffect(chr.getSkillLevel(400041007));

            int plusTime = (int) ((System.currentTimeMillis() - chr.megaSmasherChargeStartTime) / 1000);

            int maxChargeTime = effect.getDuration() + effect.getZ() * 1000;
            int chargeTime = Math.min(maxChargeTime,
                    effect.getDuration() + plusTime / effect.getY() * 1000);

            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.MegaSmasher, new Pair<>(1, chargeTime));
            statups.put(MapleBuffStat.NotDamaged, new Pair<>(1, chargeTime));

            c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));
            final long starttime = System.currentTimeMillis();

            for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {

                MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(chr, effect, starttime, statup.getKey());
                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                    cancelAction.run();
                }, statup.getValue().right);

                chr.registerEffect(effect, starttime,
                        schedule,
                        statup, false, chr.getId());
            }

            chr.megaSmasherChargeStartTime = 0;
            chr.getMap().broadcastMessage(chr, BuffPacket.giveForeignBuff(chr, statups, effect), false);

            chr.getClient().getSession().writeAndFlush(CField.skillCooldown(400041007, effect.getCooldown(chr)));
            chr.addCooldown(400041007, System.currentTimeMillis(), effect.getCooldown(chr));
        }
    }

    public static void SoulMatch(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4); // type maybe
        int state = slea.readInt();
        if (state == 1) {
            c.getPlayer().dropMessage(6, "보스 입장이 시작됩니다.");
        }
        for (List<Pair<Integer, MapleCharacter>> souls : c.getChannelServer().getSoulmatch()) {
            for (Pair<Integer, MapleCharacter> soulz : souls) {
                if (soulz.right.equals(c.getPlayer())) {
                    c.getChannelServer().getSoulmatch().remove(souls);
                }
            }
        }
        c.getSession().writeAndFlush(UIPacket.closeUI(184));
    }

    public static void DailyGift(MapleClient c) {
        int date = Integer.parseInt(c.getKeyValue("dailyGiftDay"));
        int complete = Integer.parseInt(c.getKeyValue("dailyGiftComplete"));

        if (complete == 0) {
            if (date >= GameConstants.dailyItems.size()) {
                c.getSession().writeAndFlush(CField.dailyGift(c.getPlayer(), 3, 0));
                return;
            }
            DailyGiftItemInfo item = GameConstants.dailyItems.get(date);

            int itemId = item.getItemId();
            int quantity = item.getQuantity();

            if (item.getItemId() == 0 && item.getSN() > 0) {
                CashItemInfo cashItem = CashItemFactory.getInstance().getItem(item.getSN());
                itemId = cashItem.getId();
                quantity = cashItem.getCount();
            }

            if (itemId == 4310291) {
                c.getPlayer().AddStarDustCoin(quantity);
            } else if (!MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                c.getSession().writeAndFlush(CField.dailyGift(c.getPlayer(), 7, 0));
                return;
            } else {
                Item addItem;

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    addItem = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId);
                } else {
                    addItem = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);

                }

                if (MapleItemInformationProvider.getInstance().isCash(itemId)) {
                    addItem.setUniqueId(MapleInventoryIdentifier.getInstance());
                }

                MapleInventoryManipulator.addbyItem(c, addItem);
            }

            c.setKeyValue("dailyGiftDay", String.valueOf(date + 1));
            c.setKeyValue("dailyGiftComplete", "1");

            c.getSession().writeAndFlush(CWvsContext.updateDailyGift("count=" + c.getKeyValue("dailyGiftComplete") + ";day=" + c.getKeyValue("dailyGiftDay") + ";date=" + c.getPlayer().getKeyValue(16700, "date")));
            c.getSession().writeAndFlush(CField.dailyGift(c.getPlayer(), 2, itemId));
            c.getSession().writeAndFlush(CField.dailyGift(c.getPlayer(), 0, itemId));
        } else {
            c.getSession().writeAndFlush(CField.dailyGift(c.getPlayer(), 5, 0));
        }
    }

    public static void ShadowServentExtend(LittleEndianAccessor slea, MapleClient c) {
//		Point newpos = slea.readPos();

        for (MapleSummon s : c.getPlayer().getSummons()) {
            if (s.getMovementType() == SummonMovementType.ShadowServantExtend) {
                if (s.getChangePositionCount() < 3) {
                    s.setChangePositionCount((byte) (s.getChangePositionCount() + 1));
                    Point summonpos = s.getTruePosition();
                    c.getSession().writeAndFlush(CField.ShadowServentExtend(summonpos));
                    s.setPosition(c.getPlayer().getTruePosition());
                    c.getPlayer().setPosition(summonpos);
                }
            }
        }
    }

    public static void joker(MapleClient c) {

        for (int i = 1; i <= 14; ++i) {

            MapleAtom atom = new MapleAtom(false, c.getPlayer().getId(), 1, true, 400041010, c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y);

            List<MapleMapObject> mobs = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                    500000, Arrays.asList(MapleMapObjectType.MONSTER));

            if (mobs.size() > 0) {
                atom.setDwFirstTargetId(mobs.get(0).getObjectId());
                ForceAtom forceAtom = new ForceAtom(2, Randomizer.rand(0x10, 0x1A), Randomizer.rand(7, 11), Randomizer.nextInt(4) + 5, (short) 0);
                forceAtom.setnAttackCount(forceAtom.getnAttackCount() + 1);
                atom.addForceAtom(forceAtom);
                c.getPlayer().getMap().spawnMapleAtom(atom);
            }
        }
    }

    public static void activePrayBuff(final MapleClient c) {
        final MapleCharacter player = c.getPlayer();
        MapleParty party = player.getParty();
        final MapleStatEffect effect = player.getBuffedEffect(MapleBuffStat.Pray);
        final Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();

        final long starttime = System.currentTimeMillis();

        if (effect != null) {
            int int_ = player.getStat().getTotalInt(); // 자신의 순수 마력

            // 마력 #q2%당 최종 데미지 1% 증가, 최대 #w%까지 증가 가능
            final int incPMdR = effect.getQ() + Math.min(effect.getW(), (int_ / effect.getQ2()));

            // 마력 #y%당 HP/MP 회복량 1% 증가, 최대 #z%까지 증가 가능
            int incRecovery = int_ / effect.getY();

            // 마력 #u%당 공격속도 1단계 증가, 최대 3단계까지 증가 가능
            final int incBooster = Math.min(int_ / effect.getU(), 3);

            //나에게 우선 적용
            int incRecoveryHP = (int) Math.min(effect.getZ(),
                    incRecovery + effect.getX());
            int incRecoveryMP = (int) Math.min(effect.getZ(),
                    incRecovery + effect.getX());

            incRecoveryHP = (int) (player.getStat().getMaxHp() * (incRecoveryHP * 0.01));
            incRecoveryMP = (int) (player.getStat().getMaxMp() * (incRecoveryMP * 0.01));
            //victim.dropMessage(5, "hp : " + incRecoveryHP + ", mp : " +
            //incRecoveryMP);

            if (player.isAlive()) {
                player.addMPHP(incRecoveryHP, incRecoveryMP);
            }
//            if (victim.getId() != player.getId()) {
            //victim.dropMessage(5, "범위 내에 들어와 버프 시전!");

            if (incPMdR > 0) {
                statups.put(MapleBuffStat.IndiePmdR, new Pair<>(incPMdR * 3, 2000));
            }
            if (incBooster > 0) {
                statups.put(MapleBuffStat.IndieBooster, new Pair<>(-incBooster, 2000));
            }

            player.cancelEffectFromBuffStat(MapleBuffStat.IndiePmdR, effect.getSourceId());
            player.cancelEffectFromBuffStat(MapleBuffStat.IndieBooster, effect.getSourceId());

            for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
                MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(player, effect, starttime, statup.getKey());
                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                    cancelAction.run();
                }, statup.getValue().right);

                player.registerEffect(effect, starttime,
                        schedule,
                        statup, false, player.getId());
            }

            c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, player));

            if (party != null) { // 파티원이 근처에 있는지 체크
                for (MaplePartyCharacter pc : party.getMembers()) {
                    if (pc.isOnline()) {
                        if (pc.getMapid() == player.getMapId()) {
                            if (pc.getChannel() == c.getChannel()) {
                                final MapleCharacter victim = c.getChannelServer().getPlayerStorage()
                                        .getCharacterByName(pc.getName());
                                if (victim != null) {
                                    if (!victim.getBuffedValue(400021003) && victim.isAlive() && player.getId() != victim.getId()) { // 이미 다른 비숍에 프레이 버프를 받고 있다면 중첩 불가.
                                        if (effect.calculateBoundingBox(player.getTruePosition(), player.isFacingLeft()).contains(victim.getTruePosition())) { // 시전자 좌표 기준 범위내에 있는 파티원인지 체크

                                            incRecoveryHP = (int) (victim.getStat().getMaxHp() * (incRecoveryHP * 0.01));
                                            incRecoveryMP = (int) (victim.getStat().getMaxMp() * (incRecoveryMP * 0.01));
                                            //victim.dropMessage(5, "hp : " + incRecoveryHP + ", mp : " +
                                            //incRecoveryMP);
                                            victim.addMPHP(incRecoveryHP, incRecoveryMP);

                                            statups.clear();

//                                            if (victim.getId() != player.getId()) {
                                            //    victim.dropMessage(5, "범위 내에 들어와 버프 시전!");
                                            if (incPMdR > 0) {
                                                statups.put(MapleBuffStat.IndiePmdR, new Pair<>(incPMdR, 5000));
                                            }
                                            if (incBooster > 0) {
                                                statups.put(MapleBuffStat.IndieBooster, new Pair<>(-incBooster, 5000));
                                            }

                                            //victim.cancelEffectFromBuffStat(MapleBuffStat.IndiePmdR, effect.getSourceId());
                                            //victim.cancelEffectFromBuffStat(MapleBuffStat.IndieBooster, effect.getSourceId());
                                            for (Entry<MapleBuffStat, Pair<Integer, Integer>> statup : statups.entrySet()) {
                                                MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(victim, effect, starttime, statup.getKey());
                                                ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(() -> {
                                                    cancelAction.run();
                                                }, statup.getValue().right);
                                                victim.registerEffect(effect, starttime,
                                                        schedule,
                                                        statup, false, victim.getId());
                                            }

                                            victim.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, player));

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void InhumanSpeed(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        slea.readInt();
        if (c.getPlayer().getBuffedValue(400031020) || c.getPlayer().getBuffedValue(400031021)) {

            MapleAtom atom = new MapleAtom(false, c.getPlayer().getId(), 31, true, c.getPlayer().getBuffedValue(400031020) ? 400031020 : 400031021, 0, 0);

            atom.setDwFirstTargetId(objectId);
            atom.addForceAtom(new ForceAtom(1, 12, 15, 70, (short) 0));
            c.getPlayer().getMap().spawnMapleAtom(atom);
        }
    }

    public static void CreateKinesisPsychicArea(LittleEndianAccessor rm, MapleClient c) {
        int nAction = rm.readInt();
        int ActionSpeed = rm.readInt();
        int PsychicAreaKey = rm.readInt();
        int LocalKey = rm.readInt();
        int SkillID = rm.readInt();
        short SLV = rm.readShort();
        int DurationTime = rm.readInt();
        byte second = rm.readByte();
        short SkeletonFieldPathIdx = rm.readShort();
        short SkeletonAniIdx = rm.readShort();
        short SkeletonLoop = rm.readShort();
        int mask8 = rm.readInt();
        int mask9 = rm.readInt();
        MapleStatEffect eff = SkillFactory.getSkill(SkillID).getEffect(SLV);
        eff.applyTo(c.getPlayer(), false);
        c.getPlayer().getMap()
                .broadcastMessage(CWvsContext.OnCreatePsychicArea(c.getPlayer().getId(), nAction, ActionSpeed,
                        LocalKey, SkillID, SLV, PsychicAreaKey, DurationTime, second, SkeletonFieldPathIdx,
                        SkeletonAniIdx, SkeletonLoop, mask8, mask9));
        c.getPlayer().givePPoint(SkillID, false);
        if (eff.getCooldown(c.getPlayer()) > 0 && !c.getPlayer().isGM()) {
            c.getSession()
                    .writeAndFlush(CField.skillCooldown(SkillID, eff.getCooldown(c.getPlayer())));
            c.getPlayer().addCooldown(SkillID, System.currentTimeMillis(), eff.getCooldown(c.getPlayer()));
        }
    }

    public static void HolyPountin(MapleCharacter chr, int oid, int skillid, Point pos) {

        if (chr == null || chr.getMap() == null) {
            return;
        }
        List<MapleMist> mistsInMap = chr.getMap().getAllMistsThreadsafe();
        MapleMist fountain = null;
        for (MapleMapObject nn : mistsInMap) {
            MapleMist mist = (MapleMist) nn;
            if (mist.getSource() != null) {
                if (mist.getSource().getSourceId() == 2311011) {
                    fountain = mist;
                    break;
                }
            }
        }

        if (fountain == null || chr.getBuffedEffect(MapleBuffStat.DebuffIncHp) != null) {
            return;
        }
        if (fountain.getObjectId() == oid) {
            if (skillid == 2311011) {
                /*				if (fountain.getPosition().x != x || fountain.getTruePosition().x != x) {
                 c.getPlayer().dropMessage(5, "pos:x error.");
                 } else if (fountain.getPosition().y != y || fountain.getTruePosition().y != y) {
                 c.getPlayer().dropMessage(5, "pos:y error.");
                 } else if (c.getPlayer().getHolyPountinOid() == oid && c.getPlayer().getHolyPountin() > 20) {
                 c.getPlayer().dropMessage(5, "20번 이상 회복하실 수 없습니다.");
                 } else {*/
                if (chr.getHolyPountinOid() != oid) {
                    chr.setHolyPountin((byte) 0);
                } else {
                    chr.setHolyPountin((byte) (chr.getHolyPountin() + 1));
                }
                chr.addHP(chr.getStat().getMaxHp() / 100 * fountain.getSource().getX());
                chr.setHolyPountinOid(oid);

//				}
            } else {
                chr.dropMessageGM(5, "SkillId Error.");
            }
        } else {
            chr.dropMessageGM(5, "ObjectId Error.");
        }

    }

    public static void UpdateJaguar(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4); // unk
        int changed = slea.readInt();
//		if (Integer.parseInt(c.getPlayer().getInfoQuest(123456)) == prevJaguar * 10) {
        c.getPlayer().updateInfoQuest(123456, String.valueOf((changed + 1) * 10));
        c.getSession().writeAndFlush(CWvsContext.updateJaguar(c.getPlayer()));
        /*		} else {
         c.getPlayer().dropMessage(5, "prevJaguar : " + prevJaguar);*/
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
//		}
    }

    public static void auraWeapon(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
//        if (skillid == 80003023) {
//            MapleStatEffect eff = SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getSkillLevel(skillid));
//            Map<MapleBuffStat, Pair<Integer, Integer>> statup = new HashMap<>();
//            statup.put(MapleBuffStat.EventSpecialSkill, new Pair<>(1, 0));
//            if (c.getPlayer().getKeyValue(20210113, "orgelonoff") == 0) {
//                c.getSession().writeAndFlush(BuffPacket.giveBuff(statup, eff, c.getPlayer()));
//                c.getPlayer().setKeyValue(20210113, "orgelonoff", "1");
//            } else if (c.getPlayer().getKeyValue(20210113, "orgelonoff") == 1) {
//                c.getSession().writeAndFlush(BuffPacket.cancelBuff(statup, c.getPlayer()));
//                c.getPlayer().setKeyValue(20210113, "orgelonoff", "0");
//                c.getSession().writeAndFlush(CWvsContext.showPopupMessage("르네의 마법오르골이 비활성화되었습니다."));
//            }
//        }
        /*		List<Triple<Integer, Integer, Integer>> finalMobList = new ArrayList<>();
         c.getSession().writeAndFlush(CField.bonusAttackRequest(skillid, finalMobList, true, 0));*/
    }

    public static void removeMist(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.IndiePadR, 80001455);
        c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.IndieMadR, 80001455);
        c.getPlayer().getMap().removeMist(skillid);
    }

    public static void PeaceMaker(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        slea.skip(28);
        int plus = SkillFactory.getSkill(400021070).getEffect(c.getPlayer().getSkillLevel(40021070)).getQ2() + (slea.readInt() * SkillFactory.getSkill(400021070).getEffect(c.getPlayer().getSkillLevel(40021070)).getW2());
        if (c.getPlayer().getParty() != null) {
            for (MapleCharacter chr : c.getPlayer().getPartyMembers()) {
                chr.setPeaceMaker(plus);
                SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getSkillLevel(GameConstants.getLinkedSkill(skillid))).applyTo(chr);
            }
        } else {
            c.getPlayer().setPeaceMaker(plus);
            SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getSkillLevel(GameConstants.getLinkedSkill(skillid))).applyTo(c.getPlayer(), true);
        }
    }

    public static void PeaceMaker2(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();

        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.skillCancel(c.getPlayer(), skillid), false);

        SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getSkillLevel(GameConstants.getLinkedSkill(skillid))).applyTo(c.getPlayer(), true);
    }

    public static void DemonFrenzy(MapleClient c) {
        /*if (c.getPlayer().getMapId() == ServerConstants.WarpMap && !c.getPlayer().isGM()) { ??..
            c.getPlayer().changeMap(ServerConstants.WarpMap, 0);
            return;
        }*/

        MapleStatEffect Frenzy = c.getPlayer().getBuffedEffect(400011010);
        if (Frenzy == null) {
            return;
        }

        if (c.getPlayer().getStat().getHp() > c.getPlayer().getStat().getCurrentMaxHp() * Frenzy.getQ2() / 100) {
            c.getPlayer().addHP(-c.getPlayer().getStat().getCurrentMaxHp() * Frenzy.getQ2() / 100);
            MapleStatEffect FrenzyMist = SkillFactory.getSkill(400010010).getEffect(c.getPlayer().getSkillLevel(400011010));
            final Rectangle bounds = FrenzyMist.calculateBoundingBox(new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y), c.getPlayer().isFacingLeft());
            final MapleMist mist = new MapleMist(new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height), c.getPlayer(), FrenzyMist, 5000, (byte) (c.getPlayer().isFacingLeft() ? 1 : 0));
            c.getPlayer().getMap().spawnMist(mist, false);
        }
    }

    public static void grandCross(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();

        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        if (skillid >= 3321034 && skillid <= 3321040) {
            MapleStatEffect eff = SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getSkillLevel(GameConstants.getLinkedSkill(skillid)));
            c.getPlayer().energy -= eff.getForceCon();
            if (c.getPlayer().energy < 0) {
                c.getPlayer().energy = 0;
            }
            statups.put(MapleBuffStat.RelikGauge, new Pair<>(c.getPlayer().energy, 0));
            c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, null, c.getPlayer()));

        } else if (c.getPlayer().getBuffedValue(skillid)) {
            MapleStatEffect eff = c.getPlayer().getBuffedEffect(skillid);
            c.getPlayer().addHP(-c.getPlayer().getStat().getHp() * eff.getT() / 100);
            c.getPlayer().setBuffedValue(MapleBuffStat.GrandCrossSize, 2);
            statups.put(MapleBuffStat.GrandCrossSize, new Pair<>(2, (int) c.getPlayer().getBuffLimit(skillid)));
            c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, eff, c.getPlayer()));
            List<MapleMonster> finalattckmobs = new ArrayList<>();
            c.getSession().writeAndFlush(CField.finalAttackRequest(0, skillid, 0, 0, finalattckmobs));
        } else if (skillid == 400021086) {
            if (c.getPlayer().getBHGCCount() > 0) {

                MapleStatEffect effect = SkillFactory.getSkill(skillid).getEffect(c.getPlayer().getSkillLevel(GameConstants.getLinkedSkill(skillid)));

                if (c.getPlayer().getBHGCCount() == effect.getY())
                    c.getPlayer().lastBHGCGiveTime = System.currentTimeMillis();

                c.getPlayer().setBHGCCount(c.getPlayer().getBHGCCount() - 1);

                statups = new HashMap<>();
                statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(c.getPlayer().getBHGCCount(), 0));

                c.getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, c.getPlayer()));
            }
        }
    }

    public static void subSkillEffect(LittleEndianAccessor slea, MapleCharacter chr) {
        // Rectangle
        int newx = slea.readInt();
        int newy = slea.readInt();
        int oldx = slea.readInt();
        int oldy = slea.readInt();
        int delay = slea.readInt();
        final int skillId = slea.readInt();
        final int unk = slea.readInt();
        byte facingleft = slea.readByte();
        slea.skip(4);
        int objectId = slea.readInt();

        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedSkill(skillId));
        if (chr == null || skill == null || chr.getMap() == null) {
            return;
        }

        final byte skilllevel_serv = (byte) chr.getTotalSkillLevel(skill);
        if (skillId == 400031003 || skillId == 400031004) {
            chr.getClient().getSession().writeAndFlush(CField.removeProjectile(chr.lastHowlingGaleObjectId));
            chr.setHowlingGaleCount(chr.getHowlingGaleCount() - (skillId == 400031003 ? 1 : 2));

            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
            statups.put(MapleBuffStat.HowlingGale, new Pair<>(chr.getHowlingGaleCount(), 0));

            chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, skill.getEffect(skilllevel_serv), chr));
            chr.lastHowlingGaleObjectId = objectId;
        }

        chr.getMap().broadcastMessage(chr, CField.showProjectileEffect(chr, newx, newy, delay, skillId, skilllevel_serv, unk, facingleft, objectId), false);
        chr.getClient().getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, skillId, 1, 0, 0, (byte) (chr.isFacingLeft() ? 1 : 0), true, chr.getTruePosition(), null, null));
        if (!GameConstants.isLinkMap(chr.getMapId())) {
            chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, 0, skillId, 1, 0, 0, (byte) (chr.isFacingLeft() ? 1 : 0), false, chr.getTruePosition(), null, null), false);
        }

        if (skill.getEffect(skilllevel_serv).getCooldown(chr) > 0 && chr.getCooldownLimit(skillId) == 0) {
            chr.giveCoolDowns(skillId, System.currentTimeMillis(), skill.getEffect(skilllevel_serv).getCooldown(chr));
            chr.getClient().getSession().writeAndFlush(CField.skillCooldown(skillId, skill.getEffect(skilllevel_serv).getCooldown(chr)));
        }

        if (skillId == 64101002) {
            chr.wingDagger = true;
        }

        skill.getEffect(skilllevel_serv).applyTo(chr, false);
    }

    public static void cancelSubEffect(LittleEndianAccessor slea, MapleCharacter chr) {
        chr.getMap().broadcastMessage(chr, CField.removeProjectileEffect(chr.getId(), slea.readInt()), false);
        slea.readByte();
        slea.readInt(); // skillID
    }

    public static void changeSubEffect(LittleEndianAccessor slea, MapleCharacter chr) {
        chr.getMap().broadcastMessage(chr, CField.updateProjectileEffect(chr.getId(), slea.readInt(), slea.readInt(), slea.readInt(), slea.readInt(), slea.readByte()), false);
    }

    public static void LinkofArk(LittleEndianAccessor slea, MapleCharacter player) {
        slea.skip(4); // 1 or 2?
        int sourceid = slea.readInt();
        if (player != null && player.getSkillLevel(sourceid) > 0) {
            player.LinkofArk = Math.min(5, player.LinkofArk + 1);
            SkillFactory.getSkill(sourceid).getEffect(player.getSkillLevel(sourceid)).applyTo(player, false);
        }
    }

    public static void FlowOfFight(MapleCharacter player) {
        if (player != null && player.getSkillLevel(80000268) > 0) {
            player.FlowofFight = Math.min(6, player.FlowofFight + 1);
            SkillFactory.getSkill(80000268).getEffect(player.getSkillLevel(80000268)).applyTo(player, false);
        }
    }

    public static void TowerChair(LittleEndianAccessor slea, MapleClient c) {
        List<Integer> chairs = new ArrayList<Integer>();
        for (int a = 0; a < 6; a++) {
            int val = slea.readInt();
            if (val == 0) {
                break;
            }
            chairs.add(val);
        }

        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < chairs.size(); a++) {
            sb.append(a);
            sb.append('=');
            sb.append(chairs.get(a));
            if (a != chairs.size() - 1) {
                sb.append(';');
            }
        }

        c.getPlayer().updateInfoQuest(7266, sb.toString());
        c.getSession().writeAndFlush(SLFCGPacket.TowerChairSaveDone());
//        Point temppoint = c.getPlayer().getPosition();
//        c.getPlayer().changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().getPortal(0));
        //      c.getSession().writeAndFlush(SLFCGPacket.CharReLocationPacket(temppoint.x, temppoint.y));
    }

    public static void HandleCellClick(final int number, MapleClient c) {
        if (c.getPlayer().getBingoGame().getRanking().contains(c.getPlayer())) {
            // return;
        }
        int[][] table = c.getPlayer().getBingoGame().getTable(c.getPlayer());
        c.getSession().writeAndFlush(SLFCGPacket.BingoCheckNumber(number));
        int jj = 0;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (table[x][y] == number) {
                    table[x][y] = 0xFF;
                }
            }
        }
        int temp = 0;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (table[x][y] == 0xFF || table[x][y] == 0x00) {
                    temp++;
                }
            }
            if (temp == 5) {
                c.getSession().writeAndFlush(SLFCGPacket.BingoDrawLine(y * 5, 0, number));
            }
            temp = 0;
        }
        temp = 0;
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                if (table[x][y] == 0xFF || table[x][y] == 0x00) {
                    temp++;
                }
            }
            if (temp == 5) {
                c.getSession().writeAndFlush(SLFCGPacket.BingoDrawLine(x, 1, number));
            }
            temp = 0;
        }
        int crossCnt = 0;
        int rcrossCnt = 0;
        for (int i = 0; i < 5; i++) {
            if (table[i][i] == 0xFF || table[i][i] == 0x00) {
                crossCnt++;
            }
            if (table[i][4 - i] == 0xFF || table[i][4 - i] == 0x00) {
                rcrossCnt++;
            }
            if (crossCnt == 5) {
                c.getSession().writeAndFlush(SLFCGPacket.BingoDrawLine(1, 2, number));
            }
            if (rcrossCnt == 5) {
                c.getSession().writeAndFlush(SLFCGPacket.BingoDrawLine(1, 3, number));
            }
        }
    }

    public static void HandleHundredDetectiveGame(LittleEndianAccessor slea, MapleClient c) {
        String input = String.valueOf(slea.readInt());
        DetectiveGame game = c.getPlayer().getDetectiveGame();
        int asdf = game.getAnswer(c.getPlayer());
        String Answer = String.valueOf(c.getPlayer().getDetectiveGame().getAnswer(c.getPlayer()));
        int result = 0;
        for (int a = 0; a < 3; a++) {
            char inputchar = input.charAt(a);
            char answerchar = Answer.charAt(a);
            if (inputchar == answerchar) {
                result += 10;
            } else if (Answer.contains(String.valueOf(inputchar))) {
                result++;
            }
        }
        c.getPlayer().getDetectiveGame().addAttempt(c.getPlayer());
        c.getSession().writeAndFlush(SLFCGPacket.HundredDetectiveGameResult(Integer.valueOf(input), result));
        if (result == 30) {
            c.getPlayer().getDetectiveGame().addRank(c.getPlayer());
        }
    }

    public static void HandlePlatformerEnter(LittleEndianAccessor slea, MapleClient c) {
        int Stage = slea.readInt();
        int Map = 993001000 + (Stage * 10);
        c.getPlayer().warp(Map);
        c.getSession().writeAndFlush(CField.getClock(600));
        if (c.getPlayer().getPlatformerTimer() != null) {
            c.getPlayer().getPlatformerTimer().cancel(false);
        }
        ScheduledFuture<?> a = Timer.ShowTimer.getInstance().schedule(() -> {
            if (c.getPlayer().getMapId() == Map) {
                c.getPlayer().warp(993001000);
            }
            c.getPlayer().setPlatformerTimer(null);
        }, 10 * 60 * 1000);
        c.getPlayer().setPlatformerTimer(a);
        c.getPlayer().setPlatformerStageEnter(System.currentTimeMillis());
        c.getSession().writeAndFlush(SLFCGPacket.PlatformerStageInfo(Stage));
        c.getSession().writeAndFlush(SLFCGPacket.PlatformerTimerInfo());
        c.getSession().writeAndFlush(SLFCGPacket.playSE("Sound/MiniGame.img/multiBingo/start"));
        c.getSession().writeAndFlush(CField.environmentChange("event/start", 0x13));
        c.getSession().writeAndFlush(CField.UIPacket.closeUI(1112));
        c.getPlayer().setKeyValue(18838, "count", c.getPlayer().getKeyValue(18838, "count") - 1 + "");
        switch (Stage) {
            case 1:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070203, 2000, "대시는 가고 싶은 방향으로 방향키 연속! 두 번! 이다...후후...", ""));
                c.getSession().writeAndFlush(CField.enforceMSG("대시를 사용하여 골인 지점으로 가는거다!?", 215, 5000));
                break;
            case 2:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "앞의 장애물은 대시 중 점프로 쉽게 넘을 수 있을거야!", ""));
                c.getSession().writeAndFlush(CField.enforceMSG("대시 중 점프를 하면 높이, 멀리 뛸 수 있어!", 214, 5000));
                break;
            case 3:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "대시 중 방향키 위를 먼저 누르고 점프하면 더 쉬워! 편한 방식을 찾아 보자", ""));
                c.getSession().writeAndFlush(CField.enforceMSG("점프 중 방향키 위를 유지하면 높이 뛰어 오를 수 있어!", 214, 5000));
                break;
            case 4:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 3));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "점프 중 방향키를 누르면 원하는 방향으로 공중 제어가 가능해.", ""));
                c.getSession().writeAndFlush(CField.enforceMSG("점프 중 좌우 방향키를 입력하면 공중에서 자세 제어가 가능해. 히힛.", 213, 5000));
                break;
            case 5:
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "점프 중 방향키를 누르면 원하는 방향으로 공중 제어가 가능해.", ""));
                c.getSession().writeAndFlush(CField.enforceMSG("전에도 말했듯이 대시 중 방향키 위를 먼저 누르고 점프해도 괜찮아", 214, 5000));
                break;
            case 6:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 3));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "음... 더러운 것에 닿는다고 해도 죽는건 아니다...", ""));
                c.getSession().writeAndFlush(CField.enforceMSG("공중에서 방향키 좌우를 눌러 더러운 것을 피해라.", 212, 5000));
                break;
            case 7:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                break;
            case 8:
                NPCScriptManager.getInstance().start(c, "Obstacle");
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 300, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                break;
            case 9:
                NPCScriptManager.getInstance().start(c, "FootHoldMove");
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 350));
                c.getSession().writeAndFlush(CField.enforceMSG("상하로 움직이는 발판을 이용해서 목적지에 도달해 봐. 히힛.", 213, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "발판이 적절한 위치에 있을 때 점프하는게 좋을거야.\r\n막 뛰면 안 된다구.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "점프는 예술과 기술이니까~", ""));
                break;
            case 10:
                c.getSession().writeAndFlush(CField.enforceMSG("상승하는 발판 위에서 더러운 것을 피해라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "우선 아래에 보이는 긴 발판에 올라서 봐라. 그럼 움직일 거야.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "그 뒤는 알아서 해라.", ""));
                break;
            case 11:
                c.getSession().writeAndFlush(CField.enforceMSG("점프 기술을 활용하여 목적지에 도달해 보자~", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "점프 중 위키를 잘 활용해 봐. 의외로 지름길도 있으니 잘 찾아가고.", ""));
                break;
            case 12:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("세 번째 발판에서 최대한 멀리 뛰어 봐~", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "세 번째 발판에서 최대한 멀리 뛰어 봐. 15미터 이상이면 합격!", ""));
                NPCScriptManager.getInstance().start(c, "Obstacle2");
                break;
            case 13:
                c.getSession().writeAndFlush(CField.enforceMSG("점프 기술을 활용해서 더러운 것을 피해 나아가라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "장애물과 장애물의 중간 지점 쯤에서 점프해 봐라.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "못 넘는 곳이 있으면 점프중 방향키 위를 잊지 마.", ""));
                break;
            case 14:
                c.getSession().writeAndFlush(CField.enforceMSG("상승하는 발판 위에서 더러운 것을 피해라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "알지? 아래의 긴 발판으로 내려가 봐.", ""));
                break;
            case 15:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 300, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("연속 대시 점프로 목적지에 도달하는거다!", 215, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070203, 2000, "대쉬 점프로 넘는거다!\r\n가끔은 연속 점프보다 잠깐 멈추는게 유리할 수도 있다!", ""));
                break;
            case 16:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 3));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(CField.enforceMSG("더러운 것을 피해서 목적지에 도달해 봐. 히힛.", 213, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "여긴 두 가지 방법이 있다. 낙하하며 좌우키를 번갈아가며 누르는 것.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "혹은 점프 중 반대 방향으로 힘을 주어 수직으로 떨어지는 것.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "선택은 네 몫이야. 히힛.", ""));
                break;
            case 17:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("낙하하는 운석을 피해 골인 지점까지 도달하는거다!", 215, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070203, 2000, "익숙한 곳일거다! 가끔은 과감히 맞으면서 돌파하는 것도 남자답지", ""));
                NPCScriptManager.getInstance().start(c, "Obstacle3");
                break;
            case 18:
                c.getSession().writeAndFlush(CField.enforceMSG("좌우의 곰을 30회 반복해서 터치하는거다!", 215, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070203, 2000, "좌우의 잠자는 곰에 반복해서 닿아라! 총 30회다!", ""));
                break;
            case 19:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("나무를 이용하여 목적지에 도달해 봐~", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "큰 나무를 조심해. 부딪히면 아프니까... 나무 위로 올라설 수 있다는 걸 명심해 둬.", ""));
                break;
            case 20:
                c.getSession().writeAndFlush(CField.enforceMSG("발판이 사라지는 숲을 돌파해 봐~", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "인간들은 인내의 숲이란 곳에서 수련을 한다며?\r\n그런데 너무 쉬운 것 같더라. 히히.\r\n발판이 사라지는 정도면 재밌지 않겠어?.", ""));
                break;
            case 21:
                c.getSession().writeAndFlush(CField.enforceMSG("장애물을 피해 상쾌하게 달려가는거야! 히힛.", 213, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "큰 나무는 그냥 지나갈 수 없어.\r\n하지만 점프로 올라갈 수 있지. 높이 점프할 땐 새를 조심하라구.", ""));
                break;
            case 22:
                c.getSession().writeAndFlush(CField.enforceMSG("공중 제어를 활용하여 발판을 올라 보자. 히힛.", 213, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "힌트를 주자면... 높고 길게 뛰어서 힘이 다할때쯤 뛴 반대 방향으로 돌아와. 꽤 어려울거야.", ""));
                break;
            case 23:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 300, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("종합적인 이동 능력을 시험해 봐라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "얼마나 수련이 잘 되었는지 확인해 봐라. 너무 괴로워서 울지도 모르겠군.", ""));
                break;
            case 24:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 300, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("닿으면 사라지는 발판을 재빠르게 넘어가 봐. 히힛.", 213, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "1초 정도 발판에 머무르면 사라지니까 조심해. 히힛.", ""));
                break;
            case 25:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("연속 대시 점프로 장애물을 넘어가 봐~", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "너무 급하게 달려가지 말고 속도를 조절할 땐 조절해~", ""));
                break;
            case 26:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("모든 점프 기술을 활용하여 더러운 것을 피해 가는거다!", 215, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070203, 2000, "더러운 것에 당하느니 천천히 생각하면서 가라!", ""));
                break;
            case 27:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 3));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(CField.enforceMSG("낙하 중 좌우 방향키로 공중 제어를 할 수 있다. 더러운 건 피해야지.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "여긴... 음. 할 말이 없다. 하하.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "실제로 돌파할 수 있긴 한거야?", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "한 사람이 있다고 하네요...", ""));
                break;
            case 28:
                c.getSession().writeAndFlush(CField.enforceMSG("점프 중 좌우 방향키로 소멸하는 발판을 돌파...해라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "나도 이런 정신나간 곳이 존재한다는게 믿기지 않는다.", ""));
                break;
            case 29:
                c.getSession().writeAndFlush(CField.enforceMSG("상승하는 발판에서 더러운 것을 피해라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "우선 아래에 있는 발판으로 내려가. 이 패턴 익숙하지?", ""));
                break;
            case 30:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(CField.enforceMSG("독수리를 피해 골인 지점까지 도달해라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "독수리는 보기 보다 판정 영역이 작다. 할 수 있겠지?", ""));
                break;
            case 31:
                c.getSession().writeAndFlush(CField.enforceMSG("점프대로 점프하고 공중에서 제어해 봐. 히힛.", 213, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "평범하게 생긴 발판도 밟으면 통통 튀어 오를 수 있다구. 히힛.", ""));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "이 곳의 공중 제어는 조금 불쾌한 느낌일지도. 히힛.", ""));
                break;
            case 32:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 350));
                c.getSession().writeAndFlush(CField.enforceMSG("발판 위에서 중심을 잘 잡으며 잘 피해봐. 꼭 피해야 해.", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "발판 안은 위험해.", ""));
                NPCScriptManager.getInstance().start(c, "FootHoldMove2");
                break;
            case 33:
                c.getSession().writeAndFlush(CField.enforceMSG("더러운 것을 공중에서 화려하게 피해 봐~", 214, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "상향 점프와 공중 제어를 잘 이용해야 해. 나처럼 섬세한 점프!", ""));
                break;
            case 34:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 3));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(CField.enforceMSG("공중에서 잘 움직이는 것 뿐이다. 알아서 피하고 싶을거다", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "세상엔 더러운 것도 위험한 것도 있다. 넌 이겨낼 수 있을거다.", ""));
                NPCScriptManager.getInstance().start(c, "Obstacle4");
                break;
            case 35:
                c.getSession().writeAndFlush(CField.enforceMSG("능력을 한 번 시험해 봐라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "이 곳은 종합적인 능력을 시험하는 곳이지. 길을 따라 가면 된다.", ""));
                break;
            case 36:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 400, 350));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070203, 2000, "한 치의 실수도 용납되지 않는 곳이다. 잔인하군...", ""));
                NPCScriptManager.getInstance().start(c, "FootHoldMove3");
                break;
            case 37:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 0, 0, 200));
                c.getSession().writeAndFlush(CField.enforceMSG("점프대를 이용해서 공중 자세 제어를 해 봐.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070201, 2000, "여기를 지나갈 수 있다면 트리플 악셀도 가능할거야.", ""));
                break;
            case 38:
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0F, 1000, 600, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0D, 0, 1000, 300, 0));
                c.getSession().writeAndFlush(SLFCGPacket.CameraCtrl(0x0B, 5));
                c.getSession().writeAndFlush(CField.enforceMSG("이동하는 발판 위에서 공중 제어 점프로 화려하게 피해 봐라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "앞에 보이는 발판에 올라서면 발판이 움직일거다.", ""));
                break;
            case 39:
                c.getSession().writeAndFlush(CField.enforceMSG("상승하는 발판 위에서 더러운 것을 피해내라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070200, 2000, "일단 아래의 발판으로 내려가는건 알지?", ""));
                break;
            case 40:
                c.getSession().writeAndFlush(CField.enforceMSG("종합 시험이다. 멘탈 붕괴에 유의해라.", 212, 5000));
                c.getSession().writeAndFlush(SLFCGPacket.OnYellowDlg(9070202, 2000, "이 스테이지가 이렇게 어렵습니다~ 히힛.", ""));
                break;
        }
    }

    public static void HandlePlatformerExit(LittleEndianAccessor slea, MapleClient c) {
        switch (slea.readByte()) {
            case 12:
                if (c.getPlayer().getPlatformerTimer() != null) {
                    c.getPlayer().getPlatformerTimer().cancel(false);
                    c.getPlayer().setPlatformerTimer(null);
                }
                c.getPlayer().setPlatformerStageEnter(0L);
                c.getPlayer().warp(993001000);
                break;
            case 18:
                c.removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 2007, "union_rade");
                break;
            default:
                c.getPlayer().dropMessage(6, slea.readByte() + "");
                break;
        }

    }

    public static void HandleResolution(LittleEndianAccessor slea, MapleClient c) {
        switch (slea.readByte()) {
            case 6:
                c.getPlayer().setResolution(1920, 1200);
                break;
            case 5:
                c.getPlayer().setResolution(1920, 1080);
                break;
            case 4:
                c.getPlayer().setResolution(1280, 720);
                break;
            case 3:
                c.getPlayer().setResolution(1366, 768);
                break;
            case 2:
                c.getPlayer().setResolution(1024, 768);
                break;
            case 1:
                c.getPlayer().setResolution(800, 600);
                break;
            default: //핵
                c.disconnect(true, false, false);
                c.getSession().close();
                break;
        }
    }

    public static void ExitSpecialGame(MapleClient c) {
        switch (c.getPlayer().getMapId()) {
            case 921172300:
                c.getPlayer().warp(921172400);
                c.getSession().writeAndFlush(CField.environmentChange("Map/Effect2.img/event/gameover", 16));
                break;
            case 921171000:
                c.getPlayer().warp(921171100);
                long temp = Long.valueOf(c.getPlayer().getKeyValue(15901, "stage"));
                long temp2 = Long.valueOf(c.getPlayer().getKeyValue(15901, "selectedStage"));
                if (temp == temp2) {
                    c.getSession().writeAndFlush(CField.environmentChange("Map/Effect2.img/event/gameover", 16));
                } else if (temp > temp2) {
                    c.getSession().writeAndFlush(CField.environmentChange("Map/Effect3.img/hungryMuto/Clear", 16));
                    c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg((temp - 1) + "스테이지 클리어!"));
                }
                DreamBreakerRank.EditRecord(c.getPlayer().getName(), Long.valueOf(c.getPlayer().getKeyValue(15901, "best")), Long.valueOf(c.getPlayer().getKeyValue(15901, "besttime")));
                break;
            case 921172000:
            case 921172100:
                c.getPlayer().warp(921172200);
                break;
            default:
                c.getPlayer().dropMessage(6, "해당 버튼은 ExitSpecialGame에서 처리됩니다.");
                break;
        }
    }

    public static void HandleDreamBreakerSkill(MapleClient c, int SkillId) {
        try {
            int dream = (int) c.getPlayer().getKeyValue(15901, "dream");
            final EventInstanceManager em = c.getPlayer().getEventInstance();
            if (em == null) {
                return;
            }
            switch (SkillId) {
                case 0:
                    if (dream >= 200) {
                        c.getPlayer().setKeyValue(15901, "dream", String.valueOf(dream - 200));
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("게이지 홀드! 5초동안 게이지의 이동이 멈춥니다!"));
                        em.setProperty("gaugeHold", "true");

                        MapTimer.getInstance().schedule(() -> {
                            em.setProperty("gaugeHold", "false");
                        }, 5000);
                        break;
                    } else {
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("드림 포인트가 부족하여 스킬을 사용할 수 없습니다."));
                    }
                    break;
                case 1:
                    if (dream >= 300) {
                        c.getPlayer().setKeyValue(15901, "dream", String.valueOf(dream - 300));
                        List<MapleMonster> Orgels = new ArrayList<>();
                        for (MapleMonster m : c.getPlayer().getMap().getAllMonster()) {
                            if (m.getId() >= 9833080 && m.getId() <= 9833084) {
                                Orgels.add(m);
                            }

                        }

                        if (Orgels.size() > 0) {
                            c.getPlayer().getMap().killMonster(Orgels.get(Randomizer.nextInt(Orgels.size())), c.getPlayer(), false, false, (byte) 1);
                            c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("자각의 종소리를 울려 한 곳의 오르골이 깨어났습니다!"));
                        } else {
                            c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("모든 오르골이 이미 깨어있는 상태입니다."));
                        }
                        break;
                    } else {
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("드림 포인트가 부족하여 스킬을 사용할 수 없습니다."));
                    }
                    break;
                case 2:
                    if (dream >= 400) {
                        c.getPlayer().setKeyValue(15901, "dream", String.valueOf(dream - 400));
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("꿈속의 헝겊인형이 소환되어 몬스터들을 도발합니다!"));
                        MapleMonster m = MapleLifeFactory.getMonster(9833100);
                        m.setHp((long) (m.getStats().getHp()));
                        m.getStats().setHp((long) (m.getStats().getHp()));
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(m, c.getPlayer().getPosition());
                        break;
                    } else {
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("드림 포인트가 부족하여 스킬을 사용할 수 없습니다."));
                    }
                    break;
                case 3:
                    if (dream >= 900) {
                        c.getPlayer().setKeyValue(15901, "dream", String.valueOf(dream - 900));
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("숙면의 오르골을 공격하던 모든 몬스터가 사라졌습니다!"));
                        for (MapleMonster m : c.getPlayer().getMap().getAllMonster()) {
                            switch (m.getId()) {
                                case 9833070:
                                case 9833071:
                                case 9833072:
                                case 9833073:
                                case 9833074:
                                case 9833080:
                                case 9833081:
                                case 9833082:
                                case 9833083:
                                case 9833084:
                                case 9833100:
                                    continue;
                                default:
                                    c.getPlayer().getMap().killMonster(m, c.getPlayer(), false, false, (byte) 1);
                            }
                        }
                        em.setProperty("stopSpawn", "true");

                        MapTimer.getInstance().schedule(() -> {
                            em.setProperty("stopSpawn", "false");
                        }, 10000);
                        break;
                    } else {
                        c.getSession().writeAndFlush(SLFCGPacket.DreamBreakerMsg("드림 포인트가 부족하여 스킬을 사용할 수 없습니다."));
                    }
                    break;
            }
            c.getSession().writeAndFlush(SLFCGPacket.DreamBreakeLockSkill(SkillId));

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            c.getSession().writeAndFlush(SLFCGPacket.DreamBreakeSkillRes());
        }
    }

    public static void HandleBingoClick(MapleClient c) {
        if (c.getPlayer().getBingoGame().getBingoTimer() == null || c.getPlayer().getBingoGame().getBingoTimer().isCancelled()) {
            return;
        }
        c.getPlayer().getBingoGame().addRank(c.getPlayer());
    }

    public static void ExitBlockGame(LittleEndianAccessor rh, MapleClient c) {
        c.getSession().writeAndFlush(SLFCGPacket.BlockGameCommandPacket(3));
        c.getPlayer().setBlockCount(0);
        // WorldBroadcasting.broadcastMessage(UIPacket.greenShowInfo("BlockGame Instance
        int block = c.getPlayer().getBlockCoin();
        c.getPlayer().setBlockCoin(0);
        // of " + c.getPlayer().getName() + " has Deleted"));
        Timer.ShowTimer.getInstance().schedule(() -> {
            c.getSession().writeAndFlush(UIPacket.IntroDisableUI(false));
            c.getSession().writeAndFlush(UIPacket.IntroLock(false));
            ChannelServer cserv = c.getChannelServer();
            MapleMap target = cserv.getMapFactory().getMap(100000000);
            c.getPlayer().changeMap(target, target.getPortal(0));
            c.getPlayer().gainNeoCore(block);
        }, 3500);
    }

    public static void HandleBlockGameRes(LittleEndianAccessor rh, MapleClient c) {
        byte type = rh.readByte();
        if (type == 0x03) {
            c.getSession().writeAndFlush(SLFCGPacket.BlockGameCommandPacket(3));
            c.getPlayer().setBlockCount(0);
            // WorldBroadcasting.broadcastMessage(UIPacket.greenShowInfo("BlockGame Instance
            // of " + c.getPlayer().getName() + " has Deleted"));
            int block = c.getPlayer().getBlockCoin();
            c.getPlayer().setBlockCoin(0);
            Timer.ShowTimer.getInstance().schedule(() -> {
                c.getSession().writeAndFlush(UIPacket.IntroDisableUI(false));
                c.getSession().writeAndFlush(UIPacket.IntroLock(false));
                ChannelServer cserv = c.getChannelServer();
                MapleMap target = cserv.getMapFactory().getMap(100000000);
                c.getPlayer().changeMap(target, target.getPortal(0));
                c.getPlayer().gainNeoCore(block);
            }, 3500);
        } else {
            c.getPlayer().addBlockCoin(type == 0x02 ? 2 : 1);
            int block = c.getPlayer().getBlockCount() + 1;
            c.getPlayer().setBlockCount(block);
            if (block == 60) {
                if (c.getPlayer().getKeyValue(20220311, "ove5") < 60) {
                    c.getPlayer().setKeyValue(20220311, "ove5", (int) c.getPlayer().getKeyValue(20220311, "ove5") + 60 + "");
                    //   c.getPlayer().getClient().getSession().writeAndFlush(CField.EffectPacket.showTextEffect(c.getPlayer(), "#fn나눔고딕 ExtraBold##fs30##fc0xFFA50000#[도전 미션] 높이높이 캐슬 " + c.getPlayer().getKeyValue(20220311, "ove5") + " 층 달성 !!", 0, 4));
                }
            }
            if (block % 10 == 0) {
                int velocity = 100 + ((block / 10) * 30);
                int misplaceallowance = 1 + (block / 10);
                c.getSession().writeAndFlush(SLFCGPacket.BlockGameControlPacket(velocity, misplaceallowance));
            }
        }
    }

    public static final void useMannequin(final LittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(4);
        final byte type = slea.readByte(); // 0 : hair, 1 : face
        final byte result = slea.readByte();
        byte slot = slea.readByte();
        byte temp = -1;
        int itemId = 0;
        boolean second = false;
        if (slea.available() < 4) {
            temp = slea.readByte();
            if (slea.available() >= 1) {
                second = slea.readByte() == 1;
            }
        } else if (slea.available() == 4) {
            itemId = slea.readInt();
        }

        if (GameConstants.isAngelicBuster(chr.getJob())) {
            second = chr.getDressup();
        }

        if (GameConstants.isZero(chr.getJob())) {
            second = chr.getGender() == 1 && chr.getSecondGender() == 0;
        }

        int[] banhair = {30070, 30071, 30072, 30073, 30074, 30075, 30076, 30077, 30080, 30081, 30082, 30083, 30084, 30085, 30086, 30087};

        if (type == 0) {
            if (result == 1) {
                if (itemId == 5680222) {
                    chr.getHairRoom().add(new MapleMannequin(0, -1, 0, 0));

                    chr.removeItem(itemId, -1);
                    chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 3, (byte) (chr.getHairRoom().size()), null));
                    chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 5, slot, null));
                }
            } else if (result == 2) {
                MapleMannequin hair = chr.getHairRoom().get(slot);

                for (int h : banhair) {
                    if (chr.getHair() == h || (second && chr.getSecondHair() == h)) {
                        chr.dropMessage(1, "이 헤어는 저장하실 수 없습니다.");
                        return;
                    }
                }
                hair.setValue(second ? chr.getSecondHair() : chr.getHair());
                hair.setBaseProb(second ? chr.getSecondBaseProb() : chr.getBaseProb());
                hair.setBaseColor(second ? chr.getSecondBaseColor() : chr.getBaseColor());
                hair.setAddColor(second ? chr.getSecondAddColor() : chr.getAddColor());

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, hair));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequinRes(type, result, 1));
            } else if (result == 3) {
                MapleMannequin hair = chr.getHairRoom().get(slot);
                hair.setValue(0);
                hair.setBaseProb(-1);
                hair.setBaseColor(0);
                hair.setAddColor(0);

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, hair));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequinRes(type, result, 1));
            } else if (result == 4) {
                MapleMannequin hair = chr.getHairRoom().get(slot);

                int oldHair = second ? chr.getSecondHair() : chr.getHair();
                int mBaseProb = second ? chr.getSecondBaseProb() : chr.getBaseProb();
                int mBaseColor = second ? chr.getSecondBaseColor() : chr.getBaseColor();
                int mAddColor = second ? chr.getSecondAddColor() : chr.getAddColor();

                if (second) {
                    chr.setSecondHair(hair.getValue());

                    chr.setSecondBaseProb(hair.getBaseProb());
                    chr.setSecondBaseColor(hair.getBaseColor());
                    chr.setSecondAddColor(hair.getAddColor());

                    chr.updateSingleStat(MapleStat.HAIR, chr.getHair());
                } else {
                    chr.setHair(hair.getValue());

                    chr.setBaseProb(hair.getBaseProb());
                    chr.setBaseColor(hair.getBaseColor());
                    chr.setAddColor(hair.getAddColor());

                    chr.updateSingleStat(MapleStat.HAIR, chr.getHair());
                }

                hair.setValue(oldHair);
                hair.setBaseProb(mBaseProb);
                hair.setBaseColor(mBaseColor);
                hair.setAddColor(mAddColor);

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, hair));
                chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 4, slot, null));

                chr.equipChanged();
            }
        } else if (type == 1) {
            if (result == 1) {
                if (itemId == 5680222) {
                    chr.getFaceRoom().add(new MapleMannequin(0, -1, -1, 0));

                    chr.removeItem(itemId, -1);
                    chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 3, (byte) (chr.getFaceRoom().size()), null));
                    chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 5, slot, null));
                }
            } else if (result == 2) {
                MapleMannequin face = chr.getFaceRoom().get(slot);
                face.setValue(second ? chr.getSecondFace() : chr.getFace());

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, face));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequinRes(type, result, 1));
            } else if (result == 3) {
                MapleMannequin face = chr.getFaceRoom().get(slot);
                face.setValue(0);
                face.setBaseProb(-1);
                face.setBaseColor(0);
                face.setAddColor(0);

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, face));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequinRes(type, result, 1));
            } else if (result == 4) {
                MapleMannequin face = chr.getFaceRoom().get(slot);

                int oldFace = second ? chr.getSecondFace() : chr.getFace();

                if (second) {
                    chr.setSecondFace(face.getValue());
                    chr.updateSingleStat(MapleStat.FACE, chr.getSecondFace());
                } else {
                    chr.setFace(face.getValue());
                    chr.updateSingleStat(MapleStat.FACE, chr.getFace());
                }

                face.setValue(oldFace);
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, face));
                chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 4, slot, null));

                chr.equipChanged();
            }
        } else if (type == 2) {
            if (result == 1) {
                if (itemId == 5680222) {
                    chr.getSkinRoom().add(new MapleMannequin(0, -1, -1, 0));

                    chr.removeItem(itemId, -1);
                    chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 3, (byte) (chr.getSkinRoom().size()), null));
                    chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 5, slot, null));
                }
            } else if (result == 2) {
                MapleMannequin skin = chr.getSkinRoom().get(slot);
                int value = second ? chr.getSecondSkinColor() : chr.getSkinColor();
                skin.setValue(value + 12000);

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, skin));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequinRes(type, result, 1));
            } else if (result == 3) {
                MapleMannequin skin = chr.getSkinRoom().get(slot);
                skin.setValue(0);
                skin.setBaseProb(-1);
                skin.setBaseColor(0);
                skin.setAddColor(0);

                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, skin));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequinRes(type, result, 1));
            } else if (result == 4) {
                MapleMannequin skin = chr.getSkinRoom().get(slot);

                int oldSkin = second ? chr.getSecondSkinColor() : chr.getSkinColor();

                if (second) {
                    chr.setSecondSkinColor((byte) (skin.getValue() - 12000));
                    chr.updateSingleStat(MapleStat.SKIN, chr.getSecondSkinColor());
                } else {
                    chr.setSkinColor((byte) (skin.getValue() - 12000));
                    chr.updateSingleStat(MapleStat.SKIN, chr.getSkinColor());
                }

                skin.setValue(oldSkin);
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 2, slot, skin));
                chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
                chr.getClient().getSession().writeAndFlush(CWvsContext.mannequin(type, result, (byte) 4, slot, null));

                chr.equipChanged();
            }
        }
    }

    public static void UseChooseAbility(final LittleEndianAccessor slea, final MapleClient c) {
        slea.skip(4);
        byte type = slea.readByte();
        if (c.getPlayer().innerCirculator == null) {
            //error
            return;
        } else {
            if (type == 1) { // after
                for (InnerSkillValueHolder inner : c.getPlayer().getInnerSkills()) {
                    c.getPlayer().changeSkillLevel(inner.getSkillId(), (byte) 0, (byte) 0);
                }
                c.getPlayer().getInnerSkills().clear();

                for (InnerSkillValueHolder inner : c.getPlayer().innerCirculator) {
                    c.getPlayer().getInnerSkills().add(inner);
                    c.getPlayer().changeSkillLevel_Inner(inner.getSkillId(), inner.getSkillLevel(), inner.getMaxLevel());
                    c.getSession().writeAndFlush(CField.updateInnerAbility(inner, c.getPlayer().getInnerSkills().size(), c.getPlayer().getInnerSkills().size() == 3));
                }

            }
            c.getPlayer().innerCirculator = null;
        }
    }

    public static void EnterDungen(final LittleEndianAccessor mplew, MapleClient c) {
        final String d = mplew.readMapleAsciiString();
        NPCScriptManager.getInstance().start(c, 9001174, null, d);
    }

    public static void ICBM(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);
        int skill = slea.readInt();
        slea.skip(4);

        MapleStatEffect eff = SkillFactory.getSkill(skill).getEffect(c.getPlayer().getSkillLevel(skill));

        if (eff.getCooldown(c.getPlayer()) > 0 && c.getPlayer().getCooldownLimit(skill) == 0) {
            c.getPlayer().addCooldown(skill, System.currentTimeMillis(), eff.getCooldown(c.getPlayer()));
            c.getSession().writeAndFlush(CField.skillCooldown(skill, eff.getCooldown(c.getPlayer())));
            if (GameConstants.isLinkedSkill(skill) && c.getPlayer().getCooldownLimit(GameConstants.getLinkedSkill(skill)) == 0) {
                c.getPlayer().addCooldown(GameConstants.getLinkedSkill(skill), System.currentTimeMillis(), eff.getCooldown(c.getPlayer()));
                c.getSession().writeAndFlush(CField.skillCooldown(GameConstants.getLinkedSkill(skill), eff.getCooldown(c.getPlayer())));
            }
        }

        short size = slea.readShort();
        for (int i = 0; i < size; i++) {
            Rectangle poz = new Rectangle(slea.readInt(), slea.readInt(), slea.readInt(), slea.readInt() + 20);
            c.getPlayer().getMap().spawnMist(new MapleMist(poz, c.getPlayer(), eff, 1300, (byte) (c.getPlayer().isFacingLeft() ? 1 : 0)), false);
        }

        /*        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
         statups.put(MapleBuffStat.KeyDownMoving, new Pair<>(0, 0));
         c.getSession().writeAndFlush(BuffPacket.cancelBuff(statups, c.getPlayer()));
         c.getPlayer().getMap().broadcastMessage(c.getPlayer(), BuffPacket.cancelForeignBuff(c.getPlayer(), statups), false);*/
    }

    public static void DimentionSword(LittleEndianAccessor slea, MapleClient c) {
        int skillId = slea.readInt();

        MapleStatEffect eff = SkillFactory.getSkill(skillId).getEffect(c.getPlayer().getSkillLevel(skillId));

        if (eff.getCooldown(c.getPlayer()) > 0 && c.getPlayer().getCooldownLimit(skillId) == 0) {
            c.getPlayer().addCooldown(skillId, System.currentTimeMillis(), eff.getCooldown(c.getPlayer()));
            c.getSession().writeAndFlush(CField.skillCooldown(skillId, eff.getCooldown(c.getPlayer())));
            if (GameConstants.isLinkedSkill(skillId) && c.getPlayer().getCooldownLimit(GameConstants.getLinkedSkill(skillId)) == 0) {
                c.getPlayer().addCooldown(GameConstants.getLinkedSkill(skillId), System.currentTimeMillis(), eff.getCooldown(c.getPlayer()));
                c.getSession().writeAndFlush(CField.skillCooldown(GameConstants.getLinkedSkill(skillId), eff.getCooldown(c.getPlayer())));
            }
        }

        if (skillId == 400011090 && c.getPlayer().getBuffedValue(400011090)) {
            MapleBuffStatValueHolder mbsvh = c.getPlayer().checkBuffStatValueHolder(MapleBuffStat.IgnoreMobPdpR, 400011090);
            int duration = mbsvh.localDuration / SkillFactory.getSkill(400011090).getEffect(c.getPlayer().getSkillLevel(400011090)).getQ();
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.IndieSummon, 400011090);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.IgnoreMobPdpR, 400011090);
            SkillFactory.getSkill(400011102).getEffect(c.getPlayer().getSkillLevel(skillId)).applyTo(c.getPlayer(), duration);

        } else if (skillId == 400051046) {
            if (!c.getPlayer().getUseTruthDoor()) {
                if (c.getPlayer().getBuffedValue(400051046)) {
                    MapleSummon summon = c.getPlayer().getSummon(400051046);
                    if (summon != null) {
                        c.getPlayer().setUseTruthDoor(true);
                        c.getPlayer().getClient().getSession().writeAndFlush(SummonPacket.DeathAttack(summon, 10));
                    }
                }
            }
        }
    }

    public static void cancelAfter(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        byte type = slea.readByte();

        MapleCharacter chr = c.getPlayer();

        chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);

        Skill skill = SkillFactory.getSkill(skillid);

        if (skillid == 63121008) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.NotDamaged, 63121008);
        }
        if (skillid == 164121042) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.DreamDowon, 164121042);
            chr.cancelEffectFromBuffStat(MapleBuffStat.NotDamaged, 164121042);
        }

        if (skillid == 162121022) {
            MapleStatEffect eff = skill.getEffect(chr.getSkillLevel(skillid));
            int duration = eff.getDuration();
            duration -= System.currentTimeMillis() - chr.getKeyDownSkill_Time();
            duration = duration / 1000 * 1000;
            if (duration > 0) {
                chr.changeCooldown(skillid, (int) -(duration * 3.5));
            }
        }

        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0);

            /*            Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
             statups.put(MapleBuffStat.KeyDownMoving, new Pair<>(0, 0));
             c.getSession().writeAndFlush(BuffPacket.cancelBuff(statups, chr));
             chr.getMap().broadcastMessage(c.getPlayer(), BuffPacket.cancelForeignBuff(chr, statups), false);*/
        }
    }

    public static void autoSkill(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        if (c.getPlayer().getKeyValue(1544, String.valueOf(skillid)) == 1) {
            c.getPlayer().setKeyValue(1544, String.valueOf(skillid), String.valueOf(0));
        } else {
            c.getPlayer().setKeyValue(1544, String.valueOf(skillid), String.valueOf(1));
        }
    }

    public static void lockSkill(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        if (skillid == 13111023) {
            if (c.getPlayer().getKeyValue(1544, "alba") == 1) {
                c.getPlayer().setKeyValue(1544, "alba", String.valueOf(0));
            } else {
                c.getPlayer().setKeyValue(1544, "alba", String.valueOf(1));
            }
        } else if (skillid == 20040219) {
            c.getPlayer().setKeyValue(60002, "aa", (c.getPlayer().getKeyValue(60002, "aa") == 1 ? "0" : "1"));
            c.getPlayer().getMap().broadcastMessage(CField.HommingRoket(c.getPlayer().getId(), (byte) c.getPlayer().getKeyValue(60002, "aa"), 20040219));
            if (c.getPlayer().getKeyValue(1555, "eq") == 1) {
                c.getPlayer().updateInfoQuest(1544, "20040219=" + 0 + ";");
                c.getPlayer().setKeyValue(1555, "eq", String.valueOf(0));
            } else {
                c.getPlayer().updateInfoQuest(1544, "20040219=" + 1 + ";");
                c.getPlayer().setKeyValue(1555, "eq", String.valueOf(1));
            }
        } else if (skillid == 14001026) {
            if (c.getPlayer().getKeyValue(1566, "dq") == 1) {
                c.getPlayer().updateInfoQuest(1544, "14001026=" + 0 + ";");
                c.getPlayer().setKeyValue(1566, "dq", String.valueOf(0));
            } else {
                c.getPlayer().updateInfoQuest(1544, "14001026=" + 1 + ";");
                c.getPlayer().setKeyValue(1566, "dq", String.valueOf(1));
            }
        }
    }

    public static void lockJump(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        if (skillid == 30010110) {
            if (c.getPlayer().getKeyValue(21770, "ds0") == 1) {
                c.getPlayer().setKeyValue(21770, "ds0", String.valueOf(0));
            } else {
                c.getPlayer().setKeyValue(21770, "ds0", String.valueOf(1));
            }
        }
    }

    public static void PoisonNova(LittleEndianAccessor slea, MapleClient c) {
        List<Integer> novas = new ArrayList<>();
        int size = slea.readInt();
        for (int i = 0; i < size; ++i) {
            novas.add(slea.readInt());
        }

        c.getPlayer().setPosionNovas(novas);

//		c.getSession().writeAndFlush(CField.poisonNova(c.getPlayer(), novas));
    }

    public static void useMoonGauge(MapleClient c) {
        System.out.println("dd");
        if (c.getPlayer().getMapId() == 450008150 || c.getPlayer().getMapId() == 450008750) {

            String name = c.getPlayer().getTruePosition().y > -1000 ? "ptup" : "ptdown";
            c.getPlayer().setMoonGauge(Math.max(0, c.getPlayer().getMoonGauge() - 45));
            c.getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(c.getPlayer().getMoonGauge()));
            c.getSession().writeAndFlush(MobPacket.BossWill.teleport());
            c.getSession().writeAndFlush(CField.portalTeleport(name));
        } else if (c.getPlayer().getMapId() == 450008250 || c.getPlayer().getMapId() == 450008850) {

            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.DebuffIncHp);

            c.getPlayer().setMoonGauge(Math.max(0, c.getPlayer().getMoonGauge() - 50));
            c.getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(c.getPlayer().getMoonGauge()));
            c.getSession().writeAndFlush(MobPacket.BossWill.cooldownMoonGauge(7000));

            Timer.ShowTimer.getInstance().schedule(() -> {
                SkillFactory.getSkill(80002404).getEffect(1).applyTo(c.getPlayer(), true);
            }, 7000);
        } else if (c.getPlayer().getMapId() == 450008350 || c.getPlayer().getMapId() == 450008950) {

            c.getPlayer().setMoonGauge(Math.max(0, c.getPlayer().getMoonGauge() - 5));
            c.getPlayer().clearWeb = 2;
            c.getSession().writeAndFlush(MobPacket.BossWill.addMoonGauge(c.getPlayer().getMoonGauge()));
            c.getSession().writeAndFlush(MobPacket.BossWill.cooldownMoonGauge(5000));

            Timer.ShowTimer.getInstance().schedule(() -> {
                c.getPlayer().clearWeb = 0;
            }, 5000);
        }
    }

    public static void touchSpider(LittleEndianAccessor slea, MapleClient c) {
        SpiderWeb web = (SpiderWeb) c.getPlayer().getMap().getMapObject(slea.readInt(), MapleMapObjectType.WEB);

        if (web == null) {
            return;
        }

        if (c.getPlayer().clearWeb > 0) {
            c.getPlayer().getMap().broadcastMessage(MobPacket.BossWill.willSpider(4, web));
            c.getPlayer().getMap().removeMapObject(web);
            c.getPlayer().clearWeb--;
        } else {
            long hp = c.getPlayer().getStat().getCurrentMaxHp() * 30 / 100;
            c.getPlayer().addHP(-hp);

            c.getPlayer().disease(120, 40);
        }
    }

    public static void SkillToCrystal(LittleEndianAccessor slea, MapleClient c) {
        int skillId = slea.readInt();

        MapleSummon summon = c.getPlayer().getSummon(152101000);

        Integer battery = c.getPlayer().getBuffedValue(MapleBuffStat.CrystalBattery);

        int attack = 0, max = 0;

        if (summon == null || battery == null) {
            c.getPlayer().dropMessage(1, "핵 시도 감지.");
            return;
        }

        if (skillId == 152001001 || skillId == 152120001 || skillId == 152120002 || skillId == 152121004) {
            if (c.getPlayer().getSkillLevel(152110001) > 0) {
                attack = 152110001;
            } else if (c.getPlayer().getSkillLevel(152100001) > 0) {
                attack = 152100001;
            }
        } else if (skillId == 152001002 || skillId == 152120003) {
            if (c.getPlayer().getSkillLevel(152110002) > 0) {
                attack = 152110002;
            } else if (c.getPlayer().getSkillLevel(152100002) > 0) {
                attack = 152100002;
            }
        }

        if (battery == 152100010) {
            max = 30;
        } else {
            max = 150;
        }

        if (skillId == 152001001 || skillId == 152120001 || skillId == 152120002) {
            summon.setEnergy(Math.min(max, summon.getEnergy() + (c.getPlayer().getBuffedValue(MapleBuffStat.FastCharge) != null ? 2 : 1)));
        } else if (skillId == 152121004) {
            summon.setEnergy(Math.min(max, summon.getEnergy() + (c.getPlayer().getBuffedValue(MapleBuffStat.FastCharge) != null ? 6 : 3)));
        } else if (skillId == 152001002 || skillId == 152120003) {
            summon.setEnergy(Math.min(max, summon.getEnergy() + (c.getPlayer().getBuffedValue(MapleBuffStat.FastCharge) != null ? 4 : 2)));
        }

        MapleStatEffect attackEff = SkillFactory.getSkill(attack).getEffect(c.getPlayer().getSkillLevel(attack));

        if (c.getPlayer().getCooldownLimit(attack) == 0) {
            c.getPlayer().addCooldown(attack, System.currentTimeMillis(), attackEff.getCooldown(c.getPlayer()));
            c.getSession().writeAndFlush(CField.skillCooldown(attack, attackEff.getCooldown(c.getPlayer())));

            if (attack == 152110001/* || attack == 152100001*/) {
                c.getSession().writeAndFlush(SummonPacket.specialSummon2(summon, attack));
            }
            c.getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 3));
        }

        if ((summon.getEnergy() >= 30 && summon.getCrystalSkills().size() == 0) || (summon.getEnergy() >= 60 && summon.getCrystalSkills().size() == 1) || (summon.getEnergy() >= 90 && summon.getCrystalSkills().size() == 2) || (summon.getEnergy() >= 150 && summon.getCrystalSkills().size() == 3)) {
            summon.getCrystalSkills().add(true);

            c.getSession().writeAndFlush(SummonPacket.transformSummon(summon, 2));

            c.getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
            c.getSession().writeAndFlush(SummonPacket.specialSummon(summon, 3));
        } else {
            c.getSession().writeAndFlush(SummonPacket.ElementalRadiance(summon, 2));
            c.getSession().writeAndFlush(SummonPacket.specialSummon(summon, 2));
        }

    }

    public static void buffFreezer(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4);

        boolean use = slea.readByte() == 1;

        int buffFreezer;
        if (c.getPlayer().itemQuantity(5133000) > 0) {
            buffFreezer = 5133000;
        } else {
            buffFreezer = 5133001;
        }

        if (use) {
            c.getPlayer().setUseBuffFreezer(true);
            c.getPlayer().removeItem(buffFreezer, -1);
        }

        c.getSession().writeAndFlush(CField.buffFreezer(buffFreezer, use));
    }

    public static void quickSlot(LittleEndianAccessor slea, MapleClient c) {
        int i = 0;
        if (c.getPlayer() != null) {
            while (slea.available() >= 4) {
                c.getPlayer().setKeyValue(333333, "quick" + i, String.valueOf(slea.readInt()));
                i++;
            }
        }
//        c.getSession().writeAndFlush(CField.quickSlot(c.getPlayer()));
    }

    public static void unlockTrinity(MapleClient c) {
        if (GameConstants.isAngelicBuster(c.getPlayer().getJob()) && c.getPlayer().getSkillLevel(65121101) > 0) {
            c.getSession().writeAndFlush(CField.lockSkill(65121101));
            c.getSession().writeAndFlush(CField.unlockSkill());
//			c.getSession().writeAndFlush(EffectPacket.showNormalEffect(c.getPlayer(), 49, true));
            //          c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.showNormalEffect(c.getPlayer(), 49, false), false);
        }
    }

    public static void checkCoreSecondpw(LittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        if (type == 0) {
            String secondpw = slea.readMapleAsciiString();

            if (c.CheckSecondPassword(secondpw)) {
                c.getSession().writeAndFlush(CWvsContext.openCore());
            }
        }
    }

    public static void inviteChair(LittleEndianAccessor slea, MapleClient c) {
        int targetId = slea.readInt();
        MapleCharacter target = c.getPlayer().getMap().getCharacterById(targetId);
        if (target != null) {
            c.getSession().writeAndFlush(CField.inviteChair(7));
            target.getClient().getSession().writeAndFlush(CField.requireChair(c.getPlayer().getId()));
        } else {
            c.getSession().writeAndFlush(CField.inviteChair(8));
        }
    }

    public static void resultChair(LittleEndianAccessor slea, MapleClient c) {
        int targetId = slea.readInt(); // 의자 주인ID
        int result = slea.readInt();
        if (result == 7) { // 수락
            MapleCharacter target = c.getPlayer().getMap().getCharacterById(targetId);
            if (target != null) {
                c.getSession().writeAndFlush(CField.resultChair(target.getChair(), 0));

                MapleSpecialChair chair = null;

                for (MapleSpecialChair chairz : target.getMap().getAllSpecialChairs()) {
                    if (chairz.getOwner().getId() == target.getId()) {
                        chair = chairz;
                        break;
                    }
                }

                if (chair != null) {
                    int[] randEmotions = {2, 10, 14, 17};
                    chair.updatePlayer(c.getPlayer(), randEmotions[Randomizer.nextInt(randEmotions.length)]);

                    c.getPlayer().setChair(target.getChair());

                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.showChair(c.getPlayer(), c.getPlayer().getChair()), false);

                    c.getPlayer().getMap().broadcastMessage(CField.specialChair(c.getPlayer(), true, false, true, chair));
                }
            } else {
                c.getSession().writeAndFlush(CField.resultChair(targetId, 1));
            }
        } else {
            c.getSession().writeAndFlush(CField.resultChair(targetId, 1));
        }
    }

    public static void bloodFist(LittleEndianAccessor slea, MapleClient c) {
        int skill = slea.readInt();
        if (skill == 400011038) {
            c.getPlayer().addHP(-c.getPlayer().getStat().getHp() * SkillFactory.getSkill(skill).getEffect(c.getPlayer().getSkillLevel(skill)).getX() / 100);
        }
    }

    public static void managementStackBuff(LittleEndianAccessor slea, MapleCharacter chr) {
        int skill = slea.readInt();

        if (SkillFactory.getSkill(skill) != null && chr != null) {
            if (chr.getSkillLevel(GameConstants.getLinkedSkill(skill)) > 0) {
                MapleStatEffect effect = SkillFactory.getSkill(skill).getEffect(chr.getSkillLevel(GameConstants.getLinkedSkill(skill)));

                if (chr.getBHGCCount() < effect.getY()) {
                    chr.setBHGCCount(chr.getBHGCCount() + 1);

                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.VMatrixStackBuff, new Pair<>(chr.getBHGCCount(), 0));

                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, effect, chr));

                    chr.lastBHGCGiveTime = System.currentTimeMillis();
                }
            }
        }
    }

    public static void managementKainStackBuff(LittleEndianAccessor slea, MapleCharacter chr) {
        int skill = slea.readInt();

        if (SkillFactory.getSkill(skill) != null && chr != null) {
            if (chr.getSkillLevel(GameConstants.getLinkedSkill(skill)) > 0) {
                MapleStatEffect effect = SkillFactory.getSkill(skill).getEffect(chr.getSkillLevel(GameConstants.getLinkedSkill(skill)));
                long time = System.currentTimeMillis();

                int cooltime = effect.getCooltime() == 0 ? effect.getU() * 1000 : effect.getCooltime();

                if (skill == 63101004) {
                    if (chr.KainscatteringShot < effect.getW() && time - chr.lastKainscatteringShot >= cooltime) {
                        chr.KainscatteringShot += 1;
                        chr.getClient().getSession().writeAndFlush(CField.KainStackSkill(63101004, chr.KainscatteringShot, effect.getW(), cooltime));
                        chr.lastKainscatteringShot = System.currentTimeMillis();
                    }

                } else if (skill == 63111003) {
                    if (chr.KainshaftBreak < effect.getU() && time - chr.lastKainshaftBreak >= cooltime) {
                        chr.KainshaftBreak += 1;
                        chr.getClient().getSession().writeAndFlush(CField.KainStackSkill(63111003, chr.KainshaftBreak, effect.getW(), cooltime));
                        chr.lastKainshaftBreak = System.currentTimeMillis();
                    }
                } else if (skill == 63121002) {
                    if (chr.KainfallingDust < effect.getW() && time - chr.lastKainfallingDust >= cooltime) {
                        chr.KainfallingDust += 1;
                        chr.getClient().getSession().writeAndFlush(CField.KainStackSkill(63121002, chr.KainfallingDust, effect.getW(), cooltime));
                        chr.lastKainfallingDust = System.currentTimeMillis();
                    }
                } else if (skill == 63121040) {
                    if (chr.KainsneakySnipingPre < effect.getW() && time - chr.lastKainsneakySnipingPre >= cooltime) {
                        chr.KainsneakySnipingPre += 1;
                        chr.getClient().getSession().writeAndFlush(CField.KainStackSkill(63121040, chr.KainsneakySnipingPre, effect.getW(), cooltime));
                        chr.lastKainsneakySnipingPre = System.currentTimeMillis();
                    }
                } else {
                    System.out.println("코딩되지 않은 카인 스택 스킬: " + skill);
                }
            }
        }
    }

    public static void updateMist(LittleEndianAccessor slea, MapleClient c) {
        int skillId = slea.readInt();
        int skillLevel = slea.readInt();
        Point pos = slea.readPos();
        if (skillId == 400031037) {
            skillId = 400031040;
        }

        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(skillLevel);

        for (MapleMist mist : c.getPlayer().getMap().getAllMistsThreadsafe()) {
            if (mist.getSource() != null) {
                if (mist.getSource().getSourceId() == skillId) {
                    c.getPlayer().getMap().broadcastMessage(CField.removeMist(mist));
                    mist.setPosition(pos);
                    mist.setBox(effect.calculateBoundingBox(pos, c.getPlayer().isFacingLeft()));
                    c.getPlayer().getMap().broadcastMessage(CField.spawnMist(mist));
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer(), true, false));
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer(), false, true));
                }
            }
        }
    }

    public static void combinationTraning(LittleEndianAccessor slea, MapleClient c) {
        int skillId = slea.readInt();

        if (c.getPlayer().getSkillLevel(37120012) > 0) {
            SkillFactory.getSkill(37120012).getEffect(c.getPlayer().getSkillLevel(37120012)).applyTo(c.getPlayer(), false);
        } else if (c.getPlayer().getSkillLevel(37110009) > 0) {
            SkillFactory.getSkill(37110009).getEffect(c.getPlayer().getSkillLevel(37110009)).applyTo(c.getPlayer(), false);
        }
    }

    public static void openHasteBox(LittleEndianAccessor slea, MapleCharacter chr) {
        byte state = slea.readByte();
        switch (state) {
            case 0:
                int id = slea.readInt(); // boxId

                String[] boxIds = {"M1", "M2", "M3", "M4", "M5", "M6"};

                if (chr.getKeyValue(500862, boxIds[id]) != 1) {
                    chr.dropMessage(1, "오류가 발생했습니다. 문의하세요.");
                    return;
                }

                int[][] items = {{4001832, 500}, {4001126, 100}};

                int[] item = items[Randomizer.nextInt(items.length)];
                chr.setKeyValue(500862, "openBox", String.valueOf(chr.getKeyValue(500862, "openBox") + 1));
                chr.setKeyValue(500862, "booster", String.valueOf(chr.getKeyValue(500862, "booster") + 1));

                if (chr.getKeyValue(500862, "openBox") == 6) {
                    chr.setKeyValue(500862, "str", "오늘의 일일 미션을 모두 완료하였습니다!");
                } else {
                    chr.setKeyValue(500862, "str", chr.getKeyValue(500862, "openBox") + "단계 상자 도전 중! 일일 미션 1개를 완료하세요!");
                }

                chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showEffect(chr, item[0], item[1], 8, 0, 1, (byte) 0, true, null, null, null));

                chr.getClient().getSession().writeAndFlush(CField.NPCPacket.getNPCTalk(0, (byte) 0, "#b#e<헤이스트 상자>#n#k에서 #b#e#i" + item[0] + ":# #t" + item[0] + ":# " + item[1] + "개#n#k를 획득하였다!", "00 01", (byte) 0x39));

//				if (chr.getKeyValue(500862, "openBox") == 6) {
                //				chr.setKeyValue(500081, "season", "2019/09");
                //			chr.setKeyValue(500081, "openH", "1");
                //	}
                break;
        }
    }

    public static void spotlightBuff(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        byte state = slea.readByte();
        int stack = slea.readInt();
        if (state == 1 && chr.getSkillLevel(400051018) > 0) {
            SkillFactory.getSkill(400051027).getEffect(chr.getSkillLevel(400051018)).applyTo(chr, false, stack);
        } else {
            if (chr.getBuffedEffect(400051027) != null) {
                chr.cancelEffect(c.getPlayer().getBuffedEffect(400051027), false, -1);
            }
        }
    }

    public static void bless5th(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        int skill = slea.readInt();
        if (skill == 400001050) {
            if (chr.getBuffedValue(400001050)) {
                int[] skills = {400001051, 400001053, 400001054, 400001055};
                chr.nextBlessSkill = skills[Randomizer.nextInt(skills.length)];

                Map<MapleBuffStat, Pair<Integer, Integer>> localstatups = new HashMap<>();
                localstatups.put(MapleBuffStat.Bless5th, new Pair<>(1, (int) chr.getBuffLimit(400001050)));
                c.getSession().writeAndFlush(BuffPacket.giveBuff(localstatups, chr.getBuffedEffect(400001050), chr));

                c.getSession().writeAndFlush(EffectPacket.showEffect(chr, 0, chr.nextBlessSkill, 1, 0, 0, (byte) (chr.isFacingLeft() ? 1 : 0), true, null, null, null));
                chr.getMap().broadcastMessage(chr, EffectPacket.showEffect(chr, 0, chr.nextBlessSkill, 1, 0, 0, (byte) (chr.isFacingLeft() ? 1 : 0), true, null, null, null), false);
            }
        }
    }

    public static void showICBM(LittleEndianAccessor slea, MapleCharacter player) {
        player.getMap().broadcastMessage(player, CField.showICBM(player.getId(), slea.readInt(), slea.readInt()), false);
    }

    public static void arkGauge(int readInt, MapleCharacter chr) {
        int gauge = readInt;

        if (GameConstants.isArk(chr.getJob())) {
            if (chr.getBuffedEffect(MapleBuffStat.SpectorTransForm) == null) {
                chr.SpectorGauge += 15;
                if (chr.SpectorGauge > 1000)
                    chr.SpectorGauge = 1000;
                Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                statups.put(MapleBuffStat.SpectorGauge, new Pair<>(1, 0));
                chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.SpectorGauge), chr));
            }
            if (chr.getBuffedValue(155101006) && chr.getBuffedEffect(MapleBuffStat.MemoryOfSource) == null) {
                chr.SpectorGauge -= 15;
                if (chr.SpectorGauge < 0)
                    chr.SpectorGauge = 0;
                if (chr.SpectorGauge == 0) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.SpectorTransForm);
                } else {
                    Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
                    statups.put(MapleBuffStat.SpectorGauge, new Pair<>(1, 0));
                    chr.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(statups, chr.getBuffedEffect(MapleBuffStat.SpectorGauge), chr));
                }
            }
        }
    }

    public static void quickPass(LittleEndianAccessor slea, MapleClient c) {
        int tt = slea.readInt();
        int type = slea.readInt();
        boolean left = slea.readByteToInt() == 1;

        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 3) {
            c.getPlayer().dropMessage(1, "장비를 3칸 이상 비워주세요.");
            return;
        }

        if (c.getPlayer().getDonationPoint() < 1500) {
            c.getPlayer().dropMessage(1, "후원 포인트가 부족합니다.");
            return;
        }

        if (left) {
            int j = Integer.parseInt(c.getPlayer().getV("arcane_quest_" + (type + 2))) + 1;
            int i = (int) (c.getPlayer().getKeyValue(39051, "c" + type) + 1);
            c.getPlayer().addKV("arcane_quest_" + (type + 2), String.valueOf(j));
            c.getPlayer().setKeyValue(39051, "c" + type, String.valueOf(i));
        } else {
            int i = (int) (c.getPlayer().getKeyValue(39052, "c" + type) + 1);
            switch (type) {
                case 0:
                    c.getPlayer().dropMessage(1, "이용 불가능합니다.");
                    return;
                case 1:
                    c.getPlayer().addKV("muto", String.valueOf(Integer.parseInt(c.getPlayer().getV("muto")) + 1));
                    c.getPlayer().setKeyValue(39052, "c1", String.valueOf(i));
                    break;
                case 2:
                    c.getPlayer().setKeyValue(20190131, "play", String.valueOf(c.getPlayer().getKeyValue(20190131, "play")) + 1);
                    c.getPlayer().setKeyValue(39052, "c2", String.valueOf(i));
                    break;
                case 3:
                    c.getPlayer().setKeyValue(16215, "play", String.valueOf(c.getPlayer().getKeyValue(16215, "play")) + 1);
                    c.getPlayer().setKeyValue(39052, "c3", String.valueOf(i));
                    break;
            }
        }

        c.getPlayer().gainItem(1712001 + type, 1);
        c.getPlayer().gainItem(1712001 + type, 1);
        c.getPlayer().gainItem(1712001 + type, 1);

        c.getPlayer().gainDonationPoint(-1500);
        c.getPlayer().dropMessage(1, "포인트를 사용하여 일일퀘스트를 완료했습니다.");
    }

    public static void selectDice(LittleEndianAccessor slea, MapleClient c) {
        int dice = slea.readInt();
        MapleStatEffect effect = SkillFactory.getSkill(400051000).getEffect(c.getPlayer().getTotalSkillLevel(400051000));
        effect.applyTo(c.getPlayer(), false, dice, true);
    }

    public static void battleStatistics(LittleEndianAccessor slea, MapleClient c) {
        c.getSession().writeAndFlush(CField.battleStatistics());
    }

    public static void goldCompleteByPass(MapleClient c) {
        if (Integer.parseInt(c.getKeyValue("goldDay")) == Integer.parseInt(c.getKeyValue("cMaxDay"))) {
            c.getPlayer().dropMessage(1, "모든 날짜에 출석하였습니다.");
            return;
        }

        /*						if (Integer.parseInt(c.getKeyValue("goldComplete")) == 1) {
         c.getPlayer().dropMessage(1, "오늘은 이미 출석하였습니다.");
         break;
         }*/
        if (c.getPlayer().getDonationPoint() < 3000) {
            c.getPlayer().dropMessage(1, "후원포인트 3000이 필요합니다.");
            return;
        }

        int value = (CurrentTime.요일() == 6 || CurrentTime.요일() == 0) ? 1 : 1;

        int k = Math.min(135, Integer.parseInt(c.getKeyValue("goldDay")) + value);

        for (Triple<Integer, Integer, Integer> item : GameConstants.chariotItems) {
            if (value == 1 ? item.left == k : (item.left == k || item.left == k - 1)) {
                if (!MapleInventoryManipulator.checkSpace(c, item.mid, item.right, "")) {
                    c.getPlayer().dropMessage(1, "보상을 받기 위한 인벤토리의 공간이 부족합니다.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                    return;
                } else if (item.mid == 4310291) {
                    c.getPlayer().dropMessage(1, "보상이 지급되었습니다.");
                    c.getPlayer().AddStarDustCoin(item.right);
                    break;
                } else {

                    Item addItem;

                    if (GameConstants.getInventoryType(item.mid) == MapleInventoryType.EQUIP) {
                        addItem = MapleItemInformationProvider.getInstance().getEquipById(item.mid);
                    } else {
                        int quantity = item.right;
                        addItem = new Item(item.mid, (short) 0, (short) quantity);
                    }

                    if (addItem != null) {
                        MapleInventoryManipulator.addbyItem(c, addItem);
                        c.getPlayer().dropMessage(1, "보상이 지급되었습니다.");
                    }
                    break;
                }
            }
        }

        c.setKeyValue("goldComplete", "1");
        c.setKeyValue("goldDay", String.valueOf(k));

        StringBuilder z = new StringBuilder(c.getKeyValue("passDate"));

        c.getPlayer().gainDonationPoint(-3000);

        if (value == 2) {
            c.setKeyValue("passDate", z.replace(k - 2, k - 1, "1").toString());
        }
        c.setKeyValue("passDate", z.replace(k - 1, k, "1").toString());
        c.getSession().writeAndFlush(InfoPacket.updateClientInfoQuest(239, "complete=" + c.getKeyValue("goldComplete") + ";day=" + c.getKeyValue("goldDay") + ";passCount=" + c.getKeyValue("passCount") + ";bMaxDay=" + c.getKeyValue("bMaxDay") + ";lastDate=" + c.getKeyValue("lastDate") + ";cMaxDay=" + c.getKeyValue("cMaxDay")));
        c.getSession().writeAndFlush(InfoPacket.updateClientInfoQuest(240, "passDate=" + c.getKeyValue("passDate")));
        c.getSession().writeAndFlush(CField.getGameMessage(18, "황금마차 골든패스를 사용했습니다."));
        c.getPlayer().dropMessage(1, "출석을 완료하여 도장 " + value + "개를 찍었습니다.");
    }

    public static void eventUIResult(LittleEndianAccessor slea, MapleClient c) {
        short type = slea.readShort(); // 30?
        int mapId = slea.readInt();
        switch (type) {
            case 11:
                if (c.getPlayer().getPlatformerTimer() != null) {
                    c.getPlayer().getPlatformerTimer().cancel(false);
                    c.getPlayer().setPlatformerTimer(null);
                }
                c.getPlayer().setPlatformerStageEnter(0L);
                c.getPlayer().warp(993001000);
                break;
            case 17: {
                c.getPlayer().setKeyValue(18772, "id", String.valueOf(mapId));
                //  c.getPlayer().changeMap(921172100, 0);
                NPCScriptManager.getInstance().start(c, 9010106, "union_rade");
                break;
            }
            case 30: {
                int objectId = slea.readInt();
                int idx = slea.readInt(); // buttonIdx

                switch (idx) {
                    case 1003: // pass
                        if (objectId == 100208) {
                            if (c.getPlayer().getMapId() == mapId) {
                                NPCScriptManager.getInstance().start(c, 2007, "goldCompleteByPass");
                            }
                        }
                        break;
                    case 1004: // complete
                        if (objectId == 100208) {
                            if (c.getPlayer().getMapId() == mapId) {
                                if (Integer.parseInt(c.getKeyValue("passCount")) <= 0) {
                                    c.getPlayer().dropMessage(1, "모든 도장을 다 사용하였습니다.");
                                    break;
                                }

                                if (Integer.parseInt(c.getKeyValue("goldDay")) == Integer.parseInt(c.getKeyValue("cMaxDay"))) {
                                    c.getPlayer().dropMessage(1, "모든 날짜에 출석하였습니다.");
                                    break;
                                }

                                if (Integer.parseInt(c.getKeyValue("goldComplete")) == 1) {
                                    c.getPlayer().dropMessage(1, "오늘은 이미 출석하였습니다.");
                                    break;
                                }

                                int value = (CurrentTime.요일() == 6 || CurrentTime.요일() == 0) ? 2 : 1;

                                int k = Math.min(135, Integer.parseInt(c.getKeyValue("goldDay")) + value);

                                for (Triple<Integer, Integer, Integer> item : GameConstants.chariotItems) {
                                    if (value == 1 ? item.left == k : (item.left == k || item.left == k - 1)) {
                                        if (!MapleInventoryManipulator.checkSpace(c, item.mid, item.right, "")) {
                                            c.getPlayer().dropMessage(1, "보상을 받기 위한 인벤토리의 공간이 부족합니다.");
                                            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
                                            return;
                                        } else if (item.mid == 4310291) {
                                            c.getPlayer().dropMessage(1, "보상이 지급되었습니다.");
                                            c.getPlayer().AddStarDustCoin(item.right);
                                            break;
                                        } else {
                                            Item addItem;

                                            if (GameConstants.getInventoryType(item.mid) == MapleInventoryType.EQUIP) {
                                                addItem = MapleItemInformationProvider.getInstance().getEquipById(item.mid);
                                            } else {
                                                int quantity = item.right;
                                                addItem = new Item(item.mid, (short) 0, (short) quantity);
                                            }

                                            if (addItem != null) {
                                                MapleInventoryManipulator.addbyItem(c, addItem);
                                                c.getPlayer().dropMessage(1, "보상이 지급되었습니다.");
                                            }
                                            break;
                                        }
                                    }
                                }

                                c.setKeyValue("goldComplete", "1");
                                c.setKeyValue("goldDay", String.valueOf(k));
                                int j = Integer.parseInt(c.getKeyValue("passCount")) - value;
                                c.setKeyValue("passCount", String.valueOf(j));

                                c.getSession().writeAndFlush(InfoPacket.updateClientInfoQuest(239, "complete=" + c.getKeyValue("goldComplete") + ";day=" + c.getKeyValue("goldDay") + ";passCount=" + c.getKeyValue("passCount") + ";bMaxDay=" + c.getKeyValue("bMaxDay") + ";lastDate=" + c.getKeyValue("lastDate") + ";cMaxDay=" + c.getKeyValue("cMaxDay")));
                                c.getSession().writeAndFlush(CField.getGameMessage(18, "황금마차 출석체크를 완료했습니다."));
                                c.getPlayer().dropMessage(1, "출석을 완료하여 도장 " + value + "개를 찍었습니다.");
                            }
                        }
                        break;
                }
                break;
            }
            case 38: { // HotelMaple
                short id = slea.readShort();
                if (id == 0) {
                    NPCScriptManager.getInstance().start(c, "hotelMaple");
                } else if (id == 1) {
                    int idx = slea.readInt(); // 배열
                    int[][] items = {{2631527, 20}, {2631878, 1}, {2430218, 2}};
                    if (items.length > idx) {
                        if (c.getPlayer().getKeyValue(501045, "lv") >= idx + 1) {
                            if (c.getPlayer().getKeyValue(501045, "reward" + idx) == 0) {
                                if (MapleInventoryManipulator.checkSpace(c, items[idx][0], items[idx][1], "")) {
                                    c.getPlayer().setKeyValue(501045, "reward" + (idx), "1");
                                    c.getPlayer().gainItem(items[idx][0], items[idx][1]);
                                } else {
                                    c.getPlayer().dropMessage(1, "인벤토리의 공간이 부족합니다.");
                                }
                            } else {
                                c.getPlayer().dropMessage(1, "이미 보상을 받았습니다.");
                            }
                        }
                    }
                } else if (id == 2) {
                    int idx = slea.readInt(); // 스킬 위치
                    NPCScriptManager.getInstance().start(c, idx, "hotelMapleSkill");
                }
                break;
            }
        }

        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    public static void wiilMoon(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(8);
        int state = slea.readInt();
        if (state == 2) {
            if (c.getPlayer().getMapId() >= 450009400 && c.getPlayer().getMapId() <= 450009500) {
                //   c.getPlayer().setDuskGauge(Math.min(1000, c.getPlayer().getDuskGauge() + 5));
                //   c.getPlayer().getClient().getSession().writeAndFlush(MobPacket.BossDusk.handleDuskGauge(c.getPlayer().isDuskBlind(), c.getPlayer().getDuskGauge(), 1000));
            } else {
                MapleMist moon = null;

                for (MapleMist mi : c.getPlayer().getMap().getAllMistsThreadsafe()) {
                    if (mi.getMobSkill() != null && mi.getMobSkill().getSkillId() == 242 && mi.getMobSkill().getSkillLevel() == 4) {
                        moon = mi;
                    }
                }

                Point pos = new Point(slea.readInt(), slea.readInt());

                if (moon != null) {
                    if (pos.y < 0 && moon.getBox().y == -2301) {
                        return;
                    } else if (pos.y >= 0 && moon.getBox().y == -122) {
                        return;
                    }
                }

                MapleMonster mob = c.getPlayer().getMap().getMonsterById(pos.y < 0 ? 8880304 : 8880303);
                if (mob == null) {
                    mob = c.getPlayer().getMap().getMonsterById(pos.y < 0 ? 8880344 : 8880343);
                }
                if (mob != null) {
                    MapleMist mist = new MapleMist(new Rectangle(-204, pos.y < 0 ? -2301 : -122, 408, 300), mob, MobSkillFactory.getMobSkill(242, 4), 30000);
                    if (pos.y < 0) {
                        mist.setPosition(new Point(0, -2021));
                    } else {
                        mist.setPosition(new Point(0, 158));
                    }
                    c.getPlayer().getMap().spawnMist(mist, false);
                }
            }
        }
    }

    public static void removeAdleProjectile(LittleEndianAccessor slea, MapleClient c) {
        int objectId = slea.readInt();
        if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
            c.getPlayer().getMap().removeAdelProjectile(c.getPlayer(), objectId);
        }
    }

    public static void ropeConnect(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
            int skillId = slea.readInt();
            short skilllv = slea.readShort();

            if (c.getPlayer().getSkillLevel(skillId) == skilllv) {
                int delay = slea.readInt();
                int x = slea.readInt();
                int y = slea.readInt();
                Point pos = new Point(x, y);
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), EffectPacket.showEffect(c.getPlayer(), delay, skillId, 1, 0, 0, (byte) 0, false, pos, "", null), false);
            }
        }
    }

    public static void psychicUltimateRecv(LittleEndianAccessor slea, MapleClient c) {
        Skill skill = SkillFactory.getSkill(142121005);
        if (c.getPlayer() != null && c.getPlayer().getSkillLevel(142121005) > 0) {
            MapleStatEffect effect = skill.getEffect(c.getPlayer().getSkillLevel(142121005));
            c.getPlayer().addCooldown(142121005, System.currentTimeMillis(), effect.getCooldown(c.getPlayer()));
            c.getSession().writeAndFlush(CField.skillCooldown(142121005, effect.getCooldown(c.getPlayer())));
        }
    }

    public static void warpGuildMap(LittleEndianAccessor slea, MapleCharacter player) {
        if (player == null) {
            return;
        }
        int id = slea.readInt();
        switch (id) {
            case 26015:
                player.changeMap(200000301, 0);
                break;
        }
    }

    public static void JobChange(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (!chr.haveItem(4310086)) {
            chr.dropMessage(1, "자유전직 코인이 없습니다.");
            return;
        }

        if (c.getPlayer().getJob() / 100 == 4) {
            Equip test2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
            if (test2 != null) {
                chr.dropMessage(1, "방패,보조무기,블레이드 는 해제해주셔야합니다.");
                return;
            }
        }
        int BeforeJob = chr.getJob();
        int AfterJob = slea.readInt();
        int unk = slea.readByte();//0이면 메소지불 1이면 코인지급

        switch (BeforeJob) {
            case 112:
            case 122:
            case 132:
            case 212:
            case 222:
            case 232:
            case 312:
            case 322:
            case 412:
            case 422:
            case 434:
            case 512:
            case 522:
            case 532:
                break;
            default:
                return;
        }

        if ((BeforeJob / 100 != AfterJob / 100) || (BeforeJob / 1000 != 0) || (AfterJob / 1000 != 0)) {
            chr.dropMessage(1, "직업 체인지 오류1");
            return;
        }
        /*   if (BeforeJob / 100 == 5) {//BeforeJob / 100 == 2 
         chr.dropMessage(1, "해적 직업군은 자유전직이 늦게 나옵니다");
         return;
         } */
        int NeedNum = 10000;
        if (chr.getLevel() <= 200) {
            NeedNum = 1;
        } else if (chr.getLevel() >= 201 && chr.getLevel() < 210) {
            NeedNum = 3;
        } else if (chr.getLevel() >= 210 && chr.getLevel() < 220) {
            NeedNum = 6;
        } else if (chr.getLevel() >= 220 && chr.getLevel() < 230) {
            NeedNum = 10;
        } else if (chr.getLevel() >= 230 && chr.getLevel() < 240) {
            NeedNum = 20;
        } else if (chr.getLevel() >= 240 && chr.getLevel() < 250) {
            NeedNum = 40;
        } else if (chr.getLevel() >= 250 && chr.getLevel() < 260) {
            NeedNum = 70;
        } else if (chr.getLevel() >= 260) {
            NeedNum = 100;
        }
        if (NeedNum == 10000) {
            chr.dropMessage(1, "자유전직 코인 계산 오류");
            return;
        }
        if (!chr.haveItem(4310086, NeedNum)) {
            chr.dropMessage(1, "자유전직 코인을 충분히 가지고 있지 않습니다. " + NeedNum + "개 필요합니다..");
            return;
        }

        chr.getClient().getSession().write(UIPacket.closeUI(3));
        chr.dispel();

        chr.removeItem(4310086, -NeedNum);

        chr.changeJob4310086(AfterJob);//직업변경

        switch (BeforeJob) {//스킬 0레벨로 초기화하기
            case 112:
            case 122:
            case 132:

                //▶히어로스킬레벨 0만들기시작
                chr.teachSkill(1001005, 0);
                chr.changeSkillLevel(1001005, (byte) 0, (byte) 0);
                chr.teachSkill(1001008, 0);
                chr.changeSkillLevel(1001008, (byte) 0, (byte) 0);
                chr.teachSkill(1000003, 0);
                chr.changeSkillLevel(1000003, (byte) 0, (byte) 0);
                chr.teachSkill(1000009, 0);
                chr.changeSkillLevel(1000009, (byte) 0, (byte) 0);
                chr.teachSkill(1101011, 0);
                chr.changeSkillLevel(1101011, (byte) 0, (byte) 0);
                chr.teachSkill(1101012, 0);
                chr.changeSkillLevel(1101012, (byte) 0, (byte) 0);
                chr.teachSkill(1101013, 0);
                chr.changeSkillLevel(1101013, (byte) 0, (byte) 0);
                chr.teachSkill(1101004, 0);
                chr.changeSkillLevel(1101004, (byte) 0, (byte) 0);
                chr.teachSkill(1101006, 0);
                chr.changeSkillLevel(1101006, (byte) 0, (byte) 0);
                chr.teachSkill(1100000, 0);
                chr.changeSkillLevel(1100000, (byte) 0, (byte) 0);
                chr.teachSkill(1100002, 0);
                chr.changeSkillLevel(1100002, (byte) 0, (byte) 0);
                chr.teachSkill(1100009, 0);
                chr.changeSkillLevel(1100009, (byte) 0, (byte) 0);
                chr.teachSkill(1111010, 0);
                chr.changeSkillLevel(1111010, (byte) 0, (byte) 0);
                chr.teachSkill(1111012, 0);
                chr.changeSkillLevel(1111012, (byte) 0, (byte) 0);
                chr.teachSkill(1111003, 0);
                chr.changeSkillLevel(1111003, (byte) 0, (byte) 0);
                chr.teachSkill(1111008, 0);
                chr.changeSkillLevel(1111008, (byte) 0, (byte) 0);
                chr.teachSkill(1110013, 0);
                chr.changeSkillLevel(1110013, (byte) 0, (byte) 0);
                chr.teachSkill(1110000, 0);
                chr.changeSkillLevel(1110000, (byte) 0, (byte) 0);
                chr.teachSkill(1110009, 0);
                chr.changeSkillLevel(1110009, (byte) 0, (byte) 0);
                chr.teachSkill(1110011, 0);
                chr.changeSkillLevel(1110011, (byte) 0, (byte) 0);
                chr.teachSkill(1121008, 0);
                chr.changeSkillLevel(1121008, (byte) 0, (byte) 0);
                chr.teachSkill(1121015, 0);
                chr.changeSkillLevel(1121015, (byte) 0, (byte) 0);
                chr.teachSkill(1121016, 0);
                chr.changeSkillLevel(1121016, (byte) 0, (byte) 0);
                chr.teachSkill(1120014, 0);
                chr.changeSkillLevel(1120014, (byte) 0, (byte) 0);
                chr.teachSkill(1121010, 0);
                chr.changeSkillLevel(1121010, (byte) 0, (byte) 0);
                chr.teachSkill(1121000, 0);
                chr.changeSkillLevel(1121000, (byte) 0, (byte) 0);
                chr.teachSkill(1121011, 0);
                chr.changeSkillLevel(1121011, (byte) 0, (byte) 0);
                chr.teachSkill(1120003, 0);
                chr.changeSkillLevel(1120003, (byte) 0, (byte) 0);
                chr.teachSkill(1120012, 0);
                chr.changeSkillLevel(1120012, (byte) 0, (byte) 0);
                chr.teachSkill(1120013, 0);
                chr.changeSkillLevel(1120013, (byte) 0, (byte) 0);
                chr.teachSkill(1120045, 0);
                chr.changeSkillLevel(1120045, (byte) 0, (byte) 0);
                chr.teachSkill(1120044, 0);
                chr.changeSkillLevel(1120044, (byte) 0, (byte) 0);
                chr.teachSkill(1120043, 0);
                chr.changeSkillLevel(1120043, (byte) 0, (byte) 0);
                chr.teachSkill(1120048, 0);
                chr.changeSkillLevel(1120048, (byte) 0, (byte) 0);
                chr.teachSkill(1120047, 0);
                chr.changeSkillLevel(1120047, (byte) 0, (byte) 0);
                chr.teachSkill(1120046, 0);
                chr.changeSkillLevel(1120046, (byte) 0, (byte) 0);
                chr.teachSkill(1120050, 0);
                chr.changeSkillLevel(1120050, (byte) 0, (byte) 0);
                chr.teachSkill(1120051, 0);
                chr.changeSkillLevel(1120051, (byte) 0, (byte) 0);
                chr.teachSkill(1120049, 0);
                chr.changeSkillLevel(1120049, (byte) 0, (byte) 0);
                chr.teachSkill(1121052, 0);
                chr.changeSkillLevel(1121052, (byte) 0, (byte) 0);
                chr.teachSkill(1121054, 0);
                chr.changeSkillLevel(1121054, (byte) 0, (byte) 0);
                chr.teachSkill(1121053, 0);
                chr.changeSkillLevel(1121053, (byte) 0, (byte) 0);
                chr.teachSkill(400011001, 0);
                chr.changeSkillLevel(400011001, (byte) 0, (byte) 0);
                chr.teachSkill(400011027, 0);
                chr.changeSkillLevel(400011027, (byte) 0, (byte) 0);
                chr.teachSkill(400011073, 0);
                chr.changeSkillLevel(400011073, (byte) 0, (byte) 0);

                //▶히어로스킬레벨 0만들기종료
                //▶팔라딘스킬레벨 0만들기시작
                chr.teachSkill(1001005, 0);
                chr.changeSkillLevel(1001005, (byte) 0, (byte) 0);
                chr.teachSkill(1001008, 0);
                chr.changeSkillLevel(1001008, (byte) 0, (byte) 0);
                chr.teachSkill(1000003, 0);
                chr.changeSkillLevel(1000003, (byte) 0, (byte) 0);
                chr.teachSkill(1000009, 0);
                chr.changeSkillLevel(1000009, (byte) 0, (byte) 0);
                chr.teachSkill(1201011, 0);
                chr.changeSkillLevel(1201011, (byte) 0, (byte) 0);
                chr.teachSkill(1201012, 0);
                chr.changeSkillLevel(1201012, (byte) 0, (byte) 0);
                chr.teachSkill(1200014, 0);
                chr.changeSkillLevel(1200014, (byte) 0, (byte) 0);
                chr.teachSkill(1201013, 0);
                chr.changeSkillLevel(1201013, (byte) 0, (byte) 0);
                chr.teachSkill(1201004, 0);
                chr.changeSkillLevel(1201004, (byte) 0, (byte) 0);
                chr.teachSkill(1200000, 0);
                chr.changeSkillLevel(1200000, (byte) 0, (byte) 0);
                chr.teachSkill(1200002, 0);
                chr.changeSkillLevel(1200002, (byte) 0, (byte) 0);
                chr.teachSkill(1200009, 0);
                chr.changeSkillLevel(1200009, (byte) 0, (byte) 0);
                chr.teachSkill(1211008, 0);
                chr.changeSkillLevel(1211008, (byte) 0, (byte) 0);
                chr.teachSkill(1211010, 0);
                chr.changeSkillLevel(1211010, (byte) 0, (byte) 0);
                chr.teachSkill(1211012, 0);
                chr.changeSkillLevel(1211012, (byte) 0, (byte) 0);
                chr.teachSkill(1211013, 0);
                chr.changeSkillLevel(1211013, (byte) 0, (byte) 0);
                chr.teachSkill(1211014, 0);
                chr.changeSkillLevel(1211014, (byte) 0, (byte) 0);
                chr.teachSkill(1211011, 0);
                chr.changeSkillLevel(1211011, (byte) 0, (byte) 0);
                chr.teachSkill(1210001, 0);
                chr.changeSkillLevel(1210001, (byte) 0, (byte) 0);
                chr.teachSkill(1210015, 0);
                chr.changeSkillLevel(1210015, (byte) 0, (byte) 0);
                chr.teachSkill(1210016, 0);
                chr.changeSkillLevel(1210016, (byte) 0, (byte) 0);
                chr.teachSkill(1221009, 0);
                chr.changeSkillLevel(1221009, (byte) 0, (byte) 0);
                chr.teachSkill(1221004, 0);
                chr.changeSkillLevel(1221004, (byte) 0, (byte) 0);
                chr.teachSkill(1221014, 0);
                chr.changeSkillLevel(1221014, (byte) 0, (byte) 0);
                chr.teachSkill(1221011, 0);
                chr.changeSkillLevel(1221011, (byte) 0, (byte) 0);
                chr.teachSkill(1221015, 0);
                chr.changeSkillLevel(1221015, (byte) 0, (byte) 0);
                chr.teachSkill(1220017, 0);
                chr.changeSkillLevel(1220017, (byte) 0, (byte) 0);
                chr.teachSkill(1221000, 0);
                chr.changeSkillLevel(1221000, (byte) 0, (byte) 0);
                chr.teachSkill(1221012, 0);
                chr.changeSkillLevel(1221012, (byte) 0, (byte) 0);
                chr.teachSkill(1221016, 0);
                chr.changeSkillLevel(1221016, (byte) 0, (byte) 0);
                chr.teachSkill(1220018, 0);
                chr.changeSkillLevel(1220018, (byte) 0, (byte) 0);
                chr.teachSkill(1220010, 0);
                chr.changeSkillLevel(1220010, (byte) 0, (byte) 0);
                chr.teachSkill(1220045, 0);
                chr.changeSkillLevel(1220045, (byte) 0, (byte) 0);
                chr.teachSkill(1220044, 0);
                chr.changeSkillLevel(1220044, (byte) 0, (byte) 0);
                chr.teachSkill(1220043, 0);
                chr.changeSkillLevel(1220043, (byte) 0, (byte) 0);
                chr.teachSkill(1220048, 0);
                chr.changeSkillLevel(1220048, (byte) 0, (byte) 0);
                chr.teachSkill(1220047, 0);
                chr.changeSkillLevel(1220047, (byte) 0, (byte) 0);
                chr.teachSkill(1220046, 0);
                chr.changeSkillLevel(1220046, (byte) 0, (byte) 0);
                chr.teachSkill(1220050, 0);
                chr.changeSkillLevel(1220050, (byte) 0, (byte) 0);
                chr.teachSkill(1220051, 0);
                chr.changeSkillLevel(1220051, (byte) 0, (byte) 0);
                chr.teachSkill(1220049, 0);
                chr.changeSkillLevel(1220049, (byte) 0, (byte) 0);
                chr.teachSkill(1221052, 0);
                chr.changeSkillLevel(1221052, (byte) 0, (byte) 0);
                chr.teachSkill(1221054, 0);
                chr.changeSkillLevel(1221054, (byte) 0, (byte) 0);
                chr.teachSkill(1221053, 0);
                chr.changeSkillLevel(1221053, (byte) 0, (byte) 0);
                chr.teachSkill(400011003, 0);
                chr.changeSkillLevel(400011003, (byte) 0, (byte) 0);
                chr.teachSkill(400011052, 0);
                chr.changeSkillLevel(400011052, (byte) 0, (byte) 0);
                chr.teachSkill(400011072, 0);
                chr.changeSkillLevel(400011072, (byte) 0, (byte) 0);
                //▶팔라딘스킬레벨 0만들기종료

                //▶다크나이트스킬레벨 0만들기시작
                chr.teachSkill(1001005, 0);
                chr.changeSkillLevel(1001005, (byte) 0, (byte) 0);
                chr.teachSkill(1001008, 0);
                chr.changeSkillLevel(1001008, (byte) 0, (byte) 0);
                chr.teachSkill(1000003, 0);
                chr.changeSkillLevel(1000003, (byte) 0, (byte) 0);
                chr.teachSkill(1000009, 0);
                chr.changeSkillLevel(1000009, (byte) 0, (byte) 0);
                chr.teachSkill(1301011, 0);
                chr.changeSkillLevel(1301011, (byte) 0, (byte) 0);
                chr.teachSkill(1301013, 0);
                chr.changeSkillLevel(1301013, (byte) 0, (byte) 0);
                chr.teachSkill(1301012, 0);
                chr.changeSkillLevel(1301012, (byte) 0, (byte) 0);
                chr.teachSkill(1301004, 0);
                chr.changeSkillLevel(1301004, (byte) 0, (byte) 0);
                chr.teachSkill(1301006, 0);
                chr.changeSkillLevel(1301006, (byte) 0, (byte) 0);
                chr.teachSkill(1301007, 0);
                chr.changeSkillLevel(1301007, (byte) 0, (byte) 0);
                chr.teachSkill(1300000, 0);
                chr.changeSkillLevel(1300000, (byte) 0, (byte) 0);
                chr.teachSkill(1300002, 0);
                chr.changeSkillLevel(1300002, (byte) 0, (byte) 0);
                chr.teachSkill(1300009, 0);
                chr.changeSkillLevel(1300009, (byte) 0, (byte) 0);
                chr.teachSkill(1311011, 0);
                chr.changeSkillLevel(1311011, (byte) 0, (byte) 0);
                chr.teachSkill(1311012, 0);
                chr.changeSkillLevel(1311012, (byte) 0, (byte) 0);
                chr.teachSkill(1311014, 0);
                chr.changeSkillLevel(1311014, (byte) 0, (byte) 0);
                chr.teachSkill(1311015, 0);
                chr.changeSkillLevel(1311015, (byte) 0, (byte) 0);
                chr.teachSkill(1310013, 0);
                chr.changeSkillLevel(1310013, (byte) 0, (byte) 0);
                chr.teachSkill(1310009, 0);
                chr.changeSkillLevel(1310009, (byte) 0, (byte) 0);
                chr.teachSkill(1310010, 0);
                chr.changeSkillLevel(1310010, (byte) 0, (byte) 0);
                chr.teachSkill(1310016, 0);
                chr.changeSkillLevel(1310016, (byte) 0, (byte) 0);
                chr.teachSkill(1321013, 0);
                chr.changeSkillLevel(1321013, (byte) 0, (byte) 0);
                chr.teachSkill(1321014, 0);
                chr.changeSkillLevel(1321014, (byte) 0, (byte) 0);
                chr.teachSkill(1321012, 0);
                chr.changeSkillLevel(1321012, (byte) 0, (byte) 0);
                chr.teachSkill(1320017, 0);
                chr.changeSkillLevel(1320017, (byte) 0, (byte) 0);
                chr.teachSkill(1321015, 0);
                chr.changeSkillLevel(1321015, (byte) 0, (byte) 0);
                chr.teachSkill(1320016, 0);
                chr.changeSkillLevel(1320016, (byte) 0, (byte) 0);
                chr.teachSkill(1320018, 0);
                chr.changeSkillLevel(1320018, (byte) 0, (byte) 0);
                chr.teachSkill(1320011, 0);
                chr.changeSkillLevel(1320011, (byte) 0, (byte) 0);
                chr.teachSkill(1321000, 0);
                chr.changeSkillLevel(1321000, (byte) 0, (byte) 0);
                chr.teachSkill(1321010, 0);
                chr.changeSkillLevel(1321010, (byte) 0, (byte) 0);
                chr.teachSkill(1320043, 0);
                chr.changeSkillLevel(1320043, (byte) 0, (byte) 0);
                chr.teachSkill(1320044, 0);
                chr.changeSkillLevel(1320044, (byte) 0, (byte) 0);
                chr.teachSkill(1320045, 0);
                chr.changeSkillLevel(1320045, (byte) 0, (byte) 0);
                chr.teachSkill(1320046, 0);
                chr.changeSkillLevel(1320046, (byte) 0, (byte) 0);
                chr.teachSkill(1320047, 0);
                chr.changeSkillLevel(1320047, (byte) 0, (byte) 0);
                chr.teachSkill(1320048, 0);
                chr.changeSkillLevel(1320048, (byte) 0, (byte) 0);
                chr.teachSkill(1320049, 0);
                chr.changeSkillLevel(1320049, (byte) 0, (byte) 0);
                chr.teachSkill(1320050, 0);
                chr.changeSkillLevel(1320050, (byte) 0, (byte) 0);
                chr.teachSkill(1320051, 0);
                chr.changeSkillLevel(1320051, (byte) 0, (byte) 0);
                chr.teachSkill(1321052, 0);
                chr.changeSkillLevel(1321052, (byte) 0, (byte) 0);
                chr.teachSkill(1321053, 0);
                chr.changeSkillLevel(1321053, (byte) 0, (byte) 0);
                chr.teachSkill(1321054, 0);
                chr.changeSkillLevel(1321054, (byte) 0, (byte) 0);
                chr.teachSkill(400011004, 0);
                chr.changeSkillLevel(400011004, (byte) 0, (byte) 0);
                chr.teachSkill(400011054, 0);
                chr.changeSkillLevel(400011054, (byte) 0, (byte) 0);
                chr.teachSkill(400011068, 0);
                chr.changeSkillLevel(400011068, (byte) 0, (byte) 0);
                //▶다크나이트스킬레벨 0만들기종료
                break;
            case 212:
            case 222:
            case 232:
                //▶불독스킬레벨 0만들기시작
                chr.teachSkill(2101004, 0);
                chr.changeSkillLevel(2101004, (byte) 0, (byte) 0);
                chr.teachSkill(2100009, 0);
                chr.changeSkillLevel(2100009, (byte) 0, (byte) 0);
                chr.teachSkill(2101005, 0);
                chr.changeSkillLevel(2101005, (byte) 0, (byte) 0);
                chr.teachSkill(2101001, 0);
                chr.changeSkillLevel(2101001, (byte) 0, (byte) 0);
                chr.teachSkill(2101008, 0);
                chr.changeSkillLevel(2101008, (byte) 0, (byte) 0);
                chr.teachSkill(2101010, 0);
                chr.changeSkillLevel(2101010, (byte) 0, (byte) 0);
                chr.teachSkill(2100006, 0);
                chr.changeSkillLevel(2100006, (byte) 0, (byte) 0);
                chr.teachSkill(2100007, 0);
                chr.changeSkillLevel(2100007, (byte) 0, (byte) 0);
                chr.teachSkill(2100000, 0);
                chr.changeSkillLevel(2100000, (byte) 0, (byte) 0);
                chr.teachSkill(2111002, 0);
                chr.changeSkillLevel(2111002, (byte) 0, (byte) 0);
                chr.teachSkill(2111003, 0);
                chr.changeSkillLevel(2111003, (byte) 0, (byte) 0);
                chr.teachSkill(2111010, 0);
                chr.changeSkillLevel(2111010, (byte) 0, (byte) 0);
                chr.teachSkill(2111011, 0);
                chr.changeSkillLevel(2111011, (byte) 0, (byte) 0);
                chr.teachSkill(2111008, 0);
                chr.changeSkillLevel(2111008, (byte) 0, (byte) 0);
                chr.teachSkill(2111007, 0);
                chr.changeSkillLevel(2111007, (byte) 0, (byte) 0);
                chr.teachSkill(2110012, 0);
                chr.changeSkillLevel(2110012, (byte) 0, (byte) 0);
                chr.teachSkill(2110001, 0);
                chr.changeSkillLevel(2110001, (byte) 0, (byte) 0);
                chr.teachSkill(2110009, 0);
                chr.changeSkillLevel(2110009, (byte) 0, (byte) 0);
                chr.teachSkill(2110000, 0);
                chr.changeSkillLevel(2110000, (byte) 0, (byte) 0);
                chr.teachSkill(2121006, 0);
                chr.changeSkillLevel(2121006, (byte) 0, (byte) 0);
                chr.teachSkill(2121003, 0);
                chr.changeSkillLevel(2121003, (byte) 0, (byte) 0);
                chr.teachSkill(2120014, 0);
                chr.changeSkillLevel(2120014, (byte) 0, (byte) 0);
                chr.teachSkill(2121007, 0);
                chr.changeSkillLevel(2121007, (byte) 0, (byte) 0);
                chr.teachSkill(2121011, 0);
                chr.changeSkillLevel(2121011, (byte) 0, (byte) 0);
                chr.teachSkill(2121004, 0);
                chr.changeSkillLevel(2121004, (byte) 0, (byte) 0);
                chr.teachSkill(2121005, 0);
                chr.changeSkillLevel(2121005, (byte) 0, (byte) 0);
                chr.teachSkill(2121000, 0);
                chr.changeSkillLevel(2121000, (byte) 0, (byte) 0);
                chr.teachSkill(2121008, 0);
                chr.changeSkillLevel(2121008, (byte) 0, (byte) 0);
                chr.teachSkill(2120010, 0);
                chr.changeSkillLevel(2120010, (byte) 0, (byte) 0);
                chr.teachSkill(2120012, 0);
                chr.changeSkillLevel(2120012, (byte) 0, (byte) 0);
                chr.teachSkill(2120043, 0);
                chr.changeSkillLevel(2120043, (byte) 0, (byte) 0);
                chr.teachSkill(2120044, 0);
                chr.changeSkillLevel(2120044, (byte) 0, (byte) 0);
                chr.teachSkill(2120045, 0);
                chr.changeSkillLevel(2120045, (byte) 0, (byte) 0);
                chr.teachSkill(2120046, 0);
                chr.changeSkillLevel(2120046, (byte) 0, (byte) 0);
                chr.teachSkill(2120047, 0);
                chr.changeSkillLevel(2120047, (byte) 0, (byte) 0);
                chr.teachSkill(2120048, 0);
                chr.changeSkillLevel(2120048, (byte) 0, (byte) 0);
                chr.teachSkill(2120049, 0);
                chr.changeSkillLevel(2120049, (byte) 0, (byte) 0);
                chr.teachSkill(2120050, 0);
                chr.changeSkillLevel(2120050, (byte) 0, (byte) 0);
                chr.teachSkill(2120051, 0);
                chr.changeSkillLevel(2120051, (byte) 0, (byte) 0);
                chr.teachSkill(2121052, 0);
                chr.changeSkillLevel(2121052, (byte) 0, (byte) 0);
                chr.teachSkill(2121053, 0);
                chr.changeSkillLevel(2121053, (byte) 0, (byte) 0);
                chr.teachSkill(2121054, 0);
                chr.changeSkillLevel(2121054, (byte) 0, (byte) 0);

                chr.teachSkill(400021001, 0);
                chr.changeSkillLevel(400021001, (byte) 0, (byte) 0);
                chr.teachSkill(400021028, 0);
                chr.changeSkillLevel(400021028, (byte) 0, (byte) 0);
                chr.teachSkill(400021066, 0);
                chr.changeSkillLevel(400021066, (byte) 0, (byte) 0);
                //▶불독스킬레벨 0만들기종료
                //▶썬콜스킬레벨 0만들기시작
                chr.teachSkill(2201008, 0);
                chr.changeSkillLevel(2201008, (byte) 0, (byte) 0);

                chr.teachSkill(2200011, 0);
                chr.changeSkillLevel(2200011, (byte) 0, (byte) 0);

                chr.teachSkill(2201005, 0);
                chr.changeSkillLevel(2201005, (byte) 0, (byte) 0);

                chr.teachSkill(2201009, 0);
                chr.changeSkillLevel(2201009, (byte) 0, (byte) 0);

                chr.teachSkill(2201001, 0);
                chr.changeSkillLevel(2201001, (byte) 0, (byte) 0);

                chr.teachSkill(2201010, 0);
                chr.changeSkillLevel(2201010, (byte) 0, (byte) 0);

                chr.teachSkill(2200006, 0);
                chr.changeSkillLevel(2200006, (byte) 0, (byte) 0);

                chr.teachSkill(2200007, 0);
                chr.changeSkillLevel(2200007, (byte) 0, (byte) 0);

                chr.teachSkill(2200000, 0);
                chr.changeSkillLevel(2200000, (byte) 0, (byte) 0);

                chr.teachSkill(2211002, 0);
                chr.changeSkillLevel(2211002, (byte) 0, (byte) 0);

                chr.teachSkill(2211010, 0);
                chr.changeSkillLevel(2211010, (byte) 0, (byte) 0);

                chr.teachSkill(2211011, 0);
                chr.changeSkillLevel(2211011, (byte) 0, (byte) 0);

                chr.teachSkill(2211012, 0);
                chr.changeSkillLevel(2211012, (byte) 0, (byte) 0);

                chr.teachSkill(2211008, 0);
                chr.changeSkillLevel(2211008, (byte) 0, (byte) 0);

                chr.teachSkill(2211007, 0);
                chr.changeSkillLevel(2211007, (byte) 0, (byte) 0);

                chr.teachSkill(2210009, 0);
                chr.changeSkillLevel(2210009, (byte) 0, (byte) 0);

                chr.teachSkill(2210000, 0);
                chr.changeSkillLevel(2210000, (byte) 0, (byte) 0);

                chr.teachSkill(2210001, 0);
                chr.changeSkillLevel(2210001, (byte) 0, (byte) 0);

                chr.teachSkill(2221006, 0);
                chr.changeSkillLevel(2221006, (byte) 0, (byte) 0);

                chr.teachSkill(2221011, 0);
                chr.changeSkillLevel(2221011, (byte) 0, (byte) 0);

                chr.teachSkill(2221007, 0);
                chr.changeSkillLevel(2221007, (byte) 0, (byte) 0);

                chr.teachSkill(2221012, 0);
                chr.changeSkillLevel(2221012, (byte) 0, (byte) 0);

                chr.teachSkill(2221004, 0);
                chr.changeSkillLevel(2221004, (byte) 0, (byte) 0);

                chr.teachSkill(2221005, 0);
                chr.changeSkillLevel(2221005, (byte) 0, (byte) 0);

                chr.teachSkill(2221000, 0);
                chr.changeSkillLevel(2221000, (byte) 0, (byte) 0);

                chr.teachSkill(2221008, 0);
                chr.changeSkillLevel(2221008, (byte) 0, (byte) 0);

                chr.teachSkill(2220013, 0);
                chr.changeSkillLevel(2220013, (byte) 0, (byte) 0);

                chr.teachSkill(2220010, 0);
                chr.changeSkillLevel(2220010, (byte) 0, (byte) 0);

                chr.teachSkill(2220015, 0);
                chr.changeSkillLevel(2220015, (byte) 0, (byte) 0);

                chr.teachSkill(2220013, 0);
                chr.changeSkillLevel(2220013, (byte) 0, (byte) 0);

                chr.teachSkill(2221045, 0);
                chr.changeSkillLevel(2221045, (byte) 0, (byte) 0);

                chr.teachSkill(2220043, 0);
                chr.changeSkillLevel(2220043, (byte) 0, (byte) 0);

                chr.teachSkill(2220044, 0);
                chr.changeSkillLevel(2220044, (byte) 0, (byte) 0);

                chr.teachSkill(2220048, 0);
                chr.changeSkillLevel(2220048, (byte) 0, (byte) 0);

                chr.teachSkill(2220047, 0);
                chr.changeSkillLevel(2220047, (byte) 0, (byte) 0);

                chr.teachSkill(2220046, 0);
                chr.changeSkillLevel(2220046, (byte) 0, (byte) 0);

                chr.teachSkill(2220049, 0);
                chr.changeSkillLevel(2220049, (byte) 0, (byte) 0);

                chr.teachSkill(2220050, 0);
                chr.changeSkillLevel(2220050, (byte) 0, (byte) 0);

                chr.teachSkill(2220051, 0);
                chr.changeSkillLevel(2220051, (byte) 0, (byte) 0);

                chr.teachSkill(2221052, 0);
                chr.changeSkillLevel(2221052, (byte) 0, (byte) 0);

                chr.teachSkill(2221053, 0);
                chr.changeSkillLevel(2221053, (byte) 0, (byte) 0);

                chr.teachSkill(2221054, 0);
                chr.changeSkillLevel(2221054, (byte) 0, (byte) 0);

                chr.teachSkill(400020002, 0);
                chr.changeSkillLevel(400020002, (byte) 0, (byte) 0);
                chr.teachSkill(400021030, 0);
                chr.changeSkillLevel(400021030, (byte) 0, (byte) 0);
                chr.teachSkill(400021067, 0);
                chr.changeSkillLevel(400021067, (byte) 0, (byte) 0);
                chr.teachSkill(400021002, 0);
                chr.changeSkillLevel(400021002, (byte) 0, (byte) 0);

                //▶썬콜스킬레벨 0만들기종료
                //▶비숍스킬레벨 0만들기시작
                chr.teachSkill(2301005, 0);
                chr.changeSkillLevel(2301005, (byte) 0, (byte) 0);

                chr.teachSkill(2300009, 0);
                chr.changeSkillLevel(2300009, (byte) 0, (byte) 0);

                chr.teachSkill(2301002, 0);
                chr.changeSkillLevel(2301002, (byte) 0, (byte) 0);

                chr.teachSkill(2301004, 0);
                chr.changeSkillLevel(2301004, (byte) 0, (byte) 0);

                chr.teachSkill(2301008, 0);
                chr.changeSkillLevel(2301008, (byte) 0, (byte) 0);

                chr.teachSkill(2300003, 0);
                chr.changeSkillLevel(2300003, (byte) 0, (byte) 0);

                chr.teachSkill(2300006, 0);
                chr.changeSkillLevel(2300006, (byte) 0, (byte) 0);

                chr.teachSkill(2300007, 0);
                chr.changeSkillLevel(2300007, (byte) 0, (byte) 0);

                chr.teachSkill(2300000, 0);
                chr.changeSkillLevel(2300000, (byte) 0, (byte) 0);

                chr.teachSkill(2311004, 0);
                chr.changeSkillLevel(2311004, (byte) 0, (byte) 0);

                chr.teachSkill(2311011, 0);
                chr.changeSkillLevel(2311011, (byte) 0, (byte) 0);

                chr.teachSkill(2311012, 0);
                chr.changeSkillLevel(2311012, (byte) 0, (byte) 0);

                chr.teachSkill(2311002, 0);
                chr.changeSkillLevel(2311002, (byte) 0, (byte) 0);

                chr.teachSkill(2311001, 0);
                chr.changeSkillLevel(2311001, (byte) 0, (byte) 0);

                chr.teachSkill(2311003, 0);
                chr.changeSkillLevel(2311003, (byte) 0, (byte) 0);

                chr.teachSkill(2311007, 0);
                chr.changeSkillLevel(2311007, (byte) 0, (byte) 0);

                chr.teachSkill(2311009, 0);
                chr.changeSkillLevel(2311009, (byte) 0, (byte) 0);

                chr.teachSkill(2310010, 0);
                chr.changeSkillLevel(2310010, (byte) 0, (byte) 0);

                chr.teachSkill(2310008, 0);
                chr.changeSkillLevel(2310008, (byte) 0, (byte) 0);

                chr.teachSkill(2321007, 0);
                chr.changeSkillLevel(2321007, (byte) 0, (byte) 0);

                chr.teachSkill(2321008, 0);
                chr.changeSkillLevel(2321008, (byte) 0, (byte) 0);

                chr.teachSkill(2321001, 0);
                chr.changeSkillLevel(2321001, (byte) 0, (byte) 0);

                chr.teachSkill(2320013, 0);
                chr.changeSkillLevel(2320013, (byte) 0, (byte) 0);

                chr.teachSkill(2321006, 0);
                chr.changeSkillLevel(2321006, (byte) 0, (byte) 0);

                chr.teachSkill(2321004, 0);
                chr.changeSkillLevel(2321004, (byte) 0, (byte) 0);

                chr.teachSkill(2321003, 0);
                chr.changeSkillLevel(2321003, (byte) 0, (byte) 0);

                chr.teachSkill(2321005, 0);
                chr.changeSkillLevel(2321005, (byte) 0, (byte) 0);

                chr.teachSkill(2321000, 0);
                chr.changeSkillLevel(2321000, (byte) 0, (byte) 0);

                chr.teachSkill(2321009, 0);
                chr.changeSkillLevel(2321009, (byte) 0, (byte) 0);

                chr.teachSkill(2320012, 0);
                chr.changeSkillLevel(2320012, (byte) 0, (byte) 0);

                chr.teachSkill(2320011, 0);
                chr.changeSkillLevel(2320011, (byte) 0, (byte) 0);

                chr.teachSkill(2320044, 0);
                chr.changeSkillLevel(2320044, (byte) 0, (byte) 0);

                chr.teachSkill(2320043, 0);
                chr.changeSkillLevel(2320043, (byte) 0, (byte) 0);

                chr.teachSkill(2320045, 0);
                chr.changeSkillLevel(2320045, (byte) 0, (byte) 0);

                chr.teachSkill(2320047, 0);
                chr.changeSkillLevel(2320047, (byte) 0, (byte) 0);

                chr.teachSkill(2320046, 0);
                chr.changeSkillLevel(2320046, (byte) 0, (byte) 0);

                chr.teachSkill(2320048, 0);
                chr.changeSkillLevel(2320048, (byte) 0, (byte) 0);

                chr.teachSkill(2320049, 0);
                chr.changeSkillLevel(2320049, (byte) 0, (byte) 0);

                chr.teachSkill(2320050, 0);
                chr.changeSkillLevel(2320050, (byte) 0, (byte) 0);

                chr.teachSkill(2320051, 0);
                chr.changeSkillLevel(2320051, (byte) 0, (byte) 0);

                chr.teachSkill(2321052, 0);
                chr.changeSkillLevel(2321052, (byte) 0, (byte) 0);

                chr.teachSkill(2321053, 0);
                chr.changeSkillLevel(2321053, (byte) 0, (byte) 0);

                chr.teachSkill(2321054, 0);
                chr.changeSkillLevel(2321054, (byte) 0, (byte) 0);

                chr.teachSkill(400021003, 0);
                chr.changeSkillLevel(400021003, (byte) 0, (byte) 0);
                chr.teachSkill(400021032, 0);
                chr.changeSkillLevel(400021032, (byte) 0, (byte) 0);
                chr.teachSkill(400021070, 0);
                chr.changeSkillLevel(400021070, (byte) 0, (byte) 0);
                //▶비숍스킬레벨 0만들기종료
                break;
            case 312:
            case 322:
                //▶보마스킬레벨 0만들기종료
                chr.teachSkill(3001004, 0);
                chr.changeSkillLevel(3001004, (byte) 0, (byte) 0);
                chr.teachSkill(3001007, 0);
                chr.changeSkillLevel(3001007, (byte) 0, (byte) 0);
                chr.teachSkill(3000001, 0);
                chr.changeSkillLevel(3000001, (byte) 0, (byte) 0);
                chr.teachSkill(3000002, 0);
                chr.changeSkillLevel(3000002, (byte) 0, (byte) 0);
                chr.teachSkill(3101005, 0);
                chr.changeSkillLevel(3101005, (byte) 0, (byte) 0);
                chr.teachSkill(3101008, 0);
                chr.changeSkillLevel(3101008, (byte) 0, (byte) 0);
                chr.teachSkill(3101002, 0);
                chr.changeSkillLevel(3101002, (byte) 0, (byte) 0);
                chr.teachSkill(3101004, 0);
                chr.changeSkillLevel(3101004, (byte) 0, (byte) 0);
                chr.teachSkill(3101009, 0);
                chr.changeSkillLevel(3101009, (byte) 0, (byte) 0);
                chr.teachSkill(3100000, (byte) 0, (byte) 0);
                chr.changeSkillLevel(3100000, (byte) 0, (byte) 0);
                chr.teachSkill(3100001, 0);
                chr.changeSkillLevel(3100001, (byte) 0, (byte) 0);
                chr.teachSkill(3100006, 0);
                chr.changeSkillLevel(3100006, (byte) 0, (byte) 0);
                chr.teachSkill(3111013, 0);
                chr.changeSkillLevel(3111013, (byte) 0, (byte) 0);
                chr.teachSkill(3111003, 0);
                chr.changeSkillLevel(3111003, (byte) 0, (byte) 0);
                chr.teachSkill(3111005, 0);
                chr.changeSkillLevel(3111005, (byte) 0, (byte) 0);
                chr.teachSkill(3111010, (byte) 0, (byte) 0);
                chr.changeSkillLevel(3111010, (byte) 0, (byte) 0);
                chr.teachSkill(3111011, 0);
                chr.changeSkillLevel(3111011, (byte) 0, (byte) 0);
                chr.teachSkill(3110001, 0);
                chr.changeSkillLevel(3110001, (byte) 0, (byte) 0);
                chr.teachSkill(3110012, 0);
                chr.changeSkillLevel(3110012, (byte) 0, (byte) 0);
                chr.teachSkill(3110007, 0);
                chr.changeSkillLevel(3110007, (byte) 0, (byte) 0);
                chr.teachSkill(3110014, 0);
                chr.changeSkillLevel(3110014, (byte) 0, (byte) 0);
                chr.teachSkill(3121020, (byte) 0, (byte) 0);
                chr.changeSkillLevel(3121020, (byte) 0, (byte) 0);
                chr.teachSkill(3121014, 0);
                chr.changeSkillLevel(3121014, (byte) 0, (byte) 0);
                chr.teachSkill(3121015, 0);
                chr.changeSkillLevel(3121015, (byte) 0, (byte) 0);
                chr.teachSkill(3121002, 0);
                chr.changeSkillLevel(3121002, (byte) 0, (byte) 0);
                chr.teachSkill(3121007, 0);
                chr.changeSkillLevel(3121007, (byte) 0, (byte) 0);
                chr.teachSkill(3121009, 0);
                chr.changeSkillLevel(3121009, (byte) 0, (byte) 0);
                chr.teachSkill(3121000, (byte) 0, (byte) 0);
                chr.changeSkillLevel(3121000, (byte) 0, (byte) 0);
                chr.teachSkill(3120008, 0);
                chr.changeSkillLevel(3120008, (byte) 0, (byte) 0);
                chr.teachSkill(3121016, 0);
                chr.changeSkillLevel(3121016, (byte) 0, (byte) 0);
                chr.teachSkill(3120018, 0);
                chr.changeSkillLevel(3120018, (byte) 0, (byte) 0);
                chr.teachSkill(3120043, 0);
                chr.changeSkillLevel(3120043, (byte) 0, (byte) 0);
                chr.teachSkill(3120044, 0);
                chr.changeSkillLevel(3120044, (byte) 0, (byte) 0);
                chr.teachSkill(3120045, 0);
                chr.changeSkillLevel(3120045, (byte) 0, (byte) 0);
                chr.teachSkill(3120046, 0);
                chr.changeSkillLevel(3120046, (byte) 0, (byte) 0);
                chr.teachSkill(3120047, 0);
                chr.changeSkillLevel(3120047, (byte) 0, (byte) 0);
                chr.teachSkill(3120048, 0);
                chr.changeSkillLevel(3120048, (byte) 0, (byte) 0);
                chr.teachSkill(3120049, 0);
                chr.changeSkillLevel(3120049, (byte) 0, (byte) 0);
                chr.teachSkill(3120050, (byte) 0, (byte) 0);
                chr.changeSkillLevel(3120050, (byte) 0, (byte) 0);
                chr.teachSkill(3120051, 0);
                chr.changeSkillLevel(3120051, (byte) 0, (byte) 0);
                chr.teachSkill(3121052, 0);
                chr.changeSkillLevel(3121052, (byte) 0, (byte) 0);
                chr.teachSkill(3120005, 0);
                chr.changeSkillLevel(3120005, (byte) 0, (byte) 0);
                chr.teachSkill(3121053, 0);
                chr.changeSkillLevel(3121053, (byte) 0, (byte) 0);
                chr.teachSkill(3121054, 0);
                chr.changeSkillLevel(3121054, (byte) 0, (byte) 0);
                chr.teachSkill(400031002, 0);
                chr.changeSkillLevel(400031002, (byte) 0, (byte) 0);
                chr.teachSkill(400030002, 0);
                chr.changeSkillLevel(400030002, (byte) 0, (byte) 0);
                chr.teachSkill(400031002, (byte) 0, (byte) 0);
                chr.changeSkillLevel(400031002, (byte) 0, (byte) 0);
                chr.teachSkill(400031020, (byte) 0, (byte) 0);
                chr.changeSkillLevel(400031020, (byte) 0, (byte) 0);
                chr.teachSkill(400031028, (byte) 0, (byte) 0);
                chr.changeSkillLevel(400031028, (byte) 0, (byte) 0);
                //▶보마스킬레벨 0만들기종료
                //▶신궁스킬레벨 0만들기시작
                chr.teachSkill(3001004, 0);
                chr.changeSkillLevel(3001004, (byte) 0, (byte) 0);
                chr.teachSkill(3001007, 0);
                chr.changeSkillLevel(3001007, (byte) 0, (byte) 0);
                chr.teachSkill(3000001, 0);
                chr.changeSkillLevel(3000001, (byte) 0, (byte) 0);
                chr.teachSkill(3000002, 0);
                chr.changeSkillLevel(3000002, (byte) 0, (byte) 0);
                chr.teachSkill(3201005, 0);
                chr.changeSkillLevel(3201005, (byte) 0, (byte) 0);
                chr.teachSkill(3200009, 0);
                chr.changeSkillLevel(3200009, (byte) 0, (byte) 0);
                chr.teachSkill(3201008, 0);
                chr.changeSkillLevel(3201008, (byte) 0, (byte) 0);
                chr.teachSkill(3201002, 0);
                chr.changeSkillLevel(3201002, (byte) 0, (byte) 0);
                chr.teachSkill(3201004, 0);
                chr.changeSkillLevel(3201004, (byte) 0, (byte) 0);
                chr.teachSkill(320000, 0, (byte) 0);
                chr.changeSkillLevel(320000, (byte) 0, (byte) 0);
                chr.teachSkill(3200001, 0);
                chr.changeSkillLevel(3200001, (byte) 0, (byte) 0);
                chr.teachSkill(3200006, 0);
                chr.changeSkillLevel(3200006, (byte) 0, (byte) 0);
                chr.teachSkill(3211009, 0);
                chr.changeSkillLevel(3211009, (byte) 0, (byte) 0);
                chr.teachSkill(3211008, 0);
                chr.changeSkillLevel(3211008, (byte) 0, (byte) 0);
                chr.teachSkill(3211005, 0);
                chr.changeSkillLevel(3211005, (byte) 0, (byte) 0);
                chr.teachSkill(321101, 0, (byte) 0);
                chr.changeSkillLevel(321101, (byte) 0, (byte) 0);
                chr.teachSkill(3211011, 0);
                chr.changeSkillLevel(3211011, (byte) 0, (byte) 0);
                chr.teachSkill(3211012, 0);
                chr.changeSkillLevel(3211012, (byte) 0, (byte) 0);
                chr.teachSkill(3210001, 0);
                chr.changeSkillLevel(3210001, (byte) 0, (byte) 0);
                chr.teachSkill(3210013, 0);
                chr.changeSkillLevel(3210013, (byte) 0, (byte) 0);
                chr.teachSkill(3210007, 0);
                chr.changeSkillLevel(3210007, (byte) 0, (byte) 0);
                chr.teachSkill(3210015, 0);
                chr.changeSkillLevel(3210015, (byte) 0, (byte) 0);
                chr.teachSkill(3221017, 0);
                chr.changeSkillLevel(3221017, (byte) 0, (byte) 0);
                chr.teachSkill(3221007, 0);
                chr.changeSkillLevel(3221007, (byte) 0, (byte) 0);
                chr.teachSkill(3221014, 0);
                chr.changeSkillLevel(3221014, (byte) 0, (byte) 0);
                chr.teachSkill(3221002, 0);
                chr.changeSkillLevel(3221002, (byte) 0, (byte) 0);
                chr.teachSkill(3221006, 0);
                chr.changeSkillLevel(3221006, (byte) 0, (byte) 0);
                chr.teachSkill(3221008, 0);
                chr.changeSkillLevel(3221008, (byte) 0, (byte) 0);
                chr.teachSkill(3221000, 0);
                chr.changeSkillLevel(3221000, (byte) 0, (byte) 0);
                chr.teachSkill(3220004, 0);
                chr.changeSkillLevel(3220004, (byte) 0, (byte) 0);
                chr.teachSkill(3220015, 0);
                chr.changeSkillLevel(3220015, (byte) 0, (byte) 0);
                chr.teachSkill(3220016, 0);
                chr.changeSkillLevel(3220016, (byte) 0, (byte) 0);
                chr.teachSkill(3220043, 0);
                chr.changeSkillLevel(3220043, (byte) 0, (byte) 0);
                chr.teachSkill(3220044, 0);
                chr.changeSkillLevel(3220044, (byte) 0, (byte) 0);
                chr.teachSkill(3220045, 0);
                chr.changeSkillLevel(3220045, (byte) 0, (byte) 0);
                chr.teachSkill(3220046, 0);
                chr.changeSkillLevel(3220046, (byte) 0, (byte) 0);
                chr.teachSkill(3220047, 0);
                chr.changeSkillLevel(3220047, (byte) 0, (byte) 0);
                chr.teachSkill(3220048, 0);
                chr.changeSkillLevel(3220048, (byte) 0, (byte) 0);
                chr.teachSkill(3220018, 0);
                chr.changeSkillLevel(3220018, (byte) 0, (byte) 0);
                chr.teachSkill(3220049, 0);
                chr.changeSkillLevel(3220049, (byte) 0, (byte) 0);
                chr.teachSkill(3220050, 0);
                chr.changeSkillLevel(3220050, (byte) 0, (byte) 0);
                chr.teachSkill(3220051, 0);
                chr.changeSkillLevel(3220051, (byte) 0, (byte) 0);
                chr.teachSkill(3221052, 0);
                chr.changeSkillLevel(3221052, (byte) 0, (byte) 0);
                chr.teachSkill(3221053, 0);
                chr.changeSkillLevel(3221053, (byte) 0, (byte) 0);
                chr.teachSkill(3221054, 0);
                chr.changeSkillLevel(3221054, (byte) 0, (byte) 0);
                chr.teachSkill(400031006, 0);
                chr.changeSkillLevel(400031006, (byte) 0, (byte) 0);
                chr.teachSkill(400031015, 0);
                chr.changeSkillLevel(400031015, (byte) 0, (byte) 0);
                chr.teachSkill(400031025, 0);
                chr.changeSkillLevel(400031025, (byte) 0, (byte) 0);
                //▶신궁스킬레벨 0만들기종료
                break;
            case 412:
            case 422:
            case 434:
                chr.teachSkill(4331000, 0);
                chr.changeSkillLevel(4331000, (byte) 0, (byte) 0);
                chr.teachSkill(4001334, 0);
                chr.changeSkillLevel(4001334, (byte) 0, (byte) 0);
                chr.teachSkill(4001344, 0);
                chr.changeSkillLevel(4001344, (byte) 0, (byte) 0);
                chr.teachSkill(4001005, 0);
                chr.changeSkillLevel(4001005, (byte) 0, (byte) 0);
                chr.teachSkill(4001003, 0);
                chr.changeSkillLevel(4001003, (byte) 0, (byte) 0);
                chr.teachSkill(4001014, 0);
                chr.changeSkillLevel(4001014, (byte) 0, (byte) 0);
                chr.teachSkill(4000000, 0);
                chr.changeSkillLevel(4000000, (byte) 0, (byte) 0);
                chr.teachSkill(4300000, 0);
                chr.changeSkillLevel(4300000, (byte) 0, (byte) 0);
                chr.teachSkill(4301003, 0);
                chr.changeSkillLevel(4301003, (byte) 0, (byte) 0);
                chr.teachSkill(4301004, 0);
                chr.changeSkillLevel(4301004, (byte) 0, (byte) 0);
                chr.teachSkill(4311002, 0);
                chr.changeSkillLevel(4311002, (byte) 0, (byte) 0);
                chr.teachSkill(4311003, 0);
                chr.changeSkillLevel(4311003, (byte) 0, (byte) 0);
                chr.teachSkill(4311009, 0);
                chr.changeSkillLevel(4311009, (byte) 0, (byte) 0);
                chr.teachSkill(4310005, 0);
                chr.changeSkillLevel(4310005, (byte) 0, (byte) 0);
                chr.teachSkill(4310006, 0);
                chr.changeSkillLevel(4310006, (byte) 0, (byte) 0);
                chr.teachSkill(4321006, 0);
                chr.changeSkillLevel(4321006, (byte) 0, (byte) 0);
                chr.teachSkill(4321004, 0);
                chr.changeSkillLevel(4321004, (byte) 0, (byte) 0);
                chr.teachSkill(4321002, 0);
                chr.changeSkillLevel(4321002, (byte) 0, (byte) 0);
                chr.teachSkill(4320005, 0);
                chr.changeSkillLevel(4320005, (byte) 0, (byte) 0);
                chr.teachSkill(4320005, 0);
                chr.changeSkillLevel(4320005, (byte) 0, (byte) 0);
                chr.teachSkill(4331011, 0);
                chr.changeSkillLevel(4331011, (byte) 0, (byte) 0);
                chr.teachSkill(4331006, 0);
                chr.changeSkillLevel(4331006, (byte) 0, (byte) 0);
                chr.teachSkill(4331002, 0);
                chr.changeSkillLevel(4331002, (byte) 0, (byte) 0);
                chr.teachSkill(4330001, 0);
                chr.changeSkillLevel(4330001, (byte) 0, (byte) 0);
                chr.teachSkill(4330007, 0);
                chr.changeSkillLevel(4330007, (byte) 0, (byte) 0);
                chr.teachSkill(4330008, 0);
                chr.changeSkillLevel(4330008, (byte) 0, (byte) 0);
                chr.teachSkill(4330009, 0);
                chr.changeSkillLevel(4330009, (byte) 0, (byte) 0);
                chr.teachSkill(4341004, 0);
                chr.changeSkillLevel(4341004, (byte) 0, (byte) 0);
                chr.teachSkill(4341009, 0);
                chr.changeSkillLevel(4341009, (byte) 0, (byte) 0);
                chr.teachSkill(4341002, 0);
                chr.changeSkillLevel(4341002, (byte) 0, (byte) 0);
                chr.teachSkill(4341011, 0);
                chr.changeSkillLevel(4341011, (byte) 0, (byte) 0);
                chr.teachSkill(4341000, 0);
                chr.changeSkillLevel(4341000, (byte) 0, (byte) 0);
                chr.teachSkill(4341008, 0);
                chr.changeSkillLevel(4341008, (byte) 0, (byte) 0);
                chr.teachSkill(4341006, 0);
                chr.changeSkillLevel(4341006, (byte) 0, (byte) 0);
                chr.teachSkill(4340007, 0);
                chr.changeSkillLevel(4340007, (byte) 0, (byte) 0);
                chr.teachSkill(4340010, 0);
                chr.changeSkillLevel(4340010, (byte) 0, (byte) 0);
                chr.teachSkill(4340012, 0);
                chr.changeSkillLevel(4340012, (byte) 0, (byte) 0);
                chr.teachSkill(4340013, 0);
                chr.changeSkillLevel(4340013, (byte) 0, (byte) 0);
                chr.teachSkill(4341052, 0);
                chr.changeSkillLevel(4341052, (byte) 0, (byte) 0);
                chr.teachSkill(4341053, 0);
                chr.changeSkillLevel(4341053, (byte) 0, (byte) 0);
                chr.teachSkill(4341054, 0);
                chr.changeSkillLevel(4341054, (byte) 0, (byte) 0);
                chr.teachSkill(400041006, 0);
                chr.changeSkillLevel(400041006, (byte) 0, (byte) 0);
                chr.teachSkill(400041021, 0);
                chr.changeSkillLevel(400041021, (byte) 0, (byte) 0);
                chr.teachSkill(400041042, 0);
                chr.changeSkillLevel(400041042, (byte) 0, (byte) 0);
                //▶듀블스킬레벨 0만들기종료

                //▶나로스킬레벨 0만들기시작
                chr.teachSkill(4001334, 0);
                chr.changeSkillLevel(4001334, (byte) 0, (byte) 0);
                chr.teachSkill(4001344, 0);
                chr.changeSkillLevel(4001344, (byte) 0, (byte) 0);
                chr.teachSkill(4001005, 0);
                chr.changeSkillLevel(4001005, (byte) 0, (byte) 0);
                chr.teachSkill(4001003, 0);
                chr.changeSkillLevel(4001003, (byte) 0, (byte) 0);
                chr.teachSkill(4001014, 0);
                chr.changeSkillLevel(4001014, (byte) 0, (byte) 0);
                chr.teachSkill(4000000, 0);
                chr.changeSkillLevel(4000000, (byte) 0, (byte) 0);
                chr.teachSkill(4101008, 0);
                chr.changeSkillLevel(4101008, (byte) 0, (byte) 0);
                chr.teachSkill(4101011, 0);
                chr.changeSkillLevel(4101011, (byte) 0, (byte) 0);
                chr.teachSkill(4100012, 0);
                chr.changeSkillLevel(4100012, (byte) 0, (byte) 0);
                chr.teachSkill(4100011, 0);
                chr.changeSkillLevel(4100011, (byte) 0, (byte) 0);
                chr.teachSkill(4101010, 0);
                chr.changeSkillLevel(4100011, (byte) 0, (byte) 0);
                chr.teachSkill(4101003, 0);
                chr.changeSkillLevel(4101003, (byte) 0, (byte) 0);
                chr.teachSkill(4100000, 0);
                chr.changeSkillLevel(4100000, (byte) 0, (byte) 0);
                chr.teachSkill(4100001, 0);
                chr.changeSkillLevel(4100001, (byte) 0, (byte) 0);
                chr.teachSkill(4100007, 0);
                chr.changeSkillLevel(4100007, (byte) 0, (byte) 0);
                chr.teachSkill(4111010, 0);
                chr.changeSkillLevel(4111010, (byte) 0, (byte) 0);
                chr.teachSkill(4111015, 0);
                chr.changeSkillLevel(4111015, (byte) 0, (byte) 0);
                chr.teachSkill(4111007, 0);
                chr.changeSkillLevel(4111007, (byte) 0, (byte) 0);
                chr.teachSkill(4111003, 0);
                chr.changeSkillLevel(4111003, (byte) 0, (byte) 0);
                chr.teachSkill(4111002, 0);
                chr.changeSkillLevel(4111002, (byte) 0, (byte) 0);
                chr.teachSkill(4111009, 0);
                chr.changeSkillLevel(4111009, (byte) 0, (byte) 0);
                chr.teachSkill(4110008, 0);
                chr.changeSkillLevel(4110008, (byte) 0, (byte) 0);
                chr.teachSkill(4110011, 0);
                chr.changeSkillLevel(4110011, (byte) 0, (byte) 0);
                chr.teachSkill(4110012, 0);
                chr.changeSkillLevel(4110012, (byte) 0, (byte) 0);
                chr.teachSkill(4110014, 0);
                chr.changeSkillLevel(4110014, (byte) 0, (byte) 0);
                chr.teachSkill(4121013, 0);
                chr.changeSkillLevel(4121013, (byte) 0, (byte) 0);
                chr.teachSkill(4121017, 0);
                chr.changeSkillLevel(4121017, (byte) 0, (byte) 0);
                chr.teachSkill(4120018, 0);
                chr.changeSkillLevel(4120018, (byte) 0, (byte) 0);
                chr.teachSkill(4120019, 0);
                chr.changeSkillLevel(4120019, (byte) 0, (byte) 0);
                chr.teachSkill(4121016, 0);
                chr.changeSkillLevel(4121016, (byte) 0, (byte) 0);
                chr.teachSkill(4121015, 0);
                chr.changeSkillLevel(4121015, (byte) 0, (byte) 0);
                chr.teachSkill(4121000, 0);
                chr.changeSkillLevel(4121000, (byte) 0, (byte) 0);
                chr.teachSkill(4121009, 0);
                chr.changeSkillLevel(4121009, (byte) 0, (byte) 0);
                chr.teachSkill(4120014, 0);
                chr.changeSkillLevel(4120014, (byte) 0, (byte) 0);
                chr.teachSkill(4120002, 0);
                chr.changeSkillLevel(4120002, (byte) 0, (byte) 0);
                chr.teachSkill(4120011, 0);
                chr.changeSkillLevel(4120011, (byte) 0, (byte) 0);
                chr.teachSkill(4120012, 0);
                chr.changeSkillLevel(4120012, (byte) 0, (byte) 0);
                chr.teachSkill(4120043, 0);
                chr.changeSkillLevel(4120043, (byte) 0, (byte) 0);
                chr.teachSkill(4120044, 0);
                chr.changeSkillLevel(4120044, (byte) 0, (byte) 0);
                chr.teachSkill(4120045, 0);
                chr.changeSkillLevel(4120045, (byte) 0, (byte) 0);
                chr.teachSkill(4120048, 0);
                chr.changeSkillLevel(4120048, (byte) 0, (byte) 0);
                chr.teachSkill(4120046, 0);
                chr.changeSkillLevel(4120046, (byte) 0, (byte) 0);
                chr.teachSkill(4120047, 0);
                chr.changeSkillLevel(4120047, (byte) 0, (byte) 0);
                chr.teachSkill(4120051, 0);
                chr.changeSkillLevel(4120051, (byte) 0, (byte) 0);
                chr.teachSkill(4120050, 0);
                chr.changeSkillLevel(4120050, (byte) 0, (byte) 0);
                chr.teachSkill(4120047, 0);
                chr.changeSkillLevel(4120047, (byte) 0, (byte) 0);
                chr.teachSkill(4120051, 0);
                chr.changeSkillLevel(4120051, (byte) 0, (byte) 0);
                chr.teachSkill(4120050, 0);
                chr.changeSkillLevel(4120050, (byte) 0, (byte) 0);
                chr.teachSkill(4120049, 0);
                chr.changeSkillLevel(4120049, (byte) 0, (byte) 0);
                chr.teachSkill(4121052, 0);
                chr.changeSkillLevel(4121052, (byte) 0, (byte) 0);
                chr.teachSkill(4121053, 0);
                chr.changeSkillLevel(4121053, (byte) 0, (byte) 0);
                chr.teachSkill(4121054, 0);
                chr.changeSkillLevel(4121054, (byte) 0, (byte) 0);
                chr.teachSkill(400041001, 0);
                chr.changeSkillLevel(400041001, (byte) 0, (byte) 0);
                chr.teachSkill(400041020, 0);
                chr.changeSkillLevel(400041020, (byte) 0, (byte) 0);
                chr.teachSkill(400041038, 0);
                chr.changeSkillLevel(400041038, (byte) 0, (byte) 0);
                //▶나로스킬레벨 0만들기종료

                //▶섀도어스킬레벨 0만들기시작
                chr.teachSkill(4221014, 0);
                chr.changeSkillLevel(4221014, (byte) 0, (byte) 0);
                chr.teachSkill(4001334, 0);
                chr.changeSkillLevel(4001334, (byte) 0, (byte) 0);
                chr.teachSkill(4001344, 0);
                chr.changeSkillLevel(4001344, (byte) 0, (byte) 0);
                chr.teachSkill(4001005, 0);
                chr.changeSkillLevel(4001005, (byte) 0, (byte) 0);
                chr.teachSkill(4001003, 0);
                chr.changeSkillLevel(4001003, (byte) 0, (byte) 0);
                chr.teachSkill(4001014, 0);
                chr.changeSkillLevel(4001014, (byte) 0, (byte) 0);
                chr.teachSkill(4000000, 0);
                chr.changeSkillLevel(4000000, (byte) 0, (byte) 0);
                chr.teachSkill(4201012, 0);
                chr.changeSkillLevel(4201012, (byte) 0, (byte) 0);
                chr.teachSkill(4200013, 0);
                chr.changeSkillLevel(4200013, (byte) 0, (byte) 0);
                chr.teachSkill(4201004, 0);
                chr.changeSkillLevel(4201004, (byte) 0, (byte) 0);
                chr.teachSkill(4201002, 0);
                chr.changeSkillLevel(4201002, (byte) 0, (byte) 0);
                chr.teachSkill(4201011, 0);
                chr.changeSkillLevel(4201011, (byte) 0, (byte) 0);
                chr.teachSkill(4200009, 0);
                chr.changeSkillLevel(4200009, (byte) 0, (byte) 0);
                chr.teachSkill(4200000, 0);
                chr.changeSkillLevel(4200000, (byte) 0, (byte) 0);
                chr.teachSkill(4200007, 0);
                chr.changeSkillLevel(4200007, (byte) 0, (byte) 0);
                chr.teachSkill(4200010, 0);
                chr.changeSkillLevel(4200010, (byte) 0, (byte) 0);
                chr.teachSkill(4201012, 0);
                chr.changeSkillLevel(4201012, (byte) 0, (byte) 0);
                chr.teachSkill(4211011, 0);
                chr.changeSkillLevel(4211011, (byte) 0, (byte) 0);
                chr.teachSkill(4211002, 0);
                chr.changeSkillLevel(4211002, (byte) 0, (byte) 0);
                chr.teachSkill(4211006, 0);
                chr.changeSkillLevel(4211006, (byte) 0, (byte) 0);
                chr.teachSkill(4211007, 0);
                chr.changeSkillLevel(4211007, (byte) 0, (byte) 0);
                chr.teachSkill(4211003, 0);
                chr.changeSkillLevel(4211003, (byte) 0, (byte) 0);
                chr.teachSkill(4211008, 0);
                chr.changeSkillLevel(4211008, (byte) 0, (byte) 0);
                chr.teachSkill(4210015, 0);
                chr.changeSkillLevel(4210015, (byte) 0, (byte) 0);
                chr.teachSkill(4211016, 0);
                chr.changeSkillLevel(4211016, (byte) 0, (byte) 0);
                chr.teachSkill(4210010, 0);
                chr.changeSkillLevel(4210010, (byte) 0, (byte) 0);
                chr.teachSkill(4210012, 0);
                chr.changeSkillLevel(4210012, (byte) 0, (byte) 0);
                chr.teachSkill(4210013, 0);
                chr.changeSkillLevel(4210013, (byte) 0, (byte) 0);
                chr.teachSkill(4210014, 0);
                chr.changeSkillLevel(4210014, (byte) 0, (byte) 0);
                chr.teachSkill(4221007, 0);
                chr.changeSkillLevel(4221007, (byte) 0, (byte) 0);
                chr.teachSkill(4220015, 0);
                chr.changeSkillLevel(4220015, (byte) 0, (byte) 0);
                chr.teachSkill(4221010, 0);
                chr.changeSkillLevel(4221010, (byte) 0, (byte) 0);
                chr.teachSkill(4221000, 0);
                chr.changeSkillLevel(4221000, (byte) 0, (byte) 0);
                chr.teachSkill(4221006, 0);
                chr.changeSkillLevel(4221006, (byte) 0, (byte) 0);
                chr.teachSkill(4221008, 0);
                chr.changeSkillLevel(4221008, (byte) 0, (byte) 0);
                chr.teachSkill(4221013, 0);
                chr.changeSkillLevel(4221013, (byte) 0, (byte) 0);
                chr.teachSkill(4220002, 0);
                chr.changeSkillLevel(4220002, (byte) 0, (byte) 0);
                chr.teachSkill(4220011, 0);
                chr.changeSkillLevel(4220011, (byte) 0, (byte) 0);
                chr.teachSkill(4220012, 0);
                chr.changeSkillLevel(4220012, (byte) 0, (byte) 0);
                chr.teachSkill(4221052, 0);
                chr.changeSkillLevel(4221052, (byte) 0, (byte) 0);
                chr.teachSkill(4221053, 0);
                chr.changeSkillLevel(4221053, (byte) 0, (byte) 0);
                chr.teachSkill(4221054, 0);
                chr.changeSkillLevel(4221054, (byte) 0, (byte) 0);
                chr.teachSkill(400041002, 0);
                chr.changeSkillLevel(400041002, (byte) 0, (byte) 0);
                chr.teachSkill(400041025, 0);
                chr.changeSkillLevel(400041025, (byte) 0, (byte) 0);
                chr.teachSkill(400041039, 0);
                chr.changeSkillLevel(400041039, (byte) 0, (byte) 0);
                //▶섀도어스킬레벨 0만들기종료
                break;
            case 512:
            case 522:
            case 532:

                break;
        }

        switch (AfterJob) {//스킬 채워넣기
            case 112:
                //▶히어로스킬레벨 max만들기시작
                chr.teachSkill(1001005, 30);
                chr.changeSkillLevel(1001005, (byte) 30, (byte) 30);
                chr.teachSkill(1001008, 30);
                chr.changeSkillLevel(1001008, (byte) 30, (byte) 30);
                chr.teachSkill(1000003, 30);
                chr.changeSkillLevel(1000003, (byte) 30, (byte) 30);
                chr.teachSkill(1000009, 30);
                chr.changeSkillLevel(1000009, (byte) 30, (byte) 30);
                chr.teachSkill(1101011, 30);
                chr.changeSkillLevel(1101011, (byte) 30, (byte) 30);
                chr.teachSkill(1101012, 30);
                chr.changeSkillLevel(1101012, (byte) 30, (byte) 30);
                chr.teachSkill(1101013, 30);
                chr.changeSkillLevel(1101013, (byte) 30, (byte) 30);
                chr.teachSkill(1101004, 30);
                chr.changeSkillLevel(1101004, (byte) 30, (byte) 30);
                chr.teachSkill(1101006, 30);
                chr.changeSkillLevel(1101006, (byte) 30, (byte) 30);
                chr.teachSkill(1100000, 30);
                chr.changeSkillLevel(11000030, (byte) 30, (byte) 30);
                chr.teachSkill(1100002, 30);
                chr.changeSkillLevel(1100002, (byte) 30, (byte) 30);
                chr.teachSkill(1100009, 30);
                chr.changeSkillLevel(1100009, (byte) 30, (byte) 30);
                chr.teachSkill(1111010, 30);
                chr.changeSkillLevel(11110130, (byte) 30, (byte) 30);
                chr.teachSkill(1111012, 30);
                chr.changeSkillLevel(1111012, (byte) 30, (byte) 30);
                chr.teachSkill(1111003, 30);
                chr.changeSkillLevel(1111003, (byte) 30, (byte) 30);
                chr.teachSkill(1111008, 30);
                chr.changeSkillLevel(1111008, (byte) 30, (byte) 30);
                chr.teachSkill(1110013, 30);
                chr.changeSkillLevel(1110013, (byte) 30, (byte) 30);
                chr.teachSkill(1110000, 30);
                chr.changeSkillLevel(11100030, (byte) 30, (byte) 0);
                chr.teachSkill(1110009, 30);
                chr.changeSkillLevel(1110009, (byte) 30, (byte) 30);
                chr.teachSkill(1110011, 30);
                chr.changeSkillLevel(1110011, (byte) 30, (byte) 30);
                chr.teachSkill(1121008, 30);
                chr.changeSkillLevel(1121008, (byte) 30, (byte) 30);
                chr.teachSkill(1121015, 30);
                chr.changeSkillLevel(1121015, (byte) 30, (byte) 30);
                chr.teachSkill(1121016, 30);
                chr.changeSkillLevel(1121016, (byte) 30, (byte) 30);
                chr.teachSkill(1120014, 30);
                chr.changeSkillLevel(1120014, (byte) 30, (byte) 30);
                chr.teachSkill(1121010, 30);
                chr.changeSkillLevel(11210130, (byte) 30, (byte) 30);
                chr.teachSkill(1121000, 30);
                chr.changeSkillLevel(11210030, (byte) 30, (byte) 30);
                chr.teachSkill(1121011, 30);
                chr.changeSkillLevel(1121011, (byte) 30, (byte) 30);
                chr.teachSkill(1120003, 30);
                chr.changeSkillLevel(1120003, (byte) 30, (byte) 30);
                chr.teachSkill(1120012, 30);
                chr.changeSkillLevel(1120012, (byte) 30, (byte) 30);
                chr.teachSkill(1120013, 30);
                chr.changeSkillLevel(1120013, (byte) 30, (byte) 30);
                chr.teachSkill(1120045, 30);
                chr.changeSkillLevel(1120045, (byte) 30, (byte) 30);
                chr.teachSkill(1120044, 30);
                chr.changeSkillLevel(1120044, (byte) 30, (byte) 30);
                chr.teachSkill(1120043, 30);
                chr.changeSkillLevel(1120043, (byte) 30, (byte) 30);
                chr.teachSkill(1120048, 30);
                chr.changeSkillLevel(1120048, (byte) 30, (byte) 30);
                chr.teachSkill(1120047, 30);
                chr.changeSkillLevel(1120047, (byte) 30, (byte) 30);
                chr.teachSkill(1120046, 30);
                chr.changeSkillLevel(1120046, (byte) 30, (byte) 30);
                chr.teachSkill(1120050, 30);
                chr.changeSkillLevel(11200530, (byte) 30, (byte) 30);
                chr.teachSkill(1120051, 30);
                chr.changeSkillLevel(1120051, (byte) 30, (byte) 30);
                chr.teachSkill(1120049, 30);
                chr.changeSkillLevel(1120049, (byte) 30, (byte) 30);
                chr.teachSkill(1121052, 30);
                chr.changeSkillLevel(1121052, (byte) 30, (byte) 30);
                chr.teachSkill(1121054, 30);
                chr.changeSkillLevel(1121054, (byte) 30, (byte) 30);
                chr.teachSkill(1121053, 30);
                chr.changeSkillLevel(1121053, (byte) 30, (byte) 30);

                //▶히어로스킬레벨 max만들기종료
                break;
            case 122:
                //▶팔라딘스킬레벨 max만들기시작
                chr.teachSkill(1001005, 30);
                chr.changeSkillLevel(1001005, (byte) 30, (byte) 30);
                chr.teachSkill(1001008, 30);
                chr.changeSkillLevel(1001008, (byte) 30, (byte) 30);
                chr.teachSkill(1000003, 30);
                chr.changeSkillLevel(1000003, (byte) 30, (byte) 30);
                chr.teachSkill(1000009, 30);
                chr.changeSkillLevel(1000009, (byte) 30, (byte) 30);
                chr.teachSkill(1201011, 30);
                chr.changeSkillLevel(1201011, (byte) 30, (byte) 30);
                chr.teachSkill(1201012, 30);
                chr.changeSkillLevel(1201012, (byte) 30, (byte) 30);
                chr.teachSkill(1200014, 30);
                chr.changeSkillLevel(1200014, (byte) 30, (byte) 30);
                chr.teachSkill(1201013, 30);
                chr.changeSkillLevel(1201013, (byte) 30, (byte) 30);
                chr.teachSkill(1201004, 30);
                chr.changeSkillLevel(1201004, (byte) 30, (byte) 30);
                chr.teachSkill(1200000, 30);
                chr.changeSkillLevel(1200000, (byte) 30, (byte) 30);
                chr.teachSkill(1200002, 30);
                chr.changeSkillLevel(1200002, (byte) 30, (byte) 30);
                chr.teachSkill(1200009, 30);
                chr.changeSkillLevel(1200009, (byte) 30, (byte) 30);
                chr.teachSkill(1211008, 30);
                chr.changeSkillLevel(1211008, (byte) 30, (byte) 30);
                chr.teachSkill(1211010, 30);
                chr.changeSkillLevel(1211010, (byte) 30, (byte) 30);
                chr.teachSkill(1211012, 30);
                chr.changeSkillLevel(1211012, (byte) 30, (byte) 30);
                chr.teachSkill(1211013, 30);
                chr.changeSkillLevel(1211013, (byte) 30, (byte) 30);
                chr.teachSkill(1211014, 30);
                chr.changeSkillLevel(1211014, (byte) 30, (byte) 30);
                chr.teachSkill(1211011, 30);
                chr.changeSkillLevel(1211011, (byte) 30, (byte) 30);
                chr.teachSkill(1210001, 30);
                chr.changeSkillLevel(1210001, (byte) 30, (byte) 30);
                chr.teachSkill(1210015, 30);
                chr.changeSkillLevel(1210015, (byte) 30, (byte) 30);
                chr.teachSkill(1210016, 30);
                chr.changeSkillLevel(1210016, (byte) 30, (byte) 30);
                chr.teachSkill(1221009, 30);
                chr.changeSkillLevel(1221009, (byte) 30, (byte) 30);
                chr.teachSkill(1221004, 30);
                chr.changeSkillLevel(1221004, (byte) 30, (byte) 30);
                chr.teachSkill(1221014, 30);
                chr.changeSkillLevel(1221014, (byte) 30, (byte) 30);
                chr.teachSkill(1221011, 30);
                chr.changeSkillLevel(1221011, (byte) 30, (byte) 30);
                chr.teachSkill(1221015, 30);
                chr.changeSkillLevel(1221015, (byte) 30, (byte) 30);
                chr.teachSkill(1220017, 30);
                chr.changeSkillLevel(1220017, (byte) 30, (byte) 30);
                chr.teachSkill(1221000, 30);
                chr.changeSkillLevel(1221000, (byte) 30, (byte) 30);
                chr.teachSkill(1221012, 30);
                chr.changeSkillLevel(1221012, (byte) 30, (byte) 30);
                chr.teachSkill(1221016, 30);
                chr.changeSkillLevel(1221016, (byte) 30, (byte) 30);
                chr.teachSkill(1220018, 30);
                chr.changeSkillLevel(1220018, (byte) 30, (byte) 30);
                chr.teachSkill(1220010, 30);
                chr.changeSkillLevel(1220010, (byte) 30, (byte) 30);
                chr.teachSkill(1220045, 30);
                chr.changeSkillLevel(1220045, (byte) 30, (byte) 30);
                chr.teachSkill(1220044, 30);
                chr.changeSkillLevel(1220044, (byte) 30, (byte) 30);
                chr.teachSkill(1220043, 30);
                chr.changeSkillLevel(1220043, (byte) 30, (byte) 30);
                chr.teachSkill(1220048, 30);
                chr.changeSkillLevel(1220048, (byte) 30, (byte) 30);
                chr.teachSkill(1220047, 30);
                chr.changeSkillLevel(1220047, (byte) 30, (byte) 30);
                chr.teachSkill(1220046, 30);
                chr.changeSkillLevel(1220046, (byte) 30, (byte) 30);
                chr.teachSkill(1220050, 30);
                chr.changeSkillLevel(1220050, (byte) 30, (byte) 30);
                chr.teachSkill(1220051, 30);
                chr.changeSkillLevel(1220051, (byte) 30, (byte) 30);
                chr.teachSkill(1220049, 30);
                chr.changeSkillLevel(1220049, (byte) 30, (byte) 30);
                chr.teachSkill(1221052, 30);
                chr.changeSkillLevel(1221052, (byte) 30, (byte) 30);
                chr.teachSkill(1221054, 30);
                chr.changeSkillLevel(1221054, (byte) 30, (byte) 30);
                chr.teachSkill(1221053, 30);
                chr.changeSkillLevel(1221053, (byte) 30, (byte) 30);
                //▶팔라딘스킬레벨 max만들기종료
                break;
            case 132:
                //▶다크나이트스킬레벨 max만들기시작
                chr.teachSkill(1001005, 30);
                chr.changeSkillLevel(1001005, (byte) 30, (byte) 30);
                chr.teachSkill(1001008, 30);
                chr.changeSkillLevel(1001008, (byte) 30, (byte) 30);
                chr.teachSkill(1000003, 30);
                chr.changeSkillLevel(1000003, (byte) 30, (byte) 30);
                chr.teachSkill(1000009, 30);
                chr.changeSkillLevel(1000009, (byte) 30, (byte) 30);
                chr.teachSkill(1301011, 30);
                chr.changeSkillLevel(1301011, (byte) 30, (byte) 30);
                chr.teachSkill(1301013, 30);
                chr.changeSkillLevel(1301013, (byte) 30, (byte) 30);
                chr.teachSkill(1301012, 30);
                chr.changeSkillLevel(1301012, (byte) 30, (byte) 30);
                chr.teachSkill(1301004, 30);
                chr.changeSkillLevel(1301004, (byte) 30, (byte) 30);
                chr.teachSkill(1301006, 30);
                chr.changeSkillLevel(1301006, (byte) 30, (byte) 30);
                chr.teachSkill(1301007, 30);
                chr.changeSkillLevel(1301007, (byte) 30, (byte) 30);
                chr.teachSkill(1300000, 30);
                chr.changeSkillLevel(1300000, (byte) 30, (byte) 30);
                chr.teachSkill(1300002, 30);
                chr.changeSkillLevel(1300002, (byte) 30, (byte) 30);
                chr.teachSkill(1300009, 30);
                chr.changeSkillLevel(1300009, (byte) 30, (byte) 30);
                chr.teachSkill(1311011, 30);
                chr.changeSkillLevel(1311011, (byte) 30, (byte) 30);
                chr.teachSkill(1311012, 30);
                chr.changeSkillLevel(1311012, (byte) 30, (byte) 30);
                chr.teachSkill(1311014, 30);
                chr.changeSkillLevel(1311014, (byte) 30, (byte) 30);
                chr.teachSkill(1311015, 30);
                chr.changeSkillLevel(1311015, (byte) 30, (byte) 30);
                chr.teachSkill(1310013, 30);
                chr.changeSkillLevel(1310013, (byte) 30, (byte) 30);
                chr.teachSkill(1310009, 30);
                chr.changeSkillLevel(1310009, (byte) 30, (byte) 30);
                chr.teachSkill(1310010, 30);
                chr.changeSkillLevel(1310010, (byte) 30, (byte) 30);
                chr.teachSkill(1310016, 30);
                chr.changeSkillLevel(1310016, (byte) 30, (byte) 30);
                chr.teachSkill(1321013, 30);
                chr.changeSkillLevel(1321013, (byte) 30, (byte) 30);
                chr.teachSkill(1321014, 30);
                chr.changeSkillLevel(1321014, (byte) 30, (byte) 30);
                chr.teachSkill(1321012, 30);
                chr.changeSkillLevel(1321012, (byte) 30, (byte) 30);
                chr.teachSkill(1320017, 30);
                chr.changeSkillLevel(1320017, (byte) 30, (byte) 30);
                chr.teachSkill(1321015, 30);
                chr.changeSkillLevel(1321015, (byte) 30, (byte) 30);
                chr.teachSkill(1320016, 30);
                chr.changeSkillLevel(1320016, (byte) 30, (byte) 30);
                chr.teachSkill(1320018, 30);
                chr.changeSkillLevel(1320018, (byte) 30, (byte) 30);
                chr.teachSkill(1320011, 30);
                chr.changeSkillLevel(1320011, (byte) 30, (byte) 30);
                chr.teachSkill(1321000, 30);
                chr.changeSkillLevel(1321000, (byte) 30, (byte) 30);
                chr.teachSkill(1321010, 30);
                chr.changeSkillLevel(1321010, (byte) 30, (byte) 30);
                chr.teachSkill(1320043, 30);
                chr.changeSkillLevel(1320043, (byte) 30, (byte) 30);
                chr.teachSkill(1320044, 30);
                chr.changeSkillLevel(1320044, (byte) 30, (byte) 30);
                chr.teachSkill(1320045, 30);
                chr.changeSkillLevel(1320045, (byte) 30, (byte) 30);
                chr.teachSkill(1320046, 30);
                chr.changeSkillLevel(1320046, (byte) 30, (byte) 30);
                chr.teachSkill(1320047, 30);
                chr.changeSkillLevel(1320047, (byte) 30, (byte) 30);
                chr.teachSkill(1320048, 30);
                chr.changeSkillLevel(1320048, (byte) 30, (byte) 30);
                chr.teachSkill(1320049, 30);
                chr.changeSkillLevel(1320049, (byte) 30, (byte) 30);
                chr.teachSkill(1320050, 30);
                chr.changeSkillLevel(1320050, (byte) 30, (byte) 30);
                chr.teachSkill(1320051, 30);
                chr.changeSkillLevel(1320051, (byte) 30, (byte) 30);
                chr.teachSkill(1321052, 30);
                chr.changeSkillLevel(1321052, (byte) 30, (byte) 30);
                chr.teachSkill(1321053, 30);
                chr.changeSkillLevel(1321053, (byte) 30, (byte) 30);
                chr.teachSkill(1321054, 30);
                chr.changeSkillLevel(1321054, (byte) 30, (byte) 30);
                //▶다크나이트스킬레벨 max만들기종료
                break;
            case 212:
                chr.teachSkill(2101004, 30);
                chr.changeSkillLevel(2101004, (byte) 30, (byte) 30);
                chr.teachSkill(2100009, 30);
                chr.changeSkillLevel(2100009, (byte) 30, (byte) 30);
                chr.teachSkill(2101005, 30);
                chr.changeSkillLevel(2101005, (byte) 30, (byte) 30);
                chr.teachSkill(2101001, 30);
                chr.changeSkillLevel(2101001, (byte) 30, (byte) 30);
                chr.teachSkill(2101008, 30);
                chr.changeSkillLevel(2101008, (byte) 30, (byte) 30);
                chr.teachSkill(2101010, 30);
                chr.changeSkillLevel(2101010, (byte) 30, (byte) 30);
                chr.teachSkill(2100006, 30);
                chr.changeSkillLevel(2100006, (byte) 30, (byte) 30);
                chr.teachSkill(2100007, 30);
                chr.changeSkillLevel(2100007, (byte) 30, (byte) 30);
                chr.teachSkill(2100000, 30);
                chr.changeSkillLevel(2100000, (byte) 30, (byte) 30);
                chr.teachSkill(2111002, 30);
                chr.changeSkillLevel(2111002, (byte) 30, (byte) 30);
                chr.teachSkill(2111003, 30);
                chr.changeSkillLevel(2111003, (byte) 30, (byte) 30);
                chr.teachSkill(2111010, 30);
                chr.changeSkillLevel(2111010, (byte) 30, (byte) 30);
                chr.teachSkill(2111011, 30);
                chr.changeSkillLevel(2111011, (byte) 30, (byte) 30);
                chr.teachSkill(2111008, 30);
                chr.changeSkillLevel(2111008, (byte) 30, (byte) 30);
                chr.teachSkill(2111007, 30);
                chr.changeSkillLevel(2111007, (byte) 30, (byte) 30);
                chr.teachSkill(2110012, 30);
                chr.changeSkillLevel(2110012, (byte) 30, (byte) 30);
                chr.teachSkill(2110001, 30);
                chr.changeSkillLevel(2110001, (byte) 30, (byte) 30);
                chr.teachSkill(2110009, 30);
                chr.changeSkillLevel(2110009, (byte) 30, (byte) 30);
                chr.teachSkill(2110000, 30);
                chr.changeSkillLevel(2110000, (byte) 30, (byte) 30);
                chr.teachSkill(2121006, 30);
                chr.changeSkillLevel(2121006, (byte) 30, (byte) 30);
                chr.teachSkill(2121003, 30);
                chr.changeSkillLevel(2121003, (byte) 30, (byte) 30);
                chr.teachSkill(2120014, 30);
                chr.changeSkillLevel(2120014, (byte) 30, (byte) 30);
                chr.teachSkill(2121007, 30);
                chr.changeSkillLevel(2121007, (byte) 30, (byte) 30);
                chr.teachSkill(2121011, 30);
                chr.changeSkillLevel(2121011, (byte) 30, (byte) 30);
                chr.teachSkill(2121004, 30);
                chr.changeSkillLevel(2121004, (byte) 30, (byte) 30);
                chr.teachSkill(2121005, 30);
                chr.changeSkillLevel(2121005, (byte) 30, (byte) 30);
                chr.teachSkill(2121000, 30);
                chr.changeSkillLevel(2121000, (byte) 30, (byte) 30);
                chr.teachSkill(2121008, 30);
                chr.changeSkillLevel(2121008, (byte) 30, (byte) 30);
                chr.teachSkill(2120010, 30);
                chr.changeSkillLevel(2120010, (byte) 30, (byte) 30);
                chr.teachSkill(2120012, 30);
                chr.changeSkillLevel(2120012, (byte) 30, (byte) 30);
                chr.teachSkill(2120043, 30);
                chr.changeSkillLevel(2120043, (byte) 30, (byte) 30);
                chr.teachSkill(2120044, 30);
                chr.changeSkillLevel(2120044, (byte) 30, (byte) 30);
                chr.teachSkill(2120045, 30);
                chr.changeSkillLevel(2120045, (byte) 30, (byte) 30);
                chr.teachSkill(2120046, 30);
                chr.changeSkillLevel(2120046, (byte) 30, (byte) 30);
                chr.teachSkill(2120047, 30);
                chr.changeSkillLevel(2120047, (byte) 30, (byte) 30);
                chr.teachSkill(2120048, 30);
                chr.changeSkillLevel(2120048, (byte) 30, (byte) 30);
                chr.teachSkill(2120049, 30);
                chr.changeSkillLevel(2120049, (byte) 30, (byte) 30);
                chr.teachSkill(2120050, 30);
                chr.changeSkillLevel(2120050, (byte) 30, (byte) 30);
                chr.teachSkill(2120051, 30);
                chr.changeSkillLevel(2120051, (byte) 30, (byte) 30);
                chr.teachSkill(2121052, 30);
                chr.changeSkillLevel(2121052, (byte) 30, (byte) 30);
                chr.teachSkill(2121053, 30);
                chr.changeSkillLevel(2121053, (byte) 30, (byte) 30);
                chr.teachSkill(2121054, 30);
                chr.changeSkillLevel(2121054, (byte) 30, (byte) 30);
                break;
            case 222:
                chr.teachSkill(2201008, 30);
                chr.changeSkillLevel(2201008, (byte) 30, (byte) 30);

                chr.teachSkill(2200011, 30);
                chr.changeSkillLevel(2200011, (byte) 30, (byte) 30);

                chr.teachSkill(2201005, 30);
                chr.changeSkillLevel(2201005, (byte) 30, (byte) 30);

                chr.teachSkill(2201009, 30);
                chr.changeSkillLevel(2201009, (byte) 30, (byte) 30);

                chr.teachSkill(2201001, 30);
                chr.changeSkillLevel(2201001, (byte) 30, (byte) 30);

                chr.teachSkill(2201010, 30);
                chr.changeSkillLevel(2201010, (byte) 30, (byte) 30);

                chr.teachSkill(2200006, 30);
                chr.changeSkillLevel(2200006, (byte) 30, (byte) 30);

                chr.teachSkill(2200007, 30);
                chr.changeSkillLevel(2200007, (byte) 30, (byte) 30);

                chr.teachSkill(2200000, 30);
                chr.changeSkillLevel(2200000, (byte) 30, (byte) 30);

                chr.teachSkill(2211002, 30);
                chr.changeSkillLevel(2211002, (byte) 30, (byte) 30);

                chr.teachSkill(2211010, 30);
                chr.changeSkillLevel(2211010, (byte) 30, (byte) 30);

                chr.teachSkill(2211011, 30);
                chr.changeSkillLevel(2211011, (byte) 30, (byte) 30);

                chr.teachSkill(2211012, 30);
                chr.changeSkillLevel(2211012, (byte) 30, (byte) 30);

                chr.teachSkill(2211008, 30);
                chr.changeSkillLevel(2211008, (byte) 30, (byte) 30);

                chr.teachSkill(2211007, 30);
                chr.changeSkillLevel(2211007, (byte) 30, (byte) 30);

                chr.teachSkill(2210009, 30);
                chr.changeSkillLevel(2210009, (byte) 30, (byte) 30);

                chr.teachSkill(2210000, 30);
                chr.changeSkillLevel(2210000, (byte) 30, (byte) 30);

                chr.teachSkill(2210001, 30);
                chr.changeSkillLevel(2210001, (byte) 30, (byte) 30);

                chr.teachSkill(2221006, 30);
                chr.changeSkillLevel(2221006, (byte) 30, (byte) 30);

                chr.teachSkill(2221011, 30);
                chr.changeSkillLevel(2221011, (byte) 30, (byte) 30);

                chr.teachSkill(2221007, 30);
                chr.changeSkillLevel(2221007, (byte) 30, (byte) 30);

                chr.teachSkill(2221012, 30);
                chr.changeSkillLevel(2221012, (byte) 30, (byte) 30);

                chr.teachSkill(2221004, 30);
                chr.changeSkillLevel(2221004, (byte) 30, (byte) 30);

                chr.teachSkill(2221005, 30);
                chr.changeSkillLevel(2221005, (byte) 30, (byte) 30);

                chr.teachSkill(2221000, 30);
                chr.changeSkillLevel(2221000, (byte) 30, (byte) 30);

                chr.teachSkill(2221008, 30);
                chr.changeSkillLevel(2221008, (byte) 30, (byte) 30);

                chr.teachSkill(2220013, 30);
                chr.changeSkillLevel(2220013, (byte) 30, (byte) 30);

                chr.teachSkill(2220010, 30);
                chr.changeSkillLevel(2220010, (byte) 30, (byte) 30);

                chr.teachSkill(2220015, 30);
                chr.changeSkillLevel(2220015, (byte) 30, (byte) 30);

                chr.teachSkill(2220013, 30);
                chr.changeSkillLevel(2220013, (byte) 30, (byte) 30);

                chr.teachSkill(2221045, 30);
                chr.changeSkillLevel(2221045, (byte) 30, (byte) 30);

                chr.teachSkill(2220043, 30);
                chr.changeSkillLevel(2220043, (byte) 30, (byte) 30);

                chr.teachSkill(2220044, 30);
                chr.changeSkillLevel(2220044, (byte) 30, (byte) 30);

                chr.teachSkill(2220048, 30);
                chr.changeSkillLevel(2220048, (byte) 30, (byte) 30);

                chr.teachSkill(2220047, 30);
                chr.changeSkillLevel(2220047, (byte) 30, (byte) 30);

                chr.teachSkill(2220046, 30);
                chr.changeSkillLevel(2220046, (byte) 30, (byte) 30);

                chr.teachSkill(2220049, 30);
                chr.changeSkillLevel(2220049, (byte) 30, (byte) 30);

                chr.teachSkill(2220050, 30);
                chr.changeSkillLevel(2220050, (byte) 30, (byte) 30);

                chr.teachSkill(2220051, 30);
                chr.changeSkillLevel(2220051, (byte) 30, (byte) 30);

                chr.teachSkill(2221052, 30);
                chr.changeSkillLevel(2221052, (byte) 30, (byte) 30);

                chr.teachSkill(2221053, 30);
                chr.changeSkillLevel(2221053, (byte) 30, (byte) 30);

                chr.teachSkill(2221054, 30);
                chr.changeSkillLevel(2221054, (byte) 30, (byte) 30);
                break;
            case 232:
                chr.teachSkill(2301005, 30);
                chr.changeSkillLevel(2301005, (byte) 30, (byte) 30);

                chr.teachSkill(2300009, 30);
                chr.changeSkillLevel(2300009, (byte) 30, (byte) 30);

                chr.teachSkill(2301002, 30);
                chr.changeSkillLevel(2301002, (byte) 30, (byte) 30);

                chr.teachSkill(2301004, 30);
                chr.changeSkillLevel(2301004, (byte) 30, (byte) 30);

                chr.teachSkill(2301008, 30);
                chr.changeSkillLevel(2301008, (byte) 30, (byte) 30);

                chr.teachSkill(2300003, 30);
                chr.changeSkillLevel(2300003, (byte) 30, (byte) 30);

                chr.teachSkill(2300006, 30);
                chr.changeSkillLevel(2300006, (byte) 30, (byte) 30);

                chr.teachSkill(2300007, 30);
                chr.changeSkillLevel(2300007, (byte) 30, (byte) 30);

                chr.teachSkill(2300000, 30);
                chr.changeSkillLevel(2300000, (byte) 30, (byte) 30);

                chr.teachSkill(2311004, 30);
                chr.changeSkillLevel(2311004, (byte) 30, (byte) 30);

                chr.teachSkill(2311011, 30);
                chr.changeSkillLevel(2311011, (byte) 30, (byte) 30);

                chr.teachSkill(2311012, 30);
                chr.changeSkillLevel(2311012, (byte) 30, (byte) 30);

                chr.teachSkill(2311002, 30);
                chr.changeSkillLevel(2311002, (byte) 30, (byte) 30);

                chr.teachSkill(2311001, 30);
                chr.changeSkillLevel(2311001, (byte) 30, (byte) 30);

                chr.teachSkill(2311003, 30);
                chr.changeSkillLevel(2311003, (byte) 30, (byte) 30);

                chr.teachSkill(2311007, 30);
                chr.changeSkillLevel(2311007, (byte) 30, (byte) 30);

                chr.teachSkill(2311009, 30);
                chr.changeSkillLevel(2311009, (byte) 30, (byte) 30);

                chr.teachSkill(2310010, 30);
                chr.changeSkillLevel(2310010, (byte) 30, (byte) 30);

                chr.teachSkill(2310008, 30);
                chr.changeSkillLevel(2310008, (byte) 30, (byte) 30);

                chr.teachSkill(2321007, 30);
                chr.changeSkillLevel(2321007, (byte) 30, (byte) 30);

                chr.teachSkill(2321008, 30);
                chr.changeSkillLevel(2321008, (byte) 30, (byte) 30);

                chr.teachSkill(2321001, 30);
                chr.changeSkillLevel(2321001, (byte) 30, (byte) 30);

                chr.teachSkill(2320013, 30);
                chr.changeSkillLevel(2320013, (byte) 30, (byte) 30);

                chr.teachSkill(2321006, 30);
                chr.changeSkillLevel(2321006, (byte) 30, (byte) 30);

                chr.teachSkill(2321004, 30);
                chr.changeSkillLevel(2321004, (byte) 30, (byte) 30);

                chr.teachSkill(2321003, 30);
                chr.changeSkillLevel(2321003, (byte) 30, (byte) 30);

                chr.teachSkill(2321005, 30);
                chr.changeSkillLevel(2321005, (byte) 30, (byte) 30);

                chr.teachSkill(2321000, 30);
                chr.changeSkillLevel(2321000, (byte) 30, (byte) 30);

                chr.teachSkill(2321009, 30);
                chr.changeSkillLevel(2321009, (byte) 30, (byte) 30);

                chr.teachSkill(2320012, 30);
                chr.changeSkillLevel(2320012, (byte) 30, (byte) 30);

                chr.teachSkill(2320011, 30);
                chr.changeSkillLevel(2320011, (byte) 30, (byte) 30);

                chr.teachSkill(2320044, 30);
                chr.changeSkillLevel(2320044, (byte) 30, (byte) 30);

                chr.teachSkill(2320043, 30);
                chr.changeSkillLevel(2320043, (byte) 30, (byte) 30);

                chr.teachSkill(2320045, 30);
                chr.changeSkillLevel(2320045, (byte) 30, (byte) 30);

                chr.teachSkill(2320047, 30);
                chr.changeSkillLevel(2320047, (byte) 30, (byte) 30);

                chr.teachSkill(2320046, 30);
                chr.changeSkillLevel(2320046, (byte) 30, (byte) 30);

                chr.teachSkill(2320048, 30);
                chr.changeSkillLevel(2320048, (byte) 30, (byte) 30);

                chr.teachSkill(2320049, 30);
                chr.changeSkillLevel(2320049, (byte) 30, (byte) 30);

                chr.teachSkill(2320050, 30);
                chr.changeSkillLevel(2320050, (byte) 30, (byte) 30);

                chr.teachSkill(2320051, 30);
                chr.changeSkillLevel(2320051, (byte) 30, (byte) 30);

                chr.teachSkill(2321052, 30);
                chr.changeSkillLevel(2321052, (byte) 30, (byte) 30);

                chr.teachSkill(2321053, 30);
                chr.changeSkillLevel(2321053, (byte) 30, (byte) 30);

                chr.teachSkill(2321054, 30);
                chr.changeSkillLevel(2321054, (byte) 30, (byte) 30);
                break;
            case 312:
                chr.teachSkill(3001004, 30);
                chr.changeSkillLevel(3001004, (byte) 30, (byte) 30);
                chr.teachSkill(3001007, 30);
                chr.changeSkillLevel(3001007, (byte) 30, (byte) 30);
                chr.teachSkill(3000001, 30);
                chr.changeSkillLevel(3000001, (byte) 30, (byte) 30);
                chr.teachSkill(3000002, 30);
                chr.changeSkillLevel(3000002, (byte) 30, (byte) 30);
                chr.teachSkill(3101005, 30);
                chr.changeSkillLevel(3101005, (byte) 30, (byte) 30);
                chr.teachSkill(3101008, 30);
                chr.changeSkillLevel(3101008, (byte) 30, (byte) 30);
                chr.teachSkill(3101002, 30);
                chr.changeSkillLevel(3101002, (byte) 30, (byte) 30);
                chr.teachSkill(3101004, 30);
                chr.changeSkillLevel(3101004, (byte) 30, (byte) 30);
                chr.teachSkill(3101009, 30);
                chr.changeSkillLevel(3101009, (byte) 30, (byte) 30);
                chr.teachSkill(3100000, (byte) 30, (byte) 30);
                chr.changeSkillLevel(310000, (byte) 30, (byte) 30);
                chr.teachSkill(3100001, 30);
                chr.changeSkillLevel(3100001, (byte) 30, (byte) 30);
                chr.teachSkill(3100006, 30);
                chr.changeSkillLevel(3100006, (byte) 30, (byte) 30);
                chr.teachSkill(3111013, 30);
                chr.changeSkillLevel(3111013, (byte) 30, (byte) 30);
                chr.teachSkill(3111003, 30);
                chr.changeSkillLevel(3111003, (byte) 30, (byte) 30);
                chr.teachSkill(3111005, 30);
                chr.changeSkillLevel(3111005, (byte) 30, (byte) 30);
                chr.teachSkill(3111010, (byte) 30, (byte) 30);
                chr.changeSkillLevel(3111010, (byte) 30, (byte) 30);
                chr.teachSkill(3111011, 30);
                chr.changeSkillLevel(3111011, (byte) 30, (byte) 30);
                chr.teachSkill(3110001, 30);
                chr.changeSkillLevel(3110001, (byte) 30, (byte) 30);
                chr.teachSkill(3110012, 30);
                chr.changeSkillLevel(3110012, (byte) 30, (byte) 30);
                chr.teachSkill(3110007, 30);
                chr.changeSkillLevel(3110007, (byte) 30, (byte) 30);
                chr.teachSkill(3110014, 30);
                chr.changeSkillLevel(3110014, (byte) 30, (byte) 30);
                chr.teachSkill(3121020, (byte) 30, (byte) 30);
                chr.changeSkillLevel(3121020, (byte) 30, (byte) 30);
                chr.teachSkill(3121014, 30);
                chr.changeSkillLevel(3121014, (byte) 30, (byte) 30);
                chr.teachSkill(3121015, 30);
                chr.changeSkillLevel(3121015, (byte) 30, (byte) 30);
                chr.teachSkill(3121002, 30);
                chr.changeSkillLevel(3121002, (byte) 30, (byte) 30);
                chr.teachSkill(3121007, 30);
                chr.changeSkillLevel(3121007, (byte) 30, (byte) 30);
                chr.teachSkill(3121009, 30);
                chr.changeSkillLevel(3121009, (byte) 30, (byte) 30);
                chr.teachSkill(3121000, (byte) 30, (byte) 30);
                chr.changeSkillLevel(3121000, (byte) 30, (byte) 30);
                chr.teachSkill(3120008, 30);
                chr.changeSkillLevel(3120008, (byte) 30, (byte) 30);
                chr.teachSkill(3121016, 30);
                chr.changeSkillLevel(3121016, (byte) 30, (byte) 30);
                chr.teachSkill(3120018, 30);
                chr.changeSkillLevel(3120018, (byte) 30, (byte) 30);
                chr.teachSkill(3120043, 30);
                chr.changeSkillLevel(3120043, (byte) 30, (byte) 30);
                chr.teachSkill(3120044, 30);
                chr.changeSkillLevel(3120044, (byte) 30, (byte) 30);
                chr.teachSkill(3120045, 30);
                chr.changeSkillLevel(3120045, (byte) 30, (byte) 30);
                chr.teachSkill(3120046, 30);
                chr.changeSkillLevel(3120046, (byte) 30, (byte) 30);
                chr.teachSkill(3120047, 30);
                chr.changeSkillLevel(3120047, (byte) 30, (byte) 30);
                chr.teachSkill(3120048, 30);
                chr.changeSkillLevel(3120048, (byte) 30, (byte) 30);
                chr.teachSkill(3120049, 30);
                chr.changeSkillLevel(3120049, (byte) 30, (byte) 30);
                chr.teachSkill(3120050, (byte) 30, (byte) 30);
                chr.changeSkillLevel(3120050, (byte) 30, (byte) 30);
                chr.teachSkill(3120051, 30);
                chr.changeSkillLevel(3120051, (byte) 30, (byte) 30);
                chr.teachSkill(3121052, 30);
                chr.changeSkillLevel(3121052, (byte) 30, (byte) 30);
                chr.teachSkill(3120005, 30);
                chr.changeSkillLevel(3120005, (byte) 30, (byte) 30);
                chr.teachSkill(3121053, 30);
                chr.changeSkillLevel(3121053, (byte) 30, (byte) 30);
                chr.teachSkill(3121054, 30);
                chr.changeSkillLevel(3121054, (byte) 30, (byte) 30);
                //▶보마스킬레벨 0만들기종료
                break;
            case 322:
                //▶신궁스킬레벨 0만들기시작
                chr.teachSkill(3001004, 30);
                chr.changeSkillLevel(3001004, (byte) 30, (byte) 30);
                chr.teachSkill(3001007, 30);
                chr.changeSkillLevel(3001007, (byte) 30, (byte) 30);
                chr.teachSkill(3000001, 30);
                chr.changeSkillLevel(3000001, (byte) 30, (byte) 30);
                chr.teachSkill(3000002, 30);
                chr.changeSkillLevel(3000002, (byte) 30, (byte) 30);
                chr.teachSkill(3201005, 30);
                chr.changeSkillLevel(3201005, (byte) 30, (byte) 30);
                chr.teachSkill(3200009, 30);
                chr.changeSkillLevel(3200009, (byte) 30, (byte) 30);
                chr.teachSkill(3201008, 30);
                chr.changeSkillLevel(3201008, (byte) 30, (byte) 30);
                chr.teachSkill(3201002, 30);
                chr.changeSkillLevel(3201002, (byte) 30, (byte) 30);
                chr.teachSkill(3201004, 30);
                chr.changeSkillLevel(3201004, (byte) 30, (byte) 30);
                chr.teachSkill(3200000, 30, (byte) 30);
                chr.changeSkillLevel(320000, (byte) 30, (byte) 30);
                chr.teachSkill(3200001, 30);
                chr.changeSkillLevel(3200001, (byte) 30, (byte) 30);
                chr.teachSkill(3200006, 30);
                chr.changeSkillLevel(3200006, (byte) 30, (byte) 30);
                chr.teachSkill(3211009, 30);
                chr.changeSkillLevel(3211009, (byte) 30, (byte) 30);
                chr.teachSkill(3211008, 30);
                chr.changeSkillLevel(3211008, (byte) 30, (byte) 30);
                chr.teachSkill(3211005, 30);
                chr.changeSkillLevel(3211005, (byte) 30, (byte) 30);
                chr.teachSkill(321101, 30, (byte) 30);
                chr.changeSkillLevel(321101, (byte) 30, (byte) 30);
                chr.teachSkill(3211011, 30);
                chr.changeSkillLevel(3211011, (byte) 30, (byte) 30);
                chr.teachSkill(3211012, 30);
                chr.changeSkillLevel(3211012, (byte) 30, (byte) 30);
                chr.teachSkill(3210001, 30);
                chr.changeSkillLevel(3210001, (byte) 30, (byte) 30);
                chr.teachSkill(3210013, 30);
                chr.changeSkillLevel(3210013, (byte) 30, (byte) 30);
                chr.teachSkill(3210007, 30);
                chr.changeSkillLevel(3210007, (byte) 30, (byte) 30);
                chr.teachSkill(3210015, 30);
                chr.changeSkillLevel(3210015, (byte) 30, (byte) 30);
                chr.teachSkill(3221017, 30);
                chr.changeSkillLevel(3221017, (byte) 30, (byte) 30);
                chr.teachSkill(3221007, 30);
                chr.changeSkillLevel(3221007, (byte) 30, (byte) 30);
                chr.teachSkill(3221014, 30);
                chr.changeSkillLevel(3221014, (byte) 30, (byte) 30);
                chr.teachSkill(3221002, 30);
                chr.changeSkillLevel(3221002, (byte) 30, (byte) 30);
                chr.teachSkill(3221006, 30);
                chr.changeSkillLevel(3221006, (byte) 30, (byte) 30);
                chr.teachSkill(3221008, 30);
                chr.changeSkillLevel(3221008, (byte) 30, (byte) 30);
                chr.teachSkill(3221000, 30);
                chr.changeSkillLevel(3221000, (byte) 30, (byte) 30);
                chr.teachSkill(3220004, 30);
                chr.changeSkillLevel(3220004, (byte) 30, (byte) 30);
                chr.teachSkill(3220015, 30);
                chr.changeSkillLevel(3220015, (byte) 30, (byte) 30);
                chr.teachSkill(3220016, 30);
                chr.changeSkillLevel(3220016, (byte) 30, (byte) 30);
                chr.teachSkill(3220043, 30);
                chr.changeSkillLevel(3220043, (byte) 30, (byte) 30);
                chr.teachSkill(3220044, 30);
                chr.changeSkillLevel(3220044, (byte) 30, (byte) 30);
                chr.teachSkill(3220045, 30);
                chr.changeSkillLevel(3220045, (byte) 30, (byte) 30);
                chr.teachSkill(3220046, 30);
                chr.changeSkillLevel(3220046, (byte) 30, (byte) 30);
                chr.teachSkill(3220047, 30);
                chr.changeSkillLevel(3220047, (byte) 30, (byte) 30);
                chr.teachSkill(3220048, 30);
                chr.changeSkillLevel(3220048, (byte) 30, (byte) 30);
                chr.teachSkill(3220018, 30);
                chr.changeSkillLevel(3220018, (byte) 30, (byte) 30);
                chr.teachSkill(3220049, 30);
                chr.changeSkillLevel(3220049, (byte) 30, (byte) 30);
                chr.teachSkill(3220050, 30);
                chr.changeSkillLevel(3220050, (byte) 30, (byte) 30);
                chr.teachSkill(3220051, 30);
                chr.changeSkillLevel(3220051, (byte) 30, (byte) 30);
                chr.teachSkill(3221052, 30);
                chr.changeSkillLevel(3221052, (byte) 30, (byte) 30);
                chr.teachSkill(3221053, 30);
                chr.changeSkillLevel(3221053, (byte) 30, (byte) 30);
                chr.teachSkill(3221054, 30);
                chr.changeSkillLevel(3221054, (byte) 30, (byte) 30);
                //▶신궁스킬레벨 0만들기종료
                break;
            case 412:
                //▶나로스킬레벨 0만들기시작
                chr.teachSkill(4001334, 30);
                chr.changeSkillLevel(4001334, (byte) 30, (byte) 30);
                chr.teachSkill(4001344, 30);
                chr.changeSkillLevel(4001344, (byte) 30, (byte) 30);
                chr.teachSkill(4001005, 30);
                chr.changeSkillLevel(4001005, (byte) 30, (byte) 30);
                chr.teachSkill(4001003, 30);
                chr.changeSkillLevel(4001003, (byte) 30, (byte) 30);
                chr.teachSkill(4001014, 30);
                chr.changeSkillLevel(4001014, (byte) 30, (byte) 30);
                chr.teachSkill(4000000, 30);
                chr.changeSkillLevel(4000000, (byte) 30, (byte) 30);
                chr.teachSkill(4101008, 30);
                chr.changeSkillLevel(4101008, (byte) 30, (byte) 30);
                chr.teachSkill(4101011, 30);
                chr.changeSkillLevel(4101011, (byte) 30, (byte) 30);
                chr.teachSkill(4100012, 30);
                chr.changeSkillLevel(4100012, (byte) 30, (byte) 30);
                chr.teachSkill(4100011, 30);
                chr.changeSkillLevel(4100011, (byte) 30, (byte) 30);
                chr.teachSkill(4101010, 30);
                chr.changeSkillLevel(4100011, (byte) 30, (byte) 30);
                chr.teachSkill(4101003, 30);
                chr.changeSkillLevel(4101003, (byte) 30, (byte) 30);
                chr.teachSkill(4100000, 30);
                chr.changeSkillLevel(4100000, (byte) 30, (byte) 30);
                chr.teachSkill(4100001, 30);
                chr.changeSkillLevel(4100001, (byte) 30, (byte) 30);
                chr.teachSkill(4100007, 30);
                chr.changeSkillLevel(4100007, (byte) 30, (byte) 30);
                chr.teachSkill(4111010, 30);
                chr.changeSkillLevel(4111010, (byte) 30, (byte) 30);
                chr.teachSkill(4111015, 30);
                chr.changeSkillLevel(4111015, (byte) 30, (byte) 30);
                chr.teachSkill(4111007, 30);
                chr.changeSkillLevel(4111007, (byte) 30, (byte) 30);
                chr.teachSkill(4111003, 30);
                chr.changeSkillLevel(4111003, (byte) 30, (byte) 30);
                chr.teachSkill(4111002, 30);
                chr.changeSkillLevel(4111002, (byte) 30, (byte) 30);
                chr.teachSkill(4111009, 30);
                chr.changeSkillLevel(4111009, (byte) 30, (byte) 30);
                chr.teachSkill(4110008, 30);
                chr.changeSkillLevel(4110008, (byte) 30, (byte) 30);
                chr.teachSkill(4110011, 30);
                chr.changeSkillLevel(4110011, (byte) 30, (byte) 30);
                chr.teachSkill(4110012, 30);
                chr.changeSkillLevel(4110012, (byte) 30, (byte) 30);
                chr.teachSkill(4110014, 30);
                chr.changeSkillLevel(4110014, (byte) 30, (byte) 30);
                chr.teachSkill(4121013, 30);
                chr.changeSkillLevel(4121013, (byte) 30, (byte) 30);
                chr.teachSkill(4121017, 30);
                chr.changeSkillLevel(4121017, (byte) 30, (byte) 30);
                chr.teachSkill(4120018, 30);
                chr.changeSkillLevel(4120018, (byte) 30, (byte) 30);
                chr.teachSkill(4120019, 30);
                chr.changeSkillLevel(4120019, (byte) 30, (byte) 30);
                chr.teachSkill(4121016, 30);
                chr.changeSkillLevel(4121016, (byte) 30, (byte) 30);
                chr.teachSkill(4121015, 30);
                chr.changeSkillLevel(4121015, (byte) 30, (byte) 30);
                chr.teachSkill(4121000, 30);
                chr.changeSkillLevel(4121000, (byte) 30, (byte) 30);
                chr.teachSkill(4121009, 30);
                chr.changeSkillLevel(4121009, (byte) 30, (byte) 30);
                chr.teachSkill(4120014, 30);
                chr.changeSkillLevel(4120014, (byte) 30, (byte) 30);
                chr.teachSkill(4120002, 30);
                chr.changeSkillLevel(4120002, (byte) 30, (byte) 30);
                chr.teachSkill(4120011, 30);
                chr.changeSkillLevel(4120011, (byte) 30, (byte) 30);
                chr.teachSkill(4120012, 30);
                chr.changeSkillLevel(4120012, (byte) 30, (byte) 30);
                chr.teachSkill(4120043, 30);
                chr.changeSkillLevel(4120043, (byte) 30, (byte) 30);
                chr.teachSkill(4120044, 30);
                chr.changeSkillLevel(4120044, (byte) 30, (byte) 30);
                chr.teachSkill(4120045, 30);
                chr.changeSkillLevel(4120045, (byte) 30, (byte) 30);
                chr.teachSkill(4120048, 30);
                chr.changeSkillLevel(4120048, (byte) 30, (byte) 30);
                chr.teachSkill(4120046, 30);
                chr.changeSkillLevel(4120046, (byte) 30, (byte) 30);
                chr.teachSkill(4120047, 30);
                chr.changeSkillLevel(4120047, (byte) 30, (byte) 30);
                chr.teachSkill(4120051, 30);
                chr.changeSkillLevel(4120051, (byte) 30, (byte) 30);
                chr.teachSkill(4120050, 30);
                chr.changeSkillLevel(4120050, (byte) 30, (byte) 30);
                chr.teachSkill(4120047, 30);
                chr.changeSkillLevel(4120047, (byte) 30, (byte) 30);
                chr.teachSkill(4120051, 30);
                chr.changeSkillLevel(4120051, (byte) 30, (byte) 30);
                chr.teachSkill(4120050, 30);
                chr.changeSkillLevel(4120050, (byte) 30, (byte) 30);
                chr.teachSkill(4120049, 30);
                chr.changeSkillLevel(4120049, (byte) 30, (byte) 30);
                chr.teachSkill(4121052, 30);
                chr.changeSkillLevel(4121052, (byte) 30, (byte) 30);
                chr.teachSkill(4121053, 30);
                chr.changeSkillLevel(4121053, (byte) 30, (byte) 30);
                chr.teachSkill(4121054, 30);
                chr.changeSkillLevel(4121054, (byte) 30, (byte) 30);
                //▶나로스킬레벨 0만들기종료
                break;
            case 422:
                //▶섀도어스킬레벨 0만들기시작
                chr.teachSkill(4221014, 30);
                chr.changeSkillLevel(4221014, (byte) 30, (byte) 30);
                chr.teachSkill(4001334, 30);
                chr.changeSkillLevel(4001334, (byte) 30, (byte) 30);
                chr.teachSkill(4001344, 30);
                chr.changeSkillLevel(4001344, (byte) 30, (byte) 30);
                chr.teachSkill(4001005, 30);
                chr.changeSkillLevel(4001005, (byte) 30, (byte) 30);
                chr.teachSkill(4001003, 30);
                chr.changeSkillLevel(4001003, (byte) 30, (byte) 30);
                chr.teachSkill(4001014, 30);
                chr.changeSkillLevel(4001014, (byte) 30, (byte) 30);
                chr.teachSkill(4000000, 30);
                chr.changeSkillLevel(4000000, (byte) 30, (byte) 30);
                chr.teachSkill(4201012, 30);
                chr.changeSkillLevel(4201012, (byte) 30, (byte) 30);
                chr.teachSkill(4200013, 30);
                chr.changeSkillLevel(4200013, (byte) 30, (byte) 30);
                chr.teachSkill(4201004, 30);
                chr.changeSkillLevel(4201004, (byte) 30, (byte) 30);
                chr.teachSkill(4201002, 30);
                chr.changeSkillLevel(4201002, (byte) 30, (byte) 30);
                chr.teachSkill(4201011, 30);
                chr.changeSkillLevel(4201011, (byte) 30, (byte) 30);
                chr.teachSkill(4200009, 30);
                chr.changeSkillLevel(4200009, (byte) 30, (byte) 30);
                chr.teachSkill(4200000, 30);
                chr.changeSkillLevel(4200000, (byte) 30, (byte) 30);
                chr.teachSkill(4200007, 30);
                chr.changeSkillLevel(4200007, (byte) 30, (byte) 30);
                chr.teachSkill(4200010, 30);
                chr.changeSkillLevel(4200010, (byte) 30, (byte) 30);
                chr.teachSkill(4201012, 30);
                chr.changeSkillLevel(4201012, (byte) 30, (byte) 30);
                chr.teachSkill(4211011, 30);
                chr.changeSkillLevel(4211011, (byte) 30, (byte) 30);
                chr.teachSkill(4211002, 30);
                chr.changeSkillLevel(4211002, (byte) 30, (byte) 30);
                chr.teachSkill(4211006, 30);
                chr.changeSkillLevel(4211006, (byte) 30, (byte) 30);
                chr.teachSkill(4211007, 30);
                chr.changeSkillLevel(4211007, (byte) 30, (byte) 30);
                chr.teachSkill(4211003, 30);
                chr.changeSkillLevel(4211003, (byte) 30, (byte) 30);
                chr.teachSkill(4211008, 30);
                chr.changeSkillLevel(4211008, (byte) 30, (byte) 30);
                chr.teachSkill(4210015, 30);
                chr.changeSkillLevel(4210015, (byte) 30, (byte) 30);
                chr.teachSkill(4211016, 30);
                chr.changeSkillLevel(4211016, (byte) 30, (byte) 30);
                chr.teachSkill(4210010, 30);
                chr.changeSkillLevel(4210010, (byte) 30, (byte) 30);
                chr.teachSkill(4210012, 30);
                chr.changeSkillLevel(4210012, (byte) 30, (byte) 30);
                chr.teachSkill(4210013, 30);
                chr.changeSkillLevel(4210013, (byte) 30, (byte) 30);
                chr.teachSkill(4210014, 30);
                chr.changeSkillLevel(4210014, (byte) 30, (byte) 30);
                chr.teachSkill(4221007, 30);
                chr.changeSkillLevel(4221007, (byte) 30, (byte) 30);
                chr.teachSkill(4220015, 30);
                chr.changeSkillLevel(4220015, (byte) 30, (byte) 30);
                chr.teachSkill(4221010, 30);
                chr.changeSkillLevel(4221010, (byte) 30, (byte) 30);
                chr.teachSkill(4221000, 30);
                chr.changeSkillLevel(4221000, (byte) 30, (byte) 30);
                chr.teachSkill(4221006, 30);
                chr.changeSkillLevel(4221006, (byte) 30, (byte) 30);
                chr.teachSkill(4221008, 30);
                chr.changeSkillLevel(4221008, (byte) 30, (byte) 30);
                chr.teachSkill(4221013, 30);
                chr.changeSkillLevel(4221013, (byte) 30, (byte) 30);
                chr.teachSkill(4220002, 30);
                chr.changeSkillLevel(4220002, (byte) 30, (byte) 30);
                chr.teachSkill(4220011, 30);
                chr.changeSkillLevel(4220011, (byte) 30, (byte) 30);
                chr.teachSkill(4220012, 30);
                chr.changeSkillLevel(4220012, (byte) 30, (byte) 30);
                chr.teachSkill(4221052, 30);
                chr.changeSkillLevel(4221052, (byte) 30, (byte) 30);
                chr.teachSkill(4221053, 30);
                chr.changeSkillLevel(4221053, (byte) 30, (byte) 30);
                chr.teachSkill(4221054, 30);
                chr.changeSkillLevel(4221054, (byte) 30, (byte) 30);
                //▶섀도어스킬레벨 0만들기종료
                break;
            case 434:
                chr.teachSkill(4331000, 30);
                chr.changeSkillLevel(4331000, (byte) 30, (byte) 30);
                chr.teachSkill(4001334, 30);
                chr.changeSkillLevel(4001334, (byte) 30, (byte) 30);
                chr.teachSkill(4001344, 30);
                chr.changeSkillLevel(4001344, (byte) 30, (byte) 30);
                chr.teachSkill(4001005, 30);
                chr.changeSkillLevel(4001005, (byte) 30, (byte) 30);
                chr.teachSkill(4001003, 30);
                chr.changeSkillLevel(4001003, (byte) 30, (byte) 30);
                chr.teachSkill(4001014, 30);
                chr.changeSkillLevel(4001014, (byte) 30, (byte) 30);
                chr.teachSkill(4000000, 30);
                chr.changeSkillLevel(4000000, (byte) 30, (byte) 30);
                chr.teachSkill(4300000, 30);
                chr.changeSkillLevel(4300000, (byte) 30, (byte) 30);
                chr.teachSkill(4301003, 30);
                chr.changeSkillLevel(4301003, (byte) 30, (byte) 30);
                chr.teachSkill(4301004, 30);
                chr.changeSkillLevel(4301004, (byte) 30, (byte) 30);
                chr.teachSkill(4311002, 30);
                chr.changeSkillLevel(4311002, (byte) 30, (byte) 30);
                chr.teachSkill(4311003, 30);
                chr.changeSkillLevel(4311003, (byte) 30, (byte) 30);
                chr.teachSkill(4311009, 30);
                chr.changeSkillLevel(4311009, (byte) 30, (byte) 30);
                chr.teachSkill(4310005, 30);
                chr.changeSkillLevel(4310005, (byte) 30, (byte) 30);
                chr.teachSkill(4310006, 30);
                chr.changeSkillLevel(4310006, (byte) 30, (byte) 30);
                chr.teachSkill(4321006, 30);
                chr.changeSkillLevel(4321006, (byte) 30, (byte) 30);
                chr.teachSkill(4321004, 30);
                chr.changeSkillLevel(4321004, (byte) 30, (byte) 30);
                chr.teachSkill(4321002, 30);
                chr.changeSkillLevel(4321002, (byte) 30, (byte) 30);
                chr.teachSkill(4320005, 30);
                chr.changeSkillLevel(4320005, (byte) 30, (byte) 30);
                chr.teachSkill(4320005, 30);
                chr.changeSkillLevel(4320005, (byte) 30, (byte) 30);
                chr.teachSkill(4331011, 30);
                chr.changeSkillLevel(4331011, (byte) 30, (byte) 30);
                chr.teachSkill(4331006, 30);
                chr.changeSkillLevel(4331006, (byte) 30, (byte) 30);
                chr.teachSkill(4331002, 30);
                chr.changeSkillLevel(4331002, (byte) 30, (byte) 30);
                chr.teachSkill(4330001, 30);
                chr.changeSkillLevel(4330001, (byte) 30, (byte) 30);
                chr.teachSkill(4330007, 30);
                chr.changeSkillLevel(4330007, (byte) 30, (byte) 30);
                chr.teachSkill(4330008, 30);
                chr.changeSkillLevel(4330008, (byte) 30, (byte) 30);
                chr.teachSkill(4330009, 30);
                chr.changeSkillLevel(4330009, (byte) 30, (byte) 30);
                chr.teachSkill(4341004, 30);
                chr.changeSkillLevel(4341004, (byte) 30, (byte) 30);
                chr.teachSkill(4341009, 30);
                chr.changeSkillLevel(4341009, (byte) 30, (byte) 30);
                chr.teachSkill(4341002, 30);
                chr.changeSkillLevel(4341002, (byte) 30, (byte) 30);
                chr.teachSkill(4341011, 30);
                chr.changeSkillLevel(4341011, (byte) 30, (byte) 30);
                chr.teachSkill(4341000, 30);
                chr.changeSkillLevel(4341000, (byte) 30, (byte) 30);
                chr.teachSkill(4341008, 30);
                chr.changeSkillLevel(4341008, (byte) 30, (byte) 30);
                chr.teachSkill(4341006, 30);
                chr.changeSkillLevel(4341006, (byte) 30, (byte) 30);
                chr.teachSkill(4340007, 30);
                chr.changeSkillLevel(4340007, (byte) 30, (byte) 30);
                chr.teachSkill(4340010, 30);
                chr.changeSkillLevel(4340010, (byte) 30, (byte) 30);
                chr.teachSkill(4340012, 30);
                chr.changeSkillLevel(4340012, (byte) 30, (byte) 30);
                chr.teachSkill(4340013, 30);
                chr.changeSkillLevel(4340013, (byte) 30, (byte) 30);
                chr.teachSkill(4341052, 30);
                chr.changeSkillLevel(4341052, (byte) 30, (byte) 30);
                chr.teachSkill(4341053, 30);
                chr.changeSkillLevel(4341053, (byte) 30, (byte) 30);
                chr.teachSkill(4341054, 30);
                chr.changeSkillLevel(4341054, (byte) 30, (byte) 30);
                break;
            case 512:
                break;
            case 522:
                break;
            case 532:
                break;
        }
        c.removeClickedNPC();
        NPCScriptManager.getInstance().dispose(c);
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        c.getPlayer().dropMessage(1, "자유전직이 완료되었습니다.");
        return;
    }

    public static void fpsShootRequest(LittleEndianAccessor slea, MapleClient c) {
        slea.skip(4); // tick
        slea.skip(4); // pos
        if (c.getPlayer() != null) {
            FrittoEagle eagle = c.getPlayer().getFrittoEagle();
            if (eagle != null) {
                int size = slea.readInt();
                eagle.shootResult(c);
                for (int i = 0; i < size; ++i) {
                    int objectId = slea.readInt();
                    slea.skip(4 + 1 + 1 + 2 + 2 + 2 + 2);
                    MapleMap map = c.getPlayer().getMap();
                    if (map != null) {
                        MapleMonster mob = map.getMonsterByOid(objectId);
                        if (mob != null) {
                            eagle.addScore(mob, c);
                            map.killMonster(mob);
                        }
                    }
                }
            }
        }
    }

    public static void courtshipCommand(LittleEndianAccessor slea, MapleClient c) {
        boolean success = slea.readByte() == 1;
        if (success) {
            if (c.getPlayer() != null) {
                FrittoDancing fd = c.getPlayer().getFrittoDancing();
                if (fd != null) {
                    c.getPlayer().setKeyValue(15143, "score", String.valueOf(c.getPlayer().getKeyValue(15143, "score") + 1));
                    if (c.getPlayer().getKeyValue(15143, "score") >= 10) {
                        fd.finish(c);
                    }
                }
            }
        }
    }

    public static void HandleFullMakerUsed(MapleCharacter chr, int objectid, int skillid, Point pos) {

        Iterator<MapleMist> mists = chr.getMap().getAllMistsThreadsafe().iterator();
        while (mists.hasNext()) {
            MapleMist mist = mists.next();
            MapleCharacter chaa = mist.getOwner();
            if (chaa.getParty() != null) {
                if (chr.getParty() == null) {
                    return;
                } else {
                    if (chr.getParty().getId() != chaa.getParty().getId()) {
                        return;
                    }
                }

            }
            if (chaa.getParty() == null && chaa.getId() != chr.getId()) {
                return;
            }
            if (mist.getObjectId() == objectid) {
                Map<MapleBuffStat, Integer> statups = new HashMap<>();
                MapleStatEffect effect = SkillFactory.getSkill(400051077).getEffect(chr.getSkillLevel(400051077));
                effect.applyTo(chr);
                chr.FullMakerBoxCount--;
                chr.getMap().broadcastMessage(CField.removeMist(mist));
                chr.getMap().removeMapObject(mist);
            }
        }
    }

    public static void HandleMeachCarrier(MapleCharacter chr, int ob, byte unkb) {
        List<AdelProjectile> atoms = new ArrayList<>();
        MapleStatEffect effect = SkillFactory.getSkill(400051069).getEffect(chr.getSkillLevel(400051069));
        MapleMonster mob = null;

        if (chr.mecahCarriercount > 16) {
            chr.mecahCarriercount = 16;
        }

        for (MapleSummon s : chr.getMap().getAllSummonsThreadsafe()) {

            if (s.getObjectId() == ob) {

                for (MapleMapObject monstermo : chr.getMap().getMapObjectsInRange(chr.getPosition(), 500000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                    mob = (MapleMonster) monstermo;
                    if (mob == null) {
                        return;
                    }
                }

                for (int i = 0; i < chr.mecahCarriercount; i++) {
                    if (mob == null) {
                        return;
                    }
                    AdelProjectile sa = new AdelProjectile(10, chr.getId(), mob.getObjectId(), 400051069, 3600, 0, 1, new Point(Randomizer.rand(effect.getLt().x, effect.getRb().x), Randomizer.rand(effect.getLt().y, effect.getRb().y)), new ArrayList<>());
                    sa.setPoint(s.getPosition());
                    atoms.add(sa);
                }

                if (!atoms.isEmpty()) {
                    chr.getMap().spawnAdelProjectile(chr, atoms, false);
                    chr.mecahCarriercount++;
                }
                break;
            }
        }

    }

    public static void HandleKainLink(MapleCharacter chr, int killcount, int bossattack) {
        if (chr.getCooldownLimit(GameConstants.isKain(chr.getJob()) ? 60030241 : 80003015) == 0) {
            if (killcount > 0) {
                chr.KainLinkKillCount += 1;
            }
            if (bossattack > 0) {
                chr.KainLinkBattackCount += 1;
            }
            if (chr.KainLinkKillCount >= 8) {
                chr.KainLinkKillCount = 0;
                chr.KainLinkCount += 1;
                SkillFactory.getSkill(80003018).getEffect(1).applyTo(chr, false);

            }
            if (chr.KainLinkBattackCount >= 5) {
                chr.KainLinkBattackCount = 0;
                chr.KainLinkCount += 1;
                SkillFactory.getSkill(80003018).getEffect(1).applyTo(chr, false);
            }
            if (chr.KainLinkCount >= 5) {
                if (chr.getBuffedValue(MapleBuffStat.IndieDamR, 80003015) == null) {
                    MapleStatEffect effect = SkillFactory.getSkill(GameConstants.isKain(chr.getJob()) ? 60030241 : 80003015).getEffect(chr.getSkillLevel(chr.getSkillLevel(GameConstants.isKain(chr.getJob()) ? 60030241 : 80003015)));
                    chr.cancelEffectFromBuffStat(MapleBuffStat.KainLink);
                    chr.addCooldown(GameConstants.isKain(chr.getJob()) ? 60030241 : 80003015, System.currentTimeMillis(), effect.getCooltime());
                    chr.KainLinkCount = 0;
                    chr.getClient().getSession().writeAndFlush(CField.skillCooldown(GameConstants.isKain(chr.getJob()) ? 60030241 : 80003015, effect.getCooltime()));
                    effect.applyTo(chr);
                }
            }
        } else {
            chr.KainLinkKillCount = 0;
            chr.KainLinkBattackCount = 0;
            chr.KainLinkCount = 0;
        }
    }

    public static void HandleGripOfAgony(MapleCharacter chr, int killcount, int bossattack) {
        Map<MapleBuffStat, Pair<Integer, Integer>> statups = new HashMap<>();
        if (killcount > 0) {
            chr.AgonyKillCount += 1;
        }
        if (bossattack > 0) {
            chr.AgonyBattackCount += 1;
        }
        if (chr.AgonyKillCount >= 30) {
            chr.AgonyKillCount = 0;
            chr.AgonyCount += 1;
            SkillFactory.getSkill(400031066).getEffect(1).applyTo(chr, false);

        }
        if (chr.AgonyBattackCount >= 25) {
            chr.AgonyBattackCount = 0;
            chr.AgonyCount += 1;
            SkillFactory.getSkill(400031066).getEffect(1).applyTo(chr, false);
        }
        if (chr.AgonyCount >= 15) {
            chr.AgonyCount = 15;
        }
    }

    public static void Tactical(LittleEndianAccessor slea, MapleCharacter chr) {
        int start = slea.readInt();
        int questid = 100417 + Integer.parseInt(chr.getClient().getKeyValue("current"));
        final MapleQuest quest = MapleQuest.getInstance(questid);
        final MapleQuestStatus status = chr.getQuestNAdd(quest);
        if (start == 1) {
            if (Integer.parseInt(chr.getClient().getKeyValue("s1")) != chr.getId() && Integer.parseInt(chr.getClient().getKeyValue("s2")) != chr.getId()
                    && Integer.parseInt(chr.getClient().getKeyValue("s3")) != chr.getId() && Integer.parseInt(chr.getClient().getKeyValue("s4")) != chr.getId()
                    && Integer.parseInt(chr.getClient().getKeyValue("s5")) != chr.getId() && Integer.parseInt(chr.getClient().getKeyValue("s6")) != chr.getId()
                    && Integer.parseInt(chr.getClient().getKeyValue("s7")) != chr.getId() && Integer.parseInt(chr.getClient().getKeyValue("s8")) != chr.getId()
                    && Integer.parseInt(chr.getClient().getKeyValue("s9")) != chr.getId()) {
                status.setStatus((byte) 1);
                chr.getClient().setKeyValue("state", "1");
                chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500885, "state=" + chr.getClient().getKeyValue("state") + ";current=" + chr.getClient().getKeyValue("current") + ";total=" + chr.getClient().getKeyValue("total") + ";"));
                chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuest(status));
            } else {
                chr.getClient().getSession().writeAndFlush(CWvsContext.serverNotice(1, "", "이 캐릭터는 이미 미션을 완료하였습니다."));
            }
        } else if (start == 2) {
            status.setStatus((byte) 0);
            chr.getClient().setKeyValue("state", "0");
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500885, "state=" + chr.getClient().getKeyValue("state") + ";total=" + chr.getClient().getKeyValue("total") + ";current=" + chr.getClient().getKeyValue("current") + ";"));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuest(status));
        } else if (start == 3) {
            chr.getClient().setKeyValue("state", "0");
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateQuestInfo(1, 2, questid));
            chr.getClient().setKeyValue("s" + chr.getClient().getKeyValue("current"), "" + chr.getId());
            chr.getClient().setKeyValue("current", "" + (Integer.parseInt(chr.getClient().getKeyValue("current")) + 1));
            chr.getClient().setKeyValue("total", "" + (Integer.parseInt(chr.getClient().getKeyValue("total")) + 50));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500885, "state=" + chr.getClient().getKeyValue("state") + ";current=" + chr.getClient().getKeyValue("current") + ";total=" + chr.getClient().getKeyValue("total") + ";"));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500886, "s1=" + chr.getClient().getKeyValue("s1") + ";s2=" + chr.getClient().getKeyValue("s2") + ";s3=" + chr.getClient().getKeyValue("s3") + ";"));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500887, "s4=" + chr.getClient().getKeyValue("s4") + ";s5=" + chr.getClient().getKeyValue("s5") + ";s6=" + chr.getClient().getKeyValue("s6") + ";"));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500888, "s7=" + chr.getClient().getKeyValue("s7") + ";s8=" + chr.getClient().getKeyValue("s8") + ";s9=" + chr.getClient().getKeyValue("s9") + ";"));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500890, "s1=1;s2=1;s3=1;s4=1;s5=1;s6=1;s7=1;s8=1;s9=1;"));
            chr.getClient().getSession().writeAndFlush(CWvsContext.InfoPacket.updateInfoQuest(500889, "s1=1;s2=1;s3=1;s4=1;s5=1;s6=1;s7=1;s8=1;s9=1;"));
        }
        chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions(chr));
    }

    public static void HandleMirrorTouch(LittleEndianAccessor slea, MapleCharacter chr) {
        int mobid = slea.readInt();
        chr.getStat().setHp(0, chr);
        for (MapleMonster m : chr.getMap().getAllMonstersThreadsafe()) {
            if (mobid == 8920005) { //카오스 유혹의 거울
                if (m.getId() >= 8920000 && m.getId() <= 8920003) { //카오스 퀸 체력 25% 회복 본섭기준 1400억 * 0.25 = 350억
                    m.setHp(m.getHp() + 35000000000L >= 140000000000L ? 140000000000L : m.getHp() + 35000000000L);
                    break;
                }
            } else if (mobid == 8920105) { //유혹의 거울
                if (m.getId() >= 8920100 && m.getId() <= 8920103) { //노말 퀸 체력 25% 회복 본섭기준 3억1500만 * 0.25 = 1575만
                    m.setHp(m.getHp() + 15750000L >= 315000000L ? 315000000L : m.getHp() + 15750000L);
                    break;
                }
            }

        }

    }

    public static void HandleQueenBreath(LittleEndianAccessor slea, MapleCharacter chr) {
        int unk1 = slea.readInt();
        byte unk2 = slea.readByte();
        if (chr.isAlive()) {
            if (chr.getBuffedValue(MapleBuffStat.NotDamaged) != null || chr.getBuffedValue(MapleBuffStat.IndieNotDamaged) != null || chr.getBuffedValue(MapleBuffStat.HeavensDoor) != null) {
                if (chr.getBuffedValue(MapleBuffStat.HeavensDoor) != null) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.HeavensDoor);
                }
            } else {
                chr.cancelEffectFromBuffStat(MapleBuffStat.FireBomb);
                chr.getMap().broadcastMessage(MobPacket.BossBlodyQueen.QueenBreathAttack(unk1, unk2));
            }
        }
    }

    public static void ReadVeinOfInfinty(LittleEndianAccessor slea, MapleCharacter chr) {
        byte idk = slea.readByte(); // 2 or 3
        int id = slea.readInt();
        byte type = slea.readByte();
        int x = slea.readInt();
        int y = slea.readInt();
        int idk2 = slea.readInt();

        List<AdelProjectile> VeinOfInfinity = new ArrayList<>();
        List<Integer> points = new ArrayList<>();
        points.add((int) type);
        points.add(id);
        MapleStatEffect effect = SkillFactory.getSkill(162101000).getEffect(chr.getSkillLevel(162101000));
        AdelProjectile ag = new AdelProjectile(0x14, chr.getId(), 0, 162101000, 0, 0, 1, new Point(x, y), points);
        ag.setIdk2(idk2);
        VeinOfInfinity.add(ag);
        chr.getMap().spawnAdelProjectile(chr, VeinOfInfinity, false);
    }

    public static void ChargeSeed(LittleEndianAccessor slea, MapleCharacter chr) {
        int skillId = slea.readInt();
        slea.readInt();

        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(chr.getSkillLevel(skillId));
        int maxStack = 0;

        switch (skillId) {
            case 162101012:
                maxStack = effect.getW2();
                break;
            case 162111006:
                maxStack = effect.getV();
                break;
            case 162121042:
                maxStack = effect.getU();
                break;
        }

        if (maxStack > 0) {
            int prevStack = chr.getSkillCustomValue(skillId);
            if (prevStack < maxStack) {
                chr.setSkillCustomValue(skillId, prevStack + 1);
                effect.applyTo(chr, false);
            }
        }

    }

    public static void UseEventSkill(LittleEndianAccessor slea, MapleCharacter chr) {
        int skillID = slea.readInt();
        int skillLevel = chr.getSkillLevel(skillID);

        if (skillID == 80003082 && skillLevel > 0) {
            MapleStatEffect effect = SkillFactory.getSkill(skillID).getEffect(chr.getSkillLevel(skillID));

            if (chr.getBuffedValue(skillID)) {
                chr.cancelEffect(effect, true, -1);
            } else {
                effect.applyTo(chr, true);
            }
        }

    }

    public static void JupiterThunder(MapleCharacter player, final LittleEndianAccessor slea, int type) {
        if (type == 0) {
            // 66 D6 D7 17 // skillid
            // D2 00 00 00
            // 01 00 00 00
            // 01 00 00 00
            // 79 04 00 00 pos x
            // 4E 01 00 00 pos y
            // EE FF FF FF
            // 00 00 00 00
            int skillid = slea.readInt();
            int unk0 = slea.readInt();
            int unk1 = slea.readInt();
            int unk2 = slea.readInt();
            Point pos = new Point(slea.readInt(), slea.readInt());
            int unk3 = slea.readInt();
            int unk4 = slea.readInt();
            MapleStatEffect effect = SkillFactory.getSkill(skillid).getEffect(player.getSkillLevel(skillid));
            player.getMap().broadcastMessage(CField.CreateJupiterThunder(player, skillid, pos, unk3, unk4, effect.getX(), effect.getSubTime() / 1000, effect.getDuration() / 1000, unk0, unk1, unk2));
            player.getClient().getSession().writeAndFlush(CField.skillCooldown(effect.getSourceId(), effect.getCooldown(player)));
            player.addCooldown(effect.getSourceId(), System.currentTimeMillis(), effect.getCooldown(player));
        } else if (type == 1) {
            int unk0 = slea.readInt();
            int objid = slea.readInt();
            int charid = slea.readInt();
            int skillid = slea.readInt();
            int unk1 = slea.readInt();
            int unk2 = slea.readInt();
            int unk3 = slea.readInt();
            int unk4 = slea.readInt();
            int unk5 = slea.readInt();
            player.getMap().broadcastMessage(CField.MoveJupiterThunder(player, unk0, objid, unk1, unk2, unk3, unk4, unk5));
        } else if (type == 2) {
            MapleStatEffect effect = SkillFactory.getSkill(400021094).getEffect(player.getSkillLevel(400021094));
            slea.skip(4);
            int oid = slea.readInt();
            int count = slea.readInt();
            double a = effect.getT();
//            player.changeCooldown(400021094, -((int) (count * a) * 1000));
//            player.getMap().broadcastMessage(CField.removeOrb(player.getId(), oid));
        }
    }
}
