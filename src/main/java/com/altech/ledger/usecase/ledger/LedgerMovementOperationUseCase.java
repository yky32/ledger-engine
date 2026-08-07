package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.service.MovementBus;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movement status / settle operations.
 */
@Component
@RequiredArgsConstructor
public class LedgerMovementOperationUseCase {
    private final LedgerMovementRepository movements;
    private final LedgerMovementExecutionUseCase execution;
    private final MovementBus movementBus;
    private final CommonUseCase commonUseCase;

    @Transactional
    public LedgerMovementDtos.Response update(Long id, LedgerMovementDtos.UpdateStatusRequest req) {
        LedgerMovement m = commonUseCase.requireMovement(id);
        if (req.status() == LedgerMovementStatus.SETTLED
            && m.getStatus() != LedgerMovementStatus.SETTLED) {
            m.setStatus(LedgerMovementStatus.PROCESSING);
            if (req.remarks() != null) m.setRemarks(req.remarks());
            movements.save(m);
            LedgerMovement done = execution.execute(m);
            movementBus.publishDone(done);
            return DtoMapper.toMovement(done);
        }
        m.setStatus(req.status());
        if (req.remarks() != null) m.setRemarks(req.remarks());
        return DtoMapper.toMovement(movements.save(m));
    }


    @Transactional
    public LedgerMovementDtos.Response settle(Long id) {
        LedgerMovement m = commonUseCase.requireMovement(id);
        if (m.getStatus() == LedgerMovementStatus.SETTLED) {
            return DtoMapper.toMovement(m);
        }
        m.setStatus(LedgerMovementStatus.PROCESSING);
        movements.save(m);
        LedgerMovement done = execution.execute(m);
        movementBus.publishDone(done);
        return DtoMapper.toMovement(done);
    }

    @Transactional
    public LedgerMovementDtos.Response updateDocuments(Long id, LedgerMovementDtos.UpdateDocumentsRequest req) {
        LedgerMovement m = commonUseCase.requireMovement(id);
        if (req.files() != null) m.setFiles(req.files());
        if (req.remarks() != null) m.setRemarks(req.remarks());
        return DtoMapper.toMovement(movements.save(m));
    }
}
