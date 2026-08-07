package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.enu.*;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.service.MovementBus;
import com.altech.ledger.service.WalletService;
import com.altech.ledger.usecase.BaseLedgerMovementShooter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * LedgerMovementShooter extends BaseLedgerMovementShooter.
 */
@Service
public class LedgerMovementShooter extends BaseLedgerMovementShooter {
    private final WalletService walletService;

    public LedgerMovementShooter(WalletService walletService, LedgerMovementRepository movements,
                                 MovementBus movementBus) {
        super(movementBus, movements, LedgerMovementMode.AUTO);
        this.walletService = walletService;
    }

    @Transactional
    public LedgerMovementDtos.Response doDeposit(LedgerMovementDtos.CreateDepositRequest req) {
        String target = req.resolvedTargetWalletId();
        if (target == null || target.isBlank()) {
            throw new BizException(MovementErrorResponse.MOV0400, "targetWalletId or targetId required");
        }
        Wallet wallet = walletService.resolve(target);
        requireActive(wallet);
        String key = key(req.movementKey(), "dep");
        return movements().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            LedgerMovement m = newMovement(key, wallet.getId(), OrderType.DEPOSIT, req.mode(),
                req.originatorId(), String.valueOf(wallet.getId()), req.amount(),
                req.currency().toUpperCase(), req.description());
            movements().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    @Transactional
    public LedgerMovementDtos.Response doWithdrawal(LedgerMovementDtos.CreateWithdrawalRequest req) {
        String origin = req.resolvedOriginatorWalletId();
        if (origin == null || origin.isBlank()) {
            throw new BizException(MovementErrorResponse.MOV0400, "originatorWalletId or originatorId required");
        }
        Wallet wallet = walletService.resolve(origin);
        requireActive(wallet);
        String key = key(req.movementKey(), "wd");
        return movements().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            LedgerMovement m = newMovement(key, wallet.getId(), OrderType.WITHDRAWAL, req.mode(),
                String.valueOf(wallet.getId()), req.targetId(), req.amount(),
                req.currency().toUpperCase(), req.description());
            movements().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    @Transactional
    public LedgerMovementDtos.Response doInWalletTransfer(LedgerMovementDtos.CreateInWalletTransferRequest req) {
        Wallet from = walletService.resolve(req.fromWalletId());
        Wallet to = walletService.resolve(req.toWalletId());
        requireActive(from);
        requireActive(to);
        String key = key(req.movementKey(), "xfer");
        return movements().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            LedgerMovement m = newMovement(key, from.getId(), OrderType.IN_WALLET_TRANSFER, req.mode(),
                String.valueOf(from.getId()), String.valueOf(to.getId()), req.amount(),
                req.currency().toUpperCase(), req.description());
            movements().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }


    @Transactional
    public LedgerMovementDtos.Response doEarnBurn(Long walletId, OrderType orderType, java.math.BigDecimal amount,
                                       String currency, String movementKey, String description) {
        Wallet wallet = walletService.get(walletId);
        requireActive(wallet);
        String key = key(movementKey, orderType.name().toLowerCase());
        return movements().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            String origin = orderType == OrderType.BURN ? String.valueOf(walletId) : null;
            String target = (orderType == OrderType.EARN || orderType == OrderType.ADJUSTMENT)
                ? String.valueOf(walletId) : null;
            LedgerMovement m = newMovement(key, walletId, orderType, LedgerMovementMode.AUTO,
                origin, target, amount, currency.toUpperCase(), description);
            movements().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    public LedgerMovementEvent convert(LedgerMovement m) {
        return MovementBus.toEvent(m);
    }

    private LedgerMovement newMovement(String key, Long walletId, OrderType orderType,
                                       LedgerMovementMode mode, String originatorId, String targetId,
                                       java.math.BigDecimal amount, String currency, String description) {
        LedgerMovementMode effective = mode == null ? LedgerMovementMode.AUTO : mode;
        LedgerMovement m = new LedgerMovement(key, walletId, orderType, effective,
            originatorId, targetId, amount, currency, description);
        m.setStatus(effective == LedgerMovementMode.AUTO
            ? LedgerMovementStatus.PROCESSING : LedgerMovementStatus.PENDING);
        return m;
    }

    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BizException(WalletErrorResponse.WAL0403, "Wallet is not active: " + wallet.getStatus());
        }
    }

    private String key(String provided, String prefix) {
        if (provided != null && !provided.isBlank()) return provided;
        return prefix + "-" + UUID.randomUUID();
    }
}
