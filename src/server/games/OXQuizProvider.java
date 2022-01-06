package server.games;

import database.DatabaseConnection;
import server.Randomizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author SLFCG
 */
public class OXQuizProvider {

    public static OXQuiz[] getQuizList(final int amount) {
        OXQuiz[] list = new OXQuiz[amount];

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM OXQuiz", ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = ps.executeQuery();
            rs.last();
            int rowcount = rs.getRow();
            rs.beforeFirst();
            ps.close();
            rs.close();
            for (int a = 0; a < amount; a++) {
                final int quizid = Randomizer.rand(0, rowcount);
                ps = con.prepareStatement("SELECT * FROM OXQuiz WHERE Id = ?");
                ps.setInt(1, quizid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    final String Question = rs.getString("Question");
                    final String Explaination = rs.getString("Explaination");
                    if (Question.endsWith("= ") || Question.endsWith("= -")) {
                        continue;
                    }
                    final boolean isX = rs.getString("Result").equals("O") ? false : true;
                    OXQuiz quiz = new OXQuiz(Question, Explaination, isX);
                    list[a] = quiz;
                }
                ps.close();
                rs.close();
            }
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public static List<OXQuiz> getQuizList2(final int amount) {
        List<OXQuiz> quizes = new ArrayList<OXQuiz>();

        for (OXQuiz q : getQuizList(amount)) {
            quizes.add(q);
        }
        return quizes;
    }
}
