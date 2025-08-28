
package com.retailai.controller;

import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.retailai.model.Inventory;
import com.retailai.model.Sale;
import com.retailai.service.IngestService;

@RestController
public class WebhookController {
    private final IngestService ingest;

    public WebhookController(IngestService s) {
        this.ingest = s;
    }

    @PostMapping("/webhooks/pos-sale")
    public ResponseEntity<?> posSale(@RequestBody java.util.Map<String, Object> payload) {
        Sale sale = ingest.ingestSale(payload);
        return ResponseEntity.ok(java.util.Map.of("saleId", sale.getId(), "invoice", sale.getInvoiceNo(), "total", sale.getTotal()));
    }

    @PostMapping("/api/ingest/inventory-row")
    public ResponseEntity<?> inventoryRow(@RequestBody java.util.Map<String, Object> row) {
        Inventory inv = ingest.upsertInventoryRow(row);
        return ResponseEntity.ok(java.util.Map.of("id", inv.getId(), "sku", inv.getProduct().getSku(), "variant", inv.getVariant(), "qty", inv.getQty()));
    }

    @GetMapping("/api/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "ok");
    }
    @PostMapping("/webhooks/handleCall")
    public ResponseEntity<?> handleCall(@RequestBody java.util.Map<String, Object> payload) {
        Sale sale = ingest.ingestSale(payload);
        return ResponseEntity.ok(java.util.Map.of("saleId", sale.getId(), "invoice", sale.getInvoiceNo(), "total", sale.getTotal()));
    }
}
