package com.altech.ledger.entity.po.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.AccountRole;
import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

/**
 * Chart-of-accounts bucket that holds live balances.
 * <p>
 * One row = one (currency, role) book under an {@link AccountSet}.
 * Phase A adds {@link #accountSetId} + {@link #accountRole}; Phase B journals post here.
 */
@Entity
@Table(
    name = "account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniqueAccountKey", columnNames = {
            "entity", "type", "sub_type", "main_account", "sub_account", "buffer", "currency"
        }),
        @UniqueConstraint(name = "uniqueMainAccountSubAccount", columnNames = {
            "main_account", "sub_account"
        }),
        @UniqueConstraint(name = "uk_account_full_number", columnNames = "full_number"),
        @UniqueConstraint(name = "uk_account_set_ccy_role", columnNames = {
            "account_set_id", "currency", "account_role"
        })
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends AuditEntityWithIsActive {

    @Id
    @GenericGenerator(name = "account_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "account_id_generator")
    private Long id;

    @Column(length = 200)
    private String fullNumber;

    @Column(length = 50)
    private String entity;
    @Column(length = 50)
    private String type;
    @Column(length = 50)
    private String subType;

    @Column(length = 100)
    private String mainAccount;
    @Column(length = 200)
    private String subAccount;

    @Column(length = 50)
    private String buffer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency currency;

    /** Owning account set (Phase A). Nullable only for legacy rows pre-backfill. */
    @Column(name = "account_set_id")
    private Long accountSetId;

    /** Product role within the set (AVAILABLE / HELD / …). */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", length = 20)
    private AccountRole accountRole;

    /** Optional display label (e.g. Available HKD). */
    @Column(length = 200)
    private String displayName;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowNegative = false;
}
