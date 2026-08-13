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
public class GetCoaProfileResponseDto {
    private Long id;
    private String code;
    private String name;
    private Boolean isDefault;
    private Boolean isEnabled;
    private String entity;
    private String type;
    private String subType;
    private String buffer;
    private String lpCurrency;
    private Boolean poolAllowNegative;
    private Instant createDt;
    private Instant updateDt;
}
