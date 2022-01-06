package server.control;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.SkillFactory;
import handling.channel.ChannelServer;
import server.MapleStatEffect;
import server.maps.MapleMap;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import tools.CurrentTime;

import java.awt.*;
import java.util.Iterator;

public class MapleSummonControl implements Runnable {

    private int date, dos, numTimes;
    private boolean isfirst;
    private long lastSaveAuctionTime = 0;

    public MapleSummonControl() {
        this.date = CurrentTime.요일();
        this.dos = 0;
        this.numTimes = 0;
        this.isfirst = false;
        this.lastSaveAuctionTime = System.currentTimeMillis();
        System.out.println("[Loading Completed] Start SummonControl");
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        numTimes++;
        Iterator<ChannelServer> channels = ChannelServer.getAllInstances().iterator();
        while (channels.hasNext()) {
            ChannelServer cs = channels.next();
            Iterator<MapleCharacter> chrs = cs.getPlayerStorage().getAllCharacters().values().iterator();
            while (chrs.hasNext()) {
                MapleCharacter chr = chrs.next();
                MapleMap map = chr.getMap();

                Iterator<MapleSummon> summons = map.getAllSummonsThreadsafe().iterator();
                while (summons.hasNext()) {
                    MapleSummon summon = summons.next();
                    if (summon.getSkill() == 400051022 && time - chr.lastSpiritGateTime >= 2000) {
                        chr.lastSpiritGateTime = System.currentTimeMillis();
                        MapleStatEffect bir = SkillFactory.getSkill(400051022).getEffect(summon.getSkillLevel());
                        MapleStatEffect birz = SkillFactory.getSkill(400051023).getEffect(summon.getSkillLevel());

                        for (int i = 0; i < bir.getX(); ++i) {
                            MapleSummon bird = new MapleSummon(chr, birz, summon.getTruePosition(), SummonMovementType.BIRD_FOLLOW2);
                            chr.getMap().spawnSummon(bird, bir.getW() * 1000);
                            chr.addSummon(bird);
                        }
                    } else if (summon.getSkill() == 400041044) {
                        if (summon.getOwner().getParty() != null && chr.getParty() != null && summon.getOwner().getParty().getId() == chr.getParty().getId()) {
                            MapleStatEffect effect = SkillFactory.getSkill(400041047).getEffect(summon.getSkillLevel());
                            Rectangle box = new Rectangle(summon.getTruePosition().x - 320, summon.getTruePosition().y - 490, 640, 530);
                            if (box.contains(chr.getTruePosition()) && chr.getBuffedValue(MapleBuffStat.IndieDamR, 400041047) == null) {
                                effect.applyTo(chr, false);
                            }
                        }
                    }
                }
            }
        }
    }
}
