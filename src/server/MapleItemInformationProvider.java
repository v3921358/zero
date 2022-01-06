package server;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.StructPotentialItem;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.DatabaseConnection;
import provider.*;
import server.StructSetItem.SetItem;
import server.enchant.EnchantFlag;
import server.enchant.EquipmentEnchant;
import server.enchant.StarForceStats;
import server.quest.MapleQuest;
import tools.Pair;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

import static server.enchant.EquipmentEnchant.*;

public class MapleItemInformationProvider {

    private final static MapleItemInformationProvider instance = new MapleItemInformationProvider();
    protected final MapleDataProvider chrData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Character.wz"));
    protected final MapleDataProvider etcData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Etc.wz"));
    protected final MapleDataProvider itemData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Item.wz"));
    protected final MapleDataProvider stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/String.wz"));
    protected final Map<Integer, ItemInformation> dataCache = new HashMap<Integer, ItemInformation>();
    protected final Map<String, List<Triple<String, Point, Point>>> afterImage = new HashMap<String, List<Triple<String, Point, Point>>>();
    protected final Map<Integer, List<StructPotentialItem>> potentialCache = new HashMap<Integer, List<StructPotentialItem>>();
    protected final Map<Integer, MapleStatEffect> itemEffects = new HashMap<Integer, MapleStatEffect>();
    protected final Map<Integer, MapleStatEffect> itemEffectsEx = new HashMap<Integer, MapleStatEffect>();
    protected final Map<Integer, Integer> mobIds = new HashMap<Integer, Integer>();
    protected final Map<Integer, Pair<Integer, Integer>> potLife = new HashMap<Integer, Pair<Integer, Integer>>(); //itemid to lifeid, levels
    protected final Map<Integer, Triple<Pair<List<Integer>, List<Integer>>, List<Integer>, Integer>> androids = new HashMap<Integer, Triple<Pair<List<Integer>, List<Integer>>, List<Integer>, Integer>>();
    protected final Map<Integer, Triple<Integer, List<Integer>, List<Integer>>> monsterBookSets = new HashMap<Integer, Triple<Integer, List<Integer>, List<Integer>>>();
    protected final Map<Integer, StructSetItem> setItems = new HashMap<>();
    protected final List<Pair<Integer, String>> itemNameCache = new ArrayList<Pair<Integer, String>>();
    protected final Map<Integer, Integer> scrollUpgradeSlotUse = new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> cursedCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, Integer> successCache = new HashMap<Integer, Integer>();
    protected final Map<Integer, List<Triple<Boolean, Integer, Integer>>> potentialOpCache = new HashMap<>();
    protected final List<Integer> specialHairFaceInfo = new ArrayList<>();

    public void runEtc() {
        if (!setItems.isEmpty() || !potentialCache.isEmpty()) {
            return;
        }
        final MapleData setsData = etcData.getData("SetItemInfo.img");
        StructSetItem itemz;
        SetItem itez;
        for (MapleData dat : setsData) {
            itemz = new StructSetItem();
            itemz.setItemID = Integer.parseInt(dat.getName());
            itemz.completeCount = (byte) MapleDataTool.getIntConvert("completeCount", dat, 0);
            itemz.jokerPossible = MapleDataTool.getIntConvert("jokerPossible", dat, 0) > 0;
            itemz.zeroWeaponJokerPossible = MapleDataTool.getIntConvert("zeroWeaponJokerPossible", dat, 0) > 0;
            for (MapleData level : dat.getChildByPath("ItemID")) {
                if (level.getType() != MapleDataType.INT) {
                    for (MapleData leve : level) {
                        if (!leve.getName().equals("representName") && !leve.getName().equals("typeName")) {
                            itemz.itemIDs.add(MapleDataTool.getInt(leve));
                        }
                    }
                } else {
                    itemz.itemIDs.add(MapleDataTool.getInt(level));
                }
            }
            for (MapleData level : dat.getChildByPath("Effect")) {
                itez = new SetItem();
                itez.incPDD = MapleDataTool.getIntConvert("incPDD", level, 0);
                itez.incMDD = MapleDataTool.getIntConvert("incMDD", level, 0);
                itez.incSTR = MapleDataTool.getIntConvert("incSTR", level, 0);
                itez.incDEX = MapleDataTool.getIntConvert("incDEX", level, 0);
                itez.incINT = MapleDataTool.getIntConvert("incINT", level, 0);
                itez.incLUK = MapleDataTool.getIntConvert("incLUK", level, 0);
                itez.incACC = MapleDataTool.getIntConvert("incACC", level, 0);
                itez.incPAD = MapleDataTool.getIntConvert("incPAD", level, 0);
                itez.incMAD = MapleDataTool.getIntConvert("incMAD", level, 0);
                itez.incSpeed = MapleDataTool.getIntConvert("incSpeed", level, 0);
                itez.incMHP = MapleDataTool.getIntConvert("incMHP", level, 0);
                itez.incMMP = MapleDataTool.getIntConvert("incMMP", level, 0);
                itez.incMHPr = MapleDataTool.getIntConvert("incMHPr", level, 0);
                itez.incMMPr = MapleDataTool.getIntConvert("incMMPr", level, 0);
                itez.incAllStat = MapleDataTool.getIntConvert("incAllStat", level, 0);
                itez.option1 = MapleDataTool.getIntConvert("Option/1/option", level, 0);
                itez.option2 = MapleDataTool.getIntConvert("Option/2/option", level, 0);
                itez.option1Level = MapleDataTool.getIntConvert("Option/1/level", level, 0);
                itez.option2Level = MapleDataTool.getIntConvert("Option/2/level", level, 0);
                if (level.getChildByPath("activeSkill") != null) {
                    for (MapleData skill : level.getChildByPath("activeSkill")) {
                        itez.activeSkills.put(MapleDataTool.getIntConvert("id", skill, 0), (byte) MapleDataTool.getIntConvert("level", skill, 0));
                    }
                }
                itemz.items.put(Integer.parseInt(level.getName()), itez);
            }
            setItems.put(itemz.setItemID, itemz);
        }
        final MapleDataDirectoryEntry e = (MapleDataDirectoryEntry) etcData.getRoot().getEntry("Android");
        for (MapleDataEntry d : e.getFiles()) {
            final MapleData iz = etcData.getData("Android/" + d.getName());
            int gender = 0;
            final List<Integer> hair = new ArrayList<Integer>(), face = new ArrayList<Integer>(), skin = new ArrayList<Integer>();
            for (MapleData ds : iz.getChildByPath("costume/hair")) {
                hair.add(MapleDataTool.getInt(ds, 30000));
            }
            for (MapleData ds : iz.getChildByPath("costume/face")) {
                face.add(MapleDataTool.getInt(ds, 20000));
            }
            for (MapleData ds : iz.getChildByPath("costume/skin")) {
                skin.add(MapleDataTool.getInt(ds, 0));
            }
            for (MapleData ds : iz.getChildByPath("info")) {
                if (ds.getName().equals("gender")) {
                    gender = MapleDataTool.getInt(ds, 0);
                }
            }
            androids.put(Integer.parseInt(d.getName().substring(0, 4)), new Triple(new Pair<List<Integer>, List<Integer>>(hair, face), skin, gender));
        }

        final MapleData lifesData = etcData.getData("ItemPotLifeInfo.img");
        for (MapleData d : lifesData) {
            if (d.getChildByPath("info") != null && MapleDataTool.getInt("type", d.getChildByPath("info"), 0) == 1) {
                potLife.put(MapleDataTool.getInt("counsumeItem", d.getChildByPath("info"), 0), new Pair<Integer, Integer>(Integer.parseInt(d.getName()), d.getChildByPath("level").getChildren().size()));
            }
        }
        List<Triple<String, Point, Point>> thePointK = new ArrayList<Triple<String, Point, Point>>();
        List<Triple<String, Point, Point>> thePointA = new ArrayList<Triple<String, Point, Point>>();

        final MapleDataDirectoryEntry a = (MapleDataDirectoryEntry) chrData.getRoot().getEntry("Afterimage");
        for (MapleDataEntry b : a.getFiles()) {
            final MapleData iz = chrData.getData("Afterimage/" + b.getName());
            List<Triple<String, Point, Point>> thePoint = new ArrayList<Triple<String, Point, Point>>();
            Map<String, Pair<Point, Point>> dummy = new HashMap<String, Pair<Point, Point>>();
            for (MapleData i : iz) {
                for (MapleData xD : i) {
                    if (xD.getName().contains("prone") || xD.getName().contains("double") || xD.getName().contains("triple")) {
                        continue;
                    }
                    if ((b.getName().contains("bow") || b.getName().contains("Bow")) && !xD.getName().contains("shoot")) {
                        continue;
                    }
                    if ((b.getName().contains("gun") || b.getName().contains("cannon")) && !xD.getName().contains("shot")) {
                        continue;
                    }
                    if (dummy.containsKey(xD.getName())) {
                        if (xD.getChildByPath("lt") != null) {
                            Point lt = (Point) xD.getChildByPath("lt").getData();
                            Point ourLt = dummy.get(xD.getName()).left;
                            if (lt.x < ourLt.x) {
                                ourLt.x = lt.x;
                            }
                            if (lt.y < ourLt.y) {
                                ourLt.y = lt.y;
                            }
                        }
                        if (xD.getChildByPath("rb") != null) {
                            Point rb = (Point) xD.getChildByPath("rb").getData();
                            Point ourRb = dummy.get(xD.getName()).right;
                            if (rb.x > ourRb.x) {
                                ourRb.x = rb.x;
                            }
                            if (rb.y > ourRb.y) {
                                ourRb.y = rb.y;
                            }
                        }
                    } else {
                        Point lt = null, rb = null;
                        if (xD.getChildByPath("lt") != null) {
                            lt = (Point) xD.getChildByPath("lt").getData();
                        }
                        if (xD.getChildByPath("rb") != null) {
                            rb = (Point) xD.getChildByPath("rb").getData();
                        }
                        dummy.put(xD.getName(), new Pair<Point, Point>(lt, rb));
                    }
                }
            }
            for (Entry<String, Pair<Point, Point>> ez : dummy.entrySet()) {
                if (ez.getKey().length() > 2 && ez.getKey().substring(ez.getKey().length() - 2, ez.getKey().length() - 1).equals("D")) { //D = double weapon
                    thePointK.add(new Triple<String, Point, Point>(ez.getKey(), ez.getValue().left, ez.getValue().right));
                } else if (ez.getKey().contains("PoleArm")) { //D = double weapon
                    thePointA.add(new Triple<String, Point, Point>(ez.getKey(), ez.getValue().left, ez.getValue().right));
                } else {
                    thePoint.add(new Triple<String, Point, Point>(ez.getKey(), ez.getValue().left, ez.getValue().right));
                }
            }
            afterImage.put(b.getName().substring(0, b.getName().length() - 4), thePoint);
        }
        afterImage.put("katara", thePointK); //hackish
        afterImage.put("aran", thePointA); //hackish
    }

    public void runItems() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();

            // Load Item Data
            ps = con.prepareStatement("SELECT * FROM wz_itemdata");

            rs = ps.executeQuery();
            while (rs.next()) {
                initItemInformation(rs);
            }
            rs.close();
            ps.close();

            // Load Item Equipment Data
            ps = con.prepareStatement("SELECT * FROM wz_itemequipdata ORDER BY itemid");
            rs = ps.executeQuery();
            while (rs.next()) {
                initItemEquipData(rs);
            }
            rs.close();
            ps.close();

            // Load Item Addition Data
            ps = con.prepareStatement("SELECT * FROM wz_itemadddata ORDER BY itemid");
            rs = ps.executeQuery();
            while (rs.next()) {
                initItemAddData(rs);
            }
            rs.close();
            ps.close();

            // Load Item Reward Data
            ps = con.prepareStatement("SELECT * FROM wz_itemrewarddata ORDER BY itemid");
            rs = ps.executeQuery();
            while (rs.next()) {
                initItemRewardData(rs);
            }
            rs.close();
            ps.close();

            // Finalize all Equipments
            for (Entry<Integer, ItemInformation> entry : dataCache.entrySet()) {
                byte type = (byte) (entry.getKey() / 1000000);
                if (type == 1) { //GameConstants.getInventoryType(entry.getKey())
                    finalizeEquipData(entry.getValue());
                }
            }
            cachePotentialItems();
            cachePotentialOption();
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

    public final int getPotentialOptionID(int level, boolean additional, int itemtype) {
        /*
        int i = 0;
        while (potentials.size() <= 0 || potentials.isEmpty()) {
            potentials = new ArrayList<>();
            potentialSet(potentials, level, additional, itemtype);
            if (i++ == 10) {
                break;
            }
        }

        if (potentials.size() <= 0 || potentials.isEmpty()) {
            System.out.println(level + "레벨 " + itemtype + "타입 아이템의 잠재능력 리스트 0개 -_- / 에디셔널 여부 : " + additional);
        }*/

        List<Integer> potentials = new ArrayList<>();
        List<Triple<Boolean, Integer, Integer>> sub = null;

        if (additional) {
            addPotential(potentials, potentialOpCache.get(11), level, additional, itemtype);
        } else {
            addPotential(potentials, potentialOpCache.get(-1), level, additional, itemtype);
        }

        if (itemtype / 10 == 100) {
            sub = potentialOpCache.get(51);
        }

        if (itemtype / 10 == 104) {
            sub = potentialOpCache.get(52);
        }

        if (itemtype / 10 == 106) {
            sub = potentialOpCache.get(53);
        }

        if (itemtype / 10 == 107) {
            sub = potentialOpCache.get(55);
        }

        if (itemtype / 10 == 108 && !additional) {
            sub = potentialOpCache.get(54);
        }

        if (!additional && isWeapon(itemtype)) {
            sub = potentialOpCache.get(10);
        }

        if ((itemtype >= 1010 && itemtype <= 1032) || itemtype == 1122 || itemtype / 10 == 111) {
            sub = potentialOpCache.get(40);
        }

        if (sub != null) {
            addPotential(potentials, sub, level, additional, itemtype);
        }

        if (additional && isWeapon(itemtype)) {
            sub = potentialOpCache.get(10);

            for (Triple<Boolean, Integer, Integer> potential : sub) {
                if (!potential.left) {
                    continue;
                } else if ((itemtype == 1190 || itemtype == 1191) && (isBossDamage(potential.right) || isAdditionalBossDamage(potential.right))) {
                    continue;
                } else if (isStatPer(potential.right) || isEditinalPMDCheck(potential.right) || isBossDamage(potential.right) || isIgnoreMobPdp(potential.right) || isHMPCheck(potential.right)) {
                    continue;
                } else if (level != (potential.right / 10000)) {
                    continue;
                } else {
                    potentials.add(potential.right);
                }
            }
        }

        if (additional && itemtype / 10 == 108) {
            sub = potentialOpCache.get(20);

            for (Triple<Boolean, Integer, Integer> potential : sub) {
                if (!potential.left) {
                    continue;
                } else if (isStatPer(potential.right) || isEditinalPMDCheck(potential.right) || isBossDamage(potential.right) || isIgnoreMobPdp(potential.right) || isHMPCheck(potential.right)) {
                    continue;
                } else if (level != (potential.right / 10000)) {
                    continue;
                } else {
                    potentials.add(potential.right);
                }
            }
        }

        return potentials.get(Randomizer.nextInt(potentials.size()));
    }

    private void potentialSet(List<Integer> potentials, int level, boolean additional, int itemtype) {

        if (isWeaponPotential(itemtype)) {
            addPotential(potentials, potentialOpCache.get(10), level, additional, itemtype);
        } else {
            addPotential(potentials, potentialOpCache.get(11), level, additional, itemtype);
        }

        if (!isWeaponPotential(itemtype)) {
            if (isAccessoryPotential(itemtype)) {
                addPotential(potentials, potentialOpCache.get(40), level, additional, itemtype);
            } else if (additional) {
                addPotential(potentials, potentialOpCache.get(20), level, additional, itemtype);
            }
        }

        if (itemtype / 10 == 100) {
            addPotential(potentials, potentialOpCache.get(51), level, additional, itemtype);
        }

        if (itemtype / 10 == 104) {
            addPotential(potentials, potentialOpCache.get(52), level, additional, itemtype);
        }

        if (itemtype / 10 == 106) {
            addPotential(potentials, potentialOpCache.get(53), level, additional, itemtype);
        }

        if (itemtype / 10 == 107) {
            addPotential(potentials, potentialOpCache.get(55), level, additional, itemtype);
        }

        if (itemtype / 10 == 108) {
            addPotential(potentials, potentialOpCache.get(54), level, additional, itemtype);
        }

        addPotential(potentials, potentialOpCache.get(-1), level, additional, itemtype);
    }

    private void addPotential(List<Integer> potentials, List<Triple<Boolean, Integer, Integer>> list, int level, boolean additional, int itemtype) {

        for (Triple<Boolean, Integer, Integer> potential : list) {
            if (additional) {
                if (!potential.left) {
                    continue;
                } else if (isStatPer(potential.right)) {
                    continue;
                } else if (!isWeapon(itemtype) && (isAdditionalBossDamage(potential.right) || isAdditionalIgnoreMobPdp(potential.right) || isEditinalPMDPerCheck(potential.right) || isEditinalCriticalCheck(potential.right))) {
                    continue;
                } else if (isEditinalPMDCheck(potential.right) || isBossDamage(potential.right) || isIgnoreMobPdp(potential.right) || isHMPCheck(potential.right)) {
                    continue;
                } else if ((potential.right % 100 >= 40 && potential.right % 100 <= 50) && (potential.right % 10000) / 1000 == 0) {
                    continue;
                } else if (level != (potential.right / 10000)) {
                    continue;
                } else {
                    potentials.add(potential.right);
                }
            } else {
                if (potential.left) {
                    continue;
                } else if ((itemtype == 1190 || itemtype == 1191) && isBossDamage(potential.right)) {
                    continue;
                } else if (isEditinalPMDCheck(potential.right) || isAdditionalBossDamage(potential.right) || isAdditionalIgnoreMobPdp(potential.right) || isEditinalPMDPerCheck(potential.right) || isEditinalCriticalCheck(potential.right) || isEditinalDamageCheck(potential.right) || isAdditionalHMPCheck(potential.right)) {
                    continue;
                } else if (level != (potential.right / 10000)) {
                    continue;
                } else {
                    potentials.add(potential.right);
                }
            }
        }
    }

    private boolean isWeaponPotential(int itemtype) {
        return (GameConstants.isWeapon(itemtype * 1000) || itemtype == 1098 || itemtype == 1092 || itemtype == 1099 || itemtype == 1190 || itemtype == 1191);
    }

    private boolean isWeapon(int itemtype) {
        return (GameConstants.isWeapon(itemtype * 1000) || itemtype == 1098 || itemtype == 1092 || itemtype == 1099 || itemtype == 1672 || itemtype == 1190 || itemtype == 1191);
    }

    private boolean isEditinalPMDPerCheck(int potential) {
        switch (potential) {
            case 12051:
            case 12052:
            case 12053:
            case 12054:
            case 22051:
            case 22052:
            case 22053:
            case 22054:
            case 32051:
            case 42051:
            case 32053:
            case 42053:
                return true;
        }
        return false;
    }

    private boolean isHMPCheck(int potential) {
        switch (potential) {
            case 10045:
            case 10046:
            case 20045:
            case 20046:
            case 30045:
            case 30046:
            case 40045:
            case 40046:
            case 40047:
            case 40048:
                return true;
        }
        return false;
    }

    private boolean isAdditionalHMPCheck(int potential) {
        switch (potential) {
            case 12045:
            case 12046:
            case 22045:
            case 22046:
            case 22047:
            case 22048:
            case 32045:
            case 32046:
            case 32047:
            case 32048:
            case 42045:
            case 42046:
            case 42047:
            case 42048:
                return true;
        }
        return false;
    }

    private boolean isEditinalPMDCheck(int potential) {
        switch (potential) {
            case 12011:
            case 12012:
            case 22011:
            case 22012:
            case 32011:
            case 32012:
            case 42011:
            case 42012:
                return true;
        }
        return false;
    }

    private boolean isEditinalCriticalCheck(int potential) {
        switch (potential) {
            case 12055:
            case 12056:
            case 22055:
            case 22056:
            case 32055:
            case 32056:
            case 32058:
            case 42055:
            case 42056:
            case 42058:
                return true;
        }
        return false;
    }

    private boolean isEditinalDamageCheck(int potential) {
        switch (potential) {
            case 12070:
            case 12071:
            case 22070:
            case 22071:
            case 32070:
            case 32071:
            case 42070:
            case 42071:
                return true;
        }
        return false;
    }

    private boolean isIgnoreMobPdp(int potential) {
        switch (potential) {
            case 10291:
            case 20291:
            case 30291:
            case 40291:
            case 40292:
                return true;
        }
        return false;
    }

    private boolean isAdditionalIgnoreMobPdp(int potential) {
        switch (potential) {
            case 22291:
            case 22801:
            case 32291:
            case 42291:
            case 42292:
                return true;
        }
        return false;
    }

    private boolean isBossDamage(int potential) {
        switch (potential) {
            case 30601:
            case 30602:
            case 40601:
            case 40602:
            case 40603:
                return true;
        }
        return false;
    }

    private boolean isAdditionalBossDamage(int potential) {
        switch (potential) {
            case 32601:
            case 42602:
            case 42603:
            case 42604:
                return true;
        }
        return false;
    }

    private boolean isAdditionalDamage(int potential) { // dummy
        switch (potential) {
            case 32052:
            case 32054:
            case 42052:
            case 42054:
                return true;
        }
        return false;
    }

    private boolean isAdditionalStatValue(int potential) {
        switch (potential) {
            case 22057:
            case 22058:
            case 22059:
            case 22060:
            case 22087:
            case 32059:
            case 32060:
            case 32061:
            case 32062:
            case 32087:
            case 42063:
            case 42064:
            case 42065:
            case 42066:
            case 42087:
                return true;
        }
        return false;
    }

    private boolean isStatPer(int potential) {
        switch (potential) {
            case 10041:
            case 10042:
            case 10043:
            case 10044:
            case 10045:
            case 10046:
            case 10047:
            case 10048:
            case 10053:
            case 10054:
            case 10086:
            case 20041:
            case 20042:
            case 20043:
            case 20044:
            case 20045:
            case 20046:
            case 20047:
            case 20048:
            case 20053:
            case 20054:
            case 20086:
            case 30041:
            case 30042:
            case 30043:
            case 30044:
            case 30045:
            case 30046:
            case 30047:
            case 30048:
            case 30053:
            case 30054:
            case 30086:
            case 32087:
            case 32801:
            case 40041:
            case 40042:
            case 40043:
            case 40044:
            case 40045:
            case 40046:
            case 40047:
            case 40048:
            case 40053:
            case 40054:
            case 40086:
            case 42087:
                return true;
        }
        return false;
    }

    private boolean isAccessoryPotential(int itemtype) {
        return ((itemtype >= 1112 && itemtype <= 1115) || itemtype == 1122 || itemtype == 1012 || itemtype == 1022 || itemtype == 1032);
    }

    public final List<StructPotentialItem> getPotentialInfo(final int potId) {
        return potentialCache.get(potId);
    }

    public void cachePotentialOption() {
        final MapleData potsData = itemData.getData("ItemOption.img");
        for (MapleData data : potsData) {
            int potentialID = Integer.parseInt(data.getName());
            int type = MapleDataTool.getInt("info/optionType", data, -1);
            int reqLevel = MapleDataTool.getInt("info/reqLevel", data, 0);
            /*
             * type 10 : 무기류 잠재
             * type 11 : 방어구 + 장신구 잠재
             * type 20 : 방어구 에디셔널 잠재
             * type 40 : 악세서리류 잠재
             * type 51 : 모자 전용 옵션
             * type 52 : 상의 전용 옵션
             * type 53 : 하의 전용 옵션
             * type 54 : 장갑 전용 옵션
             * type 55 : 신발 전용 옵션
             *
             */

            switch (potentialID) {
                case 31001:
                case 31002:
                case 31003:
                case 31004:
                case 32091:
                case 32092:
                case 32093:
                case 32094:
                case 32661:
                case 40081: // 올스탯 +12
                case 42059:
                case 42116: // 무기 전용 상태이상 내성 +5
                case 42650:
                case 42656:
                case 42661:

                case 42058://크확 1%?
                case 32058://크확 2%
                    continue;
            }

            boolean additional = type == 11 || type == 20;

            if (potentialID > 50000 || potentialID < 10000 || potentialID % 1000 < 40 || potentialID % 1000 >= 800) { // 임시방편
                continue;
            }
            if (potentialID == 42601 || isEditinalDamageCheck(potentialID) || isAdditionalStatValue(potentialID) || isAdditionalDamage(potentialID) || potentialID == 32071 || potentialID == 40091 || potentialID == 40092 || (potentialID >= 42091 && potentialID <= 42096)) { // 10렙당 예외 처리
                continue;
            }
            if (isAdditionalHMPCheck(potentialID) || potentialID % 10000 / 1000 == 2) {
                additional = true;
            }
            if (isHMPCheck(potentialID)) {
                type = -1;
                additional = false;
            }

            if (potentialOpCache.get(type) == null) {
                List<Triple<Boolean, Integer, Integer>> potentialIds = new ArrayList<>();
                potentialIds.add(new Triple<>(additional, reqLevel, potentialID));
                potentialOpCache.put(type, potentialIds);
            } else {
                potentialOpCache.get(type).add(new Triple<>(additional, reqLevel, potentialID));
            }
        }
    }

    public void cachePotentialItems() {
        final MapleData potsData = itemData.getData("ItemOption.img");
        StructPotentialItem item;
        List<StructPotentialItem> items;
        for (MapleData data : potsData) {
            items = new LinkedList<StructPotentialItem>();
            for (MapleData level : data.getChildByPath("level")) {
                item = new StructPotentialItem();
                item.optionType = MapleDataTool.getIntConvert("info/optionType", data, 0);
                item.reqLevel = MapleDataTool.getIntConvert("info/reqLevel", data, 0);
                item.weight = MapleDataTool.getIntConvert("info/weight", data, 0);
                item.string = MapleDataTool.getString("info/string", level, "");
                item.face = MapleDataTool.getString("face", level, "");
                item.boss = MapleDataTool.getIntConvert("boss", level, 0) > 0;
                item.potentialID = Integer.parseInt(data.getName());
                item.attackType = (short) MapleDataTool.getIntConvert("attackType", level, 0);
                item.incMHP = (short) MapleDataTool.getIntConvert("incMHP", level, 0);
                item.incMMP = (short) MapleDataTool.getIntConvert("incMMP", level, 0);
                item.incSTR = (byte) MapleDataTool.getIntConvert("incSTR", level, 0);
                item.incDEX = (byte) MapleDataTool.getIntConvert("incDEX", level, 0);
                item.incINT = (byte) MapleDataTool.getIntConvert("incINT", level, 0);
                item.incLUK = (byte) MapleDataTool.getIntConvert("incLUK", level, 0);
                item.incACC = (byte) MapleDataTool.getIntConvert("incACC", level, 0);
                item.incEVA = (byte) MapleDataTool.getIntConvert("incEVA", level, 0);
                item.incSpeed = (byte) MapleDataTool.getIntConvert("incSpeed", level, 0);
                item.incJump = (byte) MapleDataTool.getIntConvert("incJump", level, 0);
                item.incPAD = (byte) MapleDataTool.getIntConvert("incPAD", level, 0);
                item.incMAD = (byte) MapleDataTool.getIntConvert("incMAD", level, 0);
                item.incPDD = (byte) MapleDataTool.getIntConvert("incPDD", level, 0);
                item.incMDD = (byte) MapleDataTool.getIntConvert("incMDD", level, 0);
                item.prop = (byte) MapleDataTool.getIntConvert("prop", level, 0);
                item.time = (byte) MapleDataTool.getIntConvert("time", level, 0);
                item.incSTRr = (byte) MapleDataTool.getIntConvert("incSTRr", level, 0);
                item.incDEXr = (byte) MapleDataTool.getIntConvert("incDEXr", level, 0);
                item.incINTr = (byte) MapleDataTool.getIntConvert("incINTr", level, 0);
                item.incLUKr = (byte) MapleDataTool.getIntConvert("incLUKr", level, 0);
                item.incMHPr = (byte) MapleDataTool.getIntConvert("incMHPr", level, 0);
                item.incMMPr = (byte) MapleDataTool.getIntConvert("incMMPr", level, 0);
                item.incACCr = (byte) MapleDataTool.getIntConvert("incACCr", level, 0);
                item.incEVAr = (byte) MapleDataTool.getIntConvert("incEVAr", level, 0);
                item.incPADr = (byte) MapleDataTool.getIntConvert("incPADr", level, 0);
                item.incMADr = (byte) MapleDataTool.getIntConvert("incMADr", level, 0);
                item.incPDDr = (byte) MapleDataTool.getIntConvert("incPDDr", level, 0);
                item.incMDDr = (byte) MapleDataTool.getIntConvert("incMDDr", level, 0);
                item.incCr = (byte) MapleDataTool.getIntConvert("incCr", level, 0);
                item.incDAMr = (byte) MapleDataTool.getIntConvert("incDAMr", level, 0);
                item.RecoveryHP = (byte) MapleDataTool.getIntConvert("RecoveryHP", level, 0);
                item.RecoveryMP = (byte) MapleDataTool.getIntConvert("RecoveryMP", level, 0);
                item.HP = (byte) MapleDataTool.getIntConvert("HP", level, 0);
                item.MP = (byte) MapleDataTool.getIntConvert("MP", level, 0);
                item.level = (byte) MapleDataTool.getIntConvert("level", level, 0);
                item.ignoreTargetDEF = (byte) MapleDataTool.getIntConvert("ignoreTargetDEF", level, 0);
                item.ignoreDAM = (byte) MapleDataTool.getIntConvert("ignoreDAM", level, 0);
                item.DAMreflect = (byte) MapleDataTool.getIntConvert("DAMreflect", level, 0);
                item.mpconReduce = (byte) MapleDataTool.getIntConvert("mpconReduce", level, 0);
                item.mpRestore = (byte) MapleDataTool.getIntConvert("mpRestore", level, 0);
                item.incMesoProp = (byte) MapleDataTool.getIntConvert("incMesoProp", level, 0);
                item.incRewardProp = (byte) MapleDataTool.getIntConvert("incRewardProp", level, 0);
                item.incAllskill = (byte) MapleDataTool.getIntConvert("incAllskill", level, 0);
                item.ignoreDAMr = (byte) MapleDataTool.getIntConvert("ignoreDAMr", level, 0);
                item.RecoveryUP = (byte) MapleDataTool.getIntConvert("RecoveryUP", level, 0);
                item.reduceCooltime = (byte) MapleDataTool.getIntConvert("reduceCooltime", level, 0);
                switch (item.potentialID) {
                    case 31001:
                    case 31002:
                    case 31003:
                    case 31004:
                        item.skillID = (item.potentialID - 23001);
                        break;
                    case 41005:
                    case 41006:
                    case 41007:
                        item.skillID = (item.potentialID - 33001);
                        break;
                    default:
                        item.skillID = 0;
                        break;
                }
                items.add(item);
            }
            potentialCache.put(Integer.parseInt(data.getName()), items);
        }
    }

    public final Collection<Integer> getMonsterBookList() {
        return mobIds.values();
    }

    public final Map<Integer, Integer> getMonsterBook() {
        return mobIds;
    }

    public final Pair<Integer, Integer> getPot(int f) {
        return potLife.get(f);
    }

    public static final MapleItemInformationProvider getInstance() {
        return instance;
    }

    public final List<Pair<Integer, String>> getAllEquips() {
        final List<Pair<Integer, String>> itemPairs = new ArrayList<Pair<Integer, String>>();
        MapleData itemsData;
        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
        for (final MapleData eqpType : itemsData.getChildren()) {
            for (final MapleData itemFolder : eqpType.getChildren()) {
                itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
            }
        }
        return itemPairs;
    }

    public final List<Pair<Integer, String>> getAllItems() {
        if (!itemNameCache.isEmpty()) {
            return itemNameCache;
        }
        final List<Pair<Integer, String>> itemPairs = new ArrayList<Pair<Integer, String>>();
        MapleData itemsData;

        itemsData = stringData.getData("Cash.img");
        for (final MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }

        itemsData = stringData.getData("Consume.img");
        for (final MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }

        itemsData = stringData.getData("Eqp.img").getChildByPath("Eqp");
        for (final MapleData eqpType : itemsData.getChildren()) {
            for (final MapleData itemFolder : eqpType.getChildren()) {
                itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
            }
        }

        itemsData = stringData.getData("Etc.img").getChildByPath("Etc");
        for (final MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }

        itemsData = stringData.getData("Ins.img");
        for (final MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }

        itemsData = stringData.getData("Pet.img");
        for (final MapleData itemFolder : itemsData.getChildren()) {
            itemPairs.add(new Pair<Integer, String>(Integer.parseInt(itemFolder.getName()), MapleDataTool.getString("name", itemFolder, "NO-NAME")));
        }
        return itemPairs;
    }

    public final Triple<Pair<List<Integer>, List<Integer>>, List<Integer>, Integer> getAndroidInfo(int i) {
        return androids.get(i);
    }

    public final Triple<Integer, List<Integer>, List<Integer>> getMonsterBookInfo(int i) {
        return monsterBookSets.get(i);
    }

    public final Map<Integer, Triple<Integer, List<Integer>, List<Integer>>> getAllMonsterBookInfo() {
        return monsterBookSets;
    }

    protected final MapleData getItemData(final int itemId) {
        MapleData ret = null;
        final String idStr = "0" + String.valueOf(itemId);
        MapleDataDirectoryEntry root = itemData.getRoot();
        for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            // we should have .img files here beginning with the first 4 IID
            for (final MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr.substring(0, 4) + ".img")) {
                    ret = itemData.getData(topDir.getName() + "/" + iFile.getName());
                    if (ret == null) {
                        return null;
                    }
                    ret = ret.getChildByPath(idStr);
                    return ret;
                } else if (iFile.getName().equals(idStr.substring(1) + ".img")) {
                    return itemData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        root = chrData.getRoot();
        for (final MapleDataDirectoryEntry topDir : root.getSubdirectories()) {
            for (final MapleDataFileEntry iFile : topDir.getFiles()) {
                if (iFile.getName().equals(idStr + ".img")) {
                    return chrData.getData(topDir.getName() + "/" + iFile.getName());
                }
            }
        }
        return ret;
    }

    public Integer getItemIdByMob(int mobId) {
        return mobIds.get(mobId);
    }

    public Integer getSetId(int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return Integer.valueOf(i.cardSet);
    }

    /**
     * returns the maximum of items in one slot
     */
    public final short getSlotMax(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.slotMax;
    }

    public final int getUpgradeScrollUseSlot(int itemid) {
        if (scrollUpgradeSlotUse.containsKey(itemid)) {
            return scrollUpgradeSlotUse.get(itemid);
        }
        int useslot = MapleDataTool.getIntConvert("info/tuc", getItemData(itemid), 1);
        scrollUpgradeSlotUse.put(itemid, useslot);
        return scrollUpgradeSlotUse.get(itemid);
    }

    public final int getWholePrice(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.wholePrice;
    }

    public final double getPrice(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return -1.0;
        }
        return i.price;
    }

    protected int rand(int min, int max) {
        return Math.abs((int) Randomizer.rand(min, max));
    }

    public Equip levelUpEquip(Equip equip, Map<String, Integer> sta) {
        Equip nEquip = (Equip) equip.copy();
        //is this all the stats?
        try {
            for (Entry<String, Integer> stat : sta.entrySet()) {
                if (stat.getKey().equals("STRMin")) {
                    nEquip.setStr((short) (nEquip.getStr() + rand(stat.getValue().intValue(), sta.get("STRMax").intValue())));
                } else if (stat.getKey().equals("DEXMin")) {
                    nEquip.setDex((short) (nEquip.getDex() + rand(stat.getValue().intValue(), sta.get("DEXMax").intValue())));
                } else if (stat.getKey().equals("INTMin")) {
                    nEquip.setInt((short) (nEquip.getInt() + rand(stat.getValue().intValue(), sta.get("INTMax").intValue())));
                } else if (stat.getKey().equals("LUKMin")) {
                    nEquip.setLuk((short) (nEquip.getLuk() + rand(stat.getValue().intValue(), sta.get("LUKMax").intValue())));
                } else if (stat.getKey().equals("PADMin")) {
                    nEquip.setWatk((short) (nEquip.getWatk() + rand(stat.getValue().intValue(), sta.get("PADMax").intValue())));
                } else if (stat.getKey().equals("PDDMin")) {
                    nEquip.setWdef((short) (nEquip.getWdef() + rand(stat.getValue().intValue(), sta.get("PDDMax").intValue())));
                } else if (stat.getKey().equals("MADMin")) {
                    nEquip.setMatk((short) (nEquip.getMatk() + rand(stat.getValue().intValue(), sta.get("MADMax").intValue())));
                } else if (stat.getKey().equals("MDDMin")) {
                    nEquip.setMdef((short) (nEquip.getMdef() + rand(stat.getValue().intValue(), sta.get("MDDMax").intValue())));
                } else if (stat.getKey().equals("ACCMin")) {
                    nEquip.setAcc((short) (nEquip.getAcc() + rand(stat.getValue().intValue(), sta.get("ACCMax").intValue())));
                } else if (stat.getKey().equals("EVAMin")) {
                    nEquip.setAvoid((short) (nEquip.getAvoid() + rand(stat.getValue().intValue(), sta.get("EVAMax").intValue())));
                } else if (stat.getKey().equals("SpeedMin")) {
                    nEquip.setSpeed((short) (nEquip.getSpeed() + rand(stat.getValue().intValue(), sta.get("SpeedMax").intValue())));
                } else if (stat.getKey().equals("JumpMin")) {
                    nEquip.setJump((short) (nEquip.getJump() + rand(stat.getValue().intValue(), sta.get("JumpMax").intValue())));
                } else if (stat.getKey().equals("MHPMin")) {
                    nEquip.setHp((short) (nEquip.getHp() + rand(stat.getValue().intValue(), sta.get("MHPMax").intValue())));
                } else if (stat.getKey().equals("MMPMin")) {
                    nEquip.setMp((short) (nEquip.getMp() + rand(stat.getValue().intValue(), sta.get("MMPMax").intValue())));
                } else if (stat.getKey().equals("MaxHPMin")) {
                    nEquip.setHp((short) (nEquip.getHp() + rand(stat.getValue().intValue(), sta.get("MaxHPMax").intValue())));
                } else if (stat.getKey().equals("MaxMPMin")) {
                    nEquip.setMp((short) (nEquip.getMp() + rand(stat.getValue().intValue(), sta.get("MaxMPMax").intValue())));
                }
            }
        } catch (NullPointerException e) {
            //catch npe because obviously the wz have some error XD
            e.printStackTrace();
        }
        return nEquip;
    }

    public final List<Triple<String, String, String>> getEquipAdditions(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.equipAdditions;
    }

    public final String getEquipAddReqs(final int itemId, final String key, final String sub) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        for (Triple<String, String, String> data : i.equipAdditions) {
            if (data.getLeft().equals("key") && data.getMid().equals("con:" + sub)) {
                return data.getRight();
            }
        }
        return null;
    }

    public final Map<Integer, Map<String, Integer>> getEquipIncrements(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.equipIncs;
    }

    public final List<Integer> getEquipSkills(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.incSkill;
    }

    public final boolean canEquip(final Map<String, Integer> stats, final int itemid, final int level, final int job, final int fame, final int str, final int dex, final int luk, final int int_, final int supremacy) {
//        if ((level + supremacy) >= (stats.containsKey("reqLevel") ? stats.get("reqLevel") : 0)) {
        if (str >= (stats.containsKey("reqSTR") ? stats.get("reqSTR") : 0) && dex >= (stats.containsKey("reqDEX") ? stats.get("reqDEX") : 0) && luk >= (stats.containsKey("reqLUK") ? stats.get("reqLUK") : 0) && int_ >= (stats.containsKey("reqINT") ? stats.get("reqINT") : 0)) {
            final Integer fameReq = stats.get("reqPOP");
            if (fameReq != null && fame < fameReq) {
                return false;
            }
            return true;
        } else if (GameConstants.isDemonAvenger(job)) {
            return true;
        }
        //      }
        return false;
    }

    public final Map<String, Integer> getEquipStats(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.equipStats;
    }

    public final int getReqLevel(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqLevel")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqLevel");
    }

    public final int getReqJob(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("reqJob")) {
            return 0;
        }
        return getEquipStats(itemId).get("reqJob");
    }

    public final int getSlots(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("tuc")) {
            return 0;
        }
        return getEquipStats(itemId).get("tuc");
    }

    public final Integer getSetItemID(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("setItemID")) {
            return 0;
        }
        return getEquipStats(itemId).get("setItemID");
    }

    public final boolean isOnlyEquip(final int itemId) {
        if (getEquipStats(itemId) == null || !getEquipStats(itemId).containsKey("onlyEquip")) {
            return false;
        }
        return getEquipStats(itemId).get("onlyEquip") > 0;
    }

    public final StructSetItem getSetItem(final int setItemId) {
        return setItems.get(setItemId);
    }

    public final int getCursed(final int itemId, final MapleCharacter player) {
        return getCursed(itemId, player, null);
    }

    public final int getCursed(final int itemId, final MapleCharacter player, Item equip) {
//        if (player.getGMLevel() > 0) {
//            return -1;
//        }
        if (cursedCache.containsKey(itemId)) {
            return cursedCache.get(itemId);
        }
        final MapleData item = getItemData(itemId);
        if (item == null) {
            return -1;
        }
        int success = 0;
        success = MapleDataTool.getIntConvert("info/cursed", item, -1);
        cursedCache.put(itemId, success);
        return success;
    }

    public final List<Integer> getScrollReqs(final int itemId) {
        final List<Integer> ret = new ArrayList<Integer>();
        final MapleData data = getItemData(itemId).getChildByPath("req");

        if (data == null) {
            return ret;
        }
        for (final MapleData req : data.getChildren()) {
            ret.add(MapleDataTool.getInt(req));
        }
        return ret;
    }

    public final Item scrollEquipWithId(final Item equip, final Item scrollId, final boolean ws, final MapleCharacter chr) {
        if (equip.getType() == 1) {
            Equip nEquip = (Equip) equip;
            Equip zeroEquip = null;
            if (GameConstants.isAlphaWeapon(nEquip.getItemId())) {
                zeroEquip = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            }
            final Map<String, Integer> stats = getEquipStats(scrollId.getItemId());
            final Map<String, Integer> eqstats = getEquipStats(equip.getItemId());
            boolean failed = false;
            switch (scrollId.getItemId()) {
                case 2049000: //백의 주문서 1%
                case 2049001: //백의 주문서 3%
                case 2049002: //백의 주문서 5%
                case 2049004: //순백의 주문서 10%
                case 2049005: //백의 주문서 20%
                {
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        failed = true;
                    } else {
                        if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc") + (nEquip.getViciousHammer() > 0 ? 1 : 0)) {
                            nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 1));
                            if (zeroEquip != null) {
                                zeroEquip.setUpgradeSlots((byte) (zeroEquip.getUpgradeSlots() + 1));
                            }
                        }
                    }
                    break;
                }
                case 2049099:
                    startForceScroll(chr.getClient(), nEquip);
                    return equip;
                case 2048900:
                case 2048901:
                case 2048902:
                case 2048903:
                case 2048904:
                case 2048905:
                case 2048906:
                case 2048907:
                case 2048912:
                case 2048913:
                case 2048915:
                case 2048918: {
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        failed = true;
                    } else {
                        MapleQuest quest = MapleQuest.getInstance(41907);
                        String stringa = String.valueOf(GameConstants.getLuckyInfofromItemId(scrollId.getItemId()));
                        chr.setKeyValue(46523, "luckyscroll", stringa);
                        MapleQuestStatus queststatus = new MapleQuestStatus(quest, (byte) 1);
                        queststatus.setCustomData(stringa == null ? "0" : stringa);
                        chr.updateQuest(queststatus, true);
                    }
                    break;
                }
                case 2049006: //저주받은 백의 주문서 1%
                case 2049007: //저주받은 백의 주문서 3%
                case 2049008: //저주받은 백의 주문서 5%
                {
                    if (nEquip.getLevel() + nEquip.getUpgradeSlots() < eqstats.get("tuc") + (nEquip.getViciousHammer() > 0 ? 1 : 0)) {
                        nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() + 2));
                        if (zeroEquip != null) {
                            zeroEquip.setUpgradeSlots((byte) (zeroEquip.getUpgradeSlots() + 2));
                        }
                    }
                    break;
                }
                case 2040727: // 신발 스파이크 주문서 10% - 신발에 미끄럼 방지 옵션 추가. 성공률:10%, 업그레이드 가능 횟수에 영향 없음
                {
                    int flag = nEquip.getFlag();
                    flag += ItemFlag.SPIKES.getValue();
                    nEquip.setFlag(flag);
                    if (zeroEquip != null) {
                        zeroEquip.setFlag(flag);
                    }
                    break;
                }
                /* 8樂 주문서, 저스티스 주문서 */
                case 2046025:
                case 2046026:
                case 2046340:
                case 2046119:
                case 2046341:
                case 2046120:
                case 2046251:
                case 2046054:
                case 2046055:
                case 2046056:
                case 2046057:
                case 2046058:
                case 2046059:
                case 2046138:
                case 2046139:
                case 2046140:
                case 2046374:
                case 2046094:
                case 2046095:
                case 2046162:
                case 2046163:
                case 2046564:
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
//                                return null; //펑
                            }
                        }
                        failed = true;
                    } else {
                        switch (scrollId.getItemId()) {
                            case 2046025: // 8樂 한손 무기 공격력 주문서 20% 
                                nEquip.setWatk((short) (nEquip.getWatk() + 17));
                                break;
                            case 2046026: // 8樂 한손 무기 마력 주문서 20%
                                nEquip.setMatk((short) (nEquip.getMatk() + 17));
                                break;
                            case 2046340: // 후원 악세서리 공격력 주문서 100%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(9, 10)));
                                break;
                            case 2046341: // 후원 악세서리 마력 주문서 100%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(9, 10)));
                                break;
                            case 2046119: // 8樂 두손무기 공격력 주문서 20%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(7, 8)));
                                break;
                            case 2046120: // 8樂 두손무기 마력 주문서 20%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(7, 8)));
                                break;
                            case 2046251: // ZERO 방어구 주문서
                                nEquip.setStr((short) (nEquip.getStr() + 3));
                                nEquip.setInt((short) (nEquip.getInt() + 3));
                                nEquip.setDex((short) (nEquip.getDex() + 3));
                                nEquip.setLuk((short) (nEquip.getLuk() + 3));
                                nEquip.setMatk((short) (nEquip.getMatk() + 9));
                                nEquip.setWatk((short) (nEquip.getWatk() + 9));
                                break;
                            case 2048094: //프리미엄 펫장비 공격력 주문서 100%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(4, 5)));
                                break;
                            case 2048095: //프리미엄 펫장비 마력 주문서 100%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(4, 5)));
                                break;

                            case 2048047: //후원 펫 주문서 공
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(7, 8)));
                                break;
                            case 2048048: //프리미엄 펫장비 마력 주문서 100%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(7, 8)));
                                break;
                            case 2046831: // 홍보 악공
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(6, 7)));
                                break;
                            case 2046832: // 홍보 악마
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(6, 7)));
                                break;
                            case 2046981: // 홍보 무기한손
                                nEquip.setStr((short) (nEquip.getStr() + 7));
                                nEquip.setInt((short) (nEquip.getInt() + 7));
                                nEquip.setDex((short) (nEquip.getDex() + 7));
                                nEquip.setLuk((short) (nEquip.getLuk() + 7));
                                nEquip.setWatk((short) (nEquip.getWatk() + 13));
                                break;
                            case 2047810: // 홍보 무기두손
                                nEquip.setStr((short) (nEquip.getStr() + 7));
                                nEquip.setInt((short) (nEquip.getInt() + 7));
                                nEquip.setDex((short) (nEquip.getDex() + 7));
                                nEquip.setLuk((short) (nEquip.getLuk() + 7));
                                nEquip.setWatk((short) (nEquip.getWatk() + 13));
                                break;
                            case 2046970: // 홍보마력줌서
                                nEquip.setStr((short) (nEquip.getStr() + 7));
                                nEquip.setInt((short) (nEquip.getInt() + 7));
                                nEquip.setDex((short) (nEquip.getDex() + 7));
                                nEquip.setLuk((short) (nEquip.getLuk() + 7));
                                nEquip.setWatk((short) (nEquip.getMatk() + 13));
                                break;
                            case 2048049: // 홍보 펫장비 공
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(4, 5)));
                                break;
                            case 2048050: // 홍보 펫장비 마
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(4, 5)));
                                break;
                            case 2046076: // 후원 한손공
                                nEquip.setStr((short) (nEquip.getStr() + 20));
                                nEquip.setInt((short) (nEquip.getInt() + 20));
                                nEquip.setDex((short) (nEquip.getDex() + 20));
                                nEquip.setLuk((short) (nEquip.getLuk() + 20));
                                nEquip.setWatk((short) (nEquip.getWatk() + 15));
                                break;
                            case 2046150: // 후원 한손공
                                nEquip.setStr((short) (nEquip.getStr() + 20));
                                nEquip.setInt((short) (nEquip.getInt() + 20));
                                nEquip.setDex((short) (nEquip.getDex() + 20));
                                nEquip.setLuk((short) (nEquip.getLuk() + 20));
                                nEquip.setWatk((short) (nEquip.getWatk() + 15));
                                break;
                            case 2046077: // 후원 한손공
                                nEquip.setStr((short) (nEquip.getStr() + 20));
                                nEquip.setInt((short) (nEquip.getInt() + 20));
                                nEquip.setDex((short) (nEquip.getDex() + 20));
                                nEquip.setLuk((short) (nEquip.getLuk() + 20));
                                nEquip.setMatk((short) (nEquip.getMatk() + 15));
                                break;
                            case 2046054: // 저스티스 한손무기 공격력 주문서 20%
                            case 2046055: // 저스티스 한손 무기 마력 주문서 20%
                            case 2046056: // 저스티스 한손 무기 공격력 주문서 40%
                            case 2046057: // 저스티스 한손 무기 마력 주문서 40%
                            case 2046138: // 저스티스 두손무기 공격력 주문서 20%
                            case 2046139: // 저스티스 두손무기 공격력 주문서 40%
                                if (scrollId.getItemId() == 2046055 || scrollId.getItemId() == 2046057) {
                                    nEquip.setMatk((short) (nEquip.getMatk() + 5));
                                } else {
                                    nEquip.setWatk((short) (nEquip.getWatk() + 5));
                                }
                                nEquip.setStr((short) (nEquip.getStr() + 3));
                                nEquip.setDex((short) (nEquip.getDex() + 3));
                                nEquip.setInt((short) (nEquip.getInt() + 3));
                                nEquip.setLuk((short) (nEquip.getLuk() + 3));
                                nEquip.setAcc((short) (nEquip.getAcc() + 15));
                                break;
                            case 2046058: // 저스티스 한손 무기 공격력 주문서 70%
                            case 2046059: // 저스티스 한손 무기 마력 주문서 70%
                            case 2046140: // 저스티스 두손무기 공격력 주문서 70%
                                if (scrollId.getItemId() == 2046059) {
                                    nEquip.setMatk((short) (nEquip.getMatk() + 2));
                                } else {
                                    nEquip.setWatk((short) (nEquip.getWatk() + 2));
                                }
                                nEquip.setStr((short) (nEquip.getStr() + 1));
                                nEquip.setDex((short) (nEquip.getDex() + 1));
                                nEquip.setInt((short) (nEquip.getInt() + 1));
                                nEquip.setLuk((short) (nEquip.getLuk() + 1));
                                nEquip.setAcc((short) (nEquip.getAcc() + 5));
                                break;
                            case 2046374: //비틀린 시간의 파편
                                nEquip.setWatk((short) (nEquip.getWatk() + 3));
                                nEquip.setMatk((short) (nEquip.getMatk() + 3));
                                nEquip.setWdef((short) (nEquip.getWdef() + 25));
                                nEquip.setMdef((short) (nEquip.getMdef() + 25));
                                nEquip.setStr((short) (nEquip.getStr() + 3));
                                nEquip.setDex((short) (nEquip.getDex() + 3));
                                nEquip.setInt((short) (nEquip.getInt() + 3));
                                nEquip.setLuk((short) (nEquip.getLuk() + 3));
                                nEquip.setAvoid((short) (nEquip.getAvoid() + 30));
                                nEquip.setAcc((short) (nEquip.getAcc() + 30));
                                nEquip.setSpeed((short) (nEquip.getSpeed() + 3));
                                nEquip.setJump((short) (nEquip.getJump() + 2));
                                nEquip.setMp((short) (nEquip.getMp() + 25));
                                nEquip.setHp((short) (nEquip.getHp() + 25));
                                break;
                            case 2046094: // 9주년 한손 무기 공격력 주문서 10%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(7, 9)));
                                break;
                            case 2046095: // 9주년 한손 무기 마력 주문서 10%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(7, 9)));
                                break;
                            case 2046162: // 9주년 두손 무기 공격력 주문서 10%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(7, 9)));
                                break;
                            case 2046163: // 9주년 두손 무기 마력 주문서 10%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(7, 9)));
                                break;
                            case 5530336: // 악세서리 공격력 스크롤 100%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(2, 4)));
                                break;
                            case 5530337: // 악세서리 마력 스크롤 100%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(2, 4)));
                                break;
                            case 5530338: // 펫장비 공격력 스크롤 100%
                                nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(2, 4)));
                                break;
                            case 5530339: // 펫장비 마력 스크롤 100%
                                nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(2, 4)));
                                break;
                            case 2046564: // 9주년 방어구 강화 주문서 10%
                                nEquip.setStr((short) (nEquip.getStr() + 5));
                                nEquip.setInt((short) (nEquip.getInt() + 5));
                                nEquip.setDex((short) (nEquip.getDex() + 5));
                                nEquip.setLuk((short) (nEquip.getLuk() + 5));
                                break;
                        }
                    }
                    break;
                case 2046996:
                case 2047818: {
                    short watk = (short) Randomizer.rand(12, 15);
                    short str = (short) Randomizer.rand(15, 20);
                    short dex = (short) Randomizer.rand(15, 20);
                    short int_ = (short) Randomizer.rand(15, 20);
                    short luk = (short) Randomizer.rand(15, 20);

                    List<Pair<EnchantFlag, Integer>> statz = new ArrayList<>();
                    nEquip.addWatk(watk);
                    statz.add(new Pair<>(EnchantFlag.Watk, (int) watk));

                    nEquip.addStr(str);
                    statz.add(new Pair<>(EnchantFlag.Str, (int) str));

                    nEquip.addDex(dex);
                    statz.add(new Pair<>(EnchantFlag.Dex, (int) dex));

                    nEquip.addInt(int_);
                    statz.add(new Pair<>(EnchantFlag.Int, (int) int_));

                    nEquip.addLuk(luk);
                    statz.add(new Pair<>(EnchantFlag.Luk, (int) luk));

                    StarForceStats sf = new StarForceStats(statz);
                    chr.getClient().getSession().writeAndFlush(CField.showScrollOption(nEquip.getItemId(), scrollId.getItemId(), sf));
                    break;
                }
                case 2046997: {
                    short matk = (short) Randomizer.rand(12, 15);
                    short str = (short) Randomizer.rand(15, 20);
                    short dex = (short) Randomizer.rand(15, 20);
                    short int_ = (short) Randomizer.rand(15, 20);
                    short luk = (short) Randomizer.rand(15, 20);

                    List<Pair<EnchantFlag, Integer>> statz = new ArrayList<>();
                    nEquip.addMatk(matk);
                    statz.add(new Pair<>(EnchantFlag.Matk, (int) matk));

                    nEquip.addStr(str);
                    statz.add(new Pair<>(EnchantFlag.Str, (int) str));

                    nEquip.addDex(dex);
                    statz.add(new Pair<>(EnchantFlag.Dex, (int) dex));

                    nEquip.addInt(int_);
                    statz.add(new Pair<>(EnchantFlag.Int, (int) int_));

                    nEquip.addLuk(luk);
                    statz.add(new Pair<>(EnchantFlag.Luk, (int) luk));

                    StarForceStats sf = new StarForceStats(statz);
                    chr.getClient().getSession().writeAndFlush(CField.showScrollOption(nEquip.getItemId(), scrollId.getItemId(), sf));
                    break;
                }
                case 2046841:
                case 2046842:
                case 2046967:
                case 2046971:
                case 2047803:
                case 2047917:
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
//                                return null; //펑
                            }
                        }
                        failed = true;
                    } else {
                        switch (scrollId.getItemId()) {
                            case 2046841:
                                nEquip.setWatk((short) (nEquip.getWatk() + 1));
                                break;
                            case 2046842:
                                nEquip.setMatk((short) (nEquip.getMatk() + 1));
                                break;
                            case 2046967:
                                nEquip.setWatk((short) (nEquip.getWatk() + 9));
                                nEquip.setStr((short) (nEquip.getStr() + 3));
                                nEquip.setInt((short) (nEquip.getInt() + 3));
                                nEquip.setDex((short) (nEquip.getDex() + 3));
                                nEquip.setLuk((short) (nEquip.getLuk() + 3));
                                break;
                            case 2046971:
                                nEquip.setMatk((short) (nEquip.getMatk() + 9));
                                nEquip.setStr((short) (nEquip.getStr() + 3));
                                nEquip.setInt((short) (nEquip.getInt() + 3));
                                nEquip.setDex((short) (nEquip.getDex() + 3));
                                nEquip.setLuk((short) (nEquip.getLuk() + 3));
                                break;
                            case 2047803:
                                nEquip.setWatk((short) (nEquip.getWatk() + 9));
                                nEquip.setStr((short) (nEquip.getStr() + 3));
                                nEquip.setInt((short) (nEquip.getInt() + 3));
                                nEquip.setDex((short) (nEquip.getDex() + 3));
                                nEquip.setLuk((short) (nEquip.getLuk() + 3));
                                break;
                            case 2047917:
                                nEquip.setStr((short) (nEquip.getStr() + 9));
                                nEquip.setInt((short) (nEquip.getInt() + 9));
                                nEquip.setDex((short) (nEquip.getDex() + 9));
                                nEquip.setLuk((short) (nEquip.getLuk() + 9));
                                break;
                        }
                    }
                    break;
                case 2049701:
                case 2049702:
                case 2049703:
                case 2049700:
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
//                                return null; //펑
                            }
                        }
                        failed = true;
                    } else {
                        if (nEquip.getState() <= 17) {
                            nEquip.setState((byte) 2);
                            if (Randomizer.nextInt(100) < 30) {
                                nEquip.setLines((byte) 3);
                            } else {
                                nEquip.setLines((byte) 2);
                            }
                        }
                    }
                    break;
                case 2049750:
                case 2049751:
                case 2049752:
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
//                                return null; //펑
                            }
                        }
                        failed = true;
                    } else {
                        if (nEquip.getState() <= 19) {
                            nEquip.setState((byte) 3);
                            if (Randomizer.nextInt(100) < 30) {
                                nEquip.setLines((byte) 3);
                            } else {
                                nEquip.setLines((byte) 2);
                            }
                        }
                    }
                    break;
                case 2048306:
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
//                                return null; //펑
                            }
                        }
                        failed = true;
                    } else {
                        if (nEquip.getState() <= 17) {
                            nEquip.setState((byte) 4);
                            if (Randomizer.nextInt(100) < 30) {
                                nEquip.setLines((byte) 3);
                            } else {
                                nEquip.setLines((byte) 2);
                            }
                        }
                    }
                    break;
                case 2531000: // 프로텍트
                case 2531001:
                case 2531005: {
                    int flag = nEquip.getFlag();
                    flag += ItemFlag.PROTECT_SHIELD.getValue();
                    nEquip.setFlag(flag);
                    break;
                }
                case 2532000: // 세이프티
                case 2532002:
                case 2532005: {
                    int flag = nEquip.getFlag();
                    flag += ItemFlag.SAFETY_SHIELD.getValue();
                    nEquip.setFlag(flag);
                    break;
                }
                case 2533000: // 리커버리
                {
                    int flag = nEquip.getFlag();
                    flag += ItemFlag.RECOVERY_SHIELD.getValue();
                    nEquip.setFlag(flag);
                    break;
                }
                case 2643128:
                    if (nEquip.getItemId() == 1114300) {
                        nEquip.addStr((short) 1);
                        nEquip.addDex((short) 1);
                        nEquip.addInt((short) 1);
                        nEquip.addLuk((short) 1);
                        nEquip.addWatk((short) 1);
                        nEquip.addMatk((short) 1);
                        nEquip.addHp((short) 100);
                        nEquip.addMp((short) 100);
                    }
                    break;
                case 2643130:
                    if (nEquip.getItemId() == 1114303) {
                        nEquip.addStr((short) 1);
                        nEquip.addDex((short) 1);
                        nEquip.addInt((short) 1);
                        nEquip.addLuk((short) 1);
                        nEquip.addWatk((short) 1);
                        nEquip.addMatk((short) 1);
                        nEquip.addHp((short) 100);
                        nEquip.addMp((short) 100);
                    }
                    break;
                case 5063000:
                case 2049704: // 레전드리 잠재 주문서/에픽 40%에 적용 - 120705 추가
                    if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                        if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr, nEquip))) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
//                                return null; //펑
                            }
                        }
                        failed = true;
                    } else {
                        if (nEquip.getState() <= 17) {
                            nEquip.setState((byte) 4);
                            if (Randomizer.nextInt(100) < 30) {
                                nEquip.setLines((byte) 3);
                            } else {
                                nEquip.setLines((byte) 2);
                            }
                        }
                    }
                    break;
                case 2530000:
                case 2530001:
                case 2530002: // 럭키데이
                {
                    int flag = nEquip.getFlag();
                    flag += ItemFlag.LUCKY_PROTECT_SHIELD.getValue();
                    nEquip.setFlag(flag);
                    break;
                }
                case 2046991:
                case 2046992:
                case 2047814: {
                    int lucky = 9 + Randomizer.rand(0, 2);
                    nEquip.addStr((short) 3);
                    nEquip.addDex((short) 3);
                    nEquip.addInt((short) 3);
                    nEquip.addLuk((short) 3);
                    if (scrollId.getItemId() == 2046992) {
                        nEquip.addMatk((short) lucky);
                    } else {
                        nEquip.addWatk((short) lucky);
                    }
                    break;
                }
                case 2046856:
                case 2046857: {
                    int lucky = 4 + (Randomizer.isSuccess(15) ? 1 : 0);
                    if (scrollId.getItemId() == 2046857) {
                        nEquip.addMatk((short) lucky);
                    } else {
                        nEquip.addWatk((short) lucky);
                    }
                    break;
                }
                case 2048804:
                case 2048094: {
                    nEquip.setWatk((short) (nEquip.getWatk() + Randomizer.rand(4, 5)));
                    break;
                }
                case 2048805:
                case 2048095: {
                    nEquip.setMatk((short) (nEquip.getMatk() + Randomizer.rand(4, 5)));
                    break;
                }
                case 2048809: {
                    nEquip.setWatk((short) (nEquip.getWatk() + 2));
                    break;
                }
                case 2048810: {
                    nEquip.setMatk((short) (nEquip.getMatk() + 2));
                    break;
                }
                case 2645000:
                case 2645001: {
                    switch (nEquip.getItemId()) {
                        case 1113072:
                        case 1113073:
                        case 1113074:
                        case 1032220:
                        case 1032221:
                        case 1032222:
                        case 1122264:
                        case 1122265:
                        case 1122266:
                        case 1132243:
                        case 1132244:
                        case 1132245:
                            nEquip.addStr((short) 3);
                            nEquip.addDex((short) 3);
                            nEquip.addInt((short) 3);
                            nEquip.addLuk((short) 3);
                            if (scrollId.getItemId() == 2645000) {
                                nEquip.addWatk((short) 3);
                            } else {
                                nEquip.addMatk((short) 3);
                            }
                            break;
                    }
                    break;
                }
                case 2645002:
                case 2645003: {
                    switch (nEquip.getItemId()) {
                        case 1113075:
                        case 1032223:
                        case 1122267:
                        case 1132246:
                            nEquip.addStr((short) Randomizer.rand(10, 30));
                            nEquip.addDex((short) Randomizer.rand(10, 30));
                            nEquip.addInt((short) Randomizer.rand(10, 30));
                            nEquip.addLuk((short) Randomizer.rand(10, 30));
                            if (scrollId.getItemId() == 2645002) {
                                nEquip.addWatk((short) Randomizer.rand(10, 20));
                            } else {
                                nEquip.addMatk((short) Randomizer.rand(10, 20));
                            }
                            break;
                    }
                    break;
                }
                default: {
                    if (GameConstants.isChaosScroll(scrollId.getItemId())) {
                        if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                            if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr, nEquip))) {
                                if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                    chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                                } else {
//                                    return null; //펑
                                }
                            }
                            failed = true;
                        } else {
                            int start = 0;
                            if (scrollId.getItemId() == 2049153)
                                start = 4;
                            final int end = GameConstants.getChaosNumber(scrollId.getItemId()); 
                            final boolean a = scrollId.getItemId() == 2049122 || scrollId.getItemId() == 2049153; //긍정의 혼돈의 주문서
                            if (nEquip.getStr() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1);
                                nEquip.addStr((short) rand);
                            }
                            if (nEquip.getDex() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1);
                                nEquip.addDex((short) rand);
                            }
                            if (nEquip.getInt() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1);
                                nEquip.addInt((short) rand);
                            }
                            if (nEquip.getLuk() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1);
                                nEquip.addLuk((short) rand);
                            }
                            if (nEquip.getWatk() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1);
                                nEquip.addWatk((short) rand);
                            }
                            if (nEquip.getMatk() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1);
                                nEquip.addMatk((short) rand);
                            }
                            if (nEquip.getHp() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1) * 30;
                                nEquip.addHp((short) rand);
                            }
                            if (nEquip.getMp() > 0) {
                                int rand = Randomizer.rand(start, end) * (a ? 1 : Randomizer.nextBoolean() ? 1 : -1) * 30;
                                nEquip.addMp((short) rand);
                            }
                        }
                        break;
                    } else if ((scrollId.getItemId() == 2049360) || (scrollId.getItemId() == 2049361)) { //놀장강
                        int chane; // 확률
                        final MapleData IData = getItemData(nEquip.getItemId());
                        final MapleData info = IData.getChildByPath("info");
                        int level = MapleDataTool.getInt("reqLevel", info, 0);
                        if (level > 200) {
                            chr.dropMessage(6, "150레벨 이하의 장비 아이템에만 사용하실 수 있습니다.");
                            break;
                        }
                        switch (nEquip.getEnhance()) { // 성당 강화 확률
                            case 0:
                                chane = 60;
                                break;
                            case 1:
                                chane = 55;
                                break;
                            case 2:
                                chane = 50;
                                break;
                            case 3:
                                chane = 40;
                                break;
                            case 4:
                                chane = 30;
                                break;
                            case 5:
                                chane = 20;
                                break;
                            case 6:
                                chane = 19;
                                break;
                            case 7:
                                chane = 18;
                                break;
                            case 8:
                                chane = 17;
                                break;
                            case 9:
                                chane = 16;
                                break;
                            case 10:
                                chane = 14;
                                break;
                            case 11:
                                chane = 12;
                                break;
                            default: //13성 이상
                                chane = 10;
                                break;
                        }
                        if (chr.getGMLevel() > 0) {
                            chane = 100;
                        }
                        if (!Randomizer.isSuccess(chane)) {
                            if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                            } else {
                                return null; // 펑
                            }
                        } else {

                            int ordinary;
                            if (EquipmentEnchant.isMagicWeapon(GameConstants.getWeaponType(nEquip.getItemId()))) {
                                ordinary = nEquip.getMatk();
                            } else {
                                ordinary = nEquip.getWatk();
                            }

                            //환불 값을 '제외'한 무기의 공격력/마력이므로 지워주자...
                            if (nEquip.getFire() > 0) {
                                long fire1 = (nEquip.getFire() % 1000 / 10);
                                long fire2 = (nEquip.getFire() % 1000000 / 10000);
                                long fire3 = (nEquip.getFire() % 1000000000 / 10000000);
                                long fire4 = (nEquip.getFire() % 1000000000000L / 10000000000L);
                                for (int i = 0; i < 4; ++i) {
                                    int dat = (int) (i == 0 ? fire1 : i == 1 ? fire2 : i == 2 ? fire3 : fire4);
                                    if (dat == (EquipmentEnchant.isMagicWeapon(GameConstants.getWeaponType(nEquip.getItemId())) ? 18 : 17)) {
                                        int value;
                                        if (i == 0) {
                                            value = (int) (nEquip.getFire() % 10 / 1);
                                        } else if (i == 1) {
                                            value = (int) (nEquip.getFire() % 10000 / 1000);
                                        } else if (i == 2) {
                                            value = (int) (nEquip.getFire() % 10000000 / 1000000);
                                        } else {
                                            value = (int) (nEquip.getFire() % 10000000000L / 1000000000L);
                                        }
                                        switch (value) {
                                            case 3:
                                                if (getReqLevel(nEquip.getItemId()) <= 150) {
                                                    ordinary -= ((short) (((ordinary * 1200) / 10000) + 1));
                                                } else if (getReqLevel(nEquip.getItemId()) <= 160) {
                                                    ordinary -= ((short) (((ordinary * 1500) / 10000) + 1));
                                                } else {
                                                    ordinary -= ((short) (((ordinary * 1800) / 10000) + 1));
                                                }
                                                break;
                                            case 4:
                                                if (getReqLevel(nEquip.getItemId()) <= 150) {
                                                    ordinary -= ((short) (((ordinary * 1760) / 10000) + 1));
                                                } else if (getReqLevel(nEquip.getItemId()) <= 160) {
                                                    ordinary -= ((short) (((ordinary * 2200) / 10000) + 1));
                                                } else {
                                                    ordinary -= ((short) (((ordinary * 2640) / 10000) + 1));
                                                }
                                                break;
                                            case 5:
                                                if (getReqLevel(nEquip.getItemId()) <= 150) {
                                                    ordinary -= ((short) (((ordinary * 2420) / 10000) + 1));
                                                } else if (getReqLevel(nEquip.getItemId()) <= 160) {
                                                    ordinary -= ((short) (((ordinary * 3025) / 10000) + 1));
                                                } else {
                                                    ordinary -= ((short) (((ordinary * 3630) / 10000) + 1));
                                                }
                                                break;
                                            case 6:
                                                if (getReqLevel(nEquip.getItemId()) <= 150) {
                                                    ordinary -= ((short) (((ordinary * 3200) / 10000) + 1));
                                                } else if (getReqLevel(nEquip.getItemId()) <= 160) {
                                                    ordinary -= ((short) (((ordinary * 4000) / 10000) + 1));
                                                } else {
                                                    ordinary -= ((short) (((ordinary * 4800) / 10000) + 1));
                                                }
                                                break;
                                            case 7:
                                                if (getReqLevel(nEquip.getItemId()) <= 150) {
                                                    ordinary -= ((short) (((ordinary * 4100) / 10000) + 1));
                                                } else if (getReqLevel(nEquip.getItemId()) <= 160) {
                                                    ordinary -= ((short) (((ordinary * 5125) / 10000) + 1));
                                                } else {
                                                    ordinary -= ((short) (((ordinary * 6150) / 10000) + 1));
                                                }
                                                break;
                                        }
                                    }
                                }
                            }
                            int weaponwatk = (int) ((ordinary / 50) + 1); // 무기 공격력 변동량
                            int weaponmatk = (int) ((ordinary / 50) + 1); // 무기 마력 변동량
                            int data[];
                            int reallevel = (int) ((level / 10) * 10);
                            switch (reallevel) {
                                case 80:
                                    data = new int[]{2, 3, 5, 8, 12, 2, 3, 4, 5, 6, 7, 9, 10, 11};
                                    break;
                                case 90:
                                    data = new int[]{4, 5, 7, 10, 14, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13};
                                    break;
                                case 100:
                                    data = new int[]{7, 8, 10, 13, 17, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14};
                                    break;
                                case 110:
                                    data = new int[]{9, 10, 12, 15, 19, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15};
                                    break;
                                case 120:
                                    data = new int[]{12, 13, 15, 18, 22, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16};
                                    break;
                                case 130:
                                    data = new int[]{14, 15, 17, 20, 24, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17};
                                    break;
                                case 140:
                                    data = new int[]{17, 18, 20, 23, 27, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18};
                                    break;
                                case 150:
                                    data = new int[]{19, 20, 22, 25, 29, 9, 10, 11, 12, 13, 14, 16, 17, 18, 19};
                                    break;
                                default: // 80제 미만
                                    data = new int[]{1, 2, 4, 7, 11, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11};
                                    break;
                            }
                            if (nEquip.getEnhance() < 5) { // 5성 미만일시
                                nEquip.addStr((short) data[nEquip.getEnhance()]);
                                nEquip.addDex((short) data[nEquip.getEnhance()]);
                                nEquip.addInt((short) data[nEquip.getEnhance()]);
                                nEquip.addLuk((short) data[nEquip.getEnhance()]);
                            } else {
                                nEquip.addWatk((short) data[nEquip.getEnhance()]);
                                nEquip.addMatk((short) data[nEquip.getEnhance()]);
                            }
                            if (GameConstants.isWeapon(nEquip.getItemId())) {
                                nEquip.addWatk((short) weaponwatk);
                                nEquip.addMatk((short) weaponmatk);
                                if (Randomizer.nextBoolean()) {
                                    nEquip.addWatk((short) 1);
                                    nEquip.addMatk((short) 1);
                                }
                            } else if (GameConstants.isAccessory(nEquip.getItemId())) {
                                if (Randomizer.nextBoolean()) {
                                    if (level < 120) {
                                        if (nEquip.getEnhance() < 5) {
                                            nEquip.addStr((short) 1);
                                            nEquip.addDex((short) 1);
                                            nEquip.addInt((short) 1);
                                            nEquip.addLuk((short) 1);
                                        } else {
                                            nEquip.addStr((short) 2);
                                            nEquip.addDex((short) 2);
                                            nEquip.addInt((short) 2);
                                            nEquip.addLuk((short) 2);
                                        }
                                    } else {
                                        if (nEquip.getEnhance() < 5) {
                                            nEquip.addStr((short) Randomizer.rand(1, 2));
                                            nEquip.addDex((short) Randomizer.rand(1, 2));
                                            nEquip.addInt((short) Randomizer.rand(1, 2));
                                            nEquip.addLuk((short) Randomizer.rand(1, 2));
                                        } else {
                                            nEquip.addStr((short) 2);
                                            nEquip.addDex((short) 2);
                                            nEquip.addInt((short) 2);
                                            nEquip.addLuk((short) 2);
                                        }
                                    }
                                }
                            }
                            nEquip.setEnhance((byte) (nEquip.getEnhance() + 1));
                            nEquip.setEquipmentType(nEquip.getEquipmentType() | 0x600);
                            break;
                        }
                    } else if (scrollId.getItemId() == 2049370 || scrollId.getItemId() == 2049371 || scrollId.getItemId() == 2049372 || scrollId.getItemId() == 2049376 || scrollId.getItemId() == 2049377 || scrollId.getItemId() == 2049380) {
                        int max = 0;
                        switch (scrollId.getItemId()) {
                            case 2049370:
                                max = 12;
                                break;
                            case 2049371:
                                max = 17;
                                break;
                            case 2049372:
                                max = 15;
                                break;
                            case 2049376:
                                max = 20;
                                break;
                            case 2049377:
                                max = 22;
                                break;
                            case 2049380:
                                max = 25;
                                break;
                        }

                        boolean isSuperiol = isSuperial(nEquip.getItemId()).left != null;

                        int reqLevel = getReqLevel(nEquip.getItemId()), maxEnhance;

                        if (reqLevel < 95) {
                            maxEnhance = isSuperiol ? 3 : 5;
                        } else if (reqLevel <= 107) {
                            maxEnhance = isSuperiol ? 5 : 8;
                        } else if (reqLevel <= 119) {
                            maxEnhance = isSuperiol ? 8 : 10;
                        } else if (reqLevel <= 129) {
                            maxEnhance = isSuperiol ? 10 : 15;
                        } else if (reqLevel <= 139) {
                            maxEnhance = isSuperiol ? 12 : 20;
                        } else {
                            maxEnhance = isSuperiol ? 15 : 25;
                        }

                        if (maxEnhance < max) {
                            max = maxEnhance;
                        }

                        while (nEquip.getEnhance() < max) {
                            StarForceStats statz = starForceStats(nEquip);
                            nEquip.setEnchantBuff((short) 0);
                            nEquip.setEnhance((byte) (nEquip.getEnhance() + 1));
                            for (Pair<EnchantFlag, Integer> stat : statz.getStats()) {
                                if (EnchantFlag.Watk.check(stat.left.getValue())) {
                                    nEquip.setEnchantWatk((short) (nEquip.getEnchantWatk() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantWatk((short) (zeroEquip.getEnchantWatk() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Matk.check(stat.left.getValue())) {
                                    nEquip.setEnchantMatk((short) (nEquip.getEnchantMatk() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantMatk((short) (zeroEquip.getEnchantMatk() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Str.check(stat.left.getValue())) {
                                    nEquip.setEnchantStr((short) (nEquip.getEnchantStr() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantStr((short) (zeroEquip.getEnchantStr() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Dex.check(stat.left.getValue())) {
                                    nEquip.setEnchantDex((short) (nEquip.getEnchantDex() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantDex((short) (zeroEquip.getEnchantDex() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Int.check(stat.left.getValue())) {
                                    nEquip.setEnchantInt((short) (nEquip.getEnchantInt() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantInt((short) (zeroEquip.getEnchantInt() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Luk.check(stat.left.getValue())) {
                                    nEquip.setEnchantLuk((short) (nEquip.getEnchantLuk() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantLuk((short) (zeroEquip.getEnchantLuk() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Wdef.check(stat.left.getValue())) {
                                    nEquip.setEnchantWdef((short) (nEquip.getEnchantWdef() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantWdef((short) (zeroEquip.getEnchantWdef() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Mdef.check(stat.left.getValue())) {
                                    nEquip.setEnchantMdef((short) (nEquip.getEnchantMdef() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantMdef((short) (zeroEquip.getEnchantMdef() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Hp.check(stat.left.getValue())) {
                                    nEquip.setEnchantHp((short) (nEquip.getEnchantHp() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantHp((short) (zeroEquip.getEnchantHp() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Mp.check(stat.left.getValue())) {
                                    nEquip.setEnchantMp((short) (nEquip.getEnchantMp() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantMp((short) (zeroEquip.getEnchantMp() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Acc.check(stat.left.getValue())) {
                                    nEquip.setEnchantAcc((short) (nEquip.getEnchantAcc() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantAcc((short) (zeroEquip.getEnchantAcc() + stat.right));
                                    }
                                }

                                if (EnchantFlag.Avoid.check(stat.left.getValue())) {
                                    nEquip.setEnchantAvoid((short) (nEquip.getEnchantAvoid() + stat.right));
                                    if (zeroEquip != null) {
                                        zeroEquip.setEnchantAvoid((short) (zeroEquip.getEnchantAvoid() + stat.right));
                                    }
                                }
                            }
                        }
                    } else if (GameConstants.isEquipScroll(scrollId.getItemId())) {
                        if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                            if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr, nEquip))) {
                                if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                    chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                                } else {
                                    return null; //펑
                                }
                            }
                            failed = true;
                        } else {
                            for (int i = 1; i <= MapleDataTool.getIntConvert("info/forceUpgrade", getItemData(scrollId.getItemId()), 1); i++) {
                                if (GameConstants.isSuperior(nEquip.getItemId())) {
                                    int slevel = getReqLevel(nEquip.getItemId());
                                    int senhance = nEquip.getEnhance();
                                    if (senhance < 1) {
                                        nEquip.setStr((short) (nEquip.getStr() + (slevel > 70 ? 2 : slevel > 100 ? 9 : slevel > 140 ? 19 : 1)));
                                        nEquip.setDex((short) (nEquip.getDex() + (slevel > 70 ? 2 : slevel > 100 ? 9 : slevel > 140 ? 19 : 1)));
                                        nEquip.setInt((short) (nEquip.getInt() + (slevel > 70 ? 2 : slevel > 100 ? 9 : slevel > 140 ? 19 : 1)));
                                        nEquip.setLuk((short) (nEquip.getLuk() + (slevel > 70 ? 2 : slevel > 100 ? 9 : slevel > 140 ? 19 : 1)));
                                        nEquip.setEnhance((byte) 1);
                                    } else if (senhance == 1) {
                                        nEquip.setStr((short) (nEquip.getStr() + (slevel > 70 ? 3 : slevel > 100 ? 10 : slevel > 140 ? 20 : 2)));
                                        nEquip.setDex((short) (nEquip.getDex() + (slevel > 70 ? 3 : slevel > 100 ? 10 : slevel > 140 ? 20 : 2)));
                                        nEquip.setInt((short) (nEquip.getInt() + (slevel > 70 ? 3 : slevel > 100 ? 10 : slevel > 140 ? 20 : 2)));
                                        nEquip.setLuk((short) (nEquip.getLuk() + (slevel > 70 ? 3 : slevel > 100 ? 10 : slevel > 140 ? 20 : 2)));
                                        nEquip.setEnhance((byte) 2);
                                    } else if (senhance == 2) {
                                        nEquip.setStr((short) (nEquip.getStr() + (slevel > 70 ? 5 : slevel > 100 ? 12 : slevel > 140 ? 22 : 4)));
                                        nEquip.setDex((short) (nEquip.getDex() + (slevel > 70 ? 5 : slevel > 100 ? 12 : slevel > 140 ? 22 : 4)));
                                        nEquip.setInt((short) (nEquip.getInt() + (slevel > 70 ? 5 : slevel > 100 ? 12 : slevel > 140 ? 22 : 4)));
                                        nEquip.setLuk((short) (nEquip.getLuk() + (slevel > 70 ? 5 : slevel > 100 ? 12 : slevel > 140 ? 22 : 4)));
                                        nEquip.setEnhance((byte) 3);
                                    } else if (senhance == 3) {
                                        nEquip.setStr((short) (nEquip.getStr() + (slevel > 70 ? 8 : slevel > 100 ? 15 : slevel > 140 ? 25 : 7)));
                                        nEquip.setDex((short) (nEquip.getDex() + (slevel > 70 ? 8 : slevel > 100 ? 15 : slevel > 140 ? 25 : 7)));
                                        nEquip.setInt((short) (nEquip.getInt() + (slevel > 70 ? 8 : slevel > 100 ? 15 : slevel > 140 ? 25 : 7)));
                                        nEquip.setLuk((short) (nEquip.getLuk() + (slevel > 70 ? 8 : slevel > 100 ? 15 : slevel > 140 ? 25 : 7)));
                                        nEquip.setEnhance((byte) 4);
                                    } else if (senhance == 4) {
                                        nEquip.setStr((short) (nEquip.getStr() + (slevel > 70 ? 12 : slevel > 100 ? 19 : slevel > 140 ? 29 : 11)));
                                        nEquip.setDex((short) (nEquip.getDex() + (slevel > 70 ? 12 : slevel > 100 ? 19 : slevel > 140 ? 29 : 11)));
                                        nEquip.setInt((short) (nEquip.getInt() + (slevel > 70 ? 12 : slevel > 100 ? 19 : slevel > 140 ? 29 : 11)));
                                        nEquip.setLuk((short) (nEquip.getLuk() + (slevel > 70 ? 12 : slevel > 100 ? 19 : slevel > 140 ? 29 : 11)));
                                        nEquip.setEnhance((byte) 5);
                                    } else if (senhance == 5) {
                                        nEquip.setWatk((short) (nEquip.getWatk() + (slevel > 70 ? 2 : slevel > 100 ? 5 : slevel > 140 ? 9 : 2)));
                                        nEquip.setMatk((short) (nEquip.getMatk() + (slevel > 70 ? 2 : slevel > 100 ? 5 : slevel > 140 ? 9 : 2)));
                                        nEquip.setEnhance((byte) 6);
                                    } else if (senhance == 6) {
                                        nEquip.setWatk((short) (nEquip.getWatk() + (slevel > 70 ? 3 : slevel > 100 ? 6 : slevel > 140 ? 10 : 3)));
                                        nEquip.setMatk((short) (nEquip.getMatk() + (slevel > 70 ? 3 : slevel > 100 ? 6 : slevel > 140 ? 10 : 3)));
                                        nEquip.setEnhance((byte) 7);
                                    } else if (senhance == 7) {
                                        nEquip.setWatk((short) (nEquip.getWatk() + (slevel > 70 ? 4 : slevel > 100 ? 7 : slevel > 140 ? 11 : 5)));
                                        nEquip.setMatk((short) (nEquip.getMatk() + (slevel > 70 ? 4 : slevel > 100 ? 7 : slevel > 140 ? 11 : 5)));
                                        nEquip.setEnhance((byte) 8);
                                    } else if (senhance == 8) {
                                        nEquip.setWatk((short) (nEquip.getWatk() + (slevel > 70 ? 5 : slevel > 100 ? 8 : slevel > 140 ? 12 : 8)));
                                        nEquip.setMatk((short) (nEquip.getMatk() + (slevel > 70 ? 5 : slevel > 100 ? 8 : slevel > 140 ? 12 : 8)));
                                        nEquip.setEnhance((byte) 9);
                                    } else if (senhance == 9) {
                                        nEquip.setWatk((short) (nEquip.getWatk() + (slevel > 70 ? 6 : slevel > 100 ? 9 : slevel > 140 ? 13 : 12)));
                                        nEquip.setMatk((short) (nEquip.getMatk() + (slevel > 70 ? 6 : slevel > 100 ? 9 : slevel > 140 ? 13 : 12)));
                                        nEquip.setEnhance((byte) 10);
                                    } else {
                                        nEquip.setStr((short) (nEquip.getStr() + (slevel > 70 ? 15 : slevel > 100 ? 20 : slevel > 140 ? 30 : 10)));
                                        nEquip.setDex((short) (nEquip.getDex() + (slevel > 70 ? 15 : slevel > 100 ? 20 : slevel > 140 ? 30 : 10)));
                                        nEquip.setInt((short) (nEquip.getInt() + (slevel > 70 ? 15 : slevel > 100 ? 20 : slevel > 140 ? 30 : 10)));
                                        nEquip.setLuk((short) (nEquip.getLuk() + (slevel > 70 ? 15 : slevel > 100 ? 20 : slevel > 140 ? 30 : 10)));
                                        nEquip.setEnhance((byte) (nEquip.getEnhance() + 1));
                                    }
                                } else {
                                    if (nEquip.getStr() > 0) {
                                        nEquip.setStr((short) (nEquip.getStr() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(0, 1))));
                                    }
                                    if (nEquip.getDex() > 0) {
                                        nEquip.setDex((short) (nEquip.getDex() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(0, 1))));
                                    }
                                    if (nEquip.getInt() > 0) {
                                        nEquip.setInt((short) (nEquip.getInt() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(0, 1))));
                                    }
                                    if (nEquip.getLuk() > 0) {
                                        nEquip.setLuk((short) (nEquip.getLuk() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(0, 1))));
                                    }
                                    if (nEquip.getWatk() > 0) {
                                        nEquip.setWatk((short) (nEquip.getWatk() + getEquipLevel(getReqLevel(nEquip.getItemId()))));
                                    }
                                    if (nEquip.getWdef() > 0) {
                                        nEquip.setWdef((short) (nEquip.getWdef() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(1, 2))));
                                    }
                                    if (nEquip.getMatk() > 0) {
                                        nEquip.setMatk((short) (nEquip.getMatk() + getEquipLevel(getReqLevel(nEquip.getItemId()))));
                                    }
                                    if (nEquip.getMdef() > 0) {
                                        nEquip.setMdef((short) (nEquip.getMdef() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(1, 2))));
                                    }
                                    if (nEquip.getAcc() > 0) {
                                        nEquip.setAcc((short) (nEquip.getAcc() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(1, 2))));
                                    }
                                    if (nEquip.getAvoid() > 0) {
                                        nEquip.setAvoid((short) (nEquip.getAvoid() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(1, 2))));
                                    }
                                    if (nEquip.getHp() > 0) {
                                        nEquip.setHp((short) (nEquip.getHp() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(1, 2))));
                                    }
                                    if (nEquip.getMp() > 0) {
                                        nEquip.setMp((short) (nEquip.getMp() + getEquipLevel(getReqLevel(nEquip.getItemId()) + Randomizer.rand(1, 2))));
                                    }
                                    nEquip.setEnhance((byte) (nEquip.getEnhance() + 1)); //이게 별붙는 부분
                                }
                            }
                            break;
                        }
                    } else if (GameConstants.isPotentialScroll(scrollId.getItemId())) {
                        if (nEquip.getState() == 0) {
                            if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                                if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                                    if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                        chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                                    } else {
//                                        return null; //펑
                                    }
                                }
                                failed = true;
                            } else {
                                nEquip.setState((byte) 1);
                            }
                        }
                        break;
                    } else if (GameConstants.isRebirthFireScroll(scrollId.getItemId())) {
                        nEquip.resetRebirth(getReqLevel(nEquip.getItemId()));
                        nEquip.setFire(nEquip.newRebirth(getReqLevel(nEquip.getItemId()), scrollId.getItemId(), true));
                        return nEquip;
                    } else {
                        if (!Randomizer.isSuccess(getSuccess(scrollId.getItemId(), chr, nEquip))) {
                            if (Randomizer.isSuccess(getCursed(scrollId.getItemId(), chr))) {
                                if (ItemFlag.PROTECT_SHIELD.check(nEquip.getFlag())) {
                                    chr.dropMessage(5, "주문서의 효과로 아이템이 파괴되지 않았습니다.");
                                } else {
//                                    return null; //펑
                                }
                            }
                            failed = true;
                        } else {
                            for (Entry<String, Integer> stat : stats.entrySet()) {
                                final String key = stat.getKey();

                                if (key.equals("STR")) {
                                    nEquip.setStr((short) (nEquip.getStr() + stat.getValue().intValue()));
                                } else if (key.equals("DEX")) {
                                    nEquip.setDex((short) (nEquip.getDex() + stat.getValue().intValue()));
                                } else if (key.equals("INT")) {
                                    nEquip.setInt((short) (nEquip.getInt() + stat.getValue().intValue()));
                                } else if (key.equals("LUK")) {
                                    nEquip.setLuk((short) (nEquip.getLuk() + stat.getValue().intValue()));
                                } else if (key.equals("PAD")) {
                                    nEquip.setWatk((short) (nEquip.getWatk() + stat.getValue().intValue()));
                                } else if (key.equals("PDD")) {
                                    nEquip.setWdef((short) (nEquip.getWdef() + stat.getValue().intValue()));
                                } else if (key.equals("MAD")) {
                                    nEquip.setMatk((short) (nEquip.getMatk() + stat.getValue().intValue()));
                                } else if (key.equals("MDD")) {
                                    nEquip.setMdef((short) (nEquip.getMdef() + stat.getValue().intValue()));
                                } else if (key.equals("ACC")) {
                                    nEquip.setAcc((short) (nEquip.getAcc() + stat.getValue().intValue()));
                                } else if (key.equals("EVA")) {
                                    nEquip.setAvoid((short) (nEquip.getAvoid() + stat.getValue().intValue()));
                                } else if (key.equals("Speed")) {
                                    nEquip.setSpeed((short) (nEquip.getSpeed() + stat.getValue().intValue()));
                                } else if (key.equals("Jump")) {
                                    nEquip.setJump((short) (nEquip.getJump() + stat.getValue().intValue()));
                                } else if (key.equals("MHP")) {
                                    nEquip.setHp((short) (nEquip.getHp() + stat.getValue().intValue()));
                                } else if (key.equals("MMP")) {
                                    nEquip.setMp((short) (nEquip.getMp() + stat.getValue().intValue()));
//                                } else if (key.equals("MHPr")) {
//                                    nEquip.setHpR((short) (nEquip.getHpR() + stat.getValue().intValue()));
//                                } else if (key.equals("MMPr")) {
//                                    nEquip.setMpR((short) (nEquip.getMpR() + stat.getValue().intValue()));
                                }
                            }
                        }
                        break;
                    }
                }
            }
            if (!GameConstants.isCleanSlate(scrollId.getItemId()) && !GameConstants.isSpecialScroll(scrollId.getItemId()) && !GameConstants.isEquipScroll(scrollId.getItemId()) && !GameConstants.isPotentialScroll(scrollId.getItemId()) && !GameConstants.isRebirthFireScroll(scrollId.getItemId()) && scrollId.getItemId() != 2049360 && scrollId.getItemId() != 2049361) {
                if (ItemFlag.SAFETY_SHIELD.check(nEquip.getFlag()) && failed) {
                    chr.dropMessage(5, "주문서의 효과로 업그레이드 가능 횟수가 차감되지 않았습니다.");
                } else {
                    nEquip.setUpgradeSlots((byte) (nEquip.getUpgradeSlots() - getUpgradeScrollUseSlot(scrollId.getItemId())));
                }
                if (!failed) {
                    nEquip.setLevel((byte) (nEquip.getLevel() + 1));
                }
            }
        }
        return equip;
    }

    private static void startForceScroll(MapleClient c, Equip item) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip item2 = null;
        if (GameConstants.isZeroWeapon(item.getItemId())) {
            item2 = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) (item.getPosition() == -11 ? -10 : -11));
        }

        Equip ordinary = (Equip) item.copy();
        Pair<Integer, Integer> per = starForcePercent(item);
        int destroy = 0;
        StarForceStats stats;

        int result;
        if (true) { //항상성공하는 스타포스 +1 강화서
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
        }
        if (item.getEnchantBuff() >= 0x88 && item.getPosition() < 0) {
            MapleInventoryManipulator.unequip(c, item.getPosition(), c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), GameConstants.getInventoryType(item.getItemId()));
            if (item2 != null) { // 제로 무기 파괴시
                MapleInventoryManipulator.unequip(c, item2.getPosition(), c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot(), GameConstants.getInventoryType(item2.getItemId()));

                Item zw = MapleInventoryManipulator.addId_Item(c, 1572000, (short) 1, "", null, -1, "", false);
                if (zw != null) {
                    MapleInventoryManipulator.equip(c, zw.getPosition(), (short) -11, GameConstants.getInventoryType(zw.getItemId()));
                }

                Item zw2 = MapleInventoryManipulator.addId_Item(c, 1562000, (short) 1, "", null, -1, "", false);
                if (zw2 != null) {
                    MapleInventoryManipulator.equip(c, zw2.getPosition(), (short) -10, GameConstants.getInventoryType(zw2.getItemId()));
                }
            }
        }

        c.getPlayer().shield = false;

        c.getSession().writeAndFlush(CWvsContext.equipmentEnchantResult(101, ordinary, item, null, null, result));
        c.getSession().writeAndFlush(CWvsContext.InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item));
        if (item2 != null) {
            c.getSession().writeAndFlush(CWvsContext.InventoryPacket.updateInventoryItem(false, MapleInventoryType.EQUIP, item2));
        }
    }

    private static int getEquipLevel(int level) {
        int stat = 0;
        if (level >= 0 && level <= 50) {
            stat = 1;
        } else if (level >= 51 && level <= 100) {
            stat = 2;
        } else {
            stat = 3;
        }
        return stat;
    }

    public final int getSuccess(final int itemId, final MapleCharacter player, Item equip) {
        if (player.getGMLevel() > 0) {
            return 100;
        }
        if (equip == null) {
            System.err.println("[오류] 주문서의 성공확률을 구하던 중, 장비 아이템 값에 널 값이 입력되었습니다." + itemId);
            player.dropMessage(5, "[오류] 현재 주문서의 성공확률을 구하는데 실패하였습니다.");
            player.gainItem(itemId, (short) 1, false, -1, "주문서 성공확률 얻기 실패로 얻은 주문서");
            player.getClient().getSession().writeAndFlush(CWvsContext.enableActions(player));
            return 0;
        }

        Equip t = (Equip) equip.copy();
        if (itemId / 100 == 20493) {
            int success = 0;
            Equip lev = (Equip) equip.copy();
            byte leve = lev.getEnhance();
            switch (itemId) {
                case 2049300:
                case 2049303:
                case 2049306:
                case 2049323: {
                    if (leve == 0) {
                        success = 100;
                    } else if (leve == 1) {
                        success = 90;
                    } else if (leve == 2) {
                        success = 80;
                    } else if (leve == 3) {
                        success = 70;
                    } else if (leve == 4) {
                        success = 60;
                    } else if (leve == 5) {
                        success = 50;
                    } else if (leve == 6) {
                        success = 40;
                    } else if (leve == 7) {
                        success = 30;
                    } else if (leve == 8) {
                        success = 20;
                    } else if (leve == 9) {
                        success = 10;
                    } else if (leve >= 10) {
                        success = 5;
                    }
                    return success;
                }
                case 2049301:
                case 2049307: {
                    if (leve == 0) {
                        success = 80;
                    } else if (leve == 1) {
                        success = 70;
                    } else if (leve == 2) {
                        success = 60;
                    } else if (leve == 3) {
                        success = 50;
                    } else if (leve == 4) {
                        success = 40;
                    } else if (leve == 5) {
                        success = 30;
                    } else if (leve == 6) {
                        success = 20;
                    } else if (leve == 7) {
                        success = 10;
                    } else if (leve >= 8) {
                        success = 5;
                    }
                    return success;
                }
            }
        }
        switch (itemId) {
            case 2046841:
            case 2046842:
            case 2046967:
            case 2046971:
            case 2047803:
            case 2047917: {
                return 20;
            }
        }
        if (successCache.containsKey(itemId)) {
            return successCache.get(itemId);
        }

        final MapleData item = getItemData(itemId);
        if (item == null) {
            System.err.println("[오류] 주문서의 성공확률을 구하던 중, 주문서 데이터 값에 널 값이 입력되었습니다." + itemId);
            player.dropMessage(5, "[오류] 현재 주문서의 성공확률을 구하는데 실패하였습니다.");
            player.gainItem(itemId, (short) 1, false, -1, "주문서 성공확률 얻기 실패로 얻은 주문서");
            player.getClient().getSession().writeAndFlush(CWvsContext.enableActions(player));
            return 0;
        }
        int success = 0;
        if (item.getChildByPath("info/successRates") != null) {
            success = MapleDataTool.getIntConvert(t.getLevel() + "", item.getChildByPath("info/successRates"), 20);
        } else {
            success = MapleDataTool.getIntConvert("info/success", item, 100);
        }
        if (!GameConstants.isPotentialScroll(itemId) && !GameConstants.isEquipScroll(itemId)) {
            if (ItemFlag.LUCKY_PROTECT_SHIELD.check(t.getFlag())) {
                success += 10;
            }
        }
        successCache.put(itemId, success);
        return success;
    }

    public final Item getEquipById(final int equipId) {
        return getEquipById(equipId, -1, true);
    }

    public final Item getEquipById(final int equipId, boolean rebirth) {
        return getEquipById(equipId, -1, rebirth);
    }

    public final Item getEquipById(final int equipId, int ringId) {
        return getEquipById(equipId, ringId, true);
    }

    public final Item getEquipById(final int equipId, final int ringId, boolean rebirth) {
        final ItemInformation i = getItemInformation(equipId);
        if (i == null) {
            return new Equip(equipId, (short) 0, ringId, (byte) 0);
        }
        final Item eq = i.eq.copy();
        eq.setUniqueId(ringId);
        Equip eqz = (Equip) eq;
        if (!isCash(equipId) && rebirth) {
            eqz.setFire(eqz.newRebirth(getReqLevel(equipId), 0, true));
            if (ItemFlag.UNTRADEABLE.check(eqz.getFlag()) && eqz.getKarmaCount() < 0 && (isKarmaEnabled(equipId) || isPKarmaEnabled(equipId))) {
                eqz.setKarmaCount((byte) 10);
            }
        }
        return eq;
    }

    public final int getTotalStat(final Equip equip) { //i get COOL when my defense is higher on gms...
        return equip.getStr() + equip.getDex() + equip.getInt() + equip.getLuk() + equip.getMatk() + equip.getWatk() + equip.getAcc() + equip.getAvoid() + equip.getJump()
                + equip.getHands() + equip.getSpeed() + equip.getHp() + equip.getMp() + equip.getWdef() + equip.getMdef();
    }

    public final MapleStatEffect getItemEffect(final int itemId) {
        MapleStatEffect ret = itemEffects.get(Integer.valueOf(itemId));
        if (ret == null) {
            final MapleData item = getItemData(itemId);
            if (item == null || item.getChildByPath("spec") == null) {
                return null;
            }
            ret = MapleStatEffect.loadItemEffectFromData(item.getChildByPath("spec"), itemId);
            itemEffects.put(Integer.valueOf(itemId), ret);
        }
        return ret;
    }

    public final MapleStatEffect getItemEffectEX(final int itemId) {
        MapleStatEffect ret = itemEffectsEx.get(Integer.valueOf(itemId));
        if (ret == null) {
            final MapleData item = getItemData(itemId);
            if (item == null || item.getChildByPath("specEx") == null) {
                return null;
            }
            ret = MapleStatEffect.loadItemEffectFromData(item.getChildByPath("specEx"), itemId);
            itemEffectsEx.put(Integer.valueOf(itemId), ret);
        }
        return ret;
    }

    public final int getCreateId(final int id) {
        final ItemInformation i = getItemInformation(id);
        if (i == null) {
            return 0;
        }
        return i.create;
    }

    public final int getCardMobId(final int id) {
        final ItemInformation i = getItemInformation(id);
        if (i == null) {
            return 0;
        }
        return i.monsterBook;
    }

    public final int getBagType(final int id) {
        final ItemInformation i = getItemInformation(id);
        if (i == null) {
            return 0;
        }
        return i.flag & 0xF;
    }

    public final int getWatkForProjectile(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null || i.equipStats == null || i.equipStats.get("incPAD") == null) {
            return 0;
        }
        return i.equipStats.get("incPAD");
    }

    public final boolean canScroll(final int scrollid, final int itemid) {
        return (scrollid / 100) % 100 == (itemid / 10000) % 100;
    }

    public final String getName(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.name;
    }

    public final String getDesc(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.desc;
    }

    public final String getMsg(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.msg;
    }

    public final short getItemMakeLevel(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.itemMakeLevel;
    }

    public final boolean isDropRestricted(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return ((i.flag & 0x200) != 0 || (i.flag & 0x400) != 0 || GameConstants.isDropRestricted(itemId)) && (itemId == 3012000 || itemId == 3012015 || itemId / 10000 != 301) && itemId != 2041200 && itemId != 5640000 && itemId != 4170023 && itemId != 2040124 && itemId != 2040125 && itemId != 2040126 && itemId != 2040211 && itemId != 2040212 && itemId != 2040227 && itemId != 2040228 && itemId != 2040229 && itemId != 2040230 && itemId != 1002926 && itemId != 1002906 && itemId != 1002927;
    }

    public final boolean isPickupRestricted(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return ((i.flag & 0x80) != 0 || GameConstants.isPickupRestricted(itemId)) && itemId != 4001168 && itemId != 4031306 && itemId != 4031307;
    }

    public final boolean isAccountShared(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x100) != 0;
    }

    public final int getStateChangeItem(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.stateChange;
    }

    public final int getMeso(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return 0;
        }
        return i.meso;
    }

    public final boolean isShareTagEnabled(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x800) != 0;
    }

    public final boolean isKarmaEnabled(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return i.karmaEnabled == 1;
    }

    public final boolean isPKarmaEnabled(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return i.karmaEnabled == 2;
    }

    public final boolean isPickupBlocked(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x40) != 0;
    }

    public final boolean isLogoutExpire(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x20) != 0;
    }

    public final boolean cantSell(final int itemId) { //true = cant sell, false = can sell
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x10) != 0;
    }

    public final Pair<Integer, List<StructRewardItem>> getRewardItem(final int itemid) {
        final ItemInformation i = getItemInformation(itemid);
        if (i == null) {
            return null;
        }
        return new Pair<Integer, List<StructRewardItem>>(i.totalprob, i.rewardItems);
    }

    public final boolean isMobHP(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x1000) != 0;
    }

    public final boolean isQuestItem(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return false;
        }
        return (i.flag & 0x200) != 0 && itemId / 10000 != 301;
    }

    public final Pair<Integer, List<Integer>> questItemInfo(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return new Pair<Integer, List<Integer>>(i.questId, i.questItems);
    }

    public final Pair<Integer, String> replaceItemInfo(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return new Pair<Integer, String>(i.replaceItem, i.replaceMsg);
    }

    public final List<Triple<String, Point, Point>> getAfterImage(final String after) {
        return afterImage.get(after);
    }

    public final String getAfterImage(final int itemId) {
        final ItemInformation i = getItemInformation(itemId);
        if (i == null) {
            return null;
        }
        return i.afterImage;
    }

    public final boolean isJokerToSetItem(final int itemId) {
        if (getEquipStats(itemId) == null) {
            return false;
        }
        return getEquipStats(itemId).containsKey("jokerToSetItem");
    }

    public final boolean itemExists(final int itemId) {
        if (GameConstants.getInventoryType(itemId) == MapleInventoryType.UNDEFINED) {
            return false;
        }
        return getItemInformation(itemId) != null;
    }

    public final boolean isCash(final int itemId) {
        if (getEquipStats(itemId) == null) {
            return GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || GameConstants.getInventoryType(itemId) == MapleInventoryType.DECORATION;
        }
        return GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH || getEquipStats(itemId).get("cash") != null || GameConstants.getInventoryType(itemId) == MapleInventoryType.DECORATION;
    }

    public final ItemInformation getItemInformation(final int itemId) {
        if (itemId <= 0) {
            return null;
        }
        return dataCache.get(itemId);
    }
    private ItemInformation tmpInfo = null;

    public void initItemRewardData(ResultSet sqlRewardData) throws SQLException {
        final int itemID = sqlRewardData.getInt("itemid");
        if (tmpInfo == null || tmpInfo.itemId != itemID) {
            if (!dataCache.containsKey(itemID)) {
                System.out.println("[initItemRewardData] Tried to load an item while this is not in the cache: " + itemID);
                return;
            }
            tmpInfo = dataCache.get(itemID);
        }

        if (tmpInfo.rewardItems == null) {
            tmpInfo.rewardItems = new ArrayList<StructRewardItem>();
        }

        StructRewardItem add = new StructRewardItem();
        add.itemid = sqlRewardData.getInt("item");
        add.period = (add.itemid == 1122017 ? Math.max(sqlRewardData.getInt("period"), 7200) : sqlRewardData.getInt("period"));
        add.prob = sqlRewardData.getInt("prob");
        add.quantity = sqlRewardData.getShort("quantity");
        add.worldmsg = sqlRewardData.getString("worldMsg").length() <= 0 ? null : sqlRewardData.getString("worldMsg");
        add.effect = sqlRewardData.getString("effect");

        tmpInfo.rewardItems.add(add);
    }

    public void initItemAddData(ResultSet sqlAddData) throws SQLException {
        final int itemID = sqlAddData.getInt("itemid");
        if (tmpInfo == null || tmpInfo.itemId != itemID) {
            if (!dataCache.containsKey(itemID)) {
                System.out.println("[initItemAddData] Tried to load an item while this is not in the cache: " + itemID);
                return;
            }
            tmpInfo = dataCache.get(itemID);
        }

        if (tmpInfo.equipAdditions == null) {
            tmpInfo.equipAdditions = new LinkedList<>();
        }

        while (sqlAddData.next()) {
            tmpInfo.equipAdditions.add(new Triple<>(sqlAddData.getString("key"), sqlAddData.getString("subKey"), sqlAddData.getString("value")));
        }
    }

    public void initItemEquipData(ResultSet sqlEquipData) throws SQLException {
        final int itemID = sqlEquipData.getInt("itemid");
        if (tmpInfo == null || tmpInfo.itemId != itemID) {
            if (!dataCache.containsKey(itemID)) {
                System.out.println("[initItemEquipData] Tried to load an item while this is not in the cache: " + itemID);
                return;
            }
            tmpInfo = dataCache.get(itemID);
        }

        if (tmpInfo.equipStats == null) {
            tmpInfo.equipStats = new HashMap<String, Integer>();
        }

        final int itemLevel = sqlEquipData.getInt("itemLevel");
        if (itemLevel == -1) {
            tmpInfo.equipStats.put(sqlEquipData.getString("key"), sqlEquipData.getInt("value"));
        } else {
            if (tmpInfo.equipIncs == null) {
                tmpInfo.equipIncs = new HashMap<Integer, Map<String, Integer>>();
            }

            Map<String, Integer> toAdd = tmpInfo.equipIncs.get(itemLevel);
            if (toAdd == null) {
                toAdd = new HashMap<String, Integer>();
                tmpInfo.equipIncs.put(itemLevel, toAdd);
            }
            toAdd.put(sqlEquipData.getString("key"), sqlEquipData.getInt("value"));
        }
    }

    public void finalizeEquipData(ItemInformation item) {
        int itemId = item.itemId;

        // Some equips do not have equip data. So we initialize it anyway if not initialized
        // already
        // Credits: Jay :)
        if (item.equipStats == null) {
            item.equipStats = new HashMap<String, Integer>();
        }

        item.eq = new Equip(itemId, (byte) 0, -1, (byte) 0);
        short stats = GameConstants.getStat(itemId, 0);
        if (stats > 0) {
            item.eq.setStr(stats);
            item.eq.setDex(stats);
            item.eq.setInt(stats);
            item.eq.setLuk(stats);
        }
        stats = GameConstants.getATK(itemId, 0);
        if (stats > 0) {
            item.eq.setWatk(stats);
            item.eq.setMatk(stats);
        }
        stats = GameConstants.getHpMp(itemId, 0);
        if (stats > 0) {
            item.eq.setHp(stats);
            item.eq.setMp(stats);
        }
        stats = GameConstants.getDEF(itemId, 0);
        if (stats > 0) {
            item.eq.setWdef(stats);
            item.eq.setMdef(stats);
        }
        if (item.equipStats.size() > 0) {
            for (Entry<String, Integer> stat : item.equipStats.entrySet()) {
                final String key = stat.getKey();

                if (key.equals("STR")) {
                    item.eq.setStr(GameConstants.getStat(itemId, stat.getValue().intValue()));
                } else if (key.equals("DEX")) {
                    item.eq.setDex(GameConstants.getStat(itemId, stat.getValue().intValue()));
                } else if (key.equals("INT")) {
                    item.eq.setInt(GameConstants.getStat(itemId, stat.getValue().intValue()));
                } else if (key.equals("LUK")) {
                    item.eq.setLuk(GameConstants.getStat(itemId, stat.getValue().intValue()));
                } else if (key.equals("PAD")) {
                    item.eq.setWatk(GameConstants.getATK(itemId, stat.getValue().intValue()));
                } else if (key.equals("PDD")) {
                    item.eq.setWdef(GameConstants.getDEF(itemId, stat.getValue().intValue()));
                } else if (key.equals("MAD")) {
                    item.eq.setMatk(GameConstants.getATK(itemId, stat.getValue().intValue()));
                } else if (key.equals("MDD")) {
                    item.eq.setMdef(GameConstants.getDEF(itemId, stat.getValue().intValue()));
                } else if (key.equals("ACC")) {
                    item.eq.setAcc((short) stat.getValue().intValue());
                } else if (key.equals("EVA")) {
                    item.eq.setAvoid((short) stat.getValue().intValue());
                } else if (key.equals("Speed")) {
                    item.eq.setSpeed((short) stat.getValue().intValue());
                } else if (key.equals("Jump")) {
                    item.eq.setJump((short) stat.getValue().intValue());
                } else if (key.equals("MHP")) {
                    item.eq.setHp(GameConstants.getHpMp(itemId, stat.getValue().intValue()));
                } else if (key.equals("MMP")) {
                    item.eq.setMp(GameConstants.getHpMp(itemId, stat.getValue().intValue()));
                } else if (key.equals("tuc")) {
                    item.eq.setUpgradeSlots(stat.getValue().byteValue());
                } else if (key.equals("Craft")) {
                    item.eq.setHands(stat.getValue().shortValue());
                } else if (key.equals("durability")) {
                    item.eq.setDurability(stat.getValue().intValue());
                } else if (key.equals("charmEXP")) {
                    item.eq.setCharmEXP(stat.getValue().shortValue());
                } else if (key.equals("PVPDamage")) {
                    item.eq.setPVPDamage(stat.getValue().shortValue());
                } else if (key.equals("bdR")) {
                    item.eq.setBossDamage(stat.getValue().shortValue());
                } else if (key.equals("imdR")) {
                    item.eq.setIgnorePDR(stat.getValue().shortValue());
                }
            }
            if (item.equipStats.get("cash") != null && item.eq.getCharmEXP() <= 0) { //set the exp
                short exp = 0;
                int identifier = itemId / 10000;
                if (GameConstants.isWeapon(itemId) || identifier == 106) { //weapon overall
                    exp = 60;
                } else if (identifier == 100) { //hats
                    exp = 50;
                } else if (GameConstants.isAccessory(itemId) || identifier == 102 || identifier == 108 || identifier == 107) { //gloves shoes accessory
                    exp = 40;
                } else if (identifier == 104 || identifier == 105 || identifier == 110) { //top bottom cape
                    exp = 30;
                }
                item.eq.setCharmEXP(exp);
            }
        }
    }

    public void initItemInformation(ResultSet sqlItemData) throws SQLException {
        final ItemInformation ret = new ItemInformation();
        final int itemId = sqlItemData.getInt("itemid");
        ret.itemId = itemId;
        ret.slotMax = GameConstants.getSlotMax(itemId) > 0 ? GameConstants.getSlotMax(itemId) : sqlItemData.getShort("slotMax");
        ret.price = Double.parseDouble(sqlItemData.getString("price"));
        ret.wholePrice = sqlItemData.getInt("wholePrice");
        ret.stateChange = sqlItemData.getInt("stateChange");
        ret.name = sqlItemData.getString("name");
        ret.desc = sqlItemData.getString("desc");
        ret.msg = sqlItemData.getString("msg");

        ret.flag = sqlItemData.getInt("flags");

        ret.karmaEnabled = sqlItemData.getByte("karma");
        ret.meso = sqlItemData.getInt("meso");
        ret.monsterBook = sqlItemData.getInt("monsterBook");
        ret.itemMakeLevel = sqlItemData.getShort("itemMakeLevel");
        ret.questId = sqlItemData.getInt("questId");
        ret.create = sqlItemData.getInt("create");
        ret.replaceItem = sqlItemData.getInt("replaceId");
        ret.replaceMsg = sqlItemData.getString("replaceMsg");
        ret.afterImage = sqlItemData.getString("afterImage");
        ret.chairType = sqlItemData.getString("chairType");
        ret.nickSkill = sqlItemData.getInt("nickSkill");
        ret.cardSet = 0;
        if (ret.monsterBook > 0 && itemId / 10000 == 238) {
            mobIds.put(ret.monsterBook, itemId);
            for (Entry<Integer, Triple<Integer, List<Integer>, List<Integer>>> set : monsterBookSets.entrySet()) {
                if (set.getValue().mid.contains(itemId)) {
                    ret.cardSet = set.getKey();
                    break;
                }
            }
        }

        final String scrollRq = sqlItemData.getString("scrollReqs");
        if (scrollRq.length() > 0) {
            ret.scrollReqs = new ArrayList<Integer>();
            final String[] scroll = scrollRq.split(",");
            for (String s : scroll) {
                if (s.length() > 1) {
                    ret.scrollReqs.add(Integer.parseInt(s));
                }
            }
        }
        final String consumeItem = sqlItemData.getString("consumeItem");
        if (consumeItem.length() > 0) {
            ret.questItems = new ArrayList<Integer>();
            final String[] scroll = scrollRq.split(",");
            for (String s : scroll) {
                if (s.length() > 1) {
                    ret.questItems.add(Integer.parseInt(s));
                }
            }
        }

        ret.totalprob = sqlItemData.getInt("totalprob");
        final String incRq = sqlItemData.getString("incSkill");
        if (incRq.length() > 0) {
            ret.incSkill = new ArrayList<Integer>();
            final String[] scroll = incRq.split(",");
            for (String s : scroll) {
                if (s.length() > 1) {
                    ret.incSkill.add(Integer.parseInt(s));
                }
            }
        }
        dataCache.put(itemId, ret);
    }

    public Pair<String, Boolean> isSuperial(int itemid) {
        if ((itemid >= 1102471 && itemid <= 1102475) || (itemid >= 1072732 && itemid <= 1072736) || (itemid >= 1132164 && itemid <= 1132168)) {
            return new Pair<String, Boolean>("Helisium", true);
        } else if ((itemid >= 1102476 && itemid <= 1102480) || (itemid >= 1072737 && itemid <= 1072741) || (itemid >= 1132169 && itemid <= 1132173)) {
            return new Pair<String, Boolean>("Nova", true);
        } else if ((itemid >= 1102481 && itemid <= 1102485) || (itemid >= 1072743 && itemid <= 1072747) || (itemid >= 1132174 && itemid <= 1132178 || (itemid >= 1082543 && itemid <= 1082547))) {
            return new Pair<String, Boolean>("Tilent", true);
        } else if ((itemid >= 1122241 && itemid <= 1122245)) {
            return new Pair<String, Boolean>("MindPendent", true);
        }
        return new Pair<String, Boolean>(null, false);
    }

    public final void cacheSpecialHairFace() {
        List<Pair<Integer, String>> nameData = getAllItems();
        MapleDataProvider root = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Character.wz/Hair"));

        root.getRoot().getFiles().stream()
                .filter(file -> Integer.parseInt(file.getName().split(".img")[0]) / 10000 == 6)
                .forEach(file -> {
                    int id = Integer.parseInt(file.getName().split(".img")[0]);
                    if (!nameData.stream().anyMatch(p -> p.left == id)) {
                        specialHairFaceInfo.add(id);
                    }
                });

        root = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wz") + "/Character.wz/Face"));

        root.getRoot().getFiles().stream()
                .filter(file -> Integer.parseInt(file.getName().split(".img")[0]) / 10000 == 5)
                .forEach(file -> {
                    int id = Integer.parseInt(file.getName().split(".img")[0]);
                    if (!nameData.stream().anyMatch(p -> p.left == id)) {
                        specialHairFaceInfo.add(id);
                    }
                });
    }

    public final List<Integer> getAllSpecialHairFaces() {
        if (specialHairFaceInfo == null || specialHairFaceInfo.isEmpty()) {
            cacheSpecialHairFace();
        }
        return specialHairFaceInfo;
    }


}
