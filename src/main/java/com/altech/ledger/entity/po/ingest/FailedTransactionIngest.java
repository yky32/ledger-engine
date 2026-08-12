package com.altech.ledger.entity.po.ingest;

import com.altech.core.entity.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FailedTransactionIngest extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    private String ownerId;

    private String eventType;

    @Column(precision = 36, scale = 18)
    private BigDecimal amount;

    private String currency;

    private Instant occurredAt;

    @Column(nullable = false)
    private String failureCode;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String rawPayload;
}
