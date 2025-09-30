import Exam.*;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String directory = "C:\\Users\\Mujgan\\Dropbox\\Midterm1-Annotated (3)";

        try {
            Exam m = new Exam(directory, "Summer2025-Midterm1");
            m.exportGradingPNGs("C:\\Users\\Mujgan\\Desktop\\Grading1","M");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
