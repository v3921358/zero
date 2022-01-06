package client;

public class Core implements Comparable<Core> {

    private int coreId, id, charId, level, exp, state, maxlevel, skill1, skill2, skill3, position;
    private long crcId;

    public Core(long crcid, int coreid, int charid, int level, int exp, int state, int maxlevel, int skill1, int skill2, int skill3, int position) {
        this.setCrcId(crcid);
        this.setCoreId(coreid);
        this.setCharId(charid);
        this.setLevel(level);
        this.setExp(exp);
        this.setState(state);
        this.setMaxlevel(maxlevel);
        this.setSkill1(skill1);
        this.setSkill2(skill2);
        this.setSkill3(skill3);
        this.setPosition(position);
    }

    public int getCoreId() {
        return coreId;
    }

    public void setCoreId(int coreid) {
        this.coreId = coreid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charid) {
        this.charId = charid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getSkill1() {
        return skill1;
    }

    public void setSkill1(int skill1) {
        this.skill1 = skill1;
    }

    public int getSkill2() {
        return skill2;
    }

    public void setSkill2(int skill2) {
        this.skill2 = skill2;
    }

    public int getSkill3() {
        return skill3;
    }

    public void setSkill3(int skill3) {
        this.skill3 = skill3;
    }

    public long getCrcId() {
        return crcId;
    }

    public void setCrcId(long crcid) {
        this.crcId = crcid;
    }

    public int getMaxlevel() {
        return maxlevel;
    }

    public void setMaxlevel(int maxlevel) {
        this.maxlevel = maxlevel;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int compareTo(Core o) {
        if (this.id < o.getId()) {
            return -1;
        } else if (this.id > o.getId()) {
            return 1;
        }
        return 0;
    }
}
