package client;

public class BuddylistEntry {

    private String name = "", repName = "", group = "", memo = "";
    private int accountId, cid, channel = -1, level, job;
    private boolean visible;

    public BuddylistEntry(String name, String repName, int accountId, int characterId, String group, int channel, boolean visible, int level, int job, String memo) {
        super();
        this.setName(name);
        this.repName = repName;
        this.setAccountId(accountId);
        this.setCharacterId(characterId);
        this.group = group;
        this.channel = channel;
        this.visible = visible;
        this.setLevel(level);
        this.setJob(job);
        this.memo = memo;
    }

    /**
     * @return the channel the character is on. If the character is offline
     * returns -1.
     */
    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public boolean isOnline() {
        return channel >= 0;
    }

    public void setOffline() {
        channel = -1;
    }

    public String getName() {
        return name;
    }

    public int getCharacterId() {
        return cid;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getGroupName() {
        return group;
    }

    public void setGroupName(String groupName) {
        this.group = groupName;
    }

    public String getMemo() {
        if (memo == null) {
            memo = "";
        }
        return memo;
    }

    public void setMemo(String m) {
        this.memo = m;
    }

    public int getLevel() {
        return level;
    }

    public int getJob() {
        return job;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getCharacterId();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BuddylistEntry other = (BuddylistEntry) obj;
        if (getCharacterId() != other.getCharacterId()) {
            return false;
        }
        return true;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getRepName() {
        return repName;
    }

    public void setRepName(String repName) {
        this.repName = repName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setCharacterId(int cid) {
        this.cid = cid;
    }
}
