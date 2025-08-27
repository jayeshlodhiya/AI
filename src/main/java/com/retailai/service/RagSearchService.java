package com.retailai.service;

import com.retailai.model.RagChunkNew;
import com.retailai.repo.RagChunkRepoNew;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hybrid RAG search:
 *  1) Metadata/exact (phones, emails, invoice IDs) -> precise
 *  2) Keyword/FTS (DB full-text or LIKE with trigram) -> robust for numbers, IDs, names
 *  3) Semantic (optional VectorSearchClient) -> concept queries
 *
 * Returns a merged, deduped list of snippets for the UI.
 */
@Service
public class RagSearchService {

    /** Optional semantic client. Provide your own implementation or omit entirely. */
    public interface VectorSearchClient {
        List<RagChunkNew> vectorTopK(String query, String tenantId, String docType, int k);
    }

    private final RagChunkRepoNew repo;
    private final Optional<VectorSearchClient> vecOpt;

    // --- Heuristics/patterns for “metadata” needles ---
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern INVOICE = Pattern.compile(
            "(?:INV[-/\\s]?\\d+|#\\d{3,}|Invoice\\s*No\\.?\\s*\\w+)",
            Pattern.CASE_INSENSITIVE
    );
    // We treat phones as any 10–13 digits (India-friendly), we’ll normalize to last10 too
    private static final Pattern DIGITS = Pattern.compile("\\d{8,}"); // broad; we’ll prune below

    @Autowired
    public RagSearchService(RagChunkRepoNew repo,
                            @Autowired(required = false) VectorSearchClient vec) {
        this.repo = repo;
        this.vecOpt = Optional.ofNullable(vec);
    }

    /**
     * Main search entrypoint used by your controller.
     *
     * @param q        User query
     * @param tenantId Optional tenant
     * @param type     Optional type (unused in this service, but kept for signature symmetry)
     * @param docType  Optional docType filter
     * @param docId    Optional docId (if present, short-circuit to that doc)
     * @param limit    Max results (1..50)
     */
    public Map<String, Object> search(String q,
                                      String tenantId,
                                      String type,
                                      String docType,
                                      String docId,
                                      int limit) {

        int k = Math.max(1, Math.min(limit, 50));
        final String query = (q == null ? "" : q).trim();

        // If docId is provided, give chunks of that doc (ordered) — easy win for UX.
        if (docId != null && !docId.isBlank()) {
            List<RagChunkNew> fromDoc = topChunksByDoc(tenantId, docId, k);
            return wrapResults(query, fromDoc, "docId");
        }

        // 1) extract “needles” from the query (phones/emails/invoice IDs)
        Set<String> needles = extractNeedles(query);

        // 2) metadata hits (fast & precise)
        List<RagChunkNew> meta = List.of();
        if (!needles.isEmpty()) {
            // Try each needle; stop early if we already have enough
            List<RagChunkNew> collected = new ArrayList<>();
            for (String needle : needles) {
                List<RagChunkNew> hits = safeMetaHits(needle, tenantId, docType, k);
                for (RagChunkNew c : hits) {
                    if (collected.stream().noneMatch(e -> Objects.equals(e.getId(), c.getId()))) {
                        collected.add(c);
                        if (collected.size() >= k) break;
                    }
                }
                if (collected.size() >= k) break;
            }
            meta = collected;
        }

        // 3) keyword/FTS path
        List<RagChunkNew> kw = safeFtsOrLike(query, tenantId, docType, k);

        // 4) semantic (optional)
        List<RagChunkNew> sem = vecOpt
                .map(vec -> safeVectorTopK(vec, query, tenantId, docType, k))
                .orElse(List.of());

        // 5) merge & dedupe with priority: meta > kw > semantic
        LinkedHashMap<Long, RagChunkNew> merged = new LinkedHashMap<>();
        meta.forEach(c -> merged.putIfAbsent(c.getId(), c));
        kw.forEach(c -> merged.putIfAbsent(c.getId(), c));
        sem.forEach(c -> merged.putIfAbsent(c.getId(), c));

        List<RagChunkNew> finalList = new ArrayList<>(merged.values());
        if (finalList.size() > k) finalList = finalList.subList(0, k);

        return wrapResults(query, finalList, !needles.isEmpty() ? "metadata" : "text");
    }

    // ---------- Helpers ----------

    private List<RagChunkNew> safeMetaHits(String needle, String tenantId, String docType, int k) {
        try {
            return repo.metaHits(needle, tenantId, docType, k);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RagChunkNew> safeFtsOrLike(String q, String tenantId, String docType, int k) {
        // Prefer FTS if your repo supports it; fallback to LIKE if not
        try {
            List<RagChunkNew> fts = repo.ftsEntities(q, tenantId, docType, k);
            if (!fts.isEmpty()) return fts;
        } catch (Exception ignore) { /* fall back to LIKE */ }
        try {
            return repo.like(q, tenantId, docType, k);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<RagChunkNew> safeVectorTopK(VectorSearchClient vec,
                                             String q, String tenantId, String docType, int k) {
        try {
            return vec.vectorTopK(q, tenantId, docType, k);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * If the UI asks for a specific doc’s chunks, honor that quickly.
     * Assumes you added this query in your repository:
     *   List<RagChunkNew> findTop10ByTenantIdAndDocIdOrderByChunkIndexAsc(String tenantId, String docId);
     */
    private List<RagChunkNew> topChunksByDoc(String tenantId, String docId, int k) {
        try {
            List<RagChunkNew> chunks = repo.findTop10ByTenantIdAndDocIdOrderByChunkIndexAsc(tenantId, docId);
            if (chunks.size() > k) return chunks.subList(0, k);
            return chunks;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> wrapResults(String query, List<RagChunkNew> list, String mode) {
        List<Map<String, Object>> results = new ArrayList<>(list.size());
        for (RagChunkNew c : list) {
            results.add(Map.of(
                    "docId", nullToEmpty(c.getDocId()),
                    "chunkId", c.getId(),
                    "docType", nullToEmpty(c.getDocType()),
                    "snippet", makeSnippet(c.getContent(), query),
                    "chunkIndex", c.getChunkIndex()
            ));
        }
        return Map.of(
                "query", query,
                "mode", mode,       // “metadata”, “text”, or “docId”
                "count", results.size(),
                "results", results
        );
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    /**
     * Extract metadata-like “needles” from user query, normalized to forms we index:
     *  - emails (as-is)
     *  - invoice IDs (e.g., INV-123, #12345)
     *  - phone numbers: digits-only + last-10
     */
    private Set<String> extractNeedles(String q) {
        Set<String> out = new LinkedHashSet<>();
        if (q == null || q.isBlank()) return out;

        // Emails
        Matcher e = EMAIL.matcher(q);
        while (e.find()) out.add(e.group().toLowerCase());

        // Invoice-like tokens
        Matcher inv = INVOICE.matcher(q);
        while (inv.find()) out.add(inv.group().toUpperCase(Locale.ROOT));

        // Digits -> phone-ish candidates or invoice numbers
        String digitsOnly = q.replaceAll("\\D", "");
        if (digitsOnly.length() >= 8) {
            // full
            out.add(digitsOnly);
            // last 10 (often phone)
            if (digitsOnly.length() >= 10) {
                out.add(digitsOnly.substring(digitsOnly.length() - 10));
            }
        } else {
            // also scan tokens for mixed forms like "INV-000123"
            Matcher dmatcher = DIGITS.matcher(q);
            while (dmatcher.find()) {
                String d = dmatcher.group();
                out.add(d);
                if (d.length() >= 10) out.add(d.substring(d.length() - 10));
            }
        }
        return out;
    }

    /**
     * Tiny snippet maker with naive highlighting for the first keyword in q.
     * Keeps things simple (no HTML sanitizer here—ensure your frontend handles it accordingly).
     */
    private String makeSnippet(String content, String q) {
        if (content == null || content.isBlank()) return "";
        if (q == null || q.isBlank()) {
            return content.length() > 300 ? content.substring(0, 300) + "…" : content;
        }

        String needle = firstMeaningfulToken(q);
        if (needle.isEmpty()) {
            return content.length() > 300 ? content.substring(0, 300) + "…" : content;
        }
        String lc = content.toLowerCase(Locale.ROOT);
        int idx = lc.indexOf(needle.toLowerCase(Locale.ROOT));
        if (idx < 0) idx = 0;
        int start = Math.max(0, idx - 80);
        int end = Math.min(content.length(), start + 300);
        String slice = content.substring(start, end);
        return slice.replaceAll("(?i)(" + Pattern.quote(needle) + ")", "<mark>$1</mark>")
                + (end < content.length() ? "…" : "");
    }

    private String firstMeaningfulToken(String q) {
        if (q == null) return "";
        for (String t : q.trim().split("\\s+")) {
            if (t.length() >= 2) return t;
        }
        return "";
    }
}
