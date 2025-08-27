
package com.retailai.repo;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.retailai.model.Inventory;
public interface InventoryRepo extends JpaRepository<Inventory, Long> {
  @Query("select i from Inventory i where lower(i.product.sku) = lower(?1) and lower(i.variant) = lower(?2)")
  Optional<Inventory> findBySkuAndVariant(String sku, String variant);
  @Query("select i from Inventory i where i.qty <= i.reorderLevel")
  List<Inventory> findLowStock();
}
