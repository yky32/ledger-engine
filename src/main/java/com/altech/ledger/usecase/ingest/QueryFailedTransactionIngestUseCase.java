package com.altech.ledger.usecase.ingest;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.entity.po.ingest.FailedTransactionIngest;
import com.altech.ledger.exception.response.IntegrationErrorResponse;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query failed / skipped transactional ingest rows (ops / admin).
 */
@Component
@RequiredArgsConstructor
public class QueryFailedTransactionIngestUseCase {
    private final FailedTransactionIngestRepository repository;

    @Transactional(readOnly = true)
    public GetFailedTransactionIngestResponseDto one(Long id) {
        FailedTransactionIngest row = repository.findById(id)
            .orElseThrow(() -> new BizException(IntegrationErrorResponse.ING0404, "Failed ingest not found: " + id));
        return _toDto(row);
    }

    @Transactional(readOnly = true)
    public List<GetFailedTransactionIngestResponseDto> byEventId(String eventId) {
        return repository.findByEventIdOrderByIdDesc(eventId).stream().map(this::_toDto).toList();
    }

    /**
     * @param status optional OPEN | REVIEWED | REPLAYED
     * @param associatedIdentifier optional CUST_ID
     * @param failureCode optional AGE | CURRENCY | NO_WALLET | …
     * @param limit max rows (default 50, cap 200)
     */
    @Transactional(readOnly = true)
    public List<GetFailedTransactionIngestResponseDto> search(
        String status,
        String associatedIdentifier,
        String failureCode,
        Integer limit
    ) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        String st = blankToNull(status);
        String aid = blankToNull(associatedIdentifier);
        String code = blankToNull(failureCode);
        return repository.search(st, aid, code, PageRequest.of(0, size)).stream().map(this::_toDto).toList();
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private GetFailedTransactionIngestResponseDto _toDto(FailedTransactionIngest f) {
        return new GetFailedTransactionIngestResponseDto(
            f.getId(),
            f.getEventId(),
            f.getAssociatedIdentifier(),
            f.getEventType(),
            f.getAmount(),
            f.getCurrency(),
            f.getOccurredAt(),
            f.getFailureCode(),
            f.getReason(),
            f.getStatus(),
            f.getRawPayload(),
            f.getCreateDt(),
            f.getUpdateDt()
        );
    }
}
