package com.altech.ledger.endpoint.ledger.account;

import com.altech.ledger.entity.dto.parity.ParityDtos.AccountResponse;
import com.altech.ledger.entity.dto.parity.ParityDtos.CreateLedgerAccountRequest;
import com.altech.ledger.usecase.account.AccountOperationUseCase;
import com.altech.ledger.usecase.setup.AccountSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ledger-accounts")
public class LedgerAccountEndpoint {
    private final AccountSetupUseCase accountSetupUseCase;
    private final AccountOperationUseCase accountOperationUseCase;

    public LedgerAccountEndpoint(AccountSetupUseCase accountSetupUseCase,
                                 AccountOperationUseCase accountOperationUseCase) {
        this.accountSetupUseCase = accountSetupUseCase;
        this.accountOperationUseCase = accountOperationUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateLedgerAccountRequest dto) {
        return accountSetupUseCase.create(dto);
    }

    @GetMapping("/{id}")
    public AccountResponse getOne(@PathVariable Long id) {
        return accountOperationUseCase.getOne(id);
    }

    @GetMapping
    public Page<AccountResponse> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return accountOperationUseCase.getAll(pageable);
    }
}
