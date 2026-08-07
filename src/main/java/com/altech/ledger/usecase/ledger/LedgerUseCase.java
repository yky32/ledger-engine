package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account setup and balance lookup. Classic journal posting was removed;
 * balances are maintained on {@link Account} via movement execution.
 */
@Service
@RequiredArgsConstructor
public class LedgerUseCase {
    private final AccountRepository accounts;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        requireLedgerCurrency(request.currency());
        if (accounts.existsByFullNumber(request.externalReference())) {
            throw new BizException(AccountErrorResponse.ACC0409, "External reference already exists");
        }
        Account account = new Account(
            request.externalReference(),
            "10",
            request.type().name(),
            "00",
            request.externalReference(),
            request.name(),
            "NA",
            request.currency(),
            request.allowNegative()
        );
        return accountResponse(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        return accountResponse(account(id));
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long id) {
        Account account = account(id);
        return new BalanceResponse(
            id, account.getCurrency(), account.getLedgerBalance(), account.getAvailableBalance());
    }

    private Account account(Long id) {
        return accounts.findById(id).orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + id));
    }

    private void requireLedgerCurrency(String currency) {
        if (currency == null || !currency.matches("[A-Z]{2,4}")) {
            throw new BizException(AccountErrorResponse.ACC0400, "Currency must be 2-4 uppercase letters");
        }
    }

    private AccountResponse accountResponse(Account a) {
        CoaType coa;
        try {
            coa = CoaType.valueOf(a.getType());
        } catch (Exception ex) {
            coa = CoaType.LIABILITY;
        }
        return new AccountResponse(
            a.getId(), a.getFullNumber(), a.getSubAccount(), coa, a.getCurrency(), a.getStatus(),
            a.isAllowNegative(), a.getLedgerBalance(), a.getAvailableBalance(), a.getVersion(),
            a.getCreateDt(), a.getUpdateDt());
    }
}
