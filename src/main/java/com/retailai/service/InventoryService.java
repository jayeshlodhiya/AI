
package com.retailai.service;
import java.util.*; import org.springframework.stereotype.Service;
import com.retailai.model.Inventory; import com.retailai.repo.InventoryRepo;
@Service
public class InventoryService {
  private final InventoryRepo repo;
  public InventoryService(InventoryRepo repo) { this.repo = repo; }
  public Optional<Inventory> availability(String sku, String variant){ return repo.findBySkuAndVariant(sku, variant); }
  public java.util.List<Inventory> lowStock(){ return repo.findLowStock(); }
  public Map<String,Object> toDto(Inventory inv){
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("sku", inv.getProduct().getSku());
    m.put("variant", inv.getVariant());
    m.put("qty", inv.getQty());
    m.put("reorderLevel", inv.getReorderLevel());
    m.put("location", inv.getLocation());
    int suggest = Math.max(0, inv.getReorderLevel()*2 - inv.getQty());
    m.put("suggestReorderQty", suggest);
    return m;
  }
}
