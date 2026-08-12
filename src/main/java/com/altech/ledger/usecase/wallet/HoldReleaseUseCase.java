package com.altech.ledger.usecase.wallet;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.CreateHoldReleaseRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerMovementShooter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HoldReleaseUseCase {
    private final WalletRepository walletRepository;
    private final LedgerMovementShooter ledgerMovementShooter;

    @Transactional
    public GetLedgerMovementResponseDto hold(CreateHoldReleaseRequestDto req) {
        return _run(req, OrderType.HOLD);
    }

    @Transactional
    public GetLedgerMovementResponseDto release(CreateHoldReleaseRequestDto req) {
        return _run(req, OrderType.RELEASE);
    }

    private GetLedgerMovementResponseDto _run(CreateHoldReleaseRequestDto req, OrderType type) {
        Wallet w = walletRepository.findByOwnerId(req.associatedIdentifier().trim())
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404,
                "Wallet not found: " + req.associatedIdentifier()));
        String desc = req.description() != null ? req.description() : type.name() + " " + req.currency();
        return ledgerMovementShooter.doHoldRelease(
            w.getId(), type, req.amount(), req.currency(), req.movementKey(), desc);
    }
}
