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
package server.field.skill;

import client.MapleCharacter;
import client.MapleClient;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.packet.CField.AttackObjPacket;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;

public class MapleFieldAttackObj extends MapleMapObject {

    private boolean facingleft;
    private MapleCharacter chr;
    private int sourceid, duration;
    private ScheduledFuture<?> schedule = null;

    public MapleFieldAttackObj(MapleCharacter chr, int sourceid, boolean facingleft, Point pos, int duration) {
        this.setChr(chr);
        this.setSourceid(sourceid);
        this.setDuration(duration);
        this.setPosition(pos);
        this.setFacingleft(facingleft);
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.FIELD;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getPlayer().getMap().broadcastMessage(AttackObjPacket.ObjCreatePacket(this));
    }

    @Override
    public void sendDestroyData(MapleClient client) {

    }

    public MapleCharacter getChr() {
        return chr;
    }

    public void setChr(MapleCharacter chr) {
        this.chr = chr;
    }

    public int getSourceid() {
        return sourceid;
    }

    public void setSourceid(int sourceid) {
        this.sourceid = sourceid;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void onSetAttack(MapleClient client) {
        client.getSession().writeAndFlush(AttackObjPacket.OnSetAttack(this));
    }

    public boolean isFacingleft() {
        return facingleft;
    }

    public void setFacingleft(boolean facingleft) {
        this.facingleft = facingleft;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }
}
