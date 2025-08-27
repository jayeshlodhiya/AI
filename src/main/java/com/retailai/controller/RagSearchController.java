package com.retailai.controller;

import com.retailai.service.RagSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagSearchController {

    private final RagSearchService service;

    public RagSearchController(RagSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam("q") String q,
            @RequestParam(value = "tenantId", required = false) String tenantId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "docId", required = false) String docId,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit
    ) {
        var results = service.search(q, tenantId, type, docType, docId, limit);
        return ResponseEntity.ok(Map.of(
                "q", q,
                "count", results.size(),
                "results", results
        ));
    }
}
