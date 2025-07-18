package Exam;

import Graph.*;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.util.*;

public class Assessment {
    private final int grade;
    private final String feedback;
    private final boolean violation;
    private final String violationString;
    private final String codeBlock;
    private Graph abstractSyntaxTree;
    private Graph controlFlowGraph;

    public Assessment(int grade, String feedback, boolean violation, String violationString, String codeBlock) {
        this.grade    = grade;
        this.feedback = feedback;
        this.violation = violation;
        this.violationString= violationString;
        this.codeBlock=codeBlock;

        generateASTGraph();
        generateCFGGraph();
    }


    // generate graphs
    // generate abstract syntax tree methodu
    // generate cfg
    // print methoduyla exam directory test.

    public Graph getAbstractSyntaxTree() {
        return abstractSyntaxTree;
    }

    public String getCodeBlock() {
        return codeBlock;
    }

    public String getViolationString() {
        return violationString;
    }

    public boolean isViolation() {
        return violation;
    }

    public int getGrade() {
        return grade;
    }

    public String getFeedback() {
        return feedback;
    }

    public static ArrayList<String> extractTokens(String line) {
        ArrayList<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);


            if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < line.length() && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                    sb.append(line.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
            }

            else if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < line.length() && Character.isDigit(line.charAt(i))) {
                    sb.append(line.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
            }

            else {
                i++;
            }
        }
        return tokens;
    }

    ///////////////////////////////////////////////////////////////
    private static String constructAST(String parent, Graph graph, String line, int j, LineType lineType) {
        String fullLine = line + "-" + j;
        String body = fullLine;
        String condition;
        if (lineType.equals(LineType.IF) || lineType.equals(LineType.ELSE_IF)) {
            condition = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")) + "-" + j;
            graph.put(parent, condition);
            graph.put(parent, fullLine);
        } else if (lineType.equals(LineType.ELSE)) {
            graph.put(parent, fullLine);
        } else if (lineType.equals(LineType.WHILE) || lineType.equals(LineType.FOR)) {
            condition = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")) + "-" + j;
            graph.put(parent, fullLine);
            graph.put(fullLine, condition);
            body += "-branch";
            graph.put(fullLine, body);
        }
        return body;
    }

    private static boolean conditionAST(int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() && (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }

    private static int solveAST(int j, ArrayList<Pair<Integer, LineType>> lines, Graph graph, String parent, HashMap<Integer, String> map) {
        String body = constructAST(parent, graph, map.get(lines.get(j).getKey()), j, lines.get(j).getValue());
        j++;
        while (j < lines.size() && conditionAST(j, lines)) {
            if (lines.get(j).getValue().equals(LineType.CLOSE)) {
                j++;
                continue;
            }
            LineType cur = lines.get(j).getValue();
            if (cur.equals(LineType.STATEMENT)) {
                graph.put(body, cur + "-" + lines.get(j).getKey());
                j++;
            } else if (cur.equals(LineType.ELSE) || cur.equals(LineType.ELSE_IF)) {
                j = solveAST(j, lines, graph, parent, map);
            } else {
                j = solveAST(j, lines, graph, body, map);
                j++;
            }
        }
        return j;
    }
    private boolean astFailed = false;

    public boolean isAstFailed() {
        return astFailed;
    }


    private void generateASTGraph() {
        Graph graph = new Graph();
        try {
            HashMap<Integer, String> lineMap = new HashMap<>();
            String[] lines = codeBlock.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lineMap.put(i + 1, lines[i]);
            }

            ArrayList<Pair<Integer, LineType>> block = convertFromCodeBlock(codeBlock);
            if (block.isEmpty()) return;

            String parent = "start";
            for (int j = 0; j < block.size(); j++) {
                LineType type = block.get(j).getValue();
                int lineNum = block.get(j).getKey();

                if (type.equals(LineType.STATEMENT)) {
                    String line = lineMap.get(lineNum);
                    graph.put(parent, LineType.STATEMENT + "-" + lineNum);
                } else {
                    j = solveAST(j, block, graph, parent, lineMap);
                }
            }
        } catch (Exception e) {
            astFailed = true;
            System.err.println("Cannot generate AST: " + e.getMessage());
        }

        this.abstractSyntaxTree = graph;
    }


    /**
     * Determines the type of given line and returns it as a Graph.LineType.
     * @param line
     * @return types in that line as arraylist
     */
    private static ArrayList<LineType> getTypes(String line) {
        //updated
        ArrayList<LineType> types = new ArrayList<>();
        ArrayList<String> list= extractTokens(line);
        if (list.contains("}")) {
            types.add(LineType.CLOSE);
        }
        if (list.contains("else")) {
            if (list.contains("if")) {
                types.add(LineType.ELSE_IF);
            } else {
                types.add(LineType.ELSE);
            }
        } else if (list.contains("if") && list.contains("(")) {
            types.add(LineType.IF);
        } else if (list.contains("for") && list.contains("(")) {
            types.add(LineType.FOR);
        } else if (list.contains("while") && list.contains("(")) {
            types.add(LineType.WHILE);
        } else {
            if (types.isEmpty()) {
                types.add(LineType.STATEMENT);
            }
        }
        return types;
    }


    /**
     *
     * @param codeBlock
     * @return
     */
    public static ArrayList<Pair<Integer, LineType>> convertFromCodeBlock(String codeBlock) {
        ArrayList<Pair<Integer, LineType>> current = new ArrayList<>();

        String[] lines = codeBlock.split("\n");

        for (int j = 0; j < lines.length; j++) {
            String line = lines[j].trim();
            //updated
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/") || line.startsWith("") || line.startsWith("*/")) {
                continue;
            }

            int lineNumber = j + 1;
            ArrayList<LineType> lineTypes = getTypes(line);

            int i = 0;
            if (lineTypes.size() > 1) {
                i++;
            }

            for (LineType type : lineTypes) {
                current.add(new Pair<>(lineNumber, type));
            }

            // Blok başlıyorsa
            LineType type = lineTypes.get(i);
            if (type == LineType.FOR ||
                    type == LineType.IF ||
                    type == LineType.WHILE ||
                    type == LineType.ELSE ||
                    type == LineType.ELSE_IF) {

                boolean nextIsBrace = (j + 1 < lines.length) && lines[j + 1].trim().equals("{");
                boolean hasBraceOnLine = line.contains("{");
                boolean hasSemicolon = line.contains(";");

                if (nextIsBrace) {
                    j++; // Süslü parantez satırını stla
                } else if (!hasBraceOnLine && !hasSemicolon) {
                    // Süslü parantez de noktalı virgül de yoksa otomatik gövde ekle
                    current.add(new Pair<>(lineNumber + 1, LineType.STATEMENT));
                    current.add(new Pair<>(lineNumber + 1, LineType.CLOSE));
                }
            }
        }

        // Son blogun süslü parantez dengesi kontrolü
        if (check(current)) {
            if (!current.isEmpty() && current.get(current.size() - 1).getValue() == LineType.CLOSE) {
                current.remove(current.size() - 1);
            }
        }


        return current;
    }
    /**
     *Checks whether the brackets are balanced, i.e., whether constructs like if {} are properly closed.
     * @param last
     * @return boolean - whether the brackets are balanced
     */
    private static boolean check(ArrayList<Pair<Integer, LineType>> last) {
        // Opened blocks are stored here (LIFO logic)
        Stack<LineType> stack = new Stack<>();

        // This loops through each line inside a block
        for (Pair<Integer, LineType> pair : last) {

            // If it's a closing block, the stack should be empty afterwards; if so, return true
            if (pair.getValue().equals(LineType.CLOSE)) {
                if (stack.isEmpty()) {
                    return true;
                }
                stack.pop();

                // If it's not a closing block, but an opening one, add a new block to the stack
            } else if (pair.getValue().equals(LineType.ELSE) || pair.getValue().equals(LineType.ELSE_IF) || pair.getValue().equals(LineType.IF) || pair.getValue().equals(LineType.FOR) || pair.getValue().equals(LineType.WHILE)) {
                stack.add(pair.getValue());
            }
        }
        // After all lines are processed, if there are still unclosed blocks, it's an error
        return !stack.isEmpty();
    }

    ///////////////////////////////////////////////////////////////

    // type to string methodu. ve yardımcı methodlar
    // construct methodu her satıra karşılık gelen line type'ı graph yapısına ekler
    private static String constructType(String parent, Graph graph, String line, int j, LineType lineType) {
        String body = "[BODY] (Line " + j + ")";

        if (lineType.equals(LineType.IF) || lineType.equals(LineType.ELSE_IF)) {
            String conditionContent = extractConditionType(line);
            String condition = "[CONDITION] " + conditionContent + " (Line " + j + ")";
            String head = "[" + lineType + "] (Line " + j + ")";

            graph.put(parent, head);
            graph.put(head, condition);
            graph.put(head, body);

        } else if (lineType.equals(LineType.ELSE)) {
            String head = "[ELSE] (Line " + j + ")";
            graph.put(parent, head);
            graph.put(head, body);

        } else if (lineType.equals(LineType.WHILE) || lineType.equals(LineType.FOR)) {
            String conditionContent = extractConditionType(line);
            String head = "[" + lineType + "] (Line " + j + ")";
            String condition = "[CONDITION] " + conditionContent + " (Line " + j + ")";

            graph.put(parent, head);
            graph.put(head, condition);
            graph.put(head, body);
        }

        return body;
    }


    // her satırın parantez içini kırpıp alıyor  - koşulu yani.
    private static String extractConditionType(String line) {
        try {
            return line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
        } catch (Exception e) {
            return "UNKNOWN_CONDITION";
        }
    }


    // solve fonksiyonundaki döngünün devam edip etmeyeceğini () belirlemek için kullanılır.
    // örneğin else iften sonra kapalı parantez arkasından else geliyorsa devam.
    private static boolean conditionType(int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() &&
                    (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    // ağaç yapısını oluşturan kısım.
    private static int solveType(int j, ArrayList<Pair<Integer, LineType>> lines, Graph graph, String parent, HashMap<Integer, String> map) {

        String body = constructType(parent, graph, map.get(lines.get(j).getKey()), j, lines.get(j).getValue());
        j++;
        int realLineNo = lines.get(j).getKey();

        while (conditionType(j, lines)) {
            if (lines.get(j).getValue().equals(LineType.CLOSE)) {
                j++;
            }
            LineType cur = lines.get(j).getValue();

            // bu ksıımda satır içeriğini artık kullanmıyoruz.
            if (cur.equals(LineType.STATEMENT)) {
                int lineNo = lines.get(j).getKey(); // Satır numarası (gerçek kod satırı numarası)
                graph.put(body, "[STATEMENT] (Line " + lineNo + ")");
                j++;

            } else if (cur.equals(LineType.ELSE) || cur.equals(LineType.ELSE_IF)) {
                j = solveType(j, lines, graph, parent, map);

            } else {
                j = solveType(j, lines, graph, body, map);
                j++;
            }
        }

        return j;
    }


    // satır numaralarıyla birlikte girilen stringin satırlarını bir hash map içine koyar.
    // ortak method content ve type için.
    private static HashMap<Integer, String> createMapFromString(String input) {
        HashMap<Integer, String> map = new HashMap<>();
        String[] lines = input.split("\n");

        for (int i = 0; i < lines.length; i++) {
            map.put(i + 1, lines[i]);
        }

        return map;
    }

    //wrapper method
    public static ArrayList<ArrayList<Pair<Integer, LineType>>> convertFromString(String codeBlock) {
        ArrayList<ArrayList<Pair<Integer, LineType>>> result = new ArrayList<>();

        ArrayList<Pair<Integer, LineType>> singleBlock = convertFromCodeBlock(codeBlock);

        if (!singleBlock.isEmpty()) {
            result.add(singleBlock);  // Beklenen format: Liste içinde tek blok
        }

        return result;
    }


    public static ArrayList<Graph> generateGraphsFromStringType(String input) {
        ArrayList<Graph> graphs = new ArrayList<>();

        try {
            HashMap<Integer, String> map = createMapFromString(input);
            ArrayList<ArrayList<Pair<Integer, LineType>>> lines = convertFromString(input);


            for (ArrayList<Pair<Integer, LineType>> assessment : lines) {
                Graph g = new Graph();
                String parent = "start";
                for (int j = 0; j < assessment.size(); j++) {

                    if (assessment.get(j).getValue().equals(LineType.STATEMENT)) {
                        g.put(parent, LineType.STATEMENT + "-" + assessment.get(j).getKey());

                    } else {
                        j = solveType(j, assessment, g, parent, map);
                    }
                }
                graphs.add(g.clone());
            }

        } catch (Exception e) {
            System.out.println("Input string could not be processed.");
        }
        return graphs;
    }



    public static ArrayList<ArrayList<Graph>> generateGraphsType(String codeBlock) {
        ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
        return generateGraphsType(codeBlock);
    }


    /////////////////////////////////////////////////////////////////////////////
    // content to string methodu.


    private static String construct(String parent,
                                    Graph graph,
                                    String lineContent,
                                    int lineNo,
                                    LineType lineType) {

        // Artık BODY yok – head'in kendisi gövde gibi davranacak
        String head = lineContent.trim() + " (Line " + lineNo + ")";

        if (lineType == LineType.IF || lineType == LineType.ELSE_IF) {
            String cond = "[CONDITION] " + extractCondition(lineContent) + " (Line " + lineNo + ")";
            graph.put(parent, head);
            graph.put(head, cond);                // sadece CONDITION çocuğu
            return head;                          // <-- solve()’a geri head dönüyoruz
        }
        if (lineType == LineType.ELSE) {
            graph.put(parent, head);
            return head;                          // ELSE’in altına doğrudan statement’lar bağlanacak
        }
        if (lineType == LineType.WHILE || lineType == LineType.FOR) {
            String cond = "[CONDITION] " + extractCondition(lineContent) + " (Line " + lineNo + ")";
            graph.put(parent, head);
            graph.put(head, cond);
            return head;
        }
        return head;      // teorik olarak buraya düşmez ama derleyici mutlu olur
    }


    private static String extractCondition(String line) {
        try {
            return line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
        } catch (Exception e) {
            return "UNKNOWN_CONDITION";
        }
    }


    // solve fonksiyonundaki döngünün devam edip etmeyeceğini () belirlemek için kullanılır.
    private static boolean condition(int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() &&
                    (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }

    private static int solve(int j,
                             ArrayList<Pair<Integer, LineType>> lines,
                             Graph graph,
                             String parent,
                             HashMap<Integer, String> map) {

        int lineNo = lines.get(j).getKey();               // gerçek satır numarası
        String body = construct(parent, graph,
                map.get(lineNo),           // satır içeriği
                lineNo,
                lines.get(j).getValue());

        j++;                                              // <-- İLK SATIRI GEÇ

        while (j < lines.size() && condition(j, lines)) {

            if (lines.get(j).getValue() == LineType.CLOSE) {
                j++;
                continue;
            }

            LineType cur = lines.get(j).getValue();

            if (cur == LineType.STATEMENT) {
                int stmtLineNo = lines.get(j).getKey();   // aynı isimle tekrar tanımlama yok

                // bu kısma trim ekledik: sondaki ve baştaki boşlukları siliyor daha düzgün görüntü oluşturmak için.
                graph.put(body, map.get(stmtLineNo).trim() + " (Line " + stmtLineNo + ")");
                j++;

            } else if (cur == LineType.ELSE || cur == LineType.ELSE_IF) {
                j = solve(j, lines, graph, parent, map);

            } else {                                      // IF / WHILE / FOR vb.
                j = solve(j, lines, graph, body, map);
                j++;
            }
        }
        return j;
    }




    public static ArrayList<Graph> generateGraphsFromStringContent(String input) {
        ArrayList<Graph> graphs = new ArrayList<>();

        try {
            HashMap<Integer, String> map = createMapFromString(input);
            ArrayList<ArrayList<Pair<Integer, LineType>>> lines = convertFromString(input);


            for (ArrayList<Pair<Integer, LineType>> assessment : lines) {
                Graph g = new Graph();
                String parent = "start";

                for (int j = 0; j < assessment.size(); j++) {
                    if (assessment.get(j).getValue() == LineType.STATEMENT) {
                        int ln = assessment.get(j).getKey();
                        g.put(parent, map.get(ln).trim() + " (Line " + ln + ")");
                    } else {
                        j = solve(j, assessment, g, parent, map);
                    }
                }

                graphs.add(g.clone());
            }
        } catch (Exception e) {
            System.out.println("String input could not be processed.");
        }

        return graphs;
    }


    public static ArrayList<ArrayList<Graph>> generateGraphsContent(String codeBlock) throws FileNotFoundException {
        ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
        return generateGraphsContent(codeBlock);
    }

    //--------------------------------------------------------------------------------------------------------------


    private static boolean condition(ArrayList<Pair<Integer, LineType>> lines, ArrayList<String> lasts, String prev) {
        if (lines.get(0).getValue().equals(LineType.CLOSE)) {
            lasts.add(prev);
            return lines.size() > 1 &&
                    (lines.get(1).getValue().equals(LineType.ELSE_IF) || lines.get(1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    private static String ifStatement(String prev, Graph graph, ArrayList<Pair<Integer, LineType>> lines) {
        String first = prev;
        ArrayList<String> lasts = new ArrayList<>();
        boolean hasElse = false;

        while (condition(lines, lasts, prev)) {
            if (lines.get(0).getValue().equals(LineType.CLOSE)) {
                prev = first;
                lines.remove(0);
                if (!lines.isEmpty() && lines.get(0).getValue().equals(LineType.ELSE)) {
                    hasElse = true;
                }
                if (!lines.isEmpty()) lines.remove(0);
            }

            if (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
                prev = addNode(prev, graph, lines);
            }
        }

        if (!lines.isEmpty()) lines.remove(0); // Remove closing brace

        String end = "end-" + first;
        for (String last : lasts) {
            graph.put(last, end);
        }
        if (!hasElse) {
            graph.put(first, end);
        }

        return end;
    }

    private static String forStatement(String prev, Graph graph, ArrayList<Pair<Integer, LineType>> lines) {
        String first = prev;
        while (!lines.get(0).getValue().equals(LineType.CLOSE)) {
            prev = addNode(prev, graph, lines);
        }
        lines.remove(0);
        graph.put(prev, first);
        return prev;
    }

    private static String whileStatement(String prev, Graph graph, ArrayList<Pair<Integer, LineType>> lines) {
        String first = prev;
        while (!lines.get(0).getValue().equals(LineType.CLOSE)) {
            prev = addNode(prev, graph, lines);
        }
        lines.remove(0);
        graph.put(prev, first);
        return first;
    }

    private static String addNode(String prev, Graph graph, ArrayList<Pair<Integer, LineType>> lines) {
        Pair<Integer, LineType> pair = lines.remove(0);
        String cur;

        switch (pair.getValue()) {
            case IF:
                cur = "if-" + pair.getKey();
                graph.put(prev, cur);
                return ifStatement(cur, graph, lines);
            case STATEMENT:
                cur = "statement-" + pair.getKey();
                graph.put(prev, cur);
                return cur;
            case FOR:
                cur = "for-" + pair.getKey();
                graph.put(prev, cur);
                return forStatement(cur, graph, lines);
            case WHILE:
                cur = "while-" + pair.getKey();
                graph.put(prev, cur);
                return whileStatement(cur, graph, lines);
            default:
                return prev;
        }
    }

    private void generateCFGGraph() {
        try {
            ArrayList<Pair<Integer, LineType>> block = convertFromCodeBlock(codeBlock);
            Graph graph = new Graph();
            String prev = "start-0";
            while (!block.isEmpty()) {
                prev = addNode(prev, graph, block);
            }
            this.controlFlowGraph = graph;
        } catch (Exception e) {
            System.err.println("Cannot generate CFG: " + e.getMessage());
        }
    }















}