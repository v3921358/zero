package client.damage;

import server.Randomizer;

public class CRand32 {

    private long seed1;
    private long seed2;
    private long seed3;
    private long oldSeed1;
    private long oldSeed2;
    private long oldSeed3;

    public CRand32() {//constructor
        int randInt = Randomizer.nextInt();//idk, just make a random number to use as default seeds
        Seed(randInt, randInt, randInt);
    }

    public long Random() {
        long seed1 = this.seed1;
        long seed2 = this.seed2;
        long seed3 = this.seed3;

        this.oldSeed1 = seed1;
        this.oldSeed2 = seed2;
        this.oldSeed3 = seed3;

        long newSeed1 = (seed1 << 12) ^ (seed1 >> 19) ^ ((seed1 >> 6) ^ (seed1 << 12)) & 0x1FFF;
        long newSeed2 = 16 * seed2 ^ (seed2 >> 25) ^ ((16 * seed2) ^ (seed2 >> 23)) & 0x7F;
        long newSeed3 = (seed3 >> 11) ^ (seed3 << 17) ^ ((seed3 >> 8) ^ (seed3 << 17)) & 0x1FFFFF;

        this.seed1 = newSeed1;
        this.seed2 = newSeed2;
        this.seed3 = newSeed3;
        return (newSeed1 ^ newSeed2 ^ newSeed3) & 0xffffffffL;//& 0xffffffffL will help you convert long to unsigned int
    }

    public void Seed(int s1, int s2, int s3) {
        this.seed1 = s1 | 0x100000;
        this.oldSeed1 = s1 | 0x100000;

        this.seed2 = s2 | 0x1000;
        this.oldSeed2 = s2 | 0x1000;

        this.seed3 = s3 | 0x10;
        this.oldSeed3 = s3 | 0x10;
    }
}
