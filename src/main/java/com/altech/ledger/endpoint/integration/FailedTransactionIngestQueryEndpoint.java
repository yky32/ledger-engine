package com.altech.ledger.endpoint.integration;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.usecase.integration.QueryFailedTransactionIngestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Failed transactional ingest <b>Query (R)</b> — ops visibility for D5 skip store.
 */
@RestController
@RequestMapping("/integrations/failed-transactions")
@RequiredArgsConstructor
public class FailedTransactionIngestQueryEndpoint {
    private final QueryFailedTransactionIngestUseCase queryFailedTransactionIngestUseCase;

    /**
     * Search failed / skipped ingest rows.
     * <pre>
     * GET /integrations/failed-transactions?status=OPEN&amp;failureCode=AGE&amp;limit=20
     * GET /integrations/failed-transactions?associatedIdentifier=01A12345678
     * </pre>
     */
    @GetMapping
    public Result<List<GetFailedTransactionIngestResponseDto>> search(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String associatedIdentifier,
        @RequestParam(required = false) String failureCode,
        @RequestParam(required = false) Integer limit
    ) {
        return R.success(queryFailedTransactionIngestUseCase.search(
            status, associatedIdentifier, failureCode, limit));
    }

    @GetMapping("/{id}")
    public Result<GetFailedTransactionIngestResponseDto> one(@PathVariable Long id) {
        return R.success(queryFailedTransactionIngestUseCase.one(id));
    }

    @GetMapping("/by-event/{eventId}")
    public Result<List<GetFailedTransactionIngestResponseDto>> byEvent(@PathVariable String eventId) {
        return R.success(queryFailedTransactionIngestUseCase.byEventId(eventId));
    }
}
