package Graph;

import java.util.*;

/**
 * Kenar eklenme sırasını koruyor,
 * DFS destekleyen graph yapısı.
 */
public class Graph {

    /** Kenar numarası -> Kenar */
    private final TreeMap<Integer, Edge> edges = new TreeMap<>();

    // Her eklemede artan sıra numarası
    // Böylece tree mapde sıralı bir şekilde tutup printleyebilicez.
    private int seq = 0;

    /** inner class: directed edge */
    private static class Edge {
        final String from;
        final String to;
        Edge(String from, String to) {
            this.from = from;
            this.to   = to;
        }
    }

    /** Yeni kenar ekle: eklenme sırası otomatik tutulur */
    public void put(String from, String to) {
        edges.put(seq++, new Edge(from, to));
    }

    /** Eklenme sırasına göre kenarları yazdır */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Edge e : edges.values()) {
            sb.append(e.from).append(" -> ").append(e.to).append('\n');
        }
        return sb.toString();
    }

    /** Bir düğümün çocuklarını (eklenme sırasına göre) getirir */
    public List<String> getChildren(String node) {
        List<String> out = new ArrayList<>();
        for (Edge e : edges.values()) {
            if (e.from.equals(node)) {
                out.add(e.to);
            }
        }
        return out;
    }

    /** Kök düğüm
     *
     * @return
     */
    public String getRoot() {
        return "start";
    }

    public void printDFS() {
        printDFSRecursive(getRoot(), 0, new HashSet<>());
    }

    private void printDFSRecursive(String node, int depth, Set<String> visited) {
        if (visited.contains(node)) return;
        visited.add(node);

        System.out.println("    ".repeat(depth) + node);

        for (String child : getChildren(node)) {
            printDFSRecursive(child, depth + 1, visited);
        }
    }

    /**  (kenar sırası da korunur) */
    public Graph clone() {
        Graph g = new Graph();
        for (Edge e : this.edges.values()) {
            g.put(e.from, e.to);
        }
        return g;
    }
}
