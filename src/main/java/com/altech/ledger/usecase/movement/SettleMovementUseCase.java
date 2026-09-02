package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.SettleMovementRequest;
import com.altech.ledger.service.DtoWrapper;
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
        return DtoWrapper.getMovementResponse(ledgerMovementPipelineUseCase.settle(movementId));
    }
}
