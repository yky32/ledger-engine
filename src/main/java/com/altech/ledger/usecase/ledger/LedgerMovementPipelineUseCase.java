package com.altech.ledger.usecase.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerInWalletTransferRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWithdrawalRequestDto;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementDocumentsRequestDto;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementStatusRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

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
    public GetLedgerMovementResponseDto deposit(CreateLedgerDepositRequestDto req) {
        return ledgerDepositUseCase.execute(req);
    }

    @Transactional
    public GetLedgerMovementResponseDto withdraw(CreateLedgerWithdrawalRequestDto req) {
        return ledgerMovementShooter.doWithdrawal(req);
    }

    @Transactional
    public GetLedgerMovementResponseDto inWalletTransfer(CreateLedgerInWalletTransferRequestDto req) {
        return ledgerMovementShooter.doInWalletTransfer(req);
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
