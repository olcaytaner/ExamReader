package Exam;

import java.util.*;

public class Backtrack {

    boolean finished = false;

    private Map<String, String> bestMapping = null;
    private double bestScore = -1;

    private List<String> studentVars;
    private List<String> refVars;

    private String studentCode;
    private String refCode;

    //C'deki main gibi
    public void run(
            Set<String> studentVarSet,
            Set<String> refVarSet,
            String studentCode,
            String refCode
    ) {
        this.studentVars = new ArrayList<>(studentVarSet);
        this.refVars = new ArrayList<>(refVarSet);
        this.studentCode = studentCode;
        this.refCode = refCode;

        Map<String, String> a = new LinkedHashMap<>();

        bestMapping = null;
        bestScore = -1;
        finished = false;

        backtrack(a, 0);
    }

    // backtrack(int a[], int k, data input)
    void backtrack(Map<String, String> a, int k) {

        int n = studentVars.size();

        if (isSolution(a, k, n)) {
            processSolution(a, k);
        }

        // GÜVENLİK: k sınırı
        if (k >= studentVars.size()) return;

        List<String> c = new ArrayList<>();
        int[] ncandidates = new int[1];   // C'deki int *ncandidates karşılığı

        constructCandidates(a, k, n, c, ncandidates);

        String studentVar = studentVars.get(k);

        // Eğer kullanılabilir referans değişken yoksa, bu değişkeni eşleştirmeden atla
        if (ncandidates[0] == 0) {
            backtrack(a, k + 1);
            return;
        }

        // Sadece eşleştirme yaparak devam et (eşleştirme yapmadan devam et seçeneğini kaldırdık)
        // Bu performansı önemli ölçüde artırır
        for (int i = 0; i < ncandidates[0]; i++) {

            a.put(studentVar, c.get(i));   // a[k] = c[i]

            backtrack(a, k + 1);

            a.remove(studentVar);          // BACKTRACK

            if (finished) return;
        }
    }

    // is_a_solution(int a[], int k, int n)
    boolean isSolution(Map<String, String> a, int k, int n) {
        if (k == n) return true;
        
        // Eğer kullanılabilir referans değişken kalmadıysa, mevcut eşleştirme bir çözümdür
        Set<String> usedRef = new HashSet<>(a.values());
        int availableRefs = refVars.size() - usedRef.size();
        if (availableRefs == 0 && !a.isEmpty()) {
            return true;
        }
        
        // Eğer hiç eşleştirme yapılmadıysa çözüm değil
        if (a.isEmpty()) return false;
        
        return false;
    }

    // construct_candidates(int a[], int k, int n, int c[], int *ncandidates)
    void constructCandidates(
            Map<String, String> a,
            int k,
            int n,
            List<String> c,
            int[] ncandidates
    ) {
        c.clear();

        // a[] içindeki değerlerden kullanılan ref’leri çıkarıyoruz
        Set<String> usedRef = new HashSet<>(a.values());

        for (String ref : refVars) {
            if (!usedRef.contains(ref)) {
                c.add(ref);
            }
        }

        ncandidates[0] = c.size();   // *ncandidates = 2; karşılığı
    }

    // process_solution(int a[], int k)
    void processSolution(Map<String, String> a, int k) {

        String normalized = normalizeCodeWithMapping(studentCode, a);

        double score = calculateMatchScore(
                normalized.split("\n"),
                refCode.split("\n")
        );

        if (score > bestScore) {
            bestScore = score;
            bestMapping = new HashMap<>(a);
        }
    }

    // normalize code with mapping
    String normalizeCodeWithMapping(String code, Map<String, String> mapping) {

        String normalized = code;

        for (Map.Entry<String, String> e : mapping.entrySet()) {
            normalized = normalized.replaceAll(
                    "\\b" + e.getKey() + "\\b",
                    e.getValue()
            );
        }

        return normalized;
    }

    // calculate match score for only string
    double calculateMatchScore(String[] studentLines, String[] refLines) {

        int matchCount = 0;
        boolean[] used = new boolean[refLines.length];
        int nonEmptyRefLines = 0; // Boş olmayan referans satır sayısı

        // Önce boş olmayan referans satır sayısını hesapla
        for (String r : refLines) {
            if (r != null && !r.trim().isEmpty()) {
                nonEmptyRefLines++;
            }
        }

        // Sadece boş olmayan satırları eşleştir
        for (String s : studentLines) {
            if (s == null) continue;
            String sTrimmed = s.trim();
            if (sTrimmed.isEmpty()) continue; // Boş satırları atla

            for (int i = 0; i < refLines.length; i++) {
                if (used[i]) continue;

                String r = refLines[i];
                if (r == null) continue;
                String rTrimmed = r.trim();
                if (rTrimmed.isEmpty()) continue; // Boş satırları atla

                if (sTrimmed.equals(rTrimmed)) {
                    matchCount++;
                    used[i] = true;
                    break;
                }
            }
        }

        // Skoru sadece boş olmayan referans satırlarına göre hesapla
        if (nonEmptyRefLines == 0) return 0;
        return (100.0 * matchCount) / nonEmptyRefLines;
    }

    // get best mapping
    public Map<String, String> getBestMapping() {
        return bestMapping;
    }

    public double getBestScore() {
        return bestScore;
    }
}
