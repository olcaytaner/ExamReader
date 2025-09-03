import Exam.*;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String directory ="C:\\Users\\Mujgan\\Dropbox\\Midterm1-Annotated (2)";
        try {
            Exam exam = new Exam(directory,"Spring25-Midterm1");
            //exam.exportGradingPNGs("C:\\Users\\Mujgan\\Desktop\\Grading1","M");
            StudentCode a=exam.getStudentObject("2","s023287");
            System.out.println(a.getAssessments().get(1).getControlFlowGraph().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
