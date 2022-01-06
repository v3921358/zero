package server;

public class DailyGiftItemInfo {

    private int id, itemId, quantity, SN;

    public DailyGiftItemInfo(int id, int itemId, int quantity, int SN) {
        this.id = id;
        this.itemId = itemId;
        this.quantity = quantity;
        this.SN = SN;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getSN() {
        return SN;
    }

    public void setSN(int sN) {
        SN = sN;
    }

}
