package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.enu.AccountStatus;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.coa.HouseBooksUseCase;
import com.altech.ledger.util.CoaCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;

/**
 * House (UAF finance) wallet counterparty for earn/burn when no AccountingRule sequence binds.
 * Owner id {@link HouseBooksUseCase#DEFAULT_OWNER}; leftover PROGRAM is renamed on ensure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramPoolService {
    /** @deprecated use {@link HouseBooksUseCase#DEFAULT_OWNER} */
    public static final String PROGRAM_OWNER = HouseBooksUseCase.DEFAULT_OWNER;

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final HouseBooksUseCase houseBooksUseCase;

    @Transactional
    public Account ensurePoolAccount(Currency currency) {
        if (currency == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "pool currency required");
        }
        houseBooksUseCase.ensure(HouseBooksUseCase.DEFAULT_OWNER);
        Wallet house = walletRepository.findByOwnerId(HouseBooksUseCase.DEFAULT_OWNER)
            .or(() -> walletRepository.findByOwnerId(HouseBooksUseCase.LEGACY_OWNER))
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "HOUSE wallet missing after ensure"));
        Account primary = accountRepository.findById(house.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "HOUSE primary missing"));

        return accountRepository.findAllByWalletId(house.getId()).stream()
            .filter(a -> a.getCurrency() == currency)
            .min(Comparator
                .comparing((Account a) -> !"02".equals(a.getType()))
                .thenComparing(a -> a.getId() == null ? 0L : a.getId()))
            .map(pool -> {
                if (!pool.isAllowNegative()) {
                    pool.setAllowNegative(true);
                    return accountRepository.save(pool);
                }
                return pool;
            })
            .orElseGet(() -> _openOperatingBook(house, primary, currency));
    }

    private Account _openOperatingBook(Wallet house, Account primary, Currency currency) {
        String entity = "01";
        String type = "02";
        String subType = "01";
        String buffer = primary.getBuffer() == null ? CoaCodes.BUFFER : primary.getBuffer();
        String main = primary.getMainAccount();
        Account a = Account.builder()
            .walletId(house.getId())
            .entity(entity)
            .type(type)
            .subType(subType)
            .mainAccount(main)
            .buffer(buffer)
            .currency(currency)
            .allowNegative(true)
            .ledgerBalance(BigDecimal.ZERO)
            .availableBalance(BigDecimal.ZERO)
            .status(AccountStatus.ACTIVE)
            .build();
        a.setFullNumber(CoaCodes.fullNumber(entity, type, subType, main, buffer, currency));
        return accountRepository.save(a);
    }
}
