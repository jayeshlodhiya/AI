
package com.retailai.service;

import com.retailai.api.dto.AnswerResponse;
import com.retailai.api.dto.QueryRequest;
import com.retailai.service.rag.RagApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * LlmClient supports:
 *  - provider=ollama  ->  baseUrl like http://localhost:11434 (uses /api/chat)
 *  - provider=openai  ->  baseUrl like https://api.groq.com/openai/v1 (uses /chat/completions)
 *
 * Configure in application.yml:
 * app:
 *   llm:
 *     provider: ollama            # ollama | openai
 *     model: llama3:8b-instruct   # or your hosted model name
 *     base-url: http://localhost:11434
 *     api-key: ""                 # only for openai-style providers
 *     connect-timeout: 10000      # connection timeout in milliseconds (default: 10s)
 *     read-timeout: 60000         # read timeout in milliseconds (default: 60s)
 */
@Service
public class LlmClient {

  @Value("${app.llm.provider:ollama}")
  private String provider;

  @Value("${app.llm.model:llama3:8b-instruct}")
  private String model;

  @Value("${app.llm.base-url:http://localhost:11434}")
  private String baseUrl;

  @Value("${app.llm.api-key:}")
  private String apiKey;

  @Value("${app.llm.connect-timeout:10000}")
  private int connectTimeout;

  @Value("${app.llm.read-timeout:60000}")
  private int readTimeout;
    @Value("${app.seed.enabled:true}")
  private boolean externalRag;

  private final RestTemplate http;
  private final String SYSTEM_PROMPT = "Check if any followup is mentioned in response if yes then append #YES in the response otherwise add #NO ";
    @Autowired
    RagApiClient ragApiClient;

  public LlmClient() {
    // Initialize with default timeouts, will be updated by @Value injection
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(10000);  // 10 seconds default
    f.setReadTimeout(60000);     // 60 seconds default
    this.http = new RestTemplate(f);
  }

  /** Call the configured LLM and return the text content (or empty string on failure). */
  public String complete(String systemPrompt, String userPrompt) {
    // Update timeouts from configuration
    SimpleClientHttpRequestFactory factory = (SimpleClientHttpRequestFactory) http.getRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    if(externalRag) {
        String resp =  callExternalRAGSystem(systemPrompt , userPrompt);
        System.out.println("Response from rag llm external"+resp);
        return resp;
    }

    if ("ollama".equalsIgnoreCase(provider)) {
      return callOllama(systemPrompt, userPrompt);
    }
    // default: OpenAI-compatible
    return callOpenAIStyle(systemPrompt, userPrompt);
  }

    private String callExternalRAGSystem(String systemPrompt, String userPrompt) {
        QueryRequest queryRequest = new QueryRequest(userPrompt,50,null,true,true);
        AnswerResponse  answerResponse = ragApiClient.ask(queryRequest);
        return answerResponse.answer();
    }

    // ---------------- Ollama ----------------

  @SuppressWarnings("rawtypes")
  private String callOllama(String systemPrompt, String userPrompt) {
    String url = baseUrl + "/api/chat";

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);

    List<Map<String, String>> messages = new ArrayList<>();
    if (systemPrompt != null && !systemPrompt.isBlank()) {
      messages.add(Map.of("role", "system", "content", systemPrompt));
    }
    messages.add(Map.of("role", "user", "content", userPrompt));
    body.put("messages", messages);
    body.put("stream", false);

    try {
      System.out.println("[LLM] Calling Ollama at: " + url);
      System.out.println("[LLM] Model: " + model);
      System.out.println("[LLM] Timeouts - Connect: " + connectTimeout + "ms, Read: " + readTimeout + "ms");
      
      ResponseEntity<Map> res = http.postForEntity(url, body, Map.class);
      Map<?, ?> root = res.getBody();
      if (root == null) return "";

      // Newer Ollama returns { "message": { "role":"assistant", "content":"..." }, ... }
      Object msg = root.get("message");
      if (msg instanceof Map) {
        Object content = ((Map<?, ?>) msg).get("content");
        return content == null ? "" : content.toString();
      }

      // Fallback: some versions expose "choices" like OpenAI
      Object choices = root.get("choices");
      if (choices instanceof List && !((List<?>) choices).isEmpty()) {
        Object c0 = ((List<?>) choices).get(0);
        if (c0 instanceof Map) {
          Object m = ((Map<?, ?>) c0).get("message");
          if (m instanceof Map) {
            Object content = ((Map<?, ?>) m).get("content");
            return content == null ? "" : content.toString();
          }
        }
      }
    } catch (RestClientException e) {
      System.err.println("[LLM] Ollama call failed: " + e.getMessage());
      if (e.getMessage().contains("Read timed out")) {
        System.err.println("[LLM] This suggests the model is taking too long to respond.");
        System.err.println("[LLM] Consider increasing app.llm.read-timeout or using a faster model.");
      }
    }
    return "";
  }

  // --------------- OpenAI-compatible ----------------

  @SuppressWarnings("rawtypes")
  private String callOpenAIStyle(String systemPrompt, String userPrompt) {
    String url = baseUrl + "/chat/completions";

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);

    List<Map<String, String>> messages = new ArrayList<>();
    if (systemPrompt != null && !systemPrompt.isBlank()) {
      messages.add(Map.of("role", "system", "content", systemPrompt));
    }
    messages.add(Map.of("role", "user", "content", userPrompt));
    body.put("messages", messages);
    body.put("temperature", 0.2);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (apiKey != null && !apiKey.isBlank()) {
      headers.setBearerAuth(apiKey);
    }

    try {
      HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
      ResponseEntity<Map> res = http.exchange(url, HttpMethod.POST, req, Map.class);

      Map<?, ?> root = res.getBody();
      if (root == null) return "";

      Object choices = root.get("choices");
      if (choices instanceof List && !((List<?>) choices).isEmpty()) {
        Object c0 = ((List<?>) choices).get(0);
        if (c0 instanceof Map) {
          Object message = ((Map<?, ?>) c0).get("message");
          if (message instanceof Map) {
            Object content = ((Map<?, ?>) message).get("content");
            return content == null ? "" : content.toString();
          }
        }
      }
    } catch (RestClientException e) {
      System.err.println("[LLM] OpenAI-style call failed: " + e.getMessage());
    }
    return "";
  }
  public void stream(String prompt, java.util.function.Consumer<String> onToken) {
    // If you later integrate a streaming SDK, push tokens via onToken.accept(token);
    onToken.accept(complete(prompt, 512)); // naive single-chunk fallback
}

public String complete(String prompt, int maxTokens) {
    // existing completion HTTP call
    return "Hello! Streaming is wired; plug your LLM here.";
}
}

