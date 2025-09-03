package UI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class BucketPanel extends JPanel {

    private final DefaultListModel<File> model = new DefaultListModel<>();
    private final JList<File> list = new JList<>(model);
    private final JTextField pathField = new JTextField();

    private Path bucketDir;
    private final Runnable onBack;
    private final Consumer<Path> onOpenCase;

    public BucketPanel(Runnable onBack, Consumer<Path> onOpenCase) {
        super(new BorderLayout());
        this.onBack = onBack;
        this.onOpenCase = onOpenCase;

        JButton back = new JButton("← Geri");
        back.addActionListener(e -> onBack.run());

        pathField.setEditable(false);
        pathField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.add(back, BorderLayout.WEST);
        top.add(pathField, BorderLayout.CENTER);

        list.setCellRenderer(new FolderRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
                    onOpenCase.accept(list.getSelectedValue().toPath());
                }
            }
        });
        list.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && list.getSelectedValue() != null) {
                    onOpenCase.accept(list.getSelectedValue().toPath());
                }
            }
        });

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setBucket(Path dir) {
        this.bucketDir = dir;
        loadCases();
    }

    private void loadCases() {
        if (bucketDir == null || !Files.isDirectory(bucketDir)) {
            model.clear();
            return;
        }
        pathField.setText(bucketDir.toAbsolutePath().toString());
        model.clear();

        File[] dirs = bucketDir.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;
        Arrays.sort(dirs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File d : dirs) model.addElement(d);
        if (model.size() > 0) list.setSelectedIndex(0);
    }

    private static class FolderRenderer extends DefaultListCellRenderer {
        private final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            File f = (File) value;
            c.setIcon(folderIcon);
            c.setText(f.getName());
            c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            return c;
        }
    }
}
