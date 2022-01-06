/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tools.Pair;

/**
 *
 * @author cccv
 */
public class ConnectorClientStorage {

    private final Map<String, ConnectorClient> MainClients = new ConcurrentHashMap<>();//모두포함
    private final Map<String, ConnectorClient> LoginClients = new ConcurrentHashMap<>();//로그인한유저들
    private final Map<String, ConnectorClient> SClients = new ConcurrentHashMap<>();//세컨드계정

    private static final List<String> BlockedIP = new ArrayList<>();
    private static final Map<String, Long> BlockedTime = new ConcurrentHashMap<>();
    private static final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<>();

    private static final Map<String, String> removeWaiting = new ConcurrentHashMap<>();
    private static final Map<String, String> addInGameCharWaiting = new ConcurrentHashMap<>();

    public ConnectorClientStorage() {

    }

    public final void addBlockedIp(String address) {
        BlockedIP.add(address);
        BlockedTime.put(address, System.currentTimeMillis());
        tracker.remove(address); // Cleanup
    }

    public final Map<String, Pair<Long, Byte>> getTracker() {
        return tracker;
    }

    public final List<String> getBlockedIP() {
        return BlockedIP;
    }

    public final Map<String, Long> getBlockedTime() {
        return BlockedTime;
    }

    public final void addTracker(String address, byte count) {
        tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
    }

    public final void registerMainClient(final ConnectorClient c, final String s) {
        MainClients.put(s.toLowerCase(), c);
    }

    public final void registerClient(final ConnectorClient c, final String s) {
        LoginClients.put(s.toLowerCase(), c);
    }

    public final void registerSClient(final ConnectorClient c, final String s) {
        SClients.put(s.toLowerCase(), c);
    }

    public final void registerRemoveWaiting(final String c, final String s) {
        removeWaiting.put(s, c);
    }

    public final void deregisterRemoveWaiting(final String c) {
        if (c != null) {
            removeWaiting.remove(c);
        }
    }

    public final String getRemoveWaiting(final String c) {
        if (removeWaiting.get(c) != null) {
            return removeWaiting.get(c);
        }
        return null;
    }

    public final void registerChangeInGameCharWaiting(final String c, final String s) {
        if (getChangeInGameCharWaiting(c) == null) {
            addInGameCharWaiting.put(s, c);
        }
    }

    public final void deregisterChangeInGameCharWaiting(final String c) {
        if (c != null) {
            addInGameCharWaiting.remove(c);
        }
    }

    public final String getChangeInGameCharWaiting(final String c) {

        if (addInGameCharWaiting.get(c) != null) {
            return addInGameCharWaiting.get(c);
        }
        return null;
    }

    public final void deregisterClient(final ConnectorClient c) {
        if (c != null) {
            Iterator<ConnectorClient> main = getMainClients().iterator();
            while (main.hasNext()) {
                ConnectorClient cli = main.next();
                if (cli.getAddressIP().equals(c.getAddressIP())) {
                    removeMainClient(cli.getAddressIP());
                }
            }

            Iterator<ConnectorClient> login = getLoginClients().iterator();
            while (login.hasNext()) {
                ConnectorClient cli = login.next();
                if (cli.getId() == c.getId()) {
                    removeLoginClient(cli.getId());
                }
            }

            Iterator<ConnectorClient> slogin = getSClients().iterator();
            while (slogin.hasNext()) {
                ConnectorClient cli = slogin.next();
                if (cli.getSecondId() == c.getSecondId()) {
                    removeSClient(cli.getSecondId());
                }
            }
        }
    }

    public final ConnectorClient getClientByName(final String c) {
        if (LoginClients.get(c) != null) {
            return LoginClients.get(c);
        } else if (SClients.get(c) != null) {
            return SClients.get(c);
        } else if (MainClients.get(c) != null) {
            return MainClients.get(c);
        }
        return null;
    }

    public ConnectorClient getMainClient(String c) {
        return MainClients.get(c.toLowerCase());
    }

    public void removeMainClient(String c) {
        MainClients.remove(c.toLowerCase());
    }

    public ConnectorClient getLoginClient(String c) {
        return LoginClients.get(c.toLowerCase());
    }

    public void removeLoginClient(String c) {
        LoginClients.remove(c.toLowerCase());
    }

    public ConnectorClient getSClient(String c) {
        return LoginClients.get(c.toLowerCase());
    }

    public void removeSClient(String c) {
        LoginClients.remove(c.toLowerCase());
    }

    public final List<ConnectorClient> getMainClients() {
        final Iterator<ConnectorClient> itr = MainClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }

    public final List<ConnectorClient> getLoginClients() {
        final Iterator<ConnectorClient> itr = LoginClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }

    public final List<ConnectorClient> getSClients() {
        final Iterator<ConnectorClient> itr = SClients.values().iterator();
        List<ConnectorClient> asd = new ArrayList<>();
        while (itr.hasNext()) {
            asd.add(itr.next());
        }
        return asd;
    }
}
