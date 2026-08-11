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
    private String alias;
    /** Same as associatedIdentifier (lookup key for GET /wallets/{ownerId}). */
    private String ownerId;
    /** Wallet default settlement currency. */
    private Currency settlementCurrency;
    private WalletStatus status;
    /** Associated party id (CRM cust id, …). */
    private String associatedIdentifier;
    /** System that owns associatedIdentifier (e.g. CRM). */
    private String associatedFrom;
    /** Primary account (same as wallet.accountId). */
    private GetWalletAccountResponseDto account;
    /** Primary balance (convenience). */
    private GetWalletBalanceResponseDto balance;
    /** Account-set under this wallet (primary only at onboard today). */
    private List<GetWalletAccountResponseDto> accounts;
    /** Phase A: AccountSets with nested CoA (HKD + LP roles). */
    private List<GetAccountSetResponseDto> accountSets;
}
