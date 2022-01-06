package connector;

import client.MapleCharacter;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.Timer;
import server.quest.MapleQuest;
import tools.FileoutputUtil;

/**
 * @author SLFCG & 글귀
 */
public class ConnectorClient {

    public final static AttributeKey<ConnectorClient> CLIENTKEY = AttributeKey.valueOf("connectorclient_netty");
    private Channel session;
    private SGAES m_send, m_recv;
    private String id, pw, connecterkey, hardid, mac, charname, sid, spw;
    private final Map<String, String> ingamechar = new HashMap<String, String>();
    private MapleCharacter chr, secondchr;
    private final transient Lock mutex = new ReentrantLock(true);
    private int pingv, accountid, saccountid;
    private long pingtime, chattime;
    private transient ScheduledFuture<?> SkillTask;
    private boolean asd = false;
    private int bcd;

    private static byte[] sShiftKey = new byte[]{
        (byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4, (byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF,
        (byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC, (byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1,
        (byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C, (byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD,
        (byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3, (byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0
    };
    private String auth;

    public ConnectorClient(Channel socket, SGAES send, SGAES recv) {
        this.session = socket;
        this.m_send = send;
        this.m_recv = recv;
    }

    public void send(byte[] p) {
        getSession().writeAndFlush(p);
    }

    public void sendPacket(byte[] data) {
        if (data == null) {
            return;
        }
        byte[] temp = new byte[data.length];
        System.arraycopy(data, 0, temp, 0, data.length);
        session.writeAndFlush(temp);
    }

    public final MapleCharacter getSearchChar() {

        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter player : cs.getPlayerStorage().getAllCharacters().values()) {
                if (player.getClient().getAccountName().equals(this.id)) {
                    return player;
                }
            }
        }
        return null;
    }

    public final void addInGameChar(String s, String chr) {
        if (ingamechar.get(s) == null) {
            ingamechar.put(s, chr);
        }
    }

    public final void removeInGameChar(String s) {
        ingamechar.remove(s);
    }

    public final Map<String, String> getIngameChars() {
        return ingamechar;
    }

    public final String getIngameChar(String s) {
        return ingamechar.get(s);
    }

    public final String getIngameCharString() {
        StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> qs : ingamechar.entrySet()) {
            sb.append(qs.getValue()).append(",");
        }
        return sb.toString();
    }

    public final void setChatTime(long a) {
        this.chattime = a;
    }

    public long getChatTime() {
        return this.chattime;
    }

    public final void setSendSkill(int a) {
        this.bcd = a;
    }

    public long getSendSkill() {
        return this.bcd;
    }

    public final void setAuth(String a) {
        this.auth = a;
    }

    public String getAuth() {
        return this.auth;
    }

    public final void setSendSkillLimit(boolean a) {
        this.asd = a;
    }

    public boolean getSendSkillLimit() {
        return this.asd;
    }

    public final void setAccountId(int a) {
        this.accountid = a;
    }

    public int getAccountId() {
        return this.accountid;
    }

    public final void setSecondAccountId(int a) {
        this.saccountid = a;
    }

    public final void setChar(MapleCharacter c) {
        this.chr = c;
    }

    public final MapleCharacter getChar() {
        return chr;
    }

    public final void setSecondChar(MapleCharacter c) {
        this.secondchr = c;
    }

    public final MapleCharacter getSecondChar() {
        return secondchr;
    }

    public int getSecondAccountId() {
        return this.saccountid;
    }

    public final void setCharName(String a) {
        this.charname = a;
    }

    public String getCharName() {
        return this.charname;
    }

    public final void setMac(String a) {
        this.mac = a;
    }

    public String getMac() {
        return this.mac;
    }

    public final void setHard(String a) {
        this.hardid = a;
    }

    public String getHard() {
        return this.hardid;
    }

    public final void setConnecterKey(String a) {
        this.connecterkey = a;
    }

    public String getConnecterKey() {
        return this.connecterkey;
    }

    public final void setId(String a) {
        this.id = a;
    }

    public String getId() {
        return this.id;
    }

    public final void setPasswrod(String a) {
        this.pw = a;
    }

    public String getPassword() {
        return this.pw;
    }

    public final void setSecondId(String a) {
        this.sid = a;
    }

    public String getSecondId() {
        return this.sid;
    }

    public final void setSecondPasswrod(String a) {
        this.spw = a;
    }

    public String getSecondPassword() {
        return this.spw;
    }

    public int getPing() {
        return this.pingv;
    }

    public void setPing(int a) {
        this.pingv = a;
    }

    public final void setPingTime(long a) {
        this.pingtime = a;
    }

    public long getPingTime() {
        return this.pingtime;
    }

    public final Channel getSession() {
        return session;
    }

    public final SGAES getSendCrypto() {
        return this.m_send;
    }

    public final SGAES getRecvCrypto() {
        return this.m_recv;
    }

    public String getIP() {
        return session.remoteAddress().toString().split(":")[0].split("/")[1];
    }

    public String getAddressIP() {
        return session.remoteAddress().toString().split("/")[1];
    }

    public final Lock getLock() {
        return mutex;
    }

    public String processKillList() {
        String filePath = "processKill.txt";
        StringBuilder sb = new StringBuilder();
        try (
                InputStream inStream = new FileInputStream(filePath);
                InputStreamReader reader = new InputStreamReader(inStream, "UTF-8");
                BufferedReader bufReader = new BufferedReader(reader);) {
            String line = null;
            while ((line = bufReader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        };
        return sb.toString();
    }

    public void timer() {
        if (SkillTask == null) {
            SkillTask = Timer.MapTimer.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    if (getSendSkillLimit()) {
                        session.close();
                        ConnectorPanel.jTextArea2.append("스킬체크실패" + "\r\n");
                        ConnectorPanel.jTextArea2.setCaretPosition(ConnectorPanel.jTextArea2.getDocument().getLength());
                        FileoutputUtil.log(FileoutputUtil.커넥터로그, "스킬체크실패");
                    }
                    SkillTask = null;
                }
            }, 1000 * 60 * 3);
        }
    }

    public void DisableAccount(String id) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET allowed = ?, connecterClient = ?, connecterIP = ? WHERE name = ? or name = ?");
            ps.setByte(1, (byte) 0);
            ps.setString(2, null);
            ps.setString(3, null);
            ps.setString(4, id);
            ps.setString(5, this.sid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException ex) {
                    Logger.getLogger(ConnectorClient.class.getName()).log(Level.SEVERE, null, ex);
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
}
