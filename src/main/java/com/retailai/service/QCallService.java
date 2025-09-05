package com.retailai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.model.QCallPlaygroundRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QCallService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper om = new ObjectMapper();

    public QCallService(RestTemplate restTemplate,
                        @Value("${qcall.base-url}") String baseUrl,
                        @Value("${qcall.api-key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public List<Map<String,Object>> listAssistants() {
        String url = baseUrl + "/user/listAssistant";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);               // Try Bearer first
        headers.set("x-api-key", apiKey);            // Also send x-api-key (some APIs accept this)
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("QCall listAssistants failed: " + resp);
        }

        return parseAssistantList(resp.getBody());
    }

    public Map<String,Object> createAssistant(Map<String,Object> payload) {
        String url = baseUrl + "/user/createAssistant";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("QCall createAssistant failed: " + resp);
        }

        return parseObject(resp.getBody());
    }


    public Map<String, Object> startPlaygroundCall(QCallPlaygroundRequest req) {
        String url = baseUrl + "/playground/call";
        if(req.getAssistantId()==null) {
            req.setAssistantId("4b1b5677-10e3-4005-a502-386f31b579d4");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Send both, some tenants require one or the other:
        headers.setBearerAuth(apiKey);
        headers.set("x-api-key", apiKey);

        // Map DTO to QCall's expected JSON shape (snake_case keys)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("first_name",   req.getFirstName());
        body.put("last_name",    req.getLastName());
        body.put("email",        req.getEmail());
        body.put("assistant_id", req.getAssistantId());
        body.put("phone_number", req.getPhoneNumber()); // list of strings
        body.put("dialer_id",    Optional.ofNullable(req.getDialerId()).orElse(""));

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        System.out.println("Response : " + resp.getBody());

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("QCall playground call failed: " + resp.getStatusCode() +
                    " | body=" + resp.getBody());
        }

        try {
            return om.readValue(resp.getBody(), new TypeReference<Map<String,Object>>(){});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse QCall response: " + e.getMessage() +
                    " | payload=" + resp.getBody(), e);
        }
    }


    // ---------- JSON parsing helpers ----------

    private List<Map<String,Object>> parseAssistantList(String json) {
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(json);

            if (root.isArray()) {
                return om.convertValue(root, new TypeReference<>() {});
            }
            if (root.has("data")) {
                return om.convertValue(root.get("data"), new TypeReference<>() {});
            }
            if (root.has("assistants")) {
                return om.convertValue(root.get("assistants"), new TypeReference<>() {});
            }

            // fallback
            Map<String,Object> single = om.convertValue(root, new TypeReference<>() {});
            return List.of(single);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse assistants JSON: " + e.getMessage() + " | payload=" + json, e);
        }
    }

    private Map<String,Object> parseObject(String json) {
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON: " + e.getMessage() + " | payload=" + json, e);
        }
    }

    public List<Map<String, Object>> listPlayground() {
        String url = baseUrl + "/playground/list";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);               // Try Bearer first
        headers.set("x-api-key", apiKey);            // Also send x-api-key (some APIs accept this)
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("QCall listAssistants failed: " + resp);
        }

        return parseAssistantList(resp.getBody());
    }
}
