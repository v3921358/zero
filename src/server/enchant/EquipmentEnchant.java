package server.enchant;

import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.GameConstants;
import constants.ServerConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;

import java.util.ArrayList;
import java.util.List;

public class EquipmentEnchant {

    public static int[] usejuhun = new int[4];

    public static int scrollType(String name) {
        if (name.contains("100%")) {
            return 0;
        } else if (name.contains("이노센트")) {
            return 4;
        } else if (name.contains("순백")) {
            return 5;
        } else if (name.contains("70%")) {
            return 1;
        } else if (name.contains("30%")) {
            return 2;
        } else if (name.contains("15%")) {
            return 3;
        }
        return 0;
    }

    public static boolean isMagicWeapon(MapleWeaponType type) {
        switch (type) {
            case ESPLIMITER:
            case STAFF:
            case WAND:
            case PLANE:
            case MAGICGUNTLET:
                return true;
            default:
                return false;
        }
    }

    public static void checkEquipmentStats(MapleClient c, Equip equip) {

        boolean changed = false;

        if (equip == null) {
            return;
        }

        Equip item = (Equip) MapleItemInformationProvider.getInstance().getEquipById(equip.getItemId(), false);

        if (MapleItemInformationProvider.getInstance().getName(equip.getItemId()).startsWith("제네시스")) {
            item.setEnchantWatk((short) 0);
            item.setEnchantMatk((short) 0);
            item.setEnchantStr((short) 0);
            item.setEnchantDex((short) 0);
            item.setEnchantInt((short) 0);
            item.setEnchantLuk((short) 0);
            item.setEnchantWdef((short) 0);
            item.setEnchantMdef((short) 0);
            item.setEnchantHp((short) 0);
            item.setEnchantMp((short) 0);
            item.setEnchantAcc((short) 0);
            item.setEnchantAvoid((short) 0);
            return;
        }

        if ((equip.getEquipmentType() & 0x1700) == 0x1700) {
            return;
        }

        item.setStr(equip.getStr());
        item.setDex(equip.getDex());
        item.setInt(equip.getInt());
        item.setLuk(equip.getLuk());
        item.setHp(equip.getHp());
        item.setMp(equip.getMp());
        item.setWatk(equip.getWatk());
        item.setMatk(equip.getMatk());
        item.setWdef(equip.getWdef());
        item.setMdef(equip.getMdef());
        item.setAcc(equip.getAcc());
        item.setAvoid(equip.getAvoid());
        item.setFire(equip.getFire());

        int max = equip.getEnhance();

        while (item.getEnhance() < max) {
            StarForceStats statz = EquipmentEnchant.starForceStats(item);
            item.setEnchantBuff((short) 0);
            item.setEnhance((byte) (item.getEnhance() + 1));
            for (Pair<EnchantFlag, Integer> stat : statz.getStats()) {
                if (EnchantFlag.Watk.check(stat.left.getValue())) {
                    item.setEnchantWatk((short) (item.getEnchantWatk() + stat.right));
                }

                if (EnchantFlag.Matk.check(stat.left.getValue())) {
                    item.setEnchantMatk((short) (item.getEnchantMatk() + stat.right));
                }

                if (EnchantFlag.Str.check(stat.left.getValue())) {
                    item.setEnchantStr((short) (item.getEnchantStr() + stat.right));
                }

                if (EnchantFlag.Dex.check(stat.left.getValue())) {
                    item.setEnchantDex((short) (item.getEnchantDex() + stat.right));
                }

                if (EnchantFlag.Int.check(stat.left.getValue())) {
                    item.setEnchantInt((short) (item.getEnchantInt() + stat.right));
                }

                if (EnchantFlag.Luk.check(stat.left.getValue())) {
                    item.setEnchantLuk((short) (item.getEnchantLuk() + stat.right));
                }

                if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                    item.setEnchantWdef((short) (item.getEnchantWdef() + stat.right));
                }

                if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                    item.setEnchantMdef((short) (item.getEnchantMdef() + stat.right));
                }

                if (EnchantFlag.Hp.check(stat.left.getValue())) {
                    item.setEnchantHp((short) (item.getEnchantHp() + stat.right));
                }

                if (EnchantFlag.Mp.check(stat.left.getValue())) {
                    item.setEnchantMp((short) (item.getEnchantMp() + stat.right));
                }

                if (EnchantFlag.Acc.check(stat.left.getValue())) {
                    item.setEnchantAcc((short) (item.getEnchantAcc() + stat.right));
                }

                if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                    item.setEnchantAvoid((short) (item.getEnchantAvoid() + stat.right));
                }
            }
        }

        if (equip.getEnchantStr() != item.getEnchantStr()) {
            changed = true;
            equip.setEnchantStr(item.getEnchantStr());
        }

        if (equip.getEnchantDex() != item.getEnchantDex()) {
            changed = true;
            equip.setEnchantDex(item.getEnchantDex());
        }

        if (equip.getEnchantInt() != item.getEnchantInt()) {
            changed = true;
            equip.setEnchantInt(item.getEnchantInt());
        }

        if (equip.getEnchantLuk() != item.getEnchantLuk()) {
            changed = true;
            equip.setEnchantLuk(item.getEnchantLuk());
        }

        if (equip.getEnchantHp() != item.getEnchantHp()) {
            changed = true;
            equip.setEnchantHp(item.getEnchantHp());
        }

        if (equip.getEnchantMp() != item.getEnchantMp()) {
            changed = true;
            equip.setEnchantMp(item.getEnchantMp());
        }

        if (equip.getEnchantWatk() != item.getEnchantWatk()) {
            changed = true;
            equip.setEnchantWatk(item.getEnchantWatk());
        }

        if (equip.getEnchantMatk() != item.getEnchantMatk()) {
            changed = true;
            equip.setEnchantMatk(item.getEnchantMatk());
        }

        if (equip.getEnchantWdef() != item.getEnchantWdef()) {
            changed = true;
            equip.setEnchantWdef(item.getEnchantWdef());
        }

        if (equip.getEnchantMdef() != item.getEnchantMdef()) {
            changed = true;
            equip.setEnchantMdef(item.getEnchantMdef());
        }

        if (equip.getEnchantAcc() != item.getEnchantAcc()) {
            changed = true;
            equip.setEnchantAcc(item.getEnchantAcc());
        }

        if (equip.getEnchantAvoid() != item.getEnchantAvoid()) {
            changed = true;
            equip.setEnchantAvoid(item.getEnchantAvoid());
        }

        if (changed && c != null) {
            c.getSession().writeAndFlush(CField.getGameMessage(6, "스타포스 수치로 인한 손해를 복구했습니다."));
        }
    }

    public static List<EquipmentScroll> equipmentScrolls(Equip equip) {
        List<EquipmentScroll> ess = new ArrayList<>();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        int reqLevel = ii.getReqLevel(equip.getItemId());
        MapleWeaponType weaponType = GameConstants.getWeaponType(equip.getItemId());

        List<Pair<EnchantFlag, Integer>> stats = new ArrayList<>();

        setJuhun(reqLevel, GameConstants.isWeapon(equip.getItemId()));

        if (equip.getUpgradeSlots() > 0) {

            if (GameConstants.isWeapon(equip.getItemId())) {

                //magic
                if (isMagicWeapon(weaponType)) {
                    if (reqLevel < 80) {
                        stats.add(new Pair<>(EnchantFlag.Matk, 1));
                    } else if (reqLevel < 120) {
                        stats.add(new Pair<>(EnchantFlag.Matk, 2));
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Matk, 3));
                    }

                    ess.add(new EquipmentScroll("100% 마력 주문서", usejuhun[0], stats));

                    stats.clear();

                    if (reqLevel < 80) {
                        stats.add(new Pair<>(EnchantFlag.Matk, 2));
                        ess.add(new EquipmentScroll("70% 마력 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 3));
                        stats.add(new Pair<>(EnchantFlag.Int, 1));
                        ess.add(new EquipmentScroll("30% 마력(지력) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 5));
                        stats.add(new Pair<>(EnchantFlag.Int, 2));
                        ess.add(new EquipmentScroll("15% 마력(지력) 주문서", usejuhun[3], stats));
                        stats.clear();
                    } else if (reqLevel < 120) {
                        stats.add(new Pair<>(EnchantFlag.Matk, 3));
                        stats.add(new Pair<>(EnchantFlag.Int, 1));
                        ess.add(new EquipmentScroll("70% 마력(지력) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 5));
                        stats.add(new Pair<>(EnchantFlag.Int, 2));
                        ess.add(new EquipmentScroll("30% 마력(지력) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 7));
                        stats.add(new Pair<>(EnchantFlag.Int, 3));
                        ess.add(new EquipmentScroll("15% 마력(지력) 주문서", usejuhun[3], stats));
                        stats.clear();
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Matk, 5));
                        stats.add(new Pair<>(EnchantFlag.Int, 2));
                        ess.add(new EquipmentScroll("70% 마력(지력) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 7));
                        stats.add(new Pair<>(EnchantFlag.Int, 3));
                        ess.add(new EquipmentScroll("30% 마력(지력) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 9));
                        stats.add(new Pair<>(EnchantFlag.Int, 4));
                        ess.add(new EquipmentScroll("15% 마력(지력) 주문서", usejuhun[3], stats));
                        stats.clear();
                    }

                } else { // melee
                    if (reqLevel < 80) {
                        stats.add(new Pair<>(EnchantFlag.Watk, 1));
                    } else if (reqLevel < 120) {
                        stats.add(new Pair<>(EnchantFlag.Watk, 2));
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                    }

                    ess.add(new EquipmentScroll("100% 공격력 주문서", usejuhun[0], stats));

                    stats.clear();

                    if (reqLevel < 80) {
                        stats.add(new Pair<>(EnchantFlag.Watk, 2));
                        ess.add(new EquipmentScroll("70% 공격력 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Str, 1));
                        ess.add(new EquipmentScroll("30% 공격력(힘) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Str, 2));
                        ess.add(new EquipmentScroll("15% 공격력(힘) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Dex, 1));
                        ess.add(new EquipmentScroll("30% 공격력(민첩) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Dex, 2));
                        ess.add(new EquipmentScroll("15% 공격력(민첩) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Luk, 1));
                        ess.add(new EquipmentScroll("30% 공격력(행운) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Luk, 2));
                        ess.add(new EquipmentScroll("15% 공격력(행운) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Hp, 50));
                        ess.add(new EquipmentScroll("30% 공격력(체력) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Hp, 100));
                        ess.add(new EquipmentScroll("15% 공격력(체력) 주문서", usejuhun[3], stats));
                        stats.clear();
                    } else if (reqLevel < 120) {
                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Str, 1));
                        ess.add(new EquipmentScroll("70% 공격력(힘) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Str, 2));
                        ess.add(new EquipmentScroll("30% 공격력(힘) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Str, 3));
                        ess.add(new EquipmentScroll("15% 공격력(힘) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Dex, 1));
                        ess.add(new EquipmentScroll("70% 공격력(민첩) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Dex, 2));
                        ess.add(new EquipmentScroll("30% 공격력(민첩) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Dex, 3));
                        ess.add(new EquipmentScroll("15% 공격력(민첩) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Luk, 1));
                        ess.add(new EquipmentScroll("70% 공격력(행운) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Luk, 2));
                        ess.add(new EquipmentScroll("30% 공격력(행운) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Luk, 3));
                        ess.add(new EquipmentScroll("15% 공격력(행운) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        stats.add(new Pair<>(EnchantFlag.Hp, 50));
                        ess.add(new EquipmentScroll("70% 공격력(체력) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Hp, 100));
                        ess.add(new EquipmentScroll("30% 공격력(체력) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Hp, 150));
                        ess.add(new EquipmentScroll("15% 공격력(체력) 주문서", usejuhun[3], stats));
                        stats.clear();
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Str, 2));
                        ess.add(new EquipmentScroll("70% 공격력(힘) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Str, 3));
                        ess.add(new EquipmentScroll("30% 공격력(힘) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 9));
                        stats.add(new Pair<>(EnchantFlag.Str, 4));
                        ess.add(new EquipmentScroll("15% 공격력(힘) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Dex, 2));
                        ess.add(new EquipmentScroll("70% 공격력(민첩) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Dex, 3));
                        ess.add(new EquipmentScroll("30% 공격력(민첩) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 9));
                        stats.add(new Pair<>(EnchantFlag.Dex, 4));
                        ess.add(new EquipmentScroll("15% 공격력(민첩) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Luk, 2));
                        ess.add(new EquipmentScroll("70% 공격력(행운) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Luk, 3));
                        ess.add(new EquipmentScroll("30% 공격력(행운) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 9));
                        stats.add(new Pair<>(EnchantFlag.Luk, 4));
                        ess.add(new EquipmentScroll("15% 공격력(행운) 주문서", usejuhun[3], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 5));
                        stats.add(new Pair<>(EnchantFlag.Hp, 100));
                        ess.add(new EquipmentScroll("70% 공격력(체력) 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 7));
                        stats.add(new Pair<>(EnchantFlag.Hp, 150));
                        ess.add(new EquipmentScroll("30% 공격력(체력) 주문서", usejuhun[2], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 9));
                        stats.add(new Pair<>(EnchantFlag.Hp, 200));
                        ess.add(new EquipmentScroll("15% 공격력(체력) 주문서", usejuhun[3], stats));
                        stats.clear();
                    }
                }
            } else if (equip.getItemId() / 10000 == 108) {
                if (reqLevel < 80) {
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("100% 방어력 주문서", usejuhun[0], stats));
                    stats.clear();

                    if (ii.getReqJob(equip.getItemId()) == 2) { // 법사 장갑
                        stats.add(new Pair<>(EnchantFlag.Matk, 1));
                        ess.add(new EquipmentScroll("70% 마력 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 2));
                        ess.add(new EquipmentScroll("30% 마력 주문서", usejuhun[2], stats));
                        stats.clear();
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, 1));
                        ess.add(new EquipmentScroll("70% 공격력 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 2));
                        ess.add(new EquipmentScroll("30% 공격력 주문서", usejuhun[2], stats));
                        stats.clear();
                    }
                } else {
                    if (ii.getReqJob(equip.getItemId()) == 2) { // 법사 장갑
                        stats.add(new Pair<>(EnchantFlag.Matk, 1));
                        ess.add(new EquipmentScroll("100% 마력 주문서", usejuhun[0], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 2));
                        ess.add(new EquipmentScroll("70% 마력 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Matk, 1));
                        ess.add(new EquipmentScroll("30% 마력 주문서", usejuhun[2], stats));
                        stats.clear();
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, 1));
                        ess.add(new EquipmentScroll("100% 공격력 주문서", usejuhun[0], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 2));
                        ess.add(new EquipmentScroll("70% 공격력 주문서", usejuhun[1], stats));
                        stats.clear();

                        stats.add(new Pair<>(EnchantFlag.Watk, 3));
                        ess.add(new EquipmentScroll("30% 공격력 주문서", usejuhun[2], stats));
                        stats.clear();
                    }
                }
            } else if (equip.getItemId() / 10000 == 111 || equip.getItemId() / 10000 == 112 || equip.getItemId() / 10000 == 113 || equip.getItemId() / 10000 == 103) { // 반지, 펜던트, 벨트, 귀고리
                if (reqLevel < 80) {
                    stats.add(new Pair<>(EnchantFlag.Str, 1));
                    ess.add(new EquipmentScroll("100% 힘 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 2));
                    ess.add(new EquipmentScroll("70% 힘 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 3));
                    ess.add(new EquipmentScroll("30% 힘 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 1));
                    ess.add(new EquipmentScroll("100% 민첩 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 2));
                    ess.add(new EquipmentScroll("70% 민첩 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 3));
                    ess.add(new EquipmentScroll("30% 민첩 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 1));
                    ess.add(new EquipmentScroll("100% 지력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 2));
                    ess.add(new EquipmentScroll("70% 지력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 3));
                    ess.add(new EquipmentScroll("30% 지력 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 1));
                    ess.add(new EquipmentScroll("100% 행운 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 2));
                    ess.add(new EquipmentScroll("70% 행운 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 3));
                    ess.add(new EquipmentScroll("30% 행운 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 50));
                    ess.add(new EquipmentScroll("100% 체력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 100));
                    ess.add(new EquipmentScroll("70% 체력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 150));
                    ess.add(new EquipmentScroll("30% 체력 주문서", usejuhun[2], stats));
                    stats.clear();
                } else if (reqLevel < 120) {
                    stats.add(new Pair<>(EnchantFlag.Str, 1));
                    ess.add(new EquipmentScroll("100% 힘 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 2));
                    ess.add(new EquipmentScroll("70% 힘 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 4));
                    ess.add(new EquipmentScroll("30% 힘 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 1));
                    ess.add(new EquipmentScroll("100% 민첩 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 2));
                    ess.add(new EquipmentScroll("70% 민첩 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 4));
                    ess.add(new EquipmentScroll("30% 민첩 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 1));
                    ess.add(new EquipmentScroll("100% 지력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 2));
                    ess.add(new EquipmentScroll("70% 지력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 4));
                    ess.add(new EquipmentScroll("30% 지력 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 1));
                    ess.add(new EquipmentScroll("100% 행운 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 2));
                    ess.add(new EquipmentScroll("70% 행운 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 4));
                    ess.add(new EquipmentScroll("30% 행운 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 50));
                    ess.add(new EquipmentScroll("100% 체력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 100));
                    ess.add(new EquipmentScroll("70% 체력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 200));
                    ess.add(new EquipmentScroll("30% 체력 주문서", usejuhun[2], stats));
                    stats.clear();
                } else {
                    stats.add(new Pair<>(EnchantFlag.Str, 2));
                    ess.add(new EquipmentScroll("100% 힘 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 3));
                    ess.add(new EquipmentScroll("70% 힘 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 5));
                    ess.add(new EquipmentScroll("30% 힘 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 2));
                    ess.add(new EquipmentScroll("100% 민첩 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 3));
                    ess.add(new EquipmentScroll("70% 민첩 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 5));
                    ess.add(new EquipmentScroll("30% 민첩 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 2));
                    ess.add(new EquipmentScroll("100% 지력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 3));
                    ess.add(new EquipmentScroll("70% 지력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 5));
                    ess.add(new EquipmentScroll("30% 지력 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 2));
                    ess.add(new EquipmentScroll("100% 행운 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 3));
                    ess.add(new EquipmentScroll("70% 행운 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 5));
                    ess.add(new EquipmentScroll("30% 행운 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 100));
                    ess.add(new EquipmentScroll("100% 체력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 150));
                    ess.add(new EquipmentScroll("70% 체력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 250));
                    ess.add(new EquipmentScroll("30% 체력 주문서", usejuhun[2], stats));
                    stats.clear();
                }
            } else {
                if (reqLevel < 80) {
                    stats.add(new Pair<>(EnchantFlag.Str, 1));
                    stats.add(new Pair<>(EnchantFlag.Hp, 5));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 1));
                    ess.add(new EquipmentScroll("100% 힘 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 15));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("70% 힘 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("30% 힘 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 1));
                    stats.add(new Pair<>(EnchantFlag.Hp, 5));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 1));
                    ess.add(new EquipmentScroll("100% 민첩 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 15));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("70% 민첩 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("30% 민첩 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 1));
                    stats.add(new Pair<>(EnchantFlag.Hp, 5));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 1));
                    ess.add(new EquipmentScroll("100% 지력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 15));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("70% 지력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("30% 지력 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 1));
                    stats.add(new Pair<>(EnchantFlag.Hp, 5));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 1));
                    ess.add(new EquipmentScroll("100% 행운 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 15));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("70% 행운 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("30% 행운 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 55));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 1));
                    ess.add(new EquipmentScroll("100% 체력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 115));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("70% 체력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 180));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("30% 체력 주문서", usejuhun[2], stats));
                    stats.clear();
                } else if (reqLevel < 120) {
                    stats.add(new Pair<>(EnchantFlag.Str, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 20));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("100% 힘 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 40));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 4));
                    ess.add(new EquipmentScroll("70% 힘 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 5));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 7));
                    ess.add(new EquipmentScroll("30% 힘 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 20));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("100% 민첩 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 40));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 4));
                    ess.add(new EquipmentScroll("70% 민첩 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 5));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 7));
                    ess.add(new EquipmentScroll("30% 민첩 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 20));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("100% 지력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 40));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 4));
                    ess.add(new EquipmentScroll("70% 지력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 5));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 7));
                    ess.add(new EquipmentScroll("30% 지력 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 2));
                    stats.add(new Pair<>(EnchantFlag.Hp, 20));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("100% 행운 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 40));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 4));
                    ess.add(new EquipmentScroll("70% 행운 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 5));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 7));
                    ess.add(new EquipmentScroll("30% 행운 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 120));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 2));
                    ess.add(new EquipmentScroll("100% 체력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 190));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 4));
                    ess.add(new EquipmentScroll("70% 체력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 320));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 7));
                    ess.add(new EquipmentScroll("30% 체력 주문서", usejuhun[2], stats));
                    stats.clear();
                } else {
                    stats.add(new Pair<>(EnchantFlag.Str, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("100% 힘 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 4));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 5));
                    ess.add(new EquipmentScroll("70% 힘 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Str, 7));
                    stats.add(new Pair<>(EnchantFlag.Hp, 120));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 10));
                    ess.add(new EquipmentScroll("30% 힘 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("100% 민첩 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 4));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 5));
                    ess.add(new EquipmentScroll("70% 민첩 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Dex, 7));
                    stats.add(new Pair<>(EnchantFlag.Hp, 120));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 10));
                    ess.add(new EquipmentScroll("30% 민첩 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("100% 지력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 4));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 5));
                    ess.add(new EquipmentScroll("70% 지력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Int, 7));
                    stats.add(new Pair<>(EnchantFlag.Hp, 120));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 10));
                    ess.add(new EquipmentScroll("30% 지력 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 3));
                    stats.add(new Pair<>(EnchantFlag.Hp, 30));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("100% 행운 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 4));
                    stats.add(new Pair<>(EnchantFlag.Hp, 70));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 5));
                    ess.add(new EquipmentScroll("70% 행운 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Luk, 7));
                    stats.add(new Pair<>(EnchantFlag.Hp, 120));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 10));
                    ess.add(new EquipmentScroll("30% 행운 주문서", usejuhun[2], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 180));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 3));
                    ess.add(new EquipmentScroll("100% 체력 주문서", usejuhun[0], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 270));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 5));
                    ess.add(new EquipmentScroll("70% 체력 주문서", usejuhun[1], stats));
                    stats.clear();

                    stats.add(new Pair<>(EnchantFlag.Hp, 470));
                    stats.add(new Pair<>(EnchantFlag.Wdef, 10));
                    ess.add(new EquipmentScroll("30% 체력 주문서", usejuhun[2], stats));
                    stats.clear();
                }
            }
        }

        ess.add(new EquipmentScroll("이노센트 주문서 30%", 5000, stats));
        ess.add(new EquipmentScroll("아크 이노센트 주문서 30%", 10000, stats));

        if (equip.getViciousHammer() == 0) { // 황금망치 안 썼을 때
            if (equip.getUpgradeSlots() < ii.getSlots(equip.getItemId())) { // 완작이 아니라면
                ess.add(new EquipmentScroll("순백의 주문서 5%", 3000, stats));
            }
        } else {
            if (equip.getUpgradeSlots() < ii.getSlots(equip.getItemId()) + 1) { // 완작이 아니라면
                ess.add(new EquipmentScroll("순백의 주문서 5%", 3000, stats));
            }
        }
        return ess;
    }

    public static int addExtra(int enhance) {
        int extra = 0;
        if (enhance < 5) {
            for (int i = 0; i < enhance; ++i) {
                extra += (i + 1);
            }
        } else if (enhance < 10) {
            for (int i = 0; i < (enhance - 5); ++i) {
                extra += 1;
            }
        } else if (enhance < 15) {
            for (int i = 0; i < (enhance - 10); ++i) {
                extra += 2;
            }
        }
        return extra;
    }

    public static StarForceStats starForceStats(Equip item) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int reqLevel = ii.getReqLevel(item.getItemId());
        List<Pair<EnchantFlag, Integer>> stats = new ArrayList<>();
        MapleWeaponType weaponType = GameConstants.getWeaponType(item.getItemId());

        if (ii.isSuperial(item.getItemId()).right) {
            if (item.getEnhance() >= 5 && item.getEnhance() < 10) {
                if (ii.isSuperial(item.getItemId()).getLeft().equals("Helisium")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 3 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 3 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("Nova")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 6 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 6 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("Tilent")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 9 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 9 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("MindPendent")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 9 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 9 + addExtra(item.getEnhance())));
                }
            } else if (item.getEnhance() >= 10 && item.getEnhance() < 15) {
                if (ii.isSuperial(item.getItemId()).getLeft().equals("Helisium")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 9 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 9 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("Nova")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 12 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 12 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("Tilent")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 15 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 15 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("MindPendent")) {
                    stats.add(new Pair<>(EnchantFlag.Watk, 15 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Matk, 15 + addExtra(item.getEnhance())));
                }
            } else {
                if (ii.isSuperial(item.getItemId()).getLeft().equals("Helisium")) {
                    stats.add(new Pair<>(EnchantFlag.Str, 5 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Dex, 5 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Int, 5 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Luk, 5 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("Nova")) {
                    stats.add(new Pair<>(EnchantFlag.Str, 10 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Dex, 10 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Int, 10 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Luk, 10 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("Tilent")) {
                    stats.add(new Pair<>(EnchantFlag.Str, 19 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Dex, 19 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Int, 19 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Luk, 19 + addExtra(item.getEnhance())));
                } else if (ii.isSuperial(item.getItemId()).getLeft().equals("MindPendent")) {
                    stats.add(new Pair<>(EnchantFlag.Str, 19 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Dex, 19 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Int, 19 + addExtra(item.getEnhance())));
                    stats.add(new Pair<>(EnchantFlag.Luk, 19 + addExtra(item.getEnhance())));
                }
            }
        } else {
            if (item.getEnhance() < 5) {
                stats.add(new Pair<>(EnchantFlag.Str, 2));
                stats.add(new Pair<>(EnchantFlag.Dex, 2));
                stats.add(new Pair<>(EnchantFlag.Int, 2));
                stats.add(new Pair<>(EnchantFlag.Luk, 2));
                if (item.getItemId() / 10000 == 108 && item.getEnhance() == 4) {
                    if (ii.getReqJob(item.getItemId()) == 2) {
                        stats.add(new Pair<>(EnchantFlag.Matk, 1));
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, 1));
                    }
                } else if (GameConstants.isWeapon(item.getItemId())) {
                    int ordinary;
                    if (isMagicWeapon(weaponType)) {
                        ordinary = item.getMatk();
                    } else {
                        ordinary = item.getWatk();
                    }

                    //환불 값을 '제외'한 무기의 공격력/마력이므로 지워주자...
                    if (item.getFire() > 0) {
                        long fire1 = (item.getFire() % 1000 / 10);
                        long fire2 = (item.getFire() % 1000000 / 10000);
                        long fire3 = (item.getFire() % 1000000000 / 10000000);
                        long fire4 = (item.getFire() % 1000000000000L / 10000000000L);
                        for (int i = 0; i < 4; ++i) {
                            int dat = (int) (i == 0 ? fire1 : i == 1 ? fire2 : i == 2 ? fire3 : fire4);
                            if (dat == (isMagicWeapon(weaponType) ? 18 : 17)) {
                                int value;
                                if (i == 0) {
                                    value = (int) (item.getFire() % 10 / 1);
                                } else if (i == 1) {
                                    value = (int) (item.getFire() % 10000 / 1000);
                                } else if (i == 2) {
                                    value = (int) (item.getFire() % 10000000 / 1000000);
                                } else {
                                    value = (int) (item.getFire() % 10000000000L / 1000000000L);
                                }
                                switch (value) {
                                    case 3:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 1200) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 1500) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 1800) / 10000) + 1));
                                        }
                                        break;
                                    case 4:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 1760) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 2200) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 2640) / 10000) + 1));
                                        }
                                        break;
                                    case 5:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 2420) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 3025) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 3630) / 10000) + 1));
                                        }
                                        break;
                                    case 6:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 3200) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 4000) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 4800) / 10000) + 1));
                                        }
                                        break;
                                    case 7:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 4100) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 5125) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 6150) / 10000) + 1));
                                        }
                                        break;
                                }
                            }
                        }
                    }

                    if (isMagicWeapon(weaponType)) {
                        stats.add(new Pair<>(EnchantFlag.Matk, ordinary / 50 + 1));
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, ordinary / 50 + 1));
                    }
                }
            } else if (item.getEnhance() < 15) {
                stats.add(new Pair<>(EnchantFlag.Str, 3));
                stats.add(new Pair<>(EnchantFlag.Dex, 3));
                stats.add(new Pair<>(EnchantFlag.Int, 3));
                stats.add(new Pair<>(EnchantFlag.Luk, 3));
                if (item.getItemId() / 10000 == 108) {
                    if (item.getEnhance() == 6 || item.getEnhance() == 8 || item.getEnhance() == 10 || item.getEnhance() >= 12) {
                        if (ii.getReqJob(item.getItemId()) == 2) {
                            stats.add(new Pair<>(EnchantFlag.Matk, 1));
                        } else {
                            stats.add(new Pair<>(EnchantFlag.Watk, 1));
                        }
                    }
                } else if (GameConstants.isWeapon(item.getItemId())) {
                    int ordinary;
                    if (isMagicWeapon(weaponType)) {
                        ordinary = item.getMatk();
                    } else {
                        ordinary = item.getWatk();
                    }

                    //환불 값을 '제외'한 무기의 공격력/마력이므로 지워주자...
                    if (item.getFire() > 0) {
                        long fire1 = (item.getFire() % 1000 / 10);
                        long fire2 = (item.getFire() % 1000000 / 10000);
                        long fire3 = (item.getFire() % 1000000000 / 10000000);
                        long fire4 = (item.getFire() % 1000000000000L / 10000000000L);
                        for (int i = 0; i < 4; ++i) {
                            int dat = (int) (i == 0 ? fire1 : i == 1 ? fire2 : i == 2 ? fire3 : fire4);
                            if (dat == (isMagicWeapon(weaponType) ? 18 : 17)) {
                                int value;
                                if (i == 0) {
                                    value = (int) (item.getFire() % 10 / 1);
                                } else if (i == 1) {
                                    value = (int) (item.getFire() % 10000 / 1000);
                                } else if (i == 2) {
                                    value = (int) (item.getFire() % 10000000 / 1000000);
                                } else {
                                    value = (int) (item.getFire() % 10000000000L / 1000000000L);
                                }
                                switch (value) {
                                    case 3:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 1200) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 1500) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 1800) / 10000) + 1));
                                        }
                                        break;
                                    case 4:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 1760) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 2200) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 2640) / 10000) + 1));
                                        }
                                        break;
                                    case 5:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 2420) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 3025) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 3630) / 10000) + 1));
                                        }
                                        break;
                                    case 6:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 3200) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 4000) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 4800) / 10000) + 1));
                                        }
                                        break;
                                    case 7:
                                        if (reqLevel <= 150) {
                                            ordinary -= ((short) (((ordinary * 4100) / 10000) + 1));
                                        } else if (reqLevel <= 160) {
                                            ordinary -= ((short) (((ordinary * 5125) / 10000) + 1));
                                        } else {
                                            ordinary -= ((short) (((ordinary * 6150) / 10000) + 1));
                                        }
                                        break;
                                }
                            }
                        }
                    }

                    if (isMagicWeapon(weaponType)) {
                        stats.add(new Pair<>(EnchantFlag.Matk, ordinary / 50 + 1));
                    } else {
                        stats.add(new Pair<>(EnchantFlag.Watk, ordinary / 50 + 1));
                    }
                }
            } else {
                if (reqLevel < 140) {
                    stats.add(new Pair<>(EnchantFlag.Str, 7));
                    stats.add(new Pair<>(EnchantFlag.Dex, 7));
                    stats.add(new Pair<>(EnchantFlag.Int, 7));
                    stats.add(new Pair<>(EnchantFlag.Luk, 7));
                } else if (reqLevel < 150) {
                    stats.add(new Pair<>(EnchantFlag.Str, 9));
                    stats.add(new Pair<>(EnchantFlag.Dex, 9));
                    stats.add(new Pair<>(EnchantFlag.Int, 9));
                    stats.add(new Pair<>(EnchantFlag.Luk, 9));
                } else if (reqLevel < 160) {
                    stats.add(new Pair<>(EnchantFlag.Str, 11));
                    stats.add(new Pair<>(EnchantFlag.Dex, 11));
                    stats.add(new Pair<>(EnchantFlag.Int, 11));
                    stats.add(new Pair<>(EnchantFlag.Luk, 11));
                } else if (reqLevel < 200) {
                    stats.add(new Pair<>(EnchantFlag.Str, 13));
                    stats.add(new Pair<>(EnchantFlag.Dex, 13));
                    stats.add(new Pair<>(EnchantFlag.Int, 13));
                    stats.add(new Pair<>(EnchantFlag.Luk, 13));
                } else {
                    stats.add(new Pair<>(EnchantFlag.Str, 15));
                    stats.add(new Pair<>(EnchantFlag.Dex, 15));
                    stats.add(new Pair<>(EnchantFlag.Int, 15));
                    stats.add(new Pair<>(EnchantFlag.Luk, 15));
                }

                if (GameConstants.isWeapon(item.getItemId())) {
                    if (reqLevel < 140) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 6));
                                break;
                            case 16:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 7));
                                break;
                            case 17:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 7));
                                break;
                            case 18:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 8));
                                break;
                            case 19:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 9));
                                break;
                        }
                    } else if (reqLevel < 150) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 7));
                                break;
                            case 16:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 8));
                                break;
                            case 17:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 8));
                                break;
                            case 18:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 9));
                                break;
                            case 19:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 10));
                                break;
                            case 20:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 11));
                                break;
                            case 21:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 12));
                                break;
                            case 22:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 13));
                                break;
                            case 23:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 14));
                                break;
                            case 24:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 15));
                                break;
                        }
                    } else if (reqLevel < 160) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 8));
                                break;
                            case 16:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 9));
                                break;
                            case 17:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 9));
                                break;
                            case 18:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 10));
                                break;
                            case 19:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 11));
                                break;
                            case 20:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 12));
                                break;
                            case 21:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 13));
                                break;
                            case 22:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 14));
                                break;
                            case 23:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 15));
                                break;
                            case 24:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 16));
                                break;
                        }
                    } else if (reqLevel < 200) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 9));
                                break;
                            case 16:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 9));
                                break;
                            case 17:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 10));
                                break;
                            case 18:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 11));
                                break;
                            case 19:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 12));
                                break;
                            case 20:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 13));
                                break;
                            case 21:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 14));
                                break;
                            case 22:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 15));
                                break;
                            case 23:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 16));
                                break;
                            case 24:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 17));
                                break;
                        }
                    } else {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 13));
                                break;
                            case 16:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 13));
                                break;
                            case 17:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 14));
                                break;
                            case 18:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 14));
                                break;
                            case 19:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 15));
                                break;
                            case 20:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 16));
                                break;
                            case 21:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 17));
                                break;
                            case 22:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 18));
                                break;
                            case 23:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 19));
                                break;
                            case 24:
                                stats.add(new Pair<>(isMagicWeapon(weaponType) ? EnchantFlag.Matk : EnchantFlag.Watk, 20));
                                break;
                        }
                    }
                } else {
                    if (reqLevel < 140) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(EnchantFlag.Watk, 7));
                                stats.add(new Pair<>(EnchantFlag.Matk, 7));
                                break;
                            case 16:
                                stats.add(new Pair<>(EnchantFlag.Watk, 8));
                                stats.add(new Pair<>(EnchantFlag.Matk, 8));
                                break;
                            case 17:
                                stats.add(new Pair<>(EnchantFlag.Watk, 9));
                                stats.add(new Pair<>(EnchantFlag.Matk, 9));
                                break;
                            case 18:
                                stats.add(new Pair<>(EnchantFlag.Watk, 10));
                                stats.add(new Pair<>(EnchantFlag.Matk, 10));
                                break;
                            case 19:
                                stats.add(new Pair<>(EnchantFlag.Watk, 11));
                                stats.add(new Pair<>(EnchantFlag.Matk, 11));
                                break;
                        }
                    } else if (reqLevel < 150) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(EnchantFlag.Watk, 8));
                                stats.add(new Pair<>(EnchantFlag.Matk, 8));
                                break;
                            case 16:
                                stats.add(new Pair<>(EnchantFlag.Watk, 9));
                                stats.add(new Pair<>(EnchantFlag.Matk, 9));
                                break;
                            case 17:
                                stats.add(new Pair<>(EnchantFlag.Watk, 10));
                                stats.add(new Pair<>(EnchantFlag.Matk, 10));
                                break;
                            case 18:
                                stats.add(new Pair<>(EnchantFlag.Watk, 11));
                                stats.add(new Pair<>(EnchantFlag.Matk, 11));
                                break;
                            case 19:
                                stats.add(new Pair<>(EnchantFlag.Watk, 12));
                                stats.add(new Pair<>(EnchantFlag.Matk, 12));
                                break;
                            case 20:
                                stats.add(new Pair<>(EnchantFlag.Watk, 13));
                                stats.add(new Pair<>(EnchantFlag.Matk, 13));
                                break;
                            case 21:
                                stats.add(new Pair<>(EnchantFlag.Watk, 15));
                                stats.add(new Pair<>(EnchantFlag.Matk, 15));
                                break;
                            case 22:
                                stats.add(new Pair<>(EnchantFlag.Watk, 16));
                                stats.add(new Pair<>(EnchantFlag.Matk, 16));
                                break;
                            case 23:
                                stats.add(new Pair<>(EnchantFlag.Watk, 17));
                                stats.add(new Pair<>(EnchantFlag.Matk, 17));
                                break;
                            case 24:
                                stats.add(new Pair<>(EnchantFlag.Watk, 18));
                                stats.add(new Pair<>(EnchantFlag.Matk, 18));
                                break;
                        }
                    } else if (reqLevel < 160) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(EnchantFlag.Watk, 9));
                                stats.add(new Pair<>(EnchantFlag.Matk, 9));
                                break;
                            case 16:
                                stats.add(new Pair<>(EnchantFlag.Watk, 10));
                                stats.add(new Pair<>(EnchantFlag.Matk, 10));
                                break;
                            case 17:
                                stats.add(new Pair<>(EnchantFlag.Watk, 11));
                                stats.add(new Pair<>(EnchantFlag.Matk, 11));
                                break;
                            case 18:
                                stats.add(new Pair<>(EnchantFlag.Watk, 12));
                                stats.add(new Pair<>(EnchantFlag.Matk, 12));
                                break;
                            case 19:
                                stats.add(new Pair<>(EnchantFlag.Watk, 13));
                                stats.add(new Pair<>(EnchantFlag.Matk, 13));
                                break;
                            case 20:
                                stats.add(new Pair<>(EnchantFlag.Watk, 14));
                                stats.add(new Pair<>(EnchantFlag.Matk, 14));
                                break;
                            case 21:
                                stats.add(new Pair<>(EnchantFlag.Watk, 16));
                                stats.add(new Pair<>(EnchantFlag.Matk, 16));
                                break;
                            case 22:
                                stats.add(new Pair<>(EnchantFlag.Watk, 17));
                                stats.add(new Pair<>(EnchantFlag.Matk, 17));
                                break;
                            case 23:
                                stats.add(new Pair<>(EnchantFlag.Watk, 18));
                                stats.add(new Pair<>(EnchantFlag.Matk, 18));
                                break;
                            case 24:
                                stats.add(new Pair<>(EnchantFlag.Watk, 19));
                                stats.add(new Pair<>(EnchantFlag.Matk, 19));
                                break;
                        }
                    } else if (reqLevel < 200) {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(EnchantFlag.Watk, 10));
                                stats.add(new Pair<>(EnchantFlag.Matk, 10));
                                break;
                            case 16:
                                stats.add(new Pair<>(EnchantFlag.Watk, 11));
                                stats.add(new Pair<>(EnchantFlag.Matk, 11));
                                break;
                            case 17:
                                stats.add(new Pair<>(EnchantFlag.Watk, 12));
                                stats.add(new Pair<>(EnchantFlag.Matk, 12));
                                break;
                            case 18:
                                stats.add(new Pair<>(EnchantFlag.Watk, 13));
                                stats.add(new Pair<>(EnchantFlag.Matk, 13));
                                break;
                            case 19:
                                stats.add(new Pair<>(EnchantFlag.Watk, 14));
                                stats.add(new Pair<>(EnchantFlag.Matk, 14));
                                break;
                            case 20:
                                stats.add(new Pair<>(EnchantFlag.Watk, 15));
                                stats.add(new Pair<>(EnchantFlag.Matk, 15));
                                break;
                            case 21:
                                stats.add(new Pair<>(EnchantFlag.Watk, 17));
                                stats.add(new Pair<>(EnchantFlag.Matk, 17));
                                break;
                            case 22:
                                stats.add(new Pair<>(EnchantFlag.Watk, 18));
                                stats.add(new Pair<>(EnchantFlag.Matk, 18));
                                break;
                            case 23:
                                stats.add(new Pair<>(EnchantFlag.Watk, 19));
                                stats.add(new Pair<>(EnchantFlag.Matk, 19));
                                break;
                            case 24:
                                stats.add(new Pair<>(EnchantFlag.Watk, 20));
                                stats.add(new Pair<>(EnchantFlag.Matk, 20));
                                break;
                        }
                    } else {
                        switch (item.getEnhance()) {
                            case 15:
                                stats.add(new Pair<>(EnchantFlag.Watk, 12));
                                stats.add(new Pair<>(EnchantFlag.Matk, 12));
                                break;
                            case 16:
                                stats.add(new Pair<>(EnchantFlag.Watk, 13));
                                stats.add(new Pair<>(EnchantFlag.Matk, 13));
                                break;
                            case 17:
                                stats.add(new Pair<>(EnchantFlag.Watk, 14));
                                stats.add(new Pair<>(EnchantFlag.Matk, 14));
                                break;
                            case 18:
                                stats.add(new Pair<>(EnchantFlag.Watk, 15));
                                stats.add(new Pair<>(EnchantFlag.Matk, 15));
                                break;
                            case 19:
                                stats.add(new Pair<>(EnchantFlag.Watk, 16));
                                stats.add(new Pair<>(EnchantFlag.Matk, 16));
                                break;
                            case 20:
                                stats.add(new Pair<>(EnchantFlag.Watk, 17));
                                stats.add(new Pair<>(EnchantFlag.Matk, 17));
                                break;
                            case 21:
                                stats.add(new Pair<>(EnchantFlag.Watk, 19));
                                stats.add(new Pair<>(EnchantFlag.Matk, 19));
                                break;
                            case 22:
                                stats.add(new Pair<>(EnchantFlag.Watk, 20));
                                stats.add(new Pair<>(EnchantFlag.Matk, 20));
                                break;
                            case 23:
                                stats.add(new Pair<>(EnchantFlag.Watk, 21));
                                stats.add(new Pair<>(EnchantFlag.Matk, 21));
                                break;
                            case 24:
                                stats.add(new Pair<>(EnchantFlag.Watk, 22));
                                stats.add(new Pair<>(EnchantFlag.Matk, 22));
                                break;
                        }
                    }
                }
            }
        }

        return new StarForceStats(stats);
    }

    public static void handleEnchant(LittleEndianAccessor slea, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip item;
        byte type = slea.readByte();
        switch (type) {
            case 0: { //  주문의 흔적 : 강화 시도
                slea.skip(4); // tick
                short pos = slea.readShort();
                int index = slea.readInt();
                if (pos > 0) {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                } else {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                }

                Equip item2 = null;

                if (GameConstants.isZeroWeapon(item.getItemId())) {
                    item2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) (pos == -11 ? -10 : -11));
                }

                List<EquipmentScroll> ess = equipmentScrolls(item);

                if (ess.size() <= index) { // error or bug
                    return;
                }

                EquipmentScroll es = ess.get(index);

                int percent, success = 0;
                if (scrollType(es.getName()) == 4) {
                    percent = 30;
                } else if (scrollType(es.getName()) == 5) {
                    percent = 5;
                } else {
                    percent = Integer.parseInt(es.getName().split("%")[0]);
                }

                if (Randomizer.nextInt(1000) < percent * 10) {
                    success = 1;
                }

                if (c.getPlayer().isGM()) {
                    success = 1;
                }

                if (c.getPlayer().haveItem(4001832, es.getJuhun())) {
                    c.getPlayer().removeItem(4001832, -es.getJuhun());
                } else {
                    return;
                }

                Equip ordinary = (Equip) item.copy();

                if (success > 0) {
                    if (scrollType(es.getName()) == 4) {
                        Equip origin = (Equip) MapleItemInformationProvider.getInstance().getEquipById(item.getItemId(), false);

                        int reqLevel = MapleItemInformationProvider.getInstance().getReqLevel(item.getItemId());
                        int ordinaryPad = origin.getWatk() > 0 ? origin.getWatk() : origin.getMatk();
                        int ordinaryMad = origin.getMatk() > 0 ? origin.getMatk() : origin.getWatk();

                        origin.setState(ordinary.getState());
                        origin.setPotential1(ordinary.getPotential1());
                        origin.setPotential2(ordinary.getPotential2());
                        origin.setPotential3(ordinary.getPotential3());
                        origin.setPotential4(ordinary.getPotential4());
                        origin.setPotential5(ordinary.getPotential5());
                        origin.setPotential6(ordinary.getPotential6());
                        origin.setSoulEnchanter(ordinary.getSoulEnchanter());
                        origin.setSoulName(ordinary.getSoulName());
                        origin.setSoulPotential(ordinary.getSoulPotential());
                        origin.setSoulSkill(ordinary.getSoulSkill());
                        origin.setFire(ordinary.getFire());
                        origin.setKarmaCount(ordinary.getKarmaCount());
                        item.set(origin);
                        if (item2 != null) {
                            item2.set(origin);
                        }

                        int[] rebirth = new int[4];
                        String fire = String.valueOf(item.getFire());
                        if (fire.length() == 12) {
                            rebirth[0] = Integer.parseInt(fire.substring(0, 3));
                            rebirth[1] = Integer.parseInt(fire.substring(3, 6));
                            rebirth[2] = Integer.parseInt(fire.substring(6, 9));
                            rebirth[3] = Integer.parseInt(fire.substring(9));
                        } else if (fire.length() == 11) {
                            rebirth[0] = Integer.parseInt(fire.substring(0, 2));
                            rebirth[1] = Integer.parseInt(fire.substring(2, 5));
                            rebirth[2] = Integer.parseInt(fire.substring(5, 8));
                            rebirth[3] = Integer.parseInt(fire.substring(8));
                        } else if (fire.length() == 10) {
                            rebirth[0] = Integer.parseInt(fire.substring(0, 1));
                            rebirth[1] = Integer.parseInt(fire.substring(1, 4));
                            rebirth[2] = Integer.parseInt(fire.substring(4, 7));
                            rebirth[3] = Integer.parseInt(fire.substring(7));
                        }

                        if (fire.length() >= 10) {
                            for (int i = 0; i < 4; ++i) {
                                int randomOption = rebirth[i] / 10;
                                int randomValue = rebirth[i] - (rebirth[i] / 10 * 10);
                                item.setFireOption(randomOption, reqLevel, randomValue, ordinaryPad, ordinaryMad);
                                if (item2 != null) {
                                    item2.setFireOption(randomOption, reqLevel, randomValue, ordinaryPad, ordinaryMad);
                                }
                            }
                        }

                        item.setEquipmentType(0x1100);

                        if (es.getName().contains("아크")) { // 아크 이노는 스타포스 까지 다시 해줘야함.
                            for (int i = 0; i < ordinary.getEnhance(); ++i) {
                                StarForceStats stats = starForceStats(item);
                                item.setEnhance((byte) (item.getEnhance() + 1));
                                if (item2 != null) {
                                    item2.setEnhance((byte) (item2.getEnhance() + 1));
                                }
                                for (Pair<EnchantFlag, Integer> stat : stats.getStats()) {
                                    if (EnchantFlag.Watk.check(stat.left.getValue())) {
                                        item.setEnchantWatk((short) (item.getEnchantWatk() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantWatk((short) (item2.getEnchantWatk() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Matk.check(stat.left.getValue())) {
                                        item.setEnchantMatk((short) (item.getEnchantMatk() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantMatk((short) (item2.getEnchantMatk() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Str.check(stat.left.getValue())) {
                                        item.setEnchantStr((short) (item.getEnchantStr() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantStr((short) (item2.getEnchantStr() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Dex.check(stat.left.getValue())) {
                                        item.setEnchantDex((short) (item.getEnchantDex() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantDex((short) (item2.getEnchantDex() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Int.check(stat.left.getValue())) {
                                        item.setEnchantInt((short) (item.getEnchantInt() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantInt((short) (item2.getEnchantInt() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Luk.check(stat.left.getValue())) {
                                        item.setEnchantLuk((short) (item.getEnchantLuk() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantLuk((short) (item2.getEnchantLuk() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                                        item.setEnchantWdef((short) (item.getEnchantWdef() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantWdef((short) (item2.getEnchantWdef() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                                        item.setEnchantMdef((short) (item.getEnchantMdef() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantMdef((short) (item2.getEnchantMdef() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Hp.check(stat.left.getValue())) {
                                        item.setEnchantHp((short) (item.getEnchantHp() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantHp((short) (item2.getEnchantHp() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Mp.check(stat.left.getValue())) {
                                        item.setEnchantMp((short) (item.getEnchantMp() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantMp((short) (item2.getEnchantMp() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Acc.check(stat.left.getValue())) {
                                        item.setEnchantAcc((short) (item.getEnchantAcc() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantAcc((short) (item2.getEnchantAcc() + stat.right));
                                        }
                                    }

                                    if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                                        item.setEnchantAvoid((short) (item.getEnchantAvoid() + stat.right));
                                        if (item2 != null) {
                                            item2.setEnchantAvoid((short) (item2.getEnchantAvoid() + stat.right));
                                        }
                                    }
                                }
                            }
                        }
                    } else if (scrollType(es.getName()) == 5) {
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                        if (item2 != null) {
                            item2.setUpgradeSlots((byte) (item2.getUpgradeSlots() + 1));
                        }
                    } else {
                        item.setLevel((byte) (item.getLevel() + 1));
                        if (item2 != null) {
                            item2.setLevel((byte) (item2.getLevel() + 1));
                        }
                        if (EnchantFlag.Watk.check(es.getFlag())) {
                            item.setWatk((short) (item.getWatk() + es.getFlag(EnchantFlag.Watk).right));
                            if (item2 != null) {
                                item2.setWatk((short) (item2.getWatk() + es.getFlag(EnchantFlag.Watk).right));
                            }
                        }

                        if (EnchantFlag.Matk.check(es.getFlag())) {
                            item.setMatk((short) (item.getMatk() + es.getFlag(EnchantFlag.Matk).right));
                            if (item2 != null) {
                                item2.setMatk((short) (item2.getMatk() + es.getFlag(EnchantFlag.Matk).right));
                            }
                        }

                        if (EnchantFlag.Str.check(es.getFlag())) {
                            item.setStr((short) (item.getStr() + es.getFlag(EnchantFlag.Str).right));
                            if (item2 != null) {
                                item2.setStr((short) (item2.getStr() + es.getFlag(EnchantFlag.Str).right));
                            }
                        }

                        if (EnchantFlag.Dex.check(es.getFlag())) {
                            item.setDex((short) (item.getDex() + es.getFlag(EnchantFlag.Dex).right));
                            if (item2 != null) {
                                item2.setDex((short) (item2.getDex() + es.getFlag(EnchantFlag.Dex).right));
                            }
                        }

                        if (EnchantFlag.Int.check(es.getFlag())) {
                            item.setInt((short) (item.getInt() + es.getFlag(EnchantFlag.Int).right));
                            if (item2 != null) {
                                item2.setInt((short) (item2.getInt() + es.getFlag(EnchantFlag.Int).right));
                            }
                        }

                        if (EnchantFlag.Luk.check(es.getFlag())) {
                            item.setLuk((short) (item.getLuk() + es.getFlag(EnchantFlag.Luk).right));
                            if (item2 != null) {
                                item2.setLuk((short) (item2.getLuk() + es.getFlag(EnchantFlag.Luk).right));
                            }
                        }

                        if (EnchantFlag.Wdef.check(es.getFlag())) {
                            item.setWdef((short) (item.getWdef() + es.getFlag(EnchantFlag.Wdef).right));
                            if (item2 != null) {
                                item2.setWdef((short) (item2.getWdef() + es.getFlag(EnchantFlag.Wdef).right));
                            }
                        }

                        if (EnchantFlag.Mdef.check(es.getFlag())) {
                            item.setMdef((short) (item.getMdef() + es.getFlag(EnchantFlag.Mdef).right));
                            if (item2 != null) {
                                item2.setMdef((short) (item2.getMdef() + es.getFlag(EnchantFlag.Mdef).right));
                            }
                        }

                        if (EnchantFlag.Hp.check(es.getFlag())) {
                            item.setHp((short) (item.getHp() + es.getFlag(EnchantFlag.Hp).right));
                            if (item2 != null) {
                                item2.setHp((short) (item2.getHp() + es.getFlag(EnchantFlag.Hp).right));
                            }
                        }

                        if (EnchantFlag.Mp.check(es.getFlag())) {
                            item.setMp((short) (item.getMp() + es.getFlag(EnchantFlag.Mp).right));
                            if (item2 != null) {
                                item2.setMp((short) (item2.getMp() + es.getFlag(EnchantFlag.Mp).right));
                            }
                        }

                        if (EnchantFlag.Acc.check(es.getFlag())) {
                            item.setAcc((short) (item.getAcc() + es.getFlag(EnchantFlag.Acc).right));
                            if (item2 != null) {
                                item2.setAcc((short) (item2.getAcc() + es.getFlag(EnchantFlag.Acc).right));
                            }
                        }

                        if (EnchantFlag.Avoid.check(es.getFlag())) {
                            item.setAvoid((short) (item.getAvoid() + es.getFlag(EnchantFlag.Avoid).right));
                            if (item2 != null) {
                                item2.setAvoid((short) (item2.getAvoid() + es.getFlag(EnchantFlag.Avoid).right));
                            }
                        }

                        //장갑을 제외한 방어구는 4작에서 공 or 마 +1
                        if (!GameConstants.isWeapon(item.getItemId()) && item.getItemId() / 10000 != 108 && item.getLevel() == 4) {
                            if (ii.getReqJob(item.getItemId()) == 2) {
                                item.addMatk((short) 1);
                                if (item2 != null) {
                                    item2.addMatk((short) 1);
                                }
                            } else {
                                item.addWatk((short) 1);
                                if (item2 != null) {
                                    item2.addWatk((short) 1);
                                }
                            }
                        }
                    }
                }

                if (scrollType(es.getName()) <= 3) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() - 1));
                    if (item2 != null) {
                        item2.setUpgradeSlots((byte) (item2.getUpgradeSlots() - 1));
                    }
                }

                checkEquipmentStats(c, item);
                if (item2 != null) {
                    checkEquipmentStats(c, item2);
                }

                c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(100, ordinary, item, es, null, success));
                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));

                if (item2 != null) {
                    c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item2));
                }
                break;
            }
            case 1: { // 스타포스 : 강화 시도
                slea.skip(4);
                short pos = slea.readShort();
                if (pos > 0) {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                } else {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                }

                Equip item2 = null;

                if (GameConstants.isZeroWeapon(item.getItemId())) {
                    item2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) (pos == -11 ? -10 : -11));
                }

                byte catchStar = slea.readByte();
                if (catchStar == 1) {
                    slea.skip(4);
                }
                slea.readInt(); // 1
                slea.readInt(); // -1; 가횟?
                boolean shield = c.getPlayer().shield;

                Equip ordinary = (Equip) item.copy();

                Pair<Integer, Integer> per = starForcePercent(item);

                int success = per.left, destroy = per.right;

                long meso = StarForceMeso(item);

                double rate = (100 - ServerConstants.starForceSalePercent) / 100.0;

                meso *= rate;

                if (meso < 0) {
                    meso = meso & 0xFFFFFFFFL;
                }

                if (catchStar == 1) {
                    success += 45; // 4.5%라고 세간에 알려짐..
                }

                if (shield) { // 파괴방지
                    destroy = 0;
                }

                if (c.getPlayer().isGM()) {
                    success = 1000;
                }

                if (c.getPlayer().getMeso() < meso) {
                    c.getPlayer().dropMessage(1, "메소가 부족합니다.");
                    return;
                } else {
                    c.getPlayer().gainMeso(-meso, false);
                    if (shield) { // 파괴방지 메소 2배
                        c.getPlayer().gainMeso(-meso, false);
                    }
                }

                if ((item.getEnchantBuff() & 0x20) != 0) {
                    success = 1000;
                    destroy = 0;
                }

                StarForceStats stats;

                int result;

                if (Randomizer.nextInt(1000) < success) {
                    stats = starForceStats(item);
                    item.setEnhance((byte) (item.getEnhance() + 1));
                    if (item2 != null) {
                        item2.setEnhance((byte) (item2.getEnhance() + 1));
                    }
                    result = 1;
                    for (Pair<EnchantFlag, Integer> stat : stats.getStats()) {
                        if (EnchantFlag.Watk.check(stat.left.getValue())) {
                            item.setEnchantWatk((short) (item.getEnchantWatk() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantWatk((short) (item2.getEnchantWatk() + stat.right));
                            }
                        }

                        if (EnchantFlag.Matk.check(stat.left.getValue())) {
                            item.setEnchantMatk((short) (item.getEnchantMatk() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantMatk((short) (item2.getEnchantMatk() + stat.right));
                            }
                        }

                        if (EnchantFlag.Str.check(stat.left.getValue())) {
                            item.setEnchantStr((short) (item.getEnchantStr() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantStr((short) (item2.getEnchantStr() + stat.right));
                            }
                        }

                        if (EnchantFlag.Dex.check(stat.left.getValue())) {
                            item.setEnchantDex((short) (item.getEnchantDex() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantDex((short) (item2.getEnchantDex() + stat.right));
                            }
                        }

                        if (EnchantFlag.Int.check(stat.left.getValue())) {
                            item.setEnchantInt((short) (item.getEnchantInt() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantInt((short) (item2.getEnchantInt() + stat.right));
                            }
                        }

                        if (EnchantFlag.Luk.check(stat.left.getValue())) {
                            item.setEnchantLuk((short) (item.getEnchantLuk() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantLuk((short) (item2.getEnchantLuk() + stat.right));
                            }
                        }

                        if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                            item.setEnchantWdef((short) (item.getEnchantWdef() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantWdef((short) (item2.getEnchantWdef() + stat.right));
                            }
                        }

                        if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                            item.setEnchantMdef((short) (item.getEnchantMdef() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantMdef((short) (item2.getEnchantMdef() + stat.right));
                            }
                        }

                        if (EnchantFlag.Hp.check(stat.left.getValue())) {
                            item.setEnchantHp((short) (item.getEnchantHp() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantHp((short) (item2.getEnchantHp() + stat.right));
                            }
                        }

                        if (EnchantFlag.Mp.check(stat.left.getValue())) {
                            item.setEnchantMp((short) (item.getEnchantMp() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantMp((short) (item2.getEnchantMp() + stat.right));
                            }
                        }

                        if (EnchantFlag.Acc.check(stat.left.getValue())) {
                            item.setEnchantAcc((short) (item.getEnchantAcc() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantAcc((short) (item2.getEnchantAcc() + stat.right));
                            }
                        }

                        if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                            item.setEnchantAvoid((short) (item.getEnchantAvoid() + stat.right));
                            if (item2 != null) {
                                item2.setEnchantAvoid((short) (item2.getEnchantAvoid() + stat.right));
                            }
                        }
                    }

                    if ((item.getEnchantBuff() & 0x20) != 0) {
                        item.setEnchantBuff((short) (item.getEnchantBuff() - 0x20));
                        if (item2 != null && (item2.getEnchantBuff() & 0x20) != 0) {
                            item2.setEnchantBuff((short) (item2.getEnchantBuff() - 0x20));
                        }
                    }
                    if ((item.getEnchantBuff() & 0x10) != 0) {
                        item.setEnchantBuff((short) (item.getEnchantBuff() - 0x10));
                        if (item2 != null && (item2.getEnchantBuff() & 0x10) != 0) {
                            item2.setEnchantBuff((short) (item2.getEnchantBuff() - 0x10));
                        }
                    }

                    checkEquipmentStats(c, item);
                    if (item2 != null) {
                        checkEquipmentStats(c, item2);
                    }
                } else if (Randomizer.nextInt(1000) < destroy) {
                    result = 2;
                    while (item.getEnhance() > (ii.isSuperial(item.getItemId()).right ? 0 : 12)) {
                        item.setEnhance((byte) (item.getEnhance() - 1));
                        if (item2 != null) {
                            item2.setEnhance((byte) (item2.getEnhance() - 1));
                        }
                        stats = starForceStats(item); // enhance 값마다 달라지므로 계속해서 새로 로딩 해 줘야함.
                        for (Pair<EnchantFlag, Integer> stat : stats.getStats()) {
                            if (EnchantFlag.Watk.check(stat.left.getValue())) {
                                item.setEnchantWatk((short) (item.getEnchantWatk() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantWatk((short) (item2.getEnchantWatk() - stat.right));
                                }
                            }

                            if (EnchantFlag.Matk.check(stat.left.getValue())) {
                                item.setEnchantMatk((short) (item.getEnchantMatk() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantMatk((short) (item2.getEnchantMatk() - stat.right));
                                }
                            }

                            if (EnchantFlag.Str.check(stat.left.getValue())) {
                                item.setEnchantStr((short) (item.getEnchantStr() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantStr((short) (item2.getEnchantStr() - stat.right));
                                }
                            }

                            if (EnchantFlag.Dex.check(stat.left.getValue())) {
                                item.setEnchantDex((short) (item.getEnchantDex() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantDex((short) (item2.getEnchantDex() - stat.right));
                                }
                            }

                            if (EnchantFlag.Int.check(stat.left.getValue())) {
                                item.setEnchantInt((short) (item.getEnchantInt() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantInt((short) (item2.getEnchantInt() - stat.right));
                                }
                            }

                            if (EnchantFlag.Luk.check(stat.left.getValue())) {
                                item.setEnchantLuk((short) (item.getEnchantLuk() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantLuk((short) (item2.getEnchantLuk() - stat.right));
                                }
                            }

                            if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                                item.setEnchantWdef((short) (item.getEnchantWdef() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantWdef((short) (item2.getEnchantWdef() - stat.right));
                                }
                            }

                            if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                                item.setEnchantMdef((short) (item.getEnchantMdef() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantMdef((short) (item2.getEnchantMdef() - stat.right));
                                }
                            }

                            if (EnchantFlag.Hp.check(stat.left.getValue())) {
                                item.setEnchantHp((short) (item.getEnchantHp() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantHp((short) (item2.getEnchantHp() - stat.right));
                                }
                            }

                            if (EnchantFlag.Mp.check(stat.left.getValue())) {
                                item.setEnchantMp((short) (item.getEnchantMp() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantMp((short) (item2.getEnchantMp() - stat.right));
                                }
                            }

                            if (EnchantFlag.Acc.check(stat.left.getValue())) {
                                item.setEnchantAcc((short) (item.getEnchantAcc() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantAcc((short) (item2.getEnchantAcc() - stat.right));
                                }
                            }

                            if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                                item.setEnchantAvoid((short) (item.getEnchantAvoid() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantAvoid((short) (item2.getEnchantAvoid() - stat.right));
                                }
                            }
                        }
                    }
                    item.setEnchantBuff((short) 0x88); // 장비의 흔적 flag
                    if (item2 != null) {
                        item2.setEnchantBuff((short) 0x88); // 장비의 흔적 flag
                    }
                    checkEquipmentStats(c, item);
                    if (item2 != null) {
                        checkEquipmentStats(c, item2);
                    }
                } else {
                    if ((ii.isSuperial(item.getItemId()).right || item.getEnhance() > 10) && item.getEnhance() % 5 != 0) { // 5의 배수 구간, 10성 이하에서는 하락하지 않음.
                        result = 0;
                        item.setEnhance((byte) (item.getEnhance() - 1));
                        if (item2 != null) {
                            item2.setEnhance((byte) (item2.getEnhance() - 1));
                        }
                        stats = starForceStats(item);
                        for (Pair<EnchantFlag, Integer> stat : stats.getStats()) {
                            if (EnchantFlag.Watk.check(stat.left.getValue())) {
                                item.setEnchantWatk((short) (item.getEnchantWatk() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantWatk((short) (item2.getEnchantWatk() - stat.right));
                                }
                            }

                            if (EnchantFlag.Matk.check(stat.left.getValue())) {
                                item.setEnchantMatk((short) (item.getEnchantMatk() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantMatk((short) (item2.getEnchantMatk() - stat.right));
                                }
                            }

                            if (EnchantFlag.Str.check(stat.left.getValue())) {
                                item.setEnchantStr((short) (item.getEnchantStr() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantStr((short) (item2.getEnchantStr() - stat.right));
                                }
                            }

                            if (EnchantFlag.Dex.check(stat.left.getValue())) {
                                item.setEnchantDex((short) (item.getEnchantDex() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantDex((short) (item2.getEnchantDex() - stat.right));
                                }
                            }

                            if (EnchantFlag.Int.check(stat.left.getValue())) {
                                item.setEnchantInt((short) (item.getEnchantInt() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantInt((short) (item2.getEnchantInt() - stat.right));
                                }
                            }

                            if (EnchantFlag.Luk.check(stat.left.getValue())) {
                                item.setEnchantLuk((short) (item.getEnchantLuk() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantLuk((short) (item2.getEnchantLuk() - stat.right));
                                }
                            }

                            if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                                item.setEnchantWdef((short) (item.getEnchantWdef() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantWdef((short) (item2.getEnchantWdef() - stat.right));
                                }
                            }

                            if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                                item.setEnchantMdef((short) (item.getEnchantMdef() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantMdef((short) (item2.getEnchantMdef() - stat.right));
                                }
                            }

                            if (EnchantFlag.Hp.check(stat.left.getValue())) {
                                item.setEnchantHp((short) (item.getEnchantHp() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantHp((short) (item2.getEnchantHp() - stat.right));
                                }
                            }

                            if (EnchantFlag.Mp.check(stat.left.getValue())) {
                                item.setEnchantMp((short) (item.getEnchantMp() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantMp((short) (item2.getEnchantMp() - stat.right));
                                }
                            }

                            if (EnchantFlag.Acc.check(stat.left.getValue())) {
                                item.setEnchantAcc((short) (item.getEnchantAcc() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantAcc((short) (item2.getEnchantAcc() - stat.right));
                                }
                            }

                            if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                                item.setEnchantAvoid((short) (item.getEnchantAvoid() - stat.right));
                                if (item2 != null) {
                                    item2.setEnchantAvoid((short) (item2.getEnchantAvoid() - stat.right));
                                }
                            }
                        }

                        checkEquipmentStats(c, item);
                        if (item2 != null) {
                            checkEquipmentStats(c, item2);
                        }

                        if ((item.getEnchantBuff() & 0x10) != 0) {
                            item.setEnchantBuff((short) (item.getEnchantBuff() + 0x20));
                        } else {
                            item.setEnchantBuff((short) (item.getEnchantBuff() + 0x10));
                        }

                        if (item2 != null) {
                            if ((item2.getEnchantBuff() & 0x10) != 0) {
                                item2.setEnchantBuff((short) (item2.getEnchantBuff() + 0x20));
                            } else {
                                item2.setEnchantBuff((short) (item2.getEnchantBuff() + 0x10));
                            }
                        }
                    } else {
                        result = 3; // 유지
                    }
                }

                if (item.getEnchantBuff() >= 0x88 && item.getPosition() < 0) {
                    MapleInventoryManipulator.unequip(c, item.getPosition(), c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), MapleInventoryType.EQUIP);
                    if (item2 != null) { // 제로 무기 파괴시
                        MapleInventoryManipulator.unequip(c, item2.getPosition(), c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), null);

                        Item zw = MapleInventoryManipulator.addId_Item(c, 1572000, (short) 1, "", null, -1, "", false);
                        if (zw != null) {
                            MapleInventoryManipulator.equip(c, zw.getPosition(), (short) -11, null);
                        }

                        Item zw2 = MapleInventoryManipulator.addId_Item(c, 1562000, (short) 1, "", null, -1, "", false);
                        if (zw2 != null) {
                            MapleInventoryManipulator.equip(c, zw2.getPosition(), (short) -10, null);
                        }
                    }
                }

                c.getPlayer().shield = false;

                c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(101, ordinary, item, null, null, result));
                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));
                if (item2 != null) {
                    c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item2));
                }
                break;
            }
            case 2: { // 장비의 흔적 : 전승 시도
                slea.skip(4);

                short pos = slea.readShort();
                if (pos > 0) {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                } else {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                }

                Equip trace;
                pos = slea.readShort();
                if (pos > 0) {
                    trace = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                } else {
                    trace = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                }

                Equip ordinary = (Equip) trace.copy();

                trace.setEnchantBuff((short) 0x0);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, item.getPosition(), (short) 1, false);

                c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(101, ordinary, trace, null, null, 1));
                c.getSession().writeAndFlush(InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, trace));

                if (GameConstants.isZeroWeapon(trace.getItemId())) {
                    if (GameConstants.isAlphaWeapon(trace.getItemId())) {
                        MapleInventoryManipulator.equip(c, trace.getPosition(), (short) -11, null);
                    } else if (GameConstants.isBetaWeapon(trace.getItemId())) {
                        MapleInventoryManipulator.equip(c, trace.getPosition(), (short) -10, null);
                    }
                }
                break;
            }
            case 50: { // 주문의 흔적 : 아이템 올려놓기
                short pos = slea.readShort();
                if (pos > 0) {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                } else {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                }

                c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(type, item, null, null, null));
                break;
            }
            case 52: { // 스타포스 : 아이템 올려놓기
                short pos = (short) slea.readInt();
                if (pos > 0) {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(pos);
                } else {
                    item = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                }

                boolean shield = slea.readByte() == 1;

                c.getPlayer().shield = shield;

                Pair<Integer, Integer> per = starForcePercent(item);

                int success = per.left, destroy = per.right, meso = (int) StarForceMeso(item); // 롱이 맞긴한데, 아직 21억 안 넘어가니까 ㅇㅇ..

                if (shield) {
                    meso *= 2;
                }
                int down = 1000 - success - destroy;

                StarForceStats stats = starForceStats(item);

                if ((!ii.isSuperial(item.getItemId()).right && item.getEnhance() <= 10) || (item.getEnhance() % 5) == 0) {
                    down = 0;
                }

                if ((item.getEnchantBuff() & 0x20) != 0) {
                    success = 1000;
                    down = 0;
                    destroy = 0;
                }

                if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < (GameConstants.isZeroWeapon(item.getItemId()) ? 2 : 1)) {
                    c.getPlayer().dropMessage(1, "장비 창에 " + (GameConstants.isZeroWeapon(item.getItemId()) ? 2 : 1) + "칸 이상의 공간이 필요합니다.");
                    return;
                }

                //downper, destroyper, successper, meso 순으로
                c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(type, item, null, null, stats, down, destroy, success, meso));
                break;
            }
            case 53: { // 스타포스 : 스타 캐치
                c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(53, null, null, null, null, 0xA5135C00)); // 값 뭔지 모름
                break;
            }
        }
    }

    public static Pair<Integer, Integer> starForcePercent(Equip item) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean superial = ii.isSuperial(item.getItemId()).right;
        Pair<Integer, Integer> percent = new Pair<>(0, 0);
        switch (item.getEnhance()) {
            case 0:
                percent.left = superial ? 500 : 1000;
                break;
            case 1:
                percent.left = superial ? 500 : 1000;
                break;
            case 2:
                percent.left = superial ? 450 : 1000;
                break;
            case 3:
                percent.left = superial ? 400 : 1000;
                break;
            case 4:
                percent.left = superial ? 400 : 1000;
                break;
            case 5:
                percent.left = superial ? 400 : 1000;
                percent.right = superial ? 18 : 0;
                break;
            case 6:
                percent.left = superial ? 400 : 1000;
                percent.right = superial ? 30 : 0;
                break;
            case 7:
                percent.left = superial ? 400 : 1000;
                percent.right = superial ? 42 : 0;
                break;
            case 8:
                percent.left = superial ? 400 : 1000;
                percent.right = superial ? 60 : 0;
                break;
            case 9:
                percent.left = superial ? 370 : 1000;
                percent.right = superial ? 95 : 0;
                break;
            case 10:
                percent.left = superial ? 350 : 1000;
                percent.right = superial ? 130 : 0;
                break;
            case 11:
                percent.left = superial ? 350 : 1000;
                percent.right = superial ? 163 : 0;
                break;
            case 12:
                percent.left = superial ? 30 : 1000;
                percent.right = superial ? 485 : 6;
                break;
            case 13:
                percent.left = superial ? 20 : 350;
                percent.right = superial ? 490 : 13;
                break;
            case 14:
                percent.left = superial ? 10 : 300;
                percent.right = superial ? 495 : 14;
                break;
            case 15:
                percent.left = 300;
                percent.right = 21;
                break;
            case 16:
                percent.left = 300;
                percent.right = 21;
                break;
            case 17:
                percent.left = 300;
                percent.right = 21;
                break;
            case 18:
                percent.left = 300;
                percent.right = 21;
                break;
            case 19:
                percent.left = 300;
                percent.right = 21;
                break;
            case 20:
                percent.left = 300;
                percent.right = 70;
                break;
            case 21:
                percent.left = 300;
                percent.right = 70;
                break;
            case 22:
                percent.left = 30;
                percent.right = 194;
                break;
            case 23:
                percent.left = 20;
                percent.right = 294;
                break;
            case 24:
                percent.left = 10;
                percent.right = 999;//396;
                break;
        }
        return percent;
    }

    public static long StarForceMeso(Equip item) {
        long base = 0;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Integer ReqLevel = ii.getEquipStats(item.getItemId()).get("reqLevel");
        if (ReqLevel == null) {
            ReqLevel = 0;
        }
        int enhance = item.getEnhance();
        if (ii.isSuperial(item.getItemId()).right) {
            switch (ii.isSuperial(item.getItemId()).left) {
                case "Helisium":
                    return 5956600 + (0 * enhance); //슈페리얼스타포스
                case "Nova":
                    return 18507900 + (0 * enhance);
                case "Tilent":
                    return 55832200 + (0 * enhance);
            }
        } else {
            if (ReqLevel < 110) {
                base = 41000;
                return (base + (enhance * 40000));
            } else if (ReqLevel >= 110 && ReqLevel < 120) { //110제 강화비용
                if (enhance <= 0) {
                    return 54200;
                } else if (enhance <= 1) {
                    return 107500;
                } else if (enhance <= 2) {
                    return 160700;
                } else if (enhance <= 3) {
                    return 214000;
                } else if (enhance <= 4) {
                    return 267200;
                } else if (enhance <= 5) {
                    return 320400;
                } else if (enhance <= 6) {
                    return 373700;
                } else if (enhance <= 7) {
                    return 426900;
                } else if (enhance <= 8) {
                    return 480200;
                } else if (enhance <= 9) {
                    return 533400;
                }

            } else if (ReqLevel >= 120 && ReqLevel < 130) { //120제 강화비용
                if (enhance <= 0) {
                    return 70100;
                } else if (enhance <= 1) {
                    return 139200;
                } else if (enhance <= 2) {
                    return 208400;
                } else if (enhance <= 3) {
                    return 277500;
                } else if (enhance <= 4) {
                    return 346600;
                } else if (enhance <= 5) {
                    return 415700;
                } else if (enhance <= 6) {
                    return 484800;
                } else if (enhance <= 7) {
                    return 554000;
                } else if (enhance <= 8) {
                    return 623100;
                } else if (enhance <= 9) {
                    return 692200;
                } else if (enhance <= 10) {
                    return 2801600;
                } else if (enhance <= 11) {
                    return 3543200;
                } else if (enhance <= 12) {
                    return 4397700;
                } else if (enhance <= 13) {
                    return 5371700;
                } else if (enhance <= 14) {
                    return 6471400;
                }

            } else if (ReqLevel >= 130 && ReqLevel < 140) { //130제 강화비용
                if (enhance <= 0) {
                    return 88900;
                } else if (enhance <= 1) {
                    return 176800;
                } else if (enhance <= 2) {
                    return 264600;
                } else if (enhance <= 3) {
                    return 352500;
                } else if (enhance <= 4) {
                    return 440400;
                } else if (enhance <= 5) {
                    return 528300;
                } else if (enhance <= 6) {
                    return 616200;
                } else if (enhance <= 7) {
                    return 704000;
                } else if (enhance <= 8) {
                    return 791900;
                } else if (enhance <= 9) {
                    return 879800;
                } else if (enhance <= 10) { //11성
                    return 3561700;
                } else if (enhance <= 11) {
                    return 4504600;
                } else if (enhance <= 12) {
                    return 5591100;
                } else if (enhance <= 13) {
                    return 6829300;
                } else if (enhance <= 14) {
                    return 8227500;
                } else if (enhance <= 15) {
                    return 19586000;
                } else if (enhance <= 16) {
                    return 23069100;
                } else if (enhance <= 17) {
                    return 26918600;
                } else if (enhance <= 18) {
                    return 31149300;
                } else if (enhance <= 19) {
                    return 35776100;
                }
            } else if (ReqLevel >= 140 && ReqLevel < 150) {
                if (enhance <= 0) {
                    return 110800;
                } else if (enhance <= 1) {
                    return 220500;
                } else if (enhance <= 2) {
                    return 330300;
                } else if (enhance <= 3) {
                    return 440000;
                } else if (enhance <= 4) {
                    return 549800;
                } else if (enhance <= 5) {
                    return 659600;
                } else if (enhance <= 6) {
                    return 769300;
                } else if (enhance <= 7) {
                    return 879100;
                } else if (enhance <= 8) {
                    return 988800;
                } else if (enhance <= 9) {
                    return 1098600;
                } else if (enhance <= 10) {
                    return 4448200;
                } else if (enhance <= 11) {
                    return 5625900;
                } else if (enhance <= 12) {
                    return 6982900;
                } else if (enhance <= 13) {
                    return 8529400;
                } else if (enhance <= 14) {
                    return 10275700;
                } else if (enhance <= 15) {
                    return 24462200;
                } else if (enhance <= 16) {
                    return 28812500;
                } else if (enhance <= 17) {
                    return 33620400;
                } else if (enhance <= 18) {
                    return 38904500;
                } else if (enhance <= 19) {
                    return 44683300;
                } else if (enhance <= 20) {
                    return 50974700;
                } else if (enhance <= 21) {
                    return 57796700;
                } else if (enhance <= 22) {
                    return 65166700;
                } else if (enhance <= 23) {
                    return 73102200;
                } else if (enhance <= 24) {
                    return 81620200;
                }

            } else if (ReqLevel >= 150 && ReqLevel < 160) {
                if (enhance <= 0) {
                    return 136000;
                } else if (enhance <= 1) {
                    return 271000;
                } else if (enhance <= 2) {
                    return 406000;
                } else if (enhance <= 3) {
                    return 541000;
                } else if (enhance <= 4) {
                    return 676000;
                } else if (enhance <= 5) {
                    return 811000;
                } else if (enhance <= 6) {
                    return 946000;
                } else if (enhance <= 7) {
                    return 1081000;
                } else if (enhance <= 8) {
                    return 1216000;
                } else if (enhance <= 9) {
                    return 1351000;
                } else if (enhance <= 10) {
                    return 5470800;
                } else if (enhance <= 11) {
                    return 6919400;
                } else if (enhance <= 12) {
                    return 8588400;
                } else if (enhance <= 13) {
                    return 10490600;
                } else if (enhance <= 14) {
                    return 12638500;
                } else if (enhance <= 15) {
                    return 30087200;
                } else if (enhance <= 16) {
                    return 35437900;
                } else if (enhance <= 17) {
                    return 41351400;
                } else if (enhance <= 18) {
                    return 47850600;
                } else if (enhance <= 19) {
                    return 54958200;
                } else if (enhance <= 20) {
                    return 62696400;
                } else if (enhance <= 21) {
                    return 71087200;
                } else if (enhance <= 22) {
                    return 80152000;
                } else if (enhance <= 23) {
                    return 89912300;
                } else if (enhance <= 24) {
                    return 100389000;
                }
            } else if (ReqLevel >= 160 && ReqLevel < 170) {
                if (enhance <= 0) {
                    return 164800;
                } else if (enhance <= 1) {
                    return 328700;
                } else if (enhance <= 2) {
                    return 492500;
                } else if (enhance <= 3) {
                    return 656400;
                } else if (enhance <= 4) {
                    return 820200;
                } else if (enhance <= 5) {
                    return 984000;
                } else if (enhance <= 6) {
                    return 1147900;
                } else if (enhance <= 7) {
                    return 1311700;
                } else if (enhance <= 8) {
                    return 1475600;
                } else if (enhance <= 9) {
                    return 1639400;
                } else if (enhance <= 10) {
                    return 6639400;
                } else if (enhance <= 11) {
                    return 8397300;
                } else if (enhance <= 12) {
                    return 10422900;
                } else if (enhance <= 13) {
                    return 12731500;
                } else if (enhance <= 14) {
                    return 15338200;
                } else if (enhance <= 15) {
                    return 36514500;
                } else if (enhance <= 16) {
                    return 43008300;
                } else if (enhance <= 17) {
                    return 50185100;
                } else if (enhance <= 18) {
                    return 58072700;
                } else if (enhance <= 19) {
                    return 66698700;
                } else if (enhance <= 20) {
                    return 76090000;
                } else if (enhance <= 21) {
                    return 86273300;
                } else if (enhance <= 22) {
                    return 97274600;
                } else if (enhance <= 23) {
                    return 89912300;
                } else if (enhance <= 24) {
                    return 100389000;
                }

            } else if (ReqLevel >= 170) {
                if (enhance <= 0) {
                    return 321000;
                } else if (enhance <= 1) {
                    return 641000;
                } else if (enhance <= 2) {
                    return 961000;
                } else if (enhance <= 3) {
                    return 1281000;
                } else if (enhance <= 4) {
                    return 1601000;
                } else if (enhance <= 5) {
                    return 1921000;
                } else if (enhance <= 6) {
                    return 2241000;
                } else if (enhance <= 7) {
                    return 2561000;
                } else if (enhance <= 8) {
                    return 2881000;
                } else if (enhance <= 9) {
                    return 3201000;
                } else if (enhance <= 10) {
                    return 12966500;
                } else if (enhance <= 11) {
                    return 16400100;
                } else if (enhance <= 12) {
                    return 20356300;
                } else if (enhance <= 13) {
                    return 24865300;
                } else if (enhance <= 14) {
                    return 29956500;
                } else if (enhance <= 15) {
                    return 71316500;
                } else if (enhance <= 16) {
                    return 83999600;
                } else if (enhance <= 17) {
                    return 98016700;
                } else if (enhance <= 18) {
                    return 113422300;
                } else if (enhance <= 19) {
                    return 130270000;
                } else if (enhance <= 20) {
                    return 148612400;
                } else if (enhance <= 21) {
                    return 168501500;
                } else if (enhance <= 22) {
                    return 189988600;
                } else if (enhance <= 23) {
                    return 213124000;
                } else if (enhance <= 24) {
                    return 237957700;
                }
            }
        }
        return 0;
    }

    private static void setJuhun(int level, boolean weapon) {
        switch (level / 10) {
            case 1:
                usejuhun[0] = 2;
                usejuhun[1] = 3;
                usejuhun[2] = 4;
                usejuhun[3] = 5;
                break;
            case 2:
                usejuhun[0] = 3;
                usejuhun[1] = 4;
                usejuhun[2] = 5;
                usejuhun[3] = 6;
                break;
            case 3:
                usejuhun[0] = 5;
                usejuhun[1] = 7;
                usejuhun[2] = 8;
                usejuhun[3] = 10;
                break;
            case 4:
                usejuhun[0] = 6;
                usejuhun[1] = 8;
                usejuhun[2] = 10;
                usejuhun[3] = 12;
                break;
            case 5:
                usejuhun[0] = 8;
                usejuhun[1] = 10;
                usejuhun[2] = 12;
                usejuhun[3] = 14;
                break;
            case 6:
                usejuhun[0] = 9;
                usejuhun[1] = 12;
                usejuhun[2] = 14;
                usejuhun[3] = 17;
                break;
            case 7:
                usejuhun[0] = 11;
                usejuhun[1] = 14;
                usejuhun[2] = 17;
                usejuhun[3] = 20;
                break;
            case 8:
                usejuhun[0] = 23;
                usejuhun[1] = 30;
                usejuhun[2] = 36;
                usejuhun[3] = 43;
                break;
            case 9:
                usejuhun[0] = 29;
                usejuhun[1] = 38;
                usejuhun[2] = 46;
                usejuhun[3] = 55;
                break;
            case 10:
                usejuhun[0] = 36;
                usejuhun[1] = 47;
                usejuhun[2] = 56;
                usejuhun[3] = 67;
                break;
            case 11:
                usejuhun[0] = 43;
                usejuhun[1] = 56;
                usejuhun[2] = 67;
                usejuhun[3] = 80;
                break;
            case 12:
                if (!weapon) {
                    usejuhun[0] = 95;
                    usejuhun[1] = 120;
                    usejuhun[2] = 145;
                } else {
                    usejuhun[0] = 155;
                    usejuhun[1] = 200;
                    usejuhun[2] = 240;
                    usejuhun[3] = 290;
                }
                break;
            case 13:
                if (!weapon) {
                    usejuhun[0] = 120;
                    usejuhun[1] = 155;
                    usejuhun[2] = 190;
                } else {
                    usejuhun[0] = 200;
                    usejuhun[1] = 260;
                    usejuhun[2] = 310;
                    usejuhun[3] = 370;
                }
                break;
            case 14:
                if (!weapon) {
                    usejuhun[0] = 150;
                    usejuhun[1] = 195;
                    usejuhun[2] = 230;
                } else {
                    usejuhun[0] = 240;
                    usejuhun[1] = 320;
                    usejuhun[2] = 380;
                    usejuhun[3] = 460;
                }
                break;
            case 15:
            case 16:
            case 20:
                if (!weapon) {
                    usejuhun[0] = 185;
                    usejuhun[1] = 240;
                    usejuhun[2] = 290;
                } else {
                    usejuhun[0] = 280;
                    usejuhun[1] = 380;
                    usejuhun[2] = 450;
                    usejuhun[3] = 570;
                }
                break;
        }
    }
}
