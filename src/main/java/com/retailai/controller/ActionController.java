package com.retailai.controller;

import com.retailai.api.dto.ExtractRequest;
import com.retailai.api.dto.ExtractResponse;
import com.retailai.api.dto.PerformRequest;
import com.retailai.api.dto.PerformResponse;
import com.retailai.model.ContactLead;
import com.retailai.model.QCallPlaygroundRequest;
import com.retailai.service.ActionService;
import com.retailai.service.QCallService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/actions")

public class ActionController {

    private static final String DEFAULT_ASSITANT_ID = "4b1b5677-10e3-4005-a502-386f31b579d4";
    private final ActionService actionService;
    private final QCallService qCallService;

    public ActionController(ActionService actionService, QCallService qCallService) {
        this.actionService = actionService;
        this.qCallService = qCallService;
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
    @PostMapping("/leads")
    public PerformResponse getLeads(@RequestBody ContactLead contactLead) {
       actionService.saveContact(contactLead);
        qCallService.startPlaygroundCall(new QCallPlaygroundRequest(contactLead.getName(),
                contactLead.getCompanyName(),
                contactLead.getEmail(),
                DEFAULT_ASSITANT_ID,
                List.of(contactLead.getPhone()),
                "",
                contactLead.getDescription()));
        return new PerformResponse(true,"Ok");
    }
}
