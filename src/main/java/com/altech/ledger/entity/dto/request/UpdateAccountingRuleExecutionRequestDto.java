package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.OrderType;

import java.util.List;

public record UpdateAccountingRuleExecutionRequestDto(
    String name,
    String description,
    OrderType orderType,
    /** Bind this combination to a Brain/SDK eventType. Blank = unbound (not used at ingest). */
    String eventType,
    List<AccountingRuleRefDto> rules,
    String metadata
) {}
