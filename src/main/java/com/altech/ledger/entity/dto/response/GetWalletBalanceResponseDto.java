package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Nested balance slice for onboarding (ledger + available on the primary account).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletBalanceResponseDto {
    private Long accountId;
    private Currency currency;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;
}
