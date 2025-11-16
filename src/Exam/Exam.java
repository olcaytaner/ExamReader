package Exam;

import Graph.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;




public class Exam {
    private ArrayList<Question> questions;
    private String examName;
    private String directory;

    public Exam(String directory, String examName) throws IOException {
        this.examName = examName;
        this.directory = directory;
        this.questions = new ArrayList<Question>();
        loadQuestionsFromFolder(directory);
    }

    private void loadQuestionsFromFolder(String rootPath) throws IOException {
        int questionNum = 0; //only to determine the total created question object
        File root = new File(rootPath);
        File[] filesAndFolders = root.listFiles();

        if (filesAndFolders == null) return;

        for (File file : filesAndFolders) {
            String name = file.getName();
            String fileDirectory = file.getAbsolutePath();
            if (name.startsWith("Q")) {
                String questionNo = name.substring(1);
                questions.add(new Question(questionNo, fileDirectory));
                questionNum++;
            }
        }
        System.out.println(questionNum + " question object created");
    }


    public String getStudentCode(String questionNo, String studentNo) throws IOException {
        String code = "";
        for (Question question : this.questions) {
            if (question.getQuestionNo().equals(questionNo)) {
                Question currentQ = question;
                for (StudentCode student : currentQ.getStudents()) {
                    if (student.getStudentNo().equals(studentNo)) {
                        code = Files.readString(Path.of(student.getPath()));
                    }
                }
            }
        }
        return code;
    }


    public String getExamName() {
        return examName;
    }

    public String getDirectory() {
        return directory;
    }

    public void exportGradingPNGs(String gradingRootDir, String examTypeLetter) throws IOException {

        Path gradingRoot = Paths.get(gradingRootDir);
        Files.createDirectories(gradingRoot);

        String typeSuffix = (examTypeLetter != null && !examTypeLetter.isBlank()) ? examTypeLetter : "";

        for (Question q : this.questions) {
            // --------- HER REF CODE İÇİN ---------
            for (RefCode ref : q.getRefcodes()) {
                ArrayList<Assessment> refAssess = ref.getAssessments();
                int refId = ref.getRefCodeNo();

                for (int i = 0; i < refAssess.size(); i++) {
                    Assessment a = refAssess.get(i);
                    int refScore = a.getGrade();

                    String caseFolderName = sanitize(
                            String.format("%s, Question %s, RefCode%d, A%d%s",
                                    this.examName, q.getQuestionNo(), refId, i + 1, typeSuffix)
                    );

                    Path caseDir = gradingRoot
                            .resolve(String.valueOf(refScore))
                            .resolve(caseFolderName);

                    Files.createDirectories(caseDir);


                    renderAssessmentPNGsTo(caseDir, a, "ref" + refId);

                    // --------- O REF CODE’A AİT ÖĞRENCİLER ---------
                    for (StudentCode st : q.getStudents()) {
                        if (st.isSkip()) continue;

                        // Öğrencinin refCodeNo’su aynı değilse geç
                        if (st.getRefCodeNo() != refId) continue;

                        // Öğrencinin aynı indexte assessment'ı varsa
                        if (i < st.getAssessments().size()) {
                            Assessment sa = st.getAssessments().get(i);
                            int stuScore = sa.getGrade();

                            Path stuBucket = caseDir.resolve(String.valueOf(stuScore));
                            Files.createDirectories(stuBucket);

                            renderAssessmentPNGsTo(stuBucket, sa, st.getStudentNo());
                        }
                    }
                }
            }
        }
    }


// --- yardımcılar ---

    private void renderAssessmentPNGsTo(Path destDir, Assessment a, String prefix) throws IOException {
        Path tmpDir = Files.createTempDirectory("graphviz_tmp_");
        try {
            if (a.getAbstractSyntaxTree() != null) {
                saveOnlyPng(a.getAbstractSyntaxTree(), tmpDir, destDir, prefix + "-ast", a.getAstNodeLabels());
            }
            if (a.getControlFlowGraph() != null) {
                saveOnlyPng(a.getControlFlowGraph(), tmpDir, destDir, prefix + "-cfg", a.getCfgNodeLabels());
            }
            if (a.getDataDependencyGraph() != null) {
                saveOnlyPng(a.getDataDependencyGraph(), tmpDir, destDir, prefix + "-ddg", a.getDdgNodeLabels());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Graphviz işlemi kesildi", e);
        } finally {
            deleteDirectoryRecursive(tmpDir);
        }
    }

    private void saveOnlyPng(Graph.Graph g,
                             Path tmpDir,
                             Path destDir,
                             String baseName,
                             Map<String, String> nodeLabelMap) throws IOException, InterruptedException {

        // ref code mu? baseName "ref..." ile başlıyorsa
        boolean isRef = baseName.startsWith("ref");

        // dot dosyası oluşturulurken prefixe bakılarak hangi graphviz methodunun oluşturulacağına karar verilir.
        if (isRef) {
            // RefCode için-> her node farklı renkte olan yeni method
            g.saveGraphvizUniqueColors(tmpDir.toString(), baseName, baseName, nodeLabelMap);
        } else {
            // Öğrenci için-> mevcut davranış eski method(güncellenecek)
            g.saveGraphviz(tmpDir.toString(), baseName, baseName, nodeLabelMap);
        }

        Path dotFile = tmpDir.resolve(baseName + ".dot");
        Path pngFile = tmpDir.resolve(baseName + ".png");

        if (!Files.exists(dotFile)) {
            throw new IOException("DOT file not created: " + dotFile);
        }

        // Graphviz dot komutu
        String dotExe = "dot"; // Windows'ta PATH'te olmalı
        ProcessBuilder pb = new ProcessBuilder(dotExe, "-Tpng",
                dotFile.toAbsolutePath().toString(),
                "-o", pngFile.toAbsolutePath().toString());

        pb.redirectErrorStream(true);
        Process p = pb.start();

        // hata mesajlarını oku
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("[Graphviz] " + line);
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("Graphviz dot komutu başarısız oldu: exit=" + exitCode);
        }

        if (!Files.exists(pngFile)) {
            throw new IOException("PNG file not created: " + pngFile);
        }

        Files.createDirectories(destDir);
        Files.move(pngFile, destDir.resolve(baseName + ".png"), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("PNG yazıldı: " + destDir.resolve(baseName + ".png"));
    }


    private static void deleteDirectoryRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
    }

    private static String sanitize(String name) {
        // Windows için yasaklı karakterleri güvenli hale getir
        return name.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    }

    public StudentCode getStudentObject(String questionNo, String studentNo) throws IOException {
        StudentCode studentR = null;
        for (Question question : this.questions) {
            if (question.getQuestionNo().equals(questionNo)) {
                Question currentQ = question;
                for (StudentCode student : currentQ.getStudents()) {
                    if (student.getStudentNo().equals(studentNo)) {
                        studentR = student;
                    }
                }
            }
        }
        return studentR;
    }

    // Exam.java içine ekle

    // Yalnız AST
    public void checkAllStudentsAST() {
        for (Question question : questions) {
            String qNo = question.getQuestionNo();
            for (StudentCode student : question.getStudents()) {
                try {
                    boolean anyFail = false;
                    for (Assessment a : student.getAssessments()) {
                        if (a.isAstFailed()) {
                            System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> AST not done");
                            anyFail = true;
                        }
                    }
                    // İstersen hiç assessment yoksa da uyar:
                    // if (!anyFail && student.getAssessments().isEmpty()) {
                    //     System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> AST not done (no assessments)");
                    // }
                } catch (Exception e) {
                    System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> AST not done");
                }
            }
        }
    }

    // Yalnız CFG
    public void checkAllStudentsCFG() {
        for (Question question : questions) {
            String qNo = question.getQuestionNo();
            for (StudentCode student : question.getStudents()) {
                try {
                    for (Assessment a : student.getAssessments()) {
                        if (a.isCfgFailed() || a.getControlFlowGraph() == null) {
                            System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> CFG not done");
                            break; // bir tanesi bile fail ise yazıp geç
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> CFG not done");
                }
            }
        }
    }

    // Yalnız DDG
    public void checkAllStudentsDDG() {
        for (Question question : questions) {
            String qNo = question.getQuestionNo();
            for (StudentCode student : question.getStudents()) {
                try {
                    for (Assessment a : student.getAssessments()) {
                        if (a.isDdgFailed() || a.getDataDependencyGraph() == null) {
                            System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> DDG not done");
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> DDG not done");
                }
            }
        }
    }

    // Hepsi bir arada (AST/CFG/DDG)
    public void checkAllStudentsGraphs() {
        for (Question question : questions) {
            String qNo = question.getQuestionNo();
            for (StudentCode student : question.getStudents()) {
                for (Assessment a : student.getAssessments()) {
                    if (a.isAstFailed()) {
                        System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> AST not done");
                    }
                    if ((a.isCfgFailed() || a.getControlFlowGraph() == null)) {
                        System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> CFG not done");
                    }
                    if ((a.isDdgFailed() || a.getDataDependencyGraph() == null)) {
                        System.out.println("Q" + qNo + " - " + student.getStudentNo() + " -> DDG not done");
                    }

                }
            }
        }
    }

    // -------------------------------------------------------------------

    public void exportGradingPNGsColored(String gradingRootDir, String examTypeLetter) throws IOException {
        Path gradingRoot = Paths.get(gradingRootDir);
        Files.createDirectories(gradingRoot);

        String typeSuffix = (examTypeLetter != null && !examTypeLetter.isBlank()) ? examTypeLetter : "";

        for (Question q : this.questions) {
            for (RefCode ref : q.getRefcodes()) {
                ArrayList<Assessment> refAssess = ref.getAssessments();
                int refId = ref.getRefCodeNo();

                for (int i = 0; i < refAssess.size(); i++) {
                    Assessment refA = refAssess.get(i);
                    int refScore = refA.getGrade();

                    String caseFolderName = sanitize(
                            String.format("%s, Question %s, RefCode%d, A%d%s",
                                    this.examName, q.getQuestionNo(), refId, i + 1, typeSuffix)
                    );

                    Path caseDir = gradingRoot
                            .resolve(String.valueOf(refScore))
                            .resolve(caseFolderName);

                    Files.createDirectories(caseDir);

                    // 1) Referans PNG'leri (RENKSİZ) – aynı klasör yapısı
                    renderAssessmentPNGsTo(caseDir, refA, "ref" + refId);

                    // 2) Öğrenciler (RENKLİ) – renksiz metotla AYNI klasörleme:
                    //    <caseDir>/<stuScore>/<studentNo>-*-colored.png
                    for (StudentCode st : q.getStudents()) {
                        if (st.isSkip()) continue;
                        if (st.getRefCodeNo() != refId) continue;

                        ArrayList<Assessment> stuAssess = st.getAssessments();
                        if (i < stuAssess.size()) {
                            Assessment stuA = stuAssess.get(i);
                            if (stuA != null) {
                                int stuScore = stuA.getGrade();

                                Path stuBucket = caseDir.resolve(String.valueOf(stuScore));
                                Files.createDirectories(stuBucket);

                                // NOT: Artık öğrenci numarasına göre alt klasör yok;
                                // dosya adında öğrenci no kullanılıyor (renksiz metotla aynı yaklaşım).
                                renderAssessmentPNGsColoredTo(stuBucket, stuA, refA, st.getStudentNo());
                            }
                        }
                    }
                }
            }
        }
    }
    private void renderAssessmentPNGsColoredTo(java.nio.file.Path destDir,
                                               Assessment studentA,
                                               Assessment referenceA,
                                               String prefix) throws IOException {
        java.nio.file.Files.createDirectories(destDir);
        java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("graphviz_tmp_colored_");
        try {
            // Öğrenci–ref kıyası: eşleşen satırlar
            Assessment.MatchResult match = studentA.calculateBestMatch(referenceA);

            if (studentA.getAbstractSyntaxTree() != null) {
                saveColoredPng(
                        studentA.getAbstractSyntaxTree(),
                        tmpDir,
                        destDir,
                        prefix + "-ast-colored",
                        studentA.getAstNodeLabels(),
                        match.matchedLines
                );
            }
            if (studentA.getControlFlowGraph() != null) {
                saveColoredPng(
                        studentA.getControlFlowGraph(),
                        tmpDir,
                        destDir,
                        prefix + "-cfg-colored",
                        studentA.getCfgNodeLabels(),
                        match.matchedLines
                );
            }
            if (studentA.getDataDependencyGraph() != null) {
                saveColoredPng(
                        studentA.getDataDependencyGraph(),
                        tmpDir,
                        destDir,
                        prefix + "-ddg-colored",
                        studentA.getDdgNodeLabels(),
                        match.matchedLines
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Graphviz işlemi kesildi", e);
        } finally {
            deleteDirectoryRecursive(tmpDir);
        }
    }
    private void saveColoredPng(
            Graph.Graph g,
            java.nio.file.Path tmpDir,
            java.nio.file.Path destDir,
            String baseName,
            java.util.Map<String, String> nodeLabelMap,
            java.util.List<Graph.Pair<java.lang.Integer, java.lang.Integer>> matchedLines
    ) throws java.io.IOException, InterruptedException {

        // 1) matchedLines -> sadece ÖĞRENCİ satır numaraları (yeşil boyanacaklar)
        java.util.List<java.lang.Integer> highlightLines = new java.util.ArrayList<>();
        if (matchedLines != null) {
            for (Graph.Pair<java.lang.Integer, java.lang.Integer> p : matchedLines) {
                if (p != null && p.getKey() != null) {
                    highlightLines.add(p.getKey());
                }
            }
        }

        // 2) DOT’u renkli yaz
        java.nio.file.Files.createDirectories(tmpDir);
        g.saveGraphviz(tmpDir.toString(), baseName, baseName, nodeLabelMap, highlightLines);

        java.nio.file.Path dotFile = tmpDir.resolve(baseName + ".dot");
        java.nio.file.Path pngFile = tmpDir.resolve(baseName + ".png");
        if (!java.nio.file.Files.exists(dotFile)) {
            throw new java.io.IOException("DOT file not created: " + dotFile);
        }

        // 3) dot -> png
        String dotExe = System.getProperty("graphviz.dot", "dot");
        ProcessBuilder pb = new ProcessBuilder(
                dotExe, "-Tpng",
                dotFile.toAbsolutePath().toString(),
                "-o", pngFile.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (java.io.BufferedReader r =
                     new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
            while (r.readLine() != null) { /* çıktıyı tüket */ }
        }
        int exit = p.waitFor();
        if (exit != 0) throw new java.io.IOException("Graphviz dot failed, exit=" + exit);

        // 4) hedefe taşı
        java.nio.file.Files.createDirectories(destDir);
        java.nio.file.Files.move(pngFile, destDir.resolve(baseName + ".png"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        System.out.println("PNG yazıldı: " + destDir.resolve(baseName + ".png"));
    }

    public void exportGradingPNGsWithRefColors(String gradingRootDir, String examTypeLetter) throws IOException {
        Path gradingRoot = Paths.get(gradingRootDir);
        Files.createDirectories(gradingRoot);

        String typeSuffix = (examTypeLetter != null && !examTypeLetter.isBlank()) ? examTypeLetter : "";

        for (Question q : this.questions) {
            for (RefCode ref : q.getRefcodes()) {
                ArrayList<Assessment> refAssess = ref.getAssessments();
                int refId = ref.getRefCodeNo();

                for (int i = 0; i < refAssess.size(); i++) {
                    Assessment refA = refAssess.get(i);
                    int refScore = refA.getGrade();

                    String caseFolderName = sanitize(
                            String.format("%s, Question %s, RefCode%d, A%d%s",
                                    this.examName, q.getQuestionNo(), refId, i + 1, typeSuffix)
                    );

                    Path caseDir = gradingRoot
                            .resolve(String.valueOf(refScore))
                            .resolve(caseFolderName);

                    Files.createDirectories(caseDir);
                    Map<Integer, String> refLineColors = generateRefCodeLineColors(refA);
                    renderRefCodeWithColors(caseDir, refA, refLineColors, "ref" + refId);
                    for (StudentCode st : q.getStudents()) {
                        if (st.isSkip()) continue;
                        if (st.getRefCodeNo() != refId) continue;

                        ArrayList<Assessment> stuAssess = st.getAssessments();
                        if (i < stuAssess.size()) {
                            Assessment stuA = stuAssess.get(i);
                            if (stuA != null) {
                                int stuScore = stuA.getGrade();

                                Path stuBucket = caseDir.resolve(String.valueOf(stuScore));
                                Files.createDirectories(stuBucket);

                                // RefCode renkleriyle öğrenci PNG'sini oluştur
                                renderAssessmentPNGsWithRefColorsTo(
                                        stuBucket,
                                        stuA,
                                        refA,
                                        refLineColors,
                                        st.getStudentNo()
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderRefCodeWithColors(
            Path destDir,
            Assessment refA,
            Map<Integer, String> refLineColors,
            String prefix) throws IOException {

        Files.createDirectories(destDir);
        Path tmpDir = Files.createTempDirectory("graphviz_tmp_ref_");

        try {
            // AST
            if (refA.getAbstractSyntaxTree() != null) {
                saveColoredPngWithMapping(
                        refA.getAbstractSyntaxTree(),
                        tmpDir,
                        destDir,
                        prefix + "-ast",
                        refA.getAstNodeLabels(),
                        refLineColors
                );
            }

            // CFG
            if (refA.getControlFlowGraph() != null) {
                saveColoredPngWithMapping(
                        refA.getControlFlowGraph(),
                        tmpDir,
                        destDir,
                        prefix + "-cfg",
                        refA.getCfgNodeLabels(),
                        refLineColors
                );
            }

            // DDG
            if (refA.getDataDependencyGraph() != null) {
                saveColoredPngWithMapping(
                        refA.getDataDependencyGraph(),
                        tmpDir,
                        destDir,
                        prefix + "-ddg",
                        refA.getDdgNodeLabels(),
                        refLineColors
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Graphviz işlemi kesildi", e);
        } finally {
            deleteDirectoryRecursive(tmpDir);
        }
    }

    private Map<Integer, String> generateRefCodeLineColors(Assessment refAssessment) {
        Map<Integer, String> lineColors = new HashMap<>();

        String[] lines = refAssessment.getCodeBlock().split("\n");
        List<Integer> validLineNumbers = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // Boş satırları ve yorumları atla
            if (!line.isEmpty() &&
                    !line.startsWith("//") &&
                    !line.startsWith("/*") &&
                    !line.startsWith("*") &&
                    !line.equals("{") &&
                    !line.equals("}")) {
                validLineNumbers.add(i + 1);
            }
        }


        int totalValidLines = validLineNumbers.size();
        for (int idx = 0; idx < totalValidLines; idx++) {
            int lineNo = validLineNumbers.get(idx);

            // Hue: 0-360 arası eşit dağılım
            double hue = (360.0 * idx) / totalValidLines;
            double saturation = 0.70;
            double lightness = 0.55;

            String hexColor = hslToHex(hue, saturation, lightness);
            lineColors.put(lineNo, hexColor);
        }

        return lineColors;
    }


    private String hslToHex(double hDeg, double s, double l) {
        double h = (hDeg % 360.0) / 360.0;

        double r, g, b;

        if (s == 0) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : (l + s - l * s);
            double p = 2 * l - q;
            r = hue2rgb(p, q, h + 1.0 / 3.0);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1.0 / 3.0);
        }

        int ri = (int) Math.round(r * 255);
        int gi = (int) Math.round(g * 255);
        int bi = (int) Math.round(b * 255);

        return String.format("#%02X%02X%02X", ri, gi, bi);
    }

    private double hue2rgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }


    private void renderAssessmentPNGsWithRefColorsTo(
            Path destDir,
            Assessment studentA,
            Assessment referenceA,
            Map<Integer, String> refLineColors,
            String prefix) throws IOException {

        Files.createDirectories(destDir);
        Path tmpDir = Files.createTempDirectory("graphviz_tmp_refcolors_");

        try {
            Assessment.MatchResult match = studentA.calculateBestMatch(referenceA);
            Map<Integer, String> studentLineColors = mapRefColorsToStudent(
                    match.matchedLines,
                    refLineColors
            );

            // AST
            if (studentA.getAbstractSyntaxTree() != null) {
                saveColoredPngWithMapping(
                        studentA.getAbstractSyntaxTree(),
                        tmpDir,
                        destDir,
                        prefix + "-ast-refcolored",
                        studentA.getAstNodeLabels(),
                        studentLineColors
                );
            }

            // CFG
            if (studentA.getControlFlowGraph() != null) {
                saveColoredPngWithMapping(
                        studentA.getControlFlowGraph(),
                        tmpDir,
                        destDir,
                        prefix + "-cfg-refcolored",
                        studentA.getCfgNodeLabels(),
                        studentLineColors
                );
            }

            // DDG
            if (studentA.getDataDependencyGraph() != null) {
                saveColoredPngWithMapping(
                        studentA.getDataDependencyGraph(),
                        tmpDir,
                        destDir,
                        prefix + "-ddg-refcolored",
                        studentA.getDdgNodeLabels(),
                        studentLineColors
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Graphviz işlemi kesildi", e);
        } finally {
            deleteDirectoryRecursive(tmpDir);
        }
    }

    /**
     * RefCode renklerini öğrenci satırlarına map'ler
     * SADECE eşleşen satırlar renk alır, eşleşmeyenler beyaz kalır
     * @param matchedLines List<Pair<ÖğrenciSatırNo, RefCodeSatırNo>>
     * @param refLineColors Map<RefCodeSatırNo, HexRenk>
     * @return Map<ÖğrenciSatırNo, HexRenk> - SADECE eşleşen satırlar için
     */
    private Map<Integer, String> mapRefColorsToStudent(
            List<Graph.Pair<Integer, Integer>> matchedLines,
            Map<Integer, String> refLineColors) {

        Map<Integer, String> studentColors = new HashMap<>();

        if (matchedLines == null || matchedLines.isEmpty()) {
            return studentColors; // Boş map - hiçbir satır renkli olmayacak
        }

        for (Graph.Pair<Integer, Integer> match : matchedLines) {
            if (match == null) continue;

            Integer studentLine = match.getKey();    // Öğrenci satır no
            Integer refLine = match.getValue();       // RefCode satır no

            if (studentLine != null && refLine != null) {
                String refColor = refLineColors.get(refLine);
                if (refColor != null) {
                    // Sadece eşleşen öğrenci satırlarına RefCode rengini ver
                    studentColors.put(studentLine, refColor);

                    System.out.println(String.format(
                            "Eşleştirme: Öğrenci satır %d -> RefCode satır %d (renk: %s)",
                            studentLine, refLine, refColor
                    ));
                }
            }
        }

        System.out.println("Toplam eşleşen satır sayısı: " + studentColors.size());
        return studentColors;
    }


    private void saveColoredPngWithMapping(
            Graph.Graph g,
            Path tmpDir,
            Path destDir,
            String baseName,
            Map<String, String> nodeLabelMap,
            Map<Integer, String> lineColorMap) throws IOException, InterruptedException {

        Files.createDirectories(tmpDir);

        // DOT dosyasını özel renk mapping ile oluştur
        g.saveGraphvizWithColorMapping(
                tmpDir.toString(),
                baseName,
                baseName,
                nodeLabelMap,
                lineColorMap
        );

        Path dotFile = tmpDir.resolve(baseName + ".dot");
        Path pngFile = tmpDir.resolve(baseName + ".png");

        if (!Files.exists(dotFile)) {
            throw new IOException("DOT file not created: " + dotFile);
        }

        // Graphviz dot komutu
        String dotExe = System.getProperty("graphviz.dot", "dot");
        ProcessBuilder pb = new ProcessBuilder(
                dotExe, "-Tpng",
                dotFile.toAbsolutePath().toString(),
                "-o", pngFile.toAbsolutePath().toString()
        );

        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.err.println("[Graphviz] " + line);
            }
        }

        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Graphviz dot failed, exit=" + exit);
        }

        if (!Files.exists(pngFile)) {
            throw new IOException("PNG file not created: " + pngFile);
        }

        Files.createDirectories(destDir);
        Files.move(pngFile, destDir.resolve(baseName + ".png"),
                StandardCopyOption.REPLACE_EXISTING);

        System.out.println("PNG yazıldı: " + destDir.resolve(baseName + ".png"));
    }}