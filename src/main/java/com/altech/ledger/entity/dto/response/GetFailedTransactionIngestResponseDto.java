package com.altech.ledger.entity.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Failed / skipped transactional ingest row (ops query).
 */
public record GetFailedTransactionIngestResponseDto(
    Long id,
    String eventId,
    String associatedIdentifier,
    String eventType,
    BigDecimal amount,
    String currency,
    Instant occurredAt,
    String failureCode,
    String reason,
    String status,
    String rawPayload,
    Instant createDt,
    Instant updateDt
) {}
