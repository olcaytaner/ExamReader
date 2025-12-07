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
    private CFGGraph controlFlowGraph;
    private DDGGraph dataDependencyGraph;

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
    public CFGGraph getControlFlowGraph() {
        return controlFlowGraph;
    }
    public DDGGraph getDataDependencyGraph() {
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
        return abstractSyntaxTree.getNodeLabels();
    }

    //-----new cfg methods - short adapted version -----
    private void generateCFGGraph(){
        controlFlowGraph = new CFGGraph();
        controlFlowGraph.generateFromCodeBlock(codeBlock);
    }
    public boolean isCfgFailed() { return controlFlowGraph.isGenerationFailed(); }

    public Map<String, String> getCfgNodeLabels() {
        return controlFlowGraph.getNodeLabels();
    }

    //-----new ddg methods - short adapted version -----
    private void generateDDGGraph() {
        dataDependencyGraph = new DDGGraph();
        dataDependencyGraph.generateFromCodeBlock(codeBlock);
    }
    public boolean isDdgFailed() { return dataDependencyGraph.isGenerationFailed(); }
    public Map<String, String> getDdgNodeLabels() {
        return dataDependencyGraph.getNodeLabels();
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


    public static ArrayList<ArrayList<Graph>> generateGraphsContent(String codeBlock) throws FileNotFoundException {
        ArrayList<ArrayList<Graph>> graphs = new ArrayList<>();
        return generateGraphsContent(codeBlock);
    }


    public void toGraphviz(String directory) {
        try {
            if (abstractSyntaxTree != null) {
                abstractSyntaxTree.saveGraphviz(directory, "ast", "AST", abstractSyntaxTree.getNodeLabels());
            }
            if (controlFlowGraph != null) {
                controlFlowGraph.saveGraphviz(directory, "cfg", "CFG", controlFlowGraph.getNodeLabels());
            }
            if (dataDependencyGraph != null) {
                dataDependencyGraph.saveGraphviz(directory, "ddg", "DDG", dataDependencyGraph.getNodeLabels());
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
                controlFlowGraph.saveGraphviz(directory, "cfg_highlighted", "CFG_Highlighted", controlFlowGraph.getNodeLabels(), highlightLines);
            }
            if (dataDependencyGraph != null) {
                dataDependencyGraph.saveGraphviz(directory, "ddg_highlighted", "DDG_Highlighted", dataDependencyGraph.getNodeLabels(), highlightLines);
            }
        } catch (Exception e) {
            System.err.println("Graphviz error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}