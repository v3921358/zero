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

import client.*;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

import java.util.HashMap;

public class HyperHandler {

    public static int[] table = {0, 1, 2, 4, 8, 10, 15, 20, 25, 30, 35, 50, 65, 80, 95, 110};

    public static void getHyperSkill(LittleEndianAccessor rh, MapleClient c) {
        String value = rh.readMapleAsciiString();
        if (value.equals("honorLeveling")) {
            return;
        }
        int lvl = rh.readInt();
        int sp = rh.readInt();
        c.getSession().writeAndFlush(CWvsContext.updateHyperSp(value, lvl, sp, value.equals("incHyperStat") ? 3 + (lvl - 140) / 10 : value.equals("needHyperStatLv") ? table[lvl] : 1));
    }

    public static void HyperStatHandler(LittleEndianAccessor slea, int skillid, MapleClient c) {
        final Skill skill = SkillFactory.getSkill(skillid);
        final int maxlevel = skill.getMaxLevel();
        final int curLevel = c.getPlayer().getSkillLevel(skill);
        if (curLevel + 1 <= maxlevel) {
            c.getPlayer().changeSingleSkillLevel(skill, (byte) curLevel + 1, (byte) maxlevel);
        } else {
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
        }
    }

    public static void ResetHyperStatHandler(MapleClient c) {
        long price = 10000000;
        MapleCharacter chr = c.getPlayer();
        if (chr.getMeso() < price) {
            chr.dropMessage(1, "메소가 부족합니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : chr.getSkills().keySet()) {
            if (skil.getId() / 100 == 800004) {
                sa.put(skil, new SkillEntry(0, (byte) skil.getMaxLevel(), -1));
            }
        }

        chr.gainMeso(-price, false);
        chr.changeSkillsLevel(sa);
    }

    public static void ResetHyperSkill(MapleClient c) {
        long price = 100000;
        MapleCharacter chr = c.getPlayer();
        if (chr.getMeso() < price) {
            chr.dropMessage(1, "메소가 부족합니다.");
            c.getSession().writeAndFlush(CWvsContext.enableActions(c.getPlayer()));
            return;
        }

        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : chr.getSkills().keySet()) {
            if (skil.isHyper()) {
                sa.put(skil, new SkillEntry(0, (byte) skil.getMaxLevel(), -1));
            }
        }

        chr.gainMeso(-price, false);
        chr.changeSkillsLevel(sa);
    }
}
