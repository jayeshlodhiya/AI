
package com.retailai.repo;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
import com.retailai.model.Product;
public interface ProductRepo extends JpaRepository<Product, Long> {
  Optional<Product> findBySku(String sku);
  List<Product> findByNameContainingIgnoreCase(String q);
}
