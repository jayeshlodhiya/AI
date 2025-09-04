package com.retailai.api.dto;

import java.util.List;

public record AnswerResponse(
        String answer,
        List<ContextChunk> context
) {
    public record ContextChunk(String sourceId, String title, String snippet, int position, List<String> tags) {}
}
