package Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CFGGraph extends Graph {
    private Map<String, String> nodeLabels = new HashMap<>();
    private boolean generationFailed = false;


    public CFGGraph() {
        super();
    }
    public Map<String, String> getNodeLabels() {
        return nodeLabels;
    }
    public boolean isGenerationFailed() {
        return generationFailed;
    }

    @Override
    public CFGGraph clone() {
        CFGGraph cloned = new CFGGraph();

        // Graph'taki tüm edge'leri kopyala
        for (String from : this.getGraph().keySet()) {
            for (String to : this.getGraph().get(from)) {
                cloned.put(from, to);
            }
        }

        // Node label'larını kopyala
        cloned.nodeLabels.putAll(this.nodeLabels);

        // generationFailed flag'ini kopyala
        cloned.generationFailed = this.generationFailed;

        return cloned;
    }


    private boolean shouldContinueIf(LineTypeList lines, ArrayList<String> lasts, String prev) {
        if (lines.get(0).getValue().equals(LineType.CLOSE)) {
            lasts.add(prev);
            return lines.size() > 1 &&
                    (lines.get(1).getValue().equals(LineType.ELSE_IF) || lines.get(1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    private String handleIfStatement(
            String prev,
            LineTypeList lines,
            HashMap<Integer,String> lineMap
    ) {
        String first = prev;
        ArrayList<String> lasts = new ArrayList<>();
        boolean hasElse = false;

        while (!lines.isEmpty() && shouldContinueIf(lines, lasts, prev)) {
            if (lines.get(0).getValue().equals(LineType.CLOSE)) {
                prev = first;
                lines.remove(0);
                if (!lines.isEmpty() && lines.get(0).getValue().equals(LineType.ELSE)) {
                    hasElse = true;
                }
                if (!lines.isEmpty()) lines.remove(0);
            }

            if (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
                prev = this.addNode(prev, lines, lineMap);
            }
        }

        if (!lines.isEmpty()) lines.remove(0); // kapanış parantezini at

        String end = "end-" + first;
        for (String last : lasts) {
            this.put(last, end);
        }
        if (!hasElse) {
            this.put(first, end);
        }

        return end;
    }

    private String handleForStatement(
            String prev,
            LineTypeList lines,
            HashMap<Integer,String> lineMap
    ) {
        String first = prev;
        while (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
            prev = addNode(prev, lines, lineMap);
        }
        if (!lines.isEmpty()) {
            lines.remove(0);
        }
        this.put(prev, first);


        try {
            int lineNo = Integer.parseInt(first.split("-")[1]);
            nodeLabels.put(first, lineMap.get(lineNo).trim());
        } catch (Exception ignored) {}

        return prev;
    }


    private String handleWhileStatement(
            String prev,
            LineTypeList lines,
            HashMap<Integer,String> lineMap
    ) {
        String first = prev;
        while (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
            prev = addNode(prev, lines, lineMap);
        }
        if (!lines.isEmpty()) {
            lines.remove(0);
        }
        this.put(prev, first);


        try {
            int lineNo = Integer.parseInt(first.split("-")[1]);
            nodeLabels.put(first, lineMap.get(lineNo).trim());
        } catch (Exception ignored) {}

        return first;
    }



    private String addNode(
            String prev,
            LineTypeList lines,
            HashMap<Integer,String> lineMap
    ) {
        Pair<Integer, LineType> pair = lines.remove(0);
        String cur;
        int lineNo = pair.getKey();
        String lineContent = lineMap.get(lineNo).trim();

        switch (pair.getValue()) {
            case IF:
                cur = "if-" + lineNo;
                this.put(prev, cur);
                nodeLabels.put(cur, lineContent); // label = kod satırı
                return handleIfStatement(cur, lines, lineMap);

            case STATEMENT:
                cur = "statement-" + lineNo;
                this.put(prev, cur);
                nodeLabels.put(cur, lineContent); // label = kod satırı
                return cur;

            case FOR:
                cur = "for-" + lineNo;
                this.put(prev, cur);
                nodeLabels.put(cur, lineContent);
                return handleForStatement(cur, lines, lineMap);

            case WHILE:
                cur = "while-" + lineNo;
                this.put(prev, cur);
                nodeLabels.put(cur, lineContent);
                return handleWhileStatement(cur, lines, lineMap);

            default:
                return prev;
        }
    }


    public void generateFromCodeBlock(String codeBlock) {
        nodeLabels.clear();
        try {
            LineTypeList block = new LineTypeList(SymbolTable.convertFromCodeBlock(codeBlock));

            HashMap<Integer, String> lineMap = new HashMap<>();
            String[] lines = codeBlock.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lineMap.put(i + 1, lines[i]);
            }

            String prev = "start-0";
            nodeLabels.put(prev, "start");

            while (!block.isEmpty()) {
                prev = addNode(prev, block, lineMap);
            }
        } catch (Exception e) {
            generationFailed = true;
            System.err.println("Cannot generate CFG: " + e.getMessage());
        }
    }

}
