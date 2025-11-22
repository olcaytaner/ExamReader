package Graph;

import java.util.ArrayList;

/**
 * Kod satırlarını LineType'ları ile birlikte tutan ve yöneten sınıf.
 * Her eleman bir Pair<Integer, LineType> içerir:
 * - Integer: satır numarası
 * - LineType: satırın tipi (IF, WHILE, FOR, STATEMENT, vb.)
 */
public class LineTypeList {
    private ArrayList<Pair<Integer, LineType>> lines;

    public LineTypeList(ArrayList<Pair<Integer, LineType>> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public ArrayList<Pair<Integer, LineType>> getLines() {
        return lines;
    }

    public void add(Pair<Integer, LineType> pair) {
        lines.add(pair);
    }

    public void add(int lineNumber, LineType type) {
        lines.add(new Pair<>(lineNumber, type));
    }

    public Pair<Integer, LineType> get(int index) {
        return lines.get(index);
    }

    public Pair<Integer, LineType> remove(int index) {
        return lines.remove(index);
    }

    public int size() {
        return lines.size();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    // Assessment.java'daki condition methodu (CFG için - ifStatement'ta kullanılır)
    public boolean condition(ArrayList<String> lasts, String prev) {
        if (lines.get(0).getValue().equals(LineType.CLOSE)) {
            lasts.add(prev);
            return lines.size() > 1 &&
                    (lines.get(1).getValue().equals(LineType.ELSE_IF) ||
                            lines.get(1).getValue().equals(LineType.ELSE));
        }
        return true;
    }

    // Assessment.java'daki condition methodu (AST solve için)
    public boolean condition(int j) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() &&
                    (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) ||
                            lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }

    // Assessment.java'daki conditionType methodu (AST solveType için)
    public boolean conditionType(int j) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() &&
                    (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) ||
                            lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }

    // solve methodu - AST generation için iteration logic
    // Graph.construct() metodunu kullanır
    public int solve(int j,
                     Graph graph,
                     String parent,
                     java.util.HashMap<Integer, String> map) {

        int lineNo = lines.get(j).getKey();
        String body = graph.construct(parent, map.get(lineNo), lineNo, lines.get(j).getValue());

        j++;

        while (j < lines.size() && condition(j)) {

            if (lines.get(j).getValue() == LineType.CLOSE) {
                j++;
                continue;
            }

            LineType cur = lines.get(j).getValue();

            if (cur == LineType.STATEMENT) {
                int stmtLineNo = lines.get(j).getKey();
                graph.put(body, map.get(stmtLineNo).trim() + " (Line " + stmtLineNo + ")");
                j++;

            } else if (cur == LineType.ELSE || cur == LineType.ELSE_IF) {
                j = solve(j, graph, parent, map);

            } else {
                j = solve(j, graph, body, map);
                j++;
            }
        }
        return j;
    }

    // solveType methodu - Type-based AST generation için iteration logic
    // Graph.constructType() metodunu kullanır
    public int solveType(int j,
                         Graph graph,
                         String parent,
                         java.util.HashMap<Integer, String> map) {

        String body = graph.constructType(parent, map.get(lines.get(j).getKey()), j, lines.get(j).getValue());
        j++;

        while (conditionType(j)) {
            if (lines.get(j).getValue().equals(LineType.CLOSE)) {
                j++;
            }
            LineType cur = lines.get(j).getValue();

            if (cur.equals(LineType.STATEMENT)) {
                int lineNo = lines.get(j).getKey();
                graph.put(body, "[STATEMENT] (Line " + lineNo + ")");
                j++;

            } else if (cur.equals(LineType.ELSE) || cur.equals(LineType.ELSE_IF)) {
                j = solveType(j, graph, parent, map);

            } else {
                j = solveType(j, graph, body, map);
                j++;
            }
        }

        return j;
    }

    // Debugging için
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LineTypeList[\n");
        for (Pair<Integer, LineType> pair : lines) {
            sb.append("  Line ").append(pair.getKey())
                    .append(": ").append(pair.getValue()).append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

}