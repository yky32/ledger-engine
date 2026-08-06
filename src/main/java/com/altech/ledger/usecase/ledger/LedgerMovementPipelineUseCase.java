package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade keeping existing callers stable; delegates to split use cases (old naming).
 */
@Service
public class LedgerMovementPipelineUseCase {
    private final LedgerMovementShooter shooter;
    private final LedgerMovementQueryUseCase queryUseCase;
    private final LedgerMovementOperationUseCase operationUseCase;
    private final LedgerDepositUseCase depositUseCase;

    public LedgerMovementPipelineUseCase(LedgerMovementShooter shooter,
                                         LedgerMovementQueryUseCase queryUseCase,
                                         LedgerMovementOperationUseCase operationUseCase,
                                         LedgerDepositUseCase depositUseCase) {
        this.shooter = shooter;
        this.queryUseCase = queryUseCase;
        this.operationUseCase = operationUseCase;
        this.depositUseCase = depositUseCase;
    }

    @Transactional
    public MovementResponse deposit(CreateDepositRequest req) {
        return depositUseCase.execute(req);
    }

    @Transactional
    public MovementResponse withdraw(CreateWithdrawalRequest req) {
        return shooter.doWithdrawal(req);
    }

    @Transactional
    public MovementResponse inWalletTransfer(CreateInWalletTransferRequest req) {
        return shooter.doInWalletTransfer(req);
    }

    @Transactional
    public MovementResponse swiftTransfer(CreateSwiftTransferRequest req) {
        return shooter.doSwiftTransfer(req);
    }

    @Transactional
    public MovementResponse settle(Long id) {
        return operationUseCase.settle(id);
    }

    @Transactional
    public MovementResponse updateStatus(Long id, UpdateMovementStatusRequest req) {
        return operationUseCase.update(id, req);
    }

    @Transactional
    public MovementResponse updateCompliance(Long id, UpdateComplianceRequest req) {
        return operationUseCase.updateCompliance(id, req);
    }

    @Transactional
    public MovementResponse updateDocuments(Long id, UpdateTransferDocumentsRequest req) {
        return operationUseCase.updateDocuments(id, req);
    }

    @Transactional(readOnly = true)
    public MovementResponse getOne(Long id) {
        return queryUseCase.getOne(id);
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> getAll(Pageable pageable) {
        return queryUseCase.getAll(pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> myMovements(String ownerId, Pageable pageable) {
        return queryUseCase.myMovements(ownerId, pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> byWallet(Long walletId, Pageable pageable) {
        return queryUseCase.byWallet(walletId, pageable);
    }
}
