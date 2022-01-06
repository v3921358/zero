package handling.world;

import client.BuddyList;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildCharacter;
import log.DBLogger;
import log.LogType;
import tools.CollectionUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class World {

    //Touch everything...
    public static void init() {
        World.Find.findChannel(0);
        World.Messenger.getMessenger(0);
        World.Party.getParty(0);
    }

    public static List<MapleCharacter> getAllChars() {
        List<MapleCharacter> temp = new ArrayList<MapleCharacter>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters().values()) {
                if (!temp.contains(chr)) {
                    temp.add(chr);
                }
            }
        }
        return temp;
    }

    public static String getStatus() {
        StringBuilder ret = new StringBuilder();
        int totalUsers = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            ret.append("Channel ");
            ret.append(cs.getChannel());
            ret.append(": ");
            int channelUsers = cs.getConnectedClients();
            totalUsers += channelUsers;
            ret.append(channelUsers);
            ret.append(" users\n");
        }
        ret.append("Total users online: ");
        ret.append(totalUsers);
        ret.append("\n");
        return ret.toString();
    }

    public static Map<Integer, Integer> getConnected() {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        int total = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            int curConnected = cs.getConnectedClients();
            ret.put(cs.getChannel(), curConnected);
            total += curConnected;
        }
        ret.put(0, total);
        return ret;
    }

    public static List<CheaterData> getCheaters() {
        List<CheaterData> allCheaters = new ArrayList<CheaterData>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getCheaters());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static List<CheaterData> getReports() {
        List<CheaterData> allCheaters = new ArrayList<CheaterData>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getReports());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static boolean isConnected(String charName) {
        return Find.findChannel(charName) > 0;
    }

    public static void toggleMegaphoneMuteState() {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.toggleMegaphoneMuteState();
        }
    }

    public static void ChannelChange_Data(CharacterTransfer Data, int characterid, int toChannel) {
        getStorage(toChannel).registerPendingPlayer(Data, characterid);
    }

    public static void isCharacterListConnected(String name, List<String> charName) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (final String c : charName) {
                if (cs.getPlayerStorage().getCharacterByName(c) != null) {
                    cs.getPlayerStorage().deregisterPlayer(cs.getPlayerStorage().getCharacterByName(c));
                }
            }
        }
    }

    public static boolean hasMerchant(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsMerchant(accountID, characterID)) {
                return true;
            }
        }
        return false;
    }

    public static PlayerStorage getStorage(int channel) {
        if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        } else if (channel == -20) {
            return AuctionServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    public static int getPendingCharacterSize() {
        int ret = 0;
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            ret += cserv.getPlayerStorage().pendingCharacterSize();
        }
        return ret;
    }

    public static boolean isChannelAvailable(final int ch) {
        if (ChannelServer.getInstance(ch) == null || ChannelServer.getInstance(ch).getPlayerStorage() == null) {
            return false;
        }
        return ChannelServer.getInstance(ch).getPlayerStorage().getConnectedClients() < (ch == 1 ? 600 : 400);
    }

    public static class Party {

        private static Map<Integer, MapleParty> parties = new HashMap<Integer, MapleParty>();
        private static Map<Integer, MapleExpedition> expeds = new HashMap<Integer, MapleExpedition>();
        private static Map<PartySearchType, List<PartySearch>> searches = new EnumMap<PartySearchType, List<PartySearch>>(PartySearchType.class);
        private static final AtomicInteger runningPartyId = new AtomicInteger(1), runningExpedId = new AtomicInteger(1);

        static {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE characters SET party = -1, fatigue = 0");
                ps.executeUpdate();
                ps.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            for (PartySearchType pst : PartySearchType.values()) {
                searches.put(pst, new ArrayList<PartySearch>()); //according to client, max 10, even though theres page numbers ?!
            }
        }

        public static void partyChat(MapleCharacter chr, String chattext, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
            partyChat(chr, chattext, 1, slea, recv);
        }

        /*        public static void expedChat(MapleCharacter player, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
         MapleExpedition party = getExped(player.getGuild());
         if (party == null) {
         return;
         }
         for (int i : party.getParties()) {
         partyChat(i, chattext, namefrom, 4, slea, recv);
         }
         }

         public static void expedPacket(int expedId, byte[] packet, MaplePartyCharacter exception) {
         MapleExpedition party = getExped(expedId);
         if (party == null) {
         return;
         }
         for (int i : party.getParties()) {
         partyPacket(i, packet, exception);
         }
         }*/
        public static void partyPacket(int partyid, byte[] packet, MaplePartyCharacter exception) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0 && (exception == null || partychar.getId() != exception.getId())) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) { //Extra check just in case
                        chr.getClient().getSession().writeAndFlush(packet);
                    }
                }
            }
        }

        public static void partyChat(MapleCharacter player, String chattext, int mode, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
            MapleParty party = getParty(player.getParty().getId());
            if (party == null) {
                return;
            }

            Item item = null;
            if (recv == RecvPacketOpcode.PARTYCHATITEM) {
                if (player != null) {
                    byte invType = (byte) slea.readInt();
                    short pos = (short) slea.readInt();
                    item = player.getInventory(MapleInventoryType.getByType((pos > 0 ? invType : -1))).getItem(pos);
                }
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null && !chr.getName().equalsIgnoreCase(player.getName())) { //Extra check just in case

                        chr.getClient().getSession().writeAndFlush(CField.multiChat(player, chattext, mode, item));
                        if (chr.getClient().isMonitored()) {
//                            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "", "[GM Message] " + player.getName() + " said to " + chr.getName() + " (Party): " + chattext));
                        }
                    }
                }
            }
        }

        public static void partyMessage(int partyid, String chattext) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) { //Extra check just in case
                        chr.dropMessage(5, chattext);
                    }
                }
            }
        }

        public static void expedMessage(int expedId, String chattext) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyMessage(i, chattext);
            }
        }

        public static void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return; //Don't update, just return. And definitely don't throw a damn exception.
                //throw new IllegalArgumentException("no party with the specified partyid exists");
            }
            final int oldExped = party.getExpeditionId();
            final int oldSize = party.getMembers().size();
            int oldInd = -1;
            if (oldExped > 0) {
                MapleExpedition exped = getExped(oldExped);
                if (exped != null) {
                    oldInd = exped.getIndex(partyid);
                }
            }
            switch (operation) {
                case JOIN:
                    party.addMember(target);
                    if (party.getMembers().size() >= 6) {
                        PartySearch toRemove = getSearchByParty(partyid);
                        if (toRemove != null) {
                            removeSearch(toRemove, "The Party Listing was removed because the party is full.");
                        } else if (party.getExpeditionId() > 0) {
                            MapleExpedition exped = getExped(party.getExpeditionId());
                            if (exped != null && exped.getAllMembers() >= exped.getType().maxMembers) {
                                toRemove = getSearchByExped(exped.getId());
                                if (toRemove != null) {
                                    removeSearch(toRemove, "The Party Listing was removed because the party is full.");
                                }
                            }
                        }
                    }
                    break;
                case EXPEL:
                case LEAVE:
                    party.removeMember(target);
                    break;
                case DISBAND:
                    disbandParty(partyid);
                    break;
                case SILENT_UPDATE:
                case LOG_ONOFF:
                    party.updateMember(target);
                    break;
                case CHANGE_LEADER:
                case CHANGE_LEADER_DC:
                    party.setLeader(target);
                    break;
                default:
                    throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
            }
            if (operation == PartyOperation.LEAVE || operation == PartyOperation.EXPEL) {
                int chz = Find.findChannel(target.getName());
                if (chz > 0) {
                    MapleCharacter chr = getStorage(chz).getCharacterByName(target.getName());
                    if (chr != null) {
                        chr.setParty(null);
                        if (oldExped > 0) {
                            chr.getClient().getSession().writeAndFlush(ExpeditionPacket.expeditionMessage(80));
                        }
                        chr.getClient().getSession().writeAndFlush(PartyPacket.updateParty(chr.getClient().getChannel(), party, operation, target));
                    }
                }
                if (target.getId() == party.getLeader().getId() && party.getMembers().size() > 0) { //pass on lead
                    MaplePartyCharacter lchr = null;
                    for (MaplePartyCharacter pchr : party.getMembers()) {
                        if (pchr != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                            lchr = pchr;
                        }
                    }
                    if (lchr != null) {
                        updateParty(partyid, PartyOperation.CHANGE_LEADER_DC, lchr);
                    }
                }
            }
            if (party.getMembers().size() <= 0) { //no members left, plz disband
                disbandParty(partyid);
            }
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar == null) {
                    continue;
                }
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = getStorage(ch).getCharacterByName(partychar.getName());
                    if (chr != null) {

                        if (operation == PartyOperation.DISBAND) {
                            chr.setParty(null);
                            if (oldExped > 0) {
                                chr.getClient().getSession().writeAndFlush(ExpeditionPacket.expeditionMessage(83));
                            }
                        } else {
                            chr.setParty(party);
                        }
                        chr.getClient().getSession().writeAndFlush(PartyPacket.updateParty(chr.getClient().getChannel(), party, operation, target));
                        chr.getStat().recalcLocalStats(chr);
                    }
                }
            }

//            if (oldExped > 0) {
            //              expedPacket(oldExped, ExpeditionPacket.expeditionUpdate(oldInd, party), operation == PartyOperation.LOG_ONOFF || operation == PartyOperation.SILENT_UPDATE ? target : null);
            //        }
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor) {
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor);
            parties.put(party.getId(), party);
            return party;
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor, int expedId) {
            ExpeditionType ex = ExpeditionType.getById(expedId);
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, ex != null ? runningExpedId.getAndIncrement() : -1);
            parties.put(party.getId(), party);
            if (ex != null) {
                final MapleExpedition exp = new MapleExpedition(ex, chrfor.getId(), party.getExpeditionId());
                exp.getParties().add(party.getId());
                expeds.put(party.getExpeditionId(), exp);
            }
            return party;
        }

        public static MapleParty createPartyAndAdd(MaplePartyCharacter chrfor, int expedId) {
            MapleExpedition ex = getExped(expedId);
            if (ex == null) {
                return null;
            }
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, expedId);
            parties.put(party.getId(), party);
            ex.getParties().add(party.getId());
            return party;
        }

        public static MapleParty getParty(int partyid) {
            return parties.get(partyid);
        }

        public static MapleExpedition getExped(int partyid) {
            return expeds.get(partyid);
        }

        public static MapleExpedition disbandExped(int partyid) {
            PartySearch toRemove = getSearchByExped(partyid);
            if (toRemove != null) {
                removeSearch(toRemove, "The Party Listing was removed because the party disbanded.");
            }
            final MapleExpedition ret = expeds.remove(partyid);
            if (ret != null) {
                for (int p : ret.getParties()) {
                    MapleParty pp = getParty(p);
                    if (pp != null) {
                        updateParty(p, PartyOperation.DISBAND, pp.getLeader());
                    }
                }
            }
            return ret;
        }

        public static MapleParty disbandParty(int partyid) {
            PartySearch toRemove = getSearchByParty(partyid);
            if (toRemove != null) {
                removeSearch(toRemove, "The Party Listing was removed because the party disbanded.");
            }
            final MapleParty ret = parties.remove(partyid);
            if (ret == null) {
                return null;
            }
            if (ret.getExpeditionId() > 0) {
                MapleExpedition me = getExped(ret.getExpeditionId());
                if (me != null) {
                    final int ind = me.getIndex(partyid);
                    if (ind >= 0) {
                        me.getParties().remove(ind);
//                        expedPacket(me.getId(), ExpeditionPacket.expeditionUpdate(ind, null), null);
                    }
                }
            }
            ret.disband();
            return ret;
        }

        public static List<PartySearch> searchParty(PartySearchType pst) {
            return searches.get(pst);
        }

        public static void removeSearch(PartySearch ps, String text) {
            List<PartySearch> ss = searches.get(ps.getType());
            if (ss.contains(ps)) {
                ss.remove(ps);
                ps.cancelRemoval();
                if (ps.getType().exped) {
                    expedMessage(ps.getId(), text);
                } else {
                    partyMessage(ps.getId(), text);
                }
            }
        }

        public static void addSearch(PartySearch ps) {
            searches.get(ps.getType()).add(ps);
        }

        public static PartySearch getSearch(MapleParty party) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if ((p.getId() == party.getId() && !p.getType().exped) || (p.getId() == party.getExpeditionId() && p.getType().exped)) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static PartySearch getSearchByParty(int partyId) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if (p.getId() == partyId && !p.getType().exped) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static PartySearch getSearchByExped(int partyId) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if (p.getId() == partyId && p.getType().exped) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static boolean partyListed(MapleParty party) {
            return getSearchByParty(party.getId()) != null;
        }
    }

    public static class Buddy {

        public static void buddyChat(int[] recipientCharacterIds, MapleCharacter player, String chattext, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
            String targets = "";

            Item item = null;
            if (recv == RecvPacketOpcode.PARTYCHATITEM) {
                if (player != null) {
                    byte invType = (byte) slea.readInt();
                    byte pos = (byte) slea.readInt();
                    item = player.getInventory(MapleInventoryType.getByType((pos > 0 ? invType : -1))).getItem(pos);
                }
            }

            for (int characterId : recipientCharacterIds) {
                int ch = Find.findChannel(characterId);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(characterId);
                    if (chr != null && player != null && chr.getBuddylist().containsVisible(player.getAccountID())) {
                        targets += chr.getName() + ", ";
                        chr.getClient().getSession().writeAndFlush(CField.multiChat(player, chattext, 0, item));
                        if (chr.getClient().isMonitored()) {
//                            World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "", "[GM Message] " + player.getName() + " said to " + chr.getName() + " (Buddy): " + chattext));
                        }
                    }
                }
            }
            DBLogger.getInstance().logChat(LogType.Chat.Buddy, player.getId(), player.getName(), chattext, "수신 : " + targets);
        }

        public static void updateBuddies(String name, int characterId, int channel, int[] buddies, int accId, boolean offline) {
            for (int buddy : buddies) {
                int ch = World.Find.findAccChannel(buddy);
                if (ch > 0) {
                    MapleClient c = ChannelServer.getInstance(ch).getPlayerStorage().getClientById(buddy);
                    if (c != null && c.getPlayer() != null) {
                        BuddylistEntry ble = c.getPlayer().getBuddylist().get(accId);
                        if (ble != null && ble.isVisible()) {
                            int mcChannel;
                            if (offline) {
                                ble.setChannel(-1);
                                mcChannel = -1;
                            } else {
                                ble.setChannel(channel);
                                mcChannel = channel - 1;
                            }
                            ble.setName(name);
                            ble.setCharacterId(characterId);
                            //                        chr.getClient().getSession().writeAndFlush(BuddylistPacket.updateBuddylist(chr.getBuddylist().getBuddies(), ble, (byte) 25));
                            c.getSession().writeAndFlush(BuddylistPacket.updateBuddyChannel(ble.getCharacterId(), accId, mcChannel, name));
                        }
                    }
                }
            }
        }

        public static void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation, int level, int job, int accId, String memo) {
            int ch = Find.findChannel(cid);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(cid);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    switch (operation) {
                        case ADDED:
                            if (buddylist.contains(accId)) {
                                buddylist.put(new BuddylistEntry(name, name, accId, cidFrom, "그룹 미지정", channel, true, level, job, memo));
                                addChar.getClient().getSession().writeAndFlush(BuddylistPacket.updateBuddyChannel(cidFrom, accId, channel, buddylist.get(accId).getName()));
                            }
                            break;
                        case DELETED:
                            if (buddylist.contains(accId)) {
                                buddylist.put(new BuddylistEntry(name, name, accId, cidFrom, "그룹 미지정", -1, buddylist.get(accId).isVisible(), level, job, memo));
                                addChar.getClient().getSession().writeAndFlush(BuddylistPacket.updateBuddyChannel(cidFrom, accId, -1, buddylist.get(accId).getName()));
                            }
                            break;
                    }
                }
            }
        }

        public static BuddyList.BuddyAddResult requestBuddyAdd(String addName, int accid, int channelFrom, int cidFrom, String nameFrom, int levelFrom, int jobFrom, String groupName, String memo) {
            for (ChannelServer server : ChannelServer.getAllInstances()) {
                final MapleCharacter addChar = server.getPlayerStorage().getCharacterByName(addName);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    if (buddylist.isFull()) {
                        return BuddyList.BuddyAddResult.BUDDYLIST_FULL;
                    }
                    if (!buddylist.contains(accid)) {
                        buddylist.addBuddyRequest(addChar.getClient(), accid, cidFrom, nameFrom, nameFrom, channelFrom, levelFrom, jobFrom, groupName, memo);
                    } else {
                        if (buddylist.containsVisible(accid)) {
                            return BuddyList.BuddyAddResult.ALREADY_ON_LIST;
                        }
                    }
                }
            }
            return BuddyList.BuddyAddResult.OK;

        }

        public static void loggedOn(String name, int characterId, int channel, int accId, int[] buddies) {
            updateBuddies(name, characterId, channel, buddies, accId, false);
        }

        public static void loggedOff(String name, int characterId, int channel, int accId, int[] buddies) {
            updateBuddies(name, characterId, channel, buddies, accId, true);
        }
    }

    public static class Messenger {

        private static Map<Integer, MapleMessenger> messengers = new HashMap<Integer, MapleMessenger>();
        private static final AtomicInteger runningMessengerId = new AtomicInteger();

        static {
            runningMessengerId.set(1);
        }

        public static MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
            int messengerid = runningMessengerId.getAndIncrement();
            MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
            messengers.put(messenger.getId(), messenger);
            return messenger;
        }

        public static void declineChat(String target, String namefrom) {
            int ch = Find.findChannel(target);
            if (ch > 0) {
                ChannelServer cs = ChannelServer.getInstance(ch);
                MapleCharacter chr = cs.getPlayerStorage().getCharacterByName(target);
                if (chr != null) {
                    MapleMessenger messenger = chr.getMessenger();
                    if (messenger != null) {
                        chr.getClient().getSession().writeAndFlush(CField.messengerNote(namefrom, 5, 0));
                    }
                }
            }
        }

        public static MapleMessenger getMessenger(int messengerid) {
            return messengers.get(messengerid);
        }

        public static void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            int position = messenger.getPositionByName(target.getName());
            messenger.removeMember(target);

            for (MapleMessengerCharacter mmc : messenger.getMembers()) {
                if (mmc != null) {
                    int ch = Find.findChannel(mmc.getId());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(mmc.getName());
                        if (chr != null) {
                            chr.getClient().getSession().writeAndFlush(CField.removeMessengerPlayer(position));
                        }
                    }
                }
            }
        }

        public static void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentRemoveMember(target);
        }

        public static void silentJoinMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentAddMember(target);
        }

        public static void updateMessenger(int messengerid, String namefrom, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            int position = messenger.getPositionByName(namefrom);

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                            chr.getClient().getSession().writeAndFlush(CField.updateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                        }
                    }
                }
            }
        }

        public static void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.addMember(target);
            int position = messenger.getPositionByName(target.getName());
            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null) {
                    int mposition = messenger.getPositionByName(messengerchar.getName());
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            if (!messengerchar.getName().equals(from)) {
                                MapleCharacter fromCh = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(from);
                                if (fromCh != null) {
                                    chr.getClient().getSession().writeAndFlush(CField.addMessengerPlayer(from, fromCh, position, fromchannel - 1));
                                    fromCh.getClient().getSession().writeAndFlush(CField.addMessengerPlayer(chr.getName(), chr, mposition, messengerchar.getChannel() - 1));
                                }
                            } else {
                                chr.getClient().getSession().writeAndFlush(CField.joinMessenger(mposition));
                            }
                        }
                    }
                }
            }
        }

        public static void messengerChat(int messengerid, String charname, String text, String namefrom) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            chr.getClient().getSession().writeAndFlush(CField.messengerChat(charname, text));
                        }
                    }
                }
            }
        }

        public static void messengerWhisperChat(int messengerid, String charname, String text, String namefrom) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            chr.getClient().getSession().writeAndFlush(CField.messengerWhisperChat(charname, text));
                        }
                    }
                }
            }
        }

        public static void messengerInvite(String sender, int messengerid, String target, int fromchannel, boolean gm) {

            if (isConnected(target)) {

                int ch = Find.findChannel(target);
                if (ch > 0) {
                    MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender);
                    MapleCharacter targeter = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(target);
                    if (targeter != null && targeter.getMessenger() == null) {
                        if (!targeter.isIntern() || gm) {
                            targeter.getClient().getSession().writeAndFlush(CField.messengerInvite(sender, messengerid));
                            from.getClient().getSession().writeAndFlush(CField.messengerNote(target, 4, 1));
                        } else {
                            from.getClient().getSession().writeAndFlush(CField.messengerNote(target, 4, 0));
                        }
                    } else {
                        from.getClient().getSession().writeAndFlush(CField.messengerChat(sender, " : " + target + " is already using Maple Messenger"));
                    }
                }
            }

        }
    }

    public static class Guild {

        private static final Map<Integer, MapleGuild> guilds = new ConcurrentHashMap<Integer, MapleGuild>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public static void addLoadedGuild(MapleGuild f) {
            if (f.isProper()) {
                guilds.put(f.getId(), f);
            }
        }

        public static int createGuild(int leaderId, String name) {
            return MapleGuild.createGuild(leaderId, name);
        }

        public static List<MapleGuild> getGuilds() {
            List<MapleGuild> ret = new ArrayList<>();
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    ret.add(g);
                }
                return ret;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static List<MapleGuild> getGuilds(int minlevel, int maxlevel, int minsize, int maxsize, int minmlevel, int maxmlevel) {
            List<MapleGuild> ret = new ArrayList<>();
            lock.readLock().lock();
            try {
                for (MapleGuild guild : guilds.values()) {
                    if (guild.getLevel() >= minlevel && guild.getLevel() <= maxlevel && guild.getMembers().size() >= minsize && guild.getMembers().size() <= maxsize && guild.getLevel() >= minmlevel && guild.getLevel() <= maxmlevel) {
                        ret.add(guild);
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            return ret;
        }

        public static MapleGuild getGuild(int id) {
            MapleGuild ret = guilds.get(id);
            if (ret == null) {
                ret = new MapleGuild(id);
                if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                    return null;
                }
                guilds.put(id, ret);
            }
            return ret; //Guild doesn't exist?
        }

        public static List<MapleGuild> getGuildsByName(String name) {
            List<MapleGuild> ret = new ArrayList<>();
            for (MapleGuild g : guilds.values()) {
                if (g.getName().matches(name)) {
                    ret.add(g);
                }
            }
            return ret;
        }

        public static List<MapleGuild> getGuildsByName(String name, boolean check) {
            List<MapleGuild> ret = new ArrayList<>();
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    if (check) {
                        if (g.getName().contentEquals(name)) {
                            ret.add(g);
                        }
                    } else {
                        if (g.getName().contains(name)) {
                            ret.add(g);
                        }
                    }
                }
                return ret;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static MapleGuild getGuildByName(String guildName) {
            for (MapleGuild g : guilds.values()) {
                if (g.getName().equalsIgnoreCase(guildName)) {
                    return g;
                }
            }
            return null;
        }

        public static MapleGuild getGuild(MapleCharacter mc) {
            return getGuild(mc.getGuildId());
        }

        public static void setGuildMemberOnline(MapleGuildCharacter mc, boolean bOnline, int channel) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.setOnline(mc.getId(), bOnline, channel);
            }
        }

        public static List<MapleGuild> getGuildsByLeader(String name, boolean check) {
            List<MapleGuild> ret = new ArrayList<>();
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    if (check) {
                        if (g.getLeaderName().contentEquals(name)) {
                            ret.add(g);
                        } else {
                            getGuildsByName(name, check);
                        }
                    } else {
                        if (g.getLeaderName().contains(name)) {
                            ret.add(g);
                        } else {
                            getGuildsByName(name, check);
                        }
                    }
                }
                return ret;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static List<MapleGuild> getGuildsByAll(String name, boolean check) {
            List<MapleGuild> ret = new ArrayList<>();
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    if (check) {
                        if (g.getLeaderName().contentEquals(name)) {
                            ret.add(g);
                        }
                        if (g.getName().contentEquals(name)) {
                            ret.add(g);
                        }
                    } else {
                        if (g.getLeaderName().contentEquals(name)) {
                            ret.add(g);
                        }
                        if (g.getName().contains(name)) {
                            ret.add(g);
                        }
                    }
                }
                return ret;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static void guildPacket(int gid, byte[] message) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.broadcast(message);
            }
        }

        public static int addGuildMember(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                return g.addGuildMember(mc);
            }
            return 0;
        }

        public static void leaveGuild(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.leaveGuild(mc);
            }
        }

        public static void guildChat(MapleCharacter chr, String msg, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.guildChat(chr, msg, slea, recv);
            }
        }

        public static void changeRank(int gid, int cid, int newRank) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRank(cid, newRank);
            }
        }

        public static void expelMember(MapleGuildCharacter initiator, String name, int cid) {
            MapleGuild g = getGuild(initiator.getGuildId());
            if (g != null) {
                g.expelMember(initiator, name, cid);
            }
        }

        public static void setGuildNotice(MapleCharacter chr, String notice) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.setGuildNotice(chr, notice);
            }
        }

        public static void setGuildLeader(int gid, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeGuildLeader(cid);
            }
        }

        public static int getSkillLevel(int gid, int sid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getSkillLevel(sid);
            }
            return 0;
        }

        public static boolean purchaseSkill(int gid, int sid, String name, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.purchaseSkill(sid, name, cid);
            }
            return false;
        }

        public static boolean activateSkill(int gid, int sid, String name) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.activateSkill(sid, name);
            }
            return false;
        }

        public static void memberLevelJobUpdate(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.memberLevelJobUpdate(mc);
            }
        }

        public static void changeRankTitle(MapleCharacter chr, String[] ranks) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.changeRankTitle(chr, ranks);
            }
        }

        public static void changeRankRole(MapleCharacter chr, int[] roles) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.changeRankRole(chr, roles);
            }
        }

        public static void changeRankTitleRole(MapleCharacter chr, String[] ranks, int[] roles) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.changeRankTitleRole(chr, ranks, roles);
            }
        }

        public static void setGuildEmblem(MapleCharacter chr, short bg, byte bgcolor, short logo, byte logocolor) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.setGuildEmblem(chr, bg, bgcolor, logo, logocolor);
            }
        }

        public static void setGuildCustomEmblem(MapleCharacter chr, byte[] imgdata) {
            MapleGuild g = getGuild(chr.getGuildId());
            if (g != null) {
                g.setGuildCustomEmblem(chr, imgdata);
            }
        }

        public static void disbandGuild(int gid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.disbandGuild();
                guilds.remove(gid);
            }
        }

        public static void deleteGuildCharacter(int guildid, int charid) {

            //ensure it's loaded on world server
            //setGuildMemberOnline(mc, false, -1);
            MapleGuild g = getGuild(guildid);
            if (g != null) {
                MapleGuildCharacter mc = g.getMGC(charid);
                if (mc != null) {
                    if (mc.getGuildRank() > 1) //not leader
                    {
                        g.leaveGuild(mc);
                    } else {
                        g.disbandGuild();
                    }
                }
            }
        }

        public static boolean increaseGuildCapacity(int gid, boolean b) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.increaseCapacity(b);
            }
            return false;
        }

        public static void gainContribution(int gid, int amount) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount);
            }
        }

        public static void gainContribution(int gid, int amount, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount, false, cid);
            }
        }

        public static int getGP(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getGP();
            }
            return 0;
        }

        public static int getInvitedId(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getInvitedId();
            }
            return 0;
        }

        public static void setInvitedId(final int gid, final int inviteid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setInvitedId(inviteid);
            }
        }

        public static int getGuildLeader(final int guildName) {
            final MapleGuild mga = getGuild(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static int getGuildLeader(final String guildName) {
            final MapleGuild mga = getGuildByName(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void save() {
            System.out.println("Saving guilds...");
            for (MapleGuild a : guilds.values()) {
                a.writeToDB(false);
            }
        }

        public static void load() {
            System.out.println("Load guilds...");
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM guilds");

                rs = ps.executeQuery();

                if (rs.next()) {
                    getGuild(rs.getInt("guildid"));
                }
                rs.close();
                ps.close();
                con.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        public static void changeEmblem(MapleCharacter chr, int affectedPlayers, MapleGuild mgs) {
            Broadcast.sendGuildPacket(affectedPlayers, GuildPacket.guildEmblemChange(chr, (short) mgs.getLogoBG(), (byte) mgs.getLogoBGColor(), (short) mgs.getLogo(), (byte) mgs.getLogoColor()), -1, chr.getGuildId());
            setGuildAndRank(affectedPlayers, -1, -1, -1, -1);   //respawn player
        }

        public static void setGuildAndRank(int cid, int guildid, int rank, int contribution, int alliancerank) {
            int ch = Find.findChannel(cid);
            if (ch == -1) {
                // System.out.println("ERROR: cannot find player in given channel");
                return;
            }
            MapleCharacter mc = getStorage(ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferentGuild;
            if (guildid == -1 && rank == -1) { //just need a respawn
                bDifferentGuild = true;
            } else {
                bDifferentGuild = guildid != mc.getGuildId();
                mc.setGuildId(guildid);
                mc.setGuildRank((byte) rank);
                mc.setGuildContribution(contribution);
                mc.setAllianceRank((byte) alliancerank);
                mc.saveGuildStatus();
            }
            if (bDifferentGuild && ch > 0) {
//                mc.getMap().broadcastMessage(mc, CField.loadGuildName(mc), false);
                mc.getMap().broadcastMessage(mc, CField.loadGuildIcon(mc), false);
            }
        }
    }

    public static class Broadcast {

        public static long chatDelay = 0;

        public static void broadcastSmega(byte[] message) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.broadcastSmega(message);
            }
        }

        public static void broadcastGMMessage(byte[] message) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.broadcastGMMessage(message);
            }
        }

        public static void broadcastMessage(byte[] message) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.broadcastMessage(message);
            }
        }

        public static void sendPacket(List<Integer> targetIds, byte[] packet, int exception) {
            MapleCharacter c;
            for (int i : targetIds) {
                if (i == exception) {
                    continue;
                }
                int ch = Find.findChannel(i);
                if (ch < 0) {
                    continue;
                }
                c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(i);
                if (c != null) {
                    c.getClient().getSession().writeAndFlush(packet);
                }
            }
        }

        public static void sendPacket(int targetId, byte[] packet) {
            int ch = Find.findChannel(targetId);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetId);
            if (c != null) {
                c.getClient().getSession().writeAndFlush(packet);
            }
        }

        public static void sendGuildPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getGuildId() == guildid) {
                c.getClient().getSession().writeAndFlush(packet);
            }
        }
    }

    public static class Find {

        private static Map<Integer, Integer> idToChannel = new ConcurrentHashMap<Integer, Integer>();
        private static Map<String, Integer> nameToChannel = new ConcurrentHashMap<String, Integer>();
        private static Map<Integer, Integer> accIdToChannel = new ConcurrentHashMap<>();

        public static void forceDeregister(int id) {
            idToChannel.remove(id);
            //System.out.println("Char removed: " + id);
        }

        public static void forceDeregister(String id) {
            nameToChannel.remove(id.toLowerCase());
            //System.out.println("Char removed: " + id);
        }

        public static void register(int id, int accId, String name, int channel) {
            idToChannel.put(id, channel);
            nameToChannel.put(name.toLowerCase(), channel);
            accIdToChannel.put(accId, channel);
            //System.out.println("Char added: " + id + " " + name + " to channel " + channel);
        }

        public static void forceAccDeregister(int id) {
            accIdToChannel.remove(id);
            //System.out.println("Char removed: " + id);
        }

        public static void forceDeregister(int id, int accId, String name) {
            idToChannel.remove(id);
            nameToChannel.remove(name.toLowerCase());
            accIdToChannel.remove(accId);
            //System.out.println("Char removed: " + id + " " + name);
        }

        public static int findChannel(int id) {
            Integer ret = idToChannel.get(id);
            if (ret != null) {
                if (ret != -10 && ChannelServer.getInstance(ret) == null) { //wha
                    forceDeregister(id);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static int findChannel(String st) {
            Integer ret = nameToChannel.get(st.toLowerCase());
            if (ret != null) {
                if (ret != -10 && ChannelServer.getInstance(ret) == null) { //wha
                    forceDeregister(st);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static int findAccChannel(int id) {
            Integer ret = accIdToChannel.get(id);
            if (ret != null) {
                if (ret != -10 && ChannelServer.getInstance(ret) == null) { //wha
                    forceAccDeregister(id);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static AccountIdChannelPair[] multiBuddyFind(BuddyList bl, int charIdFrom, int[] accIds) {
            List<AccountIdChannelPair> foundsChars = new ArrayList<AccountIdChannelPair>(accIds.length);
            for (int i : accIds) {
                int ret = findAccChannel(i);
                if (ret > 0) {
                    MapleClient c = ChannelServer.getInstance(ret).getPlayerStorage().getClientById(i);
                    if (bl.contains(i) && c != null) {
                        BuddylistEntry ble = bl.get(i);
                        ble.setCharacterId(c.getPlayer().getId());
                        ble.setName(c.getPlayer().getName());
                    }
                }
                foundsChars.add(new AccountIdChannelPair(i, ret));
            }
            Collections.sort(foundsChars);
            return foundsChars.toArray(new AccountIdChannelPair[foundsChars.size()]);
        }
    }

    public static class Alliance {

        private static final Map<Integer, MapleGuildAlliance> alliances = new ConcurrentHashMap<Integer, MapleGuildAlliance>();

        static {
            Collection<MapleGuildAlliance> allGuilds = MapleGuildAlliance.loadAll();
            for (MapleGuildAlliance g : allGuilds) {
                alliances.put(g.getId(), g);
            }
        }

        public static MapleGuildAlliance getAlliance(final int allianceid) {
            MapleGuildAlliance ret = alliances.get(allianceid);
            if (ret == null) {
                ret = new MapleGuildAlliance(allianceid);
                if (ret == null || ret.getId() <= 0) { //failed to load
                    return null;
                }
                alliances.put(allianceid, ret);
            }
            return ret;
        }

        public static int getAllianceLeader(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void updateAllianceRanks(final int allianceid, final String[] ranks) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setRank(ranks);
            }
        }

        public static void updateAllianceNotice(final int allianceid, final String notice) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setNotice(notice);
            }
        }

        public static boolean canInvite(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getCapacity() > mga.getNoGuilds();
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid);
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid, final boolean sameGuild) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid, sameGuild);
            }
            return false;
        }

        public static boolean changeAllianceRank(final int allianceid, final int cid, final int change) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.changeAllianceRank(cid, change);
            }
            return false;
        }

        public static boolean changeAllianceCapacity(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setCapacity();
            }
            return false;
        }

        public static boolean disbandAlliance(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.disband();
            }
            return false;
        }

        public static boolean addGuildToAlliance(final int allianceid, final int gid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.addGuild(gid);
            }
            return false;
        }

        public static boolean removeGuildFromAlliance(final int allianceid, final int gid, final boolean expelled) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.removeGuild(gid, expelled);
            }
            return false;
        }

        public static void sendGuild(final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                sendGuild(AlliancePacket.getAllianceUpdate(alliance), -1, allianceid);
                sendGuild(AlliancePacket.getGuildAlliance(alliance), -1, allianceid);
            }
        }

        public static void sendGuild(final byte[] packet, final int exceptionId, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    int gid = alliance.getGuildId(i);
                    if (gid > 0 && gid != exceptionId) {
                        Guild.guildPacket(gid, packet);
                    }
                }
            }
        }

        public static boolean createAlliance(final String alliancename, final int cid, final int cid2, final int gid, final int gid2) {
            final int allianceid = MapleGuildAlliance.createToDb(cid, alliancename, gid, gid2);
            if (allianceid <= 0) {
                return false;
            }
            final MapleGuild g = Guild.getGuild(gid), g_ = Guild.getGuild(gid2);
            g.setAllianceId(allianceid);
            g_.setAllianceId(allianceid);
            g.changeARank(true);
            g_.changeARank(false);

            final MapleGuildAlliance alliance = getAlliance(allianceid);

            sendGuild(AlliancePacket.createGuildAlliance(alliance), -1, allianceid);
            sendGuild(AlliancePacket.getAllianceInfo(alliance), -1, allianceid);
            sendGuild(AlliancePacket.getGuildAlliance(alliance), -1, allianceid);
            sendGuild(AlliancePacket.changeAlliance(alliance, true), -1, allianceid);
            return true;
        }

        public static void allianceChat(MapleCharacter player, final String msg, final LittleEndianAccessor slea, final RecvPacketOpcode recv) {
            final MapleGuild g = Guild.getGuild(player.getGuildId());
            if (g != null) {
                final MapleGuildAlliance ga = getAlliance(g.getAllianceId());
                if (ga != null) {
                    for (int i = 0; i < ga.getNoGuilds(); i++) {
                        final MapleGuild g_ = Guild.getGuild(ga.getGuildId(i));
                        if (g_ != null) {
                            g_.allianceChat(player, msg, slea, recv);
                            if (i == 0) {
                                DBLogger.getInstance().logChat(LogType.Chat.Guild, player.getId(), player.getName(), msg, "[" + ga.getName() + " - 연합 ]");
                            }
                        }
                    }
                }
            }
        }

        public static void setNewAlliance(final int gid, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild guild = Guild.getGuild(gid);
            if (alliance != null && guild != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    if (gid == alliance.getGuildId(i)) {
                        guild.setAllianceId(allianceid);
                        guild.broadcast(AlliancePacket.getAllianceInfo(alliance));
                        guild.broadcast(AlliancePacket.getGuildAlliance(alliance));
                        guild.broadcast(AlliancePacket.changeAlliance(alliance, true));
                        guild.changeARank();
                        guild.writeToDB(false);
                    } else {
                        final MapleGuild g_ = Guild.getGuild(alliance.getGuildId(i));
                        if (g_ != null) {
                            g_.broadcast(AlliancePacket.addGuildToAlliance(alliance, guild));
                            g_.broadcast(AlliancePacket.changeGuildInAlliance(alliance, guild, true));
                        }
                    }
                }
            }
        }

        public static void setOldAlliance(final int gid, final boolean expelled, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild g_ = Guild.getGuild(gid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    final MapleGuild guild = Guild.getGuild(alliance.getGuildId(i));
                    if (guild == null) {
                        if (gid != alliance.getGuildId(i)) {
                            alliance.removeGuild(gid, false, true);
                        }
                        continue; //just skip
                    }
                    if (g_ == null || gid == alliance.getGuildId(i)) {
                        guild.changeARank(5);
                        guild.setAllianceId(0);
                        guild.broadcast(AlliancePacket.disbandAlliance(allianceid));
                    } else if (g_ != null) {
                        guild.broadcast(CWvsContext.serverNotice(5, "", "[" + g_.getName() + "] Guild has left the alliance."));
                        guild.broadcast(AlliancePacket.changeGuildInAlliance(alliance, g_, false));
                        guild.broadcast(AlliancePacket.removeGuildFromAlliance(alliance, g_, expelled));
                    }

                }
            }

            if (gid == -1) {
                alliances.remove(allianceid);
            }
        }

        public static List<byte[]> getAllianceInfo(final int allianceid, final boolean start) {
            List<byte[]> ret = new ArrayList<byte[]>();
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                if (start) {
                    ret.add(AlliancePacket.getAllianceInfo(alliance));
                    ret.add(AlliancePacket.getGuildAlliance(alliance));
                }
                ret.add(AlliancePacket.getAllianceUpdate(alliance));
            }
            return ret;
        }

        public static void save() {
            System.out.println("Saving alliances...");
            for (MapleGuildAlliance a : alliances.values()) {
                a.saveToDb();
            }
        }
    }

}
