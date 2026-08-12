package com.altech.ledger.entity.po.ingest;

import com.altech.core.entity.AuditEntity;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FailedTransactionIngest extends AuditEntity {

    @Id
    @Column
    @GenericGenerator(name = "failed_txn_ingest_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "failed_txn_ingest_id_generator")
    private Long id;

    @Column(nullable = false)
    private String eventId;

    @Column
    private String ownerId;

    @Column
    private String eventType;

    @Column(precision = 36, scale = 18)
    private BigDecimal amount;

    @Column
    private String currency;

    @Column
    private Instant occurredAt;

    @Column(nullable = false)
    private String failureCode;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object rawPayload;

    @PrePersist
    void applyDefaults() {
        if (status == null) {
            status = "OPEN";
        }
    }
}
