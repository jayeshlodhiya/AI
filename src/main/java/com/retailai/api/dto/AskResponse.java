package com.retailai.api.dto;

import java.util.ArrayList;
import java.util.List;

public class AskResponse {
    private String type = "rag_response";
    private String answer;
    private String reply; // mirror of answer
    private List<RagSource> sources = new ArrayList<>();
    private List<RagSuggestion> suggestions = new ArrayList<>();

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<RagSource> getSources() { return sources; }
    public void setSources(List<RagSource> sources) { this.sources = sources; }
    public List<RagSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<RagSuggestion> suggestions) { this.suggestions = suggestions; }
}
