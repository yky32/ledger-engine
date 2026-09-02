package com.altech.ledger.entity.po.ingest;

import com.altech.core.entity.AuditEntityWithIsActive;
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

/** Ingest policy — webhook door. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class IngestPolicy extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "ingest_policy_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "ingest_policy_id_generator")
    private Long id;

    @Column(nullable = false)
    private Boolean isEnabled;

    @Column(nullable = false)
    private Boolean isAutoCreateWallet;

    @Column(nullable = false)
    private String autoWalletSettlementCurrency;

    @Column(nullable = false)
    private String autoWalletEnsureCurrency;

    @Column
    private String autoWalletAssociatedFrom;

    @Column
    private String autoWalletNamePrefix;

    /**
     * COA profile for Door lazy onboard (product stream), e.g. STREAM_A.
     * Blank → CUSTOMER_CUST_{reward ccy} (01-01-01). Overridden by event metadata.coaProfileCode.
     */
    @Column
    private String autoWalletCoaProfileCode;

    /**
     * Entry factors (JSONB). Array (AND) or FactorSet object. Empty/null = only isEnabled.
     * See docs/FACTORS.md.
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Object entryFactors;

    @PrePersist
    void applyDefaults() {
        if (isEnabled == null) {
            isEnabled = Boolean.TRUE;
        }
        if (isAutoCreateWallet == null) {
            isAutoCreateWallet = Boolean.TRUE;
        }
        if (autoWalletSettlementCurrency == null) {
            autoWalletSettlementCurrency = "HKD";
        }
        if (autoWalletEnsureCurrency == null) {
            autoWalletEnsureCurrency = "LP";
        }
        if (autoWalletAssociatedFrom == null) {
            autoWalletAssociatedFrom = "CRM";
        }
        if (autoWalletNamePrefix == null) {
            autoWalletNamePrefix = "Auto ";
        }
        if (autoWalletCoaProfileCode != null) {
            autoWalletCoaProfileCode = autoWalletCoaProfileCode.trim().toUpperCase();
            if (autoWalletCoaProfileCode.isEmpty()) {
                autoWalletCoaProfileCode = null;
            }
        }
    }
}
