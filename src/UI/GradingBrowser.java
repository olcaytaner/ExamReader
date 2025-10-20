package UI;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public class GradingBrowser extends JFrame {

    // Fixed root
    private static final Path ROOT = Path.of("C:\\Users\\Mujgan\\Desktop\\test");

    // Card layout
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // ROOT (bucket list)
    private final DefaultListModel<File> rootModel = new DefaultListModel<>();
    private final JList<File> rootList = new JList<>(rootModel);
    private final JTextField rootField = new JTextField();

    // BUCKET and CASE panels
    private final BucketPanel bucketPanel = new BucketPanel(this::showRoot, this::openCase);
    private final CasePanel casePanel = new CasePanel(this::showBucket);

    // Last selected bucket (for back navigation)
    private Path currentBucketDir;

    public GradingBrowser() {
        super("Grading Browser");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setLocationByPlatform(true);

        // --- ROOT PANEL ---
        JPanel rootTop = new JPanel(new BorderLayout());
        rootField.setEditable(false);
        rootField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        rootField.setText(ROOT.toAbsolutePath().toString());
        rootTop.add(new JLabel("Root: "), BorderLayout.WEST);
        rootTop.add(rootField, BorderLayout.CENTER);

        rootList.setCellRenderer(new FolderRenderer());
        rootList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rootList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && rootList.getSelectedValue() != null) {
                    openBucket(rootList.getSelectedValue());
                }
            }
        });
        rootList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && rootList.getSelectedValue() != null) {
                    openBucket(rootList.getSelectedValue());
                }
            }
        });

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(rootTop, BorderLayout.NORTH);
        rootPanel.add(new JScrollPane(rootList), BorderLayout.CENTER);

        // Add to card stack
        cards.add(rootPanel, "ROOT");
        cards.add(bucketPanel, "BUCKET");
        cards.add(casePanel, "CASE");
        setContentPane(cards);

        loadRoot();
        showRoot();
    }

    private void loadRoot() {
        if (!Files.isDirectory(ROOT)) {
            JOptionPane.showMessageDialog(this, "Folder not found:\n" + ROOT,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        rootModel.clear();
        File[] dirs = ROOT.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;

        Arrays.sort(dirs, Comparator
                .comparingInt((File f) -> {
                    try { return Integer.parseInt(f.getName().replaceAll("[^0-9]", "")); }
                    catch (Exception e) { return Integer.MAX_VALUE; }
                })
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File d : dirs) rootModel.addElement(d);
        if (rootModel.size() > 0) rootList.setSelectedIndex(0);
    }

    private void openBucket(File bucketFolder) {
        this.currentBucketDir = bucketFolder.toPath();
        setTitle("Bucket: " + bucketFolder.getName());
        bucketPanel.setBucket(currentBucketDir);
        cardLayout.show(cards, "BUCKET");
    }

    // Transition from BucketPanel to Case
    private void openCase(Path caseDir) {
        setTitle(caseDir.getFileName().toString());
        casePanel.setCase(caseDir);
        cardLayout.show(cards, "CASE");
    }

    // "Back" (CASE -> BUCKET)
    private void showBucket() {
        if (currentBucketDir != null) {
            setTitle("Bucket: " + currentBucketDir.getFileName());
            bucketPanel.setBucket(currentBucketDir);
            cardLayout.show(cards, "BUCKET");
        } else {
            showRoot();
        }
    }

    // "Back" (BUCKET -> ROOT)
    private void showRoot() {
        setTitle("Grading Browser");
        cardLayout.show(cards, "ROOT");
        rootList.requestFocusInWindow();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GradingBrowser().setVisible(true));
    }
}
