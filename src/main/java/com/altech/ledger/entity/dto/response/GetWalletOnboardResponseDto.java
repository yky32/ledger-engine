package com.altech.ledger.entity.dto.response;

import com.altech.core.entity.dto.BaseResponseDto;
import com.altech.ledger.entity.enu.WalletStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Product wallet onboarding response: wallet identity + linked account and balances.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletOnboardResponseDto extends BaseResponseDto {
    private Long walletId;
    private String alias;
    private String ownerId;
    private String currency;
    private WalletStatus status;
    private String externalId;
    private String externalType;
    private GetWalletAccountResponseDto account;
    private GetWalletBalanceResponseDto balance;
}
