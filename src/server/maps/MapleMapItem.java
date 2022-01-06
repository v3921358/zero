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
package server.maps;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import server.MapleStatEffect;
import tools.packet.CField;
import tools.packet.CWvsContext.BuffPacket;

import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapleMapItem extends MapleMapObject {

    protected Item item;
    protected MapleMapObject dropper;
    protected MapleCharacter owner;
    protected int meso = 0, questid = -1;
    private int flyingSpeed = 0, flyingAngle = 0, publicDropId = 0;
    protected byte type;
    protected boolean pickedUp = false, playerDrop, randDrop = false;
    private boolean flyingDrop = false, touchDrop = false;
    protected long nextExpiry = 0, nextFFA = 0;
    protected Equip equip;
    private ReentrantLock lock = new ReentrantLock();

    public MapleMapItem(Item item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.owner = owner;
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public MapleMapItem(Item item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop, Equip equip) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.owner = owner;
        this.type = type;
        this.playerDrop = playerDrop;
        this.equip = equip;
    }

    public MapleMapItem(Item item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop, int questid) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.owner = owner;
        this.type = type;
        this.playerDrop = playerDrop;
        this.questid = questid;
    }

    public MapleMapItem(int meso, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = null;
        this.dropper = dropper;
        this.owner = owner;
        this.meso = meso;
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public MapleMapItem(Point position, Item item) {
        setPosition(position);
        this.item = item;
        this.owner = null;
        this.type = 2;
        this.playerDrop = false;
        this.randDrop = true;
    }

    public final Item getItem() {
        return item;
    }

    public void setItem(Item z) {
        this.item = z;
    }

    public final int getQuest() {
        return questid;
    }

    public final int getItemId() {
        if (getMeso() > 0) {
            return meso;
        }
        return item.getItemId();
    }

    public final MapleMapObject getDropper() {
        return dropper;
    }

    public final int getOwner() {
        return owner.getId();
    }

    public final int getMeso() {
        return meso;
    }

    public final boolean isPlayerDrop() {
        return playerDrop;
    }

    public final boolean isPickedUp() {
        return pickedUp;
    }

    public void setPickedUp(final boolean pickedUp) {
        this.pickedUp = pickedUp;
    }

    public byte getDropType() {
        return type;
    }

    public void setDropType(byte z) {
        this.type = z;
    }

    public final boolean isRandDrop() {
        return randDrop;
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.ITEM;
    }

    @Override
    public void sendSpawnData(final MapleClient client) {
        if (questid <= 0 || client.getPlayer().getQuestStatus(questid) == 1) {
            if (publicDropId <= 0 || (publicDropId > 0 && client.getAccID() == publicDropId)) {
                client.getSession().writeAndFlush(CField.dropItemFromMapObject(this, null, getTruePosition(), (byte) 2, client.getPlayer().getBuffedEffect(MapleBuffStat.PickPocket) != null));
            }
        }
    }

    @Override
    public void sendDestroyData(final MapleClient client) {
        client.getSession().writeAndFlush(CField.removeItemFromMap(getObjectId(), 1, 0));
    }

    public Lock getLock() {
        return lock;
    }

    public void registerExpire(final long time) {
        nextExpiry = System.currentTimeMillis() + time;
    }

    public void registerFFA(final long time) {
        nextFFA = System.currentTimeMillis() + time;
    }

    public boolean shouldExpire(long now) {
        return !pickedUp && nextExpiry > 0 && nextExpiry < now;
    }

    public boolean shouldFFA(long now) {
        return !pickedUp && type < 2 && nextFFA > 0 && nextFFA < now;
    }

    public boolean hasFFA() {
        return nextFFA > 0;
    }

    public void expire(final MapleMap map) {
        pickedUp = true;
        map.broadcastMessage(CField.removeItemFromMap(getObjectId(), 0, 0));
        map.removeMapObject(this);
        if (randDrop) {
            map.spawnRandDrop();
        }
        if (owner != null) {
            MapleStatEffect pickPocket = owner.getBuffedEffect(MapleBuffStat.PickPocket);
            if (pickPocket != null) {
                owner.setPickPocket(Math.max(0, owner.getPickPocket() - 1));
                owner.getClient().getSession().writeAndFlush(BuffPacket.giveBuff(pickPocket.getStatups(), pickPocket, owner));
            }
        }
    }

    public final Equip getEquip() {
        return equip;
    }

    public boolean isFlyingDrop() {
        return flyingDrop;
    }

    public void setFlyingDrop(boolean flyingDrop) {
        this.flyingDrop = flyingDrop;
    }

    public int getFlyingSpeed() {
        return flyingSpeed;
    }

    public void setFlyingSpeed(int flyingSpeed) {
        this.flyingSpeed = flyingSpeed;
    }

    public int getFlyingAngle() {
        return flyingAngle;
    }

    public void setFlyingAngle(int flyingAngle) {
        this.flyingAngle = flyingAngle;
    }

    public boolean isTouchDrop() {
        return touchDrop;
    }

    public void setTouchDrop(boolean touchDrop) {
        this.touchDrop = touchDrop;
    }

    public int getPublicDropId() {
        return publicDropId;
    }

    public void setPublicDropId(int publicDropId) {
        this.publicDropId = publicDropId;
    }
}
