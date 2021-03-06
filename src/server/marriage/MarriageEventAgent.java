/*
 * Copyright (C) 2013 Nemesis Maple Story Online Server Program

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.marriage;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.World;
import server.MapleItemInformationProvider;
import server.Timer.EventTimer;
import server.maps.MapleMap;
import tools.packet.CField;
import tools.packet.CWvsContext;

import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author Eternal
 */
public class MarriageEventAgent {

    private boolean canStart = true;
    private boolean canEnter = true;
    private boolean finaleStarted = false;
    private int finaleLevel = 0;
    private MarriageDataEntry runningDataEntry = null;
    private ScheduledFuture<?> endSchedule = null;
    private ScheduledFuture<?> ceremonySchedule = null;
    private long endTime = 0;
    private final ChannelServer cserv;
    private final int channel;
    private final static int[] weddingMaps = {680000200, 680000210, 680000300, 680000400, 680000401};
    private boolean endedCeremony = false;

    public MarriageEventAgent(int channel) {
        this.channel = channel;
        cserv = ChannelServer.getInstance(channel);
    }

    public void setCanStart(boolean bln) {
        canStart = bln;
    }

    public boolean canStart() {
        return canStart;
    }

    public boolean canEnter() {
        return canEnter;
    }

    public final void setCanEnter(boolean bln) {
        canEnter = bln;
    }

    public void setDataEntry(MarriageDataEntry data) {
        runningDataEntry = data;
    }

    public MarriageDataEntry getDataEntry() {
        return runningDataEntry;
    }

    public int getChannel() {
        return channel;
    }

    public void registerEvent(MapleCharacter chr) {
        if (!canStart()) {
            return;
        }
        setCanStart(false);
        setDataEntry(MarriageManager.getInstance().getMarriage(chr.getMarriageId()));
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(5, "", runningDataEntry.getGroomName() + "?????? " + runningDataEntry.getBrideName() + "?????? ????????? " + channel + "?????? ??????????????? ???????????? ?????????."));
        if (this.endSchedule != null) {
            this.endSchedule.cancel(false);
        }
        this.endSchedule = EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                endEvent(false);
            }
        }, 10 * 60 * 1000);

        endTime = System.currentTimeMillis() + 600000L;
    }

    public static boolean isWeddingMap(int map) {
        for (int i : weddingMaps) {
            if (map == i) {
                return true;
            }
        }
        return false;
    }

    public void checkEnterMap(MapleCharacter chr) {
        if (endTime > 0) {
            chr.getClient().getSession().writeAndFlush(CField.getClock((int) (((endTime) - System.currentTimeMillis()) / 1000L)));
        }
    }

    public void checkLeaveMap(MapleCharacter chr, int newMap) {
        if (runningDataEntry != null && (!MarriageEventAgent.isWeddingMap(newMap))) {
            if (chr.getId() == runningDataEntry.getGroomId() || chr.getId() == runningDataEntry.getBrideId()) {
                endEvent(true);
            }
        }
    }

    public void startEvent() {
        if (endSchedule != null) {
            endSchedule.cancel(true);
        }
        endTime = System.currentTimeMillis() + 600000L;
        this.endSchedule = EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                finaleEvent();
            }
        }, 10 * 60 * 1000);
        final MapleMap hall = cserv.getMapFactory().getMap(680000210);
        hall.broadcastMessage(CField.musicChange("BgmGL/cathedral"));
        hall.broadcastMessage(CField.getClock(600));
        setCanEnter(false);

        Runnable run = new Runnable() {
            private long firstTime = System.currentTimeMillis();
            private int count = 0;

            @Override
            public void run() {
                long time = System.currentTimeMillis() - firstTime;

                if (count == 0) {
                    count++;
                    String str = "?????? ????????? ??? ???????????? ???????????? ?????? ???????????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                    for (MapleCharacter chr : hall.getCharacters()) {
                        MapleItemInformationProvider.getInstance().getItemEffect(runningDataEntry.getTicketType().getBuffEffectItemId()).applyTo(chr, true);
                    }
                } else if (count == 1 && time >= 10000) {
                    count++;
                    String str = "?????? ????????? ?????? ????????? ??????????????? ?????? ???????????? ????????? ????????? ?????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 2 && time >= 20000) {
                    count++;
                    String str = "?????? ???????????? ??? ???????????? ??? ????????? ?????? ?????? ????????? ????????? ??? ????????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 3 && time >= 30000) {
                    count++;
                    String str = "??? ?????? ???????????? ????????? ????????? ????????? ??? ?????????????????? ????????? ?????????????????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 4 && time >= 40000) {
                    count++;
                    String str = "???????????? ?????? ??? ??? ????????? ???????????? ??????????????? ?????? ????????? ??? ???????????? ????????? ??????????????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 5 && time >= 50000) {
                    count++;
                    String str = "??????, ????????? ????????? ??????, ??? ???????????? ??????????????? ????????? ????????? ????????? ??????????????? ????????? ?????????????????????????";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 6 && time >= 60000) {
                    count++;
                    String str = "??????, ????????? ?????? ????????? ???????????? ?????? ????????? ?????? ?????? ????????? ????????? ?????????????????????????";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 7 && time >= 70000) {
                    count++;
                    String str = "????????? ?????? ?????? ??? ???????????? ????????? ???????????? ???????????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 8 && time >= 80000) {
                    setEndedCeremony(true);
                    count++;
                    String str = "????????? ???????????? ???????????? ????????????.";
                    hall.broadcastMessage(CField.startMapEffect(str, 5120025, true));
                    hall.broadcastMessage(CWvsContext.yellowChat(str));
                } else if (count == 9 && time >= 100000) {
                    count++;
                    hall.broadcastMessage(CField.musicChange("BgmGL/chapel"));
                    hall.broadcastMessage(CField.startMapEffect(null, 0, false));
                }
            }
        };

        this.ceremonySchedule = EventTimer.getInstance().register(run, 5000L);

    }

    public final void setEndedCeremony(boolean fln) {
        endedCeremony = fln;
    }

    public final boolean isEndedCeremony() {
        return endedCeremony;
    }

    public final void setFinaleStarted(boolean f) {
        finaleStarted = f;
    }

    public final boolean isFinaleStarted() {
        return finaleStarted;
    }

    public final void doNextFinale() {
        if (finaleLevel == 0) {
            if (endSchedule != null) {
                endSchedule.cancel(false);
                endSchedule = null;
            }
            if (ceremonySchedule != null) {
                ceremonySchedule.cancel(false);
                ceremonySchedule = null;
            }
            endTime = System.currentTimeMillis() + 300000L;
            this.endSchedule = EventTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    doNextFinale();
                }
            }, 5 * 60 * 1000);
            final MapleMap target = cserv.getMapFactory().getMap(680000300);
            for (final MapleCharacter chr : cserv.getMapFactory().getMap(680000210).getCharacters()) {
                chr.changeMap(target, target.getPortal(0));
            }
            finaleLevel++;
        } else if (finaleLevel == 1) {
            if (endSchedule != null) {
                endSchedule.cancel(false);
                endSchedule = null;
            }
            endTime = System.currentTimeMillis() + 180000L;
            this.endSchedule = EventTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    doNextFinale();
                }
            }, 3 * 60 * 1000);
            final MapleMap target = cserv.getMapFactory().getMap(680000400);
            target.respawn(true);
            for (final MapleCharacter chr : cserv.getMapFactory().getMap(680000300).getCharacters()) {
                chr.changeMap(target, target.getPortal(0));
            }
            finaleLevel++;
            if (runningDataEntry.getTicketType() == MarriageTicketType.SweetieTicket) {
                finaleLevel++;
            }
        } else if (finaleLevel == 2) {
            if (endSchedule != null) {
                endSchedule.cancel(false);
                endSchedule = null;
            }
            final MapleMap target = cserv.getMapFactory().getMap(680000401);
            final MapleMap target2 = cserv.getMapFactory().getMap(680000500);
            target.resetFully();
            for (final MapleCharacter chr : cserv.getMapFactory().getMap(680000400).getCharacters()) {
                if (chr.getId() != runningDataEntry.getBrideId() && chr.getId() != runningDataEntry.getGroomId()) {
                    chr.changeMap(target2, target2.getPortal(0));
                } else {
                    chr.changeMap(target, target.getPortal(0));
                }
            }
            finaleLevel++;
        } else if (finaleLevel == 3) {
            endEvent(true);
        }
    }

    public final void finaleEvent() {
        if (runningDataEntry != null) {
            switch (runningDataEntry.getTicketType()) {
                case CheapTicket:
                    endEvent(true);
                    break;
                case SweetieTicket:
                case PremiumTicket:
                    setFinaleStarted(true);
                    doNextFinale();
                    break;
            }
        } else {
            endEvent(true);
        }
    }

    public final void endEvent(boolean cancelSchedule) {
        if (cancelSchedule && endSchedule != null) {
            endSchedule.cancel(false);
            endSchedule = null;
        }
        if (ceremonySchedule != null) {
            ceremonySchedule.cancel(false);
            ceremonySchedule = null;
        }
        if (runningDataEntry != null) {
            runningDataEntry.setStatus(2);
            runningDataEntry.setWeddingStatus(8);
            MapleCharacter chr = cserv.getPlayerStorage().getCharacterById(runningDataEntry.getBrideId());
            if (chr != null) {
                chr.dropMessage(1, "????????? ?????????????????????.");
            }
            chr = cserv.getPlayerStorage().getCharacterById(runningDataEntry.getGroomId());
            if (chr != null) {
                chr.dropMessage(1, "????????? ?????????????????????.");
            }
        }

        endTime = 0;
        runningDataEntry = null;

        setCanStart(true);
        setCanEnter(true);
        setFinaleStarted(false);
        finaleLevel = 0;
        MarriageManager.getInstance().saveAll();
        final MapleMap target = cserv.getMapFactory().getMap(680000500);
        for (int mapid : weddingMaps) {
            for (MapleCharacter dchr : cserv.getMapFactory().getMap(mapid).getCharacters()) {
                dchr.changeMap(target, target.getPortal(0));
            }
        }
    }
}
