package com.retailai.controller;

import com.retailai.api.dto.ExtractRequest;
import com.retailai.api.dto.ExtractResponse;
import com.retailai.api.dto.PerformRequest;
import com.retailai.api.dto.PerformResponse;
import com.retailai.service.ActionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")

public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping("/extract")
    public ExtractResponse extract(@RequestBody ExtractRequest req) {
        return new ExtractResponse(actionService.extractFromDoc(req.tenantId(), req.docId()));
    }

    @PostMapping("/perform")
    public PerformResponse perform(@RequestBody PerformRequest req) {
        boolean ok = actionService.perform(req.tenantId(), req.action());
        return new PerformResponse(ok, ok ? "done" : "failed");
    }
}
