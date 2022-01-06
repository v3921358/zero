package server.field.boss.lotus;

import client.MapleClient;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

import java.util.concurrent.ScheduledFuture;

public class MapleEnergySphere extends MapleMapObject {

    private int x, y, size, retitution, destroyDelay, startDelay, density, friction;
    private boolean ok, noGravity, noDeleteFromOthers, isDelayed;
    private ScheduledFuture<?> schedule;

    public MapleEnergySphere(boolean isDelayed, int size, int y, int density, int friction, int destroyDelay, int startDelay) {
        this.setOk(true);
        this.setDelayed(isDelayed);
        this.setY(y);
        this.setDensity(density);
        this.setFriction(friction);
        this.destroyDelay = destroyDelay;
        this.startDelay = startDelay;
        this.size = size;
    }

    public MapleEnergySphere(int x, int retitution, int destroyDelay, int startDelay, boolean noGravity, boolean noDeleteFromOthers) {
        this.setOk(false);
        this.setX(x); // x좌표
        this.setRetitution(retitution); // 벽면에 튕기는 횟수
        this.setDestroyDelay(destroyDelay); // 삭제까지 걸리는 시간 (지속시간)
        this.setStartDelay(startDelay); // 생성까지 걸리는 시간
        this.setNoGravity(noGravity); // 중력의 영향을 받는가
        this.setNoDeleteFromOthers(noDeleteFromOthers); // 외부(플레이어 등)에서 공격등으로 삭제가 불가능한가
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getRetitution() {
        return retitution;
    }

    public void setRetitution(int retitution) {
        this.retitution = retitution;
    }

    public int getDestroyDelay() {
        return destroyDelay;
    }

    public void setDestroyDelay(int destroyDelay) {
        this.destroyDelay = destroyDelay;
    }

    public int getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
    }

    public boolean isNoGravity() {
        return noGravity;
    }

    public void setNoGravity(boolean noGravity) {
        this.noGravity = noGravity;
    }

    public boolean isNoDeleteFromOthers() {
        return noDeleteFromOthers;
    }

    public void setNoDeleteFromOthers(boolean noDeleteFromOthers) {
        this.noDeleteFromOthers = noDeleteFromOthers;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getDensity() {
        return density;
    }

    public void setDensity(int density) {
        this.density = density;
    }

    public boolean isDelayed() {
        return isDelayed;
    }

    public void setDelayed(boolean isDelayed) {
        this.isDelayed = isDelayed;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getFriction() {
        return friction;
    }

    public void setFriction(int friction) {
        this.friction = friction;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.ENERGY;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        // TODO Auto-generated method stub

    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduledFuture<?> schedule) {
        this.schedule = schedule;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
