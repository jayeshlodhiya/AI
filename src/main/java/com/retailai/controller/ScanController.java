// src/main/java/com/retailai/controller/ScanController.java
package com.retailai.controller;

import com.retailai.service.ScanIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/ingest")
public class ScanController {

  private final ScanIngestService scan;
  public ScanController(ScanIngestService scan) { this.scan = scan; }

  @PostMapping(path="/scan", consumes={"multipart/form-data"})
  public ResponseEntity<?> scan(
    @RequestParam("type") String type,            // invoice | warranty | policy | note | other
    @RequestPart("file") MultipartFile file,
    @RequestParam(value="tenant_id", defaultValue="demo") String tenant
  ) throws Exception {
    String clean = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(),"scan"));
    String docId = clean.replaceAll("[^A-Za-z0-9._-]","_");
    return ResponseEntity.ok(scan.scanAndIndex(tenant, type, docId, file));
  }
}
