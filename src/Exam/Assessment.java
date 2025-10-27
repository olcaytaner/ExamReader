package Exam;


import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;

import Graph.*;

import Exam.Code;



public class Assessment {
    private final int grade;
    private final String feedback;
    private final boolean violation;
    private final String violationString;
    private final String codeBlock;
    private Code code;
    private Graph abstractSyntaxTree;
    private Graph controlFlowGraph;
    private Graph dataDependencyGraph;

    private Map<String, String> astNodeLabels = new HashMap<>();
    private Map<String, String> cfgNodeLabels = new HashMap<>();
    private Map<String, String> ddgNodeLabels = new HashMap<>();

    private boolean astFailed = false;
    private boolean cfgFailed = false;
    private boolean ddgFailed = false;

    public boolean isAstFailed() { return astFailed; }
    public boolean isCfgFailed() { return cfgFailed; }
    public boolean isDdgFailed() { return ddgFailed; }

    public Map<String, String> getAstNodeLabels() {
        return astNodeLabels;
    }

    public Map<String, String> getCfgNodeLabels() {
        return cfgNodeLabels;
    }

    public Map<String, String> getDdgNodeLabels() {
        return ddgNodeLabels;
    }

    public Assessment(int grade, String feedback, boolean violation, String violationString, String codeBlock) {
        this.grade    = grade;
        this.feedback = feedback;
        this.violation = violation;
        this.violationString= violationString;
        this.codeBlock=codeBlock;

        generateASTGraph();
        generateCFGGraph();
        generateDDGGraph();
    }

    public Assessment(int grade, String feedback, boolean violation, String violationString, String codeBlock, Code code) {
        this(grade, feedback, violation, violationString, codeBlock); // eski constructor burada kullanılıyor
        this.code = code; // code burada atanıyor.
    }


    // generate graphs
    // generate abstract syntax tree methodu
    // generate cfg
    // print methoduyla exam directory test.



    public Graph getAbstractSyntaxTree() {
        return abstractSyntaxTree;
    }

    public Graph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public Graph getDataDependencyGraph() {
        return dataDependencyGraph;
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

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
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
            }else if (c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']') {
                tokens.add(String.valueOf(c));
                i++;
            }

            else if ("=+-*/%<>!&|;,".indexOf(c) >= 0) {
                StringBuilder sb = new StringBuilder();
                while (i < line.length() && "=+-*/%<>!&|;,".indexOf(line.charAt(i)) >= 0 ){
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

    public static ArrayList<String> extractTokensWithDots(String line) {
        ArrayList<String> tokens = new ArrayList<>();
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);

            // identifier veya _ ile başlayanları yakala
            if (Character.isLetter(c) || c == '_') {
                // 1) temel identifier
                StringBuilder ident = new StringBuilder();
                while (i < line.length() && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                    ident.append(line.charAt(i));
                    i++;
                }
                String base = ident.toString();
                tokens.add(base);

                // 2) full token: [ ... ] ve .segment zincirlerini ekle
                StringBuilder full = new StringBuilder(base);
                boolean extended = false;
                boolean keepGoing = true;

                while (keepGoing && i < line.length()) {
                    char ch = line.charAt(i);

                    if (ch == '[') {
                        extended = true;
                        // köşeli parantezli index(ler)i olduğu gibi full'e ekle (basit dengeleme ile)
                        int depth = 0;
                        do {
                            char cc = line.charAt(i);
                            full.append(cc);
                            if (cc == '[') depth++;
                            else if (cc == ']') depth--;
                            i++;
                            if (i >= line.length()) break;
                        } while (depth > 0);
                    } else if (ch == '.') {
                        extended = true;
                        full.append(ch);
                        i++;
                        // nokta sonrası boşlukları at
                        while (i < line.length() && Character.isWhitespace(line.charAt(i))) i++;
                        // nokta sonrası yeni identifier bekliyoruz
                        StringBuilder seg = new StringBuilder();
                        while (i < line.length() && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                            char cc = line.charAt(i);
                            full.append(cc);
                            seg.append(cc);
                            i++;
                        }
                        // seg yoksa zincir bitmiş say
                        if (seg.length() == 0) keepGoing = false;
                    } else {
                        keepGoing = false;
                    }
                }

                if (extended) {
                    tokens.add(full.toString());
                }
                continue;
            }

            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < line.length() && Character.isDigit(line.charAt(i))) {
                    sb.append(line.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                continue;
            }

            if (c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']') {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            if ("=+-*/%<>!&|;,".indexOf(c) >= 0) {
                StringBuilder sb = new StringBuilder();
                while (i < line.length() && "=+-*/%<>!&|;,".indexOf(line.charAt(i)) >= 0) {
                    sb.append(line.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                continue;
            }

            i++;
        }
        return tokens;
    }

    private String constructAST(String parent,
                                Graph graph,
                                String line,
                                int lineNumber,
                                LineType lineType,
                                Map<String, String> nodeLabels) {

        String fullLine = line.trim();
        String nodeId = lineType + "-" + lineNumber;
        graph.put(parent, nodeId);
        nodeLabels.put(nodeId, fullLine);

        if (lineType.equals(LineType.IF) || lineType.equals(LineType.ELSE_IF)) {
            String condContent = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
            String condId = "cond-" + lineNumber;
            graph.put(nodeId, condId);
            nodeLabels.put(condId, condContent);
            return condId;

        } else if (lineType.equals(LineType.ELSE)) {
            return nodeId;

        } else if (lineType.equals(LineType.WHILE) || lineType.equals(LineType.FOR)) {
            String condContent = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
            String condId = "cond-" + lineNumber;
            graph.put(nodeId, condId);
            nodeLabels.put(condId, condContent);

            String bodyId = "body-" + lineNumber;
            graph.put(nodeId, bodyId);
            nodeLabels.put(bodyId, "body");
            return bodyId;
        }

        return nodeId;
    }



    private static boolean conditionAST(int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() && (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }

    private int solveAST(int j,
                         ArrayList<Pair<Integer, LineType>> lines,
                         Graph graph,
                         String parent,
                         HashMap<Integer, String> map) {

        int lineNumber = lines.get(j).getKey();
        LineType type = lines.get(j).getValue();

        String body = constructAST(
                parent,
                graph,
                map.get(lineNumber),   // satır içeriği
                lineNumber,
                type,
                astNodeLabels
        );

        j++;
        while (j < lines.size() && conditionAST(j, lines)) {
            if (lines.get(j).getValue().equals(LineType.CLOSE)) {
                j++;
                continue;
            }
            LineType cur = lines.get(j).getValue();
            if (cur.equals(LineType.STATEMENT)) {
                int stmtLine = lines.get(j).getKey();
                String nodeId = cur + "-" + stmtLine;
                graph.put(body, nodeId);
                astNodeLabels.put(nodeId, map.get(stmtLine).trim());
                j++;
            }
            else if (cur.equals(LineType.ELSE) || cur.equals(LineType.ELSE_IF)) {
                j = solveAST(j, lines, graph, parent, map);
            } else {
                j = solveAST(j, lines, graph, body, map);
                j++;
            }
        }
        return j;
    }



    private void generateASTGraph() {
        Graph graph = new Graph();
        astNodeLabels.clear();
        try {
            HashMap<Integer, String> lineMap = new HashMap<>();
            String[] lines = codeBlock.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lineMap.put(i + 1, lines[i]);
            }

            ArrayList<Pair<Integer, LineType>> block = convertFromCodeBlock(codeBlock);
            if (block.isEmpty()) return;

            String parent = "start";
            astNodeLabels.put(parent, "start");

            for (int j = 0; j < block.size(); j++) {
                LineType type = block.get(j).getValue();
                int lineNum = block.get(j).getKey();

                if (type.equals(LineType.STATEMENT)) {
                    String nodeId = LineType.STATEMENT + "-" + lineNum;
                    graph.put(parent, nodeId);
                    astNodeLabels.put(nodeId, lineMap.get(lineNum).trim());
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
     * Determines the type of given line and returns it as a LineType.
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
            // BU KISIM FARKLI!! BURADA HATA MI VAR BAK!!
            if (line.isEmpty()
                    || line.startsWith("/")
                    || line.startsWith("*")
                    || line.startsWith("*/")) {
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
                    j++; // Süslü parantez satırını atla
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
            result.add(singleBlock);
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

    // BU KISIM FARKLI - müjganın exam reader'ı (githubdaki son hali). bendeki exam reader ile aynı.
    public static ArrayList<ArrayList<Graph>> generateGraphsType(String codeBlock) {
        ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
        ArrayList<Graph> singleGraphList = generateGraphsFromStringType(codeBlock);
        graphs.add(singleGraphList);
        return graphs;
    }


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
    // CFG

    private static boolean condition(ArrayList<Pair<Integer, LineType>> lines, ArrayList<String> lasts, String prev) {
        if (lines.get(0).getValue().equals(LineType.CLOSE)) {
            lasts.add(prev);
            return lines.size() > 1 &&
                    (lines.get(1).getValue().equals(LineType.ELSE_IF) || lines.get(1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    private static String ifStatement(
            String prev,
            Graph graph,
            ArrayList<Pair<Integer, LineType>> lines,
            Map<String,String> nodeLabels,
            HashMap<Integer,String> lineMap
    ) {
        String first = prev;
        ArrayList<String> lasts = new ArrayList<>();
        boolean hasElse = false;

        while (!lines.isEmpty() && condition(lines, lasts, prev)) {
            if (lines.get(0).getValue().equals(LineType.CLOSE)) {
                prev = first;
                lines.remove(0);
                if (!lines.isEmpty() && lines.get(0).getValue().equals(LineType.ELSE)) {
                    hasElse = true;
                }
                if (!lines.isEmpty()) lines.remove(0);
            }

            if (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
                prev = addNode(prev, graph, lines, nodeLabels, lineMap);
            }
        }

        if (!lines.isEmpty()) lines.remove(0); // kapanış parantezini at

        String end = "end-" + first;
        for (String last : lasts) {
            graph.put(last, end);
        }
        if (!hasElse) {
            graph.put(first, end);
        }

        return end;
    }

    private static String forStatement(
            String prev,
            Graph graph,
            ArrayList<Pair<Integer, LineType>> lines,
            Map<String,String> nodeLabels,
            HashMap<Integer,String> lineMap
    ) {
        String first = prev;
        while (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
            prev = addNode(prev, graph, lines, nodeLabels, lineMap);
        }
        if (!lines.isEmpty()) {
            lines.remove(0);
        }
        graph.put(prev, first);


        try {
            int lineNo = Integer.parseInt(first.split("-")[1]);
            nodeLabels.put(first, lineMap.get(lineNo).trim());
        } catch (Exception ignored) {}

        return prev;
    }

    private static String whileStatement(
            String prev,
            Graph graph,
            ArrayList<Pair<Integer, LineType>> lines,
            Map<String,String> nodeLabels,
            HashMap<Integer,String> lineMap
    ) {
        String first = prev;
        while (!lines.isEmpty() && !lines.get(0).getValue().equals(LineType.CLOSE)) {
            prev = addNode(prev, graph, lines, nodeLabels, lineMap);
        }
        if (!lines.isEmpty()) {
            lines.remove(0);
        }
        graph.put(prev, first);


        try {
            int lineNo = Integer.parseInt(first.split("-")[1]);
            nodeLabels.put(first, lineMap.get(lineNo).trim());
        } catch (Exception ignored) {}

        return first;
    }


    private static String addNode(
            String prev,
            Graph graph,
            ArrayList<Pair<Integer, LineType>> lines,
            Map<String,String> nodeLabels,
            HashMap<Integer,String> lineMap
    ) {
        Pair<Integer, LineType> pair = lines.remove(0);
        String cur;
        int lineNo = pair.getKey();
        String lineContent = lineMap.get(lineNo).trim();

        switch (pair.getValue()) {
            case IF:
                cur = "if-" + lineNo;
                graph.put(prev, cur);
                nodeLabels.put(cur, lineContent); // label = kod satırı
                return ifStatement(cur, graph, lines, nodeLabels, lineMap);

            case STATEMENT:
                cur = "statement-" + lineNo;
                graph.put(prev, cur);
                nodeLabels.put(cur, lineContent); // label = kod satırı
                return cur;

            case FOR:
                cur = "for-" + lineNo;
                graph.put(prev, cur);
                nodeLabels.put(cur, lineContent);
                return forStatement(cur, graph, lines, nodeLabels, lineMap);

            case WHILE:
                cur = "while-" + lineNo;
                graph.put(prev, cur);
                nodeLabels.put(cur, lineContent);
                return whileStatement(cur, graph, lines, nodeLabels, lineMap);

            default:
                return prev;
        }
    }



    // BU KISIM FARKLI - müjganın exam reader'ı, githubdaki son hali.
    private void generateCFGGraph() {
        Graph graph = new Graph();
        cfgNodeLabels.clear();
        try {
            ArrayList<Pair<Integer, LineType>> block = convertFromCodeBlock(codeBlock);

            HashMap<Integer, String> lineMap = new HashMap<>();
            String[] lines = codeBlock.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lineMap.put(i + 1, lines[i]);
            }

            String prev = "start-0";
            cfgNodeLabels.put(prev, "start");

            while (!block.isEmpty()) {
                prev = addNode(prev, graph, block, cfgNodeLabels, lineMap);
            }
            this.controlFlowGraph = graph;
        } catch (Exception e) {
            cfgFailed = true;
            this.controlFlowGraph = new Graph();
            System.err.println("Cannot generate CFG: " + e.getMessage());
        }
    }


    private Set<Pair<Integer, String>> getWrittenVars(String line, int lineNumber) {
        Set<Pair<Integer, String>> result = new HashSet<>();
        ArrayList<String> tokens = extractTokens(line);

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("=") && i > 0) {
                String left = tokens.get(i - 1);
                if (isVariable(left)) {
                    result.add(new Pair<>(lineNumber, left));
                }
            }
        }
        return result;
    }


    private Set<Pair<Integer, String>> getReadVars(String line, int lineNumber) {
        Set<Pair<Integer, String>> result = new HashSet<>();
        ArrayList<String> tokens = extractTokensWithDots(line);

        boolean reading = false;

        for (int i = 0; i< tokens.size();i++) {

            String t = tokens.get(i);
            // ifin içinde == veya != varsa hem sağındaki hem solundaki değişkeni ekle.
            if (t.equals("==") || t.equals("!=")) {
                // soldaki ilk değişken
                int li = i - 1;
                while (li >= 0) {
                    String lt = tokens.get(li);
                    if (isVariable(lt)) { result.add(new Pair<>(lineNumber, lt)); break; }
                    if (lt.equals(";") || lt.equals("{") || lt.equals("}")) break;
                    li--;
                }
                // sağdaki ilk değişken
                int ri = i + 1;
                while (ri < tokens.size()) {
                    String rt = tokens.get(ri);
                    if (isVariable(rt)) { result.add(new Pair<>(lineNumber, rt)); break; }
                    if (rt.equals(";") || rt.equals("{") || rt.equals("}")) break;
                    ri++;
                }
                continue;
            }

            // diğer atamalar için bu tokenlar geldiğinde eşitleme yapılan sağ tarafı da ekle.
            if (t.equals("+=") || t.equals("-=") || t.equals("*=") || t.equals("/=") ||
                    t.equals("%=") || t.equals("&=") || t.equals("|=") || t.equals("^=") ||
                    t.equals("<<=") || t.equals(">>=") || t.equals(">>>=")) {

                // soldaki ilk değişkeni yakala ve ekle
                int li = i - 1;
                while (li >= 0) {
                    String lt = tokens.get(li);
                    if (isVariable(lt)) { result.add(new Pair<>(lineNumber, lt)); break; }
                    if (lt.equals(";") || lt.equals("{") || lt.equals("}")) break;
                    li--;
                }
                // ve sağ tarafı okumaya başla
                reading = true;
                continue;
            }


            // eğer = varsa direkt sağ tarafı oku.
            if (t.equals("=")) {
                reading = true;
                continue;
            }

            //okuma yapılacak yerleri kapatmak için. eğer token bunlardaysan biriyse okuma dursun
            if (t.equals(";") || t.equals(")") || t.equals("{") || t.equals("}")) {
                reading = false;
            }

            if (reading && isVariable(t)) {
                result.add(new Pair<>(lineNumber, tokens.get(i)));
            }
        }

        if (line.contains("if") || line.contains("while") || line.contains("for")) {
            int open = line.indexOf("(");
            int close = line.lastIndexOf(")");
            if (open >= 0 && close > open) {
                String inside = line.substring(open + 1, close);

                if (line.contains("for")) {
                    // for(init; condition; update) - sadece condition kısmını alıp read vars buluyoruz
                    String[] parts = inside.split(";", -1);

                    String cond;
                    if (parts.length == 3) {
                        cond = parts[1];
                    } else {
                        cond = inside;
                    }

                    ArrayList<String> condTokens = extractTokensWithDots(cond);
                    for (int i = 0; i < condTokens.size(); i++) {
                        String ct = condTokens.get(i);
                        if (isVariable(ct)) {
                            result.add(new Pair<>(lineNumber, ct));
                        } else if (ct.equals("==") || ct.equals("!=")) {
                            // sol index - eşitliğin soluna bakıyoruz
                            int li = i - 1;
                            while (li >= 0) {
                                String lt = condTokens.get(li); //left token
                                if (isVariable(lt)) {
                                    result.add(new Pair<>(lineNumber, lt));
                                    break;
                                }
                                if (lt.equals(";") || lt.equals("{") || lt.equals("}")){
                                    break;
                                }
                                li--;
                            }
                            // sağ index
                            int ri = i + 1;
                            while (ri < condTokens.size()) {
                                String rt = condTokens.get(ri); //right token
                                if (isVariable(rt)) {
                                    result.add(new Pair<>(lineNumber, rt));
                                    break;
                                }
                                if (rt.equals(";") || rt.equals("{") || rt.equals("}")){
                                    break;
                                }
                                ri++;
                            }
                        }
                    }
                } else {
                    // if / while: tüm parantez içinden değişkenleri al
                    ArrayList<String> condTokens = extractTokensWithDots(inside);
                    for (int i = 0; i < condTokens.size(); i++) {
                        String ct = condTokens.get(i);
                        if (isVariable(ct)) {
                            result.add(new Pair<>(lineNumber, ct));
                        } else if (ct.equals("==") || ct.equals("!=")) {
                            // sol
                            int li = i - 1;
                            while (li >= 0) {
                                String lt = condTokens.get(li);
                                if (isVariable(lt)) { result.add(new Pair<>(lineNumber, lt)); break; }
                                if (lt.equals(";") || lt.equals("{") || lt.equals("}")) break;
                                li--;
                            }
                            // sağ
                            int ri = i + 1;
                            while (ri < condTokens.size()) {
                                String rt = condTokens.get(ri);
                                if (isVariable(rt)) { result.add(new Pair<>(lineNumber, rt)); break; }
                                if (rt.equals(";") || rt.equals("{") || rt.equals("}")) break;
                                ri++;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }


    //BU KISIM FARKLI
    //Bu methodda bazı farklılıklar var. teknik değil ama kod okunabilirliği açısından.
    private void generateDDGGraph() {
        ddgNodeLabels.clear();
        try {
            Graph graph = new Graph();
            Map<String, List<Integer>> writeMap = new HashMap<>();

            String[] lines = codeBlock.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                int lineNumber = i + 1;

                if (line.isEmpty() || line.startsWith("//")) continue;

                Set<Pair<Integer, String>> writtenVars = getWrittenVars(line, lineNumber);
                Set<Pair<Integer, String>> readVars = getReadVars(line, lineNumber);

                // yazılan değişkenler
                for (Pair<Integer, String> pair : writtenVars) {
                    String variable = pair.getValue();
                    int writeLine = pair.getKey();

                    if (!writeMap.containsKey(variable)) {
                        writeMap.put(variable, new ArrayList<>());
                    }
                    writeMap.get(variable).add(writeLine);

                    String nodeId = "Line " + writeLine + " (" + variable + ")";
                    ddgNodeLabels.put(nodeId, line);  // buraya satırın tamamını koyuyoruz
                }

                // okunan değişkenler
                for (Pair<Integer, String> pair : readVars) {
                    String variable = pair.getValue();
                    int readLine = pair.getKey();

                    if (writeMap.containsKey(variable)) {
                        List<Integer> writtenLines = writeMap.get(variable);
                        Integer lastWrite = null;
                        for (int writeLine : writtenLines) {
                            if (writeLine < readLine) {
                                if (lastWrite == null || writeLine > lastWrite) {
                                    lastWrite = writeLine;
                                }
                            }
                        }

                        if (lastWrite != null) {
                            String from = "Line " + lastWrite + " (" + variable + ")";
                            String to = "Line " + readLine + " (" + variable + ")";
                            graph.put(from, to);

                            ddgNodeLabels.put(to, line); // okunan satırın tamamı
                        }
                    }
                }
            }
            this.dataDependencyGraph = graph;
        } catch (Exception e) {
            ddgFailed = true;
            this.dataDependencyGraph = new Graph();
            System.err.println("Cannot generate DDG: " + e.getMessage());
        }
    }


//doubly linked list ve linked list ekle

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "null", "true", "false", "head", "tail", "pop", "push"
    );


    private boolean isVariable(String token) {
        return token.matches("[a-zA-Z_][a-zA-Z0-9_]*") &&
                !JAVA_KEYWORDS.contains(token);
    }

    public void toGraphviz(String directory) {
        try {
            if (abstractSyntaxTree != null) {
                abstractSyntaxTree.saveGraphviz(directory, "ast", "AST", astNodeLabels);
            }
            if (controlFlowGraph != null) {
                controlFlowGraph.saveGraphviz(directory, "cfg", "CFG", cfgNodeLabels);
            }
            if (dataDependencyGraph != null) {
                dataDependencyGraph.saveGraphviz(directory, "ddg", "DDG", ddgNodeLabels);
            }
            System.out.println("Graphviz çıktıları kaydedildi: " + directory);
        } catch (Exception e) {
            System.err.println("Graphviz çıktısı alınırken hata: " + e.getMessage());
        }
    }


    private static final Set<String> JAVA_TYPES = Set.of(
            "boolean", "bool", "byte", "char",  "double", "float", "int", "long", "short", "LinkedList" ,"ArrayList",
            "Stack", "Queue",  "DoublyList", "DoublyLinkedList", "Element", "Node", "DoublyNode", "Str"
    );

    private boolean isTypeToken(String t) {
        return t != null && JAVA_TYPES.contains(t);
    }

    private void addIfAbsent(Map<String, Variable> vars, String name, String type) {
        if (!vars.containsKey(name)) {
            vars.put(name, new Variable(type, name));
        }
    }

    public Set<Variable> getAllVariables() {
        //değişkenleri map olarak tutuyoruz. (string kısmında değişkenin adı olucak tekrarı önlemek için
        Map<String, Variable> vars = new LinkedHashMap<>();
        String[] lines = codeBlock.split("\n");
        boolean inBlockComment = false;

        for (String raw : lines) {
            String line = raw;

            // Yorum satırı varsa bu kısımda temizliyoruz.
            if (inBlockComment) {
                int end = line.indexOf("*/");
                if (end >= 0) {
                    line = line.substring(end + 2);
                    inBlockComment = false;
                } else {
                    continue;
                }
            }
            int bs = line.indexOf("/*");
            if (bs >= 0) {
                int be = line.indexOf("*/", bs + 2);
                if (be >= 0) {
                    line = line.substring(0, bs) + line.substring(be + 2);
                } else {
                    line = line.substring(0, bs);
                    inBlockComment = true;
                }
            }
            int sl = line.indexOf("//");
            if (sl >= 0) line = line.substring(0, sl);

            line = line.trim();
            if (line.isEmpty()) continue;

            // DİĞER CASE'ler  (farklı şekilde tanımlanan variable'lar için)
            // for(...) init kısmında tipli değişken )
            // for (int k = 1; k <= d; ++k) gibi. buradaki k'yı alıyor.

            if (line.contains("for(") || line.startsWith("for ")) {
                int o = line.indexOf('(');          // iç kısmını alacağımız başlangıç noktası
                int c = line.lastIndexOf(')');  // iç kısmını alacağımız bitiş noktası
                if (o >= 0 && c > o) {
                    String inside = line.substring(o + 1, c); // forun iç kısmı
                    String[] parts = inside.split(";", -1); // forun içini bölüp arraya ekledik.
                    String init = "";
                    if (parts.length > 0) {
                        init = parts[0].trim();
                    }
                    ArrayList<String> itok = extractTokensWithDots(init);
                    // for each olan for döngüsü için
                    if (init.contains(":") && itok.size() >= 2
                            && isTypeToken(itok.get(0)) && isVariable(itok.get(1))) {
                        addIfAbsent(vars, itok.get(1), itok.get(0));   // q : Queue
                    }
                    // normal for loop için olan for döngüsü için
                    else if (itok.size() >= 3 && isTypeToken(itok.get(0))
                            && isVariable(itok.get(1)) && "=".equals(itok.get(2))) {
                        addIfAbsent(vars, itok.get(1), itok.get(0));   // k : int
                    }
                }
            }

            // Array tanımlama  T [ ] name = ...
            ArrayList<String> tok = extractTokensWithDots(line);
            if (    tok.size() >= 5
                    && isTypeToken(tok.get(0))
                    && "[".equals(tok.get(1))
                    && "]".equals(tok.get(2))
                    && isVariable(tok.get(3))
                    && "=".equals(tok.get(4))) {
                addIfAbsent(vars, tok.get(3), tok.get(0) + "[]");
            }

            // Pointer tanımlama  T * name = ...
            if (tok.size() >= 4
                    && isTypeToken(tok.get(0))
                    && "*".equals(tok.get(1))
                    && isVariable(tok.get(2))
                    && "=".equals(tok.get(3))) {
                addIfAbsent(vars, tok.get(2), tok.get(0) + "*");
            }

            //  Klasik tanımlama  T name = ...
            if (tok.size() >= 3
                    && isTypeToken(tok.get(0))
                    && isVariable(tok.get(1))
                    && "=".equals(tok.get(2))) {
                addIfAbsent(vars, tok.get(1), tok.get(0));
            }

            // direkt tanımlama  T name ;
            if (tok.size() >= 3
                    && isTypeToken(tok.get(0))
                    && isVariable(tok.get(1))
                    && ";".equals(tok.get(2))) {
                addIfAbsent(vars, tok.get(1), tok.get(0));
            }

            // (sadece extractTokensWithDots + isVariable ile handle edebildiklerimiz)
            // - if/while/for koşullarındaki değişkenleri
            // - this, null, true/false, left.data, arr[i] vb. olan tokenları otomatik eleyerek toplar
            for (String t : tok) {
                if (isVariable(t) && !isTypeToken(t)) {   // <-- type token'larını at
                    addIfAbsent(vars, t, null);
                }
            }

        }

        if (this.code != null && this.code.getVariables() != null && !this.code.getVariables().isEmpty()) {
            Map<String, String> typeByName = new HashMap<>();
            for (Variable sv : this.code.getVariables()) {
                if (sv.getName() != null && sv.getType() != null) {
                    typeByName.putIfAbsent(sv.getName(), sv.getType());
                }
            }
            for (Variable v : vars.values()) {
                if (v.getType() == null) {
                    String t = typeByName.get(v.getName());
                    if (t != null) {
                        v.setType(t);
                    }
                }
            }
        }

        return new LinkedHashSet<>(vars.values());
    }

    public static class MatchResult {
        public Map<String, String> mapping;
        public List<Pair<Integer, Integer>> matchedLines;

        // Sadece eşleşen satırlar için constructor
        public MatchResult(List<Pair<Integer, Integer>> matchedLines) {
            this.mapping = Collections.emptyMap();
            this.matchedLines = matchedLines;
        }

        // Mapping ile birlikte eşleşen satırlar için constructor
        public MatchResult(Map<String, String> mapping, List<Pair<Integer, Integer>> matchedLines) {
            this.mapping = mapping;
            this.matchedLines = matchedLines;
        }
    }

    public MatchResult calculateBestMatch(Assessment other) {
        Set<Variable> studentVarsSet = getAllVariables();
        Set<Variable> refVarsSet = other.getAllVariables();

        // Variable nesnelerinden isimleri çıkar
        Set<String> studentVars = new HashSet<>();
        for (Variable var : studentVarsSet) {
            studentVars.add(var.getName());
        }

        Set<String> refVars = new HashSet<>();
        for (Variable var : refVarsSet) {
            refVars.add(var.getName());
        }

        // Bu assessment'ın kodunu satır satır ayır (her satır bir dizi elemanı olacak)
        String[] studentLines = codeBlock.split("\n");
        String[] refLines = other.codeBlock.split("\n");

        // değişkenleri isimden bağımsız eşit sayıp satırları doğrudan karşılaştır
        List<Pair<Integer, Integer>> matches = compareLines(studentLines, refLines, studentVars, refVars);
        return new MatchResult(Collections.emptyMap(), matches);
    }

    private List<Pair<Integer, Integer>> compareLines(String[] studentLines, String[] refLines, Set<String> studentVars, Set<String> refVars) {
        List<Pair<Integer, Integer>> matches = new ArrayList<>();
        boolean[] usedReferenceLine = new boolean[refLines.length];

        for (int studentLineIndex = 0; studentLineIndex < studentLines.length; studentLineIndex++) {
            String rawStudentLine = studentLines[studentLineIndex].trim();
            if (rawStudentLine.replaceAll("[\\s{}();,]", "").isEmpty()) continue;

            String normalizedStudentLine = rawStudentLine.replaceAll("\\s+", " ").trim();
            ArrayList<String> studentTokens = extractTokensWithDots(normalizedStudentLine);
            boolean studentOnlyElseHeader = !studentTokens.contains("if") &&
                    studentTokens.stream().allMatch(t -> t.equals("{") || t.equals("}") || t.equals("else"));
            if (studentOnlyElseHeader) continue;

            int bestRefIndex = -1;

            for (int refLineIndex = 0; refLineIndex < refLines.length; refLineIndex++) {
                if (usedReferenceLine[refLineIndex]) continue;

                String rawRefLine = refLines[refLineIndex].trim();
                if (rawRefLine.replaceAll("[\\s{}();,]", "").isEmpty()) continue;

                String normalizedRefLine = rawRefLine.replaceAll("\\s+", " ").trim();
                ArrayList<String> referenceTokens = extractTokensWithDots(normalizedRefLine);
                boolean referenceOnlyElseHeader = !referenceTokens.contains("if") &&
                        referenceTokens.stream().allMatch(t -> t.equals("{") || t.equals("}") || t.equals("else"));
                if (referenceOnlyElseHeader) continue;

                // Tam eşitlik kontrolü
                if (normalizedStudentLine.equals(normalizedRefLine)) {
                    bestRefIndex = refLineIndex;
                    break;
                }

                // Token bazlı eşleşme (değişken isimleri farklı olabilir)
                if (studentTokens.size() == referenceTokens.size()) {
                    boolean isMatch = true;
                    for (int k = 0; k < studentTokens.size(); k++) {
                        String ts = studentTokens.get(k);
                        String tr = referenceTokens.get(k);
                        boolean tsVar = isVariable(ts) || studentVars.contains(ts);
                        boolean trVar = isVariable(tr) || refVars.contains(tr);

                        if (tsVar && trVar) {
                            // Her ikisi de değişken - eşleşme
                            continue;
                        } else if (!tsVar && !trVar && ts.equals(tr)) {
                            // Her ikisi de değişken değil ve eşit - eşleşme
                            continue;
                        } else {
                            isMatch = false;
                            break;
                        }
                    }
                    if (isMatch) {
                        bestRefIndex = refLineIndex;
                        break;
                    }
                }
            }

            if (bestRefIndex >= 0) {
                usedReferenceLine[bestRefIndex] = true;
                matches.add(new Pair<>(studentLineIndex + 1, bestRefIndex + 1));
            }
        }

        return matches;
    }

    public int findBestMatch(RefCode refCode) {

        int bestMatchCount = 0;  // Şu ana kadar bulunan en fazla eşleşen satır sayısı
        int bestIndex = -1;      // En iyi eşleşen assessment'ın index'i (-1 = hiç eşleşme yok)


        for (int i = 0; i < refCode.getAssessments().size(); i++) {

            Assessment refAssessment = refCode.getAssessments().get(i);
            // Bu öğrenci assessment'ı ile i. referans assessment'ı arasında en iyi eşleşmeyi bul
            MatchResult result = this.calculateBestMatch(refAssessment);

            int matchCount = result.matchedLines.size();

            // Bu assessment ile daha fazla satır eşleştiyse, bunu yeni en iyi sonuç olarak kaydet
            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount;  // Yeni rekor eşleşme sayısı
                bestIndex = i;               // Bu assessment'ın index'i
            }
        }

        return bestIndex;
    }

    public void toGraphvizWithHighlights(String directory, List<Pair<Integer, Integer>> matchedLines) {
        try {
            // 🔹 matchMap'i listeye çeviriyoruz (sadece öğrenci satırlarını alıyoruz)
            List<Integer> highlightLines = new ArrayList<>();
            for (Pair<Integer, Integer> p : matchedLines) {
                if (p != null && p.getKey() != null) {
                    highlightLines.add(p.getKey());
                }
            }

            // 🔥 AST, CFG, DDG renklendirilmiş olarak kaydediliyor
            if (abstractSyntaxTree != null) {
                abstractSyntaxTree.saveGraphviz(directory, "ast_highlighted", "AST_Highlighted", astNodeLabels, highlightLines);
            }
            if (controlFlowGraph != null) {
                controlFlowGraph.saveGraphviz(directory, "cfg_highlighted", "CFG_Highlighted", cfgNodeLabels, highlightLines);
            }
            if (dataDependencyGraph != null) {
                dataDependencyGraph.saveGraphviz(directory, "ddg_highlighted", "DDG_Highlighted", ddgNodeLabels, highlightLines);
            }


        } catch (Exception e) {
            System.err.println("Graphviz error: " + e.getMessage());
            e.printStackTrace();
        }
    }



}