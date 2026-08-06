package com.altech.ledger.usecase.account;

import com.altech.ledger.entity.dto.parity.ParityDtos.AccountResponse;
import com.altech.ledger.service.DtoMapper;
import com.altech.ledger.usecase.setup.AccountSetupUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of the-wallet-ledger AccountOperationUseCase (account queries).
 */
@Service
public class AccountOperationUseCase {
    private final AccountSetupUseCase accountSetupUseCase;

    public AccountOperationUseCase(AccountSetupUseCase accountSetupUseCase) {
        this.accountSetupUseCase = accountSetupUseCase;
    }

    @Transactional(readOnly = true)
    public AccountResponse getOne(Long id) {
        return accountSetupUseCase.getOne(id);
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAll(Pageable pageable) {
        return accountSetupUseCase.getAll(pageable);
    }
}
