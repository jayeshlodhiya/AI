package com.retailai.service;

import java.util.stream.Collectors;

import com.retailai.model.RagChunkNew;
import com.retailai.repo.RagChunkRepoNew;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retailai.model.RagChunk;
import com.retailai.repo.RagChunkRepo;

@Service
public class RAGService {

  private final RagChunkRepo repo;
  private final RagChunkRepoNew repoNew;

  public RAGService(RagChunkRepo r, RagChunkRepoNew repoNew) {
    this.repo = r;
      this.repoNew = repoNew;
  }

  private static java.util.Set<String> tokenize(String s) {
    if (s == null) return java.util.Set.of();
    String n = s.toLowerCase().replaceAll("[^a-z0-9 ]", " ");
    String[] p = n.split("\\s+");
    java.util.Set<String> out = new java.util.HashSet<>();
    for (String w : p) if (w.length() >= 2) out.add(w);
    return out;
  }

  @Transactional
  public int index(String tenantId, String docType, String docId, String text, java.util.List<String> tags) {


       RagChunk c = new RagChunk();
       c.setTenantId(tenantId);
       c.setDocType(docType);
       c.setDocId(docId);
       c.setText(text);
       c.setTags(tags == null ? null : String.join(",", tags));
       repo.save(c);
       return 1;


  }

  @Transactional
  public int indexBulk(String tenantId, String docType, java.util.List<java.util.Map<String, Object>> chunks) {
    int n = 0;
    for (var ch : chunks) {
      String docId = String.valueOf(ch.getOrDefault("id", ch.getOrDefault("doc_id", "unknown")));
      String text = String.valueOf(ch.getOrDefault("text", ""));
      @SuppressWarnings("unchecked") java.util.List<String> tags = (java.util.List<String>) ch.getOrDefault("tags", java.util.List.of());
      index(tenantId, docType, docId, text, tags);
      n++;
    }
    return n;
  }

  public java.util.List<java.util.Map<String, Object>> search(String tenantId, String query, int k, String docType) {
    java.util.List<RagChunk> pool = (docType == null || docType.isBlank()) ? repo.findAll() : repo.findTop10ByTenantIdAndDocTypeOrderByIdDesc(tenantId, docType);
      java.util.List<RagChunkNew> poolNew = (docType == null || docType.isBlank()) ? repoNew.findByTenantId(tenantId) : repoNew.findTop10ByTenantIdAndDocTypeOrderByIdDesc(tenantId, docType);

    if (pool.isEmpty() && poolNew.isEmpty()) return java.util.List.of();


    java.util.Set<String> q = tokenize(query);
    java.util.List<java.util.Map<String, Object>> scored = new java.util.ArrayList<>();

    for (RagChunk c : pool) {
      java.util.Set<String> t = tokenize(c.getText());
      int ov = 0;
      for (String s : q) if (t.contains(s)) ov++;
      double score = q.isEmpty() ? 0 : (double) ov / q.size();
      java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
      m.put("id", c.getId());
      m.put("tenant_id", c.getTenantId());
      m.put("doc_type", c.getDocType());
      m.put("doc_id", c.getDocId());
      m.put("text", c.getText());
      m.put("score", score);
      scored.add(m);
    }
      for (RagChunkNew c : poolNew) {
          java.util.Set<String> t = tokenize(c.getContent());
          int ov = 0;
          for (String s : q) if (t.contains(s)) ov++;
          double score = q.isEmpty() ? 0 : (double) ov / q.size();
          java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
          m.put("id", c.getId());
          m.put("tenant_id", c.getTenantId());
          m.put("doc_type", c.getDocType());
          m.put("doc_id", c.getDocId());
          m.put("text", c.getContent());
          m.put("score", score);
          scored.add(m);
      }
    return scored.stream().sorted((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score"))).limit(k).collect(Collectors.toList());
  }
}
