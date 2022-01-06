package handling.channel.handler;

import client.Core;
import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillEntry;
import client.SkillFactory;
import client.VMatrix;
import constants.GameConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleInventoryManipulator;
import server.Randomizer;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

public class MatrixHandler {

    private static List<Pair<Core, List<String>>> passiveCores = new ArrayList<>();
    private static List<Pair<Core, List<String>>> activeCores = new ArrayList<>();
    private static List<Pair<Core, List<String>>> specialCores = new ArrayList<>();

    public static void loadCore() {
        final String WZpath = System.getProperty("wz");
        final MapleDataProvider prov = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/Etc.wz"));
        MapleData nameData = prov.getData("VCore.img");
        try {
            for (MapleData dat : nameData) {
                if (dat.getName().equals("CoreData")) {
                    for (MapleData d : dat) {
                        int coreid = Integer.parseInt(d.getName());
                        int skillid = MapleDataTool.getInt("connectSkill/0", d, 0); // 274기준 1번째만 불러도 됨.
                        int maxlevel = MapleDataTool.getInt("maxLevel", d, 0);
                        List<String> jobs = new ArrayList<>();
                        if (d.getName().equals(d.getName())) {
                            for (MapleData j : d) {
                                if (j.getName().equals("job")) {
                                    for (MapleData jobz : j) {
                                        String job = MapleDataTool.getString(jobz);
                                        jobs.add(job);
                                    }
                                }
                            }
                        }
                        if (!jobs.contains("none")) {
                            switch (coreid / 10000000) {
                                case 1: {
                                    activeCores.add(new Pair<>(new Core(-1, coreid, 0, 1, 0, 1, maxlevel, skillid, 0, 0, -1), jobs));
                                    break;
                                }
                                case 2: {
                                    passiveCores.add(new Pair<>(new Core(-1, coreid, 0, 1, 0, 1, maxlevel, skillid, 0, 0, -1), jobs));
                                    break;
                                }
                                case 3: {
                                    specialCores.add(new Pair<>(new Core(-1, coreid, 0, 1, 0, 1, maxlevel, skillid, 0, 0, -1), jobs));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean CheckUseableJobs(List<String> jobz, List<String> list) {
        for (String job : jobz) {
            for (String jobs : list) {
                if (jobs.equals("none") || job.equals("none")) { // 직업이 없을 때
                    return true;
                }
                if (jobs.equals("all") || job.equals("all")) {
                    return true;
                }
                if (jobs.equals("warrior") && GameConstants.isWarrior(Short.valueOf(job))) {
                    return true;
                }
                if (jobs.equals("magician") && GameConstants.isMagician(Short.valueOf(job))) {
                    return true;
                }
                if (jobs.equals("archer") && GameConstants.isArcher(Short.valueOf(job))) {
                    return true;
                }
                if (jobs.equals("rogue") && GameConstants.isThief(Short.valueOf(job))) {
                    return true;
                }
                if (jobs.equals("pirate") && GameConstants.isPirate(Short.valueOf(job))) {
                    return true;
                }
                if (job.equals("warrior") && GameConstants.isWarrior(Short.valueOf(jobs))) {
                    return true;
                }
                if (job.equals("magician") && GameConstants.isMagician(Short.valueOf(jobs))) {
                    return true;
                }
                if (job.equals("archer") && GameConstants.isArcher(Short.valueOf(jobs))) {
                    return true;
                }
                if (job.equals("rogue") && GameConstants.isThief(Short.valueOf(jobs))) {
                    return true;
                }
                if (job.equals("pirate") && GameConstants.isPirate(Short.valueOf(jobs))) {
                    return true;
                }
                if (GameConstants.JobCodeCheck(Short.valueOf(job), Short.valueOf(jobs))) {
                    return true;
                }
                if (GameConstants.JobCodeCheck(Short.valueOf(jobs), Short.valueOf(job))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean checkOwnUseableJobs(Pair<Core, List<String>> data, MapleClient c) {
        int jobcode = c.getPlayer().getJob();
        List<String> list = data.getRight();
        if (data.getLeft().getCoreId() == 10000024 || data.getLeft().getCoreId() == 10000031) { // 스파이더 인 미러는 얻지못하게
            return false;
        }
        for (String jobs : list) {
            if (isNumeric(jobs)) {
                if (GameConstants.JobCodeCheck(Short.valueOf(jobs), jobcode)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkUseableJobs(Pair<Core, List<String>> data, MapleClient c) {
        int jobcode = c.getPlayer().getJob();
        List<String> list = data.getRight();
        if (data.getLeft().getCoreId() == 10000024 || data.getLeft().getCoreId() == 10000031) { // 스파이더 인 미러는 얻지못하게
            return false;
        }
        for (String jobs : list) {
            if (jobs.equals("none")) { // 직업이 없을 때
                return true;
            }
            if (jobs.equals("all")) {
                return true;
            }
            if (jobs.equals("warrior") && GameConstants.isWarrior(jobcode)) {
                return true;
            }
            if (jobs.equals("magician") && GameConstants.isMagician(jobcode)) {
                return true;
            }
            if (jobs.equals("archer") && GameConstants.isArcher(jobcode)) {
                return true;
            }
            if (jobs.equals("rogue") && GameConstants.isThief(jobcode)) {
                return true;
            }
            if (jobs.equals("pirate") && GameConstants.isPirate(jobcode)) {
                return true;
            }
            if (isNumeric(jobs)) {
                if (GameConstants.JobCodeCheck(Short.valueOf(jobs), jobcode)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean ResetCore(MapleClient c, Pair<Core, List<String>> origin, Pair<Core, List<String>> fresh, boolean checkjob) {
        if (origin.getLeft().getCoreId() == fresh.getLeft().getCoreId()) { // 두 코어 아이디가 같으면.
            return true;
        }
        if ((fresh.getLeft().getCoreId() / 10000000) != 2) { // 강화 코어가 아니면
            return true;
        }

        if (checkjob) {
            if (!CheckUseableJobs(origin.getRight(), fresh.getRight())) { // 두 코어의 직업이 서로 다를 때
                return true;
            }
        }
        if (!checkUseableJobs(fresh, c)) { // 자기 직업아니면
            return true;
        }
        return false;
    }

    public static void UseMirrorCoreJamStone(MapleClient c, int itemid, long crcid) {
        crcid = Randomizer.nextLong();

        try {
            if (c.getPlayer().getCore().size() >= 300) {
                c.getPlayer().dropMessage(1, "코어는 최대 300개까지 보유하실 수 있습니다.");
                return;
            }
            if (c.getPlayer().haveItem(itemid)) {
                MapleInventoryManipulator.removeById_Lock(c, GameConstants.getInventoryType(itemid), itemid);
                Core core = new Core(crcid, 10000024, c.getPlayer().getId(), 1, 0, 1, 50, 400001039, 0, 0, -1);
                c.getPlayer().getCore().add(core);
                core.setId(c.getPlayer().getCore().indexOf(core));
                c.getSession().writeAndFlush(CWvsContext.AddCore(core));
            }
            c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer()));
        } finally {

        }
    }

    public static void UseEnforcedCoreJamStone(MapleClient c, int itemid, long crcid) {
        if (c.getPlayer().getCore().size() >= 300) {
            c.getPlayer().dropMessage(1, "코어는 최대 300개까지 보유하실 수 있습니다.");
            return;
        }

        if (c.getPlayer().haveItem(itemid)) {
            if (MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemid), itemid, 1, false, false)) {
                Core core = new Core(crcid, 40000000, c.getPlayer().getId(), 1, 0, 1, 0, 1, 0, 0, -1);
                c.getPlayer().getCore().add(core);
                core.setId(c.getPlayer().getCore().indexOf(core));
                c.getSession().writeAndFlush(CWvsContext.AddCore(core));
            }
        }
        c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer()));
    }

    public static void UseCoreJamStone(MapleClient c, int itemid, long crcid) {
        crcid = Randomizer.nextLong();

        try {
            if (c.getPlayer().getCore().size() >= 300) {
                c.getPlayer().dropMessage(1, "코어는 최대 300개까지 보유하실 수 있습니다.");
                return;
            }
            if (c.getPlayer().haveItem(itemid)) {
                MapleInventoryManipulator.removeById_Lock(c, GameConstants.getInventoryType(itemid), itemid);

                Pair<Core, List<String>> skill1, skill2, skill3;

                int rand = Randomizer.nextInt(100);

                if (rand < 5) {
                    int rand1 = Randomizer.nextInt(specialCores.size());
                    skill1 = specialCores.get(rand1);
                    skill2 = null;
                    skill3 = null;
                    while (!checkUseableJobs(skill1, c)) {
                        skill1 = specialCores.get(Randomizer.nextInt(specialCores.size()));
                    }
                } else if (rand < 70) {
                    int rand1 = Randomizer.nextInt(passiveCores.size()), rand2 = Randomizer.nextInt(passiveCores.size()), rand3 = Randomizer.nextInt(passiveCores.size());
                    skill1 = passiveCores.get(rand1);
                    skill2 = passiveCores.get(rand2);
                    skill3 = passiveCores.get(rand3);
                    while (!checkUseableJobs(skill1, c)) {
                        skill1 = passiveCores.get(Randomizer.nextInt(passiveCores.size()));
                    }
                    while (ResetCore(c, skill1, skill2, true)) {
                        skill2 = passiveCores.get(Randomizer.nextInt(passiveCores.size()));
                    }
                    while (ResetCore(c, skill1, skill3, true)) {
                        skill3 = passiveCores.get(Randomizer.nextInt(passiveCores.size()));
                    }
                    while (ResetCore(c, skill2, skill3, true)) {
                        skill3 = passiveCores.get(Randomizer.nextInt(passiveCores.size()));
                    }
                } else {
                    int rand1 = Randomizer.nextInt(activeCores.size());
                    skill1 = activeCores.get(rand1);
                    skill2 = null;
                    skill3 = null;
                    while (!checkUseableJobs(skill1, c)) {
                        skill1 = activeCores.get(Randomizer.nextInt(activeCores.size()));
                    }
                }
                Core core = new Core(crcid, skill1.getLeft().getCoreId(), c.getPlayer().getId(), 1, 0, 1, skill1.getLeft().getMaxlevel(), skill1.getLeft().getSkill1(), skill2 == null ? 0 : skill2.getLeft().getSkill1(), skill3 == null ? 0 : skill3.getLeft().getSkill1(), -1);
                c.getPlayer().getCore().add(core);
                core.setId(c.getPlayer().getCore().indexOf(core));
                c.getSession().writeAndFlush(CWvsContext.AddCore(core));
            }
            c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer()));
        } finally {

        }
    }

    public static void gainVCoreLevel(MapleCharacter player) {
        for (Pair<Core, List<String>> coreskill : activeCores) {
            if (checkOwnUseableJobs(coreskill, player.getClient())) {
                Core core = new Core(Randomizer.nextLong(), coreskill.getLeft().getCoreId(), player.getId(), 1, 0, 1, coreskill.getLeft().getMaxlevel(), coreskill.getLeft().getSkill1(), 0, 0, -1);
                player.getCore().add(core);
                core.setId(player.getCore().indexOf(core));
            }
        }
        player.getClient().getSession().writeAndFlush(CWvsContext.UpdateCore(player));
        player.getClient().getSession().writeAndFlush(CWvsContext.enableActions(player));
    }

    public static void updateCore(LittleEndianAccessor slea, MapleClient c) { // 7 - 대기코어들, 8 - 장착코어들
        int state = slea.readInt();

        switch (state) {
            case 0: { // 코어 장착
                int coreId = slea.readInt(); // 새로 장착할 코어
                int prevCoreId = slea.readInt(); // 기존에 장착중인 코어
                slea.skip(4); // -1
//    			int posLevel = slea.readInt();
                int position = slea.readInt();

                if (position < 0) { // -1 : 자동으로 가장 가까운 쪽에 등록
                    position = 0;
                    for (VMatrix matrix : c.getPlayer().getMatrixs()) {
                        if (matrix.getId() != -1) {
                            position++;
                        } else {
                            break;
                        }
                    }
                }

                Core core = corefromId(c.getPlayer(), coreId);
                VMatrix matrix = VMatrixFromPos(c.getPlayer(), position);

                if (core == null || matrix == null) {
                    c.getPlayer().dropMessage(6, "매트릭스 장착 오류가 발생했습니다.");
                    return;
                }

                if (!matrix.isUnLock() && (position > ((c.getPlayer().getLevel() / 5) - 36))) {
                    //레벨 범위 제한 코딩
                    c.getPlayer().dropMessage(6, "착용이 불가능합니다.");
                    return;
                }

                if (prevCoreId >= 0) {
                    Core prevCore = c.getPlayer().getCore().get(prevCoreId);

                    if (prevCore.getPosition() == -1 || prevCore.getState() == 1) {
                        c.getPlayer().dropMessage(6, "코어 장착 도중 오류가 발생했습니다.");
                    } else {

                        prevCore.setState(1);
                        prevCore.setPosition(-1);

                        core.setState(2);
                        core.setPosition(position);

                        matrix.setId(coreId);
                    }

                } else if (core.getPosition() >= 0 || core.getState() == 2) {
                    c.getPlayer().dropMessage(6, "이미 착용중인 코어입니다.");
                } else {
                    core.setState(2);
                    core.setPosition(position);

                    matrix.setId(coreId);
                }

                calcSkillLevel(c.getPlayer(), -1);
                break;
            }
            case 1: { // 코어 해제
                int coreId = slea.readInt();
                slea.skip(4); // -1
//    			int posLevel = slea.readInt();

                Core core = corefromId(c.getPlayer(), coreId);
                VMatrix matrix = VMatrixFromPos(c.getPlayer(), core.getPosition());

                if (core == null || matrix == null) {
                    c.getPlayer().dropMessage(6, "매트릭스 해제 오류가 발생했습니다.");
                    return;
                }

                if (core.getPosition() == -1 || core.getState() == 1) {
                    c.getPlayer().dropMessage(6, "미착용중인 코어입니다.");
                } else {
                    if (c.getPlayer().getCooldownLimit(core.getSkill1()) > 0) {
                        c.getPlayer().dropMessage(6, "재사용 대기시간 중인 코어는 해제할 수 없습니다.");
                        return;
                    }
                    core.setState(1);
                    core.setPosition(-1);

                    matrix.setId(-1);
                }

                calcSkillLevel(c.getPlayer(), -1);
                break;
            }
            case 2: { // 코어 교체
                //59 02 02 00 00 00 01 00 00 00 FF FF FF FF 01 00 00 00 00 00 00 00
                int targetId = slea.readInt();
                int sourceId = slea.readInt();

                int targetPosition = slea.readInt();
                int sourcePosition = slea.readInt();

                VMatrix targetMatrix = VMatrixFromPos(c.getPlayer(), targetPosition);
                VMatrix sourceMatrix = VMatrixFromPos(c.getPlayer(), sourcePosition);

                if (targetMatrix == null || sourceMatrix == null) {
                    c.getPlayer().dropMessage(6, "매트릭스 교체 오류가 발생했습니다.");
                    return;
                }

                Core targetCore = c.getPlayer().getCore().get(targetId);

                Core sourceCore = null;
                if (sourceId > -1) {
                    if (c.getPlayer().getCore().get(sourceId) != null) {
                        sourceCore = c.getPlayer().getCore().get(sourceId);
                    }
                }

                if (c.getPlayer().getCooldownLimit(targetCore.getSkill1()) > 0 || (sourceCore != null && c.getPlayer().getCooldownLimit(sourceCore.getSkill1()) > 0)) {
                    c.getPlayer().dropMessage(6, "재사용 대기시간 중인 코어는 해제할 수 없습니다.");
                    return;
                }

                targetCore.setPosition(sourcePosition);
                targetMatrix.setId(sourceId);

                if (sourceCore != null) {
                    sourceCore.setPosition(targetPosition);
                    sourceMatrix.setId(targetId);
                }

                calcSkillLevel(c.getPlayer(), -1);
                break;
            }
            case 3: {
                System.out.println("New state " + state + " detected : " + slea);
                break;
            }
            case 4: { // 코어 강화
                //59 02 04 00 00 00 08 00 00 00 01 00 00 00 07 00 00 00
                int target = slea.readInt(); // 강화 할 대상
                int size = slea.readInt();

                List<Core> removes = new ArrayList<>();

                int exp = 0;

                Core core = corefromId(c.getPlayer(), target);

                if (core == null) {
                    c.getPlayer().dropMessage(6, "매트릭스 강화 오류가 발생했습니다.");
                    return;
                }

                int prevLevel = core.getLevel();

                for (int i = 0; i < size; ++i) {
                    int source = slea.readInt(); // 강화 제물

                    Core src = c.getPlayer().getCore().get(source);

                    int gainExp = expByLevel(src);

                    core.setExp(core.getExp() + gainExp);

                    exp += gainExp;

                    src.setState(0);
                    src.setExp(0);
                    src.setLevel(0);
                    removes.add(src);
                }

                c.getPlayer().getCore().removeAll(removes);

                while (core.getExp() >= neededLevelUpExp(core)) {
                    core.setExp(core.getExp() - neededLevelUpExp(core));
                    core.setLevel(core.getLevel() + 1);
                    if (core.getLevel() >= 25) {
                        core.setLevel(25);
                        core.setExp(0);
                        break;
                    }
                }

                calcSkillLevel(c.getPlayer(), 3);
                c.getSession().writeAndFlush(CWvsContext.OnCoreEnforcementResult(target, exp, prevLevel, core.getLevel()));
                break;
            }
            case 5: { // 코어 분해
                //59 02 05 00 00 00 06 00 00 00 FF FF FF FF
                int target = slea.readInt();
                slea.skip(4); // -1

                Core core = corefromId(c.getPlayer(), target);

                if (core == null) {
                    c.getPlayer().dropMessage(6, "매트릭스 분해 오류가 발생했습니다.");
                    return;
                }

                int type = core.getCoreId() / 10000000;

                int count = 0;

                switch (type) {
                    case 1:
                        count = 2 * core.getLevel() * (core.getLevel() + 19);
                        break;
                    case 2:
                        count = (3 * (core.getLevel() * core.getLevel()) + 13 * core.getLevel() + 4) / 2;
                        break;
                    case 3:
                        count = 50;
                        break;
                }

                c.getPlayer().setKeyValue(1477, "count", String.valueOf(c.getPlayer().getKeyValue(1477, "count") + count)); // 10
                c.getPlayer().getCore().remove(target);

                calcSkillLevel(c.getPlayer(), 5);
//                c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer(), 1, 5));
                c.getSession().writeAndFlush(CWvsContext.DeleteCore(count));
                break;
            }
            case 6: { // 코어 다중 분해
                //59 02 06 00 00 00 02 00 00 00 06 00 00 00 0D 00 00 00
                int size = slea.readInt();
                int count = 0;

                List<Core> removes = new ArrayList<>();

                for (int i = 0; i < size; ++i) {
                    int source = slea.readInt(); // 분해 대상

                    Core core = corefromId(c.getPlayer(), source);

                    if (core == null) {
                        c.getPlayer().dropMessage(6, "매트릭스 다중 분해 오류가 발생했습니다.");
                        continue;
                    }

                    int type = core.getCoreId() / 10000000;

                    switch (type) {
                        case 1:
                            count += 2 * core.getLevel() * (core.getLevel() + 19);
                            break;
                        case 2:
                            count += (3 * (core.getLevel() * core.getLevel()) + 13 * core.getLevel() + 4) / 2;
                            break;
                        case 3:
                            count += 50;
                            break;
                    }
                    removes.add(core);
                }

                c.getPlayer().setKeyValue(1477, "count", String.valueOf(c.getPlayer().getKeyValue(1477, "count") + count)); // 10

                c.getPlayer().getCore().removeAll(removes);

                calcSkillLevel(c.getPlayer(), 5);
//                c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer(), 1, 5));
                c.getSession().writeAndFlush(CWvsContext.DeleteCore(count));
                break;
            }
            case 7: { // 코어 제작
                int coreId = slea.readInt();
                int nCount = slea.readInt(); // 333++

                List<Pair<Core, List<String>>> cores = new ArrayList<>();
                switch (coreId / 10000000) {
                    case 1:
                        cores = activeCores;
                        break;
                    case 2:
                        cores = passiveCores;
                        break;
                    case 3:
                        cores = specialCores;
                        break;
                }

                for (Pair<Core, List<String>> skill1 : cores) {
                    if (skill1.getLeft().getCoreId() == coreId) {
                        if (nCount == 1) {
                            Pair<Core, List<String>> skill2 = cores.get(Randomizer.nextInt(cores.size())), skill3 = cores.get(Randomizer.nextInt(cores.size()));
                            if ((skill1.getLeft().getCoreId() / 10000000) == 2) { // 강화코어 일 때
                                while (ResetCore(c, skill1, skill2, true)) {
                                    skill2 = cores.get(Randomizer.nextInt(cores.size()));
                                }
                                while (ResetCore(c, skill1, skill3, true)) {
                                    skill3 = cores.get(Randomizer.nextInt(cores.size()));
                                }
                                while (ResetCore(c, skill2, skill3, true)) {
                                    skill3 = cores.get(Randomizer.nextInt(cores.size()));
                                }
                            } else {
                                skill2 = null;
                                skill3 = null;
                            }
                            Core core = new Core(Randomizer.nextLong(), skill1.getLeft().getCoreId(), c.getPlayer().getId(), 1, 0, 1, skill1.getLeft().getMaxlevel(), skill1.getLeft().getSkill1(), skill2 == null ? 0 : skill2.getLeft().getSkill1(), skill3 == null ? 0 : skill3.getLeft().getSkill1(), -1);
                            c.getPlayer().getCore().add(core);
                            core.setId(c.getPlayer().getCore().indexOf(core));
                            c.getSession().writeAndFlush(CWvsContext.ViewNewCore(core, nCount));
                            c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer(), core.getId()));
                            c.getPlayer().setKeyValue(1477, "count", String.valueOf(c.getPlayer().getKeyValue(1477, "count") - ((coreId / 10000000 == 1) ? 110 : 70)));
                        } else {
                            for (int i = 0; i < nCount; ++i) {
                                Pair<Core, List<String>> skill2 = cores.get(Randomizer.nextInt(cores.size())), skill3 = cores.get(Randomizer.nextInt(cores.size()));
                                if ((skill1.getLeft().getCoreId() / 10000000) == 2) { // 강화코어 일 때
                                    while (ResetCore(c, skill1, skill2, true)) {
                                        skill2 = cores.get(Randomizer.nextInt(cores.size()));
                                    }
                                    while (ResetCore(c, skill1, skill3, true)) {
                                        skill3 = cores.get(Randomizer.nextInt(cores.size()));
                                    }
                                    while (ResetCore(c, skill2, skill3, true)) {
                                        skill3 = cores.get(Randomizer.nextInt(cores.size()));
                                    }
                                } else {
                                    skill2 = null;
                                    skill3 = null;
                                }
                                Core core = new Core(Randomizer.nextLong(), skill1.getLeft().getCoreId(), c.getPlayer().getId(), 1, 0, 1, skill1.getLeft().getMaxlevel(), skill1.getLeft().getSkill1(), skill2 == null ? 0 : skill2.getLeft().getSkill1(), skill3 == null ? 0 : skill3.getLeft().getSkill1(), -1);
                                c.getPlayer().getCore().add(core);
                                core.setId(c.getPlayer().getCore().indexOf(core));
                                c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer(), core.getId()));
                            }
                            c.getPlayer().setKeyValue(1477, "count", String.valueOf(c.getPlayer().getKeyValue(1477, "count") - (nCount * ((coreId / 10000000 == 1) ? 110 : 70))));
                            c.getSession().writeAndFlush(CWvsContext.ViewNewCore(skill1.getLeft(), nCount));
                        }
                    }
                }
                break;
            }
//            case 8: {
//                System.out.println("New state " + state + " detected : " + slea);
//                break;
//            }
            case 8: { // 코어 젬스톤 제작
                if (c.getPlayer().getKeyValue(1477, "count") < 35) {
                    c.getPlayer().dropMessage(1, "코어 조각이 부족합니다.");
                    break;
                }
                c.getPlayer().setKeyValue(1477, "count", String.valueOf(c.getPlayer().getKeyValue(1477, "count") - 35));
                c.getPlayer().gainItem(2435719, 1);
                c.getPlayer().dropMessage(1, "코어젬스톤을 획득하였습니다.");
                break;
            }
            case 9: { // 코어칸 강화
                int position = slea.readInt();
                slea.skip(4); // -1
//    			int posLevel = slea.readInt();

                VMatrix matrix = VMatrixFromPos(c.getPlayer(), position);

                if (matrix == null) {
                    c.getPlayer().dropMessage(6, "매트릭스 강화 오류가 발생했습니다.");
                    return;
                }
                matrix.setLevel(Math.min(5, c.getPlayer().getMatrixs().get(position).getLevel() + 1));

                calcSkillLevel(c.getPlayer(), -1);
                break;
            }
            case 10: { // 다음 칸 해금
                //59 02 0B 00 00 00 06 00 00 00 FF FF FF FF
                int position = slea.readInt();
                slea.skip(4); // -1

                for (VMatrix matrix : c.getPlayer().getMatrixs()) {
                    if (matrix.getPosition() == position) {
                        matrix.setUnLock(true);
                        break;
                    }
                }

                c.getSession().writeAndFlush(CWvsContext.UpdateCore(c.getPlayer()));
                break;
            }
            case 12: { // 코어칸 강화 초기화
                for (VMatrix matrix : c.getPlayer().getMatrixs()) {
                    matrix.setLevel(0);
                }

                calcSkillLevel(c.getPlayer(), -1);
                break;
            }
            default: {
                System.out.println("New state " + state + " detected : " + slea);
                break;
            }
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
    }

    private static int neededLevelUpExp(Core core) {
        int type = core.getCoreId() / 10000000;

        if (type == 1) { // 스킬 코어
            return 5 * core.getLevel() + 50;
        }
        return 15 * core.getLevel() + 40; // 강화 코어
    }

    private static int expByLevel(Core core) {
        //Etc/Vcore/expEnforce에 저장되어 있긴 한데 걍 공식 짰음 (캐싱 귀찮음)

        if (core.getCoreId() / 10000000 == 4) {
            return 150;
        }

        int a = core.getExp();
        for (int i = 0; i < core.getLevel(); ++i) {
            a += 50 + (i * 5);
        }
        return a;
    }

    public static void gainMatrix(MapleCharacter chr) {
        List<VMatrix> matrixs = chr.getMatrixs();
        while (matrixs.size() < 26) {
            matrixs.add(new VMatrix(-1, matrixs.size(), 0, false));
        }
        chr.setMatrixs(matrixs);
        chr.getClient().getSession().writeAndFlush(CWvsContext.UpdateCore(chr));
    }

    public static VMatrix VMatrixFromPos(MapleCharacter player, int pos) {
        for (VMatrix ma : player.getMatrixs()) {
            if (ma.getPosition() == pos) {
                return ma;
            }
        }

        return null;
    }

    public static Core corefromId(MapleCharacter player, int id) {
        for (Core core : player.getCore()) {
            if (core.getId() == id) {
                return core;
            }
        }
        return null;
    }

    public static void calcSkillLevel(MapleCharacter player, int position) {

        Map<Skill, SkillEntry> updateSkills = new HashMap<>();

        for (Entry<Skill, SkillEntry> skill : player.getSkills().entrySet()) {
            if (skill.getKey().isVMatrix()) {
                updateSkills.put(skill.getKey(), new SkillEntry(0, (byte) 0, -1));
            }
        }

        for (VMatrix matrix : player.getMatrixs()) {
            matrix.setId(-1);
        }

        player.changeSkillsLevel(updateSkills);

        updateSkills.clear();

        Map<Integer, Integer> addSkills = new HashMap<>();

        for (Core core : player.getCore()) {
            core.setId(player.getCore().indexOf(core));
            if (core.getState() == 2 && core.getPosition() >= 0) {
                if (core.getPosition() >= 26) {
                    core.setState(1);
                    core.setPosition(-1);
                    continue;
                }

                VMatrix matrix = VMatrixFromPos(player, core.getPosition());

                if (matrix != null) {
                    if (matrix.getId() != core.getId()) {
                        matrix.setId(core.getId());
                    }

                    if (core.getSkill1() != 0) {
                        if (addSkills.containsKey(core.getSkill1())) {
                            addSkills.put(core.getSkill1(), addSkills.get(core.getSkill1()) + core.getLevel() + Math.max(0, matrix.getLevel()));
                        } else {
                            addSkills.put(core.getSkill1(), core.getLevel() + Math.max(0, matrix.getLevel()));
                        }

                        if (core.getSkill1() == 400051000 && (GameConstants.isStriker(player.getJob()) || GameConstants.isArk(player.getJob()) || GameConstants.isEunWol(player.getJob()) || GameConstants.isAngelicBuster(player.getJob()) || GameConstants.isXenon(player.getJob()))) {
                            addSkills.put(400051001, core.getLevel() + Math.max(0, matrix.getLevel()));
                        }
                    }

                    if (core.getSkill2() != 0) {
                        if (addSkills.containsKey(core.getSkill2())) {
                            addSkills.put(core.getSkill2(), addSkills.get(core.getSkill2()) + core.getLevel() + Math.max(0, matrix.getLevel()));
                        } else {
                            addSkills.put(core.getSkill2(), core.getLevel() + Math.max(0, matrix.getLevel()));
                        }
                    }

                    if (core.getSkill3() != 0) {
                        if (addSkills.containsKey(core.getSkill3())) {
                            addSkills.put(core.getSkill3(), addSkills.get(core.getSkill3()) + core.getLevel() + Math.max(0, matrix.getLevel()));
                        } else {
                            addSkills.put(core.getSkill3(), core.getLevel() + Math.max(0, matrix.getLevel()));
                        }
                    }
                }
            }
        }

        for (Entry<Integer, Integer> addSkill : addSkills.entrySet()) {
            updateSkills.put(SkillFactory.getSkill(addSkill.getKey()), new SkillEntry(addSkill.getValue(), (byte) SkillFactory.getSkill(addSkill.getKey()).getMasterLevel(), -1));
        }

        player.changeSkillsLevel(updateSkills);

        if (position != -1) {
            player.getClient().getSession().writeAndFlush(CWvsContext.UpdateCore(player, 1, position));
        } else {
            player.getClient().getSession().writeAndFlush(CWvsContext.UpdateCore(player));
        }
    }
}
