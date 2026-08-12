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
 * Failed transactional ingest Query — filters via query params only.
 */
@RestController
@RequestMapping("/integrations/failed-transactions")
@RequiredArgsConstructor
public class FailedTransactionIngestQueryEndpoint {
    private final QueryFailedTransactionIngestUseCase queryFailedTransactionIngestUseCase;

    /**
     * GET /integrations/failed-transactions?status=OPEN&amp;ownerId=01A…
     * GET /integrations/failed-transactions?eventId=txn-xxx
     */
    @GetMapping
    public Result<List<GetFailedTransactionIngestResponseDto>> search(
        @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "id", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String ownerId,
        @RequestParam(required = false) String failureCode,
        @RequestParam(required = false) String eventId
    ) {
        if (eventId != null && !eventId.isBlank()) {
            return R.success(queryFailedTransactionIngestUseCase.byEventId(eventId.trim()));
        }
        Page<GetFailedTransactionIngestResponseDto> page = queryFailedTransactionIngestUseCase.search(
            status, ownerId, failureCode, pageable);
        return R.success(page.getContent(), Pagination.create(page));
    }

    @GetMapping("/{id}")
    public Result<GetFailedTransactionIngestResponseDto> one(@PathVariable Long id) {
        return R.success(queryFailedTransactionIngestUseCase.one(id));
    }
}
