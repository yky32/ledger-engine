package com.altech.ledger.entity.dto.response;

import java.util.List;

/** createIfNotFound UA posting sequences + legs. */
public record GetAccountingRulesBundleDto(
    List<GetAccountingRuleResponseDto> rules,
    List<GetAccountingRuleExecutionResponseDto> executions
) {}
