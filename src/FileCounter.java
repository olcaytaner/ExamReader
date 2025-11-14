import java.io.File;

public class FileCounter {

    public static int countFilesAndFolders(File folder, int depth) {
        if (!folder.exists()) {
            System.out.println("Yol bulunamadı!");
            return 0;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }

        int fileCount = 0;
        int folderCount = 0;
        int totalFileCount = 0;

        for (File file : files) {
            if (file.isDirectory()) {
                folderCount++;
            } else {
                fileCount++;
            }
        }

        totalFileCount += fileCount;

        String indent = "  ".repeat(depth);
        System.out.println(indent + "📂 " + folder.getName() + " → Klasör: " + folderCount + " | Dosya: " + fileCount);

        for (File file : files) {
            if (file.isDirectory()) {
                totalFileCount += countFilesAndFolders(file, depth + 1);
            }
        }


        return totalFileCount;
    }
}
