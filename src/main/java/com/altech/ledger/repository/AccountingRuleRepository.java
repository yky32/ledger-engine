package com.altech.ledger.repository;

import com.altech.ledger.entity.po.accounting.AccountingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountingRuleRepository extends JpaRepository<AccountingRule, Long> {
    Optional<AccountingRule> findByName(String name);
}
