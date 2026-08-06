package com.altech.ledger.repository;

import com.altech.ledger.entity.po.accounting.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, Long> {
}
