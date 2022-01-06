/**
 * @package : server.field.boss.demian
 * @author : Yein
 * @fileName : MapleDelayedAttack.java
 * @date : 2019. 9. 7.
 */
package server.field.boss.demian;

import client.MapleClient;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

import java.awt.*;

public class MapleDelayedAttack extends MapleMapObject {

    private boolean isfacingLeft;
    private int angle;
    private Point pos;
    private MapleMonster owner;

    public MapleDelayedAttack(MapleMonster owner, Point pos, boolean facingleft) {
        this.owner = owner;
        this.setPos(pos);
        this.isfacingLeft = facingleft;
    }

    public MapleDelayedAttack(MapleMonster owner, Point pos, int angle, boolean facingleft) {
        this.owner = owner;
        this.setPos(pos);
        this.angle = angle;
        this.isfacingLeft = facingleft;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.ATTACK;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }

    @Override
    public void sendDestroyData(MapleClient client) {
    }

    public MapleMonster getOwner() {
        return owner;
    }

    public void setOwner(MapleMonster owner) {
        this.owner = owner;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public boolean isIsfacingLeft() {
        return isfacingLeft;
    }

    public void setIsfacingLeft(boolean isfacingLeft) {
        this.isfacingLeft = isfacingLeft;
    }
}
