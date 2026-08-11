package com.altech.ledger.entity.po.integration;

import com.altech.core.entity.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persisted failed / skipped transactional ingest (eligibility gate, no wallet, errors).
 * Success path does not write here.
 */
@Entity
@Table(name = "failed_transaction_ingest")
@Getter
@Setter
@NoArgsConstructor
public class FailedTransactionIngest extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String eventId;

    @Column(length = 100)
    private String associatedIdentifier;

    @Column(length = 80)
    private String eventType;

    @Column(precision = 36, scale = 18)
    private BigDecimal amount;

    @Column(length = 16)
    private String currency;

    private Instant occurredAt;

    /** Stable code: AMOUNT | CURRENCY | AGE | NO_RULE | NO_WALLET | DISABLED | ERROR | … */
    @Column(nullable = false, length = 40)
    private String failureCode;

    @Column(nullable = false, length = 500)
    private String reason;

    /** OPEN | REVIEWED | REPLAYED */
    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String rawPayload;
}
