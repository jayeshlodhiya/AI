package com.retailai.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record IngestTextRequest(
        @NotBlank String sourceId,
        String title,
        @NotBlank String text,
        List<String> tags
) {}
