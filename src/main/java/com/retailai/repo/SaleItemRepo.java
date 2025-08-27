
package com.retailai.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import com.retailai.model.SaleItem;
public interface SaleItemRepo extends JpaRepository<SaleItem, Long> { }
