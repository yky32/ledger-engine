package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.usecase.CommonUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class QueryLedgerAccountUseCase {
    private final CommonUseCase commonUseCase;

    @Transactional(readOnly = true)
    public AccountResponse one(Long id) {
        return _toResponse(commonUseCase.requireAccount(id));
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(Long id) {
        Account account = commonUseCase.requireAccount(id);
        return new BalanceResponse(
            id, account.getCurrency(), account.getLedgerBalance(), account.getAvailableBalance());
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
