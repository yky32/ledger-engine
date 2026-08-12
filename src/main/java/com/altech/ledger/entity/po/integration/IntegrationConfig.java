package com.altech.ledger.entity.po.integration;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

/**
 * Runtime integration / auto-wallet policy (single active row pattern).
 * <p>
 * Editable via {@code /integrations/config} — no restart. Env {@code IntegrationProperties}
 * is boot fallback only when table empty.
 */
@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class IntegrationConfig extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "integration_config_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "integration_config_id_generator")
    private Long id;

    /** Master kill-switch for webhook integration. */
    @Column(nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    /** After gates: auto-create wallet if missing. */
    @Column(nullable = false)
    private Boolean isAutoCreateWallet = Boolean.TRUE;

    @Column(nullable = false, length = 16)
    private String autoWalletSettlementCurrency = "HKD";

    @Column(nullable = false, length = 16)
    private String autoWalletEnsureCurrency = "LP";

    @Column(length = 50)
    private String autoWalletAssociatedFrom = "CRM";

    @Column(length = 50)
    private String autoWalletNamePrefix = "Auto ";
}
