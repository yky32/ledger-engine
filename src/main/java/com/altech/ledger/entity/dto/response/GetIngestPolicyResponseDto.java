package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetIngestPolicyResponseDto {
    private Long id;
    private Boolean isEnabled;
    private Boolean isAutoCreateWallet;
    private String autoWalletSettlementCurrency;
    private String autoWalletEnsureCurrency;
    private String autoWalletAssociatedFrom;
    private String autoWalletNamePrefix;
    private String autoWalletCoaProfileCode;
    private Instant createDt;
    private Instant updateDt;
}
