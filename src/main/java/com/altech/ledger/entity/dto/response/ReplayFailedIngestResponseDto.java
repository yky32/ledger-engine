package com.altech.ledger.entity.dto.response;

import com.altech.ledger.entity.dto.ingest.IngestionResult;

/**
 * Outcome of replaying a failed ingest row through the normal webhook pipeline.
 */
public record ReplayFailedIngestResponseDto(
    Long failedIngestId,
    String previousStatus,
    String status,
    IngestionResult ingestion
) {}
