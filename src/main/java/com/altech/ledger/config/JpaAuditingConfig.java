package com.altech.ledger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables JPA auditing for {@code AuditEntity} ({@code @CreatedDate} / {@code @LastModifiedDate}).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // Standalone: no auth principal yet — stamp system.
        return () -> Optional.of("system");
    }
}
