package com.retailai.repo;



import com.retailai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  // Look up by SKU (code)
  Optional<Product> findBySku(String sku);
}
