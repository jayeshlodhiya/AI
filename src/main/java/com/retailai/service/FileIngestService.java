
package com.retailai.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailai.model.Inventory;
import com.retailai.model.Product;
import com.retailai.repo.InventoryRepo;
import com.retailai.repo.ProductRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader; import java.io.InputStreamReader;
import java.math.BigDecimal; import java.nio.charset.StandardCharsets;
import java.util.*; import java.util.stream.Collectors;

@Service
public class FileIngestService {
  private final IngestService ingestService; private final ProductRepo productRepo; private final InventoryRepo inventoryRepo; private final RAGService ragService;
  private final ObjectMapper mapper = new ObjectMapper();
  public FileIngestService(IngestService ingestService, ProductRepo productRepo, InventoryRepo inventoryRepo, RAGService ragService){
    this.ingestService=ingestService; this.productRepo=productRepo; this.inventoryRepo=inventoryRepo; this.ragService=ragService;
  }
  public Map<String,Object> ingest(String type, MultipartFile file) throws Exception {
    switch (type.toLowerCase()) {
      case "inventory": return ingestInventoryCsv(file);
      case "products": return ingestProductsCsv(file);
      case "sales": return ingestSalesJsonl(file);
      case "rag_policies": return indexPolicies(file);
      case "rag_catalog": return indexCatalog(file);
      default: return Map.of("status","error","message","Unknown type: "+type);
    }
  }
  private Map<String,Object> ingestInventoryCsv(MultipartFile file) throws Exception {
    List<Map<String,Object>> rows = readCsv(file); int upserts=0;
    for (Map<String,Object> row: rows){ ingestService.upsertInventoryRow(row); upserts++; }
    return Map.of("status","ok","type","inventory","rows", upserts);
  }
  private Map<String,Object> ingestProductsCsv(MultipartFile file) throws Exception {
    List<Map<String,Object>> rows = readCsv(file); int count=0;
    for (Map<String,Object> r: rows){ String sku=String.valueOf(r.get("sku"));
      Product p = productRepo.findBySku(sku).orElseGet(Product::new); p.setSku(sku); p.setName(String.valueOf(r.get("name")));
      p.setCategory(String.valueOf(r.get("category")));
      try { p.setMrp(new BigDecimal(String.valueOf(r.get("mrp")))); } catch(Exception ignore){}
      try { p.setCost(new BigDecimal(String.valueOf(r.get("cost")))); } catch(Exception ignore){}
      p.setAttributesJson(String.valueOf(r.get("attributes"))); productRepo.save(p); count++; }
    return Map.of("status","ok","type","products","rows", count);
  }
  private Map<String,Object> ingestSalesJsonl(MultipartFile file) throws Exception {
    int ing=0; try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line; while((line=br.readLine())!=null){ if (line.isBlank()) continue;
        Map<String,Object> payload = mapper.readValue(line, new TypeReference<Map<String,Object>>(){}); ingestService.ingestSale(payload); ing++; }
    }
    return Map.of("status","ok","type","sales","rows", ing);
  }
  private Map<String,Object> indexPolicies(MultipartFile file) throws Exception {
    String text = new String(file.getBytes(), StandardCharsets.UTF_8);
    String[] parts = text.split("\n\s*##|\n\n+"); int n=0;
    for (String p: parts){ String t=p.trim(); if (t.length()<8) continue; ragService.index("demo","policy","policy-"+n, t, java.util.List.of("policy")); n++; }
    return Map.of("status","ok","type","rag_policies","indexed", n);
  }
  private Map<String,Object> indexCatalog(MultipartFile file) throws Exception {
    int n=0; try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line; while((line=br.readLine())!=null){ if (line.isBlank()) continue;
        Map<String,Object> obj = mapper.readValue(line, new TypeReference<Map<String,Object>>(){});
        String id = String.valueOf(obj.getOrDefault("doc_id","cat-"+n)); String t = String.valueOf(obj.getOrDefault("text",""));
        @SuppressWarnings("unchecked") java.util.List<String> tags = (java.util.List<String>) obj.getOrDefault("tags", java.util.List.of("product"));
        if (t.length()<4) continue; ragService.index("demo","product", id, t, tags); n++; }
    }
    return Map.of("status","ok","type","rag_catalog","indexed", n);
  }
  private List<Map<String,Object>> readCsv(MultipartFile file) throws Exception {
    List<String> lines; try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      lines = br.lines().collect(Collectors.toList());
    }
    if (lines.isEmpty()) return List.of();
    String[] headers = lines.get(0).split(",");
    List<Map<String,Object>> rows = new ArrayList<>();
    for (int i=1;i<lines.size();i++){ String[] vals = lines.get(i).split(",", -1); Map<String,Object> map = new LinkedHashMap<>();
      for (int j=0;j<headers.length && j<vals.length;j++){ map.put(headers[j].trim(), vals[j].trim()); } rows.add(map); }
    return rows;
  }
}
