package com.altech.ledger.endpoint.ledger.account;

import com.altech.ledger.usecase.account.AccountOperationUseCase;
import com.altech.ledger.usecase.setup.AccountSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.account.LedgerAccountDtos;

@RestController
@RequestMapping("/ledger-accounts")
@RequiredArgsConstructor
public class LedgerAccountEndpoint {
    private final AccountSetupUseCase accountSetupUseCase;
    private final AccountOperationUseCase accountOperationUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerAccountDtos.Response create(@Valid @RequestBody LedgerAccountDtos.CreateRequest dto) {
        return accountSetupUseCase.create(dto);
    }

    @GetMapping("/{id}")
    public LedgerAccountDtos.Response getOne(@PathVariable Long id) {
        return accountOperationUseCase.getOne(id);
    }

    @GetMapping
    public Page<LedgerAccountDtos.Response> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return accountOperationUseCase.getAll(pageable);
    }
}
