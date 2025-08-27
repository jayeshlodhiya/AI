package com.retailai.config;

import com.retailai.model.*;
import com.retailai.repo.*;
import com.retailai.service.IngestService;
import com.retailai.service.RAGService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class StartupDataLoader {//implements CommandLineRunner {

  @Value("${app.seed.enabled:true}")
  private boolean seedEnabled;

  private final ProductRepo productRepo;
  private final InventoryRepo inventoryRepo;
  private final CustomerRepo customerRepo;
  private final SaleRepo saleRepo;
  private final PromotionRepo promotionRepo;
  private final IngestService ingestService;
  private final RAGService ragService;

  public StartupDataLoader(
    ProductRepo productRepo,
    InventoryRepo inventoryRepo,
    CustomerRepo customerRepo,
    SaleRepo saleRepo,
    PromotionRepo promotionRepo,
    IngestService ingestService,
    RAGService ragService
  ) {
    this.productRepo = productRepo;
    this.inventoryRepo = inventoryRepo;
    this.customerRepo = customerRepo;
    this.saleRepo = saleRepo;
    this.promotionRepo = promotionRepo;
    this.ingestService = ingestService;
    this.ragService = ragService;
  }

  //@Override
  public void run(String... args) {
    if (!seedEnabled) return;

    // idempotent: only seed when empty
    if (productRepo.count() == 0) seedProducts();
    if (inventoryRepo.count() == 0) seedInventory();
    if (customerRepo.count() == 0 || saleRepo.count() == 0) seedCustomersAndSales();
    if (promotionRepo.count() == 0) seedPromotions();
    seedRagIfEmpty();

    System.out.println("[SEED] Done. Products=" + productRepo.count() +
                       " Inventory=" + inventoryRepo.count() +
                       " Customers=" + customerRepo.count() +
                       " Sales=" + saleRepo.count());
  }

  private void seedProducts() {
    productRepo.saveAll(List.of(
      makeProduct("SKU-302", "Aurora Pendant", "Jewelry", "22K gold pendant", new BigDecimal("82000"), new BigDecimal("52000")),
      makeProduct("SKU-221", "Heritage Chain 20in", "Jewelry", "22K chain", new BigDecimal("110000"), new BigDecimal("78000")),
      makeProduct("SKU-110", "Rhodium Silver Ring", "Jewelry", "Silver ring", new BigDecimal("3200"), new BigDecimal("1200")),
      makeProduct("SKU-507", "Platinum Band", "Jewelry", "Platinum ring", new BigDecimal("125000"), new BigDecimal("90000")),
      makeProduct("SKU-900", "Noir Tee", "Apparel", "100% cotton tee", new BigDecimal("1499"), new BigDecimal("650")),
      makeProduct("SKU-901", "Noir Tee (L)", "Apparel", "100% cotton tee L", new BigDecimal("1499"), new BigDecimal("650")),
      makeProduct("SKU-950", "Volt Powerbank 20k", "Electronics", "20000mAh, 18W", new BigDecimal("3299"), new BigDecimal("1950")),
      makeProduct("SKU-951", "Volt Powerbank 10k", "Electronics", "10000mAh, 15W", new BigDecimal("2199"), new BigDecimal("1250"))
    ));
    System.out.println("[SEED] Products inserted");
  }

  private Product makeProduct(String sku, String name, String cat, String attrs, BigDecimal mrp, BigDecimal cost) {
    Product p = new Product();
    p.setSku(sku);
    p.setName(name);
    p.setCategory(cat);
    p.setAttributesJson(attrs);
    p.setMrp(mrp);
    p.setCost(cost);
    return p;
  }

  private void seedInventory() {
    // uses IngestService.upsertInventoryRow so it’s consistent with your CSV ingest
    List<Map<String,Object>> rows = List.of(
      Map.of("sku","SKU-302","variant","18in|22K|Gold","qty",3,"reorderLevel",12,"location","LOC-A2"),
      Map.of("sku","SKU-221","variant","20in|22K|Gold","qty",2,"reorderLevel",10,"location","LOC-A1"),
      Map.of("sku","SKU-110","variant","Std|Silver|Rhodium","qty",5,"reorderLevel",20,"location","LOC-B4"),
      Map.of("sku","SKU-507","variant","Std|Platinum","qty",4,"reorderLevel",8,"location","LOC-D1"),
      Map.of("sku","SKU-900","variant","M|Black|Cotton","qty",18,"reorderLevel",12,"location","RACK-T1"),
      Map.of("sku","SKU-901","variant","L|Black|Cotton","qty",8,"reorderLevel",12,"location","RACK-T1"),
      Map.of("sku","SKU-950","variant","20000mAh|18W|Black","qty",22,"reorderLevel",10,"location","ELEC-03"),
      Map.of("sku","SKU-951","variant","10000mAh|15W|White","qty",5,"reorderLevel",12,"location","ELEC-03")
    );
    rows.forEach(ingestService::upsertInventoryRow);
    System.out.println("[SEED] Inventory inserted");
  }

  private void seedCustomersAndSales() {
    Customer c1 = new Customer(); c1.setPhoneHash("+919999000001"); c1.setTier("VIP"); c1.setLastVisitAt(OffsetDateTime.now().minusDays(2));
    Customer c2 = new Customer(); c2.setPhoneHash("+919999000002"); c2.setTier("STD"); c2.setLastVisitAt(OffsetDateTime.now().minusDays(7));
    customerRepo.saveAll(List.of(c1, c2));


    System.out.println("[SEED] Customers & Sales inserted");
  }

  private void seedPromotions() {
    Promotion p1 = new Promotion();
    p1.setTitle("Monsoon Sale 10% on slow movers");
    p1.setStatus("DRAFT");
    p1.setStartsAt(OffsetDateTime.now().minusDays(1));
    p1.setEndsAt(OffsetDateTime.now().plusDays(14));
    p1.setRuleJson("{\"type\":\"discount\",\"value\":10,\"scope\":\"slow_movers\"}");

    Promotion p2 = new Promotion();
    p2.setTitle("Jewelry Festive Promo");
    p2.setStatus("PLANNED");
    p2.setStartsAt(OffsetDateTime.now().plusDays(10));
    p2.setEndsAt(OffsetDateTime.now().plusDays(25));
    p2.setRuleJson("{\"type\":\"bundle\",\"value\":5,\"scope\":\"jewelry\"}");

    promotionRepo.saveAll(List.of(p1, p2));
    System.out.println("[SEED] Promotions inserted");
  }

  private void seedRagIfEmpty() {
    // Rough check: if no chunks yet, add some policy & catalog entries
    var demoPolicy = "Returns accepted within 7 days with original bill & tags. Electronics DOA returns in 3 days; 1-year brand warranty.";
    ragService.index("demo","policy","returns-1", demoPolicy, List.of("policy","returns"));
    ragService.index("demo","product","SKU-302", "Aurora Pendant – 22K gold, 18in chain, minimal design ideal for daily wear.", List.of("jewelry","gold","pendant"));
    ragService.index("demo","product","SKU-950", "Volt Powerbank 20k – 20000mAh, 18W fast charge, dual USB-A + USB-C.", List.of("electronics","powerbank"));
    System.out.println("[SEED] RAG chunks inserted");
  }
}
