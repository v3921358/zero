/**
 * @package : client
 * @author : Yein
 * @fileName : UnionList.java
 * @date : 2019. 10. 2.
 */
package client;

import constants.GameConstants;
import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UnionList {

    private List<MapleUnion> unions;

    public UnionList() {
        unions = new ArrayList<>();
    }

    public void loadFromDb(int accId) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT u.unk1, u.unk3, c.name as unionname, c.id as id, c.job as unionjob, c.level as unionlevel, u.position, u.position2 FROM unions as u, characters as c WHERE c.id = u.id && c.accountid = ?");
        ps.setInt(1, accId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            if (GameConstants.isZero(rs.getInt("unionjob"))) { // zero Exception
                if (rs.getInt("unionlevel") < 130) {
                    continue;
                }
            } else {
                if (rs.getInt("unionlevel") < 60) {
                    continue;
                }
            }
            unions.add(new MapleUnion(rs.getInt("id"), rs.getInt("unionlevel"), rs.getInt("unionjob"), rs.getInt("unk1"), rs.getInt("position2"), rs.getInt("position"), rs.getInt("unk3"), rs.getString("unionname")));
        }
        rs.close();
        ps.close();

        con.close();
    }

    public void loadFromTransfer(List<MapleUnion> unions) {
        this.unions.addAll(unions);
    }

    public void savetoDB(Connection con, int accId) throws SQLException {

        for (MapleUnion union : unions) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM unions WHERE id = ?");
            ps.setInt(1, union.getCharid());
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("INSERT INTO unions (id, unk1, unk3, position, position2) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, union.getCharid());
            ps.setInt(2, union.getUnk1());
            ps.setInt(3, union.getUnk3());
            ps.setInt(4, union.getPosition());
            ps.setInt(5, union.getUnk2());
            ps.executeUpdate();
            ps.close();
        }
    }

    public List<MapleUnion> getUnions() {
        return unions;
    }

    public void setUnions(List<MapleUnion> unions) {
        this.unions = unions;
    }

}
