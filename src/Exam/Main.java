package Exam;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        /*
        Exam exam = new Exam("C:\\Users\\nermi\\Desktop\\hehe","Midterm1");
        exam.exportGradingPNGsColored("C:\\Users\\nermi\\Desktop\\hehe.pngs","idc");

        Exam exam1 = new Exam("C:\\Users\\Mujgan\\Dropbox\\Midterm1-Annotated (1)", "ExamName");
        exam1.exportGradingPNGsWithRefColors("C:\\Users\\Mujgan\\Desktop\\test2", "A");
         */
        String testCode = "int x = 5;\n" +
                "            if (x > 0) {\n" +
                "                x = x + 1;\n" +
                "            }";

        Assessment assessment = new Assessment(100, "Good", false, "", testCode);

        // AST oluştu mu?
        System.out.println("AST generated: " + (assessment.getAbstractSyntaxTree() != null));
        System.out.println("AST failed: " + assessment.isAstFailed());
        System.out.println("Node labels: " + assessment.getAstNodeLabels().size());
    }
}
