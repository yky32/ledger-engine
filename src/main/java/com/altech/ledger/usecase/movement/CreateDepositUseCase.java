package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.DepositRequest;
import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

@Component
@RequiredArgsConstructor
public class CreateDepositUseCase {
    private final CommonUseCase commonUseCase;
    private final LedgerMovementPipelineUseCase ledgerMovementPipelineUseCase;

    @Transactional
    public MovementResponse execute(DepositRequest request) {
        Wallet wallet = commonUseCase.requireActiveWallet(request.ownerId(), request.currency());
        GetLedgerMovementResponseDto r = ledgerMovementPipelineUseCase.deposit(new CreateLedgerDepositRequestDto(
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

    private MovementResponse _toDto(GetLedgerMovementResponseDto r) {
        return new MovementResponse(
            r.id(), r.movementKey(), r.walletId(), r.orderType(), r.status(), r.mode(),
            r.originatorId(), r.targetId(), r.amount(), r.currency(),
            r.createDt(), r.updateDt());
    }
}
