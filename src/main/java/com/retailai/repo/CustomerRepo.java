
package com.retailai.repo;
import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
import com.retailai.model.Customer;
public interface CustomerRepo extends JpaRepository<Customer, Long> {
  Optional<Customer> findByPhoneHash(String phoneHash);
}
