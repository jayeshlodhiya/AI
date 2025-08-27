
package com.retailai.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import com.retailai.model.Sale;
public interface SaleRepo extends JpaRepository<Sale, Long> { }
