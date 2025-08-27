
package com.retailai.controller;
import org.springframework.beans.factory.annotation.Value; import org.springframework.core.io.FileSystemResource; import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.io.File;
@RestController @RequestMapping("/api/files")
public class FileServeController {
  @Value("${app.upload-dir:uploads}") private String uploadDir;
  @GetMapping("/{filename}") public ResponseEntity<Resource> serve(@PathVariable String filename){
    File f = new File(uploadDir, filename); if (!f.exists()) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(new FileSystemResource(f));
  }
}
