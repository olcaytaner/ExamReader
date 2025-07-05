package org.example.Exam;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Exam {
    private ArrayList<Question> questions;
    private String examName;
    private String directory;

    public Exam(String directory, String examName) {
        this.examName = examName;
        this.directory = directory;
        this.questions = new ArrayList<Question>();
        loadQuestionsFromFolder(directory);
    }

    private void loadQuestionsFromFolder(String rootPath) {
        int questionNum = 0; //only to determine the total created question object
        File root = new File(rootPath);
        File[] filesAndFolders = root.listFiles();

        if (filesAndFolders == null) return;

        for (File file : filesAndFolders) {
            String name = file.getName();
            String fileDirectory= file.getAbsolutePath();
            if (name.startsWith("Q")) {
                try {
                    String numberPart = name.substring(1);
                    int questionNo = Integer.parseInt(numberPart);
                    questions.add(new Question(String.valueOf(questionNo), fileDirectory));
                    questionNum++;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid file or folder name : " + name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println(questionNum + " question object created");
    }


    public String getExamName() {
        return examName;
    }

    public String getDirectory() {
        return directory;
    }
}


