package com.altech.ledger.entity.po.ingest;

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
    private String ownerId;

    @Column(length = 80)
    private String eventType;

    @Column(precision = 36, scale = 18)
    private BigDecimal amount;

    @Column(length = 16)
    private String currency;

    private Instant occurredAt;

    @Column(nullable = false, length = 40)
    private String failureCode;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String rawPayload;
}
