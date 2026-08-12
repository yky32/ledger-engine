package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.dto.BaseResponseDto;
import com.altech.ledger.entity.enu.WalletStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Product wallet onboarding response: wallet identity + primary account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletOnboardResponseDto extends BaseResponseDto {
    private Long walletId;
    /** Same as associatedIdentifier (lookup key). */
    private String ownerId;
    /** Wallet default settlement currency. */
    private Currency settlementCurrency;
    private WalletStatus status;
    /** CRM / customer id — same as ownerId. */
    private String associatedIdentifier;
    /** Optional display name. */
    private String name;
    /** Primary account (same as wallet.accountId). */
    private GetWalletAccountResponseDto account;
    /** Primary balance (convenience). */
    private GetWalletBalanceResponseDto balance;
    private List<GetWalletAccountResponseDto> accounts;
}
