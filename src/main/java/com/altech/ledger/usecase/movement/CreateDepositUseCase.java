package com.altech.ledger.usecase.movement;

import com.altech.ledger.entity.dto.movement.MovementDto.DepositRequest;
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
 * Product deposit — single-sided credit on the member book.
 * Balance write is {@code OperateAccountBalanceUseCase.deposit} via {@link ApplyPostingUseCase}.
 * Do not call this for loyalty earn (that needs HOUSE DEBIT + member CREDIT).
 */
@Component
@RequiredArgsConstructor
public class CreateDepositUseCase {
    private final CommonUseCase commonUseCase;
    private final ApplyPostingUseCase applyPostingUseCase;

    @Transactional
    public MovementResponse execute(DepositRequest request) {
        Wallet wallet = commonUseCase.requireActiveWallet(request.ownerId(), request.currency());
        GetLedgerMovementResponseDto r = applyPostingUseCase.execute(PostingCommand.deposit(
            wallet.getId(),
            request.amount(),
            request.currency(),
            request.movementKey(),
            request.description(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode()
        ));
        return DtoWrapper.getMovementResponse(r);
    }
}
