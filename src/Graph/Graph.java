package Graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Graph {

    private final HashMap<String, HashSet<String>> graph;

    public Graph() {
        this.graph = new HashMap<>();
    }

    public void put(String from, String to) {
        if (!graph.containsKey(from)) {
            graph.put(from, new HashSet<>());
        }
        graph.get(from).add(to);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (String node : graph.keySet()) {
            for (String key : graph.get(node)) {
                str.append(node).append( " -> ").append(key).append("\n");
            }
        }
        return str.toString();
    }

    public Graph clone() {
        Graph graph = new Graph();
        for (String key : this.graph.keySet()) {
            for (String value : this.graph.get(key)) {
                graph.put(key, value);
            }
        }
        return graph;
    }
    public HashMap<String, HashSet<String>> getGraph() {
        return this.graph;
    }
    public String toGraphvizString(String graphName) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph \"").append(escapeDot(graphName)).append("\" {\n");
        dot.append("    node [shape=box];\n");

        java.util.Set<String> allNodes = new java.util.HashSet<>();
        for (String from : this.graph.keySet()) {
            allNodes.add(from);
            for (String to : this.graph.get(from)) {
                allNodes.add(to);
            }
        }

        for (String from : this.graph.keySet()) {
            for (String to : this.graph.get(from)) {
                dot.append("    \"").append(escapeDot(from)).append("\" -> \"").append(escapeDot(to)).append("\";\n");
            }
        }

        for (String node : allNodes) {
            dot.append("    \"").append(escapeDot(node)).append("\" [label=\"").append(escapeDot(node)).append("\"];\n");
        }

        dot.append("}\n");
        return dot.toString();
    }

    public void saveGraphviz(String outputDir, String baseName, String graphName)
            throws IOException, InterruptedException {

        if (outputDir == null || outputDir.isEmpty()) {
            outputDir = System.getProperty("user.home") + File.separator + "Desktop";
        }
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String dotPath = new File(dir, baseName + ".dot").getAbsolutePath();
        String pngPath = new File(dir, baseName + ".png").getAbsolutePath();

        try (FileWriter writer = new FileWriter(dotPath)) {
            writer.write(toGraphvizString(graphName));
        }

        String[] cmd = {"dot", "-Tpng", dotPath, "-o", pngPath};
        Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("'dot' komutu başarısız oldu. DOT dosyası: " + dotPath);
        }
    }

    private static String escapeDot(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }


}
