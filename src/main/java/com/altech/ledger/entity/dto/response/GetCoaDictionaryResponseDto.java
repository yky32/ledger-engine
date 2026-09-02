package com.altech.ledger.entity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetCoaDictionaryResponseDto(
    Long id,
    String kind,
    String code,
    String name,
    String definition,
    String example,
    String side,
    Instant createDt,
    Instant updateDt
) {}
