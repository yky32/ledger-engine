package com.altech.ledger.entity.po.ingest;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * Ingest policy — webhook door (global on/off + auto-wallet).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class IngestPolicy extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "ingest_policy_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "ingest_policy_id_generator")
    private Long id;

    @Column(nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    @Column(nullable = false)
    private Boolean isAutoCreateWallet = Boolean.TRUE;

    @Column(nullable = false)
    private String autoWalletSettlementCurrency = "HKD";

    @Column(nullable = false)
    private String autoWalletEnsureCurrency = "LP";

    private String autoWalletAssociatedFrom = "CRM";

    private String autoWalletNamePrefix = "Auto ";
}
