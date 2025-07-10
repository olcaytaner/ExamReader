package Exam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Question {
    private  ArrayList<StudentCode> students;
    private  ArrayList<RefCode> refcodes;
    private final String questionNo;
    private String directory;

    public Question(String questionNo , String directory) throws IOException {
        this.students = new ArrayList<StudentCode>();
        this.refcodes = new ArrayList<RefCode>();
        this.questionNo=questionNo;
        this.directory=directory;
        try {
            loadStudents(directory);
        } catch (IOException e) {
            System.err.println("Error while creating student: " + e.getMessage());
        }
        loadRefCodes(directory);
    }


    /**
     * txt dosyaları - student code varsayılsın.
     * cpp. ve java. - ref code varsayılsın.
     * @param directory
     * @throws IOException
     */
    private void loadStudents(String directory) throws IOException {
        // to count how many student obj. created
        AtomicInteger studentNum = new AtomicInteger();

        // o directorydeki tüm file'ları gez.
        Files.walk(Paths.get(directory))
                .forEach(path -> {
                    try {
                        //file değilse geç.
                        if (!Files.isRegularFile(path)) return;

                        // dosya adını al.
                        String fileName = path.getFileName().toString().toLowerCase();

                        String studentNo = fileName.substring(0, 7);

                        if (fileName.endsWith(".txt")) {
                            StudentCode student = new StudentCode(false,studentNo, 1, path.toString());
                            students.add(student);
                            studentNum.getAndIncrement();
                        }

                    } catch (IOException ex) {
                        System.err.println("Student object couldn't be created: " + path + " -> " + ex.getMessage());
                    }
                });

        System.out.println(studentNum.get() + " new student object created.");
    }



    private void loadRefCodes(String directory) throws IOException {
        AtomicInteger refCodeNum = new AtomicInteger();
        Files.walk(Paths.get(directory))
                .forEach(path -> {
                    try {
                        if (!Files.isRegularFile(path)) return;

                        String fileName = path.getFileName().toString().toLowerCase();

                        if (fileName.endsWith(".java") || fileName.endsWith(".cpp")) {
                            RefCode refCode = new RefCode(1, path.toString());
                            refcodes.add(refCode);
                            refCodeNum.getAndIncrement();
                        }

                    } catch (IOException e) {
                        System.err.println("RefCode object couldn't be created: " + path + " -> " + e.getMessage());
                    }
                });

        System.out.println(refCodeNum.get() + " new refCode object created.");
    }




    public ArrayList<StudentCode> getStudents() {
        return students;
    }

    public String getQuestionNo() {
        return questionNo;
    }

}