package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade for movement pipeline callers; delegates to Verb / query use cases.
 */
@Component
@RequiredArgsConstructor
public class LedgerMovementPipelineUseCase {
    private final LedgerMovementShooter ledgerMovementShooter;
    private final LedgerMovementQueryUseCase ledgerMovementQueryUseCase;
    private final LedgerMovementOperationUseCase ledgerMovementOperationUseCase;
    private final LedgerDepositUseCase ledgerDepositUseCase;

    @Transactional
    public LedgerMovementDtos.Response deposit(LedgerMovementDtos.CreateDepositRequest req) {
        return ledgerDepositUseCase.execute(req);
    }

    @Transactional
    public LedgerMovementDtos.Response withdraw(LedgerMovementDtos.CreateWithdrawalRequest req) {
        return ledgerMovementShooter.doWithdrawal(req);
    }

    @Transactional
    public LedgerMovementDtos.Response inWalletTransfer(LedgerMovementDtos.CreateInWalletTransferRequest req) {
        return ledgerMovementShooter.doInWalletTransfer(req);
    }

    @Transactional
    public LedgerMovementDtos.Response settle(Long id) {
        return ledgerMovementOperationUseCase.settle(id);
    }

    @Transactional
    public LedgerMovementDtos.Response updateStatus(Long id, LedgerMovementDtos.UpdateStatusRequest req) {
        return ledgerMovementOperationUseCase.update(id, req);
    }

    @Transactional
    public LedgerMovementDtos.Response updateDocuments(Long id, LedgerMovementDtos.UpdateDocumentsRequest req) {
        return ledgerMovementOperationUseCase.updateDocuments(id, req);
    }

    @Transactional(readOnly = true)
    public LedgerMovementDtos.Response getOne(Long id) {
        return ledgerMovementQueryUseCase.one(id);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> getAll(Pageable pageable) {
        return ledgerMovementQueryUseCase.list(pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> myMovements(String ownerId, Pageable pageable) {
        return ledgerMovementQueryUseCase.myMovements(ownerId, pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> byWallet(Long walletId, Pageable pageable) {
        return ledgerMovementQueryUseCase.byWallet(walletId, pageable);
    }
}
