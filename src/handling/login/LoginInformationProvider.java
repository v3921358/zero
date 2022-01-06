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
package handling.login;

import constants.GameConstants;
import constants.ServerConstants;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Triple;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginInformationProvider {

    public enum JobType {

        UltimateAdventurer(-1, 0, ServerConstants.StartMap, false, false, true, false),
        Resistance(0, 3000, ServerConstants.StartMap, false, false, false, false),
        Adventurer(1, 0, ServerConstants.StartMap, false, false, false, false),
        Cygnus(2, 1000, ServerConstants.StartMap, false, false, false, true),
        Aran(3, 2000, ServerConstants.StartMap, false, false, true, false),
        Evan(4, 2001, ServerConstants.StartMap, false, false, true, false),//evan starter map - need to test tutorial
        Mercedes(5, 2002, ServerConstants.StartMap, false, false, false, false),//101050000 - 910150000
        Demon(6, 3001, ServerConstants.StartMap, true, false, false, false),
        Phantom(7, 2003, ServerConstants.StartMap, false, false, false, true),
        DualBlade(8, 0, ServerConstants.StartMap, false, false, false, false),
        Mihile(9, 5000, ServerConstants.StartMap, false, false, true, false),
        Luminous(10, 2004, ServerConstants.StartMap, false, false, false, true),//Ellinia atm TODO tutorial
        Kaiser(11, 6000, 0, false, false, false, false),
        AngelicBuster(12, 6001, ServerConstants.StartMap, false, false, false, false),//400000000 - 940011000 - town now TODO tutorial
        Cannoneer(13, 1, ServerConstants.StartMap, false, false, true, false),
        Xenon(14, 3002, ServerConstants.StartMap, true, false, false, false),
        Zero(15, 10100, ServerConstants.StartMap, false, false, false, true),//321000000 = zero starter map
        EunWol(16, 2005, ServerConstants.StartMap, false, false, true, true),//End map for tutorial
        PinkBean(17, 13000, ServerConstants.StartMap, false, false, false, false),
        Kinesis(18, 14000, ServerConstants.StartMap, false, false, false, false),
        Kadena(19, 6002, ServerConstants.StartMap, false, false, false, false),
        Iliume(20, 15000, ServerConstants.StartMap, false, false, false, false),
        ark(21, 15001, ServerConstants.StartMap, true, false, false, false),
        pathFinder(22, 0, ServerConstants.StartMap, false, false, false, false),
        Hoyeong(23, 16000, ServerConstants.StartMap, true, false, false, true),
        Adel(24, 15002, ServerConstants.StartMap, false, false, false, false),
        Kain(25, 6003, ServerConstants.StartMap, false, false, false, false),
        Yeti(26, 0, ServerConstants.StartMap, false, false, false, false),
        Lala(27, 16001, ServerConstants.StartMap, false, false, false, false);

        public int type, id, map;
        public boolean hairColor, skinColor, faceMark, hat, bottom, cape;

        private JobType(int type, int id, int map, boolean faceMark, boolean hat, boolean bottom, boolean cape) {
            this.type = type;
            this.id = id;
            this.map = ServerConstants.StartMap;
            this.faceMark = faceMark;
            this.hat = hat;
            this.bottom = bottom;
            this.cape = cape;
        }

        public static JobType getByType(int g) {
            if (g == JobType.Cannoneer.type) {
                return JobType.Adventurer;
            }
            for (JobType e : JobType.values()) {
                if (e.type == g) {
                    return e;
                }
            }
            return null;
        }

        public static JobType getById(int g) {
            if (g == JobType.Adventurer.id) {
                return JobType.Adventurer;
            }
            for (JobType e : JobType.values()) {
                if (e.id == g) {
                    return e;
                }
            }
            return null;
        }
    }
    private final static LoginInformationProvider instance = new LoginInformationProvider();
    protected final List<String> ForbiddenName = new ArrayList<String>();
    //gender, val, job
    protected final Map<Triple<Integer, Integer, Integer>, List<Integer>> makeCharInfo = new HashMap<Triple<Integer, Integer, Integer>, List<Integer>>();
    //0 = eyes 1 = hair 2 = haircolor 3 = skin 4 = top 5 = bottom 6 = shoes 7 = weapon

    public static LoginInformationProvider getInstance() {
        return instance;
    }

    protected LoginInformationProvider() {
        final String WZpath = System.getProperty("wz");
        final MapleDataProvider prov = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/Etc.wz"));
        MapleData nameData = prov.getData("ForbiddenName.img");
        for (final MapleData data : nameData.getChildren()) {
            ForbiddenName.add(MapleDataTool.getString(data));
        }
        nameData = prov.getData("Curse.img");
        for (final MapleData data : nameData.getChildren()) {
            ForbiddenName.add(MapleDataTool.getString(data).split(",")[0]);
        }
        final MapleData infoData = prov.getData("MakeCharInfo.img");
        for (MapleData dat : infoData) {
            try {
                final int type;
                if (dat.getName().equals("000_1")) {
                    type = JobType.DualBlade.type;
                } else {
                    type = JobType.getById(Integer.parseInt(dat.getName())).type;
                }
                for (MapleData d : dat) {
                    int val;
                    if (d.getName().contains("female")) {
                        val = 1;
                    } else if (d.getName().contains("male")) {
                        val = 0;
                    } else {
                        continue;
                    }
                    for (MapleData da : d) {
                        int index;
                        Triple<Integer, Integer, Integer> key;
                        index = Integer.parseInt(da.getName());
                        key = new Triple<>(val, index, type);
                        List<Integer> our = makeCharInfo.get(key);
                        if (our == null) {
                            our = new ArrayList<>();
                            makeCharInfo.put(key, our);
                        }
                        for (MapleData dd : da) {
                            if (dd.getName().equalsIgnoreCase("color")) {
                                for (MapleData dda : dd) {
                                    for (MapleData ddd : dda) {
                                        our.add(MapleDataTool.getInt(ddd, -1));
                                    }
                                }
                            } else {
                                try {
                                    our.add(MapleDataTool.getInt(dd, -1));
                                } catch (Exception ex) { //probably like color
                                    for (MapleData dda : dd) {
                                        for (MapleData ddd : dda) {
                                            our.add(MapleDataTool.getInt(ddd, -1));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (NumberFormatException | NullPointerException e) {
            }
        }
        final MapleData uA = infoData.getChildByPath("UltimateAdventurer");
        for (MapleData dat : uA) {
            final Triple<Integer, Integer, Integer> key = new Triple<Integer, Integer, Integer>(-1, Integer.parseInt(dat.getName()), JobType.UltimateAdventurer.type);
            List<Integer> our = makeCharInfo.get(key);
            if (our == null) {
                our = new ArrayList<Integer>();
                makeCharInfo.put(key, our);
            }
            for (MapleData d : dat) {
                our.add(MapleDataTool.getInt(d, -1));
            }
        }
    }

    public static boolean isExtendedSpJob(int jobId) {
        return GameConstants.isSeparatedSp((short) jobId);
    }

    public final boolean isForbiddenName(final String in) {
        for (final String name : ForbiddenName) {
            if (in.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public final boolean isEligibleItem(final int gender, final int val, final int job, final int item) {
        if (item < 0) {
            return false;
        }
        final Triple<Integer, Integer, Integer> key = new Triple<Integer, Integer, Integer>(gender, val, job);
        final List<Integer> our = makeCharInfo.get(key);
        if (our == null) {
            return false;
        }
        return our.contains(item);
    }
}
