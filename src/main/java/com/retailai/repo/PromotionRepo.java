
package com.retailai.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import com.retailai.model.Promotion;
public interface PromotionRepo extends JpaRepository<Promotion, Long> { }
