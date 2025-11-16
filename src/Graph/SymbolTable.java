package Graph;

import java.util.*;

/**
 * SymbolTable class handles tokenization, parsing, and symbol management
 * for code analysis. This class contains utility methods used across
 * AST, CFG, and DDG generation.
 */
public class SymbolTable {

    // Java keywords to exclude from variable detection
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "null", "true", "false", "head", "tail",
            "pop", "push"
    );

    // Java type keywords
    private static final Set<String> JAVA_TYPES = Set.of(
            "boolean", "bool", "byte", "char", "double", "float", "int", "long", "short",
            "LinkedList", "ArrayList", "Stack", "Queue", "DoublyList", "DoublyLinkedList",
            "Element", "Node", "DoublyNode", "Str"
    );

    public static int getVariable(String line, int i, ArrayList<String> tokens) {
        StringBuilder sb = new StringBuilder();
        while (i < line.length() && (Character.isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
            sb.append(line.charAt(i));
            i++;
        }
        tokens.add(sb.toString());
        return i;
    }

    // küçük methodlara bölme yapılacak
    public static ArrayList<String> extractTokens(String line) {
        ArrayList<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);


            if (Character.isLetter(c)) {
                i = getVariable(line, i, tokens);
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
    public static String extractCondition(String line) {
        try {
            return line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
        } catch (Exception e) {
            return "UNKNOWN_CONDITION";
        }
    }

    // her satırın parantez içini kırpıp alıyor  - koşulu yani.
    public static String extractConditionType(String line) {
        try {
            return line.substring(line.indexOf("(") + 1, line.lastIndexOf(")")).trim();
        } catch (Exception e) {
            return "UNKNOWN_CONDITION";
        }
    }

    // satır numaralarıyla birlikte girilen stringin satırlarını bir hash map içine koyar.
    public static HashMap<Integer, String> createMapFromString(String input) {
        HashMap<Integer, String> map = new HashMap<>();
        String[] lines = input.split("\n");

        for (int i = 0; i < lines.length; i++) {
            map.put(i + 1, lines[i]);
        }

        return map;
    }
    /**
     * Determines the type of given line and returns it as a LineType.
     * @param line
     * @return types in that line as arraylist
     */
    public static ArrayList<LineType> getTypes(String line) {
        //updated
        ArrayList<LineType> types = new ArrayList<>();
        ArrayList<String> list= SymbolTable.extractTokens(line);
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
     * Converts code block to a list of line-type pairs
     */
    public static ArrayList<Pair<Integer, LineType>> convertFromCodeBlock(String codeBlock) {
        ArrayList<Pair<Integer, LineType>> current = new ArrayList<>();

        String[] lines = codeBlock.split("\n");

        for (int j = 0; j < lines.length; j++) {
            String line = lines[j].trim();

            if (line.isEmpty() || line.startsWith("/") || line.startsWith("*") || line.startsWith("*/")) {
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

            LineType type = lineTypes.get(i);
            if (type == LineType.FOR || type == LineType.IF || type == LineType.WHILE ||
                    type == LineType.ELSE || type == LineType.ELSE_IF) {

                boolean nextIsBrace = (j + 1 < lines.length) && lines[j + 1].trim().equals("{");
                boolean hasBraceOnLine = line.contains("{");
                boolean hasSemicolon = line.contains(";");

                if (nextIsBrace) {
                    j++;
                } else if (!hasBraceOnLine && !hasSemicolon) {
                    current.add(new Pair<>(lineNumber + 1, LineType.STATEMENT));
                    current.add(new Pair<>(lineNumber + 1, LineType.CLOSE));
                }
            }
        }

        if (checkBracketBalance(current)) {
            if (!current.isEmpty() && current.get(current.size() - 1).getValue() == LineType.CLOSE) {
                current.remove(current.size() - 1);
            }
        }

        return current;
    }


    public static boolean checkBracketBalance(ArrayList<Pair<Integer, LineType>> last) {
        Stack<LineType> stack = new Stack<>();

        for (Pair<Integer, LineType> pair : last) {
            if (pair.getValue().equals(LineType.CLOSE)) {
                if (stack.isEmpty()) {
                    return true;
                }
                stack.pop();
            } else if (pair.getValue().equals(LineType.ELSE) ||
                    pair.getValue().equals(LineType.ELSE_IF) ||
                    pair.getValue().equals(LineType.IF) ||
                    pair.getValue().equals(LineType.FOR) ||
                    pair.getValue().equals(LineType.WHILE)) {
                stack.add(pair.getValue());
            }
        }
        return !stack.isEmpty();
    }


    /**
     * Wrapper method for convertFromCodeBlock
     */
    public static ArrayList<ArrayList<Pair<Integer, LineType>>> convertFromString(String codeBlock) {
        ArrayList<ArrayList<Pair<Integer, LineType>>> result = new ArrayList<>();
        ArrayList<Pair<Integer, LineType>> singleBlock = convertFromCodeBlock(codeBlock);

        if (!singleBlock.isEmpty()) {
            result.add(singleBlock);
        }

        return result;
    }

    /**
     * Checks if a token is a valid variable name
     */
    public static boolean isVariable(String token) {
        return token.matches("[a-zA-Z_][a-zA-Z0-9_]*") && !JAVA_KEYWORDS.contains(token);
    }

    /**
     * Checks if a token is a Java type
     */
    public static boolean isTypeToken(String t) {
        return t != null && JAVA_TYPES.contains(t);
    }

    /**
     * Gets the set of Java keywords
     */
    public static Set<String> getJavaKeywords() {
        return JAVA_KEYWORDS;
    }

    /**
     * Gets the set of Java types
     */
    public static Set<String> getJavaTypes() {
        return JAVA_TYPES;
    }
}