package server.field.boss.demian;

import client.MapleClient;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.packet.MobPacket;

import java.util.concurrent.ScheduledFuture;

public class MapleIncinerateObject extends MapleMapObject {

    private int x, y;
    private ScheduledFuture<?> schedule;

    public MapleIncinerateObject(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.INCINERATE;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(MobPacket.incinerateObject(this, true));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(MobPacket.incinerateObject(this, false));
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }

}
