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
package server.polofritto;

import client.MapleClient;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

import java.awt.*;

public class MapleRandomPortal extends MapleMapObject {

    public static final int MAP_PORTAL = 2;
    public static final int DOOR_PORTAL = 6;

    private boolean polo;
    private int type, mapId, charId;
    private Point pos;

    public MapleRandomPortal(final int type, final Point pos, final int mapId, final int charId, boolean isPolo) {
        this.type = type;
        this.pos = pos;
        this.mapId = mapId;
        this.charId = charId;
        this.polo = isPolo;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.RANDOM_PORTAL;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }

    @Override
    public void sendDestroyData(MapleClient client) {
    }

    public int getPortalType() {
        return type;
    }

    public void setPortalType(int type) {
        this.type = type;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public boolean ispolo() {
        return polo;
    }

    public void setpolo(boolean polo) {
        this.polo = polo;
    }
}
