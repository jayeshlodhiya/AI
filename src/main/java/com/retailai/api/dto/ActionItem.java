package com.retailai.api.dto;

import java.util.Map;

public record ActionItem(
        String id,
        String title,
        String description,
        ActionType type,
        Map<String,Object> parameters,
        Double confidence,
        ActionSource source
) {}