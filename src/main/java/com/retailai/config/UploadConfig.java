// com/retailai/config/UploadConfig.java
package com.retailai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class UploadConfig {

    // If not provided, default under user home: ~/retailai/uploads
    @Value("${app.upload.base:#{systemProperties['user.home'] + '/retailai/uploads'}}")
    private String baseDir;

    @Bean(name = "uploadRoot")
    public Path uploadRoot() {
        try {
            Path root = Paths.get(baseDir).toAbsolutePath().normalize();
            Files.createDirectories(root); // ensures it exists
            return root;
        } catch (IOException e) {
            // Throwing IllegalStateException surfaces the real cause in startup logs
            throw new IllegalStateException("Failed to initialize upload root at: " + baseDir, e);
        }
    }
}
