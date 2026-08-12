package com.altech.ledger.endpoint.ingest;

import com.altech.core.exception.BizException;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.ingest.LedgerLegDto;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.usecase.ingest.IngestTransactionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Query double-entry legs for loyalty / ledger movements (read-only).
 * <p>
 * Use either {@code ?movementId=} or {@code ?eventId=} (optional {@code operation=earn|burn|process}).
 */
@RestController
@RequestMapping("/integrations/ledger-entries")
@RequiredArgsConstructor
public class LedgerEntryQueryEndpoint {
    private final IngestTransactionUseCase ingestTransactionUseCase;

    @GetMapping
    public Result<List<LedgerLegDto>> query(
        @RequestParam(required = false) Long movementId,
        @RequestParam(required = false) String eventId,
        @RequestParam(required = false) String operation
    ) {
        boolean hasMovement = movementId != null;
        boolean hasEvent = eventId != null && !eventId.isBlank();
        if (hasMovement == hasEvent) {
            // both or neither
            throw new BizException(MovementErrorResponse.MOV0400,
                "Provide exactly one of movementId or eventId");
        }
        if (hasMovement) {
            return R.success(ingestTransactionUseCase.legsForMovementId(movementId));
        }
        return R.success(ingestTransactionUseCase.legsForEventId(eventId.trim(), operation));
    }
}
