package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerInWalletTransferRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWithdrawalRequestDto;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementDocumentsRequestDto;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementStatusRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.PostingIntent;
import com.altech.ledger.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade for movement pipeline callers; balance writes go through {@link ApplyPostingUseCase}.
 */
@Component
@RequiredArgsConstructor
public class LedgerMovementPipelineUseCase {
    private final ApplyPostingUseCase applyPostingUseCase;
    private final WalletService walletService;
    private final LedgerMovementQueryUseCase ledgerMovementQueryUseCase;
    private final LedgerMovementOperationUseCase ledgerMovementOperationUseCase;
    private final LedgerDepositUseCase ledgerDepositUseCase;

    @Transactional
    public GetLedgerMovementResponseDto deposit(CreateLedgerDepositRequestDto req) {
        return ledgerDepositUseCase.execute(req);
    }

    @Transactional
    public GetLedgerMovementResponseDto withdraw(CreateLedgerWithdrawalRequestDto req) {
        var wallet = walletService.resolve(req.resolvedOriginatorWalletId());
        return applyPostingUseCase.execute(new PostingCommand(
            PostingIntent.WITHDRAWAL,
            wallet.getId(),
            req.amount(),
            req.currency(),
            req.movementKey(),
            req.description(),
            req.mode() == null ? LedgerMovementMode.AUTO : req.mode(),
            null,
            req.targetId()
        ));
    }

    @Transactional
    public GetLedgerMovementResponseDto inWalletTransfer(CreateLedgerInWalletTransferRequestDto req) {
        var from = walletService.resolve(req.fromWalletId());
        var to = walletService.resolve(req.toWalletId());
        return applyPostingUseCase.execute(PostingCommand.inWalletTransfer(
            from.getId(),
            to.getId(),
            req.amount(),
            req.currency(),
            req.movementKey(),
            req.description(),
            req.mode() == null ? LedgerMovementMode.AUTO : req.mode()
        ));
    }

    @Transactional
    public GetLedgerMovementResponseDto settle(Long id) {
        return ledgerMovementOperationUseCase.settle(id);
    }

    @Transactional
    public GetLedgerMovementResponseDto updateStatus(Long id, UpdateLedgerMovementStatusRequestDto req) {
        return ledgerMovementOperationUseCase.update(id, req);
    }

    @Transactional
    public GetLedgerMovementResponseDto updateDocuments(Long id, UpdateLedgerMovementDocumentsRequestDto req) {
        return ledgerMovementOperationUseCase.updateDocuments(id, req);
    }

    @Transactional(readOnly = true)
    public GetLedgerMovementResponseDto getOne(Long id) {
        return ledgerMovementQueryUseCase.one(id);
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> getAll(Pageable pageable) {
        return ledgerMovementQueryUseCase.list(pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> myMovements(String ownerId, Pageable pageable) {
        return ledgerMovementQueryUseCase.myMovements(ownerId, pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> byWallet(Long walletId, Pageable pageable) {
        return ledgerMovementQueryUseCase.byWallet(walletId, pageable);
    }
}
