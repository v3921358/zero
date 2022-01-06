package server.maps;

public class BossReward {

    private int objectId, mobId, partyId, price;

    public BossReward(int objectId, int mobId, int partyId, int price) {
        this.setObjectId(objectId);
        this.setMobId(mobId);
        this.setPartyId(partyId);
        this.setPrice(price);
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public int getMobId() {
        return mobId;
    }

    public void setMobId(int mobId) {
        this.mobId = mobId;
    }

    public int getPartyId() {
        return partyId;
    }

    public void setPartyId(int partyId) {
        this.partyId = partyId;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
