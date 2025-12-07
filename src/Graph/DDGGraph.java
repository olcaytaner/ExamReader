package Graph;

import java.util.*;

public class DDGGraph extends Graph {
    private Map<String, String> nodeLabels = new HashMap<>();
    private boolean generationFailed = false;

    public DDGGraph() {
        super();
    }
    public Map<String, String> getNodeLabels() {
        return nodeLabels;
    }
    public boolean isGenerationFailed() {
        return generationFailed;
    }

    @Override
    public DDGGraph clone() {
        DDGGraph cloned = new DDGGraph();
        for (String from : this.getGraph().keySet()) {
            for (String to : this.getGraph().get(from)) {
                cloned.put(from, to);
            }
        }
        cloned.nodeLabels.putAll(this.nodeLabels);
        cloned.generationFailed = this.generationFailed;
        return cloned;
    }


    public void generateFromCodeBlock(String codeBlock) {
        nodeLabels.clear();
        try {
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
                    this.nodeLabels.put(nodeId, line);  // buraya satırın tamamını koyuyoruz
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
                            this.put(from, to);
                            this.nodeLabels.put(to, line); // okunan satırın tamamı
                        }
                    }
                }
            }
        } catch (Exception e) {
            generationFailed = true;
            System.err.println("Cannot generate DDG: " + e.getMessage());
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
}
