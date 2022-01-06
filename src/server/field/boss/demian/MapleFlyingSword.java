package server.field.boss.demian;

import client.MapleCharacter;
import client.MapleClient;
import server.Randomizer;
import server.Timer.MapTimer;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import tools.packet.MobPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MapleFlyingSword extends MapleMapObject {

    private boolean stop;
    private int objectType, count = 0;
    private MapleMonster owner;
    private MapleCharacter target;
    private List<FlyingSwordNode> nodes = new ArrayList<>();

    public MapleFlyingSword(int objectType, MapleMonster owner) {
        this.setObjectType(objectType);
        this.setOwner(owner);
        this.setStop(false);

        this.count = 0;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SWORD;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(MobPacket.FlyingSword(this, true));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(MobPacket.FlyingSword(this, false));
    }

    public int getObjectType() {
        return objectType;
    }

    public void setObjectType(int objectType) {
        this.objectType = objectType;
    }

    public MapleMonster getOwner() {
        return owner;
    }

    public void setOwner(MapleMonster owner) {
        this.owner = owner;
    }

    public MapleCharacter getTarget() {
        return target;
    }

    public void setTarget(MapleCharacter target) {
        this.target = target;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public List<FlyingSwordNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<FlyingSwordNode> nodes) {
        this.nodes = nodes;
    }

    public void updateFlyingSwordNode(MapleMap map) {
        this.count++;
        setStop(false);
        List<FlyingSwordNode> nodes = new ArrayList<>();
        int size = Randomizer.rand(10, 16);
        //-20 ~ 5
        for (int i = 0; i < size; ++i) {
            FlyingSwordNode node;
            int x = Randomizer.rand(-900, 1900);
            int y = Randomizer.rand(-650, 100);
            if (i == size) { // 마지막엔 멈춰야지
                node = new FlyingSwordNode(2, 4, i, 35, 0, 11000, 0, true, 0, new Point(0, 0));
            } else {
                node = new FlyingSwordNode(1, 4, i, 35, 0, 0, 0, false, 0, new Point(x, y));
            }
            nodes.add(node);
        }
        setNodes(nodes);
        map.broadcastMessage(MobPacket.FlyingSwordNode(this));
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                tryAttack(map);
            }
        }, 11000);
    }

    public void tryAttack(MapleMap map) {
        setStop(true);
        List<FlyingSwordNode> nodes = new ArrayList<>();

        MapleCharacter target = map.getCharacter(getTarget().getId());

        if (target != null) {
            Point pos = target.getTruePosition();

            nodes.add(new FlyingSwordNode(1, 8, 0, 60, 500, 0, 0, false, 0, pos));
            nodes.add(new FlyingSwordNode(1, 8, 1, 35, 0, 11000, 0, true, 0, pos));
            setNodes(nodes);
            map.broadcastMessage(MobPacket.FlyingSwordNode(this));

            MobSkill skill = MobSkillFactory.getMobSkill(131, 28); // 하드는 다른 값 쓸수도

            MapleMist mist = new MapleMist(new Rectangle(pos.x, -169, 390, 185), owner, skill, (int) skill.getDuration());

            mist.setPosition(new Point((pos.x + (pos.x + 390) / 2), 17));

            if (mist != null) {
                map.spawnMist(mist, false);
            }
        }

        if (this.count >= 5 && this.objectType == 1) {
            map.broadcastMessage(MobPacket.FlyingSword(this, false));
            map.removeMapObject(this);
        }

        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                updateTarget(map);
            }
        }, 1000);

        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                updateFlyingSwordNode(map);
            }
        }, 11000);
    }

    public void updateTarget(MapleMap map) {
        if (map.getCharactersThreadsafe().size() > 0) {
            MapleCharacter target = map.getCharactersThreadsafe().get(Randomizer.nextInt(map.getCharactersThreadsafe().size()));
            if (target != null) {
                setTarget(target);
                map.broadcastMessage(MobPacket.FlyingSwordTarget(this));
            }
        }
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
