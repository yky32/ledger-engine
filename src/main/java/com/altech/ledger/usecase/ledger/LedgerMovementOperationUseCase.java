package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.service.MovementBus;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementDocumentsRequestDto;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementStatusRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

/**
 * Movement status / settle operations.
 */
@Component
@RequiredArgsConstructor
public class LedgerMovementOperationUseCase {
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerMovementExecutionUseCase ledgerMovementExecutionUseCase;
    private final MovementBus movementBus;
    private final CommonUseCase commonUseCase;

    @Transactional
    public GetLedgerMovementResponseDto update(Long id, UpdateLedgerMovementStatusRequestDto req) {
        LedgerMovement m = commonUseCase.requireMovement(id);
        if (req.status() == LedgerMovementStatus.SETTLED
            && m.getStatus() != LedgerMovementStatus.SETTLED) {
            m.setStatus(LedgerMovementStatus.PROCESSING);
            if (req.remarks() != null) m.setRemarks(req.remarks());
            ledgerMovementRepository.save(m);
            LedgerMovement done = ledgerMovementExecutionUseCase.execute(m);
            movementBus.publishDone(done);
            return DtoMapper.toMovement(done);
        }
        m.setStatus(req.status());
        if (req.remarks() != null) m.setRemarks(req.remarks());
        return DtoMapper.toMovement(ledgerMovementRepository.save(m));
    }


    @Transactional
    public GetLedgerMovementResponseDto settle(Long id) {
        LedgerMovement m = commonUseCase.requireMovement(id);
        if (m.getStatus() == LedgerMovementStatus.SETTLED) {
            return DtoMapper.toMovement(m);
        }
        m.setStatus(LedgerMovementStatus.PROCESSING);
        ledgerMovementRepository.save(m);
        LedgerMovement done = ledgerMovementExecutionUseCase.execute(m);
        movementBus.publishDone(done);
        return DtoMapper.toMovement(done);
    }

    @Transactional
    public GetLedgerMovementResponseDto updateDocuments(Long id, UpdateLedgerMovementDocumentsRequestDto req) {
        LedgerMovement m = commonUseCase.requireMovement(id);
        if (req.files() != null) m.setFiles(req.files());
        if (req.remarks() != null) m.setRemarks(req.remarks());
        return DtoMapper.toMovement(ledgerMovementRepository.save(m));
    }
}
