package com.altech.ledger.endpoint.ingest;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.BulkReplayFailedIngestRequestDto;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.entity.dto.response.ReplayFailedIngestResponseDto;
import com.altech.ledger.usecase.ingest.ReplayFailedTransactionIngestUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Failed-ingest CUD ops (review / replay). Query stays on {@link FailedTransactionIngestQueryEndpoint}.
 */
@RestController
@RequestMapping("/integrations/failed-transactions")
@RequiredArgsConstructor
public class FailedTransactionIngestCudEndpoint {
    private final ReplayFailedTransactionIngestUseCase replayFailedTransactionIngestUseCase;

    /** Mark OPEN → REVIEWED (ops ack without re-running). */
    @PostMapping("/{id}/review")
    public Result<GetFailedTransactionIngestResponseDto> review(@PathVariable Long id) {
        return R.success(replayFailedTransactionIngestUseCase.markReviewed(id));
    }

    /**
     * Re-run stored payload through webhook ingest pipeline (no duplicate fail row on skip).
     * On EARNED/BURNED/DUPLICATE → status {@code REPLAYED}.
     */
    @PostMapping("/{id}/replay")
    public Result<ReplayFailedIngestResponseDto> replay(@PathVariable Long id) {
        return R.success(replayFailedTransactionIngestUseCase.replay(id));
    }

    /** Bulk replay (max 50 ids). Continues on individual errors. */
    @PostMapping("/replay")
    public Result<List<ReplayFailedIngestResponseDto>> replayBulk(
        @Valid @RequestBody BulkReplayFailedIngestRequestDto body
    ) {
        return R.success(replayFailedTransactionIngestUseCase.replayBulk(body));
    }
}
