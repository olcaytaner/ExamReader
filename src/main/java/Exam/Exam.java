package Exam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
}


