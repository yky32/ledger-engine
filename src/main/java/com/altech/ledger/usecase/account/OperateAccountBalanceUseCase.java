package com.altech.ledger.usecase.account;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * The only place ledger/available balances are mutated. Three core ops:
 * <ol>
 *   <li>{@link #deposit} — credit one book</li>
 *   <li>{@link #withdrawal} — debit one book</li>
 *   <li>{@link #inWalletTransfer} — debit from + credit to (same currency)</li>
 * </ol>
 * Product APIs stay movement+legs verbs. Earn/burn compose {@code withdrawal} + {@code deposit}
 * on HOUSE vs member (not {@code CreateDepositUseCase}).
 */
@Component
@RequiredArgsConstructor
public class OperateAccountBalanceUseCase {
    private final AccountRepository accountRepository;

    /** Credit ledger and available (product deposit, earn CREDIT, refund CREDIT). */
    @Transactional
    public Account deposit(Long accountId, BigDecimal amount) {
        return creditLocked(requireLocked(accountId), requireAmount(amount));
    }

    /** Debit ledger and available (product withdrawal, earn DEBIT, refund DEBIT). */
    @Transactional
    public Account withdrawal(Long accountId, BigDecimal amount) {
        return debitLocked(requireLocked(accountId), requireAmount(amount));
    }

    /**
     * Move amount from one book to another. Locks both ids in ascending order.
     * Same-currency only.
     */
    @Transactional
    public TransferResult inWalletTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId == null || toAccountId == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "from and to accountId required");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new BizException(MovementErrorResponse.MOV0400, "in-wallet transfer requires two books");
        }
        BigDecimal delta = requireAmount(amount);
        Account first = requireLocked(fromAccountId < toAccountId ? fromAccountId : toAccountId);
        Account second = requireLocked(fromAccountId < toAccountId ? toAccountId : fromAccountId);
        Account from = first.getId().equals(fromAccountId) ? first : second;
        Account to = first.getId().equals(toAccountId) ? first : second;
        if (from.getCurrency() != to.getCurrency()) {
            throw new BizException(AccountErrorResponse.ACC0400,
                "in-wallet transfer currency mismatch " + from.getCurrency() + " -> " + to.getCurrency());
        }
        debitLocked(from, delta);
        creditLocked(to, delta);
        return new TransferResult(from, to);
    }

    /** HOLD: available only (ledger unchanged). */
    @Transactional
    public Account lockAvailable(Long accountId, BigDecimal amount) {
        Account locked = requireLocked(accountId);
        BigDecimal delta = requireAmount(amount);
        BigDecimal available = locked.getAvailableBalance();
        if (!locked.isAllowNegative() && available.compareTo(delta) < 0) {
            throw new BizException(MovementErrorResponse.MOV0403,
                "Insufficient available to hold on account " + locked.getId());
        }
        locked.setAvailableBalance(available.subtract(delta));
        return accountRepository.save(locked);
    }

    /** RELEASE: available only; cannot exceed ledger. */
    @Transactional
    public Account unlockAvailable(Long accountId, BigDecimal amount) {
        Account locked = requireLocked(accountId);
        BigDecimal delta = requireAmount(amount);
        BigDecimal next = locked.getAvailableBalance().add(delta);
        if (next.compareTo(locked.getLedgerBalance()) > 0) {
            throw new BizException(MovementErrorResponse.MOV0400,
                "Release would make available > ledger on account " + locked.getId());
        }
        locked.setAvailableBalance(next);
        return accountRepository.save(locked);
    }

    public record TransferResult(Account from, Account to) {}

    private Account creditLocked(Account locked, BigDecimal delta) {
        locked.setLedgerBalance(locked.getLedgerBalance().add(delta));
        locked.setAvailableBalance(locked.getAvailableBalance().add(delta));
        return accountRepository.save(locked);
    }

    private Account debitLocked(Account locked, BigDecimal delta) {
        BigDecimal available = locked.getAvailableBalance();
        if (!locked.isAllowNegative() && available.compareTo(delta) < 0) {
            throw new BizException(MovementErrorResponse.MOV0403,
                "Insufficient available balance on account " + locked.getId());
        }
        locked.setLedgerBalance(locked.getLedgerBalance().subtract(delta));
        locked.setAvailableBalance(available.subtract(delta));
        return accountRepository.save(locked);
    }

    private Account requireLocked(Long accountId) {
        if (accountId == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "accountId required");
        }
        return accountRepository.lockById(accountId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + accountId));
    }

    private static BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "amount required");
        }
        return amount;
    }
}
