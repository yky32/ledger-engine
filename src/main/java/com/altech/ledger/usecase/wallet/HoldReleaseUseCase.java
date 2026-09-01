package com.altech.ledger.usecase.wallet;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.request.CreateHoldReleaseRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.coa.CoaBookResolver;
import com.altech.ledger.usecase.ledger.ApplyPostingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HoldReleaseUseCase {
    private final WalletRepository walletRepository;
    private final ApplyPostingUseCase applyPostingUseCase;
    private final CoaBookResolver coaBookResolver;

    @Transactional
    public GetLedgerMovementResponseDto hold(CreateHoldReleaseRequestDto req) {
        return _run(req, OrderType.HOLD);
    }

    @Transactional
    public GetLedgerMovementResponseDto release(CreateHoldReleaseRequestDto req) {
        return _run(req, OrderType.RELEASE);
    }

    private GetLedgerMovementResponseDto _run(CreateHoldReleaseRequestDto req, OrderType type) {
        Wallet w = walletRepository.findByOwnerId(req.ownerId().trim())
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found: " + req.ownerId()));
        String desc = req.description() != null ? req.description() : type.name() + " " + req.currency();
        Account book = coaBookResolver.resolveMemberBook(w.getId(), req.currency());
        PostingCommand cmd = type == OrderType.HOLD
            ? PostingCommand.hold(w.getId(), req.amount(), req.currency(), req.movementKey(), desc, book.getId())
            : PostingCommand.release(w.getId(), req.amount(), req.currency(), req.movementKey(), desc, book.getId());
        return applyPostingUseCase.execute(cmd);
    }
}
