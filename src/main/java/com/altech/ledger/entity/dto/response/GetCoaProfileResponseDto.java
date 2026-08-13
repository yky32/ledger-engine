package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

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
    private Map<String, Object> bindings;
    private Instant createDt;
    private Instant updateDt;
}
