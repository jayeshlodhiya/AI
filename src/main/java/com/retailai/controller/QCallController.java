package com.retailai.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.api.dto.CallLogDTO;
import com.retailai.model.QCallPlaygroundRequest;
import com.retailai.service.QCallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.retailai.util.AnswerNormalizer.normalizeTranscript;
import static com.retailai.util.AnswerNormalizer.toTurns;

import static com.retailai.util.TranscriptParser.*;
import static org.hibernate.engine.config.spi.StandardConverters.asInteger;
import static org.hibernate.engine.config.spi.StandardConverters.asString;

@RestController
@RequestMapping("/api/qcall")
public class QCallController {
    private final QCallService qCallService;
    private final ObjectMapper mapper;

    public QCallController(QCallService qCallService, ObjectMapper mapper) {
        this.qCallService = qCallService;
        this.mapper = mapper;
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
    @GetMapping("/playgroundHistory")
    public ResponseEntity<List<CallLogDTO>> playgroundHistory() throws JsonProcessingException {
        List<Map<String,Object>> callLogs = qCallService.listPlayground();
        System.out.println("call logs: " + callLogs.size());

        List<CallLogDTO> out = new ArrayList<>(callLogs.size());
        for (Map<String, Object> row : callLogs) {
            CallLogDTO dto = new CallLogDTO();
            dto.id = asString(row.get("id"));
            dto.phone_number = asString(row.get("phone_number"));
            dto.call_cost = asString(row.get("call_cost"));
            dto.call_duration_in_sec = asInteger(row.get("call_duration_in_sec"));
            dto.call_sentiment = row.get("call_sentiment");
            dto.created_at = asString(row.get("created_at"));
            dto.assistant_name = asString(row.get("assistant_name"));
           // call.setCallTranscribe(new ObjectMapper().writeValueAsString(transcript));
            if(row.get("call_transcribe")!=null){
                dto.call_transcribe = formatTranscriptMore(row.get("call_transcribe").toString());//formatTranscript(row.get("call_transcribe").toString()));// toTurns(row.get("call_transcribe"), mapper); // normalize here
            }else {
                dto.call_transcribe = "-";
            }
            // ... map other fields you need ...
            System.out.println("call logs dto: " + dto.toString());
            out.add(dto);
        }

        return ResponseEntity.ok(out);
    }
}

