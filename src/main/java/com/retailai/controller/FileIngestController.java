package com.retailai.controller;

import com.retailai.service.RAGService;
import com.retailai.service.RagIngestService;
import com.retailai.service.StorageService;
 // whatever does chunking + upsert
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class FileIngestController {

    private final StorageService storage;
    private final RagIngestService rag; // your existing service that extracts + chunks

    public FileIngestController(StorageService storage, RagIngestService rag) {
        this.storage = storage;
        this.rag = rag;
    }

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "tenant_id", defaultValue = "demo") String tenantId
    ) {
        try {
            // Put each tenant/type in its own subdir (optional)
            String subdir = (tenantId == null ? "default" : tenantId) +
                    (type != null && !type.isBlank() ? ("/" + type) : "");

            Path saved = storage.save(file, subdir); // <-- NO duplicate "uploads"

            // Hand off to RAG pipeline (parse PDF/Word/Image, chunk, upsert)
            int chunks = rag.ingestFile(file, tenantId, type);//.size(); // implement this

            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", true);
            resp.put("path", saved.toString());
            resp.put("docId", saved.getFileName().toString());
            resp.put("chunks", chunks);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
