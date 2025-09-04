package com.retailai.controller;

import com.retailai.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ChatController
 * - Accepts input as {"q": "..."} OR {"message": "..."} (for voice page)
 * - Always returns: { type, answer, reply, sources[], suggestions[] }
 * - Normalizes/repairs whitespace in answer to avoid "jammed" text
 * - Cleans malformed sources/suggestions into arrays of objects
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping(value = "/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, Object> payload) {

        String q = asString(payload.get("q"));
        if (q == null || q.isBlank()) q = asString(payload.get("message"));
        String tenant = asStringOrDefault(payload.get("tenant_id"), "demo");

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "type", "error",
                    "error", "Missing question. Send 'q' or 'message'."
            ));
        }

        Map<String, Object> raw = chat.answerQuestion(tenant, q.trim());
        if (raw == null) raw = new HashMap<>();

        // ---- Normalize answer ----
        String answer = asString(raw.get("answer"));
        if (answer == null || answer.isBlank()) {
            answer = "I couldn't find an exact answer. Try rephrasing or upload FAQs to RAG.";
        }
        // If upstream nuked whitespace, repair; otherwise gently normalize
        answer = (!hasWhitespace(answer)) ? smartSpace(answer) : normalizeSpaces(answer);

        // ---- Normalize optional fields ----
        String type = asStringOrDefault(raw.get("type"), "rag_response");
        List<Map<String, Object>> sources = normalizeObjList(
                raw.get("sources"),
                List.of("id", "tenant_id", "doc_type", "doc_id", "text", "score")
        );
        List<Map<String, Object>> suggestions = normalizeObjList(
                raw.get("suggestions"),
                List.of("title", "type", "impact")
        );

        // ---- Build clean response ----
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("type", type);
        resp.put("answer", answer);
        resp.put("reply", answer); // backward compatibility for UIs that expect {reply}
        resp.put("sources", sources);
        resp.put("suggestions", suggestions);

        return ResponseEntity.ok(resp);
    }

    // ---------------- helpers ----------------

    private static String asString(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private static String asStringOrDefault(Object o, String def) {
        String s = asString(o);
        return (s == null || s.isBlank()) ? def : s;
    }

    private static boolean hasWhitespace(String s) {
        return s != null && s.matches(".*\\s+.*");
    }

    /** Normalize spaces/newlines but keep readability (no markdown parsing here). */
    private static String normalizeSpaces(String s) {
        if (s == null) return null;
        return s.replace("\r\n", "\n")
                .replaceAll("[\\u00A0\\u2007\\u202F]", " ")   // exotic spaces -> regular
                .replaceAll("[ \\t]{2,}", " ")                // collapse multi-spaces
                .replaceAll("\\n{3,}", "\n\n")                // avoid huge blank blocks
                .trim();
    }

    /** Heuristically re-insert spaces when the string has none (common sanitizer bug). */
    private static String smartSpace(String s) {
        if (s == null) return null;
        s = s.replaceAll("([.,;:!?])(?!\\s)", "$1 ");         // add space after punctuation
        s = s.replaceAll("(?<=[a-z])(?=[A-Z])", " ");         // split lower→Upper (camel bumps)
        s = s.replaceAll("(?<=[A-Za-z])(?=\\d)", " ");        // letter→digit boundary
        s = s.replaceAll("(?<=\\d)(?=[A-Za-z])", " ");        // digit→letter boundary
        s = s.replaceAll("([*-])(?!\\s)", "$1 ");             // bullets "*point" -> "* point"
        return normalizeSpaces(s);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> normalizeObjList(Object raw, List<String> allowedKeys) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object el : list) {
                if (el instanceof Map<?, ?> m) {
                    Map<String, Object> clean = new LinkedHashMap<>();
                    for (String k : allowedKeys) {
                        if (m.containsKey(k)) clean.put(k, m.get(k));
                    }
                    if (!clean.isEmpty()) out.add(clean);
                }
                // Ignore stray strings like "I{...}" to avoid invalid JSON downstream
            }
        }
        return out;
        }
}
