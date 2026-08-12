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
 * Ingest policy: how transactional webhooks behave (on/off + auto-wallet).
 * <p>
 * Typically <b>one active row</b>. API: {@code GET/PUT /ingest-policy}.
 * Example meaning — see docs/INGEST_POLICY.md.
 */
@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class IngestPolicy extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "ingest_policy_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "ingest_policy_id_generator")
    private Long id;

    /** Webhook ingest master switch. false → all events SKIPPED DISABLED. */
    @Column(nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    /**
     * After digestion gates pass: if customer has no wallet, create one automatically
     * (settlement + ensure currency books) then continue earn/burn.
     */
    @Column(nullable = false)
    private Boolean isAutoCreateWallet = Boolean.TRUE;

    /** Primary account currency when auto-creating wallet (e.g. HKD). */
    @Column(nullable = false, length = 16)
    private String autoWalletSettlementCurrency = "HKD";

    /** Extra book always opened on auto-create (e.g. LP for points). */
    @Column(nullable = false, length = 16)
    private String autoWalletEnsureCurrency = "LP";

    /** associatedFrom label on auto-created wallet. */
    @Column(length = 50)
    private String autoWalletAssociatedFrom = "CRM";

    /** Nickname prefix, e.g. "Auto " + CUST_ID. */
    @Column(length = 50)
    private String autoWalletNamePrefix = "Auto ";
}
