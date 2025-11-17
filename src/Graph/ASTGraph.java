package Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ASTGraph extends Graph {

    private Map<String, String> nodeLabels = new HashMap<>();
    private boolean generationFailed = false;
    public ASTGraph() {
        super();
    }
    public Map<String, String> getNodeLabels() {
        return nodeLabels;
    }
    public boolean isGenerationFailed() {
        return generationFailed;
    }

    @Override
    public ASTGraph clone() {
        ASTGraph cloned = new ASTGraph();
        for (String from : this.getGraph().keySet()) {
            for (String to : this.getGraph().get(from)) {
                cloned.put(from, to);
            }
        }
        // Node labels'ı kopyala
        cloned.nodeLabels.putAll(this.nodeLabels);
        return cloned;
    }

    public void generateFromCodeBlock(String codeBlock) {
        nodeLabels.clear();
        generationFailed = false;
        try {
            HashMap<Integer, String> lineMap = new HashMap<>();
            String[] lines = codeBlock.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lineMap.put(i + 1, lines[i]);
            }

            ArrayList<Pair<Integer, LineType>> block = SymbolTable.convertFromCodeBlock(codeBlock);
            if (block.isEmpty()) return;

            String parent = "start";
            nodeLabels.put(parent, "start");

            for (int j = 0; j < block.size(); j++) {
                LineType type = block.get(j).getValue();
                int lineNum = block.get(j).getKey();

                if (type.equals(LineType.STATEMENT)) {
                    String nodeId = LineType.STATEMENT + "-" + lineNum;
                    this.put(parent, nodeId);
                    nodeLabels.put(nodeId, lineMap.get(lineNum).trim());
                } else {
                    j = solve(j, block, parent, lineMap);
                }
            }
        } catch (Exception e) {
            generationFailed = true;
            System.err.println("Cannot generate AST: " + e.getMessage());
        }

    }

    private String constructAST(String parent,
                                String line,
                                int lineNumber,
                                LineType lineType) {

        String fullLine = line.trim();
        String nodeId = lineType + "-" + lineNumber;
        this.put(parent, nodeId);
        nodeLabels.put(nodeId, fullLine);

        if (lineType.equals(LineType.IF) || lineType.equals(LineType.ELSE_IF)) {
            String condContent = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
            String condId = "cond-" + lineNumber;
            this.put(nodeId, condId);
            nodeLabels.put(condId, condContent);
            return condId;

        } else if (lineType.equals(LineType.ELSE)) {
            return nodeId;

        } else if (lineType.equals(LineType.WHILE) || lineType.equals(LineType.FOR)) {
            String condContent = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
            String condId = "cond-" + lineNumber;
            this.put(nodeId, condId);
            nodeLabels.put(condId, condContent);

            String bodyId = "body-" + lineNumber;
            this.put(nodeId, bodyId);
            nodeLabels.put(bodyId, "body");
            return bodyId;
        }

        return nodeId;
    }

    private static boolean shouldContinue (int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() && (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    private int solve(int j,
                         ArrayList<Pair<Integer, LineType>> lines,
                         String parent,
                         HashMap<Integer, String> map) {

        int lineNumber = lines.get(j).getKey();
        LineType type = lines.get(j).getValue();

        String body = constructAST(
                parent,
                map.get(lineNumber),   // satır içeriği
                lineNumber,
                type
        );

        j++;
        while (j < lines.size() && shouldContinue(j, lines)) {
            if (lines.get(j).getValue().equals(LineType.CLOSE)) {
                j++;
                continue;
            }
            LineType cur = lines.get(j).getValue();
            if (cur.equals(LineType.STATEMENT)) {
                int stmtLine = lines.get(j).getKey();
                String nodeId = cur + "-" + stmtLine;
                this.put(body, nodeId);
                nodeLabels.put(nodeId, map.get(stmtLine).trim());
                j++;
            }
            else if (cur.equals(LineType.ELSE) || cur.equals(LineType.ELSE_IF)) {
                j = solve(j, lines, parent, map);
            } else {
                j = solve(j, lines, body, map);
                j++;
            }
        }
        return j;
    }

    public static ArrayList<ASTGraph> generateGraphsFromStringContent(String input) {
        ArrayList<ASTGraph> graphs = new ArrayList<>();

        try {
            HashMap<Integer, String> map = SymbolTable.createMapFromString(input);
            ArrayList<ArrayList<Pair<Integer, LineType>>> lines = SymbolTable.convertFromString(input);

            for (ArrayList<Pair<Integer, LineType>> assessment : lines) {
                ASTGraph astGraph = new ASTGraph();
                String parent = "start";

                for (int j = 0; j < assessment.size(); j++) {
                    if (assessment.get(j).getValue() == LineType.STATEMENT) {
                        int ln = assessment.get(j).getKey();
                        astGraph.put(parent, map.get(ln).trim() + " (Line " + ln + ")");
                    } else {
                        j = astGraph.solve(j, assessment, parent, map);
                    }
                }

                graphs.add(astGraph.clone());
            }
        } catch (Exception e) {
            System.out.println("String input could not be processed.");
        }

        return graphs;
    }

}
