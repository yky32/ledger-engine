package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class GetCoaProfileResponseDto {
    private Long id;
    private String code;
    private String name;
    /** Bound transaction / eventType code (unique when set). */
    private String transactionCode;
    private Boolean isDefault;
    private Boolean isEnabled;
    private String entity;
    private String type;
    private String subType;
    private String buffer;
    @JsonProperty("currency")
    @JsonAlias("lpCurrency")
    private String currency;
    private Boolean poolAllowNegative;
    /** House COA: company wallet these books live on. */
    private Long walletId;
    private Instant createDt;
    private Instant updateDt;
}
