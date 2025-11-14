import Exam.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        //------- FILE COUNTER - CHECK FILE COUNT -------------------//

        File mainFolder = new File("C:\\Users\\nermi\\Desktop\\Dropbox.lnk");

        int totalFileCount = FileCounter.countFilesAndFolders(mainFolder, 0);

        System.out.println("\n📊 Bu klasördeki toplam dosya sayısı: " + totalFileCount);
        if(totalFileCount!= 6084 ){
            System.out.println("Total file number changed to " + totalFileCount + " from 6084 ");
        }

        /*
        String studentCode = """
                int x = 5;
                int y = 10;
                if (x < y) {
                    x = this.y;
                } else {
                    y = x;
                }
                System.out.println(x);
                """;

        String referenceCode = """
                int a = 5;
                int b = 10;
                if (a < b) {
                    a = b;
                } else {
                    t = a.getData();
                }
                System.out.println(a);
                """;

        Assessment student = new Assessment(100, "ok", false, "", studentCode);
        Assessment reference = new Assessment(100, "ok", false, "", referenceCode);

        Assessment.MatchResult result = student.calculateBestMatch(reference);

        student.toGraphvizWithHighlights("C:\\Users\\Mujgan\\Desktop\\test", result.matchedLines);


        Exam test=new Exam("C:\\Users\\Mujgan\\Dropbox\\Final-Annotated (1)","Summer2024-Final");
        test.exportGradingPNGsColored("C:\\Users\\Mujgan\\Dropbox\\Grading","F");
  */
    }
}
