package com.retailai.controller;

import com.retailai.api.dto.*;
import com.retailai.service.ChatService;
import com.retailai.util.AnswerNormalizer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:8080"}, allowCredentials = "true")
public class ChatV2Controller {

    private final ChatService chat;

    public ChatV2Controller(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping(value = "/ask2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AskResponse> ask2(@RequestBody AskRequest req) {
        String question = firstNonBlank(req.getQ(), req.getMessage());
        String tenant = blankTo(req.getTenant_id(), "demo");

        if (question == null || question.isBlank()) {
            AskResponse err = new AskResponse();
            err.setType("error");
            err.setAnswer("Missing question. Send 'q' or 'message'.");
            err.setReply(err.getAnswer());
            return ResponseEntity.badRequest().body(err);
        }

        // Call your existing service (assumed to return a Map)
        Map<String,Object> raw = chat.answerQuestion(tenant, question.trim());
        if (raw == null) raw = new HashMap<>();

        // Build typed response
        AskResponse out = new AskResponse();

        // type
        Object type = raw.get("type");
        if (type instanceof String t && !t.isBlank()) out.setType(t);

        // answer (normalize/repair)
        String ans = string(raw.get("answer"));
        if (ans == null || ans.isBlank()) {
            ans = "I couldn't find an exact answer. Try rephrasing or upload FAQs to RAG.";
        }
        ans = AnswerNormalizer.normalize(ans);
        out.setAnswer(ans);
        out.setReply(ans); // voice/legacy clients

        // sources
        List<RagSource> srcs = new ArrayList<>();
        Object sObj = raw.get("sources");
        if (sObj instanceof List<?> sList) {
            for (Object el : sList) {
                if (el instanceof Map<?,?> m) {
                    RagSource rs = new RagSource();
                    Object v;
                    v = m.get("id"); if (v instanceof Number n) rs.setId(n.longValue());
                    v = m.get("tenant_id"); if (v != null) rs.setTenant_id(String.valueOf(v));
                    v = m.get("doc_type"); if (v != null) rs.setDoc_type(String.valueOf(v));
                    v = m.get("doc_id"); if (v != null) rs.setDoc_id(String.valueOf(v));
                    v = m.get("text"); if (v != null) rs.setText(AnswerNormalizer.normalize(String.valueOf(v)));
                    v = m.get("score"); if (v instanceof Number n) rs.setScore(n.doubleValue());
                    if (rs.getDoc_id() != null || rs.getText() != null) srcs.add(rs);
                }
            }
        }
        out.setSources(srcs);

        // suggestions
        List<RagSuggestion> suggs = new ArrayList<>();
        Object gObj = raw.get("suggestions");
        if (gObj instanceof List<?> gList) {
            for (Object el : gList) {
                if (el instanceof Map<?,?> m) {
                    RagSuggestion sg = new RagSuggestion();
                    Object v;
                    v = m.get("title"); if (v != null) sg.setTitle(String.valueOf(v));
                    v = m.get("type"); if (v != null) sg.setType(String.valueOf(v));
                    v = m.get("impact"); if (v != null) sg.setImpact(String.valueOf(v));
                    if (sg.getTitle() != null) suggs.add(sg);
                }
            }
        }
        out.setSuggestions(suggs);

        return ResponseEntity.ok(out);
    }

    // ---------- helpers ----------
    private static String string(Object o) { return (o == null) ? null : String.valueOf(o); }
    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }
    private static String blankTo(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
