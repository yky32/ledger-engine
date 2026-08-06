package com.altech.ledger.repository;

import com.altech.ledger.entity.po.accounting.RuleExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuleExecutionRepository extends JpaRepository<RuleExecution, Long> {
    Optional<RuleExecution> findByName(String name);
}
