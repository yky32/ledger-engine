package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Facade keeping existing callers stable; delegates to split use cases (old naming).
 */
@Service
@RequiredArgsConstructor
public class LedgerMovementPipelineUseCase {
    private final LedgerMovementShooter shooter;
    private final LedgerMovementQueryUseCase queryUseCase;
    private final LedgerMovementOperationUseCase operationUseCase;
    private final LedgerDepositUseCase depositUseCase;

    @Transactional
    public LedgerMovementDtos.Response deposit(LedgerMovementDtos.CreateDepositRequest req) {
        return depositUseCase.execute(req);
    }

    @Transactional
    public LedgerMovementDtos.Response withdraw(LedgerMovementDtos.CreateWithdrawalRequest req) {
        return shooter.doWithdrawal(req);
    }

    @Transactional
    public LedgerMovementDtos.Response inWalletTransfer(LedgerMovementDtos.CreateInWalletTransferRequest req) {
        return shooter.doInWalletTransfer(req);
    }


    @Transactional
    public LedgerMovementDtos.Response settle(Long id) {
        return operationUseCase.settle(id);
    }

    @Transactional
    public LedgerMovementDtos.Response updateStatus(Long id, LedgerMovementDtos.UpdateStatusRequest req) {
        return operationUseCase.update(id, req);
    }


    @Transactional
    public LedgerMovementDtos.Response updateDocuments(Long id, LedgerMovementDtos.UpdateDocumentsRequest req) {
        return operationUseCase.updateDocuments(id, req);
    }

    @Transactional(readOnly = true)
    public LedgerMovementDtos.Response getOne(Long id) {
        return queryUseCase.getOne(id);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> getAll(Pageable pageable) {
        return queryUseCase.getAll(pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> myMovements(String ownerId, Pageable pageable) {
        return queryUseCase.myMovements(ownerId, pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<LedgerMovementDtos.Response> byWallet(Long walletId, Pageable pageable) {
        return queryUseCase.byWallet(walletId, pageable);
    }
}
