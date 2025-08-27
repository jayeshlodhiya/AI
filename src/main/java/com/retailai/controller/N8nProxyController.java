package com.retailai.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/n8n/promotions")
public class N8nProxyController {
    private final WebClient web = WebClient.builder().baseUrl("https://jayeshlodhiya.app.n8n.cloud").build();
//https://jayeshlodhiya.app.n8n.cloud/webhook/5ad43414-12cd-42d1-bec0-953132fe4dfc
@PostMapping(value = "/start", consumes = MediaType.APPLICATION_JSON_VALUE)
public Mono<ResponseEntity<String>> startJson(@RequestBody Map<String, Object> payload) {
    return web.post()
            .uri("https://jayeshlodhiya.app.n8n.cloud/webhook/5ad43414-12cd-42d1-bec0-953132fe4dfc")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(payload)            // must be non-null JSON
            .retrieve()
            .toEntity(String.class);
}

    @GetMapping("/status/{id}")
    public Mono<ResponseEntity<String>> status(@PathVariable String id) {
        return web.get().uri("/webhook/promotions-status/{id}", id)
                .retrieve().toEntity(String.class);
    }
}
