package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetDigestionRuleResponseDto {
    private Long id;
    private String code;
    private String name;
    private String eventType;
    private String operation;
    private Boolean isEnabled;
    private Integer priority;
    private BigDecimal minAmount;
    private List<String> eligibleCurrencies;
    private Integer maxAgeDays;
    private String pointCurrency;
    /** JSON config object, e.g. {@code {"type":"RATE","rate":0.01}}. */
    private Object formula;
    private String processType;
    private Instant createDt;
    private Instant updateDt;
}
