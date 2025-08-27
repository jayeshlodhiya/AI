
package com.retailai.controller;
import java.util.*; import java.util.stream.*; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import com.retailai.model.Inventory; import com.retailai.service.InventoryService;
@RestController @RequestMapping("/api/inventory")
public class InventoryController {
  private final InventoryService svc; public InventoryController(InventoryService s){ this.svc=s; }
  @GetMapping("/availability") public ResponseEntity<?> availability(@RequestParam String sku, @RequestParam String variant){
    return svc.availability(sku, variant).<ResponseEntity<?>>map(i -> ResponseEntity.ok(svc.toDto(i))).orElse(ResponseEntity.status(404).body(java.util.Map.of("message","Not found")));
  }
  @GetMapping("/low-stock") public java.util.List<java.util.Map<String,Object>> lowStock(){ return svc.lowStock().stream().map(svc::toDto).collect(Collectors.toList()); }
}
