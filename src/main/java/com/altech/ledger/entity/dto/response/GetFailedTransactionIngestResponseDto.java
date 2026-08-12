package com.altech.ledger.entity.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record GetFailedTransactionIngestResponseDto(
    Long id,
    String eventId,
    String ownerId,
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
