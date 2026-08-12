package com.altech.ledger.entity.dto.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.MovementDirection;

import java.math.BigDecimal;

/** One double-entry leg on a settled movement. */
public record LedgerLegDto(
    Long entryId,
    Long accountId,
    MovementDirection direction,
    BigDecimal amount,
    Currency currency
) {}
