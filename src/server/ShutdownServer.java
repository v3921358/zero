package server;

import handling.auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import server.Timer.*;
import server.marriage.MarriageManager;
import tools.packet.CWvsContext;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShutdownServer implements Runnable {

    public static final ShutdownServer instance = new ShutdownServer();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);
    public long startTime = 0;

    public static ShutdownServer getInstance() {
        return instance;
    }

    public int mode = 0;

    public void shutdown() {//can execute twice
        run();
    }

    @Override
    public void run() {
        startTime = System.currentTimeMillis();
        if (mode == 0) {
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "", "서버가 곧 종료됩니다. 안전한 저장을 위해 게임을 종료해주세요."));
            World.Guild.save();
            World.Alliance.save();
            AuctionServer.saveItems();
            MarriageManager.getInstance().saveAll();
            System.out.println("Shutdown 1 has completed.");
            mode++;
        } else if (mode == 1) {
            mode++;
            System.out.println("Shutdown 2 commencing...");
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "", "서버가 종료됩니다. 안전한 저장을 위해 게임을 종료해주세요."));

            AllShutdown sd = new AllShutdown();

            sd.start();
        }
    }

    private static class LoadingThread extends Thread {

        protected String LoadingThreadName;

        private LoadingThread(Runnable r, String t, Object o) {
            super(new NotifyingRunnable(r, o, t));
            LoadingThreadName = t;
        }

        @Override
        public synchronized void start() {
            System.out.println("[Loading...] Started " + LoadingThreadName + " Thread");
            super.start();
        }
    }

    private static class NotifyingRunnable implements Runnable {

        private String LoadingThreadName;
        private long StartTime;
        private Runnable WrappedRunnable;
        private final Object ToNotify;

        private NotifyingRunnable(Runnable r, Object o, String name) {
            WrappedRunnable = r;
            ToNotify = o;
            LoadingThreadName = name;
        }

        public void run() {
            StartTime = System.currentTimeMillis();
            WrappedRunnable.run();
            System.out.println("[Loading Completed] " + LoadingThreadName + " | Completed in " + (System.currentTimeMillis() - StartTime) + " Milliseconds. (" + (CompletedLoadingThreads.get() + 1) + "/10)");
            synchronized (ToNotify) {
                CompletedLoadingThreads.incrementAndGet();
                ToNotify.notify();
            }
        }
    }

    private class AllShutdown extends Thread {

        @Override
        public void run() {

            List<LoadingThread> loadingThreads = new ArrayList<>();

            Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);

            for (int i : chs) {
                try {
                    LoadingThread thread = new LoadingThread(new Runnable() {
                        public void run() {
                            ChannelServer cs = ChannelServer.getInstance(i);
                            cs.shutdown();
                        }
                    }, "Channel " + i, this);

                    loadingThreads.add(thread);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    continue;
                }
            }

            for (Thread t : loadingThreads) {
                t.start();
            }

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            while (CompletedLoadingThreads.get() != loadingThreads.size()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            WorldTimer.getInstance().stop();
            MapTimer.getInstance().stop();
            MobTimer.getInstance().stop();
            BuffTimer.getInstance().stop();
            CloneTimer.getInstance().stop();
            EventTimer.getInstance().stop();
            EtcTimer.getInstance().stop();
            PingTimer.getInstance().stop();
            LoginServer.shutdown();
            CashShopServer.shutdown();
            AuctionServer.shutdown();

            System.out.println("[Fully Shutdowned in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds]");

            System.out.println("Shutdown 2 has finished.");
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                //shutdown
            } finally {
                System.exit(0); //not sure if this is really needed for ChannelServer
            }
        }
    }
}
