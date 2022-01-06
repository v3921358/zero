package server.life;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.life.MobAttackInfo.MobSkillData;
import tools.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobAttackInfoFactory {

    private static final MobAttackInfoFactory instance = new MobAttackInfoFactory();
    public static Map<Integer, List<MobAttackInfo>> mobAttacks;
    public static List<String> strings;

    public MobAttackInfoFactory() {
        initialize();
    }

    private void initialize() {
        mobAttacks = new HashMap<>();
        strings = new ArrayList<>();
        final String WZpath = System.getProperty("wz");
        final MapleDataProvider stringData = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/String.wz"));
        final MapleData mobStringData = stringData.getData("Mob.img");

        for (MapleData ms : mobStringData) {
            try {
                strings.add(ms.getName());
            } catch (Exception e) {
                continue;
            }
        }

        final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/Mob.wz"));
        for (String string : strings) {
            try {
                if (mobStringData.getChildByPath(string) != null) {
                    List<MobAttackInfo> attacks = new ArrayList<>();
                    MapleData mobData = data.getData(StringUtil.getLeftPaddedStr(string + ".img", '0', 11));
                    if (mobData != null) {
                        MapleData infoData = mobData.getChildByPath("info/link");
                        if (infoData != null) {
                            String linkedmob = MapleDataTool.getString("info/link", mobData);
                            mobData = data.getData(StringUtil.getLeftPaddedStr(linkedmob + ".img", '0', 11));
                        }
                        for (int j = 0; j < 20; ++j) {
                            final MapleData attackData = mobData.getChildByPath("info/attack/" + j);
                            if (attackData != null) {
                                MobAttackInfo ret = new MobAttackInfo(Integer.parseInt(string), j);
                                ret.setDiseaseSkill(MapleDataTool.getInt("disease", attackData, 0));
                                ret.setDiseaseLevel(MapleDataTool.getInt("level", attackData, 0));
                                ret.setMpCon(MapleDataTool.getInt("conMP", attackData, 0));
                                ret.setFixDamR(MapleDataTool.getInt("fixDamR", attackData, 0));
                                final MapleData skillData = attackData.getChildByPath("callSkillWithData");
                                if (skillData != null) {
                                    MobSkillData skill = new MobSkillData(
                                            MapleDataTool.getInt("skill", skillData, 0),
                                            MapleDataTool.getInt("level", skillData, 0),
                                            MapleDataTool.getInt("delay", skillData, 0));
                                    ret.setSkill(skill);
                                }

                                attacks.add(ret);
                            }
                        }
                    }
                    mobAttacks.put(Integer.parseInt(string), attacks);
                }
            } catch (RuntimeException e) { //swallow, don't add if
                continue;
            }
        }
    }

    public static MobAttackInfoFactory getInstance() {
        return instance;
    }

    public static MobAttackInfo getMobAttackInfo(MapleMonster mob, int type) {
        List<MobAttackInfo> attacks = mobAttacks.get(mob.getId());
        for (MobAttackInfo attack : attacks) {
            if (attack.getAttackId() == type) {
                return attack;
            }
        }
        return null;
    }
}
