package com.retailai.controller;




import com.retailai.service.GeminiImageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/images")
public class ImageController {
    private final GeminiImageService service;
    public ImageController(GeminiImageService s) { this.service = s; }

    public record GeneratedImagesResponse(List<String> dataUrls) {}

    @PostMapping(value="/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GeneratedImagesResponse> generate(
            @RequestParam @NotBlank String prompt,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) throws Exception {

        List<Map<String, String>> result = service.generate(prompt, images == null ? List.of() : images);

        // Convert to data URLs for easy frontend display
        List<String> dataUrls = new ArrayList<>();
        for (var img : result) {
            String mime = img.get("mimeType");
            String b64 = img.get("base64");
            if (mime.startsWith("image/")) {
                dataUrls.add("data:" + mime + ";base64," + b64);
            } else {
                // non-image (e.g., text explanation fallback)
                dataUrls.add("data:text/plain;base64," + b64);
            }
        }
        return ResponseEntity.ok(new GeneratedImagesResponse(dataUrls));
    }
}
