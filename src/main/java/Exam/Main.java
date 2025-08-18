package org.example.Exam;

import org.example.Graph.Graph;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        String directory ="C:\\Users\\nermi\\Dropbox\\Final-Annotated (1)";
        //Exam exam = new Exam(directory,"-");
        //StudentCode student = exam.getStudentObject("1","s034084");
        //Assessment assessment = student.assessments.get(1);
        //System.out.println(assessment.getCodeBlock());
        //System.out.println(assessment.getAbstractSyntaxTree().toString());

        /*Assessment assessment = new Assessment(4,"",false, "", "int ali=4;\n" +
                "int veli=4;\n" +
                "x=ali+veli;");

         */

        //Graph graph =assessment.getDataDependencyGraph();
        //System.out.println(graph.toString());
        //exam.checkAllStudentsGraphs();

        ArrayList list = Assessment.extractTokensWithDots("int a = temp.next ");
        for (int i = 0; i< list.size() ; i++){
            System.out.println(list.get(i));
        }
    }
}
