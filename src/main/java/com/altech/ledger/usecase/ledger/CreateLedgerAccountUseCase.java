package com.altech.ledger.usecase.ledger;

import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product ledger account create (external reference COA).
 */
@Component
@RequiredArgsConstructor
public class CreateLedgerAccountUseCase {
    private final AccountRepository accountRepository;
    private final CommonUseCase commonUseCase;

    @Transactional
    public AccountResponse execute(CreateAccountRequest request) {
        commonUseCase.requireCurrency(request.currency());
        if (accountRepository.existsByFullNumber(request.externalReference())) {
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
        return _toResponse(accountRepository.save(account));
    }

    private AccountResponse _toResponse(Account a) {
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
