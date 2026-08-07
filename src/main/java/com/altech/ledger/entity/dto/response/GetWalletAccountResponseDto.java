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
    private CoaType type;
    private Currency currency;
    private AccountStatus status;
    private boolean allowNegative;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;
    private int version;
}
