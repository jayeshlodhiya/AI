
package com.retailai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class UploadDirInitializer {
  @Value("${app.upload-dir:uploads}") private String uploadDir;
  @PostConstruct public void init() {
    File d = new File(uploadDir);
    if (!d.exists()) d.mkdirs();
    System.out.println("[UPLOAD] Using dir: " + d.getAbsolutePath());
  }
}
