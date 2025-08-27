
package com.retailai.controller;
import java.util.*; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import com.retailai.model.Promotion; import com.retailai.repo.PromotionRepo; import com.retailai.service.SuggestionService;
@RestController @RequestMapping("/api")
public class PromotionController {
  private final PromotionRepo repo; private final SuggestionService sug; public PromotionController(PromotionRepo r, SuggestionService s){ this.repo=r; this.sug=s; }
  @PostMapping("/ai/suggest-promotion") public java.util.List<java.util.Map<String,Object>> suggestPromotion(){ return sug.suggestPromotions(); }
  @PostMapping("/promotions/activate") public ResponseEntity<?> activate(@RequestBody java.util.Map<String,Object> body){
    Promotion p = new Promotion(); p.setTitle(String.valueOf(body.getOrDefault("title","Promo"))); p.setStatus("ACTIVE"); p = repo.save(p);
    return ResponseEntity.ok(java.util.Map.of("promotionId", p.getId(), "status", p.getStatus()));
  }
}
