/**
 * @package : constants
 * @author : Yein
 * @fileName : DeleteAccounts.java
 * @date : 2019. 7. 29.
 */
package constants;

import database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeleteAccounts {

    public static void main(String[] args) {

        DatabaseConnection.init();

        List<Integer> deletes = new ArrayList<>();

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("lastlogin");
                if (ts != null) {
                    if (ts.getYear() <= 2019 && ts.getDate() <= 31 && ts.getMonth() <= 4) {
                        deletes.add(rs.getInt("id"));
                    }
                } else {
                    deletes.add(rs.getInt("id"));
                }
            }
            ps.close();
            rs.close();

            for (int delete : deletes) {
                ps = con.prepareStatement("DELETE FROM accounts WHERE id = ?");
                ps.setInt(1, delete);
                ps.executeUpdate();
                ps.close();
            }
            con.close();

        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

}
