package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.WithdrawalRequest;
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
 * Product withdrawal — single-sided debit on the member book.
 * Balance write is {@code OperateAccountBalanceUseCase.withdrawal} via {@link ApplyPostingUseCase}.
 * Do not call this for loyalty burn (that needs member DEBIT + HOUSE CREDIT).
 */
@Component
@RequiredArgsConstructor
public class CreateWithdrawalUseCase {
    private final CommonUseCase commonUseCase;
    private final ApplyPostingUseCase applyPostingUseCase;

    @Transactional
    public MovementResponse execute(WithdrawalRequest request) {
        Wallet wallet = commonUseCase.requireActiveWallet(request.ownerId(), request.currency());
        GetLedgerMovementResponseDto r = applyPostingUseCase.execute(PostingCommand.withdrawal(
            wallet.getId(),
            request.amount(),
            request.currency(),
            request.movementKey(),
            request.description(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.targetId()
        ));
        return DtoWrapper.getMovementResponse(r);
    }
}
