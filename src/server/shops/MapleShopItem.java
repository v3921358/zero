package server.shops;

public class MapleShopItem {

    private short buyable;
    private int itemId;
    private long price;
    private int pricequantity;
    private byte tab;
    private int quantity;
    private int period;
    private int position;
    private int itemRate;

    public MapleShopItem(short buyable, int itemId, long price, int pricequantity, byte tab, int quantity, int period, int position, int itemRate) {
        this.buyable = buyable;
        this.itemId = itemId;
        this.price = price;
        this.pricequantity = pricequantity;
        this.tab = tab;
        this.quantity = quantity;
        this.period = period;
        this.position = position;
        this.itemRate = itemRate;
    }

    public int getPriceQuantity() {
        return pricequantity;
    }

    public byte getTab() {
        return tab;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPeriod() {
        return period;
    }

    public short getBuyable() {
        return buyable;
    }

    public int getItemId() {
        return itemId;
    }

    public long getPrice() {
        return price;
    }

    public int getPosition() {
        return position;
    }

    public int getItemRate() {
        return itemRate;
    }

    public void setItemRate(int itemRate) {
        this.itemRate = itemRate;
    }
}
