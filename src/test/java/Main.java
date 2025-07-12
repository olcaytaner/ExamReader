import Exam.Exam;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String directory ="C:\\Users\\nermi\\Dropbox\\Midterm1-Annotated";
        try {
            Exam exam = new Exam(directory,"Midterm1");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
