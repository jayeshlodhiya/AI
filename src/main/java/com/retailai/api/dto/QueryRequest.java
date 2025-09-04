package com.retailai.api.dto;

import java.util.List;

public record QueryRequest(
        String query,
        Integer topK,
        List<String> mustHaveTags,
        boolean useHyde,
        boolean useMmr
) {}
