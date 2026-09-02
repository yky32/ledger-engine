package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;

import com.altech.core.entity.dto.BaseResponseDto;
import com.altech.ledger.entity.enu.AccountStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Nested account slice on wallet APIs. COA columns match {@code account} (camelCase JSON).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletAccountResponseDto extends BaseResponseDto {
    private Long id;
    private Long walletId;
    /** Same as DB column {@code account.full_number} (numeric COA key). */
    private String fullNumber;
    private String name;
    /**
     * Free-form product line code (suffix after wallet base ref), e.g. {@code LOAN}, {@code CARD-A}.
     * Null when this is the primary account. Values are client/SDK-defined.
     */
    private String refCode;
    /** True when this is wallet.accountId (primary). */
    private Boolean primary;
    /** COA {@code account.entity}. */
    private String entity;
    /** COA {@code account.type} (digit segment, not ASSET/LIABILITY). */
    private String type;
    /** COA {@code account.sub_type}. */
    private String subType;
    /** COA {@code account.main_account}. */
    private String mainAccount;
    /** COA {@code account.buffer}. */
    private String buffer;
    private Currency currency;
    private AccountStatus status;
    private boolean allowNegative;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;
    private int version;
}
