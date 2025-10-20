package Exam;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        String directory ="C:\\Users\\nermi\\Dropbox\\Midterm1-Annotated";
        //Exam exam = new Exam(directory,"Midterm1");
        Exam a= new Exam("C:\\Users\\Mujgan\\Dropbox\\Final-Annotated (1)", "Summer2024-Final");
        a.exportGradingPNGsColored("C:\\Users\\Mujgan\\Desktop\\test","F");
       /* Assessment test= new Assessment(2,"",false,"","    if (left->getData() != right->getData()) {\n" +
                "        return false;\n" +
                "    } else {\n" +
                "        flag1 = isMirror(left->getLeft(), right->getRight());\n" +
                "        flag2 = isMirror(left->getRight()  , right->getLeft());\n" +
                "    }\n" +
                "\n" +
                "    if (flag1 && flag2) {\n" +
                "        return true;\n" +
                "    }\n");
        System.out.println(test.getDataDependencyGraph().toString());
        ArrayList list=Assessment.extractTokens("if (left->getData() != right->getData()) {\n");
        for (int i=0; i< list.size();i++){
            System.out.println(list.get(i));
        }


        */

    }
}
