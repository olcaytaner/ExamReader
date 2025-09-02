package Exam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.regex.*;

public class StudentCode extends Code {
    private boolean skip;
    private String studentNo;

    public StudentCode( boolean skip, String studentNo, int refCodeNo, String path) throws IOException {
        super(refCodeNo, path);
        this.studentNo = studentNo;
        this.skip = skip;
        loadAssessments(path);
    }

    private void loadAssessments(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);

        if (lines.isEmpty()) return;

        String firstLine = lines.get(0).trim().toLowerCase();

        if (firstLine.contains("skip")) {
            this.skip = true;
            return;
        }

        // RefCode no varsa çek
        Matcher m = Pattern.compile("refcode\\s*(\\d+)").matcher(firstLine);
        if (m.find()) {
            this.refCodeNo = Integer.parseInt(m.group(1));
        }

        parseFile(lines);
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public boolean isSkip() {
        return skip;
    }

}
