package Exam;

import Graph.*;
import Graph.LineType;
import Graph.Pair;
import Graph.SymbolTable;
import java.io.FileNotFoundException;
import java.util.*;

public class Assessment {
    private final int grade;
    private final String feedback;
    private final boolean violation;
    private final String violationString;
    private final String codeBlock;
    private Code code;
    private ASTGraph abstractSyntaxTree;
    private Graph controlFlowGraph;
    private Graph dataDependencyGraph;

    private Map<String, String> astNodeLabels = new HashMap<>();
    private Map<String, String> cfgNodeLabels = new HashMap<>();
    private Map<String, String> ddgNodeLabels = new HashMap<>();

    private boolean cfgFailed = false;
    private boolean ddgFailed = false;

    public boolean isCfgFailed() { return cfgFailed; }
    public boolean isDdgFailed() { return ddgFailed; }

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

    public ASTGraph getAbstractSyntaxTree() {
        return abstractSyntaxTree;
    }
    public Graph getControlFlowGraph() {
        return controlFlowGraph;
    }

    public Graph getDataDependencyGraph() {
        return dataDependencyGraph;
    }

    public static ArrayList<Graph> generateGraphsFromStringContent(String input) {
        ArrayList<ASTGraph> astGraphs = ASTGraph.generateGraphsFromStringContent(input);
        return new ArrayList<>(astGraphs);
    }

    //-----new ast methods - short adapted version -----
    private void generateASTGraph() {
        abstractSyntaxTree = new ASTGraph();
        abstractSyntaxTree.generateFromCodeBlock(codeBlock);
    }
    public boolean isAstFailed() {
        return abstractSyntaxTree != null && abstractSyntaxTree.isGenerationFailed();
    }

    public Map<String, String> getAstNodeLabels() {
        return abstractSyntaxTree != null ? abstractSyntaxTree.getNodeLabels() : new HashMap<>();
    }

    //----------------------------------------------
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


    // type to string methodu. ve yardımcı methodlar
    // construct methodu her satıra karşılık gelen line type'ı graph yapısına ekler
    //******************************
    private static String constructType(String parent, Graph graph, String line, int j, LineType lineType) {
        String body = "[BODY] (Line " + j + ")";

        if (lineType.equals(LineType.IF) || lineType.equals(LineType.ELSE_IF)) {
            String conditionContent = SymbolTable.extractConditionType(line);
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
            String conditionContent = SymbolTable.extractConditionType(line);
            String head = "[" + lineType + "] (Line " + j + ")";
            String condition = "[CONDITION] " + conditionContent + " (Line " + j + ")";

            graph.put(parent, head);
            graph.put(head, condition);
            graph.put(head, body);
        }

        return body;
    }


    // solve fonksiyonundaki döngünün devam edip etmeyeceğini () belirlemek için kullanılır.
    // örneğin else iften sonra kapalı parantez arkasından else geliyorsa devam.

    // ArrayList<Pair<Integer, LineType>> nesnesinin methodu
    private static boolean conditionType(int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() &&
                    (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    // ağaç yapısını oluşturan kısım.

    // ArrayList<Pair<Integer, LineType>> nesnesinin ya da Graphın methodu
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


    // graph'a eklenicek tekrar
    private static String construct(String parent,
                                    Graph graph,
                                    String lineContent,
                                    int lineNo,
                                    LineType lineType) {

        // Artık BODY yok – head'in kendisi gövde gibi davranacak
        String head = lineContent.trim() + " (Line " + lineNo + ")";

        if (lineType == LineType.IF || lineType == LineType.ELSE_IF) {
            String cond = "[CONDITION] " + SymbolTable.extractCondition(lineContent) + " (Line " + lineNo + ")";
            graph.put(parent, head);
            graph.put(head, cond);                // sadece CONDITION çocuğu
            return head;                          // <-- solve()’a geri head dönüyoruz
        }
        if (lineType == LineType.ELSE) {
            graph.put(parent, head);
            return head;                          // ELSE’in altına doğrudan statement’lar bağlanacak
        }
        if (lineType == LineType.WHILE || lineType == LineType.FOR) {
            String cond = "[CONDITION] " + SymbolTable.extractCondition(lineContent) + " (Line " + lineNo + ")";
            graph.put(parent, head);
            graph.put(head, cond);
            return head;
        }
        return head;      // teorik olarak buraya düşmez ama derleyici mutlu olur
    }



    // solve fonksiyonundaki döngünün devam edip etmeyeceğini () belirlemek için kullanılır.
    // ArrayList<Pair<Integer, LineType>> nesnesinin methodu olacak
    private static boolean condition(int j, ArrayList<Pair<Integer, LineType>> lines) {
        if (lines.get(j).getValue().equals(LineType.CLOSE)) {
            return j + 1 != lines.size() &&
                    (lines.get(j + 1).getValue().equals(LineType.ELSE_IF) || lines.get(j + 1).getValue().equals(LineType.ELSE));
        }
        return true;
    }


    // ArrayList<Pair<Integer, LineType>> nesnesinin methodu olacak
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
            ArrayList<Pair<Integer, LineType>> block = SymbolTable.convertFromCodeBlock(codeBlock);

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
        ArrayList<String> tokens = SymbolTable.extractTokens(line);

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.equals("=") && i > 0) {
                String left = tokens.get(i - 1);
                if (SymbolTable.isVariable(left)) {
                    result.add(new Pair<>(lineNumber, left));
                }
            }
        }
        return result;
    }


    private Set<Pair<Integer, String>> getReadVars(String line, int lineNumber) {
        Set<Pair<Integer, String>> result = new HashSet<>();
        ArrayList<String> tokens = SymbolTable.extractTokensWithDots(line);

        boolean reading = false;

        for (int i = 0; i< tokens.size();i++) {

            String t = tokens.get(i);
            // ifin içinde == veya != varsa hem sağındaki hem solundaki değişkeni ekle.
            if (t.equals("==") || t.equals("!=")) {
                // soldaki ilk değişken
                int li = i - 1;
                while (li >= 0) {
                    String lt = tokens.get(li);
                    if (SymbolTable.isVariable(lt)) { result.add(new Pair<>(lineNumber, lt)); break; }
                    if (lt.equals(";") || lt.equals("{") || lt.equals("}")) break;
                    li--;
                }
                // sağdaki ilk değişken
                int ri = i + 1;
                while (ri < tokens.size()) {
                    String rt = tokens.get(ri);
                    if (SymbolTable.isVariable(rt)) { result.add(new Pair<>(lineNumber, rt)); break; }
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
                    if (SymbolTable.isVariable(lt)) { result.add(new Pair<>(lineNumber, lt)); break; }
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

            if (reading && SymbolTable.isVariable(t)) {
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

                    ArrayList<String> condTokens = SymbolTable.extractTokensWithDots(cond);
                    for (int i = 0; i < condTokens.size(); i++) {
                        String ct = condTokens.get(i);
                        if (SymbolTable.isVariable(ct)) {
                            result.add(new Pair<>(lineNumber, ct));
                        } else if (ct.equals("==") || ct.equals("!=")) {
                            // sol index - eşitliğin soluna bakıyoruz
                            int li = i - 1;
                            while (li >= 0) {
                                String lt = condTokens.get(li); //left token
                                if (SymbolTable.isVariable(lt)) {
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
                                if (SymbolTable.isVariable(rt)) {
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
                    ArrayList<String> condTokens = SymbolTable.extractTokensWithDots(inside);
                    for (int i = 0; i < condTokens.size(); i++) {
                        String ct = condTokens.get(i);
                        if (SymbolTable.isVariable(ct)) {
                            result.add(new Pair<>(lineNumber, ct));
                        } else if (ct.equals("==") || ct.equals("!=")) {
                            // sol
                            int li = i - 1;
                            while (li >= 0) {
                                String lt = condTokens.get(li);
                                if (SymbolTable.isVariable(lt)) { result.add(new Pair<>(lineNumber, lt)); break; }
                                if (lt.equals(";") || lt.equals("{") || lt.equals("}")) break;
                                li--;
                            }
                            // sağ
                            int ri = i + 1;
                            while (ri < condTokens.size()) {
                                String rt = condTokens.get(ri);
                                if (SymbolTable.isVariable(rt)) { result.add(new Pair<>(lineNumber, rt)); break; }
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

    public void toGraphviz(String directory) {
        try {
            if (abstractSyntaxTree != null) {
                abstractSyntaxTree.saveGraphviz(directory, "ast", "AST", abstractSyntaxTree.getNodeLabels());
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
                    ArrayList<String> itok = SymbolTable.extractTokensWithDots(init);
                    // for each olan for döngüsü için
                    if (init.contains(":") && itok.size() >= 2
                            && SymbolTable.isTypeToken(itok.get(0)) && SymbolTable.isVariable(itok.get(1))) {
                        addIfAbsent(vars, itok.get(1), itok.get(0));   // q : Queue
                    }
                    // klasik for init: Type a=0, b=1, c[];
                    else if (!itok.isEmpty() && SymbolTable.isTypeToken(itok.get(0))) {
                        String baseType = itok.get(0);
                        int i = 1;

                        String typeSuffix = "";
                        if (i + 1 < itok.size() && "[".equals(itok.get(i)) && "]".equals(itok.get(i + 1))) {
                            typeSuffix = "[]";
                            i += 2;
                        }

                        while (i < itok.size()) {
                            if (i < itok.size() && SymbolTable.isVariable(itok.get(i))) {
                                String name = itok.get(i); i++;

                                String localSuffix = typeSuffix;
                                while (i + 1 < itok.size() && "[".equals(itok.get(i)) && "]".equals(itok.get(i + 1))) {
                                    localSuffix += "[]"; i += 2;
                                }

                                addIfAbsent(vars, name, baseType + localSuffix);

                                if (i < itok.size() && "=".equals(itok.get(i))) {
                                    i++;
                                    int depth = 0;
                                    while (i < itok.size()) {
                                        String t = itok.get(i);
                                        if ("(".equals(t) || "{".equals(t) || "[".equals(t)) depth++;
                                        else if (")".equals(t) || "}".equals(t) || "]".equals(t)) depth--;
                                        if (depth == 0 && ",".equals(t)) break; // init kısmında ';' yok
                                        i++;
                                    }
                                }
                                if (i < itok.size() && ",".equals(itok.get(i))) { i++; continue; }
                                break; // init biter
                            } else break;
                        }
                    }

                    /*
                    // normal for loop için olan for döngüsü için
                    else if (itok.size() >= 3 && isTypeToken(itok.get(0))
                            && isVariable(itok.get(1)) && "=".equals(itok.get(2))) {
                        addIfAbsent(vars, itok.get(1), itok.get(0));   // k : int
                    }

                     */
                }
            }

            // Array tanımlama  T [ ] name = ...
            ArrayList<String> tok = SymbolTable.extractTokensWithDots(line);

            // Çoklu bildirim desteği (Type a=0, b, c[] = foo();) ---
            if (!tok.isEmpty() && SymbolTable.isTypeToken(tok.get(0))) {
                String baseType = tok.get(0);
                int i = 1;

                // Tip sonrası [] (örn. int [] a, b;)
                String typeSuffix = "";
                if (i + 1 < tok.size() && "[".equals(tok.get(i)) && "]".equals(tok.get(i + 1))) {
                    typeSuffix = "[]";
                    i += 2;
                }

                while (i < tok.size()) {
                    // Ad
                    if (i < tok.size() && SymbolTable.isVariable(tok.get(i))) {
                        String name = tok.get(i);
                        i++;

                        // Ad sonrası []'ler (a[], a[][])
                        String localSuffix = typeSuffix;
                        while (i + 1 < tok.size() && "[".equals(tok.get(i)) && "]".equals(tok.get(i + 1))) {
                            localSuffix += "[]";
                            i += 2;
                        }

                        addIfAbsent(vars, name, baseType + localSuffix);

                        // initializer'ı atla (= ... , ;  gelene kadar, parantez derinliği korunur)
                        if (i < tok.size() && "=".equals(tok.get(i))) {
                            i++;
                            int depth = 0;
                            while (i < tok.size()) {
                                String t = tok.get(i);
                                if ("(".equals(t) || "{".equals(t) || "[".equals(t)) depth++;
                                else if (")".equals(t) || "}".equals(t) || "]".equals(t)) depth--;
                                if (depth == 0 && (",".equals(t) || ";".equals(t))) break;
                                i++;
                            }
                        }

                        // sonraki bildirim
                        if (i < tok.size() && ",".equals(tok.get(i))) { i++; continue; }
                        // ';' veya başka bir şey → tipli bildirim bloğu biter
                        break;
                    } else {
                        break; // beklenmedik token
                    }
                }
            }

            if (    tok.size() >= 5
                    && SymbolTable.isTypeToken(tok.get(0))
                    && "[".equals(tok.get(1))
                    && "]".equals(tok.get(2))
                    && SymbolTable.isVariable(tok.get(3))
                    && "=".equals(tok.get(4))) {
                addIfAbsent(vars, tok.get(3), tok.get(0) + "[]");
            }

            // Pointer tanımlama  T * name = ...
            if (tok.size() >= 4
                    && SymbolTable.isTypeToken(tok.get(0))
                    && "*".equals(tok.get(1))
                    && SymbolTable.isVariable(tok.get(2))
                    && "=".equals(tok.get(3))) {
                addIfAbsent(vars, tok.get(2), tok.get(0) + "*");
            }

            /*
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
            */

            // (sadece extractTokensWithDots + isVariable ile handle edebildiklerimiz)
            // - if/while/for koşullarındaki değişkenleri
            // - this, null, true/false, left.data, arr[i] vb. olan tokenları otomatik eleyerek toplar
            for (String t : tok) {
                if (SymbolTable.isVariable(t) && !SymbolTable.isTypeToken(t)) {   // <-- type token'larını at
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

    public List<Pair<Integer, Integer>> calculateBestMatch(Assessment other) {
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
        return matches;
    }

    private ArrayList<String> filterTokens(ArrayList<String> tokens) {
        ArrayList<String> normalized = new ArrayList<>();

        for (String token : tokens) {
            if (token == null) continue;
            token = token.trim();
            if (token.isEmpty()) continue;

            // "this" tamamen sil
            if (token.equals("this")) continue;

            // "this." ile başlayanlar -> sadece nokta sonrasını al
            if (token.startsWith("this.")) {
                token = token.substring(5);
            }
            // a[i], a[index] gibi array erişimlerini parçala
            int lb = token.indexOf('[');
            int rb = token.lastIndexOf(']');
            if (lb > 0 && rb == token.length() - 1 && rb > lb + 1) {
                String base   = token.substring(0, lb);      // "a"
                String inside = token.substring(lb + 1, rb); // "i" veya "index"

                if (!base.isEmpty()) {
                    // Son eklenen token zaten 'a' ise tekrar ekleme
                    if (normalized.isEmpty() || !normalized.get(normalized.size() - 1).equals(base)) {
                        normalized.add(base);
                    }
                }
                if (!inside.isEmpty()) {
                    normalized.add(inside);    // i / index
                }
                // Köşeli parantezleri hiç eklemiyoruz
                continue;
            }


            // current.getNext gibi yapıları tek token olarak normalize et
            if (token.contains(".get")) {
                int idx = token.indexOf(".get");
                String before = token.substring(0, idx);
                String after = token.substring(idx + 4);

                if (!after.isEmpty()) {
                    String normalizedAfter = after;
                    if (normalizedAfter.startsWith("get")
                            && normalizedAfter.length() > 3
                            && Character.isUpperCase(normalizedAfter.charAt(3))) {
                        normalizedAfter = Character.toLowerCase(normalizedAfter.charAt(3)) + normalizedAfter.substring(4);
                    } else if (!normalizedAfter.isEmpty() && Character.isUpperCase(normalizedAfter.charAt(0))) {
                        normalizedAfter = Character.toLowerCase(normalizedAfter.charAt(0)) + normalizedAfter.substring(1);
                    }

                    if (!before.isEmpty()) {
                        token = before + "." + normalizedAfter;
                    } else {
                        token = normalizedAfter;
                    }
                }
            }

            // getNext, getData gibi tokenları normalize et
            if (token.startsWith("get") && token.length() > 3 && Character.isUpperCase(token.charAt(3))) {
                token = Character.toLowerCase(token.charAt(3)) + token.substring(4);
            }

            // Eğer token sadece parantezse geç
            if (token.equals("(") || token.equals(")") || token.equals("{") || token.equals("}")) continue;

            if (!token.isEmpty()) {
                normalized.add(token);
            }
        }

        return normalized;
    }

    private List<Pair<Integer, Integer>> compareLines(String[] studentLines, String[] refLines, Set<String> studentVars, Set<String> refVars) {
        List<Pair<Integer, Integer>> matches = new ArrayList<>();
        boolean[] usedReferenceLine = new boolean[refLines.length]; //true olursa, o ref satırı tekrar kullanılmıyor (1 ref satırı = en fazla 1 eşleşme)

        for (int stuIndex = 0; stuIndex < studentLines.length; stuIndex++) {
            String rawS = studentLines[stuIndex]; //bu satırın ham hali
            int sl = rawS.indexOf("//"); //bu string içinde "//" kaçıncı index’te başlıyor onu döner
            if (sl >= 0) { //"//" yoksa, indexOf -1 döner
                rawS = rawS.substring(0, sl); //"//" yoksa, substring ile "//" dan önceki kısmı alır
            }
            rawS = rawS.trim();
            if (rawS.replaceAll("[\\s{}();,]", "").isEmpty()) continue;//boş / sadece süslü parantez satırlarını atla

            ArrayList<String> sTokens = filterTokens(SymbolTable.extractTokensWithDots(rawS));//["DoublyNode", "tmp", "=", "DoublyNode", "head", ";"] vb dönüyor
            if (sTokens.isEmpty()) continue;

            boolean sElse = !sTokens.contains("if") && 
                    sTokens.stream().allMatch(t -> t.equals("{") || t.equals("}") || t.equals("else"));
            if (sElse) continue; // if içermiyor ve tüm satır sadece {, } veya else ise atla

            int bestRef = -1; //eşleşme bulduğu ref indexini tutacak -1 ise eşleşme yok

            for (int refIndex = 0; refIndex < refLines.length; refIndex++) {
                if (usedReferenceLine[refIndex]) continue;//daha önce kullanılmamış ref satırlanı deneyeceğiz

                String rawR = refLines[refIndex];
                int rl = rawR.indexOf("//"); //yorum satırına kadar olan kısmı al
                if (rl >= 0) {
                    rawR = rawR.substring(0, rl);
                }
                rawR = rawR.trim();
                if (rawR.replaceAll("[\\s{}();,]", "").isEmpty()) continue; //satırlar sadece whitespace karakterlerden oluşuyorsa atla

                ArrayList<String> rTokens = filterTokens(SymbolTable.extractTokensWithDots(rawR));
                if (rTokens.isEmpty()) continue;

                boolean rElse = !rTokens.contains("if") &&
                        rTokens.stream().allMatch(t -> t.equals("{") || t.equals("}") || t.equals("else"));
                if (rElse) continue;

                boolean isMatch = true;
                int i = 0, j = 0;

                while (i < sTokens.size() && j < rTokens.size()) {
                    String ts = sTokens.get(i);
                    String tr = rTokens.get(j);

                    boolean tsVar = SymbolTable.isVariable(ts) || studentVars.contains(ts); //int "="  false döner
                    boolean trVar = SymbolTable.isVariable(tr) || refVars.contains(tr); //variable varsa true döner

                    if (tsVar && trVar) //her iki taraftaki token da değişkense, isimleri ne olursa olsun eşleşiyor
                    {
                        i++; j++;
                        continue;
                    }
                    if (!tsVar && !trVar && ts.equals(tr)) // ikisinin de değişken olmayan ve aynı olduğu 
                    { 
                        i++; j++; 
                        continue;
                    }
                    if (!tsVar && !trVar && ts.contains(".") && tr.contains(".")) //current.getNext → filter sonrası current.next’e dönüyor (suffixler aynıysa eşleşiyor)
                    {
                        int tstuIndex = ts.lastIndexOf('.'); //son "." indexi
                        int trefIndex = tr.lastIndexOf('.');
                        if (tstuIndex >= 0 && trefIndex >= 0) { //"." varsa iki kodda da
                            String tsSuffix = ts.substring(tstuIndex + 1); //ts = "tmp.next" → tsSuffix = "next"
                            String trSuffix = tr.substring(trefIndex + 1);
                            if (!tsSuffix.isEmpty() && tsSuffix.equals(trSuffix))
                            {
                                i++; j++;
                                continue;//eşleşmiş saydık diğer tokena geçtik
                            }
                        }
                    }

                    isMatch = false; // eşleşme olmadı 
                    break;
                }

                if (isMatch && (i != sTokens.size() || j != rTokens.size())) { //ref ya da stu da tokenların biri biterse
                    isMatch = false;
                }

                if (isMatch) {
                    bestRef = refIndex; //bu öğrenci satırı için hangi ref satırının seçildiğini tutuyor
                    break;
                }
            }

            if (bestRef >= 0) { //bestRef 0 veya daha büyük (ref satır index’i)  eşleşen satırın indexi
                usedReferenceLine[bestRef] = true;//bir ref satırını birden fazla öğrenci satırı ile eşleştirmiyor
                matches.add(new Pair<>(stuIndex + 1, bestRef + 1)); // +1  ler satır sayılaraı 0 dan başlaması için
            }
        }

        return matches;
    }

    public void toGraphvizWithHighlights(String directory, List<Pair<Integer, Integer>> matchedLines) {
        try {
            // matchMap'i listeye çeviriyoruz (sadece öğrenci satırlarını alıyoruz)
            List<Integer> highlightLines = new ArrayList<>();
            for (Pair<Integer, Integer> p : matchedLines) {
                if (p != null && p.getKey() != null) {
                    highlightLines.add(p.getKey());
                }
            }

            if (abstractSyntaxTree != null) {
                abstractSyntaxTree.saveGraphviz(directory, "ast_highlighted", "AST_Highlighted", abstractSyntaxTree.getNodeLabels(), highlightLines);
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