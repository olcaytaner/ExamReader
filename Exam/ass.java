package org.example.Exam;

import org.example.Graph.Graph;
import org.example.Graph.LineConverter;
import org.example.Graph.LineType;
import org.example.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class ass {
    private final int grade;
    private final String feedback;
    private final boolean violation;
    private final String violationString;
    private final String codeBlock;
    private Graph abstractSyntaxTree;

    public ass(int grade, String feedback, boolean violation, String violationString, String codeBlock) {
        this.grade    = grade;
        this.feedback = feedback;
        this.violation = violation;
        this.violationString= violationString;
        this.codeBlock=codeBlock;

        generateASTGraph();
    }

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
            } else if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < line.length() && Character.isDigit(line.charAt(i))) {
                    sb.append(line.charAt(i));
                    i++;
                }
                tokens.add(sb.toString());
                //updated
            } else if(Character.isWhitespace(c)) {
                i++;
            } else{
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

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
                graph.put(body, cur + "-" + j);
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
}