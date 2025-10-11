import Exam.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String studentCode = """
                int x = 5;
                int y = 10;
                if (x < y) {
                    x = y;
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
                    t = a;
                }
                System.out.println(a);
                """;

        Assessment student = new Assessment(100, "ok", false, "", studentCode);
        Assessment reference = new Assessment(100, "ok", false, "", referenceCode);

        // Eşleşme bul
        Assessment.MatchResult result = student.calculateBestMatch(reference);

        // 🔥 Renkli Graphviz dosyaları oluştur
        student.toGraphvizWithHighlights("C:\\Users\\Mujgan\\Desktop\\test", result.matchedLines);
    }
}
