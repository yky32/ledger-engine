package com.altech.ledger.endpoint.integration;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.integration.LedgerLegDto;
import com.altech.ledger.usecase.integration.IngestTransactionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Query double-entry legs for loyalty / ledger movements (read-only).
 */
@RestController
@RequestMapping("/integrations/ledger-entries")
@RequiredArgsConstructor
public class LedgerEntryQueryEndpoint {
    private final IngestTransactionUseCase ingestTransactionUseCase;

    @GetMapping
    public Result<List<LedgerLegDto>> byMovementId(@RequestParam Long movementId) {
        return R.success(ingestTransactionUseCase.legsForMovementId(movementId));
    }

    @GetMapping("/by-event/{eventId}")
    public Result<List<LedgerLegDto>> byEventId(
        @PathVariable String eventId,
        @RequestParam(required = false) String operation
    ) {
        return R.success(ingestTransactionUseCase.legsForEventId(eventId, operation));
    }
}
