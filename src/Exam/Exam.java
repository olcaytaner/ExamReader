package Exam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;


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
            String fileDirectory= file.getAbsolutePath();
            if (name.startsWith("Q")) {
                String questionNo = name.substring(1);
                questions.add(new Question(questionNo, fileDirectory));
                questionNum++;
            }
        }
        System.out.println(questionNum + " question object created");
    }


    public String getStudentCode (String questionNo, String studentNo) throws IOException {
        String code = "";
        for ( Question question: this.questions){
            if(question.getQuestionNo().equals(questionNo)){
                Question currentQ = question;
                for (StudentCode student: currentQ.getStudents()){
                    if(student.getStudentNo().equals(studentNo)){
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
                             Map<String,String> nodeLabelMap) throws IOException, InterruptedException {

        // DOT dosyasını oluştur
        g.saveGraphviz(tmpDir.toString(), baseName, baseName, nodeLabelMap);

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

        System.out.println("DOT dosyası yazıldı: " + dotFile);
        System.out.println("PNG dosyası yazıldı: " + destDir.resolve(baseName + ".png"));
    }


    private static void deleteDirectoryRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
    }

    private static String sanitize(String name) {
        // Windows için yasaklı karakterleri güvenli hale getir
        return name.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
    }
    public StudentCode getStudentObject(String questionNo, String studentNo) throws IOException {
        StudentCode studentR = null;
        for ( Question question: this.questions){
            if(question.getQuestionNo().equals(questionNo)){
                Question currentQ = question;
                for (StudentCode student: currentQ.getStudents()){
                    if(student.getStudentNo().equals(studentNo)){
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
                    if (a.isAstFailed() ) {
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
}


