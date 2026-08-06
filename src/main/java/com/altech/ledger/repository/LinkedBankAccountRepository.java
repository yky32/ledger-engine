package com.altech.ledger.repository;

import com.altech.ledger.entity.po.LinkedBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccount, Long> {
    List<LinkedBankAccount> findByTenantId(Long tenantId);
}
