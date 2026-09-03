package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.BalanceExecutionResultCommand;
import com.altech.ledger.entity.dto.event.WalletTierChangedEvent;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.entity.po.wallet.WalletTierPolicy;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.coa.HouseBooksUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

/**
 * Realtime wallet.tier from the watched COA book's ledgerBalance after settle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssessWalletTierUseCase {
    private final WalletRepository walletRepository;
    private final WalletTierPolicyUseCase walletTierPolicyUseCase;

    public record Change(
        Wallet wallet,
        String fromTier,
        String toTier,
        BigDecimal lp,
        Currency currency,
        String reason
    ) {}

    @Transactional
    public Optional<Change> assess(LedgerMovement movement, BalanceExecutionResultCommand command) {
        if (movement == null || movement.getWalletId() == null) {
            return Optional.empty();
        }
        Wallet wallet = walletRepository.findById(movement.getWalletId()).orElse(null);
        if (wallet == null || isHouse(wallet.getOwnerId())) {
            return Optional.empty();
        }
        WalletTierPolicy policy = walletTierPolicyUseCase.findEnabled().orElse(null);
        if (policy == null) {
            return Optional.empty();
        }
        Account watched = matchingBook(movement.getWalletId(), policy, command);
        if (watched == null) {
            return Optional.empty();
        }
        String from = wallet.getTier() == null || wallet.getTier().isBlank()
            ? policy.getBands().get(0).code()
            : wallet.getTier().trim().toUpperCase(Locale.ROOT);
        String next = WalletTierPolicyUseCase.nextTier(policy.getBands(), from, watched.getLedgerBalance());
        if (next == null || next.equalsIgnoreCase(from)) {
            return Optional.empty();
        }
        wallet.setTier(next);
        walletRepository.save(wallet);
        String reason = rank(policy, next) > rank(policy, from) ? "UPGRADE" : "DOWNGRADE";
        log.info("wallet tier {} {} → {} lp={} movementId={}",
            wallet.getOwnerId(), from, next, watched.getLedgerBalance(), movement.getId());
        return Optional.of(new Change(
            wallet, from, next, watched.getLedgerBalance(), watched.getCurrency(), reason));
    }

    public static WalletTierChangedEvent toEvent(LedgerMovement movement, Change change) {
        return WalletTierChangedEvent.builder()
            .eventName("WALLET_TIER_CHANGED")
            .walletId(change.wallet().getId())
            .ownerId(change.wallet().getOwnerId())
            .fromTier(change.fromTier())
            .toTier(change.toTier())
            .lpLedgerBalance(change.lp())
            .currency(change.currency())
            .movementId(movement.getId())
            .movementKey(movement.getMovementKey())
            .orderType(movement.getOrderType())
            .reason(change.reason())
            .build();
    }

    private static Account matchingBook(
        Long walletId,
        WalletTierPolicy policy,
        BalanceExecutionResultCommand command
    ) {
        if (command == null || command.getDetails() == null) {
            return null;
        }
        String entity = policy.getEntity();
        String type = policy.getType();
        String subType = policy.getSubType();
        String ccy = policy.getCurrency() == null ? null : policy.getCurrency().toUpperCase(Locale.ROOT);
        for (BalanceExecutionResultCommand.CommandDetail d : command.getDetails()) {
            Account a = d.getAccount();
            if (a == null || a.getCurrency() == null || !walletId.equals(a.getWalletId())) {
                continue;
            }
            if (entity.equals(a.getEntity())
                && type.equals(a.getType())
                && subType.equals(a.getSubType())
                && ccy.equals(a.getCurrency().getIsoCode())) {
                return a;
            }
        }
        return null;
    }

    private static int rank(WalletTierPolicy policy, String code) {
        var ordered = WalletTierPolicyUseCase.ordered(policy.getBands());
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).code().equalsIgnoreCase(code)) {
                return i;
            }
        }
        return 0;
    }

    private static boolean isHouse(String ownerId) {
        if (ownerId == null) {
            return false;
        }
        String o = ownerId.trim().toUpperCase(Locale.ROOT);
        return HouseBooksUseCase.DEFAULT_OWNER.equals(o) || "PROGRAM".equals(o);
    }
}
