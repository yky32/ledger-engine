package com.altech.ledger.entity.po.ingest;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

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
    }
}
