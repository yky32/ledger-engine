package com.altech.ledger.entity.dto.response;

import com.altech.core.constant.enu.Currency;
import com.altech.core.entity.dto.BaseResponseDto;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/** Product wallet response. Lookup key = {@code ownerId}. */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletOnboardResponseDto extends BaseResponseDto {
    private Long walletId;
    /** Query key — CRM / customer id. */
    private String ownerId;
    /** Customer-facing vanity / premium display (optional). */
    private String vanityCode;
    private Currency settlementCurrency;
    private WalletStatus status;
    private WalletAssociationType type;
    private WalletType walletType;
    private String name;
    private GetWalletAccountResponseDto account;
    private GetWalletBalanceResponseDto balance;
    private List<GetWalletAccountResponseDto> accounts;
}
