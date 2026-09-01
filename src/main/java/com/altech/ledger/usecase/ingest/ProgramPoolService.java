package com.altech.ledger.usecase.ingest;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.request.AccountOpenSpecDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.enu.AccountStatus;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * System PROGRAM wallet + per-currency pool accounts (allow-negative) for double-entry
 * earn/burn counterparty legs.
 * <p>
 * Owner id fixed: {@link #PROGRAM_OWNER}. Created lazily on first earn/burn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramPoolService {
    public static final String PROGRAM_OWNER = "PROGRAM";

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;

    @Transactional
    public Account ensurePoolAccount(Currency currency) {
        if (currency == null) {
            throw new BizException(AccountErrorResponse.ACC0400, "pool currency required");
        }
        Wallet program = walletRepository.findByOwnerId(PROGRAM_OWNER).orElseGet(this::_bootstrapProgramWallet);
        Account primary = accountRepository.findById(program.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "PROGRAM primary missing"));

        Account pool;
        if (primary.getCurrency() == currency) {
            pool = primary;
        } else {
            pool = accountRepository.findByMainAccountAndCurrency(primary.getMainAccount(), currency)
                .orElseGet(() -> _openCurrencyBook(program, primary, currency));
        }
        if (!pool.isAllowNegative()) {
            pool.setAllowNegative(true);
            pool = accountRepository.save(pool);
        }
        return pool;
    }

    private Wallet _bootstrapProgramWallet() {
        log.info("Bootstrapping PROGRAM wallet for double-entry pool");
        try {
            createWalletOnboardingUseCase.execute(new CreateWalletOnboardRequestDto(
                PROGRAM_OWNER,
                Currency.HKD,
                "System PROGRAM",
                List.of(new AccountOpenSpecDto("LP", "PROGRAM LP pool", false, true, Currency.LP))
            ));
        } catch (BizException ex) {
            String code = ex.getResponse() != null ? ex.getResponse().getCode() : null;
            if (!WalletErrorResponse.WAL0409.getCode().equals(code)) {
                throw ex;
            }
        }
        return walletRepository.findByOwnerId(PROGRAM_OWNER)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "PROGRAM wallet missing after bootstrap"));
    }

    private Account _openCurrencyBook(Wallet program, Account primary, Currency currency) {
        String main = primary.getMainAccount();
        int n = accountRepository.allSubAccountNumbers(main).size() + 1;
        String sub = String.format("%04d", n);
        while (accountRepository.findByMainAccountAndSubAccount(main, sub).isPresent()) {
            n++;
            sub = String.format("%04d", n);
        }
        Account a = Account.builder()
            .walletId(program.getId())
            .entity(primary.getEntity())
            .type(primary.getType())
            .subType(primary.getSubType())
            .mainAccount(main)
            .subAccount(sub)
            .buffer(primary.getBuffer())
            .currency(currency)
            .allowNegative(true)
            .ledgerBalance(BigDecimal.ZERO)
            .availableBalance(BigDecimal.ZERO)
            .status(AccountStatus.ACTIVE)
            .build();
        a.setFullNumber(a.getEntity() + a.getType() + a.getSubType() + a.getMainAccount()
            + a.getSubAccount() + a.getBuffer() + a.getCurrency());
        return accountRepository.save(a);
    }
}