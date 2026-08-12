package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.ingest.IngestionResult;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import com.altech.ledger.entity.dto.request.BulkReplayFailedIngestRequestDto;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.entity.dto.response.ReplayFailedIngestResponseDto;
import com.altech.ledger.entity.po.ingest.FailedTransactionIngest;
import com.altech.ledger.exception.response.IntegrationErrorResponse;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.repository.FailedTransactionIngestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ops: mark reviewed / replay failed ingest through {@link IngestTransactionUseCase}.
 * <p>
 * Replay uses {@link IngestTransactionUseCase#executeWithoutFailPersist} so a still-skipped
 * event does <b>not</b> insert a duplicate fail row.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplayFailedTransactionIngestUseCase {
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_REVIEWED = "REVIEWED";
    public static final String STATUS_REPLAYED = "REPLAYED";

    private final FailedTransactionIngestRepository repository;
    private final IngestTransactionUseCase ingestTransactionUseCase;
    private final ObjectMapper objectMapper;
    private final QueryFailedTransactionIngestUseCase queryFailedTransactionIngestUseCase;

    @Transactional
    public GetFailedTransactionIngestResponseDto markReviewed(Long id) {
        FailedTransactionIngest row = _require(id);
        if (STATUS_REPLAYED.equals(row.getStatus())) {
            throw new BizException(MovementErrorResponse.MOV0400, "Already REPLAYED; cannot mark REVIEWED");
        }
        row.setStatus(STATUS_REVIEWED);
        repository.save(row);
        return queryFailedTransactionIngestUseCase.one(id);
    }

    @Transactional
    public ReplayFailedIngestResponseDto replay(Long id) {
        FailedTransactionIngest row = _require(id);
        String previous = row.getStatus();
        if (STATUS_REPLAYED.equals(previous)) {
            throw new BizException(MovementErrorResponse.MOV0409, "Failed ingest already REPLAYED: " + id);
        }

        TransactionalEvent event = _toEvent(row);
        // no second fail row on still-skipped
        IngestionResult result = ingestTransactionUseCase.executeWithoutFailPersist(event);

        boolean applied = result.status() == IngestionResult.Status.EARNED
            || result.status() == IngestionResult.Status.BURNED
            || result.status() == IngestionResult.Status.PROCESSED
            || result.status() == IngestionResult.Status.DUPLICATE;

        row = _require(id);
        if (applied) {
            row.setStatus(STATUS_REPLAYED);
            row.setReason(_trim(result.reason() != null
                ? "REPLAYED: " + result.status() + " " + result.reason()
                : "REPLAYED: " + result.status()));
        } else {
            if (!STATUS_REVIEWED.equals(previous)) {
                row.setStatus(STATUS_OPEN);
            }
            String why = result.reason() != null ? result.reason() : String.valueOf(result.status());
            row.setReason(_trim("REPLAY_STILL_SKIPPED: " + why));
            // refresh failureCode from skip reason when possible
            if (result.status() == IngestionResult.Status.SKIPPED && result.reason() != null) {
                // keep original failureCode; reason carries replay note
            }
        }
        repository.save(row);
        return new ReplayFailedIngestResponseDto(id, previous, row.getStatus(), result);
    }

    /** Best-effort bulk replay (each id independent; continues on single failures). */
    @Transactional
    public List<ReplayFailedIngestResponseDto> replayBulk(BulkReplayFailedIngestRequestDto req) {
        List<ReplayFailedIngestResponseDto> out = new ArrayList<>();
        for (Long id : req.ids()) {
            if (id == null) {
                continue;
            }
            try {
                out.add(replay(id));
            } catch (BizException ex) {
                log.warn("bulk replay skip id={}: {}", id, ex.getMessage());
                out.add(new ReplayFailedIngestResponseDto(
                    id,
                    null,
                    "ERROR",
                    IngestionResult.skipped(String.valueOf(id),
                        ex.getMessage() == null ? "replay failed" : ex.getMessage())
                ));
            } catch (RuntimeException ex) {
                log.error("bulk replay error id={}", id, ex);
                out.add(new ReplayFailedIngestResponseDto(
                    id, null, "ERROR",
                    IngestionResult.skipped(String.valueOf(id),
                        ex.getMessage() == null ? "replay failed" : ex.getMessage())
                ));
            }
        }
        return out;
    }

    private FailedTransactionIngest _require(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new BizException(IntegrationErrorResponse.ING0404, "id=" + id));
    }

    private TransactionalEvent _toEvent(FailedTransactionIngest row) {
        if (row.getRawPayload() != null && !row.getRawPayload().isBlank()) {
            try {
                return objectMapper.readValue(row.getRawPayload(), TransactionalEvent.class);
            } catch (Exception ex) {
                log.warn("rawPayload parse failed id={} — rebuild from columns", row.getId());
            }
        }
        if (row.getEventId() == null || row.getAssociatedIdentifier() == null
            || row.getEventType() == null || row.getAmount() == null || row.getCurrency() == null) {
            throw new BizException(MovementErrorResponse.MOV0400,
                "Cannot rebuild event for failed ingest id=" + row.getId());
        }
        Currency ccy = Currency.get(row.getCurrency());
        return new TransactionalEvent(
            row.getEventId(),
            row.getAssociatedIdentifier(),
            row.getEventType(),
            row.getAmount(),
            ccy,
            row.getOccurredAt(),
            Map.of("replayFromFailedIngestId", String.valueOf(row.getId()))
        );
    }

    private static String _trim(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
