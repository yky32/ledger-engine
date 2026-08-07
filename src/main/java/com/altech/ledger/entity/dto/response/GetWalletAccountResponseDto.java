package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;

import com.altech.core.entity.dto.BaseResponseDto;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.enu.AccountStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Nested account slice inside onboarding responses (reference, type, balances).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletAccountResponseDto extends BaseResponseDto {
    private Long id;
    private String externalReference;
    private String name;
    /**
     * Free-form product line code (suffix after wallet base ref), e.g. {@code LOAN}, {@code CARD-A}.
     * Null when this is the primary account. Values are client/SDK-defined.
     */
    private String refCode;
    /** True when this is wallet.accountId (primary). */
    private Boolean primary;
    private CoaType type;
    private Currency currency;
    private AccountStatus status;
    private boolean allowNegative;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;
    private int version;
}
