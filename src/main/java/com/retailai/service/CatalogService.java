package com.retailai.service;

import com.retailai.model.Product;
import com.retailai.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CatalogService {

  private final ProductRepository repo;

  public CatalogService(ProductRepository repo) {
    this.repo = repo;
  }

  /**
   * Resolve a scanned code to a Product.
   * - Tries SKU first (e.g., "SKU-302")
   * - If not found and the code is numeric, tries ID (e.g., "1")
   */
  public Product findByCode(String code) {
    if (code == null || code.isBlank()) return null;

    // 1) Try SKU (string)
    Optional<Product> bySku = repo.findBySku(code.trim());
    if (bySku.isPresent()) return bySku.get();

    // 2) If code looks numeric, try ID
    try {
      long id = Long.parseLong(code.trim());
      return repo.findById(id).orElse(null);
    } catch (NumberFormatException ignore) {
      // not a numeric ID
    }
    return null;
  }

  // Optional convenience methods if you need them elsewhere
  public Optional<Product> findBySku(String sku) {
    return repo.findBySku(sku);
  }

  public Optional<Product> findById(Long id) {
    return repo.findById(id);
  }
}
