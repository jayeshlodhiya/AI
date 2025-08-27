package com.retailai.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;

@Service
public class StorageService {

    private final Path uploadRoot;

    public StorageService(Path uploadRoot) {
        this.uploadRoot = uploadRoot; // injected bean named "uploadRoot"
    }

    public Path save(MultipartFile file, String subdir) throws Exception {
        // Clean filename to avoid path traversal
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (filename.contains("..")) throw new IllegalArgumentException("Invalid filename");

        Path targetDir = (subdir == null || subdir.isBlank())
                ? uploadRoot
                : uploadRoot.resolve(subdir).normalize();

        if (!targetDir.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid subdir");
        }
        Files.createDirectories(targetDir);

        Path target = targetDir.resolve(filename).normalize();

        // Write atomically: copy stream → replace existing
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
