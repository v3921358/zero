package server.field.boss.will;

import client.MapleClient;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.packet.MobPacket.BossWill;

public class WillPoison extends MapleMapObject {

    private int ownerId;

    public WillPoison(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.POSION;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(BossWill.poison(getObjectId(), ownerId));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(BossWill.removePoison(getObjectId()));
    }
}
