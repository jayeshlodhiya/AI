
package com.retailai.controller;

import com.retailai.service.FileIngestService;
import com.retailai.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest2")
public class FileUploadController {
    private final FileIngestService svc;
    private final StorageService storageService;
    public FileUploadController(FileIngestService s,  StorageService storageService) {
        this.svc = s;

        this.storageService = storageService;
    }

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(path = "/file", consumes = {"multipart/form-data"})
    public ResponseEntity<?> upload(@RequestParam("type") String type, @RequestPart("file") MultipartFile file) throws Exception {
        File dest = new File(uploadDir, file.getOriginalFilename());
        file.transferTo(dest);
        Map<String, Object> result = svc.ingest(type, file);
        result.put("savedPath", dest.getAbsolutePath());
        return ResponseEntity.ok(result);
    }
}
