package com.altech.ledger.endpoint.ledger.account;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import java.util.List;

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
    public Result<LedgerAccountDtos.Response> create(@Valid @RequestBody LedgerAccountDtos.CreateRequest dto) {
        return R.success(accountSetupUseCase.create(dto));
    }

    @GetMapping("/{id}")
    public Result<LedgerAccountDtos.Response> getOne(@PathVariable Long id) {
        return R.success(accountOperationUseCase.getOne(id));
    }

    @GetMapping
    public Result<List<LedgerAccountDtos.Response>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return R.success(accountOperationUseCase.getAll(pageable));
    }
}
