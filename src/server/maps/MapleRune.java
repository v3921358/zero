package server.maps;

import client.MapleClient;
import tools.packet.CField;

public class MapleRune extends MapleMapObject {

    private final int type, posX, posY;
    private long createTimeMills;
    private MapleMap map;

    public MapleRune(int type, int posX, int posY, MapleMap map) {
        this.type = type;
        this.posX = posX;
        this.posY = posY;
        this.map = map;
        this.createTimeMills = System.currentTimeMillis();
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.RUNE;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public MapleMap getMap() {
        return map;
    }

    public int getRuneType() {
        return type;
    }

    public int getPositionX() {
        return posX;
    }

    public int getPositionY() {
        return posY;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(CField.spawnRune(this, true));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(CField.removeRune(this, client.getPlayer()));
    }

    public long getCreateTimeMills() {
        return createTimeMills;
    }

    public void setCreateTimeMills(long createTimeMills) {
        this.createTimeMills = createTimeMills;
    }
}
