package server.field.boss.will;

import client.MapleClient;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.packet.MobPacket.BossWill;

public class SpiderWeb extends MapleMapObject {

    private int pattern, x1, y1;

    public SpiderWeb(int pattern, int x1, int y1) {
        this.pattern = pattern;
        this.x1 = x1;
        this.y1 = y1;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.WEB;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(BossWill.willSpider(3, this));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(BossWill.willSpider(4, this));
    }

}
