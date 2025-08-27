
package com.retailai.service;
import java.util.*; import org.springframework.stereotype.Service;
@Service public class SuggestionService {
  public List<Map<String,Object>> suggestPromotions(){
    return java.util.List.of(
      java.util.Map.of("title","Discount slow movers by 10%","impact","+12% conv.","type","Pricing"),
      java.util.Map.of("title","Reorder SKU-302 ×12","impact","Avoid OOS","type","Stock")
    );
  }
}
