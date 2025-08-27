package com.retailai.repo;

import com.retailai.model.RagChunk;
import com.retailai.model.RagChunkNew;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.util.List;

public interface RagChunkRepoNew extends JpaRepository<RagChunkNew, Long> {
    List<RagChunkNew> findByTenantId(String tenantId);
    List<RagChunkNew> findTop10ByTenantIdAndDocTypeOrderByIdDesc(String tenantId, String docType);
    /* -------------------------
       1) Metadata / exact hits
       ------------------------- */
    @Query(value = """
        SELECT c.*
        FROM rag_chunk_new c
        WHERE (:tenantId IS NULL OR c.tenant_id = :tenantId)
          AND (:docType  IS NULL OR c.doc_type  = :docType)
          AND (
                c.phones_norm  ILIKE CONCAT('%', :needle, '%')
             OR c.emails_norm  ILIKE CONCAT('%', :needle, '%')
             OR c.invoice_ids  ILIKE CONCAT('%', :needle, '%')
          )
        ORDER BY c.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RagChunkNew> metaHits(@Param("needle") String needle,
                               @Param("tenantId") String tenantId,
                               @Param("docType") String docType,
                               @Param("limit") int limit);

    /* -------------------------
       2) Full-text search (PostgreSQL)
          Requires:
            - search_text column (TEXT) populated at ingest (normalized content)
            - GIN index recommended:
              CREATE INDEX IF NOT EXISTS idx_rag_chunk_new_fts
              ON rag_chunk_new USING GIN (to_tsvector('simple', search_text));
       ------------------------- */
    @Query(value = """
        SELECT c.*
        FROM rag_chunk_new c
        WHERE (:tenantId IS NULL OR c.tenant_id = :tenantId)
          AND (:docType  IS NULL OR c.doc_type  = :docType)
          AND to_tsvector('simple', coalesce(c.search_text, ''))
              @@ plainto_tsquery('simple', :q)
        ORDER BY
          ts_rank_cd(
            to_tsvector('simple', coalesce(c.search_text,'')),
            plainto_tsquery('simple', :q)
          ) DESC,
          c.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RagChunkNew> ftsEntities(@Param("q") String q,
                                  @Param("tenantId") String tenantId,
                                  @Param("docType") String docType,
                                  @Param("limit") int limit);

    /* -------------------------
       3) LIKE fallback (works everywhere).
          Tip (Postgres): enable pg_trgm for better performance:
            CREATE EXTENSION IF NOT EXISTS pg_trgm;
            CREATE INDEX IF NOT EXISTS idx_rag_chunk_new_trgm
            ON rag_chunk_new USING GIN (search_text gin_trgm_ops);
       ------------------------- */
    @Query(value = """
        SELECT c.*
        FROM rag_chunk_new c
        WHERE (:tenantId IS NULL OR c.tenant_id = :tenantId)
          AND (:docType  IS NULL OR c.doc_type  = :docType)
          AND c.search_text ILIKE CONCAT('%', :q, '%')
        ORDER BY c.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RagChunkNew> like(@Param("q") String q,
                           @Param("tenantId") String tenantId,
                           @Param("docType") String docType,
                           @Param("limit") int limit);

    /* -------------------------
       4) Fetch ordered chunks for a specific doc
       ------------------------- */
    List<RagChunkNew> findTop10ByTenantIdAndDocIdOrderByChunkIndexAsc(String tenantId, String docId);
    @Query("select c from RagChunkNew c where c.tenantId = :tenantId and c.docId = :docId order by c.id desc")
    List<RagChunkNew> findTopNByTenantIdAndDocIdOrderByIdDesc(@Param("tenantId") String tenantId,
                                                              @Param("docId") String docId,
                                                              PageRequest pageable);
}
