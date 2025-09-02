package Exam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.regex.*;

public class RefCode extends Code {

    public RefCode(int refCodeNo, String path) throws IOException {
        super(refCodeNo, path);
        loadAssessmentFromFile(path);
    }

    private void loadAssessmentFromFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);

        if (lines.isEmpty()) return;

        String firstLine = lines.get(0).trim().toLowerCase();

        Matcher m = Pattern.compile("refcode\\s*(\\d+)").matcher(firstLine);
        if (m.find()) {
            this.refCodeNo = Integer.parseInt(m.group(1));
        }

        parseFile(lines);
    }

}
