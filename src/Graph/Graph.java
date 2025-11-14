package Graph;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Graph {

    private final HashMap<String, HashSet<String>> graph;

    public Graph() {
        this.graph = new HashMap<>();
    }

    public void put(String from, String to) {
        graph.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    public HashMap<String, HashSet<String>> getGraph() {
        return this.graph;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String node : graph.keySet()) {
            for (String key : graph.get(node)) {
                str.append(node).append(" -> ").append(key).append("\n");
            }
        }
        return str.toString();
    }

    public Graph clone() {
        Graph g = new Graph();
        for (String from : graph.keySet()) {
            for (String to : graph.get(from)) {
                g.put(from, to);
            }
        }
        return g;
    }

    public void saveGraphviz(String directory,
                             String fileName,
                             String graphName,
                             Map<String, String> nodeLabels,
                             List<Integer> highlightLines) throws IOException {

        File outFile = new File(directory, fileName + ".dot");
        try (PrintWriter writer = new PrintWriter(outFile)) {
            writer.println("digraph \"" + escapeDot(graphName) + "\" {");
            writer.println("  node [shape=box, style=filled, fontname=\"Helvetica\"];");

            // Edges
            for (Map.Entry<String, HashSet<String>> e : graph.entrySet()) {
                String from = e.getKey();
                for (String to : e.getValue()) {
                    writer.printf("  \"%s\" -> \"%s\";%n", escapeDot(from), escapeDot(to));
                }
            }

            // Nodes (renkli)
            for (Map.Entry<String, String> entry : nodeLabels.entrySet()) {
                String nodeId = entry.getKey();
                String label = entry.getValue();

                Integer lineNo = extractLineNo(nodeId, label);
                String color = "white";

                if (lineNo != null) {
                    color = (highlightLines != null && highlightLines.contains(lineNo))
                            ? "#9aff9a" // yeşil: eşleşen
                            : "#ffb3b3"; // kırmızı: eşleşmeyen
                }

                writer.printf("  \"%s\" [label=\"%s\", style=filled, fillcolor=\"%s\"];%n",
                        escapeDot(nodeId),
                        escapeDot(label),
                        color);
            }

            writer.println("}");
        }
    }


    public void saveGraphviz(String directory,
                             String fileName,
                             String graphName,
                             Map<String, String> nodeLabels) throws IOException {
        // Boş highlight listesiyle çağır
        saveGraphviz(directory, fileName, graphName, nodeLabels, Collections.emptyList());
    }


    private static Integer extractLineNo(String nodeId, String label) {
        Matcher m;
        // if-12, statement-7 gibi ID'lerden
        m = Pattern.compile("-(\\d+)$").matcher(nodeId);
        if (m.find()) return Integer.parseInt(m.group(1));

        // Label içinde "Line 7" veya "(Line 7)"
        m = Pattern.compile("\\bLine\\s*(\\d+)\\b").matcher(label);
        if (m.find()) return Integer.parseInt(m.group(1));
        m = Pattern.compile("\\(\\s*Line\\s*(\\d+)\\s*\\)").matcher(label);
        if (m.find()) return Integer.parseInt(m.group(1));

        return null;
    }


    private static String escapeDot(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    // new methods for generating colored graphs.
    // HSL'den hex renk üretmek için kullanılan yardımcı methodlar
    private static String hslToHex(double hDeg, double s, double l) {
        double h = (hDeg % 360.0) / 360.0; // 0–1 aralığında üretmek için

        double r, g, b;

        if (s == 0) {
            r = g = b = l; // gri ton
        } else {
            double q = l < 0.5 ? l * (1 + s) : (l + s - l * s);
            double p = 2 * l - q;
            r = hue2rgb(p, q, h + 1.0 / 3.0);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1.0 / 3.0);
        }

        int ri = (int) Math.round(r * 255);
        int gi = (int) Math.round(g * 255);
        int bi = (int) Math.round(b * 255);

        return String.format("#%02X%02X%02X", ri, gi, bi);
    }

    private static double hue2rgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }

    private static Map<String, String> generateUniqueColors(Set<String> nodeIds) {
        Map<String, String> colorMap = new HashMap<>();

        int n = nodeIds.size();
        if (n == 0) {
            return colorMap;
        }

        int index = 0;
        for (String id : nodeIds) {
            // Hue: 0–360 dereceleri node sayısına göre eşit böl
            double h = (360.0 * index) / n;
            double s = 0.70;  // doygunluk
            double l = 0.55;  // parlaklık

            String hex = hslToHex(h, s, l);
            colorMap.put(id, hex);

            index++;
        }

        return colorMap;
    }

    // Her node için benzersiz renk üreten Graphviz çıktısı
    // Bu methodu exam classtaki saveonlypngs methodunda refcode/student code ayrımı yapıldıktan sonra,
    // eğer elimizdeki refcode ise bu methodu değil ise diğer methodu kullanacağız.
    public void saveGraphvizUniqueColors(String directory,
                                         String fileName,
                                         String graphName,
                                         Map<String, String> nodeLabels) throws IOException {

        File outFile = new File(directory, fileName + ".dot");
        try (PrintWriter writer = new PrintWriter(outFile)) {
            writer.println("digraph \"" + escapeDot(graphName) + "\" {");
            writer.println("  node [shape=box, style=filled, fontname=\"Helvetica\"];");

            // Kenarlar
            for (Map.Entry<String, HashSet<String>> e : graph.entrySet()) {
                String from = e.getKey();
                for (String to : e.getValue()) {
                    writer.printf("  \"%s\" -> \"%s\";%n",
                            escapeDot(from),
                            escapeDot(to));
                }
            }

            // Her node'a benzersiz renk ata
            Map<String, String> colorMap = generateUniqueColors(nodeLabels.keySet());

            for (Map.Entry<String, String> entry : nodeLabels.entrySet()) {
                String nodeId = entry.getKey();
                String label  = entry.getValue();

                String color = colorMap.getOrDefault(nodeId, "white");

                writer.printf("  \"%s\" [label=\"%s\", style=filled, fillcolor=\"%s\"];%n",
                        escapeDot(nodeId),
                        escapeDot(label),
                        color);
            }

            writer.println("}");
        }
    }

}
