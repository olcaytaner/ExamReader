package UI;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public class CasePanel extends JPanel {

    private final Runnable onBack;

    // === Top bar ===
    private final JButton backBtn = new JButton("← Back");
    private final JTextField casePathField = new JTextField();

    // === LEFT SIDE (RefCode) ===
    private final JPanel refGrid = makeWrapPanel();                 // small tiles
    private final JLabel refImageLabel = new JLabel("", SwingConstants.CENTER);   // large preview
    private final JScrollPane refViewer = new JScrollPane(refImageLabel);
    private final JSplitPane refSplit =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(refGrid), refViewer);

    // === RIGHT SIDE (Folders / Students) ===
    private final CardLayout rightCards = new CardLayout();
    private final JPanel rightPanel = new JPanel(rightCards);

    // 1) Folders card (0,1,...)
    private final JPanel scoresGrid = makeWrapPanel();
    private final JPanel scoresCard = new JPanel(new BorderLayout());

    // 2) Student card (s012345-ast.png ... grid + large preview)
    private final JPanel imagesGrid = makeWrapPanel();
    private final JLabel stuImageLabel = new JLabel("", SwingConstants.CENTER);
    private final JScrollPane stuViewer = new JScrollPane(stuImageLabel);
    private final JSplitPane imagesSplit =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(imagesGrid), stuViewer);
    private final JPanel imagesCard = new JPanel(new BorderLayout());
    private final JButton rightBackBtn = new JButton("← Folders");

    // === Main LEFT/RIGHT split (resizable by drag) ===
    private final JSplitPane mainSplit;

    // State
    private Path caseDir;
    private Path currentScoreDir;

    public CasePanel(Runnable onBack) {
        super(new BorderLayout());
        this.onBack = onBack;

        // Top bar
        casePathField.setEditable(false);
        casePathField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        backBtn.addActionListener(e -> onBack.run());

        JPanel top = new JPanel(new BorderLayout());
        top.add(backBtn, BorderLayout.WEST);
        top.add(casePathField, BorderLayout.CENTER);

        // Left header
        JLabel refTitle = new JLabel("Ref Code", SwingConstants.CENTER);
        refTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(refTitle, BorderLayout.NORTH);
        refSplit.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        refSplit.setResizeWeight(0.55);
        refSplit.setContinuousLayout(true);
        leftPanel.add(refSplit, BorderLayout.CENTER);

        // Right: folders card
        JLabel rightTitle = new JLabel("Folders", SwingConstants.LEFT);
        rightTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel scoresHeader = new JPanel(new BorderLayout());
        scoresHeader.add(rightTitle, BorderLayout.WEST);

        scoresCard.add(scoresHeader, BorderLayout.NORTH);
        scoresCard.add(new JScrollPane(scoresGrid), BorderLayout.CENTER);

        // Right: student images card
        JPanel imagesHeader = new JPanel(new BorderLayout());
        imagesHeader.add(rightBackBtn, BorderLayout.WEST);
        JLabel imgsTitle = new JLabel("Student Images", SwingConstants.LEFT);
        imgsTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        imagesHeader.add(imgsTitle, BorderLayout.CENTER);
        rightBackBtn.addActionListener(e -> showScoresCard());

        imagesSplit.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        imagesSplit.setResizeWeight(0.55);
        imagesSplit.setContinuousLayout(true);

        imagesCard.add(imagesHeader, BorderLayout.NORTH);
        imagesCard.add(imagesSplit, BorderLayout.CENTER);

        // Right cards
        rightPanel.add(scoresCard, "SCORES");
        rightPanel.add(imagesCard, "IMAGES");

        // Main horizontal split
        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplit.setResizeWeight(0.36);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);

        add(top, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
    }

    public void setCase(Path caseDir) {
        this.caseDir = caseDir;
        this.casePathField.setText(caseDir.toAbsolutePath().toString());

        refImageLabel.setIcon(null);
        stuImageLabel.setIcon(null);

        loadRefImages();
        loadScoreFolders();

        showScoresCard();
        mainSplit.setDividerLocation(0.36);
        refSplit.setDividerLocation(0.55);
    }

    // ------------------ LEFT: Ref Code ------------------

    private void loadRefImages() {
        refGrid.removeAll();
        addRefTile(findRefFile("ast"), "AST");
        addRefTile(findRefFile("cfg"), "CFG");
        addRefTile(findRefFile("ddg"), "DDG");
        refGrid.revalidate();
        refGrid.repaint();
    }

    private File findRefFile(String type) {
        File[] matches = caseDir.toFile().listFiles(f ->
                f.isFile() &&
                        f.getName().toLowerCase().endsWith(type + ".png") &&
                        f.getName().toLowerCase().startsWith("ref")
        );
        if (matches != null && matches.length > 0) {
            Arrays.sort(matches);
            return matches[0]; // ilk bulunanı al
        }
        return caseDir.resolve("ref-" + type + ".png").toFile(); // fallback
    }

    private void addRefTile(File png, String label) {
        JPanel tile = makeTile(label, png.isFile() ? loadThumb(png, 320, 220) : null);
        tile.setToolTipText(png.getName());
        if (png.isFile()) {
            tile.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showLeftImage(png);
                }
            });
        } else {
            tile.add(new JLabel("(missing)", SwingConstants.CENTER), BorderLayout.CENTER);
        }
        refGrid.add(tile);
    }

    private void showLeftImage(File imgFile) {
        setImageTo(refImageLabel, imgFile);
    }

    // ------------------ RIGHT: Folders / Students ------------------

    private void loadScoreFolders() {
        scoresGrid.removeAll();
        if (caseDir == null || !Files.isDirectory(caseDir)) return;
        File[] dirs = caseDir.toFile().listFiles(File::isDirectory);
        if (dirs == null) return;

        Arrays.sort(dirs, Comparator
                .comparingInt((File f) -> {
                    try {
                        return Integer.parseInt(f.getName().replaceAll("[^0-9]", ""));
                    } catch (Exception e) {
                        return Integer.MAX_VALUE;
                    }
                })
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File d : dirs) {
            JPanel tile = makeFolderTile(d.getName());
            tile.setToolTipText(d.getAbsolutePath());
            tile.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showImagesIn(d.toPath());
                }
            });
            scoresGrid.add(tile);
        }
        scoresGrid.revalidate();
        scoresGrid.repaint();
    }

    private void showScoresCard() {
        rightCards.show(rightPanel, "SCORES");
        stuImageLabel.setIcon(null);
    }

    private void showImagesIn(Path scoreDir) {
        this.currentScoreDir = scoreDir;
        imagesGrid.removeAll();

        File[] pngs = scoreDir.toFile().listFiles(f -> {
            String n = f.getName().toLowerCase();
            return f.isFile() && n.endsWith(".png");
        });

        if (pngs != null) {
            Arrays.sort(pngs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File f : pngs) {
                String name = f.getName().replace(".png", "");
                JPanel tile = makeTile(name, loadThumb(f, 240, 180));
                tile.setToolTipText(f.getAbsolutePath());
                tile.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        showRightImage(f);
                    }
                });
                imagesGrid.add(tile);
            }
        }

        imagesGrid.revalidate();
        imagesGrid.repaint();
        rightCards.show(rightPanel, "IMAGES");
        imagesSplit.setDividerLocation(0.55);
    }

    private void showRightImage(File imgFile) {
        setImageTo(stuImageLabel, imgFile);
    }

    // ------------------ shared helpers ------------------

    private static JPanel makeWrapPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return p;
    }

    private static JPanel makeTile(String title, Image iconImage) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        panel.setPreferredSize(new Dimension(260, 220));

        if (iconImage != null) {
            JLabel img = new JLabel(new ImageIcon(iconImage));
            img.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(img, BorderLayout.CENTER);
        } else {
            JLabel ph = new JLabel("", UIManager.getIcon("FileView.fileIcon"), SwingConstants.CENTER);
            ph.setVerticalTextPosition(SwingConstants.BOTTOM);
            ph.setHorizontalTextPosition(SwingConstants.CENTER);
            panel.add(ph, BorderLayout.CENTER);
        }

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 6));
        panel.add(titleLbl, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel makeFolderTile(String name) {
        JPanel panel = makeTile(name, null);
        JLabel icon = new JLabel(UIManager.getIcon("FileView.directoryIcon"), SwingConstants.CENTER);
        panel.add(icon, BorderLayout.CENTER);
        return panel;
    }

    private static Image loadThumb(File file, int w, int h) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return null;
            return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void setImageTo(JLabel target, File imgFile) {
        try {
            BufferedImage img = ImageIO.read(imgFile);
            target.setIcon(new ImageIcon(img));
            target.setText("");
        } catch (Exception e) {
            target.setIcon(null);
            target.setText("Image failed to open: " + imgFile.getName());
        }
    }
}
