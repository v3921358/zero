package handling.auction.handler;

import database.DatabaseConnection;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionItemIdentifier implements Serializable {

    private static final long serialVersionUID = 21830921831301L;
    private final AtomicInteger runningUID = new AtomicInteger(0);
    private static AuctionItemIdentifier instance = new AuctionItemIdentifier();

    public static int getInstance() {
        return instance.getNextUniqueId();
    }

    public int getNextUniqueId() {
        if (runningUID.get() <= 0) {
            runningUID.set(initUID());
        } else {
            runningUID.set(runningUID.get() + 1);
        }
        return runningUID.get();
    }

    public int initUID() {
        int ret = 0;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            int[] ids = new int[6];
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT MAX(auctionitemid) FROM auctionitems");
            rs = ps.executeQuery();
            if (rs.next()) {
                ids[0] = rs.getInt(1) + 1;
            }
            rs.close();
            ps.close();
            con.close();

            for (int i = 0; i < ids.length; i++) {
                if (ids[i] > ret) {
                    ret = ids[i];
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}
