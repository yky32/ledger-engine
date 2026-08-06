package com.altech.ledger.usecase.movement;

import com.altech.ledger.config.IntegrationProperties;
import com.altech.ledger.entity.dto.ledger.LedgerDto.*;
import com.altech.ledger.entity.dto.movement.MovementDto.*;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.po.journal.JournalEntry;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovementUseCase {
    private final IntegrationProperties properties;
    private final LedgerUseCase ledgerUseCase;
    private final WalletRepository wallets;
    private final AccountRepository accounts;
    private final LedgerMovementRepository movements;

    @Transactional
    public MovementResponse deposit(DepositRequest request) {
        Wallet wallet = requireActiveWallet(request.ownerId(), request.currency());
        return createMovement(request.movementKey(), wallet, OrderType.DEPOSIT, request.mode(),
            wallet.getOwnerId(), null, request.amount(), request.currency(), request.description());
    }

    @Transactional
    public MovementResponse withdraw(WithdrawalRequest request) {
        Wallet wallet = requireActiveWallet(request.ownerId(), request.currency());
        return createMovement(request.movementKey(), wallet, OrderType.WITHDRAWAL, request.mode(),
            wallet.getOwnerId(), request.targetId(), request.amount(), request.currency(), request.description());
    }

    @Transactional
    public MovementResponse transfer(InWalletTransferRequest request) {
        Wallet source = requireActiveWallet(request.fromOwnerId(), request.currency());
        requireActiveWallet(request.toOwnerId(), request.currency());
        return createMovement(request.movementKey(), source, OrderType.IN_WALLET_TRANSFER,
            request.mode(), source.getOwnerId(), request.toOwnerId(), request.amount(), request.currency(),
            request.description());
    }

    @Transactional
    public MovementResponse settle(Long movementId, SettleMovementRequest request) {
        LedgerMovement movement = movement(movementId);
        if (movement.getStatus() != LedgerMovementStatus.PENDING
            && movement.getStatus() != LedgerMovementStatus.PROCESSING) {
            throw LedgerException.conflict("MOVEMENT_NOT_SETTLABLE",
                "Movement is not pending settlement: " + movement.getStatus());
        }
        if (movement.getJournalTransactionId() != null) {
            return response(movement);
        }
        Wallet wallet = wallet(movement.getWalletId());
        postForMovement(movement, wallet, request.description());
        return response(movements.save(movement));
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

    private MovementResponse createMovement(String movementKey, Wallet wallet, OrderType orderType,
                                            LedgerMovementMode mode, String originatorId, String targetId,
                                            java.math.BigDecimal amount, String currency, String description) {
        Optional<LedgerMovement> existing = movements.findByMovementKey(movementKey);
        if (existing.isPresent()) {
            return response(existing.get());
        }

        LedgerMovement movement = new LedgerMovement(movementKey, wallet.getId(), orderType, mode,
            originatorId, targetId, amount, currency, null);
        movements.save(movement);

        if (mode == LedgerMovementMode.AUTO) {
            postForMovement(movement, wallet, description);
            movements.save(movement);
        } else {
            movement.markProcessing();
            movements.save(movement);
        }
        return response(movement);
    }

    private void postForMovement(LedgerMovement movement, Wallet wallet, String description) {
        List<EntryRequest> legs = buildLegs(movement, wallet);
        PostTransactionRequest post = new PostTransactionRequest(
            "movement:" + movement.getMovementKey(),
            movement.getMovementKey(),
            description,
            null,
            legs
        );
        LedgerUseCase.PostingResult result = ledgerUseCase.post(post);
        movement.markSettled(result.transaction().id());
    }

    private List<EntryRequest> buildLegs(LedgerMovement movement, Wallet wallet) {
        Long walletAccountId = wallet.getAccountId();
        java.math.BigDecimal amount = movement.getAmount();
        String currency = movement.getCurrency();

        return switch (movement.getOrderType()) {
            case DEPOSIT -> List.of(
                entry(clearingDeposit(currency).getId(), JournalEntry.Side.DEBIT, amount, currency, 1),
                entry(walletAccountId, JournalEntry.Side.CREDIT, amount, currency, 2));
            case WITHDRAWAL -> List.of(
                entry(walletAccountId, JournalEntry.Side.DEBIT, amount, currency, 1),
                entry(clearingWithdrawal(currency).getId(), JournalEntry.Side.CREDIT, amount, currency, 2));
            case IN_WALLET_TRANSFER, WALLET_TRANSFER -> {
                Wallet target = wallets.findByOwnerIdAndCurrency(movement.getTargetId(), currency)
                    .orElseThrow(() -> LedgerException.notFound("WALLET_NOT_FOUND",
                        "Target wallet not found: " + movement.getTargetId()));
                yield List.of(
                    entry(walletAccountId, JournalEntry.Side.DEBIT, amount, currency, 1),
                    entry(target.getAccountId(), JournalEntry.Side.CREDIT, amount, currency, 2));
            }
            default -> throw LedgerException.badRequest("UNSUPPORTED_ORDER_TYPE",
                "Order type not supported for posting: " + movement.getOrderType());
        };
    }

    private EntryRequest entry(Long accountId, JournalEntry.Side side, java.math.BigDecimal amount,
                               String currency, int sequence) {
        return new EntryRequest(accountId, side, amount, currency, sequence);
    }

    private Account clearingDeposit(String currency) {
        return requirePool(properties.getDepositClearingRefTemplate(), currency);
    }

    private Account clearingWithdrawal(String currency) {
        return requirePool(properties.getWithdrawalClearingRefTemplate(), currency);
    }

    private Account requirePool(String template, String currency) {
        String ref = template.replace("{currency}", currency);
        return accounts.findByFullNumber(ref)
            .orElseThrow(() -> LedgerException.notFound("POOL_NOT_FOUND", "Program pool missing: " + ref));
    }

    private Wallet requireActiveWallet(String ownerId, String currency) {
        Wallet wallet = wallets.findByOwnerIdAndCurrency(ownerId, currency)
            .orElseThrow(() -> LedgerException.notFound("WALLET_NOT_FOUND",
                "Wallet not onboarded for owner " + ownerId + " / " + currency));
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw LedgerException.conflict("WALLET_NOT_ACTIVE", "Wallet is not active: " + wallet.getStatus());
        }
        return wallet;
    }

    private LedgerMovement movement(Long id) {
        return movements.findById(id)
            .orElseThrow(() -> LedgerException.notFound("MOVEMENT_NOT_FOUND", "Movement not found: " + id));
    }

    private Wallet wallet(Long id) {
        return wallets.findById(id)
            .orElseThrow(() -> LedgerException.notFound("WALLET_NOT_FOUND", "Wallet not found: " + id));
    }

    private MovementResponse response(LedgerMovement movement) {
        return new MovementResponse(movement.getId(), movement.getMovementKey(), movement.getWalletId(),
            movement.getOrderType(), movement.getStatus(), movement.getMode(), movement.getOriginatorId(),
            movement.getTargetId(), movement.getAmount(), movement.getCurrency(),
            movement.getJournalTransactionId(), movement.getCreateDt(), movement.getUpdateDt());
    }
}
