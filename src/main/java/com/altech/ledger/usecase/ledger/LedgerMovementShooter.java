package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
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
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerInWalletTransferRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWithdrawalRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

/**
 * LedgerMovementShooter extends BaseLedgerMovementShooter.
 */
@Service
public class LedgerMovementShooter extends BaseLedgerMovementShooter {
    private final WalletService walletService;

    public LedgerMovementShooter(WalletService walletService, LedgerMovementRepository ledgerMovementRepository,
                                 MovementBus movementBus) {
        super(movementBus, ledgerMovementRepository, LedgerMovementMode.AUTO);
        this.walletService = walletService;
    }

    @Transactional
    public GetLedgerMovementResponseDto doDeposit(CreateLedgerDepositRequestDto req) {
        String target = req.resolvedTargetWalletId();
        if (target == null || target.isBlank()) {
            throw new BizException(MovementErrorResponse.MOV0400, "targetWalletId or targetId required");
        }
        Wallet wallet = walletService.resolve(target);
        requireActive(wallet);
        String key = key(req.movementKey(), "dep");
        return ledgerMovementRepository().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            LedgerMovement m = newMovement(key, wallet.getId(), OrderType.DEPOSIT, req.mode(),
                req.originatorId(), String.valueOf(wallet.getId()), req.amount(),
                req.currency(), req.description());
            ledgerMovementRepository().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    @Transactional
    public GetLedgerMovementResponseDto doWithdrawal(CreateLedgerWithdrawalRequestDto req) {
        String origin = req.resolvedOriginatorWalletId();
        if (origin == null || origin.isBlank()) {
            throw new BizException(MovementErrorResponse.MOV0400, "originatorWalletId or originatorId required");
        }
        Wallet wallet = walletService.resolve(origin);
        requireActive(wallet);
        String key = key(req.movementKey(), "wd");
        return ledgerMovementRepository().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            LedgerMovement m = newMovement(key, wallet.getId(), OrderType.WITHDRAWAL, req.mode(),
                String.valueOf(wallet.getId()), req.targetId(), req.amount(),
                req.currency(), req.description());
            ledgerMovementRepository().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    @Transactional
    public GetLedgerMovementResponseDto doInWalletTransfer(CreateLedgerInWalletTransferRequestDto req) {
        Wallet from = walletService.resolve(req.fromWalletId());
        Wallet to = walletService.resolve(req.toWalletId());
        requireActive(from);
        requireActive(to);
        String key = key(req.movementKey(), "xfer");
        return ledgerMovementRepository().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            LedgerMovement m = newMovement(key, from.getId(), OrderType.IN_WALLET_TRANSFER, req.mode(),
                String.valueOf(from.getId()), String.valueOf(to.getId()), req.amount(),
                req.currency(), req.description());
            ledgerMovementRepository().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }


    @Transactional
    public GetLedgerMovementResponseDto doEarnBurn(Long walletId, OrderType orderType, java.math.BigDecimal amount,
                                       Currency currency, String movementKey, String description) {
        return doEarnBurn(walletId, orderType, amount, currency, movementKey, description, null);
    }

    public GetLedgerMovementResponseDto doEarnBurn(Long walletId, OrderType orderType, java.math.BigDecimal amount,
                                       Currency currency, String movementKey, String description, Long accountId) {
        Wallet wallet = walletService.get(walletId);
        requireActive(wallet);
        String key = key(movementKey, orderType.name().toLowerCase());
        return ledgerMovementRepository().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            String bookRef = accountId != null ? String.valueOf(accountId) : String.valueOf(walletId);
            String origin = orderType == OrderType.BURN ? bookRef : null;
            String target = (orderType == OrderType.EARN || orderType == OrderType.ADJUSTMENT)
                ? bookRef : null;
            LedgerMovement m = newMovement(key, walletId, orderType, LedgerMovementMode.AUTO,
                origin, target, amount, currency, description);
            ledgerMovementRepository().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    /**
     * HOLD locks available only; RELEASE unlocks available only (ledger unchanged).
     */
    @Transactional
    public GetLedgerMovementResponseDto doHoldRelease(
        Long walletId,
        OrderType orderType,
        java.math.BigDecimal amount,
        Currency currency,
        String movementKey,
        String description
    ) {
        if (orderType != OrderType.HOLD && orderType != OrderType.RELEASE) {
            throw new BizException(MovementErrorResponse.MOV0400, "orderType must be HOLD or RELEASE");
        }
        Wallet wallet = walletService.get(walletId);
        requireActive(wallet);
        String key = key(movementKey, orderType.name().toLowerCase());
        return ledgerMovementRepository().findByMovementKey(key).map(DtoMapper::toMovement).orElseGet(() -> {
            String walletRef = String.valueOf(walletId);
            // HOLD uses originator; RELEASE uses target (same wallet)
            String origin = orderType == OrderType.HOLD ? walletRef : null;
            String target = orderType == OrderType.RELEASE ? walletRef : null;
            LedgerMovement m = newMovement(key, walletId, orderType, LedgerMovementMode.AUTO,
                origin, target, amount, currency, description);
            ledgerMovementRepository().save(m);
            return DtoMapper.toMovement(execute(m));
        });
    }

    public LedgerMovementEvent convert(LedgerMovement m) {
        return MovementBus.toEvent(m);
    }

    private LedgerMovement newMovement(String key, Long walletId, OrderType orderType,
                                       LedgerMovementMode mode, String originatorId, String targetId,
                                       java.math.BigDecimal amount, Currency currency, String description) {
        LedgerMovementMode effective = mode == null ? LedgerMovementMode.AUTO : mode;
        LedgerMovement m = new LedgerMovement();
        m.setMovementKey(key);
        m.setWalletId(walletId);
        m.setOrderType(orderType);
        m.setMode(effective);
        m.setOriginatorId(originatorId);
        m.setTargetId(targetId);
        m.setAmount(amount);
        m.setCurrency(currency);
        m.setMetadata(description);
        m.setAlias(key);
        m.setType(com.altech.ledger.entity.enu.LedgerMovementType.TRANSFER);
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
