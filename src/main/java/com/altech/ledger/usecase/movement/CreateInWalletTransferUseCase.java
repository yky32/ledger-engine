package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.InWalletTransferRequest;
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
public class CreateInWalletTransferUseCase {
    private final CommonUseCase commonUseCase;
    private final LedgerMovementPipelineUseCase ledgerMovementPipelineUseCase;

    @Transactional
    public MovementResponse execute(InWalletTransferRequest request) {
        Wallet from = commonUseCase.requireActiveWallet(request.fromOwnerId(), request.currency());
        Wallet to = commonUseCase.requireActiveWallet(request.toOwnerId(), request.currency());
        LedgerMovementDtos.Response r = ledgerMovementPipelineUseCase.inWalletTransfer(new LedgerMovementDtos.CreateInWalletTransferRequest(
            String.valueOf(from.getId()),
            String.valueOf(to.getId()),
            request.currency(),
            request.amount(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.movementKey(),
            request.description()
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
