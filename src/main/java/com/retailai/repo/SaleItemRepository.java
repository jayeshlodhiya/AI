// src/main/java/com/retailai/repo/SaleItemRepository.java
package com.retailai.repo;


import com.retailai.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

  int countByProductIdAndSale_CreatedAtBetween(Long productId, LocalDateTime start, LocalDateTime end);
  int countByProductId(Long productId);
  @Query("SELECT AVG(s.price) FROM SaleItem s WHERE s.product.id = :productId")
  Double findAveragePriceByProductId(Long productId);
}
