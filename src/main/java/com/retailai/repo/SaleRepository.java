package com.retailai.repo;



import java.util.Optional;

import com.retailai.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {
  Optional<Sale> findByInvoiceNo(String invoiceNo);
}
