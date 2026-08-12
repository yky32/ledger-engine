package com.altech.ledger.endpoint.ingest;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.usecase.ingest.QueryFailedTransactionIngestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Failed transactional ingest <b>Query (R)</b> — ops visibility for D5 skip store.
 * <p>
 * Lookup filters are query params only — no {@code /by-*} paths.
 * Pagination: tgt.profile style 1-based {@link PageableDefault}.
 */
@RestController
@RequestMapping("/integrations/failed-transactions")
@RequiredArgsConstructor
public class FailedTransactionIngestQueryEndpoint {
    private final QueryFailedTransactionIngestUseCase queryFailedTransactionIngestUseCase;

    /**
     * Search failed / skipped ingest rows.
     * <pre>
     * GET /integrations/failed-transactions?status=OPEN&amp;failureCode=AGE
     * GET /integrations/failed-transactions?associatedIdentifier=01A12345678
     * GET /integrations/failed-transactions?eventId=txn-xxx
     * </pre>
     */
    @GetMapping
    public Result<List<GetFailedTransactionIngestResponseDto>> search(
        @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "id", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String associatedIdentifier,
        @RequestParam(required = false) String failureCode,
        @RequestParam(required = false) String eventId
    ) {
        if (eventId != null && !eventId.isBlank()) {
            return R.success(queryFailedTransactionIngestUseCase.byEventId(eventId.trim()));
        }
        Page<GetFailedTransactionIngestResponseDto> page = queryFailedTransactionIngestUseCase.search(
            status, associatedIdentifier, failureCode, pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }

    @GetMapping("/{id}")
    public Result<GetFailedTransactionIngestResponseDto> one(@PathVariable Long id) {
        return R.success(queryFailedTransactionIngestUseCase.one(id));
    }
}
