package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.json_context.WalletTierBand;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetWalletTierPolicyResponseDto {
    private Long id;
    private Boolean isEnabled;
    private String criterion;
    private String entity;
    private String type;
    private String subType;
    private String currency;
    private List<WalletTierBand> bands;
    private Instant createDt;
    private Instant updateDt;
}
