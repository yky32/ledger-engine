package com.altech.ledger.entity.po.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
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
 */
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "uniqueAccountKey", columnNames = {
            "entity", "type", "sub_type", "main_account", "sub_account", "buffer", "currency"
        }),
        @UniqueConstraint(name = "uniqueMainAccountSubAccount", columnNames = {
            "main_account", "sub_account"
        }),
        @UniqueConstraint(name = "uk_account_full_number", columnNames = "full_number")
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

    private String fullNumber;

    private String entity;
    private String type;
    private String subType;

    private String mainAccount;
    private String subAccount;

    private String buffer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowNegative = false;
}
