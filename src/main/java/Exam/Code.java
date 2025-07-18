package Exam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

public class Code {
    protected ArrayList<Assessment> assessments = new ArrayList<>();
    protected int refCodeNo;
    protected String path;

    public Code(int refCodeNo, String path) throws IOException {
        this.refCodeNo = refCodeNo;
        this.path = path;
    }


    protected void parseFile(List<String> lines) {
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            if (line.startsWith("/**") && line.contains("ASSESSMENT")) {
                int grade = 0;
                String feedback = "";
                boolean violation = false;
                String violationString = "";

                while (i < lines.size() && !lines.get(i).trim().endsWith("*/")) {
                    String t = lines.get(i).trim().replaceFirst("^\\*\\s?", "");
                    if (t.startsWith("@grade")) {
                        grade = Integer.parseInt(t.substring(6).trim());
                    } else if (t.startsWith("@feedback")) {
                        feedback = t.substring(9).trim();
                    } else if (t.startsWith("@violation")) {
                        violation = true;
                        violationString = t.substring(10).trim();
                    }
                    i++;
                }
                i++; // yorum bloğu bitti, kod kısmına geç.

                StringBuilder code = new StringBuilder();
                while (i < lines.size()) {
                    String next = lines.get(i).trim();
                    if (next.startsWith("/**") && next.contains("ASSESSMENT")) break;
                    code.append(lines.get(i)).append("\n");
                    i++;
                }

                Assessment a = new Assessment(grade, feedback, violation, violationString, code.toString());
                assessments.add(a);
            } else {
                i++;
            }
        }
    }

    public int getRefCodeNo() {
        return refCodeNo;
    }

    public ArrayList<Assessment> getAssessments() {
        return assessments;
    }

    public String getPath() {
        return path;
    }
}
