package com.altech.ledger.entity.po.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.AuditEntityWithIsActive;
import com.altech.core.utils.generator.id.SnowflakeIdGenerator;
import com.altech.ledger.entity.enu.AccountStatus;
import jakarta.persistence.*;
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
 * One row = one currency balance (ledger + available). Product wallets and program
 * pools both point here. Structure fields (entity/type/main/sub/…) form the COA key;
 * {@link #fullNumber} is the unique COA key used for lookups.
 * Balances change only through movement execution — not by free-form updates.
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

    @Column(length = 200)
    private String fullNumber;

    // === Chart of Account - COA
    @Column(length = 50)
    private String entity;
    @Column(length = 50)
    private String type;
    @Column(length = 50)
    private String subType;

    // === ASSOCIATION
    @Column(length = 100)
    private String mainAccount;
    @Column(length = 200)
    private String subAccount;
    // === ASSOCIATION

    @Column(length = 50)
    private String buffer;

    /** Unit of balance: fiat, loyalty point (LP), or crypto — see {@link Currency}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Currency currency;
    // === Chart of Account - COA

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal availableBalance = BigDecimal.ZERO;
    // === BALANCE

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    /** Posting may refuse negative signed balance when false. */
    @Builder.Default
    @Column(nullable = false)
    private boolean allowNegative = false;
}
