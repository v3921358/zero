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
import server.life.MapleLifeFactory;
import server.quest.MapleQuest;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapleQuestStatus implements Serializable {

    private static final long serialVersionUID = 91795419934134L;
    private transient MapleQuest quest;
    private byte status;
    private Map<Integer, Integer> killedMobs = null;
    private int npc;
    private long completionTime;
    private int forfeited = 0;
    private String customData;

    /**
     * Creates a new instance of MapleQuestStatus
     */
    public MapleQuestStatus(final MapleQuest quest, final int status) {
        this.quest = quest;
        this.setStatus((byte) status);
        this.completionTime = System.currentTimeMillis();
        if (status == 1) { // Started
            if (!quest.getRelevantMobs().isEmpty()) {
                registerMobs();
            }
        }
    }

    public MapleQuestStatus(final MapleQuest quest, final byte status, final int npc) {
        this.quest = quest;
        this.setStatus(status);
        this.setNpc(npc);
        this.completionTime = System.currentTimeMillis();
        if (status == 1) { // Started
            if (!quest.getRelevantMobs().isEmpty()) {
                registerMobs();
            }
        }
    }

    public final void setQuest(int qid) {
        this.quest = MapleQuest.getInstance(qid);
    }

    public final MapleQuest getQuest() {
        return quest;
    }

    public final byte getStatus() {
        return status;
    }

    public final void setStatus(final byte status) {
        this.status = status;
    }

    public final int getNpc() {
        return npc;
    }

    public final void setNpc(final int npc) {
        this.npc = npc;
    }

    public boolean isCustom() {
        return GameConstants.isCustomQuest(quest.getId());
    }

    private final void registerMobs() {
        killedMobs = new LinkedHashMap<Integer, Integer>();
        for (final int i : quest.getRelevantMobs().keySet()) {
            killedMobs.put(i, 0);
        }
    }

    private final int maxMob(final int mobid) {
        for (final Map.Entry<Integer, Integer> qs : quest.getRelevantMobs().entrySet()) {
            if (qs.getKey() == mobid) {
                return qs.getValue();
            }
        }
        return 0;
    }

    public final boolean mobKilled(final int id, final int skillID, MapleCharacter chr) {
        if (quest != null && quest.getSkillID() > 0) {
            if (quest.getSkillID() != skillID) {
                return false;
            }
        }
        Integer mob = killedMobs.get(id);
        if (mob != null) {
            final int mo = maxMob(id);
            if (mob >= mo) {
                return false; //nothing happened
            }
            killedMobs.put(id, Math.min(mob + 1, mo));
            return true;
            /*       } else {
             mob = killedMobs.get(9101025);
             if (mob != null) {
             final int mo = maxMob(9101025);
             if (mob >= mo) {
             return false; //nothing happened
             }
             int reqLevel = MapleLifeFactory.getMonster(id).getStats().getLevel();
             if (reqLevel >= chr.getLevel() - 20 && reqLevel <= chr.getLevel() + 20) {
             killedMobs.put(9101025, Math.min(mob + 1, mo));
             return true;
             } else {
             return false;
             }
             }*/
        }

        //특수 몬스터 처리 (레범몬 등)
        for (Entry<Integer, Integer> mo : killedMobs.entrySet()) {
            if (questCount(mo.getKey(), id)) {
                final int mobb = maxMob(mo.getKey());
                if (mo.getValue() >= mobb) {
                    return false; //nothing
                }
                if (mo.getKey() == 9101025) { // 레범몬
                    int reqLevel = MapleLifeFactory.getMonster(id).getStats().getLevel();
                    if (reqLevel >= chr.getLevel() - 20 && reqLevel <= chr.getLevel() + 20) {
                        killedMobs.put(mo.getKey(), Math.min(mo.getValue() + 1, mobb));
                    }
                } else if (mo.getKey() == 9101067) { // 엘몹
                    int scale = MapleLifeFactory.getMonster(id).getScale();
                    if (scale > 100) {
                        killedMobs.put(mo.getKey(), Math.min(mo.getValue() + 1, mobb));
                    }
                } else {
                    killedMobs.put(mo.getKey(), Math.min(mo.getValue() + 1, mobb));
                }
                return true;
            }
        } //i doubt this
        return false;
    }

    private final boolean questCount(final int mo, final int id) {
        if (MapleLifeFactory.getQuestCount(mo) != null) {
            for (int i : MapleLifeFactory.getQuestCount(mo)) {
                if (i == id || mo == 9101025) {
                    return true;
                }
            }
        }
        return false;
    }

    public final void setMobKills(final int id, final int count) {
        if (killedMobs == null) {
            registerMobs(); //lol
        }
        killedMobs.put(id, count);
    }

    public final boolean hasMobKills() {
        if (killedMobs == null) {
            return false;
        }
        return killedMobs.size() > 0;
    }

    public final int getMobKills(final int id) {
        final Integer mob = killedMobs.get(id);
        if (mob == null) {
            return 0;
        }
        return mob;
    }

    public final Map<Integer, Integer> getMobKills() {
        return killedMobs;
    }

    public final long getCompletionTime() {
        return completionTime;
    }

    public final void setCompletionTime(final long completionTime) {
        this.completionTime = completionTime;
    }

    public final int getForfeited() {
        return forfeited;
    }

    public final void setForfeited(final int forfeited) {
        if (forfeited >= this.forfeited) {
            this.forfeited = forfeited;
        } else {
            throw new IllegalArgumentException("Can't set forfeits to something lower than before.");
        }
    }

    public final void setCustomData(final String customData) {
        this.customData = customData;
    }

    public final String getCustomData() {
        return customData;
    }
}
