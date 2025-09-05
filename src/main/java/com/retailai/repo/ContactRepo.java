package com.retailai.repo;

import com.retailai.model.ContactLead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepo extends JpaRepository<ContactLead, Long> {

}
