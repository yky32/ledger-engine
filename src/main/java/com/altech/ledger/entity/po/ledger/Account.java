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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;

/** Chart-of-accounts bucket that holds live balances. */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uniqueAccountKey", columnNames = {
        "entity", "type", "sub_type", "main_account", "sub_account", "buffer", "currency"
    }),
    @UniqueConstraint(name = "uniqueMainAccountSubAccount", columnNames = {
        "main_account", "sub_account"
    }),
    @UniqueConstraint(name = "uk_account_full_number", columnNames = "full_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends AuditEntityWithIsActive {

    @Id
    @Column
    @GenericGenerator(name = "account_id_generator", type = SnowflakeIdGenerator.class)
    @GeneratedValue(generator = "account_id_generator")
    private Long id;

    @Column
    private String fullNumber;

    @Column
    private String entity;
    @Column
    private String type;
    @Column
    private String subType;

    @Column
    private String mainAccount; // more or less linked to client-specific identifier
    @Column
    private String subAccount;

    @Column
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

    @PrePersist
    void applyDefaults() {
        if (ledgerBalance == null) {
            ledgerBalance = BigDecimal.ZERO;
        }
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
    }
}
