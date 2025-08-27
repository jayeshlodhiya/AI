package com.retailai.controller;

import com.retailai.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@CrossOrigin(origins = {"http://localhost:3000","http://localhost:8080","http://localhost:8082"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/chat")
public class ChatStreamController {

    private final ChatService chat;

    public ChatStreamController(ChatService chat) {
        this.chat = chat;
    }

    /**
     * GET /api/chat/stream?q=Your%20question
     * Produces Server-Sent Events: events named "token" and final "done".
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("q") String q) {
        // 0L = no timeout (or set e.g. 120_000L)
        SseEmitter emitter = new SseEmitter(0L);

        // Keepalive ping (optional)
        CompletableFuture.runAsync(() -> {
            try {
                chat.stream(q, token -> {
                    try {
                        // stream token as UTF-8 (trim optional)
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(token.getBytes(StandardCharsets.UTF_8)));
                    } catch (Exception sendEx) {
                        emitter.completeWithError(sendEx);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); } catch (Exception ignore) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
