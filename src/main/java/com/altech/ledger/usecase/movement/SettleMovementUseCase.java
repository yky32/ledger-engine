package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.SettleMovementRequest;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SettleMovementUseCase {
    private final LedgerMovementPipelineUseCase ledgerMovementPipelineUseCase;

    @Transactional
    public MovementResponse execute(Long movementId, SettleMovementRequest request) {
        return _toDto(ledgerMovementPipelineUseCase.settle(movementId));
    }

    private MovementResponse _toDto(LedgerMovementDtos.Response r) {
        return new MovementResponse(
            r.id(), r.movementKey(), r.walletId(), r.orderType(), r.status(), r.mode(),
            r.originatorId(), r.targetId(), r.amount(), r.currency(),
            r.createDt(), r.updateDt());
    }
}
