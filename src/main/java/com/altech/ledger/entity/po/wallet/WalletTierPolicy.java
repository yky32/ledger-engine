package com.altech.ledger.entity.po.wallet;

import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.WalletTierCriterion;
import com.altech.ledger.entity.json_context.WalletTierBand;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Ops config for wallet membership bands. One effective row (Door-shaped).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class WalletTierPolicy extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "wallet_tier_policy_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "wallet_tier_policy_id_generator")
    private Long id;

    @Column(nullable = false)
    private Boolean isEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTierCriterion criterion;

    @Column(nullable = false)
    private String entity;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String subType;

    @Column(nullable = false)
    private String currency;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<WalletTierBand> bands = new ArrayList<>();

    @PrePersist
    void applyDefaults() {
        if (isEnabled == null) {
            isEnabled = Boolean.TRUE;
        }
        if (criterion == null) {
            criterion = WalletTierCriterion.LEDGER_BALANCE;
        }
        if (entity == null) {
            entity = "01";
        }
        if (type == null) {
            type = "01";
        }
        if (subType == null) {
            subType = "01";
        }
        if (currency == null) {
            currency = "LP";
        }
    }
}
