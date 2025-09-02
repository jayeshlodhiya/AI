package com.retailai.service;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.Base64;

@Service
public class GeminiImageService {

    @Value("${google.gemini.api-key:}")
    private String apiKey;

    @Value("${google.gemini.model:gemini-2.5-flash-image-preview}")
    private String model;

    private HttpClient http;
    private final ObjectMapper om = new ObjectMapper();

    @PostConstruct
    void init() {
        http = HttpClient.newHttpClient();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY not configured.");
        }
    }

    public List<Map<String, String>> generate(String prompt, List<MultipartFile> images) throws Exception {
        // Build "parts": first the text prompt, then inline image(s) if provided
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (images != null) {
            // Google suggests best results with up to ~3 input images.
            for (MultipartFile f : images.stream().limit(3).toList()) {
                String mime = Optional.ofNullable(f.getContentType()).orElse("image/png");
                String b64 = Base64.getEncoder().encodeToString(f.getBytes());
                Map<String, Object> inline = Map.of(
                        "inline_data", Map.of(
                                "mime_type", mime,
                                "data", b64
                        )
                );
                parts.add(inline);
            }
        }

        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of("parts", parts))
        );

        byte[] body = om.writeValueAsBytes(payload);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI("https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent"))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Gemini API error: " + resp.statusCode() + " -> " + resp.body());
        }

        JsonNode root = om.readTree(resp.body());
        // Collect all returned images (inlineData/inline_data .data + mimeType/mime_type)
        List<Map<String, String>> out = new ArrayList<>();

        var candidates = root.path("candidates");
        for (JsonNode cand : candidates) {
            var partsNode = cand.path("content").path("parts");
            for (JsonNode p : partsNode) {
                // Support both camelCase and snake_case
                JsonNode inline = p.has("inlineData") ? p.path("inlineData") : p.path("inline_data");
                if (inline.has("data")) {
                    String data = inline.path("data").asText();
                    String mime = inline.has("mimeType") ? inline.path("mimeType").asText()
                            : (inline.has("mime_type") ? inline.path("mime_type").asText()
                            : "image/png");
                    out.add(Map.of("mimeType", mime, "base64", data));
                }
            }
        }
        if (out.isEmpty()) {
            // Sometimes first part can be text (explanations). Not an error, but surface it.
            out.add(Map.of("mimeType", "text/plain", "base64", Base64.getEncoder().encodeToString(
                    ("No image returned. Response: " + resp.body()).getBytes())));
        }
        return out;
    }
}
