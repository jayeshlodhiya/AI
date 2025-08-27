
package com.retailai.service;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.retailai.model.*; import com.retailai.repo.*;
@Service
public class IngestService {
  private final ProductRepo productRepo; private final SaleRepo saleRepo; private final CustomerRepo customerRepo; private final InventoryRepo inventoryRepo;
  public IngestService(ProductRepo p, SaleRepo s, CustomerRepo c, InventoryRepo i) { this.productRepo=p; this.saleRepo=s; this.customerRepo=c; this.inventoryRepo=i; }
  @Transactional
  public Sale ingestSale(Map<String, Object> m) {
    Long productId = toLong(m.getOrDefault("productId", m.get("product_id")));
    String sku     = asString(m.get("sku"));
    int qty        = toInt(m.getOrDefault("qty", 1));
    BigDecimal price = toBigDecimal(m.get("price"));
    String variant = asString(m.getOrDefault("variant",""));
    String channel = asString(m.getOrDefault("channel","pos"));
    String invoiceNo = asString(m.get("invoiceNo"));
    if (invoiceNo == null || invoiceNo.isBlank()) invoiceNo = "INV-" + System.currentTimeMillis();
    BigDecimal total = toBigDecimal(m.get("total"));
    Long customerId  = toLong(m.get("customerId"));

    // Resolve product (by productId or sku)
    Product p = null;
    if (productId != null) {
      p = productRepo.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    } else if (sku != null && !sku.isBlank()) {
      p = productRepo.findBySku(sku)
        .orElseThrow(() -> new IllegalArgumentException("Product not found by sku: " + sku));
    } else {
      throw new IllegalArgumentException("Provide productId or sku");
    }

    if (price == null) {
      price = (p.getMrp() != null) ? p.getMrp() : p.getCost(); // fallback
    }
    if (total == null) {
      total = price.multiply(BigDecimal.valueOf(qty));
    }

    // Build header
    Sale sale = new Sale();
    sale.setChannel(channel);
    sale.setCreatedAt(OffsetDateTime.now());
    sale.setInvoiceNo(invoiceNo);
    sale.setTotal(total);
    sale.setCustomerId(customerId);

    // Build line
    SaleItem si = new SaleItem();
    si.setProduct(p);
    si.setQty(qty);
    si.setPrice(price);
    si.setVariant(variant);
    si.setSku(                               // ← set it!
                                             java.util.Objects.requireNonNull(
                                               p.getSku(), "Product SKU is null for product_id=" + p.getId()
                                             )
    );
    // Link both sides (requires Sale#addItem and items initialized)
    sale.addItem(si);

    return saleRepo.save(sale);
  }

  // helpers (same as I shared earlier)
  private static String asString(Object o){ return o==null? null : String.valueOf(o); }
  private static Long toLong(Object o){ if(o==null)return null; if(o instanceof Number n)return n.longValue(); String s=String.valueOf(o).trim(); return s.isEmpty()?null:Long.parseLong(s); }
  private static Integer toInt(Object o){ if(o==null)return 0; if(o instanceof Number n)return n.intValue(); String s=String.valueOf(o).trim(); return s.isEmpty()?0:Integer.parseInt(s); }
  private static BigDecimal toBigDecimal(Object o){ if(o==null)return null; if(o instanceof BigDecimal b)return b; if(o instanceof Number n)return new BigDecimal(n.toString()); String s=String.valueOf(o).trim(); return s.isEmpty()?null:new BigDecimal(s); }
  @Transactional public Inventory upsertInventoryRow(Map<String,Object> row){
    String sku = String.valueOf(row.get("sku")); String variant = String.valueOf(row.getOrDefault("variant",""));
    int qty = ((Number) row.getOrDefault("qty",0)).intValue(); int reorder = ((Number) row.getOrDefault("reorderLevel",5)).intValue();
    String location = String.valueOf(row.getOrDefault("location","LOC-UNK"));
    Product p = productRepo.findBySku(sku).orElseGet(() -> { Product np=new Product(); np.setSku(sku); np.setName("Auto "+sku); return productRepo.save(np); });
    return inventoryRepo.findBySkuAndVariant(sku, variant).map(inv -> { inv.setQty(qty); inv.setReorderLevel(reorder); inv.setLocation(location); return inventoryRepo.save(inv); })
      .orElseGet(() -> { Inventory i=new Inventory(); i.setProduct(p); i.setVariant(variant); i.setQty(qty); i.setReorderLevel(reorder); i.setLocation(location); return inventoryRepo.save(i); });
  }
}
