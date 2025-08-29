
package com.retailai.repo;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
import com.retailai.model.RagChunk;
public interface RagChunkRepo extends JpaRepository<RagChunk, Long> {
  List<RagChunk> findByTenantId(String tenantId);
  List<RagChunk> findTop10ByTenantIdAndDocTypeOrderByIdDesc(String tenantId, String docType);

    @Override
    List<RagChunk> findAll();

    boolean existsByDocId(String docId);
}
