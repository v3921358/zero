package client;

public enum MapleStat {

    SKIN(0x1), // byte
    FACE(0x2), // int
    HAIR(0x4), // int
    LEVEL(0x10), // byte
    JOB(0x20), // short
    STR(0x40), // short
    DEX(0x80), // short
    INT(0x100), // short
    LUK(0x200), // short
    HP(0x400), // int
    MAXHP(0x800), // int
    MP(0x1000), // int
    MAXMP(0x2000), // int
    AVAILABLEAP(0x4000), // short
    AVAILABLESP(0x8000), // short (depends)
    EXP(0x10000), // int
    FAME(0x20000), // int
    MESO(0x40000), // int
    FATIGUE(0x80000),
    CHARISMA(0x100000),
    PET(0x180008),
    INSIGHT(0x200000),
    WILL(0x400000),
    CRAFT(0x800000),
    SENSE(0x1000000),
    CHARM(0x2000000),
    TRAIT_LIMIT(0x4000000),
    BATTLE_EXP(0x40000000), // int
    BATTLE_RANK(0x80000000), // byte
    BATTLE_POINTS(0x10000000),
    ICE_GAGE(0x20000000),
    VIRTUE(0x40000000);

    private final int i;

    private MapleStat(int i) {
        this.i = i;
    }

    public int getValue() {
        return i;
    }

    public static final MapleStat getByValue(final int value) {
        for (final MapleStat stat : MapleStat.values()) {
            if (stat.i == value) {
                return stat;
            }
        }
        return null;
    }

    public static enum Temp {

        STR(0x1),
        DEX(0x2),
        INT(0x4),
        LUK(0x8),
        WATK(0x10),
        WDEF(0x20),
        MATK(0x40),
        MDEF(0x80),
        ACC(0x100),
        AVOID(0x200),
        SPEED(0x400), // byte
        JUMP(0x800), // byte
        UNKNOWN(0x1000); // byte

        private final int i;

        private Temp(int i) {
            this.i = i;
        }

        public int getValue() {
            return i;
        }
    }
}
