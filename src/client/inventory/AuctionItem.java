package client.inventory;

public class AuctionItem {

    private int auctionId, auctionType, accountId, characterId, state, worldId, bidUserId = 0, nexonOid = -1, deposit = 0, sStype = 0, bidWorld = 0;
    private long price, secondPrice = 0, directPrice, endDate, registerDate;
    private String name, bidUserName = "";
    private Item item;
    private AuctionHistory history;

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionType() {
        return auctionType;
    }

    public void setAuctionType(int auctionType) {
        this.auctionType = auctionType;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    /*
     * 0 : 팔리는 중, 기본값
     * 1 : 안보임
     * 2 : 낙찰, 처리 실패
     * 3 : 판매 완료, 대금 수령
     * 4 : 미판매, 처리 실패
     * 5 : 차액 발생, 입찰금 반환
     * 6 : 상회 입찰, 수령 완료
     * 7 : 낙찰, 수령 완료
     * 8 : 판매 완료, 수령 완료
     * 9 : 미판매, 수령 완료
     */
    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getWorldId() {
        return worldId;
    }

    public void setWorldId(int worldId) {
        this.worldId = worldId;
    }

    public int getBidUserId() {
        return bidUserId;
    }

    public void setBidUserId(int bidUserId) {
        this.bidUserId = bidUserId;
    }

    public int getNexonOid() {
        return nexonOid;
    }

    public void setNexonOid(int nexonOid) {
        this.nexonOid = nexonOid;
    }

    public int getDeposit() {
        return deposit;
    }

    public void setDeposit(int deposit) {
        this.deposit = deposit;
    }

    public int getsStype() {
        return sStype;
    }

    public void setsStype(int sStype) {
        this.sStype = sStype;
    }

    public int getBidWorld() {
        return bidWorld;
    }

    public void setBidWorld(int bidWorld) {
        this.bidWorld = bidWorld;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getSecondPrice() {
        return secondPrice;
    }

    public void setSecondPrice(long secondPrice) {
        this.secondPrice = secondPrice;
    }

    public long getDirectPrice() {
        return directPrice;
    }

    public void setDirectPrice(long directPrice) {
        this.directPrice = directPrice;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public long getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(long registerDate) {
        this.registerDate = registerDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBidUserName() {
        return bidUserName;
    }

    public void setBidUserName(String bidUserName) {
        this.bidUserName = bidUserName;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public AuctionHistory getHistory() {
        return history;
    }

    public void setHistory(AuctionHistory history) {
        this.history = history;
    }
}
