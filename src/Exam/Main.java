package Exam;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws IOException {
        String directory ="C:\\Users\\nermi\\Desktop\\hehe";
        Exam exam = new Exam(directory,"Midterm1");
        exam.exportGradingPNGsColored("C:\\Users\\nermi\\Desktop\\hehe.pngs","idc");

        /*
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

         */

        /*
        StudentCode ss = new StudentCode(false, "s333888", 1, "C:\\Users\\nermi\\Dropbox\\Midterm1-Annotated (4)\\Q1\\RefCode.java");
        Assessment as = new Assessment(2, "", false, "","int zeroth = 0, first = 1, second = 1, third = zeroth + second;\n" +
                "    while (third >= A && third <= B) {\n" +
                "        Node newNode = new Node(third);\n" +
                "        if (result.tail != null) {\n" +
                "           result.tail.setNext(newNode);\n" +
                "        } else {\n" +
                "            result.head = newNode;\n" +
                "        }\n" +
                "        result.tail = newNode;\n" +
                "        zeroth = first;\n" +
                "        first = second;\n" +
                "        second = third;\n" +
                "        third = zeroth + second;\n" +
                "    }", ss);


        System.out.println(as.getAllVariables());

         */

    }
}
