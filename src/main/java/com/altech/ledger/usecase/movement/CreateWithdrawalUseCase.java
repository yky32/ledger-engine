package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.WithdrawalRequest;
import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.ledger.PostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product withdrawal — debits member book for currency (single-sided).
 * Posts via central {@link PostingService} (not loyalty BURN / PROGRAM DE).
 */
@Component
@RequiredArgsConstructor
public class CreateWithdrawalUseCase {
    private final CommonUseCase commonUseCase;
    private final PostingService postingService;

    @Transactional
    public MovementResponse execute(WithdrawalRequest request) {
        Wallet wallet = commonUseCase.requireActiveWallet(request.ownerId(), request.currency());
        GetLedgerMovementResponseDto r = postingService.post(PostingCommand.withdrawal(
            wallet.getId(),
            request.amount(),
            request.currency(),
            request.movementKey(),
            request.description(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.targetId()
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
