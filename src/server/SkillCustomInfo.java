package server;

/**
 *
 * @author 윤정환
 */
public class SkillCustomInfo {
    private long value;
    private long endtime = 0;

    public SkillCustomInfo(long value, long time) {
        this.value = value;
        if (time > 0)
            this.endtime = System.currentTimeMillis() + time;
    }

    public boolean canCancel(long now) {
        return (endtime > 0 && now >= endtime);
    }

    public long getValue() {
        return value;
    }

    public long getEndTime() {
        return endtime;
    }
}
