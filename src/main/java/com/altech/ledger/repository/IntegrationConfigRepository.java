package com.altech.ledger.repository;

import com.altech.ledger.entity.po.integration.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {

    @Query("select c from IntegrationConfig c where c.isActive = true order by c.id asc")
    java.util.List<IntegrationConfig> findActiveOrdered();

    default Optional<IntegrationConfig> findFirstActive() {
        return findActiveOrdered().stream().findFirst();
    }
}
