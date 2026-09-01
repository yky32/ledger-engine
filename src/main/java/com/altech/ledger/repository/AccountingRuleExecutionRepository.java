package com.altech.ledger.repository;

import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountingRuleExecutionRepository extends JpaRepository<AccountingRuleExecution, Long> {
    Optional<AccountingRuleExecution> findByName(String name);

    Optional<AccountingRuleExecution> findByEventType(String eventType);

    Optional<AccountingRuleExecution> findFirstByOrderTypeAndEventTypeIsNull(OrderType orderType);
}
