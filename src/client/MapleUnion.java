package client;

public class MapleUnion {

    private int charid, level, job, unk1, unk2, position, unk3;
    private String name;

    public MapleUnion(int charid, int level, int job, int unk1, int unk2, int position, int unk3, String name) {
        this.setCharid(charid);
        this.setLevel(level);
        this.setJob(job);
        this.setUnk1(unk1);
        this.setUnk2(unk2);
        this.setPosition(position);
        this.setUnk3(unk3);
        this.setName(name);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getCharid() {
        return charid;
    }

    public void setCharid(int charid) {
        this.charid = charid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getJob() {
        return job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public int getUnk1() {
        return unk1;
    }

    public void setUnk1(int unk1) {
        this.unk1 = unk1;
    }

    public int getUnk2() {
        return unk2;
    }

    public void setUnk2(int unk2) {
        this.unk2 = unk2;
    }

    public int getUnk3() {
        return unk3;
    }

    public void setUnk3(int unk3) {
        this.unk3 = unk3;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
