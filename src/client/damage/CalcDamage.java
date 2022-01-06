package client.damage;

import client.MapleCharacter;
import client.SkillFactory;
import handling.channel.handler.AttackInfo;
import server.MapleStatEffect;
import server.life.MapleMonster;
import tools.AttackPair;
import tools.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CalcDamage {

    CRand32 rndGenForCharacter;
    //CRand32 rndForCheckDamageMiss;//not implement yet
    //CRand32 rndGenForMob;//not implement yet
    int invalidCount;

    public CalcDamage() {
        rndGenForCharacter = new CRand32();
        invalidCount = 0;
    }

    public void SetSeed(int seed1, int seed2, int seed3) {
        rndGenForCharacter.Seed(seed1, seed2, seed3);
        //rndForCheckDamageMiss.Seed(seed1, seed2, seed3);//not implement yet
        //rndGenForMob.Seed(seed1, seed2, seed3);//not implement yet
    }

    private int numRand = 11; //A number of random number for calculate damage (KMST 1029 기준 11번)

    public List<Pair<Long, Boolean>> PDamage(MapleCharacter chr, AttackInfo attack) {
        List<Pair<Long, Boolean>> realDamageList = new ArrayList<>();
        for (AttackPair eachMob : attack.allDamage) { //For each monster
            MapleMonster monster = chr.getMap().getMonsterByOid(eachMob.objectid);

            //we need save it as long type to store unsigned int
            long rand[] = new long[numRand];
            for (int i = 0; i < numRand; i++) {
                rand[i] = rndGenForCharacter.Random();
            }
            byte index = 0;

            for (Pair<Long, Boolean> att : eachMob.attack) { //For each attack
                double realDamage = 0.0;
                boolean critical = false;

                index++;
                long unkRand1 = rand[index++ % numRand];

                //Adjusted Random Damage
                long maxDamage = 38;//0;//(long) chr.getStat().getCurrentMaxBaseDamage();
                long minDamage = 8;//Long.MAX_VALUE;//chr.getStat().getCurrentMinBaseDamage();
                double adjustedRandomDamage = RandomInRange(rand[index++ % numRand], maxDamage, minDamage);
                realDamage += adjustedRandomDamage;

//	            chr.dropMessage(6, "adjustedRandomDamage : " + adjustedRandomDamage);
                //Adjusted Damage By Monster's Physical Defense Rate
                if (monster == null) {
                    chr.dropMessageGM(6, "monster null");
                    continue;
                } else if (monster.getStats() == null) {
                    chr.dropMessageGM(6, "stat null");
                    continue;
                }

                /*	            double monsterPDRate = monster.getStats().getPDRate();
                 double percentDmgAfterPDRate = Math.max(0.0, 100.0 - monsterPDRate);
                 realDamage = percentDmgAfterPDRate * realDamage / 100.0;*/
                //방어 비율 계산 이상함 ㅡ.ㅡ;
                //Adjusted Damage By Skill
                MapleStatEffect skillEffect = null;
                if (attack.skill > 0) {
                    skillEffect = SkillFactory.getSkill(attack.skill).getEffect(chr.getTotalSkillLevel(attack.skill));
                }
                if (skillEffect != null) {
                    chr.dropMessageGM(6, "skillDamage : " + skillEffect.getDamage());
                    realDamage = realDamage * (double) skillEffect.getDamage() / 100.0;
                }

                //Adjusted Critical Damage
                if (RandomInRange(rand[index++ % numRand], 100, 0) < chr.getStat().critical_rate) {
                    critical = true;
                    int maxCritDamage = chr.getStat().critical_damage;
                    int criticalDamageRate = (int) RandomInRange(rand[index++ % numRand], maxCritDamage, maxCritDamage);
                    //nexon convert realDamage to int when multiply with criticalDamageRate
                    realDamage = realDamage + (criticalDamageRate / 100.0 * (int) realDamage);
                }

                realDamageList.add(new Pair<>((long) realDamage, critical));
            }
        }

        return realDamageList;
    }

    public double RandomInRange(long randomNum, long maxDamage, long minDamage) {
        //java not have unsigned long, so i used BigInteger
        BigInteger ECX = new BigInteger("" + randomNum);//random number from Crand32::Random()
        BigInteger EAX = new BigInteger("1801439851");//0x6B5FCA6B; <= this is const
        //ECX * EAX = EDX:EAX (64bit register)
        BigInteger multipled = ECX.multiply(EAX);
        //get EDX from EDX:EAX
        long highBit = multipled.shiftRight(32).longValue();//get 32bit high
        long rightShift = highBit >>> 22;//SHR EDX,16
        double newRandNum = randomNum - (rightShift * 10000000.0);

        double value;
        if (minDamage != maxDamage) {
            if (minDamage > maxDamage) {//swap
                long temp = maxDamage;
                maxDamage = minDamage;
                minDamage = temp;
            }
            value = (maxDamage - minDamage) * newRandNum / 9999999.0 + minDamage;
        } else {
            value = maxDamage;
        }
        return value;
    }
}
