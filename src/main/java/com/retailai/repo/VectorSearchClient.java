package com.retailai.repo;

import com.retailai.model.RagChunkNew;

import java.util.List;

public interface VectorSearchClient {
    List<RagChunkNew> vectorTopK(String query,
                                 String tenantId,
                                 String docType,
                                 int k);
}
