package com.retailai.api.dto;

public record IngestResponse(String sourceId, int chunks, String status) {}
