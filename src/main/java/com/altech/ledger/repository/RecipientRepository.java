package com.altech.ledger.repository;

import com.altech.ledger.entity.po.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    List<Recipient> findByTenantId(Long tenantId);
}
