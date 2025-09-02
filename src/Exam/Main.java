package Exam;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String directory ="C:\\Users\\nermi\\Dropbox\\Midterm1-Annotated";
        try {
            //Exam exam = new Exam(directory,"Midterm1");
            Exam a= new Exam("C:\\Users\\Mujgan\\Dropbox\\Midterm3-Annotated", "Fall2023-M3");
            a.exportGradingPNGs("C:\\Users\\Mujgan\\Desktop\\Grading1","M");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
