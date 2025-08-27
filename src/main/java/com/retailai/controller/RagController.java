
package com.retailai.controller;

import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.retailai.service.RAGService;

@RestController
@RequestMapping("/api/rag2")
public class RagController {
    private final RAGService rag;

    public RagController(RAGService r) {
        this.rag = r;
    }

    @PostMapping("/index")
    public ResponseEntity<?> index(@RequestBody java.util.Map<String, Object> body) {
        String tenant = String.valueOf(body.getOrDefault("tenant_id", "demo"));
        String docType = String.valueOf(body.getOrDefault("doc_type", "other"));
        @SuppressWarnings("unchecked") java.util.List<java.util.Map<String, Object>> chunks = (java.util.List<java.util.Map<String, Object>>) body.get("chunks");
        int n = rag.indexBulk(tenant, docType, chunks);
        return ResponseEntity.ok(java.util.Map.of("status", "ok", "indexed", n));
    }

    @GetMapping("/search")
    public java.util.List<java.util.Map<String, Object>> search(@RequestParam String q, @RequestParam(defaultValue = "demo") String tenant_id, @RequestParam(defaultValue = "5") int k, @RequestParam(required = false) String doc_type) {
        return rag.search(tenant_id, q, k, doc_type);
    }
}
