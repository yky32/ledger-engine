package com.altech.ledger.repository;

import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, Long> {
    Optional<SystemConfiguration> findByTargetAndScope(String target, String scope);
}
