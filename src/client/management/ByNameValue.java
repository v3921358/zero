package client.management;

public class ByNameValue {

    private String name = "";
    private int value = 0;
    private long time = 0;

    public ByNameValue(String name, int value, long time) {
        this.name = name;
        this.value = value;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public long getTime() {
        return time;
    }

}
