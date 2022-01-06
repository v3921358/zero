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

import client.MapleCharacter;
import client.Skill;
import client.SkillFactory;
import constants.GameConstants;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import server.MapleStatEffect;
import server.life.MapleMonster;
import tools.AttackPair;

public class AttackInfo {

    public int skill, charge = 0, lastAttackTickCount, usedCount, unk;
    public List<AttackPair> allDamage;
    public Point attPos, attPos2, attPos3;
    public Point position, chain, plusPosition = new Point(), plusPosition2;
    public Rectangle acrossPosition;
    public int display = 0;
    public byte hits, targets, tbyte = 0, speed = 0, animation, plusPos;
    public short AOE, slot, csstar;
    public boolean real = true, across = false;
    public byte attacktype = 0;
    public boolean isLink = false;
    public byte isBuckShot = 0, isShadowPartner = 0;
    public byte nMoveAction = -1, rlType;
    public byte bShowFixedDamage = 0;
    public int item, skilllevel = 0;
    public byte asist;
    public List<MapleMonster> mobs = new ArrayList<>();
    public List<Point> mistPoints = new ArrayList<>();
    public List<Point> mobpos = new ArrayList<>();

    public final MapleStatEffect getAttackEffect(final MapleCharacter chr, int skillLevel, final Skill skill_) {
        if (GameConstants.isLinkedSkill(skill)) {
            final Skill skillLink = SkillFactory.getSkill(GameConstants.getLinkedSkill(skill));
            return skillLink.getEffect(skillLevel);
        }
        return skill_.getEffect(skillLevel);
    }
}
