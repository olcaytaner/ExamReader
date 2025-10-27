package Exam;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        /* String directory ="C:\\Users\\nermi\\Dropbox\\Midterm1-Annotated";
        //Exam exam = new Exam(directory,"Midterm1");
        Exam a= new Exam("C:\\Users\\Mujgan\\Dropbox\\Final-Annotated (1)", "Summer2024-Final");
        a.exportGradingPNGsColored("C:\\Users\\Mujgan\\Desktop\\test","F");
       Assessment test= new Assessment(2,"",false,"","    if (left->getData() != right->getData()) {\n" +
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
        StudentCode s = new StudentCode(false,"s033247", 1, "C:\\Users\\nermi\\Dropbox\\Midterm1-Annotated (1)\\Q1\\S024527.txt");
        Assessment a = new Assessment(4,"", false, ""," while(p1 !=null && p2 !=null){\n" +
                "            if(p1.getData()< p2.getData()){\n" +
                "                p1=p1.getNext();\n" +
                "                System.out.println(\"ileri p1\");\n" +
                "            }else if(p1.getData() > p2.getData()){\n" +
                "                p2=p2.getNext();\n" +
                "                System.out.println();\n" +
                "            }else{\n" +
                "                Node newNode=new Node(p1.getData());\n" +
                "                p1=p1.getNext();\n" +
                "                p2=p2.getNext();\n" +
                "                if(result.head==null){\n" +
                "                    result.head=newNode;\n" +
                "                    result.tail=newNode;\n" +
                "                }else{\n" +
                "                    result.tail.setNext(newNode);\n" +
                "                    result.tail=newNode;\n" +
                "                }\n" +
                "            }\n" +
                "        }", s);


        System.out.println(a.getAllVariables());



    }
}
