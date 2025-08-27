
package com.retailai.controller;
import java.util.*; import org.springframework.web.bind.annotation.*; import com.retailai.model.Product; import com.retailai.repo.ProductRepo;
@RestController @RequestMapping("/api/products")
public class ProductController {
  private final ProductRepo repo; public ProductController(ProductRepo r){ this.repo=r; }
  @GetMapping("/search") public List<Product> search(@RequestParam String q){ return repo.findByNameContainingIgnoreCase(q); }
}
