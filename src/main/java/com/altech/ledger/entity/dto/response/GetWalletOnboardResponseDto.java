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
 * Product wallet onboarding response: wallet identity + primary account and full account-set.
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
    /** Same as associatedIdentifier (lookup key for GET /wallets/{ownerId}/…). */
    private String ownerId;
    private Currency currency;
    private WalletStatus status;
    /** Associated party id (CRM cust id, …). */
    private String associatedIdentifier;
    /** System that owns associatedIdentifier (e.g. CRM). */
    private String associatedFrom;
    /** Primary account (same as wallet.accountId). */
    private GetWalletAccountResponseDto account;
    /** Primary balance (convenience). */
    private GetWalletBalanceResponseDto balance;
    /** Full account-set opened under this wallet (MAIN + product lines). */
    private List<GetWalletAccountResponseDto> accounts;
}
