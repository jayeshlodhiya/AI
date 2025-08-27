package com.retailai.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

@Service
public class ChatService {

  private static final Pattern AVAIL_PATTERN_FREE = Pattern.compile("(?i)(availability|stock|have|in\s*stock).*?([A-Z0-9-]{3,})[^0-9A-Za-z]+([A-Za-z0-9| ]{1,40})");
  private static final Pattern AVAIL_PATTERN_COLON = Pattern.compile("(?i)\b([A-Z0-9-]{3,})[: ]([A-Za-z0-9|]{1,40})\b");
  private final InventoryService inventoryService;
  private final RAGService ragService;
  private final SuggestionService suggestionService;
  private final LlmClient llm;

  public ChatService(InventoryService inv, RAGService rag, SuggestionService sug, LlmClient llm) {
    this.inventoryService = inv;
    this.ragService = rag;
    this.suggestionService = sug;
    this.llm = llm;
  }

  @SafeVarargs
  private static Map<String, Object> mapOf(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
    return m;
  }

  public Map<String, Object> answerQuestion(String tenantId, String question) {
    String q = question == null ? "" : question.trim();
    Optional<SkuVariant> parsed = parseSkuVariant(q);
    if (parsed.isPresent()) {
      SkuVariant sv = parsed.get();
     // return inventoryService.availability(sv.sku, sv.variant).map(inv -> {
      //  Map<String, Object> dto = inventoryService.toDto(inv);
       // String ans = String.format("Availability for %s (%s): qty %s at %s. Suggested reorder: %s.", dto.get("sku"), dto.get("variant"), dto.get("qty"), dto.get("location"), dto.get("suggestReorderQty"));
        //return mapOf("type", "tool_response", "tool", "get_inventory", "answer", ans, "data", dto, "suggestions", suggestionService.suggestPromotions());
      //}).orElseGet(() -> mapOf("type", "tool_response", "tool", "get_inventory", "answer", String.format("No inventory found for %s (%s).", sv.sku, sv.variant), "data", Map.of("sku", sv.sku, "variant", sv.variant), "suggestions", suggestionService.suggestPromotions()));
    }
    String tenant = (tenantId == null || tenantId.isBlank()) ? "demo" : tenantId;
    java.util.List<java.util.Map<String, Object>> hits = ragService.search(tenant, q, 6, null);
    StringBuilder ctx = new StringBuilder();
    for (int i = 0; i < hits.size(); i++) {
      ctx.append("Source ").append(i + 1).append(": ").append(hits.get(i).getOrDefault("text", "")).append("\n");
    }
    String system = "You are a real estate builder and sales person. Answer clearly using ONLY the provided context. If insufficient, say so and suggest what to upload.Be specifically with the person's name mentioned in query if you do not find in chunks say unable to provide data for mentioned name. Be concise. Also provide followup suggestion about further quetions that can be asked from chunks returned and also mentioned IF ANY deadline or scheduling mentioned in it";
    String user = "Question: " + q + "\n\nContext:\n" + ctx + "\n\nAnswer:";
    String llmAns = llm.complete(system, user);
    System.out.println("LLm response : "+llmAns);
    if (llmAns == null || llmAns.isBlank()) {
      StringBuilder sb = new StringBuilder("I couldn't find an exact answer. Try rephrasing or upload FAQs/Product docs to RAG.");
      return mapOf("type", "rag_response", "answer", sb.toString(), "sources", hits, "suggestions", suggestionService.suggestPromotions());
    }
    return mapOf("type", "rag_response", "answer", llmAns.trim(), "sources", hits, "suggestions", suggestionService.suggestPromotions());
  }

  public void stream(String question, Consumer<String> tokenConsumer) {
    String q = question == null ? "" : question.trim();
    String tenant = "demo"; // Default tenant for streaming
    java.util.List<java.util.Map<String, Object>> hits = ragService.search(tenant, q, 6, null);
    
    StringBuilder ctx = new StringBuilder();
    for (int i = 0; i < hits.size(); i++) {
      ctx.append("Source ").append(i + 1).append(": ").append(hits.get(i).getOrDefault("text", "")).append("\n");
    }
    
    String system = "You are a retail operations assistant. Answer clearly using ONLY the provided context. If insufficient, say so and suggest what to upload. Be concise.";
    String user = "Question: " + q + "\n\nContext:\n" + ctx + "\n\nAnswer:";
    
    // For now, use the existing complete method and stream the result character by character
    // In a real implementation, you'd want to modify LlmClient to support actual streaming
    String response = llm.complete(system, user);
    // Keep single spaces; normalize newlines for UI
   

    if (response != null && !response.isBlank()) {
      response = response.replace("\r\n", "\n");              // normalize CRLF -> LF
      response = response.replaceAll("[ \\t\\u00A0\\u2007\\u202F]+", " "); // collapse spaces, keep one
      response = response.replaceAll("\\n{3,}", "\n\n");      // limit blank lines
      response = response.trim();
      // Stream the response character by character to simulate streaming
      for (char c : response.toCharArray()) {
        tokenConsumer.accept(String.valueOf(c));
      }
    } else {
      tokenConsumer.accept("I couldn't find an exact answer. Try rephrasing or upload FAQs/Product docs to RAG.");
    }
  }

  private Optional<SkuVariant> parseSkuVariant(String q) {
    if (q == null || q.isBlank()) return Optional.empty();
    Matcher m = AVAIL_PATTERN_FREE.matcher(q);
    if (m.find()) return Optional.of(new SkuVariant(m.group(2).trim(), m.group(3).trim()));
    Matcher m2 = AVAIL_PATTERN_COLON.matcher(q);
    if (m2.find()) {
      String sku = m2.group(1).trim(), variant = m2.group(2).trim();
      if (!variant.equalsIgnoreCase("stock") && !variant.equalsIgnoreCase("availability")) {
        return Optional.of(new SkuVariant(sku, variant));
      }
    }
    return Optional.empty();
  }

  private static class SkuVariant {

    final String sku;
    final String variant;

    SkuVariant(String s, String v) {
      this.sku = s;
      this.variant = v;
    }
  }
}
