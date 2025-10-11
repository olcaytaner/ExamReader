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
}
