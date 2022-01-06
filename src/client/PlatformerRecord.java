package client;

/**
 *
 * @author SLFCG
 */
public class PlatformerRecord {

    private int Stage, ClearTime, Stars;

    public PlatformerRecord(int stage, int cleartime, int stars) {
        Stage = stage;
        ClearTime = cleartime;
        Stars = stars;
    }

    public void setStage(int a) {
        Stage = a;
    }

    public void setClearTime(int a1) {
        ClearTime = a1;
    }

    public void setStars(int a1) {
        Stars = a1;
    }

    public int getStage() {
        return Stage;
    }

    public int getClearTime() {
        return ClearTime;
    }

    public int getStars() {
        return Stars;
    }
}
