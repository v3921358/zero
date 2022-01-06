/**
 * @package : client
 * @author : Yein
 * @fileName : VMatrix.java
 * @date : 2019. 11. 21.
 */
package client;

public class VMatrix {

    private boolean isUnLock = false;
    private int id, position, level;

    public VMatrix(int id, int position, int level, boolean isUnLock) {
        this.id = id;
        this.position = position;
        this.level = level;
        this.isUnLock = isUnLock;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMaxLevel() {
        if (id == -1 || id == 0) {
            return 0;
        }
        Skill skill = SkillFactory.getSkill(id);
        return skill == null ? 0 : skill.getMaxLevel();
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isUnLock() {
        return isUnLock;
    }

    public void setUnLock(boolean isUnLock) {
        this.isUnLock = isUnLock;
    }
}
