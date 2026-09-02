package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.InWalletTransferRequest;
import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.ledger.ApplyPostingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product in-wallet transfer — {@code withdrawal} on origin + {@code deposit} on destination
 * via {@link ApplyPostingUseCase}.
 */
@Component
@RequiredArgsConstructor
public class CreateInWalletTransferUseCase {
    private final CommonUseCase commonUseCase;
    private final ApplyPostingUseCase applyPostingUseCase;

    @Transactional
    public MovementResponse execute(InWalletTransferRequest request) {
        Wallet from = commonUseCase.requireActiveWallet(request.fromOwnerId(), request.currency());
        Wallet to = commonUseCase.requireActiveWallet(request.toOwnerId(), request.currency());
        GetLedgerMovementResponseDto r = applyPostingUseCase.execute(PostingCommand.inWalletTransfer(
            from.getId(),
            to.getId(),
            request.amount(),
            request.currency(),
            request.movementKey(),
            request.description(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode()
        ));
        return DtoWrapper.getMovementResponse(r);
    }
}
