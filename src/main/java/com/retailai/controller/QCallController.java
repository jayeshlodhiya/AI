package com.retailai.controller;


import com.retailai.model.QCallPlaygroundRequest;
import com.retailai.service.QCallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qcall")
public class QCallController {
    private final QCallService qCallService;

    public QCallController(QCallService qCallService) {
        this.qCallService = qCallService;
    }

    @GetMapping("/assistants")
    public ResponseEntity<List<Map<String,Object>>> getAssistants() {
        List<Map<String,Object>> assistants = qCallService.listAssistants();
        System.out.println("assistants: " + assistants.size());
        return ResponseEntity.ok(assistants);
    }

    @PostMapping("/assistants")
    public ResponseEntity<Map<String,Object>> createAssistant(@RequestBody Map<String,Object> payload) {
        Map<String,Object> result = qCallService.createAssistant(payload);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/playground/call")
    public ResponseEntity<Map<String,Object>> playgroundCall(@RequestBody QCallPlaygroundRequest req) {
        Map<String,Object> result = qCallService.startPlaygroundCall(req);
        return ResponseEntity.ok(result);
    }
}

