package client;

public class MapleMannequin {

    private int value, baseProb, baseColor, addColor;

    public MapleMannequin(int value, int baseProb, int baseColor, int addColor) {
        this.value = value;
        this.baseProb = baseProb;
        this.baseColor = baseColor;
        this.addColor = addColor;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getBaseProb() {
        return baseProb;
    }

    public void setBaseProb(int baseProb) {
        this.baseProb = baseProb;
    }

    public int getBaseColor() {
        return baseColor;
    }

    public void setBaseColor(int baseColor) {
        this.baseColor = baseColor;
    }

    public int getAddColor() {
        return addColor;
    }

    public void setAddColor(int addColor) {
        this.addColor = addColor;
    }

}
