package com.altech.ledger.repository;

import com.altech.ledger.entity.po.digestion.DigestionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DigestionRuleRepository extends JpaRepository<DigestionRule, Long> {
    Optional<DigestionRule> findByCode(String code);

    boolean existsByCode(String code);

    List<DigestionRule> findAllByOrderByPriorityAscIdAsc();

    @Query("select d from DigestionRule d where d.isEnabled = true and d.isActive = true order by d.priority asc, d.id asc")
    List<DigestionRule> findAllEnabledOrdered();
}
