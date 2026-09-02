package com.altech.ledger.usecase.ingest;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.entity.po.ingest.FailedTransactionIngest;
import com.altech.ledger.exception.response.IntegrationErrorResponse;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.util.Pageables;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return DtoWrapper.getFailedTransactionIngestResponseDto(row);
    }

    @Transactional(readOnly = true)
    public List<GetFailedTransactionIngestResponseDto> byEventId(String eventId) {
        return repository.findByEventIdOrderByIdDesc(eventId).stream()
            .map(DtoWrapper::getFailedTransactionIngestResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<GetFailedTransactionIngestResponseDto> search(
        String status,
        String ownerId,
        String failureCode,
        Pageable pageable
    ) {
        String st = blankToNull(status);
        String aid = blankToNull(ownerId);
        String code = blankToNull(failureCode);
        return repository.search(st, aid, code, Pageables.toZeroBased(pageable))
            .map(DtoWrapper::getFailedTransactionIngestResponseDto);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
