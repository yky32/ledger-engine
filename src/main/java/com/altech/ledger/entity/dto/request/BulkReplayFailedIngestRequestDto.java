package com.altech.ledger.entity.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkReplayFailedIngestRequestDto(
    @NotEmpty @Size(max = 50) List<Long> ids
) {}
