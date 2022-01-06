/**
 * @package : server.maps
 * @author : Yein
 * @fileName : MapleSpecialChair.java
 * @date : 2020. 2. 9.
 */
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import tools.packet.CField;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MapleSpecialChair extends MapleMapObject {

    private int itemId;
    private Rectangle rect;
    private Point point;
    private MapleCharacter owner;
    private List<MapleSpecialChairPlayer> players = new ArrayList<>();

    public MapleSpecialChair(int itemId, Rectangle rect, Point point, MapleCharacter owner, List<MapleSpecialChairPlayer> players) {
        this.itemId = itemId;
        this.rect = rect;
        this.point = point;
        this.owner = owner;
        this.players = players;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SPECIAL_CHAIR;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(CField.specialChair(owner, true, false, true, this));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(CField.specialChair(owner, false, false, false, this));
    }

    public void addPlayer(MapleCharacter chr, int emotion) {
        players.add(new MapleSpecialChairPlayer(chr, emotion));
    }

    public void updatePlayer(MapleCharacter chr, int emotion) {
        for (int i = 0; i < players.size(); ++i) {
            if (players.get(i).emotion == -1) {
                players.set(i, new MapleSpecialChairPlayer(chr, emotion));
                break;
            }
        }
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public Rectangle getRect() {
        return rect;
    }

    public void setRect(Rectangle rect) {
        this.rect = rect;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public List<MapleSpecialChairPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<MapleSpecialChairPlayer> players) {
        this.players = players;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public void setOwner(MapleCharacter owner) {
        this.owner = owner;
    }

    public class MapleSpecialChairPlayer {

        private MapleCharacter chr;
        private int emotion;

        public MapleSpecialChairPlayer(MapleCharacter chr, int emotion) {
            this.chr = chr;
            this.emotion = emotion;
        }

        public MapleCharacter getPlayer() {
            return chr;
        }

        public void setPlayer(MapleCharacter chr) {
            this.chr = chr;
        }

        public int getEmotion() {
            return emotion;
        }

        public void setEmotion(int emotion) {
            this.emotion = emotion;
        }
    }

}
