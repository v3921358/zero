package server.games;

/**
 * @author SLFCG
 */
public class OXQuiz {

    private String Question, Explaination;
    private boolean isX;

    public OXQuiz(String a1, String a2, boolean a3) {
        Question = a1;
        Explaination = a2;
        isX = a3;
    }

    public String getQuestion() {
        return this.Question;
    }

    public String getExplaination() {
        return this.Explaination;
    }

    public boolean isX() {
        return this.isX;
    }
}
