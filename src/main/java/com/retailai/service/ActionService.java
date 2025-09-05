package com.retailai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.api.dto.ActionItem;
import com.retailai.api.dto.ActionSource;
import com.retailai.api.dto.ActionType;
import com.retailai.model.ContactLead;
import com.retailai.model.RagChunkNew;
import com.retailai.repo.ContactRepo;
import com.retailai.repo.RagChunkRepoNew;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActionService {

    private final RagChunkRepoNew ragRepo;          // your repo to fetch doc/chunks
    private final ObjectMapper om = new ObjectMapper();
    private final LlmClient llm;
    private final ContactRepo contactRepo;

    public ActionService(RagChunkRepoNew ragRepo, LlmClient llm, ContactRepo contactRepo) {
        this.ragRepo = ragRepo;
        this.llm = llm;
        this.contactRepo = contactRepo;
    }

    public List<ActionItem> extractFromDoc(String tenantId, String docId) {
        // 1) Pull a concise context: latest N chunks of this doc
        List<RagChunkNew> chunks = ragRepo.findTopNByTenantIdAndDocIdOrderByIdDesc(
                tenantId, docId, PageRequest.of(0, 50));
        String context = chunks.stream()
                .map(RagChunkNew::getContent)
                .collect(Collectors.joining("\n---\n"));

        // 2) Ask LLM to return STRICT JSON of action items
        String prompt = """
            You are an assistant that finds concrete, user-actionable items in business documents.
            From the context, extract 0-8 action items. STRICT JSON only, matching this schema:

            {
              "items":[
                {
                  "id": "string",                        // stable id (hash ok)
                  "title": "short imperative",
                  "description": "one sentence",
                  "type": "SEND_EMAIL|SCHEDULE_CALL|CREATE_TASK|UPDATE_INVENTORY|ADD_LEAD|GENERIC",
                  "parameters": { "key":"value" },       // minimal info to execute
                  "confidence": 0.0-1.0,
                  "source": { "docId":"", "docName":"", "chunkIndex": 0 }
                }
              ]
            }

            Only output JSON. Context:
            """ + context;

        // call your LLM client (OpenAI, etc.)
        String json = callLlm(prompt);

        // ---- Robust parsing starts here ----
        json = (json == null) ? "" : json.trim();

        // 2.a strip ```json ... ``` fences if present
        if (json.startsWith("```")) {
            int firstNl = json.indexOf('\n');
            int lastFence = json.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                json = json.substring(firstNl + 1, lastFence).trim();
            }
        }

        // 2.b parse as tree, fail soft on bad JSON
        JsonNode root;
        try {
            root = om.readTree(json);
        } catch (Exception e) {
            System.err.println("[actions] LLM returned non-JSON. First 400 chars:\n" +
                    (json.length() > 400 ? json.substring(0, 400) + "…" : json));
            return List.of();
        }

        // 2.c locate items array in a tolerant way
        JsonNode itemsNode = root.path("items");                   // preferred location
        if (itemsNode.isMissingNode() && root.has("output")) {
            itemsNode = root.path("output").path("items");         // sometimes wrapped
        }
        if (itemsNode.isMissingNode() && root.isArray()) {
            itemsNode = root;                                      // bare array returned
        }

        if (!itemsNode.isArray()) {
            System.err.println("[actions] Unexpected LLM shape (no items[]). First 400 chars:\n" +
                    (json.length() > 400 ? json.substring(0, 400) + "…" : json));
            return List.of();
        }

        // 2.d map JSON -> ActionItem list defensively
        List<ActionItem> items = new ArrayList<>();
        for (JsonNode n : itemsNode) {
            String id         = n.path("id").asText(UUID.randomUUID().toString());
            String title      = n.path("title").asText("Action");
            String desc       = n.path("description").asText("");
            String typeStr    = n.path("type").asText("GENERIC");
            double confidence = n.path("confidence").isNumber() ? n.path("confidence").asDouble() : 0.7;

            Map<String, Object> params = n.path("parameters").isObject()
                    ? om.convertValue(n.path("parameters"), new TypeReference<Map<String, Object>>() {})
                    : Map.of();

            JsonNode s = n.path("source");
            ActionSource src = new ActionSource(
                    s.path("docId").asText(docId),
                    s.path("docName").asText(""),
                    s.path("chunkIndex").isInt() ? s.path("chunkIndex").asInt() : null
            );

            items.add(new ActionItem(
                    id,
                    title,
                    desc,
                    parseType(typeStr),
                    params,
                    confidence,
                    src
            ));
        }

        // Optional: dedupe by id
        // items = items.stream().collect(collectingAndThen(
        //        toMap(ActionItem::id, Function.identity(), (a,b)->a, LinkedHashMap::new),
        //        m -> new ArrayList<>(m.values())));

        return items;
    }

    private static String get(JsonNode n, String f, String d) {
        return n.path(f).isMissingNode() ? d : n.path(f).asText(d);
    }

    @SuppressWarnings("unused")
    private static String get(JsonNode n, String f, String d, boolean required) {
        return n.path(f).asText(d);
    }

    private static ActionType parseType(String s) {
        try { return ActionType.valueOf(s); }
        catch (Exception e) { return ActionType.GENERIC; }
    }

    public boolean perform(String tenantId, ActionItem action) {
        try {
            switch (action.type()) {
                case SEND_EMAIL -> {
                    // parameters: to, subject, body
                    String to = (String) action.parameters().get("to");
                    String subject = (String) action.parameters().getOrDefault("subject", action.title());
                    String body = (String) action.parameters().getOrDefault("body", action.description());
                    // TODO: integrate with your email service
                    return sendEmail(to, subject, body);
                }
                case SCHEDULE_CALL -> {
                    // parameters: customerName, phone, when
                    return scheduleCall(action.parameters());
                }
                case CREATE_TASK -> {
                    return createTask(action.parameters());
                }
                case UPDATE_INVENTORY -> {
                    return updateInventory(action.parameters());
                }
                case ADD_LEAD -> {
                    return addLead(action.parameters());
                }
                default -> {
                    return createTask(action.parameters());
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    // --- stubs to integrate with your app ---
    private boolean sendEmail(String to, String subject, String body){ /* impl */ return true; }
    private boolean scheduleCall(Map<String,Object> p){ /* impl */ return true; }
    private boolean createTask(Map<String,Object> p){ /* impl */ return true; }
    private boolean updateInventory(Map<String,Object> p){ /* impl */ return true; }
    private boolean addLead(Map<String,Object> p){ /* impl */ return true; }

    // --- your LLM client hook ---
    private String callLlm(String prompt){
        // Call your existing OpenAI/other client; return raw JSON as string
        // IMPORTANT: use JSON mode / tool schema to force valid JSON.
        return llm.complete(prompt, "");
    }


    public void saveContact(ContactLead contactLead) {
        contactRepo.save(contactLead);
    }
}
