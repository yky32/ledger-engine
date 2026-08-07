package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.DepositRequest;
import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateDepositUseCase {
    private final CommonUseCase commonUseCase;
    private final LedgerMovementPipelineUseCase pipeline;

    @Transactional
    public MovementResponse execute(DepositRequest request) {
        Wallet wallet = commonUseCase.requireActiveWallet(request.ownerId(), request.currency());
        LedgerMovementDtos.Response r = pipeline.deposit(new LedgerMovementDtos.CreateDepositRequest(
            String.valueOf(wallet.getId()),
            request.currency(),
            request.amount(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.ownerId(),
            request.movementKey(),
            request.description(),
            null
        ));
        return _toDto(r);
    }

    private MovementResponse _toDto(LedgerMovementDtos.Response r) {
        return new MovementResponse(
            r.id(), r.movementKey(), r.walletId(), r.orderType(), r.status(), r.mode(),
            r.originatorId(), r.targetId(), r.amount(), r.currency(),
            r.createDt(), r.updateDt());
    }
}
