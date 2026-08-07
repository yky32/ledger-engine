package com.altech.ledger.usecase.movement;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;

import com.altech.ledger.entity.dto.ledger.LedgerDto.PageResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.*;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner-centric movement API. Posts via movement pipeline (account balances + movement log);
 * no classic journal layer.
 */
@Service
@RequiredArgsConstructor
public class MovementUseCase {
    private final WalletRepository wallets;
    private final LedgerMovementRepository movements;
    private final LedgerMovementPipelineUseCase pipeline;

    @Transactional
    public MovementResponse deposit(DepositRequest request) {
        Wallet wallet = requireActiveWallet(request.ownerId(), request.currency());
        LedgerMovementDtos.Response r = pipeline.deposit(new LedgerMovementDtos.CreateDepositRequest(
            String.valueOf(wallet.getId()),
            request.currency(),
            request.amount(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.ownerId(),
            request.movementKey(),
            request.description(),
            null
        ));
        return toDto(r);
    }

    @Transactional
    public MovementResponse withdraw(WithdrawalRequest request) {
        Wallet wallet = requireActiveWallet(request.ownerId(), request.currency());
        LedgerMovementDtos.Response r = pipeline.withdraw(new LedgerMovementDtos.CreateWithdrawalRequest(
            String.valueOf(wallet.getId()),
            request.currency(),
            request.amount(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.targetId(),
            request.movementKey(),
            request.description()
        ));
        return toDto(r);
    }

    @Transactional
    public MovementResponse transfer(InWalletTransferRequest request) {
        Wallet from = requireActiveWallet(request.fromOwnerId(), request.currency());
        Wallet to = requireActiveWallet(request.toOwnerId(), request.currency());
        LedgerMovementDtos.Response r = pipeline.inWalletTransfer(new LedgerMovementDtos.CreateInWalletTransferRequest(
            String.valueOf(from.getId()),
            String.valueOf(to.getId()),
            request.currency(),
            request.amount(),
            request.mode() == null ? LedgerMovementMode.AUTO : request.mode(),
            request.movementKey(),
            request.description()
        ));
        return toDto(r);
    }

    @Transactional
    public MovementResponse settle(Long movementId, SettleMovementRequest request) {
        return toDto(pipeline.settle(movementId));
    }

    @Transactional(readOnly = true)
    public MovementResponse get(Long id) {
        return response(movement(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> listByWallet(Long walletId, Pageable pageable) {
        wallet(walletId);
        Page<LedgerMovement> page = movements.findByWalletId(walletId, pageable);
        return new PageResponse<>(page.map(this::response).getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    private MovementResponse toDto(LedgerMovementDtos.Response r) {
        return new MovementResponse(
            r.id(), r.movementKey(), r.walletId(), r.orderType(), r.status(), r.mode(),
            r.originatorId(), r.targetId(), r.amount(), r.currency(),
            r.createDt(), r.updateDt());
    }

    private MovementResponse response(LedgerMovement movement) {
        return new MovementResponse(movement.getId(), movement.getMovementKey(), movement.getWalletId(),
            movement.getOrderType(), movement.getStatus(), movement.getMode(), movement.getOriginatorId(),
            movement.getTargetId(), movement.getAmount(), movement.getCurrency(),
            movement.getCreateDt(), movement.getUpdateDt());
    }

    private Wallet requireActiveWallet(String ownerId, String currency) {
        Wallet wallet = wallets.findByOwnerIdAndCurrency(ownerId, currency)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not onboarded for owner " + ownerId + " / " + currency));
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BizException(WalletErrorResponse.WAL0403, "Wallet is not active: " + wallet.getStatus());
        }
        return wallet;
    }

    private LedgerMovement movement(Long id) {
        return movements.findById(id)
            .orElseThrow(() -> new BizException(MovementErrorResponse.MOV0404, "Movement not found: " + id));
    }

    private Wallet wallet(Long id) {
        return wallets.findById(id)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found: " + id));
    }
}
