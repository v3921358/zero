/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import handling.world.CharacterTransfer;
import handling.world.CheaterData;
import handling.world.World;
import server.Timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

public class PlayerStorage {

    private final Map<String, MapleCharacter> nameToChar = new ConcurrentHashMap<String, MapleCharacter>();
    private final Map<Integer, MapleCharacter> idToChar = new ConcurrentHashMap<Integer, MapleCharacter>();
    private final Map<Integer, MapleClient> idToClient = new ConcurrentHashMap<>();
    private final Map<Integer, CharacterTransfer> PendingCharacter = new ConcurrentHashMap<Integer, CharacterTransfer>();
    private final StampedLock readLock = new StampedLock();
//    private int channel;

    public PlayerStorage() {
//        this.channel = channel;
        // Prune once every 15 minutes
        Timer.PingTimer.getInstance().register(new PersistingTask(), 60000);
    }

    public final Map<Integer, MapleCharacter> getAllCharacters() {
        return idToChar;
    }

    public final ArrayList<MapleCharacter> getAllCharacters1() {
        long stamp = readLock.readLock();
        try {
            return new ArrayList<MapleCharacter>(idToChar.values());
        } finally {
            readLock.unlockRead(stamp);
        }
    }

    public final void registerPlayer(final MapleCharacter chr) {
        nameToChar.put(chr.getName().toLowerCase(), chr);
        idToChar.put(chr.getId(), chr);
        idToClient.put(chr.getAccountID(), chr.getClient());
        World.Find.register(chr.getId(), chr.getAccountID(), chr.getName(), chr.getClient().getChannel());
    }

    public final void registerPendingPlayer(final CharacterTransfer chr, final int playerid) {
        PendingCharacter.put(playerid, chr);//new Pair(System.currentTimeMillis(), chr));
    }

    public final void deregisterPlayer(final MapleCharacter chr) {
        nameToChar.remove(chr.getName().toLowerCase());
        idToChar.remove(chr.getId());
        idToClient.remove(chr.getAccountID());
        World.Find.forceDeregister(chr.getId(), chr.getAccountID(), chr.getName());
    }

    public final void deregisterPlayer(final int idz, final int accIdz, final String namez) {
        nameToChar.remove(namez.toLowerCase());
        idToChar.remove(idz);
        idToClient.remove(accIdz);
        World.Find.forceDeregister(idz, accIdz, namez);
    }

    public final int pendingCharacterSize() {
        return PendingCharacter.size();
    }

    public final void deregisterPendingPlayer(final int charid) {
        PendingCharacter.remove(charid);
    }

    public final CharacterTransfer getPendingCharacter(final int charid) {
        return PendingCharacter.remove(charid);
    }

    public final MapleCharacter getCharacterByName(final String name) {
        return nameToChar.get(name.toLowerCase());
    }

    public final MapleCharacter getCharacterById(final int id) {
        return idToChar.get(id);
    }

    public final MapleClient getClientById(final int id) {
        return idToClient.get(id);
    }

    public final int getConnectedClients() {
        return idToChar.size();
    }

    public final List<CheaterData> getCheaters() {
        final List<CheaterData> cheaters = new ArrayList<CheaterData>();
        return cheaters;
    }

    public final List<CheaterData> getReports() {
        final List<CheaterData> cheaters = new ArrayList<CheaterData>();
        return cheaters;
    }

    public final String getOnlinePlayers(final boolean byGM) {
        final StringBuilder sb = new StringBuilder();

        if (byGM) {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            while (itr.hasNext()) {
                sb.append(MapleCharacterUtil.makeMapleReadable(itr.next().getName()));
                sb.append(", ");
            }

            sb.insert(0, "동접 (" + nameToChar.size() + "명) [");
            sb.append("]");
        } else {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (!chr.isGM()) {
                    sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    public final void disconnectAll() {
        disconnectAll(false);
    }

    public final void disconnectAll(final boolean checkGM) {

        final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
        MapleCharacter chr;
        while (itr.hasNext()) {
            chr = itr.next();
            try {
//                if (!chr.isGM() || !checkGM) {
                chr.getClient().disconnect(true, false, true);
                chr.getClient().getSession().close();
                World.Find.forceDeregister(chr.getId(), chr.getAccountID(), chr.getName());
//                    itr.remove();
                System.out.println(chr.getName() + " 캐릭터를 셧다운 했습니다.");
//                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public final void broadcastPacket(final byte[] data) {

        final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
        while (itr.hasNext()) {
            itr.next().getClient().getSession().writeAndFlush(data);
        }
    }

    public final void broadcastSmegaPacket(final byte[] data) {

        final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
        MapleCharacter chr;
        while (itr.hasNext()) {
            chr = itr.next();

            if (chr.getClient().isLoggedIn() && chr.getSmega()) {
                chr.getClient().getSession().writeAndFlush(data);
            }
        }
    }

    public final void broadcastGMPacket(final byte[] data) {

        final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
        MapleCharacter chr;
        while (itr.hasNext()) {
            chr = itr.next();

            if (chr.getClient().isLoggedIn() && chr.isIntern()) {
                chr.getClient().getSession().writeAndFlush(data);
            }
        }
    }

    public class PersistingTask implements Runnable {

        @Override
        public void run() {

            final long currenttime = System.currentTimeMillis();

            Iterator<Entry<Integer, CharacterTransfer>> itr = PendingCharacter.entrySet().iterator();
            List<Entry<Integer, CharacterTransfer>> removes = new ArrayList<>();

            while (itr.hasNext()) {
                Entry<Integer, CharacterTransfer> target = itr.next();
                if (currenttime - target.getValue().TranferTime > 40000) { // 40 sec
                    removes.add(target);
                }
            }

            for (Entry<Integer, CharacterTransfer> remove : removes) {
                PendingCharacter.remove(remove.getKey());
            }
        }
    }
}
