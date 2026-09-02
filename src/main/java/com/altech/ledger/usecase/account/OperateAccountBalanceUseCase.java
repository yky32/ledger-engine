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
 * The only place ledger/available balances are mutated.
 * <p>
 * Two book ops: {@link #deposit} (credit) and {@link #withdrawal} (debit).
 * In-wallet transfer / earn / burn are those two composed (from-leg debit, to-leg credit).
 * Product APIs stay movement+legs verbs.
 * <p>
 * Concurrency is {@code Account.version} ({@code @Version} on {@code AuditEntity}).
 */
@Component
@RequiredArgsConstructor
public class OperateAccountBalanceUseCase {
    private final AccountRepository accountRepository;

    /** Credit ledger and available (product deposit, earn CREDIT, refund CREDIT). */
    @Transactional
    public Account deposit(Long accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        BigDecimal delta = requireAmount(amount);
        account.setLedgerBalance(account.getLedgerBalance().add(delta));
        account.setAvailableBalance(account.getAvailableBalance().add(delta));
        return accountRepository.save(account);
    }

    /** Debit ledger and available (product withdrawal, earn DEBIT, refund DEBIT). */
    @Transactional
    public Account withdrawal(Long accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        BigDecimal delta = requireAmount(amount);
        BigDecimal available = account.getAvailableBalance();
        if (!account.isAllowNegative() && available.compareTo(delta) < 0) {
            throw new BizException(MovementErrorResponse.MOV0403,
                "Insufficient available balance on account " + account.getId());
        }
        account.setLedgerBalance(account.getLedgerBalance().subtract(delta));
        account.setAvailableBalance(available.subtract(delta));
        return accountRepository.save(account);
    }

    /** HOLD: available only (ledger unchanged). */
    @Transactional
    public Account lockAvailable(Long accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        BigDecimal delta = requireAmount(amount);
        BigDecimal available = account.getAvailableBalance();
        if (!account.isAllowNegative() && available.compareTo(delta) < 0) {
            throw new BizException(MovementErrorResponse.MOV0403,
                "Insufficient available to hold on account " + account.getId());
        }
        account.setAvailableBalance(available.subtract(delta));
        return accountRepository.save(account);
    }

    /** RELEASE: available only; cannot exceed ledger. */
    @Transactional
    public Account unlockAvailable(Long accountId, BigDecimal amount) {
        Account account = requireAccount(accountId);
        BigDecimal delta = requireAmount(amount);
        BigDecimal next = account.getAvailableBalance().add(delta);
        if (next.compareTo(account.getLedgerBalance()) > 0) {
            throw new BizException(MovementErrorResponse.MOV0400,
                "Release would make available > ledger on account " + account.getId());
        }
        account.setAvailableBalance(next);
        return accountRepository.save(account);
    }

    private Account requireAccount(Long accountId) {
        if (accountId == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "accountId required");
        }
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + accountId));
    }

    private static BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "amount required");
        }
        return amount;
    }
}
