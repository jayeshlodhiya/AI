package com.retailai.controller;

import com.retailai.model.Product;
import com.retailai.service.CatalogService;
import com.retailai.service.PitchService;
import com.retailai.service.SalesHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GET /api/vision/lookup?code=SKU-302
 * - code can be a SKU ("SKU-302") or numeric product ID ("1")
 * - returns product info + sales history (computed from sale_items by product_id) + a suggested sales pitch
 */
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/vision")
public class VisionLookupController {

  private final CatalogService catalogService;
  private final SalesHistoryService salesHistoryService;
  private final PitchService pitchService;

  public VisionLookupController(CatalogService catalogService,
                                SalesHistoryService salesHistoryService,
                                PitchService pitchService) {
    this.catalogService = catalogService;
    this.salesHistoryService = salesHistoryService;
    this.pinchCheck(); // no-op; keeps constructor tidy if you want to add health checks later
    this.pitchService = pitchService;
  }

  // Optional internal check (can be removed)
  private void pinchCheck() { /* no-op */ }

  @GetMapping("/lookup")
  public ResponseEntity<Map<String, Object>> lookup(
    @RequestParam("code") String code
  ) {
    // 1) Resolve product by scanned code (SKU or numeric ID)
    Product p = catalogService.findByCode(code);
    System.out.println("Product info: "+p);
    if (p == null) {
      return ResponseEntity.ok(Map.of(
        "name", "Unknown product",
        "tip", "Scan a valid SKU QR or shelf code.",
        "history", Map.of()
      ));
    }

    // 2) Summaries from sale_items by product_id
    Map<String, Object> history = salesHistoryService.summarizeByProductId(p.getId());

    // 3) Sales pitch (rule-based or LLM-backed inside PitchService)
    String pitchText = pitchService.craftPitch(p, history);

    // 4) Build overlay payload (keep it compact for realtime UI)
    return ResponseEntity.ok(Map.of(
      "id", p.getId(),
      "sku", safe(p.getSku()),
      "name", safe(p.getName()),
      "category", safe(p.getCategory()),
      "mrp", p.getMrp(),                       // BigDecimal (serialize as string/number)
      "attrs", safe(p.getAttributesJson()),    // e.g., "22K gold pendant"
      "history", history,                      // { units_30d, revenue_30d, avg_selling_price_last10, trend_last10, recent[] }
      "tip", pitchText                         // short suggestion for salesperson
    ));
  }

  // --- Helpers ---
  private static String safe(String s) { return s == null ? "" : s; }
}
