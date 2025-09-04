package com.retailai.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record IngestUrlRequest(
        @NotBlank String sourceId,
        @NotBlank String url,
        String title,
        List<String> tags
) {}
