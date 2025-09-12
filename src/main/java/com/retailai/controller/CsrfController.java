package com.retailai.controller;
// src/main/java/com/retailai/controller/CsrfController.java


import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class CsrfController {
    @GetMapping("/csrf-token")
    public Map<String, String> token(HttpServletRequest request) {
        CsrfToken t = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return Map.of(
                "headerName", t.getHeaderName(),
                "parameterName", t.getParameterName(),
                "token", t.getToken()
        );
    }
}
